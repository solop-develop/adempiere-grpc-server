/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.core.domains.models.I_AD_Language;
import org.adempiere.core.domains.models.I_AD_Org;
import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.core.domains.models.I_C_BP_Group;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_ConversionType;
import org.adempiere.core.domains.models.I_C_Country;
import org.adempiere.core.domains.models.I_C_Currency;
import org.adempiere.core.domains.models.I_C_POS;
import org.adempiere.core.domains.models.I_C_Region;
import org.adempiere.core.domains.models.I_C_UOM_Conversion;
import org.adempiere.core.domains.models.I_M_Warehouse;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCountry;
import org.compiere.model.MLanguage;
import org.compiere.model.MLocation;
import org.compiere.model.MOrg;
import org.compiere.model.MRole;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;
import org.spin.backend.grpc.common.BusinessPartner;
import org.spin.backend.grpc.common.ConversionRate;
import org.spin.backend.grpc.common.CoreFunctionalityGrpc.CoreFunctionalityImplBase;
import org.spin.backend.grpc.common.Country;
import org.spin.backend.grpc.common.CreateBusinessPartnerRequest;
import org.spin.backend.grpc.common.GetBusinessPartnerRequest;
import org.spin.backend.grpc.common.GetConversionRateRequest;
import org.spin.backend.grpc.common.GetCountryRequest;
import org.spin.backend.grpc.common.ListBusinessPartnersRequest;
import org.spin.backend.grpc.common.ListBusinessPartnersResponse;
import org.spin.backend.grpc.common.ListLanguagesRequest;
import org.spin.backend.grpc.common.ListLanguagesResponse;
import org.spin.backend.grpc.common.ListOrganizationsRequest;
import org.spin.backend.grpc.common.ListOrganizationsResponse;
import org.spin.backend.grpc.common.ListProductConversionRequest;
import org.spin.backend.grpc.common.ListProductConversionResponse;
import org.spin.backend.grpc.common.ListWarehousesRequest;
import org.spin.backend.grpc.common.ListWarehousesResponse;
import org.spin.backend.grpc.common.ProductConversion;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Core functionality
 */
public class CoreFunctionalityImplementation extends CoreFunctionalityImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(CoreFunctionalityImplementation.class);
	/**	Country */
	private static CCache<String, MCountry> countryCache = new CCache<String, MCountry>(I_C_Country.Table_Name + "_UUID", 30, 0);	//	no time-out


	@Override
	public void getCountry(GetCountryRequest request, StreamObserver<Country> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Country Request Null");
			}
			log.fine("Country Requested = " + request.getUuid());
			Country.Builder country = getCountry(request);
			responseObserver.onNext(country.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listLanguages(ListLanguagesRequest request, StreamObserver<ListLanguagesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLanguagesResponse.Builder languagesList = convertLanguagesList(request);
			responseObserver.onNext(languagesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getMessage())
					.augmentDescription(e.getMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request,
			StreamObserver<ListBusinessPartnersResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Add Line for Order = " + request.getSearchValue());
			ListBusinessPartnersResponse.Builder businessPartnerList = getBusinessPartnerList(request);
			responseObserver.onNext(businessPartnerList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void getBusinessPartner(GetBusinessPartnerRequest request,
			StreamObserver<BusinessPartner> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + request.getSearchValue());
			BusinessPartner.Builder businessPartner = getBusinessPartner(request);
			responseObserver.onNext(businessPartner.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void createBusinessPartner(CreateBusinessPartnerRequest request,
			StreamObserver<BusinessPartner> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + request.getValue());
			BusinessPartner.Builder businessPartner = createBusinessPartner(request);
			responseObserver.onNext(businessPartner.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void getConversionRate(GetConversionRateRequest request, StreamObserver<ConversionRate> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + request.getConversionTypeUuid());
			ConversionRate.Builder conversionRate = ConvertUtil.convertConversionRate(getConversionRate(request));
			responseObserver.onNext(conversionRate.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	/**
	 * Get conversion Rate from ValidFrom, Currency From, Currency To and Conversion Type
	 * @param request
	 * @return
	 */
	private MConversionRate getConversionRate(GetConversionRateRequest request) {
		if(Util.isEmpty(request.getConversionTypeUuid())
				|| Util.isEmpty(request.getCurrencyFromUuid())
				|| Util.isEmpty(request.getCurrencyToUuid())) {
			return null;
		}
		//	Get values
		Timestamp conversionDate = ValueUtil.convertStringToDate(request.getConversionDate());
		if(conversionDate == null) {
			conversionDate = TimeUtil.getDay(System.currentTimeMillis());
		}
		//	
		return RecordUtil.getConversionRate(Env.getAD_Org_ID(Env.getCtx()), 
				RecordUtil.getIdFromUuid(I_C_ConversionType.Table_Name, request.getConversionTypeUuid(), null), 
				RecordUtil.getIdFromUuid(I_C_Currency.Table_Name, request.getCurrencyFromUuid(), null), 
				RecordUtil.getIdFromUuid(I_C_Currency.Table_Name, request.getCurrencyToUuid(), null), 
				TimeUtil.getDay(conversionDate));
	}
	
	
	@Override
	public void listProductConversion(ListProductConversionRequest request, StreamObserver<ListProductConversionResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + request.getProductUuid());
			ListProductConversionResponse.Builder conversionProductList = convertListProductConversion(Env.getCtx(), request);
			responseObserver.onNext(conversionProductList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private ListProductConversionResponse.Builder convertListProductConversion(Properties context, ListProductConversionRequest request) {
		int productId = request.getProductId();
		if (!Util.isEmpty(request.getProductUuid(), true)) {
			productId = RecordUtil.getIdFromUuid(I_C_UOM_Conversion.Table_Name, request.getProductUuid(), null);
		} 

		if (productId <= 0) {
			throw new AdempiereException("@M_Product_ID@ @NotFound@");
		}

		ListProductConversionResponse.Builder productConversionListBuilder = ListProductConversionResponse.newBuilder();
		Arrays.asList(MUOMConversion.getProductConversions(context, productId)).forEach(conversion -> {
			ProductConversion.Builder productConversionBuilder = ConvertUtil.convertProductConversion(conversion);
			productConversionListBuilder.addProductConversion(productConversionBuilder);
		});
		
		return productConversionListBuilder;
	}

	/**
	 * List business partner
	 * @param context
	 * @param request
	 * @return
	 */
	private ListBusinessPartnersResponse.Builder getBusinessPartnerList(ListBusinessPartnersRequest request) {
		ListBusinessPartnersResponse.Builder builder = ListBusinessPartnersResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Get business partner list
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	For search value
		if(!Util.isEmpty(request.getSearchValue())) {
			whereClause.append("("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name2) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ")");
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}
		//	For value
		if(!Util.isEmpty(request.getValue())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("("
				+ "UPPER(Value) LIKE UPPER(?)"
				+ ")");
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("("
				+ "UPPER(Name) LIKE UPPER(?)"
				+ ")");
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("(EXISTS(SELECT 1 FROM AD_User u WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID AND UPPER(u.Name) LIKE UPPER(?)))");
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
				
			}
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("("
					+ "EXISTS(SELECT 1 FROM AD_User u WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID AND UPPER(u.Phone) LIKE UPPER(?)) "
					+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID AND UPPER(bpl.Phone) LIKE UPPER(?))"
					+ ")");
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
					+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
					+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
					+ "AND UPPER(l.Postal) LIKE UPPER(?)))");
			//	Add parameters
			parameters.add(request.getPostalCode());
		}
		//	
		String criteriaWhereClause = ValueUtil.getWhereClauseFromCriteria(request.getCriteria(), I_C_BPartner.Table_Name, parameters);
		if(whereClause.length() > 0
				&& !Util.isEmpty(criteriaWhereClause)) {
			whereClause.append(" AND (").append(criteriaWhereClause).append(")");
		}
		//	Get Product list
		Query query = new Query(Env.getCtx(), I_C_BPartner.Table_Name, 
				whereClause.toString(), null)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true);
		int count = query.count();
		query
		.setLimit(limit, offset)
		.<MBPartner>list()
		.forEach(businessPartner -> builder.addBusinessPartners(ConvertUtil.convertBusinessPartner(businessPartner)));
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(count > limit) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		return builder;
	}
	
	/**
	 * Create business partner
	 * @param request
	 * @return
	 */
	private BusinessPartner.Builder createBusinessPartner(CreateBusinessPartnerRequest request) {
		//	Validate name
		if(Util.isEmpty(request.getName())) {
			throw new AdempiereException("@Name@ @IsMandatory@");
		}
		//	POS Uuid
		if(Util.isEmpty(request.getPosUuid())) {
			throw new AdempiereException("@C_POS_ID@ @IsMandatory@");
		}
		MBPartner businessPartner = MBPartner.getTemplate(Env.getCtx(), Env.getAD_Client_ID(Env.getCtx()), RecordUtil.getIdFromUuid(I_C_POS.Table_Name, request.getPosUuid(), null));
		Trx.run(transactionName -> {
			//	Create it
			businessPartner.setAD_Org_ID(0);
			businessPartner.setIsCustomer (true);
			businessPartner.setIsVendor (false);
			businessPartner.set_TrxName(transactionName);
			//	Set Value
			String value = request.getValue();
			if(Util.isEmpty(value)) {
				value = DB.getDocumentNo(Env.getAD_Client_ID(Env.getCtx()), "C_BPartner", transactionName, businessPartner);
			}
			//	
			businessPartner.setValue(value);
			//	Tax Id
			if(!Util.isEmpty(request.getTaxId())) {
				businessPartner.setTaxID(request.getTaxId());
			}
			//	Duns
			if(!Util.isEmpty(request.getDuns())) {
				businessPartner.setDUNS(request.getDuns());
			}
			//	Naics
			if(!Util.isEmpty(request.getNaics())) {
				businessPartner.setNAICS(request.getNaics());
			}
			//	Name
			businessPartner.setName(request.getName());
			//	Last name
			if(!Util.isEmpty(request.getLastName())) {
				businessPartner.setName2(request.getLastName());
			}
			//	Description
			if(!Util.isEmpty(request.getDescription())) {
				businessPartner.setDescription(request.getDescription());
			}
			//	Business partner group
			if(!Util.isEmpty(request.getBusinessPartnerGroupUuid())) {
				int businessPartnerGroupId = RecordUtil.getIdFromUuid(I_C_BP_Group.Table_Name, request.getBusinessPartnerGroupUuid(), transactionName);
				if(businessPartnerGroupId != 0) {
					businessPartner.setC_BP_Group_ID(businessPartnerGroupId);
				}
			}
			//	Save it
			businessPartner.saveEx(transactionName);
			MUser contact = null;
			//	Contact
			if(!Util.isEmpty(request.getContactName()) || !Util.isEmpty(request.getEmail()) || !Util.isEmpty(request.getPhone())) {
				contact = new MUser(businessPartner);
				//	Name
				if(!Util.isEmpty(request.getContactName())) {
					contact.setName(request.getContactName());
				}
				//	EMail
				if(!Util.isEmpty(request.getEmail())) {
					contact.setEMail(request.getEmail());
				}
				//	Phone
				if(!Util.isEmpty(request.getPhone())) {
					contact.setPhone(request.getPhone());
				}
				//	Description
				if(!Util.isEmpty(request.getDescription())) {
					contact.setDescription(request.getDescription());
				}
				//	Save
				contact.saveEx(transactionName);
	 		}
			//	Location
			int countryId = 0;
			if(!Util.isEmpty(request.getCountryUuid())) {
				countryId = RecordUtil.getIdFromUuid(I_C_Country.Table_Name, request.getCountryUuid(), transactionName);
			}
			if(countryId <= 0) {
				countryId = Env.getContextAsInt(Env.getCtx(), "#C_Country_ID");
			}
			//	
			int regionId = 0;
			if(!Util.isEmpty(request.getRegionUuid())) {
				regionId = RecordUtil.getIdFromUuid(I_C_Region.Table_Name, request.getRegionUuid(), transactionName);
			}
			String cityName = null;
			int cityId = 0;
			//	City Name
			if(!Util.isEmpty(request.getCityName())) {
				cityName = request.getCityName();
			}
			//	City Reference
			if(!Util.isEmpty(request.getCityUuid())) {
				cityId = RecordUtil.getIdFromUuid(I_C_Region.Table_Name, request.getRegionUuid(), transactionName);
			}
			//	Instance it
			MLocation location = new MLocation(Env.getCtx(), countryId, regionId, cityName, transactionName);
			if(cityId > 0) {
				location.setC_City_ID(cityId);
			}
			//	Postal Code
			if(!Util.isEmpty(request.getPostalCode())) {
				location.setPostal(request.getPostalCode());
			}
			//	Address
			Optional.ofNullable(request.getAddress1()).ifPresent(address -> location.setAddress1(address));
			Optional.ofNullable(request.getAddress2()).ifPresent(address -> location.setAddress2(address));
			Optional.ofNullable(request.getAddress3()).ifPresent(address -> location.setAddress3(address));
			Optional.ofNullable(request.getAddress4()).ifPresent(address -> location.setAddress4(address));
			//	
			location.saveEx(transactionName);
			//	Create BP location
			MBPartnerLocation businessPartnerLocation = new MBPartnerLocation(businessPartner);
			businessPartnerLocation.setC_Location_ID(location.getC_Location_ID());
			//	Phone
			if(!Util.isEmpty(request.getPhone())) {
				businessPartnerLocation.setPhone(request.getPhone());
			}
			//	Contact
			if(!Util.isEmpty(request.getContactName())) {
				businessPartnerLocation.setContactPerson(request.getContactName());
			}
			//	Save
			businessPartnerLocation.saveEx(transactionName);
			//	Set Location
			Optional.ofNullable(contact).ifPresent(contactToSave -> {
				contactToSave.setC_BPartner_Location_ID(businessPartnerLocation.getC_BPartner_Location_ID());
				contactToSave.saveEx(transactionName);
			});
		});
		//	Default return
		return ConvertUtil.convertBusinessPartner(businessPartner);
	}
	
	/**
	 * Get business partner
	 * @param request
	 * @return
	 */
	private BusinessPartner.Builder getBusinessPartner(GetBusinessPartnerRequest request) {
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	For search value
		if(!Util.isEmpty(request.getSearchValue())) {
			whereClause.append("("
				+ "UPPER(Value) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?)"
				+ ")");
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}
		//	For value
		if(!Util.isEmpty(request.getValue())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("("
				+ "UPPER(Value) = UPPER(?)"
				+ ")");
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("("
				+ "UPPER(Name) = UPPER(?)"
				+ ")");
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("(EXISTS(SELECT 1 FROM AD_User u WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID AND UPPER(u.Name) = UPPER(?)))");
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("(EXISTS(SELECT 1 FROM AD_User u WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID AND UPPER(u.EMail) = UPPER(?)))");
			//	Add parameters
			parameters.add(request.getEmail());
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("("
					+ "EXISTS(SELECT 1 FROM AD_User u WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID AND UPPER(u.Phone) = UPPER(?)) "
					+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID AND UPPER(bpl.Phone) = UPPER(?))"
					+ ")");
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode())) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
					+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
					+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
					+ "AND UPPER(l.Postal) = UPPER(?)))");
			//	Add parameters
			parameters.add(request.getPostalCode());
		}
		//	
		String criteriaWhereClause = ValueUtil.getWhereClauseFromCriteria(request.getCriteria(), parameters);
		if(whereClause.length() > 0
				&& !Util.isEmpty(criteriaWhereClause)) {
			whereClause.append(" AND (").append(criteriaWhereClause).append(")");
		}
		//	Get business partner
		MBPartner businessPartner = new Query(Env.getCtx(), I_C_BPartner.Table_Name, 
				whereClause.toString(), null)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.first();
		//	Default return
		return ConvertUtil.convertBusinessPartner(businessPartner);
	}
	
	/**
	 * Convert a Country
	 * @param context
	 * @param request
	 * @return
	 */
	private Country.Builder getCountry(GetCountryRequest request) {
		String key = null;
		MCountry country = null;
		if(Util.isEmpty(request.getUuid()) && request.getId() == 0) {
			country = ContextManager.getDefaultCountry();
		}
		int id = request.getId();
		if(id <= 0) {
			id = RecordUtil.getIdFromUuid(I_C_Country.Table_Name, request.getUuid(), null);
		}
		//	
		if(id > 0
				&& country == null) {
			key = "ID:|" + request.getId();
			country = countryCache.put(key, country);
			if(country == null) {
				country = MCountry.get(Env.getCtx(), request.getId());
			}
		}
		if(!Util.isEmpty(key)
				&& country != null) {
			countryCache.put(key, country);
		}
		//	Return
		return ConvertUtil.convertCountry(Env.getCtx(), country);
	}
	
	/**
	 * Convert languages to gRPC
	 * @param request
	 * @return
	 */
	private ListLanguagesResponse.Builder convertLanguagesList(ListLanguagesRequest request) {
		ListLanguagesResponse.Builder builder = ListLanguagesResponse.newBuilder();
		new Query(Env.getCtx(), I_AD_Language.Table_Name, "(IsSystemLanguage=? OR IsBaseLanguage=?)", null)
			.setParameters(true, true)
			.setOnlyActiveRecords(true)
			.<MLanguage>list()
			.forEach(language -> {
				builder.addLanguages(ConvertUtil.convertLanguage(language));
			});
		//	Return
		return builder;
	}



	@Override
	public void listOrganizations(ListOrganizationsRequest request,
			StreamObserver<ListOrganizationsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListOrganizationsResponse.Builder organizationsList = listOrganizations(request);
			responseObserver.onNext(organizationsList.build());
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
	 * Convert Organization to list
	 * @param request
	 * @return
	 */
	private ListOrganizationsResponse.Builder listOrganizations(ListOrganizationsRequest request) {
		MRole role = null;
		if(request.getRoleId() != 0) {
			role = MRole.get(Env.getCtx(), request.getRoleId());
		} else if(!Util.isEmpty(request.getRoleUuid())) {
			role = new Query(Env.getCtx(), I_AD_Role.Table_Name, I_AD_Role.COLUMNNAME_UUID + " = ?", null)
				.setParameters(request.getRoleUuid())
				.setOnlyActiveRecords(true)
				.first();
		}
		//	get from role access
		if (role == null || !role.isActive()) {
			throw new AdempiereException("@AD_Role_ID@ @NotFound@");
		}

		List<Object> parameters = new ArrayList<Object>();
		String whereClause = "1 = 2";
		//	get from role access
		if (role.isAccessAllOrgs()) {
			whereClause = "(EXISTS(SELECT 1 FROM AD_Role r "
				+ "WHERE r.AD_Client_ID = AD_Org.AD_Client_ID "
				+ "AND r.AD_Role_ID = ? "
				+ "AND r.IsActive = 'Y') "
				+ "OR AD_Org_ID = 0) "
			;
			parameters.add(role.getAD_Role_ID());
		} else {
			if(role.isUseUserOrgAccess()) {
				whereClause = "EXISTS(SELECT 1 FROM AD_User_OrgAccess ua "
					+ "WHERE ua.AD_Org_ID = AD_Org.AD_Org_ID "
					+ "AND ua.AD_User_ID = ? "
					+ "AND ua.IsActive = 'Y')";
				parameters.add(Env.getAD_User_ID(Env.getCtx()));
			} else {
				whereClause = "EXISTS(SELECT 1 FROM AD_Role_OrgAccess ra "
					+ "WHERE ra.AD_Org_ID = AD_Org.AD_Org_ID "
					+ "AND ra.AD_Role_ID = ? "
					+ "AND ra.IsActive = 'Y')";
				parameters.add(role.getAD_Role_ID());
			}
		}
		whereClause += " AND IsSummary = ? ";
		parameters.add(false);

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		Query query = new Query(Env.getCtx(), I_AD_Org.Table_Name, whereClause, null)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
		;
		//	Count
		int count = query.count();

		ListOrganizationsResponse.Builder builder = ListOrganizationsResponse.newBuilder();
		//	Get List
		query.setLimit(limit, offset)
			.<MOrg>list()
			.forEach(organization -> {
				builder.addOrganizations(ConvertUtil.convertOrganization(organization));
			});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(count > limit) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set netxt page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		return builder;
	}



	@Override
	public void listWarehouses(ListWarehousesRequest request, StreamObserver<ListWarehousesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListWarehousesResponse.Builder organizationsList = listWarehouses(request);
			responseObserver.onNext(organizationsList.build());
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
	 * Convert warehouses list
	 * @param request
	 * @return
	 */
	private ListWarehousesResponse.Builder listWarehouses(ListWarehousesRequest request) {
		ListWarehousesResponse.Builder builder = ListWarehousesResponse.newBuilder();
		//	Get page and count
		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		int organizationId = request.getOrganizationId();
		if (organizationId <= 0) {
			organizationId = RecordUtil.getIdFromUuid(I_AD_Org.Table_Name, request.getOrganizationUuid(), null);
		}

		final String whereClause = "AD_Org_ID = ? AND IsInTransit = ? ";
		Query query = new Query(
			Env.getCtx(),
			I_M_Warehouse.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(organizationId, false)
		;
		//	Count
		int count = query.count();
		//	Get List
		query.setLimit(limit, offset)
			.<MWarehouse>list()
			.forEach(warehouse -> {
				builder.addWarehouses(ConvertUtil.convertWarehouse(warehouse));
			});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(count > limit) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set netxt page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		return builder;
	}
}
