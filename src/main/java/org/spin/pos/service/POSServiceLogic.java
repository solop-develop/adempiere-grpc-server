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

import org.adempiere.core.domains.models.I_C_Bank;
import org.adempiere.core.domains.models.I_C_BankAccount;
import org.adempiere.core.domains.models.I_C_Campaign;
import org.compiere.model.MBank;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.BankAccount;
import org.spin.backend.grpc.pos.Bank;
import org.spin.backend.grpc.pos.Campaign;
import org.spin.backend.grpc.pos.ListBankAccountsRequest;
import org.spin.backend.grpc.pos.ListBankAccountsResponse;
import org.spin.backend.grpc.pos.ListBanksRequest;
import org.spin.backend.grpc.pos.ListBanksResponse;
import org.spin.backend.grpc.pos.ListCampaignsRequest;
import org.spin.backend.grpc.pos.ListCampaignsResponse;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;
import org.spin.pos.service.bank.BankManagement;
import org.spin.pos.util.POSConvertUtil;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Point Of Sales form
 */
public class POSServiceLogic {

	public static ListBanksResponse.Builder listBanks(ListBanksRequest request) {
		List<Object> filtersList = new ArrayList<>();

		String whereClause = "BankType = 'B' ";
		if (!Util.isEmpty(request.getSearchValue(), false)) {
			filtersList.add(request.getSearchValue());
			whereClause += "AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query query = new Query(
			Env.getCtx(),
			I_C_Bank.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int recordCount = query.count();

		ListBanksResponse.Builder builderList = ListBanksResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		//	Get List
		List<Integer> banksIdsList = query
			.setLimit(limit, offset)
			.getIDsAsList()
		;

		banksIdsList.forEach(tableId -> {
			Bank.Builder accountingDocument = POSConvertUtil.convertBank(tableId);
			builderList.addRecords(accountingDocument);
		});

		return builderList;
	}


	public static ListBankAccountsResponse.Builder listBankAccounts(ListBankAccountsRequest request) {
		// validate and get Bank
		MBank bank = BankManagement.validateAndGetBank(request.getBankId());

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(bank.getC_Bank_ID());
		String whereClause = "C_Bank_ID = ? ";
		if (!Util.isEmpty(request.getSearchValue(), false)) {
			filtersList.add(request.getSearchValue());
			whereClause = "AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query query = new Query(
			Env.getCtx(),
			I_C_BankAccount.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		int recordCount = query.count();

		ListBankAccountsResponse.Builder builderList = ListBankAccountsResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		//	Get List
		query.getIDsAsList().forEach(bankAccountId -> {
			BankAccount.Builder bankAccountBuilder = ConvertUtil.convertBankAccount(bankAccountId);
			builderList.addRecords(bankAccountBuilder);
		});
		return builderList;
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
