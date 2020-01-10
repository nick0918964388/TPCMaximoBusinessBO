/**
* System      : Asset Management System                                        
* Class name  : CustClsificationTmpObj.java                                                     
* Description : Method for clsificationtmpobj        
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.virtual;

public class CustClsificationTmpObj
{
  private String classstructureid = "";
  private String classificationid = "";
  private String clsIficationDesc = "";
  private String parent = "";
  
  public String getClassstructureid()
  {
    return this.classstructureid;
  }
  
  public String getClassificationid()
  {
    return this.classificationid;
  }
  
  public String getClsIficationDesc()
  {
    return this.clsIficationDesc;
  }
  
  public String getParent()
  {
    return this.parent;
  }
  
  public void setClassstructureid(String str)
  {
    this.classstructureid = str;
  }
  
  public void setClassificationid(String str)
  {
    this.classificationid = str;
  }
  
  public void setClsIficationDesc(String str)
  {
    this.clsIficationDesc = str;
  }
  
  public void setParent(String str)
  {
    this.parent = str;
  }
}
