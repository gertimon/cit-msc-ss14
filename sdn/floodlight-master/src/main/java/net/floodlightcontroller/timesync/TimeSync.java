package net.floodlightcontroller.timesync;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;


public final class TimeSync {

	/* Uses the more accurate and more robust NTP */
	public static long sendTimeMessage(TimeInfo info) {
		
		NtpV3Packet ntpMessage = info.getMessage();
		long returnTimeStamp = 0;
		
		// TODO: Converting Apache's TimeStamp to the right data type to calculate duration time
		//       Return type for returning a time value back to RemoveMessageListener class
		
		TimeStamp refNtpTime = ntpMessage.getReferenceTimeStamp();
        System.out.println(" Reference Timestamp:\t" + refNtpTime + "  " + refNtpTime.toDateString()+ "   " +refNtpTime.getSeconds()+ "   " +refNtpTime.ntpValue());

        // Originate Time is time request sent by client (t1)
        TimeStamp origNtpTime = ntpMessage.getOriginateTimeStamp();
        System.out.println(" Originate Timestamp:\t" + origNtpTime + "  " + origNtpTime.toDateString()+ "   " +origNtpTime.getSeconds()+ "   " +origNtpTime.ntpValue());
        returnTimeStamp = origNtpTime.getSeconds();

        long destTime = info.getReturnTime();
        // Receive Time is time request received by server (t2)
        TimeStamp rcvNtpTime = ntpMessage.getReceiveTimeStamp();
        System.out.println(" Receive Timestamp:\t" + rcvNtpTime + "  " + rcvNtpTime.toDateString()+ "   " +rcvNtpTime.getSeconds()+ "   " +rcvNtpTime.ntpValue());

        // Transmit time is time reply sent by server (t3)
        TimeStamp xmitNtpTime = ntpMessage.getTransmitTimeStamp();
        System.out.println(" Transmit Timestamp:\t" + xmitNtpTime + "  " + xmitNtpTime.toDateString()+ "   " +xmitNtpTime.getSeconds()+ "   " +xmitNtpTime.ntpValue());

        // Destination time is time reply received by client (t4)
        TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
        System.out.println(" Destination Timestamp:\t" + destNtpTime + "  " + destNtpTime.toDateString()+ "   " +destNtpTime.getSeconds()+ "   " +destNtpTime.ntpValue());

        info.computeDetails(); // compute offset/delay if not already done
        Long offsetValue = info.getOffset();
        Long delayValue = info.getDelay();
        String delay = (delayValue == null) ? "N/A" : delayValue.toString();
        String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

        System.out.println(" Roundtrip delay(ms)=" + delay
                + ", clock offset(ms)=" + offset); // offset in ms
        return returnTimeStamp;
	}
	
	public static long getNtpTimestamp() {
		
		// --- NTPv3 Version (RFC 1305) --- // 
    	// http://commons.apache.org/proper/commons-net/javadocs/api-3.3/index.html
		long returnTimeStamp = 0;
    	NTPUDPClient timeClient = new NTPUDPClient();
    	timeClient.setDefaultTimeout(5000); // 5 sec
    	
    	try {
			timeClient.open();
			try {
				// see: https://www.eecs.tu-berlin.de/irb/v-menu/dienstleistungen/ntp/
				InetAddress hostAddr = InetAddress.getByName("ntps1-1.eecsit.tu-berlin.de");
				TimeInfo info;
				try {
					info = timeClient.getTime(hostAddr);
					returnTimeStamp = sendTimeMessage(info);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	return returnTimeStamp;
		
	}

    
}
