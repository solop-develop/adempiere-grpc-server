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

/** Generated Interface for C_Commission
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_C_Commission 
{

    /** TableName=C_Commission */
    public static final String Table_Name = "C_Commission";

    /** AD_Table_ID=429 */
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

    /** Column name C_Charge_ID */
    public static final String COLUMNNAME_C_Charge_ID = "C_Charge_ID";

	/** Set Charge.
	  * Additional document charges
	  */
	public void setC_Charge_ID (int C_Charge_ID);

	/** Get Charge.
	  * Additional document charges
	  */
	public int getC_Charge_ID();

	public I_C_Charge getC_Charge() throws RuntimeException;

    /** Column name C_CommissionGroup_ID */
    public static final String COLUMNNAME_C_CommissionGroup_ID = "C_CommissionGroup_ID";

	/** Set Commission Group	  */
	public void setC_CommissionGroup_ID (int C_CommissionGroup_ID);

	/** Get Commission Group	  */
	public int getC_CommissionGroup_ID();

	public I_C_CommissionGroup getC_CommissionGroup() throws RuntimeException;

    /** Column name C_Commission_ID */
    public static final String COLUMNNAME_C_Commission_ID = "C_Commission_ID";

	/** Set Commission.
	  * Commission
	  */
	public void setC_Commission_ID (int C_Commission_ID);

	/** Get Commission.
	  * Commission
	  */
	public int getC_Commission_ID();

    /** Column name C_CommissionType_ID */
    public static final String COLUMNNAME_C_CommissionType_ID = "C_CommissionType_ID";

	/** Set Commission Type.
	  * Defined for custom query on commission
	  */
	public void setC_CommissionType_ID (int C_CommissionType_ID);

	/** Get Commission Type.
	  * Defined for custom query on commission
	  */
	public int getC_CommissionType_ID();

	public I_C_CommissionType getC_CommissionType() throws RuntimeException;

    /** Column name C_Currency_ID */
    public static final String COLUMNNAME_C_Currency_ID = "C_Currency_ID";

	/** Set Currency.
	  * The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID);

	/** Get Currency.
	  * The Currency for this record
	  */
	public int getC_Currency_ID();

	public I_C_Currency getC_Currency() throws RuntimeException;

    /** Column name C_DocTypeInvoice_ID */
    public static final String COLUMNNAME_C_DocTypeInvoice_ID = "C_DocTypeInvoice_ID";

	/** Set Document Type for Invoice.
	  * Document type used for invoices generated from this sales document
	  */
	public void setC_DocTypeInvoice_ID (int C_DocTypeInvoice_ID);

	/** Get Document Type for Invoice.
	  * Document type used for invoices generated from this sales document
	  */
	public int getC_DocTypeInvoice_ID();

	public I_C_DocType getC_DocTypeInvoice() throws RuntimeException;

    /** Column name C_DocTypeOrder_ID */
    public static final String COLUMNNAME_C_DocTypeOrder_ID = "C_DocTypeOrder_ID";

	/** Set Document Type For Order	  */
	public void setC_DocTypeOrder_ID (int C_DocTypeOrder_ID);

	/** Get Document Type For Order	  */
	public int getC_DocTypeOrder_ID();

	public I_C_DocType getC_DocTypeOrder() throws RuntimeException;

    /** Column name C_DocTypeReverse_ID */
    public static final String COLUMNNAME_C_DocTypeReverse_ID = "C_DocTypeReverse_ID";

	/** Set DocType Reverse	  */
	public void setC_DocTypeReverse_ID (int C_DocTypeReverse_ID);

	/** Get DocType Reverse	  */
	public int getC_DocTypeReverse_ID();

	public I_C_DocType getC_DocTypeReverse() throws RuntimeException;

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

    /** Column name CreateFrom */
    public static final String COLUMNNAME_CreateFrom = "CreateFrom";

	/** Set Create lines from.
	  * Process which will generate a new document lines based on an existing document
	  */
	public void setCreateFrom (String CreateFrom);

	/** Get Create lines from.
	  * Process which will generate a new document lines based on an existing document
	  */
	public String getCreateFrom();

    /** Column name DateLastRun */
    public static final String COLUMNNAME_DateLastRun = "DateLastRun";

	/** Set Date last run.
	  * Date the process was last run.
	  */
	public void setDateLastRun (Timestamp DateLastRun);

	/** Get Date last run.
	  * Date the process was last run.
	  */
	public Timestamp getDateLastRun();

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

    /** Column name DocBasisType */
    public static final String COLUMNNAME_DocBasisType = "DocBasisType";

	/** Set Calculation Basis.
	  * Basis for the calculation the commission
	  */
	public void setDocBasisType (String DocBasisType);

	/** Get Calculation Basis.
	  * Basis for the calculation the commission
	  */
	public String getDocBasisType();

    /** Column name FrequencyType */
    public static final String COLUMNNAME_FrequencyType = "FrequencyType";

	/** Set Frequency Type.
	  * Frequency of event
	  */
	public void setFrequencyType (String FrequencyType);

	/** Get Frequency Type.
	  * Frequency of event
	  */
	public String getFrequencyType();

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

    /** Column name IsAllowRMA */
    public static final String COLUMNNAME_IsAllowRMA = "IsAllowRMA";

	/** Set Allow RMA.
	  * Allow to consider RMA
	  */
	public void setIsAllowRMA (boolean IsAllowRMA);

	/** Get Allow RMA.
	  * Allow to consider RMA
	  */
	public boolean isAllowRMA();

    /** Column name IsDaysDueFromPaymentTerm */
    public static final String COLUMNNAME_IsDaysDueFromPaymentTerm = "IsDaysDueFromPaymentTerm";

	/** Set Days due from Payment Term	  */
	public void setIsDaysDueFromPaymentTerm (boolean IsDaysDueFromPaymentTerm);

	/** Get Days due from Payment Term	  */
	public boolean isDaysDueFromPaymentTerm();

    /** Column name IsTotallyPaid */
    public static final String COLUMNNAME_IsTotallyPaid = "IsTotallyPaid";

	/** Set Paid totally.
	  * The document is totally paid
	  */
	public void setIsTotallyPaid (boolean IsTotallyPaid);

	/** Get Paid totally.
	  * The document is totally paid
	  */
	public boolean isTotallyPaid();

    /** Column name IsUseDocumentCurrency */
    public static final String COLUMNNAME_IsUseDocumentCurrency = "IsUseDocumentCurrency";

	/** Set Use Document Currency	  */
	public void setIsUseDocumentCurrency (boolean IsUseDocumentCurrency);

	/** Get Use Document Currency	  */
	public boolean isUseDocumentCurrency();

    /** Column name ListDetails */
    public static final String COLUMNNAME_ListDetails = "ListDetails";

	/** Set List Details.
	  * List document details
	  */
	public void setListDetails (boolean ListDetails);

	/** Get List Details.
	  * List document details
	  */
	public boolean isListDetails();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name SP011_CommissionForSalesReps */
    public static final String COLUMNNAME_SP011_CommissionForSalesReps = "SP011_CommissionForSalesReps";

	/** Set Commission For Sales Reps	  */
	public void setSP011_CommissionForSalesReps (boolean SP011_CommissionForSalesReps);

	/** Get Commission For Sales Reps	  */
	public boolean isSP011_CommissionForSalesReps();

    /** Column name SP027_Charge_ID */
    public static final String COLUMNNAME_SP027_Charge_ID = "SP027_Charge_ID";

	/** Set Charge.
	  * Additional document charges
	  */
	public void setSP027_Charge_ID (int SP027_Charge_ID);

	/** Get Charge.
	  * Additional document charges
	  */
	public int getSP027_Charge_ID();

	public I_C_Charge getSP027_Charge() throws RuntimeException;

    /** Column name SP027_IsTaxIncluded */
    public static final String COLUMNNAME_SP027_IsTaxIncluded = "SP027_IsTaxIncluded";

	/** Set Price includes Tax.
	  * Tax is included in the price 
	  */
	public void setSP027_IsTaxIncluded (boolean SP027_IsTaxIncluded);

	/** Get Price includes Tax.
	  * Tax is included in the price 
	  */
	public boolean isSP027_IsTaxIncluded();

    /** Column name SP027_Product_ID */
    public static final String COLUMNNAME_SP027_Product_ID = "SP027_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setSP027_Product_ID (int SP027_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getSP027_Product_ID();

	public I_M_Product getSP027_Product() throws RuntimeException;

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
