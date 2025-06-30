/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/

package org.spin.pos.util;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * A util class for static values of column added for POS
 */
public interface ColumnsAdded {
	String COLUMNNAME_ECA14_WriteOffByPercent = "ECA14_WriteOffByPercent";
	String COLUMNNAME_IsAllowsAllocateSeller = "IsAllowsAllocateSeller";
	String COLUMNNAME_IsAllowsApplyDiscount = "IsAllowsApplyDiscount";
	String COLUMNNAME_IsAllowsCashClosing = "IsAllowsCashClosing";
	String COLUMNNAME_IsAllowsCashOpening = "IsAllowsCashOpening";
	String COLUMNNAME_IsAllowsCashWithdrawal = "IsAllowsCashWithdrawal";
	String COLUMNNAME_IsAllowsCollectOrder = "IsAllowsCollectOrder";
	String COLUMNNAME_IsAllowsConfirmShipment = "IsAllowsConfirmShipment";
	String COLUMNNAME_IsAllowsConfirmShipmentByOrder = "IsAllowsConfirmShipmentByOrder";
	String COLUMNNAME_IsAllowsCreateCustomer = "IsAllowsCreateCustomer";
	String COLUMNNAME_IsAllowsCreateOrder = "IsAllowsCreateOrder";
	String COLUMNNAME_IsAllowsDetailCashClosing = "IsAllowsDetailCashClosing";
	String COLUMNNAME_IsAllowsModifyCustomer = "IsAllowsModifyCustomer";
	String COLUMNNAME_IsAllowsModifyDiscount = "IsAllowsModifyDiscount";
	String COLUMNNAME_IsAllowsModifyQuantity = "IsAllowsModifyQuantity";
	String COLUMNNAME_IsAllowsPOSManager = "IsAllowsPOSManager";
	String COLUMNNAME_IsAllowsPreviewDocument = "IsAllowsPreviewDocument";
	String COLUMNNAME_IsAllowsPrintDocument = "IsAllowsPrintDocument";
	String COLUMNNAME_IsAllowsReturnOrder = "IsAllowsReturnOrder";
	String COLUMNNAME_IsModifyPrice = "IsModifyPrice";
	String COLUMNNAME_MaximumDailyRefundAllowed = "MaximumDailyRefundAllowed";
	String COLUMNNAME_MaximumDiscountAllowed = "MaximumDiscountAllowed";
	String COLUMNNAME_MaximumLineDiscountAllowed = "MaximumLineDiscountAllowed";
	String COLUMNNAME_MaximumRefundAllowed = "MaximumRefundAllowed";
	String COLUMNNAME_WriteOffAmtCurrency_ID = "WriteOffAmtCurrency_ID";
	String COLUMNNAME_WriteOffAmtTolerance = "WriteOffAmtTolerance";
	String COLUMNNAME_WriteOffPercentageTolerance = "WriteOffPercentageTolerance";
	String COLUMNNAME_IsAllowsWriteOffAmount = "IsAllowsWriteOffAmount";
	String COLUMNNAME_IsAllowsCustomerTemplate = "IsAllowsCustomerTemplate";
	String COLUMNNAME_IsAllowsApplyShemaDiscount = "IsAllowsApplyShemaDiscount";
	String COLUMNNAME_MaximumShemaDiscountAllowed = "MaximumShemaDiscountAllowed";
	/**	Main POS Definition	*/
	String COLUMNNAME_DefaultCampaign_ID = "DefaultCampaign_ID";
	String COLUMNNAME_DefaultDiscountCharge_ID = "DefaultDiscountCharge_ID";
	String COLUMNNAME_DisplayCurrency_ID = "DisplayCurrency_ID";
	String COLUMNNAME_IsAllowsConcurrentUse = "IsAllowsConcurrentUse";
	String COLUMNNAME_IsConfidentialInfo = "IsConfidentialInfo";
	String COLUMNNAME_IsConfirmCompleteShipment = "IsConfirmCompleteShipment";
	String COLUMNNAME_IsDisplayDiscount = "IsDisplayDiscount";
	String COLUMNNAME_IsDisplayTaxAmount = "IsDisplayTaxAmount";
	String COLUMNNAME_IsKeepPriceFromCustomer = "IsKeepPriceFromCustomer";
	String COLUMNNAME_IsValidatePOSCashOpening = "IsValidatePOSCashOpening";
	String COLUMNNAME_IsValidatePOSPreviousCash = "IsValidatePOSPreviousCash";
	String COLUMNNAME_POSCashClosingDocumentType_ID = "POSCashClosingDocumentType_ID";
	String COLUMNNAME_POSCollectingDocumentType_ID = "POSCollectingDocumentType_ID";
	String COLUMNNAME_POSDepositDocumentType_ID = "POSDepositDocumentType_ID";
	String COLUMNNAME_POSOpeningDocumentType_ID = "POSOpeningDocumentType_ID";
	String COLUMNNAME_POSRefundDocumentType_ID = "POSRefundDocumentType_ID";
	String COLUMNNAME_POSWithdrawalDocumentType_ID = "POSWithdrawalDocumentType_ID";
	String COLUMNNAME_RefundReferenceCurrency_ID = "RefundReferenceCurrency_ID";
	/**	For Sales Order	*/
	String COLUMNNAME_AssignedSalesRep_ID = "AssignedSalesRep_ID";
	String COLUMNNAME_ECA14_Source_OrderLine_ID = "ECA14_Source_OrderLine_ID";
	String COLUMNNAME_ECA14_Source_Order_ID = "ECA14_Source_Order_ID";
	String COLUMNNAME_ECA14_Source_RMALine_ID = "ECA14_Source_RMALine_ID";
	String COLUMNNAME_ECA14_Source_RMA_ID = "ECA14_Source_RMA_ID";
	/**	For Payment	*/
	String COLUMNNAME_ECA14_Invoice_Reference_ID = "ECA14_Invoice_Reference_ID";
	String COLUMNNAME_ECA14_Reference_Amount = "ECA14_Reference_Amount";
	/**	For Gift Card */
	String COLUMNNAME_ECA14_GiftCard_ID = "ECA14_GiftCard_ID";
	String COLUMNNAME_ECA14_GiftCardLine_ID = "ECA14_GiftCardLine_ID";
	String COLUMNNAME_IsAllowsGiftCard = "IsAllowsGiftCard";
	String COLUMNNAME_ECA14_DefaultGiftCardCharge_ID = "ECA14_DefaultGiftCardCharge_ID";
	String COLUMNNAME_IsPrepayment = "IsPrepayment";
	String COLUMNNAME_DateDoc = "DateDoc";
	String COLUMNNAME_Amount = "Amount";
	String IsMandatoryCreditMemoRef = "IsMandatoryCreditMemoRef";
}
