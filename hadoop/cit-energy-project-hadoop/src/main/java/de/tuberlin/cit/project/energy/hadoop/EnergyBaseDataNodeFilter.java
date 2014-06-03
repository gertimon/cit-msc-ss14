package de.tuberlin.cit.project.energy.hadoop;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Future;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private final String BLACK_BOX_URI = "TODO" + "/api/v1/";

    public EnergyBaseDataNodeFilter(String dataNodeSelectorAddress, int dataNodeSelectorPort) {
        this.dataNodeSelectorAddress = dataNodeSelectorAddress;
        this.dataNodeSelectorPort = dataNodeSelectorPort;

        LOG.info("New energy data node selector client initialized with address="
            + this.dataNodeSelectorAddress + " and port=" + this.dataNodeSelectorPort + ".");
    }

    public LocatedBlocks filterBlockLocations(LocatedBlocks locatedBlocks, String path, String username, String remoteAddress, String nnConfigValue) {

        LocatedBlocks orderedBlocks = null;

        try {

            // send username and ip to blackbox
            LOG.info("Got user request, inform blackbox about user's ip");
            AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
            final String userInfoURI = BLACK_BOX_URI + "setCurrentUser?user=" + username + "&ip=" + remoteAddress;
            Future<Response> f = asyncHttpClient.preparePut(userInfoURI).execute();
            Response response = f.get();
            if (response.getStatusCode() != 250) {
                LOG.error("could not update ip for user '" + username + "' to '" + remoteAddress + "'");
            } else {
                LOG.info("successfully updated ip from user '" + username + "' to '" + remoteAddress + "'");
            }
            if (nnConfigValue != null && !nnConfigValue.isEmpty()) {
                orderedBlocks = orderBlocks(nnConfigValue, locatedBlocks);
            } else {
                orderedBlocks = locatedBlocks;
                LOG.warn("Did not optimize list of blocks for current user '" + username + "'");
            }
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
    private LocatedBlocks orderBlocks(String blockFilterStrategy, LocatedBlocks locatedBlocks) {
        LocatedBlocks filteredLocatedBlocks = null;
        Properties properties = loadProperties();
        // try to recognize filter strategy or return same list
        if (blockFilterStrategy != null && blockFilterStrategy.equals("CHEAP")) {
            // remove everything, thats not CHEAP if copy existing
        } else if (blockFilterStrategy != null && blockFilterStrategy.equals("FAST")) {

        } else {
            LOG.warn("could not recognize a filter strategy for '" + blockFilterStrategy + "', use 'CHEAP' or 'FAST' instead, return same list of blocks.");
            filteredLocatedBlocks = locatedBlocks;
        }

        return filteredLocatedBlocks;
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
    private Properties loadProperties() {

        Properties prop = new Properties();
        InputStream input = null;

        try {

            String filename = "energy.config.properties";
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
