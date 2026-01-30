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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_AD_Browse;
import org.adempiere.core.domains.models.I_AD_Form;
import org.adempiere.core.domains.models.I_AD_Menu;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_AD_Workflow;
import org.adempiere.model.MBrowse;
import org.compiere.model.MForm;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.spin.eca56.util.support.DictionaryDocument;

/**
 * 	The document class for Menu sender
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class MenuItem extends DictionaryDocument {

	public static final String CHANNEL = "menu_item";
	public static final String KEY = "new";
	
	@Override
	public String getKey() {
		return KEY;
	}
	
	private Map<String, Object> convertMenu(MMenu menu) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("internal_id", menu.getAD_Menu_ID());
		detail.put("id", menu.getUUID());
		detail.put("uuid", menu.getUUID());
		detail.put("name", menu.get_Translation(I_AD_Menu.COLUMNNAME_Name, getLanguage()));
		detail.put("description", menu.get_Translation(I_AD_Menu.COLUMNNAME_Description, getLanguage()));
		detail.put("is_read_only", menu.isReadOnly());
		detail.put("is_sales_transaction", menu.isSOTrx());
		detail.put("is_summary", menu.isSummary());
		detail.put("action", menu.getAction());
		//	
		if(!Util.isEmpty(menu.getAction())) {
			if(menu.getAction().equals(MMenu.ACTION_Form)) {
				if(menu.getAD_Form_ID() > 0) {
					MForm form = new MForm(menu.getCtx(), menu.getAD_Form_ID(), null);
					Map<String, Object> referenceDetail = new HashMap<>();
					referenceDetail.put("internal_id", form.getAD_Form_ID());
					referenceDetail.put("id", form.getUUID());
					referenceDetail.put("uuid", form.getUUID());
					referenceDetail.put("name", form.get_Translation(I_AD_Form.COLUMNNAME_Name, getLanguage()));
					referenceDetail.put("description", form.get_Translation(I_AD_Form.COLUMNNAME_Description, getLanguage()));
					referenceDetail.put("help", form.get_Translation(I_AD_Form.COLUMNNAME_Help, getLanguage()));
					detail.put("form", referenceDetail);
					detail.put("action_id", form.getAD_Form_ID());
					detail.put("action_uuid", form.getUUID());
				}
			} else if(menu.getAction().equals(MMenu.ACTION_Window)) {
				if(menu.getAD_Window_ID() > 0) {
					MWindow window = new MWindow(menu.getCtx(), menu.getAD_Window_ID(), null);
					Map<String, Object> referenceDetail = new HashMap<>();
					referenceDetail.put("internal_id", window.getAD_Window_ID());
					referenceDetail.put("id", window.getUUID());
					referenceDetail.put("uuid", window.getUUID());
					referenceDetail.put("name", window.get_Translation(I_AD_Window.COLUMNNAME_Name, getLanguage()));
					referenceDetail.put("description", window.get_Translation(I_AD_Window.COLUMNNAME_Description, getLanguage()));
					referenceDetail.put("help", window.get_Translation(I_AD_Window.COLUMNNAME_Help, getLanguage()));
					detail.put("window", referenceDetail);
					detail.put("action_id", window.getAD_Window_ID());
					detail.put("action_uuid", window.getUUID());
				}
			} else if(menu.getAction().equals(MMenu.ACTION_Process)
				|| menu.getAction().equals(MMenu.ACTION_Report)) {
				if(menu.getAD_Process_ID() > 0) {
					MProcess process = MProcess.get(menu.getCtx(), menu.getAD_Process_ID());
					Map<String, Object> referenceDetail = new HashMap<>();
					referenceDetail.put("internal_id", process.getAD_Process_ID());
					referenceDetail.put("id", process.getUUID());
					referenceDetail.put("uuid", process.getUUID());
					referenceDetail.put("name", process.get_Translation(I_AD_Process.COLUMNNAME_Name, getLanguage()));
					referenceDetail.put("description", process.get_Translation(I_AD_Process.COLUMNNAME_Description, getLanguage()));
					referenceDetail.put("help", process.get_Translation(I_AD_Process.COLUMNNAME_Help, getLanguage()));
					detail.put("process", referenceDetail);
					detail.put("action_id", process.getAD_Process_ID());
					detail.put("action_uuid", process.getUUID());
				}
			} else if(menu.getAction().equals(MMenu.ACTION_SmartBrowse)) {
				if(menu.getAD_Browse_ID() > 0) {
					MBrowse smartBrowser = MBrowse.get(menu.getCtx(), menu.getAD_Browse_ID());
					Map<String, Object> referenceDetail = new HashMap<>();
					referenceDetail.put("internal_id", smartBrowser.getAD_Browse_ID());
					referenceDetail.put("id", smartBrowser.getUUID());
					referenceDetail.put("uuid", smartBrowser.getUUID());
					referenceDetail.put("name", smartBrowser.get_Translation(I_AD_Browse.COLUMNNAME_Name, getLanguage()));
					referenceDetail.put("description", smartBrowser.get_Translation(I_AD_Browse.COLUMNNAME_Description, getLanguage()));
					referenceDetail.put("help", smartBrowser.get_Translation(I_AD_Browse.COLUMNNAME_Help, getLanguage()));
					detail.put("browser", referenceDetail);
					detail.put("action_id", smartBrowser.getAD_Browse_ID());
					detail.put("action_uuid", smartBrowser.getUUID());
				}
			} else if(menu.getAction().equals(MMenu.ACTION_WorkFlow)) {
				if(menu.getAD_Workflow_ID() > 0) {
					MWorkflow workflow = MWorkflow.get(menu.getCtx(), menu.getAD_Workflow_ID());
					Map<String, Object> referenceDetail = new HashMap<>();
					referenceDetail.put("internal_id", workflow.getAD_Workflow_ID());
					referenceDetail.put("id", workflow.getUUID());
					referenceDetail.put("uuid", workflow.getUUID());
					referenceDetail.put("name", workflow.get_Translation(I_AD_Workflow.COLUMNNAME_Name, getLanguage()));
					referenceDetail.put("description", workflow.get_Translation(I_AD_Workflow.COLUMNNAME_Description, getLanguage()));
					referenceDetail.put("help", workflow.get_Translation(I_AD_Workflow.COLUMNNAME_Help, getLanguage()));
					detail.put("workflow", referenceDetail);
					detail.put("action_id", workflow.getAD_Workflow_ID());
					detail.put("action_uuid", workflow.getUUID());
				}
			}
		}

		// new UI
		if (menu.get_ColumnIndex("WebPath") >= 0 && !Util.isEmpty(menu.get_ValueAsString("WebPath"))) {
			final String targetPath = getTargetPath(menu);
			detail.put("target_path", targetPath);
			detail.put("web_path", menu.get_ValueAsString("WebPath"));
		}
		if (menu.get_ColumnIndex("AD_Module_ID") >= 0 && menu.get_ValueAsInt("AD_Module_ID") > 0) {
			detail.put("module_id", menu.get_ValueAsInt("AD_Module_ID"));
		}
		if (menu.get_ColumnIndex("AD_SubModule_ID") >= 0 && menu.get_ValueAsInt("AD_SubModule_ID") > 0) {
			detail.put("sub_module_id", menu.get_ValueAsInt("AD_SubModule_ID"));
		}

		return detail;
	}

	private String getTargetPath(MMenu menu) {
		final String webPath = menu.get_ValueAsString("WebPath");
		if (Util.isEmpty(webPath, true) || !webPath.contains("@")) {
			return webPath;
		}
		Properties context = Env.getCtx();
		final int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);

		final int menuId = menu.getAD_Menu_ID();
		final int windowId = menu.getAD_Window_ID();
		final int processId = menu.getAD_Process_ID();
		final int browserId = menu.getAD_Browse_ID();
		final int workflowId = menu.getAD_Workflow_ID();
		final int formId = menu.getAD_Form_ID();
		final int moduleId = menu.get_ValueAsInt("AD_Module_ID");
		final int subModuleId = menu.get_ValueAsInt("AD_SubModule_ID");

		Env.setContext(context, windowNo, "AD_Menu_ID", menuId);
		Env.setContext(context, windowNo, "AD_Window_ID", windowId);
		Env.setContext(context, windowNo, "AD_Process_ID", processId);
		Env.setContext(context, windowNo, "AD_Browse_ID", browserId);
		Env.setContext(context, windowNo, "AD_Workflow_ID", workflowId);
		Env.setContext(context, windowNo, "AD_Form_ID", formId);
		Env.setContext(context, windowNo, "AD_Module_ID", moduleId);
		Env.setContext(context, windowNo, "AD_SubModule_ID", subModuleId);

		final String targetParh = Env.parseContext(context, windowNo, webPath, false);
		return targetParh;
	}

	@Override
	public DictionaryDocument withEntity(PO entity) {
		MMenu menu = (MMenu) entity;
		Map<String, Object> documentDetail = convertMenu(menu);
		putDocument(documentDetail);
		return this;
	}
	
	private MenuItem() {
		super();
	}
	
	/**
	 * Default instance
	 * @return
	 */
	public static MenuItem newInstance() {
		return new MenuItem();
	}

	@Override
	public String getChannel() {
		return CHANNEL;
	}
}
