/**
* System      : Asset Management System                                        
* Class name  : Asset.java                                                     
* Description : asset generate UUID & edit maximo description rule             
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.asset;

import java.rmi.RemoteException;
import psdi.app.asset.AssetRemote; //import psdi.mbo.MboRemote;
import psdi.mbo.DBShortcut;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote; //import psdi.mbo.MboSetRemote;
import psdi.security.ConnectionKey;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import java.util.UUID;

import com.ibm.tivoli.maximo.srvad.app.ServiceAddressSet;

public class Asset extends psdi.app.asset.Asset implements AssetRemote {
	MXLogger	myLogger	= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");	// use for save log

	MXServer	mxServer;
	UserInfo	ui;

	public Asset(MboSet ms) throws MXException, RemoteException {

		super(ms);
	}

	// public void delete(long modifier) throws MXException, RemoteException {
	// String tempServicedel = getString("SADDRESSCODE");
	// StringBuilder setWhereClause = new StringBuilder();
	//		
	//
	// //setValue("SADDRESSCODE", "", 11L);
	// // super.save();
	// // if (!Service_Check.isEmpty())
	// // Service_Check.deleteAll(2L);
	//
	// // myLogger.debug("davis tst");
	//		
	// super.delete(modifier);
	// // MboSetRemote serviceCheck = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
	// // if (!tempServicedel.equalsIgnoreCase("") && tempServicedel != null) {
	// // setWhereClause.append("UPPER(ADDRESSCODE)=UPPER('");
	// // setWhereClause.append(tempServicedel);
	// // setWhereClause.append("')");
	// // serviceCheck.setWhere(setWhereClause.toString());
	// // serviceCheck.reset();
	// // if (!serviceCheck.isEmpty()) {
	// //
	// // serviceCheck.getMbo(0).delete(11L);
	// // if (serviceCheck.toBeSaved())
	// // serviceCheck.save();
	// // }
	// //
	// // }
	// }

	public void save() throws MXException, RemoteException {
		StringBuilder setWhereClause = new StringBuilder();
		// int id = getInt("ASSETID");
		String gen_uuid;
		// mxServer = MXServer.getMXServer();
		// ui = mxServer.getSystemUserInfo();
		if (toBeAdded()) {
			MboSetRemote assetSet = getMboSet("ASSET_ASSET");
			MboSetRemote serviceSet = null;
			String Get_uuid = getString("ZZ_uuid");

			if (Get_uuid.isEmpty()) {// no value
				for (int i = 0; i < 5; i++) {
					gen_uuid = UUID.randomUUID().toString();
					assetSet.setWhere("ZZ_UUID='" + gen_uuid + "'");
					assetSet.reset();
					if (assetSet.isEmpty()) { // no uuid exist then insert
						setValue("ZZ_uuid", gen_uuid, 11L); // set the value to the
						// myLogger.debug("insert UUID SuccessFul!!");
						break;
					}
				}
			}
		}
		// String tempServicedel = getString("SADDRESSCODE");
		super.save();

	}

	public void updateDesc() throws MXException, RemoteException {
		String Get_tmpdes = getString("description");
		// setValue("description", "davistst", 2L);
		super.updateDesc();
		setValue("description", Get_tmpdes, 2L);
	}

}
