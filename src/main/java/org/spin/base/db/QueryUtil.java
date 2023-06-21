package org.spin.base.db;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.adempiere.core.domains.models.I_AD_WF_Node;
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
import org.spin.util.ASPUtil;

public class QueryUtil {

	/**
	 * Add references to original query from columnsList
	 * @param {MTable} table
	 * @param {ArrayList<MColumn>} columns
	 * @return
	 */
	public static String getTableQueryWithReferences(MTable table) {
		final String tableName = table.getTableName();

		String originalQuery = "SELECT " + tableName + ".* FROM " + tableName + " AS " + tableName + " ";
		int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");
		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));
		Language language = Language.getLanguage(Env.getAD_Language(Env.getCtx()));
		List<MColumn> columnsList = table.getColumnsAsList();

		for (MColumn column : columnsList) {
			int displayTypeId = column.getAD_Reference_ID();
			String columnName = column.getColumnName();

			if (displayTypeId == DisplayType.Button) {
				if (columnName.equals(I_AD_WF_Node.COLUMNNAME_DocAction)) {
					displayTypeId = DisplayType.List;
				}
			}
			if (ReferenceUtil.validateReference(displayTypeId)) {
				//	Reference Value
				int referenceValueId = column.getAD_Reference_Value_ID();

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
	 * Add references to original query from tab
	 * @param originalQuery
	 * @return
	 */
	public static String getTabQueryWithReferences(MTab tab) {
		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		final String tableName = table.getTableName();

		String originalQuery = "SELECT " + tableName + ".* FROM " + tableName + " AS " + tableName + " ";
		int fromIndex = originalQuery.toUpperCase().indexOf(" FROM ");
		StringBuffer queryToAdd = new StringBuffer(originalQuery.substring(0, fromIndex));
		StringBuffer joinsToAdd = new StringBuffer(originalQuery.substring(fromIndex, originalQuery.length() - 1));
		Language language = Language.getLanguage(Env.getAD_Language(Env.getCtx()));
		for (MField field : tab.getFields(false, null)) {
			if (!field.isDisplayed()) {
				// key column on table
				if (!field.getAD_Column().isKey()) {
					continue;
				}
			}
			MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
			String columnName = column.getColumnName();

			// Add virutal column
			String columnSQL = column.getColumnSQL();
			if (!Util.isEmpty(columnSQL, true)) {
				queryToAdd.append(", ")
					.append(columnSQL)
					.append(" AS ")
					.append(column.getColumnName())
				;
			}

			int displayTypeId = field.getAD_Reference_ID();
			if (displayTypeId <= 0) {
				displayTypeId = column.getAD_Reference_ID();
			}
			if (displayTypeId == DisplayType.Button) {
				if (columnName.equals(I_AD_WF_Node.COLUMNNAME_DocAction)) {
					displayTypeId = DisplayType.List;
				}
			}
			if (ReferenceUtil.validateReference(displayTypeId)) {
				if (!Util.isEmpty(columnSQL, true)) {
					StringBuffer displayColumnSQL = new StringBuffer()
						.append(", ")
						.append(columnSQL)
						.append(LookupUtil.DISPLAY_COLUMN_KEY)
						.append("_")
						.append(column.getColumnName())
					;
					queryToAdd.append(displayColumnSQL);
					continue;
				}

				//	Reference Value
				int referenceValueId = field.getAD_Reference_Value_ID();
				if(referenceValueId == 0) {
					referenceValueId = column.getAD_Reference_Value_ID();
				}

				//	Validation Code
				ReferenceInfo referenceInfo = ReferenceUtil.getInstance(
					Env.getCtx()
				).getReferenceInfo(
					displayTypeId,
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
	 * Get SQL from View with a custom column as alias
	 * @param viewId
	 * @param columnNameForAlias
	 * @param trxName
	 * @return
	 */
	public static String getBrowserQuery(MBrowse browser) {
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
	public static String getBrowserQueryWithReferences(MBrowse browser) {
		String originalQuery = getBrowserQuery(browser);
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
				ReferenceInfo referenceInfo = ReferenceUtil.getInstance(Env.getCtx())
					.getReferenceInfo(
						displayTypeId,
						referenceValueId,
						columnName,
						Env.getAD_Language(Env.getCtx()), tableName
					);
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

}
