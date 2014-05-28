package net.floodlightcontroller.bandwidthtracker;

public class FlowInformation {
    private int flowId;
    private String srcMac;
    private String dstMac;
    private double dataSize;



    private double time;

    public FlowInformation(int id,String src, String dst, double dataSize, double time){
        flowId = id;
        srcMac = src;
        dstMac = dst;
        this.dataSize = dataSize;
        this.time = time;
    }

    public int getFlowId() {
        return flowId;
    }

    public void setFlowId(int flowId) {
        this.flowId = flowId;
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
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String toString(){
        return "Flow: " + flowId + ", Src: " + srcMac + ", Dst: " + dstMac + ", Datasize: " + dataSize + ", DurationTime: " + time +" s";
    }
}