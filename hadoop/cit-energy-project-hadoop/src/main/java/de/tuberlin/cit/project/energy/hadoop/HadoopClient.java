package de.tuberlin.cit.project.energy.hadoop;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Simple hadoop client, shows block locations and produces some data node traffic.
 * 
 * @author Sascha Wolke
 */
public class HadoopClient {
	private final FileSystem fs;
	
	public HadoopClient() throws IOException {
		Configuration conf = new Configuration();
		this.fs = FileSystem.get(conf);
	}
		
	public void listBlockLocations(Path path) throws IOException {
		if (this.fs.exists(path)) {
			FileStatus fileStatus = this.fs.getFileStatus(path);
			long length = this.fs.getFileStatus(path).getLen();
			System.out.println("Block locations for " + path + " ("
					+ "owner: " + fileStatus.getOwner() 
					+ ", length: " + fileStatus.getLen()
					+ ")");
			for (BlockLocation location : this.fs.getFileBlockLocations(path, 0, length)) {
				System.out.println(
						location.getOffset() + "-" + (location.getOffset() + location.getLength()) + ": "
								+ Arrays.toString(location.getHosts())
								+ " (" + Arrays.toString(location.getTopologyPaths()) + ")");
			}
			System.out.print("Reading first 1024bytes from hdfs...");
			FSDataInputStream fsDataInputStream = fs.open(path);
			InputStreamReader reader = new InputStreamReader(fsDataInputStream);
			char cbuf[] = new char[1024];
			reader.read(cbuf);
			reader.close();
			System.out.println("done.");
		} else {
			System.err.println("Can't find path " + path);
		}
	}

	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage: HadoopClient hadoop-file-path-1 [hadoop-file-path-2...]");
		} else {
			HadoopClient client = new HadoopClient();
			for (String file : args)
				client.listBlockLocations(new Path(file));
		}
	}
}
