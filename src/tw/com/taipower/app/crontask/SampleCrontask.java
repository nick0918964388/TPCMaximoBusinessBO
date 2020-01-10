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

public class SampleCrontask extends SimpleCronTask {
	private MXServer	mxServer;
	private UserInfo	ui;
	MXLogger			myLogger	= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");
	private static CrontaskParamInfo[]	params;
	public  void sendMeterExchangeToHes(String apiUrl, String sendPayLoad) throws RemoteException, MXException, Exception {
		HttpURLConnection conn = null;
		StringBuilder response = new StringBuilder();
		String newMeterData;
		String oldMeterData;
		String newMeterUuidData;
		String oldMeterUuidData;
		MboSetRemote tempGetAmiSet = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String gettimestamp = sdf.format(new Date());
		String sessionId = UUID.randomUUID().toString();
		StringBuilder returnUuidPayload = new StringBuilder();

		ArrayList<String> MeterMessageList = new ArrayList<String>();
		returnUuidPayload.append("{\"time\":\"");
		returnUuidPayload.append(gettimestamp);
		returnUuidPayload.append("\",\"session\":\"");
		returnUuidPayload.append(sessionId);
		returnUuidPayload.append("\",\"meter\":[");

		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			tempGetAmiSet = mxServer.getMboSet("ZZ_AMI_IMP_DATA", ui);
			tempGetAmiSet.setWhere("UPLSTATUS is null");
			tempGetAmiSet.reset();
			for (MboRemote currMbo = tempGetAmiSet.moveFirst(); currMbo != null; currMbo = tempGetAmiSet.moveNext()) {
				oldMeterData = currMbo.getString("COMP_METER_NUM");
				newMeterData = currMbo.getString("EXCHANGE_METER_NUM");

				MboSetRemote getOldAssetUuid = currMbo.getMboSet("ASSETRELATION");
				MboSetRemote getNewAssetUuid = currMbo.getMboSet("ASSETEXCHANGERELATION");
				oldMeterUuidData = getOldAssetUuid.getMbo(0).getString("zz_uuid");
				newMeterUuidData = getNewAssetUuid.getMbo(0).getString("zz_uuid");

				returnUuidPayload.append("{\"oldMeterUuid\":\"");
				returnUuidPayload.append(oldMeterUuidData);
				returnUuidPayload.append("\",\"oldMeterId\":\"");
				returnUuidPayload.append(oldMeterData);
				returnUuidPayload.append("\",\"newMeterUuid\":\"");
				returnUuidPayload.append(newMeterUuidData);
				returnUuidPayload.append("\",\"newMeterId\":\"");
				returnUuidPayload.append(newMeterData);
				returnUuidPayload.append("\"}");
				// if (!TmpAssetSet.isEmpty()) {
				// currMbo.setValue("MDMS_UPLSTATUS", "Y");
				// currMbo.setValue("DESCRIPTION", "");
				// }
			}
			// if (Tmp_ZZ_NBS.toBeSaved())
			// Tmp_ZZ_NBS.save();
			// return response.toString();
		} catch (Exception ex) { // post fail!!
			myLogger.debug("Err" + ex.getLocalizedMessage());
			// for (MboRemote currMbo = Tmp_ZZ_NBS.moveFirst(); currMbo != null; currMbo = Tmp_ZZ_NBS.moveNext()) {
			//				
			// }
			// if (Tmp_ZZ_NBS.toBeSaved())
			// Tmp_ZZ_NBS.save();

		}
		returnUuidPayload.append("]}");

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
		JsonObject obj = new JsonParser().parse(returnUuidPayload.toString()).getAsJsonObject();
		writer.write(obj.toString().getBytes("UTf-8"));
		writer.flush();
		writer.close();
		os.close();

		// Get Response
		InputStream is = conn.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;

		while ((line = reader.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		reader.close();

		// return response.toString();

	}
	
	protected void doChangeMeter() {

	}
	static {
		params = new CrontaskParamInfo[0];
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
