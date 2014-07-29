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
    
    public double getPower() {
        
        try {
        	// TODO: change URL with livesystem energy meter
            energyUrl = new URL("http://localhost:8082/login.html"); // Uni Livesystem
        	//energyUrl = new URL("http://192.168.100.116/login.html");
            connect = energyUrl.openConnection();
            connect.setDoOutput(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        BufferedWriter writer;
        try {
            // --- Write the necessary parameters
            writer = new BufferedWriter(new OutputStreamWriter(connect.getOutputStream()));
            writer.write("pw=1"); // <name of formular field>=<Password>
            writer.close();
            
            // --- Read the page source into a string
            BufferedReader reader = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            double currPower = 0.0;
            String htmlSrc = "";
            
            while ((htmlSrc = reader.readLine()) != null) {
                // --- Split html source with ';' as delimiter
                for (String xtract : htmlSrc.split(";")) {
                    // --- Search for the character sequence "var P" - this is the current Power in the html source
                    if (xtract.contains("var P")) {
                        reader.close();
                        // --- ...and delete non-digits with this regexp
                        currPower = Double.parseDouble(xtract.replaceAll("\\D+",""));
                        currPower /= 466;
                        DecimalFormat DecForm =  new DecimalFormat("#.#");
                        return Double.valueOf(DecForm.format(currPower));
                    }
                }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(EnergyDataPusher.class.getName()).log(Level.SEVERE, null, ex);
        }
		return 0.0;
        
    }
    
    /*
    public String getPower() {
    	
    	// --- Connect and Login to the webinterface
    	try {
			Connection.Response res =
					Jsoup.connect("http://192.168.100.116/login.html")
					.data("pw", "1")
					.method(Method.POST)
					.execute();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	
    	
    	
    	// --- extracting power data data from HTML
    	try {
			Document doc = Jsoup.connect("http://192.168.100.116/energenie.html").get();
			String power = doc.getElementById("pC").html();
			//String power = doc.getElementsByClass("ig").html(); // For testing purposes
			System.out.println("Data: " +power);
			return power;
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	
    	return "";
    	
    }*/
    
	
	
	
	
	
}
