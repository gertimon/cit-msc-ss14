package de.tuberlin.cit.project.energy.reporting.model;

import de.tuberlin.cit.project.energy.zabbix.model.ZabbixHistoryObject;
import java.util.ArrayList;
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

    Power() {
    }

    public void setPowerValues(List<PowerValue> powerValues) {
        this.powerValues = powerValues;
    }

    public List<PowerValue> getPowerValues() {
        return powerValues;
    }

    /**
     * converts wattseconds to kwh. dividing given value through 3600000.
     *
     * @param wattSeconds
     * @return
     */
    public static Double wsToKwh(Double wattSeconds) {
        return wattSeconds / 3600000;
    }

    public static Double getPowerAsWattSeconds(List<PowerValue> powerValues, long startTimeMillis, long endTimeMillis) {
        // use seconds for times
        long lastTimeSeconds = startTimeMillis / 1000;
        long stopTimeSeconds = endTimeMillis / 1000;
        double wattSeconds = 0.0;
        float value;
        if (powerValues != null) {
            for (PowerValue pv : powerValues) {
                if (pv.timeAsSeconds < stopTimeSeconds) {
                    value = pv.powerAsWatt;
                    wattSeconds += (pv.timeAsSeconds - lastTimeSeconds) * value;
                    lastTimeSeconds = pv.timeAsSeconds;
                } else {
                    // will ignore last two seconds...
                    break;
                }
            }
            return wattSeconds;
        }
        return null;
    }

    PowerValue createPowerValue(Long timeMillis, int i) {
        return new PowerValue(timeMillis / 1000, i);
    }

    public class PowerValue {

        public long timeAsSeconds;
        public int powerAsWatt;

        public PowerValue(Long timeAsSeconds, int powerAsWatt) {
            this.timeAsSeconds = timeAsSeconds.intValue();
            this.powerAsWatt = powerAsWatt;
        }

    }

}
