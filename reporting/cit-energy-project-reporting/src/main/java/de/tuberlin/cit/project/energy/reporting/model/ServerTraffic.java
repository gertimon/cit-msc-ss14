package de.tuberlin.cit.project.energy.reporting.model;

import java.util.HashMap;

class ServerTraffic {
    private final UsageTimeFrame timeFrame;
    private final HashMap<String, float[]> userTraffic;
    private final HashMap<String, Integer> lastUserTrafficEnd;

    ServerTraffic(UsageTimeFrame timeFrame) {
        this.timeFrame = timeFrame;
        this.userTraffic = new HashMap<>();
        this.lastUserTrafficEnd = new HashMap<>();
    }

    public HashMap<String, float[]> getUserTraffic() {
        return userTraffic;
    }

    public void addEntry(TrafficHistoryEntry entry) {
        float traffic[] = this.userTraffic.get(entry.getUsername());
        int rangeStart;
        int rangeEnd = (int) (this.timeFrame.getStartTime() - entry.getTimestamp());

        if (traffic == null) {
            traffic = new float[(int) this.timeFrame.getDurationInSeconds()];
            this.userTraffic.put(entry.getUsername(), traffic);
            rangeStart = 0;

        } else {
            rangeStart = this.lastUserTrafficEnd.get(entry.getUsername()) + 1;
        }

        for (int i = rangeStart; i <= rangeEnd; i++) {
            traffic[i] = entry.getUsedBytes();
        }

        this.lastUserTrafficEnd.put(entry.getUsername(), rangeEnd);
    }
}