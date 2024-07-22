package org.spin.grpc.service.field.order;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
import org.spin.backend.grpc.field.order.ListBusinessPartnersOrderRequest;
import org.spin.backend.grpc.field.order.ListOrdersInfoRequest;
import org.spin.backend.grpc.field.order.ListOrdersInfoResponse;
import org.spin.backend.grpc.field.order.OrderInfo;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.UserInterface;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

public class OrderInfoLogic {
	// public static tableName = C_Order.Table_Name;

	/**
	 * Validate productId and MProduct, and get instance
	 * @param tableName
	 * @return
	 */

	 public static ListLookupItemsResponse.Builder listBusinessPartners(ListBusinessPartnersOrderRequest request) {
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static OrderInfo.Builder getOrder(GetOrderInfoRequest request) {
		final int orderId = request.getId();
		if (orderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Invoice_ID@");
		}
		final String sql = "SELECT "
			+ "o.C_Order_ID, o.UUID,"
			+ "(SELECT Name FROM C_BPartner bp WHERE bp.C_BPartner_ID=o.C_BPartner_ID) AS BusinessPartner, "
			+ "o.DateOrdered, "
			+ "o.DocumentNo, "
			+ "((SELECT ISO_Code FROM C_Currency c WHERE c.C_Currency_ID=o.C_Currency_ID)) AS Currency, "
			+ "o.GrandTotal, "
			+ "currencyBase(o.GrandTotal,o.C_Currency_ID,o.DateAcct, o.AD_Client_ID,o.AD_Org_ID), "
			+ "o.IsSOTrx, "
			+ "o.Description, "
			+ "o.POReference, "
			+ "o.isdelivered, "
			+ "o.DocStatus "
			+ "FROM C_Order AS o "
			+ "WHERE o.C_Order_ID = ? "
		;

		OrderInfo.Builder builder = OrderInfo.newBuilder();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, orderId);
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
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, Env.getCtx(), request.getContextAttributes());

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
		String sql = "SELECT "
			+ "o.C_Order_ID, o.UUID,"
			+ "(SELECT Name FROM C_BPartner bp WHERE bp.C_BPartner_ID=o.C_BPartner_ID) AS BusinessPartner, "
			+ "o.DateOrdered, "
			+ "o.DocumentNo, "
			+ "((SELECT ISO_Code FROM C_Currency c WHERE c.C_Currency_ID=o.C_Currency_ID)) AS Currency, "
			+ "o.GrandTotal, "
			+ "currencyBase(o.GrandTotal,o.C_Currency_ID,o.DateAcct, o.AD_Client_ID,o.AD_Org_ID), "
			+ "o.IsSOTrx, "
			+ "o.Description, "
			+ "o.POReference, "
			+ "o.isdelivered, "
			+ "o.DocStatus "
			+ "FROM C_Order AS o "
			+ "WHERE 1=1 "
		;

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
		Timestamp dateFrom = ValueManager.getDateFromTimestampDate(
			request.getOrderDateFrom()
		);
		Timestamp dateTo = ValueManager.getDateFromTimestampDate(
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
		// Order
		if (request.getOrderId() > 0) {
			sql += "AND o.C_Order_ID = ? ";
			filtersList.add(
				request.getOrderId()
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
		String sqlWithRoleAccess = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql.toString(),
				"o",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();

		// validation code of field
		if (!request.getIsWithoutValidation()) {
			String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
				tableName,
				"o",
				reference.ValidationCode
			);
			if (!Util.isEmpty(reference.ValidationCode, true)) {
				String parsedValidationCode = Env.parseContext(Env.getCtx(), windowNo, validationCode, false);
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
				.append(")");
		}

		sqlWithRoleAccess += whereClause;
		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, tableName, "o", request.getSearchValue(), filtersList);

		//	Get page and count
		final int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		final int limit = LimitUtil.getPageSize(request.getPageSize());
		final int offset = (pageNumber - 1) * limit;
		final int count = CountUtil.countRecords(parsedSQL, tableName, "o", filtersList);

		//	Add Row Number
		parsedSQL += " ORDER BY o.DateOrdered desc, o.DocumentNo ";
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);

		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListOrdersInfoResponse.Builder builderList = ListOrdersInfoResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueManager.validateNull(
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
		}

		return builderList;
	}
}
