/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_AD_PrintFormat;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_C_Order;
import org.compiere.model.MColumn;
import org.compiere.model.MMenu;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MReportView;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.base.db.CountUtil;
import org.spin.base.db.LimitUtil;
import org.spin.base.db.ParameterUtil;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.dictionary.DictionaryUtil;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;
import org.spin.backend.grpc.common.BusinessDataGrpc.BusinessDataImplBase;
import org.spin.backend.grpc.common.CreateEntityRequest;
import org.spin.backend.grpc.common.Criteria;
import org.spin.backend.grpc.common.DeleteEntityRequest;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.common.Entity;
import org.spin.backend.grpc.common.GetEntityRequest;
import org.spin.backend.grpc.common.KeyValue;
import org.spin.backend.grpc.common.KeyValueSelection;
import org.spin.backend.grpc.common.ListEntitiesRequest;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.ReportOutput;
import org.spin.backend.grpc.common.RunBusinessProcessRequest;
import org.spin.backend.grpc.common.UpdateEntityRequest;
import org.spin.backend.grpc.common.Value;

import com.google.protobuf.ByteString;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Business data service
 */
public class BusinessDataServiceImplementation extends BusinessDataImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(BusinessDataServiceImplementation.class);
	/**	Reference cache	*/
	private static CCache<String, String> referenceWhereClauseCache = new CCache<String, String>("Reference_WhereClause", 30, 0);	//	no time-out
	@Override
	public void getEntity(GetEntityRequest request, StreamObserver<Entity> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + request.getUuid());
			Entity.Builder entityValue = getEntity(request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
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
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
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
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void deleteEntity(DeleteEntityRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + request.getUuid());
			
			Empty.Builder entityValue = deleteEntity(Env.getCtx(), request);
			responseObserver.onNext(entityValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listEntities(ListEntitiesRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			
			ListEntitiesResponse.Builder entityValueList = convertEntitiesList(Env.getCtx(), request);
			responseObserver.onNext(entityValueList.build());
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
	public static ProcessLog.Builder runBusinessProcess(RunBusinessProcessRequest request) throws FileNotFoundException, IOException {
		//	Get Process definition
		MProcess process = MProcess.get(Env.getCtx(), RecordUtil.getIdFromUuid(I_AD_Process.Table_Name, request.getProcessUuid(), null));
		if(process == null
				|| process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}
		if(!MRole.getDefault().getProcessAccess(process.getAD_Process_ID())) {
			if (process.isReport()) {
				throw new AdempiereException("@AccessCannotReport@");
			}
			throw new AdempiereException("@AccessCannotProcess@");
		}

		ProcessLog.Builder response = ProcessLog.newBuilder()
			.setUuid(request.getProcessUuid())
		;

		int tableId = 0;
		int recordId = request.getId();
		if (!Util.isEmpty(request.getTableName(), true)) {
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
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
		List<KeyValue> parametersList = new ArrayList<KeyValue>();
		parametersList.addAll(request.getParametersList());
		if ((recordId > 0
				|| !Util.isEmpty(request.getUuid(), true))
				&& !Util.isEmpty(request.getTableName(), true)) {
			String uuid = request.getUuid();
			if (recordId > 0) {
				uuid = null;
			}
			entity = RecordUtil.getEntity(Env.getCtx(), request.getTableName(), uuid, recordId, null);
			if(entity != null) {
				recordId = entity.get_ID();
			}

			// Add record as parameter
			Value.Builder value = Value.newBuilder()
				.setValueType(Value.ValueType.INTEGER)
				.setIntValue(recordId);
			KeyValue.Builder recordParameter = KeyValue.newBuilder()
				.setKey(request.getTableName() + "_ID")
				.setValue(value);
			// set as first position
			parametersList.add(0, recordParameter.build());
		}

		//	Add to recent Item
		DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Process,
			process.getAD_Process_ID()
		);
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
		if(process.isReport()) {
			builder.withReportExportFormat(request.getReportType());
		}
		//	Selection
		if(request.getSelectionsCount() > 0) {
			List<Integer> selectionKeys = new ArrayList<>();
			LinkedHashMap<Integer, LinkedHashMap<String, Object>> selection = new LinkedHashMap<>();
			for(KeyValueSelection selectionKey : request.getSelectionsList()) {
				selectionKeys.add(selectionKey.getSelectionId());
				if(selectionKey.getValuesCount() > 0) {
					selection.put(selectionKey.getSelectionId(), new LinkedHashMap<>(ValueUtil.convertValuesToObjects(selectionKey.getValuesList())));
				}
			}
			builder.withSelectedRecordsIds(request.getTableSelectedId(), selectionKeys, selection);
		}
		//	get document action
		String documentAction = null;
		//	Parameters
		if(request.getParametersCount() > 0) {
			for(KeyValue parameter : parametersList.stream().filter(parameterValue -> !parameterValue.getKey().endsWith("_To")).collect(Collectors.toList())) {
				Object value = ValueUtil.getObjectFromValue(parameter.getValue());
				Optional<KeyValue> maybeToParameter = parametersList.stream().filter(parameterValue -> parameterValue.getKey().equals(parameter.getKey() + "_To")).findFirst();
				if(value != null) {
					if(maybeToParameter.isPresent()) {
						Object valueTo = ValueUtil.getObjectFromValue(maybeToParameter.get().getValue());
						builder.withParameter(parameter.getKey(), value, valueTo);
					} else {
						builder.withParameter(parameter.getKey(), value);
					}
					//	For Document Action
					if(parameter.getKey().equals(I_C_Order.COLUMNNAME_DocAction)) {
						documentAction = (String) value;
					}
				}
			}
		}
		//	For Document
		if(!Util.isEmpty(documentAction)
				&& process.getAD_Workflow_ID() != 0
				&& entity != null
				&& DocAction.class.isAssignableFrom(entity.getClass())) {
			entity.set_ValueOfColumn(I_C_Order.COLUMNNAME_DocAction, documentAction);
			entity.saveEx();
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
				ValueUtil.validateNull(summary)
			);
		}
		String reportViewUuid = null;
		String printFormatUuid = request.getPrintFormatUuid();
		String tableName = null;
		//	Get process instance from identifier
		if(result.getAD_PInstance_ID() != 0) {
			MPInstance instance = new Query(Env.getCtx(), I_AD_PInstance.Table_Name, I_AD_PInstance.COLUMNNAME_AD_PInstance_ID + " = ?", null)
					.setParameters(result.getAD_PInstance_ID())
					.first();
			response.setInstanceUuid(ValueUtil.validateNull(instance.getUUID()));
			response.setLastRun(instance.getUpdated().getTime());
			if(process.isReport()) {
				int printFormatId = 0;
				int reportViewId = 0;
				if(instance.getAD_PrintFormat_ID() != 0) {
					printFormatId = instance.getAD_PrintFormat_ID();
				} else if(process.getAD_PrintFormat_ID() != 0) {
					printFormatId = process.getAD_PrintFormat_ID();
				} else if(process.getAD_ReportView_ID() != 0) {
					reportViewId = process.getAD_ReportView_ID();
				}
				//	Get from report view or print format
				MPrintFormat printFormat = null;
				if(!Util.isEmpty(printFormatUuid)) {
					printFormat = new Query(Env.getCtx(), I_AD_PrintFormat.Table_Name, I_AD_PrintFormat.COLUMNNAME_UUID + " = ?", null)
							.setParameters(printFormatUuid)
							.first();
					tableName = printFormat.getAD_Table().getTableName();
					if(printFormat.getAD_ReportView_ID() != 0) {
						MReportView reportView = MReportView.get(Env.getCtx(), printFormat.getAD_ReportView_ID());
						reportViewUuid = reportView.getUUID();
					}
				} else if(printFormatId != 0) {
					printFormat = MPrintFormat.get(Env.getCtx(), printFormatId, false);
					printFormatUuid = printFormat.getUUID();
					tableName = printFormat.getAD_Table().getTableName();
					if(printFormat.getAD_ReportView_ID() != 0) {
						MReportView reportView = MReportView.get(Env.getCtx(), printFormat.getAD_ReportView_ID());
						reportViewUuid = reportView.getUUID();
					}
				} else if(reportViewId != 0) {
					MReportView reportView = MReportView.get(Env.getCtx(), reportViewId);
					reportViewUuid = reportView.getUUID();
					tableName = reportView.getAD_Table().getTableName();
					printFormat = MPrintFormat.get(Env.getCtx(), reportViewId, 0);
					if(printFormat != null) {
						printFormatUuid = printFormat.getUUID();
					}
				}
			}
		}
		//	Validate print format
		if(Util.isEmpty(printFormatUuid)) {
			printFormatUuid = request.getPrintFormatUuid();
		}
		//	Validate report view
		if(Util.isEmpty(reportViewUuid)) {
			reportViewUuid = request.getReportViewUuid();
		}
		//	
		response.setIsError(result.isError());
		if(!Util.isEmpty(result.getSummary())) {
			response.setSummary(Msg.parseTranslation(Env.getCtx(), result.getSummary()));
		}
		//	
		response.setResultTableName(ValueUtil.validateNull(result.getResultTableName()));
		//	Convert Log
		if(result.getLogList() != null) {
			for(org.compiere.process.ProcessInfoLog log : result.getLogList()) {
				ProcessInfoLog.Builder infoLogBuilder = ConvertUtil.convertProcessInfoLog(log);
				response.addLogs(infoLogBuilder.build());
			}
		}
		//	Verify Output
		if(process.isReport()) {
			File reportFile = Optional.ofNullable(result.getReportAsFile()).orElse(result.getPDFReport());
			if(reportFile != null
					&& reportFile.exists()) {
				String validFileName = getValidName(reportFile.getName());
				ReportOutput.Builder output = ReportOutput.newBuilder();
				output.setFileName(ValueUtil.validateNull(validFileName));
				output.setName(result.getTitle());
				output.setMimeType(ValueUtil.validateNull(MimeType.getMimeType(validFileName)));
				output.setDescription(ValueUtil.validateNull(process.getDescription()));
				//	Type
				String reportType = result.getReportType();
				if(Util.isEmpty(result.getReportType())) {
					reportType = result.getReportType();
				}
				if(!Util.isEmpty(getExtension(validFileName))
						&& !getExtension(validFileName).equals(reportType)) {
					reportType = getExtension(validFileName);
				}
				output.setReportType(request.getReportType());

				ByteString resultFile = ByteString.empty();
				try {
					resultFile = ByteString.readFrom(new FileInputStream(reportFile));
				} catch (IOException e) {
					e.printStackTrace();
					// log.severe(e.getLocalizedMessage());

					if (Util.isEmpty(response.getSummary(), true)) {
						response.setSummary(
							ValueUtil.validateNull(
								e.getLocalizedMessage()
							)
						);
					}
				}
				if(reportType.endsWith("html") || reportType.endsWith("txt")) {
					output.setOutputBytes(resultFile);
				}
				output.setReportType(reportType);
				output.setOutputStream(resultFile);
				output.setReportViewUuid(ValueUtil.validateNull(reportViewUuid));
				output.setPrintFormatUuid(ValueUtil.validateNull(printFormatUuid));
				output.setTableName(ValueUtil.validateNull(tableName));
				response.setOutput(output.build());
			}
		}
		return response;
	}



	/**
	 * Convert Name
	 * @param name
	 * @return
	 */
	private static String getValidName(String fileName) {
		if(Util.isEmpty(fileName)) {
			return "";
		}
		return fileName.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$()*#/><]", "").replaceAll(" ", "-");
	}
	
	/**
	 * get file extension
	 * @param fileName
	 * @return
	 */
	private static String getExtension(String fileName) {
		if(Util.isEmpty(fileName)) {
			return "";
		}
		int index = fileName.lastIndexOf(".");
		if(index <= -1) {
			return "";
		}
		//	return
		return fileName.substring(index + 1);
	}
	
	/**
	 * Convert a PO from query
	 * @param request
	 * @return
	 */
	private Entity.Builder getEntity(GetEntityRequest request) {
		String tableName = request.getTableName();
		if(Util.isEmpty(request.getTableName())) {
			if(request.getCriteria() != null) {
				tableName = request.getCriteria().getTableName();
			}
		}
		PO entity = null;
		if(!Util.isEmpty(request.getUuid())
				|| request.getId() != 0) {
			entity = RecordUtil.getEntity(Env.getCtx(), tableName, request.getUuid(), request.getId(), null);
		} else if(request.getCriteria() != null) {
			List<Object> parameters = new ArrayList<Object>();
			String whereClause = WhereClauseUtil.getWhereClauseFromCriteria(request.getCriteria(), parameters);
			entity = RecordUtil.getEntity(Env.getCtx(), tableName, whereClause, parameters, null);
		}
		//	Return
		return ConvertUtil.convertEntity(entity);
	}
	
	/**
	 * Delete a entity
	 * @param context
	 * @param request
	 * @return
	 */
	private Empty.Builder deleteEntity(Properties context, DeleteEntityRequest request) {
		Trx.run(transactionName -> {
			if(Util.isEmpty(request.getTableName())) {
				throw new AdempiereException("@AD_Table_ID@ @NotFound@");
			}
			
			if (!Util.isEmpty(request.getUuid()) || request.getId() > 0) {
				PO entity = RecordUtil.getEntity(context, request.getTableName(), request.getUuid(), request.getId(), transactionName);
				if (entity != null && entity.get_ID() > 0) {
					entity.deleteEx(true);
				}
			}
			else {
				List<Integer> ids = request.getIdsList();
				if (ids.size() > 0) {
					MTable table = MTable.get(context, request.getTableName());
					ids.stream().forEach(id -> {
						PO entity = table.getPO(id, transactionName);
						if (entity != null && entity.get_ID() > 0) {
							entity.deleteEx(true);
						}
					});
				}
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
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		String tableName = request.getTableName();
		MTable table = MTable.get(context, tableName);
		PO entity = table.getPO(0, null);
		if(entity == null) {
			throw new AdempiereException("@Error@ PO is null");
		}
		request.getAttributesList().forEach(attribute -> {
			int referenceId = DictionaryUtil.getReferenceId(entity.get_Table_ID(), attribute.getKey());
			Object value = null;
			if(referenceId > 0) {
				value = ValueUtil.getObjectFromReference(attribute.getValue(), referenceId);
			} 
			if(value == null) {
				value = ValueUtil.getObjectFromValue(attribute.getValue());
			}
			entity.set_ValueOfColumn(attribute.getKey(), value);
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
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		
		PO entity = RecordUtil.getEntity(context, request.getTableName(), request.getUuid(), request.getId(), null);
		if(entity != null
				&& entity.get_ID() >= 0) {
			request.getAttributesList().forEach(attribute -> {
				int referenceId = DictionaryUtil.getReferenceId(entity.get_Table_ID(), attribute.getKey());
				Object value = null;
				if(referenceId > 0) {
					value = ValueUtil.getObjectFromReference(attribute.getValue(), referenceId);
				} 
				if(value == null) {
					value = ValueUtil.getObjectFromValue(attribute.getValue());
				}
				entity.set_ValueOfColumn(attribute.getKey(), value);
			});
			//	Save entity
			entity.saveEx();
		}
		//	Return
		return ConvertUtil.convertEntity(entity);
	}

	/**
	 * Convert Object to list
	 * @param request
	 * @return
	 */
	private ListEntitiesResponse.Builder convertEntitiesList(Properties context, ListEntitiesRequest request) {
		Criteria criteria = request.getCriteria();
		StringBuffer whereClause = new StringBuffer();
		List<Object> params = new ArrayList<>();
		//	For dynamic condition
		String dynamicWhere = WhereClauseUtil.getWhereClauseFromCriteria(criteria, params);
		if(!Util.isEmpty(dynamicWhere)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			//	Add
			whereClause.append(dynamicWhere);
		}
		//	Add from reference
		if(!Util.isEmpty(criteria.getReferenceUuid())) {
			String referenceWhereClause = referenceWhereClauseCache.get(criteria.getReferenceUuid());
			if(!Util.isEmpty(referenceWhereClause)) {
				if(whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
				whereClause.append("(").append(referenceWhereClause).append(")");
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
		if(Util.isEmpty(criteria.getQuery())) {
			Query query = new Query(context, criteria.getTableName(), whereClause.toString(), null)
					.setParameters(params);
			count = query.count();
			if(!Util.isEmpty(criteria.getOrderByClause())) {
				query.setOrderBy(criteria.getOrderByClause());
			}
			List<PO> entityList = query
					.setLimit(limit, offset)
					.<PO>list();
			//	
			for(PO entity : entityList) {
				Entity.Builder valueObject = ConvertUtil.convertEntity(entity);
				builder.addRecords(valueObject.build());
			}
		} else {
			StringBuilder sql = new StringBuilder(criteria.getQuery());
			if (whereClause.length() > 0) {
				sql.append(" WHERE ").append(whereClause); // includes first AND
			}
			//	
			String parsedSQL = MRole.getDefault().addAccessSQL(sql.toString(),
					null, MRole.SQL_FULLYQUALIFIED,
					MRole.SQL_RO);

			String orderByClause = criteria.getOrderByClause();
			if (!Util.isEmpty(orderByClause, true)) {
				orderByClause = " ORDER BY " + orderByClause;
			}

			//	Count records
			count = CountUtil.countRecords(parsedSQL, criteria.getTableName(), params);
			//	Add Row Number
			parsedSQL = LimitUtil.getQueryWithLimit(parsedSQL, limit, offset);
			//	Add Order By
			parsedSQL = parsedSQL + orderByClause;
			builder = convertListEntitiesResult(MTable.get(context, criteria.getTableName()), parsedSQL, params);
		}
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//	Set netxt page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		//	Return
		return builder;
	}
	
	/**
	 * Convert Entities List
	 * @param table
	 * @param sql
	 * @return
	 */
	private ListEntitiesResponse.Builder convertListEntitiesResult(MTable table, String sql, List<Object> params) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ListEntitiesResponse.Builder builder = ListEntitiesResponse.newBuilder();
		long recordCount = 0;
		try {
			LinkedHashMap<String, MColumn> columnsMap = new LinkedHashMap<>();
			//	Add field to map
			for(MColumn column: table.getColumnsAsList()) {
				columnsMap.put(column.getColumnName().toUpperCase(), column);
			}
			//	SELECT Key, Value, Name FROM ...
			pstmt = DB.prepareStatement(sql, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, params);

			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				Entity.Builder valueObjectBuilder = Entity.newBuilder();
				ResultSetMetaData metaData = rs.getMetaData();
				for (int index = 1; index <= metaData.getColumnCount(); index++) {
					try {
						String columnName = metaData.getColumnName (index);
						MColumn field = columnsMap.get(columnName.toUpperCase());
						Value.Builder valueBuilder = Value.newBuilder();
						//	Display Columns
						if(field == null) {
							String value = rs.getString(index);
							if(!Util.isEmpty(value)) {
								valueBuilder = ValueUtil.getValueFromString(value);
							}
							valueObjectBuilder.putValues(columnName, valueBuilder.build());
							continue;
						}
						//	From field
						String fieldColumnName = field.getColumnName();
						valueBuilder = ValueUtil.getValueFromReference(rs.getObject(index), field.getAD_Reference_ID());
						if(!valueBuilder.getValueType().equals(Value.ValueType.UNRECOGNIZED)) {
							valueObjectBuilder.putValues(fieldColumnName, valueBuilder.build());
						}
					} catch (Exception e) {
						log.severe(e.getLocalizedMessage());
						e.printStackTrace();
					}
				}
				//	
				builder.addRecords(valueObjectBuilder.build());
				recordCount++;
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		} finally {
			DB.close(rs, pstmt);
		}
		//	Set record counts
		builder.setRecordCount(recordCount);
		//	Return
		return builder;
	}
}
