package de.tuberlin.cit.project.energy.reporting.model;

import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * represents power in kwh used for a specific range.
 *
 * @author Tobias
 */
public class Power {

    private List<PowerValue> powerValues;

    public Power(List<ZabbixHistoryObject> response) {
        powerValues = new ArrayList<>();
        for (ZabbixHistoryObject zabbixHistoryObject : response) {
            powerValues.add(new PowerValue(zabbixHistoryObject.getClock(), zabbixHistoryObject.getIntValue()));
        }
    }

    public Power() {
        this.powerValues = new ArrayList<>();
    }

    public void setPowerValues(List<PowerValue> powerValues) {
        this.powerValues = powerValues;
    }

    public void addValue(long timeInSeconds, int powerInWatt) {
        this.powerValues.add(new PowerValue(timeInSeconds, powerInWatt));
    }

    public List<PowerValue> getPowerValues() {
        return powerValues;
    }

    /**
     * converts wattseconds to kwh. dividing given value through 60*60*1000.
     *
     * @param wattSeconds
     * @return
     */
    public static Double wsToKwh(Double wattSeconds) {
        return wattSeconds / 60 * 60 * 1000;
    }

    public static Double getPowerAsWattSeconds(List<PowerValue> powerValues, long startTimeSeconds, long endTimeSeconds) {
        // use seconds for times
        long lastTimeSeconds = startTimeSeconds;
        double wattSeconds = 0.0;
        float value;
        if (powerValues != null) {
            Collections.sort(powerValues);
            for (PowerValue pv : powerValues) {
                if (pv.timeAsSeconds < endTimeSeconds) {
                    value = pv.powerAsWatt;
                    wattSeconds += (pv.timeAsSeconds - lastTimeSeconds) * value;
                    lastTimeSeconds = pv.timeAsSeconds;
                } else {
                    // will ignore last two seconds...
                    break;
                }
            }
            return wattSeconds / (endTimeSeconds - startTimeSeconds);
        }
        return null;
    }

    PowerValue createPowerValue(Long timeMillis, int i) {
        return new PowerValue(timeMillis / 1000, i);
    }

    public class PowerValue implements Comparable<PowerValue> {

        public long timeAsSeconds;
        public int powerAsWatt;

        public PowerValue(long timeAsSeconds, int powerAsWatt) {
            this.timeAsSeconds = timeAsSeconds;
            this.powerAsWatt = powerAsWatt;
        }

        @Override
        public int compareTo(PowerValue other) {
            return (int) (timeAsSeconds - other.timeAsSeconds);
        }
    }
}
