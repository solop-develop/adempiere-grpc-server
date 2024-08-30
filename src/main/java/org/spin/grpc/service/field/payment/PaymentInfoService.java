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
package org.spin.grpc.service.field.payment;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.payment.ListPaymentInfoRequest;
import org.spin.backend.grpc.field.payment.ListPaymentInfoResponse;
import org.spin.backend.grpc.field.payment.ListBankAccountRequest;
import org.spin.backend.grpc.field.payment.ListBusinessPartnersRequest;
import org.spin.backend.grpc.field.payment.PaymentInfo;
import org.spin.backend.grpc.field.payment.PaymentInfoServiceGrpc.PaymentInfoServiceImplBase;

import io.grpc.stub.StreamObserver;

import io.grpc.Status;

public class PaymentInfoService extends PaymentInfoServiceImplBase {
    private CLogger log = CLogger.getCLogger(PaymentInfo.class);

	@Override
	public void listPaymentInfo(ListPaymentInfoRequest request, StreamObserver<ListPaymentInfoResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListPaymentInfoResponse.Builder entityValueList = PaymentInfoLogic.listPaymentInfo(request);
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

	/**
	 * @param request
	 * @param responseObserver
	 */
	@Override
	public void listBusinessPartners(ListBusinessPartnersRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object ListBusinessPartnersRequest Null");
			}
			ListLookupItemsResponse.Builder entityValueList = PaymentInfoLogic.listBusinessPartners(request);
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

	/**
	 * @param request
	 * @param responseObserver
	 */
	@Override
	public void listBankAccount(ListBankAccountRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object ListBankAccountRequest Null");
			}
			ListLookupItemsResponse.Builder entityValueList = PaymentInfoLogic.listBankAccount(request);
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
}
