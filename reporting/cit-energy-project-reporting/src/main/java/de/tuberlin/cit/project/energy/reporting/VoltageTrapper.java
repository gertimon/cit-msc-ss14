package de.tuberlin.cit.project.energy.reporting;

import java.util.Random;

/**
 * Class for generating and sending dummy voltage data(0-400) every 30 seconds
 * to the monitor as an external thread.
 */
public class VoltageTrapper extends Thread {

    private String zabbixMonitorAdress = "";
    private String hostName = "";
    private String itemKeyName = "";
    private boolean continueSending = true;
    private Random rm = null;

    /**
     * Constructor for dummy voltage sender.
     *
     * @param zabbixServerIpAdress Zabbix monitor IP Adress or DNS name.
     * @param hostname	Registered hostname within zabbix.(Not IP adress or DNS
     * name)
     * @param itemKey Itemkey name of the metric.
     */
    public VoltageTrapper(String zabbixServerIpAdress, String hostname,
        String itemKey) {
        this.zabbixMonitorAdress = zabbixServerIpAdress;
        this.hostName = hostname;
        this.itemKeyName = itemKey;
        rm = new Random();
        //System.out.println("init done");

    }

    @Override
    public void run() {

        while (continueSending) {
            //send dummy/random metric data to monitor
            // TODO implement send metric variant inside helper
//            ZabbixHelper.getZabbixHelper().sendMetric(zabbixMonitorAdress, hostName, itemKeyName, true, rm.nextInt(400));
//			System.out.println(System.currentTimeMillis());
            //wait 30 seconds before another sending attempt
            try {
                this.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//		System.out.println("Sending stopped");
    }

    /**
     * Method to signal the sender to halt on next sending attempt.
     */
    public void haltExecution() {
        this.continueSending = false;
//		System.out.println("sending will be halted");
    }

}
