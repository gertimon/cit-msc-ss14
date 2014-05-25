package de.tuberlin.cit.project.energy.hadoop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;


/**
 * This class acts as interface between the energy based data node selection and the name node.
 * 
 * @author Sascha Wolke
 */
public class EnergyBaseDataNodeFilter {
	private final static Log LOG = LogFactory.getLog(EnergyBaseDataNodeFilter.class);
	private final String dataNodeSelectorAddress;
	private final int dataNodeSelectorPort;
	
	public EnergyBaseDataNodeFilter(String dataNodeSelectorAddress, int dataNodeSelectorPort) {
		this.dataNodeSelectorAddress = dataNodeSelectorAddress;
		this.dataNodeSelectorPort = dataNodeSelectorPort;
		
		LOG.info("New energy data node selector client initialized with address=" 
				+ this.dataNodeSelectorAddress + " and port=" + this.dataNodeSelectorPort + ".");
	}
	
	
	public LocatedBlocks filterBlockLocations(LocatedBlocks blocks, String path, String username, String remoteAddress) {
		LOG.info("Got decision request (" + path + ")!");
		// TODO: ask blackbox
		return blocks;
	}
}
