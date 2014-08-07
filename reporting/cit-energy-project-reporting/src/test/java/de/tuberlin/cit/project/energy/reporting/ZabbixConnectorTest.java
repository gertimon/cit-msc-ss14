package de.tuberlin.cit.project.energy.reporting;

import de.tuberlin.cit.project.energy.reporting.model.Power;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.naming.AuthenticationException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Tobias
 */
public class ZabbixConnectorTest {

    public ZabbixConnectorTest() {
    }

    @Test
    public void testGetPowerUsageForRange() throws Exception {
        ZabbixConnector connector = new ZabbixConnector();
        Long now = new Date().getTime() / 1000;
        Power power = connector.getPowerUsageByRange("CitProjectAsok05", now - 86000 * 2, now);
        Double wattSeconds = Power.getPowerAsWattSeconds(power.getPowerValues(), now - 86000 * 2, now);
        System.out.println(wattSeconds + " watt seconds");
    }

    @Test
    public void testGetUserTraffic() throws Exception {
        ZabbixConnector connector = new ZabbixConnector();
        List<ZabbixHistoryObject> result = connector.getUserTraffic("mpjss14", "CitProjectOffice", new Date().getTime() - 86400, new Date().getTime());
        System.out.println(result);

    }

    @Test
    public void testGetUserStorage() throws KeyManagementException, NoSuchAlgorithmException {
        ZabbixConnector connector = new ZabbixConnector();
        List<ZabbixHistoryObject> result = connector.getUserStorage("mpjss14", new Date().getTime() - 86400000, new Date().getTime());
        System.out.println(result);
    }

    @Test
    public void testGetAllUsernames() throws Exception {
        ZabbixConnector connector = new ZabbixConnector();
        Assert.assertTrue("Contains user mpjss14", connector.getAllUsernames().contains("mpjss14"));
    }

    @Test
    public void testDate() {
        System.out.println(new Date().getTime());
    }

}
