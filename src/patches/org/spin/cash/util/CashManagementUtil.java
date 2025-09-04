/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.cash.util;

import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MBankStatementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.cash.model.MCBankAccountWithdrawal;
import org.spin.cash.util.PaymentSummaryWrapper;
import org.spin.cash.util.PaymentWrapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Added for handle custom values for ADempiere core
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class CashManagementUtil {

	/**	Logger							*/
	protected static CLogger log = CLogger.getCLogger (CashManagementUtil.class);

	/**
	 * Create withdrawal automatically after close cash
	 * @param bankStatement
	 */
	public static void createWithdrawalFromBankStatement(MBankStatement bankStatement) {
		MBankAccount cashAccount = MBankAccount.get(bankStatement.getCtx(), bankStatement.getC_BankAccount_ID());
		if(!cashAccount.isAutoDepositAfterClose()) {
			return;
		}
		Map<Integer, List<PaymentWrapper>> paymentsByMatchedCombination = new HashMap<Integer, List<PaymentWrapper>>();
		getCollectsToDeposit(bankStatement)
				.forEach(paymentWrapper -> {
					AtomicInteger matchedCombinationId = new AtomicInteger(0);
					Optional.ofNullable(MCBankAccountWithdrawal.findMatchFromPayment(bankStatement.getCtx(), paymentWrapper, bankStatement.get_TrxName()))
							.ifPresent(matchedCombination -> matchedCombinationId.set(matchedCombination.getC_BankAccountWithdrawal_ID()));
					List<PaymentWrapper> payments = paymentsByMatchedCombination.get(matchedCombinationId.get());
					if(payments == null) {
						payments = new ArrayList<PaymentWrapper>();
					}
					payments.add(paymentWrapper);
					paymentsByMatchedCombination.put(matchedCombinationId.get(), payments);
				});
		//	Create deposits
		if(!paymentsByMatchedCombination.isEmpty()) {
			paymentsByMatchedCombination.keySet().forEach(combinationId -> {
				//	Get default values
				AtomicInteger depositBankAccountId = new AtomicInteger(cashAccount.getDepositBankAccount_ID());
				AtomicBoolean reconcilePayments = new AtomicBoolean(true);
				AtomicBoolean splitDeposits = new AtomicBoolean(false);
				AtomicReference<String> defaultTenderType = new AtomicReference<String>(cashAccount.getDepositTenderType());
				if(combinationId > 0) {
					MCBankAccountWithdrawal withdrawalConfiguration = new MCBankAccountWithdrawal(bankStatement.getCtx(), combinationId, bankStatement.get_TrxName());
					if(withdrawalConfiguration.getDepositBankAccount_ID() > 0) {
						depositBankAccountId.set(withdrawalConfiguration.getDepositBankAccount_ID());
					}
					reconcilePayments.set(withdrawalConfiguration.isAutoReconciled());
					splitDeposits.set(withdrawalConfiguration.isSplitDeposits());
					if(!Util.isEmpty(withdrawalConfiguration.getTenderType())) {
						defaultTenderType.set(withdrawalConfiguration.getTenderType());
					}
				}
				//	Split all deposits
				if(splitDeposits.get()) {
					paymentsByMatchedCombination.get(combinationId).forEach(paymentWrapper -> {
						createWithdrawal(cashAccount, bankStatement, depositBankAccountId.get(), paymentWrapper.getCurrencyId(), paymentWrapper.getConversionTypeId(), paymentWrapper.getAmount(), reconcilePayments.get(), paymentWrapper.getDocumentNo(), paymentWrapper.getTenderType(), paymentWrapper.getPaymentMethodId(), paymentWrapper.getBusinessPartnerId(), List.of(paymentWrapper.getPaymentId()));
					});
				} else {
					Map<String, PaymentSummaryWrapper> paymentToWithdrawal = new HashMap<String, PaymentSummaryWrapper>();
					paymentsByMatchedCombination.get(combinationId).forEach(paymentWrapper -> {
						PaymentSummaryWrapper summary = paymentToWithdrawal.get(paymentWrapper.getCurrencyId() + "|" + paymentWrapper.getConversionTypeId());
						if(summary == null) {
							summary = PaymentSummaryWrapper.newInstance().withCurrencyId(paymentWrapper.getCurrencyId()).withConversionTypeId(paymentWrapper.getConversionTypeId());
						}
						summary.withAmount(paymentWrapper.getAmount()).withPaymentId(paymentWrapper.getPaymentId());
						paymentToWithdrawal.put(paymentWrapper.getCurrencyId() + "|" + paymentWrapper.getConversionTypeId(), summary);
					});
					if(!paymentToWithdrawal.isEmpty()) {
						paymentToWithdrawal.values().forEach(summaryWrapper -> {
							createWithdrawal(cashAccount, bankStatement, depositBankAccountId.get(), summaryWrapper.getCurrencyId(), summaryWrapper.getConversionTypeId(), summaryWrapper.getAmount(), reconcilePayments.get(), bankStatement.getDocumentNo(), defaultTenderType.get(), 0, cashAccount.getC_BPartner_ID(), summaryWrapper.getPaymentIds());
						});
					}
				}
			});
		}
		//	Calculate balance
		calculateBankStatementBalance(bankStatement);
	}

	private static void createWithdrawal(MBankAccount cashAccount, MBankStatement bankStatement, int depositBankAccountId, int currencyId, int conversionTypeId, BigDecimal amount, boolean isReconciled, String documentNo, String tenderType, int paymentMethodId, int businessPartnerId, List<Integer> paymentIds) {
		Timestamp statementDate = bankStatement.getStatementDate();
		Timestamp dateAcct = bankStatement.getStatementDate();
		MBankAccount mBankFrom = MBankAccount.get(bankStatement.getCtx(), bankStatement.getC_BankAccount_ID());
		MBankAccount mBankTo = MBankAccount.get(bankStatement.getCtx(), depositBankAccountId);

		MPayment paymentBankFrom = new MPayment(bankStatement.getCtx(), 0 ,  bankStatement.get_TrxName());
		if(!paymentIds.isEmpty()) {
			int paymentId = paymentIds.get(0);
			MPayment originalPayment = new MPayment(bankStatement.getCtx(), paymentId, bankStatement.get_TrxName());
			PO.copyValues(originalPayment, paymentBankFrom);
			paymentBankFrom.setC_POS_ID(-1);
		}
		paymentBankFrom.setRelatedPayment_ID(-1);
		paymentBankFrom.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
		paymentBankFrom.setDocumentNo(documentNo);
		paymentBankFrom.setDateAcct(dateAcct);
		paymentBankFrom.setDateTrx(statementDate);
		paymentBankFrom.setTenderType(tenderType);
		paymentBankFrom.setDescription(bankStatement.getDescription());
		paymentBankFrom.setC_BPartner_ID (businessPartnerId);
		paymentBankFrom.setC_Currency_ID(currencyId);
		if(conversionTypeId > 0) {
			paymentBankFrom.setC_ConversionType_ID(conversionTypeId);
		}
		if(paymentMethodId > 0) {
			paymentBankFrom.setC_PaymentMethod_ID(paymentMethodId);
		}
		paymentBankFrom.setPayAmt(amount);
		paymentBankFrom.setOverUnderAmt(Env.ZERO);
		if(cashAccount.getWithdrawalDocumentType_ID() != 0) {
			paymentBankFrom.setC_DocType_ID(cashAccount.getWithdrawalDocumentType_ID());
		} else {
			paymentBankFrom.setC_DocType_ID(false);
		}
		paymentBankFrom.setC_Charge_ID(cashAccount.getDepositCharge_ID());
		paymentBankFrom.saveEx();
		//
		MPayment paymentBankTo = new MPayment(bankStatement.getCtx(), 0 ,  bankStatement.get_TrxName());
		PO.copyValues(paymentBankFrom, paymentBankTo);
		paymentBankTo.setC_POS_ID(-1);
		paymentBankTo.setC_BankAccount_ID(mBankTo.getC_BankAccount_ID());
		paymentBankTo.setDocumentNo(documentNo);
		paymentBankTo.setDateAcct(dateAcct);
		paymentBankTo.setDateTrx(statementDate);
		paymentBankTo.setTenderType(tenderType);
		paymentBankTo.setDescription(bankStatement.getDescription());
		paymentBankTo.setC_BPartner_ID (businessPartnerId);
		paymentBankTo.setC_Currency_ID(currencyId);
		if(paymentBankFrom.getCreditCardType() != null) {
			paymentBankTo.setCreditCardType(paymentBankFrom.getCreditCardType());
		}
		if(paymentBankFrom.getC_CardProvider_ID() > 0) {
			paymentBankTo.setC_CardProvider_ID(paymentBankFrom.getC_CardProvider_ID());
		}
		if(paymentBankFrom.getC_Card_ID() > 0) {
			paymentBankTo.setC_Card_ID(paymentBankFrom.getC_Card_ID());
		}
		if(conversionTypeId > 0) {
			paymentBankTo.setC_ConversionType_ID(conversionTypeId);
		}
		if(paymentMethodId > 0) {
			paymentBankTo.setC_PaymentMethod_ID(paymentMethodId);
		}
		//	Support to cash opening
		if(bankStatement.getC_POS_ID() > 0) {
			paymentBankFrom.setC_POS_ID(bankStatement.getC_POS_ID());
			paymentBankTo.setC_POS_ID(bankStatement.getC_POS_ID());
		}
		paymentBankTo.setPayAmt(amount);
		paymentBankTo.setOverUnderAmt(Env.ZERO);
		if(cashAccount.getDepositDocumentType_ID() != 0) {
			paymentBankTo.setC_DocType_ID(cashAccount.getDepositDocumentType_ID());
		} else {
			paymentBankTo.setC_DocType_ID(true);
		}
		paymentBankTo.setC_Charge_ID(cashAccount.getDepositCharge_ID());
		paymentBankTo.saveEx();

		paymentBankFrom.setRelatedPayment_ID(paymentBankTo.getC_Payment_ID());
		paymentBankFrom.setDocStatus(MPayment.DOCSTATUS_Drafted);
		paymentBankFrom.saveEx();
		paymentBankFrom.processIt(MPayment.DOCACTION_Complete);
		paymentBankFrom.saveEx();
		log.fine("@C_Payment_ID@ @IsReceipt@: ");
		//	Add to current bank statement for account
		if(isReconciled) {
			MBankStatementLine bsl = MBankStatement.addPayment(paymentBankFrom);
			if(bsl != null) {
				log.fine("@C_Payment_ID@: " + paymentBankFrom.getDocumentNo()
						+ " @Added@ @to@ [@AccountNo@ " + paymentBankFrom.getC_BankAccount().getAccountNo()
						+ " @C_BankStatement_ID@ " + bsl.getC_BankStatement().getName() + "]");
			}
		}
		paymentBankTo.setRelatedPayment_ID(paymentBankFrom.getC_Payment_ID());
		paymentBankTo.setDocStatus(MPayment.DOCSTATUS_Drafted);
		paymentBankTo.saveEx();
		paymentBankTo.processIt(MPayment.DOCACTION_Complete);
		paymentBankTo.saveEx();
		if(!paymentIds.isEmpty()) {
			paymentIds.forEach(sourcePaymentId-> {
				MPayment sourcePayment = new MPayment(bankStatement.getCtx(), sourcePaymentId, bankStatement.get_TrxName());
				sourcePayment.setWithdrawal_ID(paymentBankFrom.getC_Payment_ID());
				sourcePayment.setDeposit_ID(paymentBankTo.getC_Payment_ID());
				sourcePayment.saveEx();
			});
		}
		//	Add to current bank statement for account
		if(isReconciled) {
			MBankStatementLine bsl = MBankStatement.addPayment(paymentBankTo);
			if(bsl != null) {
				log.fine("@C_Payment_ID@: " + paymentBankTo.getDocumentNo()
						+ " @Added@ @to@ [@AccountNo@ " + paymentBankTo.getC_BankAccount().getAccountNo()
						+ " @C_BankStatement_ID@ " + bsl.getC_BankStatement().getName() + "]");
			}
		}
		//	Return
		log.fine("@Created@ (1) @From@ " + mBankFrom.getAccountNo()+ " @To@ " + mBankTo.getAccountNo() + " @Amt@ " + DisplayType.getNumberFormat(DisplayType.Amount).format(amount));
	}

	/**
	 * Recalculate bank statement balance
	 */
	private static void calculateBankStatementBalance(MBankStatement bankStatement) {
		List<MBankStatementLine> lines = Arrays.asList(bankStatement.getLines(true));
		//	Lines
		AtomicReference<BigDecimal> total = new AtomicReference<BigDecimal>(Env.ZERO);
		AtomicReference<Timestamp> minimumDate = new AtomicReference<Timestamp>(bankStatement.getStatementDate());
		AtomicReference<Timestamp> maximumDate = new AtomicReference<Timestamp>(bankStatement.getStatementDate());
		lines.forEach(statementLine -> {
			total.updateAndGet(totalAmount -> totalAmount.add(statementLine.getStmtAmt()));
			if (statementLine.getDateAcct().before(minimumDate.get())) {
				minimumDate.set(statementLine.getDateAcct()); 
			}
			if (statementLine.getDateAcct().after(maximumDate.get())) {
				maximumDate.set(statementLine.getDateAcct());
			}
		});
		bankStatement.setStatementDifference(total.get());
		bankStatement.setEndingBalance(bankStatement.getBeginningBalance().add(total.get()));
		bankStatement.saveEx();
	}
	
	/**
	 * Validate that exists a cash opening
	 */
	public static void validateCashOpeningForPayment(MPayment payment) {
		if(payment.getC_POS_ID() <= 0) {
			return;
		}
		MBankAccount cashAccount = MBankAccount.get(payment.getCtx(), payment.getC_BankAccount_ID());
		if(cashAccount.isValidateCashOpening()) {
			int paymentId = new Query(payment.getCtx(), I_C_Payment.Table_Name, "DocStatus IN('CO', 'CL') "
					+ "AND IsReceipt = 'Y' "
					+ "AND C_Charge_ID IS NOT NULL "
					+ "AND DateTrx = ? "
					+ "AND EXISTS(SELECT 1 FROM C_BankStatementLine bsl WHERE bsl.C_Payment_ID = C_Payment.C_Payment_ID AND bsl.Processed = 'N')", payment.get_TrxName())
					.setParameters(TimeUtil.getDay(System.currentTimeMillis()))
					.firstId();
			if(paymentId <= 0) {
				throw new AdempiereException(Msg.parseTranslation(payment.getCtx(), "@CashOpeningValidationError@"));
			}
		}
	}
	
	/**
	 * Validate that exists a cash opening
	 */
	public static void validateCashOpeningForOrder(MOrder order) {
		if(order.getC_POS_ID() <= 0) {
			return;
		}
		MPOS pointOfSales = MPOS.get(order.getCtx(), order.getC_POS_ID());
		MBankAccount cashAccount = MBankAccount.get(order.getCtx(), pointOfSales.getC_BankAccount_ID());
		if(cashAccount.isValidateCashOpening()) {
			int paymentId = new Query(order.getCtx(), I_C_Payment.Table_Name, "DocStatus IN('CO', 'CL') "
					+ "AND IsReceipt = 'Y' "
					+ "AND C_Charge_ID IS NOT NULL "
					+ "AND C_POS_ID = ? "
					+ "AND DateTrx = ? "
					+ "AND EXISTS(SELECT 1 FROM C_BankStatementLine bsl WHERE bsl.C_Payment_ID = C_Payment.C_Payment_ID AND bsl.Processed = 'N')", order.get_TrxName())
					.setParameters(order.getC_POS_ID(), TimeUtil.getDay(System.currentTimeMillis()))
					.firstId();
			if(paymentId <= 0) {
				throw new AdempiereException(Msg.parseTranslation(order.getCtx(), "@CashOpeningValidationError@"));
			}
		}
	}
	
	/**
	 * Get List of payments for a bank statement
	 */
	private static List<PaymentWrapper> getCollectsToDeposit(MBankStatement bankStatement) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<PaymentWrapper> wrapperList = new ArrayList<PaymentWrapper>();
		try {
			String sql = "SELECT p.C_Payment_ID, p.DocumentNo, p.C_DocType_ID, p.C_BPartner_ID, p.C_Bank_ID, p.C_BankAccount_ID, p.TenderType, p.C_Currency_ID, p.C_PaymentMethod_ID, p.C_ConversionType_ID, (p.PayAmt * CASE WHEN p.IsReceipt = 'Y' THEN 1 ELSE -1 END) AS PaymentAmount "
					+ "FROM C_Payment p "
					+ "WHERE p.DocStatus IN('CO', 'CL') "
					+ "AND EXISTS(SELECT 1 FROM C_BankStatementLine bsl WHERE bsl.C_Payment_ID = p.C_Payment_ID AND bsl.C_BankStatement_ID = ?)";
			pstmt = DB.prepareStatement(sql, bankStatement.get_TrxName());
			pstmt.setInt(1, bankStatement.getC_BankStatement_ID());
			rs = pstmt.executeQuery();
			while(rs.next()) {
				wrapperList.add(PaymentWrapper.newInstance()
						.withPaymentId(rs.getInt("C_Payment_ID"))
						.withDocumentNo(rs.getString("DocumentNo"))
						.withDocumentTypeId(rs.getInt("C_DocType_ID"))
						.withBusinessPartnerId(rs.getInt("C_BPartner_ID"))
						.withBankId(rs.getInt("C_Bank_ID"))
						.withBankAccountId(rs.getInt("C_BankAccount_ID"))
						.withTenderType(rs.getString("TenderType"))
						.withCurrencyId(rs.getInt("C_Currency_ID"))
						.withConversionTypeId(rs.getInt("C_ConversionType_ID"))
						.withAmount(rs.getBigDecimal("PaymentAmount"))
						.withPaymentMethodId(rs.getInt("C_PaymentMethod_ID"))
				);
			}
		} catch (Exception e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
		}
		return wrapperList;
	}
}
