package de.tuberlin.cit.project.energy.zabbix.model;

import java.util.Date;

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
    private int itemId;
    @JsonProperty("hostid")
    private int hostId;
    private String name;
    @JsonProperty("key_")
    private String key;
    private int history;
    private long lastClock;
    private int valueType;
    private String lastValue;
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
}
