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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
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
import org.spin.backend.grpc.common.Criteria;
import org.spin.backend.grpc.common.KeyValue;
import org.spin.backend.grpc.common.Operator;
import org.spin.backend.grpc.common.Value;
import org.spin.base.dictionary.WindowUtil;
import org.spin.base.util.ValueUtil;
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
	public static String getRestrictionByOperator(
		String columnName, int operatorValue,
		Value value, Value valueTo, List<Value> valuesList, List<Object> params
	) {
		String rescriction = "";
		String sqlOperator = OperatorUtil.convertOperator(operatorValue);

		String sqlValue = "";
		//	For IN or NOT IN
		if (operatorValue == Operator.IN_VALUE
				|| operatorValue == Operator.NOT_IN_VALUE) {
			StringBuffer parameterValues = new StringBuffer();
			valuesList.forEach(itemValue -> {
				if (parameterValues.length() > 0) {
					parameterValues.append(", ");
				}
				String sqlItemValue = "?";
				parameterValues.append(sqlItemValue);
				params.add(ValueUtil.getObjectFromValue(itemValue));
			});
			sqlValue = "(" + parameterValues.toString() + ")";
			// if (valuesList.size() <= 0 && value.getValueType() != ValueType.UNKNOWN) {
			// 	sqlValue = "(?)";
			// 	params.add(
			// 		ValueUtil.getObjectFromValue(value)
			// 	);
			// }
		} else if(operatorValue == Operator.BETWEEN_VALUE || operatorValue == Operator.NOT_BETWEEN_VALUE) {
			sqlValue = " ? AND ? ";
			params.add(
				ValueUtil.getObjectFromValue(value)
			);
			params.add(
				ValueUtil.getObjectFromValue(valueTo)
			);
		} else if(operatorValue == Operator.LIKE_VALUE || operatorValue == Operator.NOT_LIKE_VALUE) {
			columnName = "UPPER(" + columnName + ")";
			String parameterValue = ValueUtil.validateNull(
				(String) ValueUtil.getObjectFromValue(value, true)
			);
			// if (!Util.isEmpty(parameterValue, true)) {
			// 	if (!parameterValue.startsWith("%")) {
			// 		parameterValue = "%" + parameterValue;
			// 	}
			// 	if (!parameterValue.endsWith("%")) {
			// 		parameterValue += "%";
			// 	}
			// }
			// parameterValue = "UPPPER(" + parameterValue + ")";
			sqlValue = "'%' || UPPER(?) || '%'";
			params.add(parameterValue);
		} else if(operatorValue == Operator.NULL_VALUE || operatorValue == Operator.NOT_NULL_VALUE) {
			;
		} else {
			// Equal, Not Equal, Greater, Greater Equal, Less, Less Equal
			Object parameterValue = ValueUtil.getObjectFromValue(
				value
			);
			sqlValue = " ? ";
			if (parameterValue instanceof String) {
				columnName = "UPPER(" + columnName + ")";
				sqlValue = "UPPER(?)";
			}
			params.add(parameterValue);
		}
		rescriction = "(" + columnName + sqlOperator + sqlValue + ")";

		return rescriction;
	}



	/**
	 * Get Where Clause from criteria and dynamic condition
	 * @param {Criteria} criteria
	 * @param {List<Object>} params
	 * @return
	 */
	public static String getWhereClauseFromCriteria(Criteria criteria, List<Object> params) {
		return getWhereClauseFromCriteria(criteria, null, params);
	}

	/**
	 * Get Where Clause from criteria and dynamic condition
	 * @param {Criteria} criteria
	 * @param {String} tableName
	 * @param {List<Object>} params
	 * @return
	 */
	public static String getWhereClauseFromCriteria(Criteria criteria, String tableName, List<Object> params) {
		return getWhereClauseFromCriteria(criteria, tableName, null, params);
	}

	/**
	 * Get Where Clause from criteria and dynamic condition
	 * @param {Criteria} criteria
	 * @param {String} tableName
	 * @param {String} tableAlias
	 * @param {List<Object>} params
	 * @return
	 */
	public static String getWhereClauseFromCriteria(Criteria criteria, String tableName, String tableAlias, List<Object> params) {
		StringBuffer whereClause = new StringBuffer();
		if (!Util.isEmpty(criteria.getWhereClause(), true)) {
			whereClause.append("(").append(criteria.getWhereClause()).append(")");
		}
		if (Util.isEmpty(tableName, true)) {
			tableName = criteria.getTableName();
		}
		final MTable table = MTable.get(Env.getCtx(), tableName);
		//	Validate
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		if (Util.isEmpty(tableAlias, true)) {
			tableAlias = tableName;
		}
		final String tableNameAlias = tableAlias;

		criteria.getConditionsList().stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				int operatorValue = condition.getOperatorValue();
				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
				String columnName = tableNameAlias + "." + condition.getColumnName();
				if (operatorValue < 0 || operatorValue == Operator.VOID_VALUE) {
					operatorValue = OperatorUtil.getDefaultOperatorByConditionValue(
						condition
					);
				}

				String restriction = WhereClauseUtil.getRestrictionByOperator(
					columnName,
					operatorValue,
					condition.getValue(),
					condition.getValueTo(),
					condition.getValuesList(),
					params
				);

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




	/**
	 * Get Where clause for Smart Browse
 	 * @deprecated use {@link #getBrowserWhereClauseFromCriteria} instead
	 * @param browser
	 * @param parsedWhereClause
	 * @param values
	 * @return
	 */
	public static String getBrowserWhereClause(MBrowse browser, String parsedWhereClause,
			List<KeyValue> contextAttributes, HashMap<String, Object> parameterMap, List<Object> values) {
		AtomicReference<String> convertedWhereClause = new AtomicReference<String>(parsedWhereClause);
		if (!Util.isEmpty(parsedWhereClause, true) && contextAttributes != null && contextAttributes.size() > 0) {
			contextAttributes.forEach(contextValue -> {
				String value = String.valueOf(ValueUtil.getObjectFromValue(contextValue.getValue()));
				String contextKey = "@" + contextValue.getKey() + "@";
				convertedWhereClause.set(
						convertedWhereClause.get().replaceAll(contextKey, value));
			});
		}

		// Add field to map
		List<MBrowseField> fields = ASPUtil.getInstance().getBrowseFields(browser.getAD_Browse_ID());
		HashMap<String, MBrowseField> fieldsMap = new HashMap<>();
		for (MBrowseField field : fields) {
			fieldsMap.put(field.getAD_View_Column().getColumnName(), field);
		}

		//
		StringBuilder browserWhereClause = new StringBuilder();
		boolean onRange = false;
		if (parameterMap != null && parameterMap.size() > 0) {
			for (Entry<String, Object> parameter : parameterMap.entrySet()) {
				MBrowseField field = fieldsMap.get(parameter.getKey());
				if (field == null) {
					continue;
				}
				String columnName = field.getAD_View_Column().getColumnSQL();
				Object parameterValue = parameter.getValue();
				if (!onRange) {
					if (parameterValue != null && !field.isRange()) {
						if (browserWhereClause.length() > 0) {
							browserWhereClause.append(" AND ");
						}
						if (DisplayType.String == field.getAD_Reference_ID()) {
							String value = (String) parameterValue;
							if (value.contains(",")) {
								value = value.replace(" ", "");
								String inStr = new String(value);
								StringBuffer outStr = new StringBuffer("(");
								int i = inStr.indexOf(',');
								while (i != -1) {
									outStr.append("'" + inStr.substring(0, i) + "',");
									inStr = inStr.substring(i + 1, inStr.length());
									i = inStr.indexOf(',');

								}
								outStr.append("'" + inStr + "')");
								//
								browserWhereClause.append(columnName).append(" IN ")
										.append(outStr);
							} else if (value.contains("%")) {
								browserWhereClause.append(" lower( ").append(columnName).append(") LIKE ? ");
								values.add(parameterValue.toString().toLowerCase());
							} else {
								browserWhereClause.append(" lower( ").append(columnName).append(") = ? ");
								values.add(parameterValue.toString().toLowerCase());
							}
						} else {
							browserWhereClause.append(columnName).append("=? ");
							values.add(parameterValue);
						}
					} else if (parameterValue != null && field.isRange()) {
						if (browserWhereClause.length() > 0) {
							browserWhereClause.append(" AND ");
						}
						if (DisplayType.String == field.getAD_Reference_ID()) {
							browserWhereClause.append(" lower( ").append(columnName).append(") >= ? ");
							values.add(parameterValue.toString().toLowerCase());
						} else {
							browserWhereClause.append(columnName).append(" >= ? ");
							values.add(parameterValue);
						}
						onRange = true;
					} else if (parameterValue == null && field.isRange()) {
						onRange = true;
					} else
						continue;
				} else if (parameterValue != null) {
					if (browserWhereClause.length() > 0) {
						browserWhereClause.append(" AND ");
					}
					if (DisplayType.String == field.getAD_Reference_ID()) {
						browserWhereClause.append(" lower( ").append(columnName).append(") <= ? ");
						values.add(parameterValue.toString().toLowerCase());
					} else {
						browserWhereClause.append(columnName).append(" <= ? ");
						values.add(parameterValue);
					}
					onRange = false;
				} else {
					onRange = false;
				}
			}
		}
		//
		String whereClause = null;
		//
		if (!Util.isEmpty(convertedWhereClause.get(), true)) {
			whereClause = convertedWhereClause.get();
		}
		if (browserWhereClause.length() > 0) {
			if (Util.isEmpty(whereClause, true)) {
				whereClause = " (" + browserWhereClause.toString() + ") ";
			} else {
				whereClause = " (" + whereClause + ") AND (" + browserWhereClause + ") ";
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
	public static String getBrowserWhereClauseFromCriteria(int browseId, Criteria criteria, List<Object> filterValues) {
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
	 * @param criteria
	 * @param browser
	 * @param filterValues
	 * @return where clasuse with generated restrictions
	 */
	public static String getBrowserWhereClauseFromCriteria(MBrowse browser, Criteria criteria, List<Object> filterValues) {
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			return null;
		}
		if (criteria == null) {
			return null;
		}

		StringBuffer whereClause = new StringBuffer();
		if (!Util.isEmpty(criteria.getWhereClause(), true)) {
			whereClause.append("(").append(criteria.getWhereClause()).append(")");
		}
		if (criteria.getConditionsList() == null || criteria.getConditionsList().size() <= 0) {
			return whereClause.toString();
		}

 		// Add browse field to map
		List<MBrowseField> browseFieldsList = ASPUtil.getInstance().getBrowseFields(browser.getAD_Browse_ID());
		HashMap<String, MBrowseField> browseFields = new HashMap<>();
		for (MBrowseField browseField : browseFieldsList) {
			browseFields.put(browseField.getAD_View_Column().getColumnName(), browseField);
		}

		criteria.getConditionsList().stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				MBrowseField browseField = browseFields.get(condition.getColumnName());
				MViewColumn viewColumn = browseField.getAD_View_Column();

				int operatorValue = condition.getOperatorValue();
				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}

				String columnName = viewColumn.getColumnSQL();
				if (operatorValue < 0 || operatorValue == Operator.VOID_VALUE) {
					operatorValue = OperatorUtil.getDefaultOperatorByConditionValue(
						condition
					);
				}

				String restriction = WhereClauseUtil.getRestrictionByOperator(
					columnName,
					operatorValue,
					condition.getValue(),
					condition.getValueTo(),
					condition.getValuesList(),
					filterValues
				);

				whereClause.append(restriction);
			});

		return whereClause.toString();
	}



	/**
	 * Get Where clause for View by Criteria Conditions
	 * 
	 * @param criteria
	 * @param viewId
	 * @param filterValues
	 * @return
	 */
	public static String getViewWhereClauseFromCriteria(Criteria criteria, int viewId, List<Object> filterValues) {
		if (viewId <= 0) {
			return null;
		}
		MView view = new MView(Env.getCtx(), viewId);
		if (view == null || view.getAD_View_ID() <= 0) {
			return null;
		}
		return getViewWhereClauseFromCriteria(criteria, view, filterValues);
	}

	/**
	 * Get Where clause for View by Criteria Conditions
	 * 
	 * @param criteria
	 * @param view
	 * @param filterValues
	 * @return
	 */
	public static String getViewWhereClauseFromCriteria(Criteria criteria, MView view, List<Object> filterValues) {
		if (view == null || view.getAD_View_ID() <= 0) {
			return null;
		}
		if (criteria == null) {
			return null;
		}

		StringBuffer whereClause = new StringBuffer();
		if (!Util.isEmpty(criteria.getWhereClause(), true)) {
			whereClause.append("(").append(criteria.getWhereClause()).append(")");
		}
		if (criteria.getConditionsList() == null || criteria.getConditionsList().size() <= 0) {
			return whereClause.toString();
		}

		// Add view columns to map
		List<MViewColumn> viewColumnsList = view.getViewColumns();
		HashMap<String, MViewColumn> viewColummns = new HashMap<>();
		for (MViewColumn viewColumn : viewColumnsList) {
			viewColummns.put(viewColumn.getColumnName(), viewColumn);
		}

		criteria.getConditionsList().stream()
			.filter(condition -> !Util.isEmpty(condition.getColumnName(), true))
			.forEach(condition -> {
				MViewColumn viewColumn = viewColummns.get(condition.getColumnName());
				if (viewColumn == null || viewColumn.getAD_View_Column_ID() <= 0) {
					return;
				}

				int operatorValue = condition.getOperatorValue();
				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}

				String columnName = viewColumn.getColumnSQL();
				if (operatorValue < 0 || operatorValue == Operator.VOID_VALUE) {
					operatorValue = OperatorUtil.getDefaultOperatorByConditionValue(
						condition
					);
				}

				String restriction = WhereClauseUtil.getRestrictionByOperator(
					columnName,
					operatorValue,
					condition.getValue(),
					condition.getValueTo(),
					condition.getValuesList(),
					filterValues
				);

				whereClause.append(restriction);
			});

		return whereClause.toString();
	}

}
