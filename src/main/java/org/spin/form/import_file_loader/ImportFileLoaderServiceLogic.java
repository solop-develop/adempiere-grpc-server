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
package org.spin.form.import_file_loader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ImpFormat;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.impexp.ImpFormat;
import org.compiere.impexp.MImpFormat;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.RunBusinessProcessRequest;
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
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsRequest;
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsResponse;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.base.util.ValueUtil;
import org.spin.grpc.service.BusinessDataServiceImplementation;
import org.spin.grpc.service.UserInterfaceServiceImplementation;
import org.spin.util.AttachmentUtil;

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

	public static MTable validateAndGetTable(int tableId, String tableName) {
		if (tableId <= 0 && Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table;
		if (tableId > 0) {
			table = MTable.get(Env.getCtx(), tableId);
		} else {
			table = MTable.get(Env.getCtx(), tableName);
		}
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		return table;
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
			I_AD_Table.Table_Name,
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
		// validate and get table
		MTable table = validateAndGetTable(request.getTableId(), request.getTableName());

		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			I_AD_ImpFormat.COLUMNNAME_AD_ImpFormat_ID,
			0,
			"AD_ImpFormat.AD_Table_ID = " + table.getAD_Table_ID()
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
		// validate and get table
		MTable table = validateAndGetTable(request.getTableId(), request.getTableName());

		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			I_AD_ImpFormat.COLUMNNAME_AD_ImpFormat_ID,
			0,
			"AD_ImpFormat.AD_Client_ID = @#AD_Client_ID@ AND AD_ImpFormat.AD_Table_ID = " + table.getAD_Table_ID()
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


	public static SaveRecordsResponse.Builder saveRecords(SaveRecordsRequest request) throws Exception {
		MImpFormat importFormat = validateAndGetImportFormat(request.getImportFormatId());

		// validate attachment reference
		int attachmentReferenceId = request.getResourceId();
		if (attachmentReferenceId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_AttachmentReference_ID@");
		}

		//	Get class from parent
		ImpFormat format = ImpFormat.load(importFormat.getName());
		if (format == null) {
			throw new AdempiereException("@FileImportNoFormat@");
		}
		Class<?> clazz = format.getConnectionClass();
		//	Not yet implemented
		if (clazz == null) {
			// log.log(Level.INFO, "Using GenericDeviceHandler");
			// return;
		}

		byte[] file = AttachmentUtil.getInstance()
			.withClientId(Env.getAD_Client_ID(Env.getCtx()))
			.withAttachmentReferenceId(attachmentReferenceId)
			.getAttachment();
		if (file == null) {
			new AdempiereException("@NotFound@");
		}

		String charsetValue = request.getCharset();
		if (Util.isEmpty(charsetValue, true) || !Charset.isSupported(charsetValue)) {
			charsetValue = Charset.defaultCharset().name();
		}
		Charset charset = Charset.forName(charsetValue);

		InputStream inputStream = new ByteArrayInputStream(file);
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charset);
		BufferedReader in = new BufferedReader(inputStreamReader, 10240);

		//	not safe see p108 Network pgm
		String s = null;
		ArrayList<String> data = new ArrayList<String>();
		while ((s = in.readLine()) != null) {
			data.add(s);
		}
		in.close();

		//	For all rows - update/insert DB table
		int row = 0;
		int imported = 0;
		for(String line : data) {
			row++;
			if (format.updateDB(Env.getCtx(), line, null)) {
				imported++;
			}
		}
		//	Clear
		data.clear();
		String message = Msg.parseTranslation(Env.getCtx(), "@FileImportR/I@") + " (" + row + " / " + imported + "#)";
		SaveRecordsResponse.Builder builder = SaveRecordsResponse.newBuilder()
			.setMessage(message)
			.setTotal(imported)
		;

		if (request.getIsProcess()) {
			if (request.getProcessId() <= 0) {
				new AdempiereException("@FillMandatory@ @AD_Process_ID@");
			}
			MProcess process = MProcess.get(Env.getCtx(), request.getProcessId());
			if (process == null || process.getAD_Process_ID() <= 0) {
				new AdempiereException("@AD_Process_ID@ @NotFound@");
			}

			RunBusinessProcessRequest.Builder runProcessRequest = RunBusinessProcessRequest.newBuilder()
				.setId(process.getAD_Process_ID())
				.setUuid(process.getUUID())
				.addAllParameters(request.getParametersList())
			;
			ProcessLog.Builder processLog = BusinessDataServiceImplementation.runBusinessProcess(
				runProcessRequest.build()
			);
			builder.setProcessLog(processLog);
		}

		return builder;
	}


	public static ListEntitiesResponse.Builder listFilePreview(ListFilePreviewRequest request) {
		return ListEntitiesResponse.newBuilder();
	}


	public static ListLookupItemsResponse.Builder listImportProcesses(ListImportProcessesRequest request) {
		// validate and get table
		MTable table = validateAndGetTable(request.getTableId(), request.getTableName());

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

}
