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
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.X_C_Payment;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.core_functionality.Currency;
import org.spin.backend.grpc.pos.Card;
import org.spin.backend.grpc.pos.CardProvider;
import org.spin.backend.grpc.pos.CreditCardType;
import org.spin.backend.grpc.pos.Payment;
import org.spin.backend.grpc.pos.PaymentMethod;
import org.spin.backend.grpc.pos.PaymentReference;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.util.ColumnsAdded;
import org.spin.pos.util.POSConvertUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.store.model.MCPaymentMethod;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Payment Convert Util from Point Of Sales
 */
public class PaymentConvertUtil {

	public static CreditCardType.Builder convertCreditCardType(String creditCardTypeValue) {
		CreditCardType.Builder builder = CreditCardType.newBuilder();
		if (Util.isEmpty(creditCardTypeValue, true)) {
			return builder;
		}
		MRefList creditCardTypeReference = MRefList.get(
			Env.getCtx(),
			X_C_Payment.CREDITCARDTYPE_AD_Reference_ID,
			creditCardTypeValue,
			null
		);
		if (creditCardTypeReference == null) {
			return builder;
		}
		builder = convertCreditCardType(creditCardTypeReference);
		return builder;
	}
	public static CreditCardType.Builder convertCreditCardType(MRefList creditCardTypeReference) {
		CreditCardType.Builder builder = CreditCardType.newBuilder();
		if (creditCardTypeReference == null) {
			return builder;
		}
		builder.setId(
				creditCardTypeReference.getAD_Ref_List_ID()
			)
			.setUuid(
				StringManager.getValidString(
					creditCardTypeReference.getUUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					creditCardTypeReference.getValue()
				)
			)
			.setName(
				StringManager.getValidString(
					creditCardTypeReference.get_Translation(I_AD_Ref_List.COLUMNNAME_Name)
				)
			)
			.setDescription(
				StringManager.getValidString(
					creditCardTypeReference.get_Translation(I_AD_Ref_List.COLUMNNAME_Description)
				)
			)
			.setIsActive(
				creditCardTypeReference.isActive()
			)
		;
		return builder;
	}


	public static CardProvider.Builder convertCardProvider(int cardProviderId) {
		CardProvider.Builder builder = CardProvider.newBuilder();
		if (cardProviderId <= 0) {
			return builder;
		}
		PO cardProvider = RecordUtil.getEntity(
			Env.getCtx(),
			"C_CardProvider",
			cardProviderId,
			null
		);
		return convertCardProvider(cardProvider);
	}
	public static CardProvider.Builder convertCardProvider(PO cardProvider) {
		CardProvider.Builder builder = CardProvider.newBuilder();
		if (cardProvider == null || cardProvider.get_ID() <= 0) {
			return builder;
		}
		builder.setId(
				cardProvider.get_ID()
			)
			.setUuid(
				StringManager.getValidString(
					cardProvider.get_UUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					cardProvider.get_ValueAsString("Value")
				)
			)
			.setName(
				StringManager.getValidString(
					cardProvider.get_ValueAsString("Name")
				)
			)
			.setDescription(
				StringManager.getValidString(
					cardProvider.get_ValueAsString("Description")
				)
			)
			.setIsActive(
				cardProvider.isActive()
			)
		;
		return builder;
	}


	public static Card.Builder convertCard(int cardId) {
		Card.Builder builder = Card.newBuilder();
		if (cardId <= 0) {
			return builder;
		}
		PO card = RecordUtil.getEntity(
			Env.getCtx(),
			"C_Card",
			cardId,
			null
		);
		return convertCard(card);
	}
	public static Card.Builder convertCard(PO card) {
		Card.Builder builder = Card.newBuilder();
		if (card == null || card.get_ID() <= 0) {
			return builder;
		}
		builder.setId(
				card.get_ID()
			)
			.setUuid(
				StringManager.getValidString(
					card.get_UUID()
				)
			)
			.setValue(
				StringManager.getValidString(
					card.get_ValueAsString("Value")
				)
			)
			.setName(
				StringManager.getValidString(
					card.get_ValueAsString("Name")
				)
			)
			.setDescription(
				StringManager.getValidString(
					card.get_ValueAsString("Description")
				)
			)
			.setIsActive(
				card.isActive()
			)
		;
		return builder;
	}


	public static PaymentMethod.Builder convertPaymentMethod(MCPaymentMethod paymentMethod) {
		PaymentMethod.Builder paymentMethodBuilder = PaymentMethod.newBuilder();
		if(paymentMethod == null) {
			return paymentMethodBuilder;
		}
		paymentMethodBuilder
			.setId(
				paymentMethod.getC_PaymentMethod_ID()
			)
			.setName(
				StringManager.getValidString(
					paymentMethod.getName()
				)
			)
			.setValue(
				StringManager.getValidString(
					paymentMethod.getValue()
				)
			)
			.setDescription(
				StringManager.getValidString(
					paymentMethod.getDescription()
				)
			)
			.setTenderType(
				StringManager.getValidString(
					paymentMethod.getTenderType()
				)
			)
			.setIsActive(
				paymentMethod.isActive()
			)
		;

		return paymentMethodBuilder;
	}


	/**
	 * Conver Refund Reference to gRPC stub object
	 * @param paymentReference
	 * @return
	 * @return RefundReference.Builder
	 */
	public static PaymentReference.Builder convertPaymentReference(PO paymentReference) {
		PaymentReference.Builder builder = PaymentReference.newBuilder();
		if(paymentReference == null || paymentReference.get_ID() <= 0) {
			return builder;
		}
		MCPaymentMethod paymentMethod = MCPaymentMethod.getById(Env.getCtx(), paymentReference.get_ValueAsInt("C_PaymentMethod_ID"), null);
		PaymentMethod.Builder paymentMethodBuilder = convertPaymentMethod(paymentMethod);

		int presicion = MCurrency.getStdPrecision(paymentReference.getCtx(), paymentReference.get_ValueAsInt("C_Currency_ID"));

		BigDecimal amount = (BigDecimal) paymentReference.get_Value("Amount");
		amount.setScale(presicion, RoundingMode.HALF_UP);

		MOrder order = new MOrder(Env.getCtx(), paymentReference.get_ValueAsInt("C_Order_ID"), null);
		BigDecimal convertedAmount = ConvertUtil.getConvetedAmount(order, paymentReference, amount)
			.setScale(presicion, RoundingMode.HALF_UP)
		;

		builder.setAmount(
				NumberManager.getBigDecimalToString(
					amount
				)
			)
			.setDescription(
				StringManager.getValidString(
					paymentReference.get_ValueAsString("Description")
				)
			)
			.setIsPaid(
				!paymentReference.get_ValueAsBoolean("IsReceipt")
			)
			.setTenderTypeCode(
				StringManager.getValidString(
					paymentReference.get_ValueAsString("TenderType")
				)
			)
			.setCurrency(
				CoreFunctionalityConvert.convertCurrency(
					paymentReference.get_ValueAsInt("C_Currency_ID")
				)
			)
			.setCustomerBankAccountId(
				paymentReference.get_ValueAsInt("C_BP_BankAccount_ID")
			)
			.setOrderId(
				paymentReference.get_ValueAsInt("C_Order_ID")
			)
			.setPosId(
				paymentReference.get_ValueAsInt("C_POS_ID")
			)
			.setSalesRepresentative(
				CoreFunctionalityConvert.convertSalesRepresentative(
					MUser.get(Env.getCtx(), paymentReference.get_ValueAsInt("SalesRep_ID"))
				)
			)
			.setId(
				paymentReference.get_ID()
			)
			.setPaymentMethod(paymentMethodBuilder)
			.setPaymentDate(
				ValueManager.getTimestampFromDate(
					(Timestamp) paymentReference.get_Value("PayDate")
				)
			)
			.setIsAutomatic(
				paymentReference.get_ValueAsBoolean("IsAutoCreatedReference")
			)
			.setIsProcessed(
				paymentReference.get_ValueAsBoolean("Processed")
			)
			.setConvertedAmount(
				NumberManager.getBigDecimalToString(
					convertedAmount
				)
			)
		;

		if (paymentReference.get_ColumnIndex(I_C_Payment.COLUMNNAME_CreditCardType) >= 0) {
			builder.setCreditCardType(
				convertCreditCardType(
					paymentReference.get_ValueAsString(
						I_C_Payment.COLUMNNAME_CreditCardType
					)
				)
			);
		}

		if (paymentReference.get_ColumnIndex(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID) >= 0) {
			MTable giftCardTable = MTable.get(Env.getCtx(), "ECA14_GiftCard");
			if (giftCardTable != null && giftCardTable.get_ID() > 0) {
				PO giftCard = giftCardTable.getPO(
					paymentReference.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID),
					null
				);
				if (giftCard != null && giftCard.get_ID() > 0) {
					builder.setGiftCardId(
							giftCard.get_ID()
						)
						.setGiftCardCode(
							StringManager.getValidString(
								giftCard.get_ValueAsString(
									I_C_Payment.COLUMNNAME_DocumentNo
								)
							)
						)
					;
				}
			}
		}
		//	
		return builder;
	}


	/**
	 * Convert payment
	 * @param payment
	 * @return
	 */
	public static Payment.Builder convertPayment(MPayment payment) {
		Payment.Builder builder = Payment.newBuilder();
		if(payment == null) {
			return builder;
		}
		//	
		MRefList reference = MRefList.get(Env.getCtx(), MPayment.DOCSTATUS_AD_REFERENCE_ID, payment.getDocStatus(), payment.get_TrxName());
		int presicion = MCurrency.getStdPrecision(payment.getCtx(), payment.getC_Currency_ID());
		BigDecimal paymentAmount = payment.getPayAmt(true);
		if(payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)
				&& paymentAmount.compareTo(Env.ZERO) == 0) {
			MInvoice creditMemo = new Query(payment.getCtx(), MInvoice.Table_Name, "C_Payment_ID = ?", payment.get_TrxName()).setParameters(payment.getC_Payment_ID()).first();
			if(creditMemo != null) {
				paymentAmount = creditMemo.getGrandTotal();
			}
		}
		paymentAmount = paymentAmount.setScale(presicion, RoundingMode.HALF_UP);

		MCPaymentMethod paymentMethod = MCPaymentMethod.getById(Env.getCtx(), payment.get_ValueAsInt("C_PaymentMethod_ID"), null);
		PaymentMethod.Builder paymentMethodBuilder = convertPaymentMethod(paymentMethod);
		
		Currency.Builder currencyBuilder = CoreFunctionalityConvert.convertCurrency(
			payment.getC_Currency_ID()
		);
		MOrder order = new MOrder(payment.getCtx(), payment.getC_Order_ID(), null);
		BigDecimal convertedAmount = ConvertUtil.getConvetedAmount(order, payment, paymentAmount)
			.setScale(presicion, RoundingMode.HALF_UP);
		String invoiceNo = null;
		if(payment.getC_Invoice_ID() > 0) {
			invoiceNo = payment.getC_Invoice().getDocumentNo();
		}
		
		//	Convert
		builder
			.setId(
				payment.getC_Payment_ID()
			)
			.setOrderId(
				payment.getC_Order_ID()
			)
			.setPosId(
				payment.getC_POS_ID()
			)
			.setDocumentNo(
				StringManager.getValidString(
					payment.getDocumentNo()
				)
			)
			.setOrderDocumentNo(
				StringManager.getValidString(
					order.getDocumentNo()
				)
			)
			.setInvoiceDocumentNo(
				StringManager.getValidString(invoiceNo)
			)
			.setTenderTypeCode(
				StringManager.getValidString(
					payment.getTenderType()
				)
			)
			.setReferenceNo(
				StringManager.getValidString(
					Optional.ofNullable(
						payment.getCheckNo()
					).orElse(
						payment.getDocumentNo()
					)
				)
			)
			.setDescription(
				StringManager.getValidString(
					payment.getDescription()
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					paymentAmount
				)
			)
			.setConvertedAmount(
				NumberManager.getBigDecimalToString(
					convertedAmount
				)
			)
			.setBankId(
				payment.getC_Bank_ID()
			)
			.setCustomer(
				POSConvertUtil.convertCustomer(
					(MBPartner) payment.getC_BPartner()
				)
			)
			.setCurrency(currencyBuilder)
			.setPaymentDate(
				ValueManager.getTimestampFromDate(
					payment.getDateTrx()
				)
			)
			.setIsRefund(
				!payment.isReceipt()
			)
			.setPaymentAccountDate(
				ValueManager.getTimestampFromDate(
					payment.getDateAcct()
				)
			)
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					StringManager.getValidString(
						payment.getDocStatus()
					),
					StringManager.getValidString(
						ValueManager.getTranslation(
							reference,
							I_AD_Ref_List.COLUMNNAME_Name
						)
					),
					StringManager.getValidString(
						ValueManager.getTranslation(
							reference,
							I_AD_Ref_List.COLUMNNAME_Description
						)
					)
				)
			)
			.setPaymentMethod(paymentMethodBuilder)
			.setCreditCardType(
				convertCreditCardType(
					payment.getCreditCardType()
				)
			)
			.setCharge(
				CoreFunctionalityConvert.convertCharge(
					payment.getC_Charge_ID()
				)
			)
			.setDocumentType(
				CoreFunctionalityConvert.convertDocumentType(
					payment.getC_DocType_ID()
				)
			)
			.setBankAccount(
				CoreFunctionalityConvert.convertBankAccount(
					payment.getC_BankAccount_ID()
				)
			)
			.setReferenceBankAccount(
				CoreFunctionalityConvert.convertBankAccount(
					payment.get_ValueAsInt("POSReferenceBankAccount_ID")
				)
			)
			.setIsProcessed(
				payment.isProcessed()
			)
			.setIsProcessing(
				payment.isProcessing()
			)
			.setIsOnline(
				payment.isOnline()
			)
			.setResponseStatus(
				StringManager.getValidString(
					payment.get_ValueAsString("ResponseStatus")
				)
			)
			.setResponseMessage(
				StringManager.getValidString(
					payment.get_ValueAsString("ResponseMessage")
				)
			)
			.setResponseCode(
				StringManager.getValidString(
					payment.get_ValueAsString("ResponseCode")
				)
			)
			.setNextRequestTime(
				payment.get_ValueAsInt("NextRequestTime")
			)
		;

		if (payment.get_ColumnIndex(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID) >= 0) {
			MTable giftCardTable = MTable.get(Env.getCtx(), "ECA14_GiftCard");
			if (giftCardTable != null && giftCardTable.get_ID() > 0) {
				PO giftCard = giftCardTable.getPO(
					payment.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_GiftCard_ID),
					null
				);
				if (giftCard != null && giftCard.get_ID() > 0) {
					builder.setGiftCardId(
							giftCard.get_ID()
						)
						.setGiftCardCode(
							StringManager.getValidString(
								giftCard.get_ValueAsString(
									I_C_Payment.COLUMNNAME_DocumentNo
								)
							)
						)
					;
				}
			}
		}

		if(payment.getCollectingAgent_ID() > 0) {
			builder.setCollectingAgent(
				CoreFunctionalityConvert.convertSalesRepresentative(
					MUser.get(payment.getCtx(), payment.getCollectingAgent_ID())
				)
			);
		}
		return builder;
	}
}
