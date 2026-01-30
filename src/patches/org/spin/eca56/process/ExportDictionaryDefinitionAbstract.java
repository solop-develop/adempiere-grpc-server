/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.spin.eca56.process;

import org.compiere.process.SvrProcess;

/** Generated Process for (Export Dictionary Definition)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class ExportDictionaryDefinitionAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "ECA56_ExportDictionaryDefinition";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Export Dictionary Definition";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54692;
	/**	Parameter Name for Export Menu	*/
	public static final String ECA56_EXPORTMENU = "ECA56_ExportMenu";
	/**	Parameter Name for Menu	*/
	public static final String AD_MENU_ID = "AD_Menu_ID";
	/**	Parameter Name for Export Windows	*/
	public static final String ECA56_EXPORTWINDOWS = "ECA56_ExportWindows";
	/**	Parameter Name for Window	*/
	public static final String AD_WINDOW_ID = "AD_Window_ID";
	/**	Parameter Name for Export Process	*/
	public static final String ECA56_EXPORTPROCESS = "ECA56_ExportProcess";
	/**	Parameter Name for Process	*/
	public static final String AD_PROCESS_ID = "AD_Process_ID";
	/**	Parameter Name for Export Browsers	*/
	public static final String ECA56_EXPORTBROWSERS = "ECA56_ExportBrowsers";
	/**	Parameter Name for Smart Browse	*/
	public static final String AD_BROWSE_ID = "AD_Browse_ID";
	/**	Parameter Name for Export Forms	*/
	public static final String ECA56_EXPORTFORMS = "ECA56_ExportForms";
	/**	Parameter Name for Special Form	*/
	public static final String AD_FORM_ID = "AD_Form_ID";
	/**	Parameter Name for Export Roles	*/
	public static final String ECA56_EXPORTROLES = "ECA56_ExportRoles";
	/**	Parameter Name for Role	*/
	public static final String AD_ROLE_ID = "AD_Role_ID";
	/**	Parameter Name for Export Tree	*/
	public static final String ECA56_EXPORTTREE = "ECA56_ExportTree";
	/**	Parameter Name for Tree	*/
	public static final String AD_TREE_ID = "AD_Tree_ID";
	/**	Parameter Name for Filter By Tree	*/
	public static final String ECA56_ISFILTERBYTREE = "ECA56_IsFilterByTree";
	/**	Parameter Value for Export Menu	*/
	private boolean isExportMenu;
	/**	Parameter Value for Menu	*/
	private int menuId;
	/**	Parameter Value for Export Windows	*/
	private boolean isExportWindows;
	/**	Parameter Value for Window	*/
	private int windowId;
	/**	Parameter Value for Export Process	*/
	private boolean isExportProcess;
	/**	Parameter Value for Process	*/
	private int aDProcessId;
	/**	Parameter Value for Export Browsers	*/
	private boolean isExportBrowsers;
	/**	Parameter Value for Smart Browse	*/
	private int browseId;
	/**	Parameter Value for Export Forms	*/
	private boolean isExportForms;
	/**	Parameter Value for Special Form	*/
	private int formId;
	/**	Parameter Value for Export Roles	*/
	private boolean isExportRoles;
	/**	Parameter Value for Role	*/
	private int roleId;
	/**	Parameter Value for Export Tree	*/
	private boolean isExportTree;
	/**	Parameter Value for Tree	*/
	private int treeId;
	/**	Parameter Value for Filter By Tree	*/
	private boolean isFilterByTree;

	@Override
	protected void prepare() {
		isExportMenu = getParameterAsBoolean(ECA56_EXPORTMENU);
		menuId = getParameterAsInt(AD_MENU_ID);
		isExportWindows = getParameterAsBoolean(ECA56_EXPORTWINDOWS);
		windowId = getParameterAsInt(AD_WINDOW_ID);
		isExportProcess = getParameterAsBoolean(ECA56_EXPORTPROCESS);
		aDProcessId = getParameterAsInt(AD_PROCESS_ID);
		isExportBrowsers = getParameterAsBoolean(ECA56_EXPORTBROWSERS);
		browseId = getParameterAsInt(AD_BROWSE_ID);
		isExportForms = getParameterAsBoolean(ECA56_EXPORTFORMS);
		formId = getParameterAsInt(AD_FORM_ID);
		isExportRoles = getParameterAsBoolean(ECA56_EXPORTROLES);
		roleId = getParameterAsInt(AD_ROLE_ID);
		isExportTree = getParameterAsBoolean(ECA56_EXPORTTREE);
		treeId = getParameterAsInt(AD_TREE_ID);
		isFilterByTree = getParameterAsBoolean(ECA56_ISFILTERBYTREE);
	}

	/**	 Getter Parameter Value for Export Menu	*/
	protected boolean isExportMenu() {
		return isExportMenu;
	}

	/**	 Setter Parameter Value for Export Menu	*/
	protected void setExportMenu(boolean isExportMenu) {
		this.isExportMenu = isExportMenu;
	}

	/**	 Getter Parameter Value for Menu	*/
	protected int getMenuId() {
		return menuId;
	}

	/**	 Setter Parameter Value for Menu	*/
	protected void setMenuId(int menuId) {
		this.menuId = menuId;
	}

	/**	 Getter Parameter Value for Export Windows	*/
	protected boolean isExportWindows() {
		return isExportWindows;
	}

	/**	 Setter Parameter Value for Export Windows	*/
	protected void setExportWindows(boolean isExportWindows) {
		this.isExportWindows = isExportWindows;
	}

	/**	 Getter Parameter Value for Window	*/
	protected int getWindowId() {
		return windowId;
	}

	/**	 Setter Parameter Value for Window	*/
	protected void setWindowId(int windowId) {
		this.windowId = windowId;
	}

	/**	 Getter Parameter Value for Export Process	*/
	protected boolean isExportProcess() {
		return isExportProcess;
	}

	/**	 Setter Parameter Value for Export Process	*/
	protected void setExportProcess(boolean isExportProcess) {
		this.isExportProcess = isExportProcess;
	}

	/**	 Getter Parameter Value for Process	*/
	protected int getADProcessId() {
		return aDProcessId;
	}

	/**	 Setter Parameter Value for Process	*/
	protected void setADProcessId(int aDProcessId) {
		this.aDProcessId = aDProcessId;
	}

	/**	 Getter Parameter Value for Export Browsers	*/
	protected boolean isExportBrowsers() {
		return isExportBrowsers;
	}

	/**	 Setter Parameter Value for Export Browsers	*/
	protected void setExportBrowsers(boolean isExportBrowsers) {
		this.isExportBrowsers = isExportBrowsers;
	}

	/**	 Getter Parameter Value for Smart Browse	*/
	protected int getBrowseId() {
		return browseId;
	}

	/**	 Setter Parameter Value for Smart Browse	*/
	protected void setBrowseId(int browseId) {
		this.browseId = browseId;
	}

	/**	 Getter Parameter Value for Export Forms	*/
	protected boolean isExportForms() {
		return isExportForms;
	}

	/**	 Setter Parameter Value for Export Forms	*/
	protected void setExportForms(boolean isExportForms) {
		this.isExportForms = isExportForms;
	}

	/**	 Getter Parameter Value for Special Form	*/
	protected int getFormId() {
		return formId;
	}

	/**	 Setter Parameter Value for Special Form	*/
	protected void setFormId(int formId) {
		this.formId = formId;
	}

	/**	 Getter Parameter Value for Export Roles	*/
	protected boolean isExportRoles() {
		return isExportRoles;
	}

	/**	 Setter Parameter Value for Export Roles	*/
	protected void setExportRoles(boolean isExportRoles) {
		this.isExportRoles = isExportRoles;
	}

	/**	 Getter Parameter Value for Role	*/
	protected int getRoleId() {
		return roleId;
	}

	/**	 Setter Parameter Value for Role	*/
	protected void setRoleId(int roleId) {
		this.roleId = roleId;
	}

	/**	 Getter Parameter Value for Export Tree	*/
	protected boolean isExportTree() {
		return isExportTree;
	}

	/**	 Setter Parameter Value for Export Tree	*/
	protected void setExportTree(boolean isExportTree) {
		this.isExportTree = isExportTree;
	}

	/**	 Getter Parameter Value for Tree	*/
	protected int getTreeId() {
		return treeId;
	}

	/**	 Setter Parameter Value for Tree	*/
	protected void setTreeId(int treeId) {
		this.treeId = treeId;
	}

	/**	 Getter Parameter Value for Filter By Tree	*/
	protected boolean isFilterByTree() {
		return isFilterByTree;
	}

	/**	 Setter Parameter Value for Filter By Tree	*/
	protected void setIsFilterByTree(boolean isFilterByTree) {
		this.isFilterByTree = isFilterByTree;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}