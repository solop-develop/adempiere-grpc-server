/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2023 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.eca56.util.support.documents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.core.domains.models.I_AD_TreeNodeMM;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTree;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.spin.eca56.util.support.DictionaryDocument;

/**
 * 	The document class for Menu Tree sender
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class MenuTree extends DictionaryDocument {

	public static final String CHANNEL = "menu_tree";
	public static final String KEY = "new";

	@Override
	public String getKey() {
		return KEY;
	}

	private Map<String, Object> convertNode(TreeNodeReference node) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("node_id", node.getNodeId());
		detail.put("internal_id", node.getNodeId());
		detail.put("parent_id", node.getParentId());
		detail.put("sequence", node.getSequence());
		return detail;
	}

	@Override
	public DictionaryDocument withEntity(PO entity) {
		MTree tree = (MTree) entity;
		return withNode(tree);
	}

	/**
	 * Load all tree nodes in a single SQL query and index them by parent id.
	 * This replaces the previous N+1 pattern (one query per visited parent)
	 * with a single round trip to the database.
	 *
	 * @param treeId the AD_Tree_ID
	 * @return map from parent id (0 = root) to its children, sorted by SeqNo
	 */
	private Map<Integer, List<TreeNodeReference>> loadNodesByParent(int treeId) {
		String tableName = MTree.getNodeTableName(MTree.TREETYPE_Menu);
		final String sql = "SELECT tn.Node_ID, COALESCE(tn.Parent_ID, 0) AS Parent_ID, tn.SeqNo "
			+ "FROM " + tableName + " tn "
			+ "WHERE tn.Node_ID > 0 "
			+ "AND tn.AD_Tree_ID = ? "
			+ "ORDER BY COALESCE(tn.Parent_ID, 0), tn.SeqNo"
		;
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(treeId);
		Map<Integer, List<TreeNodeReference>> nodesByParent = new HashMap<>();
		DB.runResultSet(null, sql, parameters, resulset -> {
			while (resulset.next()) {
				int parentId = resulset.getInt("Parent_ID");
				TreeNodeReference treeNode = TreeNodeReference.newInstance()
					.withNodeId(
						resulset.getInt(
							I_AD_TreeNodeMM.COLUMNNAME_Node_ID
						)
					)
					.withParentId(parentId)
					.withSequence(
						resulset.getInt(
							I_AD_TreeNodeMM.COLUMNNAME_SeqNo
						)
					)
				;
				nodesByParent
					.computeIfAbsent(parentId, k -> new ArrayList<>())
					.add(treeNode);
			}
		}).onFailure(throwable -> {
			throw new AdempiereException(throwable);
		});
		return nodesByParent;
	}

	public MenuTree withNode(MTree tree) {
		// Single query: fetch the whole tree in one round trip.
		Map<Integer, List<TreeNodeReference>> nodesByParent = loadNodesByParent(tree.getAD_Tree_ID());

		Map<String, Object> documentDetail = convertNode(TreeNodeReference.newInstance());
		documentDetail.put("internal_id", tree.getAD_Tree_ID());
		documentDetail.put("id", tree.getUUID());
		documentDetail.put("uuid", tree.getUUID());
		documentDetail.put("name", tree.getName());
		documentDetail.put("children", buildChildren(nodesByParent, 0));
		putDocument(documentDetail);
		return this;
	}

	/**
	 * Build the children list for a given parent id from the pre-loaded map.
	 * Recurses in-memory without touching the database.
	 */
	private List<Map<String, Object>> buildChildren(
		Map<Integer, List<TreeNodeReference>> nodesByParent,
		int parentId
	) {
		List<TreeNodeReference> children = nodesByParent.get(parentId);
		if (children == null || children.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> childrenAsMap = new ArrayList<>(children.size());
		for (TreeNodeReference child : children) {
			Map<String, Object> nodeAsMap = convertNode(child);
			nodeAsMap.put("children", buildChildren(nodesByParent, child.getNodeId()));
			childrenAsMap.add(nodeAsMap);
		}
		return childrenAsMap;
	}

	private MenuTree() {
		super();
	}

	/**
	 * Default instance
	 * @return
	 */
	public static MenuTree newInstance() {
		return new MenuTree();
	}

	@Override
	public String getLanguage() {
		return null;
	}

	@Override
	public String getChannel() {
		return CHANNEL;
	}

}
