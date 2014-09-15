package de.tuberlin.cit.project.energy.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.tuberlin.cit.project.energy.reporting.model.HistoryEntry;
import de.tuberlin.cit.project.energy.reporting.model.PowerHistoryEntry;
import de.tuberlin.cit.project.energy.reporting.model.StorageHistoryEntry;
import de.tuberlin.cit.project.energy.reporting.model.TrafficHistoryEntry;
import de.tuberlin.cit.project.energy.reporting.model.UsageReport;
import de.tuberlin.cit.project.energy.reporting.web.WebFrontEnd;

/**
 * Simple Report CSV export/import utility class.
 *
 * Example usage: {@link WebFrontEnd}
 *
 * @author Sascha
 */
public class CSVUtils {
    
    @SuppressWarnings("unchecked")
    public static void writeCSV(UsageReport report, String filenamePrefix) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filenamePrefix + ".csv"));
        writer.write("startTime;endTime;intervalCount;intervalSize\n");
        writer.write(report.getStartTime() + ";" + report.getEndTime() + ";" + report.getIntervalCount() + ";" + report.getIntervalSize());
        writer.close();
        writeCSV(filenamePrefix + ".power.csv", (List<HistoryEntry>)(Object) report.getPowerUsage());
        writeCSV(filenamePrefix + ".storage.csv", (List<HistoryEntry>)(Object) report.getStorageUsage());
        writeCSV(filenamePrefix + ".traffic.csv", (List<HistoryEntry>)(Object) report.getTrafficUsage());
    }
    
    private static void writeCSV(String fileName, List<HistoryEntry> entries) throws Exception {
        GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(fileName + ".gz"));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        StringBuilder sb = new StringBuilder();
        for(HistoryEntry entry : entries) {
            toCSV(entry, sb);
            sb.append('\n');
            writer.write(sb.toString());
            sb.setLength(0);
        }
        writer.close();
    }
    
    private static void toCSV(HistoryEntry entry, StringBuilder sb) {
        sb.append(entry.getTimestamp());
        sb.append(";");
        
        if (entry instanceof PowerHistoryEntry) {
            sb.append(((PowerHistoryEntry)entry).getHostname());
            sb.append(";");
            sb.append(((PowerHistoryEntry)entry).getUsedPower());

        } else if (entry instanceof StorageHistoryEntry) {
            sb.append(((StorageHistoryEntry)entry).getUsername());
            sb.append(";");
            sb.append(((StorageHistoryEntry)entry).getUsedBytes());

        } else if (entry instanceof TrafficHistoryEntry) {
            sb.append(((TrafficHistoryEntry)entry).getHostname());
            sb.append(";");
            sb.append(((TrafficHistoryEntry)entry).getUsername());
            sb.append(";");
            sb.append(((TrafficHistoryEntry)entry).getUsedBytes());
        }
    }
    
    @SuppressWarnings("unchecked")
    public static UsageReport readCSV(String filenamePrefix) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filenamePrefix + ".csv"));
        reader.readLine(); // drop header
        String values[] = reader.readLine().split(";");
        reader.close();
        UsageReport report = new UsageReport(Long.parseLong(values[0]), Integer.parseInt(values[2]), Integer.parseInt(values[3]));
        
        report.setPowerUsage((List<PowerHistoryEntry>)(Object)readCSV(filenamePrefix + ".power.csv", PowerHistoryEntry.class));
        report.setStorageUsage((List<StorageHistoryEntry>)(Object)readCSV(filenamePrefix + ".storage.csv", StorageHistoryEntry.class));
        report.setTrafficUsage((List<TrafficHistoryEntry>)(Object)readCSV(filenamePrefix + ".traffic.csv", TrafficHistoryEntry.class));
        
        report.calculateReport();
        
        return report;
    }

    private static List<HistoryEntry> readCSV(String filename, Class<? extends HistoryEntry> dst) throws Exception {
        List<HistoryEntry> result = new LinkedList<>();

        GZIPInputStream is = new GZIPInputStream(new FileInputStream(filename + ".gz"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while((line = reader.readLine()) != null) {
            result.add(readCSV(line.split(";"), dst));
        }
        reader.close();
        
        return result;
    }

    private static HistoryEntry readCSV(String values[], Class<? extends HistoryEntry> dst) {
        long timestamp = Long.parseLong(values[0]);

        if (dst.equals(PowerHistoryEntry.class)) {
            return new PowerHistoryEntry(timestamp, values[1], Float.parseFloat(values[2]));
        } else if (dst.equals(StorageHistoryEntry.class)) {
            return new StorageHistoryEntry(timestamp, values[1], Long.parseLong(values[2]));
        } else if (dst.equals(TrafficHistoryEntry.class)) {
            return new TrafficHistoryEntry(timestamp, values[1], values[2], Float.parseFloat(values[3]));
        } else {
            return null;
        }
    }
}
