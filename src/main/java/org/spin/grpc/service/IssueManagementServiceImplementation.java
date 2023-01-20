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

import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.core.domains.models.I_R_RequestType;
import org.adempiere.core.domains.models.I_R_RequestUpdate;
import org.adempiere.core.domains.models.X_R_RequestUpdate;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestType;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
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
import org.spin.backend.grpc.issue_management.IssueManagementGrpc.IssueManagementImplBase;
import org.spin.backend.grpc.issue_management.ListIssueCommentsReponse;
import org.spin.backend.grpc.issue_management.ListIssueCommentsRequest;
import org.spin.backend.grpc.issue_management.ListIssuesReponse;
import org.spin.backend.grpc.issue_management.ListIssuesRequest;
import org.spin.backend.grpc.issue_management.ListRequestTypesRequest;
import org.spin.backend.grpc.issue_management.ListRequestTypesResponse;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesRequest;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesResponse;
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

		Query queryRequestTypes =  new Query(
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
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		//  Set page token
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
		Query querySaleRepresentatives =  new Query(
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
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		//  Set page token
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
		Query queryRequests = new Query(
			context,
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(recordId, table.getAD_Table_ID())
		;

		int recordCount = queryRequests.count();

		ListIssuesReponse.Builder builderList = ListIssuesReponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		//  Set page token
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
		builder.setSalesRepresentative(
			convertSalesRepresentative(request.getSalesRep_ID())
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

		return builder;
	}


	@Override
	public void createIssue(CreateIssueRequest request, StreamObserver<Issue> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Issue.Builder builder = Issue.newBuilder();
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



	@Override
	public void updateIssue(UpdateIssueRequest request, StreamObserver<Issue> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Issue.Builder builder = Issue.newBuilder();
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
		
		// validate record
		int recordId = request.getId();
		if (recordId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
			recordId = RecordUtil.getIdFromUuid(I_R_Request.Table_Name, request.getUuid(), null);
			if (recordId < 0) {
				throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
			}
		}

		MRequest requestRecord = new MRequest(context, recordId, null);
		if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_Request_ID@ @NotFound@");
		}

		requestRecord.deleteEx(true);

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

		final String whereClause = "R_Request_ID = ? ";
		Query queryRequests = new Query(
			context,
			I_R_RequestUpdate.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(recordId)
		;

		int recordCount = queryRequests.count();

		ListIssueCommentsReponse.Builder builderList = ListIssueCommentsReponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * RecordUtil.getPageSize(request.getPageSize());

		//  Set page token
		if (RecordUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		queryRequests
			.setLimit(limit, offset)
			.list(X_R_RequestUpdate.class)
			.forEach(requestRecord -> {
				IssueComment.Builder builder = convertRequestUpdate(requestRecord);
			    builderList.addRecords(builder);
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
		;
		MUser user = MUser.get(Env.getCtx(), requestUpdate.getCreatedBy());
		builder.setUserId(user.getAD_User_ID())
			.setUserUuid(
				ValueUtil.validateNull(user.getUUID())
			)
			.setUserName(
				ValueUtil.validateNull(user.getName())
			)
		;

		return builder;
	}


	@Override
	public void createIssueComment(CreateIssueCommentRequest request, StreamObserver<IssueComment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			IssueComment.Builder builder = IssueComment.newBuilder();
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


	@Override
	public void updateIssueComment(UpdateIssueCommentRequest request, StreamObserver<IssueComment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			IssueComment.Builder builder = IssueComment.newBuilder();
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

		MRequest requestRecord = new MRequest(context, recordId, null);
		if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_RequestUpdate_ID@ @NotFound@");
		}

		requestRecord.deleteEx(true);

		return Empty.newBuilder();
	}

}
