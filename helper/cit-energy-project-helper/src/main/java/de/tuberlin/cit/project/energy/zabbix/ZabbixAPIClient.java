package de.tuberlin.cit.project.energy.zabbix;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import javax.naming.AuthenticationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Response;

import de.tuberlin.cit.project.energy.zabbix.asynchttpclient.LooseTrustManager;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.exception.TemplateNotFoundException;
import de.tuberlin.cit.project.energy.zabbix.exception.UserNotFoundException;

/**
 * Access zabbix config via REST API.
 *
 * Configure Zabbix end point via System properties:
 * java ... -Dzabbix.restURL=https://...:100443/... -Dzabbix.username=admin -Dzabbix.password=...
 *
 * @author Sascha
 */
public class ZabbixAPIClient {
    private static final Log log = LogFactory.getLog(ZabbixAPIClient.class);

    public final String zabbixURL;
    public final String zabbixUsername;
    public final String zabbixPassword;

    public final AsyncHttpClient httpClient;
    public final ObjectMapper objectMapper = new ObjectMapper();

    private String authToken = null;
    /** Cached template id */
    private int dataNodeTemplateID = -1;

    public ZabbixAPIClient(String zabbixURL, String zabbixUsername, String zabbixPassword, AsyncHttpClientConfig config) {
        this.zabbixURL = zabbixURL;
        this.zabbixUsername = zabbixUsername;
        this.zabbixPassword = zabbixPassword;
        this.httpClient = new AsyncHttpClient(config);

        log.info("New ZabbixAPIClient initialized with endpoint " + this.zabbixURL + " and username " + this.zabbixUsername + ".");
    }

    public ZabbixAPIClient(String zabbixURL, String zabbixUsername, String zabbixPassword) throws KeyManagementException, NoSuchAlgorithmException {
        this.zabbixURL = zabbixURL;
        this.zabbixUsername = zabbixUsername;
        this.zabbixPassword = zabbixPassword;
        AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
        configBuilder.setSSLContext(LooseTrustManager.getSSLContext());
        this.httpClient = new AsyncHttpClient(configBuilder.build());

        log.info("New ZabbixAPIClient initialized with endpoint " + this.zabbixURL + " and username " + this.zabbixUsername + ".");
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

        log.info("New ZabbixAPIClient initialized with endpoint " + this.zabbixURL + " and username " + this.zabbixUsername + ".");
    }

    public ZabbixAPIClient() throws NoSuchAlgorithmException, KeyManagementException {
        this(System.getProperty("zabbix.restURL", ZabbixParams.DEFAULT_ZABBIX_REST_URL),
                System.getProperty("zabbix.username", ZabbixParams.DEFAULT_ZABBIX_USERNAME),
                System.getProperty("zabbix.password", ZabbixParams.DEFAULT_ZABBIX_PASSWORD));
    }

    /**
     * Authenticate against Zabbix if no authentication token and cache token.
     */
    public void authenticate() throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
            AuthenticationException, InternalErrorException {

        if (this.authToken == null) {
            ObjectNode params = this.objectMapper.createObjectNode();
            params.put("user", this.zabbixUsername);
            params.put("password", this.zabbixPassword);
            Response response = executeRPC("user.login", params);

            if (response.getStatusCode() == 200) {
                JsonNode jsonResponse = this.objectMapper.readTree(response.getResponseBody());

                if (jsonResponse.hasNonNull("result") && !jsonResponse.get("result").asText().isEmpty()) {
                    this.authToken = jsonResponse.get("result").asText();

                } else if(jsonResponse.has("error")) {
                    throw new AuthenticationException(jsonResponse.get("error").asText());

                } else
                    throw new InternalErrorException();

            } else
                throw new InternalErrorException();

        }
    }

    public void setAuthToken(String authToken) { this.authToken = authToken; }
    public String getAuthToken() { return authToken; }

    /**
     * Construct a JSON RPC body around given parameters with given method name.
     * Adds authentication token if present.
     * @see #authenticate()
     *
     * @param method
     * @param parameters params block in request
     * @return JSON request
     */
    public String getRPCBody(String method, ObjectNode parameters) {
        final ObjectNode m = this.objectMapper.createObjectNode();
        m.put("jsonrpc", "2.0");
        if (this.authToken != null)
            m.put("auth", this.authToken);
        m.put("method", method);
        if (parameters != null)
            m.with("params").setAll(parameters);
        else
            m.putArray("params"); // empty array, required by zabbix
        m.put("id", 1);

        return this.objectMapper.valueToTree(m).toString();
    }

    /**
     * Initiates a synchrony JSON RPC call to Zabbix REST API.
     */
    public Response executeRPC(String method, ObjectNode params) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        String rpcBody = getRPCBody(method, params);
        log.debug("Running json rpc with body: " + rpcBody);

        Response r = this.httpClient.preparePost(this.zabbixURL)
                .addHeader("Content-Type", "application/json-rpc")
                .setBody(rpcBody).execute().get();

        log.debug("Got json rpc response: " + r.getResponseBody());

        return r;
    }

    /**
     * Try to find username with given ip:port connected to given hostname.
     *
     * @param dataNodeName as hostname
     * @param clientAddress as ip:port
     */
    public String getUsernameByDataNodeConnection(String dataNodeName, String clientAddress)
            throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
            AuthenticationException, UserNotFoundException, InternalErrorException {

        this.authenticate();

        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("host", dataNodeName);
        params.with("search").put("key_", String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, "*"));
        params.put("searchWildcardsEnabled", true);
        params.with("filter").put("value", clientAddress);

        params.withArray("output").add("key_");
        params.withArray("output").add("lastvalue");
        params.withArray("output").add("lastclock");

        Response response = this.executeRPC("item.get", params);

        if (response.getStatusCode() == 200) {
            String usernameKey = null;
            long lastclock = Long.MIN_VALUE;

            JsonNode jsonResponse = objectMapper.readTree(response.getResponseBody());

            if (jsonResponse.get("result").isArray()) {
                for (JsonNode item : jsonResponse.get("result")) {
                    long currentLastclock = item.get("lastclock").asLong();
                    if (usernameKey == null || currentLastclock > lastclock) {
                        usernameKey = item.get("key_").asText();
                        lastclock = currentLastclock;
                    }
                }
            }

            if (usernameKey != null && System.currentTimeMillis()/1000 - ZabbixParams.MAX_USER_IP_MAPPING_AGE < lastclock) {
                String username = usernameKey.substring("user.".length());
                return username.substring(0, username.length() - ".lastAddress".length());
            } else
                throw new UserNotFoundException();
        } else
            throw new InternalErrorException();
    }

    /**
     * Fetches the DataNode template id from Zabbix and caches it.
     *
     * @return Cached or fetched template id from Zabbix.
     */
    public int getDataNodeTemplateID()
            throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
            AuthenticationException, TemplateNotFoundException, InternalErrorException {

        if (this.dataNodeTemplateID > 0) {
            return this.dataNodeTemplateID;

        } else {
            this.authenticate();

            ObjectNode params = this.objectMapper.createObjectNode();
            params.put("output", "templateid");
            params.with("filter").put("host", ZabbixParams.DATANODE_TEMPLATE_NAME);

            Response response = this.executeRPC("template.get", params);

            if (response.getStatusCode() == 200) {
                JsonNode jsonResponse = this.objectMapper.readTree(response.getResponseBody());
                if (jsonResponse.get("result").isArray() && jsonResponse.get("result").size() > 0) {
                    return jsonResponse.get("result").get(0).get("templateid").asInt();
                } else
                    throw new TemplateNotFoundException();
            } else
                throw new InternalErrorException();
        }
    }

    /**
     * Set cached template id manual. Used in tests.
     */
    public void setDataNodeTemplateID(int dataNodeTemplateID) {
        this.dataNodeTemplateID = dataNodeTemplateID;
    }

    /**
     * Checks if given username has a last address mapping key in DataNode template.
     */
    public boolean doesUserExistsInDataNodeTemplate(String username)
            throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
            AuthenticationException, InternalErrorException {

        this.authenticate();

        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("host", ZabbixParams.DATANODE_TEMPLATE_NAME);
        params.put("key_", String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, username));

        Response response = this.executeRPC("item.exists", params);

        if (response.getStatusCode() == 200) {
            return this.objectMapper.readTree(response.getResponseBody()).get("result").asBoolean();
        } else
            throw new InternalErrorException();
    }

    /**
     * Creates all user items in DataNode template.
     */
    public void createUserInDataNodeTemplate(String username)
            throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
            AuthenticationException, InternalErrorException, TemplateNotFoundException {
        this.authenticate();

        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("name", "Last client address from user " + username);
        params.put("key_", String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, username));
        params.put("delay", "30"); // dummy, but required value
        params.put("type", "2"); // zabbix trapper
        params.put("value_type", "4"); // text
        params.put("hostid", getDataNodeTemplateID()); // use template id as host

        if (this.executeRPC("item.create", params).getStatusCode() != 200)
            throw new InternalErrorException();

        // TODO: add more here
    }

    /**
     * Kills all connections and stops HTTP client threads.
     */
    public void quit() {
        this.httpClient.close();
    }
}
