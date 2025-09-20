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
package org.spin.grpc.service.form.import_file_loader;
import java.io.BufferedReader;
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
import org.compiere.model.MClientInfo;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.compiere.util.Util;
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
import org.spin.backend.grpc.form.import_file_loader.ListFilePreviewResponse;
import org.spin.backend.grpc.form.import_file_loader.ListImportFormatsRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportProcessesRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportTablesRequest;
import org.spin.backend.grpc.form.import_file_loader.ListImportTablesResponse;
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsRequest;
import org.spin.backend.grpc.form.import_file_loader.SaveRecordsResponse;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.eca62.support.IS3;
import org.spin.eca62.support.ResourceMetadata;
import org.spin.grpc.service.BusinessData;
import org.spin.grpc.service.field.field_management.FieldManagementLogic;
import org.spin.model.MADAppRegistration;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.support.AppSupportHandler;
import org.spin.util.support.IAppSupport;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * @author Elsio Sanchez, Elsiosanches@gmail.com, https://github.com/ElsioSanchez
 * Service logic of Import File Loader form
 */
public class ImportFileLoaderLogic {

	public static final int FORM_ID = 101;

	public static final int MAX_SHOW_LINES = 100;



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

		final String searchValue = StringManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			charsetsList = charsetsList.parallelStream().filter(charset -> {
				return charset.name().toLowerCase().contains(
					searchValue.toLowerCase()
				);
			})
			.collect(Collectors.toList());
		}

		charsetsList.parallelStream().forEach(charset -> {
			Value.Builder value = ValueManager.getValueFromString(
				charset.name()
			);
			Struct.Builder values = Struct.newBuilder()
				.putFields(
					LookupUtil.VALUE_COLUMN_KEY,
					value.build()
				)
				.putFields(
					LookupUtil.DISPLAY_COLUMN_KEY,
					value.build()
				)
			;
			LookupItem.Builder builder = LookupItem.newBuilder()
				.setValues(values)
			;
			builderList.addRecords(builder);
		});

		return builderList;
	}


	public static ListImportTablesResponse.Builder listImportTables(ListImportTablesRequest request) {
		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Form,
			FORM_ID
		);

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
		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Form,
			FORM_ID
		);

		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			I_AD_ImpFormat.COLUMNNAME_AD_ImpFormat_ID,
			0,
			"AD_ImpFormat.AD_Table_ID = " + table.getAD_Table_ID()
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listClientImportFormats(ListClientImportFormatsRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			I_AD_ImpFormat.COLUMNNAME_AD_ImpFormat_ID,
			0,
			"AD_ImpFormat.AD_Client_ID = @#AD_Client_ID@ AND AD_ImpFormat.AD_Table_ID = " + table.getAD_Table_ID()
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
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

		// validate Get File Name reference
		String attachmentFileName = request.getResourceName();
		if (Util.isEmpty(attachmentFileName, true) ) {
			throw new AdempiereException("@FileName@ @NotFound@");
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

		MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
		if (clientInfo == null) {
			throw new AdempiereException("@ClientInfo@");
		}
		if (clientInfo.getFileHandler_ID() <= 0) {
			throw new AdempiereException("@FileHandler_ID@ @NotFound@");
		}
		// Connector S3
		MADAppRegistration genericConnector = MADAppRegistration.getById(
			Env.getCtx(),
			clientInfo.getFileHandler_ID(),
			null
		);
		if (genericConnector == null || genericConnector.getAD_AppRegistration_ID() <= 0) {
			throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
		}
		//	Load
		IAppSupport supportedApi = AppSupportHandler.getInstance().getAppSupport(genericConnector);
		if (supportedApi == null) {
			throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
		}
		if (!IS3.class.isAssignableFrom(supportedApi.getClass())) {
			throw new AdempiereException("@AD_AppSupport_ID@ @Unsupported@");
		}
		//	Get it
		IS3 fileHandler = (IS3) supportedApi;

		//  Resource 
		ResourceMetadata resourceMetadata = ResourceMetadata.newInstance()
					.withResourceName(attachmentFileName);
		InputStream inputStream = fileHandler.getResource(resourceMetadata);
		if (inputStream == null) {
			throw new AdempiereException("@InputStream@ @NotFound@");
		}

		String charsetValue = request.getCharset();
		if (Util.isEmpty(charsetValue, true) || !Charset.isSupported(charsetValue)) {
			charsetValue = Charset.defaultCharset().name();
		}
		Charset charset = Charset.forName(charsetValue);

		// InputStream inputStream = new ByteArrayInputStream(file);
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

		String message = Msg.parseTranslation(
				Env.getCtx(),
				"@FileImportR/I@"
			)
			+ " (" + totalRows + " / " + importedRows + "#)"
		;
		SaveRecordsResponse.Builder builder = SaveRecordsResponse.newBuilder()
			.setMessage(
				StringManager.getValidString(
					message
				)
			)
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



	public static ListFilePreviewResponse.Builder listFilePreview(ListFilePreviewRequest request) throws Exception {
		MImpFormat importFormat = validateAndGetImportFormat(request.getImportFormatId());
		//	Get class from parent
		ImpFormat format = ImpFormat.load(importFormat.getName());
		if (format == null) {
			throw new AdempiereException("@FileImportNoFormat@");
		}

		// Get File Name
		String fileName = request.getResourceName();

		if (fileName == null) {
			throw new AdempiereException("@FileName@ @NotFound@");
		}

		MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
		if (clientInfo == null) {
			throw new AdempiereException("@ClientInfo@");
		}
		if (clientInfo.getFileHandler_ID() <= 0) {
			throw new AdempiereException("@FileHandler_ID@ @NotFound@");
		}
		// Connector S3
		MADAppRegistration genericConnector = MADAppRegistration.getById(
			Env.getCtx(),
			clientInfo.getFileHandler_ID(),
			null
		);
		if (genericConnector == null || genericConnector.getAD_AppRegistration_ID() <= 0) {
			throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
		}
		//	Load
		IAppSupport supportedApi = AppSupportHandler.getInstance().getAppSupport(genericConnector);
		if (supportedApi == null) {
			throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
		}
		if (!IS3.class.isAssignableFrom(supportedApi.getClass())) {
			throw new AdempiereException("@AD_AppSupport_ID@ @Unsupported@");
		}
		//	Get it
		IS3 fileHandler = (IS3) supportedApi;
		
		// Resource
		ResourceMetadata resourceMetadata = ResourceMetadata.newInstance()
			.withResourceName(fileName)
		;
		InputStream inputStream = fileHandler.getResource(resourceMetadata);
		if (inputStream == null) {
			throw new AdempiereException("@InputStream@ @NotFound@");
		}

		// Charset
		String charsetValue = request.getCharset();
		if (Util.isEmpty(charsetValue, true) || !Charset.isSupported(charsetValue)) {
			charsetValue = Charset.defaultCharset().name();
		}
		Charset charset = Charset.forName(charsetValue);

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

		MTable table = MTable.get(Env.getCtx(), importFormat.getAD_Table_ID());

		ListFilePreviewResponse.Builder builderList = ListFilePreviewResponse.newBuilder()
			.setRecordCount(count)
			.setTableName(
				StringManager.getValidString(
					table.getTableName()
				)
			)
		;

		data.forEach(line -> {
			Struct.Builder lineValues = Struct.newBuilder();

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
					info = format.parseFlexFormat(line, format.getFormatType(), row.getStartNo());
				}

				if (Util.isEmpty(info, true)) {
					if (row.getDefaultValue() != null ) {
						info = row.getDefaultValue();
					}
					else {
						info = "";
					}
				}
				String entry = row.parse(info);
				Value.Builder valueBuilder = Value.newBuilder();
				if(!Util.isEmpty(entry)) {
					try {
						if (row.isDate()) {
							Timestamp dateValue = TimeManager.getTimestampFromString(entry);
							valueBuilder = ValueManager.getValueFromTimestamp(dateValue);
						} else if (row.isNumber()) {
							BigDecimal numberValue = null;
							if (!Util.isEmpty(entry, true)) {
								numberValue = NumberManager.getBigDecimalFromString(entry);
								if (numberValue != null && row.isDivideBy100()) {
									numberValue = numberValue.divide(
										BigDecimal.valueOf(100)
									);
								}
							}
							valueBuilder = ValueManager.getValueFromBigDecimal(numberValue);
						} else {
							valueBuilder = ValueManager.getValueFromString(entry);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				lineValues.putFields(
					row.getColumnName(),
					valueBuilder.build()
				);
			}

			// columns.fo
			builderList.addRecords(lineValues);
		});

		return builderList;
	}


	public static ListLookupItemsResponse.Builder listImportProcesses(ListImportProcessesRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

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

		List<Integer> processFromColumnsList = new Query(
			Env.getCtx(),
			I_AD_Process.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersLit)
			.setOnlyActiveRecords(true)
			.getIDsAsList()
		;

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder()
			.setRecordCount(
				processFromColumnsList.size()
			)
		;
		processFromColumnsList.forEach(processId -> {
			MProcess processDefinition = MProcess.get(
				Env.getCtx(),
				processId
			);
			String displayedValue = processDefinition.getValue() + " - " + processDefinition.getName();
			if (!Env.isBaseLanguage(Env.getCtx(), "")) {
				// set translated values
				displayedValue = processDefinition.getValue() + " - " + processDefinition.get_Translation(I_AD_Process.COLUMNNAME_Name);
			}

			LookupItem.Builder builderItem = LookupUtil.convertObjectFromResult(
				processDefinition.getAD_Process_ID(),
				processDefinition.getUUID(),
				processDefinition.getValue(),
				displayedValue,
				processDefinition.isActive()
			);

			builderItem.setTableName(I_AD_Process.Table_Name);
			builderItem.setId(processDefinition.getAD_Process_ID());
			builderItem.setUuid(
				StringManager.getValidString(
					processDefinition.getUUID()
				)
			);

			builderList.addRecords(builderItem.build());
		});

		return builderList;
	}

}
