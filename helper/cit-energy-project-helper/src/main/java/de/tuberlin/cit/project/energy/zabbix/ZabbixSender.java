package de.tuberlin.cit.project.energy.zabbix;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Simple asynchron zabbix sender.
 * 
 * @author CIT VS Energy Project Team
 * 
 * Configure zabbix endpoint via system properties:
 * java ... -Dzabbix.hostname=localhost -Dzabbix.port=10051
 */
public class ZabbixSender implements Runnable {
	
	public static final String DEFAULT_ZABBIX_HOST = "mpjss14.cit.tu-berlin.de";
	public static final String DEFAULT_ZABBIX_PORT = "10051";
	
	public static final String POWER_CONSUMPTION_KEY = "datanode.power";
	public static final String USER_CLIENT_MAPPING_DUMMY_HOST = "UserClientMappingDummy";
	public static final String USER_IP_MAPPING_KEY = "user.%s.ip";
	public static final String USER_PORT_MAPPING_KEY = "user.%s.port";
	public static final String USER_BANDWIDTH_KEY = "user.%s.bandwidth";
	public static final String USER_DURATION_KEY = "user.%s.duration";

	private final String zabbixHostname;
	private final int zabbixPort;
	private final BlockingQueue<HostKeyValueTriple> valuesQueue;
	private final Thread senderThread;
	
	private class HostKeyValueTriple {
		// TODO: add timestamp
		public final String host;
		public final String key;
		public final String value;
		
		public HostKeyValueTriple(String host, String key, String value) {
			this.host = host;
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return "(" + host + ", " + key + ", " + value + ")";
		}
	}
	
	public ZabbixSender(String zabbixHostname, int zabbixPort) {
		this.zabbixHostname = zabbixHostname;
		this.zabbixPort = zabbixPort;
		this.valuesQueue = new ArrayBlockingQueue<HostKeyValueTriple>(10);
		this.senderThread = new Thread(this, "ZabbixSender");
		this.senderThread.start();
	}

	public ZabbixSender() {
		this(System.getProperty("zabbix.hostname", DEFAULT_ZABBIX_HOST),
				Integer.parseInt(System.getProperty("zabbix.port", DEFAULT_ZABBIX_PORT)));
	}
	
	public void run() {
		System.out.println("New zabbix sender with hostname " + this.zabbixHostname + " started.");
		while(!Thread.interrupted()) {
			try {
				HostKeyValueTriple data = valuesQueue.take();
//				System.out.println("Sending: " + data);
				sendDataToZabbix(data);
			} catch (UnknownHostException e) {
				break;
			} catch (IOException e) {
				// do nothing, just drop the current (failed) value
				System.err.println("Failed to send values to zabbix!");
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

    private String toJson(HostKeyValueTriple data) {
        return "{"
            + " \"request\":\"sender data\","
            + " \"data\":["
            + "   {"
            + "     \"host\":\"" + data.host + "\","
            + "     \"key\":\"" + data.key + "\","
            + "     \"value\":\"" + data.value + "\""
            + "   }"
            + " ] }";
    }

    private void sendDataToZabbix(HostKeyValueTriple data) throws UnknownHostException, IOException {
    	String json = toJson(data);
        byte[] header = calculateHeader(json.length());
        
        Socket clientSocket = new Socket(this.zabbixHostname, this.zabbixPort);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        outToServer.write(header);
        outToServer.write(json.getBytes());
        outToServer.flush();
        outToServer.close();
        clientSocket.close();
    }
    
    public void sendPowerConsumption(String hostname, double powerConsumed) {
    	valuesQueue.add(new HostKeyValueTriple(hostname, POWER_CONSUMPTION_KEY, Double.toString(powerConsumed)));
    }
    
    public void sendBandwidthUsage(String hostname, String username, double bandwidthConsumed) {
    	valuesQueue.add(new HostKeyValueTriple(hostname, String.format(USER_BANDWIDTH_KEY, username), Double.toString(bandwidthConsumed)));
    }

    public void sendDuration(String hostname, String username, double duration) {
    	valuesQueue.add(new HostKeyValueTriple(hostname, String.format(USER_DURATION_KEY, username), Double.toString(duration)));
    }

    public void sendBandwidthUsageByIp(String hostname, String ip, double bandwidthConsumed) {
    	throw new RuntimeException("Not implemented");
    }
    
    public void sendLastUserIP(String username, String lastIP) {
    	valuesQueue.add(new HostKeyValueTriple(USER_CLIENT_MAPPING_DUMMY_HOST, String.format(USER_IP_MAPPING_KEY, username), lastIP));
    }

    public void sendLastUserIPAndPort(String username, String lastIP, int lastPort) {
    	sendLastUserIP(username, lastIP);
    	valuesQueue.add(new HostKeyValueTriple(USER_CLIENT_MAPPING_DUMMY_HOST, String.format(USER_PORT_MAPPING_KEY, username), Integer.toBinaryString(lastPort)));
    }
}
