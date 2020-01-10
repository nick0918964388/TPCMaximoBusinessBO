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
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class LogUtil {

	public static final int LEFT = 0; 
	public static final int RIGHT = 1; 

	public static final boolean LEADINGZERO = true; 
	public static BufferedWriter writer = null;
    /** Creates a new instance of plainString */
    public LogUtil(String logfullpath) {
    	try {
			writer = new BufferedWriter(new FileWriter(logfullpath, true));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }

    public void close() throws IOException {    	
    	writer.close();
    }

    public void log(String msg) {
    	if(writer!=null) {
    		try {
				writer.write(msg + "\r\n");
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }
	
	
	
	
}