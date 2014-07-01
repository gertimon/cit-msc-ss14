package net.floodlightcontroller.bandwidthtracker;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.exception.UserNotFoundException;
import net.floodlightcontroller.core.*;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.timesync.TimeSync;
import net.floodlightcontroller.zabbix_pusher.ProjectTrapper;
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
    ZabbixAPIClient client;

    public RemoveMessageListener(){
        try {
            client = new ZabbixAPIClient();
            client.authenticate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
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
        }
    }
    @SuppressWarnings("static-access")
	@Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

    	long timeStamp = TimeSync.getNtpTimestamp();
    	//long timeStamp = System.currentTimeMillis();

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
        String dataNode = "";
        String user = "";
        System.err.println(flowInf);
        try {
            if ((nw_src + ":" + srcPort).equals("10.0.42.1:50010") || (nw_src + ":" + srcPort).equals("10.0.42.2:50010")) {
                dataNode = getDataNodeByIP(nw_src);
                user = client.getUsernameByDataNodeConnection(dataNode, nw_dst + ":" + dstPort);

            } else if ((nw_dst + ":" + dstPort).equals("10.0.42.1:50010") || (nw_dst + ":" + dstPort).equals("10.0.42.2:50010")) {
                dataNode = getDataNodeByIP(nw_dst);
                user = client.getUsernameByDataNodeConnection(dataNode, nw_src + ":" + srcPort);
            }
            if (!dataNode.isEmpty() && !user.isEmpty()) sendDataToZabbix(flowInf,dataNode,user);

        }catch (InterruptedException e) {
            e.printStackTrace();
        }catch (ExecutionException e) {
            e.printStackTrace();
        }catch (InternalErrorException e) {
            e.printStackTrace();
        }catch (AuthenticationException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (UserNotFoundException e) {
            e.printStackTrace();
        }
        return Command.CONTINUE;
    }

    private void sendDataToZabbix(FlowInformation flowInf, String dataNode, String user) {
        ZabbixSender zabbixSender = new ZabbixSender();
        zabbixSender.sendBandwidthUsage(dataNode,user,flowInf.getBandWith());
        zabbixSender.sendDuration(dataNode,user,flowInf.getTime());
    }

    //TODO: Mapping from targetIp to Hostname
    private String getDataNodeByIP(String dstIp) {
        try {
            InetAddress addr = InetAddress.getByName(dstIp);
            String host = addr.getHostName();
            return host;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }finally {
            return null;
        }
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
