package de.tuberlin.cit.project.energy.reporting;

/**
 *
 * @author Tobias
 */
public interface ReportingZabbixRequests {

    /**
     *
     * @param userName
     * @param fromMillis
     * @param toMillis
     * @return json response from zabbix containing an array of traffic
     * information related to given user by userName
     */
    public String getUserTraffic(String userName, long fromMillis, long toMillis);

    /**
     *
     * @param userName
     * @param fromMillis
     * @param toMillis
     * @return json response from zabbix containing an array of storage
     * information related to given user by userName
     */
    public String getUserStorage(String userName, long fromMillis, long toMillis);

}
