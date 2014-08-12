package de.tuberlin.cit.project.energy.reporting.model;

/**
 * Created by fubezz on 11.08.14.
 */
public class Bill {
    final String user;
    final Double powerUsagePerHour;
    final Double traffic;
    final Double price;

    public Bill(String user, Double powerUsagePerHour, Double traffic, Double price) {
        this.user = user;
        this.powerUsagePerHour = powerUsagePerHour;
        this.traffic = traffic;
        this.price = price;
    }

    public Double getPrice() {
        return price;
    }

    public Double getTraffic() {
        return traffic;
    }

    public Double getPowerUsagePerHour() {
        return powerUsagePerHour;
    }

    public String getUser() {
        return user;
    }


}
