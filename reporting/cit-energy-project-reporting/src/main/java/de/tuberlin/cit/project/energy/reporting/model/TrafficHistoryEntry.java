package de.tuberlin.cit.project.energy.reporting.model;

public class TrafficHistoryEntry extends HistoryEntry {
    private final String hostname;
    private final String username;
    private float usedBytesPerSeconds;

    public TrafficHistoryEntry(long timestamp, String hostname, String username, float usedBytesPerSeconds) {
        super(timestamp);
        this.hostname = hostname;
        this.username = username;
        this.usedBytesPerSeconds = usedBytesPerSeconds;
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
    
    public void addUsedBytes(TrafficHistoryEntry entry) {
        this.usedBytesPerSeconds += entry.usedBytesPerSeconds;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrafficHistoryEntry) {
            TrafficHistoryEntry other = (TrafficHistoryEntry) obj;
            return  other.hostname.equals(this.hostname) && other.timestamp == this.timestamp;
        }
        
        return false;
    }
}
