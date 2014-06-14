package net.floodlightcontroller.bandwidthtracker;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.zabbix_pusher.ProjectTrapper;
import org.openflow.protocol.*;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by fubezz on 18.05.14.
 */
public class FlowTableGetter implements Runnable {
    IFloodlightProviderService provider;
    Logger log;
  //  HashMap<String, FlowInformation> dataCouter;
   // StaticFlowEntryPusher pusher;

    public FlowTableGetter(IFloodlightProviderService floodlightProvider, Logger log){
        provider = floodlightProvider;
    //    provider.addOFMessageListener(OFType.FLOW_REMOVED,new RemoveMessageListener());
     //   dataCouter = new HashMap<String, FlowInformation>();

      // pusher = new StaticFlowEntryPusher();

    }

    @Override
    public void run() {

        while(true){
            getFlows(provider);

            try {

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                String nw_src = IPv4.fromIPv4Address(flow.getMatch().getNetworkSource());
                String nw_dst = IPv4.fromIPv4Address(flow.getMatch().getNetworkDestination());
                String dl_src = HexString.toHexString(flow.getMatch().getDataLayerSource());
                String dl_dst = HexString.toHexString(flow.getMatch().getDataLayerDestination());

                int switchId = (int)sw.getId();

                long count = flow.getByteCount();
                long r = flow.getPacketCount();
                int time = flow.getDurationSeconds() - flow.getIdleTimeout();
              //  long startTime = timeStamp - (time*1000);
                //long mb = (count);
                FlowInformation flowInf = new FlowInformation(switchId,0,0, dl_src, dl_dst, nw_src, nw_dst, count, time);
              //  System.err.println(String.valueOf(flow));

                String account = "klaus";
                if (flowInf.getDataSize() >0) {
                    System.out.println("Sende: " + flowInf);
                    System.out.println(Double.toString(flowInf.getBandWith()));
                    String keys[] = new String[]{"user." + account + ".ip", "user." + account + ".daten", "user." + account + ".bandwidth"};
                    String vals[] = new String[]{flowInf.getSrcIp(), Integer.toString((int) flowInf.getDataSize()), Integer.toString((int) flowInf.getBandWith())};
                    try {

                        sendToZabbix(flowInf, keys, vals, flowInf.getStartTime(), flowInf.getEndTime());


                    } catch (IOException e) {
                        System.err.println("Unable to push to Zabbix");
                    }


                }
            }

            }

    }


    private void sendToZabbix(FlowInformation flow,String[] keys, String[] vals, long startTime, long endTime) throws IOException {
        ProjectTrapper trapper = new ProjectTrapper();

        // for (int i = 0; i < keys.length; i++){
        if (flow.getSrcIp().equals("10.0.0.1")){

            trapper.sendMetricJson("localhost", "CitProjectDummy1", keys, vals, startTime, false);

            for (int i = 0; i < keys.length; i++){
                vals[i] = "0";
            }
            trapper.sendMetricJson("localhost","CitProjectDummy1",keys,vals,System.currentTimeMillis(),false);
        	/* Sending data via JSON */
            // trapper.sendMetricJson("localhost", "CitProjectDummy1", keys, vals, endTime, true);


        }
        //}


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
