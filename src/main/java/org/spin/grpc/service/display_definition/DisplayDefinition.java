/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt EdwinBetanc0urt@outlook.com                     *
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
package org.spin.grpc.service.display_definition;

import org.compiere.util.CLogger;
import org.spin.backend.grpc.display_definition.DisplayDefinitionGrpc.DisplayDefinitionImplBase;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataRequest;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataResponse;
import org.spin.backend.grpc.display_definition.ListCalendarsRequest;
import org.spin.backend.grpc.display_definition.ListCalendarsResponse;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataRequest;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowDefinitionRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowDefinitionResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowsRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowsResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class DisplayDefinition extends DisplayDefinitionImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(DisplayDefinition.class);



	@Override
	public void existsDisplayDefinitionsMetadata(ExistsDisplayDefinitionMetadataRequest request, StreamObserver<ExistsDisplayDefinitionMetadataResponse> responseObserver) {
		try {
			ExistsDisplayDefinitionMetadataResponse.Builder builder = DisplayDefinitionServiceLogic.existsDisplayDefinitionsMetadata(request);
			responseObserver.onNext(builder.build());
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
	public void listDisplayDefinitionsMetadata(ListDisplayDefinitionsMetadataRequest request, StreamObserver<ListDisplayDefinitionsMetadataResponse> responseObserver) {
		try {
			ListDisplayDefinitionsMetadataResponse.Builder builder = DisplayDefinitionServiceLogic.listDisplayDefinitionsMetadata(request);
			responseObserver.onNext(builder.build());
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
	public void listWorkflowDefinition(ListWorkflowDefinitionRequest request, StreamObserver<ListWorkflowDefinitionResponse> responseObserver) {
		try {
			ListWorkflowDefinitionResponse.Builder builder = DisplayDefinitionServiceLogic.listWorkflowDefinition(request);
			responseObserver.onNext(builder.build());
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
	public void listWorkflows(ListWorkflowsRequest request, StreamObserver<ListWorkflowsResponse> responseObserver) {
		try {
			ListWorkflowsResponse.Builder builder = DisplayDefinitionServiceLogic.listWorkflows(request);
			responseObserver.onNext(builder.build());
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
	public void listCalendars(ListCalendarsRequest request, StreamObserver<ListCalendarsResponse> responseObserver) {
		try {
			ListCalendarsResponse.Builder builder = DisplayDefinitionServiceLogic.listCalendars(request);
			responseObserver.onNext(builder.build());
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
