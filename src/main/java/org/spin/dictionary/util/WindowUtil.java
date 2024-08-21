/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.dictionary.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_Process;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.base.util.AccessUtil;

/**
 * Class for handle Window, Tab, and Field
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class WindowUtil {

	/**
	 * Get Tabs Fields to display type
	 * @param tab
	 * @return
	 */
	public static Map<String, Integer> getTabFieldsDisplayType(MTab tab) {
		List<MField> tabFields = Arrays.asList(
			tab.getFields(false, null)
		);

		Map<String, Integer> displayTypeColumns = new HashMap<>();

		if (tabFields == null || tabFields.isEmpty()) {
			return displayTypeColumns;
		}
		tabFields.forEach(tabField -> {
			MColumn column = MColumn.get(
				tab.getCtx(),
				tabField.getAD_Column_ID()
			);
			int displayTypeId = tabField.getAD_Reference_ID();
			if (displayTypeId <= 0) {
				displayTypeId = column.getAD_Reference_ID();
			}
			displayTypeColumns.put(
				column.getColumnName(),
				displayTypeId
			);
		});

		return displayTypeColumns;
	}


	/**
	 * Get Parent column name from tab
	 * @param tab
	 * @return
	 */
	public static String getParentColumnNameFromTab(MTab tab) {
		String parentColumnName = null;
		if(tab.getParent_Column_ID() != 0) {
			parentColumnName = MColumn.getColumnName(tab.getCtx(), tab.getParent_Column_ID());
		}
		return parentColumnName;
	}

	/**
	 * Get Link column name from tab
	 * @param tab
	 * @return
	 */
	public static String getLinkColumnNameFromTab(MTab tab) {
		String parentColumnName = null;
		if(tab.getAD_Column_ID() != 0) {
			parentColumnName = MColumn.getColumnName(tab.getCtx(), tab.getAD_Column_ID());
		}
		return parentColumnName;
	}


	/**
	 *	Returns true if this is a detail record
	 *  @return true if not parent tab
	 */
	public static boolean isDetail(MTab tab)
	{
		// First Tab Level is not a detail 
		if (tab.getTabLevel() == 0) {
			return false;
		}

		List<MField> fielsList = Arrays.asList(
			tab.getFields(false, null)
		);
		boolean isWithParentFields = fielsList
			.parallelStream()
			.filter(field -> {
				MColumn column = MColumn.get(
					field.getCtx(),
					field.getAD_Column_ID()
				);
				return column.isParent();
			})
			.findFirst()
			.isPresent()
		;
		//	We have IsParent columns and/or a link column
		if (isWithParentFields || tab.getAD_Column_ID() > 0) {
			return true;
		}
		return false;
	}	//	isDetail



	/**
	 * Get Direct Partent Tab by Current Tab
	 * @param windowId
	 * @param tabId
	 * @return
	 */
	public static int getDirectParentTabId(int windowId, int tabId) {
		MTab tab = MTab.get(
			Env.getCtx(),
			tabId
		);

		final int tabLevel = tab.getTabLevel();
		final int tabSequence = tab.getSeqNo();
		int parentTabId = -1;
		// root tab has no parent
		if (tabLevel > 0) {
			AtomicReference<Integer> parentTabSequence = new AtomicReference<Integer>(-1);
			AtomicReference<MTab> parentTabRefecence = new AtomicReference<MTab>();
			MWindow windowDefintion = MWindow.get(
				Env.getCtx(),
				windowId
			);
			List<MTab> tabsList = Arrays.asList(
				windowDefintion.getTabs(false, null)
			);
			tabsList.forEach(tabItem -> {
				if (tabItem.getTabLevel() >= tabLevel || tabItem.getSeqNo() >= tabSequence) {
					// it is child tab
					return;
				}

				// current tab is more down that tab list
				if (parentTabSequence.get() == -1 || tabItem.getSeqNo() > parentTabSequence.get()) {
					parentTabSequence.set(tabItem.getSeqNo());
					parentTabRefecence.set(tabItem);
				}
			});
			if (parentTabRefecence.get() != null) {
				parentTabId = parentTabRefecence.get().getAD_Tab_ID();
			}
		}
		return parentTabId;
	}

	/**
	 * Get list of direct parent tabs by current tab id
	 * @param windowId window of tabs
	 * @param currentTabId current tab to get parents
	 * @param tabsList
	 * @return
	 */
	public static List<MTab> getParentTabsList(int windowId, int currentTabId, List<MTab> tabsList) {
		int parentTabId = getDirectParentTabId(windowId, currentTabId);
		if (parentTabId > 0) {
			MTab parentTab = MTab.get(
				Env.getCtx(),
				parentTabId
			);
			// TODO: Evaluate `IsActive`
			tabsList.add(parentTab);
			getParentTabsList(windowId, parentTabId, tabsList);
		}
		return tabsList;
	}



	/**
	 * Get process action from tab
	 * @param context
	 * @param tab
	 * @return
	 */
	public static List<MProcess> getProcessActionFromTab(Properties context, MTab tab) {
		// to prevent duplicity of associated processes in different locations (table, column and tab).
		HashMap<Integer, MProcess> processList = new HashMap<>();

		final String whereClause = "IsActive='Y' "
			// first process on tab
			+ "AND (AD_Process_ID = ? " // #1
			// process on column
			+ "OR EXISTS("
				+ "SELECT 1 FROM AD_Field f "
				+ "INNER JOIN AD_Column c ON(c.AD_Column_ID = f.AD_Column_ID) "
				+ "WHERE c.AD_Process_ID = AD_Process.AD_Process_ID "
				+ "AND f.IsDisplayed = 'Y' "
				// ASP filter
				// TODO: Add filter with ASP Level
				// + "AND NOT EXISTS("
				// 	+ "SELECT 1 FROM AD_FieldCustom AS fc "
				// 	+ "INNER JOIN AD_TabCustom AS tc "
				// 		+ "ON(tc.AD_TabCustom_ID = fc.AD_TabCustom_ID AND tc.IsActive = 'Y') "
				// 	+ "INNER JOIN AD_WindowCustom AS wc "
				// 		+ "ON(wc.AD_WindowCustom_ID = tc.AD_WindowCustom_ID AND wc.IsActive = 'Y') "
				// 	+ "WHERE fc.IsActive = 'Y' "
				// 	+ "AND fc.IsDisplayed = 'N' "
				// 	+ "AND fc.AD_Field_ID = f.AD_Field_ID "
				// 	+ "AND (wc.AD_User_ID = ? OR wc.AD_Role_ID = ?)"
				// + ") "
				+ "AND f.AD_Tab_ID = ? " // #2
				+ "AND f.IsActive = 'Y'"
			+ ") "
			// process on table
			+ "OR EXISTS("
				+ "SELECT 1 FROM AD_Table_Process AS tp "
				+ "WHERE tp.AD_Process_ID = AD_Process.AD_Process_ID "
				+ "AND tp.AD_Table_ID = ? " // #3
				+ "AND tp.IsActive = 'Y'"
			+ ")"
			+ ")"
		;
		List<Object> filterList = new ArrayList<>();
		filterList.add(tab.getAD_Process_ID());
		filterList.add(tab.getAD_Tab_ID());
		filterList.add(tab.getAD_Table_ID());
		//	Process from tab
		List<Integer> processIdList = new Query(
			tab.getCtx(),
			I_AD_Process.Table_Name,
			whereClause,
			null
		)
			.setParameters(filterList)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.getIDsAsList()
		;

		MRole role = MRole.getDefault(tab.getCtx(), false);
		for(Integer processId : processIdList) {
			// Record/Role access
			boolean isWithAccess = AccessUtil.isProcessAccess(role, processId);
			if (isWithAccess) {
				MProcess process = MProcess.get(
					tab.getCtx(),
					processId
				);
				processList.put(processId, process);
			}
		}

		return new ArrayList<MProcess>(processList.values());
	}

}
