package de.tuberlin.cit.project.energy.reporting;

import java.util.Collection;

/**
 *
 * @author Tobias
 */
public interface ReportingZabbixRequestsInterface {

    /**
     *
     * @param fromMillis
     * @param toMillis
     * @return a json string that keeps power information of different machines
     * for the given range
     */
    public String getPowerUsageForRange(long fromMillis, long toMillis);

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
     * User storage is measured by collecting all data inside of an users home
     * directory. that should automatically created on user setup. a user should
     * not access any different folders. the directory measured could be
     * /user/username for example.
     *
     * @param userName
     * @param fromMillis
     * @param toMillis
     * @return json response from zabbix containing an array of storage
     * information related to given user by userName
     */
    public String getUserStorage(String userName, long fromMillis, long toMillis);

    /**
     *
     * @param fromMillis
     * @param toMillis
     * @return usernames array that have traffic/storage used in range
     */
    public Collection<String> getUserNamesforRange(long fromMillis, long toMillis);

    /**
     * may ask at zabbix for all known users or use our small database.
     *
     * @return all usernames known in the system
     */
    public Collection<String> getAllUsernames();

}
