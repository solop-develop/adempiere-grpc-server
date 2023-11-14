/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.record_management.RecordManagementGrpc.RecordManagementImplBase;
import org.spin.backend.grpc.record_management.ExistsRecordReferencesRequest;
import org.spin.backend.grpc.record_management.ExistsRecordReferencesResponse;
import org.spin.backend.grpc.record_management.ListRecordReferencesRequest;
import org.spin.backend.grpc.record_management.ListRecordReferencesResponse;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordRequest;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordResponse;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordsBatchRequest;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordsBatchResponse;
import org.spin.grpc.logic.RecordManagementServiceLogic;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Record Management
 */
public class RecordManagement extends RecordManagementImplBase {
	private CLogger log = CLogger.getCLogger(RecordManagement.class);


	@Override
	public void toggleIsActiveRecord(ToggleIsActiveRecordRequest request, StreamObserver<ToggleIsActiveRecordResponse> responseObserver) {
		try {
			ToggleIsActiveRecordResponse.Builder builder = RecordManagementServiceLogic.toggleIsActiveRecord(request);
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
	
	@Override
	public void toggleIsActiveRecordsBatch(ToggleIsActiveRecordsBatchRequest request,
			StreamObserver<ToggleIsActiveRecordsBatchResponse> responseObserver) {
		try {
			ToggleIsActiveRecordsBatchResponse.Builder builder = RecordManagementServiceLogic.toggleIsActiveRecords(request);
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


	@Override
	public void existsRecordReferences(ExistsRecordReferencesRequest request, StreamObserver<ExistsRecordReferencesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Exists References Requested is Null");
			}
			log.fine("References Info Requested = " + request);
			ExistsRecordReferencesResponse.Builder entityValueList = RecordManagementServiceLogic.existsRecordReferences(
				request
			);
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


	// @Override
	public void listRecordReferences(ListRecordReferencesRequest request, StreamObserver<ListRecordReferencesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("List References Requested is Null");
			}
			log.fine("References Info Requested = " + request);
			ListRecordReferencesResponse.Builder entityValueList = RecordManagementServiceLogic.listRecordReferences(
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

}
