package de.tuberlin.cit.project.energy.reporting.model;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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


        while (powerIterator.hasNext()) {
            PowerHistoryEntry powerEntry = powerIterator.next();
            powerIterator.remove();
            if (powerEntry.getTimestamp() < currentEnd) {
                powerEntry = powerIterator.next();
                currentTimeFrame.addPowerUsage(powerEntry);
            } else {
                powerUsage.add(0, powerEntry);
                break;
            }
        }
        while (trafficIterator.hasNext()) {
            TrafficHistoryEntry trafficEntry = trafficIterator.next();
            trafficIterator.remove();
            if (trafficEntry.getTimestamp() < currentEnd) {
                trafficEntry = trafficIterator.next();
                currentTimeFrame.addTrafficUsage(trafficEntry);
            } else {
                trafficUsage.add(0, trafficEntry);
                break;
            }
        }
        while (storageIterator.hasNext()) {
            StorageHistoryEntry storageEntry = storageIterator.next();
            storageIterator.remove();
            if (storageEntry.getTimestamp() < currentEnd) {
                storageEntry = storageIterator.next();
                currentTimeFrame.addStorageUsage(storageEntry);
            } else {
                storageUsage.add(0, storageEntry);
                break;
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
}
