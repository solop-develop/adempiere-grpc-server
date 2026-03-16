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
package org.spin.grpc.service.form.bank_statement_match;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.X_C_BankStatement;
import org.adempiere.core.domains.models.X_C_Payment;
import org.adempiere.core.domains.models.X_I_BankStatement;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MCurrency;
import org.compiere.model.MOrg;
import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.bank_statement_match.Bank;
import org.spin.backend.grpc.form.bank_statement_match.BankAccount;
import org.spin.backend.grpc.form.bank_statement_match.BankStatement;
import org.spin.backend.grpc.form.bank_statement_match.BusinessPartner;
import org.spin.backend.grpc.form.bank_statement_match.Currency;
import org.spin.backend.grpc.form.bank_statement_match.ImportedBankMovement;
import org.spin.backend.grpc.form.bank_statement_match.MatchingMovement;
import org.spin.backend.grpc.form.bank_statement_match.Payment;
import org.spin.backend.grpc.form.bank_statement_match.ResultMovement;
import org.spin.backend.grpc.form.bank_statement_match.TenderType;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

/**
 * This class was created for add all convert methods for Issue Management form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class BankStatementMatchConvertUtil {

	public static Bank.Builder convertBank(int bankId) {
		if (bankId > 0) {
			MBank bankAccount = MBank.get(Env.getCtx(), bankId);
			return convertBank(bankAccount);
		}
		return Bank.newBuilder();
	}
	public static Bank.Builder convertBank(MBank bank) {
		Bank.Builder builder = Bank.newBuilder();
		if (bank == null) {
			return builder;
		}
		builder.setId(
				bank.getC_Bank_ID()
			)
			.setUuid(
				TextManager.getValidString(
					bank.getUUID()
				)
			)
			.setName(
				TextManager.getValidString(
					bank.getName()
				)
			)
			.setDisplayValue(
				TextManager.getValidString(
					bank.getDisplayValue()
				)
			)
			.setRoutingNo(
				TextManager.getValidString(
					bank.getRoutingNo()
				)
			)
			.setSwiftCode(
				TextManager.getValidString(
					bank.getSwiftCode()
				)
			)
		;
		return builder;
	}



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

		String accountNo = TextManager.getValidString(
			bankAccount.getAccountNo()
		);
		int accountNoLength = accountNo.length();
		if (accountNoLength > 4) {
			accountNo = accountNo.substring(accountNoLength - 4);
		}
		accountNo = String.format("%1$" + 20 + "s", accountNo).replace(" ", "*");

		Currency.Builder currencyBuilder = convertCurrency(bankAccount.getC_Currency_ID());
		builder.setId(
				bankAccount.getC_BankAccount_ID()
			)
			.setUuid(
				TextManager.getValidString(
					bankAccount.getUUID()
				)
			)
			.setAccountNo(
				TextManager.getValidString(
					bankAccount.getAccountNo()
				)
			)
			.setAccountNoMask(
				TextManager.getValidString(
					accountNo
				)
			)
			.setName(
				TextManager.getValidString(
					bankAccount.getName()
				)
			)
			.setDisplayValue(
				TextManager.getValidString(
					bankAccount.getDisplayValue()
				)
			)
			.setBank(
				convertBank(
					bankAccount.getC_Bank_ID()
				)
			)
			.setCurrency(
				currencyBuilder
			)
			.setCurrentBalance(
				NumberManager.getBigDecimalToString(
					bankAccount.getCurrentBalance()
				)
			)
			.setOrganizationName(
				TextManager.getValidString(
					MOrg.get(Env.getCtx(), bankAccount.getAD_Org_ID()).getName()
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

		builder.setId(
				bankStatement.getC_BankStatement_ID()
			)
			.setUuid(
				TextManager.getValidString(
					bankStatement.getUUID()
				)
			)
			.setDocumentNo(
				TextManager.getValidString(
					bankStatement.getDocumentNo()
				)
			)
			.setDisplayValue(
				TextManager.getValidString(
					bankStatement.getDisplayValue()
				)
			)
			.setName(
				TextManager.getValidString(
					bankStatement.getName()
				)
			)
			.setBankAccount(
				convertBankAccount(
					bankStatement.getC_BankAccount_ID()
				)
			)
			.setDescription(
				TextManager.getValidString(
					bankStatement.getDescription()
				)
			)
			.setDocumentStatus(
				TextManager.getValidString(
					documentSatusName
				)
			)
			.setStatementDate(
				TimeManager.getProtoTimestampFromTimestamp(
					bankStatement.getStatementDate()
				)
			)
			.setIsManual(
				bankStatement.isManual()
			)
			.setIsProcessed(
				bankStatement.isProcessed()
			)
			.setBeginningBalance(
				NumberManager.getBigDecimalToString(
					bankStatement.getBeginningBalance()
				)
			)
			.setStatementDifference(
				NumberManager.getBigDecimalToString(
					bankStatement.getStatementDifference()
				)
			)
			.setEndingBalance(
				NumberManager.getBigDecimalToString(
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
	public static BusinessPartner.Builder convertBusinessPartner(String businessPartnerValue) {
		if (Util.isEmpty(businessPartnerValue, true)) {
			return BusinessPartner.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerValue);
		return convertBusinessPartner(businessPartner);
	}
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}

		builder.setId(
				businessPartner.getC_BPartner_ID()
			)
			.setUuid(
				TextManager.getValidString(
					businessPartner.getUUID()
				)
			)
			.setValue(
				TextManager.getValidString(
					businessPartner.getValue()
				)
			)
			.setTaxId(
				TextManager.getValidString(
					businessPartner.getTaxID()
				)
			)
			.setName(
				TextManager.getValidString(
					businessPartner.getName()
				)
			)
			.setDescription(
				TextManager.getValidString(
					businessPartner.getDescription()
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
				TextManager.getValidString(
					currency.getUUID()
				)
			)
			.setIsoCode(
				TextManager.getValidString(
					currency.getISO_Code()
				)
			)
			.setDescription(
				TextManager.getValidString(
					currency.getDescription()
				)
			)
		;

		return builder;
	}



	public static TenderType.Builder convertTenderType(String value) {
		if (Util.isEmpty(value, true)) {
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

		builder.setId(
				tenderType.getAD_Ref_List_ID()
			)
			.setUuid(
				TextManager.getValidString(
					tenderType.getUUID()
				)
			)
			.setValue(
				TextManager.getValidString(
					tenderType.getValue()
				)
			)
			.setName(
				TextManager.getValidString(name)
			)
			.setDescription(
				TextManager.getValidString(description)
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

		builder.setId(
				payment.getC_Payment_ID()
			)
			.setUuid(
				TextManager.getValidString(
					payment.getUUID()
				)
			)
			.setTransactionDate(
				TimeManager.getProtoTimestampFromTimestamp(
					payment.getDateTrx()
				)
			)
			.setTransactionDateFormatted(
				TextManager.getValidString(
					TimeManager.getDateDisplayValue(
						payment.getDateTrx()
					)
				)
			)
			.setIsReceipt(isReceipt)
			.setDocumentNo(
				TextManager.getValidString(
					payment.getDocumentNo()
				)
			)
			.setDescription(
				TextManager.getValidString(
					payment.getDescription()
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					paymentAmount
				)
			)
			.setBusinessPartner(businessPartnerBuilder)
			.setTenderType(tenderTypeBuilder)
			.setCurrency(currencyBuilder)
		;

		return builder;
	}


	public static Payment.Builder convertPayment(ResultSet rs) throws SQLException {
		Payment.Builder builder = Payment.newBuilder();
		if (rs == null) {
			return builder;
		}

		int businessPartnerId = rs.getInt("C_BPartner_ID");
		BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(businessPartnerId);

		int currencyId = rs.getInt("C_Currency_ID");
		Currency.Builder currencyBuilder = convertCurrency(currencyId);

		String tenderType = rs.getString("TenderType");
		TenderType.Builder tenderTypeBuilder = convertTenderType(tenderType);

		boolean isReceipt = "Y".equals(rs.getString("IsReceipt"));
		BigDecimal paymentAmount = rs.getBigDecimal("PayAmt");
		if (!isReceipt) {
			paymentAmount = paymentAmount.negate();
		}

		java.sql.Timestamp dateTrx = rs.getTimestamp("DateTrx");

		builder.setId(
				rs.getInt("C_Payment_ID")
			)
			.setTransactionDate(
				TimeManager.getProtoTimestampFromTimestamp(dateTrx)
			)
			.setTransactionDateFormatted(
				TextManager.getValidString(
					TimeManager.getDateDisplayValue(
						dateTrx
					)
				)
			)
			.setIsReceipt(isReceipt)
			.setDocumentNo(
				TextManager.getValidString(
					rs.getString("DocumentNo")
				)
			)
			.setDescription(
				TextManager.getValidString(
					rs.getString("Description")
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(paymentAmount)
			)
			.setAmountFormatted(
				TextManager.getValidString(
					NumberManager.getAmountDisplayValueWithCurrency(
						paymentAmount,
						currencyId
					)
				)
			)
			.setBusinessPartner(businessPartnerBuilder)
			.setTenderType(tenderTypeBuilder)
			.setCurrency(currencyBuilder)
			.setIsMatched(
				rs.getBoolean("IsMatched")
			)
			.setIsManualMatch(
				rs.getBoolean("IsManualMatch")
			)
		;

		return builder;
	}


	public static ImportedBankMovement.Builder convertImportedBankMovement(X_I_BankStatement importBankStatement) {
		ImportedBankMovement.Builder builder = ImportedBankMovement.newBuilder();
		if (importBankStatement == null || importBankStatement.getI_BankStatement_ID() <= 0) {
			return builder;
		}

		builder.setId(
				importBankStatement.getI_BankStatement_ID()
			)
			.setUuid(
				TextManager.getValidString(
					importBankStatement.getUUID()
				)
			)
			.setReferenceNo(
				TextManager.getValidString(
					importBankStatement.getReferenceNo()
				)
			)
			.setIsReceipt(
				importBankStatement.getTrxAmt().compareTo(BigDecimal.ZERO) >= 0
			)
			.setReferenceNo(
				TextManager.getValidString(
					importBankStatement.getReferenceNo()
				)
			)
			.setMemo(
				TextManager.getValidString(
					importBankStatement.getMemo()
				)
			)
			.setLineDescription(
				TextManager.getValidString(
					importBankStatement.getLineDescription()
				)
			)
			.setTransactionDate(
				TimeManager.getProtoTimestampFromTimestamp(
					importBankStatement.getStatementLineDate()
				)
			)
			.setTransactionDateFormatted(
				TextManager.getValidString(
					TimeManager.getDateDisplayValue(
						importBankStatement.getStatementLineDate()
					)
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					importBankStatement.getTrxAmt()
				)
			)
			.setBankStatementLineId(
				importBankStatement.getC_BankStatementLine_ID()
			)
			.setIsManualMatch(
				importBankStatement.get_ValueAsBoolean(
					"IsManualMatch"
				)
			)
			.setIsMatched(
				importBankStatement.get_ValueAsBoolean(
					"IsMatched"
				)
				|| importBankStatement.getC_Payment_ID() > 0
			)
		;

		if (importBankStatement.getC_Payment_ID() > 0) {
			MPayment payment = new MPayment(Env.getCtx(), importBankStatement.getC_Payment_ID(), null);

			BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
				payment.getC_BPartner_ID()
			);
			builder.setBusinessPartner(businessPartnerBuilder);

			Currency.Builder currencyBuilder = convertCurrency(
				payment.getC_Currency_ID()
			);
			builder.setCurrency(currencyBuilder);
		}

		// Fill Business Partner
		if (builder.getBusinessPartner() == null || builder.getBusinessPartner().getId() <= 0) {
			if (importBankStatement.getC_BPartner_ID() > 0) {
				BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
					importBankStatement.getC_BPartner_ID()
				);
				builder.setBusinessPartner(businessPartnerBuilder);
			} else if (!Util.isEmpty(importBankStatement.getBPartnerValue(), true)) {
				BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
					importBankStatement.getBPartnerValue()
				);
				builder.setBusinessPartner(businessPartnerBuilder);
			}
		}

		// Fill Currency
		if (builder.getCurrency() == null || builder.getCurrency().getId() <= 0) {
			if (importBankStatement.getC_Currency_ID() > 0) {
				Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
					importBankStatement.getC_Currency_ID()
				);
				builder.setCurrency(currencyBuilder);
			} else if (!Util.isEmpty(importBankStatement.getISO_Code(), true)) {
				Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
					importBankStatement.getISO_Code()
				);
				builder.setCurrency(currencyBuilder);
			}
		}

		builder
			.setAmountFormatted(
				TextManager.getValidString(
					NumberManager.getAmountDisplayValueWithCurrency(
						importBankStatement.getTrxAmt(),
						builder.getCurrency().getId()
					)
				)
			)
		;

		return builder;
	}



	public static MatchingMovement.Builder convertMatchMovement(X_I_BankStatement importBankStatement) {
		MatchingMovement.Builder builder = MatchingMovement.newBuilder();
		if (importBankStatement == null || importBankStatement.getI_BankStatement_ID() <= 0) {
			return builder;
		}

		builder.setId(
				importBankStatement.getI_BankStatement_ID()
			)
			.setUuid(
				TextManager.getValidString(
					importBankStatement.getUUID()
				)
			)
			.setReferenceNo(
				TextManager.getValidString(
					importBankStatement.getReferenceNo()
				)
			)
			.setIsReceipt(
				importBankStatement.getTrxAmt().compareTo(BigDecimal.ZERO) >= 0
			)
			.setDescription(
				TextManager.getValidString(
					importBankStatement.getDescription()
				)
			)
			.setMemo(
				TextManager.getValidString(
					importBankStatement.getMemo()
				)
			)
			.setLineDescription(
				TextManager.getValidString(
					importBankStatement.getLineDescription()
				)
			)
			.setTransactionDate(
				TimeManager.getProtoTimestampFromTimestamp(
					importBankStatement.getStatementLineDate()
				)
			)
			.setTransactionDateFormatted(
				TextManager.getValidString(
					TimeManager.getDateDisplayValue(
						importBankStatement.getStatementLineDate()
					)
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					importBankStatement.getTrxAmt()
				)
			)
			.setBankStatementLineId(
				importBankStatement.getC_BankStatementLine_ID()
			)
			.setIsManualMatch(
				importBankStatement.get_ValueAsBoolean(
					"IsManualMatch"
				)
			)
			.setIsMatched(
				importBankStatement.get_ValueAsBoolean(
					"IsMatched"
				)
				|| importBankStatement.getC_Payment_ID() > 0
			)
		;

		// Fill Business Partner
		if (importBankStatement.getC_BPartner_ID() > 0) {
			BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
				importBankStatement.getC_BPartner_ID()
			);
			builder.setBusinessPartner(businessPartnerBuilder);
		} else if (!Util.isEmpty(importBankStatement.getBPartnerValue(), true)) {
			BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
				importBankStatement.getBPartnerValue()
			);
			builder.setBusinessPartner(businessPartnerBuilder);
		}

		// Fill Currency
		if (importBankStatement.getC_Currency_ID() > 0) {
			Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				importBankStatement.getC_Currency_ID()
			);
			builder.setCurrency(currencyBuilder);
		} else if (!Util.isEmpty(importBankStatement.getISO_Code(), true)) {
 			Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				importBankStatement.getISO_Code()
			);
			builder.setCurrency(currencyBuilder);
		}

		if (importBankStatement.getC_Payment_ID() > 0) {
			MPayment payment = new MPayment(Env.getCtx(), importBankStatement.getC_Payment_ID(), null);
			builder.setPaymentId(
					payment.getC_Payment_ID()
				)
				.setPaymentUuid(
					TextManager.getValidString(
						payment.getUUID()
					)
				)
				.setDocumentNo(
					TextManager.getValidString(
						payment.getDocumentNo()
					)
				)
				.setPaymentDate(
					TimeManager.getProtoTimestampFromTimestamp(
						payment.getDateTrx()
					)
				)
				.setPaymentDateFormatted(
					TextManager.getValidString(
						TimeManager.getDateDisplayValue(
							payment.getDateTrx()
						)
					)
				)
				.setPaymentAmount(
					NumberManager.getBigDecimalToString(
						payment.getPayAmt()
					)
				)
				.setDescription(
					TextManager.getValidString(
						payment.getDescription()
					)
				)
			;
			TenderType.Builder tenderTypeBuilder = convertTenderType(
				payment.getTenderType()
			);
			builder.setTenderType(tenderTypeBuilder);

			// Fill Business Partner
			if (builder.getBusinessPartner() == null || builder.getBusinessPartner().getId() <= 0) {
				BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
					payment.getC_BPartner_ID()
				);
				builder.setBusinessPartner(businessPartnerBuilder);
			}

			// Fill Currency
			if (builder.getCurrency() == null || builder.getCurrency().getId() <= 0) {
				Currency.Builder currencyBuilder = convertCurrency(
					payment.getC_Currency_ID()
				);
				builder.setCurrency(currencyBuilder);
			}

			builder
				.setPaymentAmountFormatted(
					TextManager.getValidString(
						NumberManager.getAmountDisplayValueWithCurrency(
							payment.getPayAmt(),
							builder.getCurrency().getId()
						)
					)
				)
			;
		}

		builder
			.setAmountFormatted(
				TextManager.getValidString(
					NumberManager.getAmountDisplayValueWithCurrency(
						importBankStatement.getTrxAmt(),
						builder.getCurrency().getId()
					)
				)
			)
		;

		return builder;
	}



	public static ResultMovement.Builder convertResultMovement(X_I_BankStatement importBankStatement) {
		ResultMovement.Builder builder = ResultMovement.newBuilder();
		if (importBankStatement == null || importBankStatement.getI_BankStatement_ID() <= 0) {
			return builder;
		}
		builder.setId(
				importBankStatement.getI_BankStatement_ID()
			)
			.setUuid(
				TextManager.getValidString(
					importBankStatement.getUUID()
				)
			)
			.setReferenceNo(
				TextManager.getValidString(
					importBankStatement.getReferenceNo()
				)
			)
			.setIsReceipt(
				importBankStatement.getTrxAmt().compareTo(BigDecimal.ZERO) >= 0
			)
			.setMemo(
				TextManager.getValidString(
					importBankStatement.getMemo()
				)
			)
			.setTransactionDate(
				TimeManager.getProtoTimestampFromTimestamp(
					importBankStatement.getStatementLineDate()
				)
			)
			.setTransactionDateFormatted(
				TextManager.getValidString(
					TimeManager.getDateDisplayValue(
						importBankStatement.getStatementLineDate()
					)
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					importBankStatement.getTrxAmt()
				)
			)
			.setBankStatementLineId(
				importBankStatement.getC_BankStatementLine_ID()
			)
			.setIsManualMatch(
				importBankStatement.get_ValueAsBoolean(
					"IsManualMatch"
				)
			)
			.setIsMatched(
				importBankStatement.get_ValueAsBoolean(
					"IsMatched"
				)
				|| importBankStatement.getC_Payment_ID() > 0
			)
		;

		// Fill Business Partner
		if (importBankStatement.getC_BPartner_ID() > 0) {
			BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
				importBankStatement.getC_BPartner_ID()
			);
			builder.setBusinessPartner(businessPartnerBuilder);
		} else if (!Util.isEmpty(importBankStatement.getBPartnerValue(), true)) {
			BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
				importBankStatement.getBPartnerValue()
			);
			builder.setBusinessPartner(businessPartnerBuilder);
		}

		// Fill Currency
		if (importBankStatement.getC_Currency_ID() > 0) {
			Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				importBankStatement.getC_Currency_ID()
			);
			builder.setCurrency(currencyBuilder);
		} else if (!Util.isEmpty(importBankStatement.getISO_Code(), true)) {
 			Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
				importBankStatement.getISO_Code()
			);
			builder.setCurrency(currencyBuilder);
		}

		if (importBankStatement.getC_Payment_ID() > 0) {
			MPayment payment = new MPayment(Env.getCtx(), importBankStatement.getC_Payment_ID(), null);
			builder.setPaymentId(
					payment.getC_Payment_ID()
				)
				.setUuid(
					TextManager.getValidString(
						payment.getUUID()
					)
				)
				.setDocumentNo(
					TextManager.getValidString(
						payment.getDocumentNo()
					)
				)
				.setPaymentDate(
					TimeManager.getProtoTimestampFromTimestamp(
						payment.getDateTrx()
					)
				)
				.setPaymentDateFormatted(
					TextManager.getValidString(
						TimeManager.getDateDisplayValue(
							payment.getDateTrx()
						)
					)
				)
				.setPaymentAmount(
					NumberManager.getBigDecimalToString(
						payment.getPayAmt()
					)
				)
				.setDescription(
					TextManager.getValidString(
						payment.getDescription()
					)
				)
			;
			TenderType.Builder tenderTypeBuilder = convertTenderType(
				payment.getTenderType()
			);
			builder.setTenderType(tenderTypeBuilder);

			// Fill Business Partner
			if (builder.getBusinessPartner() == null || builder.getBusinessPartner().getId() <= 0) {
				BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
					payment.getC_BPartner_ID()
				);
				builder.setBusinessPartner(businessPartnerBuilder);
				builder.setBusinessPartner(businessPartnerBuilder);
			}

			// Fill Currency
			if (builder.getCurrency() == null || builder.getCurrency().getId() <= 0) {
				Currency.Builder currencyBuilder = convertCurrency(
					payment.getC_Currency_ID()
				);
				builder.setCurrency(currencyBuilder);
			}
		}

		builder
			.setAmountFormatted(
				TextManager.getValidString(
					NumberManager.getAmountDisplayValueWithCurrency(
						importBankStatement.getTrxAmt(),
						builder.getCurrency().getId()
					)
				)
			)
		;

		return builder;
	}

}
