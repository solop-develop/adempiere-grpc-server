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

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.core.domains.models.I_R_RequestAction;
import org.adempiere.core.domains.models.I_R_RequestType;
import org.adempiere.core.domains.models.I_R_RequestUpdate;
import org.adempiere.core.domains.models.I_R_Status;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRefList;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestAction;
import org.compiere.model.MRequestUpdate;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
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
import org.spin.backend.grpc.issue_management.ListPrioritiesRequest;
import org.spin.backend.grpc.issue_management.ListPrioritiesResponse;
import org.spin.backend.grpc.issue_management.ListRequestTypesRequest;
import org.spin.backend.grpc.issue_management.ListRequestTypesResponse;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesRequest;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesResponse;
import org.spin.backend.grpc.issue_management.ListStatusesRequest;
import org.spin.backend.grpc.issue_management.ListStatusesResponse;
import org.spin.backend.grpc.issue_management.Priority;
import org.spin.backend.grpc.issue_management.RequestType;
import org.spin.backend.grpc.issue_management.UpdateIssueCommentRequest;
import org.spin.backend.grpc.issue_management.UpdateIssueRequest;
import org.spin.backend.grpc.issue_management.User;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.RecordUtil;
import org.spin.form.issue_management.IssueManagementConvertUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class IssueManagement extends IssueManagementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(IssueManagement.class);


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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListRequestTypesResponse.Builder listRequestTypes(ListRequestTypesRequest request) {
		String whereClause = null;
		List<Object> filtersList = new ArrayList<>();
		if (!Util.isEmpty(request.getSearchValue(), false)) {
			filtersList.add(request.getSearchValue());
			whereClause = " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query queryRequestTypes = new Query(
			Env.getCtx(),
			I_R_RequestType.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setParameters(filtersList)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOnlyActiveRecords(true)
		;
		int recordCount = queryRequestTypes.count();

		ListRequestTypesResponse.Builder builderList = ListRequestTypesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		queryRequestTypes
			.setLimit(limit, offset)
			.getIDsAsList()
			// .list(MRequestType.class)
			.forEach(requestTypeId -> {
				RequestType.Builder builder = IssueManagementConvertUtil.convertRequestType(requestTypeId);
				builderList.addRecords(builder);
			});

		return builderList;
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListSalesRepresentativesResponse.Builder listSalesRepresentatives(ListSalesRepresentativesRequest request) {
		String whereClause = "EXISTS("
			+ "SELECT * FROM C_BPartner bp WHERE "
			+ "AD_User.C_BPartner_ID=bp.C_BPartner_ID "
			+ "AND (bp.IsEmployee='Y' OR bp.IsSalesRep='Y'))"
		;
		List<Object> filtersList = new ArrayList<>();
		if (!Util.isEmpty(request.getSearchValue(), false)) {
			filtersList.add(request.getSearchValue());
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query querySaleRepresentatives = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setParameters(filtersList)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOnlyActiveRecords(true)
		;
		int recordCount = querySaleRepresentatives.count();

		ListSalesRepresentativesResponse.Builder builderList = ListSalesRepresentativesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		querySaleRepresentatives
			.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(userId -> {
				User.Builder builder = IssueManagementConvertUtil.convertUser(userId);
				builderList.addRecords(builder);
			});

		return builderList;
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListPrioritiesResponse.Builder listPriorities(ListPrioritiesRequest request) {
		String whereClause = "AD_Reference_ID = ?";

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(MRequest.PRIORITY_AD_Reference_ID);

		if (!Util.isEmpty(request.getSearchValue(), false)) {
			filtersList.add(request.getSearchValue());
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query queryPriority = new Query(
			Env.getCtx(),
			MRefList.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryPriority.count();

		ListPrioritiesResponse.Builder builderList = ListPrioritiesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		queryPriority
			.setLimit(limit, offset)
			.list(MRefList.class)
			.forEach(priority -> {
				Priority.Builder builder = IssueManagementConvertUtil.convertPriority(priority);
				builderList.addRecords(builder);
			});

		return builderList;
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListStatusesResponse.Builder listStatuses(ListStatusesRequest request) {
		int requestTypeId = request.getRequestTypeId();
		if (requestTypeId <= 0) {
			throw new AdempiereException("@R_RequestType_ID@ @NotFound@");
		}

		String whereClause = "EXISTS (SELECT * FROM R_RequestType rt "
			+ "INNER JOIN R_StatusCategory sc "
			+ "ON (rt.R_StatusCategory_ID = sc.R_StatusCategory_ID) "
			+ "WHERE R_Status.R_StatusCategory_ID = sc.R_StatusCategory_ID "
			+ "AND rt.R_RequestType_ID = ?)"
		;

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(requestTypeId);

		if (!Util.isEmpty(request.getSearchValue(), false)) {
			filtersList.add(request.getSearchValue());
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_Status.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListStatusesResponse.Builder builderList = ListStatusesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		queryRequests
			.setLimit(limit, offset)
			.getIDsAsList()
			// .list(MStatus.class)
			.forEach(statusId -> {
				org.spin.backend.grpc.issue_management.Status.Builder builder = IssueManagementConvertUtil.convertStatus(statusId);
				builderList.addRecords(builder);
			});

		return builderList;
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}
	
	private ExistsIssuesResponse.Builder existsIssues(ExistsIssuesRequest request) {
		if (Util.isEmpty(request.getTableName(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}

		MTable table = MTable.get(Env.getCtx(), request.getTableName());
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}

		// validate record
		int recordId = request.getRecordId();
		final String whereClause = "Record_ID = ? "
			+ "AND AD_Table_ID = ? "
		;
		int recordCount = new Query(
			Env.getCtx(),
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			.setParameters(recordId, table.getAD_Table_ID())
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListIssuesReponse.Builder listIssues(ListIssuesRequest request) {
		List<Object> parametersList = new ArrayList<>();
		String whereClause = "";

		if (!Util.isEmpty(request.getTableName(), true)) {
			// validate table
			if (Util.isEmpty(request.getTableName(), true)) {
				throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
			}
			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			if (table == null || table.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@AD_Table_ID@ @NotFound@");
			}

			// validate record
			int recordId = request.getRecordId();
			if (recordId < 0) {
				throw new AdempiereException("@Record_ID@ / @NotFound@");
			}
			parametersList.add(table.getAD_Table_ID());
			parametersList.add(recordId);
			whereClause = "AD_Table_ID = ? AND Record_ID = ? ";
		} else {
			int userId = Env.getAD_User_ID(Env.getCtx());
			int roleId = Env.getAD_Role_ID(Env.getCtx());

			parametersList.add(userId);
			parametersList.add(roleId);
			whereClause = "Processed='N' "
				+ "AND (SalesRep_ID=? OR AD_Role_ID = ?) "
				+ "AND (R_Status_ID IS NULL OR R_Status_ID IN (SELECT R_Status_ID FROM R_Status WHERE IsClosed='N'))"
			;
		}

		if (!Util.isEmpty(request.getSearchValue(), false)) {
			whereClause += " AND (UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Subject) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Summary) LIKE '%' || UPPER(?) || '%' )"
			;
			parametersList.add(request.getSearchValue());
			parametersList.add(request.getSearchValue());
			parametersList.add(request.getSearchValue());
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setParameters(parametersList)
		;

		int recordCount = queryRequests.count();

		ListIssuesReponse.Builder builderList = ListIssuesReponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		queryRequests
			.setLimit(limit, offset)
			.setOrderBy(I_R_Request.COLUMNNAME_DateNextAction + " NULLS FIRST ")
			.getIDsAsList()
			// .list(MRequest.class)
			.forEach(requestRecordId -> {
				Issue.Builder builder = IssueManagementConvertUtil.convertRequest(requestRecordId);
				builderList.addRecords(builder);
			});

		return builderList;
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Issue.Builder createIssue(CreateIssueRequest request) {
		MRequest requestRecord = new MRequest(Env.getCtx(), 0, null);

		// create issue with record on window
		if (!Util.isEmpty(request.getTableName(), true) || request.getRecordId() > 0) {
			if (Util.isEmpty(request.getTableName(), true)) {
				throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
			}

			MTable table = MTable.get(Env.getCtx(), request.getTableName());
			if (table == null || table.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@AD_Table_ID@ @NotFound@");
			}

			// validate record
			if (request.getRecordId() < 0) {
				throw new AdempiereException("@Record_ID@ / @NotFound@");
			}
			PO entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), request.getRecordId(), null);
			if (entity == null) {
				throw new AdempiereException("@PO@ @NotFound@");
			}
			PO.copyValues(entity, requestRecord, true);

			// validate if entity key column exists on request to set
			String keyColumn = entity.get_TableName() + "_ID";
			if (requestRecord.get_ColumnIndex(keyColumn) >= 0) {
				requestRecord.set_ValueOfColumn(keyColumn, entity.get_ID());
			}
			requestRecord.setRecord_ID(entity.get_ID());
			requestRecord.setAD_Table_ID(table.getAD_Table_ID());
		}

		if (Util.isEmpty(request.getSubject(), true)) {
			throw new AdempiereException("@FillMandatory@ @Subject@");
		}

		if (Util.isEmpty(request.getSummary(), true)) {
			throw new AdempiereException("@FillMandatory@ @Summary@");
		}

		int requestTypeId = request.getRequestTypeId();
		if (requestTypeId <= 0) {
			throw new AdempiereException("@R_RequestType_ID@ @NotFound@");
		}

		int salesRepresentativeId = request.getSalesRepresentativeId();
		if (salesRepresentativeId <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @NotFound@");
		}

		// fill values
		requestRecord.setR_RequestType_ID(requestTypeId);
		requestRecord.setSubject(request.getSubject());
		requestRecord.setSummary(request.getSummary());
		requestRecord.setSalesRep_ID(salesRepresentativeId);
		requestRecord.setPriority(
			ValueManager.validateNull(request.getPriorityValue())
		);
		requestRecord.setDateNextAction(
			TimeManager.getTimestampFromString(request.getDateNextAction())
		);
		requestRecord.saveEx();

		Issue.Builder builder = IssueManagementConvertUtil.convertRequest(requestRecord);

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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Issue.Builder updateIssue(UpdateIssueRequest request) {
		// validate record
		int recordId = request.getId();
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
		if (requestTypeId <= 0) {
			throw new AdempiereException("@R_RequestType_ID@ @NotFound@");
		}

		int salesRepresentativeId = request.getSalesRepresentativeId();
		if (salesRepresentativeId <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @NotFound@");
		}

		MRequest requestRecord = new MRequest(Env.getCtx(), recordId, null);
		if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_Request_ID@ @NotFound@");
		}
		requestRecord.setR_RequestType_ID(requestTypeId);
		requestRecord.setSubject(request.getSubject());
		requestRecord.setSummary(request.getSummary());
		requestRecord.setSalesRep_ID(salesRepresentativeId);
		requestRecord.setPriority(
			ValueManager.validateNull(request.getPriorityValue())
		);
		requestRecord.setDateNextAction(
			ValueManager.getDateFromTimestampDate(request.getDateNextAction())
		);
		
		if (request.getStatusId() > 0) {
			int statusId = request.getStatusId();
			requestRecord.setR_Status_ID(statusId);
		}

		requestRecord.saveEx();

		Issue.Builder builder = IssueManagementConvertUtil.convertRequest(requestRecord);
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Empty.Builder deleteIssue(DeleteIssueRequest request) {
		Trx.run(transactionName -> {
			// validate record
			int recordId = request.getId();
			if (recordId < 0) {
				throw new AdempiereException("@Record_ID@ / @NotFound@");
			}
			MRequest requestRecord = new MRequest(Env.getCtx(), recordId, transactionName);
			if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
				throw new AdempiereException("@R_Request_ID@ @NotFound@");
			}

			final String whereClause = "R_Request_ID = ?";

			// delete actions
			new Query(
				Env.getCtx(),
				I_R_RequestAction.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(requestRecord.getR_Request_ID())
				.getIDsAsList()
				// .list(MRequestAction.class);
				.forEach(requestActionId -> {
					MRequestAction requestAction = new MRequestAction(Env.getCtx(), requestActionId, null);
					requestAction.deleteEx(true);
				});

			// delete updates
			new Query(
				Env.getCtx(),
				I_R_RequestUpdate.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(requestRecord.getR_Request_ID())
				.getIDsAsList()
				// .list(MRequestUpdate.class);
				.forEach(requestUpdateId -> {
					MRequestUpdate requestUpdate = new MRequestUpdate(Env.getCtx(), requestUpdateId, null);
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListIssueCommentsReponse.Builder listIssueComments(ListIssueCommentsRequest request) {
		// validate parent record
		int recordId = request.getIssueId();
		if (recordId < 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		
		MRequest requestRecord = new MRequest(Env.getCtx(), recordId, null);
		if (requestRecord == null || requestRecord.getR_Request_ID() <= 0) {
			throw new AdempiereException("@Record_ID@ / @UUID@ @NotFound@");
		}

		final String whereClause = "R_Request_ID = ? ";
		Query queryRequestsUpdate = new Query(
			Env.getCtx(),
			I_R_RequestUpdate.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(recordId)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		Query queryRequestsLog = new Query(
			Env.getCtx(),
			I_R_RequestAction.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(recordId)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		int recordCount = queryRequestsUpdate.count() + queryRequestsLog.count();

		ListIssueCommentsReponse.Builder builderList = ListIssueCommentsReponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			ValueManager.validateNull(nexPageToken)
		);

		List<IssueComment.Builder> issueCommentsList = new ArrayList<>();
		queryRequestsUpdate
			.setLimit(limit, offset)
			.getIDsAsList()
			// .list(X_R_RequestUpdate.class)
			.forEach(requestUpdateId -> {
				IssueComment.Builder builder = IssueManagementConvertUtil.convertRequestUpdate(requestUpdateId);
				issueCommentsList.add(builder);
				// builderList.addRecords(builder);
			});

		queryRequestsLog
			.setLimit(limit, offset)
			.getIDsAsList()
			// .list(MRequestAction.class)
			.forEach(requestActionId -> {
				IssueComment.Builder builder = IssueManagementConvertUtil.convertRequestAction(requestActionId);
				issueCommentsList.add(builder);
				// builderList.addRecords(builder);
			});

		issueCommentsList.stream()
		//	TODO: Add support here... Other way?
//			.sorted(Comparator.comparing(IssueComment.Builder::getCreated))
			.forEach(issueComment -> {
				builderList.addRecords(issueComment);
			});

		return builderList;
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private IssueComment.Builder createIssueComment(CreateIssueCommentRequest request) {
		// validate parent record
		int recordId = request.getIssueId();
		if (recordId < 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		MRequest requestRecord = new MRequest(Env.getCtx(), recordId, null);
		requestRecord.setResult(
			ValueManager.validateNull(request.getResult())
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private IssueComment.Builder updateIssueComment(UpdateIssueCommentRequest request) {
		// validate parent record
		int recordId = request.getId();
		if (recordId <= 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		// validate entity
		MRequestUpdate requestUpdate = new MRequestUpdate(Env.getCtx(), recordId, null);
		if (requestUpdate == null || requestUpdate.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_RequestUpdate_ID@ @NotFound@");
		}
		int userId = Env.getAD_User_ID(Env.getCtx());
		if (requestUpdate.getCreatedBy() != userId) {
			throw new AdempiereException("@ActionNotAllowedHere@");
		}
		if (Util.isEmpty(request.getResult(), true)) {
			throw new AdempiereException("@Result@ @NotFound@");
		}

		requestUpdate.setResult(
			ValueManager.validateNull(request.getResult())
		);
		requestUpdate.saveEx();

		return IssueManagementConvertUtil.convertRequestUpdate(requestUpdate);
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
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Empty.Builder deleteIssueComment(DeleteIssueCommentRequest request) {
		// validate record
		int recordId = request.getId();
		if (recordId < 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		// validate entity
		MRequestUpdate requestUpdate = new MRequestUpdate(Env.getCtx(), recordId, null);
		if (requestUpdate == null || requestUpdate.getR_Request_ID() <= 0) {
			throw new AdempiereException("@R_RequestUpdate_ID@ @NotFound@");
		}
		int userId = Env.getAD_User_ID(Env.getCtx());
		if (requestUpdate.getCreatedBy() != userId) {
			throw new AdempiereException("@ActionNotAllowedHere@");
		}

		requestUpdate.deleteEx(true);

		return Empty.newBuilder();
	}

}
