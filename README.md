# Smart Filesystem / CIT-VS-Project SS2014 / Deployment guide

## Contents
 * [Directory overview](#directory-overview)
 * [Compile project with maven](#compile-project-with-maven)
 * [Zabbix setup](#zabbix-setup)
 * [Floodlight setup](#floodlight-setup)
 * [Hadoop setup](#hadoop-setup)
 * [Energenie logger](#energenie-logger)
 * [Reporting](#reporting)

## Directory overview
 * [Project documentation / paper (LaTeX files)](Ausarbeitung)
 * [Zabbix helper and utilities](helper/cit-energy-project-helper)
 * [Zabbix templates](zabbix-templates)
 * [SDN/Floodlight deployment](sdn/floodlight)
 * Hadoop
   * [Hadoop 2.4.0 modifications](hadoop/hadoop-2.4.0-cit-enery-project)
   * [Hadoop filter and utils classes](hadoop/cit-energy-project-hadoop)
 * [Reporting web frontend and webapp](reporting/cit-energy-project-reporting)
 * [Energenie cloud protocol documentation](energenie/README.md)

## Compile project with maven
First stept is to compile everything and install it to your local maven repository:

    mvn package install

## Zabbix setup
  1. Import both datanode templates:
    * [Datanode template](zabbix-templates/zabbix-template-datanode.xml) (Contains power usage and OS specific linked templates)
    * [Datanode user template](zabbix-templates/zabbix-template-datanode-users.xml) (Contains user specific items)
    * User fields in user template, e.g. user mpjss14:

        | Key | Description |
        | --- | ----------- |
        | user.mpjss14.lastAddress | Last IP:Port used on datanode connection. |
        | user.mpjss14.lastInternalAddress | Last internal used IP:Port on data connection (internal = connection between datanodes, e.g. on replica creation). |
        | user.mpjss14.bandwidth | Used bandwidth in Byte/s between external client and datanode (including both directions). |
        | user.mpjss14.internalBandwidth | Internal used bandwith in Bytes/s (e.g. on replica creation). |
        | user.mpjss14.blockEvents | Block creation and removal events. |
        | user.mpjss14.dataUsageDeltas | Used storage difference on last event, can be positive or negativ on removal in bytes. |
        | user.mpjss14.dataUsage | Storage usage in directory /users/mpjss14 after last event in bytes. |
        | user.mpjss14.lastUsedProfile | Last requested profile. |

  2. Create new datanodes and link them with datanode template.
  3. Manage users within zabbix template via users util class:

        cd helper/cit-energy-project-helper
        java -jar target/cit-energy-project-helper-1.3-SNAPSHOT-jar-with-dependencies.jar de.tuberlin.cit.project.energy.zabbix.ZabbixUserMappingUtil --delete userA
        java -jar target/cit-energy-project-helper-1.3-SNAPSHOT-jar-with-dependencies.jar de.tuberlin.cit.project.energy.zabbix.ZabbixUserMappingUtil --create userB

## Floodlight setup
  1. Compile floodlight:

      ```bash
      cd sdn/floodlight-master
      ant dist
      ```
  2. Copy `sdn/floodlight-master/target/floodlight.jar` on to your server.
  3. Run with `java -jar floodlight.jar`.

## Hadoop setup
  1. Patch hadoop 2.4.0 distribution with [hadoop-2.4.0-cit-energy-project.patch](hadoop/hadoop-2.4.0-cit-enery-project/hadoop-2.4.0-cit-energy-project.patch).
  2. Compile and setup hadoop as [usual](http://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/ClusterSetup.html).
  3. Add energy project libs:

        cp hadoop/cit-energy-project-hadoop/target/cit-energy-project-hadoop-1.3-SNAPSHOT-with-additional-dependencies.jar [hadoop-2.4.0]/share/hadoop/hdfs/

  4. Add `bin/topology.sh` with e.g. the following content:
      ```bash
      #!/bin/bash
      if [ "$1" == "10.42.0.1" ]; then
          echo "/fast-rack" 
      elif [ "$1" == "10.42.0.2" ]; then
          echo "/cheap-rack" 
      else
          echo "/default-rack" 
      fi
      ```
  5. Edit `etc/hadoop/hdfs-site.xml` and add at least the folowing lines:
      ```xml
      <configuration>
       <property>
          <name>dfs.block.access.token.enable</name>
          <value>true</value> <!-- adds usernames in datanode connections -->
        </property>
        <property>
          <name>dfs.namenode.acls.enabled</name>
          <value>true</value> <!-- enables usernames in hadoop -->
        </property>

        <property>
          <name>dfs.datanode.hostname</name>
          <value>CitProjectDummy2</value> <!-- used as hostname in zabbix logging -->
        </property>
        <property>
          <name>topology.script.file.name</name>
          <value>bin/topology.sh</value> <!-- initial rack configuration (see above) -->
        </property>
         <property>
          <name>dfs.energy.zabbix.hostname</name>
          <value>mpjss14.cit.tu-berlin.de</value> <!-- zabbix agent endpoint -->
        </property>
        <property>
          <name>dfs.energy.zabbix.port</name>
          <value>10051</value>
        </property>
        <property>
          <name>dfs.energy.zabbix.rest.url</name>
          <value>https://mpjss14.cit.tu-berlin.de/zabbix/api_jsonrpc.php</value>
        </property>
        <property>
          <name>dfs.energy.zabbix.rest.username</name>
          <value>admin</value>
        </property>
        <property>
          <name>dfs.energy.zabbix.rest.password</name>
          <value><!-- Password --></value>
        </property>
      </configuration>
      ```

  6. If something doesn't work, debugging can be enabled in `etc/hadoop/log4j.properties`:

      ```ini
      log4j.de.tuberlin.cit.project.energy=DEBUG
      ```

  7. Create some data as _mpjss14_ user (only files stored in `/users/[user]` are used in statistics!):

      ```bash
      dd if=/dev/zero of=136mb-dummy.dat bs=136MB count=1 # erzeugt Datei mit 136MB
      ./bin/hadoop fs -mkdir /users/mpjss14
      ./bin/hadoop fs -copyFromLocal 136mb-dummy.dat /users/mpjss14/136mb-dummy.dat
      ```

  8. Profiles can be select via `curl` and filter results testet with hadoop util class:

      ```bash
      # show current profile
      curl http://mpjss14.cit.tu-berlin.de:50200/api/v1/user-profile; echo
      # switch profile to CHEAP
      curl -XPUT -d 'username=mpjss14&profile=CHEAP' http://mpjss14.cit.tu-berlin.de:50200/api/v1/user-profile; echo
      # fetch first 1024 and show filtered/available data nodes
      java -cp $(./bin/hadoop classpath) de.tuberlin.cit.project.energy.hadoop.HadoopClient /users/$USER/136mb-dummy.dat

      # now check traffic at zabbix

      # switch profile to FAST
      curl -XPUT -d 'username=mpjss14&profile=FAST' http://mpjss14.cit.tu-berlin.de:50200/api/v1/user-profile; echo
      # fetch first 1024 and show filtered/available data nodes
      java -cp $(./bin/hadoop classpath) de.tuberlin.cit.project.energy.hadoop.HadoopClient /users/$USER/136mb-dummy.dat
      ```
## Energenie logger
  * Power usage collector (html scraper) can be started as follows:
        
        cd helper/cit-energy-project-helper/target
        java -cp cit-energy-project-helper-1.3-SNAPSHOT-jar-with-dependencies.jar de.tuberlin.cit.project.energy.zabbix.EnergyDataPusher

  * Simulator ca be started via:

        cd helper/cit-energy-project-helper/target
        java -cp cit-energy-project-helper-1.3-SNAPSHOT-jar-with-dependencies.jar de.tuberlin.cit.project.energy.zabbix.PowerUsageSimulator

## Reporting
### Webapp
  A precompiled version can be found in `reporting/cit-energy-project-reporting/webapp/dist`.
  
  Steps to recompile after changing the app (not required if not changed):

  1. Setup required tools:

    ```bash
    cd reporting/cit-energy-project-reporting/webapp
    npm install # installs nodejs modules into node_modules
    bower install # installs bower dependencies into app/bower_components
    ```

  2. Compile via [grunt](http://gruntjs.com/):

    ```bash
    # serve site at http://localhost:9000 in development mode (including livereload and api proxy)
    grunt serve
    # after development phase, compile distribution files
    grunt dist
    # final files are now in dist dir
    ```

### Web frontend server
 1. Start web frontend: `java -jar target/cit-energy-project-reporting-1.3-SNAPSHOT-jar-with-dependencies.jar`
 2. Open it in your browser: http://localhost:50201/

