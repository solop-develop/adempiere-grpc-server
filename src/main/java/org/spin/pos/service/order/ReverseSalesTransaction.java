/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.pos.service.order;

import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.base.util.DocumentUtil;
import org.spin.pos.service.cash.CashManagement;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.util.value.BooleanManager;

/**
 * This class was created for Reverse Sales Transaction
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ReverseSalesTransaction {

	/**
	 * Create a Return order and cancel all payments
	 * @param pos
	 * @param sourceOrderId
	 * @param description
	 * @return
	 */
	public static MOrder returnSalesOrder(MPOS pos, int sourceOrderId, String description, boolean processDocuments, String manualInvoiceDocumentNo, String manualShipmentDocumentNo, String manualMovementDocumentNo) {
		AtomicReference<MOrder> returnOrderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder sourceOrder = new MOrder(Env.getCtx(), sourceOrderId, transactionName);
  			if (sourceOrder.isReturnOrder()) {
				throw new AdempiereException("@POSReturnDocumentType_ID@ @smenu.customer.returned.order@");
			}
			//	Validate source document
			if(DocumentUtil.isDrafted(sourceOrder) 
					|| DocumentUtil.isClosed(sourceOrder)
					|| !OrderUtil.isValidOrder(sourceOrder)) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}

			CashManagement.validatePreviousCashClosing(pos, sourceOrder.getDateOrdered(), transactionName);

			MOrder returnOrder = RMAUtil.copyRMAFromOrder(pos, sourceOrder, transactionName);
			if(!Util.isEmpty(description, true)) {
				returnOrder.setDescription(description);
			} else {
				returnOrder.setDescription(sourceOrder.getDocumentNo());
			}
			returnOrder.saveEx();
			RMAUtil.createReturnOrderLines(sourceOrder, returnOrder, transactionName);
			RMAUtil.createReversedPayments(pos, sourceOrder, returnOrder, transactionName);

			//	Process return Order
			if (processDocuments) {
				// process and generate documents
				returnOrder = processReverseSalesOrder(
					pos,
					sourceOrder,
					returnOrder,
					manualInvoiceDocumentNo,
					manualShipmentDocumentNo,
					manualMovementDocumentNo,
					transactionName
				);
			}
			returnOrderReference.set(returnOrder);
		});
		return returnOrderReference.get();
	}

	/**
	 * Create return order
	 * @param pos
	 * @param sourceOrder
	 * @param transactionName
	 * @return
	 */
	public static MOrder processReverseSalesOrder(MPOS pos, MOrder sourceOrder, MOrder returnOrder, String manualInvoiceDocumentNo, String manualShipmentDocumentNo, String manualMovementDocumentNo,  String transactionName) {
		CashManagement.validatePreviousCashClosing(pos, sourceOrder.getDateOrdered(), transactionName);

		final boolean isManualReturnOrder = returnOrder.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsManualDocument);
		final boolean isManualSalesOrder = sourceOrder.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsManualDocument);
		/*if (isManualSalesOrder != isManualReturnOrder) {
			throw new AdempiereException(
				"@M_RMA_ID@ (" + returnOrder.getDocumentNo() + ") @IsManualDocument@:" + BooleanManager.getBooleanToTranslated(isManualReturnOrder)
				+ " | " +
				"@C_Order_ID@ (" + sourceOrder.getDocumentNo() + ") @IsManualDocument@:" + BooleanManager.getBooleanToTranslated(isManualSalesOrder)
			);
		}*/
		if (isManualReturnOrder) {
			returnOrder.set_ValueOfColumn("ManualInvoiceDocumentNo", manualInvoiceDocumentNo);
			returnOrder.set_ValueOfColumn("ManualShipmentDocumentNo", manualShipmentDocumentNo);
			// salesOrder.set_ValueOfColumn("ManualMovementDocumentNo", manualMovementDocumentNo);
			returnOrder.saveEx(transactionName);
		}

		//	Close all
		if(!sourceOrder.processIt(MOrder.DOCACTION_Close)) {
			throw new AdempiereException("@ProcessFailed@ :" + sourceOrder.getProcessMsg());
		}
		sourceOrder.saveEx();
		if(!returnOrder.processIt(MOrder.DOCACTION_Close)) {
			throw new AdempiereException("@ProcessFailed@ :" + returnOrder.getProcessMsg());
		}
		returnOrder.saveEx();

		//	Generate Return
		RMAUtil.generateReturnFromRMA(returnOrder, transactionName);
		//	Generate Credit Memo
		RMAUtil.generateCreditMemoFromRMA(returnOrder, transactionName);

		OrderManagement.processPayments(returnOrder, pos, true, transactionName);

		return returnOrder;
	}

}
