package org.spin.grpc.service.field.payment;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.payment.ListBankAccountRequest;
import org.spin.backend.grpc.field.payment.ListBusinessPartnersRequest;
import org.spin.backend.grpc.field.payment.ListPaymentInfoRequest;
import org.spin.backend.grpc.field.payment.ListPaymentInfoResponse;
import org.spin.backend.grpc.field.payment.PaymentInfo;
import org.spin.base.db.WhereClauseUtil;
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

public class PaymentInfoLogic {

	/**
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
			I_C_Payment.COLUMNNAME_C_BPartner_ID, I_C_Payment.Table_Name,
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

	/**
	 * @param tableName
	 * @return
	 */

	 public static ListLookupItemsResponse.Builder listBankAccount(ListBankAccountRequest request) {
		String whereClause = "";
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_C_Payment.COLUMNNAME_C_BankAccount_ID, I_C_Payment.Table_Name,
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

	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListPaymentInfoResponse.Builder listPaymentInfo(ListPaymentInfoRequest request) {
		// Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		// ContextManager.setContextWithAttributesFromString(windowNo, Env.getCtx(), request.getContextAttributes());

		String tableName;
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			tableName = "C_Payment"
		);

		//
		String sql = "SELECT "
			+ "p.C_Payment_ID, p.UUID, "
			+ "(SELECT b.Name || ' ' || ba.AccountNo FROM C_Bank b, C_BankAccount ba WHERE b.C_Bank_ID=ba.C_Bank_ID AND ba.C_BankAccount_ID=p.C_BankAccount_ID) AS BankAccount, "
			+ "(SELECT Name FROM C_BPartner bp WHERE bp.C_BPartner_ID=p.C_BPartner_ID) AS BusinessPartner, "
			+ "p.DateTrx, "
			+ "p.DocumentNo, "
			+ "p.IsReceipt, "
			+ "(SELECT ISO_Code FROM C_Currency c WHERE c.C_Currency_ID=p.C_Currency_ID) AS Currency, "
			+ "p.PayAmt, "
			+ "currencyBase(p.PayAmt,p.C_Currency_ID,p.DateTrx, p.AD_Client_ID,p.AD_Org_ID), "
			+ "p.DiscountAmt, "
			+ "p.WriteOffAmt, "
			+ "p.IsAllocated, "
			+ "docstatus "
			+ "FROM C_Payment AS p "
			+ "WHERE 1=1 "
		;

		List<Object> filtersList = new ArrayList<>(); // includes on filters criteria
		// Document No
		if (!Util.isEmpty(request.getDocumentNo(), true)) {
			sql += "AND UPPER(p.DocumentNo) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(
				request.getDocumentNo()
			);
		}
		// Business Partner
		if (request.getBusinessPartnerId() > 0) {
			sql += "AND p.C_BPartner_ID = ? ";
			filtersList.add(
				request.getBusinessPartnerId()
			);
		}
		// Business Partner
		if (request.getBankAccountId() > 0) {
			sql += "AND p.C_BankAccount_ID = ? ";
			filtersList.add(
				request.getBankAccountId()
			);
		}
		// Is Receipt
		if (!Util.isEmpty(request.getIsReceipt(), true)) {
			sql += "AND IsReceipt = ? ";
			filtersList.add(
				BooleanManager.getBooleanToString(
					request.getIsReceipt()
				)
			);
		}
		// Payment Date
		Timestamp dateFrom = ValueManager.getDateFromTimestampDate(
			request.getPaymentDateFrom()
		);
		Timestamp dateTo = ValueManager.getDateFromTimestampDate(
			request.getPaymentDateTo()
		);
		if (dateFrom != null || dateTo != null) {
			sql += " AND ";
			if (dateFrom != null && dateTo != null) {
				sql += "TRUNC(p.DateTrx, 'DD') BETWEEN ? AND ? ";
				filtersList.add(dateFrom);
				filtersList.add(dateTo);
			}
			else if (dateFrom != null) {
				sql += "TRUNC(p.DateTrx, 'DD') >= ? ";
				filtersList.add(dateFrom);
			}
			else {
				// DateTo != null
				sql += "TRUNC(p.DateTrx, 'DD') <= ? ";
				filtersList.add(dateTo);
			}
		}
		// Grand Total From
		BigDecimal grandTotalFrom = NumberManager.getBigDecimalFromString(
			request.getGrandTotalFrom()
		);
		if (grandTotalFrom != null) {
			sql += "AND p.PayAmt >= ? ";
			filtersList.add(
				grandTotalFrom
			);
		}
		// Grand Total To
		BigDecimal grandTotalTo = NumberManager.getBigDecimalFromString(
			request.getGrandTotalTo()
		);
		if (grandTotalTo != null) {
			sql += "AND p.PayAmt <= ? ";
			filtersList.add(
				grandTotalTo
			);
		}

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql.toString(),
				"p",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();

		// validation code of field
		String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias("p", reference.ValidationCode);
		String parsedValidationCode = Env.parseContext(Env.getCtx(), windowNo, validationCode, false);
		if (!Util.isEmpty(reference.ValidationCode, true)) {
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException("@WhereClause@ @Unparseable@");
			}
			whereClause.append(" AND ").append(parsedValidationCode);
		}

		//	For dynamic condition
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), tableName, "p", filtersList);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//	Add includes first AND
			whereClause.append(" AND ")
				.append("(")
				.append(dynamicWhere)
				.append(")");
		}

		sqlWithRoleAccess += whereClause;
		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, tableName, "p", request.getSearchValue(), filtersList);

		//	Get page and count
		final int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		final int limit = LimitUtil.getPageSize(request.getPageSize());
		final int offset = (pageNumber - 1) * limit;
		final int count = CountUtil.countRecords(parsedSQL, tableName, "p", filtersList);

		//	Add Row Number
		parsedSQL += " ORDER BY p.DateTrx desc, p.DocumentNo ";
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);

		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListPaymentInfoResponse.Builder builderList = ListPaymentInfoResponse.newBuilder()
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
				PaymentInfo.Builder builder = PaymentInfoConvert.convertPaymentInfo(
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
