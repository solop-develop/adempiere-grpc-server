/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2024 ADempiere Foundation, All Rights Reserved.         *
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

import org.adempiere.core.domains.models.I_AD_Browse;
import org.adempiere.core.domains.models.I_AD_Form;
import org.adempiere.core.domains.models.I_AD_Menu;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Tree;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.compiere.model.MForm;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MTree;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.spin.eca56.util.queue.ApplicationDictionary;
import org.spin.queue.util.QueueLoader;

/** Generated Process for (Export Current Dictionary Definition)
 *  @author Edwin Betancourt
 *  @version Release 3.9.4
 */
public class ExportCurrentDictionaryDefinition extends ExportCurrentDictionaryDefinitionAbstract
{

	@Override
	protected void prepare()
	{
		super.prepare();

		// Valid Record Identifier
		if (this.getRecord_ID() <= 0) {
			throw new AdempiereException("@FillMandatory@ @Record_ID@");
		}

		if(this.getTable_ID() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Table@");
		}
	}

	@Override
	protected String doIt() throws Exception {
		//	For Window Definition
		if(this.getTable_ID() == I_AD_Window.Table_ID) {
			exportWindowDefinition();
		}

		//	For Process Definition
		if(this.getTable_ID() == I_AD_Process.Table_ID) {
			exportProcessDefinition();
		}

		//	For Browser Definition
		if(this.getTable_ID() == I_AD_Browse.Table_ID) {
			exportBrowserDefinition();
		}

		//	For Form Definition
		if(this.getTable_ID() == I_AD_Form.Table_ID) {
			exportFormDefinition();
		}

		//	For Menu Tree
		if (this.getTable_ID() == I_AD_Tree.Table_ID) {
			exportMenuTree();
		}

		//	For menu
		if (this.getTable_ID() == I_AD_Menu.Table_ID) {
			exportMenuItemDefinition();
		}

		return "Ok";
	}

	private void exportWindowDefinition() {
		addLog("@AD_Window_ID@");

		// Add filter a specific Window
		MWindow window = new MWindow(getCtx(), this.getRecord_ID(), get_TrxName());
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@NotFound@ @AD_Window_ID@ " + this.getRecord_ID());
		}
		QueueLoader.getInstance()
			.getQueueManager(ApplicationDictionary.CODE)
			.withEntity(window)
			.addToQueue()
		;
		addLog(window.getAD_Window_ID() + " - " + window.getName());
	}


	private void exportProcessDefinition() {
		addLog("@AD_Process_ID@");

		// Add filter a specific Process
		MProcess process = new MProcess(getCtx(), this.getRecord_ID(), get_TrxName());
		if (process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@NotFound@ @AD_Process_ID@ " + this.getRecord_ID());
		}
		QueueLoader.getInstance()
			.getQueueManager(ApplicationDictionary.CODE)
			.withEntity(process)
			.addToQueue()
		;
		addLog(process.getValue() + " - " + process.getName());
	}

	private void exportBrowserDefinition() {
		addLog("@AD_Browse_ID@");

		// Add filter a specific Browse
		MBrowse browser = new MBrowse(getCtx(), this.getRecord_ID(), get_TrxName());
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			throw new AdempiereException("@NotFound@ @AD_Browse_ID@ " + this.getRecord_ID());
		}
		QueueLoader.getInstance()
			.getQueueManager(ApplicationDictionary.CODE)
			.withEntity(browser)
			.addToQueue()
		;
		addLog(browser.getValue() + " - " + browser.getName());
	}


	private void exportFormDefinition() {
		addLog("@AD_Form_ID@");

		// Add filter a specific Form
		MForm form = new MForm(getCtx(), this.getRecord_ID(), get_TrxName());
		if (form == null || form.getAD_Form_ID() <= 0) {
			throw new AdempiereException("@NotFound@ @AD_Form_ID@ " + this.getRecord_ID());
		}
		QueueLoader.getInstance()
			.getQueueManager(ApplicationDictionary.CODE)
			.withEntity(form)
			.addToQueue()
		;
		addLog(form.getClassname() + " - " + form.getName());
	}


	private void exportMenuTree() {
		addLog("@AD_Tree_ID@");
		MTree menuTree = new MTree(getCtx(), this.getRecord_ID(), false, false, null, get_TrxName());
		if (menuTree == null || menuTree.getAD_Tree_ID() <= 0) {
			throw new AdempiereException("@NotFound@ @AD_Tree_ID@ " + this.getRecord_ID());
		}
		if (!MTree.TREETYPE_Menu.equals(menuTree.getTreeType())) {
			throw new AdempiereException("@NotFound@ MM @TreeType@ " + menuTree.getTreeType());
		} else if (menuTree.getAD_Table_ID() != MMenu.Table_ID) {
			throw new AdempiereException("@NotFound@ @AD_Table_ID@ " + menuTree.getAD_Table_ID());
		}
		QueueLoader.getInstance()
			.getQueueManager(ApplicationDictionary.CODE)
			.withEntity(menuTree)
			.addToQueue()
		;
		addLog(menuTree.getAD_Tree_ID() + " - " + menuTree.getName());
	}


	private void exportMenuItemDefinition() {
		addLog("@AD_Menu_ID@");
		//	For only specific Menu Item
		MMenu menuItem = new Query(
			getCtx(),
			I_AD_Menu.Table_Name,
			I_AD_Menu.COLUMNNAME_AD_Menu_ID + " = ?",
			get_TrxName()
		)
			.setParameters(this.getRecord_ID())
			.first()
		;
		if (menuItem == null) {
			throw new AdempiereException("@NotFound@ @AD_Menu_ID@ " + this.getRecord_ID());
		}
		QueueLoader.getInstance()
			.getQueueManager(ApplicationDictionary.CODE)
			.withEntity(menuItem)
			.addToQueue()
		;
		addLog(menuItem.getAD_Menu_ID() + " - " + menuItem.getName());
	}

}
