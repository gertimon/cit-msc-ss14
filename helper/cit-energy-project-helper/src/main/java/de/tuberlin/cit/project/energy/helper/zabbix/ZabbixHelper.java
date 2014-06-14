package de.tuberlin.cit.project.energy.helper.zabbix;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Mapper for any zabbix requests. Implements a singleton, use getZabbixHelper()
 * for getting it's
 *
 * @author Tobias
 */
public class ZabbixHelper {
    //user.name.blabla
    private final String ZABBIX_API_URI;
    private final String ZABBIX_Port;
    private final String ZABBIX_USER;
    private final String ZABBIX_PASSWORD;
    private String AUTH_HASH_VALUE;
    private final static Log LOG = LogFactory.getLog(ZabbixHelper.class);
    private final String userItems[] = {".bandwidth", ".daten", ".duration", ".ip", ".port", ".storage"};
    private final boolean userItemsIsNumeric[] = {true, true, true, false, false, true};
    private static ZabbixHelper zabbixHelper;

    /**
     * private constructor, use getZabbixHelper() instead (singleton) TODO
     * extract attributes to property-file
     */
    private ZabbixHelper() {
        ZABBIX_API_URI = "http://mpjss14.cit.tu-berlin.de/zabbix/api_jsonrpc.php";
        ZABBIX_Port = "10051";
        ZABBIX_USER = "admin";
        ZABBIX_PASSWORD = "zabbix!";
        try {
            userAuthenticate();
        } catch (IOException e) {
            System.err.println("Error while authentication to Server");
        }
    }

    /**
     * @return singleton instance of ZabbixHelper
     */
    public static synchronized ZabbixHelper getZabbixHelper() {
        return zabbixHelper == null ? zabbixHelper = new ZabbixHelper() : zabbixHelper;
    }

    /**
     * Login to the Zabbix Server and sets the authHash value
     *
     * @throws IOException
     */
    public void userAuthenticate() throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(ZABBIX_API_URI);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"jsonrpc\":\"2.0\"").
                append(",\"params\":{").
                append("\"user\":\"").append(ZABBIX_USER).
                append("\",\"password\":\"").append(ZABBIX_PASSWORD).
                append("\"},").
                append("\"method\":\"user.authenticate\",").
                append("\"id\":\"2\"}");

        httpPost.setEntity(new StringEntity(sb.toString()));
        httpPost.addHeader("Content-Type", "application/json");
        CloseableHttpResponse response = httpclient.execute(httpPost);
        // System.err.println(response);
        byte[] respArr = new byte[(int) response.getEntity().getContentLength()];
        response.getEntity().getContent().read(respArr);
        String out = new String(respArr);
        System.err.println(out);
        System.err.println("----------------------------------");
        out = parseAuth(out);

        AUTH_HASH_VALUE = out;
        response.close();

    }

    private String parseAuth(String out) {
        String res;
        String parts[] = out.split(":");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].endsWith("result\"")) {
                // System.err.println(parts[i+1]);
                res = parts[i + 1].replace("\"", "");
                //System.err.println(res);
                return res.split(",")[0];
            }
        }
        return null;
    }

    /**
     * Used from NameNode to send username-IP-connection to zabbix
     *
     * @param ip
     * @param username
     */
    public void setIpForUser(String ip, String username) throws IOException {


        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(ZABBIX_API_URI);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"jsonrpc\":\"2.0\"").
                append(",\"method\":\"item.create\"").
                append(",\"params\":[");


        for (int i = 0; i < 6; i++) {
            sb.append("{").
                    append("\"name\":\"").append(username + " of " + userItems[i].replace(".", "")).
                    append("\",\"key_\":\"").append("user." + username + userItems[i]).
                    append("\",\"hostid\":\"").append("10109\"").
                    append(",\"type\":2");
            if (userItemsIsNumeric[i]) {
                sb.append(",\"value_type\":3");
            } else sb.append(",\"value_type\":1");

            if (i < 5) sb.append("},");
            else sb.append("}");
        }
        sb.append("]").
                append(",\"id\":\"2\"").
                append(",\"auth\":\"" + AUTH_HASH_VALUE + "\"}");

        httpPost.setEntity(new StringEntity(sb.toString()));
        httpPost.addHeader("Content-Type", "application/json");
        CloseableHttpResponse response = httpclient.execute(httpPost);
        System.err.println(response);
        System.err.println("----------------------------------");
        byte[] respArr = new byte[(int) response.getEntity().getContentLength()];

        response.getEntity().getContent().read(respArr);
        String out = new String(respArr);

        System.err.println(out);


        try {
            ProjectTrapper.sendMetricJson("localhost", ip, username + ".ip", false, ip);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    
    /**
     *  Checks whether items for given username exist within Zabbix Configuration
     *  on given host.
     * @param username Name of the user for which to check items
     * @return Boolean, whether items exist
     */
    private boolean itemsExistForUser(String username){
        boolean ret = false;
        
        //Building JSON request String
        String jsonString = "{"+
            "\"jsonrpc\":\"2.0\""+
            ",\"method\":\"item.exists\""+
            ",\"params\": {"+
            "\"host\":\"CitProjectDummy1\""+
            ",\"key_\":\"user."+ username +".ip\""+
            "}"+
            ",\"auth\":\""+AUTH_HASH_VALUE+"\""+
            ",\"id\": \"0005\""+
        "}";
        //TODO:Sending JSON request
  
        //TODO:Parsing Response
        
        return ret;
    }
    
    /**
     * retrieves the last username known for a given ip address
     *
     * @param ip
     * @return
     */
    public String getUser(String ip) {
        //Building JSON request String
        //TODO: timestamp management in time_from and time_till
        int milli = 0;
        String jsonString ="{\"id\":" + "\"0006\"" +
            ",\"method\":" + "\"history.get\"" +
            ",\"params\":{" + 
                "\"output\":" + "\"extend\"" +
                ",\"history\":" + 4 +
                ",\"time_from\":" + (milli/1000-5) +
                ",\"time_till\":" + (milli/1000) +					
                ",\"search\":{" +
                        "\"value\":" + "\""+ip+";\"" +
                        "}" +
                "}" +
            ",\"jsonrpc\":" + "\"2.0\"" +
            ",\"auth\":" + "\"" + AUTH_HASH_VALUE + "\"" +
            "}";
        //TODO:Sending JSON request
        
        //TODO:Parse Response
        
        return null;
    }

    public void incrementFileDownloadRequest(String filepath) {
    }

    public void setUserDirSize(String username, long bytes) {
    }

    /**
     * return latest entry for an attribute
     *
     * @param username
     * @param attribute
     * @return
     */
    public String getAttribute(String username, String attribute) {
        String value = null;
        // TODO
        return value;
    }

    /**
     * return list of attributes for a given range
     *
     * @param username
     * @param attribute
     * @param from
     * @param to
     * @return
     */
    public List<String> getAttributes(String username, String attribute, Date from, Date to) {
        return null;
    }

}
