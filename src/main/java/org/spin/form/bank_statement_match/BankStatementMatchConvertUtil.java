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
import org.adempiere.core.domains.models.X_C_BankStatement;
import org.adempiere.core.domains.models.X_C_Payment;
import org.adempiere.core.domains.models.X_I_BankStatement;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MCurrency;
import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.bank_statement_match.BankAccount;
import org.spin.backend.grpc.form.bank_statement_match.BankStatement;
import org.spin.backend.grpc.form.bank_statement_match.BusinessPartner;
import org.spin.backend.grpc.form.bank_statement_match.Currency;
import org.spin.backend.grpc.form.bank_statement_match.ImportedBankMovement;
import org.spin.backend.grpc.form.bank_statement_match.MatchingMovement;
import org.spin.backend.grpc.form.bank_statement_match.Payment;
import org.spin.backend.grpc.form.bank_statement_match.ResultMovement;
import org.spin.backend.grpc.form.bank_statement_match.TenderType;
import org.spin.service.grpc.util.ValueManager;

/**
 * This class was created for add all convert methods for Issue Management form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class BankStatementMatchConvertUtil {

	public static BankAccount.Builder convertBankAccount(int bankAccountId) {
		if (bankAccountId > 0) {
			MBankAccount bankAccount = MBankAccount.get(Env.getCtx(), bankAccountId);
			return convertBankAccount(bankAccount);
		}
		return BankAccount.newBuilder();
	}
	public static BankAccount.Builder convertBankAccount(MBankAccount bankAccount) {
		BankAccount.Builder builder = BankAccount.newBuilder();
		if (bankAccount == null || bankAccount.getC_BankAccount_ID() <= 0) {
			return builder;
		}
		
		MBank bank = MBank.get(Env.getCtx(), bankAccount.getC_Bank_ID());

		String accountNo = ValueManager.validateNull(bankAccount.getAccountNo());
		int accountNoLength = accountNo.length();
		if (accountNoLength > 4) {
			accountNo = accountNo.substring(accountNoLength - 4);
		}
		accountNo = String.format("%1$" + 20 + "s", accountNo).replace(" ", "*");

		Currency.Builder currencyBuilder = convertCurrency(bankAccount.getC_Currency_ID());
		builder.setId(bankAccount.getC_BankAccount_ID())
			.setAccountNo(accountNo)
			.setAccountName(
				ValueManager.validateNull(bankAccount.getName())
			)
			.setBankName(
				ValueManager.validateNull(bank.getName())
			)
			.setCurrency(
				currencyBuilder
			)
			.setCurrentBalance(
				ValueManager.getValueFromBigDecimal(
					bankAccount.getCurrentBalance()
				)
			)
		;

		return builder;
	}

	public static BankStatement.Builder convertBankStatement(int bankStatementId) {
		BankStatement.Builder builder = BankStatement.newBuilder();
		if (bankStatementId <= 0) {
			return builder;
		}
		MBankStatement bankStatement = new MBankStatement(Env.getCtx(), bankStatementId, null);
		return convertBankStatement(bankStatement);
	}

	public static BankStatement.Builder convertBankStatement(MBankStatement bankStatement) {
		BankStatement.Builder builder = BankStatement.newBuilder();
		if (bankStatement == null || bankStatement.getC_BankStatement_ID() <= 0) {
			return builder;
		}

		String documentSatusName = MRefList.getListName(
			Env.getCtx(),
			X_C_BankStatement.DOCSTATUS_AD_Reference_ID,
			bankStatement.getDocStatus()
		);

		builder.setId(bankStatement.getC_BankStatement_ID())
			.setBankAccount(
				convertBankAccount(
					bankStatement.getC_BankAccount_ID()
				)
			)
			.setDocumentNo(
				ValueManager.validateNull(
					bankStatement.getDocumentNo()
				)
			)
			.setName(
				ValueManager.validateNull(
					bankStatement.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					bankStatement.getDescription()
				)
			)
			.setDocumentStatus(
				ValueManager.validateNull(
					documentSatusName
				)
			)
			.setStatementDate(
				ValueManager.getTimestampFromDate(
					bankStatement.getStatementDate()
				)
			)
			.setIsManual(bankStatement.isManual())
			.setIsProcessed(bankStatement.isProcessed())
			.setBeginningBalance(
				ValueManager.getValueFromBigDecimal(
					bankStatement.getBeginningBalance()
				)
			)
			.setStatementDifference(
				ValueManager.getValueFromBigDecimal(
					bankStatement.getStatementDifference()
				)
			)
			.setEndingBalance(
				ValueManager.getValueFromBigDecimal(
					bankStatement.getEndingBalance()
				)
			)
		;

		return builder;
	}



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
			.setValue(
				ValueManager.validateNull(businessPartner.getValue())
			)
			.setTaxId(
				ValueManager.validateNull(businessPartner.getTaxID())
			)
			.setName(
				ValueManager.validateNull(businessPartner.getName())
			)
			.setDescription(
				ValueManager.validateNull(businessPartner.getDescription())
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
			.setIsoCode(
				ValueManager.validateNull(currency.getISO_Code())
			)
			.setDescription(
				ValueManager.validateNull(currency.getDescription())
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
			.setValue(
				ValueManager.validateNull(tenderType.getValue())
			)
			.setName(
				ValueManager.validateNull(name)
			)
			.setDescription(
				ValueManager.validateNull(description)
			)
		;

		return builder;
	}

	public static Payment.Builder convertPayment(MPayment payment) {
		Payment.Builder builder = Payment.newBuilder();
		if (payment == null) {
			return builder;
		}

		BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
			payment.getC_BPartner_ID()
		);

		Currency.Builder currencyBuilder = convertCurrency(
			payment.getC_Currency_ID()
		);

		TenderType.Builder tenderTypeBuilder = convertTenderType(
			payment.getTenderType()
		);
		boolean isReceipt = payment.isReceipt();
		BigDecimal paymentAmount = payment.getPayAmt();
		if (!isReceipt) {
			paymentAmount = paymentAmount.negate();
		}

		builder.setId(payment.getC_Payment_ID())
			.setTransactionDate(
				ValueManager.getTimestampFromDate(
					payment.getDateTrx()
				)
			)
			.setIsReceipt(isReceipt)
			.setDocumentNo(
				ValueManager.validateNull(
					payment.getDocumentNo()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					payment.getDescription()
				)
			)
			.setAmount(
				ValueManager.getValueFromBigDecimal(
					paymentAmount
				)
			)
			.setBusinessPartner(businessPartnerBuilder)
			.setTenderType(tenderTypeBuilder)
			.setCurrency(currencyBuilder)
		;

		return builder;
	}


	public static ImportedBankMovement.Builder convertImportedBankMovement(X_I_BankStatement bankStatemet) {
		ImportedBankMovement.Builder builder = ImportedBankMovement.newBuilder();
		if (bankStatemet == null || bankStatemet.getI_BankStatement_ID() <= 0) {
			return builder;
		}

		builder.setId(bankStatemet.getI_BankStatement_ID())
			.setReferenceNo(
				ValueManager.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setIsReceipt(
				bankStatemet.getTrxAmt().compareTo(BigDecimal.ZERO) < 0
			)
			.setReferenceNo(
				ValueManager.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setMemo(
				ValueManager.validateNull(
					bankStatemet.getMemo()
				)
			)
			.setTransactionDate(
				ValueManager.getTimestampFromDate(
					bankStatemet.getStatementLineDate()
				)
			)
			.setAmount(
				ValueManager.getValueFromBigDecimal(
					bankStatemet.getTrxAmt()
				)
			)
			.setBankStatementLineId(bankStatemet.getC_BankStatementLine_ID())
		;

		BusinessPartner.Builder businessPartnerBuilder = BankStatementMatchConvertUtil.convertBusinessPartner(
			bankStatemet.getC_BPartner_ID()
		);
		if (bankStatemet.getC_BPartner_ID() <= 0) {
			businessPartnerBuilder.setName(
				ValueManager.validateNull(
					bankStatemet.getBPartnerValue()
				)
			).setValue(
				ValueManager.validateNull(
					bankStatemet.getBPartnerValue()
				)
			);
		}
		builder.setBusinessPartner(businessPartnerBuilder);

		Currency.Builder currencyBuilder = Currency.newBuilder();
		if (bankStatemet.getC_Currency_ID() > 0) {
			currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				bankStatemet.getC_Currency_ID()
			);
		} else {
 			currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				bankStatemet.getISO_Code()
			);
		}
		builder.setCurrency(currencyBuilder);

		return builder;
	}


	public static MatchingMovement.Builder convertMatchMovement(X_I_BankStatement bankStatemet) {
		MatchingMovement.Builder builder = MatchingMovement.newBuilder();
		if (bankStatemet == null || bankStatemet.getI_BankStatement_ID() <= 0) {
			return builder;
		}

		builder.setId(bankStatemet.getI_BankStatement_ID())
			.setReferenceNo(
				ValueManager.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					bankStatemet.getDescription()
				)
			)
			.setIsReceipt(
				bankStatemet.getTrxAmt().compareTo(BigDecimal.ZERO) < 0
			)
			.setMemo(
				ValueManager.validateNull(
					bankStatemet.getMemo()
				)
			)
			.setTransactionDate(
				ValueManager.getTimestampFromDate(
					bankStatemet.getStatementLineDate()
				)
			)
			.setBusinessPartner(
				convertBusinessPartner(
					bankStatemet.getC_BPartner_ID()
				)
			)
			.setAmount(
				ValueManager.getValueFromBigDecimal(
					bankStatemet.getTrxAmt()
				)
			)
			.setIsAutomatic(
				ValueManager.stringToBoolean(
					bankStatemet.getEftMemo()
				)
			)
		;

		if (bankStatemet.getC_Payment_ID() > 0) {
			MPayment payment = new MPayment(Env.getCtx(), bankStatemet.getC_Payment_ID(), null);
			builder.setPaymentId(payment.getC_Payment_ID())
				.setDocumentNo(
					ValueManager.validateNull(
						payment.getDocumentNo()
					)
				)
				.setPaymentAmount(
					ValueManager.getValueFromBigDecimal(
						payment.getPayAmt()
					)
				)
				.setPaymentDate(
					ValueManager.getTimestampFromDate(
						payment.getDateTrx()
					)
				)
			;
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


	public static ResultMovement.Builder convertResultMovement(X_I_BankStatement bankStatemet) {
		ResultMovement.Builder builder = ResultMovement.newBuilder();
		if (bankStatemet == null || bankStatemet.getI_BankStatement_ID() <= 0) {
			return builder;
		}

		builder.setId(bankStatemet.getI_BankStatement_ID())
			.setReferenceNo(
				ValueManager.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setIsReceipt(
				bankStatemet.getTrxAmt().compareTo(BigDecimal.ZERO) < 0
			)
			.setReferenceNo(
				ValueManager.validateNull(
					bankStatemet.getReferenceNo()
				)
			)
			.setMemo(
				ValueManager.validateNull(
					bankStatemet.getMemo()
				)
			)
			.setTransactionDate(
				ValueManager.getTimestampFromDate(
					bankStatemet.getStatementLineDate()
				)
			)
			.setAmount(
				ValueManager.getValueFromBigDecimal(
					bankStatemet.getTrxAmt()
				)
			)
			.setIsAutomatic(
				ValueManager.stringToBoolean(
					bankStatemet.getEftMemo()
				)
			)
		;

		BusinessPartner.Builder businessPartnerBuilder = BankStatementMatchConvertUtil.convertBusinessPartner(
			bankStatemet.getC_BPartner_ID()
		);
		if (bankStatemet.getC_BPartner_ID() <= 0) {
			businessPartnerBuilder.setName(
				ValueManager.validateNull(
					bankStatemet.getBPartnerValue()
				)
			).setValue(
				ValueManager.validateNull(
					bankStatemet.getBPartnerValue()
				)
			);
		}
		builder.setBusinessPartner(businessPartnerBuilder);

		Currency.Builder currencyBuilder = Currency.newBuilder();
		if (bankStatemet.getC_Currency_ID() > 0) {
			currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				bankStatemet.getC_Currency_ID()
			);
		} else {
 			currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				bankStatemet.getISO_Code()
			);
		}
		builder.setCurrency(currencyBuilder);

		if (bankStatemet.getC_Payment_ID() > 0) {
			MPayment payment = new MPayment(Env.getCtx(), bankStatemet.getC_Payment_ID(), null);
			builder.setPaymentId(payment.getC_Payment_ID())
				.setDocumentNo(
					ValueManager.validateNull(
						payment.getDocumentNo()
					)
				)
				.setPaymentAmount(
					ValueManager.getValueFromBigDecimal(
						payment.getPayAmt()
					)
				)
				.setPaymentDate(
					ValueManager.getTimestampFromDate(
						payment.getDateTrx()
					)
				)
			;
			TenderType.Builder tenderTypeBuilder = convertTenderType(
				payment.getTenderType()
			);
			builder.setTenderType(tenderTypeBuilder);

			if (builder.getBusinessPartner().getId() <= 0) {
				businessPartnerBuilder = convertBusinessPartner(
					payment.getC_BPartner_ID()
				);
			}

			if (builder.getCurrency().getId() <= 0) {
				currencyBuilder = convertCurrency(
					payment.getC_Currency_ID()
				);
			}
		}

		builder.setBusinessPartner(businessPartnerBuilder)
			.setCurrency(currencyBuilder);

		return builder;
	}

}
