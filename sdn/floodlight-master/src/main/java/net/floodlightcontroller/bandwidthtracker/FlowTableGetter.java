package net.floodlightcontroller.bandwidthtracker;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.staticflowentry.StaticFlowEntryPusher;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyInstance;
import net.floodlightcontroller.topology.TopologyManager;
import net.floodlightcontroller.zabbix_pusher.ProjectTrapper;
import org.openflow.protocol.*;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import sun.security.krb5.internal.HostAddress;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by fubezz on 18.05.14.
 */
public class FlowTableGetter implements Runnable {
    IFloodlightProviderService provider;
    Logger log;
    HashMap<String, FlowInformation> dataCouter;
   // StaticFlowEntryPusher pusher;
    int help = 0;
    public FlowTableGetter(IFloodlightProviderService floodlightProvider, Logger log){
        provider = floodlightProvider;
   //     provider.addOFMessageListener(OFType.PACKET_IN,new PacketListener());
        dataCouter = new HashMap<String, FlowInformation>();
      // pusher = new StaticFlowEntryPusher();

    }

    @Override
    public void run() {

        while(true){
            getFlows(provider);

            for (FlowInformation inf : dataCouter.values()){
                System.out.println(inf);
                
                try {
					sendToZabbix();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            System.out.println("-----------------------------------------------------");

            try {

                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    private void sendToZabbix() throws IOException {
        ProjectTrapper trapper = new ProjectTrapper();
        for (FlowInformation flow : dataCouter.values()){

        	/* Sending data via Zabbix Sender */
            //Send IP
            //trapper.sendMetric("localhost","CitProjectDummy","project.user.ipport.klaus",false,flow.getSrcIp());
            //Send Datasize
            //trapper.sendMetric("localhost","CitProjectDummy","project.user.bandwidth.klaus",true,flow.getDataSize());
        	
        	/* Sending data via JSON */
        	trapper.sendMetricJson("192.168.178.28", "CitProjectDummy", "project.user.ipport.klaus", false, flow.getSrcIp());
        	
        	
        }


    }

    public void getFlows(IFloodlightProviderService floodlightProvider)
    {
        List<IOFSwitch> switches = getSwitches(floodlightProvider);

        for( IOFSwitch sw : switches )
        {
            List<OFFlowStatisticsReply> flowTable = getSwitchFlowTable(sw, (short)-1);
            for(OFFlowStatisticsReply flow : flowTable)
            {

              //  System.err.println(String.valueOf(flow));

                String nw_src =IPv4.fromIPv4Address(flow.getMatch().getNetworkSource());
                String nw_dst = IPv4.fromIPv4Address(flow.getMatch().getNetworkDestination());
                long switchId = sw.getId();

                long count = flow.getByteCount();
                int time = flow.getDurationSeconds();
                long mb = (count);
                String key = nw_src;
                boolean changed = false;
                FlowInformation inf = dataCouter.get(key);
                if (dataCouter.containsKey(key)){
                    double size = inf.getDataSize();
                    inf.setDataSize((long)size + mb);
                    inf.setTime(inf.getTime()+time);
                  //  changed = true;
                  //  System.out.println(inf);
                }else{
                    FlowInformation counter = new FlowInformation(flow.getTableId(),nw_src,nw_dst,mb,time);
                    dataCouter.put(key,counter);
                   // changed = true;
                   // System.err.println(counter);
                }
                deleteFlowEntry(sw, flow);
            }

            }

    }

    private void deleteFlowEntry(IOFSwitch sw, OFFlowStatisticsReply rp){
        OFMatch match = new OFMatch();

        match.setDataLayerSource(rp.getMatch().getDataLayerSource());
        match.setDataLayerDestination(rp.getMatch().getDataLayerDestination());
        match.setInputPort(rp.getMatch().getInputPort());
        match.setWildcards(Wildcards.FULL.matchOn(Wildcards.Flag.DL_SRC).matchOn(Wildcards.Flag.DL_DST).matchOn(Wildcards.Flag.IN_PORT));

        OFFlowMod flowMod = (OFFlowMod)provider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);

        flowMod.setMatch(match);
        flowMod.setCommand(OFFlowMod.OFPFC_DELETE);
        try {
            sw.write(flowMod,null);
            sw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<IOFSwitch> getSwitches(IFloodlightProviderService floodlightProvider)
    {
        List<IOFSwitch> switchList = new ArrayList<IOFSwitch>();

        Map<Long, IOFSwitch> switchMap = floodlightProvider.getAllSwitchMap();
        for(Map.Entry<Long, IOFSwitch> entry : switchMap.entrySet()) {
            switchList.add( entry.getValue() );
        }

        return switchList;
    }

    // Get flow table in the switch, from PortDownReconciliation class.
    public List<OFFlowStatisticsReply> getSwitchFlowTable(IOFSwitch sw, Short outPort) {
        List<OFFlowStatisticsReply> statsReply = new ArrayList<OFFlowStatisticsReply>();
        List<OFStatistics> values = null;
        Future<List<OFStatistics>> future;
        // Statistics request object for getting flows
        OFStatisticsRequest req = new OFStatisticsRequest();
        req.setStatisticType(OFStatisticsType.FLOW);
        int requestLength = req.getLengthU();

        OFFlowStatisticsRequest specificReq = new OFFlowStatisticsRequest();
        specificReq.setMatch(new OFMatch().setWildcards(0xffffffff));
        specificReq.setOutPort(outPort);
        specificReq.setTableId((byte) 0xff);

        req.setStatistics(Collections.singletonList((OFStatistics) specificReq));
        requestLength += specificReq.getLength();
        req.setLengthU(requestLength);

        try {
            future = sw.queryStatistics(req);
            values = future.get(10, TimeUnit.SECONDS);
            if (values != null) {
                for (OFStatistics stat : values) {
                    statsReply.add((OFFlowStatisticsReply) stat);
                }
            }
        } catch (Exception e) {
            log.error("Failure retrieving statistics from switch " + sw, e);
        }

        return statsReply;


    }


}
