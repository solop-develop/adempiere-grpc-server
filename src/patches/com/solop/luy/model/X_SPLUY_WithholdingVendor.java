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
package com.solop.luy.model;

import org.adempiere.core.domains.models.I_C_BPartner;
import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.spin.model.I_WH_Definition;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for SPLUY_WithholdingVendor
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_SPLUY_WithholdingVendor extends PO implements I_SPLUY_WithholdingVendor, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251126L;

    /** Standard Constructor */
    public X_SPLUY_WithholdingVendor (Properties ctx, int SPLUY_WithholdingVendor_ID, String trxName)
    {
      super (ctx, SPLUY_WithholdingVendor_ID, trxName);
      /** if (SPLUY_WithholdingVendor_ID == 0)
        {
			setC_BPartner_ID (0);
			setSPLUY_WithholdingVendor_ID (0);
			setWH_Definition_ID (0);
        } */
    }

    /** Load Constructor */
    public X_SPLUY_WithholdingVendor (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SPLUY_WithholdingVendor[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_BPartner getC_BPartner() throws RuntimeException
    {
		return (I_C_BPartner)MTable.get(getCtx(), I_C_BPartner.Table_Name)
			.getPO(getC_BPartner_ID(), get_TrxName());	}

	/** Set Business Partner .
		@param C_BPartner_ID 
		Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1) 
			set_Value (COLUMNNAME_C_BPartner_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner .
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Withholding Definition Vendor.
		@param SPLUY_WithholdingVendor_ID Withholding Definition Vendor	  */
	public void setSPLUY_WithholdingVendor_ID (int SPLUY_WithholdingVendor_ID)
	{
		if (SPLUY_WithholdingVendor_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SPLUY_WithholdingVendor_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SPLUY_WithholdingVendor_ID, Integer.valueOf(SPLUY_WithholdingVendor_ID));
	}

	/** Get Withholding Definition Vendor.
		@return Withholding Definition Vendor	  */
	public int getSPLUY_WithholdingVendor_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SPLUY_WithholdingVendor_ID);
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

	public I_WH_Definition getWH_Definition() throws RuntimeException
    {
		return (I_WH_Definition)MTable.get(getCtx(), I_WH_Definition.Table_Name)
			.getPO(getWH_Definition_ID(), get_TrxName());	}

	/** Set Withholding .
		@param WH_Definition_ID 
		Withholding Definition is used for define a withholding rule for BP
	  */
	public void setWH_Definition_ID (int WH_Definition_ID)
	{
		if (WH_Definition_ID < 1) 
			set_Value (COLUMNNAME_WH_Definition_ID, null);
		else 
			set_Value (COLUMNNAME_WH_Definition_ID, Integer.valueOf(WH_Definition_ID));
	}

	/** Get Withholding .
		@return Withholding Definition is used for define a withholding rule for BP
	  */
	public int getWH_Definition_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WH_Definition_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}