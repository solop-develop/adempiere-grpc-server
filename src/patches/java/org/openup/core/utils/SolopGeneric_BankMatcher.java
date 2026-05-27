/**************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                               *
 * This program is free software; you can redistribute it and/or modify it    		  *
 * under the terms version 2 or later of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope           *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                   *
 * See the GNU General Public License for more details.                               *
 * You should have received a copy of the GNU General Public License along            *
 * with this program; if not, printLine to the Free Software Foundation, Inc.,        *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                             *
 * For the text or an alternative of this public license, you may reach us            *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved.  *
 * Contributor: Yamel Senih ysenih@erpya.com                                          *
 * Contributor: Carlos Parada cparada@erpya.com                                       *
 * See: www.erpya.com                                                                 *
 *************************************************************************************/
package org.openup.core.utils;

import org.adempiere.core.domains.models.X_I_BankStatement;
import org.compiere.impexp.BankStatementMatchInfo;
import org.compiere.impexp.BankStatementMatcherInterface;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MBankStatementMatcher;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Add matcher by reference
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * <li> FR [ 1807 ] Add Match class for Reference No
 * @see https://github.com/adempiere/adempiere/issues/1807
 */
public class SolopGeneric_BankMatcher implements BankStatementMatcherInterface {

	private BigDecimal matchTolerance = Env.ZERO;

	public SolopGeneric_BankMatcher() {

	}

	@Override
	public void configure(MBankStatementMatcher definition) {
		if (definition == null) {
			return;
		}
		BigDecimal tolerance = definition.getMatchTolerance();
		matchTolerance = tolerance != null ? tolerance : Env.ZERO;
	}

	@Override
	public BankStatementMatchInfo findMatch(MBankStatementLine bsl, List<Integer> includedPayments, List<Integer> exludedPayments) {
		return null;
	}

	@Override
	public BankStatementMatchInfo findMatch(X_I_BankStatement ibs, List<Integer> includedPayments, List<Integer> exludedPayments) {
		StringBuffer paymentWhereClause = new StringBuffer();
		if(includedPayments != null
				&& includedPayments.size() > 0) {
			paymentWhereClause.append(" AND ").append("p.C_Payment_ID").append(" IN").append(includedPayments.toString().replace('[','(').replace(']',')'));
		}
		if(exludedPayments != null
				&& exludedPayments.size() > 0) {
			paymentWhereClause.append(" AND ").append("p.C_Payment_ID").append(" NOT IN").append(exludedPayments.toString().replace('[','(').replace(']',')'));
		}
		BankStatementMatchInfo info = new BankStatementMatchInfo();
		//	Validate
		if(ibs.getC_Payment_ID() != 0) {
			return info;
		}
		//	
		String ORDERVALUE = " DESC NULLS LAST";
		StringBuffer sql = new StringBuffer("SELECT p.C_Payment_ID "
				+ "FROM C_Payment p "
				+ "WHERE p.AD_Client_ID = ? ");
		if(paymentWhereClause.length() > 0) {
			sql.append(paymentWhereClause);
		}
		//	Were
		StringBuffer where = new StringBuffer();
		StringBuffer orderByClause = new StringBuffer(" ORDER BY ");
		//	Search criteria
		List<Object> params = new ArrayList<Object>();
		//	Client
		params.add(ibs.getAD_Client_ID());
		//	For reference

		// Openup Solutions - #11240 - Raúl Capecce
		// Se quita filtro por número de documento
//		if(!Util.isEmpty(ibs.getReferenceNo())) {
//			where.append("? = TRIM(p.CheckNo) ");
//			where.append("OR ? = TRIM(p.DocumentNo) ");
//			params.add(ibs.getReferenceNo().trim());
//			params.add(ibs.getReferenceNo().trim());
//		}


		//	Add
//		if(where.length() > 0) {
//			where.insert(0, "AND (").append(")");
//		}
		//	Add Currency
		if(!Util.isEmpty(ibs.getISO_Code())) {
			where.append(" AND EXISTS(SELECT 1 FROM C_Currency c WHERE c.C_Currency_ID = p.C_Currency_ID AND c.ISO_Code = ?) ");
			params.add(ibs.getISO_Code());
		} else if(ibs.getC_Currency_ID() != 0){
			where.append(" AND p.C_Currency_ID = ? ");
			params.add(ibs.getC_Currency_ID());
		}
		//	For Amount
		if(where.length() > 0) {
			where.append(" AND ");
		}
		//	Validate amount for it
		boolean isReceipt = ibs.getTrxAmt().compareTo(Env.ZERO) > 0;
		BigDecimal targetAmt = isReceipt ? ibs.getTrxAmt() : ibs.getTrxAmt().negate();
		if (matchTolerance.signum() > 0) {
			where.append("(p.PayAmt BETWEEN ? AND ? ");
			params.add(targetAmt.subtract(matchTolerance));
			params.add(targetAmt.add(matchTolerance));
		} else {
			where.append("(p.PayAmt = ? ");
			params.add(targetAmt);
		}
		//	Add Receipt
		where.append("AND p.IsReceipt = ? )");
		params.add(isReceipt);
		//	For Account
		if(where.length() > 0) {
			where.append(" AND ");
		}
		where.append("(p.C_BankAccount_ID = ?)");
		params.add(ibs.getC_BankAccount_ID());
		//	Additional validation
		// Same as `p.DocStatus IN('CO', 'CL', 'VO', 'RE')`
		// but avoiding using states that may not exist in some document types.
		where.append(" AND p.Processed = 'Y'");
		where.append(" AND p.IsReconciled = 'N'");
		where.append(" AND NOT EXISTS(SELECT 1 FROM I_BankStatement i WHERE i.C_Payment_ID = p.C_Payment_ID) ");
		// where.append("AND NOT EXISTS(")
		// 	.append("SELECT 1 FROM C_BankStatementLine bsl ")
		// 	.append("INNER JOIN C_BankStatement bs ON bs.C_BankStatement_ID = bsl.C_BankStatement_ID ")
		// 	.append("WHERE bsl.C_Payment_ID = p.C_Payment_ID ")
		// 	.append("AND bs.DocStatus IN ('CO', 'CL') ")
		// 	.append(") ")
		// ;

		//	Add Where Clause
		sql.append(where);

		//	Add Order By
		orderByClause.append("p.DocumentNo").append(ORDERVALUE);
		orderByClause.append(", p.CheckNo").append(ORDERVALUE);
		orderByClause.append(", p.DateTrx ASC");
		orderByClause.append(", p.Description").append(ORDERVALUE);
		sql.append(orderByClause);
		//	Find payment
		int paymentId = DB.getSQLValue(ibs.get_TrxName(), sql.toString(), params);
		//	set if exits
		if(paymentId > 0) {
			info.setC_Payment_ID(paymentId);
		}
		return info;
	}
}
