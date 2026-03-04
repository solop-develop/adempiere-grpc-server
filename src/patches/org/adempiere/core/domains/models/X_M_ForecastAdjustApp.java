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
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for M_ForecastAdjustApp
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_M_ForecastAdjustApp extends PO implements I_M_ForecastAdjustApp, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260224L;

    /** Standard Constructor */
    public X_M_ForecastAdjustApp (Properties ctx, int M_ForecastAdjustApp_ID, String trxName)
    {
      super (ctx, M_ForecastAdjustApp_ID, trxName);
      /** if (M_ForecastAdjustApp_ID == 0)
        {
			setC_Period_ID (0);
			setFactorValue (Env.ZERO);
			setJustification (null);
			setM_ForecastAdjustApp_ID (0);
			setM_ForecastAdjustFactor_ID (0);
        } */
    }

    /** Load Constructor */
    public X_M_ForecastAdjustApp (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_ForecastAdjustApp[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_User getApprove() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getApprovedBy(), get_TrxName());	}

	/** Set Approved By.
		@param ApprovedBy 
		User who approved the budget
	  */
	public void setApprovedBy (int ApprovedBy)
	{
		set_Value (COLUMNNAME_ApprovedBy, Integer.valueOf(ApprovedBy));
	}

	/** Get Approved By.
		@return User who approved the budget
	  */
	public int getApprovedBy () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ApprovedBy);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Period getC_Period() throws RuntimeException
    {
		return (I_C_Period)MTable.get(getCtx(), I_C_Period.Table_Name)
			.getPO(getC_Period_ID(), get_TrxName());	}

	/** Set Period.
		@param C_Period_ID 
		Period of the Calendar
	  */
	public void setC_Period_ID (int C_Period_ID)
	{
		if (C_Period_ID < 1) 
			set_Value (COLUMNNAME_C_Period_ID, null);
		else 
			set_Value (COLUMNNAME_C_Period_ID, Integer.valueOf(C_Period_ID));
	}

	/** Get Period.
		@return Period of the Calendar
	  */
	public int getC_Period_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Period_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Period getC_Period_To() throws RuntimeException
    {
		return (I_C_Period)MTable.get(getCtx(), I_C_Period.Table_Name)
			.getPO(getC_Period_To_ID(), get_TrxName());	}

	/** Set Period To.
		@param C_Period_To_ID 
		Ending period for the factor application
	  */
	public void setC_Period_To_ID (int C_Period_To_ID)
	{
		if (C_Period_To_ID < 1) 
			set_Value (COLUMNNAME_C_Period_To_ID, null);
		else 
			set_Value (COLUMNNAME_C_Period_To_ID, Integer.valueOf(C_Period_To_ID));
	}

	/** Get Period To.
		@return Ending period for the factor application
	  */
	public int getC_Period_To_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Period_To_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Date Approved.
		@param DateApproved 
		Sets the approval date of a document.
	  */
	public void setDateApproved (Timestamp DateApproved)
	{
		set_Value (COLUMNNAME_DateApproved, DateApproved);
	}

	/** Get Date Approved.
		@return Sets the approval date of a document.
	  */
	public Timestamp getDateApproved () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateApproved);
	}

	/** Set Factor Value.
		@param FactorValue 
		Value of the adjustment factor applied
	  */
	public void setFactorValue (BigDecimal FactorValue)
	{
		set_Value (COLUMNNAME_FactorValue, FactorValue);
	}

	/** Get Factor Value.
		@return Value of the adjustment factor applied
	  */
	public BigDecimal getFactorValue () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_FactorValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Approved.
		@param IsApproved 
		Indicates if this document requires approval
	  */
	public void setIsApproved (boolean IsApproved)
	{
		set_Value (COLUMNNAME_IsApproved, Boolean.valueOf(IsApproved));
	}

	/** Get Approved.
		@return Indicates if this document requires approval
	  */
	public boolean isApproved () 
	{
		Object oo = get_Value(COLUMNNAME_IsApproved);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Manual Override.
		@param IsManualOverride 
		Manual override flag
	  */
	public void setIsManualOverride (boolean IsManualOverride)
	{
		set_Value (COLUMNNAME_IsManualOverride, Boolean.valueOf(IsManualOverride));
	}

	/** Get Manual Override.
		@return Manual override flag
	  */
	public boolean isManualOverride () 
	{
		Object oo = get_Value(COLUMNNAME_IsManualOverride);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Justification.
		@param Justification 
		Reason for applying this adjustment factor
	  */
	public void setJustification (String Justification)
	{
		set_Value (COLUMNNAME_Justification, Justification);
	}

	/** Get Justification.
		@return Reason for applying this adjustment factor
	  */
	public String getJustification () 
	{
		return (String)get_Value(COLUMNNAME_Justification);
	}

	/** Set Forecast Adjust Application.
		@param M_ForecastAdjustApp_ID Forecast Adjust Application	  */
	public void setM_ForecastAdjustApp_ID (int M_ForecastAdjustApp_ID)
	{
		if (M_ForecastAdjustApp_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_ForecastAdjustApp_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_ForecastAdjustApp_ID, Integer.valueOf(M_ForecastAdjustApp_ID));
	}

	/** Get Forecast Adjust Application.
		@return Forecast Adjust Application	  */
	public int getM_ForecastAdjustApp_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ForecastAdjustApp_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_ForecastAdjustFactor getM_ForecastAdjustFactor() throws RuntimeException
    {
		return (I_M_ForecastAdjustFactor)MTable.get(getCtx(), I_M_ForecastAdjustFactor.Table_Name)
			.getPO(getM_ForecastAdjustFactor_ID(), get_TrxName());	}

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

	public I_M_Forecast getM_Forecast() throws RuntimeException
    {
		return (I_M_Forecast)MTable.get(getCtx(), I_M_Forecast.Table_Name)
			.getPO(getM_Forecast_ID(), get_TrxName());	}

	/** Set Forecast.
		@param M_Forecast_ID 
		Material Forecast
	  */
	public void setM_Forecast_ID (int M_Forecast_ID)
	{
		if (M_Forecast_ID < 1) 
			set_Value (COLUMNNAME_M_Forecast_ID, null);
		else 
			set_Value (COLUMNNAME_M_Forecast_ID, Integer.valueOf(M_Forecast_ID));
	}

	/** Get Forecast.
		@return Material Forecast
	  */
	public int getM_Forecast_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Forecast_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_ForecastLine getM_ForecastLine() throws RuntimeException
    {
		return (I_M_ForecastLine)MTable.get(getCtx(), I_M_ForecastLine.Table_Name)
			.getPO(getM_ForecastLine_ID(), get_TrxName());	}

	/** Set Forecast Line.
		@param M_ForecastLine_ID 
		Forecast Line
	  */
	public void setM_ForecastLine_ID (int M_ForecastLine_ID)
	{
		if (M_ForecastLine_ID < 1) 
			set_Value (COLUMNNAME_M_ForecastLine_ID, null);
		else 
			set_Value (COLUMNNAME_M_ForecastLine_ID, Integer.valueOf(M_ForecastLine_ID));
	}

	/** Get Forecast Line.
		@return Forecast Line
	  */
	public int getM_ForecastLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ForecastLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Product_Category getM_Product_Category() throws RuntimeException
    {
		return (I_M_Product_Category)MTable.get(getCtx(), I_M_Product_Category.Table_Name)
			.getPO(getM_Product_Category_ID(), get_TrxName());	}

	/** Set Product Category.
		@param M_Product_Category_ID 
		Category of a Product
	  */
	public void setM_Product_Category_ID (int M_Product_Category_ID)
	{
		if (M_Product_Category_ID < 1) 
			set_Value (COLUMNNAME_M_Product_Category_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_Category_ID, Integer.valueOf(M_Product_Category_ID));
	}

	/** Get Product Category.
		@return Category of a Product
	  */
	public int getM_Product_Category_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_Category_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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