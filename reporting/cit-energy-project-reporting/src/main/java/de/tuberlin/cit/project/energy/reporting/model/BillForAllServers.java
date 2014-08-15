package de.tuberlin.cit.project.energy.reporting.model;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by fubezz on 15.08.14.
 */
public class BillForAllServers {
    private final Bill asok;
    private final Bill office;
    private long fromTime;
    private long toTime;

    public BillForAllServers(long from, long to, Bill asok, Bill office) {
        this.asok = asok;
        this.office = office;
        fromTime = from;
        toTime = to;
    }

    public BillForAllServers(Bill asok, Bill office) {
        this.asok = asok;
        this.office = office;
    }

    public void setToTime(long toTime) {
        this.toTime = toTime;
    }

    public void setFromTime(long fromTime) {
        this.fromTime = fromTime;
    }

    public long getToTime() {
        return toTime;
    }

    public long getFromTime() {
        return fromTime;
    }

    public Bill getOffice() {
        return office;
    }

    public Bill getAsok() {
        return asok;
    }

    @Override
    public String toString(){
        Date from = new Date(fromTime*1000);
        Date to = new Date(toTime*1000);
        return from + " till " + to + ":\n" + "Asok: " + asok.getUser() + " Price: " + asok.getPrice() + "\n" + "Office: " + office.getUser() + " Price: " + office.getPrice();
    }


}
