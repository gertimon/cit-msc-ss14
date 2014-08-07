package net.floodlightcontroller.bandwidthtracker;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.exception.UserNotFoundException;
import net.floodlightcontroller.core.*;
import net.floodlightcontroller.packet.IPv4;
import org.openflow.protocol.*;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.util.HexString;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;


/**
 * Created by fubezz on 11.06.14.
 */


public class RemoveMessageListener implements IOFMessageListener {
    FlowTableGetter flowGetter;

    public RemoveMessageListener(FlowTableGetter flowTableGetter) throws KeyManagementException, NoSuchAlgorithmException {
        // TODO: provide zabbix credentials from config file
       flowGetter = flowTableGetter;

    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        OFFlowRemoved flow = (OFFlowRemoved) msg;
        FlowInformation flowInf = flowGetter.createFlowInformation(flow);
        String hashKey = flowInf.getHashKey();
        if (flowGetter.flowMap.containsKey(hashKey) && flowGetter.conInfMap.containsKey(hashKey)) {
            FlowInformation oldFlow = flowGetter.flowMap.get(hashKey);
            FlowTableGetter.ConnectionInfos conInf = flowGetter.conInfMap.get(hashKey);
            FlowInformation modFlow = flowGetter.modifyFlow(flowInf, oldFlow);
            flowGetter.flowMap.remove(hashKey);
            flowGetter.conInfMap.remove(hashKey);
            if (modFlow != null) {
                System.out.println("DELETE: " + modFlow);
                //TODO Enable to send to Zabbix!
                flowGetter.sendDataToZabbix(modFlow, conInf);
                //Stop Flow on Zabbix
                modFlow.setBandwidth(0.0);
                //modFlow.setDataSize(0.0);
                //modFlow.setTime(0.0);
                //TODO Enable to send to Zabbix!
                flowGetter.sendDataToZabbix(modFlow, conInf);

            } else {
                System.out.println("DELETE: " + flowInf);
            }

        }
        return Command.CONTINUE;
    }


    @Override
    public String getName() {
        return "RemoveMessageListener.java";
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }
}
