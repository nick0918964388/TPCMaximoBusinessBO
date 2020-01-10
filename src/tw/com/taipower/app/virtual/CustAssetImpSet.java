/**
* System      : Asset Management System                                        
* Class name  : CustAssetImpSet.java                                                     
* Description : Import and preview for asset,amidata and hes         
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.virtual;

import com.ibm.tivoli.maximo.srvad.app.ServiceAddressSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

import psdi.app.assetcatalog.Classification;
import psdi.app.assetcatalog.SpecificationMboRemote;
import psdi.app.location.LocAncestorSet;
import psdi.app.location.LocHierarchySet;
import psdi.app.location.LocSystemSet;
import psdi.app.location.LocationSet;
import psdi.mbo.DBShortcut;
import psdi.mbo.Mbo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboServerInterface;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValueData;
import psdi.mbo.NonPersistentMboSet;
import psdi.mbo.NonPersistentMboSetRemote;
import psdi.mbo.SqlFormat;
import psdi.security.ConnectionKey;
import psdi.security.ProfileRemote;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import tw.com.taipower.app.asset.Asset;
import tw.com.taipower.app.asset.AssetSet;
import tw.com.taipower.app.util.StringUtil;

public class CustAssetImpSet extends NonPersistentMboSet implements
		NonPersistentMboSetRemote {
	MXLogger				myLogger			= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");

	int						importStatus;
	long					get_count1;
	long					get_count2;
	long					get_count3;
	public boolean			Replace_Flag;
	public String			R_Site_ID;
	public LocationSet		locationsSet;
	// (LocationSet)MXServer.getMXServer().
	public LocSystemSet		LS_Set;
	public MboSetRemote		AMI_Insert			= null;
	public MboSetRemote		HES_Insert			= null;
	public MboSetRemote		AMI_Service_Insert	= null;
	public MboSetRemote		ASSET_NBS_Insert	= null;
	public MboSetRemote		AssetspecSet		= null;
	public MboSetRemote		meterTypeMboSet		= null;
	public MXServer			mxServer;
	public UserInfo			ui;
	public ConnectionKey	conKey;
	public String			Classifty_ID;
	public String			Location_Insert;
	public String			Premise_Insert;
	public MboSetRemote		classspecSet		= null;

	public CustAssetImpSet(MboServerInterface ms) throws RemoteException {
		super(ms);
	}

	protected Mbo getMboInstance(MboSet ms) throws MXException, RemoteException {
		return new CustAssetImp(ms);
	}

	public MboRemote setup() {
		return null;
	}

	// -----------V1.5 Add Start-------------------
	// this is for architecture use
	public void CheckImpLocExist(MboRemote LocData) throws RemoteException, MXException {
		String[] attrs = { "ELECT_NUM" };
		MboValueData[] valData = LocData.getMboValueData(attrs);

		// int tmp_count = 0;
		// String tmp_ser = "";
		// String tmp_data = "";
		Location_Insert = "";
		try {
			// if (!AMI_Insert.isEmpty()) {
			// tmp_count = rs.getInt(1);
			// ------------v1.7 add start-----------------------------
			// tmp_count = AMI_Insert.count();
			// tmp_ser = String.format("%04d", tmp_count + 1);
			// tmp_data = valData[0].getData() + "-" + tmp_ser + "-P";
			// Location_Insert = valData[0].getData() + "-" + tmp_ser;
			// AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ");
			// AMI_Service_Insert.reset();
			// Location_Insert = UUID.randomUUID().toString();
			// Premise_Insert=UUID.randomUUID().toString();
			// AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + Location_Insert + "') AND UPPER(ORGID) = 'TPC' ");
			// AMI_Service_Insert.reset();
			// ----------v1.7 add end ------------------------------------------------------
			// if (AMI_Service_Insert.isEmpty()) {
			// MboRemote newService = AMI_Service_Insert.add();
			// newService.setValue("ADDRESSCODE", Premise_Insert, 11L);
			// newService.setValue("LANGCODE", "ZHT", 11L);
			// newService.setValue("ORGID", "TPC", 11L);
			// newService.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
			// AMI_Service_Insert.save();
			// }else{
			// myLogger.debug("service address code uuid duplicate!");
			// }

			// myLogger.debug("Check how many meter install under elect_num");
			// } else { // add new custno
			// --------v1.7 add start---------------------
			// tmp_count = 0;
			// tmp_ser = String.format("%04d", tmp_count + 1);
			// tmp_data = valData[0].getData() + "-" + tmp_ser + "-P";
			// Location_Insert = valData[0].getData() + "-" + tmp_ser;
			// AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ");
			// AMI_Service_Insert.reset();
			// mxServer.getMXServer().getProperty("tpc.assetnum.length");
			Premise_Insert = UUID.randomUUID().toString();
			Location_Insert = UUID.randomUUID().toString();
			AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + Premise_Insert + "') AND UPPER(ORGID) = 'TPC' ");
			AMI_Service_Insert.reset();
			myLogger.debug("Step1_1 create premise");
			if (AMI_Service_Insert.isEmpty()) {
				MboRemote newService = AMI_Service_Insert.add();
				newService.setValue("ADDRESSCODE", Premise_Insert, 11L);
				newService.setValue("LANGCODE", "ZHT", 11L);
				newService.setValue("ORGID", "TPC", 11L);
				newService.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				AMI_Service_Insert.save();
			} else {
				myLogger.debug("service address code uuid duplicate!");
			}
			myLogger.debug("Step1_1 create premise done");
			// }
		} catch (Exception e) {

			myLogger.debug("Create Service Err:" + e.getMessage());
		}
		myLogger.debug("Step1_2 create location");
		locationsSet.setWhere("UPPER(LOCATION) = UPPER('" + Location_Insert + "')" + "AND UPPER(SITEID) = UPPER('" + valData[0].getData().substring(0, 2) + "')"); //

		try {
			if (!locationsSet.isEmpty()) { // Location exist

			} else {
				MboRemote newLocation = locationsSet.add();
				newLocation.setValue("LOCATION", Location_Insert, 11L);
				newLocation.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				newLocation.setValue("SADDRESSCODE", Premise_Insert, 11L);

				if (mxServer.getProperty("mxe.hostname").equalsIgnoreCase("vm-win2012r2"))
					newLocation.setValue("TYPE", "OPERATING", 11L);
				else
					newLocation.setValue("TYPE", "作業中", 11L);

				locationsSet.save();
				try {
					MboSetRemote locHierarchySet = null;
					locHierarchySet = newLocation.getMboSet("LOCHIERARCHY");
					MboRemote newHierarchy = locHierarchySet.add();
					newHierarchy.setValue("CHILDREN", 0, 11L);
					newHierarchy.setValue("LOCATION", Location_Insert, 11L);
					newHierarchy.setValue("SYSTEMID", valData[0].getData().substring(0, 2), 11L);
					newHierarchy.setValue("PARENT", valData[0].getData().substring(0, 2), 11L);
					locHierarchySet.save();
				} catch (MXException e) {
					myLogger.debug("Save Locate LOCHIERARCHY:" + e.getMessage());
					throw e;
				}
			}
			// try {
			// locationsSet.setWhere("UPPER(LOCATION) = UPPER('" + "06" + "')");
			// MboRemote newLocation = locationsSet.getMbo(0);
			// MboSetRemote locHierarchySet = null;
			// locHierarchySet = (LocHierarchySet)
			// newLocation.getMboSet("LOCHIERARCHY");
			// MboRemote newHierarchy = locHierarchySet.add();
			// newHierarchy.setValue("LOCATION", "06", 11L);
			// newHierarchy.setValue("SYSTEMID", "06", 11L);
			// locHierarchySet.save();
			// } catch (MXException e) {
			// myLogger.debug("Save Locate Err:" + e.getMessage());
			// throw e;
			// }
		} catch (Exception e) {

			myLogger.debug("Save Locate Err:" + e.getMessage());
		}
		myLogger.debug("Step1_2 create location done");
	}

	public void CheckImpLoc(MboRemote LocData, DBShortcut DB_Use)
			throws RemoteException, MXException {

		String[] attrs = { "ELECT_NUM", "OPER_TYPE" };
		MboValueData[] valData = LocData.getMboValueData(attrs);
		// String LOC_Tmp;

		// LOC_Tmp = LocData.getString("elect_num");
		// locationsSet = (LocationSet)MXServer.getMXServer().
		// getMboSet("LOCATIONS", MXServer.getMXServer().getSystemUserInfo());
		Replace_Flag = false;
		myLogger.debug("Start Check Loc!! " + "UPPER(LOCATION) = UPPER('"
				+ valData[0].getData() + "')");
		locationsSet.setWhere("UPPER(LOCATION) = UPPER('" +
				valData[0].getData() + "')");
		locationsSet.reset();
		String saSQL = "select SITEID from LOCATIONS WHERE "
				+ "UPPER(LOCATION) = UPPER('" + valData[0].getData() + "')";

		// ResultSet rs = DB_Use.executeQuery(saSQL);
		try {
			if (!locationsSet.isEmpty()) { // Location exist
				Replace_Flag = true;
				// R_Site_ID = rs.getString(1);
				MboRemote oldLocation = locationsSet.getMbo(0);
				R_Site_ID = oldLocation.getString("SITEID");
				myLogger.debug("Location Old Site_ID 0Use" + R_Site_ID);
			} else {
				MboRemote newLocation = locationsSet.add();
				newLocation.setValue("LOCATION", valData[0].getData(), 11L);
				// Tst tmp
				newLocation.setValue("TYPE", valData[1].getData(), 11L);
				newLocation.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				// newLocation.setValue("SITEID", valData[2].getData(), 11L);
				// newLocation.setValue("TYPE", "OPERATING", 11L);
				// newLocation.setValue("SITEID", "TPCSITE", 11L);
				locationsSet.save();
				// myLogger.debug("Save Locate Record Finish"
				// + valData[0].getData() + " " + valData[1].getData()
				// + " " + valData[2].getData());
			}
		} catch (Exception e) {

		}
	}

	private void doAMIDelACT(MboRemote amiMboImp)
			throws RemoteException, MXException, Exception {
		String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "METER_NUM", "REMOVE_TIME" };
		MboValueData[] valData = amiMboImp.getMboValueData(attrs);
		StringBuilder setWhereClause = new StringBuilder();

		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());

		setWhereClause.append("UPPER(ASSETNUM) = UPPER('");
		setWhereClause.append(valData[1].getData());
		setWhereClause.append("')  AND UPPER(STATUS) <> 'INSTOCK'");

		AMI_Insert.setWhere(setWhereClause.toString());
		AMI_Insert.reset();
		try {
			if (!AMI_Insert.isEmpty()) { // 電號 exist
				for (MboRemote currMbo = AMI_Insert.moveFirst(); currMbo != null; currMbo = AMI_Insert.moveNext()) {
					((Asset) currMbo).changeStatus("INSTOCK", mxServer.getDate(), "Remove Meter from status instock");
					((Asset) currMbo).moveAssetWithinNonInventory("LOCATION_INV", "amiremoveby", mxServer.getDate(), "MAXADMIN", currMbo.getString("wonum"), currMbo.getString("newparent"), true, true, true);
				}
				AMI_Insert.save();
				importStatus = 1;
			} else { // 電號不在 新增 資料
				importStatus = 4;
			}
		} catch (Exception e) {
			myLogger.debug("ami error:" + e.getLocalizedMessage());

		}
	}

	private void doAMIUpdACT(MboRemote amiMboImp)
			throws RemoteException, MXException, Exception {
		String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "EXCHANGE_METER_NUM", "REMOVE_METER_NUM", "REMOVE_TIME" };
		StringBuilder setWhereClause = new StringBuilder();
		MboValueData[] valData = amiMboImp.getMboValueData(attrs);

		String oldMeterId = valData[1].getData();
		String newMeterId = valData[2].getData();
		String oldUuid = null;

		String oldLocation = "";
		String oldSaddresscode = "";
		String newMeterUuid = "";
		StringBuilder memoClause = new StringBuilder();
		MboRemote assetmbo = null;
		AssetSet newAssetSet = null;
		AssetSet oldAssetSet = null;
		// newAssetSet = (AssetSet)mxServer.getMboSet("ASSET", ui);
		// newAssetSet.setWhere("UPPER(ASSETNUM) = UPPER('" + AssetNumTmp +
		// "')");
		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());

		setWhereClause.append("ASSETNUM='");
		setWhereClause.append(oldMeterId);
		setWhereClause.append("' AND SITEID='");
		setWhereClause.append(valData[0].getData().subSequence(0, 2));
		setWhereClause.append("'");

		// oldAssetSet = (AssetSet) amiMboImp.getMboSet("$ASSET", "ASSET", setWhereClause.toString());
		// oldAssetSet.reset();
		AMI_Insert.setWhere(setWhereClause.toString());
		AMI_Insert.reset();
		try {
			if (AMI_Insert.isEmpty()) {
				importStatus = 4;// data does not exist
			} else {
				assetmbo = AMI_Insert.getMbo(0);
				String strTimeFormat1 = "yyyy/MM/dd HH:mm:ss";
				importStatus = 3;// data exist
				oldLocation = assetmbo.getString("LOCATION");
				oldSaddresscode = assetmbo.getString("SADDRESSCODE");
				oldUuid = assetmbo.getString("ZZ_UUID");
				// ((Asset) assetmbo).changeStatus("INSTOCK", mxServer.getDate(), "AMI Import change orgmeter status instock");
				// ((Asset) assetmbo).moveAssetWithinNonInventory("LOCATION_INV", "AMI Import change orgmeter status instock", mxServer.getDate(),
				// "MAXADMIN", assetmbo.getString("wonum"), assetmbo.getString("newparent"), true, true, true);
				//
				// if (oldAssetSet.toBeSaved())
				// oldAssetSet.save();

				// check update meter exist--------
				if (setWhereClause.length() > 0)
					setWhereClause.delete(0, setWhereClause.length());

				setWhereClause.append("ASSETNUM='");
				setWhereClause.append(newMeterId);
				setWhereClause.append("' AND SITEID='");
				setWhereClause.append(valData[0].getData().subSequence(0, 2));
				setWhereClause.append("'");

				newAssetSet = (AssetSet) amiMboImp.getMboSet("$ASSET", "ASSET", setWhereClause.toString());
				newAssetSet.reset();

				if (newAssetSet.isEmpty()) { // data not exist create new meter
					MboRemote newAssetMbo = newAssetSet.add();

					Date r_date = new SimpleDateFormat(strTimeFormat1).parse(valData[4].getData());
					// Date r_date = valData[4].getDataAsDate();

					newAssetMbo.setValue("ASSETNUM", newMeterId, 11L);
					newAssetMbo.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
					newAssetMbo.setValue("NEWSITE", valData[0].getData().substring(0, 2), 11L);
					// AMI_Insert.save();
					((Asset) newAssetMbo).moveAssetWithinNonInventory(oldLocation, "AMI Import Update Add New Meter!!", mxServer.getDate(),
							"MAXADMIN", newAssetMbo.getString("wonum"), newAssetMbo.getString("newparent"), true, true, true);
					// newLocation.setValue("LOCATION", valData[0].getData(), 11L);
					// -------------------------------------
					newAssetMbo.setValue("ZZ_CUSTNO", valData[0].getData(), 11L); // v1.5
					newAssetMbo.setValue("DESCRIPTION", valData[2].getData().substring(2, valData[2].getData().length()), 11L);
					newAssetMbo.setValue("STATUS", "OPERATE", 11L);
					newAssetMbo.setValue("ZZ_PSTATUS", "OPERATE", 11L); // V1.2
					newAssetMbo.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.7

					if (!Classifty_ID.equals(""))
						newAssetMbo.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);
					newAssetMbo.setValue("ZZ_INSTALLDATE", r_date, 11L);
					if (newAssetSet.toBeSaved())
						newAssetSet.save();
					newMeterUuid = newAssetMbo.getString("ZZ_UUID");

					classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");
					classspecSet.reset();
					if (!classspecSet.isEmpty()) {
						// MboSetRemote newAssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
						for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
							// newAssetspecSet.setWhere("UPPER(assetnum) = UPPER('" + valData[2].getData() + "')" +
							// " AND UPPER(assetattrid) = UPPER('" + assetSpec.getString("assetattrid") + "')");
							//
							// newAssetspecSet.reset();
							// if (newAssetspecSet.isEmpty()) {
							SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
							assetspec.addDetailInfor(newAssetMbo, assetSpec);
							assetspec.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
							// }
						}
						if (AssetspecSet.toBeSaved()) {
							AssetspecSet.save();
						}

					}
				} else {
					MboRemote updAsset = newAssetSet.getMbo(0);
					if (updAsset.getString("STATUS").equalsIgnoreCase("INSTOCK")) {
						((Asset) updAsset).changeStatus("OPERATE", mxServer.getDate(), "AMI Update New Meter status operate");
						((Asset) updAsset).moveAssetWithinNonInventory(oldLocation, "AMI Update New Meter status operate", mxServer.getDate(),
								"MAXADMIN", updAsset.getString("wonum"), updAsset.getString("newparent"), true, true, true);
						updAsset.setValue("ZZ_CUSTNO", valData[0].getData(), 11L); // v1.5
						updAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
						updAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L); // V1.2
						updAsset.setValue("SADDRESSCODE", oldSaddresscode, 11L); // V1.2
						updAsset.setValue("DESCRIPTION", valData[2].getData().substring(2, valData[2].getData().length()), 11L);
						if (newAssetSet.toBeSaved())
							newAssetSet.save();
						newMeterUuid = updAsset.getString("ZZ_UUID");
					}
				}
				// ------------------do old device change status--------------------------------
				if (memoClause.length() > 0)
					memoClause.delete(0, memoClause.length());
				memoClause.append("amichangeby,");
				memoClause.append(oldUuid);
				memoClause.append(",");
				memoClause.append(newMeterId);
				memoClause.append(",");
				memoClause.append(newMeterUuid);
				((Asset) assetmbo).changeStatus("INSTOCK", mxServer.getDate(), "AMI Import change orgmeter status instock");
				((Asset) assetmbo).moveAssetWithinNonInventory("LOCATION_INV", memoClause.toString(), mxServer.getDate(),
						"MAXADMIN", assetmbo.getString("wonum"), assetmbo.getString("newparent"), true, true, true);

				if (AMI_Insert.toBeSaved())
					AMI_Insert.save();

			}
		} catch (Exception e) {
			myLogger.debug("ami upd error:" + e.getLocalizedMessage());

		}
	}

	private void doAMIAddACT(MboRemote amiMboImp, String Inser_Loc)
			throws RemoteException, MXException, Exception {
		String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "EXCHANGE_METER_NUM", "REMOVE_TIME" };
		MboValueData[] valData = amiMboImp.getMboValueData(attrs);
		AMI_Insert.setWhere("UPPER(ASSETNUM) = UPPER('" + valData[1].getData() + "')");
		AMI_Insert.reset();
		try {
			if (!AMI_Insert.isEmpty()) { // 電號 exist //V1.2
				// ---V1.6 Add Start-------------------------------------
				MboRemote newAsset = AMI_Insert.getMbo(0);
				if (newAsset.getString("STATUS").equalsIgnoreCase("INSTOCK")) {
					String strTimeFormat1 = "yyyy/MM/dd HH:mm:ss";
					Date parseDate = new SimpleDateFormat(strTimeFormat1).parse(valData[3].getData());
					((Asset) newAsset).changeStatus("NOCOMM", mxServer.getDate(), "NBS New Meter from status instock");
					((Asset) newAsset).moveAssetWithinNonInventory(Location_Insert, "NBS New Meter from status instock", mxServer.getDate(), "MAXADMIN", newAsset.getString("wonum"), newAsset.getString("newparent"), true, true, true);
					newAsset.setValue("LOCATION", Location_Insert, 11L);
					newAsset.setValue("ZZ_CUSTNO", valData[0].getData(), 11L); // v1.5
					newAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
					// newAsset.setValue("STATUS", "OPERATE", 11L);
					newAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L); // V1.2
					newAsset.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.7

					if (!Classifty_ID.equals(""))
						newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);
					// newLocation.setValue("ZZ_INSTALLDATE", r_date, 11L);
					newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);

					// ---V1.2 Add Start
					if (AMI_Insert.toBeSaved())
						AMI_Insert.save();
					// MboSetRemote classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
					classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");

					if (!classspecSet.isEmpty()) {
						// MboSetRemote newAssetspecSet = mxServer.getMboSet("ASSETSPEC", MXServer.getMXServer().getSystemUserInfo());
						for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
							AssetspecSet.setWhere("UPPER(assetnum) = UPPER('" + valData[1].getData() + "')" +
									" AND UPPER(assetattrid) = UPPER('" + assetSpec.getString("assetattrid") + "')");

							AssetspecSet.reset();
							if (AssetspecSet.isEmpty()) {
								SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
								assetspec.addDetailInfor(AMI_Insert.getMbo(0), assetSpec);
								assetspec.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
							}
						}
						if (AssetspecSet.toBeSaved()) {
							AssetspecSet.save();
						}
					}
					importStatus = 1;
				}

				// ---V1.6 Add End---------------------------------------
				// Insert_Status = 2; V1.6
			} else { // 電號不在 新增 資料
				// CheckImpLocExist(amiMboImp); //V1.6
				// myLogger.debug("Step2_1 create asset start!!");
				MboRemote newAsset = AMI_Insert.add();
				String strTimeFormat = "yyyy-MM-dd HH:mm:ss"; // Davis Differ
				String strTimeFormat1 = "yyyy/MM/dd HH:mm:ss";
				Date parseDate = new SimpleDateFormat(strTimeFormat1).parse(valData[3].getData());
				// Date r_date = valData[3].getDataAsDate();

				newAsset.setValue("ASSETNUM", valData[1].getData(), 11L);

				if (!valData[0].getData().equals("")) {
					newAsset.setValue("LOCATION", Inser_Loc, 11L);
					newAsset.setValue("ZZ_CUSTNO", valData[0].getData(), 11L); // v1.5
					newAsset.setValue("STATUS", "NOCOMM", 11L);
					newAsset.setValue("ZZ_PSTATUS", "OPERATE", 11L); // V1.2
					newAsset.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.7
					newAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				} else {
					newAsset.setValue("STATUS", "INSTOCK", 11L);
					newAsset.setValue("ZZ_PSTATUS", "", 11L); // V1.2
					// newAsset.setValue("SITEID", "", 11L);
				}

				// newAsset.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);

				// newAsset.setValue("ZZ_PSTATUS", "OPERATING", 11L); // V1.2
				// newAsset.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.7
				newAsset.setValue("DESCRIPTION", valData[1].getData().substring(2, valData[1].getData().length()), 11L); // V1.7
				if (!Classifty_ID.equals(""))
					newAsset.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);

				newAsset.setValue("ZZ_INSTALLDATE", parseDate, 11L);

				// ---V1.2 Add Start
				// MboSetRemote classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
				classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");

				AMI_Insert.save();
				// myLogger.debug("Step2_2 create assetspec start!!");
				if (!classspecSet.isEmpty()) {
					// MboSetRemote newAssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
					for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
						SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
						assetspec.addDetailInfor(AMI_Insert.getMbo(0), assetSpec);

						if (!valData[0].getData().equals("")) {
							assetspec.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
						}

					}
					if (AssetspecSet.toBeSaved()) {
						AssetspecSet.save();
					}
				}
				importStatus = 1;
				// myLogger.debug("Step2_2 create assetspec done!!");
			}
		} catch (Exception e) {
			myLogger.debug("ami error:" + e.getLocalizedMessage());

		}

	}

	// -------------V1.1 Start
	public void CheckAMIImpLoc(MboRemote LocData, DBShortcut DB_Use)
			throws RemoteException, MXException {

		String[] attrs = { "ELECT_NUM" };
		MboValueData[] valData = LocData.getMboValueData(attrs);
		myLogger.debug("Start Check Loc!! " + "UPPER(LOCATION) = UPPER('"
				+ valData[0].getData() + "')");
		String saSQL = "select SITEID from LOCATIONS WHERE "
				+ "UPPER(LOCATION) = UPPER('" + valData[0].getData() + "')";

		ResultSet rs = DB_Use.executeQuery(saSQL);
		try {
			if (rs.next()) { // Location exist
				Replace_Flag = true;
				R_Site_ID = rs.getString(1);
				myLogger.debug("Location Old Site_ID Use" + R_Site_ID);
			} else {
				MboRemote newLocation = locationsSet.add();
				newLocation.setValue("LOCATION", valData[0].getData(), 11L);

				newLocation.setValue("TYPE", "作業中", 11L);
				// newLocation.setValue("TYPE", "OPERATING", 11L);

				newLocation.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				locationsSet.save();
				myLogger.debug("Save Locate Record Finish elect_num "
						+ valData[0].getData());
			}
		} catch (Exception e) {

		}
	}

	public void CheckAMIServiceAdd(MboRemote LocData, DBShortcut DB_Use)
			throws RemoteException, MXException {
		String[] attrs = { "ELECT_NUM" };
		MboValueData[] valData = LocData.getMboValueData(attrs);
		// First Find the SADDRESSCODE(ELECT_NUM_SerNo_P) in asset
		// String saSQL = "select COUNT(1) from ASSET WHERE "
		// + "UPPER(LOCATION) = UPPER('" + valData[0].getData() + "')";
		// ResultSet rs = DB_Use.executeQuery(saSQL);
		AMI_Insert.setWhere("UPPER(LOCATION) = UPPER('" + valData[0].getData() + "')");
		AMI_Insert.reset();
		int tmp_count = 0;
		String tmp_ser = "";
		String tmp_data = "";
		try {
			if (!AMI_Insert.isEmpty()) {
				// tmp_count = rs.getInt(1);
				tmp_count = AMI_Insert.count();
				tmp_ser = String.format("%04d", tmp_count + 1);
				tmp_data = valData[0].getData() + "-" + tmp_ser + "-P";

				// ---V1.3 Add Start
				AMI_Service_Insert.setWhere("UPPER(ADDRESSCODE)=UPPER('" + tmp_data + "') AND UPPER(ORGID) = 'TPC' ");
				AMI_Service_Insert.reset();
				if (AMI_Service_Insert.isEmpty()) {
					MboRemote newService = AMI_Service_Insert.add();
					newService.setValue("ADDRESSCODE", tmp_data, 11L);
					newService.setValue("LANGCODE", "ZHT", 11L);
					newService.setValue("ORGID", "TPC", 11L);
					newService.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
					AMI_Service_Insert.save();
				}

				myLogger.debug("Check how many meter install under elect_num");
			} else {
				// MboRemote newLocation = locationsSet.add();
				// newLocation.setValue("LOCATION", valData[0].getData(), 11L);
				// newLocation.setValue("TYPE", "OPERATE", 11L);
				// newLocation.setValue("SITEID",
				// valData[0].getData().substring(0, 2), 11L);
				// locationsSet.save();

			}
		} catch (Exception e) {

		}

		// saSQL = "select SITEID from SERVICEADDRESS WHERE "
		// + "UPPER(ADDRESSCODE) = UPPER('" + valData[0].getData() + "')";

	}

	// -------------V1.1 End

	// 2019/03/11 Davis Edit----------------
	public int execImpAMI_Change() throws RemoteException, MXException,
			Exception {
		int errorCnt = 0;
		// 2019/0227/Davis Edit
		FileOutputStream fop = null;
		File file;
		// String content = "This is the text content";
		// file = new File("/opt/inst/newfile.txt");
		// fop = new FileOutputStream(file);
		// Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		// content="start insert asset"+timestamp+"\r\n";
		// fop.write(content.getBytes());

		AMI_Insert = (AssetSet) mxServer.getMboSet("ASSET", ui);
		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
		// newAssetSet = (AssetSet) assetMboImp.getMboSet("$ASSET", "ASSET",
		// "ASSETNUM='" + AssetNumTmp + "'");

		// 2019/03/25 Davis Edit
		DBShortcut dbs = new DBShortcut();
		dbs.connect(conKey);
		String saSQL = "select CLASSSTRUCTUREID from CLASSSTRUCTURE WHERE "
				+ "UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')";

		ResultSet rs = dbs.executeQuery(saSQL);

		// find the CLASSSTRCTUREID

		if (rs.next()) {
			Classifty_ID = rs.getString(1);
		} else {
			Classifty_ID = "";
		}

		// 2019/03/06 Davis Edit
		// DBShortcut dbs = new DBShortcut();
		// dbs.connect(conKey);
		try {
			for (MboRemote locMboImp = moveFirst(); locMboImp != null; locMboImp = moveNext()) {
				try {

					String actType = locMboImp.getString("actiontyp");
					importStatus = 0;
					if ("NEW".equalsIgnoreCase(actType)) {
						// myLogger.debug("Step1 check location");
						if (!locMboImp.getString("elect_num").equals("")) {
							// CheckAMIImpLoc(locMboImp, dbs);
							// CheckAMIServiceAdd(locMboImp, dbs);
							CheckImpLocExist(locMboImp);
						}
						// myLogger.debug("Step1 Finish create location");
						// myLogger.debug("Step2 create asset start!!");
						doAMIAddACT(locMboImp, Location_Insert);
						// myLogger.debug("Step2 create asset done!!");
					} else if ("UPDATE".equalsIgnoreCase(actType)) {
						doAMIUpdACT(locMboImp);
					} else if ("REMOVE".equalsIgnoreCase(actType)) {
						doAMIDelACT(locMboImp);
					}
					if (importStatus == 1)
						locMboImp.setValue("impstatus", "匯入成功", 11L);
					else if (importStatus == 2)
						locMboImp.setValue("impstatus", "資料存在", 11L);
					else if (importStatus == 3)
						locMboImp.setValue("impstatus", "資料更新成功", 11L);
					else if (importStatus == 4) {
						locMboImp.setValue("impstatus", "資料不存在! ", 11L);
					} else
						locMboImp.setValue("impstatus", "狀態未定", 11L);

				} catch (MXException e) {
					locMboImp.setValue("impstatus", "匯入失敗! "
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				} catch (Exception e) {
					locMboImp.setValue("impstatus", "匯入失敗! "
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				}
			}
		} catch (MXException e) {
			throw (e);
		} catch (Exception e) {
			throw (e);
		} finally {
			dbs.close();
		}
		// timestamp = new Timestamp(System.currentTimeMillis());
		// content="end insert asset"+timestamp+"\r\n";
		// fop.write(content.getBytes());
		// fop.flush();
		// fop.close();
		return errorCnt;
	}

	private void doMeterTypeRemoveACT(MboRemote meterTypeMboImp) throws RemoteException, MXException, Exception {
		StringBuilder errClause = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		setWhereClause.append("METERTYPE='");
		setWhereClause.append(meterTypeMboImp.getString("metertype"));
		setWhereClause.append("'");
		meterTypeMboSet.setWhere(setWhereClause.toString());
		meterTypeMboSet.reset();
		if (!meterTypeMboSet.isEmpty()) {
			meterTypeMboSet.deleteAll();
			if (meterTypeMboSet.toBeSaved())
				meterTypeMboSet.save();
		}
		importStatus = 4;
	}

	private void doMeterTypeUpdateACT(MboRemote meterTypeMboImp) throws RemoteException, MXException, Exception {
		String[] getImportDefine = { "METERMANUFACTURE", "METERDIRECTION", "METERPHASE", "METERDEGREE", "METERREMOTE" };
		String[] getDescriptionDefine = { "廠牌", "單雙向", "單三相", "倍數表", "遠端啟斷" };
		String[] getAttributeDefine = { "MODELNAM", "DIRECTION", "PHASE", "DEGREE", "REMOTECONTROL" };
		MboRemote updateMeterTypeMbo = null;
		StringBuilder errClause = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		setWhereClause.append("METERTYPE='");
		setWhereClause.append(meterTypeMboImp.getString("metertype"));
		setWhereClause.append("'");
		meterTypeMboSet.setWhere(setWhereClause.toString());
		meterTypeMboSet.reset();
		if (!meterTypeMboSet.isEmpty()) {
			for (MboRemote curMbo = meterTypeMboSet.moveFirst(); curMbo != null; curMbo = meterTypeMboSet.moveNext()) {
				for (int i = 0; i < getAttributeDefine.length; i++) {
					if (curMbo.getString("ATTRIBUTEID").equalsIgnoreCase(getAttributeDefine[i])) {
						curMbo.setValue("VALUE", meterTypeMboImp.getString(getImportDefine[i]), 11L);
						break;
					}
				}
				// if(curMbo.getString("ATTRIBUTEID"))
			}
			// for(int i=0;i<getAttributeDefine.length;i++){
			//				
			// addMeterTypeMbo = meterTypeMboSet.add();
			// addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
			// addMeterTypeMbo.setValue("ATTRIBUTEID", getAttributeDefine[i], 11L);
			// addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString(getImportDefine[i]), 11L);
			// addMeterTypeMbo.setValue("DESCRIPTION", getDescriptionDefine[i], 11L);
			//				
			// }
			if (meterTypeMboSet.toBeSaved())
				meterTypeMboSet.save();
			importStatus = 3;
		}

	}

	public int execImpMeterTypeChange(MXServer mxServerUse, UserInfo userUse) throws RemoteException, MXException,
			Exception {
		int errorCnt = 0;
		// 2019/0227/Davis Edit
		FileOutputStream fop = null;
		File file;

		meterTypeMboSet = mxServerUse.getMboSet("ZZ_METERTYPE", userUse);

		// 2019/03/06 Davis Edit
		// DBShortcut dbs = new DBShortcut();
		// dbs.connect(conKey);
		try {
			for (MboRemote MeterTypeMboImp = moveFirst(); MeterTypeMboImp != null; MeterTypeMboImp = moveNext()) {
				try {

					String actType = MeterTypeMboImp.getString("actiontyp");
					importStatus = 0;
					if ("NEW".equalsIgnoreCase(actType)) {

						doMeterTypeAddACT(MeterTypeMboImp);

					} else if ("UPDATE".equalsIgnoreCase(actType)) {
						doMeterTypeUpdateACT(MeterTypeMboImp);
					} else if ("REMOVE".equalsIgnoreCase(actType)) {
						doMeterTypeRemoveACT(MeterTypeMboImp);
					}
					if (importStatus == 1)
						MeterTypeMboImp.setValue("impstatus", "匯入成功", 11L);
					else if (importStatus == 2)
						MeterTypeMboImp.setValue("impstatus", "資料存在", 11L);
					else if (importStatus == 3)
						MeterTypeMboImp.setValue("impstatus", "資料更新成功", 11L);
					else if (importStatus == 4) {
						MeterTypeMboImp.setValue("impstatus", "資料移除成功", 11L);
					} else
						MeterTypeMboImp.setValue("impstatus", "匯入失敗", 11L);

				} catch (MXException e) {
					MeterTypeMboImp.setValue("impstatus", "匯入失敗"
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				} catch (Exception e) {
					MeterTypeMboImp.setValue("impstatus", "匯入失敗"
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				}
			}
		} catch (MXException e) {
			throw (e);
		} catch (Exception e) {
			throw (e);
		} finally {

		}
		// timestamp = new Timestamp(System.currentTimeMillis());
		// content="end insert asset"+timestamp+"\r\n";
		// fop.write(content.getBytes());
		// fop.flush();
		// fop.close();
		return errorCnt;
	}

	private void doMeterTypeAddACT(MboRemote meterTypeMboImp)
			throws RemoteException, MXException, Exception {
		StringBuilder errClause = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		// String[] getImportDefine={"METERMANUFACTURE","METERDIRECTION","METERPHASE","METERDEGREE","METERREMOE"};
		// String[] getDescriptionDefine={"廠牌","單雙向","單三相","倍數表","遠端啟斷"};
		// String[] getAttributeDefine={"MODELNAM","DIRECTION","PHASE","DEGREE","REMOTECONTROL"};
		MboRemote addMeterTypeMbo = null;
//		setWhereClause.append("METERTYPE='");
//		setWhereClause.append(meterTypeMboImp.getString("metertype"));
//		setWhereClause.append("'");
		Hashtable<String, String> getAttribute = new Hashtable<String, String>();
		Hashtable<String, String> tempAttributeData = new Hashtable<String, String>();
		Hashtable<String, Hashtable<String, String>> setAttribute = new Hashtable<String, Hashtable<String, String>>();

		// meterTypeMboSet.setWhere(setWhereClause.toString());
		// meterTypeMboSet.reset();
		// if (!meterTypeMboSet.isEmpty()) {
		// for (MboRemote curMbo = meterTypeMboSet.moveFirst(); curMbo != null; curMbo = meterTypeMboSet.moveNext()) {
		// getAttribute.put(curMbo.getString("ATTRIBUTEID"), curMbo.getString("VALUE"));
		// }
		// }
		//		
		// for(int i=0;i<getAttributeDefine.length;i++){
		// if(!getAttribute.containsKey(getAttributeDefine[i])){//attributeid already exist, pass it
		// addMeterTypeMbo = meterTypeMboSet.add();
		// addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		// addMeterTypeMbo.setValue("ATTRIBUTEID", getAttributeDefine[i], 11L);
		// addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString(getImportDefine[i]), 11L);
		// addMeterTypeMbo.setValue("DESCRIPTION", getDescriptionDefine[i], 11L);
		// }
		// }

		// if (!getAttribute.containsKey("MODELNAM")) {
		// addMeterTypeMbo = meterTypeMboSet.add();
		// addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		// addMeterTypeMbo.setValue("ATTRIBUTEID", "MODELNAM", 11L);
		// addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString("METERMANUFACTURE"), 11L);
		// addMeterTypeMbo.setValue("DESCRIPTION", "廠牌", 11L);
		// }

		meterTypeMboSet.setWhere("1=2");
		meterTypeMboSet.reset();
		//---------add one metertype for attributedid is null--------------------
		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		
		
		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "MODELNAM", 11L);
		addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString("METERMANUFACTURE"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "廠牌", 11L);
		
		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "DIRECTION", 11L);
		addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString("METERDIRECTION"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "單雙向", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "PHASE", 11L);
		addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString("METERPHASE"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "單三相", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "DEGREE", 11L);
		addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString("METERDEGREE"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "倍數表", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "REMOTECONTROL", 11L);
		addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString("METERREMOTE"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "遠端啟斷", 11L);

		try {
			if (meterTypeMboSet.toBeSaved())
				meterTypeMboSet.save();
			importStatus = 1;
		} catch (Exception e) {
			errClause.append("Import MetertType Error:");
			errClause.append(e.getLocalizedMessage());
			myLogger.debug(errClause.toString());
		}
	}

	private void doProgramIdUpdateACT(MboRemote meterTypeMboImp) throws RemoteException, MXException, Exception {
		String[] getImportDefine = { "TOU", "HVLV", "ISDEMAND", "VIRTUALWORK", "AIRCON","DIRECTION" };
		String[] getDescriptionDefine = { "廠牌", "單雙向", "單三相", "倍數表", "遠端啟斷" };
		String[] getAttributeDefine = { "TOU", "HV_LV", "ISDEMAND", "VIRTUALWORK", "AIRCON","DIRECTION" };
		MboRemote updateMeterTypeMbo = null;
		StringBuilder errClause = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		setWhereClause.append("PROGRAMID='");
		setWhereClause.append(meterTypeMboImp.getString("PROGRAMID"));
		setWhereClause.append("'");
		meterTypeMboSet.setWhere(setWhereClause.toString());
		meterTypeMboSet.reset();
		if (!meterTypeMboSet.isEmpty()) {
			for (MboRemote curMbo = meterTypeMboSet.moveFirst(); curMbo != null; curMbo = meterTypeMboSet.moveNext()) {
				for (int i = 0; i < getAttributeDefine.length; i++) {
					if (curMbo.getString("ATTRIBUTEID").equalsIgnoreCase(getAttributeDefine[i])) {
						curMbo.setValue("VALUE", meterTypeMboImp.getString(getImportDefine[i]), 11L);
						break;
					}
				}
				// if(curMbo.getString("ATTRIBUTEID"))
			}
			// for(int i=0;i<getAttributeDefine.length;i++){
			//				
			// addMeterTypeMbo = meterTypeMboSet.add();
			// addMeterTypeMbo.setValue("METERTYPE", meterTypeMboImp.getString("metertype"), 11L);
			// addMeterTypeMbo.setValue("ATTRIBUTEID", getAttributeDefine[i], 11L);
			// addMeterTypeMbo.setValue("VALUE", meterTypeMboImp.getString(getImportDefine[i]), 11L);
			// addMeterTypeMbo.setValue("DESCRIPTION", getDescriptionDefine[i], 11L);
			//				
			// }
			if (meterTypeMboSet.toBeSaved())
				meterTypeMboSet.save();
			importStatus = 3;
		}

	}
	private void doProgramIdRemoveACT(MboRemote meterTypeMboImp) throws RemoteException, MXException, Exception {
		StringBuilder errClause = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();
		setWhereClause.append("PROGRAMID='");
		setWhereClause.append(meterTypeMboImp.getString("PROGRAMID"));
		setWhereClause.append("'");
		meterTypeMboSet.setWhere(setWhereClause.toString());
		meterTypeMboSet.reset();
		if (!meterTypeMboSet.isEmpty()) {
			meterTypeMboSet.deleteAll();
			if (meterTypeMboSet.toBeSaved())
				meterTypeMboSet.save();
		}
		importStatus = 4;
	}
	
	private void doProgramIdaddACT(MboRemote programIdMboImp)
			throws RemoteException, MXException, Exception {
		StringBuilder errClause = new StringBuilder();
		StringBuilder setWhereClause = new StringBuilder();

		MboRemote addMeterTypeMbo = null;
		
		meterTypeMboSet.setWhere("1==2");
		meterTypeMboSet.reset();

		//---add one programid null attributeid for display-------------
		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("PROGRAMID", programIdMboImp.getString("PROGRAMID"), 11L);
		
		
		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("PROGRAMID", programIdMboImp.getString("PROGRAMID"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "TOU", 11L);
		addMeterTypeMbo.setValue("VALUE", programIdMboImp.getString("TOU"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "TOU", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("PROGRAMID", programIdMboImp.getString("PROGRAMID"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "HV_LV", 11L);
		addMeterTypeMbo.setValue("VALUE", programIdMboImp.getString("HVLV"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "高低壓", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("PROGRAMID", programIdMboImp.getString("PROGRAMID"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "ISDEMAND", 11L);
		addMeterTypeMbo.setValue("VALUE", programIdMboImp.getString("ISDEMAND"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "倍數表", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("PROGRAMID", programIdMboImp.getString("PROGRAMID"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "VIRTUALWORK", 11L);
		addMeterTypeMbo.setValue("VALUE", programIdMboImp.getString("VIRTUALWORK"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "含虛功", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("PROGRAMID", programIdMboImp.getString("PROGRAMID"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "AIRCON", 11L);
		addMeterTypeMbo.setValue("VALUE", programIdMboImp.getString("AIRCON"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "含空調分別", 11L);

		addMeterTypeMbo = meterTypeMboSet.add();
		addMeterTypeMbo.setValue("PROGRAMID", programIdMboImp.getString("PROGRAMID"), 11L);
		addMeterTypeMbo.setValue("ATTRIBUTEID", "DIRECTION", 11L);
		addMeterTypeMbo.setValue("VALUE", programIdMboImp.getString("DIRECTION"), 11L);
		addMeterTypeMbo.setValue("DESCRIPTION", "單雙向", 11L);

		try {
			if (meterTypeMboSet.toBeSaved())
				meterTypeMboSet.save();
			importStatus = 1;
		} catch (Exception e) {
			errClause.append("Import MetertType Error:");
			errClause.append(e.getLocalizedMessage());
			myLogger.debug(errClause.toString());
		}
	}

	public int execImpProgramIdChange(MXServer mxServerUse, UserInfo userUse) throws RemoteException, MXException,
			Exception {
		int errorCnt = 0;
		// 2019/0227/Davis Edit
		FileOutputStream fop = null;
		File file;

		meterTypeMboSet = mxServerUse.getMboSet("ZZ_METERTYPE", userUse);

		// 2019/03/06 Davis Edit
		// DBShortcut dbs = new DBShortcut();
		// dbs.connect(conKey);
		try {
			for (MboRemote programIdMboImp = moveFirst(); programIdMboImp != null; programIdMboImp = moveNext()) {
				try {

					String actType = programIdMboImp.getString("actiontyp");
					importStatus = 0;
					if ("NEW".equalsIgnoreCase(actType)) {

						doProgramIdaddACT(programIdMboImp);

					} else if ("UPDATE".equalsIgnoreCase(actType)) {
						doProgramIdUpdateACT(programIdMboImp);
					} else if ("REMOVE".equalsIgnoreCase(actType)) {
						doProgramIdRemoveACT(programIdMboImp);
					}
					if (importStatus == 1)
						programIdMboImp.setValue("impstatus", "匯入成功", 11L);
					else if (importStatus == 2)
						programIdMboImp.setValue("impstatus", "資料存在", 11L);
					else if (importStatus == 3)
						programIdMboImp.setValue("impstatus", "資料更新成功", 11L);
					else if (importStatus == 4) {
						programIdMboImp.setValue("impstatus", "資料移除成功", 11L);
					} else
						programIdMboImp.setValue("impstatus", "匯入失敗", 11L);

				} catch (MXException e) {
					programIdMboImp.setValue("impstatus", "匯入失敗"
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				} catch (Exception e) {
					programIdMboImp.setValue("impstatus", "匯入失敗"
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				}
			}
		} catch (MXException e) {
			throw (e);
		} catch (Exception e) {
			throw (e);
		} finally {

		}
		// timestamp = new Timestamp(System.currentTimeMillis());
		// content="end insert asset"+timestamp+"\r\n";
		// fop.write(content.getBytes());
		// fop.flush();
		// fop.close();
		return errorCnt;
	}

	private void doHESAddACT(MboRemote amiMboImp, DBShortcut DB_Use)
			throws RemoteException, MXException, Exception {
		String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "HES_CODE" };
		MboValueData[] valData = amiMboImp.getMboValueData(attrs);
		// String saSQL =
		// "select ELECT_NUM,COMP_METER_NUM from ZZ_HES_IMP_DATA WHERE "
		// + "UPPER(COMP_METER_NUM) = UPPER('"
		// + valData[1].getData()
		// + "')";
		// ResultSet rs = DB_Use.executeQuery(saSQL);
		// V1.2
		AMI_Insert.setWhere("UPPER(ZZ_CUSTNO) = UPPER('"
				+ valData[0].getData() + "')" + " AND UPPER(ASSETNUM) = UPPER('"
				+ valData[1].getData() + "')");
		AMI_Insert.reset();
		try {
			if (!AMI_Insert.isEmpty()) { // 電號 exist //V1.2

				// This is for HES_Edit history data
				MboRemote newAsset = AMI_Insert.getMbo(0);
				newAsset.setValue("ZZ_HES_CODE", valData[2].getData(), 11L);
				AMI_Insert.save();

				MboRemote newHes = HES_Insert.add();
				newHes.setValue("ELECT_NUM", valData[0].getData(), 11L);
				newHes.setValue("COMP_METER_NUM", valData[1].getData(), 11L);
				newHes.setValue("HES_CODE", valData[2].getData(), 11L);
				newHes.setValue("ACTIONTYP", "NEW", 11L);
				newHes.setValue("EDIT_USER", ui.getLoginUserName().toString(), 11L);
				newHes.setValue("IMPTIME", mxServer.getDate(), 11L);
				newHes.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				HES_Insert.save();
				importStatus = 1;
			} else { // 電號不在 新增 資料
				// MboRemote newLocation = HES_Insert.add();
				// Date date = mxServer.getDate();
				// newLocation.setValue("ELECT_NUM", valData[0].getData(), 11L);
				// newLocation.setValue("COMP_METER_NUM", valData[1].getData(),
				// 11L);
				// newLocation.setValue("HES_CODE", valData[2].getData(), 11L);
				// newLocation.setValue("ACTIONTYP", "NEW", 11L);
				// newLocation.setValue("EDIT_USER", ui.getLoginUserName()
				// .toString(), 11L);
				// newLocation.setValue("IMPTIME", date, 11L);
				// newLocation.setValue("SITEID",
				// valData[0].getData().substring(
				// 0, 2), 11L);
				// HES_Insert.save();
				// MboRemote newHes = HES_Insert.add();
				// newHes.setValue("ELECT_NUM", valData[0].getData(), 11L);
				// newHes.setValue("COMP_METER_NUM", valData[1].getData(), 11L);
				// newHes.setValue("HES_CODE", valData[2].getData(), 11L);
				// newHes.setValue("ACTIONTYP", "NEW", 11L);
				// newHes.setValue("EDIT_USER", ui.getLoginUserName().toString(), 11L);
				// newHes.setValue("IMPTIME", mxServer.getDate(), 11L);
				// newHes.setValue("SITEID", valData[0].getData().substring(0, 2), 11L);
				// HES_Insert.save();
				// Insert_Status = 1;
				importStatus = 5;
			}
		} catch (Exception e) {
			myLogger.debug("C" + e.getLocalizedMessage());

		}
	}

	private void doHESUpdACT(MboRemote amiMboImp, DBShortcut DB_Use)
			throws RemoteException, MXException, Exception {
		String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "HES_CODE" };
		MboValueData[] valData = amiMboImp.getMboValueData(attrs);
		// String saSQL =
		// "select ELECT_NUM,COMP_METER_NUM from ZZ_HES_IMP_DATA WHERE "
		// + "UPPER(COMP_METER_NUM) = UPPER('" + valData[1].getData() + "')";
		// ResultSet rs = DB_Use.executeQuery(saSQL);
		// HES_Insert.setWhere("UPPER(COMP_METER_NUM) = UPPER('"
		// + valData[1].getData() + "')");
		AMI_Insert.setWhere("UPPER(LOCATION) = UPPER('"
				+ valData[0].getData() + "')" + " AND UPPER(ASSETNUM) = UPPER('"
				+ valData[1].getData() + "')");
		AMI_Insert.reset();
		try {
			// if (rs.next()) { // 電號 exist
			if (!AMI_Insert.isEmpty()) {
				MboRemote newAsset = AMI_Insert.getMbo(0);
				newAsset.setValue("zz_hes_code", valData[2].getData(), 11L);
				AMI_Insert.save();

				// Record the hes update history
				MboRemote updhes = HES_Insert.getMbo(0);
				Date date = mxServer.getDate();

				updhes.setValue("HES_CODE", valData[2].getData(), 11L);
				updhes.setValue("ACTIONTYP", "UPDATE", 11L);
				updhes.setValue("EDIT_USER", ui.getLoginUserName().toString(), 11L);
				updhes.setValue("IMPTIME", date, 11L);
				HES_Insert.save();

				importStatus = 1;
			} else { // 電號不在 無法更新
				importStatus = 4;
			}
		} catch (Exception e) {
			myLogger.debug("ami error:" + e.getLocalizedMessage());

		}
	}

	private void doHESDelACT(MboRemote amiMboImp, DBShortcut DB_Use)
			throws RemoteException, MXException, Exception {
		String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "HES_CODE" };
		MboValueData[] valData = amiMboImp.getMboValueData(attrs);
		String saSQL = "select ELECT_NUM,COMP_METER_NUM from ZZ_HES_IMP_DATA WHERE "
				+ "UPPER(COMP_METER_NUM) = UPPER('"
				+ valData[1].getData()
				+ "')";
		ResultSet rs = DB_Use.executeQuery(saSQL);
		try {
			if (rs.next()) { // 表號 exist
				// if (rs.getString("COMP_METER_NUM").equalsIgnoreCase(
				// valData[1].getData())) { // 電號存在 表號不存在 新增表號
				saSQL = "DELETE FROM  ZZ_HES_IMP_DATA  "
						+ " WHERE COMP_METER_NUM='" + valData[1].getData()
						+ "'";
				rs = DB_Use.executeQuery(saSQL);
				importStatus = 1;
				// } else {

				// Insert_Status = 4;
				// }
			} else { // 電號不在 新增 資料
				importStatus = 4;
			}
		} catch (Exception e) {
			myLogger.debug("ami error:" + e.getLocalizedMessage());

		}
	}

	public int execImpHES_Change() throws RemoteException, MXException,
			Exception {
		int errorCnt = 0;
		AMI_Insert = (AssetSet) mxServer.getMboSet("ASSET", ui);
		HES_Insert = mxServer.getMboSet("ZZ_HES_IMP_DATA", ui);

		// 2019/03/06 Davis Edit
		DBShortcut dbs = new DBShortcut();
		dbs.connect(conKey);
		try {
			for (MboRemote locMboImp = moveFirst(); locMboImp != null; locMboImp = moveNext()) {
				try {
					String actType = locMboImp.getString("actiontyp");
					importStatus = 0;
					if ("NEW".equalsIgnoreCase(actType)) {
						doHESAddACT(locMboImp, dbs);
					} else if ("UPDATE".equalsIgnoreCase(actType)) {
						doHESUpdACT(locMboImp, dbs);
					} else if ("REMOVE".equalsIgnoreCase(actType)) {
						doHESDelACT(locMboImp, dbs);
					}
					if (importStatus == 1)
						locMboImp.setValue("impstatus", "匯入成功", 11L);
					else if (importStatus == 2)
						locMboImp.setValue("impstatus", "資料存在", 11L);
					else if (importStatus == 3)
						locMboImp.setValue("impstatus", "資料更新", 11L);
					else if (importStatus == 4)
						locMboImp.setValue("impstatus", "資料不存在! ", 11L);
					else if (importStatus == 5)
						locMboImp.setValue("impstatus", "電表電號不存在!! ", 11L);
					else
						locMboImp.setValue("impstatus", "狀態未定", 11L);

				} catch (MXException e) {
					locMboImp.setValue("impstatus", "匯入失敗! "
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				} catch (Exception e) {
					locMboImp.setValue("impstatus", "匯入失敗! "
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				}
			}
		} catch (MXException e) {
			throw (e);
		} catch (Exception e) {
			throw (e);
		} finally {
			dbs.close();
		}
		return errorCnt;
	}

	/* Error */
	public int execImpLoc() throws RemoteException, MXException, Exception {
		int errorCnt = 0;
		get_count1 = 0;
		get_count2 = 0;
		get_count3 = 0;
		// 2019/0227/Davis Edit
		locationsSet = (LocationSet) mxServer.getMboSet("LOCATIONS", ui);
		LS_Set = (LocSystemSet) mxServer.getMboSet("LOCSYSTEM", ui);
		AMI_Insert = (AssetSet) mxServer.getMboSet("ASSET", ui);
		AMI_Service_Insert = (ServiceAddressSet) mxServer.getMboSet("SERVICEADDRESS", ui);
		ASSET_NBS_Insert = mxServer.getMboSet("ZZ_ASSETTONBS", ui); // V1.7
		AssetspecSet = mxServer.getMboSet("ASSETSPEC", ui);
		classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
		// 2019/03/06 Davis Edit

		DBShortcut dbs = new DBShortcut();
		dbs.connect(conKey);
		// 2019/03/21 Davis Edit
		String saSQL = "select CLASSSTRUCTUREID from CLASSSTRUCTURE WHERE "
				+ "UPPER(CLASSIFICATIONID) = UPPER('" + "AMIMETER" + "')";

		ResultSet rs = dbs.executeQuery(saSQL);

		// find the CLASSSTRCTUREID

		if (rs.next()) {
			Classifty_ID = rs.getString(1);
		} else {
			Classifty_ID = "";
		}

		try {
			for (MboRemote locMboImp = moveFirst(); locMboImp != null; locMboImp = moveNext()) {
				try {

					String actType = locMboImp.getString("actiontyp");
					importStatus = 0;
					if ("A".equalsIgnoreCase(actType)) {
						// ----check location exist(elect_num)
						// 2019/02/27 Davis Edit use for unknown error
						if (!locMboImp.getString("elect_num").equals("")) {
							// CheckImpLoc(locMboImp, dbs);
							CheckImpLocExist(locMboImp);

						}
						doAddACT(locMboImp);
					} else if ("U".equalsIgnoreCase(actType)) {
						doUpdACT(locMboImp);
					} else if ("D".equalsIgnoreCase(actType)) {
						// doDelACT(locMboImp, dbs);
					}
					if (importStatus == 2)
						locMboImp.setValue("impstatus", "匯入成功", 11L);
					else if (importStatus == 1)
						locMboImp.setValue("impstatus", "資料存在", 11L);
					else if (importStatus == 3)
						locMboImp.setValue("impstatus", "資料更新", 11L);
					else if (importStatus == 4) {
						locMboImp.setValue("impstatus", "資料不存在 無法更新! ", 11L);
					} else
						locMboImp.setValue("impstatus", "狀態未定", 11L);

				} catch (MXException e) {
					locMboImp.setValue("impstatus", "匯入失敗! "
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				} catch (Exception e) {
					locMboImp.setValue("impstatus", "匯入失敗! "
							+ e.getLocalizedMessage(), 11L);
					errorCnt++;
				}
			}
		} catch (MXException e) {
			throw (e);
		} catch (Exception e) {
			throw (e);
		} finally {
			dbs.close();
		}

		// myLogger.debug("count_1:"+String.valueOf(get_count1));
		// myLogger.debug("count_2:"+String.valueOf(get_count2));
		// myLogger.debug("count_3:"+String.valueOf(get_count3));
		return errorCnt;
	}

	private void doAddACT(MboRemote assetMboImp) throws RemoteException,
			MXException, Exception {
		// MboSetRemote newAssetSet = null;
		AssetSet newAssetSet = null;
		String asset_err = "";
		try {
			// 2019/02/27 Davis Edit
			// if(!assetMboImp.getString("elect_num").equals("")){
			// CheckImpLoc(assetMboImp);
			// }
			String[] attrs = { "comp_meter_num", "elect_num",
					"contract_type", "sector",
					"removemark", "meter_type", "meter_num", "siteid" };
			MboValueData[] valData = assetMboImp.getMboValueData(attrs);

			String AssetNumTmp = assetMboImp.getString("comp_meter_num");

			// String AssetNumTmp =valData[0].getData();
			MboRemote assetmbo = null;
			newAssetSet = (AssetSet) assetMboImp.getMboSet("$ASSET", "ASSET",
					"ASSETNUM='" + AssetNumTmp + "'");

			if (!newAssetSet.isEmpty()) {
				assetmbo = newAssetSet.getMbo(0);
				importStatus = 1;// data exist
				get_count1 = get_count1 + 1;
			} else {
				assetmbo = (Asset) newAssetSet.add();
				asset_err = valData[0].getData();
				assetmbo.setValue("ASSETNUM", valData[0].getData(), 11L);
				assetmbo.setValue("LOCATION", Location_Insert, 11L);
				assetmbo.setValue("ZZ_CUSTNO", valData[1].getData(), 11L);
				assetmbo.setValue("ZZ_METERTYPE", valData[5].getData(), 11L);
				assetmbo.setValue("ZZ_REMOVEMARK", valData[4].getData(), 11L);
				assetmbo.setValue("DESCRIPTION", valData[6].getData(), 11L);
				assetmbo.setValue("STATUS", "OPERATE", 11L);
				assetmbo.setValue("ZZ_PSTATUS", "OPERATE", 11L);
				assetmbo.setValue("SADDRESSCODE", Premise_Insert, 11L); // V1.7
				assetmbo.setValue("SITEID", valData[1].getData().substring(0, 2), 11L);
				// ------------V1.7 Add Start-----
				ASSET_NBS_Insert.setWhere("UPPER(ASSETNUM)=UPPER('" + valData[0].getData() + "')");
				// data not exist
				if (ASSET_NBS_Insert.isEmpty()) {
					MboRemote newNbsData = ASSET_NBS_Insert.add();
					newNbsData.setValue("ASSETNUM", valData[0].getData(), 11L);
					newNbsData.setValue("CONTRACTTYPE", valData[2].getData(), 11L);
					newNbsData.setValue("SECTION", valData[3].getData(), 11L);
				} else {
					MboRemote newNbsData = ASSET_NBS_Insert.getMbo(0);
					newNbsData.setValue("ASSETNUM", valData[0].getData(), 11L);
					newNbsData.setValue("CONTRACTTYPE", valData[2].getData(), 11L);
					newNbsData.setValue("SECTION", valData[3].getData(), 11L);
				}
				if (ASSET_NBS_Insert.toBeSaved())
					ASSET_NBS_Insert.save();
				// ---------------V1.7 End Start----------------------------------
				// ----------contract type and section------------------
				// assetmbo.setValue("ZZ_CONTRACT_KIND", valData[2].getData(), 11L);
				// assetmbo.setValue("ZZ_SECTOR", valData[3].getData(), 11L);

				if (!Classifty_ID.equals(""))
					assetmbo.setValue("CLASSSTRUCTUREID", Classifty_ID, 11L);

				// MboSetRemote classspecSet = mxServer.getMboSet("CLASSSPEC", ui);
				classspecSet.setWhere("UPPER(CLASSSTRUCTUREID) = UPPER('" + Classifty_ID + "')");

				if (!classspecSet.isEmpty()) {
					// MboSetRemote newAssetspecSet = mxServer.getMboSet("ASSETSPEC", MXServer.getMXServer().getSystemUserInfo());
					for (MboRemote assetSpec = classspecSet.moveFirst(); assetSpec != null; assetSpec = classspecSet.moveNext()) {
						SpecificationMboRemote assetspec = (SpecificationMboRemote) AssetspecSet.addAtEnd();
						assetspec.addDetailInfor(newAssetSet.getMbo(0), assetSpec);
						assetspec.setValue("SITEID", valData[1].getData().substring(0, 2), 11L);
					}
					if (AssetspecSet.toBeSaved()) {
						AssetspecSet.save();
					}
				}
				// if(!valData[1].getData().equals("")){
				if (!assetMboImp.getString("elect_num").equals("")) {
					if (Replace_Flag == true) {
						assetmbo.setValue("SITEID", R_Site_ID, 11L); // davis
						myLogger.debug("電號"
								+ assetMboImp.getString("comp_meter_num")
								+ " Site:" + R_Site_ID);
					} else {
						// assetmbo.setValue("SITEID",valData[7].getData(),
						// 11L); //davis
						assetmbo.setValue("SITEID", assetMboImp.getString("elect_num").substring(0, 2), 11L);
						// assetmbo.setValue("SITEID","TPCSITE", 11L);
						myLogger.debug("New Site_ID Use"
								+ assetMboImp.getString("siteid"));
					}
				}

				importStatus = 2;// data exist
				get_count2 = get_count2 + 1;
			}
			if (assetmbo != null) {
				if (newAssetSet.toBeSaved()) {
					newAssetSet.save();
					newAssetSet.close();
				}
			} else if (importStatus == 2) {
				get_count3 = get_count3 + 1;
			}

		} catch (MXException e) {

			myLogger.debug("Err msg " + asset_err);
			throw e;
		} catch (Exception e) {

			throw e;
		}
	}

	private void doUpdACT(MboRemote assetMboImp) throws RemoteException,
			MXException, Exception {
		try {
			if (!assetMboImp.getString("elect_num").equals("")) {
				// CheckImpLoc(assetMboImp);
			}
			String AssetNumTmp = assetMboImp.getString("comp_meter_num");
			MboRemote assetmbo = null;
			AssetSet newAssetSet = null;
			newAssetSet = (AssetSet) assetMboImp.getMboSet("$ASSET", "ASSET",
					"ASSETNUM='" + AssetNumTmp + "'");
			if (!newAssetSet.isEmpty()) {
				assetmbo = newAssetSet.getMbo(0);
				importStatus = 3;// data exist
				assetmbo.setValue("ASSETNUM", assetMboImp.getString("comp_meter_num"), 11L);
				assetmbo.setValue("LOCATION", assetMboImp.getString("elect_num"), 11L);
				assetmbo.setValue("ZZ_CONTRACT_KIND", assetMboImp.getString("contract_type"), 11L);
				assetmbo.setValue("ZZ_SECTOR", assetMboImp.getString("sector"), 11L);
				assetmbo.setValue("ZZ_REMOVEMARK", assetMboImp.getString("removemark"), 11L);
				assetmbo.setValue("ZZ_METERTYPE", assetMboImp.getString("meter_type"), 11L);
				assetmbo.setValue("DESCRIPTION", assetMboImp.getString("meter_num"), 11L);
				if (!assetMboImp.getString("elect_num").equals("")) {
					if (Replace_Flag == true) {
						assetmbo.setValue("SITEID", R_Site_ID, 11L); // davis
						myLogger.debug("電號"
								+ assetMboImp.getString("comp_meter_num")
								+ " Site:" + R_Site_ID);
					} else {
						assetmbo.setValue("SITEID", assetMboImp
								.getString("siteid"), 11L); // davis
						myLogger.debug("New Site_ID Use"
								+ assetMboImp.getString("siteid"));
					}
				}
				assetmbo.setValue("STATUS", "active", 11L);
				newAssetSet.save();
			} else {
				importStatus = 4;
			}

		} catch (MXException e) {

			throw e;
		} catch (Exception e) {

			throw e;
		}
	}

	// 2019/03/12 Davis Edit for HES Data Previw
	public void HESImportListByExcel(
			Hashtable<String, Hashtable<String, String>> readedData)
			throws RemoteException, MXException {
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		conKey = ui.getConnectionKey();
		for (String i : readedData.keySet()) {
			Hashtable<String, String> Row = readedData.get(i);
			MboRemote newImpLine = add();
			newImpLine.setValue("ACTIONTYP", StringUtil.emptyStr((String) Row
					.get("異動別")), 11L);
			newImpLine.setValue("ELECT_NUM", StringUtil.emptyStr((String) Row
					.get("電號")), 11L);
			newImpLine.setValue("COMP_METER_NUM", StringUtil
					.emptyStr((String) Row.get("電表表號")), 11L);
			newImpLine.setValue("HES_CODE", StringUtil.emptyStr((String) Row
					.get("HES代號")), 11L);

			newImpLine.setValue("UPLSTATUS", StringUtil.isEmpty(Row
					.get("ERROR")) ? "Y" : "N", 11L);
			newImpLine.setValue("UPLRESULT", StringUtil.isEmpty(Row
					.get("ERROR")) ? "OK!" : Row.get("ERROR"), 11L);
			newImpLine.setValue("IMPSTATUS", "", 11L);
		}
	}

	public void AMIImportListByExcel(
			Hashtable<String, Hashtable<String, String>> readedData)
			throws RemoteException, MXException {
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		conKey = ui.getConnectionKey();
		for (String i : readedData.keySet()) {
			Hashtable<String, String> Row = readedData.get(i);
			MboRemote newImpLine = add();
			newImpLine.setValue("ACTIONTYP", StringUtil.emptyStr((String) Row.get("異動別")), 11L);
			newImpLine.setValue("ELECT_NUM", StringUtil.emptyStr((String) Row.get("電號")), 11L);
			newImpLine.setValue("COMP_METER_NUM", StringUtil.emptyStr((String) Row.get("電表表號")), 11L);
			newImpLine.setValue("EXCHANGE_METER_NUM", StringUtil.emptyStr((String) Row.get("更新電表表號")), 11L);
			// newImpLine.setValue("REMOVE_METER_NUM", StringUtil
			// .emptyStr((String) Row.get("移除電表表號")), 11L);
			int tet = StringUtil.emptyStr((String) Row.get("ERROR")).indexOf(
					"日期格式不正確");
			// if(StringUtil.emptyStr((String)
			// Row.get("ERROR")).indexOf("日期格式不正確")>-1){
			//
			// }else{
			newImpLine.setValue("REMOVE_TIME", StringUtil.emptyStr((String) Row
					.get("設備異動時間")), 11L);
			// }

			newImpLine.setValue("UPLSTATUS", StringUtil.isEmpty(Row
					.get("ERROR")) ? "Y" : "N", 11L);
			newImpLine.setValue("UPLRESULT", StringUtil.isEmpty(Row
					.get("ERROR")) ? "OK!" : Row.get("ERROR"), 11L);
			newImpLine.setValue("IMPSTATUS", "", 11L);
		}
	}

	public void ProgramIdImportListByExcel(
			Hashtable<String, Hashtable<String, String>> readedData)
			throws RemoteException, MXException {

		for (String i : readedData.keySet()) {
			Hashtable<String, String> Row = readedData.get(i);
			MboRemote newImpLine = add();
			newImpLine.setValue("ACTIONTYP", StringUtil.emptyStr((String) Row.get("異動別")), 11L);
			newImpLine.setValue("PROGRAMID", StringUtil.emptyStr((String) Row.get("ProgramId")), 11L);
			newImpLine.setValue("TOU", StringUtil.emptyStr((String) Row.get("TOU")), 11L);
			newImpLine.setValue("HVLV", StringUtil.emptyStr((String) Row.get("高低壓")), 11L);
			newImpLine.setValue("ISDEMAND", StringUtil.emptyStr((String) Row.get("需量用戶")), 11L);
			newImpLine.setValue("VIRTUALWORK", StringUtil.emptyStr((String) Row.get("含虛功")), 11L);
			newImpLine.setValue("AIRCON", StringUtil.emptyStr((String) Row.get("含空調分表")), 11L);
			newImpLine.setValue("DIRECTION", StringUtil.emptyStr((String) Row.get("單雙向")), 11L);
			newImpLine.setValue("NOTE", StringUtil.emptyStr((String) Row.get("備註")), 11L);

			newImpLine.setValue("UPLSTATUS", StringUtil.isEmpty(Row.get("ERROR")) ? "Y" : "N", 11L);
			newImpLine.setValue("UPLRESULT", StringUtil.isEmpty(Row.get("ERROR")) ? "OK!" : Row.get("ERROR"), 11L);
			newImpLine.setValue("IMPSTATUS", "", 11L);
		}
	}

	public void MeterTypeImportListByExcel(
			Hashtable<String, Hashtable<String, String>> readedData)
			throws RemoteException, MXException {

		for (String i : readedData.keySet()) {
			Hashtable<String, String> Row = readedData.get(i);
			MboRemote newImpLine = add();
			newImpLine.setValue("ACTIONTYP", StringUtil.emptyStr((String) Row.get("異動別")), 11L);
			newImpLine.setValue("METERTYPE", StringUtil.emptyStr((String) Row.get("電表型式")), 11L);
			newImpLine.setValue("DESCRIPTION", StringUtil.emptyStr((String) Row.get("描述")), 11L);
			newImpLine.setValue("METERMANUFACTURE", StringUtil.emptyStr((String) Row.get("廠商")), 11L);
			newImpLine.setValue("METERDIRECTION", StringUtil.emptyStr((String) Row.get("單雙向")), 11L);
			newImpLine.setValue("METERPHASE", StringUtil.emptyStr((String) Row.get("單三相")), 11L);
			newImpLine.setValue("METERDEGREE", StringUtil.emptyStr((String) Row.get("倍數表")), 11L);
			newImpLine.setValue("METERREMOTE", StringUtil.emptyStr((String) Row.get("遠端啟斷")), 11L);
			newImpLine.setValue("NOTE", StringUtil.emptyStr((String) Row.get("備註")), 11L);
			
			newImpLine.setValue("UPLSTATUS", StringUtil.isEmpty(Row.get("ERROR")) ? "Y" : "N", 11L);
			newImpLine.setValue("UPLRESULT", StringUtil.isEmpty(Row.get("ERROR")) ? "OK!" : Row.get("ERROR"), 11L);
			newImpLine.setValue("IMPSTATUS", "", 11L);
		}
	}

	public void NewImportListByExcel(
			Hashtable<String, Hashtable<String, String>> readedData) throws RemoteException, MXException {
		// 2019/02/27 Davis Edit
		// locationsSet =
		// (LocationSet)MXServer.getMXServer().getMboSet("LOCATIONS",
		// MXServer.getMXServer().getSystemUserInfo());
		mxServer = MXServer.getMXServer();
		ui = mxServer.getSystemUserInfo();
		conKey = ui.getConnectionKey();
		// DBShortcut dbs = new DBShortcut();
		// dbs.connect(conKey);
		for (String i : readedData.keySet()) {
			Hashtable<String, String> Row = readedData.get(i);
			MboRemote newImpLine = add();
			newImpLine.setValue("ACTIONTYP", StringUtil.emptyStr((String) Row
					.get("編輯狀態")), 11L);
			newImpLine.setValue("ELECT_NUM", StringUtil.emptyStr((String) Row
					.get("電號")), 11L);
			newImpLine.setValue("METER_NUM", StringUtil.emptyStr((String) Row
					.get("表號")), 11L);
			newImpLine.setValue("CONTRACT_TYPE", StringUtil
					.emptyStr((String) Row.get("契約種類")), 11L);
			newImpLine.setValue("SECTOR", StringUtil.emptyStr((String) Row
					.get("段別")), 11L);
			newImpLine.setValue("REMOVEMARK", StringUtil.emptyStr((String) Row
					.get("已移除註記")), 11L);
			newImpLine.setValue("METER_TYPE", StringUtil.emptyStr((String) Row
					.get("電表型式")), 11L);
			newImpLine.setValue("COMP_METER_NUM", StringUtil
					.emptyStr((String) Row.get("完整表號")), 11L);
			newImpLine.setValue("OPER_TYPE", StringUtil.emptyStr((String) Row
					.get("種類")), 11L);
			// newImpLine.setValue("SITEID",
			// StringUtil.emptyStr((String)Row.get("區域")), 11L); //davis

			// newImpLine.setValue("SITEID",
			// StringUtil.emptyStr((String)Row.get("電號")).substring(0,2), 11L);

			// check location here-----2019/02/27 Davis
			// if(!StringUtil.isEmpty((String)Row.get("電號"))){
			// newImpLine.setValue("SITEID",
			// StringUtil.emptyStr((String)Row.get("電號")).substring(0,2), 11L);
			// //CheckImpLocExist(Row);
			// locationsSet.setWhere("UPPER(LOCATION) = UPPER('" +
			// StringUtil.emptyStr((String)Row.get("電號")) + "')");
			// //myLogger.debug("Start Check Loc!! "+"UPPER(LOCATION) = UPPER('"
			// + StringUtil.emptyStr((String)Row.get("電號")) + "')");
			// String saSQL = "select * from LOCATIONS WHERE " +
			// "UPPER(LOCATION) = UPPER('" +
			// StringUtil.emptyStr((String)Row.get("電號")) + "')";
			// //if (locationsSet.isEmpty()) {
			// if(!checkIsExist(dbs, saSQL)){
			// try
			// {
			// myLogger.debug("Save Locate Record Start!!");
			// MboRemote newLocation = locationsSet.add();
			// newLocation.setValue("LOCATION",
			// StringUtil.emptyStr((String)Row.get("電號")), 11L);
			// newLocation.setValue("TYPE",
			// StringUtil.emptyStr((String)Row.get("種類")), 11L);
			// newLocation.setValue("SITEID",
			// StringUtil.emptyStr((String)Row.get("電號")).substring(0,2), 11L);
			// //newLocation.setValue("TYPE", "OPERATING", 11L);
			// //newLocation.setValue("SITEID", "TPCSITE", 11L);
			// locationsSet.save();
			// myLogger.debug("Save Locate Record Finish");
			// }
			// catch (Exception e)
			// {
			// e.printStackTrace();
			// }
			// }
			//    	  
			// }

			if (!StringUtil.isEmpty((String) Row.get("電號"))) {
				newImpLine.setValue("SITEID", StringUtil.emptyStr(
						(String) Row.get("電號")).substring(0, 2), 11L);
				// myLogger.debug("Save Asset SITEID "+
				// StringUtil.emptyStr((String)Row.get("電號")).substring(0,2));
			}
			// TMP TST
			// newImpLine.setValue("SITEID", "TPCSITE", 11L);
			// newImpLine.setValue("OPER_TYPE", "OPERATING", 11L);

			newImpLine.setValue("IMPSTATUS", "", 11L);
			newImpLine.setValue("UPLSTATUS", StringUtil.isEmpty(Row
					.get("ERROR")) ? "Y" : "N", 11L);
			newImpLine.setValue("UPLRESULT", StringUtil.isEmpty(Row
					.get("ERROR")) ? "OK!" : Row.get("ERROR"), 11L);
		}
	}

}
