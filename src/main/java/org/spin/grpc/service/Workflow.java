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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_WF_Activity;
import org.adempiere.core.domains.models.I_AD_Workflow;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MMenu;
import org.compiere.model.MOrder;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWorkflow;
import org.spin.backend.grpc.common.DocumentAction;
import org.spin.backend.grpc.common.DocumentStatus;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.wf.ForwardRequest;
import org.spin.backend.grpc.wf.ListDocumentActionsRequest;
import org.spin.backend.grpc.wf.ListDocumentActionsResponse;
import org.spin.backend.grpc.wf.ListDocumentStatusesRequest;
import org.spin.backend.grpc.wf.ListDocumentStatusesResponse;
import org.spin.backend.grpc.wf.ListWorkflowActivitiesRequest;
import org.spin.backend.grpc.wf.ListWorkflowActivitiesResponse;
import org.spin.backend.grpc.wf.ListWorkflowsRequest;
import org.spin.backend.grpc.wf.ListWorkflowsResponse;
import org.spin.backend.grpc.wf.ProcessRequest;
import org.spin.backend.grpc.wf.RunDocumentActionRequest;
import org.spin.backend.grpc.wf.WorkflowActivity;
import org.spin.backend.grpc.wf.WorkflowDefinition;
import org.spin.backend.grpc.wf.WorkflowDefinitionRequest;
import org.spin.backend.grpc.wf.WorkflowGrpc.WorkflowImplBase;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.WorkflowUtil;
import org.spin.dictionary.util.DictionaryUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Workflow service
 */
public class Workflow extends WorkflowImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(Workflow.class);


	@Override
	public void getWorkflow(WorkflowDefinitionRequest request, StreamObserver<WorkflowDefinition> responseObserver) {
		try {
			WorkflowDefinition.Builder workflowBuilder = getWorkflow(request);
			responseObserver.onNext(workflowBuilder.build());
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
	 * Request Workflow from uuid or id
	 * @param request
	 * @return builder
	 */
	private WorkflowDefinition.Builder getWorkflow(WorkflowDefinitionRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Workflow_ID@");
		}
		Properties context = Env.getCtx();
		MWorkflow workflow = MWorkflow.get(context, request.getId());
		if (workflow == null || workflow.getAD_Workflow_ID() <= 0) {
			throw new AdempiereException("@AD_Workflow_ID@ @NotFound@");
		}

		//	Add to recent Item
		DictionaryUtil.addToRecentItem(
			MMenu.ACTION_WorkFlow,
			workflow.getAD_Workflow_ID()
		);

		return WorkflowUtil.convertWorkflowDefinition(workflow);
	}



	@Override
	public void listWorkflows(ListWorkflowsRequest request, StreamObserver<ListWorkflowsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Workflow Logs Requested is Null");
			}
			log.fine("Object List Requested = " + request);
			ListWorkflowsResponse.Builder entityValueList = convertWorkflows(Env.getCtx(), request);
			responseObserver.onNext(entityValueList.build());
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
	public void listWorkflowActivities(ListWorkflowActivitiesRequest request,
			StreamObserver<ListWorkflowActivitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Request is Null");
			}
			log.fine("Object List Requested = " + request);
			ListWorkflowActivitiesResponse.Builder activitiesList = listWorkflowActivities(request);
			responseObserver.onNext(activitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	/**
	 * Convert request for workflow to builder
	 * @param request
	 * @return
	 */
	private ListWorkflowActivitiesResponse.Builder listWorkflowActivities(ListWorkflowActivitiesRequest request) {
		if(request.getUserId() <= 0) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}
		//	
		int userId = request.getUserId();
		String whereClause = "AD_WF_Activity.Processed='N' "
				+ "AND AD_WF_Activity.WFState='OS' "
				+ "AND ( AD_WF_Activity.AD_User_ID=? "
				+ "				OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE AD_WF_Activity.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID AND COALESCE(r.AD_User_ID,0)=0 AND COALESCE(r.AD_Role_ID,0)=0 "
				+ "AND (AD_WF_Activity.AD_User_ID=? "
				+ "				OR AD_WF_Activity.AD_User_ID IS NULL)) "
				+ "				OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE AD_WF_Activity.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID AND r.AD_User_ID=?) "
				+ "				OR EXISTS (SELECT * FROM AD_WF_Responsible r INNER JOIN AD_User_Roles ur ON (r.AD_Role_ID=ur.AD_Role_ID) WHERE AD_WF_Activity.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID AND ur.AD_User_ID=?)"
				+ ")";
		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		Query query = new Query(
			Env.getCtx(),
			I_AD_WF_Activity.Table_Name,
			whereClause,
			null
		)
			.setParameters(userId, userId, userId, userId)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;
		int count = query.count();
		List<MWFActivity> workflowActivitiesList = query
				.setLimit(limit, offset)
				.setOrderBy("Priority DESC, Created")
				.<MWFActivity>list();
		//	
		ListWorkflowActivitiesResponse.Builder builder = ListWorkflowActivitiesResponse.newBuilder();
		//	Convert Record Log
		for(MWFActivity workflowActivity : workflowActivitiesList) {
			WorkflowActivity.Builder valueObject = WorkflowUtil.convertWorkflowActivity(workflowActivity);
			builder.addActivities(valueObject.build());
		}
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);
		//	Return
		return builder;
	}



	@Override
	public void listDocumentStatuses(ListDocumentStatusesRequest request,
			StreamObserver<ListDocumentStatusesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Document Statuses Request is Null");
			}
			log.fine("Object List Requested = " + request);
			ListDocumentStatusesResponse.Builder entityValueList = listDocumentStatuses(request);
			responseObserver.onNext(entityValueList.build());
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
	 * Convert document statuses
	 * @param context
	 * @param request
	 * @return
	 */
	private ListDocumentStatusesResponse.Builder listDocumentStatuses(ListDocumentStatusesRequest request) {
		Properties context = Env.getCtx();
		/** Drafted = DR */
		List<String> statusesList = new ArrayList<>();
		String documentStatus = null;
		String documentAction = null;
		String orderType = "--";
		String isSOTrx = "Y";
		Object isProcessing = "N";
		//	New
		int documentTypeId = 0;
		//	Get Table from Name
		MTable table = MTable.get(context, request.getTableName());
		int recordId = 0;
		//	Get entity
		PO entity = RecordUtil.getEntity(context, request.getTableName(), request.getId(), null);
		if(entity != null) {
			//	
			documentStatus = entity.get_ValueAsString(I_C_Order.COLUMNNAME_DocStatus);
			documentAction = entity.get_ValueAsString(I_C_Order.COLUMNNAME_DocAction);
			//
			isProcessing = BooleanManager.getBooleanToString(
				entity.get_ValueAsBoolean(I_C_Order.COLUMNNAME_Processing)
			);
			documentTypeId = entity.get_ValueAsInt(I_C_Order.COLUMNNAME_C_DocTypeTarget_ID);
			if(documentTypeId == 0) {
				documentTypeId = entity.get_ValueAsInt(I_C_Order.COLUMNNAME_C_DocType_ID);
			}
			if(documentTypeId != 0) {
				MDocType documentType = MDocType.get(context, documentTypeId);
				if(documentType != null
						&& !Util.isEmpty(documentType.getDocBaseType())) {
					orderType = documentType.getDocBaseType();
				}
			}
			if (entity.get_ColumnIndex(I_C_Order.COLUMNNAME_IsSOTrx) >= 0) {
				isSOTrx = BooleanManager.getBooleanToString(
					entity.get_ValueAsBoolean(I_C_Order.COLUMNNAME_IsSOTrx)
				);
			}
			recordId = entity.get_ID();
		}
		if (documentStatus == null) {
			/** Drafted = DR */
			documentStatus = DocumentEngine.STATUS_Drafted;
		}
		if (documentAction == null) {
			/** Prepare = PR */
			documentAction = DocumentEngine.ACTION_Prepare;
		}
		//	Standard
		if(documentStatus.equals(DocumentEngine.STATUS_Completed)
				|| documentStatus.equals(DocumentEngine.STATUS_Voided)
				|| documentStatus.equals(DocumentEngine.STATUS_Reversed)
				|| documentStatus.equals(DocumentEngine.STATUS_Unknown)
				|| documentStatus.equals(DocumentEngine.STATUS_Closed)) {
			/** Drafted = DR */
			statusesList.add(DocumentEngine.STATUS_Drafted);
			/** In Progress = IP */
			statusesList.add(DocumentEngine.STATUS_InProgress);
			/** Approved = AP */
			statusesList.add(DocumentEngine.STATUS_Approved);
			//	For Prepaid Order
			if(orderType.equals(MOrder.DocSubTypeSO_Prepay)) {
				/** Waiting Payment = WP */
				statusesList.add(DocumentEngine.STATUS_WaitingPayment);
				/** Waiting Confirmation = WC */
				statusesList.add(DocumentEngine.STATUS_WaitingConfirmation);
			}
		}
		//	Add status
		statusesList.add(documentStatus);
		//	Get All document Actions
		ArrayList<String> valueList = new ArrayList<String>();
		ArrayList<String> nameList = new ArrayList<String>();
		ArrayList<String> descriptionList = new ArrayList<String>();
		//	Load all reference
		readDocumentStatusList(valueList, nameList, descriptionList);
		//	
		ListDocumentStatusesResponse.Builder builder = ListDocumentStatusesResponse.newBuilder();
		statusesList.stream().filter(status -> status != null).forEach(status -> {
			for (int i = 0; i < valueList.size(); i++) {
				if (status.equals(valueList.get(i))) {
					DocumentStatus.Builder documentStatusBuilder = DocumentStatus.newBuilder()
						.setValue(
							ValueManager.validateNull(valueList.get(i))
						)
						.setName(
							ValueManager.validateNull(nameList.get(i))
						)
						.setDescription(
							ValueManager.validateNull(descriptionList.get(i))
						)
					;
					builder.addDocumentStatuses(documentStatusBuilder);
				}
			}
		});
		//	Get Actions
		log.fine("DocStatus=" + documentStatus 
			+ ", DocAction=" + documentAction + ", OrderType=" + orderType 
			+ ", IsSOTrx=" + isSOTrx + ", Processing=" + isProcessing 
			+ ", AD_Table_ID=" + table.getAD_Table_ID() + ", Record_ID=" + recordId);
		//
		String[] options = new String[valueList.size()];
		int index = 0;
		
		/*******************
		 *  General Actions
		 */
		String[] docActionHolder = new String[] {documentAction};
		index = DocumentEngine.getValidActions(documentStatus, isProcessing, orderType, isSOTrx, table.getAD_Table_ID(), docActionHolder, options);

		if (entity != null
				&& entity instanceof DocOptions) {
			index = ((DocOptions) entity).customizeValidActions(documentStatus, isProcessing, orderType, isSOTrx, entity.get_Table_ID(), docActionHolder, options, index);
		}

		log.fine("get doctype: " + documentTypeId);
		if (documentTypeId != 0) {
			index = DocumentEngine.checkActionAccess(Env.getAD_Client_ID(Env.getCtx()), Env.getAD_Role_ID(Env.getCtx()), documentTypeId, options, index);
		}
		//	
		documentAction = docActionHolder[0];
		//	
		//	Get
		Arrays.asList(options).stream().filter(option -> option != null).forEach(option -> {
			for (int i = 0; i < valueList.size(); i++) {
				if (option.equals(valueList.get(i))) {
					DocumentStatus.Builder documentActionBuilder = DocumentStatus.newBuilder()
						.setValue(
							ValueManager.validateNull(valueList.get(i))
						)
						.setName(
							ValueManager.validateNull(nameList.get(i))
						)
						.setDescription(
							ValueManager.validateNull(descriptionList.get(i))
						)
					;
					builder.addDocumentStatuses(documentActionBuilder);
				}
			}
		});
		//	Add record count
		builder.setRecordCount(builder.getDocumentStatusesCount());
		//	Return
		return builder;
	}
	
	/**
	 * Fill Vector with DocAction Ref_List(135) values
	 * @param v_value
	 * @param v_name
	 * @param v_description
	 */
	private void readDocumentStatusList(ArrayList<String> v_value, ArrayList<String> v_name, ArrayList<String> v_description) {
		if (v_value == null) 
			throw new IllegalArgumentException("v_value parameter is null");
		if (v_name == null)
			throw new IllegalArgumentException("v_name parameter is null");
		if (v_description == null)
			throw new IllegalArgumentException("v_description parameter is null");
		
		String sql;
		if (Env.isBaseLanguage(Env.getCtx(), "AD_Ref_List"))
			sql = "SELECT Value, Name, Description FROM AD_Ref_List "
				+ "WHERE AD_Reference_ID=? ORDER BY Name";
		else
			sql = "SELECT l.Value, t.Name, t.Description "
				+ "FROM AD_Ref_List l, AD_Ref_List_Trl t "
				+ "WHERE l.AD_Ref_List_ID=t.AD_Ref_List_ID"
				+ " AND t.AD_Language='" + Env.getAD_Language(Env.getCtx()) + "'"
				+ " AND l.AD_Reference_ID=? ORDER BY t.Name";
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try
		{
			preparedStatement = DB.prepareStatement(sql, null);
			preparedStatement.setInt(1, DocAction.DOCSTATUS_AD_REFERENCE_ID);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next())
			{
				String value = resultSet.getString(1);
				String name = resultSet.getString(2);
				String description = resultSet.getString(3);
				if (description == null)
					description = "";
				//
				v_value.add(value);
				v_name.add(name);
				v_description.add(description);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(resultSet, preparedStatement);
			resultSet = null;
			preparedStatement = null;
		}

	}
	
	/**
	 * Convert request for workflow to builder
	 * @param context
	 * @param request
	 * @return
	 */
	private ListWorkflowsResponse.Builder convertWorkflows(Properties context, ListWorkflowsRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		StringBuffer whereClause = new StringBuffer();
		List<Object> parameters = new ArrayList<>();
		//	
		whereClause
			.append(I_AD_Workflow.COLUMNNAME_AD_Table_ID).append(" = ?");
		//	Set parameters
		parameters.add(table.getAD_Table_ID());
		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		Query query = new Query(context, I_AD_Workflow.Table_Name, whereClause.toString(), null)
				.setParameters(parameters);
		int count = query.count();
		List<MWorkflow> workflowList = query
				.setLimit(limit, offset)
				.<MWorkflow>list();
		//	
		ListWorkflowsResponse.Builder builder = ListWorkflowsResponse.newBuilder();
		//	Convert Record Log
		for(MWorkflow workflowDefinition : workflowList) {
			WorkflowDefinition.Builder valueObject = WorkflowUtil.convertWorkflowDefinition(workflowDefinition);
			builder.addWorkflows(valueObject.build());
		}
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);
		//	Return
		return builder;
	}



	@Override
	public void listDocumentActions(ListDocumentActionsRequest request,
			StreamObserver<ListDocumentActionsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Document Actions Request is Null");
			}
			log.fine("Object List Requested = " + request);
			ListDocumentActionsResponse.Builder entityValueList = listDocumentActions(request);
			responseObserver.onNext(entityValueList.build());
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
	 * Convert document actions
	 * @param context
	 * @param request
	 * @return
	 */
	private ListDocumentActionsResponse.Builder listDocumentActions(ListDocumentActionsRequest request) {
		Properties context = Env.getCtx();
		if(request.getId() <= 0) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);
		//	
		PO entity = RecordUtil.getEntity(context, table.getTableName(), request.getId(), null);
		if (entity == null) {
			throw new AdempiereException("@Error@ @PO@ @NotFound@");
		}

		//	
		String documentStatus = entity.get_ValueAsString(I_C_Order.COLUMNNAME_DocStatus);
		String documentAction = entity.get_ValueAsString(I_C_Order.COLUMNNAME_DocAction);
		//
		Object isProcessing = entity.get_ValueAsBoolean(I_C_Order.COLUMNNAME_Processing)? "Y": "N";
		String orderType = "--";
		int documentTypeId = entity.get_ValueAsInt(I_C_Order.COLUMNNAME_C_DocTypeTarget_ID);
		if(documentTypeId == 0) {
			documentTypeId = entity.get_ValueAsInt(I_C_Order.COLUMNNAME_C_DocType_ID);
		}
		if(documentTypeId != 0) {
			MDocType documentType = MDocType.get(context, documentTypeId);
			if(documentType != null
					&& !Util.isEmpty(documentType.getDocBaseType())) {
				orderType = documentType.getDocBaseType();
			}
		}

		String isSOTrx = "Y";
		if (entity.get_ColumnIndex(I_C_Order.COLUMNNAME_IsSOTrx) >= 0) {
			isSOTrx = BooleanManager.getBooleanToString(
				entity.get_ValueAsBoolean(I_C_Order.COLUMNNAME_IsSOTrx)
			);
		}

		//	
		if (documentStatus == null) {
			throw new AdempiereException("@DocStatus@ @NotFound@");
		}

		//
		ListDocumentActionsResponse.Builder builder = ListDocumentActionsResponse.newBuilder();

		boolean isEndAction = (
			documentStatus.equals(DocumentEngine.STATUS_Closed)
			|| documentStatus.equals(DocumentEngine.STATUS_Voided)
			|| documentStatus.equals(DocumentEngine.STATUS_Reversed)
		);
		if (isEndAction) {
			return builder;
		}

		//	Get All document Actions
		ArrayList<String> valueList = new ArrayList<String>();
		ArrayList<String> nameList = new ArrayList<String>();
		ArrayList<String> descriptionList = new ArrayList<String>();
		//	Load all reference
		DocumentEngine.readReferenceList(valueList, nameList, descriptionList);

		log.fine("DocStatus=" + documentStatus 
			+ ", DocAction=" + documentAction + ", OrderType=" + orderType 
			+ ", IsSOTrx=" + isSOTrx + ", Processing=" + isProcessing 
			+ ", AD_Table_ID=" + entity.get_Table_ID() + ", Record_ID=" + entity.get_ID());
		//
		String[] options = new String[valueList.size()];
		int index = 0;

		/*******************
		 *  General Actions
		 */
		String[] docActionHolder = new String[] {documentAction};
		index = DocumentEngine.getValidActions(documentStatus, isProcessing, orderType, isSOTrx, entity.get_Table_ID(), docActionHolder, options);

		if (entity instanceof DocOptions) {
			index = ((DocOptions) entity).customizeValidActions(documentStatus, isProcessing, orderType, isSOTrx, entity.get_Table_ID(), docActionHolder, options, index);
		}

		log.fine("get doctype: " + documentTypeId);
		if (documentTypeId != 0) {
			index = DocumentEngine.checkActionAccess(Env.getAD_Client_ID(Env.getCtx()), Env.getAD_Role_ID(Env.getCtx()), documentTypeId, options, index);
		}
		//	
		documentAction = docActionHolder[0];

		//	Get
		Arrays.asList(options).stream().filter(option -> option != null).forEach(option -> {
			for (int i = 0; i < valueList.size(); i++) {
				if (option.equals(valueList.get(i))) {
					DocumentAction.Builder documentActionBuilder = ConvertUtil.convertDocumentAction(
							valueList.get(i),
							nameList.get(i),
							descriptionList.get(i)
					);
					builder.addDocumentActions(documentActionBuilder);
				}
			}
		});

		//	setDefault
		if (documentAction.equals("--")) {
			//	If None, suggest closing
			documentAction = DocumentEngine.ACTION_Close;
		}
		String defaultValue = "";
		String defaultName = "";
		String defaultDescription = "";
		for (int i = 0; i < valueList.size() && defaultName.equals(""); i++) {
			if (documentAction.equals(valueList.get(i))) {
				defaultValue = valueList.get(i);
				defaultName = nameList.get(i);
				defaultDescription = descriptionList.get(i);
			}
		}
		//	Set default value
		if (!Util.isEmpty(defaultName)) {
			DocumentAction.Builder documentActionBuilder = ConvertUtil.convertDocumentAction(
				defaultValue,
				defaultName,
				defaultDescription
			);
			builder.setDefaultDocumentAction(
				documentActionBuilder
			);
		}
		//	Add record count
		builder.setRecordCount(builder.getDocumentActionsCount());
		//	Return
		return builder;
	}



	@Override
	public void runDocumentAction(RunDocumentActionRequest request, StreamObserver<ProcessLog> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessLog.Builder processReponse = runDocumentAction(request);
			responseObserver.onNext(processReponse.build());
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
	 * Run a process from request
	 * @param request
	 */
	private ProcessLog.Builder runDocumentAction(RunDocumentActionRequest request) {
		ProcessLog.Builder response = org.spin.base.workflow.WorkflowUtil.startWorkflow(
			request.getTableName(),
			request.getId(),
			request.getDocumentAction()
		);
		return response;
	}


	@Override
	public void process(ProcessRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Empty.Builder processReponse = process(request);
			responseObserver.onNext(processReponse.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private Empty.Builder process(ProcessRequest request) {
		Trx.run(transactionName -> {
			// validate workflow activity
			int workflowActivityId = request.getId();
			if (workflowActivityId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}

			MWFActivity workActivity = new MWFActivity(Env.getCtx(), workflowActivityId, transactionName);
			if (workActivity == null || workActivity.getAD_WF_Activity_ID() <= 0) {
				throw new AdempiereException("@AD_WF_Activity_ID@ @NotFound@");
			}

			String message = ValueManager.validateNull(request.getMessage());
			int userId = Env.getAD_User_ID(Env.getCtx());

			MWFNode node = workActivity.getNode();

			// User Choice - Answer
			if (MWFNode.ACTION_UserChoice.equals(node.getAction())) {
				MColumn column = MColumn.get(Env.getCtx(), node.getAD_Column_ID());
				int displayTypeId = column.getAD_Reference_ID();
				String isApproved = BooleanManager.getBooleanToString(
					request.getIsApproved());
				try {
					workActivity.setUserChoice(userId, isApproved, displayTypeId, message);
				} catch (Exception e) {
					throw new AdempiereException(e.getLocalizedMessage());
				}
			}
			// User Action
			else {
				try {
					workActivity.setUserConfirmation(userId, message);
				} catch (Exception e) {
					throw new AdempiereException(e.getLocalizedMessage());
				}
			}
		});

		return Empty.newBuilder();
	}



	@Override
	public void forward(ForwardRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Empty.Builder processReponse = forward(request);
			responseObserver.onNext(processReponse.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	private Empty.Builder forward(ForwardRequest request) {
		// validate workflow activity
		int workflowActivityId = request.getId();
		if (workflowActivityId <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		MWFActivity workActivity = new MWFActivity(Env.getCtx(), workflowActivityId, null);
		if (workActivity == null || workActivity.getAD_WF_Activity_ID() <= 0) {
			throw new AdempiereException("@AD_WF_Activity_ID@ @NotFound@");
		}

		// validate user
		int userId = request.getUserId();
		if (userId <= 0) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}
		MUser user = MUser.get(Env.getCtx(), userId);
		if (user == null || user.getAD_User_ID() <= 0) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}

		String message = ValueManager.validateNull(
			request.getMessage()
		);
		boolean isSuccefully = workActivity.forwardTo(user.getAD_User_ID(), message);
		if (!isSuccefully) {
			throw new AdempiereException("@CannotForward@");
		}

		return Empty.newBuilder();
	}
}
