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
import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.Campaign;
import org.spin.backend.grpc.pos.ListCampaignsRequest;
import org.spin.backend.grpc.pos.ListCampaignsResponse;
import org.spin.pos.util.POSConvertUtil;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.value.StringManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Point Of Sales form
 */
public class POS {

	/**
	 * Get Payment Method allocation from payment
	 * @return
	 * @return PO
	 */
	public static PO getPaymentMethodAllocation(int paymentMethodId, int posId, String transactionName) {
		if(MTable.get(Env.getCtx(), "C_POSPaymentTypeAllocation") == null) {
			return null;
		}
		return new Query(Env.getCtx(), "C_POSPaymentTypeAllocation", "C_POS_ID = ? AND C_PaymentMethod_ID = ?", transactionName)
				.setParameters(posId, paymentMethodId)
				.setOnlyActiveRecords(true)
				.first();
	}

	public static PO getPaymentMethodAllocationFromTenderType(int posId, String tenderType) {
		if(MTable.get(Env.getCtx(), "C_POSPaymentTypeAllocation") == null) {
			return null;
		}
		return new Query(Env.getCtx(), "C_POSPaymentTypeAllocation", "C_POS_ID = ? " +
				"AND IsDisplayedFromCollection = 'Y' " +
				"AND EXISTS(SELECT 1 FROM C_PaymentMethod pm WHERE pm.C_PaymentMethod_ID = C_POSPaymentTypeAllocation.C_PaymentMethod_ID AND pm.TenderType = ?)", null)
				.setParameters(posId, tenderType)
				.setOnlyActiveRecords(true)
				.first();
	}

	public static PO getPaymentTypeAllocation(int paymentTypeAllocationId, String transactionName) {
		if(MTable.get(Env.getCtx(), "C_POSPaymentTypeAllocation") == null) {
			return null;
		}
		if (paymentTypeAllocationId <= 0) {
			return null;
		}
		return new Query(
			Env.getCtx(),
			"C_POSPaymentTypeAllocation",
			" C_POSPaymentTypeAllocation_ID = ?",
			transactionName
		)
			.setParameters(paymentTypeAllocationId)
			.setOnlyActiveRecords(true)
			.first()
		;
	}



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
		
		MPOS pos = validateAndGetPOS(request.getPosId(), true);

		String whereClause = "1=1 ";
		final String searchValue = StringManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
			filtersList.add(searchValue);
			whereClause = " AND ("
				+ "UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ ")"
			;
		}

		final int defaultCampaigndId = pos.get_ValueAsInt("DefaultCampaign_ID");
		if (defaultCampaigndId > 0) {
			filtersList.add(defaultCampaigndId);
			whereClause += " OR C_Campaign_ID = ? ";
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
