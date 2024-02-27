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
package org.spin.grpc.service.field.business_partner;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_BPartner_Location;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.field.business_partner.BusinessPartnerAddressLocation;
import org.spin.backend.grpc.field.business_partner.BusinessPartnerContact;
import org.spin.backend.grpc.field.business_partner.BusinessPartnerInfo;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerAddressLocationsRequest;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerAddressLocationsResponse;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerContactsRequest;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnerContactsResponse;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnersInfoRequest;
import org.spin.backend.grpc.field.business_partner.ListBusinessPartnersInfoResponse;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceInfo;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.ValueManager;

public class BusinessPartnerLogic {
	
	public static String tableName = I_C_BPartner.Table_Name;



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



	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListBusinessPartnersInfoResponse.Builder listBusinessPartnersInfo(ListBusinessPartnersInfoRequest request) {
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			request.getTableName()
		);

		//  Fill Cintext
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, context, request.getContextAttributes());

		StringBuffer whereClause = new StringBuffer("1=1");
		// validation code of field
		String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(tableName, reference.ValidationCode);
		String parsedValidationCode = Env.parseContext(context, windowNo, validationCode, false);
		if (!Util.isEmpty(reference.ValidationCode, true)) {
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException("@WhereClause@ @Unparseable@");
			}
			whereClause.append(" AND ").append(parsedValidationCode);
		}

		List<Object> parametersList = new ArrayList<>();
		//	For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append(" AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(TaxID) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name2) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ") "
			);
			//	Add parameters
			parametersList.add(searchValue);
			parametersList.add(searchValue);
			parametersList.add(searchValue);
			parametersList.add(searchValue);
			parametersList.add(searchValue);
		}

		// Customer
		if (!Util.isEmpty(request.getIsCustomer(), true)) {
			whereClause.append(" AND IsCustomer = ? ");
			parametersList.add(request.getIsCustomer());
		}
		// Vendor
		if (!Util.isEmpty(request.getIsVendor(), true)) {
			whereClause.append(" AND IsVendor = ? ");
			parametersList.add(request.getIsVendor());
		}
		// Value
		final String value = ValueManager.getDecodeUrl(
			request.getValue()
		);
		if (!Util.isEmpty(value)) {
			whereClause.append(" AND UPPER(Value) LIKE '%' || UPPER(?) || '%' ");
			parametersList.add(value);
		}
		// Name
		final String name = ValueManager.getDecodeUrl(
			request.getName()
		);
		if (!Util.isEmpty(name)) {
			whereClause.append(" AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ");
			parametersList.add(name);
		}
		// Contact
		final String contact = ValueManager.getDecodeUrl(
			request.getContact()
		);
		if (!Util.isEmpty(contact)) {
			whereClause.append(" AND C_BPartner.C_BPartner_ID IN (SELECT C_BPartner_ID FROM AD_User AS c ")
				.append("WHERE UPPER(c.Name) LIKE '%' || UPPER(?) || '%') ");
			parametersList.add(contact);
		}
		// E-Mail
		final String eMail = ValueManager.getDecodeUrl(
			request.getEmail()
		);
		if (!Util.isEmpty(eMail)) {
			whereClause.append(" AND C_BPartner.C_BPartner_ID IN (SELECT C_BPartner_ID FROM AD_User AS c ")
				.append("WHERE UPPER(c.EMail) LIKE '%' || UPPER(?) || '%') ");
			parametersList.add(eMail);
		}
		// Phone
		final String phone = ValueManager.getDecodeUrl(
			request.getPhone()
		);
		if (!Util.isEmpty(phone)) {
			whereClause.append(" AND C_BPartner.C_BPartner_ID IN (SELECT C_BPartner_ID FROM AD_User AS c ")
				.append("WHERE UPPER(c.Phone) LIKE '%' || UPPER(?) || '%') ");
			parametersList.add(phone);
		}
		// Postal Code
		final String postalCode = ValueManager.getDecodeUrl(
			request.getPostalCode()
		);
		if (!Util.isEmpty(postalCode)) {
			whereClause.append(" AND C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_BPartner_Location bpl, C_Location AS l ")
				.append("WHERE l.C_Location_ID = bpl.C_Location_ID AND UPPER(Postal) '%' || UPPER(?) || '%') ")
			;
			parametersList.add(postalCode);
		}

		Query query = new Query(
			context,
			I_C_BPartner.Table_Name,
			whereClause.toString(),
			null
		)
			.setClient_ID()
			.setParameters(parametersList)
			.setOrderBy(I_C_BPartner.COLUMNNAME_Value)
		;

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		// Set page token
		String nexPageToken = null;
		int recordCount = query.count();
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListBusinessPartnersInfoResponse.Builder builderList = ListBusinessPartnersInfoResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueManager.validateNull(
					nexPageToken
				)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.stream().forEach(businessPartnerId -> {
				BusinessPartnerInfo.Builder builder = BusinessPartnerConvert.convertBusinessPartner(businessPartnerId);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}


	public static ListBusinessPartnerContactsResponse.Builder listBusinessPartnerContacts(ListBusinessPartnerContactsRequest request) {
		MBPartner businessPartner = validateAndGetBusinessPartner(
			request.getBusinessPartnerId()
		);

		Query query = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			"C_BPartner_ID = ?",
			null
		)
			.setParameters(businessPartner.getC_BPartner_ID())
			.setOrderBy(I_AD_User.COLUMNNAME_AD_User_ID)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		ListBusinessPartnerContactsResponse.Builder builderList = ListBusinessPartnerContactsResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
		;

		query.getIDsAsList().stream().forEach(contactId -> {
			BusinessPartnerContact.Builder builder = BusinessPartnerConvert.convertBusinessPartnerContact(contactId);
			builderList.addRecords(builder);
		});

		return builderList;
	}


	public static ListBusinessPartnerAddressLocationsResponse.Builder listBusinessPartnerAddressLocations(ListBusinessPartnerAddressLocationsRequest request) {
		MBPartner businessPartner = validateAndGetBusinessPartner(
			request.getBusinessPartnerId()
		);

		final String whereClause = "C_BPartner_ID = ?";

		Query query = new Query(
			Env.getCtx(),
			I_C_BPartner_Location.Table_Name,
			whereClause,
			null
		)
			.setParameters(businessPartner.getC_BPartner_ID())
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		int recordCount = query.count();
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListBusinessPartnerAddressLocationsResponse.Builder builderList = ListBusinessPartnerAddressLocationsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueManager.validateNull(
					nexPageToken
				)
			)
		;

		query.getIDsAsList().parallelStream().forEach(businessPartnerLocationId -> {
			BusinessPartnerAddressLocation.Builder builder = BusinessPartnerConvert.convertBusinessPartnerLocationAddress(
				businessPartnerLocationId
			);
			builderList.addRecords(builder);
		});

		return builderList;
	}

}
