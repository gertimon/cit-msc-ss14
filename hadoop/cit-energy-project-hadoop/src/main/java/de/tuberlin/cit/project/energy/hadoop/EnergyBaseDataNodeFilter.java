package de.tuberlin.cit.project.energy.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.web.JsonUtil;
import org.mortbay.util.ajax.JSON;

import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;

/**
 * This class acts as interface between the energy based data node selection and
 * the name node.
 *
 * @author Tobias und Sascha
 */
public class EnergyBaseDataNodeFilter {

    private final static Log LOG = LogFactory.getLog(EnergyBaseDataNodeFilter.class);
    private final String dataNodeSelectorAddress;
    private final int dataNodeSelectorPort;

    private final Properties ENERGY_USER_PROPERTIES = loadProperties("energy.user.config.properties");
    private final Properties ENERGY_RACK_PROPERTIES = loadProperties("energy.rack.config.properties");

    private final ZabbixSender zabbixSender;

    public enum EnergyMode { FAST, CHEAP, NONE }

    public EnergyBaseDataNodeFilter(String dataNodeSelectorAddress, int dataNodeSelectorPort) {
        this.dataNodeSelectorAddress = dataNodeSelectorAddress;
        this.dataNodeSelectorPort = dataNodeSelectorPort;
        this.zabbixSender = new ZabbixSender();

        LOG.info("New energy data node selector client initialized with address="
            + this.dataNodeSelectorAddress + " and port=" + this.dataNodeSelectorPort + ".");
    }

    public LocatedBlocks filterBlockLocations(LocatedBlocks locatedBlocks, String path, String username, String remoteAddress) {

        LocatedBlocks orderedBlocks = null;

        try {
            LOG.info("Got decision request (" + path + "), loggin user's ip into zabbix.");
            zabbixSender.sendLastUserIP(username, remoteAddress);
            // TODO push filename, opened files, removed replication locations
            // datanode send ip+port+user

            LOG.info("Input: " + toJson(locatedBlocks, path, username, remoteAddress));

            // try ordering
            orderedBlocks = orderBlocks(username, locatedBlocks);

            LOG.info("Located blocks output: " + JsonUtil.toJsonString(locatedBlocks));

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.fatal(e);
        }
        
        return orderedBlocks;
    }

    /**
     * removes some blocks that mismatchs a filter strategy if required packets
     * exist on desired data nodes.
     *
     * @param blockFilterStrategy recognises CHEAP, FAST, null
     * @param locatedBlocks
     * @return
     */
    private LocatedBlocks orderBlocks(String username, LocatedBlocks locatedBlocks) {

        EnergyMode userMode = setEnergyMode(username, ENERGY_USER_PROPERTIES);

        // create set of racks for each mode given
        // only take cheap racks
        Set<String> cheapRacks = new HashSet<String>();
        for (String propertyName : ENERGY_RACK_PROPERTIES.stringPropertyNames()) {
            if (ENERGY_RACK_PROPERTIES.getProperty(propertyName).equals(EnergyMode.CHEAP.toString())) {
                cheapRacks.add(propertyName);
            }
        }

        // try to recognize filter strategy or return same list
        if (EnergyMode.CHEAP == userMode) {
            for (LocatedBlock block : locatedBlocks.getLocatedBlocks()) {
                // find first matching
                List<DatanodeInfo> cleanLocations = new ArrayList<DatanodeInfo>();
                for (DatanodeInfo location : block.getLocations()) {
                    if (cheapRacks.contains(location.getNetworkLocation())) {
                        cleanLocations.add(location);
                    }
                }
                if (cleanLocations.size() > 0) {
                    DatanodeInfo[] locations = block.getLocations();
                    // TODO replace by override, this maybe will not change anything
                    locations = (DatanodeInfo[]) cleanLocations.toArray();
                    LOG.info("block location list manipulated");
                } else {
                    LOG.info("block location list not manipulated");
                }
            }
        } else {
            LOG.warn("could not recognize a filter strategy for mode '" + userMode + "', use 'CHEAP' or -'FAST'- instead, return same list of blocks.");
        }
        
        return locatedBlocks;
    }

    private EnergyMode setEnergyMode(String username, Properties properties) {
        EnergyMode mode = EnergyMode.NONE;
        String property = properties.getProperty(username);
        if (property.equals(EnergyMode.CHEAP.toString())) {
            mode = EnergyMode.CHEAP;
        }
        return mode;
    }

    public String toJson(LocatedBlocks locatedBlocks, String path, String username, String remoteAddress) throws IOException {
        final Map<String, Object> m = new TreeMap<String, Object>();
        m.put("locatedBlocks", JsonUtil.toJsonString(locatedBlocks));
        m.put("path", path);
        m.put("username", username);
        m.put("remoteAddress", remoteAddress);
        return JSON.toString(m);
    }

    /**
     * loads file, TODO cache file for specific time te reduce disk io
     *
     * @return
     */
    private static Properties loadProperties(String filename) {

        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = EnergyBaseDataNodeFilter.class.getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                System.out.println("Sorry, unable to find " + filename);
                return null;
            }

            //load a properties file from class path, inside static method
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }
}
