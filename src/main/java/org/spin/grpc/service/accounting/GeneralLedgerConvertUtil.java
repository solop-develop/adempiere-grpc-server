/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.grpc.service.accounting;

import org.adempiere.core.domains.models.I_A_Asset_Addition;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_ConversionType;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_Payment;
import org.compiere.model.MConversionRate;
import org.compiere.model.MConversionType;
import org.compiere.model.MCurrency;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.solop.sp032.util.CurrencyConvertDocumentsUtil;
import org.spin.backend.grpc.general_ledger.AccountingDocument;
import org.spin.backend.grpc.general_ledger.ConversionRate;
import org.spin.backend.grpc.general_ledger.ConversionType;
import org.spin.backend.grpc.general_ledger.Currency;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;

/**
 * This class was created for add all convert methods for General Ledger service
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class GeneralLedgerConvertUtil {

	public static Currency.Builder convertCurrency(String isoCode) {
		if (Util.isEmpty(isoCode, true)) {
			return Currency.newBuilder();
		}
		MCurrency currency = MCurrency.get(Env.getCtx(), isoCode);
		return convertCurrency(currency);
	}
	public static Currency.Builder convertCurrency(int currencyId) {
		if (currencyId <= 0) {
			return Currency.newBuilder();
		}
		MCurrency currency = MCurrency.get(Env.getCtx(), currencyId);
		return convertCurrency(currency);
	}
	public static Currency.Builder convertCurrency(MCurrency currency) {
		Currency.Builder builder = Currency.newBuilder();
		if (currency == null || currency.getC_Currency_ID() <= 0) {
			return builder;
		}

		builder.setId(
				currency.getC_Currency_ID()
			)
			.setUuid(
				TextManager.getValidString(
					currency.getUUID()
				)
			)
			.setIsoCode(
				TextManager.getValidString(
					currency.getISO_Code()
				)
			)
			.setName(
				TextManager.getValidString(
					currency.getDescription()
				)
			)
			.setSymbol(
				TextManager.getValidString(
					currency.getCurSymbol()
				)
			)
			.setStandardPrecision(
				currency.getStdPrecision()
			)
		;

		return builder;
	}

	public static ConversionType.Builder convertConversionType(int conversionTypeId) {
		ConversionType.Builder builder = ConversionType.newBuilder();
		if (conversionTypeId <= 0) {
			return builder;
		}
		MConversionType conversionType = new Query(
			Env.getCtx(),
			I_C_ConversionType.Table_Name,
			"C_ConversionType_ID = ?",
			null
		)
			.setParameters(conversionTypeId)
			.first()
		;
		builder = convertConversionType(conversionType);
		return builder;
	}
	public static ConversionType.Builder convertConversionType(MConversionType conversionType) {
		ConversionType.Builder builder = ConversionType.newBuilder();
		if (conversionType == null || conversionType.getC_ConversionType_ID() <= 0) {
			return builder;
		}
		builder.setId(
				conversionType.getC_ConversionType_ID()
			)
			.setUuid(
				TextManager.getValidString(
					conversionType.getUUID()
				)
			)
			.setValue(
				TextManager.getValidString(
					conversionType.getValue()
				)
			)
			.setName(
				TextManager.getValidString(
					conversionType.getName()
				)
			)
			.setDescription(
				TextManager.getValidString(
					conversionType.getDescription()
				)
			)
			.setIsDefault(
				conversionType.isDefault()
			)
			.setParentId(
				conversionType.get_ValueAsInt(
					CurrencyConvertDocumentsUtil.COLUMNNAME_SP032_ParentCType_ID
				)
			)
			.setBusinessPartnerId(
				conversionType.get_ValueAsInt(
					I_C_BPartner.COLUMNNAME_C_BPartner_ID
				)
			)
			.setOrderId(
				conversionType.get_ValueAsInt(
					I_C_Order.COLUMNNAME_C_Order_ID
				)
			)
			.setInvoiceId(
				conversionType.get_ValueAsInt(
					I_C_Invoice.COLUMNNAME_C_Invoice_ID
				)
			)
			.setPaymentId(
				conversionType.get_ValueAsInt(
					I_C_Payment.COLUMNNAME_C_Payment_ID
				)
			)
			.setAssetAdditionId(
				conversionType.get_ValueAsInt(
					I_A_Asset_Addition.COLUMNNAME_A_Asset_Addition_ID
				)
			)
			.setExpedientId(
				conversionType.get_ValueAsInt(
					CurrencyConvertDocumentsUtil.ColumnName_SP009_Expedient_ID
				)
			)
		;
		return builder;
	}



	public static ConversionRate.Builder convertConversionRate(MConversionRate conversionRate) {
		ConversionRate.Builder builder = ConversionRate.newBuilder();
		if (conversionRate == null || conversionRate.getC_Conversion_Rate_ID() <= 0) {
			return builder;
		}
		builder.setId(
				conversionRate.getC_Conversion_Rate_ID()
			)
			.setUuid(
				TextManager.getValidString(
					conversionRate.getUUID()
				)
			)
			.setOrganizationId(
				conversionRate.getAD_Org_ID()
			)
			.setCurrencyFrom(
				convertCurrency(
					conversionRate.getC_Currency_ID()
				)
			)
			.setCurrencyTo(
				convertCurrency(
					conversionRate.getC_Currency_ID_To()
				)
			)
			.setMultiplyRate(
				NumberManager.getBigDecimalToString(
					conversionRate.getMultiplyRate()
				)
			)
			.setDivideRate(
				NumberManager.getBigDecimalToString(
					conversionRate.getDivideRate()
				)
			)
			.setConversionType(
				convertConversionType(
					conversionRate.getC_ConversionType_ID()
				)
			)
		;
		return builder;
	}


	public static AccountingDocument.Builder convertAccountingDocument(int tableId) {
		AccountingDocument.Builder builder = AccountingDocument.newBuilder();
		if (tableId <= 0) {
			return builder;
		}
		MTable table = MTable.get(Env.getCtx(), tableId);
		builder = convertAccountingDocument(table);
		return builder;
	}
	public static AccountingDocument.Builder convertAccountingDocument(MTable table) {
		AccountingDocument.Builder builder = AccountingDocument.newBuilder();
		if (table == null || table.getAD_Table_ID() <= 0) {
			return builder;
		}

		builder.setId(
				table.getAD_Table_ID()
			)
			.setName(
				TextManager.getValidString(
					table.getName()
				)
			)
			.setTableName(
				TextManager.getValidString(
					table.getTableName()
				)
			)
		;

		return builder;
	}

}
