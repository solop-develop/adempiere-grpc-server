/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.pos.service.pos;

import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_C_ConversionType;
import org.compiere.model.MBankAccount;
import org.compiere.model.MPOS;
import org.compiere.model.MPriceList;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.util.Env;
import org.spin.backend.grpc.pos.PointOfSales;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.service.customer.CustomerConvertUtil;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;

public class POSConvertUtil {

	/**
	 * Convert POS
	 * @param pos
	 * @return
	 */
	public static PointOfSales.Builder convertPointOfSales(MPOS pos) {
		int userId = Env.getAD_User_ID(pos.getCtx());

		PointOfSales.Builder builder = PointOfSales.newBuilder()
			.setId(
				pos.getC_POS_ID()
			)
			.setName(
				TextManager.getValidString(
					pos.getName()
				)
			)
			.setDescription(
				TextManager.getValidString(
					pos.getDescription()
				)
			)
			.setHelp(
				TextManager.getValidString(
					pos.getHelp()
				)
			)
			.setIsModifyPrice(
				pos.isModifyPrice()
			)
			.setIsPosRequiredPin(
				pos.isPOSRequiredPIN()
			)
			.setSalesRepresentative(
				CoreFunctionalityConvert.convertSalesRepresentative(
					MUser.get(pos.getCtx(), pos.getSalesRep_ID())
				)
			)
			.setTemplateCustomer(
				CustomerConvertUtil.convertCustomer(
					pos.getBPartner()
				)
			)
			.setKeyLayoutId(
				pos.getC_POSKeyLayout_ID()
			)
			.setIsAisleSeller(
				// pos.get_ValueAsBoolean("IsAisleSeller")
				AccessManagement.getBooleanValueFromPOS(pos, userId, "IsAisleSeller")

			)
			.setIsSharedPos(
				pos.get_ValueAsBoolean("IsSharedPOS")
			)
			.setConversionTypeId(
				pos.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID)
			)
			.setIsDirectPrint(
				pos.get_ValueAsBoolean(I_AD_Process.COLUMNNAME_IsDirectPrint)
			)
		;

		// Write Off
		builder.setIsAllowsWriteOffAmount(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsWriteOffAmount)
			)
			.setWriteOffAmountTolerance(
				NumberManager.getBigDecimalToString(
					AccessManagement.getWriteOffAmtTolerance(pos)
				)
			)
			.setIsWriteOffByPercent(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_ECA14_WriteOffByPercent)
			)
			.setWriteOffPercentageTolerance(
				NumberManager.getBigDecimalToString(
					AccessManagement.getBigDecimalValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_WriteOffPercentageTolerance)
				)
			)
		;

		// Discount
		builder
			.setIsDisplayDiscount(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsDisplayDiscount)
			)
			.setIsAllowsApplyDiscount(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsApplyDiscount)
			)
			.setMaximumDiscountAllowed(
				NumberManager.getBigDecimalToString(
					AccessManagement.getBigDecimalValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_MaximumDiscountAllowed)
				)
			)
			.setIsAllowsModifyDiscount(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsModifyDiscount)
			)
			.setMaximumLineDiscountAllowed(
				NumberManager.getBigDecimalToString(
					AccessManagement.getBigDecimalValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_MaximumLineDiscountAllowed)
				)
			)
			.setIsAllowsApplySchemaDiscount(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsApplyShemaDiscount)
			)
			.setMaximumSchemaDiscountAllowed(
				NumberManager.getBigDecimalToString(
					AccessManagement.getBigDecimalValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_MaximumShemaDiscountAllowed)
				)
			)
		;

		// Collect / Refund
		builder
			.setIsAllowsCollectOrder(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCollectOrder)
			)
			.setIsPrintCollect(
				AccessManagement.getBooleanValueFromPOS(pos, userId, "IsPrintCollect")
			)
			.setIsAllowsOpenAmount(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsOpenAmount)
			)
			.setMaximumOpenAmount(
				NumberManager.getBigDecimalToString(
					AccessManagement.getBigDecimalValueFromPOS(pos, userId, "MaximumOpenAmount")
				)
			)
			// TODO: Add flag column `IsAllowsRefund` to pin accesss
			.setMaximumRefundAllowed(
				NumberManager.getBigDecimalToString(
					AccessManagement.getBigDecimalValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_MaximumRefundAllowed)
				)
			)
			.setMaximumDailyRefundAllowed(
				NumberManager.getBigDecimalToString(
					AccessManagement.getBigDecimalValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_MaximumDailyRefundAllowed)
				)
			)
		;
		if(pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_RefundReferenceCurrency_ID) > 0) {
			builder.setRefundReferenceCurrency(
				CoreFunctionalityConvert.convertCurrency(
					pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_RefundReferenceCurrency_ID)
				)
			);
		}
		// if(pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_CollectingAgent_ID) > 0) {
		// 	builder.setCollectingAgent(
		// 		CoreFunctionalityConvert.convertSalesRepresentative(
		// 			MUser.get(pos.getCtx(), pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_CollectingAgent_ID)
		// 		)
		// 	);
		// }

		//	Special values
		builder
			.setIsAllowsModifyQuantity(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsModifyQuantity)
			)
			.setIsAllowsReturnOrder(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsReturnOrder)
			)
			.setIsAllowsCreateOrder(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCreateOrder)
			)
			.setIsDisplayTaxAmount(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsDisplayTaxAmount)
			)
			.setIsAllowsConfirmShipment(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsConfirmShipment)
			)
			.setIsConfirmCompleteShipment(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsConfirmCompleteShipment)
			)
			.setIsAllowsAllocateSeller(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsAllocateSeller)
			)
			.setIsAllowsConcurrentUse(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsConcurrentUse)
			)
			.setIsAllowsCashOpening(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCashOpening)
			)
			.setIsAllowsCashClosing(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCashClosing)
			)
			.setIsAllowsCashWithdrawal(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCashWithdrawal)
			)
			.setIsAllowsCreateCustomer(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCreateCustomer)
			)
			.setIsAllowsModifyCustomer(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsModifyCustomer)
			)
			.setIsAllowsPrintDocument(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsPrintDocument)
			)
			.setIsAllowsPreviewDocument(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsPreviewDocument)
			)
			.setIsKeepPriceFromCustomer(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsKeepPriceFromCustomer)
			)
			.setIsModifyPrice(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsModifyPrice)
			)
			.setIsAllowsDetailCashClosing(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsDetailCashClosing)
			)
			.setIsAllowsCustomerTemplate(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCustomerTemplate)
			)
			.setIsAllowsGiftCard(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsGiftCard)
			)
			.setDefaultGiftCardChargeId(
				pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_DefaultGiftCardCharge_ID)
			)
			.setIsAllowsCreateManualDocument(
				AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsCreateManualDocument)
			)
		;

		//	Set Price List and currency
		if(pos.getM_PriceList_ID() != 0) {
			MPriceList priceList = MPriceList.get(Env.getCtx(), pos.getM_PriceList_ID(), null);
			builder.setPriceList(CoreFunctionalityConvert.convertPriceList(priceList));
		}
		//	Bank Account
		if(pos.getC_BankAccount_ID() != 0) {
			MBankAccount cashAccount = MBankAccount.get(Env.getCtx(), pos.getC_BankAccount_ID());
			builder.setDefaultOpeningChargeId(
					cashAccount.get_ValueAsInt("DefaultOpeningCharge_ID")
				)
				.setDefaultWithdrawalChargeId(
					cashAccount.get_ValueAsInt("DefaultWithdrawalCharge_ID")
				)
				.setCashBankAccount(
					CoreFunctionalityConvert.convertBankAccount(cashAccount)
				)
			;
		}
		//	Bank Account to transfer
		if(pos.getCashTransferBankAccount_ID() != 0) {
			builder.setCashTransferBankAccount(
				CoreFunctionalityConvert.convertBankAccount(
					pos.getCashTransferBankAccount_ID()
				)
			);
		}
		//	Warehouse
		if(pos.getM_Warehouse_ID() > 0) {
			MWarehouse warehouse = MWarehouse.get(Env.getCtx(), pos.getM_Warehouse_ID());
			builder.setWarehouse(
				CoreFunctionalityConvert.convertWarehouse(warehouse)
			);
		}
		//	Price List
		if(pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DisplayCurrency_ID) > 0) {
			builder.setDisplayCurrency(CoreFunctionalityConvert.convertCurrency(
				pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DisplayCurrency_ID))
			);
		}
		//	Document Type
		if(pos.getC_DocType_ID() > 0) {
			builder.setDocumentType(
				CoreFunctionalityConvert.convertDocumentType(
					pos.getC_DocType_ID()
				)
			);
		}
		//	Return Document Type
		if(pos.get_ValueAsInt("C_DocTypeRMA_ID") > 0) {
			builder.setReturnDocumentType(
				CoreFunctionalityConvert.convertDocumentType(
					pos.get_ValueAsInt("C_DocTypeRMA_ID")
				)
			);
		}
		// Campaign
		if (pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultCampaign_ID) > 0) {
			builder.setDefaultCampaign(
				org.spin.pos.util.POSConvertUtil.convertCampaign(
					pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_DefaultCampaign_ID)
				)
			);
		}
		
		return builder;
	}

}
