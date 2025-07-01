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

import org.adempiere.exceptions.AdempiereException;
import org.spin.backend.grpc.pos.ExistsUnapprovedOnlinePaymentsRequest;
import org.spin.backend.grpc.pos.ExistsUnapprovedOnlinePaymentsResponse;

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
	public static ExistsUnapprovedOnlinePaymentsResponse.Builder existsUnapprovedOnlinePayments(ExistsUnapprovedOnlinePaymentsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if (request.getOrderId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		int countRecords = PaymentManagement.isOrderWithoutOnlinePaymentApproved(
			request.getOrderId()
		);

		ExistsUnapprovedOnlinePaymentsResponse.Builder builder = ExistsUnapprovedOnlinePaymentsResponse.newBuilder()
			.setRecordCount(
				countRecords
			)
		;

		return builder;
	}

}
