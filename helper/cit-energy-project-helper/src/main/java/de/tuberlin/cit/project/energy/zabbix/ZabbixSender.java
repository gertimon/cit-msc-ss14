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
 * Simple asynchron zabbix sender.
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
	
	public void quit() {
		this.senderThread.interrupt();
	}

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
    
    private ObjectNode createDataNode(String host, String key, String value) {
    	ObjectNode data = this.objectMapper.createObjectNode();
    	data.put("host", host);
    	data.put("key", key);
    	data.put("value", value);
    	return data;
    }
    
    private void sendDataToZabbix(ObjectNode data[]) throws UnknownHostException, IOException {
    	ObjectNode request = this.objectMapper.createObjectNode();
    	request.put("request", "sender data");
    	for(ObjectNode node : data)
    		request.withArray("data").add(node);
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
    
    public void sendPowerConsumption(String hostname, double powerConsumed) {
    	valuesQueue.add(new ObjectNode[] {
			createDataNode(hostname, ZabbixParams.POWER_CONSUMPTION_KEY, Double.toString(powerConsumed))
		});
    }
    
    public void sendBandwidthUsage(String serverName, String username, double bandwidthConsumed) {
    	valuesQueue.add(new ObjectNode[] {
			createDataNode(serverName, String.format(ZabbixParams.USER_BANDWIDTH_KEY, username), Double.toString(bandwidthConsumed))
		});
    }

    public void sendDuration(String serverName, String username, double duration) {
    	valuesQueue.add(new ObjectNode[] {
			createDataNode(serverName, String.format(ZabbixParams.USER_DURATION_KEY, username), Double.toString(duration))
    	});
    }

    /**
     * @param dataNodeServerName as ip or name
     * @param username
     * @param address in form ip-address:port
     */
    public void sendUserDataNodeConnection(String dataNodeServerName, String username, String clientAddress) {
    	valuesQueue.add(new ObjectNode[] {
			createDataNode(dataNodeServerName, String.format(ZabbixParams.USER_LAST_ADDRESS_MAPPING_KEY, username), clientAddress)
    	});
    }
    
    /**
     * @param dataNodeServerName as IP or name
     * @param username
     * @param ip client IP
     * @param port client port
     */
    public void sendUserDataNodeConnection(String dataNodeServerName, String username, String ip, int port) {
    	sendUserDataNodeConnection(dataNodeServerName, username, ip+":"+port);
    }    

    @Deprecated
    public void sendBandwidthUsageByConnection(String serverName, String clientIP, String clientPort, double bandwidthConsumed) {
    	throw new RuntimeException("Not implemented here! Implement in floodlight controller and cache user/ip+port mapping.");
    }
}
