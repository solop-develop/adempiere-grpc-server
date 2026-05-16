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
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for C_DunningInterestRate
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_DunningInterestRate extends PO implements I_C_DunningInterestRate, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260514L;

    /** Standard Constructor */
    public X_C_DunningInterestRate (Properties ctx, int C_DunningInterestRate_ID, String trxName)
    {
      super (ctx, C_DunningInterestRate_ID, trxName);
      /** if (C_DunningInterestRate_ID == 0)
        {
			setC_DunningInterestRate_ID (0);
			setC_DunningInterestVersion_ID (0);
			setDaysFrom (0);
			setRate (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_C_DunningInterestRate (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_DunningInterestRate[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_Currency getC_Currency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getC_Currency_ID(), get_TrxName());	}

	/** Set Currency.
		@param C_Currency_ID 
		The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1) 
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else 
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Dunning Interes Rate.
		@param C_DunningInterestRate_ID Dunning Interes Rate	  */
	public void setC_DunningInterestRate_ID (int C_DunningInterestRate_ID)
	{
		if (C_DunningInterestRate_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_DunningInterestRate_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_DunningInterestRate_ID, Integer.valueOf(C_DunningInterestRate_ID));
	}

	/** Get Dunning Interes Rate.
		@return Dunning Interes Rate	  */
	public int getC_DunningInterestRate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DunningInterestRate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DunningInterestVersion getC_DunningInterestVersion() throws RuntimeException
    {
		return (I_C_DunningInterestVersion)MTable.get(getCtx(), I_C_DunningInterestVersion.Table_Name)
			.getPO(getC_DunningInterestVersion_ID(), get_TrxName());	}

	/** Set Dunning Interest Version.
		@param C_DunningInterestVersion_ID Dunning Interest Version	  */
	public void setC_DunningInterestVersion_ID (int C_DunningInterestVersion_ID)
	{
		if (C_DunningInterestVersion_ID < 1) 
			set_Value (COLUMNNAME_C_DunningInterestVersion_ID, null);
		else 
			set_Value (COLUMNNAME_C_DunningInterestVersion_ID, Integer.valueOf(C_DunningInterestVersion_ID));
	}

	/** Get Dunning Interest Version.
		@return Dunning Interest Version	  */
	public int getC_DunningInterestVersion_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DunningInterestVersion_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Days From.
		@param DaysFrom Days From	  */
	public void setDaysFrom (int DaysFrom)
	{
		set_Value (COLUMNNAME_DaysFrom, Integer.valueOf(DaysFrom));
	}

	/** Get Days From.
		@return Days From	  */
	public int getDaysFrom () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DaysFrom);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Days To.
		@param DaysTo Days To	  */
	public void setDaysTo (int DaysTo)
	{
		set_Value (COLUMNNAME_DaysTo, Integer.valueOf(DaysTo));
	}

	/** Get Days To.
		@return Days To	  */
	public int getDaysTo () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DaysTo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rate.
		@param Rate 
		Rate or Tax or Exchange
	  */
	public void setRate (BigDecimal Rate)
	{
		set_Value (COLUMNNAME_Rate, Rate);
	}

	/** Get Rate.
		@return Rate or Tax or Exchange
	  */
	public BigDecimal getRate () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Rate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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