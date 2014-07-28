/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuberlin.cit.project.energy.hadoop;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Tobias
 */
public class EnergyConservingDataNodeFilterTest {

    public EnergyConservingDataNodeFilterTest() {
    }

    @Test
    @Ignore("not working yet")
    public void testCreateLocatedBlock() throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        LocatedBlocks locatedBlocks;
        locatedBlocks = mapper.readValue(this.getClass().getResourceAsStream("LocatedBlocksSample.json"), LocatedBlocks.class);

    }

    @Test
    public void ListToArrayTest() {

        List<DatanodeInfo> testList = new ArrayList<DatanodeInfo>();
        DatanodeInfo testNode = new DatanodeInfo(new DatanodeID("0.0.0.0", "hostname", "dnuuid", 0, 0, 33, 22));
        testList.add(testNode);
        testList.add(testNode);
        DatanodeInfo[] list = {};
        DatanodeInfo[] resultArray = testList.toArray(list);

        Assert.assertEquals(2, resultArray.length);
    }

}
