package de.tuberlin.cit.project.energy.reporting.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents usage values on a given time frame (3600s = 1h).
 *
 * @author Sascha
 */
public class UsageTimeFrame {
    public final static HashMap<String, Integer> IDLE_POWERS = new HashMap<>();
    static {
        IDLE_POWERS.put("CitProjectAsok05", 400);
        IDLE_POWERS.put("CitProjectOffice", 75);
    }

    /** Frame start time in seconds since 1.1.1970. */
    private final long startTime;
    /** Interval size in seconds. */
    private final long frameDuration;

    // history entries from zabbix
    private final List<PowerHistoryEntry> powerUsageHistory;
    private final List<StorageHistoryEntry> storageUsageHistory;
    private final List<TrafficHistoryEntry> trafficUsageHistory;

    /**
     * Contains a username <-> initial storage mapping. If storageUsage list
     * contains a username, this map contains at an entry too.
     *
     * => It's safe to iterate over usernames in storageUsage an pull an initial value from this map.
     */
    private HashMap<String, Long> initialStorageEntries;

    // calculated statistics
    private HashMap<String, float[]> powerUsageByHost;
    private HashMap<String, ServerTraffic> trafficByHost;
    private HashMap<String, long[]> storageByUser;

    // sums
    private HashMap<String, Float> powerUsageByHostSum;
    private HashMap<String, Float> powerUsageByUserSum;
    private float powerUsageByTypeSum[] = new float[2]; // 0=idle, 1=rest
    private HashMap<String, Float> storageUsageByUserSum;
    private HashMap<String, Float> trafficUsageByUserSum;


    public UsageTimeFrame(long startTime, long frameDuration) {
        this.startTime = startTime;
        this.frameDuration = frameDuration;

        this.powerUsageHistory = new LinkedList<>();
        this.storageUsageHistory = new LinkedList<>();
        this.trafficUsageHistory = new LinkedList<>();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return this.startTime + this.frameDuration - 1;
    }

    public long getDurationInSeconds() {
        return frameDuration;
    }

    public void addPowerUsage(PowerHistoryEntry powerEntry) {
        this.powerUsageHistory.add(powerEntry);
    }

    public void addStorageUsage(StorageHistoryEntry storageEntry) {
        this.storageUsageHistory.add(storageEntry);

        if (!this.initialStorageEntries.containsKey(storageEntry.getUsername()))
            this.initialStorageEntries.put(storageEntry.getUsername(), 0l);
    }

    public void setInitialStorageEntries(HashMap<String, Long> initialStorageEntries) {
        this.initialStorageEntries = initialStorageEntries;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Long> getLastStorageEntries() {
        HashMap<String, Long> result = (HashMap<String, Long>) this.initialStorageEntries.clone();

        for (StorageHistoryEntry entry : this.storageUsageHistory)
            result.put(entry.getUsername(), entry.getUsedBytes());

        return result;
    }

    public void addTrafficUsage(TrafficHistoryEntry trafficEntry) {
        this.trafficUsageHistory.add(trafficEntry);
    }

    public void addUsageEntry(HistoryEntry entry) {
        if (entry instanceof PowerHistoryEntry) {
            this.addPowerUsage((PowerHistoryEntry) entry);
        } else if (entry instanceof StorageHistoryEntry) {
            this.addStorageUsage((StorageHistoryEntry) entry);
        } else if (entry instanceof TrafficHistoryEntry) {
            this.addTrafficUsage((TrafficHistoryEntry) entry);
        }
    }

    /**
     * After adding history entries, this method does the final calculation step.
     */
    public void calculateSummary() {
        this.powerUsageByHost = generatePowerMap();
        this.trafficByHost = generateUserTrafficMap();
        this.storageByUser = generateUserStorageMap();
        calculateUsageSums();
    }

    /** Generates an user -> storage mapping. */
    private HashMap<String, long[]> generateUserStorageMap() {
        HashMap<String, long[]> userStorage = new HashMap<String, long[]>();

        for (StorageHistoryEntry entry : this.storageUsageHistory) {
            long[] storageValues = userStorage.get(entry.getUsername());

            if (storageValues == null) {
                storageValues = new long[3600];
                Arrays.fill(storageValues, -1);
                userStorage.put(entry.getUsername(), storageValues);
                storageValues[0] = this.initialStorageEntries.get(entry.getUsername());
            }

            int offset = (int) (entry.getTimestamp() - this.startTime);
            storageValues[offset] = entry.getUsedBytes();
        }

        // fill empty values with previous / initial values
        for (String username : this.initialStorageEntries.keySet()) {
            if (userStorage.containsKey(username)) {
                long storageValues[] = userStorage.get(username);
                long lastValue = storageValues[0];
                for (int i = 1; i < storageValues.length; i++) {
                    if (storageValues[i] == -1)
                        storageValues[i] = lastValue;
                    else
                        lastValue = storageValues[i];
                }

            } else {
                long[] storageValues = new long[3600];
                Arrays.fill(storageValues, this.initialStorageEntries.get(username));
                userStorage.put(username, storageValues);
            }
        }

        return userStorage;
    }

    /** Generates a server -> user -> traffic mapping. */
    private HashMap<String, ServerTraffic> generateUserTrafficMap() {
        HashMap<String, ServerTraffic> trafficByHost = new HashMap<>();
        HashMap<String, TrafficHistoryEntry> lastEntry = new HashMap<>();

        // merge entries
        Iterator<TrafficHistoryEntry> it = this.trafficUsageHistory.iterator();
        while(it.hasNext()) {
            TrafficHistoryEntry current = it.next();
            if (lastEntry.containsKey(current.getHostname()) && lastEntry.get(current.getHostname()).equals(current)) {
                lastEntry.get(current.getHostname()).addUsedBytes(current);
                it.remove();
            } else
                lastEntry.put(current.getHostname(), current);
        }

        for (TrafficHistoryEntry entry : this.trafficUsageHistory) {
            ServerTraffic serverTraffic = trafficByHost.get(entry.getHostname());

            if (serverTraffic == null) {
                serverTraffic = new ServerTraffic(this);
                trafficByHost.put(entry.getHostname(), serverTraffic);
            }

            serverTraffic.addEntry(entry);
        }

        return trafficByHost;
    }

    /** Generates a server -> power usage mapping. */
    private HashMap<String, float[]> generatePowerMap() {
        HashMap<String, float[]> dataNodesPowers = new HashMap<>();

        for (PowerHistoryEntry entry : this.powerUsageHistory) {
            float hostPower[] = dataNodesPowers.get(entry.getHostname());

            if (hostPower == null) {
                hostPower = new float[3600];
                Arrays.fill(hostPower, -1);
                dataNodesPowers.put(entry.getHostname(), hostPower);
                hostPower[0] = entry.getUsedPower(); // initial value
            }

            int offset = (int) (entry.getTimestamp() - this.startTime);
            hostPower[offset] = entry.getUsedPower();
        }

        // find initial values and fill empty values with previous values
        for (String hostname : dataNodesPowers.keySet()) {
            float hostPower[] = dataNodesPowers.get(hostname);
            float lastValue = hostPower[0];
            for (int i = 1; i < hostPower.length; i++) {
                if (hostPower[i] == -1)
                    hostPower[i] = lastValue;
                else
                    lastValue = hostPower[i];
            }
        }

        return dataNodesPowers;
    }

    /** Generates the final summary statistics */
    private void calculateUsageSums() {
        String user[] = this.storageByUser.keySet().toArray(new String[]{});
        String hosts[] = this.powerUsageByHost.keySet().toArray(new String[]{});
        
        float powerUsageByUserSum[] = new float[user.length];
        float powerUsageByHostSum[] = new float[hosts.length];
        float powerUsageByTypeSum[] = new float[2]; // 0=idle, 1=rest
        float storageUsageByUserSum[] = new float[user.length];
        float trafficUsageByUserSum[] = new float[user.length];

        float powerByHost[][] = new float[hosts.length][3600];
        for (int h = 0; h < hosts.length; h++)
            powerByHost[h] = this.powerUsageByHost.get(hosts[h]);

        long storageByUser[][] = new long[user.length][3600];
        for (int u = 0; u < user.length; u++)
            storageByUser[u] = this.storageByUser.get(user[u]);

        long storageUsageSum[] = sum(storageByUser);

        for (int h = 0; h < hosts.length; h++) {
            float idle = IDLE_POWERS.get(hosts[h]);
            double usageByUserOnHost[] = new double[user.length];

            ServerTraffic traffic = null;
            float trafficSum[] = null;

            if (this.trafficByHost.containsKey(hosts[h])) {
                traffic = this.trafficByHost.get(hosts[h]);
                trafficSum = traffic.getSum();
            }

            for (int t = 0; t < 3600; t++) {
                float currentIdle = Math.min(powerByHost[h][t], idle);
                float currentRest = powerByHost[h][t] - currentIdle;

                // idle / storage
                if (storageUsageSum[t] > 0) {
                    for (int u = 0; u < user.length; u++) {
                        if (storageByUser[u][t] > 0) {
                            usageByUserOnHost[u] += (1.0 * storageByUser[u][t] / storageUsageSum[t]) * currentIdle;
                            storageUsageByUserSum[u] += storageByUser[u][t];
                        }
                    }
                }

                // rest / traffic
                if (traffic != null && currentRest > 0 && trafficSum[t] > 0) {
                    for (int u = 0; u < user.length; u++) {
                        if (traffic.getUserTraffic().containsKey(user[u]) &&
                                    traffic.getUserTraffic().get(user[u])[t] > 0) {

                                usageByUserOnHost[u] += (traffic.getUserTraffic().get(user[u])[t] / trafficSum[t]) * currentRest;
                                trafficUsageByUserSum[u] += traffic.getUserTraffic().get(user[u])[t];
                        }
                    }
                }

                powerUsageByHostSum[h] += powerByHost[h][t];
                powerUsageByTypeSum[0] += currentIdle;
                powerUsageByTypeSum[1] += currentRest;
            }

            for (int u = 0; u < user.length; u++) {
                powerUsageByUserSum[u] += usageByUserOnHost[u];
            }
        }

        // map results
        this.powerUsageByHostSum = new HashMap<>();
        for (int h = 0; h < hosts.length; h++)
            this.powerUsageByHostSum.put(hosts[h], powerUsageByHostSum[h] / (3600 * 1000)); // KWh
        this.powerUsageByUserSum = new HashMap<>();
        for (int u = 0; u < user.length; u++)
            this.powerUsageByUserSum.put(user[u], powerUsageByUserSum[u] / (3600 * 1000)); // KWh
        this.powerUsageByTypeSum = new float[powerUsageByTypeSum.length];
        for (int t = 0; t < 2; t++)
            this.powerUsageByTypeSum[t] = powerUsageByTypeSum[t] / (3600 * 1000); // KWh
        this.storageUsageByUserSum = new HashMap<>();
        for (int u = 0; u < user.length; u++)
            this.storageUsageByUserSum.put(user[u], storageUsageByUserSum[u] / 3600); // AVG
        this.trafficUsageByUserSum = new HashMap<>();
        for (int u = 0; u < user.length; u++)
            this.trafficUsageByUserSum.put(user[u], trafficUsageByUserSum[u]);        
    }

    private long[] sum(long values[][]) {
        long[] result = new long[values[0].length];
        Arrays.fill(result, 0);
        for (int i = 0; i < values.length; i++) {
            for (int t = 0; t < values[i].length; t++)
                result[t] += values[i][t];
        }
        return result;
    }

    public ObjectNode toJson(ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("startTime", this.startTime);
        result.put("endTime", this.startTime + this.frameDuration - 1);

        for (String hostname : this.powerUsageByHostSum.keySet())
            result.with("powerUsageByHost").put(hostname, this.powerUsageByHostSum.get(hostname));
        for (String user : this.powerUsageByUserSum.keySet())
            result.with("powerUsageByUser").put(user, this.powerUsageByUserSum.get(user));
        result.with("powerUsageByType").put("idle", this.powerUsageByTypeSum[0]);
        result.with("powerUsageByType").put("rest", this.powerUsageByTypeSum[1]);
        for (String user : this.storageUsageByUserSum.keySet())
            result.with("storageUsageByUser").put(user, this.storageUsageByUserSum.get(user));
        for (String user : this.trafficUsageByUserSum.keySet())
            result.with("trafficUsageByUser").put(user, this.trafficUsageByUserSum.get(user));

        // some debug informations
        // result.with("statistic").with("count").put("power", this.powerUsageHistory.size());
        // result.with("statistic").with("count").put("storage", this.storageUsageHistory.size());
        // result.with("statistic").with("count").put("initStorageEntries", this.initialStorageEntries.size());
        // result.with("statistic").with("count").put("traffic", this.trafficUsageHistory.size());

        return result;
    }
}
