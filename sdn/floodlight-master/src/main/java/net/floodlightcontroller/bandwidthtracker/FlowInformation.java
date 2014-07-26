package net.floodlightcontroller.bandwidthtracker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class FlowInformation {
    private long startTime;
    private long endTime;
    private String srcIp;
    private String dstIp;
    private String srcPort;
    private String dstPort;
    private String srcMac;
    private String dstMac;
    private double dataSize;
    private double bandWidth;
    private double time;
    private String hashKey;
    private DecimalFormat df = new DecimalFormat("######,##");



    public FlowInformation(long startTime, long endTime, String srcMac, String dstMac, String srcIP, String dstIP, String srcPort,String dstPort, long dataSize, double time){
        this.startTime = startTime;
        this.endTime = endTime;
        this.srcIp = srcIP;
        this.dstIp = dstIP;
        this.srcMac = srcMac;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.dstMac = dstMac;
        this.dataSize = (dataSize);
        this.time = time ;
        bandWidth = computeBandwidth(this.dataSize/this.time);
        hashKey = this.srcIp + this.srcPort + this.dstIp + this.dstPort;
    }

    public double getBandWidth() {
        return bandWidth;
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
        String flow ="MAC_Src: " + srcMac + ", IP_Src: " +
                srcIp + ", SrcPort:" + srcPort + ", MAC_Dst: " + dstMac + ", IP_Dst: " + dstIp + ", DstPort: "+ dstPort + ", Datasize in kB: " +
                dataSize + ", DurationTime: " + time +" s" + ", Bandwidth: " + bandWidth + " kB/s";
        return flow;
    }

    public String getHashKey(){
        return hashKey;
    }

    public void setBandwidth(double bandwidth) {
        this.bandWidth = computeBandwidth(bandWidth);
    }

    private double computeBandwidth(double bandWidth){
        if (!Double.isInfinite(bandWidth) && !Double.isNaN(bandWidth)){
            return round(bandWidth,2);
        }else return 0.0;
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
