package de.tuberlin.cit.project.energy.reporting;

import de.tuberlin.cit.project.energy.reporting.model.Power;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tobias
 */
public class Report {

    ZabbixConnector connector;

    private final long fromTimeMillis;
    private final long toTimeMillis;

    private List<String> hosts;

    private Map<String, Double> power; // hostname:value
    private List<String> users;

    /**
     *
     * @param fromTimeMillis
     * @param toTimeMillis
     * @throws java.security.KeyManagementException
     * @throws java.security.NoSuchAlgorithmException
     */
    public Report(long fromTimeMillis, long toTimeMillis) throws KeyManagementException, NoSuchAlgorithmException {
        this.fromTimeMillis = fromTimeMillis;
        this.toTimeMillis = toTimeMillis;
        System.out.println("Report range: " + new Date(fromTimeMillis) + " - " + new Date(toTimeMillis));
        this.connector = new ZabbixConnector();
        this.power = new HashMap<>();
    }

    private void retrieveHostsList() {
        hosts = connector.getDatanodeHosts();
    }

    public void fetchValues() {

        // get list of available hosts
        retrieveHostsList();

        // calculate host oriented values
        retrieveHostsPowerConsumption();

        // fetch user relevant information
        retrieveUserList();
//        retrieveUserStorage();
//        retrieveUserTraffic();
//        retrieveUserProfileChanges();
    }

    public List<String> getHosts() {
        return hosts;
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

    private void retrieveHostsPowerConsumption() {
        for (String hostname : getHosts()) {
            Power p = connector.getPower(hostname, fromTimeMillis, toTimeMillis);
            Double wattSeconds = Power.getPowerAsWattSeconds(p.getPowerValues(), fromTimeMillis, toTimeMillis);
            Double kwh = Power.wsToKwh(wattSeconds);
            power.put(hostname, kwh);
        }
        System.out.println("Power Consumption: " + power);
    }

    private void retrieveUserList() {
        users = connector.getAllUsernames();
        System.out.println("Users: " + users);
    }

    private void retrieveUserStorage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void retrieveUserTraffic() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void retrieveUserProfileChanges() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException {
        Date begin = new Date();
        Long from = begin.getTime() - 86400000;
        Long to = begin.getTime();
        Report report = new Report(from, to);
        report.fetchValues();
    }
}
