package de.tuberlin.cit.project.energy.hadoop;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import java.io.IOException;
import java.util.Map;
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

    public enum BlockStrategy {

        FAST, CHEAP
    };

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
            if (nnConfigValue.equals("FAST")) {
                orderedBlocks = orderBlocks(BlockStrategy.FAST, locatedBlocks);
            } else if (nnConfigValue.equals("CHEAP")) {
                orderedBlocks = orderBlocks(BlockStrategy.CHEAP, locatedBlocks);
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

    private LocatedBlocks orderBlocks(BlockStrategy blockStrategy, LocatedBlocks locatedBlocks) {
        return locatedBlocks;
    }

    public String toJson(LocatedBlocks locatedBlocks, String path, String username, String remoteAddress) throws IOException {
        final Map<String, Object> m = new TreeMap<String, Object>();
        m.put("locatedBlocks", JsonUtil.toJsonString(locatedBlocks));
        m.put("path", path);
        m.put("username", username);
        m.put("remoteAddress", remoteAddress);
        return JSON.toString(m);
    }
}
