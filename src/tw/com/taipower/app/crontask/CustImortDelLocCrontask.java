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

public class CustImortDelLocCrontask extends SimpleCronTask {
	public MXLogger						myLogger			= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public String						Location_Insert;
	public String						Premise_Insert;
	public LocationSet					locationsSet;
	public MboSetRemote					AMI_Service_Insert	= null;
	public MboSetRemote					AMIInsert			= null;
	public MboSetRemote					ASSET_NBS_Insert	= null;
	public MboSetRemote					AssetspecSet		= null;
	public MXServer						mxServer;
	public UserInfo						ui;
	public ConnectionKey				conKey;
	public String						Classifty_ID;
	public MboSetRemote					classspecSet		= null;
	int									commit_cnt;
	String								LOG_PATH;
	int									dostartindex;
	int									doendindex;
	private static CrontaskParamInfo[]	params;
	static {
		params = new CrontaskParamInfo[2];
		params[0] = new CrontaskParamInfo();
		params[0].setName("LOADFILE");
		params[0].setDefault("");
		params[1] = new CrontaskParamInfo();
		params[1].setName("logPath");
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
			//dostartindex = Integer.parseInt(getParamAsString("StartIndex"));
			//doendindex = Integer.parseInt(getParamAsString("EndIndex"));

			LOG_PATH = getParamAsString("logPath");
			KeepLog("start contask!!");
			boolean checksatus = false;
			Hashtable<String, Hashtable<String, String>> GetData = new Hashtable<String, Hashtable<String, String>>();
			readDelLocation(LoadFilePath);
			KeepLog("end contask!!");
			// GetData=ReadMeterData("D:\\CP2_Data\\103_¬[ªÅ.csv");
			// GetData = ReadMeterData(LoadFilePath);
			// checksatus = InsertMeterData(GetData);
			// checksatus = compensateDate(GetData, getMissData);
			// checksatus = checkMeterImport(GetData);

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
				if (line.length() < 10)
					continue;
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

}
