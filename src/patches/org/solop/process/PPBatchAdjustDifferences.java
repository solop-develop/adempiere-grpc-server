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
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MPPBatchLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentProcessorBatch;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Process for (Adjust Differences)
 *  @author Gabriel Escalona
 *  @version Release 3.9.4
 */
public class PPBatchAdjustDifferences extends PPBatchAdjustDifferencesAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}
	private Timestamp documentDate;
	@Override
	protected String doIt() throws Exception
	{
		if (getRecord_ID() <= 0) {
			throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @NotFound@");
		}
		if (getCorrectAmount().signum() == 0) {
			throw new AdempiereException("@CorrectAmount@: 0");
		}
		if (getPaymentId() <= 0) {
			throw new AdempiereException("@C_Payment_ID@ @NotFound@");
		}
		if (getChargeId() <= 0) {
			throw new AdempiereException("@C_Charge_ID@ @NotFound@");
		}
		MPaymentProcessorBatch batch = new MPaymentProcessorBatch(getCtx(), getRecord_ID(), get_TrxName());
		if (batch.isProcessed()) {
			throw new AdempiereException("@C_PaymentProcessorBatch_ID@ @DocProcessed@");
		}

		MPayment originalPayment = new MPayment(getCtx(), getPaymentId(), get_TrxName());

		if (!originalPayment.getDocStatus().equals(MPayment.DOCSTATUS_Completed)
				&& !originalPayment.getDocStatus().equals(MPayment.DOCSTATUS_Closed)) {
			throw new AdempiereException("@C_Payment_ID@ @NoCompleted@");
		}

		BigDecimal difference = originalPayment.getPayAmt().subtract(getCorrectAmount());
		if (!originalPayment.isReceipt()){
			difference = difference.negate();
		}
		if (difference.signum() == 0) {
			throw new AdempiereException ("@CorrectAmount@ = @PayAmt@");
		}
		documentDate = originalPayment.getDateTrx();
		if (!isKeepOriginalDate()) {
			if (getDateDoc() == null) {
				throw new AdempiereException("@DateDoc@ @NotFound@");
			}
			documentDate = getDateDoc();
		}
		// If difference > 0 create a Payment, else create a Receipt
		boolean isReceipt = difference.signum() < 0;
		BigDecimal adjustmentAmount = difference.abs();

		MPayment adjustmentPayment = new MPayment(getCtx(), 0, get_TrxName());
		PO.copyValues(originalPayment, adjustmentPayment);
		adjustmentPayment.setDateAcct(documentDate);
		adjustmentPayment.setDateTrx(documentDate);
		adjustmentPayment.setIsReceipt(isReceipt);
		adjustmentPayment.setPayAmt(adjustmentAmount);
		adjustmentPayment.setC_Charge_ID(getChargeId());
		adjustmentPayment.setAD_Org_ID(originalPayment.getAD_Org_ID());
		adjustmentPayment.setC_Invoice_ID(-1);
		adjustmentPayment.setC_Order_ID(-1);
		adjustmentPayment.setWithdrawal_ID(-1);
		adjustmentPayment.setDeposit_ID(-1);

		adjustmentPayment.setC_DocType_ID(isReceipt);
		adjustmentPayment.setDocStatus(MPayment.DOCSTATUS_Drafted);

		String description = Msg.parseTranslation(getCtx(), "@Difference@: @C_Payment_ID@") + ": " + originalPayment.getDocumentNo();
		if (originalPayment.getDescription() != null && !originalPayment.getDescription().isEmpty()) {
			description += " - " + originalPayment.getDescription();
		}
		adjustmentPayment.setDescription(description);

		adjustmentPayment.saveEx();
		if (!adjustmentPayment.processIt(MPayment.DOCACTION_Complete)) {
			throw new AdempiereException("@ProcessFailed@: " + adjustmentPayment.getProcessMsg());
		}
		adjustmentPayment.saveEx();

		// Create BankStatement and line for the adjustment payment
		MBankStatement bankStatement = createBankStatementForAdjustment(
			originalPayment, adjustmentPayment, adjustmentAmount, description);

		// Assign the payment to the PaymentProcessorBatch
		assignPaymentToBatch(adjustmentPayment, adjustmentAmount, isReceipt, batch.getAD_Org_ID());


		return "@C_Payment_ID@: " + adjustmentPayment.getDocumentNo() +
			   " - @C_BankStatement_ID@: " + bankStatement.getDocumentNo() +
			   " - @Amount@: " + adjustmentAmount;
	}

	/**
	 * Create a BankStatement and line for the adjustment payment
	 * @param originalPayment Original payment
	 * @param adjustmentPayment Adjustment payment
	 * @param adjustmentAmount Adjustment amount
	 * @param description Description
	 * @return Created and completed BankStatement
	 */
	private MBankStatement createBankStatementForAdjustment(
		MPayment originalPayment,
		MPayment adjustmentPayment,
		BigDecimal adjustmentAmount,
		String description)
	{

		MBankStatement bankStatement = new MBankStatement(getCtx(), 0, get_TrxName());
		bankStatement.setAD_Org_ID(originalPayment.getAD_Org_ID());
		bankStatement.setC_BankAccount_ID(originalPayment.getC_BankAccount_ID());
		bankStatement.setStatementDate(documentDate);
		bankStatement.setName(Msg.parseTranslation(getCtx(), "@Difference@: @C_Payment_ID@") + ": " +
			adjustmentPayment.getDocumentNo());
		bankStatement.setDescription(description);
		//bankStatement.setBeginningBalance(Env.ZERO);
		//bankStatement.setEndingBalance(adjustmentAmount);
		//bankStatement.setStatementDifference(adjustmentAmount);
		bankStatement.setIsManual(true);
		bankStatement.saveEx();

		MBankStatementLine bankStatementLine = new MBankStatementLine(bankStatement);
		bankStatementLine.setPayment(adjustmentPayment);
		bankStatementLine.setStatementLineDate(documentDate);
		bankStatementLine.setDateAcct(documentDate);
		bankStatementLine.saveEx();

		if (!bankStatement.processIt(DocAction.ACTION_Complete)) {
			throw new AdempiereException("@ProcessFailed@ @C_BankStatement_ID@: " +
				bankStatement.getProcessMsg());
		}
		bankStatement.saveEx();

		return bankStatement;
	}

	/**
	 * Assign the adjustment payment to the PaymentProcessorBatch
	 * @param adjustmentPayment Adjustment payment
	 * @param adjustmentAmount Adjustment amount
	 * @param isReceipt Whether it is a receipt or payment
	 */
	private void assignPaymentToBatch(
		MPayment adjustmentPayment,
		BigDecimal adjustmentAmount,
		boolean isReceipt, int orgId)
	{
		MPPBatchLine ppBatchLine = new MPPBatchLine(getCtx(), 0, get_TrxName());
		ppBatchLine.setC_PaymentProcessorBatch_ID(getRecord_ID());
		ppBatchLine.setC_Payment_ID(adjustmentPayment.getC_Payment_ID());
		ppBatchLine.setAD_Org_ID(orgId);
		// Determine multiplier: negative for payments (not receipts)
		BigDecimal multiplier = isReceipt ? BigDecimal.ONE : BigDecimal.ONE.negate();

		ppBatchLine.setPayAmt(adjustmentAmount.multiply(multiplier));
		ppBatchLine.setTaxAmt(adjustmentPayment.getTaxAmt().multiply(multiplier));
		ppBatchLine.setFeeAmt(Env.ZERO); // No fee for adjustments
		ppBatchLine.setDiscountAmt(adjustmentPayment.getDiscountAmt().multiply(multiplier));

		ppBatchLine.saveEx();

		// Update batch totals
		MPaymentProcessorBatch batch = new MPaymentProcessorBatch(getCtx(), getRecord_ID(), get_TrxName());
		batch.updateTotals();
		batch.saveEx();
	}
}