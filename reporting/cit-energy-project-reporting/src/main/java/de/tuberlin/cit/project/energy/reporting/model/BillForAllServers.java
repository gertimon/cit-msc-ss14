package de.tuberlin.cit.project.energy.reporting.model;

import java.util.Date;
import java.util.List;

/**
 * Created by fubezz on 15.08.14.
 */
public class BillForAllServers {
    private final List<Bill> serverBills;
    private long startTime;
    private long endTime;

    public BillForAllServers(long from, long to, List<Bill> servers) {
        serverBills = servers;
        startTime = from;
        endTime = to;
    }

    public BillForAllServers(List<Bill> servers) {
        serverBills = servers;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public List<Bill> getServerBills() {
        return serverBills;
    }

    @Override
    public String toString(){
        Date from = new Date(startTime *1000);
        Date to = new Date(endTime *1000);
        String print = "";
        print = print.concat(from + " --> " + to + ":\n");
        for (Bill bill : serverBills){
            print = print.concat("Server: " + bill.getServer() + " User: " + bill.getUser() + " Price: " + bill.getPrice() + " €\n");
            print = print.concat("Average Traffic: " + bill.getAverageTraffic() + " B/s, " + bill.getPartitialTrafficInPercent() + " % of whole traffic\n");
            print = print.concat("Average Storage: " + bill.getAverageStorage() + " Bytes, " + bill.getPartitialStorageInPercent() + " % of whole used storage \n\n");
        }
        return print;
    }


}
