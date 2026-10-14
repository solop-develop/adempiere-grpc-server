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
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.base.util.DocumentUtil;
import org.spin.pos.service.cash.CashManagement;
import org.spin.pos.util.ColumnsAdded;

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
	public static MOrder returnSalesOrder(MPOS pos, int sourceOrderId, String description, boolean processDocuments, boolean isManualDocument, int manualDocumentTypeId, String manualInvoiceDocumentNo, String manualShipmentDocumentNo, String manualMovementDocumentNo) {
		AtomicReference<MOrder> returnOrderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder sourceOrder = new MOrder(Env.getCtx(), sourceOrderId, transactionName);
			//	Wait for a concurrent reverse of the same order and read its result
			RMAUtil.lockAndReload(sourceOrder, transactionName);
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

			//	Reuse a reverse of this order still waiting to be processed (e.g. online refund pending),
			//	a second one would create other reversed payments and request another refund
			MOrder returnOrder = getOpenReverseOrder(sourceOrder, transactionName);
			if (returnOrder == null) {
				//	A reverse returns the whole sale and all its payments, not allowed after a return by product
				validateWithoutReturns(sourceOrder, transactionName);
				returnOrder = RMAUtil.copyRMAFromOrder(pos, sourceOrder, transactionName);
				if(!Util.isEmpty(description, true)) {
					returnOrder.setDescription(description);
				} else {
					returnOrder.setDescription(sourceOrder.getDocumentNo());
				}
				returnOrder.saveEx();
				RMAUtil.createReturnOrderLines(sourceOrder, returnOrder, transactionName);
				RMAUtil.createReversedPayments(pos, sourceOrder, returnOrder, isManualDocument, transactionName);
			}

			//	Process return Order
			if (processDocuments) {
				// process and generate documents
				returnOrder = processReverseSalesOrder(
					pos,
					sourceOrder,
					returnOrder,
					manualDocumentTypeId,
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
	 * Get the open (not processed) return order of the source order created by a previous reverse
	 * @param sourceOrder
	 * @param transactionName
	 * @return the open reverse or null when there is none
	 */
	private static MOrder getOpenReverseOrder(MOrder sourceOrder, String transactionName) {
		MOrder openReturnOrder = new Query(
			sourceOrder.getCtx(),
			I_C_Order.Table_Name,
			ColumnsAdded.COLUMNNAME_ECA14_Source_Order_ID + " = ? AND DocStatus IN('DR','IP') AND Processed = 'N'",
			transactionName
		)
			.setParameters(sourceOrder.getC_Order_ID())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(I_C_Order.COLUMNNAME_C_Order_ID + " DESC")
			.first()
		;
		if (openReturnOrder == null || openReturnOrder.getC_Order_ID() <= 0) {
			return null;
		}
		//	A return without reversed payments is a draft of return by product, not a reverse
		boolean isReverse = new Query(
			sourceOrder.getCtx(),
			I_C_Payment.Table_Name,
			I_C_Payment.COLUMNNAME_C_Order_ID + " = ?",
			transactionName
		)
			.setParameters(openReturnOrder.getC_Order_ID())
			.match()
		;
		if (!isReverse) {
			throw new AdempiereException(
				"@ActionNotAllowedHere@ (@M_RMA_ID@: " + openReturnOrder.getDocumentNo() + ")"
			);
		}
		return openReturnOrder;
	}

	/**
	 * Validate that the source order has no completed or closed return orders
	 * @param sourceOrder
	 * @param transactionName
	 */
	private static void validateWithoutReturns(MOrder sourceOrder, String transactionName) {
		String documentNos = new Query(
			sourceOrder.getCtx(),
			I_C_Order.Table_Name,
			ColumnsAdded.COLUMNNAME_ECA14_Source_Order_ID + " = ? AND DocStatus IN('CO','CL')",
			transactionName
		)
			.setParameters(sourceOrder.getC_Order_ID())
			.setClient_ID()
			.<MOrder>list()
			.stream()
			.map(MOrder::getDocumentNo)
			.collect(Collectors.joining(", "))
		;
		if (!Util.isEmpty(documentNos, true)) {
			throw new AdempiereException(
				"@ActionNotAllowedHere@ (@M_RMA_ID@: " + documentNos + ")"
			);
		}
	}

	/**
	 * Create return order
	 * @param pos
	 * @param sourceOrder
	 * @param transactionName
	 * @return
	 */
	public static MOrder processReverseSalesOrder(MPOS pos, MOrder sourceOrder, MOrder returnOrder, int manualDocumentTypeId, String manualInvoiceDocumentNo, String manualShipmentDocumentNo, String manualMovementDocumentNo,  String transactionName) {
		//	Wait for a concurrent process of the same return order (double click or retry)
		RMAUtil.lockAndReload(returnOrder, transactionName);
		if (returnOrder.isProcessed()) {
			//	Already processed, the return and credit memo exist
			return returnOrder;
		}
		CashManagement.validatePreviousCashClosing(pos, sourceOrder.getDateOrdered(), transactionName);

		if (manualDocumentTypeId > 0) {
			// To change IsManual on Order by Document type
			returnOrder.setC_DocTypeTarget_ID(manualDocumentTypeId);
			returnOrder.saveEx(transactionName);
		}
		final boolean isManualReturnOrder = returnOrder.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsManualDocument);
		/*
		final boolean isManualSalesOrder = sourceOrder.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsManualDocument);
		if (isManualSalesOrder != isManualReturnOrder) {
			throw new AdempiereException(
				"@M_RMA_ID@ (" + returnOrder.getDocumentNo() + ") @IsManualDocument@:" + BooleanManager.getBooleanToTranslated(isManualReturnOrder)
				+ " | " +
				"@C_Order_ID@ (" + sourceOrder.getDocumentNo() + ") @IsManualDocument@:" + BooleanManager.getBooleanToTranslated(isManualSalesOrder)
			);
		}
		*/
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
