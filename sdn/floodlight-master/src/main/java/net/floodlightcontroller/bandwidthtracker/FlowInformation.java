package net.floodlightcontroller.bandwidthtracker;

public class FlowInformation {
    private long startTime;
    private long endTime;
    private int switchId;
    private String srcIp;
    private String dstIp;
    private double dataSize;
    private String srcMac;
    private String dstMac;
    private double bandWith;



    private double time;



    public FlowInformation(int id,long startTime, long endTime, String srcMac, String dstMac, String srcIP, String dstIP, long dataSize, double time){
        switchId = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.srcIp = srcIP;
        this.dstIp = dstIP;
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.dataSize = (dataSize/1000000);
        this.time = time;
        bandWith = this.dataSize/this.time;
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

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String toString(){
        return "Switch: " + switchId + ", MAC_Src: " + srcMac + ", MAC_Dst: " + dstMac + ", IP_Src: " +
                srcIp + ", IP_Dst: " + dstIp + ", Datasize in MB: " +
                dataSize + ", DurationTime: " + time +" s" + ", Bandwidth: " + bandWith + " Mb/s";
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
