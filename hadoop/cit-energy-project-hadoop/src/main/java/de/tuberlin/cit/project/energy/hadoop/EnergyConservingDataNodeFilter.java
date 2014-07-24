package de.tuberlin.cit.project.energy.hadoop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.web.JsonUtil;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;

/**
 * This class acts as interface between the energy based data node selection and
 * the name node.
 *
 * @author Tobias und Sascha
 */
@SuppressWarnings("unused")
public class EnergyConservingDataNodeFilter {
    private final static Log LOG = LogFactory.getLog(EnergyConservingDataNodeFilter.class);

    public static final String RACK_MAPPING_FILENAME = "de/tuberlin/cit/project/energy/hadoop/energy.rack.config.properties";
    public static final String USER_MAPPING_FILENAME = "de/tuberlin/cit/project/energy/hadoop/energy.user.config.properties";
    private final Map<String, EnergyMode> rackEnergyMapping;
    private final Map<String, EnergyMode> userEnergyMapping;

    private final String zabbixHostname;
    private final int zabbixPort;
    private final String zabbixRestUrl;
    private final String zabbixRestUsername;
    private final String zabbixRestPassword;

    private final ZabbixSender zabbixSender;
    private final ZabbixAPIClient zabbixApiClient;
    private final WebFrontEnd webFrontEnd;

    public enum EnergyMode { DEFAULT, FAST, CHEAP }

    public EnergyConservingDataNodeFilter(String zabbixHostname, int zabbixPort, 
    		String zabbixRestUrl, String zabbixRestUsername, String zabbixRestPassword) {
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
        
        this.rackEnergyMapping = loadEnergyMapping(RACK_MAPPING_FILENAME);
        this.userEnergyMapping = loadEnergyMapping(USER_MAPPING_FILENAME);

        this.webFrontEnd = new WebFrontEnd(this);

        LOG.info("New filter initialized.");
    }

    public LocatedBlocks filterBlockLocations(LocatedBlocks locatedBlocks, String path, String username, String remoteAddress) {
        try {
            EnergyMode energyMode = this.userEnergyMapping.get(username);
            if (energyMode == null)
            	energyMode = EnergyMode.DEFAULT;

            LOG.info("New filter request: path="+path+", username="+username+", remoteAddress="+remoteAddress+", energyMode=" + energyMode + ", locatedBlocks=" + JsonUtil.toJsonString(locatedBlocks));
            LocatedBlocks filteredBlockLocations = filterBlocks(username, path, energyMode, locatedBlocks);
            LOG.info("Returning filtered block locations: " + JsonUtil.toJsonString(filteredBlockLocations));
        	return filteredBlockLocations;
            
        } catch (IOException e) {
            LOG.fatal("Failure while filtering block locations, falling back to original locations: " + e.getMessage());
            e.printStackTrace();
            return locatedBlocks;
        }
    }

    /**
     * Remove all block locations except locations matching given strategy.
     *
     * @param blockFilterStrategy keep locations with this strategy
     */
    private LocatedBlocks filterBlocks(String username, String path, EnergyMode blockFilterStrategy, LocatedBlocks locatedBlocks) {
        // TODO: test this...
        Set<String> allowedRacks = getRacks(blockFilterStrategy);
        return reduceRacks(allowedRacks, locatedBlocks);
    }

    /**
     * removes some blocks that mismatchs a filter strategy iff required packets
     * exist on desired data nodes.
     *
     * @param blockFilterStrategy recognises CHEAP, FAST, null
     * @param locatedBlocks
     * @return
     */
    private LocatedBlocks reduceRacks(Set<String> racks, LocatedBlocks locatedBlocks) {

        try {
            List<LocatedBlock> filteredLocatedBlocks = new ArrayList<LocatedBlock>();

            for (LocatedBlock block : locatedBlocks.getLocatedBlocks()) {
                // extract locations that match the energy mode
                List<DatanodeInfo> cleanLocations = new ArrayList<DatanodeInfo>();
                for (DatanodeInfo location : block.getLocations()) {
                    if (racks.contains(location.getNetworkLocation())) {
                        cleanLocations.add(location);
                    }
                }
                if (cleanLocations.size() > 0) {
                    // if locations matching energy mode were found

                    LocatedBlock filteredBlock = createLocatedBlock(block, (DatanodeInfo[]) cleanLocations.toArray());
                    filteredLocatedBlocks.add(filteredBlock);
                    LOG.info("block location list manipulated");
                } else {
                    // nothing changed or matched energy mode

                    filteredLocatedBlocks.add(block);
                    LOG.info("block location list not manipulated");
                }
            }

            // empty and override block list
            LOG.info("Original block: " + JsonUtil.toJsonString(locatedBlocks));
            locatedBlocks.getLocatedBlocks().clear();
            locatedBlocks.getLocatedBlocks().addAll(filteredLocatedBlocks);
            LOG.info("Modified block: " + JsonUtil.toJsonString(locatedBlocks));

            return locatedBlocks;

        } catch (IOException ex) {
            LOG.error("Exception occured while filtering block locations, return original block list.", ex);
            return locatedBlocks;
        }
    }

    /**
     * Copies a block while overriding the locations by new values.
     *
     */
    public LocatedBlock createLocatedBlock(LocatedBlock block, DatanodeInfo[] locations) {
        LocatedBlock filteredBlock = new LocatedBlock(block.getBlock(), locations, block.getStorageIDs(), block.getStorageTypes(), block.getStartOffset(), block.isCorrupt(), block.getCachedLocations());
        return filteredBlock;
    }

    /**
     * filters a rack list for an EnergyMode.
     *
     * @param energyMode
     * @return list of with rack names, matching given energy mode
     */
    public Set<String> getRacks(EnergyMode energyMode) {
        Set<String> racks = new HashSet<String>();
        for (String rackName : this.rackEnergyMapping.keySet()) {
            if (this.rackEnergyMapping.get(rackName) == energyMode)
                racks.add(rackName);
        }
        return racks;
    }

    public Map<String, EnergyMode> getUserEnergyMapping() {
        return userEnergyMapping;
    }

    private Map<String, EnergyMode> loadEnergyMapping(String resourceName) {
        HashMap<String, EnergyMode> mapping = new HashMap<String, EnergyMode>();
        
    	try {
	    	Properties properties = loadProperties(resourceName);
	        for (String key : properties.stringPropertyNames()) {
	        	EnergyMode value = EnergyMode.valueOf(properties.getProperty(key));
	        	mapping.put(key, value);
	        }
    	} catch(IOException e) {
    		LOG.warn("Can't load " + resourceName + " ( " + e.getMessage() + ")! Using default values.");
    		e.printStackTrace();
    	}
    	
        return mapping;
    }

    /**
     * Load given filename via class loader.
     */
    public Properties loadProperties(String filename) throws IOException, FileNotFoundException {
        Properties prop = new Properties();
        InputStream input = EnergyConservingDataNodeFilter.class.getClassLoader().getResourceAsStream(filename);
        if (input == null) {
        	throw new FileNotFoundException(filename);
        }
        
        prop.load(input);
        input.close();

        return prop;
    }
}
