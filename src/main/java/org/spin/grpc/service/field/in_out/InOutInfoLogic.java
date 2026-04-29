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
package org.spin.grpc.service.field.in_out;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.inout.GetInOutInfoRequest;
import org.spin.backend.grpc.field.inout.InOutInfo;
import org.spin.backend.grpc.field.inout.ListBusinessPartnersRequest;
import org.spin.backend.grpc.field.inout.ListInOutInfoRequest;
import org.spin.backend.grpc.field.inout.ListInOutInfoResponse;
import org.spin.backend.grpc.field.inout.ListInvoicesRequest;
import org.spin.backend.grpc.field.inout.ListShippersRequest;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.field.field_management.FieldManagementLogic;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

public class InOutInfoLogic {

	public static final String Table_Name = I_M_InOut.Table_Name;



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
		// Business Partner
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_InOut.COLUMNNAME_C_BPartner_ID, I_M_InOut.Table_Name,
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



	public static ListLookupItemsResponse.Builder listShippers(ListShippersRequest request) {
		// Shipper
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_InOut.COLUMNNAME_M_Shipper_ID, I_M_InOut.Table_Name,
			0,
			null
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



	public static ListLookupItemsResponse.Builder listInvoices(ListInvoicesRequest request) {
		String whereClause = "";
		if (!Util.isEmpty(request.getIsSalesTransaction(), true)) {
			boolean isSalesTransaction = BooleanManager.getBooleanFromString(
				request.getIsSalesTransaction()
			);
			if (isSalesTransaction) {
				whereClause = " C_Invoice.IsSOTrx = 'Y' ";
			} else {
				whereClause = " C_Invoice.IsSOTrx = 'N' ";
			}
		}
		// Invoice
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_InOut.COLUMNNAME_C_Invoice_ID, I_M_InOut.Table_Name,
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



	public static InOutInfo.Builder getInOutInfo(GetInOutInfoRequest request) {
		final int id = request.getId();
		final String uuid = request.getUuid();
		final String code = request.getCode();
		if (id <= 0 && Util.isEmpty(uuid, true) && Util.isEmpty(code, true)) {
			throw new AdempiereException("@FillMandatory@ @M_InOut_ID@ | @UUID@ | @DocumentNo@");
		}
		//
		String whereClause = "";
		List<Object> filtersList = new ArrayList<>();
		if (id > 0) {
			whereClause = "M_InOut_ID = ? ";
			filtersList.add(id);
		} else if (!Util.isEmpty(uuid, true)) {
			whereClause = "UUID = ? ";
			filtersList.add(uuid);
		} else if (!Util.isEmpty(code, true)) {
			whereClause = "DocumentNo = ? ";
			filtersList.add(code);
		}

		MInOut inOut = new Query(
			Env.getCtx(),
			Table_Name,
			whereClause,
			null
		)
			.setClient_ID()
			.setParameters(filtersList)
			// .setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.first()
		;

		InOutInfo.Builder builder = InOutInfoConvert.convertInOutInfo(inOut);

		return builder;
	}



	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListInOutInfoResponse.Builder listInOutInfo(ListInOutInfoRequest request) {
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

		StringBuffer whereClause = new StringBuffer(" 1=1 ");
		List<Object> parametersList = new ArrayList<>();

		// validate is active record
		if (request.getIsOnlyActiveRecords()) {
			whereClause.append("AND IsActive = ? ");
			parametersList.add(true);
		}

		// validation code of field
		if (!request.getIsWithoutValidation() && !Util.isEmpty(reference.ValidationCode, true)) {
			String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
				Table_Name,
				reference.ValidationCode
			);
			String parsedValidationCode = Env.parseContext(context, windowNo, validationCode, false);
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException(
					"@AD_Reference_ID@ " + reference.KeyColumn + ", @Code@/@WhereClause@ @Unparseable@"
				);
			}
			whereClause
				.append(" AND ")
				.append(parsedValidationCode)
			;
		}

		// URL decode to change characteres and add search value to filter
		final String searchValue = TextManager.getValidString(
			TextManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append(" AND ("
				+ "UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(POReference) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ") "
			);
			//	Add parameters
			parametersList.add(searchValue);
			parametersList.add(searchValue);
			parametersList.add(searchValue);
		}

		// Document No
		final String documentNo = TextManager.getDecodeUrl(
			request.getDocumentNo()
		);
		if (!Util.isEmpty(documentNo)) {
			whereClause.append(" AND UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' ");
			parametersList.add(documentNo);
		}
		// Business Partner
		final int businessPartnerId = request.getBusinessPartnerId();
		if (businessPartnerId > 0) {
			whereClause.append(" AND C_BPartner_ID = ? ");
			parametersList.add(
				businessPartnerId
			);
		}
		// Is Sales Transaction
		if (!Util.isEmpty(request.getIsSalesTransaction(), true)) {
			whereClause.append(" AND IsSOTrx = ? ");
			parametersList.add(
				BooleanManager.getBooleanToString(
					request.getIsSalesTransaction()
				)
			);
		}
		// Description
		final String description = TextManager.getDecodeUrl(
			request.getDescription()
		);
		if (!Util.isEmpty(description)) {
			whereClause.append(" AND UPPER(Description) LIKE '%' || UPPER(?) || '%' ");
			parametersList.add(description);
		}
		// Date Invoiced
		final Timestamp dateFrom = TimeManager.getTimestampFromProtoTimestamp(
			request.getMovementDateFrom()
		);
		final Timestamp dateTo = TimeManager.getTimestampFromProtoTimestamp(
			request.getMovementDateTo()
		);
		if (dateFrom != null || dateTo != null) {
			whereClause.append(" AND ");
			if (dateFrom != null && dateTo != null) {
				whereClause.append("TRUNC(MovementDate, 'DD') BETWEEN ? AND ? ");
				parametersList.add(dateFrom);
				parametersList.add(dateTo);
			}
			else if (dateFrom != null) {
				whereClause.append("TRUNC(MovementDate, 'DD') >= ? ");
				parametersList.add(dateFrom);
			}
			else {
				// DateTo != null
				whereClause.append("TRUNC(MovementDate, 'DD') <= ? ");
				parametersList.add(dateTo);
			}
		}
		// Purcharse Order Reference
		final String poReference = TextManager.getDecodeUrl(
			request.getOrderReference()
		);
		if (!Util.isEmpty(poReference)) {
			whereClause.append(" AND UPPER(POReference) LIKE '%' || UPPER(?) || '%' ");
			parametersList.add(poReference);
		}
		// Shipper
		final int shipperId = request.getShipperId();
		if (shipperId > 0) {
			whereClause.append(" AND M_Shipper_ID = ? ");
			parametersList.add(
				shipperId
			);
		}
		// Invoice
		final int invoiceId = request.getInvoiceId();
		if (invoiceId > 0) {
			whereClause.append(" AND C_Invoice_ID = ? ");
			parametersList.add(
				invoiceId
			);
		}

		Query query = new Query(
			context,
			Table_Name,
			whereClause.toString(),
			null
		)
			.setClient_ID()
			.setParameters(parametersList)
			.setOrderBy("MovementDate DESC, DocumentNo")
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		final int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		final int limit = LimitUtil.getPageSize(request.getPageSize());
		final int offset = (pageNumber - 1) * limit;
		final int recordCount = query.count();
		// Set page token
		String nexPageToken = null;
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListInOutInfoResponse.Builder responseBuilder = ListInOutInfoResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				TextManager.getValidString(
					nexPageToken
				)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.stream()
			.forEach(inOutId -> {
				InOutInfo.Builder builder = InOutInfoConvert.convertInOutInfo(inOutId);
				responseBuilder.addRecords(builder);
			})
		;

		return responseBuilder;
	}

}
