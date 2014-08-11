package de.tuberlin.cit.project.energy.reporting.model;

public class TrafficHistoryEntry extends HistoryEntry {
    private final String username;
    private final long usedBytesPerSeconds;

    public TrafficHistoryEntry(long timestamp, String username, long usedBytes) {
        super(timestamp);
        this.username = username;
        this.usedBytesPerSeconds = usedBytes;
    }
    
    public String getUsername() {
        return username;
    }

    public long getUsedBytes() {
        return usedBytesPerSeconds;
    }
}
