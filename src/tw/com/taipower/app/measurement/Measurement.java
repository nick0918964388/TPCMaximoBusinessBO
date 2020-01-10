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
package tw.com.taipower.app.measurement;

import java.rmi.RemoteException;

import psdi.app.measurement.MeasurementRemote;
import psdi.app.measurement.MeasurementSetRemote;

import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote; //import psdi.mbo.MboSetRemote;
import psdi.security.ConnectionKey;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;


public class Measurement extends psdi.app.measurement.Measurement implements MeasurementRemote {
	MXLogger	myLogger	= MXLoggerFactory.getLogger("maximo.sql.LOCATION.LOCATIONS");  //use for save log

	MXServer	mxServer;
	UserInfo	ui;      

	public Measurement(MboSet ms) throws MXException, RemoteException {

		super(ms);
	}
	
	
	

}
