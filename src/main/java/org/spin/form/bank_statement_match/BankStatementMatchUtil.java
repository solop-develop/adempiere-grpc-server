package org.spin.form.bank_statement_match;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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


	public static String buildSQLImportedMovements(
		List<Object> filterParameters,
		boolean isMatchedMode,
		Timestamp dateFrom,
		Timestamp dateTo,
		BigDecimal paymentAmountFrom,
		BigDecimal paymentAmountTo
	) {
		StringBuffer sql = new StringBuffer(
			"SELECT p.I_BankStatement_ID, p.UUID, p.StatementLineDate, "
			+ "(CASE WHEN p.TrxAmt < 0 THEN 'N' ELSE 'Y' END) AS IsReceipt, "
			+ "p.ReferenceNo, p.C_BPartner_ID, "
			+ "(CASE WHEN p.C_BPartner_ID IS NULL THEN BPartnerValue ELSE bp.Name END) BPName, "
			+ "COALESCE(p.ISO_Code, c.ISO_Code) AS ISO_Code, p.TrxAmt, p.Memo "
			+ "FROM I_BankStatement p "
			+ "LEFT JOIN C_BPartner bp ON(bp.C_BPartner_ID = p.C_BPartner_ID) "
			+ "LEFT JOIN C_Currency c ON(c.C_Currency_ID = p.C_Currency_ID) "
		);
		//	Where Clause
		sql.append("WHERE p.C_BankAccount_ID = ? ");

		//	Match
		if (isMatchedMode) {
			sql.append(
				"AND (p.C_Payment_ID IS NOT NULL "
				+ "OR p.C_BPartner_ID IS NOT NULL "
				+ "OR p.C_Invoice_ID IS NOT NULL) "
			);
		} else {
			sql.append(
				"AND (p.C_Payment_ID IS NULL "
				+ "AND p.C_BPartner_ID IS NULL "
				+ "AND p.C_Invoice_ID IS NULL) "
			);
		}

		//	For parameters
		//	Date Trx
		if (dateFrom != null) {
			sql.append("AND p.StatementLineDate >= ? ");
			filterParameters.add(dateFrom);
		}
		if (dateTo != null) {
			sql.append("AND p.StatementLineDate <= ? ");
			filterParameters.add(dateTo);
		}

		//	Amount
		if (paymentAmountFrom != null) {
			sql.append("AND p.TrxAmt >= ? ");
			filterParameters.add(paymentAmountFrom);
		}
		if (paymentAmountTo != null) {
			sql.append("AND p.TrxAmt <= ? ");
			filterParameters.add(paymentAmountTo);
		}
		// role security
		sql = new StringBuffer(
			MRole.getDefault(Env.getCtx(), false).addAccessSQL(
				sql.toString(),
				"p",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			)
		);

		//	Order by
		sql.append(" ORDER BY p.StatementLineDate");

		return sql.toString();
	}


	public static Query buildPaymentQuery(
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
		ArrayList<Object> paymentFilters = new ArrayList<Object>();
		paymentFilters.add(bankAccountId);

		//	Match
		if(isMatchedMode) {
			whereClasuePayment += "AND EXISTS(SELECT 1 FROM I_BankStatement ibs "
				+ "WHERE ibs.C_Payment_ID = C_Payment_ID.C_Payment_ID) ";
		} else {
			whereClasuePayment += "AND NOT EXISTS(SELECT 1 FROM I_BankStatement ibs "
				+ "WHERE ibs.C_Payment_ID = C_Payment.C_Payment_ID) ";
		}

		//	Date Trx
		if (dateFrom != null) {
			whereClasuePayment += "AND p.DateTrx >= ? ";
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
			whereClasuePayment += "AND p.C_BPartner_ID = ? ";
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

		//	Match
		if(isMatchedMode) {
			whereClasueBankStatement += "AND (C_Payment_ID IS NOT NULL "
				+ "OR C_BPartner_ID IS NOT NULL "
				+ "OR C_Invoice_ID IS NOT NULL) ";
		} else {
			whereClasueBankStatement += "AND (C_Payment_ID IS NULL "
				+ "AND C_BPartner_ID IS NULL "
				+ "AND C_Invoice_ID IS NULL) ";
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

		Query paymentQuery = new Query(
			Env.getCtx(),
			I_I_BankStatement.Table_Name,
			whereClasueBankStatement,
			null
		)
			.setParameters(filterParameters)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOrderBy(I_I_BankStatement.COLUMNNAME_StatementLineDate)
		;

		return paymentQuery;
	}

}
