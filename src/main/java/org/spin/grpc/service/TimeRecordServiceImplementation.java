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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.adempiere.core.domains.models.I_C_Project;
import org.adempiere.core.domains.models.I_R_Request;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProject;
import org.compiere.model.MRequest;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MResourceType;
import org.compiere.model.MUOM;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.time_record.CreateTimeRecordRequest;
import org.spin.backend.grpc.form.time_record.Issue;
import org.spin.backend.grpc.form.time_record.ListIssuesRequest;
import org.spin.backend.grpc.form.time_record.ListIssuesResponse;
import org.spin.backend.grpc.form.time_record.ListProjectsRequest;
import org.spin.backend.grpc.form.time_record.ListProjectsResponse;
import org.spin.backend.grpc.form.time_record.ListTimeRecordRequest;
import org.spin.backend.grpc.form.time_record.ListTimeRecordResponse;
import org.spin.backend.grpc.form.time_record.Project;
import org.spin.backend.grpc.form.time_record.Resource;
import org.spin.backend.grpc.form.time_record.ResourceAssignment;
import org.spin.backend.grpc.form.time_record.ResourceType;
import org.spin.backend.grpc.form.time_record.User;
import org.spin.backend.grpc.form.time_record.TimeRecordGrpc.TimeRecordImplBase;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Time Record
 */
public class TimeRecordServiceImplementation extends TimeRecordImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(TimeRecordServiceImplementation.class);

	/**
	 * Convert MRequest to gRPC
	 * @param resourceType
	 * @return
	 */
	public static Issue.Builder convertRequest(org.compiere.model.MRequest request) {
		Issue.Builder builder = Issue.newBuilder();
		if (request == null) {
			return builder;
		}
		builder.setId(request.getR_Request_ID());
		builder.setUuid(ValueUtil.validateNull(request.getUUID()));
		builder.setDocumentNo(ValueUtil.validateNull(request.getDocumentNo()));
		builder.setSubject(ValueUtil.validateNull(request.getSubject()));
		builder.setSummary(ValueUtil.validateNull(request.getSummary()));

		return builder;
	}

	@Override
	public void listIssues(ListIssuesRequest request, StreamObserver<ListIssuesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListIssuesResponse.Builder recordsList = listIssues(request);
			responseObserver.onNext(recordsList.build());
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

	ListIssuesResponse.Builder listIssues(ListIssuesRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		List<Object> parametersList = new ArrayList<>();
		int userId = Env.getAD_User_ID(context);
		parametersList.add(userId);

		int roleId = Env.getAD_Role_ID(context);
		parametersList.add(roleId);

		final String whereClause = "Processed='N' "
			+ "AND (SalesRep_ID=? OR AD_Role_ID = ?) "
			+ "AND (DateNextAction IS NULL OR TRUNC(DateNextAction, 'DD') <= TRUNC(SysDate, 'DD'))"
			+ "AND (R_Status_ID IS NULL OR R_Status_ID IN (SELECT R_Status_ID FROM R_Status WHERE IsClosed='N'))"
		;

		Query queryRequests = new Query(
			context,
			I_R_Request.Table_Name,
			whereClause,
			null
		)
			// .setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(parametersList)
			.setApplyAccessFilter(true)
			.setOrderBy(I_R_Request.COLUMNNAME_Created)
		;

		int recordCount = queryRequests.count();

		ListIssuesResponse.Builder builderList = ListIssuesResponse.newBuilder();
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



	/**
	 * Convert MProject to gRPC
	 * @param project
	 * @return
	 */
	public static Project.Builder convertProject(org.compiere.model.MProject project) {
		Project.Builder builder = Project.newBuilder();
		if (project == null) {
			return builder;
		}
		builder.setId(project.getC_Project_ID());
		builder.setUuid(ValueUtil.validateNull(project.getUUID()));
		builder.setValue(ValueUtil.validateNull(project.getValue()));
		builder.setName(ValueUtil.validateNull(project.getName()));

		return builder;
	}

	@Override
	public void listProjects(ListProjectsRequest request, StreamObserver<ListProjectsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListProjectsResponse.Builder recordsList = listProjects(request);
			responseObserver.onNext(recordsList.build());
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

	private ListProjectsResponse.Builder listProjects(ListProjectsRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());

		List<Object> parametersList = new ArrayList<>();
		parametersList.add(false); // N

		final String whereClause = "Processed = ?";

		Query queryRequests = new Query(
			context,
			I_C_Project.Table_Name,
			whereClause,
			null
		)
			.setParameters(parametersList)
			.setApplyAccessFilter(true)
			.setOnlyActiveRecords(true)
			.setOrderBy(I_C_Project.COLUMNNAME_Created)
		;

		int recordCount = queryRequests.count();

		ListProjectsResponse.Builder builderList = ListProjectsResponse.newBuilder();
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
			.list(MProject.class)
			.forEach(projectRecord -> {
				Project.Builder builder = convertProject(projectRecord);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	/**
	 * Convert MResourceType to gRPC
	 * @param resourceType
	 * @return
	 */
	public static ResourceType.Builder convertResourceType(org.compiere.model.MResourceType resourceType) {
		ResourceType.Builder builder = ResourceType.newBuilder();
		if (resourceType == null) {
			return builder;
		}
		builder.setId(resourceType.getS_ResourceType_ID());
		builder.setUuid(ValueUtil.validateNull(resourceType.getUUID()));
		builder.setValue(ValueUtil.validateNull(resourceType.getValue()));
		builder.setName(ValueUtil.validateNull(resourceType.getName()));
		builder.setDescription(ValueUtil.validateNull(resourceType.getDescription()));

		MUOM unitOfMeasure = MUOM.get(Env.getCtx(), resourceType.getC_UOM_ID());
		builder.setUnitOfMeasure(ConvertUtil.convertUnitOfMeasure(unitOfMeasure));

		return builder;
	}

	/**
	 * Convert MUser to gRPC
	 * @param user
	 * @return
	 */
	public static User.Builder convertUser(org.compiere.model.MUser user) {
		User.Builder builder = User.newBuilder();
		if (user == null || user.getAD_User_ID() <= 0) {
			return builder;
		}
		builder.setId(user.getAD_User_ID());
		builder.setUuid(ValueUtil.validateNull(user.getUUID()));
		builder.setValue(ValueUtil.validateNull(user.getValue()));
		builder.setName(ValueUtil.validateNull(user.getName()));
		builder.setDescription(ValueUtil.validateNull(user.getDescription()));

		return builder;
	}
	public static User.Builder convertUser(int userId) {
		User.Builder builder = User.newBuilder();
		if (userId <= 0) {
			return builder;
		}
		MUser user = MUser.get(Env.getCtx(), userId);

		return convertUser(user);
	}

	/**
	 * Convert MResource to gRPC
	 * @param log
	 * @return
	 */
	public static Resource.Builder convertResource(MResource resource) {
		Resource.Builder builder = Resource.newBuilder();
		if (resource == null) {
			return builder;
		}
		builder.setId(resource.getS_ResourceType_ID());
		builder.setUuid(ValueUtil.validateNull(resource.getUUID()));
		builder.setName(ValueUtil.validateNull(resource.getName()));

		MResourceType resourceType = MResourceType.get(Env.getCtx(), resource.getS_ResourceType_ID());
		ResourceType.Builder resourceTypeBuilder = convertResourceType(resourceType);
		builder.setResourceType(resourceTypeBuilder);
		if (resource.getAD_User_ID() > 0) {
			builder.setUser(
				convertUser(resource.getAD_User_ID())
			);
		}

		return builder;
	}

	public static ResourceAssignment.Builder convertResourceAssignment(int resourceAssignmentId) {
		ResourceAssignment.Builder builder = ResourceAssignment.newBuilder();
		if (resourceAssignmentId > 0) {
			MResourceAssignment resourceAssigment = new MResourceAssignment(Env.getCtx(), resourceAssignmentId, null);
			return convertResourceAssignment(resourceAssigment);
		}
		return builder;
	}

	/**
	 * Convert MResourceAssignment to gRPC
	 * @param log
	 * @return
	 */
	public static ResourceAssignment.Builder convertResourceAssignment(MResourceAssignment resourceAssignment) {
		ResourceAssignment.Builder builder = ResourceAssignment.newBuilder();
		if (resourceAssignment == null) {
			return builder;
		}
		builder.setId(resourceAssignment.getS_ResourceAssignment_ID());
		builder.setUuid(ValueUtil.validateNull(resourceAssignment.getUUID()));
		builder.setName(ValueUtil.validateNull(resourceAssignment.getName()));
		builder.setDescription(ValueUtil.validateNull(resourceAssignment.getDescription()));
		if (resourceAssignment.getAssignDateFrom() != null) {
			builder.setAssignDateFrom(resourceAssignment.getAssignDateFrom().getTime());
		}
		if (resourceAssignment.getAssignDateTo() != null) {
			builder.setAssignDateTo(resourceAssignment.getAssignDateTo().getTime());
		}
		builder.setIsConfirmed(resourceAssignment.isConfirmed());
		builder.setQuantity(
			ValueUtil.getDecimalFromBigDecimal(
				resourceAssignment.getQty()
			)
		);

		MResource resourceType = MResource.get(Env.getCtx(), resourceAssignment.getS_Resource_ID());
		Resource.Builder resourceTypeBuilder = convertResource(resourceType);
		builder.setResource(resourceTypeBuilder);

		return builder;
	}

	public static ResourceAssignment.Builder convertResourceAssignmentByResource(int resourceId) {
		ResourceAssignment.Builder builder = ResourceAssignment.newBuilder();
		MResource resource = new MResource(Env.getCtx(), resourceId, null);

		if (resource == null || resource.getS_Resource_ID() <= 0) {
			return builder;
		}
		MResourceAssignment resourceAssignment = new Query(
			Env.getCtx(),
				MResourceAssignment.Table_Name,
				" S_Resource_ID = ? ",
				null
			)
				.setParameters(resource.getS_Resource_ID())
				.first();

		return convertResourceAssignment(resourceAssignment);
	}

	@Override
	public void createTimeRecord(CreateTimeRecordRequest request, StreamObserver<ResourceAssignment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ResourceAssignment.Builder entity = createResourceAssignment(request);
			responseObserver.onNext(entity.build());
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

	private ResourceAssignment.Builder createResourceAssignment(CreateTimeRecordRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());
		int orgId = Env.getAD_Org_ID(context);
		if (orgId <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		}

		if (Util.isEmpty(request.getName(), true)) {
			throw new AdempiereException("@FillMandatory@ @Name@");
		}
		BigDecimal quantity = ValueUtil.getBigDecimalFromDecimal(request.getQuantity());
		if (quantity == null) {
			throw new AdempiereException("@FillMandatory@ @Qty@");
		}

		int userId = Env.getAD_User_ID(context);
		MResource resource = new Query(
			Env.getCtx(),
			MResource.Table_Name,
			" AD_User_ID = ? ",
			null
		)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(userId)
			.first()
		;
		if (resource == null || resource.getS_Resource_ID() <= 0) {
			throw new AdempiereException("@S_Resource_ID@ @NotFound@");
		}

		MResourceAssignment resourceAssignment = new MResourceAssignment(context, 0, null);
		resourceAssignment.setS_Resource_ID(resource.getS_Resource_ID());
		resourceAssignment.getAD_Org_ID();
		resourceAssignment.setName(request.getName());
		resourceAssignment.setQty(quantity);
		resourceAssignment.setDescription(ValueUtil.validateNull(request.getDescription()));

		// set date
		Timestamp dateFrom = new Timestamp(System.currentTimeMillis());
		if (!Objects.isNull(request.getDate()) && request.getDate() > 0) {
			dateFrom = new Timestamp(request.getDate());
		}
		resourceAssignment.setAssignDateFrom(dateFrom);

		// set project
		if (request.getProjectId() > 0 || !Util.isEmpty(request.getProjectUuid(), true)) {
			int projectId = request.getProjectId();
			if (projectId <= 0) {
				projectId = RecordUtil.getIdFromUuid(I_C_Project.Table_Name, request.getProjectUuid(), null);
			}
			// checks if the column exists in the database
			if (resourceAssignment.get_ColumnIndex(I_C_Project.COLUMNNAME_C_Project_ID) >= 0) {
				resourceAssignment.set_ValueOfColumn(
					I_C_Project.COLUMNNAME_C_Project_ID,
					projectId
				);
			}
		}

		// set request
		if (request.getRequestId() > 0 || !Util.isEmpty(request.getRequestUuid(), true)) {
			int requestId = request.getProjectId();
			if (requestId <= 0) {
				requestId = RecordUtil.getIdFromUuid(I_R_Request.Table_Name, request.getRequestUuid(), null);
			}
			// checks if the column exists in the database
			if (resourceAssignment.get_ColumnIndex(I_R_Request.COLUMNNAME_R_Request_ID) >= 0) {
				resourceAssignment.set_ValueOfColumn(
					I_R_Request.COLUMNNAME_R_Request_ID,
					requestId
				);
			}
		}

		resourceAssignment.saveEx();

		ResourceAssignment.Builder builder = convertResourceAssignment(resourceAssignment);
		return builder;
	}

	@Override
	public void listTimeRecord(ListTimeRecordRequest request, StreamObserver<ListTimeRecordResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListTimeRecordResponse.Builder entitiesList = listTimeRecord(request);
			responseObserver.onNext(entitiesList.build());
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

	private ListTimeRecordResponse.Builder listTimeRecord(ListTimeRecordRequest request) {
		Properties context = ContextManager.getContext(request.getClientRequest());
		int userId = Env.getAD_User_ID(context);

		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(request.getClientRequest().getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Object> parametersList = new ArrayList<>();
		// filter by user type
		parametersList.add(userId);
		String whereClause = " EXISTS("
			+ " SELECT 1 FROM S_Resource "
			+ " WHERE S_Resource.S_Resource_ID = S_ResourceAssignment.S_Resource_ID "
			+ " AND S_Resource.AD_User_ID = ? "
			+ ") "
		;

		if (!Objects.isNull(request.getDateFrom()) && request.getDateFrom() > 0) {
			Timestamp dateFrom = new Timestamp(request.getDateFrom());
			parametersList.add(dateFrom);
			whereClause += " AND AssignDateFrom = ? ";
		}
		if (!Objects.isNull(request.getDateTo()) && request.getDateTo() > 0) {
			Timestamp dateTo = new Timestamp(request.getDateTo());
			parametersList.add(dateTo);
			whereClause += " AND AssignDateTo = ? ";
		}

		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause += " AND ("
				+ "UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%' "
			+ ")";
			parametersList.add(request.getSearchValue());
			parametersList.add(request.getSearchValue());
		}

		Query query = new Query(
			context,
			MResourceAssignment.Table_Name,
			whereClause,
			null
		)
			.setApplyAccessFilter(true)
			.setParameters(parametersList)
			.setOnlyActiveRecords(true)
			.setOrderBy(MResourceAssignment.COLUMNNAME_Created)
		;

		int count = query.count();

		List<MResourceAssignment> resourceAssignmentList = query.setLimit(limit, offset).list();

		ListTimeRecordResponse.Builder builderList = ListTimeRecordResponse.newBuilder();
		resourceAssignmentList.forEach(resourceAssignment -> {
			ResourceAssignment.Builder resourceAssignmentBuilder = convertResourceAssignment(resourceAssignment);
			builderList.addRecords(resourceAssignmentBuilder);
		});
		builderList.setRecordCount(count);
		// Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(request.getClientRequest().getSessionUuid()) + (pageNumber + 1);
		}
		// Set next page
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		return builderList;
	}

}
