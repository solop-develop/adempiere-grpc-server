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
package com.solop.sp009.model;

import org.adempiere.core.domains.models.I_M_Product;
import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for SP009_ProductExpedient
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_SP009_ProductExpedient extends PO implements I_SP009_ProductExpedient, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260803L;

    /** Standard Constructor */
    public X_SP009_ProductExpedient (Properties ctx, int SP009_ProductExpedient_ID, String trxName)
    {
      super (ctx, SP009_ProductExpedient_ID, trxName);
      /** if (SP009_ProductExpedient_ID == 0)
        {
			setM_Product_ID (0);
			setSP009_Expedient_ID (0);
			setSP009_ProductExpedient_ID (0);
        } */
    }

    /** Load Constructor */
    public X_SP009_ProductExpedient (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SP009_ProductExpedient[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_M_Product getM_Product() throws RuntimeException
    {
		return (I_M_Product)MTable.get(getCtx(), I_M_Product.Table_Name)
			.getPO(getM_Product_ID(), get_TrxName());	}

	/** Set Product.
		@param M_Product_ID 
		Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_Value (COLUMNNAME_M_Product_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Import Expedient ID.
		@param SP009_Expedient_ID Import Expedient ID	  */
	public void setSP009_Expedient_ID (int SP009_Expedient_ID)
	{
		if (SP009_Expedient_ID < 1) 
			set_Value (COLUMNNAME_SP009_Expedient_ID, null);
		else 
			set_Value (COLUMNNAME_SP009_Expedient_ID, Integer.valueOf(SP009_Expedient_ID));
	}

	/** Get Import Expedient ID.
		@return Import Expedient ID	  */
	public int getSP009_Expedient_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SP009_Expedient_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Product Expedient.
		@param SP009_ProductExpedient_ID Product Expedient	  */
	public void setSP009_ProductExpedient_ID (int SP009_ProductExpedient_ID)
	{
		if (SP009_ProductExpedient_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SP009_ProductExpedient_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SP009_ProductExpedient_ID, Integer.valueOf(SP009_ProductExpedient_ID));
	}

	/** Get Product Expedient.
		@return Product Expedient	  */
	public int getSP009_ProductExpedient_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SP009_ProductExpedient_ID);
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