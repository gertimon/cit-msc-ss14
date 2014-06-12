package de.tuberlin.cit.project.energy.helper.zabbix;

import java.util.Date;
import java.util.List;

/**
 * Mapper for any zabbix requests. Implements a singleton, use getZabbixHelper()
 * for getting it's
  *
 * @author Tobias
 */
public class ZabbixHelper {

    private final String ZABBIX_API_URI;
    private final String ZABBIX_USER;
    private final String ZABBIX_PASSWORD;

    private static ZabbixHelper zabbixHelper;

    /**
     * private constructor, use getZabbixHelper() instead (singleton) TODO
     * extract attributes to property-file
     */
    private ZabbixHelper() {
        ZABBIX_API_URI = "";
        ZABBIX_USER = "";
        ZABBIX_PASSWORD = "";
    }

    /**
     * 
     * @return singleton instance of ZabbixHelper
     */
    public static synchronized ZabbixHelper getZabbixHelper() {
        return zabbixHelper == null ? zabbixHelper = new ZabbixHelper() : zabbixHelper;
    }

    /**
     * Used from NameNode to send username-IP-connection to zabbix
     *
     * @param ip
     * @param username
     */
    public void setIpForUser(String ip, String username) {
        ProjectTrapper.sendMetricJson("localhost", "BLAH", username + ".ip", false, ip);
    }

    /**
     * retrieves the last username known for a given ip address
     *
     * @param ip
     * @return
     */
    public String getUser(String ip) {
        return null;
    }

    public void incrementFileDownloadRequest(String filepath) {
    }

    public void setUserDirSize(String username, long bytes) {
    }

    /**
     * return latest entry for an attribute
     *
     * @param username
     * @param attribute
     * @return
     */
    public String getAttribute(String username, String attribute) {
        String value = null;
        // TODO
        return value;
    }

    /**
     * return list of attributes for a given range
     *
     * @param username
     * @param attribute
     * @param from
     * @param to
     * @return
     */
    public List<String> getAttributes(String username, String attribute, Date from, Date to) {
        return null;
    }


}
