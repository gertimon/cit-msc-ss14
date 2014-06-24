package de.tuberlin.cit.project.energy.hadoop;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.security.token.Token;

import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;

/**
 * Simple wrapper around hadoop data types. Sends transfer informations from datanode to zabbix.
 * 
 * @author Sascha
 */
public class DataNodeTransferLogger {
	private static Log log = LogFactory.getLog(DataNodeTransferLogger.class);
	private final ZabbixSender zabbixSender;
	
	public DataNodeTransferLogger(String zabbixHostname, int zabbixPort) {
		this.zabbixSender = new ZabbixSender(zabbixHostname, zabbixPort);
		log.info("New DataNodeTransfer logger initialized with zabbixHostname " + zabbixHostname + " and port " + zabbixPort + ".");
	}
	
	/**
	 * Send client informations to zabbix.
	 * 
	 * @param localAddress in form of /ip-address:port
	 * @param remoteAddress in form of /ip-address:port
	 * @param blockToken containing username informations
	 */
	public void logTransferStart(String localAddress, String remoteAddress, final Token<BlockTokenIdentifier> blockToken) throws IOException {
		String dataNodeIP = localAddress.replaceFirst("/", "").split(":")[0];
		String clientAddress = remoteAddress.replaceFirst("/", "");
		String username = blockToken.decodeIdentifier().getUserId();
		log.info("Logging transfer between " + dataNodeIP + " and " + clientAddress + " as " + username + ".");
		this.zabbixSender.sendUserDataNodeConnection(dataNodeIP, username, clientAddress);
	}
}
