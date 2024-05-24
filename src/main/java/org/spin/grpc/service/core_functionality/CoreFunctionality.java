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
package org.spin.grpc.service.core_functionality;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Language;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_Country;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCountry;
import org.compiere.model.MLanguage;
import org.compiere.model.MLocation;
import org.compiere.model.MRole;
import org.compiere.model.MSystem;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.core_functionality.BusinessPartner;
import org.spin.backend.grpc.core_functionality.ConversionRate;
import org.spin.backend.grpc.core_functionality.Country;
import org.spin.backend.grpc.core_functionality.CreateBusinessPartnerRequest;
import org.spin.backend.grpc.core_functionality.GetBusinessPartnerRequest;
import org.spin.backend.grpc.core_functionality.GetConversionRateRequest;
import org.spin.backend.grpc.core_functionality.GetCountryRequest;
import org.spin.backend.grpc.core_functionality.GetSystemInfoRequest;
import org.spin.backend.grpc.core_functionality.Language;
import org.spin.backend.grpc.core_functionality.ListBusinessPartnersRequest;
import org.spin.backend.grpc.core_functionality.ListBusinessPartnersResponse;
import org.spin.backend.grpc.core_functionality.ListLanguagesRequest;
import org.spin.backend.grpc.core_functionality.ListLanguagesResponse;
import org.spin.backend.grpc.core_functionality.ListProductConversionRequest;
import org.spin.backend.grpc.core_functionality.ListProductConversionResponse;
import org.spin.backend.grpc.core_functionality.ProductConversion;
import org.spin.backend.grpc.core_functionality.SystemInfo;
import org.spin.backend.grpc.core_functionality.CoreFunctionalityGrpc.CoreFunctionalityImplBase;
import org.spin.base.Version;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Core functionality
 */
public class CoreFunctionality extends CoreFunctionalityImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(CoreFunctionality.class);
	/**	Country */
	private static CCache<String, MCountry> countryCache = new CCache<String, MCountry>(I_C_Country.Table_Name + "_UUID", 30, 0);	//	no time-out


	@Override
	public void getSystemInfo(GetSystemInfoRequest request, StreamObserver<SystemInfo> responseObserver) {
		try {
			SystemInfo.Builder builder = getSystemInfo();
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

	private SystemInfo.Builder getSystemInfo() {
		SystemInfo.Builder builder = SystemInfo.newBuilder();

		MSystem adempiereInfo = MSystem.get(Env.getCtx());
		if (adempiereInfo != null) {
			String name = ValueManager.validateNull(
				adempiereInfo.getName()
			);
			if (name.trim().equals("?")) {
				name = "";
			}
			builder.setName(name)
				.setReleaseNo(
					ValueManager.validateNull(
						adempiereInfo.getReleaseNo()
					)
				)
				.setVersion(
					ValueManager.validateNull(
						adempiereInfo.getVersion()
					)
				)
				.setLastBuildInfo(
					ValueManager.validateNull(
						adempiereInfo.getLastBuildInfo()
					)
				)
			;
		}
		
		// backend info
		builder.setBackendDateVersion(
				ValueManager.getTimestampFromDate(
					TimeManager.getTimestampFromString(Version.DATE_VERSION)
				)
			)
			.setBackendMainVersion(
				ValueManager.validateNull(
					Version.MAIN_VERSION
				)
			)
			.setBackendImplementationVersion(
				ValueManager.validateNull(
					Version.IMPLEMENTATION_VERSION
				)
			)
			.setLogoUrl(
				ValueManager.validateNull(
					System.getenv("SYSTEM_LOGO_URL")
				)
			)
		;

		return builder;
	}


	@Override
	public void getCountry(GetCountryRequest request, StreamObserver<Country> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Country Request Null");
			}
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

	/**
	 * Convert a Country
	 * @param context
	 * @param request
	 * @return
	 */
	private Country.Builder getCountry(GetCountryRequest request) {
		String key = null;
		MCountry country = null;
		if(request.getId() <= 0) {
			country = ContextManager.getDefaultCountry();
		}
		int id = request.getId();
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
		return CoreFunctionalityConvert.convertCountry(Env.getCtx(), country);
	}



	/**
	 * TODO: Duplicated with Security service
	 */
	@Override
	public void listLanguages(ListLanguagesRequest request, StreamObserver<ListLanguagesResponse> responseObserver) {
		try {
			ListLanguagesResponse.Builder languagesList = listLanguages(request);
			responseObserver.onNext(languagesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getMessage())
					.augmentDescription(e.getMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert languages to gRPC
	 * @param request
	 * @return
	 */
	private ListLanguagesResponse.Builder listLanguages(ListLanguagesRequest request) {
		Query query = new Query(
			Env.getCtx(),
			I_AD_Language.Table_Name,
			"(IsSystemLanguage=? OR IsBaseLanguage=?)",
			null
			)
			.setParameters(true, true)
			.setOnlyActiveRecords(true)
		;

		ListLanguagesResponse.Builder builder = ListLanguagesResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
		;
		query
			.<MLanguage>list()
			.forEach(language -> {
				Language.Builder languaBuilder = convertLanguage(language);
				builder.addLanguages(
					languaBuilder
				);
			})
		;
		//	Return
		return builder;
	}

	/**
	 * Convert Language to gRPC
	 * @param language
	 * @return
	 */
	private static Language.Builder convertLanguage(MLanguage language) {
		Language.Builder languaBuilder = Language.newBuilder();
		if (language == null || language.getAD_Language_ID() <= 0) {
			return languaBuilder;
		}

		String datePattern = language.getDatePattern();
		String timePattern = language.getTimePattern();
		if(Util.isEmpty(datePattern, true)) {
			org.compiere.util.Language staticLanguage = org.compiere.util.Language.getLanguage(language.getAD_Language());
			if(staticLanguage != null) {
				datePattern = staticLanguage.getDateFormat().toPattern();
			}
			//	Validate
			if(Util.isEmpty(datePattern, true)) {
				datePattern = language.getDateFormat().toPattern();
			}
		}
		if(Util.isEmpty(timePattern, true)) {
			org.compiere.util.Language staticLanguage = org.compiere.util.Language.getLanguage(language.getAD_Language());
			if(staticLanguage != null) {
				timePattern = staticLanguage.getTimeFormat().toPattern();
			}
		}
		return languaBuilder
			.setLanguage(
				ValueManager.validateNull(
					language.getAD_Language()
				)
			)
			.setCountryCode(
				ValueManager.validateNull(
					language.getCountryCode()
				)
			)
			.setLanguageIso(
				ValueManager.validateNull(
					language.getLanguageISO()
				)
			)
			.setLanguageName(
				ValueManager.validateNull(
					language.getName()
				)
			)
			.setDatePattern(
				ValueManager.validateNull(datePattern)
			)
			.setTimePattern(
				ValueManager.validateNull(timePattern)
			)
			.setIsBaseLanguage(
				language.isBaseLanguage()
			)
			.setIsSystemLanguage(
				language.isSystemLanguage()
			)
			.setIsDecimalPoint(
				language.isDecimalPoint()
			)
		;
	}



	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request,
			StreamObserver<ListBusinessPartnersResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListBusinessPartnersResponse.Builder businessPartnerList = getBusinessPartnerList(request);
			responseObserver.onNext(businessPartnerList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	
	@Override
	public void getBusinessPartner(GetBusinessPartnerRequest request,
			StreamObserver<BusinessPartner> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			BusinessPartner.Builder businessPartner = getBusinessPartner(request);
			responseObserver.onNext(businessPartner.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	
	@Override
	public void createBusinessPartner(CreateBusinessPartnerRequest request,
			StreamObserver<BusinessPartner> responseObserver) {
		try {
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
			ConversionRate.Builder conversionRate = CoreFunctionalityConvert.convertConversionRate(
				getConversionRate(request)
			);
			responseObserver.onNext(conversionRate.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Get conversion Rate from ValidFrom, Currency From, Currency To and Conversion Type
	 * @param request
	 * @return
	 */
	private MConversionRate getConversionRate(GetConversionRateRequest request) {
		if(request.getConversionTypeId() <= 0
				|| request.getCurrencyFromId() <= 0
				|| request.getCurrencyToId() <= 0) {
			return null;
		}
		//	Get values
		Timestamp conversionDate = ValueManager.getDateFromTimestampDate(request.getConversionDate());
		if(conversionDate == null) {
			conversionDate = TimeUtil.getDay(System.currentTimeMillis());
		}
		//	
		return RecordUtil.getConversionRate(Env.getAD_Org_ID(Env.getCtx()), 
				request.getConversionTypeId(), 
				request.getCurrencyFromId(), 
				request.getCurrencyToId(), 
				TimeUtil.getDay(conversionDate));
	}
	
	
	@Override
	public void listProductConversion(ListProductConversionRequest request, StreamObserver<ListProductConversionResponse> responseObserver) {
		try {
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
		int productId = request.getId();
		if (productId <= 0) {
			throw new AdempiereException("@M_Product_ID@ @NotFound@");
		}

		ListProductConversionResponse.Builder productConversionListBuilder = ListProductConversionResponse.newBuilder();
		Arrays.asList(MUOMConversion.getProductConversions(context, productId)).forEach(conversion -> {
			ProductConversion.Builder productConversionBuilder = CoreFunctionalityConvert.convertProductConversion(conversion);
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
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Get business partner list
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		//	For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append("("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name2) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ")");
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
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

		query.setLimit(limit, offset)
			.getIDsAsList()
			.parallelStream()
			.forEach(businessPartnerId -> {
				BusinessPartner.Builder businessPartnerBuilder = CoreFunctionalityConvert.convertBusinessPartner(businessPartnerId);
				builder.addBusinessPartners(businessPartnerBuilder);
			});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(count > limit) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);
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
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @IsMandatory@");
		}
		MBPartner businessPartner = MBPartner.getTemplate(Env.getCtx(), Env.getAD_Client_ID(Env.getCtx()), request.getPosId());
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
			if(request.getBusinessPartnerGroupId() > 0) {
				businessPartner.setC_BP_Group_ID(request.getBusinessPartnerGroupId());
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
			if(request.getCountryId() > 0) {
				countryId = request.getCountryId();
			}
			if(countryId <= 0) {
				countryId = Env.getContextAsInt(Env.getCtx(), "#C_Country_ID");
			}
			//	
			int regionId = 0;
			if(request.getRegionId() > 0) {
				regionId = request.getRegionId();
			}
			String cityName = null;
			int cityId = 0;
			//	City Name
			if(!Util.isEmpty(request.getCityName())) {
				cityName = request.getCityName();
			}
			//	City Reference
			if(request.getCityId() > 0) {
				cityId = request.getCityId();
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
		return CoreFunctionalityConvert.convertBusinessPartner(
			businessPartner
		);
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
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append("("
				+ "UPPER(Value) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?)"
				+ ")");
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
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
		String criteriaWhereClause = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), parameters);
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
		return CoreFunctionalityConvert.convertBusinessPartner(
			businessPartner
		);
	}

}
