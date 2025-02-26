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
package org.spin.grpc.service.form.out_bound_order;

import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryRulesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryViasRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentActionsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentTypesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListLocatorsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListOrganizationsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListSalesRegionsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListSalesRepresentativesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListShippersRequest;
import org.spin.backend.grpc.form.out_bound_order.ListTargetDocumentTypesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListWarehousesRequest;
import org.spin.backend.grpc.form.out_bound_order.OutBoundOrderServiceGrpc.OutBoundOrderServiceImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class OutBoundOrder extends OutBoundOrderServiceImplBase {

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(OutBoundOrder.class);



	@Override
	public void listOrganizations(ListOrganizationsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = OutBoundOrderLogic.listOrganizations(request);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listWarehouses(ListWarehousesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = OutBoundOrderLogic.listWarehouses(request);
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listDocumentTypes(ListDocumentTypesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listTargetDocumentTypes(ListTargetDocumentTypesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listSalesRegions(ListSalesRegionsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listSalesRepresentatives(ListSalesRepresentativesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listDeliveryRules(ListDeliveryRulesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listDeliveryVias(ListDeliveryViasRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listShippers(ListShippersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listDocumentActions(ListDocumentActionsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
	public void listLocators(ListLocatorsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			ListLookupItemsResponse.Builder buildersList = ListLookupItemsResponse.newBuilder();
			responseObserver.onNext(
				buildersList.build()
			);
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
