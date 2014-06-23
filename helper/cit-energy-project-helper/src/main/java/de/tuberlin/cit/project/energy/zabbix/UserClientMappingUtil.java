package de.tuberlin.cit.project.energy.zabbix;



/**
 * @author Sascha
 */
public class UserClientMappingUtil {
	public static void main(String[] args) throws Exception {
		if (args.length == 2 && args[0].equals("--exists")) {
			String username = args[1];
			ZabbixAPIClient apiClient = new ZabbixAPIClient();
			System.out.println("Testing existens of user " + username + ": "
					+ apiClient.doesUserExistsInDataNodeTemplate(username));
			apiClient.quit();

		} else if (args.length == 2 && args[0].equals("--add")) {
			String username = args[1];
			ZabbixAPIClient apiClient = new ZabbixAPIClient();
			System.out.print("Creating user " + username + "...");
			apiClient.createUserInDataNodeTemplate(username);
			System.out.println("done.");
			apiClient.quit();
	
		} else if (args.length == 5 && args[0].equals("--send")) {
			ZabbixSender zabbixSender = new ZabbixSender();
			System.out.print("Setting user mapping...");
			zabbixSender.sendUserDataNodeConnection(args[1], args[2], args[3], Integer.parseInt(args[4]));
			System.out.println("done.");
			zabbixSender.quit();

		} else if (args.length == 4 && args[0].equals("--get")) {
			ZabbixAPIClient apiClient = new ZabbixAPIClient();
			System.out.println("Searching for user mapping: "
					+ apiClient.getUsernameByDataNodeConnection(args[1], args[2], args[3]));
			apiClient.quit();

		} else {
			System.err.println("Usage: ... -Dzabbix.restURL=... -Dzabbix.hostname=... -Dzabbix.port=...");
			System.err.println("\t--exists username");
			System.err.println("\t--add username");
			System.err.println("\t--send datanode username ip port");
			System.err.println("\t--get datanode ip port");
		}
	}

}
