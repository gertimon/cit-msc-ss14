package de.tuberlin.cit.project.energy.zabbix.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple POJO wrapper around Zabbix Items. All not listed properties are ignored. Feel free to more if necessary :-)
 * @see https://www.zabbix.com/documentation/2.2/manual/api/reference/item/object
 * @author Sascha Wolke
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ZabbixItem {
    @JsonProperty("itemid")
    private int itemId = -1;
    @JsonProperty("hostid")
    private int hostId = -1;
    private String name;
    @JsonProperty("key_")
    private String key;
    private int history = -1;
    @JsonProperty("lastclock")
    private long lastClock = -1;
    @JsonProperty("valuetype")
    private int valueType = -1;
    @JsonProperty("lastvalue")
    private String lastValue;
    @JsonProperty("prevvalue")
    private String prevValue;
    
    public int getItemId() { return itemId; }
    public int getHostId() { return hostId; }
    public String getName() { return name; }
    public String getKey() { return key; }
    public int getHistory() { return history; }
    public long getLastClock() { return lastClock; }
    public int getValueType() { return valueType; }
    public String getLastValue() { return lastValue; }
    public String getPrevValue() { return prevValue; }
    
    public Date getLastDate() {
        return new Date(this.lastClock * 1000);
    }

    @Override
    public String toString() {
        List<String> keyValues = new LinkedList<String>();

        if (itemId > 0)
            keyValues.add("itemId=" + this.itemId);
        if (hostId > 0)
            keyValues.add("hostId=" + this.hostId);
        if (key != null)
            keyValues.add("key_=" + this.key);
        if (history > 0)
            keyValues.add("history=" + this.history);
        if (name != null)
            keyValues.add("name=" + this.name);
        if (history > 0)
            keyValues.add("history=" + this.history);
        if (lastClock > 0)
            keyValues.add("lastClock=" + this.lastClock);
        if (valueType > 0)
            keyValues.add("valueType=" + this.valueType);
        if (lastValue != null)
            keyValues.add("lastValue=" + this.lastValue);
        if (prevValue != null)
            keyValues.add("prevValue=" + this.prevValue);

        return "ZabbixItem" + keyValues + "";
    }
}
