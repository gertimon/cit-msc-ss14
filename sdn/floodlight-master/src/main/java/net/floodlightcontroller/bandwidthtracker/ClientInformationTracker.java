package net.floodlightcontroller.bandwidthtracker;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import org.openflow.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/**
 * Created by fubezz on 14.05.14.
 */



public class ClientInformationTracker implements IFloodlightModule{
    private final static Logger log = LoggerFactory.getLogger(ClientInformationTracker.class);

    protected IFloodlightProviderService provider;
    protected IRestApiService service;

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
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        try {
            FlowTableGetter flowGetter = new FlowTableGetter(provider);
            Thread flowWorker = new Thread(flowGetter);
            flowWorker.start();
            RemoveMessageListener removeMessageListener = new RemoveMessageListener(flowGetter);
            provider.addOFMessageListener(OFType.FLOW_REMOVED, removeMessageListener);
        } catch(KeyManagementException e) {
            throw new FloodlightModuleException("Failed to initilize remove message listener.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new FloodlightModuleException("Failed to initilize remove message listener.", e);
        }
    }
}


