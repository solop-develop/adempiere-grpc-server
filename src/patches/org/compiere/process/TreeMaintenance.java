/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MElementValue;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTree_Node;
import org.compiere.model.MTree_NodeBP;
import org.compiere.model.MTree_NodeMM;
import org.compiere.model.MTree_NodePR;
import org.compiere.model.MTree_NodeU1;
import org.compiere.model.MTree_NodeU2;
import org.compiere.model.MTree_NodeU3;
import org.compiere.model.MTree_NodeU4;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 *	Tree Maintenance	
 *	
 *  @author Jorg Janke
 *  @version $Id: TreeMaintenance.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 *  @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 2015-09-09
 *  	<li>FR [ 9223372036854775807 ] Add Support to Dynamic Tree
 *  @see https://adempiere.atlassian.net/browse/ADEMPIERE-442
 *  @author Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 *  		<a href="https://github.com/adempiere/adempiere/issues/729">
 *			@see FR [ 729 ] Add Support to Parent Column And Search Column for Tree </a>
 */
public class TreeMaintenance extends SvrProcess
{
	/**	Tree				*/
	private int		m_AD_Tree_ID;

	private Set<Integer> orderedNodeIds;
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
		m_AD_Tree_ID = getRecord_ID();		//	from Window
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		log.info("AD_Tree_ID=" + m_AD_Tree_ID);
		if (m_AD_Tree_ID == 0)
			throw new IllegalArgumentException("Tree_ID = 0");
		MTree tree = new MTree (getCtx(), m_AD_Tree_ID, get_TrxName());	
		if (tree.getAD_Tree_ID() == 0)
			throw new IllegalArgumentException("No Tree -" + tree);
		//
		if (MTree.TREETYPE_BoM.equals(tree.getTreeType()))
			return "BOM Trees not implemented";
		orderedNodeIds = new HashSet<>();
		return verifyTree(tree);
	}	//	doIt

	/**
	 *  Verify Tree
	 * 	@param tree tree
	 */
	private String verifyTree (MTree tree) {
		String nodeTableName = tree.getNodeTableName();
		String sourceTableName = tree.getSourceTableName();
		String sourceTableKey = sourceTableName + "_ID";
		int AD_Client_ID = tree.getAD_Client_ID();
		
		List<Integer> treeElements = new ArrayList<Integer>();
		
		
		if (MTree.TREETYPE_ElementValue.equals(tree.getTreeType())
		||	MTree.TREETYPE_User1.equals(tree.getTreeType())
		||	MTree.TREETYPE_User2.equals(tree.getTreeType())
		||	MTree.TREETYPE_User3.equals(tree.getTreeType())
		||	MTree.TREETYPE_User4.equals(tree.getTreeType()))
		{
			String sql = "SELECT C_Element_ID FROM C_Element "
				+ "WHERE AD_Tree_ID= ?"  ;
			
			int[] elements = DB.getIDsEx(null, sql, tree.getAD_Tree_ID());
			if (elements.length <= 0) {
				throw new AdempiereException("@C_Element_ID@ @NotFound@");
			}
			for (int i : elements) {
				treeElements.add(i);
			}
		} else {
			treeElements.add(0);
		}
		
		AtomicReference<Boolean> ok = new AtomicReference<>();
		ok.set(true);
		
        treeElements.forEach(treeElement -> {
			AtomicReference<String> parentColumnName = new AtomicReference<>();
			AtomicReference<String> sortColumnName = new AtomicReference<>();

			MTable sourceTable	 = null;
			if (tree.getParent_Column_ID() > 0) {
				parentColumnName.set(MColumn.getColumnName(Env.getCtx(), tree.getParent_Column_ID()));
				sourceTable = MTable.get(Env.getCtx(),tree.getAD_Table_ID());
			}
			if (tree.getAD_ColumnSortOrder_ID() > 0) {
				sortColumnName.set(MColumn.getColumnName(Env.getCtx(), tree.getAD_ColumnSortOrder_ID()));
			}




			int C_Element_ID = treeElement;
			StringBuffer sql = new StringBuffer();
			sql.append("DELETE ").append(nodeTableName)
					.append(" WHERE AD_Tree_ID=").append(tree.getAD_Tree_ID())
					.append(" AND Node_ID NOT IN (SELECT ").append(sourceTableKey)
					.append(" FROM ").append(sourceTableName)
					.append(" st WHERE st.AD_Client_ID=").append(AD_Client_ID);
			if (C_Element_ID > 0)
				sql.append(" AND EXISTS (SELECT 1 FROM C_Element WHERE ")
						.append(" C_Element_ID=").append(C_Element_ID)
						.append(" AND C_Element.AD_Tree_ID = ").append(nodeTableName).append(".AD_Tree_ID)");
			sql.append(")");
			log.finer(sql.toString());
			//
			int deletes = DB.executeUpdate(sql.toString(), get_TrxName());
			addLog(0,null, new BigDecimal(deletes), tree.getName()+ " Deleted");
			String whereClause = "1=1";
			List<Object> parameters = new ArrayList<>();
			if (C_Element_ID > 0) {
				whereClause = " C_Element_ID = ?";
				parameters.add(C_Element_ID);
			}
			List<Integer> treeElementIds = new Query(getCtx(), sourceTableName, whereClause, get_TrxName())
					.setParameters(parameters)
					.setOnlyActiveRecords(true)
					.getIDsAsList();

			whereClause = "C_Element_ID = " + C_Element_ID;
			MTable treeTable = MTable.get(getCtx(), nodeTableName);
			for (Integer treeElementId : treeElementIds) {

				PO treeNode = treeTable.getPO("AD_Tree_ID = " + tree.get_ID() + " AND Node_ID = " + treeElementId, get_TrxName());
				if (treeNode == null) {
					treeNode = treeTable.getPO(0, get_TrxName());
					treeNode.setAD_Org_ID(0);
					treeNode.set_CustomColumn("AD_Tree_ID", tree.get_ID());
					treeNode.set_CustomColumn("Node_ID", treeElementId);
				}
				treeNode.setIsActive(true);

				PO element = sourceTable.getPO(treeElementId, get_TrxName());
				String sortColumnValue = element.get_ValueAsString(sortColumnName.get());
				if (!Util.isEmpty(sortColumnName.get(), true) && C_Element_ID > 0) {
					int parentId = getParentFromSort(sortColumnName.get(), sortColumnValue, whereClause, sourceTableName);
					if (parentId <= 0) {
						treeNode.set_CustomColumn("Parent_ID", null);
					} else {
						treeNode.set_CustomColumn("Parent_ID", parentId);
					}
					if (!Util.isEmpty(parentColumnName.get(), true)) {
						if (parentId <= 0) {
							element.set_ValueOfColumn(parentColumnName.get(), null);
						}else {
							element.set_ValueOfColumn(parentColumnName.get(), parentId);
						}

					}
				} else if (element.get_ValueAsInt(parentColumnName.get()) > 0) {
					treeNode.set_CustomColumn("Parent_ID", element.get_ValueAsInt(parentColumnName.get()));
				}
				element.saveEx();


				treeNode.setIsDirectLoad(true);
				treeNode.set_CustomColumn("SeqNo", 999);
				treeNode.saveEx();
			}

			whereClause = "AD_Tree_ID = ?";
			List<PO> nodeList = new Query(getCtx(), nodeTableName, whereClause, get_TrxName())
				.setParameters(tree.get_ID())
				.list();
			for (PO node : nodeList) {
				updateTreeNodeOrder(nodeTableName,sortColumnName.get(),tree.get_ID(), node.get_ValueAsInt("Parent_ID"),parentColumnName.get(), sourceTableName, C_Element_ID);
			}

		});
		return tree.getName() + (ok.get() ? " OK" : " Error");
	}	//	verifyTree

	private void updateTreeNodeOrder (String treeTableName, String sortColumnName, int treeId, int parentId, String parentColumnName, String tableName, int elementId) {
		List<Object> parameters = new ArrayList<>();

		String whereClause = "AD_Client_ID = ?";
		parameters.add(getAD_Client_ID());
		if (parentId >0) {
			whereClause += "AND " + parentColumnName + " = ? ";
			parameters.add(parentId);

		} else {
			whereClause += " AND (" + parentColumnName + " IS NULL OR "+ parentColumnName +" = 0)";
		}
		if (elementId > 0) {
			whereClause += " AND C_Element_ID = ? ";
			parameters.add(elementId);
		}

		List<Integer> siblingIds = new Query(getCtx(), tableName, whereClause, get_TrxName())
				.setParameters(parameters)
				.setOnlyActiveRecords(true)
				.setOrderBy(sortColumnName)
				.getIDsAsList();
		int index=0;
		for (Integer siblingId : siblingIds) {
			index++;
			if (orderedNodeIds.contains(siblingId) ) {
				continue;
			}

			PO treeNode = MTable.get(getCtx(), treeTableName).getPO("Node_ID = " + siblingId + " AND AD_Tree_ID = " + treeId , get_TrxName());
			if(treeNode != null) {
				treeNode.set_ValueOfColumn("SeqNo", index);
				treeNode.setIsDirectLoad(true);
				treeNode.saveEx();
				orderedNodeIds.add(siblingId);
			}
		}

	}


	/**
	 * FR [ 729 ]
	 * Get Tree Parent from Sort
	 * @param sortColumn
	 * @param sortValue
	 * @return
	 */
	private int getParentFromSort(String sortColumn ,String sortValue, String whereClause, String tableName) {
		Integer parentID = -1 ;
		whereClause = Optional.ofNullable(whereClause + " AND ").orElse("");

		if (sortValue!=null) {
			List<PO> parentPO = new Query(getCtx(), tableName, whereClause + " IsSummary = 'Y' ", get_TrxName()).setOrderBy(sortColumn).list();
			HashMap<String,Integer> currentValues = new HashMap<String,Integer>();

			for (PO po : parentPO)
				currentValues.put(po.get_ValueAsString(sortColumn), po.get_ID());

			while (!sortValue.isEmpty()) {
				sortValue = sortValue.substring(0, sortValue.length()-1);
				parentID = currentValues.get(sortValue);
				if (parentID==null)
					parentID = 0;
				else
					break;

			}
		}
		return parentID;
	}


	private static PO getPO(MTree tree, String nodeTableName, int nodeId) {
		PO node = null;
		if (nodeTableName.equals("AD_TreeNode")) {
			node = new MTree_Node(tree, nodeId);
		} else if (nodeTableName.equals("AD_TreeNodeBP")) {
			node = new MTree_NodeBP(tree, nodeId);
		} else if (nodeTableName.equals("AD_TreeNodePR")) {
			node = new MTree_NodePR(tree, nodeId);
		} else if (nodeTableName.equals("AD_TreeNodeMM")) {
			node = new MTree_NodeMM(tree, nodeId);
		} else if (nodeTableName.equals("AD_TreeNodeU1")) {
			node = new MTree_NodeU1(tree, nodeId);
		} else if (nodeTableName.equals("AD_TreeNodeU2")) {
			node = new MTree_NodeU2(tree, nodeId);
		} else if (nodeTableName.equals("AD_TreeNodeU3")) {
			node = new MTree_NodeU3(tree, nodeId);
		} else if (nodeTableName.equals("AD_TreeNodeU4")) {
			node = new MTree_NodeU4(tree, nodeId);
		}
		return node;
	}

}	//	TreeMaintenence
