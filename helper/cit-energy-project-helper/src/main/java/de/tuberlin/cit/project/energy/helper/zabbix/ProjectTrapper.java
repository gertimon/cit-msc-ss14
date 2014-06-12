/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.tuberlin.cit.project.energy.helper.zabbix;

/**
 * only used by ZabbixHelper
 *
 * @author Tobias
 */
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.UnknownHostException;

/**
 * Class for manually pushing metric data into a zabbix server using
 * zabbix_sender commandline prompts. (created for Distributed Systems Project
 * SoSe 2014 CIT TU Berlin)
 */
public class ProjectTrapper {

    /**
     * TODO use AsyncHttpClient instead if Socket directly.
     * @see https://github.com/AsyncHttpClient/async-http-client (already added to POM)
     *
     * @param zabbixServerIpAdress
     * @param hostname
     * @param itemKey
     * @param isKeyNumeric
     * @param value
     * @throws IOException
     */
    public static void sendMetricJson(String zabbixServerIpAdress, String hostname,
        String itemKey, boolean isKeyNumeric, String value) throws IOException {

        String zabbix_URL = "http://" + zabbixServerIpAdress + "/zabbix/api_jsonrpc.php";
        String keyType;
        if (isKeyNumeric) {
            keyType = "0";
        } else {
            keyType = "4";
        }
        // System.err.println(value);
        String sender = "{\n"
            + //    "        \"jsonrpc\":\"2.0\",\n" +
            "        \"request\":\"sender data\",\n"
            + "        \"data\":[\n"
            + "                {\n"
            + "                        \"host\":\"" + hostname + "\",\n"
            + "                        \"key\":\"" + itemKey + "\",\n"
            + "                        \"value\":\"" + value + "\"\n"
            + //    "                        \"value_type\":\""+ keyType+ "\"\n"+
            "                 }\n"
            + "       ]\n"
            + "}";
        int msglength = sender.length();

        byte[] header = new byte[]{
            'Z', 'B', 'X', 'D',
            '\1',
            (byte) (msglength & 0xFF),
            (byte) ((msglength >> 8) & 0x00FF),
            (byte) ((msglength >> 16) & 0x0000FF),
            (byte) ((msglength >> 24) & 0x000000FF),
            '\0', '\0', '\0', '\0'};
        try {
            Socket clientSocket = new Socket(zabbixServerIpAdress, 10051);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
//        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToServer.write(header);
            outToServer.write(sender.getBytes());
            outToServer.flush();
            clientSocket.close();
            outToServer.close();
        } catch (UnknownHostException e) {
            System.err.println("Error while connecting to server");
        } catch (IOException e) {
            System.err.println("Error while sending to Server");

        }

    }

    /**
     * This is an example to modify Zabbix from the Controller. Needed JSON and
     * Apache HTTPClient Components
     *
     * @param zabbix_url
     * @return
     * @throws IOException
     */
//    private String userAuthenticate(String zabbix_url) throws IOException {
//
//        CloseableHttpClient httpclient = HttpClients.createDefault();
//        HttpPost httpPost = new HttpPost(zabbix_url);
//        List<NameValuePair> valuePairList = new ArrayList<NameValuePair>();
//        StringBuilder sb = new StringBuilder();
//        sb.append("{\"jsonrpc\":\"2.0\"").
//                append(",\"params\":{").
//                append("\"user\":\"").append("admin").
//                append("\",\"password\":\"").append("zabbix").
//                append("\"},").
//                append("\"method\":\"user.authenticate\",").
//                append("\"id\":\"2\"}");
//
//        try {
//
//            httpPost.setEntity(new StringEntity(sb.toString()));
//            httpPost.addHeader("Content-Type","application/json");
//            CloseableHttpResponse response = httpclient.execute(httpPost);
//            byte[] respArr = new byte[(int)response.getEntity().getContentLength()];
//            response.getEntity().getContent().read(respArr);
//            String out = new String(respArr);
//          //  System.out.println(out);
//            out = parseAuth(out);
//            response.close();
//            return out;
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//
//    return null;
//
//    }
//
//    private String parseAuth(String out) {
//        String res;
//        String parts[] = out.split(":");
//        for(int i = 0; i <parts.length ; i++){
//            if (parts[i].endsWith("result\"")){
//               // System.err.println(parts[i+1]);
//                res = parts[i+1].replace("\"","");
//                //System.err.println(res);
//                return res.split(",")[0];
//            }
//        }
//        return null;
//    }
}