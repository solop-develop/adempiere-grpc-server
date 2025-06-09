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
 * Copyright (C) 2018-2024 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.grpc.service.ui;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MView;
import org.adempiere.model.MViewColumn;
import org.adempiere.model.MViewDefinition;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.KeyValueSelection;
import org.spin.backend.grpc.user_interface.ExportBrowserItemsRequest;
import org.spin.backend.grpc.user_interface.ExportBrowserItemsResponse;
import org.spin.backend.grpc.user_interface.ListBrowserItemsRequest;
import org.spin.backend.grpc.user_interface.ListBrowserItemsResponse;
import org.spin.base.db.OrderByUtil;
import org.spin.base.db.QueryUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.AccessUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.dictionary.util.DictionaryUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.query.FilterManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * This class was created for add all logic methods for User Interface service
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com , https://github.com/EdwinBetanc0urt
 */
public class BrowserLogic {

	private static CLogger log = CLogger.getCLogger(BrowserLogic.class);

	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	public static ListBrowserItemsResponse.Builder listBrowserItems(ListBrowserItemsRequest request) {
		ListBrowserItemsResponse.Builder builder = ListBrowserItemsResponse.newBuilder();
		Properties context = Env.getCtx();
		MBrowse browser = MBrowse.get(
			context,
			request.getId()
		);
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			return builder;
		}

		//	Add to recent Item
		DictionaryUtil.addToRecentItem(
			MMenu.ACTION_SmartBrowse,
			browser.getAD_Browse_ID()
		);

		HashMap<String, Object> parameterMap = new HashMap<>();
		HashMap<String, String> parameterOperator = new HashMap<>();
		//	Populate map
		FilterManager.newInstance(request.getFilters()).getConditions()
			.stream()
			.forEach(condition -> {
				parameterOperator.put(condition.getColumnName(), condition.getOperator());
				parameterMap.put(condition.getColumnName(), condition.getValue());
			});

		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, context, request.getContextAttributes());
		ContextManager.setContextWithAttributes(windowNo, context, parameterMap, false);
		MView view = browser.getAD_View();

		// Run search process
		StringBuffer processInstanceWhere = new StringBuffer();
		if (browser.get_ColumnIndex("SearchProcess_ID") >= 0 && browser.get_ValueAsInt("SearchProcess_ID") > 0) {
			int searchProcessId = browser.get_ValueAsInt("SearchProcess_ID");
			// Get Instance of process
			MProcess process = MProcess.get(
				context,
				searchProcessId
			);
			if (process == null || process.getAD_Process_ID() <= 0) {
				throw new AdempiereException("@SearchProcess_ID@ @NotFound@");
			}

			// Record/Role access
			boolean isWithAccess = AccessUtil.isProcessAccess(process.getAD_Process_ID());
			if (!isWithAccess) {
				throw new AdempiereException("@SearchProcess_ID@ @AccessCannotProcess@");
			}
			String viewProcessInstanceColumnSql = "";
			List<MViewColumn> viewColumns = view.getViewColumns();
			HashMap<String, Object> parametersToAdd = new HashMap<>();
			//Get Process Parameters
			List<MProcessPara> searchProcessParameters = process.getParametersAsList();
			for (MViewColumn viewColumn: viewColumns) {
				String columnName = viewColumn.getColumnName();
				if (columnName.endsWith(I_AD_PInstance.COLUMNNAME_AD_PInstance_ID)) {
					viewProcessInstanceColumnSql = viewColumn.getColumnSQL();
					if (searchProcessParameters.isEmpty()) {
						break;
					}
				}
				if (searchProcessParameters.isEmpty()) {
					continue;
				}
				String columnSql = "";
				int index = -1;
				String parameterName = "";
				Optional<MProcessPara> maybeParameter = null;
				boolean isRange = false;

				if (parameterMap.containsKey(columnName)) {
					Object value = parameterMap.get(columnName);
				 	columnSql = viewColumn.getColumnSQL();
				 	index = columnSql.lastIndexOf(".");
				 	parameterName = columnSql.substring(index +1);
				 	String searchString = parameterName;
			 		maybeParameter = searchProcessParameters.stream().filter(param -> param.getColumnName().equals(searchString)).findFirst();
					if (maybeParameter.isEmpty()) {
						continue;
					}
					isRange = maybeParameter.get().isRange();
					String operatorName = parameterOperator.get(columnName);
					if (isRange && !operatorName.equalsIgnoreCase(Filter.BETWEEN)
						&& !operatorName.equalsIgnoreCase(Filter.GREATER_EQUAL)) {
						continue;
					}
					if (!isRange && !operatorName.equalsIgnoreCase(Filter.EQUAL)) {
						continue;
					}
					parametersToAdd.put(parameterName, value);
				}
				if (isRange && parameterMap.containsKey(columnName + "_To")) {
					Object value = parameterMap.get(columnName  + "_To");
					String operatorName = parameterOperator.get(columnName + "_To");
					if (!operatorName.equalsIgnoreCase(Filter.LESS_EQUAL)) {
						continue;
					}
					parametersToAdd.put(parameterName + "_To", value);
				}
			}

			int tableId = 0;
			int recordId = 0;
			MTable table = null;
			if(!Util.isEmpty(request.getTableName(), true)) {
				table = MTable.get(Env.getCtx(), request.getTableName());
				if (table != null && table.getAD_Table_ID() > 0) {
					tableId = table.getAD_Table_ID();
					PO entity = null;
					if(RecordUtil.isValidId(request.getRecordId(), table.getAccessLevel())) {
						entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), request.getRecordId(), null);
						if(entity != null) {
							recordId = entity.get_ID();
						}
					}

					if (table.getAD_Window_ID() > 0) {
						//	Add to recent Item
						DictionaryUtil.addToRecentItem(
							MMenu.ACTION_Window,
							table.getAD_Window_ID()
						);
					}
				}
			}

			//	Call process builder
			org.eevolution.services.dsl.ProcessBuilder searchProcessBuilder = ProcessBuilder.create(context)
				.process(searchProcessId)
				.withTitle(process.getName())
				.withWindowNo(0)
				.withRecordId(tableId, recordId)
			;
			for (Entry <String, Object> parameter: parametersToAdd.entrySet()) {
				Object parameterValue = parameter.getValue();
				if (parameterValue instanceof List ) {
					@SuppressWarnings("unchecked")
					List<Object> parameterList = (List<Object>) parameterValue;
					if (parameterList.size() >= 2) {
						searchProcessBuilder.withParameter(parameter.getKey(), parameterList.get(0), parameterList.get(1));
					} else {
						searchProcessBuilder.withParameter(parameter.getKey(), parameterList.get(0));
					}
				} else {
					searchProcessBuilder.withParameter(parameter.getKey(), parameter.getValue());
				}
			}

			// Execute Search Process
			ProcessInfo processInfo = searchProcessBuilder.execute();
			int processInstanceID = processInfo.getAD_PInstance_ID();
			if (!Util.isEmpty(viewProcessInstanceColumnSql, true)) {
				processInstanceWhere.append(" AND ")
					.append(viewProcessInstanceColumnSql)
					.append(" = ")
					.append(processInstanceID)
				;
			}
		}

		//	get query columns
		String query = QueryUtil.getBrowserQueryWithReferences(browser);
		String sql = Env.parseContext(context, windowNo, query, false);
		if (Util.isEmpty(sql, true)) {
			throw new AdempiereException(
				"@AD_Browse_ID@ " + browser.getName() + " (" + browser.getAD_Browse_ID() + "), @SQL@ @Unparseable@"
			);
		}

		MViewDefinition parentDefinition = view.getParentViewDefinition();
		String tableNameAlias = parentDefinition.getTableAlias();
		String tableName = parentDefinition.getAD_Table().getTableName();

		String sqlWithRoleAccess = MRole.getDefault(context, false)
			.addAccessSQL(
				sql,
				tableNameAlias,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();
		String where = browser.getWhereClause();
		if (!Util.isEmpty(where, true)) {
			String parsedWhereClause = Env.parseContext(context, windowNo, where, false);
			if (Util.isEmpty(parsedWhereClause, true)) {
				throw new AdempiereException(
					"@AD_Browse_ID@ " + browser.getName() + " (" + browser.getAD_Browse_ID() + "), @WhereClause@ @Unparseable@"
				);
			}
			whereClause
				.append(" AND ")
				.append(parsedWhereClause);
		}

		// Add PInstanceID validation
		whereClause.append(processInstanceWhere);

		//	For dynamic condition
		List<Object> filterValues = new ArrayList<Object>();
		String dynamicWhere = WhereClauseUtil.getBrowserWhereClauseFromCriteria(
			browser,
			request.getFilters(),
			filterValues
		);
		if (!Util.isEmpty(dynamicWhere, true)) {
			String parsedDynamicWhere = Env.parseContext(context, windowNo, dynamicWhere, false);
			if (Util.isEmpty(parsedDynamicWhere, true)) {
				throw new AdempiereException(
					"@AD_Browse_ID@ " + browser.getName() + " (" + browser.getAD_Browse_ID() + "), @WhereClause@ @Unparseable@"
				);
			}
			//	Add
			whereClause.append(" AND (")
				.append(parsedDynamicWhere)
				.append(") ")
			;
		}
		if (!Util.isEmpty(whereClause.toString(), true)) {
			// includes first AND
			sqlWithRoleAccess += whereClause;
		}

		String orderByClause = OrderByUtil.getBrowseOrderBy(browser);
		if (!Util.isEmpty(orderByClause, true)) {
			orderByClause = " ORDER BY " + orderByClause;
		}

		//	Get page and count
		int count = CountUtil.countRecords(sqlWithRoleAccess, tableName, tableNameAlias, filterValues);
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Add Row Number
		String parsedSQL = LimitUtil.getQueryWithLimit(sqlWithRoleAccess, limit, offset);
		//	Add Order By
		parsedSQL = parsedSQL + orderByClause;
		//	Return
		List<Entity> entitiesList = BrowserLogic.convertBrowserResult(browser, parsedSQL, filterValues);
		builder.addAllRecords(entitiesList);
		//	Validate page token
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		builder.setRecordCount(count);
		//	Return
		return builder;
	}


	/**
	 * Convert SQL to list values
	 * @param pagePrefix
	 * @param browser
	 * @param sql
	 * @param values
	 * @return
	 */
	public static List<Entity> convertBrowserResult(MBrowse browser, String sql, List<Object> parameters) {
		List<Entity> entitiesList = new ArrayList<Entity>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			LinkedHashMap<String, MBrowseField> fieldsMap = new LinkedHashMap<>();
			//	Add field to map
			List<MBrowseField> browseFieldsList = browser.getFields();
			for(MBrowseField field: browseFieldsList) {
				fieldsMap.put(field.getAD_View_Column().getColumnName().toUpperCase(), field);
			}
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, parameters);

			MBrowseField fieldKey = browser.getFieldKey();
			String keyColumnName = null;
			if (fieldKey != null && fieldKey.get_ID() > 0) {
				keyColumnName = fieldKey.getAD_View_Column().getColumnName();
			}
			keyColumnName = StringManager.getValidString(keyColumnName);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				Struct.Builder rowValues = Struct.newBuilder();
				ResultSetMetaData metaData = rs.getMetaData();

				Entity.Builder entityBuilder = Entity.newBuilder();
				if (!Util.isEmpty(keyColumnName, true) && rs.getObject(keyColumnName) != null) {
					entityBuilder.setId(
						rs.getInt(keyColumnName)
					);
				}
				for (int index = 1; index <= metaData.getColumnCount(); index++) {
					try {
						String columnName = metaData.getColumnName(index);
						MBrowseField field = fieldsMap.get(columnName.toUpperCase());
						//	Display Columns
						if(field == null) {
							String displayValue = rs.getString(index);
							Value.Builder displayValueBuilder = ValueManager.getValueFromString(displayValue);

							rowValues.putFields(
								columnName,
								displayValueBuilder.build()
							);
							continue;
						}
						//	From field
						String fieldColumnName = field.getAD_View_Column().getColumnName();
						Object value = rs.getObject(index);
						Value.Builder valueBuilder = ValueManager.getValueFromReference(
							value,
							field.getAD_Reference_ID()
						);
						rowValues.putFields(
							fieldColumnName,
							valueBuilder.build()
						);
					} catch (Exception e) {
						log.severe(e.getLocalizedMessage());
					}
				}
				//	
				entityBuilder.setValues(rowValues);
				entitiesList.add(
					entityBuilder.build()
				);
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		//	Return
		return entitiesList;
	}

	public static ExportBrowserItemsResponse.Builder exportBrowserItems(ExportBrowserItemsRequest request) {
		ExportBrowserItemsResponse.Builder builder = ExportBrowserItemsResponse.newBuilder();

		Properties context = Env.getCtx();
		MBrowse browser = MBrowse.get(
			context,
			request.getId()
		);
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			return builder;
		}
		HashMap<String, Object> parameterMap = new HashMap<>();
		//	Populate map
		FilterManager.newInstance(request.getFilters()).getConditions()
			.parallelStream()
			.forEach(condition -> {
				parameterMap.put(condition.getColumnName(), condition.getValue());
			});

		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, context, request.getContextAttributes());
		ContextManager.setContextWithAttributes(windowNo, context, parameterMap, false);

		//	get query columns
		String query = QueryUtil.getBrowserQueryWithReferences(browser);
		String sql = Env.parseContext(context, windowNo, query, false);
		if (Util.isEmpty(sql, true)) {
			throw new AdempiereException("@AD_Browse_ID@ @SQL@ @Unparseable@");
		}

		MView view = browser.getAD_View();
		MViewDefinition parentDefinition = view.getParentViewDefinition();
		String tableNameAlias = parentDefinition.getTableAlias();
		String tableName = parentDefinition.getAD_Table().getTableName();

		String sqlWithRoleAccess = MRole.getDefault(context, false)
			.addAccessSQL(
				sql,
				tableNameAlias,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();
		String where = browser.getWhereClause();
		if (!Util.isEmpty(where, true)) {
			String parsedWhereClause = Env.parseContext(context, windowNo, where, false);
			if (Util.isEmpty(parsedWhereClause, true)) {
				throw new AdempiereException("@AD_Browse_ID@ @WhereClause@ @Unparseable@");
			}
			whereClause
				.append(" AND ")
				.append(parsedWhereClause);
		}

		//	For dynamic condition
		List<Object> filterValues = new ArrayList<Object>();
		String dynamicWhere = WhereClauseUtil.getBrowserWhereClauseFromCriteria(
			browser,
			request.getFilters(),
			filterValues
		);
		if (!Util.isEmpty(dynamicWhere, true)) {
			String parsedDynamicWhere = Env.parseContext(context, windowNo, dynamicWhere, false);
			if (Util.isEmpty(parsedDynamicWhere, true)) {
				throw new AdempiereException("@AD_Browse_ID@ @WhereClause@ @Unparseable@");
			}
			//	Add
			whereClause.append(" AND (")
				.append(parsedDynamicWhere)
				.append(") ")
			;
		}
		if (!Util.isEmpty(whereClause.toString(), true)) {
			// includes first AND
			sqlWithRoleAccess += whereClause;
		}

		String orderByClause = org.spin.service.grpc.util.db.OrderByUtil.getBrowseOrderBy(browser);
		if (!Util.isEmpty(orderByClause, true)) {
			orderByClause = " ORDER BY " + orderByClause;
		}

		//	Get page and count
		int count = CountUtil.countRecords(sqlWithRoleAccess, tableName, tableNameAlias, filterValues);

		//	Add Order By
		String parsedSQL = sqlWithRoleAccess + orderByClause;
		List<Entity> entitiesList = BrowserLogic.convertBrowserResult(browser, parsedSQL, filterValues);

		//	Return
		builder.addAllRecords(entitiesList)
			.setRecordCount(count)
		;
		return builder;
	}


	public static List<KeyValueSelection> getAllSelectionByCriteria(int browserId, String contextAttributes, String criteriaFilters) {
		List<KeyValueSelection> selectionsList = new ArrayList<KeyValueSelection>();

		Properties context = Env.getCtx();
		MBrowse browser = MBrowse.get(
			context,
			browserId
		);
		if (browser == null || browser.getAD_Browse_ID() <= 0) {
			return selectionsList;
		}
		HashMap<String, Object> parameterMap = new HashMap<>();
		//	Populate map
		FilterManager.newInstance(criteriaFilters).getConditions()
			.parallelStream()
			.forEach(condition -> {
				parameterMap.put(condition.getColumnName(), condition.getValue());
			});

		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, context, contextAttributes);
		ContextManager.setContextWithAttributes(windowNo, context, parameterMap, false);

		//	get query columns
		String query = QueryUtil.getBrowserQueryWithReferences(browser);
		String sql = Env.parseContext(context, windowNo, query, false);
		if (Util.isEmpty(sql, true)) {
			throw new AdempiereException("@AD_Browse_ID@ @SQL@ @Unparseable@");
		}

		MView view = browser.getAD_View();
		MViewDefinition parentDefinition = view.getParentViewDefinition();
		String tableNameAlias = parentDefinition.getTableAlias();

		String sqlWithRoleAccess = MRole.getDefault(context, false)
			.addAccessSQL(
				sql,
				tableNameAlias,
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		StringBuffer whereClause = new StringBuffer();
		String where = browser.getWhereClause();
		if (!Util.isEmpty(where, true)) {
			String parsedWhereClause = Env.parseContext(context, windowNo, where, false);
			if (Util.isEmpty(parsedWhereClause, true)) {
				throw new AdempiereException("@AD_Browse_ID@ @WhereClause@ @Unparseable@");
			}
			whereClause
				.append(" AND ")
				.append(parsedWhereClause);
		}

		//	For dynamic condition
		List<Object> filterValues = new ArrayList<Object>();
		String dynamicWhere = WhereClauseUtil.getBrowserWhereClauseFromCriteria(
			browser,
			criteriaFilters,
			filterValues
		);
		if (!Util.isEmpty(dynamicWhere, true)) {
			String parsedDynamicWhere = Env.parseContext(context, windowNo, dynamicWhere, false);
			if (Util.isEmpty(parsedDynamicWhere, true)) {
				throw new AdempiereException("@AD_Browse_ID@ @WhereClause@ @Unparseable@");
			}
			//	Add
			whereClause.append(" AND (")
				.append(parsedDynamicWhere)
				.append(") ")
			;
		}
		if (!Util.isEmpty(whereClause.toString(), true)) {
			// includes first AND
			sqlWithRoleAccess += whereClause;
		}

		String orderByClause = org.spin.service.grpc.util.db.OrderByUtil.getBrowseOrderBy(browser);
		if (!Util.isEmpty(orderByClause, true)) {
			orderByClause = " ORDER BY " + orderByClause;
		}

		//	Add Order By
		String parsedSQL = sqlWithRoleAccess + orderByClause;
		//	Return
		List<Entity> entitiesList = BrowserLogic.convertBrowserResult(browser, parsedSQL, filterValues);

		List<MBrowseField> browseFields = browser.getFields()
			.stream()
			.filter(browserField -> {
				return browserField.isKey() || browserField.isIdentifier() || !browserField.isReadOnly();
			})
			.collect(Collectors.toList());
		;

		// key column to key selection
		MBrowseField keyField = browser.getFieldKey();
		if (keyField == null || keyField.getAD_Browse_Field_ID() <= 0) {
			throw new AdempiereException("@AD_Browse_ID@ @KeyColumn@ @NotFound@");
		}
		final String keyColumnName = keyField.getAD_View_Column().getColumnName();

		entitiesList.forEach(row -> {
			Map<String, Value> valuesRow = row.getValues().getFieldsMap();

			Struct.Builder rowSelection = Struct.newBuilder();
			browseFields.forEach(browseField -> {
				String columnName = browseField.getAD_View_Column().getColumnName();
				Value valueBuilder = valuesRow.get(columnName);
				rowSelection.putFields(columnName, valueBuilder);
			});

			int selectionId = ValueManager.getIntegerFromValue(
				valuesRow.get(
					keyColumnName
				)
			);
			KeyValueSelection.Builder selectionBuilder = KeyValueSelection.newBuilder()
				.setSelectionId(selectionId)
				.setValues(rowSelection)
			;

			selectionsList.add(
				selectionBuilder.build()
			);
		});

		return selectionsList;
	}

}
