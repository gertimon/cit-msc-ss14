package de.tuberlin.cit.project.energy.hadoop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
public class EnergyBaseDataNodeFilter {
    private final static Log LOG = LogFactory.getLog(EnergyBaseDataNodeFilter.class);

    public static final String RACK_MAPPING_FILENAME = "de/tuberlin/cit/project/energy/hadoop/energy.rack.config.properties";
    public static final String USER_MAPPING_FILENAME = "de/tuberlin/cit/project/energy/hadoop/energy.user.config.properties";
    private final Map<String, EnergyMode> rackEnergyMapping;
    private final Map<String, EnergyMode> userEnergyMapping;

    private final String zabbixHostname;
    private final int zabbixPort;
    private final String zabbixRestUrl;
    private final String zabbixRestUsername;
    private final String zabbixRestPassword;

    private ZabbixSender zabbixSender;
    private ZabbixAPIClient zabbixApiClient;

    public enum EnergyMode { DEFAULT, FAST, CHEAP }

    public EnergyBaseDataNodeFilter(String zabbixHostname, int zabbixPort, 
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
    	// TODO: fix this...
//        if (EnergyMode.CHEAP == userMode) {
//            for (LocatedBlock block : locatedBlocks.getLocatedBlocks()) {
//                // find first matching
//                List<DatanodeInfo> cleanLocations = new ArrayList<DatanodeInfo>();
//                for (DatanodeInfo location : block.getLocations()) {
//                    if (cheapRacks.contains(location.getNetworkLocation())) {
//                        cleanLocations.add(location);
//                    }
//                }
//                if (cleanLocations.size() > 0) {
//                    DatanodeInfo[] locations = block.getLocations();
//                    // TODO replace by override, this maybe will not change anything
//                    locations = (DatanodeInfo[]) cleanLocations.toArray();
//                    LOG.info("block location list manipulated");
//                } else {
//                    LOG.info("block location list not manipulated");
//                }
//            }
//        } else {
//            LOG.warn("could not recognize a filter strategy for mode '" + userMode + "', use 'CHEAP' or -'FAST'- instead, return same list of blocks.");
//        }
        
        return locatedBlocks;
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
        InputStream input = EnergyBaseDataNodeFilter.class.getClassLoader().getResourceAsStream(filename);
        if (input == null) {
        	throw new FileNotFoundException(filename);
        }
        
        prop.load(input);
        input.close();

        return prop;
    }
}
