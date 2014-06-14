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
     *
     * @see https://github.com/AsyncHttpClient/async-http-client (already added
     * to POM)
     *
     * @param zabbixServerIpAdress
     * @param hostname
     * @param itemKey
     * @param isKeyNumeric
     * @param value
     * @throws IOException
     */
    public static void sendMetricJson(
        String zabbixServerIpAdress,
        String hostname,
        String itemKey,
        boolean isKeyNumeric,
        String value) throws IOException {

        String keyType = isKeyNumeric ? "0" : "4";
        String sendingJson = jsonForZabbix(hostname, itemKey, value, keyType);
        byte[] header = sendingHeader(sendingJson.length());

        sendDataToZabbix(zabbixServerIpAdress, header, sendingJson);

    }

    private static byte[] sendingHeader(int msglength) {
        return new byte[]{
            'Z', 'B', 'X', 'D',
            '\1',
            (byte) (msglength & 0xFF),
            (byte) ((msglength >> 8) & 0x00FF),
            (byte) ((msglength >> 16) & 0x0000FF),
            (byte) ((msglength >> 24) & 0x000000FF),
            '\0', '\0', '\0', '\0'};
    }

    private static String jsonForZabbix(String hostname, String itemKey, String value, String keyType) {
        return "{"
            // + " \"jsonrpc\":\"2.0\",\n"
            + " \"request\":\"sender data\","
            + " \"data\":["
            + "   {"
            + "     \"host\":\"" + hostname + "\","
            + "     \"key\":\"" + itemKey + "\","
            + "     \"value\":\"" + value + "\""
            // + "     \"value_type\":\""+ keyType+ "\""
            + "   }"
            + " ] }";
    }

    private static void sendDataToZabbix(String zabbixServerIpAdress, byte[] header, String sendingJson) throws UnknownHostException, IOException {
        Socket clientSocket = new Socket(zabbixServerIpAdress, 10051);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        outToServer.write(header);
        outToServer.write(sendingJson.getBytes());
        outToServer.flush();
        outToServer.close();
        clientSocket.close();
    }
}
