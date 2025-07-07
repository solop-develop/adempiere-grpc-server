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

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.spin.base.util.RecordUtil;
import org.spin.pos.service.cash.CashUtil;
import org.spin.pos.util.ColumnsAdded;
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

}
