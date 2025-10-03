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

/** Generated Interface for C_POS
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_C_POS 
{

    /** TableName=C_POS */
    public static final String Table_Name = "C_POS";

    /** AD_Table_ID=748 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 2 - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(2);

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

    /** Column name AutoLogoutDelay */
    public static final String COLUMNNAME_AutoLogoutDelay = "AutoLogoutDelay";

	/** Set Auto Logout Delay.
	  * Automatically logout if terminal inactive for this period
	  */
	public void setAutoLogoutDelay (int AutoLogoutDelay);

	/** Get Auto Logout Delay.
	  * Automatically logout if terminal inactive for this period
	  */
	public int getAutoLogoutDelay();

    /** Column name CashDrawer */
    public static final String COLUMNNAME_CashDrawer = "CashDrawer";

	/** Set CashDrawer	  */
	public void setCashDrawer (String CashDrawer);

	/** Get CashDrawer	  */
	public String getCashDrawer();

    /** Column name CashTransferBankAccount_ID */
    public static final String COLUMNNAME_CashTransferBankAccount_ID = "CashTransferBankAccount_ID";

	/** Set Transfer Cash trx to.
	  * Bank Account on which to transfer all Cash transactions
	  */
	public void setCashTransferBankAccount_ID (int CashTransferBankAccount_ID);

	/** Get Transfer Cash trx to.
	  * Bank Account on which to transfer all Cash transactions
	  */
	public int getCashTransferBankAccount_ID();

	public I_C_BankAccount getCashTransferBankAccount() throws RuntimeException;

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

    /** Column name C_BPartnerCashTrx_ID */
    public static final String COLUMNNAME_C_BPartnerCashTrx_ID = "C_BPartnerCashTrx_ID";

	/** Set Template B.Partner.
	  * Business Partner used for creating new Business Partners on the fly
	  */
	public void setC_BPartnerCashTrx_ID (int C_BPartnerCashTrx_ID);

	/** Get Template B.Partner.
	  * Business Partner used for creating new Business Partners on the fly
	  */
	public int getC_BPartnerCashTrx_ID();

	public I_C_BPartner getC_BPartnerCashTrx() throws RuntimeException;

    /** Column name C_CashBook_ID */
    public static final String COLUMNNAME_C_CashBook_ID = "C_CashBook_ID";

	/** Set Cash Book.
	  * Cash Book for recording petty cash transactions
	  */
	public void setC_CashBook_ID (int C_CashBook_ID);

	/** Get Cash Book.
	  * Cash Book for recording petty cash transactions
	  */
	public int getC_CashBook_ID();

	public I_C_CashBook getC_CashBook() throws RuntimeException;

    /** Column name C_ConversionType_ID */
    public static final String COLUMNNAME_C_ConversionType_ID = "C_ConversionType_ID";

	/** Set Currency Type.
	  * Currency Conversion Rate Type
	  */
	public void setC_ConversionType_ID (int C_ConversionType_ID);

	/** Get Currency Type.
	  * Currency Conversion Rate Type
	  */
	public int getC_ConversionType_ID();

	public I_C_ConversionType getC_ConversionType() throws RuntimeException;

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

    /** Column name C_POS_ID */
    public static final String COLUMNNAME_C_POS_ID = "C_POS_ID";

	/** Set POS Terminal.
	  * Point of Sales Terminal
	  */
	public void setC_POS_ID (int C_POS_ID);

	/** Get POS Terminal.
	  * Point of Sales Terminal
	  */
	public int getC_POS_ID();

    /** Column name C_POSKeyLayout_ID */
    public static final String COLUMNNAME_C_POSKeyLayout_ID = "C_POSKeyLayout_ID";

	/** Set POS Key Layout.
	  * POS Function Key Layout
	  */
	public void setC_POSKeyLayout_ID (int C_POSKeyLayout_ID);

	/** Get POS Key Layout.
	  * POS Function Key Layout
	  */
	public int getC_POSKeyLayout_ID();

	public I_C_POSKeyLayout getC_POSKeyLayout() throws RuntimeException;

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

    /** Column name DefaultCampaign_ID */
    public static final String COLUMNNAME_DefaultCampaign_ID = "DefaultCampaign_ID";

	/** Set Default Campaign.
	  * Marketing Campaign
	  */
	public void setDefaultCampaign_ID (int DefaultCampaign_ID);

	/** Get Default Campaign.
	  * Marketing Campaign
	  */
	public int getDefaultCampaign_ID();

	public I_C_Campaign getDefaultCampaign() throws RuntimeException;

    /** Column name DefaultDiscountCharge_ID */
    public static final String COLUMNNAME_DefaultDiscountCharge_ID = "DefaultDiscountCharge_ID";

	/** Set Default Discount Charge.
	  * Default Discount Charge for POS
	  */
	public void setDefaultDiscountCharge_ID (int DefaultDiscountCharge_ID);

	/** Get Default Discount Charge.
	  * Default Discount Charge for POS
	  */
	public int getDefaultDiscountCharge_ID();

	public I_C_Charge getDefaultDiscountCharge() throws RuntimeException;

    /** Column name DeliveryRule */
    public static final String COLUMNNAME_DeliveryRule = "DeliveryRule";

	/** Set Delivery Rule.
	  * Defines the timing of Delivery
	  */
	public void setDeliveryRule (String DeliveryRule);

	/** Get Delivery Rule.
	  * Defines the timing of Delivery
	  */
	public String getDeliveryRule();

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

    /** Column name DisplayCurrency_ID */
    public static final String COLUMNNAME_DisplayCurrency_ID = "DisplayCurrency_ID";

	/** Set Display Currency.
	  * Display Currency for POS and Price Checking
	  */
	public void setDisplayCurrency_ID (int DisplayCurrency_ID);

	/** Get Display Currency.
	  * Display Currency for POS and Price Checking
	  */
	public int getDisplayCurrency_ID();

	public I_C_Currency getDisplayCurrency() throws RuntimeException;

    /** Column name ECA14_DefaultGiftCardCharge_ID */
    public static final String COLUMNNAME_ECA14_DefaultGiftCardCharge_ID = "ECA14_DefaultGiftCardCharge_ID";

	/** Set Default Gift Card Charge.
	  * Default Gift Card Charge for POS
	  */
	public void setECA14_DefaultGiftCardCharge_ID (int ECA14_DefaultGiftCardCharge_ID);

	/** Get Default Gift Card Charge.
	  * Default Gift Card Charge for POS
	  */
	public int getECA14_DefaultGiftCardCharge_ID();

	public I_C_Charge getECA14_DefaultGiftCardCharge() throws RuntimeException;

    /** Column name ElectronicScales */
    public static final String COLUMNNAME_ElectronicScales = "ElectronicScales";

	/** Set Electronic Scales.
	  * Allows to define path for Device Electronic Scales e.g. /dev/ttyS0/
	  */
	public void setElectronicScales (String ElectronicScales);

	/** Get Electronic Scales.
	  * Allows to define path for Device Electronic Scales e.g. /dev/ttyS0/
	  */
	public String getElectronicScales();

    /** Column name Help */
    public static final String COLUMNNAME_Help = "Help";

	/** Set Comment/Help.
	  * Comment or Hint
	  */
	public void setHelp (String Help);

	/** Get Comment/Help.
	  * Comment or Hint
	  */
	public String getHelp();

    /** Column name InvoiceRule */
    public static final String COLUMNNAME_InvoiceRule = "InvoiceRule";

	/** Set Invoice Rule.
	  * Frequency and method of invoicing 
	  */
	public void setInvoiceRule (String InvoiceRule);

	/** Get Invoice Rule.
	  * Frequency and method of invoicing 
	  */
	public String getInvoiceRule();

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

    /** Column name IsAllowsAllocateSeller */
    public static final String COLUMNNAME_IsAllowsAllocateSeller = "IsAllowsAllocateSeller";

	/** Set Allows Allocate Seller.
	  * Allows Allocate Seller for this POS Terminal
	  */
	public void setIsAllowsAllocateSeller (boolean IsAllowsAllocateSeller);

	/** Get Allows Allocate Seller.
	  * Allows Allocate Seller for this POS Terminal
	  */
	public boolean isAllowsAllocateSeller();

    /** Column name IsAllowsApplyDiscount */
    public static final String COLUMNNAME_IsAllowsApplyDiscount = "IsAllowsApplyDiscount";

	/** Set Allows Apply Discount (By Document).
	  * Allows Apply Discount for this POS Terminal
	  */
	public void setIsAllowsApplyDiscount (boolean IsAllowsApplyDiscount);

	/** Get Allows Apply Discount (By Document).
	  * Allows Apply Discount for this POS Terminal
	  */
	public boolean isAllowsApplyDiscount();

    /** Column name IsAllowsCashClosing */
    public static final String COLUMNNAME_IsAllowsCashClosing = "IsAllowsCashClosing";

	/** Set Allows Cash Closing.
	  * Allows Cash Closing
	  */
	public void setIsAllowsCashClosing (boolean IsAllowsCashClosing);

	/** Get Allows Cash Closing.
	  * Allows Cash Closing
	  */
	public boolean isAllowsCashClosing();

    /** Column name IsAllowsCashOpening */
    public static final String COLUMNNAME_IsAllowsCashOpening = "IsAllowsCashOpening";

	/** Set Allows Cash Opening.
	  * Allows Cash Opening
	  */
	public void setIsAllowsCashOpening (boolean IsAllowsCashOpening);

	/** Get Allows Cash Opening.
	  * Allows Cash Opening
	  */
	public boolean isAllowsCashOpening();

    /** Column name IsAllowsCashWithdrawal */
    public static final String COLUMNNAME_IsAllowsCashWithdrawal = "IsAllowsCashWithdrawal";

	/** Set Allows Cash Withdrawal.
	  * Allows Cash Withdrawal
	  */
	public void setIsAllowsCashWithdrawal (boolean IsAllowsCashWithdrawal);

	/** Get Allows Cash Withdrawal.
	  * Allows Cash Withdrawal
	  */
	public boolean isAllowsCashWithdrawal();

    /** Column name IsAllowsCollectOrder */
    public static final String COLUMNNAME_IsAllowsCollectOrder = "IsAllowsCollectOrder";

	/** Set Allows Collect Order.
	  * Allows collect a Sales Order
	  */
	public void setIsAllowsCollectOrder (boolean IsAllowsCollectOrder);

	/** Get Allows Collect Order.
	  * Allows collect a Sales Order
	  */
	public boolean isAllowsCollectOrder();

    /** Column name IsAllowsConcurrentUse */
    public static final String COLUMNNAME_IsAllowsConcurrentUse = "IsAllowsConcurrentUse";

	/** Set Allows Concurrent Use.
	  * Allows Concurrent Use for this terminal, both sellers can make sales on one time
	  */
	public void setIsAllowsConcurrentUse (boolean IsAllowsConcurrentUse);

	/** Get Allows Concurrent Use.
	  * Allows Concurrent Use for this terminal, both sellers can make sales on one time
	  */
	public boolean isAllowsConcurrentUse();

    /** Column name IsAllowsConfirmShipment */
    public static final String COLUMNNAME_IsAllowsConfirmShipment = "IsAllowsConfirmShipment";

	/** Set Allows Confirm Shipment.
	  * Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipment (boolean IsAllowsConfirmShipment);

	/** Get Allows Confirm Shipment.
	  * Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipment();

    /** Column name IsAllowsConfirmShipmentByOrder */
    public static final String COLUMNNAME_IsAllowsConfirmShipmentByOrder = "IsAllowsConfirmShipmentByOrder";

	/** Set Allows Confirm Shipment by Order.
	  * Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipmentByOrder (boolean IsAllowsConfirmShipmentByOrder);

	/** Get Allows Confirm Shipment by Order.
	  * Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipmentByOrder();

    /** Column name IsAllowsCreateCustomer */
    public static final String COLUMNNAME_IsAllowsCreateCustomer = "IsAllowsCreateCustomer";

	/** Set Allows Create Customer.
	  * Allows create a Customer from POS
	  */
	public void setIsAllowsCreateCustomer (boolean IsAllowsCreateCustomer);

	/** Get Allows Create Customer.
	  * Allows create a Customer from POS
	  */
	public boolean isAllowsCreateCustomer();

    /** Column name IsAllowsCreateOrder */
    public static final String COLUMNNAME_IsAllowsCreateOrder = "IsAllowsCreateOrder";

	/** Set Allows Create Order.
	  * Allows create a Sales Order
	  */
	public void setIsAllowsCreateOrder (boolean IsAllowsCreateOrder);

	/** Get Allows Create Order.
	  * Allows create a Sales Order
	  */
	public boolean isAllowsCreateOrder();

    /** Column name IsAllowsGiftCard */
    public static final String COLUMNNAME_IsAllowsGiftCard = "IsAllowsGiftCard";

	/** Set Allows Gift Card.
	  * Allows Gift Card
	  */
	public void setIsAllowsGiftCard (boolean IsAllowsGiftCard);

	/** Get Allows Gift Card.
	  * Allows Gift Card
	  */
	public boolean isAllowsGiftCard();

    /** Column name IsAllowsModifyCustomer */
    public static final String COLUMNNAME_IsAllowsModifyCustomer = "IsAllowsModifyCustomer";

	/** Set Allows Modify Customer.
	  * Allows Modify Customer from POS
	  */
	public void setIsAllowsModifyCustomer (boolean IsAllowsModifyCustomer);

	/** Get Allows Modify Customer.
	  * Allows Modify Customer from POS
	  */
	public boolean isAllowsModifyCustomer();

    /** Column name IsAllowsModifyQuantity */
    public static final String COLUMNNAME_IsAllowsModifyQuantity = "IsAllowsModifyQuantity";

	/** Set Allows Modify Quantity.
	  * Allows modifying the quantity
	  */
	public void setIsAllowsModifyQuantity (boolean IsAllowsModifyQuantity);

	/** Get Allows Modify Quantity.
	  * Allows modifying the quantity
	  */
	public boolean isAllowsModifyQuantity();

    /** Column name IsAllowsPreviewDocument */
    public static final String COLUMNNAME_IsAllowsPreviewDocument = "IsAllowsPreviewDocument";

	/** Set Allows Print Preview.
	  * Allows print document with preview from POS
	  */
	public void setIsAllowsPreviewDocument (boolean IsAllowsPreviewDocument);

	/** Get Allows Print Preview.
	  * Allows print document with preview from POS
	  */
	public boolean isAllowsPreviewDocument();

    /** Column name IsAllowsPrintDocument */
    public static final String COLUMNNAME_IsAllowsPrintDocument = "IsAllowsPrintDocument";

	/** Set Allows Print Document.
	  * Allows print document from POS
	  */
	public void setIsAllowsPrintDocument (boolean IsAllowsPrintDocument);

	/** Get Allows Print Document.
	  * Allows print document from POS
	  */
	public boolean isAllowsPrintDocument();

    /** Column name IsAllowsReturnOrder */
    public static final String COLUMNNAME_IsAllowsReturnOrder = "IsAllowsReturnOrder";

	/** Set Allows Return Order.
	  * Allows return a Sales Order
	  */
	public void setIsAllowsReturnOrder (boolean IsAllowsReturnOrder);

	/** Get Allows Return Order.
	  * Allows return a Sales Order
	  */
	public boolean isAllowsReturnOrder();

    /** Column name IsConfidentialInfo */
    public static final String COLUMNNAME_IsConfidentialInfo = "IsConfidentialInfo";

	/** Set Confidential Info.
	  * Can enter confidential information
	  */
	public void setIsConfidentialInfo (boolean IsConfidentialInfo);

	/** Get Confidential Info.
	  * Can enter confidential information
	  */
	public boolean isConfidentialInfo();

    /** Column name IsConfirmCompleteShipment */
    public static final String COLUMNNAME_IsConfirmCompleteShipment = "IsConfirmCompleteShipment";

	/** Set Confirm Only Complete Shipment.
	  * Confirm Only when a Shipment is completely
	  */
	public void setIsConfirmCompleteShipment (boolean IsConfirmCompleteShipment);

	/** Get Confirm Only Complete Shipment.
	  * Confirm Only when a Shipment is completely
	  */
	public boolean isConfirmCompleteShipment();

    /** Column name IsDirectPrint */
    public static final String COLUMNNAME_IsDirectPrint = "IsDirectPrint";

	/** Set Direct print.
	  * Print without dialog
	  */
	public void setIsDirectPrint (boolean IsDirectPrint);

	/** Get Direct print.
	  * Print without dialog
	  */
	public boolean isDirectPrint();

    /** Column name IsDisplayDiscount */
    public static final String COLUMNNAME_IsDisplayDiscount = "IsDisplayDiscount";

	/** Set Display Discount.
	  * Display Discount on POS window
	  */
	public void setIsDisplayDiscount (boolean IsDisplayDiscount);

	/** Get Display Discount.
	  * Display Discount on POS window
	  */
	public boolean isDisplayDiscount();

    /** Column name IsDisplayTaxAmount */
    public static final String COLUMNNAME_IsDisplayTaxAmount = "IsDisplayTaxAmount";

	/** Set Display Tax Amount.
	  * Display Tax Amount on POS window
	  */
	public void setIsDisplayTaxAmount (boolean IsDisplayTaxAmount);

	/** Get Display Tax Amount.
	  * Display Tax Amount on POS window
	  */
	public boolean isDisplayTaxAmount();

    /** Column name IsEnableProductLookup */
    public static final String COLUMNNAME_IsEnableProductLookup = "IsEnableProductLookup";

	/** Set Enable POS Product Lookup.
	  * Allows product lookup in order to show search key , name , quantity available , standard price and list price for selecting a product
	  */
	public void setIsEnableProductLookup (boolean IsEnableProductLookup);

	/** Get Enable POS Product Lookup.
	  * Allows product lookup in order to show search key , name , quantity available , standard price and list price for selecting a product
	  */
	public boolean isEnableProductLookup();

    /** Column name IsKeepPriceFromCustomer */
    public static final String COLUMNNAME_IsKeepPriceFromCustomer = "IsKeepPriceFromCustomer";

	/** Set Keep Price from Customer.
	  * Keep Price from Customer when ia create a sales order from POS
	  */
	public void setIsKeepPriceFromCustomer (boolean IsKeepPriceFromCustomer);

	/** Column name IsValidateOnlineClosing */
	public static final String COLUMNNAME_IsValidateOnlineClosing = "IsValidateOnlineClosing";

	/** Set Validate Online Closing	  */
	public void setIsValidateOnlineClosing (boolean IsValidateOnlineClosing);

	/** Get Validate Online Closing	  */
	public boolean isValidateOnlineClosing();

	/** Get Keep Price from Customer.
	  * Keep Price from Customer when ia create a sales order from POS
	  */
	public boolean isKeepPriceFromCustomer();

    /** Column name IsModifyPrice */
    public static final String COLUMNNAME_IsModifyPrice = "IsModifyPrice";

	/** Set Modify Price.
	  * Allow modifying the price
	  */
	public void setIsModifyPrice (boolean IsModifyPrice);

	/** Get Modify Price.
	  * Allow modifying the price
	  */
	public boolean isModifyPrice();

    /** Column name IsPOSRequiredPIN */
    public static final String COLUMNNAME_IsPOSRequiredPIN = "IsPOSRequiredPIN";

	/** Set POS Required PIN.
	  * Indicates that a Supervisor Pin is mandatory to execute some tasks e.g. (Change Price , Offer Discount , Delete POS Line)
	  */
	public void setIsPOSRequiredPIN (boolean IsPOSRequiredPIN);

	/** Get POS Required PIN.
	  * Indicates that a Supervisor Pin is mandatory to execute some tasks e.g. (Change Price , Offer Discount , Delete POS Line)
	  */
	public boolean isPOSRequiredPIN();

    /** Column name IsPrintCollet */
    public static final String COLUMNNAME_IsPrintCollet = "IsPrintCollet";

	/** Set Print Collet.
	  * Print collet from POS
	  */
	public void setIsPrintCollet (boolean IsPrintCollet);

	/** Get Print Collet.
	  * Print collet from POS
	  */
	public boolean isPrintCollet();

    /** Column name IsPrintGiftCard */
    public static final String COLUMNNAME_IsPrintGiftCard = "IsPrintGiftCard";

	/** Set Print Gift Card.
	  * Print gift card from POS
	  */
	public void setIsPrintGiftCard (boolean IsPrintGiftCard);

	/** Get Print Gift Card.
	  * Print gift card from POS
	  */
	public boolean isPrintGiftCard();

    /** Column name IsPrintShipment */
    public static final String COLUMNNAME_IsPrintShipment = "IsPrintShipment";

	/** Set Print Shipment.
	  * Print shipment from POS
	  */
	public void setIsPrintShipment (boolean IsPrintShipment);

	/** Get Print Shipment.
	  * Print shipment from POS
	  */
	public boolean isPrintShipment();

    /** Column name IsValidatePOSCashOpening */
    public static final String COLUMNNAME_IsValidatePOSCashOpening = "IsValidatePOSCashOpening";

	/** Set Validate Cash Opening (From POS).
	  * Make a validation for Cash Opening for this POS
	  */
	public void setIsValidatePOSCashOpening (boolean IsValidatePOSCashOpening);

	/** Get Validate Cash Opening (From POS).
	  * Make a validation for Cash Opening for this POS
	  */
	public boolean isValidatePOSCashOpening();

    /** Column name IsValidatePOSPreviousCash */
    public static final String COLUMNNAME_IsValidatePOSPreviousCash = "IsValidatePOSPreviousCash";

	/** Set Validate Previous Cash Closing (From POS).
	  * Make a validation for this Terminal before create translation
	  */
	public void setIsValidatePOSPreviousCash (boolean IsValidatePOSPreviousCash);

	/** Get Validate Previous Cash Closing (From POS).
	  * Make a validation for this Terminal before create translation
	  */
	public boolean isValidatePOSPreviousCash();

    /** Column name MaximumDailyRefundAllowed */
    public static final String COLUMNNAME_MaximumDailyRefundAllowed = "MaximumDailyRefundAllowed";

	/** Set Maximum Daily Refund Allowed.
	  * Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public void setMaximumDailyRefundAllowed (BigDecimal MaximumDailyRefundAllowed);

	/** Get Maximum Daily Refund Allowed.
	  * Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumDailyRefundAllowed();

    /** Column name MaximumDiscountAllowed */
    public static final String COLUMNNAME_MaximumDiscountAllowed = "MaximumDiscountAllowed";

	/** Set Maximum Discount %.
	  * Discount in percent
	  */
	public void setMaximumDiscountAllowed (BigDecimal MaximumDiscountAllowed);

	/** Get Maximum Discount %.
	  * Discount in percent
	  */
	public BigDecimal getMaximumDiscountAllowed();

    /** Column name MaximumRefundAllowed */
    public static final String COLUMNNAME_MaximumRefundAllowed = "MaximumRefundAllowed";

	/** Set Maximum Refund Allowed.
	  * Set the maximum refund allowed for this tender type using the POS currency
	  */
	public void setMaximumRefundAllowed (BigDecimal MaximumRefundAllowed);

	/** Get Maximum Refund Allowed.
	  * Set the maximum refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumRefundAllowed();

    /** Column name MeasureRequestCode */
    public static final String COLUMNNAME_MeasureRequestCode = "MeasureRequestCode";

	/** Set Measure Request Code.
	  * String for  taking measurement from Device Electronic Scales
	  */
	public void setMeasureRequestCode (String MeasureRequestCode);

	/** Get Measure Request Code.
	  * String for  taking measurement from Device Electronic Scales
	  */
	public String getMeasureRequestCode();

    /** Column name M_PriceList_ID */
    public static final String COLUMNNAME_M_PriceList_ID = "M_PriceList_ID";

	/** Set Price List.
	  * Unique identifier of a Price List
	  */
	public void setM_PriceList_ID (int M_PriceList_ID);

	/** Get Price List.
	  * Unique identifier of a Price List
	  */
	public int getM_PriceList_ID();

	public I_M_PriceList getM_PriceList() throws RuntimeException;

    /** Column name M_Warehouse_ID */
    public static final String COLUMNNAME_M_Warehouse_ID = "M_Warehouse_ID";

	/** Set Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID);

	/** Get Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID();

	public I_M_Warehouse getM_Warehouse() throws RuntimeException;

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

    /** Column name OSK_KeyLayout_ID */
    public static final String COLUMNNAME_OSK_KeyLayout_ID = "OSK_KeyLayout_ID";

	/** Set On Screen Keyboard layout.
	  * The key layout to use for on screen keyboard for text fields.
	  */
	public void setOSK_KeyLayout_ID (int OSK_KeyLayout_ID);

	/** Get On Screen Keyboard layout.
	  * The key layout to use for on screen keyboard for text fields.
	  */
	public int getOSK_KeyLayout_ID();

	public I_C_POSKeyLayout getOSK_KeyLayout() throws RuntimeException;

    /** Column name OSNP_KeyLayout_ID */
    public static final String COLUMNNAME_OSNP_KeyLayout_ID = "OSNP_KeyLayout_ID";

	/** Set On Screen Number Pad layout.
	  * The key layout to use for on screen number pad for numeric fields.
	  */
	public void setOSNP_KeyLayout_ID (int OSNP_KeyLayout_ID);

	/** Get On Screen Number Pad layout.
	  * The key layout to use for on screen number pad for numeric fields.
	  */
	public int getOSNP_KeyLayout_ID();

	public I_C_POSKeyLayout getOSNP_KeyLayout() throws RuntimeException;

    /** Column name PINEntryTimeout */
    public static final String COLUMNNAME_PINEntryTimeout = "PINEntryTimeout";

	/** Set PIN Entry Timeout.
	  * PIN Entry Timeout - the amount of time from initial display until the PIN entry dialog times out, in milliseconds.
	  */
	public void setPINEntryTimeout (int PINEntryTimeout);

	/** Get PIN Entry Timeout.
	  * PIN Entry Timeout - the amount of time from initial display until the PIN entry dialog times out, in milliseconds.
	  */
	public int getPINEntryTimeout();

    /** Column name POSCashClosingDocumentType_ID */
    public static final String COLUMNNAME_POSCashClosingDocumentType_ID = "POSCashClosingDocumentType_ID";

	/** Set Cash Closing Document Type.
	  * Cash Closing Document Type for Cash or bank
	  */
	public void setPOSCashClosingDocumentType_ID (int POSCashClosingDocumentType_ID);

	/** Get Cash Closing Document Type.
	  * Cash Closing Document Type for Cash or bank
	  */
	public int getPOSCashClosingDocumentType_ID();

	public I_C_DocType getPOSCashClosingDocumentType() throws RuntimeException;

    /** Column name POSCollectingDocumentType_ID */
    public static final String COLUMNNAME_POSCollectingDocumentType_ID = "POSCollectingDocumentType_ID";

	/** Set Collecting Document Type.
	  * Collecting Document Type for Cash or bank
	  */
	public void setPOSCollectingDocumentType_ID (int POSCollectingDocumentType_ID);

	/** Get Collecting Document Type.
	  * Collecting Document Type for Cash or bank
	  */
	public int getPOSCollectingDocumentType_ID();

	public I_C_DocType getPOSCollectingDocumentType() throws RuntimeException;

    /** Column name POSDepositDocumentType_ID */
    public static final String COLUMNNAME_POSDepositDocumentType_ID = "POSDepositDocumentType_ID";

	/** Set Deposit Document Type.
	  * Deposit Document Type for Cash or bank
	  */
	public void setPOSDepositDocumentType_ID (int POSDepositDocumentType_ID);

	/** Get Deposit Document Type.
	  * Deposit Document Type for Cash or bank
	  */
	public int getPOSDepositDocumentType_ID();

	public I_C_DocType getPOSDepositDocumentType() throws RuntimeException;

    /** Column name POSOpeningDocumentType_ID */
    public static final String COLUMNNAME_POSOpeningDocumentType_ID = "POSOpeningDocumentType_ID";

	/** Set Opening Document Type.
	  * Opening Document Type for Cash or bank
	  */
	public void setPOSOpeningDocumentType_ID (int POSOpeningDocumentType_ID);

	/** Get Opening Document Type.
	  * Opening Document Type for Cash or bank
	  */
	public int getPOSOpeningDocumentType_ID();

	public I_C_DocType getPOSOpeningDocumentType() throws RuntimeException;

    /** Column name POSRefundDocumentType_ID */
    public static final String COLUMNNAME_POSRefundDocumentType_ID = "POSRefundDocumentType_ID";

	/** Set Refund Document Type.
	  * Refund Document Type for Cash or bank
	  */
	public void setPOSRefundDocumentType_ID (int POSRefundDocumentType_ID);

	/** Get Refund Document Type.
	  * Refund Document Type for Cash or bank
	  */
	public int getPOSRefundDocumentType_ID();

	public I_C_DocType getPOSRefundDocumentType() throws RuntimeException;

    /** Column name POSWithdrawalDocumentType_ID */
    public static final String COLUMNNAME_POSWithdrawalDocumentType_ID = "POSWithdrawalDocumentType_ID";

	/** Set Withdrawal Document Type.
	  * Withdrawal Document Type for Cash or bank
	  */
	public void setPOSWithdrawalDocumentType_ID (int POSWithdrawalDocumentType_ID);

	/** Get Withdrawal Document Type.
	  * Withdrawal Document Type for Cash or bank
	  */
	public int getPOSWithdrawalDocumentType_ID();

	public I_C_DocType getPOSWithdrawalDocumentType() throws RuntimeException;

    /** Column name PrinterName */
    public static final String COLUMNNAME_PrinterName = "PrinterName";

	/** Set Printer Name.
	  * Name of the Printer
	  */
	public void setPrinterName (String PrinterName);

	/** Get Printer Name.
	  * Name of the Printer
	  */
	public String getPrinterName();

    /** Column name RefundReferenceCurrency_ID */
    public static final String COLUMNNAME_RefundReferenceCurrency_ID = "RefundReferenceCurrency_ID";

	/** Set Refund Reference Currency.
	  * Refund Reference Currency for limit the allowed amount
	  */
	public void setRefundReferenceCurrency_ID (int RefundReferenceCurrency_ID);

	/** Get Refund Reference Currency.
	  * Refund Reference Currency for limit the allowed amount
	  */
	public int getRefundReferenceCurrency_ID();

	public I_C_Currency getRefundReferenceCurrency() throws RuntimeException;

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

    /** Column name TicketClassName */
    public static final String COLUMNNAME_TicketClassName = "TicketClassName";

	/** Set Ticket Handler Class Name.
	  * Java Classname for Ticket Handler
	  */
	public void setTicketClassName (String TicketClassName);

	/** Get Ticket Handler Class Name.
	  * Java Classname for Ticket Handler
	  */
	public String getTicketClassName();

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

    /** Column name WriteOffAmtCurrency_ID */
    public static final String COLUMNNAME_WriteOffAmtCurrency_ID = "WriteOffAmtCurrency_ID";

	/** Set Currency for write-off per Document.
	  * Currency amount to be written off in invoice currency
	  */
	public void setWriteOffAmtCurrency_ID (int WriteOffAmtCurrency_ID);

	/** Get Currency for write-off per Document.
	  * Currency amount to be written off in invoice currency
	  */
	public int getWriteOffAmtCurrency_ID();

	public I_C_Currency getWriteOffAmtCurrency() throws RuntimeException;

    /** Column name WriteOffAmtTolerance */
    public static final String COLUMNNAME_WriteOffAmtTolerance = "WriteOffAmtTolerance";

	/** Set Tolerance for write-off per Document.
	  * Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffAmtTolerance (BigDecimal WriteOffAmtTolerance);

	/** Get Tolerance for write-off per Document.
	  * Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffAmtTolerance();

    /** Column name WriteOffPercentageTolerance */
    public static final String COLUMNNAME_WriteOffPercentageTolerance = "WriteOffPercentageTolerance";

	/** Set Tolerance for write-off %.
	  * Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffPercentageTolerance (BigDecimal WriteOffPercentageTolerance);

	/** Get Tolerance for write-off %.
	  * Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffPercentageTolerance();
}
