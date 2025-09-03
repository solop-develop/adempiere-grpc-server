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
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MPPVendorTransaction;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessor;
import org.compiere.model.MPaymentProcessorBatch;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/338">https://github.com/solop-develop/adempiere-base/issues/338</a>
 */
public class AllocatePaymentsFromBankStatement extends AllocatePaymentsFromBankStatementAbstract {
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
		AtomicInteger counter = new AtomicInteger(0);
		String whereClause = MInvoice.COLUMNNAME_C_PaymentProcessorBatch_ID +" = ? AND IsSOTrx = 'Y'";
		MInvoice vendorFeesInvoice = new Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
			.setParameters(getRecord_ID())
			.first();
		if (vendorFeesInvoice == null){
			throw new AdempiereException("@C_Invoice@ @NotFound@");
		}
		if(!getSelectionKeys().isEmpty()) {
			getSelectionKeys().forEach(key -> {
				MPayment payment = new MPayment(getCtx(), key, get_TrxName());
				MPPVendorTransaction vendorTransaction = new MPPVendorTransaction(getCtx(), 0, get_TrxName());
				vendorTransaction.setC_PaymentProcessorBatch_ID(getRecord_ID());
				vendorTransaction.setDeposit_ID(payment.getC_Payment_ID());
				int bankStatementLineId = getSelectionAsInt(key, "BSL_C_BankStatementLine_ID");
				MBankStatementLine bankStatementLine = new MBankStatementLine(getCtx(), bankStatementLineId, get_TrxName());
				vendorTransaction.setPayAmt(bankStatementLine.getStmtAmt());
				vendorTransaction.setReferenceNo(payment.getDocumentNo());
				//	TODO: Generate Withdrawal from Receipts

				MPayment withdrawal = new MPayment(getCtx(), 0, get_TrxName());
				withdrawal.setDocStatus(MPayment.DOCSTATUS_Drafted);
				withdrawal.setDocAction(MPayment.DOCACTION_Complete);
				MPaymentProcessor paymentProcessor = (MPaymentProcessor) batch.getC_PaymentProcessor();
				int feeChargeId = paymentProcessor.getFeeCharge_ID();
				withdrawal.setC_Charge_ID(feeChargeId);
				withdrawal.setPayAmt(bankStatementLine.getStmtAmt());
				withdrawal.setC_BPartner_ID(paymentProcessor.getPaymentProcessorVendor_ID());

				withdrawal.setC_DocType_ID(false);
				withdrawal.setTenderType(MPayment.TENDERTYPE_Account);
				withdrawal.setC_BankAccount_ID(batch.getTransitBankAccount_ID());
				withdrawal.setIsReceipt(false);
				withdrawal.setC_Currency_ID(paymentProcessor.getFeeCurrency_ID());
				withdrawal.saveEx();

				vendorTransaction.setC_Payment_ID(withdrawal.get_ID());
				vendorTransaction.saveEx();
				//Automatic Allocation
				createInvoiceChargeAllocation(paymentProcessor.getPaymentProcessorVendor_ID(),paymentProcessor.getFeeCurrency_ID(),
					batch.getAD_Org_ID(),payment.getDateTrx(), feeChargeId,"description",
					vendorFeesInvoice, get_TrxName(),bankStatementLine.getStmtAmt()
				);

				counter.incrementAndGet();
			});
		}
		batch.updateTotals();
		return "@Created@ " + counter.get();
	}



	private void createInvoiceChargeAllocation(
			int businessPartnerId,
			int currencyId,
			int organizationId,
			Timestamp transactionDate,
			int chargeId,
			String description,
			MInvoice invoice,
			String transactionName,
			BigDecimal amountToApply
	) {
		if (invoice == null) {
			throw new AdempiereException("Invoice selection is required");
		}

		if (organizationId <= 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}

		if (chargeId <= 0) {
			throw new AdempiereException("Charge ID is required");
		}

		// Create Allocation header
		final String userName = Env.getContext(Env.getCtx(), "#AD_User_Name");
		MAllocationHdr alloc = new MAllocationHdr(
				Env.getCtx(),
				true,
				Env.getContextAsDate(Env.getCtx(),"@#Date@"),
				currencyId,
				userName,
				transactionName
		);
		alloc.setAD_Org_ID(organizationId);

		// Set Description
		if (!Util.isEmpty(description, true)) {
			alloc.setDescription(description);
		}
		alloc.saveEx();

		// Process the single invoice
		int C_Invoice_ID = invoice.get_ID();
		BigDecimal DiscountAmt = Env.ZERO;
		BigDecimal WriteOffAmt = Env.ZERO;
		BigDecimal invoiceOpen = invoice.getOpenAmt(); //TODO: convert from invoice to allocation currency
		// OverUnderAmt needs to be in Allocation Currency
		BigDecimal OverUnderAmt = invoiceOpen.subtract(amountToApply)
				.subtract(DiscountAmt)
				.subtract(WriteOffAmt);

		// Create allocation line for the invoice
		MAllocationLine invoiceLine = new MAllocationLine(
				alloc,
				amountToApply,
				DiscountAmt,
				WriteOffAmt,
				OverUnderAmt
		);
		invoiceLine.setDocInfo(businessPartnerId, invoice.getC_Order_ID(), C_Invoice_ID);
		invoiceLine.saveEx();

		// Create allocation line for the charge
		MAllocationLine chargeLine = new MAllocationLine(
				alloc,
				amountToApply,
				Env.ZERO,
				Env.ZERO,
				Env.ZERO
		);
		chargeLine.set_CustomColumn("C_Charge_ID", chargeId);
		chargeLine.setC_BPartner_ID(businessPartnerId);
		chargeLine.saveEx();



		// Complete the allocation
		if (alloc.get_ID() > 0) {
			if (!alloc.processIt(DocAction.ACTION_Complete)) {
				throw new AdempiereException("@ProcessFailed@: " + alloc.getProcessMsg());
			}
			alloc.saveEx();
		}

		// Test/Set IsPaid for the invoice

		BigDecimal open = invoice.getOpenAmt();
		if (open != null && open.signum() == 0) {
			invoice.setIsPaid(true);
			invoice.saveEx();
		}
	}
}