/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.pos.service.cash;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.X_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBankAccount;
import org.compiere.model.MBankStatement;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.RunBusinessProcessRequest;
import org.spin.backend.grpc.pos.CashClosingRequest;
import org.spin.backend.grpc.pos.CashMovements;
import org.spin.backend.grpc.pos.CashOpeningRequest;
import org.spin.backend.grpc.pos.CashWithdrawalRequest;
import org.spin.backend.grpc.pos.ListCashMovementsRequest;
import org.spin.backend.grpc.pos.ListCashMovementsResponse;
import org.spin.backend.grpc.pos.ListCashSummaryMovementsRequest;
import org.spin.backend.grpc.pos.ListCashSummaryMovementsResponse;
import org.spin.backend.grpc.pos.Payment;
import org.spin.backend.grpc.pos.PaymentSummary;
import org.spin.backend.grpc.pos.PaymentTotal;
import org.spin.backend.grpc.pos.PrintPreviewCashMovementsRequest;
import org.spin.backend.grpc.pos.PrintPreviewCashMovementsResponse;
import org.spin.base.util.RecordUtil;
import org.spin.grpc.service.BusinessData;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.process.inf_POS_Sales_Detail_And_CollectionAbstract;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.CurrencyCashKey;
import org.spin.pos.util.PaymentConvertUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class CashServiceLogic {

	/**
	 * Cash Opening
	 * @param request
	 * @return
	 */
	public static CashMovements.Builder cashOpening(CashOpeningRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if(pos.getC_BankAccount_ID() <= 0) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}

		AtomicReference<MBankStatement> bankStatementReference = new AtomicReference<MBankStatement>();
		Trx.run(transactionName -> {
			MBankAccount cashAccount = MBankAccount.get(Env.getCtx(), pos.getC_BankAccount_ID());
			int defaultChargeId = cashAccount.get_ValueAsInt("DefaultOpeningCharge_ID");
			if(defaultChargeId <= 0) {
				throw new AdempiereException("@DefaultOpeningCharge_ID@ @NotFound@");
			}
			final String whereClause = "C_POS_ID = ? AND C_Charge_ID = ? AND DocStatus = ?";
			List<Integer> paymentsIdList = new Query(
				Env.getCtx(),
				I_C_Payment.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(
					pos.getC_POS_ID(),
					defaultChargeId,
					X_C_Payment.DOCSTATUS_Drafted
				)
				.getIDsAsList()
			;
			if(paymentsIdList == null || paymentsIdList.isEmpty()) {
				throw new AdempiereException("@C_Payment_ID@ @NotFound@");
			}

			paymentsIdList.forEach(paymentId -> {
				MPayment payment = new MPayment(Env.getCtx(), paymentId, transactionName);
				payment.setDateTrx(
					RecordUtil.getDate()
				);
				payment.saveEx();

				//	Add bank statement
				MBankStatement bankStatement = CashManagement.createCashClosing(pos, payment);
				//	
				CashManagement.processPayment(pos, payment, transactionName);

				//
				bankStatementReference.set(bankStatement);
			});
		});

		CashMovements.Builder cashClosing = CashConvertUtil.convertCashMovements(
			bankStatementReference.get()
		);
		return cashClosing;
	}


	/**
	 * Closing
	 * @param request
	 * @return
	 */
	public static CashMovements.Builder cashClosing(CashClosingRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}

		AtomicReference<MBankStatement> bankStatementReference = new AtomicReference<MBankStatement>();
		Trx.run(transactionName -> {
			int bankStatementId = request.getId();
			if(bankStatementId <= 0
					&& request.getId() <= 0) {
				throw new AdempiereException("@C_BankStatement_ID@ @NotFound@");
			}
			MBankStatement bankStatement = new MBankStatement(Env.getCtx(), bankStatementId, transactionName);
			if(bankStatement.isProcessed()) {
				throw new AdempiereException("@C_BankStatement_ID@ @Processed@");
			}
			if(!Util.isEmpty(request.getDescription())) {
				bankStatement.addDescription(request.getDescription());
			}
			bankStatement.setDocStatus(MBankStatement.DOCSTATUS_Drafted);
			bankStatement.setDocAction(MBankStatement.ACTION_Complete);
			bankStatement.saveEx(transactionName);
			if(!bankStatement.processIt(DocAction.ACTION_Complete)) {
				throw new AdempiereException(bankStatement.getProcessMsg());
			}
			bankStatement.saveEx(transactionName);

			//
			bankStatementReference.set(bankStatement);
		});

		CashMovements.Builder cashClosing = CashConvertUtil.convertCashMovements(
			bankStatementReference.get()
		);

		return cashClosing;
	}


	/**
	 * Withdrawal
	 * @param request
	 * @return
	 */
	public static CashMovements.Builder cashWithdrawal(CashWithdrawalRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		// if(request.getCollectingAgentId() <= 0) {
		// 	throw new AdempiereException("@CollectingAgent_ID@ @NotFound@");
		// }
		if(pos.getC_BankAccount_ID() <= 0) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}
		Trx.run(transactionName -> {
			MBankAccount cashAccount = MBankAccount.get(Env.getCtx(), pos.getC_BankAccount_ID());
			int defaultChargeId = cashAccount.get_ValueAsInt("DefaultWithdrawalCharge_ID");
			if(defaultChargeId <= 0) {
				throw new AdempiereException("@DefaultWithdrawalCharge_ID@ @NotFound@");
			}
			final String whereClause = "C_POS_ID = ? AND C_Charge_ID = ? AND DocStatus = ?";
			List<Integer> paymentsIdList = new Query(
				Env.getCtx(),
				I_C_Payment.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(
					pos.getC_POS_ID(),
					defaultChargeId,
					X_C_Payment.DOCSTATUS_Drafted
				)
				.getIDsAsList()
			;
			if(paymentsIdList == null || paymentsIdList.isEmpty()) {
				throw new AdempiereException("@C_Payment_ID@ @NotFound@");
			}

			paymentsIdList.forEach(paymentId -> {
				MPayment payment = new MPayment(Env.getCtx(), paymentId, transactionName);
				payment.setDateTrx(
					RecordUtil.getDate()
				);
				payment.saveEx();
				//	
				CashManagement.processPayment(pos, payment, transactionName);
			});
		});
		return CashMovements.newBuilder();
	}


	/**
	 * List all movements from cash
	 * @return
	 */
	public static ListCashMovementsResponse.Builder listCashMovements(ListCashMovementsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @IsMandatory@");
		}
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		MBankStatement cashClosing = CashManagement.getOpenCashClosing(pos, RecordUtil.getDate(), true, null);
		if(cashClosing == null
				|| cashClosing.getC_BankStatement_ID() <= 0) {
			throw new AdempiereException("@C_BankStatement_ID@ @NotFound@");
		}

		List<Object> parameters = new ArrayList<Object>();
		StringBuffer whereClause = new StringBuffer(
			"DocStatus IN('CO', 'CL') "
			+ "AND C_POS_ID = ? "
			+ "AND EXISTS("
				+ "SELECT 1 FROM C_BankStatementLine AS bsl "
				+ "WHERE bsl.C_Payment_ID = C_Payment.C_Payment_ID "
				+ "AND bsl.C_BankStatement_ID = ?"
			+ ")"
		);
		parameters.add(pos.getC_POS_ID());
		parameters.add(cashClosing.getC_BankStatement_ID());
		//	Optional
		if(request.getBusinessPartnerId() > 0) {
			parameters.add(request.getBusinessPartnerId());
			whereClause.append(" AND C_BPartner_ID = ?");
		} else if(request.getSalesRepresentativeId() > 0) {
			parameters.add(request.getSalesRepresentativeId());
			whereClause.append(" AND CollectingAgent_ID = ?");
		}
		//	Get Refund Reference list
		Query query = new Query(
			Env.getCtx(),
			I_C_Payment.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;

		//	Set page token
		int count = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		HashMap<Integer, BigDecimal> cashCurrencySummary = new HashMap<Integer, BigDecimal>();
		ListCashMovementsResponse.Builder builder = ListCashMovementsResponse.newBuilder()
			.setId(
				cashClosing.getC_BankStatement_ID()
			)
			.setDocumentNo(
				StringManager.getValidString(
					cashClosing.getDocumentNo()
				)
			)
			.setName(
				StringManager.getValidString(
					cashClosing.getName()
				)
			)
			.setDate(
				ValueManager.getTimestampFromDate(
					cashClosing.getStatementDate()
				)
			)
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query
			.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(paymentId -> {
				MPayment payment = new MPayment(pos.getCtx(), paymentId, null);
				Payment.Builder paymentBuilder = PaymentConvertUtil.convertPayment(
					payment
				);
				builder.addCashMovements(
					paymentBuilder
				);

				int currencyId = payment.getC_Currency_ID();
				BigDecimal amount = payment.getPayAmt(true);
				BigDecimal totalAmount = Env.ZERO;
				BigDecimal paymentAmount = Optional.ofNullable(amount).orElse(Env.ZERO);
				if (cashCurrencySummary.containsKey(currencyId)) {
					totalAmount = cashCurrencySummary.get(currencyId);
				}
				totalAmount = totalAmount.add(paymentAmount);
				cashCurrencySummary.put(currencyId, totalAmount);
			})
		;

		cashCurrencySummary.forEach((currencyId, totalAmount) -> {
			PaymentTotal.Builder paymentTotalBuilder = PaymentTotal.newBuilder()
				.setCurrency(
					CoreFunctionalityConvert.convertCurrency(
						currencyId
					)
				)
				.setTotalAmount(
					NumberManager.getBigDecimalToString(
						totalAmount
					)
				)
			;
			builder.addTotalMovements(paymentTotalBuilder);
		});

		//	Return
		return builder;
	}


	/**
	 * List all movements from cash
	 * @return
	 */
	public static ListCashSummaryMovementsResponse.Builder listCashSummaryMovements(ListCashSummaryMovementsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @IsMandatory@");
		}
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		MBankStatement cashClosing = CashManagement.getOpenCashClosing(pos, RecordUtil.getDate(), true, null);
		if(cashClosing == null
				|| cashClosing.getC_BankStatement_ID() <= 0) {
			throw new AdempiereException("@C_BankStatement_ID@ @NotFound@");
		}

		Map<CurrencyCashKey, BigDecimal> cashCurrencySummary = new HashMap<CurrencyCashKey, BigDecimal>();
		ListCashSummaryMovementsResponse.Builder builder = ListCashSummaryMovementsResponse.newBuilder()
			.setId(
				cashClosing.getC_BankStatement_ID()
			)
			.setDocumentNo(
				StringManager.getValidString(
					cashClosing.getDocumentNo()
				)
			)
			.setName(
				StringManager.getValidString(
					cashClosing.getName()
				)
			)
			.setDate(
				ValueManager.getTimestampFromDate(
					cashClosing.getStatementDate()
				)
			)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		String sql = "SELECT "
				+ "pm.C_PaymentMethod_ID, "
				+ "pm.Name AS PaymentMethodName, "
				+ "pm.TenderType AS TenderTypeCode, "
				+ "p.C_Currency_ID, "
				;
		if (request.getIsDetailMovementType()) {
			sql += "p.IsReceipt, ";
		}

		sql += "(SUM(CASE WHEN p.PayAmt <> 0 THEN p.PayAmt ELSE COALESCE(p.ECA14_Reference_Amount, 0) END";
		if (request.getIsDetailMovementType()) {
			sql += ")";
		}
		sql	+= " * CASE WHEN p.IsReceipt = 'Y' THEN 1 ELSE -1 END)";
		if (!request.getIsDetailMovementType()) {
			sql += ")";
		}

		sql += " AS PaymentAmount "
				+ "FROM C_Payment AS p "
				+ "INNER JOIN C_PaymentMethod AS pm ON(pm.C_PaymentMethod_ID = p.C_PaymentMethod_ID) "
				+ "WHERE p.DocStatus IN('CO', 'CL') "
				+ "AND p.C_POS_ID = ? "
				+ "AND EXISTS(SELECT 1 FROM C_BankStatementLine bsl WHERE bsl.C_Payment_ID = p.C_Payment_ID AND bsl.C_BankStatement_ID = ?) "
				+ "GROUP BY pm.C_PaymentMethod_ID, pm.Name, pm.TenderType, p.C_Currency_ID"
		;

		if (request.getIsDetailMovementType()) {
			sql += ", p.IsReceipt ";
		}
		AtomicInteger counter = new AtomicInteger(0);
		//	Count records
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(pos.getC_POS_ID());
		parameters.add(cashClosing.getC_BankStatement_ID());
		DB.runResultSet(null, sql, parameters, resultset -> {
			while (resultset.next()) {
				int currencyId = resultset.getInt("C_Currency_ID");
				BigDecimal amount = resultset.getBigDecimal("PaymentAmount");
				String tenderTypeCode = resultset.getString("TenderTypeCode");
				PaymentSummary.Builder paymentSummary = PaymentSummary.newBuilder()
						.setPaymentMethodId(
								resultset.getInt("C_PaymentMethod_ID")
						)
						.setPaymentMethodName(
								StringManager.getValidString(
										resultset.getString("PaymentMethodName")
								)
						)
						.setTenderTypeCode(
								StringManager.getValidString(
										tenderTypeCode
								)
						)
						.setCurrency(
								CoreFunctionalityConvert.convertCurrency(
										currencyId
								)
						)
						.setAmount(
								NumberManager.getBigDecimalToString(
										amount
								)
						)
						;
				if (request.getIsDetailMovementType()) {
					paymentSummary.setIsRefund(
							!resultset.getBoolean("IsReceipt")
					);
				}

				BigDecimal totalAmount = Env.ZERO;
				BigDecimal paymentAmount = Optional.ofNullable(amount).orElse(Env.ZERO);
				CurrencyCashKey key = CurrencyCashKey.newInstance(currencyId, tenderTypeCode);
				if (cashCurrencySummary.containsKey(key)) {
					totalAmount = cashCurrencySummary.get(key);
				}
				totalAmount = totalAmount.add(paymentAmount);
				cashCurrencySummary.put(key, totalAmount);
				//
				builder.addCashMovements(paymentSummary.build());
				counter.incrementAndGet();
			}
		}).onFailure(throwable -> {
			throw new AdempiereException(throwable);
		});
		cashCurrencySummary.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			CurrencyCashKey currencyCashKey = entry.getKey();
			BigDecimal totalAmount = entry.getValue();
			PaymentTotal.Builder paymentTotalBuilder = PaymentTotal.newBuilder()
				.setCurrency(
					CoreFunctionalityConvert.convertCurrency(
							currencyCashKey.getCurrencyId()
					)
				)
					.setDescription(currencyCashKey.getValidDisplayValue())
				.setTotalAmount(
					NumberManager.getBigDecimalToString(
						totalAmount
					)
				)
			;
			builder.addTotalMovements(paymentTotalBuilder);
		});

		//	Set page token
		if(LimitUtil.isValidNextPageToken(counter.get(), offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
			.setRecordCount(counter.get())
		;
		//	Return
		return builder;
	}


	public static PrintPreviewCashMovementsResponse.Builder printPreviewCashMovements(PrintPreviewCashMovementsRequest request) throws FileNotFoundException, IOException {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

		// inf_POS_Sales_Detail_And_Collection
		final int processId = inf_POS_Sales_Detail_And_CollectionAbstract.getProcessId();
		String reportType = "pdf";
		if (!Util.isEmpty(request.getReportType(), true)) {
			reportType = request.getReportType();
		}
		Struct.Builder parameters = Struct.newBuilder();

		Timestamp dateFrom = null;
		Timestamp dateTo = null;
		if (request.getBankStatementId() > 0) {
			Value.Builder bankStatementBuilder = ValueManager.getValueFromInt(
				request.getBankStatementId()
			);
			parameters.putFields(
					MBankStatement.COLUMNNAME_C_BankStatement_ID,
					bankStatementBuilder.build()
			);
		}
		if (request.hasDateFrom() && request.hasDateTo()) {
			dateFrom = ValueManager.getDateFromTimestampDate(request.getDateFrom());
			dateTo = ValueManager.getDateFromTimestampDate(request.getDateTo());
		} else {
			Timestamp nowTimestamp = new Timestamp(System.currentTimeMillis());
			dateFrom = TimeUtil.getDay(nowTimestamp);
			dateTo = dateFrom;
		}

		Value.Builder builderDateInvoiced = ValueManager.getValueFromTimestamp(
			dateFrom
		);
		Value.Builder builderDateInvoicedTo = ValueManager.getValueFromTimestamp(
			dateTo
		);
		Value valueDateFrom = builderDateInvoiced.build();
		Value valueDateTo = builderDateInvoicedTo.build();
		parameters.putFields(
			inf_POS_Sales_Detail_And_CollectionAbstract.DATEINVOICED,
			valueDateFrom
		);
		parameters.putFields(
			inf_POS_Sales_Detail_And_CollectionAbstract.DATEINVOICED + "_To",
			valueDateTo
		);

		Value.Builder builderPosId = ValueManager.getValueFromInt(
			pos.getC_POS_ID()
		);

		parameters.putFields(
			inf_POS_Sales_Detail_And_CollectionAbstract.C_POS_ID,
			builderPosId.build()
		);

		RunBusinessProcessRequest.Builder processRequest = RunBusinessProcessRequest.newBuilder()
			.setId(processId)
			.setReportType(reportType)
			.setParameters(parameters)
		;

		ProcessLog.Builder processLog = BusinessData.runBusinessProcess(
			processRequest.build()
		);

		// preview document
		PrintPreviewCashMovementsResponse.Builder ticket = PrintPreviewCashMovementsResponse.newBuilder()
			.setResult("Ok")
			.setProcessLog(
				processLog.build()
			)
		;

		return ticket;
	}

}
