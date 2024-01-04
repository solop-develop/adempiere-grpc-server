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

import java.util.Properties;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_C_ElementValue;
import org.adempiere.core.domains.models.I_Fact_Acct_Summary;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAcctSchemaGL;
import org.compiere.model.MColumn;
import org.compiere.model.MElementValue;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MPeriod;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.trial_balance_drillable.FactAcctSummary;
import org.spin.backend.grpc.form.trial_balance_drillable.ListAccoutingKeysRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListBudgetsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListFactAcctSummaryRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListFactAcctSummaryResponse;
import org.spin.backend.grpc.form.trial_balance_drillable.ListOrganizationsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListPeriodsRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListReportCubesRequest;
import org.spin.backend.grpc.form.trial_balance_drillable.ListUser1Request;
import org.spin.base.db.ParameterUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.grpc.service.UserInterface;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Trial Balance Drillable Report
 */
public class TrialBalanceDrillableServiceLogic {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(TrialBalanceDrillableServiceLogic.class);


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

		int periodId = request.getPeriodId();
		if (periodId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Period_ID@");
		}

		int reportCubeId = request.getReportCubeId();
		if (reportCubeId <= 0) {
			throw new AdempiereException("@FillMandatory@ @PA_ReportCube_ID@");
		}

		Properties context = Env.getCtx();


		int clientId = Env.getAD_Client_ID(context);
		int accountingSchemaId = DB.getSQLValue(null, "SELECT MIN(C_AcctSchema_ID) FROM C_AcctSchema WHERE AD_CLient_ID=?", clientId);
		// if (accountingSchemaId <= 0) {
		// 	accountingSchemaId = Env.getContextAsInt(context, "$C_AcctSchema_ID");
		// }
		MAcctSchemaGL acctSchemaGL = MAcctSchemaGL.get(Env.getCtx(),accountingSchemaId);
		MElementValue reEl = (MElementValue) acctSchemaGL.getRetainedEarning_A().getAccount();
		int RETAIN_EARNING_ELEMENT_ID = reEl.getC_ElementValue_ID();
		String RETAIN_EARNING_ELEMENT_VALUE = reEl.getValue();

		List<Object> filterParametersList = new ArrayList<>();

		MPeriod period = new MPeriod(context, periodId, null);
		filterParametersList.add(period.getStartDate());
		filterParametersList.add(period.getEndDate());
		filterParametersList.add(period.getStartDate());
		filterParametersList.add(period.getEndDate());

		MPeriod yearFrom = MPeriod.getFirstInYear(context, period.getStartDate(), organizationId);
		filterParametersList.add(yearFrom.getStartDate());
		filterParametersList.add(period.getEndDate());
		filterParametersList.add(yearFrom.getStartDate());
		filterParametersList.add(period.getEndDate());

		filterParametersList.add(clientId);

		StringBuffer sql = new StringBuffer(" SELECT ");
		sql.append(" fs.Account_ID, ev.value, ev.name, ");
		sql.append( " COALESCE(SUM(CASE WHEN (DateAcct  BETWEEN (? :: date) AND (? :: date)) AND PostingType = 'A' THEN (AmtacctDr-AmtacctCr) ELSE 0 END), 0) AS total1,");
		sql.append( " COALESCE(SUM(CASE WHEN (DateAcct  BETWEEN (? :: date) AND (? :: date)) AND PostingType = 'B' THEN (AmtacctDr-AmtacctCr) ELSE 0 END), 0) AS total2,");
		sql.append( " COALESCE(Sum(CASE WHEN ((DateAcct  >= (? :: date) OR ev.AccountType NOT IN ('E','R')) AND DateAcct  <= (? :: date)) AND PostingType='A' THEN (AmtacctDr-AmtacctCr) ELSE 0 END), 0) AS total3,");
		sql.append( " COALESCE(Sum(CASE WHEN ((DateAcct  >= (? :: date) OR ev.AccountType NOT IN ('E','R')) AND DateAcct  <= (? :: date)) AND PostingType='B' THEN (AmtacctDr-AmtacctCr) ELSE 0 END), 0) AS total4,");
		// sql.append(" fs.User1_ID, ");
		sql.append(" u1.Value AS userlist1 ");
		sql.append(" FROM  Fact_Acct_Summary fs" 
		+ " INNER JOIN C_ElementValue ev ON fs.Account_ID = ev.C_ElementValue_ID AND fs.AD_Client_ID = ev.AD_Client_ID ");
		sql.append(" LEFT OUTER JOIN C_ElementValue u1 ON (fs.user1_id=u1.C_ElementValue_ID) ");
		sql.append(" WHERE fs.AD_Client_ID=? ");
		if (organizationId > 0) {
			sql.append(" AND fs.AD_Org_ID=?");
			filterParametersList.add(organizationId);
		}

		if (accountingSchemaId > 0) {
			sql.append(" AND fs.C_AcctSchema_ID=?");
			filterParametersList.add(accountingSchemaId);
		}
		sql.append(" AND (PostingType = 'A' OR fs.GL_Budget_ID=?)");

		int budgetId = 0;
		if (request.getBudgetId() > 0) {
			budgetId = request.getBudgetId();
		}
		filterParametersList.add(budgetId);

		if (reportCubeId > 0) {
			sql.append(" AND fs.PA_ReportCube_ID=? ");
			filterParametersList.add(reportCubeId);
		}

		int user1_ID = request.getUser1Id();
		if (user1_ID > 0) {
			sql.append(" AND fs.user1_id=? ");
			filterParametersList.add(user1_ID);
		}

		int accountingFromId = request.getAccoutingFromId();
		int accountingToId = request.getAccoutingToId();
		String accountingFromValue = null;
		String accountingToValue = null;

		if (accountingFromId > 0 || accountingToId > 0) {
			final String sqlAccoutingValue = "SELECT Value from C_ElementValue WHERE C_ElementValue_ID = ?";
			accountingFromValue = DB.getSQLValueString(null, sqlAccoutingValue, accountingFromId);
			accountingToValue = DB.getSQLValueString(null, sqlAccoutingValue, accountingToId);

			if (accountingFromValue != null && accountingToValue != null) {
				sql.append(" AND (fs.Account_ID IS NULL OR EXISTS (SELECT * FROM C_ElementValue ev ")
					.append("WHERE fs.Account_ID=ev.C_ElementValue_ID AND ev.Value >= ")
					.append(DB.TO_STRING(accountingFromValue)).append(" AND ev.Value <= ")
					.append(DB.TO_STRING(accountingToValue)).append("))")
				;
			}
			else if (accountingFromValue != null && accountingToValue == null) {
				sql.append(" AND (fs.Account_ID IS NULL OR EXISTS (SELECT * FROM C_ElementValue ev ")
					.append("WHERE fs.Account_ID=ev.C_ElementValue_ID AND ev.Value >= ")
					.append(DB.TO_STRING(accountingFromValue)).append("))")
				;
			}
			else if (accountingFromValue == null && accountingToValue != null) {
				sql.append(" AND (fs.Account_ID IS NULL OR EXISTS (SELECT * FROM C_ElementValue ev ")
					.append("WHERE fs.Account_ID=ev.C_ElementValue_ID AND ev.Value <= ")
					.append(DB.TO_STRING(accountingToValue)).append("))")
				;
			}
		}
		sql.append(" GROUP BY fs.Account_ID,ev.value,ev.name, u1.value ");
		
		// calculate prior year earnings if all accounts selected
		if (accountingFromValue == null && accountingFromValue == null) {
			filterParametersList.add(yearFrom.getStartDate());
			filterParametersList.add(yearFrom.getStartDate());

			sql.append(" UNION ALL SELECT ");
			sql.append(RETAIN_EARNING_ELEMENT_ID).append(" AS Account_ID, '").append(RETAIN_EARNING_ELEMENT_VALUE + '_')
				.append("' AS Value, '>>> Prior Year Earnings' AS name, ");
			sql.append(" 0 AS total1,");
			sql.append(" 0 AS total2,");
			sql.append(" COALESCE(Sum(CASE WHEN DateAcct  < (? :: date) AND ev.AccountType IN ('E','R') AND PostingType='A' THEN (AmtacctDr-AmtacctCr) ELSE 0 END), 0) AS total3,");
			sql.append(" COALESCE(Sum(CASE WHEN DateAcct  < (? :: date) AND ev.AccountType IN ('E','R') AND PostingType='B' THEN (AmtacctDr-AmtacctCr) ELSE 0 END), 0) AS total4,");
			// sql.append(" 0 AS User1_ID, ");
			sql.append(" NULL AS userlist1 ");
			sql.append(" FROM  Fact_Acct_Summary fs"
				+ " INNER JOIN C_ElementValue ev ON fs.Account_ID = ev.C_ElementValue_ID AND fs.AD_Client_ID = ev.AD_Client_ID ");
			sql.append(" LEFT OUTER JOIN C_ElementValue u1 ON (fs.user1_id=u1.C_ElementValue_ID) ");
			sql.append(" WHERE fs.AD_Client_ID=? ");
			filterParametersList.add(clientId);

			if (organizationId > 0) {
				sql.append(" AND fs.AD_Org_ID=?");
				filterParametersList.add(organizationId);
			}
			if (accountingSchemaId > 0) {
				sql.append(" AND fs.C_AcctSchema_ID=?");
				filterParametersList.add(accountingSchemaId);
			}
			sql.append(" AND (PostingType = 'A' OR fs.GL_Budget_ID=?)");
			filterParametersList.add(budgetId);

			if (reportCubeId > 0) {
				sql.append(" AND fs.PA_ReportCube_ID=? ");
				filterParametersList.add(reportCubeId);
			}
			if (user1_ID > 0) {
				sql.append(" AND fs.user1_id=? ");
				filterParametersList.add(user1_ID);
			}
			sql.append(" AND (fs.Account_ID IS NULL OR EXISTS (SELECT * FROM C_ElementValue ev ");
			sql.append(" WHERE fs.Account_ID = ev.C_ElementValue_ID AND " +
				" ev.Value IN (SELECT value FROM C_ElementValue WHERE AccountType IN ('R','E') AND IsSummary = 'N') AND AD_Client_ID = ? )) ");
			filterParametersList.add(clientId);
		}

		sql.append(" ORDER BY 2");


		ListFactAcctSummaryResponse.Builder builderList = ListFactAcctSummaryResponse.newBuilder();

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		long recordCount = 0;
		try {
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql.toString(), null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filterParametersList);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				BigDecimal periodActualAmount = rs.getBigDecimal("Total1");
				BigDecimal periodBudgetAmount = rs.getBigDecimal("Total2");
				BigDecimal ytdActualAmount = rs.getBigDecimal("Total3");
				BigDecimal ytdBudgetAmount = rs.getBigDecimal("Total4");

				FactAcctSummary.Builder builder = FactAcctSummary.newBuilder()
					.setId(
						rs.getInt(
							I_Fact_Acct_Summary.COLUMNNAME_Account_ID
						)
					)
					.setValue(
						ValueManager.validateNull(
							rs.getString(
								I_C_ElementValue.COLUMNNAME_Value
							)
						)
					)
					.setName(
						ValueManager.validateNull(
							rs.getString(
								I_C_ElementValue.COLUMNNAME_Name
							)
						)
					)
					// .setUserListId(
					// 	rs.getInt(
					// 		I_Fact_Acct_Summary.COLUMNNAME_User1_ID
					// 	)
					// )
					.setUserListName(
						ValueManager.validateNull(
							rs.getString(
								"userlist1"
							)
						)
					)
					.setPeriodActualAmount(
						NumberManager.getBigDecimalToString(
							periodActualAmount
						)
					)
					.setPeriodBudgetAmount(
						NumberManager.getBigDecimalToString(
							periodBudgetAmount
						)
					)
					.setPeriodVarianceAmount(
						NumberManager.getBigDecimalToString(
							periodBudgetAmount.subtract(
								periodActualAmount
							)
						)
					)
					.setYtdActualAmount(
						NumberManager.getBigDecimalToString(
							ytdActualAmount
						)
					)
					.setYtdBudgetAmount(
						NumberManager.getBigDecimalToString(
							ytdBudgetAmount
						)
					)
					.setVarianceAmount(
						NumberManager.getBigDecimalToString(
							ytdBudgetAmount.subtract(
								ytdActualAmount
							)
						)
					)
				;
				builderList.addRecords(builder);
				recordCount++;
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
		}

		builderList.setRecordCount(recordCount);

		return builderList;
	}

}
