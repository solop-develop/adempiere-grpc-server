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
import org.spin.backend.grpc.display_definition.CreateDataEntryRequest;
import org.spin.backend.grpc.display_definition.DataEntry;
import org.spin.backend.grpc.display_definition.DeleteDataEntryRequest;
import org.spin.backend.grpc.display_definition.DisplayDefinitionGrpc.DisplayDefinitionImplBase;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataRequest;
import org.spin.backend.grpc.display_definition.ExistsDisplayDefinitionMetadataResponse;
import org.spin.backend.grpc.display_definition.ListCalendarsDataRequest;
import org.spin.backend.grpc.display_definition.ListCalendarsDataResponse;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionFieldsMetadataRequest;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionFieldsMetadataResponse;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataRequest;
import org.spin.backend.grpc.display_definition.ListDisplayDefinitionsMetadataResponse;
import org.spin.backend.grpc.display_definition.ListKanbansDataRequest;
import org.spin.backend.grpc.display_definition.ListKanbansDataResponse;
import org.spin.backend.grpc.display_definition.ListKanbansDefinitionRequest;
import org.spin.backend.grpc.display_definition.ListKanbansDefinitionResponse;
import org.spin.backend.grpc.display_definition.ListResourcesDataRequest;
import org.spin.backend.grpc.display_definition.ListResourcesDataResponse;
import org.spin.backend.grpc.display_definition.ListTimelinesDataRequest;
import org.spin.backend.grpc.display_definition.ListTimelinesDataResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowsDataRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowsDataResponse;
import org.spin.backend.grpc.display_definition.ListWorkflowsDefinitionRequest;
import org.spin.backend.grpc.display_definition.ListWorkflowsDefinitionResponse;
import org.spin.backend.grpc.display_definition.ReadDataEntryRequest;
import org.spin.backend.grpc.display_definition.UpdateDataEntryRequest;

import com.google.protobuf.Empty;

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
	public void listDisplayDefinitionFieldsMetadata(ListDisplayDefinitionFieldsMetadataRequest request, StreamObserver<ListDisplayDefinitionFieldsMetadataResponse> responseObserver) {
		try {
			ListDisplayDefinitionFieldsMetadataResponse.Builder builder = DisplayDefinitionServiceLogic.listDisplayDefinitionFieldsMetadata(request);
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
	public void listCalendarsData(ListCalendarsDataRequest request, StreamObserver<ListCalendarsDataResponse> responseObserver) {
		try {
			ListCalendarsDataResponse.Builder builder = DisplayDefinitionServiceLogic.listCalendarsData(request);
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
	public void listKanbansDefinition(ListKanbansDefinitionRequest request, StreamObserver<ListKanbansDefinitionResponse> responseObserver) {
		try {
			ListKanbansDefinitionResponse.Builder builder = DisplayDefinitionServiceLogic.listKanbansDefinition(request);
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
	public void listKanbansData(ListKanbansDataRequest request, StreamObserver<ListKanbansDataResponse> responseObserver) {
		try {
			ListKanbansDataResponse.Builder builder = DisplayDefinitionServiceLogic.listKanbansData(request);
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
	public void listResourcesData(ListResourcesDataRequest request, StreamObserver<ListResourcesDataResponse> responseObserver) {
		try {
			ListResourcesDataResponse.Builder builder = DisplayDefinitionServiceLogic.listResourcesData(request);
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
	public void listTimelinesData(ListTimelinesDataRequest request, StreamObserver<ListTimelinesDataResponse> responseObserver) {
		try {
			ListTimelinesDataResponse.Builder builder = DisplayDefinitionServiceLogic.listTimelinesData(request);
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
	public void listWorkflowsDefinition(ListWorkflowsDefinitionRequest request, StreamObserver<ListWorkflowsDefinitionResponse> responseObserver) {
		try {
			ListWorkflowsDefinitionResponse.Builder builder = DisplayDefinitionServiceLogic.listWorkflowsDefinition(request);
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
	public void listWorkflowsData(ListWorkflowsDataRequest request, StreamObserver<ListWorkflowsDataResponse> responseObserver) {
		try {
			ListWorkflowsDataResponse.Builder builder = DisplayDefinitionServiceLogic.listWorkflowsData(request);
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
	public void createDataEntry(CreateDataEntryRequest request, StreamObserver<DataEntry> responseObserver) {
		try {
			DataEntry.Builder builder = DisplayDefinitionServiceLogic.createDataEntry(request);
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
	public void readDataEntry(ReadDataEntryRequest request, StreamObserver<DataEntry> responseObserver) {
		try {
			DataEntry.Builder builder = DisplayDefinitionServiceLogic.readDataEntry(request);
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
	public void updateDataEntry(UpdateDataEntryRequest request, StreamObserver<DataEntry> responseObserver) {
		try {
			DataEntry.Builder builder = DisplayDefinitionServiceLogic.updateDataEntry(request);
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
	public void deleteDataEntry(DeleteDataEntryRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder builder = DisplayDefinitionServiceLogic.deleteDataEntry(request);
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
