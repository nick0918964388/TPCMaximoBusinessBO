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
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

public class CustImortNewMeterDataCrontask extends SimpleCronTask {
	public MXLogger						myLogger					= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public String[]						EXCEL_COL_NAME_Under_Meter	= { "custNo", "meterId", "type", "sec",
																	"delFlag", "subtype" };
	public String[]						EXCEL_COL_NAME_NBS			= { // get json data from soa
																	"custNo", "customerName", "customerAddress", "backUsuallyContract",
																	"backNoneSummerContract", "backsaturDayHalfPeakContract",
																	"backOffPeakContract", "usuallyContract", "noneSummerContract",
																	"offPeakContract", "saturdayHalfPeakContract", "contractType",
																	"meterBatch", "updateDate", "updateType", "meterId", "group",
																	"meterMultiple", "taxId", "industClass", "section",
																	"capacityStrengh", "capacityHeat", "capacityLight", "transdt",
																	"cycleCd", "dayCd", "cityCd", "feeder", "custCd", "outageGroup",
																	"subMeterId", "publicClassId", "meterType" };
	static String[]						ZZ_Tmp_NBS_Insert			= {
																	"assetnum", "customerName", "customerAddress", "backUsuallyContract",
																	"backNoneSummerContract", "backsaturDayHalfPeakContract",
																	"backOffPeakContract", "usuallyContract", "noneSummerContract",
																	"offPeakContract", "saturdayHalfPeakContract", "contractType",
																	"meterBatch", "updateDate", "updateType", "metergroup",
																	"meterMultiple", "taxId", "industClass", "section",
																	"capacityStrengh", "capacityHeat", "capacityLight", "transdt",
																	"cycleCd", "dayCd", "cityCd", "feeder", "custCd", "outageGroup",
																	"subMeterId", "publicClassId" };
	static String[]						ZZ_Tmp_GET_CSV				= {
																	"meterId", "customerName", "customerAddress", "backUsuallyContract",
																	"backNoneSummerContract", "backsaturDayHalfPeakContract",
																	"backOffPeakContract", "usuallyContract", "noneSummerContract",
																	"offPeakContract", "saturdayHalfPeakContract", "contractType",
																	"meterBatch", "updateDate", "updateType", "group",
																	"meterMultiple", "taxId", "industClass", "section",
																	"capacityStrengh", "capacityHeat", "capacityLight", "transdt",
																	"cycleCd", "dayCd", "cityCd", "feeder", "custCd", "outageGroup",
																	"subMeterId", "publicClassId" };

	public String						Location_Insert;
	public String						Premise_Insert;
	public LocationSet					locationsSet;
	public MboSetRemote					AMI_Service_Insert			= null;
	public MboSetRemote					AMIInsert					= null;
	public MboSetRemote					ASSET_NBS_Insert			= null;
	public MboSetRemote					AssetspecSet				= null;
	public MXServer						mxServer;
	public UserInfo						ui;
	public ConnectionKey				conKey;
	public String						Classifty_ID;
	public MboSetRemote					classspecSet				= null;
	int									commit_cnt;
	String								LOG_PATH;
	int									dostartindex;
	int									doendindex;
	private static CrontaskParamInfo[]	params;
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
		params[3].setDefault("150000");
		params[4] = new CrontaskParamInfo();
		params[4].setName("logPath");
		// params[5] = new CrontaskParamInfo();
		// params[5].setName("missDataPath");

	}

	@Override public void cronAction() {
		// Your custom code here. e.g. A sample code is below.

		try {
			// mxServer = MXServer.getMXServer();
			// ui = mxServer.getSystemUserInfo();
			myLogger.debug("Cron task Upload Meter Data Start!!");
			String LoadFilePath = getParamAsString("LOADFILE");
			// String LoadMissFilePath = getParamAsString("missDataPath");
			dostartindex = Integer.parseInt(getParamAsString("StartIndex"));
			doendindex = Integer.parseInt(getParamAsString("EndIndex"));

			LOG_PATH = getParamAsString("logPath");
			KeepLog("start contask!!");
			boolean checksatus = false;
			Hashtable<String, Hashtable<String, String>> GetData = new Hashtable<String, Hashtable<String, String>>();
			Hashtable<String, String> getMissData = new Hashtable<String, String>();
			GetData = ReadMeterDataCSV(LoadFilePath);
			// UpdateMeterData(GetData);
			UpdateNBSData(GetData);
			KeepLog("end contask!!");
			// GetData=ReadMeterData("D:\\CP2_Data\\103_架空.csv");
			// GetData = ReadMeterData(LoadFilePath);
			// checksatus = InsertMeterData(GetData);
			// checksatus = compensateDate(GetData, getMissData);
			// checksatus = checkMeterImport(GetData);
			// readDelLocation(LoadFilePath);

		} catch (Exception e) {

		}
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	public void readDelLocation(String FilePath) throws RemoteException, Exception {
		int count = 0;
		Hashtable<String, String> cellValue = new Hashtable<String, String>();
		StringBuilder setWhereClause = new StringBuilder();
		StringBuilder setLogClause = new StringBuilder();
		KeepLog("start load data!!");
		try {
			File file = new File(FilePath);
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "Big5");
			// BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedReader reader = new BufferedReader(isr);
			String line = null;
			while ((line = reader.readLine()) != null) {
				cellValue.put(line, line);
				count = count + 1;
			}
			reader.close();

		} catch (Exception e) {

		}
		KeepLog("end load data!!");
		// System.out.println("total count is "+cellValue.size());
		try {
			count = 0;
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
			for (String i : cellValue.keySet()) {
				try {
					if (setWhereClause.length() > 0)
						setWhereClause.delete(0, setWhereClause.length());
					if (setLogClause.length() > 0)
						setLogClause.delete(0, setLogClause.length());

					setWhereClause.append("UPPER(LOCATION)=UPPER('");
					setWhereClause.append(cellValue.get(i));
					setWhereClause.append("')");
					locationsSet.setWhere(setWhereClause.toString());
					locationsSet.reset();
					if (!locationsSet.isEmpty()) {
						setLogClause.append("delete location: ");
						setLogClause.append(locationsSet.getMbo(0).getString("location"));
						KeepLog(setLogClause.toString());
						locationsSet.deleteAll();
						if (locationsSet.toBeSaved())
							locationsSet.save();
					}
					count++;
					// if(count>10)
					// break;
				} catch (Exception e) {
					KeepLog("delete error!!");
				}
			}
		} catch (Exception e) {
			KeepLog("end load data ERR!!");
		}
		myLogger.debug("Cron task Delete Location Data ERROR!!");
	}

	public Boolean compensateDate(Hashtable<String, Hashtable<String, String>> MeterData, Hashtable<String, String> missData) throws RemoteException, MXException, Exception {
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		AMIInsert = (AssetSet) mxServer.getMboSet("ASSET", ui);

		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
		ASSET_NBS_Insert = mxServer.getMboSet("ZZ_ASSETTONBS", ui);

		locationsSet.setWhere("1=2");
		AssetspecSet.setWhere("1=2");
		AMI_Service_Insert.setWhere("1=2");

		// ZZ_Transformer_Insert = mxServer.getMboSet("ZZ_TransformerMasterData", ui);

		MboRemote add_transforme = null;

		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		Get_CLASSIFICATIONID.reset();
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		// davis edit
		classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
		classspecSet.reset();

		myLogger.debug("Start  Insert Data");

		commit_cnt = 0;
		int count = 0;
		// dostartindex = 40;
		// doendindex = 50;

		int batchEveryCount = 0;
		int batchIndex = 0;

		KeepLog("Start Crontask Log!!");
		int save_index = 0;
		StringBuilder setWhereClause = new StringBuilder();
		ArrayList<String> getNotFindData = new ArrayList<String>();
		ArrayList<String> getNotFindDataNbs = new ArrayList<String>();
		ArrayList<String> getMoveAssetData = new ArrayList<String>();

		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());

		boolean doActFlag = false;

		setWhereClause.append("ASSETNUM IN(");
		try {
			for (String i : missData.keySet()) {
				setWhereClause.append("'");
				setWhereClause.append(MeterData.get(missData.get(i)).get("meterId"));
				setWhereClause.append("',");
				batchIndex = batchIndex + 1;
				// getNotFindData.add(MeterData.get(i).get("meterId"));
				getNotFindDataNbs.add(MeterData.get(missData.get(i)).get("meterId"));

				if ((batchIndex > batchEveryCount) || doActFlag == true) {
					setWhereClause.delete(setWhereClause.length() - 1, setWhereClause.length());
					setWhereClause.append(")");

					// ClearData1(setWhereClause.toString());

					ASSET_NBS_Insert.setWhere(setWhereClause.toString());
					ASSET_NBS_Insert.reset();
					if (!ASSET_NBS_Insert.isEmpty()) {
						for (MboRemote currMbo = ASSET_NBS_Insert.moveFirst(); currMbo != null; currMbo = ASSET_NBS_Insert.moveNext()) {
							// currMbo.setValue("CONTRACTTYPE", MeterData.get(currMbo.getString("ASSETNUM")).get("type"), 11L);
							// currMbo.setValue("SECTION", MeterData.get(currMbo.getString("ASSETNUM")).get("sec"), 11L);
							getNotFindDataNbs.remove(currMbo.getString("ASSETNUM"));
						}

					}
					if (getNotFindDataNbs.size() == 0) {
						if (setWhereClause.length() > 0)
							setWhereClause.delete(0, setWhereClause.length());
						setWhereClause.append("ASSETNUM IN(");

						batchIndex = 0;
						continue;
					}
					// -----cleart data--------------------------
					if (setWhereClause.length() > 0)
						setWhereClause.delete(0, setWhereClause.length());
					setWhereClause.append("ASSETNUM IN(");
					for (int getCount = 0; getCount < getNotFindDataNbs.size(); getCount++) {
						setWhereClause.append("'");
						setWhereClause.append(getNotFindDataNbs.get(getCount));
						setWhereClause.append("',");
					}
					setWhereClause.delete(setWhereClause.length() - 1, setWhereClause.length());
					setWhereClause.append(")");
					ClearData1(setWhereClause.toString());

					AMIInsert.setWhere("1= 2");
					AMIInsert.reset();
					for (int j = 0; j < getNotFindDataNbs.size(); j++) {// not find the meter in asest
						// create position and hirachy
						CheckImpLocExist(MeterData.get(getNotFindDataNbs.get(j)));
						// create asset
						doAMIAddACT(MeterData.get(getNotFindDataNbs.get(j)));
					}
					// KeepLog("save step 8!!");
					if (AMIInsert.toBeSaved())
						AMIInsert.save();
					// ------------asset move
					// KeepLog("save step 9!!");

					ASSET_NBS_Insert.setWhere("1=2");
					ASSET_NBS_Insert.reset();
					for (int j = 0; j < getNotFindDataNbs.size(); j++) {// not find the meter in asest
						MboRemote addNbsMbo = ASSET_NBS_Insert.add();
						addNbsMbo.setValue("CONTRACTTYPE", MeterData.get(getNotFindDataNbs.get(j)).get("type"), 11L);
						addNbsMbo.setValue("SECTION", MeterData.get(getNotFindDataNbs.get(j)).get("sec"), 11L);
						addNbsMbo.setValue("ASSETNUM", MeterData.get(getNotFindDataNbs.get(j)).get("meterId"), 11L);
					}
					if (ASSET_NBS_Insert.toBeSaved())
						ASSET_NBS_Insert.save();
					// KeepLog("save step 10!!");
					batchIndex = 0;
					if (setWhereClause.length() > 0)
						setWhereClause.delete(0, setWhereClause.length());
					setWhereClause.append("ASSETNUM IN(");
					// getNotFindData.clear();
					getNotFindDataNbs.clear();
					// getMoveAssetData.clear();

					AMIInsert.clear();
					// AMIInsert.cleanup();
					AMI_Service_Insert.clear();
					// AMI_Service_Insert.cleanup();
					locationsSet.clear();
					// locationsSet.cleanup();
					AssetspecSet.clear();
					// AssetspecSet.cleanup();
					// break;
				}

				// doAMIAddACT(MeterData.get(i));

				// if ((count + 1) >= doendindex)
				// break;
			}

			count = count + 1;
			save_index = save_index + 1;

		} catch (Exception e) {
			KeepLog("Error " + e.getLocalizedMessage());
		}
		KeepLog("End Insert Data");

		return true;
	}

	public Hashtable<String, String> readMissData(String FilePath) throws RemoteException, Exception {
		int count = 0;
		Hashtable<String, String> cellValue = new Hashtable<String, String>();
		try {

			File file = new File(FilePath);
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "Big5");
			// BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedReader reader = new BufferedReader(isr);
			String line = null;

			String[] data_split = null;

			while ((line = reader.readLine()) != null) {
				cellValue.put(line, line);
				count = count + 1;
			}
			reader.close();

		} catch (Exception e) {

		}
		System.out.println("total count is" + cellValue.size());
		return cellValue;
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
			// BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedReader reader = new BufferedReader(isr);
			String line = null;
			Hashtable<String, String> cellValue = new Hashtable<String, String>();

			String[] data_split = null;

			while ((line = reader.readLine()) != null) {
				// line=new String(line.getBytes("utf-8"),"utf-8");

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
				// if (count > 100)
				// break;
			}
			reader.close();

		} catch (Exception e) {
			myLogger.debug("Cron task Upload Load NBS Meter Data Err!!" + e.getMessage());
		}
		myLogger.debug("File Count: " + count);
		// ----------------save miss village file------------

		// ----------------------------------------------------------
		return ReadData;
	}

	private void doUpdDataACT(Hashtable<String, String> MeterData) throws RemoteException, MXException, Exception {
		try {
			Date parseDate;
			ASSET_NBS_Insert.setWhere("UPPER(ASSETNUM) = UPPER('" + MeterData.get("meterId") + "')");
			ASSET_NBS_Insert.reset();
			String stringTimeFormat1 = "yyyyMMdd";
			SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
			String tempDate;
			String getDate;
			if (ASSET_NBS_Insert.isEmpty()) {
				MboRemote newnbs = ASSET_NBS_Insert.add();
				for (int i = 0; i < ZZ_Tmp_NBS_Insert.length; i++) {
					if (ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("transdt")) {

						tempDate = MeterData.get("transdt");
						if (tempDate.equalsIgnoreCase(""))
							continue;

						try {
							String getChineseDate = Integer.toString(Integer.parseInt(tempDate.substring(0, 3)) + 1911) + tempDate.substring(3, tempDate.length());
							parseDate = new SimpleDateFormat(stringTimeFormat1).parse(getChineseDate);
							getDate = sdFormat.format(parseDate);
							// KeepLog("set trnasdt"+getDate.toString());
							newnbs.setValue("transdt", getDate, 11L);

						} catch (Exception e) {
							KeepLog("Err!! transdt" + MeterData.get("meterId") + e.getLocalizedMessage());
						}
					} else
						// KeepLog("set" + ZZ_Tmp_NBS_Insert[i]+"value is"+MeterData.get(ZZ_Tmp_GET_CSV[i]));
						newnbs.setValue(ZZ_Tmp_NBS_Insert[i], MeterData.get(ZZ_Tmp_GET_CSV[i]));

				}
				newnbs.setValue("SITEID", MeterData.get("custNo").substring(0, 2));
			} else {
				MboRemote newnbs = ASSET_NBS_Insert.getMbo(0);
				for (int i = 0; i < ZZ_Tmp_NBS_Insert.length; i++) {
					if (ZZ_Tmp_NBS_Insert[i].equalsIgnoreCase("transdt")) {

						tempDate = MeterData.get("transdt");
						if (tempDate.equalsIgnoreCase(""))
							continue;

						try {
							String getChineseDate = Integer.toString(Integer.parseInt(tempDate.substring(0, 3)) + 1911) + tempDate.substring(3, tempDate.length());
							parseDate = new SimpleDateFormat(stringTimeFormat1).parse(getChineseDate);
							getDate = sdFormat.format(parseDate);
							newnbs.setValue("transdt", getDate, 11L);
						} catch (Exception e) {
							KeepLog("Err transdt!!" + MeterData.get("meterId") + e.getLocalizedMessage());
						}
					} else
						// KeepLog("set" + ZZ_Tmp_NBS_Insert[i]+"value is"+MeterData.get(ZZ_Tmp_GET_CSV[i]));
						newnbs.setValue(ZZ_Tmp_NBS_Insert[i], MeterData.get(ZZ_Tmp_GET_CSV[i]));

				}
				// KeepLog("set siteid");
				newnbs.setValue("SITEID", MeterData.get("custNo").substring(0, 2));

			}
			if (ASSET_NBS_Insert.toBeSaved())
				ASSET_NBS_Insert.save();

		} catch (Exception e) {
			KeepLog("Err!!" + MeterData.get("meterId") + e.getLocalizedMessage());
		}
	}

	public Hashtable<String, Hashtable<String, String>> ReadMeterData(String FilePath) throws RemoteException, MXException, Exception {
		Hashtable<String, Hashtable<String, String>> ReadData = new Hashtable<String, Hashtable<String, String>>();
		Hashtable<String, String> Village_map = new Hashtable<String, String>();
		myLogger.debug("Start Read File!!");
		String tempuse = null;
		// LOG_PATH = FilePath.replace(".csv", "_log.csv");

		int count = 0;
		try {

			File file = new File(FilePath);
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "Big5");
			// BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedReader reader = new BufferedReader(isr);
			String line = null;
			Hashtable<String, String> cellValue = new Hashtable<String, String>();

			String[] data_split = null;

			while ((line = reader.readLine()) != null) {
				// line=new String(line.getBytes("utf-8"),"utf-8");

				if (count > 0) {
					data_split = line.split(",");
					cellValue = new Hashtable<String, String>();
					cellValue.clear();
					if (data_split[4].equalsIgnoreCase("Y"))
						continue;
					for (int j = 0; j < EXCEL_COL_NAME_Under_Meter.length; j++) {
						if (EXCEL_COL_NAME_Under_Meter[j].equalsIgnoreCase("meterId")) {
							tempuse = data_split[1].substring(1, data_split[1].length());
							tempuse = data_split[5] + tempuse;
							cellValue.put(EXCEL_COL_NAME_Under_Meter[j], tempuse);
						} else
							cellValue.put(EXCEL_COL_NAME_Under_Meter[j], data_split[j]);
					}
					ReadData.put(tempuse, cellValue);
				}

				count = count + 1;
				// if (count > 100)
				// break;
			}
			reader.close();

		} catch (Exception e) {
			myLogger.debug("Cron task Upload Load Meter Data Err!!" + e.getMessage());
		}
		myLogger.debug("File Count: " + count);
		// ----------------save miss village file------------

		// ----------------------------------------------------------
		return ReadData;
	}

	public void CheckImpLocExist(Hashtable<String, String> cellValue) throws RemoteException, MXException {

		String custNo = cellValue.get("custNo");

		Location_Insert = "";
		MboRemote newLocation = null;
		try {

			// for (int i = 0; i < 5; i++) {
			Premise_Insert = UUID.randomUUID().toString();
			// AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + Premise_Insert + "') AND UPPER(ORGID) = 'TPC' ");
			// AMI_Service_Insert.reset();
			// if (AMI_Service_Insert.isEmpty()) {
			// KeepLog("save step 1!!");
			MboRemote newService = AMI_Service_Insert.add();
			newService.setValue("ADDRESSCODE", Premise_Insert, 11L);
			newService.setValue("LANGCODE", "ZHT", 11L);
			newService.setValue("ORGID", "TPC", 11L);
			newService.setValue("SITEID", custNo.substring(0, 2), 11L);
			AMI_Service_Insert.save();
			// KeepLog("save step 2!!");
			// break;
			// }
			// }
		} catch (Exception e) {

			myLogger.debug("Create Service Err:" + e.getMessage());
		}

		try {
			// for (int i = 0; i < 5; i++) {
			// KeepLog("save step 3!!");
			Location_Insert = UUID.randomUUID().toString();
			// locationsSet.setWhere("UPPER(LOCATION) = UPPER('" + Location_Insert + "')" + "AND UPPER(SITEID) = UPPER('" + custNo.substring(0, 2) + "')");
			// locationsSet.reset();
			// if (locationsSet.isEmpty()) { // Location exist
			newLocation = locationsSet.add();
			newLocation.setValue("LOCATION", Location_Insert, 11L);
			newLocation.setValue("SITEID", custNo.substring(0, 2), 11L);
			newLocation.setValue("SADDRESSCODE", Premise_Insert, 11L);

			if (mxServer.getProperty("mxe.hostname").equalsIgnoreCase("vm-win2012r2"))
				newLocation.setValue("TYPE", "OPERATING", 11L);
			else
				newLocation.setValue("TYPE", "作業中", 11L);

			locationsSet.save();
			// KeepLog("save step 4!!");
			// break;
			// }
			// }

			try {

				MboSetRemote locHierarchySet = null;
				locHierarchySet = newLocation.getMboSet("LOCHIERARCHY");
				MboRemote newHierarchy = locHierarchySet.add();
				newHierarchy.setValue("CHILDREN", 0, 11L);
				newHierarchy.setValue("LOCATION", Location_Insert, 11L);
				newHierarchy.setValue("SYSTEMID", custNo.substring(0, 2), 11L);
				newHierarchy.setValue("PARENT", custNo.substring(0, 2), 11L);
				locHierarchySet.save();
				// KeepLog("save step 5!!");
			} catch (MXException e) {
				myLogger.debug("Save Locate LOCHIERARCHY:" + e.getMessage());
				throw e;
			}

			// }
		} catch (Exception e) {

			myLogger.debug("Save Locate Err:" + e.getMessage());
			KeepLog("Save Locate Err:!" + e.getMessage());
		}
		// myLogger.debug("Step1_2 create location done");
	}

	private void doAMIMove(Hashtable<String, String> cellValue) {
		MboSetRemote moveAssetMbo = null;
		String meterId = null;
		meterId = cellValue.get("meterId");
		String custNo = cellValue.get("custNo");
		StringBuilder setWhereClause = new StringBuilder();
		try {
			setWhereClause.append("UPPER(ASSETNUM)=UPPER('");
			setWhereClause.append(meterId);
			setWhereClause.append("')");

			moveAssetMbo = (AssetSet) mxServer.getMboSet("ASSET", ui);
			moveAssetMbo.setWhere(setWhereClause.toString());
			if (!moveAssetMbo.isEmpty()) {
				MboRemote moveAssetData = moveAssetMbo.getMbo(0);
				moveAssetData.setValue("NEWSITE", custNo.substring(0, 2), 11L);
				moveAssetData.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.2
				((Asset) moveAssetData).moveAssetWithinNonInventory(Location_Insert, "Hes Updatedevice For New Meter!!", mxServer.getDate(),
						"MAXADMIN", moveAssetData.getString("wonum"), moveAssetData.getString("newparent"), true, true, true);
			}

			if (moveAssetMbo.toBeSaved())
				moveAssetMbo.save();

		} catch (Exception e) {
			myLogger.debug("error message" + e.getLocalizedMessage());
		}

	}

	private void doAMIUpdData(Hashtable<String, String> cellValue, MboRemote updMboAsset) {
		String updCustno = cellValue.get("custNo");
		try {
			if (!updMboAsset.getString("ZZ_CUSTNO").equalsIgnoreCase(updCustno)) {
				KeepLog("UpdateMeter custno not same" + cellValue.get("meterId"));
				if (!updMboAsset.getString("SITEID").equalsIgnoreCase(updCustno.substring(0, 2))) { // site id different
					updMboAsset.delete();
					KeepLog("UpdateMeter siteid not same clear data" + cellValue.get("meterId"));
					if (AMIInsert.toBeSaved())
						AMIInsert.save();
					doAMIAddData(cellValue);
					KeepLog("UpdateMeter siteid not same create data" + cellValue.get("meterId"));
				} else {
					updMboAsset.setValue("ZZ_CUSTNO", updCustno, 11L);
					updMboAsset.setValue("ZZ_FEEDER", cellValue.get("feeder"), 11L);
					KeepLog("UpdateMeter custno not same upd data" + cellValue.get("meterId"));
				}
			} else {
				updMboAsset.setValue("ZZ_FEEDER", cellValue.get("feeder"), 11L);
				KeepLog("UpdateMeter custno  same upd data" + cellValue.get("meterId"));
			}
			if (AMIInsert.toBeSaved())
				AMIInsert.save();

		} catch (Exception e) {

		}

	}

	private void doAMIAddData(Hashtable<String, String> cellValue) {
		String meterId = null;
		String getLook = null;
		try {
			// KeepLog("save step 6!!");
			meterId = cellValue.get("meterId");
			String custNo = cellValue.get("custNo");
			MboRemote newAsset = AMIInsert.add();
			newAsset.setValue("ASSETNUM", meterId, 11L);
			newAsset.setValue("LOCATION", Location_Insert, 11L);
			newAsset.setValue("ZZ_CUSTNO", custNo, 11L);
			newAsset.setValue("SITEID", custNo.substring(0, 2), 11L);
			newAsset.setValue("STATUS", "NOCOMM", 11L);
			newAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L);
			newAsset.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.2
			newAsset.setValue("DESCRIPTION", meterId.substring(2, meterId.length()), 11L);
			newAsset.setValue("ZZ_ISTWOWAY", false, 11L);
			newAsset.setValue("ZZ_INSTALLDATE", mxServer.getDate(), 11L);
			newAsset.setValue("ZZ_INSTALLDATE", mxServer.getDate(), 11L);
			newAsset.setValue("ZZ_FEEDER", cellValue.get("feeder"), 11L);
			KeepLog("set value ok!!" + meterId);

			if (!Classifty_ID.equals(""))
				newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);

			// classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
			// classspecSet.reset();
			// ----------asset spec write---------------------------------------------------------------------------------------

			if (!classspecSet.isEmpty()) {
				AssetspecSet.reset();
				// for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
				// SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
				// assetspec.addDetailInfor(newAsset, assetSpec);
				// assetspec.setValue("SITEID", custNo.substring(0, 2), 11L);
				// try {
				// if (AssetspecSet.toBeSaved()) {
				// AssetspecSet.save();
				// }
				// } catch (Exception e) {
				// KeepLog("set value error!!" + e.getLocalizedMessage());
				// }
				// }
				for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
					AssetspecSet.setWhere("UPPER(assetnum) = UPPER('" + meterId + "')" +
							" AND UPPER(assetattrid) = UPPER('" + assetSpec.getString("assetattrid") + "')");
					AssetspecSet.reset();
					if (AssetspecSet.isEmpty()) {
						SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
						assetspec.addDetailInfor(newAsset, assetSpec);
						assetspec.setValue("SITEID", custNo.substring(0, 2), 11L);
						if (AssetspecSet.toBeSaved()) {
							AssetspecSet.save();
						}
					}
				}
			}
			if (AMIInsert.toBeSaved())
				AMIInsert.save();

			KeepLog("Commit  OK!!" + meterId);
		} catch (Exception e) {
			commit_cnt = 0;
			// myLogger.debug("insert error:" + e.getLocalizedMessage()+"  "+);
			KeepLog("set value error 1!! " + e.getLocalizedMessage());
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

	private void doAMIAddACT(Hashtable<String, String> cellValue) {
		String meterId = null;
		String getLook = null;
		try {
			// KeepLog("save step 6!!");
			meterId = cellValue.get("meterId");
			String custNo = cellValue.get("custNo");
			MboRemote newAsset = AMIInsert.add();
			newAsset.setValue("ASSETNUM", meterId, 11L);
			newAsset.setValue("LOCATION", Location_Insert, 11L);
			newAsset.setValue("ZZ_CUSTNO", custNo, 11L);
			newAsset.setValue("SITEID", custNo.substring(0, 2), 11L);
			newAsset.setValue("STATUS", "NOCOMM", 11L);
			newAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L);
			newAsset.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.2
			newAsset.setValue("DESCRIPTION", meterId.substring(2, meterId.length()), 11L);
			newAsset.setValue("ZZ_ISTWOWAY", false, 11L);
			newAsset.setValue("ZZ_INSTALLDATE", mxServer.getDate(), 11L);
			if (!Classifty_ID.equals(""))
				newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);

			// classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
			// classspecSet.reset();
			// ----------asset spec write---------------------------------------------------------------------------------------

			if (!classspecSet.isEmpty()) {
				AssetspecSet.reset();
				for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
					AssetspecSet.setWhere("UPPER(assetnum) = UPPER('" + meterId + "')" +
							" AND UPPER(assetattrid) = UPPER('" + assetSpec.getString("assetattrid") + "')");
					AssetspecSet.reset();
					if (AssetspecSet.isEmpty()) {
						SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
						assetspec.addDetailInfor(newAsset, assetSpec);
						assetspec.setValue("SITEID", custNo.substring(0, 2), 11L);
						if (AssetspecSet.toBeSaved()) {
							AssetspecSet.save();
						}
					}

				}

			}
			// KeepLog("save step 7!!");
			// if (AMIInsert.toBeSaved()) {
			// AMIInsert.save(11L);
			// AMIInsert.cleanup();
			// }

			KeepLog("Commit  OK!!" + meterId);
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

	private void KeepLog(String Message) {
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

	public void ClearData1(String setWhereString) throws RemoteException, MXException, Exception {
		StringBuilder setWhereClause = new StringBuilder();
		MboSetRemote assetDelMboSet = null;
		// mxServer = MXServer.getMXServer();
		// ui = mxServer.getSystemUserInfo();
		assetDelMboSet = (AssetSet) mxServer.getMboSet("ASSET", ui);
		// locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		// AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		// AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);

		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());

		// setWhereClause.append("UPPER(ASSETNUM)=UPPER('");
		// setWhereClause.append(meterId);
		// setWhereClause.append("') AND SITEID='");
		// setWhereClause.append(siteId);
		// setWhereClause.append("'");
		KeepLog("clear start step1!!");
		assetDelMboSet.setWhere(setWhereString);
		assetDelMboSet.reset();
		String Location_tmp;
		String Sdp_tmp;
		String asset_tmp;
		int getIndex = 0;

		try {
			if (!assetDelMboSet.isEmpty()) {
				while (assetDelMboSet.getMbo(getIndex) != null) {
					MboRemote tmpAssetDel = null;
					tmpAssetDel = assetDelMboSet.getMbo(getIndex);

					MboSetRemote locationDelMbo = tmpAssetDel.getMboSet("LOCATION");
					MboSetRemote serviceDelMbo = tmpAssetDel.getMboSet("SERVICEADDRESS");
					tmpAssetDel.setValue("location", "", 11L);
					tmpAssetDel.setValue("SADDRESSCODE", "", 11L);
					tmpAssetDel.setValue("CLASSSTRUCTUREID", "", 11L);
					if (assetDelMboSet.toBeSaved())
						assetDelMboSet.save();

					if (!locationDelMbo.isEmpty()) {
						locationDelMbo.getMbo(0).setValue("SADDRESSCODE", "", 11L);
						if (locationDelMbo.toBeSaved())
							locationDelMbo.save();
					}

					if (!serviceDelMbo.isEmpty()) {
						serviceDelMbo.deleteAll(11L);
						if (serviceDelMbo.toBeSaved())
							serviceDelMbo.save();
					}
					if (!locationDelMbo.isEmpty()) {
						locationDelMbo.deleteAll(11L);
					}

					getIndex++;
				}
				// KeepLog("clear start step 7!!");
				assetDelMboSet.deleteAll();
				if (assetDelMboSet.toBeSaved())
					assetDelMboSet.save();
				// KeepLog("clear start step 8!!");
			}
		} catch (Exception e) {
			myLogger.debug("error " + e.getLocalizedMessage());

		}
	}

	public void ClearData(String meterId, String siteId) throws RemoteException, MXException, Exception {
		StringBuilder setWhereClause = new StringBuilder();
		MboSetRemote assetDelMboSet = null;
		// mxServer = MXServer.getMXServer();
		// ui = mxServer.getSystemUserInfo();
		assetDelMboSet = (AssetSet) mxServer.getMboSet("ASSET", ui);
		// locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		// AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		// AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);

		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());

		setWhereClause.append("UPPER(ASSETNUM)=UPPER('");
		setWhereClause.append(meterId);
		setWhereClause.append("') AND SITEID='");
		setWhereClause.append(siteId);
		setWhereClause.append("'");
		assetDelMboSet.setWhere(setWhereClause.toString());
		assetDelMboSet.reset();
		String Location_tmp;
		String Sdp_tmp;
		String asset_tmp;
		int get_index = 0;
		// MboRemote tmp_del = null;
		try {
			if (!assetDelMboSet.isEmpty()) {
				MboRemote tmpAssetDel = null;
				tmpAssetDel = assetDelMboSet.getMbo(0);

				MboSetRemote locationDelMbo = tmpAssetDel.getMboSet("LOCATION");
				MboSetRemote serviceDelMbo = tmpAssetDel.getMboSet("SERVICEADDRESS");
				tmpAssetDel.setValue("location", "", 11L);
				tmpAssetDel.setValue("SADDRESSCODE", "", 11L);
				tmpAssetDel.setValue("CLASSSTRUCTUREID", "", 11L);
				if (assetDelMboSet.toBeSaved())
					assetDelMboSet.save();
				if (!locationDelMbo.isEmpty()) {
					locationDelMbo.getMbo(0).setValue("SADDRESSCODE", "", 11L);
					if (locationDelMbo.toBeSaved())
						locationDelMbo.save();
				}

				if (!serviceDelMbo.isEmpty())
					serviceDelMbo.deleteAll(11L);

				if (!locationDelMbo.isEmpty()) {
					locationDelMbo.deleteAll(11L);
				}
				assetDelMboSet.deleteAll();
				if (assetDelMboSet.toBeSaved())
					assetDelMboSet.save();

				// Location_tmp = tmp_del.getString("location");
				// Sdp_tmp = tmp_del.getString("SADDRESSCODE");
				// tmp_del.setValue("location", "", 11L);
				// tmp_del.setValue("SADDRESSCODE", "", 11L);
				// tmp_del.setValue("CLASSSTRUCTUREID", "", 11L);
				//
				// tmp_del.delete();
				// asset_del.save();
				// locationsSet.setWhere("location='" + Location_tmp + "'");
				// locationsSet.reset();
				// if (!locationsSet.isEmpty()) {
				// tmp_del = locationsSet.getMbo(0);
				// tmp_del.setValue("SADDRESSCODE", "", 11L);
				// tmp_del.delete();
				//
				// }
				// locationsSet.save();
				// // ----delete assetspec
				//
				// if (setWhereClause.length() > 0)
				// setWhereClause.delete(0, setWhereClause.length());
				//
				// setWhereClause.append("UPPER(ASSETNUM)=UPPER('");
				// setWhereClause.append(meterId);
				// setWhereClause.append("') AND SITEID='");
				// setWhereClause.append(siteId);
				// setWhereClause.append("'");
				// AssetspecSet.setWhere(setWhereClause.toString());
				// if (!AssetspecSet.isEmpty()) {
				// AssetspecSet.deleteAll();
				// }
				// if (AssetspecSet.toBeSaved())
				// AssetspecSet.save();
				//
				// AMI_Service_Insert.setWhere("ADDRESSCODE='" + Sdp_tmp + "'");
				// AMI_Service_Insert.reset();
				// if (!AMI_Service_Insert.isEmpty()) {
				// tmp_del = AMI_Service_Insert.getMbo(0);
				// tmp_del.delete();
				// }
				// AMI_Service_Insert.save();
				// // get_index=get_index+1;

			}
		} catch (Exception e) {
			myLogger.debug("error " + e.getLocalizedMessage());
			KeepLog("Cleardata error " + e.getLocalizedMessage());
		}
	}

	public Boolean checkMeterImport(Hashtable<String, Hashtable<String, String>> MeterData) throws RemoteException, MXException, Exception {
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		AMIInsert = (AssetSet) mxServer.getMboSet("ASSET", ui);

		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
		ASSET_NBS_Insert = mxServer.getMboSet("ZZ_ASSETTONBS", ui);

		locationsSet.setWhere("1=2");
		AssetspecSet.setWhere("1=2");
		AMI_Service_Insert.setWhere("1=2");

		// ZZ_Transformer_Insert = mxServer.getMboSet("ZZ_TransformerMasterData", ui);

		MboRemote add_transforme = null;

		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		Get_CLASSIFICATIONID.reset();
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		// davis edit
		classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
		classspecSet.reset();

		myLogger.debug("Start  Insert Data");

		commit_cnt = 0;
		int count = 0;
		// dostartindex = 40;
		// doendindex = 50;

		int batchEveryCount = 60;
		int batchIndex = 0;

		KeepLog("Start Crontask Log!!");
		int save_index = 0;
		StringBuilder setWhereClause = new StringBuilder();
		ArrayList<String> getNotFindData = new ArrayList<String>();
		ArrayList<String> getNotFindDataNbs = new ArrayList<String>();
		ArrayList<String> getMoveAssetData = new ArrayList<String>();

		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());

		boolean doActFlag = false;

		setWhereClause.append("ASSETNUM IN(");
		try {
			for (String i : MeterData.keySet()) {
				if (count + 1 >= dostartindex) {
					setWhereClause.append("'");
					setWhereClause.append(MeterData.get(i).get("meterId"));
					setWhereClause.append("',");
					batchIndex = batchIndex + 1;
					// getNotFindData.add(MeterData.get(i).get("meterId"));
					getNotFindDataNbs.add(MeterData.get(i).get("meterId"));

					if ((count + 1) >= doendindex)
						doActFlag = true;

					if ((batchIndex > batchEveryCount) || doActFlag == true) {
						setWhereClause.delete(setWhereClause.length() - 1, setWhereClause.length());
						setWhereClause.append(")");

						// ClearData1(setWhereClause.toString());

						ASSET_NBS_Insert.setWhere(setWhereClause.toString());
						ASSET_NBS_Insert.reset();
						if (!ASSET_NBS_Insert.isEmpty()) {
							for (MboRemote currMbo = ASSET_NBS_Insert.moveFirst(); currMbo != null; currMbo = ASSET_NBS_Insert.moveNext()) {
								// currMbo.setValue("CONTRACTTYPE", MeterData.get(currMbo.getString("ASSETNUM")).get("type"), 11L);
								// currMbo.setValue("SECTION", MeterData.get(currMbo.getString("ASSETNUM")).get("sec"), 11L);
								getNotFindDataNbs.remove(currMbo.getString("ASSETNUM"));
							}
							// if (ASSET_NBS_Insert.toBeSaved())
							// ASSET_NBS_Insert.save();
						}
						if (getNotFindDataNbs.size() == 0) {
							if (setWhereClause.length() > 0)
								setWhereClause.delete(0, setWhereClause.length());
							setWhereClause.append("ASSETNUM IN(");

							if ((count + 1) >= doendindex)
								break;
							// count = count + 1;
							batchIndex = 0;
							continue;
						}
						// -----cleart data--------------------------
						if (setWhereClause.length() > 0)
							setWhereClause.delete(0, setWhereClause.length());
						setWhereClause.append("ASSETNUM IN(");
						for (int getCount = 0; getCount < getNotFindDataNbs.size(); getCount++) {
							setWhereClause.append("'");
							setWhereClause.append(getNotFindDataNbs.get(getCount));
							setWhereClause.append("',");
						}
						setWhereClause.delete(setWhereClause.length() - 1, setWhereClause.length());
						setWhereClause.append(")");
						ClearData1(setWhereClause.toString());

						AMIInsert.setWhere("1= 2");
						AMIInsert.reset();
						for (int j = 0; j < getNotFindDataNbs.size(); j++) {// not find the meter in asest
							// create position and hirachy
							CheckImpLocExist(MeterData.get(getNotFindDataNbs.get(j)));
							// create asset
							doAMIAddACT(MeterData.get(getNotFindDataNbs.get(j)));
						}
						KeepLog("save step 8!!");
						if (AMIInsert.toBeSaved())
							AMIInsert.save();
						// ------------asset move
						KeepLog("save step 9!!");

						ASSET_NBS_Insert.setWhere("1=2");
						ASSET_NBS_Insert.reset();
						for (int j = 0; j < getNotFindDataNbs.size(); j++) {// not find the meter in asest
							MboRemote addNbsMbo = ASSET_NBS_Insert.add();
							addNbsMbo.setValue("CONTRACTTYPE", MeterData.get(getNotFindDataNbs.get(j)).get("type"), 11L);
							addNbsMbo.setValue("SECTION", MeterData.get(getNotFindDataNbs.get(j)).get("sec"), 11L);
							addNbsMbo.setValue("ASSETNUM", MeterData.get(getNotFindDataNbs.get(j)).get("meterId"), 11L);

						}
						if (ASSET_NBS_Insert.toBeSaved())
							ASSET_NBS_Insert.save();
						KeepLog("save step 10!!");
						batchIndex = 0;
						if (setWhereClause.length() > 0)
							setWhereClause.delete(0, setWhereClause.length());
						setWhereClause.append("ASSETNUM IN(");
						// getNotFindData.clear();
						getNotFindDataNbs.clear();
						// getMoveAssetData.clear();

						AMIInsert.clear();
						// AMIInsert.cleanup();
						AMI_Service_Insert.clear();
						// AMI_Service_Insert.cleanup();
						locationsSet.clear();
						// locationsSet.cleanup();
						AssetspecSet.clear();
						// AssetspecSet.cleanup();
					}

					// doAMIAddACT(MeterData.get(i));

					if ((count + 1) >= doendindex)
						break;
				}

				count = count + 1;
				save_index = save_index + 1;

			}

			if (AMIInsert.toBeSaved())
				AMIInsert.save();

		} catch (Exception e) {
			KeepLog("Error " + e.getLocalizedMessage());
		}
		KeepLog("End Insert Data");

		return true;
	}

	public Boolean UpdateNBSData(Hashtable<String, Hashtable<String, String>> MeterData) throws RemoteException, MXException, Exception {
		int docount = 0;
		StringBuilder setWhereClause = new StringBuilder();
		String getMeterId = null;
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		AMIInsert = (AssetSet) mxServer.getMboSet("ASSET", ui);
		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		classspecSet = mxServer.getMboSet("CLASSSPEC", ui);

		ASSET_NBS_Insert = mxServer.getMboSet("ZZ_ASSETTONBS", ui);
		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		Get_CLASSIFICATIONID.reset();
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		// davis edit
		classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
		classspecSet.reset();

		locationsSet.setWhere("1=2");
		AssetspecSet.setWhere("1=2");
		AMI_Service_Insert.setWhere("1=2");

		try {
			for (String i : MeterData.keySet()) {
				if (docount + 1 >= dostartindex) {
					getMeterId = MeterData.get(i).get("meterId");
					if (setWhereClause.length() > 0)
						setWhereClause.delete(0, setWhereClause.length());
					setWhereClause.append("assetnum='");
					setWhereClause.append(getMeterId);
					setWhereClause.append("'");
					// KeepLog(setWhereClause.toString());
					AMIInsert.setWhere(setWhereClause.toString());
					AMIInsert.reset();
					if (AMIInsert.isEmpty()) { // add meter
						// KeepLog("create new location!!");
						CheckImpLocExist(MeterData.get(getMeterId));
						KeepLog("create new location end!!");
						doAMIAddData(MeterData.get(getMeterId));

					} else {
						// MboRemote updAsset = AMIInsert.getMbo(0);
						// updAsset.setValue("ZZ_CUSTNO", MeterData.get(i).get("custNo"), 11L);
						// updAsset.setValue("ZZ_FEEDER", MeterData.get(i).get("feeder"), 11L);
						// if (AMIInsert.toBeSaved())
						// AMIInsert.save();
						// AMIInsert.deleteAll();

						// if (AMIInsert.toBeSaved())
						// AMIInsert.save();
						// KeepLog("create new location!!");
						// CheckImpLocExist(MeterData.get(getMeterId));
						// KeepLog("create new location end!!");
						doAMIUpdData(MeterData.get(getMeterId), AMIInsert.getMbo(0));
						// doAMIAddData(MeterData.get(getMeterId));
					}
					// ----------------SAVE NBS-------------------
					// doUpdDataACT(MeterData.get(getMeterId));
				}

				// doAMIAddACT(MeterData.get(i));
				docount = docount + 1;
				if (docount >= doendindex)
					break;
			}

		} catch (Exception e) {
			KeepLog("Error " + e.getLocalizedMessage());
		}

		return true;
	}

	public Boolean UpdateMeterData(Hashtable<String, Hashtable<String, String>> MeterData) throws RemoteException, MXException, Exception {
		int docount = 0;
		StringBuilder setWhereClause = new StringBuilder();
		String getMeterId = null;
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		AMIInsert = (AssetSet) mxServer.getMboSet("ASSET", ui);
		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		classspecSet = mxServer.getMboSet("CLASSSPEC", ui);

		ASSET_NBS_Insert = mxServer.getMboSet("ZZ_ASSETTONBS", ui);
		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		Get_CLASSIFICATIONID.reset();
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		// davis edit
		classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
		classspecSet.reset();

		locationsSet.setWhere("1=2");
		AssetspecSet.setWhere("1=2");
		AMI_Service_Insert.setWhere("1=2");

		try {
			for (String i : MeterData.keySet()) {
				if (docount + 1 >= dostartindex) {
					getMeterId = MeterData.get(i).get("meterId");
					if (setWhereClause.length() > 0)
						setWhereClause.delete(0, setWhereClause.length());
					setWhereClause.append("assetnum='");
					setWhereClause.append(getMeterId);
					setWhereClause.append("'");

					AMIInsert.setWhere(setWhereClause.toString());
					AMIInsert.reset();
					if (AMIInsert.isEmpty()) { // add meter
						KeepLog("create new location!!");
						CheckImpLocExist(MeterData.get(getMeterId));
						KeepLog("create new location end!!");
						doAMIAddData(MeterData.get(getMeterId));
					} else {
						// AMIInsert.deleteAll();
						// if (AMIInsert.toBeSaved())
						// AMIInsert.save();
						// KeepLog("create new location!!");
						// CheckImpLocExist(MeterData.get(getMeterId));
						// KeepLog("create new location end!!");
						// doAMIAddData(MeterData.get(getMeterId));
						// String custNo=
					}
					// ----------------SAVE NBS-------------------
					ASSET_NBS_Insert.setWhere(setWhereClause.toString());
					ASSET_NBS_Insert.reset();
					if (!ASSET_NBS_Insert.isEmpty()) {
						MboRemote getNbsData = ASSET_NBS_Insert.getMbo(0);
						getNbsData.setValue("CONTRACTTYPE", MeterData.get(getMeterId).get("type"), 11L);
						getNbsData.setValue("SECTION", MeterData.get(getMeterId).get("sec"), 11L);

						if (ASSET_NBS_Insert.toBeSaved())
							ASSET_NBS_Insert.save();
					} else {
						MboRemote addNbsMbo = ASSET_NBS_Insert.add();
						addNbsMbo.setValue("CONTRACTTYPE", MeterData.get(getMeterId).get("type"), 11L);
						addNbsMbo.setValue("SECTION", MeterData.get(getMeterId).get("sec"), 11L);
						addNbsMbo.setValue("ASSETNUM", MeterData.get(getMeterId).get("meterId"), 11L);
						if (ASSET_NBS_Insert.toBeSaved())
							ASSET_NBS_Insert.save();
					}
				}

				// doAMIAddACT(MeterData.get(i));
				docount = docount + 1;
				if (docount > doendindex)
					break;
			}

		} catch (Exception e) {
			KeepLog("Error " + e.getLocalizedMessage());
		}

		return true;
	}

	public Boolean InsertMeterData(Hashtable<String, Hashtable<String, String>> MeterData) throws RemoteException, MXException, Exception {
		// --------------------------------------------------------------------------------------------------------------------------

		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		AMIInsert = (AssetSet) mxServer.getMboSet("ASSET", ui);

		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
		ASSET_NBS_Insert = mxServer.getMboSet("ZZ_ASSETTONBS", ui);

		locationsSet.setWhere("1=2");
		AssetspecSet.setWhere("1=2");
		AMI_Service_Insert.setWhere("1=2");

		// ZZ_Transformer_Insert = mxServer.getMboSet("ZZ_TransformerMasterData", ui);

		MboRemote add_transforme = null;

		MboSetRemote Get_CLASSIFICATIONID = mxServer.getMboSet("CLASSSTRUCTURE", ui);
		Get_CLASSIFICATIONID.setWhere("UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')");
		Get_CLASSIFICATIONID.reset();
		if (!Get_CLASSIFICATIONID.isEmpty()) {
			Classifty_ID = Get_CLASSIFICATIONID.getMbo(0).getString("CLASSSTRUCTUREID");
		}

		// davis edit
		classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
		classspecSet.reset();

		myLogger.debug("Start  Insert Data");

		commit_cnt = 0;
		int count = 0;
		// dostartindex = 40;
		// doendindex = 50;

		int batchEveryCount = 100;
		int batchIndex = 0;

		KeepLog("Start Crontask Log!!");
		int save_index = 0;
		StringBuilder setWhereClause = new StringBuilder();
		ArrayList<String> getNotFindData = new ArrayList<String>();
		ArrayList<String> getNotFindDataNbs = new ArrayList<String>();
		ArrayList<String> getMoveAssetData = new ArrayList<String>();

		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());

		boolean doActFlag = false;

		setWhereClause.append("ASSETNUM IN(");
		try {
			for (String i : MeterData.keySet()) {
				if (count + 1 >= dostartindex) {
					setWhereClause.append("'");
					setWhereClause.append(MeterData.get(i).get("meterId"));
					setWhereClause.append("',");
					batchIndex = batchIndex + 1;
					getNotFindData.add(MeterData.get(i).get("meterId"));
					getNotFindDataNbs.add(MeterData.get(i).get("meterId"));

					if ((count + 1) >= doendindex)
						doActFlag = true;

					if ((batchIndex > batchEveryCount) || doActFlag == true) {
						setWhereClause.delete(setWhereClause.length() - 1, setWhereClause.length());
						setWhereClause.append(")");
						AMIInsert.setWhere(setWhereClause.toString());
						AMIInsert.reset();
						// if (!AMIInsert.isEmpty()) {
						// for (MboRemote currMbo = AMIInsert.moveFirst(); currMbo != null; currMbo = AMIInsert.moveNext()) {
						// getNotFindData.remove(currMbo.getString("ASSETNUM"));
						//								
						// if(!MeterData.get(currMbo.getString("ASSETNUM")).get("custNo").substring(0, 2).equalsIgnoreCase(currMbo.getString("SITEID"))){
						// getMoveAssetData.add(currMbo.getString("ASSETNUM"));
						// getMoveAssetSite.add(currMbo.getString("SITEID"));
						// continue;
						// }
						// currMbo.setValue("ZZ_CUSTNO", MeterData.get(currMbo.getString("ASSETNUM")).get("custNo"), 11L);
						// KeepLog("Update:"+currMbo.getString("ASSETNUM"));
						// }
						// if (AMIInsert.toBeSaved())
						// AMIInsert.save();
						// for (MboRemote currMbo = AMIInsert.moveFirst(); currMbo != null; currMbo = AMIInsert.moveNext()) {
						// ClearData(currMbo.getString("ASSETNUM"), currMbo.getString("SITEID"));
						// }
						// KeepLog("clear start!!");
						ClearData1(setWhereClause.toString());
						// KeepLog("clear end!!");

						ASSET_NBS_Insert.setWhere(setWhereClause.toString());
						ASSET_NBS_Insert.reset();
						if (!ASSET_NBS_Insert.isEmpty()) {
							for (MboRemote currMbo = ASSET_NBS_Insert.moveFirst(); currMbo != null; currMbo = ASSET_NBS_Insert.moveNext()) {
								currMbo.setValue("CONTRACTTYPE", MeterData.get(currMbo.getString("ASSETNUM")).get("type"), 11L);
								currMbo.setValue("SECTION", MeterData.get(currMbo.getString("ASSETNUM")).get("sec"), 11L);
								getNotFindDataNbs.remove(currMbo.getString("ASSETNUM"));
							}
							if (ASSET_NBS_Insert.toBeSaved())
								ASSET_NBS_Insert.save();
						}

						AMIInsert.setWhere("1= 2");
						AMIInsert.reset();
						for (int j = 0; j < getNotFindData.size(); j++) {// not find the meter in asest
							// create position and hirachy
							CheckImpLocExist(MeterData.get(getNotFindData.get(j)));
							// create asset
							doAMIAddACT(MeterData.get(getNotFindData.get(j)));
						}
						KeepLog("save step 8!!");
						if (AMIInsert.toBeSaved())
							AMIInsert.save();
						KeepLog("save step 9!!");
						// ------------asset move
						// for (int j = 0; j < getMoveAssetData.size(); j++) {
						// KeepLog("clear Data assetnum is " + getMoveAssetData.get(j) + " Org Site is " +
						// getMoveAssetSite.get(j) + "new custno is " + MeterData.get(getMoveAssetData.get(j)).get("custNo"));
						// ClearData(MeterData.get(getMoveAssetData.get(j)).get("meterId"), getMoveAssetSite.get(j));
						// KeepLog("clear Data assetnum is " + getMoveAssetData.get(j) + " Org Site is " +
						// getMoveAssetSite.get(j) + "new custno is " + MeterData.get(getMoveAssetData.get(j)).get("custNo") + " Finish!!");
						// // CheckImpLocExist(MeterData.get(getMoveAssetData.get(j)));
						// // doAMIMove(MeterData.get(getMoveAssetData.get(j)));
						// CheckImpLocExist(MeterData.get(getMoveAssetData.get(j)));
						// doAMIAddACT(MeterData.get(getMoveAssetData.get(j)));
						// if (AMIInsert.toBeSaved())
						// AMIInsert.save();
						//
						// }

						ASSET_NBS_Insert.setWhere("1=2");
						ASSET_NBS_Insert.reset();
						for (int j = 0; j < getNotFindDataNbs.size(); j++) {// not find the meter in asest
							MboRemote addNbsMbo = ASSET_NBS_Insert.add();
							addNbsMbo.setValue("CONTRACTTYPE", MeterData.get(getNotFindDataNbs.get(j)).get("type"), 11L);
							addNbsMbo.setValue("SECTION", MeterData.get(getNotFindDataNbs.get(j)).get("sec"), 11L);
							addNbsMbo.setValue("ASSETNUM", MeterData.get(getNotFindDataNbs.get(j)).get("meterId"), 11L);

						}
						if (ASSET_NBS_Insert.toBeSaved())
							ASSET_NBS_Insert.save();

						batchIndex = 0;
						if (setWhereClause.length() > 0)
							setWhereClause.delete(0, setWhereClause.length());
						setWhereClause.append("ASSETNUM IN(");
						getNotFindData.clear();
						getNotFindDataNbs.clear();
						getMoveAssetData.clear();

						AMIInsert.clear();
						// AMIInsert.cleanup();
						AMI_Service_Insert.clear();
						// AMI_Service_Insert.cleanup();
						locationsSet.clear();
						// locationsSet.cleanup();
						AssetspecSet.clear();
						// AssetspecSet.cleanup();
					}

					// doAMIAddACT(MeterData.get(i));

					if ((count + 1) >= doendindex)
						break;
				}

				count = count + 1;
				save_index = save_index + 1;

			}

			if (AMIInsert.toBeSaved())
				AMIInsert.save();

		} catch (Exception e) {
			KeepLog("Error " + e.getLocalizedMessage());
		}
		KeepLog("End Insert Data");
		return true;
	}
}
