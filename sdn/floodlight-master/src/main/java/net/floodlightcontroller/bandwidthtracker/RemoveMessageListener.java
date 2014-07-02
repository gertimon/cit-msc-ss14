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

    public RemoveMessageListener() throws KeyManagementException, NoSuchAlgorithmException {
        // TODO: provide zabbix credentials from config file
        this.zabbixApiClient = new ZabbixAPIClient();
        this.zabbixSender = new ZabbixSender();
    }

	@Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

        long timeStamp = System.currentTimeMillis();

        OFFlowRemoved flow = (OFFlowRemoved) msg;
        int tcpSrc = 0xFFFF & flow.getMatch().getTransportSource();
        int tcpDst = 0xFFFF & flow.getMatch().getTransportDestination();

        timeStamp = timeStamp - (flow.getIdleTimeout()*1000);
        String srcPort = Integer.toString(tcpSrc);
        String dstPort = Integer.toString(tcpDst);
        String nw_src = IPv4.fromIPv4Address(flow.getMatch().getNetworkSource());
        String nw_dst = IPv4.fromIPv4Address(flow.getMatch().getNetworkDestination());
        String dl_src = HexString.toHexString(flow.getMatch().getDataLayerSource());
        String dl_dst = HexString.toHexString(flow.getMatch().getDataLayerDestination());

        int switchId = (int)sw.getId();
        long count = flow.getByteCount();
        int time = flow.getDurationSeconds() - flow.getIdleTimeout();
        long startTime = timeStamp - (time*1000);
        //long mb = (count);

        FlowInformation flowInf = new FlowInformation(switchId,startTime,timeStamp, dl_src, dl_dst, nw_src, nw_dst, srcPort, dstPort, count, time);
        System.err.println(flowInf);

        try {
            if ((nw_src + ":" + srcPort).equals("10.0.42.1:50010") || (nw_src + ":" + srcPort).equals("10.0.42.2:50010")) {
                String dataNode = getDataNodeByIP(nw_src);
                String user = zabbixApiClient.getUsernameByDataNodeConnection(dataNode, nw_dst + ":" + dstPort);
                sendDataToZabbix(flowInf,dataNode,user);

            } else if ((nw_dst + ":" + dstPort).equals("10.0.42.1:50010") || (nw_dst + ":" + dstPort).equals("10.0.42.2:50010")) {
                String dataNode = getDataNodeByIP(nw_dst);
                String user = zabbixApiClient.getUsernameByDataNodeConnection(dataNode, nw_src + ":" + srcPort);
                sendDataToZabbix(flowInf,dataNode,user);
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

    private void sendDataToZabbix(FlowInformation flowInf, String dataNode, String user) {
        this.zabbixSender.sendBandwidthUsage(dataNode,user,flowInf.getBandWith());
        this.zabbixSender.sendDuration(dataNode,user,flowInf.getTime());
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
