/**
* System      : Asset Management System                                        
* Class name  : ExpAMIData.java                                                     
* Description : Export AMIData Record            
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFDataValidation;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddressList;

import psdi.mbo.DBShortcut;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValueData;
import psdi.security.ConnectionKey;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

public class ExportAMIData {
	MXServer			server			= null;
	List<String>		excelColumnList		= null;
	List<String>		excelNameColumnList	= null;
	String				userName		= "";
	public HSSFWorkbook	wb				= null;
	MXLogger			myLogger		= MXLoggerFactory
												.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public ExportAMIData() {

	}

	public ExportAMIData(String fileName) throws RemoteException, MXException,
			IOException {
		server = MXServer.getMXServer();
		FileInputStream fis = new FileInputStream(fileName);
		getExcelFile(fis); // 讀入Eexcel範本

	}

	public String getOutputFileName() throws RemoteException, MXException {
		String Filename = ".xls";
		userName = MXServer.getMXServer().getName();
		String dlDate = DateUtil.formatDate(new Date(), "yyyyMMddHHmmss");
		Filename = userName + "-" + dlDate + Filename;
		return Filename;
	}

	/*
	 * 取得匯出之Excel範本
	 */
	public File getTempFile() throws RemoteException, MXException, IOException {
		String readTemplatePath="";
		server = MXServer.getMXServer();
		if (server.getProperty("mxe.hostname").equalsIgnoreCase("vm-win2012r2"))
			readTemplatePath="C:\\doclinks\\ExportTemplate\\AMI_output.xls";
		else
			readTemplatePath="/opt/inst/AMI_output.xls";
	
		File fTemp = new File(readTemplatePath);
		
		if (!fTemp.exists()) {
			System.out.println("no this file ==>" + fTemp.getAbsolutePath());
			return null;
		}
		System.out.println("Export AMI getTempfile==>"
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
		server = MXServer.getMXServer();
		String[] getClassSpecVal = { "NEW", "UPDATE", "REMOVE" };
		MboSetRemote assetSet = null;
		assetSet = server.getMboSet("ASSET", server.getSystemUserInfo());
		// qbeStr=qbeStr.toUpperCase();
		// qbeStr=qbeStr.replaceAll("ASSETNUM", "COMP_METER_NUM");

		//System.out.println("Export AMI sql==>" + qbeStr);

		assetSet.setWhere(qbeStr);
		assetSet.reset();
		assetSet.setLogLargFetchResultDisabled(true);
		// String[] attrs = { "ELECT_NUM", "COMP_METER_NUM",
		// "EXCHANGE_METER_NUM",
		// "REMOVE_METER_)NUM", "ACTIONTYP", "REMOVE_TIME"};//
		// "EDIT_USER","IMPTIME"};
		assetSet.setLogLargFetchResultDisabled(true);

		while (assetSet.getMbo(x) != null) {
			MboRemote asset = assetSet.getMbo(x);
			// MboValueData[] valData = asset.getMboValueData(attrs);

			Row row = st.createRow(rowX);
			// Cell cell = row.createCell(0);
			// cell.setCellValue(!valData[0].getData().equalsIgnoreCase("")?valData[0].getData():"");
			// cell = row.createCell(1);
			// cell.setCellValue(!valData[1].getData().equalsIgnoreCase("")?valData[1].getData().substring(2,valData[1].getData().length()):"");
			// cell = row.createCell(2);
			// cell.setCellValue(!valData[2].getData().equalsIgnoreCase("")?valData[2].getData():"");
			// cell = row.createCell(3);
			// cell.setCellValue(!valData[3].getData().equalsIgnoreCase("")?valData[3].getData():"");
			// cell = row.createCell(4);
			// cell.setCellValue(!valData[4].getData().equalsIgnoreCase("")?valData[4].getData():"");
			// cell = row.createCell(5);
			// cell.setCellValue(!valData[5].getData().equalsIgnoreCase("")?valData[5].getData():"");
			// cell = row.createCell(6);
			// cell.setCellValue(!valData[1].getData().equalsIgnoreCase("")?valData[1].getData():"");
			// cell = row.createCell(7);
			// cell.setCellValue(!valData[6].getData().equalsIgnoreCase("")?valData[6].getData():"");
			// myLogger.debug("asset ==>" + ExcelColumn.size());
			for (y = 0; y < excelColumnList.size(); y++) {
				Cell cell = row.createCell(y);
				if (excelColumnList.get(y) == "ACTIONTYP") {
					CellRangeAddressList addressList = new CellRangeAddressList(row.getRowNum(), row.getRowNum(), y, y);
					DVConstraint dvConstraint = DVConstraint.createExplicitListConstraint(getClassSpecVal);
					DataValidation dataValidation = new HSSFDataValidation(addressList, dvConstraint);
					dataValidation.setSuppressDropDownArrow(false);
					st.addValidationData(dataValidation);
					cell.setCellValue("NEW");
				} else if (excelColumnList.get(y) == "REMOVE_TIME") {
					Date date = new Date();
					String strTimeFormat = "yyyy/MM/dd HH:mm:ss";
					DateFormat timeFormat = new SimpleDateFormat(strTimeFormat);
					String formattedTime = timeFormat.format(date);
					cell.setCellValue(formattedTime);
					HSSFCellStyle cellStyle = wb.createCellStyle();
					HSSFDataFormat format = wb.createDataFormat();
					// cellStyle.setDataFormat(format.getFormat("@"));
					cellStyle.setDataFormat(format.getFormat("yyyy/MM/dd HH:mm:ss"));
					cell.setCellStyle(cellStyle);
				} else if (y < 2) {
					// myLogger.debug("asset cellvalue ==>"
					// + asset.getString(ExcelColumn.get(y)));

					cell.setCellValue(!asset.getString(excelColumnList.get(y)).equalsIgnoreCase("") ? asset.getString(excelColumnList.get(y)) : "");
				}

			}
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
		excelColumnList = new ArrayList<String>();
		excelColumnList.add("ZZ_CUSTNO"); // 電號
		excelColumnList.add("ASSETNUM"); // 表號

		// ExcelColumn.add("ELECT_NUM"); //電號
		// ExcelColumn.add("COMP_METER_NUM"); //表號
		excelColumnList.add("EXCHANGE_METER_NUM"); // 更新電表
		// ExcelColumn.add("REMOVE_METER_NUM"); // 移除電表
		excelColumnList.add("ACTIONTYP"); // 異動別
		excelColumnList.add("REMOVE_TIME"); // 設備異動時間
	}
}