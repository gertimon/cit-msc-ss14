package de.tuberlin.cit.project.energy.reporting;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.naming.AuthenticationException;

/**
 *
 * @author Tobias
 */
public class ZabbixConnector {

    private ZabbixAPIClient client;
    static final String[] HOSTS = {"CitProjectOffice", "CitProjectAsok05"};

    public ZabbixConnector() throws KeyManagementException, NoSuchAlgorithmException {
        client = new ZabbixAPIClient(ZabbixParams.DEFAULT_ZABBIX_REST_URL, ZabbixParams.DEFAULT_ZABBIX_USERNAME, ZabbixParams.DEFAULT_ZABBIX_PASSWORD, ZabbixParams.USERNAME, ZabbixParams.PASSWORD);

    }

    public ZabbixAPIClient getClient() {
        return client;
    }

    public List<ZabbixHistoryObject> getPowerUsageForRange(long fromMillis, long toMillis) throws AuthenticationException, ExecutionException, IOException, IllegalArgumentException, InternalErrorException, InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        List<ZabbixHistoryObject> result = null;
        ZabbixConnector connector = new ZabbixConnector();

        result = connector.getClient().getNumericHistory(HOSTS[0], "datanode.power", fromMillis, toMillis);

        return result;
    }

//    @Override
    public String getUserTraffic(String userName, long fromMillis, long toMillis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    @Override
    public String getUserStorage(String userName, long fromMillis, long toMillis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    @Override
    public Collection<String> getUserNamesforRange(long fromMillis, long toMillis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    @Override
    public Collection<String> getAllUsernames() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
