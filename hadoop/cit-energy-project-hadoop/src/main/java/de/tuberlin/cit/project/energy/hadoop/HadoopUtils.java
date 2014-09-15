package de.tuberlin.cit.project.energy.hadoop;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;

public class HadoopUtils {
    
    protected static Set<String> getDataNodeNames(LocatedBlocks locatedBlocks) {
        Set<String> datanodes = new HashSet<String>();
        for (LocatedBlock block : locatedBlocks.getLocatedBlocks()) {
            for (DatanodeInfo datanode : block.getLocations()) {
                datanodes.add(datanode.getHostName());
            }
        }

        return datanodes;
    }
}
