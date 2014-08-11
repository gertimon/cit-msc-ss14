package de.tuberlin.cit.project.energy.reporting.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents usage values on a given time frame.
 *
 * @author Sascha
 */
public class UsageTimeFrame {

    private final long startTime;
    private final long durationInSeconds;
    
    private final List<PowerHistoryEntry> powerUsage;
    private final List<StorageHistoryEntry> storageUsage;
    private final List<TrafficHistoryEntry> trafficUsage;
    
    public UsageTimeFrame(long startTime, long durationInSeconds) {
        this.startTime = startTime;
        this.durationInSeconds = durationInSeconds;
        
        this.powerUsage = new LinkedList<>();
        this.storageUsage = new LinkedList<>();
        this.trafficUsage = new LinkedList<>();
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getDurationInSeconds() {
        return durationInSeconds;
    }
    
    public void addPowerUsage(PowerHistoryEntry powerEntry) {
        this.powerUsage.add(powerEntry);
    }
    
    public void addStorageUsage(StorageHistoryEntry storageEntry) {
        this.storageUsage.add(storageEntry);
    }
    
    public void addTrafficUsage(TrafficHistoryEntry trafficEntry) {
        this.trafficUsage.add(trafficEntry);
    }
}
