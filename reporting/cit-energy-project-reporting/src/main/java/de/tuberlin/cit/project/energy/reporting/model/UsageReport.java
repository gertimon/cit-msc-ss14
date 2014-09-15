package de.tuberlin.cit.project.energy.reporting.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class describes a time range containing all history entries and usage time frames.
 *
 * @author Tobias, Fabian und Sascha
 */
public class UsageReport {

    /** Start time in seconds since 1970. */
    private final long startTime;
    private final long intervalCount;
    /** Size in seconds (e.g. 1h). */
    private final int intervalSize;

    private List<PowerHistoryEntry> powerUsage;
    private List<StorageHistoryEntry> storageUsage;
    private List<TrafficHistoryEntry> trafficUsage;

    public LinkedList<UsageTimeFrame> getUsageTimeFrames() {
        return usageTimeFrames;
    }

    /** Usage time frames ordered by frame start time. */
    private LinkedList<UsageTimeFrame> usageTimeFrames;

    /**
     * Construct a report with e.g. intervalCount=3 and intervalSize=60*60 => 3h starting at startTime.
     *
     * @param startTime in seconds since 1970, aligned with intervalSize.
     * @param intervalSize in seconds.
     */
    public UsageReport(long startTime, int intervalCount, int intervalSize) {
        this.startTime = startTime;
        this.intervalCount = intervalCount;
        this.intervalSize = intervalSize;

        if (this.startTime % this.intervalSize != 0)
            throw new IllegalArgumentException("Start time has to be aligned on interval size.");
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return this.startTime + this.intervalCount*this.intervalSize - 1;
    }

    public long getIntervalCount() {
        return intervalCount;
    }

    public int getIntervalSize() {
        return intervalSize;
    }

    public void setPowerUsage(List<PowerHistoryEntry> powerUsage) {
        this.powerUsage = powerUsage;
    }
    
    public List<PowerHistoryEntry> getPowerUsage() {
        return powerUsage;
    }

    public void setStorageUsage(List<StorageHistoryEntry> storageUsage) {
        this.storageUsage = storageUsage;
    }
    
    public List<StorageHistoryEntry> getStorageUsage() {
        return storageUsage;
    }

    public void setTrafficUsage(List<TrafficHistoryEntry> trafficUsage) {
        this.trafficUsage = trafficUsage;
    }

    public List<TrafficHistoryEntry> getTrafficUsage() {
        return trafficUsage;
    }

    @SuppressWarnings("unchecked")
    public void calculateReport() {
        LinkedList<UsageTimeFrame> timeFrames = initializeUsageTimeFrames();
        List<HistoryEntry> dataLists[] = new List[]{ this.powerUsage, this.storageUsage, this.trafficUsage };
        assignDataToTimeFrames(this.startTime, this.getEndTime(), timeFrames, dataLists);
        for (UsageTimeFrame frame : timeFrames)
            frame.calculateSummary();
        this.usageTimeFrames = timeFrames;
    }

    public LinkedList<UsageTimeFrame> initializeUsageTimeFrames() {
        LinkedList<UsageTimeFrame> timeFrames = new LinkedList<>();

        for (int i = 0; i < this.intervalCount; i++) {
            timeFrames.add(new UsageTimeFrame(this.startTime + (i * this.intervalSize), this.intervalSize));
        }
        
        return timeFrames;
    }

    public void assignDataToTimeFrames(long reportStartTime, long reportEndTime,
            List<UsageTimeFrame> timeFrames, List<HistoryEntry> dataLists[]) {

        for (List<HistoryEntry> list : dataLists) {
            if (!list.isEmpty()) {
                Iterator<HistoryEntry> entryIterator = list.iterator();
                HistoryEntry lastEntry = entryIterator.next();
                HistoryEntry entry = lastEntry;
                Iterator<UsageTimeFrame> timeFrameIterator = timeFrames.iterator();
                UsageTimeFrame frame = timeFrameIterator.next();
                HashMap<String, Long> lastEntryByUser = new HashMap<>();

                // find last initial storage value
                if (lastEntry instanceof StorageHistoryEntry) {
                    StorageHistoryEntry storageEntry = (StorageHistoryEntry) entry;
                    lastEntryByUser.put(storageEntry.getUsername(), storageEntry.getUsedBytes());

                    while(storageEntry.getTimestamp() < this.startTime && entryIterator.hasNext()) {
                        lastEntryByUser.put(storageEntry.getUsername(), storageEntry.getUsedBytes());
                        storageEntry = (StorageHistoryEntry) entryIterator.next();
                    }

                    frame.setInitialStorageEntries(lastEntryByUser);
                    entry = storageEntry;
                }

                // assign entries to usage time frames
                while(entryIterator.hasNext() && entry.getTimestamp() <= reportEndTime) {
                    // usage time frame
                    if (entry.getTimestamp() > frame.getEndTime() && timeFrameIterator.hasNext()) {
                        // add initial value as last state
                        if (lastEntry instanceof StorageHistoryEntry) {
                            lastEntryByUser = frame.getLastStorageEntries();
                            frame = timeFrameIterator.next();
                            frame.setInitialStorageEntries(lastEntryByUser);
                        } else
                            frame = timeFrameIterator.next();
                    }

                    // history entry
                    if (entry.getTimestamp() >= frame.getStartTime() && entry.getTimestamp() <= frame.getEndTime()) {
                        frame.addUsageEntry(entry);
                    }

                    if (entry.getTimestamp() <= frame.getEndTime()) {
                        lastEntry = entry;
                        entry = entryIterator.next();
                    }
                }

                // add last entry
                if (entry.getTimestamp() >= frame.getStartTime() && entry.getTimestamp() <= frame.getEndTime()) {
                    frame.addUsageEntry(entry);
                }

                // fix initial values if we don't reach the last usage time frame
                if (entry instanceof StorageHistoryEntry) {
                    if (frame.getStartTime() != this.startTime + (this.intervalCount - 1) * this.intervalSize) {
                        while(timeFrameIterator.hasNext()) {
                            lastEntryByUser = frame.getLastStorageEntries();
                            frame = timeFrameIterator.next();
                            frame.setInitialStorageEntries(lastEntryByUser);
                        }
                    }
                }
            }
        }
    }

    public ObjectNode toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode output = objectMapper.createObjectNode();
        output.put("startTime", this.startTime);
        output.put("endTime", this.getEndTime());
        output.put("intervalCount", this.intervalCount);
        output.put("intervalSize", this.intervalSize);

        ArrayNode timeFrameNodes = output.withArray("timeFrames");
        for (UsageTimeFrame timeFrame : this.usageTimeFrames) {
            timeFrameNodes.add(timeFrame.toJson(objectMapper));
        }

        return output;
    }
}
