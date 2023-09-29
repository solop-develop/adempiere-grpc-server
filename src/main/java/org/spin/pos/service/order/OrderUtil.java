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

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Timestamp;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MPriceList;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.spin.base.util.RecordUtil;
import org.spin.pos.util.ColumnsAdded;

/**
 * A util class for change values for documents
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class OrderUtil {

	public static MOrder validateAndGetOrder(int orderId) {
		if (orderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@");
		}
		MOrder order = new MOrder(Env.getCtx(), orderId, null);
		if (order == null || order.getC_Order_ID() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		return order;
	}
	
    public static boolean isDelivered(int orderId, String transactionName) {
 		String sql = "SELECT M_InOut_ID FROM M_InOut "
			+ "WHERE C_Order_ID=? AND DocStatus IN ('CO','CL')";
		int deliveryId = DB.getSQLValue(transactionName, sql, orderId);
		return deliveryId > 0;
	}
    
    public static boolean isInvoiced(int orderId, String transactionName) {
    	String sql = "SELECT C_Invoice_ID FROM C_Invoice "
    			+ "WHERE C_Order_ID=? AND DocStatus IN ('CO','CL')";
    		int invoiceId = DB.getSQLValue(transactionName, sql, orderId);
    		return invoiceId > 0;
	}
	
	/**
	 * Get Refund references from order
	 * @param order
	 * @return
	 * @return List<PO>
	 */
	private static List<PO> getPaymentReferences(MOrder order) {
		if(MTable.get(Env.getCtx(), "C_POSPaymentReference") == null) {
			return new ArrayList<PO>();
		}
		//	
		return new Query(order.getCtx(), "C_POSPaymentReference", "C_Order_ID = ?", order.get_TrxName()).setParameters(order.getC_Order_ID()).list();
	}
	
	public static List<PO> getPaymentReferencesList(MOrder order) {
		return getPaymentReferences(order)
			.stream()
			.filter(paymentReference -> {
				return (!paymentReference.get_ValueAsBoolean("Processed") && !paymentReference.get_ValueAsBoolean("IsPaid")) 
					|| paymentReference.get_ValueAsBoolean("IsKeepReferenceAfterProcess");
			})
			.collect(Collectors.toList());
	}

	public static Optional<BigDecimal> getPaymentChageOrCredit(MOrder order, boolean isCredit) {
		return getPaymentReferencesList(order)
			.stream()
			.filter(paymentReference -> {
				return paymentReference.get_ValueAsBoolean("IsReceipt") == isCredit;
			})
			.map(paymentReference -> {
				BigDecimal amount = ((BigDecimal) paymentReference.get_Value("Amount"));
				return getConvetedAmount(order, paymentReference, amount);
			})
			.collect(Collectors.reducing(BigDecimal::add));
	}

	/**
	 * Set UOM and Quantity based on unit of measure
	 * @param orderLine
	 * @param unitOfMeasureId
	 * @param quantity
	 */
	public static void updateUomAndQuantity(MOrderLine orderLine, int unitOfMeasureId, BigDecimal quantity) {
		if(quantity != null) {
			orderLine.setQty(quantity);
		}
		if(unitOfMeasureId > 0) {
			orderLine.setC_UOM_ID(unitOfMeasureId);
		}
		BigDecimal quantityEntered = orderLine.getQtyEntered();
		BigDecimal priceEntered = orderLine.getPriceEntered();
		BigDecimal convertedQuantity = MUOMConversion.convertProductFrom(orderLine.getCtx(), orderLine.getM_Product_ID(), orderLine.getC_UOM_ID(), quantityEntered);
		BigDecimal convertedPrice = MUOMConversion.convertProductTo(orderLine.getCtx(), orderLine.getM_Product_ID(), orderLine.getC_UOM_ID(), priceEntered);
		orderLine.setQtyOrdered(convertedQuantity);
		orderLine.setPriceActual(convertedPrice);
		orderLine.setLineNetAmt();
		orderLine.saveEx();
	}

	public static boolean isValidOrder(MOrder order) {
		return !isBindingOffer(order);
	}
	
	public static boolean isBindingOffer(MOrder order) {
		MDocType documentType = MDocType.get(order.getCtx(), order.getC_DocTypeTarget_ID());
		return documentType.isOffer() || documentType.isProposal();
	}

	/**
	 * Set current date to order
	 * @param salesOrder
	 * @return void
	 */
	public static void setCurrentDate(MOrder salesOrder) {
		//	Ignore if the document is processed
		if(salesOrder.isProcessed() || salesOrder.isProcessing()) {
			return;
		}
		if(!salesOrder.getDateOrdered().equals(RecordUtil.getDate())
				|| !salesOrder.getDateAcct().equals(RecordUtil.getDate())) {
			salesOrder.setDateOrdered(RecordUtil.getDate());
			salesOrder.setDateAcct(RecordUtil.getDate());
			salesOrder.saveEx();
		}
	}
	
	/**
	 * Get converted Amount from POS
	 * @param pos
	 * @param fromCurrencyId
	 * @param amount
	 * @return
	 */
	public static BigDecimal getConvetedAmount(MPOS pos, int fromCurrencyId, BigDecimal amount) {
		int toCurrencyId = pos.getM_PriceList().getC_Currency_ID();
		if(fromCurrencyId == toCurrencyId
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(pos.getCtx(), amount, fromCurrencyId, toCurrencyId, TimeUtil.getDay(System.currentTimeMillis()), pos.get_ValueAsInt("C_ConversionType_ID"), pos.getAD_Client_ID(), pos.getAD_Org_ID());
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}
	
	/**
	 * Get Converted Amount based on Order currency and optional currency id
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvetedAmount(MOrder order, int fromCurrencyId, BigDecimal amount) {
		if(fromCurrencyId == order.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(order.getCtx(), amount, fromCurrencyId, order.getC_Currency_ID(), order.getDateAcct(), order.get_ValueAsInt("C_ConversionType_ID"), order.getAD_Client_ID(), order.getAD_Org_ID());
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}
	
	/**
	 * Get Converted Amount based on Order currency
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvetedAmount(MOrder order, PO payment, BigDecimal amount) {
		if(payment.get_ValueAsInt("C_Currency_ID") == order.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(payment.getCtx(), amount, payment.get_ValueAsInt("C_Currency_ID"), order.getC_Currency_ID(), order.getDateAcct(), payment.get_ValueAsInt("C_ConversionType_ID"), payment.getAD_Client_ID(), payment.getAD_Org_ID());
		//	
		return Optional.ofNullable(convertedAmount).orElse(Env.ZERO);
	}
	
	/**
	 * Get Converted Amount based on Order currency
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvetedAmount(MOrder order, MPayment payment, BigDecimal amount) {
		if(payment.getC_Currency_ID() == order.getC_Currency_ID()
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(payment.getCtx(), amount, payment.getC_Currency_ID(), order.getC_Currency_ID(), payment.getDateAcct(), payment.getC_ConversionType_ID(), payment.getAD_Client_ID(), payment.getAD_Org_ID());
		if(convertedAmount == null
				|| convertedAmount.compareTo(Env.ZERO) == 0) {
			throw new AdempiereException(MConversionRate.getErrorMessage(payment.getCtx(), "ErrorConvertingDocumentCurrencyToBaseCurrency", payment.getC_Currency_ID(), order.getC_Currency_ID(), payment.getC_ConversionType_ID(), payment.getDateAcct(), payment.get_TrxName()));
		}
		//	
		return convertedAmount;
	}
	
	/**
	 * Get Converted Amount based on Order currency
	 * @param order
	 * @param payment
	 * @return
	 * @return BigDecimal
	 */
	public static BigDecimal getConvetedAmount(MPOS pos, MPayment payment, BigDecimal amount) {
		int toCurrencyId = pos.getM_PriceList().getC_Currency_ID();
		if(payment.getC_Currency_ID() == toCurrencyId
				|| amount == null
				|| amount.compareTo(Env.ZERO) == 0) {
			return amount;
		}
		BigDecimal convertedAmount = MConversionRate.convert(payment.getCtx(), amount, payment.getC_Currency_ID(), toCurrencyId, payment.getDateAcct(), payment.getC_ConversionType_ID(), payment.getAD_Client_ID(), payment.getAD_Org_ID());
		if(convertedAmount == null
				|| convertedAmount.compareTo(Env.ZERO) == 0) {
			throw new AdempiereException(MConversionRate.getErrorMessage(payment.getCtx(), "ErrorConvertingDocumentCurrencyToBaseCurrency", payment.getC_Currency_ID(), toCurrencyId, payment.getC_ConversionType_ID(), payment.getDateAcct(), payment.get_TrxName()));
		}
		//	
		return convertedAmount;
	}
	
    public static Timestamp getToday() {
    	return TimeUtil.getDay(System.currentTimeMillis());
    }
    
    
    /**
     * Create new order from source order
     * @param pos
     * @param sourceOrder
     * @param transactionName
     * @return
     */
    public static MOrder copyOrder(MPOS pos, MOrder sourceOrder, String transactionName) {
    	MOrder salesOrder = new MOrder (sourceOrder.getCtx(), 0, transactionName);
		salesOrder.set_TrxName(transactionName);
		PO.copyValues(sourceOrder, salesOrder, false);
		//
		salesOrder.setDocStatus (DocAction.STATUS_Drafted);		//	Draft
		salesOrder.setDocAction(DocAction.ACTION_Complete);
		//
		salesOrder.setIsSelected (false);
		salesOrder.setDateOrdered(OrderUtil.getToday());
		salesOrder.setDateAcct(OrderUtil.getToday());
		salesOrder.setDatePromised(OrderUtil.getToday());	//	assumption
		salesOrder.setDatePrinted(null);
		salesOrder.setIsPrinted (false);
		//
		salesOrder.setIsApproved (false);
		salesOrder.setIsCreditApproved(false);
		salesOrder.setC_Payment_ID(0);
		salesOrder.setC_CashLine_ID(0);
		//	Amounts are updated  when adding lines
		salesOrder.setGrandTotal(Env.ZERO);
		salesOrder.setTotalLines(Env.ZERO);
		//
		salesOrder.setIsDelivered(false);
		salesOrder.setIsInvoiced(false);
		salesOrder.setIsSelfService(false);
		salesOrder.setIsTransferred (false);
		salesOrder.setPosted (false);
		salesOrder.setProcessed (false);
		salesOrder.setAD_Org_ID(sourceOrder.getAD_Org_ID());
		salesOrder.setC_DocTypeTarget_ID(sourceOrder.getC_DocTypeTarget_ID());
        //	Set references
		salesOrder.setC_POS_ID(pos.getC_POS_ID());
		salesOrder.setC_BPartner_ID(sourceOrder.getC_BPartner_ID());
		salesOrder.setRef_Order_ID(0);
		salesOrder.setProcessed(false);
		//	
		copyAccountDimensions(sourceOrder, salesOrder);
		salesOrder.setM_PriceList_ID(sourceOrder.getM_PriceList_ID());
		return salesOrder;
    }
    
    /**
     * Copy a order line and create a new instance for Order Line
     * @param targetOrder
     * @param sourcerOrderLine
     * @param keepPrice
     * @param transactionName
     * @return
     */
    public static MOrderLine copyOrderLine(MOrderLine sourcerOrderLine, MOrder targetOrder, boolean keepPrice, String transactionName) {
    	MOrderLine salesOrderLine = new MOrderLine(targetOrder);
		if (sourcerOrderLine.getM_Product_ID() > 0) {
			salesOrderLine.setM_Product_ID(sourcerOrderLine.getM_Product_ID());
			if(sourcerOrderLine.getM_AttributeSetInstance_ID() > 0) {
				salesOrderLine.setM_AttributeSetInstance_ID(sourcerOrderLine.getM_AttributeSetInstance_ID());
			}
		} else if(sourcerOrderLine.getC_Charge_ID() != 0) {
			salesOrderLine.setC_Charge_ID(sourcerOrderLine.getC_Charge_ID());
		}
		//	
		copyAccountDimensions(sourcerOrderLine, salesOrderLine);
		//	Set quantity
		if(keepPrice) {
			salesOrderLine.setQty(sourcerOrderLine.getQtyEntered());
			salesOrderLine.setQtyOrdered(sourcerOrderLine.getQtyOrdered());
			salesOrderLine.setC_UOM_ID(sourcerOrderLine.getC_UOM_ID());
			salesOrderLine.setPriceList(sourcerOrderLine.getPriceList());
			salesOrderLine.setPriceEntered(sourcerOrderLine.getPriceEntered());
			salesOrderLine.setPriceActual(sourcerOrderLine.getPriceActual());
			salesOrderLine.setC_Tax_ID(sourcerOrderLine.getC_Tax_ID());
		} else {
			salesOrderLine.setQty(sourcerOrderLine.getQtyEntered());
			salesOrderLine.setQtyOrdered(sourcerOrderLine.getQtyOrdered());
			salesOrderLine.setC_UOM_ID(sourcerOrderLine.getC_UOM_ID());
			salesOrderLine.setC_Tax_ID(sourcerOrderLine.getC_Tax_ID());
			salesOrderLine.setPrice();
			salesOrderLine.setTax();
		}
		return salesOrderLine;
    }
    
    /**
     * Create all lines from order
     * @param sourceOrder
     * @param targetOrder
     * @param transactionName
     */
    public static void copyOrderLinesFromOrder(MOrder sourceOrder, MOrder targetOrder, String transactionName) {
    	new Query(sourceOrder.getCtx(), I_C_OrderLine.Table_Name, "C_Order_ID = ?", transactionName)
    		.setParameters(sourceOrder.getC_Order_ID())
    		.setClient_ID()
    		.getIDsAsList()
    		.forEach(sourceOrderLineId -> {
    			MOrderLine sourcerOrderLine = new MOrderLine(sourceOrder.getCtx(), sourceOrderLineId, transactionName);
				//	Create new Invoice Line
				MOrderLine targetOrderLine = copyOrderLine(sourcerOrderLine, targetOrder, false, transactionName);
				//	Save
				targetOrderLine.saveEx(transactionName);
    		});
    }
    
    /**
     * Create all lines from order
     * @param sourceRma
     * @param targetOrder
     * @param transactionName
     */
    public static void copyOrderLinesFromRMA(MOrder sourceRma, MOrder targetOrder, String transactionName) {
    	new Query(sourceRma.getCtx(), I_C_OrderLine.Table_Name, "C_Order_ID = ?", transactionName)
    		.setParameters(sourceRma.getC_Order_ID())
    		.setClient_ID()
    		.getIDsAsList()
    		.forEach(sourceRmaLineId -> {
    			MOrderLine sourcerRmaLine = new MOrderLine(sourceRma.getCtx(), sourceRmaLineId, transactionName);
				//	Create new Invoice Line
				MOrderLine targetOrderLine = copyOrderLine(sourcerRmaLine, targetOrder, true, transactionName);
				targetOrderLine.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ECA14_Source_RMALine_ID, sourcerRmaLine.getC_OrderLine_ID());
				BigDecimal availableQuantity = getAvailableQuantityForSell(sourcerRmaLine.getC_OrderLine_ID(), sourcerRmaLine.getQtyEntered(), targetOrderLine.getQtyEntered());
				if(availableQuantity.compareTo(Env.ZERO) > 0) {
					OrderUtil.updateUomAndQuantity(targetOrderLine, targetOrderLine.getC_UOM_ID(), availableQuantity);
    			}
				//	Just copy available
//				else {
//    				throw new AdempiereException("@QtyInsufficient@");
//    			}
    		});
    }
    
    /**
     * Copy all account dimensions
     * @param source
     * @param target
     */
    public static void copyAccountDimensions(PO source, PO target) {
		//	
		if(source.get_ValueAsInt("AD_OrgTrx_ID") != 0) {
			target.set_ValueOfColumn("AD_OrgTrx_ID", source.get_ValueAsInt("AD_OrgTrx_ID"));
		}
		if(source.get_ValueAsInt("C_Project_ID") != 0) {
			target.set_ValueOfColumn("C_Project_ID", source.get_ValueAsInt("C_Project_ID"));
		}
		if(source.get_ValueAsInt("C_Campaign_ID") != 0) {
			target.set_ValueOfColumn("C_Campaign_ID", source.get_ValueAsInt("C_Campaign_ID"));
		}
		if(source.get_ValueAsInt("C_Activity_ID") != 0) {
			target.set_ValueOfColumn("C_Activity_ID", source.get_ValueAsInt("C_Activity_ID"));
		}
		if(source.get_ValueAsInt("User1_ID") != 0) {
			target.set_ValueOfColumn("User1_ID", source.get_ValueAsInt("User1_ID"));
		}
		if(source.get_ValueAsInt("User2_ID") != 0) {
			target.set_ValueOfColumn("User2_ID", source.get_ValueAsInt("User2_ID"));
		}
		if(source.get_ValueAsInt("User3_ID") != 0) {
			target.set_ValueOfColumn("User3_ID", source.get_ValueAsInt("User3_ID"));
		}
		if(source.get_ValueAsInt("User4_ID") != 0) {
			target.set_ValueOfColumn("User4_ID", source.get_ValueAsInt("User4_ID"));
		}
    }

	public static BigDecimal getCreditAmount(MOrder order) {
		if (order == null) {
			return BigDecimal.ZERO;
		}
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		int standardPrecision = priceList.getStandardPrecision();

		BigDecimal creditAmt = BigDecimal.ZERO.setScale(standardPrecision, RoundingMode.HALF_UP);
		Optional<BigDecimal> maybeCreditAmt = getPaymentChageOrCredit(order, true);
		if (maybeCreditAmt.isPresent()) {
			creditAmt = maybeCreditAmt.get()
				.setScale(standardPrecision, RoundingMode.HALF_UP);
		}
		return creditAmt;
	}

	public static BigDecimal getChargeAmount(MOrder order) {
		if (order == null) {
			return BigDecimal.ZERO;
		}
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		int standardPrecision = priceList.getStandardPrecision();

		BigDecimal chargeAmt = BigDecimal.ZERO.setScale(standardPrecision, RoundingMode.HALF_UP);
		Optional<BigDecimal> maybeChargeAmt = getPaymentChageOrCredit(order, false);
		if (maybeChargeAmt.isPresent()) {
			chargeAmt = maybeChargeAmt.get()
				.setScale(standardPrecision, RoundingMode.HALF_UP);
		}
		return chargeAmt;
	}

	public static BigDecimal getTotalOpenAmount(MOrder order) {
		if (order == null) {
			return BigDecimal.ZERO;
		}
		
		BigDecimal grandTotal = order.getGrandTotal();
		BigDecimal creditAmt = getCreditAmount(order);
		BigDecimal chargeAmt = getChargeAmount(order);

		BigDecimal grandTotalWithCreditCharges = grandTotal.subtract(creditAmt).add(chargeAmt);

		return grandTotalWithCreditCharges;
	}
	
	/**
	 * Return true if the document should be apply for writeoff
	 * @param order
	 * @param openAmount
	 * @return
	 */
	public static boolean isAutoWriteOff(MOrder order, BigDecimal openAmount) {
		if (order == null
				|| openAmount == null) {
			return false;
		}
		MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), order.get_TrxName());
		return openAmount.setScale(priceList.getStandardPrecision()).compareTo(Env.ZERO) == 0;
	}
	
	/**
	 * Get Write Off Amount based on open amount vs payment amount
	 * @param openAmount
	 * @param paymentAmount
	 * @return
	 */
	public static BigDecimal getWriteOffPercent(BigDecimal openAmount, BigDecimal paymentAmount, int precision) {
		if(Optional.ofNullable(openAmount).orElse(Env.ZERO).compareTo(Env.ZERO) == 0) {
			return null;
		}
		BigDecimal difference = Optional.ofNullable(openAmount).orElse(Env.ZERO).subtract(Optional.ofNullable(paymentAmount).orElse(Env.ZERO));
		return difference.divide(Optional.ofNullable(openAmount).orElse(Env.ZERO), MathContext.DECIMAL128).setScale(precision, RoundingMode.HALF_UP).multiply(Env.ONEHUNDRED);
	}
	
	/**
	 * Get Payment Amount
	 * @param order
	 * @return
	 */
	public static BigDecimal getTotalPaymentAmount(MOrder order) {
		if (order == null) {
			return BigDecimal.ZERO;
		}
		Optional<BigDecimal> paidAmount = MPayment.getOfOrder(order).stream().map(payment -> {
			BigDecimal paymentAmount = payment.getPayAmt();
			if(paymentAmount.compareTo(Env.ZERO) == 0
					&& payment.getTenderType().equals(MPayment.TENDERTYPE_CreditMemo)) {
				MInvoice creditMemo = new Query(payment.getCtx(), MInvoice.Table_Name, "C_Payment_ID = ?", payment.get_TrxName()).setParameters(payment.getC_Payment_ID()).first();
				if(creditMemo != null) {
					paymentAmount = creditMemo.getGrandTotal();
				}
			}
			if(!payment.isReceipt()) {
				paymentAmount = payment.getPayAmt().negate();
			}
			return getConvetedAmount(order, payment, paymentAmount);
		}).collect(Collectors.reducing(BigDecimal::add));

		List<PO> paymentReferencesList = getPaymentReferencesList(order);
		Optional<BigDecimal> paymentReferenceAmount = paymentReferencesList.stream()
				.map(paymentReference -> {
			BigDecimal amount = ((BigDecimal) paymentReference.get_Value("Amount"));
			if(paymentReference.get_ValueAsBoolean("IsReceipt")) {
				amount = amount.negate();
			}
			return getConvetedAmount(order, paymentReference, amount);
		}).collect(Collectors.reducing(BigDecimal::add));

		BigDecimal paymentAmount = Env.ZERO;
		if(paidAmount.isPresent()) {
			paymentAmount = paidAmount.get();
		}		
		BigDecimal totalPaymentAmount = paymentAmount;
		if(paymentReferenceAmount.isPresent()) {
			totalPaymentAmount = totalPaymentAmount.subtract(paymentReferenceAmount.get());
		}

		return totalPaymentAmount;
	}

    /**
     * Get Available Quantity for Return Order
     * @param sourceRmaLineId
     * @param sourceQuantity
     * @param quantityToReturn
     * @return
     */
    public static BigDecimal getAvailableQuantityForSell(int sourceRmaLineId, BigDecimal sourceQuantity, BigDecimal quantityToReturn) {
    	BigDecimal quantity = getSoldQuantity(sourceRmaLineId);
    	BigDecimal availableQuantity = Optional.ofNullable(sourceQuantity).orElse(Env.ZERO).subtract(Optional.ofNullable(quantity).orElse(Env.ZERO));
    	if(availableQuantity.compareTo(quantityToReturn) <= 0) {
    		return availableQuantity;
    	}
    	return quantityToReturn;
    }
    
    public static BigDecimal getAvailableQuantityForSell(int sourceRmaLineId, int orderLineId, BigDecimal sourceQuantity, BigDecimal quantityToReturn) {
    	BigDecimal quantity = getSoldQuantityExcludeSalesOrder(sourceRmaLineId, orderLineId);
    	BigDecimal availableQuantity = Optional.ofNullable(sourceQuantity).orElse(Env.ZERO).subtract(Optional.ofNullable(quantity).orElse(Env.ZERO));
    	if(availableQuantity.compareTo(quantityToReturn) <= 0) {
    		return availableQuantity;
    	}
    	return quantityToReturn;
    }
    
    /**
     * Get sum of complete returned quantity for order
     * @param sourceRmaLineId
     * @return
     */
    public static BigDecimal getSoldQuantity(int sourceRmaLineId) {
    	BigDecimal quantity = new Query(Env.getCtx(), I_C_OrderLine.Table_Name, ColumnsAdded.COLUMNNAME_ECA14_Source_RMALine_ID + " = ? "
    			+ "AND EXISTS(SELECT 1 FROM C_Order o WHERE o.C_Order_ID = C_OrderLine.C_Order_ID)", null)
    			.setParameters(sourceRmaLineId)
    			.aggregate(I_C_OrderLine.COLUMNNAME_QtyEntered, Query.AGGREGATE_SUM);
    	return Optional.ofNullable(quantity).orElse(Env.ZERO);
    }
    
    public static BigDecimal getSoldQuantityExcludeSalesOrder(int sourceRmaLineId, int salesOrderLineId) {
    	BigDecimal quantity = new Query(Env.getCtx(), I_C_OrderLine.Table_Name, ColumnsAdded.COLUMNNAME_ECA14_Source_RMALine_ID + " = ? AND C_OrderLine_ID <> ? "
    			+ "AND EXISTS(SELECT 1 FROM C_Order o WHERE o.C_Order_ID = C_OrderLine.C_Order_ID)", null)
    			.setParameters(sourceRmaLineId, salesOrderLineId)
    			.aggregate(I_C_OrderLine.COLUMNNAME_QtyEntered, Query.AGGREGATE_SUM);
    	return Optional.ofNullable(quantity).orElse(Env.ZERO);
    }
	
}
