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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_ValidCombination;
import org.adempiere.core.domains.models.I_Fact_Acct;
import org.adempiere.core.domains.models.X_C_AcctSchema_Element;
import org.adempiere.core.domains.models.X_Fact_Acct;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.GridWindowVO;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.grpc.logic.GeneralLedgerServiceLogic;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.general_ledger.GeneralLedgerGrpc.GeneralLedgerImplBase;
import org.spin.backend.grpc.general_ledger.AccoutingElement;
import org.spin.backend.grpc.general_ledger.ExistsAccoutingDocumentRequest;
import org.spin.backend.grpc.general_ledger.ExistsAccoutingDocumentResponse;
import org.spin.backend.grpc.general_ledger.GetAccountingCombinationRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingCombinationsRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingDocumentsRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingDocumentsResponse;
import org.spin.backend.grpc.general_ledger.ListAccountingFactsRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingSchemasRequest;
import org.spin.backend.grpc.general_ledger.ListAccoutingElementValuesRequest;
import org.spin.backend.grpc.general_ledger.ListAccoutingElementsRequest;
import org.spin.backend.grpc.general_ledger.ListAccoutingElementsResponse;
import org.spin.backend.grpc.general_ledger.ListPostingTypesRequest;
import org.spin.backend.grpc.general_ledger.SaveAccountingCombinationRequest;
import org.spin.backend.grpc.general_ledger.StartRePostRequest;
import org.spin.backend.grpc.general_ledger.StartRePostResponse;
import org.spin.backend.grpc.user_interface.GetTabEntityRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Paryroll Action Notice Form
 */
public class GeneralLedger extends GeneralLedgerImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(GeneralLedger.class);

	private String tableName = MAccount.Table_Name;

	private int ACCOUNT_COMBINATION_TAB = 207;


	@Override
	public void listAccoutingElements(ListAccoutingElementsRequest request, StreamObserver<ListAccoutingElementsResponse> responseObserver) {
		try {
			ListAccoutingElementsResponse.Builder accountingElementsList = listAccoutingElements(request);
			responseObserver.onNext(accountingElementsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	ListAccoutingElementsResponse.Builder listAccoutingElements(ListAccoutingElementsRequest request) {
		int accountingSchemaId = request.getAccoutingSchemaId();
		if (accountingSchemaId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
		}
		Properties context = Env.getCtx();
		MAcctSchema accoutingSchema = MAcctSchema.get(context, accountingSchemaId);
		if (accoutingSchema == null || accoutingSchema.getC_AcctSchema_ID() <= 0) {
			throw new AdempiereException("@C_AcctSchema_ID@ @NotFound@");
		}

		ListAccoutingElementsResponse.Builder builderList = ListAccoutingElementsResponse.newBuilder();
		MRole role = MRole.getDefault();
		if (role == null || !role.isShowAcct()) {
			log.warning(
				Msg.translate(context, "@AccessTableNoView@")
			);
			return builderList;
		}

		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		int AD_Window_ID = 153; // Maintain Account Combinations
		GridWindowVO wVO = GridWindowVO.create(context, windowNo, AD_Window_ID);
		if (wVO == null) {
			log.warning(
				Msg.translate(context, "@AccessTableNoView@")
			);
			return builderList;
		}

		GridWindow m_mWindow = new GridWindow (wVO);
		GridTab mTab = m_mWindow.getTab(0);
		if (!mTab.isLoadComplete()) {
			m_mWindow.initTab(0);
		}

		if (accoutingSchema.isHasAlias()) {
			AccoutingElement.Builder elementBuilder = AccoutingElement.newBuilder()
				.setColumnName(I_C_ValidCombination.COLUMNNAME_Combination)
				.setSequece(0)
				.setDisplayType(DisplayType.Text)
				.setIsMandatory(false)
			;
			GridField field = mTab.getField(I_C_ValidCombination.COLUMNNAME_Combination);
			if (field != null) {
				elementBuilder.setName(
						field.getHeader()
					)
					.setFieldId(field.getAD_Field_ID())
				;
			}

			builderList.addAccoutingElements(elementBuilder);
		}

		List.of(accoutingSchema.getAcctSchemaElements()).forEach(accoutingElement -> {
			String columnName = accoutingElement.getColumnName();
			AccoutingElement.Builder elemeBuilder = AccoutingElement.newBuilder()
				.setColumnName(
					columnName
				)
				.setSequece(
					accoutingElement.getSeqNo()
				)
				.setIsMandatory(
					accoutingElement.isMandatory()
				)
				.setIsBalanced(
					accoutingElement.isBalanced()
				)
				.setElementType(
					ValueManager.validateNull(
						accoutingElement.getElementType()
					)
				)
			;

			GridField field = mTab.getField(columnName);
			if (field != null) {
				elemeBuilder.setFieldId(field.getAD_Field_ID())
					.setName(
						field.getHeader()
					)
					.setDisplayType(
						field.getDisplayType()
					)
				;
				if (ReferenceUtil.validateReference(field.getDisplayType())) {
					int columnId = MColumn.getColumn_ID(mTab.getTableName(), field.getColumnName());
					MColumn column = MColumn.get(context, columnId);
					MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
						field.getDisplayType(), field.getAD_Reference_Value_ID(), field.getColumnName(), column.getAD_Val_Rule_ID()
					);
					if (info != null) {
						List<String> contextColumnsList = ContextManager.getContextColumnNames(
							Optional.ofNullable(info.QueryDirect).orElse("")
							+ Optional.ofNullable(info.Query).orElse("")
							+ Optional.ofNullable(info.ValidationCode).orElse("")
						);
						elemeBuilder.addAllContextColumnNames(contextColumnsList);
					}
				}
			}

			builderList.addAccoutingElements(elemeBuilder);
		});

		builderList.setRecordCount(
			builderList.getAccoutingElementsCount()
		);

		return builderList;
	}




	@Override
	public void listAccoutingElementValues(ListAccoutingElementValuesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder accountingElementsList = listAccoutingElementValues(request);
			responseObserver.onNext(accountingElementsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private ListLookupItemsResponse.Builder listAccoutingElementValues(ListAccoutingElementValuesRequest request) {
		int accountingSchemaId = request.getAccoutingSchemaId();
		if (accountingSchemaId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
		}
		Properties context = Env.getCtx();
		MAcctSchema accoutingSchema = MAcctSchema.get(context, accountingSchemaId);
		if (accoutingSchema == null || accoutingSchema.getC_AcctSchema_ID() <= 0) {
			throw new AdempiereException("@C_AcctSchema_ID@ @NotFound@");
		}

		String elementType = request.getElementType();
		if (Util.isEmpty(elementType, true)) {
			throw new AdempiereException("@C_AcctSchema_ID@ @ElementType@");
		}

		MAcctSchemaElement schemaElement = accoutingSchema.getAcctSchemaElement(elementType);
		if (schemaElement == null || schemaElement.getC_AcctSchema_Element_ID() <= 0) {
			throw new AdempiereException("@C_AcctSchema_Element_ID@ @NotFound@");
		}
		ListLookupItemsResponse.Builder accountingElementsList = ListLookupItemsResponse.newBuilder();

		int columnId = MColumn.getColumn_ID(
			I_C_ValidCombination.Table_Name,
			schemaElement.getColumnName()
		);
		MColumn column = MColumn.get(context, columnId);
		if (column == null) {
			return accountingElementsList;
		}

		MField field = new Query(
			Env.getCtx(),
			I_AD_Field.Table_Name,
			"AD_Tab_ID = ? AND AD_Column_ID = ?",
			null
		)
			.setParameters(this.ACCOUNT_COMBINATION_TAB, column.getAD_Column_ID())
			.first()
		;
		if (field == null || field.getAD_Field_ID() <= 0) {
			return accountingElementsList;
		}

		int displayTypeId = column.getAD_Reference_ID();
		int referenceValueId = column.getAD_Reference_Value_ID();
		int validationRuleId = column.getAD_Val_Rule_ID();
		if (field.getAD_Reference_ID() > 0) {
			displayTypeId = field.getAD_Reference_ID();
		}
		if (field.getAD_Reference_Value_ID() > 0) {
			referenceValueId = field.getAD_Reference_Value_ID();
		}
		if (field.getAD_Val_Rule_ID() > 0) {
			validationRuleId = field.getAD_Val_Rule_ID();
		}
		MLookupInfo info = ReferenceUtil.getReferenceLookupInfo(
			displayTypeId, referenceValueId, column.getColumnName(), validationRuleId
		);

		if (info == null) {
			throw new AdempiereException("@AD_Reference_ID@ @NotFound@");
		}

		return UserInterface.listLookupItems(
			info,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);
	}



	@Override
	public void getAccountingCombination(GetAccountingCombinationRequest request, StreamObserver<Entity> responseObserver) {
		try {
			Entity.Builder accountingCombination = getAccountingCombination(request);
			responseObserver.onNext(accountingCombination.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private Entity.Builder getAccountingCombination(GetAccountingCombinationRequest request) {
		// Validate ID
		if(request.getId() == 0 && Util.isEmpty(request.getValue())) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}

		MAccount accountingCombination = null;
		if(request.getId() > 0) {
			accountingCombination = MAccount.getValidCombination(Env.getCtx(), request.getId(), null);
		} else if (!Util.isEmpty(request.getValue(), true)) {
			// Value as combination
			accountingCombination = new Query(
					Env.getCtx(),
					this.tableName,
					MAccount.COLUMNNAME_Combination + " = ? ",
					null
				)
				.setParameters(request.getValue())
				.firstOnly();
		}
		if(accountingCombination == null) {
			throw new AdempiereException("@Error@ @AccountCombination@ @not.found@");
		}


		GetTabEntityRequest.Builder getEntityBuilder = GetTabEntityRequest.newBuilder()
			.setTabId(this.ACCOUNT_COMBINATION_TAB)
			.setId(accountingCombination.get_ID())
		;

		Entity.Builder entityBuilder = UserInterface.getTabEntity(
			getEntityBuilder.build(),
			new ArrayList<Object>()
		);

		return entityBuilder;
	}



	@Override
	public void listAccountingCombinations(ListAccountingCombinationsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			ListEntitiesResponse.Builder entitiesList = listAccountingCombinations(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private ListEntitiesResponse.Builder listAccountingCombinations(ListAccountingCombinationsRequest request) {
		// Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, Env.getCtx(), request.getContextAttributes());

		int organizationId = request.getOrganizationId();
		if (request.getOrganizationId() <= 0) {
			// throw new AdempiereException("@Org0NotAllowed@");
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		}

		int accountId = request.getAccountId();
		if (request.getAccountId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @Account_ID@");
		}

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
		sqlWithRoleAccess += " AND (C_ValidCombination.AD_Org_ID = ? AND C_ValidCombination.Account_ID = ?) ";
		params.add(organizationId);
		params.add(accountId);

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
		builder.setNextPageToken(ValueManager.validateNull(nexPageToken));

		return builder;
	}

	@Override
	public void saveAccountingCombination(SaveAccountingCombinationRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder entity = saveAccountingCombination(request);
			responseObserver.onNext(entity.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private Entity.Builder saveAccountingCombination(SaveAccountingCombinationRequest request) {
		// Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromStruct(windowNo, Env.getCtx(), request.getContextAttributes());

		final int organizationId = request.getOrganizationId();
		if (organizationId <= 0) {
			// throw new AdempiereException("@Org0NotAllowed@");
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		}

		final int accountId = request.getAccountId();
		if (accountId <= 0) {
			throw new AdempiereException("@FillMandatory@ @Account_ID@");
		}

		final int accountingSchemaId = request.getAccountingSchemaId();
		if (accountingSchemaId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
		}
		MAcctSchema accountingSchema = MAcctSchema.get(Env.getCtx(), accountingSchemaId, null);

		final String accountingCombinationAlias = ValueManager.validateNull(
			request.getAlias()
		);

		List<MAcctSchemaElement> acctingSchemaElements = Arrays.asList(accountingSchema.getAcctSchemaElements());

		Map<String, Object> attributesList = ValueManager.convertValuesMapToObjects(request.getAttributes().getFieldsMap());
		StringBuffer sql = generateSQL(acctingSchemaElements, attributesList);

		int clientId = Env.getContextAsInt(Env.getCtx(), windowNo, MAccount.COLUMNNAME_AD_Client_ID);

		int accountingCombinationId = 0;
		String accountingAlias = "";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), null);
			pstmt.setInt(1, clientId);
			pstmt.setInt(2, accountingSchema.getC_AcctSchema_ID());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				accountingCombinationId = rs.getInt(1);
				accountingAlias = ValueManager.validateNull(
					rs.getString(2)
				);
			}
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql.toString(), e);
			accountingCombinationId = 0;
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	We have an account like this already - check alias
		if (accountingCombinationId > 0) {
			if (accountingSchema.isHasAlias() && !accountingCombinationAlias.equals(accountingAlias)) {
				sql = new StringBuffer("UPDATE C_ValidCombination SET Alias = ");
				if (Util.isEmpty(accountingCombinationAlias, true)) {
					sql.append("NULL");
				} else {
					sql.append("'").append(accountingCombinationAlias).append("'");
				}
				sql.append(" WHERE C_ValidCombination_ID=").append(accountingCombinationId);
				int rowChanges = 0;
				PreparedStatement stmt = null;
				try {
					stmt = DB.prepareStatement(
						sql.toString(),
						ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_UPDATABLE,
						null
					);
					rowChanges = stmt.executeUpdate();
				}
				catch (SQLException e) {
					log.log(Level.SEVERE, sql.toString(), e);
				}
				finally {
					DB.close(stmt);
				}
				if (rowChanges == 0) {
					// FDialog.error(m_WindowNo, this, "AccountNotUpdated");
				}
			}

			// loadInfo(accountingCombinationId, accountingSchema.getC_AcctSchema_ID());
			// action_Find(false);
			// return;
		}

		log.config("New Accouting Combination");
		MAccount accountCombination = setAccountingCombinationByAttributes(clientId, organizationId, accountingSchemaId, accountId, attributesList);
		
		Entity.Builder builder = ConvertUtil.convertEntity(accountCombination);

		return builder;
	}
	
	private MAccount setAccountingCombinationByAttributes(int clientId, int organizationId, int accountingSchemaId, int accountId, Map<String, Object> attributesList) {
		String accountingAlias = null;
		if (attributesList.get(MAccount.COLUMNNAME_Alias) != null) {
			accountingAlias = (String) attributesList.get(MAccount.COLUMNNAME_Alias);
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
			accountingSchemaId,
			accountId, subAccountId,
			productId, businessPartnerId, organizationTrxId,
			locationFromId,locationToId, salesRegionId,
			projectId, campaignId, activityId,
			user1Id, user2Id , user3Id , user4Id,
			0, 0, null
		);
		
		if (!Util.isEmpty(accountingAlias, true) && accountCombination != null && accountCombination.getAccount_ID() > 0) {
			accountCombination.setAlias(accountingAlias);
			accountCombination.saveEx();
		}
		return accountCombination;
	}

	private StringBuffer generateSQL(List<MAcctSchemaElement> acctingSchemaElements, Map<String, Object> attributesList) {
		StringBuffer sql = new StringBuffer ("SELECT C_ValidCombination_ID, Alias FROM C_ValidCombination WHERE 1=1 ");

		acctingSchemaElements.forEach(acctingSchemaElement -> {
			String elementType = acctingSchemaElement.getElementType();
			String columnName = MAcctSchemaElement.getColumnName(elementType);

			// The alias does not affect the query criteria
			if (columnName == MAccount.COLUMNNAME_Alias) {
				return;
			}

			Object value = attributesList.get(columnName);

			boolean isEmptyValue = value == null || (value instanceof String && Util.isEmpty((String) value, true));
			if (acctingSchemaElement.isMandatory() && isEmptyValue) {
				throw new AdempiereException("@" + columnName + "@ @IsMandatory@");
			}

			sql.append(" AND ").append(columnName);
			if (isEmptyValue) {
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
				throw new AdempiereException("StartRePostRequest Null");
			}
			StartRePostResponse.Builder builder = startRePost(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private StartRePostResponse.Builder startRePost(StartRePostRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		// Validate ID
		final int recordId = request.getRecordId();
		RecordUtil.validateRecordId(recordId, table.getAccessLevel());
		StartRePostResponse.Builder rePostBuilder = StartRePostResponse.newBuilder();

		int clientId = Env.getAD_Client_ID(Env.getCtx());

		String errorMessage = DocumentEngine.postImmediate(
			Env.getCtx(), clientId,
			table.getAD_Table_ID(),
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
	public void listAccountingSchemas(ListAccountingSchemasRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder entitiesList = GeneralLedgerServiceLogic.listAccountingSchemas(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	@Override
	public void listPostingTypes(ListPostingTypesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListLookupItemsResponse.Builder entitiesList = GeneralLedgerServiceLogic.listPostingTypes(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	@Override
	public void listAccountingDocuments(ListAccountingDocumentsRequest request, StreamObserver<ListAccountingDocumentsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListAccountingDocumentsResponse.Builder entitiesList = GeneralLedgerServiceLogic.listAccountingDocuments(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	@Override
	public void existsAccoutingDocument(ExistsAccoutingDocumentRequest request, StreamObserver<ExistsAccoutingDocumentResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("ExistsAccoutingDocumentRequest Null");
			}
			ExistsAccoutingDocumentResponse.Builder builder = existsAccoutingDocument(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private ExistsAccoutingDocumentResponse.Builder existsAccoutingDocument(ExistsAccoutingDocumentRequest request) {
		ExistsAccoutingDocumentResponse.Builder builder = ExistsAccoutingDocumentResponse.newBuilder();
		MRole role = MRole.getDefault();
		if (role == null || !role.isShowAcct()) {
			return builder;
		}

		// Validate accounting schema
		int acctSchemaId = request.getAccountingSchemaId();
		if (acctSchemaId <= 0) {
			// throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
			return builder;
		}

		// Validate table
		if (Util.isEmpty(request.getTableName(), true)) {
			// throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
			return builder;
		}
		final MTable documentTable = MTable.get(Env.getCtx(), request.getTableName());
		if (documentTable == null || documentTable.getAD_Table_ID() == 0 || !documentTable.isDocument()) {
			// throw new AdempiereException("@AD_Table_ID@ @Invalid@");
			return builder;
		}

		// Validate record
		final int recordId = request.getRecordId();
		if (!RecordUtil.isValidId(recordId, tableName)) {
			// throw new AdempiereException("@FillMandatory@ @Record_ID@");
			return builder;
		}
		PO record = RecordUtil.getEntity(Env.getCtx(), documentTable.getTableName(), recordId, null);
		if (record == null || record.get_ID() <= 0) {
			return builder;
		}

		// Validate `Posted` column
		if (record.get_ColumnIndex(I_C_Invoice.COLUMNNAME_Posted) < 0) {
			// without `Posted` button
			return builder;
		}

		// Validate `Processed` column
		if (record.get_ColumnIndex(I_C_Invoice.COLUMNNAME_Processed) < 0) {
			return builder;
		}
		if (!record.get_ValueAsBoolean(I_C_Invoice.COLUMNNAME_Processed)) {
			return builder;
		}

		builder.setIsShowAccouting(true);
		return builder;
	}



	@Override
	public void listAccountingFacts(ListAccountingFactsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListEntitiesResponse.Builder entitiesList = listAccountingFacts(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	ListEntitiesResponse.Builder listAccountingFacts(ListAccountingFactsRequest request) {
		int acctSchemaId = request.getAccountingSchemaId();
		if (acctSchemaId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_AcctSchema_ID@");
		}

		//
		MTable table = MTable.get(Env.getCtx(), I_Fact_Acct.Table_Name);
		StringBuilder sql = new StringBuilder(QueryUtil.getTableQueryWithReferences(table));

		List<Object> filtersList = new ArrayList<>();
		StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
		whereClause.append(" AND ")
			.append(I_Fact_Acct.Table_Name)
			.append(".")
			.append(I_Fact_Acct.COLUMNNAME_C_AcctSchema_ID)
			.append(" = ? ")
		;
		filtersList.add(acctSchemaId);

		//	Accounting Elements
		List<MAcctSchemaElement> acctSchemaElements = new Query(
			Env.getCtx(),
			MAcctSchemaElement.Table_Name,
			" C_AcctSchema_ID = ?" ,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(acctSchemaId)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.<MAcctSchemaElement>list()
		;
		List<Filter> conditionsList = FilterManager.newInstance(request.getFilters()).getConditions();
		acctSchemaElements.forEach(acctSchemaElement -> {
			if (acctSchemaElement.getElementType().equals(X_C_AcctSchema_Element.ELEMENTTYPE_Organization)) {
				// Organization filter is inside the request
				return;
			}

			String columnName = MAcctSchemaElement.getColumnName(
				acctSchemaElement.getElementType()
			);

			Filter elementAccount = conditionsList.parallelStream()
				.filter(condition -> {
					return condition.getColumnName().equals(columnName);
				})
				.findFirst()
				.orElse(null)
			;
			if (elementAccount == null) {
				return;
			}
			Object value = elementAccount.getValue();
			if (value == null) {
				return;
			}
			whereClause.append(" AND ")
				.append(I_Fact_Acct.Table_Name)
				.append(".")
				.append(columnName)
				.append(" = ? ")
			;
			filtersList.add(value);
		});

		// Posting Type
		if (!Util.isEmpty(request.getPostingType(), true)) {
			final String postingType = request.getPostingType();
			MRefList referenceList = MRefList.get(Env.getCtx(), X_Fact_Acct.POSTINGTYPE_AD_Reference_ID, postingType, null);
			if (referenceList == null) {
				throw new AdempiereException("@AD_Ref_List_ID@ @Invalid@: " + postingType);
			}
			whereClause.append(" AND ")
				.append(I_Fact_Acct.Table_Name)
				.append(".")
				.append(I_Fact_Acct.COLUMNNAME_PostingType)
				.append(" = ? ")
			;
			filtersList.add(postingType);
		}

		// Date
		Timestamp dateFrom = ValueManager.getDateFromTimestampDate(request.getDateFrom());
		Timestamp dateTo = ValueManager.getDateFromTimestampDate(request.getDateTo());
		if (dateFrom != null || dateTo != null) {
			whereClause.append(" AND ");
			if (dateFrom != null && dateTo != null) {
				whereClause.append("TRUNC(")
					.append(I_Fact_Acct.Table_Name)
					.append(".DateAcct, 'DD') BETWEEN ? AND ? ")
				;
				filtersList.add(dateFrom);
				filtersList.add(dateTo);
			}
			else if (dateFrom != null) {
				whereClause.append("TRUNC(")
					.append(I_Fact_Acct.Table_Name)
					.append(".DateAcct, 'DD') >= ? ")
				;
				filtersList.add(dateFrom);
			}
			else {
				// DateTo != null
				whereClause.append("TRUNC(")
					.append(I_Fact_Acct.Table_Name)
					.append(".DateAcct, 'DD') <= ? ")
				;
				filtersList.add(dateTo);
			}
		}

		// Document
		if (!Util.isEmpty(request.getTableName(), true)) {
			final MTable documentTable = MTable.get(Env.getCtx(), request.getTableName());
			if (documentTable == null || documentTable.getAD_Table_ID() == 0) {
				throw new AdempiereException("@AD_Table_ID@ @Invalid@");
			}
			// validate record
			final int recordId = request.getRecordId();
			RecordUtil.validateRecordId(recordId, documentTable.getAccessLevel());

			// table
			whereClause.append(" AND ")
				.append(I_Fact_Acct.Table_Name)
				.append(".")
				.append(I_Fact_Acct.COLUMNNAME_AD_Table_ID)
				.append(" = ? ")
			;
			filtersList.add(documentTable.getAD_Table_ID());

			// record
			whereClause.append(" AND ")
				.append(I_Fact_Acct.Table_Name)
				.append(".")
				.append(I_Fact_Acct.COLUMNNAME_Record_ID)
				.append(" = ? ")
			;
			filtersList.add(recordId);
		}

		// Organization
		if (request.getOrganizationId() > 0) {
			whereClause.append(" AND ")
				.append(I_Fact_Acct.Table_Name)
				.append(".")
				.append(I_Fact_Acct.COLUMNNAME_AD_Org_ID)
				.append(" = ? ")
			;
			filtersList.add(request.getOrganizationId());
		}

		// add where with access restriction
		String sqlWithRescriction = sql.toString() + whereClause.toString();
		String parsedSQL = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(sqlWithRescriction,
				I_Fact_Acct.Table_Name,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		//  Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
 
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();

		//  Count records
		int count = CountUtil.countRecords(parsedSQL, I_Fact_Acct.Table_Name, filtersList);
		//  Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(table, parsedSQL, filtersList);
		//
		builder.setRecordCount(count);
		//  Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//  Set next page
		builder.setNextPageToken(ValueManager.validateNull(nexPageToken));

		return builder;
	}

}
