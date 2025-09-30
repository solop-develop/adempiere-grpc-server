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

import org.adempiere.core.domains.models.X_C_PPBatchConfiguration;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessorBatch;
import org.compiere.model.MPriceList;
import org.compiere.model.MTax;
import org.compiere.model.MTaxCategory;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/338">https://github.com/solop-develop/adempiere-base/issues/338</a>
 */
public class GenerateAPInvoiceFromBatch extends GenerateAPInvoiceFromBatchAbstract {
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
		if(batch.getC_PPBatchConfiguration_ID() <= 0) {
			throw new AdempiereException("@C_PPBatchConfiguration_ID@ @NotFound@");
		}
		if(Optional.ofNullable(batch.getFeeAmt()).orElse(Env.ZERO).signum() == 0) {
			throw new AdempiereException("@FeeAmt@ = 0");
		}
		X_C_PPBatchConfiguration batchConfiguration = new X_C_PPBatchConfiguration(getCtx(), batch.getC_PPBatchConfiguration_ID(), get_TrxName());
		if(batch.getC_Currency_ID() <= 0) {
			throw new AdempiereException("@C_Currency_ID@ @NotFound@");
		}
		int documentTypeId= 0;
		BigDecimal invoiceAmount = BigDecimal.ZERO;
		int chargeId = 0;
		if ("FEE".equals(getVendorDocumentType())) {
			documentTypeId = batchConfiguration.getPurchaseInvoiceDocType_ID();
			invoiceAmount = batch.getFeeAmt();
			chargeId = batchConfiguration.getFeeCharge_ID();
		} else if ("WTH".equals(getVendorDocumentType())) {
			documentTypeId = batchConfiguration.getWithholdingDocType_ID();
			invoiceAmount = batch.getWithholdingAmt();
			chargeId = batchConfiguration.getWithholdingCharge_ID();
		}
		if (documentTypeId <= 0) {
			throw new AdempiereException("@C_DocTypeTarget_ID@ @NotFound@");
		}
		if (chargeId <= 0) {
			throw new AdempiereException("@C_Charge_ID@ @NotFound@");
		}
		if (invoiceAmount.signum() <= 0) {
			throw new AdempiereException("@Amount@ @Invalid@");
		}
		String whereClause = "IsSOTrx = 'N' AND C_PaymentProcessorBatch_ID = ? AND DocStatus NOT IN ('RE','VO') AND C_DocTypeTarget_ID = ?";
		List<Integer> invoiceIds = new Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
				.setParameters(getRecord_ID(), documentTypeId)
				.getIDsAsList();
		if (isOverwriteAmtAndCharge()) {
			AtomicReference<BigDecimal> usedInvoiceAmount = new AtomicReference<>(BigDecimal.ZERO);

			invoiceIds.forEach(invoiceId -> {
				MInvoice invoice = new MInvoice(getCtx(), invoiceId, get_TrxName());
				usedInvoiceAmount.getAndUpdate(current -> current.add(invoice.getGrandTotal()));
			});

			if (getAmount().signum() == 0) {
				throw new AdempiereException("@Amount@ @Invalid@");//TODO: better message
			}
			if (usedInvoiceAmount.get().add(getAmount()).compareTo(invoiceAmount) > 0){
				throw new AdempiereException("@Amount@ @Invalid@");//TODO: better message
			}
			invoiceAmount = getAmount();
			chargeId = getChargeId();
		} else {
			if (!invoiceIds.isEmpty()) {
				throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @Invalid@");//TODO: better message
			}
		}




		MBPartner businessPartner = MBPartner.get(getCtx(), batch.getC_BPartner_ID());
		//	Create Invoice
		MInvoice invoice = new MInvoice (getCtx(), 0, get_TrxName());
		invoice.setClientOrg(batch.getAD_Client_ID(), batch.getAD_Org_ID());
		invoice.setC_DocTypeTarget_ID(documentTypeId);
		invoice.setIsSOTrx(false);
		invoice.setBPartner(businessPartner);
		invoice.setSalesRep_ID(getAD_User_ID());	//	caller
		invoice.setDateInvoiced(getDateDoc());
		String currencyIsoCode = MCurrency.get(getCtx(), batch.getC_Currency_ID()).getISO_Code();
		MPriceList priceList = MPriceList.getDefault(getCtx(), false, currencyIsoCode);
		if(priceList == null) {
			throw new IllegalArgumentException("@M_PriceList_ID@ @NotFound@ (@C_Currency_ID@ " + currencyIsoCode + ")");
		}
		MCharge charge =MCharge.get(getCtx(), chargeId);

		Optional<MTax> optionalTax = Arrays.asList(MTax.getAll(Env.getCtx()))
				.parallelStream()
				.filter(tax -> tax.getC_TaxCategory_ID() == charge.getC_TaxCategory_ID()
						&& (!tax.isSalesTax()
						|| (!Util.isEmpty(tax.getSOPOType())
						&& (tax.getSOPOType().equals(MTax.SOPOTYPE_Both)
						|| tax.getSOPOType().equals(MTax.SOPOTYPE_PurchaseTax)))))
				.findFirst()
				;

		if (optionalTax.isPresent()) {
			BigDecimal taxRate = optionalTax.get().getRate();
			int precision = priceList.getStandardPrecision();
			BigDecimal multiplier = BigDecimal.ONE.add(taxRate.divide(Env.ONEHUNDRED, precision, RoundingMode.HALF_UP));
			invoiceAmount = invoiceAmount.divide(multiplier, precision, RoundingMode.HALF_UP);
		}
		invoice.setM_PriceList_ID(priceList.getM_PriceList_ID());
		invoice.setC_PaymentProcessorBatch_ID(batch.getC_PaymentProcessorBatch_ID());
		invoice.setDocStatus(MInvoice.DOCSTATUS_Drafted);
		invoice.setDocAction(getDocAction());
		//
		invoice.saveEx();

		//	Create Invoice Line
		MInvoiceLine invoiceLine = new MInvoiceLine(invoice);
		invoiceLine.setC_Charge_ID(chargeId);
		invoiceLine.setQty(1);
		invoiceLine.setPrice(invoiceAmount);
		invoiceLine.setTax();
		invoiceLine.saveEx();
		if(!invoice.processIt(getDocAction())) {
			throw new AdempiereException(invoice.getProcessMsg());
		}
		invoice.saveEx();
		if(getDocAction().equals(MInvoice.DOCACTION_Complete)) {
			MPayment payment = getPayment(invoice, batch);
			payment.saveEx();
			if(!payment.processIt(MPayment.DOCACTION_Complete)) {
				throw new AdempiereException(payment.getProcessMsg());
			}
			payment.saveEx();
		}
		return "@Created@ " + invoice.getDocumentNo();
	}

	private MPayment getPayment(MInvoice invoice, MPaymentProcessorBatch batch) {
		MPayment payment = new MPayment(getCtx(), 0, get_TrxName());
		payment.setDocStatus(MPayment.DOCSTATUS_Drafted);
		payment.setDocAction(MPayment.DOCACTION_Complete);
		payment.setC_BPartner_ID(invoice.getC_BPartner_ID());
		payment.setIsReceipt(false);
		payment.setC_BankAccount_ID(batch.getTransitBankAccount_ID());
		payment.setDateAcct(invoice.getDateInvoiced());
		payment.setTenderType(MPayment.TENDERTYPE_Account);
		payment.setPayAmt(invoice.getGrandTotal());
		payment.setC_DocType_ID(false);
		payment.setC_Invoice_ID(invoice.getC_Invoice_ID());
		payment.setC_Currency_ID(invoice.getC_Currency_ID());
		return payment;
	}
}