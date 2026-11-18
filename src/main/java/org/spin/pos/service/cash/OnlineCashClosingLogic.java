/************************************************************************************
 * Copyright (C) 2012-present E.R.P. Consultores y Asociados, C.A.                  *
 * This program is free software: you can redistribute it and/or modify            *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.pos.service.cash;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_C_PaymentProcessor;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.interfaces.PaymentProcessorClosing;
import org.compiere.model.MBankStatement;
import org.compiere.model.MColumn;
import org.compiere.model.MPOS;
import org.compiere.model.MPaymentProcessor;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.CashMovements;
import org.spin.backend.grpc.pos.ListCashClosingsRequest;
import org.spin.backend.grpc.pos.ListCashClosingsResponse;
import org.spin.backend.grpc.pos.ListOnlineCashClosingProcessorsRequest;
import org.spin.backend.grpc.pos.ListOnlineCashClosingProcessorsResponse;
import org.spin.backend.grpc.pos.ListOnlineCashClosingsRequest;
import org.spin.backend.grpc.pos.ListOnlineCashClosingsResponse;
import org.spin.backend.grpc.pos.OnlineCashClosing;
import org.spin.backend.grpc.pos.OnlineCashClosingProcessor;
import org.spin.pos.service.pos.POS;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

/**
 * Online cash closing with several payment processors: one closing (payment processor run)
 * for each processor and terminal (store payment method) with approved online payments.
 * The processor is taken from payment, from the store payment method (terminal) or,
 * for compatibility, from the first processor of the bank account.
 */
public class OnlineCashClosingLogic {

	/**	Logger	*/
	private static final CLogger log = CLogger.getCLogger(OnlineCashClosingLogic.class);

	public static final String TABLE_PAYMENT_PROCESSOR_RUN = "C_PaymentProcessorRun";
	public static final String TABLE_PAYMENT_METHOD = "C_PaymentMethod";
	public static final String TABLE_PAYMENT = "C_Payment";
	public static final String COLUMN_PAYMENT_PROCESSOR_ID = I_C_PaymentProcessor.COLUMNNAME_C_PaymentProcessor_ID;
	public static final String COLUMN_PAYMENT_METHOD_ID = "C_PaymentMethod_ID";
	public static final String COLUMN_BANK_STATEMENT_ID = "C_BankStatement_ID";
	public static final String COLUMN_RESPONSE_STATUS = "ResponseStatus";
	public static final String COLUMN_RESPONSE_MESSAGE = "ResponseMessage";
	public static final String COLUMN_NEXT_REQUEST_TIME = "NextRequestTime";

	public static final String STATUS_APPROVED = "A";
	public static final String STATUS_ERROR = "E";
	public static final String STATUS_REJECTED = "R";

	/**
	 * Processor and terminal to close for a cash closing
	 */
	public static class ProcessorClosing {
		private final MPaymentProcessor paymentProcessor;
		private final int paymentMethodId;
		private int paymentsCount = 0;
		private PO lastRun;

		ProcessorClosing(MPaymentProcessor paymentProcessor, int paymentMethodId) {
			this.paymentProcessor = paymentProcessor;
			this.paymentMethodId = paymentMethodId;
		}

		public MPaymentProcessor getPaymentProcessor() {
			return paymentProcessor;
		}

		public int getPaymentMethodId() {
			return paymentMethodId;
		}

		public int getPaymentsCount() {
			return paymentsCount;
		}

		public PO getLastRun() {
			return lastRun;
		}

		public boolean isClosed() {
			return lastRun != null && STATUS_APPROVED.equals(lastRun.get_ValueAsString(COLUMN_RESPONSE_STATUS));
		}
	}

	/**
	 * Validate if the table has the C_PaymentProcessor_ID column (dictionary not yet applied)
	 * @param tableName
	 * @return
	 */
	public static boolean isPaymentProcessorColumnAvailable(String tableName) {
		return MColumn.getColumn_ID(tableName, COLUMN_PAYMENT_PROCESSOR_ID) > 0;
	}

	/**
	 * Set processor on record when the column exists
	 * @param entity
	 * @param paymentProcessorId
	 */
	public static void setPaymentProcessorId(PO entity, int paymentProcessorId) {
		if (entity == null || paymentProcessorId <= 0 || entity.get_ColumnIndex(COLUMN_PAYMENT_PROCESSOR_ID) < 0) {
			return;
		}
		entity.set_ValueOfColumn(COLUMN_PAYMENT_PROCESSOR_ID, paymentProcessorId);
	}

	private static MPaymentProcessor getActivePaymentProcessor(int paymentProcessorId, String transactionName) {
		if (paymentProcessorId <= 0) {
			return null;
		}
		MPaymentProcessor paymentProcessor = new MPaymentProcessor(Env.getCtx(), paymentProcessorId, transactionName);
		if (paymentProcessor.getC_PaymentProcessor_ID() <= 0 || !paymentProcessor.isActive()) {
			return null;
		}
		return paymentProcessor;
	}

	/**
	 * Processor of store payment method (terminal)
	 * @param paymentMethodId
	 * @param transactionName
	 * @return
	 */
	public static MPaymentProcessor getPaymentProcessorFromPaymentMethod(int paymentMethodId, String transactionName) {
		if (paymentMethodId <= 0 || !isPaymentProcessorColumnAvailable(TABLE_PAYMENT_METHOD)) {
			return null;
		}
		int paymentProcessorId = DB.getSQLValue(
			transactionName,
			"SELECT C_PaymentProcessor_ID FROM C_PaymentMethod WHERE C_PaymentMethod_ID = ?",
			paymentMethodId
		);
		return getActivePaymentProcessor(paymentProcessorId, transactionName);
	}

	/**
	 * First processor of bank account (previous behavior, only one processor by account)
	 * @param bankAccountId
	 * @param transactionName
	 * @return
	 */
	public static MPaymentProcessor getPaymentProcessorFromBankAccount(int bankAccountId, String transactionName) {
		if (bankAccountId <= 0) {
			return null;
		}
		return new Query(
			Env.getCtx(),
			I_C_PaymentProcessor.Table_Name,
			I_C_PaymentProcessor.COLUMNNAME_C_BankAccount_ID + " = ?",
			transactionName
		)
			.setParameters(bankAccountId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(COLUMN_PAYMENT_PROCESSOR_ID)
			.first()
		;
	}

	/**
	 * Resolve the processor to use: explicit processor, processor of terminal or processor of bank account
	 * @param paymentProcessorId optional
	 * @param paymentMethodId terminal
	 * @param bankAccountId bank account of cash
	 * @param transactionName
	 * @return processor or null
	 */
	public static MPaymentProcessor getPaymentProcessor(int paymentProcessorId, int paymentMethodId, int bankAccountId, String transactionName) {
		if (paymentProcessorId > 0) {
			MPaymentProcessor paymentProcessor = getActivePaymentProcessor(paymentProcessorId, transactionName);
			if (paymentProcessor == null) {
				throw new AdempiereException("@C_PaymentProcessor_ID@ (" + paymentProcessorId + ") @NotFound@");
			}
			return paymentProcessor;
		}
		MPaymentProcessor paymentProcessor = getPaymentProcessorFromPaymentMethod(paymentMethodId, transactionName);
		if (paymentProcessor != null) {
			return paymentProcessor;
		}
		return getPaymentProcessorFromBankAccount(bankAccountId, transactionName);
	}

	/**
	 * Validate if the processor class supports batch closing
	 * @param paymentProcessor
	 * @return
	 */
	public static boolean isClosingSupported(MPaymentProcessor paymentProcessor) {
		if (paymentProcessor == null) {
			return false;
		}
		if (Util.isEmpty(paymentProcessor.getPayProcessorClass(), true)) {
			return false;
		}
		try {
			Class<?> processorClass = Class.forName(paymentProcessor.getPayProcessorClass().trim());
			return PaymentProcessorClosing.class.isAssignableFrom(processorClass);
		} catch (Exception e) {
			log.warning(paymentProcessor + ": " + e.getLocalizedMessage());
			return false;
		}
	}

	/**
	 * Get last online closing (run) of a processor and terminal for a cash closing
	 * @param bankStatementId
	 * @param paymentMethodId
	 * @param paymentProcessorId
	 * @param transactionName
	 * @return run or null
	 */
	public static PO getLastRun(int bankStatementId, int paymentMethodId, int paymentProcessorId, String transactionName) {
		StringBuilder whereClause = new StringBuilder("C_BankStatement_ID = ? AND C_PaymentMethod_ID = ? AND C_Payment_ID IS NULL");
		List<Object> parameters = new ArrayList<>();
		parameters.add(bankStatementId);
		parameters.add(paymentMethodId);
		if (paymentProcessorId > 0 && isPaymentProcessorColumnAvailable(TABLE_PAYMENT_PROCESSOR_RUN)) {
			whereClause.append(" AND (C_PaymentProcessor_ID = ? OR C_PaymentProcessor_ID IS NULL)");
			parameters.add(paymentProcessorId);
		}
		return new Query(
			Env.getCtx(),
			TABLE_PAYMENT_PROCESSOR_RUN,
			whereClause.toString(),
			transactionName
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOrderBy("Created DESC, C_PaymentProcessorRun_ID DESC")
			.first()
		;
	}

	/**
	 * Get processors (processor + terminal) with approved online payments in the cash closing
	 * @param bankStatement cash closing
	 * @param transactionName
	 * @return list of processors to close, with the last closing of each one
	 */
	public static List<ProcessorClosing> getProcessorsToClose(MBankStatement bankStatement, String transactionName) {
		Map<String, ProcessorClosing> processors = new LinkedHashMap<>();
		if (bankStatement == null || bankStatement.getC_BankStatement_ID() <= 0) {
			return new ArrayList<>(processors.values());
		}
		boolean isPaymentWithProcessor = isPaymentProcessorColumnAvailable(TABLE_PAYMENT);
		String processorColumn = isPaymentWithProcessor ? "COALESCE(p.C_PaymentProcessor_ID, 0)" : "0";
		String sql = "SELECT p.C_PaymentMethod_ID, " + processorColumn + " AS C_PaymentProcessor_ID, COUNT(*) "
			+ "FROM C_Payment p "
			+ "INNER JOIN C_BankStatementLine line ON (line.C_Payment_ID = p.C_Payment_ID) "
			+ "WHERE line.C_BankStatement_ID = ? "
			+ "AND p.IsOnline = 'Y' "
			+ "AND p.ResponseStatus = 'A' "
			+ "AND p.C_PaymentMethod_ID IS NOT NULL "
			+ "GROUP BY p.C_PaymentMethod_ID, " + processorColumn + " "
			+ "ORDER BY p.C_PaymentMethod_ID"
		;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, transactionName);
			pstmt.setInt(1, bankStatement.getC_BankStatement_ID());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int paymentMethodId = rs.getInt(1);
				int paymentProcessorId = rs.getInt(2);
				int count = rs.getInt(3);
				MPaymentProcessor paymentProcessor = null;
				if (paymentProcessorId > 0) {
					paymentProcessor = getActivePaymentProcessor(paymentProcessorId, transactionName);
				}
				if (paymentProcessor == null) {
					paymentProcessor = getPaymentProcessor(0, paymentMethodId, bankStatement.getC_BankAccount_ID(), transactionName);
				}
				if (paymentProcessor == null) {
					log.warning("@C_PaymentProcessor_ID@ @NotFound@ C_PaymentMethod_ID=" + paymentMethodId);
					continue;
				}
				if (!isClosingSupported(paymentProcessor)) {
					continue;
				}
				String key = paymentProcessor.getC_PaymentProcessor_ID() + "|" + paymentMethodId;
				ProcessorClosing processorClosing = processors.get(key);
				if (processorClosing == null) {
					processorClosing = new ProcessorClosing(paymentProcessor, paymentMethodId);
					processors.put(key, processorClosing);
				}
				processorClosing.paymentsCount += count;
			}
		} catch (Exception e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
		}
		processors.values().forEach(processorClosing -> {
			processorClosing.lastRun = getLastRun(
				bankStatement.getC_BankStatement_ID(),
				processorClosing.getPaymentMethodId(),
				processorClosing.getPaymentProcessor().getC_PaymentProcessor_ID(),
				transactionName
			);
		});
		return new ArrayList<>(processors.values());
	}

	/**
	 * Processors pending to close
	 * @param bankStatement
	 * @param transactionName
	 * @return
	 */
	public static List<ProcessorClosing> getPendingProcessors(MBankStatement bankStatement, String transactionName) {
		return getProcessorsToClose(bankStatement, transactionName).stream()
			.filter(processorClosing -> !processorClosing.isClosed())
			.collect(Collectors.toList());
	}

	/**
	 * Update online closing approved flag of cash closing: only when all processors are closed
	 * @param bankStatement
	 * @param transactionName
	 * @return true if all processors are closed
	 */
	public static boolean updateOnlineClosingApproved(MBankStatement bankStatement, String transactionName) {
		List<ProcessorClosing> processors = getProcessorsToClose(bankStatement, transactionName);
		boolean isAllClosed = !processors.isEmpty() && processors.stream().allMatch(ProcessorClosing::isClosed);
		if (bankStatement.isOnlineClosingApproved() != isAllClosed) {
			bankStatement.setIsOnlineClosingApproved(isAllClosed);
			bankStatement.saveEx(transactionName);
		}
		return isAllClosed;
	}

	/**
	 * Validate the online closing before the cash closing
	 * @param bankStatement
	 * @param transactionName
	 */
	public static void validateOnlineClosing(MBankStatement bankStatement, String transactionName) {
		List<ProcessorClosing> processors = getProcessorsToClose(bankStatement, transactionName);
		if (processors.isEmpty()) {
			//	Without processor resolved: previous validation
			if (CashManagement.isCashMovementWithOnlinePaymentApproved(bankStatement.getC_BankStatement_ID())
					&& !bankStatement.isOnlineClosingApproved()) {
				throw new AdempiereException("@C_BankStatement_ID@ @Online@ (" + bankStatement.getDocumentNo() + ") @POS.PreviousCashClosingOpened@ (@Online@)");
			}
			return;
		}
		List<ProcessorClosing> pending = processors.stream()
			.filter(processorClosing -> !processorClosing.isClosed())
			.collect(Collectors.toList());
		if (!pending.isEmpty()) {
			String pendingNames = pending.stream()
				.map(processorClosing -> processorClosing.getPaymentProcessor().getName()
					+ " / " + getPaymentMethodName(processorClosing.getPaymentMethodId()))
				.collect(Collectors.joining(", "));
			throw new AdempiereException("@C_BankStatement_ID@ @Online@ (" + bankStatement.getDocumentNo() + ") @POS.PreviousCashClosingOpened@ (@Online@): " + pendingNames);
		}
	}

	public static String getPaymentMethodName(int paymentMethodId) {
		if (paymentMethodId <= 0) {
			return "";
		}
		return DB.getSQLValueString(null, "SELECT Name FROM C_PaymentMethod WHERE C_PaymentMethod_ID = ?", paymentMethodId);
	}

	public static String getPaymentMethodValue(int paymentMethodId) {
		if (paymentMethodId <= 0) {
			return "";
		}
		return DB.getSQLValueString(null, "SELECT Value FROM C_PaymentMethod WHERE C_PaymentMethod_ID = ?", paymentMethodId);
	}

	/**
	 * Get processor of an online closing (run)
	 * @param paymentProcessorRun
	 * @param bankAccountId
	 * @param transactionName
	 * @return
	 */
	public static MPaymentProcessor getPaymentProcessorOfRun(PO paymentProcessorRun, int bankAccountId, String transactionName) {
		int paymentProcessorId = 0;
		if (paymentProcessorRun.get_ColumnIndex(COLUMN_PAYMENT_PROCESSOR_ID) >= 0) {
			paymentProcessorId = paymentProcessorRun.get_ValueAsInt(COLUMN_PAYMENT_PROCESSOR_ID);
		}
		MPaymentProcessor paymentProcessor = getActivePaymentProcessor(paymentProcessorId, transactionName);
		if (paymentProcessor != null) {
			return paymentProcessor;
		}
		return getPaymentProcessor(0, paymentProcessorRun.get_ValueAsInt(COLUMN_PAYMENT_METHOD_ID), bankAccountId, transactionName);
	}

	public static boolean isErrorStatus(String status) {
		return STATUS_ERROR.equals(status) || STATUS_REJECTED.equals(status);
	}

	/**
	 * List processors to close of a cash closing
	 * @param request
	 * @return
	 */
	public static ListOnlineCashClosingProcessorsResponse.Builder listOnlineCashClosingProcessors(ListOnlineCashClosingProcessorsRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BankStatement_ID@");
		}
		POS.validateAndGetPOS(request.getPosId(), true);
		MBankStatement bankStatement = new MBankStatement(Env.getCtx(), request.getId(), null);
		if (bankStatement.getC_BankStatement_ID() <= 0) {
			throw new AdempiereException("@C_BankStatement_ID@ (" + request.getId() + ") @NotFound@");
		}
		List<ProcessorClosing> processors = getProcessorsToClose(bankStatement, null);
		ListOnlineCashClosingProcessorsResponse.Builder builder = ListOnlineCashClosingProcessorsResponse.newBuilder()
			.setRecordCount(processors.size())
			.setIsAllClosed(
				!processors.isEmpty() && processors.stream().allMatch(ProcessorClosing::isClosed)
			)
		;
		processors.forEach(processorClosing -> {
			OnlineCashClosingProcessor.Builder processorBuilder = OnlineCashClosingProcessor.newBuilder()
				.setPaymentProcessorId(
					processorClosing.getPaymentProcessor().getC_PaymentProcessor_ID()
				)
				.setPaymentProcessorName(
					TextManager.getValidString(
						processorClosing.getPaymentProcessor().getName()
					)
				)
				.setPaymentMethodId(
					processorClosing.getPaymentMethodId()
				)
				.setPaymentMethodName(
					TextManager.getValidString(
						getPaymentMethodName(processorClosing.getPaymentMethodId())
					)
				)
				.setTerminalCode(
					TextManager.getValidString(
						getPaymentMethodValue(processorClosing.getPaymentMethodId())
					)
				)
				.setPaymentsCount(
					processorClosing.getPaymentsCount()
				)
				.setIsClosed(
					processorClosing.isClosed()
				)
			;
			PO lastRun = processorClosing.getLastRun();
			if (lastRun != null) {
				String status = lastRun.get_ValueAsString(COLUMN_RESPONSE_STATUS);
				processorBuilder
					.setPaymentProcessorRunId(
						lastRun.get_ID()
					)
					.setStatus(
						TextManager.getValidString(status)
					)
					.setMessage(
						TextManager.getValidString(
							lastRun.get_ValueAsString(COLUMN_RESPONSE_MESSAGE)
						)
					)
					.setIsError(
						isErrorStatus(status)
					)
					.setNextRequestTime(
						lastRun.get_ValueAsInt(COLUMN_NEXT_REQUEST_TIME)
					)
				;
			}
			builder.addProcessors(processorBuilder);
		});
		return builder;
	}

	/**
	 * List online closings (runs) of POS, ordered by date (last first)
	 * @param request
	 * @return
	 */
	public static ListOnlineCashClosingsResponse.Builder listOnlineCashClosings(ListOnlineCashClosingsRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		boolean isRunWithProcessor = isPaymentProcessorColumnAvailable(TABLE_PAYMENT_PROCESSOR_RUN);
		List<Object> parameters = new ArrayList<>();
		StringBuilder whereClause = new StringBuilder(
			"C_Payment_ID IS NULL "
			+ "AND EXISTS(SELECT 1 FROM C_BankStatement bs "
				+ "WHERE bs.C_BankStatement_ID = C_PaymentProcessorRun.C_BankStatement_ID "
				+ "AND bs.C_POS_ID = ?)"
		);
		parameters.add(pos.getC_POS_ID());
		if (request.getBankStatementId() > 0) {
			whereClause.append(" AND C_BankStatement_ID = ?");
			parameters.add(request.getBankStatementId());
		}
		if (request.getPaymentMethodId() > 0) {
			whereClause.append(" AND C_PaymentMethod_ID = ?");
			parameters.add(request.getPaymentMethodId());
		}
		if (request.getPaymentProcessorId() > 0) {
			if (isRunWithProcessor) {
				//	Previous runs without processor: by terminal of processor
				whereClause.append(" AND (C_PaymentProcessor_ID = ?"
					+ " OR (C_PaymentProcessor_ID IS NULL AND EXISTS(SELECT 1 FROM C_PaymentMethod pm"
					+ " WHERE pm.C_PaymentMethod_ID = C_PaymentProcessorRun.C_PaymentMethod_ID"
					+ " AND pm.C_PaymentProcessor_ID = ?)))");
				parameters.add(request.getPaymentProcessorId());
				parameters.add(request.getPaymentProcessorId());
			} else {
				whereClause.append(" AND EXISTS(SELECT 1 FROM C_BankStatement bs"
					+ " INNER JOIN C_PaymentProcessor pp ON (pp.C_BankAccount_ID = bs.C_BankAccount_ID)"
					+ " WHERE bs.C_BankStatement_ID = C_PaymentProcessorRun.C_BankStatement_ID"
					+ " AND pp.C_PaymentProcessor_ID = ?)");
				parameters.add(request.getPaymentProcessorId());
			}
		}
		if (!Util.isEmpty(request.getStatus(), true)) {
			whereClause.append(" AND ResponseStatus = ?");
			parameters.add(request.getStatus().trim());
		}
		if (request.hasDateFrom()) {
			Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(request.getDateFrom());
			if (dateFrom != null) {
				whereClause.append(" AND Created >= ?");
				parameters.add(TimeUtil.getDay(dateFrom));
			}
		}
		if (request.hasDateTo()) {
			Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(request.getDateTo());
			if (dateTo != null) {
				whereClause.append(" AND Created < ?");
				parameters.add(TimeUtil.addDays(TimeUtil.getDay(dateTo), 1));
			}
		}
		Query query = new Query(
			Env.getCtx(),
			TABLE_PAYMENT_PROCESSOR_RUN,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOrderBy("Created DESC, C_PaymentProcessorRun_ID DESC")
		;
		int count = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		ListOnlineCashClosingsResponse.Builder builder = ListOnlineCashClosingsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(nexPageToken)
			)
		;
		query.setLimit(limit, offset)
			.list()
			.forEach(paymentProcessorRun -> builder.addOnlineCashClosings(
				convertOnlineCashClosing(paymentProcessorRun)
			));
		return builder;
	}

	private static OnlineCashClosing.Builder convertOnlineCashClosing(PO paymentProcessorRun) {
		OnlineCashClosing.Builder builder = OnlineCashClosing.newBuilder();
		String status = paymentProcessorRun.get_ValueAsString(COLUMN_RESPONSE_STATUS);
		int paymentMethodId = paymentProcessorRun.get_ValueAsInt(COLUMN_PAYMENT_METHOD_ID);
		builder
			.setId(
				paymentProcessorRun.get_ID()
			)
			.setCreated(
				TimeManager.getProtoTimestampFromTimestamp(
					paymentProcessorRun.getCreated()
				)
			)
			.setPaymentMethodId(paymentMethodId)
			.setPaymentMethodName(
				TextManager.getValidString(
					getPaymentMethodName(paymentMethodId)
				)
			)
			.setStatus(
				TextManager.getValidString(status)
			)
			.setMessage(
				TextManager.getValidString(
					paymentProcessorRun.get_ValueAsString(COLUMN_RESPONSE_MESSAGE)
				)
			)
			.setIsError(
				isErrorStatus(status)
			)
		;
		int bankStatementId = paymentProcessorRun.get_ValueAsInt(COLUMN_BANK_STATEMENT_ID);
		int bankAccountId = 0;
		if (bankStatementId > 0) {
			MBankStatement bankStatement = new MBankStatement(Env.getCtx(), bankStatementId, null);
			bankAccountId = bankStatement.getC_BankAccount_ID();
			builder
				.setBankStatementId(bankStatementId)
				.setBankStatementDocumentNo(
					TextManager.getValidString(
						bankStatement.getDocumentNo()
					)
				)
				.setStatementDate(
					TimeManager.getProtoTimestampFromTimestamp(
						bankStatement.getStatementDate()
					)
				)
			;
		}
		MPaymentProcessor paymentProcessor = getPaymentProcessorOfRun(paymentProcessorRun, bankAccountId, null);
		if (paymentProcessor != null) {
			builder
				.setPaymentProcessorId(
					paymentProcessor.getC_PaymentProcessor_ID()
				)
				.setPaymentProcessorName(
					TextManager.getValidString(
						paymentProcessor.getName()
					)
				)
			;
		}
		return builder;
	}

	/**
	 * List cash closings (bank statements) of POS, ordered by date (last first)
	 * @param request
	 * @return
	 */
	public static ListCashClosingsResponse.Builder listCashClosings(ListCashClosingsRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		List<Object> parameters = new ArrayList<>();
		StringBuilder whereClause = new StringBuilder("C_POS_ID = ?");
		parameters.add(pos.getC_POS_ID());
		int cashClosingDocumentTypeId = pos.get_ValueAsInt("POSCashClosingDocumentType_ID");
		if (cashClosingDocumentTypeId > 0) {
			whereClause.append(" AND C_DocType_ID = ?");
			parameters.add(cashClosingDocumentTypeId);
		}
		if (request.getIsOnlyProcessed()) {
			whereClause.append(" AND Processed = 'Y'");
		}
		if (request.hasDateFrom()) {
			Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(request.getDateFrom());
			if (dateFrom != null) {
				whereClause.append(" AND StatementDate >= ?");
				parameters.add(TimeUtil.getDay(dateFrom));
			}
		}
		if (request.hasDateTo()) {
			Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(request.getDateTo());
			if (dateTo != null) {
				whereClause.append(" AND StatementDate < ?");
				parameters.add(TimeUtil.addDays(TimeUtil.getDay(dateTo), 1));
			}
		}
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause.append(" AND (UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' OR UPPER(Name) LIKE '%' || UPPER(?) || '%')");
			parameters.add(request.getSearchValue().trim());
			parameters.add(request.getSearchValue().trim());
		}
		Query query = new Query(
			Env.getCtx(),
			MBankStatement.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy("StatementDate DESC, C_BankStatement_ID DESC")
		;
		int count = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		ListCashClosingsResponse.Builder builder = ListCashClosingsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(nexPageToken)
			)
		;
		query.setLimit(limit, offset)
			.<MBankStatement>list()
			.forEach(bankStatement -> {
				CashMovements.Builder cashClosing = CashConvertUtil.convertCashMovements(bankStatement);
				builder.addCashClosings(cashClosing);
			});
		return builder;
	}

	/**
	 * Get run table
	 * @return
	 */
	public static MTable getRunTable() {
		MTable processorRunTable = MTable.get(Env.getCtx(), TABLE_PAYMENT_PROCESSOR_RUN);
		if (processorRunTable == null || processorRunTable.get_ID() <= 0) {
			throw new AdempiereException("@M_Table_ID@: C_PaymentProcessorRun @NotFound@");
		}
		return processorRunTable;
	}
}
