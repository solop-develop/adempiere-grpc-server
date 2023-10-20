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

package org.spin.pos.service;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_BPartner;
import org.compiere.model.MBPartner;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.Customer;
import org.spin.backend.grpc.pos.GetCustomerRequest;
import org.spin.backend.grpc.pos.ListCustomersRequest;
import org.spin.backend.grpc.pos.ListCustomersResponse;
import org.spin.base.db.LimitUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.pos.util.POSConvertUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.ValueManager;

public class POSLogic {

	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static ListCustomersResponse.Builder listCustomers(ListCustomersRequest request) {
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	For search value
		if(!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause.append("("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name2) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}
		//	For value
		if(!Util.isEmpty(request.getValue(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Value) LIKE UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Name) LIKE UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Name) LIKE UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.EMail) LIKE UPPER(?)))"
			);
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"("
				+ "EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Phone) LIKE UPPER(?)) "
				+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(bpl.Phone) LIKE UPPER(?))"
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(l.Postal) LIKE UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getPostalCode());
		}
		//	
		String criteriaWhereClause = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), I_C_BPartner.Table_Name, parameters);
		if(whereClause.length() > 0
				&& !Util.isEmpty(criteriaWhereClause)) {
			whereClause.append(" AND (").append(criteriaWhereClause).append(")");
		}

		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			I_C_BPartner.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		int count = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListCustomersResponse.Builder builderList = ListCustomersResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueManager.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.stream()
			.forEach(businessPartnerId -> {
				Customer.Builder customBuilder = POSConvertUtil.convertCustomer(
					businessPartnerId
				);
				builderList.addCustomers(customBuilder);
			});
	
		//	Default return
		return builderList;
	}

	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static Customer.Builder getCustomer(GetCustomerRequest request) {
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	For search value
		if(!Util.isEmpty(request.getSearchValue(), true)) {
			// TODO: Check if it is better with the `LIKE` operator 
			whereClause.append(
				"(UPPER(Value) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}
		//	For value
		if(!Util.isEmpty(request.getValue(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Value) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Name) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Name) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.EMail) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getEmail());
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"("
				+ "EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Phone) = UPPER(?)) "
				+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(bpl.Phone) = UPPER(?))"
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(l.Postal) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getPostalCode());
		}

		//	Get business partner
		MBPartner businessPartner = new Query(
			Env.getCtx(),
			I_C_BPartner.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.first()
		;
		//	Default return
		return POSConvertUtil.convertCustomer(
			businessPartner
		);
	}

}
