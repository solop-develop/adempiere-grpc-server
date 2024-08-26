/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
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
package org.spin.grpc.service.form;

import org.adempiere.exceptions.AdempiereException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Org;
import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_C_Charge;
import org.adempiere.core.domains.models.I_C_Currency;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.X_T_InvoiceGL;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MCurrency;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MOrg;
import org.compiere.model.MPayment;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
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
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.field.field_management.FieldManagementLogic;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Payment Allocation form
 */
public class PaymentAllocation extends PaymentAllocationImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PaymentAllocation.class);



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
			e.printStackTrace();
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
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}

	public static MBPartner validateAndGetBusinessPartner(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BPartner_ID@");
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ @NotFound@");
		}
		if (!businessPartner.isActive()) {
			throw new AdempiereException("@C_BPartner_ID@ @NotActive@");
		}
		return businessPartner;
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
			e.printStackTrace();
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
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
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
			.setValue(
				ValueManager.validateNull(
					organization.getName()
				)
			)
			.setName(
				ValueManager.validateNull(
					organization.getName()
				)
			)
		;

		return builder;
	}

	public static MOrg validateAndGetOrganization(int organizationId) {
		if (organizationId < 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		}
		if (organizationId == 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}
		MOrg organization = new Query(
			Env.getCtx(),
			I_AD_Org.Table_Name,
			" AD_Org_ID = ? ",
			null
		)
			.setParameters(organizationId)
			.setClient_ID()
			.first()
		;
		if (organization == null || organization.getAD_Org_ID() <= 0) {
			throw new AdempiereException("@AD_Org_ID@ @NotFound@");
		}
		if (!organization.isActive()) {
			throw new AdempiereException("@AD_Org_ID@ @NotActive@");
		}
		return organization;
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
			e.printStackTrace();
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
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
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
			.setIsoCode(
				ValueManager.validateNull(
					currency.getISO_Code()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					currency.getDescription()
				)
			)
		;

		return builder;
	}

	public static MCurrency validateAndGetCurrency(int currencyId) {
		if (currencyId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Currency_ID@");
		}
		MCurrency currency = new Query(
			Env.getCtx(),
			I_C_Currency.Table_Name,
			" C_Currency_ID = ? ",
			null
		)
			.setParameters(currencyId)
			.first()
		;
		if (currency == null || currency.getC_Currency_ID() <= 0) {
			throw new AdempiereException("@C_Currency_ID@ @NotFound@");
		}
		if (!currency.isActive()) {
			throw new AdempiereException("@C_Currency_ID@ @NotActive@");
		}
		return currency;
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
			e.printStackTrace();
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
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}


	public static TransactionType.Builder convertTransactionType(String value) {
		if (Util.isEmpty(value, true)) {
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
			.setValue(
				ValueManager.validateNull(
					transactionType.getValue()
				)
			)
			.setName(
				ValueManager.validateNull(name)
			)
			.setDescription(
				ValueManager.validateNull(description)
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
			e.printStackTrace();
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
		Timestamp date = ValueManager.getDateFromTimestampDate(
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
			String isReceipt = BooleanManager.getBooleanToString(
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

				boolean isReceipt = BooleanManager.getBooleanFromString(
					rs.getString("IsReceipt")
				);
				TransactionType.Builder transactionTypeBuilder = convertTransactionType(
					isReceipt ? X_T_InvoiceGL.APAR_ReceivablesOnly : X_T_InvoiceGL.APAR_PayablesOnly
				);

				int paymentId = rs.getInt(I_C_Payment.COLUMNNAME_C_Payment_ID);
				Payment.Builder paymentBuilder = Payment.newBuilder()
					.setId(paymentId)
					.setTransactionDate(
						ValueManager.getTimestampFromDate(
							rs.getTimestamp(I_C_Payment.COLUMNNAME_DateTrx)
						)
					)
					.setIsReceipt(isReceipt)
					.setDocumentNo(
						ValueManager.validateNull(
							rs.getString(I_C_Payment.COLUMNNAME_DocumentNo)
						)
					)
					.setDescription(
						ValueManager.validateNull(
							rs.getString(I_C_Payment.COLUMNNAME_Description)
						)
					)
					.setPaymentAmount(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal(I_C_Payment.COLUMNNAME_PayAmt)
						)
					)
					.setConvertedAmount(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal("ConvertedAmt")
						)
					)
					.setOpenAmount(
						NumberManager.getBigDecimalToString(
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
			e.printStackTrace();
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
		validateAndGetCurrency(currencyId);
		int businessPartnerId = request.getBusinessPartnerId();
		validateAndGetBusinessPartner(businessPartnerId);
		Timestamp date = ValueManager.getDateFromTimestampDate(
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
			String isReceipt = BooleanManager.getBooleanToString(
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

				boolean isSalesTransaction = BooleanManager.getBooleanFromString(
					rs.getString(I_C_Invoice.COLUMNNAME_IsSOTrx)
				);
				TransactionType.Builder transactionTypeBuilder = convertTransactionType(
					isSalesTransaction ? X_T_InvoiceGL.APAR_ReceivablesOnly : X_T_InvoiceGL.APAR_PayablesOnly
				);

				int invoiceId = rs.getInt(I_C_Invoice.COLUMNNAME_C_Invoice_ID);
				Invoice.Builder invoiceBuilder = Invoice.newBuilder()
					.setId(invoiceId)
					.setDateInvoiced(
						ValueManager.getTimestampFromDate(
							rs.getTimestamp(I_C_Invoice.COLUMNNAME_DateInvoiced)
						)
					)
					.setIsSalesTransaction(isSalesTransaction)
					.setDocumentNo(
						ValueManager.validateNull(
							rs.getString(I_C_Invoice.COLUMNNAME_DocumentNo)
						)
					)
					.setDescription(
						ValueManager.validateNull(
							rs.getString(I_C_Invoice.COLUMNNAME_Description)
						)
					)
					.setOriginalAmount(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal("OriginalAmt")
						)
					)
					.setConvertedAmount(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal("ConvertedAmt")
						)
					)
					.setOpenAmount(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal("OpenAmt")
						)
					)
					.setDiscountAmount(
						NumberManager.getBigDecimalToString(
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
			e.printStackTrace();
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
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}
	
	public static Charge.Builder convertCharge(MCharge charge) {
		Charge.Builder builder = Charge.newBuilder();
		if (charge == null || charge.getC_Charge_ID() <= 0) {
			return builder;
		}

		builder.setId(charge.getC_Charge_ID())
			.setName(
				ValueManager.validateNull(
					charge.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					charge.getDescription()
				)
			)
			.setAmount(
				NumberManager.getBigDecimalToString(
					charge.getChargeAmt()
				)
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
			e.printStackTrace();
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
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}


	private Timestamp getTransactionDate(List<PaymentSelection> paymentSelection, List<InvoiceSelection> invoiceSelection) {
		AtomicReference<Timestamp> transactionDateReference = new AtomicReference<Timestamp>();
		paymentSelection.forEach(paymentSelected -> {
			Timestamp paymentDate = ValueManager.getDateFromTimestampDate(
				paymentSelected.getTransactionDate()
			);
			Timestamp transactionDate = TimeUtil.max(transactionDateReference.get(), paymentDate);
			transactionDateReference.set(transactionDate);
		});
		invoiceSelection.forEach(invoiceSelected -> {
			Timestamp invoiceDate = ValueManager.getDateFromTimestampDate(
				invoiceSelected.getDateInvoiced()
			);
			Timestamp transactionDate = TimeUtil.max(transactionDateReference.get(), invoiceDate);
			transactionDateReference.set(transactionDate);
		});

		return transactionDateReference.get();
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ProcessResponse.Builder process(ProcessRequest request) {
		// validate and get Organization
		MOrg organization = validateAndGetOrganization(request.getTransactionOrganizationId());
		Properties context = Env.getCtx();

		AtomicReference<String> atomicStatus = new AtomicReference<String>();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		Env.setContext(context, windowNo, I_AD_Org.COLUMNNAME_AD_Org_ID, request.getTransactionOrganizationId());

		// validate and get Business Partner
		MBPartner businessPartner = validateAndGetBusinessPartner(request.getBusinessPartnerId());

		// validate and get Currency
		MCurrency currency = validateAndGetCurrency(request.getCurrencyId());

		Trx.run(transactionName -> {
			// transaction date
			Timestamp transactionDate = ValueManager.getDateFromTimestampDate(
				request.getDate()
			);
			if (transactionDate == null) {
				transactionDate = getTransactionDate(
					request.getPaymentSelectionsList(),
					request.getInvoiceSelectionsList()
				);
			}

			BigDecimal totalDifference = NumberManager.getBigDecimalFromString(
				request.getTotalDifference()
			);
			String status = saveData(
				windowNo, businessPartner.getC_BPartner_ID(),
				currency.getC_Currency_ID(), request.getIsMultiCurrency(),
				organization.getAD_Org_ID(), transactionDate,
				request.getChargeId(), request.getDescription(),
				totalDifference,
				request.getPaymentSelectionsList(),
				request.getInvoiceSelectionsList(),
				transactionName
			);
			atomicStatus.set(status);
		});

		return ProcessResponse.newBuilder()
			.setMessage(
				ValueManager.validateNull(
					atomicStatus.get()
				)
			)
		;
	}

	private String saveData(
		int windowNo, int businessPartnerId,
		int currencyId, boolean isMultiCurrency,
		int organizationId, Timestamp transactionDate,
		int chargeId, String description,
		BigDecimal totalDifference,
		List<PaymentSelection> paymentSelection,
		List<InvoiceSelection> invoiceSelection,
		String transactionName
	) {
		if (paymentSelection == null || invoiceSelection == null || (paymentSelection.size() + invoiceSelection.size() == 0)) {
			return "";
		}

		if (organizationId <= 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}

		final int orderId = 0;
		final int cashLineId = 0;

		int pRows = paymentSelection.size();
		List<Integer> paymentList = paymentSelection.stream()
			.map(pay -> {
				return pay.getId();
			})
			.collect(Collectors.toList())
		;

		List<BigDecimal> amountList = new ArrayList<>(pRows);

		// Sum up the payment and applied amounts.
		BigDecimal paymentAppliedAmt = Env.ZERO;
		for (PaymentSelection payment : paymentSelection) {
			BigDecimal paymentAmt = NumberManager.getBigDecimalFromString(
				payment.getAppliedAmount()
			);
			amountList.add(paymentAmt);
			paymentAppliedAmt = paymentAppliedAmt.add(paymentAmt);
		}

		//	Create Allocation manual
		final String userName = Env.getContext(Env.getCtx(), "#AD_User_Name");
		MAllocationHdr alloc = new MAllocationHdr(
			Env.getCtx(),
			true,
			transactionDate,
			currencyId,
			userName,
			transactionName
		);
		alloc.setAD_Org_ID(organizationId);
		//	Set Description
		if (!Util.isEmpty(description, true)) {
			alloc.setDescription(description);
		}
		alloc.saveEx();

		//  Invoices - Loop and generate allocations
		BigDecimal unmatchedApplied = Env.ZERO;
		//	For all invoices
		for (InvoiceSelection invoice : invoiceSelection) {
			//  Invoice variables
			int C_Invoice_ID = invoice.getId();
			BigDecimal AppliedAmt = NumberManager.getBigDecimalFromString(
				invoice.getAppliedAmount()
			);
			//  semi-fixed fields (reset after first invoice)
			BigDecimal DiscountAmt = NumberManager.getBigDecimalFromString(
				invoice.getDiscountAmount()
			);
			BigDecimal WriteOffAmt = NumberManager.getBigDecimalFromString(
				invoice.getWriteOffAmount()
			);
			BigDecimal invoiceOpen = NumberManager.getBigDecimalFromString(
				invoice.getOpenAmount()
			);
			//	OverUnderAmt needs to be in Allocation Currency
			BigDecimal OverUnderAmt = invoiceOpen.subtract(AppliedAmt)
				.subtract(DiscountAmt)
				.subtract(WriteOffAmt)
			;

			// for (PaymentSelection payment : paymentSelection) {
			for (int j = 0; j < paymentSelection.size() && AppliedAmt.signum() != 0; j++) {
				// if (AppliedAmt.signum() == 0) {
				// 	break;
				// }
				PaymentSelection payment = paymentSelection.get(j);

				int paymentId = payment.getId();
				BigDecimal paymentAmt = NumberManager.getBigDecimalFromString(
					payment.getAppliedAmount()
				);

				// only match same sign (otherwise appliedAmt increases)
				// and not zero (appliedAmt was checked earlier)
				if (paymentAmt.signum() == AppliedAmt.signum()) {
					BigDecimal amount = AppliedAmt;

					// if there's more open on the invoice than left in the payment
					if (amount.abs().compareTo(paymentAmt.abs()) > 0) {
						amount = paymentAmt;
					}

					//	Allocation Line
					MAllocationLine aLine = new MAllocationLine(
						alloc, amount,
						DiscountAmt, WriteOffAmt, OverUnderAmt
					);
					aLine.setDocInfo(businessPartnerId, orderId, C_Invoice_ID);
					aLine.setPaymentInfo(paymentId, cashLineId);
					aLine.saveEx();
					
					// Apply Discounts and WriteOff only first time
					DiscountAmt = Env.ZERO;
					WriteOffAmt = Env.ZERO;
					// subtract amount from Payment/Invoice
					AppliedAmt = AppliedAmt.subtract(amount);
					paymentAmt = paymentAmt.subtract(amount);
					amountList.set(j, paymentAmt); // update
				} //	for all applied amounts
			} //	loop through payments for invoice

			if (AppliedAmt.signum() == 0 && DiscountAmt.signum() == 0 && WriteOffAmt.signum() == 0) {
				continue;
			}
			// remainder will need to match against other invoices
			else {
				int C_Payment_ID = 0;
				
				//	Allocation Line
				MAllocationLine aLine = new MAllocationLine(
					alloc, AppliedAmt,
					DiscountAmt, WriteOffAmt, OverUnderAmt
				);
				aLine.setDocInfo(businessPartnerId, 0, C_Invoice_ID);
				aLine.setPaymentInfo(C_Payment_ID, 0);
				aLine.saveEx();
				log.fine("Allocation Amount=" + AppliedAmt);
				unmatchedApplied = unmatchedApplied.add(AppliedAmt);
			}
		} //	invoice loop

		
		// check for unapplied payment amounts (eg from payment reversals)
		for (int i = 0; i < paymentList.size(); i++) {
			BigDecimal payAmt = (BigDecimal) amountList.get(i);
			if (payAmt.signum() == 0) {
				continue;
			}
			int paymentId = paymentList.get(i);
			log.fine("Payment=" + paymentId + ", Amount=" + payAmt);

			//	Allocation Line
			MAllocationLine aLine = new MAllocationLine(
				alloc, payAmt,
				Env.ZERO, Env.ZERO, Env.ZERO
			);
			aLine.setDocInfo(businessPartnerId, 0, 0);
			aLine.setPaymentInfo(paymentId, 0);
			aLine.saveEx();
			unmatchedApplied = unmatchedApplied.subtract(payAmt);
		}

		// check for charge amount
		if (chargeId > 0 && unmatchedApplied.compareTo(BigDecimal.ZERO) != 0) {
			// BigDecimal chargeAmt = totalDiff;
			BigDecimal chargeAmt = BigDecimal.ZERO;
		
			//	Allocation Line
			MAllocationLine aLine = new MAllocationLine(
				alloc,
				chargeAmt.negate(),
				Env.ZERO, Env.ZERO, Env.ZERO
			);
			aLine.set_CustomColumn(I_C_Charge.COLUMNNAME_C_Charge_ID, chargeId);
			aLine.setC_BPartner_ID(businessPartnerId);
			aLine.saveEx(transactionName);
			unmatchedApplied = unmatchedApplied.add(chargeAmt);
		}

		if (unmatchedApplied.signum() != 0) {
			log.log(Level.SEVERE, "Allocation not balanced -- out by " + unmatchedApplied);
		}
		
		//	Should start WF
		if (alloc.get_ID() > 0) {
			if (!alloc.processIt(DocAction.ACTION_Complete)) {
				throw new AdempiereException("@ProcessFailed@: " + alloc.getProcessMsg());
			}
			alloc.saveEx();
		}

		//  Test/Set IsPaid for Invoice - requires that allocation is posted
		for (InvoiceSelection invoice : invoiceSelection) {
			//  Invoice variables
			int C_Invoice_ID = invoice.getId();
			String sql = "SELECT invoiceOpen(C_Invoice_ID, 0) "
				+ "FROM C_Invoice WHERE C_Invoice_ID = ?"
			;
			BigDecimal open = DB.getSQLValueBD(transactionName, sql, C_Invoice_ID);
			if (open != null && open.signum() == 0) {
				sql = "UPDATE C_Invoice SET IsPaid='Y' "
					+ "WHERE C_Invoice_ID=" + C_Invoice_ID;
				int no = DB.executeUpdate(sql, transactionName);
				log.config("Invoice #" + C_Invoice_ID + " is paid - updated=" + no);
			}
			else {
				log.config("Invoice #" + C_Invoice_ID + " is not paid - " + open);
			}
		}
		
		//  Test/Set Payment is fully allocated
		for (PaymentSelection payment : paymentSelection) {
		// for (int i = 0; i < paymentList.size(); i++) {
			int paymentId = payment.getId();
			MPayment pay = new MPayment(
				Env.getCtx(),
				paymentId,
				transactionName
			);
			if (pay.testAllocation()) {
				pay.saveEx();
			}
			log.config(
				"Payment #" + paymentId +
				(pay.isAllocated() ? " not" : " is")
				+ " fully allocated"
			);
		}
		paymentList.clear();
		amountList.clear();

		return Msg.parseTranslation(Env.getCtx(), "@C_AllocationHdr_ID@ @Created@: " + alloc.getDocumentNo());
	}

}
