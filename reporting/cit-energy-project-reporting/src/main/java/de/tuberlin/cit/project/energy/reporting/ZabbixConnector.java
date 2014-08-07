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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    public List<String> getDatanodeHosts() throws AuthenticationException, IllegalArgumentException,
            InterruptedException, ExecutionException, InternalErrorException, IOException {

        List<String> hosts = client.getHosts(null);
        System.out.println("Host List: " + hosts);
        return hosts;
    }

    public Power getPowerUsageByRange(String hostname, long fromMillis, long toMillis) 
            throws AuthenticationException, ExecutionException, IOException, IllegalArgumentException,
            InternalErrorException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {

        return new Power(client.getNumericHistory(hostname, "datanode.power", fromMillis, toMillis));
    }

    public List<ZabbixHistoryObject> getUserTraffic(String userName, String host, long fromMillis, long toMillis)
            throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException,
            IOException, InternalErrorException {

        return client.getNumericHistory(host, String.format(ZabbixParams.USER_BANDWIDTH_KEY, userName), fromMillis / 1000, toMillis / 1000);
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
    public List<String> getAllUsernames() throws AuthenticationException, IllegalArgumentException,
            InterruptedException, ExecutionException, InternalErrorException, IOException, TemplateNotFoundException {

        return client.getAllUsers();
    }

}
