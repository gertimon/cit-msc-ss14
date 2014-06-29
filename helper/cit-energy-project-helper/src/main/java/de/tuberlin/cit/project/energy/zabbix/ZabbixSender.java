package de.tuberlin.cit.project.energy.zabbix;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple asynchrony zabbix sender.
 *
 * New data gets collected within a queue and send via an additional thread.
 *
 * @author CIT VS Energy Project Team
 *
 * Configure zabbix endpoint via system properties:
 * java ... -Dzabbix.hostname=localhost -Dzabbix.port=10051
 */
public class ZabbixSender implements Runnable {
    private static final Log log = LogFactory.getLog(ZabbixSender.class);

    private final String zabbixHostname;
    private final int zabbixPort;
    private final BlockingQueue<ObjectNode[]> valuesQueue;
    private final Thread senderThread;
    private final ObjectMapper objectMapper;

    public ZabbixSender(String zabbixHostname, int zabbixPort) {
        this.zabbixHostname = zabbixHostname;
        this.zabbixPort = zabbixPort;
        this.valuesQueue = new ArrayBlockingQueue<ObjectNode[]>(10);
        this.objectMapper = new ObjectMapper();
        this.senderThread = new Thread(this, "ZabbixSender");
        this.senderThread.start();

        log.info("New ZabbixSender initialized with zabbix hostname " + this.zabbixHostname + " and port " + this.zabbixPort + ".");
    }

    public ZabbixSender() {
        this(System.getProperty("zabbix.hostname", ZabbixParams.DEFAULT_ZABBIX_HOST),
                Integer.parseInt(System.getProperty("zabbix.port", ZabbixParams.DEFAULT_ZABBIX_PORT)));
    }

    /**
     * Sender thread.
     *
     * Fetches data from queue and send them to zabbix.
     */
    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                ObjectNode data[] = valuesQueue.take();
                sendDataToZabbix(data);
            } catch (UnknownHostException e) {
                System.err.println("Can't find zabbix host: " + e);
                break;
            } catch (IOException e) {
                // do nothing, just drop the current (failed) value
                System.err.println("Failed to send values to zabbix!");
                e.printStackTrace();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Stops sender thread.
     */
    public void quit() {
        this.senderThread.interrupt();
    }

    /**
     * Calculate special Zabbix agent protocol header (part before data as JSON).
     * @param msglength length of JSON part
     * @return Zabbix agent protocol header as bytes
     */
    private byte[] calculateHeader(int msglength) {
        return new byte[]{
            'Z', 'B', 'X', 'D',
            '\1',
            (byte) (msglength & 0xFF),
            (byte) ((msglength >> 8) & 0x00FF),
            (byte) ((msglength >> 16) & 0x0000FF),
            (byte) ((msglength >> 24) & 0x000000FF),
            '\0', '\0', '\0', '\0'};
    }

    /**
     * Create JSON data nodes with given values.
     * @param hostname known by Zabbix
     * @param clock timestamp in seconds since January 1st 1970
     */
    private ObjectNode createDataNode(String hostname, String key, String value, long clock) {
        ObjectNode data = this.objectMapper.createObjectNode();
        data.put("host", hostname);
        data.put("key", key);
        data.put("value", value);
        data.put("clock", clock);
        return data;
    }

    /**
     * Create JSON data nodes with given values and current time.
     * @param hostname known by Zabbix
     */
    private ObjectNode createDataNode(String hostname, String key, String value) {
        return createDataNode(hostname, key, value, System.currentTimeMillis() / 1000);
    }

    /**
     * Opens a connection and send given data objects.
     * @param data JSON objects produced by {@link #createDataNode()}
     */
    private void sendDataToZabbix(ObjectNode data[]) throws UnknownHostException, IOException {
        ObjectNode request = this.objectMapper.createObjectNode();
        request.put("request", "sender data");
        for(ObjectNode node : data)
            request.withArray("data").add(node);
        request.put("clock", System.currentTimeMillis()/1000);

        String jsonRequest = request.toString();
        byte[] header = calculateHeader(jsonRequest.length());

        Socket clientSocket = new Socket(this.zabbixHostname, this.zabbixPort);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        outToServer.write(header);
        outToServer.write(jsonRequest.getBytes());
        outToServer.flush();
        outToServer.close();
        clientSocket.close();
    }

    /**
     * @param dataNodeName as hostname
     * @param powerConsumed in watt
     */
    public void sendPowerConsumption(String dataNodeName, double powerConsumed) {
        valuesQueue.add(new ObjectNode[] {
            createDataNode(dataNodeName, ZabbixParams.POWER_CONSUMPTION_KEY, Double.toString(powerConsumed))
        });
    }

    /**
     * @param dataNodeName as hostname
     * @param powerConsumed in watt
     * @param clock timestamp in seconds since January 1st 1970
     */
    public void sendPowerConsumption(String dataNodeName, double powerConsumed, long clock) {
        valuesQueue.add(new ObjectNode[] {
            createDataNode(dataNodeName, ZabbixParams.POWER_CONSUMPTION_KEY, Double.toString(powerConsumed), clock)
        });
    }

    /**
     * @param dataNodeName as hostname
     * @param username
     * @param bandwidthConsumed in KByte/second
     */
    public void sendBandwidthUsage(String dataNodeName, String username, double bandwidthConsumed) {
        valuesQueue.add(new ObjectNode[] {
            createDataNode(dataNodeName, String.format(ZabbixParams.USER_BANDWIDTH_KEY, username), Double.toString(bandwidthConsumed))
        });
    }

    /**
     * @param dataNodeName as hostname
     * @param username
     * @param bandwidthConsumed in KByte/second
     * @param clock timestamp in seconds since January 1st 1970
     */
    public void sendBandwidthUsage(String dataNodeName, String username, double bandwidthConsumed, long clock) {
        valuesQueue.add(new ObjectNode[] {
            createDataNode(dataNodeName, String.format(ZabbixParams.USER_BANDWIDTH_KEY, username), Double.toString(bandwidthConsumed), clock)
        });
    }

    /**
     * @param dataNodeName as hostname
     * @param username
     * @param duration in seconds
     */
    public void sendDuration(String dataNodeName, String username, double duration) {
        valuesQueue.add(new ObjectNode[] {
            createDataNode(dataNodeName, String.format(ZabbixParams.USER_DURATION_KEY, username), Double.toString(duration))
        });
    }

    /**
     * @param dataNodeName as hostname
     * @param username
     * @param duration in seconds
     * @param clock timestamp in seconds since January 1st 1970
     */
    public void sendDuration(String dataNodeName, String username, double duration, long clock) {
        valuesQueue.add(new ObjectNode[] {
            createDataNode(dataNodeName, String.format(ZabbixParams.USER_DURATION_KEY, username), Double.toString(duration), clock)
        });
    }

    /**
     * @param dataNodeName as hostname
     * @param username
     * @param clientAddress as ip:port
     */
    public void sendUserDataNodeConnection(String dataNodeName, String username, String clientAddress) {
        valuesQueue.add(new ObjectNode[] {
            createDataNode(dataNodeName, String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, username), clientAddress)
        });
    }

    /**
     * @param dataNodeName as hostname
     * @param username
     * @param clientIp client ip
     * @param clientPort client port
     */
    public void sendUserDataNodeConnection(String dataNodeName, String username, String clientIp, int clientPort) {
        sendUserDataNodeConnection(dataNodeName, username, clientIp+":"+clientPort);
    }

    @Deprecated
    public void sendBandwidthUsageByConnection(String serverName, String clientIP, String clientPort, double bandwidthConsumed) {
        throw new RuntimeException("Not implemented here! Implement in floodlight controller and cache user/ip+port mapping.");
    }
}