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
    ZabbixSender sender;

    public RemoveMessageListener(){
        Thread sender = new Thread(new ZabbixSender());
        sender.start();
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
        timeStamp = timeStamp - (flow.getIdleTimeout()*1000);
        String srcPort = Integer.toString(flow.getMatch().getInputPort());
        String nw_src = IPv4.fromIPv4Address(flow.getMatch().getNetworkSource());
        String nw_dst = IPv4.fromIPv4Address(flow.getMatch().getNetworkDestination());
        String dl_src = HexString.toHexString(flow.getMatch().getDataLayerSource());
        String dl_dst = HexString.toHexString(flow.getMatch().getDataLayerDestination());

        int switchId = (int)sw.getId();
        long count = flow.getByteCount();
        int time = flow.getDurationSeconds() - flow.getIdleTimeout();
        long startTime = timeStamp - (time*1000);
        //long mb = (count);

        FlowInformation flowInf = new FlowInformation(switchId,startTime,timeStamp, dl_src, srcPort, dl_dst, nw_src, nw_dst, count, time);
        String dataNode = getDataNodeByIP(flowInf.getDstIp());
        if (dataNode != null){
            try {
                String user = client.getUsernameByDataNodeConnection(dataNode,flowInf.getSrcIp());
                //Start pushing stuff

                sender.sendBandwidthUsage(dataNode,user,flowInf.getBandWith());
                sender.sendDuration(dataNode,user,flowInf.getTime());
                sender.run();

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
        }
        System.err.println(flowInf);
        // klaus = getUserNameByIP


//        String keys[] = new String[]{"project.user.ipport.klaus","project.user.bandwidth.klaus"};
//        String vals[] = new String[]{flowInf.getSrcIp(),Integer.toString((int) flowInf.getDataSize())};

        return Command.CONTINUE;
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
