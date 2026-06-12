/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.adempiere.core.domains.models;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for AD_StorageUpdateQueue
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_StorageUpdateQueue extends PO implements I_AD_StorageUpdateQueue, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260612L;

    /** Standard Constructor */
    public X_AD_StorageUpdateQueue (Properties ctx, int AD_StorageUpdateQueue_ID, String trxName)
    {
      super (ctx, AD_StorageUpdateQueue_ID, trxName);
      /** if (AD_StorageUpdateQueue_ID == 0)
        {
			setAD_StorageUpdateQueue_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_StorageUpdateQueue (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_AD_StorageUpdateQueue[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_QueueType getAD_QueueType() throws RuntimeException
    {
		return (I_AD_QueueType)MTable.get(getCtx(), I_AD_QueueType.Table_Name)
			.getPO(getAD_QueueType_ID(), get_TrxName());	}

	/** Set Queue Type.
		@param AD_QueueType_ID Queue Type	  */
	public void setAD_QueueType_ID (int AD_QueueType_ID)
	{
		if (AD_QueueType_ID < 1) 
			set_Value (COLUMNNAME_AD_QueueType_ID, null);
		else 
			set_Value (COLUMNNAME_AD_QueueType_ID, Integer.valueOf(AD_QueueType_ID));
	}

	/** Get Queue Type.
		@return Queue Type	  */
	public int getAD_QueueType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_QueueType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Storage Update Queue.
		@param AD_StorageUpdateQueue_ID Storage Update Queue	  */
	public void setAD_StorageUpdateQueue_ID (int AD_StorageUpdateQueue_ID)
	{
		if (AD_StorageUpdateQueue_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_StorageUpdateQueue_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_StorageUpdateQueue_ID, Integer.valueOf(AD_StorageUpdateQueue_ID));
	}

	/** Get Storage Update Queue.
		@return Storage Update Queue	  */
	public int getAD_StorageUpdateQueue_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_StorageUpdateQueue_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Immutable Universally Unique Identifier.
		@param UUID 
		Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID)
	{
		set_Value (COLUMNNAME_UUID, UUID);
	}

	/** Get Immutable Universally Unique Identifier.
		@return Immutable Universally Unique Identifier
	  */
	public String getUUID () 
	{
		return (String)get_Value(COLUMNNAME_UUID);
	}
}