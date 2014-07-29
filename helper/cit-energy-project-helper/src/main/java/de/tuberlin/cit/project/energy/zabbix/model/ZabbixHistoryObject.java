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
    private long clock;
    private String value;
    private long ns;
    @JsonProperty("hosts")
    private List<HostId> hostIds;

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

    public float getFloatValue() {
        return Float.parseFloat(this.value);
    }

    @Override
    public String toString() {
        return "ZabbixHistoryObject{" + "itemId=" + itemId + ", clock=" + clock + ", value=" + value + ", ns=" + ns + ", hostIds=" + hostIds + '}';
    }

}
