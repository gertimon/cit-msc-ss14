package de.tuberlin.cit.project.energy.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuberlin.cit.project.energy.reporting.model.PowerHistoryEntry;
import de.tuberlin.cit.project.energy.reporting.model.StorageHistoryEntry;
import de.tuberlin.cit.project.energy.reporting.model.TrafficHistoryEntry;
import de.tuberlin.cit.project.energy.reporting.model.UsageReport;
import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.naming.AuthenticationException;

/**
 * Generates reports from zabbix data.
 *
 * @author Sascha
 */
public class ReportGenerator {

    private final ZabbixAPIClient client;
    private final int datanodesHostGroupId;
    private final Map<Integer, String> hostnames;
    private final ObjectMapper objectMapper;

    public ReportGenerator() {
        try {
            this.client = new ZabbixAPIClient();
            this.datanodesHostGroupId = client.getDataNodeHostGroupId();
            Map<String, Integer> hostIds = client.getDataNodeHostIds();
            this.hostnames = new HashMap<>(hostIds.size());
            for (String hostname : hostIds.keySet()) {
                this.hostnames.put(hostIds.get(hostname), hostname);
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't get host ids from Zabbix.", e);
        }

        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates a new report.
     *
     * @param from in seconds since 1970.
     * @param to in seconds since 1970.
     * @param resolution time in seconds.
     */
    public UsageReport getReport(long from, long to, int resolution) {
        try {
            UsageReport report = new UsageReport(from, to, resolution);

            // calculate host oriented values
            addHostsPowerConsumption(report);

            // fetch user relevant information
            addUserTraffic(report);
            addUserStorage(report);
            
            // now calculate the report
            report.calculateReport();

            return report;
        } catch (Exception e) {
            throw new RuntimeException("Failure while generating report.", e);
        }
    }

    private void addHostsPowerConsumption(UsageReport report) throws AuthenticationException, KeyManagementException,
            IllegalArgumentException, NoSuchAlgorithmException, ExecutionException, IOException,
            InternalErrorException, InterruptedException {

        // lookup item id's
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("groupids", this.datanodesHostGroupId);
        params.with("search").put("key_", ZabbixParams.POWER_CONSUMPTION_KEY);
        List<ZabbixItem> powerConsumptionItems = client.getItems(params);

        if (powerConsumptionItems.size() > 0) {
            Map<Integer, String> itemHostnameMap = new HashMap<>();

            params = this.objectMapper.createObjectNode();
            params.put("output", "extend");
            params.put("history", 0); // float value
            params.put("time_from", report.getFromTime());
            params.put("time_till", report.getToTime());
            for (ZabbixItem item : powerConsumptionItems) {
                params.withArray("itemids").add(item.getItemId());
                itemHostnameMap.put(item.getItemId(), this.hostnames.get(item.getHostId()));
            }
            params.put("sortfield", "clock");
            params.put("sortorder", "ASC");

            List<ZabbixHistoryObject> historyObjects = client.getHistory(params);
            List<PowerHistoryEntry> powerUsage = new LinkedList<>();

            for (ZabbixHistoryObject h : historyObjects) {
                String hostname = itemHostnameMap.get(h.getItemId());
                long timestamp = h.getClock();
                float usedPower = h.getFloatValue();
                powerUsage.add(new PowerHistoryEntry(timestamp, hostname, usedPower));
            }
            
            Collections.sort(powerUsage);

            report.setPowerUsage(powerUsage);
        }
    }

    private String getUsernameFromKey(String keyPattern, String key) {
        return key.replaceFirst(String.format(keyPattern, "(.*)"), "$1");
    }

    private void addUserTraffic(UsageReport report) throws AuthenticationException, KeyManagementException,
            IllegalArgumentException, NoSuchAlgorithmException, ExecutionException, IOException,
            InternalErrorException, InterruptedException {

        // lookup item id's
        // unfortunately, zabbix supports search on keys only with text values, not numeric values.
        // we have to pull all items and fiter them on client side...
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("groupids", this.datanodesHostGroupId);
        List<ZabbixItem> userTrafficItems = new LinkedList<>();
        for (ZabbixItem item : client.getItems(params)) {
            if (item.getKey().matches(String.format(ZabbixParams.USER_BANDWIDTH_KEY, "*"))
                && !item.getKey().equals(String.format(ZabbixParams.USER_BANDWIDTH_KEY, "all"))
                && this.hostnames.containsKey(item.getHostId())) // no template item
            {
                userTrafficItems.add(item);
            }
        }

        if (userTrafficItems.size() > 0) {
            Map<Integer, String> itemHostnameMap = new HashMap<>();
            Map<Integer, String> itemKeyMap = new HashMap<>();

            params = this.objectMapper.createObjectNode();
            params.put("history", 0);
            params.put("time_from", report.getFromTime());
            params.put("time_till", report.getToTime());
            for (ZabbixItem item : userTrafficItems) {
                params.withArray("itemids").add(item.getItemId());
                itemHostnameMap.put(item.getItemId(), this.hostnames.get(item.getHostId()));
                itemKeyMap.put(item.getItemId(), item.getKey());
            }
            params.put("sortfield", "clock");
            params.put("sortorder", "ASC");

            List<ZabbixHistoryObject> historyObjects = client.getHistory(params);
            List<TrafficHistoryEntry> trafficUsage = new LinkedList<>();

            for (ZabbixHistoryObject h : historyObjects) {
                String username = getUsernameFromKey(ZabbixParams.USER_BANDWIDTH_KEY, itemKeyMap.get(h.getItemId()));
                String hostname = itemHostnameMap.get(h.getItemId());
                System.err.println(username);
                float usedBytes = h.getFloatValue();
                long timestamp = h.getClock();
//              System.out.println("TRAFFIC Found: " + h);
//               System.out.println("Username=" + username + ", hostname=" + hostname);

                trafficUsage.add(new TrafficHistoryEntry(timestamp, hostname, username, usedBytes));
            }
            
            Collections.sort(trafficUsage);

            report.setTrafficUsage(trafficUsage);
        }
    }

    // TODO retrieve one earlier created element too
    private void addUserStorage(UsageReport report) throws AuthenticationException, KeyManagementException,
            IllegalArgumentException, NoSuchAlgorithmException, ExecutionException, IOException,
            InternalErrorException, InterruptedException {

        // lookup item id's
        // unfortunately, zabbix supports search on keys only with text values, not numeric values.
        // we have to pull all items and fiter them on client side...
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("groupids", this.datanodesHostGroupId);
        HashMap<String, ZabbixItem> userStorageItems = new HashMap<>(); // use one key per user
        for (ZabbixItem item : client.getItems(params)) {
            if (item.getKey().matches(String.format(ZabbixParams.USER_DATA_USAGE_KEY, "*"))
                && !item.getKey().equals(String.format(ZabbixParams.USER_DATA_USAGE_KEY, "all"))
                && this.hostnames.containsKey(item.getHostId())) // no template item
            {
                userStorageItems.put(item.getKey(), item);
            }
        }

        if (userStorageItems.size() > 0) {
            Map<Integer, String> itemKeyMap = new HashMap<>();

            params = this.objectMapper.createObjectNode();
            params.put("history", 0);
            params.put("time_from", report.getFromTime());
            params.put("time_till", report.getToTime());
            for (ZabbixItem item : userStorageItems.values()) {
                params.withArray("itemids").add(item.getItemId());
                itemKeyMap.put(item.getItemId(), item.getKey());
            }
            params.put("sortfield", "clock");
            params.put("sortorder", "ASC");

            List<ZabbixHistoryObject> historyObjects = client.getHistory(params);
            
            // now fetch some initial values (last values before current period)
            params.put("time_till", report.getFromTime() - 1);
            params.remove("time_from");
            params.put("sortorder", "DESC");
            params.put("limit", 1);
            for (ZabbixItem item : userStorageItems.values()) {
                params.remove("itemids");
                params.put("itemids", item.getItemId());
                historyObjects.addAll(client.getHistory(params));
            }
            
            List<StorageHistoryEntry> storageUsage = new LinkedList<>();

            for (ZabbixHistoryObject h : historyObjects) {
                String username = getUsernameFromKey(ZabbixParams.USER_BANDWIDTH_KEY, itemKeyMap.get(h.getItemId()));
                long storageUsed = h.getLongValue();
                long timestamp = h.getClock();
//                System.out.println("STORAGE Found: " + h);
//                System.out.println("Username=" + username + ", timestamp= " + (new Date(timestamp*1000)));

                storageUsage.add(new StorageHistoryEntry(timestamp, username, storageUsed));
            }

            Collections.sort(storageUsage);

            report.setStorageUsage(storageUsage);
        }
    }

    public void quit() {
        this.client.quit();
    }

    public static void main(String[] args) throws Exception {
        ReportGenerator generator = new ReportGenerator();

        long now = Calendar.getInstance().getTimeInMillis() / 1000;
//        Calendar today = Calendar.getInstance();
//        today.set(Calendar.HOUR_OF_DAY, 0);
//        today.set(Calendar.MINUTE, 0);
//        today.set(Calendar.SECOND, 0);
//        long todaySeconds = today.getTimeInMillis() / 1000;

        double days = 0.25;

        UsageReport report = generator.getReport((long)(now - 60 * 60 * 24 * days), now, 60*60);

        generator.quit();

        System.out.println(report.toString());
    }
}
