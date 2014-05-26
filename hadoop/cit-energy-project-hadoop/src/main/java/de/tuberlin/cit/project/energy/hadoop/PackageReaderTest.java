package de.tuberlin.cit.project.energy.hadoop;

import static org.apache.hadoop.hdfs.protocolPB.PBHelper.vintPrefixed;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;

import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.datatransfer.DataTransferProtocol;
import org.apache.hadoop.hdfs.protocol.datatransfer.Op;
import org.apache.hadoop.hdfs.protocol.proto.DataTransferProtos.OpReadBlockProto;
import org.apache.hadoop.hdfs.protocolPB.PBHelper;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.security.token.Token;

/**
 * This class reads a data node package, send from a client and shows some information about it.
 * 
 * @author Sascha Wolke
 * 
 * @see org.apache.hadoop.hdfs.server.datanode.DataXceiver#run
 * @see org.apache.hadoop.hdfs.protocol.datatransfer.Receiver#opReadBlock
 */
public class PackageReaderTest {

	public static void main(String[] args) {
		try {
			System.out.println("Reading file: "+ args[0]);

			ByteArrayInputStream inputStream = new ByteArrayInputStream(CLIENT_TO_DATANODE_READ_BLOCK);
			DataInputStream dataInputStream = new DataInputStream(inputStream);
			
			final short version = dataInputStream.readShort();
	    	System.out.println("Found Version: " + version + " vs. " + DataTransferProtocol.DATA_TRANSFER_VERSION);
		    if (version == DataTransferProtocol.DATA_TRANSFER_VERSION) {
		    	final Op operation = Op.read(dataInputStream);
		    	
		    	System.out.println("Found operation: " + operation);
		    	
		        OpReadBlockProto proto = OpReadBlockProto.parseFrom(vintPrefixed(dataInputStream));
		        
		        final ExtendedBlock block = PBHelper.convert(proto.getHeader().getBaseHeader().getBlock());
		        final Token<BlockTokenIdentifier> blockToken = PBHelper.convert(proto.getHeader().getBaseHeader().getToken());
		        final String clientName = proto.getHeader().getClientName();
		        final long blockOffset = proto.getOffset();
		        final long length = proto.getLen();
		        final boolean sendChecksum = proto.getSendChecksums();

		        System.out.println("Found package: "
		        		+ "extendedBlock: " + block
		        		+ ", blockToken: " + blockToken
		        		+ ", username: " + blockToken.decodeIdentifier().getUserId()
		        		+ ", clientName: " + clientName
		        		+ ", offset: " + blockOffset
		        		+ ", length: " + length
		        		+ ", sendChecksum: " + sendChecksum);
		    }

		    dataInputStream.close();
		    inputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public final static byte[] CLIENT_TO_DATANODE_READ_BLOCK = new byte[]{
			(byte) 0x00, (byte) 0x1C, (byte) 0x51, (byte) 0xDF, (byte) 0x01,
			(byte) 0x0A, (byte) 0xD3, (byte) 0x01, (byte) 0x0A, (byte) 0xAB,
			(byte) 0x01, (byte) 0x0A, (byte) 0x33, (byte) 0x0A, (byte) 0x25,
			(byte) 0x42, (byte) 0x50, (byte) 0x2D, (byte) 0x31, (byte) 0x32,
			(byte) 0x30, (byte) 0x37, (byte) 0x39, (byte) 0x33, (byte) 0x34,
			(byte) 0x31, (byte) 0x30, (byte) 0x34, (byte) 0x2D, (byte) 0x31,
			(byte) 0x32, (byte) 0x37, (byte) 0x2E, (byte) 0x30, (byte) 0x2E,
			(byte) 0x31, (byte) 0x2E, (byte) 0x31, (byte) 0x2D, (byte) 0x31,
			(byte) 0x34, (byte) 0x30, (byte) 0x30, (byte) 0x37, (byte) 0x34,
			(byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x31, (byte) 0x39,
			(byte) 0x31, (byte) 0x30, (byte) 0x10, (byte) 0x87, (byte) 0x80,
			(byte) 0x80, (byte) 0x80, (byte) 0x04, (byte) 0x18, (byte) 0xEF,
			(byte) 0x07, (byte) 0x20, (byte) 0xE8, (byte) 0x07, (byte) 0x12,
			(byte) 0x74, (byte) 0x0A, (byte) 0x48, (byte) 0x8A, (byte) 0x01,
			(byte) 0x46, (byte) 0x3A, (byte) 0x8F, (byte) 0x43, (byte) 0x12,
			(byte) 0x8C, (byte) 0x02, (byte) 0x1F, (byte) 0xDB, (byte) 0x1D,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x68,
			(byte) 0x75, (byte) 0x67, (byte) 0x6F, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x25, (byte) 0x42, (byte) 0x50, (byte) 0x2D,
			(byte) 0x31, (byte) 0x32, (byte) 0x30, (byte) 0x37, (byte) 0x39,
			(byte) 0x33, (byte) 0x34, (byte) 0x31, (byte) 0x30, (byte) 0x34,
			(byte) 0x2D, (byte) 0x31, (byte) 0x32, (byte) 0x37, (byte) 0x2E,
			(byte) 0x30, (byte) 0x2E, (byte) 0x31, (byte) 0x2E, (byte) 0x31,
			(byte) 0x2D, (byte) 0x31, (byte) 0x34, (byte) 0x30, (byte) 0x30,
			(byte) 0x37, (byte) 0x34, (byte) 0x32, (byte) 0x33, (byte) 0x34,
			(byte) 0x31, (byte) 0x39, (byte) 0x31, (byte) 0x30, (byte) 0x8C,
			(byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x01,
			(byte) 0x04, (byte) 0x52, (byte) 0x45, (byte) 0x41, (byte) 0x44,
			(byte) 0x12, (byte) 0x14, (byte) 0xA6, (byte) 0xF1, (byte) 0xED,
			(byte) 0x09, (byte) 0xCA, (byte) 0x45, (byte) 0x6E, (byte) 0x43,
			(byte) 0x55, (byte) 0x79, (byte) 0xB1, (byte) 0x1A, (byte) 0xC9,
			(byte) 0x86, (byte) 0xE7, (byte) 0x7C, (byte) 0x39, (byte) 0x83,
			(byte) 0x45, (byte) 0x7C, (byte) 0x1A, (byte) 0x10, (byte) 0x48,
			(byte) 0x44, (byte) 0x46, (byte) 0x53, (byte) 0x5F, (byte) 0x42,
			(byte) 0x4C, (byte) 0x4F, (byte) 0x43, (byte) 0x4B, (byte) 0x5F,
			(byte) 0x54, (byte) 0x4F, (byte) 0x4B, (byte) 0x45, (byte) 0x4E,
			(byte) 0x22, (byte) 0x00, (byte) 0x12, (byte) 0x23, (byte) 0x44,
			(byte) 0x46, (byte) 0x53, (byte) 0x43, (byte) 0x6C, (byte) 0x69,
			(byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x5F, (byte) 0x4E,
			(byte) 0x4F, (byte) 0x4E, (byte) 0x4D, (byte) 0x41, (byte) 0x50,
			(byte) 0x52, (byte) 0x45, (byte) 0x44, (byte) 0x55, (byte) 0x43,
			(byte) 0x45, (byte) 0x5F, (byte) 0x2D, (byte) 0x38, (byte) 0x37,
			(byte) 0x32, (byte) 0x33, (byte) 0x33, (byte) 0x30, (byte) 0x37,
			(byte) 0x36, (byte) 0x32, (byte) 0x5F, (byte) 0x31, (byte) 0x10,
			(byte) 0x00, (byte) 0x18, (byte) 0xE8, (byte) 0x07, (byte) 0x20,
			(byte) 0x01, (byte) 0x2A, (byte) 0x00, (byte) 0x02, (byte) 0x08,
			(byte) 0x06
	};
	
	public static void dumpFile(String filename) throws Exception {
		FileInputStream fileInputStream = new FileInputStream(filename);
		byte b[] = new byte[1];
		System.out.println("new byte[]{ ");
		while(fileInputStream.read(b) > 0) {
			System.out.printf("(byte) 0x%02X, ", b[0]);
		}
		System.out.println("}");
	}

}
