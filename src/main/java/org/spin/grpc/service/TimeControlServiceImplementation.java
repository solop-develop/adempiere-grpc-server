/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_S_ResourceType;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MResourceType;
import org.compiere.model.MRole;
import org.compiere.model.MUOM;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.time_control.ConfirmResourceAssignmentRequest;
import org.spin.backend.grpc.time_control.CreateResourceAssignmentRequest;
import org.spin.backend.grpc.time_control.DeleteResourceAssignmentRequest;
import org.spin.backend.grpc.time_control.ListResourcesAssignmentRequest;
import org.spin.backend.grpc.time_control.ListResourcesAssignmentResponse;
import org.spin.backend.grpc.time_control.Resource;
import org.spin.backend.grpc.time_control.ResourceAssignment;
import org.spin.backend.grpc.time_control.ResourceType;
import org.spin.backend.grpc.time_control.TimeControlGrpc.TimeControlImplBase;
import org.spin.backend.grpc.time_control.UpdateResourceAssignmentRequest;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for Time Control
 */
public class TimeControlServiceImplementation extends TimeControlImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(TimeControlServiceImplementation.class);

	/**
	 * Convert MResourceType to gRPC
	 * @param log
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
	public void createResourceAssignment(CreateResourceAssignmentRequest request, StreamObserver<ResourceAssignment> responseObserver) {
		try {
			if(request == null) {
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
				.asRuntimeException());
		}
	}
	
	private ResourceAssignment.Builder createResourceAssignment(CreateResourceAssignmentRequest request) {
	    int resourceTypeId = request.getResourceTypeId();
        if (resourceTypeId <= 0) {
            if (!Util.isEmpty(request.getResourceTypeUuid(), true)) {
                resourceTypeId = RecordUtil.getIdFromUuid(MResourceType.Table_Name, request.getResourceTypeUuid(), null);
            }
            if (resourceTypeId <= 0) {
                throw new AdempiereException("@FillMandatory@ @S_ResourceType_ID@");
            }
        }

		if (Util.isEmpty(request.getName(), true)) {
			throw new AdempiereException("@FillMandatory@ @Name@");
		}
		
		MResource resource = new Query(
            Env.getCtx(),
            MResource.Table_Name,
            " S_ResourceType_ID = ? ",
            null
        )
	        .setParameters(resourceTypeId)
	        .first();
		if (resource == null || resource.getS_Resource_ID() <= 0) {
            throw new AdempiereException("@S_Resource_ID@ @NotFound@");
		}
		
		MResourceAssignment resourceAssignment = new MResourceAssignment(Env.getCtx(), 0, null);
		resourceAssignment.setAD_Org_ID(Env.getAD_Org_ID(Env.getCtx()));
		resourceAssignment.setName(request.getName());
		resourceAssignment.setDescription(ValueUtil.validateNull(request.getDescription()));
		resourceAssignment.setAssignDateFrom(new Timestamp(System.currentTimeMillis()));
		resourceAssignment.setS_Resource_ID(resource.getS_Resource_ID());
		resourceAssignment.setQty(BigDecimal.ZERO); // overwrite constructor value
		resourceAssignment.saveEx();

		ResourceAssignment.Builder builder = convertResourceAssignment(resourceAssignment);
		return builder;
	}

	@Override
	public void listResourcesAssignment(ListResourcesAssignmentRequest request, StreamObserver<ListResourcesAssignmentResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListResourcesAssignmentResponse.Builder entitiesList = listResourcesAssignment(request);
			responseObserver.onNext(entitiesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}
	
	private ListResourcesAssignmentResponse.Builder listResourcesAssignment(ListResourcesAssignmentRequest request) {
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Object> parametersList = new ArrayList<>();
		String whereClause = " 1=1 ";
		// filter by resource type
		if (request.getResourceTypeId() > 0 || !Util.isEmpty(request.getResourceTypeUuid(), true)) {
			int resourceTypeId = request.getResourceTypeId();
			if (resourceTypeId <= 0) {
				resourceTypeId = RecordUtil.getIdFromUuid(I_S_ResourceType.Table_Name, request.getResourceTypeUuid(), null);
			}
			parametersList.add(resourceTypeId);
			whereClause += " AND NOT EXISTS("
				+ " SELECT 1 FROM S_Resource "
				+ " WHERE S_Resource.S_Resource_ID = S_ResourceAssignment.S_Resource_ID "
				+ " AND S_Resource.S_ResourceType_ID = ? "
				+ ") ";
		}
		// filter by is name
		if (!Util.isEmpty(request.getName(), true)) {
			parametersList.add(request.getName());
			whereClause += " AND UPPER(Name) LIKE '%' || UPPER(?) || '%' ";
		}
		// filter by is description
		if (!Util.isEmpty(request.getDescription(), true)) {
			parametersList.add(request.getDescription());
			whereClause += " AND UPPER(Description) LIKE '%' || UPPER(?) || '%' ";
		}
		// filter by is confirmed
		if (!Util.isEmpty(request.getConfirmed(), true)) {
			parametersList.add(request.getConfirmed());
			whereClause += " AND IsConfirmed = ? ";
		}
		// filter not in orders line
		if (request.getIsWaitingForOrdered()) {
			whereClause += " AND NOT EXISTS("
				+ " SELECT 1 FROM C_OrderLine "
				+ " WHERE C_OrderLine.S_ResourceAssignment_ID = S_ResourceAssignment.S_ResourceAssignment_ID "
				+ ") ";
		}
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

		Query query =  new Query(
			Env.getCtx(),
			MResourceAssignment.Table_Name,
			whereClause,
			null
		)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(parametersList)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOrderBy(MResourceAssignment.COLUMNNAME_Created);

		int count = query.count();

		List<MResourceAssignment> resourceAssignmentList = query.setLimit(limit, offset).list();
		
		ListResourcesAssignmentResponse.Builder builderList = ListResourcesAssignmentResponse.newBuilder();
		resourceAssignmentList.forEach(resourceAssignment -> {
		    ResourceAssignment.Builder resourceAssignmentBuilder = convertResourceAssignment(resourceAssignment);
		    builderList.addRecords(resourceAssignmentBuilder);
		});
		builderList.setRecordCount(count);
		//  Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		//  Set next page
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		
		return builderList;
	}

	@Override
	public void updateResourceAssignment(UpdateResourceAssignmentRequest request, StreamObserver<ResourceAssignment> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ResourceAssignment.Builder entity = updateResourcesAssignment(request);
			responseObserver.onNext(entity.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

    private ResourceAssignment.Builder updateResourcesAssignment(UpdateResourceAssignmentRequest request) {
        int resourceAssignmentId = request.getId();
        if (resourceAssignmentId <= 0) {
            if (!Util.isEmpty(request.getUuid(), true)) {
                resourceAssignmentId = RecordUtil.getIdFromUuid(MResourceAssignment.Table_Name, request.getUuid(), null);
            }
            if (resourceAssignmentId <= 0) {
                throw new AdempiereException("@FillMandatory@ @S_ResourceType_ID@");
            }
        }
        MResourceAssignment resourceAssignment = new MResourceAssignment(Env.getCtx(), resourceAssignmentId, null);
        if (resourceAssignment == null || resourceAssignment.getS_ResourceAssignment_ID() <= 0) {
            throw new AdempiereException("@ResourceNotAvailable@");
        }
        if (resourceAssignment.isConfirmed()) {
            throw new AdempiereException("@IsConfirmed@");
        }

        resourceAssignment.setName(ValueUtil.validateNull(request.getName()));
        resourceAssignment.setDescription(ValueUtil.validateNull(request.getDescription()));
        resourceAssignment.saveEx();

        ResourceAssignment.Builder builder = convertResourceAssignment(resourceAssignment);
        
        return builder;
    }

	@Override
	public void deleteResourceAssignment(DeleteResourceAssignmentRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Empty.Builder emptyBuilder = deleteResourceAssignment(request);
			responseObserver.onNext(emptyBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}
	
	private Empty.Builder deleteResourceAssignment(DeleteResourceAssignmentRequest request) {
        // Validate ID
        int recordId = request.getId();
        if (recordId <= 0) {
            String recordUuid = ValueUtil.validateNull(request.getUuid());
            recordId = RecordUtil.getIdFromUuid(MResourceAssignment.Table_Name, recordUuid, null);
            if (recordId <= 0) {
                throw new AdempiereException("@Record_ID@ @NotFound@");
            }
        }

		MResourceAssignment resourceAssignment = new MResourceAssignment(Env.getCtx(), recordId, null);
		if (resourceAssignment == null || resourceAssignment.getS_ResourceAssignment_ID() <= 0) {
            throw new AdempiereException("@ResourceNotAvailable@");
		}
		if (resourceAssignment.isConfirmed()) {
            throw new AdempiereException("@IsConfirmed@");
		}
		resourceAssignment.deleteEx(true);

		return Empty.newBuilder();
	}
	
	
	@Override
	public void confirmResourceAssignment(ConfirmResourceAssignmentRequest request, StreamObserver<ResourceAssignment> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ResourceAssignment.Builder resourceAssignmentBuilder = confirmResourceAssignment(request);
			responseObserver.onNext(resourceAssignmentBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException());
		}
	}

	private ResourceAssignment.Builder confirmResourceAssignment(ConfirmResourceAssignmentRequest request) {
		// Validate ID
		int resourceAssignmentId = request.getId();
		if (resourceAssignmentId <= 0) {
			String recordUuid = ValueUtil.validateNull(request.getUuid());
			resourceAssignmentId = RecordUtil.getIdFromUuid(MResourceAssignment.Table_Name, recordUuid, null);
			if (resourceAssignmentId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}
		}

		MResourceAssignment resourceAssignment = confirmResourceAssignment(request.getId(), request.getUuid());

		ResourceAssignment.Builder builder = convertResourceAssignment(resourceAssignment);
		
		return builder;
	}
	
	public static MResourceAssignment confirmResourceAssignment(int resourceAssignmentId, String resourceAssignmentUuid) {

		AtomicReference<MResourceAssignment> resourceAssignmentReference = new AtomicReference<MResourceAssignment>();

		Trx.run(transactionName -> {
			int recordId = resourceAssignmentId;
			// Validate ID
			if (recordId <= 0) {
				recordId = RecordUtil.getIdFromUuid(MResourceAssignment.Table_Name, resourceAssignmentUuid, transactionName);
			}
			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}

			MResourceAssignment resourceAssignment = new MResourceAssignment(Env.getCtx(), recordId, transactionName);
			if (resourceAssignment.getS_ResourceAssignment_ID() <= 0) {
				throw new AdempiereException("@S_ResourceAssignment_ID@ @NotFound@");
			}
			if (resourceAssignment.isConfirmed()) {
				throw new AdempiereException("@IsConfirmed@");
			}
	
			resourceAssignment.setIsConfirmed(true);
			resourceAssignment.setAssignDateTo(new Timestamp(System.currentTimeMillis()));
			
			long differenceTime = resourceAssignment.getAssignDateTo().getTime() - resourceAssignment.getAssignDateFrom().getTime();
			long minutesDiff = TimeUnit.MILLISECONDS.toMinutes(differenceTime);

			// get standard precision by unit of measure
			MResource resource = new MResource(Env.getCtx(), resourceAssignment.getS_Resource_ID(), transactionName);
			MResourceType resourceType = new MResourceType(Env.getCtx(), resource.getS_ResourceType_ID(), transactionName);
			MUOM uom = MUOM.get(Env.getCtx(), resourceType.getC_UOM_ID());
			int standardPrecision = uom.getStdPrecision();

			BigDecimal quantity = BigDecimal.valueOf(minutesDiff).setScale(standardPrecision);
			quantity = quantity.divide(BigDecimal.valueOf(60).setScale(standardPrecision), RoundingMode.UP);
			
			resourceAssignment.setQty(quantity);
			resourceAssignment.saveEx();
			
			resourceAssignmentReference.set(resourceAssignment);
		});

		return resourceAssignmentReference.get();
	}

}
