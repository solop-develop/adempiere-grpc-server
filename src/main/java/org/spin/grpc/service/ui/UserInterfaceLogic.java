/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/

package org.spin.grpc.service.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.user_interface.ListGeneralSearchRecordsRequest;
import org.spin.backend.grpc.user_interface.ListTreeNodesRequest;
import org.spin.backend.grpc.user_interface.ListTreeNodesResponse;
import org.spin.backend.grpc.user_interface.TreeNode;
import org.spin.backend.grpc.user_interface.TreeType;
import org.spin.base.db.CountUtil;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * This class was created for add all logic methods for User Interface service
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com , https://github.com/EdwinBetanc0urt
 */
public class UserInterfaceLogic {

	/**
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	public static ListEntitiesResponse.Builder listGeneralSearchRecords(ListGeneralSearchRecordsRequest request) {

		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			request.getTableName()
		);

		final MTable table = RecordUtil.validateAndGetTable(
			reference.TableName
		);

		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, Env.getCtx(), request.getContextAttributes()
		);

		//
		StringBuilder sql = new StringBuilder(QueryUtil.getTableQueryWithReferences(table));

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql.toString(),
				null,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();

		// validation code of field
		String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
			table.getTableName(),
			reference.ValidationCode
		);
		String parsedValidationCode = Env.parseContext(Env.getCtx(), windowNo, validationCode, false);
		if (!Util.isEmpty(reference.ValidationCode, true)) {
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException("@WhereClause@ @Unparseable@");
			}
			whereClause.append(" AND ").append(parsedValidationCode);
		}

		//	For dynamic condition
		List<Object> params = new ArrayList<>(); // includes on filters criteria
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(
			request.getFilters(),
			table.getTableName(),
			params
		);
		if (!Util.isEmpty(dynamicWhere, true)) {
			//	Add includes first AND
			whereClause.append(" AND ")
				.append("(")
				.append(dynamicWhere)
				.append(")");
		}

		sqlWithRoleAccess += whereClause;
		String parsedSQL = RecordUtil.addSearchValueAndGet(
			sqlWithRoleAccess,
			table.getTableName(),
			request.getSearchValue(),
			false,
			params
		);

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		
		//	Count records
		count = CountUtil.countRecords(
			parsedSQL,
			table.getTableName(),
			params
		);
		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		builder = RecordUtil.convertListEntitiesResult(
			table,
			parsedSQL,
			params
		);
		//	
		builder.setRecordCount(count);
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		return builder;
	}



	public static ListTreeNodesResponse.Builder listTreeNodes(ListTreeNodesRequest request) {
		if (Util.isEmpty(request.getTableName(), true) && request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		Properties context = Env.getCtx();

		// get element id
		int elementId = request.getElementId();
		MTable table = null;
		// tab where clause
		String whereClause = null;
		if (request.getTabId() > 0) {
			MTab tab = MTab.get(context, request.getTabId());
			if (tab == null || tab.getAD_Tab_ID() <= 0) {
				throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
			}

			table = MTable.get(context, tab.getAD_Table_ID());
			final String whereTab = WhereClauseUtil.getWhereClauseFromTab(tab.getAD_Tab_ID());
			//	Fill context
			int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
			ContextManager.setContextWithAttributesFromStruct(windowNo, context, null);
			String parsedWhereClause = Env.parseContext(context, windowNo, whereTab, false);
			if (Util.isEmpty(parsedWhereClause, true) && !Util.isEmpty(whereTab, true)) {
				throw new AdempiereException("@AD_Tab_ID@ @WhereClause@ @Unparseable@");
			}
			whereClause = parsedWhereClause;
		} else {
			// validate and get table
			table = RecordUtil.validateAndGetTable(
				request.getTableName()
			);
		}
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		if (!MTree.hasTree(table.getAD_Table_ID())) {
			throw new AdempiereException("@AD_Table_ID@ + @AD_Tree_ID@ @NotFound@");
		}

		final int clientId = Env.getAD_Client_ID(context);
		int treeId = getDefaultTreeIdFromTableName(clientId, table.getTableName(), elementId);
		MTree tree = new MTree(context, treeId, false, true, whereClause, null);

		MTreeNode treeNode = tree.getRoot();

		int treeNodeId = request.getId();
		ListTreeNodesResponse.Builder builder = ListTreeNodesResponse.newBuilder();

		TreeType.Builder treeTypeBuilder = UserInterfaceConvertUtil.convertTreeType(tree.getTreeType());
		builder.setTreeType(treeTypeBuilder);

		// list child nodes
		Enumeration<?> childrens = Collections.emptyEnumeration();
		if (treeNodeId <= 0) {
			// get root children's
			childrens = treeNode.children();
			builder.setRecordCount(treeNode.getChildCount());
		} else {
			// get current node
			MTreeNode currentNode = treeNode.findNode(treeNodeId);
			if (currentNode == null) {
				throw new AdempiereException("@Node_ID@ @NotFound@");
			}
			childrens = currentNode.children();
			builder.setRecordCount(currentNode.getChildCount());
		}

		final boolean isWhitChilds = true;
		while (childrens.hasMoreElements()) {
			MTreeNode child = (MTreeNode) childrens.nextElement();
			TreeNode.Builder childBuilder = UserInterfaceConvertUtil.convertTreeNode(table, child, isWhitChilds);
			builder.addRecords(childBuilder.build());
		}

		return builder;
	}

	public static int getDefaultTreeIdFromTableName(int clientId, String tableName, int elementId) {
		if(Util.isEmpty(tableName)) {
			return -1;
		}
		//
		Integer treeId = null;
		String whereClause = new String();
		//	Valid Accouting Element
		if (elementId > 0) {
			whereClause = " AND EXISTS ("
				+ "SELECT 1 FROM C_Element ae "
				+ "WHERE ae.C_Element_ID=" + elementId
				+ " AND tr.AD_Tree_ID=ae.AD_Tree_ID) "
			;
		}
		if(treeId == null || treeId == 0) {
			String sql = "SELECT tr.AD_Tree_ID "
				+ "FROM AD_Tree tr "
				+ "INNER JOIN AD_Table tb ON (tr.AD_Table_ID=tb.AD_Table_ID) "
				+ "WHERE tr.AD_Client_ID IN(0, ?) "
				+ "AND tb.TableName=? "
				+ "AND tr.IsActive='Y' "
				+ "AND tr.IsAllNodes='Y' "
				+ whereClause
				+ "ORDER BY tr.AD_Client_ID DESC, tr.IsDefault DESC, tr.AD_Tree_ID"
			;
			//	Get Tree
			treeId = DB.getSQLValue(null, sql, clientId, tableName);
		}
		//	Default Return
		return treeId;
	}

}
