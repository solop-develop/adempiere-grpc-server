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
package com.solop.lco.model;

import com.solop.lco.model.I_LCO_WH_Combination;
import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for LCO_WH_Combination
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_LCO_WH_Combination extends PO implements I_LCO_WH_Combination, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260601L;

    /** Standard Constructor */
    public X_LCO_WH_Combination (Properties ctx, int LCO_WH_Combination_ID, String trxName)
    {
      super (ctx, LCO_WH_Combination_ID, trxName);
      /** if (LCO_WH_Combination_ID == 0)
        {
			setLCO_WH_Combination_ID (0);
			setLCO_WH_Concept_ID (0);
			setSeqNo (0);
// 10
			setWithholdingRate (Env.ZERO);
// 0
        } */
    }

    /** Load Constructor */
    public X_LCO_WH_Combination (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LCO_WH_Combination[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.adempiere.core.domains.models.I_C_Currency getC_Currency() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Currency)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Currency.Table_Name)
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

	public org.adempiere.core.domains.models.I_C_Region getC_Region() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Region)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Region.Table_Name)
			.getPO(getC_Region_ID(), get_TrxName());	}

	/** Set Region.
		@param C_Region_ID 
		Identifies a geographical Region
	  */
	public void setC_Region_ID (int C_Region_ID)
	{
		if (C_Region_ID < 1) 
			set_Value (COLUMNNAME_C_Region_ID, null);
		else 
			set_Value (COLUMNNAME_C_Region_ID, Integer.valueOf(C_Region_ID));
	}

	/** Get Region.
		@return Identifies a geographical Region
	  */
	public int getC_Region_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Region_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Withholding Combination.
		@param LCO_WH_Combination_ID Withholding Combination	  */
	public void setLCO_WH_Combination_ID (int LCO_WH_Combination_ID)
	{
		if (LCO_WH_Combination_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LCO_WH_Combination_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LCO_WH_Combination_ID, Integer.valueOf(LCO_WH_Combination_ID));
	}

	/** Get Withholding Combination.
		@return Withholding Combination	  */
	public int getLCO_WH_Combination_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_WH_Combination_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public com.solop.lco.model.I_LCO_WH_Concept getLCO_WH_Concept() throws RuntimeException
    {
		return (com.solop.lco.model.I_LCO_WH_Concept)MTable.get(getCtx(), com.solop.lco.model.I_LCO_WH_Concept.Table_Name)
			.getPO(getLCO_WH_Concept_ID(), get_TrxName());	}

	/** Set Withholding Concept.
		@param LCO_WH_Concept_ID Withholding Concept	  */
	public void setLCO_WH_Concept_ID (int LCO_WH_Concept_ID)
	{
		if (LCO_WH_Concept_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LCO_WH_Concept_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LCO_WH_Concept_ID, Integer.valueOf(LCO_WH_Concept_ID));
	}

	/** Get Withholding Concept.
		@return Withholding Concept	  */
	public int getLCO_WH_Concept_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_WH_Concept_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Max Value.
		@param MaxValue Max Value	  */
	public void setMaxValue (BigDecimal MaxValue)
	{
		set_Value (COLUMNNAME_MaxValue, MaxValue);
	}

	/** Get Max Value.
		@return Max Value	  */
	public BigDecimal getMaxValue () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaxValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Min Value.
		@param MinValue Min Value	  */
	public void setMinValue (BigDecimal MinValue)
	{
		set_Value (COLUMNNAME_MinValue, MinValue);
	}

	/** Get Min Value.
		@return Min Value	  */
	public BigDecimal getMinValue () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MinValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Sequence.
		@param SeqNo 
		Method of ordering records; lowest number comes first
	  */
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
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

	/** Set Withholding Rate.
		@param WithholdingRate 
		Withholding Rate applied to Document
	  */
	public void setWithholdingRate (BigDecimal WithholdingRate)
	{
		set_Value (COLUMNNAME_WithholdingRate, WithholdingRate);
	}

	/** Get Withholding Rate.
		@return Withholding Rate applied to Document
	  */
	public BigDecimal getWithholdingRate () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WithholdingRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}