package tw.com.taipower.app.crontask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import psdi.app.system.CrontaskParamInfo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.server.SimpleCronTask;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import tw.com.taipower.app.asset.AssetSet;

public class CustMeterTypeCronTask extends SimpleCronTask {
	static MXServer							mxServer;
	static UserInfo							ui;
	static MXLogger							myLogger	= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");
	private static CrontaskParamInfo[]	params;

	public static void updateMeterType() {
		MboSetRemote meterTypeMbo = null;
		MboSetRemote assetMbo = null;
		String meterType = "GD";
		StringBuilder setWhereClause = new StringBuilder();
		Hashtable<String, Hashtable<String, String>> meterTypeHashTable = new Hashtable<String, Hashtable<String, String>>();
		Hashtable<String, String> getCellData = null;// =new Hashtable<String, String>();
		String oldMeterType = "";

		int getIndex = 0;
		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			meterTypeMbo = mxServer.getMboSet("ZZ_METERTYPE", ui);
			assetMbo = (AssetSet) mxServer.getMboSet("ASSET", ui);
			if (setWhereClause.length() > 0)
				setWhereClause.delete(0, setWhereClause.length());
			setWhereClause.append("ATTRIBUTEID in('PHASE','DIRECTION') ORDER BY METERTYPE");
			meterTypeMbo.setWhere(setWhereClause.toString());
			meterTypeMbo.reset();

			// --------------getMeterType zz_metertype table----------------
			if (!meterTypeMbo.isEmpty()) {

				for (MboRemote currMbo = meterTypeMbo.moveFirst(); currMbo != null; currMbo = meterTypeMbo.moveNext()) {
					if (getIndex > 0) {
						if (!oldMeterType.equalsIgnoreCase(currMbo.getString("METERTYPE"))) {
							
							meterTypeHashTable.put(oldMeterType, getCellData);
							oldMeterType = currMbo.getString("METERTYPE");
							getCellData = new Hashtable<String, String>();
						}
					}
					if (getIndex == 0) {
						getCellData = new Hashtable<String, String>();
						oldMeterType = currMbo.getString("METERTYPE");
					}
					getCellData.put(currMbo.getString("ATTRIBUTEID"), currMbo.getString("VALUE"));
					getIndex++;
				}

			}
			meterTypeHashTable.put(oldMeterType, getCellData);
			if (setWhereClause.length() > 0)
				setWhereClause.delete(0, setWhereClause.length());
			setWhereClause.append("ASSETNUM='");
			setWhereClause.append("GD18679914");
			setWhereClause.append("'");
			assetMbo.setWhere(setWhereClause.toString());
			if (!assetMbo.isEmpty()) {
				MboSetRemote getAssetSpec = assetMbo.getMbo(0).getMboSet("ASSETSPEC");
				if (!getAssetSpec.isEmpty()) {
					
					getAssetSpec.setWhere("ASSETATTRID IN ('PHASELINE','DIRECTION')");
					getAssetSpec.reset();
					if (!getAssetSpec.isEmpty()) {
						for (MboRemote currMbo = getAssetSpec.moveFirst(); currMbo != null; currMbo = getAssetSpec.moveNext()) {
							if (currMbo.getString("ASSETATTRID").equalsIgnoreCase("PHASELINE")) {
								if(meterTypeHashTable.get(meterType).get("PHASE").indexOf("1")>-1)
									currMbo.setValue("ALNVALUE", "單相三線");
								else if(meterTypeHashTable.get(meterType).get("PHASE").indexOf("3")>-1)
									currMbo.setValue("ALNVALUE", "三相四線");
								//currMbo.setValue("ALNVALUE", meterTypeHashTable.get(meterType).get("PHASE"));
							} else if (currMbo.getString("ASSETATTRID").equalsIgnoreCase("DIRECTION")) {
								currMbo.setValue("ALNVALUE", meterTypeHashTable.get(meterType).get("DIRECTION"));
							}

						}
					}
				}

				if (assetMbo.toBeSaved())
					assetMbo.save();
			}

		} catch (Exception e) {
			myLogger.debug("crontask custmetertype err:"+e.getLocalizedMessage());
		}
	}

	static {
		params = new CrontaskParamInfo[1];
		params[0] = new CrontaskParamInfo();
		params[0].setName("requestUrl");
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	@Override public void cronAction() {
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
			// SaveNBSData("http://127.0.0.1:1880/NBS_TST", isExeMDMS);
			// End Sample Code
		} catch (Exception e) {

		}
	}
	// TODO Auto-generated method stub

}
