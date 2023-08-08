/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.spin.form.bank_statement_match;

import java.math.BigDecimal;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.X_C_Payment;
import org.adempiere.core.domains.models.X_I_BankStatement;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.bank_statement_match.BusinessPartner;
import org.spin.backend.grpc.form.bank_statement_match.Currency;
import org.spin.backend.grpc.form.bank_statement_match.ImportedBankMovement;
import org.spin.backend.grpc.form.bank_statement_match.MatchingMovement;
import org.spin.backend.grpc.form.bank_statement_match.TenderType;
import org.spin.base.util.ValueUtil;

/**
 * This class was created for add all convert methods for Issue Management form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class BankStatementMatchConvertUtil {

	public static BusinessPartner.Builder convertBusinessPartner(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			return BusinessPartner.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertBusinessPartner(businessPartner);
	}
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}

		builder.setId(businessPartner.getC_BPartner_ID())
			.setUuid(
				ValueUtil.validateNull(businessPartner.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(businessPartner.getValue())
			)
			.setTaxId(
				ValueUtil.validateNull(businessPartner.getTaxID())
			)
			.setName(
				ValueUtil.validateNull(businessPartner.getName())
			)
			.setDescription(
				ValueUtil.validateNull(businessPartner.getDescription())
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

		builder.setId(currency.getC_Currency_ID())
			.setUuid(
				ValueUtil.validateNull(currency.getUUID())
			)
			.setIsoCode(
				ValueUtil.validateNull(currency.getISO_Code())
			)
			.setDescription(
				ValueUtil.validateNull(currency.getDescription())
			)
		;

		return builder;
	}



	public static TenderType.Builder convertTenderType(String value) {
		if (Util.isEmpty(value, false)) {
			return TenderType.newBuilder();
		}

		MRefList tenderType = MRefList.get(Env.getCtx(), X_C_Payment.TENDERTYPE_AD_Reference_ID, value, null);
		return convertTenderType(tenderType);
	}
	public static TenderType.Builder convertTenderType(MRefList tenderType) {
		TenderType.Builder builder = TenderType.newBuilder();
		if (tenderType == null || tenderType.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = tenderType.getName();
		String description = tenderType.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = tenderType.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = tenderType.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(tenderType.getAD_Ref_List_ID())
			.setUuid(ValueUtil.validateNull(tenderType.getUUID()))
			.setValue(ValueUtil.validateNull(tenderType.getValue()))
			.setName(
				ValueUtil.validateNull(name)
			)
			.setDescription(
				ValueUtil.validateNull(description)
			)
		;

		return builder;
	}


	public static ImportedBankMovement.Builder convertImportedBankMovement(X_I_BankStatement bankStatemet) {
		ImportedBankMovement.Builder builder = ImportedBankMovement.newBuilder();
		if (bankStatemet == null || bankStatemet.getI_BankStatement_ID() <= 0) {
			return builder;
		}

		builder.setId(bankStatemet.getI_BankStatement_ID())
			.setUuid(
				ValueUtil.validateNull(
					bankStatemet.getUUID()
				)
			)
			.setReferenceNo(
				ValueUtil.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setIsReceipt(
				bankStatemet.getTrxAmt().compareTo(BigDecimal.ZERO) < 0
			)
			.setReferenceNo(
				ValueUtil.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setMemo(
				ValueUtil.validateNull(
					bankStatemet.getMemo()
				)
			)
			.setTransactionDate(
				ValueUtil.getLongFromTimestamp(
					bankStatemet.getStatementLineDate()
				)
			)
			.setAmount(
				ValueUtil.getDecimalFromBigDecimal(
					bankStatemet.getTrxAmt()
				)
			)
		;

		BusinessPartner.Builder businessPartnerBuilder = BankStatementMatchConvertUtil.convertBusinessPartner(
			bankStatemet.getC_BPartner_ID()
		);
		if (bankStatemet.getC_BPartner_ID() <= 0) {
			businessPartnerBuilder.setName(
				ValueUtil.validateNull(
					bankStatemet.getBPartnerValue()
				)
			).setValue(
				ValueUtil.validateNull(
					bankStatemet.getBPartnerValue()
				)
			);
		}
		builder.setBusinessPartner(businessPartnerBuilder);

		Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
			bankStatemet.getISO_Code()
		);
		builder.setCurrency(currencyBuilder);

		return builder;
	}


	public static MatchingMovement.Builder convertMatchMovement(X_I_BankStatement bankStatemet) {
		MatchingMovement.Builder builder = MatchingMovement.newBuilder();
		if (bankStatemet == null || bankStatemet.getI_BankStatement_ID() <= 0) {
			return builder;
		}

		builder.setId(bankStatemet.getI_BankStatement_ID())
			.setUuid(
				ValueUtil.validateNull(
					bankStatemet.getUUID()
				)
			)
			.setReferenceNo(
				ValueUtil.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setDescription(
				ValueUtil.validateNull(
					bankStatemet.getDescription()
				)
			)
			.setIsReceipt(
				bankStatemet.getTrxAmt().compareTo(BigDecimal.ZERO) < 0
			)
			.setMemo(
				ValueUtil.validateNull(
					bankStatemet.getMemo()
				)
			)
			.setTransactionDate(
				ValueUtil.getLongFromTimestamp(
					bankStatemet.getStatementLineDate()
				)
			)
			.setBusinessPartner(
				convertBusinessPartner(
					bankStatemet.getC_BPartner_ID()
				)
			)
		;

		if (bankStatemet.getC_Payment_ID() > 0) {
			MPayment payment = new MPayment(Env.getCtx(), bankStatemet.getC_Payment_ID(), null);
			builder.setDocumentNo(
				ValueUtil.validateNull(
					payment.getDocumentNo()
				)
			);
			TenderType.Builder tenderTypeBuilder = convertTenderType(
				payment.getTenderType()
			);
			builder.setTenderType(tenderTypeBuilder);

			if (builder.getBusinessPartner().getId() <= 0) {
				BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
					payment.getC_BPartner_ID()
				);
				builder.setBusinessPartner(businessPartnerBuilder);
			}
			Currency.Builder currencyBuilder = convertCurrency(
				payment.getC_Currency_ID()
			);
			builder.setCurrency(currencyBuilder);
		}

		return builder;
	}

}
