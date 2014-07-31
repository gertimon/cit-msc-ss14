package net.floodlightcontroller.bandwidthtracker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.tuberlin.cit.project.energy.zabbix.ZabbixAPIClient;
import de.tuberlin.cit.project.energy.zabbix.ZabbixSender;
import de.tuberlin.cit.project.energy.zabbix.exception.InternalErrorException;
import de.tuberlin.cit.project.energy.zabbix.exception.UserNotFoundException;
import de.tuberlin.cit.project.energy.zabbix.model.DatanodeUserConnection;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.IPv4;

import org.openflow.protocol.*;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;

/**
 * Created by fubezz on 18.05.14.
 */
public class FlowTableGetter implements Runnable {
    private final static Logger log = LoggerFactory.getLogger(FlowTableGetter.class);
    private final IFloodlightProviderService provider;
    final static String searchedIpdataNode1 = "10.0.42.1";
    final static String searchedIpdataNode2 = "10.0.42.3";
    final static String dataNodePort = "50010";
     HashMap<String, FlowInformation> flowMap;
     HashMap<String, ConnectionInfos> conInfMap;
    private ZabbixAPIClient zabbixApiClient;
    private ZabbixSender zabbixSender;


    public FlowTableGetter(IFloodlightProviderService floodlightProvider) {
        this.provider = floodlightProvider;
        this.flowMap = new HashMap<String, FlowInformation>();
        this.conInfMap = new HashMap<String, ConnectionInfos>();
        try {
            this.zabbixApiClient = new ZabbixAPIClient();
            zabbixSender = new ZabbixSender();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public class ConnectionInfos {
        String datanode;
        DatanodeUserConnection connection;

        private ConnectionInfos(String dataNode, DatanodeUserConnection connection) {
            this.datanode = dataNode;
            this.connection = connection;
        }
    }


    @Override
    public void run() {
        long time = System.currentTimeMillis();
        while (true) {
            long newTime = (System.currentTimeMillis() - time)/1000;
            handleFlows(provider);
            System.out.println("---------------------RUNNING FOR "+(int)newTime+"s--------------------------------");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    public void handleFlows(IFloodlightProviderService floodlightProvider) {
        List<IOFSwitch> switches = getSwitches(floodlightProvider);

        for (IOFSwitch sw : switches) {
            List<OFFlowStatisticsReply> flowTable = getSwitchFlowTable(sw, (short) -1);
            for (OFFlowStatisticsReply flow : flowTable) {

                FlowInformation flowInf = createFlowInformation(flow);
                String hashKey = flowInf.getHashKey();
                if (flowMap.containsKey(hashKey) && conInfMap.containsKey(hashKey)){
                    FlowInformation oldFlow = flowMap.get(hashKey);
                    ConnectionInfos conInf = conInfMap.get(hashKey);
                    FlowInformation modFlow = modifyFlow(flowInf,oldFlow);
                    if (modFlow != null){
                        //TODO Enable to send to Zabbix!
                        sendDataToZabbix(modFlow, conInf);
                        System.out.println(modFlow);
                        flowMap.remove(hashKey);
                        flowMap.put(hashKey, flowInf);
                    }

                }else {
                        ConnectionInfos conInf = getConnectionInfos(flowInf.getSrcIp(), flowInf.getSrcPort(), flowInf.getDstIp(), flowInf.getDstPort());
                            if (conInf != null) {
                                System.out.println("NEW CONNECTIONT: "+ flowInf);
                                //TODO Enable to send to Zabbix!
                                sendDataToZabbix(flowInf, conInf);
                                flowMap.put(hashKey, flowInf);
                                conInfMap.put(hashKey, conInf);
                            }
                    }

                }
            }



    }

    public FlowInformation modifyFlow(FlowInformation newInf, FlowInformation oldInf) {
        double newDataSize = newInf.getDataSize();
        double oldDataSize = oldInf.getDataSize();
        double newDurationTime = newInf.getTime();
        double oldDurationTime = oldInf.getTime();
            if (newDataSize != oldDataSize && newDurationTime != oldDurationTime) {
                double currentBandwidth = (newDataSize - oldDataSize) / (newDurationTime - oldDurationTime);
                oldInf.setDataSize(newDataSize);
                oldInf.setTime(newDurationTime);
                oldInf.setBandwidth(currentBandwidth);
                return oldInf;
            }
        return null;
    }



    public ConnectionInfos getConnectionInfos(String srcIp, String srcPort, String dstIp, String dstPort) {
        String dataNode;
        System.out.println("Connection from: " + srcIp+":"+ srcPort + " to " + dstIp + ":" + dstPort);
        try {
        if (srcPort.equals("50010")){
            //if (srcIp.equals("10.0.42.1")) dataNode = "CitProjectAsok05";
            //else dataNode = "CitProjectOffice";
            dataNode = getDataNodeByIP(srcIp);
            DatanodeUserConnection connection = null;
            System.err.println("SRC Get Connection: " + dataNode + ", " + dstIp +":"+dstPort);
            connection = zabbixApiClient.getUsernameByDataNodeConnection(dataNode,dstIp + ":" + dstPort);
            ConnectionInfos conInf = new ConnectionInfos(dataNode,connection);
            return conInf;
        }else if (dstPort.equals("50010")){
            //if (srcIp.equals("10.0.42.1")) dataNode = "CitProjectAsok05";
           // else dataNode = "CitProjectOffice";
            dataNode = getDataNodeByIP(dstIp);
            System.err.println("DST Get Connection: " + dataNode + ", " + srcIp +":"+srcPort);
            DatanodeUserConnection connection = null;
            connection = zabbixApiClient.getUsernameByDataNodeConnection(dataNode, srcIp + ":" + srcPort);
            ConnectionInfos conInf = new ConnectionInfos(dataNode,connection);
            return conInf;
        }else return null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticationException e) {
                e.printStackTrace();
            } catch (UserNotFoundException e) {
                e.printStackTrace();
            } catch (InternalErrorException e) {
                e.printStackTrace();
            }finally {
            return  null;
        }


        /*try {
            if ((srcIp.equals(searchedIpdataNode1) || srcIp.equals(searchedIpdataNode2)) && srcPort.equals(dataNodePort) &&
                    !(dstIp.equals(searchedIpdataNode2) || dstIp.equals(searchedIpdataNode1))) {
                dataNode = getDataNodeByIP(srcIp);
                DatanodeUserConnection connection = zabbixApiClient.getUsernameByDataNodeConnection(dataNode,dstIp + ":" + dstPort);
                //user = zabbixApiClient.getUsernameByDataNodeConnection(dataNode, dstIp + ":" + dstPort);
                ConnectionInfos conInf = new ConnectionInfos(dataNode,connection);
                return conInf;

            } else if ((dstIp.equals(searchedIpdataNode1) || dstIp.equals(searchedIpdataNode2)) &&
                    dstPort.equals(dataNodePort) && !(srcIp.equals(searchedIpdataNode2) || srcIp.equals(searchedIpdataNode1))) {
                dataNode = getDataNodeByIP(dstIp);
                DatanodeUserConnection connection = zabbixApiClient.getUsernameByDataNodeConnection(dataNode,srcIp + ":" + srcPort);
//                user = zabbixApiClient.getUsernameByDataNodeConnection(dataNode, srcIp + ":" + srcPort);
                ConnectionInfos conInf = new ConnectionInfos(dataNode,connection);
//                return conInf;
                return conInf;
            } else if ((dstIp.equals(searchedIpdataNode1) || dstIp.equals(searchedIpdataNode2)) &&
                    dstPort.equals(dataNodePort) && (srcIp.equals(searchedIpdataNode2) || srcIp.equals(searchedIpdataNode1))) {
                //TODO Handle Case if both dataNodes communicates
                return null;
            } else {
                return null;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        } catch (InternalErrorException e) {
           // e.printStackTrace();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } finally {
            //TODO Change to "return null"
            return null;
        }*/

    }

    public FlowInformation createFlowInformation(OFFlowStatisticsReply flow) {
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
        int time = flow.getDurationSeconds();
        long startTime = timeStamp - (time * 1000);
        FlowInformation flowInf = new FlowInformation(startTime, timeStamp, dl_src, dl_dst, nw_src, nw_dst, srcPort, dstPort, count, time);
        return flowInf;
    }


    public List<IOFSwitch> getSwitches(IFloodlightProviderService floodlightProvider) {
        List<IOFSwitch> switchList = new ArrayList<IOFSwitch>();

        Map<Long, IOFSwitch> switchMap = floodlightProvider.getAllSwitchMap();
        for (Map.Entry<Long, IOFSwitch> entry : switchMap.entrySet()) {
            switchList.add(entry.getValue());
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

    private String getDataNodeByIP(String dstIp) {
        try {
            return InetAddress.getByName(dstIp).getHostName();
        } catch (UnknownHostException e) {
           return null;
        }
    }

    public void sendDataToZabbix(FlowInformation flowInf, ConnectionInfos conInf) {
        long ts = System.currentTimeMillis() / 1000;
        System.err.println("Ich schicke etwas");
        if (conInf.connection.isInternal()){
          zabbixSender.sendInternalBandwidthUsage(conInf.datanode,conInf.connection.getUser(),flowInf.getBandWidth(),ts);
        }else{
            zabbixSender.sendBandwidthUsage(conInf.datanode,conInf.connection.getUser(),flowInf.getBandWidth(),ts);
        }
       // this.zabbixSender.sendDuration(dataNode, user, flowInf.getTime());
    }

}
