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

public class CustDeleteMeterDataCrontask extends SimpleCronTask {
	public MXLogger						myLogger					= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public String[]						EXCEL_COL_NAME_Under_Meter	= { "custNo", "meterId", "type", "sec",
																	"delFlag", "subtype" };

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
		params = new CrontaskParamInfo[1];
		params[0] = new CrontaskParamInfo();
		params[0].setName("logPath");
		// params[5] = new CrontaskParamInfo();
		// params[5].setName("missDataPath");

	}

	@Override public void cronAction() {
		// Your custom code here. e.g. A sample code is below.

		try {
			// mxServer = MXServer.getMXServer();
			// ui = mxServer.getSystemUserInfo();
			myLogger.debug("Cron task Delete Meter Data Start!!");


			LOG_PATH = getParamAsString("logPath");
			KeepLog("start contask!!");
			doAMIMove();
			KeepLog("end contask!!");

		} catch (Exception e) {

		}
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	private void doAMIMove() {
		MboSetRemote moveAssetMbo = null;
		String meterId = null;
		int getMboCount = 0;
		StringBuilder setWhereClause = new StringBuilder();
		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			moveAssetMbo = (AssetSet) mxServer.getMboSet("ASSET", ui);
			setWhereClause.append("assetnum like '4%' and SITEID='04'");
			//setWhereClause.append("assetnum like 'TH%'");
			moveAssetMbo.setWhere(setWhereClause.toString());
			moveAssetMbo.reset();
			if (!moveAssetMbo.isEmpty()) {
				while (moveAssetMbo.getMbo(getMboCount) != null) {
					MboRemote currMbo = moveAssetMbo.getMbo(getMboCount);
					currMbo.delete();

					getMboCount++;
					if (getMboCount > 499) {
						if (moveAssetMbo.toBeSaved()) {
							moveAssetMbo.save();
							getMboCount=0;
							KeepLog("Delete Asset Batch Success");
						}

					}

				}

			}

			if (moveAssetMbo.toBeSaved())
				moveAssetMbo.save();

		} catch (Exception e) {
			KeepLog("Delete Asset Eror");
			// myLogger.debug("error message" + e.getLocalizedMessage());
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

}
