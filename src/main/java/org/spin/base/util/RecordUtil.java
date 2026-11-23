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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Table;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MConversionRate;
import org.compiere.model.MField;
import org.compiere.model.MQuery;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.base.interim.ContextTemporaryWorkaround;
import org.spin.dictionary.util.DictionaryUtil;
import org.spin.service.grpc.util.db.FromUtil;
import org.spin.service.grpc.util.db.OrderByUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for handle records utils values
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class RecordUtil {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(RecordUtil.class);


	/**	Reference cache	*/
	public static CCache<String, MQuery> referenceWhereClauseCache = new CCache<String, MQuery>("Record_Reference_WhereClause", 100, 120);	//	2 hours, it is generated again when not exists

	public static CCache<String, LinkedHashMap<String, MColumn>> columnsMapCache = new CCache<String, LinkedHashMap<String, MColumn>>("Table_Columns_Map", 30, 0);	//	no time-out


	/**
	 * Get conversion Rate from ValidFrom, Currency From, Currency To and Conversion Type
	 * @param request
	 * @return
	 */
	public static MConversionRate getConversionRate(int organizationId, int conversionTypeId, int currencyFromId, int currencyToId, Timestamp conversionDate) {
		if(conversionTypeId <= 0
				|| currencyFromId <= 0
				|| currencyToId <= 0) {
			return null;
		}
		//	Get values
		conversionDate = TimeUtil.getDay(Optional.ofNullable(conversionDate).orElse(new Timestamp(System.currentTimeMillis())));
		if(organizationId < 0) {
			organizationId = 0;
		}
		int conversionRateId = MConversionRate.getConversionRateId(currencyFromId, 
				currencyToId, 
				conversionDate, 
				conversionTypeId, 
				Env.getAD_Client_ID(Env.getCtx()), 
				organizationId);
		if(conversionRateId > 0) {
			return MConversionRate.get(Env.getCtx(), conversionRateId);
		}
		//	
		return null;
	}



	/**
	 * Add where clause with search value and return the new complete SQL
	 * @param sql
	 * @param tableName
	 * @param searchValue
	 * @param parameters
	 * @return
	 */
	public static String addSearchValueAndGet(String sql, String tableName, String searchValue, List<Object> parameters) {
		return addSearchValueAndGet(sql, tableName, null, searchValue, true, parameters);
	}

	/**
	 * Add where clause with search value and return the new complete SQL
	 * @param sql
	 * @param tableName
	 * @param searchValue
	 * @param parameters
	 * @return
	 */
	public static String addSearchValueAndGet(String sql, String tableName, String tableAlias, String searchValue, List<Object> parameters) {
		return addSearchValueAndGet(sql, tableName, tableAlias, searchValue, true, parameters);
	}

	/**
	 * Add where clause with search value and return the new complete SQL
	 * @param sql
	 * @param tableName
	 * @param searchValue
	 * @param isTranslated
	 * @param parameters
	 * @return
	 */
	public static String addSearchValueAndGet(String sql, String tableName, String searchValue, boolean isTranslated, List<Object> parameters) {
		return addSearchValueAndGet(sql, tableName, null, searchValue, isTranslated, parameters);
	}

	/**
	 * Add where clause with search value and return the new complete SQL
	 * @param sql
	 * @param tableName
	 * @param tableAlias
	 * @param searchValue
	 * @param isTranslated
	 * @param parameters
	 * @return
	 */
	public static String addSearchValueAndGet(String sql, String table_name, String table_alias, String search_value, boolean isTranslated, List<Object> parameters) {
		if(Util.isEmpty(search_value, true)) {
			return sql;
		}
		MTable table = MTable.get(Env.getCtx(), table_name);
		if(table == null || table.getAD_Table_ID() <= 0) {
			return sql;
		}

		// URL decode to change characteres and add search value to filter
		final String searchValue = TextManager.getValidString(
			TextManager.getDecodeUrl(
				search_value
			)
		).strip();

		String lang = Env.getAD_Language(Env.getCtx());
		// search on trl table
		final MTable tableTranslation = org.compiere.util.Language.isBaseLanguage(lang) || !isTranslated ?
			null : MTable.get(Env.getCtx(), table_name + DictionaryUtil.TRANSLATION_SUFFIX);
		final String tableAlias = Util.isEmpty(table_alias, true) ? table.getTableName() : table_alias;

		List<MColumn> selectionColums = table.getColumnsAsList().parallelStream()
			.filter(column -> {
				return (column.isIdentifier() || column.isSelectionColumn()
					|| column.getColumnName().equals("Name")
					|| column.getColumnName().equals("Value")
					|| column.getColumnName().equals("DocumentNo"))
					&& Util.isEmpty(column.getColumnSQL(), true)
					&& DisplayType.isText(column.getAD_Reference_ID());
			})
			.collect(Collectors.toList());

		// TODO: Add 1=1 to remove `if (where.length() > 0)` and change stream with parallelStream
		StringBuffer where = new StringBuffer();
		selectionColums.stream()
			.filter(column -> {
				// "Value" is not translated for example
				return (tableTranslation == null || (tableTranslation != null && !column.isTranslated()));
			})
			.forEach(column -> {
				if(where.length() > 0) {
					where.append(" OR ");
				}
				where.append("UPPER(")
					.append(tableAlias).append(".")
					.append(column.getColumnName())
					.append(")")
					.append(" LIKE ")
					.append("'%' || UPPER(?) || '%'");
				parameters.add(searchValue);
			});

		// Add language clause
		StringBuffer whereClause = where;
		String joinTranslation = "";
		if (tableTranslation != null) {
			// TODO: Add 1=1 to remove `if (whereTranslation.length() > 0)` and change stream with parallelStream
			StringBuffer whereTranslation = new StringBuffer();
			
			selectionColums.stream()
				.filter(column -> {
					// Name is translated for example
					return tableTranslation != null && column.isTranslated();
				})
				.forEach(column -> {
					if(whereTranslation.length() > 0) {
						whereTranslation.append(" OR ");
					}
					whereTranslation.append("UPPER(")
						.append("NVL(")
						.append(tableTranslation.getTableName()).append(".")
						.append(column.getColumnName())
						.append(", ")
						.append(tableAlias).append(".")
						.append(column.getColumnName())
						.append("))")
						.append(" LIKE ")
						.append("'%'|| UPPER(?) || '%'");
					parameters.add(searchValue);
				});

			if (!Util.isEmpty(whereTranslation.toString(), true)) {
				if (!Util.isEmpty(where.toString(), true)) {
					whereClause.append(" OR ");
				}
				whereClause
					.append("((")
					.append(whereTranslation)
					.append(") AND ")
					.append(tableTranslation.getTableName() + ".AD_Language = ? ")
					// .append(" AND ")
					// .append(tableTranslation.getTableName() + ".IsTranslated = 'Y' ")
					.append(")")
				;
				parameters.add(lang);
	
				String keyColumn = table.getTableName() + "_ID";
				String translationTableName = tableTranslation.getTableName();
				// only table, tableName tableName, tableName AS tableName
				String tableWithAliases = FromUtil.getPatternTableName(translationTableName, null);

				String joinPattern = "JOIN(\\s+)" 
					+ "(" + tableWithAliases + ")"
					+ "(\\s+ON(\\s*\\(){0,1})\\s*"
					// base_table.table_id = translation_table.table_id
					+ "((\\w+." + keyColumn + "\\s*=\\s*" + translationTableName + "." + keyColumn + ")"
					// translation_table.table_id = base_table.table_id
					+ "|(" + translationTableName + "." + keyColumn + "\\s*=\\s*\\w+." + keyColumn +"))"
					;
				Matcher matcherJoinTranslated = Pattern.compile(
					joinPattern,
					Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE
				).matcher(sql);
				// Add tranlated join
				if (!matcherJoinTranslated.find()) {
					joinTranslation = " LEFT JOIN " + translationTableName + " AS " + translationTableName
						+ " ON " + translationTableName + "." + keyColumn + " = " + tableAlias + "." + keyColumn + " "
					;
				}
			}
		}
		
		//	Validate and return
		if(whereClause.length() > 0) {
			//	Order by
			Matcher matcherOrderBy = OrderByUtil.SQL_ORDER_BY_PATTERN
				.matcher(sql)
			;
			int positionOrderBy = -1;
			String orderByClause = "";
			String queryWithoutOrderBy = sql;
			if (matcherOrderBy.find()) {
				positionOrderBy = matcherOrderBy.start();
				orderByClause = sql.substring(positionOrderBy);
				// remove order By
				queryWithoutOrderBy = sql.substring(0, positionOrderBy);
			}

			// tableName tableName, tableName AS tableName
			String patternAlias = FromUtil.getPatternTableName(table.getTableName(), tableAlias);

			String fromWherePattern = "\\s+(FROM)\\s+(" + patternAlias + ")" + "\\s+(WHERE)";
			Matcher matcher = Pattern.compile(
				fromWherePattern,
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			)
			.matcher(queryWithoutOrderBy);
			Matcher matcherJoin = Pattern.compile(
				"JOIN(\\w|\\s)*(\\((\\w|\\.|\\s|=|\\')*\\))(\\s*)(WHERE)",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			).matcher(queryWithoutOrderBy);

			String conditional = " WHERE ";
			List<MatchResult> fromWhereParts = matcher.results()
					.collect(Collectors.toList());
			List<MatchResult> joinWhereParts = matcherJoin.results()
					.collect(Collectors.toList());
			
			String onlyQuery = queryWithoutOrderBy;
			if (joinWhereParts != null && joinWhereParts.size() > 0) {
				MatchResult lastJoinWhere = joinWhereParts.get(joinWhereParts.size() - 1);
				onlyQuery = queryWithoutOrderBy.substring(0, lastJoinWhere.end());

				int lastWhere = onlyQuery.lastIndexOf("WHERE");
				onlyQuery = onlyQuery.substring(0, lastWhere);

				conditional = queryWithoutOrderBy.substring(lastWhere, queryWithoutOrderBy.length()) + " AND ";
			} else if (fromWhereParts != null && fromWhereParts.size() > 0) {
				MatchResult lastJoinWhere = fromWhereParts.get(fromWhereParts.size() - 1);
				onlyQuery = queryWithoutOrderBy.substring(0, lastJoinWhere.end());

				int lastWhere = onlyQuery.lastIndexOf("WHERE");
				onlyQuery = onlyQuery.substring(0, lastWhere);
				
				conditional = queryWithoutOrderBy.substring(lastWhere, queryWithoutOrderBy.length()) + " AND ";
			}

			sql = onlyQuery + joinTranslation + conditional + "(" + whereClause + ")" + orderByClause;
		}
		return sql;
	}


	/**
	 * Get the display type of each field that overwrites the reference of its column
	 * (AD_Field.AD_Reference_ID), keyed by upper case column name
	 * @param tab window tab, can be null
	 * @return map, empty without tab or overwrites
	 */
	public static Map<String, Integer> getFieldDisplayTypes(MTab tab) {
		Map<String, Integer> fieldDisplayTypes = new HashMap<>();
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			return fieldDisplayTypes;
		}
		for (MField field : tab.getFields(false, null)) {
			if (field == null || field.getAD_Reference_ID() <= 0) {
				continue;
			}
			MColumn column = MColumn.get(tab.getCtx(), field.getAD_Column_ID());
			if (column == null || column.getAD_Column_ID() <= 0) {
				continue;
			}
			fieldDisplayTypes.put(
				column.getColumnName().toUpperCase(),
				field.getAD_Reference_ID()
			);
		}
		return fieldDisplayTypes;
	}


	public static Entity.Builder convertResultSetToStruct(MTable table, ResultSet resultSet) throws SQLException {
		return convertResultSetToStruct(table, resultSet, null);
	}

	/**
	 * Convert the current row to entity
	 * @param table
	 * @param resultSet
	 * @param fieldDisplayTypes display type overwritten by the window fields
	 * (see {@link #getFieldDisplayTypes(MTab)}), used for the display values; can be null
	 * @return entity builder
	 * @throws SQLException
	 */
	public static Entity.Builder convertResultSetToStruct(MTable table, ResultSet resultSet, Map<String, Integer> fieldDisplayTypes) throws SQLException {
		Entity.Builder entityBuilder = Entity.newBuilder()
			.setTableName(
				table.getTableName()
			)
		;
		Struct.Builder rowValues = Struct.newBuilder();

		LinkedHashMap<String, MColumn> columnsMap = org.spin.base.util.RecordUtil.columnsMapCache.get(table.getTableName());
		if (columnsMap ==  null || columnsMap.isEmpty()) {
			columnsMap = new LinkedHashMap<>();
			//	Add field to map
			for(MColumn column: table.getColumnsAsList()) {
				columnsMap.put(
					column.getColumnName().toUpperCase(),
					column
				);
			}
		}

		ResultSetMetaData metaData = resultSet.getMetaData();
		for (int index = 1; index <= metaData.getColumnCount(); index++) {
			try {
				final String columnNameMetadata = metaData.getColumnName(index);
				MColumn column = columnsMap.get(columnNameMetadata.toUpperCase());
				//	Display Columns
				if(column == null) {
					final String displayValue = resultSet.getString(index);
					Value.Builder displayValueBuilder = TextManager.getProtoValueFromString(displayValue);

					rowValues.putFields(
						columnNameMetadata,
						displayValueBuilder.build()
					);
					continue;
				}
				final int displayTypeId = column.getAD_Reference_ID();
				// display as the window field shows it, when it overwrites the column reference
				int displayValueTypeId = displayTypeId;
				if (fieldDisplayTypes != null && fieldDisplayTypes.containsKey(columnNameMetadata.toUpperCase())) {
					displayValueTypeId = fieldDisplayTypes.get(columnNameMetadata.toUpperCase());
				}
				if (column.isKey()) {
					final int identifier = resultSet.getInt(index);
					entityBuilder.setId(identifier);
				}
				if (I_AD_Element.COLUMNNAME_UUID.toLowerCase().equals(columnNameMetadata.toLowerCase())) {
					final String uuid = resultSet.getString(index);
					entityBuilder.setUuid(
						TextManager.getValidString(uuid)
					);
				}
				//	From Coumn
				final String columnName = column.getColumnName();
				Object value = resultSet.getObject(index);
				Value.Builder valueBuilder = ValueManager.getProtoValueFromObject(
					value,
					displayTypeId
				);
				rowValues.putFields(
					columnName,
					valueBuilder.build()
				);

				// Add custom display values
				if (DisplayType.isDate(displayValueTypeId)) {
					final String displayValue = TimeManager.getDisplayValue(
						value,
						displayValueTypeId
					);
					Value.Builder displayValueBuilder = TextManager.getProtoValueFromString(
						displayValue
					);
					rowValues.putFields(
						LookupUtil.DISPLAY_COLUMN_KEY + "_" + columnName,
						displayValueBuilder.build()
					);
				} else if (DisplayType.isNumeric(displayValueTypeId)) {
					if (ReferenceUtil.REFERENCES_CURRENCY.contains(displayValueTypeId)) {
						// final String displayValue = NumberManager.getDisplayValueWithCurrencyFromObject(
						// 	value,
						// 	displayValueTypeId,
						// 	recordCurrencyId
						// );
						// Value.Builder displayValueBuilder = TextManager.getProtoValueFromString(
						// 	displayValue
						// );
						// rowValues.putFields(
						// 	LookupUtil.DISPLAY_COLUMN_KEY + "_" + columnName,
						// 	displayValueBuilder.build()
						// );
					} else if (ReferenceUtil.REFERENCES_DECIMALS.contains(displayValueTypeId)) {
						final String displayValue = NumberManager.getDisplayValue(
							value,
							displayValueTypeId
						);
						Value.Builder displayValueBuilder = TextManager.getProtoValueFromString(
							displayValue
						);
						rowValues.putFields(
							LookupUtil.DISPLAY_COLUMN_KEY + "_" + columnName,
							displayValueBuilder.build()
						);
					}
				} else if (DisplayType.YesNo == displayValueTypeId) {
					final String displayValue = BooleanManager.getDisplayValue(
						value
					);
					Value.Builder displayValueBuilder = TextManager.getProtoValueFromString(
						displayValue
					);
					rowValues.putFields(
						LookupUtil.DISPLAY_COLUMN_KEY + "_" + columnName,
						displayValueBuilder.build()
					);
				}

				// to add client uuid by record
				if (columnName.equals(I_AD_Element.COLUMNNAME_AD_Client_ID)) {
					final int clientId = NumberManager.getIntegerFromObject(value);
					MClient clientEntity = MClient.get(
						table.getCtx(),
						clientId
					);
					if (clientEntity != null) {
						final String clientUuid = clientEntity.get_UUID();
						Value.Builder valueUuidBuilder = TextManager.getProtoValueFromString(
							clientUuid
						);
						rowValues.putFields(
							LookupUtil.getUuidColumnName(
								I_AD_Element.COLUMNNAME_AD_Client_ID
							),
							valueUuidBuilder.build()
						);
					}
				} else if (columnName.equals(I_AD_ChangeLog.COLUMNNAME_Record_ID)) {
					if (resultSet.getInt(I_AD_Table.COLUMNNAME_AD_Table_ID) > 0) {
						MTable tableRow = MTable.get(table.getCtx(), resultSet.getInt(I_AD_Table.COLUMNNAME_AD_Table_ID));
						if (tableRow != null) {
							PO entityRow = tableRow.getPO(resultSet.getInt(I_AD_ChangeLog.COLUMNNAME_Record_ID), null);
							if (entityRow != null) {
								final String recordIdDisplayValue = entityRow.getDisplayValue();
								Value.Builder recordIdDisplayBuilder = TextManager.getProtoValueFromString(
									recordIdDisplayValue
								);
								rowValues.putFields(
									LookupUtil.getDisplayColumnName(
										I_AD_ChangeLog.COLUMNNAME_Record_ID
									),
									recordIdDisplayBuilder.build()
								);
							}

						}
					}
				}
			} catch (Exception e) {
				log.warning(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}

		// TODO: Temporary Workaround
		rowValues = ContextTemporaryWorkaround.setContextAsUnknowColumn(
			table.getTableName(),
			rowValues
		);
		//
		entityBuilder.setValues(rowValues);

		// record identifier as root display value
		final String recordDisplayValue = getRecordDisplayValue(
			table,
			columnsMap,
			rowValues
		);
		entityBuilder.setDisplayValue(
			TextManager.getValidString(recordDisplayValue)
		);

		return entityBuilder;
	}



	/**
	 * Get the record display value from the values already loaded on the row,
	 * without additional queries: first the display column of the key column
	 * (identifier columns embedded by the lookup), otherwise the identifier
	 * columns sorted by sequence and joined with ` - `
	 * @param table
	 * @param columnsMap
	 * @param rowValues
	 * @return display value, or null without identifier values
	 */
	private static String getRecordDisplayValue(MTable table, LinkedHashMap<String, MColumn> columnsMap, Struct.Builder rowValues) {
		final String[] keyColumns = table.getKeyColumns();
		if (keyColumns != null && keyColumns.length == 1) {
			final String keyDisplayValue = getStringFromValue(
				rowValues.getFieldsMap().get(
					LookupUtil.getDisplayColumnName(keyColumns[0])
				)
			);
			if (!Util.isEmpty(keyDisplayValue, true)) {
				return keyDisplayValue;
			}
		}

		final String identifier = columnsMap.values().stream()
			.filter(column -> column.isIdentifier())
			.sorted(Comparator.comparing(MColumn::getSeqNo))
			.map(column -> {
				final String columnName = column.getColumnName();
				// formatted value (lookup, date, number, yes/no)
				String displayValue = getStringFromValue(
					rowValues.getFieldsMap().get(
						LookupUtil.getDisplayColumnName(columnName)
					)
				);
				if (Util.isEmpty(displayValue, true)) {
					displayValue = getStringFromValue(
						rowValues.getFieldsMap().get(columnName)
					);
				}
				return displayValue;
			})
			.filter(displayValue -> !Util.isEmpty(displayValue, true))
			.collect(Collectors.joining(" - "));
		if (Util.isEmpty(identifier, true)) {
			return null;
		}
		return identifier;
	}

	/**
	 * Get plain string from a proto value: string, number, boolean or
	 * the `value` field of a typed struct (date, decimal)
	 * @param value
	 * @return string or null
	 */
	private static String getStringFromValue(Value value) {
		if (value == null) {
			return null;
		}
		switch (value.getKindCase()) {
			case STRING_VALUE:
				return value.getStringValue();
			case NUMBER_VALUE:
				final double numberValue = value.getNumberValue();
				if (numberValue == Math.rint(numberValue) && !Double.isInfinite(numberValue)) {
					return String.valueOf((long) numberValue);
				}
				return String.valueOf(numberValue);
			case BOOL_VALUE:
				return BooleanManager.getDisplayValue(
					value.getBoolValue()
				);
			case STRUCT_VALUE:
				return getStringFromValue(
					value.getStructValue().getFieldsMap().get("value")
				);
			default:
				return null;
		}
	}



	/**
	 * Convert Entities List
	 * @param table
	 * @param sql
	 * @param params
	 * @return
	 */
	public static ListEntitiesResponse.Builder convertListEntitiesResult(MTable table, String sql, List<Object> params) {
		return convertListEntitiesResult(table, sql, params, null);
	}

	/**
	 * Convert Entities List with the display types of the window tab fields
	 * @param table
	 * @param sql
	 * @param params
	 * @param tab window tab whose fields can overwrite the column reference, can be null
	 * @return
	 */
	public static ListEntitiesResponse.Builder convertListEntitiesResult(MTable table, String sql, List<Object> params, MTab tab) {
		final Map<String, Integer> fieldDisplayTypes = getFieldDisplayTypes(tab);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		long recordCount = 0;
		try {
			LinkedHashMap<String, MColumn> columnsMap = columnsMapCache.get(table.getTableName());
			if (columnsMap ==  null || columnsMap.isEmpty()) {
				columnsMap = new LinkedHashMap<>();
				//	Add field to map
				for(MColumn column: table.getColumnsAsList()) {
					columnsMap.put(column.getColumnName().toUpperCase(), column);
				}
			}
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, params);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				Entity.Builder entityBuilder = org.spin.base.util.RecordUtil.convertResultSetToStruct(
					table,
					rs,
					fieldDisplayTypes
				);
				//	
				builder.addRecords(entityBuilder.build());
				recordCount++;
			}
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
		}
		//	Set record counts
		builder.setRecordCount(recordCount);
		//	Return
		return builder;
	}

}
