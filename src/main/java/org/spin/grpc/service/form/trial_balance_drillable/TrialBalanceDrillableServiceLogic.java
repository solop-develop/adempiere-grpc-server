/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt EdwinBetanc0urt@outlook.com                     *
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

package org.spin.grpc.service.form.trial_balance_drillable;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MLookupInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.trial_balance_drillable.ListAccoutingKeysRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListBudgetsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListFactAcctSummaryRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListFactAcctSummaryResponse;
import org.spin.backend.grpc.form.trial_balance_drillable.ListOrganizationsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListPeriodsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListReportCubesRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListUser1Request;
import org.spin.base.util.ReferenceUtil;
import org.spin.grpc.service.UserInterface;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Trial Balance Drillable Report
 */
public class TrialBalanceDrillableServiceLogic {


	public static ListLookupItemsResponse.Builder listOrganizations(ListOrganizationsRequest request) {
		final int columnId = 839; // C_Period.AD_Org_ID
		MColumn column = MColumn.get(Env.getCtx(), columnId);

		final int tableReferenceId = 322; // where clause AD_Org.AD_Org_ID<>0
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.Table,
			tableReferenceId,
			column.getColumnName(),
			0
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listBudgets(ListBudgetsRequest request) {
		final int columnId = 2536; // Fact_Acct.GL_Budget_ID
		MColumn column = MColumn.get(Env.getCtx(), columnId);

		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			column.getColumnName(),
			0
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listUser1(ListUser1Request request) {
		final int columnId = 69948; // GL_JournalLine.User1_ID
		MColumn column = MColumn.get(Env.getCtx(), columnId);

		final int tableReferenceId = 134; // where clause C_ElementValue.IsActive='Y'
		// AND C_ElementValue.IsSummary='N' AND C_ElementValue.C_Element_ID IN 
		// (SELECT C_Element_ID FROM C_AcctSchema_Element ase WHERE ase.ElementType='U1' 
		// AND ase.AD_Client_ID=@AD_Client_ID@)
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			tableReferenceId,
			column.getColumnName(),
			0
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listPeriods(ListPeriodsRequest request) {
		final int columnId = 2516; // Fact_Acct.C_Period_ID
		MColumn column = MColumn.get(Env.getCtx(), columnId);

		final int tableReferenceId = 275; // order by C_Period.StartDate
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			tableReferenceId,
			column.getColumnName(),
			0
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listAccoutingKeys(ListAccoutingKeysRequest request) {
		final int columnId = 69936; // GL_JournalLine.Account_ID
		MColumn column = MColumn.get(Env.getCtx(), columnId);
		final int clientId = Env.getAD_Client_ID(Env.getCtx());

		final int tableReferenceId = 362; // where clause C_ElementValue.IsSummary<>'Y'
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			tableReferenceId,
			column.getColumnName(),
			0,
			"C_ElementValue.IsActive='Y' AND C_ElementValue.AD_Client_ID=" + clientId
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listReportCubes(ListReportCubesRequest request) {
		final int columnId = 57582; // PA_Report.PA_ReportCube_ID
		MColumn column = MColumn.get(Env.getCtx(), columnId);
		
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			column.getColumnName(),
			0
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ListFactAcctSummaryResponse.Builder listFactAcctSummary(ListFactAcctSummaryRequest request) {
		int organizationId = request.getOrganizationId();
		if (organizationId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		}

		int reportCubeId = request.getReportCubeId();
		if (reportCubeId <= 0) {
			throw new AdempiereException("@FillMandatory@ @PA_ReportCube_ID@");
		}

		ListFactAcctSummaryResponse.Builder builder = ListFactAcctSummaryResponse.newBuilder();

		return builder;
	}

}
