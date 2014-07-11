package net.floodlightcontroller.bandwidthtracker;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.exception.UserNotFoundException;
import net.floodlightcontroller.core.*;
import net.floodlightcontroller.packet.IPv4;
import org.openflow.protocol.*;
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
    private final ZabbixAPIClient zabbixApiClient;
    private final ZabbixSender zabbixSender;
    private final String searchedIpdataNode1 = "10.42.0.1";
    private final String searchedIpdataNode2 = "10.42.0.2";
    private final String dataNodePort = "50010";


    public RemoveMessageListener() throws KeyManagementException, NoSuchAlgorithmException {
        // TODO: provide zabbix credentials from config file
        this.zabbixApiClient = new ZabbixAPIClient();
        this.zabbixSender = new ZabbixSender();
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

        OFFlowRemoved flow = (OFFlowRemoved) msg;
        FlowInformation flowInf = createFlowInformation(flow);
        System.err.println(flowInf);
        try {
            if (isSearchedFlow(flowInf.getSrcIp(), flowInf.getSrcPort())) {
                String dataNode = getDataNodeByIP(flowInf.getSrcIp());
                String user = zabbixApiClient.getUsernameByDataNodeConnection(dataNode, flowInf.getDstIp() + ":" + flowInf.getDstPort());
                sendDataToZabbix(flowInf, dataNode, user);

            } else if (isSearchedFlow(flowInf.getDstIp(), flowInf.getDstPort())) {
                String dataNode = getDataNodeByIP(flowInf.getDstIp());
                String user = zabbixApiClient.getUsernameByDataNodeConnection(dataNode, flowInf.getSrcIp() + ":" + flowInf.getSrcPort());
                sendDataToZabbix(flowInf, dataNode, user);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InternalErrorException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }

        return Command.CONTINUE;
    }

    private boolean isSearchedFlow(String ip, String port) {
        if (ip.equals(searchedIpdataNode1) && port.equals(dataNodePort)) {
            return true;
        } else if (ip.equals(searchedIpdataNode2) && port.equals(dataNodePort)) {
            return true;
        } else {
            return false;
        }
    }

    private FlowInformation createFlowInformation(OFFlowRemoved flow) {
        long timeStamp = System.currentTimeMillis();
        int tcpSrcPort = 0xFFFF & flow.getMatch().getTransportSource();
        int tcpDstPort = 0xFFFF & flow.getMatch().getTransportDestination();
        timeStamp = timeStamp - (flow.getIdleTimeout() * 1000);
        String srcPort = Integer.toString(tcpSrcPort);
        String dstPort = Integer.toString(tcpDstPort);
        String nw_src = IPv4.fromIPv4Address(flow.getMatch().getNetworkSource());
        String nw_dst = IPv4.fromIPv4Address(flow.getMatch().getNetworkDestination());
        String dl_src = HexString.toHexString(flow.getMatch().getDataLayerSource());
        String dl_dst = HexString.toHexString(flow.getMatch().getDataLayerDestination());
        long count = flow.getByteCount();
        int time = flow.getDurationSeconds() - flow.getIdleTimeout();
        long startTime = timeStamp - (time * 1000);
        FlowInformation flowInf = new FlowInformation(startTime, timeStamp, dl_src, dl_dst, nw_src, nw_dst, srcPort, dstPort, count, time);
        return flowInf;
    }

    private void sendDataToZabbix(FlowInformation flowInf, String dataNode, String user) {
        this.zabbixSender.sendDataAmountUsage(dataNode, user, flowInf.getDataSize());
        this.zabbixSender.sendBandwidthUsage(dataNode, user, flowInf.getBandWith());
        this.zabbixSender.sendDuration(dataNode, user, flowInf.getTime());
    }

    //TODO: Mapping from targetIp to Hostname
    private String getDataNodeByIP(String dstIp) throws UnknownHostException {
        return InetAddress.getByName(dstIp).getHostName();
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
