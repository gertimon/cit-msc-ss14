package de.tuberlin.cit.project.energy.reporting.model;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Tobias und Sascha
 */
public class UsageReport {

    private final long fromTime;
    private final long toTime;

    private List<String> hosts;

    private Map<String, Double> power; // hostname:value
    private Map<String, Double> userTraffic; // hostname:value
    private Map<String, Double> allUsersTraffic; // hostname:value
    private Storage userStorage; // dateSeconds:value

    /**
     *
     * @param username
     * @param from in seconds since 1970.
     * @param to in seconds since 1970.
     */
    public UsageReport(long from, long to) {
        this.fromTime = from;
        this.toTime = to;
    }

    public long getFromTime() {
        return fromTime;
    }

    public long getToTime() {
        return toTime;
    }

    public void setPower(Map<String, Double> power) {
        this.power = power;
    }

    public Map<String, Double> getPower() {
        return power;
    }

    public void setTraffic(Map<String, Double> traffic) {
        this.userTraffic = traffic;
    }

    public Map<String, Double> getTraffic() {
        return userTraffic;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public Map<String, Double> getUserTraffic() {
        return userTraffic;
    }

    public void setUserTraffic(Map<String, Double> userTraffic) {
        this.userTraffic = userTraffic;
    }

    public Map<String, Double> getAllUsersTraffic() {
        return allUsersTraffic;
    }

    public void setAllUsersTraffic(Map<String, Long> allUsersTraffic) {
//        this.allUsersTraffic = allUsersTraffic;
    }

    public void setUserStorage(TreeMap<Long, Double> userStorage) {
        this.userStorage = new Storage(userStorage);
    }

    /**
     * @see
     * http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
     * @param size
     * @return
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
            sb.append("\n- Storage Mean: ").append(readableFileSize(userStorage.calculateWeigthedHarmonicMedian(fromTime, toTime)));
            sb.append("\n# Report finished\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //
        return sb.toString();
    }
}
