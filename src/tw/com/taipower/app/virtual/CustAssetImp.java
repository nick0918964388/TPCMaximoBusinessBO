/**
* System      : Asset Management System                                        
* Class name  : CustAssetImp.jvaa                                                     
* Description : Method for CustAssetImp          
* (c) Copyright Taiwan Power Company 2018-2020                                 
* Modification history : 
*  Date         Person           Comment      Version 
* ----------   -----------    ------------------------
* 2019-06-20   Davis Wang     Initial Release  V1.0                                                     
**/
package tw.com.taipower.app.virtual;

import java.rmi.RemoteException;
import psdi.mbo.MboSet;
import psdi.mbo.NonPersistentMbo;
import psdi.mbo.NonPersistentMboRemote;
import psdi.util.MXException;

public class CustAssetImp
  extends NonPersistentMbo
  implements NonPersistentMboRemote
{
  public CustAssetImp(MboSet ms)
    throws RemoteException
  {
    super(ms);
  }
  
  public void init()
    throws MXException
  {
    super.init();
  }
}
