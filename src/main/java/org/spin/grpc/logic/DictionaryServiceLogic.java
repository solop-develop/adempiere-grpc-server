package org.spin.grpc.logic;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.dictionary.Field;
import org.spin.backend.grpc.dictionary.ListFieldsRequest;
import org.spin.backend.grpc.dictionary.ListFieldsResponse;
import org.spin.grpc.service.Dictionary;

import io.vavr.control.Try;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Dictionary
 */
public class DictionaryServiceLogic {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(DictionaryServiceLogic.class);



	public static ListFieldsResponse.Builder listSearchInfoFields(ListFieldsRequest request) {
		if (Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		Properties context = Env.getCtx();
		MTable table = MTable.get(context, request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		Map<Integer, Field.Builder> fieldsList = new HashMap<Integer, Field.Builder>();
		ListFieldsResponse.Builder fieldsListBuilder = ListFieldsResponse.newBuilder();

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
			+ "	ORDER BY c.IsIdentifier DESC, c.SeqNo "
			// + " LIMIT 4 "
		;

		Try<Void> queryFields = DB.runResultSet(null, sqlQueryCriteria, parametersList, resultSet -> {
			int recordCount = 0;
			while(resultSet.next()) {
				MColumn column = MColumn.get(context, resultSet.getInt(MColumn.COLUMNNAME_AD_Column_ID));
				if (column == null || column.getAD_Column_ID() <= 0) {
					continue;
				}
				Field.Builder fieldBuilder = Dictionary.convertField(context, column);
				int sequence = (recordCount + 1) * 10;
				fieldBuilder.setSequence(sequence);
				fieldBuilder.setIsDisplayed(true);

				// disable on table
				fieldBuilder.setSeqNoGrid(0);
				fieldBuilder.setIsDisplayedGrid(false);

				fieldsList.put(column.getAD_Column_ID(), fieldBuilder);
				// fieldsListBuilder.addFields(fieldBuilder.build());

				recordCount++;
				if (recordCount == 4) {
					// as LIMIT 4 sql
					break;
				}
			}
		}).onFailure(throwable -> log.log(Level.SEVERE, sqlQueryCriteria, throwable));
		if (queryFields.isFailure()) {
			queryFields.getCause();
		}

		final String sqlTableFields = "SELECT f.AD_Field_ID "
			// + ", c.ColumnName, c.AD_Reference_ID, c.IsKey, f.IsDisplayed, c.AD_Reference_Value_ID, c.ColumnSql "
			+ " FROM AD_Column c "
			+ " INNER JOIN AD_Table t ON (c.AD_Table_ID=t.AD_Table_ID)"
			+ " INNER JOIN AD_Tab tab ON (t.AD_Window_ID=tab.AD_Window_ID)"
			+ " INNER JOIN AD_Field f ON (tab.AD_Tab_ID=f.AD_Tab_ID AND f.AD_Column_ID=c.AD_Column_ID) "
			+ " WHERE t.AD_Table_ID=? "
			+ " AND (c.IsKey='Y' OR "
				// + " (f.IsDisplayed='Y' AND f.IsEncrypted='N' AND f.ObscureType IS NULL)) "
				+ " (f.IsEncrypted='N' AND f.ObscureType IS NULL)) "
			+ "ORDER BY c.IsKey DESC, f.SeqNo "
		;

		Try<Void> tableFields = DB.runResultSet(null, sqlTableFields, parametersList, resultSet -> {
			int recordCount = 0;
			while(resultSet.next()) {
				MField field = new MField(context, resultSet.getInt(MField.COLUMNNAME_AD_Field_ID), null);
				if (field == null || field.getAD_Field_ID() <= 0) {
					continue;
				}
			
				Field.Builder fieldBuilder = fieldsList.get(field.getAD_Column_ID());
				if (fieldBuilder == null) {
					fieldBuilder = Dictionary.convertField(context, field, true);
					// disable on query
					fieldBuilder.setSequence(0);
					fieldBuilder.setIsDisplayed(false);
				}

				// enable on table
				int sequenceGrid = (recordCount + 1) * 10;
				fieldBuilder.setSeqNoGrid(sequenceGrid);
				fieldBuilder.setIsDisplayedGrid(true);
				fieldsList.put(field.getAD_Column_ID(), fieldBuilder);
				// fieldsListBuilder.addFields(fieldBuilder.build());
				recordCount++;
			}
		}).onFailure(throwable -> log.log(Level.SEVERE, sqlTableFields, throwable));
		if (tableFields.isFailure()) {
			tableFields.getCause();
		}

		fieldsListBuilder.setRecordCount(fieldsList.size());

		List<Field> fields = fieldsList.values().stream()
			.map(fieldBuilder -> {
				return fieldBuilder.build();
			})
			.sorted(
				Comparator.comparing(Field::getSequence)
			)
			.collect(Collectors.toList());

		fieldsListBuilder.addAllFields(fields);

		return fieldsListBuilder;
	}


}
