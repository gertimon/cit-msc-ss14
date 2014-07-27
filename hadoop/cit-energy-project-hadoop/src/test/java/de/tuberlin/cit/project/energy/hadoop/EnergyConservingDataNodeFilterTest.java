/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuberlin.cit.project.energy.hadoop;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
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

}
