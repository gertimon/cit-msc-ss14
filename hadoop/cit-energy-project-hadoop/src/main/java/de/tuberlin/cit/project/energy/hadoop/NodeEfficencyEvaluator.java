package de.tuberlin.cit.project.energy.hadoop;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixParams;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import de.tuberlin.cit.project.energy.zabbix.model.ZabbixItem;

import java.io.IOException;
import java.util.HashMap;
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

        rackNodes = EnergyBaseDataNodeFilter.loadProperties("energy.nodes.config.properties");

        Map<String, Float> resultMap = new HashMap<String, Float>();

        for (Object zabbixNodeName : rackNodes.keySet()) {

            Float energy = fetchNodePowerConsumption(zabbixNodeName.toString());
            Float bandwidth = fetchNodeBandwidth(zabbixNodeName.toString());
            resultMap.put(zabbixNodeName.toString(), energy / bandwidth);

        }

        return resultMap;
    }

    /**
     * fetch last value for power consumption for given node
     *
     * @param zabbixNodeName
     * @return
     */
    private static Float fetchNodePowerConsumption(String zabbixNodeName) {
        ZabbixAPIClient apiClient = new ZabbixAPIClient();
        List<ZabbixItem> items = apiClient.getItems(zabbixNodeName, "datanode.power");

        return items.getLastValue();
    }

    /**
     * fetch last value for bandwith for given node
     *
     * @param zabbixNodeName
     * @return
     */
    private static Float fetchNodeBandwidth(String zabbixNodeName) {
        ZabbixAPIClient apiClient = new ZabbixAPIClient();
        List<ZabbixItem> items = apiClient.getItems(zabbixNodeName, "user.all.bandwidth");

        return items.getLastValue();
    }
}
