package de.tuberlin.cit.project.energy.zabbix;

import java.util.List;

import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;

/**
 * User managment Zabbix API Client (add/drop HDFS users in Zabbix templates).
 * 
 * @author Sascha
 */
public class ZabbixUserMappingUtil {
    public static void main(String[] args) throws Exception {
        ZabbixAPIClient apiClient = null;
        ZabbixSender sender = null;

        try {
            if (args.length == 2 && args[0].equals("--exists")) {
                String username = args[1];
                apiClient = new ZabbixAPIClient();
                System.out.println("Testing existens of user " + username + ": "
                        + apiClient.doesUserExistsInDataNodeTemplate(username));

            } else if (args.length == 2 && args[0].equals("--create")) {
                String username = args[1];
                apiClient = new ZabbixAPIClient();
                System.out.print("Creating user " + username + "...");
                apiClient.createUserInDataNodeTemplate(username);
                System.out.println("done.");

            } else if (args.length == 4 && args[0].equals("--send")) {
                sender = new ZabbixSender();
                System.out.print("Setting user mapping...");
                sender.sendUserDataNodeConnection(args[1], args[2], args[3]);
                System.out.println("done.");

            } else if (args.length == 3 && args[0].equals("--get")) {
                apiClient = new ZabbixAPIClient();
                System.out.println("Searching for user mapping: "
                        + apiClient.getUsernameByDataNodeConnection(args[1], args[2]));

            } else if (args.length == 2 && args[0].equals("--delete")) {
                String username = args[1];
                apiClient = new ZabbixAPIClient();
                System.out.print("Removing user " + username + "...");
                List<ZabbixItem> removedItems = apiClient.deleteUserInDataNodeTemplate(username);
                System.out.println("done.");
                if (removedItems.size() > 0) {
                    System.out.println("Removed items:");
                    for (ZabbixItem item : removedItems)
                        System.out.println(item);
                } else
                    System.out.println("No items found.");

            } else {
                System.err.println("Usage: ... -Dzabbix.restURL=... -Dzabbix.hostname=... -Dzabbix.port=...");
                System.err.println("\t--exists username");
                System.err.println("\t--create username");
                System.err.println("\t--send datanode username ip:port");
                System.err.println("\t--get datanode ip:port");
                System.err.println("\t--delete username");
            }

        } finally {
            if (apiClient != null)
                apiClient.quit();

            if (sender != null)
                sender.quit();
        }
    }

}
