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

/** Generated Interface for C_BankAccount
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_C_BankAccount 
{

    /** TableName=C_BankAccount */
    public static final String Table_Name = "C_BankAccount";

    /** AD_Table_ID=297 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AccountNo */
    public static final String COLUMNNAME_AccountNo = "AccountNo";

	/** Set Account No.
	  * Account Number
	  */
	public void setAccountNo (String AccountNo);

	/** Get Account No.
	  * Account Number
	  */
	public String getAccountNo();

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

    /** Column name BankAccountType */
    public static final String COLUMNNAME_BankAccountType = "BankAccountType";

	/** Set Bank Account Type.
	  * Bank Account Type
	  */
	public void setBankAccountType (String BankAccountType);

	/** Get Bank Account Type.
	  * Bank Account Type
	  */
	public String getBankAccountType();

    /** Column name BBAN */
    public static final String COLUMNNAME_BBAN = "BBAN";

	/** Set BBAN.
	  * Basic Bank Account Number
	  */
	public void setBBAN (String BBAN);

	/** Get BBAN.
	  * Basic Bank Account Number
	  */
	public String getBBAN();

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

    /** Column name C_Bank_ID */
    public static final String COLUMNNAME_C_Bank_ID = "C_Bank_ID";

	/** Set Bank.
	  * Bank
	  */
	public void setC_Bank_ID (int C_Bank_ID);

	/** Get Bank.
	  * Bank
	  */
	public int getC_Bank_ID();

	public I_C_Bank getC_Bank() throws RuntimeException;

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

    /** Column name CreditLimit */
    public static final String COLUMNNAME_CreditLimit = "CreditLimit";

	/** Set Credit limit.
	  * Amount of Credit allowed
	  */
	public void setCreditLimit (BigDecimal CreditLimit);

	/** Get Credit limit.
	  * Amount of Credit allowed
	  */
	public BigDecimal getCreditLimit();

    /** Column name CurrentBalance */
    public static final String COLUMNNAME_CurrentBalance = "CurrentBalance";

	/** Set Current balance.
	  * Current Balance
	  */
	public void setCurrentBalance (BigDecimal CurrentBalance);

	/** Get Current balance.
	  * Current Balance
	  */
	public BigDecimal getCurrentBalance();

    /** Column name DefaultCollectDocType_ID */
    public static final String COLUMNNAME_DefaultCollectDocType_ID = "DefaultCollectDocType_ID";

	/** Set Default Collect Document Type.
	  * Default Collect Document Type from this bank or account
	  */
	public void setDefaultCollectDocType_ID (int DefaultCollectDocType_ID);

	/** Get Default Collect Document Type.
	  * Default Collect Document Type from this bank or account
	  */
	public int getDefaultCollectDocType_ID();

	public I_C_DocType getDefaultCollectDocType() throws RuntimeException;

    /** Column name DefaultOpeningCharge_ID */
    public static final String COLUMNNAME_DefaultOpeningCharge_ID = "DefaultOpeningCharge_ID";

	/** Set Default Opening Charge.
	  * Default Opening Charge for POS
	  */
	public void setDefaultOpeningCharge_ID (int DefaultOpeningCharge_ID);

	/** Get Default Opening Charge.
	  * Default Opening Charge for POS
	  */
	public int getDefaultOpeningCharge_ID();

	public I_C_Charge getDefaultOpeningCharge() throws RuntimeException;

    /** Column name DefaultPaymentDocType_ID */
    public static final String COLUMNNAME_DefaultPaymentDocType_ID = "DefaultPaymentDocType_ID";

	/** Set Default Payment Document Type.
	  * Default Payment Document Type from this bank or account
	  */
	public void setDefaultPaymentDocType_ID (int DefaultPaymentDocType_ID);

	/** Get Default Payment Document Type.
	  * Default Payment Document Type from this bank or account
	  */
	public int getDefaultPaymentDocType_ID();

	public I_C_DocType getDefaultPaymentDocType() throws RuntimeException;

    /** Column name DefaultWithdrawalCharge_ID */
    public static final String COLUMNNAME_DefaultWithdrawalCharge_ID = "DefaultWithdrawalCharge_ID";

	/** Set Default Withdrawal Charge.
	  * Default Withdrawal Charge for POS
	  */
	public void setDefaultWithdrawalCharge_ID (int DefaultWithdrawalCharge_ID);

	/** Get Default Withdrawal Charge.
	  * Default Withdrawal Charge for POS
	  */
	public int getDefaultWithdrawalCharge_ID();

	public I_C_Charge getDefaultWithdrawalCharge() throws RuntimeException;

    /** Column name DepositBankAccount_ID */
    public static final String COLUMNNAME_DepositBankAccount_ID = "DepositBankAccount_ID";

	/** Set Deposit Bank Account.
	  * Bank Account used for deposit from cash by default
	  */
	public void setDepositBankAccount_ID (int DepositBankAccount_ID);

	/** Get Deposit Bank Account.
	  * Bank Account used for deposit from cash by default
	  */
	public int getDepositBankAccount_ID();

	public I_C_BankAccount getDepositBankAccount() throws RuntimeException;

    /** Column name DepositCharge_ID */
    public static final String COLUMNNAME_DepositCharge_ID = "DepositCharge_ID";

	/** Set Deposit Charge.
	  * Charge used for deposit from cash
	  */
	public void setDepositCharge_ID (int DepositCharge_ID);

	/** Get Deposit Charge.
	  * Charge used for deposit from cash
	  */
	public int getDepositCharge_ID();

	public I_C_Charge getDepositCharge() throws RuntimeException;

    /** Column name DepositDocumentType_ID */
    public static final String COLUMNNAME_DepositDocumentType_ID = "DepositDocumentType_ID";

	/** Set Deposit Document Type.
	  * Deposit Document Type for Cash or bank
	  */
	public void setDepositDocumentType_ID (int DepositDocumentType_ID);

	/** Get Deposit Document Type.
	  * Deposit Document Type for Cash or bank
	  */
	public int getDepositDocumentType_ID();

	public I_C_DocType getDepositDocumentType() throws RuntimeException;

    /** Column name DepositTenderType */
    public static final String COLUMNNAME_DepositTenderType = "DepositTenderType";

	/** Set Deposit Tender Type.
	  * Tender type used for Deposit from cash
	  */
	public void setDepositTenderType (String DepositTenderType);

	/** Get Deposit Tender Type.
	  * Tender type used for Deposit from cash
	  */
	public String getDepositTenderType();

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

    /** Column name IBAN */
    public static final String COLUMNNAME_IBAN = "IBAN";

	/** Set IBAN.
	  * International Bank Account Number
	  */
	public void setIBAN (String IBAN);

	/** Get IBAN.
	  * International Bank Account Number
	  */
	public String getIBAN();

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

    /** Column name IsAutoDepositAfterClose */
    public static final String COLUMNNAME_IsAutoDepositAfterClose = "IsAutoDepositAfterClose";

	/** Set Deposit Automatically After Close Cash.
	  * Deposit automatically after close cash
	  */
	public void setIsAutoDepositAfterClose (boolean IsAutoDepositAfterClose);

	/** Get Deposit Automatically After Close Cash.
	  * Deposit automatically after close cash
	  */
	public boolean isAutoDepositAfterClose();

    /** Column name IsDefault */
    public static final String COLUMNNAME_IsDefault = "IsDefault";

	/** Set Default.
	  * Default value
	  */
	public void setIsDefault (boolean IsDefault);

	/** Get Default.
	  * Default value
	  */
	public boolean isDefault();

    /** Column name IsDefaultCurrency */
    public static final String COLUMNNAME_IsDefaultCurrency = "IsDefaultCurrency";

	/** Set Default Currency	  */
	public void setIsDefaultCurrency (boolean IsDefaultCurrency);

	/** Get Default Currency	  */
	public boolean isDefaultCurrency();

    /** Column name IsSOTrx */
    public static final String COLUMNNAME_IsSOTrx = "IsSOTrx";

	/** Set Sales Transaction.
	  * This is a Sales Transaction
	  */
	public void setIsSOTrx (boolean IsSOTrx);

	/** Get Sales Transaction.
	  * This is a Sales Transaction
	  */
	public boolean isSOTrx();

    /** Column name IsSplitDeposits */
    public static final String COLUMNNAME_IsSplitDeposits = "IsSplitDeposits";

	/** Set Split Deposits	  */
	public void setIsSplitDeposits (boolean IsSplitDeposits);

	/** Get Split Deposits	  */
	public boolean isSplitDeposits();

    /** Column name IsValidateCashOpening */
    public static final String COLUMNNAME_IsValidateCashOpening = "IsValidateCashOpening";

	/** Set Validate Cash Opening.
	  * Validate Cash Opening for this bank account
	  */
	public void setIsValidateCashOpening (boolean IsValidateCashOpening);

	/** Get Validate Cash Opening.
	  * Validate Cash Opening for this bank account
	  */
	public boolean isValidateCashOpening();

    /** Column name PaymentExportClass */
    public static final String COLUMNNAME_PaymentExportClass = "PaymentExportClass";

	/** Set Payment Export Class	  */
	public void setPaymentExportClass (String PaymentExportClass);

	/** Get Payment Export Class	  */
	public String getPaymentExportClass();

    /** Column name PayrollPaymentExportClass */
    public static final String COLUMNNAME_PayrollPaymentExportClass = "PayrollPaymentExportClass";

	/** Set Payment Export Class (Payroll)	  */
	public void setPayrollPaymentExportClass (String PayrollPaymentExportClass);

	/** Get Payment Export Class (Payroll)	  */
	public String getPayrollPaymentExportClass();

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

    /** Column name WithdrawalDocumentType_ID */
    public static final String COLUMNNAME_WithdrawalDocumentType_ID = "WithdrawalDocumentType_ID";

	/** Set Withdrawal Document Type.
	  * Withdrawal Document Type for Cash or bank
	  */
	public void setWithdrawalDocumentType_ID (int WithdrawalDocumentType_ID);

	/** Get Withdrawal Document Type.
	  * Withdrawal Document Type for Cash or bank
	  */
	public int getWithdrawalDocumentType_ID();

	public I_C_DocType getWithdrawalDocumentType() throws RuntimeException;
}
