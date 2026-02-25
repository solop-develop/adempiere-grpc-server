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
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for M_ForecastAdjustFactor
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_M_ForecastAdjustFactor extends PO implements I_M_ForecastAdjustFactor, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260224L;

    /** Standard Constructor */
    public X_M_ForecastAdjustFactor (Properties ctx, int M_ForecastAdjustFactor_ID, String trxName)
    {
      super (ctx, M_ForecastAdjustFactor_ID, trxName);
      /** if (M_ForecastAdjustFactor_ID == 0)
        {
			setFactorScope (null);
			setFactorType (null);
			setM_ForecastAdjustFactor_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_M_ForecastAdjustFactor (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_ForecastAdjustFactor[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Default Logic.
		@param DefaultValue 
		Default value hierarchy, separated by ;
	  */
	public void setDefaultValue (BigDecimal DefaultValue)
	{
		set_Value (COLUMNNAME_DefaultValue, DefaultValue);
	}

	/** Get Default Logic.
		@return Default value hierarchy, separated by ;
	  */
	public BigDecimal getDefaultValue () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_DefaultValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** FactorScope AD_Reference_ID=54591 */
	public static final int FACTORSCOPE_AD_Reference_ID=54591;
	/** Global = GL */
	public static final String FACTORSCOPE_Global = "GL";
	/** Category = CA */
	public static final String FACTORSCOPE_Category = "CA";
	/** Product = PR */
	public static final String FACTORSCOPE_Product = "PR";
	/** Sales Rep = SR */
	public static final String FACTORSCOPE_SalesRep = "SR";
	/** Set Factor Scope.
		@param FactorScope 
		Level at which the factor is applied
	  */
	public void setFactorScope (String FactorScope)
	{

		set_Value (COLUMNNAME_FactorScope, FactorScope);
	}

	/** Get Factor Scope.
		@return Level at which the factor is applied
	  */
	public String getFactorScope () 
	{
		return (String)get_Value(COLUMNNAME_FactorScope);
	}

	/** FactorType AD_Reference_ID=54590 */
	public static final int FACTORTYPE_AD_Reference_ID=54590;
	/** Multiplier = MU */
	public static final String FACTORTYPE_Multiplier = "MU";
	/** Additive = AD */
	public static final String FACTORTYPE_Additive = "AD";
	/** Override = OV */
	public static final String FACTORTYPE_Override = "OV";
	/** Set Factor Type.
		@param FactorType 
		Type of adjustment operation
	  */
	public void setFactorType (String FactorType)
	{

		set_Value (COLUMNNAME_FactorType, FactorType);
	}

	/** Get Factor Type.
		@return Type of adjustment operation
	  */
	public String getFactorType () 
	{
		return (String)get_Value(COLUMNNAME_FactorType);
	}

	/** Set System Factor.
		@param IsSystemFactor 
		Indicates this is a predefined system factor
	  */
	public void setIsSystemFactor (boolean IsSystemFactor)
	{
		set_Value (COLUMNNAME_IsSystemFactor, Boolean.valueOf(IsSystemFactor));
	}

	/** Get System Factor.
		@return Indicates this is a predefined system factor
	  */
	public boolean isSystemFactor () 
	{
		Object oo = get_Value(COLUMNNAME_IsSystemFactor);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

	/** Set Forecast Adjust Factor.
		@param M_ForecastAdjustFactor_ID Forecast Adjust Factor	  */
	public void setM_ForecastAdjustFactor_ID (int M_ForecastAdjustFactor_ID)
	{
		if (M_ForecastAdjustFactor_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_ForecastAdjustFactor_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_ForecastAdjustFactor_ID, Integer.valueOf(M_ForecastAdjustFactor_ID));
	}

	/** Get Forecast Adjust Factor.
		@return Forecast Adjust Factor	  */
	public int getM_ForecastAdjustFactor_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ForecastAdjustFactor_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
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

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}