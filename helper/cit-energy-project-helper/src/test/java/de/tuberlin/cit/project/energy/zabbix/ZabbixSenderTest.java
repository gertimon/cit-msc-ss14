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

	@Test
	public void testSendPowerConsumption() throws InterruptedException {
		testServer.resetLastReceivedAgentMessage();
		this.zabbixSender.sendPowerConsumption("testHostname", 123.45);
		String receivedMessage = testServer.waitForNextMessage(10000);
		assertTrue("Contains hostname", receivedMessage.matches(".*\"data\":.*\\{.*\"host\":\"testHostname\".*\\}.*"));
		assertTrue("Contains power key", receivedMessage.matches(".*\"data\":.*\\{.*\"key\":\"datanode.power\".*\\}.*"));
		assertTrue("Contains value", receivedMessage.matches(".*\"data\":.*\\{.*\"value\":\"123.45\".*\\}.*"));
	}

	@Test
	public void testSendBandwidthUsage() throws InterruptedException {
		testServer.resetLastReceivedAgentMessage();
		this.zabbixSender.sendBandwidthUsage("testHostname", "testUsername", 123.45);
		String receivedMessage = testServer.waitForNextMessage(10000);
		assertTrue("Contains hostname", receivedMessage.matches(".*\"data\":.*\\{.*\"host\":\"testHostname\".*\\}.*"));
		assertTrue("Contains bandwidth key", receivedMessage.matches(".*\"data\":.*\\{.*\"key\":\"user.testUsername.bandwidth\".*\\}.*"));
		assertTrue("Contains value", receivedMessage.matches(".*\"data\":.*\\{.*\"value\":\"123.45\".*\\}.*"));
	}

	@Test
	public void testSendDuration() throws InterruptedException {
		testServer.resetLastReceivedAgentMessage();
		this.zabbixSender.sendDuration("testHostname", "testUsername", 123.45);
		String receivedMessage = testServer.waitForNextMessage(10000);
		assertTrue("Contains hostname", receivedMessage.matches(".*\"data\":.*\\{.*\"host\":\"testHostname\".*\\}.*"));
		assertTrue("Contains duration key", receivedMessage.matches(".*\"data\":.*\\{.*\"key\":\"user.testUsername.duration\".*\\}.*"));
		assertTrue("Contains value", receivedMessage.matches(".*\"data\":.*\\{.*\"value\":\"123.45\".*\\}.*"));
	}

	@Test
	public void testSendUserDataNodeConnection() throws InterruptedException {
		testServer.resetLastReceivedAgentMessage();
		this.zabbixSender.sendUserDataNodeConnection("dataNodeTestHostname", "testUsername", "testIP:4567");
		String receivedMessage = testServer.waitForNextMessage(10000);
		assertTrue("Contains hostname", receivedMessage.matches(".*\"data\":.*\\{.*\"host\":\"dataNodeTestHostname\".*\\}.*"));
		assertTrue("Contains last addr key", receivedMessage.matches(".*\"data\":.*\\{.*\"key\":\"user.testUsername.lastAddress\".*\\}.*"));
		assertTrue("Contains ip:port as value", receivedMessage.matches(".*\"data\":.*\\{.*\"value\":\"testIP:4567\"\\}.*"));
	}
}
