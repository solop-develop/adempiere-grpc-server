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
package org.spin.grpc.logic;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ImpFormat;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.impexp.MImpFormat;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.common.Value;
import org.spin.backend.grpc.form.import_file_loader.GetImportFromatRequest;
import org.spin.backend.grpc.form.import_file_loader.ImportFormat;
import org.spin.backend.grpc.form.import_file_loader.ImportTable;
import org.spin.backend.grpc.form.import_file_loader.ListCharsetsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListClientImportFormatsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListFilePreviewRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportFormatsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportProcessesRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportTablesRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportTablesResponse;
import org.spin.backend.grpc.form.import_file_loader.ProcessImportRequest;
import org.spin.backend.grpc.form.import_file_loader.ProcessImportResponse;
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsRequest;
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsResponse;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.base.util.ValueUtil;
import org.spin.form.import_file_loader.ImportFileLoaderConvertUtil;
import org.spin.grpc.service.UserInterfaceServiceImplementation;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service logic of Import File Loader form
 */
public class ImportFileLoaderServiceLogic {

	public static MImpFormat validateAndGetImportFormat(int importFormatId) {
		if (importFormatId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_ImpFormat_ID@");
		}
		MImpFormat importFormat = new Query(
			Env.getCtx(),
			I_AD_ImpFormat.Table_Name,
			" AD_ImpFormat_ID = ? ",
			null
		)
			.setParameters(importFormatId)
			.setApplyAccessFilter(true)
			.first();
		if (importFormat == null || importFormat.getAD_ImpFormat_ID() <= 0) {
			throw new AdempiereException("@AD_ImpFormat_ID@ @NotFound@");
		}
		return importFormat;
	}



	public static ListLookupItemsResponse.Builder listCharsets(ListCharsetsRequest request) {
		List<Charset> charsetsList = Arrays.asList(
			Ini.getAvailableCharsets()
		);

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(charsetsList.size())
		;

		if (!Util.isEmpty(request.getSearchValue(), true)) {
			final String searchValue = request.getSearchValue().toLowerCase();

			charsetsList = charsetsList.stream().filter(charset -> {
				return charset.name().toLowerCase().contains(searchValue);
			})
			.collect(Collectors.toList());
		}

		charsetsList.stream().forEach(charset -> {
			Value.Builder value = ValueUtil.getValueFromString(
				charset.name()
			);
			LookupItem.Builder builder = LookupItem.newBuilder()
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					value.build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					value.build()
				)
			;
			builderList.addRecords(builder);
		});

		return builderList;
	}


	public static ListImportTablesResponse.Builder listImportTables(ListImportTablesRequest request) {
		final String whereClause = "TableName LIKE 'I#_%' ESCAPE '#'";

		List<MTable> importTablesList = new Query(
			Env.getCtx(),
			I_AD_Process.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(true)
			.<MTable>list()
		;

		ListImportTablesResponse.Builder builderList = ListImportTablesResponse.newBuilder()
			.setRecordCount(importTablesList.size())
		;
		importTablesList.stream().forEach(table -> {
			ImportTable.Builder builder = ImportFileLoaderConvertUtil.convertImportTable(table);
			builderList.addRecords(builder);
		});

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listImportFormats(ListImportFormatsRequest request) {
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir, 0, I_AD_ImpFormat.COLUMNNAME_AD_ImpFormat_ID, 0
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listClientImportFormats(ListClientImportFormatsRequest request) {
		int validationClientLogin = 116; // AD_Client Login (Restrict to login client)
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			I_AD_ImpFormat.COLUMNNAME_AD_ImpFormat_ID,
			validationClientLogin
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}


	public static ImportFormat.Builder getImportFromat(GetImportFromatRequest request) {
		MImpFormat importFormat = validateAndGetImportFormat(request.getId());

		ImportFormat.Builder builder = ImportFileLoaderConvertUtil.convertImportFormat(importFormat);

		return builder;
	}


	public static SaveRecordsResponse.Builder saveRecords(SaveRecordsRequest request) {
		// MImpFormat importFormat = validateAndGetImportFormat(request.getImportFormatId());

		return SaveRecordsResponse.newBuilder();
	}


	public static ListEntitiesResponse.Builder listFilePreview(ListFilePreviewRequest request) {
		return ListEntitiesResponse.newBuilder();
	}


	public static ListLookupItemsResponse.Builder listImportProcesses(ListImportProcessesRequest request) {
		if (request.getTableId() <= 0 || Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table;
		if (request.getTableId() > 0) {
			table = MTable.get(Env.getCtx(), request.getTableId());
		} else {
			table = MTable.get(Env.getCtx(), request.getTableName());
		}
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		List<Object> filtersLit = new ArrayList<Object>();
		//	Process associated from table or column
		final String whereClause = "EXISTS(SELECT 1 FROM AD_Table_Process AS tp "
			+ "WHERE tp.AD_Process_ID = AD_Process.AD_Process_ID "
			+ "AND tp.AD_Table_ID = ? "
			+ "AND tp.IsActive = 'Y') "
			+ "OR EXISTS (SELECT 1 FROM AD_Column c "
			+ "WHERE c.AD_Process_ID = AD_Process.AD_Process_ID "
			+ "AND c.AD_Table_ID = ? "
			+ "AND c.IsActive = 'Y')";
		filtersLit.add(table.getAD_Table_ID());
		filtersLit.add(table.getAD_Table_ID());

		List<MProcess> processFromColumnsList = new Query(
			Env.getCtx(),
			I_AD_Process.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersLit)
			.setOnlyActiveRecords(true)
			.<MProcess>list()
		;

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();
		processFromColumnsList.forEach(processDefinition -> {
			String displayedValue = processDefinition.getValue() + " - " + processDefinition.getName();
			if (!Env.isBaseLanguage(Env.getCtx(), "")) {
				// set translated values
				displayedValue = processDefinition.getValue() + " - " + processDefinition.get_Translation(I_AD_Process.COLUMNNAME_Name);
			}

			LookupItem.Builder builderItem = LookupUtil.convertObjectFromResult(
				processDefinition.getAD_Process_ID(), processDefinition.getUUID(), processDefinition.getValue(), displayedValue
			);

			builderItem.setTableName(I_AD_Process.Table_Name);
			builderItem.setId(processDefinition.getAD_Process_ID());
			builderItem.setUuid(ValueUtil.validateNull(processDefinition.getUUID()));

			builderList.addRecords(builderItem.build());
		});

		return builderList;
	}


	public static ProcessImportResponse.Builder processImport(ProcessImportRequest request) {
		// MImpFormat importFormat = validateAndGetImportFormat(request.getImportFormatId());

		return ProcessImportResponse.newBuilder();
	}

}
