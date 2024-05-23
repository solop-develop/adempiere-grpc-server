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
