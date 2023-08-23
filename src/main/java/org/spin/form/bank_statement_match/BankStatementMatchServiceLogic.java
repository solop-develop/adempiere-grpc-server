/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Edwin Betancourt EdwinBetanc0urt@outlook.com                     *
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_C_BankStatement;
import org.adempiere.core.domains.models.X_I_BankStatement;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.impexp.BankStatementMatchInfo;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MBankStatementMatcher;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MPayment;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.form.bank_statement_match.BankStatement;
import org.spin.backend.grpc.form.bank_statement_match.GetBankStatementRequest;
import org.spin.backend.grpc.form.bank_statement_match.ImportedBankMovement;
import org.spin.backend.grpc.form.bank_statement_match.ListBankAccountsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListBusinessPartnersRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListResultMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListResultMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListSearchModesRequest;
import org.spin.backend.grpc.form.bank_statement_match.MatchMode;
import org.spin.backend.grpc.form.bank_statement_match.MatchPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.MatchPaymentsResponse;
import org.spin.backend.grpc.form.bank_statement_match.MatchingMovement;
import org.spin.backend.grpc.form.bank_statement_match.Payment;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ResultMovement;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchPaymentsResponse;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;
import org.spin.grpc.service.UserInterfaceServiceImplementation;


/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Bank Statement Match form
 */
public abstract class BankStatementMatchServiceLogic {

	public static BankStatement.Builder getBankStatement(GetBankStatementRequest request) {
		if (request.getId() < 0 && Util.isEmpty(request.getUuid(), true)) {
			throw new AdempiereException("@FillMandatory@ @C_BankStatement_ID@/@UUID@");
		}
		int recordId = request.getId();
		if (recordId <= 0) {
			recordId = RecordUtil.getIdFromUuid(I_C_BankStatement.Table_Name, request.getUuid(), null);
		}
		
		MBankStatement bankStatement = new MBankStatement(Env.getCtx(), recordId, null);
		if (bankStatement == null || bankStatement.getC_BankStatement_ID() <= 0) {
			throw new AdempiereException("@C_BankStatement_ID@ @NotFound@");
		}

		return BankStatementMatchConvertUtil.convertBankStatement(bankStatement);
	}



	public static ListLookupItemsResponse.Builder listSearchModes(ListSearchModesRequest request) {
		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		LookupItem.Builder lookupMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_NOT_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(Env.getCtx(), "NotMatched")
				).build())
		;
		builderList.addRecords(lookupMatched);

		LookupItem.Builder lookupUnMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(Env.getCtx(), "Matched")
				).build())
		;
		builderList.addRecords(lookupUnMatched);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listBankAccounts(ListBankAccountsRequest request) {
		// Bank Account
		int columnId = 4917; // C_BankStatement.C_BankAccount_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listBusinessPartners(ListBusinessPartnersRequest request) {
		// Business Partner
		int columnId = 3499; // C_Invoice.C_BPartner_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	public static ListPaymentsResponse.Builder listPayments(ListPaymentsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		boolean isMatchedMode = request.getMatchMode() == MatchMode.MODE_MATCHED;
		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);
		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);

		Query query = BankStatementMatchUtil.buildPaymentQuery(
			bankAccount.getC_BankAccount_ID(),
			isMatchedMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo,
			request.getBusinessPartnerId(),
			request.getBankStatementId()
		);

		int count = query.count();
		String nexPageToken = "";
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListPaymentsResponse.Builder builderList = ListPaymentsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(paymentId -> {
				MPayment payment = new MPayment(
					Env.getCtx(),
					paymentId,
					null
				);
				Payment.Builder paymentBuilder = BankStatementMatchConvertUtil.convertPayment(payment);

				builderList.addRecords(paymentBuilder);
			});
		;

		return builderList;
	}



	public static ListImportedBankMovementsResponse.Builder listImportedBankMovements(ListImportedBankMovementsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccount.getC_BankAccount_ID());

		//	For parameters
		boolean isMatchedMode = request.getMatchMode() == MatchMode.MODE_MATCHED;

		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);
		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);

		Query importMovementsQuery = BankStatementMatchUtil.buildBankMovementQuery(
			bankAccount.getC_BankAccount_ID(),
			isMatchedMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo
		);
		List<Integer> importedBankMovementsId = importMovementsQuery
			// .setLimit(0, 0)
			.getIDsAsList()
		;

		ListImportedBankMovementsResponse.Builder builderList = ListImportedBankMovementsResponse.newBuilder()
			.setRecordCount(importMovementsQuery.count())
		;

		importedBankMovementsId.forEach(bankStatementId -> {
			X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(Env.getCtx(), bankStatementId, null);
			ImportedBankMovement.Builder importedBuilder = BankStatementMatchConvertUtil.convertImportedBankMovement(currentBankStatementImport);
			builderList.addRecords(importedBuilder);
		});

		return builderList;
	}



	public static ListMatchingMovementsResponse.Builder listMatchingMovements(ListMatchingMovementsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		Properties context = Env.getCtx();
		ListMatchingMovementsResponse.Builder builderList = ListMatchingMovementsResponse.newBuilder();

		List<MBankStatementMatcher> matchersList = MBankStatementMatcher.getMatchersList(Env.getCtx(), bankAccount.getC_BankAccount_ID());
		if (matchersList == null || matchersList.isEmpty()) {
			return builderList;
		}

		//	For parameters
		boolean isMatchedMode = request.getMatchMode() == MatchMode.MODE_MATCHED;

		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);

		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);

		Query paymentQuery = BankStatementMatchUtil.buildPaymentQuery(
			bankAccount.getC_BankAccount_ID(),
			isMatchedMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo,
			request.getBusinessPartnerId(),
			request.getBankStatementId()
		);
		List<Integer> paymentsId = paymentQuery.getIDsAsList();
		if (paymentsId == null || paymentsId.isEmpty()) {
			return builderList;
		}

		Query bankMovementQuery = BankStatementMatchUtil.buildBankMovementQuery(
			bankAccount.getC_BankAccount_ID(),
			isMatchedMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo
		);
		List<Integer> importedPaymentsId = bankMovementQuery.getIDsAsList();
		if (importedPaymentsId == null || importedPaymentsId.isEmpty()) {
			return builderList;
		}

		Map<Integer, X_I_BankStatement> matchedPaymentHashMap = new HashMap<Integer, X_I_BankStatement>();
		int matched = 0;
		for (int bankStatementId: importedPaymentsId) {
			X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(context, bankStatementId, null);
			
			if(currentBankStatementImport.getC_Payment_ID() != 0
				// || currentBankStatementImport.getC_BPartner_ID() != 0
				// || currentBankStatementImport.getC_Invoice_ID() != 0
			) {
				//	put on hash
				matchedPaymentHashMap.put(currentBankStatementImport.getC_Payment_ID(), currentBankStatementImport);
				matched++;
				continue;
			}

			for (MBankStatementMatcher matcher : matchersList) {
				if (matcher.isMatcherValid()) {
					BankStatementMatchInfo info = matcher.getMatcher().findMatch(
						currentBankStatementImport,
						paymentsId,
						matchedPaymentHashMap.keySet().stream().collect(Collectors.toList())
					);
					if (info == null || !info.isMatched()) {
						continue;
					}

					//	Duplicate match
					if(matchedPaymentHashMap.containsKey(info.getC_Payment_ID())) {
						continue;
					}
					if (info.getC_Payment_ID() > 0) {
						currentBankStatementImport.setC_Payment_ID(info.getC_Payment_ID());
					}
					if (info.getC_Invoice_ID() > 0) {
						currentBankStatementImport.setC_Invoice_ID(info.getC_Invoice_ID());
					}
					if (info.getC_BPartner_ID() > 0) {
						currentBankStatementImport.setC_BPartner_ID(info.getC_BPartner_ID());
					}
					// TODO: Change to automatic match flag
					currentBankStatementImport.setEftMemo("Y");

					//	put on hash
					matchedPaymentHashMap.put(currentBankStatementImport.getC_Payment_ID(), currentBankStatementImport);
					matched++;
				}
			}	//	for all matchers
		}

		builderList.setRecordCount(matched);
		for (Map.Entry<Integer, X_I_BankStatement> entry: matchedPaymentHashMap.entrySet()) {
			X_I_BankStatement importBankStatement = entry.getValue();
			importBankStatement.saveEx();
			MatchingMovement.Builder builder = BankStatementMatchConvertUtil.convertMatchMovement(
				importBankStatement
			);
			builderList.addRecords(builder);
		}

		return builderList;
	}


	public static ListBankStatementsResponse.Builder listBankStatements(ListBankStatementsRequest request) {
		final String whereClause = "Processed = 'Y' ";
		Query query = new Query(
			Env.getCtx(),
			I_C_BankStatement.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		int recordCount = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListBankStatementsResponse.Builder builderList = ListBankStatementsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(bankStatementId -> {
				BankStatement.Builder builder = BankStatementMatchConvertUtil.convertBankStatement(bankStatementId);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListResultMovementsResponse.Builder listResultMovements(ListResultMovementsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccount.getC_BankAccount_ID());

		//	For parameters
		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);
		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);

		Query importMovementsQuery = BankStatementMatchUtil.buildResultMovementsQuery(
			request.getBankStatementId(),
			bankAccount.getC_BankAccount_ID(),
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo
		);
		List<Integer> importedBankMovementsId = importMovementsQuery
			// .setLimit(0, 0)
			.getIDsAsList()
		;

		ListResultMovementsResponse.Builder builderList = ListResultMovementsResponse.newBuilder()
			.setRecordCount(importMovementsQuery.count())
		;

		importedBankMovementsId.forEach(bankStatementId -> {
			X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(Env.getCtx(), bankStatementId, null);
			ResultMovement.Builder importedBuilder = BankStatementMatchConvertUtil.convertResultMovement(currentBankStatementImport);
			builderList.addRecords(importedBuilder);
		});

		return builderList;
	}



	public static MatchPaymentsResponse.Builder matchPayments(MatchPaymentsRequest request) {
		AtomicInteger result = new AtomicInteger(0);

		request.getKeyMatchesList().forEach(keyMatch -> {
			if (keyMatch.getImportedMovementId() <= 0 || keyMatch.getPaymentId() <= 0) {
				return;
			}
			X_I_BankStatement bankStatement = new X_I_BankStatement(Env.getCtx(), keyMatch.getImportedMovementId(), null);
			if (bankStatement.isProcessed()) {
				return;
			}
			MPayment payment = new MPayment(Env.getCtx(), keyMatch.getPaymentId(), null);
			if (payment != null && payment.getC_Payment_ID() > 0) {
				bankStatement.setC_Payment_ID(payment.getC_Payment_ID());
				if (bankStatement.is_Changed() && bankStatement.save()) {
					result.incrementAndGet();
				}
			}
		});

		MatchPaymentsResponse.Builder builder = MatchPaymentsResponse.newBuilder()
			.setMessage(String.valueOf(result.get()))
		;

		return builder;
	}



	public static UnmatchPaymentsResponse.Builder unmatchPayments(UnmatchPaymentsRequest request) {
		AtomicInteger result = new AtomicInteger(0);

		request.getImportedMovementsIdsList().stream().forEach(importedBankMovementId -> {
			X_I_BankStatement bankStatement = new X_I_BankStatement(Env.getCtx(), importedBankMovementId, null);
			if (bankStatement.isProcessed()) {
				return;
			}
			bankStatement.setC_Payment_ID(0);
			if (bankStatement.is_Changed() && bankStatement.save()) {
				result.incrementAndGet();
			}
		});

		UnmatchPaymentsResponse.Builder builder = UnmatchPaymentsResponse.newBuilder()
			.setMessage(String.valueOf(result.get()))
		;

		return builder;
	}



	public static ProcessMovementsResponse.Builder processMovements(ProcessMovementsRequest request) {
		if(request.getBankStatementId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BankStatement_ID@");
		}
		MBankStatement bankStatement = new MBankStatement(Env.getCtx(), request.getBankStatementId(), null);
		if(bankStatement == null || bankStatement.getC_BankStatement_ID() <= 0) {
			throw new AdempiereException("@C_BankStatement_ID@ @NotFound@");
		}
		if(bankStatement.isProcessed()) {
			throw new AdempiereException("@C_BankStatement_ID@ @IsProcessed@");
		}

		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		AtomicInteger processed = new AtomicInteger();
		AtomicInteger lineNo = new AtomicInteger(10);
		int defaultChargeId = DB.getSQLValue(null, "SELECT MAX(C_Charge_ID) FROM C_Charge WHERE AD_Client_ID = ?", Env.getAD_Client_ID(Env.getCtx()));
		if(defaultChargeId <= 0) {
			throw new AdempiereException("@C_Charge_ID@ @NotFound@");
		}

		//	For parameters
		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);

		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);

		Query bankMovementQuery = BankStatementMatchUtil.buildResultMovementsQuery(
			bankStatement.getC_BankStatement_ID(),
			bankAccount.getC_BankAccount_ID(),
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo
		);

		ProcessMovementsResponse.Builder builder = ProcessMovementsResponse.newBuilder();
		List<Integer> importedPaymentsId = bankMovementQuery.getIDsAsList();
		if (importedPaymentsId == null || importedPaymentsId.isEmpty()) {
			return builder;
		}

		importedPaymentsId
			.stream()
			.forEach(importedBankMovementId -> {
				X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(Env.getCtx(), importedBankMovementId, null);

				//	Set trx
				// currentBankStatementImport.set_TrxName(null);
				//	Save It
				currentBankStatementImport.saveEx();
				//	For Bank Statement
				if(currentBankStatementImport.getC_Payment_ID() <= 0 && currentBankStatementImport.getC_Charge_ID() <= 0) {
					currentBankStatementImport.setC_Charge_ID(defaultChargeId);
				}
				if(currentBankStatementImport.getC_BankStatementLine_ID() == 0) {
					//	
					MBankStatementLine lineToImport = new MBankStatementLine(bankStatement, currentBankStatementImport, lineNo.get());
					lineToImport.saveEx();
					currentBankStatementImport.setC_BankStatement_ID(bankStatement.getC_BankStatement_ID());
					currentBankStatementImport.setC_BankStatementLine_ID(lineToImport.getC_BankStatementLine_ID());
					currentBankStatementImport.setI_IsImported(true);
					currentBankStatementImport.setProcessed(true);
					currentBankStatementImport.saveEx();

					lineNo.addAndGet(10);
				} else {
					MBankStatementLine currentStatementLine = new MBankStatementLine(Env.getCtx(), currentBankStatementImport.getC_BankStatementLine_ID(), null);
					if(currentBankStatementImport.getC_Payment_ID() == 0) {
						currentStatementLine.setC_Payment_ID(-1);
					} else {
						currentStatementLine.setC_Payment_ID(currentBankStatementImport.getC_Payment_ID());
					}
					if(currentBankStatementImport.getC_BPartner_ID() == 0) {
						currentStatementLine.setC_BPartner_ID(-1);
					} else {
						currentStatementLine.setC_BPartner_ID(currentBankStatementImport.getC_BPartner_ID());
					}
					if(currentBankStatementImport.getC_Invoice_ID() == 0) {
						currentStatementLine.setC_Invoice_ID(-1);
					} else {
						currentStatementLine.setC_Invoice_ID(currentBankStatementImport.getC_Invoice_ID());
					}
					currentStatementLine.saveEx();
				}
				currentBankStatementImport.saveEx();

				processed.addAndGet(1);
			});

		builder.setMessage(
			Msg.translate(Env.getCtx(), "BankStatementMatch.MatchedProcessed") + ": " + processed
		);

		return builder;
	}

}
