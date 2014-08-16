package de.tuberlin.cit.project.energy.reporting.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents usage values on a given time frame.
 *
 * @author Sascha
 */
public class UsageTimeFrame {
    /** Frame start time in seconds since 1.1.1970. */
    private final long startTime;
    /** Interval size in seconds. */
    private final long frameDuration;



    private final List<PowerHistoryEntry> powerUsage;
    private final List<StorageHistoryEntry> storageUsage;
    private final List<TrafficHistoryEntry> trafficUsage;

    /**
     * Contains a username <-> initial storage mapping. If storageUsage list
     * contains a username, this map contains at an entry too.
     * => It's safe to iterate over usernames in storageUsage an pull an initial value from this map.
     */
    private HashMap<String, Long> initialStorageEntries;

    // statistics
    private Map<String, Double> powerUsageByHost;
    
    public UsageTimeFrame(long startTime, long frameDuration) {
        this.startTime = startTime;
        this.frameDuration = frameDuration;
        
        this.powerUsage = new LinkedList<>();
        this.storageUsage = new LinkedList<>();
        this.trafficUsage = new LinkedList<>();
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
        this.powerUsage.add(powerEntry);
    }

    public List<PowerHistoryEntry> getPowerUsage() {
        return powerUsage;
    }
    
    public void addStorageUsage(StorageHistoryEntry storageEntry) {
        this.storageUsage.add(storageEntry);

        if (!this.initialStorageEntries.containsKey(storageEntry.getUsername()))
            this.initialStorageEntries.put(storageEntry.getUsername(), 0l);
    }

    public void setInitialStorageEntries(HashMap<String, Long> initialStorageEntries) {
        this.initialStorageEntries = initialStorageEntries;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Long> getLastStorageEntries() {
        HashMap<String, Long> result = (HashMap<String, Long>) this.initialStorageEntries.clone();

        for (StorageHistoryEntry entry : this.storageUsage)
            result.put(entry.getUsername(), entry.getUsedBytes());

        return result;
    }

    public void addTrafficUsage(TrafficHistoryEntry trafficEntry) {
        this.trafficUsage.add(trafficEntry);
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
    
    /** After adding history entries, this method does the final calculation step. */
    public void calculateSummary() {
        this.powerUsageByHost = calculatePowerUsageByHost();
    }
    
    /** Calculates the power usage in KWh per host */
    private Map<String, Double> calculatePowerUsageByHost() {
        HashMap<String, Double> powerUsageByHost = new HashMap<>();
        HashMap<String, Long> lastTimestamp = new HashMap<>();
        
        for (PowerHistoryEntry entry : this.powerUsage) {
            if (powerUsageByHost.containsKey(entry.getHostname())) {
                long offset = entry.getTimestamp() - lastTimestamp.get(entry.getHostname());
                double lastValue = powerUsageByHost.get(entry.getHostname());
                powerUsageByHost.put(entry.getHostname(), lastValue + offset*entry.getUsedPower());
                lastTimestamp.put(entry.getHostname(), entry.getTimestamp());
                
            } else {
                powerUsageByHost.put(entry.getHostname(), 0d);
                lastTimestamp.put(entry.getHostname(), entry.getTimestamp());
            }
        }
        
        // calculate KWh
        for (String hostname : powerUsageByHost.keySet()) {
            powerUsageByHost.put(hostname, powerUsageByHost.get(hostname) / (3600.0*1000));
        }
        
        return powerUsageByHost;
    }
    
    public ObjectNode toJson(ObjectMapper objectMapper) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("startTime", this.startTime);
        result.put("endTime", this.startTime + this.frameDuration - 1);

        for (String hostname : this.powerUsageByHost.keySet()) {
            double usage = powerUsageByHost.get(hostname);
            result.with("powerUsage").put(hostname, usage);
        }

        // some debug informations
        result.with("statistic").with("count").put("power", this.powerUsage.size());
        result.with("statistic").with("count").put("storage", this.storageUsage.size());
        result.with("statistic").with("count").put("initStorageEntries", this.initialStorageEntries.size());
        result.with("statistic").with("count").put("traffic", this.trafficUsage.size());
        
        return result;
    }

    public HashMap<String, Long> getInitialStorageEntries() {
        return initialStorageEntries;
    }

    public List<StorageHistoryEntry> getStorageUsage() {
        return storageUsage;
    }

    public List<TrafficHistoryEntry> getTrafficUsage() {
        return trafficUsage;
    }

    public Map<String, Double> getPowerUsageByHost() {
        return powerUsageByHost;
    }
}
