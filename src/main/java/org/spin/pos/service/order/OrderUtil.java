/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com				  		                 *
 *************************************************************************************/
package org.spin.pos.service.order;

import java.math.BigDecimal;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.spin.base.util.RecordUtil;

/**
 * A util class for change values for documents
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class OrderUtil {
	
	public static boolean isValidOrder(MOrder order) {
		MDocType documentType = MDocType.get(order.getCtx(), order.getC_DocTypeTarget_ID());
		return !documentType.isOffer() && !documentType.isProposal();
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
}
