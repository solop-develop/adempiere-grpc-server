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
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for M_ForecastFact
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_M_ForecastFact extends PO implements I_M_ForecastFact, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260303L;

    /** Standard Constructor */
    public X_M_ForecastFact(Properties ctx, int M_ForecastFact_ID, String trxName)
    {
      super (ctx, M_ForecastFact_ID, trxName);
      /** if (M_ForecastFact_ID == 0)
        {
			setAD_Table_ID (0);
			setM_ForecastFact_ID (0);
			setRecord_ID (0);
        } */
    }

    /** Load Constructor */
    public X_M_ForecastFact(Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_ForecastFact[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_Table getAD_Table() throws RuntimeException
    {
		return (I_AD_Table)MTable.get(getCtx(), I_AD_Table.Table_Name)
			.getPO(getAD_Table_ID(), get_TrxName());	}

	/** Set Table.
		@param AD_Table_ID 
		Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1) 
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}



	/** Set Document Date.
		@param DateDoc 
		Date of the Document
	  */
	public void setDateDoc (Timestamp DateDoc)
	{
		set_Value (COLUMNNAME_DateDoc, DateDoc);
	}

	/** Get Document Date.
		@return Date of the Document
	  */
	public Timestamp getDateDoc () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateDoc);
	}

	public I_M_ForecastComparison getM_ForecastComparison() throws RuntimeException
    {
		return (I_M_ForecastComparison)MTable.get(getCtx(), I_M_ForecastComparison.Table_Name)
			.getPO(getM_ForecastComparison_ID(), get_TrxName());	}

	/** Set Forecast Comparison.
		@param M_ForecastComparison_ID Forecast Comparison	  */
	public void setM_ForecastComparison_ID (int M_ForecastComparison_ID)
	{
		if (M_ForecastComparison_ID < 1) 
			set_Value (COLUMNNAME_M_ForecastComparison_ID, null);
		else 
			set_Value (COLUMNNAME_M_ForecastComparison_ID, Integer.valueOf(M_ForecastComparison_ID));
	}

	/** Get Forecast Comparison.
		@return Forecast Comparison	  */
	public int getM_ForecastComparison_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ForecastComparison_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Forecast Fact.
		@param M_ForecastFact_ID Forecast Fact	  */
	public void setM_ForecastFact_ID (int M_ForecastFact_ID)
	{
		if (M_ForecastFact_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_ForecastFact_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_ForecastFact_ID, Integer.valueOf(M_ForecastFact_ID));
	}

	/** Get Forecast Fact.
		@return Forecast Fact	  */
	public int getM_ForecastFact_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ForecastFact_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Record ID.
		@param Record_ID 
		Direct internal record ID
	  */
	public void setRecord_ID (int Record_ID)
	{
		if (Record_ID < 0) 
			set_Value (COLUMNNAME_Record_ID, null);
		else 
			set_Value (COLUMNNAME_Record_ID, Integer.valueOf(Record_ID));
	}

	/** Get Record ID.
		@return Direct internal record ID
	  */
	public int getRecord_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Record_ID);
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