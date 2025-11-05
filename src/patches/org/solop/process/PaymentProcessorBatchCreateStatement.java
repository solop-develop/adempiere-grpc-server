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
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MConversionRate;
import org.compiere.model.MPPBatchLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessorBatch;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/338">https://github.com/solop-develop/adempiere-base/issues/338</a>
 */
public class PaymentProcessorBatchCreateStatement extends PaymentProcessorBatchCreateStatementAbstract {
	@Override
	protected void prepare() {
		super.prepare();
		if(getRecord_ID() <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
	}

	@Override
	protected String doIt() throws Exception {
		AtomicInteger counter = new AtomicInteger(0);
		if(!getSelectionKeys().isEmpty()) {
			getSelectionKeys().forEach(key -> {
				MPayment payment = new MPayment(getCtx(), key, get_TrxName());
				MPPBatchLine paymentProcessorLine = new MPPBatchLine(getCtx(), 0, get_TrxName());
				MPaymentProcessorBatch batch = (MPaymentProcessorBatch) paymentProcessorLine.getC_PaymentProcessorBatch();
				paymentProcessorLine.setC_PaymentProcessorBatch_ID(getRecord_ID());
				paymentProcessorLine.setC_Payment_ID(payment.getC_Payment_ID());
				int bankStatementLineId = getSelectionAsInt(key, "BSL_C_BankStatementLine_ID");
				MBankStatementLine bankStatementLine = new MBankStatementLine(getCtx(), bankStatementLineId, get_TrxName());
				BigDecimal feeAmt = bankStatementLine.getChargeAmt();
				if (bankStatementLine.getC_Currency_ID() != batch.getC_Currency_ID()) {
					feeAmt = MConversionRate.convert(getCtx(), feeAmt, bankStatementLine.getC_Currency_ID(),
							batch.getC_Currency_ID(), bankStatementLine.getStatementLineDate(), 0, getAD_Client_ID(), batch.getAD_Org_ID());
				}

				paymentProcessorLine.setPayAmt(payment.getPayAmt());
				paymentProcessorLine.setTaxAmt(payment.getTaxAmt());
				paymentProcessorLine.setFeeAmt(feeAmt);
				paymentProcessorLine.setDiscountAmt(payment.getDiscountAmt());
				if(payment.getWithdrawal_ID() > 0) {
					paymentProcessorLine.setWithdrawal_ID(payment.getWithdrawal_ID());
				}
				if(payment.getDeposit_ID() > 0) {
					paymentProcessorLine.setDeposit_ID(payment.getDeposit_ID());
				}
				paymentProcessorLine.saveEx();
				counter.incrementAndGet();
			});
		}
		MPaymentProcessorBatch batch = new MPaymentProcessorBatch(getCtx(), getRecord_ID(), get_TrxName());
		batch.updateTotals();
		return "@Created@ " + counter.get();
	}
}