package de.tuberlin.cit.project.energy.reporting.model;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 * represents user storage amount by time.
 *
 * @author Tobias
 */
public class Storage {

    private final TreeMap<Long, Double> storageValues; // at least one value earlier than fromTimeMillis

    /**
     * To initialize Storage, create a Map with time stamps (Long) and storage
     * amount values (Double).
     *
     * @param storageValues
     * @param fromTimeMillis
     * @param toTimeMillis
     */
    public Storage(TreeMap<Long, Double> storageValues) {
        this.storageValues = storageValues;
    }

    /**
     * calculate median for given range of time. Needs at least one entry in
     * storageValues with a smaller or equal time value than requested.
     *
     * @param fromTimeMillis
     * @param toTimeMillis
     * @return
     * @throws Exception
     */
    public double calculateWeigthedHarmonicMedian(long fromTimeMillis, long toTimeMillis) throws Exception {
        // create ordered list of all given time stamps
        List<Long> times = new LinkedList<Long>(storageValues.keySet());
        int size = times.size();

        // test that there is an key existing smaller fromTimeMillis
        if (size == 0 || times.get(0) > fromTimeMillis) {
            times.add(0, 0l);
        }

        long lastTime = 0;
        long duration;

        double weights = 0, durations = 0, value = 0;

        for (long time : times) {

            if (time >= fromTimeMillis) {
                // given value after start time
                // retrieve weight for value
                if (lastTime < fromTimeMillis) {
                    // first run, calculate time from given range
                    duration = time - fromTimeMillis;
                } else {
                    // not first run, calculate difference to last entry
                    duration = time - lastTime;
                }
                // retrieve storage value
                value = storageValues.get(lastTime);

                // add values to lists
                weights += (duration * value);
                durations += duration;
            }
            // keep last time entry
            lastTime = time;
        }
        // add last row
        if (lastTime < toTimeMillis) {
            duration = toTimeMillis - lastTime;
            value = storageValues.get(lastTime);
            // add values to lists
            weights += (duration * value);
            durations += duration;
        }

        return weights / durations;
    }

}
