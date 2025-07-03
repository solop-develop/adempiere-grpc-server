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
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.spin.pos.service.pos.POS;

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


	public static void setDocumentType(MPOS pointOfSalesDefinition, MPayment payment, PO paymentTypeAllocation, String transactionName) {
		if (paymentTypeAllocation == null) {
			paymentTypeAllocation = POS.getPaymentMethodAllocation(
				payment.getC_PaymentMethod_ID(),
				pointOfSalesDefinition.getC_POS_ID(),
				transactionName
			);
		}

		String documentTypeColumnName = payment.isReceipt() ? "POSCollectingDocumentType_ID" : "POSRefundDocumentType_ID";
		int documentTypeId = pointOfSalesDefinition.get_ValueAsInt(documentTypeColumnName);
		if (payment.isReceipt()) {
			// TODO: Rename this column as `POSCollectingDocumentType_ID`
			if(paymentTypeAllocation.get_ValueAsInt("C_DocTypeTarget_ID") > 0) {
				documentTypeId = paymentTypeAllocation.get_ValueAsInt("C_DocTypeTarget_ID");
			}
		} else {
			if(paymentTypeAllocation.get_ValueAsInt("POSRefundDocumentType_ID") > 0) {
				documentTypeId = paymentTypeAllocation.get_ValueAsInt("POSRefundDocumentType_ID");
			}
		}

		if(documentTypeId > 0) {
			payment.setC_DocType_ID(documentTypeId);
		} else {
			payment.setC_DocType_ID(payment.isReceipt());
		}
	}

}
