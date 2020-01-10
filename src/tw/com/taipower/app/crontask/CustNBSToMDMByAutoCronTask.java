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
import java.io.FileInputStream;
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
import java.util.Hashtable;
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
import tw.com.taipower.app.util.StringUtil;
import tw.com.taipower.webclient.util.DataLenCheck;
import tw.com.taipower.webclient.util.ServerletUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.tivoli.maximo.srvad.app.ServiceAddressSet;

public class CustNBSToMDMByAutoCronTask extends SimpleCronTask {
	MXLogger							myLogger			= MXLoggerFactory
																	.getLogger("maximo.sql.LOCATION.LOCATIONS");

	// use for data post
	static String[]						ZZ_Tmp_NBS_Post		= { "custno", "customername", "backusuallycontract", "backnonesummercontract",
															"backsaturdayhalfpeakcontract", "backoffpeakcontract", "usuallycontract", "nonesummercontract",
															"offpeakcontract", "saturdayhalfpeakcontract", "contracttype", "meterBatch", "updatedate", "updatetype",
															"meterId", "zz_group", "meterMultiple", "taxid", "industclass", "section", "billingDate" };

	public String[]						EXCEL_COL_NAME_NBS	= { // get json data from soa
															"custNo", "customerName", "customerAddress", "backUsuallyContract",
															"backNoneSummerContract", "backsaturDayHalfPeakContract",
															"backOffPeakContract", "usuallyContract", "noneSummerContract",
															"offPeakContract", "saturdayHalfPeakContract", "contractType",
															"meterBatch", "updateDate", "updateType", "meterId", "group",
															"meterMultiple", "taxId", "industClass", "section",
															"capacityStrengh", "capacityHeat", "capacityLight", "transdt",
															"cycleCd", "dayCd", "cityCd", "feeder", "custCd", "outageGroup",
															"subMeterId", "publicClassId", "meterType" };
	int									dostartindex;
	int									doendindex;
	public static MXServer				mxServer;
	public static UserInfo				ui;
	public static String				Location_Insert;
	public static MboSetRemote			AMI_Insert			= null;
	private static CrontaskParamInfo[]	params;
	public static String				Service_Address_Insert;
	public static MboSetRemote			assetToNbsMbo		= null;
	static {
		params = new CrontaskParamInfo[5];
		params[0] = new CrontaskParamInfo();
		params[0].setName("WHERE");
		params[1] = new CrontaskParamInfo();
		params[1].setName("LOADFILE");
		params[1].setDefault("");
		params[2] = new CrontaskParamInfo();
		params[2].setName("StartIndex");
		params[2].setDefault("0");
		params[3] = new CrontaskParamInfo();
		params[3].setName("EndIndex");
		params[3].setDefault("270000");
		params[4] = new CrontaskParamInfo();
		params[4].setName("logPath");
	}

	public Hashtable<String, Hashtable<String, String>> ReadMeterDataCSV(String FilePath) throws RemoteException, MXException, Exception {
		Hashtable<String, Hashtable<String, String>> ReadData = new Hashtable<String, Hashtable<String, String>>();

		myLogger.debug("Start Read File!!");
		String tempuse = null;
		// LOG_PATH = FilePath.replace(".csv", "_log.csv");

		int count = 0;
		try {
			File file = new File(FilePath);
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8");
			BufferedReader reader = new BufferedReader(isr);
			String line = null;
			Hashtable<String, String> cellValue = new Hashtable<String, String>();
			String[] data_split = null;
			while ((line = reader.readLine()) != null) {
				if (count > 0) {
					data_split = line.split(",");
					cellValue = new Hashtable<String, String>();
					cellValue.clear();
					for (int j = 0; j < EXCEL_COL_NAME_NBS.length; j++) {
						if (EXCEL_COL_NAME_NBS[j].equalsIgnoreCase("meterid")) {
							cellValue.put(EXCEL_COL_NAME_NBS[j], data_split[33] + data_split[15].substring(1, data_split[15].length()));
						} else
							cellValue.put(EXCEL_COL_NAME_NBS[j], data_split[j]);
					}
					ReadData.put(cellValue.get("meterId"), cellValue);
				}
				count = count + 1;
			}
			reader.close();

		} catch (Exception e) {
			myLogger.debug("Cron task Upload Load NBS Meter Data Err!!" + e.getMessage());
		}
		myLogger.debug("File Count: " + count);
		return ReadData;
	}

	@Override public void cronAction() {
		// Your custom code here. e.g. A sample code is below.

		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			// System.out.println("MyFirstCrontaskRTING CRONTASK");
			myLogger.debug("Cron task MAM Update NBS Data to SOA");
			String LoadFilePath = getParamAsString("LOADFILE");
			dostartindex = Integer.parseInt(getParamAsString("StartIndex"));
			doendindex = Integer.parseInt(getParamAsString("EndIndex"));

			Boolean isExeMDMS = true;

			Hashtable<String, Hashtable<String, String>> GetData = new Hashtable<String, Hashtable<String, String>>();
			GetData = ReadMeterDataCSV(LoadFilePath);

			SaveNBSData("http://127.0.0.1:1880/NBS_TST", isExeMDMS, GetData);
			// End Sample Code
		} catch (Exception e) {

		}
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	protected boolean crateXml_multi(String PostDes, Hashtable<String, Hashtable<String, String>> Use_XML_Data, Hashtable<String, Integer> type_cnt, int typeIndex)
			throws ServletException, IOException, MXException {
		String xmlFilePath = "C:\\xmlfile_0924.xml";
		// String xmlFilePath = "/opt/inst/nbstomdms.xml";
		String tmp_str = "";
		StringBuilder xmlClause = new StringBuilder();
		try {

			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

			Document document = documentBuilder.newDocument();

			// root element 123
			Element root = document.createElement("SDPSyncMessage");
			document.appendChild(root);

			Attr attr = document.createAttribute("xsi:schemaLocation");
			attr.setValue("http://www.emeter.com/energyip/syncinterface/v8 UniversalSyncInterface.xsd ");

			root.setAttributeNode(attr);

			attr = document.createAttribute("xmlns");
			attr.setValue("http://www.emeter.com/energyip/syncinterface/v8 UniversalSyncInterface.xsd ");
			root.setAttributeNode(attr);

			attr = document.createAttribute("xmlns:xsi");
			attr.setValue("http://www.w3.org/2001/XMLSchema-instance");
			root.setAttributeNode(attr);
			// ---------Set Header--------------------------------

			// employee element

			Element employee = document.createElement("header");
			root.appendChild(employee);

			// firstname element
			Element headerTemp = document.createElement("verb");
			headerTemp.appendChild(document.createTextNode("SDPSync"));
			employee.appendChild(headerTemp);

			headerTemp = document.createElement("noun");
			headerTemp.appendChild(document.createTextNode("SDPSync"));
			employee.appendChild(headerTemp);

			headerTemp = document.createElement("revision");
			headerTemp.appendChild(document.createTextNode("1"));
			employee.appendChild(headerTemp);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String Head_time = sdf.format(new Date());

			headerTemp = document.createElement("dateTime");
			headerTemp.appendChild(document.createTextNode(Head_time));
			employee.appendChild(headerTemp);

			headerTemp = document.createElement("source");
			headerTemp.appendChild(document.createTextNode("NBS")); // source can be NBS/RNBS
			employee.appendChild(headerTemp);

			String header__uuid = UUID.randomUUID().toString();
			headerTemp = document.createElement("messageID");
			headerTemp.appendChild(document.createTextNode(header__uuid));
			employee.appendChild(headerTemp);

			headerTemp = document.createElement("asyncReplyTo");
			headerTemp.appendChild(document.createTextNode("none"));
			employee.appendChild(headerTemp);

			headerTemp = document.createElement("syncMode");
			headerTemp.appendChild(document.createTextNode("sync"));
			employee.appendChild(headerTemp);

			headerTemp = document.createElement("optimizationLevel");
			headerTemp.appendChild(document.createTextNode("Full")); // Optmistic for update, Full for new insert
			employee.appendChild(headerTemp);
			// -------------------------------------------------------------------
			MboSetRemote Tmp_ZZ_NBS = null;
			MboSetRemote Tmp_ZZ_ASSETTONBS = null;
			Element payload = document.createElement("payload");
			root.appendChild(payload);
			mxServer = MXServer.getMXServer();

			// Account INFORMATION -------------create consumer --------------------
			String Tmp = "";
			ArrayList<String> Arr_Consumer = new ArrayList<String>();
			for (int i = 0; i < type_cnt.get("consumer"); i++) {
				if (Arr_Consumer.indexOf(Use_XML_Data.get("consumer").get("custNo" + (i + 1))) < 0) {
					Arr_Consumer.add(Use_XML_Data.get("consumer").get("custNo" + (i + 1)));
				} else {
					continue;
				}

				Element tag_counsumer = document.createElement("account");
				Element tag_consumer_tmp = document.createElement("mRID");
				Tmp = Use_XML_Data.get("consumer").get("custNo" + (i + 1));
				tag_consumer_tmp.appendChild(document.createTextNode(Use_XML_Data.get("consumer").get("custNo" + (i + 1))));
				// tag_consumer_tmp.appendChild(document.createTextNode(currMbo.getString("ZZ_CUSTNO")));
				tag_counsumer.appendChild(tag_consumer_tmp);

				tag_consumer_tmp = document.createElement("name");
				// tag_consumer_tmp.appendChild(document.createTextNode(currMbo.getString("zz_customername")));//remove customername to nbsttto asset
				// tag_consumer_tmp.appendChild(document.createTextNode("Davis"));

				tag_consumer_tmp.appendChild(document.createTextNode(Use_XML_Data.get("consumer").get("customername" + (i + 1))));
				tag_counsumer.appendChild(tag_consumer_tmp);

				tag_consumer_tmp = document.createElement("accountType");
				tag_consumer_tmp.appendChild(document.createTextNode("Default")); // (N)fixed
				tag_counsumer.appendChild(tag_consumer_tmp);

				tag_consumer_tmp = document.createElement("status");
				tag_consumer_tmp.appendChild(document.createTextNode("Active")); // (N)fixed
				tag_counsumer.appendChild(tag_consumer_tmp);

				payload.appendChild(tag_counsumer);
			}
			// PREMISE INFORMATION----------------------------------------------------------------------------------
			for (int i = 0; i < type_cnt.get("servicelocation"); i++) {
				Element tag_serviceLocation = document.createElement("serviceLocation");

				Element tag_serviceLocation_tmp = document.createElement("mRID");
				// tag_serviceLocation_tmp.appendChild(document.createTextNode(currMbo.getString("SADDRESSCODE")));
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("PRMS-");
				xmlClause.append(Use_XML_Data.get("servicelocation").get("prms" + (i + 1)));

				tag_serviceLocation_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("addressLine1");
				// tag_serviceLocation_tmp.appendChild(document.createTextNode(currMbo.getString("zz_customeraddress")));
				// tag_serviceLocation_tmp.appendChild(document.createTextNode("Davis address"));
				tag_serviceLocation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicelocation").get("customeraddress" + (i + 1))));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("latitude");
				// tag_serviceLocation_tmp.appendChild(document.createTextNode(currMbo.getString("zz_lat")));
				tag_serviceLocation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicelocation").get("latitude" + (i + 1))));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("longitude");
				// tag_serviceLocation_tmp.appendChild(document.createTextNode(currMbo.getString("zz_lon")));
				tag_serviceLocation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicelocation").get("longitude" + (i + 1))));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("stateOrProvince");
				tag_serviceLocation_tmp.appendChild(document.createTextNode("中華民國"));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("timeZone");
				tag_serviceLocation_tmp.appendChild(document.createTextNode("Aisa/Taipei"));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("country");
				tag_serviceLocation_tmp.appendChild(document.createTextNode("ROC"));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("postalCode");
				tag_serviceLocation_tmp.appendChild(document.createTextNode("2677"));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				tag_serviceLocation_tmp = document.createElement("status");
				tag_serviceLocation_tmp.appendChild(document.createTextNode("Active"));
				tag_serviceLocation.appendChild(tag_serviceLocation_tmp);

				payload.appendChild(tag_serviceLocation);
			}
			// ----------------service point---------------------------------------------------------

			for (int i = 0; i < type_cnt.get("servicepoint"); i++) {
				Element tag_servicePoint = document.createElement("servicePoint");

				Element tag_servicePoint_tmp = document.createElement("mRID");
				// tag_servicePoint_tmp.appendChild(document.createTextNode(currMbo.getString("LOCATION")));

				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("SDP-");
				xmlClause.append(Use_XML_Data.get("servicepoint").get("location" + (i + 1)));
				tag_servicePoint_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("universalId");
				// tag_servicePoint_tmp.appendChild(document.createTextNode(currMbo.getString("zz_uuid")));
				// Tmp = Use_XML_Data.get("servicepoint").get("uuid" + (i + 1));
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("N-");

				xmlClause.append(Use_XML_Data.get("servicepoint").get("contracttype" + (i + 1)));
				xmlClause.append("-");
				xmlClause.append(Use_XML_Data.get("servicepoint").get("section" + (i + 1)));
				xmlClause.append("-");
				xmlClause.append(Use_XML_Data.get("servicepoint").get("cyclecd" + (i + 1)));
				xmlClause.append("-");
				xmlClause.append(Use_XML_Data.get("servicepoint").get("location" + (i + 1)));

				// tag_servicePoint_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("uuid" + (i + 1))));
				tag_servicePoint_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("className");
				tag_servicePoint_tmp.appendChild(document.createTextNode("Electric"));
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("type");
				tag_servicePoint_tmp.appendChild(document.createTextNode("ServiceDeliveryPoint"));
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("status");
				tag_servicePoint_tmp.appendChild(document.createTextNode("Active"));
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("premiseId");
				Element servicePoint_sub_tmp = document.createElement("mRID");
				// servicePoint_sub_tmp.appendChild(document.createTextNode(currMbo.getString("SADDRESSCODE")));
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("PRMS-");
				xmlClause.append(Use_XML_Data.get("servicepoint").get("premisid" + (i + 1)));
				servicePoint_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				// servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("premisid" + (i + 1))));
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("latitude");
				// tag_servicePoint_tmp.appendChild(document.createTextNode(currMbo.getString("zz_lat")));
				tag_servicePoint_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("latitude" + (i + 1))));
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("longitude");
				// tag_servicePoint_tmp.appendChild(document.createTextNode(currMbo.getString("zz_lon")));
				tag_servicePoint_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("longitude" + (i + 1))));

				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("feedLocation");
				tag_servicePoint_tmp.appendChild(document.createTextNode("underground")); // NBS.Installation_type - pobbible values:Overhead/Underground.
				tag_servicePoint.appendChild(tag_servicePoint_tmp); // This is optional but good to have if information available.

				tag_servicePoint_tmp = document.createElement("timezone");
				tag_servicePoint_tmp.appendChild(document.createTextNode("Asia/Taipei"));
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				tag_servicePoint_tmp = document.createElement("gisId");
				// gisId
				// tag_servicePoint_tmp.appendChild(document.createTextNode("00023736928")); // Maximo.taipower_Grid
				tag_servicePoint_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("gisId" + (i + 1)))); // Maximo.taipower_Grid
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				// -----contractType parameter--------------------------//
				tag_servicePoint_tmp = document.createElement("parameter");
				servicePoint_sub_tmp = document.createElement("name");
				servicePoint_sub_tmp.appendChild(document.createTextNode("contractType"));
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("value");
				// servicePoint_sub_tmp.appendChild(document.createTextNode(currMbo.getString("zz_contract_kind")));
				servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("contracttype" + (i + 1))));
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("startDate");
				servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				tag_servicePoint.appendChild(tag_servicePoint_tmp);
				// -----Section parameter--------------------------//
				tag_servicePoint_tmp = document.createElement("parameter");
				servicePoint_sub_tmp = document.createElement("name");
				servicePoint_sub_tmp.appendChild(document.createTextNode("Section"));
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("value");
				servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("section" + (i + 1))));
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("startDate");
				servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				tag_servicePoint.appendChild(tag_servicePoint_tmp);
				// -----Region parameter--------------------------//
				tag_servicePoint_tmp = document.createElement("parameter");
				servicePoint_sub_tmp = document.createElement("name");
				servicePoint_sub_tmp.appendChild(document.createTextNode("Region")); // --N for north, C for central, S for south--
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("value");
				servicePoint_sub_tmp.appendChild(document.createTextNode("N"));
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("startDate");
				servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				tag_servicePoint.appendChild(tag_servicePoint_tmp);
				// -----publishType parameter--------------------------//
				tag_servicePoint_tmp = document.createElement("parameter");
				servicePoint_sub_tmp = document.createElement("name");
				servicePoint_sub_tmp.appendChild(document.createTextNode("publishType")); // --N for north, C for central, S for south--
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("value");
				servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("publishType" + (i + 1))));
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				servicePoint_sub_tmp = document.createElement("startDate");
				servicePoint_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("servicepoint").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_servicePoint_tmp.appendChild(servicePoint_sub_tmp);
				tag_servicePoint.appendChild(tag_servicePoint_tmp);

				payload.appendChild(tag_servicePoint);
			}
			// --------------servicePointStatus-----------------
			Element tag_servicePointStatus = document.createElement("servicePointStatus");

			Element tag_servicePointStatust_tmp = document.createElement("billedUptoDate");
			tag_servicePointStatust_tmp.appendChild(document.createTextNode(""));
			tag_servicePointStatus.appendChild(tag_servicePointStatust_tmp);

			tag_servicePointStatust_tmp = document.createElement("powerStatus");
			tag_servicePointStatust_tmp.appendChild(document.createTextNode("Y"));
			tag_servicePointStatus.appendChild(tag_servicePointStatust_tmp);

			tag_servicePointStatust_tmp = document.createElement("loadStatus");
			tag_servicePointStatust_tmp.appendChild(document.createTextNode("N"));
			tag_servicePointStatus.appendChild(tag_servicePointStatust_tmp);

			payload.appendChild(tag_servicePointStatus);

			// --------------device---------------------------------------

			for (int i = 0; i < type_cnt.get("device"); i++) {
				Element tag_device = document.createElement("device");

				Element tag_device_tmp = document.createElement("mRID");

				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("meterid" + (i + 1))));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("badgeId");
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("meterid" + (i + 1))));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("electronicId");
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("0");
				xmlClause.append(Use_XML_Data.get("device").get("meterid" + (i + 1)).substring(2, 9));
				tag_device_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("className");
				tag_device_tmp.appendChild(document.createTextNode("Electric"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("type");
				tag_device_tmp.appendChild(document.createTextNode("Meter"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("virtualInd");
				tag_device_tmp.appendChild(document.createTextNode("N"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("model");
				tag_device_tmp.appendChild(document.createTextNode("UAA EIP"));// -Concatonate(Maximo.Modelnum, Maximo.catelog)
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("manufacturedDate");
				// String manuDate = sdf.format(currMbo.getDate("statusdate"));
				// tag_device_tmp.appendChild(document.createTextNode(manuDate));
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("manufacturedDate" + (i + 1))));

				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("mfgSerialNumber");
				// tag_device_tmp.appendChild(document.createTextNode(currMbo.getString("serialnum")));
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("serialnum" + (i + 1))));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("status");
				// tag_device_tmp.appendChild(document.createTextNode(currMbo.getString("status")));
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("status" + (i + 1))));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("commTechnology");
				tag_device_tmp.appendChild(document.createTextNode("P6"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("make");
				// tag_device_tmp.appendChild(document.createTextNode(currMbo.getString("assetnum").substring(0, 2)));
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("make" + (i + 1))));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("location");
				// tag_device_tmp.appendChild(document.createTextNode(currMbo.getString("zz_custno").substring(0, 2)));
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("city" + (i + 1))));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("deviceFunctionType");
				tag_device_tmp.appendChild(document.createTextNode("N"));
				tag_device.appendChild(tag_device_tmp);

				// -----------deviceMultiplier--------------------
				tag_device_tmp = document.createElement("deviceMultiplier");
				Element tag_device_sub_tmp = document.createElement("multiplierType");
				tag_device_sub_tmp.appendChild(document.createTextNode("SC")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("multiplierValue");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("multiplierValue" + (i + 1))));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("effectiveStartDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);

				// ---------------Dials----------------------------------------------
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("Dials")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				tag_device_sub_tmp.appendChild(document.createTextNode("9"));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// ---------------ProgrameId----------------------------------------------
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("ProgramId")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("programId" + (i + 1))));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// --------------DemandUser-----
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("DemandUser")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				//tag_device_sub_tmp.appendChild(document.createTextNode("TRUE"));
				tag_device_sub_tmp.appendChild(document.createTextNode(""));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// --------------WithPeactive-----
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("WithReactive"));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				//tag_device_sub_tmp.appendChild(document.createTextNode("TRUE"));
				tag_device_sub_tmp.appendChild(document.createTextNode(""));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// --------------VotageType-----
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("VotageType"));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("voltageType" + (i + 1))));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// --------------Phase-----
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("phase")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("phase" + (i + 1))));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// --------------Direction-----
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("Direction")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("direction" + (i + 1))));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// --------------HESNAME-----
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("HESName")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("hesname" + (i + 1))));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);
				// --------------usuallyContract-----
				tag_device_tmp = document.createElement("parameter");
				tag_device_sub_tmp = document.createElement("name");
				tag_device_sub_tmp.appendChild(document.createTextNode("usuallyContract")); // --N for north, C for central, S for south--
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("value");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("usuallyContract" + (i + 1))));
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device_sub_tmp = document.createElement("startDate");
				tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
				tag_device_tmp.appendChild(tag_device_sub_tmp);
				tag_device.appendChild(tag_device_tmp);

				payload.appendChild(tag_device);

				// -----------------COMMUNICATION MODULE INFORMATION---------------------------
				tag_device = document.createElement("device");

				tag_device_tmp = document.createElement("mRID");

				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());

				xmlClause.append("LTEFAN-");
				xmlClause.append(Use_XML_Data.get("device").get("meterid" + (i + 1)));
				tag_device_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("className");
				tag_device_tmp.appendChild(document.createTextNode("LTEFAN"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("type");
				tag_device_tmp.appendChild(document.createTextNode("CommModule"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("virtualInd");
				tag_device_tmp.appendChild(document.createTextNode("N"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("manufacturedDate");
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("fanManufactureDate" + (i + 1))));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("mfgSerialNumber");
				tag_device_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("fanSerialNum" + (i + 1))));// -Concatonate(Maximo.Modelnum, Maximo.catelog)
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("status");
				tag_device_tmp.appendChild(document.createTextNode("Attach To Meter"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("commTechnology");
				tag_device_tmp.appendChild(document.createTextNode("P6"));
				tag_device.appendChild(tag_device_tmp);

				tag_device_tmp = document.createElement("deviceFunctionType");
				tag_device_tmp.appendChild(document.createTextNode("Y"));
				tag_device.appendChild(tag_device_tmp);
				payload.appendChild(tag_device);

				// -----------------CTPT INFORMATION---------------------------
				if (typeIndex == 0) {
					tag_device = document.createElement("device");

					tag_device_tmp = document.createElement("mRID");

					if (xmlClause.length() > 0)
						xmlClause.delete(0, xmlClause.length());

					xmlClause.append("CTPT-");
					xmlClause.append(Use_XML_Data.get("device").get("meterUuid" + (i + 1)));
					tag_device_tmp.appendChild(document.createTextNode(xmlClause.toString()));
					tag_device.appendChild(tag_device_tmp);

					tag_device_tmp = document.createElement("className");
					tag_device_tmp.appendChild(document.createTextNode("Generic Current/Potential Transformer (CT/PT)"));
					tag_device.appendChild(tag_device_tmp);

					tag_device_tmp = document.createElement("type");
					tag_device_tmp.appendChild(document.createTextNode("CTPT"));
					tag_device.appendChild(tag_device_tmp);

					tag_device_tmp = document.createElement("description");
					tag_device_tmp.appendChild(document.createTextNode("CTPT Description"));
					tag_device.appendChild(tag_device_tmp);

					tag_device_tmp = document.createElement("status");
					tag_device_tmp.appendChild(document.createTextNode("Active"));
					tag_device.appendChild(tag_device_tmp);

					tag_device_tmp = document.createElement("virtualInd");
					tag_device_tmp.appendChild(document.createTextNode("N"));
					tag_device.appendChild(tag_device_tmp);

					tag_device_tmp = document.createElement("deviceFunctionType");
					tag_device_tmp.appendChild(document.createTextNode("N"));
					tag_device.appendChild(tag_device_tmp);

					// -----------deviceMultiplier--------------------
					tag_device_tmp = document.createElement("deviceMultiplier");
					tag_device_sub_tmp = document.createElement("multiplierType");
					tag_device_sub_tmp.appendChild(document.createTextNode("CT")); // --N for north, C for central, S for south--
					tag_device_tmp.appendChild(tag_device_sub_tmp);
					tag_device_sub_tmp = document.createElement("multiplierValue");
					tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("ctRatio" + (i + 1))));
					tag_device_tmp.appendChild(tag_device_sub_tmp);
					tag_device_sub_tmp = document.createElement("effectiveStartDate");
					tag_device_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("device").get("startdate" + (i + 1)))); // (N)start date of meter
					tag_device_tmp.appendChild(tag_device_sub_tmp);
					tag_device.appendChild(tag_device_tmp);
				}
				payload.appendChild(tag_device);

			}

			// -------------------------servicePointDeviceAssociation---------------------------------------------------------------
			for (int i = 0; i < type_cnt.get("sdp_device"); i++) {
				// ----SERVICE POINT METER ASSOCIATION
				Element servicePointDeviceAssociation = document.createElement("servicePointDeviceAssociation");

				Element tag_servicePointDeviceAssociation_tmp = document.createElement("relType");
				tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode("SDP-METER"));
				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				tag_servicePointDeviceAssociation_tmp = document.createElement("startDate");

				// tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode(currMbo.getString("statusdate")));
				tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_device").get("startdate" + (i + 1))));
				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				// tag_servicePointDeviceAssociation_tmp = document.createElement("endDate");
				// tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_device").get("startdate" + (i + 1))));
				// servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				tag_servicePointDeviceAssociation_tmp = document.createElement("servicePointId");

				Element servicePointDeviceAssociation_sub_tmp = document.createElement("mRID");
				// servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("SADDRESSCODE")));
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());

				xmlClause.append("SDP-");
				xmlClause.append(Use_XML_Data.get("sdp_device").get("sdp" + (i + 1)));
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation_sub_tmp = document.createElement("type");
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode("ServiceDeliveryPoint"));
				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				tag_servicePointDeviceAssociation_tmp = document.createElement("deviceId");

				servicePointDeviceAssociation_sub_tmp = document.createElement("mRID");
				// servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("assetnum")));
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());

				xmlClause.append(Use_XML_Data.get("sdp_device").get("meterid" + (i + 1)));
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));

				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation_sub_tmp = document.createElement("type");
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode("Meter"));
				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				payload.appendChild(servicePointDeviceAssociation);

				// ----SERVICE POINT CTPT ASSOCIATION
				if (typeIndex == 0) {
					servicePointDeviceAssociation = document.createElement("servicePointDeviceAssociation");

					tag_servicePointDeviceAssociation_tmp = document.createElement("relType");
					tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode("SDP-CT/PT"));
					servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

					tag_servicePointDeviceAssociation_tmp = document.createElement("startDate");

					tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_device").get("startdate" + (i + 1))));
					servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

					tag_servicePointDeviceAssociation_tmp = document.createElement("servicePointId");
					servicePointDeviceAssociation_sub_tmp = document.createElement("mRID");

					if (xmlClause.length() > 0)
						xmlClause.delete(0, xmlClause.length());

					xmlClause.append("SDP-");
					xmlClause.append(Use_XML_Data.get("sdp_device").get("sdp" + (i + 1)));
					servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));
					tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

					servicePointDeviceAssociation_sub_tmp = document.createElement("type");
					servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode("ServiceDeliveryPoint"));
					tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

					servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

					tag_servicePointDeviceAssociation_tmp = document.createElement("deviceId");

					servicePointDeviceAssociation_sub_tmp = document.createElement("mRID");
					// servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("assetnum")));
					if (xmlClause.length() > 0)
						xmlClause.delete(0, xmlClause.length());
					xmlClause.append("CTPT-");
					xmlClause.append(Use_XML_Data.get("sdp_device").get("meterUuid" + (i + 1)));
					servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));
					tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

					servicePointDeviceAssociation_sub_tmp = document.createElement("description");
					servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode("description"));
					tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

					servicePointDeviceAssociation_sub_tmp = document.createElement("type");
					servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode("CTPT"));
					tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

					servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);
				}
				payload.appendChild(servicePointDeviceAssociation);
			}
			// -----------------------------sdp group--------------------------------------------------
			for (int i = 0; i < type_cnt.get("sdp_group"); i++) {
				Element servicePointServicePointGroupAssociation = document.createElement("servicePointServicePointGroupAssociation");

				Element tag_servicePointServicePointGroupAssociation_tmp = document.createElement("startDate");
				// tag_servicePointServicePointGroupAssociation_tmp.appendChild(document.createTextNode(currMbo.getString("statusdate")));
				tag_servicePointServicePointGroupAssociation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_group").get("startdate" + (i + 1))));
				servicePointServicePointGroupAssociation.appendChild(tag_servicePointServicePointGroupAssociation_tmp);

				tag_servicePointServicePointGroupAssociation_tmp = document.createElement("servicePointId");

				Element servicePointServicePointGroupAssociation_sub_tmp = document.createElement("mRID");
				// servicePointServicePointGroupAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("SADDRESSCODE")));

				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("SDP-");
				xmlClause.append(Use_XML_Data.get("sdp_group").get("sdp" + (i + 1)));
				servicePointServicePointGroupAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_servicePointServicePointGroupAssociation_tmp.appendChild(servicePointServicePointGroupAssociation_sub_tmp);

				servicePointServicePointGroupAssociation_sub_tmp = document.createElement("type");
				servicePointServicePointGroupAssociation_sub_tmp.appendChild(document.createTextNode("ServiceDeliveryPoint"));
				tag_servicePointServicePointGroupAssociation_tmp.appendChild(servicePointServicePointGroupAssociation_sub_tmp);

				servicePointServicePointGroupAssociation.appendChild(tag_servicePointServicePointGroupAssociation_tmp);

				tag_servicePointServicePointGroupAssociation_tmp = document.createElement("servicePointGroupId");

				servicePointServicePointGroupAssociation_sub_tmp = document.createElement("mRID");
				// servicePointServicePointGroupAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("zz_group")));
				// servicePointServicePointGroupAssociation_sub_tmp.appendChild(document.createTextNode("group"));

				servicePointServicePointGroupAssociation_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_group").get("cyclecd" + (i + 1))));// NBS.cycleCd 計算日

				tag_servicePointServicePointGroupAssociation_tmp.appendChild(servicePointServicePointGroupAssociation_sub_tmp);

				servicePointServicePointGroupAssociation_sub_tmp = document.createElement("type");
				servicePointServicePointGroupAssociation_sub_tmp.appendChild(document.createTextNode("Route"));
				tag_servicePointServicePointGroupAssociation_tmp.appendChild(servicePointServicePointGroupAssociation_sub_tmp);

				servicePointServicePointGroupAssociation.appendChild(tag_servicePointServicePointGroupAssociation_tmp);

				// tag_servicePointServicePointGroupAssociation_tmp = document.createElement("readSeq");
				// tag_servicePointServicePointGroupAssociation_tmp.appendChild(document.createTextNode("1"));
				// servicePointServicePointGroupAssociation.appendChild(tag_servicePointServicePointGroupAssociation_tmp);

				payload.appendChild(servicePointServicePointGroupAssociation);
			}
			// ----------------device functionassociation-------
			for (int i = 0; i < type_cnt.get("sdp_device"); i++) {
				// ----SERVICE POINT METER ASSOCIATION
				Element servicePointDeviceAssociation = document.createElement("deviceFunctionAssociation");

				Element tag_servicePointDeviceAssociation_tmp = document.createElement("relType");
				tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode("COMMUNICATION-METER"));
				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				tag_servicePointDeviceAssociation_tmp = document.createElement("startDate");
				tag_servicePointDeviceAssociation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_device").get("startdate" + (i + 1))));
				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				tag_servicePointDeviceAssociation_tmp = document.createElement("deviceId");
				Element servicePointDeviceAssociation_sub_tmp = document.createElement("mRID");

				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());

				xmlClause.append(Use_XML_Data.get("sdp_device").get("meterid" + (i + 1)));
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));
				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation_sub_tmp = document.createElement("type");
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode("Meter"));
				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				tag_servicePointDeviceAssociation_tmp = document.createElement("comFunctionId");

				servicePointDeviceAssociation_sub_tmp = document.createElement("mRID");
				// servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("assetnum")));
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("LTEFAN-");
				xmlClause.append(Use_XML_Data.get("sdp_device").get("meterid" + (i + 1)));
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));

				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation_sub_tmp = document.createElement("type");
				servicePointDeviceAssociation_sub_tmp.appendChild(document.createTextNode("CommModule"));
				tag_servicePointDeviceAssociation_tmp.appendChild(servicePointDeviceAssociation_sub_tmp);

				servicePointDeviceAssociation.appendChild(tag_servicePointDeviceAssociation_tmp);

				payload.appendChild(servicePointDeviceAssociation);
			}

			// ----------original consumerServicePointAssociation=>accountServicePointAssociation
			for (int i = 0; i < type_cnt.get("sdp_consumer"); i++) {
				Element consumerServicePointAssociation = document.createElement("accountServicePointAssociation");

				Element tag_consumerServicePointAssociation_tmp = document.createElement("startDate");

				// String Body_Consumer_time = sdf.format(currMbo.getDate("statusdate"));
				// tag_consumerServicePointAssociation_tmp.appendChild(document.createTextNode(Body_Consumer_time));
				tag_consumerServicePointAssociation_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_consumer").get("startdate" + (i + 1))));

				consumerServicePointAssociation.appendChild(tag_consumerServicePointAssociation_tmp);

				tag_consumerServicePointAssociation_tmp = document.createElement("accountId");

				Element deviceFunctionAssociation_sub_tmp = document.createElement("mRID");
				// deviceFunctionAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("zz_custno")));
				deviceFunctionAssociation_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_consumer").get("custNo" + (i + 1))));
				tag_consumerServicePointAssociation_tmp.appendChild(deviceFunctionAssociation_sub_tmp);
				consumerServicePointAssociation.appendChild(tag_consumerServicePointAssociation_tmp);
				// ------------------------------

				tag_consumerServicePointAssociation_tmp = document.createElement("servicePointId");

				deviceFunctionAssociation_sub_tmp = document.createElement("mRID");
				// deviceFunctionAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("SADDRESSCODE")));
				if (xmlClause.length() > 0)
					xmlClause.delete(0, xmlClause.length());
				xmlClause.append("SDP-");
				xmlClause.append(Use_XML_Data.get("sdp_consumer").get("sdp" + (i + 1)));
				deviceFunctionAssociation_sub_tmp.appendChild(document.createTextNode(xmlClause.toString()));

				tag_consumerServicePointAssociation_tmp.appendChild(deviceFunctionAssociation_sub_tmp);

				deviceFunctionAssociation_sub_tmp = document.createElement("type");
				deviceFunctionAssociation_sub_tmp.appendChild(document.createTextNode("ServiceDeliveryPoint"));
				tag_consumerServicePointAssociation_tmp.appendChild(deviceFunctionAssociation_sub_tmp);
				consumerServicePointAssociation.appendChild(tag_consumerServicePointAssociation_tmp);

				// tag_consumerServicePointAssociation_tmp = document.createElement("premiseId");
				// deviceFunctionAssociation_sub_tmp = document.createElement("mRID");
				// // deviceFunctionAssociation_sub_tmp.appendChild(document.createTextNode(currMbo.getString("SADDRESSCODE")));
				// deviceFunctionAssociation_sub_tmp.appendChild(document.createTextNode(Use_XML_Data.get("sdp_consumer").get("sdp" + (i + 1))));
				// tag_consumerServicePointAssociation_tmp.appendChild(deviceFunctionAssociation_sub_tmp);
				// consumerServicePointAssociation.appendChild(tag_consumerServicePointAssociation_tmp);

				// tag_consumerServicePointAssociation_tmp = document.createElement("relType");
				// tag_consumerServicePointAssociation_tmp.appendChild(document.createTextNode("relType"));
				// consumerServicePointAssociation.appendChild(tag_consumerServicePointAssociation_tmp);

				payload.appendChild(consumerServicePointAssociation);
			}

			// transform the DOM Object to an XML File

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(new File(xmlFilePath));

			// If you use
			// StreamResult result = new StreamResult(System.out);
			// the output will be pushed to the standard output ...
			// You can use that for debugging
			transformer.transform(domSource, streamResult);

			System.out.println("Done creating XML File");

			StringWriter writer1 = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(writer1));
			tmp_str = writer1.getBuffer().toString();
			// ----------------------------------------------------------------

			try {
				HttpURLConnection conn = null;
				StringBuilder response = new StringBuilder();

				// URL url = new
				// URL("http://nickall.asuscomm.com:1880/NBS_TST");
				// URL url = new URL("http://vm-win2012r2/maximo/NBSShow");
				// URL url = new URL("http://vm-win2012r2:1880/NBS_TST");
				// URL url = new URL("http://192.168.34.22/maximo/NBSShow");
				URL url = new URL(PostDes);
				myLogger.debug("start post url");
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("Content-Type", "text/xml");
				conn.setRequestMethod("POST");
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);
				conn.setDoInput(true); // 允許輸入流，即允許下載
				conn.setDoOutput(true); // 允許輸出流，即允許上傳
				conn.setUseCaches(false); // 設置是否使用緩存

				OutputStream os = conn.getOutputStream();
				myLogger.debug("Send post url");
				DataOutputStream writer = new DataOutputStream(os);
				// JsonObject obj = newJsonParser().parse(NBS_payload.toString()).getAsJsonObject();
				// writer.write(obj.toString().getBytes("UTf-8"));

				// writer.write(streamResult.toString().getBytes("UTF-8"));
				writer.write(tmp_str.getBytes("UTF-8"));
				writer.flush();
				writer.close();
				os.close();

				// Get Response

				InputStream is = conn.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				String line;

				myLogger.debug("get post result");
				while ((line = reader.readLine()) != null) {
					response.append(line);
					response.append('\r');
				}
				reader.close();

				// return response.toString();
			} catch (Exception ex) { // post fail!!
				myLogger.debug("send xml error");
				return false;
			}

		} catch (ParserConfigurationException pce) {

		} catch (TransformerException tfe) {

		}
		return true;

	}

	public Boolean PrepareMDMS_XML_Data(MboRemote AssetTmp, MboRemote NBSMboImp, int type_index, Hashtable<String, String> cell_consumer_xml,
			Hashtable<String, String> cell_servicelocation_xml, Hashtable<String, String> cell_servicepoint_xml,
			Hashtable<String, String> cell_device_xml, Hashtable<String, String> cell_sdp_device_xml, Hashtable<String, String> cell_sdp_group_xml,
			Hashtable<String, String> cell_sdp_consumer_xml, Hashtable<String, Integer> xml_type_cnt)
			throws RemoteException, MXException, Exception {

		// -----------consumer-------------------------------------------------------
		Hashtable<String, String> cell_tmp_xml = new Hashtable<String, String>();

		//String[] attrs = { "custno", "assetnum", "updatedate", };
		String[] attrs = { "zz_custno", "assetnum"};
		MboValueData[] valData = AssetTmp.getMboValueData(attrs);
		String xmlPrms = AssetTmp.getString("SADDRESSCODE");
		String xml_location = AssetTmp.getString("location");
		String getLatData = "";
		String getLonData = "";
		String get_data;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		get_data = AssetTmp.getString("assetnum");
		String getStartDate = sdf.format(AssetTmp.getDate("statusdate"));

		getLatData = AssetTmp.getString("ZZ_LAT");
		if (getLatData.equals(null))
			getLatData = "";
		getLonData = AssetTmp.getString("ZZ_LON");
		if (getLonData.equals(null))
			getLonData = "";

		int tmp_cnt = 0;

		if (type_index == 1) {
			// if (Arr_Consumer.indexOf(valData[0].getData()) < 0) {
			tmp_cnt = xml_type_cnt.get("consumer");
			tmp_cnt++;
			cell_consumer_xml.put("custNo" + tmp_cnt, valData[0].getData());
			cell_consumer_xml.put("customername" + tmp_cnt, NBSMboImp.getString("customername"));
			xml_type_cnt.put("consumer", tmp_cnt);
			// Arr_Consumer.add(valData[0].getData());
			// }

			// ----------servicelocation------------------------------------------------
			tmp_cnt = xml_type_cnt.get("servicelocation");
			tmp_cnt++;

			cell_servicelocation_xml.put("prms" + tmp_cnt, xmlPrms);
			cell_servicelocation_xml.put("customeraddress" + tmp_cnt, NBSMboImp.getString("customeraddress"));
			cell_servicelocation_xml.put("latitude" + tmp_cnt, AssetTmp.getString("ZZ_LAT"));
			cell_servicelocation_xml.put("longitude" + tmp_cnt, AssetTmp.getString("ZZ_LON"));
			xml_type_cnt.put("servicelocation", tmp_cnt);

			// -----------servicepoint--------------------------------------------------
			tmp_cnt = xml_type_cnt.get("servicepoint");
			tmp_cnt++;
			// cell_servicepoint_xml.put("location" + tmp_cnt, Location_Insert);
			cell_servicepoint_xml.put("location" + tmp_cnt, xml_location);
			cell_servicepoint_xml.put("uuid" + tmp_cnt, AssetTmp.getString("ZZ_UUID"));
			cell_servicepoint_xml.put("premisid" + tmp_cnt, xmlPrms);
			cell_servicepoint_xml.put("latitude" + tmp_cnt, AssetTmp.getString("ZZ_LAT"));
			cell_servicepoint_xml.put("longitude" + tmp_cnt, AssetTmp.getString("ZZ_LON"));
			cell_servicepoint_xml.put("contracttype" + tmp_cnt, NBSMboImp.getString("contracttype"));
			cell_servicepoint_xml.put("section" + tmp_cnt, NBSMboImp.getString("section"));
			cell_servicepoint_xml.put("cyclecd" + tmp_cnt, NBSMboImp.getString("cyclecd"));
			cell_servicepoint_xml.put("gisId" + tmp_cnt, AssetTmp.getString("zz_taipowergrid"));
			cell_servicepoint_xml.put("publishType" + tmp_cnt, NBSMboImp.getString("publicclassid"));
			cell_servicepoint_xml.put("startdate" + tmp_cnt, getStartDate);

			xml_type_cnt.put("servicepoint", tmp_cnt);

		}
		// -----------device----------------------------------------------------------
		tmp_cnt = xml_type_cnt.get("device");
		tmp_cnt++;
		cell_device_xml.put("meterid" + tmp_cnt, valData[1].getData());
		cell_device_xml.put("manufacturedDate" + tmp_cnt, AssetTmp.getString("statusdate"));
		cell_device_xml.put("serialnum" + tmp_cnt, AssetTmp.getString("serialnum"));
		cell_device_xml.put("status" + tmp_cnt, AssetTmp.getString("status"));
		cell_device_xml.put("make" + tmp_cnt, valData[1].getData().substring(0, 2));
		cell_device_xml.put("location" + tmp_cnt, valData[0].getData().substring(0, 2));
		cell_device_xml.put("usuallyContract" + tmp_cnt, NBSMboImp.getString("usuallycontract"));
		cell_device_xml.put("startdate" + tmp_cnt, getStartDate);
		cell_device_xml.put("multiplierValue" + tmp_cnt, NBSMboImp.getString("metermultiple"));
		cell_device_xml.put("meterUuid" + tmp_cnt, AssetTmp.getString("zz_uuid"));

		MboSetRemote getFanMboset = AssetTmp.getMboSet("PARENT");
		if (!getFanMboset.isEmpty()) {
			cell_device_xml.put("fanManufactureDate" + tmp_cnt, getFanMboset.getMbo(0).getString("statusdate"));
			cell_device_xml.put("fanSerialNum" + tmp_cnt, getFanMboset.getMbo(0).getString("serialnum"));
		} else {
			cell_device_xml.put("fanManufactureDate" + tmp_cnt, "");
			cell_device_xml.put("fanSerialNum" + tmp_cnt, "");
		}

		MboSetRemote getCityAttrMboSet = AssetTmp.getMboSet("SERVICEADDRESS");
		if (!getCityAttrMboSet.isEmpty())
			cell_device_xml.put("city" + tmp_cnt, getCityAttrMboSet.getMbo(0).getString("city"));
		else
			cell_device_xml.put("city" + tmp_cnt, "");

		MboSetRemote getAssetSpecMboSet = AssetTmp.getMboSet("ASSETSPEC");
		if (!getAssetSpecMboSet.isEmpty()) {
			for (int specCount = 0; specCount < getAssetSpecMboSet.count(); specCount++) {

				if (getAssetSpecMboSet.getMbo(specCount).getString("assetattrid").equalsIgnoreCase("programid")) {
					cell_device_xml.put("programId" + tmp_cnt, getAssetSpecMboSet.getMbo(specCount).getString("alnvalue"));
				} else if (getAssetSpecMboSet.getMbo(specCount).getString("assetattrid").equalsIgnoreCase("hv_lv")) {
					cell_device_xml.put("voltageType" + tmp_cnt, getAssetSpecMboSet.getMbo(specCount).getString("alnvalue"));
				} else if (getAssetSpecMboSet.getMbo(specCount).getString("assetattrid").equalsIgnoreCase("CTRATIO")) {
					cell_device_xml.put("ctRatio" + tmp_cnt, getAssetSpecMboSet.getMbo(specCount).getString("alnvalue"));
				} else if (getAssetSpecMboSet.getMbo(specCount).getString("assetattrid").equalsIgnoreCase("phaseline")) {

					if (getAssetSpecMboSet.getMbo(specCount).getString("alnvalue").equalsIgnoreCase("單相三線"))
						cell_device_xml.put("phase" + tmp_cnt, "1");
					else if (getAssetSpecMboSet.getMbo(specCount).getString("alnvalue").equalsIgnoreCase("三相四線"))
						cell_device_xml.put("phase" + tmp_cnt, "3");
					else
						cell_device_xml.put("phase" + tmp_cnt, "");
				} else if (getAssetSpecMboSet.getMbo(specCount).getString("assetattrid").equalsIgnoreCase("direction")) {
					if (getAssetSpecMboSet.getMbo(specCount).getString("alnvalue").equalsIgnoreCase("oneway"))
						cell_device_xml.put("direction" + tmp_cnt, "Uni");
					else if (getAssetSpecMboSet.getMbo(specCount).getString("alnvalue").equalsIgnoreCase("twoway"))
						cell_device_xml.put("direction" + tmp_cnt, "Bi");
					else
						cell_device_xml.put("direction" + tmp_cnt, "");
				}
			}
		} else {
			cell_device_xml.put("programId" + tmp_cnt, "");

		}
		MboSetRemote getHesMboSet = AssetTmp.getMboSet("ZZ_HESCODEDATA");
		if (!getHesMboSet.isEmpty())
			cell_device_xml.put("hesname" + tmp_cnt, getHesMboSet.getMbo(0).getString("hes_code"));
		else
			cell_device_xml.put("hesname" + tmp_cnt, "");

		xml_type_cnt.put("device", tmp_cnt);
		// ------------sdp device---------------------------------------------------------
		tmp_cnt = xml_type_cnt.get("sdp_device");
		tmp_cnt++;
		cell_sdp_device_xml.put("meterid" + tmp_cnt, valData[1].getData());
		cell_sdp_device_xml.put("startdate" + tmp_cnt, getStartDate);
		cell_sdp_device_xml.put("sdp" + tmp_cnt, xml_location);
		cell_sdp_device_xml.put("prms" + tmp_cnt, xmlPrms);
		cell_sdp_device_xml.put("meterUuid" + tmp_cnt, AssetTmp.getString("zz_uuid"));

		xml_type_cnt.put("sdp_device", tmp_cnt);

		if (type_index == 1) {
			// -----------sdp group-------------------------------------------------
			tmp_cnt = xml_type_cnt.get("sdp_group");
			tmp_cnt++;
			cell_sdp_group_xml.put("startdate" + tmp_cnt, getStartDate);
			cell_sdp_group_xml.put("prms" + tmp_cnt, xmlPrms);
			cell_sdp_group_xml.put("sdp" + tmp_cnt, xml_location);
			cell_sdp_group_xml.put("group" + tmp_cnt, NBSMboImp.getString("metergroup"));
			cell_sdp_group_xml.put("cyclecd" + tmp_cnt, NBSMboImp.getString("cyclecd"));

			xml_type_cnt.put("sdp_group", tmp_cnt);
			// -----------consumer group-------------------------------------------------------
			tmp_cnt = xml_type_cnt.get("sdp_consumer");
			tmp_cnt++;
			cell_sdp_consumer_xml.put("startdate" + tmp_cnt, getStartDate);
			cell_sdp_consumer_xml.put("custNo" + tmp_cnt, valData[0].getData());
			cell_sdp_consumer_xml.put("prms" + tmp_cnt, xmlPrms);
			cell_sdp_consumer_xml.put("sdp" + tmp_cnt, xml_location);
			xml_type_cnt.put("sdp_consumer", tmp_cnt);
		}
		return true;
	}

	public void SaveNBSData(String PostDes, Boolean Exe_MDMS_Flg, Hashtable<String,
			Hashtable<String, String>> MeterData) throws ServletException, IOException {
		HttpURLConnection conn = null;
		// StringBuilder response = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		String[] Getdata = null;
		Object[] param = new Object[2];
		String[] get_err_mes = null;
		mxServer = MXServer.getMXServer();
		try {
			ui = mxServer.getSystemUserInfo();
		} catch (MXException e1) {

		}
		AssetSet TmpAssetSet = null;

		// ------------------------------------------------------
		int docount = 0;
		String getMeterId = "";
		boolean checkSendXmlFlag = false;
		try {
			TmpAssetSet = (AssetSet) mxServer.getMboSet("ASSET", ui);
			assetToNbsMbo = mxServer.getMboSet("ZZ_ASSETTONBS", ui);
			for (String i : MeterData.keySet()) {
				// ----use for for create xml for new
				Hashtable<String, Hashtable<String, String>> NBS_XML_Data = new Hashtable<String, Hashtable<String, String>>();
				Hashtable<String, Integer> xml_type_cnt = new Hashtable<String, Integer>();
				Hashtable<String, String> cell_consumer_xml = new Hashtable<String, String>();
				Hashtable<String, String> cell_servicelocation_xml = new Hashtable<String, String>();
				Hashtable<String, String> cell_servicepoint_xml = new Hashtable<String, String>();
				Hashtable<String, String> cell_device_xml = new Hashtable<String, String>();
				Hashtable<String, String> cell_sdp_device_xml = new Hashtable<String, String>();
				Hashtable<String, String> cell_sdp_group_xml = new Hashtable<String, String>();
				Hashtable<String, String> cell_sdp_consumer_xml = new Hashtable<String, String>();
				NBS_XML_Data = new Hashtable<String, Hashtable<String, String>>(); // save for xml data
				xml_type_cnt = new Hashtable<String, Integer>();
				cell_consumer_xml = new Hashtable<String, String>();
				cell_servicelocation_xml = new Hashtable<String, String>();
				cell_servicepoint_xml = new Hashtable<String, String>();
				cell_device_xml = new Hashtable<String, String>();
				cell_sdp_device_xml = new Hashtable<String, String>();
				cell_sdp_group_xml = new Hashtable<String, String>();
				cell_sdp_consumer_xml = new Hashtable<String, String>();

				NBS_XML_Data.put("consumer", cell_consumer_xml);
				NBS_XML_Data.put("servicelocation", cell_servicelocation_xml);
				NBS_XML_Data.put("servicepoint", cell_servicepoint_xml);
				NBS_XML_Data.put("device", cell_device_xml);
				NBS_XML_Data.put("sdp_device", cell_sdp_device_xml);
				NBS_XML_Data.put("sdp_group", cell_sdp_group_xml);
				NBS_XML_Data.put("sdp_consumer", cell_sdp_consumer_xml);

				xml_type_cnt.put("consumer", 0);
				xml_type_cnt.put("servicelocation", 0);
				xml_type_cnt.put("servicepoint", 0);
				xml_type_cnt.put("sdp_device", 0);
				xml_type_cnt.put("device", 0);
				xml_type_cnt.put("sdp_group", 0);
				xml_type_cnt.put("sdp_consumer", 0);

				if (docount + 1 >= dostartindex) {
					getMeterId = MeterData.get(i).get("meterId");
					if (setWhereClause.length() > 0)
						setWhereClause.delete(0, setWhereClause.length());
					setWhereClause.append("assetnum='");
					setWhereClause.append(getMeterId);
					setWhereClause.append("'");
					// KeepLog(setWhereClause.toString());
					assetToNbsMbo.setWhere(setWhereClause.toString());
					assetToNbsMbo.reset();
					if (assetToNbsMbo.isEmpty()) { // add meter

					} else { // find the nbs data
						TmpAssetSet.setWhere(setWhereClause.toString());
						TmpAssetSet.reset();
						if (TmpAssetSet.isEmpty()) {

						} else { // find the asset data

							PrepareMDMS_XML_Data(
									TmpAssetSet.getMbo(0), assetToNbsMbo.getMbo(0), 1, cell_consumer_xml, cell_servicelocation_xml,
									cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
									cell_sdp_consumer_xml, xml_type_cnt);
							checkSendXmlFlag = crateXml_multi(PostDes, NBS_XML_Data, xml_type_cnt, 0);
						}
					}

				}
				docount = docount + 1;
				if (docount >= doendindex)
					break;
			}

		} catch (Exception e) {

		}

	}

}
