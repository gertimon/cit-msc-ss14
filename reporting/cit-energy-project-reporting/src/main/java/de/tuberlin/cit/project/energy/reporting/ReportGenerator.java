package de.tuberlin.cit.project.energy.reporting;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.naming.AuthenticationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuberlin.cit.project.energy.reporting.model.Power;
import de.tuberlin.cit.project.energy.reporting.model.Report;
import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;

/**
 * Generates reports from zabbix data.
 * 
 * @author Sascha
 */
public class ReportGenerator {
    private final ZabbixAPIClient client;
    private final int datanodesHostGroupId;
    private final Map<Integer, String> hostnames;
    private final ObjectMapper objectMapper;

    public ReportGenerator() {
        try {
            this.client = new ZabbixAPIClient();
            this.datanodesHostGroupId = client.getDataNodeHostGroupId();
            Map<String, Integer> hostIds = client.getDataNodeHostIds();
            this.hostnames = new HashMap<>(hostIds.size());
            for (String hostname : hostIds.keySet())
                this.hostnames.put(hostIds.get(hostname), hostname);
        } catch (Exception e) {
            throw new RuntimeException("Can't get host ids from Zabbix.", e);
        }

        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generates a new report.
     * @param from in seconds since 1970.
     * @param to in seconds since 1970.
     */
    public Report getReport(long from, long to) {
        try {
            Report report = new Report(from, to);

            // calculate host oriented values
            addHostsPowerConsumption(report);

            // fetch user relevant information
//          retrieveUserList();
////          retrieveUserStorage();
////          retrieveUserTraffic();
////          retrieveUserProfileChanges();

            return report;
        } catch (Exception e) {
            throw new RuntimeException("Failure while generating report.", e);
        }
    }
    
    private void addHostsPowerConsumption(Report report) throws AuthenticationException, KeyManagementException,
            IllegalArgumentException, NoSuchAlgorithmException, ExecutionException, IOException,
            InternalErrorException, InterruptedException {
        
        // lookup item id's
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("groupids", this.datanodesHostGroupId);
        params.with("search").put("key_", ZabbixParams.POWER_CONSUMPTION_KEY);
        List<ZabbixItem> powerConsumptionItems = client.getItems(params);
        
        if (powerConsumptionItems.size() > 0) {
            Map<Integer, String> itemHostnameMap = new HashMap<>();
            
            params = this.objectMapper.createObjectNode();
            params.put("output", "extend");
            params.put("history", 3);
            params.put("time_from", report.getFromTime());
            params.put("time_till", report.getToTime());
            for (ZabbixItem item : powerConsumptionItems) {
                params.withArray("itemids").add(item.getItemId());
                itemHostnameMap.put(item.getItemId(), this.hostnames.get(item.getHostId()));
            }
            params.put("sortfield", "clock");
            params.put("sortorder", "ASC");
            
            List<ZabbixHistoryObject> historyObjects = client.getHistory(params);
            HashMap<String, Power> powerConsumptionWatt = new HashMap<>(this.hostnames.size());
            
            for (ZabbixHistoryObject h : historyObjects) {
                String hostname = itemHostnameMap.get(h.getItemId());
                
                Power hostConsumption = powerConsumptionWatt.get(hostname);
                if (hostConsumption == null) {
                    hostConsumption = new Power();
                    powerConsumptionWatt.put(hostname, hostConsumption);
                }

                hostConsumption.addValue(h.getClock(), h.getIntValue());
            }
            
            HashMap<String, Double> powerConsumptionKWh = new HashMap<>(powerConsumptionWatt.size());

            for (String hostname : powerConsumptionWatt.keySet()) {
                Power p = powerConsumptionWatt.get(hostname);
                double ws = Power.getPowerAsWattSeconds(p.getPowerValues(), report.getFromTime(), report.getToTime());
                double kwh = Power.wsToKwh(ws);
                powerConsumptionKWh.put(hostname, kwh);
            }
            
            report.setPower(powerConsumptionKWh);
        }
    }
    
//    private void retrieveUserList() throws AuthenticationException, IllegalArgumentException, InterruptedException,
//            ExecutionException, InternalErrorException, IOException, TemplateNotFoundException {
//
//        users = connector.getAllUsernames();
//        System.out.println("Users: " + users);
//    }
//
//    private void retrieveUserStorage() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private void retrieveUserTraffic() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private void retrieveUserProfileChanges() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    public void quit() {
        this.client.quit();
    }
    
    public static void main(String[] args) throws Exception {
        ReportGenerator generator = new ReportGenerator();
        
        long now = (new Date()).getTime() / 1000;
        Report report = generator.getReport(now - 60*60*24*31, now);
        System.out.println("Got report: " + report);
        
        for (String hostname : report.getPower().keySet())
            System.out.println("Host " + hostname + " used " + report.getPower().get(hostname) + " KWh.");

        generator.quit();
    }
}
