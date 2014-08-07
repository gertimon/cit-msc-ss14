package de.tuberlin.cit.project.energy.reporting;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Tobias und Sascha
 */
public class Report {

    ZabbixConnector connector;

    private final long fromTime;
    private final long toTime;

    private List<String> hosts;

    private Map<String, Double> power; // hostname:value
    private List<String> users;

    /**
     *
     */
    public Report(long fromTimeMillis, long toTimeMillis) {
        this.fromTime = fromTimeMillis;
        this.toTime = toTimeMillis;
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

    /**
     * implementation time tests. will be kept as tests itself later.
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder report = new StringBuilder();
        // interesting values
        // complete power usage
        // user power assigned -> cost translation
        // fast traffic used complete
        // cheap traffic used complete
        // fast traffic for user
        // cheap traffic for user
        // user storage median
        //
        return report.toString();
    }
}
