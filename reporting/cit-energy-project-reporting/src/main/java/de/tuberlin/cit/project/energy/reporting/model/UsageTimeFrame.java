package de.tuberlin.cit.project.energy.reporting.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuberlin.cit.project.energy.reporting.Properties;

/**
 * Represents usage values on a given time frame.
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
    private Map<String, Double> powerUsageByHostSum;
    private HashMap<String, float[]> powerUsageByHost;
    private HashMap<String, ServerTraffic> trafficByHost;
    private HashMap<String, long[]> storageByUser;
    private HashMap<String, BillForAllServers> billByUser;

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
        this.powerUsageByHostSum = calculatePowerUsageByHostSum();
        this.powerUsageByHost = generatePowerMap();
        this.trafficByHost = generateUserTrafficMap();
        this.storageByUser = generateUserStorageMap();
        this.billByUser = calculateUserBills();
    }

    public HashMap<String, BillForAllServers> getBillByUser() {
        return billByUser;
    }

    /** Calculates the power usage in KWh per host */
    private Map<String, Double> calculatePowerUsageByHostSum() {
        HashMap<String, Double> powerUsageByHost = new HashMap<>();
        HashMap<String, Long> lastTimestamp = new HashMap<>();

        for (PowerHistoryEntry entry : this.powerUsageHistory) {
            if (powerUsageByHost.containsKey(entry.getHostname())) {
                long offset = entry.getTimestamp() - lastTimestamp.get(entry.getHostname());
                double lastValue = powerUsageByHost.get(entry.getHostname());
                powerUsageByHost.put(entry.getHostname(), lastValue + offset * entry.getUsedPower());
                lastTimestamp.put(entry.getHostname(), entry.getTimestamp());

            } else {
                powerUsageByHost.put(entry.getHostname(), 0d);
                lastTimestamp.put(entry.getHostname(), entry.getTimestamp());
            }
        }

        // calculate KWh
        for (String hostname : powerUsageByHost.keySet()) {
            powerUsageByHost.put(hostname, powerUsageByHost.get(hostname) / (3600.0 * 1000));
        }

        return powerUsageByHost;
    }

    /** Generates an user -> traffic mapping. */
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

    private HashMap<String, BillForAllServers> calculateUserBills() {
        HashMap<String, BillForAllServers> billforUserOfServers = new HashMap<>();

        for (String username : this.storageByUser.keySet()) {
            BillForAllServers bills = computeBill(
                    this.powerUsageByHost, this.trafficByHost, this.storageByUser, username);
            bills.setStartTime(this.startTime);
            bills.setEndTime(getEndTime());
            billforUserOfServers.put(username, bills);
        }

        return billforUserOfServers;
    }

    private BillForAllServers computeBill(HashMap<String, float[]> serverPowers,
            HashMap<String, ServerTraffic> usersTraffic, HashMap<String, long[]> userStorage, String user) {

        long[] userStore = userStorage.get(user);
        if (userStore == null) {
            userStore = new long[3600];
            Arrays.fill(userStore, 0);
        }
        List<Bill> userBillsForServer = new LinkedList<>();
        for (String serverName : usersTraffic.keySet()) {
            ServerTraffic traffic = usersTraffic.get(serverName);
            float[] userTrafficOFServer = traffic.getUserTraffic().get(user);
            if (userTrafficOFServer == null) {
                userTrafficOFServer = new float[3600];
                Arrays.fill(userTrafficOFServer, 0);
            }
            Bill serverBill = computePrice(serverName, serverPowers.get(serverName), IDLE_POWERS.get(serverName),
                    userTrafficOFServer, userStore, traffic.getUserTraffic(), userStorage, user);
            userBillsForServer.add(serverBill);
        }
        BillForAllServers compBill = new BillForAllServers(userBillsForServer);
        return compBill;
    }

    private Bill computePrice(String serverName, float[] server, int idlePower, float[] userTrafficForServer,
            long[] userStore, HashMap<String, float[]> usersTrafficForServer, HashMap<String, long[]> usersStorage,
            String user) {

        float[] pricePart = new float[3600];
        double allTraffic = 0;
        double allStorage = 0;
        for (int i = 0; i < 3600; i++) {
            if (userTrafficForServer[i] == 0) {
                Set<String> keys = usersStorage.keySet();
                long userPart = userStore[i];
                long rest = 0;
                for (String k : keys) {
                    rest += usersStorage.get(k)[i];
                }
                pricePart[i] = (userPart / rest) * idlePower;
                allStorage += rest;
            } else {
                Set<String> keys = usersStorage.keySet();
                long userPart = userStore[i];
                long rest = 0;
                for (String k : keys) {
                    rest += usersStorage.get(k)[i];
                    allStorage += rest;
                }
                pricePart[i] = (userPart / rest) * idlePower;
                keys = usersTrafficForServer.keySet();
                float userPart2 = userTrafficForServer[i];
                float rest2 = 0;
                for (String k : keys) {
                    rest2 += usersTrafficForServer.get(k)[i];
                    allTraffic += rest2;
                }
                pricePart[i] += (userPart2 / rest2) * (server[i] - idlePower);
            }
        }
        float sum = 0;
        float averageTraffic = 0;
        long averageStorage = 0;

        for (int i = 0; i < 3600; i++) {
            sum += pricePart[i];
            averageTraffic += userTrafficForServer[i];
            averageStorage += userStore[i];
        }
        float kWhOfUser = (sum / 3600) / 1000;
        double price = kWhOfUser * Properties.KWH_PRICE;

        averageTraffic = averageTraffic / 3600;
        averageStorage = averageStorage / 3600;
        allTraffic = (allTraffic / 3600);
        allStorage = allStorage / 3600;
        double averageStoragePercent = (averageStorage / allStorage) * 100;
        double averageTrafficPercent = (averageTraffic / allTraffic) * 100;
        if (Double.isNaN(averageTrafficPercent))
            averageTrafficPercent = 0.0;

        Bill bill = new Bill(serverName, user, kWhOfUser, averageTraffic, averageStorage, price, averageStoragePercent,
                averageTrafficPercent);
        return bill;
    }

    public ObjectNode toJson(ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("startTime", this.startTime);
        result.put("endTime", this.startTime + this.frameDuration - 1);

        for (String hostname : this.powerUsageByHostSum.keySet()) {
            double usage = powerUsageByHostSum.get(hostname);
            result.with("powerUsageSum").put(hostname, usage);
        }

        // some debug informations
        result.with("statistic").with("count").put("power", this.powerUsageHistory.size());
        result.with("statistic").with("count").put("storage", this.storageUsageHistory.size());
        result.with("statistic").with("count").put("initStorageEntries", this.initialStorageEntries.size());
        result.with("statistic").with("count").put("traffic", this.trafficUsageHistory.size());

        return result;
    }
}
