package de.tuberlin.cit.project.energy.zabbix;


/**
 * @author Sascha
 */
public class UserClientMappingUtil {

	/**
	 * @param args currentIP [username]
	 * 
	 * Example:
	 * java ... -Dzabbix.restURL=https://localhost:10443/zabbix/api_jsonrpc.php 12.34.56.78
	 * java ... -Dzabbix.hostname=localhost 12.34.56.78 max
	 * java ... -Dzabbix.restURL=https://localhost:10443/zabbix/api_jsonrpc.php 12.34.56.78
	 */
	public static void main(String[] args) throws Exception {		
		if (args.length == 1) {
			ZabbixAPIClient apiClient = new ZabbixAPIClient();

			String username = apiClient.getUsernameByIP(args[0]);
			System.out.println("Got username " + username + " at ip " + args[0] + ".");
		    apiClient.quit();

		} else if (args.length == 2) {
			System.out.println("Sending " + args[0] + " as current ip of " + args[1]);
			ZabbixSender zabbixSender = new ZabbixSender();
			zabbixSender.sendLastUserIP(args[1], args[0]);
			zabbixSender.quit();
		} else {
			System.err.println("Usage: ... currentIP [username]");
		}
	}

}
