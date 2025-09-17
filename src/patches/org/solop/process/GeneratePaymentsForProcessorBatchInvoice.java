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
import org.compiere.model.MInvoice;
import org.compiere.model.MPPVendorTransaction;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessorBatch;
import org.compiere.model.MPaymentProcessorSchedule;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Generate AR Invoice From Payment Processor Batch)
 *  @author ADempiere (generated)
 *  @version Release 3.9.4
 */
public class GeneratePaymentsForProcessorBatchInvoice extends GeneratePaymentsForProcessorBatchInvoiceAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		String whereClause = "DateDoc = ? AND EXISTS (SELECT 1 FROM C_PaymentProcessorBatch ppb WHERE ppb.FinalAccount_ID IS NOT NULL " +
			" AND ppb.DocStatus = 'CO' AND ppb.OpenAmt > 0 AND IsAutomaticReceipt = 'Y' AND ppb.C_PaymentProcessorBatch_ID = C_PaymentProcessorSchedule.C_PaymentProcessorBatch_ID)";
		List<Integer> batchScheduleIds = new Query(getCtx(), MPaymentProcessorSchedule.Table_Name, whereClause, get_TrxName())
			.setParameters(getDateDoc())
			.getIDsAsList();

		AtomicInteger created = new AtomicInteger(0);
		String invoiceWhereClause = "DocumentNo = ? AND C_PaymentProcessorBatch_ID = ? AND DateInvoiced = ? AND IsPaid = 'N' AND DocStatus NOT IN ('RE', 'VO')";
		batchScheduleIds.forEach(batchScheduleId -> {

			MPaymentProcessorSchedule batchSchedule = new MPaymentProcessorSchedule(getCtx(), batchScheduleId, get_TrxName());
			MPaymentProcessorBatch batch = (MPaymentProcessorBatch) batchSchedule.getC_PaymentProcessorBatch();
			if (batch.getC_PPBatchConfiguration_ID() <= 0) {
				return;
			}
			X_C_PPBatchConfiguration batchConfiguration = new X_C_PPBatchConfiguration(getCtx(), batch.getC_PPBatchConfiguration_ID(), get_TrxName());
			if(batchConfiguration.getFeeCurrency_ID() <= 0) {
				throw new AdempiereException("@FeeCurrency_ID@ @NotFound@");
			}
			int documentTypeId = batchConfiguration.getSalesInvoiceDocType_ID();
			if (documentTypeId <= 0) {
				throw new AdempiereException("@SalesInvoiceDocType_ID@ @NotFound@");
			}

			//	Create Invoice
			MInvoice invoice = new Query(getCtx(), MInvoice.Table_Name, invoiceWhereClause, get_TrxName())
				.setParameters(batchSchedule.getReferenceNo(), batch.get_ID(), batch.getDateDoc())
				.first();
			if (invoice == null) {
				return;
			}


			MPayment payment = getPayment(invoice, batch);
			payment.saveEx();
			if(!payment.processIt(MPayment.DOCACTION_Complete)) {
				throw new AdempiereException(payment.getProcessMsg());
			}
			payment.saveEx();

			MPPVendorTransaction vendorTransaction = new MPPVendorTransaction(getCtx(), 0, get_TrxName());
			vendorTransaction.setC_PaymentProcessorBatch_ID(batch.getC_PaymentProcessorBatch_ID());
			vendorTransaction.setDeposit_ID(payment.getC_Payment_ID());
			vendorTransaction.setPayAmt(payment.getPayAmt());
			vendorTransaction.setReferenceNo(payment.getDocumentNo());
			vendorTransaction.setProcessed(true);
			vendorTransaction.saveEx();
			addLog("@C_PaymentProcessorBatch_ID@: " + batch.getDocumentNo()+ ", @DocumentNo@: " + payment.getDocumentNo() + ", @Date@: " + DisplayType.getDateFormat(DisplayType.Date).format(payment.getDateTrx()) +
					", @PayAmount@: " + DisplayType.getNumberFormat(DisplayType.Amount).format(payment.getPayAmt()));
			batch.updateTotals();
			created.getAndIncrement();

		});

		return "@Created@ " + created.get();
	}
	private MPayment getPayment(MInvoice invoice, MPaymentProcessorBatch batch) {
		MPayment payment = new MPayment(getCtx(), 0, get_TrxName());
		payment.setDocStatus(MPayment.DOCSTATUS_Drafted);
		payment.setDocAction(MPayment.DOCACTION_Complete);
		payment.setC_BPartner_ID(invoice.getC_BPartner_ID());
		payment.setIsReceipt(true);
		payment.setC_BankAccount_ID(batch.getFinalAccount_ID());
		payment.setDateAcct(invoice.getDateInvoiced());
		payment.setTenderType(MPayment.TENDERTYPE_Account);
		payment.setPayAmt(invoice.getGrandTotal());
		payment.setC_DocType_ID(true);
		payment.setC_Invoice_ID(invoice.getC_Invoice_ID());
		payment.setC_Currency_ID(invoice.getC_Currency_ID());
		payment.set_ValueOfColumn("C_PaymentProcessorBatch_ID", getRecord_ID());
		return payment;
	}
}