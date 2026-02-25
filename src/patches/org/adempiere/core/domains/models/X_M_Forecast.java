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
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for M_Forecast
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_M_Forecast extends PO implements I_M_Forecast, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260224L;

    /** Standard Constructor */
    public X_M_Forecast (Properties ctx, int M_Forecast_ID, String trxName)
    {
      super (ctx, M_Forecast_ID, trxName);
      /** if (M_Forecast_ID == 0)
        {
			setIsDefault (false);
			setM_Forecast_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_M_Forecast (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_Forecast[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_Calendar getC_Calendar() throws RuntimeException
    {
		return (I_C_Calendar)MTable.get(getCtx(), I_C_Calendar.Table_Name)
			.getPO(getC_Calendar_ID(), get_TrxName());	}

	/** Set Calendar.
		@param C_Calendar_ID 
		Accounting Calendar Name
	  */
	public void setC_Calendar_ID (int C_Calendar_ID)
	{
		if (C_Calendar_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_Calendar_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_Calendar_ID, Integer.valueOf(C_Calendar_ID));
	}

	/** Get Calendar.
		@return Accounting Calendar Name
	  */
	public int getC_Calendar_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Calendar_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Campaign getC_Campaign() throws RuntimeException
    {
		return (I_C_Campaign)MTable.get(getCtx(), I_C_Campaign.Table_Name)
			.getPO(getC_Campaign_ID(), get_TrxName());	}

	/** Set Campaign.
		@param C_Campaign_ID 
		Marketing Campaign
	  */
	public void setC_Campaign_ID (int C_Campaign_ID)
	{
		if (C_Campaign_ID < 1) 
			set_Value (COLUMNNAME_C_Campaign_ID, null);
		else 
			set_Value (COLUMNNAME_C_Campaign_ID, Integer.valueOf(C_Campaign_ID));
	}

	/** Get Campaign.
		@return Marketing Campaign
	  */
	public int getC_Campaign_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Campaign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Project getC_Project() throws RuntimeException
    {
		return (I_C_Project)MTable.get(getCtx(), I_C_Project.Table_Name)
			.getPO(getC_Project_ID(), get_TrxName());	}

	/** Set Project.
		@param C_Project_ID 
		Financial Project
	  */
	public void setC_Project_ID (int C_Project_ID)
	{
		if (C_Project_ID < 1) 
			set_Value (COLUMNNAME_C_Project_ID, null);
		else 
			set_Value (COLUMNNAME_C_Project_ID, Integer.valueOf(C_Project_ID));
	}

	/** Get Project.
		@return Financial Project
	  */
	public int getC_Project_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Project_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_ProjectPhase getC_ProjectPhase() throws RuntimeException
    {
		return (I_C_ProjectPhase)MTable.get(getCtx(), I_C_ProjectPhase.Table_Name)
			.getPO(getC_ProjectPhase_ID(), get_TrxName());	}

	/** Set Project Phase.
		@param C_ProjectPhase_ID 
		Phase of a Project
	  */
	public void setC_ProjectPhase_ID (int C_ProjectPhase_ID)
	{
		if (C_ProjectPhase_ID < 1) 
			set_Value (COLUMNNAME_C_ProjectPhase_ID, null);
		else 
			set_Value (COLUMNNAME_C_ProjectPhase_ID, Integer.valueOf(C_ProjectPhase_ID));
	}

	/** Get Project Phase.
		@return Phase of a Project
	  */
	public int getC_ProjectPhase_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ProjectPhase_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_SalesBudget getC_SalesBudget() throws RuntimeException
    {
		return (I_C_SalesBudget)MTable.get(getCtx(), I_C_SalesBudget.Table_Name)
			.getPO(getC_SalesBudget_ID(), get_TrxName());	}

	/** Set Sales Budget.
		@param C_SalesBudget_ID Sales Budget	  */
	public void setC_SalesBudget_ID (int C_SalesBudget_ID)
	{
		if (C_SalesBudget_ID < 1) 
			set_Value (COLUMNNAME_C_SalesBudget_ID, null);
		else 
			set_Value (COLUMNNAME_C_SalesBudget_ID, Integer.valueOf(C_SalesBudget_ID));
	}

	/** Get Sales Budget.
		@return Sales Budget	  */
	public int getC_SalesBudget_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_SalesBudget_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Year getC_Year() throws RuntimeException
    {
		return (I_C_Year)MTable.get(getCtx(), I_C_Year.Table_Name)
			.getPO(getC_Year_ID(), get_TrxName());	}

	/** Set Year.
		@param C_Year_ID 
		Calendar Year
	  */
	public void setC_Year_ID (int C_Year_ID)
	{
		if (C_Year_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_Year_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_Year_ID, Integer.valueOf(C_Year_ID));
	}

	/** Get Year.
		@return Calendar Year
	  */
	public int getC_Year_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Year_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** ForecastLevel AD_Reference_ID=54593 */
	public static final int FORECASTLEVEL_AD_Reference_ID=54593;
	/** Financial = FI */
	public static final String FORECASTLEVEL_Financial = "FI";
	/** Classification = CL */
	public static final String FORECASTLEVEL_Classification = "CL";
	/** Operational = OP */
	public static final String FORECASTLEVEL_Operational = "OP";
	/** Set Forecast Level.
		@param ForecastLevel 
		Financial, Commercial, or Operational forecast level
	  */
	public void setForecastLevel (String ForecastLevel)
	{

		set_Value (COLUMNNAME_ForecastLevel, ForecastLevel);
	}

	/** Get Forecast Level.
		@return Financial, Commercial, or Operational forecast level
	  */
	public String getForecastLevel () 
	{
		return (String)get_Value(COLUMNNAME_ForecastLevel);
	}

	/** Set Comment/Help.
		@param Help 
		Comment or Hint
	  */
	public void setHelp (String Help)
	{
		set_Value (COLUMNNAME_Help, Help);
	}

	/** Get Comment/Help.
		@return Comment or Hint
	  */
	public String getHelp () 
	{
		return (String)get_Value(COLUMNNAME_Help);
	}

	/** Set Default.
		@param IsDefault 
		Default value
	  */
	public void setIsDefault (boolean IsDefault)
	{
		set_Value (COLUMNNAME_IsDefault, Boolean.valueOf(IsDefault));
	}

	/** Get Default.
		@return Default value
	  */
	public boolean isDefault () 
	{
		Object oo = get_Value(COLUMNNAME_IsDefault);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Forecast.
		@param M_Forecast_ID 
		Material Forecast
	  */
	public void setM_Forecast_ID (int M_Forecast_ID)
	{
		if (M_Forecast_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Forecast_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Forecast_ID, Integer.valueOf(M_Forecast_ID));
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

	public I_M_PriceList getM_PriceList() throws RuntimeException
    {
		return (I_M_PriceList)MTable.get(getCtx(), I_M_PriceList.Table_Name)
			.getPO(getM_PriceList_ID(), get_TrxName());	}

	/** Set Price List.
		@param M_PriceList_ID 
		Unique identifier of a Price List
	  */
	public void setM_PriceList_ID (int M_PriceList_ID)
	{
		if (M_PriceList_ID < 1) 
			set_Value (COLUMNNAME_M_PriceList_ID, null);
		else 
			set_Value (COLUMNNAME_M_PriceList_ID, Integer.valueOf(M_PriceList_ID));
	}

	/** Get Price List.
		@return Unique identifier of a Price List
	  */
	public int getM_PriceList_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_PriceList_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set Overall Accuracy.
		@param OverallAccuracy 
		Overall forecast accuracy percentage
	  */
	public void setOverallAccuracy (BigDecimal OverallAccuracy)
	{
		set_Value (COLUMNNAME_OverallAccuracy, OverallAccuracy);
	}

	/** Get Overall Accuracy.
		@return Overall forecast accuracy percentage
	  */
	public BigDecimal getOverallAccuracy () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_OverallAccuracy);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_M_Forecast getParentForecast() throws RuntimeException
    {
		return (I_M_Forecast)MTable.get(getCtx(), I_M_Forecast.Table_Name)
			.getPO(getParentForecast_ID(), get_TrxName());	}

	/** Set Parent Forecast.
		@param ParentForecast_ID 
		Self-referencing parent forecast
	  */
	public void setParentForecast_ID (int ParentForecast_ID)
	{
		if (ParentForecast_ID < 1) 
			set_Value (COLUMNNAME_ParentForecast_ID, null);
		else 
			set_Value (COLUMNNAME_ParentForecast_ID, Integer.valueOf(ParentForecast_ID));
	}

	/** Get Parent Forecast.
		@return Self-referencing parent forecast
	  */
	public int getParentForecast_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ParentForecast_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_PP_Calendar getPP_Calendar() throws RuntimeException
    {
		return (I_PP_Calendar)MTable.get(getCtx(), I_PP_Calendar.Table_Name)
			.getPO(getPP_Calendar_ID(), get_TrxName());	}

	/** Set Operational Calendar.
		@param PP_Calendar_ID 
		Operational Period, allows to define the periods for the Operational Calendar
	  */
	public void setPP_Calendar_ID (int PP_Calendar_ID)
	{
		if (PP_Calendar_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_PP_Calendar_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_PP_Calendar_ID, Integer.valueOf(PP_Calendar_ID));
	}

	/** Get Operational Calendar.
		@return Operational Period, allows to define the periods for the Operational Calendar
	  */
	public int getPP_Calendar_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Calendar_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_PP_PeriodDefinition getPP_PeriodDefinition() throws RuntimeException
    {
		return (I_PP_PeriodDefinition)MTable.get(getCtx(), I_PP_PeriodDefinition.Table_Name)
			.getPO(getPP_PeriodDefinition_ID(), get_TrxName());	}

	/** Set Current Period.
		@param PP_PeriodDefinition_ID 
		Period Definition, allows to define time cycles for the Operational Calendar
	  */
	public void setPP_PeriodDefinition_ID (int PP_PeriodDefinition_ID)
	{
		if (PP_PeriodDefinition_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_PP_PeriodDefinition_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_PP_PeriodDefinition_ID, Integer.valueOf(PP_PeriodDefinition_ID));
	}

	/** Get Current Period.
		@return Period Definition, allows to define time cycles for the Operational Calendar
	  */
	public int getPP_PeriodDefinition_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_PeriodDefinition_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Processed.
		@param Processed 
		The document has been processed
	  */
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Processed On.
		@param ProcessedOn 
		The date+time (expressed in decimal format) when the document has been processed
	  */
	public void setProcessedOn (BigDecimal ProcessedOn)
	{
		set_Value (COLUMNNAME_ProcessedOn, ProcessedOn);
	}

	/** Get Processed On.
		@return The date+time (expressed in decimal format) when the document has been processed
	  */
	public BigDecimal getProcessedOn () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ProcessedOn);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Process Now.
		@param Processing Process Now	  */
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Process Now.
		@return Process Now	  */
	public boolean isProcessing () 
	{
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Total Actual Amount.
		@param TotalActualAmt 
		Total actual amount
	  */
	public void setTotalActualAmt (BigDecimal TotalActualAmt)
	{
		set_Value (COLUMNNAME_TotalActualAmt, TotalActualAmt);
	}

	/** Get Total Actual Amount.
		@return Total actual amount
	  */
	public BigDecimal getTotalActualAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalActualAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Total Forecast Amount.
		@param TotalForecastAmt 
		Total forecast amount
	  */
	public void setTotalForecastAmt (BigDecimal TotalForecastAmt)
	{
		set_Value (COLUMNNAME_TotalForecastAmt, TotalForecastAmt);
	}

	/** Get Total Forecast Amount.
		@return Total forecast amount
	  */
	public BigDecimal getTotalForecastAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalForecastAmt);
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