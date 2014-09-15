package de.tuberlin.cit.project.energy.zabbix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class EnergyDataPusher {
    private final static Logger LOG = Logger.getLogger(EnergyDataPusher.class.getName());

    public static final int SIMULATING_INTERVAL = 2; // interval in seconds

    private final static HashMap<String, String> ENDPOINTS = new HashMap<String, String>();
    static {
        ENDPOINTS.put("CitProjectAsok05", "http://10.0.42.2:8082/login.html");
        ENDPOINTS.put("CitProjectOffice", "http://10.0.42.2:8081/login.html");
    }

	public static double getPower(String hostname, URL energyUrl) throws IOException {
	    URLConnection connection = energyUrl.openConnection();
		connection.setDoOutput(true);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
		writer.write("pw=1"); // <name of formular field>=<Password>
		writer.close();

		// --- Read the page source into a string
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		double currPower = 0.0;
		String htmlSrc = "";

		while ((htmlSrc = reader.readLine()) != null) {
			// --- Split html source with ';' as delimiter
			for (String xtract : htmlSrc.split(";")) {
				// --- Search for the character sequence "var P" - this is
				// "var P" current Power in the html source
				if (xtract.contains("var P")) {
					reader.close();
					// --- ...and delete non-digits with this regexp
					currPower = Double.parseDouble(xtract.replaceAll("\\D+", ""));
					currPower /= 466;
					// --- Two decimal places are enough
					return Math.round(currPower * 100.0) / 100.0;
				}
			}
		}
		
		return 0.0;
	}
	
	public static void main(String[] args) throws InterruptedException {
        ZabbixSender zabbixSender = new ZabbixSender();

        while(true) {
            for (String hostname : ENDPOINTS.keySet()) {
                try {
                    URL energyUrl = new URL(ENDPOINTS.get(hostname));
                    double currentPowerUsage = getPower(hostname, energyUrl);
                    zabbixSender.sendPowerConsumption(hostname, currentPowerUsage);
                    LOG.debug("Found " + currentPowerUsage + "W on host " + hostname + ".");
                    
                } catch (IOException ex) {
                    LOG.warn("Failed to fetch energy data: " + ex.getMessage(), ex);
                    ex.printStackTrace();
                }
            }
            
            Thread.sleep(SIMULATING_INTERVAL * 1000);
        }
    }
}
