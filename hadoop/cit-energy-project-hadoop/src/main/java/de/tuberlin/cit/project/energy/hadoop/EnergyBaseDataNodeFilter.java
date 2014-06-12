package de.tuberlin.cit.project.energy.hadoop;

import de.tuberlin.cit.project.energy.helper.zabbix.ZabbixHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.web.JsonUtil;
import org.mortbay.util.ajax.JSON;

/**
 * This class acts as interface between the energy based data node selection and
 * the name node.
 *
 * @author Sascha Wolke
 */
public class EnergyBaseDataNodeFilter {

    private final static Log LOG = LogFactory.getLog(EnergyBaseDataNodeFilter.class);
    private final String dataNodeSelectorAddress;
    private final int dataNodeSelectorPort;

    private final Properties ENERGY_USER_PROPERTIES = loadProperties("energy.user.config.properties");
    private final Properties ENERGY_RACK_PROPERTIES = loadProperties("energy.user.config.properties");

    private final ZabbixHelper zabbixHelper;

    public enum EnergyMode {

        FAST, CHEAP, NONE
    }

    public EnergyBaseDataNodeFilter(String dataNodeSelectorAddress, int dataNodeSelectorPort) {
        this.dataNodeSelectorAddress = dataNodeSelectorAddress;
        this.dataNodeSelectorPort = dataNodeSelectorPort;
        this.zabbixHelper = ZabbixHelper.getZabbixHelper();

        LOG.info("New energy data node selector client initialized with address="
            + this.dataNodeSelectorAddress + " and port=" + this.dataNodeSelectorPort + ".");
    }

    public LocatedBlocks filterBlockLocations(LocatedBlocks locatedBlocks, String path, String username, String remoteAddress) {

        LocatedBlocks orderedBlocks = null;

        try {

            // send username and ip to blackbox
            LOG.info("Got user request, inform blackbox about user's ip");

//            zabbixHelper.setIpForUser(remoteAddress, username); // TODO get different ip's for user
            // TODO push filename, opened files, removed replication locations
            // datanode send ip+port+user

            EnergyMode userMode = getEnergyMode(username, ENERGY_USER_PROPERTIES);
            Set<String> allowedRacks = getRacks(userMode);

            // try ordering
            orderedBlocks = reduceRacks(allowedRacks, locatedBlocks);
           
            LOG.info("Got decision request (" + path + ")!");
            LOG.info("Request: " + toJson(locatedBlocks, path, username, remoteAddress));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.fatal(e);
        }
        // TODO: ask blackbox
        return orderedBlocks;
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
     * @return
     */
    public Set<String> getRacks(EnergyMode energyMode) {
        // create set of racks for each mode given
        // only take cheap racks
        Set<String> racks = new HashSet<String>();
        for (String propertyName : ENERGY_RACK_PROPERTIES.stringPropertyNames()) {
            if (ENERGY_RACK_PROPERTIES.getProperty(propertyName).equals(energyMode.toString())) {
                racks.add(propertyName);
            }
        }
        return racks;
    }

    private EnergyMode getEnergyMode(String username, Properties properties) {
        EnergyMode mode = EnergyMode.NONE;
        String property = properties.getProperty(username);
        if (property.equals(EnergyMode.CHEAP.toString())) {
            mode = EnergyMode.CHEAP;
        }
        return mode;
    }

    public String toJson(LocatedBlocks locatedBlocks, String path, String username, String remoteAddress) throws IOException {
        final Map<String, Object> m = new TreeMap<String, Object>();
        m.put("locatedBlocks", JsonUtil.toJsonString(locatedBlocks));
        m.put("path", path);
        m.put("username", username);
        m.put("remoteAddress", remoteAddress);
        return JSON.toString(m);
    }

    /**
     * loads file, TODO cache file for specific time te reduce disk io
     *
     * @return
     */
    private static Properties loadProperties(String filename) {

        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = EnergyBaseDataNodeFilter.class.getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                System.out.println("Sorry, unable to find " + filename);
                return null;
            }

            //load a properties file from class path, inside static method
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }
}
