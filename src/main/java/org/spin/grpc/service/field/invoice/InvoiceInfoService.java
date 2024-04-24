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
package org.spin.grpc.service.field.invoice;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.invoice.GetInvoiceInfoRequest;
import org.spin.backend.grpc.field.invoice.InvoiceInfo;
import org.spin.backend.grpc.field.invoice.InvoiceInfoServiceGrpc.InvoiceInfoServiceImplBase;
import org.spin.backend.grpc.field.invoice.ListBusinessPartnersRequest;
import org.spin.backend.grpc.field.invoice.ListInvoicesInfoRequest;
import org.spin.backend.grpc.field.invoice.ListInvoicesInfoResponse;
import org.spin.backend.grpc.field.invoice.ListOrdersRequest;
import org.spin.backend.grpc.field.invoice.ListInvoicePaySchedulesRequest;
import org.spin.backend.grpc.field.invoice.ListInvoicePaySchedulesResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class InvoiceInfoService extends InvoiceInfoServiceImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(InvoiceInfo.class);


	@Override
	public void listOrders(ListOrdersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object ListOrdersRequest Null");
			}
			ListLookupItemsResponse.Builder entityValueList = InvoiceInfoLogic.listOrders(request);
			responseObserver.onNext(
				entityValueList.build()
			);
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


	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object ListBusinessPartnersRequest Null");
			}
			ListLookupItemsResponse.Builder entityValueList = InvoiceInfoLogic.listBusinessPartners(request);
			responseObserver.onNext(
				entityValueList.build()
			);
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


	@Override
	public void listInvoiceInfo(ListInvoicesInfoRequest request, StreamObserver<ListInvoicesInfoResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListInvoicesInfoResponse.Builder entityValueList = InvoiceInfoLogic.listInvoiceInfo(request);
			responseObserver.onNext(
				entityValueList.build()
			);
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


	@Override
	public void getInvoiceInfo(GetInvoiceInfoRequest request, StreamObserver<InvoiceInfo> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object GetInvoiceInfoRequest Null");
			}
			InvoiceInfo.Builder entityValue = InvoiceInfoLogic.getInvoice(request);
			responseObserver.onNext(
				entityValue.build()
			);
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


	@Override
	public void listInvoicePaySchedules(ListInvoicePaySchedulesRequest request, StreamObserver<ListInvoicePaySchedulesResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object ListInvoicePaySchedulesRequest Null");
			}
			ListInvoicePaySchedulesResponse.Builder entityValuesList = InvoiceInfoLogic.listInvoicePaySchedules(request);
			responseObserver.onNext(
				entityValuesList.build()
			);
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

}
