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
import java.util.concurrent.atomic.AtomicBoolean;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.X_AD_Reference;
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
					.append(columnName)
				;
			}

			//	Reference Value
			int referenceValueId = column.getAD_Reference_Value_ID();

			if (DisplayType.Button == displayTypeId) {
				//	Reference Value
				if (referenceValueId > 0) {
					X_AD_Reference reference = new X_AD_Reference(column.getCtx(), referenceValueId, null);
					if (reference != null && reference.getAD_Reference_ID() > 0) {
						// overwrite display type to Table or List
						if (X_AD_Reference.VALIDATIONTYPE_TableValidation.equals(reference.getValidationType())) {
							displayTypeId = DisplayType.Table;
						} else {
							displayTypeId = DisplayType.List;
						}
					}
				} else if (columnName.equals(I_AD_ChangeLog.COLUMNNAME_Record_ID)) {
					// int tableId = Env.getContextAsInt(column.getCtx(), 0, I_AD_Table.COLUMNNAME_AD_Table_ID);
					// MTable tableButton = MTable.get(column.getCtx(), tableId);
					// String tableKeyColumn = tableButton.getTableName() + "_ID";
					// columnName = tableKeyColumn;
					// // overwrite display type to Table Direct
					// displayTypeId = DisplayType.TableDir;
				}
			}

			if (ReferenceUtil.validateReference(displayTypeId)) {
				// Add display virutal column
				if (!Util.isEmpty(columnSQL, true)) {
					StringBuffer displayColumnSQL = new StringBuffer()
						.append(", ")
						.append(columnSQL)
						.append(" AS ")
						.append(
							LookupUtil.getDisplayColumnName(
								columnName
							)
						)
					;
					queryToAdd.append(displayColumnSQL);
					continue;
				}

				if (columnName.equals(tableName + "_ID")) {
					// overwrite to correct sub-query table alias
					displayTypeId = DisplayType.ID;
				}
				final ReferenceInfo referenceInfo = ReferenceUtil.getInstance(column.getCtx())
					.getReferenceInfo(
						displayTypeId,
						referenceValueId,
						columnName,
						language.getAD_Language(),
						tableAlias
					);
				if(referenceInfo != null) {
					queryToAdd.append(", ");
					final String displayColumn = referenceInfo.getDisplayValue(columnName);
					queryToAdd.append(displayColumn);
					final String joinClause = referenceInfo.getJoinValue(columnName, tableAlias);
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
	public static String getTabQueryWithReferences(MTab tab) {
		return getTabQueryWithReferences(tab, null);
	}

	/**
	 * Add references to original query from tab
	 * @param {MTab} tab
	 * @param {String} tableAlias to reference base table name
	 * @return
	 */
	public static String getTabQueryWithReferences(MTab tab, String tableAlias) {
		MTable table = MTable.get(tab.getCtx(), tab.getAD_Table_ID());
		final String tableName = table.getTableName();
		if (Util.isEmpty(tableAlias, true)) {
			tableAlias = tableName;
		}

		final String originalQuery = "SELECT " + tableAlias + ".* FROM " + tableName + " AS " + tableAlias + " ";
		final int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");

		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));

		final Language language = Language.getLanguage(Env.getAD_Language(tab.getCtx()));
		final MField[] fieldsList = tab.getFields(false, null);
		for (MField field : fieldsList) {
			MColumn column = MColumn.get(field.getCtx(), field.getAD_Column_ID());
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
					.append(columnName)
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

			if (DisplayType.Button == displayTypeId) {
				//	Reference Value
				if (referenceValueId > 0) {
					X_AD_Reference reference = new X_AD_Reference(field.getCtx(), referenceValueId, null);
					if (reference != null && reference.getAD_Reference_ID() > 0) {
						// overwrite display type to Table or List
						if (X_AD_Reference.VALIDATIONTYPE_TableValidation.equals(reference.getValidationType())) {
							displayTypeId = DisplayType.Table;
						} else {
							displayTypeId = DisplayType.List;
						}
					}
				} else if (columnName.equals(I_AD_ChangeLog.COLUMNNAME_Record_ID)) {
					// int tableId = Env.getContextAsInt(field.getCtx(), 0, I_AD_Table.COLUMNNAME_AD_Table_ID);
					// MTable tableButton = MTable.get(field.getCtx(), tableId);
					// String tableKeyColumn = tableButton.getTableName() + "_ID";
					// columnName = tableKeyColumn;
					// // overwrite display type to Table Direct
					// displayTypeId = DisplayType.TableDir;
				}
			}

			if (ReferenceUtil.validateReference(displayTypeId)) {
				// Add display virutal column
				if (!Util.isEmpty(columnSQL, true)) {
					StringBuffer displayColumnSQL = new StringBuffer()
						.append(", ")
						.append(columnSQL)
						.append(" AS ")
						.append(
							LookupUtil.getDisplayColumnName(
								columnName
							)
						)
					;
					queryToAdd.append(displayColumnSQL);
					continue;
				}

				if (columnName.equals(tableName + "_ID")) {
					// overwrite to correct sub-query table alias
					displayTypeId = DisplayType.ID;
				}
				final ReferenceInfo referenceInfo = ReferenceUtil.getInstance(
					field.getCtx()
				).getReferenceInfo(
					displayTypeId,
					referenceValueId,
					columnName,
					language.getAD_Language(),
					tableAlias
				);
				if(referenceInfo != null) {
					queryToAdd.append(", ");
					final String displayColumn = referenceInfo.getDisplayValue(columnName);
					queryToAdd.append(displayColumn);
					final String joinClause = referenceInfo.getJoinValue(columnName, tableAlias);
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
		AtomicBoolean isAddFirstColumn = new AtomicBoolean(false);
		final List<MBrowseField> browseFieldsList = browser.getFields();
		for (MBrowseField browseField : browseFieldsList) {
			if (!browseField.isActive()) {
				// key column on table
				if (!browseField.isKey()) {
					continue;
				}
			}
			// TODO: Add sort column
			if (!(browseField.isKey() || browseField.isDisplayed() || browseField.isIdentifier())) {
				continue;
			}
			if (isAddFirstColumn.get()) {
				sql.append(",");
			}

			MViewColumn viewColumn = MViewColumn.getById(
				browseField.getCtx(),
				browseField.getAD_View_Column_ID(),
				null
			);
			if (!Util.isEmpty(viewColumn.getColumnSQL(), true)) {
				sql.append(viewColumn.getColumnSQL());
				isAddFirstColumn.set(true);
			}

			sql.append(" AS ")
				.append("\"" + viewColumn.getColumnName() + "\"")
			;
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

		final Language language = Language.getLanguage(Env.getAD_Language(browser.getCtx()));
		final List<MBrowseField> browseFieldsList = browser.getFields();
		for (MBrowseField browseField : browseFieldsList) {
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

			int displayTypeId = browseField.getAD_Reference_ID();

			//	Reference Value
			int referenceValueId = browseField.getAD_Reference_Value_ID();

			if (DisplayType.Button == displayTypeId) {
				//	Reference Value
				if (referenceValueId > 0) {
					X_AD_Reference reference = new X_AD_Reference(browseField.getCtx(), referenceValueId, null);
					if (reference != null && reference.getAD_Reference_ID() > 0) {
						// overwrite display type to Table or List
						if (X_AD_Reference.VALIDATIONTYPE_TableValidation.equals(reference.getValidationType())) {
							displayTypeId = DisplayType.Table;
						} else {
							displayTypeId = DisplayType.List;
						}
					}
				}
			}

			if (ReferenceUtil.validateReference(displayTypeId)) {
				MViewColumn viewColumn = MViewColumn.getById(browseField.getCtx(), browseField.getAD_View_Column_ID(), null);
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
						language.getAD_Language(),
						tableName
					);
				if(referenceInfo != null) {
					queryToAdd.append(", ");
					final String dbColumnName = viewColumn.getColumnName();
					referenceInfo.setDisplayColumnAlias(
						LookupUtil.getDisplayColumnName(
							dbColumnName
						)
					);
					final String displayColumn = referenceInfo.getDisplayValue(columnName);
					queryToAdd.append(displayColumn);
					String joinClause = referenceInfo.getJoinValue(columnName, tableName);
					if (viewColumn.getAD_Column_ID() <= 0) {
						// sub query
						joinClause = referenceInfo.getJoinValue(columnName);
						if (!Util.isEmpty(viewColumn.getColumnSQL(), true) && viewColumn.getColumnSQL().contains("(")) {
							joinClause = referenceInfo.getJoinValue(viewColumn.getColumnSQL());
						}
					}
					joinsToAdd.append(joinClause);
				}
			}
		}
		queryToAdd.append(joinsToAdd);
		return queryToAdd.toString();
	}

}
