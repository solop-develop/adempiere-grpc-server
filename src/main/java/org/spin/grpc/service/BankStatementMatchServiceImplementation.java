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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_BankAccount;
import org.adempiere.core.domains.models.I_C_Currency;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.I_I_BankStatement;
import org.adempiere.core.domains.models.X_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCurrency;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
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
import org.spin.backend.grpc.form.bank_statement_match.BankStatementMatchGrpc.BankStatementMatchImplBase;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

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

	public static MBankAccount validateAndGetBankAccount(int bankAccountId) {
		if (bankAccountId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BankAccount_ID@");
		}
		MBankAccount bankAccount = new Query(
			Env.getCtx(),
			I_C_BankAccount.Table_Name,
			" C_BankAccount_ID = ? ",
			null
		)
			.setParameters(bankAccountId)
			.setClient_ID()
			.first();
		if (bankAccount == null || bankAccount.getC_BankAccount_ID() <= 0) {
			throw new AdempiereException("@C_BankAccount_ID@ @NotFound@");
		}
		return bankAccount;
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


	public static BusinessPartner.Builder convertBusinessPartner(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			return BusinessPartner.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertBusinessPartner(businessPartner);
	}
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}

		builder.setId(businessPartner.getC_BPartner_ID())
			.setUuid(
				ValueUtil.validateNull(businessPartner.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(businessPartner.getValue())
			)
			.setTaxId(
				ValueUtil.validateNull(businessPartner.getTaxID())
			)
			.setName(
				ValueUtil.validateNull(businessPartner.getName())
			)
			.setDescription(
				ValueUtil.validateNull(businessPartner.getDescription())
			)
		;

		return builder;
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



	public static Currency.Builder convertCurrency(String isoCode) {
		if (Util.isEmpty(isoCode, true)) {
			return Currency.newBuilder();
		}
		MCurrency currency = MCurrency.get(Env.getCtx(), isoCode);
		return convertCurrency(currency);
	}
	public static Currency.Builder convertCurrency(int currencyId) {
		if (currencyId <= 0) {
			return Currency.newBuilder();
		}
		MCurrency currency = MCurrency.get(Env.getCtx(), currencyId);
		return convertCurrency(currency);
	}
	public static Currency.Builder convertCurrency(MCurrency currency) {
		Currency.Builder builder = Currency.newBuilder();
		if (currency == null || currency.getC_Currency_ID() <= 0) {
			return builder;
		}

		builder.setId(currency.getC_Currency_ID())
			.setUuid(
				ValueUtil.validateNull(currency.getUUID())
			)
			.setIsoCode(
				ValueUtil.validateNull(currency.getISO_Code())
			)
			.setDescription(
				ValueUtil.validateNull(currency.getDescription())
			)
		;

		return builder;
	}



	public static TenderType.Builder convertTenderType(String value) {
		if (Util.isEmpty(value, false)) {
			return TenderType.newBuilder();
		}

		MRefList tenderType = MRefList.get(Env.getCtx(), X_C_Payment.TENDERTYPE_AD_Reference_ID, value, null);
		return convertTenderType(tenderType);
	}
	public static TenderType.Builder convertTenderType(MRefList tenderType) {
		TenderType.Builder builder = TenderType.newBuilder();
		if (tenderType == null || tenderType.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = tenderType.getName();
		String description = tenderType.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = tenderType.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = tenderType.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(tenderType.getAD_Ref_List_ID())
			.setUuid(ValueUtil.validateNull(tenderType.getUUID()))
			.setValue(ValueUtil.validateNull(tenderType.getValue()))
			.setName(
				ValueUtil.validateNull(name)
			)
			.setDescription(
				ValueUtil.validateNull(description)
			)
		;

		return builder;
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
		Properties context = Env.getCtx();

		// validate and get Bank Account
		MBankAccount bankAccount = validateAndGetBankAccount(request.getBankAccountId());

		StringBuffer sql = new StringBuffer(
			"SELECT p.I_BankStatement_ID, p.UUID, p.StatementLineDate, "
			+ "(CASE WHEN p.TrxAmt < 0 THEN 'N' ELSE 'Y' END) AS IsReceipt, "
			+ "p.ReferenceNo, p.C_BPartner_ID, "
			+ "(CASE WHEN p.C_BPartner_ID IS NULL THEN BPartnerValue ELSE bp.Name END) BPName, "
			+ "COALESCE(p.ISO_Code, c.ISO_Code) AS ISO_Code, p.TrxAmt, p.Memo "
			+ "FROM I_BankStatement p "
			+ "LEFT JOIN C_BPartner bp ON(bp.C_BPartner_ID = p.C_BPartner_ID) "
			+ "LEFT JOIN C_Currency c ON(c.C_Currency_ID = p.C_Currency_ID) "
		);
		//	Where Clause
		sql.append("WHERE p.C_BankAccount_ID = ? ");

		//	Match
		boolean isMatchedMode = request.getMatchMode() == MatchMode.MODE_MATCHED;
		if (isMatchedMode) {
			sql.append(
				"AND (p.C_Payment_ID IS NOT NULL "
				+ "OR p.C_BPartner_ID IS NOT NULL "
				+ "OR p.C_Invoice_ID IS NOT NULL) "
			);
		} else {
			sql.append(
				"AND (p.C_Payment_ID IS NULL "
				+ "AND p.C_BPartner_ID IS NULL "
				+ "AND p.C_Invoice_ID IS NULL) "
			);
		}

		//	For parameters
		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		if (dateFrom != null) {
			sql.append("AND p.StatementLineDate >= ? ");
		}
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);
		if (dateTo != null) {
			sql.append("AND p.StatementLineDate <= ? ");
		}
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		//	Amount
		if (paymentAmountFrom != null) {
			sql.append("AND p.TrxAmt >= ? ");
		}
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);
		if (paymentAmountTo != null) {
			sql.append("AND p.TrxAmt <= ? ");
		}
		// role security
		sql = new StringBuffer(
			MRole.getDefault(context, false).addAccessSQL(
				sql.toString(),
				"p",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			)
		);

		//	Order by
		sql.append("ORDER BY p.StatementLineDate");

		ListImportedBankMovementsResponse.Builder builderList = ListImportedBankMovementsResponse.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), null);

			int argumentPosition = 1;
			pstmt.setInt(argumentPosition++, bankAccount.getC_BankAccount_ID());
			if (dateFrom != null) {
				pstmt.setTimestamp(argumentPosition++, dateFrom);
			}
			if (dateTo != null) {
				pstmt.setTimestamp(argumentPosition++, dateTo);
			}
			if (paymentAmountFrom != null) {
				pstmt.setBigDecimal(argumentPosition++, paymentAmountFrom);
			}
			if (paymentAmountTo != null) {
				pstmt.setBigDecimal(argumentPosition++, paymentAmountTo);
			}
			rs = pstmt.executeQuery();
			int recordCount = 0;
			while (rs.next()) {
				recordCount++;

				int businessPartnerId = rs.getInt(I_I_BankStatement.COLUMNNAME_C_BPartner_ID);
				BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
					businessPartnerId
				);
				if (businessPartnerId <= 0) {
					businessPartnerBuilder.setName(
						ValueUtil.validateNull(
							rs.getString("BPName")
						)
					);
				}

				Currency.Builder currencyBuilder = convertCurrency(
					rs.getString(I_C_Currency.COLUMNNAME_ISO_Code)
				);

				ImportedBankMovement.Builder importedBuilder = ImportedBankMovement.newBuilder()
					.setId(
						rs.getInt(I_I_BankStatement.COLUMNNAME_C_BankStatement_ID)
					)
					.setUuid(
						ValueUtil.validateNull(
							rs.getString(I_I_BankStatement.COLUMNNAME_UUID)
						)
					)
					.setTransactionDate(
						ValueUtil.getLongFromTimestamp(
							rs.getTimestamp(I_I_BankStatement.COLUMNNAME_StatementLineDate)
						)
					)
					.setIsReceipt(
						ValueUtil.stringToBoolean(
							rs.getString("IsReceipt")
						)
					)
					.setReferenceNo(
						ValueUtil.validateNull(
							rs.getString(I_I_BankStatement.COLUMNNAME_ReferenceNo)
						)
					)
					.setAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal(I_I_BankStatement.COLUMNNAME_TrxAmt)
						)
					)
					.setMemo(
						ValueUtil.validateNull(
							rs.getString(I_I_BankStatement.COLUMNNAME_Memo)
						)
					)
					.setBusinessPartner(businessPartnerBuilder)
					.setCurrency(currencyBuilder)
				;
				builderList.addRecords(importedBuilder);
			}
			builderList.setRecordCount(recordCount);

		}
		catch (SQLException e) {
			throw new AdempiereException(e.getLocalizedMessage());
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

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
		MBankAccount bankAccount = validateAndGetBankAccount(request.getBankAccountId());

		Properties context = Env.getCtx();

		//	Dynamic where clause
		List<Object> parameters = new ArrayList<Object>();
		String whereClause = "C_BankAccount_ID = ? "
			+ " AND DocStatus NOT IN('IP', 'DR') "
			+ " AND IsReconciled = 'N' "
		;
		parameters.add(bankAccount.getC_BankAccount_ID());

		//	Date Trx
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(
			request.getTransactionDateFrom()
		);
		if (dateFrom != null) {
			whereClause += "AND DateTrx >= ? ";
			parameters.add(dateFrom);
		}
		Timestamp dateTo = ValueUtil.getTimestampFromLong(
			request.getTransactionDateTo()
		);
		if (dateTo != null) {
			whereClause += "AND DateTrx <= ? ";
			parameters.add(dateTo);
		}
		//	Amount
		BigDecimal paymentAmountFrom = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountFrom()
		);
		if (paymentAmountFrom != null) {
			whereClause += "AND PayAmt >= ? ";
			parameters.add(paymentAmountFrom);
		}
		BigDecimal paymentAmountTo = ValueUtil.getBigDecimalFromDecimal(
			request.getPaymentAmountTo()
		);
		if (paymentAmountTo != null) {
			whereClause += "AND PayAmt <= ? ";
			parameters.add(paymentAmountTo);
		}
		//	for BP
		int businessPartnerIdQuery = request.getBusinessPartnerId();
		if (businessPartnerIdQuery > 0) {
			whereClause += "AND C_BPartner_ID = ? ";
			parameters.add(businessPartnerIdQuery);
		}

		Query query = new Query(
			context,
			I_C_Payment.Table_Name,
			whereClause,
			null
		)
			.setParameters(parameters)
			.setClient_ID()
		;

		int count = query.count();
		String nexPageToken = "";
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListPaymentsResponse.Builder builderList = ListPaymentsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list(MPayment.class)
			.forEach(payment -> {
				BusinessPartner.Builder businessPartnerBuilder = convertBusinessPartner(
					payment.getC_BPartner_ID()
				);

				Currency.Builder currencyBuilder = convertCurrency(
					payment.getC_Currency_ID()
				);

				TenderType.Builder tenderTypeBuilder = convertTenderType(
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
			ListMatchingMovementsResponse.Builder builder = listMatchingMovements(request);
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

	private ListMatchingMovementsResponse.Builder listMatchingMovements(ListMatchingMovementsRequest request) {
		// validate and get Bank Account
		MBankAccount bankAccount = validateAndGetBankAccount(request.getBankAccountId());

		ListMatchingMovementsResponse.Builder builderList = ListMatchingMovementsResponse.newBuilder();

		return builderList;
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
