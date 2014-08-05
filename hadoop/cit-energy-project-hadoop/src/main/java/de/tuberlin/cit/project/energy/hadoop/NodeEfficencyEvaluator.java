package de.tuberlin.cit.project.energy.hadoop;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Functionalitiies to extract rack selection intelligence from zabbix.
 *
 * @author Tobias
 */
public class NodeEfficencyEvaluator {

    /**
     * Method retrieves the power consumption summarized for all nodes of one
     * rack.
     *
     * @param keySet contains requested rack-names
     * @return rack-names with last power consumption value
     */
    static Map<String, Float> getPowerBandwidthRelation() throws IOException {

        Properties rackNodes;

        rackNodes = EnergyConservingDataNodeFilter.loadProperties("energy.nodes.config.properties");

        Map<String, Float> resultMap = new HashMap<String, Float>();

        for (Object zabbixNodeName : rackNodes.keySet()) {
            
            try {
                Float energy = fetchNodePowerConsumption(zabbixNodeName.toString());
                Float bandwidth = fetchNodeBandwidth(zabbixNodeName.toString());
                resultMap.put(zabbixNodeName.toString(), energy / bandwidth);
            } catch(Exception e) {
                e.printStackTrace();
            }

        }

        return resultMap;
    }

    /**
     * fetch last value for power consumption for given node
     *
     * @param zabbixNodeName
     * @return
     */
    private static Float fetchNodePowerConsumption(String zabbixNodeName) throws Exception {
        ZabbixAPIClient apiClient = new ZabbixAPIClient();
        // TODO: refactor getItems
        List<ZabbixItem> items = apiClient.getItems(zabbixNodeName, "datanode.power");

        return new Float(items.get(items.size() - 1).getLastValue());
    }

    /**
     * fetch last value for bandwith for given node
     *
     * @param zabbixNodeName
     * @return
     */
    private static Float fetchNodeBandwidth(String zabbixNodeName) throws Exception {
        ZabbixAPIClient apiClient = new ZabbixAPIClient();
        List<ZabbixItem> items = apiClient.getItems(zabbixNodeName, "user.all.bandwidth");

        return new Float(items.get(items.size() - 1).getLastValue());
    }
}
