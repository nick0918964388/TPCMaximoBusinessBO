package tw.com.taipower.app.crontask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
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

public class CustHesGetDeviceNetStatusCrontask extends SimpleCronTask {

	private static CrontaskParamInfo[]	params;
	MXLogger							myLogger	= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public void sendMeterExchangeToHes(String apiUrl, String sendPayLoad) throws RemoteException, MXException, Exception {
		HttpURLConnection conn = null;
		StringBuilder response = new StringBuilder();

		URL url = new URL(apiUrl);
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestMethod("POST");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		conn.setDoInput(true); // 允許輸入流，即允許下載
		conn.setDoOutput(true); // 允許輸出流，即允許上傳
		conn.setUseCaches(false); // 設置是否使用緩存

		OutputStream os = conn.getOutputStream();
		DataOutputStream writer = new DataOutputStream(os);
		JsonObject obj = new JsonParser().parse(sendPayLoad.toString()).getAsJsonObject();
		writer.write(obj.toString().getBytes("UTf-8"));
		writer.flush();
		writer.close();
		os.close();

		// Get Response
		InputStream is = conn.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		response.append("Get response from hesGetDeviceNetStatus is:");
		while ((line = reader.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		myLogger.debug(response.toString());
		reader.close();
	}

	protected void doRequestClause() throws RemoteException {
		StringBuilder returnPayload = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		StringBuilder setRequestClause = new StringBuilder();
		StringBuilder messageLog = new StringBuilder();
		boolean findDeviceFlag = false;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String gettimestamp = sdf.format(new Date());
		String sessionId = UUID.randomUUID().toString();
		MboSetRemote getAssetMboSet = null;
		MboSetRemote mboSetClassstructure = null;
		Hashtable<String, String> getClassifyName = new Hashtable<String, String>();
		MXServer mxServer;
		UserInfo ui;
		try {

			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			getAssetMboSet = mxServer.getMboSet("ASSET", ui);
			mboSetClassstructure = mxServer.getMboSet("CLASSSTRUCTURE", ui);
			// -----get ClassStructureId-----------------------
			mboSetClassstructure.setWhere("UPPER(CLASSIFICATIONID) IN ('FAN','AMIMETER','DCU')");
			mboSetClassstructure.reset();
			if (!mboSetClassstructure.isEmpty()) {
				for (MboRemote currMbo = mboSetClassstructure.moveFirst(); currMbo != null; currMbo = mboSetClassstructure.moveNext()) {
					getClassifyName.put(currMbo.getString("CLASSSTRUCTUREID"), currMbo.getString("CLASSIFICATIONID"));
				}
			} else {
  
				myLogger.debug("Crontask hes getNetworkRoundTripTime err no device classificationid exist");
				return;
			}

			returnPayload.append("{\"time\":\"");
			returnPayload.append(gettimestamp);
			returnPayload.append("\",\"session\":\"");
			returnPayload.append(sessionId);
			returnPayload.append("\",\"device\":[");
			setWhereClause.append(getParamAsString("setWhereClause"));
			getAssetMboSet.setWhere(setWhereClause.toString());
			getAssetMboSet.reset();
			if (!getAssetMboSet.isEmpty()) {
				if (setRequestClause.length() > 0)
					setRequestClause.delete(0, setRequestClause.length());
				for (MboRemote currMbo = getAssetMboSet.moveFirst(); currMbo != null; currMbo = getAssetMboSet.moveNext()) {
					setRequestClause.append("{\"deviceUuid\":\"");
					setRequestClause.append(currMbo.getString("ZZ_UUID"));
					setRequestClause.append("\",");
					if (getClassifyName.get(currMbo.getString("CLASSSTRUCTUREID")).equalsIgnoreCase("AMIMETER")) { // Device is meter
						setRequestClause.append("\"meterId\":\"");
						setRequestClause.append(currMbo.getString("ASSETNUM"));
						setRequestClause.append("\",");
					} else { // Device is FAN DCU
						setRequestClause.append("\"meterId\":null");
						setRequestClause.append("\",");
					}
					setRequestClause.append("\"dataFlow\":");
					if (getParamAsString("dataFlow").equalsIgnoreCase("")) {
						setRequestClause.append("null");
						setRequestClause.append("},");
					} else {
						setRequestClause.append("{");
						setRequestClause.append(getParamAsString("dataFlow"));
						setRequestClause.append("},");
					}
					setRequestClause.append("\"RSSI\":");
					setRequestClause.append(getParamAsString("RSSI"));
					setRequestClause.append(",");
					setRequestClause.append("\"SNR\":");
					setRequestClause.append(getParamAsString("SNR"));
					setRequestClause.append("},");				
				}
				findDeviceFlag = true;
			}
			if (findDeviceFlag == true) {
				if (setRequestClause.length() > 0)
					setRequestClause.delete(setRequestClause.length() - 1, setRequestClause.length());
			}
			returnPayload.append(setRequestClause.toString());
			returnPayload.append("]}");

			if (messageLog.length() > 0)
				messageLog.delete(0, messageLog.length());
			messageLog.append("send to Hes getDeviceNetStatus request is ");
			messageLog.append(returnPayload.toString());
			myLogger.debug(messageLog.toString());
			
			if (findDeviceFlag == true)
				sendMeterExchangeToHes(getParamAsString("requestUrl"), returnPayload.toString());

		} catch (Exception e) {
			if (messageLog.length() > 0)
				messageLog.delete(0, messageLog.length());
			messageLog.append("Crontask hes getDeviceList getNetworkRoundTripTime err2!!");
			messageLog.append(e.getLocalizedMessage());
			myLogger.debug(messageLog.toString());
		}

	}

	static {
		params = new CrontaskParamInfo[5];
		params[0] = new CrontaskParamInfo();
		params[0].setName("requestUrl");
		params[1] = new CrontaskParamInfo();
		params[1].setName("setWhereClause");
		params[2] = new CrontaskParamInfo();
		params[2].setName("dataFlow");
		params[3] = new CrontaskParamInfo();
		params[3].setName("RSSI");
		params[4] = new CrontaskParamInfo();
		params[4].setName("SNR");
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	@Override public void cronAction() {
		StringBuilder errorLog = new StringBuilder();
		try {
			myLogger.debug("Cron task send getDeviceNetStatus to hes");
			doRequestClause();
			myLogger.debug("Crontask hes getDeviceNetStatus finish!!");

		} catch (Exception e) {
			if (errorLog.length() > 0)
				errorLog.delete(0, errorLog.length());
			errorLog.append("Crontask hes getDeviceNetStatus error");
			errorLog.append(e.getLocalizedMessage());
			myLogger.debug(errorLog.toString());
		}
	}

}
