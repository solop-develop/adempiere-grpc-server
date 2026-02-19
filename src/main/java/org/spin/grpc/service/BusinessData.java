/************************************************************************************
 * Copyright (C) 2012-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewDefinition;
import org.compiere.model.*;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.*;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.backend.grpc.common.BusinessDataGrpc.BusinessDataImplBase;
import org.spin.backend.grpc.common.CreateEntityRequest;
import org.spin.backend.grpc.common.DeleteEntitiesBatchRequest;
import org.spin.backend.grpc.common.DeleteEntityRequest;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.GetEntityRequest;
import org.spin.backend.grpc.common.KeyValueSelection;
import org.spin.backend.grpc.common.ListEntitiesRequest;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.RunBusinessProcessRequest;
import org.spin.backend.grpc.common.UpdateEntityRequest;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.AccessUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.workflow.WorkflowUtil;
import org.spin.dictionary.util.BrowserUtil;
import org.spin.dictionary.util.DictionaryUtil;
import org.spin.dictionary.util.WindowUtil;
import org.spin.eca62.support.IS3;
import org.spin.eca62.support.ResourceMetadata;
import org.spin.grpc.service.ui.BrowserLogic;
import org.spin.model.MADAppRegistration;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.base.RecordUtil;
// import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.query.SortingManager;
import org.spin.service.grpc.util.value.CollectionManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Empty;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.spin.util.support.AppSupportHandler;
import org.spin.util.support.IAppSupport;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Business data service
 */
public class BusinessData extends BusinessDataImplBase {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(BusinessData.class);


	@Override
	public void getEntity(GetEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder entityValue = getEntity(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void createEntity(CreateEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Entity.Builder entityValue = createEntity(Env.getCtx(), request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	
	@Override
	public void updateEntity(UpdateEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			Entity.Builder entityValue = updateEntity(Env.getCtx(), request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void deleteEntitiesBatch(DeleteEntitiesBatchRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder entityValue = deleteEntities(Env.getCtx(), request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}


	@Override
	public void runBusinessProcess(RunBusinessProcessRequest request, StreamObserver<ProcessLog> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessLog.Builder processReponse = runBusinessProcess(request);
			responseObserver.onNext(processReponse.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public static ProcessLog.Builder runBusinessProcess(RunBusinessProcessRequest request) throws FileNotFoundException, IOException {
		if(request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		}
		final int processId = request.getId();
		//	Get Process definition
		MProcess process = MProcess.get(
			Env.getCtx(),
			processId
		);
		if(process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ (" + processId + ") @NotFound@");
		}

		// Record/Role access
		boolean isWithAccess = AccessUtil.isProcessAccess(process.getAD_Process_ID());
		if(!isWithAccess) {
			if (process.isReport()) {
				throw new AdempiereException("@AccessCannotReport@: " + process.getDisplayValue());
			}
			throw new AdempiereException("@AccessCannotProcess@: " + process.getDisplayValue());
		}

		if (process.isReport()) {
			return ReportManagement.generateReport(
				processId,
				request.getParameters(),
				request.getReportType(),
				request.getPrintFormatId(),
				request.getReportViewId(),
				request.getIsSummary(),
				request.getTableName(),
				request.getRecordId()
			);
		}

		//	Add to recent Item
		DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Process,
			process.getAD_Process_ID()
		);

		ProcessLog.Builder response = ProcessLog.newBuilder()
			.setId(
				process.getAD_Process_ID()
			);

		int tableId = 0;
		MTable table = null;
		if (!Util.isEmpty(request.getTableName(), true)) {
			table = MTable.get(Env.getCtx(), request.getTableName());
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


		//	browser/window selection by client or generate selection by server
		List<KeyValueSelection> selectionsList = request.getSelectionsList();
		boolean isMultiSelection = false;
		if (process.get_ColumnIndex("SP003_IsMultiSelection") >= 0) {
			isMultiSelection = process.get_ValueAsBoolean("SP003_IsMultiSelection");
		}

		PO entity = null;
		int recordId = request.getRecordId();
		if (isMultiSelection && selectionsList != null && !selectionsList.isEmpty() && selectionsList.size() == 1) {
			// KeyValueSelection oneRow = selectionsList.get(0);
			// recordId = oneRow.getSelectionId();
		} else if (table != null && RecordUtil.isValidId(recordId, table.getAccessLevel())) {
			entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), recordId, null);
			if(entity != null) {
				recordId = entity.get_ID();
			} else {
				recordId = 0;
			}
		}

		//	Validate duplicate execution BEFORE the builder creates AD_PInstance via withRecordId()
		checkDuplicateExecution(processId, recordId, request.getParameters().getFieldsMap());

		//	Call process builder
		ProcessBuilder builder = ProcessBuilder.create(Env.getCtx())
			.process(process.getAD_Process_ID())
			.withTitle(process.getName())
			.withWindowNo(0)
			.withRecordId(tableId, recordId)
			.withoutPrintPreview()
			.withoutBatchMode()
			.withoutTransactionClose()
		;

		if(request.getBrowserId() > 0) {
			MBrowse browse = MBrowse.get(
				Env.getCtx(),
				request.getBrowserId()
			);
			if (browse == null || browse.getAD_Browse_ID() <= 0) {
				throw new AdempiereException("@AD_Browse_ID@ @NotFound@");
			}
			List<Integer> selectionKeys = new ArrayList<>();
			LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();

			if (request.getIsAllSelection()) {
				// get all records march with browser criteria
				selectionsList = BrowserLogic.getAllSelectionByCriteria(
					request.getBrowserId(),
					request.getBrowserContextAttributes(),
					request.getCriteriaFilters(),
					request.getTableName(),
					recordId
				);
			}
			if (selectionsList == null || selectionsList.isEmpty()) {
				throw new AdempiereException("@AD_Browse_ID@ @FillMandatory@ @Selection@");
			}

			Map<String, Integer> displayTypeColumns = BrowserUtil.getBrowseFieldsSelectionDisplayType(browse);
			for(KeyValueSelection selectionKey : selectionsList) {
				selectionKeys.add(selectionKey.getSelectionId());
				if(selectionKey.getValues().getFieldsCount() > 0) {
					LinkedHashMap<String, Object> entities = new LinkedHashMap<String, Object>(
						CollectionManager.getMapObjectFromMapProtoValue(
							selectionKey.getValues().getFieldsMap(),
							displayTypeColumns
						)
					);
					selection.put(
						selectionKey.getSelectionId(),
						entities
					);
				}
			}
			MBrowseField fieldKey = browse.getFieldKey();
			int tableSelectionId = 0;
			String tableAlias = null;
			//	Set Selected Values
			if (fieldKey != null && fieldKey.get_ID() > 0) {
				MViewDefinition viewDefinition = (MViewDefinition) fieldKey.getAD_View_Column().getAD_View_Definition();
				tableSelectionId = viewDefinition.getAD_Table_ID();
				tableAlias = viewDefinition.getTableAlias();
			}
			builder.withSelectedRecordsIds(tableSelectionId, selectionKeys, selection)
				.withSelectedRecordsIds(tableSelectionId, tableAlias, selectionKeys)
			;
		} else if (table != null && isMultiSelection) {
			List<Integer> selectionKeys = new ArrayList<>();
			LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();
			if (selectionsList != null && !selectionsList.isEmpty()) {
				Map<String, Integer> displayTypeColumns = WindowUtil.getTableColumnsDisplayType(table);
				for(KeyValueSelection selectionKey : selectionsList) {
					selectionKeys.add(selectionKey.getSelectionId());
					if(selectionKey.getValues().getFieldsCount() > 0) {
						LinkedHashMap<String, Object> entities = new LinkedHashMap<String, Object>(
							CollectionManager.getMapObjectFromMapProtoValue(
								selectionKey.getValues().getFieldsMap(),
								displayTypeColumns
							)
						);
						selection.put(
							selectionKey.getSelectionId(),
							entities
						);
					}
				}
			}

			if (!selectionKeys.isEmpty()) {
				builder.withSelectedRecordsIds(table.getAD_Table_ID(), selectionKeys, selection)
					.withSelectedRecordsIds(table.getAD_Table_ID(), table.getTableName(), selectionKeys)
				;
			}
		}

		//	get document action
		String documentAction = null;
		//	Parameters
		Map<String, Value> parametersList = new HashMap<String, Value>();
		parametersList.putAll(request.getParameters().getFieldsMap());
		if(request.getParameters().getFieldsCount() > 0) {
			List<Entry<String, Value>> parametersListWithoutRange = parametersList
				.entrySet()
				.parallelStream()
				.filter(parameterValue -> {
					return !parameterValue.getKey().endsWith("_To");
				})
				.collect(Collectors.toList())
			;
			for(Entry<String, Value> parameter : parametersListWithoutRange) {
				final String columnName = parameter.getKey();
				final String columnNameTo = columnName + "_To";
				int displayTypeId = -1;
				boolean isMandatory = false;
				boolean isRange = false;
				MProcessPara processParameter = new Query(
					Env.getCtx(),
					I_AD_Process_Para.Table_Name,
					"AD_Process_ID = ? AND (ColumnName = ? OR ColumnName = ?)",
					null
				)
					.setParameters(process.getAD_Process_ID(), columnName, columnNameTo)
					.first()
				;
				if (processParameter != null) {
					// TODO: validate null or `AD_PInstance` and `Record_ID` columns to break
					displayTypeId = processParameter.getAD_Reference_ID();
					isMandatory = processParameter.isMandatory();
					isRange = processParameter.isRange();
				}

				Object value = null;
				if (displayTypeId > 0) {
					value = ValueManager.getObjectFromProtoValue(
						parameter.getValue(),
						displayTypeId
					);
				} else {
					value = ValueManager.getObjectFromProtoValue(
						parameter.getValue()
					);
				}
				//	For Document Action
				if(value != null && columnName.equals(I_C_Order.COLUMNNAME_DocAction)) {
					documentAction = TextManager.getStringFromObject(value);
				}

				if (!isRange) {
					if (isMandatory && value == null) {
						throw new AdempiereException("@FillMandatory@ @" + columnName + "@");
					}
					//	Get Valid Local file
					value = getValidParameterValue(value, displayTypeId);
					builder.withParameter(columnName, value);
					continue;
				}

				// _To parameter
				Optional<Entry<String, Value>> maybeToParameter = parametersList
					.entrySet()
					.parallelStream()
					.filter(parameterValue -> {
						return parameterValue.getKey().equals(columnNameTo);
					})
					.findFirst()
				;
				Object valueTo = null;
				if(maybeToParameter.isPresent()) {
					if (displayTypeId > 0) {
						valueTo = ValueManager.getObjectFromProtoValue(
							maybeToParameter.get().getValue(),
							displayTypeId
						);
					} else {
						valueTo = ValueManager.getObjectFromProtoValue(
							maybeToParameter.get().getValue()
						);
					}
					//	Get Valid Local file
					valueTo = getValidParameterValue(valueTo, displayTypeId);
				}
				if (isMandatory && (value == null || valueTo == null)) {
					throw new AdempiereException("@FillMandatory@ @" + columnName + "@ / @" + columnName + "@ @To@");
				}
				builder.withParameter(columnName, value, valueTo);
			}
		}
		//	For Document
		if(process.getAD_Workflow_ID() > 0 && !Util.isEmpty(documentAction, true)
			&& entity != null && DocAction.class.isAssignableFrom(entity.getClass())) {
			return WorkflowUtil.startWorkflow(
				request.getTableName(),
				entity.get_ID(),
				documentAction
			);
		}

		//	Execute Process
		ProcessInfo result = null;
		try {
			result = builder.execute();
		} catch (Exception e) {
			e.printStackTrace();
			log.warning(e.getLocalizedMessage());

			result = builder.getProcessInfo();
			//	Set error message
			String summary = result.getSummary();
			if(Util.isEmpty(summary, true)) {
				summary = e.getLocalizedMessage();
			}
			result.setSummary(
				TextManager.getValidString(
					Msg.parseTranslation(
						Env.getCtx(),
						summary
					)
				)
			);
		}

		//	Get process instance from identifier
		if(result.getAD_PInstance_ID() > 0) {
			MPInstance instance = new Query(
				Env.getCtx(),
				I_AD_PInstance.Table_Name,
				I_AD_PInstance.COLUMNNAME_AD_PInstance_ID + " = ?",
				null
			)
				.setParameters(result.getAD_PInstance_ID())
				.first()
			;
			response.setInstanceId(
					instance.getAD_PInstance_ID()
				)
				.setLastRun(
					TimeManager.getProtoTimestampFromTimestamp(
						instance.getUpdated()
					)
				)
			;
		}

		//	
		response.setIsError(result.isError());
		if(!Util.isEmpty(result.getSummary(), true)) {
			response.setSummary(
				TextManager.getValidString(
					Msg.parseTranslation(
						Env.getCtx(),
						result.getSummary()
					)
				)
			);
		}
		//	
		response.setResultTableName(
			TextManager.getValidString(
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

		return response;
	}

	private static Object getValidParameterValue(Object value, int displayTypeId) {
		if(value == null) {
			return null;
		}
		//	Get from S3
		if(displayTypeId == DisplayType.FileName || displayTypeId == DisplayType.FilePath || displayTypeId == DisplayType.FilePathOrName) {
			//	Push to S3
			if(!String.class.isAssignableFrom(value.getClass())) {
				return null;
			}
			String fileName = (String) value;
			//	Push to S3
			try {
				MClientInfo clientInfo = MClientInfo.get(Env.getCtx());
				if(clientInfo.getFileHandler_ID() <= 0) {
					throw new AdempiereException("@FileHandler_ID@ @NotFound@");
				}
				MADAppRegistration genericConnector = MADAppRegistration.getById(
					Env.getCtx(),
					clientInfo.getFileHandler_ID(),
					null
				);
				if(genericConnector == null) {
					throw new AdempiereException("@AD_AppRegistration_ID@ @NotFound@");
				}
				//	Load
				IAppSupport supportedApi = AppSupportHandler.getInstance().getAppSupport(genericConnector);
				if(supportedApi == null) {
					throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
				}
				if(!IS3.class.isAssignableFrom(supportedApi.getClass())) {
					throw new AdempiereException("@AD_AppSupport_ID@ @Unsupported@");
				}
				//	Push it
				IS3 fileHandler = (IS3) supportedApi;
				ResourceMetadata resourceMetadata = ResourceMetadata.newInstance()
					.withClientId(Env.getAD_Client_ID(Env.getCtx()))
					.withUserId(Env.getAD_User_ID(Env.getCtx()))
					.withContainerType(ResourceMetadata.ContainerType.RESOURCE)
					.withContainerId("tmp")
					.withName(fileName)
				;
				InputStream inputStream = fileHandler.getResource(resourceMetadata);
				if (inputStream == null) {
					throw new AdempiereException("@InputStream@ @NotFound@");
				}
				String tempFolder = System.getProperty("java.io.tmpdir");
				File tmpFile = new File(tempFolder + File.separator + fileName);
				Files.copy(inputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				return tmpFile.getAbsolutePath();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return value;
	}

	/**
	 * Validates that the same process with the same parameters has not been recently
	 * executed (within 2 minutes) by the same user and session.
	 * Blocks concurrent execution (IsProcessing=Y) and recently completed duplicate runs.
	 * @param processId  AD_Process_ID
	 * @param recordId   Record_ID used for the process
	 * @param requestParameters parameters map from the gRPC request
	 */
	private static void checkDuplicateExecution(int processId, int recordId, Map<String, Value> requestParameters) {
		int userId = Env.getAD_User_ID(Env.getCtx());
		if (userId <= 0) {
			return;
		}
		Timestamp twoMinutesAgo = new Timestamp(System.currentTimeMillis() - (2L * 60 * 1000));

		StringBuilder whereClause = new StringBuilder(
			"AD_Process_ID = ? AND AD_User_ID = ? AND Record_ID = ? AND Created >= ?"
		);
		List<Object> params = new ArrayList<>();
		params.add(processId);
		params.add(userId);
		params.add(recordId);
		params.add(twoMinutesAgo);

		//	Filter by current session when available
		MSession currentSession = MSession.get(Env.getCtx(), false);
		if (currentSession != null && currentSession.getAD_Session_ID() > 0) {
			whereClause.append(" AND AD_Session_ID = ?");
			params.add(currentSession.getAD_Session_ID());
		}

		List<MPInstance> recentInstances = new Query(
			Env.getCtx(),
			I_AD_PInstance.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(params)
			.list()
		;

		if (recentInstances.isEmpty()) {
			return;
		}

		// TODO: Add support to browser selection and row edits.
		TreeMap<String, String> currentFingerprint = buildParametersFingerprint(requestParameters);
		for (MPInstance recentInstance : recentInstances) {
			//	Block concurrent executions of the same process for the same user/session/record
			if (recentInstance.isProcessing()) {
				throw new AdempiereException("@AD_Process_ID@ @IsProcessing@");
			}
			//	For recently completed instances: compare parameters to detect duplicate submission
			MPInstancePara[] storedParams = recentInstance.getParameters();
			TreeMap<String, String> storedFingerprint = buildStoredParametersFingerprint(storedParams);
			if (currentFingerprint.equals(storedFingerprint)) {
				throw new AdempiereException("@DuplicateProcess@");
			}
		}
	}

	/**
	 * Builds a normalized parameter fingerprint from the gRPC request parameters.
	 * @param parameters proto Value map from the request
	 * @return sorted map of parameter name → string value
	 */
	private static TreeMap<String, String> buildParametersFingerprint(Map<String, Value> parameters) {
		TreeMap<String, String> fingerprint = new TreeMap<>();
		if (parameters == null) {
			return fingerprint;
		}
		for (Map.Entry<String, Value> entry : parameters.entrySet()) {
			Object val = ValueManager.getObjectFromProtoValue(entry.getValue());
			String valStr = "";
			if (val instanceof java.math.BigDecimal) {
				valStr = ((java.math.BigDecimal) val).toPlainString();
			} else if (val != null) {
				valStr = val.toString();
			}
			fingerprint.put(entry.getKey(), valStr);
		}
		return fingerprint;
	}

	/**
	 * Builds a normalized parameter fingerprint from stored AD_PInstance_Para rows.
	 * Each row may carry both the base value and its range (_To) counterpart.
	 * @param params array of MPInstancePara loaded from DB
	 * @return sorted map of parameter name → string value
	 */
	private static TreeMap<String, String> buildStoredParametersFingerprint(MPInstancePara[] params) {
		TreeMap<String, String> fingerprint = new TreeMap<>();
		if (params == null) {
			return fingerprint;
		}
		for (MPInstancePara param : params) {
			String name = param.getParameterName();
			if (Util.isEmpty(name, true)) {
				continue;
			}
			//	Resolve effective base value
			String value = "";
			if (!Util.isEmpty(param.getP_String(), true)) {
				value = param.getP_String();
			} else if (param.getP_Number() != null) {
				value = param.getP_Number().toPlainString();
			} else if (param.getP_Date() != null) {
				value = param.getP_Date().toString();
			}
			fingerprint.put(name, value);

			//	Resolve range (_To) value stored on the same row
			if (!Util.isEmpty(param.getP_String_To(), true)) {
				fingerprint.put(name + "_To", param.getP_String_To());
			} else if (param.getP_Number_To() != null) {
				fingerprint.put(name + "_To", param.getP_Number_To().toPlainString());
			} else if (param.getP_Date_To() != null) {
				fingerprint.put(name + "_To", param.getP_Date_To().toString());
			}
		}
		return fingerprint;
	}


	/**
	 * Convert a PO from query
	 * @param request
	 * @return
	 */
	private Entity.Builder getEntity(GetEntityRequest request) {
		final String tableName = request.getTableName();
		MTable table = RecordUtil.validateAndGetTable(tableName);
		PO entity = null;
		if(RecordUtil.isValidId(request.getId(), table)) {
			entity = RecordUtil.getEntity(Env.getCtx(), tableName, request.getId(), null);
		} else if(request.getFilters() != null) {
			List<Object> parameters = new ArrayList<Object>();
			String whereClause = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), parameters);
			entity = RecordUtil.getEntity(Env.getCtx(), tableName, whereClause, parameters, null);
		}
		//	Return
		return ConvertUtil.convertEntity(entity);
	}



	@Override
	public void deleteEntity(DeleteEntityRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder entityValue = deleteEntity(Env.getCtx(), request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	 * Delete a entity
	 * @param context
	 * @param request
	 * @return
	 */
	private Empty.Builder deleteEntity(Properties context, DeleteEntityRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		Trx.run(transactionName -> {
			PO entity = RecordUtil.getEntity(context, table.getTableName(), request.getId(), transactionName);
			if (entity != null && RecordUtil.isValidId(entity.get_ID(), table.getAccessLevel())) {
				entity.deleteEx(true);
			}
		});
		//	Return
		return Empty.newBuilder();
	}
	
	/**
	 * Delete many entities
	 * @param context
	 * @param request
	 * @return
	 */
	private Empty.Builder deleteEntities(Properties context, DeleteEntitiesBatchRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);
		Trx.run(transactionName -> {
			List<Integer> ids = request.getIdsList();
			if (ids.size() > 0) {
				ids.stream().forEach(id -> {
					PO entity = table.getPO(id, transactionName);
					if (entity != null && entity.get_ID() > 0) {
						entity.deleteEx(true);
					}
				});
			}
		});
		//	Return
		return Empty.newBuilder();
	}
	
	/**
	 * Create Entity
	 * @param context
	 * @param request
	 * @return
	 */
	private Entity.Builder createEntity(Properties context, CreateEntityRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		PO entity = table.getPO(0, null);
		if(entity == null) {
			throw new AdempiereException("@Error@ PO is null");
		}
		Map<String, Value> attributes = request.getAttributes().getFieldsMap();
		attributes.keySet().forEach(key -> {
			Value attribute = attributes.get(key);
			int referenceId = DictionaryUtil.getReferenceId(entity.get_Table_ID(), key);
			Object value = null;
			if(referenceId > 0) {
				value = ValueManager.getObjectFromProtoValue(
					attribute,
					referenceId
				);
			} 
			if(value == null) {
				value = ValueManager.getObjectFromProtoValue(
					attribute
				);
			}
			entity.set_ValueOfColumn(key, value);
		});
		//	Save entity
		entity.saveEx();
		//	Return
		return ConvertUtil.convertEntity(entity);
	}
	
	/**
	 * Update Entity
	 * @param context
	 * @param request
	 * @return
	 */
	private Entity.Builder updateEntity(Properties context, UpdateEntityRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		PO entity = RecordUtil.getEntity(context, table.getTableName(), request.getId(), null);
		if (entity == null) {
			throw new AdempiereException("@Error@ @PO@ @NotFound@");
		}
		PO currentEntity = entity;
		POAdapter adapter = new POAdapter(currentEntity);

		//	Fill context
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextFromPO(
			windowNo, context, entity, false
		);

		final String[] keyColumns = table.getKeyColumns();
		Entity.Builder builder = ConvertUtil.convertEntity(entity);

		if (table.isView()) {
			log.warning("@Ignored@ @AD_Table_ID@ (" + table.getName() + ") : @IsView@ ");
			return builder;
		}

		final int sessionClientId = Env.getAD_Client_ID(context);
		if (sessionClientId != currentEntity.getAD_Client_ID()) {
			log.warning("@Ignored@ : Record is other client");
			return builder;
		}

		// final boolean isActiveRecord = currentEntity.isActive();
		final boolean isProcessedRecord = currentEntity.get_ValueAsBoolean("Processed");
		final boolean isProcessingRecord = currentEntity.get_ValueAsBoolean("Processing");

		Map<String, Value> attributes = request.getAttributes().getFieldsMap();
		attributes.entrySet().forEach(attribute -> {
			final String columnName = attribute.getKey();
			if (Util.isEmpty(columnName, true) || columnName.startsWith(LookupUtil.DISPLAY_COLUMN_KEY) || columnName.endsWith("_" + LookupUtil.UUID_COLUMN_KEY)) {
				return;
			}
			MColumn column = table.getColumn(columnName);
			if (column == null || column.getAD_Column_ID() <= 0) {
				// checks if the column exists in the database
				return;
			}
			final int displayTypeId = column.getAD_Reference_ID();

			String displayValue = ValueManager.getDisplayedValueFromReference(
				context,
				attribute.getValue(),
				columnName,
				displayTypeId,
				column.getAD_Reference_Value_ID()
			);

			if (Arrays.stream(keyColumns).anyMatch(columnName::equals)) {
				// prevent warning `PO.set_Value: Column not updateable`
				log.warning(
					Msg.parseTranslation(
						context,
						"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @IsKey@ "
					)
				);
				return;
			}

			if (!column.isAlwaysUpdateable()) {
				if (!column.isUpdateable()) {
					log.warning(
						Msg.parseTranslation(
							context,
							"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @IsUpdateable@ @NotValid@"
						)
					);
					return;
				}

				if (isProcessedRecord) {
					log.warning(
						Msg.parseTranslation(
							context,
							"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @Processed@ "
						)
					);
					return;
				}
				if (isProcessingRecord) {
					log.warning(
						Msg.parseTranslation(
							context,
							"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @Processing@ "
						)
					);
					return;
				}

				// if (!Util.isEmpty(column.getReadOnlyLogic(), true)) {
				// 	boolean isReadOnlyColumn = Evaluator.evaluateLogic(currentEntity, column.getReadOnlyLogic());
				// 	if (isReadOnlyColumn) {
				// 		log.warning(
				// 			Msg.parseTranslation(
				// 				context,
				// 				"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @ReadOnlyLogic@ "
				// 			)
				// 		);
				// 		return;
				// 	}
				// }

				// if (!columnName.equals(I_AD_Element.COLUMNNAME_IsActive)) {
				// 	if (!isActiveRecord) {
				// 		log.warning(
				// 			Msg.parseTranslation(
				// 				context,
				// 				"@Ignored@ " + column.getName() + " (" + columnName + ") @NewValue@ = " + displayValue + " : @NotActive@ "
				// 			)
				// 		);
				// 		return;
				// 	}
				// }
			}

			Object value = null;
			if (!attribute.getValue().hasNullValue()) {
				if (displayTypeId > 0) {
					value = ValueManager.getObjectFromProtoValue(
						attribute.getValue(),
						displayTypeId
					);
				} 
				if (value == null) {
					value = ValueManager.getObjectFromProtoValue(
						attribute.getValue()
					);
				}
			}
			adapter.set_ValueNoCheck(columnName, value);
		});
		//	Save entity
		currentEntity.saveEx();


		// Reload entity
		builder = ConvertUtil.convertEntity(currentEntity);
		return builder;
	}



	@Override
	public void listEntities(ListEntitiesRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			ListEntitiesResponse.Builder entityValueList = listEntities(Env.getCtx(), request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	private ListEntitiesResponse.Builder listEntities(Properties context, ListEntitiesRequest request) {
		StringBuffer whereClause = new StringBuffer();
		List<Object> params = new ArrayList<>();
		//	For dynamic condition
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), params);
		if(!Util.isEmpty(dynamicWhere)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			//	Add
			whereClause.append(dynamicWhere);
		}

		//	TODO: Add support to this functionality with a distinct scope
		//	Add from reference
		if(!Util.isEmpty(request.getRecordReferenceUuid())) {
			MQuery zoomQuery = org.spin.base.util.RecordUtil.referenceWhereClauseCache.get(
				request.getRecordReferenceUuid()
			);
			//	TODO: When is null refresh cache
			if (zoomQuery != null) {
				final String referenceWhereClause = zoomQuery.getWhereClause();
				if(!Util.isEmpty(referenceWhereClause, true)) {
					if(whereClause.length() > 0) {
						whereClause.append(" AND ");
					}
					whereClause
						.append("(")
						.append(referenceWhereClause)
						.append(")")
					;
				}
			}
		}

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = 0;

		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		//	
//		if(Util.isEmpty(criteria.getQuery())) {
//			Query query = new Query(context, criteria.getTableName(), whereClause.toString(), null)
//					.setParameters(params);
//			count = query.count();
//			if(!Util.isEmpty(criteria.getOrderByClause())) {
//				query.setOrderBy(criteria.getOrderByClause());
//			}
//			List<PO> entityList = query
//					.setLimit(limit, offset)
//					.<PO>list();
//			//	
//			for(PO entity : entityList) {
//				Entity.Builder valueObject = ConvertUtil.convertEntity(entity);
//				builder.addRecords(valueObject.build());
//			}
//		} else {
//			StringBuilder sql = new StringBuilder(criteria.getQuery());
//			if (whereClause.length() > 0) {
//				sql.append(" WHERE ").append(whereClause); // includes first AND
//			}
//			//	
//			String parsedSQL = MRole.getDefault().addAccessSQL(sql.toString(),
//					null, MRole.SQL_FULLYQUALIFIED,
//					MRole.SQL_RO);
//
//			String orderByClause = criteria.getOrderByClause();
//			if (!Util.isEmpty(orderByClause, true)) {
//				orderByClause = " ORDER BY " + orderByClause;
//			}
//
//			//	Count records
//			count = CountUtil.countRecords(parsedSQL, criteria.getTableName(), params);
//			//	Add Row Number
//			parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
//			//	Add Order By
//			parsedSQL = parsedSQL + orderByClause;
//			builder = RecordUtil.convertListEntitiesResult(MTable.get(context, criteria.getTableName()), parsedSQL, params);
//		}
		Query query = new Query(
			context,
			request.getTableName(),
			whereClause.toString(),
			null
		)
			.setParameters(params)
		;
		count = query.count();
		if(!Util.isEmpty(request.getSortBy(), true)) {
			query.setOrderBy(
				SortingManager.newInstance(
					request.getSortBy()
				).getSotingAsSQL()
			);
		}
		List<PO> entityList = query
			.setLimit(limit, offset)
			.<PO>list()
		;
		//	
		for(PO entity : entityList) {
			Entity.Builder valueObject = ConvertUtil.convertEntity(entity);
			builder.addRecords(valueObject.build());
		}
		//
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set netxt page
		builder.setNextPageToken(
			TextManager.getValidString(
				nexPageToken
			)
		);
		//	Return
		return builder;
	}

}
