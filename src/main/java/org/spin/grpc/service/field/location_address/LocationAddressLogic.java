package org.spin.grpc.service.field.location_address;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_City;
import org.adempiere.core.domains.models.I_C_Country;
import org.adempiere.core.domains.models.I_C_Region;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCity;
import org.compiere.model.MCountry;
import org.compiere.model.MLocation;
import org.compiere.model.MRegion;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.backend.grpc.field.location_address.Address;
import org.spin.backend.grpc.field.location_address.Country;
import org.spin.backend.grpc.field.location_address.CreateAddressRequest;
import org.spin.backend.grpc.field.location_address.GetAddressRequest;
import org.spin.backend.grpc.field.location_address.GetCountryRequest;
import org.spin.backend.grpc.field.location_address.ListCitiesRequest;
import org.spin.backend.grpc.field.location_address.ListCitiesResponse;
import org.spin.backend.grpc.field.location_address.ListCountriesRequest;
import org.spin.backend.grpc.field.location_address.ListCountriesResponse;
import org.spin.backend.grpc.field.location_address.ListItem;
import org.spin.backend.grpc.field.location_address.ListRegionsRequest;
import org.spin.backend.grpc.field.location_address.ListRegionsResponse;
import org.spin.backend.grpc.field.location_address.UpdateAddressRequest;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

public class LocationAddressLogic {

	private static MCountry validateAndCountry(int countryId) {
		if (countryId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Country_ID@");
		}
		MCountry country = MCountry.get(Env.getCtx(), countryId);
		if (country == null || country.getC_Country_ID() <= 0) {
			throw new AdempiereException("@C_Country_ID@ @NotFound@");
		}
		return country;
	}


	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListCountriesResponse.Builder listCountries(ListCountriesRequest request) {
		String whereClause = null;
		List<Object> parameters = new ArrayList<Object>();

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			whereClause = "("
				+ "UPPER(CountryCode) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?) "
				+ ")"
			;
			parameters.add(searchValue);
			parameters.add(searchValue);
		}
		Query query = new Query(
			Env.getCtx(),
			I_C_Country.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
			.setOrderBy(I_C_Country.COLUMNNAME_Name)
		;

		ListCountriesResponse.Builder builder = ListCountriesResponse.newBuilder()
			.setRecordCount(query.count())
		;

		String sessionLanguage = Env.getAD_Language(Env.getCtx());
		boolean isBaseLangague = Language.isBaseLanguage(sessionLanguage);
		query.getIDsAsList().forEach(countryId -> {
			MCountry country = MCountry.get(Env.getCtx(), countryId);
			String name = country.getName();
			if (!isBaseLangague) {
				name = country.get_Translation(
					I_C_Country.COLUMNNAME_Name
				);
			}
			ListItem.Builder countryIntem = ListItem.newBuilder()
				.setId(country.getC_Country_ID())
				.setName(
					ValueManager.validateNull(
						name
					)
				)
			;
			builder.addCountries(countryIntem);
		});

		return builder;
	}

	public static Country.Builder getCountry(GetCountryRequest request) {
		if (request.getId() <= 0 && Util.isEmpty(request.getCountryCode(), true)) {
			throw new AdempiereException("@FillMandatory@ @CountryCode@ / @C_Country_ID@");
		}
		MCountry country = null;
		if (request.getId() > 0) {
			country = MCountry.get(Env.getCtx(), request.getId());
		} else if (!Util.isEmpty(request.getCountryCode(), true)) {
			country = MCountry.get(Env.getCtx(), request.getCountryCode());
		}
		if (country == null || country.getC_Country_ID() <= 0) {
			throw new AdempiereException("@C_Country_ID@ @NotFound@");
		}
		Country.Builder builder = LocationAddressConvertUtil.convertCountry(
			country
		);
		return builder;
	}

	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListRegionsResponse.Builder listRegions(ListRegionsRequest request) {
		MCountry country = validateAndCountry(
			request.getCountryId()
		);

		String whereClause = "C_Country_ID = ? ";
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(country.getC_Country_ID());

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			whereClause = "AND ("
				+ "UPPER(CountryCode) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?) "
				+ ")"
			;
			parameters.add(searchValue);
			parameters.add(searchValue);
		}
		Query query = new Query(
			Env.getCtx(),
			I_C_Region.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
		;

		ListRegionsResponse.Builder builder = ListRegionsResponse.newBuilder()
			.setRecordCount(query.count())
		;

		query.getIDsAsList()
			.parallelStream()
			.forEach(regionId -> {
				MRegion region = MRegion.get(Env.getCtx(), regionId);
				ListItem.Builder regionItem = ListItem.newBuilder()
					.setId(region.getC_Region_ID())
					.setName(
						ValueManager.validateNull(
							region.getName()
						)
					)
				;
				builder.addRegions(regionItem);
			});

		return builder;
	}

	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListCitiesResponse.Builder listCities(ListCitiesRequest request) {
		MCountry country = validateAndCountry(
			request.getCountryId()
		);

		String whereClause = "C_Country_ID = ? ";
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(country.getC_Country_ID());

		if (country.isHasRegion()) {
			if (request.getRegionId() > 0) {
				whereClause += "AND C_Region_ID = ? ";
				parameters.add(request.getRegionId());
			}
		}

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += "AND ("
				+ "UPPER(CountryCode) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?) "
				+ ")"
			;
			parameters.add(searchValue);
			parameters.add(searchValue);
		}
		Query query = new Query(
			Env.getCtx(),
			I_C_City.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
		;

		ListCitiesResponse.Builder builder = ListCitiesResponse.newBuilder()
			.setRecordCount(query.count())
		;

		query.getIDsAsList()
			.parallelStream()
			.forEach(cityId -> {
				MCity city = MCity.get(Env.getCtx(), cityId);
				ListItem.Builder cityItem = ListItem.newBuilder()
					.setId(city.getC_City_ID())
					.setName(
						ValueManager.validateNull(
							city.getName()
						)
					)
				;
				builder.addCities(cityItem);
			});

		return builder;
	}


	public static MLocation validateAndGetAddress(int addressId) {
		if (addressId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Location_ID@");
		}
		MLocation address = MLocation.get(Env.getCtx(), addressId, null);
		if (address == null || address.getC_Country_ID() <= 0) {
			throw new AdempiereException("@C_Location_ID@ @NotFound@");
		}
		return address;
	}

	public static Address.Builder createAddress(CreateAddressRequest request) {
		if (request.getCountryId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Country_ID@");
		}

		MLocation address = new MLocation(Env.getCtx(), 0, null);

		address.setC_Country_ID(
			request.getCountryId()
		);

		if (request.getRegionId() > 0) {
			address.setC_Region_ID(
				request.getRegionId()
			);
		}
		if (request.getCityId() > 0) {
			address.setC_City_ID(
				request.getCityId()
			);
		}
		if (!Util.isEmpty(request.getCity(), true)) {
			address.setCity(
				request.getCity()
			);
		}
		if (!Util.isEmpty(request.getAddress1(), true)) {
			address.setAddress1(
				request.getAddress1()
			);
		}
		if (!Util.isEmpty(request.getAddress2(), true)) {
			address.setAddress2(
				request.getAddress2()
			);
		}
		if (!Util.isEmpty(request.getAddress3(), true)) {
			address.setAddress3(
				request.getAddress3()
			);
		}
		if (!Util.isEmpty(request.getAddress4(), true)) {
			address.setAddress4(
				request.getAddress1()
			);
		}
		if (!Util.isEmpty(request.getPostalCode(), true)) {
			address.setPostal(
				request.getPostalCode()
			);
		}
		if (!Util.isEmpty(request.getPosalCodeAdditional(), true)) {
			address.setPostal_Add(
				request.getPosalCodeAdditional()
			);
		}
		if (!Util.isEmpty(request.getLatitude(), true)) {
			BigDecimal latitude = NumberManager.getBigDecimalFromString(
				request.getLatitude()
			);
			address.setLatitude(latitude);
		}
		if (!Util.isEmpty(request.getLongitude(), true)) {
			BigDecimal longitude = NumberManager.getBigDecimalFromString(
				request.getLongitude()
			);
			address.setLongitude(longitude);
		}
		if (!Util.isEmpty(request.getLatitude(), true)) {
			BigDecimal latitude = NumberManager.getBigDecimalFromString(
				request.getLatitude()
			);
			address.setLatitude(latitude);
		}

		address.saveEx();

		Address.Builder builder = LocationAddressConvertUtil.convertAddress(
			address
		);
		return builder;
	}

	public static Address.Builder updateAddress(UpdateAddressRequest request) {
		MLocation address = validateAndGetAddress(request.getId());

		if (request.getCountryId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Country_ID@");
		}

		address.setC_Country_ID(
			request.getCountryId()
		);
		address.setC_Region_ID(
			request.getRegionId()
		);
		address.setC_City_ID(
			request.getCityId()
		);
		address.setCity(
			request.getCity()
		);

		address.setAddress1(
			request.getAddress1()
		);
		address.setAddress2(
			request.getAddress2()
		);
		address.setAddress3(
			request.getAddress3()
		);
		address.setAddress4(
			request.getAddress1()
		);
		address.setPostal(
			request.getPostalCode()
		);
		address.setPostal_Add(
			request.getPosalCodeAdditional()
		);

		BigDecimal latitude = NumberManager.getBigDecimalFromString(
			request.getLatitude()
		);
		address.setLatitude(latitude);

		BigDecimal longitude = NumberManager.getBigDecimalFromString(
			request.getLongitude()
		);
		address.setLongitude(longitude);

		BigDecimal altitude = NumberManager.getBigDecimalFromString(
			request.getAltitude()
		);
		address.setAltitude(altitude);

		if (address.is_Changed()) {
			address.saveEx();
		}

		Address.Builder builder = LocationAddressConvertUtil.convertAddress(
			address
		);
		return builder;
	}


	public static Address.Builder getAddress(GetAddressRequest request) {
		MLocation address = validateAndGetAddress(request.getId());
		Address.Builder builder = LocationAddressConvertUtil.convertAddress(
			address
		);
		return builder;
	}

}
