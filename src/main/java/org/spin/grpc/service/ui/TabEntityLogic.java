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

package org.spin.grpc.service.ui;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.X_AD_Window;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MMenu;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.POAdapter;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Evaluator;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.user_interface.CreateTabEntityRequest;
import org.spin.backend.grpc.user_interface.GetTabEntityRequest;
import org.spin.backend.grpc.user_interface.ListTabEntitiesRequest;
import org.spin.backend.grpc.user_interface.UpdateTabEntityRequest;
import org.spin.base.db.OrderByUtil;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.interim.ContextTemporaryWorkaround;
import org.spin.base.util.ContextManager;
import org.spin.base.util.LookupUtil;
import org.spin.dictionary.util.DictionaryUtil;
import org.spin.grpc.service.UserInterface;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.query.SortingManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class TabEntityLogic {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(TabEntityLogic.class);


	public static Entity.Builder createTabEntity(CreateTabEntityRequest request) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		MTab tab = MTab.get(Env.getCtx(), request.getTabId());
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException(
				"@AD_Tab_ID@ " + request.getTabId() + " @NotFound@"
			);
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		PO currentEntity = table.getPO(0, null);
		if (currentEntity == null) {
			throw new AdempiereException("@Error@ @PO@ @NotFound@");
		}
		POAdapter adapter = new POAdapter(currentEntity);

		Map<String, Value> attributes = new HashMap<>(request.getAttributes().getFieldsMap());
		attributes.entrySet().stream().forEach(attribute -> {
			final String columnName = attribute.getKey();
			if (Util.isEmpty(columnName, true) || columnName.startsWith(LookupUtil.DISPLAY_COLUMN_KEY) || columnName.endsWith("_" + LookupUtil.UUID_COLUMN_KEY)) {
				return;
			}
			MColumn column = table.getColumn(columnName);
			if (column == null || column.getAD_Column_ID() <= 0) {
				// checks if the column exists in the database
				return;
			}
			int referenceId = column.getAD_Reference_ID();
			Object value = null;
			if (referenceId > 0) {
				value = ValueManager.getObjectFromProtoValue(
					attribute.getValue(),
					referenceId
				);
			} 
			if (value == null) {
				value = ValueManager.getObjectFromProtoValue(
					attribute.getValue()
				);
			}
			// entity.set_ValueOfColumn(columnName, value);
			adapter.set_ValueNoCheck(columnName, value);
		});
		//	Save entity
		currentEntity.saveEx();

		String[] keyColumns = table.getKeyColumns();
		ArrayList<Object> parametersList = new ArrayList<Object>();
		if (keyColumns.length > 1) {
			parametersList = ParameterUtil.getParametersFromKeyColumns(
				keyColumns,
				attributes
			);
		}

		GetTabEntityRequest.Builder getEntityBuilder = GetTabEntityRequest.newBuilder()
			.setTabId(request.getTabId())
			.setId(currentEntity.get_ID())
		;

		Entity.Builder builder = TabEntityLogic.getTabEntity(
			getEntityBuilder.build(),
			parametersList
		);
		return builder;
	}



	/**
	 * Convert a PO from query
	 * @param request
	 * @return
	 */
	public static Entity.Builder getTabEntity(GetTabEntityRequest request, ArrayList<Object> multiKeys) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		Properties context = Env.getCtx();

		MTab tab = MTab.get(
			context,
			request.getTabId()
		);
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException(
				"@AD_Tab_ID@ " + request.getTabId() + " @NotFound@"
			);
		}

		//	Add to recent Item
		DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Window,
			tab.getAD_Window_ID()
		);

		MTable table = MTable.get(context, tab.getAD_Table_ID());
		final String[] keyColumns = table.getKeyColumns();

		String sql = QueryUtil.getTabQueryWithReferences(
			context,
			tab
		);
		// add filter
		StringBuffer whereClause = new StringBuffer();
		List<Object> filtersList = new ArrayList<Object>();
		if (keyColumns.length == 1) {
			for (final String keyColumnName: keyColumns) {
				MColumn column = table.getColumn(keyColumnName);
				if (DisplayType.isID(column.getAD_Reference_ID())) {
					if (whereClause.length() > 0) {
						whereClause.append(" OR ");
					}
					whereClause.append(
						table.getTableName() + "." + keyColumnName + " = ?"
					);
					filtersList.add(
						request.getId()
					);
				}
			}
		} else {
			final String whereMultiKeys = WhereClauseUtil.getWhereClauseFromKeyColumns(
				table.getTableName(),
				keyColumns
			);
			whereClause.append(whereMultiKeys);
			filtersList.addAll(multiKeys);
		}
		sql += " WHERE " + whereClause.toString();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Entity.Builder entityBuilder = Entity.newBuilder()
			.setTableName(
				table.getTableName()
			)
		;
		CLogger log = CLogger.getCLogger(UserInterface.class);

		try {
			LinkedHashMap<String, MColumn> columnsMap = new LinkedHashMap<String, MColumn>();
			//	Add field to map
			for (MColumn column: table.getColumnsAsList()) {
				columnsMap.put(
					column.getColumnName().toUpperCase(),
					column
				);
			}

			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);

			// add query parameters
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);

			//	Get from Query
			rs = pstmt.executeQuery();
			if (rs.next()) {
				Struct.Builder rowValues = Struct.newBuilder();
				ResultSetMetaData metaData = rs.getMetaData();
				for (int index = 1; index <= metaData.getColumnCount(); index++) {
					try {
						String columnName = metaData.getColumnName(index);
						MColumn column = columnsMap.get(columnName.toUpperCase());
						//	Display Columns
						if(column == null) {
							String displayValue = rs.getString(index);
							Value.Builder displayValueBuilder = TextManager.getProtoValueFromString(displayValue);

							rowValues.putFields(
								columnName,
								displayValueBuilder.build()
							);
							continue;
						}
						if (column.isKey()) {
							final int identifier = rs.getInt(index);
							entityBuilder.setId(identifier);
						}
						if (I_AD_Element.COLUMNNAME_UUID.toLowerCase().equals(columnName.toLowerCase())) {
							final String uuid = rs.getString(index);
							entityBuilder.setUuid(
								TextManager.getValidString(uuid)
							);
						}
						//	From field
						String fieldColumnName = column.getColumnName();
						Object value = rs.getObject(index);
						Value.Builder valueBuilder = ValueManager.getProtoValueFromObject(
							value,
							column.getAD_Reference_ID()
						);
						rowValues.putFields(
							fieldColumnName,
							valueBuilder.build()
						);

						// to add client uuid by record
						if (fieldColumnName.equals(I_AD_Element.COLUMNNAME_AD_Client_ID)) {
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
						} else if (fieldColumnName.equals(I_AD_ChangeLog.COLUMNNAME_Record_ID)) {
							if (rs.getInt(I_AD_Table.COLUMNNAME_AD_Table_ID) > 0) {
								MTable tableRow = MTable.get(table.getCtx(), rs.getInt(I_AD_Table.COLUMNNAME_AD_Table_ID));
								if (tableRow != null) {
									PO entityRow = tableRow.getPO(rs.getInt(I_AD_ChangeLog.COLUMNNAME_Record_ID), null);
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

				entityBuilder.setValues(rowValues);
			}
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		//	Return
		return entityBuilder;
	}



	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	public static ListEntitiesResponse.Builder listTabEntities(ListTabEntitiesRequest request) {
		int tabId = request.getTabId();
		if (tabId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		Properties context = Env.getCtx();

		MTab tab = MTab.get(
			context,
			tabId
		);
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException(
				"@AD_Tab_ID@ " + tabId + " @NotFound@"
			);
		}

		//	Add to recent Item
		DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Window,
			tab.getAD_Window_ID()
		);

		//	
		MTable table = MTable.get(context, tab.getAD_Table_ID());
		String tableName = table.getTableName();

		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(
			windowNo, context, request.getContextAttributes()
		);

		// get where clause including link column and parent column
		String where = WhereClauseUtil.getTabWhereClauseFromParentTabs(context, tab, null);
		String parsedWhereClause = Env.parseContext(context, windowNo, where, false);
		if (Util.isEmpty(parsedWhereClause, true) && !Util.isEmpty(where, true)) {
			throw new AdempiereException(
				"@AD_Tab_ID@ " + tab.getName() + " (" + tab.getAD_Tab_ID() + "), @WhereClause@ @Unparseable@"
			);
		}
		StringBuffer whereClause = new StringBuffer(parsedWhereClause);
		List<Object> params = new ArrayList<>();

		//	For dynamic condition
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(
			request.getFilters(),
			tableName,
			params
		);
		if(!Util.isEmpty(dynamicWhere, true)) {
			if(!Util.isEmpty(whereClause.toString(), true)) {
				whereClause.append(" AND ");
			}
			//	Add
			whereClause.append(dynamicWhere);
		}

		//	Add from reference
		if(!Util.isEmpty(request.getRecordReferenceUuid(), true)) {
			MQuery zoomQuery = org.spin.base.util.RecordUtil.referenceWhereClauseCache.get(
				request.getRecordReferenceUuid()
			);
			//	TODO: When is null refresh cache
			if (zoomQuery != null) {
				final String referenceWhereClause = UserInterfaceLogic.getWhereClauseFromChildTab(
					zoomQuery,
					tab
				);
				if(!Util.isEmpty(referenceWhereClause, true)) {
					String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
						tableName,
						referenceWhereClause
					);
					if(whereClause.length() > 0) {
						whereClause.append(" AND ");
					}
					whereClause
						.append("(")
						.append(validationCode)
						.append(")")
					;
				}
			}
		}

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		//	
		StringBuilder sql = new StringBuilder(
			QueryUtil.getTabQueryWithReferences(
				context,
				tab
			)
		);
		String sqlWithRoleAccess = MRole.getDefault()
			.addAccessSQL(
				sql.toString(),
				tableName,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);
		if (!Util.isEmpty(whereClause.toString(), true)) {
			// includes first AND
			sqlWithRoleAccess += " AND " + whereClause;
		}
		//
		String parsedSQL = org.spin.base.util.RecordUtil.addSearchValueAndGet(sqlWithRoleAccess, tableName, request.getSearchValue(), false, params);

		String orderByClause = "";
		if (!Util.isEmpty(request.getSortBy(), true)) {
			orderByClause = " ORDER BY " + SortingManager.newInstance(request.getSortBy()).getSotingAsSQL();
		} else {
			String tabOrderBy = OrderByUtil.getTabOrderByClause(tab);
			if (!Util.isEmpty(tabOrderBy, true)) {
				String parsedTabOrderBy = Env.parseContext(context, windowNo, tabOrderBy, false);
				if (Util.isEmpty(parsedTabOrderBy, true)) {
					throw new AdempiereException(
						"@AD_Tab_ID@ " + tab.getName() + " (" + tab.getAD_Tab_ID() + "), @OrderByClause@ @Unparseable@"
					);
				}
				orderByClause = " ORDER BY " + parsedTabOrderBy;
			}
		}

		// improves performance in the query
		String countSQL = "SELECT COUNT(*) FROM " + tableName + " AS " + tableName;
		if (!Util.isEmpty(whereClause.toString(), true) && !whereClause.toString().trim().startsWith("WHERE")) {
			countSQL += " WHERE ";
		}
		countSQL += whereClause;
		countSQL = MRole.getDefault()
			.addAccessSQL(
				countSQL,
				tableName,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);
		String parsedCountSQL = org.spin.base.util.RecordUtil.addSearchValueAndGet(countSQL, tableName, request.getSearchValue(), false, new ArrayList<Object>());

		//	Add Row Number
		parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
		//	Add Order By
		parsedSQL = parsedSQL + orderByClause;
		builder = org.spin.base.util.RecordUtil.convertListEntitiesResult(table, parsedSQL, params);

		//	Count records, Set page token
		count = CountUtil.countRecords(parsedCountSQL, tableName, params);
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(nexPageToken)
			)
		;
		//	Return
		return builder;
	}



	public static Entity.Builder updateTabEntity(UpdateTabEntityRequest request) {
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}

		MTab tab = MTab.get(Env.getCtx(), request.getTabId());
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ (" + request.getTabId() + ") @NotFound@");
		}

		MWindow window = MWindow.get(Env.getCtx(), tab.getAD_Window_ID());
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ (" + tab.getAD_Window_ID() + ") @NotFound@");
		}

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		final String[] keyColumns = table.getKeyColumns();
		Map<String, Value> attributes = new HashMap<>(request.getAttributes().getFieldsMap());
		ArrayList<Object> parametersList = new ArrayList<Object>();
		PO entity = null;
		if (keyColumns.length == 1) {
			entity = RecordUtil.getEntity(
				Env.getCtx(),
				table.getTableName(),
				request.getId(),
				null
			);
		} else {
			final String whereClause = WhereClauseUtil.getWhereClauseFromKeyColumns(keyColumns);
			parametersList = ParameterUtil.getParametersFromKeyColumns(
				keyColumns,
				attributes
			);
			entity = new Query(
				Env.getCtx(),
				table.getTableName(),
				whereClause,
				null
			)
				.setParameters(parametersList)
				.first()
			;
		}
		if (entity == null) {
			throw new AdempiereException("@Error@ @PO@ @NotFound@");
		}
		PO currentEntity = entity;
		POAdapter adapter = new POAdapter(currentEntity);

		GetTabEntityRequest.Builder getEntityBuilder = GetTabEntityRequest.newBuilder()
			.setTabId(request.getTabId())
			.setId(entity.get_ID())
		;
		Entity.Builder builder = getTabEntity(
			getEntityBuilder.build(),
			parametersList
		);

		Properties context = Env.getCtx();
		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextFromPO(
			windowNo, context, entity, false
		);

		if (window.getWindowType().equals(X_AD_Window.WINDOWTYPE_QueryOnly)) {
			log.warning("@Ignored@ @AD_Window_ID@ (" + window.getName() + ") : @WindowType@ " + X_AD_Window.WINDOWTYPE_QueryOnly);
			return builder;
		}
		if (table.isView()) {
			log.warning("@Ignored@ @AD_Table_ID@ (" + table.getName() + ") : @IsView@ ");
			return builder;
		}
		if (tab.isReadOnly()) {
			log.warning("@Ignored@ @AD_Tab_ID@ (" + tab.getName() + ") : @IsReadOnly@ ");
			return builder;
		}
		if (!Util.isEmpty(tab.getReadOnlyLogic(), true)) {
			boolean isReadOnlyTab = Evaluator.evaluateLogic(currentEntity, tab.getReadOnlyLogic());
			if (isReadOnlyTab) {
				log.warning("@Ignored@ @AD_Tab_ID@ (" + tab.getName() + ") : @ReadOnlyLogic@ ");
				return builder;
			}
		}

		final int sessionClientId = Env.getAD_Client_ID(context);
		if (sessionClientId != currentEntity.getAD_Client_ID()) {
			log.warning("@Ignored@ : Record is other client");
			return builder;
		}

		// final boolean isActiveRecord = currentEntity.isActive();
		final boolean isProcessedRecord = currentEntity.get_ValueAsBoolean("Processed");
		final boolean isProcessingRecord = currentEntity.get_ValueAsBoolean("Processing");

		attributes.entrySet().forEach(attribute -> {
			final String columnName = attribute.getKey();
			if (Util.isEmpty(columnName, true) || columnName.startsWith(LookupUtil.DISPLAY_COLUMN_KEY) || columnName.endsWith("_" + LookupUtil.UUID_COLUMN_KEY)) {
				return;
			}
			MColumn column = table.getColumn(columnName);
			if (column == null || column.getAD_Column_ID() <= 0) {
				// checks if the column exists in the database
				return;
			}
			final int displayTypeId = column.getAD_Reference_ID();

			String displayValue = ValueManager.getDisplayedValueFromReference(
				context,
				attribute.getValue(),
				columnName,
				displayTypeId,
				column.getAD_Reference_Value_ID()
			);

			if (Arrays.stream(keyColumns).anyMatch(columnName::equals)) {
				// prevent warning `PO.set_Value: Column not updateable`
				log.warning(
					Msg.parseTranslation(
						context,
						"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @IsKey@ "
					)
				);
				return;
			}

			if (!column.isAlwaysUpdateable()) {
				if (!column.isUpdateable()) {
					log.warning(
						Msg.parseTranslation(
							context,
							"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @IsUpdateable@ @NotValid@"
						)
					);
					return;
				}

				if (isProcessedRecord) {
					log.warning(
						Msg.parseTranslation(
							context,
							"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @Processed@ "
						)
					);
					return;
				}
				if (isProcessingRecord) {
					log.warning(
						Msg.parseTranslation(
							context,
							"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @Processing@ "
						)
					);
					return;
				}

				// if (!Util.isEmpty(column.getReadOnlyLogic(), true)) {
				// 	boolean isReadOnlyColumn = Evaluator.evaluateLogic(currentEntity, column.getReadOnlyLogic());
				// 	if (isReadOnlyColumn) {
				// 		log.warning(
				// 			Msg.parseTranslation(
				// 				context,
				// 				"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @ReadOnlyLogic@ "
				// 			)
				// 		);
				// 		return;
				// 	}
				// }

				// if (!columnName.equals(I_AD_Element.COLUMNNAME_IsActive)) {
				// 	if (!isActiveRecord) {
				// 		log.warning(
				// 			Msg.parseTranslation(
				// 				context,
				// 				"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @NotActive@ "
				// 			)
				// 		);
				// 		return;
				// 	}
				// }

				// MField field = new Query(
				// 	Env.getCtx(),
				// 	I_AD_Field.Table_Name,
				// 	I_AD_Field.COLUMNNAME_AD_Column_ID + " = ? AND " + I_AD_Field.COLUMNNAME_AD_Tab_ID + " = ?",
				// 	null
				// )
				// 	.setParameters(column.getAD_Column_ID(), tab.getAD_Tab_ID())
				// 	.setOnlyActiveRecords(true)
				// 	.first()
				// ;
				// if (field != null) {
				// 	if (field.isReadOnly()) {
				// 		log.warning(
				// 			Msg.parseTranslation(
				// 				context,
				// 				"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @IsReadOnly@ "
				// 			)
				// 		);
				// 		return;
				// 	}
				// }
			}

			Object value = null;
			if (!attribute.getValue().hasNullValue()) {
				if (displayTypeId > 0) {
					value = ValueManager.getObjectFromProtoValue(
						attribute.getValue(),
						displayTypeId
					);
				} 
				if (value == null) {
					value = ValueManager.getObjectFromProtoValue(
						attribute.getValue()
					);
				}
			}
			adapter.set_ValueNoCheck(columnName, value);
		});
		//	Save entity
		currentEntity.saveEx();

		// Reload entity
		builder = getTabEntity(
			getEntityBuilder.build(),
			parametersList
		);

		return builder;
	}

}
