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

/** Generated Model for M_ForecastKPISnapshot
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_M_ForecastKPISnapshot extends PO implements I_M_ForecastKPISnapshot, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260224L;

    /** Standard Constructor */
    public X_M_ForecastKPISnapshot (Properties ctx, int M_ForecastKPISnapshot_ID, String trxName)
    {
      super (ctx, M_ForecastKPISnapshot_ID, trxName);
      /** if (M_ForecastKPISnapshot_ID == 0)
        {
			setC_Period_ID (0);
			setDateCalculated (new Timestamp( System.currentTimeMillis() ));
			setKPILevel (null);
			setM_Forecast_ID (0);
			setM_ForecastKPISnapshot_ID (0);
        } */
    }

    /** Load Constructor */
    public X_M_ForecastKPISnapshot (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_ForecastKPISnapshot[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Bias.
		@param Bias 
		Systematic forecast bias
	  */
	public void setBias (BigDecimal Bias)
	{
		set_Value (COLUMNNAME_Bias, Bias);
	}

	/** Get Bias.
		@return Systematic forecast bias
	  */
	public BigDecimal getBias () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Bias);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Date Calculated.
		@param DateCalculated 
		Date the comparison metrics were calculated
	  */
	public void setDateCalculated (Timestamp DateCalculated)
	{
		set_Value (COLUMNNAME_DateCalculated, DateCalculated);
	}

	/** Get Date Calculated.
		@return Date the comparison metrics were calculated
	  */
	public Timestamp getDateCalculated () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateCalculated);
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

	/** DimensionType AD_Reference_ID=54595 */
	public static final int DIMENSIONTYPE_AD_Reference_ID=54595;
	/** Organization = OR */
	public static final String DIMENSIONTYPE_Organization = "OR";
	/** Sales Rep = SR */
	public static final String DIMENSIONTYPE_SalesRep = "SR";
	/** Region = RG */
	public static final String DIMENSIONTYPE_Region = "RG";
	/** Category = CA */
	public static final String DIMENSIONTYPE_Category = "CA";
	/** Product = PR */
	public static final String DIMENSIONTYPE_Product = "PR";
	/** Set Dimension Type.
		@param DimensionType 
		Type of analysis dimension for this snapshot
	  */
	public void setDimensionType (String DimensionType)
	{

		set_Value (COLUMNNAME_DimensionType, DimensionType);
	}

	/** Get Dimension Type.
		@return Type of analysis dimension for this snapshot
	  */
	public String getDimensionType () 
	{
		return (String)get_Value(COLUMNNAME_DimensionType);
	}

	/** Set Dimension Value.
		@param DimensionValue 
		Identifier of the dimension entity
	  */
	public void setDimensionValue (int DimensionValue)
	{
		set_Value (COLUMNNAME_DimensionValue, Integer.valueOf(DimensionValue));
	}

	/** Get Dimension Value.
		@return Identifier of the dimension entity
	  */
	public int getDimensionValue () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DimensionValue);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Forecast Accuracy.
		@param ForecastAccuracy 
		Forecast accuracy percentage
	  */
	public void setForecastAccuracy (BigDecimal ForecastAccuracy)
	{
		set_Value (COLUMNNAME_ForecastAccuracy, ForecastAccuracy);
	}

	/** Get Forecast Accuracy.
		@return Forecast accuracy percentage
	  */
	public BigDecimal getForecastAccuracy () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ForecastAccuracy);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** KPILevel AD_Reference_ID=54594 */
	public static final int KPILEVEL_AD_Reference_ID=54594;
	/** Financial = FI */
	public static final String KPILEVEL_Financial = "FI";
	/** Classification = CL */
	public static final String KPILEVEL_Classification = "CL";
	/** Operational = OP */
	public static final String KPILEVEL_Operational = "OP";
	/** Set KPI Level.
		@param KPILevel 
		Aggregation level of the KPI metrics
	  */
	public void setKPILevel (String KPILevel)
	{

		set_Value (COLUMNNAME_KPILevel, KPILevel);
	}

	/** Get KPI Level.
		@return Aggregation level of the KPI metrics
	  */
	public String getKPILevel () 
	{
		return (String)get_Value(COLUMNNAME_KPILevel);
	}

	/** Set MAPE.
		@param MAPE 
		Mean Absolute Percentage Error
	  */
	public void setMAPE (BigDecimal MAPE)
	{
		set_Value (COLUMNNAME_MAPE, MAPE);
	}

	/** Get MAPE.
		@return Mean Absolute Percentage Error
	  */
	public BigDecimal getMAPE () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MAPE);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Forecast KPI Snapshot.
		@param M_ForecastKPISnapshot_ID Forecast KPI Snapshot	  */
	public void setM_ForecastKPISnapshot_ID (int M_ForecastKPISnapshot_ID)
	{
		if (M_ForecastKPISnapshot_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_ForecastKPISnapshot_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_ForecastKPISnapshot_ID, Integer.valueOf(M_ForecastKPISnapshot_ID));
	}

	/** Get Forecast KPI Snapshot.
		@return Forecast KPI Snapshot	  */
	public int getM_ForecastKPISnapshot_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ForecastKPISnapshot_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SKUs In Alert.
		@param SKUsInAlert 
		Number of SKUs with variance exceeding the alert threshold
	  */
	public void setSKUsInAlert (int SKUsInAlert)
	{
		set_Value (COLUMNNAME_SKUsInAlert, Integer.valueOf(SKUsInAlert));
	}

	/** Get SKUs In Alert.
		@return Number of SKUs with variance exceeding the alert threshold
	  */
	public int getSKUsInAlert () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SKUsInAlert);
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

	/** Set Weighted MAPE.
		@param WeightedMAPE 
		Weighted Mean Absolute Percentage Error
	  */
	public void setWeightedMAPE (BigDecimal WeightedMAPE)
	{
		set_Value (COLUMNNAME_WeightedMAPE, WeightedMAPE);
	}

	/** Get Weighted MAPE.
		@return Weighted Mean Absolute Percentage Error
	  */
	public BigDecimal getWeightedMAPE () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WeightedMAPE);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}