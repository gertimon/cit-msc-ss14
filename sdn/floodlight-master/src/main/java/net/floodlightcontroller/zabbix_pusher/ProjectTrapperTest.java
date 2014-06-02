package net.floodlightcontroller.zabbix_pusher;

import java.util.Random;

/**
 * Class with a few tests and examples on pushing data into zabbix with zabbix_sender
 */

public class ProjectTrapperTest {


	public static void main(String[] args) {
		ProjectTrapper pt = new ProjectTrapper();
		
		//test for sending Strings
//		pt.sendMetric("192.168.2.109", "Zabbix_client_agent", "trapper.test", false, "Viele tolle neue Nachrichten");
		
		//test for sending numeric data
//		pt.sendMetric("192.168.2.109", "Zabbix_client_agent", "my.trapper.test.numeric", true, 100);
		
		//test for sending multiple Random numeric data with an interval
//		Random rm = new Random();
//		Integer rmInt = null;
//		for(int i = 0; i < 20; i++){
//			rmInt = rm.nextInt(300);
//			//System.out.println("Gesendete Nummer: \t" + rmInt);
//			pt.sendMetric("192.168.2.109", "Zabbix_client_agent", "my.trapper.test.numeric", true, rmInt);
//			try {
//				Thread.sleep(10000);   //this is the sending intervall (in milliseconds)
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
		//test for Voltage Trapper
//		VoltageTrapper vt = new VoltageTrapper("192.168.2.109", "Zabbix_client_agent", "my.trapper.test.numeric");
//		vt.start();
//		try {
//			Thread.sleep(600000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		vt.haltExecution();	
//		try {
//			Thread.sleep(45000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		//Funktioniert ÜBERHAUPT nicht. Zabbix Trapper zu doof. (my.param.key[*])
//		pt.sendMetric("192.168.2.109", "Zabbix_client_agent", "my.param.key[alex]" , true, 10);
//		pt.sendMetric("192.168.2.109", "Zabbix_client_agent", "my.param.key[horst]" , true, 120);
//		pt.sendMetric("192.168.2.109", "Zabbix_client_agent", "my.param.key[klaus]" , true, 80);
	}

}
