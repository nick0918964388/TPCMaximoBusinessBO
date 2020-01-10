/**
* System      : Asset Management System                                        
* Class name  : ExpAssetData.java                                                     
* Description : Export Asset Record            
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
import psdi.mbo.MboValueData;
import psdi.security.ConnectionKey;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

public class ExportAssetData {
	MXServer server = null;
	
	List<String> ExcelColumn = null;
	List<String> ExcelNameColumn = null;
	String UserName = "";
	public HSSFWorkbook wb = null;
	MXLogger myLogger = MXLoggerFactory
			.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public ExportAssetData() {

	}
	
	public ExportAssetData(String fileName) throws RemoteException,
			MXException, IOException {
		server = MXServer.getMXServer();
		//assetSet = server.getMboSet("ASSET", server.getSystemUserInfo());
		//assetSet.setWhere("assetnum like 'GD%'");
		//assetSet.setWhere(qbeStr);
		//assetSet.reset();
		//myLogger.debug("asset ==>" + assetSet.count());
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
		String tempFileName = "Asset_output.xls";
		File fTemp = new File(tempPath + tempFileName);
		if (!fTemp.exists()) {
			System.out.println("no this file ==>" + fTemp.getAbsolutePath());
			return null;
		}
		System.out.println("Export Asset getTempfile1==>"
				+ fTemp.getAbsolutePath());
		return fTemp;
	}

	public void getExcelFile(InputStream is) throws RemoteException,
			MXException, IOException {

		wb = new HSSFWorkbook(is);
	}

	
	
	public void writeToExcel(String qbeStr) throws RemoteException, MXException {
		SetDbViewColumn();
		HSSFSheet st = wb.getSheetAt(0);
		int x = 0;
		int y = 0;
		int rowX = 2;
		MboSetRemote assetSet = null;
		assetSet = server.getMboSet("ASSET", server.getSystemUserInfo());
		//assetSet.setWhere("assetnum like 'GD%'");
		assetSet.setWhere(qbeStr);
		assetSet.reset();
		assetSet.setLogLargFetchResultDisabled(true);
		String[] attrs = { "LOCATION", "ASSETNUM", "ZZ_CONTRACT_KIND", 
	    	      "ZZ_SECTOR", "ZZ_REMOVEMARK", "ZZ_METERTYPE", "ZZ_UUID"};
		while (assetSet.getMbo(x) != null) {
			MboRemote asset = assetSet.getMbo(x);  
			MboValueData[] valData = asset.getMboValueData(attrs);
			
			Row row = st.createRow(rowX);
			//myLogger.debug("asset ==>" + asset.getString("ASSETNUM"));
			
			Cell cell = row.createCell(0);
			cell.setCellValue(!valData[0].getData().equalsIgnoreCase("")?valData[0].getData():"");
			cell = row.createCell(1);
			cell.setCellValue(!valData[1].getData().equalsIgnoreCase("")?
					valData[1].getData().substring(2,valData[1].getData().length()):"");
			cell = row.createCell(2);
			cell.setCellValue(!valData[2].getData().equalsIgnoreCase("")?valData[2].getData():"");
			cell = row.createCell(3);
			cell.setCellValue(!valData[3].getData().equalsIgnoreCase("")?valData[3].getData():"");
			cell = row.createCell(4);
			cell.setCellValue(!valData[4].getData().equalsIgnoreCase("")?valData[4].getData():"");
			cell = row.createCell(5);
			cell.setCellValue(!valData[5].getData().equalsIgnoreCase("")?valData[5].getData():"");
			cell = row.createCell(6);
			cell.setCellValue(!valData[1].getData().equalsIgnoreCase("")?valData[1].getData():"");
			cell = row.createCell(7);
			cell.setCellValue(!valData[6].getData().equalsIgnoreCase("")?valData[6].getData():"");
//			myLogger.debug("asset ==>" + ExcelColumn.size());
//			for (y = 0; y < ExcelColumn.size(); y++) {
//				Cell cell = row.createCell(y);
//				myLogger.debug("asset cellvalue ==>"
//						+ asset.getString(ExcelColumn.get(y)));
//				cell.setCellValue(!asset.getString(ExcelColumn.get(y))
//						.equalsIgnoreCase("") ? asset.getString(ExcelColumn
//						.get(y)) : "");
//			}
			x++;
			rowX++;
		}

		if (myLogger.isDebugEnabled()) {
			myLogger.debug("ExportItem writeToExcel ==>OK!");
		}
		assetSet.setLogLargFetchResultDisabled(false);
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
		ExcelColumn.add("LOCATION");         //電號      
		ExcelColumn.add("ASSETNUM");         //表號
		ExcelColumn.add("ZZ_CONTRACT_KIND"); //契約種類 
		ExcelColumn.add("ZZ_SECTOR");        //段號
		ExcelColumn.add("ZZ_REMOVEMARK");    //已移除註記
		ExcelColumn.add("ZZ_METERTYPE");    //電表型式
		ExcelColumn.add("ASSETNUM");         //完整表號
		//ExcelColumn.add("OPER_TYPE");
		//ExcelColumn.add("SITEID");
		ExcelColumn.add("ZZ_UUID");             //UUID 
	}
}