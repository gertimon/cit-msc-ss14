package de.tuberlin.cit.project.energy.reporting.model;

public class StorageHistoryEntry extends HistoryEntry {
    private final String username;
    private final long usedBytes;

    public StorageHistoryEntry(long timestamp, String username, long usedBytes) {
        super(timestamp);
        this.username = username;
        this.usedBytes = usedBytes;
    }

    public String getUsername() {
        return username;
    }

    public long getUsedBytes() {
        return usedBytes;
    }
}
