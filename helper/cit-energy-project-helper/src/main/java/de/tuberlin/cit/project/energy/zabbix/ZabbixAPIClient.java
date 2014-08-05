package de.tuberlin.cit.project.energy.zabbix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import de.tuberlin.cit.project.energy.zabbix.model.DatanodeUserConnection;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.naming.AuthenticationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Access zabbix config via REST API.
 *
 * Configure Zabbix end point via System properties: java ...
 * -Dzabbix.restURL=https://...:100443/... -Dzabbix.username=admin
 * -Dzabbix.password=...
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
    /**
     * Cached template id
     */
    private int dataNodeUserTemplateID = -1;

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

    public ObjectMapper getObjectMapper() {
        return objectMapper;
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

                } else if (jsonResponse.has("error")) {
                    throw new AuthenticationException(jsonResponse.get("error").asText());

                } else {
                    throw new InternalErrorException();
                }

            } else {
                throw new InternalErrorException();
            }

        }
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    /**
     * Construct a JSON RPC body around given parameters with given method name.
     * Adds authentication token if present.
     *
     * @see #authenticate()
     *
     * @param method
     * @param parameters params block in request
     * @return JSON request
     */
    public String getRPCBody(String method, JsonNode parameters) {
        final ObjectNode m = this.objectMapper.createObjectNode();
        m.put("jsonrpc", "2.0");
        if (this.authToken != null) {
            m.put("auth", this.authToken);
        }
        m.put("method", method);
        if (parameters != null) {
            if (parameters instanceof ArrayNode) {
                m.withArray("params").addAll((ArrayNode) parameters);
            } else if (parameters instanceof ObjectNode) {
                m.with("params").setAll((ObjectNode) parameters);
            }
        } else {
            m.putArray("params"); // empty array, required by zabbix
        }
        m.put("id", 1);

        return this.objectMapper.valueToTree(m).toString();
    }

    /**
     * Initiates a synchrony JSON RPC call to Zabbix REST API.
     */
    public Response executeRPC(String method, JsonNode params) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
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
     * @param datanodeName as hostname
     * @param clientAddress as ip:port
     */
    public DatanodeUserConnection getUsernameByDataNodeConnection(String datanodeName, String clientAddress)
        throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
        AuthenticationException, UserNotFoundException, InternalErrorException {

        this.authenticate();

        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("host", datanodeName);
        params.with("search").put("key_", "user.*.last*Address"); // accepts only one value, no array!
        params.put("searchWildcardsEnabled", true);
        // filter doesn't work with text fields...

        params.withArray("output").add("key_");
        params.withArray("output").add("lastvalue");
        params.withArray("output").add("lastclock");

        List<ZabbixItem> items = getItems(params);
        String usernameKey = null;
        long lastclock = Long.MIN_VALUE;

        // find item with matching client id and highest clock value
        for (ZabbixItem item : items) {
            if (item.getLastValue() != null && item.getLastValue().equals(clientAddress)
                    && (usernameKey == null || item.getLastClock() > lastclock)) {
                usernameKey = item.getKey();
                lastclock = item.getLastClock();
            }
        }

        if (usernameKey != null && System.currentTimeMillis() / 1000 - ZabbixParams.MAX_USER_IP_MAPPING_AGE < lastclock) {
            if (usernameKey.matches(String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, ".*"))) {
                String username = usernameKey.replaceFirst(
                    String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, "(.*)\\"), "$1");
                return new DatanodeUserConnection(clientAddress, datanodeName, username, false);

            } else if (usernameKey.matches(String.format(ZabbixParams.USER_LAST_INTERNAL_ADDRESS_MAPPING_KEY, ".*"))) {
                String username = usernameKey.replaceFirst(
                    String.format(ZabbixParams.USER_LAST_INTERNAL_ADDRESS_MAPPING_KEY, "(.*)"), "$1");
                return new DatanodeUserConnection(clientAddress, datanodeName, username, true);

            } else {
                throw new InternalErrorException("Unknown key in mapping found: " + usernameKey);
            }
        } else {
            throw new UserNotFoundException();
        }
    }

    /**
     * Fetches the DataNode template id from Zabbix and caches it.
     *
     * @return Cached or fetched template id from Zabbix.
     */
    public int getDataNodeTemplateId()
        throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
        AuthenticationException, TemplateNotFoundException, InternalErrorException {

        if (this.dataNodeUserTemplateID > 0) {
            return this.dataNodeUserTemplateID;

        } else {
            this.authenticate();

            ObjectNode params = this.objectMapper.createObjectNode();
            params.put("output", "templateid");
            params.with("filter").put("host", ZabbixParams.DATANODE_USER_TEMPLATE_NAME);

            Response response = this.executeRPC("template.get", params);

            if (response.getStatusCode() == 200) {
                JsonNode jsonResponse = this.objectMapper.readTree(response.getResponseBody());
                if (jsonResponse.get("result").isArray() && jsonResponse.get("result").size() > 0) {
                    this.dataNodeUserTemplateID = jsonResponse.get("result").get(0).get("templateid").asInt();
                    return this.dataNodeUserTemplateID;
                } else {
                    throw new TemplateNotFoundException();
                }
            } else {
                throw new InternalErrorException();
            }
        }
    }

    /**
     * Set cached template id manual. Used in tests.
     */
    public void setDataNodeUserTemplateId(int dataNodeTemplateId) {
        this.dataNodeUserTemplateID = dataNodeTemplateId;
    }

    /**
     * Fetches the application id with given name from data node user template.
     *
     * @return application id from Zabbix if found or -1 otherwise
     */
    public int getApplictionId(String name)
        throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
        AuthenticationException, TemplateNotFoundException, InternalErrorException {
        this.authenticate();

        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("hostids", getDataNodeTemplateId());

        Response response = this.executeRPC("application.get", params);

        if (response.getStatusCode() == 200) {
            JsonNode jsonResponse = this.objectMapper.readTree(response.getResponseBody());
            if (jsonResponse.get("result").isArray() && jsonResponse.get("result").size() > 0) {
                for (JsonNode o : jsonResponse.get("result")) {
                    if (o.get("name").asText().equalsIgnoreCase(name)) {
                        return o.get("applicationid").asInt();
                    }
                }

                return -1;
            } else {
                throw new TemplateNotFoundException();
            }
        } else {
            throw new InternalErrorException();
        }
    }

    /**
     * Checks if given username has a last address mapping key in DataNode
     * template.
     */
    public boolean doesUserExistsInDataNodeTemplate(String username)
        throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
        AuthenticationException, InternalErrorException {

        this.authenticate();

        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("host", ZabbixParams.DATANODE_USER_TEMPLATE_NAME);
        params.put("key_", String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, username));

        Response response = this.executeRPC("item.exists", params);

        if (response.getStatusCode() == 200) {
            return this.objectMapper.readTree(response.getResponseBody()).get("result").asBoolean();
        } else {
            throw new InternalErrorException();
        }
    }

    /**
     * Creates all user items in DataNode template.
     */
    public void createUserInDataNodeTemplate(String username)
        throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
        AuthenticationException, InternalErrorException, TemplateNotFoundException {
        this.authenticate();

        // find HDFS application id
        int appId = getApplictionId("HDFS");

        // external client connection <-> user mapping
        ObjectNode text = this.objectMapper.createObjectNode();
        text.put("name", "Last client address from user " + username);
        text.put("key_", String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, username));
        text.put("type", "2"); // zabbix trapper
        text.put("value_type", "4"); // text
        text.put("hostid", getDataNodeTemplateId()); // use template id as host
        if (appId > 0) {
            text.withArray("applications").add(appId);
        }
        if (this.executeRPC("item.create", text).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // internal datanode connection <-> user mapping
        text.put("name", "Last internal datanode address from user " + username);
        text.put("key_", String.format(ZabbixParams.USER_LAST_INTERNAL_ADDRESS_MAPPING_KEY, username));
        if (this.executeRPC("item.create", text).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // bandwidth usage
        ObjectNode numeric = this.objectMapper.createObjectNode();
        numeric.put("name", "Bandwidth consumed by user " + username);
        numeric.put("key_", String.format(ZabbixParams.USER_BANDWIDTH_KEY, username));
        numeric.put("delay", "30"); // dummy, but required value
        numeric.put("type", "2"); // zabbix trapper
        numeric.put("value_type", "0"); // numeric float
        numeric.put("units", "Byte/s");
        numeric.put("hostid", getDataNodeTemplateId()); // use template id as host
        if (appId > 0) {
            numeric.withArray("applications").add(appId);
        }
        if (this.executeRPC("item.create", numeric).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // internal bandwidth usage
        numeric.put("name", "Bandwidth internal consumed by user " + username);
        numeric.put("key_", String.format(ZabbixParams.USER_INTERNAL_BANDWIDTH_KEY, username));
        if (this.executeRPC("item.create", numeric).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // data usage (delta)
        numeric.put("name", "Allocated data space change by user " + username);
        numeric.put("key_", String.format(ZabbixParams.USER_DATA_USAGE_DELTA_KEY, username));
        numeric.put("units", "Byte");
        numeric.put("value_type", "0"); // numeric float
        if (this.executeRPC("item.create", numeric).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // complete data usage
        numeric.put("name", "Complete allocated data space by user " + username);
        numeric.put("key_", String.format(ZabbixParams.USER_DATA_USAGE_KEY, username));
        numeric.put("value_type", "3"); // numeric unsigned
        if (this.executeRPC("item.create", numeric).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // block events
        text.put("name", "Create/remove block events performed by user " + username);
        text.put("key_", String.format(ZabbixParams.USER_BLOCK_EVENTS_KEY, username));
        text.put("value_type", "2"); // log
        if (this.executeRPC("item.create", text).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // last used profile
        text.put("name", "Last energy profile used by user " + username);
        text.put("key_", String.format(ZabbixParams.USER_LAST_USED_PROFILE_KEY, username));
        text.put("value_type", "2"); // log
        if (this.executeRPC("item.create", text).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // TODO: add more here
    }

    /**
     * Removes all user items in DataNode template.
     *
     * @return removed items
     */
    public List<ZabbixItem> deleteUserInDataNodeTemplate(String username)
        throws IllegalArgumentException, InterruptedException, ExecutionException, IOException,
        AuthenticationException, InternalErrorException, TemplateNotFoundException {
        this.authenticate();

        // find item ids
        ObjectNode searchParams = this.objectMapper.createObjectNode();
        String userKeys[] = {
            ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY,
            ZabbixParams.USER_LAST_INTERNAL_ADDRESS_MAPPING_KEY,
            ZabbixParams.USER_BANDWIDTH_KEY,
            ZabbixParams.USER_INTERNAL_BANDWIDTH_KEY,
            ZabbixParams.USER_DATA_USAGE_DELTA_KEY,
            ZabbixParams.USER_BLOCK_EVENTS_KEY
        };
        for (String key : userKeys) {
            searchParams.with("filter").withArray("key_").add(String.format(key, username));
        }
        searchParams.put("hostids", getDataNodeTemplateId()); // use template id as host
        searchParams.withArray("output").add("itemid");
        searchParams.withArray("output").add("key_");
        List<ZabbixItem> items = getItems(searchParams);

        if (items.size() > 0) {
            int ids[] = new int[items.size()];
            for (int i = 0; i < items.size(); i++) {
                ids[i] = items.get(i).getItemId();
            }
            deleteItems(ids);

            return items;
        } else {
            return new ArrayList<ZabbixItem>(0);
        }
    }

    /**
     * Implements item.get from Zabbix API.
     *
     * @param jsonFilter e.g. search, itemids, limit, time_from, time_till
     * @see
     * https://www.zabbix.com/documentation/2.2/manual/api/reference/item/get
     * @return List with values or empty list if no items match
     */
    public List<ZabbixItem> getItems(ObjectNode filterParams) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException {
        this.authenticate();

        if (!filterParams.has("output")) {
            filterParams.put("output", "extend");
        }

        Response response = this.executeRPC("item.get", filterParams);

        if (response.getStatusCode() == 200) {
            JsonNode jsonResponse = objectMapper.readTree(response.getResponseBody());

            if (jsonResponse.get("result").isArray() && jsonResponse.get("result").size() > 0) {
                List<ZabbixItem> resultList = new ArrayList<ZabbixItem>(jsonResponse.get("result").size());

                for (JsonNode item : jsonResponse.get("result")) {
                    resultList.add(this.objectMapper.readValue(item.traverse(), ZabbixItem.class));
                }

                return resultList;
            } else {
                return new ArrayList<ZabbixItem>(0);
            }
        } else {
            throw new InternalErrorException();
        }
    }
    
    /**
     * Method for getting Zabbix Items using hostname and itemkey.
     * @param hostName Hostname of the Server where the Item is stored
     * @param itemKey identification key for the ZabbixItem
     * @return 
     * @throws javax.naming.AuthenticationException 
     * @throws java.lang.InterruptedException 
     * @throws java.util.concurrent.ExecutionException 
     * @throws de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException 
     * @throws java.io.IOException 
     */
    public List<ZabbixItem> getItems(String hostName, String itemKey) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException{
        
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output","extend");
        params.put("host",hostName);
        params.with("search").put("key_",itemKey);
        return this.getItems(params);
    }
    /**
     * Implements item.delete from Zabbix API.
     *
     * @param itemIds
     * @see
     * https://www.zabbix.com/documentation/2.2/manual/api/reference/item/delete
     * @return true if succeed, throws exception otherwise
     */
    public boolean deleteItems(int[] itemIds) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException {
        this.authenticate();

        if (itemIds.length == 0) {
            throw new IllegalArgumentException("No item id's provided.");
        }

        ArrayNode params = this.objectMapper.createArrayNode();
        for (int id : itemIds) {
            params.add(id + "");
        }
        Response response = this.executeRPC("item.delete", params);

        System.out.println("Request: " + params);
        System.out.println("Got result: " + response.getResponseBody());

        if (response.getStatusCode() == 200) {
            return true;
        } else {
            throw new InternalErrorException("Got HTTP status code: " + response.getStatusCode());
        }
    }

    /**
     * Implements history.get from Zabbix API.
     *
     * @param jsonFilter e.g. search, itemids, limit, time_from, time_till
     * @see
     * https://www.zabbix.com/documentation/2.2/manual/api/reference/history/get
     * @return List with values or empty list if no items match
     */
    public List<ZabbixHistoryObject> getHistory(ObjectNode filterParams) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException {
        this.authenticate();

        if (!filterParams.has("output")) {
            filterParams.put("output", "extend");
        }
        if (!filterParams.has("history")) {
            filterParams.put("history", "4"); // 4 = text
        }
        Response response = this.executeRPC("history.get", filterParams);

        if (response.getStatusCode() == 200) {
            JsonNode jsonResponse = objectMapper.readTree(response.getResponseBody());

            if (jsonResponse.get("result").isArray() && jsonResponse.get("result").size() > 0) {
                List<ZabbixHistoryObject> resultList = new ArrayList<ZabbixHistoryObject>(jsonResponse.get("result").size());

                for (JsonNode item : jsonResponse.get("result")) {
                    resultList.add(this.objectMapper.readValue(item.traverse(), ZabbixHistoryObject.class));
                }

                return resultList;
            } else {
                return new ArrayList<ZabbixHistoryObject>(0);
            }
        } else {
            throw new InternalErrorException();
        }
    }

    /**
     * Method for requesting numeric history from Zabbix on a given hostname and
     * itemkey.
     *
     * @param hostName specified hostname on which data is being requested
     * @param itemKey specified itemkey for data
     * @param timeFrom beginning of the timeframe for data request (in seconds,
     * UNIX timestamp)
     * @param timeTill end of the timeframe for data request (in seconds, UNIX
     * timestamp)
     * @return List numeric history data
     * @throws javax.naming.AuthenticationException
     * @throws
     * de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     * @throws java.io.IOException
     */
    public List<ZabbixHistoryObject> getNumericHistory(String hostName, String itemKey, long timeFrom, long timeTill) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException {

        //request for itemID
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("host", hostName);
        params.with("search").put("key_", itemKey);

        int itemID = this.getItems(params).get(0).getItemId();

        //request for item history in between fromTime and tillTime
        params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("history", 3);
        params.put("itemids", itemID);
        params.put("time_from", timeFrom);
        params.put("time_till", timeTill);

        return this.getHistory(params);
    }

    /**
     * Method for requesting numeric history from Zabbix on a given hostname and
     * itemkey including preceding Element to the timeFrom timestamp.
     *
     * @param hostName specified hostname on which data is being requested
     * @param itemKey specified itemkey for data
     * @param timeFrom beginning of the timeframe for data request (in seconds,
     * UNIX timestamp)
     * @param timeTill end of the timeframe for data request (in seconds, UNIX
     * timestamp)
     * @return List numeric history data including the preceding history element
     * @throws javax.naming.AuthenticationException
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     * @throws
     * de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException
     */
    public List<ZabbixHistoryObject> getNumericHistoryWithPrecedingElement(String hostName, String itemKey, long timeFrom, long timeTill) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException {

        //request for itemID
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("host", hostName);
        params.with("search").put("key_", itemKey);

        int itemID = this.getItems(params).get(0).getItemId();

        //request preceding element
        params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("history", 3);
        params.put("itemids", itemID);
        params.put("limit", 1);
        params.put("time_till", timeFrom);
        params.put("sortfield", "clock");
        params.put("sortorder", "DESC");

        ZabbixHistoryObject precedingElement = this.getHistory(params).get(0);

        //request for item history in between fromTime and tillTime
        params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("history", 3);
        params.put("itemids", itemID);
        params.put("time_from", timeFrom);
        params.put("time_till", timeTill);

        List<ZabbixHistoryObject> result = this.getHistory(params);
        result.add(0, precedingElement);

        return result;
    }

    /**
     * Kills all connections and stops HTTP client threads.
     */
    public void quit() {
        this.httpClient.close();
    }
}
