package de.tuberlin.cit.project.energy.reporting.model;

public class TrafficHistoryEntry extends HistoryEntry {
    private final String hostname;
    private final String username;
    private final float usedBytesPerSeconds;

    public TrafficHistoryEntry(long timestamp, String hostname, String username, float usedBytes) {
        super(timestamp);
        this.hostname = hostname;
        this.username = username;
        this.usedBytesPerSeconds = usedBytes;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public String getUsername() {
        return username;
    }

    public float getUsedBytes() {
        return usedBytesPerSeconds;
    }
}
