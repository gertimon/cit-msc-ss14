package de.tuberlin.cit.project.energy.zabbix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnergyDataPusher {

	private static URL energyUrl;
	private static URLConnection connect;

	public EnergyDataPusher() {
		// TODO Auto-generated constructor stub
	}

	public double getPower(String hostname) {

		try {
			if (hostname == "CitProjectAsok05") {
				energyUrl = new URL("http://localhost:8082/login.html"); // Asok05
				//energyUrl = new URL("http://10.0.42.2:8082/login.html");
			} else if (hostname == "CitProjectOffice") {
				energyUrl = new URL("http://localhost:8081/login.html"); // OfficePC
				//energyUrl = new URL("http://10.0.42.2:8081/login.html");
			} else {
				System.out.println("Wrong hostname.");
				return 0.0;
			}
			connect = energyUrl.openConnection();
			connect.setDoOutput(true);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

		BufferedWriter writer;
		try {
			// --- Write the necessary parameters
			writer = new BufferedWriter(new OutputStreamWriter(
					connect.getOutputStream()));
			writer.write("pw=1"); // <name of formular field>=<Password>
			writer.close();

			// --- Read the page source into a string
			BufferedReader reader = new BufferedReader(new InputStreamReader(connect.getInputStream()));
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

		}
		catch (IOException ex) {
			Logger.getLogger(EnergyDataPusher.class.getName()).log(Level.SEVERE, null, ex);
		}
		return 0.0;
	}
}
