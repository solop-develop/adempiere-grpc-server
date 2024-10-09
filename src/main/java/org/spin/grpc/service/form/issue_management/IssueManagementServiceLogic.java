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
import org.adempiere.core.domains.models.I_R_RequestType;
import org.adempiere.core.domains.models.I_R_Status;
import org.adempiere.core.domains.models.I_R_StatusCategory;
import org.adempiere.core.domains.models.X_R_Request;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.issue_management.BusinessPartner;
import org.spin.backend.grpc.issue_management.Category;
import org.spin.backend.grpc.issue_management.ExistsIssuesRequest;
import org.spin.backend.grpc.issue_management.ExistsIssuesResponse;
import org.spin.backend.grpc.issue_management.Group;
import org.spin.backend.grpc.issue_management.Issue;
import org.spin.backend.grpc.issue_management.ListBusinessPartnersRequest;
import org.spin.backend.grpc.issue_management.ListBusinessPartnersResponse;
import org.spin.backend.grpc.issue_management.ListCategoriesRequest;
import org.spin.backend.grpc.issue_management.ListCategoriesResponse;
import org.spin.backend.grpc.issue_management.ListGroupsRequest;
import org.spin.backend.grpc.issue_management.ListGroupsResponse;
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
import org.spin.backend.grpc.issue_management.User;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class IssueManagementServiceLogic {

	public static ListRequestTypesResponse.Builder listRequestTypes(ListRequestTypesRequest request) {
		String whereClause = null;
		List<Object> filtersList = new ArrayList<>();

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
			whereClause = " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
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
			ValueManager.validateNull(nexPageToken)
		);

		queryRequestTypes
			// .setLimit(limit, offset)
			.getIDsAsList()
			.parallelStream()
			// .list(MRequestType.class)
			.forEach(requestTypeId -> {
				RequestType.Builder builder = IssueManagementConvertUtil.convertRequestType(requestTypeId);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListSalesRepresentativesResponse.Builder listSalesRepresentatives(ListSalesRepresentativesRequest request) {
		String whereClause = "EXISTS("
			+ "SELECT * FROM C_BPartner bp WHERE "
			+ "AD_User.C_BPartner_ID=bp.C_BPartner_ID "
			+ "AND (bp.IsEmployee='Y' OR bp.IsSalesRep='Y'))"
		;
		List<Object> filtersList = new ArrayList<>();

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
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
			ValueManager.validateNull(nexPageToken)
		);

		querySaleRepresentatives
			// .setLimit(limit, offset)
			.getIDsAsList()
			.parallelStream()
			.forEach(userId -> {
				User.Builder builder = IssueManagementConvertUtil.convertUser(userId);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListPrioritiesResponse.Builder listPriorities(ListPrioritiesRequest request) {
		String whereClause = "AD_Reference_ID = ?";

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(X_R_Request.PRIORITY_AD_Reference_ID);

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
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
			ValueManager.validateNull(nexPageToken)
		);

		queryPriority
			// .setLimit(limit, offset)
			.list(MRefList.class)
			.parallelStream()
			.forEach(priority -> {
				Priority.Builder builder = IssueManagementConvertUtil.convertPriority(priority);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListStatusCategoriesResponse.Builder listStatusCategories(ListStatusCategoriesRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		//		For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
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
			ValueManager.validateNull(nexPageToken)
		);

		queryRequests
			// .setLimit(limit, offset)
			.getIDsAsList()
			.parallelStream()
			.forEach(statusCategoryId -> {
				StatusCategory.Builder builder = IssueManagementConvertUtil.convertStatusCategory(
					statusCategoryId
				);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListStatusesResponse.Builder listStatuses(ListStatusesRequest request) {
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

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			filtersList.add(searchValue);
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
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
			.setOrderBy(I_R_Status.COLUMNNAME_IsDefault + " DESC, " + I_R_Status.COLUMNNAME_SeqNo)
			// .setLimit(limit, offset)
			.getIDsAsList()
			// .list(MStatus.class)
			.forEach(statusId -> {
				org.spin.backend.grpc.issue_management.Status.Builder builder = IssueManagementConvertUtil.convertStatus(statusId);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListCategoriesResponse.Builder listCategories(ListCategoriesRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		//		For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
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

		ListCategoriesResponse.Builder builderList = ListCategoriesResponse.newBuilder();
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
			// .setLimit(limit, offset)
			.getIDsAsList()
			.parallelStream()
			.forEach(categoryId -> {
				Category.Builder builder = IssueManagementConvertUtil.convertCategory(
					categoryId
				);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListGroupsResponse.Builder listGroups(ListGroupsRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		//		For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
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

		ListGroupsResponse.Builder builderList = ListGroupsResponse.newBuilder();
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
			// .setLimit(limit, offset)
			.getIDsAsList()
			.parallelStream()
			.forEach(groupId -> {
				Group.Builder builder = IssueManagementConvertUtil.convertGroup(
					groupId
				);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListTaskStatusesResponse.Builder listTaskStatuses(ListTaskStatusesRequest request) {
		String whereClause = "AD_Reference_ID = ?";

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(X_R_Request.TASKSTATUS_AD_Reference_ID);

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
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

		ListTaskStatusesResponse.Builder builderList = ListTaskStatusesResponse.newBuilder();
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
			// .setLimit(limit, offset)
			.list(MRefList.class)
			.parallelStream()
			.forEach(priority -> {
				TaskStatus.Builder builder = IssueManagementConvertUtil.convertTaskStatus(priority);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListBusinessPartnersResponse.Builder listBusinessPartners(ListBusinessPartnersRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		//		For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
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
			I_C_BPartner.Table_Name,
			whereClause.toString(),
			null
		)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO) // TODO: Fix Record access with pagination
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
		;

		int recordCount = queryRequests.count();

		ListBusinessPartnersResponse.Builder builderList = ListBusinessPartnersResponse.newBuilder();
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
			// .setLimit(limit, offset)
			.getIDsAsList()
			.parallelStream()
			.forEach(businessPartnerId -> {
				BusinessPartner.Builder builder = IssueManagementConvertUtil.convertBusinessPartner(
					businessPartnerId
				);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ListProjectsResponse.Builder listProjects(ListProjectsRequest request) {
		List<Object> filtersList = new ArrayList<>();
		StringBuffer whereClause = new StringBuffer();

		//		For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
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

		ListProjectsResponse.Builder builderList = ListProjectsResponse.newBuilder();
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
			.parallelStream()
			.forEach(projectId -> {
				Project.Builder builder = IssueManagementConvertUtil.convertProject(
					projectId
				);
				builderList.addRecords(builder);
			});

		return builderList;
	}



	public static ExistsIssuesResponse.Builder existsIssues(ExistsIssuesRequest request) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			request.getTableName()
		);

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



	public static ListIssuesReponse.Builder listMyIssues(ListIssuesRequest request) {
		List<Object> parametersList = new ArrayList<>();
		String whereClause = "";

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

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += " AND (UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Subject) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Summary) LIKE '%' || UPPER(?) || '%' )"
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
				+ "AND R_Request.R_StatusCategory_ID = sc.R_StatusCategory_ID"
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

		// Created Date
		Timestamp createdFrom = ValueManager.getDateFromTimestampDate(request.getCreatedFrom());
		Timestamp createdTo = ValueManager.getDateFromTimestampDate(request.getCreatedTo());
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
		Timestamp dateNextActionFrom = ValueManager.getDateFromTimestampDate(request.getDateNextActionFrom());
		Timestamp dateNextActionTo = ValueManager.getDateFromTimestampDate(request.getDateNextActionFrom());
		if (dateNextActionFrom != null || dateNextActionTo != null) {
			whereClause += " AND ";
			if (dateNextActionFrom != null && dateNextActionTo != null) {
				whereClause += "TRUNC(DateNextAction, 'DD') BETWEEN ? AND ? ";
				parametersList.add(dateNextActionFrom);
				parametersList.add(dateNextActionTo);
			}
			else if (dateNextActionFrom != null) {
				whereClause += "TRUNC(DateNextAction, 'DD') >= ? ";
				parametersList.add(dateNextActionFrom);
			}
			else {
				// DateTo != null
				whereClause += "TRUNC(DateNextAction, 'DD') <= ? ";
				parametersList.add(dateNextActionTo);
			}
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



	public static ListIssuesReponse.Builder listIssues(ListIssuesRequest request) {
		List<Object> parametersList = new ArrayList<>();
		String whereClause = "";

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if (!Util.isEmpty(searchValue, true)) {
			whereClause += " AND (UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Subject) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Summary) LIKE '%' || UPPER(?) || '%' )"
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
				+ "AND R_Request.R_StatusCategory_ID = sc.R_StatusCategory_ID"
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

}
