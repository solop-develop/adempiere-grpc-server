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

/** Generated Interface for M_ForecastAdjustApp
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_M_ForecastAdjustApp 
{

    /** TableName=M_ForecastAdjustApp */
    public static final String Table_Name = "M_ForecastAdjustApp";

    /** AD_Table_ID=55085 */
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

    /** Column name ApprovedBy */
    public static final String COLUMNNAME_ApprovedBy = "ApprovedBy";

	/** Set Approved By.
	  * User who approved the budget
	  */
	public void setApprovedBy (int ApprovedBy);

	/** Get Approved By.
	  * User who approved the budget
	  */
	public int getApprovedBy();

	public I_AD_User getApprove() throws RuntimeException;

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

    /** Column name C_Period_To_ID */
    public static final String COLUMNNAME_C_Period_To_ID = "C_Period_To_ID";

	/** Set Period To.
	  * Ending period for the factor application
	  */
	public void setC_Period_To_ID (int C_Period_To_ID);

	/** Get Period To.
	  * Ending period for the factor application
	  */
	public int getC_Period_To_ID();

	public I_C_Period getC_Period_To() throws RuntimeException;

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

    /** Column name DateApproved */
    public static final String COLUMNNAME_DateApproved = "DateApproved";

	/** Set Date Approved.
	  * Sets the approval date of a document.
	  */
	public void setDateApproved (Timestamp DateApproved);

	/** Get Date Approved.
	  * Sets the approval date of a document.
	  */
	public Timestamp getDateApproved();

    /** Column name FactorValue */
    public static final String COLUMNNAME_FactorValue = "FactorValue";

	/** Set Factor Value.
	  * Value of the adjustment factor applied
	  */
	public void setFactorValue (BigDecimal FactorValue);

	/** Get Factor Value.
	  * Value of the adjustment factor applied
	  */
	public BigDecimal getFactorValue();

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

    /** Column name IsApproved */
    public static final String COLUMNNAME_IsApproved = "IsApproved";

	/** Set Approved.
	  * Indicates if this document requires approval
	  */
	public void setIsApproved (boolean IsApproved);

	/** Get Approved.
	  * Indicates if this document requires approval
	  */
	public boolean isApproved();

    /** Column name IsManualOverride */
    public static final String COLUMNNAME_IsManualOverride = "IsManualOverride";

	/** Set Manual Override.
	  * Manual override flag
	  */
	public void setIsManualOverride (boolean IsManualOverride);

	/** Get Manual Override.
	  * Manual override flag
	  */
	public boolean isManualOverride();

    /** Column name Justification */
    public static final String COLUMNNAME_Justification = "Justification";

	/** Set Justification.
	  * Reason for applying this adjustment factor
	  */
	public void setJustification (String Justification);

	/** Get Justification.
	  * Reason for applying this adjustment factor
	  */
	public String getJustification();

    /** Column name M_ForecastAdjustApp_ID */
    public static final String COLUMNNAME_M_ForecastAdjustApp_ID = "M_ForecastAdjustApp_ID";

	/** Set Forecast Adjust Application	  */
	public void setM_ForecastAdjustApp_ID (int M_ForecastAdjustApp_ID);

	/** Get Forecast Adjust Application	  */
	public int getM_ForecastAdjustApp_ID();

    /** Column name M_ForecastAdjustFactor_ID */
    public static final String COLUMNNAME_M_ForecastAdjustFactor_ID = "M_ForecastAdjustFactor_ID";

	/** Set Forecast Adjust Factor	  */
	public void setM_ForecastAdjustFactor_ID (int M_ForecastAdjustFactor_ID);

	/** Get Forecast Adjust Factor	  */
	public int getM_ForecastAdjustFactor_ID();

	public I_M_ForecastAdjustFactor getM_ForecastAdjustFactor() throws RuntimeException;

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

    /** Column name M_Product_Category_ID */
    public static final String COLUMNNAME_M_Product_Category_ID = "M_Product_Category_ID";

	/** Set Product Category.
	  * Category of a Product
	  */
	public void setM_Product_Category_ID (int M_Product_Category_ID);

	/** Get Product Category.
	  * Category of a Product
	  */
	public int getM_Product_Category_ID();

	public I_M_Product_Category getM_Product_Category() throws RuntimeException;

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
}
