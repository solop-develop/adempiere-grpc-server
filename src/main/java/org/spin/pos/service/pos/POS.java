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

package org.spin.pos.service.pos;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_Campaign;
import org.adempiere.core.domains.models.I_C_POS;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPOS;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.Campaign;
import org.spin.backend.grpc.pos.ListCampaignsRequest;
import org.spin.backend.grpc.pos.ListCampaignsResponse;
import org.spin.base.util.RecordUtil;
import org.spin.pos.util.POSConvertUtil;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Point Of Sales form
 */
public class POS {

	/**
	 * Get POS with identifier
	 * @param posId
	 * @param requery
	 * @return
	 */
	public static MPOS validateAndGetPOS(int posId, boolean requery) {
		if (posId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_POS_ID@");
		}
		MPOS pos = MPOS.get(Env.getCtx(), posId);
		if (requery) {
			pos = new MPOS(Env.getCtx(), posId, null);
		} else {
			pos = MPOS.get(Env.getCtx(), posId);
		}
		if (pos == null || pos.getC_POS_ID() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		return pos;
	}

	/**
	 * Get POS with uuid
	 * @param posId
	 * @param requery
	 * @return
	 */
	public static MPOS validateAndGetPOS(String posUuid, boolean requery) {
		if (Util.isEmpty(posUuid, true)) {
			throw new AdempiereException("@FillMandatory@ @C_POS_ID@");
		}
		int posId = RecordUtil.getIdFromUuid(I_C_POS.Table_Name, posUuid, null);
		return validateAndGetPOS(posId, requery);
	}

	/**
	 * Get POS with identifier or uuid
	 * @param posId
	 * @param requery
	 * @return
	 */
	public static MPOS validateAndGetPOS(int posId, String posUuid, boolean requery) {
		if (posId > 0) {
			return validateAndGetPOS(posId, requery);
		}
		return validateAndGetPOS(posUuid, requery);
	}

	public static ListCampaignsResponse.Builder listCampaigns(ListCampaignsRequest request) {
		List<Object> filtersList = new ArrayList<>();

		String whereClause = null;
		if (!Util.isEmpty(request.getSearchValue(), false)) {
			filtersList.add(request.getSearchValue());
			whereClause = "UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query query = new Query(
			Env.getCtx(),
			I_C_Campaign.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		int recordCount = query.count();

		ListCampaignsResponse.Builder builderList = ListCampaignsResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		//	Get List
		query.getIDsAsList().forEach(campaignId -> {
			Campaign.Builder campaignBuilder = POSConvertUtil.convertCampaign(campaignId);
			builderList.addRecords(campaignBuilder);
		});

		return builderList;
	}

}
