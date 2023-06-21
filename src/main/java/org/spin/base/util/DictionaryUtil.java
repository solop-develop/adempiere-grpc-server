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
package org.spin.base.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.compiere.model.MColumn;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.grpc.service.DictionaryServiceImplementation;
import org.spin.util.ASPUtil;

/**
 * Class for handle records utils values
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class DictionaryUtil {
	
	public static String ID_PREFIX = "_ID";
	
	public static String TRANSLATION_SUFFIX = "_Trl";
	
	/**
	 * Get Context column names from context
	 * @param context
	 * @return
	 * @return List<String>
	 */
	public static List<String> getContextColumnNames(String context) {
		if (Util.isEmpty(context, true)) {
			return new ArrayList<String>();
		}
		String START = "\\@";  // A literal "(" character in regex
		String END   = "\\@";  // A literal ")" character in regex

		// Captures the word(s) between the above two character(s)
		String patternValue = START + "(#|$){0,1}(\\w+)" + END;

		Pattern pattern = Pattern.compile(patternValue);
		Matcher matcher = pattern.matcher(context);
		Map<String, Boolean> columnNamesMap = new HashMap<String, Boolean>();
		while(matcher.find()) {
			columnNamesMap.put(matcher.group().replace("@", "").replace("@", ""), true);
		}
		return new ArrayList<String>(columnNamesMap.keySet());
	}

	/**
	 * Determinate if columnName is used on context values
	 * @param columnName
	 * @param context
	 * @return boolean
	 */
	public static boolean isUseParentColumnOnContext(String columnName, String context) {
		if (Util.isEmpty(columnName, true)) {
			return false;
		}
		if (Util.isEmpty(context, true)) {
			return false;
		}

		// @ColumnName@ , @#ColumnName@ , @$ColumnName@
		StringBuffer patternValue = new StringBuffer()
			.append("(@")
			.append("($|#){0,1}")
			.append(columnName)
			.append("@)");

		Pattern pattern = Pattern.compile(patternValue.toString(), Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(context);
		boolean isUsedParentColumn = matcher.find();

		return isUsedParentColumn;
	}

	/**
	 * Add and get talbe alias to columns in validation code sql
	 * @param tableAlias
	 * @param dynamicValidation
	 * @return {String}
	 */
	public static String getValidationCodeWithAlias(String tableAlias, String dynamicValidation) {
		if (Util.isEmpty(dynamicValidation, true)) {
			return "";
		}

		Matcher matcherTableAliases = Pattern.compile(
				tableAlias + "\\.",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			)
			.matcher(dynamicValidation);

		String validationCode = dynamicValidation;
		if (!matcherTableAliases.find()) {
			String sqlOperators = "(<>|<=|>=|!=|<|=|>|NOT\\s+IN|IN|NOT\\s+BETWEEN|BETWEEN|NOT\\s+LIKE|LIKE|IS\\s+NULL|IS\\s+NOT\\s+NULL)";
			// columnName = value
			Pattern patternColumnName = Pattern.compile(
				"(\\w+)(\\s+){0,1}" + sqlOperators,
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			);
			Matcher matchColumnName = patternColumnName.matcher(validationCode);
			validationCode = matchColumnName.replaceAll(tableAlias + ".$1$2$3"); // $&
		}

		return validationCode;
	}


	public static int getDirectParentTabId(int windowId, int tabId) {
		MTab tab = ASPUtil.getInstance(Env.getCtx()).getWindowTab(windowId, tabId);

		final int tabLevel = tab.getTabLevel();
		final int tabSequence = tab.getSeqNo();
		int parentTabId = -1;
		// root tab has no parent
		if (tabLevel > 0) {
			AtomicReference<Integer> parentTabSequence = new AtomicReference<Integer>(-1);
			AtomicReference<MTab> parentTabRefecence = new AtomicReference<MTab>();
			List<MTab> tabsList = ASPUtil.getInstance(Env.getCtx()).getWindowTabs(tab.getAD_Window_ID());
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
			MTab parentTab = ASPUtil.getInstance(Env.getCtx()).getWindowTab(windowId, parentTabId);
			tabsList.add(parentTab);
			getParentTabsList(windowId, parentTabId, tabsList);
		}
		return tabsList;
	}

	/**
	 * Get SQL Where Clause including link column and parent column
	 * @param {Properties} context
	 * @param {MTab} tab
	 * @param {List<MTab>} tabs
	 * @return {String}
	 */
	public static String getSQLWhereClauseFromTab(Properties context, MTab tab, List<MTab> tabs) {
		if (tabs == null) {
			tabs = ASPUtil.getInstance(context).getWindowTabs(tab.getAD_Window_ID());
		}

		StringBuffer whereClause = new StringBuffer();
		String parentTabUuid = null;
		MTable table = MTable.get(context, tab.getAD_Table_ID());

		int tabId = tab.getAD_Tab_ID();
		int seqNo = tab.getSeqNo();
		int tabLevel = tab.getTabLevel();
		//	Create where clause for children
		if (tab.getTabLevel() > 0 && tabs != null) {
			Optional<MTab> optionalTab = tabs.stream()
				.filter(parentTab -> {
					return parentTab.getAD_Tab_ID() != tabId
						&& parentTab.getTabLevel() == 0;
				})
				.findFirst();
			String mainColumnName = null;
			MTable mainTable = null;
			if(optionalTab.isPresent()) {
				mainTable = MTable.get(context, optionalTab.get().getAD_Table_ID());
				mainColumnName = mainTable.getKeyColumns()[0];
			}

			List<MTab> parentTabsList = getParentTabsList(tab.getAD_Window_ID(), tabId, new ArrayList<MTab>());
			List<MTab> tabList = parentTabsList.stream()
				.filter(parentTab -> {
					return parentTab.getAD_Tab_ID() != tabId
						&& parentTab.getAD_Tab_ID() != optionalTab.get().getAD_Tab_ID()
						&& parentTab.getSeqNo() < seqNo
						&& parentTab.getTabLevel() < tabLevel
						&& !parentTab.isTranslationTab()
					;
				})
				.sorted(
					Comparator.comparing(MTab::getSeqNo)
						.thenComparing(MTab::getTabLevel)
						.reversed()
				)
				.collect(Collectors.toList());

			//	Validate direct child
			if (tabList == null || tabList.size() == 0) {
				if (tab.getParent_Column_ID() > 0) {
					mainColumnName = MColumn.getColumnName(context, tab.getParent_Column_ID());
				}
				String childColumn = mainColumnName;
				if (tab.getAD_Column_ID() > 0) {
					childColumn = MColumn.getColumnName(context, tab.getAD_Column_ID());
					mainColumnName = childColumn;
				}

				whereClause.append(table.getTableName()).append(".").append(childColumn);
				if (mainColumnName != null && mainColumnName.endsWith("_ID")) {
					whereClause.append(" = ").append("@").append(mainColumnName).append("@");
				} else {
					whereClause.append(" = ").append("'@").append(mainColumnName).append("@'");
				}
				if(optionalTab.isPresent()) {
					parentTabUuid = optionalTab.get().getUUID();
				}
			} else {
				whereClause.append("EXISTS(SELECT 1 FROM");
				Map<Integer, MTab> tablesMap = new HashMap<>();
				int aliasIndex = 0;
				boolean firstResult = true;
				for(MTab currentTab : tabList) {
					tablesMap.put(aliasIndex, currentTab);
					MTable currentTable = MTable.get(context, currentTab.getAD_Table_ID());
					if(firstResult) {
						whereClause.append(" ").append(currentTable.getTableName()).append(" AS t").append(aliasIndex);
						firstResult = false;
					} else {
						MTab childTab = tablesMap.get(aliasIndex -1);
						String childColumnName = DictionaryServiceImplementation.getParentColumnNameFromTab(childTab);
						String childLinkColumnName = DictionaryServiceImplementation.getLinkColumnNameFromTab(childTab);
						//	Get from parent
						if (Util.isEmpty(childColumnName, true)) {
							MTable childTable = MTable.get(context, currentTab.getAD_Table_ID());
							childColumnName = childTable.getKeyColumns()[0];
						}
						if (Util.isEmpty(childLinkColumnName, true)) {
							childLinkColumnName = childColumnName;
						}
						whereClause.append(" INNER JOIN ").append(currentTable.getTableName()).append(" AS t").append(aliasIndex)
							.append(" ON(").append("t").append(aliasIndex).append(".").append(childLinkColumnName)
							.append("=").append("t").append(aliasIndex - 1).append(".").append(childColumnName).append(")")
						;
					}
					aliasIndex++;
					if (Util.isEmpty(parentTabUuid, true)) {
						parentTabUuid = currentTab.getUUID();
					}
				}
				whereClause.append(" WHERE t").append(aliasIndex - 1).append(".").append(mainColumnName).append(" = ")
					.append("@").append(mainColumnName).append("@")
				;
				//	Add support to child
				MTab parentTab = tablesMap.get(aliasIndex -1);
				String parentColumnName = DictionaryServiceImplementation.getParentColumnNameFromTab(tab);
				String linkColumnName = DictionaryServiceImplementation.getLinkColumnNameFromTab(tab);
				if (Util.isEmpty(parentColumnName, true)) {
					MTable parentTable = MTable.get(context, parentTab.getAD_Table_ID());
					parentColumnName = parentTable.getKeyColumns()[0];
				}
				if (Util.isEmpty(linkColumnName, true)) {
					linkColumnName = parentColumnName;
				}
				whereClause.append(" AND t").append(0).append(".").append(parentColumnName).append(" = ")
					.append(table.getTableName()).append(".").append(linkColumnName)
					.append(")")
				;
			}
		}

		StringBuffer where = new StringBuffer();
		final String whereTab = org.spin.base.dictionary.DictionaryUtil.getWhereClauseFromTab(tab.getAD_Window_ID(), tabId);
		if (!Util.isEmpty(whereTab, true)) {
			String whereWithAlias = DictionaryUtil.getValidationCodeWithAlias(
				table.getTableName(),
				whereTab
			);
			where.append(whereWithAlias);
		}

		//	Set where clause for tab
		if (Util.isEmpty(where.toString(), true)) {
			return whereClause.toString();
		}
		if (Util.isEmpty(whereClause.toString(), true)) {
			return where.toString();
		}
		// joined tab where clause with generated where clause
		where.append(" AND ").append("(").append(whereClause).append(")");
		return where.toString();
	}

}
