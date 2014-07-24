package de.tuberlin.cit.project.energy.hadoop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import de.tuberlin.cit.project.energy.hadoop.EnergyConservingDataNodeFilter.EnergyMode;

/**
 * Web front end tests.
 * 
 * @author Sascha
 */
public class WebFrontEndTest {
    private final static int TEST_WEB_PORT = 50100;
    private static EnergyConservingDataNodeFilter dataNodeFilter;
    private static WebFrontEnd webFrontEnd;

    @BeforeClass
    public static void setUp() throws Exception {
        dataNodeFilter = new EnergyConservingDataNodeFilter("127.0.0.1", 10051,
                "http://127.0.0.1/zabbix/api_jsonrpc.php", "dummy", "dummy");
        webFrontEnd = new WebFrontEnd(dataNodeFilter, TEST_WEB_PORT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        webFrontEnd.stopServer();
    }

    @Test
    public void testGetUserProfiles() throws Exception {
        dataNodeFilter.getUserEnergyMapping().clear();
        dataNodeFilter.getUserEnergyMapping().put("testUser", EnergyMode.DEFAULT);
        AsyncHttpClient httpClient = new AsyncHttpClient();
        Response response = httpClient.prepareGet("http://localhost:" + TEST_WEB_PORT + "/api/v1/user-profile").execute().get();

        assertEquals("Has http status ok", 200, response.getStatusCode());
        String result = response.getResponseBody().replace(" ", "").replace("\n", "");        
        assertTrue("Contains username", result.matches(".*\"username\":\"testUser\".*"));
        assertTrue("Contains energy mode", result.matches(".*\"profile\" ?: ?\"DEFAULT\".*"));
        assertTrue("Contains available profile default", result.matches(".*\"availableProfiles\":\\[.*\"DEFAULT\".*\\].*"));
        assertTrue("Contains available profile cheap", result.matches(".*\"availableProfiles\":\\[.*\"CHEAP\".*\\].*"));
        assertTrue("Contains available profile fast", result.matches(".*\"availableProfiles\":\\[.*\"FAST\".*\\].*"));
    }

    @Test
    public void testPutUserProfiles() throws Exception {
        dataNodeFilter.getUserEnergyMapping().clear();
        dataNodeFilter.getUserEnergyMapping().put("testUser", EnergyMode.DEFAULT);
        AsyncHttpClient httpClient = new AsyncHttpClient();
        Response response = httpClient.preparePut("http://localhost:" + TEST_WEB_PORT + "/api/v1/user-profile")
                .addParameter("username", "testUser").addParameter("profile", "fast").execute().get();
        assertEquals("Has http status ok", 200, response.getStatusCode());
        assertEquals("Update profile", EnergyMode.FAST, dataNodeFilter.getUserEnergyMapping().get("testUser"));
    }
}
