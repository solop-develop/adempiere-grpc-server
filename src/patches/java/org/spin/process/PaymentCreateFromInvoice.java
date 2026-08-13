/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.spin.process;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.MUser;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Payment Create From Invoice, used for Smart Browse (Create Payment From Invoice)
 *	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<li> FR [ 1616 ] Create collect from Invoice list
 *		@see https://github.com/adempiere/adempiere/issues/1616
 */
public class PaymentCreateFromInvoice extends PaymentCreateFromInvoiceAbstract {

	/**	Is New				*/
	private boolean			isNew = false;
	/**	Payment Selection	*/
	private MPayment		payment = null;
	/**	Remaining			*/
	private BigDecimal		remaining = Env.ZERO;
	/**	Total Amount		*/
	private BigDecimal		totalPayAmt = Env.ZERO;

	@Override
	protected void prepare() {
		super.prepare();
		//	Valid Record Identifier
		if(getRecord_ID() <= 0
				&& getBankAccountId() == 0
				&& getPayDate() == null)
			throw new AdempiereException("@C_Payment_ID@ @NotFound@");
	}

	@Override
	protected String doIt() throws Exception {
		//	Invoked from the Payment window over an existing record: keep the legacy
		//	single-payment behavior (the selection is expected to belong to that
		//	payment's own Business Partner already).
		if(getRecord_ID() > 0) {
			return doItForExistingPayment();
		}
		//	Invoked from the Smart Browse (mass selection): a selection can include
		//	invoices from several Business Partners, so a Payment is created per
		//	Business Partner / Currency instead of a single one for everybody.
		//	See issue #3489: a single payment was generated for many customers,
		//	assigning invoices from unrelated Business Partners to it.
		return doItGroupedByBusinessPartner();
	}

	/**
	 * Legacy path: add the selected invoices to the Payment identified by
	 * getRecord_ID() (or create a single new one when there is none), exactly
	 * as this process behaved before issue #3489.
	 */
	private String doItForExistingPayment() throws Exception {
		//	Sum all
		for(int invoiceId : getSelectionKeys()) {
			BigDecimal payAmt = getSelectionAsBigDecimal(invoiceId, "INV_PayAmt");
			BigDecimal discountAmt = getSelectionAsBigDecimal(invoiceId, "INV_DiscountAmt");
			//	Validate discount
			if(discountAmt == null) {
				discountAmt = Env.ZERO;
			}
			totalPayAmt = totalPayAmt.add(payAmt.subtract(discountAmt));
		}
		//	Verify remaining
		if(!isOverUnderPayment()
				&& getPayAmt() != null
				&& getPayAmt().compareTo(Env.ZERO) > 0) {
			//	Validate
			if(totalPayAmt.compareTo(getPayAmt()) != 0) {
				throw new AdempiereException("@PaymentAllocateSumInconsistent@");
			}
		}
		//	Set Total Payment
		if(getPayAmt() == null
				|| getPayAmt().compareTo(Env.ZERO) <= 0) {
			setPayAmt(totalPayAmt);
		}
		//	Loop for keys
		for(int invoiceId : getSelectionKeys()) {
			if(payment == null) {
				MInvoice invoice = new MInvoice(getCtx(), invoiceId, get_TrxName());
				//	Create and fill payment
				createPayment(invoice.getC_BPartner_ID(), invoice.getC_Currency_ID());
			}
			//	get values from result set
			BigDecimal openAmt = getSelectionAsBigDecimal(invoiceId, "INV_OpenAmt");
			BigDecimal payAmt = getSelectionAsBigDecimal(invoiceId, "INV_PayAmt");
			BigDecimal discountAmt = getSelectionAsBigDecimal(invoiceId, "INV_DiscountAmt");
			//	Validate discount
			if(discountAmt == null) {
				discountAmt = Env.ZERO;
			}
			//	Add invoice to invoice pay
			MPaymentAllocate invoicePayAllocate = new MPaymentAllocate(getCtx(), 0, get_TrxName());
			//
			invoicePayAllocate.setC_Payment_ID(payment.getC_Payment_ID());
			invoicePayAllocate.setC_Invoice_ID(invoiceId);
			//	For Pay amount
			payAmt = payAmt.subtract(discountAmt);
			BigDecimal overUnderAmt = openAmt.subtract(payAmt);
			//	Set Remaining
			if(remaining.compareTo(Env.ZERO) > 0) {
				remaining = remaining.subtract(payAmt);
				if(remaining.compareTo(Env.ZERO) < 0) {
					overUnderAmt = payAmt;
					payAmt = payAmt.add(remaining);
					overUnderAmt = overUnderAmt.subtract(payAmt);
				}
			}
			invoicePayAllocate.setInvoiceAmt(openAmt);
			invoicePayAllocate.setAmount(payAmt);
			invoicePayAllocate.setDiscountAmt(discountAmt);
			invoicePayAllocate.setOverUnderAmt(overUnderAmt);
			//	Save
			invoicePayAllocate.saveEx();
			//	Last invoice
			if(overUnderAmt.compareTo(Env.ZERO) > 0) {
				break;
			}
		}
		//	For new
		if(isNew) {
			//	Load Record
			payment.load(get_TrxName());
			//	Process Selection
			if(!payment.processIt(MPaySelection.DOCACTION_Complete)) {
				throw new AdempiereException("@Error@ " + payment.getProcessMsg());
			}
			//
			payment.saveEx();
			//	Notify
			return payment.getDescription();
		}
		//	Add log
		addLog(payment.getC_Payment_ID(), payment.getDateTrx(), null, payment.getDocumentNo());
		//	Default Ok
		return "@Created@ @C_Payment_ID@ " + payment.getDocumentInfo();
	}

	/**
	 * Mass-selection path: group the selected invoices by Business Partner and
	 * Currency, and create one new Payment per group instead of a single one
	 * for the whole selection. Fixes issue #3489.
	 */
	private String doItGroupedByBusinessPartner() throws Exception {
		//	Sum by Business Partner / Currency group, loading each invoice once
		Map<Integer, MInvoice> invoicesById = new LinkedHashMap<>();
		Map<List<Integer>, BigDecimal> totalPayAmtByGroup = new LinkedHashMap<>();
		for(int invoiceId : getSelectionKeys()) {
			MInvoice invoice = new MInvoice(getCtx(), invoiceId, get_TrxName());
			invoicesById.put(invoiceId, invoice);
			List<Integer> groupKey = Arrays.asList(invoice.getC_BPartner_ID(), invoice.getC_Currency_ID());
			BigDecimal payAmt = getSelectionAsBigDecimal(invoiceId, "INV_PayAmt");
			BigDecimal discountAmt = getSelectionAsBigDecimal(invoiceId, "INV_DiscountAmt");
			//	Validate discount
			if(discountAmt == null) {
				discountAmt = Env.ZERO;
			}
			totalPayAmtByGroup.merge(groupKey, payAmt.subtract(discountAmt), BigDecimal::add);
		}
		//	The manual "Pay Amount" override only makes sense when the whole
		//	selection belongs to a single Business Partner / Currency; with
		//	several groups each Payment is set to the sum of its own invoices.
		boolean singleGroup = totalPayAmtByGroup.size() == 1;
		BigDecimal totalPayAmt = Env.ZERO;
		for(BigDecimal groupTotal : totalPayAmtByGroup.values()) {
			totalPayAmt = totalPayAmt.add(groupTotal);
		}
		if(singleGroup
				&& !isOverUnderPayment()
				&& getPayAmt() != null
				&& getPayAmt().compareTo(Env.ZERO) > 0
				&& totalPayAmt.compareTo(getPayAmt()) != 0) {
			throw new AdempiereException("@PaymentAllocateSumInconsistent@");
		}
		//	Create one Payment per Business Partner / Currency group
		Map<List<Integer>, PaymentGroup> paymentGroups = new LinkedHashMap<>();
		for(Map.Entry<List<Integer>, BigDecimal> groupEntry : totalPayAmtByGroup.entrySet()) {
			int businessPartnerId = groupEntry.getKey().get(0);
			int currencyId = groupEntry.getKey().get(1);
			BigDecimal groupPayAmt = singleGroup && getPayAmt() != null && getPayAmt().compareTo(Env.ZERO) > 0
					? getPayAmt()
					: groupEntry.getValue();
			PaymentGroup group = new PaymentGroup();
			group.businessPartnerId = businessPartnerId;
			group.payment = new MPayment(getCtx(), 0, get_TrxName());
			fillPaymentAttributes(group.payment, businessPartnerId, currencyId, groupPayAmt);
			group.remaining = groupPayAmt;
			paymentGroups.put(groupEntry.getKey(), group);
		}
		//	Allocate each invoice to the Payment of its own Business Partner / Currency
		for(int invoiceId : getSelectionKeys()) {
			MInvoice invoice = invoicesById.get(invoiceId);
			List<Integer> groupKey = Arrays.asList(invoice.getC_BPartner_ID(), invoice.getC_Currency_ID());
			PaymentGroup group = paymentGroups.get(groupKey);
			//	Group already reached its target amount: leave remaining invoices unallocated
			if(group.closed) {
				continue;
			}
			BigDecimal openAmt = getSelectionAsBigDecimal(invoiceId, "INV_OpenAmt");
			BigDecimal payAmt = getSelectionAsBigDecimal(invoiceId, "INV_PayAmt");
			BigDecimal discountAmt = getSelectionAsBigDecimal(invoiceId, "INV_DiscountAmt");
			//	Validate discount
			if(discountAmt == null) {
				discountAmt = Env.ZERO;
			}
			//	Add invoice to invoice pay
			MPaymentAllocate invoicePayAllocate = new MPaymentAllocate(getCtx(), 0, get_TrxName());
			invoicePayAllocate.setC_Payment_ID(group.payment.getC_Payment_ID());
			invoicePayAllocate.setC_Invoice_ID(invoiceId);
			//	For Pay amount
			payAmt = payAmt.subtract(discountAmt);
			BigDecimal overUnderAmt = openAmt.subtract(payAmt);
			//	Set Remaining
			if(group.remaining.compareTo(Env.ZERO) > 0) {
				group.remaining = group.remaining.subtract(payAmt);
				if(group.remaining.compareTo(Env.ZERO) < 0) {
					overUnderAmt = payAmt;
					payAmt = payAmt.add(group.remaining);
					overUnderAmt = overUnderAmt.subtract(payAmt);
				}
			}
			invoicePayAllocate.setInvoiceAmt(openAmt);
			invoicePayAllocate.setAmount(payAmt);
			invoicePayAllocate.setDiscountAmt(discountAmt);
			invoicePayAllocate.setOverUnderAmt(overUnderAmt);
			//	Save
			invoicePayAllocate.saveEx();
			//	Group reached its target amount
			if(overUnderAmt.compareTo(Env.ZERO) > 0) {
				group.closed = true;
			}
		}
		//	Complete every Payment created and build the result message
		StringBuilder resultMsg = new StringBuilder();
		for(PaymentGroup group : paymentGroups.values()) {
			group.payment.load(get_TrxName());
			if(!group.payment.processIt(MPaySelection.DOCACTION_Complete)) {
				throw new AdempiereException("@Error@ " + group.payment.getProcessMsg()
						+ " (C_BPartner_ID=" + group.businessPartnerId + ")");
			}
			group.payment.saveEx();
			if(resultMsg.length() > 0) {
				resultMsg.append(", ");
			}
			resultMsg.append(group.payment.getDocumentInfo());
		}
		return "@Created@ @C_Payment_ID@ " + resultMsg;
	}

	/**	Payment being built for a single Business Partner / Currency group	*/
	private static class PaymentGroup {
		private int businessPartnerId;
		private MPayment payment;
		private BigDecimal remaining = Env.ZERO;
		private boolean closed = false;
	}

	/**
	 * Create and fill Payment from parameters
	 * @param businessPartnerId
	 * @param currencyId
	 */
	private void createPayment(int businessPartnerId, int currencyId) {
		if(getRecord_ID() > 0) {	//	Already exists
			payment = new MPayment(getCtx(), getRecord_ID(), get_TrxName());
			remaining = payment.getPayAmt();
			return;
		} else {
			payment = new MPayment(getCtx(), 0, get_TrxName());
			//
			isNew = true;
		}
		//
		remaining = getPayAmt();
		fillPaymentAttributes(payment, businessPartnerId, currencyId, getPayAmt());
	}

	/**
	 * Fill the attributes of a new Payment from the process parameters. Shared by
	 * the legacy single-payment path and by the Payments created per Business
	 * Partner / Currency group when a mass selection spans more than one customer.
	 * @param target Payment to fill (already instantiated as new)
	 * @param businessPartnerId
	 * @param currencyId
	 * @param payAmt
	 */
	private void fillPaymentAttributes(MPayment target, int businessPartnerId, int currencyId, BigDecimal payAmt) {
		target.setC_BPartner_ID(businessPartnerId);
		target.setC_BankAccount_ID(getBankAccountId());
		target.setDateTrx(getPayDate());
		target.setDateAcct(getDateDoc());
		if(getDocTypeTargetId() > 0) {
			target.setC_DocType_ID(getDocTypeTargetId());
		}
		//	Add data for payment amount
		target.setPayAmt(payAmt);
		MUser user = MUser.get(getCtx(), getAD_User_ID());
		String userName = "";
		if(user != null)
			userName = user.getName();
		//	Set description
		target.setDescription(Msg.parseTranslation(Env.getCtx(), "@Created@ @from@")
				+ " - " + userName
				+ " - " + DisplayType.getDateFormat(DisplayType.Date).format(getPayDate()));
		//	Tender Type
		target.setTenderType(getTenderType());
		//	Currency
		target.setC_Currency_ID(currencyId);
		//
		if(!Util.isEmpty(getAccountNo())) {
			target.setAccountNo(getAccountNo());
		}
		//	Routing No
		if(!Util.isEmpty(getRoutingNo())) {
			target.setRoutingNo(getRoutingNo());
		}
		//	Check No
		if(!Util.isEmpty(getCheckNo())) {
			target.setCheckNo(getCheckNo());
		}
		//	Micr
		if(!Util.isEmpty(getMicr())) {
			target.setMicr(getMicr());
		}
		//	Credit Card Type
		if(!Util.isEmpty(getCreditCardType())) {
			target.setCreditCardType(getCreditCardType());
		}
		//	Trx Type
		if(!Util.isEmpty(getTrxType())) {
			target.setTrxType(getTrxType());
		}
		//	Credit Card Number
		if(!Util.isEmpty(getCreditCardNumber())) {
			target.setCreditCardNumber(getCreditCardNumber());
		}
		//	Credit Card VV
		if(!Util.isEmpty(getCreditCardVV())) {
			target.setCreditCardVV(getCreditCardVV());
		}
		//	Credit Card Exp MM
		if(getCreditCardExpMM() != 0) {
			target.setCreditCardExpMM(getCreditCardExpMM());
		}
		//	Credit Card Exp YY
		if(getCreditCardExpYY() != 0) {
			target.setCreditCardExpYY(getCreditCardExpYY());
		}
		//	Name
		if(!Util.isEmpty(getName())) {
			target.setA_Name(getName());
		}
		//	Street
		if(!Util.isEmpty(getStreet())) {
			target.setA_Street(getStreet());
		}
		//	City
		if(!Util.isEmpty(getCity())) {
			target.setA_City(getCity());
		}
		//	Zip
		if(!Util.isEmpty(getZip())) {
			target.setA_Zip(getZip());
		}
		//	State
		if(!Util.isEmpty(getState())) {
			target.setA_State(getState());
		}
		//	Country
		if(!Util.isEmpty(getCountry())) {
			target.setA_Country(getCountry());
		}
		//	IdentDL
		if(!Util.isEmpty(getIdentDL())) {
			target.setA_Ident_DL(getIdentDL());
		}
		//	IdentSSN
		if(!Util.isEmpty(getIdentSSN())) {
			target.setA_Ident_SSN(getIdentSSN());
		}
		//	EMail
		if(!Util.isEmpty(getEMail())) {
			target.setA_EMail(getEMail());
		}
		//	Tax Amt
		if(getTaxAmt() != null) {
			target.setTaxAmt(getTaxAmt());
		}
		//	PO Num
		if(!Util.isEmpty(getPONum())) {
			target.setPONum(getPONum());
		}
		//	Voice Auth Code
		if(!Util.isEmpty(getVoiceAuthCode())) {
			target.setVoiceAuthCode(getVoiceAuthCode());
		}
		//	trx ID
		if(!Util.isEmpty(getTrxID())) {
			target.setOrig_TrxID(getTrxID());
		}
		//	Save
		target.saveEx();
	}
}
