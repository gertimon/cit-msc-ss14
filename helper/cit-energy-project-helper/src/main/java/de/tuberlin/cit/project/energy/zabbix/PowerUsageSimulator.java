package de.tuberlin.cit.project.energy.zabbix;

import java.util.Random;

/**
 * This simple simulator sends randomized power consumption data to zabbix.
 *
 * @author Sascha
 */
public class PowerUsageSimulator {

    public static final int SIMULATING_INTERVAL = 2; // interval in seconds
    public static final String[] DEFAULT_HOSTS = new String[]{"CitProjectOffice", "CitProjectAsok05"};

    /**
     * @param args list of hostnames
     */
    public static void main(String[] args) throws Exception {
        ZabbixSender zabbixSender = new ZabbixSender();
        String hosts[] = DEFAULT_HOSTS;

        if (args.length > 0) {
            hosts = args;
        }

        System.out.println("Simulating server power consumption every " + SIMULATING_INTERVAL + " seconds.");
        EnergyDataPusher pusher = new EnergyDataPusher();

        while(true) {
            for(String hostname : hosts) {
                //double powerConsumption = 200 + rand.nextInt(100);
            	
            	double powerConsumption = pusher.getPower();
                System.out.println("Host " + hostname + " consumed " + powerConsumption + "W.");
                zabbixSender.sendPowerConsumption(hostname, powerConsumption);
            }
            Thread.sleep(SIMULATING_INTERVAL * 1000);
        }
    }
}
