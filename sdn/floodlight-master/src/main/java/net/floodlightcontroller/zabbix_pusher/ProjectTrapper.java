package net.floodlightcontroller.zabbix_pusher;




import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for manually pushing metric data into a 
 * zabbix server using zabbix_sender commandline prompts.
 * (created for Distributed Systems Project SoSe 2014 CIT TU Berlin)
 */
public class ProjectTrapper{
		
	String command ="";
	Process p = null;
	
	private String jsonMsg;
	private OutputStream zabbixMsg;

    public ProjectTrapper(){
        zabbixMsg = new ByteArrayOutputStream();
    }
	
	/**
	 * Method for pushing desired metric information into specified zabbix monitor
	 * 
	 * @param zabbixServerIpAdress IP adress or DNS name of the zabbix monitor.
	 * @param hostname Registered hostname within zabbix monitor (Warning: Not IP adress or DNS name). Also the name of the server to which the specified information should be assigned to.
	 * @param itemKey Name of the item key for the metric information.(Casesensitive)
	 * @param isKeyNumeric Boolean parameter for determining whether the numeric value is an integer or a character. (true = value is numeric, false = value is a character)
	 * @param Value Metric information that is going to be sent to the monitor.
	 */
//	public void sendMetric(String zabbixServerIpAdress, String hostname,
//			String itemKey,boolean isKeyNumeric, Object Value) {
//
//		//Constructing command for execution
//		String command = "zabbix_sender -z " + zabbixServerIpAdress  +
//				" -s " + hostname +
//				" -k " +itemKey;
//		if(isKeyNumeric){
//			command += " -o " + Value.toString();
//		}
//		else{
//			//TODO: Problem fixen mit dem Senden von Nachrichten mit Leerzeichen
//			command += " -o \'" + (String)Value + "\'";
//		}
//
//		System.out.println(command);
//
//		p = null;
//
//		//executing the commandline via Runtime
//		try{
//			p = Runtime.getRuntime().exec(command);
////			p.waitFor();
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
//
//		//resetting the command
//		command = "";
//
//	}
	
	public void sendMetricJson (String zabbixServerIpAdress, String hostname,
	String itemKey,boolean isKeyNumeric, String value) throws IOException {

	String zabbix_URL = "http://" + zabbixServerIpAdress + "/zabbix/api_jsonrpc.php";
    String auth = userAuthenticate(zabbix_URL);
    String keyUpdate = "\""+itemKey + "\":\"" + value + "\"";
    String type = "\"type\":" + "\"2\"";
    String keyType;
    if (isKeyNumeric){
       keyType = "\"value_type\":" + "\"0\"";
    }else{
        keyType = "\"value_type\":" + "\"4\"";
    }




        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(zabbix_URL);
        List<NameValuePair> valuePairList = new ArrayList<NameValuePair>();
        valuePairList.add(new BasicNameValuePair("jsonrpc","2.0"));
        valuePairList.add(new BasicNameValuePair("method","item.update"));
        valuePairList.add(new BasicNameValuePair("params", "[" + keyUpdate + "," + type + "," + keyType + "]"));
        valuePairList.add(new BasicNameValuePair("id","2"));
        httpPost.setEntity(new UrlEncodedFormEntity(valuePairList));
        CloseableHttpResponse response = httpclient.execute(httpPost);

        System.out.println(response.getStatusLine());
        response.close();
		// --- prepare JSON string
//		jsonMsg = "{"
//		        + "\"request\":\"sender data\",\n"
//		        + "\"data\":[\n"
//		        +        "{\n"
//		        +                "\"host\":\"" + hostname + "\",\n"
//		        +                "\"key\":\"" + itemKey + "\",\n"
//		        +                "\"value\":\"" + value.replace("\\", "\\\\") + "\"}]}\n" ;
//
//		System.out.println("\nJSON String output for debugging purposes:\n" +jsonMsg+ "\n");
//
//		// --- write JSON string to a zabbix message
//		byte[] zabbixData = jsonMsg.getBytes();
//		int lengthZabbixMsg = zabbixData.length;
//        System.err.println(zabbixData.length);
//        zabbixMsg.write(new byte[] {
//			'Z', 'B', 'X', 'D',
//			'\1',
//			(byte)(lengthZabbixMsg & 0xFF),
//			(byte)((lengthZabbixMsg >> 8) & 0x00FF),
//			(byte)((lengthZabbixMsg >> 16) & 0x0000FF),
//			(byte)((lengthZabbixMsg >> 24) & 0x000000FF),
//			'\0','\0','\0','\0'});
//
//		zabbixMsg.write(zabbixData);
	
	}

    private String userAuthenticate(String zabbix_url) throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(zabbix_url);
        List<NameValuePair> valuePairList = new ArrayList<NameValuePair>();
        valuePairList.add(new BasicNameValuePair("jsonrpc","2.0"));
        valuePairList.add(new BasicNameValuePair("method","user.login"));
        valuePairList.add(new BasicNameValuePair("params", "{\"user\": \"admin\", \"password\": \"zabbix\"}"));
        valuePairList.add(new BasicNameValuePair("id","1"));
        try {

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairList,"UTF-8");
            System.err.println(EntityUtils.toString(entity,"UTF-8"));
            httpPost.setEntity(entity);
            httpPost.addHeader("Content-Type","application/json");
            CloseableHttpResponse response = httpclient.execute(httpPost);
            byte[] respArr = new byte[(int)response.getEntity().getContentLength()];
            response.getEntity().getContent().read(respArr);
            String out = new String(respArr);
      //      System.err.println(out);

            response.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }



        return null;
    }

}
		

