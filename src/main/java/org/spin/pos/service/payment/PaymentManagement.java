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

package org.spin.pos.service.payment;

import org.adempiere.core.domains.models.I_C_Payment;
import org.compiere.model.MOrder;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Payment Management for backend of Point Of Sales form
 */
public class PaymentManagement {

	public static boolean isOrderWithOnlinePaymentApproved(int salesOrderId) {
		if (salesOrderId <= 0) {
			return false;
		}
		MOrder salesOrder = new MOrder(Env.getCtx(), salesOrderId, null);
		if (salesOrder == null || salesOrder.getC_Order_ID() <= 0) {
			return false;
		}
		// Exists Online Payment Approved
		final String sql = "SELECT 1 "
			+ "FROM C_Payment "
			+ "WHERE IsOnline = 'Y' "
			+ "AND ResponseStatus = 'A' "
			+ "AND C_Order_ID = ? "
			+ "LIMIT 1"
		;
		boolean isOnlinePaymentApproved = 1 == DB.getSQLValue(null, sql, salesOrderId);
		return isOnlinePaymentApproved;
	}

	public static int isOrderWithoutOnlinePaymentApproved(int salesOrderId) {
		if (salesOrderId <= 0) {
			return 0;
		}
		MOrder salesOrder = new MOrder(Env.getCtx(), salesOrderId, null);
		if (salesOrder == null || salesOrder.getC_Order_ID() <= 0) {
			return 0;
		}
		// Exists Online Payment Approved
		final String whereClause = "IsOnline = 'Y' "
			+ "AND ResponseStatus <> 'A' "
			// + "AND IsReceipt = ? "
			+ "AND C_Order_ID = ? "
		;

		int countRecords = new Query(
			Env.getCtx(),
			I_C_Payment.Table_Name,
			whereClause,
			null
		)
			.setParameters(salesOrder.getC_Order_ID())
			.count()
		;
		
		return countRecords;
	}


}
