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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.spin.pos.service.cash.CashUtil;
import org.spin.pos.service.order.RMAUtil;
import org.spin.pos.service.order.ReturnSalesOrder;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TimeManager;

public class GiftCardManagement {

	public static void processingGiftCard(int giftCardId) {
		if (giftCardId <= 0) {
			throw new AdempiereException("@FillMandatory@ @ECA14_GiftCard_ID@");
		}
		// TODO: Support with lines
		PO giftCard = RecordUtil.getEntity(
			Env.getCtx(),
			"ECA14_GiftCard",
			giftCardId,
			null
		);
		if (giftCard == null || giftCard.get_ID() <= 0) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @NotFound@");
		}
		if (giftCard.get_ValueAsBoolean("Processed")) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processed@");
		}
		if (giftCard.get_ValueAsBoolean("Processing")) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processing@");
		}
		giftCard.set_ValueOfColumn("Processing", true);
		giftCard.saveEx();
	}

	public static void unProcessingGiftCard(int giftCardId, boolean forced) {
		if (giftCardId <= 0) {
			throw new AdempiereException("@FillMandatory@ @ECA14_GiftCard_ID@");
		}
		// TODO: Support with lines
		PO giftCard = RecordUtil.getEntity(
			Env.getCtx(),
			"ECA14_GiftCard",
			giftCardId,
			null
		);
		if (giftCard == null || giftCard.get_ID() <= 0) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @NotFound@");
		}
		if (giftCard.get_ValueAsBoolean("Processed")) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processed@");
		}
		if (giftCard.get_ValueAsBoolean("Processing") && !forced) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processing@");
		}
		giftCard.set_ValueOfColumn("Processing", false);
		giftCard.saveEx();
	}

	/**
	 * Create Gift Card from payment
	 * @param salesOrder
	 * @param payment
	 * @param transactionName
	 * @return void
	 */
	public static void createGiftCard(MOrder salesOrder, MPayment payment, String transactionName) {
		MTable giftCardTable = MTable.get(payment.getCtx(), "ECA14_GiftCard");
		if (giftCardTable == null || giftCardTable.get_ID() <= 0) {
			return;
		}
		PO giftCard = giftCardTable.getPO(0, payment.get_TrxName());
		giftCard.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_DateDoc, payment.getDateTrx());
		giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_C_BPartner_ID, payment.getC_BPartner_ID());
		giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_C_ConversionType_ID, payment.getC_ConversionType_ID());
		giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_C_Currency_ID, payment.getC_Currency_ID());
		giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_C_Order_ID, salesOrder.getC_Order_ID());

		// TODO: Add `C_Payment_ID` as source refund
		if (giftCardTable.get_ColumnIndex(I_C_Payment.COLUMNNAME_C_Payment_ID) >= 0) {
			giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_C_Payment_ID, payment.getC_Payment_ID());
		}

		String description = Msg.parseTranslation(
			payment.getCtx(),
			"@C_Order_ID@: " + salesOrder.getDisplayValue() + "\n" +
			"@C_Payment_ID@: " + payment.getDisplayValue() + "\n"
		);
		giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_Description, description);
		// set total amount on header
		giftCard.set_ValueOfColumn(I_C_Payment.COLUMNNAME_IsPrepayment, true);
		giftCard.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_Amount, payment.getPayAmt());
		giftCard.saveEx(transactionName);
		//	Set reference to Payment
		payment.setC_Invoice_ID(-1);
		payment.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ECA14_Reference_Amount, payment.getPayAmt());
		payment.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID, giftCard.get_ID());
		payment.setPayAmt(Env.ZERO);
		CashUtil.setCurrentDate(payment, true);
		payment.saveEx(transactionName);
	}

	public static void createGiftCardReference(MOrder salesOrder, MPayment payment, String transactionName) {
		final int giftCardId = payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID);
		processGiftCard(
			giftCardId,
			payment,
			null,
			transactionName
		);

		//	Set reference to Payment
		payment.setC_Invoice_ID(-1);
		payment.set_ValueOfColumn(
			ColumnsAdded.COLUMNNAME_ECA14_Reference_Amount,
			payment.getPayAmt()
		);
		payment.setPayAmt(Env.ZERO);
		CashUtil.setCurrentDate(payment, true);
		payment.saveEx(transactionName);
	}


	/**
	 * Create Gift Card from payment
	 * @param salesOrder
	 * @param payment
	 * @param transactionName
	 * @return void
	 */
	public static PO createGiftCardFromPaymentReference(MOrder salesOrder, PO paymentReference, String transactionName) {
		MTable giftCardTable = MTable.get(paymentReference.getCtx(), "ECA14_GiftCard");
		if (giftCardTable == null || giftCardTable.get_ID() <= 0) {
			return null;
		}
		PO giftCard = giftCardTable.getPO(0, paymentReference.get_TrxName());
		giftCard.set_ValueOfColumn(
			ColumnsAdded.COLUMNNAME_DateDoc,
			TimeManager.getTimestampFromString(
				paymentReference.get_ValueAsString("PayDate")
			)
		);
		giftCard.set_ValueOfColumn(
			I_C_Order.COLUMNNAME_C_BPartner_ID,
			paymentReference.get_ValueAsInt(
				I_C_Order.COLUMNNAME_C_BPartner_ID
			)
		);
		giftCard.set_ValueOfColumn(
			I_C_Order.COLUMNNAME_C_ConversionType_ID,
			paymentReference.get_ValueAsInt(
				I_C_Order.COLUMNNAME_C_ConversionType_ID
			)
		);
		giftCard.set_ValueOfColumn(
			I_C_Order.COLUMNNAME_C_Currency_ID,
			paymentReference.get_ValueAsInt(
				I_C_Order.COLUMNNAME_C_Currency_ID
			)
		);
		giftCard.set_ValueOfColumn(
			I_C_Order.COLUMNNAME_C_Order_ID,
			salesOrder.getC_Order_ID()
		);

		// TODO: Add `C_Payment_ID` as source refund
		// if (giftCardTable.get_ColumnIndex(I_C_Payment.COLUMNNAME_C_Payment_ID) >= 0) {
		// 	giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_C_Payment_ID, paymentReference.getC_Payment_ID());
		// }

		String description = Msg.parseTranslation(
			paymentReference.getCtx(),
			"@C_Order_ID@: " + salesOrder.getDisplayValue() + "\n" +
			"@C_POSPaymentReference_ID@: " + paymentReference.getDisplayValue() + "\n"
		);
		giftCard.set_ValueOfColumn(I_C_Order.COLUMNNAME_Description, description);
		// set total amount on header
		giftCard.set_ValueOfColumn(I_C_Payment.COLUMNNAME_IsPrepayment, true);
		giftCard.set_ValueOfColumn(
			ColumnsAdded.COLUMNNAME_Amount,
			NumberManager.getBigDecimalFromString(
				paymentReference.get_ValueAsString(
					ColumnsAdded.COLUMNNAME_Amount
				)
			)
		);
		giftCard.saveEx(transactionName);
		return giftCard;
	}


	public static PO processGiftCard(int giftCardId, MPayment payment, PO paymentReference, String transactionName) {
		MTable giftCardTable = MTable.get(Env.getCtx(), "ECA14_GiftCard");
		if (giftCardTable == null || giftCardTable.get_ID() <= 0) {
			return null;
		}
		if (giftCardId <= 0) {
			throw new AdempiereException("@FillMandatory@ @ECA14_GiftCard_ID@");
		}
		PO giftCard = giftCardTable.getPO(
			giftCardId,
			transactionName
		);
		if (giftCard == null || giftCard.get_ID() <= 0) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @NotFound@");
		}
		if (giftCard.get_ValueAsBoolean("Processed")) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processed@");
		}
		int referenceGiftCardId = -1;
		if (payment != null) {
			referenceGiftCardId = payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID);
		} else if (paymentReference != null) {
			referenceGiftCardId = paymentReference.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID);
		}
		if (giftCard.get_ValueAsBoolean("Processing") && giftCard.get_ID() != referenceGiftCardId) {
			throw new AdempiereException("@ECA14_GiftCard_ID@ @Processing@");
		}
		giftCard.set_ValueOfColumn("Processing", false);
		giftCard.set_ValueOfColumn("Processed", true);
		giftCard.saveEx(transactionName);
		return giftCard;
	}


	public static void createReturnSalesOrder(MOrder salesOrder, List<Integer> paymentsIds, String transactionName) {
		MTable giftCardTable = MTable.get(salesOrder.getCtx(), "ECA14_GiftCard");
		MTable giftCardLineTable = MTable.get(salesOrder.getCtx(), "ECA14_GiftCardLine");

		List<PO> giftCardPayments = paymentsIds.stream()
			.map(paymentId -> {
				MPayment payment = new MPayment(Env.getCtx(), paymentId, transactionName);
				return payment;
			})
			.filter(payment -> {
				if (payment.isReceipt()) {
					boolean isGiftCard = payment.getTenderType().equals("G");
					return isGiftCard;
				}
				return false;
			})
			.map(payment -> {
				final int giftCardId = payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID);
				PO giftCard = giftCardTable.getPO(
					giftCardId,
					transactionName
				);
				return giftCard;
			})
			.filter(giftCard -> {
				return !giftCard.get_ValueAsBoolean(I_C_Payment.COLUMNNAME_IsPrepayment);
			})
			// .toList()
			.collect(Collectors.toList())
		;

		if (giftCardPayments == null || giftCardPayments.isEmpty()) {
			return;
		}

		// Order ID, Order Lines ID
		Map<Integer, List<Integer>> ordersToReverse = new HashMap<Integer, List<Integer>>();
		// Order Line ID, Gift Card Quantity
		Map<Integer, BigDecimal> orderLinesToReverse = new HashMap<Integer, BigDecimal>();
		giftCardPayments.forEach(giftCard -> {
			final int giftCardId = giftCard.get_ID();
			List<PO> giftCardLines = new Query(
				Env.getCtx(),
				giftCardLineTable,
				ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID + " = ?",
				null
			)
				.setParameters(giftCardId)
				.list()
			;
			giftCardLines.forEach(giftCardLine -> {
				final int orderLineId = giftCardLine.get_ValueAsInt(
					I_C_OrderLine.COLUMNNAME_C_OrderLine_ID
				);
				BigDecimal reverseQuantity = Env.ZERO;
				if (orderLinesToReverse.containsKey(orderLineId)) {
					reverseQuantity = reverseQuantity.add(
						orderLinesToReverse.get(orderLineId)
					);
				}

				BigDecimal giftCardQuantity = Optional.ofNullable(
					NumberManager.getBigDecimalFromObject(
						giftCardLine.get_Value(
							I_C_OrderLine.COLUMNNAME_QtyEntered
						)
					)
				).orElse(Env.ZERO);
				reverseQuantity = reverseQuantity.add(giftCardQuantity);
				orderLinesToReverse.put(orderLineId, reverseQuantity);

				MOrderLine orderLine = new MOrderLine(Env.getCtx(), orderLineId, transactionName);
				List<Integer> orderLinesIds = new ArrayList<Integer>();
				if (ordersToReverse.containsKey(orderLine.getC_Order_ID())) {
					orderLinesIds = ordersToReverse.get(orderLine.getC_Order_ID());
				}
				orderLinesIds.add(orderLineId);
				ordersToReverse.put(
					orderLine.getC_Order_ID(),
					orderLinesIds
				);
			});
		});

		MPOS pos = POS.validateAndGetPOS(salesOrder.getC_POS_ID(), false);
		ordersToReverse.entrySet().forEach(entry -> {
			final int sourceOrderId = entry.getKey();
			MOrder returnOrder = ReturnSalesOrder.createRMAFromOrder(
				salesOrder.getC_POS_ID(),
				sourceOrderId,
				0,
				false,
				transactionName
			);

			List<Integer> linesIds = entry.getValue();
			for (Integer sourceLineId : linesIds) {
				BigDecimal giftCardQuantity = orderLinesToReverse.get(sourceLineId);
				ReturnSalesOrder.createRMALineFromOrder(
					returnOrder.getC_Order_ID(),
					sourceLineId,
					giftCardQuantity,
					transactionName
				);
			}

			processReverseSalesOrder(
				pos,
				returnOrder,
				transactionName
			);
		});
	}


	/**
	 * Create return order
	 * @param pos
	 * @param sourceOrder
	 * @param transactionName
	 * @return
	 */
	public static MOrder processReverseSalesOrder(MPOS pos, MOrder returnOrder, String transactionName) {
		//	Close all
		if(!returnOrder.processIt(MOrder.DOCACTION_Close)) {
			throw new AdempiereException("@ProcessFailed@ :" + returnOrder.getProcessMsg());
		}
		returnOrder.saveEx();

		//	Generate Return
		RMAUtil.generateReturnFromRMA(returnOrder, transactionName);

		//	Generate Credit Memo
		MInvoice creditMemo = RMAUtil.generateCreditMemoFromRMA(returnOrder, transactionName);

		createPaymentAllocation(
			pos,
			returnOrder,
			creditMemo,
			transactionName
		);

		return returnOrder;
	}


	public static MAllocationHdr createPaymentAllocation(MPOS pos, MOrder returnOrder, MInvoice creditMemo, String transactionName) {
		String description = Msg.parseTranslation(Env.getCtx(), "@C_POS_ID@: " + pos.getName() + " - " + returnOrder.getDocumentNo());

		MAllocationHdr paymentAllocation = new MAllocationHdr(
			Env.getCtx(),
			true,
			TimeManager.getDate(),
			returnOrder.getC_Currency_ID(),
			description,
			transactionName
		);
		paymentAllocation.setAD_Org_ID(returnOrder.getAD_Org_ID());
		//	Set Description
		paymentAllocation.saveEx();

		// Credit Memo line
		MAllocationLine paymentAllocationLineMemo = new MAllocationLine(
			paymentAllocation,
			returnOrder.getGrandTotal(),
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO
		);
		paymentAllocationLineMemo.setDocInfo(
			returnOrder.getC_BPartner_ID(),
			returnOrder.getC_Order_ID(),
			creditMemo.getC_Invoice_ID()
		);
		paymentAllocationLineMemo.saveEx();

		// Gift Card Charge line
		final int defaultGiftCardChargeId = pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_DefaultGiftCardCharge_ID);
		MAllocationLine paymentAllocationLineCharge = new MAllocationLine(
			paymentAllocation,
			returnOrder.getGrandTotal().negate(),
			Env.ZERO,
			Env.ZERO,
			Env.ZERO
		);
		paymentAllocationLineCharge.setDocInfo(
			returnOrder.getC_BPartner_ID(),
			returnOrder.getC_Order_ID(),
			0
		);
		paymentAllocationLineCharge.setC_Charge_ID(defaultGiftCardChargeId);
		paymentAllocationLineCharge.saveEx();

		//	Complete
		if (!paymentAllocation.processIt(MAllocationHdr.DOCACTION_Complete)) {
			throw new AdempiereException(paymentAllocation.getProcessMsg());
		}

		return paymentAllocation;
	}

}
