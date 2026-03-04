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
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for M_ForecastLine
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_M_ForecastLine extends PO implements I_M_ForecastLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260302L;

    /** Standard Constructor */
    public X_M_ForecastLine(Properties ctx, int M_ForecastLine_ID, String trxName)
    {
      super (ctx, M_ForecastLine_ID, trxName);
      /** if (M_ForecastLine_ID == 0)
        {
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setM_Forecast_ID (0);
			setM_ForecastLine_ID (0);
			setM_Product_ID (0);
			setM_Warehouse_ID (0);
// @M_Warehouse_ID@
			setQty (Env.ZERO);
			setQtyCalculated (Env.ZERO);
        } */
    }

    /** Load Constructor */
    public X_M_ForecastLine(Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_ForecastLine[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set Confidence Level.
		@param ConfidenceLevel 
		Confidence level percentage 0-100
	  */
	public void setConfidenceLevel (BigDecimal ConfidenceLevel)
	{
		set_Value (COLUMNNAME_ConfidenceLevel, ConfidenceLevel);
	}

	/** Get Confidence Level.
		@return Confidence level percentage 0-100
	  */
	public BigDecimal getConfidenceLevel () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ConfidenceLevel);
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
			set_ValueNoCheck (COLUMNNAME_C_Period_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_Period_ID, Integer.valueOf(C_Period_ID));
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

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getC_Period_ID()));
    }

	/** Set Date Promised.
		@param DatePromised 
		Date Order was promised
	  */
	public void setDatePromised (Timestamp DatePromised)
	{
		set_Value (COLUMNNAME_DatePromised, DatePromised);
	}

	/** Get Date Promised.
		@return Date Order was promised
	  */
	public Timestamp getDatePromised () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DatePromised);
	}

	/** Set Date Reviewed.
		@param DateReviewed 
		Date of review
	  */
	public void setDateReviewed (Timestamp DateReviewed)
	{
		set_Value (COLUMNNAME_DateReviewed, DateReviewed);
	}

	/** Get Date Reviewed.
		@return Date of review
	  */
	public Timestamp getDateReviewed () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateReviewed);
	}

	/** Set Factors Applied.
		@param FactorsApplied 
		Number of adjustment factors applied
	  */
	public void setFactorsApplied (int FactorsApplied)
	{
		set_Value (COLUMNNAME_FactorsApplied, Integer.valueOf(FactorsApplied));
	}

	/** Get Factors Applied.
		@return Number of adjustment factors applied
	  */
	public int getFactorsApplied () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FactorsApplied);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** ForecastOrigin AD_Reference_ID=54592 */
	public static final int FORECASTORIGIN_AD_Reference_ID=54592;
	/** Manual = MA */
	public static final String FORECASTORIGIN_Manual = "MA";
	/** Calculated = CA */
	public static final String FORECASTORIGIN_Calculated = "CA";
	/** Adjusted = AD */
	public static final String FORECASTORIGIN_Adjusted = "AD";
	/** Imported = IM */
	public static final String FORECASTORIGIN_Imported = "IM";
	/** Set Forecast Origin.
		@param ForecastOrigin 
		Origin of forecast data
	  */
	public void setForecastOrigin (String ForecastOrigin)
	{

		set_Value (COLUMNNAME_ForecastOrigin, ForecastOrigin);
	}

	/** Get Forecast Origin.
		@return Origin of forecast data
	  */
	public String getForecastOrigin () 
	{
		return (String)get_Value(COLUMNNAME_ForecastOrigin);
	}

	public I_M_Brand getM_Brand() throws RuntimeException
    {
		return (I_M_Brand)MTable.get(getCtx(), I_M_Brand.Table_Name)
			.getPO(getM_Brand_ID(), get_TrxName());	}

	/** Set Product Brand.
		@param M_Brand_ID Product Brand	  */
	public void setM_Brand_ID (int M_Brand_ID)
	{
		if (M_Brand_ID < 1) 
			set_Value (COLUMNNAME_M_Brand_ID, null);
		else 
			set_Value (COLUMNNAME_M_Brand_ID, Integer.valueOf(M_Brand_ID));
	}

	/** Get Product Brand.
		@return Product Brand	  */
	public int getM_Brand_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Brand_ID);
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

	/** Set Forecast Line.
		@param M_ForecastLine_ID 
		Forecast Line
	  */
	public void setM_ForecastLine_ID (int M_ForecastLine_ID)
	{
		if (M_ForecastLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_ForecastLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_ForecastLine_ID, Integer.valueOf(M_ForecastLine_ID));
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

	public I_M_FreightCategory getM_FreightCategory() throws RuntimeException
    {
		return (I_M_FreightCategory)MTable.get(getCtx(), I_M_FreightCategory.Table_Name)
			.getPO(getM_FreightCategory_ID(), get_TrxName());	}

	/** Set Freight Category.
		@param M_FreightCategory_ID 
		Category of the Freight
	  */
	public void setM_FreightCategory_ID (int M_FreightCategory_ID)
	{
		if (M_FreightCategory_ID < 1) 
			set_Value (COLUMNNAME_M_FreightCategory_ID, null);
		else 
			set_Value (COLUMNNAME_M_FreightCategory_ID, Integer.valueOf(M_FreightCategory_ID));
	}

	/** Get Freight Category.
		@return Category of the Freight
	  */
	public int getM_FreightCategory_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_FreightCategory_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Industry_Sector getM_Industry_Sector() throws RuntimeException
    {
		return (I_M_Industry_Sector)MTable.get(getCtx(), I_M_Industry_Sector.Table_Name)
			.getPO(getM_Industry_Sector_ID(), get_TrxName());	}

	/** Set Industry Sector.
		@param M_Industry_Sector_ID Industry Sector	  */
	public void setM_Industry_Sector_ID (int M_Industry_Sector_ID)
	{
		if (M_Industry_Sector_ID < 1) 
			set_Value (COLUMNNAME_M_Industry_Sector_ID, null);
		else 
			set_Value (COLUMNNAME_M_Industry_Sector_ID, Integer.valueOf(M_Industry_Sector_ID));
	}

	/** Get Industry Sector.
		@return Industry Sector	  */
	public int getM_Industry_Sector_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Industry_Sector_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Material_Group getM_Material_Group() throws RuntimeException
    {
		return (I_M_Material_Group)MTable.get(getCtx(), I_M_Material_Group.Table_Name)
			.getPO(getM_Material_Group_ID(), get_TrxName());	}

	/** Set Material Group.
		@param M_Material_Group_ID Material Group	  */
	public void setM_Material_Group_ID (int M_Material_Group_ID)
	{
		if (M_Material_Group_ID < 1) 
			set_Value (COLUMNNAME_M_Material_Group_ID, null);
		else 
			set_Value (COLUMNNAME_M_Material_Group_ID, Integer.valueOf(M_Material_Group_ID));
	}

	/** Get Material Group.
		@return Material Group	  */
	public int getM_Material_Group_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Material_Group_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Material_Type getM_Material_Type() throws RuntimeException
    {
		return (I_M_Material_Type)MTable.get(getCtx(), I_M_Material_Type.Table_Name)
			.getPO(getM_Material_Type_ID(), get_TrxName());	}

	/** Set Material Type.
		@param M_Material_Type_ID Material Type	  */
	public void setM_Material_Type_ID (int M_Material_Type_ID)
	{
		if (M_Material_Type_ID < 1) 
			set_Value (COLUMNNAME_M_Material_Type_ID, null);
		else 
			set_Value (COLUMNNAME_M_Material_Type_ID, Integer.valueOf(M_Material_Type_ID));
	}

	/** Get Material Type.
		@return Material Type	  */
	public int getM_Material_Type_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Material_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_PartType getM_PartType() throws RuntimeException
    {
		return (I_M_PartType)MTable.get(getCtx(), I_M_PartType.Table_Name)
			.getPO(getM_PartType_ID(), get_TrxName());	}

	/** Set Part Type.
		@param M_PartType_ID Part Type	  */
	public void setM_PartType_ID (int M_PartType_ID)
	{
		if (M_PartType_ID < 1) 
			set_Value (COLUMNNAME_M_PartType_ID, null);
		else 
			set_Value (COLUMNNAME_M_PartType_ID, Integer.valueOf(M_PartType_ID));
	}

	/** Get Part Type.
		@return Part Type	  */
	public int getM_PartType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_PartType_ID);
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

	public I_M_Product_Class getM_Product_Class() throws RuntimeException
    {
		return (I_M_Product_Class)MTable.get(getCtx(), I_M_Product_Class.Table_Name)
			.getPO(getM_Product_Class_ID(), get_TrxName());	}

	/** Set Product Class.
		@param M_Product_Class_ID 
		Class of a Product
	  */
	public void setM_Product_Class_ID (int M_Product_Class_ID)
	{
		if (M_Product_Class_ID < 1) 
			set_Value (COLUMNNAME_M_Product_Class_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_Class_ID, Integer.valueOf(M_Product_Class_ID));
	}

	/** Get Product Class.
		@return Class of a Product
	  */
	public int getM_Product_Class_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_Class_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Product_Classification getM_Product_Classification() throws RuntimeException
    {
		return (I_M_Product_Classification)MTable.get(getCtx(), I_M_Product_Classification.Table_Name)
			.getPO(getM_Product_Classification_ID(), get_TrxName());	}

	/** Set Product Classification.
		@param M_Product_Classification_ID 
		Classification of a Product
	  */
	public void setM_Product_Classification_ID (int M_Product_Classification_ID)
	{
		if (M_Product_Classification_ID < 1) 
			set_Value (COLUMNNAME_M_Product_Classification_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_Classification_ID, Integer.valueOf(M_Product_Classification_ID));
	}

	/** Get Product Classification.
		@return Classification of a Product
	  */
	public int getM_Product_Classification_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_Classification_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Product_Group getM_Product_Group() throws RuntimeException
    {
		return (I_M_Product_Group)MTable.get(getCtx(), I_M_Product_Group.Table_Name)
			.getPO(getM_Product_Group_ID(), get_TrxName());	}

	/** Set Product Group.
		@param M_Product_Group_ID 
		Group of a Product
	  */
	public void setM_Product_Group_ID (int M_Product_Group_ID)
	{
		if (M_Product_Group_ID < 1) 
			set_Value (COLUMNNAME_M_Product_Group_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_Group_ID, Integer.valueOf(M_Product_Group_ID));
	}

	/** Get Product Group.
		@return Group of a Product
	  */
	public int getM_Product_Group_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_Group_ID);
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
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
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

	public I_M_Purchase_Group getM_Purchase_Group() throws RuntimeException
    {
		return (I_M_Purchase_Group)MTable.get(getCtx(), I_M_Purchase_Group.Table_Name)
			.getPO(getM_Purchase_Group_ID(), get_TrxName());	}

	/** Set Purchase Group.
		@param M_Purchase_Group_ID Purchase Group	  */
	public void setM_Purchase_Group_ID (int M_Purchase_Group_ID)
	{
		if (M_Purchase_Group_ID < 1) 
			set_Value (COLUMNNAME_M_Purchase_Group_ID, null);
		else 
			set_Value (COLUMNNAME_M_Purchase_Group_ID, Integer.valueOf(M_Purchase_Group_ID));
	}

	/** Get Purchase Group.
		@return Purchase Group	  */
	public int getM_Purchase_Group_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Purchase_Group_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Sales_Group getM_Sales_Group() throws RuntimeException
    {
		return (I_M_Sales_Group)MTable.get(getCtx(), I_M_Sales_Group.Table_Name)
			.getPO(getM_Sales_Group_ID(), get_TrxName());	}

	/** Set Sales Group.
		@param M_Sales_Group_ID Sales Group	  */
	public void setM_Sales_Group_ID (int M_Sales_Group_ID)
	{
		if (M_Sales_Group_ID < 1) 
			set_Value (COLUMNNAME_M_Sales_Group_ID, null);
		else 
			set_Value (COLUMNNAME_M_Sales_Group_ID, Integer.valueOf(M_Sales_Group_ID));
	}

	/** Get Sales Group.
		@return Sales Group	  */
	public int getM_Sales_Group_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Sales_Group_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Warehouse getM_Warehouse() throws RuntimeException
    {
		return (I_M_Warehouse)MTable.get(getCtx(), I_M_Warehouse.Table_Name)
			.getPO(getM_Warehouse_ID(), get_TrxName());	}

	/** Set Warehouse.
		@param M_Warehouse_ID 
		Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 0) 
			set_Value (COLUMNNAME_M_Warehouse_ID, null);
		else 
			set_Value (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_PP_Period getPP_Period() throws RuntimeException
    {
		return (I_PP_Period)MTable.get(getCtx(), I_PP_Period.Table_Name)
			.getPO(getPP_Period_ID(), get_TrxName());	}

	/** Set Operational Period.
		@param PP_Period_ID 
		Forecast Definition Periods.
	  */
	public void setPP_Period_ID (int PP_Period_ID)
	{
		if (PP_Period_ID < 1) 
			set_Value (COLUMNNAME_PP_Period_ID, null);
		else 
			set_Value (COLUMNNAME_PP_Period_ID, Integer.valueOf(PP_Period_ID));
	}

	/** Get Operational Period.
		@return Forecast Definition Periods.
	  */
	public int getPP_Period_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Period_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Quantity.
		@param Qty 
		Quantity
	  */
	public void setQty (BigDecimal Qty)
	{
		set_Value (COLUMNNAME_Qty, Qty);
	}

	/** Get Quantity.
		@return Quantity
	  */
	public BigDecimal getQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Qty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Adjusted Quantity.
		@param QtyAdjusted 
		Adjusted quantity after factor application
	  */
	public void setQtyAdjusted (BigDecimal QtyAdjusted)
	{
		set_Value (COLUMNNAME_QtyAdjusted, QtyAdjusted);
	}

	/** Get Adjusted Quantity.
		@return Adjusted quantity after factor application
	  */
	public BigDecimal getQtyAdjusted () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyAdjusted);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Base Quantity.
		@param QtyBase 
		Base quantity before adjustments
	  */
	public void setQtyBase (BigDecimal QtyBase)
	{
		set_Value (COLUMNNAME_QtyBase, QtyBase);
	}

	/** Get Base Quantity.
		@return Base quantity before adjustments
	  */
	public BigDecimal getQtyBase () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyBase);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Calculated Quantity.
		@param QtyCalculated 
		Calculated Quantity
	  */
	public void setQtyCalculated (BigDecimal QtyCalculated)
	{
		set_Value (COLUMNNAME_QtyCalculated, QtyCalculated);
	}

	/** Get Calculated Quantity.
		@return Calculated Quantity
	  */
	public BigDecimal getQtyCalculated () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyCalculated);
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

	public I_AD_User getReviewe() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getReviewedBy(), get_TrxName());	}

	/** Set Reviewed By.
		@param ReviewedBy 
		User who reviewed this line
	  */
	public void setReviewedBy (int ReviewedBy)
	{
		set_Value (COLUMNNAME_ReviewedBy, Integer.valueOf(ReviewedBy));
	}

	/** Get Reviewed By.
		@return User who reviewed this line
	  */
	public int getReviewedBy () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ReviewedBy);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_User getSalesRep() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getSalesRep_ID(), get_TrxName());	}

	/** Set Sales Representative.
		@param SalesRep_ID 
		Sales Representative or Company Agent
	  */
	public void setSalesRep_ID (int SalesRep_ID)
	{
		if (SalesRep_ID < 1) 
			set_Value (COLUMNNAME_SalesRep_ID, null);
		else 
			set_Value (COLUMNNAME_SalesRep_ID, Integer.valueOf(SalesRep_ID));
	}

	/** Get Sales Representative.
		@return Sales Representative or Company Agent
	  */
	public int getSalesRep_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SalesRep_ID);
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