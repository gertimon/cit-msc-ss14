package de.tuberlin.cit.project.energy.reporting;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 *
 * @author Tobias
 */
public class Report {

    ZabbixConnector connector;

    private final long fromTimeMillis;
    private final long toTimeMillis;

    private List<String> hosts;

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
        this.connector = new ZabbixConnector();
    }

    private void retrieveHostsList() {
        hosts = connector.getDatanodeHosts();
    }

    public void fetchValues() {
        retrieveHostsList();
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

    private void calculate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
