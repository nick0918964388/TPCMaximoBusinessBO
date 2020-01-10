/**
* System      : Asset Management System                                        
* Class name  : StringUtil.java                                                     
* Description : Method for String check            
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class StringUtil {

	public static final int LEFT = 0; 
	public static final int RIGHT = 1; 

	public static final boolean LEADINGZERO = true; 

    /** Creates a new instance of plainString */
    public StringUtil() {
    }

    public static String getDecimalDouble(double d, int scale) {
        BigDecimal bd = new BigDecimal(String.valueOf(d));
        //System.out.println("\t" + bd.setScale(scale, BigDecimal.ROUND_HALF_UP));
        bd = bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
        return bd.toString();
    }

    @SuppressWarnings("unchecked")
	public static String[] splitString(String data, String token) {
        if (data == null) {
            return null;
        }
        Vector vec = new Vector();
        while (data.indexOf(token) > -1) {
            if (data.startsWith(",")) {
                vec.add("");
            } else {
                String s0 = (String) data.substring(0, data.indexOf(token));
                vec.add(s0);
            }
            data = data.substring(data.indexOf(token) + 1);
        }
        vec.add(data);
        String[] list = new String[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            String s1 = (String) vec.get(i);
            list[i] = s1;
        }
        return list;
    }
    @SuppressWarnings("unchecked")
	public static Vector splitString2(String data, String token) {
        Vector list = new Vector();
        while (data.indexOf(token) > -1) {
            if (data.startsWith(",")) {
                list.add("");
            } else {
                String s0 = (String) data.substring(0, data.indexOf(token));
                list.add(s0);
            }
            data = data.substring(data.indexOf(token) + 1);
        }
        list.add(data);
        return list;
    }
    @SuppressWarnings("unchecked")
	public static String mergeVectorToString(Vector vec, String token){
        String data = "";
        if (vec.size()>0){
            data = (String)vec.get(0);
            for (int i=1;i<vec.size();i++){
                data = data + token + (String)vec.get(i);
            }
        }
        return data;
    }
    public static String formatDate(Date dd, String format) {
        String rtnValue = "";
        SimpleDateFormat sf = new SimpleDateFormat(format);
        sf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        rtnValue = sf.format(dd);
        return rtnValue;
    }
    public static String formatDigit(String sData, int digit) {
        while (sData.length() < digit) {
            sData = "0" + sData;
        }
        return sData;
    }
    @SuppressWarnings("unchecked")
	public static boolean checkExist(Vector vec, String s0) {
        boolean flag = false;
        for (int i = 0; i < vec.size(); i++) {
            String s1 = (String) vec.get(i);
            if (s1.equals(s0)) {
                flag = true;
                break;
            }
        }
        return flag;
    }
	public static String formatString(String s, int fixedNum, int alignment) {
		int num = (fixedNum < 1) ? 1 : fixedNum;
		String str = ((str = s) == null) ? "" : str.trim();
		str = str.length() > num ? str.substring(0, num) : str;
		StringBuilder format = new StringBuilder("%");
		if (alignment == LEFT)
			format.append("-");
		format.append(num).append("s");
		return String.format(new String(format), str);
	}
	public static String formatNumber(double d, int integer, int fraction, int alignment) {
		int len_num = String.valueOf((long)d).length();
		int inte = (integer < len_num)? len_num : integer; 
		int frac = (fraction < 0 )? 0 : fraction;
		StringBuilder format = new StringBuilder("%");
		if (alignment == LEFT)
			format.append("-");
		format.append(inte + frac + 1).append(".").append(frac).append("f");
		return String.format(new String(format), d);
	}

	public static String formatNumber(double d, int integer, int fraction, boolean leadingZero) {
		int len_num = String.valueOf((long)d).length();
		int inte = (integer < len_num)? len_num : integer; 
		int frac = (fraction < 0 )? 0 : fraction;
		StringBuilder format = new StringBuilder("%");
		if (leadingZero) 
			format.append('0');
		format.append(inte + (frac == 0 ? 0 : (frac + 1))).append(".").append(frac).append("f");
		return String.format(new String(format), d);
	}	

	public static boolean isEmpty(String s) {
		return (s == null || s.trim().length() == 0) ? true : false;
	}

	public static boolean isYesOrNo(String s) {
		
		if (isEmpty(s)){
			return false;
		}else if(s.equals("Y") || s.equals("1")){
			return true;
		}
		
		return false;
	}
	
	public static String reduceString(String s0, int digit){
		String value = "";
		value = s0.substring(0, s0.length()-digit);
		return value;
	}
	
	public static String emptyStr(String s0){
		if (isEmpty(s0)) {
			return "";
		} else {
			return s0;
		}
	}
	
	
	public static String decode(String url) {
		try {
			
			if (isEmpty(url)) {
				return "";
			}
			
			String prevURL = "";
			String decodeURL = url;
			while (!prevURL.equals(decodeURL)) {
				prevURL = decodeURL;
				decodeURL = URLDecoder.decode(decodeURL, "UTF-8");
			}
			return decodeURL;
		} catch (UnsupportedEncodingException e) {
			return "Error: " + e.getMessage();
		}
	}

	public static String encode(String url) {
		try {
			
			if (isEmpty(url)) {
				return "";
			}
			
			String encodeURL = URLEncoder.encode(url, "UTF-8");
			return encodeURL;
		} catch (UnsupportedEncodingException e) {
			return "Error: " + e.getMessage();
		}
	}
	
	/**
	 * String to unicode
	 */
	public static String string2Unicode(String str) {
	 
		if (isEmpty(str)) {
			return "";
		}
		
	    StringBuffer unicode = new StringBuffer();
	 
	    for (int i = 0; i < str.length(); i++) {
	 
	        char c = str.charAt(i);

	        unicode.append("\\u" + Integer.toHexString(c));
	    }
	 
	    return unicode.toString();
	}
	
	/**
	 * unicode to String
	 */
	public static String unicode2String(String unicode) {
	 
		if (isEmpty(unicode)) {
			return "";
		}
		
	    StringBuffer string = new StringBuffer();
	 
	    String[] hex = unicode.split("\\\\u");
	 
	    for (int i = 1; i < hex.length; i++) {
	 
	        int data = Integer.parseInt(hex[i], 16);
	        
	        string.append((char) data);
	    }
	 
	    return string.toString();
	}
	
	
	
	
}