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
package org.spin.grpc.service.field.field_management;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.field.ListZoomWindowsRequest;
import org.spin.backend.grpc.field.ListZoomWindowsResponse;
import org.spin.backend.grpc.common.ListEntitiesResponse;
import org.spin.backend.grpc.common.ListLookupItemsRequest;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.DefaultValue;
import org.spin.backend.grpc.field.FieldManagementServiceGrpc.FieldManagementServiceImplBase;
import org.spin.backend.grpc.field.GetDefaultValueRequest;
import org.spin.backend.grpc.field.GetZoomParentRecordRequest;
import org.spin.backend.grpc.field.GetZoomParentRecordResponse;
import org.spin.backend.grpc.field.ListGeneralSearchRecordsRequest;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class FieldManagementService extends FieldManagementServiceImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(FieldManagementService.class);


	@Override
	public void getDefaultValue(GetDefaultValueRequest request, StreamObserver<DefaultValue> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			DefaultValue.Builder defaultValue = FieldManagementLogic.getDefaultValue(request);
			responseObserver.onNext(defaultValue.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException())
			;
		}
	}


	@Override
	public void listLookupItems(ListLookupItemsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Lookup Request Null");
			}

			ListLookupItemsResponse.Builder entityValueList = FieldManagementLogic.listLookupItems(request);
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



	@Override
	public void listGeneralSearchRecords(ListGeneralSearchRecordsRequest request, StreamObserver<ListEntitiesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("List General Search Records Request Null");
			}
			ListEntitiesResponse.Builder entityValueList = FieldManagementLogic.listGeneralSearchRecords(
				request
			);
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


	@Override
	public void listZoomWindows(ListZoomWindowsRequest request, StreamObserver<ListZoomWindowsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object ListZoomWindowsRequest Null");
			}
			ListZoomWindowsResponse.Builder responseList = FieldManagementLogic.listZoomWindows(request);
			responseObserver.onNext(
				responseList.build()
			);
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


	@Override
	public void getZoomParentRecord(GetZoomParentRecordRequest request, StreamObserver<GetZoomParentRecordResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object GetZoomParentRecordRequest Null");
			}
			GetZoomParentRecordResponse.Builder responseList = FieldManagementLogic.getZoomParentRecord(request);
			responseObserver.onNext(
				responseList.build()
			);
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

}
