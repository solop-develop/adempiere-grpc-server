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

package org.spin.pos.service.order;

import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.spin.backend.grpc.pos.Order;
import org.spin.backend.grpc.pos.ProcessReverseSalesRequest;
import org.spin.backend.grpc.pos.ReverseSalesRequest;
import org.spin.pos.service.payment.PaymentManagement;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.OrderConverUtil;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Sales Order / RMA Logic for backend of Point Of Sales form
 */
public class OrderServiceLogic {

	/**
	 * Reverse Sales Transaction
	 * @param request ReverseSalesRequest
	 * @return Order.Builder
	 */
	public static Order.Builder reverseSalesTransaction(ReverseSalesRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_POS_ID@");
		}
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if (pos.getC_POS_ID() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		int orderId = request.getId();
		if (orderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@");
		}

		MOrder returnOrder = null;
		// Exists Online Payment Approved
		boolean isOnlinePaymentApproved = PaymentManagement.isOrderWithOnlinePaymentApproved(
			orderId
		);
		returnOrder = ReverseSalesTransaction.returnSalesOrder(
			pos,
			orderId,
			request.getDescription(),
			!isOnlinePaymentApproved
		);
		//	Default
		Order.Builder builder = OrderConverUtil.convertOrder(returnOrder);

		return builder;
	}


	/**
	 * Reverse Sales Transaction
	 * @param request ReverseSalesRequest
	 * @return Order.Builder
	 */
	public static Order.Builder processReverseSales(ProcessReverseSalesRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_POS_ID@");
		}
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if (pos.getC_POS_ID() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		int orderId = request.getId();
		if (orderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@");
		}

		AtomicReference<MOrder> returnOrderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder returnOrder = new MOrder(Env.getCtx(), request.getId(), transactionName);

			MOrder sourceOrder = new MOrder(Env.getCtx(), request.getSourceOrderId(), transactionName);
			returnOrder = ReverseSalesTransaction.processReverseSalesOrder(
				pos,
				sourceOrder,
				returnOrder,
				transactionName
			);
			returnOrderReference.set(returnOrder);
		});
		//	Default
		Order.Builder builder = OrderConverUtil.convertOrder(
			returnOrderReference.get()
		);
		return builder;
	}

}
