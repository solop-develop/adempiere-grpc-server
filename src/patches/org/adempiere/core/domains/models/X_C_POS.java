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

/** Generated Model for C_POS
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_POS extends PO implements I_C_POS, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250811L;

    /** Standard Constructor */
    public X_C_POS(Properties ctx, int C_POS_ID, String trxName)
    {
      super (ctx, C_POS_ID, trxName);
      /** if (C_POS_ID == 0)
        {
			setC_CashBook_ID (0);
			setC_POS_ID (0);
			setIsModifyPrice (false);
// N
			setM_PriceList_ID (0);
			setM_Warehouse_ID (0);
			setName (null);
			setSalesRep_ID (0);
        } */
    }

    /** Load Constructor */
    public X_C_POS(Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 2 - Client 
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
      StringBuffer sb = new StringBuffer ("X_C_POS[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Auto Logout Delay.
		@param AutoLogoutDelay 
		Automatically logout if terminal inactive for this period
	  */
	public void setAutoLogoutDelay (int AutoLogoutDelay)
	{
		set_Value (COLUMNNAME_AutoLogoutDelay, Integer.valueOf(AutoLogoutDelay));
	}

	/** Get Auto Logout Delay.
		@return Automatically logout if terminal inactive for this period
	  */
	public int getAutoLogoutDelay () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AutoLogoutDelay);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set CashDrawer.
		@param CashDrawer CashDrawer	  */
	public void setCashDrawer (String CashDrawer)
	{
		set_Value (COLUMNNAME_CashDrawer, CashDrawer);
	}

	/** Get CashDrawer.
		@return CashDrawer	  */
	public String getCashDrawer () 
	{
		return (String)get_Value(COLUMNNAME_CashDrawer);
	}

	public I_C_BankAccount getCashTransferBankAccount() throws RuntimeException
    {
		return (I_C_BankAccount)MTable.get(getCtx(), I_C_BankAccount.Table_Name)
			.getPO(getCashTransferBankAccount_ID(), get_TrxName());	}

	/** Set Transfer Cash trx to.
		@param CashTransferBankAccount_ID 
		Bank Account on which to transfer all Cash transactions
	  */
	public void setCashTransferBankAccount_ID (int CashTransferBankAccount_ID)
	{
		if (CashTransferBankAccount_ID < 1) 
			set_Value (COLUMNNAME_CashTransferBankAccount_ID, null);
		else 
			set_Value (COLUMNNAME_CashTransferBankAccount_ID, Integer.valueOf(CashTransferBankAccount_ID));
	}

	/** Get Transfer Cash trx to.
		@return Bank Account on which to transfer all Cash transactions
	  */
	public int getCashTransferBankAccount_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_CashTransferBankAccount_ID);
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
			set_Value (COLUMNNAME_C_BankAccount_ID, null);
		else 
			set_Value (COLUMNNAME_C_BankAccount_ID, Integer.valueOf(C_BankAccount_ID));
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

	public I_C_BPartner getC_BPartnerCashTrx() throws RuntimeException
    {
		return (I_C_BPartner)MTable.get(getCtx(), I_C_BPartner.Table_Name)
			.getPO(getC_BPartnerCashTrx_ID(), get_TrxName());	}

	/** Set Template B.Partner.
		@param C_BPartnerCashTrx_ID 
		Business Partner used for creating new Business Partners on the fly
	  */
	public void setC_BPartnerCashTrx_ID (int C_BPartnerCashTrx_ID)
	{
		if (C_BPartnerCashTrx_ID < 1) 
			set_Value (COLUMNNAME_C_BPartnerCashTrx_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartnerCashTrx_ID, Integer.valueOf(C_BPartnerCashTrx_ID));
	}

	/** Get Template B.Partner.
		@return Business Partner used for creating new Business Partners on the fly
	  */
	public int getC_BPartnerCashTrx_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartnerCashTrx_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_CashBook getC_CashBook() throws RuntimeException
    {
		return (I_C_CashBook)MTable.get(getCtx(), I_C_CashBook.Table_Name)
			.getPO(getC_CashBook_ID(), get_TrxName());	}

	/** Set Cash Book.
		@param C_CashBook_ID 
		Cash Book for recording petty cash transactions
	  */
	public void setC_CashBook_ID (int C_CashBook_ID)
	{
		if (C_CashBook_ID < 1) 
			set_Value (COLUMNNAME_C_CashBook_ID, null);
		else 
			set_Value (COLUMNNAME_C_CashBook_ID, Integer.valueOf(C_CashBook_ID));
	}

	/** Get Cash Book.
		@return Cash Book for recording petty cash transactions
	  */
	public int getC_CashBook_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_CashBook_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_ConversionType getC_ConversionType() throws RuntimeException
    {
		return (I_C_ConversionType)MTable.get(getCtx(), I_C_ConversionType.Table_Name)
			.getPO(getC_ConversionType_ID(), get_TrxName());	}

	/** Set Currency Type.
		@param C_ConversionType_ID 
		Currency Conversion Rate Type
	  */
	public void setC_ConversionType_ID (int C_ConversionType_ID)
	{
		if (C_ConversionType_ID < 1) 
			set_Value (COLUMNNAME_C_ConversionType_ID, null);
		else 
			set_Value (COLUMNNAME_C_ConversionType_ID, Integer.valueOf(C_ConversionType_ID));
	}

	/** Get Currency Type.
		@return Currency Conversion Rate Type
	  */
	public int getC_ConversionType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ConversionType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getC_DocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getC_DocType_ID(), get_TrxName());	}

	/** Set Document Type.
		@param C_DocType_ID 
		Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0) 
			set_Value (COLUMNNAME_C_DocType_ID, null);
		else 
			set_Value (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set POS Terminal.
		@param C_POS_ID 
		Point of Sales Terminal
	  */
	public void setC_POS_ID (int C_POS_ID)
	{
		if (C_POS_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_POS_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_POS_ID, Integer.valueOf(C_POS_ID));
	}

	/** Get POS Terminal.
		@return Point of Sales Terminal
	  */
	public int getC_POS_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_POS_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_POSKeyLayout getC_POSKeyLayout() throws RuntimeException
    {
		return (I_C_POSKeyLayout)MTable.get(getCtx(), I_C_POSKeyLayout.Table_Name)
			.getPO(getC_POSKeyLayout_ID(), get_TrxName());	}

	/** Set POS Key Layout.
		@param C_POSKeyLayout_ID 
		POS Function Key Layout
	  */
	public void setC_POSKeyLayout_ID (int C_POSKeyLayout_ID)
	{
		if (C_POSKeyLayout_ID < 1) 
			set_Value (COLUMNNAME_C_POSKeyLayout_ID, null);
		else 
			set_Value (COLUMNNAME_C_POSKeyLayout_ID, Integer.valueOf(C_POSKeyLayout_ID));
	}

	/** Get POS Key Layout.
		@return POS Function Key Layout
	  */
	public int getC_POSKeyLayout_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_POSKeyLayout_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Campaign getDefaultCampaign() throws RuntimeException
    {
		return (I_C_Campaign)MTable.get(getCtx(), I_C_Campaign.Table_Name)
			.getPO(getDefaultCampaign_ID(), get_TrxName());	}

	/** Set Default Campaign.
		@param DefaultCampaign_ID 
		Marketing Campaign
	  */
	public void setDefaultCampaign_ID (int DefaultCampaign_ID)
	{
		if (DefaultCampaign_ID < 1) 
			set_Value (COLUMNNAME_DefaultCampaign_ID, null);
		else 
			set_Value (COLUMNNAME_DefaultCampaign_ID, Integer.valueOf(DefaultCampaign_ID));
	}

	/** Get Default Campaign.
		@return Marketing Campaign
	  */
	public int getDefaultCampaign_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DefaultCampaign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Charge getDefaultDiscountCharge() throws RuntimeException
    {
		return (I_C_Charge)MTable.get(getCtx(), I_C_Charge.Table_Name)
			.getPO(getDefaultDiscountCharge_ID(), get_TrxName());	}

	/** Set Default Discount Charge.
		@param DefaultDiscountCharge_ID 
		Default Discount Charge for POS
	  */
	public void setDefaultDiscountCharge_ID (int DefaultDiscountCharge_ID)
	{
		if (DefaultDiscountCharge_ID < 1) 
			set_Value (COLUMNNAME_DefaultDiscountCharge_ID, null);
		else 
			set_Value (COLUMNNAME_DefaultDiscountCharge_ID, Integer.valueOf(DefaultDiscountCharge_ID));
	}

	/** Get Default Discount Charge.
		@return Default Discount Charge for POS
	  */
	public int getDefaultDiscountCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DefaultDiscountCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** DeliveryRule AD_Reference_ID=151 */
	public static final int DELIVERYRULE_AD_Reference_ID=151;
	/** After Receipt = R */
	public static final String DELIVERYRULE_AfterReceipt = "R";
	/** Availability = A */
	public static final String DELIVERYRULE_Availability = "A";
	/** Complete Line = L */
	public static final String DELIVERYRULE_CompleteLine = "L";
	/** Complete Order = O */
	public static final String DELIVERYRULE_CompleteOrder = "O";
	/** Force = F */
	public static final String DELIVERYRULE_Force = "F";
	/** Manual = M */
	public static final String DELIVERYRULE_Manual = "M";
	/** Set Delivery Rule.
		@param DeliveryRule 
		Defines the timing of Delivery
	  */
	public void setDeliveryRule (String DeliveryRule)
	{

		set_Value (COLUMNNAME_DeliveryRule, DeliveryRule);
	}

	/** Get Delivery Rule.
		@return Defines the timing of Delivery
	  */
	public String getDeliveryRule () 
	{
		return (String)get_Value(COLUMNNAME_DeliveryRule);
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

	public I_C_Currency getDisplayCurrency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getDisplayCurrency_ID(), get_TrxName());	}

	/** Set Display Currency.
		@param DisplayCurrency_ID 
		Display Currency for POS and Price Checking
	  */
	public void setDisplayCurrency_ID (int DisplayCurrency_ID)
	{
		if (DisplayCurrency_ID < 1) 
			set_Value (COLUMNNAME_DisplayCurrency_ID, null);
		else 
			set_Value (COLUMNNAME_DisplayCurrency_ID, Integer.valueOf(DisplayCurrency_ID));
	}

	/** Get Display Currency.
		@return Display Currency for POS and Price Checking
	  */
	public int getDisplayCurrency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DisplayCurrency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Charge getECA14_DefaultGiftCardCharge() throws RuntimeException
    {
		return (I_C_Charge)MTable.get(getCtx(), I_C_Charge.Table_Name)
			.getPO(getECA14_DefaultGiftCardCharge_ID(), get_TrxName());	}

	/** Set Default Gift Card Charge.
		@param ECA14_DefaultGiftCardCharge_ID 
		Default Gift Card Charge for POS
	  */
	public void setECA14_DefaultGiftCardCharge_ID (int ECA14_DefaultGiftCardCharge_ID)
	{
		if (ECA14_DefaultGiftCardCharge_ID < 1) 
			set_Value (COLUMNNAME_ECA14_DefaultGiftCardCharge_ID, null);
		else 
			set_Value (COLUMNNAME_ECA14_DefaultGiftCardCharge_ID, Integer.valueOf(ECA14_DefaultGiftCardCharge_ID));
	}

	/** Get Default Gift Card Charge.
		@return Default Gift Card Charge for POS
	  */
	public int getECA14_DefaultGiftCardCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ECA14_DefaultGiftCardCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Electronic Scales.
		@param ElectronicScales 
		Allows to define path for Device Electronic Scales e.g. /dev/ttyS0/
	  */
	public void setElectronicScales (String ElectronicScales)
	{
		set_Value (COLUMNNAME_ElectronicScales, ElectronicScales);
	}

	/** Get Electronic Scales.
		@return Allows to define path for Device Electronic Scales e.g. /dev/ttyS0/
	  */
	public String getElectronicScales () 
	{
		return (String)get_Value(COLUMNNAME_ElectronicScales);
	}

	/** Set Comment/Help.
		@param Help 
		Comment or Hint
	  */
	public void setHelp (String Help)
	{
		set_Value (COLUMNNAME_Help, Help);
	}

	/** Get Comment/Help.
		@return Comment or Hint
	  */
	public String getHelp () 
	{
		return (String)get_Value(COLUMNNAME_Help);
	}

	/** InvoiceRule AD_Reference_ID=150 */
	public static final int INVOICERULE_AD_Reference_ID=150;
	/** After Order delivered = O */
	public static final String INVOICERULE_AfterOrderDelivered = "O";
	/** After Delivery = D */
	public static final String INVOICERULE_AfterDelivery = "D";
	/** Customer Schedule after Delivery = S */
	public static final String INVOICERULE_CustomerScheduleAfterDelivery = "S";
	/** Immediate = I */
	public static final String INVOICERULE_Immediate = "I";
	/** Set Invoice Rule.
		@param InvoiceRule 
		Frequency and method of invoicing 
	  */
	public void setInvoiceRule (String InvoiceRule)
	{

		set_Value (COLUMNNAME_InvoiceRule, InvoiceRule);
	}

	/** Get Invoice Rule.
		@return Frequency and method of invoicing 
	  */
	public String getInvoiceRule () 
	{
		return (String)get_Value(COLUMNNAME_InvoiceRule);
	}

	/** Set Allows Allocate Seller.
		@param IsAllowsAllocateSeller 
		Allows Allocate Seller for this POS Terminal
	  */
	public void setIsAllowsAllocateSeller (boolean IsAllowsAllocateSeller)
	{
		set_Value (COLUMNNAME_IsAllowsAllocateSeller, Boolean.valueOf(IsAllowsAllocateSeller));
	}

	/** Get Allows Allocate Seller.
		@return Allows Allocate Seller for this POS Terminal
	  */
	public boolean isAllowsAllocateSeller () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsAllocateSeller);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Apply Discount (By Document).
		@param IsAllowsApplyDiscount 
		Allows Apply Discount for this POS Terminal
	  */
	public void setIsAllowsApplyDiscount (boolean IsAllowsApplyDiscount)
	{
		set_Value (COLUMNNAME_IsAllowsApplyDiscount, Boolean.valueOf(IsAllowsApplyDiscount));
	}

	/** Get Allows Apply Discount (By Document).
		@return Allows Apply Discount for this POS Terminal
	  */
	public boolean isAllowsApplyDiscount () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsApplyDiscount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Cash Closing.
		@param IsAllowsCashClosing 
		Allows Cash Closing
	  */
	public void setIsAllowsCashClosing (boolean IsAllowsCashClosing)
	{
		set_Value (COLUMNNAME_IsAllowsCashClosing, Boolean.valueOf(IsAllowsCashClosing));
	}

	/** Get Allows Cash Closing.
		@return Allows Cash Closing
	  */
	public boolean isAllowsCashClosing () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCashClosing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Cash Opening.
		@param IsAllowsCashOpening 
		Allows Cash Opening
	  */
	public void setIsAllowsCashOpening (boolean IsAllowsCashOpening)
	{
		set_Value (COLUMNNAME_IsAllowsCashOpening, Boolean.valueOf(IsAllowsCashOpening));
	}

	/** Get Allows Cash Opening.
		@return Allows Cash Opening
	  */
	public boolean isAllowsCashOpening () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCashOpening);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Cash Withdrawal.
		@param IsAllowsCashWithdrawal 
		Allows Cash Withdrawal
	  */
	public void setIsAllowsCashWithdrawal (boolean IsAllowsCashWithdrawal)
	{
		set_Value (COLUMNNAME_IsAllowsCashWithdrawal, Boolean.valueOf(IsAllowsCashWithdrawal));
	}

	/** Get Allows Cash Withdrawal.
		@return Allows Cash Withdrawal
	  */
	public boolean isAllowsCashWithdrawal () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCashWithdrawal);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Collect Order.
		@param IsAllowsCollectOrder 
		Allows collect a Sales Order
	  */
	public void setIsAllowsCollectOrder (boolean IsAllowsCollectOrder)
	{
		set_Value (COLUMNNAME_IsAllowsCollectOrder, Boolean.valueOf(IsAllowsCollectOrder));
	}

	/** Get Allows Collect Order.
		@return Allows collect a Sales Order
	  */
	public boolean isAllowsCollectOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCollectOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Concurrent Use.
		@param IsAllowsConcurrentUse 
		Allows Concurrent Use for this terminal, both sellers can make sales on one time
	  */
	public void setIsAllowsConcurrentUse (boolean IsAllowsConcurrentUse)
	{
		set_Value (COLUMNNAME_IsAllowsConcurrentUse, Boolean.valueOf(IsAllowsConcurrentUse));
	}

	/** Get Allows Concurrent Use.
		@return Allows Concurrent Use for this terminal, both sellers can make sales on one time
	  */
	public boolean isAllowsConcurrentUse () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsConcurrentUse);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Confirm Shipment.
		@param IsAllowsConfirmShipment 
		Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipment (boolean IsAllowsConfirmShipment)
	{
		set_Value (COLUMNNAME_IsAllowsConfirmShipment, Boolean.valueOf(IsAllowsConfirmShipment));
	}

	/** Get Allows Confirm Shipment.
		@return Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipment () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsConfirmShipment);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Confirm Shipment by Order.
		@param IsAllowsConfirmShipmentByOrder 
		Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipmentByOrder (boolean IsAllowsConfirmShipmentByOrder)
	{
		set_Value (COLUMNNAME_IsAllowsConfirmShipmentByOrder, Boolean.valueOf(IsAllowsConfirmShipmentByOrder));
	}

	/** Get Allows Confirm Shipment by Order.
		@return Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipmentByOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsConfirmShipmentByOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Create Customer.
		@param IsAllowsCreateCustomer 
		Allows create a Customer from POS
	  */
	public void setIsAllowsCreateCustomer (boolean IsAllowsCreateCustomer)
	{
		set_Value (COLUMNNAME_IsAllowsCreateCustomer, Boolean.valueOf(IsAllowsCreateCustomer));
	}

	/** Get Allows Create Customer.
		@return Allows create a Customer from POS
	  */
	public boolean isAllowsCreateCustomer () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCreateCustomer);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Create Order.
		@param IsAllowsCreateOrder 
		Allows create a Sales Order
	  */
	public void setIsAllowsCreateOrder (boolean IsAllowsCreateOrder)
	{
		set_Value (COLUMNNAME_IsAllowsCreateOrder, Boolean.valueOf(IsAllowsCreateOrder));
	}

	/** Get Allows Create Order.
		@return Allows create a Sales Order
	  */
	public boolean isAllowsCreateOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCreateOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Gift Card.
		@param IsAllowsGiftCard 
		Allows Gift Card
	  */
	public void setIsAllowsGiftCard (boolean IsAllowsGiftCard)
	{
		set_Value (COLUMNNAME_IsAllowsGiftCard, Boolean.valueOf(IsAllowsGiftCard));
	}

	/** Get Allows Gift Card.
		@return Allows Gift Card
	  */
	public boolean isAllowsGiftCard () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsGiftCard);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Modify Customer.
		@param IsAllowsModifyCustomer 
		Allows Modify Customer from POS
	  */
	public void setIsAllowsModifyCustomer (boolean IsAllowsModifyCustomer)
	{
		set_Value (COLUMNNAME_IsAllowsModifyCustomer, Boolean.valueOf(IsAllowsModifyCustomer));
	}

	/** Get Allows Modify Customer.
		@return Allows Modify Customer from POS
	  */
	public boolean isAllowsModifyCustomer () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsModifyCustomer);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Modify Quantity.
		@param IsAllowsModifyQuantity 
		Allows modifying the quantity
	  */
	public void setIsAllowsModifyQuantity (boolean IsAllowsModifyQuantity)
	{
		set_Value (COLUMNNAME_IsAllowsModifyQuantity, Boolean.valueOf(IsAllowsModifyQuantity));
	}

	/** Get Allows Modify Quantity.
		@return Allows modifying the quantity
	  */
	public boolean isAllowsModifyQuantity () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsModifyQuantity);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Print Preview.
		@param IsAllowsPreviewDocument 
		Allows print document with preview from POS
	  */
	public void setIsAllowsPreviewDocument (boolean IsAllowsPreviewDocument)
	{
		set_Value (COLUMNNAME_IsAllowsPreviewDocument, Boolean.valueOf(IsAllowsPreviewDocument));
	}

	/** Get Allows Print Preview.
		@return Allows print document with preview from POS
	  */
	public boolean isAllowsPreviewDocument () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsPreviewDocument);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Print Document.
		@param IsAllowsPrintDocument 
		Allows print document from POS
	  */
	public void setIsAllowsPrintDocument (boolean IsAllowsPrintDocument)
	{
		set_Value (COLUMNNAME_IsAllowsPrintDocument, Boolean.valueOf(IsAllowsPrintDocument));
	}

	/** Get Allows Print Document.
		@return Allows print document from POS
	  */
	public boolean isAllowsPrintDocument () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsPrintDocument);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Return Order.
		@param IsAllowsReturnOrder 
		Allows return a Sales Order
	  */
	public void setIsAllowsReturnOrder (boolean IsAllowsReturnOrder)
	{
		set_Value (COLUMNNAME_IsAllowsReturnOrder, Boolean.valueOf(IsAllowsReturnOrder));
	}

	/** Get Allows Return Order.
		@return Allows return a Sales Order
	  */
	public boolean isAllowsReturnOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsReturnOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Confidential Info.
		@param IsConfidentialInfo 
		Can enter confidential information
	  */
	public void setIsConfidentialInfo (boolean IsConfidentialInfo)
	{
		set_Value (COLUMNNAME_IsConfidentialInfo, Boolean.valueOf(IsConfidentialInfo));
	}

	/** Get Confidential Info.
		@return Can enter confidential information
	  */
	public boolean isConfidentialInfo () 
	{
		Object oo = get_Value(COLUMNNAME_IsConfidentialInfo);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Confirm Only Complete Shipment.
		@param IsConfirmCompleteShipment 
		Confirm Only when a Shipment is completely
	  */
	public void setIsConfirmCompleteShipment (boolean IsConfirmCompleteShipment)
	{
		set_Value (COLUMNNAME_IsConfirmCompleteShipment, Boolean.valueOf(IsConfirmCompleteShipment));
	}

	/** Get Confirm Only Complete Shipment.
		@return Confirm Only when a Shipment is completely
	  */
	public boolean isConfirmCompleteShipment () 
	{
		Object oo = get_Value(COLUMNNAME_IsConfirmCompleteShipment);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Direct print.
		@param IsDirectPrint 
		Print without dialog
	  */
	public void setIsDirectPrint (boolean IsDirectPrint)
	{
		set_Value (COLUMNNAME_IsDirectPrint, Boolean.valueOf(IsDirectPrint));
	}

	/** Get Direct print.
		@return Print without dialog
	  */
	public boolean isDirectPrint () 
	{
		Object oo = get_Value(COLUMNNAME_IsDirectPrint);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Display Discount.
		@param IsDisplayDiscount 
		Display Discount on POS window
	  */
	public void setIsDisplayDiscount (boolean IsDisplayDiscount)
	{
		set_Value (COLUMNNAME_IsDisplayDiscount, Boolean.valueOf(IsDisplayDiscount));
	}

	/** Get Display Discount.
		@return Display Discount on POS window
	  */
	public boolean isDisplayDiscount () 
	{
		Object oo = get_Value(COLUMNNAME_IsDisplayDiscount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Display Tax Amount.
		@param IsDisplayTaxAmount 
		Display Tax Amount on POS window
	  */
	public void setIsDisplayTaxAmount (boolean IsDisplayTaxAmount)
	{
		set_Value (COLUMNNAME_IsDisplayTaxAmount, Boolean.valueOf(IsDisplayTaxAmount));
	}

	/** Get Display Tax Amount.
		@return Display Tax Amount on POS window
	  */
	public boolean isDisplayTaxAmount () 
	{
		Object oo = get_Value(COLUMNNAME_IsDisplayTaxAmount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Enable POS Product Lookup.
		@param IsEnableProductLookup 
		Allows product lookup in order to show search key , name , quantity available , standard price and list price for selecting a product
	  */
	public void setIsEnableProductLookup (boolean IsEnableProductLookup)
	{
		set_Value (COLUMNNAME_IsEnableProductLookup, Boolean.valueOf(IsEnableProductLookup));
	}

	/** Get Enable POS Product Lookup.
		@return Allows product lookup in order to show search key , name , quantity available , standard price and list price for selecting a product
	  */
	public boolean isEnableProductLookup () 
	{
		Object oo = get_Value(COLUMNNAME_IsEnableProductLookup);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Validate Online Closing.
	 @param IsValidateOnlineClosing Validate Online Closing	  */
	public void setIsValidateOnlineClosing (boolean IsValidateOnlineClosing)
	{
		set_Value (COLUMNNAME_IsValidateOnlineClosing, Boolean.valueOf(IsValidateOnlineClosing));
	}

	/** Get Validate Online Closing.
	 @return Validate Online Closing	  */
	public boolean isValidateOnlineClosing ()
	{
		Object oo = get_Value(COLUMNNAME_IsValidateOnlineClosing);
		if (oo != null)
		{
			if (oo instanceof Boolean)
				return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Keep Price from Customer.
		@param IsKeepPriceFromCustomer 
		Keep Price from Customer when ia create a sales order from POS
	  */
	public void setIsKeepPriceFromCustomer (boolean IsKeepPriceFromCustomer)
	{
		set_Value (COLUMNNAME_IsKeepPriceFromCustomer, Boolean.valueOf(IsKeepPriceFromCustomer));
	}

	/** Get Keep Price from Customer.
		@return Keep Price from Customer when ia create a sales order from POS
	  */
	public boolean isKeepPriceFromCustomer () 
	{
		Object oo = get_Value(COLUMNNAME_IsKeepPriceFromCustomer);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Modify Price.
		@param IsModifyPrice 
		Allow modifying the price
	  */
	public void setIsModifyPrice (boolean IsModifyPrice)
	{
		set_Value (COLUMNNAME_IsModifyPrice, Boolean.valueOf(IsModifyPrice));
	}

	/** Get Modify Price.
		@return Allow modifying the price
	  */
	public boolean isModifyPrice () 
	{
		Object oo = get_Value(COLUMNNAME_IsModifyPrice);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set POS Required PIN.
		@param IsPOSRequiredPIN 
		Indicates that a Supervisor Pin is mandatory to execute some tasks e.g. (Change Price , Offer Discount , Delete POS Line)
	  */
	public void setIsPOSRequiredPIN (boolean IsPOSRequiredPIN)
	{
		set_Value (COLUMNNAME_IsPOSRequiredPIN, Boolean.valueOf(IsPOSRequiredPIN));
	}

	/** Get POS Required PIN.
		@return Indicates that a Supervisor Pin is mandatory to execute some tasks e.g. (Change Price , Offer Discount , Delete POS Line)
	  */
	public boolean isPOSRequiredPIN () 
	{
		Object oo = get_Value(COLUMNNAME_IsPOSRequiredPIN);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Print Collet.
		@param IsPrintCollet 
		Print collet from POS
	  */
	public void setIsPrintCollet (boolean IsPrintCollet)
	{
		set_Value (COLUMNNAME_IsPrintCollet, Boolean.valueOf(IsPrintCollet));
	}

	/** Get Print Collet.
		@return Print collet from POS
	  */
	public boolean isPrintCollet () 
	{
		Object oo = get_Value(COLUMNNAME_IsPrintCollet);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Print Gift Card.
		@param IsPrintGiftCard 
		Print gift card from POS
	  */
	public void setIsPrintGiftCard (boolean IsPrintGiftCard)
	{
		set_Value (COLUMNNAME_IsPrintGiftCard, Boolean.valueOf(IsPrintGiftCard));
	}

	/** Get Print Gift Card.
		@return Print gift card from POS
	  */
	public boolean isPrintGiftCard () 
	{
		Object oo = get_Value(COLUMNNAME_IsPrintGiftCard);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Print Shipment.
		@param IsPrintShipment 
		Print shipment from POS
	  */
	public void setIsPrintShipment (boolean IsPrintShipment)
	{
		set_Value (COLUMNNAME_IsPrintShipment, Boolean.valueOf(IsPrintShipment));
	}

	/** Get Print Shipment.
		@return Print shipment from POS
	  */
	public boolean isPrintShipment () 
	{
		Object oo = get_Value(COLUMNNAME_IsPrintShipment);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Validate Cash Opening (From POS).
		@param IsValidatePOSCashOpening 
		Make a validation for Cash Opening for this POS
	  */
	public void setIsValidatePOSCashOpening (boolean IsValidatePOSCashOpening)
	{
		set_Value (COLUMNNAME_IsValidatePOSCashOpening, Boolean.valueOf(IsValidatePOSCashOpening));
	}

	/** Get Validate Cash Opening (From POS).
		@return Make a validation for Cash Opening for this POS
	  */
	public boolean isValidatePOSCashOpening () 
	{
		Object oo = get_Value(COLUMNNAME_IsValidatePOSCashOpening);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Validate Previous Cash Closing (From POS).
		@param IsValidatePOSPreviousCash 
		Make a validation for this Terminal before create translation
	  */
	public void setIsValidatePOSPreviousCash (boolean IsValidatePOSPreviousCash)
	{
		set_Value (COLUMNNAME_IsValidatePOSPreviousCash, Boolean.valueOf(IsValidatePOSPreviousCash));
	}

	/** Get Validate Previous Cash Closing (From POS).
		@return Make a validation for this Terminal before create translation
	  */
	public boolean isValidatePOSPreviousCash () 
	{
		Object oo = get_Value(COLUMNNAME_IsValidatePOSPreviousCash);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Maximum Daily Refund Allowed.
		@param MaximumDailyRefundAllowed 
		Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public void setMaximumDailyRefundAllowed (BigDecimal MaximumDailyRefundAllowed)
	{
		set_Value (COLUMNNAME_MaximumDailyRefundAllowed, MaximumDailyRefundAllowed);
	}

	/** Get Maximum Daily Refund Allowed.
		@return Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumDailyRefundAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumDailyRefundAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Maximum Discount %.
		@param MaximumDiscountAllowed 
		Discount in percent
	  */
	public void setMaximumDiscountAllowed (BigDecimal MaximumDiscountAllowed)
	{
		set_Value (COLUMNNAME_MaximumDiscountAllowed, MaximumDiscountAllowed);
	}

	/** Get Maximum Discount %.
		@return Discount in percent
	  */
	public BigDecimal getMaximumDiscountAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumDiscountAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Maximum Refund Allowed.
		@param MaximumRefundAllowed 
		Set the maximum refund allowed for this tender type using the POS currency
	  */
	public void setMaximumRefundAllowed (BigDecimal MaximumRefundAllowed)
	{
		set_Value (COLUMNNAME_MaximumRefundAllowed, MaximumRefundAllowed);
	}

	/** Get Maximum Refund Allowed.
		@return Set the maximum refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumRefundAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumRefundAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Measure Request Code.
		@param MeasureRequestCode 
		String for  taking measurement from Device Electronic Scales
	  */
	public void setMeasureRequestCode (String MeasureRequestCode)
	{
		set_Value (COLUMNNAME_MeasureRequestCode, MeasureRequestCode);
	}

	/** Get Measure Request Code.
		@return String for  taking measurement from Device Electronic Scales
	  */
	public String getMeasureRequestCode () 
	{
		return (String)get_Value(COLUMNNAME_MeasureRequestCode);
	}

	public I_M_PriceList getM_PriceList() throws RuntimeException
    {
		return (I_M_PriceList)MTable.get(getCtx(), I_M_PriceList.Table_Name)
			.getPO(getM_PriceList_ID(), get_TrxName());	}

	/** Set Price List.
		@param M_PriceList_ID 
		Unique identifier of a Price List
	  */
	public void setM_PriceList_ID (int M_PriceList_ID)
	{
		if (M_PriceList_ID < 1) 
			set_Value (COLUMNNAME_M_PriceList_ID, null);
		else 
			set_Value (COLUMNNAME_M_PriceList_ID, Integer.valueOf(M_PriceList_ID));
	}

	/** Get Price List.
		@return Unique identifier of a Price List
	  */
	public int getM_PriceList_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_PriceList_ID);
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

	public I_C_POSKeyLayout getOSK_KeyLayout() throws RuntimeException
    {
		return (I_C_POSKeyLayout)MTable.get(getCtx(), I_C_POSKeyLayout.Table_Name)
			.getPO(getOSK_KeyLayout_ID(), get_TrxName());	}

	/** Set On Screen Keyboard layout.
		@param OSK_KeyLayout_ID 
		The key layout to use for on screen keyboard for text fields.
	  */
	public void setOSK_KeyLayout_ID (int OSK_KeyLayout_ID)
	{
		if (OSK_KeyLayout_ID < 1) 
			set_Value (COLUMNNAME_OSK_KeyLayout_ID, null);
		else 
			set_Value (COLUMNNAME_OSK_KeyLayout_ID, Integer.valueOf(OSK_KeyLayout_ID));
	}

	/** Get On Screen Keyboard layout.
		@return The key layout to use for on screen keyboard for text fields.
	  */
	public int getOSK_KeyLayout_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_OSK_KeyLayout_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_POSKeyLayout getOSNP_KeyLayout() throws RuntimeException
    {
		return (I_C_POSKeyLayout)MTable.get(getCtx(), I_C_POSKeyLayout.Table_Name)
			.getPO(getOSNP_KeyLayout_ID(), get_TrxName());	}

	/** Set On Screen Number Pad layout.
		@param OSNP_KeyLayout_ID 
		The key layout to use for on screen number pad for numeric fields.
	  */
	public void setOSNP_KeyLayout_ID (int OSNP_KeyLayout_ID)
	{
		if (OSNP_KeyLayout_ID < 1) 
			set_Value (COLUMNNAME_OSNP_KeyLayout_ID, null);
		else 
			set_Value (COLUMNNAME_OSNP_KeyLayout_ID, Integer.valueOf(OSNP_KeyLayout_ID));
	}

	/** Get On Screen Number Pad layout.
		@return The key layout to use for on screen number pad for numeric fields.
	  */
	public int getOSNP_KeyLayout_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_OSNP_KeyLayout_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set PIN Entry Timeout.
		@param PINEntryTimeout 
		PIN Entry Timeout - the amount of time from initial display until the PIN entry dialog times out, in milliseconds.
	  */
	public void setPINEntryTimeout (int PINEntryTimeout)
	{
		set_Value (COLUMNNAME_PINEntryTimeout, Integer.valueOf(PINEntryTimeout));
	}

	/** Get PIN Entry Timeout.
		@return PIN Entry Timeout - the amount of time from initial display until the PIN entry dialog times out, in milliseconds.
	  */
	public int getPINEntryTimeout () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PINEntryTimeout);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPOSCashClosingDocumentType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPOSCashClosingDocumentType_ID(), get_TrxName());	}

	/** Set Cash Closing Document Type.
		@param POSCashClosingDocumentType_ID 
		Cash Closing Document Type for Cash or bank
	  */
	public void setPOSCashClosingDocumentType_ID (int POSCashClosingDocumentType_ID)
	{
		if (POSCashClosingDocumentType_ID < 1) 
			set_Value (COLUMNNAME_POSCashClosingDocumentType_ID, null);
		else 
			set_Value (COLUMNNAME_POSCashClosingDocumentType_ID, Integer.valueOf(POSCashClosingDocumentType_ID));
	}

	/** Get Cash Closing Document Type.
		@return Cash Closing Document Type for Cash or bank
	  */
	public int getPOSCashClosingDocumentType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_POSCashClosingDocumentType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPOSCollectingDocumentType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPOSCollectingDocumentType_ID(), get_TrxName());	}

	/** Set Collecting Document Type.
		@param POSCollectingDocumentType_ID 
		Collecting Document Type for Cash or bank
	  */
	public void setPOSCollectingDocumentType_ID (int POSCollectingDocumentType_ID)
	{
		if (POSCollectingDocumentType_ID < 1) 
			set_Value (COLUMNNAME_POSCollectingDocumentType_ID, null);
		else 
			set_Value (COLUMNNAME_POSCollectingDocumentType_ID, Integer.valueOf(POSCollectingDocumentType_ID));
	}

	/** Get Collecting Document Type.
		@return Collecting Document Type for Cash or bank
	  */
	public int getPOSCollectingDocumentType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_POSCollectingDocumentType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPOSDepositDocumentType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPOSDepositDocumentType_ID(), get_TrxName());	}

	/** Set Deposit Document Type.
		@param POSDepositDocumentType_ID 
		Deposit Document Type for Cash or bank
	  */
	public void setPOSDepositDocumentType_ID (int POSDepositDocumentType_ID)
	{
		if (POSDepositDocumentType_ID < 1) 
			set_Value (COLUMNNAME_POSDepositDocumentType_ID, null);
		else 
			set_Value (COLUMNNAME_POSDepositDocumentType_ID, Integer.valueOf(POSDepositDocumentType_ID));
	}

	/** Get Deposit Document Type.
		@return Deposit Document Type for Cash or bank
	  */
	public int getPOSDepositDocumentType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_POSDepositDocumentType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPOSOpeningDocumentType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPOSOpeningDocumentType_ID(), get_TrxName());	}

	/** Set Opening Document Type.
		@param POSOpeningDocumentType_ID 
		Opening Document Type for Cash or bank
	  */
	public void setPOSOpeningDocumentType_ID (int POSOpeningDocumentType_ID)
	{
		if (POSOpeningDocumentType_ID < 1) 
			set_Value (COLUMNNAME_POSOpeningDocumentType_ID, null);
		else 
			set_Value (COLUMNNAME_POSOpeningDocumentType_ID, Integer.valueOf(POSOpeningDocumentType_ID));
	}

	/** Get Opening Document Type.
		@return Opening Document Type for Cash or bank
	  */
	public int getPOSOpeningDocumentType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_POSOpeningDocumentType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPOSRefundDocumentType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPOSRefundDocumentType_ID(), get_TrxName());	}

	/** Set Refund Document Type.
		@param POSRefundDocumentType_ID 
		Refund Document Type for Cash or bank
	  */
	public void setPOSRefundDocumentType_ID (int POSRefundDocumentType_ID)
	{
		if (POSRefundDocumentType_ID < 1) 
			set_Value (COLUMNNAME_POSRefundDocumentType_ID, null);
		else 
			set_Value (COLUMNNAME_POSRefundDocumentType_ID, Integer.valueOf(POSRefundDocumentType_ID));
	}

	/** Get Refund Document Type.
		@return Refund Document Type for Cash or bank
	  */
	public int getPOSRefundDocumentType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_POSRefundDocumentType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPOSWithdrawalDocumentType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPOSWithdrawalDocumentType_ID(), get_TrxName());	}

	/** Set Withdrawal Document Type.
		@param POSWithdrawalDocumentType_ID 
		Withdrawal Document Type for Cash or bank
	  */
	public void setPOSWithdrawalDocumentType_ID (int POSWithdrawalDocumentType_ID)
	{
		if (POSWithdrawalDocumentType_ID < 1) 
			set_Value (COLUMNNAME_POSWithdrawalDocumentType_ID, null);
		else 
			set_Value (COLUMNNAME_POSWithdrawalDocumentType_ID, Integer.valueOf(POSWithdrawalDocumentType_ID));
	}

	/** Get Withdrawal Document Type.
		@return Withdrawal Document Type for Cash or bank
	  */
	public int getPOSWithdrawalDocumentType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_POSWithdrawalDocumentType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Printer Name.
		@param PrinterName 
		Name of the Printer
	  */
	public void setPrinterName (String PrinterName)
	{
		set_Value (COLUMNNAME_PrinterName, PrinterName);
	}

	/** Get Printer Name.
		@return Name of the Printer
	  */
	public String getPrinterName () 
	{
		return (String)get_Value(COLUMNNAME_PrinterName);
	}

	public I_C_Currency getRefundReferenceCurrency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getRefundReferenceCurrency_ID(), get_TrxName());	}

	/** Set Refund Reference Currency.
		@param RefundReferenceCurrency_ID 
		Refund Reference Currency for limit the allowed amount
	  */
	public void setRefundReferenceCurrency_ID (int RefundReferenceCurrency_ID)
	{
		if (RefundReferenceCurrency_ID < 1) 
			set_Value (COLUMNNAME_RefundReferenceCurrency_ID, null);
		else 
			set_Value (COLUMNNAME_RefundReferenceCurrency_ID, Integer.valueOf(RefundReferenceCurrency_ID));
	}

	/** Get Refund Reference Currency.
		@return Refund Reference Currency for limit the allowed amount
	  */
	public int getRefundReferenceCurrency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_RefundReferenceCurrency_ID);
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

	/** Set Ticket Handler Class Name.
		@param TicketClassName 
		Java Classname for Ticket Handler
	  */
	public void setTicketClassName (String TicketClassName)
	{
		set_Value (COLUMNNAME_TicketClassName, TicketClassName);
	}

	/** Get Ticket Handler Class Name.
		@return Java Classname for Ticket Handler
	  */
	public String getTicketClassName () 
	{
		return (String)get_Value(COLUMNNAME_TicketClassName);
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

	public I_C_Currency getWriteOffAmtCurrency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getWriteOffAmtCurrency_ID(), get_TrxName());	}

	/** Set Currency for write-off per Document.
		@param WriteOffAmtCurrency_ID 
		Currency amount to be written off in invoice currency
	  */
	public void setWriteOffAmtCurrency_ID (int WriteOffAmtCurrency_ID)
	{
		if (WriteOffAmtCurrency_ID < 1) 
			set_Value (COLUMNNAME_WriteOffAmtCurrency_ID, null);
		else 
			set_Value (COLUMNNAME_WriteOffAmtCurrency_ID, Integer.valueOf(WriteOffAmtCurrency_ID));
	}

	/** Get Currency for write-off per Document.
		@return Currency amount to be written off in invoice currency
	  */
	public int getWriteOffAmtCurrency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WriteOffAmtCurrency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Tolerance for write-off per Document.
		@param WriteOffAmtTolerance 
		Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffAmtTolerance (BigDecimal WriteOffAmtTolerance)
	{
		set_Value (COLUMNNAME_WriteOffAmtTolerance, WriteOffAmtTolerance);
	}

	/** Get Tolerance for write-off per Document.
		@return Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffAmtTolerance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WriteOffAmtTolerance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Tolerance for write-off %.
		@param WriteOffPercentageTolerance 
		Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffPercentageTolerance (BigDecimal WriteOffPercentageTolerance)
	{
		set_Value (COLUMNNAME_WriteOffPercentageTolerance, WriteOffPercentageTolerance);
	}

	/** Get Tolerance for write-off %.
		@return Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffPercentageTolerance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WriteOffPercentageTolerance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}