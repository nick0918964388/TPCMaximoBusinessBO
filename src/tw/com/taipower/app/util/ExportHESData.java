/**
* System      : Asset Management System                                        
* Class name  : ExportHESData.java                                                     
* Description : Export Hes Record            
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
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.poi.hssf.usermodel.DVConstraint;
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

public class ExportHESData {
	MXServer			server				= null;
	List<String>		excelColumnList		= null;
	List<String>		excelNameColumnList	= null;
	String				userName			= "";
	public HSSFWorkbook	wb					= null;
	MXLogger			myLogger			= MXLoggerFactory
													.getLogger("maximo.sql.LOCATION.LOCATIONS");

	public ExportHESData() {

	}

	public ExportHESData(String fileName) throws RemoteException,
			MXException, IOException {
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
		myLogger.debug("test STEP3");
		if (server.getProperty("mxe.hostname").equalsIgnoreCase("vm-win2012r2"))
			readTemplatePath="C:\\doclinks\\ExportTemplate\\HES_output.xls";
		else
			readTemplatePath="/opt/inst/HES_output.xls";
	
		File fTemp = new File(readTemplatePath);
		
		if (!fTemp.exists()) {
			System.out.println("no this file ==>" + fTemp.getAbsolutePath());
			return null;
		}
		System.out.println("Export HES getTempfile==>" + fTemp.getAbsolutePath());
		return fTemp;
	}

	public void getExcelFile(InputStream is) throws RemoteException,
			MXException, IOException {

		wb = new HSSFWorkbook(is);
	}

	public void writeToExcel(String qbeStr) throws RemoteException, MXException {
		SetDbViewColumn();
		
		HSSFSheet st = wb.getSheetAt(0);
		HSSFSheet st1 = wb.getSheetAt(1);
		StringBuilder setWhereClause = new StringBuilder();
		int x = 0;
		int y = 0;
		int rowX = 1;
		Hashtable<String, Hashtable<String, String>> hesInfoData = new Hashtable<String, Hashtable<String, String>>();
		String hesGetFromDescription = null;
		MboSetRemote assetSet = null;
		MboSetRemote getHesDomain = null;
		// assetSet = server.getMboSet("ZZ_HES_IMP_DATA", server.getSystemUserInfo());

		server = MXServer.getMXServer();
		assetSet = server.getMboSet("ASSET", server.getSystemUserInfo());
		getHesDomain = server.getMboSet("ASSET", server.getSystemUserInfo());
		qbeStr = qbeStr.toUpperCase();
		String hesClassifyId = null;
		
		// qbeStr=qbeStr.replace("ASSETNUM", "COMP_METER_NUM");
		// ----------V1.1 Add Start------------------------------------------
		MboSetRemote Get_ZZ_hesDomain = null;
		MboSetRemote mboSetClassstructure = null;
		mboSetClassstructure = server.getMboSet("CLASSSTRUCTURE", server.getSystemUserInfo());

		mboSetClassstructure.setWhere("UPPER(CLASSIFICATIONID)='HES'");
		mboSetClassstructure.reset();
		if (!mboSetClassstructure.isEmpty())
			hesClassifyId = mboSetClassstructure.getMbo(0).getString("CLASSSTRUCTUREID");

		if (setWhereClause.length() > 0)
			setWhereClause.delete(0, setWhereClause.length());
		setWhereClause.append("CLASSSTRUCTUREID='");
		setWhereClause.append(hesClassifyId);
		setWhereClause.append("'");
		getHesDomain.setWhere(setWhereClause.toString());
		getHesDomain.reset();
		if (!getHesDomain.isEmpty()) {
			for (MboRemote currMbo = getHesDomain.moveFirst(); currMbo != null; currMbo = getHesDomain.moveNext()) {
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
		}

		String[] set_hes_code = new String[hesInfoData.size()];
		int indexSave = 0;
		for (String i : hesInfoData.keySet()) {
			set_hes_code[indexSave] = hesInfoData.get(i).get("hesCode");
			indexSave++;
		}

		// for(int i=0;i<hesInfoData.size();i++)
		// set_hes_code[i]=Arr_HesCode.get(i);
		//		

		// ----------V1.1 Add End------------------------------------------
	
		assetSet.setWhere(qbeStr);
		assetSet.reset();
		assetSet.setLogLargFetchResultDisabled(true);
		// String[] attrs = { "ELECT_NUM", "COMP_METER_NUM", "EXCHANGE_METER_NUM",
		// "REMOVE_METER_NUM", "ACTIONTYP", "REMOVE_TIME"};// "EDIT_USER","IMPTIME"};
		assetSet.setLogLargFetchResultDisabled(true);
		String[] getClassSpecVal = { "NEW", "UPDATE" };

		for (String i : hesInfoData.keySet()) {
			Row row1 = st1.createRow(rowX);
			Cell cell = row1.createCell(0);
			cell.setCellValue(hesInfoData.get(i).get("hesCode"));
			// cell = row1.createCell(1);
			// cell.setCellValue(hesInfoData.get(i).get("hesIp"));
			// cell = row1.createCell(2);
			// cell.setCellValue(hesInfoData.get(i).get("hesUrl"));
			rowX++;
		}
		rowX = 1;
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
			// for (y = 0; y < ExcelColumn.size(); y++) {
			// Cell cell = row.createCell(y);
			// myLogger.debug("asset cellvalue ==>"
			// + asset.getString(ExcelColumn.get(y)));
			// cell.setCellValue(!asset.getString(ExcelColumn.get(y))
			// .equalsIgnoreCase("") ? asset.getString(ExcelColumn
			// .get(y)) : "");
			// }
			for (y = 0; y < excelColumnList.size(); y++) {
				Cell cell = row.createCell(y);
				if (excelColumnList.get(y) == "ACTIONTYP") {
					CellRangeAddressList addressList = new CellRangeAddressList(row.getRowNum(), row.getRowNum(), y, y);
					DVConstraint dvConstraint = DVConstraint.createExplicitListConstraint(getClassSpecVal);
					DataValidation dataValidation = new HSSFDataValidation(addressList, dvConstraint);
					dataValidation.setSuppressDropDownArrow(false);
					st.addValidationData(dataValidation);
					if (asset.isNull("ZZ_HES_CODE"))
						cell.setCellValue("NEW");
					else
						cell.setCellValue("UPDATE");
				}
				// -------v1.1 add start------------------------
				else if (excelColumnList.get(y) == "ZZ_HES_CODE") {
					CellRangeAddressList addressList = new CellRangeAddressList(row.getRowNum(), row.getRowNum(), y, y);
					DVConstraint dvConstraint = DVConstraint.createExplicitListConstraint(set_hes_code);
					DataValidation dataValidation = new HSSFDataValidation(addressList, dvConstraint);
					dataValidation.setSuppressDropDownArrow(false);
					st.addValidationData(dataValidation);
					cell.setCellValue(!asset.getString(excelColumnList.get(y)).equalsIgnoreCase("") ? asset.getString(excelColumnList.get(y)) : "");

				} else {
					myLogger.debug("asset cellvalue ==>" + asset.getString(excelColumnList.get(y)));

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
		excelColumnList.add("ZZ_CUSTNO"); // elect num
		excelColumnList.add("ASSETNUM"); // meter_num
		excelColumnList.add("ZZ_HES_CODE"); // hes_code
		excelColumnList.add("ACTIONTYP"); // edit type
	}
}