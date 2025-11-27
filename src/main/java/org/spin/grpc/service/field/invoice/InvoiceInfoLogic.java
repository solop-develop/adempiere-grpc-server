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
package org.spin.grpc.service.field.invoice;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.invoice.GetInvoiceInfoRequest;
import org.spin.backend.grpc.field.invoice.InvoiceInfo;
import org.spin.backend.grpc.field.invoice.InvoicePaySchedule;
import org.spin.backend.grpc.field.invoice.ListBusinessPartnersRequest;
import org.spin.backend.grpc.field.invoice.ListInvoicePaySchedulesRequest;
import org.spin.backend.grpc.field.invoice.ListInvoicePaySchedulesResponse;
import org.spin.backend.grpc.field.invoice.ListInvoicesInfoRequest;
import org.spin.backend.grpc.field.invoice.ListInvoicesInfoResponse;
import org.spin.backend.grpc.field.invoice.ListOrdersRequest;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.field.field_management.FieldManagementLogic;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

public class InvoiceInfoLogic {

	public final static String Table_Name = I_C_Invoice.Table_Name;

	private static final String SQL = "SELECT "
		+ "i.C_Invoice_ID, i.UUID, "
		+ "(SELECT Name FROM C_BPartner bp WHERE bp.C_BPartner_ID=i.C_BPartner_ID) AS BusinessPartner, "
		+ "i.DateInvoiced, "
		+ "i.DocumentNo, "
		+ "(SELECT ISO_Code FROM C_Currency c WHERE c.C_Currency_ID=i.C_Currency_ID) AS Currency, "
		+ "i.GrandTotal, "
		+ "currencyBase(i.GrandTotal, i.C_Currency_ID, i.DateAcct, i.AD_Client_ID, i.AD_Org_ID) AS ConvertedAmount, "
		+ "invoiceOpen(C_Invoice_ID, 0) AS OpenAmt, "
		+ "(SELECT pt.Name FROM C_PaymentTerm pt WHERE pt.C_PaymentTerm_ID = i.C_PaymentTerm_ID) AS PaymentTerm, "
		+ "i.IsPaid, "
		+ "i.IsSOTrx, "
		+ "i.Description, "
		+ "i.POReference, "
		+ "i.DocStatus "
		+ "FROM C_Invoice AS i "
		+ "WHERE 1=1 "
	;



	/**
	 * Validate productId and MProduct, and get instance
	 * @param invoiceId
	 * @return
	 */
	public static MInvoice validateAndGetInvoice(int invoiceId) {
		if (invoiceId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Invoice_ID@");
		}
		MInvoice invoice = new MInvoice(Env.getCtx(), invoiceId, null);
		if (invoice == null || invoice.getC_Invoice_ID() <= 0) {
			throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
		}
		return invoice;
	}



	public static ListLookupItemsResponse.Builder listOrders(ListOrdersRequest request) {
		String whereClause = "";
		if (!Util.isEmpty(request.getIsSalesTransaction(), true)) {
			String isSalesTransaction = BooleanManager.getBooleanToString(
				request.getIsSalesTransaction()
			);
			whereClause = " C_Order.IsSOTrx = '" + isSalesTransaction + "' ";
		}
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_C_Invoice.COLUMNNAME_C_Order_ID, I_C_Invoice.Table_Name,
			0,
			whereClause
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listBusinessPartners(ListBusinessPartnersRequest request) {
		String whereClause = "";
		if (!Util.isEmpty(request.getIsSalesTransaction(), true)) {
			boolean isSalesTransaction = BooleanManager.getBooleanFromString(
				request.getIsSalesTransaction()
			);
			if (isSalesTransaction) {
				whereClause = " C_BPartner.IsCustomer = 'Y' ";
			} else {
				whereClause = " C_BPartner.IsVendor = 'Y' ";
			}
		}
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_C_Invoice.COLUMNNAME_C_BPartner_ID, I_C_Invoice.Table_Name,
			0,
			whereClause
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}



	public static InvoiceInfo.Builder getInvoiceInfo(GetInvoiceInfoRequest request) {
		final int id = request.getId();
		final String uuid = request.getUuid();
		final String code = request.getCode();
		if (id <= 0 && Util.isEmpty(uuid, true) && Util.isEmpty(code, true)) {
			throw new AdempiereException("@FillMandatory@ @C_Invoice_ID@ | @UUID@ | @DocumentNo@");
		}

		//
		List<Object> filtersList = new ArrayList<>();
		String sql = SQL;
		if (id > 0) {
			sql += "AND i.C_Invoice_ID = ? ";
			filtersList.add(id);
		} else if (!Util.isEmpty(uuid, true)) {
			sql += "AND i.UUID = ? ";
			filtersList.add(uuid);
		} else if (!Util.isEmpty(code, true)) {
			sql += "AND i.DocumentNo = ? ";
			filtersList.add(code);

			// Add AD_Client_ID restriction
			sql += "AND AD_Client_ID = ? ";
			final int clientId = Env.getAD_Client_ID(Env.getCtx());
			filtersList.add(clientId);
		}

		//	Limit to 1 record to performance
		final int pageNumber = 1;
		final int limit = 1;
		final int offset = (pageNumber - 1) * limit;
		final String parsedSQL = LimitUtil.getQueryWithLimit(sql, limit, offset);

		InvoiceInfo.Builder builder = InvoiceInfo.newBuilder();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(parsedSQL, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				builder = InvoiceInfoConvert.convertInvoiceInfo(
					rs
				);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builder;
	}



	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListInvoicesInfoResponse.Builder listInvoiceInfo(ListInvoicesInfoRequest request) {
		// Fill context
		Properties context = Env.getCtx();
		final int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo,
			context,
			request.getContextAttributes()
		);

		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			Table_Name,
			request.getIsWithoutValidation()
		);

		//
		String sql = SQL;

		List<Object> filtersList = new ArrayList<>(); // includes on filters criteria
		// Document No
		if (!Util.isEmpty(request.getDocumentNo(), true)) {
			sql += "AND UPPER(i.DocumentNo) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(
				request.getDocumentNo()
			);
		}
		// Business Partner
		int businessPartnerId = request.getBusinessPartnerId();
		if (businessPartnerId > 0) {
			sql += "AND i.C_BPartner_ID = ? ";
			filtersList.add(
				businessPartnerId
			);
		}
		// Is Sales Transaction
		if (!Util.isEmpty(request.getIsSalesTransaction(), true)) {
			sql += "AND i.IsSOTrx = ? ";
			filtersList.add(
				BooleanManager.getBooleanToString(
					request.getIsSalesTransaction()
				)
			);
		}
		// Is Paid
		if (!Util.isEmpty(request.getIsPaid(), true)) {
			sql += "AND i.IsPaid = ? ";
			filtersList.add(
				BooleanManager.getBooleanToString(
					request.getIsPaid()
				)
			);
		}
		// Description
		if (!Util.isEmpty(request.getDescription(), true)) {
			sql += "AND UPPER(i.Description) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(
				request.getDescription()
			);
		}
		// Date Invoiced
		Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(
			request.getInvoiceDateFrom()
		);
		Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(
			request.getInvoiceDateTo()
		);
		if (dateFrom != null || dateTo != null) {
			sql += " AND ";
			if (dateFrom != null && dateTo != null) {
				sql += "TRUNC(i.DateInvoiced, 'DD') BETWEEN ? AND ? ";
				filtersList.add(dateFrom);
				filtersList.add(dateTo);
			}
			else if (dateFrom != null) {
				sql += "TRUNC(i.DateInvoiced, 'DD') >= ? ";
				filtersList.add(dateFrom);
			}
			else {
				// DateTo != null
				sql += "TRUNC(i.DateInvoiced, 'DD') <= ? ";
				filtersList.add(dateTo);
			}
		}
		// Order
		if (request.getOrderId() > 0) {
			sql += "AND i.C_Order_ID = ? ";
			filtersList.add(
				request.getOrderId()
			);
		}
		// Grand Total From
		BigDecimal grandTotalFrom = NumberManager.getBigDecimalFromString(
			request.getGrandTotalFrom()
		);
		if (grandTotalFrom != null) {
			sql += "AND i.GrandTotal >= ? ";
			filtersList.add(
				grandTotalFrom
			);
		}
		// Grand Total To
		BigDecimal grandTotalTo = NumberManager.getBigDecimalFromString(
			request.getGrandTotalTo()
		);
		if (grandTotalTo != null) {
			sql += "AND i.GrandTotal <= ? ";
			filtersList.add(
				grandTotalTo
			);
		}

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault(context, false)
			.addAccessSQL(
				sql.toString(),
				"i",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer(" 1=1 ");
		// validation code of field
		if (!request.getIsWithoutValidation() && !Util.isEmpty(reference.ValidationCode, true)) {
			String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
				Table_Name,
				"i",
				reference.ValidationCode
			);
			String parsedValidationCode = Env.parseContext(context, windowNo, validationCode, false);
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException(
					"@AD_Reference_ID@ " + reference.KeyColumn + ", @Code@/@WhereClause@ @Unparseable@"
				);
			}
			whereClause.append(" AND ")
				.append(parsedValidationCode)
			;
		}

		//	For dynamic condition
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), Table_Name, "i", filtersList);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//	Add includes first AND
			whereClause
				.append(" AND ")
				.append("(")
				.append(dynamicWhere)
				.append(")")
			;
		}

		if (!whereClause.toString().trim().startsWith("AND")) {
			sqlWithRoleAccess += " AND ";
		}
		sqlWithRoleAccess += whereClause;

		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, Table_Name, "i", request.getSearchValue(), filtersList);

		//	Get page and count
		final int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		final int limit = LimitUtil.getPageSize(request.getPageSize());
		final int offset = (pageNumber - 1) * limit;
		final int count = CountUtil.countRecords(parsedSQL, Table_Name, "i", filtersList);

		//	Add Row Number
		parsedSQL += " ORDER BY i.DateInvoiced desc, i.DocumentNo ";
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);

		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListInvoicesInfoResponse.Builder builderList = ListInvoicesInfoResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(
					nexPageToken
				)
			)
		;
	
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(parsedSQL, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				InvoiceInfo.Builder builder = InvoiceInfoConvert.convertInvoiceInfo(
					rs
				);
				builderList.addRecords(builder);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	public static ListInvoicePaySchedulesResponse.Builder listInvoicePaySchedules(ListInvoicePaySchedulesRequest request) {
		final int invoiceId = request.getInvoiceId();
		if (invoiceId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Invoice_ID@");
		}

		final String sql = "SELECT "
			+ "i.C_InvoicePaySchedule_ID, "
			+ "COALESCE((SELECT COUNT(ps.C_PaymentTerm_ID)"
			+ " 		FROM"
			+ " 			C_PaySchedule ps, C_InvoicePaySchedule cips"
			+ " 		WHERE"
			+ " 			ps.C_PaySchedule_ID = cips.C_PaySchedule_ID"
			+ " 			AND cips.C_INVOICE_ID = i.C_Invoice_ID"
			+ " 			AND cips.duedate <= i.duedate"
			+ " 		GROUP BY ps.C_PaymentTerm_ID),1) || ' / ' ||"
			+ " 		COALESCE((SELECT COUNT(ps.C_PaymentTerm_ID) AS maxpayno"
			+ " 			FROM "
			+ " 			C_PaySchedule ps, C_InvoicePaySchedule cips"
			+ " 		WHERE "
			+ " 			ps.C_PaySchedule_ID = cips.C_PaySchedule_ID"
			+ " 			AND cips.C_INVOICE_ID = i.C_Invoice_ID"
			+ " 		GROUP BY ps.C_PaymentTerm_ID),1) AS PaymentCount, "
			+ "i.DueDate, "
			+ "(SELECT ISO_Code FROM C_Currency c WHERE c.C_Currency_ID=i.C_Currency_ID) AS Currency, "
			+ "i.GrandTotal, "
			+ "currencyBase(i.GrandTotal, i.C_Currency_ID, i.DateAcct, i.AD_Client_ID, i.AD_Org_ID) AS ConvertedAmount, "
			+ "invoiceOpen(C_Invoice_ID, C_InvoicePaySchedule_ID) AS OpenAmt, "
			+ "CASE WHEN invoiceOpen(C_Invoice_ID,C_InvoicePaySchedule_ID) <= 0 THEN 'Y' ELSE 'N' END AS IsPaid "
			+ "FROM C_Invoice_v AS i "
			+ "WHERE i.C_Invoice_ID = ? "
		;

		List<Object> parametersList = new ArrayList<>();
		parametersList.add(invoiceId);

		//	Count records
		final int pageNumber = LimitUtil.getPageNumber(
			SessionManager.getSessionUuid(),
			request.getPageToken()
		);
		final int limit = LimitUtil.getPageSize(request.getPageSize());
		final int offset = (pageNumber - 1) * limit;
		final int count = CountUtil.countRecords(sql, Table_Name, "i", parametersList);
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListInvoicePaySchedulesResponse.Builder builderList = ListInvoicePaySchedulesResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(
					nexPageToken
				)
			)
		;

		String parsedSQL = LimitUtil.getQueryWithLimit(sql, limit, offset);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(parsedSQL, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, parametersList);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				InvoicePaySchedule.Builder builder = InvoiceInfoConvert.convertInvoicePaySchedule(
					rs
				);
				builderList.addRecords(builder);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}

}
