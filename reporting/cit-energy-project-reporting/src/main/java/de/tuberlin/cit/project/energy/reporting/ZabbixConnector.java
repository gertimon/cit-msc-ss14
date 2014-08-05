package de.tuberlin.cit.project.energy.reporting;

import de.tuberlin.cit.project.energy.reporting.model.Power;
import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.exception.TemplateNotFoundException;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.AuthenticationException;

/**
 *
 * @author Tobias
 */
public class ZabbixConnector {

    private final ZabbixAPIClient client;
    static final String[] HOSTS = {"CitProjectOffice", "CitProjectAsok05"};

    public ZabbixConnector() throws KeyManagementException, NoSuchAlgorithmException {
        client = new ZabbixAPIClient(ZabbixParams.DEFAULT_ZABBIX_REST_URL, ZabbixParams.DEFAULT_ZABBIX_USERNAME, ZabbixParams.DEFAULT_ZABBIX_PASSWORD, ZabbixParams.USERNAME, ZabbixParams.PASSWORD);

    }

    public List<String> getDatanodeHosts() {
        try {
            List<String> hosts = client.getHosts(null);
            System.out.println("Host List: " + hosts);
            return hosts;
        } catch (IllegalArgumentException | InterruptedException | ExecutionException | AuthenticationException | InternalErrorException | IOException ex) {
            Logger.getLogger(ZabbixConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    protected List<ZabbixHistoryObject> getPowerUsageForRange(String hostname, long fromMillis, long toMillis) throws AuthenticationException, ExecutionException, IOException, IllegalArgumentException, InternalErrorException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        List<ZabbixHistoryObject> result = null;
        result = client.getNumericHistory(hostname, "datanode.power", fromMillis, toMillis);
        return result;
    }

    public Power getPower(String hostname, long from, long to) {
        List<ZabbixHistoryObject> historyObjects = null;
        try {
            historyObjects = getPowerUsageForRange(hostname, from, to);
        } catch (AuthenticationException | ExecutionException | IOException | IllegalArgumentException | InternalErrorException | InterruptedException | KeyManagementException | NoSuchAlgorithmException ex) {
            Logger.getLogger(ZabbixConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (historyObjects != null) {
            return new Power(historyObjects);
        }
        return null;
    }

    public List<ZabbixHistoryObject> getUserTraffic(String userName, String host, long fromMillis, long toMillis) {
        try {
            return client.getNumericHistory(host, String.format(ZabbixParams.USER_BANDWIDTH_KEY, userName), fromMillis / 1000, toMillis / 1000);
        } catch (AuthenticationException | IllegalArgumentException | InterruptedException | ExecutionException | IOException | InternalErrorException ex) {
            Logger.getLogger(ZabbixConnector.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
        return null;
    }

    public List<ZabbixHistoryObject> getUserStorage(String username, long fromMillis, long toMillis) {
        try {
            // each node has same value for that
            return client.getNumericHistoryWithPrecedingElement(HOSTS[1], String.format(ZabbixParams.USER_DATA_USAGE_KEY, username), toMillis / 1000, toMillis / 1000);
        } catch (AuthenticationException | IllegalArgumentException | InterruptedException | ExecutionException | IOException | InternalErrorException ex) {
            Logger.getLogger(ZabbixConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

//    @Override
    public List<String> getAllUsernames() {
        try {
            return client.getAllUsers();
        } catch (AuthenticationException | IllegalArgumentException | InterruptedException | ExecutionException | InternalErrorException | IOException | TemplateNotFoundException ex) {
            Logger.getLogger(ZabbixConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
