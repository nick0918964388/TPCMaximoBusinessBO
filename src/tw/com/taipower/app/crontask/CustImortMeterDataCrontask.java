package tw.com.taipower.app.crontask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.ibm.tivoli.maximo.srvad.app.ServiceAddressSet;

import psdi.app.assetcatalog.SpecificationMboRemote;
import psdi.app.location.LocationSet;
import psdi.app.system.CrontaskParamInfo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValueData;
import psdi.security.ConnectionKey;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.server.SimpleCronTask;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import tw.com.taipower.app.asset.Asset;
import tw.com.taipower.app.asset.AssetSet;

public class CustImortMeterDataCrontask extends SimpleCronTask {
	static MXLogger						myLogger					= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");
	static String[]						EXCEL_COL_NAME_Air_Meter	= { "電號", "區處代碼", "區處中文", "村里代碼", "縣市",
																	"鄉鎮區", "村里", "圖號座標", "X(67)", "Y(67)", "饋線代號" };
	static String[]						EXCEL_COL_NAME_Under_Meter	= { "電號", "區處代碼", "區處中文", "縣市",
																	"鄉鎮區", "村里", "圖號座標組別", "X(67)", "Y(67)", "饋線代號" };

	static String						Location_Insert;
	static String						Premise_Insert;
	public static LocationSet			locationsSet;
	public static MboSetRemote			AMI_Service_Insert			= null;
	public MboSetRemote					AMIInsert					= null;
	public MboSetRemote					ASSET_NBS_Insert			= null;
	public static MboSetRemote			AssetspecSet				= null;
	public static MboSetRemote			ZZ_Transformer_Insert		= null;
	public static MXServer				mxServer;
	public static UserInfo				ui;
	public ConnectionKey				conKey;
	public static String				Classifty_ID;
	public static MboSetRemote			classspecSet				= null;
	static double						a							= 6378137.0;
	static double						b							= 6356752.314245;
	static double						lon0						= 121 * Math.PI / 180;
	static double						k0							= 0.9999;
	static int							dx							= 250000;
	static double						XY67_A						= 0.00001549;
	static double						XY67_B						= 0.000006521;
	static String[]						Loc_use;
	static String[]						SDP_Use;
	static int							commit_cnt;
	static String						LOG_PATH;
	int									dostartindex;
	int									doendindex;
	private static CrontaskParamInfo[]	params;
	static {
		params = new CrontaskParamInfo[4];
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
		params[3].setDefault("150000");
	}

	@Override public void cronAction() {
		// Your custom code here. e.g. A sample code is below.

		try {
			// mxServer = MXServer.getMXServer();
			// ui = mxServer.getSystemUserInfo();
			myLogger.debug("Cron task Upload Meter Data Start!!");
			String LoadFilePath = getParamAsString("LOADFILE");
			dostartindex = Integer.parseInt(getParamAsString("StartIndex"));
			doendindex = Integer.parseInt(getParamAsString("EndIndex"));

			LOG_PATH = LoadFilePath.replace(".csv", "_log.csv");
			boolean checksatus = false;
			Hashtable<String, Hashtable<String, String>> GetData = new Hashtable<String, Hashtable<String, String>>();
			// GetData=ReadMeterData("D:\\CP2_Data\\103_架空.csv");
			GetData = ReadMeterData(LoadFilePath);
			checksatus = InsertMeterData(GetData);

		} catch (Exception e) {
			
		}
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	public static void ClearData() throws RemoteException, MXException, Exception {
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		MboSetRemote asset_del = null;
		asset_del = (AssetSet) mxServer.getMboSet("ASSET", ui);
		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		asset_del.setWhere("zz_custno like '04%'");
		asset_del.reset();
		String Location_tmp;
		String Sdp_tmp;
		String asset_tmp;
		int get_index = 0;
		// MboRemote tmp_del = null;
		try {
			if (!asset_del.isEmpty()) {
				for (MboRemote tmp_del = asset_del.moveFirst(); tmp_del != null; tmp_del = asset_del.moveNext()) {
					// while (asset_del.getMbo(get_index) != null) {
					tmp_del = asset_del.getMbo(0);
					Location_tmp = tmp_del.getString("location");
					Sdp_tmp = tmp_del.getString("SADDRESSCODE");
					tmp_del.setValue("location", "", 11L);
					tmp_del.setValue("SADDRESSCODE", "", 11L);

					// MboSetRemote locationset = tmp_del.getMboSet("LOCATION");
					// locationset.deleteAll(11L);
					//
					// MboSetRemote serviceset = tmp_del.getMboSet("SERVICEADDRESS");
					// serviceset.deleteAll(11L);
					//
					// tmp_del.delete();
					tmp_del.delete();
					asset_del.save();
					locationsSet.setWhere("location='" + Location_tmp + "'");
					locationsSet.reset();
					if (!locationsSet.isEmpty()) {
						tmp_del = locationsSet.getMbo(0);
						tmp_del.setValue("SADDRESSCODE", "", 11L);
						tmp_del.delete();

					}
					locationsSet.save();
					AMI_Service_Insert.setWhere("ADDRESSCODE='" + Sdp_tmp + "'");
					AMI_Service_Insert.reset();
					if (!AMI_Service_Insert.isEmpty()) {
						tmp_del = AMI_Service_Insert.getMbo(0);
						tmp_del.delete();
					}

					AMI_Service_Insert.save();
					// get_index=get_index+1;
				}

			}
		} catch (Exception e) {
			myLogger.debug("error " + e.getLocalizedMessage());
		}
	}

	public static String CoordinateTrans(double x, double y)

	{
		double dy = 0;
		double e = Math.pow((1 - Math.pow(b, 2) / Math.pow(a, 2)), 0.5);

		x -= dx;
		y -= dy;
		// Calculate the Meridional Arc
		double M = y / k0;

		// Calculate Footprint Latitude
		double mu = M / (a * (1.0 - Math.pow(e, 2) / 4.0 - 3 * Math.pow(e, 4) / 64.0 - 5 * Math.pow(e, 6) / 256.0));
		double e1 = (1.0 - Math.pow((1.0 - Math.pow(e, 2)), 0.5)) / (1.0 + Math.pow((1.0 - Math.pow(e, 2)), 0.5));

		double J1 = (3 * e1 / 2 - 27 * Math.pow(e1, 3) / 32.0);
		double J2 = (21 * Math.pow(e1, 2) / 16 - 55 * Math.pow(e1, 4) / 32.0);
		double J3 = (151 * Math.pow(e1, 3) / 96.0);
		double J4 = (1097 * Math.pow(e1, 4) / 512.0);

		double fp = mu + J1 * Math.sin(2 * mu) + J2 * Math.sin(4 * mu) + J3 * Math.sin(6 * mu) + J4 * Math.sin(8 * mu);

		// Calculate Latitude and Longitude

		double e2 = Math.pow((e * a / b), 2);
		double C1 = Math.pow(e2 * Math.cos(fp), 2);
		double T1 = Math.pow(Math.tan(fp), 2);
		double R1 = a * (1 - Math.pow(e, 2)) / Math.pow((1 - Math.pow(e, 2) * Math.pow(Math.sin(fp), 2)), (3.0 / 2.0));
		double N1 = a / Math.pow((1 - Math.pow(e, 2) * Math.pow(Math.sin(fp), 2)), 0.5);

		double D = x / (N1 * k0);

		// 計算緯度
		double Q1 = N1 * Math.tan(fp) / R1;
		double Q2 = (Math.pow(D, 2) / 2.0);
		double Q3 = (5 + 3 * T1 + 10 * C1 - 4 * Math.pow(C1, 2) - 9 * e2) * Math.pow(D, 4) / 24.0;
		double Q4 = (61 + 90 * T1 + 298 * C1 + 45 * Math.pow(T1, 2) - 3 * Math.pow(C1, 2) - 252 * e2) * Math.pow(D, 6) / 720.0;
		double lat = fp - Q1 * (Q2 - Q3 + Q4);

		// 計算經度
		double Q5 = D;
		double Q6 = (1 + 2 * T1 + C1) * Math.pow(D, 3) / 6;
		double Q7 = (5 - 2 * C1 + 28 * T1 - 3 * Math.pow(C1, 2) + 8 * e2 + 24 * Math.pow(T1, 2)) * Math.pow(D, 5) / 120.0;
		double lon = lon0 + (Q5 - Q6 + Q7) / Math.cos(fp);

		lat = (lat * 180) / Math.PI; // 緯
		lon = (lon * 180) / Math.PI; // 經

		String lonlat = lon + "," + lat;
		return lonlat;
	}

	public static Hashtable<String, String> LoadVillageCode() {
		Hashtable<String, String> cellValue = new Hashtable<String, String>();
		try {
			File file = null;
			if (MXServer.getMXServer().getProperty("mxe.hostname").equalsIgnoreCase("vm-win2012r2")) {
				file = new File("D:\\CP2_Data\\villageCode.csv");
			} else {
				file = new File("/opt/inst/cp2/villageCode.csv");
			}

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			String[] data_split;
			while ((line = reader.readLine()) != null) {
				data_split = line.split(",");
				cellValue.put(data_split[0], data_split[1]);
			}
			reader.close();
		} catch (Exception e) {

		}
		return cellValue;
	}

	public static Hashtable<String, Hashtable<String, String>> ReadMeterData(String FilePath) throws RemoteException, MXException, Exception {
		// Hashtable<Integer, Hashtable<String, String>> ReadData = new Hashtable<Integer, Hashtable<String, String>>();
		Hashtable<String, Hashtable<String, String>> ReadData = new Hashtable<String, Hashtable<String, String>>();
		Hashtable<String, String> Village_map = new Hashtable<String, String>();
		// String ls = System.getProperty("line.separator");
		myLogger.debug("Start Read File!!");
		ArrayList<String> Record_miss_vcode = new ArrayList<String>();
		String type = "";
		String get_data_tst = "";
		int tmp_cnt = 0;
		boolean v_code_get_flg = false;
		LOG_PATH = FilePath.replace(".csv", "_log.csv");
		if (FilePath.indexOf("地下") > -1)
			type = "地下";
		else
			type = "架空";
		try {
			if (type.equalsIgnoreCase("地下")) {
				Village_map = LoadVillageCode();

			}

			File file = new File(FilePath);
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "Big5");
			// BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedReader reader = new BufferedReader(isr);
			String line = null;
			Hashtable<String, String> cellValue = new Hashtable<String, String>();
			int count = 0;
			String[] data_split;
			String tmp_village_data = "";
			String tmp_villae_code = "";
			String tmp_transformer_code = "";

			while ((line = reader.readLine()) != null) {
				// line=new String(line.getBytes("utf-8"),"utf-8");

				if (count > 0) {
					data_split = line.split(",");
					cellValue = new Hashtable<String, String>();
					cellValue.clear();
					get_data_tst = line;
					if (type.equalsIgnoreCase("架空")) {
						for (int j = 0; j < EXCEL_COL_NAME_Air_Meter.length; j++) {
							cellValue.put(EXCEL_COL_NAME_Air_Meter[j], data_split[j]);
						}
					} else { // 電表地下資料
						for (int j = 0; j < EXCEL_COL_NAME_Under_Meter.length; j++) {
							if (EXCEL_COL_NAME_Under_Meter[j].equals("村里")) {
								tmp_village_data = data_split[3] + data_split[4] + data_split[5];
								// tmp_villae_code = Village_map.get(tmp_village_data);
								if (Village_map.containsKey(tmp_village_data) == false) {
									if (Record_miss_vcode.indexOf(data_split[0] + "," + data_split[4] + "," + data_split[5] + "," + data_split[6]) < 0)
										Record_miss_vcode.add(data_split[0] + "," + data_split[3] + data_split[4] + data_split[5]);
									cellValue.put("村里代碼", data_split[3] + data_split[4] + data_split[5] + "(E)");
								} else {

									cellValue.put("村里代碼", Village_map.get(tmp_village_data));
								}
							} else if (EXCEL_COL_NAME_Under_Meter[j].equals("圖號座標組別")) {
								tmp_transformer_code = data_split[6].replace(" ", "-");
								cellValue.put("圖號座標", tmp_transformer_code);
							} else
								cellValue.put(EXCEL_COL_NAME_Under_Meter[j], data_split[j]);
						}

					}
					// ReadData.put(count - 1, cellValue);
					ReadData.put(data_split[0], cellValue);
				}
				count = count + 1;
				// myLogger.debug("File content: " + line);
				// if (count >30)
				// break;
			}
			reader.close();

		} catch (Exception e) {
			myLogger.debug("Cron task Upload Load Meter Data Err!!" + e.getMessage());
		}
		myLogger.debug("File Count: " + ReadData.size());
		// ----------------save miss village file------------
		if (type.equalsIgnoreCase("地下")) {
			String ls = System.getProperty("line.separator");
			String Save_Miss_V_Code = FilePath.replace(".csv", "_vcode.csv");
			// File logFile = new File(Save_Miss_V_Code);
			// BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(Save_Miss_V_Code), "Big5"));
			for (int i = 0; i < Record_miss_vcode.size(); i++) {
				writer.write(Record_miss_vcode.get(i) + ls);
			}
			writer.close();
		}
		// ----------------------------------------------------------
		return ReadData;
	}

	public static void CheckImpLocExist(Hashtable<String, String> cellValue) throws RemoteException, MXException {

		String custNo = cellValue.get("電號");
		String villageCode = cellValue.get("村里代碼");
		String cityCode = cellValue.get("縣市");
		String countyCode = cellValue.get("鄉鎮區");
		double longitudexCode = Double.parseDouble(cellValue.get("X(67)"));
		double latitudeyCode = Double.parseDouble(cellValue.get("Y(67)"));
		Location_Insert = "";
		try {

			for (int i = 0; i < 5; i++) {
				Premise_Insert = UUID.randomUUID().toString();
				AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + Premise_Insert + "') AND UPPER(ORGID) = 'TPC' ");
				AMI_Service_Insert.reset();
				// myLogger.debug("Step1_1 create premise");
				if (AMI_Service_Insert.isEmpty()) {
					MboRemote newService = AMI_Service_Insert.add();
					newService.setValue("ADDRESSCODE", Premise_Insert, 11L);
					newService.setValue("LANGCODE", "ZHT", 11L);
					newService.setValue("ORGID", "TPC", 11L);
					newService.setValue("SITEID", custNo.substring(0, 2), 11L);
					newService.setValue("REGIONDISTRICT", villageCode, 11L);
					newService.setValue("CITY", cityCode, 11L);
					newService.setValue("COUNTY", countyCode, 11L);
					newService.setValue("LONGITUDEX", longitudexCode, 11L);
					newService.setValue("LATITUDEY", latitudeyCode, 11L);
					AMI_Service_Insert.save();
					break;
				}
			}
		} catch (Exception e) {
			myLogger.debug("Create Service Err:" + e.getMessage());
		}
		// myLogger.debug("Step1_2 create location");
		// locationsSet.setWhere("UPPER(LOCATION) = UPPER('" + Location_Insert + "')" + "AND UPPER(SITEID) = UPPER('" + custNo + "')"); //

		try {
			for (int i = 0; i < 5; i++) {
				Location_Insert = UUID.randomUUID().toString();
				locationsSet.setWhere("UPPER(LOCATION) = UPPER('" + Location_Insert + "')" + "AND UPPER(SITEID) = UPPER('" + custNo.substring(0, 2) + "')");
				locationsSet.reset();
				if (locationsSet.isEmpty()) { // Location exist
					MboRemote newLocation = locationsSet.add();
					newLocation.setValue("LOCATION", Location_Insert, 11L);
					newLocation.setValue("SITEID", custNo.substring(0, 2), 11L);
					newLocation.setValue("SADDRESSCODE", Premise_Insert, 11L);

					if (mxServer.getProperty("mxe.hostname").equalsIgnoreCase("vm-win2012r2"))
						newLocation.setValue("TYPE", "OPERATING", 11L);
					else
						newLocation.setValue("TYPE", "作業中", 11L);

					locationsSet.save();
					break;
				}
			}
			/*
			 * try {
			 * MboSetRemote locHierarchySet = null;
			 * locHierarchySet = newLocation.getMboSet("LOCHIERARCHY");
			 * MboRemote newHierarchy = locHierarchySet.add();
			 * newHierarchy.setValue("CHILDREN", 0, 11L);
			 * newHierarchy.setValue("LOCATION", Location_Insert, 11L);
			 * newHierarchy.setValue("SYSTEMID", custNo.substring(0, 2), 11L);
			 * newHierarchy.setValue("PARENT", custNo.substring(0, 2), 11L);
			 * locHierarchySet.save();
			 * } catch (MXException e) {
			 * myLogger.debug("Save Locate LOCHIERARCHY:" + e.getMessage());
			 * throw e;
			 * }
			 */
			// }
		} catch (Exception e) {
		
			myLogger.debug("Save Locate Err:" + e.getMessage());
		}
		// myLogger.debug("Step1_2 create location done");
	}

	private void doAMIAddACT(Hashtable<String, String> cellValue) {
		// String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "EXCHANGE_METER_NUM", "REMOVE_TIME" };
		// MboValueData[] valData = amiMboImp.getMboValueData(attrs);

		// myLogger.debug("start add!!");
		// AMI_Insert.setWhere("UPPER(ASSETNUM) = UPPER('" + meterId + "')");
		// AMI_Insert.reset();
		String meterId = null;
		try {
			meterId = cellValue.get("電號").substring(1, cellValue.get("電號").length());
			String custNo = cellValue.get("電號");
			String transfomerCode = cellValue.get("圖號座標");
			String feederCode = cellValue.get("饋線代號");
			double longitudexCode = Double.parseDouble(cellValue.get("X(67)"));
			double latitudeyCode = Double.parseDouble(cellValue.get("Y(67)"));
			String[] Getlonlat = CoordinateTrans(longitudexCode, latitudeyCode).split(",");
			double lonCode = Double.parseDouble(Getlonlat[0]);
			double latCode = Double.parseDouble(Getlonlat[1]);
			// if (!AMI_Insert.isEmpty()) { // 電號 exist //V1.2
			// // ---V1.6 Add Start-------------------------------------
			// MboRemote newAsset = AMI_Insert.getMbo(0);
			// if (newAsset.getString("STATUS").equalsIgnoreCase("INSTOCK")) {
			// String strTimeFormat1 = "yyyy/MM/dd HH:mm:ss";
			// Date parseDate = mxServer.getDate();
			// ((Asset) newAsset).changeStatus("OPERATE", mxServer.getDate(), "NBS New Meter from status instock");
			// ((Asset) newAsset).moveAssetWithinNonInventory(Location_Insert, "NBS New Meter from status instock", mxServer.getDate(), "MAXADMIN", newAsset.getString("wonum"),
			// newAsset.getString("newparent"), true, true, true);
			// newAsset.setValue("LOCATION", Location_Insert, 11L);
			// newAsset.setValue("ZZ_CUSTNO", custNo, 11L);
			// newAsset.setValue("SITEID", custNo.substring(0, 2), 11L);
			// newAsset.setValue("ZZ_PSTATUS", "OPERATING", 11L);
			// newAsset.setValue("SADDRESSCODE", Premise_Insert, 11L);
			// newAsset.setValue("ZZ_TRANSFORMER", transfomerCode, 11L);
			// newAsset.setValue("ZZ_FEEDER", feederCode, 11L);
			// newAsset.setValue("ZZ_TRANSFORMER", transfomerCode, 11L);
			// newAsset.setValue("ZZ_FEEDER", feederCode, 11L);
			// newAsset.setValue("ZZ_LAT", latCode, 11L);
			// newAsset.setValue("ZZ_LON", lonCode, 11L);
			//					
			// if (!Classifty_ID.equals(""))
			// newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);
			//
			// newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);
			//
			// // if (AMI_Insert.toBeSaved())
			// // AMI_Insert.save();
			//
			// classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
			//
			// if (!classspecSet.isEmpty()) {
			//
			// for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
			// AssetspecSet.setWhere("UPPER(assetnum) = UPPER('" + meterId + "')" +
			// " AND UPPER(assetattrid) = UPPER('" + assetSpec.getString("assetattrid") + "')");
			//
			// AssetspecSet.reset();
			// if (AssetspecSet.isEmpty()) {
			// SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
			// assetspec.addDetailInfor(AMI_Insert.getMbo(0), assetSpec);
			// assetspec.setValue("SITEID", meterId.substring(0, 2), 11L);
			// }
			// }
			// if (AssetspecSet.toBeSaved()) {
			// AssetspecSet.save();
			// }
			// }
			// }
			//
			// // ---V1.6 Add End---------------------------------------
			// // Insert_Status = 2; V1.6
			// } else { // 電號不在 新增 資料
			// AMI_Insert.setWhere("UPPER(ASSETNUM) = UPPER('" + meterId + "')");
			// AMI_Insert.reset();
			// if (AMI_Insert.isEmpty()) {
			MboRemote newAsset = AMIInsert.add();
			Date parseDate = mxServer.getDate();
			newAsset.setValue("ASSETNUM", meterId, 11L);
			newAsset.setValue("ZZ_TRANSFORMER", transfomerCode, 11L);
			newAsset.setValue("ZZ_FEEDER", feederCode, 11L);
			newAsset.setValue("LOCATION", Location_Insert, 11L);
			newAsset.setValue("ZZ_CUSTNO", custNo, 11L);
			newAsset.setValue("STATUS", "OPERATE", 11L);
			newAsset.setValue("ZZ_PSTATUS", "OPERATING", 11L);
			// newAsset.setValue("SADDRESSCODE", Premise_Insert, 11L);
			newAsset.setValue("SITEID", custNo.substring(0, 2), 11L);
			newAsset.setValue("ZZ_TRANSFORMER", transfomerCode, 11L);
			newAsset.setValue("ZZ_FEEDER", feederCode, 11L);
			newAsset.setValue("ZZ_LAT", latCode, 11L);
			newAsset.setValue("ZZ_LON", lonCode, 11L);
			newAsset.setValue("DESCRIPTION", meterId.substring(2, meterId.length()), 11L); // V1.7

			// commit_cnt = commit_cnt + 1;
			// if (commit_cnt > 150) {
			// commit_cnt = 0;
			// myLogger.debug("start commit:");
			if (AMIInsert.toBeSaved()) {
				AMIInsert.save(11L);
				AMIInsert.cleanup();
			}
			KeepLog("Commit OK!!");
			// AMI_Insert.clear();
			// }

			// }
			// AMI_Insert.save();
			// }
			// AMI_Insert.save();
			/*
			 * if (!Classifty_ID.equals(""))
			 * newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);
			 * 
			 * newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);
			 * 
			 * classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
			 * 
			 * // AMI_Insert.save();
			 * 
			 * if (!classspecSet.isEmpty()) {
			 * for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
			 * SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
			 * assetspec.addDetailInfor(AMI_Insert.getMbo(0), assetSpec);
			 * 
			 * if (!custNo.equals("")) {
			 * assetspec.setValue("SITEID", custNo.substring(0, 2), 11L);
			 * }
			 * 
			 * }
			 * if (AssetspecSet.toBeSaved()) {
			 * AssetspecSet.save();
			 * }
			 * }
			 */
			// }
		} catch (Exception e) {
			commit_cnt = 0;
			// myLogger.debug("insert error:" + e.getLocalizedMessage()+"  "+);
			KeepLog("Data Already Exist!!" + "CUSTNO  " + meterId);
			// try{
			try {
				AMIInsert.clear();
				AMIInsert.cleanup();
			} catch (RemoteException e1) {

				KeepLog("Data Already Exist!!" + e1.getLocalizedMessage());
			} catch (MXException e1) {

				KeepLog("Data Already Exist!!" + e1.getLocalizedMessage());
			}
			// }
			KeepLog("Data Already Exist!!");

		}

	}

	private static void RemoveData(Hashtable<String, Hashtable<String, String>> MeterData, ArrayList<String> RemoveData, String SavePath) {
		String ls = System.getProperty("line.separator");
		try {
			for (int i = 0; i < RemoveData.size(); i++)
				MeterData.remove(RemoveData.get(i));
			File logFile = new File(SavePath);
			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
			for (String i : MeterData.keySet()) {
				writer.write(MeterData.get(i) + ls);
			}
			writer.close();
		} catch (Exception e) {

		}

	}

	private static void KeepLog(String Message) {
		FileOutputStream fop = null;
		File file;
		String content = "Message";
		try {
			file = new File(LOG_PATH);
			fop = new FileOutputStream(file, true);
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			content = Message + " " + timestamp + "\r\n";
			fop.write(content.getBytes());
			fop.flush();
			fop.close();
		} catch (Exception e) {

		}
	}

	public Boolean InsertMeterData(Hashtable<String, Hashtable<String, String>> MeterData) throws RemoteException, MXException, Exception {
		// --------------------------------------------------------------------------------------------------------------------------

		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		AMIInsert = (AssetSet) mxServer.getMboSet("ASSET", ui);
		AMIInsert.setWhere("1=2");

		// locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		// AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		// AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		// classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
		// ZZ_Transformer_Insert = mxServer.getMboSet("ZZ_TransformerMasterData", ui);

		MboRemote add_transforme = null;

		// MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		// Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		// Get_CLASSIFICATIONID.reset();
		// if (!Get_CLASSIFICATIONID.isEmpty()) {
		// Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		// }
		// FileOutputStream fop = null;
		// File file;
		// String content = "This is the text content";

		// file = new File("C:\\importtime.txt");
		// fop = new FileOutputStream(file, true);
		// Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		// content = "start insert asset" + timestamp + "\r\n";
		// fop.write(content.getBytes());
		// fop.flush();
		// fop.close();

		myLogger.debug("Start  Insert Data");
		String tmp_str = "";
		// try {
		// for (int i = 0; i < 30; i++) {
		// // tmp_str = MeterData.get(i).get("電號");
		// CheckImpLocExist(MeterData.get(i));
		// doAMIAddACT(MeterData.get(i));
		// }
		// if (AMI_Insert.toBeSaved())
		// AMI_Insert.save();
		// } catch (Exception e) {
		//
		// }
		commit_cnt = 0;
		int count = 0;
		// ArrayList<String> RemoveData = new ArrayList<String>();
		Location_Insert = "DE9DC14C-1ECB-4010-B500-044B2311C339";
		// Location_Insert="1F48171F-8153-4CDC-A720-ED0195EF7098";
		KeepLog("Start Crontask Log!!");
		int save_index = 0;
		try {
			for (String i : MeterData.keySet()) {
				// if (count == 0)
				// CheckImpLocExist(MeterData.get(i));
				if (count > dostartindex) {
					doAMIAddACT(MeterData.get(i));
					if (count>doendindex)
						break;
				}

				
				count = count + 1;
				
				save_index = save_index + 1;
				if (save_index > 2000) {
					KeepLog("Sum is " + count);
					save_index = 0;
				}
			}
			// if (AMI_Insert.toBeSaved())
			// AMI_Insert.save();

			if (AMIInsert.toBeSaved())
				AMIInsert.save();
		} catch (Exception e) {
			KeepLog("Error " + e.getLocalizedMessage());
		}
		// timestamp = new Timestamp(System.currentTimeMillis());
		// content = "end insert asset" + timestamp + "\r\n";
		// fop.write(content.getBytes());
		// fop.flush();
		// fop.close();
		KeepLog("End Insert Data");
		// myLogger.debug("End Insert Data");
		return true;
	}
}
