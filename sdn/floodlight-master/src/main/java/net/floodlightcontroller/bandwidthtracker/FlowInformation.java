package net.floodlightcontroller.bandwidthtracker;

public class FlowInformation {
    private int flowId;
    private String srcIp;
    private String dstIp;
    private double dataSize;
    private double sizeMB;



    private double time;

    public FlowInformation(int id,String src, String dst, long dataSize, double time){
        flowId = id;
        srcIp = src;
        dstIp = dst;
        this.dataSize = dataSize;
        this.time = time;
    }

    public int getFlowId() {
        return flowId;
    }

    public void setFlowId(int flowId) {
        this.flowId = flowId;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public String getSrcMac() {
        return srcMac;
    }

    public void setSrcMac(String srcMac) {
        this.srcMac = srcMac;
    }

    public String getDstMac() {
        return dstMac;
    }

    public void setDstMac(String dstMac) {
        this.dstMac = dstMac;
    }

    public double getDataSize() {
        return dataSize;
    }

    public void setDataSize(double dataSize) {
        this.dataSize = dataSize;
        sizeMB = dataSize/1000000;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String toString(){
        return "Flow: " + flowId + ", Src: " + srcIp + ", Dst: " + dstIp + ", Datasize in MB: " + sizeMB + ", DurationTime: " + time +" s";
    }
}
