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
package org.spin.grpc.service.form.bank_statement_match;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.I_I_BankStatement;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBankAccount;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.bank_statement_match.MatchMode;

public class BankStatementMatchUtil {

	public static MBankAccount validateAndGetBankAccount(int bankAccountId) {
		if (bankAccountId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BankAccount_ID@");
		}
		// MBankAccount bankAccount = new Query(
		// 	Env.getCtx(),
		// 	I_C_BankAccount.Table_Name,
		// 	" C_BankAccount_ID = ? ",
		// 	null
		// )
		// 	.setParameters(bankAccountId)
		// 	.setClient_ID()
		// 	.first()
		// ;
		MBankAccount bankAccount = MBankAccount.get(Env.getCtx(), bankAccountId);
		if (bankAccount == null || bankAccount.getC_BankAccount_ID() <= 0) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}
		return bankAccount;
	}



	public static Query buildPaymentQuery(
		int bankStatementId,
		int bankAccountId,
		int matchMode,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo,
		int businessPartnerId,
		String searchValue
	) {
		String whereClasuePayment = "C_BankAccount_ID = ? "
			+ " AND DocStatus NOT IN('IP', 'DR') "
			+ " AND IsReconciled = 'N' "
		;

		if(bankStatementId > 0) {
			whereClasuePayment +=
				"AND NOT EXISTS("
					+ "SELECT 1 FROM C_BankStatement AS bs "
					+ "INNER JOIN C_BankStatementLine AS bsl "
						+ "ON bsl.C_BankStatement_ID = bs.C_BankStatement_ID "
					+ "WHERE bsl.C_Payment_ID = C_Payment.C_Payment_ID "
						+ "AND bs.DocStatus IN('CO', 'CL') "
						+ "AND bsl.C_BankStatement_ID <> " + bankStatementId
				+ ") "
			;
		}

		ArrayList<Object> paymentFilters = new ArrayList<Object>();
		paymentFilters.add(bankAccountId);

		//	Match
		if(matchMode == MatchMode.MODE_MATCHED_VALUE) {
			whereClasuePayment += "AND EXISTS("
				+ "SELECT 1 "
				+ "FROM I_BankStatement AS ibs "
				+ "WHERE ibs.C_Payment_ID = C_Payment.C_Payment_ID"
				// + "AND (COALESCE(ibs.IsManualMatch, 'N') = 'Y' OR COALESCE(ibs.IsMatched, 'N') = 'Y') "
			+ ") ";
		} else if (matchMode == MatchMode.MODE_NOT_MATCHED_VALUE) {
			whereClasuePayment += "AND ("
				+ "NOT EXISTS("
					+ "SELECT 1 "
					+ "FROM I_BankStatement AS ibs "
					+ "WHERE ibs.C_Payment_ID = C_Payment.C_Payment_ID "
					// + "OR (COALESCE(ibs.IsManualMatch, 'N') = 'Y' OR COALESCE(ibs.IsMatched, 'N') = 'N') "
				+ ") "
				+ "OR EXISTS("
					+ "SELECT 1 "
					+ "FROM I_BankStatement AS ibs "
					+ "WHERE ibs.C_Payment_ID = C_Payment.C_Payment_ID "
					+ "AND (COALESCE(ibs.IsManualMatch, 'N') = 'Y' OR COALESCE(ibs.IsMatched, 'N') = 'N') "
				+ ") "
			+ ") ";
		} else {
			// all mode MatchMode.MODE_ALL
		}

		//	Date Trx
		if (dateFrom != null) {
			whereClasuePayment += "AND DateTrx >= ? ";
			paymentFilters.add(dateFrom);
		}
		if (dateTo != null) {
			whereClasuePayment += "AND DateTrx <= ? ";
			paymentFilters.add(dateTo);
		}

		//	Amount (negate for payments IsReceipt='N' to match visual display)
		if (paymentAmountFrom != null) {
			whereClasuePayment += "AND (CASE WHEN IsReceipt = 'N' THEN -PayAmt ELSE PayAmt END) >= ? ";
			paymentFilters.add(paymentAmountFrom);
		}
		if (paymentAmountTo != null) {
			whereClasuePayment += "AND (CASE WHEN IsReceipt = 'N' THEN -PayAmt ELSE PayAmt END) <= ? ";
			paymentFilters.add(paymentAmountTo);
		}

		// Business Partner
		if (businessPartnerId > 0) {
			whereClasuePayment += "AND C_BPartner_ID = ? ";
			paymentFilters.add(businessPartnerId);
		}

		// Search Value
		if (!Util.isEmpty(searchValue, true)) {
			whereClasuePayment += "AND ("
					+ "UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' "
					+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%' "
				+ ") "
			;
			paymentFilters.add(searchValue);
			paymentFilters.add(searchValue);
		}

		Query paymentQuery = new Query(
			Env.getCtx(),
			I_C_Payment.Table_Name,
			whereClasuePayment,
			null
		)
			.setParameters(paymentFilters)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setClient_ID()
			.setOrderBy(I_C_Payment.COLUMNNAME_DateTrx)
		;

		return paymentQuery;
	}



	public static String buildPaymentSQL(
		int bankStatementId,
		int bankAccountId,
		int matchMode,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo,
		int businessPartnerId,
		String searchValue,
		List<Object> parametersList
	) {
		StringBuffer sql = new StringBuffer(
			"SELECT p.*, "
				+ "EXISTS("
					+ "SELECT 1 FROM I_BankStatement ibs "
					+ "WHERE ibs.C_Payment_ID = p.C_Payment_ID "
						// + "AND COALESCE(ibs.IsMatched, 'N') = 'Y' "
				+ ") AS IsMatched, "
				+ "EXISTS("
					+ "SELECT 1 FROM I_BankStatement ibs "
					+ "WHERE ibs.C_Payment_ID = p.C_Payment_ID "
						+ "AND COALESCE(ibs.IsManualMatch, 'N') = 'Y' "
				+ ") AS IsManualMatch "
			+ "FROM C_Payment p "
			+ "WHERE "
				+ "p.C_BankAccount_ID = ? "
				+ "AND p.DocStatus NOT IN('IP', 'DR') "
				+ "AND p.IsReconciled = 'N' "
		);

		parametersList.add(bankAccountId);

		if (bankStatementId > 0) {
			sql.append(
				"AND NOT EXISTS("
					+ "SELECT 1 FROM C_BankStatement AS bs "
					+ "INNER JOIN C_BankStatementLine AS bsl "
						+ "ON bsl.C_BankStatement_ID = bs.C_BankStatement_ID "
					+ "WHERE bsl.C_Payment_ID = p.C_Payment_ID "
						+ "AND bs.DocStatus IN('CO', 'CL') "
					+ "AND bsl.C_BankStatement_ID <> " + bankStatementId
				+ ") "
			);
		}

		//	Match
		if (matchMode == MatchMode.MODE_MATCHED_VALUE) {
			sql.append(
				"AND EXISTS("
					+ "SELECT 1 "
					+ "FROM I_BankStatement AS ibs "
					+ "WHERE ibs.C_Payment_ID = p.C_Payment_ID"
				+ ") "
			);
		} else if (matchMode == MatchMode.MODE_NOT_MATCHED_VALUE) {
			sql.append(
				"AND ("
					+ "NOT EXISTS("
						+ "SELECT 1 "
						+ "FROM I_BankStatement AS ibs "
						+ "WHERE ibs.C_Payment_ID = p.C_Payment_ID "
					+ ") "
					+ "OR EXISTS("
						+ "SELECT 1 "
						+ "FROM I_BankStatement AS ibs "
						+ "WHERE ibs.C_Payment_ID = p.C_Payment_ID "
						+ "AND (COALESCE(ibs.IsManualMatch, 'N') = 'Y' OR COALESCE(ibs.IsMatched, 'N') = 'N') "
					+ ") "
				+ ") "
			);
		}

		//	Date Trx
		if (dateFrom != null) {
			sql.append("AND p.DateTrx >= ? ");
			parametersList.add(dateFrom);
		}
		if (dateTo != null) {
			sql.append("AND p.DateTrx <= ? ");
			parametersList.add(dateTo);
		}

		//	Amount (negate for payments IsReceipt='N' to match visual display)
		if (paymentAmountFrom != null) {
			sql.append("AND (CASE WHEN p.IsReceipt = 'N' THEN -p.PayAmt ELSE p.PayAmt END) >= ? ");
			parametersList.add(paymentAmountFrom);
		}
		if (paymentAmountTo != null) {
			sql.append("AND (CASE WHEN p.IsReceipt = 'N' THEN -p.PayAmt ELSE p.PayAmt END) <= ? ");
			parametersList.add(paymentAmountTo);
		}

		// Business Partner
		if (businessPartnerId > 0) {
			sql.append("AND p.C_BPartner_ID = ? ");
			parametersList.add(businessPartnerId);
		}

		// Search Value
		if (!Util.isEmpty(searchValue, true)) {
			sql.append("AND ("
				+ "UPPER(p.DocumentNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(p.Description) LIKE '%' || UPPER(?) || '%' "
			+ ") ");
			parametersList.add(searchValue);
			parametersList.add(searchValue);
		}

		sql.append("ORDER BY p.DateTrx ");

		// role security
		return MRole.getDefault(Env.getCtx(), false).addAccessSQL(
			sql.toString(),
			"p",
			MRole.SQL_FULLYQUALIFIED,
			MRole.SQL_RO
		);
	}



	public static Query buildImportBankMovementQuery(
		int bankStatementId,
		int bankAccountId,
		int matchMode,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo,
		String searchValue
	) {
		String whereClasueBankStatement = "C_BankAccount_ID = ? ";

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccountId);

		if(bankStatementId > 0) {
			whereClasueBankStatement += "AND NOT EXISTS("
					+ "SELECT 1 FROM C_BankStatement AS bs "
					+ "INNER JOIN C_BankStatementLine AS bsl "
					+ "ON(bsl.C_BankStatement_ID = bs.C_BankStatement_ID) "
					+ "WHERE bsl.C_BankStatementLine_ID = I_BankStatement.C_BankStatementLine_ID "
					+ "AND bs.DocStatus IN('CO', 'CL') "
					+ "AND bsl.C_BankStatement_ID <> " + bankStatementId
				+ ") "
			;
		}

		//	Match
		if(matchMode == MatchMode.MODE_MATCHED_VALUE) {
			whereClasueBankStatement += "AND ("
				+ "(COALESCE(IsManualMatch, 'N') = 'Y' AND COALESCE(IsMatched, 'N') = 'Y') "
				+ "OR (C_Payment_ID IS NOT NULL "
				+ "OR C_BPartner_ID IS NOT NULL "
				+ "OR C_Invoice_ID IS NOT NULL) "
				+ ") "
			;
		} else if (matchMode == MatchMode.MODE_NOT_MATCHED_VALUE) {
			whereClasueBankStatement += "AND ("
				+ "(COALESCE(IsManualMatch, 'N') = 'Y' OR COALESCE(IsMatched, 'N') = 'N') "
				+ "OR (C_Payment_ID IS NULL "
				+ "AND C_BPartner_ID IS NULL "
				+ "AND C_Invoice_ID IS NULL) "
				+ ") "
			;
		} else {
			// all mode MatchMode.MODE_ALL
		}

		//	Date Trx
		if (dateFrom != null) {
			whereClasueBankStatement += "AND StatementLineDate >= ? ";
			filterParameters.add(dateFrom);
		}
		if (dateTo != null) {
			whereClasueBankStatement += "AND StatementLineDate <= ? ";
			filterParameters.add(dateTo);
		}

		//	Amount
		if (paymentAmountFrom != null) {
			whereClasueBankStatement += "AND TrxAmt >= ? ";
			filterParameters.add(paymentAmountFrom);
		}
		if (paymentAmountTo != null) {
			whereClasueBankStatement += "AND TrxAmt <= ? ";
			filterParameters.add(paymentAmountTo);
		}

		// Search Value
		if (!Util.isEmpty(searchValue, true)) {
			whereClasueBankStatement += "AND ("
				+ "UPPER(ReferenceNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(LineDescription) LIKE '%' || UPPER(?) || '%' "
			+ ") ";
			filterParameters.add(searchValue);
			filterParameters.add(searchValue);
		}

		Query importBankMovementQuery = new Query(
			Env.getCtx(),
			I_I_BankStatement.Table_Name,
			whereClasueBankStatement,
			null
		)
			.setParameters(filterParameters)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setClient_ID()
			.setOrderBy(I_I_BankStatement.COLUMNNAME_StatementLineDate)
		;

		return importBankMovementQuery;
	}



	public static Query buildResultMovementsQuery(
		int bankStatementId,
		int bankAccountId,
		int matchMode,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo
	) {
		String whereClasueBankStatement = "C_BankAccount_ID = ? ";

		if(bankStatementId > 0) {
			whereClasueBankStatement += "AND NOT EXISTS("
				+ "SELECT 1 FROM C_BankStatement AS bs "
				+ "INNER JOIN C_BankStatementLine AS bsl "
				+ "ON(bsl.C_BankStatement_ID = bs.C_BankStatement_ID) "
				+ "WHERE bsl.C_Payment_ID = I_BankStatement.C_Payment_ID "
				+ "AND bs.DocStatus IN('CO', 'CL') "
				+ "AND bsl.C_BankStatement_ID <> " + bankStatementId 
			+ ") ";
		}

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccountId);

		//	Date Trx
		if (dateFrom != null) {
			whereClasueBankStatement += "AND StatementLineDate >= ? ";
			filterParameters.add(dateFrom);
		}
		if (dateTo != null) {
			whereClasueBankStatement += "AND StatementLineDate <= ? ";
			filterParameters.add(dateTo);
		}

		//	Amount
		if (paymentAmountFrom != null) {
			whereClasueBankStatement += "AND TrxAmt >= ? ";
			filterParameters.add(paymentAmountFrom);
		}
		if (paymentAmountTo != null) {
			whereClasueBankStatement += "AND TrxAmt <= ? ";
			filterParameters.add(paymentAmountTo);
		}

		Query resultMovementsQuery = new Query(
			Env.getCtx(),
			I_I_BankStatement.Table_Name,
			whereClasueBankStatement,
			null
		)
			.setParameters(filterParameters)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setClient_ID()
			.setOrderBy(I_I_BankStatement.COLUMNNAME_StatementLineDate)
		;

		return resultMovementsQuery;
	}

}
