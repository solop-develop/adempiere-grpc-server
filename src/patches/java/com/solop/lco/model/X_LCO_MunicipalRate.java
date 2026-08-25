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

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for LCO_MunicipalRate
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_LCO_MunicipalRate extends PO implements I_LCO_MunicipalRate, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260601L;

    /** Standard Constructor */
    public X_LCO_MunicipalRate (Properties ctx, int LCO_MunicipalRate_ID, String trxName)
    {
      super (ctx, LCO_MunicipalRate_ID, trxName);
      /** if (LCO_MunicipalRate_ID == 0)
        {
			setC_City_ID (0);
			setLCO_MunicipalConcept_ID (0);
			setLCO_MunicipalRate_ID (0);
        } */
    }

    /** Load Constructor */
    public X_LCO_MunicipalRate (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LCO_MunicipalRate[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.adempiere.core.domains.models.I_C_City getC_City() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_City)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_City.Table_Name)
			.getPO(getC_City_ID(), get_TrxName());	}

	/** Set City.
		@param C_City_ID 
		City
	  */
	public void setC_City_ID (int C_City_ID)
	{
		if (C_City_ID < 1) 
			set_Value (COLUMNNAME_C_City_ID, null);
		else 
			set_Value (COLUMNNAME_C_City_ID, Integer.valueOf(C_City_ID));
	}

	/** Get City.
		@return City
	  */
	public int getC_City_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_City_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set External Code.
		@param ExternalCode External Code	  */
	public void setExternalCode (String ExternalCode)
	{
		set_Value (COLUMNNAME_ExternalCode, ExternalCode);
	}

	/** Get External Code.
		@return External Code	  */
	public String getExternalCode () 
	{
		return (String)get_Value(COLUMNNAME_ExternalCode);
	}

	public com.solop.lco.model.I_LCO_MunicipalConcept getLCO_MunicipalConcept() throws RuntimeException
    {
		return (com.solop.lco.model.I_LCO_MunicipalConcept)MTable.get(getCtx(), com.solop.lco.model.I_LCO_MunicipalConcept.Table_Name)
			.getPO(getLCO_MunicipalConcept_ID(), get_TrxName());	}

	/** Set Municipal Concept.
		@param LCO_MunicipalConcept_ID Municipal Concept	  */
	public void setLCO_MunicipalConcept_ID (int LCO_MunicipalConcept_ID)
	{
		if (LCO_MunicipalConcept_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LCO_MunicipalConcept_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LCO_MunicipalConcept_ID, Integer.valueOf(LCO_MunicipalConcept_ID));
	}

	/** Get Municipal Concept.
		@return Municipal Concept	  */
	public int getLCO_MunicipalConcept_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_MunicipalConcept_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Municipal Rate.
		@param LCO_MunicipalRate_ID Municipal Rate	  */
	public void setLCO_MunicipalRate_ID (int LCO_MunicipalRate_ID)
	{
		if (LCO_MunicipalRate_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LCO_MunicipalRate_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LCO_MunicipalRate_ID, Integer.valueOf(LCO_MunicipalRate_ID));
	}

	/** Get Municipal Rate.
		@return Municipal Rate	  */
	public int getLCO_MunicipalRate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_MunicipalRate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rate per Thousand.
		@param RatePerThousand Rate per Thousand	  */
	public void setRatePerThousand (BigDecimal RatePerThousand)
	{
		set_Value (COLUMNNAME_RatePerThousand, RatePerThousand);
	}

	/** Get Rate per Thousand.
		@return Rate per Thousand	  */
	public BigDecimal getRatePerThousand () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_RatePerThousand);
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
}