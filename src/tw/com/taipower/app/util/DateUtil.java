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
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class DateUtil {

    /** Creates a new instance of plainString */
    public DateUtil() {
    	
    	
    }

    public static Date calDate(Date dateFrom ,int diff) {
		Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE, diff);
	    return cal.getTime();
	}

    
	
}