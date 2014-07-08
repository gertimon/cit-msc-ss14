package de.tuberlin.cit.project.energy.reporting.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tobias
 */
public class Storage {

    private final Map<Long, Double> storageValues; // at least one value earlier than fromTimeMillis 
    private final long fromTimeMillis;
    private final long toTimeMillis;

    public Storage(Map<Long, Double> storageValues, long fromTimeMillis, long toTimeMillis) {
        this.storageValues = storageValues;
        this.fromTimeMillis = fromTimeMillis;
        this.toTimeMillis = toTimeMillis;
    }

    public double getStorageMedian() throws Exception {
        // create ordered list of time stamps
        List<Long> times = new LinkedList<>(storageValues.keySet());
        Collections.sort(times);
        int size = times.size();

        // test that there is an element existing before fromTimeMillis
        if (size == 0 || times.get(0) > fromTimeMillis) {
            throw new Exception("need a storage value before fromTimeMillis");
        }

//        times.
        long durationComplete = toTimeMillis - fromTimeMillis;

        long lastTime = times.get(0);
        double result = 0.0;

        for (Long time : times) {
            if (time <= toTimeMillis) {
                lastTime = time;
            } else {
                double lastStorageValue = storageValues.get(lastTime);
                long durationPart = time - lastTime;

                lastTime = time;
            }

        }

        return result;
    }

}
