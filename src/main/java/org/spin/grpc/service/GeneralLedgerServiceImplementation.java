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
package org.spin.grpc.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.core.domains.models.I_Fact_Acct;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.base.db.CountUtil;
import org.spin.base.db.LimitUtil;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

import org.spin.backend.grpc.common.Condition;
import org.spin.backend.grpc.common.Criteria;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.general_ledger.GeneralLedgerGrpc.GeneralLedgerImplBase;
import org.spin.backend.grpc.general_ledger.GetAccountingCombinationRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingCombinationsRequest;
import org.spin.backend.grpc.general_ledger.ListAccoutingFactsRequest;
import org.spin.backend.grpc.general_ledger.SaveAccountingCombinationRequest;
import org.spin.backend.grpc.general_ledger.StartRePostRequest;
import org.spin.backend.grpc.general_ledger.StartRePostResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Paryroll Action Notice Form
 */
public class GeneralLedgerServiceImplementation extends GeneralLedgerImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(GeneralLedgerServiceImplementation.class);

	private String tableName = MAccount.Table_Name;

	@Override
	public void getAccountingCombination(GetAccountingCombinationRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder accountingCombination = getAccountingCombination(request);
			responseObserver.onNext(accountingCombination.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private Entity.Builder getAccountingCombination(GetAccountingCombinationRequest request) {
		// Validate ID
		if(request.getId() == 0 && Util.isEmpty(request.getUuid()) && Util.isEmpty(request.getValue())) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}

		MAccount accoutingCombination = null;
		if(request.getId() > 0) {
			accoutingCombination = MAccount.getValidCombination(Env.getCtx(), request.getId(), null);
		} else if(!Util.isEmpty(request.getUuid(), true)) {
			accoutingCombination = new Query(
					Env.getCtx(),
					this.tableName,
					MAccount.COLUMNNAME_UUID + " = ? ",
					null
				)
				.setParameters(request.getUuid())
				.firstOnly();
		} else if (!Util.isEmpty(request.getValue(), true)) {
			// Value as combination
			accoutingCombination = new Query(
					Env.getCtx(),
					this.tableName,
					MAccount.COLUMNNAME_Combination + " = ? ",
					null
				)
				.setParameters(request.getValue())
				.firstOnly();
		}
		if(accoutingCombination == null) {
			throw new AdempiereException("@Error@ @AccountCombination@ @not.found@");
		}

		Entity.Builder entityBuilder = ConvertUtil.convertEntity(accoutingCombination);

		return entityBuilder;
	}


	@Override
	public void listAccountingCombinations(ListAccountingCombinationsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListEntitiesResponse.Builder entitiesList = listAccountingCombinations(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListEntitiesResponse.Builder listAccountingCombinations(ListAccountingCombinationsRequest request) {
		Map<String, Object> contextAttributesList = ValueUtil.convertValuesToObjects(request.getContextAttributesList());
		if (contextAttributesList.get(MAccount.COLUMNNAME_AD_Org_ID) == null) {
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		} else if ((int) contextAttributesList.get(MAccount.COLUMNNAME_AD_Org_ID) <= 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}

		if (contextAttributesList.get(MAccount.COLUMNNAME_Account_ID) == null || (int) contextAttributesList.get(MAccount.COLUMNNAME_Account_ID) <= 0) {
			throw new AdempiereException("@FillMandatory@ @Account_ID@");
		}

		//
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributes(windowNo, Env.getCtx(), request.getContextAttributesList());

		MTable table = MTable.get(Env.getCtx(), this.tableName);
		StringBuilder sql = new StringBuilder(QueryUtil.getTableQueryWithReferences(table));

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault()
			.addAccessSQL(
				sql.toString(),
				null,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		//	For dynamic condition
		List<Object> params = new ArrayList<>(); // includes on filters criteria
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), this.tableName, params);
		if (!Util.isEmpty(dynamicWhere, true)) {
			// includes first AND
			sqlWithRoleAccess += " AND " + dynamicWhere; 
		}

		// add where with search value
		String parsedSQL = RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, this.tableName, request.getSearchValue(), params);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();

		//	Count records
		count = CountUtil.countRecords(parsedSQL, this.tableName, params);
		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(MTable.get(Env.getCtx(), this.tableName), parsedSQL, params);
		//	
		builder.setRecordCount(count);
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		return builder;
	}

	@Override
	public void saveAccountingCombination(SaveAccountingCombinationRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder entity = convertAccountingCombination(request);
			responseObserver.onNext(entity.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private Entity.Builder convertAccountingCombination(SaveAccountingCombinationRequest request) {
		// set context values
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributes(windowNo, Env.getCtx(), request.getContextAttributesList());

		Map<String, Object> contextAttributesList = ValueUtil.convertValuesToObjects(request.getContextAttributesList());
		if (contextAttributesList.get(MAccount.COLUMNNAME_AD_Org_ID) == null) {
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		} else if ((int) contextAttributesList.get(MAccount.COLUMNNAME_AD_Org_ID) <= 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}
		int organizationId = (int) contextAttributesList.get(MAccount.COLUMNNAME_AD_Org_ID);

		if (contextAttributesList.get(MAccount.COLUMNNAME_Account_ID) == null || (int) contextAttributesList.get(MAccount.COLUMNNAME_Account_ID) <= 0) {
			throw new AdempiereException("@FillMandatory@ @Account_ID@");
		}
		int accountId = (int) contextAttributesList.get(MAccount.COLUMNNAME_Account_ID);

		if (contextAttributesList.get(MAccount.COLUMNNAME_C_AcctSchema_ID) == null || (int) contextAttributesList.get(MAccount.COLUMNNAME_C_AcctSchema_ID) <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
		}
		int accoutingSchemaId = (int) contextAttributesList.get(MAccount.COLUMNNAME_C_AcctSchema_ID);
		MAcctSchema accoutingSchema = MAcctSchema.get(Env.getCtx(), accoutingSchemaId, null);

		String accountingCombinationAlias = ValueUtil.validateNull((String) contextAttributesList.get(MAccount.COLUMNNAME_Alias));
		
		List<MAcctSchemaElement> acctingSchemaElements = Arrays.asList(accoutingSchema.getAcctSchemaElements());

		Map<String, Object> attributesList = ValueUtil.convertValuesToObjects(request.getAttributesList());
		StringBuffer sql = generateSQL(acctingSchemaElements, attributesList);

		int clientId = Env.getContextAsInt(Env.getCtx(), windowNo, MAccount.COLUMNNAME_AD_Client_ID);

		int accountingCombinationId = 0;
		String accountingAlias = "";
		try {
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, clientId);
			pstmt.setInt(2, accoutingSchema.getC_AcctSchema_ID());
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				accountingCombinationId = rs.getInt(1);
				accountingAlias = ValueUtil.validateNull(rs.getString(2));
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql.toString(), e);
			accountingCombinationId = 0;
		}

		//	We have an account like this already - check alias
		if (accountingCombinationId != 0) {
			if (accoutingSchema.isHasAlias() && !accountingCombinationAlias.equals(accountingAlias)) {
				sql = new StringBuffer("UPDATE C_ValidCombination SET Alias = ");
				if (Util.isEmpty(accountingCombinationAlias, true)) {
					sql.append("NULL");
				} else {
					sql.append("'").append(accountingCombinationAlias).append("'");
				}
				sql.append(" WHERE C_ValidCombination_ID=").append(accountingCombinationId);
				int rowChanges = 0;
				try {
					PreparedStatement stmt = DB.prepareStatement(
						sql.toString(),
						ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_UPDATABLE,
						null
					);
					rowChanges = stmt.executeUpdate();
					stmt.close();
				}
				catch (SQLException e) {
					log.log(Level.SEVERE, sql.toString(), e);
				}
				if (rowChanges == 0) {
					// FDialog.error(m_WindowNo, this, "AccountNotUpdated");
				}
			}

			// loadInfo(accountingCombinationId, accoutingSchema.getC_AcctSchema_ID());
			// action_Find(false);
			// return;
		}

		log.config("New");
		MAccount accountCombination = setAccoutingCombinationByAttributes(clientId, organizationId, accoutingSchemaId, accountId, attributesList);
		
		Entity.Builder builder = ConvertUtil.convertEntity(accountCombination);

		return builder;
	}
	
	private MAccount setAccoutingCombinationByAttributes(int clientId, int organizationId, int accoutingSchemaId, int accountId, Map<String, Object> attributesList) {
		String accoutingAlias = null;
		if (attributesList.get(MAccount.COLUMNNAME_Alias) != null) {
			accoutingAlias = (String) attributesList.get(MAccount.COLUMNNAME_Alias);
		}
		int subAccountId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_SubAcct_ID) != null) {
			subAccountId = (int) attributesList.get(MAccount.COLUMNNAME_C_SubAcct_ID);
		}
		int productId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_M_Product_ID) != null) {
			productId = (int) attributesList.get(MAccount.COLUMNNAME_M_Product_ID);
		}
		int businessPartnerId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_BPartner_ID) != null) {
			businessPartnerId = (int) attributesList.get(MAccount.COLUMNNAME_C_BPartner_ID);
		}
		int organizationTrxId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_AD_OrgTrx_ID) != null) {
			organizationTrxId = (int) attributesList.get(MAccount.COLUMNNAME_AD_OrgTrx_ID);
		}
		int locationFromId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_LocFrom_ID) != null) {
			locationFromId = (int) attributesList.get(MAccount.COLUMNNAME_C_LocFrom_ID);
		}
		int locationToId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_LocTo_ID) != null) {
			locationToId = (int) attributesList.get(MAccount.COLUMNNAME_C_LocTo_ID);
		}
		int salesRegionId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_SalesRegion_ID) != null) {
			salesRegionId = (int) attributesList.get(MAccount.COLUMNNAME_C_SalesRegion_ID);
		}
		int projectId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_Project_ID) != null) {
			projectId = (int) attributesList.get(MAccount.COLUMNNAME_C_Project_ID);
		}
		int campaignId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_Campaign_ID) != null) {
			campaignId = (int) attributesList.get(MAccount.COLUMNNAME_C_Campaign_ID);
		}
		int activityId = 0;
		if (attributesList.get(MAccount.COLUMNNAME_C_Activity_ID) != null) {
			activityId = (int) attributesList.get(MAccount.COLUMNNAME_C_Activity_ID);
		}
		int user1Id = 0;
		if (attributesList.get(MAccount.COLUMNNAME_User1_ID) != null) {
			user1Id = (int) attributesList.get(MAccount.COLUMNNAME_User1_ID);
		}
		int user2Id = 0;
		if (attributesList.get(MAccount.COLUMNNAME_User2_ID) != null) {
			user2Id = (int) attributesList.get(MAccount.COLUMNNAME_User2_ID);
		}
		int user3Id = 0;
		if (attributesList.get(MAccount.COLUMNNAME_User3_ID) != null) {
			user3Id = (int) attributesList.get(MAccount.COLUMNNAME_User3_ID);
		}
		int user4Id = 0;
		if (attributesList.get(MAccount.COLUMNNAME_User4_ID) != null) {
			user4Id = (int) attributesList.get(MAccount.COLUMNNAME_User4_ID);
		}

		MAccount accountCombination = MAccount.get(
			Env.getCtx(), clientId,
			organizationId,
			accoutingSchemaId,
			accountId, subAccountId,
			productId, businessPartnerId, organizationTrxId,
			locationFromId,locationToId, salesRegionId,
			projectId, campaignId, activityId,
			user1Id, user2Id , user3Id , user4Id,
			0, 0, null
		);
		
		if (!Util.isEmpty(accoutingAlias, true) && accountCombination != null && accountCombination.getAccount_ID() > 0) {
			accountCombination.setAlias(accoutingAlias);
			accountCombination.saveEx();
		}
		return accountCombination;
	}

	private StringBuffer generateSQL(List<MAcctSchemaElement> acctingSchemaElements, Map<String, Object> attributesList) {
		StringBuffer sql = new StringBuffer ("SELECT C_ValidCombination_ID, Alias FROM C_ValidCombination WHERE 1=1 ");

		acctingSchemaElements.forEach(acctingSchemaElement -> {
			String elementType = acctingSchemaElement.getElementType();
			String columnName = MAcctSchemaElement.getColumnName(elementType);
			Object value = attributesList.get(columnName);

			if (acctingSchemaElement.isMandatory() && (value == null || (value instanceof String && Util.isEmpty((String) value, true)))) {
				throw new AdempiereException("@" + columnName + "@ @IsMandatory@");
			}

			// The alias does not affect the query criteria
			if (columnName == MAccount.COLUMNNAME_Alias) {
				return;
			}

			sql.append(" AND ").append(columnName);
			if (value == null || (value instanceof String && Util.isEmpty((String) value, true))) {
				sql.append(" IS NULL ");
			} else {
				sql.append(" = ").append(value);
			}
		});

		sql.append(" AND AD_Client_ID = ? AND C_AcctSchema_ID = ? ");

		return sql;
	}



	@Override
	public void startRePost(StartRePostRequest request, StreamObserver<StartRePostResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			StartRePostResponse.Builder builder = convertStartRePost(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}
	
	private StartRePostResponse.Builder convertStartRePost(StartRePostRequest request) {
		String tableName = ValueUtil.validateNull(request.getTableName());
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		// Validate ID
		if (request.getRecordId() <= 0 && Util.isEmpty(request.getRecordUuid())) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		int recordId = request.getRecordId();
		if (recordId <= 0) {
			String recordUuid = ValueUtil.validateNull(request.getRecordUuid());
			recordId = RecordUtil.getIdFromUuid(tableName, recordUuid, null);
		}

		StartRePostResponse.Builder rePostBuilder = StartRePostResponse.newBuilder();

		int clientId = Env.getAD_Client_ID(Env.getCtx());
		int tableId = MTable.getTable_ID(request.getTableName());

		String errorMessage = DocumentEngine.postImmediate(
			Env.getCtx(), clientId,
			tableId,
			recordId,
			request.getIsForce(),
			null
		);
		if (!Util.isEmpty(errorMessage, true)) {
			rePostBuilder.setErrorMsg(errorMessage);
		}
		
		return rePostBuilder;
	}

	@Override
	public void listAccoutingFacts(ListAccoutingFactsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListEntitiesResponse.Builder entitiesList = convertListAccountingFacts(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}
	
	ListEntitiesResponse.Builder convertListAccountingFacts(ListAccoutingFactsRequest request) {
		ArrayList<Condition> conditionsList = new ArrayList<Condition>(request.getFilters().getConditionsList());
		Optional<Condition> maybeAccoutingSchemaId  = conditionsList.stream()
			.filter(condition -> {
				return condition.getColumnName().equals(I_Fact_Acct.COLUMNNAME_C_AcctSchema_ID);
			})
			.findFirst();
		if (!maybeAccoutingSchemaId.isPresent()) {
			throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
		}

		String tableName = request.getTableName();
		int tableId = -1;
		if (!Util.isEmpty(tableName, true)) {
			tableId = MTable.getTable_ID(tableName);
			if (tableId > 0) {
				org.spin.backend.grpc.common.Value.Builder tableIdValue = ValueUtil.getValueFromInt(tableId);
				Condition.Builder tableCondition = Condition.newBuilder()
					.setColumnName(I_Fact_Acct.COLUMNNAME_AD_Table_ID)
					.setValue(tableIdValue);
				conditionsList.add(0, tableCondition.build());

				// record
				int recordId = request.getRecordId();
				if (recordId <= 0 && !Util.isEmpty(request.getRecordUuid())) {
					recordId = RecordUtil.getIdFromUuid(tableName, request.getRecordUuid(), null);
				}
				if (recordId > 0) {
					org.spin.backend.grpc.common.Value.Builder recordIdValue = ValueUtil.getValueFromInt(recordId);
					Condition.Builder recordCondition = Condition.newBuilder()
						.setColumnName(I_Fact_Acct.COLUMNNAME_Record_ID)
						.setValue(recordIdValue);
					conditionsList.add(0, recordCondition.build());
				}
			}
		}
		
		Criteria.Builder filter = Criteria.newBuilder();
		filter.addAllConditions(conditionsList);

		//
		MTable table = MTable.get(Env.getCtx(), I_Fact_Acct.Table_Name);

		StringBuilder sql = new StringBuilder(QueryUtil.getTableQueryWithReferences(table));
		StringBuffer whereClause = new StringBuffer(" WHERE 1=1 ");

		// For dynamic condition
		List<Object> params = new ArrayList<>(); // includes on filters criteria
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(filter.build(), table.getTableName(), params);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//  Add includes first AND
			whereClause.append(" AND ")
				.append(dynamicWhere);
		}
		sql.append(whereClause);

		// add where with access restriction
		String parsedSQL = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(sql.toString(),
				null,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		//  Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;
 
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();

		//  Count records
		count = CountUtil.countRecords(parsedSQL, I_Fact_Acct.Table_Name, params);
		//  Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(MTable.get(Env.getCtx(), I_Fact_Acct.Table_Name), parsedSQL, params);
		//
		builder.setRecordCount(count);
		//  Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//  Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		return builder;
	}

}
