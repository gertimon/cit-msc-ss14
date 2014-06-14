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

    private final String ZABBIX_API_URI;
    private final String ZABBIX_Port;
    private final String ZABBIX_USER;
    private final String ZABBIX_PASSWORD;
    private String AUTH_HASH_VALUE;
    private final static Log LOG = LogFactory.getLog(ZabbixHelper.class);

    private static ZabbixHelper zabbixHelper;

    /**
     * private constructor, use getZabbixHelper() instead (singleton) TODO
     * extract attributes to property-file
     */
    private ZabbixHelper() {
        ZABBIX_API_URI = "http://10.42.0.2/zabbix/api_jsonrpc.php";
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
     *
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
        byte[] respArr = new byte[(int) response.getEntity().getContentLength()];
        response.getEntity().getContent().read(respArr);
        String out = new String(respArr);
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
    public void setIpForUser(String ip, String username) {
        try {
            ProjectTrapper.sendMetricJson("localhost", "BLAH", username + ".ip", false, ip);
        } catch (IOException ex) {
            LOG.error("Could not send user and ip to zabbix: " + username + ", ip: " + ip, ex);
        }
    }

    /**
     * retrieves the last username known for a given ip address
     *
     * @param ip
     * @return
     */
    public String getUser(String ip) {
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
