/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.solop.sp016.util;

import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_M_InventoryLine;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProductPO;
import org.compiere.model.Query;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Added for handle custom values for ADempiere core
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ConsignedMaterialUtil {
	
	/**	Column Used for Landed Cost	*/
	public static final String COLUMNNAME_PriceLastLanded = "PriceLastLanded";
	
	/**
	 * Get Linked Order Lines from Invoice
	 * @param context
	 * @param invoiceId
	 * @param transactionName
	 * @return
	 */
	public static final List<MOrderLine>getLinkedSalesOrderLinesFromInvoice(Properties context, int invoiceId, String transactionName) {
		return new Query(context, I_C_OrderLine.Table_Name, "EXISTS(SELECT 1 FROM C_InvoiceLine il "
				+ "WHERE il.C_Invoice_ID = ? "
				+ "AND il.C_OrderLine_ID = C_OrderLine.Link_OrderLine_ID)", transactionName)
				.setParameters(invoiceId)
				.list();
	}
	
	/**
	 * Get Linked Inventory Lines from Invoice
	 * @param context
	 * @param invoiceId
	 * @param transactionName
	 * @return
	 */
	public static final List<MInventoryLine>getLinkedInventoryLineFromInvoice(Properties context, int invoiceId, String transactionName) {
		return new Query(context, I_M_InventoryLine.Table_Name, "EXISTS(SELECT 1 FROM C_InvoiceLine il "
				+ "WHERE il.C_Invoice_ID = ? "
				+ "AND il.C_OrderLine_ID = M_InventoryLine.Link_OrderLine_ID)", transactionName)
				.setParameters(invoiceId)
				.list();
	}
	
	/**
	 * Get Linked Order Line from Purchase Order
	 * @param context
	 * @param purchaseOrderId
	 * @param transactionName
	 * @return
	 */
	public static final List<MOrderLine>getLinkedSalesOrderLinesFromPurchaseOrder(Properties context, int purchaseOrderId, String transactionName) {
		return new Query(context, I_C_OrderLine.Table_Name, "EXISTS(SELECT 1 FROM C_Order lo "
				+ "INNER JOIN C_OrderLine lol ON(lol.C_Order_ID = lo.C_Order_ID) "
				+ "WHERE lo.C_Order_ID = ? "
				+ "AND lol.C_OrderLine_ID = C_OrderLine.Link_OrderLine_ID)", transactionName)
				.setParameters(purchaseOrderId)
				.list();
	}
	
	/**
	 * Get Linked Inventory Line from Purchase Order
	 * @param context
	 * @param purchaseOrderId
	 * @param transactionName
	 * @return
	 */
	public static final List<MInventoryLine>getLinkedInventoryLinesFromPurchaseOrder(Properties context, int purchaseOrderId, String transactionName) {
		return new Query(context, I_M_InventoryLine.Table_Name, "EXISTS(SELECT 1 FROM C_Order lo "
				+ "INNER JOIN C_OrderLine lol ON(lol.C_Order_ID = lo.C_Order_ID) "
				+ "WHERE lo.C_Order_ID = ? "
				+ "AND lol.C_OrderLine_ID = M_InventoryLine.Link_OrderLine_ID)", transactionName)
				.setParameters(purchaseOrderId)
				.list();
	}
	
	/**
	 * Get Linked Order Lines from Invoice Line
	 * @param context
	 * @param invoiceLineId
	 * @param transactionName
	 * @return
	 */
	public static final List<MOrderLine>getLinkedSalesOrderLinesFromInvoiceLine(Properties context, int invoiceLineId, String transactionName) {
		return new Query(context, I_C_OrderLine.Table_Name, "EXISTS(SELECT 1 FROM C_InvoiceLine il "
				+ "WHERE il.C_InvoiceLine_ID = ? "
				+ "AND il.C_OrderLine_ID = C_OrderLine.Link_OrderLine_ID)", transactionName)
				.setParameters(invoiceLineId)
				.list();
	}
	
	/**
	 * Get Linked Inventory Lines from Invoice Line
	 * @param context
	 * @param invoiceLineId
	 * @param transactionName
	 * @return
	 */
	public static final List<MInventoryLine>getLinkedInventoryLinesFromInvoiceLine(Properties context, int invoiceLineId, String transactionName) {
		return new Query(context, I_M_InventoryLine.Table_Name, "EXISTS(SELECT 1 FROM C_InvoiceLine il "
				+ "WHERE il.C_InvoiceLine_ID = ? "
				+ "AND il.C_OrderLine_ID = M_InventoryLine.Link_OrderLine_ID)", transactionName)
				.setParameters(invoiceLineId)
				.list();
	}
	
	/**
	 * Recaulculate Invoice Line from Order
	 * @param invoiceLine
	 * @return
	 */
	public static final void recalculateInvoiceLineRate(MInvoiceLine invoiceLine) {
		if(invoiceLine.getC_OrderLine_ID() > 0) {
			if(!invoiceLine.isProcessed()) {
				MOrderLine orderLine = (MOrderLine) invoiceLine.getC_OrderLine();
				MOrder order = orderLine.getParent();
				MInvoice invoice = invoiceLine.getParent();
				if(invoice.getC_Currency_ID() != order.getC_Currency_ID()
						&& !invoice.isReversal()) {
					int conversionTypeId = invoice.getC_ConversionType_ID();
					if(conversionTypeId <= 0) {
						conversionTypeId = order.getC_ConversionType_ID();
					}
					BigDecimal orderPriceList = Optional.ofNullable(orderLine.getPriceList()).orElse(Env.ZERO);
					BigDecimal orderPriceActual = Optional.ofNullable(orderLine.getPriceActual()).orElse(Env.ZERO);
					BigDecimal orderPriceEntered = Optional.ofNullable(orderLine.getPriceEntered()).orElse(Env.ZERO);
					BigDecimal conversionRate = Optional.ofNullable(MConversionRate.getRate (order.getC_Currency_ID(),
		                    invoice.getC_Currency_ID(), invoice.getDateAcct(), conversionTypeId, invoice.getAD_Client_ID(),
		                    invoice.getAD_Org_ID()))
		            		.orElse(Env.ZERO);
					MCurrency currencyTo = MCurrency.get (invoice.getCtx(), invoice.getC_Currency_ID());
					BigDecimal invoicePriceList = orderPriceList.multiply(conversionRate).setScale(currencyTo.getStdPrecision(), RoundingMode.HALF_UP);
					BigDecimal invoicePriceActual = orderPriceActual.multiply(conversionRate).setScale(currencyTo.getStdPrecision(), RoundingMode.HALF_UP);
					BigDecimal invoicePriceEntered = orderPriceEntered.multiply(conversionRate).setScale(currencyTo.getStdPrecision(), RoundingMode.HALF_UP);
					//	Set Price
					invoiceLine.setPriceList(invoicePriceList);
					invoiceLine.setPriceActual(invoicePriceActual);
					invoiceLine.setPriceEntered(invoicePriceEntered);
					invoiceLine.setLineNetAmt();
					invoiceLine.setTaxAmt();
				}
			}
		}
	}
	
	/**
	 * Set Price Limit for sales from purchases
	 * @param invoiceLine
	 * @return
	 */
	public static final void setPriceLimitFromLastPurchase(MInvoiceLine invoiceLine) {
		if(!invoiceLine.isProcessed()
				&& invoiceLine.getM_Product_ID() > 0) {
			MInvoice invoice = invoiceLine.getParent();
			Optional<MProductPO> purchasedProduct = Arrays.asList(MProductPO.getOfProductAndOrg(invoiceLine.getCtx(), invoiceLine.getM_Product_ID(), invoiceLine.getAD_Org_ID(), invoiceLine.get_TrxName()))
				.stream()
				.filter(purchase -> !purchase.isDiscontinued())
				.sorted(Comparator.comparing(MProductPO::getUpdated).reversed())
				.findFirst();
			purchasedProduct.ifPresent(purchase -> {
				BigDecimal convertedPrice = MConversionRate.convert(invoice.getCtx(), purchase.getPricePO(), purchase.getC_Currency_ID(), invoice.getC_Currency_ID(), invoice.getDateInvoiced(), invoice.getC_ConversionType_ID(), invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
				Optional.ofNullable(convertedPrice).ifPresent(convertedPriceToSet -> invoiceLine.setPriceLimit(convertedPriceToSet));
			});
		}
	}
}
