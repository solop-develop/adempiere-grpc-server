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

/** Generated Model for M_ForecastComparison
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_M_ForecastComparison extends PO implements I_M_ForecastComparison, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260224L;

    /** Standard Constructor */
    public X_M_ForecastComparison (Properties ctx, int M_ForecastComparison_ID, String trxName)
    {
      super (ctx, M_ForecastComparison_ID, trxName);
      /** if (M_ForecastComparison_ID == 0)
        {
			setC_Period_ID (0);
			setM_ForecastComparison_ID (0);
			setM_Forecast_ID (0);
			setM_Product_ID (0);
			setQtyActual (Env.ZERO);
// 0
			setQtyForecast (Env.ZERO);
// 0
        } */
    }

    /** Load Constructor */
    public X_M_ForecastComparison (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_ForecastComparison[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Actual Amount.
		@param AmtActual 
		Actual sales amount in the period
	  */
	public void setAmtActual (BigDecimal AmtActual)
	{
		set_Value (COLUMNNAME_AmtActual, AmtActual);
	}

	/** Get Actual Amount.
		@return Actual sales amount in the period
	  */
	public BigDecimal getAmtActual () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_AmtActual);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Forecast Amount.
		@param AmtForecast 
		Forecasted amount for the period
	  */
	public void setAmtForecast (BigDecimal AmtForecast)
	{
		set_Value (COLUMNNAME_AmtForecast, AmtForecast);
	}

	/** Get Forecast Amount.
		@return Forecasted amount for the period
	  */
	public BigDecimal getAmtForecast () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_AmtForecast);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Amount Variance.
		@param AmtVariance 
		Difference between actual and forecasted amount
	  */
	public void setAmtVariance (BigDecimal AmtVariance)
	{
		set_Value (COLUMNNAME_AmtVariance, AmtVariance);
	}

	/** Get Amount Variance.
		@return Difference between actual and forecasted amount
	  */
	public BigDecimal getAmtVariance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_AmtVariance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** ComparisonSource AD_Reference_ID=54589 */
	public static final int COMPARISONSOURCE_AD_Reference_ID=54589;
	/** Order = OR */
	public static final String COMPARISONSOURCE_Order = "OR";
	/** Shipment = SH */
	public static final String COMPARISONSOURCE_Shipment = "SH";
	/** Invoice = IN */
	public static final String COMPARISONSOURCE_Invoice = "IN";
	/** Set Comparison Source.
		@param ComparisonSource 
		Document source used for actual sales comparison
	  */
	public void setComparisonSource (String ComparisonSource)
	{

		set_Value (COLUMNNAME_ComparisonSource, ComparisonSource);
	}

	/** Get Comparison Source.
		@return Document source used for actual sales comparison
	  */
	public String getComparisonSource () 
	{
		return (String)get_Value(COLUMNNAME_ComparisonSource);
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

	/** Set MAD.
		@param MAD 
		Mean Absolute Deviation
	  */
	public void setMAD (BigDecimal MAD)
	{
		set_Value (COLUMNNAME_MAD, MAD);
	}

	/** Get MAD.
		@return Mean Absolute Deviation
	  */
	public BigDecimal getMAD () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MAD);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Forecast Comparison.
		@param M_ForecastComparison_ID Forecast Comparison	  */
	public void setM_ForecastComparison_ID (int M_ForecastComparison_ID)
	{
		if (M_ForecastComparison_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_ForecastComparison_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_ForecastComparison_ID, Integer.valueOf(M_ForecastComparison_ID));
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

	/** Set Actual Quantity.
		@param QtyActual 
		Actual quantity sold in the period
	  */
	public void setQtyActual (BigDecimal QtyActual)
	{
		set_Value (COLUMNNAME_QtyActual, QtyActual);
	}

	/** Get Actual Quantity.
		@return Actual quantity sold in the period
	  */
	public BigDecimal getQtyActual () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyActual);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Forecast Quantity.
		@param QtyForecast 
		Forecasted quantity for the period
	  */
	public void setQtyForecast (BigDecimal QtyForecast)
	{
		set_Value (COLUMNNAME_QtyForecast, QtyForecast);
	}

	/** Get Forecast Quantity.
		@return Forecasted quantity for the period
	  */
	public BigDecimal getQtyForecast () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyForecast);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Quantity Variance.
		@param QtyVariance 
		Difference between actual and forecasted quantity
	  */
	public void setQtyVariance (BigDecimal QtyVariance)
	{
		set_Value (COLUMNNAME_QtyVariance, QtyVariance);
	}

	/** Get Quantity Variance.
		@return Difference between actual and forecasted quantity
	  */
	public BigDecimal getQtyVariance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyVariance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Requires Review.
		@param RequiresReview 
		Indicates this record needs manual review
	  */
	public void setRequiresReview (boolean RequiresReview)
	{
		set_Value (COLUMNNAME_RequiresReview, Boolean.valueOf(RequiresReview));
	}

	/** Get Requires Review.
		@return Indicates this record needs manual review
	  */
	public boolean isRequiresReview () 
	{
		Object oo = get_Value(COLUMNNAME_RequiresReview);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Review Notes.
		@param ReviewNotes 
		Notes from the variance review
	  */
	public void setReviewNotes (String ReviewNotes)
	{
		set_Value (COLUMNNAME_ReviewNotes, ReviewNotes);
	}

	/** Get Review Notes.
		@return Notes from the variance review
	  */
	public String getReviewNotes () 
	{
		return (String)get_Value(COLUMNNAME_ReviewNotes);
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

	/** Set Variance %.
		@param VariancePct 
		Variance expressed as percentage
	  */
	public void setVariancePct (BigDecimal VariancePct)
	{
		set_Value (COLUMNNAME_VariancePct, VariancePct);
	}

	/** Get Variance %.
		@return Variance expressed as percentage
	  */
	public BigDecimal getVariancePct () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_VariancePct);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}