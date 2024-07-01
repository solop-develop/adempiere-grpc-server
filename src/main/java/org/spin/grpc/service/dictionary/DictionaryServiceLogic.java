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
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.ListIdentifierColumnsRequest;
import org.spin.backend.grpc.dictionary.ListIdentifierColumnsResponse;
import org.spin.backend.grpc.dictionary.ListSearchFieldsRequest;
import org.spin.backend.grpc.dictionary.ListSearchFieldsResponse;
import org.spin.backend.grpc.dictionary.SearchColumn;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.service.grpc.util.value.ValueManager;

import io.vavr.control.Try;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Dictionary
 */
public class DictionaryServiceLogic {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(DictionaryServiceLogic.class);


	public static ListIdentifierColumnsResponse.Builder getIdentifierFields(ListIdentifierColumnsRequest request) {
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
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		Properties context = Env.getCtx();
		MTable table = MTable.get(context, tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		ListIdentifierColumnsResponse.Builder fieldsListBuilder = ListIdentifierColumnsResponse.newBuilder();

		final String sql = "SELECT c.ColumnName"
			// + "c.AD_Column_ID , c.ColumnName, t.AD_Table_ID, t.TableName, c.ColumnSql "
			+ " FROM AD_Column AS c"
			+ " WHERE "
			// + "	WHERE c.AD_Reference_ID = 10 "
			+ " c.AD_Table_ID = ? "
			+ " AND c.IsIdentifier = 'Y'"
			+ "	ORDER BY c.SeqNo "
		;

		DB.runResultSet(null, sql, List.of(table.getAD_Table_ID()), resultSet -> {
			int recordCount = 0;
			while(resultSet.next()) {
				String columnName = resultSet.getString(I_AD_Column.COLUMNNAME_ColumnName);
				fieldsListBuilder.addIdentifierColumns(
					columnName
				);
				recordCount++;
			}
			fieldsListBuilder.setRecordCount(recordCount);
		}).onFailure(throwable -> {
			log.log(Level.SEVERE, sql, throwable);
		});

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
		}
		if (fieldsListBuilder.getIdentifierColumnsCount() <= 0) {
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

		//	empty general info
		// if (fieldsListBuilder.getFieldsList().size() == 0) {
		// }

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
			ValueManager.validateNull(
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
