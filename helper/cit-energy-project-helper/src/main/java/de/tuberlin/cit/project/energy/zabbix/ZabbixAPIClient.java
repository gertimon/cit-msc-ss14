package de.tuberlin.cit.project.energy.zabbix;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.ajax.JSON;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Response;

import de.tuberlin.cit.project.energy.zabbix.asynchttpclient.LooseTrustManager;

/**
 * Access zabbix config via REST API.
 * 
 * Configure Zabbix endpoint via System properties:
 * java ... -Dzabbix.restURL=https://...:100443/... -Dzabbix.username=admin -Dzabbix.password=...
 *  
 * @author Sascha
 */
public class ZabbixAPIClient {
	private static final Log log = LogFactory.getLog(ZabbixAPIClient.class);
	public static final String DEFAULT_ZABBIX_URL = "https://mpjss14.cit.tu-berlin.de/zabbix/api_jsonrpc.php";
	public static final String DEFAULT_ZABBIX_USERNAME = "admin";
	public static final String DEFAULT_ZABBIX_PASSWORD = "zabbix!";
	
	public static final int MAX_USER_IP_MAPPING_AGE = 60*60; // use only values updated within the given time frame (in seconds)

	public final String zabbixURL;
	public final String zabbixUsername;
	public final String zabbixPassword;
	
	public final AsyncHttpClient httpClient;
	
	public String authToken = null;
	
	public ZabbixAPIClient(String zabbixURL, String zabbixUsername, String zabbixPassword, AsyncHttpClientConfig config) {
		this.zabbixURL = zabbixURL;
		this.zabbixUsername = zabbixUsername;
		this.zabbixPassword = zabbixPassword;
		this.httpClient = new AsyncHttpClient(config);
	}

	public ZabbixAPIClient(String zabbixURL, String zabbixUsername, String zabbixPassword) throws KeyManagementException, NoSuchAlgorithmException {
		this.zabbixURL = zabbixURL;
		this.zabbixUsername = zabbixUsername;
		this.zabbixPassword = zabbixPassword;
		AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
		configBuilder.setSSLContext(LooseTrustManager.getSSLContext());
		this.httpClient = new AsyncHttpClient(configBuilder.build());
	}

	public ZabbixAPIClient(String zabbixURL, String zabbixUsername, String zabbixPassword, String httpAuthUsername, String httpAuthPassword) throws NoSuchAlgorithmException, KeyManagementException {
		this.zabbixURL = zabbixURL;
		this.zabbixUsername = zabbixUsername;
		this.zabbixPassword = zabbixPassword;

		AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
		configBuilder.setSSLContext(LooseTrustManager.getSSLContext());
		configBuilder.setRealm((new Realm.RealmBuilder())
			.setPrincipal(httpAuthUsername)
			.setPassword(httpAuthPassword)
			.setUsePreemptiveAuth(true)
			.setScheme(AuthScheme.BASIC)
			.build()
		);
		this.httpClient = new AsyncHttpClient(configBuilder.build());
	}

	public ZabbixAPIClient() throws NoSuchAlgorithmException, KeyManagementException {
		this(System.getProperty("zabbix.restURL", DEFAULT_ZABBIX_URL),
				System.getProperty("zabbix.username", DEFAULT_ZABBIX_USERNAME),
				System.getProperty("zabbix.passwort", DEFAULT_ZABBIX_PASSWORD));
	}
	
	@SuppressWarnings("unchecked")
	public boolean authenticate() throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
		if (this.authToken != null) {
			return true;
		} else {
	        final Map<String, Object> m = new TreeMap<String, Object>();
	        m.put("user", this.zabbixUsername);
	        m.put("password", this.zabbixPassword);
	        Response response = executeRPC("user.authenticate", m);
	        
	        if (response.getStatusCode() == 200) {
	        	Object parsedBody = JSON.parse(response.getResponseBody());
	        	
	        	if (parsedBody instanceof Map) {
	                this.authToken = (String) ((Map<String, Object>) parsedBody).get("result");
	                log.debug("Got authentication token: " + this.authToken);
	            	return true;
	        	}
	        }
	        
	        return false;
		}
	}

	public String getRPCBody(String method, Map<String, Object> params) {
        final Map<String, Object> m = new TreeMap<String, Object>();
        m.put("jsonrpc", "2.0");
		if (this.authToken != null)
			m.put("auth", this.authToken);
        m.put("method", method);
        if (params != null)
        	m.put("params", params);
        else
        	m.put("params", new int[]{});
        m.put("id", 1);
		
        return JSON.toString(m);
	}

	public Response executeRPC(String method, Map<String, Object> params) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        String rpcBody = getRPCBody(method, params);
        log.debug("Running json rpc with body: " + rpcBody);
        
        Response r = this.httpClient.preparePost(this.zabbixURL)
        		.addHeader("Content-Type", "application/json-rpc")
        		.setBody(rpcBody).execute().get();
        
        log.debug("Got json rpc response: " + r.getResponseBody());
        
        return r;
	}
	
	
	@SuppressWarnings("unchecked")
	public String getUsernameByIP(String ip) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
	    this.authenticate();

	    Map<String, Object> params = new TreeMap<String, Object>();
		params.put("host", ZabbixSender.USER_CLIENT_MAPPING_DUMMY_HOST);
		Map<String, Object> search = new TreeMap<String, Object>();
		search.put("key_", "user.*.ip");
	    params.put("search",  search);
	    params.put("searchWildcardsEnabled", true);
	    params.put("output", new String[]{ "key_", "lastvalue", "lastclock" });
	    
	    Response response = this.executeRPC("item.get", params);
	    
	    if (response.getStatusCode() == 200) {
	    	Map<String, Object> responseJson = (Map<String, Object>) JSON.parse(response.getResponseBody());
	    	String usernameKey = null;
	    	int lastclock = Integer.MIN_VALUE;
	    	
	    	if (responseJson.get("result").getClass().isArray()) {
	    		for(Object itemObject : (Object[])responseJson.get("result")) {
	    			Map<String, String> item = (Map<String, String>) itemObject;

	    			if (item.get("lastvalue").equalsIgnoreCase(ip)) {
	    				int currentLastclock = Integer.parseInt(item.get("lastclock"));
	    				if (usernameKey == null || currentLastclock > lastclock) {
	    					usernameKey = item.get("key_");
	    					lastclock = currentLastclock;
	    				}
	    			}
	    		}

	    	}
	    	
	    	if (usernameKey != null && System.currentTimeMillis()/1000 - MAX_USER_IP_MAPPING_AGE < lastclock) {
				String username = usernameKey.substring("user.".length());
				return username.substring(0, username.length() - ".ip".length());
	    	}
	    }
	    
	    return null;
	}
	
	public void quit() {
		this.httpClient.close();
	}

}
