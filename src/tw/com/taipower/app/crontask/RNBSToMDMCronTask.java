/*---------------------------  Modification history :---------------------------*/
/* Date        Version Author         Note                                      */
/* ----------  ------- -------------  ------------------------------------------*/
/* 2019-03-20   V1.0   Davis Wang     Initial Release                           */
/* 2019-04-24   V1.1   Davis Wang     Edit Update Rule                          */
package tw.com.taipower.app.crontask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import javax.servlet.ServletException;

import psdi.app.assetcatalog.SpecificationMboRemote;
import psdi.app.location.LocationSet;
import psdi.app.system.CrontaskParamInfo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValueData;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.server.SimpleCronTask;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import tw.com.taipower.app.asset.Asset;
import tw.com.taipower.app.asset.AssetSet;
import tw.com.taipower.app.util.StringUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.tivoli.maximo.srvad.app.ServiceAddressSet;

public class RNBSToMDMCronTask extends SimpleCronTask {
	static MXLogger						myLogger			= MXLoggerFactory
																	.getLogger("maximo.sql.LOCATION.LOCATIONS");
	static String[]						NBS_NAME			= { "custNo", "customername", "backusuallycontract", "backnonesummercontract",
															"backsaturdayhalfpeakcontract", "backoffpeakcontract", "usuallycontract", "nonesummercontract",
															"offpeakcontract", "saturdayhalfpeakcontract", "contracttype", "meterlayer", "updatedate", "updatetype",
															"meterid", "group", "metertimes", "taxid", "industclass", "section", "billingDate" };
	// static String[] ZZ_ASSET_NAME = { "LOCATION", "zz_customername",
	// "zz_backusuallycontract", "zz_nonesummercontract",
	// "zz_backsaturdayhalfpeakcontract", "zz_backoffpeakcontract",
	// "zz_usuallycontract", "zz_nonesummercontract",
	// "zz_offpeakcontract", "zz_saturdayhalfpeakcontract", "zz_contract_kind",
	// "zz_meterbatch", "zz_updatedate",
	// "zz_group", "zz_metertimes", "zz_taxid", "zz_industclass", "zz_sector",
	// "zz_billingDate" };

	static String[]						ZZ_ASSET_NAME		= { "zz_customername", "zz_backusuallycontract", "zz_nonesummercontract",
															"zz_backsaturdayhalfpeakcontract", "zz_backoffpeakcontract", "zz_usuallycontract", "zz_nonesummercontract",
															"zz_offpeakcontract", "zz_saturdayhalfpeakcontract", "zz_contract_kind", "zz_meterbatch", "zz_updatedate", "zz_updatetype",
															"zz_group", "zz_metertimes", "zz_taxid", "zz_industclass", "zz_sector", "zz_billingDate" };

	// use for data insert
	static String[]						ZZ_Tmp_NBS_Insert	= { "customername", "backusuallycontract", "backnonesummercontract",
															"backsaturdayhalfpeakcontract", "backoffpeakcontract", "usuallycontract", "nonesummercontract",
															"offpeakcontract", "saturdayhalfpeakcontract", "contracttype", "meterlayer", "updatedate", "updatetype",
															"zz_group", "metertimes", "taxid", "industclass", "section", "billingDate", "uplstatus", "upltime", "uplresult" };
	// use for data post
	static String[]						ZZ_Tmp_NBS_Post		= { "custno", "customername", "backusuallycontract", "backnonesummercontract",
															"backsaturdayhalfpeakcontract", "backoffpeakcontract", "usuallycontract", "nonesummercontract",
															"offpeakcontract", "saturdayhalfpeakcontract", "contracttype", "meterlayer", "updatedate", "updatetype",
															"meterid", "zz_group", "metertimes", "taxid", "industclass", "section", "billingDate" };
	public static MXServer				mxServer;
	public static UserInfo				ui;
	public static String				Location_Insert;
	public static MboSetRemote			AMI_Insert			= null;
	private static CrontaskParamInfo[]	params;
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
		String tmp_ser = "";
		String tmp_data = "";
		Location_Insert = "";
		String tmp_mes = "";
		try {
			if (!AMI_Insert.isEmpty()) {
				// tmp_count = rs.getInt(1);
				tmp_count = AMI_Insert.count();
				tmp_ser = String.format("%04d", tmp_count + 1);
				tmp_data = CustNo + "-" + tmp_ser + "-P";
				Location_Insert = CustNo + "-" + tmp_ser;
				AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ");
				AMI_Service_Insert.reset();
				if (AMI_Service_Insert.isEmpty()) {
					MboRemote newService = AMI_Service_Insert.add();
					newService.setValue("ADDRESSCODE", tmp_data, 11L);
					newService.setValue("LANGCODE", "ZHT", 11L);
					newService.setValue("ORGID", "TPC", 11L);
					newService.setValue("SITEID", CustNo.substring(0, 2), 11L);
					AMI_Service_Insert.save();
				}

				myLogger.debug("Check how many meter install under elect_num");
			} else { // add new custno
				tmp_count = 0;
				tmp_ser = String.format("%04d", tmp_count + 1);
				tmp_data = CustNo + "-" + tmp_ser + "-P";
				Location_Insert = CustNo + "-" + tmp_ser;
				tmp_mes = "UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ";
				AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ");
				AMI_Service_Insert.reset();
				if (AMI_Service_Insert.isEmpty()) {
					MboRemote newService = AMI_Service_Insert.add();
					newService.setValue("ADDRESSCODE", tmp_data, 11L);
					newService.setValue("LANGCODE", "ZHT", 11L);
					newService.setValue("ORGID", "TPC", 11L);
					newService.setValue("SITEID", CustNo.substring(0, 2), 11L);
					AMI_Service_Insert.save();
				}
			}
		} catch (Exception e) {
		
			myLogger.debug("Create Service Err:" + e.getMessage());
		}

		locationsSet.setWhere("UPPER(LOCATION) = UPPER('" + Location_Insert + "')");

		try {
			if (!locationsSet.isEmpty()) { // Location exist

			} else {
				MboRemote newLocation = locationsSet.add();
				newLocation.setValue("LOCATION", Location_Insert, 11L);
				newLocation.setValue("SITEID", CustNo.substring(0, 2), 11L);

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

	private static Boolean doUpdACT(MboSetRemote AssetSetTmp, MboRemote NBSMboImp) throws RemoteException, MXException, Exception {
		// String[] attrs = { "custno", "meterid", "updatedate", };
		// MboValueData[] valData = NBSMboImp.getMboValueData(attrs);
		//		
		// AMI_Insert.setWhere("UPPER(ASSETNUM) = UPPER('"+ valData[1] + "')");
		// AMI_Insert.reset();

		// MboRemote updAsset = AMI_Insert.getMbo(0);
		MboRemote updAsset = AssetSetTmp.getMbo(0);
		for (int i = 0; i < ZZ_ASSET_NAME.length; i++) {
			if (ZZ_ASSET_NAME[i].equalsIgnoreCase("zz_updatedate") || ZZ_ASSET_NAME[i].equalsIgnoreCase("zz_billingDate"))
				updAsset.setValue(ZZ_ASSET_NAME[i], NBSMboImp.getDate(ZZ_Tmp_NBS_Insert[i]));
			else
				updAsset.setValue(ZZ_ASSET_NAME[i], NBSMboImp.getString(ZZ_Tmp_NBS_Insert[i]));
		}
		try {
			AssetSetTmp.save(); // save asset
			// NBSMboImp.setValue("UPLSTATUS", "Y");
			// Date date = mxServer.getDate();
			// NBSMboImp.setValue("UPLTIME", date);
			return true;
		} catch (Exception e) {
			// NBSMboImp.setValue("UPLSTATUS", "N");
			// NBSMboImp.setValue("MDMS_UPLSTATUS", "N");
			return false;
		}

	}

	private static Boolean doAddInstock(MboSetRemote AssetSetTmp, MboRemote NBSMboImp)
			throws RemoteException, MXException, Exception {

		String[] attrs = { "custno", "meterid", "updatedate", };
		MboValueData[] valData = NBSMboImp.getMboValueData(attrs);

		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		String Classifty_ID = "";
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		try {
			MboRemote newAsset = AssetSetTmp.getMbo(0);
			((Asset) newAsset).changeStatus("OPERATE", mxServer.getDate(), "NBS New Meter from status instock");
			((Asset) newAsset).moveAssetWithinNonInventory(Location_Insert, "NBS New Meter from status instock", mxServer.getDate(), "MAXADMIN", newAsset.getString("wonum"), newAsset.getString("newparent"), true, true, true);

			// String strTimeFormat = "yyyy-MM-dd HH:mm:ss"; // Davis Differ
			// String strTimeFormat1 = "yyyy/MM/dd HH:mm:ss";

			Date parseDate = (valData[2].getDataAsDate());

			// newAsset.setValue("LOCATION", Location_Insert, 11L);
			newAsset.setValue("ZZ_CUSTNO", valData[0].getData(), 11L); // v1.5
			newAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
			// newAsset.setValue("STATUS", "OPERATE", 11L);
			newAsset.setValue("ZZ_PSTATUS", "OPERATING", 11L); // V1.2

			if (!Classifty_ID.equals(""))
				newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);

			newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);
			MboSetRemote classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
			classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");

			for (int i = 0; i < ZZ_ASSET_NAME.length; i++) {
				if (ZZ_ASSET_NAME[i].equalsIgnoreCase("zz_updatedate") || ZZ_ASSET_NAME[i].equalsIgnoreCase("zz_billingDate"))
					newAsset.setValue(ZZ_ASSET_NAME[i], NBSMboImp.getDate(ZZ_Tmp_NBS_Insert[i]));
				else
					newAsset.setValue(ZZ_ASSET_NAME[i], NBSMboImp.getString(ZZ_Tmp_NBS_Insert[i]));
			}
			AssetSetTmp.save();

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
			return true;

		} catch (Exception e) {
			myLogger.debug("ami error:" + e.getLocalizedMessage());
	
			return false;
		}
	}

	private static Boolean doAddACT(MboSetRemote AssetSetTmp, MboRemote NBSMboImp)
			throws RemoteException, MXException, Exception {

		String[] attrs = { "custno", "meterid", "updatedate", };
		MboValueData[] valData = NBSMboImp.getMboValueData(attrs);

		// AMI_Insert.setWhere("UPPER(LOCATION) = UPPER('"
		// + Inser_Loc + "')" + " AND UPPER(ASSETNUM) = UPPER('"
		// + meter_id + "')");
		// AMI_Insert.reset();
		// String saSQL = "select CLASSSTRUCTUREID from CLASSSTRUCTURE WHERE "
		// + "UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')";
		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		String Classifty_ID = "";
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		try {

			MboRemote newAsset = AssetSetTmp.add();
			// String strTimeFormat = "yyyy-MM-dd HH:mm:ss"; // Davis Differ
			String strTimeFormat1 = "yyyy/MM/dd HH:mm:ss";
			Date parseDate = (valData[2].getDataAsDate());
			newAsset.setValue("ASSETNUM", valData[1].getData(), 11L);
			newAsset.setValue("LOCATION", Location_Insert, 11L);
			newAsset.setValue("ZZ_CUSTNO", valData[0].getData(), 11L); // v1.5
			newAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
			newAsset.setValue("STATUS", "OPERATE", 11L);
			newAsset.setValue("ZZ_PSTATUS", "OPERATING", 11L); // V1.2

			if (!Classifty_ID.equals(""))
				newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);

			newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);
			MboSetRemote classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
			classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");

			for (int i = 0; i < ZZ_ASSET_NAME.length; i++) {
				if (ZZ_ASSET_NAME[i].equalsIgnoreCase("zz_updatedate") || ZZ_ASSET_NAME[i].equalsIgnoreCase("zz_billingDate"))
					newAsset.setValue(ZZ_ASSET_NAME[i], NBSMboImp.getDate(ZZ_Tmp_NBS_Insert[i]));
				else
					newAsset.setValue(ZZ_ASSET_NAME[i], NBSMboImp.getString(ZZ_Tmp_NBS_Insert[i]));
			}
			AssetSetTmp.save();
			if (!classspecSet.isEmpty()) {
				MboSetRemote newAssetspecSet = mxServer.getMboSet("ASSETSPEC", MXServer.getMXServer().getSystemUserInfo());
				for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
					SpecificationMboRemote assetspec = (SpecificationMboRemote) newAssetspecSet.addAtEnd();
					assetspec.addDetailInfor(AssetSetTmp.getMbo(0), assetSpec);
					assetspec.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				}
				if (newAssetspecSet.toBeSaved()) {
					newAssetspecSet.save();
				}
			}
			return true;

		} catch (Exception e) {
			myLogger.debug("ami error:" + e.getLocalizedMessage());
		
			return false;
		}
	}

	@Override public void cronAction() {
		// Your custom code here. e.g. A sample code is below.

		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			// System.out.println("MyFirstCrontaskRTING CRONTASK");
			myLogger.debug("Cron task MAM Update RNBS Data to SOA");
			// nickall.asuscomm.com:
			// httpConnectionPost("http://127.0.0.1:1880/NBS_TST");
			Boolean isExeMDMS = getParamAsBoolean("MDMS_EXECUTE");
			// httpConnectionPost("http://nickall.asuscomm.com:1880/NBS_TST",
			// isExeMDMS);
			httpConnectionPost("http://192.168.34.22/maximo/NBSShow", isExeMDMS);
			// End Sample Code
		} catch (Exception e) {
			
		}
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	public static String httpConnectionPost(String apiUrl, Boolean Exe_MDMS_Flg) throws ServletException, IOException {
		HttpURLConnection conn = null;
		StringBuilder response = new StringBuilder();

		mxServer = MXServer.getMXServer();

		try {
			ui = mxServer.getSystemUserInfo();
		} catch (MXException e1) {
			// TODO Auto-generated catch block
		
		}
		AssetSet TmpAssetSet = null;
		MboSetRemote Tmp_ZZ_NBS = null;
		MboSetRemote Get_ZZ_AlnDomain = null;
		Hashtable<String, Hashtable<String, String>> NBSValue = new Hashtable<String, Hashtable<String, String>>();
		String NBS_payload = "{\"data\":[";

		String[][] NBS_Rule = null;
		int total_rule_cnt = 0;
		int tmp_updtype;
		int up_bound;
		int low_bound;
		String Act_Type = "";
		Boolean Check_status;
		try {
			TmpAssetSet = (AssetSet) mxServer.getMboSet("ASSET", ui);
			Tmp_ZZ_NBS = mxServer.getMboSet("ZZ_NBS_IMP_DATA", ui);
			Tmp_ZZ_NBS.setWhere("UPLSTATUS ='N' or UPLSTATUS is NULL");
			Get_ZZ_AlnDomain = mxServer.getMboSet("ALNDOMAIN", ui);
			Get_ZZ_AlnDomain.setWhere("DOMAINID='ZZ_NBS_RULE'");
			Get_ZZ_AlnDomain.reset();
			if (!Get_ZZ_AlnDomain.isEmpty()) {
				int rule_cnt = Get_ZZ_AlnDomain.count();

				NBS_Rule = new String[rule_cnt][3];
				for (MboRemote currMbo = Get_ZZ_AlnDomain.moveFirst(); currMbo != null; currMbo = Get_ZZ_AlnDomain.moveNext()) {
					String[] Tmp_str = currMbo.getString("VALUE").split(";");
					for (int i = 0; i < 3; i++) {
						NBS_Rule[total_rule_cnt][i] = Tmp_str[i];
					}
					total_rule_cnt = total_rule_cnt + 1;
				}
			}
			Tmp_ZZ_NBS.reset();
			if (!Tmp_ZZ_NBS.isEmpty()) {
				// MboValueData[] valData =
				// Tmp_ZZ_NBS.getMboValueData(ZZ_Tmp_NBS_NAME);
				for (MboRemote currMbo = Tmp_ZZ_NBS.moveFirst(); currMbo != null; currMbo = Tmp_ZZ_NBS.moveNext()) {
					// --Check Updatetype First--
					for (int rule_cnt = 0; rule_cnt < total_rule_cnt; rule_cnt++) {
						tmp_updtype = Integer.valueOf(Tmp_ZZ_NBS.getString("updatetype"));
						low_bound = Integer.valueOf(NBS_Rule[rule_cnt][0]);
						up_bound = Integer.valueOf(NBS_Rule[rule_cnt][1]);
						if (low_bound < tmp_updtype && tmp_updtype <= up_bound) {
							Act_Type = NBS_Rule[rule_cnt][2];
							break;
						}
					}

					if (Act_Type.equalsIgnoreCase("C")) { // add new asset; need
						// to check status
						// wheather instock
						TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('"
								+ StringUtil.emptyStr((String) Tmp_ZZ_NBS.getString("meterid")) + "')");
						TmpAssetSet.reset();
						String[] attrs = { "custno", "meterid", "updatedate", };
						MboValueData[] valData = currMbo.getMboValueData(attrs);

						if (!TmpAssetSet.isEmpty()) {

							MboRemote Asset_tmp = TmpAssetSet.getMbo(0);
							if (Asset_tmp.getString("STATUS").equalsIgnoreCase("INSTOCK")) {

								CheckImpLocExist(valData[0].getData());
								Check_status = doAddInstock(TmpAssetSet, currMbo);

								if (Check_status == true) {
									currMbo.setValue("UPLSTATUS", "Y");
									Date date = mxServer.getDate();
									currMbo.setValue("UPLTIME", date);
									currMbo.setValue("MDMS_UPLSTATUS", "N", 11L);

								}

							} else {// meter data already exist and the status // is not instock
								// currMbo.setValue("UPLSTATUS", "D");
								currMbo.setValue("DESCRIPTION", "Meter status is operate", 11L);
								currMbo.setValue("UPLSTATUS", "E", 11L);
								currMbo.setValue("MDMS_UPLSTATUS", "E", 11L);
							}

						} else {
							// updAsset = TmpAssetSet.add();

							CheckImpLocExist(valData[0].getData());
							Check_status = doAddACT(TmpAssetSet, currMbo);
							if (Check_status == true) {
								currMbo.setValue("UPLSTATUS", "Y", 11L);
								Date date = mxServer.getDate();
								currMbo.setValue("UPLTIME", date, 11L);
								currMbo.setValue("MDMS_UPLSTATUS", "N", 11L);
							} else {
								currMbo.setValue("UPLSTATUS", "E", 11L);
								currMbo.setValue("MDMS_UPLSTATUS", "E", 11L);
							}
						}
					} else if (Act_Type.equalsIgnoreCase("U")) {

						TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('"
								+ StringUtil.emptyStr((String) Tmp_ZZ_NBS.getString("meterid")) + "')" // V1.1
								+ "AND UPPER(STATUS) <> UPPER('INSTOCK')"); // V1.1
						TmpAssetSet.reset();
						if (!TmpAssetSet.isEmpty()) { // start insert data and

							Check_status = doUpdACT(TmpAssetSet, currMbo);
							if (Check_status == true) {
								currMbo.setValue("UPLSTATUS", "Y", 11L);
								Date date = mxServer.getDate();
								currMbo.setValue("UPLTIME", date, 11L);
								currMbo.setValue("MDMS_UPLSTATUS", "N", 11L);
							} else {
								currMbo.setValue("UPLSTATUS", "E", 11L);
								currMbo.setValue("MDMS_UPLSTATUS", "E", 11L);
							}

						} else {
							myLogger.debug("NBS Crontask not find data assetnum" + Tmp_ZZ_NBS.getString("meterid"));
							currMbo.setValue("DESCRIPTION", "can not find meterid and custno");
						}
					} else if (Act_Type.equalsIgnoreCase("D")) {
						// TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('" //V1.1
						// + StringUtil.emptyStr((String) Tmp_ZZ_NBS.getString("meterid")) + "')"); //V1.1
						TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('" // V1.1
								+ StringUtil.emptyStr((String) Tmp_ZZ_NBS.getString("meterid")) + "')" + // V1.1
								" AND UPPER(STATUS) <> 'INSTOCK'"); // V1.1
						TmpAssetSet.reset();
						if (!TmpAssetSet.isEmpty()) {
							// String[] attrs = { "custno", "meterid",
							// "updatedate", };
							// MboValueData[] valData =
							// currMbo.getMboValueData(attrs);
							MboRemote Asset_tmp = TmpAssetSet.getMbo(0);
							// Asset_tmp.setValue("SITEID", "09",11L);
							((Asset) Asset_tmp).changeStatus("INSTOCK", mxServer.getDate(), "NBS Remove Meter from status instock");
							((Asset) Asset_tmp).moveAssetWithinNonInventory("LOCATION_INV", "NBS Remove Meter from status instock", mxServer.getDate(), "MAXADMIN", Asset_tmp.getString("wonum"), Asset_tmp.getString("newparent"), true, true, true);

							if (TmpAssetSet.toBeSaved()) {
								TmpAssetSet.save();
								currMbo.setValue("UPLSTATUS", "Y", 11L);
								Date date = mxServer.getDate();
								currMbo.setValue("UPLTIME", date, 11L);
								currMbo.setValue("MDMS_UPLSTATUS", "N", 11L);
							}
						} else {
							// currMbo.setValue("UPLSTATUS", "N",11L);

						}
					} else { // should delete but not find the delete meter id
						currMbo.setValue("MDMS_UPLSTATUS", "O", 11L);

					}
				}
			} else {

			}

			if (Tmp_ZZ_NBS.toBeSaved()) {
				Tmp_ZZ_NBS.save();
			}
			// NBS_payload = NBS_payload.substring(0, NBS_payload.length() - 1);
			// NBS_payload = NBS_payload + "]}";
		} catch (Exception e) {
			
		}

		try {
			Tmp_ZZ_NBS.setWhere("MDMS_UPLSTATUS='N'");
			Tmp_ZZ_NBS.reset();
			if (!Tmp_ZZ_NBS.isEmpty()) {
				for (MboRemote currMbo = Tmp_ZZ_NBS.moveFirst(); currMbo != null; currMbo = Tmp_ZZ_NBS.moveNext()) {
					TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('"
							+ StringUtil.emptyStr((String) Tmp_ZZ_NBS.getString("meterid"))
							+ "')");
					TmpAssetSet.reset();
					if (!TmpAssetSet.isEmpty()) { // start insert data and
						NBS_payload = NBS_payload + "{";
						for (int i = 0; i < NBS_NAME.length; i++) {
							if (i == NBS_NAME.length - 1) {
								NBS_payload = NBS_payload + "\"" + NBS_NAME[i] + "\":\"" +
										currMbo.getString(ZZ_Tmp_NBS_Post[i]) + "\"},";
							} else {
								NBS_payload = NBS_payload + "\"" + NBS_NAME[i] + "\":\"" +
										currMbo.getString(ZZ_Tmp_NBS_Post[i]) + "\",";
							}
						}
					}
				}
				NBS_payload = NBS_payload.substring(0, NBS_payload.length() - 1);
				NBS_payload = NBS_payload + "]}";

				try {
					// URL url = new
					// URL("http://nickall.asuscomm.com:1880/NBS_TST");
					// URL url = new URL("http://vm-win2012r2/maximo/NBSShow");
					URL url = new URL("http://vm-win2012r2:1880/NBS_TST");
					// URL url = new URL("http://192.168.34.22/maximo/NBSShow");
					myLogger.debug("start post url");
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestProperty("Content-Type", "application/json");
					conn.setRequestMethod("POST");
					conn.setConnectTimeout(10000);
					conn.setReadTimeout(10000);
					conn.setDoInput(true); // 允許輸入流，即允許下載
					conn.setDoOutput(true); // 允許輸出流，即允許上傳
					conn.setUseCaches(false); // 設置是否使用緩存

					OutputStream os = conn.getOutputStream();
					myLogger.debug("Send post url");
					DataOutputStream writer = new DataOutputStream(os);
					JsonObject obj = new
							JsonParser().parse(NBS_payload.toString()).getAsJsonObject();
					writer.write(obj.toString().getBytes("UTf-8"));
					writer.flush();
					writer.close();
					os.close();

					// Get Response
					InputStream is = conn.getInputStream();
					BufferedReader reader = new BufferedReader(new
							InputStreamReader(is));
					String line;

					myLogger.debug("get post result");
					while ((line = reader.readLine()) != null) {
						response.append(line);
						response.append('\r');
					}
					reader.close();
					// Tmp_ZZ_NBS.setWhere("MDMS_UPLSTATUS='N'");
					// Tmp_ZZ_NBS.reset();
					for (MboRemote currMbo = Tmp_ZZ_NBS.moveFirst(); currMbo != null; currMbo = Tmp_ZZ_NBS.moveNext()) {
						// TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('"
						// + StringUtil.emptyStr((String)
						// Tmp_ZZ_NBS.getString("meterid"))
						// + "')");
						// TmpAssetSet.reset();
						// if (!TmpAssetSet.isEmpty()) {
						currMbo.setValue("MDMS_UPLSTATUS", "Y");
						currMbo.setValue("DESCRIPTION", "");
						// }
					}
					if (Tmp_ZZ_NBS.toBeSaved())
						Tmp_ZZ_NBS.save();
					// return response.toString();
				} catch (Exception ex) { // post fail!!
					// Tmp_ZZ_NBS.setWhere("MDMS_UPLSTATUS='N'");
					// Tmp_ZZ_NBS.reset();
					for (MboRemote currMbo = Tmp_ZZ_NBS.moveFirst(); currMbo != null; currMbo = Tmp_ZZ_NBS.moveNext()) {
						// TmpAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('"
						// + StringUtil.emptyStr((String)
						// Tmp_ZZ_NBS.getString("meterid"))+"') AND "
						// + "') AND " + "UPPER(LOCATION) = UPPER('"
						// + StringUtil.emptyStr((String)
						// Tmp_ZZ_NBS.getString("custno"))
						// + "')");
						// TmpAssetSet.reset();
						// if (!TmpAssetSet.isEmpty()) {
						currMbo.setValue("DESCRIPTION", "MDMS Upload Fall");
						// }
					}
					if (Tmp_ZZ_NBS.toBeSaved())
						Tmp_ZZ_NBS.save();

				}

			}

			// return response.toString();

		} catch (Exception ex) {
		
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return "Done";

	}
}
