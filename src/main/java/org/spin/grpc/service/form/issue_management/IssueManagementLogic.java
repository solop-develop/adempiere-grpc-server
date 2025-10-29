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
package org.spin.grpc.service.form.issue_management;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_Project;
import org.adempiere.core.domains.models.I_R_Category;
import org.adempiere.core.domains.models.I_R_Group;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.core.domains.models.I_R_RequestAction;
import org.adempiere.core.domains.models.I_R_RequestType;
import org.adempiere.core.domains.models.I_R_RequestUpdate;
import org.adempiere.core.domains.models.I_R_Status;
import org.adempiere.core.domains.models.I_R_StatusCategory;
import org.adempiere.core.domains.models.X_R_Request;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRefList;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestAction;
import org.compiere.model.MRequestUpdate;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.issue_management.BusinessPartner;
import org.spin.backend.grpc.issue_management.Category;
import org.spin.backend.grpc.issue_management.CreateIssueCommentRequest;
import org.spin.backend.grpc.issue_management.CreateIssueRequest;
import org.spin.backend.grpc.issue_management.DeleteIssueCommentRequest;
import org.spin.backend.grpc.issue_management.DeleteIssueRequest;
import org.spin.backend.grpc.issue_management.ExistsIssuesRequest;
import org.spin.backend.grpc.issue_management.ExistsIssuesResponse;
import org.spin.backend.grpc.issue_management.Group;
import org.spin.backend.grpc.issue_management.Issue;
import org.spin.backend.grpc.issue_management.IssueComment;
import org.spin.backend.grpc.issue_management.ListBusinessPartnersRequest;
import org.spin.backend.grpc.issue_management.ListBusinessPartnersResponse;
import org.spin.backend.grpc.issue_management.ListCategoriesRequest;
import org.spin.backend.grpc.issue_management.ListCategoriesResponse;
import org.spin.backend.grpc.issue_management.ListGroupsRequest;
import org.spin.backend.grpc.issue_management.ListGroupsResponse;
import org.spin.backend.grpc.issue_management.ListIssueCommentsReponse;
import org.spin.backend.grpc.issue_management.ListIssueCommentsRequest;
import org.spin.backend.grpc.issue_management.ListIssuesReponse;
import org.spin.backend.grpc.issue_management.ListIssuesRequest;
import org.spin.backend.grpc.issue_management.ListPrioritiesRequest;
import org.spin.backend.grpc.issue_management.ListPrioritiesResponse;
import org.spin.backend.grpc.issue_management.ListProjectsRequest;
import org.spin.backend.grpc.issue_management.ListProjectsResponse;
import org.spin.backend.grpc.issue_management.ListRequestTypesRequest;
import org.spin.backend.grpc.issue_management.ListRequestTypesResponse;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesRequest;
import org.spin.backend.grpc.issue_management.ListSalesRepresentativesResponse;
import org.spin.backend.grpc.issue_management.ListStatusCategoriesRequest;
import org.spin.backend.grpc.issue_management.ListStatusCategoriesResponse;
import org.spin.backend.grpc.issue_management.ListStatusesRequest;
import org.spin.backend.grpc.issue_management.ListStatusesResponse;
import org.spin.backend.grpc.issue_management.ListTaskStatusesRequest;
import org.spin.backend.grpc.issue_management.ListTaskStatusesResponse;
import org.spin.backend.grpc.issue_management.Priority;
import org.spin.backend.grpc.issue_management.Project;
import org.spin.backend.grpc.issue_management.RequestType;
import org.spin.backend.grpc.issue_management.StatusCategory;
import org.spin.backend.grpc.issue_management.TaskStatus;
import org.spin.backend.grpc.issue_management.UpdateIssueCommentRequest;
import org.spin.backend.grpc.issue_management.UpdateIssueRequest;
import org.spin.backend.grpc.issue_management.User;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Empty;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class IssueManagementLogic {

	public static ListRequestTypesResponse.Builder listRequestTypes(ListRequestTypesRequest request) {
		String whereClause = null;
		List<Object> filtersList = new ArrayList<>();

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
			whereClause = " UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query queryRequestTypes = new Query(
			Env.getCtx(),
			I_R_RequestType.Table_Name,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
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
			StringManager.getValidString(nexPageToken)
		);

		queryRequestTypes
			// .setLimit(limit, offset)
			.getIDsAsList()
			// .list(MRequestType.class)
			.forEach(requestTypeId -> {
				RequestType.Builder builder = IssueManagementConvertUtil.convertRequestType(requestTypeId);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListSalesRepresentativesResponse.Builder listSalesRepresentatives(ListSalesRepresentativesRequest request) {
		String whereClause = "EXISTS("
			+ "SELECT * FROM C_BPartner bp WHERE "
			+ "AD_User.C_BPartner_ID=bp.C_BPartner_ID "
			+ "AND (bp.IsEmployee='Y' OR bp.IsSalesRep='Y'))"
		;
		List<Object> filtersList = new ArrayList<>();

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(searchValue);
		}

		Query querySaleRepresentatives = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setParameters(filtersList)
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
			StringManager.getValidString(nexPageToken)
		);

		querySaleRepresentatives
			// .setLimit(limit, offset)
			.getIDsAsList()
			.forEach(userId -> {
				User.Builder builder = IssueManagementConvertUtil.convertUser(userId);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListPrioritiesResponse.Builder listPriorities(ListPrioritiesRequest request) {
		String whereClause = "AD_Reference_ID = ?";

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(X_R_Request.PRIORITY_AD_Reference_ID);

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(searchValue);
		}

		Query queryPriority = new Query(
			Env.getCtx(),
			MRefList.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
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
			StringManager.getValidString(nexPageToken)
		);

		queryPriority
			// .setLimit(limit, offset)
			.list(MRefList.class)
			.forEach(priority -> {
				Priority.Builder builder = IssueManagementConvertUtil.convertPriority(priority);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListStatusCategoriesResponse.Builder listStatusCategories(ListStatusCategoriesRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause.append(
				"UPPER(Name) LIKE '%' || UPPER(?) || '%' "
			);
			filtersList.add(searchValue);
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_StatusCategory.Table_Name,
			whereClause.toString(),
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListStatusCategoriesResponse.Builder builderList = ListStatusCategoriesResponse.newBuilder();
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
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			// .setLimit(limit, offset)
			.getIDsAsList()
			.forEach(statusCategoryId -> {
				StatusCategory.Builder builder = IssueManagementConvertUtil.convertStatusCategory(
					statusCategoryId
				);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListStatusesResponse.Builder listStatuses(ListStatusesRequest request) {
		final int requestTypeId = request.getRequestTypeId();
		final int requestStatusCategoryId = request.getStatusCategoryId();
		if (requestTypeId <= 0 && requestStatusCategoryId <= 0) {
			throw new AdempiereException("@R_RequestType_ID@ / @R_StatusCategory_ID@ @NotFound@");
		}

		String whereClause = "1=1 ";
		List<Object> filtersList = new ArrayList<>();

		if (requestStatusCategoryId > 0) {
			filtersList.add(requestStatusCategoryId);
			whereClause = "R_StatusCategory_ID = ? ";
		} else if (requestTypeId > 0) {
			filtersList.add(requestTypeId);
			whereClause = "EXISTS ("
				+ "SELECT * FROM R_RequestType rt "
				+ "INNER JOIN R_StatusCategory sc "
				+ "ON (rt.R_StatusCategory_ID = sc.R_StatusCategory_ID) "
				+ "WHERE R_Status.R_StatusCategory_ID = sc.R_StatusCategory_ID "
				+ "AND rt.R_RequestType_ID = ?"
				+ ") "
			;
		}

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
			filtersList.add(searchValue);
			whereClause += " AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "AND UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+") "
			;
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_Status.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListStatusesResponse.Builder builderList = ListStatusesResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			.setOrderBy(I_R_Status.COLUMNNAME_IsDefault + " DESC, " + I_R_Status.COLUMNNAME_SeqNo)
			// .setLimit(limit, offset)
			.getIDsAsList()
			// .list(MStatus.class)
			.forEach(statusId -> {
				org.spin.backend.grpc.issue_management.Status.Builder builder = IssueManagementConvertUtil.convertStatus(statusId);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListCategoriesResponse.Builder listCategories(ListCategoriesRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause.append(
				"UPPER(Name) LIKE '%' || UPPER(?) || '%' "
			);
			filtersList.add(searchValue);
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_Category.Table_Name,
			whereClause.toString(),
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListCategoriesResponse.Builder builderList = ListCategoriesResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			// .setLimit(limit, offset)
			.getIDsAsList()
			.forEach(categoryId -> {
				Category.Builder builder = IssueManagementConvertUtil.convertCategory(
					categoryId
				);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListGroupsResponse.Builder listGroups(ListGroupsRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause.append(
				"UPPER(Name) LIKE '%' || UPPER(?) || '%' "
			);
			filtersList.add(searchValue);
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_Group.Table_Name,
			whereClause.toString(),
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListGroupsResponse.Builder builderList = ListGroupsResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			// .setLimit(limit, offset)
			.getIDsAsList()
			.forEach(groupId -> {
				Group.Builder builder = IssueManagementConvertUtil.convertGroup(
					groupId
				);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListTaskStatusesResponse.Builder listTaskStatuses(ListTaskStatusesRequest request) {
		String whereClause = "AD_Reference_ID = ?";

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(X_R_Request.TASKSTATUS_AD_Reference_ID);

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}

		Query queryPriority = new Query(
			Env.getCtx(),
			MRefList.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryPriority.count();

		ListTaskStatusesResponse.Builder builderList = ListTaskStatusesResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryPriority
			// .setLimit(limit, offset)
			.list(MRefList.class)
			.forEach(priority -> {
				TaskStatus.Builder builder = IssueManagementConvertUtil.convertTaskStatus(priority);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListBusinessPartnersResponse.Builder listBusinessPartners(ListBusinessPartnersRequest request) {
		List<Object> filtersList = new ArrayList<>();
		String whereClause = "";

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause = "("
				+ "UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ " OR UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ " OR UPPER(TaxID) LIKE '%' || UPPER(?) || '%' "
				+ ") "
			;
			filtersList.add(searchValue);
			filtersList.add(searchValue);
			filtersList.add(searchValue);
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_C_BPartner.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListBusinessPartnersResponse.Builder builderList = ListBusinessPartnersResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			// .setLimit(limit, offset)
			.getIDsAsList()
			.forEach(businessPartnerId -> {
				BusinessPartner.Builder builder = IssueManagementConvertUtil.convertBusinessPartner(
					businessPartnerId
				);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListProjectsResponse.Builder listProjects(ListProjectsRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause.append(
				"(UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ " OR UPPER(Value) LIKE '%' || UPPER(?) || '%' )"
			);
			filtersList.add(searchValue);
			filtersList.add(searchValue);
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_C_Project.Table_Name,
			whereClause.toString(),
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListProjectsResponse.Builder builderList = ListProjectsResponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(projectId -> {
				Project.Builder builder = IssueManagementConvertUtil.convertProject(
					projectId
				);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ExistsIssuesResponse.Builder existsIssues(ExistsIssuesRequest request) {
		ExistsIssuesResponse.Builder builder = ExistsIssuesResponse.newBuilder();

		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

		// validate record
		final int recordId = request.getRecordId();
		final boolean isValidIdentifier = RecordUtil.isValidId(
			recordId,
			table
		);
		if (!isValidIdentifier) {
			return builder;
		}

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

		builder.setRecordCount(recordCount);

		return builder;
	}



	public static ListIssuesReponse.Builder listMyIssues(ListIssuesRequest request) {
		List<Object> parametersList = new ArrayList<>();
		String whereClause = "";

		int userId = Env.getAD_User_ID(Env.getCtx());
		int roleId = Env.getAD_Role_ID(Env.getCtx());

		parametersList.add(userId);
		parametersList.add(roleId);
		whereClause = "Processed='N' "
			+ "AND (SalesRep_ID=? OR AD_Role_ID = ?) "
		;

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += " AND ("
				+ "UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Subject) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Summary) LIKE '%' || UPPER(?) || '%' "
				+ ") "
			;
			parametersList.add(searchValue);
			parametersList.add(searchValue);
			parametersList.add(searchValue);
		}

		if (request.getCategoryId() > 0) {
			whereClause += " AND (R_Category_ID = ?) ";
			parametersList.add(
				request.getCategoryId()
			);
		}
		if (request.getTypeId() > 0) {
			whereClause += " AND (R_RequestType_ID = ?) ";
			parametersList.add(
				request.getTypeId()
			);
		}
		// filter status by status category
		if (request.getStatusCategoryId() > 0) {
			whereClause += " AND EXISTS("
				+ "SELECT 1 FROM R_Status AS sc "
				+ "WHERE sc.R_StatusCategory_ID = ? "
				+ "AND R_Request.R_Status_ID = sc.R_Status_ID"
				+ ") "
			;
			parametersList.add(request.getStatusCategoryId());
		}
		if (request.getStatusId() > 0) {
			whereClause += " AND (R_Status_ID = ?) ";
			parametersList.add(
				request.getStatusId()
			);
		} else {
			whereClause += " AND (R_Status_ID IS NULL "
				+ " OR R_Status_ID IN ("
				+ "SELECT R_Status_ID FROM R_Status WHERE IsClosed='N') "
				+ ") "
			;
		}

		if (request.getGroupId() > 0) {
			whereClause += " AND (R_Group_ID = ?) ";
			parametersList.add(
				request.getGroupId()
			);
		}

		if (request.getBusinessPartnerId() > 0) {
			whereClause += " AND (C_BPartner_ID = ?) ";
			parametersList.add(
				request.getBusinessPartnerId()
			);
		}

		if (request.getProjectId() > 0) {
			// TODO: Add Project Phase and Poject Task
			whereClause += " AND ("
				+ "C_Project_ID = ? "
				+ "OR EXISTS( "
					+ "SELECT 1 FROM C_ProjectLine AS pl "
					+ "WHERE pl.C_Project_ID = ? "
					+ "AND pl.C_ProjectLine_ID = R_Request.C_ProjectLine_ID"
					+ ") "
				+ ") "
			;
			parametersList.add(
				request.getProjectId()
			);
			parametersList.add(
				request.getProjectId()
			);
		}

		if (!Util.isEmpty(request.getPriorityValue(), true)) {
			whereClause += " AND (Priority = ?) ";
			parametersList.add(
				request.getPriorityValue()
			);
		}

		if (!Util.isEmpty(request.getTaskStatusValue(), true)) {
			whereClause += " AND (TaskStatus = ?) ";
			parametersList.add(
				request.getTaskStatusValue()
			);
		}

		// Created Date
		Timestamp createdFrom = ValueManager.getTimestampFromProtoTimestamp(
			request.getCreatedFrom()
		);
		Timestamp createdTo = ValueManager.getTimestampFromProtoTimestamp(
			request.getCreatedTo()
		);
		if (createdFrom != null || createdTo != null) {
			whereClause += " AND ";
			if (createdFrom != null && createdTo != null) {
				whereClause += "TRUNC(Created, 'DD') BETWEEN ? AND ? ";
				parametersList.add(createdFrom);
				parametersList.add(createdTo);
			}
			else if (createdFrom != null) {
				whereClause += "TRUNC(Created, 'DD') >= ? ";
				parametersList.add(createdFrom);
			}
			else {
				// DateTo != null
				whereClause += "TRUNC(Created, 'DD') <= ? ";
				parametersList.add(createdTo);
			}
		}

		// Date Next Action
		Timestamp dateNextActionFrom = ValueManager.getTimestampFromProtoTimestamp(
			request.getDateNextActionFrom()
		);
		Timestamp dateNextActionTo = ValueManager.getTimestampFromProtoTimestamp(
			request.getDateNextActionFrom()
		);
		whereClause += " AND (DateNextAction IS NULL ";
		if (dateNextActionFrom != null || dateNextActionTo != null) {
			if (dateNextActionFrom != null && dateNextActionTo != null) {
				whereClause += "OR TRUNC(DateNextAction, 'DD') BETWEEN ? AND ? ";
				parametersList.add(dateNextActionFrom);
				parametersList.add(dateNextActionTo);
			}
			else if (dateNextActionFrom != null) {
				whereClause += "OR TRUNC(DateNextAction, 'DD') >= ? ";
				parametersList.add(dateNextActionFrom);
			}
			else {
				// DateTo != null
				whereClause += "OR TRUNC(DateNextAction, 'DD') <= ? ";
				parametersList.add(dateNextActionTo);
			}
		} else {
			whereClause += "OR TRUNC(DateNextAction, 'DD') <= TRUNC(SysDate, 'DD')";
		}
		whereClause += ") ";

		// Reset all filters on window
		if (!Util.isEmpty(request.getTableName(), true)) {
			// validate and get table
			final MTable table = RecordUtil.validateAndGetTable(
				request.getTableName()
			);

			// validate record
			int recordId = request.getRecordId();
			if (!RecordUtil.isValidId(recordId, table.getAccessLevel())) {
				throw new AdempiereException("@Record_ID@ / @NotFound@");
			}
			parametersList.clear();
			parametersList.add(table.getAD_Table_ID());
			parametersList.add(recordId);
			whereClause = "AD_Table_ID = ? AND Record_ID = ? ";
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			// TODO: Fix Record access with pagination
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setParameters(parametersList)
		;

		int recordCount = queryRequests.count();

		ListIssuesReponse.Builder builderList = ListIssuesReponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			.setLimit(limit, offset)
			.setOrderBy(I_R_Request.COLUMNNAME_DateNextAction + " NULLS FIRST ")
			.getIDsAsList()
			// .list(MRequest.class)
			.forEach(requestRecordId -> {
				Issue.Builder builder = IssueManagementConvertUtil.convertRequest(requestRecordId);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static ListIssuesReponse.Builder listIssues(ListIssuesRequest request) {
		List<Object> parametersList = new ArrayList<>();
		String whereClause = "Processed='N' ";

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += " AND ("
				+ "UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Subject) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Summary) LIKE '%' || UPPER(?) || '%' "
				+ ")"
			;
			parametersList.add(searchValue);
			parametersList.add(searchValue);
			parametersList.add(searchValue);
		}

		if (request.getSalesRepresentativeId() > 0) {
			whereClause += " AND (SalesRep_ID = ?) ";
			parametersList.add(
				request.getSalesRepresentativeId()
			);
		}

		if (request.getCategoryId() > 0) {
			whereClause += " AND (R_Category_ID = ?) ";
			parametersList.add(
				request.getCategoryId()
			);
		}
		if (request.getTypeId() > 0) {
			whereClause += " AND (R_RequestType_ID = ?) ";
			parametersList.add(
				request.getTypeId()
			);
		}
		// filter status by status category
		if (request.getStatusCategoryId() > 0) {
			whereClause += " AND EXISTS("
				+ "SELECT 1 FROM R_Status AS sc "
				+ "WHERE sc.R_StatusCategory_ID = ? "
				+ "AND R_Request.R_Status_ID = sc.R_Status_ID"
				+ ")"
			;
			parametersList.add(request.getStatusCategoryId());
		}
		if (request.getStatusId() > 0) {
			whereClause += " AND (R_Status_ID = ?) ";
			parametersList.add(
				request.getStatusId()
			);
		}

		if (request.getGroupId() > 0) {
			whereClause += " AND (R_Group_ID = ?) ";
			parametersList.add(
				request.getGroupId()
			);
		}

		if (request.getBusinessPartnerId() > 0) {
			whereClause += " AND (C_BPartner_ID = ?) ";
			parametersList.add(
				request.getBusinessPartnerId()
			);
		}

		if (request.getProjectId() > 0) {
			// TODO: Add Project Phase and Poject Task
			whereClause += " AND ("
				+ "C_Project_ID = ? "
				+ "OR EXISTS( "
					+ "SELECT 1 FROM C_ProjectLine AS pl "
					+ "WHERE pl.C_Project_ID = ? "
					+ "AND pl.C_ProjectLine_ID = R_Request.C_ProjectLine_ID"
					+ ") "
				+ ") "
			;
			parametersList.add(
				request.getProjectId()
			);
			parametersList.add(
				request.getProjectId()
			);
		}

		if (!Util.isEmpty(request.getPriorityValue(), true)) {
			whereClause += " AND (Priority = ?) ";
			parametersList.add(
				request.getPriorityValue()
			);
		}

		if (!Util.isEmpty(request.getTaskStatusValue(), true)) {
			whereClause += " AND (TaskStatus = ?) ";
			parametersList.add(
				request.getTaskStatusValue()
			);
		}

		Query queryRequests = new Query(
			Env.getCtx(),
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setParameters(parametersList)
		;

		int recordCount = queryRequests.count();

		ListIssuesReponse.Builder builderList = ListIssuesReponse.newBuilder()
			.setRecordCount(recordCount)
		;

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);

		queryRequests
			.setLimit(limit, offset)
			.setOrderBy(I_R_Request.COLUMNNAME_DateNextAction + " NULLS FIRST ")
			.getIDsAsList()
			// .list(MRequest.class)
			.forEach(requestRecordId -> {
				Issue.Builder builder = IssueManagementConvertUtil.convertRequest(requestRecordId);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}



	public static Issue.Builder createIssue(CreateIssueRequest request) {
		MRequest requestRecord = new MRequest(Env.getCtx(), 0, null);

		// create issue with record on window
		if (!Util.isEmpty(request.getTableName(), true) || request.getRecordId() > 0) {
			// validate and get table
			final MTable table = RecordUtil.validateAndGetTable(
				request.getTableName()
			);

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
		requestRecord.setR_Status_ID(request.getStatusId());
		requestRecord.setSubject(request.getSubject());
		requestRecord.setSummary(request.getSummary());
		requestRecord.setSalesRep_ID(salesRepresentativeId);
		requestRecord.setPriority(
			StringManager.getValidString(
				request.getPriorityValue()
			)
		);
		requestRecord.setDateNextAction(
			TimeManager.getTimestampFromString(request.getDateNextAction())
		);
		if (request.getCategoryId() > 0) {
			requestRecord.setR_Category_ID(
				request.getCategoryId()
			);
		}
		if (request.getGroupId() > 0) {
			requestRecord.setR_Group_ID(
				request.getGroupId()
			);
		}
		if (!Util.isEmpty(request.getTaskStatusValue(), true)) {
			requestRecord.setTaskStatus(
				request.getTaskStatusValue()
			);
		}
		if (request.getBusinessPartnerId() > 0) {
			requestRecord.setC_BPartner_ID(
				request.getBusinessPartnerId()
			);
		}
		if (request.getProjectId() > 0) {
			requestRecord.setC_Project_ID(
				request.getProjectId()
			);
		}
		requestRecord.saveEx();

		Issue.Builder builder = IssueManagementConvertUtil.convertRequest(requestRecord);

		return builder;
	}



	public static Issue.Builder updateIssue(UpdateIssueRequest request) {
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
			StringManager.getValidString(
				request.getPriorityValue()
			)
		);
		requestRecord.setDateNextAction(
			ValueManager.getTimestampFromProtoTimestamp(
				request.getDateNextAction()
			)
		);
		
		requestRecord.setR_Status_ID(request.getStatusId());
		requestRecord.setR_Category_ID(
			request.getCategoryId()
		);
		requestRecord.setR_Group_ID(
			request.getGroupId()
		);
		String taskStatus = null;
		if (!Util.isEmpty(request.getTaskStatusValue(), true)) {
			taskStatus = request.getTaskStatusValue();
		}
		requestRecord.setTaskStatus(
			taskStatus
		);
		requestRecord.setC_BPartner_ID(
			request.getBusinessPartnerId()
		);
		requestRecord.setC_Project_ID(
			request.getProjectId()
		);

		requestRecord.saveEx();

		Issue.Builder builder = IssueManagementConvertUtil.convertRequest(requestRecord);
		return builder;
	}



	public static Empty.Builder deleteIssue(DeleteIssueRequest request) {
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
				.parallelStream()
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
				.parallelStream()
				.forEach(requestUpdateId -> {
					MRequestUpdate requestUpdate = new MRequestUpdate(Env.getCtx(), requestUpdateId, null);
					requestUpdate.deleteEx(true);
				});

			// delete header
			requestRecord.deleteEx(true);
		});

		return Empty.newBuilder();
	}



	public static ListIssueCommentsReponse.Builder listIssueComments(ListIssueCommentsRequest request) {
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
			StringManager.getValidString(nexPageToken)
		);

		List<IssueComment.Builder> issueCommentsList = new ArrayList<>();
		queryRequestsUpdate
			// .setLimit(limit, offset)
			.getIDsAsList()
			// .list(X_R_RequestUpdate.class)
			.forEach(requestUpdateId -> {
				IssueComment.Builder builder = IssueManagementConvertUtil.convertRequestUpdate(requestUpdateId);
				issueCommentsList.add(builder);
				// builderList.addRecords(builder);
			})
		;

		queryRequestsLog
			// .setLimit(limit, offset)
			.getIDsAsList()
			// .list(MRequestAction.class)
			.forEach(requestActionId -> {
				IssueComment.Builder builder = IssueManagementConvertUtil.convertRequestAction(requestActionId);
				issueCommentsList.add(builder);
				// builderList.addRecords(builder);
			})
		;

		issueCommentsList.stream()
			.sorted((comment1, comment2) -> {
				Timestamp from = ValueManager.getTimestampFromProtoTimestamp(
					comment1.getCreated()
				);

				Timestamp to = ValueManager.getTimestampFromProtoTimestamp(
					comment2.getCreated()
				);

				if (from == null || to == null) {
					// prevent Null Pointer Exception
					if (from == null && to == null) {
						return 0;
					} else if (from == null) {
						return -1;
					} else if (to == null) {
						return 1;
					}
				}
				int compared = to.compareTo(from);
				return compared;
			})
			.forEach(issueComment -> {
				builderList.addRecords(issueComment);
			})
		;

		return builderList;
	}


	public static IssueComment.Builder createIssueComment(CreateIssueCommentRequest request) {
		// validate parent record
		int recordId = request.getIssueId();
		if (recordId < 0) {
			throw new AdempiereException("@Record_ID@ @NotFound@");
		}
		MRequest requestRecord = new MRequest(Env.getCtx(), recordId, null);
		requestRecord.setResult(
			StringManager.getValidString(
				request.getResult()
			)
		);
		requestRecord.saveEx();

		// requestRecord.request
		MRequestUpdate requestUpdate = new Query(
			Env.getCtx(),
			I_R_RequestUpdate.Table_Name,
			"R_Request_ID = ?",
			requestRecord.get_TrxName()
		)
			.setParameters(
				requestRecord.getR_Request_ID()
			)
			.setOrderBy(
				I_R_RequestUpdate.COLUMNNAME_Created + " DESC "
			)
			.first()
		;
		return IssueManagementConvertUtil.convertRequestUpdate(
			requestUpdate
		);
	}


	public static IssueComment.Builder updateIssueComment(UpdateIssueCommentRequest request) {
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
			StringManager.getValidString(
				request.getResult()
			)
		);
		requestUpdate.saveEx();

		return IssueManagementConvertUtil.convertRequestUpdate(requestUpdate);
	}


	public static Empty.Builder deleteIssueComment(DeleteIssueCommentRequest request) {
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
