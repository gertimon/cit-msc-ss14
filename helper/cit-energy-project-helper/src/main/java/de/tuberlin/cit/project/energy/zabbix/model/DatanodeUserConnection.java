package de.tuberlin.cit.project.energy.zabbix.model;

import java.net.InetSocketAddress;

/**
 * Represents a data node connection, based on logged ip:prot mappings.
 * 
 * @author Sascha
 */
public class DatanodeUserConnection {
	public static final int DEFAULT_DATANODE_PORT = 50020;
	private InetSocketAddress src;
	private InetSocketAddress dst;
	private String username;
	private boolean isInternal;
	
	/**
	 * @param srcAddress as IP:Port
	 * @param dstAddress as IP:Port or only IP using Port {@link #DEFAULT_DATANODE_PORT}
	 * @param username
	 * @param isInternal true if internal data node <-> data node connection (e.g. replication, pipelining)
	 */
	public DatanodeUserConnection(String srcAddress, String dstAddress, String username, boolean isInternal) {
		String clientIpPort[] = srcAddress.split(":");
		this.src = new InetSocketAddress(clientIpPort[0], Integer.parseInt(clientIpPort[1]));
		
		String datanodeIpPort[] = dstAddress.split(":");
		if (datanodeIpPort.length > 1)
			this.dst = new InetSocketAddress(datanodeIpPort[0], Integer.parseInt(datanodeIpPort[1]));
		else
			this.dst = new InetSocketAddress(datanodeIpPort[0], DEFAULT_DATANODE_PORT);
		
		this.username = username;
		this.isInternal = isInternal;
	}
	
	public InetSocketAddress getSrc() {
		return src;
	}
	
	public InetSocketAddress getDst() {
		return dst;
	}
	
	public String getUser() {
		return username;
	}
	
	public boolean isInternal() {
		return isInternal;
	}
	
	@Override
	public String toString() {
		return "DatanodeUserConnection("
				+ "src=" + this.src
				+ ", dst=" + this.dst
				+ ", username=" + this.username
				+ ", isInternal=" + this.isInternal + ")";
	}
}
