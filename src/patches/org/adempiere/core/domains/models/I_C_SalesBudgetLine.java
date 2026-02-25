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

/** Generated Interface for C_SalesBudgetLine
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_C_SalesBudgetLine 
{

    /** TableName=C_SalesBudgetLine */
    public static final String Table_Name = "C_SalesBudgetLine";

    /** AD_Table_ID=55082 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name ActualAmt */
    public static final String COLUMNNAME_ActualAmt = "ActualAmt";

	/** Set Actual Amount.
	  * The actual amount
	  */
	public void setActualAmt (BigDecimal ActualAmt);

	/** Get Actual Amount.
	  * The actual amount
	  */
	public BigDecimal getActualAmt();

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

    /** Column name BudgetAmt */
    public static final String COLUMNNAME_BudgetAmt = "BudgetAmt";

	/** Set Budget Amount.
	  * Budgeted sales amount for this line
	  */
	public void setBudgetAmt (BigDecimal BudgetAmt);

	/** Get Budget Amount.
	  * Budgeted sales amount for this line
	  */
	public BigDecimal getBudgetAmt();

    /** Column name BudgetQty */
    public static final String COLUMNNAME_BudgetQty = "BudgetQty";

	/** Set Budget Quantity.
	  * Budgeted sales quantity for this line
	  */
	public void setBudgetQty (BigDecimal BudgetQty);

	/** Get Budget Quantity.
	  * Budgeted sales quantity for this line
	  */
	public BigDecimal getBudgetQty();

    /** Column name C_Channel_ID */
    public static final String COLUMNNAME_C_Channel_ID = "C_Channel_ID";

	/** Set Channel.
	  * Sales Channel
	  */
	public void setC_Channel_ID (int C_Channel_ID);

	/** Get Channel.
	  * Sales Channel
	  */
	public int getC_Channel_ID();

	public I_C_Channel getC_Channel() throws RuntimeException;

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

    /** Column name C_SalesBudget_ID */
    public static final String COLUMNNAME_C_SalesBudget_ID = "C_SalesBudget_ID";

	/** Set Sales Budget	  */
	public void setC_SalesBudget_ID (int C_SalesBudget_ID);

	/** Get Sales Budget	  */
	public int getC_SalesBudget_ID();

	public I_C_SalesBudget getC_SalesBudget() throws RuntimeException;

    /** Column name C_SalesBudgetLine_ID */
    public static final String COLUMNNAME_C_SalesBudgetLine_ID = "C_SalesBudgetLine_ID";

	/** Set Sales Budget Line	  */
	public void setC_SalesBudgetLine_ID (int C_SalesBudgetLine_ID);

	/** Get Sales Budget Line	  */
	public int getC_SalesBudgetLine_ID();

    /** Column name C_SalesRegion_ID */
    public static final String COLUMNNAME_C_SalesRegion_ID = "C_SalesRegion_ID";

	/** Set Sales Region.
	  * Sales coverage region
	  */
	public void setC_SalesRegion_ID (int C_SalesRegion_ID);

	/** Get Sales Region.
	  * Sales coverage region
	  */
	public int getC_SalesRegion_ID();

	public I_C_SalesRegion getC_SalesRegion() throws RuntimeException;

    /** Column name CumulativeActualAmt */
    public static final String COLUMNNAME_CumulativeActualAmt = "CumulativeActualAmt";

	/** Set Cumulative Actual.
	  * Year-to-date cumulative actual sales amount
	  */
	public void setCumulativeActualAmt (BigDecimal CumulativeActualAmt);

	/** Get Cumulative Actual.
	  * Year-to-date cumulative actual sales amount
	  */
	public BigDecimal getCumulativeActualAmt();

    /** Column name CumulativeBudgetAmt */
    public static final String COLUMNNAME_CumulativeBudgetAmt = "CumulativeBudgetAmt";

	/** Set Cumulative Budget.
	  * Year-to-date cumulative budget amount
	  */
	public void setCumulativeBudgetAmt (BigDecimal CumulativeBudgetAmt);

	/** Get Cumulative Budget.
	  * Year-to-date cumulative budget amount
	  */
	public BigDecimal getCumulativeBudgetAmt();

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

    /** Column name Line */
    public static final String COLUMNNAME_Line = "Line";

	/** Set Line No.
	  * Unique line for this document
	  */
	public void setLine (int Line);

	/** Get Line No.
	  * Unique line for this document
	  */
	public int getLine();

    /** Column name SalesRep_ID */
    public static final String COLUMNNAME_SalesRep_ID = "SalesRep_ID";

	/** Set Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public void setSalesRep_ID (int SalesRep_ID);

	/** Get Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public int getSalesRep_ID();

	public I_AD_User getSalesRep() throws RuntimeException;

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

    /** Column name VarianceAmt */
    public static final String COLUMNNAME_VarianceAmt = "VarianceAmt";

	/** Set Variance Amount.
	  * Difference between actual and budgeted amount
	  */
	public void setVarianceAmt (BigDecimal VarianceAmt);

	/** Get Variance Amount.
	  * Difference between actual and budgeted amount
	  */
	public BigDecimal getVarianceAmt();

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
