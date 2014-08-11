package de.tuberlin.cit.project.energy.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.tuberlin.cit.project.energy.reporting.model.Power;
import de.tuberlin.cit.project.energy.reporting.model.UserReport;
import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
     * @param username
     * @param from in seconds since 1970.
     * @param to in seconds since 1970.
     */
    public UserReport getUserReport(String username, long from, long to) {
        try {
            UserReport report = new UserReport(username, from, to);

            // calculate host oriented values
            addHostsPowerConsumption(report);

            // fetch user relevant information
            addUserTraffic(report);
            addUserStorage(report);

            return report;
        } catch (Exception e) {
            throw new RuntimeException("Failure while generating report.", e);
        }
    }

    private void addHostsPowerConsumption(UserReport report) throws AuthenticationException, KeyManagementException,
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
            params.put("history", 3);
            params.put("time_from", report.getFromTime());
            params.put("time_till", report.getToTime());
            for (ZabbixItem item : powerConsumptionItems) {
                params.withArray("itemids").add(item.getItemId());
                itemHostnameMap.put(item.getItemId(), this.hostnames.get(item.getHostId()));
            }
            params.put("sortfield", "clock");
            params.put("sortorder", "ASC");

            List<ZabbixHistoryObject> historyObjects = client.getHistory(params);
            HashMap<String, Power> powerConsumptionWatt = new HashMap<>(this.hostnames.size());

            for (ZabbixHistoryObject h : historyObjects) {
                String hostname = itemHostnameMap.get(h.getItemId());

                Power hostConsumption = powerConsumptionWatt.get(hostname);
                if (hostConsumption == null) {
                    hostConsumption = new Power();
                    powerConsumptionWatt.put(hostname, hostConsumption);
                }

                hostConsumption.addValue(h.getClock(), h.getIntValue());
            }

            HashMap<String, Double> powerConsumptionKWh = new HashMap<>(powerConsumptionWatt.size());

            for (String hostname : powerConsumptionWatt.keySet()) {
                Power p = powerConsumptionWatt.get(hostname);
                double ws = Power.getPowerAsWattSeconds(p.getPowerValues(), report.getFromTime(), report.getToTime());
                double kwh = Power.wsToKwh(ws);
                powerConsumptionKWh.put(hostname, kwh);
            }

            report.setPower(powerConsumptionKWh);
        }
    }

    private String getUsernameFromKey(String keyPattern, String key) {
        return key.replaceFirst(String.format(keyPattern, "(.*)"), "$1");
    }

    private void addUserTraffic(UserReport report) throws AuthenticationException, KeyManagementException,
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
            params.put("history", 3);
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

            String reportUser = report.getUsername();

            for (ZabbixHistoryObject h : historyObjects) {
                String username = getUsernameFromKey(ZabbixParams.USER_BANDWIDTH_KEY, itemKeyMap.get(h.getItemId()));
                String hostname = itemHostnameMap.get(h.getItemId());
                int bandwidth = h.getIntValue();
                long clock = h.getClock();
                System.out.println("TRAFFIC Found: " + h);
                System.out.println("Username=" + username + ", hostname=" + hostname);
//                if (username.equals(reportUser)) {
//                    report.addUserStorage(clock, new Long(h.getValue()));
//                }
            }
        }
    }

    private void addUserStorage(UserReport report) throws AuthenticationException, KeyManagementException,
        IllegalArgumentException, NoSuchAlgorithmException, ExecutionException, IOException,
        InternalErrorException, InterruptedException {
// TODO retrieve one earlier created element too
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
            params.put("history", 3);
            params.put("time_from", report.getFromTime());
            params.put("time_till", report.getToTime());
            for (ZabbixItem item : userStorageItems.values()) {
                params.withArray("itemids").add(item.getItemId());
                itemKeyMap.put(item.getItemId(), item.getKey());
            }
            params.put("sortfield", "clock");
            params.put("sortorder", "ASC");

            List<ZabbixHistoryObject> historyObjects = client.getHistory(params);
            String reportUser = report.getUsername();
            TreeMap<Long, Double> userStorage = new TreeMap<>();
            for (ZabbixHistoryObject h : historyObjects) {
                String username = getUsernameFromKey(ZabbixParams.USER_BANDWIDTH_KEY, itemKeyMap.get(h.getItemId()));
                int storageUsed = h.getIntValue();
                long clock = h.getClock();
                System.out.println("STORAGE Found: " + h);
                System.out.println("Username=" + username);
                if (username.matches("user\\." + reportUser + "\\.dataUsage")) {
                    Double value = new Double(h.getValue());
                    userStorage.put(clock, value);
                }
            }
            report.setUserStorage(userStorage);
        }
    }

    public void quit() {
        this.client.quit();
    }

    public static void main(String[] args) throws Exception {
        ReportGenerator generator = new ReportGenerator();

        long now = (new Date()).getTime() / 1000;
        UserReport report = generator.getUserReport("mpjss14", now - 60 * 60 * 24 * 31, now);

        for (String hostname : report.getPower().keySet()) {
            System.out.println("Host " + hostname + " used " + report.getPower().get(hostname) + " KWh.");
        }

        generator.quit();

        System.out.println(report.toString());
    }
}
