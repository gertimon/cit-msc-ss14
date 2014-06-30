package de.tuberlin.cit.project.energy.hadoop;

import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Tobias
 */
public class EnergyBaseDataNodeFilterTest {

    EnergyBaseDataNodeFilter filter;

    public EnergyBaseDataNodeFilterTest() {
        filter = new EnergyBaseDataNodeFilter(null, 0, null, null, null);
    }

    @Test
    public void testFilterBlockLocations() {
        // TODO
    }

    /**
     * Test that a new copy of a Block only changes the datanodeinfo []
     * locations.
     */
    @Test
    public void testCreateLocatedBlock() {
// TODO
        ExtendedBlock extendedBlock = new ExtendedBlock();
        DatanodeInfo[] locs = new DatanodeInfo[2];
//        locs[0] = new DatanodeInfo(new DatanodeID, null)
//
//        LocatedBlock block = new LocatedBlock(extendedBlock, locs, storageIDs, storageTypes, startOffset, true, cachedLocs);
//        DatanodeInfo[] locations = {""};

    }

    /**
     * Tests that there is one entry for cheap and fast as default in properties
     * given. Update on change of that file.
     */
    @Test
    public void testGetRacks() {
        Assert.assertEquals(1, filter.getRacks(EnergyBaseDataNodeFilter.EnergyMode.CHEAP).size());
        Assert.assertEquals(1, filter.getRacks(EnergyBaseDataNodeFilter.EnergyMode.FAST).size());
    }

    @Test
    public void testGetUserEnergyMapping() {
        // TODO or NOT
    }

    @Ignore("Done by testGetRacks()")
    @Test
    public void testLoadProperties() throws Exception {
    }

}
