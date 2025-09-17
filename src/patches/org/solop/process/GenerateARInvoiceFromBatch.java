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
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessorBatch;
import org.compiere.model.MPaymentProcessorSchedule;
import org.compiere.model.MPriceList;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
		String whereClause = "C_PaymentProcessorBatch_ID = ?";
		List<Integer> batchScheduleIds = new Query(getCtx(), MPaymentProcessorSchedule.Table_Name, whereClause, get_TrxName())
				.setParameters(getRecord_ID())
				.getIDsAsList();

		if (batchScheduleIds == null) {
			throw new AdempiereException("@C_PaymentProcessorSchedule_ID@ @NotFound@ @Date@: " +  getDateDoc());
		}
		if(!batch.isProcessed() || (!batch.getDocStatus().equals(MPaymentProcessorBatch.DOCSTATUS_Completed) && !batch.getDocStatus().equals(MPaymentProcessorBatch.DOCSTATUS_Closed))) {
			throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @Unprocessed@");
		}
		if(batch.getC_PPBatchConfiguration_ID() <= 0) {
			throw new AdempiereException("@C_PPBatchConfiguration_ID@ @NotFound@");
		}

		X_C_PPBatchConfiguration batchConfiguration = new X_C_PPBatchConfiguration(getCtx(), batch.getC_PPBatchConfiguration_ID(), get_TrxName());
		if(batch.getC_Currency_ID() <= 0) {
			throw new AdempiereException("@C_Currency_ID@ @NotFound@");
		}
		int documentTypeId = batchConfiguration.getSalesInvoiceDocType_ID();
		if (documentTypeId <= 0) {
			throw new AdempiereException("@SalesInvoiceDocType_ID@ @NotFound@");
		}
		whereClause = "IsSOTrx = 'Y' AND C_PaymentProcessorBatch_ID = ? AND DocStatus NOT IN ('RE','VO')";
		boolean exists = new Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
				.setParameters(getRecord_ID())
				.match();
		if (exists) {
			throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @Invalid@");
		}
		AtomicReference<BigDecimal> maybeWithdrawalAmount = new AtomicReference<>(BigDecimal.ZERO);
		AtomicInteger created = new AtomicInteger(0);
		batchScheduleIds.forEach(scheduleId -> {
			MPaymentProcessorSchedule batchSchedule = new MPaymentProcessorSchedule(getCtx(), scheduleId, get_TrxName());
			MBPartner businessPartner = MBPartner.get(getCtx(), batch.getC_BPartner_ID());
			//	Create Invoice
			MInvoice invoice = new MInvoice (getCtx(), 0, get_TrxName());
			invoice.setClientOrg(batch.getAD_Client_ID(), batch.getAD_Org_ID());
			invoice.setC_DocTypeTarget_ID(documentTypeId);
			invoice.setIsSOTrx(true);
			invoice.setBPartner(businessPartner);
			invoice.setSalesRep_ID(getAD_User_ID());	//	caller
			invoice.setDateInvoiced(batch.getDateDoc());
			String currencyIsoCode = MCurrency.get(getCtx(), batch.getC_Currency_ID()).getISO_Code();
			MPriceList priceList = (MPriceList) businessPartner.getM_PriceList();
			if (priceList == null) {
				priceList = MPriceList.getDefault(getCtx(), true, currencyIsoCode);
			}

			if(priceList == null || priceList.get_ID() <= 0) {
				throw new IllegalArgumentException("@M_PriceList_ID@ @NotFound@ (@C_Currency_ID@ " + currencyIsoCode + ")");
			}
			invoice.setM_PriceList_ID(priceList.getM_PriceList_ID());
			invoice.setC_PaymentProcessorBatch_ID(batch.getC_PaymentProcessorBatch_ID());
			invoice.setDocumentNo(batchSchedule.getReferenceNo());
			invoice.setDocStatus(MInvoice.DOCSTATUS_Drafted);
			invoice.setDocAction(getDocAction());
			//
			invoice.saveEx();

			//	Create Invoice Line
			MInvoiceLine invoiceLine = new MInvoiceLine(invoice);
			invoiceLine.setC_Charge_ID(batchConfiguration.getFeeCharge_ID());
			invoiceLine.setQty(1);
			invoiceLine.setPrice(batchSchedule.getAmount());
			invoiceLine.setTax();
			invoiceLine.saveEx();
			if(!invoice.processIt(getDocAction())) {
				throw new AdempiereException(invoice.getProcessMsg());
			}
			invoice.saveEx();

			//Invoice Pay Schedule
			MInvoicePaySchedule invoiceSchedule = new MInvoicePaySchedule(getCtx(), 0, get_TrxName());
			invoiceSchedule.setC_Invoice_ID(invoice.get_ID());
			invoiceSchedule.setDueDate(batchSchedule.getDateDoc());
			invoiceSchedule.setDueAmt(invoiceLine.getPriceEntered());
			invoiceSchedule.setAD_Org_ID(invoice.getAD_Org_ID());
			invoiceSchedule.setDiscountDate(invoiceSchedule.getDueDate());
			invoiceSchedule.saveEx();

			created.getAndIncrement();
			if (batch.isAutomaticReceipt()) {
				maybeWithdrawalAmount.getAndUpdate(current -> current.add(invoice.getGrandTotal()));
			}
			addLog("@DocumentNo@: " + invoice.getDocumentNo()+", @DueDate@: " + DisplayType.getDateFormat(DisplayType.Date).format(batchSchedule.getDateDoc()) +
					", @GrandTotal@: " + DisplayType.getNumberFormat(DisplayType.Amount).format(invoice.getGrandTotal()));
		});

		//For Withdrawal
		BigDecimal withdrawalAmount = maybeWithdrawalAmount.get();
		if (withdrawalAmount.signum() != 0) {
			MPayment withdrawal = getWithdrawal(batchConfiguration, withdrawalAmount, batch);
			withdrawal.saveEx();

			if(!withdrawal.processIt(MPayment.DOCACTION_Complete)) {
				throw new AdempiereException(withdrawal.getProcessMsg());
			}
			withdrawal.saveEx();
		}

		return "@Created@: " + created.get();
	}

	private MPayment getWithdrawal(X_C_PPBatchConfiguration batchConfiguration, BigDecimal amount, MPaymentProcessorBatch batch) {
		MPayment withdrawal = new MPayment(getCtx(), 0, get_TrxName());
		withdrawal.setDocStatus(MPayment.DOCSTATUS_Drafted);
		withdrawal.setDocAction(MPayment.DOCACTION_Complete);

		withdrawal.setC_Charge_ID(batchConfiguration.getFeeCharge_ID());
		withdrawal.setPayAmt(amount);
		withdrawal.setC_BPartner_ID(batchConfiguration.getC_BPartner_ID());

		withdrawal.setC_DocType_ID(false);
		withdrawal.setTenderType(MPayment.TENDERTYPE_Account);
		withdrawal.setC_BankAccount_ID(batch.getTransitBankAccount_ID());
		withdrawal.setIsReceipt(false);
		withdrawal.setC_Currency_ID(batch.getC_Currency_ID());
		withdrawal.set_ValueOfColumn("C_PaymentProcessorBatch_ID", getRecord_ID());
		return withdrawal;
	}


}