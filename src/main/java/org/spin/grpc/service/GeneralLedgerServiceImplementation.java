/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
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
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.DictionaryUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.general_ledger.GeneralLedgerGrpc.GeneralLedgerImplBase;
import org.spin.backend.grpc.general_ledger.GetAccountingCombinationRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingCombinationsRequest;
import org.spin.backend.grpc.general_ledger.SaveAccountingCombinationRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Paryroll Action Notice Form
 */
public class GeneralLedgerServiceImplementation extends GeneralLedgerImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(GeneralLedgerServiceImplementation.class);

	@Override
	public void getAccountingCombination(GetAccountingCombinationRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder accountingCombination = convertAccountingCombination(request);
			responseObserver.onNext(accountingCombination.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private Entity.Builder convertAccountingCombination(GetAccountingCombinationRequest request) {
		// Validate ID
		if(request.getId() == 0 && Util.isEmpty(request.getUuid())) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		Properties context = ContextManager.getContext(request.getClientRequest());
		String tableName = MAccount.Table_Name;

		MAccount accoutingCombination = null;
		if(request.getId() > 0) {
			accoutingCombination = MAccount.getValidCombination(context, request.getId(), null);
		} else if(!Util.isEmpty(request.getUuid(), true)) {
			accoutingCombination = new Query(
					context,
					tableName,
					MAccount.COLUMNNAME_UUID + " = ? ",
					null
				)
				.setParameters(request.getUuid())
				.firstOnly();
		}
		if(accoutingCombination == null) {
			throw new AdempiereException("@Error@ PO is null");
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
			ListEntitiesResponse.Builder entitiesList = convertListAccountingCombinations(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ListEntitiesResponse.Builder convertListAccountingCombinations(ListAccountingCombinationsRequest request) {
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
		String tableName = MAccount.Table_Name;
		Properties context = ContextManager.getContext(request.getClientRequest());
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		context = ContextManager.setContextWithAttributes(windowNo, context, request.getContextAttributesList());
		MTable table = MTable.get(context, tableName);
		StringBuilder sql = new StringBuilder(DictionaryUtil.getQueryWithReferencesFromColumns(table));
		StringBuffer whereClause = new StringBuffer(" WHERE 1=1 ");

		//	For dynamic condition
		List<Object> params = new ArrayList<>(); // includes on filters criteria
		String dynamicWhere = ValueUtil.getWhereClauseFromCriteria(request.getFilters(), tableName, params);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//	Add includes first AND
			whereClause.append(" AND ")
				.append(dynamicWhere);
		}
		sql.append(whereClause); 
		// add where with search value
		String parsedSQL = RecordUtil.addSearchValueAndGet(sql.toString(), tableName, request.getSearchValue(), params);

		// add where with access restriction
		parsedSQL = MRole.getDefault(context, false)
			.addAccessSQL(parsedSQL,
				null,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		//	Get page and count
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());
		int count = 0;
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();

		//	Count records
		count = RecordUtil.countRecords(parsedSQL, tableName, params);
		//	Add Row Number
		parsedSQL = RecordUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(MTable.get(context, tableName), parsedSQL, params);
		//	
		builder.setRecordCount(count);
		//	Set page token
		String nexPageToken = null;
		if(RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
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
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private Entity.Builder convertAccountingCombination(SaveAccountingCombinationRequest request) {
		// set context values
		Properties context = ContextManager.getContext(request.getClientRequest());
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		context = ContextManager.setContextWithAttributes(windowNo, context, request.getContextAttributesList());

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
		MAcctSchema accoutingSchema = MAcctSchema.get(context, accoutingSchemaId, null);

		String accountingCombinationAlias = ValueUtil.validateNull((String) contextAttributesList.get(MAccount.COLUMNNAME_Alias));
		
		List<MAcctSchemaElement> acctingSchemaElements = Arrays.asList(accoutingSchema.getAcctSchemaElements());

		Map<String, Object> attributesList = ValueUtil.convertValuesToObjects(request.getAttributesList());
		StringBuffer sql = generateSQL(acctingSchemaElements, attributesList);

		int clientId = Env.getContextAsInt(context, windowNo, MAccount.COLUMNNAME_AD_Client_ID);

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


		MAccount accountCombination = MAccount.get (
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
			String columnName = getColumnNameFromElementType(elementType);
			Object value = attributesList.get(columnName);

			if (acctingSchemaElement.isMandatory() && (value == null || (value instanceof String && Util.isEmpty((String) value, true)))) {
				throw new AdempiereException("@" + columnName + "@ @IsMandatory@");
			}

			sql.append(" AND ").append(columnName);
			if (value == null) {
				sql.append(" IS NULL ");
			} else {
				sql.append(" = ").append(value);
			}
		});

		sql.append(" AND AD_Client_ID = ? AND C_AcctSchema_ID = ? ");

		return sql;
	}

	private String getColumnNameFromElementType(String elementType) {
		String columnName = null;
		switch (elementType) {
			case MAcctSchemaElement.ELEMENTTYPE_Organization:
				columnName = MAccount.COLUMNNAME_AD_Org_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_Account:
				columnName = MAccount.COLUMNNAME_Account_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_SubAccount:
				columnName = MAccount.COLUMNNAME_C_SubAcct_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_Product:
				columnName = MAccount.COLUMNNAME_M_Product_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_BPartner:
				columnName = MAccount.COLUMNNAME_C_BPartner_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_Campaign:
				columnName = MAccount.COLUMNNAME_C_Campaign_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_Project:
				columnName = MAccount.COLUMNNAME_C_Project_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_SalesRegion:
				columnName = MAccount.COLUMNNAME_C_SalesRegion_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_OrgTrx:
				columnName = MAccount.COLUMNNAME_AD_OrgTrx_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_Activity:
				columnName = MAccount.COLUMNNAME_C_Activity_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_UserList1:
				columnName = MAccount.COLUMNNAME_User1_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_UserList2:
				columnName = MAccount.COLUMNNAME_User2_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_UserList3:
				columnName = MAccount.COLUMNNAME_User3_ID;
				break;
			case MAcctSchemaElement.ELEMENTTYPE_UserList4:
				columnName = MAccount.COLUMNNAME_User4_ID;
				break;
		}
		return columnName;
	}

}
