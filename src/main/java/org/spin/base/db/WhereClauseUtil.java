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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MView;
import org.adempiere.model.MViewColumn;
import org.compiere.model.MColumn;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.base.query.Filter;
import org.spin.base.query.FilterManager;
import org.spin.base.util.RecordUtil;
import org.spin.dictionary.util.WindowUtil;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.ASPUtil;

/**
 * Class for handle SQL Where Clause
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class WhereClauseUtil {

	/**
	 * Add and get talbe alias to columns in validation code sql
	 * @param tableAlias
	 * @param dynamicValidation
	 * @return {String}
	 */
	public static String getWhereRestrictionsWithAlias(String tableAlias, String dynamicValidation) {
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
			// columnName = value
			Pattern patternColumnName = Pattern.compile(
				"(\\w+)(\\s+){0,1}" + OperatorUtil.SQL_OPERATORS_REGEX,
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			);
			Matcher matchColumnName = patternColumnName.matcher(validationCode);
			validationCode = matchColumnName.replaceAll(tableAlias + ".$1$2$3"); // $&
		}

		return validationCode;
	}



	/**
	 * Get sql restriction by operator
	 * @param columnName
	 * @param operatorValue
	 * @param value
	 * @param valueTo
	 * @param valuesList
	 * @param params
	 * @return
	 */
	public static String getRestrictionByOperator(Filter condition, List<Object> params) {
		return getRestrictionByOperator(condition, 0, params);
	}

	/**
	 * Get sql restriction by operator
	 * @param condition
	 * @param displayType
	 * @param parameters
	 * @return
	 */
	public static String getRestrictionByOperator(Filter condition, int displayType, List<Object> parameters) {
		String operatorValue = Filter.EQUAL;
		if (!Util.isEmpty(condition.getOperator(), true)) {
			operatorValue = condition.getOperator().toLowerCase();
		}
		String sqlOperator = OperatorUtil.convertOperator(condition.getOperator());

		String columnName = condition.getColumnName();
		String sqlValue = "";
		StringBuilder additionalSQL = new StringBuilder();
		//	For IN or NOT IN
		if (operatorValue.equals(Filter.IN)
				|| operatorValue.equals(Filter.NOT_IN)) {
			StringBuilder parameterValues = new StringBuilder();
			final String baseColumnName = columnName;
			StringBuilder column_name = new StringBuilder(columnName);

			condition.getValues().forEach(currentValue -> {
				boolean isString = DisplayType.isText(displayType) || currentValue instanceof String;

				if (currentValue == null || (isString && Util.isEmpty((String) currentValue, true))) {
					if (Util.isEmpty(additionalSQL.toString(), true)) {
						additionalSQL.append("(SELECT " + baseColumnName + " WHERE " + baseColumnName + " IS NULL)");
					}
					if (isString) {
						currentValue = "";
					} else {
						// does not add the null value to the filters, another restriction is
						// added only for null values `additionalSQL`.
						return;
					}
				}
				if (parameterValues.length() > 0) {
					parameterValues.append(", ");
				}
				String sqlInValue = "?";
				if (isString) {
					column_name.delete(0, column_name.length());
					column_name.append("UPPER(").append(baseColumnName).append(")");
					sqlInValue = "UPPER(?)";
				}
				parameterValues.append(sqlInValue);

				Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
					displayType,
					currentValue
				);
				parameters.add(valueToFilter);
			});

			columnName = column_name.toString();
			if (!Util.isEmpty(parameterValues.toString(), true)) {
				sqlValue = "(" + parameterValues.toString() + ")";
				if (!Util.isEmpty(additionalSQL.toString(), true)) {
					additionalSQL.insert(0, " OR " + columnName + sqlOperator);
				}
			}
		} else if(operatorValue.equals(Filter.BETWEEN) || operatorValue.equals(Filter.NOT_BETWEEN)) {
			// List<Object> values = condition.getValues();
			Object valueStartToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getFromValue()
			);
			Object valueEndToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getToValue()
			);

			sqlValue = "";
			if (valueStartToFilter == null) {
				sqlValue = " ? ";
				sqlOperator = OperatorUtil.convertOperator(Filter.LESS_EQUAL);
				parameters.add(valueEndToFilter);
			} else if (valueEndToFilter == null) {
				sqlValue = " ? ";
				sqlOperator = OperatorUtil.convertOperator(Filter.GREATER_EQUAL);
				parameters.add(valueStartToFilter);
			} else {
				sqlValue = " ? AND ? ";
				parameters.add(valueStartToFilter);
				parameters.add(valueEndToFilter);
			}
		} else if(operatorValue.equals(Filter.LIKE) || operatorValue.equals(Filter.NOT_LIKE)) {
			columnName = "UPPER(" + columnName + ")";
			String valueToFilter = ValueManager.validateNull(
				(String) condition.getValue()
			);
			// if (!Util.isEmpty(valueToFilter, true)) {
			// 	if (!valueToFilter.startsWith("%")) {
			// 		valueToFilter = "%" + valueToFilter;
			// 	}
			// 	if (!valueToFilter.endsWith("%")) {
			// 		valueToFilter += "%";
			// 	}
			// }
			// valueToFilter = "UPPPER(" + valueToFilter + ")";
			sqlValue = "'%' || UPPER(?) || '%'";
			parameters.add(valueToFilter);
		} else if(operatorValue.equals(Filter.NULL) || operatorValue.equals(Filter.NOT_NULL)) {
			;
		} else if (operatorValue.equals(Filter.EQUAL) || operatorValue.equals(Filter.NOT_EQUAL)) {
			Object parameterValue = condition.getValue();
			sqlValue = " ? ";

			boolean isString = DisplayType.isText(displayType);
			boolean isEmptyString = isString && Util.isEmpty((String) parameterValue, true);
			if (isString) {
				if (isEmptyString) {
					parameterValue = "";
				} else {
					columnName = "UPPER(" + columnName + ")";
					sqlValue = "UPPER(?)";
				}
			}
			if (parameterValue == null || isEmptyString) {
				additionalSQL.append(" OR ")
					.append(columnName)
					.append(" IS NULL ")
				;
			}

			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				parameterValue
			);
			parameters.add(valueToFilter);
		} else {
			// Greater, Greater Equal, Less, Less Equal
			sqlValue = " ? ";

			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);
			parameters.add(valueToFilter);
		}

		String rescriction = "(" + columnName + sqlOperator + sqlValue + additionalSQL.toString() + ")";

		return rescriction;
	}

	/**
	 * Get sql restriction by operator without manage filters
	 * @param columnName
	 * @param operatorValue
	 * @param value
	 * @param valueTo
	 * @param valuesList
	 * @param params
	 * @return
	 */
	public static String getRestrictionByOperator(Filter condition, int displayType) {
		String sqlOperator = OperatorUtil.convertOperator(condition.getOperator());
		String columnName = condition.getColumnName();
		String operatorValue = condition.getOperator();
		String sqlValue = "";
		StringBuilder additionalSQL = new StringBuilder();
		//	For IN or NOT IN
		if (operatorValue.equals(Filter.IN)
				|| operatorValue.equals(Filter.NOT_IN)) {
			StringBuilder parameterValues = new StringBuilder();
			final String baseColumnName = columnName;
			StringBuilder column_name = new StringBuilder(columnName);

			condition.getValues().forEach(currentValue -> {
				boolean isString = DisplayType.isText(displayType) || currentValue instanceof String;

				if (currentValue == null || (isString && Util.isEmpty((String) currentValue, true))) {
					if (Util.isEmpty(additionalSQL.toString(), true)) {
						additionalSQL.append("(SELECT " + baseColumnName + " WHERE " + baseColumnName + " IS NULL)");
					}
					if (isString) {
						currentValue = "";
					} else {
						// does not add the null value to the filters, another restriction is
						// added only for null values `additionalSQL`.
						return;
					}
				}
				if (parameterValues.length() > 0) {
					parameterValues.append(", ");
				}

				Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
					displayType,
					currentValue
				);
				String dbValue = ParameterUtil.getDBValue(
					valueToFilter,
					displayType
				);
				String sqlInValue = dbValue;
				if (isString) {
					column_name.delete(0, column_name.length());
					column_name.append("UPPER(").append(baseColumnName).append(")");
					sqlInValue = "UPPER(" + dbValue + ")";
				}
				parameterValues.append(sqlInValue);
			});

			columnName = column_name.toString();
			if (!Util.isEmpty(parameterValues.toString(), true)) {
				sqlValue = "(" + parameterValues.toString() + ")";
				if (!Util.isEmpty(additionalSQL.toString(), true)) {
					additionalSQL.insert(0, " OR " + columnName + sqlOperator);
				}
			}
		} else if(operatorValue.equals(Filter.BETWEEN) || operatorValue.equals(Filter.NOT_BETWEEN)) {
			Object valueStartToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getFromValue()
			);
			Object valueEndToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getToValue()
			);

			String dbValueStart = ParameterUtil.getDBValue(valueStartToFilter, displayType);
			String dbValueEnd = ParameterUtil.getDBValue(valueEndToFilter, displayType);

			sqlValue = "";
			if (valueStartToFilter == null) {
				sqlValue = dbValueEnd;
				sqlOperator = OperatorUtil.convertOperator(Filter.LESS_EQUAL);
			} else if (valueEndToFilter == null) {
				sqlValue = dbValueStart;
				sqlOperator = OperatorUtil.convertOperator(Filter.GREATER_EQUAL);
			} else {
				sqlValue = dbValueStart + " AND " + dbValueEnd;
			}
		} else if(operatorValue.equals(Filter.LIKE) || operatorValue.equals(Filter.NOT_LIKE)) {
			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);
			String dbValue = ParameterUtil.getDBValue(
				valueToFilter,
				displayType
			);

			columnName = "UPPER(" + columnName + ")";
			sqlValue = "'%' || UPPER(" + dbValue + ") || '%'";
		} else if(operatorValue.equals(Filter.NULL) || operatorValue.equals(Filter.NOT_NULL)) {
			;
		} else if (operatorValue.equals(Filter.EQUAL) || operatorValue.equals(Filter.NOT_EQUAL)) {
			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);

			String dbValue = ParameterUtil.getDBValue(
				valueToFilter,
				displayType
			);
			sqlValue = dbValue;

			boolean isString = DisplayType.isText(displayType) || valueToFilter instanceof String;
			boolean isEmptyString = isString && Util.isEmpty((String) valueToFilter, true);
			if (isString) {
				if (isEmptyString) {
					valueToFilter = "";
				} else {
					columnName = "UPPER(" + columnName + ")";
					sqlValue = "UPPER(" + dbValue + ")";
				}
			}
			if (valueToFilter == null || isEmptyString) {
				additionalSQL.append(" OR ")
					.append(columnName)
					.append(" IS NULL ")
				;
			}
		} else {
			// Greater, Greater Equal, Less, Less Equal
			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);
			sqlValue = ParameterUtil.getDBValue(
				valueToFilter,
				displayType
			);
		}

		String rescriction = "(" + columnName + sqlOperator + sqlValue + additionalSQL.toString() + ")";

		return rescriction;
	}


	/**
	 * Get Where Clause from criteria and dynamic condition
	 * @param {Criteria} criteria
	 * @param {List<Object>} params
	 * @return
	 */
	public static String getWhereClauseFromCriteria(String filters, List<Object> params) {
		return getWhereClauseFromCriteria(filters, null, params);
	}

	/**
	 * Get Where Clause from criteria and dynamic condition
	 * @param {Criteria} criteria
	 * @param {String} tableName
	 * @param {List<Object>} params
	 * @return
	 */
	public static String getWhereClauseFromCriteria(String filters, String tableName, List<Object> params) {
		return getWhereClauseFromCriteria(filters, tableName, null, params);
	}

	/**
	 * Get Where Clause from criteria and dynamic condition
	 * @param {Criteria} criteria
	 * @param {String} tableName
	 * @param {String} tableAlias
	 * @param {List<Object>} params
	 * @return
	 */
	public static String getWhereClauseFromCriteria(String filters, String tableName, String tableAlias, List<Object> params) {
		StringBuffer whereClause = new StringBuffer();
		// Vaidate and Table
		final MTable table = RecordUtil.validateAndGetTable(tableName);
		if (Util.isEmpty(tableAlias, true)) {
			tableAlias = tableName;
		}
		final String tableNameAlias = tableAlias;
		FilterManager.newInstance(filters).getConditions().stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
				int displayTypeId = 0;
				int columnId = MColumn.getColumn_ID(table.getTableName(), condition.getColumnName());
				if (columnId > 0) {
					MColumn column = MColumn.get(
						Env.getCtx(),
						columnId
					);
					if (column != null) {
						displayTypeId = column.getAD_Reference_ID();
						// set table alias to column name
						String columnName = tableNameAlias + "." + condition.getColumnName();
						condition.setColumnName(columnName);
					}
				}

				String restriction = WhereClauseUtil.getRestrictionByOperator(condition, displayTypeId, params);

				whereClause.append(restriction);
		});
		//	Return where clause
		return whereClause.toString();
	}



	/**
	 * Get Where Clause from Tab
	 * @param tabId
	 * @return
	 */
	public static String getWhereClauseFromTab(int tabId) {
		MTab tab = MTab.get(Env.getCtx(), tabId);
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			return null;
		}
		return getWhereClauseFromTab(tab.getAD_Window_ID(), tabId);
	}

	public static String getWhereClauseFromTab(int windowId, int tabId) {
		MTab aspTab = ASPUtil.getInstance(Env.getCtx()).getWindowTab(windowId, tabId);
		if (aspTab == null || aspTab.getAD_Tab_ID() <= 0) {
			return null;
		}
		return aspTab.getWhereClause();
	}

	/**
	 * Get SQL Where Clause including link column and parent column
	 * @param {Properties} context
	 * @param {MTab} tab
	 * @param {List<MTab>} tabs
	 * @return {String}
	 */
	public static String getTabWhereClauseFromParentTabs(Properties context, MTab tab, List<MTab> tabs) {
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

			List<MTab> parentTabsList = WindowUtil.getParentTabsList(tab.getAD_Window_ID(), tabId, new ArrayList<MTab>());
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

				if (table.getColumn(childColumn) != null) {
					whereClause.append(table.getTableName()).append(".").append(childColumn);
					if (mainColumnName != null && mainColumnName.endsWith("_ID")) {
						whereClause.append(" = ").append("@").append(mainColumnName).append("@");
					} else {
						whereClause.append(" = ").append("'@").append(mainColumnName).append("@'");
					}
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
						String childColumnName = WindowUtil.getParentColumnNameFromTab(childTab);
						String childLinkColumnName = WindowUtil.getLinkColumnNameFromTab(childTab);
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
				String parentColumnName = WindowUtil.getParentColumnNameFromTab(tab);
				String linkColumnName = WindowUtil.getLinkColumnNameFromTab(tab);
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
		final String whereTab = WhereClauseUtil.getWhereClauseFromTab(tab.getAD_Window_ID(), tabId);
		if (!Util.isEmpty(whereTab, true)) {
			String whereWithAlias = WhereClauseUtil.getWhereRestrictionsWithAlias(
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

	public static String getWhereClauseFromKeyColumns(String[] keyColumns) {
		String whereClause = "";
		if (keyColumns != null && keyColumns.length > 0) {
			for (String columnName: keyColumns) {
				if (!Util.isEmpty(whereClause, true)) {
					whereClause += " AND ";
				}
				whereClause += columnName + " = ? ";
			}
		}

		return whereClause;
	}

	/**
	 * Get Where clause for Smart Browse by Criteria Conditions
	 * 
	 * @param criteria
	 * @param browseId
	 * @param filterValues
	 * @return
	 */
	public static String getBrowserWhereClauseFromCriteria(int browseId, String criteria, List<Object> filterValues) {
		if (browseId <= 0) {
			return null;
		}
		MBrowse browse = ASPUtil.getInstance().getBrowse(browseId);
		if (browse == null || browse.getAD_Browse_ID() <= 0) {
			return null;
		}
		return getBrowserWhereClauseFromCriteria(browse, criteria, filterValues);
	}

	/**
	 * Get Where clause for Smart Browse by Criteria Conditions
	 *
	 * @param filters
	 * @param browser
	 * @param filterValues
	 * @return where clasuse with generated restrictions
	 */
	public static String getBrowserWhereClauseFromCriteria(MBrowse browser, String filters, List<Object> filterValues) {
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			return null;
		}
		if (filters == null) {
			return null;
		}
		
		StringBuffer whereClause = new StringBuffer();
		List<Filter> conditions = FilterManager.newInstance(filters).getConditions();
//		if (!Util.isEmpty(filters.getWhereClause(), true)) {
//			whereClause.append("(").append(filters.getWhereClause()).append(")");
//		}
		if (conditions == null || conditions.size() == 0) {
			return whereClause.toString();
		}

 		// Add browse field to map
		List<MBrowseField> browseFieldsList = ASPUtil.getInstance().getBrowseFields(browser.getAD_Browse_ID());
		HashMap<String, MBrowseField> browseFields = new HashMap<>();
		for (MBrowseField browseField : browseFieldsList) {
			browseFields.put(browseField.getAD_View_Column().getColumnName(), browseField);
		}
		HashMap<String, String> rangeAdd = new HashMap<>();
		conditions.stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				String columnName = condition.getColumnName();
				MBrowseField browseField = browseFields.get(columnName);
				if (browseField == null && columnName.endsWith("_To")) {
					String rangeColumnName = columnName.substring(0, columnName.length() - "_To".length());
					browseField = browseFields.get(rangeColumnName);
				}
				if (browseField == null || browseField.isInfoOnly()) {
					return;
				}
				MViewColumn viewColumn = browseField.getAD_View_Column();
				if (rangeAdd.containsKey(viewColumn.getColumnName())) {
					return;
				}
				// overwrite column name on restriction
				columnName = viewColumn.getColumnSQL();
				condition.setColumnName(columnName);
				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
				String restriction = WhereClauseUtil.getRestrictionByOperator(
					condition,
					browseField.getAD_Reference_ID(),
					filterValues
				);
				whereClause.append(restriction);
			});

		return whereClause.toString();
	}



	/**
	 * Get Where clause for View by Criteria Conditions
	 * 
	 * @param filters
	 * @param viewId
	 * @param filterValues
	 * @return
	 */
	public static String getViewWhereClauseFromCriteria(String filters, int viewId, List<Object> filterValues) {
		if (viewId <= 0) {
			return null;
		}
		MView view = new MView(Env.getCtx(), viewId);
		if (view == null || view.getAD_View_ID() <= 0) {
			return null;
		}
		return getViewWhereClauseFromCriteria(filters, view, filterValues);
	}

	/**
	 * Get Where clause for View by Criteria Conditions
	 * 
	 * @param filters
	 * @param view
	 * @param filterValues
	 * @return
	 */
	public static String getViewWhereClauseFromCriteria(String filters, MView view, List<Object> filterValues) {
		if (view == null || view.getAD_View_ID() <= 0) {
			return null;
		}
		if (filters == null) {
			return null;
		}

		StringBuffer whereClause = new StringBuffer();
		List<Filter> conditions = FilterManager.newInstance(filters).getConditions();
//		if (!Util.isEmpty(filters.getWhereClause(), true)) {
//			whereClause.append("(").append(filters.getWhereClause()).append(")");
//		}
		if (conditions == null || conditions.size() == 0) {
			return whereClause.toString();
		}
		// Add view columns to map
		List<MViewColumn> viewColumnsList = view.getViewColumns();
		HashMap<String, MViewColumn> viewColummns = new HashMap<>();
		for (MViewColumn viewColumn : viewColumnsList) {
			viewColummns.put(viewColumn.getColumnName(), viewColumn);
		}

		conditions.stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				MViewColumn viewColumn = viewColummns.get(condition.getColumnName());
				if (viewColumn == null || viewColumn.getAD_View_Column_ID() <= 0) {
					return;
				}
				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
				String restriction = WhereClauseUtil.getRestrictionByOperator(condition, viewColumn.getAD_Reference_ID(), filterValues);
				whereClause.append(restriction);
			});

		return whereClause.toString();
	}

}
