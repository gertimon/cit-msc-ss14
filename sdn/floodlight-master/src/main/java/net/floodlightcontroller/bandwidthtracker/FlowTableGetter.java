package net.floodlightcontroller.bandwidthtracker;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by fubezz on 18.05.14.
 */
public class FlowTableGetter implements Runnable {
    IFloodlightProviderService provider;
    Logger log;
    protected HashMap<Integer, IpCounter> ipSave;
    public FlowTableGetter(IFloodlightProviderService floodlightProvider, Logger log){
        provider = floodlightProvider;
        this.log = log;
        ipSave = new HashMap<Integer, IpCounter>();
    }

    @Override
    public void run() {
        while(true){
            getFlows(provider);
            try {
                Thread.sleep(10000);
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
                String src = HexString.toHexString(flow.getMatch().getDataLayerSource());
                long switchId = sw.getId();
                long count = flow.getByteCount();
                int time = flow.getDurationSeconds();
                long mbps = ((count/time)*8)/1000000;
                System.err.println("Client: " + src + " used: " + mbps + "Mbit/s of Switch: " + switchId);
            }
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

    private class IpCounter{
        private int mac;
        private int dataSize;

        public IpCounter(int ip){
            this.mac = ip;
            dataSize = 0;
        }

        public int getMac(){
            return this.mac;
        }

        public int getDataSize(){
            return dataSize;
        }

    }
}
