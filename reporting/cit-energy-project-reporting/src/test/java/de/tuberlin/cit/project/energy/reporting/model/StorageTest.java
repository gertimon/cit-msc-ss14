package de.tuberlin.cit.project.energy.reporting.model;

import java.util.TreeMap;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Tobias
 */
public class StorageTest {

    public StorageTest() {
    }

    @Test
    public void testSomeMethod() {
    }

    @Test
    public void testGetStorageMedian() throws Exception {

        // define a test day
        long fromTimeMillis = 1l;//new Date().getTime();
        long toTimeMillis = fromTimeMillis + 3;// * 21600000; // ein drei-viertel Tag

        // add some sample storage change entries
        TreeMap<Long, Double> values = new TreeMap<>();

        // add some older entries
        values.put(fromTimeMillis - 30, 5000.0);
        values.put(fromTimeMillis - 1, 50.0);

        // add some changes during the day
        values.put(fromTimeMillis + 2, 100.0);//* 21600000, 100.0); // nach 12 Stunden

        // create storage object
        Storage storage = new Storage(values);

        // calculate median for given day
        Assert.assertEquals(200.0 / 3, storage.calculateWeigthedHarmonicMedian(fromTimeMillis, toTimeMillis));

    }

}
