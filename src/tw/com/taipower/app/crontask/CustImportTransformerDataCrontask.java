package tw.com.taipower.app.crontask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import psdi.app.system.CrontaskParamInfo;
import tw.com.taipower.app.asset.Asset;
import tw.com.taipower.app.asset.AssetSet;
public class CustImportTransformerDataCrontask extends SimpleCronTask {
	static MXLogger				myLogger					= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");
	static String[]				EXCEL_COL_NAME_Transformer	= { "圖號座標", "X(67)", "Y(67)", "lon", "lat" };

	static String				Location_Insert;
	static String				Premise_Insert;
	public static MboSetRemote	ZZ_Transformer_Insert		= null;
	public static MXServer		mxServer;
	public static UserInfo		ui;
	public ConnectionKey		conKey;
	static double				a							= 6378137.0;
	static double				b							= 6356752.314245;
	static double				lon0						= 121 * Math.PI / 180;
	static double				k0							= 0.9999;
	static int					dx							= 250000;
	static double				XY67_A						= 0.00001549;
	static double				XY67_B						= 0.000006521;
	static String  LOG_PATH;
	private static CrontaskParamInfo[]	params;
	static {
		params = new CrontaskParamInfo[2];
		params[0] = new CrontaskParamInfo();
		params[0].setName("WHERE");
		params[1] = new CrontaskParamInfo();
		params[1].setName("LOADFILE");
		params[1].setDefault("");

	}
	@Override public void cronAction() {
		// Your custom code here. e.g. A sample code is below.

		try {
			//mxServer = MXServer.getMXServer();
			//ui = mxServer.getSystemUserInfo();
			//myLogger.debug("Cron task MAM Update NBS Data to SOA");
			String LoadFilePath =getParamAsString("LOADFILE");
			boolean checksatus=false;
			Hashtable<Integer, Hashtable<String, String>>GetData = new Hashtable<Integer, Hashtable<String, String>>();
			//GetData=ReadMeterData("D:\\CP2_Data\\103_架空.csv");
			GetData=ReadMeterData(LoadFilePath);
			checksatus=InsertTranData(GetData);
			LOG_PATH=LoadFilePath.replace(".txt", "translog.txt");

		} catch (Exception e) {

		}
	}
	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
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

	public static Hashtable<Integer, Hashtable<String, String>> ReadMeterData(String FilePath) throws RemoteException, MXException, Exception {
		Hashtable<Integer, Hashtable<String, String>> ReadData = new Hashtable<Integer, Hashtable<String, String>>();
		Hashtable<String, String> Village_map = new Hashtable<String, String>();
		// String ls = System.getProperty("line.separator");
		// ArrayList<String> Get_CodeData = new ArrayList<String>();
		try {

			File file = new File(FilePath);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			Hashtable<String, String> cellValue = new Hashtable<String, String>();
			int count = 0;
			String[] data_split;
			while ((line = reader.readLine()) != null) {
				//if (count > 0) {
					data_split = line.split(",");
					cellValue = new Hashtable<String, String>();
					cellValue.clear();

					for (int j = 0; j < EXCEL_COL_NAME_Transformer.length; j++) {
						cellValue.put(EXCEL_COL_NAME_Transformer[j], data_split[j]);
					}

					ReadData.put(count, cellValue);
				//}
				count = count + 1;
//				if (count > 10)
//					break;
			}
			reader.close();

		} catch (Exception e) {

		}
		return ReadData;
	}

	private static void insertTransformerData(Hashtable<String, String> tran_data) throws RemoteException, MXException {
		try {
			//StringBuilder logSaveMesssage = null;
			MboRemote tmp_tran_add = ZZ_Transformer_Insert.add();
			tmp_tran_add.setValue("transformerCode", tran_data.get("圖號座標"),11L);
			tmp_tran_add.setValue("lat", Double.parseDouble(tran_data.get("lat")),11L);
			tmp_tran_add.setValue("lon", Double.parseDouble(tran_data.get("lon")),11L);
			
			ZZ_Transformer_Insert.save();
			//logSaveMesssage.append("commit success!! ");
			//logSaveMesssage.append(tran_data.get("圖號座標"));
			KeepLog("commit success!!" + tran_data.get("圖號座標"));
		} catch (Exception e) {
			//myLogger.debug("Insrt Transformer error" +e.getMessage());
			KeepLog("Data Exist" +tran_data.get("圖號座標"));
			ZZ_Transformer_Insert.clear();
			ZZ_Transformer_Insert.cleanup();
		}

	}
	private  static void KeepLog(String Message) {
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

	public  static Boolean InsertTranData(Hashtable<Integer, Hashtable<String, String>> MeterData) throws RemoteException, MXException, Exception {
		// --------------------------------------------------------------------------------------------------------------------------

		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		ZZ_Transformer_Insert = mxServer.getMboSet("ZZ_TransformerMasterData", ui);
		MboRemote add_transforme = null;

		//FileOutputStream fop = null;
		//File file;
		//String content = "This is the text content";
		//file = new File("C:\\Trans_importtime.txt");
		//fop = new FileOutputStream(file);
		//Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		//content = "start insert asset" + timestamp + "\r\n";
		//fop.write(content.getBytes());
		
		myLogger.debug("Start Insert Transformer Data!!");
		String tmp_str = "";
		try {
			for (int i = 0; i < MeterData.size(); i++) {
				// tmp_str = MeterData.get(i).get("電號");
				// CheckImpLocExist(MeterData.get(i));
				// doAMIAddACT(MeterData.get(i));
				KeepLog("start Insert" + i+" "+MeterData.get(i).get("圖號座標"));
				insertTransformerData(MeterData.get(i));
				
			}
//			if (ZZ_Transformer_Insert.toBeSaved())
//				ZZ_Transformer_Insert.save();
			
		} catch (Exception e) {
			myLogger.debug("End Insert Data");
		}
		//timestamp = new Timestamp(System.currentTimeMillis());
		//content = "end insert asset" + timestamp + "\r\n";
		//fop.write(content.getBytes());
		//fop.flush();
		//fop.close();
		KeepLog("End Transformer Insert Data");
		return true;
	}
}
