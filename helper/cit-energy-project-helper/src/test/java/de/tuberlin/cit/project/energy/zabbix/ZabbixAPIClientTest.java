package de.tuberlin.cit.project.energy.zabbix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZabbixAPIClientTest {
	private static final int TEST_SERVER_PORT = 10052;
	private static ZabbixAPITestServer testServer;
	
	@BeforeClass
	public static void setUp() throws Exception {
		testServer = new ZabbixAPITestServer(TEST_SERVER_PORT);
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		testServer.stopServer();
	}

	@Test
	public void testAuthenticate() throws Exception {
		ZabbixAPIClient apiClient = new ZabbixAPIClient("http://localhost:"+TEST_SERVER_PORT+"/api", "zabbixTestUsername", "zabbixTestPassword");
		testServer.setNextResponse("{ \"jsonrpc\":\".20\", \"result\":\"testAuthToken\", \"id\":1 }");
		apiClient.authenticate();
		String request = testServer.getLastRequest();
		assertTrue("Contains user.login as method", request.matches(".*\"method\":\"user.login\".*"));
		assertTrue("Contains username as parameter", request.matches(".*\"params\":\\{.*\"user\":\"zabbixTestUsername\".*\\}.*"));
		assertTrue("Contains password as parameter", request.matches(".*\"params\":\\{.*\"password\":\"zabbixTestPassword\".*\\}.*"));
		assertEquals("Reads auth token from response", "testAuthToken", apiClient.getAuthToken());
	}
	
	@Test
	public void testGetUsernameByDataNodeConnection() throws Exception {
		ZabbixAPIClient apiClient = new ZabbixAPIClient("http://localhost:"+TEST_SERVER_PORT+"/api", "zabbixTestUsername", "zabbixTestPassword");
		apiClient.setAuthToken("testAuthToken");
		long lastclock = (System.currentTimeMillis() / 1000) - 5;
		testServer.setNextResponse("{ \"jsonrpc\":\"2.0\", \"id\":2, \"result\":["
				+ "{\"key_\":\"user.klaus.lastAddress\",\"lastvalue\":\"11.11.11.11:1111\",\"lastclock\":\"" + lastclock + "\"},"
				+ "{\"key_\":\"user.peterFirst.lastAddress\",\"lastvalue\":\"22.22.22.22:2222\",\"lastclock\":\"" + (lastclock - 10) + "\"},"
				+ "{\"key_\":\"user.peter.lastAddress\",\"lastvalue\":\"22.22.22.22:2222\",\"lastclock\":\"" + lastclock + "\"},"
				+ "{\"key_\":\"user.peterLast.lastAddress\",\"lastvalue\":\"22.22.22.22:2222\",\"lastclock\":\"" + (lastclock - 20) + "\"},"
				+ "{\"key_\":\"user.emil.lastAddress\",\"lastvalue\":\"33.33.33.33:3333\",\"lastclock\":\"" + lastclock + "\"}"
				+ "]}");
		String result = apiClient.getUsernameByDataNodeConnection("testDataNode", "22.22.22.22:2222");
		String request = testServer.getLastRequest();
		assertTrue("Request contains get.item method", request.matches(".*\"method\":\"item.get\".*"));
		assertTrue("Request contains user.*.lastAddress as key filter", request.matches(".*\"search\":\\{.*\"key_\":\"user\\.\\*\\.lastAddress\".*\\}.*"));
		assertEquals("Finds correct username from results", "peter", result);
	}

	@Test
	public void testGetDataNodeTemplateID() throws Exception {
		ZabbixAPIClient apiClient = new ZabbixAPIClient("http://localhost:"+TEST_SERVER_PORT+"/api", "zabbixTestUsername", "zabbixTestPassword");
		apiClient.setAuthToken("testAuthToken");
		testServer.setNextResponse("{ \"jsonrpc\":\".20\", \"result\":[{\"templateid\":\"12345\"}], \"id\":1 }");
		int result = apiClient.getDataNodeTemplateID();
		String request = testServer.getLastRequest();
		assertTrue("Contains template.get as method", request.matches(".*\"method\":\"template.get\".*"));
		assertTrue("Contains template name as host parameter", request.matches(".*\"params\":\\{.*\"host\":\"CIT DataNode\".*\\}.*"));
		assertEquals("Reads template id from response", 12345, result);
	}

	@Test
	public void testDoesUserExistsInDataNodeTemplate() throws Exception {
		ZabbixAPIClient apiClient = new ZabbixAPIClient("http://localhost:"+TEST_SERVER_PORT+"/api", "zabbixTestUsername", "zabbixTestPassword");
		apiClient.setAuthToken("testAuthToken");
		testServer.setNextResponse("{ \"jsonrpc\":\".20\", \"result\":false, \"id\":1 }");
		boolean result = apiClient.doesUserExistsInDataNodeTemplate("testUsername");
		String request = testServer.getLastRequest();
		assertTrue("Contains item.exists as method", request.matches(".*\"method\":\"item.exists\".*"));
		assertTrue("Contains template name as host parameter", request.matches(".*\"params\":\\{.*\"host\":\"CIT DataNode\".*\\}.*"));
		assertTrue("Contains user in key parameter", request.matches(".*\"params\":\\{.*\"key_\":\"user.testUsername.lastAddress\".*\\}.*"));
		assertEquals("Reads result from response", false, result);
	}
}