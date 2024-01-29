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

package org.spin.pos.service.bank;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_Bank;
import org.adempiere.core.domains.models.I_C_BankAccount;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBank;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.core_functionality.BankAccount;
import org.spin.backend.grpc.pos.Bank;
import org.spin.backend.grpc.pos.ListBankAccountsRequest;
import org.spin.backend.grpc.pos.ListBankAccountsResponse;
import org.spin.backend.grpc.pos.ListBanksRequest;
import org.spin.backend.grpc.pos.ListBanksResponse;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.util.POSConvertUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * A util class for change values for documents
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class BankManagement {

	public static MBank validateAndGetBank(int bankId) {
		if (bankId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Bank_ID@");
		}
		MBank bank = MBank.get(Env.getCtx(), bankId);
		if (bank == null || bank.getC_Bank_ID() <= 0) {
			throw new AdempiereException("@C_Bank_ID@ @NotFound@");
		}
		return bank;
	}
	
	public static ListBanksResponse.Builder listBanks(ListBanksRequest request) {
		List<Object> filtersList = new ArrayList<>();

		String whereClause = "BankType = 'B' ";
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
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
				ValueManager.validateNull(nexPageToken)
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
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
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
			BankAccount.Builder bankAccountBuilder = CoreFunctionalityConvert.convertBankAccount(bankAccountId);
			builderList.addRecords(bankAccountBuilder);
		});
		return builderList;
	}

}
