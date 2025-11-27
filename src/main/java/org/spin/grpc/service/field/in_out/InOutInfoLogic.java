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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.inout.GetInOutInfoRequest;
import org.spin.backend.grpc.inout.ListInOutInfoRequest;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.TextManager;

public class InOutInfoLogic {

	public static final String Table_Name = I_M_InOut.Table_Name;



	public static Entity.Builder getInOutInfo(GetInOutInfoRequest request) {
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

		// Entity.Builder builder = BusinessPartnerConver.convertBusinessPartner(
		// 	businessPartner
		// );

		return Entity.newBuilder();
	}



	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListEntitiesResponse.Builder listInOutInfo(ListInOutInfoRequest request) {
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
		MTable table = MTable.get(Env.getCtx(), Table_Name);
		StringBuilder sql = new StringBuilder(QueryUtil.getTableQueryWithReferences(table));

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql.toString(),
				null,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer(" 1=1 ");
		// validation code of field
		if (!request.getIsWithoutValidation()) {
			String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
				Table_Name,
				reference.ValidationCode
			);
			if (!Util.isEmpty(reference.ValidationCode, true)) {
				String parsedValidationCode = Env.parseContext(Env.getCtx(), windowNo, validationCode, false);
				if (Util.isEmpty(parsedValidationCode, true)) {
					throw new AdempiereException(
						"@Reference@ " + reference.KeyColumn + ", @Code@/@WhereClause@ @Unparseable@"
					);
				}
				whereClause.append(" AND ")
					.append(parsedValidationCode)
				;
			}
		}

		//	For dynamic condition
		List<Object> params = new ArrayList<>(); // includes on filters criteria
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), Table_Name, params);
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

		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, Table_Name, request.getSearchValue(), params);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		
		//	Count records
		count = CountUtil.countRecords(parsedSQL, Table_Name, params);
		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(MTable.get(Env.getCtx(), Table_Name), parsedSQL, params);
		//	
		builder.setRecordCount(count);
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(
			TextManager.getValidString(
				nexPageToken
			)
		);

		return builder;
	}

}
