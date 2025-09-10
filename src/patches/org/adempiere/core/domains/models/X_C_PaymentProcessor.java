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
import java.util.Properties;

/** Generated Model for C_PaymentProcessor
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_PaymentProcessor extends PO implements I_C_PaymentProcessor, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250910L;

    /** Standard Constructor */
    public X_C_PaymentProcessor (Properties ctx, int C_PaymentProcessor_ID, String trxName)
    {
      super (ctx, C_PaymentProcessor_ID, trxName);
      /** if (C_PaymentProcessor_ID == 0)
        {
			setAcceptAMEX (false);
			setAcceptATM (false);
			setAcceptCheck (false);
			setAcceptCorporate (false);
			setAcceptDiners (false);
			setAcceptDirectDebit (false);
			setAcceptDirectDeposit (false);
			setAcceptDiscover (false);
			setAcceptMC (false);
			setAcceptVisa (false);
			setC_BankAccount_ID (0);
			setCommission (Env.ZERO);
			setCostPerTrx (Env.ZERO);
			setC_PaymentProcessor_ID (0);
			setHostAddress (null);
			setHostPort (0);
			setName (null);
			setPassword (null);
			setRequireVV (false);
			setUserID (null);
        } */
    }

    /** Load Constructor */
    public X_C_PaymentProcessor (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_PaymentProcessor[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Accept AMEX.
		@param AcceptAMEX 
		Accept American Express Card
	  */
	public void setAcceptAMEX (boolean AcceptAMEX)
	{
		set_Value (COLUMNNAME_AcceptAMEX, Boolean.valueOf(AcceptAMEX));
	}

	/** Get Accept AMEX.
		@return Accept American Express Card
	  */
	public boolean isAcceptAMEX () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptAMEX);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept ATM.
		@param AcceptATM 
		Accept Bank ATM Card
	  */
	public void setAcceptATM (boolean AcceptATM)
	{
		set_Value (COLUMNNAME_AcceptATM, Boolean.valueOf(AcceptATM));
	}

	/** Get Accept ATM.
		@return Accept Bank ATM Card
	  */
	public boolean isAcceptATM () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptATM);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept Electronic Check.
		@param AcceptCheck 
		Accept ECheck (Electronic Checks)
	  */
	public void setAcceptCheck (boolean AcceptCheck)
	{
		set_Value (COLUMNNAME_AcceptCheck, Boolean.valueOf(AcceptCheck));
	}

	/** Get Accept Electronic Check.
		@return Accept ECheck (Electronic Checks)
	  */
	public boolean isAcceptCheck () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptCheck);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept Corporate.
		@param AcceptCorporate 
		Accept Corporate Purchase Cards
	  */
	public void setAcceptCorporate (boolean AcceptCorporate)
	{
		set_Value (COLUMNNAME_AcceptCorporate, Boolean.valueOf(AcceptCorporate));
	}

	/** Get Accept Corporate.
		@return Accept Corporate Purchase Cards
	  */
	public boolean isAcceptCorporate () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptCorporate);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept Diners.
		@param AcceptDiners 
		Accept Diner's Club
	  */
	public void setAcceptDiners (boolean AcceptDiners)
	{
		set_Value (COLUMNNAME_AcceptDiners, Boolean.valueOf(AcceptDiners));
	}

	/** Get Accept Diners.
		@return Accept Diner's Club
	  */
	public boolean isAcceptDiners () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptDiners);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept Direct Debit.
		@param AcceptDirectDebit 
		Accept Direct Debits (vendor initiated)
	  */
	public void setAcceptDirectDebit (boolean AcceptDirectDebit)
	{
		set_Value (COLUMNNAME_AcceptDirectDebit, Boolean.valueOf(AcceptDirectDebit));
	}

	/** Get Accept Direct Debit.
		@return Accept Direct Debits (vendor initiated)
	  */
	public boolean isAcceptDirectDebit () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptDirectDebit);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept Direct Deposit.
		@param AcceptDirectDeposit 
		Accept Direct Deposit (payee initiated)
	  */
	public void setAcceptDirectDeposit (boolean AcceptDirectDeposit)
	{
		set_Value (COLUMNNAME_AcceptDirectDeposit, Boolean.valueOf(AcceptDirectDeposit));
	}

	/** Get Accept Direct Deposit.
		@return Accept Direct Deposit (payee initiated)
	  */
	public boolean isAcceptDirectDeposit () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptDirectDeposit);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept Discover.
		@param AcceptDiscover 
		Accept Discover Card
	  */
	public void setAcceptDiscover (boolean AcceptDiscover)
	{
		set_Value (COLUMNNAME_AcceptDiscover, Boolean.valueOf(AcceptDiscover));
	}

	/** Get Accept Discover.
		@return Accept Discover Card
	  */
	public boolean isAcceptDiscover () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptDiscover);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept MasterCard.
		@param AcceptMC 
		Accept Master Card
	  */
	public void setAcceptMC (boolean AcceptMC)
	{
		set_Value (COLUMNNAME_AcceptMC, Boolean.valueOf(AcceptMC));
	}

	/** Get Accept MasterCard.
		@return Accept Master Card
	  */
	public boolean isAcceptMC () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptMC);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Accept Visa.
		@param AcceptVisa 
		Accept Visa Cards
	  */
	public void setAcceptVisa (boolean AcceptVisa)
	{
		set_Value (COLUMNNAME_AcceptVisa, Boolean.valueOf(AcceptVisa));
	}

	/** Get Accept Visa.
		@return Accept Visa Cards
	  */
	public boolean isAcceptVisa () 
	{
		Object oo = get_Value(COLUMNNAME_AcceptVisa);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_AD_Sequence getAD_Sequence() throws RuntimeException
    {
		return (I_AD_Sequence)MTable.get(getCtx(), I_AD_Sequence.Table_Name)
			.getPO(getAD_Sequence_ID(), get_TrxName());	}

	/** Set Sequence.
		@param AD_Sequence_ID 
		Document Sequence
	  */
	public void setAD_Sequence_ID (int AD_Sequence_ID)
	{
		if (AD_Sequence_ID < 1) 
			set_Value (COLUMNNAME_AD_Sequence_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Sequence_ID, Integer.valueOf(AD_Sequence_ID));
	}

	/** Get Sequence.
		@return Document Sequence
	  */
	public int getAD_Sequence_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Sequence_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_BankAccount getC_BankAccount() throws RuntimeException
    {
		return (I_C_BankAccount)MTable.get(getCtx(), I_C_BankAccount.Table_Name)
			.getPO(getC_BankAccount_ID(), get_TrxName());	}

	/** Set Bank Account.
		@param C_BankAccount_ID 
		Account at the Bank
	  */
	public void setC_BankAccount_ID (int C_BankAccount_ID)
	{
		if (C_BankAccount_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_BankAccount_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_BankAccount_ID, Integer.valueOf(C_BankAccount_ID));
	}

	/** Get Bank Account.
		@return Account at the Bank
	  */
	public int getC_BankAccount_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankAccount_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Bank getC_Bank() throws RuntimeException
    {
		return (I_C_Bank)MTable.get(getCtx(), I_C_Bank.Table_Name)
			.getPO(getC_Bank_ID(), get_TrxName());	}

	/** Set Bank.
		@param C_Bank_ID 
		Bank
	  */
	public void setC_Bank_ID (int C_Bank_ID)
	{
		if (C_Bank_ID < 1) 
			set_Value (COLUMNNAME_C_Bank_ID, null);
		else 
			set_Value (COLUMNNAME_C_Bank_ID, Integer.valueOf(C_Bank_ID));
	}

	/** Get Bank.
		@return Bank
	  */
	public int getC_Bank_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Bank_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Currency getC_Currency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getC_Currency_ID(), get_TrxName());	}

	/** Set Currency.
		@param C_Currency_ID 
		The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1) 
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else 
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Commission %.
		@param Commission 
		Commission stated as a percentage
	  */
	public void setCommission (BigDecimal Commission)
	{
		set_Value (COLUMNNAME_Commission, Commission);
	}

	/** Get Commission %.
		@return Commission stated as a percentage
	  */
	public BigDecimal getCommission () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Commission);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Cost per transaction.
		@param CostPerTrx 
		Fixed cost per transaction
	  */
	public void setCostPerTrx (BigDecimal CostPerTrx)
	{
		set_Value (COLUMNNAME_CostPerTrx, CostPerTrx);
	}

	/** Get Cost per transaction.
		@return Fixed cost per transaction
	  */
	public BigDecimal getCostPerTrx () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_CostPerTrx);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Payment Processor.
		@param C_PaymentProcessor_ID 
		Payment processor for electronic payments
	  */
	public void setC_PaymentProcessor_ID (int C_PaymentProcessor_ID)
	{
		if (C_PaymentProcessor_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_PaymentProcessor_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_PaymentProcessor_ID, Integer.valueOf(C_PaymentProcessor_ID));
	}

	/** Get Payment Processor.
		@return Payment processor for electronic payments
	  */
	public int getC_PaymentProcessor_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PaymentProcessor_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_C_Charge getFeeCharge() throws RuntimeException
    {
		return (I_C_Charge)MTable.get(getCtx(), I_C_Charge.Table_Name)
			.getPO(getFeeCharge_ID(), get_TrxName());	}

	/** Set Charge for Fee.
		@param FeeCharge_ID 
		Charge for Fee
	  */
	public void setFeeCharge_ID (int FeeCharge_ID)
	{
		if (FeeCharge_ID < 1) 
			set_Value (COLUMNNAME_FeeCharge_ID, null);
		else 
			set_Value (COLUMNNAME_FeeCharge_ID, Integer.valueOf(FeeCharge_ID));
	}

	/** Get Charge for Fee.
		@return Charge for Fee
	  */
	public int getFeeCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FeeCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Currency getFeeCurrency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getFeeCurrency_ID(), get_TrxName());	}

	/** Set Currency for Fee.
		@param FeeCurrency_ID 
		Currency for Fee
	  */
	public void setFeeCurrency_ID (int FeeCurrency_ID)
	{
		if (FeeCurrency_ID < 1) 
			set_Value (COLUMNNAME_FeeCurrency_ID, null);
		else 
			set_Value (COLUMNNAME_FeeCurrency_ID, Integer.valueOf(FeeCurrency_ID));
	}

	/** Get Currency for Fee.
		@return Currency for Fee
	  */
	public int getFeeCurrency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FeeCurrency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Host Address.
		@param HostAddress 
		Host Address URL or DNS
	  */
	public void setHostAddress (String HostAddress)
	{
		set_Value (COLUMNNAME_HostAddress, HostAddress);
	}

	/** Get Host Address.
		@return Host Address URL or DNS
	  */
	public String getHostAddress () 
	{
		return (String)get_Value(COLUMNNAME_HostAddress);
	}

	/** Set Host port.
		@param HostPort 
		Host Communication Port
	  */
	public void setHostPort (int HostPort)
	{
		set_Value (COLUMNNAME_HostPort, Integer.valueOf(HostPort));
	}

	/** Get Host port.
		@return Host Communication Port
	  */
	public int getHostPort () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HostPort);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Minimum Amt.
		@param MinimumAmt 
		Minimum Amount in Document Currency
	  */
	public void setMinimumAmt (BigDecimal MinimumAmt)
	{
		set_Value (COLUMNNAME_MinimumAmt, MinimumAmt);
	}

	/** Get Minimum Amt.
		@return Minimum Amount in Document Currency
	  */
	public BigDecimal getMinimumAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MinimumAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set Partner ID.
		@param PartnerID 
		Partner ID or Account for the Payment Processor
	  */
	public void setPartnerID (String PartnerID)
	{
		set_Value (COLUMNNAME_PartnerID, PartnerID);
	}

	/** Get Partner ID.
		@return Partner ID or Account for the Payment Processor
	  */
	public String getPartnerID () 
	{
		return (String)get_Value(COLUMNNAME_PartnerID);
	}

	/** Set Password.
		@param Password 
		Password of any length (case sensitive)
	  */
	public void setPassword (String Password)
	{
		set_Value (COLUMNNAME_Password, Password);
	}

	/** Get Password.
		@return Password of any length (case sensitive)
	  */
	public String getPassword () 
	{
		return (String)get_Value(COLUMNNAME_Password);
	}

	public I_C_BPartner getPaymentProcessorVendor() throws RuntimeException
    {
		return (I_C_BPartner)MTable.get(getCtx(), I_C_BPartner.Table_Name)
			.getPO(getPaymentProcessorVendor_ID(), get_TrxName());	}

	/** Set Processor Vendor.
		@param PaymentProcessorVendor_ID 
		Processor Vendor
	  */
	public void setPaymentProcessorVendor_ID (int PaymentProcessorVendor_ID)
	{
		if (PaymentProcessorVendor_ID < 1) 
			set_Value (COLUMNNAME_PaymentProcessorVendor_ID, null);
		else 
			set_Value (COLUMNNAME_PaymentProcessorVendor_ID, Integer.valueOf(PaymentProcessorVendor_ID));
	}

	/** Get Processor Vendor.
		@return Processor Vendor
	  */
	public int getPaymentProcessorVendor_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PaymentProcessorVendor_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Payment Processor Class.
		@param PayProcessorClass 
		Payment Processor Java Class
	  */
	public void setPayProcessorClass (String PayProcessorClass)
	{
		set_Value (COLUMNNAME_PayProcessorClass, PayProcessorClass);
	}

	/** Get Payment Processor Class.
		@return Payment Processor Java Class
	  */
	public String getPayProcessorClass () 
	{
		return (String)get_Value(COLUMNNAME_PayProcessorClass);
	}

	/** Set Proxy address.
		@param ProxyAddress 
		 Address of your proxy server
	  */
	public void setProxyAddress (String ProxyAddress)
	{
		set_Value (COLUMNNAME_ProxyAddress, ProxyAddress);
	}

	/** Get Proxy address.
		@return  Address of your proxy server
	  */
	public String getProxyAddress () 
	{
		return (String)get_Value(COLUMNNAME_ProxyAddress);
	}

	/** Set Proxy logon.
		@param ProxyLogon 
		Logon of your proxy server
	  */
	public void setProxyLogon (String ProxyLogon)
	{
		set_Value (COLUMNNAME_ProxyLogon, ProxyLogon);
	}

	/** Get Proxy logon.
		@return Logon of your proxy server
	  */
	public String getProxyLogon () 
	{
		return (String)get_Value(COLUMNNAME_ProxyLogon);
	}

	/** Set Proxy password.
		@param ProxyPassword 
		Password of your proxy server
	  */
	public void setProxyPassword (String ProxyPassword)
	{
		set_Value (COLUMNNAME_ProxyPassword, ProxyPassword);
	}

	/** Get Proxy password.
		@return Password of your proxy server
	  */
	public String getProxyPassword () 
	{
		return (String)get_Value(COLUMNNAME_ProxyPassword);
	}

	/** Set Proxy port.
		@param ProxyPort 
		Port of your proxy server
	  */
	public void setProxyPort (int ProxyPort)
	{
		set_Value (COLUMNNAME_ProxyPort, Integer.valueOf(ProxyPort));
	}

	/** Get Proxy port.
		@return Port of your proxy server
	  */
	public int getProxyPort () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ProxyPort);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPurchaseInvoiceDocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPurchaseInvoiceDocType_ID(), get_TrxName());	}

	/** Set Purchase Invoice Document Type.
		@param PurchaseInvoiceDocType_ID Purchase Invoice Document Type	  */
	public void setPurchaseInvoiceDocType_ID (int PurchaseInvoiceDocType_ID)
	{
		if (PurchaseInvoiceDocType_ID < 1) 
			set_Value (COLUMNNAME_PurchaseInvoiceDocType_ID, null);
		else 
			set_Value (COLUMNNAME_PurchaseInvoiceDocType_ID, Integer.valueOf(PurchaseInvoiceDocType_ID));
	}

	/** Get Purchase Invoice Document Type.
		@return Purchase Invoice Document Type	  */
	public int getPurchaseInvoiceDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PurchaseInvoiceDocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Require CreditCard Verification Code.
		@param RequireVV 
		Require 3/4 digit Credit Verification Code
	  */
	public void setRequireVV (boolean RequireVV)
	{
		set_Value (COLUMNNAME_RequireVV, Boolean.valueOf(RequireVV));
	}

	/** Get Require CreditCard Verification Code.
		@return Require 3/4 digit Credit Verification Code
	  */
	public boolean isRequireVV () 
	{
		Object oo = get_Value(COLUMNNAME_RequireVV);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_C_DocType getSalesInvoiceDocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getSalesInvoiceDocType_ID(), get_TrxName());	}

	/** Set Sales Invoice Document Type.
		@param SalesInvoiceDocType_ID Sales Invoice Document Type	  */
	public void setSalesInvoiceDocType_ID (int SalesInvoiceDocType_ID)
	{
		if (SalesInvoiceDocType_ID < 1) 
			set_Value (COLUMNNAME_SalesInvoiceDocType_ID, null);
		else 
			set_Value (COLUMNNAME_SalesInvoiceDocType_ID, Integer.valueOf(SalesInvoiceDocType_ID));
	}

	/** Get Sales Invoice Document Type.
		@return Sales Invoice Document Type	  */
	public int getSalesInvoiceDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SalesInvoiceDocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set User ID.
		@param UserID 
		User ID or account number
	  */
	public void setUserID (String UserID)
	{
		set_Value (COLUMNNAME_UserID, UserID);
	}

	/** Get User ID.
		@return User ID or account number
	  */
	public String getUserID () 
	{
		return (String)get_Value(COLUMNNAME_UserID);
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

	/** Set Vendor ID.
		@param VendorID 
		Vendor ID for the Payment Processor
	  */
	public void setVendorID (String VendorID)
	{
		set_Value (COLUMNNAME_VendorID, VendorID);
	}

	/** Get Vendor ID.
		@return Vendor ID for the Payment Processor
	  */
	public String getVendorID () 
	{
		return (String)get_Value(COLUMNNAME_VendorID);
	}

	public I_C_Charge getWithholdingCharge() throws RuntimeException
    {
		return (I_C_Charge)MTable.get(getCtx(), I_C_Charge.Table_Name)
			.getPO(getWithholdingCharge_ID(), get_TrxName());	}

	/** Set Withholding Charge.
		@param WithholdingCharge_ID Withholding Charge	  */
	public void setWithholdingCharge_ID (int WithholdingCharge_ID)
	{
		if (WithholdingCharge_ID < 1) 
			set_Value (COLUMNNAME_WithholdingCharge_ID, null);
		else 
			set_Value (COLUMNNAME_WithholdingCharge_ID, Integer.valueOf(WithholdingCharge_ID));
	}

	/** Get Withholding Charge.
		@return Withholding Charge	  */
	public int getWithholdingCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WithholdingCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getWithholdingDocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getWithholdingDocType_ID(), get_TrxName());	}

	/** Set Withholding Document Type.
		@param WithholdingDocType_ID Withholding Document Type	  */
	public void setWithholdingDocType_ID (int WithholdingDocType_ID)
	{
		if (WithholdingDocType_ID < 1) 
			set_Value (COLUMNNAME_WithholdingDocType_ID, null);
		else 
			set_Value (COLUMNNAME_WithholdingDocType_ID, Integer.valueOf(WithholdingDocType_ID));
	}

	/** Get Withholding Document Type.
		@return Withholding Document Type	  */
	public int getWithholdingDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WithholdingDocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}