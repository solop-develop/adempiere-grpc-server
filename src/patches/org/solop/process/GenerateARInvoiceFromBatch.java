/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.solop.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessor;
import org.compiere.model.MPaymentProcessorBatch;
import org.compiere.model.MPriceList;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

/**
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/338">https://github.com/solop-develop/adempiere-base/issues/338</a>
 */
public class GenerateARInvoiceFromBatch extends GenerateARInvoiceFromBatchAbstract {
	@Override
	protected void prepare() {
		super.prepare();
		if(getRecord_ID() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
	}

	@Override
	protected String doIt() throws Exception {
		MPaymentProcessorBatch batch = new MPaymentProcessorBatch(getCtx(), getRecord_ID(), get_TrxName());
		if(!batch.isProcessed() || (!batch.getDocStatus().equals(MPaymentProcessorBatch.DOCSTATUS_Completed) && !batch.getDocStatus().equals(MPaymentProcessorBatch.DOCSTATUS_Closed))) {
			throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @Unprocessed@");
		}
		if(batch.getC_PaymentProcessor_ID() <= 0) {
			throw new AdempiereException("@C_PaymentProcessor_ID@ @NotFound@");
		}
		if(Optional.ofNullable(batch.getFeeAmt()).orElse(Env.ZERO).signum() == 0) {
			throw new AdempiereException("@FeeAmt@ = 0");
		}
		MPaymentProcessor paymentProcessor = new MPaymentProcessor(getCtx(), batch.getC_PaymentProcessor_ID(), get_TrxName());
		if(paymentProcessor.getFeeCurrency_ID() <= 0) {
			throw new AdempiereException("@FeeCurrency_ID@ @NotFound@");
		}
		String whereClause = "IsSOTrx = 'Y' AND C_PaymentProcessorBatch_ID = ? AND DocStatus NOT IN ('RE','VO')";
		boolean exists = new Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
			.setParameters(getRecord_ID())
			.match();
		if (exists) {
			throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @Invalid@");
		}
		MBPartner businessPartner = MBPartner.get(getCtx(), batch.getC_BPartner_ID());
		//	Create Invoice
		MInvoice invoice = new MInvoice (getCtx(), 0, get_TrxName());
		invoice.setClientOrg(batch.getAD_Client_ID(), batch.getAD_Org_ID());
		if(getDocTypeTargetId() > 0) {
			invoice.setC_DocTypeTarget_ID(getDocTypeTargetId());	//	ARI
		} else {
			invoice.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ARInvoice);	//	ARI
		}
		invoice.setIsSOTrx(true);
		invoice.setBPartner(businessPartner);
		invoice.setSalesRep_ID(getAD_User_ID());	//	caller
		invoice.setDateInvoiced(getDateDoc());
		String currencyIsoCode = MCurrency.get(getCtx(), paymentProcessor.getFeeCurrency_ID()).getISO_Code();
		MPriceList priceList = (MPriceList) businessPartner.getPO_PriceList();
		if (priceList == null) {
			priceList = MPriceList.getDefault(getCtx(), false, currencyIsoCode);
		}

		if(priceList == null) {
			throw new IllegalArgumentException("@M_PriceList_ID@ @NotFound@ (@C_Currency_ID@ " + currencyIsoCode + ")");
		}
		invoice.setM_PriceList_ID(priceList.getM_PriceList_ID());
		invoice.setC_PaymentProcessorBatch_ID(batch.getC_PaymentProcessorBatch_ID());
		invoice.setDocStatus(MInvoice.DOCSTATUS_Drafted);
		invoice.setDocAction(getDocAction());
		//
		invoice.saveEx();

		//	Create Invoice Line
		MInvoiceLine invoiceLine = new MInvoiceLine(invoice);
		invoiceLine.setC_Charge_ID(paymentProcessor.getFeeCharge_ID());
		invoiceLine.setQty(1);
		invoiceLine.setPrice(batch.getOpenAmt());
		invoiceLine.setTax();
		invoiceLine.saveEx();
		if(!invoice.processIt(getDocAction())) {
			throw new AdempiereException(invoice.getProcessMsg());
		}
		invoice.saveEx();


		MPayment withdrawal = new MPayment(getCtx(), 0, get_TrxName());
		withdrawal.setDocStatus(MPayment.DOCSTATUS_Drafted);
		withdrawal.setDocAction(MPayment.DOCACTION_Complete);

		withdrawal.setC_Charge_ID(paymentProcessor.getFeeCharge_ID());
		withdrawal.setPayAmt(invoice.getGrandTotal());
		withdrawal.setC_BPartner_ID(paymentProcessor.getPaymentProcessorVendor_ID());

		withdrawal.setC_DocType_ID(false);
		withdrawal.setTenderType(MPayment.TENDERTYPE_Account);
		withdrawal.setC_BankAccount_ID(batch.getTransitBankAccount_ID());
		withdrawal.setIsReceipt(false);
		withdrawal.setC_Currency_ID(paymentProcessor.getFeeCurrency_ID());
		withdrawal.set_ValueOfColumn("C_PaymentProcessorBatch_ID", getRecord_ID());
		withdrawal.saveEx();

		if(!withdrawal.processIt(MPayment.DOCACTION_Complete)) {
			throw new AdempiereException(withdrawal.getProcessMsg());
		}
		withdrawal.saveEx();


		return "@Created@: " + invoice.getDocumentNo();
	}


}