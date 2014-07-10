package de.tuberlin.cit.project.energy.zabbix.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;

public class ZabbixHistoryDumper {
    /**
     * Normal arguments:
     * java ... hostname(known in zabbix) key type [limit] [time_from] [time_till]
     * 
     * Example: java ... CitProjectDummy2 agent.ping
     * 
     * Optional arguments:
     * java ... 
     *      -Dzabbix.restURL=https://...:100443/...
     *      -Dzabbix.username=admin
     *      -Dzabbix.password=...
     *      -Dtubit.username=...
     *      -Dtubit.password=...
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String restUrl = System.getProperty("zabbix.restURL", ZabbixParams.DEFAULT_ZABBIX_REST_URL);
        String zabbixUsername = System.getProperty("zabbix.username", ZabbixParams.DEFAULT_ZABBIX_USERNAME);
        String zabbixPassword = System.getProperty("zabbix.password", ZabbixParams.DEFAULT_ZABBIX_PASSWORD);
        String tubitUsername = System.getProperty("tubit.username", null);
        String tubitPassword = System.getProperty("tubit.password", null);

        ZabbixAPIClient apiClient = null;
        
        try {
            
            if (tubitUsername != null && tubitPassword != null) {
                apiClient = new ZabbixAPIClient(restUrl, zabbixUsername, zabbixPassword, tubitUsername, tubitPassword);
            } else {
                apiClient = new ZabbixAPIClient(restUrl, zabbixUsername, zabbixPassword);
            }
            
            /* First step: Lookup host and item id via item.get
             * More filter parameters: https://www.zabbix.com/documentation/2.2/manual/api/reference/item/get */
            ObjectNode filterItemParams = apiClient.getObjectMapper().createObjectNode(); 
            filterItemParams.put("host", (args.length > 0) ? args[0] : "CitProjectDummy2"); // default to asok08
            filterItemParams.with("search").put("key_", (args.length > 1) ? args[1] : "agent.ping");
            filterItemParams.withArray("output").add("hostid"); // optional: limit output to hostid, itemid and key_ 
            filterItemParams.withArray("output").add("itemid");
            filterItemParams.withArray("output").add("key_");
            
            int hostId = -1;
            int itemId = -1;
            System.out.println("\nitemid;hostid;key_");
            for(ZabbixItem item : apiClient.getItems(filterItemParams)) {
                System.out.println(item.getItemId() + ";" + item.getHostId() + ";" + item.getKey());
                hostId = item.getHostId();
                itemId = item.getItemId();
            }
    
            /* Second step: Find history data via history.get
             * More filter parameters: https://www.zabbix.com/documentation/2.2/manual/api/reference/history/get */
            ObjectNode filterHistoryParams = apiClient.getObjectMapper().createObjectNode();
            if (hostId > 0 && itemId > 0) {
                filterHistoryParams.put("hostids", Integer.toString(hostId));
                filterHistoryParams.put("itemids", Integer.toString(itemId));
            } else {
                filterHistoryParams.with("search").put("key_", (args.length > 1) ? args[1] : "agent.ping");
                // filterParams.put("searchWildcardsEnabled", true);
            }
            filterHistoryParams.put("history", (args.length > 2) ? args[2] : "3"); // integer
            filterHistoryParams.put("limit", (args.length > 3) ? args[3] : "100");
            if (args.length > 4)
                filterHistoryParams.put("limit_from", args[4]);
            if (args.length > 5)
                filterHistoryParams.put("limit_from", args[5]);
            filterHistoryParams.put("sortfield", "clock");
            filterHistoryParams.put("sortorder", "DESC");
    
            System.out.println("\nitemid;clock;human clock;value");
            for(ZabbixHistoryObject value : apiClient.getHistory(filterHistoryParams)) {
                System.out.println(value.getItemId() + ";" + value.getClock() 
                                    + ";" + value.getDate() + ";" + value.getValue());
            }
        
        } finally {
            apiClient.quit();
        }
    }

}
