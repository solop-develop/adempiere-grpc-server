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

/** Generated Interface for M_ForecastKPISnapshot
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_M_ForecastKPISnapshot 
{

    /** TableName=M_ForecastKPISnapshot */
    public static final String Table_Name = "M_ForecastKPISnapshot";

    /** AD_Table_ID=55086 */
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

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name DimensionType */
    public static final String COLUMNNAME_DimensionType = "DimensionType";

	/** Set Dimension Type.
	  * Type of analysis dimension for this snapshot
	  */
	public void setDimensionType (String DimensionType);

	/** Get Dimension Type.
	  * Type of analysis dimension for this snapshot
	  */
	public String getDimensionType();

    /** Column name DimensionValue */
    public static final String COLUMNNAME_DimensionValue = "DimensionValue";

	/** Set Dimension Value.
	  * Identifier of the dimension entity
	  */
	public void setDimensionValue (int DimensionValue);

	/** Get Dimension Value.
	  * Identifier of the dimension entity
	  */
	public int getDimensionValue();

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

    /** Column name KPILevel */
    public static final String COLUMNNAME_KPILevel = "KPILevel";

	/** Set KPI Level.
	  * Aggregation level of the KPI metrics
	  */
	public void setKPILevel (String KPILevel);

	/** Get KPI Level.
	  * Aggregation level of the KPI metrics
	  */
	public String getKPILevel();

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

    /** Column name M_ForecastKPISnapshot_ID */
    public static final String COLUMNNAME_M_ForecastKPISnapshot_ID = "M_ForecastKPISnapshot_ID";

	/** Set Forecast KPI Snapshot	  */
	public void setM_ForecastKPISnapshot_ID (int M_ForecastKPISnapshot_ID);

	/** Get Forecast KPI Snapshot	  */
	public int getM_ForecastKPISnapshot_ID();

    /** Column name SKUsInAlert */
    public static final String COLUMNNAME_SKUsInAlert = "SKUsInAlert";

	/** Set SKUs In Alert.
	  * Number of SKUs with variance exceeding the alert threshold
	  */
	public void setSKUsInAlert (int SKUsInAlert);

	/** Get SKUs In Alert.
	  * Number of SKUs with variance exceeding the alert threshold
	  */
	public int getSKUsInAlert();

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

    /** Column name WeightedMAPE */
    public static final String COLUMNNAME_WeightedMAPE = "WeightedMAPE";

	/** Set Weighted MAPE.
	  * Weighted Mean Absolute Percentage Error
	  */
	public void setWeightedMAPE (BigDecimal WeightedMAPE);

	/** Get Weighted MAPE.
	  * Weighted Mean Absolute Percentage Error
	  */
	public BigDecimal getWeightedMAPE();
}
