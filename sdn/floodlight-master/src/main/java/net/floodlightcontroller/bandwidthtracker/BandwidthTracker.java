package net.floodlightcontroller.bandwidthtracker;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.internal.IOFSwitchFeatures;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.SwitchMessagePair;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.pktinhistory.ConcurrentCircularBuffer;
import net.floodlightcontroller.restserver.IRestApiService;
import org.openflow.protocol.*;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigInteger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * Created by fubezz on 14.05.14.
 */



public class BandwidthTracker implements IFloodlightModule{



    protected IFloodlightProviderService provider;
    protected IRestApiService service;


    protected static Logger log;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {


        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {


        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        provider = context.getServiceImpl(IFloodlightProviderService.class);
        log = LoggerFactory.getLogger(BandwidthTracker.class);


    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {

            new Thread(new FlowTableGetter(provider,log)).start();
        provider.addOFMessageListener(OFType.PACKET_IN,new IOFMessageListener() {
            @Override
            public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
            //    System.out.println(sw);
                return null;
            }

            @Override
            public String getName() {
                return BandwidthTracker.class.getSimpleName();
            }

            @Override
            public boolean isCallbackOrderingPrereq(OFType type, String name) {
                return false;
            }

            @Override
            public boolean isCallbackOrderingPostreq(OFType type, String name) {
                return false;
            }
        });



    }
}


