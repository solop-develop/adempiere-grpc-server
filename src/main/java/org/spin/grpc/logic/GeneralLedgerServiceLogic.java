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
package org.spin.grpc.logic;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_AD_Reference;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_M_MatchInv;
import org.adempiere.core.domains.models.I_M_MatchPO;
import org.adempiere.core.domains.models.X_Fact_Acct;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MOrg;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.general_ledger.AccountingDocument;
import org.spin.backend.grpc.general_ledger.ExistsAccoutingDocumentRequest;
import org.spin.backend.grpc.general_ledger.ExistsAccoutingDocumentResponse;
import org.spin.backend.grpc.general_ledger.ListAccountingDocumentsRequest;
import org.spin.backend.grpc.general_ledger.ListAccountingDocumentsResponse;
import org.spin.backend.grpc.general_ledger.ListAccountingSchemasRequest;
import org.spin.backend.grpc.general_ledger.ListPostingTypesRequest;
import org.spin.base.util.GeneralLedgerConvertUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.StringManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of General Ledger
 */
public class GeneralLedgerServiceLogic {

	private static String TABLE_NAME = MAccount.Table_Name;

	private final static List<String> POSTED_TABLES_WITHOUT_DOCUMENT = Arrays.asList(
		I_M_MatchInv.Table_Name,
		I_M_MatchPO.Table_Name
	);


	public static ListLookupItemsResponse.Builder listAccountingSchemas(ListAccountingSchemasRequest request) {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		List<MAcctSchema> accountingShemasList = Arrays.asList(
			MAcctSchema.getClientAcctSchema(Env.getCtx(), clientId)
		);

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(accountingShemasList.size())
		;

		accountingShemasList.stream()
			.forEach(accountingShema -> {
				LookupItem.Builder lookupBuilder = LookupUtil.convertObjectFromResult(
					accountingShema.getC_AcctSchema_ID(),
					accountingShema.getUUID(),
					null,
					accountingShema.getName(),
					accountingShema.isActive()
				);
				builderList.addRecords(lookupBuilder);
			})
		;

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listPostingTypes(ListPostingTypesRequest request) {
		// Posting Type = 125
		int referenceId = X_Fact_Acct.POSTINGTYPE_AD_Reference_ID;

		final String whereClause = I_AD_Reference.COLUMNNAME_AD_Reference_ID + " = ? ";
		Query query = new Query(
			Env.getCtx(),
			I_AD_Ref_List.Table_Name,
			whereClause,
			null
		)
			.setParameters(referenceId)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int recordCount = query.count();

		ListLookupItemsResponse.Builder builder = ListLookupItemsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		//	Get List
		query.setLimit(limit, offset)
			.<MRefList>list()
			.forEach(refList -> {
				LookupItem.Builder lookup = LookupUtil.convertLookupItemFromReferenceList(refList);
				builder.addRecords(lookup);
			})
		;

		return builder;
	}


	public static ListAccountingDocumentsResponse.Builder listAccountingDocuments(ListAccountingDocumentsRequest request) {
		final String whereClause = " IsView='N' "
			+ " AND EXISTS(SELECT 1 FROM AD_Column c"
			+ " WHERE AD_Table.AD_Table_ID = c.AD_Table_ID "
			+ " AND c.ColumnName = 'Posted')"
		;
		Query query = new Query(
			Env.getCtx(),
			I_AD_Table.Table_Name,
			whereClause,
			null
		);

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int recordCount = query.count();

		ListAccountingDocumentsResponse.Builder builderList = ListAccountingDocumentsResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		//	Get List
		List<Integer> tableAccountigIds = query
			.setLimit(limit, offset)
			.getIDsAsList()
		;

		// query.setLimit(limit, offset)
		// 	.<MRefList>list()
		tableAccountigIds.forEach(tableId -> {
			AccountingDocument.Builder accountingDocument = GeneralLedgerConvertUtil.convertAccountingDocument(tableId);
			builderList.addRecords(accountingDocument);
		});

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listOrganizations(ListAccountingSchemasRequest request) {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		MClient client = MClient.get(Env.getCtx(), clientId);

		List<MOrg> organizationList = Arrays.asList(
			MOrg.getOfClient(client)
		)
			.stream()
			.sorted(
				Comparator.comparing(MOrg::getValue)
			)
			.collect(Collectors.toList())
		;

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(organizationList.size())
		;

		organizationList.stream()
			.forEach(organization -> {
				LookupItem.Builder lookupBuilder = LookupUtil.convertObjectFromResult(
					organization.getAD_Org_ID(),
					organization.getUUID(),
					organization.getValue(),
					organization.getName(),
					organization.isActive()
				);
				builderList.addRecords(lookupBuilder);
			})
		;

		return builderList;
	}



	public static ExistsAccoutingDocumentResponse.Builder existsAccoutingDocument(ExistsAccoutingDocumentRequest request) {
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
		if (documentTable == null || documentTable.getAD_Table_ID() == 0) {
			// throw new AdempiereException("@AD_Table_ID@ @Invalid@");
			return builder;
		}

		if (documentTable.isView()) {
			return builder;
		}
		if (!documentTable.isDocument()) {
			// TODO: Remove this condition when complete support to document table
			if (!POSTED_TABLES_WITHOUT_DOCUMENT.contains(documentTable.getTableName())) {
				// With `Posted` column
				if (documentTable.getColumn(I_C_Invoice.COLUMNNAME_Posted) == null) {
					return builder;
				}
			}
		}

		// Validate record
		final int recordId = request.getRecordId();
		if (!RecordUtil.isValidId(recordId, TABLE_NAME)) {
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

}
