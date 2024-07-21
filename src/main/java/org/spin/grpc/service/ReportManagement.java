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
package org.spin.grpc.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_AD_PrintFormat;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_AD_ReportView;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMenu;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MQuery;
import org.compiere.model.MReportView;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.model.PO;
import org.compiere.model.PrintInfo;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.ReportOutput;
import org.spin.backend.grpc.report_management.DrillTable;
import org.spin.backend.grpc.report_management.GenerateReportRequest;
import org.spin.backend.grpc.report_management.GetReportOutputRequest;
import org.spin.backend.grpc.report_management.ListDrillTablesRequest;
import org.spin.backend.grpc.report_management.ListDrillTablesResponse;
import org.spin.backend.grpc.report_management.ListPrintFormatsRequest;
import org.spin.backend.grpc.report_management.ListPrintFormatsResponse;
import org.spin.backend.grpc.report_management.ListReportViewsRequest;
import org.spin.backend.grpc.report_management.ListReportViewsResponse;
import org.spin.backend.grpc.report_management.PrintFormat;
import org.spin.backend.grpc.report_management.ReportView;
import org.spin.backend.grpc.report_management.ReportManagementGrpc.ReportManagementImplBase;
import org.spin.base.util.AccessUtil;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.FileUtil;
import org.spin.base.util.RecordUtil;
import org.spin.dictionary.util.DictionaryUtil;
import org.spin.dictionary.util.ReportUtil;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.ASPUtil;

import com.google.protobuf.ByteString;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ReportManagement extends ReportManagementImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(ReportManagement.class);


	
	@Override
	public void generateReport(GenerateReportRequest request, StreamObserver<ProcessLog> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessLog.Builder processReponse = generateReport(request);
			responseObserver.onNext(processReponse.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	/**
	 * Run a process from request
	 * @param request
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static ProcessLog.Builder generateReport(GenerateReportRequest request) throws FileNotFoundException, IOException {
		return generateReport(
			request.getId(),
			request.getParameters(),
			request.getReportType(),
			request.getPrintFormatId(),
			request.getReportViewId(),
			request.getIsSummary(),
			request.getTableName(),
			request.getRecordId()
		);
	}

	public static ProcessLog.Builder generateReport(
		int reportId, com.google.protobuf.Struct parameters, String reportType,
		int printFormatId, int reportViewId, boolean isSummary,
		String tableName, int recordId
	) {
		if(reportId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		}
		//	Get Process definition
		MProcess process = ASPUtil.getInstance().getProcess(reportId);
		if (process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@Report@ @NotFound@");
		}
		if (!process.isReport()) {
			throw new AdempiereException("@WFA.NotReport@");
		}
		// Record/Role access
		boolean isReportAccess = AccessUtil.isProcessAccess(process.getAD_Process_ID());
		if(!isReportAccess) {
			throw new AdempiereException("@AccessCannotReport@");
		}

		ProcessLog.Builder response = ProcessLog.newBuilder()
			.setId(
				process.getAD_Process_ID()
			);

		int tableId = 0;
		MTable table = null;
		if (!Util.isEmpty(tableName, true)) {
			table = MTable.get(Env.getCtx(), tableName);
			if (table != null && table.getAD_Table_ID() > 0) {
				tableId = table.getAD_Table_ID();

				if (table.getAD_Window_ID() > 0) {
					//	Add to recent Item
					DictionaryUtil.addToRecentItem(
						MMenu.ACTION_Window,
						table.getAD_Window_ID()
					);
				}
			}
		}
		PO entity = null;
		if (table != null && RecordUtil.isValidId(recordId, table.getAccessLevel())) {
			entity = RecordUtil.getEntity(Env.getCtx(), tableName, recordId, null);
			if(entity != null) {
				recordId = entity.get_ID();
			}
		}

		//	Call process builder
		ProcessBuilder builder = ProcessBuilder.create(Env.getCtx())
			.process(process.getAD_Process_ID())
			.withRecordId(tableId, recordId)
			.withoutPrintPreview()
			.withoutBatchMode()
			.withWindowNo(0)
			.withTitle(process.getName())
			.withoutTransactionClose()
		;
		//	Set Report Export Type
		if(Util.isEmpty(reportType, true)) {
			reportType = ReportUtil.DEFAULT_REPORT_TYPE;
		}
		builder.withReportExportFormat(reportType);

		//	Parameters
		Map<String, Value> parametersList = new HashMap<String, Value>();
		parametersList.putAll(parameters.getFieldsMap());
		if(parameters.getFieldsCount() > 0) {
			List<Entry<String, Value>> parametersListWithoutRange = parametersList.entrySet().parallelStream()
				.filter(parameterValue -> {
					return !parameterValue.getKey().endsWith("_To");
				})
				.collect(Collectors.toList());
			for(Entry<String, Value> parameter : parametersListWithoutRange) {
				final String columnName = parameter.getKey();
				int displayTypeId = -1;
				MProcessPara processParameter = new Query(
					Env.getCtx(),
					I_AD_Process_Para.Table_Name,
					"AD_Process_ID = ? AND (ColumnName = ? OR ColumnName = ?)",
					null
				)
					.setParameters(process.getAD_Process_ID(), columnName, columnName + "_To")
					.first()
				;
				if (processParameter != null) {
					displayTypeId = processParameter.getAD_Reference_ID();
				}

				Object value = null;
				if (displayTypeId > 0) {
					value = ValueManager.getObjectFromReference(parameter.getValue(), displayTypeId);
				} else {
					value = ValueManager.getObjectFromValue(parameter.getValue());
				}
				Optional<Entry<String, Value>> maybeToParameter = parametersList.entrySet().parallelStream()
					.filter(parameterValue -> {
						return parameterValue.getKey().equals(columnName + "_To");
					})
					.findFirst();
				if(value != null) {
					if(maybeToParameter.isPresent()) {
						Object valueTo = null;
						if (displayTypeId > 0) {
							valueTo = ValueManager.getObjectFromReference(maybeToParameter.get().getValue(), displayTypeId);
						} else {
							valueTo = ValueManager.getObjectFromValue(maybeToParameter.get().getValue());
						}
						builder.withParameter(columnName, value, valueTo);
					} else {
						builder.withParameter(columnName, value);
					}
				}
			}
		}

		//	Execute Process
		ProcessInfo result = null;
		try {
			result = builder.execute();
		} catch (Exception e) {
			e.printStackTrace();
			// log.severe(e.getLocalizedMessage());

			result = builder.getProcessInfo();
			//	Set error message
			String summary = Msg.parseTranslation(Env.getCtx(), result.getSummary());
			if(Util.isEmpty(summary, true)) {
				summary = e.getLocalizedMessage();
			}
			result.setSummary(
				ValueManager.validateNull(summary)
			);
		}

		int reportViewReferenceId = 0;
		int printFormatReferenceId = printFormatId;
		String tableNameReference = null;

		//	Get process instance from identifier
		if(result.getAD_PInstance_ID() > 0) {
			MPInstance instance = new Query(
				Env.getCtx(),
				I_AD_PInstance.Table_Name,
				I_AD_PInstance.COLUMNNAME_AD_PInstance_ID + " = ?",
				null
			)
				.setParameters(result.getAD_PInstance_ID())
				.first();
			response.setInstanceId(instance.getAD_PInstance_ID());

			response.setLastRun(
				ValueManager.getTimestampFromDate(
					instance.getUpdated()
				)
			);

			if(instance.getAD_PrintFormat_ID() > 0) {
				printFormatId = instance.getAD_PrintFormat_ID();
			} else if(process.getAD_PrintFormat_ID() > 0) {
				printFormatReferenceId = process.getAD_PrintFormat_ID();
			} else if(process.getAD_ReportView_ID() > 0) {
				reportViewId = process.getAD_ReportView_ID();
			}

			if (printFormatReferenceId <= 0 && printFormatId > 0) {
				printFormatReferenceId = printFormatId;
			}
			//	Get from report view or print format
			MPrintFormat printFormat = null;
			if(printFormatReferenceId > 0) {
				printFormat = MPrintFormat.get(Env.getCtx(), printFormatReferenceId, false);
				tableNameReference = printFormat.getAD_Table().getTableName();
				if(printFormat.getAD_ReportView_ID() > 0) {
					reportViewReferenceId = printFormat.getAD_ReportView_ID();
				}
			} else if(reportViewId > 0) {
				MReportView reportView = MReportView.get(Env.getCtx(), reportViewId);
				reportViewReferenceId = reportViewId;
				tableNameReference = reportView.getAD_Table().getTableName();
				printFormat = MPrintFormat.get(Env.getCtx(), reportViewId, 0);
				if(printFormat != null) {
					printFormatReferenceId = printFormat.getAD_PrintFormat_ID();
				}
			}
		}
		if (Util.isEmpty(tableNameReference, true)) {
			tableNameReference = tableName;
		}

		//	
		response.setIsError(result.isError());
		if(!Util.isEmpty(result.getSummary())) {
			response.setSummary(Msg.parseTranslation(Env.getCtx(), result.getSummary()));
		}
		//	
		response.setResultTableName(
			ValueManager.validateNull(
				result.getResultTableName()
			)
		);
		//	Convert Log
		if(result.getLogList() != null) {
			for(org.compiere.process.ProcessInfoLog log : result.getLogList()) {
				ProcessInfoLog.Builder infoLogBuilder = ConvertUtil.convertProcessInfoLog(log);
				response.addLogs(infoLogBuilder.build());
			}
		}
		//	Verify Output
		response = addReportOutput(
			response,
			result,
			process,
			reportType,
			printFormatReferenceId,
			reportViewReferenceId,
			tableNameReference
		);

		return response;
	}


	public static ProcessLog.Builder addReportOutput(
		ProcessLog.Builder processBuilder,
		ProcessInfo processInfo,
		MProcess process,
		String reportType,
		int printFormatReferenceId,
		int reportViewReferenceId,
		String tableName
	) {
		//	Verify Output
		File reportFile = Optional.ofNullable(processInfo.getReportAsFile()).orElse(processInfo.getPDFReport());
		if(reportFile != null && reportFile.exists()) {
			String validFileName = FileUtil.getValidFileName(reportFile.getName());
			ReportOutput.Builder output = ReportOutput.newBuilder()
				.setId(
					processInfo.getAD_PInstance_ID()
				)
				.setFileName(
					ValueManager.validateNull(
						validFileName
					)
				)
				.setName(
					ValueManager.validateNull(
						processInfo.getTitle()
					)
				)
				.setMimeType(
					ValueManager.validateNull(
						MimeType.getMimeType(
							validFileName
						)
					)
				)
				.setDescription(
					ValueManager.validateNull(
						process.getDescription()
					)
				)
			;

			//	Report Type
			if(Util.isEmpty(reportType, true)) {
				reportType = processInfo.getReportType();
			}
			if(!Util.isEmpty(FileUtil.getExtension(validFileName))
					&& !FileUtil.getExtension(validFileName).equals(reportType)) {
				reportType = FileUtil.getExtension(validFileName);
			}
			if (Util.isEmpty(reportType, true)) {
				reportType = ReportUtil.DEFAULT_REPORT_TYPE;
			}
			output.setReportType(
				processInfo.getReportType()
			);

			ByteString resultFile = ByteString.empty();
			try {
				resultFile = ByteString.readFrom(new FileInputStream(reportFile));
			} catch (IOException e) {
				e.printStackTrace();
				// log.severe(e.getLocalizedMessage());

				if (Util.isEmpty(processBuilder.getSummary(), true)) {
					processBuilder.setSummary(
						ValueManager.validateNull(
							e.getLocalizedMessage()
						)
					);
				}
			}
			if(reportType.endsWith("html") || reportType.endsWith("txt")) {
				output.setOutputBytes(resultFile);
			}
			output.setReportType(reportType)
				.setOutputStream(resultFile)
				.setReportViewId(reportViewReferenceId)
				.setPrintFormatId(printFormatReferenceId)
				.setTableName(
					ValueManager.validateNull(tableName)
				)
			;
			processBuilder.setOutput(output.build());
		}

		return processBuilder;
	}


	@Override
	public void getReportOutput(GetReportOutputRequest request, StreamObserver<ReportOutput> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ReportOutput.Builder reportOutput = getReportOutput(request);
			responseObserver.onNext(reportOutput.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException())
			;
		}
	}

	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException 
	 */
	private ReportOutput.Builder getReportOutput(GetReportOutputRequest request) throws FileNotFoundException, IOException {
		if (request.getReportId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		}
		int processId = request.getReportId();
		MProcess process = ASPUtil.getInstance().getProcess(processId);
		if (process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@Report@ @NotFound@");
		}
		if (!process.isReport()) {
			throw new AdempiereException("@WFA.NotReport@");
		}
		// Record/Role access
		boolean isReportAccess = AccessUtil.isProcessAccess(process.getAD_Process_ID());
		if(!isReportAccess) {
			throw new AdempiereException("@AccessCannotReport@");
		}

		if(Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @TableName@");
		}
		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@TableName@ @NotFound@");
		}
		//	
		if(!MRole.getDefault().isCanReport(table.getAD_Table_ID())) {
			throw new AdempiereException("@AccessCannotReport@");
		}

		//	Validate print format
		if(request.getPrintFormatId() <= 0 && request.getReportViewId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_PrintFormat_ID@ / @AD_ReportView_ID@");
		}

		//	
		PrintInfo printInformation = new PrintInfo(request.getReportName(), table.getAD_Table_ID(), 0, 0);

		//	Get Report View
		MReportView reportView = null;
		if(request.getReportViewId() > 0) {
			reportView = new Query(
				Env.getCtx(),
				I_AD_ReportView.Table_Name,
				I_AD_ReportView.COLUMNNAME_AD_ReportView_ID + " = ?",
				null
			)
				.setParameters(request.getReportViewId())
				.first();
		}

		//	Get Print Format
		MPrintFormat printFormat = null;
		if (request.getPrintFormatId() > 0) {
			printFormat = new Query(
				Env.getCtx(),
				I_AD_PrintFormat.Table_Name,
				I_AD_PrintFormat.COLUMNNAME_AD_PrintFormat_ID + " = ?",
				null
			)
				.setParameters(request.getPrintFormatId())
				.first();
		}
		//	Get Default
		if(printFormat == null || printFormat.getAD_PrintFormat_ID() <= 0) {
			if (reportView != null) {
				printFormat = MPrintFormat.get(Env.getCtx(), reportView.getAD_ReportView_ID(), table.getAD_Table_ID());
			}
		}
		//	Validate print format
		if(printFormat == null || printFormat.getAD_PrintFont_ID() <= 0) {
			throw new AdempiereException("@AD_PrintFormat_ID@ @NotFound@");
		}

		if (table == null || table.getAD_Table_ID() != printFormat.getAD_Table_ID()) {
			table = MTable.get(Env.getCtx(), printFormat.getAD_Table_ID());
		}

		//	
		MQuery query = ReportUtil.getReportQueryFromCriteria(
			process.getAD_Process_ID(),
			request.getTableName(),
			request.getFilters()
		);

		//	Run report engine
		ReportEngine reportEngine = new ReportEngine(Env.getCtx(), printFormat, query, printInformation);
		//	Set report view
		if(reportView != null) {
			reportEngine.setAD_ReportView_ID(reportView.getAD_ReportView_ID());
		} else {
			reportView = MReportView.get(Env.getCtx(), reportEngine.getAD_ReportView_ID());
		}
		//	Set Summary
		reportEngine.setSummary(
			request.getIsSummary()
		);

		//	Set Report Export Type
		String reportType = request.getReportType();
		if (Util.isEmpty(reportType, true)) {
			reportType = ReportUtil.DEFAULT_REPORT_TYPE;
		}

		ReportOutput.Builder builder = ReportOutput.newBuilder()
			.setId(
				printInformation.getAD_PInstance_ID()
			)
			.setReportType(reportType)
		;
		//	
		File reportFile = ReportUtil.createOutput(reportEngine, reportType);
		if (reportFile != null && reportFile.exists()) {
			String validFileName = FileUtil.getValidFileName(
				reportFile.getName()
			);
			builder.setFileName(
					ValueManager.validateNull(validFileName)
				)
				.setName(
					ValueManager.validateNull(
						reportEngine.getName()
					)
				)
				.setMimeType(
					ValueManager.validateNull(
						MimeType.getMimeType(validFileName)
					)
				)
			;
			// Header
			String headerName = Msg.getMsg(Env.getCtx(), "Report") + ": " + reportEngine.getName() + "  " + Env.getHeader(Env.getCtx(), 0);
			builder.setHeaderName(
				ValueManager.validateNull(headerName)
			);
			// Footer
			StringBuffer footerName = new StringBuffer ();
			footerName.append(Msg.getMsg(Env.getCtx(), "DataCols")).append("=")
				.append(reportEngine.getColumnCount())
				.append(", ").append(Msg.getMsg(Env.getCtx(), "DataRows")).append("=")
				.append(reportEngine.getRowCount())
			;
			builder.setFooterName(
				ValueManager.validateNull(
					footerName.toString()
				)
			);
			ByteString resultFile = ByteString.readFrom(new FileInputStream(reportFile));
			if (reportType.endsWith("html") || reportType.endsWith("txt")) {
				builder.setOutputBytes(resultFile);
			}
			if(reportView != null) {
				builder.setReportViewId(
					reportView.getAD_ReportView_ID()
				);
			}
			builder.setPrintFormatId(
					printFormat.getAD_PrintFormat_ID()
				)
				.setTableName(
					ValueManager.validateNull(
						table.getTableName()
					)
				)
				.setOutputStream(resultFile)
			;
		}
		//	Return
		return builder;
	}



	@Override
	public void listPrintFormats(ListPrintFormatsRequest request, StreamObserver<ListPrintFormatsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPrintFormatsResponse.Builder printFormatsList = listPrintFormats(Env.getCtx(), request);
			responseObserver.onNext(printFormatsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert print formats to gRPC
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	private ListPrintFormatsResponse.Builder listPrintFormats(Properties context, ListPrintFormatsRequest request) {
		//	Get entity
		if(Util.isEmpty(request.getTableName())
				&& request.getReportId() <= 0
				&& request.getReportViewId() <= 0) {
			throw new AdempiereException("@TableName@ / @AD_Process_ID@ / @AD_ReportView_ID@ @NotFound@");
		}
		String whereClause = null;
		List<Object> parameters = new ArrayList<>();
		//	For Table Name
		if(!Util.isEmpty(request.getTableName())) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			whereClause = "AD_Table_ID = ?";
			parameters.add(table.getAD_Table_ID());
		} else if(request.getReportId() > 0) {
			whereClause = "EXISTS(SELECT 1 FROM AD_Process p WHERE p.AD_Process_ID = ? AND (p.AD_PrintFormat_ID = AD_PrintFormat.AD_PrintFormat_ID OR p.AD_ReportView_ID = AD_PrintFormat.AD_ReportView_ID))";
			parameters.add(request.getReportId());
		} else if(request.getReportViewId() > 0) {
			MReportView reportView = new Query(Env.getCtx(), I_AD_ReportView.Table_Name, I_AD_ReportView.COLUMNNAME_AD_ReportView_ID + " = ?", null)
				.setParameters(request.getReportViewId())
				.first();
			whereClause = "AD_ReportView_ID = ?";
			parameters.add(reportView.getUUID());
		}
		//	Get List
		Query query = new Query(
			Env.getCtx(),
			I_AD_PrintFormat.Table_Name,
			whereClause,
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;
		ListPrintFormatsResponse.Builder builder = ListPrintFormatsResponse.newBuilder()
			.setRecordCount(query.count())
		;

		query.setOrderBy(I_AD_PrintFormat.COLUMNNAME_Name)
			.getIDsAsList().forEach(printFormatId -> {
				MPrintFormat printFormat = MPrintFormat.get(Env.getCtx(), printFormatId, false);

				PrintFormat.Builder printFormatBuilder = PrintFormat.newBuilder()
					.setId(printFormat.getAD_PrintFormat_ID())
					.setName(
						ValueManager.validateNull(
							printFormat.getName()
						)
					)
					.setDescription(
						ValueManager.validateNull(
							printFormat.getDescription()
						)
					)
					.setIsDefault(printFormat.isDefault())
				;
				MTable table = MTable.get(Env.getCtx(), printFormat.getAD_Table_ID());
				printFormatBuilder.setTableName(
					ValueManager.validateNull(
						table.getTableName()
					)
				);
				if(printFormat.getAD_ReportView_ID() != 0) {
					printFormatBuilder.setReportViewId(printFormat.getAD_ReportView_ID());
				}
				//	add
				builder.addPrintFormats(printFormatBuilder);
			});

		//	Return
		return builder;
	}



	@Override
	public void listReportViews(ListReportViewsRequest request, StreamObserver<ListReportViewsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			ListReportViewsResponse.Builder reportViewsList = listReportViews(request);
			responseObserver.onNext(reportViewsList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * Convert Report View to gRPC
	 * @param request
	 * @return
	 */
	private ListReportViewsResponse.Builder listReportViews(ListReportViewsRequest request) {
		Properties context = Env.getCtx();
		String whereClause = null;
		List<Object> parameters = new ArrayList<>();
		//	For Table Name
		if(!Util.isEmpty(request.getTableName(), true)) {
			MTable table = MTable.get(context, request.getTableName());
			if(table == null || table.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@TableName@ @NotFound@");
			}
			whereClause = "AD_Table_ID = ?";
			parameters.add(table.getAD_Table_ID());
		} else if(request.getReportId() > 0) {
			whereClause = "EXISTS(SELECT 1 FROM AD_Process p WHERE p.AD_Process_ID = ? AND p.AD_ReportView_ID = AD_ReportView.AD_ReportView_ID)";
			parameters.add(request.getReportId());
		} else {
			throw new AdempiereException("@TableName@ / @AD_Process_ID@ @NotFound@");
		}
		String language = context.getProperty(Env.LANGUAGE);
		//	Get List
		Query query = new Query(
			context,
			I_AD_ReportView.Table_Name,
			whereClause,
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		ListReportViewsResponse.Builder builder = ListReportViewsResponse.newBuilder()
			.setRecordCount(query.count())
		;
		query.setOrderBy(I_AD_ReportView.COLUMNNAME_PrintName + ", " + I_AD_ReportView.COLUMNNAME_Name)
			.getIDsAsList().forEach(reportViewId -> {
				MReportView reportView = MReportView.get(context, reportViewId);

				String name = reportView.getName();
				String description = reportView.getDescription();
				// add translation
				if(!Util.isEmpty(language) && !Env.isBaseLanguage(Env.getCtx(), "")) {
					/*
					String translation = reportViewReference.get_Translation("Name");
					if(!Util.isEmpty(translation)) {
						name = translation;
					}
					translation = reportViewReference.get_Translation("Description");
					if(!Util.isEmpty(translation)) {
						description = translation;
					}
					*/

					// TODO: Remove with fix the issue https://github.com/solop-develop/backend/issues/31
					PO translation = new Query(
						context,
						I_AD_ReportView.Table_Name + "_Trl",
						I_AD_ReportView.COLUMNNAME_AD_ReportView_ID + " = ? AND " +
						"IsTranslated = ? AND AD_Language = ?",
						null
					)
						.setParameters(reportView.get_ID(), "Y", language)
						.setOnlyActiveRecords(true)
						.first();

					if (translation != null) {
						String nameTranslated = translation.get_ValueAsString(I_AD_ReportView.COLUMNNAME_Name);
						if(!Util.isEmpty(nameTranslated)) {
							name = nameTranslated;
						}
						String desciptionTranslated = translation.get_ValueAsString(I_AD_ReportView.COLUMNNAME_Description);
						if(!Util.isEmpty(desciptionTranslated)) {
							description = desciptionTranslated;
						}
					}
				}

				ReportView.Builder reportViewBuilder = ReportView.newBuilder()
					.setId(reportView.getAD_ReportView_ID())
					.setName(
						ValueManager.validateNull(name)
					)
					.setDescription(
						ValueManager.validateNull(description)
					)
				;
				MTable table = MTable.get(context, reportView.getAD_Table_ID());
				reportViewBuilder.setTableName(
					ValueManager.validateNull(
						table.getTableName()
					)
				);
				//	add
				builder.addReportViews(reportViewBuilder);
			});
		//	Return
		return builder;
	}



	@Override
	public void listDrillTables(ListDrillTablesRequest request, StreamObserver<ListDrillTablesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListDrillTablesResponse.Builder drillTablesList = listDrillTables(request);
			responseObserver.onNext(drillTablesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	/**
	 * Convert Report View to gRPC
	 * @param request
	 * @return
	 */
	private ListDrillTablesResponse.Builder listDrillTables(ListDrillTablesRequest request) {
		ListDrillTablesResponse.Builder builder = ListDrillTablesResponse.newBuilder();
		//	Get entity
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@TableName@ @NotFound@");
		}
		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		String sql = "SELECT t.TableName, e.ColumnName, NULLIF(e.PO_PrintName,e.PrintName) "
				+ "FROM AD_Column c "
				+ " INNER JOIN AD_Column used ON (c.ColumnName=used.ColumnName)"
				+ " INNER JOIN AD_Table t ON (used.AD_Table_ID=t.AD_Table_ID AND t.IsView='N' AND t.AD_Table_ID <> c.AD_Table_ID)"
				+ " INNER JOIN AD_Column cKey ON (t.AD_Table_ID=cKey.AD_Table_ID AND cKey.IsKey='Y')"
				+ " INNER JOIN AD_Element e ON (cKey.ColumnName=e.ColumnName) "
				+ "WHERE c.AD_Table_ID=? AND c.IsKey='Y' "
				+ "ORDER BY 3";
			PreparedStatement pstmt = null;
			ResultSet resultSet = null;
			try {
				pstmt = DB.prepareStatement(sql, null);
				pstmt.setInt(1, table.getAD_Table_ID());
				resultSet = pstmt.executeQuery();
				int recordCount = 0;
				while (resultSet.next()) {
					String drillTableName = resultSet.getString("TableName");
					String columnName = resultSet.getString("ColumnName");
					M_Element element = M_Element.get(Env.getCtx(), columnName);
					//	Add here
					DrillTable.Builder drillTable = DrillTable.newBuilder();
					drillTable.setTableName(
						ValueManager.validateNull(drillTableName)
					);
					String name = element.getPrintName();
					String poName = element.getPO_PrintName();
					if(!Env.isBaseLanguage(Env.getCtx(), "")) {
						String translation = element.get_Translation("PrintName");
						if(!Util.isEmpty(translation)) {
							name = translation;
						}
						translation = element.get_Translation("PO_PrintName");
						if(!Util.isEmpty(translation)) {
							poName = translation;
						}
					}
					if(!Util.isEmpty(poName)) {
						name = name + "/" + poName;
					}
					//	Print Name
					drillTable.setPrintName(
						ValueManager.validateNull(name)
					);
					recordCount++;
					//	Add to list
					builder.addDrillTables(drillTable);
				}
				builder.setRecordCount(recordCount);
				resultSet.close();
				pstmt.close();
			} catch (SQLException e) {
				log.log(Level.SEVERE, sql, e);
			} finally {
				DB.close(resultSet, pstmt);
			}
		//	Return
		return builder;
	}

}
