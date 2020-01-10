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

public class CustKmsGetCustomerIdCrontask extends SimpleCronTask {
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

		response.append("Get response from KMS GetCustomerId:");
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String gettimestamp = sdf.format(new Date());
		String sessionId = UUID.randomUUID().toString();
		StringBuilder messageLog = new StringBuilder();
		String getMeterClassId = null;
		boolean findDeviceFlag = false;
		MXServer mxServer;
		UserInfo ui;
		MboSetRemote getAssetMboSet = null;
		MboSetRemote mboSetClassstructure = null;
		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			getAssetMboSet = mxServer.getMboSet("ASSET", ui);
			mboSetClassstructure = mxServer.getMboSet("CLASSSTRUCTURE", ui);
			// get the structureId of FAN
			mboSetClassstructure.setWhere("UPPER(CLASSIFICATIONID) = 'AMIMETER'");
			mboSetClassstructure.reset();
			if (!mboSetClassstructure.isEmpty()) {
				getMeterClassId = mboSetClassstructure.getMbo(0).getString("CLASSSTRUCTUREID");
			}

			returnPayload.append("{\"time\":\"");
			returnPayload.append(gettimestamp);
			returnPayload.append("\",\"session\":\"");
			returnPayload.append(sessionId);
			returnPayload.append("\",\"device\":[");
			setWhereClause.append(getParamAsString("setWhereClause"));
			if (getMeterClassId != null) {
				setWhereClause.append(" AND CLASSSTRUCTUREID = '");
				setWhereClause.append(getMeterClassId);
				setWhereClause.append("'");
			}
			getAssetMboSet.setWhere(setWhereClause.toString());
			getAssetMboSet.reset();
			if (!getAssetMboSet.isEmpty()) {
				for (MboRemote currMbo = getAssetMboSet.moveFirst(); currMbo != null; currMbo = getAssetMboSet.moveNext()) {
					returnPayload.append("\"");
					returnPayload.append(currMbo.getString("ASSETNUM"));
					returnPayload.append("\",");				
				}
				findDeviceFlag = true;
			}
			if (findDeviceFlag == true) {
				returnPayload.delete(returnPayload.length() - 1, returnPayload.length());
			}
			returnPayload.append("]}");

			if (messageLog.length() > 0)
				messageLog.delete(0, messageLog.length());
			messageLog.append("send to KMS GetCustomerId request is ");
			messageLog.append(returnPayload.toString());
			myLogger.debug(messageLog.toString());

			if (findDeviceFlag == true)
				sendMeterExchangeToHes(getParamAsString("requestUrl"), returnPayload.toString());

		} catch (Exception e) {

			if (messageLog.length() > 0)
				messageLog.delete(0, messageLog.length());
			messageLog.append("Crontask KMS GetCustomerId error!!");
			messageLog.append(e.getLocalizedMessage());
			myLogger.debug(messageLog.toString());
		}
	}

	static {
		params = new CrontaskParamInfo[2];
		params[0] = new CrontaskParamInfo();
		params[0].setName("requestUrl");
		params[1] = new CrontaskParamInfo();
		params[1].setName("timeStart");
	}

	public CrontaskParamInfo[] getParameters() throws MXException, RemoteException {
		return params;
	}

	@Override public void cronAction() {
		try {
			myLogger.debug("Crontask send GetCustomerId to KMS");
			doRequestClause();
			myLogger.debug("Crontask KMS GetCustomerId finish!!");

		} catch (Exception e) {
			myLogger.debug("Crontask KMS GetCustomerId  error!!" + e.getLocalizedMessage());
		}
	}
	// TODO Auto-generated method stub

}
