
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class JsonTest {

    private static String ZABBIX_API_URL = "http://192.168.2.111/zabbix/api_jsonrpc.php";

    public static void main(String[] args) {

        String auth = zabbixAuthenticate();
//		zabbixCreateItem("json test", "json.test", "10106", false, "0002", auth);
//		zabbixGetHistory(auth);
//		{"id":"0003","method":"history.get",
//		"params":{"output":"extend","filter":"value:1.2.3.15"},
//		"jsonrpc":"2.0","auth":"aa179db870d44b59b0b7696cee69492c"}
        long milli = System.currentTimeMillis();
        System.out.println(milli);
        String jsonString = "{\"id\":" + "\"0006\""
            + ",\"method\":" + "\"history.get\""
            + ",\"params\":{"
            + "\"output\":" + "\"extend\""
            + ",\"history\":" + 4
            + ",\"time_from\":" + (milli / 1000 - 5)
            + ",\"time_till\":" + (milli / 1000)
            + //					",\"sortorder\":" + "\"DESC\"" +
            //					",\"itemids\":" + "\"23712\"" +
            //					",\"hostids\":" +"\"10107\""+
            //					",\"sortfield\":" + "\"clock\"" +
            ",\"search\":{"
            + "\"value\":" + "\"192.168.2.121;\""
            + "}"
            + "}"
            + ",\"jsonrpc\":" + "\"2.0\""
            + ",\"auth\":" + "\"" + auth + "\""
            + "}";
        System.out.println(jsonString);
        JSONRPC2Response jsonResponse = null;
        try {
            HttpResponse httpResponse = postAndGet(jsonString);
            HttpEntity entity = httpResponse.getEntity();

            String out = EntityUtils.toString(entity);
            System.out.println(out);

	        //TODO: parse text
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO
    private static void zabbixGetHistory(String auth) {
        String method = "history.get";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("output", "extend");
        params.put("history", 3);
//		params.put("hostids", "10106");
        params.put("itemids", "23714");
        params.put("sortfield", "clock");
        params.put("sortorder", "DESC");
//		params.put("time_from", 1402324475);
//		params.put("time_till", 1402324495);
//		params.put("limit", 10);
        String jsonString = constructJsonRequest(method, params, "0003", auth);
//		System.out.println(jsonString);
        try {
            HttpResponse httpResponse = postAndGet(jsonString);
            HttpEntity entity = httpResponse.getEntity();

            String out = EntityUtils.toString(entity);
            System.out.println(out);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: void weg, i-welchen verwertbaren rückgabe wert schaffen
    private static void zabbixCreateItem(String name, String key, String host, boolean isNumeric, String reqId, String auth) {
        String method = "item.create";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", name);
        params.put("key_", key);
        params.put("hostid", host);
        params.put("type", 2);
        if (isNumeric) {
            //item ist numerisch (numeric unsigned)
            params.put("value_type", 3);
        } else {
            //item ist ein String
            params.put("value_type", 1);
        }

        String id = reqId;
        String jsonString = constructJsonRequest(method, params, id, auth);
//		System.out.println(jsonString);
//        JSONRPC2Response jsonResponse = null;

        try {
            HttpResponse httpResponse = postAndGet(jsonString);
            HttpEntity entity = httpResponse.getEntity();

            String out = EntityUtils.toString(entity);
            System.out.println(out);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO:
    private static String zabbixAuthenticate() {

        String method = "user.login";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user", "admin");
        params.put("password", "zabbix");
        String id = "0001";
        String jsonString = constructJsonAuthRequest(method, params, id);

//		System.out.println(jsonString);
        JSONRPC2Response jsonResponse = null;
        String auth = "";

        try {
            HttpResponse httpResponse = postAndGet(jsonString);
            HttpEntity entity = httpResponse.getEntity();

            String out = EntityUtils.toString(entity);
//	        System.out.println(out);

            jsonResponse = JSONRPC2Response.parse(out);
            auth = (String) jsonResponse.getResult();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return auth;
    }

    //TODO
    private static String constructJsonAuthRequest(String method, Map<String, Object> param, String reqId) {

        JSONRPC2Request reqOut = new JSONRPC2Request(method, param, reqId);
        return reqOut.toString();

    }

    //TODO
    private static String constructJsonRequest(String method, Map<String, Object> param, String reqId, String auth) {

        JSONRPC2Request reqOut = new JSONRPC2Request(method, param, reqId);
        String fixedReqOut = reqOut.toString();

//		bla = bla.substring(0, bla.length() - 1)
//				+ ",\"auth\":\""+ 1234567890 + "\"}";
        return fixedReqOut.substring(0, fixedReqOut.length() - 1)
            + ",\"auth\":\"" + auth + "\"}";

    }

    /**
     * Method for invoking Remote Procedure Calls at the target URL using Json
     * Requests
     *
     * @params request String containing the full and formated JSON Request
     */
    private static HttpResponse postAndGet(String request) throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(ZABBIX_API_URL);
        httpPost.setEntity(new StringEntity(request));
        httpPost.addHeader("Content-Type", "application/json-rpc");
        return httpclient.execute(httpPost);

    }

}
