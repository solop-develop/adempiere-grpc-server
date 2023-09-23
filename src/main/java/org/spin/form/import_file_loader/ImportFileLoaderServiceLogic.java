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
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ImpFormat;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.core.domains.models.X_AD_ImpFormat;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.impexp.ImpFormat;
import org.compiere.impexp.ImpFormatRow;
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
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.RunBusinessProcessRequest;
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
import org.spin.grpc.service.BusinessData;
import org.spin.grpc.service.UserInterface;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service logic of Import File Loader form
 */
public class ImportFileLoaderServiceLogic {

	public static int MAX_SHOW_LINES = 100;



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
			LookupItem.Builder builder = LookupItem.newBuilder().setValues(Struct.newBuilder().putFields(
					LookupUtil.VALUE_COLUMN_KEY,
					value.build()
				)
				.putFields(
					LookupUtil.DISPLAY_COLUMN_KEY,
					value.build()
				))
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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
			throw new AdempiereException("@AD_AttachmentReference_ID@ @NotFound@");
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
		int count = 0;
		int totalRows = 0;
		int importedRows = 0;
		while ((s = in.readLine()) != null) {
			data.add(s);
			count++;
			totalRows++;
			// paging/partition of data to process
			if (count == 100) {
				count = 0;
				// import bd
				importedRows += saveOnDataBase(format, data);
				data.clear();
			}
		}
		in.close();

		// first data of total less than 100, or remainder of last data
		if (data.size() > 0) {
			importedRows += saveOnDataBase(format, data);
		}
		//	Clear
		data.clear();

		String message = Msg.parseTranslation(Env.getCtx(), "@FileImportR/I@") + " (" + totalRows + " / " + importedRows + "#)";
		SaveRecordsResponse.Builder builder = SaveRecordsResponse.newBuilder()
			.setMessage(message)
			.setTotal(importedRows)
		;

		if (request.getIsProcess()) {
			if (request.getProcessId() <= 0) {
				throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
			}
			MProcess process = MProcess.get(Env.getCtx(), request.getProcessId());
			if (process == null || process.getAD_Process_ID() <= 0) {
				throw new AdempiereException("@AD_Process_ID@ @NotFound@");
			}

			RunBusinessProcessRequest.Builder runProcessRequest = RunBusinessProcessRequest.newBuilder()
				.setId(process.getAD_Process_ID())
				.setParameters(request.getParameters())
			;
			ProcessLog.Builder processLog = BusinessData.runBusinessProcess(
				runProcessRequest.build()
			);
			builder.setProcessLog(processLog);
		}

		return builder;
	}


	private static int saveOnDataBase(ImpFormat format, ArrayList<String> data) {
		int imported = 0;
		for(String line : data) {
			if (format.updateDB(Env.getCtx(), line, null)) {
				imported++;
			}
		}
		return imported;
	}




	/**
	 * TODO: Delete this redundant method with accept the changes on https://github.com/adempiere/adempiere/pull/4125
	 *  Parse flexible line format.
	 *  A bit inefficient as it always starts from the start
	 *
	 *  @param line the line to be parsed
	 *  @param formatType Comma or Tab
	 *  @param fieldNo number of field to be returned
	 *  @return field in lime or ""
	@throws IllegalArgumentException if format unknowns
	 */
	private static String parseFlexFormat(String line, String formatType, int fieldNo, String separatorChar)
	{
		final char QUOTE = '"';
		//  check input
		char delimiter = ' ';
		if (formatType.equals(X_AD_ImpFormat.FORMATTYPE_CommaSeparated)) {
			delimiter = ',';
		} else if (formatType.equals(X_AD_ImpFormat.FORMATTYPE_TabSeparated)) {
			delimiter = '\t';
		} else if (formatType.equals(X_AD_ImpFormat.FORMATTYPE_CustomSeparatorChar)) {
			delimiter = separatorChar.charAt(0);
		} else {
			throw new IllegalArgumentException ("ImpFormat.parseFlexFormat - unknown format: " + formatType);
		}
		if (line == null || line.length() == 0 || fieldNo < 0) {
			return "";
		}

		//  We need to read line sequentially as the fields may be delimited
		//  with quotes (") when fields contain the delimiter
		//  Example:    "Artikel,bez","Artikel,""nr""",DEM,EUR
		//  needs to result in - Artikel,bez - Artikel,"nr" - DEM - EUR
		int pos = 0;
		int length = line.length();
		for (int field = 1; field <= fieldNo && pos < length; field++) {
			StringBuffer content = new StringBuffer();
			//  two delimiter directly after each other
			if (line.charAt(pos) == delimiter) {
				pos++;
				continue;
			}
			//  Handle quotes
			if (line.charAt(pos) == QUOTE) {
				pos++;  //  move over beginning quote
				while (pos < length) {
					//  double quote
					if (line.charAt(pos) == QUOTE && pos+1 < length && line.charAt(pos+1) == QUOTE) {
						content.append(line.charAt(pos++));
						pos++;
					}
					//  end quote
					else if (line.charAt(pos) == QUOTE) {
						pos++;
						break;
					}
					//  normal character
					else {
						content.append(line.charAt(pos++));
					}
				}
				//  we should be at end of line or a delimiter
				if (pos < length && line.charAt(pos) != delimiter) {
					// log.info("Did not find delimiter at pos " + pos + " " + line);
				}
				pos++;  //  move over delimiter
			}
			// plain copy
			else {
				while (pos < length && line.charAt(pos) != delimiter)
					content.append(line.charAt(pos++));
				pos++;  //  move over delimiter
			}
			if (field == fieldNo) {
				return content.toString();
			}
		}

		//  nothing found
		return "";
	}   //  parseFlexFormat


	public static ListEntitiesResponse.Builder listFilePreview(ListFilePreviewRequest request) throws Exception {
		MImpFormat importFormat = validateAndGetImportFormat(request.getImportFormatId());
		//	Get class from parent
		ImpFormat format = ImpFormat.load(importFormat.getName());
		if (format == null) {
			throw new AdempiereException("@FileImportNoFormat@");
		}

		// validate attachment reference
		int attachmentReferenceId = request.getResourceId();
		if (attachmentReferenceId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_AttachmentReference_ID@");
		}

		byte[] file = AttachmentUtil.getInstance()
			.withClientId(Env.getAD_Client_ID(Env.getCtx()))
			.withAttachmentReferenceId(attachmentReferenceId)
			.getAttachment();
		if (file == null) {
			throw new AdempiereException("@NotFound@");
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
		int count = 0;
		while ((s = in.readLine()) != null) {
			data.add(s);
			count++;
			// paging/partition of data to process
			if (count == MAX_SHOW_LINES) {
				break;
			}
		}
		in.close();

		ListEntitiesResponse.Builder builderList = ListEntitiesResponse.newBuilder()
			.setRecordCount(count)
		;

		MTable table = MTable.get(Env.getCtx(), importFormat.getAD_Table_ID());

		data.forEach(line -> {
			Entity.Builder entitBuilder = Entity.newBuilder()
				.setTableName(table.getTableName())
			;

			for (int i = 0; i < format.getRowCount(); i++) {
				ImpFormatRow row = (ImpFormatRow) format.getRow(i);

				//	Get Data
				String info = null;
				if (row.isConstant()) {
					// info = "Constant";
					info = row.getConstantValue();
				} else if (X_AD_ImpFormat.FORMATTYPE_FixedPosition.equals(format.getFormatType())) {
					//	check length
					if (row.getStartNo() > 0 && row.getEndNo() <= line.length()) {
						info = line.substring(row.getStartNo()-1, row.getEndNo());
					}
				} else {
					info = parseFlexFormat(line, format.getFormatType(), row.getStartNo(), format.getSeparatorChar());
				}

				if (Util.isEmpty(info, true)) {
					if (row.getDefaultValue() != null ) {
						info = row.getDefaultValue();
					}
					else {
						info = "";
					}
				}
				String entry = info;

				Value.Builder valueBuilder = Value.newBuilder();
				if (row.isDate()) {
					Timestamp dateValue = Timestamp.valueOf(entry);
					valueBuilder = ValueUtil.getValueFromDate(dateValue);
				} else if (row.isNumber()) {
					BigDecimal numberValue = null;
					if (!Util.isEmpty(entry, true)) {
						numberValue = new BigDecimal(entry);
						// if (row.isDivideBy100()) {
						// 	numberValue = numberValue.divide(BigDecimal.valueOf(100));
						// }
					}
					valueBuilder = ValueUtil.getValueFromDecimal(numberValue);
				} else {
					valueBuilder = ValueUtil.getValueFromString(entry);
				}

				entitBuilder.setValues(Struct.newBuilder().putFields(row.getColumnName(), valueBuilder.build()));
			}

			// columns.fo
			builderList.addRecords(entitBuilder);
		});

		return builderList;
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

			builderList.addRecords(builderItem.build());
		});

		return builderList;
	}

}
