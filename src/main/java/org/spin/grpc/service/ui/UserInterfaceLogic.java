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

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MQuery;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.user_interface.ListTreeNodesRequest;
import org.spin.backend.grpc.user_interface.ListTreeNodesResponse;
import org.spin.backend.grpc.user_interface.TreeNode;
import org.spin.backend.grpc.user_interface.TreeType;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.service.grpc.util.base.RecordUtil;

/**
 * This class was created for add all logic methods for User Interface service
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com , https://github.com/EdwinBetanc0urt
 */
public class UserInterfaceLogic {

	public static String getWhereClauseFromChildTab(MQuery zoomQuery, MTab currentTab) {
		String whereClause = zoomQuery.getWhereClause();
		final String zoomTableName = zoomQuery.getZoomTableName();
		final String zoomColumnname = zoomQuery.getZoomColumnName();
		final MTable zoomTable = MTable.get(Env.getCtx(), zoomTableName);
		if (zoomTable != null && zoomTable.getAD_Table_ID() > 0) {
			MWindow window = MWindow.get(Env.getCtx(), currentTab.getAD_Window_ID());
			MTab mainTab = new Query(
					Env.getCtx(),
					I_AD_Tab.Table_Name,
					"AD_Window_ID = ? AND IsSortTab = 'N'",
					null
				)
				.setParameters(currentTab.getAD_Window_ID())
				.setOrderBy(I_AD_Tab.COLUMNNAME_SeqNo)
				.setOnlyActiveRecords(true)
				.first()
			;
			if (mainTab != null && mainTab.getAD_Tab_ID() > 0) {
				MTable mainTabTable = MTable.get(Env.getCtx(), mainTab.getAD_Table_ID());
				final String mainTabTableName = mainTabTable.getTableName();
				if (!zoomTableName.equalsIgnoreCase(mainTabTableName)) {
					AtomicReference<String> whereClasueReference = new AtomicReference<String>();
					List<MTab> tabsList = Arrays.asList(
						window.getTabs(false, null)
					)
						.stream()
						.filter(tabItem -> {
							if (!tabItem.isActive()) {
								return false;
							}
							if (tabItem.isSortTab()) {
								return false;
							}
							return true;
						})
						.collect(Collectors.toList())
					;
					for (MTab tabItem : tabsList) {
						MTable tabTable = MTable.get(Env.getCtx(), tabItem.getAD_Table_ID());
						final String tabTableName = tabTable.getTableName();
						if (!zoomTableName.equalsIgnoreCase(tabTableName)) {
							// break loop
							continue;
						}
						// if (mainTab.getTabLevel() < tabItem.getTabLevel()) {
						// 	// break loop
						// 	continue;
						// }

						List<MColumn> columnsList = Arrays.asList(
							tabTable.getColumns(false)
						);
						for (MColumn columnItem : columnsList) {
							final String fieldColumnName = columnItem.getColumnName();
							if (fieldColumnName.equalsIgnoreCase(zoomColumnname)) {
								String linkColumnName = "";
								if (tabItem.getAD_Column_ID() > 0) {
									MColumn linkColunm = MColumn.get(Env.getCtx(), tabItem.getAD_Column_ID());
									linkColumnName = linkColunm.getColumnName();
								}
								if (Util.isEmpty(linkColumnName, true)) {
									MColumn column = null;
									if (columnItem.isParent()) {
										column = columnItem;
									} else {
										column = new Query(
											Env.getCtx(),
											I_AD_Column.Table_Name,
											"AD_Table_ID = ? AND IsParent = 'Y'",
											null
										)
											.setParameters(tabTable.getAD_Table_ID())
											.first()
										;
									}
									if (column == null || column.getAD_Column_ID() <= 0) {
										break;
									}
									linkColumnName = column.getColumnName();
								}
								int parentId = DB.getSQLValue(null, "SELECT " + linkColumnName + " FROM " + tabTableName + " WHERE " + zoomQuery.getWhereClause());
								if (parentId <= 0) {
									// break loop
									continue;
								}

								String newWhereClause = "";
								if (!Util.isEmpty(zoomQuery.getWhereClause(), true)) {
									newWhereClause = " AND " + zoomQuery.getWhereClause();
								}
								final String tableAlias = tabTableName;
								String whereExists = ""
									+ " EXISTS("
										+ "SELECT 1 FROM " + tabTableName + " AS " + tableAlias + " "
										+ "WHERE "
										+ mainTabTableName + "." + linkColumnName + " = " + tableAlias + "." + linkColumnName
										+ newWhereClause + " "
									+ ") "
								;
								whereClasueReference.set(whereExists);
								break;
							}
						}
					}

					if (whereClasueReference.get() != null) {
						whereClause = whereClasueReference.get();
					}
				}
			}
		}

		return whereClause;
	}

	public static ListTreeNodesResponse.Builder listTreeNodes(ListTreeNodesRequest request) {
		if (Util.isEmpty(request.getTableName(), true) && request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		Properties context = Env.getCtx();

		// get element id
		final int elementId = request.getElementId();
		MTable table = null;
		// tab where clause
		String whereClause = null;
		if (request.getTabId() > 0) {
			MTab tab = MTab.get(context, request.getTabId());
			if (tab == null || tab.getAD_Tab_ID() <= 0) {
				throw new AdempiereException(
					"@AD_Tab_ID@ " + request.getTabId() + " @NotFound@"
				);
			}

			table = MTable.get(context, tab.getAD_Table_ID());
			final String whereTab = WhereClauseUtil.getWhereClauseFromTab(tab.getAD_Tab_ID());
			//	Fill context
			int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
			ContextManager.setContextWithAttributesFromStruct(windowNo, context, null);
			String parsedWhereClause = Env.parseContext(context, windowNo, whereTab, false);
			if (Util.isEmpty(parsedWhereClause, true) && !Util.isEmpty(whereTab, true)) {
				throw new AdempiereException(
					"@AD_Tab_ID@ " + tab.getName() + " (" + tab.getAD_Tab_ID() + "), @WhereClause@ @Unparseable@"
				);
			}
			whereClause = parsedWhereClause;
		} else {
			// validate and get table
			table = RecordUtil.validateAndGetTable(
				request.getTableName()
			);
		}
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException(
				"@AD_Table_ID@ " + request.getTableName() + " @NotFound@"
			);
		}
		if (!MTree.hasTree(table.getAD_Table_ID())) {
			throw new AdempiereException(
				"@AD_Table_ID@ " + table.getName() + " (" + table.getAD_Table_ID() + "), @AD_Tree_ID@ @NotFound@"
			);
		}

		ListTreeNodesResponse.Builder builder = ListTreeNodesResponse.newBuilder();

		final int clientId = Env.getAD_Client_ID(context);
		final int treeId = getDefaultTreeIdFromTableName(clientId, table.getTableName(), elementId);
		if (treeId <= 0) {
			return builder;
		}
		MTree tree = new MTree(context, treeId, false, true, whereClause, null);
		if (tree == null || tree.getAD_Tree_ID() <= 0) {
			return builder;
		}

		MTreeNode treeNode = tree.getRoot();
		final int treeNodeId = request.getId();

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
				throw new AdempiereException(
					"@Node_ID@ " + treeNodeId + " @NotFound@"
				);
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

	public static int getDefaultTreeIdFromTableName(int clientId, String tableName) {
		return getDefaultTreeIdFromTableName(clientId, tableName, 0);
	}
	public static int getDefaultTreeIdFromTableName(int clientId, String tableName, int elementId) {
		if (Util.isEmpty(tableName, true)) {
			return -1;
		}
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			return -1;
		}
		// call main method
		return getDefaultTreeIdFromTableId(clientId, table.getAD_Table_ID(), elementId);
	}

	public static int getDefaultTreeIdFromTableId(int clientId, int tableId) {
		// MTree.getDefaultTreeIdFromTableId(menu.getAD_Client_ID(), I_AD_Menu.Table_ID);
		return getDefaultTreeIdFromTableId(clientId, tableId, 0);
	}
	public static int getDefaultTreeIdFromTableId(int clientId, int tableId, int elementId) {
		int treeId = -1;
		if(tableId <= 0) {
			return treeId;
		}
		MTable table = MTable.get(Env.getCtx(), tableId);
		if (table == null || table.getAD_Table_ID() <= 0) {
			return treeId;
		}
		//
		String elementWhereClause = "";
		//	Valid Accounting Element
		if (elementId > 0) {
			elementWhereClause = " AND EXISTS ("
				+ "SELECT 1 FROM C_Element ae "
				+ "WHERE ae.C_Element_ID = " + elementId
				+ " AND tr.AD_Tree_ID = ae.AD_Tree_ID "
				+ ") "
			;
		}
		final String sql = "SELECT tr.AD_Tree_ID "
			+ "FROM AD_Tree AS tr "
			+ "WHERE tr.IsActive = 'Y' "
			+ "AND tr.AD_Client_ID IN(0, ?) "
			+ "AND tr.AD_Table_ID = ? "
			+ "AND tr.IsAllNodes = 'Y' "
			+ elementWhereClause
			+ "AND ROWNUM = 1 "
			+ "ORDER BY tr.AD_Client_ID DESC, tr.IsDefault DESC, tr.AD_Tree_ID"
		;
		//	Get Tree
		treeId = DB.getSQLValue(null, sql, clientId, table.getAD_Table_ID());
		//	Default Return
		return treeId;
	}

}
