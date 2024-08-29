/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
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

package org.spin.grpc.service.form.import_file_loader;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Table;
import org.compiere.impexp.MImpFormat;
import org.compiere.impexp.MImpFormatRow;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.spin.backend.grpc.form.import_file_loader.FormatField;
import org.spin.backend.grpc.form.import_file_loader.ImportColumn;
import org.spin.backend.grpc.form.import_file_loader.ImportFormat;
import org.spin.backend.grpc.form.import_file_loader.ImportTable;
import org.spin.service.grpc.util.value.ValueManager;

public class ImportFileLoaderConvertUtil {

	public static ImportTable.Builder convertImportTable(MTable table) {
		ImportTable.Builder builder = ImportTable.newBuilder();
		if (table == null || table.getAD_Table_ID() <= 0) {
			return builder;
		}
		String name = table.getName();
		boolean isBaseLanguage = Env.isBaseLanguage(Env.getCtx(), "");
		if (!isBaseLanguage) {
			// set translated values
			name = table.get_Translation(I_AD_Table.COLUMNNAME_Name);
		}

		builder.setId(
				table.getAD_Table_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					table.getUUID()
				)
			)
			.setName(
				ValueManager.validateNull(name)
			)
			.setTableName(table.getTableName())
		;

		table.getColumnsAsList().forEach(column -> {
			String nameOfColumn = column.getName();
			if (!isBaseLanguage) {
				// set translated values
				nameOfColumn = column.get_Translation(I_AD_Column.COLUMNNAME_Name);
			}

			ImportColumn.Builder columnBuilder = ImportColumn.newBuilder()
				.setId(
					column.getAD_Column_ID()
				)
				.setUuid(
					ValueManager.validateNull(
						column.getUUID()
					)
				)
				.setName(
					ValueManager.validateNull(nameOfColumn)
				)
				.setColumnName(
					ValueManager.validateNull(
						column.getColumnName()
					)
				)
				.setDisplayType(
					column.getAD_Reference_ID()
				)
			;

			builder.addImportColumns(columnBuilder);
		});

		return builder;
	}


	public static ImportFormat.Builder convertImportFormat(MImpFormat importFormat) {
		ImportFormat.Builder builder = ImportFormat.newBuilder();
		if (importFormat == null || importFormat.getAD_ImpFormat_ID() <= 0) {
			return builder;
		}

		builder.setId(
				importFormat.getAD_ImpFormat_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					importFormat.getUUID()
				)
			)
			.setName(
				ValueManager.validateNull(
					importFormat.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					importFormat.getDescription()
				)
			)
			.setTableName(
				MTable.getTableName(Env.getCtx(), importFormat.getAD_Table_ID())
			)
			.setFormatType(
				ValueManager.validateNull(
					importFormat.getFormatType()
				)
			)
			.setSeparatorCharacter(
				ValueManager.validateNull(
					importFormat.getSeparatorChar()
				)
			)
		;

		// add Import Format Row as Format Field
		for (MImpFormatRow row : importFormat.getRows()) {
			FormatField.Builder builderFormatField = convertFormatField(row);
			builder.addFormatFields(builderFormatField);
		};

		return builder;
	}


	public static FormatField.Builder convertFormatField(MImpFormatRow importFormatRow) {
		FormatField.Builder builder = FormatField.newBuilder();
		if (importFormatRow == null || importFormatRow.getAD_ImpFormat_ID() <= 0) {
			return builder;
		}

		builder.setId(
				importFormatRow.getAD_ImpFormat_Row_ID()
			)
			.setUuid(
				ValueManager.validateNull(
					importFormatRow.getUUID()
				)
			)
			.setName(
				ValueManager.validateNull(
					importFormatRow.getName()
				)
			)
			.setSequence(
				importFormatRow.getSeqNo()
			)
			.setColumnName(
				MColumn.getColumnName(Env.getCtx(), importFormatRow.getAD_Column_ID())
			)
			.setDataType(
				ValueManager.validateNull(
					importFormatRow.getDataType()
				)
			)
			.setStartNo(
				importFormatRow.getStartNo()
			)
			.setEndNo(
				importFormatRow.getEndNo()
			)
			.setDefaultValue(
				ValueManager.validateNull(
					importFormatRow.getDefaultValue()
				)
			)
			.setDefimalPoint(
				ValueManager.validateNull(
					importFormatRow.getDecimalPoint()
				)
			)
			.setIsDivideBy100(
				importFormatRow.isDivideBy100()
			)
			.setDateFormat(
				ValueManager.validateNull(
					importFormatRow.getDataFormat()
				)
			)
			.setConstantValue(
				ValueManager.validateNull(
					importFormatRow.getConstantValue()
				)
			)
		;

		return builder;
	}

}
