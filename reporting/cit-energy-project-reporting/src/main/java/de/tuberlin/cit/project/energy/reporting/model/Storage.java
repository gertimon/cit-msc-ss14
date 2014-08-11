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

    private final TreeMap<Long, Double> storageValues; // at least one value earlier than fromTimeSeconds

    /**
     * To initialize Storage, create a Map with time stamps (Long) and storage
     * amount values (Double).
     *
     * @param storageValues
     */
    public Storage(TreeMap<Long, Double> storageValues) {
        this.storageValues = storageValues;
    }

    /**
     * calculate median for given range of time. Needs at least one entry in
     * storageValues with a smaller or equal time value than requested.
     *
     * @param fromTimeSeconds
     * @param toTimeSeconds
     * @return
     * @throws Exception
     */
    public double calculateWeigthedHarmonicMedian(long fromTimeSeconds, long toTimeSeconds) throws Exception {
        // create ordered list of all given time stamps
        List<Long> times = new LinkedList<>(storageValues.keySet());
        int size = times.size();

        // test that there is an key existing smaller fromTimeSeconds
        if (size == 0 || times.get(0) > fromTimeSeconds) {
            times.add(0, 0l);
            size++;
        }

        long lastTime = 0;
        long duration;

        double weights = 0, durations = 0, value = 0;

        for (long time : times) {

            if (time >= fromTimeSeconds) {
                // given value after start time
                // retrieve weight for value
                if (lastTime < fromTimeSeconds) {
                    // first run, calculate time from given range
                    duration = time - fromTimeSeconds;
                } else {
                    // not first run, calculate difference to last entry
                    duration = time - lastTime;
                }
                // retrieve storage value
                if (lastTime != 0l) {
                    value = storageValues.get(lastTime);
                }

                // add values to lists
                weights += (duration * value);
                durations += duration;
            }
            // keep last time entry
            lastTime = time;
        }
        // add last row
        if (lastTime < toTimeSeconds) {
            duration = toTimeSeconds - lastTime;
            value = storageValues.get(lastTime);
            // add values to lists
            weights += (duration * value);
            durations += duration;
        }

        if (durations != 0l) {
            return weights / durations;
        }
        return 0.0;
    }

}
