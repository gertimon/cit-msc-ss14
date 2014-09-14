package de.tuberlin.cit.project.energy.reporting.model;

public class HistoryEntry implements Comparable<HistoryEntry> {
    protected final long timestamp;
    
    public HistoryEntry(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public int compareTo(HistoryEntry other) {
        return (int) (this.timestamp - other.timestamp);
    }
}
