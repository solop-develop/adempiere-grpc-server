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

import org.adempiere.core.domains.models.I_C_BankAccount;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.I_I_BankStatement;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBankAccount;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class BankStatementMatchUtil {

	public static MBankAccount validateAndGetBankAccount(int bankAccountId) {
		if (bankAccountId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BankAccount_ID@");
		}
		MBankAccount bankAccount = new Query(
			Env.getCtx(),
			I_C_BankAccount.Table_Name,
			" C_BankAccount_ID = ? ",
			null
		)
			.setParameters(bankAccountId)
			.setClient_ID()
			.first();
		if (bankAccount == null || bankAccount.getC_BankAccount_ID() <= 0) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}
		return bankAccount;
	}


	public static Query buildPaymentQuery(
		int bankStatementId,
		int bankAccountId,
		boolean isMatchedMode,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo,
		int businessPartnerId
	) {
		String whereClasuePayment = "C_BankAccount_ID = ? "
			+ " AND DocStatus NOT IN('IP', 'DR') "
			+ " AND IsReconciled = 'N' "
		;

		if(bankStatementId > 0) {
			whereClasuePayment += "AND NOT EXISTS(SELECT 1 FROM C_BankStatement bs "
				+ "INNER JOIN C_BankStatementLine bsl "
				+ "ON(bsl.C_BankStatement_ID = bs.C_BankStatement_ID) "
				+ "WHERE bsl.C_Payment_ID = C_Payment.C_Payment_ID "
				+ "AND bs.DocStatus IN('CO', 'CL') "
				+ "AND bsl.C_BankStatement_ID <> " + bankStatementId + ") "
			;
		}

		ArrayList<Object> paymentFilters = new ArrayList<Object>();
		paymentFilters.add(bankAccountId);

		//	Match
		// if(isMatchedMode) {
		// 	whereClasuePayment += "AND EXISTS(SELECT 1 FROM I_BankStatement ibs "
		// 		+ "WHERE ibs.C_Payment_ID = C_Payment.C_Payment_ID) ";
		// } else {
		// 	whereClasuePayment += "AND NOT EXISTS(SELECT 1 FROM I_BankStatement ibs "
		// 		+ "WHERE ibs.C_Payment_ID = C_Payment.C_Payment_ID) ";
		// }

		//	Date Trx
		if (dateFrom != null) {
			whereClasuePayment += "AND DateTrx >= ? ";
			paymentFilters.add(dateFrom);
		}
		if (dateTo != null) {
			whereClasuePayment += "AND DateTrx <= ? ";
			paymentFilters.add(dateTo);
		}

		//	Amount
		if (paymentAmountFrom != null) {
			whereClasuePayment += "PayAmt >= ? ";
			paymentFilters.add(paymentAmountFrom);
		}
		if (paymentAmountTo != null) {
			whereClasuePayment += "PayAmt <= ? ";
			paymentFilters.add(paymentAmountTo);
		}

		// Business Partner
		if (businessPartnerId > 0) {
			whereClasuePayment += "AND C_BPartner_ID = ? ";
			paymentFilters.add(businessPartnerId);
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


	public static Query buildBankMovementQuery(
		int bankStatementId,
		int bankAccountId,
		boolean isMatchedMode,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo
	) {
		String whereClasueBankStatement = "C_BankAccount_ID = ? ";

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccountId);

		if(bankStatementId > 0) {
			whereClasueBankStatement += "AND NOT EXISTS(SELECT 1 FROM C_BankStatement bs "
				+ "INNER JOIN C_BankStatementLine bsl "
				+ "ON(bsl.C_BankStatement_ID = bs.C_BankStatement_ID) "
				+ "WHERE bsl.C_BankStatementLine_ID = I_BankStatement.C_BankStatementLine_ID "
				+ "AND bs.DocStatus IN('CO', 'CL') "
				+ "AND bsl.C_BankStatement_ID <> " + bankStatementId + ") "
			;
		}

		//	Match
		// if(isMatchedMode) {
		// 	whereClasueBankStatement += "AND (C_Payment_ID IS NOT NULL "
		// 		+ "OR C_BPartner_ID IS NOT NULL "
		// 		+ "OR C_Invoice_ID IS NOT NULL) ";
		// } else {
		// 	whereClasueBankStatement += "AND (C_Payment_ID IS NULL "
		// 		+ "AND C_BPartner_ID IS NULL "
		// 		+ "AND C_Invoice_ID IS NULL) ";
		// }

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

		Query paymentQuery = new Query(
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

		return paymentQuery;
	}


	public static Query buildResultMovementsQuery(
		int bankStatementId,
		int bankAccountId,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo
	) {
		String whereClasueBankStatement = "C_BankAccount_ID = ? ";

		if(bankStatementId > 0) {
			whereClasueBankStatement += "AND NOT EXISTS(SELECT 1 FROM C_BankStatement bs "
				+ "INNER JOIN C_BankStatementLine bsl "
				+ "ON(bsl.C_BankStatement_ID = bs.C_BankStatement_ID) "
				+ "WHERE bsl.C_Payment_ID = I_BankStatement.C_Payment_ID "
				+ "AND bs.DocStatus IN('CO', 'CL') "
				+ "AND bsl.C_BankStatement_ID <> " + bankStatementId + ") "
			;
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

		Query paymentQuery = new Query(
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

		return paymentQuery;
	}

}
