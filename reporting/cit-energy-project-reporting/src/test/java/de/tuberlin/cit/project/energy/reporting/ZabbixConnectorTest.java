package de.tuberlin.cit.project.energy.reporting;

import de.tuberlin.cit.project.energy.reporting.model.Power;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
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
        Power power = connector.getPower("CitProjectAsok05", now - 86000 * 2, now);
        Double wattSeconds = power.getPowerAsWattSeconds(now - 86000 * 2, now);
        Double kwh = Power.wsToKwh(wattSeconds);
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
