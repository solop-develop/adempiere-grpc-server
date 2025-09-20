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
package org.spin.grpc.service.field.order;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.order.GetOrderInfoRequest;
import org.spin.backend.grpc.field.order.ListBusinessPartnersRequest;
import org.spin.backend.grpc.field.order.ListOrdersInfoRequest;
import org.spin.backend.grpc.field.order.ListOrdersInfoResponse;
import org.spin.backend.grpc.field.order.OrderInfo;
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
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

public class OrderInfoLogic {

	// public static tableName = C_Order.Table_Name;

	private static final String SQL = "SELECT "
		+ "o.C_Order_ID, o.UUID,"
		+ "o.C_BPartner_ID,"
		+ "(SELECT Name FROM C_BPartner AS bp WHERE bp.C_BPartner_ID = o.C_BPartner_ID) AS BusinessPartner, "
		+ "o.DateOrdered, "
		+ "o.DocumentNo, "
		+ "o.C_Currency_ID, "
		+ "((SELECT ISO_Code FROM C_Currency AS c WHERE c.C_Currency_ID = o.C_Currency_ID)) AS Currency, "
		+ "o.GrandTotal, "
		+ "currencyBase(o.GrandTotal, o.C_Currency_ID,o.DateAcct, o.AD_Client_ID,o.AD_Org_ID), "
		+ "o.IsSOTrx, "
		+ "o.Description, "
		+ "o.POReference, "
		+ "o.isDelivered, "
		+ "o.DocStatus "
		+ "FROM C_Order AS o "
		+ "WHERE 1=1 "
	;



	/**
	 * Validate productId and MProduct, and get instance
	 * @param tableName
	 * @return
	 */
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
			I_C_Order.COLUMNNAME_C_BPartner_ID, I_C_Order.Table_Name,
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



	public static OrderInfo.Builder getOrderInfo(GetOrderInfoRequest request) {
		final int id = request.getId();
		final String uuid = request.getUuid();
		final String code = request.getCode();
		if (id <= 0 && Util.isEmpty(uuid, true) && Util.isEmpty(code, true)) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@ | @UUID@ | @DocumentNo@");
		}

		//
		List<Object> filtersList = new ArrayList<>();
		String sql = SQL;
		if (id > 0) {
			sql += "AND o.C_Order_ID = ? ";
			filtersList.add(id);
		} else if (!Util.isEmpty(uuid, true)) {
			sql += "AND o.UUID = ? ";
			filtersList.add(uuid);
		} else if (!Util.isEmpty(code, true)) {
			sql += "AND o.DocumentNo = ? ";
			filtersList.add(code);

			// Add AD_Client_ID restriction
			sql += "AND o.AD_Client_ID = ? ";
			final int clientId = Env.getAD_Client_ID(Env.getCtx());
			filtersList.add(clientId);
		}

		//	Limit to 1 record to performance
		final int pageNumber = 1;
		final int limit = 1;
		final int offset = (pageNumber - 1) * limit;
		final String parsedSQL = LimitUtil.getQueryWithLimit(sql, limit, offset);

		OrderInfo.Builder builder = OrderInfo.newBuilder();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(parsedSQL, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				builder = OrderInfoConvert.convertOrderInfo(
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
	public static ListOrdersInfoResponse.Builder listOrderInfo(ListOrdersInfoRequest request) {
		// Fill context
		Properties context = Env.getCtx();
		final int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo,
			context,
			request.getContextAttributes()
		);

		String tableName;
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			tableName = "C_Order",
			request.getIsWithoutValidation()
		);

		//
		String sql = SQL;

		List<Object> filtersList = new ArrayList<>(); // includes on filters criteria
		// Document No
		if (!Util.isEmpty(request.getDocumentNo(), true)) {
			sql += "AND UPPER(o.DocumentNo) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(
				request.getDocumentNo()
			);
		}
		// Business Partner
		int businessPartnerId = request.getBusinessPartnerId();
		if (businessPartnerId > 0) {
			sql += "AND o.C_BPartner_ID = ? ";
			filtersList.add(
				businessPartnerId
			);
		}
		// Is Sales Transaction
		if (!Util.isEmpty(request.getIsSalesTransaction(), true)) {
			sql += "AND o.IsSOTrx = ? ";
			filtersList.add(
				BooleanManager.getBooleanToString(
					request.getIsSalesTransaction()
				)
			);
		}
		// Is Delivered
		if (!Util.isEmpty(request.getIsDelivered(), true)) {
			sql += "AND IsDelivered = ? ";
			filtersList.add(
				BooleanManager.getBooleanToString(
					request.getIsDelivered()
				)
			);
		}
		// Description
		if (!Util.isEmpty(request.getDescription(), true)) {
			sql += "AND UPPER(o.Description) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(
				request.getDescription()
			);
		}
		// Date Order
		Timestamp dateFrom = ValueManager.getTimestampFromProtoTimestamp(
			request.getOrderDateFrom()
		);
		Timestamp dateTo = ValueManager.getTimestampFromProtoTimestamp(
			request.getOrderDateTo()
		);
		if (dateFrom != null || dateTo != null) {
			sql += " AND ";
			if (dateFrom != null && dateTo != null) {
				sql += "TRUNC(o.DateOrdered, 'DD') BETWEEN ? AND ? ";
				filtersList.add(dateFrom);
				filtersList.add(dateTo);
			}
			else if (dateFrom != null) {
				sql += "TRUNC(o.DateOrdered, 'DD') >= ? ";
				filtersList.add(dateFrom);
			}
			else {
				// DateTo != null
				sql += "TRUNC(o.DateOrdered, 'DD') <= ? ";
				filtersList.add(dateTo);
			}
		}
		// Order Reference
		if (!Util.isEmpty(request.getOrderReference(), true)) {
			sql += "AND UPPER(o.POReference) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(
				request.getOrderReference()
			);
		}
		// Grand Total From
		BigDecimal grandTotalFrom = NumberManager.getBigDecimalFromString(
			request.getGrandTotalFrom()
		);
		if (grandTotalFrom != null) {
			sql += "AND o.GrandTotal >= ? ";
			filtersList.add(
				grandTotalFrom
			);
		}
		// Grand Total To
		BigDecimal grandTotalTo = NumberManager.getBigDecimalFromString(
			request.getGrandTotalTo()
		);
		if (grandTotalTo != null) {
			sql += "AND o.GrandTotal <= ? ";
			filtersList.add(
				grandTotalTo
			);
		}

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault(context, false)
			.addAccessSQL(
				sql.toString(),
				"o",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			)
		;

		StringBuffer whereClause = new StringBuffer();

		// validation code of field
		if (!request.getIsWithoutValidation()) {
			String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
				tableName,
				"o",
				reference.ValidationCode
			);
			if (!Util.isEmpty(reference.ValidationCode, true)) {
				String parsedValidationCode = Env.parseContext(context, windowNo, validationCode, false);
				if (Util.isEmpty(parsedValidationCode, true)) {
					throw new AdempiereException("@WhereClause@ @Unparseable@");
				}
				whereClause.append(" AND ").append(parsedValidationCode);
			}
		}

		//	For dynamic condition
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), tableName, "o", filtersList);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//	Add includes first AND
			whereClause.append(" AND ")
				.append("(")
				.append(dynamicWhere)
				.append(")")
			;
		}

		sqlWithRoleAccess += whereClause;
		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, tableName, "o", request.getSearchValue(), filtersList);

		//	Get page and count
		final int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		final int limit = LimitUtil.getPageSize(request.getPageSize());
		final int offset = (pageNumber - 1) * limit;
		final int count = CountUtil.countRecords(parsedSQL, tableName, "o", filtersList);

		//	Add Row Number
		parsedSQL += " ORDER BY o.DateOrdered DESC, o.DocumentNo ";
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);

		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListOrdersInfoResponse.Builder builderList = ListOrdersInfoResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(
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
				OrderInfo.Builder builder = OrderInfoConvert.convertOrderInfo(
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
