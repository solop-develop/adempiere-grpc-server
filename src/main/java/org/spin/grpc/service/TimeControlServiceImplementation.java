/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
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

import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MResourceType;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.glassfish.grizzly.http.server.SessionManager;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.time_control.CreateResourceAssignmentRequest;
import org.spin.backend.grpc.time_control.DeleteResourceAssignmentRequest;
import org.spin.backend.grpc.time_control.ListResourcesAssigmentRequest;
import org.spin.backend.grpc.time_control.ListResourcesAssigmentResponse;
import org.spin.backend.grpc.time_control.Resource;
import org.spin.backend.grpc.time_control.ResourceAssignment;
import org.spin.backend.grpc.time_control.ResourceType;
import org.spin.backend.grpc.time_control.TimeControlGrpc.TimeControlImplBase;
import org.spin.base.util.ContextManager;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.backend.grpc.time_control.UpdateResourceAssignmentRequest;

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

	/**
	 * Convert MResourceAssignment to gRPC
	 * @param log
	 * @return
	 */
	public static ResourceAssignment.Builder convertResourceAssigment(MResourceAssignment resourceAssigment) {
		ResourceAssignment.Builder builder = ResourceAssignment.newBuilder();
		if (resourceAssigment == null) {
			return builder;
		}
		builder.setId(resourceAssigment.getS_ResourceAssignment_ID());
		builder.setUuid(ValueUtil.validateNull(resourceAssigment.getUUID()));
		builder.setName(ValueUtil.validateNull(resourceAssigment.getName()));
		if (resourceAssigment.getAssignDateFrom() != null) {
		    builder.setAssignDateFrom(resourceAssigment.getAssignDateFrom().getTime());
		}
		if (resourceAssigment.getAssignDateTo() != null) {
		    builder.setAssignDateTo(resourceAssigment.getAssignDateTo().getTime());
		}
		builder.setIsConfirmed(resourceAssigment.isConfirmed());

		MResource resourceType = MResource.get(Env.getCtx(), resourceAssigment.getS_Resource_ID());
		Resource.Builder resourceTypeBuilder = convertResource(resourceType);
		builder.setResource(resourceTypeBuilder);
		
		return builder;
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
	    int resourceTypeId = request.getTypeId();
        if (resourceTypeId <= 0) {
            if (!Util.isEmpty(request.getTypeUuid(), true)) {
                resourceTypeId = RecordUtil.getIdFromUuid(MResourceType.Table_Name, request.getTypeUuid(), null);
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
		
		Properties context = ContextManager.getContext(request.getClientRequest());
		
		MResourceAssignment resourceAssigment = new MResourceAssignment(context, 0, null);
        resourceAssigment.setAD_Org_ID(Env.getAD_Org_ID(context));
		resourceAssigment.setName(request.getName());
		resourceAssigment.setDescription(ValueUtil.validateNull(request.getDescription()));
		resourceAssigment.setAssignDateFrom(new Timestamp(System.currentTimeMillis()));
		resourceAssigment.setS_Resource_ID(resource.getS_Resource_ID());
		resourceAssigment.saveEx();

		ResourceAssignment.Builder builder = convertResourceAssigment(resourceAssigment);
		return builder;
	}

	@Override
	public void listResourcesAssigment(ListResourcesAssigmentRequest request, StreamObserver<ListResourcesAssigmentResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListResourcesAssigmentResponse.Builder entitiesList = listResourcesAssigment(request);
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
	
	private ListResourcesAssigmentResponse.Builder listResourcesAssigment(ListResourcesAssigmentRequest request) {
		List<MResourceAssignment> resourceAssigmentList = new Query(
			Env.getCtx(),
			MResourceAssignment.Table_Name,
			null,
			null
		).list();
		
		ListResourcesAssigmentResponse.Builder builderList = ListResourcesAssigmentResponse.newBuilder();
		resourceAssigmentList.forEach(resourceAssigment -> {
		    ResourceAssignment.Builder resourceAssigmentBuilder = convertResourceAssigment(resourceAssigment);
		    builderList.addRecords(resourceAssigmentBuilder);
		});
		builderList.setRecordCount(resourceAssigmentList.size());
		
		return builderList;
	}

	@Override
	public void updateResourceAssignment(UpdateResourceAssignmentRequest request, StreamObserver<ResourceAssignment> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ResourceAssignment.Builder entity = updateResourcesAssigment(request);
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

    private ResourceAssignment.Builder updateResourcesAssigment(UpdateResourceAssignmentRequest request) {
        int resourceAssigmentId = request.getId();
        if (resourceAssigmentId <= 0) {
            if (!Util.isEmpty(request.getUuid(), true)) {
                resourceAssigmentId = RecordUtil.getIdFromUuid(MResourceAssignment.Table_Name, request.getUuid(), null);
            }
            if (resourceAssigmentId <= 0) {
                throw new AdempiereException("@FillMandatory@ @S_ResourceType_ID@");
            }
        }
        MResourceAssignment resourceAssigment = new MResourceAssignment(Env.getCtx(), resourceAssigmentId, null);
        if (resourceAssigment.getS_ResourceAssignment_ID() <= 0) {
            throw new AdempiereException("@S_ResourceAssignment_ID@ @NotFound@");
        }
        resourceAssigment.setName(ValueUtil.validateNull(request.getName()));
        resourceAssigment.setDescription(ValueUtil.validateNull(request.getDescription()));
        resourceAssigment.saveEx();

        ResourceAssignment.Builder builder = convertResourceAssigment(resourceAssigment);
        
        return builder;
    }

	@Override
	public void deleteResourceAssignment(DeleteResourceAssignmentRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Empty.Builder entity = deleteResourceAssigment(request);
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
	
	private Empty.Builder deleteResourceAssigment(DeleteResourceAssignmentRequest request) {
        // Validate ID
        int recordId = request.getId();
        if (recordId <= 0) {
            String recordUuid = ValueUtil.validateNull(request.getUuid());
            recordId = RecordUtil.getIdFromUuid(MResourceAssignment.Table_Name, recordUuid, null);
            if (recordId <= 0) {
                throw new AdempiereException("@Record_ID@ @NotFound@");
            }
        }

		MResourceAssignment resourceAssigment = new MResourceAssignment(Env.getCtx(), recordId, null);
		if (resourceAssigment.isConfirmed()) {
            throw new AdempiereException("@IsConfirmed@");
		}
		resourceAssigment.deleteEx(true);

		return Empty.newBuilder();
	}
	
}
