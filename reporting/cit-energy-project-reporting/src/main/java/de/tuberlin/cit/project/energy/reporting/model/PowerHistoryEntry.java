package de.tuberlin.cit.project.energy.reporting.model;

public class PowerHistoryEntry extends HistoryEntry {
    private final String hostname;
    private final float usedPower;

    public PowerHistoryEntry(long timestamp, String hostname, float usedPower) {
        super(timestamp);
        this.hostname = hostname;
        this.usedPower = usedPower;
    }
    
    public String getUsername() {
        return hostname;
    }
    
    public float getUsedPower() {
        return usedPower;
    }
}
