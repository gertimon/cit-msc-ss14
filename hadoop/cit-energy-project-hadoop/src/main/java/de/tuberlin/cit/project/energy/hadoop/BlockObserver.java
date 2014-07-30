package de.tuberlin.cit.project.energy.hadoop;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.apache.hadoop.hdfs.web.JsonUtil;
import org.mortbay.util.ajax.JSON;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;

/**
 * This class observes block changes from operations on a name node rpc server.
 *
 * @author Sascha
 */
@SuppressWarnings("unused")
public class BlockObserver {
    private final static Log LOG = LogFactory.getLog(BlockObserver.class);

    private final NamenodeProtocols namenode;
    private final String zabbixHostname;
    private final int zabbixPort;
    private final String zabbixRestUrl;
    private final String zabbixRestUsername;
    private final String zabbixRestPassword;

    private final ZabbixSender zabbixSender;
    private final ZabbixAPIClient zabbixApiClient;

    public BlockObserver(NamenodeProtocols namenode,
            String zabbixHostname, int zabbixPort, 
    		String zabbixRestUrl, String zabbixRestUsername, String zabbixRestPassword) {

        this.namenode = namenode;
        this.zabbixHostname = zabbixHostname;
        this.zabbixPort = zabbixPort;
        this.zabbixRestUrl = zabbixRestUrl;
        this.zabbixRestUsername = zabbixRestUsername;
        this.zabbixRestPassword = zabbixRestPassword;
        
        this.zabbixSender = new ZabbixSender(zabbixHostname, zabbixPort);
        
        try {
        	this.zabbixApiClient = new ZabbixAPIClient(zabbixRestUrl, zabbixRestUsername, zabbixRestPassword);
        } catch(NoSuchAlgorithmException e) {
        	e.printStackTrace();
        	throw new RuntimeException("Can't initialize zabbixAPIClient: " + e.getMessage());
        } catch(KeyManagementException e) {
        	e.printStackTrace();
        	throw new RuntimeException("Can't initialize zabbixAPIClient: " + e.getMessage());
        }

        LOG.info("New block observer initialized.");
    }
    
    /**
     * Called after write finishes (new file and append file!)
     * 
     * TODO: track old file size and calculate delta accordingly
     */
    public void complete(String path, long fileId, String username, LocatedBlocks locatedBlocks, boolean result) {
        LOG.info("Got complete operation with "
                + "path=" + path
                + ", username=" + username
                + ", locatedBlocks=" + locatedBlocks
                + ", result=" + result + ".");

        HashMap<String, Long> allocatedBytes = new HashMap<String, Long>();
        for (LocatedBlock block : locatedBlocks.getLocatedBlocks()) {
            for (DatanodeInfo datanode : block.getLocations()) {
                if (allocatedBytes.containsKey(datanode.getHostName()))
                    allocatedBytes.put(datanode.getHostName(),
                            allocatedBytes.get(datanode.getHostName()) + block.getBlock().getNumBytes());
                else
                    allocatedBytes.put(datanode.getHostName(), block.getBlock()
                            .getNumBytes());
            }
        }

        for (String datanode : allocatedBytes.keySet()) {
            this.zabbixSender.sendDataUsageDelta(datanode, username, allocatedBytes.get(datanode));
            this.zabbixSender.sendBlockEvent(datanode, username,
                    "{\"type\":\"complete\", \"numBytes\":" + allocatedBytes.get(datanode) + ", \"path\":\"" + path + "\"}");
        }
    }

    /**
     * Called after path successfully removed.
     * @param removedBlocksJson Located blocks before removal froozen as JSON
     */
    public void delete(String path, String username, String removedBlocksJson, boolean recursive) {
        LOG.info("Got delete operation with "
                + "path=" + path
                + ", username=" + username
                + ", removedBlocksJson=" + removedBlocksJson
                + ", recursive=" + recursive + ".");

        try {
            LocatedBlocks removedBlocks = JsonUtil.toLocatedBlocks((Map<?, ?>) JSON.parse(removedBlocksJson));

            HashMap<String, Long> removedBytes = new HashMap<String, Long>();
            for (LocatedBlock block : removedBlocks.getLocatedBlocks()) {
                for (DatanodeInfo datanode : block.getLocations()) {
                    if (removedBytes.containsKey(datanode.getHostName()))
                        removedBytes.put(datanode.getHostName(),
                                removedBytes.get(datanode.getHostName()) + block.getBlock().getNumBytes());
                    else
                        removedBytes.put(datanode.getHostName(), block.getBlock().getNumBytes());
                }
            }

            for (String datanode : removedBytes.keySet()) {
                this.zabbixSender.sendDataUsageDelta(datanode, username, -1 * removedBytes.get(datanode));
                this.zabbixSender.sendBlockEvent(datanode, username, "{\"type\":\"delete\", \"numBytes\":"
                        + removedBytes.get(datanode) + ", \"path\":\"" + path + "\"}");
            }

        } catch (IOException e) {
            LOG.error("Can't parse located blocks JSON: " + e.getMessage(), e);
        }
    }

    /**
     * TODO: Wait until replication finishes and update data usage.
     *
     * @param replication new replication count
     * @param blocksBefore frozen as JSON
     */
    public void setReplication(String path, String username, short replication, String blocksBeforeJson) {

        LOG.info("Got setReplication operation with "
                + "path=" + path
                + ", username=" + username
                + ", replication=" + replication
                + ", blocksBeforeJson=" + blocksBeforeJson
                + ".");

        try {
            LocatedBlocks blocksBefore = JsonUtil.toLocatedBlocks((Map<?, ?>) JSON.parse(blocksBeforeJson));
            for (String datanode : HadoopUtils.getDataNodeNames(blocksBefore))
                this.zabbixSender.sendBlockEvent(datanode, username,
                        "{\"type\":\"setReplication\", \"path\":\"" + path + "\", \"replication\":" + replication + "}");

        } catch (IOException e) {
			LOG.error("Can't parse located blocks JSON: " + e.getMessage(), e);
		}
    }

    /**
     * TODO: Calculate and update data usage.
     */
    public void concat(String target, String src[], String username, LocatedBlocks srcBlocks[]) {
        LOG.info("Got setReplication operation with "
                + "path=" + target
                + ", src=" + Arrays.toString(src)
                + ", username=" + username
                + ", srcBlocks=" + Arrays.toString(srcBlocks)
                + ".");

        Set<String> datanodes = new HashSet<String>();
        for (LocatedBlocks locatedBlocks : srcBlocks)
            datanodes.addAll(HadoopUtils.getDataNodeNames(locatedBlocks));

        for (String datanode : datanodes)
            this.zabbixSender.sendBlockEvent(datanode, username,
                    "{\"type\":\"concat\", \"target\":\"" + target + "\", \"src\":" + Arrays.toString(src) + "}");
    }

    /**
     * @return complete allocated space by user home dir or -1 otherwise
     */
    private long getUserDataUsage(String username) {
        try {
            String homeDir = String.format(HOME_DIR_PATTERN, username);
            return this.namenode.getContentSummary(homeDir).getSpaceConsumed();
        } catch (IOException e) {
            /* file not found, giving up... */
        }
        return -1;
    }
}
