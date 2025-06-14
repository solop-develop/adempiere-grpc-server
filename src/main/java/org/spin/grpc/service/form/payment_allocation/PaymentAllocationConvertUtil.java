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
package org.spin.grpc.service.form.payment_allocation;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_DocType;
import org.adempiere.core.domains.models.X_T_InvoiceGL;
import org.compiere.model.MCharge;
import org.compiere.model.MConversionType;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MOrg;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.payment_allocation.Charge;
import org.spin.backend.grpc.form.payment_allocation.ConversionType;
import org.spin.backend.grpc.form.payment_allocation.Currency;
import org.spin.backend.grpc.form.payment_allocation.DocumentType;
import org.spin.backend.grpc.form.payment_allocation.Organization;
import org.spin.backend.grpc.form.payment_allocation.TransactionType;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;

public class PaymentAllocationConvertUtil {

	public static Organization.Builder convertOrganization(int organizationId) {
		if (organizationId < 0) {
			return Organization.newBuilder();
		}
		MOrg organization = MOrg.get(Env.getCtx(), organizationId);
		return convertOrganization(organization);
	}
	public static Organization.Builder convertOrganization(MOrg organization) {
		Organization.Builder builder = Organization.newBuilder();
		if (organization == null || organization.getAD_Org_ID() < 0) {
			return builder;
		}

		builder.setId(
				organization.getAD_Org_ID()
			)
			.setUuid(
				StringManager.getValidString(
					organization.getUUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					organization.getName()
				)
			)
			.setName(
				StringManager.getValidString(
					organization.getName()
				)
			)
		;

		return builder;
	}



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
				StringManager.getValidString(
					currency.getUUID()
				)
			)
			.setIsoCode(
				StringManager.getValidString(
					currency.getISO_Code()
				)
			)
			.setName(
				StringManager.getValidString(
					currency.getDescription()
				)
			)
			.setSymbol(
				StringManager.getValidString(
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
		return builder;
	}
	public static ConversionType.Builder conversionType(MConversionType conversionType) {
		ConversionType.Builder builder = ConversionType.newBuilder();
		if (conversionType == null || conversionType.getC_ConversionType_ID() <= 0) {
			return builder;
		}
		builder.setId(
				conversionType.getC_ConversionType_ID()
			)
			.setUuid(
				StringManager.getValidString(
					conversionType.getUUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					conversionType.getValue()
				)
			)
			.setDescription(
				StringManager.getValidString(
					conversionType.getDescription()
				)
			)
			.setIsDefault(
				conversionType.isDefault()
			)
		;
		return builder;
	}



	public static DocumentType.Builder convertDocumentType(int documentTypeId) {
		if (documentTypeId < 0) {
			return DocumentType.newBuilder();
		}
		MDocType documentType = MDocType.get(Env.getCtx(), documentTypeId);
		return convertDocumentType(documentType);
	}
	public static DocumentType.Builder convertDocumentType(MDocType documentType) {
		DocumentType.Builder builder = DocumentType.newBuilder();
		if (documentType == null || documentType.getC_DocType_ID() < 0) {
			return builder;
		}

		builder.setId(
				documentType.getC_DocType_ID()
			)
			.setUuid(
				StringManager.getValidString(
					documentType.getUUID()
				)
			)
			.setName(
				StringManager.getValidString(
					documentType.get_Translation(I_C_DocType.COLUMNNAME_Name)
				)
			)
			.setPrintName(
				StringManager.getValidString(
					documentType.get_Translation(I_C_DocType.COLUMNNAME_PrintName)
				)
			)
			.setDescription(
				StringManager.getValidString(
					documentType.get_Translation(I_C_DocType.COLUMNNAME_Description)
				)
			)
		;

		return builder;
	}



	public static TransactionType.Builder convertTransactionType(String value) {
		if (Util.isEmpty(value, true)) {
			return TransactionType.newBuilder();
		}

		MRefList transactionType = MRefList.get(Env.getCtx(), X_T_InvoiceGL.APAR_AD_Reference_ID, value, null);
		return convertTransactionType(transactionType);
	}
	public static TransactionType.Builder convertTransactionType(MRefList transactionType) {
		TransactionType.Builder builder = TransactionType.newBuilder();
		if (transactionType == null || transactionType.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = transactionType.getName();
		String description = transactionType.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = transactionType.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = transactionType.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(
				transactionType.getAD_Ref_List_ID()
			)
			.setUuid(
				StringManager.getValidString(
					transactionType.getUUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					transactionType.getValue()
				)
			)
			.setName(
				StringManager.getValidString(name)
			)
			.setDescription(
				StringManager.getValidString(description)
			)
		;

		return builder;
	}



	public static Charge.Builder convertCharge(MCharge charge) {
		Charge.Builder builder = Charge.newBuilder();
		if (charge == null || charge.getC_Charge_ID() <= 0) {
			return builder;
		}

		builder.setId(
				charge.getC_Charge_ID()
			)
			.setUuid(
				StringManager.getValidString(
					charge.getUUID()
				)
			)
			.setName(
				StringManager.getValidString(
					charge.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					charge.getDescription()
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					charge.getChargeAmt()
				)
			)
		;

		return builder;
	}

}
