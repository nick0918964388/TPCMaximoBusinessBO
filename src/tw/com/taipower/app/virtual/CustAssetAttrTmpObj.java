/**
* System      : Asset Management System                                        
* Class name  : CustAssetAttrTmpObj.java                                                     
* Description : Method for get assetattrtmp          
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.virtual;

public class CustAssetAttrTmpObj extends Object{
	//for locspec use
	private String assetattrid = "";
	private String alnvalue = "";
	private String measureunitid = "";
	
	//For Assetattribute use
	private String datatype = "";
	private String description = "";
	private String domainid = "";
	private Integer assetattributeid;
	
	
	public CustAssetAttrTmpObj() {
		super();
	}
	
	public String getAssetattrid() {
		return assetattrid;
	}

	public void setAssetattrid(String str) {
		this.assetattrid = str;
	}
	
	public String getAlnvalue() {
		return alnvalue;
	}
	
	public void setAlnvalue(String str) {
		this.alnvalue = str;
	}
	
	public String getMeasureunitid() {
		return measureunitid;
	}
	
	public void setMeasureunitid(String str) {
		this.measureunitid = str;
	}
	
	public String getDatatype() {
		return datatype;
	}
	
	public void setDatatype(String str) {
		this.datatype = str;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String str) {
		this.description = str;
	}
	
	public String getDomainid() {
		return domainid;
	}
	
	public void setDomainid(String str) {
		this.domainid = str;
	}
	
	public Integer getAssetattributeid() {
		return assetattributeid;
	}
	
	public void setAssetattributeid(Integer assattrid) {
		this.assetattributeid = assattrid;
	}


}
