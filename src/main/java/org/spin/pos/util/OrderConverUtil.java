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

package org.spin.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.compiere.model.MBPartner;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrderTax;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.model.MStorage;
import org.compiere.model.MTax;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.backend.grpc.pos.Order;
import org.spin.backend.grpc.pos.OrderLine;
import org.spin.base.util.ConvertUtil;
import org.spin.grpc.service.TimeControl;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.service.order.OrderUtil;
import org.spin.pos.service.payment.PaymentManagement;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Sales Order Convert Util for backend of Point Of Sales form
 */
public class OrderConverUtil {

	/**
	 * Convert Order from entity
	 * @param orderId
	 * @return
	 */
	public static Order.Builder convertOder(int orderId) {
		Order.Builder builder = Order.newBuilder();
		if(orderId <= 0) {
			return builder;
		}
		MOrder order = new MOrder(Env.getCtx(), orderId, null);
		return OrderConverUtil.convertOrder(
			order
		);
	}
	/**
	 * Convert Order from entity
	 * @param order
	 * @return
	 */
	public static Order.Builder convertOrder(MOrder order) {
		Order.Builder builder = Order.newBuilder();
		if(order == null) {
			return builder;
		}
		MPOS pos = new MPOS(Env.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		final int defaultDiscountChargeId = pos.get_ValueAsInt("DefaultDiscountCharge_ID");
		// final int defaultGiftCardChargeId = pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_DefaultGiftCardCharge_ID);

		BigDecimal totalTaxAmount = Arrays.stream(order.getTaxes(true))
			// .filter(tax -> tax.getTaxAmt().signum() != 0)
			.map(MOrderTax::getTaxAmt)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
		;

		MRefList reference = MRefList.get(Env.getCtx(), MOrder.DOCSTATUS_AD_REFERENCE_ID, order.getDocStatus(), null);
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		List<MOrderLine> orderLines = Arrays.asList(order.getLines(true, null));
		BigDecimal totalLines = orderLines.stream()
			.filter(orderLine -> {
				int chargeOrdeLineId = orderLine.getC_Charge_ID();
				// if (chargeOrdeLineId <= 0) {
				// 	return true;
				// }
				if (defaultDiscountChargeId <= 0) {
					return true;
				}
				return chargeOrdeLineId != defaultDiscountChargeId; // && chargeOrdeLineId != defaultGiftCardChargeId;
			})
			.map(orderLine -> {
				return Optional.ofNullable(orderLine.getLineNetAmt()).orElse(Env.ZERO);
			})
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.subtract(totalTaxAmount)
		;

		BigDecimal discountAmount = BigDecimal.ZERO;
		if (defaultDiscountChargeId > 0) {
			discountAmount = orderLines.stream()
				.filter(orderLine -> {
					int chargeOrdeLineId = orderLine.getC_Charge_ID();
					if (chargeOrdeLineId <= 0) {
						return false;
					}
					return chargeOrdeLineId == defaultDiscountChargeId;
				})
				.map(orderLine -> {
					return Optional.ofNullable(orderLine.getLineNetAmt()).orElse(Env.ZERO);
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add)
			;
		}

		BigDecimal lineDiscountAmount = orderLines.stream()
			.filter(orderLine -> {
				int chargeOrdeLineId = orderLine.getC_Charge_ID();
				// if (chargeOrdeLineId <= 0) {
				// 	return true;
				// }
				if (defaultDiscountChargeId <= 0) {
					return true;
				}
				return chargeOrdeLineId != defaultDiscountChargeId; // && chargeOrdeLineId != defaultGiftCardChargeId;
			})
			.map(orderLine -> {
				BigDecimal priceActualAmount = Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO);
				BigDecimal priceListAmount = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO);
				BigDecimal discountLine = priceListAmount.subtract(priceActualAmount)
					.multiply(
						Optional.ofNullable(
							orderLine.getQtyOrdered()
						)
							.orElse(Env.ZERO)
					)
				;
				return discountLine;
			})
			.reduce(BigDecimal.ZERO, BigDecimal::add)
		;
		//	
		BigDecimal totalDiscountAmount = discountAmount.add(lineDiscountAmount);

		//	
		Optional<BigDecimal> paidAmount = MPayment.getOfOrder(order)
			.stream()
			.map(payment -> {
				BigDecimal paymentAmount = payment.getPayAmt();
				if(paymentAmount.compareTo(Env.ZERO) == 0 && payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)) {
					MInvoice creditMemo = new Query(
						payment.getCtx(),
						MInvoice.Table_Name,
						"C_Payment_ID = ?",
						payment.get_TrxName()
					)
						.setParameters(payment.getC_Payment_ID())
						.first()
					;
					if(creditMemo != null) {
						paymentAmount = creditMemo.getGrandTotal();
					}
				}
				if(!payment.isReceipt()) {
					paymentAmount = payment.getPayAmt().negate();
				}
				return ConvertUtil.getConvertedAmount(order, payment, paymentAmount);
			})
			.collect(Collectors.reducing(BigDecimal::add))
		;

		BigDecimal grandTotal = order.getGrandTotal();
		BigDecimal grandTotalConverted = OrderUtil.getConvertedAmountTo(
			order,
			pos.get_ValueAsInt(
				ColumnsAdded.COLUMNNAME_DisplayCurrency_ID
			),
			grandTotal
		);

		BigDecimal paymentAmount = Env.ZERO;
		if(paidAmount.isPresent()) {
			paymentAmount = paidAmount.get();
		}

		BigDecimal creditAmt = OrderUtil.getCreditAmount(order);
		BigDecimal chargeAmt = OrderUtil.getChargeAmount(order);
		BigDecimal totalPaymentAmount = OrderUtil.getTotalPaymentAmount(order);
		final boolean isReturnOrder = order.isReturnOrder();
		if (isReturnOrder) {
			totalPaymentAmount = totalPaymentAmount.negate();
		}

		BigDecimal openAmount = (grandTotal.subtract(totalPaymentAmount).compareTo(Env.ZERO) < 0 ? Env.ZERO : grandTotal.subtract(totalPaymentAmount));
		BigDecimal refundAmount = (grandTotal.subtract(totalPaymentAmount).compareTo(Env.ZERO) > 0 ? Env.ZERO : grandTotal.subtract(totalPaymentAmount).negate());
		BigDecimal displayCurrencyRate = ConvertUtil.getDisplayConversionRateFromOrder(order);

		if (order.getC_Invoice_ID() > 0) {
			MInvoice invoice = new MInvoice(order.getCtx(), order.getC_Invoice_ID(), order.get_TrxName());
			builder.setInvoiceNo(
				StringManager.getValidString(
					invoice.getDocumentNo()
				)
			);
		}

		// Exists Online Payment Approved
		boolean isOnlinePaymentApproved = PaymentManagement.isOrderWithOnlinePaymentApproved(
			order.getC_Order_ID()
		);

		//	Convert
		return builder
			.setId(
				order.getC_Order_ID()
			)
			.setUuid(
				StringManager.getValidString(
					order.getUUID()
				)
			)
			.setDocumentType(
				CoreFunctionalityConvert.convertDocumentType(
					order.getC_DocTypeTarget_ID()
				)
			)
			.setDocumentNo(
				StringManager.getValidString(
					order.getDocumentNo()
				)
			)
			.setSalesRepresentative(
				CoreFunctionalityConvert.convertSalesRepresentative(
					MUser.get(Env.getCtx(), order.getSalesRep_ID())
				)
			)
			.setDescription(
				StringManager.getValidString(
					order.getDescription()
				)
			)
			.setOrderReference(
				StringManager.getValidString(
					order.getPOReference()
				)
			)
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					StringManager.getValidString(
						order.getDocStatus()
					),
					StringManager.getValidString(
						org.spin.service.grpc.util.base.RecordUtil.getTranslation(
							reference,
							I_AD_Ref_List.COLUMNNAME_Name
						)
					),
					StringManager.getValidString(
						org.spin.service.grpc.util.base.RecordUtil.getTranslation(
							reference,
							I_AD_Ref_List.COLUMNNAME_Description
						)
					)
				)
			)
			.setPriceList(
				CoreFunctionalityConvert.convertPriceList(
					MPriceList.get(
						Env.getCtx(),
						order.getM_PriceList_ID(),
						order.get_TrxName()
					)
				)
			)
			.setWarehouse(
				CoreFunctionalityConvert.convertWarehouse(
					order.getM_Warehouse_ID()
				)
			)
			.setIsDelivered(
				order.isDelivered()
			)
			.setDiscountAmount(
				NumberManager.getBigDecimalToString(
					Optional.ofNullable(totalDiscountAmount).orElse(Env.ZERO)
						.setScale(
							priceList.getStandardPrecision(),
							RoundingMode.HALF_UP
						)
				)
			)
			.setTaxAmount(
				NumberManager.getBigDecimalToString(
					totalTaxAmount
				)
			)
			.setTotalLines(
				NumberManager.getBigDecimalToString(
					totalLines.add(totalDiscountAmount).setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setGrandTotal(
				NumberManager.getBigDecimalToString(
					grandTotal.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setGrandTotalConverted(
				NumberManager.getBigDecimalToString(
					grandTotalConverted.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setDisplayCurrencyRate(
				NumberManager.getBigDecimalToString(
					displayCurrencyRate.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setPaymentAmount(
				NumberManager.getBigDecimalToString(
					paymentAmount.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setOpenAmount(
				NumberManager.getBigDecimalToString(
					openAmount.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setRefundAmount(
				NumberManager.getBigDecimalToString(
					refundAmount.setScale(
						priceList.getStandardPrecision(),
						RoundingMode.HALF_UP
					)
				)
			)
			.setDateOrdered(
				ValueManager.getProtoTimestampFromTimestamp(
					order.getDateOrdered()
				)
			)
			.setCustomer(
				POSConvertUtil.convertCustomer(
					(MBPartner) order.getC_BPartner()
				)
			)
			.setCampaign(
				POSConvertUtil.convertCampaign(
					order.getC_Campaign_ID()
				)
			)
			.setChargeAmount(
				NumberManager.getBigDecimalToString(chargeAmt)
			)
			.setCreditAmount(
				NumberManager.getBigDecimalToString(creditAmt)
			)
			.setIsOrder(
				!isReturnOrder
			)
			.setSourceRmaId(
				order.get_ValueAsInt(
					ColumnsAdded.COLUMNNAME_ECA14_Source_RMA_ID
				)
			)
			.setIsRma(
				isReturnOrder
			)
			.setSourceOrderId(
				order.get_ValueAsInt(
					ColumnsAdded.COLUMNNAME_ECA14_Source_Order_ID
				)
			)
			.setIsBindingOffer(
				OrderUtil.isBindingOffer(order)
			)
			.setIsProcessed(
				order.isProcessed()
			)
			.setIsProcessing(
				order.isProcessing()
			)
			.setIsOnlinePaymentApproved(isOnlinePaymentApproved)
			.setIsManualDocument(
				order.get_ValueAsBoolean(
					ColumnsAdded.COLUMNNAME_IsManualDocument
				)
			)
			.setManualInvociceDocumentNo(
				StringManager.getValidString(
					order.get_ValueAsString(
						"ManualInvoiceDocumentNo"
					)
				)
			)
			.setManualShipmentDocumentNo(
				StringManager.getValidString(
					order.get_ValueAsString(
						"ManualShipmentDocumentNo"
					)
				)
			)
		;
	}



	/**
	 * Convert order line to stub
	 * @param orderLine
	 * @return
	 */
	public static OrderLine.Builder convertOrderLine(MOrderLine orderLine) {
		OrderLine.Builder builder = OrderLine.newBuilder();
		if(orderLine == null) {
			return builder;
		}
		MTax tax = MTax.get(Env.getCtx(), orderLine.getC_Tax_ID());
		MOrder order = orderLine.getParent();
		MPOS pos = new MPOS(Env.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		BigDecimal quantityEntered = orderLine.getQtyEntered();
		BigDecimal quantityOrdered = orderLine.getQtyOrdered();
		//	Units
		BigDecimal priceListAmount = orderLine.getPriceList();
		BigDecimal priceBaseAmount = orderLine.getPriceActual();
		BigDecimal priceAmount = orderLine.getPriceEntered();
		//	Discount
		BigDecimal discountRate = orderLine.getDiscount();
		BigDecimal discountAmount = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO).subtract(Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO));
		//	Taxes
		BigDecimal priceTaxAmount = tax.calculateTax(priceAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal priceBaseTaxAmount = tax.calculateTax(priceBaseAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal priceListTaxAmount = tax.calculateTax(priceListAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		//	Prices with tax
		BigDecimal priceListWithTaxAmount = priceListAmount.add(priceListTaxAmount);
		BigDecimal priceBaseWithTaxAmount = priceBaseAmount.add(priceBaseTaxAmount);
		BigDecimal priceWithTaxAmount = priceAmount.add(priceTaxAmount);
		//	Totals
		BigDecimal totalDiscountAmount = discountAmount.multiply(quantityOrdered);
		BigDecimal totalAmount = orderLine.getLineNetAmt();
		BigDecimal totalAmountConverted = OrderUtil.getConvertedAmountTo(
			order,
			pos.get_ValueAsInt(
				ColumnsAdded.COLUMNNAME_DisplayCurrency_ID
			),
			totalAmount
		);
		BigDecimal totalBaseAmount = totalAmount.subtract(totalDiscountAmount);
		BigDecimal totalTaxAmount = tax.calculateTax(totalAmount, priceList.isTaxIncluded(), priceList.getStandardPrecision());
		BigDecimal totalBaseAmountWithTax = totalBaseAmount.add(totalTaxAmount);
		BigDecimal totalAmountWithTax = totalAmount.add(totalTaxAmount);
		BigDecimal totalAmountWithTaxConverted = OrderUtil.getConvertedAmountTo(
			order,
			pos.get_ValueAsInt(
				ColumnsAdded.COLUMNNAME_DisplayCurrency_ID
			),
			totalAmountWithTax
		);

		MUOMConversion uom = null;
		MUOMConversion productUom = null;
		if (orderLine.getM_Product_ID() > 0) {
			MProduct product = MProduct.get(Env.getCtx(), orderLine.getM_Product_ID());
			List<MUOMConversion> productsConversion = Arrays.asList(MUOMConversion.getProductConversions(Env.getCtx(), product.getM_Product_ID()));
			Optional<MUOMConversion> maybeUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == orderLine.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeUom.isPresent()) {
				uom = maybeUom.get();
			}

			Optional<MUOMConversion> maybeProductUom = productsConversion.parallelStream()
				.filter(productConversion -> {
					return productConversion.getC_UOM_To_ID() == product.getC_UOM_ID();
				})
				.findFirst()
			;
			if (maybeProductUom.isPresent()) {
				productUom = maybeProductUom.get();
			}
		} else {
			uom = new MUOMConversion(Env.getCtx(), 0, null);
			uom.setC_UOM_ID(orderLine.getC_UOM_ID());
			uom.setC_UOM_To_ID(orderLine.getC_UOM_ID());
			uom.setMultiplyRate(Env.ONE);
			uom.setDivideRate(Env.ONE);
			productUom = uom;
		}

		int standardPrecision = priceList.getStandardPrecision();
		BigDecimal availableQuantity = MStorage.getQtyAvailable(orderLine.getM_Warehouse_ID(), 0, orderLine.getM_Product_ID(), orderLine.getM_AttributeSetInstance_ID(), null);
		//	Convert
		return builder.setId(
				orderLine.getC_OrderLine_ID()
			)
			.setUuid(
				StringManager.getValidString(
					orderLine.getUUID()
				)
			)
			.setOrderId(
				orderLine.getC_Order_ID()
			)
			.setLine(
				orderLine.getLine()
			)
			.setDescription(
				StringManager.getValidString(
					orderLine.getDescription()
				)
			)
			.setLineDescription(
				StringManager.getValidString(
					orderLine.getName()
				)
			)
			.setProduct(
				CoreFunctionalityConvert.convertProduct(
					orderLine.getM_Product_ID()
				)
			)
			.setCharge(
				CoreFunctionalityConvert.convertCharge(
					orderLine.getC_Charge_ID()
				)
			)
			.setWarehouse(
				CoreFunctionalityConvert.convertWarehouse(
					orderLine.getM_Warehouse_ID()
				)
			)
			.setQuantity(
				NumberManager.getBigDecimalToString(
					quantityEntered.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				))
			.setQuantityOrdered(
				NumberManager.getBigDecimalToString(
					quantityOrdered.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setAvailableQuantity(
				NumberManager.getBigDecimalToString(
					availableQuantity.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			//	Prices
			.setPriceList(
				NumberManager.getBigDecimalToString(
					priceListAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setPrice(
				NumberManager.getBigDecimalToString(
					priceAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setPriceBase(
				NumberManager.getBigDecimalToString(
					priceBaseAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			//	Taxes
			.setPriceListWithTax(
				NumberManager.getBigDecimalToString(
					priceListWithTaxAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setPriceBaseWithTax(
				NumberManager.getBigDecimalToString(
					priceBaseWithTaxAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setPriceWithTax(
				NumberManager.getBigDecimalToString(
					priceWithTaxAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			//	Prices with taxes
			.setListTaxAmount(
				NumberManager.getBigDecimalToString(
					priceListTaxAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTaxAmount(
				NumberManager.getBigDecimalToString(
					priceTaxAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setBaseTaxAmount(
				NumberManager.getBigDecimalToString(
					priceBaseTaxAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			//	Discounts
			.setDiscountAmount(
				NumberManager.getBigDecimalToString(
					discountAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setDiscountRate(
				NumberManager.getBigDecimalToString(
					discountRate.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTaxRate(
				CoreFunctionalityConvert.convertTaxRate(tax)
			)
			//	Totals
			.setTotalDiscountAmount(
				NumberManager.getBigDecimalToString(
					totalDiscountAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalTaxAmount(
				NumberManager.getBigDecimalToString(
					totalTaxAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalBaseAmount(
				NumberManager.getBigDecimalToString(
					totalBaseAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalBaseAmountWithTax(
				NumberManager.getBigDecimalToString(
					totalBaseAmountWithTax.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalAmount(
				NumberManager.getBigDecimalToString(
					totalAmount.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalAmountConverted(
				NumberManager.getBigDecimalToString(
					totalAmountConverted.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalAmountWithTax(
				NumberManager.getBigDecimalToString(
					totalAmountWithTax.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setTotalAmountWithTaxConverted(
				NumberManager.getBigDecimalToString(
					totalAmountWithTaxConverted.setScale(
						standardPrecision,
						RoundingMode.HALF_UP
					)
				)
			)
			.setUom(
				CoreFunctionalityConvert.convertProductConversion(uom)
			)
			.setProductUom(
				CoreFunctionalityConvert.convertProductConversion(productUom)
			)
			.setResourceAssignment(
				TimeControl.convertResourceAssignment(
					orderLine.getS_ResourceAssignment_ID()
				)
			)
			.setSourceRmaLineId(
				orderLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Source_RMA_ID)
			)
			.setSourceOrderLineId(
				orderLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_ECA14_Source_OrderLine_ID)
			)
			.setGiftCardQuantity(
				NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromString(
						orderLine.get_ValueAsString(
							"GiftCardQuantity"
						)
					)
				)
			)
		;
	}

}
