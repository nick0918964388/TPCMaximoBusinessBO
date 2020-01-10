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

public class CustKmskeyUpdateByMAMCrontask extends SimpleCronTask {
	private static CrontaskParamInfo[]	params;
	MXLogger							myLogger	= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public void sendKeyUpdateByMAMToKMS(String apiUrl, String sendPayLoad) throws RemoteException, MXException, Exception {
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

	private void doRequestClause() throws RemoteException {
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
		MboSetRemote getHesMboSet = null;
		Hashtable<String, String> getClassifyName = new Hashtable<String, String>();
		Hashtable<String, Hashtable<String, String>> hesInfoData = new Hashtable<String, Hashtable<String, String>>();
		Hashtable<String, Hashtable<String, String>> getHesSearchData = new Hashtable<String, Hashtable<String, String>>();

		String hesClassifyId = null;
		String amiClassifyId = null;
		String hesGetFromDescription = null;
		String getHesUrlinfo = null;
		ArrayList<String> getHesData = new ArrayList<String>();
		try {
			mxServer = MXServer.getMXServer();
			ui = mxServer.getSystemUserInfo();
			getAssetMboSet = mxServer.getMboSet("ASSET", ui);
			mboSetClassstructure = mxServer.getMboSet("CLASSSTRUCTURE", ui);
			getHesMboSet = mxServer.getMboSet("ZZ_HES_IMP_DATA", ui);
			// get the structureId of FAN
			mboSetClassstructure.setWhere("UPPER(CLASSIFICATIONID) IN ('HES','AMIMETER')");
			mboSetClassstructure.reset();
			if (!mboSetClassstructure.isEmpty()) {
				for (MboRemote currMbo = mboSetClassstructure.moveFirst(); currMbo != null; currMbo = mboSetClassstructure.moveNext()) {
					getClassifyName.put(currMbo.getString("CLASSSTRUCTUREID"), currMbo.getString("CLASSIFICATIONID"));
					if (currMbo.getString("CLASSIFICATIONID").equalsIgnoreCase("HES")) {
						hesClassifyId = currMbo.getString("CLASSSTRUCTUREID");
					} else if (currMbo.getString("CLASSIFICATIONID").equalsIgnoreCase("AMIMETER")) {
						amiClassifyId = currMbo.getString("CLASSSTRUCTUREID");
					}

				}
			}
			// --------------get hes information

			// getHesDomain.setWhere("ASSETNUM LIKE 'HES-%'");
			if (setWhereClause.length() > 0)
				setWhereClause.delete(0, setWhereClause.length());
			setWhereClause.append("CLASSSTRUCTUREID='");
			setWhereClause.append(hesClassifyId);
			setWhereClause.append("'");
			getAssetMboSet.setWhere(setWhereClause.toString());
			getAssetMboSet.reset();
			if (!getAssetMboSet.isEmpty()) {
				for (MboRemote currMbo = getAssetMboSet.moveFirst(); currMbo != null; currMbo = getAssetMboSet.moveNext()) {
					Hashtable<String, String> hesInfoValue = new Hashtable<String, String>();
					MboSetRemote getAssetSpec = currMbo.getMboSet("ASSETSPEC");
					hesGetFromDescription = currMbo.getString("DESCRIPTION");
					if (hesGetFromDescription.indexOf(",") > -1) {
						String[] getDataSplit = hesGetFromDescription.split(",");
						if (getDataSplit.length == 2) {

							// hesInfoValue.put("hesCode", currMbo.getString("ASSETNUM"));
							hesInfoValue.put("hesCode", getDataSplit[1]);
							for (MboRemote currMboSpec = getAssetSpec.moveFirst(); currMboSpec != null; currMboSpec = getAssetSpec.moveNext()) {
								if (currMboSpec.getString("assetattrid").equalsIgnoreCase("HESURL")) {
									hesInfoValue.put("hesUrl", currMboSpec.getString("alnvalue"));
								} else if (currMboSpec.getString("assetattrid").equalsIgnoreCase("HESIP")) {
									hesInfoValue.put("hesIp", currMboSpec.getString("alnvalue"));
								}
							}
							hesInfoData.put(getDataSplit[1], hesInfoValue);
						}
					}
				}
			} else {
				// returnUuidPayload.append("could not find hes info in mam.\"}");
				// JsonObject Robj = new JsonParser().parse(returnUuidPayload.toString()).getAsJsonObject();
				// out.print(Robj.toString());
				// out.flush();
				myLogger.debug("could not get the HES url info!!");
				return;
			}

			returnPayload.append("{\"time\":\"");
			returnPayload.append(gettimestamp);
			returnPayload.append("\",\"session\":\"");
			returnPayload.append(sessionId);
			returnPayload.append("\",\"list\":[");
			if (setWhereClause.length() > 0)
				setWhereClause.delete(0, setWhereClause.length());
			setWhereClause.append(getParamAsString("setWhereClause"));
			// if (setWhereClause.length()>0)
			// setWhereClause.delete(0, setWhereClause.length());
			// setWhereClause.append("ACTIONTYP='NEW'");
			getHesMboSet.setWhere(setWhereClause.toString());
			getHesMboSet.reset();
			if (!getHesMboSet.isEmpty()) {
				for (MboRemote currMbo = getHesMboSet.moveFirst(); currMbo != null; currMbo = getHesMboSet.moveNext()) {
					// if (hesInfoData.containsKey(currMbo.getString("HES_CODE"))) {
					// getHesUrlinfo = hesInfoData.get(currMbo.getString("HES_CODE")).get("hesUrl");
					// returnPayload.append("{");
					// returnPayload.append("\"hesUrl\":\"");
					// returnPayload.append(getHesUrlinfo);
					// returnPayload.append("\",\"keyType\":[");
					// returnPayload.append("\"guk_m\"");
					// returnPayload.append("\"ak_m\"");
					// returnPayload.append("\"guk_h\"");
					// returnPayload.append("\"ak_h\"],");
					// returnPayload.append("\"meterList\":[");
					// returnPayload.append(currMbo.getString("COMP_METER_NUM"));
					// returnPayload.append("\",");
					// }
					if (getHesData.indexOf(currMbo.getString("HES_CODE")) < 0) {
						getHesData.add(currMbo.getString("HES_CODE"));
					}

					Hashtable<String, String> tempHesData = new Hashtable<String, String>();
					tempHesData.put("hesCode", currMbo.getString("HES_CODE"));
					tempHesData.put("meterId", currMbo.getString("COMP_METER_NUM"));
					getHesSearchData.put(currMbo.getString("COMP_METER_NUM"), tempHesData);
				}
			}

			for (int count = 0; count < getHesData.size(); count++) {
				if (hesInfoData.containsKey(getHesData.get(count))) {
					getHesUrlinfo = hesInfoData.get(getHesData.get(count)).get("hesUrl");
					returnPayload.append("{");
					returnPayload.append("\"hesUrl\":\"");
					returnPayload.append(getHesUrlinfo);
					returnPayload.append("\",\"keyType\":[");
					returnPayload.append("\"guk_m\",");
					returnPayload.append("\"ak_m\",");
					returnPayload.append("\"guk_h\",");
					returnPayload.append("\"ak_h\"],");
					returnPayload.append("\"meterList\":[");
					for (String i : getHesSearchData.keySet()) {
						if (getHesSearchData.get(i).get("hesCode").equalsIgnoreCase(getHesData.get(count))) {
							returnPayload.append("\"");
							returnPayload.append(getHesSearchData.get(i).get("meterId"));
							returnPayload.append("\",");
						}
					}
					if (returnPayload.toString().substring(returnPayload.length() - 1, returnPayload.length()).equalsIgnoreCase(",")) {
						returnPayload.delete(returnPayload.length() - 1, returnPayload.length());
					}
					returnPayload.append("]},");
					findDeviceFlag = true;
				}
			}

			if (returnPayload.toString().substring(returnPayload.length() - 1, returnPayload.length()).equalsIgnoreCase(",")) {
				returnPayload.delete(returnPayload.length() - 1, returnPayload.length());
			}

			returnPayload.append("]}");

			if (messageLog.length() > 0)
				messageLog.delete(0, messageLog.length());
			messageLog.append("send to KMS keyUpdateByMAM request is ");
			messageLog.append(returnPayload.toString());
			myLogger.debug(messageLog.toString());

			if (findDeviceFlag == true)
				sendKeyUpdateByMAMToKMS(getParamAsString("requestUrl"), returnPayload.toString());

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
		params[1].setName("keyType");
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
