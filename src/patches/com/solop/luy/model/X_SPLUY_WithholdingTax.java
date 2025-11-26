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

import org.adempiere.core.domains.models.I_C_Tax;
import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.spin.model.I_WH_Definition;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for SPLUY_WithholdingTax
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_SPLUY_WithholdingTax extends PO implements I_SPLUY_WithholdingTax, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251126L;

    /** Standard Constructor */
    public X_SPLUY_WithholdingTax (Properties ctx, int SPLUY_WithholdingTax_ID, String trxName)
    {
      super (ctx, SPLUY_WithholdingTax_ID, trxName);
      /** if (SPLUY_WithholdingTax_ID == 0)
        {
			setC_Tax_ID (0);
			setSPLUY_WithholdingTax_ID (0);
			setWH_Definition_ID (0);
        } */
    }

    /** Load Constructor */
    public X_SPLUY_WithholdingTax (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SPLUY_WithholdingTax[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_Tax getC_Tax() throws RuntimeException
    {
		return (I_C_Tax)MTable.get(getCtx(), I_C_Tax.Table_Name)
			.getPO(getC_Tax_ID(), get_TrxName());	}

	/** Set Tax.
		@param C_Tax_ID 
		Tax identifier
	  */
	public void setC_Tax_ID (int C_Tax_ID)
	{
		if (C_Tax_ID < 1) 
			set_Value (COLUMNNAME_C_Tax_ID, null);
		else 
			set_Value (COLUMNNAME_C_Tax_ID, Integer.valueOf(C_Tax_ID));
	}

	/** Get Tax.
		@return Tax identifier
	  */
	public int getC_Tax_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Tax_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Withholding Tax.
		@param SPLUY_WithholdingTax_ID Withholding Tax	  */
	public void setSPLUY_WithholdingTax_ID (int SPLUY_WithholdingTax_ID)
	{
		if (SPLUY_WithholdingTax_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SPLUY_WithholdingTax_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SPLUY_WithholdingTax_ID, Integer.valueOf(SPLUY_WithholdingTax_ID));
	}

	/** Get Withholding Tax.
		@return Withholding Tax	  */
	public int getSPLUY_WithholdingTax_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SPLUY_WithholdingTax_ID);
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
		return (I_WH_Definition) MTable.get(getCtx(), I_WH_Definition.Table_Name)
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