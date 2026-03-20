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
package org.spin.grpc.service.form.bank_statement_match;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_C_BankStatement;
import org.adempiere.core.domains.models.I_C_BankStatementLineMatch;
import org.adempiere.core.domains.models.X_I_BankStatement;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.impexp.BankStatementMatchInfo;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MBankStatementLineMatch;
import org.compiere.model.MBankStatementMatcher;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MPayment;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.form.bank_statement_match.BankStatement;
import org.spin.backend.grpc.form.bank_statement_match.BankStatementLineMatch;
import org.spin.backend.grpc.form.bank_statement_match.GetBankStatementRequest;
import org.spin.backend.grpc.form.bank_statement_match.ImportedBankMovement;
import org.spin.backend.grpc.form.bank_statement_match.ListBankAccountsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementLineMatchesRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBankStatementLineMatchesResponse;
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
import org.spin.backend.grpc.form.bank_statement_match.ManualMatchRequest;
import org.spin.backend.grpc.form.bank_statement_match.ManualMatchResponse;
import org.spin.backend.grpc.form.bank_statement_match.MatchMode;
import org.spin.backend.grpc.form.bank_statement_match.MatchPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.MatchPaymentsResponse;
import org.spin.backend.grpc.form.bank_statement_match.MatchingMovement;
import org.spin.backend.grpc.form.bank_statement_match.MultiPaymentMatchRequest;
import org.spin.backend.grpc.form.bank_statement_match.MultiPaymentMatchResponse;
import org.spin.backend.grpc.form.bank_statement_match.Payment;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ResultMovement;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchMultiPaymentRequest;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchMultiPaymentResponse;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.UnmatchPaymentsResponse;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.field.field_management.FieldManagementLogic;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

import com.google.protobuf.Struct;


/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Bank Statement Match form
 */
public abstract class BankStatementMatchLogic {

	private static CLogger log = CLogger.getCLogger(BankStatementMatchLogic.class);

	public static final int FORM_ID = 53077;

	public static BankStatement.Builder getBankStatement(GetBankStatementRequest request) {
		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Form,
			FORM_ID
		);

		if (request.getId() < 0) {
			throw new AdempiereException("@FillMandatory@ @C_BankStatement_ID@");
		}
		int recordId = request.getId();
		MBankStatement bankStatement = new MBankStatement(Env.getCtx(), recordId, null);
		if (bankStatement == null || bankStatement.getC_BankStatement_ID() <= 0) {
			throw new AdempiereException("@C_BankStatement_ID@ (" + recordId + ") @NotFound@");
		}

		return BankStatementMatchConvertUtil.convertBankStatement(bankStatement);
	}

	public static ListBankStatementsResponse.Builder listBankStatements(ListBankStatementsRequest request) {
		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Form,
			FORM_ID
		);

		String whereClause = "Processed = 'N' AND Processing = 'N'";
		List<Object> filtersList = new ArrayList<Object>();
		final String searchValue = request.getSearchValue();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += " AND ("
				+ "UPPER(DocumentNo) LIKE UPPER(?) "
				+ "OR UPPER(Name) LIKE UPPER(?) "
				+ "OR UPPER(Description) LIKE UPPER(?)"
				+ ")"
			;
			filtersList.add(searchValue);
			filtersList.add(searchValue);
			filtersList.add(searchValue);
		}

		Query query = new Query(
			Env.getCtx(),
			I_C_BankStatement.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersList)
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
				TextManager.getValidString(nexPageToken)
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



	public static ListLookupItemsResponse.Builder listSearchModes(ListSearchModesRequest request) {
		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		// unmatched
		Struct.Builder valuesUnMatched = Struct.newBuilder()
			.putFields(
				LookupUtil.VALUE_COLUMN_KEY,
				NumberManager.getProtoValueFromInt(
					MatchMode.MODE_NOT_MATCHED_VALUE
				).build()
			)
			.putFields(
				LookupUtil.DISPLAY_COLUMN_KEY,
				TextManager.getProtoValueFromString(
					Msg.translate(Env.getCtx(), "NotMatched")
				).build()
			)
		;
		LookupItem.Builder lookupUnMatched = LookupItem.newBuilder()
			.setValues(
				valuesUnMatched
			)
		;
		builderList.addRecords(lookupUnMatched);

		// matched
		Struct.Builder valuesMatched = Struct.newBuilder()
			.putFields(
				LookupUtil.VALUE_COLUMN_KEY,
				NumberManager.getProtoValueFromInt(
					MatchMode.MODE_MATCHED_VALUE
				).build()
			)
			.putFields(
				LookupUtil.DISPLAY_COLUMN_KEY,
				TextManager.getProtoValueFromString(
					Msg.translate(Env.getCtx(), "Matched")
				).build()
			)
		;
		LookupItem.Builder lookupMatched = LookupItem.newBuilder()
			.setValues(
				valuesMatched
			)
		;
		builderList.addRecords(lookupMatched);

		// All
		Struct.Builder valuesAll = Struct.newBuilder()
			.putFields(
				LookupUtil.VALUE_COLUMN_KEY,
				NumberManager.getProtoValueFromInt(
					MatchMode.MODE_ALL_VALUE
				).build()
			)
			.putFields(
				LookupUtil.DISPLAY_COLUMN_KEY,
				TextManager.getProtoValueFromString(
					Msg.translate(Env.getCtx(), "All")
				).build()
			)
		;
		LookupItem.Builder lookupAll = LookupItem.newBuilder()
			.setValues(
				valuesAll
			)
		;
		builderList.addRecords(lookupAll);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listBankAccounts(ListBankAccountsRequest request) {
		// Bank Account
		int columnId = 4917; // C_BankStatement.C_BankAccount_ID
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listBusinessPartners(ListBusinessPartnersRequest request) {
		// Business Partner
		int columnId = 3499; // C_Invoice.C_BPartner_ID
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListPaymentsResponse.Builder listPayments(ListPaymentsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(
			request.getBankAccountId()
		);

		final int matchMode = request.getMatchModeValue();
		//	Date Trx
		final Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateFrom()
		);
		final Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateTo()
		);
		//	Amount
		final BigDecimal paymentAmountFrom = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountFrom()
		);
		final BigDecimal paymentAmountTo = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountTo()
		);

		ArrayList<Object> parametersList = new ArrayList<Object>();
		String sql = BankStatementMatchUtil.buildPaymentSQL(
			request.getBankStatementId(),
			bankAccount.getC_BankAccount_ID(),
			matchMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo,
			request.getBusinessPartnerId(),
			request.getSearchValue(),
			request.getIsReceipt(),
			request.getIsMultiPaymentMatch(),
			parametersList
		);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ListPaymentsResponse.Builder builderList = ListPaymentsResponse.newBuilder();
		try {
			pstmt = DB.prepareStatement(sql, null);
			DB.setParameters(pstmt, parametersList);
			rs = pstmt.executeQuery();
			int recordCount = 0;
			while (rs.next()) {
				recordCount++;
				Payment.Builder paymentBuilder = BankStatementMatchConvertUtil.convertPayment(rs);
				builderList.addRecords(paymentBuilder);
			}
			builderList.setRecordCount(recordCount);
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	public static ListImportedBankMovementsResponse.Builder listImportedBankMovements(ListImportedBankMovementsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(
			request.getBankAccountId()
		);

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccount.getC_BankAccount_ID());

		//	For parameters
		final int matchMode = request.getMatchModeValue();

		//	Date Trx
		final Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateFrom()
		);
		final Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateTo()
		);
		//	Amount
		final BigDecimal paymentAmountFrom = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountFrom()
		);
		final BigDecimal paymentAmountTo = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountTo()
		);

		Query importMovementsQuery = BankStatementMatchUtil.buildImportBankMovementQuery(
			request.getBankStatementId(),
			bankAccount.getC_BankAccount_ID(),
			matchMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo,
			request.getSearchValue(),
			request.getIsReceipt(),
			request.getIsMultiPaymentMatch()
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
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(
			request.getBankAccountId()
		);

		Properties context = Env.getCtx();
		ListMatchingMovementsResponse.Builder builderList = ListMatchingMovementsResponse.newBuilder();

		List<MBankStatementMatcher> matchersList = MBankStatementMatcher.getMatchersList(
			Env.getCtx(),
			bankAccount.getC_Bank_ID()
		);
		if (matchersList == null || matchersList.isEmpty()) {
			return builderList;
		}

		//	For parameters
		final int matchMode = request.getMatchModeValue();

		//	Date Trx
		final Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateFrom()
		);
		final Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateTo()
		);

		//	Amount
		final BigDecimal paymentAmountFrom = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountFrom()
		);
		final BigDecimal paymentAmountTo = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountTo()
		);

		final boolean isMultiPaymentMatch = request.getIsMultiPaymentMatch();

		Query paymentQuery = BankStatementMatchUtil.buildPaymentQuery(
			request.getBankStatementId(),
			bankAccount.getC_BankAccount_ID(),
			matchMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo,
			request.getBusinessPartnerId(),
			null,
			null,
			isMultiPaymentMatch
		);
		List<Integer> paymentsId = paymentQuery.getIDsAsList();
		if (paymentsId == null || paymentsId.isEmpty()) {
			return builderList;
		}

		Query bankMovementQuery = BankStatementMatchUtil.buildImportBankMovementQuery(
			request.getBankStatementId(),
			bankAccount.getC_BankAccount_ID(),
			matchMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo,
			null,
			null,
			isMultiPaymentMatch
		);
		List<Integer> importedBankMovementsId = bankMovementQuery.getIDsAsList();
		if (importedBankMovementsId == null || importedBankMovementsId.isEmpty()) {
			return builderList;
		}

		Map<Integer, X_I_BankStatement> matchedPaymentHashMap = new HashMap<Integer, X_I_BankStatement>();
		int matched = 0;
		for (int bankStatementId: importedBankMovementsId) {
			X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(context, bankStatementId, null);

			// Multi-payment match: use I_BankStatement_ID as key (negative) to avoid collision
			// with C_Payment_ID keys from 1:1 matches. Ensures all M movements appear.
			if (currentBankStatementImport.get_ValueAsBoolean("IsMultiPaymentMatch")) {
				int mapKey = -currentBankStatementImport.getI_BankStatement_ID();
				matchedPaymentHashMap.put(mapKey, currentBankStatementImport);
				matched++;
				continue;
			}

			if(currentBankStatementImport.getC_Payment_ID() > 0
				// || currentBankStatementImport.getC_BPartner_ID() != 0
				// || currentBankStatementImport.getC_Invoice_ID() != 0
			) {
				//	put on hash
				matchedPaymentHashMap.put(currentBankStatementImport.getC_Payment_ID(), currentBankStatementImport);
				// if (!(currentBankStatementImport.isProcessed() || currentBankStatementImport.isProcessing())) {
				// Only set IsMatched for automatic matches, not manual ones
				if (!currentBankStatementImport.get_ValueAsBoolean("IsManualMatch")) {
					currentBankStatementImport.set_ValueOfColumn(
						"IsMatched",
						true
					);
					currentBankStatementImport.save();
				}
				// }
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
					// if (!currentBankStatementImport.get_ValueAsBoolean("IsManualMatch")) {
						if (info == null || !info.isMatched()) {
							// currentBankStatementImport.setC_Payment_ID(0);
							currentBankStatementImport.set_ValueOfColumn(
								"IsMatched",
								false
							);
							currentBankStatementImport.save();
							continue;
						}
					// } else {
					// 	matchedPaymentHashMap.put(currentBankStatementImport.getC_Payment_ID(), currentBankStatementImport);
					// }

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
					if (!(currentBankStatementImport.isProcessed() || currentBankStatementImport.isProcessing())) {
						currentBankStatementImport.set_ValueOfColumn(
							"IsMatched",
							true
						);
						// currentBankStatementImport.set_ValueOfColumn(
						// 	"IsManualMatch",
						// 	false
						// );
						currentBankStatementImport.save();
					}

					//	put on hash
					matchedPaymentHashMap.put(currentBankStatementImport.getC_Payment_ID(), currentBankStatementImport);
					matched++;
				} else {
					// if (!currentBankStatementImport.get_ValueAsBoolean("IsManualMatch")) {
						// currentBankStatementImport.setC_Payment_ID(0);
						currentBankStatementImport.set_ValueOfColumn(
							"IsMatched",
							false
						);
						currentBankStatementImport.save();
					// } else {
					// 	matchedPaymentHashMap.put(currentBankStatementImport.getC_Payment_ID(), currentBankStatementImport);
					// }
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



	public static ListResultMovementsResponse.Builder listResultMovements(ListResultMovementsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(
			request.getBankAccountId()
		);

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccount.getC_BankAccount_ID());

		//	For parameters
		final int matchMode = request.getMatchModeValue();

		//	Date Trx
		Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateTo()
		);
		//	Amount
		BigDecimal paymentAmountFrom = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountTo()
		);

		Query importMovementsQuery = BankStatementMatchUtil.buildResultMovementsQuery(
			request.getBankStatementId(),
			bankAccount.getC_BankAccount_ID(),
			matchMode,
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

		importedBankMovementsId.forEach(importedBankMovementId -> {
			X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(Env.getCtx(), importedBankMovementId, null);
			ResultMovement.Builder importedBuilder = BankStatementMatchConvertUtil.convertResultMovement(currentBankStatementImport);
			builderList.addRecords(importedBuilder);
		});

		return builderList;
	}



	public static ManualMatchResponse.Builder manualMatch(ManualMatchRequest request) {
		if (request.getPaymentId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Payment_ID@");
		}
		if (request.getImportedMovementId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @I_BankStatement_ID@");
		}

		X_I_BankStatement importedMovement = new X_I_BankStatement(Env.getCtx(), request.getImportedMovementId(), null);
		if (importedMovement == null || importedMovement.getI_BankStatement_ID() <= 0) {
			throw new AdempiereException("@I_BankStatement_ID@ (" + request.getImportedMovementId() + ") @NotFound@");
		}
		// if (importedMovement.isProcessed()) {
		// 	throw new AdempiereException("@I_BankStatement_ID@ (" + request.getImportedMovementId() + ") @Processed@");
		// }

		MPayment payment = new MPayment(Env.getCtx(), request.getPaymentId(), null);
		if (payment == null || payment.getC_Payment_ID() <= 0) {
			throw new AdempiereException("@C_Payment_ID@ (" + request.getPaymentId() + ") @NotFound@");
		}

		// Validate that the payment is not already associated with another imported movement
		final String sqlAlreadyMatch = "SELECT I_BankStatement_ID "
			+ "FROM I_BankStatement "
			+ "WHERE C_Payment_ID = ? "
				+ "AND I_BankStatement_ID <> ? "
				+ "AND (COALESCE(IsMatched, 'N') = 'Y' OR COALESCE(IsManualMatch, 'N') = 'Y')"
		;
		int existingImportId = DB.getSQLValue(
			null,
			sqlAlreadyMatch,
			payment.getC_Payment_ID(),
			importedMovement.getI_BankStatement_ID()
		);
		if (existingImportId > 0) {
			throw new AdempiereException(
				"@NotMatched@: *@C_Payment_ID@ (" + payment.getC_Payment_ID() + ") #" + payment.getDocumentNo() + "*"
				+ "@AlreadyExists@ *@I_BankStatement_ID@ (" + existingImportId + ")*"
			);
		}

		// Validate same direction: receipt vs payment (receivable/payable)
		boolean paymentIsReceipt = payment.isReceipt();
		boolean importedIsReceipt = importedMovement.getTrxAmt().compareTo(BigDecimal.ZERO) >= 0;
		if (paymentIsReceipt != importedIsReceipt) {
			throw new AdempiereException("@IsReceipt@ @BankStatementMatch.NoMatchedFound@: "
				+ "@C_Payment_ID@ (" + payment.getC_Payment_ID() + " = " + (paymentIsReceipt ? "@IsReceipt@" : "@IsPayment@") + ") vs "
				+ "@I_BankStatement_ID@ (" + importedMovement.getI_BankStatement_ID() + " = " + (importedIsReceipt ? "@IsReceipt@" : "@IsPayment@") + ")"
			);
		}

		// Validate same currency
		if (payment.getC_Currency_ID() != importedMovement.getC_Currency_ID()) {
			throw new AdempiereException(
				"@C_Currency_ID@ @BankStatementMatch.NoMatchedFound@: "
				+ "@C_Payment_ID@ (" + payment.getC_Payment_ID() + " = " + payment.getCurrencyISO() + ") vs "
				+ "@I_BankStatement_ID@ (" + importedMovement.getI_BankStatement_ID() + " = " + importedMovement.getC_Currency().getISO_Code() + ")"
			);
		}

		// Validate sign: positive (receipt) with positive, negative (payment) with negative
		final BigDecimal paymentAmount = payment.getPayAmt(true);
		if ((paymentAmount.signum() < 0) != (importedMovement.getTrxAmt().signum() < 0)) {
			throw new AdempiereException("@PayAmt@/@TrxAmt@ @BankStatementMatch.NoMatchedFound@: "
				+ "@C_Payment_ID@ (" + payment.getC_Payment_ID() + " = " + paymentAmount + ") vs "
				+ "@I_BankStatement_ID@ (" + importedMovement.getI_BankStatement_ID() + " = " + importedMovement.getTrxAmt() + ")"
			);
		}

		// If there is a difference between TrxAmt and PayAmt, find and set default charge
		BigDecimal difference = importedMovement.getTrxAmt().subtract(paymentAmount);
		if (difference.compareTo(BigDecimal.ZERO) != 0) {
			final int defaultChargeId = BankStatementMatchUtil.validateAndGetDefaultChargeId();
			importedMovement.setC_Charge_ID(defaultChargeId);
			importedMovement.setChargeAmt(difference);
		}

		// Assign and persist
		importedMovement.setC_Payment_ID(payment.getC_Payment_ID());
		importedMovement.set_ValueOfColumn(
			"IsManualMatch",
			true
		);
		importedMovement.saveEx();

		MatchingMovement.Builder matchingMovementBuilder = BankStatementMatchConvertUtil.convertMatchMovement(importedMovement);
		return ManualMatchResponse.newBuilder()
			.setMatchedMovement(matchingMovementBuilder)
		;
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
			.setMessage(
				String.valueOf(
					result.get()
				)
			)
		;

		return builder;
	}



	public static UnmatchPaymentsResponse.Builder unmatchPayments(UnmatchPaymentsRequest request) {
		AtomicInteger result = new AtomicInteger(0);

		final int defaultChargeId = BankStatementMatchUtil.getDefaultChargeId();

		request.getImportedMovementsIdsList().stream().forEach(importedBankMovementId -> {
			// Delete associated C_BankStatementLineMatch records
			BankStatementMatchUtil.deleteLineMatchesByImportedMovement(importedBankMovementId);

			X_I_BankStatement importedMovement = new X_I_BankStatement(Env.getCtx(), importedBankMovementId, null);
			importedMovement.setC_Payment_ID(0);
			importedMovement.set_ValueOfColumn("IsMatched", false);
			importedMovement.set_ValueOfColumn("IsManualMatch", false);
			importedMovement.set_ValueOfColumn("IsMultiPaymentMatch", false);
			// Revert charge if it was the default charge set during manual match
			if (importedMovement.getC_Charge_ID() > 0 && importedMovement.getC_Charge_ID() == defaultChargeId) {
				importedMovement.setC_Charge_ID(0);
				importedMovement.setChargeAmt(BigDecimal.ZERO);
			}
			if (importedMovement.is_Changed() && importedMovement.save()) {
				result.incrementAndGet();
			}
		});

		UnmatchPaymentsResponse.Builder builder = UnmatchPaymentsResponse.newBuilder()
			.setMessage(
				String.valueOf(
					result.get()
				)
			)
		;

		return builder;
	}



	public static ProcessMovementsResponse.Builder processMovements(ProcessMovementsRequest request) {
		if(request.getBankStatementId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BankStatement_ID@");
		}
		MBankStatement bankStatement = new MBankStatement(Env.getCtx(), request.getBankStatementId(), null);
		if(bankStatement == null || bankStatement.getC_BankStatement_ID() <= 0) {
			throw new AdempiereException("@C_BankStatement_ID@ (" + request.getBankStatementId() + ") @NotFound@");
		}
		if(bankStatement.isProcessed()) {
			throw new AdempiereException("@C_BankStatement_ID@ (" + request.getBankStatementId() + ") @Processed@");
		}

		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(
			request.getBankAccountId()
		);

		final int defaultChargeId = BankStatementMatchUtil.validateAndGetDefaultChargeId();

		//	For parameters
		final int matchMode = request.getMatchModeValue();

		//	Date Trx
		Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(
			request.getTransactionDateTo()
		);

		//	Amount
		BigDecimal paymentAmountFrom = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = NumberManager.getBigDecimalFromString(
			request.getPaymentAmountTo()
		);

		Query bankMovementQuery = BankStatementMatchUtil.buildResultMovementsQuery(
			bankStatement.getC_BankStatement_ID(),
			bankAccount.getC_BankAccount_ID(),
			matchMode,
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

		AtomicInteger processed = new AtomicInteger();
		AtomicInteger lineNo = new AtomicInteger(10);
		Trx.run(transactionName -> {
			bankStatement.set_TrxName(transactionName);
			importedPaymentsId
				.stream()
				.forEach(importedBankMovementId -> {
					X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(Env.getCtx(), importedBankMovementId, transactionName);

					currentBankStatementImport.saveEx();
					//	Validate that the charge exists
					int chargeId = currentBankStatementImport.getC_Charge_ID();
					if (chargeId > 0) {
						int validChargeId = DB.getSQLValue(
							transactionName,
							"SELECT C_Charge_ID FROM C_Charge WHERE C_Charge_ID = ?",
							chargeId
						);
						if (validChargeId <= 0) {
							currentBankStatementImport.setC_Charge_ID(0);
						}
					}
					//	Set default charge if no payment and no charge
					if(currentBankStatementImport.getC_Payment_ID() <= 0 && currentBankStatementImport.getC_Charge_ID() <= 0) {
						currentBankStatementImport.setC_Charge_ID(defaultChargeId);
					}
					MBankStatementLine statementLine = null;
					if(currentBankStatementImport.getC_BankStatementLine_ID() > 0) {
						statementLine = new MBankStatementLine(Env.getCtx(), currentBankStatementImport.getC_BankStatementLine_ID(), transactionName);
						// Validate that the line actually exists in the database
						if (statementLine == null || statementLine.getC_BankStatementLine_ID() <= 0) {
							statementLine = null;
						}
					}
					if(statementLine == null) {
						statementLine = new MBankStatementLine(bankStatement, currentBankStatementImport, lineNo.get());
						statementLine.saveEx();
						currentBankStatementImport.setC_BankStatement_ID(bankStatement.getC_BankStatement_ID());
						currentBankStatementImport.setC_BankStatementLine_ID(statementLine.getC_BankStatementLine_ID());
						lineNo.addAndGet(10);
					} else {
						if(currentBankStatementImport.getC_Payment_ID() == 0) {
							statementLine.setC_Payment_ID(-1);
						} else {
							statementLine.setC_Payment_ID(currentBankStatementImport.getC_Payment_ID());
						}
						if(currentBankStatementImport.getC_BPartner_ID() == 0) {
							statementLine.setC_BPartner_ID(-1);
						} else {
							statementLine.setC_BPartner_ID(currentBankStatementImport.getC_BPartner_ID());
						}
						if(currentBankStatementImport.getC_Invoice_ID() == 0) {
							statementLine.setC_Invoice_ID(-1);
						} else {
							statementLine.setC_Invoice_ID(currentBankStatementImport.getC_Invoice_ID());
						}
						if(currentBankStatementImport.getC_Charge_ID() > 0) {
							statementLine.setC_Charge_ID(currentBankStatementImport.getC_Charge_ID());
						}
						statementLine.saveEx();
					}

					// Migrate C_BankStatementLineMatch from I_BankStatement_ID to C_BankStatementLine_ID
					if (currentBankStatementImport.get_ValueAsBoolean("IsMultiPaymentMatch")) {
						// Delete old matches linked by I_BankStatement_ID and recreate with C_BankStatementLine_ID
						List<MBankStatementLineMatch> lineMatches = BankStatementMatchUtil.getLineMatchesByImportedMovement(
							currentBankStatementImport.getI_BankStatement_ID()
						);
						for (MBankStatementLineMatch lineMatch : lineMatches) {
							final int paymentId = lineMatch.getC_Payment_ID();
							final int currencyId = lineMatch.getC_Currency_ID();
							final boolean isManualMatch = lineMatch.isManualMatch();
							final Timestamp matchDate = lineMatch.getMatchDate();
							lineMatch.deleteEx(true);
							// Create new match with C_BankStatementLine_ID
							MBankStatementLineMatch newMatch = new MBankStatementLineMatch(Env.getCtx(), 0, transactionName);
							newMatch.setC_BankStatement_ID(statementLine.getC_BankStatement_ID());
							newMatch.setC_BankStatementLine_ID(statementLine.getC_BankStatementLine_ID());
							newMatch.setC_Payment_ID(paymentId);
							newMatch.setC_Currency_ID(currencyId);
							newMatch.setIsManualMatch(isManualMatch);
							newMatch.setMatchDate(matchDate);
							newMatch.saveEx();
						}
					}

					currentBankStatementImport.setI_IsImported(true);
					currentBankStatementImport.setProcessed(true);
					currentBankStatementImport.set_ValueOfColumn("IsMatched", true);
					currentBankStatementImport.saveEx();

					processed.addAndGet(1);
				})
			;
		});

		builder.setMessage(
			TextManager.getValidString(
				Msg.parseTranslation(
					Env.getCtx(),
					"BankStatementMatch.MatchedProcessed"
				)
				+ ": " + processed
			)
		);

		return builder;
	}



	public static MultiPaymentMatchResponse.Builder multiPaymentMatch(MultiPaymentMatchRequest request) {
		List<Integer> paymentIds = request.getPaymentIdsList();
		List<Integer> importedMovementIds = request.getImportedMovementIdsList();

		if (paymentIds == null || paymentIds.isEmpty()) {
			throw new AdempiereException("@FillMandatory@ @C_Payment_ID@");
		}
		if (importedMovementIds == null || importedMovementIds.isEmpty()) {
			throw new AdempiereException("@FillMandatory@ @I_BankStatement_ID@");
		}

		// V4 - No duplicates
		Set<Integer> uniquePaymentIds = new HashSet<>(paymentIds);
		if (uniquePaymentIds.size() != paymentIds.size()) {
			throw new AdempiereException("@C_Payment_ID@ @AlreadyExists@");
		}
		Set<Integer> uniqueMovementIds = new HashSet<>(importedMovementIds);
		if (uniqueMovementIds.size() != importedMovementIds.size()) {
			throw new AdempiereException("@I_BankStatement_ID@ @AlreadyExists@");
		}

		// Load and validate imported movements
		List<X_I_BankStatement> importedMovements = new ArrayList<>();
		for (int movementId : importedMovementIds) {
			X_I_BankStatement importedMovement = new X_I_BankStatement(Env.getCtx(), movementId, null);
			if (importedMovement == null || importedMovement.getI_BankStatement_ID() <= 0) {
				throw new AdempiereException("@I_BankStatement_ID@ (" + movementId + ") @NotFound@");
			}
			importedMovements.add(importedMovement);
		}

		// Load and validate payments
		List<MPayment> payments = new ArrayList<>();
		for (int paymentId : paymentIds) {
			MPayment payment = new MPayment(Env.getCtx(), paymentId, null);
			if (payment == null || payment.getC_Payment_ID() <= 0) {
				throw new AdempiereException("@C_Payment_ID@ (" + paymentId + ") @NotFound@");
			}
			payments.add(payment);
		}

		// V1 - Same currency: all payments must share same currency
		int firstPaymentCurrencyId = payments.get(0).getC_Currency_ID();
		for (MPayment payment : payments) {
			if (payment.getC_Currency_ID() != firstPaymentCurrencyId) {
				throw new AdempiereException("@C_Currency_ID@ @BankStatementMatch.NoMatchedFound@: "
					+ "@C_Payment_ID@ (" + payment.getC_Payment_ID() + " = " + payment.getCurrencyISO()
					+ ") vs (" + payments.get(0).getC_Payment_ID() + " = " + payments.get(0).getCurrencyISO() + ")"
				);
			}
		}
		// V1 - Same currency: all movements must share same currency, and match payments
		int firstMovementCurrencyId = importedMovements.get(0).getC_Currency_ID();
		for (X_I_BankStatement movement : importedMovements) {
			if (movement.getC_Currency_ID() != firstMovementCurrencyId) {
				throw new AdempiereException("@C_Currency_ID@ @BankStatementMatch.NoMatchedFound@: "
					+ "@I_BankStatement_ID@ (" + movement.getI_BankStatement_ID()
					+ ") @C_Currency_ID@ @NotMatched@"
				);
			}
		}
		if (firstPaymentCurrencyId != firstMovementCurrencyId) {
			throw new AdempiereException("@C_Currency_ID@ @BankStatementMatch.NoMatchedFound@: "
				+ "@C_Payment_ID@ (" + payments.get(0).getCurrencyISO()
				+ ") vs @I_BankStatement_ID@ (" + importedMovements.get(0).getC_Currency().getISO_Code() + ")"
			);
		}

		// V2 - Same direction: all payments same sign, all movements same sign, both match
		boolean firstPaymentIsReceipt = payments.get(0).isReceipt();
		for (MPayment payment : payments) {
			if (payment.isReceipt() != firstPaymentIsReceipt) {
				throw new AdempiereException("@IsReceipt@ @BankStatementMatch.NoMatchedFound@: "
					+ "@C_Payment_ID@ (" + payment.getC_Payment_ID() + ") @NotMatched@"
				);
			}
		}
		boolean firstMovementIsReceipt = importedMovements.get(0).getTrxAmt().compareTo(BigDecimal.ZERO) >= 0;
		for (X_I_BankStatement movement : importedMovements) {
			boolean movementIsReceipt = movement.getTrxAmt().compareTo(BigDecimal.ZERO) >= 0;
			if (movementIsReceipt != firstMovementIsReceipt) {
				throw new AdempiereException("@IsReceipt@ @BankStatementMatch.NoMatchedFound@: "
					+ "@I_BankStatement_ID@ (" + movement.getI_BankStatement_ID() + ") @NotMatched@"
				);
			}
		}
		if (firstPaymentIsReceipt != firstMovementIsReceipt) {
			throw new AdempiereException("@IsReceipt@ @BankStatementMatch.NoMatchedFound@: "
				+ "@C_Payment_ID@ (" + (firstPaymentIsReceipt ? "@IsReceipt@" : "@IsPayment@")
				+ ") vs @I_BankStatement_ID@ (" + (firstMovementIsReceipt ? "@IsReceipt@" : "@IsPayment@") + ")"
			);
		}

		// V3 - Balance: Sum(PayAmt adjusted) == Sum(TrxAmt), tolerance 0.01
		BigDecimal totalPayAmt = BigDecimal.ZERO;
		for (MPayment payment : payments) {
			BigDecimal payAmt = payment.getPayAmt();
			if (!payment.isReceipt()) {
				payAmt = payAmt.negate();
			}
			totalPayAmt = totalPayAmt.add(payAmt);
		}
		BigDecimal totalTrxAmt = BigDecimal.ZERO;
		for (X_I_BankStatement movement : importedMovements) {
			totalTrxAmt = totalTrxAmt.add(movement.getTrxAmt());
		}
		if (totalPayAmt.subtract(totalTrxAmt).abs().compareTo(new BigDecimal("0.01")) > 0) {
			throw new AdempiereException("@PayAmt@/@TrxAmt@ @BankStatementMatch.NoMatchedFound@: "
				+ "@PayAmt@ (" + totalPayAmt + ") vs @TrxAmt@ (" + totalTrxAmt + ")"
			);
		}

		// Verify no existing matches for these payments/movements
		for (MPayment payment : payments) {
			List<MBankStatementLineMatch> existingMatches = new Query(
				Env.getCtx(),
				I_C_BankStatementLineMatch.Table_Name,
				"C_Payment_ID = ?",
				null
			)
				.setParameters(payment.getC_Payment_ID())
				.setClient_ID()
				.list()
			;
			if (existingMatches != null && !existingMatches.isEmpty()) {
				throw new AdempiereException("@C_Payment_ID@ (" + payment.getC_Payment_ID()
					+ ") #" + payment.getDocumentNo() + " @AlreadyExists@ @C_BankStatementLineMatch_ID@"
				);
			}
		}

		// Create N*M C_BankStatementLineMatch records
		java.sql.Timestamp matchDate = new java.sql.Timestamp(System.currentTimeMillis());
		MultiPaymentMatchResponse.Builder responseBuilder = MultiPaymentMatchResponse.newBuilder();
		int recordCount = 0;

		for (X_I_BankStatement movement : importedMovements) {
			for (MPayment payment : payments) {
				MBankStatementLineMatch lineMatch = new MBankStatementLineMatch(Env.getCtx(), 0, null);
				lineMatch.setI_BankStatement_ID(movement.getI_BankStatement_ID());
				lineMatch.setC_Payment_ID(payment.getC_Payment_ID());
				lineMatch.setC_Currency_ID(payment.getC_Currency_ID());
				lineMatch.setIsManualMatch(true);
				lineMatch.setMatchDate(matchDate);
				lineMatch.saveEx();

				BankStatementLineMatch.Builder lineMatchBuilder = BankStatementMatchConvertUtil.convertBankStatementLineMatch(lineMatch);
				responseBuilder.addLineMatches(lineMatchBuilder);
				recordCount++;
			}
		}

		// Update each imported movement flags and build matching_movements
		for (X_I_BankStatement movement : importedMovements) {
			movement.setC_Payment_ID(payments.get(0).getC_Payment_ID());
			// movement.set_ValueOfColumn("IsMatched", true);
			movement.set_ValueOfColumn("IsManualMatch", true);
			movement.set_ValueOfColumn("IsMultiPaymentMatch", true);
			movement.saveEx();

			responseBuilder.addMatchingMovements(
				BankStatementMatchConvertUtil.convertMatchMovement(movement)
			);
		}

		responseBuilder.setMessage(String.valueOf(recordCount));
		return responseBuilder;
	}



	public static UnmatchMultiPaymentResponse.Builder unmatchMultiPayment(UnmatchMultiPaymentRequest request) {
		List<Integer> importedMovementIds = request.getImportedMovementIdsList();
		if (importedMovementIds == null || importedMovementIds.isEmpty()) {
			throw new AdempiereException("@FillMandatory@ @I_BankStatement_ID@");
		}

		// Collect payment IDs before deleting
		Set<Integer> paymentIds = new HashSet<>();
		for (int movementId : importedMovementIds) {
			List<MBankStatementLineMatch> lineMatches = BankStatementMatchUtil.getLineMatchesByImportedMovement(movementId);
			for (MBankStatementLineMatch lineMatch : lineMatches) {
				if (lineMatch.getC_Payment_ID() > 0) {
					paymentIds.add(lineMatch.getC_Payment_ID());
				}
			}
		}

		int totalDeleted = BankStatementMatchUtil.deleteLineMatchesByImportedMovements(importedMovementIds);

		// Reset flags on each imported movement
		for (int movementId : importedMovementIds) {
			X_I_BankStatement importedMovement = new X_I_BankStatement(Env.getCtx(), movementId, null);
			if (importedMovement == null || importedMovement.getI_BankStatement_ID() <= 0) {
				continue;
			}
			importedMovement.setC_Payment_ID(0);
			importedMovement.set_ValueOfColumn("IsMatched", false);
			importedMovement.set_ValueOfColumn("IsManualMatch", false);
			importedMovement.set_ValueOfColumn("IsMultiPaymentMatch", false);
			importedMovement.saveEx();
		}

		return UnmatchMultiPaymentResponse.newBuilder()
			.setMessage(String.valueOf(totalDeleted))
			.addAllImportedMovementIds(importedMovementIds)
			.addAllPaymentIds(paymentIds)
		;
	}



	public static ListBankStatementLineMatchesResponse.Builder listBankStatementLineMatches(ListBankStatementLineMatchesRequest request) {
		List<MBankStatementLineMatch> lineMatches;
		if (request.getImportedMovementId() > 0) {
			lineMatches = BankStatementMatchUtil.getLineMatchesByImportedMovement(
				request.getImportedMovementId()
			);
		} else if (request.getBankStatementLineId() > 0) {
			lineMatches = BankStatementMatchUtil.getLineMatchesByBankStatementLine(
				request.getBankStatementLineId()
			);
		} else {
			throw new AdempiereException("@FillMandatory@ @I_BankStatement_ID@ / @C_BankStatementLine_ID@");
		}

		ListBankStatementLineMatchesResponse.Builder builderList = ListBankStatementLineMatchesResponse.newBuilder()
			.setRecordCount(lineMatches.size())
		;
		for (MBankStatementLineMatch lineMatch : lineMatches) {
			builderList.addRecords(
				BankStatementMatchConvertUtil.convertBankStatementLineMatch(lineMatch)
			);
		}
		return builderList;
	}

}
