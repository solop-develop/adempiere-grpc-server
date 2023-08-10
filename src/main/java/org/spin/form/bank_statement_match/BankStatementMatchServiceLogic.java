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
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_C_BankStatement;
import org.adempiere.core.domains.models.X_I_BankStatement;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.impexp.BankStatementMatchInfo;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementMatcher;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.bank_statement_match.BankStatement;
import org.spin.backend.grpc.form.bank_statement_match.GetBankStatementRequest;
import org.spin.backend.grpc.form.bank_statement_match.ImportedBankMovement;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.MatchMode;
import org.spin.backend.grpc.form.bank_statement_match.MatchingMovement;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

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
					|| currentBankStatementImport.getC_BPartner_ID() != 0
					|| currentBankStatementImport.getC_Invoice_ID() != 0) {
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
					if (info != null && info.isMatched()) {
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
						//	put on hash
						matchedPaymentHashMap.put(currentBankStatementImport.getC_Payment_ID(), currentBankStatementImport);
						matched++;
						break;
					}
				}
			}	//	for all matchers
		}

		builderList.setRecordCount(matched);
		for (Map.Entry<Integer, X_I_BankStatement> entry: matchedPaymentHashMap.entrySet()) {
			MatchingMovement.Builder builder = BankStatementMatchConvertUtil.convertMatchMovement(
				entry.getValue()
			);
			builderList.addRecords(builder);
		}

		return builderList;
	}


	public static ListBankStatementsResponse.Builder listBankStatements(ListBankStatementsRequest request) {
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		final String whereClasue = "C_BankAccount_ID = ?";
		Query query = new Query(
			Env.getCtx(),
			I_C_BankStatement.Table_Name,
			whereClasue,
			null
		)
			.setParameters(bankAccount.getC_BankAccount_ID())
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

}
