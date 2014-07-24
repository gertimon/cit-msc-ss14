package de.tuberlin.cit.project.energy.zabbix;

import java.util.List;

import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;



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

        } else if (args.length == 2 && args[0].equals("--create")) {
            String username = args[1];
            ZabbixAPIClient apiClient = new ZabbixAPIClient();
            System.out.print("Creating user " + username + "...");
            apiClient.createUserInDataNodeTemplate(username);
            System.out.println("done.");
            apiClient.quit();

        } else if (args.length == 4 && args[0].equals("--send")) {
            ZabbixSender zabbixSender = new ZabbixSender();
            System.out.print("Setting user mapping...");
            zabbixSender.sendUserDataNodeConnection(args[1], args[2], args[3]);
            System.out.println("done.");
            zabbixSender.quit();

        } else if (args.length == 3 && args[0].equals("--get")) {
            ZabbixAPIClient apiClient = new ZabbixAPIClient();
            System.out.println("Searching for user mapping: "
                    + apiClient.getUsernameByDataNodeConnection(args[1], args[2]));
            apiClient.quit();

        } else if (args.length == 2 && args[0].equals("--delete")) {
            String username = args[1];
            ZabbixAPIClient apiClient = new ZabbixAPIClient();
            System.out.print("Removing user " + username + "...");
            List<ZabbixItem> removedItems = apiClient.deleteUserInDataNodeTemplate(username);
            System.out.println("done.");
            if (removedItems.size() > 0) {
                System.out.println("Removed items:");
                for (ZabbixItem item : removedItems)
                    System.out.println(item);
            } else
                System.out.println("No items found.");
            apiClient.quit();

        } else {
            System.err.println("Usage: ... -Dzabbix.restURL=... -Dzabbix.hostname=... -Dzabbix.port=...");
            System.err.println("\t--exists username");
            System.err.println("\t--create username");
            System.err.println("\t--send datanode username ip:port");
            System.err.println("\t--get datanode ip:port");
            System.err.println("\t--delete username");
        }
    }

}
