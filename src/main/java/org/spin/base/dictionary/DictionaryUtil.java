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
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.base.dictionary;

import org.adempiere.core.domains.models.I_AD_Browse;
import org.adempiere.core.domains.models.I_AD_Form;
import org.adempiere.core.domains.models.I_AD_Menu;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.I_AD_Workflow;
import org.compiere.model.MColumn;
import org.compiere.model.MMenu;
import org.compiere.model.MRecentItem;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Class for handle records utils values
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class DictionaryUtil {



	/**
	 * Get reference from column name and table
	 * @param tableId
	 * @param columnName
	 * @return
	 */
	public static int getReferenceId(int tableId, String columnName) {
		MColumn column = MTable.get(Env.getCtx(), tableId).getColumn(columnName);
		if (column == null || column.getAD_Column_ID() <= 0) {
			return -1;
		}
		return column.getAD_Reference_ID();
	}



	/**
	 * Add element to recent item
	 * @param action
	 * @param optionId
	 */
	public static void addToRecentItem(String action, int optionId) {
		if (Util.isEmpty(action, true) || optionId <= 0) {
			return;
		}
		String whereClause = null;
		if(action.equals(MMenu.ACTION_Window)) {
			whereClause = I_AD_Window.COLUMNNAME_AD_Window_ID + " = ?";
		} else if(action.equals(MMenu.ACTION_Form)) {
			whereClause = I_AD_Form.COLUMNNAME_AD_Form_ID + " = ?";
		} else if(action.equals(MMenu.ACTION_Process) || action.equals(MMenu.ACTION_Report)) {
			whereClause = I_AD_Process.COLUMNNAME_AD_Process_ID + " = ?";
		} else if(action.equals(MMenu.ACTION_WorkFlow)) {
			whereClause = I_AD_Workflow.COLUMNNAME_AD_Workflow_ID + " = ?";
		} else if(action.equals(MMenu.ACTION_SmartBrowse)) {
			whereClause = I_AD_Browse.COLUMNNAME_AD_Browse_ID + " = ?";
		}
		//	Get menu
		int menuId = new Query(
			Env.getCtx(),
			I_AD_Menu.Table_Name,
			whereClause,
			null
		)
			.setParameters(optionId)
			.firstId()
		;
		if (menuId <= 0) {
			return;
		}
		if (!action.equals(MMenu.ACTION_Window)) {
			optionId = 0;
		}
		MRecentItem.addMenuOption(Env.getCtx(), menuId, optionId);
	}

}
