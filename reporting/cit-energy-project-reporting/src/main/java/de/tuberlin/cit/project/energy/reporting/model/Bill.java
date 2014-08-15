package de.tuberlin.cit.project.energy.reporting.model;

/**
 * Created by fubezz on 11.08.14.
 */
public class Bill {
    private final String server;
    private final String user;
    private final float kWhOFUser;
    private final float averageTraffic;
    private final float averageStorage;
    private final Double price;


    public Bill(String server, String user, float kWhOfUser, float averageTraffic, long averageStorage, double price) {
        this.server = server;
        this.user = user;
        this.kWhOFUser = kWhOfUser;
        this.averageTraffic = averageTraffic;
        this.averageStorage = averageStorage;
        this.price = price;
    }

    public float getAverageStorage() {
        return averageStorage;
    }

    public String getServer() {
        return server;
    }

    public String getUser() {
        return user;
    }

    public float getkWhOFUser() {
        return kWhOFUser;
    }

    public float getAverageTraffic() {
        return averageTraffic;
    }

    public Double getPrice() {
        return price;
    }

}
