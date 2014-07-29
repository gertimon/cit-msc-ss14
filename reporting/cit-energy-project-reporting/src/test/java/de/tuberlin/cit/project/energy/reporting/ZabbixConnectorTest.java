package de.tuberlin.cit.project.energy.reporting;

import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.naming.AuthenticationException;
import org.junit.Test;

/**
 *
 * @author Tobias
 */
public class ZabbixConnectorTest {

    public ZabbixConnectorTest() {
    }

    @Test
    public void testGetPowerUsageForRange() throws KeyManagementException, NoSuchAlgorithmException, AuthenticationException, ExecutionException, IOException, IllegalArgumentException, InternalErrorException, InterruptedException {
        ZabbixConnector connector = new ZabbixConnector();
        Long now = new Date().getTime() / 1000;
        List<ZabbixHistoryObject> result = connector.getPowerUsageForRange(now - 86400, now);
        int duration = result.size();
        double kwh = 0l;
        for (ZabbixHistoryObject zho : result) {
            kwh += zho.getFloatValue() / 3600;
        }
        System.out.println(kwh);
    }

    @Test
    public void testGetUserTraffic() {
    }

    @Test
    public void testGetUserStorage() {
    }

    @Test
    public void testGetUserNamesforRange() {
    }

    @Test
    public void testGetAllUsernames() {
    }

}
