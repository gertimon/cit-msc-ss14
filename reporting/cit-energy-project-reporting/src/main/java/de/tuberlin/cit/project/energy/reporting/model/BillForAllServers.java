package de.tuberlin.cit.project.energy.reporting.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;

/**
 * Created by fubezz on 15.08.14.
 */
public class BillForAllServers {
    private final Bill asok;
    private final Bill office;
    private long startTime;
    private long endTime;

    public BillForAllServers(long from, long to, Bill asok, Bill office) {
        this.asok = asok;
        this.office = office;
        startTime = from;
        endTime = to;
    }

    public BillForAllServers(Bill asok, Bill office) {
        this.asok = asok;
        this.office = office;
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

    public Bill getOffice() {
        return office;
    }

    public Bill getAsok() {
        return asok;
    }

    @Override
    public String toString(){
        Date from = new Date(startTime *1000);
        Date to = new Date(endTime *1000);
        return from + " till " + to + ":\n" + "Asok: " + asok.getUser() + " Price: " + asok.getPrice() + " €\n" + "Office: " + office.getUser() + " Price: " + office.getPrice() + " € \n";
    }


}
