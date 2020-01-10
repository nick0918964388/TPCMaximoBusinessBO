/*---------------------------  Modification history :---------------------------*/
/* Date        Version Author         Note                                      */
/* ----------  ------- -------------  ------------------------------------------*/
/* 2019-03-20   V1.0   Davis Wang     Initial Release                           */
/* 2019-04-24   V1.1   Davis Wang     Edit Update Rule                          */
/* 2019-06-06   V1.2   Davis Wang     Edit Update meter Rule                    */
package tw.com.taipower.app.crontask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import psdi.app.assetcatalog.SpecificationMboRemote;
import psdi.app.location.LocationSet;
import psdi.app.system.CrontaskParamInfo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValueData;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.server.SimpleCronTask;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import tw.com.taipower.app.asset.Asset;
import tw.com.taipower.app.asset.AssetSet;
import tw.com.taipower.app.util.DateUtil;
import tw.com.taipower.app.util.HttpUtil;
import tw.com.taipower.app.util.LogUtil;
import tw.com.taipower.app.util.StringUtil;
import tw.com.taipower.webclient.util.DataLenCheck;
import tw.com.taipower.webclient.util.ServerletUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.tivoli.maximo.srvad.app.ServiceAddressSet;

public class CustSTMHesCodeUpdateCrontask extends SimpleCronTask {
	static MXLogger						myLogger			= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");	
	public MXServer				mxServer;
	public UserInfo				ui;	
	private static CrontaskParamInfo[]	params;	
	public String				logFileName		= "STMHesCodeUpdatelog";	
	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	public LogUtil log = null;
	public DateUtil du = null;
	public HttpUtil httpU = null;
	public String esUrl = "";
	public String esPort = "";
	static {
		params = new CrontaskParamInfo[1];
		params[0] = new CrontaskParamInfo();
		params[0].setName("FILELOG");
		params[0].setDefault("/opt/maximofilelog/");		
	}

	@Override 
	public void cronAction() {		
		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			esUrl = mxServer.getProperty("mxe.tpc.esUrl");
			log = new LogUtil(getParamAsString("FILELOG")+this.logFileName+sdf.format(new Date())+".log");
			log.log((new Date())+"  ======start Update HesCode & MeterId======");
			log.log((new Date())+"  init ES Url =>" + esUrl);
			myLogger.debug((new Date())+"  ======start Update HesCode & MeterId======");
			myLogger.debug((new Date())+"  init ES Url =>" + esUrl);
			
			if(esUrl.isEmpty()) {
				log.log((new Date())+"  ES Url is empty , break ");
				
			}else {
				HashMap<String,String> hesCodeMap = getHesCodeMappingFromES();
				//execute mapping
				procMamHesCodeMapping(hesCodeMap);
			
				log.log((new Date())+"  ======end Update HesCode & MeterId======");
			}
			
		} catch (Exception e) {

		}finally{
			try {
				log.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private HashMap<String, String> getHesCodeMappingFromES() {
		log.log((new Date())+"  getHesCodeMappingFromES start ...");
		HashMap<String,String> hesCodeMap = new HashMap<String,String>(); 
		String afterKey = "notEmpty";
		du = new DateUtil();
		httpU = new HttpUtil();
		Date date = new Date();
		String yesterdayStart = sdf.format(du.calDate(date,-1)) + " 00:00:00";
		String yesterdayEnd = sdf.format(du.calDate(date,-1)) + " 23:59:59";
				
		log.log((new Date())+"  startDate==>"+yesterdayStart+", endDate==>"+yesterdayEnd);
		
		int count = 0 ;
		while(!afterKey.isEmpty()) {
			String httpGetReponseStr  = httpU.httpPost("http://"+esUrl+"/meterreadings/_search",generateESQuery(afterKey,yesterdayStart,yesterdayEnd),"application/json");
			if(httpGetReponseStr.isEmpty() || httpGetReponseStr == null) {
				log.log((new Date())+"  httpGetReponseStr is Empty ... break");
				break;
			}
			JsonObject res = new JsonParser().parse(httpGetReponseStr).getAsJsonObject();
			try {
				if (res.get("hits").getAsJsonObject().get("total").getAsInt() > 0
						&& res.get("aggregations").getAsJsonObject().get("__buckets").getAsJsonObject().get("buckets")
								.getAsJsonArray().size() > 0) {
					
					log.log((new Date())+"  total = "+res.get("hits").getAsJsonObject().get("total").getAsString());					
					int total = res.get("hits").getAsJsonObject().get("total").getAsInt();
					
					afterKey = res.get("aggregations").getAsJsonObject().get("__buckets").getAsJsonObject()
							.get("after_key").getAsJsonObject().get("mrId").getAsString();
					
					log.log((new Date())+"  afterKey = "+afterKey);
					
					JsonArray bucketsArr = res.get("aggregations").getAsJsonObject().get("__buckets").getAsJsonObject()
							.get("buckets").getAsJsonArray();
					
					for (JsonElement pa : bucketsArr) {
						String meterId = pa.getAsJsonObject().get("key").getAsJsonObject().get("mrId").getAsString();
						String hesCode = pa.getAsJsonObject().get("group_by").getAsJsonObject().get("buckets")
								.getAsJsonArray().get(0).getAsJsonObject().get("metadata").getAsJsonObject().get("hits")
								.getAsJsonObject().get("hits").getAsJsonArray().get(0).getAsJsonObject().get("_source")
								.getAsJsonObject().get("header").getAsJsonObject().get("source").getAsString();
						count++;
						hesCodeMap.put(meterId,hesCode);
					}
					log.log((new Date())+"  running Count = "+count);
				} else {
					afterKey = "";// complete
				}	
			}catch(Exception ex) {
				log.log((new Date())+"  exception = "+ex.getMessage());
			}
		}
		log.log((new Date())+"  getHesCodeMappingFromES end ...");
		
		return hesCodeMap;
	}
	public static String generateESQuery(String afterKey , String startDate , String endDate) {
		StringBuilder sb = new StringBuilder();
		sb.append(" {\"size\": 0,																			");
		sb.append("   \"_source\": \"false\",                                                             ");
		sb.append("   \"query\": {                                                                        ");
		sb.append("     \"bool\": {                                                                       ");
		sb.append("       \"should\": [                                                                   ");
		sb.append("         {                                                                             ");
		sb.append("           \"bool\": {                                                                 ");
		sb.append("             \"must\": [                                                               ");
		sb.append("               {                                                                       ");
		sb.append("                 \"range\": {                                                          ");
		sb.append("                   \"header.timeStamp\": {                                             ");
		sb.append("                     \"gte\": \""+startDate+"\",                                 	  ");
		sb.append("                     \"lte\": \""+endDate+"\",                                 		  ");
		sb.append("                     \"format\": \"yyyy-MM-dd HH:mm:ss\",                              ");
		sb.append("                     \"time_zone\":\"+08:00\"                                          ");
		sb.append("                   }                                                                   ");
		sb.append("                 }                                                                     ");
		sb.append("               }                                                                       ");
		sb.append("             ]                                                                         ");
		sb.append("           }                                                                           ");
		sb.append("         }                                                                             ");
		sb.append("         ]                                                                             ");
		sb.append("       }                                                                               ");
		sb.append("     },                                                                                ");
		sb.append("         \"aggs\": {                                                                   ");
		sb.append("           \"__buckets\":{                                                             ");
		sb.append("             \"composite\":{                                                           ");
		sb.append("               \"size\":10000,                                                          ");
		sb.append("               \"sources\":[{                                                          ");
		sb.append("                   \"mrId\":{\"terms\":{  \"field\":\"meterReadings.meter.mrId\"       ");
		sb.append("                     }}}                                                               ");
		sb.append("               ]                                                                       ");
		if(!afterKey.equalsIgnoreCase("notEmpty")) {
			sb.append(",\"after\": {\"mrId\": \""+afterKey+"\"}");			
		}
		sb.append("           },                                                                          ");
		sb.append("            \"aggs\":{                                                                 ");
		sb.append("                                                                                       ");
		sb.append("             \"group_by\":{                                                            ");
		sb.append("              \"terms\":{                                                              ");
		sb.append("               \"field\":\"meterReadings.meter.mrId\"                                  ");
		sb.append("             },                                                                        ");
		sb.append("             \"aggs\": {                                                               ");
		sb.append("                                                                                       ");
		sb.append("               \"metadata\": {                                                         ");
		sb.append("                 \"top_hits\": {                                                       ");
		sb.append("                 \"size\":1,                                                           ");
		sb.append("                   \"_source\": {                                                      ");
		sb.append("                     \"includes\": [                                                   ");
		sb.append("                        \"meterReadings.meter.mrId\",                                  ");
		sb.append("                        \"header.source\"                                              ");
		sb.append("                     ]                                                                 ");				
		sb.append("                   },\"sort\": [{                                                      ");
		sb.append("                     \"meterReadings.intervalBlocks.intervalReadings.timeStamp\":      ");
		sb.append("                     {                                                                 ");
		sb.append("                         \"order\": \"asc\"                                            ");
		sb.append("                     }                                                                 ");
		sb.append("                   }]                                                                  ");
		sb.append("                 }                                                                     ");
		sb.append("               }                                                                       ");
		sb.append("             }                                                                         ");
		sb.append("           }                                                                           ");
		sb.append("         }                                                                             ");
		sb.append("       }                                                                               ");
		sb.append("     }                                                                                 ");
		sb.append("   }                                                                                   ");		
		return sb.toString();
	}

	private void procMamHesCodeMapping(HashMap<String,String> hesCodeMap) throws RemoteException, MXException {
		log.log((new Date())+"  procMamHesCodeMapping start ...");
		MboSetRemote hesCodeSet = mxServer.getMboSet("ZZ_HES_IMP_DATA", ui);
		int totalHesCodeSetCnt = hesCodeSet.count();
		log.log((new Date())+"  get ZZ_HES_IMP_DATA count ==>" + totalHesCodeSetCnt);
		int changeCnt = 0 ;
		int newCnt = 0;
		int index = 0 ;
		MboRemote hesCode = null;
		List<HashMap<String,String>> toAddLst = new ArrayList<HashMap<String,String>>();
		for(String meterId : hesCodeMap.keySet()) {
			myLogger.debug("index==>"+index+", meterId==>"+meterId);
			hesCodeSet.setWhere("COMP_METER_NUM='"+meterId+"'");
			hesCodeSet.reset();
			if(!hesCodeSet.isEmpty()) {
				//update
				hesCode = hesCodeSet.getMbo(0);	
				if(!hesCode.getString("HES_CODE").equalsIgnoreCase(hesCodeMap.get(meterId))) {
					hesCode.setValue("HES_CODE", hesCodeMap.get(meterId) , 11L);
					log.log((new Date())+" [meterId="+meterId+"][CHG]  HES_CODE Change from "+hesCode.getString("HES_CODE")+" to  ==>" + hesCodeMap.get(meterId));
					changeCnt++;
				}
			}else {
				//new
				//new				
				HashMap<String,String> toAddMap = new HashMap<String,String>();
				getAssetRef(meterId,toAddMap);
				if(toAddMap.size()>0) { //maybe not in asset 
					toAddMap.put("HESCODE",hesCodeMap.get(meterId));
					toAddLst.add(toAddMap);
					log.log((new Date())+" [meterId="+meterId+"][NEW]  NEW HES_CODE ==>" + hesCodeMap.get(meterId));
					newCnt++;
				}else {
					log.log((new Date())+" [meterId="+meterId+"][MISS]  not in Asset  ==>" + hesCodeMap.get(meterId));
				}
			}
			index++;
		}
		
		//iterator new add HES Code
		
		for(HashMap<String,String> toAddMap : toAddLst) {
			MboRemote newHesMbo = hesCodeSet.addAtEnd();
			newHesMbo.setValue("COMP_METER_NUM", toAddMap.get("METERID"));
			newHesMbo.setValue("SITEID", toAddMap.get("SITEID"));
			newHesMbo.setValue("ELECT_NUM", toAddMap.get("CUSTNO"));
			newHesMbo.setValue("HES_CODE", toAddMap.get("HESCODE"));
		}
		log.log((new Date())+" now updateCnt ="+changeCnt+" , newCnt ="+newCnt);		
		log.log((new Date())+" start to save HES_CODE MBO");
		if(hesCodeSet.toBeSaved()) {
			hesCodeSet.save();
			log.log((new Date())+" SAVE done...");
		}
		log.log((new Date())+"  procMamHesCodeMapping end ...");
	}

	private void getAssetRef(String meterId, HashMap<String, String> toAddMap) throws RemoteException, MXException {
		MboSetRemote assetSet = mxServer.getMboSet("ASSET",ui);
		assetSet.setWhere("assetnum='"+meterId+"'");
		assetSet.reset();
		if(!assetSet.isEmpty()) {
			MboRemote assetMeterMbo = assetSet.getMbo(0);
			toAddMap.put("METERID",meterId);
			toAddMap.put("SITEID",assetMeterMbo.getString("SITEID"));
			toAddMap.put("CUSTNO",assetMeterMbo.getString("ZZ_CUSTNO"));						
		}
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}
	
	

}
