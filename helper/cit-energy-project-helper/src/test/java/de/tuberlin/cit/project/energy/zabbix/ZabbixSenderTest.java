package de.tuberlin.cit.project.energy.zabbix;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZabbixSenderTest {
    public static final int TEST_SERVER_PORT = 10051;
    public static ZabbixAgentTestServer testServer;
    public ZabbixSender zabbixSender;

    @BeforeClass
    public static void setUpServer() throws Exception {
        testServer = new ZabbixAgentTestServer(TEST_SERVER_PORT);
    }

    @Before
    public void setUpClient() throws Exception {
        this.zabbixSender = new ZabbixSender("localhost", TEST_SERVER_PORT);
    }

    @AfterClass
    public static void tearDownServer() throws Exception {
        testServer.close();
    }

    @After
    public void tearDownClient() throws Exception {
        this.zabbixSender.quit();
    }

    public void checkJsonFields(String receivedMessage, String hostname, String key, String value) {
        assertTrue("Contains hostname", receivedMessage.matches(".*\"data\":.*\\{.*\"host\":\"" + hostname + "\".*\\}.*"));
        assertTrue("Contains key", receivedMessage.matches(".*\"data\":.*\\{.*\"key\":\"" + key + "\".*\\}.*"));
        assertTrue("Contains value", receivedMessage.matches(".*\"data\":.*\\{.*\"value\":\"" + value + "\".*\\}.*"));
        assertTrue("Contains data clock field", receivedMessage.matches(".*\"data\":.*\\{.*\"clock\":[0-9]+.*\\}.*"));
        assertTrue("Contains request clock field", receivedMessage.matches(".*\"data\":.*\\{.*\\}.*\"clock\":[0-9]+.*"));
    }

    public void checkJsonFields(String receivedMessage, String hostname, String key, String value, long clock) {
        assertTrue("Contains hostname", receivedMessage.matches(".*\"data\":.*\\{.*\"host\":\"" + hostname + "\".*\\}.*"));
        assertTrue("Contains key", receivedMessage.matches(".*\"data\":.*\\{.*\"key\":\"" + key + "\".*\\}.*"));
        assertTrue("Contains value", receivedMessage.matches(".*\"data\":.*\\{.*\"value\":\"" + value + "\".*\\}.*"));
        assertTrue("Contains data clock field", receivedMessage.matches(".*\"data\":.*\\{.*\"clock\":" + clock +".*\\}.*"));
        assertTrue("Contains request clock field", receivedMessage.matches(".*\"data\":.*\\{.*\\}.*\"clock\":[0-9]+.*"));
    }

    @Test
    public void testSendPowerConsumption() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendPowerConsumption("testHostname", 123.45);
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "datanode.power", "123.45");
    }

    @Test
    public void testSendPowerConsumptionWithTimestamp() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendPowerConsumption("testHostname", 123.45, 678);
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "datanode.power", "123.45", 678);
    }

    @Test
    public void testSendBandwidthUsage() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendBandwidthUsage("testHostname", "testUsername", 123.45);
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "user.testUsername.bandwidth", "123.45");
    }

    @Test
    public void testSendBandwidthUsageWithTimestamp() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendBandwidthUsage("testHostname", "testUsername", 123.45, 678);
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "user.testUsername.bandwidth", "123.45", 678);
    }

    @Test
    public void testSendDuration() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendDuration("testHostname", "testUsername", 123.45);
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "user.testUsername.duration", "123.45");
    }

    @Test
    public void testSendDurationWithTimestamp() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendDuration("testHostname", "testUsername", 123.45, 678);
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "user.testUsername.duration", "123.45", 678);
    }

    @Test
    public void testSendUserDataNodeConnection() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendUserDataNodeConnection("testHostname", "testUsername", "testIP:4567");
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "user.testUsername.lastAddress", "testIP:4567");
    }

    @Test
    public void testSendInternalDataNodeConnection() throws InterruptedException {
        testServer.resetLastReceivedAgentMessage();
        this.zabbixSender.sendInternalDataNodeConnection("testHostname", "testUsername", "testIP:4567");
        String receivedMessage = testServer.waitForNextMessage(10000);
        checkJsonFields(receivedMessage, "testHostname", "user.testUsername.lastInternalAddress", "testIP:4567");
    }
}
