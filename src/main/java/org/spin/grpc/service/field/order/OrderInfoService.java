package org.spin.grpc.service.field.order;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.order.GetOrderInfoRequest;
import org.spin.backend.grpc.field.order.ListBusinessPartnersOrderRequest;
import org.spin.backend.grpc.field.order.ListOrdersInfoRequest;
import org.spin.backend.grpc.field.order.ListOrdersInfoResponse;
import org.spin.backend.grpc.field.order.OrderInfo;
import org.spin.backend.grpc.field.order.OrderInfoServiceGrpc.OrderInfoServiceImplBase;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class OrderInfoService extends OrderInfoServiceImplBase {
	private CLogger log = CLogger.getCLogger(OrderInfo.class);

	@Override
	public void listOrderInfo(ListOrdersInfoRequest request, StreamObserver<ListOrdersInfoResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListOrdersInfoResponse.Builder entityValueList = OrderInfoLogic.listOrderInfo(request);
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
	public void getOrderInfo(GetOrderInfoRequest request, StreamObserver<OrderInfo> responseObserver) {
		try {
            if(request == null) {
                throw new AdempiereException("Object GetOrderInfoRequest Null");
            }
            OrderInfo.Builder entityValue = OrderInfoLogic.getOrder(request);
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


	/**
	 * @param request
	 * @param responseObserver
	 */
	@Override
	public void listBusinessPartners(ListBusinessPartnersOrderRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			// if(request == null) {
			// 	throw new AdempiereException("Object ListBusinessPartnersRequest Null");
			// }
			ListLookupItemsResponse.Builder entityValueList = OrderInfoLogic.listBusinessPartners(request);
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

