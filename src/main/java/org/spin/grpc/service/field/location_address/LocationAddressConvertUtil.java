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
package org.spin.grpc.service.field.location_address;

import org.adempiere.core.domains.models.I_C_Country;
import org.compiere.model.MCity;
import org.compiere.model.MCountry;
import org.compiere.model.MLocation;
import org.compiere.model.MRegion;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.field.location_address.Address;
import org.spin.backend.grpc.field.location_address.Country;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;

public class LocationAddressConvertUtil {

	public static Country.Builder convertCountry(MCountry country) {
		Country.Builder builder = Country.newBuilder();
		if (country == null || country.getC_Country_ID() <= 0) {
			return builder;
		}

		builder.setId(
				country.getC_Country_ID()
			)
			.setUuid(
				StringManager.getValidString(
					country.getUUID()
				)
			)
			.setCountryCode(
				StringManager.getValidString(
					country.getCountryCode()
				)
			)
			.setName(
				StringManager.getValidString(
					country.get_Translation(
						I_C_Country.COLUMNNAME_Name
					)
				)
			)
			.setIsHasRegion(
				country.isHasRegion()
			)
			.setRegionName(
				StringManager.getValidString(
					country.get_Translation(
						I_C_Country.COLUMNNAME_RegionName
					)
				)
			)
			.setCaptureSequence(
				StringManager.getValidString(
					country.getCaptureSequence()
				)
			)
			.setDisplaySequence(
				StringManager.getValidString(
					country.getDisplaySequence()
				)
			)
			.setIsAddressLinesReverse(
				country.isAddressLinesReverse()
			)
			.setDisplaySequenceLocal(
				StringManager.getValidString(
					country.getDisplaySequenceLocal()
				)
			)
			.setIsAddressLinesLocalReverse(
				country.isAddressLinesLocalReverse()
			)
			.setExpressionPostalCode(
				StringManager.getValidString(
					country.getExpressionPostal()
				)
			)
			.setIsHasPostalCodeAdditional(
				country.isHasPostal_Add()
			)
			.setExpressionPostalCodeAdditional(
				StringManager.getValidString(
					country.getExpressionPostal_Add()
				)
			)
			.setIsAllowCitiesOutOfList(
				country.isAllowCitiesOutOfList()
			)
		;
		return builder;
	}


	public static Address.Builder convertAddress(MLocation address) {
		Address.Builder builder = Address.newBuilder();
		if (address == null || address.getC_Location_ID() <= 0) {
			return builder;
		}

		String countryName = null;
		if (address.getC_Country_ID() > 0) {
			MCountry country = MCountry.get(Env.getCtx(), address.getC_Country_ID());
			if (country != null && country.getC_Country_ID() > 0) {
				countryName = country.getName();
				if (Util.isEmpty(countryName, true)) {
					countryName = country.getCountryCode();
				}
			}
		}

		String regionName = null;
		if (address.getC_Region_ID() > 0) {
			MRegion region = MRegion.get(Env.getCtx(), address.getC_Region_ID());
			if (region != null && region.getC_Region_ID() > 0) {
				regionName = region.getName();
			}
		}

		String cityName = null;
		if (address.getC_City_ID() > 0) {
			MCity city = MCity.get(Env.getCtx(), address.getC_City_ID());
			if (city != null && city.getC_City_ID() > 0) {
				cityName = city.getName();
			}
		}

		builder.setId(
				address.getC_Location_ID()
			)
			.setUuid(
				StringManager.getValidString(
					address.getUUID()
				)
			)
			.setDisplayValue(
				StringManager.getValidString(
					address.toString()
				)
			)
			.setCountryId(
				address.getC_Country_ID()
			)
			.setCountryName(
				StringManager.getValidString(
					countryName
				)
			)
			.setRegionId(
				address.getC_Region_ID()
			)
			.setRegionName(
				StringManager.getValidString(
					regionName
				)
			)
			.setCityId(
				address.getC_City_ID()
			)
			.setCityName(
				StringManager.getValidString(
					cityName
				)
			)
			.setCity(
				StringManager.getValidString(
					address.getCity()
				)
			)
			.setAddress1(
				StringManager.getValidString(
					address.getAddress1()
				)
			)
			.setAddress2(
				StringManager.getValidString(
					address.getAddress2()
				)
			)
			.setAddress3(
				StringManager.getValidString(
					address.getAddress3()
				)
			)
			.setAddress4(
				StringManager.getValidString(
					address.getAddress4()
				)
			)
			.setPostalCode(
				StringManager.getValidString(
					address.getPostal()
				)
			)
			.setPostalCodeAdditional(
				StringManager.getValidString(
					address.getPostal_Add()
				)
			)
			.setAltitude(
				NumberManager.getBigDecimalToString(
					address.getAltitude()
				)
			)
			.setLatitude(
				NumberManager.getBigDecimalToString(
					address.getLatitude()
				)
			)
			.setLongitude(
				NumberManager.getBigDecimalToString(
					address.getLongitude()
				)
			)
		;

		return builder;
	}
}
