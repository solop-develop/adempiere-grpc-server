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
package org.spin.pos.service.cash;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.core.domains.models.I_C_BP_BankAccount;
import org.adempiere.core.domains.models.I_C_Bank;
import org.adempiere.core.domains.models.I_C_ConversionType;
import org.adempiere.core.domains.models.I_C_Currency;
import org.adempiere.core.domains.models.I_C_PaymentMethod;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBank;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.CreatePaymentRequest;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.pos.service.order.OrderManagement;
import org.spin.pos.service.order.OrderUtil;

/**
 * This class manage all related to collecting and payment methods
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class CollectingManagement {
	
	/**
	 * Create Payment based on request, transaction name and pos
	 * @param request
	 * @param pointOfSalesDefinition
	 * @param transactionName
	 * @return
	 */
	public static  MPayment createPaymentFromOrder(MOrder salesOrder, CreatePaymentRequest request, MPOS pointOfSalesDefinition, String transactionName) {
		OrderManagement.validateOrderReleased(salesOrder);
		OrderUtil.setCurrentDate(salesOrder);
		String tenderType = request.getTenderTypeCode();
		if(Util.isEmpty(tenderType)) {
			tenderType = MPayment.TENDERTYPE_Cash;
		}
		if(pointOfSalesDefinition.getC_BankAccount_ID() <= 0) {
			throw new AdempiereException("@NoCashBook@");
		}
        //	Amount
        BigDecimal paymentAmount = ValueUtil.getBigDecimalFromDecimal(request.getAmount());
		if(paymentAmount == null || paymentAmount.compareTo(Env.ZERO) == 0) {
			throw new AdempiereException("@PayAmt@ @NotFound@");
		}
		//	Order
		int currencyId = RecordUtil.getIdFromUuid(I_C_Currency.Table_Name, request.getCurrencyUuid(), transactionName);
		if(currencyId <= 0) {
			currencyId = salesOrder.getC_Currency_ID();
		}
		//	Throw if not exist conversion
		ConvertUtil.validateConversion(salesOrder, currencyId, pointOfSalesDefinition.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID), RecordUtil.getDate());
		//	
		MPayment payment = new MPayment(Env.getCtx(), 0, transactionName);
		payment.setC_BankAccount_ID(pointOfSalesDefinition.getC_BankAccount_ID());
		//	Get from POS
		int documentTypeId;
		if(!request.getIsRefund()) {
			documentTypeId = pointOfSalesDefinition.get_ValueAsInt("POSCollectingDocumentType_ID");
		} else {
			documentTypeId = pointOfSalesDefinition.get_ValueAsInt("POSRefundDocumentType_ID");
		}
		if(documentTypeId > 0) {
			payment.setC_DocType_ID(documentTypeId);
		} else {
			payment.setC_DocType_ID(!request.getIsRefund());
		}
		payment.setAD_Org_ID(salesOrder.getAD_Org_ID());
        if(!Util.isEmpty(request.getPaymentAccountDate())) {
        	Timestamp date = ValueUtil.getDateFromString(request.getPaymentAccountDate());
        	if(date != null) {
        		payment.setDateAcct(date);
        	}
        }
        payment.setTenderType(tenderType);
        payment.setDescription(Optional.ofNullable(request.getDescription()).orElse(salesOrder.getDescription()));
        payment.setC_BPartner_ID (salesOrder.getC_BPartner_ID());
        payment.setC_Currency_ID(currencyId);
        payment.setC_POS_ID(pointOfSalesDefinition.getC_POS_ID());
        if(salesOrder.getSalesRep_ID() > 0) {
        	payment.set_ValueOfColumn("CollectingAgent_ID", salesOrder.getSalesRep_ID());
        }
        if(pointOfSalesDefinition.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID) > 0) {
        	payment.setC_ConversionType_ID(pointOfSalesDefinition.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID));
        }
        payment.setPayAmt(paymentAmount);
        //	Order Reference
        payment.setC_Order_ID(salesOrder.getC_Order_ID());
        payment.setDocStatus(MPayment.DOCSTATUS_Drafted);
		if(!Util.isEmpty(request.getDescription())) {
			payment.setDescription(request.getDescription());
		} else {
			int invoiceId = salesOrder.getC_Invoice_ID();
			if(invoiceId > 0) {
				MInvoice invoice = new MInvoice(Env.getCtx(), payment.getC_Invoice_ID(), transactionName);
				payment.setDescription(Msg.getMsg(Env.getCtx(), "Invoice No ") + invoice.getDocumentNo());
			} else {
				payment.setDescription(Msg.getMsg(Env.getCtx(), "Order No ") + salesOrder.getDocumentNo());
			}
		}
		switch (tenderType) {
			case MPayment.TENDERTYPE_Check:
				payment.setCheckNo(request.getReferenceNo());
				break;
			case MPayment.TENDERTYPE_DirectDebit:
				break;
			case MPayment.TENDERTYPE_CreditCard:
				break;
			case MPayment.TENDERTYPE_MobilePaymentInterbank:
				payment.setR_PnRef(request.getReferenceNo());
				break;
			case MPayment.TENDERTYPE_Zelle:
				payment.setR_PnRef(request.getReferenceNo());
				break;
			case MPayment.TENDERTYPE_CreditMemo:
				payment.setR_PnRef(request.getReferenceNo());
				payment.setDocumentNo(request.getReferenceNo());
				payment.setCheckNo(request.getReferenceNo());
				break;
			default:
				payment.setDescription(request.getDescription());
				break;
		}
		//	Payment Method
		if(!Util.isEmpty(request.getPaymentMethodUuid())) {
			int paymentMethodId = RecordUtil.getIdFromUuid(I_C_PaymentMethod.Table_Name, request.getPaymentMethodUuid(), transactionName);
			if(paymentMethodId > 0) {
				payment.set_ValueOfColumn(I_C_PaymentMethod.COLUMNNAME_C_PaymentMethod_ID, paymentMethodId);
			}
		}
		//	Set Bank Id
		if(!Util.isEmpty(request.getBankUuid())) {
			int bankId = RecordUtil.getIdFromUuid(I_C_Bank.Table_Name, request.getBankUuid(), transactionName);
			payment.set_ValueOfColumn(MBank.COLUMNNAME_C_Bank_ID, bankId);
		}
		//	Customer Bank Account
		if(!Util.isEmpty(request.getCustomerBankAccountUuid())) {
			int customerBankAccountId = RecordUtil.getIdFromUuid(I_C_BP_BankAccount.Table_Name, request.getCustomerBankAccountUuid(), transactionName);
			if(customerBankAccountId > 0) {
				payment.setC_BP_BankAccount_ID(customerBankAccountId);
			}
		}
		//	Validate reference
		if(!Util.isEmpty(request.getReferenceNo())) {
			payment.setDocumentNo(request.getReferenceNo());
			payment.addDescription(request.getReferenceNo());
		}
		CashUtil.setCurrentDate(payment);
		if(Util.isEmpty(payment.getDocumentNo())) {
			String value = DB.getDocumentNo(payment.getC_DocType_ID(), transactionName, false,  payment);
	        payment.setDocumentNo(value);
		}
		//	
		payment.saveEx(transactionName);
		return payment;
	}
}
