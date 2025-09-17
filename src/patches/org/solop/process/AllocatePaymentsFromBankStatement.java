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
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MPPVendorTransaction;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessorBatch;
import org.compiere.model.Query;

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
		if (batch.isAutomaticReceipt()) {
			throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @IsAutomaticReceipt@");
		}
		AtomicInteger counter = new AtomicInteger(0);
		String whereClause = MInvoice.COLUMNNAME_C_PaymentProcessorBatch_ID +" = ? AND IsSOTrx = 'Y'";
		MInvoice vendorFeesInvoice = new Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
				.setParameters(getRecord_ID())
				.first();
		if (vendorFeesInvoice == null){
			throw new AdempiereException("@C_Invoice@ @NotFound@");
		}
		X_C_PPBatchConfiguration batchConfiguration = new X_C_PPBatchConfiguration(getCtx(), batch.getC_PPBatchConfiguration_ID(), get_TrxName());
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

				vendorTransaction.saveEx();

				MPayment withdrawal = new MPayment(getCtx(), 0, get_TrxName());
				withdrawal.setDocStatus(MPayment.DOCSTATUS_Drafted);
				withdrawal.setDocAction(MPayment.DOCACTION_Complete);

				withdrawal.setC_Charge_ID(batchConfiguration.getFeeCharge_ID());
				withdrawal.setPayAmt(vendorTransaction.getPayAmt());
				withdrawal.setC_BPartner_ID(batchConfiguration.getC_BPartner_ID());

				withdrawal.setC_DocType_ID(false);
				withdrawal.setTenderType(MPayment.TENDERTYPE_Account);
				withdrawal.setC_BankAccount_ID(batch.getTransitBankAccount_ID());
				withdrawal.setIsReceipt(false);
				withdrawal.setC_Currency_ID(batch.getC_Currency_ID());
				withdrawal.set_ValueOfColumn("C_PaymentProcessorBatch_ID", getRecord_ID());
				withdrawal.saveEx();

				if(!withdrawal.processIt(MPayment.DOCACTION_Complete)) {
					throw new AdempiereException(withdrawal.getProcessMsg());
				}

				counter.incrementAndGet();
			});
		}
		batch.updateTotals();
		return "@Created@ " + counter.get();
	}


}