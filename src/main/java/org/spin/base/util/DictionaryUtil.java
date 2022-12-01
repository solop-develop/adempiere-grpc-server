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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MView;
import org.adempiere.model.MViewColumn;
import org.adempiere.model.MViewDefinition;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.compiere.util.Language;
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
	 * Add references to original query from tab
	 * @param originalQuery
	 * @return
	 */
	public static String getQueryWithReferencesFromTab(MTab tab) {
		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		final String tableName = table.getTableName();

		String originalQuery = "SELECT " + tableName + ".* FROM " + tableName + " AS " + tableName + " ";
		int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");
		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));
		Language language = Language.getLanguage(Env.getAD_Language(Env.getCtx()));
		for (MField field : tab.getFields(false, null)) {
			if(!field.isDisplayed()) {
				// key column on table
				if (!field.getAD_Column().isKey()) {
					continue;
				}
			}
			MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
			int displayTypeId = field.getAD_Reference_ID();
			if(displayTypeId == 0) {
				displayTypeId = column.getAD_Reference_ID();
			}
			if (ReferenceUtil.validateReference(displayTypeId)) {
				//	Reference Value
				int referenceValueId = field.getAD_Reference_Value_ID();
				if(referenceValueId == 0) {
					referenceValueId = column.getAD_Reference_Value_ID();
				}

				String columnName = column.getColumnName();

				//	Validation Code
				ReferenceInfo referenceInfo = ReferenceUtil.getInstance(
					Env.getCtx()).getReferenceInfo(displayTypeId,
					referenceValueId,
					columnName, 
					language.getAD_Language(),
					tableName
				);
				if(referenceInfo != null) {
					queryToAdd.append(", ");
					String displayedColumn = referenceInfo.getDisplayValue(columnName);
					queryToAdd.append(displayedColumn);
					String joinClause = referenceInfo.getJoinValue(columnName, tableName);
					joinsToAdd.append(joinClause);
				}
			}
		}
		queryToAdd.append(joinsToAdd);
		return queryToAdd.toString();
	}

	/**
	 * Add references to original query from columnsList
	 * @param {MTable} table
	 * @param {ArrayList<MColumn>} columns
	 * @return
	 */
	public static String getQueryWithReferencesFromColumns(MTable table) {
		final String tableName = table.getTableName();

		String originalQuery = "SELECT " + tableName + ".* FROM " + tableName + " AS " + tableName + " ";
		int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");
		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));
		Language language = Language.getLanguage(Env.getAD_Language(Env.getCtx()));
		List<MColumn> columnsList = table.getColumnsAsList();

		for (MColumn column : columnsList) {
			int displayTypeId = column.getAD_Reference_ID();

			if (ReferenceUtil.validateReference(displayTypeId)) {
				//	Reference Value
				int referenceValueId = column.getAD_Reference_Value_ID();

				String columnName = column.getColumnName();

				//	Validation Code
				ReferenceInfo referenceInfo = ReferenceUtil.getInstance(Env.getCtx())
					.getReferenceInfo(
						displayTypeId,
						referenceValueId,
						columnName,
						language.getAD_Language(),
						tableName
					);
				if(referenceInfo != null) {
					queryToAdd.append(", ");
					queryToAdd.append(referenceInfo.getDisplayValue(columnName));
					joinsToAdd.append(referenceInfo.getJoinValue(columnName, tableName));
				}
			}
		}
		queryToAdd.append(joinsToAdd);
		return queryToAdd.toString();
	}
	
	/**
	 * Get Context column names from context
	 * @param context
	 * @return
	 * @return List<String>
	 */
	public static List<String> getContextColumnNames(String context) {
		if(context == null) {
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
			List<MTab> tabList = tabs.stream()
				.filter(parentTab -> {
					return parentTab.getAD_Tab_ID() != tabId
						&& parentTab.getAD_Tab_ID() != optionalTab.get().getAD_Tab_ID()
						&& parentTab.getSeqNo() < seqNo
						&& parentTab.getTabLevel() < tabLevel
						&& !parentTab.isTranslationTab();
				})
				.sorted(
					Comparator.comparing(MTab::getSeqNo)
						.thenComparing(MTab::getTabLevel)
						.reversed()
				)
				.collect(Collectors.toList());
			//	Validate direct child
			if(tabList.size() == 0) {
				if(tab.getParent_Column_ID() != 0) {
					mainColumnName = MColumn.getColumnName(context, tab.getParent_Column_ID());
				}
				String childColumn = mainColumnName;
				if(tab.getAD_Column_ID() != 0) {
					childColumn = MColumn.getColumnName(context, tab.getAD_Column_ID());
				}
				// TODO: Fix childColumn on 'System Translation Check' window > 'Element Translation' tab
				whereClause.append(table.getTableName()).append(".").append(childColumn)
					.append(" = ").append("@").append(mainColumnName).append("@");
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
						if(Util.isEmpty(childColumnName)) {
							MTable childTable = MTable.get(context, currentTab.getAD_Table_ID());
							childColumnName = childTable.getKeyColumns()[0];
						}
						if(Util.isEmpty(childLinkColumnName)) {
							childLinkColumnName = childColumnName;
						}
						whereClause.append(" INNER JOIN ").append(currentTable.getTableName()).append(" AS t").append(aliasIndex)
							.append(" ON(").append("t").append(aliasIndex).append(".").append(childLinkColumnName)
							.append("=").append("t").append(aliasIndex - 1).append(".").append(childColumnName).append(")");
					}
					aliasIndex++;
					if(Util.isEmpty(parentTabUuid)) {
						parentTabUuid = currentTab.getUUID();
					}
				}
				whereClause.append(" WHERE t").append(aliasIndex - 1).append(".").append(mainColumnName).append(" = ").append("@").append(mainColumnName).append("@");
				//	Add support to child
				MTab parentTab = tablesMap.get(aliasIndex -1);
				String parentColumnName = DictionaryServiceImplementation.getParentColumnNameFromTab(tab);
				String linkColumnName = DictionaryServiceImplementation.getLinkColumnNameFromTab(tab);
				if(Util.isEmpty(parentColumnName)) {
					MTable parentTable = MTable.get(context, parentTab.getAD_Table_ID());
					parentColumnName = parentTable.getKeyColumns()[0];
				}
				if(Util.isEmpty(linkColumnName)) {
					linkColumnName = parentColumnName;
				}
				whereClause.append(" AND t").append(0).append(".").append(parentColumnName).append(" = ")
					.append(table.getTableName()).append(".").append(linkColumnName);
				whereClause.append(")");
			}
		}

		StringBuffer where = new StringBuffer(ValueUtil.validateNull(tab.getWhereClause()));
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
	
	/**
	 * Get SQL from View with a custom column as alias
	 * @param viewId
	 * @param columnNameForAlias
	 * @param trxName
	 * @return
	 */
	public static String getSQLFromBrowser(MBrowse browser) {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT DISTINCT ");
		AtomicBoolean co = new AtomicBoolean(false);
		ASPUtil.getInstance().getBrowseDisplayFields(browser.getAD_Browse_ID()).forEach(field -> {
			if (co.get()) {
				sql.append(",");
			}

			MViewColumn viewColumn = MViewColumn.getById(Env.getCtx(), field.getAD_View_Column_ID(), null);
			if (!Util.isEmpty(viewColumn.getColumnSQL(), true)) {
				sql.append(viewColumn.getColumnSQL());
				co.set(true);
			}
			
			sql.append(" AS \"" + viewColumn.getColumnName() + "\"");
		});

		MView view = new MView(Env.getCtx(), browser.getAD_View_ID());
		sql.append(" FROM ").append(view.getFromClause());
		return sql.toString();
	}
	
	/**
	 * Add references to original query from smart browser
	 * @param originalQuery
	 * @return
	 */
	public static String addQueryReferencesFromBrowser(MBrowse browser) {
		String originalQuery = getSQLFromBrowser(browser);
		int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");
		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));
		for (MBrowseField browseField : ASPUtil.getInstance().getBrowseDisplayFields(browser.getAD_Browse_ID())) {
			int displayTypeId = browseField.getAD_Reference_ID();
			if (ReferenceUtil.validateReference(displayTypeId)) {
				//	Reference Value
				int referenceValueId = browseField.getAD_Reference_Value_ID();
				//	Validation Code

				MViewColumn viewColumn = MViewColumn.getById(Env.getCtx(), browseField.getAD_View_Column_ID(), null);
				MViewDefinition viewDefinition = MViewDefinition.get(Env.getCtx(), viewColumn.getAD_View_Definition_ID());
				String tableName = viewDefinition.getTableAlias();

				String columnName = browseField.getAD_Element().getColumnName();
				if (viewColumn.getAD_Column_ID() > 0) {
					MColumn column = MColumn.get(Env.getCtx(), viewColumn.getAD_Column_ID());
					columnName = column.getColumnName();
				}
				ReferenceInfo referenceInfo = ReferenceUtil.getInstance(Env.getCtx()).getReferenceInfo(displayTypeId, referenceValueId, columnName, Env.getAD_Language(Env.getCtx()), tableName);
				if(referenceInfo != null) {
					queryToAdd.append(", ");
					queryToAdd.append(referenceInfo.getDisplayValue(viewColumn.getColumnName()));
					joinsToAdd.append(referenceInfo.getJoinValue(columnName, tableName));
				}
			}
		}
		queryToAdd.append(joinsToAdd);
		return queryToAdd.toString();
	}
	
	/**
	 * Get Order By
	 * @param browser
	 * @return
	 */
	public static String getSQLOrderBy(MBrowse browser) {
		StringBuilder sqlOrderBy = new StringBuilder();
		for (MBrowseField field : ASPUtil.getInstance().getBrowseOrderByFields(browser.getAD_Browse_ID())) {
			if (sqlOrderBy.length() > 0) {
				sqlOrderBy.append(",");
			}

			MViewColumn viewColumn = MViewColumn.getById(Env.getCtx(), field.getAD_View_Column_ID(), null);
			sqlOrderBy.append(viewColumn.getColumnSQL());
		}
		return sqlOrderBy.length() > 0 ? sqlOrderBy.toString(): "";
	}
	
	/**
	 * Get Order By Postirion for SB
	 * @param BrowserField
	 * @return
	 */
	public static int getOrderByPosition(MBrowse browser, MBrowseField BrowserField) {
		int colOffset = 1; // columns start with 1
		int col = 0;
		for (MBrowseField field : browser.getFields()) {
			int sortBySqlNo = col + colOffset;
			if (BrowserField.getAD_Browse_Field_ID() == field.getAD_Browse_Field_ID())
				return sortBySqlNo;
			col ++;
		}
		return -1;
	}
}
