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

public class CustHesGetDeviceListCrontask extends SimpleCronTask {
	private static CrontaskParamInfo[]	params;
	MXLogger							myLogger	= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public void doRequestClause(String apiUrl, String sendPayLoad) throws RemoteException, MXException, Exception {
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
		response.append("Get response from hesGetDeviceList is:");
		while ((line = reader.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		myLogger.debug(response.toString());
		reader.close();
	}

	protected void doRequestClause() throws RemoteException {
		StringBuilder returnPayload = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String gettimestamp = sdf.format(new Date());
		String sessionId = UUID.randomUUID().toString();
		StringBuilder messageLog = new StringBuilder();
		try {
			returnPayload.append("{\"time\":\"");
			returnPayload.append(gettimestamp);
			returnPayload.append("\",\"session\":\"");
			returnPayload.append(sessionId);
			returnPayload.append("\",\"timeStart\":");
			returnPayload.append(getParamAsString("timeStart"));
			returnPayload.append("}");
			
			if (messageLog.length() > 0)
				messageLog.delete(0, messageLog.length());
			messageLog.append("send to Hes getDeviceList request is ");
			messageLog.append(returnPayload.toString());
			myLogger.debug(messageLog.toString());
			doRequestClause(getParamAsString("requestUrl"), returnPayload.toString());			
		} catch (Exception e) {
			if (messageLog.length() > 0)
				messageLog.delete(0, messageLog.length());
			messageLog.append("Crontask hes getDeviceList error!!");
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
			myLogger.debug("Cron task send getDeviceList to hes");
			doRequestClause();
			myLogger.debug("Crontask hes request getDeviceList finish!!");

		} catch (Exception e) {
			myLogger.debug("Crontask hes getDeviceList err2!!" + e.getLocalizedMessage());
		}
	}
	// TODO Auto-generated method stub

}
