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
package org.adempiere.core.domains.models;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Interface for M_ForecastComparison
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_M_ForecastComparison 
{

    /** TableName=M_ForecastComparison */
    public static final String Table_Name = "M_ForecastComparison";

    /** AD_Table_ID=55083 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name AmtActual */
    public static final String COLUMNNAME_AmtActual = "AmtActual";

	/** Set Actual Amount.
	  * Actual sales amount in the period
	  */
	public void setAmtActual (BigDecimal AmtActual);

	/** Get Actual Amount.
	  * Actual sales amount in the period
	  */
	public BigDecimal getAmtActual();

    /** Column name AmtForecast */
    public static final String COLUMNNAME_AmtForecast = "AmtForecast";

	/** Set Forecast Amount.
	  * Forecasted amount for the period
	  */
	public void setAmtForecast (BigDecimal AmtForecast);

	/** Get Forecast Amount.
	  * Forecasted amount for the period
	  */
	public BigDecimal getAmtForecast();

    /** Column name AmtVariance */
    public static final String COLUMNNAME_AmtVariance = "AmtVariance";

	/** Set Amount Variance.
	  * Difference between actual and forecasted amount
	  */
	public void setAmtVariance (BigDecimal AmtVariance);

	/** Get Amount Variance.
	  * Difference between actual and forecasted amount
	  */
	public BigDecimal getAmtVariance();

    /** Column name Bias */
    public static final String COLUMNNAME_Bias = "Bias";

	/** Set Bias.
	  * Systematic forecast bias
	  */
	public void setBias (BigDecimal Bias);

	/** Get Bias.
	  * Systematic forecast bias
	  */
	public BigDecimal getBias();

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set Business Partner .
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get Business Partner .
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public I_C_BPartner getC_BPartner() throws RuntimeException;

    /** Column name ComparisonSource */
    public static final String COLUMNNAME_ComparisonSource = "ComparisonSource";

	/** Set Comparison Source.
	  * Document source used for actual sales comparison
	  */
	public void setComparisonSource (String ComparisonSource);

	/** Get Comparison Source.
	  * Document source used for actual sales comparison
	  */
	public String getComparisonSource();

    /** Column name C_Period_ID */
    public static final String COLUMNNAME_C_Period_ID = "C_Period_ID";

	/** Set Period.
	  * Period of the Calendar
	  */
	public void setC_Period_ID (int C_Period_ID);

	/** Get Period.
	  * Period of the Calendar
	  */
	public int getC_Period_ID();

	public I_C_Period getC_Period() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DateCalculated */
    public static final String COLUMNNAME_DateCalculated = "DateCalculated";

	/** Set Date Calculated.
	  * Date the comparison metrics were calculated
	  */
	public void setDateCalculated (Timestamp DateCalculated);

	/** Get Date Calculated.
	  * Date the comparison metrics were calculated
	  */
	public Timestamp getDateCalculated();

    /** Column name ForecastAccuracy */
    public static final String COLUMNNAME_ForecastAccuracy = "ForecastAccuracy";

	/** Set Forecast Accuracy.
	  * Forecast accuracy percentage
	  */
	public void setForecastAccuracy (BigDecimal ForecastAccuracy);

	/** Get Forecast Accuracy.
	  * Forecast accuracy percentage
	  */
	public BigDecimal getForecastAccuracy();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name MAD */
    public static final String COLUMNNAME_MAD = "MAD";

	/** Set MAD.
	  * Mean Absolute Deviation
	  */
	public void setMAD (BigDecimal MAD);

	/** Get MAD.
	  * Mean Absolute Deviation
	  */
	public BigDecimal getMAD();

    /** Column name MAPE */
    public static final String COLUMNNAME_MAPE = "MAPE";

	/** Set MAPE.
	  * Mean Absolute Percentage Error
	  */
	public void setMAPE (BigDecimal MAPE);

	/** Get MAPE.
	  * Mean Absolute Percentage Error
	  */
	public BigDecimal getMAPE();

    /** Column name M_ForecastComparison_ID */
    public static final String COLUMNNAME_M_ForecastComparison_ID = "M_ForecastComparison_ID";

	/** Set Forecast Comparison	  */
	public void setM_ForecastComparison_ID (int M_ForecastComparison_ID);

	/** Get Forecast Comparison	  */
	public int getM_ForecastComparison_ID();

    /** Column name M_Forecast_ID */
    public static final String COLUMNNAME_M_Forecast_ID = "M_Forecast_ID";

	/** Set Forecast.
	  * Material Forecast
	  */
	public void setM_Forecast_ID (int M_Forecast_ID);

	/** Get Forecast.
	  * Material Forecast
	  */
	public int getM_Forecast_ID();

	public I_M_Forecast getM_Forecast() throws RuntimeException;

    /** Column name M_ForecastLine_ID */
    public static final String COLUMNNAME_M_ForecastLine_ID = "M_ForecastLine_ID";

	/** Set Forecast Line.
	  * Forecast Line
	  */
	public void setM_ForecastLine_ID (int M_ForecastLine_ID);

	/** Get Forecast Line.
	  * Forecast Line
	  */
	public int getM_ForecastLine_ID();

	public I_M_ForecastLine getM_ForecastLine() throws RuntimeException;

    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getM_Product_ID();

	public I_M_Product getM_Product() throws RuntimeException;

    /** Column name QtyActual */
    public static final String COLUMNNAME_QtyActual = "QtyActual";

	/** Set Actual Quantity.
	  * Actual quantity sold in the period
	  */
	public void setQtyActual (BigDecimal QtyActual);

	/** Get Actual Quantity.
	  * Actual quantity sold in the period
	  */
	public BigDecimal getQtyActual();

    /** Column name QtyForecast */
    public static final String COLUMNNAME_QtyForecast = "QtyForecast";

	/** Set Forecast Quantity.
	  * Forecasted quantity for the period
	  */
	public void setQtyForecast (BigDecimal QtyForecast);

	/** Get Forecast Quantity.
	  * Forecasted quantity for the period
	  */
	public BigDecimal getQtyForecast();

    /** Column name QtyVariance */
    public static final String COLUMNNAME_QtyVariance = "QtyVariance";

	/** Set Quantity Variance.
	  * Difference between actual and forecasted quantity
	  */
	public void setQtyVariance (BigDecimal QtyVariance);

	/** Get Quantity Variance.
	  * Difference between actual and forecasted quantity
	  */
	public BigDecimal getQtyVariance();

    /** Column name RequiresReview */
    public static final String COLUMNNAME_RequiresReview = "RequiresReview";

	/** Set Requires Review.
	  * Indicates this record needs manual review
	  */
	public void setRequiresReview (boolean RequiresReview);

	/** Get Requires Review.
	  * Indicates this record needs manual review
	  */
	public boolean isRequiresReview();

    /** Column name ReviewNotes */
    public static final String COLUMNNAME_ReviewNotes = "ReviewNotes";

	/** Set Review Notes.
	  * Notes from the variance review
	  */
	public void setReviewNotes (String ReviewNotes);

	/** Get Review Notes.
	  * Notes from the variance review
	  */
	public String getReviewNotes();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name UUID */
    public static final String COLUMNNAME_UUID = "UUID";

	/** Set Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID);

	/** Get Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public String getUUID();

    /** Column name VariancePct */
    public static final String COLUMNNAME_VariancePct = "VariancePct";

	/** Set Variance %.
	  * Variance expressed as percentage
	  */
	public void setVariancePct (BigDecimal VariancePct);

	/** Get Variance %.
	  * Variance expressed as percentage
	  */
	public BigDecimal getVariancePct();
}
