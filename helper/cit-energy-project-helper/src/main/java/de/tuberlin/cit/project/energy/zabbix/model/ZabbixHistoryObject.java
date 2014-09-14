package de.tuberlin.cit.project.energy.zabbix.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;

/**
 * Simple POJO wrapper around Zabbix history objects.
 *
 * @see
 * https://www.zabbix.com/documentation/2.2/manual/api/reference/history/object
 * @author Sascha Wolke
 */
public class ZabbixHistoryObject {

    @JsonProperty("itemid")
    private int itemId;
    private long clock; // seconds
    private String value;
    private long ns;
    @JsonProperty("hosts")
    private List<HostId> hostIds;
    private String username;

    public static class HostId {

        @JsonProperty("hostid")
        private int hostId;

        public int getHostId() {
            return hostId;
        }
    }

    public int getItemId() {
        return itemId;
    }

    /**
     * returns a timestamp in seconds. for conversion to date multiply value by
     * 1000.
     *
     * @return
     */
    public long getClock() {
        return clock;
    }

    public String getValue() {
        return value;
    }

    public long getNs() {
        return ns;
    }

    public List<HostId> getHostIds() {
        return hostIds;
    }

    public Date getDate() {
        return new Date(this.clock * 1000);
    }

    public int getIntValue() {
        return Integer.parseInt(this.value);
    }
    
    public long getLongValue() {
        return Long.parseLong(this.value);
    }

    public float getFloatValue() {
        return Float.parseFloat(this.value);
    }
    
    public String getUserName() {
        return username;
    }
    
    //required for getNumericHistory methods @ZabbixAPIClient
    public void setUserName(String name) {
        username = name;
    }

    @Override
    public String toString() {
        return "ZabbixHistoryObject{" + "itemId=" + itemId + ", clock=" + clock + ", value=" + value + ", ns=" + ns +", username=" + username + ", hostIds=" + hostIds + '}';
    }

}
