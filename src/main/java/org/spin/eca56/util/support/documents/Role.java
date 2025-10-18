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
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Browse;
import org.adempiere.core.domains.models.I_AD_Form;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_AD_Workflow;
import org.adempiere.model.MBrowse;
// import org.adempiere.core.domains.models.I_PA_DashboardContent;
import org.compiere.model.MClientInfo;
// import org.compiere.model.MDashboardContent;
import org.compiere.model.MForm;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MTree;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.wf.MWorkflow;
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
					+ "ORDER BY tr.AD_Client_ID DESC, tr.IsDefault DESC, tr.AD_Tree_ID"
				;
				//	Get Tree
				treeId = DB.getSQLValue(null, sql, role.getAD_Client_ID());
			}
		}
		MTree tree = MTree.get(role.getCtx(), treeId, null);
		detail.put("tree_id", treeId);
		detail.put("tree_uuid", tree.getUUID());

		detail.put("window_access", getWindowAccess(role));
		detail.put("process_access", getProcessAccess(role));
		detail.put("form_access", getFormAccess(role));
		detail.put("browser_access", getBrowserAccess(role));
		detail.put("workflow_access", getWorkflowAccess(role));
		detail.put("dashboard_access", getDashboardAccess(role));
		return detail;
	}

	private List<String> getWindowAccess(MRole role) {
		return new Query(
			role.getCtx(),
			I_AD_Window.Table_Name,
			"EXISTS(SELECT 1 FROM AD_Window_Access AS wa WHERE wa.IsActive = 'Y' AND wa.AD_Window_ID = AD_Window.AD_Window_ID AND wa.AD_Role_ID = ?)",
			null
		)
			.setParameters(role.getAD_Role_ID())
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.stream()
			.map(windowId -> {
				MWindow window = MWindow.get(role.getCtx(), windowId);
				return window.getUUID();
			})
			.collect(Collectors.toList())
		;
	}

	private List<String> getProcessAccess(MRole role) {
		return new Query(
			role.getCtx(),
			I_AD_Process.Table_Name,
			"EXISTS(SELECT 1 FROM AD_Process_Access AS pa WHERE pa.IsActive = 'Y' AND pa.AD_Process_ID = AD_Process.AD_Process_ID AND pa.AD_Role_ID = ?)",
			null
		)
			.setParameters(role.getAD_Role_ID())
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.stream()
			.map(processId -> {
				MProcess process = MProcess.get(role.getCtx(), processId);
				return process.getUUID();
			})
			.collect(Collectors.toList())
		;
	}

	private List<String> getFormAccess(MRole role) {
		return new Query(
			role.getCtx(),
			I_AD_Form.Table_Name,
			"EXISTS(SELECT 1 FROM AD_Form_Access AS fa WHERE fa.IsActive = 'Y' AND fa.AD_Form_ID = AD_Form.AD_Form_ID AND fa.AD_Role_ID = ?)",
			null
		)
			.setParameters(role.getAD_Role_ID())
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.stream()
			.map(formId -> {
				MForm form = new MForm(role.getCtx(), formId, null);
				return form.getUUID();
			})
			.collect(Collectors.toList())
		;
	}

	private List<String> getBrowserAccess(MRole role) {
		return new Query(
			role.getCtx(),
			I_AD_Browse.Table_Name,
			"EXISTS(SELECT 1 FROM AD_Browse_Access AS ba WHERE ba.IsActive = 'Y' AND ba.AD_Browse_ID = AD_Browse.AD_Browse_ID AND ba.AD_Role_ID = ?)",
			null
		)
			.setParameters(role.getAD_Role_ID())
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.stream()
			.map(browserId -> {
				MBrowse browse = MBrowse.get(role.getCtx(), browserId);
				return browse.getUUID();
			})
			.collect(Collectors.toList())
		;
	}

	private List<String> getWorkflowAccess(MRole role) {
		return new Query(
			role.getCtx(),
			I_AD_Workflow.Table_Name,
			"EXISTS(SELECT 1 FROM AD_Workflow_Access AS wa WHERE wa.IsActive = 'Y' AND wa.AD_Workflow_ID = AD_Workflow.AD_Workflow_ID AND wa.AD_Role_ID = ?)",
			null
		)
			.setParameters(role.getAD_Role_ID())
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.stream()
			.map(workflowId -> {
				MWorkflow workflow = MWorkflow.get(role.getCtx(), workflowId);
				return workflow.getUUID();
			})
			.collect(Collectors.toList())
		;
	}

	private List<String> getDashboardAccess(MRole role) {
		// return new Query(
		// 	role.getCtx(),
		// 	I_PA_DashboardContent.Table_Name,
		// 	"EXISTS(SELECT 1 FROM AD_Dashboard_Access AS da WHERE da.IsActive = 'Y' AND da.PA_DashboardContent_ID = PA_DashboardContent.PA_DashboardContent_ID AND da.AD_Role_ID = ?)",
		// 	null
		// )
		// 	.setParameters(role.getAD_Role_ID())
		// 	.setOnlyActiveRecords(true)
		// 	.getIDsAsList()
		// 	.stream()
		// 	.map(dashboardId -> {
		// 		MDashboardContent dashboard = new MDashboardContent(role.getCtx(), dashboardId, null);
		// 		return dashboard.getUUID();
		// 	})
		// 	.collect(Collectors.toList())
		// ;
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
