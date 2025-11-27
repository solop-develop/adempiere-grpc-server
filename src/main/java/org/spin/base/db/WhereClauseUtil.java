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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.MatchResult;
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
import org.compiere.model.MWindow;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.dictionary.util.WindowUtil;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.db.FromUtil;
import org.spin.service.grpc.util.db.OperatorUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.TextManager;

/**
 * Class for handle SQL Where Clause
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class WhereClauseUtil {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(WhereClauseUtil.class);


	/**
	 * Add and get table alias to columns in validation code sql
	* @param tableName table name
	* @param tableAlias table alias to main table name
	* @param dynamicValidation sql with restriction
	* @return {String}
	 */
	public static String getWhereRestrictionsWithAlias(String tableName, String tableAlias, String dynamicValidation) {
		String validationCode = getWhereRestrictionsWithAlias(tableAlias, dynamicValidation);
		if (Util.isEmpty(validationCode, true)) {
			return "";
		}
		if (tableName.equals(tableAlias)) {
			// ignore replace primary table with alias
			return validationCode;
		}
		final String regexPrimarayTable = "\\b" + tableName + "\\.";
		String validationWithAlias = validationCode.replaceAll(regexPrimarayTable, tableAlias + ".");
		if (Util.isEmpty(validationWithAlias, true)) {
			return "";
		}
		return validationWithAlias;
	}

	/**
	 * Add and get table alias to columns in validation code sql
	 * @param tableAlias table name or table alias
	 * @param dynamicValidation sql with restriction
	 * @return {String}
	 */
	public static String getWhereRestrictionsWithAlias(String tableAlias, String dynamicValidation) {
		if (Util.isEmpty(dynamicValidation, true)) {
			return "";
		}

		// Check if the table alias is already present in the validation
		Matcher matcherTableAliases = Pattern.compile(
				"\\b" + tableAlias + "\\.", // We search for the alias followed by a period.
				Pattern.CASE_INSENSITIVE // | Pattern.DOTALL
			)
			.matcher(dynamicValidation);

		String validationCode = dynamicValidation;

		// If the alias in the main table already exists, we do nothing.
		if (!matcherTableAliases.find()) {
			// Regular expression to identify table aliases in subqueries
			final String tableAliasRegex = "\\b(?:FROM|JOIN)\\s+(\\w+)\\s+(?:AS\\s+)?(\\w+)\\b";

			// Regular expression to identify columns without aliases. Captures any word (column) that does NOT:
			// - be immediately followed by a period (avoids already qualified columns)
			// - be immediately preceded by a period (avoids being the table/alias of a qualified column)
			// - be an SQL keyword (e.g., JOIN, IN, etc.)
			// - is inside a subquery (this is the most complex and is done with the logic of the original code)
			// final String columnsRegex = "\\b(?<!\\.\\b)(?![\\w]+\\.)(\\w+)(\\s*[=><!]|\\s*\\b(?:IN|NOT\\s+IN|LIKE|IS\\s+NULL|IS\\s+NOT\\s+NULL|BETWEEN|NOT\\s+BETWEEN)\\b)";
			// final String columnsRegex = "\\b(?![\\w.]+\\.)(?<![\\w\\s]+(\\.\\w+))(?<!\\w\\.)(?!(?:JOIN|ORDER\\s+BY|DISTINCT|NOT\\s+IN|IN|NOT\\s+BETWEEN|BETWEEN|NOT\\s+LIKE|LIKE|IS\\s+NULL|IS\\s+NOT\\s+NULL)\\b)(\\w+)(\\s*)";
			final String columnsRegex = 
				"\\b(?<!\\.)(?<!\\w\\.)" + // Lookbehind: Not preceded by . or word.
				"(?!\\w+\\.)" + // Lookahead: Not followed by a word and a period (prevents matching 'C_Invoice' in 'C_Invoice.DocStatus')
				"(?!\\b(?:FROM|JOIN|ORDER\\s+BY|DISTINCT|NOT\\s+IN|IN|NOT\\s+BETWEEN|BETWEEN|NOT\\s+LIKE|LIKE|IS\\s+NULL|IS\\s+NOT\\s+NULL)\\b)" + // Lookahead: Not an SQL keyword.
				"(\\w+)" + // Group 1: The column name
				"(\\s*)" + // Group 2: Optional whitespace
				"(?=" + OperatorUtil.SQL_OPERATORS_REGEX + ")" // Lookahead: Must be followed by an operator.
			;

			// Regular expression to sql operators
			// final String sqlOperatorsRegex = OperatorUtil.SQL_OPERATORS_REGEX;

			// Compile regular expressions
			Pattern patternTableAlias = Pattern.compile(
				tableAliasRegex,
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			);
			Pattern patternColumnName = Pattern.compile(
				columnsRegex, // + sqlOperatorsRegex,
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			);

			// Identify and store subquery aliases to avoid prefixing them
			Matcher matchTableAlias = patternTableAlias.matcher(validationCode);
			Set<String> tableAliases = new HashSet<>();
			while (matchTableAlias.find()) {
				// Store the table aliases found
				// group(2) is the alias of the table (e.g., ‘i’, ‘il’, ‘ol’)
				tableAliases.add(matchTableAlias.group(2));
			}

			// Replace columns that do not have aliases and are not aliases of tables in subqueries
			Matcher matchColumnName = patternColumnName.matcher(validationCode);
			StringBuffer sb = new StringBuffer();
			while (matchColumnName.find()) {
				String columnName = matchColumnName.group(1); // The column without an alias (e.g., C_Order_ID)
				String operatorOrSpace = matchColumnName.group(2); // The operator or space that follows

				if (columnName != null) {
					// Check if the column name is a subquery alias
					// You can also add a list of additional SQL keywords here if needed
					if (!tableAliases.contains(columnName)) {
						// If it is NOT a subquery alias, add the alias from the main table
						// We use the full expression to handle case sensitivity safely
						matchColumnName.appendReplacement(sb, tableAlias + "." + columnName + operatorOrSpace);
					} else {
						// If it is an alias (such as ‘i’, ‘il’, etc.), leave it unchanged.
						matchColumnName.appendReplacement(sb, columnName + operatorOrSpace);
					}
				}
			}
			matchColumnName.appendTail(sb);
			validationCode = sb.toString();
		}

		return validationCode;
	}


	/**
	 * Add and get sql with active records
	 * @param tableAlias
	 * @param dynamicValidation
	 * @return {String}
	 */
	public static String addIsActiveRestriction(String tableAlias, String sql) {
		//	Order by
		String queryWithoutOrderBy = org.spin.service.grpc.util.db.OrderByUtil.removeOrderBy(sql);
		String orderByClause = org.spin.service.grpc.util.db.OrderByUtil.getOnlyOrderBy(sql);


		String tableWithAliases = FromUtil.getPatternTableName(tableAlias, null);
		String regex = "\\s+(FROM)\\s+(" + tableWithAliases + ")\\s+(WHERE\\b)";

		Pattern pattern = Pattern.compile(
			regex,
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		);
		Matcher matcherFrom = pattern
			.matcher(sql);
		List<MatchResult> fromWhereParts = matcherFrom.results()
			.collect(
				Collectors.toList()
			)
		;

		StringBuffer whereClause = new StringBuffer();
		if (fromWhereParts != null && fromWhereParts.size() > 0) {
			whereClause.append(" AND ");
		} else {
			whereClause.append(" WHERE ");
		}

		if (!Util.isEmpty(tableAlias, true)) {
			whereClause.append(" " + tableAlias + ".");
		}
		whereClause.append("IsActive = 'Y' ");


		String sqlWithActiveRecords = queryWithoutOrderBy + whereClause.toString() + orderByClause;

		return sqlWithActiveRecords;
	}

	/**
	 * Add and get sql with active records
	 * @param tableAlias
	 * @param dynamicValidation
	 * @return {String}
	 */
	public static String removeIsActiveRestriction(String tableAlias, String sql) {
		// String SQL_WHERE_REGEX = "(WHERE|(AND|OR))(\\s+(" + tableAlias + ".IsActive|IsActive)\\s*=\\s*'(Y|N)')";
		String SQL_WHERE_REGEX = "(" + tableAlias + ".IsActive|IsActive)\\s*=\\s*'(Y|N)'";

		String sqlWithoutRestriction = sql;
		// remove order by clause
		Pattern patternWhere = Pattern.compile(
			SQL_WHERE_REGEX,
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		);	
		Matcher matcherWhere = patternWhere
			.matcher(sql);
		if(matcherWhere.find()) {
			int startPosition = matcherWhere.start();
			String initialPart = sql.substring(0, startPosition);
			int endPosition = matcherWhere.end();
			String finalPart = sql.substring(endPosition, sql.length());
			sqlWithoutRestriction = initialPart + " 1=1 " + finalPart;
		}

		return sqlWithoutRestriction;
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
		return getRestrictionByOperator(null, condition, 0, params);
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
	public static String getRestrictionByOperator(Filter condition, int displayType, List<Object> params) {
		return getRestrictionByOperator(null, condition, displayType, params);
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
	public static String getRestrictionByOperator(String tableAlias, Filter condition, List<Object> params) {
		return getRestrictionByOperator(tableAlias, condition, 0, params);
	}

	/**
	 * Get sql restriction by operator
	 * @param condition
	 * @param displayType
	 * @param parameters
	 * @return
	 */
	public static String getRestrictionByOperator(String tableAlias, Filter condition, int displayType, List<Object> parameters) {
		String operatorValue = OperatorUtil.EQUAL;
		if (!Util.isEmpty(condition.getOperator(), true)) {
			operatorValue = condition.getOperator().toLowerCase();
		}
		String sqlOperator = OperatorUtil.convertOperator(condition.getOperator());

		String columnName = condition.getColumnName();
		if (!Util.isEmpty(tableAlias, true)) {
			columnName = tableAlias + "." + columnName;
		}
		String sqlValue = "";
		StringBuilder additionalSQL = new StringBuilder();
		//	For IN or NOT IN
		if (operatorValue.equals(OperatorUtil.IN) || operatorValue.equals(OperatorUtil.NOT_IN)) {
			StringBuilder parameterValues = new StringBuilder();
			final String baseColumnName = columnName;
			StringBuilder column_name = new StringBuilder(columnName);

			if (condition.getValues() != null) {
				condition.getValues().forEach(currentValue -> {
					Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
						displayType,
						currentValue
					);
					boolean isString = DisplayType.isText(displayType) || valueToFilter instanceof String;
					boolean isEmptyString = isString && Util.isEmpty(TextManager.getStringFromObject(currentValue), true);
					if (currentValue == null || isEmptyString) {
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
					parameters.add(valueToFilter);
				});
			}

			columnName = column_name.toString();
			if (!Util.isEmpty(parameterValues.toString(), true)) {
				sqlValue = "(" + parameterValues.toString() + ")";
				if (!Util.isEmpty(additionalSQL.toString(), true)) {
					additionalSQL.insert(0, " OR " + columnName + sqlOperator);
				}
			}
		} else if(operatorValue.equals(OperatorUtil.BETWEEN) || operatorValue.equals(OperatorUtil.NOT_BETWEEN)) {
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
				sqlOperator = OperatorUtil.convertOperator(OperatorUtil.LESS_EQUAL);
				parameters.add(valueEndToFilter);
			} else if (valueEndToFilter == null) {
				sqlValue = " ? ";
				sqlOperator = OperatorUtil.convertOperator(OperatorUtil.GREATER_EQUAL);
				parameters.add(valueStartToFilter);
			} else {
				sqlValue = " ? AND ? ";
				parameters.add(valueStartToFilter);
				parameters.add(valueEndToFilter);
			}
		} else if(operatorValue.equals(OperatorUtil.LIKE) || operatorValue.equals(OperatorUtil.NOT_LIKE)) {
			columnName = "UPPER(" + columnName + ")";
			String valueToFilter = TextManager.getValidString(
				TextManager.getStringFromObject(
					condition.getValue()
				)
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
		} else if(operatorValue.equals(OperatorUtil.NULL) || operatorValue.equals(OperatorUtil.NOT_NULL)) {
			;
		} else if (operatorValue.equals(OperatorUtil.EQUAL) || operatorValue.equals(OperatorUtil.NOT_EQUAL)) {
			Object parameterValue = condition.getValue();
			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				parameterValue
			);
			sqlValue = " ? ";

			boolean isString = DisplayType.isText(displayType) || valueToFilter instanceof String;
			boolean isEmptyString = isString && Util.isEmpty(TextManager.getStringFromObject(parameterValue), true);
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
	 * Get sql restriction by operator
	 * @param columnName
	 * @param operatorValue
	 * @param value
	 * @param valueTo
	 * @param valuesList
	 * @return
	 */
	public static String getRestrictionByOperatorWithoutParameters(Filter condition, int displayType) {
		return getRestrictionByOperatorWithoutParameters(null, condition, displayType);
	}

	/**
	 * Get sql restriction by operator without manage filters
	 * @param columnName
	 * @param operatorValue
	 * @param value
	 * @param valueTo
	 * @param valuesList
	 * @return
	 */
	public static String getRestrictionByOperatorWithoutParameters(String tableAlias, Filter condition, int displayType) {
		String sqlOperator = OperatorUtil.convertOperator(condition.getOperator());

		String columnName = condition.getColumnName();
		if (!Util.isEmpty(tableAlias, true)) {
			columnName = tableAlias + "." + columnName;
		}

		String operatorValue = condition.getOperator();
		String sqlValue = "";
		StringBuilder additionalSQL = new StringBuilder();
		//	For IN or NOT IN
		if (operatorValue.equals(OperatorUtil.IN) || operatorValue.equals(OperatorUtil.NOT_IN)) {
			StringBuilder parameterValues = new StringBuilder();
			final String baseColumnName = columnName;
			StringBuilder column_name = new StringBuilder(columnName);

			condition.getValues().forEach(currentValue -> {
				boolean isString = DisplayType.isText(displayType) || currentValue instanceof String;
				boolean isEmptyString = isString && Util.isEmpty(TextManager.getStringFromObject(currentValue), true);
				if (currentValue == null || isEmptyString) {
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

			// TODO: Date support with `TRUNC(columnName, 'DD')`
			columnName = column_name.toString();
			if (!Util.isEmpty(parameterValues.toString(), true)) {
				sqlValue = "(" + parameterValues.toString() + ")";
				if (!Util.isEmpty(additionalSQL.toString(), true)) {
					additionalSQL.insert(0, " OR " + columnName + sqlOperator);
				}
			}
		} else if(operatorValue.equals(OperatorUtil.BETWEEN) || operatorValue.equals(OperatorUtil.NOT_BETWEEN)) {
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
				sqlOperator = OperatorUtil.convertOperator(OperatorUtil.LESS_EQUAL);
			} else if (valueEndToFilter == null) {
				sqlValue = dbValueStart;
				sqlOperator = OperatorUtil.convertOperator(OperatorUtil.GREATER_EQUAL);
			} else {
				sqlValue = dbValueStart + " AND " + dbValueEnd;
			}
			// TODO: Date support with `TRUNC(columnName, 'DD')`
		} else if(operatorValue.equals(OperatorUtil.LIKE) || operatorValue.equals(OperatorUtil.NOT_LIKE)) {
			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);
			String dbValue = ParameterUtil.getDBValue(
				valueToFilter,
				displayType
			);

			// TODO: Date support with `TRUNC(columnName, 'DD')`
			columnName = "UPPER(" + columnName + ")";
			sqlValue = "'%' || UPPER(" + dbValue + ") || '%'";
		} else if(operatorValue.equals(OperatorUtil.NULL) || operatorValue.equals(OperatorUtil.NOT_NULL)) {
			;
		} else if (operatorValue.equals(OperatorUtil.EQUAL) || operatorValue.equals(OperatorUtil.NOT_EQUAL)) {
			Object valueToFilter = ParameterUtil.getValueToFilterRestriction(
				displayType,
				condition.getValue()
			);

			String dbValue = ParameterUtil.getDBValue(
				valueToFilter,
				displayType
			);
			sqlValue = dbValue;

			// TODO: Date support with `TRUNC(columnName, 'DD')`
			boolean isString = DisplayType.isText(displayType) || valueToFilter instanceof String;
			boolean isEmptyString = isString && Util.isEmpty(TextManager.getStringFromObject(valueToFilter), true);
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
		// Vaidate and get table
		final MTable table = RecordUtil.validateAndGetTable(tableName);
		if (Util.isEmpty(tableAlias, true)) {
			tableAlias = tableName;
		}
		final String tableNameAlias = tableAlias;
		String whereClause = FilterManager.newInstance(filters).getConditions()
			.stream()
			.filter(condition -> {
				return !Util.isEmpty(condition.getColumnName(), true);
			})
			.map(condition -> {
				// TODO: Validate range columns `_To`
				MColumn column = table.getColumn(condition.getColumnName());
				if (column == null || column.getAD_Column_ID() <= 0) {
					// filter key does not exist as a column, next loop
					return null;
				}
				int displayTypeId = column.getAD_Reference_ID();
				// set table alias to column name
				// TODO: Evaluate support to columnSQL
				String columnName = tableNameAlias + "." + column.getColumnName();
				condition.setColumnName(columnName);
				String restriction = WhereClauseUtil.getRestrictionByOperator(
					condition,
					displayTypeId,
					params
				);
				return restriction;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.joining(" AND "))
		;

		if (Util.isEmpty(whereClause, true)) {
			return "";
		}
		//	Return where clause
		return " ( " + whereClause + " ) ";
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
		MTab aspTab = MTab.get(Env.getCtx(), tabId);
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
			MWindow window = MWindow.get(tab.getCtx(), tab.getAD_Window_ID());
			tabs = Arrays.asList(
				window.getTabs(false, null)
			)
				.stream()
				.filter(currentTab -> {
					return currentTab.isActive();
				})
				.collect(Collectors.toList())
			;
		}

		StringBuffer whereClause = new StringBuffer();
		String parentTabUuid = null;
		MTable table = MTable.get(context, tab.getAD_Table_ID());

		int tabId = tab.getAD_Tab_ID();
		int seqNo = tab.getSeqNo();
		int tabLevel = tab.getTabLevel();
		//	Create where clause for children
		if (tab.getTabLevel() > 0 && tabs != null) {
			Optional<MTab> optionalTab = tabs.parallelStream()
				.filter(parentTab -> {
					return parentTab.getAD_Tab_ID() != tabId
						&& parentTab.getTabLevel() == 0;
				})
				.findFirst();
			String mainColumnName = null;
			MTable mainTable = null;
			if(optionalTab.isPresent()) {
				mainTable = MTable.get(context, optionalTab.get().getAD_Table_ID());
				String[] keyColumns = mainTable.getKeyColumns();
				if (keyColumns.length > 0) {
					// get first
					mainColumnName = keyColumns[0];
				}
			}

			List<MTab> parentTabsList = WindowUtil.getParentTabsList(tab.getAD_Window_ID(), tabId, new ArrayList<MTab>());
			List<MTab> tabList = parentTabsList.stream()
				.filter(parentTab -> {
					return parentTab.isActive()
						&& parentTab.getAD_Tab_ID() != tabId
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
					// mainColumnName = childColumn;
				}

				if (table.getColumn(childColumn) != null) {
					whereClause.append(table.getTableName()).append(".").append(childColumn);
					if (mainColumnName != null && mainColumnName.endsWith("_ID")) {
						whereClause.append(" = ").append("@").append(mainColumnName).append("@");
					} else {
						whereClause.append(" = ").append("'@").append(childColumn).append("@'");
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
		String whereClause = getWhereClauseFromKeyColumns(null, keyColumns);

		return whereClause;
	}
	public static String getWhereClauseFromKeyColumns(String tableAlias, String[] keyColumns) {
		String whereClause = "";
		if (keyColumns != null && keyColumns.length > 0) {
			for (String columnName: keyColumns) {
				if (!Util.isEmpty(whereClause, true)) {
					whereClause += " AND ";
				}
				if (!Util.isEmpty(tableAlias, true)) {
					whereClause += tableAlias + ".";
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
		MBrowse browse = MBrowse.get(Env.getCtx(), browseId);
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
		// TODO: Add 1=1 to remove `if (whereClause.length() > 0)` and change stream with parallelStream
		StringBuffer whereClause = new StringBuffer();
		List<Filter> conditions = FilterManager.newInstance(filters).getConditions();
		// if (!Util.isEmpty(filters.getWhereClause(), true)) {
		// 	whereClause.append("(").append(filters.getWhereClause()).append(")");
		// }
		if (conditions == null || conditions.size() == 0) {
			return whereClause.toString();
		}

 		// Add browse field to map
		List<MBrowseField> browseFieldsList = browser.getFields();
		HashMap<String, MBrowseField> browseFields = new HashMap<>();
		for (MBrowseField browseField : browseFieldsList) {
			String viewColumnName = browseField.getAD_View_Column().getColumnName();
			if (browseFields.containsKey(viewColumnName)) {
				MBrowseField existsBrowseField = browseFields.get(viewColumnName);
				log.warning(
					"@ColumnName@: " + viewColumnName + ", @AlreadyExists@. @AD_Browse_Field@ "
					+ existsBrowseField.getName() + " (ID: " + existsBrowseField.getAD_Browse_Field_ID() + ") / "
					+ browseField.getName() + " (ID: " + browseField.getAD_Browse_Field_ID() + ")"
				);
			}
			browseFields.put(viewColumnName, browseField);
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

		// TODO: Add 1=1 to remove `if (whereClause.length() > 0)` and change stream with parallelStream
		StringBuffer whereClause = new StringBuffer();
		List<Filter> conditions = FilterManager.newInstance(filters).getConditions();
		// if (!Util.isEmpty(filters.getWhereClause(), true)) {
		// 	whereClause.append("(").append(filters.getWhereClause()).append(")");
		// }
		if (conditions == null || conditions.size() == 0) {
			return whereClause.toString();
		}
		// Add view columns to map
		List<MViewColumn> viewColumnsList = view.getViewColumns();
		HashMap<String, MViewColumn> viewColummns = new HashMap<>();
		for (MViewColumn viewColumn : viewColumnsList) {
			String viewColumnName = viewColumn.getColumnName();
			if (viewColummns.containsKey(viewColumnName)) {
				MViewColumn existsViewColumn = viewColummns.get(viewColumnName);
				log.warning(
					"@ColumnName@: " + viewColumnName + ", @AlreadyExists@. @AD_Browse_Field@ "
					+ existsViewColumn.getName() + " (ID: " + existsViewColumn.getAD_View_Column_ID() + ") / "
					+ viewColumn.getName() + " (ID: " + viewColumn.getAD_View_Column_ID() + ")"
				);
			}
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
