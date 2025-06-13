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
package org.spin.base.db;

import java.util.List;
import java.util.Properties;

import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MView;
import org.adempiere.model.MViewColumn;
import org.adempiere.model.MViewDefinition;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.base.util.ReferenceUtil;
import org.spin.service.grpc.util.db.FromUtil;
import org.spin.service.grpc.util.db.JoinUtil;

/**
 * Class for handle SQL Query columns
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class QueryUtil {

	/**
	 * Add references to original query from columnsList
	 * @param {MTable} table
	 * @param {ArrayList<MColumn>} columns
	 * @return
	 */
	public static String getTableQueryWithReferences(MTable table) {
		return getTableQueryWithReferences(table, null);
	}

	/**
	 * Add references to original query from columnsList
	 * @param {MTable} table
	 * @param {String} tableAlias to reference base table name
	 * @param {ArrayList<MColumn>} columns
	 * @return
	 */
	public static String getTableQueryWithReferences(MTable table, String tableAlias) {
		final String tableName = table.getTableName();
		if (Util.isEmpty(tableAlias, true)) {
			tableAlias = tableName;
		}

		final String originalQuery = "SELECT " + tableAlias + ".* FROM " + tableName + " AS " + tableAlias + " ";
		final int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");

		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));

		final Language language = Language.getLanguage(Env.getAD_Language(table.getCtx()));
		final List<MColumn> columnsList = table.getColumnsAsList();
		for (MColumn column : columnsList) {
			if (column == null) {
				continue;
			}
			if (!column.isActive()) {
				// key column on table
				if (!column.isKey()) {
					continue;
				}
			}
			int displayTypeId = column.getAD_Reference_ID();
			final String columnName = column.getColumnName();

			// Add virutal column
			final String columnSQL = column.getColumnSQL();
			if (!Util.isEmpty(columnSQL, true)) {
				queryToAdd.append(", ")
					.append(columnSQL)
					.append(" AS ")
					.append("\"" + columnName + "\"")
				;
			}

			//	Reference Value
			int referenceValueId = column.getAD_Reference_Value_ID();

			// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
			displayTypeId = ReferenceUtil.overwriteDisplayType(
				displayTypeId,
				referenceValueId
			);

			if (ReferenceUtil.validateReference(displayTypeId)) {
				if (columnName.equals(tableName + "_ID")) {
					// overwrite to correct sub-query table alias
					displayTypeId = DisplayType.ID;
				}
				final ReferenceInfo referenceInfo = ReferenceUtil.getInstance(
					column.getCtx()
				)
				.getReferenceInfo(
					displayTypeId,
					referenceValueId,
					columnName,
					language.getAD_Language(),
					tableAlias
				);
				if(referenceInfo != null) {
					String joinClause = referenceInfo.getJoinValue(columnName, tableAlias);
					String displayColumn = referenceInfo.getDisplayValue(columnName);
				
					// Add display virutal column
					if (!Util.isEmpty(columnSQL, true)) {
						if (!Util.isEmpty(joinClause, true)) {
							joinClause = joinClause.replace(tableAlias + "." + columnName, columnSQL);
						} else {
							displayColumn = displayColumn.replace(tableAlias + "." + columnName, columnSQL);
						}
					}

					queryToAdd.append(", ");
					queryToAdd.append(displayColumn);
					joinsToAdd.append(joinClause);
				}
			}
		}

		queryToAdd.append(joinsToAdd);
		return queryToAdd.toString();
	}


	/**
	 * Add references to original query from tab
	 * @param {MTab} tab
	 * @return
	 */
	public static String getTabQueryWithReferences(Properties context, MTab tab) {
		return getTabQueryWithReferences(context, tab, null);
	}

	/**
	 * Add references to original query from tab
	 * @param {MTab} tab
	 * @param {String} tableAlias to reference base table name
	 * @return
	 */
	public static String getTabQueryWithReferences(Properties context, MTab tab, String tableAlias) {
		MTable table = MTable.get(tab.getCtx(), tab.getAD_Table_ID());
		final String tableName = table.getTableName();
		if (Util.isEmpty(tableAlias, true)) {
			tableAlias = tableName;
		}

		final String originalQuery = "SELECT " + tableAlias + ".* FROM " + tableName + " AS " + tableAlias + " ";
		final int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");

		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));

		final Language language = Language.getLanguage(Env.getAD_Language(context));
		final MField[] fieldsList = tab.getFields(false, null);
		for (MField field : fieldsList) {
			if (field == null) {
				continue;
			}
			MColumn column = MColumn.get(context, field.getAD_Column_ID());
			if (!column.isActive() || !field.isActive() || !field.isDisplayed()) {
				// key column on table
				if (!column.isKey()) {
					continue;
				}
			}
			final String columnName = column.getColumnName();

			// Add virutal column
			final String columnSQL = column.getColumnSQL();
			if (!Util.isEmpty(columnSQL, true)) {
				queryToAdd.append(", ")
					.append(columnSQL)
					.append(" AS ")
					.append("\"" + columnName + "\"")
				;
			}

			int displayTypeId = field.getAD_Reference_ID();
			if (displayTypeId <= 0) {
				displayTypeId = column.getAD_Reference_ID();
			}

			//	Reference Value
			int referenceValueId = field.getAD_Reference_Value_ID();
			if(referenceValueId <= 0) {
				referenceValueId = column.getAD_Reference_Value_ID();
			}

			// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
			displayTypeId = ReferenceUtil.overwriteDisplayType(
				displayTypeId,
				referenceValueId
			);

			if (ReferenceUtil.validateReference(displayTypeId)) {
				if (columnName.equals(tableName + "_ID")) {
					// overwrite to correct sub-query table alias
					displayTypeId = DisplayType.ID;
				}
				final ReferenceInfo referenceInfo = ReferenceUtil.getInstance(
					context
				).getReferenceInfo(
					displayTypeId,
					referenceValueId,
					columnName,
					language.getAD_Language(),
					tableAlias
				);
				if(referenceInfo != null) {
					String joinClause = referenceInfo.getJoinValue(columnName, tableAlias);
					String displayColumn = referenceInfo.getDisplayValue(columnName);
				
					// Add display virutal column
					if (!Util.isEmpty(columnSQL, true)) {
						if (!Util.isEmpty(joinClause, true)) {
							joinClause = joinClause.replace(tableAlias + "." + columnName, columnSQL);
						} else {
							displayColumn = displayColumn.replace(tableAlias + "." + columnName, columnSQL);
						}
					}

					queryToAdd.append(", ");
					queryToAdd.append(displayColumn);
					joinsToAdd.append(joinClause);
				}
			}
		}
		queryToAdd.append(joinsToAdd);
		return queryToAdd.toString();
	}



	/**
	 * Get SQL from View with a custom column as alias
	 * @param viewId
	 * @param columnNameForAlias
	 * @param trxName
	 * @return
	 */
	public static String getBrowserQuery(MBrowse browser) {
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT DISTINCT ");

		// field key first always
		MBrowseField fieldKey = browser.getFieldKey();
		if(fieldKey == null) {
			MViewColumn column = new MViewColumn(browser.getCtx() , 0 , browser.get_TrxName());
			column.setName("Row");
			column.setColumnSQL("'Row' AS \"Row\"");

			fieldKey = new MBrowseField(browser , column);
			fieldKey.setAD_Reference_ID(DisplayType.ID);
			fieldKey.setIsKey(true);
			fieldKey.setIsReadOnly(false);
		}

		if (!Util.isEmpty(fieldKey.getAD_View_Column().getColumnSQL(), true)) {
			sql.append(
					fieldKey.getAD_View_Column().getColumnSQL()
				)
				.append(" AS")
			;
		}
		sql.append(" \"" + fieldKey.getAD_View_Column().getColumnName() + "\"");

		final List<MBrowseField> browseFieldsList = browser.getDisplayFields();
		for (MBrowseField browseField : browseFieldsList) {
			if (browseField == null) {
				continue;
			}
			// key column on table
			if (browseField.isKey()) {
				continue;
			}
			if (!browseField.isActive()) {
				continue;
			}
			// Sort column to get `DisplayColumn_`
			// if (!(browseField.isDisplayed() || browseField.isIdentifier() || browseField.isOrderBy())) {
			// 	continue;
			// }

			sql.append(", ");

			MViewColumn viewColumn = MViewColumn.getById(
				browseField.getCtx(),
				browseField.getAD_View_Column_ID(),
				null
			);
			if (!Util.isEmpty(viewColumn.getColumnSQL(), true)) {
				sql.append(
						viewColumn.getColumnSQL()
					)
					.append(" AS ")
				;
			}

			sql.append("\"" + viewColumn.getColumnName() + "\"");
		}

		MView view = new MView(browser.getCtx(), browser.getAD_View_ID());
		sql.append(" FROM").append(view.getFromClause());
		return sql.toString();
	}

	/**
	 * Add references to original query from smart browser
	 * @param originalQuery
	 * @return
	 */
	public static String getBrowserQueryWithReferences(MBrowse browser) {
		final String originalQuery = getBrowserQuery(browser);

		final String fromClause = FromUtil.getFromClauseByView(browser.getAD_View_ID());
		final int fromIndex = originalQuery.toUpperCase().indexOf(fromClause.toUpperCase());

		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));
		String secondJoin = JoinUtil.fixSqlFromClause(joinsToAdd.toString());
		if (!Util.isEmpty(secondJoin, true)) {
			joinsToAdd = new StringBuffer(secondJoin);
		}

		final Language language = Language.getLanguage(Env.getAD_Language(browser.getCtx()));
		final List<MBrowseField> browseFieldsList = browser.getDisplayFields();
		for (MBrowseField browseField : browseFieldsList) {
			if (browseField == null) {
				continue;
			}
			if (!browseField.isActive()) {
				// key column on table
				if (!browseField.isKey()) {
					continue;
				}
			}
			// Only displayed or identifier
			if (!(browseField.isKey() || browseField.isDisplayed() || browseField.isIdentifier())) {
				// TODO: Add sort column
				continue;
			}

			//	Reference Value
			int referenceValueId = browseField.getAD_Reference_Value_ID();

			// overwrite display type `Button` to `List`, example `PaymentRule` or `Posted`
			int displayTypeId = ReferenceUtil.overwriteDisplayType(
				browseField.getAD_Reference_ID(),
				referenceValueId
			);

			if (ReferenceUtil.validateReference(displayTypeId)) {
				MViewColumn viewColumn = MViewColumn.getById(browseField.getCtx(), browseField.getAD_View_Column_ID(), null);
				final String dbColumnName = viewColumn.getColumnName();

				MViewDefinition viewDefinition = MViewDefinition.get(browseField.getCtx(), viewColumn.getAD_View_Definition_ID());
				final String tableName = viewDefinition.getTableAlias();

				String columnName = browseField.getAD_Element().getColumnName();
				if (viewColumn.getAD_Column_ID() > 0) {
					MColumn column = MColumn.get(browseField.getCtx(), viewColumn.getAD_Column_ID());
					columnName = column.getColumnName();
				}

				if (columnName.equals(tableName + "_ID")) {
					// overwrite to correct sub-query table alias
					displayTypeId = DisplayType.ID;
				}
				final ReferenceInfo referenceInfo = ReferenceUtil.getInstance(browseField.getCtx())
					.getReferenceInfo(
						displayTypeId,
						referenceValueId,
						columnName,
						dbColumnName,  // as column alias
						language.getAD_Language(),
						tableName
					);
				if(referenceInfo != null) {
					queryToAdd.append(", ");
					referenceInfo.setDisplayColumnAlias(
						LookupUtil.getDisplayColumnName(
							dbColumnName
						)
					);
					final String displayColumn = referenceInfo.getDisplayValue(columnName);
					queryToAdd.append(displayColumn);

					String joinClause = "";
					if (viewColumn.getAD_Column_ID() > 0) {
						joinClause = referenceInfo.getJoinValue(columnName, tableName);
					} else {
						// sub query
						if (!Util.isEmpty(viewColumn.getColumnSQL(), true)) {
							joinClause = referenceInfo.getJoinValue(viewColumn.getColumnSQL());
						} else {
							joinClause = referenceInfo.getJoinValue(columnName);
						}
					}
					joinsToAdd.append(joinClause);
				}
			}
		}
		queryToAdd
			.append(" ")
			.append(joinsToAdd)
		;
		return queryToAdd.toString();
	}

}
