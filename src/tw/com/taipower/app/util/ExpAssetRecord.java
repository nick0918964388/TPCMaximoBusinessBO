/**
* System      : Asset Management System                                        
* Class name  : ExpAssetRecord.java                                                     
* Description : For asset import history, will export data            
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import psdi.mbo.DBShortcut;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.security.ConnectionKey;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

public class ExpAssetRecord {
	MXServer server = null;
	MboSetRemote assetSet = null;
	List<String> ExcelColumn = null;
	List<String> ExcelNameColumn = null;
	String UserName = "";
	public HSSFWorkbook wb = null;
	MXLogger myLogger = MXLoggerFactory
			.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public ExpAssetRecord() {

	}

	public ExpAssetRecord(String fileName) throws RemoteException,
			MXException, IOException {
		server = MXServer.getMXServer();
		assetSet = server.getMboSet("ASSET", server.getSystemUserInfo());
		assetSet.setWhere("assetnum like 'BENZ%'");
		assetSet.reset();
		myLogger.debug("asset ==>" + assetSet.count());
		FileInputStream fis = new FileInputStream(fileName);
		getExcelFile(fis); // 讀入Eexcel範本

	}

	public String getOutputFileName() throws RemoteException, MXException {
		String Filename = ".xls";
		UserName = MXServer.getMXServer().getName();

		// String userName =
		// server.getUserInfo().getSystemUserInfo().getUserName();
		String dlDate = DateUtil.formatDate(new Date(), "yyyyMMddHHmmss");
		Filename = UserName + "-" + dlDate + Filename;
		return Filename;
	}

	/*
	 * 取得匯出之Excel範本
	 */
	public File getTempFile() throws RemoteException, MXException, IOException {
		//String tempPath = "C:\\doclinks\\ExportTemplate\\";
		String tempPath = "/opt/inst/";
		String tempFileName = "Tpc_input.xls";
		File fTemp = new File(tempPath + tempFileName);
		if (!fTemp.exists()) {
			System.out.println("no this file ==>" + fTemp.getAbsolutePath());
			return null;
		}
		System.out.println("Export Asset getTempfile==>"
				+ fTemp.getAbsolutePath());
		return fTemp;
	}

	public void getExcelFile(InputStream is) throws RemoteException,
			MXException, IOException {

		wb = new HSSFWorkbook(is);
	}

	public void writeToExcel(MboSetRemote exp_tmp_data,String savepath,String User,String RecordTime) throws MXException, IOException {
		SetDbViewColumn();
		HSSFSheet st = wb.getSheetAt(0);
		int x = 0;
		int y = 0;
		int rowX = 2;
		//assetSet.setLogLargFetchResultDisabled(true);
		while (exp_tmp_data.getMbo(x) != null) {
			MboRemote asset = exp_tmp_data.getMbo(x);
			Row row = st.createRow(rowX);
			//myLogger.debug("asset ==>" + asset.getString("ASSETNUM"));
			//myLogger.debug("asset ==>" + ExcelColumn.size());
			for (y = 0; y < ExcelColumn.size(); y++) {
				Cell cell = row.createCell(y);
				if(ExcelColumn.get(y).equals("USER")){
					cell.setCellValue(User);
				}else if (ExcelColumn.get(y).equals("IMPTIME")){
					cell.setCellValue(RecordTime);
				}else{
					//myLogger.debug("asset cellvalue ==>"+ asset.getString(ExcelColumn.get(y)));
					cell.setCellValue(!asset.getString(ExcelColumn.get(y)).equalsIgnoreCase("") ? asset.getString(ExcelColumn.get(y)) : "");	
				}
				
				
			}
			//
			
			x++;
			rowX++;
		}
		saveExcelFile(savepath);
		if (myLogger.isDebugEnabled()) {
			myLogger.debug("ExportItem writeToExcel ==>OK!");
		}
		
		//assetSet.setLogLargFetchResultDisabled(false);
	}
    
	/* Excel存檔 */
	public void saveExcelFile(OutputStream fos) throws IOException {

		wb.write(fos);
		fos.close();
	}

	public void saveExcelFile(String fileName) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		saveExcelFile(fos);
	}

	private void SetDbViewColumn() throws RemoteException, MXException {
		ExcelColumn = new ArrayList<String>();
		ExcelColumn.add("ACTIONTYP");
		ExcelColumn.add("ELECT_NUM");
		ExcelColumn.add("METER_NUM");
		ExcelColumn.add("CONTRACT_TYPE");
		ExcelColumn.add("SECTOR");
		ExcelColumn.add("REMOVEMARK");
		ExcelColumn.add("METER_TYPE");
		ExcelColumn.add("COMP_METER_NUM");
		ExcelColumn.add("OPER_TYPE");
		ExcelColumn.add("SITEID");
		ExcelColumn.add("IMPSTATUS");
		ExcelColumn.add("UPLRESULT");
		ExcelColumn.add("USER");
		ExcelColumn.add("IMPTIME");
	}
}