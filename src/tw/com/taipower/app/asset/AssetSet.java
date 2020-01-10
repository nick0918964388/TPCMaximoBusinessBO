/**
* System      : Asset Management System                                        
* Class name  : AssetSet.java                                                     
* Description : extends psdi.app.asset.AssetSet            
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.asset;
import java.rmi.RemoteException;
import psdi.app.asset.AssetSetRemote;
import psdi.mbo.Mbo;
import psdi.mbo.MboServerInterface;
import psdi.mbo.MboSet;
import psdi.util.MXException;
public class AssetSet extends psdi.app.asset.AssetSet implements AssetSetRemote
{
  public AssetSet(MboServerInterface ms)
    throws MXException, RemoteException
  {
    super(ms);
  }
  
  protected Mbo getMboInstance(MboSet ms)
    throws MXException, RemoteException
  {
    return new Asset(ms);
  }

}
