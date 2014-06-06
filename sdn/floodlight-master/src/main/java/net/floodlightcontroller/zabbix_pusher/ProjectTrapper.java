package net.floodlightcontroller.zabbix_pusher;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class for manually pushing metric data into a 
 * zabbix server using zabbix_sender commandline prompts.
 * (created for Distributed Systems Project SoSe 2014 CIT TU Berlin)
 */
public class ProjectTrapper{
		
	String command ="";
	Process p = null;
	
	String jsonMsg;
	OutputStream zabbixMsg;
	
	/**
	 * Method for pushing desired metric information into specified zabbix monitor
	 * 
	 * @param zabbixServerIpAdress IP adress or DNS name of the zabbix monitor.
	 * @param hostname Registered hostname within zabbix monitor (Warning: Not IP adress or DNS name). Also the name of the server to which the specified information should be assigned to.
	 * @param itemKey Name of the item key for the metric information.(Casesensitive)
	 * @param isKeyNumeric Boolean parameter for determining whether the numeric value is an integer or a character. (true = value is numeric, false = value is a character)
	 * @param Value Metric information that is going to be sent to the monitor.
	 */
	public void sendMetric(String zabbixServerIpAdress, String hostname,
			String itemKey,boolean isKeyNumeric, Object Value) {
		
		//Constructing command for execution
		String command = "zabbix_sender -z " + zabbixServerIpAdress  +
				" -s " + hostname +
				" -k " +itemKey;
		if(isKeyNumeric){
			command += " -o " + Value.toString();
		}
		else{
			//TODO: Problem fixen mit dem Senden von Nachrichten mit Leerzeichen
			command += " -o \'" + (String)Value + "\'";
		}
		
		System.out.println(command);
		
		p = null;
		
		//executing the commandline via Runtime
		try{
			p = Runtime.getRuntime().exec(command);
//			p.waitFor();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//resetting the command
		command = "";
		
	}
	
	public void sendMetricJson (String zabbixServerIpAdress, String hostname,
	String itemKey,boolean isKeyNumeric, String value) throws IOException {
		
		// --- prepare JSON string
		jsonMsg = "{"
		        + "\"request\":\"sender data\",\n"
		        + "\"data\":[\n"
		        +        "{\n"
		        +                "\"host\":\"" + hostname + "\",\n"
		        +                "\"key\":\"" + itemKey + "\",\n"
		        +                "\"value\":\"" + value.replace("\\", "\\\\") + "\"}]}\n" ;
		
		System.out.println("\nJSON String output for debugging purposes:\n" +jsonMsg+ "\n");
		
		// --- write JSON string to a zabbix message
		byte[] zabbixData = jsonMsg.getBytes();
		int lengthZabbixMsg = zabbixData.length;
		
		zabbixMsg.write(new byte[] {
			'Z', 'B', 'X', 'D', 
			'\1',
			(byte)(lengthZabbixMsg & 0xFF), 
			(byte)((lengthZabbixMsg >> 8) & 0x00FF), 
			(byte)((lengthZabbixMsg >> 16) & 0x0000FF), 
			(byte)((lengthZabbixMsg >> 24) & 0x000000FF),
			'\0','\0','\0','\0'});
		
		zabbixMsg.write(zabbixData);
	
	}	
	
}
		

