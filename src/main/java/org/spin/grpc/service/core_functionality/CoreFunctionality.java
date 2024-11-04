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
import java.util.Arrays;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Language;
import org.adempiere.core.domains.models.I_C_Country;
import org.adempiere.core.domains.models.I_C_UOM;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCountry;
import org.compiere.model.MLanguage;
import org.compiere.model.MPriceList;
import org.compiere.model.MSystem;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.backend.grpc.core_functionality.ConversionRate;
import org.spin.backend.grpc.core_functionality.Country;
import org.spin.backend.grpc.core_functionality.Currency;
import org.spin.backend.grpc.core_functionality.GetConversionRateRequest;
import org.spin.backend.grpc.core_functionality.GetCountryRequest;
import org.spin.backend.grpc.core_functionality.GetCurrencyRequest;
import org.spin.backend.grpc.core_functionality.GetPriceListRequest;
import org.spin.backend.grpc.core_functionality.GetSystemInfoRequest;
import org.spin.backend.grpc.core_functionality.GetUnitOfMeasureRequest;
import org.spin.backend.grpc.core_functionality.Language;
import org.spin.backend.grpc.core_functionality.ListLanguagesRequest;
import org.spin.backend.grpc.core_functionality.ListLanguagesResponse;
import org.spin.backend.grpc.core_functionality.ListProductConversionRequest;
import org.spin.backend.grpc.core_functionality.ListProductConversionResponse;
import org.spin.backend.grpc.core_functionality.PriceList;
import org.spin.backend.grpc.core_functionality.ProductConversion;
import org.spin.backend.grpc.core_functionality.SystemInfo;
import org.spin.backend.grpc.core_functionality.UnitOfMeasure;
import org.spin.backend.grpc.core_functionality.CoreFunctionalityGrpc.CoreFunctionalityImplBase;
import org.spin.base.Version;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.util.value.StringManager;
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
			String name = StringManager.getValidString(
				adempiereInfo.getName()
			);
			if (name.trim().equals("?")) {
				name = "";
			}
			builder.setName(name)
				.setReleaseNo(
					StringManager.getValidString(
						adempiereInfo.getReleaseNo()
					)
				)
				.setVersion(
					StringManager.getValidString(
						adempiereInfo.getVersion()
					)
				)
				.setLastBuildInfo(
					StringManager.getValidString(
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
				StringManager.getValidString(
					Version.MAIN_VERSION
				)
			)
			.setBackendImplementationVersion(
				StringManager.getValidString(
					Version.IMPLEMENTATION_VERSION
				)
			)
			.setLogoUrl(
				StringManager.getValidString(
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
				StringManager.getValidString(
					language.getAD_Language()
				)
			)
			.setCountryCode(
				StringManager.getValidString(
					language.getCountryCode()
				)
			)
			.setLanguageIso(
				StringManager.getValidString(
					language.getLanguageISO()
				)
			)
			.setLanguageName(
				StringManager.getValidString(
					language.getName()
				)
			)
			.setDatePattern(
				StringManager.getValidString(datePattern)
			)
			.setTimePattern(
				StringManager.getValidString(timePattern)
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
	public void getCurrency(GetCurrencyRequest request, StreamObserver<Currency> responseObserver) {
		try {
			Currency.Builder languagesList = getCurrency(request);
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

	private Currency.Builder getCurrency(GetCurrencyRequest request) {
		if (request.getId() <= 0 && Util.isEmpty(request.getCode(), true)) {
			throw new AdempiereException("@FillMandatory@ @C_Currency_ID@");
		}
		Currency.Builder builder = Currency.newBuilder();
		if (request.getId() > 0) {
			builder = CoreFunctionalityConvert.convertCurrency(
				request.getId()
			);
		} else if (Util.isEmpty(request.getCode())) {
			builder = CoreFunctionalityConvert.convertCurrency(
				request.getCode()
			);
		}

		return builder;
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
	public void getPriceList(GetPriceListRequest request, StreamObserver<PriceList> responseObserver) {
		try {
			PriceList.Builder languagesList = getPriceList(request);
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

	private PriceList.Builder getPriceList(GetPriceListRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_PriceList_ID@");
		}
		MPriceList priceList = MPriceList.get(Env.getCtx(), request.getId(), null);
		if (priceList == null || priceList.getM_PriceList_ID() <= 0) {
			throw new AdempiereException("@M_PriceList_ID@ @NotFound@");
		}
		PriceList.Builder builder = CoreFunctionalityConvert.convertPriceList(priceList);
		return builder;
	}



	@Override
	public void getUnitOfMeasure(GetUnitOfMeasureRequest request, StreamObserver<UnitOfMeasure> responseObserver) {
		try {
			UnitOfMeasure.Builder languagesList = getUnitOfMeasure(request);
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

	private UnitOfMeasure.Builder getUnitOfMeasure(GetUnitOfMeasureRequest request) {
		if (request.getId() <= 0 && Util.isEmpty(request.getCode(), true)) {
			throw new AdempiereException("@FillMandatory@ @C_UOM_ID@");
		}

		MUOM unitOfMeasure = null;
		if (request.getId() > 0) {
			unitOfMeasure = MUOM.get(Env.getCtx(), request.getId());
		} else if (Util.isEmpty(request.getCode())) {
			unitOfMeasure = new Query(
				Env.getCtx(),
				I_C_UOM.Table_Name,
				"X12DE355 = ?",
				null
			)
				.setParameters(request.getCode())
				.first()
			;
		}
		if (unitOfMeasure == null || unitOfMeasure.getC_UOM_ID() <= 0) {
			throw new AdempiereException("@C_UOM_ID@ @NotFound@");
		}
	
		UnitOfMeasure.Builder builder = CoreFunctionalityConvert.convertUnitOfMeasure(
			unitOfMeasure
		);
		return builder;
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

}
