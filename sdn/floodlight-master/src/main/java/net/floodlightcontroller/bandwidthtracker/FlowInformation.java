package net.floodlightcontroller.bandwidthtracker;

public class FlowInformation {
    private long startTime;
    private long endTime;
    private int switchId;
    private String srcIp;
    private String dstIp;
    private String srcPort;
    private String dstPort;
    private String srcMac;
    private String dstMac;
    private double dataSize;
    private double bandWith;
    private double time;




    public FlowInformation(int id,long startTime, long endTime, String srcMac, String dstMac, String srcIP, String dstIP, String srcPort,String dstPort, long dataSize, double time){
        switchId = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.srcIp = srcIP;
        this.dstIp = dstIP;
        this.srcMac = srcMac;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.dstMac = dstMac;
        this.dataSize = (dataSize/1000);
        this.time = time;
        bandWith = this.dataSize/this.time;
    }

    public double getBandWith() {
        return bandWith;
    }

    public String getDstIp() {
        return dstIp;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public double getDataSize() {
        return dataSize;
    }

    public void setDataSize(double dataSize) {
        this.dataSize = dataSize;

    }

    public String getSrcPort() {
        return srcPort;
    }

    public String getDstPort() {
        return dstPort;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String toString(){
        return "Switch: " + switchId + ", MAC_Src: " + srcMac + ", IP_Src: " +
                srcIp + ", SrcPort:" + srcPort + ", MAC_Dst: " + dstMac + ", IP_Dst: " + dstIp + ", DstPort: "+ dstPort + ", Datasize in MB: " +
                dataSize + ", DurationTime: " + time +" s" + ", Bandwidth: " + bandWith + " kB/s";
    }

}
