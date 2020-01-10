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

public class CustNBSToMDMCronTask extends SimpleCronTask {
	static MXLogger						myLogger			= MXLoggerFactory
																	.getLogger("maximo.sql.LOCATION.LOCATIONS");
	static String[]						NBS_NAME			= { // get json data from soa
															"custNo", "customername", "customeraddress", "backusuallycontract",
															"backnonesummercontract", "backsaturdayhalfpeakcontract",
															"backoffpeakcontract", "usuallycontract", "nonesummercontract",
															"offpeakcontract", "saturdayhalfpeakcontract", "contracttype",
															"meterBatch", "updatedate", "updatetype", "meterId", "group",
															"meterMultiple", "taxid", "industclass", "section",
															"capacitystrengh", "capacityheat", "capacitylight", "cyclecd",
															"transdt", "daycd", "citycd", "feeder", "custcd", "outagegroup" };

	// use for data insert
	// static String[] ZZ_Tmp_NBS_Insert = { "customername", "customeraddress", "backusuallycontract", "backnonesummercontract",
	// "backsaturdayhalfpeakcontract", "backoffpeakcontract", "usuallycontract", "nonesummercontract",
	// "offpeakcontract", "saturdayhalfpeakcontract", "contracttype", "meterlayer", "updatedate", "updatetype",
	// "metergroup", "metertimes", "taxid", "industclass", "section", "billingDate", "uplstatus", "upltime", "uplresult" };
	static String[]						ZZ_Tmp_NBS_Insert	= {
															"assetnum", "customername", "customeraddress", "backusuallycontract",
															"backnonesummercontract", "backsaturdayhalfpeakcontract",
															"backoffpeakcontract", "usuallycontract", "nonesummercontract",
															"offpeakcontract", "saturdayhalfpeakcontract", "contracttype",
															"meterBatch", "updatedate", "updatetype", "metergroup",
															"meterMultiple", "taxid", "industclass", "section",
															"capacitystrengh", "capacityheat", "capacitylight", "cyclecd",
															"transdt", "daycd", "citycd", "feeder", "custcd", "outagegroup" };

	// use for data post
	static String[]						ZZ_Tmp_NBS_Post		= { "custno", "customername", "backusuallycontract", "backnonesummercontract",
															"backsaturdayhalfpeakcontract", "backoffpeakcontract", "usuallycontract", "nonesummercontract",
															"offpeakcontract", "saturdayhalfpeakcontract", "contracttype", "meterBatch", "updatedate", "updatetype",
															"meterId", "zz_group", "meterMultiple", "taxid", "industclass", "section", "billingDate" };
	public static MXServer				mxServer;
	public static UserInfo				ui;
	public static String				Location_Insert;
	public static MboSetRemote			AMI_Insert			= null;
	private static CrontaskParamInfo[]	params;
	public static String				Service_Address_Insert;
	public static MboSetRemote			assettonbs_st		= null;
	static {
		params = new CrontaskParamInfo[3];
		params[0] = new CrontaskParamInfo();
		params[0].setName("WHERE");
		params[1] = new CrontaskParamInfo();
		params[1].setName("MDMS_EXECUTE");
		params[1].setDefault("false");
		params[2] = new CrontaskParamInfo();
		params[2].setName("MOVEASSET");
		params[2].setDefault("false");
	}

	@Override public void cronAction() {
		// Your custom code here. e.g. A sample code is below.

		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			// System.out.println("MyFirstCrontaskRTING CRONTASK");
			myLogger.debug("Cron task MAM Update NBS Data to SOA");
			// nickall.asuscomm.com:
			// httpConnectionPost("http://127.0.0.1:1880/NBS_TST");
			Boolean isExeMDMS = getParamAsBoolean("MDMS_EXECUTE");
			// httpConnectionPost("http://nickall.asuscomm.com:1880/NBS_TST",
			// isExeMDMS);
			// httpConnectionPost("http://192.168.34.22/maximo/NBSShow", isExeMDMS);
			SaveNBSData("http://127.0.0.1:1880/NBS_TST", isExeMDMS);
			// End Sample Code
		} catch (Exception e) {

		}
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	public static void CheckImpLocExist(String CustNo)
			throws RemoteException, MXException {

		MboSetRemote AMI_Service_Insert = null;
		LocationSet locationsSet;
		AMI_Insert = (AssetSet) mxServer.getMboSet("ASSET", ui);
		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		AMI_Insert.setWhere("UPPER(ZZ_CUSTNO) = UPPER('" + CustNo + "')");
		AMI_Insert.reset();
		int tmp_count = 0;
		// String tmp_ser = "";
		// String tmp_data = "";
		Location_Insert = "";
		// String tmp_mes = "";
		try {
			if (!AMI_Insert.isEmpty()) {
				// tmp_count = rs.getInt(1);
				// ----V1.2 Add Start---------------------------------
				// tmp_count = AMI_Insert.count();
				// tmp_ser = String.format("%04d", tmp_count + 1);
				// tmp_data = CustNo + "-" + tmp_ser + "-P";
				// Location_Insert = CustNo + "-" + tmp_ser;
				// AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ");
				// AMI_Service_Insert.reset();
				Location_Insert = UUID.randomUUID().toString();
				Service_Address_Insert = UUID.randomUUID().toString();
				AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + Service_Address_Insert + "') AND UPPER(ORGID) = 'TPC' ");
				AMI_Service_Insert.reset();
				if (AMI_Service_Insert.isEmpty()) {
					MboRemote newService = AMI_Service_Insert.add();
					newService.setValue("ADDRESSCODE", Service_Address_Insert, 11L);
					newService.setValue("LANGCODE", "ZHT", 11L);
					newService.setValue("ORGID", "TPC", 11L);
					newService.setValue("SITEID", CustNo.substring(0, 2), 11L);
					AMI_Service_Insert.save();
				} else {
					myLogger.debug("Service ADDRESSCODE UUID Duplicate!!");
				}

				myLogger.debug("Check how many meter install under elect_num");
			} else { // add new custno
				// tmp_count = 0;
				// tmp_ser = String.format("%04d", tmp_count + 1);
				// tmp_data = CustNo + "-" + tmp_ser + "-P";
				// Location_Insert = CustNo + "-" + tmp_ser;
				// tmp_mes = "UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ";
				// AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ");
				// AMI_Service_Insert.reset();
				Location_Insert = UUID.randomUUID().toString();
				Service_Address_Insert = UUID.randomUUID().toString();
				AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + Service_Address_Insert + "') AND UPPER(ORGID) = 'TPC' ");
				AMI_Service_Insert.reset();
				if (AMI_Service_Insert.isEmpty()) {
					MboRemote newService = AMI_Service_Insert.add();
					newService.setValue("ADDRESSCODE", Service_Address_Insert, 11L);
					newService.setValue("LANGCODE", "ZHT", 11L);
					newService.setValue("ORGID", "TPC", 11L);
					newService.setValue("SITEID", CustNo.substring(0, 2), 11L);
					AMI_Service_Insert.save();
				} else {
					myLogger.debug("Service ADDRESSCODE UUID Duplicate!!");
				}
			}
		} catch (Exception e) {

			myLogger.debug("Create Service Err:" + e.getMessage());
		}

		locationsSet.setWhere("UPPER(LOCATION) = UPPER('" + Location_Insert + "')");

		try {
			if (!locationsSet.isEmpty()) { // Location exist
				myLogger.debug("Locations location UUID Duplicate!!");
			} else {
				MboRemote newLocation = locationsSet.add();
				newLocation.setValue("LOCATION", Location_Insert, 11L);
				newLocation.setValue("SITEID", CustNo.substring(0, 2), 11L);
				newLocation.setValue("SADDRESSCODE", Service_Address_Insert, 11L);

				newLocation.setValue("TYPE", "OPERATE", 11L);

				locationsSet.save();
				try {
					MboSetRemote locHierarchySet = null;
					locHierarchySet = newLocation.getMboSet("LOCHIERARCHY");
					MboRemote newHierarchy = locHierarchySet.add();
					newHierarchy.setValue("CHILDREN", 0, 11L);
					newHierarchy.setValue("LOCATION", Location_Insert, 11L);
					newHierarchy.setValue("SYSTEMID", CustNo.substring(0, 2), 11L);
					newHierarchy.setValue("PARENT", CustNo.substring(0, 2), 11L);
					locHierarchySet.save();
				} catch (MXException e) {
					myLogger.debug("Save Locate LOCHIERARCHY:" + e.getMessage());
					throw e;
				}
			}
		} catch (Exception e) {

			myLogger.debug("Save Locate Err:" + e.getMessage());
		}
	}

	private static Boolean doUpdDataACT(String Updassetnum, MboRemote NBSMboImp) throws RemoteException, MXException, Exception {
		try {
			assettonbs_st.setWhere("UPPER(ASSETNUM) = UPPER('" + Updassetnum + "')");
			assettonbs_st.reset();
			if (assettonbs_st.isEmpty()) {
				MboRemote newnbs = assettonbs_st.add();
				for (int i = 0; i < ZZ_Tmp_NBS_Insert.length; i++) {
					if (ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("updatedate"))
						newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getDate(ZZ_Tmp_NBS_Insert[i]));
					else
						newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getString(ZZ_Tmp_NBS_Insert[i]));
				}
			} else {
				MboRemote newnbs = assettonbs_st.getMbo(0);
				for (int i = 0; i < ZZ_Tmp_NBS_Insert.length; i++) {
					if (ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("updatedate"))
						newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getDate(ZZ_Tmp_NBS_Insert[i]));
					else if (!ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("assetnum"))
						newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getString(ZZ_Tmp_NBS_Insert[i]));
				}
			}
			if (assettonbs_st.toBeSaved())
				assettonbs_st.save();
			return true;
		} catch (Exception e) {
			NBSMboImp.setValue("description", e.getMessage(), 11L);
			myLogger.debug("Save data to assettonbs Err:" + e.getMessage());
			return false;
		}
	}

	private static Boolean doAddNBS(MboSetRemote AssetSetTmp, MboRemote NBSMboImp, String act_type)
			throws RemoteException, MXException, Exception {
		String[] attrs = { "custno", "assetnum", "updatedate", };
		MboValueData[] valData = NBSMboImp.getMboValueData(attrs);
		boolean check_flag = false;
		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		String Classifty_ID = "";
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		try {
			Date parseDate = (valData[2].getDataAsDate());
			MboRemote newAsset = null;
			if (act_type.equalsIgnoreCase("Instock")) {
				newAsset = AssetSetTmp.getMbo(0);
				((Asset) newAsset).changeStatus("NOCOMM", mxServer.getDate(), "NBS New Meter from status instock");
				((Asset) newAsset).moveAssetWithinNonInventory(Location_Insert, "NBS New Meter from status instock", mxServer.getDate(), "MAXADMIN",
						newAsset.getString("wonum"), newAsset.getString("newparent"), true, true, true);

				newAsset.setValue("ZZ_CUSTNO", valData[0].getData(), 11L); // v1.5
				newAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				newAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L); // V1.2
				newAsset.setValue("SADDRESSCODE", Service_Address_Insert, 11L); // V1.2
				newAsset.setValue("DESCRIPTION", valData[1].getData().substring(2, valData[1].getData().length()), 11L);
				newAsset.setValue("ZZ_FEEDER", NBSMboImp.getString("feeder"), 11L);
				newAsset.setValue("ZZ_ISTWOWAY", false, 11L);
				newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);

				if (!Classifty_ID.equals(""))
					newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);
			} else if (act_type.equalsIgnoreCase("New")) {
				newAsset = AssetSetTmp.add();
				newAsset.setValue("ASSETNUM", valData[1].getData(), 11L);
				newAsset.setValue("LOCATION", Location_Insert, 11L);
				newAsset.setValue("ZZ_CUSTNO", valData[0].getData(), 11L);
				newAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				newAsset.setValue("STATUS", "NOCOMM", 11L);
				newAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L);
				newAsset.setValue("ZZ_FEEDER", NBSMboImp.getString("feeder"), 11L); // V1.2
				newAsset.setValue("SADDRESSCODE", Service_Address_Insert, 11L); // V1.2
				newAsset.setValue("DESCRIPTION", valData[1].getData().substring(2, valData[1].getData().length()), 11L);
				newAsset.setValue("ZZ_ISTWOWAY", false, 11L);
				newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);
				if (!Classifty_ID.equals(""))
					newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);

			}

			MboSetRemote classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
			classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
			// ----------asset spec write---------------------------------------------------------------------------------------
			if (!classspecSet.isEmpty()) {
				MboSetRemote newAssetspecSet = mxServer.getMboSet("ASSETSPEC", MXServer.getMXServer().getSystemUserInfo());
				for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
					newAssetspecSet.setWhere("UPPER(assetnum) = UPPER('" + valData[1].getData() + "')" +
							" AND UPPER(assetattrid) = UPPER('" + assetSpec.getString("assetattrid") + "')");

					newAssetspecSet.reset();
					if (newAssetspecSet.isEmpty()) {
						SpecificationMboRemote assetspec = (SpecificationMboRemote) newAssetspecSet.addAtEnd();
						assetspec.addDetailInfor(AssetSetTmp.getMbo(0), assetSpec);
						assetspec.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
					}

				}
				if (newAssetspecSet.toBeSaved()) {
					newAssetspecSet.save();
				}
			}

			// -----------------write data to assettonbs----------------------------

			// assettonbs_st.setWhere("UPPER(ASSETNUM) = UPPER('" + valData[1].getData() + "')");
			// assettonbs_st.reset();
			// if (assettonbs_st.isEmpty()) {
			// MboRemote newnbs = assettonbs_st.add();
			// for (int i = 0; i < ZZ_Tmp_NBS_Insert.length; i++) {
			// if (ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("updatedate"))
			// newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getDate(ZZ_Tmp_NBS_Insert[i]));
			// else
			// newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getString(ZZ_Tmp_NBS_Insert[i]));
			// }
			// } else {
			// MboRemote newnbs = assettonbs_st.getMbo(0);
			// for (int i = 0; i < ZZ_Tmp_NBS_Insert.length; i++) {
			// if (ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("updatedate"))
			// newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getDate(ZZ_Tmp_NBS_Insert[i]));
			// else if (!ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("assetnum"))
			// newnbs.setValue(ZZ_Tmp_NBS_Insert[i], NBSMboImp.getString(ZZ_Tmp_NBS_Insert[i]));
			// }
			// }
			//
			// if (assettonbs_st.toBeSaved())
			// assettonbs_st.save();

			try {
				AssetSetTmp.save();
				check_flag = doUpdDataACT(valData[1].getData(), NBSMboImp);

				// if (check_flag == true)
				// PrepareXML_Data(newAsset, NBSMboImp,1);

			} catch (Exception e) {
				myLogger.debug("nbs error: save assset" + e.getLocalizedMessage());
				NBSMboImp.setValue("DESCRIPTION", e.getLocalizedMessage(), 11L);
			}

			// -----------------------------------------
			if (check_flag == true) {
				myLogger.debug("nbs aseset done");

				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
			myLogger.debug("nbs error:" + e.getLocalizedMessage());
			NBSMboImp.setValue("DESCRIPTION", e.getLocalizedMessage(), 11L);
			return false;
		}
	}

	private static Boolean doUpdACT(MboSetRemote AssetSetTmp, MboRemote NBSMboImp, String oldMeterId) throws RemoteException, MXException, Exception {
		String UpdateLocation = null;
		String UpdataSaddresscode = null;
		String new_meterid = NBSMboImp.getString("assetnum");
		String new_custno = NBSMboImp.getString("custno");
		StringBuilder setWhereClause = new StringBuilder();
		StringBuilder memoClause = new StringBuilder();
		String Remove_meter = "";
		String tmp_str = "";
		String get_asstnum = "";
		StringBuilder memoMessage = new StringBuilder();
		String oldMeterUuid = null;
		String newMeterId = null;
		String newMeterUuid = null;
		Boolean Find_Old_meter = false;
		MboRemote getMboData = null;
		boolean check_flag = false;

		// AssetSetTmp.setWhere("UPPER(zz_custno) = UPPER('" + new_custno + "')");
		// AssetSetTmp.reset();
		try {
			// if (!AssetSetTmp.isEmpty()) {
			// for (MboRemote assetremove = AssetSetTmp.moveFirst(); assetremove != null; assetremove = AssetSetTmp.moveNext()) {
			//
			// get_asstnum = assetremove.getString("assetnum");
			// if (get_asstnum.equalsIgnoreCase(new_meterid))
			// continue;
			// assettonbs_st.setWhere("UPPER(assetnum)=UPPER('" + get_asstnum + "')");
			// assettonbs_st.reset();
			//
			// if (!assettonbs_st.isEmpty()) {
			// MboRemote assettonbs_tmp = assettonbs_st.getMbo(0);
			// if (!NBSMboImp.getString("metergroup").equals("") || !assettonbs_tmp.getString("metergroup").equals("")) {
			// if (!NBSMboImp.getString("metergroup").equals(assettonbs_tmp.getString("metergroup"))) {
			// myLogger.debug("NBS Check Group not the same assetnum is  " + get_asstnum +
			// " transcation group:" + NBSMboImp.getString("metergroup") +
			// "  assettonbs group: " + assettonbs_tmp.getString("metergroup"));
			// continue;
			// }
			// } else {
			// myLogger.debug("NBS Tmp and ASSTETONBS group both empty!! check asset status");
			// if (assetremove.getString("STATUS").equalsIgnoreCase("INSTOCK")) {
			// NBSMboImp.setValue("description", "電表目前狀態為在庫!!", 11L);
			// return false;
			// }
			// }
			// myLogger.debug("NBS Tmp and ASSTETONBS group both empty!! asset statis is operate");
			// UpdateLocation = assetremove.getString("LOCATION");
			// UpdataSaddresscode = assetremove.getString("SADDRESSCODE");
			// Service_Address_Insert = UpdataSaddresscode;
			// if (memoMessage.length() > 0)
			// memoMessage.delete(0, memoMessage.length());
			// memoMessage.append("amichangeby,");
			//
			// ((Asset) assetremove).changeStatus("INSTOCK", mxServer.getDate(), "NBS Remove Meter to instock");
			// ((Asset) assetremove).moveAssetWithinNonInventory("LOCATION_INV", "NBS Remove Meter to status instock",
			// mxServer.getDate(), "MAXADMIN", assetremove.getString("wonum"), assetremove.getString("newparent"), true, true, true);
			// Find_Old_meter = true;
			// break;
			// } else {
			// myLogger.debug("can not find the data in assetonbs assetnum is " + get_asstnum);
			// if (NBSMboImp.getString("metergroup").equals("")) {
			// myLogger.debug("NBS Tmp and ASSTETONBS group both empty!! Check asset status");
			// if (assetremove.getString("STATUS").equalsIgnoreCase("INSTOCK")) {
			// NBSMboImp.setValue("description", "電表目前狀態為在庫!!", 11L);
			// return false;
			// }
			// myLogger.debug("NBS Tmp and ASSTETONBS table both empty!! status is operate");
			// UpdateLocation = assetremove.getString("LOCATION");
			// UpdataSaddresscode = assetremove.getString("SADDRESSCODE");
			// Service_Address_Insert = UpdataSaddresscode;
			// ((Asset) assetremove).changeStatus("INSTOCK", mxServer.getDate(), "NBS Remove Meter to instock");
			// ((Asset) assetremove).moveAssetWithinNonInventory("LOCATION_INV", "NBS Remove Meter to status instock",
			// mxServer.getDate(), "MAXADMIN", assetremove.getString("wonum"), assetremove.getString("newparent"), true, true, true);
			// Find_Old_meter = true;
			// break;
			// }
			// // NBSMboImp.setValue("description", "電表目前狀態為在庫!!", 11L);
			//
			// // return false;
			// }
			//
			// }
			// } else {
			// // NBSMboImp.setValue("description", "電號 " + new_custno + "目前無任何電表", 11L);
			// NBSMboImp.setValue("description", "電表基本資料不存在!!", 11L);
			// return false;
			// }
			// if (Find_Old_meter == false) {
			// NBSMboImp.setValue("description", "電表組別不存在!!", 11L);
			// return false;
			// }
			if (setWhereClause.length() > 0)
				setWhereClause.delete(0, setWhereClause.length());
			setWhereClause.append("UPPER(ASSETNUM)=UPPER('");
			setWhereClause.append(oldMeterId);
			setWhereClause.append("')");
			AssetSetTmp.setWhere(setWhereClause.toString());
			AssetSetTmp.reset();
			if (!AssetSetTmp.isEmpty()) {
				getMboData = AssetSetTmp.getMbo(0);
				UpdateLocation = getMboData.getString("LOCATION");
				UpdataSaddresscode = getMboData.getString("SADDRESSCODE");
				oldMeterUuid = getMboData.getString("ZZ_UUID");
				// ((Asset) getMboData).changeStatus("INSTOCK", mxServer.getDate(), "NBS Remove Meter to instock");
				// ((Asset) getMboData).moveAssetWithinNonInventory("LOCATION_INV", "NBS Remove Meter to instock",
				// mxServer.getDate(), "MAXADMIN", getMboData.getString("wonum"), getMboData.getString("newparent"), true, true, true);
				Find_Old_meter = true;

			} else {
				NBSMboImp.setValue("description", "舊電表資料不存在!!", 11L);
				return false;
			}

			// if (AssetSetTmp.toBeSaved())
			// AssetSetTmp.save();

			// -----------------Check if the new meter wheather exist
			// UpdateLocation=UUID.randomUUID().toString();

			AssetSetTmp.setWhere("UPPER(assetnum) = UPPER('" + new_meterid + "')"); // V1.8
			AssetSetTmp.reset();
			if (!AssetSetTmp.isEmpty()) { // -----------meter alread exist and move the related data to new custno
				MboRemote updAsset = AssetSetTmp.getMbo(0);
				if (updAsset.getString("STATUS").equalsIgnoreCase("INSTOCK")) {
					updAsset.setValue("NEWSITE", new_custno.substring(0, 2), 11L);
					((Asset) updAsset).changeStatus("NOCOMM", mxServer.getDate(), "AMI Update New Meter status nocomm");
					((Asset) updAsset).moveAssetWithinNonInventory(UpdateLocation, "AMI Update New Meter status nocomm", mxServer.getDate(),
							"MAXADMIN", updAsset.getString("wonum"), updAsset.getString("newparent"), true, true, true);
					updAsset.setValue("ZZ_CUSTNO", new_custno, 11L); // v1.5
					updAsset.setValue("SITEID", new_custno.substring(0, 2), 11L);
					updAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L); // V1.2
					updAsset.setValue("SADDRESSCODE", UpdataSaddresscode, 11L); // V1.2
					// updAsset.setValue("DESCRIPTION", meterid.substring(2, meterid.length()), 11L);
					newMeterId = updAsset.getString("assetnum");
					newMeterUuid = updAsset.getString("zz_uuid");

					if (AssetSetTmp.toBeSaved())
						AssetSetTmp.save();
				} else {
					NBSMboImp.setValue("description", "更換電表狀態非在庫!!", 11L);
					return false;
				}
				check_flag = doUpdDataACT(new_meterid, NBSMboImp);
				// return check_flag;

			} else { // -----------meter not exist and move the related data to new custno
				MboRemote newAssetMbo = AssetSetTmp.add();
				newAssetMbo.setValue("ASSETNUM", new_meterid, 11L);
				newAssetMbo.setValue("SITEID", new_custno.substring(0, 2), 11L);
				newAssetMbo.setValue("NEWSITE", new_custno.substring(0, 2), 11L);
				// AMI_Insert.save();
				if (UpdateLocation != null && UpdateLocation != "") {
					((Asset) newAssetMbo).moveAssetWithinNonInventory(UpdateLocation, "NBS Update Meter!!", mxServer.getDate(),
							"MAXADMIN", newAssetMbo.getString("wonum"), newAssetMbo.getString("newparent"), true, true, true);
				} else {
					myLogger.debug("NBS do updateact the source meter not have location!!");
				}

				// newLocation.setValue("LOCATION", valData[0].getData(), 11L);
				// -------------------------------------
				newAssetMbo.setValue("ZZ_CUSTNO", new_custno, 11L); // v1.5
				newAssetMbo.setValue("DESCRIPTION", new_meterid.substring(2, new_meterid.length()), 11L);
				newAssetMbo.setValue("STATUS", "NOCOMM", 11L);
				newAssetMbo.setValue("ZZ_PSTATUS", "OPERATE", 11L); // V1.2
				newAssetMbo.setValue("SADDRESSCODE", UpdataSaddresscode, 11L); // V1.7
				MboSetRemote Mboset_CLASSSTRUCTURE = mxServer.getMboSet("CLASSSTRUCTURE", ui); // V1.7
				Mboset_CLASSSTRUCTURE.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
				Mboset_CLASSSTRUCTURE.reset();
				String Classifty_ID = null;
				if (!Mboset_CLASSSTRUCTURE.isEmpty()) {
					MboRemote Mbo_Classstructure = Mboset_CLASSSTRUCTURE.getMbo(0);
					Classifty_ID = Mbo_Classstructure.getString("CLASSSTRUCTUREID");
				}

				if (!Classifty_ID.equals(""))
					newAssetMbo.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);
				newAssetMbo.setValue("ZZ_INSTALLDATE", NBSMboImp.getDate("updatedate"), 11L);
				// newLocation.setValue("ZZ_INSTALLDATE", valData[4].getDataAsDate(), 11L);
				if (AssetSetTmp.toBeSaved())
					AssetSetTmp.save();
				MboSetRemote classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
				classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
				classspecSet.reset();
				if (!classspecSet.isEmpty()) {
					MboSetRemote newAssetspecSet = mxServer.getMboSet("ASSETSPEC", MXServer.getMXServer().getSystemUserInfo());
					for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
						// newAssetspecSet.setWhere("UPPER(assetnum) = UPPER('" + valData[2].getData() + "')" +
						// " AND UPPER(assetattrid) = UPPER('" + assetSpec.getString("assetattrid") + "')");
						//
						// newAssetspecSet.reset();
						// if (newAssetspecSet.isEmpty()) {
						SpecificationMboRemote assetspec = (SpecificationMboRemote) newAssetspecSet.addAtEnd();
						assetspec.addDetailInfor(newAssetMbo, assetSpec);
						assetspec.setValue("SITEID", new_custno.substring(0, 2), 11L);
						// }
					}
					if (newAssetspecSet.toBeSaved()) {
						newAssetspecSet.save();
					}
				}

				newMeterId = newAssetMbo.getString("assetnum");
				newMeterUuid = newAssetMbo.getString("zz_uuid");
				check_flag = doUpdDataACT(new_meterid, NBSMboImp);
				// return check_flag;

			}
			// ----------------------------------------------------------
			if (check_flag == false)
				return false;

			if (Find_Old_meter == true) {
				if (setWhereClause.length() > 0)
					setWhereClause.delete(0, setWhereClause.length());
				setWhereClause.append("UPPER(ASSETNUM)=UPPER('");
				setWhereClause.append(oldMeterId);
				setWhereClause.append("')");
				AssetSetTmp.setWhere(setWhereClause.toString());
				AssetSetTmp.reset();
				if (!AssetSetTmp.isEmpty()) {
					if (memoClause.length() > 0)
						memoClause.delete(0, memoClause.length());
					memoClause.append("amichangeby,");
					memoClause.append(oldMeterUuid);
					memoClause.append(",");
					memoClause.append(newMeterId);
					memoClause.append(",");
					memoClause.append(newMeterUuid);
					getMboData = AssetSetTmp.getMbo(0);
					((Asset) getMboData).changeStatus("INSTOCK", mxServer.getDate(), "NBS Remove Meter to instock");
					((Asset) getMboData).moveAssetWithinNonInventory("LOCATION_INV", memoClause.toString(),
								mxServer.getDate(), "MAXADMIN", getMboData.getString("wonum"), getMboData.getString("newparent"), true, true, true);

					if (AssetSetTmp.toBeSaved())
						AssetSetTmp.save();

				}

			}

		} catch (Exception e) {
			NBSMboImp.setValue("description", e.getMessage(), 11L);
			myLogger.debug("Save nbs Err:" + e.getMessage());

			NBSMboImp.setValue("description", e.getMessage(), 11L);
			return false;
		}
		// return true;
		return true;
	}

	protected static boolean crateXml_multi(String PostDes, Hashtable<String, Hashtable<String, String>> Use_XML_Data, Hashtable<String, Integer> type_cnt, int typeIndex)
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
				tag_device_sub_tmp.appendChild(document.createTextNode("TRUE"));
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
				tag_device_sub_tmp.appendChild(document.createTextNode("TRUE"));
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

	public static void SaveNBSData(String PostDes, Boolean Exe_MDMS_Flg) throws ServletException, IOException {
		HttpURLConnection conn = null;
		// StringBuilder response = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		StringBuilder messageClause = new StringBuilder();
		Hashtable<String, String> nbs_act_des;
		Hashtable<String, String> nbsAmiType = new Hashtable<String, String>();
		Hashtable<Integer, Hashtable<String, String>> nbsTmpData = new Hashtable<Integer, Hashtable<String, String>>();
		Hashtable<Integer, Hashtable<String, String>> amiTmpData = new Hashtable<Integer, Hashtable<String, String>>();

		Hashtable<String, Hashtable<String, String>> nbsActData = new Hashtable<String, Hashtable<String, String>>();
		String[] Getdata = null;
		Object[] param = new Object[2];
		String[] get_err_mes = null;
		String errorMessage = "";
		boolean checkSendXmlFlag = false;
		mxServer = MXServer.getMXServer();
		boolean checkActionTypeFlag = false;

		try {
			ui = mxServer.getSystemUserInfo();
		} catch (MXException e1) {

		}
		AssetSet TmpAssetSet = null;
		MboSetRemote tmpNbsSet = null;
	
		MboSetRemote Get_ZZ_AlnDomain = null;
		MboRemote tmpAmiRemote = null;
		nbsAmiType.put("C", "NEW");
		nbsAmiType.put("U", "UPDATE");
		nbsAmiType.put("D", "REMOVE");
		// Hashtable<String, Hashtable<String, String>> NBSValue = new Hashtable<String, Hashtable<String, String>>();
		// String NBS_payload = "{\"data\":[";
		// MboRemote updAsset = null;

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
		Hashtable<String, Integer> checkDataLengthTable = new Hashtable<String, Integer>();
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

		String Act_Type = "";
		Boolean Check_status;
		String tmp_str = "";
		// Arr_Consumer = new ArrayList<String>();
		try {

			assettonbs_st = mxServer.getMboSet("ZZ_ASSETTONBS", ui);
			TmpAssetSet = (AssetSet) mxServer.getMboSet("ASSET", ui);
			tmpNbsSet = mxServer.getMboSet("ZZ_ASSETTONBS_TMP_DATA", ui);


			Get_ZZ_AlnDomain = mxServer.getMboSet("ALNDOMAIN", ui);
			Get_ZZ_AlnDomain.setWhere("DOMAINID='ZZ_ACTTYPE'");
			Get_ZZ_AlnDomain.reset();

			if (!Get_ZZ_AlnDomain.isEmpty()) {//
				for (MboRemote currMbo = Get_ZZ_AlnDomain.moveFirst(); currMbo != null; currMbo = Get_ZZ_AlnDomain.moveNext()) {
					String get_val = currMbo.getString("VALUE");
					String get_des = currMbo.getString("DESCRIPTION");
					String[] split_data = null;
					nbs_act_des = new Hashtable<String, String>();
					if (get_des.indexOf(",") > -1) {
						split_data = get_des.split(",");
						nbs_act_des.put("act_type", split_data[0]);
						nbs_act_des.put("act_desc", split_data[1]);
					} else {
						nbs_act_des.put("act_type", get_des);
						nbs_act_des.put("act_desc", "");
					}

					nbsActData.put(get_val, nbs_act_des);
				}

			}
			// ------------------------------------------------------------------------------------
			tmpNbsSet.setWhere("(UPLSTATUS ='N' or UPLSTATUS is NULL) AND NBS_TYPE='N'");
			tmpNbsSet.reset();
			Date amiUpdateDate;
			boolean checkFindStatus = false;
			boolean checkNBSInsert = false;
			amiUpdateDate = mxServer.getDate();
			int count = 0;
			if (!tmpNbsSet.isEmpty()) {
				count = tmpNbsSet.count();
				for (MboRemote currMbo = tmpNbsSet.moveFirst(); currMbo != null; currMbo = tmpNbsSet.moveNext()) {
					checkActionTypeFlag = false;
					checkFindStatus = false;
					String[] attrs = { "custno", "assetnum", "updatedate", "customeraddress" };
					MboValueData[] valData = currMbo.getMboValueData(attrs);
					checkDataLengthTable.put("custNo", valData[0].getData().length());
					checkDataLengthTable.put("meter", valData[1].getData().length());
					errorMessage = DataLenCheck.apiDataLengthCheck(checkDataLengthTable);

					if (errorMessage.length() > 0) {
						currMbo.setValue("UPLSTATUS", "E");
						Date date = mxServer.getDate();
						currMbo.setValue("UPLTIME", date);
						currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
						currMbo.setValue("DESCRIPTION", errorMessage, 11L);
						continue;
					}

					// ---check finish-------------------------
					if (nbsActData.containsKey(tmpNbsSet.getString("updatetype"))) {
						Act_Type = nbsActData.get(tmpNbsSet.getString("updatetype")).get("act_type");
					} else { // not in the domain
						currMbo.setValue("UPLSTATUS", "P");
						Date date = mxServer.getDate();
						currMbo.setValue("UPLTIME", date);
						currMbo.setValue("MDMSUPLSTATUS", "P", 11L);
						currMbo.setValue("DESCRIPTION", "updatetype not in the mam domain", 11L);
					}
					// ----------------CHECK THE DATA wheather exist in AMI_IMP_DATA
					if (setWhereClause.length() > 0)
						setWhereClause.delete(0, setWhereClause.length());

				

					if (Act_Type.equalsIgnoreCase("C")) { // add new asset; need
						if (setWhereClause.length() > 0)
							setWhereClause.delete(0, setWhereClause.length());
						setWhereClause.append("UPPER(ASSETNUM) = UPPER('");
						setWhereClause.append(tmpNbsSet.getString("assetnum"));
						setWhereClause.append("')");
						TmpAssetSet.setWhere(setWhereClause.toString());
						TmpAssetSet.reset();

						if (!TmpAssetSet.isEmpty()) {
							MboRemote Asset_tmp = TmpAssetSet.getMbo(0);
							// if (Asset_tmp.getString("STATUS").equalsIgnoreCase("INSTOCK")) {
							// CheckImpLocExist(valData[0].getData());
							// Check_status = doAddNBS(TmpAssetSet, currMbo, "Instock");
							// if (Check_status == true) {
							// ServerletUtil.PrepareMDMS_XML_Data(
							// TmpAssetSet.getMbo(0), currMbo, 1, cell_consumer_xml, cell_servicelocation_xml,
							// cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
							// cell_sdp_consumer_xml, xml_type_cnt);
							// currMbo.setValue("UPLSTATUS", "Y");
							// Date date = mxServer.getDate();
							// currMbo.setValue("UPLTIME", date);
							// currMbo.setValue("MDMSUPLSTATUS", "N", 11L);
							//
							// tmpAmiRemote.setValue("NBSSYNCRESULT", "Y", 11L);
							//
							// myLogger.debug("nbs uplstatus done");
							// }
							// } else {// meter data already exist and the status is operate wait?
							// currMbo.setValue("UPLSTATUS", "E");
							// Date date = mxServer.getDate();
							// currMbo.setValue("UPLTIME", date);
							// currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
							// currMbo.setValue("DESCRIPTION", "電表電號已存在", 11L);
							//
							// tmpAmiRemote.setValue("NBSSYNCRESULT", "N", 11L);
							// tmpAmiRemote.setValue("NBSABNORMALDESC", "電表電號已存在", 11L);
							// // myLogger.debug("nbs aseset donothing");
							// }
							checkNBSInsert = doUpdDataACT(valData[1].getData(), currMbo);
							if (checkNBSInsert == true) {
								ServerletUtil.PrepareMDMS_XML_Data(
										TmpAssetSet.getMbo(0), currMbo, 1, cell_consumer_xml, cell_servicelocation_xml,
										cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
										cell_sdp_consumer_xml, xml_type_cnt);
								currMbo.setValue("UPLSTATUS", "Y");
								Date date = mxServer.getDate();
								currMbo.setValue("UPLTIME", date);
								currMbo.setValue("MDMSUPLSTATUS", "N", 11L);

							}

						} else { // not find the meterid in asset
							// CheckImpLocExist(valData[0].getData());
							// Check_status = doAddNBS(TmpAssetSet, currMbo, "New");
							// if (Check_status == true) {
							// ServerletUtil.PrepareMDMS_XML_Data(
							// TmpAssetSet.getMbo(0), currMbo, 1, cell_consumer_xml, cell_servicelocation_xml,
							// cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
							// cell_sdp_consumer_xml, xml_type_cnt);
							// currMbo.setValue("UPLSTATUS", "Y", 11L);
							// Date date = mxServer.getDate();
							// currMbo.setValue("UPLTIME", date, 11L);
							// currMbo.setValue("MDMSUPLSTATUS", "N", 11L);
							// tmpAmiRemote.setValue("NBSSYNCRESULT", "Y", 11L);
							//
							// } else {
							// currMbo.setValue("UPLSTATUS", "E", 11L);
							// currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
							//
							// tmpAmiRemote.setValue("NBSSYNCRESULT", "N", 11L);
							// tmpAmiRemote.setValue("NBSABNORMALDESC", currMbo.getString("DESCRIPTION"), 11L);
							//
							// }

							currMbo.setValue("UPLSTATUS", "E", 11L);
							currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
						}

					} else if (Act_Type.equalsIgnoreCase("U")) {
						// Check_status = doUpdACT(TmpAssetSet, currMbo, tmpAmiRemote.getString("COMP_METER_NUM"));
						//
						// if (Check_status == true) {
						// currMbo.setValue("UPLSTATUS", "Y", 11L);
						// Date date = mxServer.getDate();
						// currMbo.setValue("UPLTIME", date, 11L);
						// currMbo.setValue("MDMSUPLSTATUS", "N", 11L);
						// tmpAmiRemote.setValue("NBSSYNCRESULT", "Y", 11L);
						//
						// ServerletUtil.PrepareMDMS_XML_Data(
						// TmpAssetSet.getMbo(0), currMbo, 1, cell_consumer_xml, cell_servicelocation_xml,
						// cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
						// cell_sdp_consumer_xml, xml_type_cnt);
						//
						// } else {
						// currMbo.setValue("UPLSTATUS", "E", 11L);
						// currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
						// tmpAmiRemote.setValue("NBSSYNCRESULT", "N", 11L);
						// tmpAmiRemote.setValue("NBSABNORMALDESC", currMbo.getString("DESCRIPTION"), 11L);
						// }
						if (setWhereClause.length() > 0)
							setWhereClause.delete(0, setWhereClause.length());
						setWhereClause.append("UPPER(ASSETNUM) = UPPER('");
						setWhereClause.append(tmpNbsSet.getString("assetnum"));
						setWhereClause.append("')");
						TmpAssetSet.setWhere(setWhereClause.toString());
						TmpAssetSet.reset();
						if (!TmpAssetSet.isEmpty()) {
							checkNBSInsert = doUpdDataACT(valData[1].getData(), currMbo);
							if (checkNBSInsert == true) {
								ServerletUtil.PrepareMDMS_XML_Data(
										TmpAssetSet.getMbo(0), currMbo, 0, cell_consumer_xml, cell_servicelocation_xml,
										cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
										cell_sdp_consumer_xml, xml_type_cnt);
								currMbo.setValue("UPLSTATUS", "Y");
								Date date = mxServer.getDate();
								currMbo.setValue("UPLTIME", date);
								currMbo.setValue("MDMSUPLSTATUS", "N", 11L);
							} else {

								currMbo.setValue("UPLSTATUS", "E", 11L);
								currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
							}
						} else {

							currMbo.setValue("UPLSTATUS", "E", 11L);
							currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
						}

					} else if (Act_Type.equalsIgnoreCase("UD")) {

						Check_status = doUpdDataACT(valData[1].getData(), currMbo);
						if (Check_status == true) {
							currMbo.setValue("UPLSTATUS", "Y", 11L);
							Date date = mxServer.getDate();
							currMbo.setValue("UPLTIME", date, 11L);
							currMbo.setValue("MDMSUPLSTATUS", "N", 11L);
							tmpAmiRemote.setValue("NBSSYNCRESULT", "Y", 11L);

							// ----------prepare xml file----------------
							TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('" + StringUtil.emptyStr((String) tmpNbsSet.getString("assetnum")) + "')");
							TmpAssetSet.reset();
							if (!TmpAssetSet.isEmpty()) {
								ServerletUtil.PrepareMDMS_XML_Data(
										TmpAssetSet.getMbo(0), currMbo, 1, cell_consumer_xml, cell_servicelocation_xml,
										cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
										cell_sdp_consumer_xml, xml_type_cnt);
							}

						} else {
							currMbo.setValue("UPLSTATUS", "E", 11L);
							currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
							tmpAmiRemote.setValue("NBSSYNCRESULT", "N", 11L);
							tmpAmiRemote.setValue("NBSABNORMALDESC", currMbo.getString("DESCRIPTION"), 11L);
						}
					} else if (Act_Type.equalsIgnoreCase("D")) {
						TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('" // V1.1
								+ tmpNbsSet.getString("assetnum") + "')"); // V1.1
						TmpAssetSet.reset();
						if (!TmpAssetSet.isEmpty()) {

							MboRemote Asset_tmp = TmpAssetSet.getMbo(0);
							if (!Asset_tmp.getString("status").equalsIgnoreCase("INSTOCK")) {
								ServerletUtil.PrepareMDMS_XML_Data(
										TmpAssetSet.getMbo(0), currMbo, 3, cell_consumer_xml, cell_servicelocation_xml,
										cell_servicepoint_xml, cell_device_xml, cell_sdp_device_xml, cell_sdp_group_xml,
										cell_sdp_consumer_xml, xml_type_cnt);
								((Asset) Asset_tmp).changeStatus("INSTOCK", mxServer.getDate(), "amiremoveby");
								((Asset) Asset_tmp).moveAssetWithinNonInventory("LOCATION_INV", "amiremoveby", mxServer.getDate(), "MAXADMIN", Asset_tmp.getString("wonum"), Asset_tmp.getString("newparent"), true, true, true);

								if (TmpAssetSet.toBeSaved()) {
									TmpAssetSet.save();
									// prepare xml file for delete

									currMbo.setValue("UPLSTATUS", "Y", 11L);
									Date date = mxServer.getDate();
									currMbo.setValue("UPLTIME", date, 11L);
									currMbo.setValue("MDMSUPLSTATUS", "N", 11L);
									tmpAmiRemote.setValue("NBSSYNCRESULT", "Y", 11L);

								}
								MboSetRemote hesRemove = Asset_tmp.getMboSet("ZZ_HESCODEDATA");
								if (!hesRemove.isEmpty()) {
									hesRemove.deleteAll();
									hesRemove.toBeSaved();
									hesRemove.save();
								}
							} else {
								currMbo.setValue("UPLSTATUS", "E");
								Date date = mxServer.getDate();
								currMbo.setValue("UPLTIME", date);
								currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
								currMbo.setValue("DESCRIPTION", "電表狀態為在庫!!", 11L);
								tmpAmiRemote.setValue("NBSSYNCRESULT", "N", 11L);
								tmpAmiRemote.setValue("NBSABNORMALDESC", currMbo.getString("電表狀態為在庫!!"), 11L);

							}
						} else {
							currMbo.setValue("UPLSTATUS", "E");
							Date date = mxServer.getDate();
							currMbo.setValue("UPLTIME", date);
							currMbo.setValue("MDMSUPLSTATUS", "E", 11L);
							currMbo.setValue("DESCRIPTION", "電表電號不存在", 11L);
						}
					} else { // should delete but not find the delete meter id
						// currMbo.setValue("MDMSUPLSTATUS", "O", 11L);
						currMbo.setValue("UPLSTATUS", "P");
						Date date = mxServer.getDate();
						currMbo.setValue("UPLTIME", date);
						currMbo.setValue("MDMSUPLSTATUS", "P", 11L);
						currMbo.setValue("DESCRIPTION", "mam do nothing for this actytype", 11L);
					}
					if (Exe_MDMS_Flg == true) {
						if (Act_Type.equalsIgnoreCase("C"))
							checkSendXmlFlag = crateXml_multi(PostDes, NBS_XML_Data, xml_type_cnt, 0);
						else if (Act_Type.equalsIgnoreCase("U"))
							checkSendXmlFlag = crateXml_multi(PostDes, NBS_XML_Data, xml_type_cnt, 1);
						if (checkSendXmlFlag == false) {
							currMbo.setValue("MDMSUPLSTATUS", "N", 11L);
						} else {
							currMbo.setValue("MDMSUPLSTATUS", "Y", 11L);
						}
					}
				}
			} else {

			}

			if (tmpNbsSet.toBeSaved()) {
				tmpNbsSet.save();


			} else {
				myLogger.debug("nbs not save");
			}

		} catch (Exception e) {

			myLogger.debug("nbs error" + e.getLocalizedMessage());
		}

		// if (Exe_MDMS_Flg == true) {
		// try {
		// tmpNbsSet.setWhere("MDMSUPLSTATUS='N' AND NBS_TYPE='N' ");
		// // Tmp_ZZ_NBS.setWhere("MDMSUPLSTATUS is null");
		// tmpNbsSet.reset();
		// if (!tmpNbsSet.isEmpty()) {
		//
		// crateXml_multi(PostDes, NBS_XML_Data, xml_type_cnt);
		// for (MboRemote currMbo = tmpNbsSet.moveFirst(); currMbo != null; currMbo = tmpNbsSet.moveNext()) {
		// currMbo.setValue("MDMSUPLSTATUS", "Y");
		// currMbo.setValue("DESCRIPTION", "");
		// }
		// if (tmpNbsSet.toBeSaved()) {
		// tmpNbsSet.save();
		//
		// }
		//
		// }
		//
		// // return response.toString();
		//
		// } catch (Exception ex) {
		//				
		// } finally {
		// if (conn != null) {
		// conn.disconnect();
		// }
		// }
		// }
		// return "Done";

	}

}
