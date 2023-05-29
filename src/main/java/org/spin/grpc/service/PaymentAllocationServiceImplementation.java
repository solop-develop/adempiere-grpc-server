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

import org.adempiere.exceptions.AdempiereException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Org;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_Currency;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.X_T_InvoiceGL;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MCharge;
import org.compiere.model.MCurrency;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MOrg;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.payment_allocation.Charge;
import org.spin.backend.grpc.form.payment_allocation.Currency;
import org.spin.backend.grpc.form.payment_allocation.Invoice;
import org.spin.backend.grpc.form.payment_allocation.InvoiceSelection;
import org.spin.backend.grpc.form.payment_allocation.ListBusinessPartnersRequest;
import org.spin.backend.grpc.form.payment_allocation.ListChargesRequest;
import org.spin.backend.grpc.form.payment_allocation.ListCurrenciesRequest;
import org.spin.backend.grpc.form.payment_allocation.ListInvoicesRequest;
import org.spin.backend.grpc.form.payment_allocation.ListInvoicesResponse;
import org.spin.backend.grpc.form.payment_allocation.ListOrganizationsRequest;
import org.spin.backend.grpc.form.payment_allocation.ListPaymentsRequest;
import org.spin.backend.grpc.form.payment_allocation.ListPaymentsResponse;
import org.spin.backend.grpc.form.payment_allocation.ListTransactionOrganizationsRequest;
import org.spin.backend.grpc.form.payment_allocation.ListTransactionTypesRequest;
import org.spin.backend.grpc.form.payment_allocation.Organization;
import org.spin.backend.grpc.form.payment_allocation.Payment;
import org.spin.backend.grpc.form.payment_allocation.PaymentSelection;
import org.spin.backend.grpc.form.payment_allocation.ProcessRequest;
import org.spin.backend.grpc.form.payment_allocation.ProcessResponse;
import org.spin.backend.grpc.form.payment_allocation.TransactionType;
import org.spin.backend.grpc.form.payment_allocation.PaymentAllocationGrpc.PaymentAllocationImplBase;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Payment Allocation form
 */
public class PaymentAllocationServiceImplementation extends PaymentAllocationImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PaymentAllocationServiceImplementation.class);



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
		// BPartner
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
	public void listOrganizations(ListOrganizationsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listOrganizations(request);
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

	/**
	 * @param request
	 * @return
	 */
	private ListLookupItemsResponse.Builder listOrganizations(ListOrganizationsRequest request) {
		// Organization filter selection
		int columnId = 839; // C_Period.AD_Org_ID (needed to allow org 0)
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

	public static Organization.Builder convertOrganization(int organizationId) {
		if (organizationId < 0) {
			return Organization.newBuilder();
		}
		MOrg organization = MOrg.get(Env.getCtx(), organizationId);
		return convertOrganization(organization);
	}
	public static Organization.Builder convertOrganization(MOrg organization) {
		Organization.Builder builder = Organization.newBuilder();
		if (organization == null || organization.getAD_Org_ID() < 0) {
			return builder;
		}

		builder.setId(organization.getAD_Org_ID())
			.setUuid(
				ValueUtil.validateNull(organization.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(organization.getName())
			)
			.setName(
				ValueUtil.validateNull(organization.getName())
			)
		;

		return builder;
	}



	@Override
	public void listCurrencies(ListCurrenciesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listCurrencies(request);
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

	private ListLookupItemsResponse.Builder listCurrencies(ListCurrenciesRequest request) {
		// Currency
		int columnId = 3505; // C_Invoice.C_Currency_ID
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



	@Override
	public void listTransactionTypes(ListTransactionTypesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listTransactionTypes(request);
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

	private ListLookupItemsResponse.Builder listTransactionTypes(ListTransactionTypesRequest request) {
		// APAR
		int columnId = 14082; // T_InvoiceGL.APAR
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


	public static TransactionType.Builder convertTransactionType(String value) {
		if (Util.isEmpty(value, false)) {
			return TransactionType.newBuilder();
		}

		MRefList transactionType = MRefList.get(Env.getCtx(), X_T_InvoiceGL.APAR_AD_Reference_ID, value, null);
		return convertTransactionType(transactionType);
	}
	public static TransactionType.Builder convertTransactionType(MRefList transactionType) {
		TransactionType.Builder builder = TransactionType.newBuilder();
		if (transactionType == null || transactionType.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		String name = transactionType.getName();
		String description = transactionType.getDescription();

		// set translated values
		if (!Env.isBaseLanguage(Env.getCtx(), "")) {
			name = transactionType.get_Translation(I_AD_Ref_List.COLUMNNAME_Name);
			description = transactionType.get_Translation(I_AD_Ref_List.COLUMNNAME_Description);
		}

		builder.setId(transactionType.getAD_Ref_List_ID())
			.setUuid(ValueUtil.validateNull(transactionType.getUUID()))
			.setValue(ValueUtil.validateNull(transactionType.getValue()))
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
		Properties context = Env.getCtx();
		int currencyId = request.getCurrencyId();
		int businessPartnerId = request.getBusinessPartnerId();
		Timestamp date = ValueUtil.getTimestampFromLong(
			request.getDate()
		);

		/**
		 *  Load unallocated Payments
		 *    1-TrxDate, 2-DocumentNo, (3-Currency, 4-PayAmt,)
		 *    5-ConvAmt, 6-ConvOpen, 7-Allocated
		 */
		StringBuffer sql = new StringBuffer("SELECT p.DateTrx, p.DocumentNo, p.C_Payment_ID,"	//	1..3
			+ "c.ISO_Code, p.PayAmt,"			//	4..5
			+ "currencyConvert(p.PayAmt, p.C_Currency_ID, ?, ?, p.C_ConversionType_ID, p.AD_Client_ID, p.AD_Org_ID) AS ConvertedAmt,"//		#1, #2
			+ "currencyConvert(paymentAvailable(C_Payment_ID), p.C_Currency_ID, ?, ?, p.C_ConversionType_ID, p.AD_Client_ID, p.AD_Org_ID) AS AvailableAmt,"	//	7   #3, #4
			+ "p.MultiplierAP, p.IsReceipt, p.AD_Org_ID, p.Description " //	8..11
			+ "FROM C_Payment_v p "		//	Corrected for AP/AR
			+ "INNER JOIN C_Currency c ON (p.C_Currency_ID=c.C_Currency_ID) "
			+ "WHERE p.IsAllocated='N' AND p.Processed='Y' "
			+ "AND p.C_Charge_ID IS NULL "			//	Prepayments OK
			+ "AND p.C_BPartner_ID = ? "			//	#5
		);

		if (!request.getIsMultiCurrency()) {
			sql.append(" AND p.C_Currency_ID=?");				//      #6
		}
		if (request.getOrganizationId() > 0 ) {
			sql.append(" AND p.AD_Org_ID = " + request.getOrganizationId());
		}

		String transactionType = request.getTransactionType();
		if (!Util.isEmpty(transactionType, true) && !transactionType.equals(X_T_InvoiceGL.APAR_ReceivablesPayables)) {
			String isReceipt = ValueUtil.booleanToString(
				transactionType.equals(X_T_InvoiceGL.APAR_ReceivablesOnly)
			);
			sql.append(" AND p.IsReceipt= '" + isReceipt +"'" );
		}
		sql.append(" ORDER BY p.DateTrx,p.DocumentNo");

		// role security
		sql = new StringBuffer(
			MRole.getDefault(context, false).addAccessSQL(
				sql.toString(),
				"p",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			)
		);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ListPaymentsResponse.Builder builderList = ListPaymentsResponse.newBuilder();
		try {
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, currencyId);
			pstmt.setTimestamp(2, date);
			pstmt.setInt(3, currencyId);
			pstmt.setTimestamp(4, date);
			pstmt.setInt(5, businessPartnerId);
			if (!request.getIsMultiCurrency()) {
				pstmt.setInt(6, currencyId);
			}
			rs = pstmt.executeQuery();
			int recordCount = 0;
			while (rs.next()) {
				recordCount++;

				Organization.Builder organizationBuilder = convertOrganization(
					rs.getInt(I_AD_Org.COLUMNNAME_AD_Org_ID)
				);

				Currency.Builder currencyBuilder = convertCurrency(
					rs.getString(I_C_Currency.COLUMNNAME_ISO_Code)
				);

				boolean isReceipt = ValueUtil.stringToBoolean(
					rs.getString("IsReceipt")
				);
				TransactionType.Builder transactionTypeBuilder = convertTransactionType(
					isReceipt ? X_T_InvoiceGL.APAR_ReceivablesOnly : X_T_InvoiceGL.APAR_PayablesOnly
				);

				int paymentId = rs.getInt(I_C_Payment.COLUMNNAME_C_Payment_ID);
				String paymentUuid = RecordUtil.getUuidFromId(I_C_Payment.Table_Name, paymentId, null);

				Payment.Builder paymentBuilder = Payment.newBuilder()
					.setId(paymentId)
					.setUuid(
						ValueUtil.validateNull(paymentUuid)
					)
					.setTransactionDate(
						ValueUtil.getLongFromTimestamp(
							rs.getTimestamp(I_C_Payment.COLUMNNAME_DateTrx)
						)
					)
					.setIsReceipt(isReceipt)
					.setDocumentNo(
						ValueUtil.validateNull(
							rs.getString(I_C_Payment.COLUMNNAME_DocumentNo)
						)
					)
					.setDescription(
						ValueUtil.validateNull(
							rs.getString(I_C_Payment.COLUMNNAME_Description)
						)
					)
					.setPaymentAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal(I_C_Payment.COLUMNNAME_PayAmt)
						)
					)
					.setConvertedAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal("ConvertedAmt")
						)
					)
					.setOpenAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal("AvailableAmt")
						)
					)
					.setTransactionType(transactionTypeBuilder)
					.setOrganization(organizationBuilder)
					.setCurrency(currencyBuilder)
				;

				builderList.addRecords(paymentBuilder);
			}

			builderList.setRecordCount(recordCount);
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	@Override
	public void listInvoices(ListInvoicesRequest request, StreamObserver<ListInvoicesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListInvoicesResponse.Builder builder = listInvoices(request);
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

	private ListInvoicesResponse.Builder listInvoices(ListInvoicesRequest request) {
		Properties context = Env.getCtx();
		int currencyId = request.getCurrencyId();
		int businessPartnerId = request.getBusinessPartnerId();
		Timestamp date = ValueUtil.getTimestampFromLong(
			request.getDate()
		);

		/**
		 *  Load unpaid Invoices
		 *    1-TrxDate, 2-Value, (3-Currency, 4-InvAmt,)
		 *    5-ConvAmt, 6-ConvOpen, 7-ConvDisc, 8-WriteOff, 9-Applied
		 */
		StringBuffer sql = new StringBuffer(
			"SELECT i.DateInvoiced, i.DocumentNo, i.Description, i.C_Invoice_ID, "		//	1..3
			+ "c.ISO_Code, (i.GrandTotal * i.MultiplierAP) AS OriginalAmt, "		//  4..5	Orig Currency
			+ "currencyConvert(i.GrandTotal * i.MultiplierAP, i.C_Currency_ID, ?, ?, i.C_ConversionType_ID, i.AD_Client_ID, i.AD_Org_ID) AS ConvertedAmt, "		//	6   #1  Converted, #2 Date
			+ "(currencyConvert(invoiceOpen(C_Invoice_ID,C_InvoicePaySchedule_ID), i.C_Currency_ID, ?, ?, i.C_ConversionType_ID, i.AD_Client_ID, i.AD_Org_ID) * i.MultiplierAP) AS OpenAmt, "	//  7   #3, #4  Converted Open
			+ "(currencyConvert(invoiceDiscount"		//  8  AllowedDiscount
			+ "(i.C_Invoice_ID, ?, C_InvoicePaySchedule_ID), i.C_Currency_ID, ?, i.DateInvoiced, i.C_ConversionType_ID, i.AD_Client_ID, i.AD_Org_ID) * i.Multiplier * i.MultiplierAP) AS DiscountAmt, "               //  #5, #6
			+ "i.MultiplierAP, i.IsSoTrx, i.AD_Org_ID " // 9..11
			+ "FROM C_Invoice_v i "		//  corrected for CM/Split
			+ "INNER JOIN C_Currency c ON (i.C_Currency_ID=c.C_Currency_ID) "
			+ "WHERE i.IsPaid='N' AND i.Processed='Y' "
			+ "AND i.C_BPartner_ID = ? "		//  #7
		);
		if (!request.getIsMultiCurrency()) {
			sql.append("AND i.C_Currency_ID = ? ");		//  #8
		}
		if (request.getOrganizationId() != 0 ) {
			sql.append(" AND i.AD_Org_ID = " + request.getOrganizationId());
		}

		String transactionType = request.getTransactionType();
		if (!Util.isEmpty(transactionType, true) && !transactionType.equals(X_T_InvoiceGL.APAR_ReceivablesPayables)) {
			String isReceipt = ValueUtil.booleanToString(
				transactionType.equals(X_T_InvoiceGL.APAR_ReceivablesOnly)
			);
			sql.append(" AND i.IsSOTrx= '" + isReceipt +"'" );
		}
		sql.append(" ORDER BY i.DateInvoiced, i.DocumentNo");

		// role security
		sql = new StringBuffer(
			MRole.getDefault(context, false).addAccessSQL(
				sql.toString(),
				"i",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			)
		);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ListInvoicesResponse.Builder builderList = ListInvoicesResponse.newBuilder();
		try {
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, currencyId);
			pstmt.setTimestamp(2, date);
			pstmt.setInt(3, currencyId);
			pstmt.setTimestamp(4, date);
			pstmt.setTimestamp(5, date);
			pstmt.setInt(6, currencyId);
			pstmt.setInt(7, businessPartnerId);
			if (!request.getIsMultiCurrency()) {
				pstmt.setInt(8, currencyId);
			}
			rs = pstmt.executeQuery();
			int recordCount = 0;
			while (rs.next()) {
				recordCount++;

				Organization.Builder organizationBuilder = convertOrganization(
					rs.getInt(I_AD_Org.COLUMNNAME_AD_Org_ID)
				);

				Currency.Builder currencyBuilder = convertCurrency(
					rs.getString(I_C_Currency.COLUMNNAME_ISO_Code)
				);

				boolean isSalesTransaction = ValueUtil.stringToBoolean(
					rs.getString(I_C_Invoice.COLUMNNAME_IsSOTrx)
				);
				TransactionType.Builder transactionTypeBuilder = convertTransactionType(
					isSalesTransaction ? X_T_InvoiceGL.APAR_ReceivablesOnly : X_T_InvoiceGL.APAR_PayablesOnly
				);

				int invoiceId = rs.getInt(I_C_Invoice.COLUMNNAME_C_Invoice_ID);
				String invoiceUuid = RecordUtil.getUuidFromId(I_C_Invoice.Table_Name, invoiceId, null);
				Invoice.Builder invoiceBuilder = Invoice.newBuilder()
					.setId(invoiceId)
					.setUuid(
						ValueUtil.validateNull(invoiceUuid)
					)
					.setDateInvoiced(
						ValueUtil.getLongFromTimestamp(
							rs.getTimestamp(I_C_Invoice.COLUMNNAME_DateInvoiced)
						)
					)
					.setIsSalesTransaction(isSalesTransaction)
					.setDocumentNo(
						ValueUtil.validateNull(
							rs.getString(I_C_Invoice.COLUMNNAME_DocumentNo)
						)
					)
					.setDescription(
						ValueUtil.validateNull(
							rs.getString(I_C_Invoice.COLUMNNAME_Description)
						)
					)
					.setOriginalAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal("OriginalAmt")
						)
					)
					.setConvertedAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal("ConvertedAmt")
						)
					)
					.setOpenAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal("OpenAmt")
						)
					)
					.setDiscountAmount(
						ValueUtil.getDecimalFromBigDecimal(
							rs.getBigDecimal("DiscountAmt")
						)
					)
					.setTransactionType(transactionTypeBuilder)
					.setOrganization(organizationBuilder)
					.setCurrency(currencyBuilder)
				;

				builderList.addRecords(invoiceBuilder);
			}

			builderList.setRecordCount(recordCount);
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	@Override
	public void listCharges(ListChargesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listCharges(request);
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

	private ListLookupItemsResponse.Builder listCharges(ListChargesRequest request) {
		// Charge
		int columnId = 61804; // C_AllocationLine.C_Charge_ID
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
	
	public static Charge.Builder convertCharge(MCharge charge) {
		Charge.Builder builder = Charge.newBuilder();
		if (charge == null || charge.getC_Charge_ID() <= 0) {
			return builder;
		}

		builder.setId(charge.getC_Charge_ID())
			.setUuid(
				ValueUtil.validateNull(charge.getUUID())
			)
			.setName(
				ValueUtil.validateNull(charge.getName())
			)
			.setDescription(
				ValueUtil.validateNull(charge.getDescription())
			)
			.setAmount(
				ValueUtil.getDecimalFromBigDecimal(charge.getChargeAmt())
			)
		;

		return builder;
	}



	@Override
	public void listTransactionOrganizations(ListTransactionOrganizationsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builder = listTransactionOrganizations(request);
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

	private ListLookupItemsResponse.Builder listTransactionOrganizations(ListTransactionOrganizationsRequest request) {
		// Organization to overwrite
		int columnId = 3863; // C_Period.AD_Org_ID
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
	public void process(ProcessRequest request, StreamObserver<ProcessResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessResponse.Builder builder = process(request);
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

	private ProcessResponse.Builder process(ProcessRequest request) {
		Properties context = Env.getCtx();
		if (request.getTransactionOrganizationId() <= 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}

		AtomicReference<String> atomicStatus = new AtomicReference<String>();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		Env.setContext(context, windowNo, I_AD_Org.COLUMNNAME_AD_Org_ID, request.getTransactionOrganizationId());

		Trx.run(transactionName -> {
			// transaction date
			Timestamp transactionDate = ValueUtil.getTimestampFromLong(
				request.getDate()
			);
			if (transactionDate == null) {
				transactionDate = getTransactionDate(
					request.getPaymentSelectionsList(),
					request.getInvoiceSelectionsList()
				);
			}

			String status = saveData(
				windowNo, request.getCurrencyId(), request.getTransactionOrganizationId(), transactionDate,
				request.getDescription(),
				request.getPaymentSelectionsList(),
				request.getInvoiceSelectionsList(),
				transactionName
			);
			atomicStatus.set(status);
		});

		return ProcessResponse.newBuilder()
			.setMessage(
				ValueUtil.validateNull(
					atomicStatus.get()
				)
			)
		;
	}

	private Timestamp getTransactionDate(List<PaymentSelection> paymentSelection, List<InvoiceSelection> invoiceSelection) {
		AtomicReference<Timestamp> transactionDateReference = new AtomicReference<Timestamp>();
		paymentSelection.forEach(paymentSelected -> {
			Timestamp paymentDate = ValueUtil.getTimestampFromLong(
				paymentSelected.getTransactionDate()
			);
			Timestamp transactionDate = TimeUtil.max(transactionDateReference.get(), paymentDate);
			transactionDateReference.set(transactionDate);
		});
		invoiceSelection.forEach(invoiceSelected -> {
			Timestamp invoiceDate = ValueUtil.getTimestampFromLong(
				invoiceSelected.getDateInvoiced()
			);
			Timestamp transactionDate = TimeUtil.max(transactionDateReference.get(), invoiceDate);
			transactionDateReference.set(transactionDate);
		});

		return transactionDateReference.get();
	}

	private String saveData(
		int windowNo, int currencyId,
		int organizationId,
		Timestamp transactionDate,
		String description,List<PaymentSelection> paymentSelection,
		List<InvoiceSelection> invoiceSelection,
		String transactionName
	) {
		if (paymentSelection == null || invoiceSelection == null || (paymentSelection.size() + invoiceSelection.size() == 0)) {
			return "";
		}

		String userName = Env.getContext(Env.getCtx(), "#AD_User_Name");
		//	Create Allocation manual
		MAllocationHdr alloc = new MAllocationHdr(
			Env.getCtx(), true, transactionDate, currencyId, userName, transactionName
		);
		alloc.setAD_Org_ID(organizationId);
		//	Set Description
		if (!Util.isEmpty(description, true)) {
			alloc.setDescription(description);
		}
		alloc.saveEx();

		// final int orderId = 0;
		// final int cashLineId = 0;

		return Msg.parseTranslation(Env.getCtx(), "@C_AllocationHdr_ID@ @Created@: " + alloc.getDocumentNo());
	}

}
