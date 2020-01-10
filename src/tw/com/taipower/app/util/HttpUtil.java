/**
* System      : Asset Management System                                        
* Class name  : StringUtil.java                                                     
* Description : Method for String check            
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2020-01-08   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class HttpUtil {

	public static int READTIMEOUT = 100000; //ms
	public static int CONNECTTIMEOUT = 100000; //ms
    public HttpUtil() {
    	
    	
    }

    public static String httpPost(String url, String body, String ContentType) {
	    String bs = null;
	    try {
	        URL uri = new URL(url);
	        HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
//	        conn.setReadTimeout(READTIMEOUT);
//	        conn.setConnectTimeout(CONNECTTIMEOUT);
	        conn.setUseCaches(false);
	        conn.setRequestMethod("POST");
	        conn.setRequestProperty("Content-Type", ContentType);

	        DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
	        outStream.write(body.toString().getBytes());

	        outStream.flush();

	        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            InputStream in = conn.getInputStream();
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            byte[] buffer = new byte[256];
	            int n;
	            while ((n = in.read(buffer)) >= 0) {
	                out.write(buffer, 0, n);
	            }
	            byte[] b = out.toByteArray();
	            in.close();
	            bs = new String(b);
	        }else {
	        	return "";
	        }
	        outStream.close();
	        conn.disconnect();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return bs;
	}

    
	
}