/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_PInstance;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_AD_WF_Activity;
import org.compiere.model.I_AD_WF_Node;
import org.compiere.model.I_AD_Workflow;
import org.compiere.model.I_C_Order;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWorkflow;
import org.eevolution.service.dsl.ProcessBuilder;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.DictionaryUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.base.util.WorkflowUtil;
import org.spin.backend.grpc.client.ClientRequest;
import org.spin.backend.grpc.common.DocumentStatus;
import org.spin.backend.grpc.common.ProcessInfoLog;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.wf.ListDocumentActionsRequest;
import org.spin.backend.grpc.wf.ListDocumentActionsResponse;
import org.spin.backend.grpc.wf.ListDocumentStatusesRequest;
import org.spin.backend.grpc.wf.ListDocumentStatusesResponse;
import org.spin.backend.grpc.wf.ListWorkflowActivitiesRequest;
import org.spin.backend.grpc.wf.ListWorkflowActivitiesResponse;
import org.spin.backend.grpc.wf.ListWorkflowsRequest;
import org.spin.backend.grpc.wf.ListWorkflowsResponse;
import org.spin.backend.grpc.wf.RunDocumentActionRequest;
import org.spin.backend.grpc.wf.WorkflowActivity;
import org.spin.backend.grpc.wf.WorkflowDefinition;
import org.spin.backend.grpc.wf.WorkflowDefinitionRequest;
import org.spin.backend.grpc.wf.WorkflowGrpc.WorkflowImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * https://itnext.io/customizing-grpc-generated-code-5909a2551ca1
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Workflow service
 */
public class WorkflowServiceImplementation extends WorkflowImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(WorkflowServiceImplementation.class);


	@Override
	public void getWorkflow(WorkflowDefinitionRequest request, StreamObserver<WorkflowDefinition> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Menu Requested = " + request.getUuid());
			ClientRequest applicationInfo = request.getClientRequest();
			if(applicationInfo == null || Util.isEmpty(applicationInfo.getSessionUuid())) {
				throw new AdempiereException("Object Request Null");
			}

			Properties context = ContextManager.getContext(applicationInfo);
			WorkflowDefinition.Builder workflowBuilder = convertWorkflow(context, request.getUuid(), request.getId());
			responseObserver.onNext(workflowBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.augmentDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}
	
	/**
	 * Request Workflow from uuid or id
	 * @param context
	 * @param uuid
	 * @param id
	 */
	private WorkflowDefinition.Builder convertWorkflow(Properties context, String uuid, int id) {
		String whereClause = null;
		Object parameter = null;
		if(id > 0) {
			whereClause = MWorkflow.COLUMNNAME_AD_Workflow_ID + " = ?";
			parameter = id;
		} else if(!Util.isEmpty(uuid)) {
			whereClause = MWorkflow.COLUMNNAME_UUID + " = ?";
			parameter = uuid;
		}
		if(parameter == null) {
			return WorkflowDefinition.newBuilder();
		}

		MWorkflow workflow = new Query(
				context,
				MWorkflow.Table_Name,
				whereClause,
				null
			)
			.setParameters(parameter)
			.setOnlyActiveRecords(true)
			.first();
		return convertWorkflow(context, workflow);
	}

	/**
	 * Convert Workflow from Workflow Model
	 * @param form
	 * @return
	 */
	private WorkflowDefinition.Builder convertWorkflow(Properties context, MWorkflow workflowDefinition) {
		WorkflowDefinition.Builder builder = WorkflowUtil.convertWorkflowDefinition(workflowDefinition);

		//	Add to recent Item
		// addToRecentItem(MMenu.ACTION_WorkFlow, workflowDefinition.getAD_Workflow_ID());

		return builder;
	}

	@Override
	public void listWorkflows(ListWorkflowsRequest request, StreamObserver<ListWorkflowsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Workflow Logs Requested is Null");
			}
			log.fine("Object List Requested = " + request);
			Properties context = ContextManager.getContext(request.getClientRequest().getSessionUuid(), request.getClientRequest().getLanguage(), request.getClientRequest().getOrganizationUuid(), request.getClientRequest().getWarehouseUuid());
			ListWorkflowsResponse.Builder entityValueList = convertWorkflows(context, request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.augmentDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listDocumentActions(ListDocumentActionsRequest request,
			StreamObserver<ListDocumentActionsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Document Actions is Null");
			}
			log.fine("Object List Requested = " + request);
			Properties context = ContextManager.getContext(request.getClientRequest().getSessionUuid(), request.getClientRequest().getLanguage(), request.getClientRequest().getOrganizationUuid(), request.getClientRequest().getWarehouseUuid());
			ListDocumentActionsResponse.Builder entityValueList = convertDocumentActions(context, request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.augmentDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listDocumentStatuses(ListDocumentStatusesRequest request,
			StreamObserver<ListDocumentStatusesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Document Statuses is Null");
			}
			log.fine("Object List Requested = " + request);
			Properties context = ContextManager.getContext(request.getClientRequest().getSessionUuid(), request.getClientRequest().getLanguage(), request.getClientRequest().getOrganizationUuid(), request.getClientRequest().getWarehouseUuid());
			ListDocumentStatusesResponse.Builder entityValueList = convertDocumentStatuses(context, request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.augmentDescription(e.getLocalizedMessage())
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
			Properties context = ContextManager.getContext(request.getClientRequest().getSessionUuid(), request.getClientRequest().getLanguage(), request.getClientRequest().getOrganizationUuid(), request.getClientRequest().getWarehouseUuid());
			ListWorkflowActivitiesResponse.Builder activitiesList = convertWorkflowActivities(context, request);
			responseObserver.onNext(activitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.augmentDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	/**
	 * Convert request for workflow to builder
	 * @param context
	 * @param request
	 * @return
	 */
	private ListWorkflowActivitiesResponse.Builder convertWorkflowActivities(Properties context, ListWorkflowActivitiesRequest request) {
		if(Util.isEmpty(request.getUserUuid())) {
			throw new AdempiereException("@AD_User_ID@ @NotFound@");
		}
		//	
		int userId = RecordUtil.getIdFromUuid(I_AD_User.Table_Name, request.getUserUuid(), null);
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
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());
		Query query = new Query(context, I_AD_WF_Activity.Table_Name, whereClause, null)
				.setParameters(userId, userId, userId, userId);
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
		if(RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		//	Return
		return builder;
	}
	
	/**
	 * Convert document statuses
	 * @param context
	 * @param request
	 * @return
	 */
	private ListDocumentStatusesResponse.Builder convertDocumentStatuses(Properties context, ListDocumentStatusesRequest request) {
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
		PO entity = RecordUtil.getEntity(context, request.getTableName(), request.getUuid(), request.getId(), null);
		if(entity != null) {
			//	
			documentStatus = entity.get_ValueAsString(I_C_Order.COLUMNNAME_DocStatus);
			documentAction = entity.get_ValueAsString(I_C_Order.COLUMNNAME_DocAction);
			//
			isProcessing = ValueUtil.booleanToString(
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
			isSOTrx = ValueUtil.booleanToString(
				entity.get_ValueAsBoolean(I_C_Order.COLUMNNAME_IsSOTrx)
			);
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
					DocumentStatus.Builder documentStatusBuilder = DocumentStatus.newBuilder();
					documentStatusBuilder.setValue(ValueUtil.validateNull(valueList.get(i)));
					documentStatusBuilder.setName(ValueUtil.validateNull(nameList.get(i)));
					documentStatusBuilder.setDescription(ValueUtil.validateNull(descriptionList.get(i)));
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
					DocumentStatus.Builder documentActionBuilder = DocumentStatus.newBuilder();
					documentActionBuilder.setValue(ValueUtil.validateNull(valueList.get(i)));
					documentActionBuilder.setName(ValueUtil.validateNull(nameList.get(i)));
					documentActionBuilder.setDescription(ValueUtil.validateNull(descriptionList.get(i)));
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
		StringBuffer whereClause = new StringBuffer();
		List<Object> parameters = new ArrayList<>();
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		//	
		MTable table = MTable.get(context, request.getTableName());
		if(table == null
				|| table.getAD_Table_ID() == 0) {
			throw new AdempiereException("@AD_Table_ID@ @Invalid@");
		}
		whereClause
			.append(I_AD_Workflow.COLUMNNAME_AD_Table_ID).append(" = ?");
		//	Set parameters
		parameters.add(table.getAD_Table_ID());
		//	Get page and count
		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());
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
		if(RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		//	Set next page
		builder.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		//	Return
		return builder;
	}
	
	/**
	 * Convert document actions
	 * @param context
	 * @param request
	 * @return
	 */
	private ListDocumentActionsResponse.Builder convertDocumentActions(Properties context, ListDocumentActionsRequest request) {
		if(request.getId() <= 0 && Util.isEmpty(request.getUuid())) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		//	
		MTable table = MTable.get(context, request.getTableName());
		if(table == null || table.getAD_Table_ID() == 0) {
			throw new AdempiereException("@AD_Table_ID@ @Invalid@");
		}
		PO entity = RecordUtil.getEntity(context, request.getTableName(), request.getUuid(), request.getId(), null);
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
		String isSOTrx = entity.get_ValueAsBoolean(I_C_Order.COLUMNNAME_IsSOTrx)? "Y": "N";
		//	
		if (documentStatus == null) {
			throw new AdempiereException("@DocStatus@ @NotFound@");
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

		/**
		 * 	Check Existence of Workflow Activities
		 */
		String workflowStatus = MWFActivity.getActiveInfo(Env.getCtx(), entity.get_Table_ID(), entity.get_ID()); 
		if (workflowStatus != null) {
			throw new AdempiereException("@WFActiveForRecord@");
		}
		
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
		//	
		ListDocumentActionsResponse.Builder builder = ListDocumentActionsResponse.newBuilder();
		//	Get
		Arrays.asList(options).stream().filter(option -> option != null).forEach(option -> {
			for (int i = 0; i < valueList.size(); i++) {
				if (option.equals(valueList.get(i))) {
					builder.addDocumentActions(ConvertUtil.convertDocumentAction(
							valueList.get(i), 
							nameList.get(i), 
							descriptionList.get(i)));
				}
			}
		});
		//	setDefault
		if (documentAction.equals("--"))		//	If None, suggest closing
			documentAction = DocumentEngine.ACTION_Close;
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
		if (!defaultName.equals("")) {
			builder.setDefaultDocumentAction(ConvertUtil.convertDocumentAction(
					defaultValue, 
					defaultName, 
					defaultDescription));
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

			Properties context = ContextManager.getContext(request.getClientRequest());
			ProcessLog.Builder processReponse = runDocumentAction(context, request);
			responseObserver.onNext(processReponse.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.augmentDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	
	/**
	 * Run a process from request
	 * @param context
	 * @param request
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private ProcessLog.Builder runDocumentAction(Properties context, RunDocumentActionRequest request) throws FileNotFoundException, IOException {
		if(Util.isEmpty(request.getTableName())) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		//	get document action
		if(Util.isEmpty(request.getDocumentAction())) {
			throw new AdempiereException("@DocAction@ @NotFound@");
		}
		String documentAction = request.getDocumentAction();

		int tableId = 0;
		MTable table = MTable.get(context, request.getTableName());
		if(table != null && table.getAD_Table_ID() > 0) {
			tableId = table.getAD_Table_ID();
		}

		int recordId = request.getId();
		if (recordId <= 0 && Util.isEmpty(request.getUuid())) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}
		PO entity = RecordUtil.getEntity(context, request.getTableName(), request.getUuid(), recordId, null);
		if (entity != null) {
			recordId = entity.get_ID();
		}

		MColumn docActionColumn = new Query(
				context,
				MColumn.Table_Name,
				MColumn.COLUMNNAME_AD_Table_ID + " = ? AND " + MColumn.COLUMNNAME_ColumnName + " = ? ",
				null
			)
			.setParameters(table.getAD_Table_ID(), I_AD_WF_Node.COLUMNNAME_DocAction)
			.first();

		MProcess process = MProcess.get(context, docActionColumn.getAD_Process_ID());
		if (process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}

		//	Call process builder
		ProcessBuilder builder = ProcessBuilder.create(Env.getCtx())
			.process(process.getAD_Process_ID())
			.withRecordId(tableId, recordId)
			.withoutPrintPreview()
			.withoutBatchMode()
			.withWindowNo(0)
			.withTitle(process.getName())
			.withParameter(table.getTableName() + DictionaryUtil.ID_PREFIX, recordId)
			.withParameter(I_AD_WF_Node.COLUMNNAME_DocAction, documentAction);
	
		//	For Document
		if(!Util.isEmpty(documentAction) && process.getAD_Workflow_ID() != 0 && entity != null && DocAction.class.isAssignableFrom(entity.getClass())) {
			entity.set_ValueOfColumn(I_AD_WF_Node.COLUMNNAME_DocAction, documentAction);
			entity.saveEx();
			builder.withoutTransactionClose();
		}

		//	Execute Process
		ProcessInfo result = null;
		try {
			result = builder.execute();
		} catch (Exception e) {
			result = builder.getProcessInfo();
			//	Set error message
			if(Util.isEmpty(result.getSummary())) {
				result.setSummary(e.getLocalizedMessage());
			}
		}

		ProcessLog.Builder response = ProcessLog.newBuilder();
		//	Get process instance from identifier
		if(result.getAD_PInstance_ID() != 0) {
			MPInstance instance = new Query(
					context,
					I_AD_PInstance.Table_Name,
					I_AD_PInstance.COLUMNNAME_AD_PInstance_ID + " = ?",
					null
				)
				.setParameters(result.getAD_PInstance_ID())
				.first();
			response.setInstanceUuid(ValueUtil.validateNull(instance.getUUID()));
			response.setLastRun(instance.getUpdated().getTime());
		}
		//
		response.setIsError(result.isError());
		if(!Util.isEmpty(result.getSummary())) {
			response.setSummary(Msg.parseTranslation(context, result.getSummary()));
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

		return response;
	}

}
