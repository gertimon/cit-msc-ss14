package de.tuberlin.cit.project.energy.reporting.model;

import java.text.DecimalFormat;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tobias und Sascha
 */
public class UsageReport {

    /**
     * Start time in seconds since 1970.
     */
    private final long fromTime;
    /**
     * End time in seconds since 1970.
     */
    private final long toTime;
    /**
     * Resolution in seconds (e.g. 1h).
     */
    private final int resolution;

    private List<PowerHistoryEntry> powerUsage;
    private List<StorageHistoryEntry> storageUsage;
    private List<TrafficHistoryEntry> trafficUsage;


    public LinkedList<UsageTimeFrame> getUsageTimeFrames() {
        return usageTimeFrames;
    }

    /** Usage time frames ordered by frame start time. */
    private LinkedList<UsageTimeFrame> usageTimeFrames;

    /**
     * @param from       in seconds since 1970.
     * @param to         in seconds since 1970.
     * @param resolution in seconds.
     */
    public UsageReport(long from, long to, int resolution) {
        this.fromTime = from;
        this.toTime = to;
        this.resolution = resolution;
        this.usageTimeFrames = new LinkedList<>();
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

    @SuppressWarnings("unchecked")
    public void calculateReport() {
        LinkedList<UsageTimeFrame> timeFrames = initializeUsageTimeFrames();
        List<HistoryEntry> dataLists[] = new List[]{ this.powerUsage, this.storageUsage, this.trafficUsage };
        assignDataToTimeFrames(this.startTime, this.getEndTime(), timeFrames, dataLists);
        for (UsageTimeFrame frame : timeFrames)
            frame.calculateSummary();
        this.usageTimeFrames = timeFrames;
        UserBillCalculator calc = new UserBillCalculator(usageTimeFrames);
        billList = calc.getBill();
        for (HashMap<String, BillForAllServers> bills :billList){
           for (BillForAllServers billsss : bills.values()){
               System.out.println(billsss);
           }
        }
    }


        Iterator<PowerHistoryEntry> powerIterator = this.powerUsage.iterator();
        Iterator<StorageHistoryEntry> storageIterator = this.storageUsage.iterator();
        Iterator<TrafficHistoryEntry> trafficIterator = this.trafficUsage.iterator();

        long currentStart = from;
        long currentEnd = from + this.resolution - 1;
        UsageTimeFrame currentTimeFrame = new UsageTimeFrame(currentStart, this.resolution);
        this.usageTimeFrames.add(currentTimeFrame);

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

            while(storageIterator.hasNext()){
                StorageHistoryEntry storageEntry = storageIterator.next();
                storageIterator.remove();
                currentTimeFrame.addStorageUsage(storageEntry);
                if (storageEntry.getTimestamp() <= currentEnd) {
                    storageEntry = storageIterator.next();
                    storageIterator.remove();
                    currentTimeFrame.addStorageUsage(storageEntry);
                }
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
        if (from + resolution <= to - resolution) {
            calculateReport(from + resolution, to, resolution);
        }

    }
    
    /**
     * @param size
     * @return
     * @see http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
     */
    public static String readableFileSize(double size) {

        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * implementation time tests. will be kept as tests itself later.
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n# Report range: from ").append(new Date(this.startTime * 1000))
          .append(" to ").append(new Date((this.startTime + this.intervalCount*this.intervalSize - 1) * 1000));

        try {
            // interesting values
            // complete power usage
            // user power assigned -> cost translation
            // fast userTraffic used complete
            // cheap userTraffic used complete
            // fast userTraffic for user
            // cheap userTraffic for user
            // user storage median
            // sb.append("\n- Storage Mean: ").append(readableFileSize(userStorage.calculateWeigthedHarmonicMedian(fromTime, toTime)));
            sb.append("\n# Report finished\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //
        return sb.toString();
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
