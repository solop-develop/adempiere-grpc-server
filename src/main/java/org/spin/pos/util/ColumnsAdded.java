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
public class ColumnsAdded {
	public static final String COLUMNNAME_ECA14_WriteOffByPercent = "ECA14_WriteOffByPercent";
	public static final String COLUMNNAME_IsAllowsAllocateSeller = "IsAllowsAllocateSeller";
	public static final String COLUMNNAME_IsAllowsApplyDiscount = "IsAllowsApplyDiscount";
	public static final String COLUMNNAME_IsAllowsCashClosing = "IsAllowsCashClosing";
	public static final String COLUMNNAME_IsAllowsCashOpening = "IsAllowsCashOpening";
	public static final String COLUMNNAME_IsAllowsCashWithdrawal = "IsAllowsCashWithdrawal";
	public static final String COLUMNNAME_IsAllowsCollectOrder = "IsAllowsCollectOrder";
	public static final String COLUMNNAME_IsAllowsConfirmShipment = "IsAllowsConfirmShipment";
	public static final String COLUMNNAME_IsAllowsConfirmShipmentByOrder = "IsAllowsConfirmShipmentByOrder";
	public static final String COLUMNNAME_IsAllowsCreateCustomer = "IsAllowsCreateCustomer";
	public static final String COLUMNNAME_IsAllowsCreateOrder = "IsAllowsCreateOrder";
	public static final String COLUMNNAME_IsAllowsDetailCashClosing = "IsAllowsDetailCashClosing";
	public static final String COLUMNNAME_IsAllowsModifyCustomer = "IsAllowsModifyCustomer";
	public static final String COLUMNNAME_IsAllowsModifyDiscount = "IsAllowsModifyDiscount";
	public static final String COLUMNNAME_IsAllowsModifyQuantity = "IsAllowsModifyQuantity";
	public static final String COLUMNNAME_IsAllowsPOSManager = "IsAllowsPOSManager";
	public static final String COLUMNNAME_IsAllowsPreviewDocument = "IsAllowsPreviewDocument";
	public static final String COLUMNNAME_IsAllowsPrintDocument = "IsAllowsPrintDocument";
	public static final String COLUMNNAME_IsAllowsReturnOrder = "IsAllowsReturnOrder";
	public static final String COLUMNNAME_IsModifyPrice = "IsModifyPrice";
	public static final String COLUMNNAME_MaximumDailyRefundAllowed = "MaximumDailyRefundAllowed";
	public static final String COLUMNNAME_MaximumDiscountAllowed = "MaximumDiscountAllowed";
	public static final String COLUMNNAME_MaximumLineDiscountAllowed = "MaximumLineDiscountAllowed";
	public static final String COLUMNNAME_MaximumRefundAllowed = "MaximumRefundAllowed";
	public static final String COLUMNNAME_WriteOffAmtCurrency_ID = "WriteOffAmtCurrency_ID";
	public static final String COLUMNNAME_WriteOffAmtTolerance = "WriteOffAmtTolerance";
	public static final String COLUMNNAME_WriteOffPercentageTolerance = "WriteOffPercentageTolerance";
	public static final String COLUMNNAME_IsAllowsWriteOffAmount = "IsAllowsWriteOffAmount";
	/**	Main POS Definition	*/
	public static final String COLUMNNAME_DefaultCampaign_ID = "DefaultCampaign_ID";
	public static final String COLUMNNAME_DefaultDiscountCharge_ID = "DefaultDiscountCharge_ID";
	public static final String COLUMNNAME_DisplayCurrency_ID = "DisplayCurrency_ID";
	public static final String COLUMNNAME_IsAllowsConcurrentUse = "IsAllowsConcurrentUse";
	public static final String COLUMNNAME_IsConfidentialInfo = "IsConfidentialInfo";
	public static final String COLUMNNAME_IsConfirmCompleteShipment = "IsConfirmCompleteShipment";
	public static final String COLUMNNAME_IsDisplayDiscount = "IsDisplayDiscount";
	public static final String COLUMNNAME_IsDisplayTaxAmount = "IsDisplayTaxAmount";
	public static final String COLUMNNAME_IsKeepPriceFromCustomer = "IsKeepPriceFromCustomer";
	public static final String COLUMNNAME_IsValidatePOSCashOpening = "IsValidatePOSCashOpening";
	public static final String COLUMNNAME_IsValidatePOSPreviousCash = "IsValidatePOSPreviousCash";
	public static final String COLUMNNAME_POSCashClosingDocumentType_ID = "POSCashClosingDocumentType_ID";
	public static final String COLUMNNAME_POSCollectingDocumentType_ID = "POSCollectingDocumentType_ID";
	public static final String COLUMNNAME_POSDepositDocumentType_ID = "POSDepositDocumentType_ID";
	public static final String COLUMNNAME_POSOpeningDocumentType_ID = "POSOpeningDocumentType_ID";
	public static final String COLUMNNAME_POSRefundDocumentType_ID = "POSRefundDocumentType_ID";
	public static final String COLUMNNAME_POSWithdrawalDocumentType_ID = "POSWithdrawalDocumentType_ID";
	public static final String COLUMNNAME_RefundReferenceCurrency_ID = "RefundReferenceCurrency_ID";
	/**	For Sales Order	*/
	public static final String COLUMNNAME_AssignedSalesRep_ID = "AssignedSalesRep_ID";
	public static final String COLUMNNAME_ECA14_Source_OrderLine_ID = "ECA14_Source_OrderLine_ID";
	public static final String COLUMNNAME_ECA14_Source_Order_ID = "ECA14_Source_Order_ID";
	public static final String COLUMNNAME_ECA14_Source_RMALine_ID = "ECA14_Source_RMALine_ID";
	public static final String COLUMNNAME_ECA14_Source_RMA_ID = "ECA14_Source_RMA_ID";
	/**	For Payment	*/
	public static final String COLUMNNAME_ECA14_Invoice_Reference_ID = "ECA14_Invoice_Reference_ID";
	public static final String COLUMNNAME_ECA14_Reference_Amount = "ECA14_Reference_Amount";
}
