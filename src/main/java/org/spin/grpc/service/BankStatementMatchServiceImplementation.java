/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
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
package org.spin.grpc.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.X_I_BankStatement;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBankAccount;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MPayment;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.form.bank_statement_match.BankStatementMatchGrpc.BankStatementMatchImplBase;
import org.spin.backend.grpc.form.bank_statement_match.BusinessPartner;
import org.spin.backend.grpc.form.bank_statement_match.Currency;
import org.spin.backend.grpc.form.bank_statement_match.ImportedBankMovement;
import org.spin.backend.grpc.form.bank_statement_match.ListBankAccountsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListBusinessPartnersRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListImportedBankMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListMatchingMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ListPaymentsResponse;
import org.spin.backend.grpc.form.bank_statement_match.ListSearchModesRequest;
import org.spin.backend.grpc.form.bank_statement_match.MatchMode;
import org.spin.backend.grpc.form.bank_statement_match.Payment;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsRequest;
import org.spin.backend.grpc.form.bank_statement_match.ProcessMovementsResponse;
import org.spin.backend.grpc.form.bank_statement_match.TenderType;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;
import org.spin.form.bank_statement_match.BankStatementMatchConvertUtil;
import org.spin.form.bank_statement_match.BankStatementMatchServiceLogic;
import org.spin.form.bank_statement_match.BankStatementMatchUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Bank Statement Match form
 */
public class BankStatementMatchServiceImplementation extends BankStatementMatchImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(BankStatementMatchServiceImplementation.class);



	@Override
	public void listBankAccounts(ListBankAccountsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listBankAccounts(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListLookupItemsResponse.Builder listBankAccounts(ListBankAccountsRequest request) {
		// Bank Account
		int columnId = 4917; // C_BankStatement.C_BankAccount_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listBusinessPartners(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListLookupItemsResponse.Builder listBusinessPartners(ListBusinessPartnersRequest request) {
		// Business Partner
		int columnId = 3499; // C_Invoice.C_BPartner_ID
		String columnUuid = RecordUtil.getUuidFromId(I_AD_Column.Table_Name, columnId);
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			null,
			null, null, null,
			columnUuid,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void listSearchModes(ListSearchModesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listSearchModes(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListLookupItemsResponse.Builder listSearchModes(ListSearchModesRequest request) {
		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		LookupItem.Builder lookupMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_NOT_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(Env.getCtx(), "NotMatched")
				).build())
		;
		builderList.addRecords(lookupMatched);

		LookupItem.Builder lookupUnMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(Env.getCtx(), "Matched")
				).build())
		;
		builderList.addRecords(lookupUnMatched);

		return builderList;
	}



	@Override
	public void listImportedBankMovements(ListImportedBankMovementsRequest request, StreamObserver<ListImportedBankMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListImportedBankMovementsResponse.Builder builder = listImportedBankMovements(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListImportedBankMovementsResponse.Builder listImportedBankMovements(ListImportedBankMovementsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		ArrayList<Object> filterParameters = new ArrayList<Object>();
		filterParameters.add(bankAccount.getC_BankAccount_ID());

		//	For parameters
		boolean isMatchedMode = request.getMatchMode() == MatchMode.MODE_MATCHED;

		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);
		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);

		Query importMovementsQuery = BankStatementMatchUtil.buildBankMovementQuery(
			bankAccount.getC_BankAccount_ID(),
			isMatchedMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo
		);
		List<Integer> importedBankMovementsId = importMovementsQuery
			// .setLimit(0, 0)
			.getIDsAsList()
		;

		ListImportedBankMovementsResponse.Builder builderList = ListImportedBankMovementsResponse.newBuilder()
			.setRecordCount(importMovementsQuery.count())
		;

		importedBankMovementsId.forEach(bankStatementId -> {
			X_I_BankStatement currentBankStatementImport = new X_I_BankStatement(Env.getCtx(), bankStatementId, null);
			ImportedBankMovement.Builder importedBuilder = BankStatementMatchConvertUtil.convertImportedBankMovement(currentBankStatementImport);
			builderList.addRecords(importedBuilder);
		});

		return builderList;
	}

	@Override
	public void listPayments(ListPaymentsRequest request, StreamObserver<ListPaymentsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPaymentsResponse.Builder builder = listPayments(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListPaymentsResponse.Builder listPayments(ListPaymentsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = BankStatementMatchUtil.validateAndGetBankAccount(request.getBankAccountId());

		boolean isMatchedMode = request.getMatchMode() == MatchMode.MODE_MATCHED;
		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);
		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);

		Query query = BankStatementMatchUtil.buildPaymentQuery(
			bankAccount.getC_BankAccount_ID(),
			isMatchedMode,
			dateFrom,
			dateTo,
			paymentAmountFrom,
			paymentAmountTo,
			request.getBusinessPartnerId()
		);

		int count = query.count();
		String nexPageToken = "";
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListPaymentsResponse.Builder builderList = ListPaymentsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(paymentId -> {
				MPayment payment = new MPayment(
					Env.getCtx(),
					paymentId,
					null
				);

				BusinessPartner.Builder businessPartnerBuilder = BankStatementMatchConvertUtil.convertBusinessPartner(
					payment.getC_BPartner_ID()
				);

				Currency.Builder currencyBuilder = BankStatementMatchConvertUtil.convertCurrency(
					payment.getC_Currency_ID()
				);

				TenderType.Builder tenderTypeBuilder = BankStatementMatchConvertUtil.convertTenderType(
					payment.getTenderType()
				);
				boolean isReceipt = payment.isReceipt();
				BigDecimal paymentAmount = payment.getPayAmt();
				if (!isReceipt) {
					paymentAmount = paymentAmount.negate();
				}

				Payment.Builder paymentBuilder = Payment.newBuilder()
					.setId(
						payment.getC_Payment_ID()
					)
					.setUuid(
						ValueUtil.validateNull(
							payment.getUUID()
						)
					)
					.setTransactionDate(
						ValueUtil.getLongFromTimestamp(
							payment.getDateTrx()
						)
					)
					.setIsReceipt(isReceipt)
					.setDocumentNo(
						ValueUtil.validateNull(
							payment.getDocumentNo()
						)
					)
					.setDescription(
						ValueUtil.validateNull(
							payment.getDescription()
						)
					)
					.setAmount(
						ValueUtil.getDecimalFromBigDecimal(
							paymentAmount
						)
					)
					.setBusinessPartner(businessPartnerBuilder)
					.setTenderType(tenderTypeBuilder)
					.setCurrency(currencyBuilder)
				;

				builderList.addRecords(paymentBuilder);
			});
		;

		return builderList;
	}


	@Override
	public void listMatchingMovements(ListMatchingMovementsRequest request, StreamObserver<ListMatchingMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListMatchingMovementsResponse.Builder builder = BankStatementMatchServiceLogic.listMatchingMovements(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}



	@Override
	public void processMovements(ProcessMovementsRequest request, StreamObserver<ProcessMovementsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ProcessMovementsResponse.Builder builder = processMovements(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ProcessMovementsResponse.Builder processMovements(ProcessMovementsRequest request) {
		ProcessMovementsResponse.Builder builderList = ProcessMovementsResponse.newBuilder();

		return builderList;
	}

}
