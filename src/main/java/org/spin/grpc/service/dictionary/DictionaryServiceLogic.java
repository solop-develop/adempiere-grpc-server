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
package org.spin.grpc.service.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Table_Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.ListIdentifierColumnsRequest;
import org.spin.backend.grpc.dictionary.ListIdentifierColumnsResponse;
import org.spin.backend.grpc.dictionary.ListProcessesRequest;
import org.spin.backend.grpc.dictionary.ListProcessesResponse;
import org.spin.backend.grpc.dictionary.ListSearchFieldsRequest;
import org.spin.backend.grpc.dictionary.ListSearchFieldsResponse;
import org.spin.backend.grpc.dictionary.ListSelectionColumnsRequest;
import org.spin.backend.grpc.dictionary.ListSelectionColumnsResponse;
import org.spin.backend.grpc.dictionary.Process;
import org.spin.backend.grpc.dictionary.SearchColumn;
import org.spin.base.util.ReferenceInfo;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.value.TextManager;

import io.vavr.control.Try;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Dictionary
 */
public class DictionaryServiceLogic {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(DictionaryServiceLogic.class);


	/**
	 * Convert Process from UUID
	 * @param id
	 * @param withParameters
	 * @return
	 */
	public static Process.Builder getProcess(Properties context, String processUuid, boolean withParameters) {
		if (Util.isEmpty(processUuid, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@ / @UUID@");
		}
		int processId = RecordUtil.getIdFromUuid(I_AD_Process.Table_Name, processUuid, null);
		if (processId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		}
		MProcess process = MProcess.get(context, processId);
		if (process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}
		//	Convert
		return ProcessConvertUtil.convertProcess(
			context,
			process,
			withParameters
		);
	}

	public static ListProcessesResponse.Builder listProcesses(ListProcessesRequest request) {
		MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);
		Properties context = Env.getCtx();

		Query query = new Query(
			context,
			I_AD_Table_Process.Table_Name,
			"AD_Table_ID = ?",
			null
		)
			.setParameters(table.getAD_Table_ID())
			// .getIDsAsList()
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;
		ListProcessesResponse.Builder builderList = ListProcessesResponse.newBuilder()
			.setRecordCount(query.count())
		;

		query.list()
			.forEach(processTable -> {
				int processId = processTable.get_ValueAsInt(I_AD_Table_Process.COLUMNNAME_AD_Process_ID);
				if (processId <= 0) {
					throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
				}
				MProcess process = MProcess.get(context, processId);
				if (process == null || process.getAD_Process_ID() <= 0) {
					throw new AdempiereException("@AD_Process_ID@ @NotFound@");
				}
				//	Convert
				Process.Builder processBuilder = ProcessConvertUtil.convertProcess(
					context,
					process,
					false
				);
				builderList.addProcesses(processBuilder);
			});
		;

		return builderList;
	}



	public static ListIdentifierColumnsResponse.Builder getIdentifierFields(ListIdentifierColumnsRequest request) {
		// MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
		// 	DisplayType.Search,
		// 	request.getFieldId(),
		// 	request.getProcessParameterId(),
		// 	request.getBrowseFieldId(),
		// 	request.getColumnId(),
		// 	request.getColumnName(),
		// 	request.getTableName()
		// );
		// if (reference == null) {
		// 	throw new AdempiereException("@AD_Reference_ID@ @NotFound@");
		// }
		final String tableName = request.getTableName();
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			tableName
		);

		Properties context = Env.getCtx();
		Query query = new Query(
			context,
			I_AD_Column.Table_Name,
			"AD_Table_ID = ? AND IsIdentifier = 'Y'",
			null
		)
			.setOrderBy(I_AD_Column.COLUMNNAME_SeqNo)
			.setParameters(table.getAD_Table_ID())
		;

		ListIdentifierColumnsResponse.Builder fieldsListBuilder = ListIdentifierColumnsResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
		;

		query
			.getIDsAsList()
			.forEach(columnId -> {
				MColumn column = MColumn.get(context, columnId);
				if (column == null || column.getAD_Column_ID() <= 0) {
					return;
				}
				final String columnName = column.getColumnName();
				fieldsListBuilder.addIdentifierColumns(
						columnName
					)
				;
			})
		;

		if (fieldsListBuilder.getIdentifierColumnsCount() <= 0) {
			MColumn valueColumn = table.getColumn("Value");
			if (valueColumn != null) {
				fieldsListBuilder.addIdentifierColumns(
					"Value"
				);
			}
			MColumn nameColumn = table.getColumn("Name");
			if (nameColumn != null) {
				fieldsListBuilder.addIdentifierColumns(
					"Name"
				);
			}
			MColumn documentNoColumn = table.getColumn("DocumentNo");
			if (documentNoColumn != null) {
				fieldsListBuilder.addIdentifierColumns(
					"DocumentNo"
				);
			}
		}
		if (fieldsListBuilder.getIdentifierColumnsCount() <= 0) {
			fieldsListBuilder.addAllIdentifierColumns(
				Arrays.asList(
					table.getKeyColumns()
				)
			);
		}

		return fieldsListBuilder;
	}

	public static ListSelectionColumnsResponse.Builder listSelectionColumns(ListSelectionColumnsRequest request) {
		// MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
		// 	DisplayType.Search,
		// 	request.getFieldId(),
		// 	request.getProcessParameterId(),
		// 	request.getBrowseFieldId(),
		// 	request.getColumnId(),
		// 	request.getColumnName(),
		// 	request.getTableName()
		// );
		// if (reference == null) {
		// 	throw new AdempiereException("@AD_Reference_ID@ @NotFound@");
		// }
		final String tableName = request.getTableName();
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			tableName
		);

		Properties context = Env.getCtx();
		Query query = new Query(
			context,
			I_AD_Column.Table_Name,
			"AD_Table_ID = ? AND IsSelectionColumn = 'Y'",
			null
		)
			.setOrderBy(I_AD_Column.COLUMNNAME_ColumnName)
			.setParameters(table.getAD_Table_ID())
		;

		ListSelectionColumnsResponse.Builder fieldsListBuilder = ListSelectionColumnsResponse.newBuilder()
			.setRecordCount(
				query.count()
			)
		;

		AtomicInteger sequence = new AtomicInteger(0);
		query
			.getIDsAsList()
			.forEach(columnId -> {
				MColumn column = MColumn.get(context, columnId);
				if (column == null || column.getAD_Column_ID() <= 0) {
					return;
				}
				final String columnName = column.getColumnName();
				Field.Builder fieldBuilder = DictionaryConvertUtil.convertFieldByColumn(
					context,
					column
				);
				fieldBuilder.setSequence(
						sequence.incrementAndGet()
					)
					.setIsDisplayed(true)
					.setIsMandatory(false)
				;
				fieldsListBuilder.addSelectionColumns(
						columnName
					)
					.addSelectionFields(
						fieldBuilder
					)
				;
			})
		;

		if (fieldsListBuilder.getSelectionColumnsCount() <= 0) {
			MColumn valueColumn = table.getColumn("Value");
			if (valueColumn != null) {
				fieldsListBuilder.addSelectionColumns(
					"Value"
				);
			}
			MColumn nameColumn = table.getColumn("Name");
			if (nameColumn != null) {
				fieldsListBuilder.addSelectionColumns(
					"Name"
				);
			}
			MColumn documentNoColumn = table.getColumn("DocumentNo");
			if (documentNoColumn != null) {
				fieldsListBuilder.addSelectionColumns(
					"DocumentNo"
				);
			}
		}
		if (fieldsListBuilder.getSelectionColumnsCount() <= 0) {
			fieldsListBuilder.addAllSelectionColumns(
				Arrays.asList(
					table.getKeyColumns()
				)
			);
		}

		return fieldsListBuilder;
	}



	public static ListSearchFieldsResponse.Builder listSearchFields(ListSearchFieldsRequest request) {
		ListSearchFieldsResponse.Builder responseBuilder = ListSearchFieldsResponse.newBuilder();

		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.Search,
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			request.getTableName()
		);
		if (reference == null) {
			throw new AdempiereException("@AD_Reference_ID@ @NotFound@");
		}
		final String tableName = reference.TableName;
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@AD_Reference_ID@ @AD_Table_ID@ @NotFound@");
		}

		responseBuilder.setTableName(
			TextManager.getValidString(
				tableName
			)
		);

		List<Field> queryFieldsList = listQuerySearchFields(
			tableName
		);
		responseBuilder.addAllQueryFields(queryFieldsList);

		List<SearchColumn> searchColumnsList = listSearchColumns(
			tableName
		);
		responseBuilder.addAllTableColumns(searchColumnsList);

		return responseBuilder;
	}

	public static List<Field> listQuerySearchFields(String tableName) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			tableName
		);
		Properties context = Env.getCtx();

		final List<Object> parametersList = List.of(table.getAD_Table_ID());

		final String sqlQueryCriteria = "SELECT c.AD_Column_ID"
			// + ", c.ColumnName, t.AD_Table_ID, t.TableName, c.ColumnSql "
			+ " FROM AD_Table AS t "
			+ "	INNER JOIN AD_Column c ON (t.AD_Table_ID=c.AD_Table_ID) "
			+ "	WHERE c.AD_Reference_ID = 10 "
			+ " AND t.AD_Table_ID = ? "
			//	Displayed in Window
			+ "	AND EXISTS (SELECT * FROM AD_Field AS f "
			+ "	WHERE f.AD_Column_ID=c.AD_Column_ID "
			+ " AND f.IsDisplayed='Y' AND f.IsEncrypted='N' AND f.ObscureType IS NULL) "
			// + " AND ROWNUM <= 4 " // records have different results
			+ "	ORDER BY c.IsIdentifier DESC, c.SeqNo "
			// + " LIMIT 4 "
		;

		List<Field> queryFieldsList = new ArrayList<>();
		Try<Void> queryFields = DB.runResultSet(null, sqlQueryCriteria, parametersList, resultSet -> {
			int recordCount = 0;
			while(resultSet.next()) {
				int columnId = resultSet.getInt(MColumn.COLUMNNAME_AD_Column_ID);
				MColumn column = MColumn.get(context, columnId);
				if (column == null || column.getAD_Column_ID() <= 0) {
					continue;
				}
				Field.Builder fieldBuilder = DictionaryConvertUtil.convertFieldByColumn(
					context,
					column
				);
				int sequence = (recordCount + 1) * 10;
				fieldBuilder.setSequence(sequence);
				fieldBuilder.setIsDisplayed(true);
				fieldBuilder.setIsMandatory(false);

				queryFieldsList.add(
					fieldBuilder.build()
				);

				recordCount++;
				if (recordCount == 4) {
					// as LIMIT 4 sql
					break;
				}
			}
		}).onFailure(throwable -> {
			log.log(Level.SEVERE, sqlQueryCriteria, throwable);
		});
		if (queryFields.isFailure()) {
			queryFields.getCause();
		}

		return queryFieldsList;
	}



	public static List<SearchColumn> listSearchColumns(String tableName) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			tableName
		);

		final List<Object> parametersList = List.of(table.getAD_Table_ID());

		final String sql = "SELECT f.AD_Field_ID "
			// + ", c.ColumnName, c.AD_Reference_ID, c.IsKey, f.IsDisplayed, c.AD_Reference_Value_ID, c.ColumnSql "
			+ " FROM AD_Column c "
			+ " INNER JOIN AD_Table t ON (c.AD_Table_ID=t.AD_Table_ID)"
			+ " INNER JOIN AD_Tab tab ON (t.AD_Window_ID=tab.AD_Window_ID)"
			+ " INNER JOIN AD_Field f ON (tab.AD_Tab_ID=f.AD_Tab_ID AND f.AD_Column_ID=c.AD_Column_ID) "
			+ " WHERE t.AD_Table_ID=? "
			+ " AND (c.IsKey='Y' OR "
				// Yes-No, Amount, Number, Quantity, Integer, String, Text, Memo, Date, DateTime, Time
				+ " (c.AD_Reference_ID IN (20, 12, 22, 29, 11, 10, 14, 34, 15, 16, 24, 17) "
				+ " AND f.IsDisplayed='Y'"
				// + " (f.IsDisplayed='Y' AND f.IsEncrypted='N' AND f.ObscureType IS NULL)) "
				+ " AND f.IsEncrypted='N' AND f.ObscureType IS NULL)) "
			+ "ORDER BY c.IsKey DESC, f.SeqNo "
		;

		List<SearchColumn> searchColumnsList = new ArrayList<>();
		DB.runResultSet(null, sql, parametersList, resultSet -> {
			while(resultSet.next()) {
				int fieldId = resultSet.getInt(MField.COLUMNNAME_AD_Field_ID);
				SearchColumn.Builder searchColumnBuilder = DictionaryConvertUtil.convertSearchColumnByFieldId(fieldId);

				searchColumnsList.add(
					searchColumnBuilder.build()
				);
			}
		}).onFailure(throwable -> {
			log.log(Level.WARNING, sql, throwable);
		});

		return searchColumnsList;
	}

}
