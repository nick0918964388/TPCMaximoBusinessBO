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

public class CustHesChangeMeterCrontask extends SimpleCronTask {
	private MXServer					mxServer;
	private UserInfo					ui;
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
		response.append("Get response from hesChangeMeter is:");
		while ((line = reader.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		myLogger.debug(response.toString());
		reader.close();

	}

	protected void doChangeMeter() throws RemoteException {
		MboSetRemote tempGetAssetTransSet = null;

		StringBuilder setWhereClause = new StringBuilder();
		StringBuilder returnUuidPayload = new StringBuilder();
		String getData = null;
		SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MM-yyyy");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String gettimestamp = sdf.format(new Date());
		String sessionId = UUID.randomUUID().toString();
		boolean getChangeFlag = false;
		String[] splitData = new String[4];

		returnUuidPayload.append("{\"time\":\"");
		returnUuidPayload.append(gettimestamp);
		returnUuidPayload.append("\",\"session\":\"");
		returnUuidPayload.append(sessionId);
		returnUuidPayload.append("\",\"meter\":[");
		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			String getTime = sdf1.format(mxServer.getDate());
			tempGetAssetTransSet = mxServer.getMboSet("ASSETTRANS", ui);
			setWhereClause.append("TRANSDATE > to_date('");
			setWhereClause.append(getTime);
			setWhereClause.append("','dd-MM-yyyy') and memo like 'amichange%' ");
			// setWhereClause.append("memo like 'amichange%' ");
			tempGetAssetTransSet.setWhere(setWhereClause.toString());
			tempGetAssetTransSet.reset();
			if (!tempGetAssetTransSet.isEmpty()) {
				for (MboRemote currMbo = tempGetAssetTransSet.moveFirst(); currMbo != null; currMbo = tempGetAssetTransSet.moveNext()) {
					getData = currMbo.getString("memo");
					splitData = getData.split(",");
					returnUuidPayload.append("{\"oldmeterUuid\":\"");
					returnUuidPayload.append(splitData[1]);
					//returnUuidPayload.append("\",\"oldMeterId\":\"");
					//returnUuidPayload.append(currMbo.getString("assetnum"));
					returnUuidPayload.append("\",\"newMeterUuid\":\"");
					returnUuidPayload.append(splitData[3]);
					returnUuidPayload.append("\",\"newMeterId\":\"");
					returnUuidPayload.append(splitData[2]);
					returnUuidPayload.append("\"},");
					getChangeFlag = true;
				}
			}

		} catch (MXException e) {
			myLogger.debug("Crontask hes meter change err1!!" + e.getLocalizedMessage());
		}
		if (getChangeFlag == true)
			returnUuidPayload.delete(returnUuidPayload.length() - 1, returnUuidPayload.length());
		returnUuidPayload.append("]}");
		myLogger.debug("send to hes data:"+returnUuidPayload.toString());
		try {
			sendMeterExchangeToHes(getParamAsString("requestUrl"), returnUuidPayload.toString());
		} catch (Exception e) {
			myLogger.debug("Crontask hes meter change err2!!" + e.getLocalizedMessage());
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
			myLogger.debug("Cron task send meter change to hes");
			doChangeMeter();
			myLogger.debug("Crontask hes meter change finish!!");
			// Boolean isExeMDMS = getParamAsBoolean("MDMS_EXECUTE");

		} catch (Exception e) {
			myLogger.debug("Crontask hes meter change err2!!" + e.getLocalizedMessage());
		}
	}

}
