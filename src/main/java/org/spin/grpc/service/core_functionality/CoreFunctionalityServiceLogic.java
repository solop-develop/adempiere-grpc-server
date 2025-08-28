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
package org.spin.grpc.service.core_functionality;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_Conversion_Rate;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.MConversionType;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.spin.backend.grpc.core_functionality.ConversionRate;
import org.spin.backend.grpc.core_functionality.GetConversionRateRequest;
import org.spin.backend.grpc.core_functionality.ListConversionRatesRequest;
import org.spin.backend.grpc.core_functionality.ListConversionRatesResponse;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Core Functionality
 */
public class CoreFunctionalityServiceLogic {

	/**
	 * Get conversion Rate from ValidFrom, Currency From, Currency To and Conversion Type
	 * @param request
	 * @return
	 */
	public static ConversionRate.Builder getConversionRate(GetConversionRateRequest request) {
		if(request.getConversionTypeId() <= 0
				|| request.getCurrencyFromId() <= 0
				|| request.getCurrencyToId() <= 0) {
			return null;
		}
		//	Get values
		Timestamp conversionDate = ValueManager.getDateFromTimestampDate(
			request.getConversionDate()
		);
		conversionDate = TimeUtil.getDay(conversionDate);

		MConversionRate conversionRate = RecordUtil.getConversionRate(
			Env.getAD_Org_ID(Env.getCtx()),
			request.getConversionTypeId(),
			request.getCurrencyFromId(),
			request.getCurrencyToId(),
			conversionDate
		);
		ConversionRate.Builder builder = CoreFunctionalityConvert.convertConversionRate(conversionRate);
		return builder;
	}


	public static ListConversionRatesResponse.Builder listConversionRates(ListConversionRatesRequest request) {
		if (request.getConversionTypeId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_ConversionType_ID@");
		}
		MConversionType conversionType = new MConversionType(Env.getCtx(), request.getConversionTypeId(), null);
		if (conversionType == null || conversionType.getC_ConversionType_ID() <= 0) {
			throw new AdempiereException("@C_ConversionType_ID@ @NotFound@");
		}

		Timestamp conversionDate = ValueManager.getDateFromTimestampDate(
			request.getConversionDate()
		);
		conversionDate = TimeUtil.getDay(
			conversionDate
		);

		final String whereClause = "C_ConversionType_ID = ? "
			+ "AND ? >= ValidFrom AND ? <= ValidTo "
			+ "AND AD_Client_ID IN (0,?) AND AD_Org_ID IN (0,?) "
			+ "AND IsActive = 'Y' "
		;
		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			conversionType.getC_ConversionType_ID()
		);
		filtersList.add(conversionDate);
		filtersList.add(conversionDate);
		filtersList.add(
			Env.getAD_Client_ID(Env.getCtx())
		);
		filtersList.add(
			Env.getAD_Org_ID(Env.getCtx())
		);

		Query query = new Query(
			Env.getCtx(),
			I_C_Conversion_Rate.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOrderBy("AD_Client_ID DESC, AD_Org_ID DESC, ValidFrom DESC")
		;

		// Pagination
		int recordCount = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListConversionRatesResponse.Builder builderList = ListConversionRatesResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query.getIDsAsList().forEach(conversionRateId -> {
			MConversionRate conversionRate = new MConversionRate(Env.getCtx(), conversionRateId, null);
			ConversionRate.Builder builder = CoreFunctionalityConvert.convertConversionRate(conversionRate);
			builderList.addRecords(builder);
		});

		return builderList;
	}

}
