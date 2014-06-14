package net.floodlightcontroller.bandwidthtracker;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.timesync.TimeSync;
import net.floodlightcontroller.zabbix_pusher.ProjectTrapper;


//import org.apache.commons.codec.binary.Hex;
import org.openflow.protocol.*;
import org.openflow.util.HexString;

import java.io.IOException;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Created by fubezz on 11.06.14.
 */
public class RemoveMessageListener implements IOFMessageListener {
	
	/* Uses Time Protocol (RFC868) for simplicity. */
	/*
	public static final long getCurrentSyncedTime(String host) throws IOException {
		
		TimeTCPClient timeClient = new TimeTCPClient();
		long currentTimeStamp = 0;
		
		timeClient.setDefaultTimeout(2000); // 2 sec
		timeClient.connect(host);
		//System.out.println(timeClient.getDate(InetAddress.getByName(host)));
		currentTimeStamp = timeClient.getTime();
        timeClient.disconnect();
        return currentTimeStamp;	
	}
	*/
	
    @SuppressWarnings("static-access")
	@Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
    	
    	// --- Time Protocol Version (RFC868) --- //
    	//     http://commons.apache.org/proper/commons-net/javadocs/api-3.3/index.html
    	
    	/*long currentTimeStamp = 0;
    	
    	try {
			currentTimeStamp = getCurrentSyncedTime("time.nist.gov"); //
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	System.out.println("Timestamp Server is: " +currentTimeStamp);
    	System.out.println("Timestamp Local is: " +System.currentTimeMillis());
    	*/
    	
    	long timeStamp = TimeSync.getNtpTimestamp();
    	//long timeStamp = System.currentTimeMillis();

        OFFlowRemoved flow = (OFFlowRemoved) msg;
        timeStamp = timeStamp - (flow.getIdleTimeout()*1000);
        String nw_src = IPv4.fromIPv4Address(flow.getMatch().getNetworkSource());
        String nw_dst = IPv4.fromIPv4Address(flow.getMatch().getNetworkDestination());
        String dl_src = HexString.toHexString(flow.getMatch().getDataLayerSource());
        String dl_dst = HexString.toHexString(flow.getMatch().getDataLayerDestination());

        int switchId = (int)sw.getId();

        long count = flow.getByteCount();
        long r = flow.getPacketCount();
        int time = flow.getDurationSeconds() - flow.getIdleTimeout();
        long startTime = timeStamp - (time*1000);
        //long mb = (count);
        FlowInformation flowInf = new FlowInformation(switchId,startTime,timeStamp, dl_src, dl_dst, nw_src, nw_dst, count, time);
        System.err.println(flowInf);
        // klaus = getUserNameByIP
        String keys[] = new String[]{"project.user.ipport.klaus","project.user.bandwidth.klaus"};
        String vals[] = new String[]{flowInf.getSrcIp(),Integer.toString((int) flowInf.getDataSize())};
        try {
            sendToZabbix(flowInf,keys,vals,flowInf.getStartTime(), flowInf.getEndTime());


        } catch (IOException e) {
            System.err.println("Unable to push to Zabbix");
        }
        return Command.CONTINUE;
    }

    private void sendToZabbix(FlowInformation flow,String[] keys, String[] vals, long startTime, long endTime) throws IOException {
        ProjectTrapper trapper = new ProjectTrapper();

       // for (int i = 0; i < keys.length; i++){
            if (flow.getSrcIp().equals("10.0.0.1")){

                trapper.sendMetricJson("localhost", "CitProjectDummy", keys, vals, startTime, false);

        	/* Sending data via JSON */
                trapper.sendMetricJson("localhost", "CitProjectDummy", keys, vals, endTime, true);


            }
        //}


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
