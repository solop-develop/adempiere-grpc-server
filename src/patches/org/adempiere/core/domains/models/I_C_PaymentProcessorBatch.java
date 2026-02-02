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

/** Generated Interface for C_PaymentProcessorBatch
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_C_PaymentProcessorBatch 
{

    /** TableName=C_PaymentProcessorBatch */
    public static final String Table_Name = "C_PaymentProcessorBatch";

    /** AD_Table_ID=55027 */
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

    /** Column name AllocatePayments */
    public static final String COLUMNNAME_AllocatePayments = "AllocatePayments";

	/** Set Allocate Payments.
	  * Allocate Payment from Bank Statement
	  */
	public void setAllocatePayments (String AllocatePayments);

	/** Get Allocate Payments.
	  * Allocate Payment from Bank Statement
	  */
	public String getAllocatePayments();

    /** Column name C_BankAccount_ID */
    public static final String COLUMNNAME_C_BankAccount_ID = "C_BankAccount_ID";

	/** Set Bank Account.
	  * Account at the Bank
	  */
	public void setC_BankAccount_ID (int C_BankAccount_ID);

	/** Get Bank Account.
	  * Account at the Bank
	  */
	public int getC_BankAccount_ID();

	public I_C_BankAccount getC_BankAccount() throws RuntimeException;

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

    /** Column name C_BPartner_Location_ID */
    public static final String COLUMNNAME_C_BPartner_Location_ID = "C_BPartner_Location_ID";

	/** Set Partner Location.
	  * Identifies the (ship to) address for this Business Partner
	  */
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID);

	/** Get Partner Location.
	  * Identifies the (ship to) address for this Business Partner
	  */
	public int getC_BPartner_Location_ID();

	public I_C_BPartner_Location getC_BPartner_Location() throws RuntimeException;

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

    /** Column name C_DocType_ID */
    public static final String COLUMNNAME_C_DocType_ID = "C_DocType_ID";

	/** Set Document Type.
	  * Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID);

	/** Get Document Type.
	  * Document type or rules
	  */
	public int getC_DocType_ID();

	public I_C_DocType getC_DocType() throws RuntimeException;

    /** Column name C_PaymentProcessorBatch_ID */
    public static final String COLUMNNAME_C_PaymentProcessorBatch_ID = "C_PaymentProcessorBatch_ID";

	/** Set Payment Processor Batch.
	  * Payment Processor Batch
	  */
	public void setC_PaymentProcessorBatch_ID (int C_PaymentProcessorBatch_ID);

	/** Get Payment Processor Batch.
	  * Payment Processor Batch
	  */
	public int getC_PaymentProcessorBatch_ID();

    /** Column name C_PaymentProcessor_ID */
    public static final String COLUMNNAME_C_PaymentProcessor_ID = "C_PaymentProcessor_ID";

	/** Set Payment Processor.
	  * Payment processor for electronic payments
	  */
	public void setC_PaymentProcessor_ID (int C_PaymentProcessor_ID);

	/** Get Payment Processor.
	  * Payment processor for electronic payments
	  */
	public int getC_PaymentProcessor_ID();

	public I_C_PaymentProcessor getC_PaymentProcessor() throws RuntimeException;

    /** Column name C_PPBatchConfiguration_ID */
    public static final String COLUMNNAME_C_PPBatchConfiguration_ID = "C_PPBatchConfiguration_ID";

	/** Set Payment Processor Batch Configuration	  */
	public void setC_PPBatchConfiguration_ID (int C_PPBatchConfiguration_ID);

	/** Get Payment Processor Batch Configuration	  */
	public int getC_PPBatchConfiguration_ID();

	public I_C_PPBatchConfiguration getC_PPBatchConfiguration() throws RuntimeException;

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

    /** Column name DateAcct */
    public static final String COLUMNNAME_DateAcct = "DateAcct";

	/** Set Account Date.
	  * Accounting Date
	  */
	public void setDateAcct (Timestamp DateAcct);

	/** Get Account Date.
	  * Accounting Date
	  */
	public Timestamp getDateAcct();

    /** Column name DateDoc */
    public static final String COLUMNNAME_DateDoc = "DateDoc";

	/** Set Document Date.
	  * Date of the Document
	  */
	public void setDateDoc (Timestamp DateDoc);

	/** Get Document Date.
	  * Date of the Document
	  */
	public Timestamp getDateDoc();

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

    /** Column name DiscountAmt */
    public static final String COLUMNNAME_DiscountAmt = "DiscountAmt";

	/** Set Discount Amount.
	  * Calculated amount of discount
	  */
	public void setDiscountAmt (BigDecimal DiscountAmt);

	/** Get Discount Amount.
	  * Calculated amount of discount
	  */
	public BigDecimal getDiscountAmt();

    /** Column name DocAction */
    public static final String COLUMNNAME_DocAction = "DocAction";

	/** Set Document Action.
	  * The targeted status of the document
	  */
	public void setDocAction (String DocAction);

	/** Get Document Action.
	  * The targeted status of the document
	  */
	public String getDocAction();

    /** Column name DocStatus */
    public static final String COLUMNNAME_DocStatus = "DocStatus";

	/** Set Document Status.
	  * The current status of the document
	  */
	public void setDocStatus (String DocStatus);

	/** Get Document Status.
	  * The current status of the document
	  */
	public String getDocStatus();

    /** Column name DocumentNo */
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";

	/** Set Document No.
	  * Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo);

	/** Get Document No.
	  * Document sequence number of the document
	  */
	public String getDocumentNo();

    /** Column name FeeAmt */
    public static final String COLUMNNAME_FeeAmt = "FeeAmt";

	/** Set Fee Amount.
	  * Fee amount in invoice currency
	  */
	public void setFeeAmt (BigDecimal FeeAmt);

	/** Get Fee Amount.
	  * Fee amount in invoice currency
	  */
	public BigDecimal getFeeAmt();

    /** Column name FinalAccount_ID */
    public static final String COLUMNNAME_FinalAccount_ID = "FinalAccount_ID";

	/** Set Final Account	  */
	public void setFinalAccount_ID (int FinalAccount_ID);

	/** Get Final Account	  */
	public int getFinalAccount_ID();

	public I_C_BankAccount getFinalAccount() throws RuntimeException;

    /** Column name GenerateAPInvoice */
    public static final String COLUMNNAME_GenerateAPInvoice = "GenerateAPInvoice";

	/** Set Generate AP Invoice.
	  * Generate AP Invoice from Document
	  */
	public void setGenerateAPInvoice (String GenerateAPInvoice);

	/** Get Generate AP Invoice.
	  * Generate AP Invoice from Document
	  */
	public String getGenerateAPInvoice();

    /** Column name GenerateARInvoice */
    public static final String COLUMNNAME_GenerateARInvoice = "GenerateARInvoice";

	/** Set Generate AR Invoice.
	  * Generate AR Invoice from Document
	  */
	public void setGenerateARInvoice (String GenerateARInvoice);

	/** Get Generate AR Invoice.
	  * Generate AR Invoice from Document
	  */
	public String getGenerateARInvoice();

    /** Column name GrandTotal */
    public static final String COLUMNNAME_GrandTotal = "GrandTotal";

	/** Set Grand Total.
	  * Total amount of document
	  */
	public void setGrandTotal (BigDecimal GrandTotal);

	/** Get Grand Total.
	  * Total amount of document
	  */
	public BigDecimal getGrandTotal();

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

    /** Column name IsAutomaticReceipt */
    public static final String COLUMNNAME_IsAutomaticReceipt = "IsAutomaticReceipt";

	/** Set Automatic Receipt	  */
	public void setIsAutomaticReceipt (boolean IsAutomaticReceipt);

	/** Get Automatic Receipt	  */
	public boolean isAutomaticReceipt();

    /** Column name IsManualFee */
    public static final String COLUMNNAME_IsManualFee = "IsManualFee";

	/** Set Manual Fee Calculation	  */
	public void setIsManualFee (boolean IsManualFee);

	/** Get Manual Fee Calculation	  */
	public boolean isManualFee();

    /** Column name OpenAmt */
    public static final String COLUMNNAME_OpenAmt = "OpenAmt";

	/** Set Open Amount.
	  * Open item amount
	  */
	public void setOpenAmt (BigDecimal OpenAmt);

	/** Get Open Amount.
	  * Open item amount
	  */
	public BigDecimal getOpenAmt();

    /** Column name PaidAmt */
    public static final String COLUMNNAME_PaidAmt = "PaidAmt";

	/** Set Paid Amount	  */
	public void setPaidAmt (BigDecimal PaidAmt);

	/** Get Paid Amount	  */
	public BigDecimal getPaidAmt();

    /** Column name PayAmt */
    public static final String COLUMNNAME_PayAmt = "PayAmt";

	/** Set Payment amount.
	  * Amount being paid
	  */
	public void setPayAmt (BigDecimal PayAmt);

	/** Get Payment amount.
	  * Amount being paid
	  */
	public BigDecimal getPayAmt();

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name TaxAmt */
    public static final String COLUMNNAME_TaxAmt = "TaxAmt";

	/** Set Tax Amount.
	  * Tax Amount for a document
	  */
	public void setTaxAmt (BigDecimal TaxAmt);

	/** Get Tax Amount.
	  * Tax Amount for a document
	  */
	public BigDecimal getTaxAmt();

    /** Column name TransitBankAccount_ID */
    public static final String COLUMNNAME_TransitBankAccount_ID = "TransitBankAccount_ID";

	/** Set Transit Bank Account.
	  * Transit Bank Account for movements
	  */
	public void setTransitBankAccount_ID (int TransitBankAccount_ID);

	/** Get Transit Bank Account.
	  * Transit Bank Account for movements
	  */
	public int getTransitBankAccount_ID();

	public I_C_BankAccount getTransitBankAccount() throws RuntimeException;

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

    /** Column name WithholdingAmt */
    public static final String COLUMNNAME_WithholdingAmt = "WithholdingAmt";

	/** Set Withholding Amt	  */
	public void setWithholdingAmt (BigDecimal WithholdingAmt);

	/** Get Withholding Amt	  */
	public BigDecimal getWithholdingAmt();
}
