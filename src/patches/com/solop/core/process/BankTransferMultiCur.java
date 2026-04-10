/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2008 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *****************************************************************************/
package com.solop.core.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MClient;
import org.compiere.model.MCurrency;
import org.compiere.model.MPayment;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

/**
 *  Bank Transfer. Generate two Payments entry
 *  
 *  For Bank Transfer From Bank Account "A" 
 *                 
 *	@author victor.perez@e-evoltuion.com
 *	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
*		<a href="https://github.com/adempiere/adempiere/issues/850">
* 		@see FR [ 850 ] Bank transfer does not have document no to</a>
 *	
 **/
public class BankTransferMultiCur extends SvrProcess {
	private int         m_created = 0;

	MBankAccount mBankFrom = null;
	MBankAccount mBankTo = null;


	@Override
	protected void prepare() {

	}

	/**
	 *  Perform process.
	 *  @return Message (translated text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception {

		MClient client = new MClient(getCtx(), getAD_Client_ID(), null);
		MAcctSchema schema = client.getAcctSchema();

		mBankFrom = new MBankAccount(getCtx(), getParameterAsInt("From_C_BankAccount_ID"), get_TrxName());
		mBankTo = new MBankAccount(getCtx(), getParameterAsInt("To_C_BankAccount_ID"), get_TrxName());

		if(mBankFrom.getC_Currency_ID() != schema.getC_Currency_ID()
				&& mBankTo.getC_Currency_ID() != schema.getC_Currency_ID()
				&& mBankFrom.getC_Currency_ID() != mBankTo.getC_Currency_ID())
			throw new AdempiereException("@C0028.SchemaCurrency@");

		BigDecimal currencyRate = getParameterAsBigDecimal("CurrencyRate");
		if(mBankFrom.getC_Currency_ID() != mBankTo.getC_Currency_ID() && (currencyRate == null || currencyRate.compareTo(Env.ZERO) <= 0))
			throw new AdempiereException("@C0028.ExchangeRate@");

		if(mBankFrom.getC_Currency_ID() == mBankTo.getC_Currency_ID()
				&& getParameterAsInt("C_Currency_ID") != mBankFrom.getC_Currency_ID() && (currencyRate == null || currencyRate.compareTo(Env.ZERO) <= 0))
			throw new AdempiereException("@C0028.ExchangeRate@");

		generateBankTransfer(schema.getC_Currency_ID());

		return "@Created@ = " + m_created;
	}	//	doIt
	

	/**
	 * Generate BankTransferMultiCur()
	 *
	 */
	private void generateBankTransfer(int acctCurrID) {
		Timestamp statementDate = getParameterAsTimestamp("StatementDate");
		Timestamp dateAcct = getParameterAsTimestamp("DateAcct");
		String documentNoTo = getParameterAsString("DocumentNoTo");
		String tenderTypeFrom = "";
		String tenderTypeTo = "";
		boolean isReceiptFrom = false;
		boolean isReceiptTo = true;

		MCurrency curFrom = (MCurrency) mBankFrom.getC_Currency();
		MCurrency curTo = (MCurrency) mBankTo.getC_Currency();

		if(getParameterAsBoolean("C0028_IsFromConciliation")){
			if(getParameterAsBigDecimal("Amount").signum() > 0){
				isReceiptFrom = true;
				isReceiptTo = false;
			} else if(getParameterAsBigDecimal("Amount").signum() < 0){
				isReceiptFrom = false;
				isReceiptTo = true;
			}
		}

		if(documentNoTo == null
				|| documentNoTo.trim().length() == 0) {
			documentNoTo = getParameterAsString("DocumentNo");
		}
		//	Login Date
		if (statementDate == null) {
			statementDate = Env.getContextAsDate(getCtx(), "#Date");
		}
		if (statementDate == null) {
			statementDate = new Timestamp(System.currentTimeMillis());			
		}
		//	
		if (dateAcct == null) {
			dateAcct = statementDate;
		}

		if(getParameterAsString("TenderType").equalsIgnoreCase(MPayment.TENDERTYPE_DirectDebit)){

			tenderTypeFrom = MPayment.TENDERTYPE_DirectDebit;
			tenderTypeTo = MPayment.TENDERTYPE_DirectDeposit;

		}else if(getParameterAsString("TenderType").equalsIgnoreCase(MPayment.TENDERTYPE_Check)){

			tenderTypeFrom = MPayment.TENDERTYPE_Check;
			tenderTypeTo = MPayment.TENDERTYPE_DirectDeposit;

		}else if(getParameterAsString("TenderType").equalsIgnoreCase(MPayment.TENDERTYPE_DirectDeposit)){

			tenderTypeFrom = MPayment.TENDERTYPE_DirectDeposit;
			tenderTypeTo = MPayment.TENDERTYPE_DirectDeposit;

		}
		
		MPayment paymentBankFrom = new MPayment(getCtx(), 0 ,  get_TrxName());
		paymentBankFrom.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
		paymentBankFrom.setDocumentNo(getParameterAsString("DocumentNo"));
		paymentBankFrom.setDateAcct(dateAcct);
		paymentBankFrom.setDateTrx(statementDate);
		paymentBankFrom.setTenderType(tenderTypeFrom);
		paymentBankFrom.setDescription(getParameterAsString("Description"));
		paymentBankFrom.setC_BPartner_ID (getParameterAsInt("C_BPartner_ID"));

		if(getParameterAsInt("C_ConversionType_ID") > 0) {
			paymentBankFrom.setC_ConversionType_ID(getParameterAsInt("C_ConversionType_ID"));	
		}

		if(mBankFrom.getC_Currency_ID() == getParameterAsInt("C_Currency_ID")){

			paymentBankFrom.setPayAmt(getParameterAsBigDecimal("Amount").abs());
			paymentBankFrom.setC_Currency_ID(getParameterAsInt("C_Currency_ID"));

		} else {

			if(getParameterAsInt("C_Currency_ID") != acctCurrID && mBankFrom.getC_Currency_ID() == acctCurrID){
				paymentBankFrom.setPayAmt(getParameterAsBigDecimal("Amount").abs().multiply(getParameterAsBigDecimal("CurrencyRate")).setScale(2, RoundingMode.HALF_UP));
			} else if(getParameterAsInt("C_Currency_ID") == acctCurrID && mBankFrom.getC_Currency_ID() != acctCurrID){
				paymentBankFrom.setPayAmt(getParameterAsBigDecimal("Amount").abs().divide(getParameterAsBigDecimal("CurrencyRate"), 2, RoundingMode.HALF_UP));
			}

			paymentBankFrom.setC_Currency_ID(mBankFrom.getC_Currency_ID());

		}

		paymentBankFrom.setOverUnderAmt(Env.ZERO);
		paymentBankFrom.setC_DocType_ID(isReceiptFrom);
		paymentBankFrom.setC_Charge_ID(getParameterAsInt("C_Charge_ID"));
		paymentBankFrom.saveEx();
		paymentBankFrom.processIt(MPayment.DOCACTION_Complete);
		paymentBankFrom.saveEx();

		if(getParameterAsInt("C_BankStatementLine_ID") > 0){

			MBankStatementLine line = new MBankStatementLine(getCtx(), getParameterAsInt("C_BankStatementLine_ID"), get_TrxName());

			if(line != null && line.get_ID() > 0){
				line.setPayment(paymentBankFrom);
				line.saveEx();
			}
		}

		//	Add to current bank statement for account
		if(getParameterAsBoolean("IsAutoReconciled")) {
			MBankStatementLine bsl = MBankStatement.addPayment(paymentBankFrom);
			if(bsl != null) {
				addLog("@C_Payment_ID@: " + paymentBankFrom.getDocumentNo() 
						+ " @Added@ @to@ [@AccountNo@ " + paymentBankFrom.getC_BankAccount().getAccountNo() 
						+ " @C_BankStatement_ID@ " + bsl.getC_BankStatement().getName() + "]");
			}
		}
		//	
		MPayment paymentBankTo = new MPayment(getCtx(), 0 ,  get_TrxName());
		paymentBankTo.setC_BankAccount_ID(mBankTo.getC_BankAccount_ID());
		paymentBankTo.setDocumentNo(documentNoTo);
		paymentBankTo.setDateAcct(dateAcct);
		paymentBankTo.setDateTrx(statementDate);
		paymentBankTo.setTenderType(tenderTypeTo);
		paymentBankTo.setDescription(getParameterAsString("Description"));
		paymentBankTo.setC_BPartner_ID (getParameterAsInt("C_BPartner_ID"));

		if(getParameterAsInt("C_ConversionType_ID") > 0) {
			paymentBankFrom.setC_ConversionType_ID(getParameterAsInt("C_ConversionType_ID"));	
		}

		if(mBankTo.getC_Currency_ID() == getParameterAsInt("C_Currency_ID")){

			paymentBankTo.setPayAmt(getParameterAsBigDecimal("Amount").abs());
			paymentBankTo.setC_Currency_ID(getParameterAsInt("C_Currency_ID"));

		} else {

			if(getParameterAsInt("C_Currency_ID") != acctCurrID && mBankTo.getC_Currency_ID() == acctCurrID){
				paymentBankTo.setPayAmt(getParameterAsBigDecimal("Amount").abs().multiply(getParameterAsBigDecimal("CurrencyRate")).setScale(2, RoundingMode.HALF_UP));
			} else if(getParameterAsInt("C_Currency_ID") == acctCurrID && mBankTo.getC_Currency_ID() != acctCurrID){
				paymentBankTo.setPayAmt(getParameterAsBigDecimal("Amount").abs().divide(getParameterAsBigDecimal("CurrencyRate"), 2, RoundingMode.HALF_UP));
			}

			paymentBankTo.setC_Currency_ID(mBankTo.getC_Currency_ID());

		}

		paymentBankTo.setOverUnderAmt(Env.ZERO);
		paymentBankTo.setC_DocType_ID(isReceiptTo);
		paymentBankTo.setC_Charge_ID(getParameterAsInt("C_Charge_ID"));
		paymentBankTo.saveEx();
		paymentBankTo.processIt(MPayment.DOCACTION_Complete);
		paymentBankTo.saveEx();

		paymentBankFrom.setRef_Payment_ID(paymentBankTo.get_ID());//vinculo el pago con el cobro
		paymentBankTo.setRef_Payment_ID(paymentBankFrom.get_ID());//vinculo el cobro con el pago

		//genero descripcion completa y seteo en ambos payments
		String descriptionFrom = mBankFrom.getDescription();
		String descriptionTo = mBankTo.getDescription();

		if(descriptionFrom == null || descriptionFrom.equalsIgnoreCase(""))
			descriptionFrom = mBankFrom.getAccountNo();

		if(descriptionTo == null || descriptionTo.equalsIgnoreCase(""))
			descriptionTo = mBankTo.getAccountNo();

		String description = "@C0028.Origin@: " + descriptionFrom + ", @Payment@ " + paymentBankFrom.getDocumentNo() + " " +
				curFrom.getISO_Code() + " " + paymentBankFrom.getPayAmt().setScale(2, RoundingMode.HALF_UP) +
					" - @C0028.Destination@: " + descriptionTo + ", @C0028.Receipt@ " + paymentBankTo.getDocumentNo() + " " + curTo.getISO_Code() + " " +
				paymentBankTo.getPayAmt().setScale(2, RoundingMode.HALF_UP);

		if(paymentBankFrom.getDescription() != null && !paymentBankFrom.getDescription().equalsIgnoreCase("")){
			paymentBankFrom.setDescription(paymentBankFrom.getDescription() + " " + description);
		} else paymentBankFrom.setDescription(description);

		paymentBankFrom.saveEx();

		if(paymentBankTo.getDescription() != null && !paymentBankTo.getDescription().equalsIgnoreCase("")){
			paymentBankTo.setDescription(paymentBankTo.getDescription() + " " + description);
		} else paymentBankTo.setDescription(description);

		paymentBankTo.saveEx();

		//	Add to current bank statement for account
		if(getParameterAsBoolean("IsAutoReconciled")) {
			MBankStatementLine bsl = MBankStatement.addPayment(paymentBankTo);
			if(bsl != null) {
				addLog("@C_Payment_ID@: " + paymentBankTo.getDocumentNo() 
						+ " @Added@ @to@ [@AccountNo@ " + paymentBankTo.getC_BankAccount().getAccountNo() 
						+ " @C_BankStatement_ID@ " + bsl.getC_BankStatement().getName() + "]");
			}
		}
		m_created++;
		return;

	}  //  createCashLines
	
}	//	ImmediateBankTransfer
