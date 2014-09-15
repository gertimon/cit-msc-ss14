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
import de.tuberlin.cit.project.energy.zabbix.exception.HostGroupNotFoundException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Creates all user items in DataNode template. (additionally updates calculated items, creates graphs) 
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

        //external(user) bandwidth usage
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

        // internal(HDFS data replication) bandwidth usage
        numeric.put("name", "Bandwidth internal consumed by user " + username);
        numeric.put("key_", String.format(ZabbixParams.USER_INTERNAL_BANDWIDTH_KEY, username));
        if (this.executeRPC("item.create", numeric).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // user data usage (delta)
        numeric.put("name", "Allocated data space change by user " + username);
        numeric.put("key_", String.format(ZabbixParams.USER_DATA_USAGE_DELTA_KEY, username));
        numeric.put("units", "Byte");
        numeric.put("value_type", "0"); // numeric float
        if (this.executeRPC("item.create", numeric).getStatusCode() != 200) {
            throw new InternalErrorException();
        }

        // user data usage
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

        //Requesting itemids for graph creation
        int userDataUsageId = -1;
        int userBandwidthUsageId = -1;
        int allDataUsageId = -1;
        int allBandwidthUsageId = -1;
        // - user bandwidth id
        ObjectNode idRequest = this.objectMapper.createObjectNode();
        idRequest.put("output", "extend");
        idRequest.put("hostids", getDataNodeTemplateId());
        idRequest.with("search").put("key_", String.format(ZabbixParams.USER_BANDWIDTH_KEY, username));
        List<ZabbixItem> idResponse = this.getItems(idRequest);
        if (idResponse.size() > 0) {
            for (ZabbixItem idResponseItem : idResponse) {
                if (idResponseItem.getKey().equals(String.format(ZabbixParams.USER_BANDWIDTH_KEY,username)))
                    userBandwidthUsageId = idResponseItem.getItemId();
                }
            }
        // - user data usage id
        idRequest.with("search").put("key_", String.format(ZabbixParams.USER_DATA_USAGE_KEY, username));
        idResponse = this.getItems(idRequest);
        if (idResponse.size() > 0) {
            for (ZabbixItem idResponseItem : idResponse) {
                if (idResponseItem.getKey().equals(String.format(ZabbixParams.USER_DATA_USAGE_KEY,username)))
                    userDataUsageId = idResponseItem.getItemId();
                }
            }
        // - all users bandwidth id
        idRequest.with("search").put("key_", String.format(ZabbixParams.USER_BANDWIDTH_KEY, "all"));
        idResponse = this.getItems(idRequest);
        if (idResponse.size() > 0) {
            for (ZabbixItem idResponseItem : idResponse) {
                if (idResponseItem.getKey().equals(String.format(ZabbixParams.USER_BANDWIDTH_KEY,"all")))
                    allBandwidthUsageId = idResponseItem.getItemId();
                }
            }
        // - all users data usage id
        idRequest.with("search").put("key_", String.format(ZabbixParams.USER_DATA_USAGE_KEY, "all"));
        idResponse = this.getItems(idRequest);
        if (idResponse.size() > 0) {
            for (ZabbixItem idResponseItem : idResponse) {
                if (idResponseItem.getKey().equals(String.format(ZabbixParams.USER_DATA_USAGE_KEY, "all")))
                    allDataUsageId = idResponseItem.getItemId();
                }
            }

        // send initial values (0's) where required to not break the calculated items
        // TODO: test that initial values are actually being received by zabbix
//        ZabbixSender sender = new ZabbixSender();
//        List<String> hosts = getHosts(ZabbixParams.DATANODE_HOST_GROUP_NAME);
//        long clock = System.currentTimeMillis()/1000;
//        for(String host : hosts) {
//            System.out.println("Sending Data to " + host);
//            sender.sendDataUsage(host, username, 0, clock);
//            sender.sendBandwidthUsage(host, username, 0, clock);
//        }

        // update calculated items
        this.updateCalculatedItem(String.format(ZabbixParams.USER_BANDWIDTH_KEY, "all"), String.format(ZabbixParams.USER_BANDWIDTH_KEY, username), true);
        this.updateCalculatedItem(String.format(ZabbixParams.USER_DATA_USAGE_KEY, "all"), String.format(ZabbixParams.USER_DATA_USAGE_KEY, username), true);

        //create graphs
        this.createGraph(username + ":Bandwidth consumption", userBandwidthUsageId, allBandwidthUsageId);
        this.createGraph(username + ":allocated space", userDataUsageId, allDataUsageId);

        //TODO: uncomment when sender does send data as intended
//        sender.quit();
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
            ZabbixParams.USER_LAST_USED_PROFILE_KEY,
            ZabbixParams.USER_BANDWIDTH_KEY,
            ZabbixParams.USER_INTERNAL_BANDWIDTH_KEY,
            ZabbixParams.USER_DATA_USAGE_DELTA_KEY,
            ZabbixParams.USER_BLOCK_EVENTS_KEY,
            ZabbixParams.USER_DATA_USAGE_KEY
        };
        for (String key : userKeys) {
            searchParams.with("filter").withArray("key_").add(String.format(key, username));
        }
        searchParams.put("hostids", getDataNodeTemplateId()); // use template id as host
        searchParams.withArray("output").add("itemid");
        searchParams.withArray("output").add("key_");
        List<ZabbixItem> items = getItems(searchParams);
        //find graph ids
        searchParams = this.objectMapper.createObjectNode();
        searchParams.put("hostids", getDataNodeTemplateId());
        searchParams.withArray("output").add("graphid");
        searchParams.with("search").put("name", username + ":");
        Response response = this.executeRPC("graph.get", searchParams);

        if (response.getStatusCode() == 200) {
            JsonNode jsonResponse = objectMapper.readTree(response.getResponseBody());
            if (jsonResponse.get("result").size() > 0) {
                int graphIds[] = new int[jsonResponse.get("result").size()];
                for (int counter = 0; counter < jsonResponse.get("result").size(); counter++) {
                    graphIds[counter] = jsonResponse.get("result").get(counter).findValue("graphid").asInt();
                }
                //delte graphs for given user
                this.graphDelete(graphIds);
            }
        }

        if (items.size() > 0) {
            int ids[] = new int[items.size()];
            for (int i = 0; i < items.size(); i++) {
                ids[i] = items.get(i).getItemId();
            }
            // delete items for given username
            deleteItems(ids);
            // remove entrys in calculated items for given username
            this.updateCalculatedItem(String.format(ZabbixParams.USER_BANDWIDTH_KEY, "all"), String.format(ZabbixParams.USER_BANDWIDTH_KEY, username), false);
            this.updateCalculatedItem(String.format(ZabbixParams.USER_DATA_USAGE_KEY, "all"), String.format(ZabbixParams.USER_DATA_USAGE_KEY, username), false);

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
    public List<ZabbixItem> getItems(String hostName, String itemKey) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException {

        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("host", hostName);
        params.with("search").put("key_", itemKey);
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
                return new ArrayList<>(0);
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
     * @param timeFromSeconds beginning of the timeframe for data request (in
     * seconds, UNIX timestamp)
     * @param timeTillSeconds end of the timeframe for data request (in seconds,
     * UNIX timestamp)
     * @return List numeric history data
     */
    public List<ZabbixHistoryObject> getNumericHistory(String hostName, String itemKey, long timeFromSeconds,
            long timeTillSeconds) throws AuthenticationException, IllegalArgumentException, InterruptedException,
            ExecutionException, IOException, InternalErrorException {

        return getNumericHistory(new String[]{ hostName }, itemKey, timeFromSeconds, timeTillSeconds);
    }

    /**
     * Method for requesting numeric history from Zabbix on a given hostnames and
     * itemkey.
     *
     * @param hostName specified hostname on which data is being requested
     * @param itemKey specified itemkey for data
     * @param timeFromSeconds beginning of the timeframe for data request (in
     * seconds, UNIX timestamp)
     * @param timeTillSeconds end of the timeframe for data request (in seconds,
     * UNIX timestamp)
     * @return List numeric history data
     */
    public List<ZabbixHistoryObject> getNumericHistory(String hostNames[], String itemKey, long timeFromSeconds,
            long timeTillSeconds) throws AuthenticationException, IllegalArgumentException, InterruptedException,
            ExecutionException, IOException, InternalErrorException {

        // find item id's
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        for (String hostname : hostNames)
            params.withArray("host").add(hostname);
        params.with("search").put("key_", itemKey);
        
        int itemID = -1;
        List<ZabbixItem> itemIDcheck = this.getItems(params);
        //check for item existence, result is empty if item doesnt exist
        if (itemIDcheck.size() > 0) {
            itemID = itemIDcheck.get(0).getItemId();
        } else {
            //no item exists for given itemKey, thus empty result list is returned
            return new ArrayList<>(0);
        }

        //request for item history in between timeFromSeconds and timeTillSeconds
        params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("history", 3);
        params.put("itemids", itemID);
        params.put("time_from", timeFromSeconds);
        params.put("time_till", timeTillSeconds);
        
        List<ZabbixHistoryObject> result = this.getHistory(params);
        //check for adding usernames
        if (result.size() > 0 && itemKey.startsWith("user.")) {
            //data is userspecific and username is added to results
            String name = itemKey.split("\\.")[1];
            for (ZabbixHistoryObject item : result) {
                item.setUserName(name);
            }
            return result;
        } else {
            //return list, empty or not userspecific
            return result;
        }
    }

    /**
     * Method for requesting numeric history from Zabbix on a given hostname and
     * itemkey including preceding Element to the timeFromSeconds timestamp.
     *
     * @param hostName specified hostname on which data is being requested
     * @param itemKey specified itemkey for data
     * @param timeFromSeconds beginning of the timeframe for data request (in
     * seconds, UNIX timestamp)
     * @param timeTillSeconds end of the timeframe for data request (in seconds,
     * UNIX timestamp)
     * @return List numeric history data including the preceding history element
     * @throws javax.naming.AuthenticationException
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     * @throws
     * de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException
     */
    public List<ZabbixHistoryObject> getNumericHistoryWithPrecedingElement(String hostName, String itemKey, long timeFromSeconds, long timeTillSeconds) throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException {

        //request for itemID
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("host", hostName);
        params.with("search").put("key_", itemKey);

        int itemID = -1;
        List<ZabbixItem> itemIDcheck = this.getItems(params);
        //check for item existence, result is empty if item doesnt exist
        if (itemIDcheck.size() > 0) {
            itemID = itemIDcheck.get(0).getItemId();
        } else {
            //no item exists for given itemKey, thus empty result list is returned
            return new ArrayList<>(0);
        }

        //request preceding element
        params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("history", 3);
        params.put("itemids", itemID);
        params.put("limit", 1);
        params.put("time_till", timeFromSeconds);
        params.put("sortfield", "clock");
        params.put("sortorder", "DESC");

        List<ZabbixHistoryObject> result = this.getHistory(params);
        ZabbixHistoryObject precedingElement = null;
        //check for existence of a preceding element
        if (result.size() > 0) {
            precedingElement = result.get(0);
        }
        result = null;

        //request for item history in between timeFromSeconds and timeTillSeconds
        params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("history", 3);
        params.put("itemids", itemID);
        params.put("time_from", timeFromSeconds);
        params.put("time_till", timeTillSeconds);

        result = this.getHistory(params);
        //check for adding preceding element if it was succesfully retrieved
        if (precedingElement != null){
            //check for clock times to prevent double entrys
            if (result.size() > 0){
                //resultlist not empty, clock comparison required
                if (precedingElement.getClock()<result.get(0).getClock()){
                    result.add(0, precedingElement);
                }
            } else {
                //result list is empty and only preceding element exists
                result.add(0, precedingElement);
            }
        }   
        //check for adding usernames
        if (result.size() > 0 && itemKey.startsWith("user.")) {
            //data is userspecific and username is added to results
            String name = itemKey.split("\\.")[1];
            for (ZabbixHistoryObject item : result) {
                item.setUserName(name);
            }
            return result;
        } else {
            //return list, empty or not userspecific
            return result;
        }
    }
    
    /**
     * Implements hostgroup.get from Zabbix API.
     */
    public int getHostGroupId(String hostGroupName) throws AuthenticationException, IllegalArgumentException,
            InterruptedException, ExecutionException, IOException, InternalErrorException, HostGroupNotFoundException {

        this.authenticate();

        ObjectNode params = this.objectMapper.createObjectNode();
        params.withArray("output").add("groupid");
        params.with("filter").withArray("name").add(hostGroupName);

        Response response = this.executeRPC("hostgroup.get", params);
        
        if (response.getStatusCode() == 200) {
            JsonNode jsonResponse = objectMapper.readTree(response.getResponseBody());
            if (jsonResponse.get("result").isArray() && jsonResponse.get("result").size() > 0) {
                return jsonResponse.findValue("groupid").asInt();
            } else {
                throw new HostGroupNotFoundException();
            }
        } else {
            throw new InternalErrorException();
        }
    }

    public int getDataNodeHostGroupId() throws AuthenticationException, IllegalArgumentException, InterruptedException,
            ExecutionException, IOException, InternalErrorException, HostGroupNotFoundException {

        return getHostGroupId(ZabbixParams.DATANODE_HOST_GROUP_NAME);
    }

    /**
     * Method for getting all hosts for a given HostGroup.
     * @param hostGroupName Name of the HostGroup to look up for Hosts.
     * @return list of hosts belonging to given HostGroup
     */
    public Set<String> getHostNames(String hostGroupName) throws IllegalArgumentException, InterruptedException,
             ExecutionException, AuthenticationException, InternalErrorException, IOException,
             HostGroupNotFoundException {

        return getHostIds(hostGroupName).keySet();
    }

    public Set<String> getDataNodeHostNames() throws AuthenticationException, IllegalArgumentException,
            InterruptedException, ExecutionException, InternalErrorException, IOException, HostGroupNotFoundException {

        return getHostNames(ZabbixParams.DATANODE_HOST_GROUP_NAME);
    }

    /**
     * Implements host.get from Zabbix API.
     * @return hostname <-> hostid map
     */
    public Map<String, Integer> getHostIds(String hostGroupName) throws AuthenticationException,
            IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException,
            HostGroupNotFoundException {

        ObjectNode params = this.objectMapper.createObjectNode();
        params = this.objectMapper.createObjectNode();
        params.put("groupids", getDataNodeHostGroupId());
        params.withArray("output").add("host");

        Response response = this.executeRPC("host.get", params);

        if (response.getStatusCode() == 200) {
            JsonNode jsonResponse = objectMapper.readTree(response.getResponseBody());
            HashMap<String, Integer> result = new HashMap<>(jsonResponse.get("result").size());

            if (jsonResponse.get("result").isArray() && jsonResponse.get("result").size() > 0) {
                for (JsonNode item : jsonResponse.get("result"))
                    result.put(item.get("host").asText(), item.get("hostid").asInt());
            }

            return result;
        } else{
            throw new InternalErrorException();
        }
    }

    public Map<String, Integer> getDataNodeHostIds() throws AuthenticationException,
            IllegalArgumentException, InterruptedException, ExecutionException, IOException, InternalErrorException,
            HostGroupNotFoundException {

        return getHostIds(ZabbixParams.DATANODE_HOST_GROUP_NAME);
    }

    /**
    * Method for adding/removing additional to/from calculated items formula(params).
     * @param itemKey identification String for the item to be updated
     * @param addedKey the key that is to be added/removed to/from the formula
    * @param addParams true: addedKey is to be added to the formula, false: addedKey is to be removed from the formula
     */
    private void updateCalculatedItem(String itemKey, String addedKey, boolean addParams) throws IllegalArgumentException, InterruptedException, ExecutionException, AuthenticationException, TemplateNotFoundException, InternalErrorException, IOException {
        this.authenticate();

        //read out previous value
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("hostids", getDataNodeTemplateId());
        params.with("search").put("key_", itemKey);
        List<ZabbixItem> result = this.getItems(params);
        ZabbixItem calcItem = null;
        if (result.size() > 0) {
            calcItem = result.get(0);
        }
        String newFormula = "";
        if (addParams) {
            //add new itemkey to formula
            newFormula = calcItem.getParams() + "+last(\"" + addedKey + "\")";
            if (newFormula.startsWith("+")) {
                newFormula = newFormula.substring(1, newFormula.length());
            }
        } else {
            //delete itemkey from fromula
            String formulaItems[] = calcItem.getParams().split("\\+");
            for (String formulaItem : formulaItems) {
                if (!formulaItem.equals("last(\"" + addedKey + "\")")) {
                    newFormula += formulaItem + "+";
                }
            }
            if (newFormula.endsWith("+")) {
                newFormula = newFormula.substring(0, newFormula.length() - 1);
            }
        }
        //update new formula
        params = this.objectMapper.createObjectNode();
        params.put("itemid", calcItem.getItemId());
        params.put("params", newFormula);
        if (this.executeRPC("item.update", params).getStatusCode() != 200) {
            throw new InternalErrorException();
        }
    }
    
    /**
     * 
     * @return list of all usernames
     */
    public List<String> getAllUsers() throws AuthenticationException, IllegalArgumentException, InterruptedException, ExecutionException, InternalErrorException, IOException, TemplateNotFoundException{
        this.authenticate();
        //fetching Datanode User templateID
        if (dataNodeUserTemplateID < 0){
            this.setDataNodeUserTemplateId(this.getDataNodeTemplateId());
        }
        
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("output", "extend");
        params.put("templateids", dataNodeUserTemplateID);
//        params.with("search").put("key_", "user.*.bandwidth");
        params.with("search").put("key_", String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, "*"));
//        params.with("search").withArray("key_").add(String.format(ZabbixParams.USER_LAST_INTERNAL_ADDRESS_MAPPING_KEY, "*"));
        params.put("searchWildcardsEnabled", true);
        List<ZabbixItem> response = this.getItems(params);
        
        if (response.size() > 0) {
            //adding names of the users to result list
            List<String> result = new ArrayList<>(response.size());
            for (ZabbixItem item : response) {
                result.add(item.getKey().split("\\.")[1]);
            }
            return result;
        } else {
            //no users found matching the templateid and itemKey search parameter
            return new ArrayList<>(0);
        }
    }

    /**
     * Method for creating user graph with two legend entries
     * @param graphName Visible name of the graph
     * @param userItemId id of the user item that is to be added to the future graph
     * @param allUsersItemId id of the calculated item for all users
     */
    private void createGraph(String graphName, int userItemId, int allUsersItemId) throws InternalErrorException, InterruptedException, IllegalArgumentException, ExecutionException, IOException {
        ObjectNode params = this.objectMapper.createObjectNode();
        params.put("name", graphName);
        params.put("width", 900);
        params.put("height", 200);
        params.put("ymax_type", 2); // max y in graph limited allUserItemId
        params.put("ymax_itemid", allUsersItemId);
        params.put("ymin_type", 1); // min y is limited by a fixes value (default:0)
        ObjectNode graphItem = this.objectMapper.createObjectNode();
        graphItem.put("itemid", userItemId);
        graphItem.put("color", "00AA00");
        params.withArray("gitems").add(graphItem);
        graphItem = this.objectMapper.createObjectNode();
        graphItem.put("itemid", allUsersItemId);
        graphItem.put("color", "3333FF");
        params.withArray("gitems").add(graphItem);
        if (this.executeRPC("graph.create", params).getStatusCode() != 200) {
            throw new InternalErrorException();
        }
    }

    /**
     * Method for deleting single graph for given id.
     * @param itemIds ids of the graph that are to be removed
     */
    private void graphDelete(int itemIds[]) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException, AuthenticationException, InternalErrorException {
        this.authenticate();

        if (itemIds.length == 0) {
            throw new IllegalArgumentException("No item id's provided.");
        }

        ArrayNode params = this.objectMapper.createArrayNode();
        for (int id : itemIds) {
            params.add(id + "");
        }
        Response response = this.executeRPC("graph.delete", params);

        System.out.println("Request: " + params);
        System.out.println("Got result: " + response.getResponseBody());

        if (response.getStatusCode() != 200) {
            throw new InternalErrorException("Got HTTP status code: " + response.getStatusCode());
        }
    }

    /**
     * Kills all connections and stops HTTP client threads.
     */
    public void quit() {
        this.httpClient.close();
    }
}
