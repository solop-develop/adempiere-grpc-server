/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.core.domains.models.I_R_RequestAction;
import org.adempiere.core.domains.models.I_R_RequestType;
import org.adempiere.core.domains.models.I_R_RequestUpdate;
import org.adempiere.core.domains.models.I_R_Status;
import org.adempiere.core.domains.models.X_R_RequestUpdate;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MRefList;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestAction;
import org.compiere.model.MRequestType;
import org.compiere.model.MRequestUpdate;
import org.compiere.model.MStatus;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.issue_management.CreateIssueCommentRequest;
import org.spin.backend.grpc.issue_management.CreateIssueRequest;
import org.spin.backend.grpc.issue_management.DeleteIssueCommentRequest;
import org.spin.backend.grpc.issue_management.DeleteIssueRequest;
import org.spin.backend.grpc.issue_management.ExistsIssuesRequest;
import org.spin.backend.grpc.issue_management.ExistsIssuesResponse;
import org.spin.backend.grpc.issue_management.Issue;
import org.spin.backend.grpc.issue_management.IssueComment;
import org.spin.backend.grpc.issue_management.IssueCommentType;
import org.spin.backend.grpc.issue_management.IssueManagementGrpc.IssueManagementImplBase;
import org.spin.backend.grpc.issue_management.Priority;
import org.spin.backend.grpc.issue_management.ListIssueCommentsReponse;
import org.spin.backend.grpc.issue_management.ListIssueCommentsRequest;
import org.spin.backend.grpc.issue_management.ListIssuesReponse;
import org.spin.backend.grpc.issue_management.ListIssuesRequest;
import org.spin.backend.grpc.issue_management.ListPrioritiesResponse;
import org.spin.backend.grpc.issue_management.ListPrioritiesRequest;
import org.spin.backend.grpc.issue_management.ListRequestTypesRequest;
import org.spin.backend.grpc.issue_management.ListRequestTypesResponse;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesRequest;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesResponse;
import org.spin.backend.grpc.issue_management.ListStatusesRequest;
import org.spin.backend.grpc.issue_management.ListStatusesResponse;
import org.spin.backend.grpc.issue_management.RequestType;
import org.spin.backend.grpc.issue_management.SalesRepresentative;
import org.spin.backend.grpc.issue_management.UpdateIssueCommentRequest;
import org.spin.backend.grpc.issue_management.UpdateIssueRequest;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class IssueManagementServiceImplementation extends IssueManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(IssueManagementServiceImplementation.class);


	@Override
	public void listRequestTypes(ListRequestTypesRequest request, StreamObserver<ListRequestTypesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListRequestTypesResponse.Builder entityValueList = listRequestTypes(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListRequestTypesResponse.Builder listRequestTypes(ListRequestTypesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		Query queryRequestTypes = new Query(
			context,
			I_R_RequestType.Table_Name,
			null,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
		;
		int recordCount = queryRequestTypes.count();

		ListRequestTypesResponse.Builder builderList = ListRequestTypesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		queryRequestTypes
			.setLimit(limit, offset)
			.list(MRequestType.class)
			.forEach(requestType -> {
				RequestType.Builder builder = convertRequestType(requestType);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private RequestType.Builder convertRequestType(int requestTypeId) {
		RequestType.Builder builder = RequestType.newBuilder();
		if (requestTypeId <= 0) {
			return builder;
		}

		MRequestType requestType = MRequestType.get(Env.getCtx(), requestTypeId);
		return convertRequestType(requestType);
	}
	private RequestType.Builder convertRequestType(MRequestType requestType) {
		RequestType.Builder builder = RequestType.newBuilder();
		if (requestType == null) {
			return builder;
		}

		builder.setId(requestType.getR_RequestType_ID())
			.setUuid(ValueUtil.validateNull(requestType.getUUID()))
			.setName(ValueUtil.validateNull(requestType.getName()))
			.setDescription(ValueUtil.validateNull(requestType.getDescription()))
		;

		return builder;
	}
	

	@Override
	public void listSalesRepresentatives(ListSalesRepresentativesRequest request, StreamObserver<ListSalesRepresentativesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListSalesRepresentativesResponse.Builder entityValueList = listSalesRepresentatives(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	private ListSalesRepresentativesResponse.Builder listSalesRepresentatives(ListSalesRepresentativesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		final String whereClause = "EXISTS("
			+ "SELECT * FROM C_BPartner bp WHERE "
			+ "AD_User.C_BPartner_ID=bp.C_BPartner_ID "
			+ "AND (bp.IsEmployee='Y' OR bp.IsSalesRep='Y'))"
		;
		Query querySaleRepresentatives = new Query(
			context,
			I_AD_User.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
		;
		int recordCount = querySaleRepresentatives.count();

		ListSalesRepresentativesResponse.Builder builderList = ListSalesRepresentativesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		querySaleRepresentatives
			.setLimit(limit, offset)
			.list(MUser.class)
			.forEach(requestType -> {
				SalesRepresentative.Builder builder = convertSalesRepresentative(requestType);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private SalesRepresentative.Builder convertSalesRepresentative(MUser salesRepresentative) {
		SalesRepresentative.Builder builder = SalesRepresentative.newBuilder();
		if (salesRepresentative == null || salesRepresentative.getAD_User_ID() <= 0) {
			return builder;
		}
		builder
			.setUuid(ValueUtil.validateNull(salesRepresentative.getUUID()))
			.setId(salesRepresentative.getAD_User_ID())
			.setName(ValueUtil.validateNull(salesRepresentative.getName()))
			.setDescription(ValueUtil.validateNull(salesRepresentative.getDescription()))
		;
		return builder;
	}

	private SalesRepresentative.Builder convertSalesRepresentative(int salesRepresentativeId) {
		if (salesRepresentativeId <= 0) {
			return SalesRepresentative.newBuilder();
		}
		MUser salesRepresentative = MUser.get(Env.getCtx(), salesRepresentativeId);
		return convertSalesRepresentative(salesRepresentative);
	}


	@Override
	public void listPriorities(ListPrioritiesRequest request, StreamObserver<ListPrioritiesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListPrioritiesResponse.Builder entityValueList = listPriorities(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListPrioritiesResponse.Builder listPriorities(ListPrioritiesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		final String whereClause = "AD_Reference_ID = ?";
		Query queryRequests = new Query(
			context,
			MRefList.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(MRequest.PRIORITY_AD_Reference_ID)
		;

		int recordCount = queryRequests.count();

		ListPrioritiesResponse.Builder builderList = ListPrioritiesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		queryRequests
			.setLimit(limit, offset)
			.list(MRefList.class)
			.forEach(priority -> {
				Priority.Builder builder = convertPriority(priority);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private Priority.Builder convertPriority(String value) {
		Priority.Builder builder = Priority.newBuilder();
		if (Util.isEmpty(value, true)) {
			return builder;
		}
		MRefList priority = MRefList.get(Env.getCtx(), MRequest.PRIORITY_AD_Reference_ID, value, null);
		return convertPriority(priority);
	}

	private Priority.Builder convertPriority(MRefList priority) {
		Priority.Builder builder = Priority.newBuilder();
		if (priority == null || priority.getAD_Ref_List_ID() <= 0) {
			return builder;
		}

		builder.setId(priority.getAD_Ref_List_ID())
			.setUuid(ValueUtil.validateNull(priority.getUUID()))
			.setValue(ValueUtil.validateNull(priority.getValue()))
			.setName(ValueUtil.validateNull(priority.getName()))
			.setDescription(ValueUtil.validateNull(priority.getDescription()))
		;

		return builder;
	}


	@Override
	public void listStatuses(ListStatusesRequest request, StreamObserver<ListStatusesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListStatusesResponse.Builder entityValueList = listStatuses(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListStatusesResponse.Builder listStatuses(ListStatusesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		int requestTypeId = request.getRequestTypeId();
		if (requestTypeId <= 0 && !Util.isEmpty(request.getRequestTypeUuid(), true)) {
			requestTypeId = RecordUtil.getIdFromUuid(MRequestType.Table_Name, request.getRequestTypeUuid(), null);
		}
		if (requestTypeId <= 0) {
			throw new AdempiereException("@R_RequestType_ID@ @NotFound@");
		}

		final String whereClause = "EXISTS (SELECT * FROM R_RequestType rt "
			+ "INNER JOIN R_StatusCategory sc ON (rt.R_StatusCategory_ID=sc.R_StatusCategory_ID) "
			+ "WHERE R_Status.R_StatusCategory_ID = sc.R_StatusCategory_ID "
			+ "AND rt.R_RequestType_ID = ?)"
		;
		Query queryRequests = new Query(
			context,
			I_R_Status.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(requestTypeId)
		;

		int recordCount = queryRequests.count();

		ListStatusesResponse.Builder builderList = ListStatusesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		queryRequests
			.setLimit(limit, offset)
			.list(MStatus.class)
			.forEach(requestRecord -> {
				org.spin.backend.grpc.issue_management.Status.Builder builder = convertStatus(requestRecord);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private org.spin.backend.grpc.issue_management.Status.Builder convertStatus(int statusId) {
		org.spin.backend.grpc.issue_management.Status.Builder builder = org.spin.backend.grpc.issue_management.Status.newBuilder();
		if (statusId <= 0) {
			return builder;
		}

		MStatus requestType = MStatus.get(Env.getCtx(), statusId);
		return convertStatus(requestType);
	}
	private org.spin.backend.grpc.issue_management.Status.Builder convertStatus(MStatus status) {
		org.spin.backend.grpc.issue_management.Status.Builder builder = org.spin.backend.grpc.issue_management.Status.newBuilder();
		if (status == null || status.getR_Status_ID() <= 0) {
			return builder;
		}

		builder.setId(status.getR_Status_ID())
			.setUuid(ValueUtil.validateNull(status.getUUID()))
			.setName(ValueUtil.validateNull(status.getName()))
			.setDescription(ValueUtil.validateNull(status.getDescription()))
		;

		return builder;
	}


	@Override
	public void existsIssues(ExistsIssuesRequest request, StreamObserver<ExistsIssuesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ExistsIssuesResponse.Builder entityValueList = existsIssues(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	private ExistsIssuesResponse.Builder existsIssues(ExistsIssuesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		if (Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}

		MTable table = MTable.get(context, request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		// validate record
		int recordId = request.getRecordId();
		if (recordId <= 0 && !Util.isEmpty(request.getRecordUuid(), true)) {
			recordId = RecordUtil.getIdFromUuid(table.getTableName(), request.getRecordUuid(), null);
			if (recordId < 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}

		final String whereClause = "Record_ID = ? "
			+ "AND AD_Table_ID = ? "
		;
		int recordCount = new Query(
			context,
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			.setParameters(recordId, table.getAD_Table_ID())
			.count()
		;

		ExistsIssuesResponse.Builder builder = ExistsIssuesResponse.newBuilder()
			.setRecordCount(recordCount);

		return builder;
	}


	@Override
	public void listIssues(ListIssuesRequest request, StreamObserver<ListIssuesReponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Process Activity Requested is Null");
			}
			ListIssuesReponse.Builder entityValueList = listIssues(request);
			responseObserver.onNext(entityValueList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListIssuesReponse.Builder listIssues(ListIssuesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		List<Object> parametersList = new ArrayList<>();
		String whereClause = "";

		if (!Util.isEmpty(request.getTableName(), true) || !Util.isEmpty(request.getRecordUuid(), true)) {
			// validate table
			if (Util.isEmpty(request.getTableName(), true)) {
				throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
			}
			MTable table = MTable.get(context, request.getTableName());
			if (table == null || table.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@AD_Table_ID@ @NotFound@");
			}

			// validate record
			int recordId = request.getRecordId();
			if (recordId <= 0 && !Util.isEmpty(request.getRecordUuid(), true)) {
				recordId = RecordUtil.getIdFromUuid(table.getTableName(), request.getRecordUuid(), null);
				if (recordId < 0) {
					throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
				}
			}

			parametersList.add(table.getAD_Table_ID(), recordId);
			whereClause = "AD_Table_ID = ? AND Record_ID = ? ";
		} else {
			int userId = Env.getAD_Client_ID(context);
			parametersList.add(userId, userId);
			whereClause = "AD_User_ID = ? OR AD_User_ID = ?";
		}

		Query queryRequests = new Query(
			context,
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(parametersList)
		;

		int recordCount = queryRequests.count();

		ListIssuesReponse.Builder builderList = ListIssuesReponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		queryRequests
			.setLimit(limit, offset)
			.list(MRequest.class)
			.forEach(requestRecord -> {
				Issue.Builder builder = convertRequest(requestRecord);
				builderList.addRecords(builder);
			});

		return builderList;
	}
	
	private Issue.Builder convertRequest(MRequest request) {
		Issue.Builder builder = Issue.newBuilder();
		if (request == null) {
			return builder;
		}

		builder.setId(request.getR_Request_ID())
			.setUuid(ValueUtil.validateNull(request.getUUID()))
			.setDocumentNo(ValueUtil.validateNull(request.getDocumentNo()))
			.setSubject(ValueUtil.validateNull(request.getSubject()))
			.setSummary(ValueUtil.validateNull(request.getSummary()))
			.setCreated(
				ValueUtil.getLongFromTimestamp(request.getUpdated())
			)
			.setLastUpdated(
				ValueUtil.getLongFromTimestamp(request.getUpdated())
			)
		;
		builder.setRequestType(
			convertRequestType(request.getR_RequestType_ID())
		);
		builder.setSalesRepresentative(
			convertSalesRepresentative(request.getSalesRep_ID())
		);
		builder.setStatus(
			convertStatus(request.getR_Status_ID())
		);
		if (request.getCreatedBy() > 0) {
			MUser user = MUser.get(Env.getCtx(), request.getCreatedBy());
			if (user != null) {
				builder.setUserId(user.getAD_User_ID())
					.setUserUuid(ValueUtil.validateNull(user.getUUID()))
					.setUserName(ValueUtil.validateNull(user.getName()))
				;
			}
		}
		if (!Util.isEmpty(request.getPriority(), true)) {
			builder.setPriority(
				convertPriority(request.getPriority())
			);
		}

		return builder;
	}


	@Override
	public void createIssue(CreateIssueRequest request, StreamObserver<Issue> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Issue.Builder builder = createIssue(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Issue.Builder createIssue(CreateIssueRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		if (Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}

		MTable table = MTable.get(context, request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		// validate record
		if (request.getRecordId() < 0 && Util.isEmpty(request.getRecordUuid(), true)) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}
		PO entity = RecordUtil.getEntity(context, table.getTableName(), request.getRecordUuid(), request.getRecordId(), null);
		if (entity == null) {
			throw new AdempiereException("@PO@ @NotFound@");
		}

		if (Util.isEmpty(request.getSubject(), true)) {
			throw new AdempiereException("@FillMandatory@ @Subject@");
		}

		if (Util.isEmpty(request.getSummary(), true)) {
			throw new AdempiereException("@FillMandatory@ @Summary@");
		}

		int requestTypeId = request.getRequestTypeId();
		if (requestTypeId <= 0 && !Util.isEmpty(request.getRequestTypeUuid(), true)) {
			requestTypeId = RecordUtil.getIdFromUuid(MRequestType.Table_Name, request.getRequestTypeUuid(), null);
		}
		if (requestTypeId <= 0) {
			throw new AdempiereException("@R_RequestType_ID@ @NotFound@");
		}

		int salesRepresentativeId = request.getSalesRepresentativeId();
		if (salesRepresentativeId <= 0 && !Util.isEmpty(request.getSalesRepresentativeUuid(), true)) {
			salesRepresentativeId = RecordUtil.getIdFromUuid(MUser.Table_Name, request.getSalesRepresentativeUuid(), null);
		}
		if (salesRepresentativeId <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @NotFound@");
		}

		MRequest requestRecord = new MRequest(context, 0, null);
		PO.copyValues(entity, requestRecord, true);

		// validate if entity key column exists on request to set
		String keyColumn = entity.get_TableName() + "_ID";
		if (requestRecord.get_ColumnIndex(keyColumn) >= 0) {
			requestRecord.set_ValueOfColumn(keyColumn, entity.get_ID());
		}

		requestRecord.setRecord_ID(entity.get_ID());
		requestRecord.setAD_Table_ID(table.getAD_Table_ID());
		requestRecord.setR_RequestType_ID(requestTypeId);
		requestRecord.setSubject(request.getSubject());
		requestRecord.setSummary(request.getSummary());
		requestRecord.setSalesRep_ID(salesRepresentativeId);
		requestRecord.setPriority(
			ValueUtil.validateNull(request.getPriorityValue())
		);
		requestRecord.saveEx();

		Issue.Builder builder = convertRequest(requestRecord);

		return builder;
	}


	@Override
	public void updateIssue(UpdateIssueRequest request, StreamObserver<Issue> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Issue.Builder builder = updateIssue(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Issue.Builder updateIssue(UpdateIssueRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		// validate record
		int recordId = request.getId();
		if (recordId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
			recordId = RecordUtil.getIdFromUuid(MRequest.Table_Name, request.getUuid(), null);
		}
		if (recordId <= 0) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}
		if (Util.isEmpty(request.getSubject(), true)) {
			throw new AdempiereException("@FillMandatory@ @Subject@");
		}

		if (Util.isEmpty(request.getSummary(), true)) {
			throw new AdempiereException("@FillMandatory@ @Summary@");
		}

		int requestTypeId = request.getRequestTypeId();
		if (requestTypeId <= 0 && !Util.isEmpty(request.getRequestTypeUuid(), true)) {
			requestTypeId = RecordUtil.getIdFromUuid(MRequestType.Table_Name, request.getRequestTypeUuid(), null);
		}
		if (requestTypeId <= 0) {
			throw new AdempiereException("@R_RequestType_ID@ @NotFound@");
		}

		int salesRepresentativeId = request.getSalesRepresentativeId();
		if (salesRepresentativeId <= 0 && !Util.isEmpty(request.getSalesRepresentativeUuid(), true)) {
			salesRepresentativeId = RecordUtil.getIdFromUuid(MUser.Table_Name, request.getSalesRepresentativeUuid(), null);
		}
		if (salesRepresentativeId <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @NotFound@");
		}

		MRequest requestRecord = new MRequest(context, recordId, null);
		if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_Request_ID@ @NotFound@");
		}
		requestRecord.setR_RequestType_ID(requestTypeId);
		requestRecord.setSubject(request.getSubject());
		requestRecord.setSummary(request.getSummary());
		requestRecord.setSalesRep_ID(salesRepresentativeId);
		requestRecord.setPriority(
			ValueUtil.validateNull(request.getPriorityValue())
		);
		requestRecord.saveEx();

		Issue.Builder builder = convertRequest(requestRecord);
		return builder;
	}


	@Override
	public void deleteIssue(DeleteIssueRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Empty.Builder builder = deleteIssue(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Empty.Builder deleteIssue(DeleteIssueRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		Trx.run(transactionName -> {
			// validate record
			int recordId = request.getId();
			if (recordId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
				recordId = RecordUtil.getIdFromUuid(I_R_Request.Table_Name, request.getUuid(), transactionName);
				if (recordId < 0) {
					throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
				}
			}

			MRequest requestRecord = new MRequest(context, recordId, transactionName);
			if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
				throw new AdempiereException("@R_Request_ID@ @NotFound@");
			}

			final String whereClause = "R_Request_ID = ?";

			// delete actions
			List<MRequestAction> requestActionsList = new Query(
				context,
				I_R_RequestAction.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(requestRecord.getR_Request_ID())
				.list(MRequestAction.class);
			requestActionsList.forEach(requestAction -> {
				requestAction.deleteEx(true);
			});

			// delete updates
			List<MRequestUpdate> requestUpdatesList = new Query(
				context,
				I_R_RequestUpdate.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(requestRecord.getR_Request_ID())
				.list(MRequestUpdate.class);
			requestUpdatesList.forEach(requestUpdate -> {
				requestUpdate.deleteEx(true);
			});

			// delete header
			requestRecord.deleteEx(true);
		});

		return Empty.newBuilder();
	}


	@Override
	public void listIssueComments(ListIssueCommentsRequest request, StreamObserver<ListIssueCommentsReponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListIssueCommentsReponse.Builder builder = listIssueComments(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListIssueCommentsReponse.Builder listIssueComments(ListIssueCommentsRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		// validate parent record
		int recordId = request.getIssueId();
		if (recordId <= 0 && !Util.isEmpty(request.getIssueUuid(), true)) {
			recordId = RecordUtil.getIdFromUuid(I_R_Request.COLUMNNAME_R_Request_ID, request.getIssueUuid(), null);
			if (recordId < 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}
		
		MRequest requestRecord = new MRequest(context, recordId, null);
		if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}

		final String whereClause = "R_Request_ID = ? ";
		Query queryRequestsUpdate = new Query(
			context,
			I_R_RequestUpdate.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(recordId)
		;

		Query queryRequestsLog = new Query(
			context,
			I_R_RequestAction.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(recordId)
		;

		int recordCount = queryRequestsUpdate.count() + queryRequestsLog.count();

		ListIssueCommentsReponse.Builder builderList = ListIssueCommentsReponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		List<IssueComment.Builder> issueCommentsList = new ArrayList<>();
		queryRequestsUpdate
			.setLimit(limit, offset)
			.list(X_R_RequestUpdate.class)
			.forEach(requestUpdate -> {
				IssueComment.Builder builder = convertRequestUpdate(requestUpdate);
				issueCommentsList.add(builder);
				// builderList.addRecords(builder);
			});

		queryRequestsLog
			.setLimit(limit, offset)
			.list(MRequestAction.class)
			.forEach(requestAction -> {
				IssueComment.Builder builder = convertRequestUpdate(requestAction);
				issueCommentsList.add(builder);
				// builderList.addRecords(builder);
			});

		issueCommentsList.stream()
			.sorted(Comparator.comparing(IssueComment.Builder::getCreated))
			.forEach(issueComment -> {
				builderList.addRecords(issueComment);
			});

		return builderList;
	}

	private IssueComment.Builder convertRequestUpdate(X_R_RequestUpdate requestUpdate) {
		IssueComment.Builder builder = IssueComment.newBuilder();
		if (requestUpdate == null || requestUpdate.getR_RequestUpdate_ID() <= 0) {
			return builder;
		}
		builder.setId(requestUpdate.getR_RequestUpdate_ID())
			.setUuid(ValueUtil.validateNull(requestUpdate.getUUID()))
			.setCreated(
				ValueUtil.getLongFromTimestamp(requestUpdate.getCreated())
			)
			.setResult(
				ValueUtil.validateNull(requestUpdate.getResult())
			)
			.setIssueCommentType(IssueCommentType.COMMENT)
		;

		MUser user = MUser.get(Env.getCtx(), requestUpdate.getCreatedBy());
		if (user != null && user.getAD_User_ID() > 0) {
			builder.setUserId(user.getAD_User_ID())
				.setUserUuid(
					ValueUtil.validateNull(user.getUUID())
				)
				.setUserName(
					ValueUtil.validateNull(user.getName())
				)
			;
		}

		return builder;
	}

	private IssueComment.Builder convertRequestUpdate(MRequestAction requestAction) {
		IssueComment.Builder builder = IssueComment.newBuilder();
		if (requestAction == null || requestAction.getR_RequestAction_ID() <= 0) {
			return builder;
		}
		builder.setId(requestAction.getR_RequestAction_ID())
			.setUuid(ValueUtil.validateNull(requestAction.getUUID()))
			.setCreated(
				ValueUtil.getLongFromTimestamp(requestAction.getCreated())
			)
			.setIssueCommentType(IssueCommentType.LOG)
		;

		MUser user = MUser.get(Env.getCtx(), requestAction.getCreatedBy());
		if (user != null && user.getAD_User_ID() > 0) {
			builder.setUserId(user.getAD_User_ID())
				.setUserUuid(
					ValueUtil.validateNull(user.getUUID())
				)
				.setUserName(
					ValueUtil.validateNull(user.getName())
				)
			;
		}

		String columnModified = null;
		if (!Util.isEmpty(requestAction.getNullColumns(), true)) {
			columnModified = requestAction.getNullColumns();
		} else {
			if (requestAction.getR_RequestType_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_RequestType_ID;
			} else if (requestAction.getR_Group_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Group_ID;
			} else if (requestAction.getR_Category_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Category_ID;
			} else if (requestAction.getR_Status_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Status_ID;
			} else if (requestAction.getR_Resolution_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_R_Resolution_ID;
			} else if (!Util.isEmpty(requestAction.getPriority(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_Priority;
			} else if (!Util.isEmpty(requestAction.getPriorityUser(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_PriorityUser;
			} else if (!Util.isEmpty(requestAction.getSummary(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_Summary;
			} else if (!Util.isEmpty(requestAction.getConfidentialType(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_Summary;
			} else if (!Util.isEmpty(requestAction.getIsInvoiced(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_IsInvoiced;
			} else if (!Util.isEmpty(requestAction.getIsEscalated(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_IsEscalated;
			} else if (!Util.isEmpty(requestAction.getIsSelfService(), true)) {
				columnModified = I_R_RequestAction.COLUMNNAME_IsSelfService;
			} else if (requestAction.getSalesRep_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_SalesRep_ID;
			} else if (requestAction.getAD_Role_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_AD_Role_ID;
			} else if (requestAction.getDateNextAction() != null) {
				columnModified = I_R_RequestAction.COLUMNNAME_DateNextAction;
			} else if (requestAction.getC_Activity_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Activity_ID;
			} else if (requestAction.getC_BPartner_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_BPartner_ID;
			} else if (requestAction.getAD_User_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_AD_User_ID;
			} else if (requestAction.getC_Project_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Project_ID;
			} else if (requestAction.getA_Asset_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_A_Asset_ID;
			} else if (requestAction.getC_Order_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Order_ID;
			} else if (requestAction.getC_Invoice_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Invoice_ID;
			} else if (requestAction.getM_Product_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_M_Product_ID;
			} else if (requestAction.getC_Payment_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_C_Payment_ID;
			} else if (requestAction.getM_InOut_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_M_InOut_ID;
			} else if (requestAction.getM_RMA_ID() > 0) {
				columnModified = I_R_RequestAction.COLUMNNAME_M_RMA_ID;
			}
		}

		if (!Util.isEmpty(columnModified, true)) {
			MColumn column = new Query(
				Env.getCtx(),
				I_AD_Column.Table_Name,
				"AD_Table_ID = ? AND ColumnName = ?",
				null
			)
				.setParameters(I_R_RequestAction.Table_ID, columnModified)
				.first();

			if (column != null) {
				builder.setLabel(column.getName());
				Object value = requestAction.get_Value(columnModified);
				builder.setNewValue(
					ValueUtil.getValueFromReference(value, column.getAD_Reference_ID())
				);
				String displayedValue = ValueUtil.getDisplayedValueFromReference(value, columnModified, column.getAD_Reference_ID(), column.getAD_Reference_Value_ID());
				builder.setDisplayedValue(
					ValueUtil.validateNull(displayedValue)
				);
			}
		}

		return builder;
	}


	@Override
	public void createIssueComment(CreateIssueCommentRequest request, StreamObserver<IssueComment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			IssueComment.Builder builder = createIssueComment(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private IssueComment.Builder createIssueComment(CreateIssueCommentRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		// validate parent record
		int recordId = request.getIssueId();
		if (recordId <= 0 && !Util.isEmpty(request.getIssueUuid(), true)) {
			recordId = RecordUtil.getIdFromUuid(I_R_Request.COLUMNNAME_R_Request_ID, request.getIssueUuid(), null);
			if (recordId < 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}
		MRequest requestRecord = new MRequest(context, recordId, null);
		requestRecord.setResult(
			ValueUtil.validateNull(request.getResult())
		);
		requestRecord.saveEx();

		return IssueComment.newBuilder();
	}


	@Override
	public void updateIssueComment(UpdateIssueCommentRequest request, StreamObserver<IssueComment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			IssueComment.Builder builder = updateIssueComment(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private IssueComment.Builder updateIssueComment(UpdateIssueCommentRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		// validate parent record
		int recordId = request.getId();
		if (recordId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
			recordId = RecordUtil.getIdFromUuid(I_R_Request.COLUMNNAME_R_Request_ID, request.getUuid(), null);
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}
		MRequestUpdate requestUpdate = new MRequestUpdate(context, recordId, null);
		if (requestUpdate == null || requestUpdate.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_RequestUpdate_ID@ @NotFound@");
		}
		requestUpdate.setResult(
			ValueUtil.validateNull(request.getResult())
		);
		requestUpdate.saveEx();

		return convertRequestUpdate(requestUpdate);
	}


	@Override
	public void deleteIssueComment(DeleteIssueCommentRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Empty.Builder builder = deleteIssueComment(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Empty.Builder deleteIssueComment(DeleteIssueCommentRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());
		
		// validate record
		int recordId = request.getId();
		if (recordId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
			recordId = RecordUtil.getIdFromUuid(I_R_RequestUpdate.Table_Name, request.getUuid(), null);
			if (recordId < 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}

		MRequestUpdate requestUpdate = new MRequestUpdate(context, recordId, null);
		if (requestUpdate == null || requestUpdate.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_RequestUpdate_ID@ @NotFound@");
		}

		requestUpdate.deleteEx(true);

		return Empty.newBuilder();
	}

}
