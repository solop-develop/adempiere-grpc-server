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

import org.adempiere.exceptions.AdempiereException;
// import org.adempiere.core.domains.models.I_PA_DashboardContent;
import org.compiere.model.MClientInfo;
// import org.compiere.model.MDashboardContent;
import org.compiere.model.MRole;
import org.compiere.model.MTree;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.spin.eca56.util.support.DictionaryDocument;

/**
 * 	The document class for Role sender
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class Role extends DictionaryDocument {

	public static final String CHANNEL = "role";
	public static final String KEY = "new";

	@Override
	public String getKey() {
		return KEY;
	}

	private Map<String, Object> convertRole(MRole role) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("internal_id", role.getAD_Role_ID());
		detail.put("id", role.getUUID());
		detail.put("uuid", role.getUUID());
		detail.put("name", role.getName());

		detail.put("description", role.getDescription());
		int treeId = role.getAD_Tree_Menu_ID();
		if(treeId <= 0) {
			MClientInfo clientInfo = MClientInfo.get(role.getCtx());
			treeId = clientInfo.getAD_Tree_Menu_ID();
			if(treeId <= 0) {
				String sql = "SELECT tr.AD_Tree_ID "
					+ "FROM AD_Tree AS tr "
					+ "WHERE tr.IsActive = 'Y' "
					+ "AND tr.AD_Client_ID IN(0, ?) "
					+ "AND tr.TreeType = 'MM' "
					+ "AND tr.IsAllNodes = 'Y' "
					+ "AND ROWNUM = 1 "
					+ "ORDER BY tr.AD_Client_ID DESC, tr.IsDefault DESC, tr.AD_Tree_ID "
				;
				//	Get Tree
				treeId = DB.getSQLValue(null, sql, role.getAD_Client_ID());
				// treeId = MTree.getDefaultTreeIdFromTableId(clientId, I_AD_Menu.Table_ID);
			}
		}
		if (treeId > 0) {
			MTree tree = MTree.get(role.getCtx(), treeId, null);
			if (tree != null) {
				detail.put("tree_id", treeId);
				detail.put("tree_uuid", tree.getUUID());
			}
		}

		detail.put("window_access", getWindowAccess(role));
		detail.put("process_access", getProcessAccess(role));
		detail.put("form_access", getFormAccess(role));
		detail.put("browser_access", getBrowserAccess(role));
		detail.put("workflow_access", getWorkflowAccess(role));
		detail.put("dashboard_access", getDashboardAccess(role));
		return detail;
	}

	/**
	 * Fetch a list of UUIDs for a role in a single SQL round trip.
	 *
	 * The previous implementation was an N+1 query pattern: one SELECT to get
	 * the entity IDs plus one PO lookup per entity just to read the UUID. For a
	 * role with access to hundreds of entities across six access types, that
	 * meant thousands of lookups per role export. Joining the access table with
	 * the entity table and selecting only the UUID column collapses each access
	 * type to a single query, regardless of how many rows it returns.
	 */
	private List<String> getAccessUuids(int roleId, String sql) {
		List<Object> parameters = new ArrayList<>();
		parameters.add(roleId);
		List<String> uuids = new ArrayList<>();
		DB.runResultSet(null, sql, parameters, resultSet -> {
			while (resultSet.next()) {
				uuids.add(resultSet.getString(1));
			}
		}).onFailure(throwable -> {
			throw new AdempiereException(throwable);
		});
		return uuids;
	}

	private List<String> getWindowAccess(MRole role) {
		final String sql = "SELECT w.UUID "
			+ "FROM AD_Window w "
			+ "INNER JOIN AD_Window_Access wa ON wa.AD_Window_ID = w.AD_Window_ID "
			+ "WHERE w.IsActive = 'Y' "
			+ "AND wa.IsActive = 'Y' "
			+ "AND wa.AD_Role_ID = ? "
		;
		return getAccessUuids(role.getAD_Role_ID(), sql);
	}

	private List<String> getProcessAccess(MRole role) {
		final String sql = "SELECT p.UUID "
			+ "FROM AD_Process p "
			+ "INNER JOIN AD_Process_Access pa ON pa.AD_Process_ID = p.AD_Process_ID "
			+ "WHERE p.IsActive = 'Y' "
			+ "AND pa.IsActive = 'Y' "
			+ "AND pa.AD_Role_ID = ? "
		;
		return getAccessUuids(role.getAD_Role_ID(), sql);
	}

	private List<String> getFormAccess(MRole role) {
		final String sql = "SELECT f.UUID "
			+ "FROM AD_Form f "
			+ "INNER JOIN AD_Form_Access fa ON fa.AD_Form_ID = f.AD_Form_ID "
			+ "WHERE f.IsActive = 'Y' "
			+ "AND fa.IsActive = 'Y' "
			+ "AND fa.AD_Role_ID = ? "
		;
		return getAccessUuids(role.getAD_Role_ID(), sql);
	}

	private List<String> getBrowserAccess(MRole role) {
		final String sql = "SELECT b.UUID "
			+ "FROM AD_Browse b "
			+ "INNER JOIN AD_Browse_Access ba ON ba.AD_Browse_ID = b.AD_Browse_ID "
			+ "WHERE b.IsActive = 'Y' "
			+ "AND ba.IsActive = 'Y' "
			+ "AND ba.AD_Role_ID = ?"
		;
		return getAccessUuids(role.getAD_Role_ID(), sql);
	}

	private List<String> getWorkflowAccess(MRole role) {
		final String sql = "SELECT w.UUID "
			+ "FROM AD_Workflow w "
			+ "INNER JOIN AD_Workflow_Access wa ON wa.AD_Workflow_ID = w.AD_Workflow_ID "
			+ "WHERE w.IsActive = 'Y' "
			+ "AND wa.IsActive = 'Y' "
			+ "AND wa.AD_Role_ID = ? "
		;
		return getAccessUuids(role.getAD_Role_ID(), sql);
	}

	private List<String> getDashboardAccess(MRole role) {
		// final String sql = "SELECT d.UUID "
		// 	+ "FROM PA_DashboardContent d "
		// 	+ "INNER JOIN AD_Dashboard_Access da ON da.PA_DashboardContent_ID = d.PA_DashboardContent_ID "
		// 	+ "WHERE d.IsActive = 'Y' "
		// 	+ "AND da.IsActive = 'Y' "
		// 	+ "AND da.AD_Role_ID = ? "
		// ;
		// return getAccessUuids(role.getAD_Role_ID(), sql);
		return new ArrayList<>();
	}

	@Override
	public DictionaryDocument withEntity(PO entity) {
		MRole role = (MRole) entity;
		Map<String, Object> documentDetail = convertRole(role);
		putDocument(documentDetail);
		return this;
	}

	private Role() {
		super();
	}

	/**
	 * Default instance
	 * @return
	 */
	public static Role newInstance() {
		return new Role();
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
