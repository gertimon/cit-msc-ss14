package de.tuberlin.cit.project.energy.reporting.model;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    public List<UsageTimeFrame> getUsageTimeFrames() {
        return usageTimeFrames;
    }

    private List<UsageTimeFrame> usageTimeFrames;

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

    public long getFromTime() {
        return fromTime;
    }

    public long getToTime() {
        return toTime;
    }

    public int getResolution() {
        return resolution;
    }

    public void setPowerUsage(List<PowerHistoryEntry> powerUsage) {
        this.powerUsage = powerUsage;
    }

    public void setStorageUsage(List<StorageHistoryEntry> storageUsage) {
        this.storageUsage = storageUsage;
    }

    public void setTrafficUsage(List<TrafficHistoryEntry> trafficUsage) {
        this.trafficUsage = trafficUsage;
    }

    public void calculateReport(long from, long to, int resolution) {


        Iterator<PowerHistoryEntry> powerIterator = this.powerUsage.iterator();
        Iterator<StorageHistoryEntry> storageIterator = this.storageUsage.iterator();
        Iterator<TrafficHistoryEntry> trafficIterator = this.trafficUsage.iterator();

        long currentStart = from;
        long currentEnd = from + this.resolution - 1;
        UsageTimeFrame currentTimeFrame = new UsageTimeFrame(currentStart, this.resolution);
        this.usageTimeFrames.add(currentTimeFrame);
        PowerHistoryEntry powerEntry = powerIterator.next();
        currentTimeFrame.addPowerUsage(powerEntry);
        StorageHistoryEntry storageEntry = storageIterator.next();
        currentTimeFrame.addStorageUsage(storageEntry);
        TrafficHistoryEntry trafficEntry = trafficIterator.next();
        currentTimeFrame.addTrafficUsage(trafficEntry);
        
        while(powerIterator.hasNext() && storageIterator.hasNext() && trafficIterator.hasNext()) {
            if (powerEntry.getTimestamp() <= currentEnd) {
                powerEntry = powerIterator.next();
                currentTimeFrame.addPowerUsage(powerEntry);
            } else {
                powerUsage.add(0, powerEntry);
                break;
            }
            
            if (storageEntry.getTimestamp() <= currentEnd) {
                storageEntry = storageIterator.next();
                currentTimeFrame.addStorageUsage(storageEntry);
            }

            if (trafficEntry.getTimestamp() <= currentEnd) {
                trafficEntry = trafficIterator.next();
                currentTimeFrame.addTrafficUsage(trafficEntry);
            } else {
                trafficUsage.add(0, trafficEntry);
                break;
            }
            
            // next frame if all iterator reach current end
            if (powerEntry.getTimestamp() > currentEnd && storageEntry.getTimestamp() > currentEnd && trafficEntry.getTimestamp() > currentEnd) {
                currentStart = currentStart + this.resolution;
                currentEnd = currentStart + this.resolution - 1;
                currentTimeFrame = new UsageTimeFrame(currentStart, this.resolution);
                currentTimeFrame.addPowerUsage(powerEntry);
                currentTimeFrame.addStorageUsage(storageEntry);
                currentTimeFrame.addTrafficUsage(trafficEntry);
                this.usageTimeFrames.add(currentTimeFrame);
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
        sb.append("\n# Report range: from ").append(new Date(fromTime * 1000)).append(" to ").append(new Date(toTime * 1000));

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
        output.put("from", this.fromTime);
        output.put("to", this.toTime);
        output.put("resolution", this.resolution);
        ArrayNode timeFrameNodes = output.withArray("timeFrames");
        for (UsageTimeFrame timeFrame : this.usageTimeFrames) {
            ObjectNode timeFrameNode = objectMapper.createObjectNode();
            timeFrameNode.put("startTime", timeFrame.getStartTime());
            timeFrameNodes.add(timeFrameNode);
        }
        
        return output;
    }
}
