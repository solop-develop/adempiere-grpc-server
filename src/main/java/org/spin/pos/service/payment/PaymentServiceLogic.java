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

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPayment;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.backend.grpc.pos.ListApprovedPaymentsOnlineRequest;
import org.spin.backend.grpc.pos.ListPaymentsResponse;
import org.spin.backend.grpc.pos.Payment;
import org.spin.pos.util.PaymentConvertUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.StringManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Payment Service Logic for backend of Point Of Sales form
 */
public class PaymentServiceLogic {

	/**
	 * List Payments from Sales Order or POS
	 * @param request
	 * @return
	 */
	public static ListPaymentsResponse.Builder listApprovedPaymentsOnline(ListApprovedPaymentsOnlineRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}

		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer()
			.append("C_Payment.IsOnline = 'Y' ")
			.append("AND C_Payment.ResponseStatus = 'A' ")
		;
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	Aisle Seller
		int posId = request.getPosId();
		int orderId = request.getOrderId();
		//	For order
		if(orderId > 0) {
			whereClause.append("AND C_Payment.C_Order_ID = ? ");
			parameters.add(orderId);
		} else {
			whereClause.append("AND C_Payment.C_POS_ID = ? ");
			parameters.add(posId);
			whereClause.append("AND C_Payment.C_Charge_ID IS NOT NULL AND C_Payment.Processed = 'N' ");
		}
		if(request.getIsOnlyRefund()) {
			whereClause.append("AND C_Payment.IsReceipt = 'N' ");
		}
		if(request.getIsOnlyReceipt()) {
			whereClause.append("AND C_Payment.IsReceipt = 'Y' ");
		}
		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			I_C_Payment.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;

		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		int count = query.count();
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListPaymentsResponse.Builder builderList = ListPaymentsResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query
			.setLimit(limit, offset)
			.<MPayment>list()
			.forEach(payment -> {
				Payment.Builder paymentBuilder = PaymentConvertUtil.convertPayment(
					payment
				);
				builderList.addPayments(paymentBuilder);
			})
		;
		return builderList;
	}

}
