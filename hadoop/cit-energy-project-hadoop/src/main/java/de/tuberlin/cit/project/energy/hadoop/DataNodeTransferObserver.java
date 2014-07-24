package de.tuberlin.cit.project.energy.hadoop;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.security.token.Token;

import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;

/**
 * Simple wrapper around hadoop data types. Sends transfer informations from datanode to zabbix.
 * 
 * @author Sascha
 */
public class DataNodeTransferObserver {
	private static Log log = LogFactory.getLog(DataNodeTransferObserver.class);
	private final ZabbixSender zabbixSender;
	
	public DataNodeTransferObserver(String zabbixHostname, int zabbixPort) {
		this.zabbixSender = new ZabbixSender(zabbixHostname, zabbixPort);
		log.info("New DataNodeTransfer logger initialized with zabbixHostname " + zabbixHostname + " and port " + zabbixPort + ".");
	}
	
	/**
	 * @param localAddress in form of /ip-address:port
	 * @param remoteAddress in form of /ip-address:port
	 * @param blockToken containing username informations
	 */
	public void readBlockStart(final String localAddress, final String remoteAddress, 
	        final Token<BlockTokenIdentifier> blockToken,
	        final long blockOffset, final long length) throws IOException {
		
	    String datanode = getDatanodeName(localAddress);
		String client = getClientAddress(remoteAddress);
		String username = getUsername(blockToken);
		
        log.debug("Got readBlockStart with datanode=" + datanode
                + ", client=" + client
                + ", username=" + username
                + ", blockOffset=" + blockOffset
                + ", length=" + length
                + ".");

        this.zabbixSender.sendUserDataNodeConnection(datanode, username, client);
	}
	
	/**
     * @param localAddress in form of /ip-address:port
     * @param remoteAddress in form of /ip-address:port
     * @param blockToken containing username informations
     */
    public void readBlockEnd(final String localAddress, final String remoteAddress, 
            final Token<BlockTokenIdentifier> blockToken,
            final long blockOffset, final long length,
            final long bytesRead, final long elapsed) throws IOException {
        
        String datanode = getDatanodeName(localAddress);
        String client = getClientAddress(remoteAddress);
        String username = getUsername(blockToken);
        
        log.debug("Got readBlockEnd with datanode=" + datanode
                + ", client=" + client
                + ", username=" + username
                + ", blockOffset=" + blockOffset
                + ", length=" + length
                + ", bytesRead=" + bytesRead
                + ", elapsed=" + elapsed
                + ".");
    }
	
    /**
     * @param localAddress in form of /ip-address:port
     * @param remoteAddress in form of /ip-address:port
     * @param blockToken containing username informations
     */
    public void writeBlockStart(String localAddress, String remoteAddress, boolean isDatanode,
            final Token<BlockTokenIdentifier> blockToken,
            final int piplineSize, final DatanodeInfo[] targets,
            final long minBytesRcvd, final long maxBytesRcvd) throws IOException {

        String datanode = getDatanodeName(localAddress);
        String client = getClientAddress(remoteAddress);
        String username = getUsername(blockToken);
        
        log.debug("Got writeBlockStart with datanode=" + datanode
                + ", client=" + client
                + ", isDatanode=" + isDatanode
                + ", username=" + username
                + ", piplineSize=" + piplineSize
                + ", targets length=" + targets.length
                +" , targets=" + targets
                + ", minBytesRcvd=" + minBytesRcvd
                + ", maxBytesRcvd=" + maxBytesRcvd
                + ".");

        // first node in pipeline => external client connection
        if (piplineSize == 0 || targets.length == piplineSize - 1) {
            this.zabbixSender.sendUserDataNodeConnection(datanode, username, client);

        // next node in pipeline => internal datanode connection
        } else {
            this.zabbixSender.sendInternalDataNodeConnection(datanode, username, client);
        }
    }
    
    /**
     * @param localAddress in form of /ip-address:port
     * @param remoteAddress in form of /ip-address:port
     * @param blockToken containing username informations
     * @param bytesRcvd block size (not network traffic)
     */
    public void writeBlockEnd(String localAddress, String remoteAddress, boolean isDatanode,
            final Token<BlockTokenIdentifier> blockToken, final ExtendedBlock block,
            final int piplineSize, final long elapsed) throws IOException {

        String datanode = getDatanodeName(localAddress);
        String client = getClientAddress(remoteAddress);
        String username = getUsername(blockToken);
        
        log.debug("Got writeBlockEnd with datanode=" + datanode
                + ", client=" + client
                + ", isDatanode=" + isDatanode
                + ", username=" + username
                + ", block=" + block
                + ", block.numBytes=" + block.getNumBytes()
                + ", piplineSize=" + piplineSize
                + ", elapsed=" + elapsed
                + ".");
    }

    private String getDatanodeName(String localAddress) {
        return localAddress.replaceFirst("/", "").split(":")[0];
    }
    
    private String getClientAddress(String remoteAddress) {
        return remoteAddress.replaceFirst("/", "");
    }
    
    private String getUsername(final Token<BlockTokenIdentifier> blockToken) throws IOException {
        return blockToken.decodeIdentifier().getUserId();
    }
}
