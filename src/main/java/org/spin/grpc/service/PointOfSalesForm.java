/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
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
package org.spin.grpc.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_PrintFormatItem;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_C_BP_BankAccount;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_ConversionType;
import org.adempiere.core.domains.models.I_C_Currency;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_C_POS;
import org.adempiere.core.domains.models.I_C_Payment;
import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.core.domains.models.I_M_InOutLine;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_Storage;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.GenericPO;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MColumn;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocation;
import org.compiere.model.MMenu;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MStorage;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.M_Element;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.common.ReportOutput;
import org.spin.backend.grpc.core_functionality.Currency;
import org.spin.backend.grpc.core_functionality.ProductPrice;
import org.spin.backend.grpc.pos.*;
import org.spin.backend.grpc.pos.StoreGrpc.StoreImplBase;
import org.spin.backend.grpc.report_management.GenerateReportRequest;
import org.spin.base.util.ConvertUtil;
import org.spin.base.util.DocumentUtil;
import org.spin.base.util.FileUtil;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.pos.service.POSLogic;
import org.spin.pos.service.bank.BankManagement;
import org.spin.pos.service.cash.CashManagement;
import org.spin.pos.service.cash.CashServiceLogic;
import org.spin.pos.service.cash.CashUtil;
import org.spin.pos.service.cash.CollectingManagement;
import org.spin.pos.service.order.DiscountManagement;
import org.spin.pos.service.order.OrderManagement;
import org.spin.pos.service.order.OrderServiceLogic;
import org.spin.pos.service.order.OrderUtil;
import org.spin.pos.service.order.RMAUtil;
import org.spin.pos.service.order.ReturnSalesOrder;
import org.spin.pos.service.order.ShipmentUtil;
import org.spin.pos.service.payment.GiftCardManagement;
import org.spin.pos.service.payment.PaymentConvertUtil;
import org.spin.pos.service.payment.PaymentServiceLogic;
import org.spin.pos.service.pos.POS;
import org.spin.pos.service.product.ProductServiceLogic;
import org.spin.pos.service.pos.AccessManagement;
import org.spin.pos.service.seller.SellerServiceLogic;
import org.spin.pos.util.*;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.store.util.VueStoreFrontUtil;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * Service for backend of POS
 */
public class PointOfSalesForm extends StoreImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(PointOfSalesForm.class);


	@Override
	public void createOrder(CreateOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = createOrder(request);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void getOrder(GetOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderConverUtil.convertOrder(
				OrderUtil.validateAndGetOrder(
					request.getId(),
					null
				)
			);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listOrders(ListOrdersRequest request, StreamObserver<ListOrdersResponse> responseObserver) {
		try {
			ListOrdersResponse.Builder ordersList = listOrders(request);
			responseObserver.onNext(ordersList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void updateOrder(UpdateOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderConverUtil.convertOrder(
				OrderServiceLogic.updateOrder(request)
			);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void updateManualOrder(UpdateManualOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderServiceLogic.updateManualOrder(request);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void deleteOrder(DeleteOrderRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder order = OrderServiceLogic.deleteOrder(request);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void releaseOrder(ReleaseOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderConverUtil.convertOrder(
				changeOrderAssigned(
					request.getId()
				)
			);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void holdOrder(HoldOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderConverUtil.convertOrder(
				changeOrderAssigned(
					request.getId(),
					request.getSalesRepresentativeId()
				)
			);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void processOrder(ProcessOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderConverUtil.convertOrder(
				processOrder(request)
			);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void createOrderLine(CreateOrderLineRequest request, StreamObserver<OrderLine> responseObserver) {
		try {
			OrderLine.Builder orderLine = createAndConvertOrderLine(request);
			responseObserver.onNext(orderLine.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listOrderLines(ListOrderLinesRequest request, StreamObserver<ListOrderLinesResponse> responseObserver) {
		try {
			ListOrderLinesResponse.Builder orderLinesList = listOrderLines(request);
			responseObserver.onNext(orderLinesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void updateOrderLine(UpdateOrderLineRequest request, StreamObserver<OrderLine> responseObserver) {
		try {
			OrderLine.Builder orderLine = updateAndConvertOrderLine(request);
			responseObserver.onNext(orderLine.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void deleteOrderLine(DeleteOrderLineRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder orderLine = OrderServiceLogic.deleteOrderLine(request);
			responseObserver.onNext(orderLine.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void createGiftCard(CreateGiftCardRequest request, StreamObserver<GiftCard> responseObserver) {
		try {
			GiftCard.Builder giftCard = POSLogic.createGiftCard(request);
			responseObserver.onNext(giftCard.build());
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
	public void getGiftCard(GetGiftCardRequest request, StreamObserver<GiftCard> responseObserver) {
		try {
			GiftCard.Builder giftCard = POSLogic.getGiftCard(request.getId(), null);
			responseObserver.onNext(giftCard.build());
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
	public void getValidGiftCard(GetValidGiftCardRequest request, StreamObserver<GiftCard> responseObserver) {
		try {
			GiftCard.Builder giftCard = POSLogic.getValidGiftCard(request);
			responseObserver.onNext(giftCard.build());
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
	public void listGiftCards(ListGiftCardsRequest request, StreamObserver<ListGiftCardsResponse> responseObserver) {
		try {
			ListGiftCardsResponse.Builder listGiftCards = POSLogic.listGiftCards(request);
			responseObserver.onNext(listGiftCards.build());
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
	public void updateGiftCard(UpdateGiftCardRequest request, StreamObserver<GiftCard> responseObserver) {
		try {
			GiftCard.Builder giftCard = POSLogic.updateGiftCard(request);
			responseObserver.onNext(giftCard.build());
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
	public void deleteGiftCard(DeleteGiftCardRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder orderLine = POSLogic.deleteGiftCard(request);
			responseObserver.onNext(orderLine.build());
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
	public void createGiftCardLine(CreateGiftCardLineRequest request, StreamObserver<GiftCardLine> responseObserver) {
		try {
			GiftCardLine.Builder giftCardLine = POSLogic.createAndConvertGiftCardLine(request);
			responseObserver.onNext(giftCardLine.build());
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
	public void getGiftCardLine(GetGiftCardLineRequest request, StreamObserver<GiftCardLine> responseObserver) {
		try {
			GiftCardLine.Builder giftCardLine = POSLogic.getGiftCardLine(request.getId(), null);
			responseObserver.onNext(giftCardLine.build());
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
	public void listGiftCardLines(ListGiftCardLinesRequest request, StreamObserver<ListGiftCardLinesResponse> responseObserver) {
		try {
			ListGiftCardLinesResponse.Builder listGiftCardLines = POSLogic.listGiftCardLines(request);
			responseObserver.onNext(listGiftCardLines.build());
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
	public void updateGiftCardLine(UpdateGiftCardLineRequest request, StreamObserver<GiftCardLine> responseObserver) {
		try {
			GiftCardLine.Builder giftCardLine = POSLogic.updateGiftCardLine(request);
			responseObserver.onNext(giftCardLine.build());
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
	public void deleteGiftCardLine(DeleteGiftCardLineRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder giftCardLine = POSLogic.deleteGiftCardLine(request);
			responseObserver.onNext(giftCardLine.build());
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
	public void listAvailableOrderLinesForGiftCard(ListAvailableOrderLinesForGiftCardRequest request, StreamObserver<ListAvailableOrderLinesForGiftCardResponse> responseObserver) {
		try {
			ListAvailableOrderLinesForGiftCardResponse.Builder listAvailableOrderLines = POSLogic.listAvailableOrderLinesForGiftCard(request);
			responseObserver.onNext(listAvailableOrderLines.build());
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
	public void listAvailableOrderLinesForRMA(ListAvailableOrderLinesForRMARequest request, StreamObserver<ListAvailableOrderLinesForRMAResponse> responseObserver) {
		try {
			ListAvailableOrderLinesForRMAResponse.Builder listAvailableOrderLines = POSLogic.listAvailableOrderLinesForRMA(request);
			responseObserver.onNext(listAvailableOrderLines.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
					Status.INTERNAL
							.withDescription(e.getLocalizedMessage())
							.withCause(e)
							.asRuntimeException()
			);
		}
	}



	@Override
	public void createPayment(CreatePaymentRequest request, StreamObserver<Payment> responseObserver) {
		try {
			Payment.Builder payment = PaymentConvertUtil.convertPayment(
				createPayment(request)
			);
			responseObserver.onNext(payment.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void updatePayment(UpdatePaymentRequest request, StreamObserver<Payment> responseObserver) {
		try {
			Payment.Builder payment = PaymentConvertUtil.convertPayment(
				updatePayment(request)
			);
			responseObserver.onNext(payment.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void deletePayment(DeletePaymentRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder empty = deletePayment(request);
			responseObserver.onNext(empty.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listPayments(ListPaymentsRequest request, StreamObserver<ListPaymentsResponse> responseObserver) {
		try {
			ListPaymentsResponse.Builder paymentList = listPayments(request);
			responseObserver.onNext(paymentList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void existsUnapprovedOnlinePayments(ExistsUnapprovedOnlinePaymentsRequest request, StreamObserver<ExistsUnapprovedOnlinePaymentsResponse> responseObserver) {
		try {
			ExistsUnapprovedOnlinePaymentsResponse.Builder builder = PaymentServiceLogic.existsUnapprovedOnlinePayments(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void processOnlinePayment(ProcessOnlinePaymentRequest request, StreamObserver<ProcessOnlinePaymentResponse> responseObserver) {
		try {
			ProcessOnlinePaymentResponse.Builder builder = POSLogic.processOnlinePayment(
				request
			);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void infoOnlinePayment(InfoOnlinePaymentRequest request, StreamObserver<InfoOnlinePaymentResponse> responseObserver) {
		try {
			InfoOnlinePaymentResponse.Builder builder = POSLogic.infoOnlinePayment(
				request
			);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void cancelOnlinePayment(CancelOnlinePaymentRequest request, StreamObserver<CancelOnlinePaymentResponse> responseObserver) {
		try {
			CancelOnlinePaymentResponse.Builder builder = POSLogic.cancelOnlinePayment(
				request
			);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void getKeyLayout(GetKeyLayoutRequest request, StreamObserver<KeyLayout> responseObserver) {
		try {
			KeyLayout.Builder keyLayout = ConvertUtil.convertKeyLayout(request.getKeyLayoutId());
			responseObserver.onNext(keyLayout.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}



	@Override
	public void getAvailableRefund(GetAvailableRefundRequest request, StreamObserver<AvailableRefund> responseObserver) {
		try {
			AvailableRefund.Builder availableRefund = getAvailableRefund(request);
			responseObserver.onNext(availableRefund.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	/**
	 * Calculate Available refund from daily operations
	 * @param request
	 * @return
	 * @return AvailableRefund.Builder
	 */
	private AvailableRefund.Builder getAvailableRefund(GetAvailableRefundRequest request) {
		return AvailableRefund.newBuilder();
	}



	@Override
	public void validatePIN(ValidatePINRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder empty = validatePIN(request);
			responseObserver.onNext(empty.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void listAvailableWarehouses(ListAvailableWarehousesRequest request,
			StreamObserver<ListAvailableWarehousesResponse> responseObserver) {
		try {
			ListAvailableWarehousesResponse.Builder warehouses = listWarehouses(request);
			responseObserver.onNext(warehouses.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/point-of-sales/{pos_id}/payment-methods/credit-card-types"
	 */
	@Override
	public void listCreditCardTypes(ListCreditCardTypesRequest request,
			StreamObserver<ListCreditCardTypesResponse> responseObserver) {
		try {
			ListCreditCardTypesResponse.Builder tenderTypes = PaymentServiceLogic.listCreditCardTypes(request);
			responseObserver.onNext(tenderTypes.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/card-providers"
	 */
	@Override
	public void listCardProviders(ListCardProvidersRequest request,
			StreamObserver<ListCardProvidersResponse> responseObserver) {
		try {
			ListCardProvidersResponse.Builder tenderTypes = PaymentServiceLogic.listCardProviders(request);
			responseObserver.onNext(tenderTypes.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/card-providers/{card_provider_id}/cards"
	 */
	@Override
	public void listCards(ListCardsRequest request,
			StreamObserver<ListCardsResponse> responseObserver) {
		try {
			ListCardsResponse.Builder tenderTypes = PaymentServiceLogic.listCards(request);
			responseObserver.onNext(tenderTypes.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listAvailablePaymentMethods(ListAvailablePaymentMethodsRequest request,
			StreamObserver<ListAvailablePaymentMethodsResponse> responseObserver) {
		try {
			ListAvailablePaymentMethodsResponse.Builder tenderTypes = listPaymentMethods(request);
			responseObserver.onNext(tenderTypes.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listAvailablePriceList(ListAvailablePriceListRequest request,
			StreamObserver<ListAvailablePriceListResponse> responseObserver) {
		try {
			ListAvailablePriceListResponse.Builder priceList = listPriceList(request);
			responseObserver.onNext(priceList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listAvailableCurrencies(ListAvailableCurrenciesRequest request,
			StreamObserver<ListAvailableCurrenciesResponse> responseObserver) {
		try {
			ListAvailableCurrenciesResponse.Builder currencies = listCurrencies(request);
			responseObserver.onNext(currencies.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void listAvailableDocumentTypes(ListAvailableDocumentTypesRequest request,
			StreamObserver<ListAvailableDocumentTypesResponse> responseObserver) {
		try {
			ListAvailableDocumentTypesResponse.Builder documentTypes = listDocumentTypes(request);
			responseObserver.onNext(documentTypes.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listAvailableDiscounts(ListAvailableDiscountsRequest request,
			StreamObserver<ListAvailableDiscountsResponse> responseObserver) {
		try {
			ListAvailableDiscountsResponse.Builder documentTypes = POSLogic.listAvailableDiscounts(request);
			responseObserver.onNext(documentTypes.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}



	@Override
	public void listCustomers(ListCustomersRequest request, StreamObserver<ListCustomersResponse> responseObserver) {
		try {
			ListCustomersResponse.Builder customer = POSLogic.listCustomers(request);
			responseObserver.onNext(customer.build());
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
	public void getCustomer(GetCustomerRequest request, StreamObserver<Customer> responseObserver) {
		try {
			Customer.Builder customer = POSLogic.getCustomer(request);
			responseObserver.onNext(customer.build());
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
	public void listCustomerTemplates(ListCustomerTemplatesRequest request, StreamObserver<ListCustomerTemplatesResponse> responseObserver) {
		try {
			ListCustomerTemplatesResponse.Builder customerTemplatesList = POSLogic.listCustomerTemplates(request);
			responseObserver.onNext(
				customerTemplatesList.build()
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



	/**
	 * get: "/point-of-sales/{pos_id}/orders/{order_id}/print"
	 */
	@Override
	public void printTicket(PrintTicketRequest request, StreamObserver<PrintTicketResponse> responseObserver) {
		try {
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			MOrder salesOrder = OrderUtil.validateAndGetOrder(request.getOrderId());

			log.fine("Print Ticket = " + request);
			//	Print based on handler
			TicketResult ticketResult = TicketHandler.getInstance()
				.withPosId(
					pos.getC_POS_ID()
				)
				.withTableName(I_C_Order.Table_Name)
				.withRecordId(
					salesOrder.getC_Order_ID()
				)
				.printTicket()
			;
			//	Process response
			PrintTicketResponse.Builder builder = PrintTicketResponse.newBuilder()
				.setIsDirectPrint(
					pos.get_ValueAsBoolean(I_AD_Process.COLUMNNAME_IsDirectPrint)
				)
			;
			if(ticketResult != null) {
				builder
					.setIsError(ticketResult.isError())
					.setSummary(
						StringManager.getValidString(
							Msg.parseTranslation(
								Env.getCtx(),
								ticketResult.getSummary()
							)
						)
					)
				;

				// Write a single file
				File fileReport = ticketResult.getReportFile();
				if(fileReport != null && fileReport.exists()) {
					String validFileName = FileUtil.getValidFileName(fileReport.getName());
					String fileType = FileUtil.getExtension(validFileName);
					if(Util.isEmpty(fileType)) {
						fileType = fileType.replaceAll(".", "");
					}
					ByteString resultFile = ByteString.empty();
					try {
						FileInputStream inputStream = new FileInputStream(fileReport);
						resultFile = ByteString.readFrom(inputStream);
					} catch (IOException e) {
						log.warning(e.getLocalizedMessage());
						builder
							.setSummary(
								StringManager.getValidString(
									Msg.parseTranslation(
										Env.getCtx(),
										e.getLocalizedMessage()
									)
								)
							)
							.setIsError(true);
					}
					builder
						.setFileName(
							StringManager.getValidString(validFileName)
						)
						.setMimeType(
							StringManager.getValidString(
								MimeType.getMimeType(validFileName)
							)
						)
						.setResultType(
							StringManager.getValidString(fileType)
						)
						.setOutputStream(resultFile)
					;
				}

				// Write a multiple files
				List<File> filesReportList = ticketResult.getReportFiles();
				if (filesReportList != null && !filesReportList.isEmpty()) {
					filesReportList.stream().forEach(reportFile -> {
						if(fileReport == null || !fileReport.exists()) {
							return;
						}
						ByteString resultFile = ByteString.empty();
						try {
							FileInputStream inputStream = new FileInputStream(reportFile);
							resultFile = ByteString.readFrom(inputStream);
						} catch (IOException e) {
							log.warning(e.getLocalizedMessage());
							builder
								.setSummary(
									StringManager.getValidString(
										Msg.parseTranslation(
											Env.getCtx(),
											e.getLocalizedMessage()
										)
									)
								)
								.setIsError(true);
						}
						builder.addOutputStreams(resultFile);
					});
				}

				//	Write a map values
				if(ticketResult.getResultValues() != null) {
					Map<String, Object> resultValues = ticketResult.getResultValues();
					builder.setResultValues(
						ValueManager.getProtoValueFromObject(resultValues)
					);
				}
			}
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/orders/{order_id}/preview"
	 */
	@Override
	public void printPreview(PrintPreviewRequest request, StreamObserver<PrintPreviewResponse> responseObserver) {
		try {
			final int orderId = request.getOrderId();
			MOrder order = OrderUtil.validateAndGetOrder(orderId);

			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			int userId = Env.getAD_User_ID(pos.getCtx());
			if (!AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsPreviewDocument)) {
				throw new AdempiereException("@POS.PreviewDocumentNotAllowed@");
			}

			log.fine("Print Ticket = " + request);
			PrintPreviewResponse.Builder ticket = PrintPreviewResponse.newBuilder()
				.setResult("Ok")
			;

			// run process request
			// Rpt C_Order
			int processId = 110;
			int recordId = order.getC_Order_ID();
			String tableName = I_C_Order.Table_Name;
			final int invoiceId = order.getC_Invoice_ID();

			if (invoiceId > 0) {
				// Rpt C_Invoice
				processId = 116;
				tableName = I_C_Invoice.Table_Name;
				recordId = invoiceId;
			}
			String reportType = "pdf";
			if (!Util.isEmpty(request.getReportType(), true)) {
				reportType = request.getReportType();
			}
			GenerateReportRequest.Builder reportRequest = GenerateReportRequest.newBuilder()
				.setId(processId)
				.setReportType(reportType)
				.setTableName(tableName)
				.setRecordId(recordId)
			;
			ProcessLog.Builder processLog = ReportManagement.generateReport(
				reportRequest.build()
			);
			ReportOutput.Builder outputBuilder = processLog.getOutputBuilder();
			outputBuilder.setIsDirectPrint(
				pos.get_ValueAsBoolean(I_AD_Process.COLUMNNAME_IsDirectPrint)
			);
			processLog.setOutput(outputBuilder);

			// preview document
			ticket.setProcessLog(processLog.build());

			responseObserver.onNext(ticket.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/shipments/{shipment_id}/preview"
	 */
	@Override
	public void printShipmentPreview(PrintShipmentPreviewRequest request, StreamObserver<PrintShipmentPreviewResponse> responseObserver) {
		try {
			if (request.getShipmentId() <= 0) {
				throw new AdempiereException("@FillMandatory@ @Shipment@");
			}
			final int shipmentId = request.getShipmentId();
			MInOut shipment = new MInOut(Env.getCtx(), shipmentId, null);
			if (shipment == null || shipment.getM_InOut_ID() <= 0) {
				throw new AdempiereException("@Shipment@ (" + shipmentId + ") @NotFound@");
			}

			log.fine("Print Ticket = " + request);
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			int userId = Env.getAD_User_ID(pos.getCtx());
			if (!AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsPreviewDocument)) {
				throw new AdempiereException("@POS.PreviewDocumentNotAllowed@");
			}

			PrintShipmentPreviewResponse.Builder ticket = PrintShipmentPreviewResponse.newBuilder()
				.setResult("Ok")
			;

			// Rpt M_InOut
			int processId = 117;
			String reportType = "pdf";
			if (!Util.isEmpty(request.getReportType(), true)) {
				reportType = request.getReportType();
			}
			GenerateReportRequest.Builder reportRequest = GenerateReportRequest.newBuilder()
				.setId(processId)
				.setReportType(reportType)
				.setTableName(I_M_InOut.Table_Name)
				.setRecordId(request.getShipmentId())
			;
			ProcessLog.Builder processLog = ReportManagement.generateReport(
				reportRequest.build()
			);
			ReportOutput.Builder outputBuilder = processLog.getOutputBuilder();
			outputBuilder.setIsDirectPrint(
				pos.get_ValueAsBoolean(I_AD_Process.COLUMNNAME_IsDirectPrint)
			);
			processLog.setOutput(outputBuilder);

			// preview document
			ticket.setProcessLog(processLog.build());

			responseObserver.onNext(ticket.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/orders/{order_id}/gift-cards/{id}/preview"
	 */
	@Override
	public void printGiftCardPreview(PrintGiftCardPreviewRequest request, StreamObserver<PrintGiftCardPreviewResponse> responseObserver) {
		try {
			if (request.getId() <= 0) {
				throw new AdempiereException("@FillMandatory@ @ECA14_GiftCard@");
			}
			final int giftCardId = request.getId();
			MTable giftCardTable = MTable.get(Env.getCtx(), "ECA14_GiftCard");
			if (giftCardTable == null || giftCardTable.getAD_Table_ID() <= 0) {
				throw new AdempiereException("@TableName@ @NotFound@");
			}
			PO giftCard = giftCardTable.getPO(giftCardId, null);
			if (giftCard == null || giftCard.get_ValueAsInt("ECA14_GiftCard_ID") <= 0) {
				throw new AdempiereException("@ECA14_GiftCard_ID@ (" + giftCardId + ") @NotFound@");
			}

			log.fine("Print Ticket = " + request);
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			int userId = Env.getAD_User_ID(pos.getCtx());
			if (!AccessManagement.getBooleanValueFromPOS(pos, userId, ColumnsAdded.COLUMNNAME_IsAllowsPreviewDocument)) {
				throw new AdempiereException("@POS.PreviewDocumentNotAllowed@");
			}

			int giftCardTabId = 55269;
			MTab giftCardTab = MTab.get(Env.getCtx(), giftCardTabId);
			int processId = giftCardTab.getAD_Process_ID();

			String reportType = "pdf";
			if (!Util.isEmpty(request.getReportType(), true)) {
				reportType = request.getReportType();
			}
			GenerateReportRequest.Builder reportRequest = GenerateReportRequest.newBuilder()
				.setId(processId)
				.setReportType(reportType)
				.setTableName(giftCardTable.getTableName())
				.setRecordId(giftCardId)
			;
			ProcessLog.Builder processLog = ReportManagement.generateReport(
				reportRequest.build()
			);
			ReportOutput.Builder outputBuilder = processLog.getOutputBuilder();
			outputBuilder.setIsDirectPrint(
				pos.get_ValueAsBoolean(I_AD_Process.COLUMNNAME_IsDirectPrint)
			);
			processLog.setOutput(outputBuilder);

			// preview document
			PrintGiftCardPreviewResponse.Builder ticket = PrintGiftCardPreviewResponse.newBuilder()
				.setResult("Ok")
				.setProcessLog(processLog.build())
			;

			responseObserver.onNext(ticket.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void createCustomerBankAccount(CreateCustomerBankAccountRequest request, StreamObserver<CustomerBankAccount> responseObserver) {
		try {
			if(request.getCustomerId() <= 0) {
				throw new AdempiereException("@C_BPartner_ID@ @IsMandatory@");
			}
			if(Util.isEmpty(request.getAccountNo())) {
				throw new AdempiereException("@AccountNo@ @IsMandatory@");
			}
			MBPartner businessPartner = MBPartner.get(Env.getCtx(), request.getCustomerId());
			int bankId = request.getBankId();
			if(bankId <= 0 && request.getIsAch()) {
				throw new AdempiereException("@C_Bank_ID@ @IsMandatory@");
			}
			String accountName = request.getName();
			if(Util.isEmpty(accountName)) {
				accountName = businessPartner.getName() + "_" + request.getAccountNo();
				if (bankId >= 0) {
					MBank bank = MBank.get(Env.getCtx(), bankId);
					if (bank != null) {
						accountName += " (" + bank.getRoutingNo() + ")";
					}
				}
			}
			int customerBankAccountId = BankManagement.getCustomerBankAccountFromAccount(
				Env.getCtx(),
				businessPartner.getC_BPartner_ID(),
				bankId,
				request.getAccountNo(),
				request.getSocialSecurityNumber()
			);
			if(customerBankAccountId < 0) {
				customerBankAccountId = 0;
			}
			//	For data
			MBPBankAccount businessPartnerBankAccount = new MBPBankAccount(Env.getCtx(), customerBankAccountId, null);
			businessPartnerBankAccount.setC_BPartner_ID(businessPartner.getC_BPartner_ID());
			businessPartnerBankAccount.setIsACH(request.getIsAch());
			//	Validate all data
			Optional.ofNullable(request.getCity()).ifPresent(value -> businessPartnerBankAccount.setA_City(value));
			Optional.ofNullable(request.getCountry()).ifPresent(value -> businessPartnerBankAccount.setA_Country(value));
			Optional.ofNullable(request.getEmail()).ifPresent(value -> businessPartnerBankAccount.setA_EMail(value));
			Optional.ofNullable(request.getDriverLicense()).ifPresent(value -> businessPartnerBankAccount.setA_Ident_DL(value));
			Optional.ofNullable(request.getSocialSecurityNumber()).ifPresent(value -> businessPartnerBankAccount.setA_Ident_SSN(value));
			Optional.ofNullable(accountName).ifPresent(value -> businessPartnerBankAccount.setA_Name(value));
			Optional.ofNullable(request.getState()).ifPresent(value -> businessPartnerBankAccount.setA_State(value));
			Optional.ofNullable(request.getStreet()).ifPresent(value -> businessPartnerBankAccount.setA_Street(value));
			Optional.ofNullable(request.getZip()).ifPresent(value -> businessPartnerBankAccount.setA_Zip(value));
			Optional.ofNullable(request.getAccountNo()).ifPresent(value -> businessPartnerBankAccount.setAccountNo(value));
			if(bankId > 0) {
				businessPartnerBankAccount.setC_Bank_ID(bankId);
			}
			Optional.ofNullable(request.getAddressVerified()).ifPresent(value -> businessPartnerBankAccount.setR_AvsAddr(value));
			Optional.ofNullable(request.getZipVerified()).ifPresent(value -> businessPartnerBankAccount.setR_AvsZip(value));
			Optional.ofNullable(request.getRoutingNo()).ifPresent(value -> businessPartnerBankAccount.setRoutingNo(value));
			Optional.ofNullable(request.getIban()).ifPresent(value -> businessPartnerBankAccount.setIBAN(value));
			//	Bank Account Type
			if(Util.isEmpty(request.getBankAccountType())) {
				businessPartnerBankAccount.setBankAccountType(MBPBankAccount.BANKACCOUNTTYPE_Savings);
			} else {
				businessPartnerBankAccount.setBankAccountType(request.getBankAccountType());
			}
			businessPartnerBankAccount.saveEx();
			responseObserver.onNext(
				ConvertUtil.convertCustomerBankAccount(businessPartnerBankAccount).build()
			);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void updateCustomerBankAccount(UpdateCustomerBankAccountRequest request, StreamObserver<CustomerBankAccount> responseObserver) {
		try {
			MBPBankAccount businessPartnerBankAccount = BankManagement.validateAndGetCustomerBankAccount(
				request.getId()
			);
			//	For data
			businessPartnerBankAccount.setIsACH(request.getIsAch());
			//	Validate all data
			Optional.ofNullable(request.getCity()).ifPresent(value -> businessPartnerBankAccount.setA_City(value));
			Optional.ofNullable(request.getCountry()).ifPresent(value -> businessPartnerBankAccount.setA_Country(value));
			Optional.ofNullable(request.getEmail()).ifPresent(value -> businessPartnerBankAccount.setA_EMail(value));
			Optional.ofNullable(request.getDriverLicense()).ifPresent(value -> businessPartnerBankAccount.setA_Ident_DL(value));
			Optional.ofNullable(request.getSocialSecurityNumber()).ifPresent(value -> businessPartnerBankAccount.setA_Ident_SSN(value));
			Optional.ofNullable(request.getName()).ifPresent(value -> businessPartnerBankAccount.setA_Name(value));
			Optional.ofNullable(request.getState()).ifPresent(value -> businessPartnerBankAccount.setA_State(value));
			Optional.ofNullable(request.getStreet()).ifPresent(value -> businessPartnerBankAccount.setA_Street(value));
			Optional.ofNullable(request.getZip()).ifPresent(value -> businessPartnerBankAccount.setA_Zip(value));
			Optional.ofNullable(request.getAccountNo()).ifPresent(value -> businessPartnerBankAccount.setAccountNo(value));
			if(request.getBankId() > 0) {
				businessPartnerBankAccount.setC_Bank_ID(request.getBankId());
			}
			Optional.ofNullable(request.getAddressVerified()).ifPresent(value -> businessPartnerBankAccount.setR_AvsAddr(value));
			Optional.ofNullable(request.getZipVerified()).ifPresent(value -> businessPartnerBankAccount.setR_AvsZip(value));
			Optional.ofNullable(request.getRoutingNo()).ifPresent(value -> businessPartnerBankAccount.setRoutingNo(value));
			Optional.ofNullable(request.getIban()).ifPresent(value -> businessPartnerBankAccount.setIBAN(value));
			//	Bank Account Type
			if(Util.isEmpty(request.getBankAccountType())) {
				businessPartnerBankAccount.setBankAccountType(MBPBankAccount.BANKACCOUNTTYPE_Savings);
			} else {
				businessPartnerBankAccount.setBankAccountType(request.getBankAccountType());
			}
			businessPartnerBankAccount.saveEx();
			responseObserver.onNext(
				ConvertUtil.convertCustomerBankAccount(businessPartnerBankAccount).build()
			);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void getCustomerBankAccount(GetCustomerBankAccountRequest request, StreamObserver<CustomerBankAccount> responseObserver) {
		try {
			MBPBankAccount businessPartnerBankAccount = BankManagement.validateAndGetCustomerBankAccount(
				request.getId()
			);
			//	For data
			responseObserver.onNext(
				ConvertUtil.convertCustomerBankAccount(businessPartnerBankAccount).build()
			);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void deleteCustomerBankAccount(DeleteCustomerBankAccountRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request.getId() <= 0) {
				throw new AdempiereException("@C_BP_BankAccount_ID@ @IsMandatory@");
			}
			//	For data
			MBPBankAccount businessPartnerBankAccount = BankManagement.validateAndGetCustomerBankAccount(
				request.getId()
			);
			businessPartnerBankAccount.deleteEx(true);
			responseObserver.onNext(
				Empty.newBuilder().build()
			);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listBanks(ListBanksRequest request, StreamObserver<ListBanksResponse> responseObserver) {
		try {
			ListBanksResponse.Builder builder = BankManagement.listBanks(request);
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
	public void listBankAccounts(ListBankAccountsRequest request, StreamObserver<ListBankAccountsResponse> responseObserver) {
		try {
			ListBankAccountsResponse.Builder bankAccountsBuilderList = BankManagement.listBankAccounts(request);
			responseObserver.onNext(bankAccountsBuilderList.build());
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
	public void listCustomerBankAccounts(ListCustomerBankAccountsRequest request, StreamObserver<ListCustomerBankAccountsResponse> responseObserver) {
		try {
			if(request.getCustomerId() <= 0) {
				throw new AdempiereException("@C_BPartner_ID@ @IsMandatory@");
			}
			String whereClause = I_C_BP_BankAccount.COLUMNNAME_C_BPartner_ID + " = ?";
			int customerId = request.getCustomerId();
			List<Object> filtersList = new ArrayList<Object>();
			filtersList.add(customerId);
			if (request.getBankId() > 0) {
				whereClause += " AND " + I_C_BP_BankAccount.COLUMNNAME_C_Bank_ID + " = ?";
				filtersList.add(
					request.getBankId()
				);
			}

			Query query = new Query(
				Env.getCtx(),
				I_C_BP_BankAccount.Table_Name,
				whereClause,
				null
			)
				.setParameters(filtersList)
				.setClient_ID()
				.setOnlyActiveRecords(true)
			;

			//	Set page token
			int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
			int limit = LimitUtil.getPageSize(request.getPageSize());
			int offset = (pageNumber - 1) * limit;
			int count = query.count();
			String nexPageToken = null;
			if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
				nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
			}

			ListCustomerBankAccountsResponse.Builder builder = ListCustomerBankAccountsResponse.newBuilder()
				.setRecordCount(
					count
				)
				.setNextPageToken(
					StringManager.getValidString(nexPageToken)
				)
			;
			query
				.setLimit(limit, offset)
				.getIDsAsList()
				.forEach(customerBankAccountId -> {
					MBPBankAccount customerBankAccount = new MBPBankAccount(Env.getCtx(), customerBankAccountId, null);
					builder.addCustomerBankAccounts(
						ConvertUtil.convertCustomerBankAccount(
							customerBankAccount
						)
					);
				})
			;
			//	

			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void createShipment(CreateShipmentRequest request, StreamObserver<Shipment> responseObserver) {
		try {
			Shipment.Builder shipment = createShipment(request);
			responseObserver.onNext(shipment.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	@Override
	public void createShipmentLine(CreateShipmentLineRequest request, StreamObserver<ShipmentLine> responseObserver) {
		try {
			ShipmentLine.Builder shipmentLine = createAndConvertShipmentLine(request);
			responseObserver.onNext(shipmentLine.build());
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
	public void updateShipmentLine(UpdateShipmentLineRequest request, StreamObserver<ShipmentLine> responseObserver) {
		try {
			ShipmentLine.Builder shipmentLine = POSLogic.updateShipmentLine(request);
			responseObserver.onNext(shipmentLine.build());
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
	public void deleteShipmentLine(DeleteShipmentLineRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder nothing = deleteShipmentLine(request);
			responseObserver.onNext(nothing.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listShipmentLines(ListShipmentLinesRequest request, StreamObserver<ListShipmentLinesResponse> responseObserver) {
		try {
			ListShipmentLinesResponse.Builder shipmentLinesList = listShipmentLines(request);
			responseObserver.onNext(shipmentLinesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void processShipment(ProcessShipmentRequest request, StreamObserver<Shipment> responseObserver) {
		try {
			Shipment.Builder shipment = processShipment(request);
			responseObserver.onNext(shipment.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}



	@Override
	public void reverseSales(ReverseSalesRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderServiceLogic.reverseSalesTransaction(request);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void processReverseSales(ProcessReverseSalesRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder order = OrderServiceLogic.processReverseSales(request);
			responseObserver.onNext(order.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void cashOpening(CashOpeningRequest request, StreamObserver<CashMovements> responseObserver) {
		try {
			CashMovements.Builder builder = CashServiceLogic.cashOpening(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void cashClosing(CashClosingRequest request, StreamObserver<CashMovements> responseObserver) {
		try {
			CashMovements.Builder closing = CashServiceLogic.cashClosing(request);
			responseObserver.onNext(closing.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void cashWithdrawal(CashWithdrawalRequest request, StreamObserver<CashMovements> responseObserver) {
		try {
			CashMovements.Builder builder = CashServiceLogic.cashWithdrawal(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/cash/{id}"
	 * get: "/point-of-sales/{pos_id}/cash/movements"
	 */
	@Override
	public void listCashMovements(ListCashMovementsRequest request, StreamObserver<ListCashMovementsResponse> responseObserver) {
		try {
			ListCashMovementsResponse.Builder response = CashServiceLogic.listCashMovements(request);
			responseObserver.onNext(response.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/cash/{id}/summary"
	 */
	@Override
	public void listCashSummaryMovements(ListCashSummaryMovementsRequest request, StreamObserver<ListCashSummaryMovementsResponse> responseObserver) {
		try {
			ListCashSummaryMovementsResponse.Builder response = CashServiceLogic.listCashSummaryMovements(request);
			responseObserver.onNext(response.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/cash/{bank_statement_id}/preview"
	 */
	@Override
	public void printPreviewCashMovements(PrintPreviewCashMovementsRequest request, StreamObserver<PrintPreviewCashMovementsResponse> responseObserver) {
		try {
			log.fine("Print Cash Movements = " + request);
			PrintPreviewCashMovementsResponse.Builder ticket = CashServiceLogic.printPreviewCashMovements(request);
			responseObserver.onNext(ticket.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/cash/{id}/closing/preview"
	 */
	@Override
	public void printPreviewCashClosing(PrintPreviewCashClosingRequest request, StreamObserver<PrintPreviewCashClosingResponse> responseObserver) {
		try {
			log.fine("Print Cash Closing = " + request);
			PrintPreviewCashClosingResponse.Builder ticket = CashServiceLogic.printPreviewCashClosing(request);
			responseObserver.onNext(ticket.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * get: "/point-of-sales/{pos_id}/cash/{id}/online/preview"
	 */
	@Override
	public void printPreviewOnlineCashClosing(PrintPreviewOnlineCashClosingRequest request, StreamObserver<PrintPreviewOnlineCashClosingResponse> responseObserver) {
		try {
			log.fine("Print Online Cash Closing = " + request);
			PrintPreviewOnlineCashClosingResponse.Builder ticket = CashServiceLogic.printPreviewOnlineCashClosing(request);
			responseObserver.onNext(ticket.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
					Status.INTERNAL
							.withDescription(e.getLocalizedMessage())
							.withCause(e)
							.asRuntimeException()
			);
		}
	}



	/**
	 * get: "/point-of-sales/{pos_id}/cash/{id}/online/exists"
	 */
	@Override
	public void existsOnlineCashClosingPayments(ExistsOnlineCashClosingPaymentsRequest request, StreamObserver<ExistsOnlineCashClosingPaymentsResponse> responseObserver) {
		try {
			log.fine("Exists Online Cash Closing Payments = " + request);
			ExistsOnlineCashClosingPaymentsResponse.Builder builder = ExistsOnlineCashClosingPaymentsResponse.newBuilder();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * post: "/point-of-sales/{pos_id}/cash/{id}/online"
	 */
	@Override
	public void processOnlineCashClosing(ProcessOnlineCashClosingRequest request, StreamObserver<ProcessOnlineCashClosingResponse> responseObserver) {
		try {
			log.fine("Process Online Cash Closing = " + request);
			ProcessOnlineCashClosingResponse.Builder builder = CashServiceLogic.processOnlineCashClosing(
					request
			);
			//ProcessOnlineCashClosingResponse.Builder builder = ProcessOnlineCashClosingResponse.newBuilder();
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
		 * get: "/point-of-sales/{pos_id}/cash/{id}/online"
	 */
	@Override
	public void infoOnlineCashClosing(InfoOnlineCashClosingRequest request, StreamObserver<InfoOnlineCashClosingResponse> responseObserver) {
		try {
			log.fine("Info Online Cash Closing = " + request);
			InfoOnlineCashClosingResponse.Builder builder = CashServiceLogic.infoOnlineCashClosing(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * put: "/point-of-sales/{pos_id}/cash/{id}/online"
	 */
	@Override
	public void cancelOnlineCashClosing(CancelOnlineCashClosingRequest request, StreamObserver<CancelOnlineCashClosingResponse> responseObserver) {
		try {
			log.fine("Cancel Online Cash Closing = " + request);
			CancelOnlineCashClosingResponse.Builder ticket = CancelOnlineCashClosingResponse.newBuilder();
			responseObserver.onNext(ticket.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listAvailableSellers(ListAvailableSellersRequest request, StreamObserver<ListAvailableSellersResponse> responseObserver) {
		try {
			ListAvailableSellersResponse.Builder response = SellerServiceLogic.listAvailableSellers(request);
			responseObserver.onNext(response.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void allocateSeller(AllocateSellerRequest request, StreamObserver<AvailableSeller> responseObserver) {
		try {
			AvailableSeller.Builder response = SellerServiceLogic.allocateSeller(request);
			responseObserver.onNext(response.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void deallocateSeller(DeallocateSellerRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder empty = SellerServiceLogic.deallocateSeller(request);
			responseObserver.onNext(empty.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void createPaymentReference(CreatePaymentReferenceRequest request, StreamObserver<PaymentReference> responseObserver) {
		try {
			PaymentReference.Builder refund = createPaymentReference(request);
			responseObserver.onNext(refund.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	/**
	 * delete: "/point-of-sales/orders/{order_id}/references/{id}"
	 */
	@Override
	public void deletePaymentReference(DeletePaymentReferenceRequest request, StreamObserver<Empty> responseObserver) {
		try {
			Empty.Builder empty = PaymentServiceLogic.deletePaymentReference(request);
			responseObserver.onNext(empty.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	
	@Override
	public void listPaymentReferences(ListPaymentReferencesRequest request, StreamObserver<ListPaymentReferencesResponse> responseObserver) {
		try {
			ListPaymentReferencesResponse.Builder refundReferenceList = listPaymentReferencesLines(request);
			responseObserver.onNext(refundReferenceList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}



	@Override
	public void createRMA(CreateRMARequest request, StreamObserver<RMA> responseObserver) {
		try {
			MOrder rma = ReturnSalesOrder.createRMAFromOrder(request.getPosId(), request.getSourceOrderId(), request.getSalesRepresentativeId(), request.getIsCreateLinesFromOrder(), null);
			RMA.Builder returnOrder = ConvertUtil.convertRMA(rma);
			responseObserver.onNext(returnOrder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void createRMALine(CreateRMALineRequest request, StreamObserver<RMALine> responseObserver) {
		try {
			MOrderLine rmaLine = ReturnSalesOrder.createRMALineFromOrder(
				request.getRmaId(),
				request.getSourceOrderLineId(),
				NumberManager.getBigDecimalFromString(
					request.getQuantity()
				),
				request.getDescription()
			);
			RMALine.Builder returnLine = ConvertUtil.convertRMALine(rmaLine);
			responseObserver.onNext(returnLine.build());
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
	public void updateRMALine(UpdateRMALineRequest request, StreamObserver<RMALine> responseObserver) {
		try {
			MOrderLine rmaLine = ReturnSalesOrder.updateRMALine(
				request.getId(),
				NumberManager.getBigDecimalFromString(
					request.getQuantity()
				),
				request.getDescription()
			);
			RMALine.Builder returnLine = ConvertUtil.convertRMALine(rmaLine);
			responseObserver.onNext(returnLine.build());
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
	public void deleteRMALine(DeleteRMALineRequest request, StreamObserver<Empty> responseObserver) {
		try {
			RMAUtil.deleteRMALine(request.getId());
			responseObserver.onNext(Empty.newBuilder().build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	@Override
	public void deleteRMA(DeleteRMARequest request, StreamObserver<Empty> responseObserver) {
		try {
			RMAUtil.deleteRMA(request.getId());
			responseObserver.onNext(Empty.newBuilder().build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void listRMALines(ListRMALinesRequest request, StreamObserver<ListRMALinesResponse> responseObserver) {
		try {
			ListRMALinesResponse.Builder rmaLinesList = listRMALines(request);
			responseObserver.onNext(rmaLinesList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
	
	@Override
	public void processRMA(ProcessRMARequest request, StreamObserver<RMA> responseObserver) {
		try {
			RMA.Builder rma = ConvertUtil.convertRMA(
				ReturnSalesOrder.processRMA(
					request.getRmaId(),
					request.getPosId(),
					request.getDocumentAction(),
					request.getDescription()
				)
			);
			responseObserver.onNext(rma.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}


	@Override
	public void copyOrder(CopyOrderRequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder salesOrder = OrderConverUtil.convertOrder(
				OrderManagement.createOrderFromOther(
					request.getPosId(),
					request.getSalesRepresentativeId(),
					request.getSourceOrderId()
				)
			);
			responseObserver.onNext(salesOrder.build());
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
	public void createOrderFromRMA(CreateOrderFromRMARequest request, StreamObserver<Order> responseObserver) {
		try {
			Order.Builder salesOrder = OrderConverUtil.convertOrder(
				OrderManagement.createOrderFromRMA(
					request.getPosId(),
					request.getSalesRepresentativeId(),
					request.getSourceRmaId()
				)
			);
			responseObserver.onNext(salesOrder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}


	@Override
	public void listCustomerCredits(ListCustomerCreditsRequest request, StreamObserver<ListCustomerCreditsResponse> responseObserver) {
		try {
			ListCustomerCreditsResponse.Builder response = listCustomerCredits(request);
			responseObserver.onNext(response.build());
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
	
	/**
	 * List shipment Lines from Order UUID
	 * TODO: Omit if exists into Payment Reference without open amount
	 * @param request
	 * @return
	 */
	private ListCustomerCreditsResponse.Builder listCustomerCredits(ListCustomerCreditsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if(request.getCustomerId() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ @NotFound@");
		}
		ListCustomerCreditsResponse.Builder builder = ListCustomerCreditsResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	
		MPOS pos = new MPOS(Env.getCtx(), request.getPosId(), null);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int count = 0;
		try {
			//	Get Bank statement
			StringBuffer sql = new StringBuffer("SELECT i.C_Invoice_ID, i.DocumentNo, i.Description, i.DateInvoiced, i.C_Currency_ID, ")
				.append("i.GrandTotal, (InvoiceOpen(i.C_Invoice_ID, null) * -1) AS OpenAmount, ")
				.append("i.Processed, i.Processing ")
				.append("FROM C_Invoice AS i ")
				.append("WHERE i.IsSOTrx = 'Y' ")
				.append("AND i.DocStatus IN('CO', 'CL') ")
				.append("AND i.IsPaid = 'N' ")
				.append("AND EXISTS(SELECT 1 FROM C_DocType dt WHERE dt.C_DocType_ID = i.C_DocType_ID AND dt.DocBaseType = 'ARC') ")
				.append("AND i.C_BPartner_ID = ? ")
				.append("AND i.AD_Org_ID = ? ")
				.append("AND InvoiceOpen(i.C_Invoice_ID, null) <> 0")
			;
			StringBuffer whereClause = new StringBuffer();
			List<Object> parameters = new ArrayList<Object>();
			//	Count records
			parameters.add(request.getCustomerId());
			parameters.add(pos.getAD_Org_ID());

			// search value
			final String searchValue = StringManager.getDecodeUrl(
				request.getSearchValue()
			);
			if(!Util.isEmpty(searchValue, true)) {
				whereClause.append(" AND UPPER(i.DocumentNo) LIKE '%' || UPPER(?) || '%'");
				//	Add parameters
				parameters.add(searchValue);
			}

			if(request.getDocumentTypeId() > 0) {
				sql.append(" AND i.C_DocType_ID = ?");
				parameters.add(request.getDocumentTypeId());
			}
			sql.append(whereClause);
			sql.append(" ORDER BY i.DateInvoiced DESC");
			count = CountUtil.countRecords(sql.toString(), "C_Invoice i", parameters);
			pstmt = DB.prepareStatement(sql.toString(), null);
			int index = 1;
			pstmt.setInt(index++, request.getCustomerId());
			pstmt.setInt(index++, pos.getAD_Org_ID());

			if(whereClause.length() > 0) {
				pstmt.setString(index++, searchValue);
			}
			if(request.getDocumentTypeId() > 0) {
				pstmt.setInt(index++, request.getDocumentTypeId());
			}
			//	Get from Query
			rs = pstmt.executeQuery();
			while(rs.next()) {
				CreditMemo.Builder creditMemo = CreditMemo.newBuilder()
					.setId(
						rs.getInt(
							I_C_Invoice.COLUMNNAME_C_Invoice_ID
						)
					)
					.setDocumentNo(
						StringManager.getValidString(
							rs.getString(
								I_C_Invoice.COLUMNNAME_DocumentNo
							)
						)
					)
					.setDescription(
						StringManager.getValidString(
							rs.getString(
								I_C_Invoice.COLUMNNAME_Description
							)
						)
					)
					.setDocumentDate(
						ValueManager.getProtoTimestampFromTimestamp(
							rs.getTimestamp(
								I_C_Invoice.COLUMNNAME_DateInvoiced
							)
						)
					)
					.setCurrency(
						CoreFunctionalityConvert.convertCurrency(
							rs.getInt(
								I_C_Invoice.COLUMNNAME_C_Currency_ID
							)
						)
					)
					.setAmount(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal(
								I_C_Invoice.COLUMNNAME_GrandTotal
							)
						)
					)
					.setOpenAmount(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal("OpenAmount")
						)
					)
					.setIsProcessed(
						rs.getBoolean(
							I_C_Invoice.COLUMNNAME_Processed
						)
					)
					.setIsProcessing(
						rs.getBoolean(
							I_C_Invoice.COLUMNNAME_Processing
						)
					)
				;
				//	
				builder.addRecords(creditMemo.build());
				count++;
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
		}
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}



	/**
	 * Create Refund Reference
	 * @param request
	 * @return
	 */
	private PaymentReference.Builder createPaymentReference(CreatePaymentReferenceRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @IsMandatory@");
		}
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @IsMandatory@");
		}
		if(request.getSalesRepresentativeId() <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @IsMandatory@");
		}
		if(request.getCurrencyId() <= 0) {
			throw new AdempiereException("@C_Currency_ID@ @IsMandatory@");
		}
		if(request.getCustomerId() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ @IsMandatory@");
		}
		if(request.getAmount() == null) {
			throw new AdempiereException("@Amount@ @IsMandatory@");
		}
		AtomicReference<PO> refundReference = new AtomicReference<PO>();
		Trx.run(transactionName -> {
			GenericPO refundReferenceToCreate = new GenericPO("C_POSPaymentReference", Env.getCtx(), 0, transactionName);

			if(request.getCustomerBankAccountId() > 0) {
				refundReferenceToCreate.set_ValueOfColumn("C_BP_BankAccount_ID", request.getCustomerBankAccountId());
			}
			refundReferenceToCreate.set_ValueOfColumn("C_BPartner_ID", request.getCustomerId());

			// Currency
			int currencyId = request.getCurrencyId();
			MCurrency currency = MCurrency.get(Env.getCtx(), currencyId);
			if (currency == null || currency.getC_Currency_ID() <= 0) {
				throw new AdempiereException("@C_Currency_ID@ @NotFound@");
			}
			refundReferenceToCreate.set_ValueOfColumn("C_Currency_ID", currencyId);

			//	Amount
			BigDecimal paymentAmount = NumberManager.getBigDecimalFromString(
				request.getAmount()
			);
			if(paymentAmount != null) {
				paymentAmount = paymentAmount.setScale(currency.getStdPrecision(), RoundingMode.HALF_UP);
			}
			refundReferenceToCreate.set_ValueOfColumn("Amount", paymentAmount);

			BigDecimal amountSource = NumberManager.getBigDecimalFromString(
				request.getSourceAmount()
			);
			if(amountSource != null) {
				amountSource = amountSource.setScale(currency.getStdPrecision(), RoundingMode.HALF_UP);
			}
			refundReferenceToCreate.set_ValueOfColumn("AmtSource", amountSource);

			refundReferenceToCreate.set_ValueOfColumn(
				"PayDate",
				TimeUtil.getDay(System.currentTimeMillis())
			);
	
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			if(pos.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID) > 0) {
				refundReferenceToCreate.set_ValueOfColumn("C_ConversionType_ID", pos.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID));
			}
			MOrder salesOrder = OrderUtil.validateAndGetOrder(request.getOrderId(), transactionName);

			//	Throw if not exist conversion
			ConvertUtil.validateConversion(
				salesOrder,
				currencyId,
				pos.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID),
				TimeManager.getDate()
			);
			refundReferenceToCreate.set_ValueOfColumn("C_Order_ID", salesOrder.getC_Order_ID());
			if(request.getPaymentMethodId() > 0) {
				refundReferenceToCreate.set_ValueOfColumn("C_PaymentMethod_ID", request.getPaymentMethodId());
			}
			if(pos.getC_POS_ID() > 0) {
				refundReferenceToCreate.set_ValueOfColumn("C_POS_ID", pos.getC_POS_ID());
			}
			if(request.getSalesRepresentativeId() > 0) {
				refundReferenceToCreate.set_ValueOfColumn("SalesRep_ID", request.getSalesRepresentativeId());
			}
			if (request.getGiftCardId() > 0) {
				GiftCardManagement.processingGiftCard(
						request.getGiftCardId()
				);
				refundReferenceToCreate.set_ValueOfColumn("ECA14_GiftCard_ID", request.getGiftCardId());
			}
			refundReferenceToCreate.set_ValueOfColumn("IsReceipt", request.getIsReceipt());
			refundReferenceToCreate.set_ValueOfColumn("TenderType", request.getTenderTypeCode());
			refundReferenceToCreate.set_ValueOfColumn("Description", request.getDescription());
			refundReferenceToCreate.setAD_Org_ID(salesOrder.getAD_Org_ID());
			refundReferenceToCreate.saveEx(transactionName);
			refundReference.set(refundReferenceToCreate);
		});

		return PaymentConvertUtil.convertPaymentReference(refundReference.get());
	}


	/**
	 * Process a Shipment
	 * @param request
	 * @return
	 * @return Shipment.Builder
	 */
	private Shipment.Builder processShipment(ProcessShipmentRequest request) {
		if(request.getId() <= 0) {
			throw new AdempiereException("@M_InOut_ID@ @NotFound@");
		}
		if(Util.isEmpty(request.getDocumentAction())) {
			throw new AdempiereException("@DocStatus@ @IsMandatory@");
		}
		if(!request.getDocumentAction().equals(MInOut.ACTION_Complete)
				&& !request.getDocumentAction().equals(MInOut.ACTION_Reverse_Accrual)
				&& !request.getDocumentAction().equals(MInOut.ACTION_Reverse_Correct)) {
			throw new AdempiereException("@DocStatus@ @Invalid@");
		}
		AtomicReference<MInOut> shipmentReference = new AtomicReference<MInOut>();
		Trx.run(transactionName -> {
			int shipmentId = request.getId();
			MInOut shipment = new MInOut(Env.getCtx(), shipmentId, transactionName);
			if(shipment.isProcessed()) {
				throw new AdempiereException("@M_InOut_ID@ @Processed@");
			}
			if (!shipment.processIt(request.getDocumentAction())) {
				log.warning("@ProcessFailed@ :" + shipment.getProcessMsg());
				throw new AdempiereException("@ProcessFailed@ :" + shipment.getProcessMsg());
			}
			shipment.saveEx(transactionName);
			shipmentReference.set(shipment);
		});
		//	Default
		return ConvertUtil.convertShipment(shipmentReference.get());
	}
	
	/**
	 * List RMA Lines from RMA ID
	 * @param request
	 * @return
	 */
	private ListRMALinesResponse.Builder listRMALines(ListRMALinesRequest request) {
		if(request.getRmaId() <= 0) {
			throw new AdempiereException("@M_RMA_ID@ @NotFound@");
		}
		MOrder rma = new MOrder(Env.getCtx(), request.getRmaId(), null);
		MPOS pos = new MPOS(Env.getCtx(), rma.getC_POS_ID(), null);
		ListRMALinesResponse.Builder builder = ListRMALinesResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		StringBuffer whereClause = new StringBuffer(I_C_OrderLine.COLUMNNAME_C_Order_ID + " = ?");
		List<Object> parameters = new ArrayList<>();
		parameters.add(request.getRmaId());
		if(pos.get_ValueAsInt("DefaultDiscountCharge_ID") > 0) {
			parameters.add(pos.get_ValueAsInt("DefaultDiscountCharge_ID"));
			whereClause.append(" AND (C_Charge_ID IS NULL OR C_Charge_ID <> ?)");
		}
		//	Get Product list
		Query query = new Query(Env.getCtx(), I_C_OrderLine.Table_Name, whereClause.toString(), null)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true);
		int count = query.count();
		query
		.setLimit(limit, offset)
		.setOrderBy(I_C_OrderLine.COLUMNNAME_Line)
		.<MOrderLine>list()
		.forEach(rmaLine -> {
			builder.addRmaLines(ConvertUtil.convertRMALine(rmaLine));
		});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}

	/**
	 * List shipment Lines from Order UUID
	 * @param request
	 * @return
	 */
	private ListShipmentLinesResponse.Builder listShipmentLines(ListShipmentLinesRequest request) {
		if(request.getShipmentId() <= 0) {
			throw new AdempiereException("@M_InOut_ID@ @NotFound@");
		}
		ListShipmentLinesResponse.Builder builder = ListShipmentLinesResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		int shipmentId = request.getShipmentId();
		String whereClause = I_M_InOutLine.COLUMNNAME_M_InOut_ID + " = ? AND " +
				I_M_InOutLine.COLUMNNAME_M_Product_ID + " > 0";
		//	Get Product list
		Query query = new Query(Env.getCtx(), I_M_InOutLine.Table_Name, whereClause, null)
				.setParameters(shipmentId)
				.setClient_ID()
				.setOnlyActiveRecords(true);
		int count = query.count();
		query
		.setLimit(limit, offset)
		.<MInOutLine>list()
		.forEach(line -> {
			ShipmentLine.Builder shipmenLinetBuilder = POSConvertUtil.convertShipmentLine(line);
			builder.addShipmentLines(shipmenLinetBuilder);
		});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}
	
	/**
	 * List refund references from Order UUID
	 * @param request
	 * @return
	 */
	private ListPaymentReferencesResponse.Builder listPaymentReferencesLines(ListPaymentReferencesRequest request) {
		if(request.getCustomerId() <= 0 && request.getPosId() <= 0 && request.getOrderId() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ / @C_Order_ID@ @IsMandatory@");
		}
		ListPaymentReferencesResponse.Builder builder = ListPaymentReferencesResponse.newBuilder();
		if(MTable.get(Env.getCtx(), "C_POSPaymentReference") == null) {
			return builder;
		}
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		List<Object> parameters = new ArrayList<Object>();
		StringBuilder whereClause = new StringBuilder("(IsPaid = 'N' AND Processed = 'N' OR IsKeepReferenceAfterProcess = 'Y')");
		if(request.getOrderId() > 0) {
			parameters.add(request.getOrderId());
			whereClause.append(" AND C_Order_ID = ?");
		} else if(request.getCustomerId() > 0) {
			parameters.add(request.getCustomerId());
			whereClause.append(" AND EXISTS(SELECT 1 FROM C_BP_BankAccount ba WHERE ba.C_BP_BankAccount_ID = C_POSPaymentReference.C_BP_BankAccount_ID AND ba.C_BPartner_ID = ?)");
		} else if(request.getPosId() > 0) {
			parameters.add(request.getPosId());
			whereClause.append(" AND C_POS_ID = ?");
		}
		//	Get Refund Reference list
		Query query = new Query(Env.getCtx(), "C_POSPaymentReference", whereClause.toString(), null)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true);
		int count = query.count();
		query
		.setLimit(limit, offset)
		.list()
		.forEach(refundReference -> {
			PaymentReference.Builder refundReferenceBuilder = PaymentConvertUtil.convertPaymentReference(
				refundReference
			);
			builder.addPaymentReferences(refundReferenceBuilder);
		});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}


	/**
	 * Delete shipment line from uuid
	 * @param request
	 * @return
	 */
	private Empty.Builder deleteShipmentLine(DeleteShipmentLineRequest request) {
		if(request.getId() <= 0) {
			throw new AdempiereException("@M_InOutLine_ID@ @NotFound@");
		}
		Trx.run(transactionName -> {
			MInOutLine shipmentLine = new Query(Env.getCtx(), I_M_InOutLine.Table_Name, I_M_InOutLine.COLUMNNAME_M_InOutLine_ID + " = ?", transactionName)
					.setParameters(request.getId())
					.setClient_ID()
					.first();
			if(shipmentLine != null
					&& shipmentLine.getM_InOutLine_ID() != 0) {
				//	Validate processed Order
				if(shipmentLine.isProcessed()) {
					throw new AdempiereException("@M_InOutLine_ID@ @Processed@");
				}
				if(shipmentLine != null
						&& shipmentLine.getM_InOutLine_ID() >= 0) {
					shipmentLine.deleteEx(true);
				}
			}
		});
		//	Return
		return Empty.newBuilder();
	}
	
	/**
	 * Create shipment line and return this
	 * @param request
	 * @return
	 */
	private ShipmentLine.Builder createAndConvertShipmentLine(CreateShipmentLineRequest request) {

		//	Validate Order
		if(request.getShipmentId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_InOut_ID@");
		}
		//	Validate Product and charge
		if(request.getOrderLineId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_OrderLine_ID@");
		}

		MInOut shipmentHeader = new MInOut(Env.getCtx(), request.getShipmentId(), null);
		if (shipmentHeader == null || shipmentHeader.getM_InOut_ID() <= 0) {
			throw new AdempiereException("@M_InOut_ID@ @NotFound@");
		}
		if(!DocumentUtil.isDrafted(shipmentHeader)) {
			throw new AdempiereException("@M_InOut_ID@ @Processed@");
		}
		//	Quantity
		MOrderLine salesOrderLine = new MOrderLine(Env.getCtx(), request.getOrderLineId(), null);
		if (salesOrderLine == null || salesOrderLine.getC_OrderLine_ID() <= 0) {
			throw new AdempiereException("@C_OrderLine_ID@ @NotFound@");
		}
		Optional<MInOutLine> maybeOrderLine = Arrays.asList(shipmentHeader.getLines(true))
			.parallelStream()
			.filter(shipmentLineTofind -> {
				return shipmentLineTofind.getC_OrderLine_ID() == salesOrderLine.getC_OrderLine_ID();
			})
			.findFirst()
		;
		AtomicReference<MInOutLine> shipmentLineReference = new AtomicReference<MInOutLine>();
		BigDecimal quantity = NumberManager.getBigDecimalFromString(
			request.getQuantity()
		);

		//	Validate available
		if(salesOrderLine.getQtyOrdered().subtract(salesOrderLine.getQtyDelivered()).compareTo(Optional.ofNullable(quantity).orElse(Env.ONE)) < 0) {
			throw new AdempiereException("@QtyInsufficient@");
		}
		if(maybeOrderLine.isPresent()) {
			MInOutLine shipmentLine = maybeOrderLine.get();
			if(shipmentLine.isProcessed()) {
				throw new AdempiereException("@M_InOutLine_ID@ @Processed@");
			}
			BigDecimal availableQuantity = ShipmentUtil.getAvailableQuantityForShipment(
				salesOrderLine.getC_OrderLine_ID(),
				shipmentLine.getM_InOutLine_ID(),
				salesOrderLine.getQtyEntered(),
				quantity
			);
			if (availableQuantity.compareTo(Env.ZERO) <= 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			//	Set Quantity
			BigDecimal quantityToOrder = quantity;
			if(quantity == null) {
				quantityToOrder = shipmentLine.getQtyEntered();
				quantityToOrder = quantityToOrder.add(Env.ONE);
			}
			if (availableQuantity.compareTo(quantityToOrder) < 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			//	Validate available
			if(salesOrderLine.getQtyOrdered().subtract(salesOrderLine.getQtyDelivered()).compareTo(quantityToOrder) < 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			//	Update movement quantity
			updateUomAndQuantityForShipment(shipmentLine, salesOrderLine.getC_UOM_ID(), quantityToOrder);
			shipmentLine.saveEx();
			shipmentLineReference.set(shipmentLine);
		} else {
			MInOutLine shipmentLine = new MInOutLine(shipmentHeader);
			BigDecimal availableQuantity = ShipmentUtil.getAvailableQuantityForShipment(
				salesOrderLine.getC_OrderLine_ID(),
				shipmentLine.getM_InOutLine_ID(),
				salesOrderLine.getQtyEntered(),
				quantity
			);
			if (availableQuantity.compareTo(Env.ZERO) <= 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			BigDecimal quantityToOrder = quantity;
			if (quantity == null) {
				quantityToOrder = Env.ONE;
			}
			if (availableQuantity.compareTo(quantityToOrder) < 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			//create new line
			shipmentLine.setOrderLine(salesOrderLine, 0, quantityToOrder);
			Optional.ofNullable(request.getDescription()).ifPresent(description -> shipmentLine.setDescription(description));
			//	Update movement quantity
			updateUomAndQuantityForShipment(shipmentLine, salesOrderLine.getC_UOM_ID(), quantityToOrder);
			//	Save Line
			shipmentLine.saveEx();
			shipmentLineReference.set(shipmentLine);
		}
		//	Convert Line
		return POSConvertUtil.convertShipmentLine(
			shipmentLineReference.get()
		);
	}

	/**
	 * Create  from request
	 * @param context
	 * @param request
	 * @return
	 */
	private Shipment.Builder createShipment(CreateShipmentRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		AtomicReference<MInOut> maybeShipment = new AtomicReference<MInOut>();
		Trx.run(transactionName -> {
			int orderId = request.getOrderId();
			int salesRepresentativeId = request.getSalesRepresentativeId();
			if(orderId <= 0) {
				throw new AdempiereException("@C_Order_ID@ @NotFound@");
			}
			if(salesRepresentativeId <= 0) {
				throw new AdempiereException("@SalesRep_ID@ @NotFound@");
			}
			MOrder salesOrder = new MOrder(Env.getCtx(), orderId, transactionName);
			if(!OrderUtil.isValidOrder(salesOrder)) {
				throw new AdempiereException("@ActionNotAllowedHere@");
			}
			if(salesOrder.isDelivered()) {
				throw new AdempiereException("@C_Order_ID@ @IsDelivered@");
			}
			//	Valid if has a Order
			if(!DocumentUtil.isCompleted(salesOrder)) {
				throw new AdempiereException("@Invalid@ @C_Order_ID@ " + salesOrder.getDocumentNo());
			}
			//	
			MInOut shipment = new Query(Env.getCtx(), I_M_InOut.Table_Name, 
					"DocStatus = 'DR' "
					+ "AND C_Order_ID = ? "
					+ "AND SalesRep_ID = ?", transactionName)
					.setParameters(salesOrder.getC_Order_ID(), salesRepresentativeId)
					.first();
			//	Validate
			if(shipment == null) {
				shipment = new MInOut(salesOrder, 0, TimeManager.getDate());
				shipment.setMovementType(MInOut.MOVEMENTTYPE_CustomerShipment);
			} else {
				shipment.setDateOrdered(TimeManager.getDate());
				shipment.setDateAcct(TimeManager.getDate());
				shipment.setDateReceived(TimeManager.getDate());
				shipment.setMovementDate(TimeManager.getDate());
			}
			//	Default values
			shipment.setC_Order_ID(orderId);
			shipment.setSalesRep_ID(salesRepresentativeId);
			shipment.setC_POS_ID(salesOrder.getC_POS_ID());
			shipment.saveEx(transactionName);
			maybeShipment.set(shipment);
			if (request.getIsCreateLinesFromOrder()) {
				createShipmentLines(shipment, transactionName);
			}
		});
		//	Convert order
		return ConvertUtil.convertShipment(maybeShipment.get());
	}
	
	private void createShipmentLines(MInOut shipmentHeader, String transactionName) {
		MOrder order = new MOrder(Env.getCtx(), shipmentHeader.getC_Order_ID(), transactionName);
		List<MOrderLine> orderLines = Arrays.asList(order.getLines());

		orderLines.forEach(salesOrderLine -> {
			if(!DocumentUtil.isDrafted(shipmentHeader)) {
				throw new AdempiereException("@M_InOut_ID@ @Processed@");
			}
			if (salesOrderLine.getM_Product_ID() == 0) {
				return;
			}
			//	Quantity
			Optional<MInOutLine> maybeOrderLine = Arrays.asList(shipmentHeader.getLines(true))
				.parallelStream()
				.filter(shipmentLineTofind -> shipmentLineTofind.getC_OrderLine_ID() == salesOrderLine.getC_OrderLine_ID())
				.findFirst()
			;

			AtomicReference<MInOutLine> shipmentLineReference = new AtomicReference<MInOutLine>();
			BigDecimal quantity = salesOrderLine.getQtyEntered();
			//	Validate available
			if(salesOrderLine.getQtyOrdered().subtract(salesOrderLine.getQtyDelivered()).compareTo(Optional.ofNullable(quantity).orElse(Env.ONE)) < 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			if(maybeOrderLine.isPresent()) {
				MInOutLine shipmentLine = maybeOrderLine.get();
				//	Set Quantity
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = shipmentLine.getMovementQty();
					quantityToOrder = quantityToOrder.add(Env.ONE);
				}
				//	Validate available
				if(salesOrderLine.getQtyOrdered().subtract(salesOrderLine.getQtyDelivered()).compareTo(quantityToOrder) < 0) {
					throw new AdempiereException("@QtyInsufficient@");
				}
				//	Update movement quantity
				updateUomAndQuantityForShipment(shipmentLine, salesOrderLine.getC_UOM_ID(), quantityToOrder);
				shipmentLine.saveEx();
				shipmentLineReference.set(shipmentLine);
			} else {
				MInOutLine shipmentLine = new MInOutLine(shipmentHeader);
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = Env.ONE;
				}
				//create new line
				shipmentLine.setOrderLine(salesOrderLine, 0, quantityToOrder);
				Optional.ofNullable(salesOrderLine.getDescription()).ifPresent(description -> shipmentLine.setDescription(description));
				//	Update movement quantity
				updateUomAndQuantityForShipment(shipmentLine, salesOrderLine.getC_UOM_ID(), quantityToOrder);
				//	Save Line
				shipmentLine.saveEx();
				shipmentLineReference.set(shipmentLine);
			}
		});
	}



	@Override
	public void createCustomer(CreateCustomerRequest request, StreamObserver<Customer> responseObserver) {
		try {
			Customer.Builder customer = createCustomer(Env.getCtx(), request);
			responseObserver.onNext(customer.build());
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

	/**
	 * Create Customer
	 * @param request
	 * @return
	 */
	private Customer.Builder createCustomer(Properties context, CreateCustomerRequest request) {
		//	Validate name
		if(Util.isEmpty(request.getName(), true)) {
			throw new AdempiereException("@Name@ @IsMandatory@");
		}
		final int clientId = Env.getAD_Client_ID(Env.getCtx());
		//	POS Uuid
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		MBPartner businessPartner = MBPartner.getTemplate(context, clientId, pos.getC_POS_ID());

		//	Validate Template
		int customerTemplateId = request.getCustomerTemplateId();
		if (customerTemplateId <= 0) {
			customerTemplateId = pos.getC_BPartnerCashTrx_ID();
		}
		if(customerTemplateId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BPartnerCashTrx_ID@");
		}
		MBPartner template = MBPartner.get(context, customerTemplateId);
		if (template == null || template.getC_BPartner_ID() <= 0) {
			throw new AdempiereException("@C_BPartnerCashTrx_ID@ @NotFound@");
		}

		// copy and clear values by termplate
		PO.copyValues(template, businessPartner);
		businessPartner.setTaxID("");
		businessPartner.setValue("");
		businessPartner.setNAICS(null);
		businessPartner.setName("");
		businessPartner.setName2(null);
		businessPartner.setDUNS("");
		businessPartner.setIsActive(true);

		Optional<MBPartnerLocation> maybeTemplateLocation = Arrays.asList(template.getLocations(false))
			.stream()
			.findFirst()
		;
		if(!maybeTemplateLocation.isPresent()) {
			throw new AdempiereException("@C_BPartnerCashTrx_ID@ @C_BPartner_Location_ID@ @NotFound@");
		}
		//	Get location from template
		MLocation templateLocation = maybeTemplateLocation.get().getLocation(false);
		if(templateLocation == null
				|| templateLocation.getC_Location_ID() <= 0) {
			throw new AdempiereException("@C_Location_ID@ @NotFound@");
		}
		Trx.run(transactionName -> {
			//	Create it
			businessPartner.setAD_Org_ID(0);
			businessPartner.setIsCustomer (true);
			businessPartner.setIsVendor (false);
			businessPartner.set_TrxName(transactionName);
			//	Set Value
			String code = request.getValue();
			if(Util.isEmpty(code, true)) {
				code = DB.getDocumentNo(clientId, I_C_BPartner.Table_Name, transactionName, businessPartner);
			}
			businessPartner.setValue(code);
			//	Tax Id
			Optional.ofNullable(request.getTaxId()).ifPresent(value -> businessPartner.setTaxID(value));
			//	Duns
			Optional.ofNullable(request.getDuns()).ifPresent(value -> businessPartner.setDUNS(value));
			//	Naics
			Optional.ofNullable(request.getNaics()).ifPresent(value -> businessPartner.setNAICS(value));
			//	Name
			Optional.ofNullable(request.getName()).ifPresent(value -> businessPartner.setName(value));
			//	Last name
			Optional.ofNullable(request.getLastName()).ifPresent(value -> businessPartner.setName2(value));
			//	Description
			Optional.ofNullable(request.getDescription()).ifPresent(value -> businessPartner.setDescription(value));
			//	Business partner group
			if(request.getBusinessPartnerGroupId() > 0) {
				int businessPartnerGroupId = request.getBusinessPartnerGroupId();
				if(businessPartnerGroupId != 0) {
					businessPartner.setC_BP_Group_ID(businessPartnerGroupId);
				}
			}
			//	Additional attributes
			setAdditionalAttributes(businessPartner, request.getAdditionalAttributes().getFieldsMap());
			//	Save it
			businessPartner.saveEx(transactionName);
			
			// clear price list from business partner group
			if (businessPartner.getM_PriceList_ID() > 0) {
				businessPartner.setM_PriceList_ID(0);
				businessPartner.saveEx(transactionName);
			}
			
			//	Location
			request.getAddressesList().forEach(address -> {
				createCustomerAddress(businessPartner, address, templateLocation, transactionName);
			});
		});
		//	Default return
		return POSConvertUtil.convertCustomer(
			businessPartner
		);
	}
	
	/**
	 * Set additional attributes
	 * @param entity
	 * @param attributes
	 * @return void
	 */
	private void setAdditionalAttributes(PO entity, Map<String, Value> attributes) {
		if(attributes != null) {
			attributes.keySet().forEach(key -> {
				Value attribute = attributes.get(key);
				int referenceId = getReferenceId(entity.get_Table_ID(), key);
				Object value = null;
				if(referenceId > 0) {
					value = ValueManager.getObjectFromReference(attribute, referenceId);
				} 
				if(value == null) {
					value = ValueManager.getObjectFromValue(attribute);
				}
				entity.set_ValueOfColumn(key, value);
			});
		}
	}
	
	/**
	 * Get reference from column name and table
	 * @param tableId
	 * @param columnName
	 * @return
	 */
	private int getReferenceId(int tableId, String columnName) {
		MColumn column = MTable.get(Env.getCtx(), tableId).getColumn(columnName);
		if(column == null) {
			return -1;
		}
		return column.getAD_Reference_ID();
	}
	
	/**
	 * Create Address from customer and address request
	 * @param customer
	 * @param address
	 * @param templateLocation
	 * @param transactionName
	 * @return void
	 */
	private void createCustomerAddress(MBPartner customer, AddressRequest address, MLocation templateLocation, String transactionName) {
		int countryId = address.getCountryId();
		//	Instance it
		MLocation location = new MLocation(Env.getCtx(), 0, transactionName);
		if(countryId > 0) {
			int regionId = address.getRegionId();
			int cityId = address.getCityId();
			String cityName = null;
			//	City Name
			if(!Util.isEmpty(address.getCityName())) {
				cityName = address.getCityName();
			}
			location.setC_Country_ID(countryId);
			location.setC_Region_ID(regionId);
			location.setCity(cityName);
			if(cityId > 0) {
				location.setC_City_ID(cityId);
			}
		} else {
			//	Copy
			PO.copyValues(templateLocation, location);
		}
		//	Postal Code
		if(!Util.isEmpty(address.getPostalCode())) {
			location.setPostal(address.getPostalCode());
		}
		//	Address
		Optional.ofNullable(address.getAddress1()).ifPresent(addressValue -> location.setAddress1(addressValue));
		Optional.ofNullable(address.getAddress2()).ifPresent(addressValue -> location.setAddress2(addressValue));
		Optional.ofNullable(address.getAddress3()).ifPresent(addressValue -> location.setAddress3(addressValue));
		Optional.ofNullable(address.getAddress4()).ifPresent(addressValue -> location.setAddress4(addressValue));
		Optional.ofNullable(address.getPostalCode()).ifPresent(postalCode -> location.setPostal(postalCode));
		//	
		location.saveEx(transactionName);
		//	Create BP location
		MBPartnerLocation businessPartnerLocation = new MBPartnerLocation(customer);
		businessPartnerLocation.setC_Location_ID(location.getC_Location_ID());
		//	Default
		businessPartnerLocation.setIsBillTo(address.getIsDefaultBilling());
		businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultBilling, address.getIsDefaultBilling());
		businessPartnerLocation.setIsShipTo(address.getIsDefaultShipping());
		businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultShipping, address.getIsDefaultShipping());
		Optional.ofNullable(address.getContactName()).ifPresent(contact -> businessPartnerLocation.setContactPerson(contact));
		Optional.ofNullable(address.getLocationName()).ifPresent(locationName -> businessPartnerLocation.setName(locationName));
		Optional.ofNullable(address.getEmail()).ifPresent(email -> businessPartnerLocation.setEMail(email));
		Optional.ofNullable(address.getPhone()).ifPresent(phome -> businessPartnerLocation.setPhone(phome));
		Optional.ofNullable(address.getDescription()).ifPresent(description -> businessPartnerLocation.setDescription(description));
		if(Util.isEmpty(businessPartnerLocation.getName())) {
			businessPartnerLocation.setName(".");
		}
		//	Additional attributes
		setAdditionalAttributes(businessPartnerLocation, address.getAdditionalAttributes().getFieldsMap());
		businessPartnerLocation.saveEx(transactionName);
		//	Contact
		if(!Util.isEmpty(address.getContactName()) || !Util.isEmpty(address.getEmail()) || !Util.isEmpty(address.getPhone())) {
			MUser contact = new MUser(customer);
			Optional.ofNullable(address.getEmail()).ifPresent(email -> contact.setEMail(email));
			Optional.ofNullable(address.getPhone()).ifPresent(phome -> contact.setPhone(phome));
			Optional.ofNullable(address.getDescription()).ifPresent(description -> contact.setDescription(description));
			String contactName = address.getContactName();
			if(Util.isEmpty(contactName)) {
				contactName = address.getEmail();
			}
			if(Util.isEmpty(contactName)) {
				contactName = address.getPhone();
			}
			contact.setName(contactName);
			//	Save
			contact.setC_BPartner_Location_ID(businessPartnerLocation.getC_BPartner_Location_ID());
			contact.saveEx(transactionName);
 		}
	}



	@Override
	public void updateCustomer(UpdateCustomerRequest request, StreamObserver<Customer> responseObserver) {
		try {
			Customer.Builder customer = updateCustomer(request);
			responseObserver.onNext(customer.build());
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

	/**
	 * update Customer
	 * @param request
	 * @return
	 */
	private Customer.Builder updateCustomer(UpdateCustomerRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if(!AccessManagement.getBooleanValueFromPOS(pos, Env.getAD_User_ID(Env.getCtx()), ColumnsAdded.COLUMNNAME_IsAllowsModifyCustomer)) {
			throw new AdempiereException("@POS.ModifyCustomerNotAllowed@");
		}
		//	Customer Uuid
		if(request.getId() <= 0) {
			throw new AdempiereException("@C_BPartner_ID@ @IsMandatory@");
		}
		//	
		AtomicReference<MBPartner> customer = new AtomicReference<MBPartner>();
		Trx.run(transactionName -> {
			//	Create it
			MBPartner businessPartner = MBPartner.get(Env.getCtx(), request.getId());
			if(businessPartner == null) {
				throw new AdempiereException("@C_BPartner_ID@ @NotFound@");
			}
			if(businessPartner.getC_BPartner_ID() == pos.getC_BPartnerCashTrx_ID()) {
				throw new AdempiereException("@POS.ModifyTemplateCustomerNotAllowed@");
			}
			businessPartner.set_TrxName(transactionName);
			//	Set Value
			Optional.ofNullable(request.getValue()).ifPresent(value -> businessPartner.setValue(value));
			//	Tax Id
			Optional.ofNullable(request.getTaxId()).ifPresent(value -> businessPartner.setTaxID(value));
			//	Duns
			Optional.ofNullable(request.getDuns()).ifPresent(value -> businessPartner.setDUNS(value));
			//	Naics
			Optional.ofNullable(request.getNaics()).ifPresent(value -> businessPartner.setNAICS(value));
			//	Name
			Optional.ofNullable(request.getName()).ifPresent(value -> businessPartner.setName(value));
			//	Last name
			Optional.ofNullable(request.getLastName()).ifPresent(value -> businessPartner.setName2(value));
			//	Description
			Optional.ofNullable(request.getDescription()).ifPresent(value -> businessPartner.setDescription(value));
			//	Additional attributes
			setAdditionalAttributes(businessPartner, request.getAdditionalAttributes().getFieldsMap());
			//	Save it
			businessPartner.saveEx(transactionName);
			//	Location
			request.getAddressesList().forEach(address -> {
				int countryId = address.getCountryId();
				//	
				int regionId = address.getRegionId();
				String cityName = null;
				int cityId = address.getCityId();
				//	City Name
				if(!Util.isEmpty(address.getCityName())) {
					cityName = address.getCityName();
				}
				//	Validate it
				if(countryId > 0
						|| regionId > 0
						|| cityId > 0
						|| !Util.isEmpty(cityName)) {
					//	Find it
					Optional<MBPartnerLocation> maybeCustomerLocation = Arrays.asList(businessPartner.getLocations(true))
						.parallelStream()
						.filter(customerLocation -> customerLocation.getC_BPartner_Location_ID() == address.getId())
						.findFirst()
					;
					if(maybeCustomerLocation.isPresent()) {
						MBPartnerLocation businessPartnerLocation = maybeCustomerLocation.get();
						MLocation location = businessPartnerLocation.getLocation(true);
						location.set_TrxName(transactionName);
						if(countryId > 0) {
							location.setC_Country_ID(countryId);
						}
						if(regionId > 0) {
							location.setC_Region_ID(regionId);
						}
						if(cityId > 0) {
							location.setC_City_ID(cityId);
						}
						Optional.ofNullable(cityName).ifPresent(city -> location.setCity(city));
						//	Address
						Optional.ofNullable(address.getAddress1()).ifPresent(addressValue -> location.setAddress1(addressValue));
						Optional.ofNullable(address.getAddress2()).ifPresent(addressValue -> location.setAddress2(addressValue));
						Optional.ofNullable(address.getAddress3()).ifPresent(addressValue -> location.setAddress3(addressValue));
						Optional.ofNullable(address.getAddress4()).ifPresent(addressValue -> location.setAddress4(addressValue));
						Optional.ofNullable(address.getPostalCode()).ifPresent(postalCode -> location.setPostal(postalCode));
						//	Save
						location.saveEx(transactionName);
						//	Update business partner location
						businessPartnerLocation.setIsBillTo(address.getIsDefaultBilling());
						businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultBilling, address.getIsDefaultBilling());
						businessPartnerLocation.setIsShipTo(address.getIsDefaultShipping());
						businessPartnerLocation.set_ValueOfColumn(VueStoreFrontUtil.COLUMNNAME_IsDefaultShipping, address.getIsDefaultShipping());
						Optional.ofNullable(address.getContactName()).ifPresent(contact -> businessPartnerLocation.setContactPerson(contact));
						Optional.ofNullable(address.getLocationName()).ifPresent(locationName -> businessPartnerLocation.setName(locationName));
						Optional.ofNullable(address.getEmail()).ifPresent(email -> businessPartnerLocation.setEMail(email));
						Optional.ofNullable(address.getPhone()).ifPresent(phome -> businessPartnerLocation.setPhone(phome));
						Optional.ofNullable(address.getDescription()).ifPresent(description -> businessPartnerLocation.setDescription(description));
						//	Additional attributes
						setAdditionalAttributes(businessPartnerLocation, address.getAdditionalAttributes().getFieldsMap());
						businessPartnerLocation.saveEx(transactionName);
						//	Contact
						AtomicReference<MUser> contactReference = new AtomicReference<MUser>(getOfBusinessPartnerLocation(businessPartnerLocation, transactionName));
						if(contactReference.get() == null
								|| contactReference.get().getAD_User_ID() <= 0) {
							contactReference.set(new MUser(businessPartner));
						}
						if(!Util.isEmpty(address.getContactName()) || !Util.isEmpty(address.getEmail()) || !Util.isEmpty(address.getPhone())) {
							MUser contact = contactReference.get();
							Optional.ofNullable(address.getEmail()).ifPresent(email -> contact.setEMail(email));
							Optional.ofNullable(address.getPhone()).ifPresent(phome -> contact.setPhone(phome));
							Optional.ofNullable(address.getDescription()).ifPresent(description -> contact.setDescription(description));
							String contactName = address.getContactName();
							if(Util.isEmpty(contactName)) {
								contactName = address.getEmail();
							}
							if(Util.isEmpty(contactName)) {
								contactName = address.getPhone();
							}
							contact.setName(contactName);
							//	Save
							contact.setC_BPartner_Location_ID(businessPartnerLocation.getC_BPartner_Location_ID());
							contact.saveEx(transactionName);
				 		}
					} else {
						//	Create new
						Optional<MBPartnerLocation> maybeTemplateLocation = Arrays.asList(businessPartner.getLocations(false)).stream().findFirst();
						if(!maybeTemplateLocation.isPresent()) {
							throw new AdempiereException("@C_BPartnerCashTrx_ID@ @C_BPartner_Location_ID@ @NotFound@");
						}
						//	Get location from template
						MLocation templateLocation = maybeTemplateLocation.get().getLocation(false);
						if(templateLocation == null
								|| templateLocation.getC_Location_ID() <= 0) {
							throw new AdempiereException("@C_Location_ID@ @NotFound@");
						}
						createCustomerAddress(businessPartner, address, templateLocation, transactionName);
					}
					customer.set(businessPartner);
				}
			});
		});
		//	Default return
		return POSConvertUtil.convertCustomer(
			customer.get()
		);
	}
	
	/**
	 * 
	 * @param businessPartnerLocation
	 * @param transactionName
	 * @return
	 * @return MUser
	 */
	private MUser getOfBusinessPartnerLocation(MBPartnerLocation businessPartnerLocation, String transactionName) {
		return new Query(businessPartnerLocation.getCtx(), MUser.Table_Name, "C_BPartner_Location_ID = ?", transactionName)
				.setParameters(businessPartnerLocation.getC_BPartner_Location_ID())
				.first();
	}
	
	/**
	 * List Warehouses from POS UUID
	 * @param request
	 * @return
	 */
	private ListAvailableWarehousesResponse.Builder listWarehouses(ListAvailableWarehousesRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

		ListAvailableWarehousesResponse.Builder builder = ListAvailableWarehousesResponse.newBuilder();
		final String TABLE_NAME = "C_POSWarehouseAllocation";
		if(MTable.getTable_ID(TABLE_NAME) <= 0) {
			return builder;
		}

		String whereClause = "C_POS_ID = ? ";
		ArrayList<Object> filtersList = new ArrayList<Object>();
		filtersList.add(pos.getC_POS_ID());

		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			TABLE_NAME,
			whereClause,
			null
		)
			.setParameters(filtersList)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo)
		;
		int count = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		query
			.setLimit(limit, offset)
			.list()
			.forEach(availableWarehouse -> {
				MWarehouse warehouse = MWarehouse.get(Env.getCtx(), availableWarehouse.get_ValueAsInt("M_Warehouse_ID"));
				builder.addWarehouses(
					AvailableWarehouse.newBuilder()
						.setId(
							warehouse.getM_Warehouse_ID()
						)
						.setKey(
							StringManager.getValidString(
								warehouse.getValue()
							)
						)
						.setName(
							StringManager.getValidString(
								warehouse.getName()
							)
						)
						.setIsPosRequiredPin(
							availableWarehouse.get_ValueAsBoolean(I_C_POS.COLUMNNAME_IsPOSRequiredPIN)
						)
				);
			})
		;

		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;
		return builder;
	}
	
	/**
	 * List Price List from POS UUID
	 * @param request
	 * @return
	 */
	private ListAvailablePriceListResponse.Builder listPriceList(ListAvailablePriceListRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

		ListAvailablePriceListResponse.Builder builder = ListAvailablePriceListResponse.newBuilder();
		final String TABLE_NAME = "C_POSPriceListAllocation";
		if(MTable.getTable_ID(TABLE_NAME) <= 0) {
			return builder;
		}
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Dynamic where clause
		Query query = new Query(
			Env.getCtx(),
			TABLE_NAME,
			"C_POS_ID = ?",
			null
		)
			.setParameters(pos.getC_POS_ID())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo)
		;
		int count = query.count();
		query
		.setLimit(limit, offset)
		.list()
		.forEach(availablePriceList -> {
			MPriceList priceList = MPriceList.get(Env.getCtx(), availablePriceList.get_ValueAsInt("M_PriceList_ID"), null);
			builder.addPriceList(
				AvailablePriceList.newBuilder()
					.setId(
						priceList.getM_PriceList_ID()
					)
					.setKey(
						StringManager.getValidString(
							priceList.getName()
						)
					)
					.setName(
						StringManager.getValidString(
							priceList.getName()
						)
					)
					.setIsPosRequiredPin(
						availablePriceList.get_ValueAsBoolean(I_C_POS.COLUMNNAME_IsPOSRequiredPIN)
					)
			);
		});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}
	
	/**
	 * List tender Types from POS UUID
	 * @param request
	 * @return
	 */
	private ListAvailablePaymentMethodsResponse.Builder listPaymentMethods(ListAvailablePaymentMethodsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		ListAvailablePaymentMethodsResponse.Builder builder = ListAvailablePaymentMethodsResponse.newBuilder();
		final String TABLE_NAME = "C_POSPaymentTypeAllocation";
		if(MTable.getTable_ID(TABLE_NAME) <= 0) {
			return builder;
		}
		final String PAYMENT_METHOD_TABLE_NAME = "C_PaymentMethod";
		MTable paymentTypeTable = MTable.get(Env.getCtx(), PAYMENT_METHOD_TABLE_NAME);
		if (paymentTypeTable == null || paymentTypeTable.getAD_Table_ID() <= 0) {
			return builder;
		}

		//	Dynamic where clause
		//	Aisle Seller
		int posId = request.getPosId();
		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			TABLE_NAME,
			" C_POS_ID = ? AND IsDisplayedFromCollection = 'Y' ",
			null
		)
			.setParameters(posId)
			.setOnlyActiveRecords(true)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo)
		;

		// Pagination
		int count = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		query
			.setLimit(limit, offset)
			.list()
			.forEach(availablePaymentMethod -> {
				AvailablePaymentMethod.Builder tenderTypeValue = POSConvertUtil.convertPaymentMethod(availablePaymentMethod);
				builder.addPaymentMethods(tenderTypeValue);
			})
		;
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}
	
	/**
	 * List Document Types from POS UUID
	 * @param request
	 * @return
	 */
	private ListAvailableDocumentTypesResponse.Builder listDocumentTypes(ListAvailableDocumentTypesRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		ListAvailableDocumentTypesResponse.Builder builder = ListAvailableDocumentTypesResponse.newBuilder();
		final String TABLE_NAME = "C_POSDocumentTypeAllocation";
		if(MTable.getTable_ID(TABLE_NAME) <= 0) {
			return builder;
		}
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Dynamic where clause
		//	Aisle Seller
		int posId = request.getPosId();
		//	Get Product list
		Query query = new Query(Env.getCtx(), TABLE_NAME, "C_POS_ID = ?", null)
				.setParameters(posId)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo);
		int count = query.count();
		query
		.setLimit(limit, offset)
		.list()
		.forEach(availableDocumentType -> {
			MDocType documentType = MDocType.get(Env.getCtx(), availableDocumentType.get_ValueAsInt("C_DocType_ID"));
			builder.addDocumentTypes(AvailableDocumentType.newBuilder()
				.setId(
					documentType.getC_DocType_ID()
				)
				.setKey(
					StringManager.getValidString(
						documentType.getName()
					)
				)
				.setName(
					StringManager.getValidString(
						documentType.getPrintName()
					)
				)
				.setIsPosRequiredPin(
					availableDocumentType.get_ValueAsBoolean(I_C_POS.COLUMNNAME_IsPOSRequiredPIN)
				)
			);
		});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}
	
	/**
	 * List Currencies from POS UUID
	 * @param request
	 * @return
	 */
	private ListAvailableCurrenciesResponse.Builder listCurrencies(ListAvailableCurrenciesRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		ListAvailableCurrenciesResponse.Builder builder = ListAvailableCurrenciesResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Dynamic where clause
		String whereClause = "EXISTS(SELECT 1 FROM C_Conversion_Rate cr "
				+ "WHERE (cr.C_Currency_ID = C_Currency.C_Currency_ID  OR cr.C_Currency_ID_To = C_Currency.C_Currency_ID) "
				+ "AND cr.C_ConversionType_ID = ? AND ? >= cr.ValidFrom AND ? <= cr.ValidTo)";
		//	Aisle Seller
		Timestamp now = TimeUtil.getDay(System.currentTimeMillis());
		//	Get Product list
		Query query = new Query(Env.getCtx(), I_C_Currency.Table_Name, whereClause.toString(), null)
				.setParameters(pos.get_ValueAsInt(I_C_ConversionType.COLUMNNAME_C_ConversionType_ID), now, now)
				.setOnlyActiveRecords(true);
		int count = query.count();
		query.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(currencyId -> {
				Currency.Builder currencyBuilder = CoreFunctionalityConvert.convertCurrency(currencyId);
				builder.addCurrencies(currencyBuilder);
			});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}
	
	/**
	 * Process Order from Point of Sales
	 * @param request
	 * @return
	 */
	private MOrder processOrder(ProcessOrderRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		return OrderManagement.processOrder(
			pos,
			request.getId(),
			request.getIsOpenRefund()
		);
	}



	/**
	 * List Orders from POS UUID
	 * @param request
	 * @return
	 */
	private ListOrdersResponse.Builder listOrders(ListOrdersRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @IsMandatory@");
		}
		//	Sales Representative
		if(request.getSalesRepresentativeId() <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @IsMandatory@");
		}

		//	Dynamic where clause
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		int posId = pos.getC_POS_ID();
		int salesRepresentativeId = request.getSalesRepresentativeId();
		int orgId = pos.getAD_Org_ID();
		MUser salesRepresentative = MUser.get(Env.getCtx(), salesRepresentativeId);

		StringBuffer whereClause = new StringBuffer();
		List<Object> parameters = new ArrayList<Object>();
		whereClause.append("(C_Order.AD_Org_ID = ? OR C_Order.C_POS_ID = ?)");
		parameters.add(orgId);
		parameters.add(posId);

		final String whereClauseWithoutProposal = " AND NOT EXISTS(SELECT 1 FROM C_DocType dt "
			+ "WHERE dt.C_DocType_ID = C_Order.C_DocTypeTarget_ID "
			+ "AND dt.DocSubTypeSO IN('ON', 'OB'))"
		;
		if(!salesRepresentative.get_ValueAsBoolean("IsPOSManager")) {
			if(pos.get_ValueAsBoolean("IsConfidentialInfo")) {
				whereClause.append(" AND ((C_Order.SalesRep_ID = ? OR C_Order.AssignedSalesRep_ID = ?) AND C_Order.C_POS_ID = ?)");
				parameters.add(salesRepresentativeId);
				parameters.add(salesRepresentativeId);
				parameters.add(posId);
			} else {
				if(request.getIsOnlyAisleSeller()) {
					String whereIsAisleSeller = "";
					if (pos.get_ColumnIndex("IsAisleSeller") >= 0) {
						whereIsAisleSeller = "AND EXISTS(SELECT 1 FROM C_POS p WHERE p.C_POS_ID = C_Order.C_POS_ID AND p.IsAisleSeller = 'Y')";
					}
					whereClause
						.append(" AND DocStatus NOT IN('VO', 'CL') ")
						.append("AND (")
						.append("(C_Order.SalesRep_ID = ? OR COALESCE(C_Order.AssignedSalesRep_ID, ?) = ?)")
						.append(whereIsAisleSeller)
						.append(")")
						.append(whereClauseWithoutProposal)
					;
					parameters.add(salesRepresentativeId);
					parameters.add(salesRepresentativeId);
					parameters.add(salesRepresentativeId);
				} else {
					whereClause.append(" AND ((C_Order.SalesRep_ID = ? OR COALESCE(C_Order.AssignedSalesRep_ID, ?) = ?) AND EXISTS(SELECT 1 FROM C_POS p WHERE p.C_POS_ID = C_Order.C_POS_ID AND p.IsSharedPOS = 'Y'))");
					parameters.add(salesRepresentativeId);
					parameters.add(salesRepresentativeId);
					parameters.add(salesRepresentativeId);
				}
			}
		}
		//	Document No
		if(!Util.isEmpty(request.getDocumentNo(), true)) {
			whereClause.append(" AND UPPER(DocumentNo) LIKE '%' || UPPER(?) || '%'");
			parameters.add(request.getDocumentNo());
		}
		//	Invoice Document No
		if(!Util.isEmpty(request.getInvoiceNo(), true)) {
			whereClause.append(" AND EXISTS(")
				.append("SELECT 1 ")
				.append("FROM C_Invoice AS i ")
				.append("WHERE i.C_Order_ID = C_Order.C_Order_ID ")
				.append("AND UPPER(i.DocumentNo) LIKE '%' || UPPER(?) || '%' ")
				.append(") ")
			;
			parameters.add(request.getInvoiceNo());
		}
		//	Business Partner
		if(request.getBusinessPartnerId() > 0) {
			int businessPartnerId = request.getBusinessPartnerId();
			whereClause.append(" AND C_BPartner_ID = ?");
			parameters.add(businessPartnerId);
		}
		//	Grand Total
		BigDecimal grandTotal = NumberManager.getBigDecimalFromString(
			request.getGrandTotal()
		);
		if(grandTotal != null
				&& !grandTotal.equals(Env.ZERO)) {
			whereClause.append(" AND GrandTotal = ?");
			parameters.add(grandTotal);
		}
		//	Support Open Amount
		BigDecimal openAmount = NumberManager.getBigDecimalFromString(
			request.getOpenAmount()
		);
		if(openAmount != null && !openAmount.equals(Env.ZERO)) {
			whereClause.append(" AND (EXISTS(SELECT 1 FROM C_Invoice i WHERE i.C_Order_ID = C_Order.C_Order_ID GROUP BY i.C_Order_ID HAVING(SUM(invoiceopen(i.C_Invoice_ID, 0)) = ?))"
					+ " OR EXISTS(SELECT 1 FROM C_Payment p WHERE C_Order_ID = C_Order.C_Order_ID GROUP BY p.C_Order_ID HAVING(SUM(p.PayAmt) = ?)"
					+ ")");
			parameters.add(openAmount);
			parameters.add(openAmount);
		}
		if(request.getIsOnlyProcessed()) {
			whereClause.append(" AND DocStatus IN('CO')");
		}
		//	Is Invoiced
		if(request.getIsWaitingForInvoice()) {
			whereClause.append(" AND DocStatus NOT IN('VO', 'CL')")
				.append(" AND NOT EXISTS(SELECT 1 FROM C_Invoice i WHERE i.C_Order_ID = C_Order.C_Order_ID AND i.DocStatus IN('CO', 'CL'))")
				.append(whereClauseWithoutProposal)
			;
		}
		//	for payment (credit)
		if(request.getIsWaitingForPay()) {
			whereClause.append(" AND DocStatus IN('CO')")
				.append(" AND (EXISTS(SELECT 1 FROM C_Invoice i WHERE i.C_Order_ID = C_Order.C_Order_ID AND i.DocStatus IN('CO', 'CL') AND i.IsPaid = 'N')")
				.append(" OR IsInvoiced = 'N')")
				.append(whereClauseWithoutProposal)
			;
		}
		if(request.getIsBindingOffer()) {
			whereClause.append(" AND DocStatus IN('DR', 'IP') ")
				.append("AND EXISTS(SELECT 1 FROM C_DocType dt ")
				.append("WHERE dt.C_DocType_ID = C_Order.C_DocTypeTarget_ID ")
				.append("AND dt.DocSubTypeSO IN('ON', 'OB')) ")
			;
		}
		if(request.getIsWaitingForShipment()) {
			whereClause.append(" AND DocStatus IN('CO') AND NOT EXISTS(SELECT 1 FROM M_InOut io WHERE io.C_Order_ID = C_Order.C_Order_ID AND io.DocStatus IN('CO', 'CL'))")
			.append(whereClauseWithoutProposal)
			;
		}
		if(request.getIsClosed()) {
			whereClause.append(" AND DocStatus IN('CL') ");
		}
		if(request.getIsNullified()) {
			whereClause.append(" AND DocStatus IN('VO') ");
		}
		//	Is RMA
		if(request.getIsOnlyRma()) {
			whereClause.append(" AND DocStatus IN('CO', 'CL')")
				.append(" AND EXISTS(SELECT 1 FROM C_DocType dt WHERE dt.C_DocType_ID = C_Order.C_DocTypeTarget_ID AND dt.DocSubTypeSO IN('RM') AND dt.IsSOTrx = 'Y') ")
				.append(whereClauseWithoutProposal)
			;
		}
		//	Date Order From
		if(ValueManager.getTimestampFromProtoTimestamp(request.getDateOrderedFrom()) != null) {
			whereClause.append(" AND DateOrdered >= ?");
			parameters.add(
				TimeUtil.getDay(
					ValueManager.getTimestampFromProtoTimestamp(
						request.getDateOrderedFrom()
					)
				)
			);
		}
		//	Date Order To
		if(ValueManager.getTimestampFromProtoTimestamp(request.getDateOrderedTo()) != null) {
			whereClause.append(" AND DateOrdered <= ?");
			parameters.add(
				TimeUtil.getDay(
					ValueManager.getTimestampFromProtoTimestamp(
						request.getDateOrderedTo()
					)
				)
			);
		}

		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			I_C_Order.Table_Name,
			whereClause.toString(),
			null
		)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setOrderBy(
					I_C_Order.COLUMNNAME_DateOrdered + " DESC, "
					+ I_C_Order.COLUMNNAME_Updated + " DESC"
				);

		int count = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListOrdersResponse.Builder builder = ListOrdersResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.forEach(orderId -> {
				Order.Builder orderBuilder = OrderConverUtil.convertOder(orderId);
				builder.addOrders(orderBuilder);
			});

		return builder;
	}


	/**
	 * List Payments from POS UUID
	 * @param request
	 * @return
	 */
	private ListPaymentsResponse.Builder listPayments(ListPaymentsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		ListPaymentsResponse.Builder builder = ListPaymentsResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	Aisle Seller
		int posId = request.getPosId();
		int orderId = request.getOrderId();
		//	For order
		if(orderId > 0) {
			whereClause.append("C_Payment.C_Order_ID = ?");
			parameters.add(orderId);
		} else {
			whereClause.append("C_Payment.C_POS_ID = ?");
			parameters.add(posId);
			whereClause.append(" AND C_Payment.C_Charge_ID IS NOT NULL AND C_Payment.Processed = 'N'");
		}
		if(request.getIsOnlyRefund()) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("C_Payment.IsReceipt = 'N'");
		}
		if(request.getIsOnlyReceipt()) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append("C_Payment.IsReceipt = 'Y'");
		}
		//	Get Product list
		Query query = new Query(Env.getCtx(), I_C_Payment.Table_Name, whereClause.toString(), null)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true);
		int count = query.count();
		query
		.setLimit(limit, offset)
		.<MPayment>list()
		.forEach(payment -> {
			Payment.Builder paymentBuilder = PaymentConvertUtil.convertPayment(
				payment
			);
			builder.addPayments(paymentBuilder);
		});
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}
	
	/**
	 * List Orders Lines from Order UUID
	 * @param request
	 * @return
	 */
	private ListOrderLinesResponse.Builder listOrderLines(ListOrderLinesRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		int orderId = request.getOrderId();
		MOrder order = OrderUtil.validateAndGetOrder(orderId);
		MPOS pos = new MPOS(Env.getCtx(), order.getC_POS_ID(), order.get_TrxName());
		ListOrderLinesResponse.Builder builder = ListOrderLinesResponse.newBuilder();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		StringBuffer whereClause = new StringBuffer(I_C_OrderLine.COLUMNNAME_C_Order_ID + " = ?");
		List<Object> parameters = new ArrayList<>();
		parameters.add(orderId);
		if(pos.get_ValueAsInt("DefaultDiscountCharge_ID") > 0) {
			parameters.add(pos.get_ValueAsInt("DefaultDiscountCharge_ID"));
			whereClause.append(" AND (C_Charge_ID IS NULL OR C_Charge_ID <> ?)");
		}
		if(pos.get_ValueAsInt("ECA14_DefaultGiftCardCharge_ID") > 0) {
			parameters.add(pos.get_ValueAsInt("ECA14_DefaultGiftCardCharge_ID"));
			whereClause.append(" AND (C_Charge_ID IS NULL OR C_Charge_ID <> ?)");
		}
		//	Get Product list
		Query query = new Query(Env.getCtx(), I_C_OrderLine.Table_Name, whereClause.toString(), null)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true);
		int count = query.count();
		query
		.setLimit(limit, offset)
		.setOrderBy(I_C_OrderLine.COLUMNNAME_Line)
		.<MOrderLine>list()
		.forEach(orderLine -> {
			builder.addOrderLines(
				OrderConverUtil.convertOrderLine(
					orderLine
				)
			);
		});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}


	private MOrder changeOrderAssigned(int orderId) {
		return changeOrderAssigned(orderId, 0);
	}
	
	/**
	 * Update Order from UUID
	 * @param request
	 * @return
	 */
	private MOrder changeOrderAssigned(int orderId, int salesRepresentativeId) {
		AtomicReference<MOrder> orderReference = new AtomicReference<MOrder>();
		if(orderId > 0) {
			Trx.run(transactionName -> {
				MOrder salesOrder = OrderUtil.validateAndGetOrder(orderId, transactionName);
				if(!DocumentUtil.isDrafted(salesOrder)) {
					throw new AdempiereException("@C_Order_ID@ @Processed@");
				}
				if(salesRepresentativeId > 0) {
					if(salesOrder.get_ValueAsInt("AssignedSalesRep_ID") > 0 && salesRepresentativeId != salesOrder.get_ValueAsInt(ColumnsAdded.COLUMNNAME_AssignedSalesRep_ID)) {
						throw new AdempiereException("@POS.SalesRepAssigned@");
					}
					salesOrder.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_AssignedSalesRep_ID, salesRepresentativeId);
				} else {
					salesOrder.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_AssignedSalesRep_ID, null);
				}
				//	Save
				salesOrder.saveEx(transactionName);
				orderReference.set(salesOrder);
			});
		}
		//	Return order
		return orderReference.get();
	}



	/**
	 * Validate User PIN
	 * @param userPin
     */
	private Empty.Builder validatePIN(ValidatePINRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), false);

		if(Util.isEmpty(request.getPin())) {
			throw new AdempiereException("@UserPIN@ @IsMandatory@");
		}
		if(Util.isEmpty(request.getRequestedAccess(), true)) {
			throw new AdempiereException("@FillMandatory@ `RequestedAccess`");
		}
		PO supervisorAccess = AccessManagement.getSupervisorAccessFromPIN(
			pos.getC_POS_ID(),
			Env.getAD_User_ID(Env.getCtx()),
			request.getPin(),
			request.getRequestedAccess(),
			request.getRequestedAmount()
		);
		if(supervisorAccess == null) {
			throw new AdempiereException("@POS.SupervisorNotFound@");
		}
		//	Validate special access for PIN (Amount and other types)
		if (request.getRequestedAccess().equals("IsAllowsMaximumRefund")) {
			// TODO: Validate By Current Document, Payments Methods and Daily Acumulated
		}
		else if(request.getRequestedAccess().equals(ColumnsAdded.COLUMNNAME_IsAllowsWriteOffAmount)) {
			MOrder order = OrderUtil.validateAndGetOrder(request.getOrderId());

			MPriceList priceList = MPriceList.get(Env.getCtx(), order.getM_PriceList_ID(), null);
			int standardPrecision = priceList.getStandardPrecision();
			BigDecimal totalOpenAmount = OrderUtil.getTotalOpenAmount(order);
			BigDecimal totalPaymentAmount = OrderUtil.getTotalPaymentAmount(order);
			BigDecimal writeOffAmount = Optional.ofNullable(totalOpenAmount).orElse(Env.ZERO).subtract(Optional.ofNullable(totalPaymentAmount).orElse(Env.ZERO)).abs();
			BigDecimal writeOffPercent = OrderUtil.getWriteOffPercent(totalOpenAmount, totalPaymentAmount, standardPrecision);
			//	For Write off
			if(supervisorAccess.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_ECA14_WriteOffByPercent)) {
				BigDecimal allowedPercent = Optional.ofNullable((BigDecimal) supervisorAccess.get_Value(ColumnsAdded.COLUMNNAME_WriteOffPercentageTolerance)).orElse(Env.ZERO);
				//	Validate Here
				if(allowedPercent.compareTo(Env.ZERO) == 0 || allowedPercent.compareTo(writeOffPercent) >= 0) {
					return Empty.newBuilder();
				} else {
					throw new AdempiereException("@POS.WriteOffNotAllowedByAmount@");
				}
			} else {
				BigDecimal allowedAmount = Optional.ofNullable((BigDecimal) supervisorAccess.get_Value(ColumnsAdded.COLUMNNAME_WriteOffAmtTolerance)).orElse(Env.ZERO);
				int allowedCurrencyId = supervisorAccess.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WriteOffAmtCurrency_ID);
				if(allowedCurrencyId <= 0) {
					allowedCurrencyId = pos.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WriteOffAmtCurrency_ID);
					if (allowedCurrencyId <= 0) {
						MPriceList posPriceList = MPriceList.get(Env.getCtx(), pos.getM_PriceList_ID(), null);
						allowedCurrencyId = posPriceList.getC_Currency_ID();
					}
				}
				allowedAmount = OrderUtil.getConvertedAmountFrom(
					order,
					allowedCurrencyId,
					allowedAmount
				);
				//	Validate Here
				if(allowedAmount.compareTo(Env.ZERO) == 0 || allowedAmount.compareTo(writeOffAmount) >= 0) {
					return Empty.newBuilder();
				} else {
					throw new AdempiereException("@POS.WriteOffNotAllowedByAmount@");
				}
			}
		}
		//	Default
		return Empty.newBuilder();
	}


	/**
	 * Load Price List Version from Price List
	 * @param priceListId
	 * @param validFrom
	 * @param transactionName
	 * @return
	 * @return MPriceListVersion
	 */
	public MPriceListVersion loadPriceListVersion(int priceListId, Timestamp validFrom, String transactionName) {
		MPriceList priceList = MPriceList.get(Env.getCtx(), priceListId, transactionName);
		//
		return priceList.getPriceListVersion(validFrom);
	}



	/**
	 * Delete payment from uuid
	 * @param request
	 * @return
	 */
	private Empty.Builder deletePayment(DeletePaymentRequest request) {
		final int paymentId = request.getId();
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Payment_ID@");
		}
		MPayment payment = new Query(
			Env.getCtx(),
			I_C_Payment.Table_Name,
			I_C_Payment.COLUMNNAME_C_Payment_ID + " = ?",
			null
		)
			.setParameters(paymentId)
			.setClient_ID()
			.first()
		;
		if(payment == null
				|| payment.getC_Payment_ID() == 0) {
			// throw new AdempiereException("@C_Payment_ID@ @NotFound@");
			return Empty.newBuilder();
		}
		//	Validate drafted
		if(!DocumentUtil.isDrafted(payment)) {
			throw new AdempiereException("@C_Payment_ID@ @Processed@");
		}
		//	Validate processed Order
		if(payment.isProcessed()) {
			throw new AdempiereException("@C_Payment_ID@ @Processed@");
		}
		// Validate onlye approval
		if (payment.isOnline()) {
			String onluneStatus = payment.get_ValueAsString("ResponseStatus");
			if (!Util.isEmpty(onluneStatus, true) && onluneStatus.equals("A")) {
				throw new AdempiereException("@C_Payment_ID@ @IsOnline@ @Processed@");
			}
		}
		//	
		if(payment.getC_Payment_ID() >= 0) {
			if ("G".equals(payment.getTenderType())) {
				if (payment.get_ValueAsInt("ECA14_GiftCard_ID") > 0) {
					GiftCardManagement.unProcessingGiftCard(
							payment.get_ValueAsInt("ECA14_GiftCard_ID"), true
					);
				}
			}
			if(payment.getC_Order_ID() > 0) {
				MOrder salesOrder = new MOrder(Env.getCtx(), payment.getC_Order_ID(), null);
				OrderManagement.validateOrderReleased(salesOrder);
			}
			payment.deleteEx(true);
		}
		//	Return
		return Empty.newBuilder();
	}
	
	/**
	 * Create order line and return this
	 * @param request
	 * @return
	 */
	private OrderLine.Builder updateAndConvertOrderLine(UpdateOrderLineRequest request) {
		//	Validate Order
		int orderLineId = request.getId();
		if(orderLineId <= 0) {
			throw new AdempiereException("@C_OrderLine_ID@ @NotFound@");
		}

		AtomicReference<MOrderLine> maybeOrderLine = new AtomicReference<MOrderLine>();
		Trx.run(transactionName -> {
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			//	Quantity
			MOrderLine orderLine = OrderManagement.updateOrderLine(
				transactionName,
				pos,
				orderLineId,
				NumberManager.getBigDecimalFromString(request.getQuantity()),
				NumberManager.getBigDecimalFromString(request.getPrice()),
				NumberManager.getBigDecimalFromString(request.getDiscountRate()),
				request.getIsAddQuantity(),
				request.getWarehouseId(),
				request.getUomId()
			);
			maybeOrderLine.set(orderLine);
		});
		return OrderConverUtil.convertOrderLine(
			maybeOrderLine.get()
		);
	}

	/**
	 * Create order line and return this
	 * @param request
	 * @return
	 */
	private OrderLine.Builder createAndConvertOrderLine(CreateOrderLineRequest request) {
		//	Validate Order
		int orderId = request.getOrderId();
		if(orderId <= 0) {
			throw new AdempiereException("@C_OrderLine_ID@ @NotFound@");
		}
		//	Validate Product and charge
		if(request.getProductId() <= 0
				&& request.getChargeId() <= 0
				&& request.getResourceAssignmentId() <= 0) {
			throw new AdempiereException("@M_Product_ID@ / @C_Charge_ID@ / @S_ResourceAssignment_ID@ @NotFound@");
		}
		MOrderLine orderLine = null;
		if (request.getResourceAssignmentId() > 0) {
			orderLine = addOrderLineFromResourceAssigment(
				orderId,
				request.getResourceAssignmentId(),
				request.getWarehouseId()
			);
		} else {
			orderLine = addOrderLine(
				orderId,
				request.getProductId(),
				request.getChargeId(),
				request.getWarehouseId(),
				NumberManager.getBigDecimalFromString(
					request.getQuantity()
				)
			);
		}
		//	Quantity
		return OrderConverUtil.convertOrderLine(orderLine);
	}
	
	private MOrderLine addOrderLineFromResourceAssigment(int orderId, int resourceAssignmentId, int warehouseId) {
		if(orderId <= 0) {
			return null;
		}
		//	
		AtomicReference<MOrderLine> orderLineReference = new AtomicReference<MOrderLine>();
		Trx.run(transactionName -> {
			MOrder order = new MOrder(Env.getCtx(), orderId, transactionName);
			//	Valid Complete
			if (!DocumentUtil.isDrafted(order)) {
				throw new AdempiereException("@C_Order_ID@ @Processed@");
			}
			OrderUtil.setCurrentDate(order);
			// catch Exceptions at order.getLines()
			Optional<MOrderLine> maybeOrderLine = Arrays.asList(order.getLines(true, "Line"))
				.parallelStream()
				.filter(orderLine -> {
					return resourceAssignmentId != 0 &&
						resourceAssignmentId == orderLine.getS_ResourceAssignment_ID();
				})
				.findFirst()
			;

			MResourceAssignment resourceAssigment = new MResourceAssignment(Env.getCtx(), resourceAssignmentId, transactionName);
			if (!resourceAssigment.isConfirmed()) {
				resourceAssigment = TimeControl.confirmResourceAssignment(resourceAssignmentId);
			}
			BigDecimal quantity = resourceAssigment.getQty();

			if(maybeOrderLine.isPresent()) {
				MOrderLine orderLine = maybeOrderLine.get();
				//	Set Quantity
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = orderLine.getQtyEntered();
					quantityToOrder = quantityToOrder.add(Env.ONE);
				}
				orderLine.setS_ResourceAssignment_ID(resourceAssignmentId);

				OrderUtil.updateUomAndQuantity(orderLine, orderLine.getC_UOM_ID(), quantityToOrder);
				orderLineReference.set(orderLine);
			} else {
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = Env.ONE;
				}
				//create new line
				MOrderLine orderLine = new MOrderLine(order);

				MProduct product = new Query(
					order.getCtx(),
					MProduct.Table_Name,
					" S_Resource_ID = ? ",
					null
				)
					.setParameters(resourceAssigment.getS_Resource_ID())
					.first();
				if (product != null && product.getM_Product_ID() > 0) {
					orderLine.setProduct(product);
					orderLine.setC_UOM_ID(product.getC_UOM_ID());
				}
				orderLine.setS_ResourceAssignment_ID(resourceAssignmentId);

				orderLine.setQty(quantityToOrder);
				orderLine.setPrice();
				orderLine.setTax();
				StringBuffer description = new StringBuffer(
					StringManager.getValidString(
						resourceAssigment.getName()
					)
				);
				if (!Util.isEmpty(resourceAssigment.getDescription())) {
					description.append(" (" + resourceAssigment.getDescription() + ")");
				}
				description.append(": ").append(DisplayType.getDateFormat(DisplayType.DateTime).format(resourceAssigment.getAssignDateFrom()));
				description.append(" ~ ").append(DisplayType.getDateFormat(DisplayType.DateTime).format(resourceAssigment.getAssignDateTo()));
				orderLine.setDescription(description.toString());

				//	Save Line
				orderLine.saveEx(transactionName);
				//	Apply Discount from order
				DiscountManagement.configureDiscountRateOff(
					order,
					(BigDecimal) order.get_Value("FlatDiscount"),
					transactionName
				);
				orderLineReference.set(orderLine);
			}
		});
		return orderLineReference.get();
	}
	
	/***
	 * Add order line
	 * @param orderId
	 * @param productId
	 * @param chargeId
	 * @param warehouseId
	 * @param quantity
	 * @return
	 */
	private MOrderLine addOrderLine(int orderId, int productId, int chargeId, int warehouseId, BigDecimal quantity) {
		if(orderId <= 0) {
			return null;
		}
		//	
		AtomicReference<MOrderLine> orderLineReference = new AtomicReference<MOrderLine>();
		Trx.run(transactionName -> {
			MOrder order = new MOrder(Env.getCtx(), orderId, transactionName);
			//	Valid Complete
			if (!DocumentUtil.isDrafted(order)) {
				throw new AdempiereException("@C_Order_ID@ @Processed@");
			}
			OrderUtil.setCurrentDate(order);
			StringBuffer whereClause = new StringBuffer(I_C_OrderLine.COLUMNNAME_C_Order_ID + " = ?");
			List<Object> parameters = new ArrayList<>();
			parameters.add(orderId);
			if(productId > 0) {
				whereClause.append(" AND M_Product_ID = ?");
				parameters.add(productId);
			} else if(chargeId > 0){
				whereClause.append(" AND C_Charge_ID = ?");
				parameters.add(chargeId);
			}
			MOrderLine orderLine = new Query(Env.getCtx(), I_C_OrderLine.Table_Name, whereClause.toString(), transactionName)
					.setParameters(parameters)
					.first();
			if(orderLine != null
					&& orderLine.getC_OrderLine_ID() > 0) {
				//	Set Quantity
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = orderLine.getQtyEntered();
					quantityToOrder = quantityToOrder.add(Env.ONE);
				}
				OrderUtil.updateUomAndQuantity(orderLine, orderLine.getC_UOM_ID(), quantityToOrder);
				orderLineReference.set(orderLine);
			} else {
				BigDecimal quantityToOrder = quantity;
				if(quantity == null) {
					quantityToOrder = Env.ONE;
				}
				//create new line
				orderLine = new MOrderLine(order);
				orderLine.setC_Campaign_ID(order.getC_Campaign_ID());
				if(chargeId > 0) {
					orderLine.setC_Charge_ID(chargeId);
				} else if(productId > 0) {
					orderLine.setProduct(MProduct.get(order.getCtx(), productId));
				}
				orderLine.setQty(quantityToOrder);
				orderLine.setPrice();
				orderLine.setTax();
				//	Save Line
				orderLine.saveEx(transactionName);
				//	Apply Discount from order
				DiscountManagement.configureDiscountRateOff(
					order,
					(BigDecimal) order.get_Value("FlatDiscount"),
					transactionName
				);
				orderLineReference.set(orderLine);
			}
		});
		return orderLineReference.get();
			
	} //	addOrUpdateLine


	/**
	 * Set UOM and Quantity based on unit of measure
	 * @param inOutLine
	 * @param unitOfMeasureId
	 * @param quantity
	 */
	private void updateUomAndQuantityForShipment(MInOutLine inOutLine, int unitOfMeasureId, BigDecimal quantity) {
		if(quantity != null) {
			inOutLine.setQty(quantity);
		}
		if(unitOfMeasureId > 0) {
			inOutLine.setC_UOM_ID(unitOfMeasureId);
		}
		BigDecimal quantityEntered = inOutLine.getQtyEntered();
		BigDecimal convertedQuantity = MUOMConversion.convertProductFrom(inOutLine.getCtx(), inOutLine.getM_Product_ID(), inOutLine.getC_UOM_ID(), quantityEntered);
		inOutLine.setMovementQty(convertedQuantity);
	}



	/**
	 * get: "/point-of-sales/{id}"
	 * get: "/point-of-sales/terminals/{id}"
	 */
	@Override
	public void listPointOfSales(ListPointOfSalesRequest request, StreamObserver<ListPointOfSalesResponse> responseObserver) {
		try {
			ListPointOfSalesResponse.Builder posList = listPointOfSales(request);
			responseObserver.onNext(posList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	/**
	 * Get list from user
	 * @param request
	 * @return
	 */
	private ListPointOfSalesResponse.Builder listPointOfSales(ListPointOfSalesRequest request) {
		int salesRepresentativeId = Env.getAD_User_ID(Env.getCtx());

		//	Add to recent Item
		org.spin.dictionary.util.DictionaryUtil.addToRecentItem(
			MMenu.ACTION_Form,
			113
		);

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Get POS List
		boolean isAppliedNewFeaturesPOS = M_Element.get(Env.getCtx(), "IsSharedPOS") != null
			&& M_Element.get(Env.getCtx(), "IsAllowsAllocateSeller") != null
		;
		StringBuffer whereClause = new StringBuffer("SalesRep_ID = ? ");
		List<Object> parameters = new ArrayList<>();
		parameters.add(salesRepresentativeId);
		//	applies for Shared pos
		if(isAppliedNewFeaturesPOS) {
			//	Shared POS
			whereClause.append(" OR (AD_Org_ID = ? AND IsSharedPOS = 'Y' AND IsAllowsAllocateSeller = 'N')");
			//	Allocation by Seller Allocation table
			whereClause.append(" OR (IsAllowsAllocateSeller = 'Y' AND EXISTS(SELECT 1 FROM C_POSSellerAllocation sa WHERE sa.C_POS_ID = C_POS.C_POS_ID AND sa.SalesRep_ID = ? AND sa.IsActive = 'Y'))");
			parameters.add(Env.getAD_Org_ID(Env.getCtx()));
			parameters.add(salesRepresentativeId);
		}
		Query query = new Query(
			Env.getCtx(),
			I_C_POS.Table_Name,
			whereClause.toString(),
			null
		)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
			.setOrderBy(I_C_POS.COLUMNNAME_Name)
		;
		int count = query.count();
		ListPointOfSalesResponse.Builder builder = ListPointOfSalesResponse.newBuilder()
			.setRecordCount(count)
		;

		query
			.setLimit(limit, offset)
			.<MPOS>list()
			.forEach(pos -> {
				PointOfSales.Builder posBuilder = org.spin.pos.service.pos.POSConvertUtil.convertPointOfSales(pos);
				builder.addSellingPoints(posBuilder);
			});
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}

	/**
	 * get: "/point-of-sales/{id}"
	 * get: "/point-of-sales/terminals/{id}"
	 */
	@Override
	public void getPointOfSales(PointOfSalesRequest request, StreamObserver<PointOfSales> responseObserver) {
		try {
			PointOfSales.Builder pos = getPosBuilder(request);
			responseObserver.onNext(pos.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			e.printStackTrace();
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}
	/**
	 * Get POS builder
	 * @param context
	 * @param request
	 * @return
	 */
	private PointOfSales.Builder getPosBuilder(PointOfSalesRequest request) {
		return org.spin.pos.service.pos.POSConvertUtil.convertPointOfSales(
			POS.validateAndGetPOS(request.getId(), true)
		);
	}



	/**
	 * Create Order from request
	 * @param context
	 * @param request
	 * @return
	 */
	private Order.Builder createOrder(CreateOrderRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @IsMandatory@");
		}
		if(request.getSalesRepresentativeId() <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @IsMandatory@");
		}
		
		AtomicReference<MOrder> maybeOrder = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

			Timestamp currentDate = TimeManager.getDate();
			CashManagement.validatePreviousCashClosing(pos, currentDate, transactionName);
			CashManagement.getCurrentCashClosing(pos, currentDate, true, transactionName);

			StringBuffer whereClause = new StringBuffer("DocStatus = 'DR' "
					+ "AND C_POS_ID = ? "
					+ "AND NOT EXISTS(SELECT 1 "
					+ "					FROM C_OrderLine ol "
					+ "					WHERE ol.C_Order_ID = C_Order.C_Order_ID)");
			//	
			List<Object> parameters = new ArrayList<Object>();
			parameters.add(pos.getC_POS_ID());

			//	Fill assigned seller
			whereClause.append(" AND AssignedSalesRep_ID = ?");
			parameters.add(request.getSalesRepresentativeId());

			//	Allocation by Seller Allocation table
			MOrder salesOrder = new Query(Env.getCtx(), I_C_Order.Table_Name, 
					whereClause.toString(), transactionName)
					.setParameters(parameters)
					.first();
			//	Validate
			if(salesOrder == null) {
				salesOrder = new MOrder(Env.getCtx(), 0, transactionName);
			} else {
				salesOrder.setDateOrdered(currentDate);
				salesOrder.setDateAcct(currentDate);
				salesOrder.setDatePromised(currentDate);
			}
			//	Set campaign
			//	Default values
			salesOrder.setIsSOTrx(true);
			salesOrder.setAD_Org_ID(pos.getAD_Org_ID());
			salesOrder.setC_POS_ID(pos.getC_POS_ID());
			//	Warehouse
			int warehouseId = pos.getM_Warehouse_ID();
			if(request.getWarehouseId() > 0) {
				warehouseId = request.getWarehouseId();
			}
			//	From POS
			if(warehouseId < 0) {
				warehouseId = pos.getM_Warehouse_ID();
			}
			//	Price List
			int priceListId = pos.getM_PriceList_ID();
			if(request.getPriceListId() > 0) {
				priceListId = request.getPriceListId();
			}
			//	Price List From POS
			if(priceListId < 0) {
				priceListId = pos.getM_PriceList_ID();
			}
			salesOrder.setM_PriceList_ID(priceListId);
			salesOrder.setM_Warehouse_ID(warehouseId);
			//	Document Type
			int documentTypeId = 0;
			if(request.getDocumentTypeId() > 0) {
				documentTypeId = request.getDocumentTypeId();
			}
			//	Validate
			if(documentTypeId <= 0
					&& pos.getC_DocType_ID() != 0) {
				documentTypeId = pos.getC_DocType_ID();
			}
			//	Validate
			if(documentTypeId > 0) {
				salesOrder.setC_DocTypeTarget_ID(documentTypeId);
			} else {
				salesOrder.setC_DocTypeTarget_ID(MOrder.DocSubTypeSO_POS);
			}
			//	Delivery Rules
			if (pos.getDeliveryRule() != null) {
				salesOrder.setDeliveryRule(pos.getDeliveryRule());
			}
			//	Invoice Rule
			if (pos.getInvoiceRule() != null) {
				salesOrder.setInvoiceRule(pos.getInvoiceRule());
			}
			//	Conversion Type
			if(pos.get_ValueAsInt(MOrder.COLUMNNAME_C_ConversionType_ID) > 0) {
				salesOrder.setC_ConversionType_ID(pos.get_ValueAsInt(MOrder.COLUMNNAME_C_ConversionType_ID));
			}
			int campaignId = request.getCampaignId();
			if(campaignId > 0 && campaignId != salesOrder.getC_Campaign_ID()) {
				salesOrder.setC_Campaign_ID(campaignId);
			}
			//	Set business partner
			setBPartner(pos, salesOrder, request.getCustomerId(), request.getSalesRepresentativeId(), transactionName);
			maybeOrder.set(salesOrder);
		});
		//	Convert order
		return OrderConverUtil.convertOrder(
			maybeOrder.get()
		);
	}
	
	/**
	 * Set business partner from uuid
	 * @param pos
	 * @param salesOrder
	 * @param businessPartnerUuid
	 * @param salesRepresentativeUuid
	 * @param transactionName
	 */
	private void setBPartner(MPOS pos, MOrder salesOrder, int businessPartnerId, int salesRepresentativeId, String transactionName) {
		//	Valid if has a Order
		if(DocumentUtil.isCompleted(salesOrder)
				|| DocumentUtil.isVoided(salesOrder)) {
			return;
		}
		//	Get BP
		MBPartner businessPartner = null;
		if(businessPartnerId > 0) {
			businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		}
		boolean isSamePOSPartner = false;
		if(businessPartner == null) {
			businessPartner = pos.getBPartner();
			isSamePOSPartner = true;
		}
		//	Validate business partner
		if(businessPartner == null) {
			throw new AdempiereException("@C_BPartner_ID@ @NotFound@");
		}
		log.fine( "CPOS.setC_BPartner_ID=" + businessPartner.getC_BPartner_ID());
		businessPartner.set_TrxName(transactionName);
		salesOrder.setBPartner(businessPartner);
		boolean isKeepPriceListCustomer = AccessManagement.getBooleanValueFromPOS(pos, businessPartnerId, ColumnsAdded.COLUMNNAME_IsKeepPriceFromCustomer);
		if(!isKeepPriceListCustomer && businessPartner.getM_PriceList_ID() > 0) {
			MPriceList businesPartnerPriceList = MPriceList.get(salesOrder.getCtx(), businessPartner.getM_PriceList_ID(), transactionName);
			MPriceList currentPriceList = MPriceList.get(salesOrder.getCtx(), pos.getM_PriceList_ID(), transactionName);
			if(currentPriceList.getC_Currency_ID() != businesPartnerPriceList.getC_Currency_ID()) {
				salesOrder.setM_PriceList_ID(currentPriceList.getM_PriceList_ID());
			}
		}
		//	
		MBPartnerLocation [] partnerLocations = businessPartner.getLocations(true);
		if(partnerLocations.length > 0) {
			for(MBPartnerLocation partnerLocation : partnerLocations) {
				if(partnerLocation.isBillTo())
					salesOrder.setBill_Location_ID(partnerLocation.getC_BPartner_Location_ID());
				if(partnerLocation.isShipTo())
					salesOrder.setShip_Location_ID(partnerLocation.getC_BPartner_Location_ID());
			}				
		}
		//	Validate Same BPartner
		if(isSamePOSPartner) {
			if(salesOrder.getPaymentRule() == null) {
				salesOrder.setPaymentRule(MOrder.PAYMENTRULE_Cash);
			}
		}
		//	Set Sales Representative
		if(salesRepresentativeId <= 0) {
			MUser currentUser = MUser.get(salesOrder.getCtx());
			PO sellerSupervisor = new Query(
				Env.getCtx(),
				"C_POSSellerAllocation",
				"C_POS_ID = ? AND SalesRep_ID = ? AND IsAllowsPOSManager='Y'",
				transactionName
			).setParameters(pos.getC_POS_ID(), currentUser.getAD_User_ID())
			.first();

			if (pos.get_ValueAsBoolean("IsSharedPOS")) {
				salesRepresentativeId = currentUser.getAD_User_ID();
			} else if (sellerSupervisor != null) {
				salesRepresentativeId = sellerSupervisor.get_ValueAsInt("SalesRep_ID");
			} else if (businessPartner.getSalesRep_ID() != 0) {
				salesRepresentativeId = salesOrder.getC_BPartner().getSalesRep_ID();
			} else {
				salesRepresentativeId = pos.getSalesRep_ID();
			}
		}
		//	Set
		if(salesRepresentativeId > 0) {
			salesOrder.setSalesRep_ID(salesRepresentativeId);
			if(salesOrder.get_ValueAsInt("AssignedSalesRep_ID") <= 0) {
				salesOrder.set_ValueOfColumn("AssignedSalesRep_ID", salesRepresentativeId);
			}
		}
		OrderUtil.setCurrentDate(salesOrder);
		//	Save Header
		salesOrder.saveEx();
		//	Load Price List Version
		MPriceList priceList = MPriceList.get(Env.getCtx(), salesOrder.getM_PriceList_ID(), transactionName);
		//
		MPriceListVersion priceListVersion = priceList.getPriceListVersion (TimeManager.getDate());
		List<MProductPrice> productPrices = Arrays.asList(priceListVersion.getProductPrice(" AND EXISTS("
				+ "SELECT 1 "
				+ "FROM C_OrderLine ol "
				+ "WHERE ol.C_Order_ID = " + salesOrder.getC_Order_ID() + " "
				+ "AND ol.M_Product_ID = M_ProductPrice.M_Product_ID)"));
		//	Update Lines
		Arrays.asList(salesOrder.getLines())
			.forEach(orderLine -> {
				//	Verify if exist
				if(productPrices
					.parallelStream()
					.filter(productPrice -> productPrice.getM_Product_ID() == orderLine.getM_Product_ID())
					.findFirst()
					.isPresent()) {
					orderLine.setC_BPartner_ID(businessPartnerId);
					orderLine.setC_BPartner_Location_ID(salesOrder.getC_BPartner_Location_ID());
					orderLine.setPrice();
					orderLine.setTax();
					orderLine.saveEx();
				} else {
					orderLine.deleteEx(true);
				}
			});
	}
	
	/**
	 * Update payment if is required
	 * @param request
	 * @return
	 */
	private MPayment updatePayment(UpdatePaymentRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Payment_ID@");
		}
		AtomicReference<MPayment> maybePayment = new AtomicReference<MPayment>();
		Trx.run(transactionName -> {
			String tenderType = request.getTenderTypeCode();
			final int paymentId = request.getId();
			MPayment payment = new MPayment(Env.getCtx(), paymentId, transactionName);
			if (payment == null || payment.getC_Payment_ID() <= 0) {
				throw new AdempiereException("@C_Payment_ID@ @NotFound@");
			}
			if(!DocumentUtil.isDrafted(payment)) {
				throw new AdempiereException("@C_Payment_ID@ @Processed@");
			}
			if(payment.getC_Order_ID() > 0) {
				MOrder salesOrder = new MOrder(Env.getCtx(), payment.getC_Order_ID(), transactionName);
				OrderManagement.validateOrderReleased(salesOrder);
			}
			if(!Util.isEmpty(tenderType)) {
				payment.setTenderType(tenderType);
			}
			if(ValueManager.getTimestampFromProtoTimestamp(request.getPaymentDate()) != null) {
				Timestamp date = ValueManager.getTimestampFromProtoTimestamp(
					request.getPaymentDate()
				);
				if(date != null) {
					payment.setDateTrx(date);
				}
			}
			if(ValueManager.getTimestampFromProtoTimestamp(request.getPaymentAccountDate()) != null) {
				Timestamp date = ValueManager.getTimestampFromProtoTimestamp(
					request.getPaymentAccountDate()
				);
				if(date != null) {
					payment.setDateAcct(date);
				}
			}
			//	Set Bank Id
			if(request.getBankId() > 0) {
				payment.set_ValueOfColumn(MBank.COLUMNNAME_C_Bank_ID, request.getBankId());
			}
			//	Validate reference
			if(!Util.isEmpty(request.getReferenceNo())) {
				payment.addDescription(request.getReferenceNo());
			}
			//	Set Description
			if(!Util.isEmpty(request.getDescription())) {
				payment.addDescription(request.getDescription());
			}
			// Currency
			MCurrency currency = MCurrency.get(Env.getCtx(), payment.getC_Currency_ID());
			if (currency == null || currency.getC_Currency_ID() <= 0) {
				throw new AdempiereException("@C_Currency_ID@ @NotFound@");
			}
			//	Amount
			BigDecimal paymentAmount = NumberManager.getBigDecimalFromString(
				request.getAmount()
			);
			if (paymentAmount != null) {
				paymentAmount = paymentAmount.setScale(currency.getStdPrecision(), RoundingMode.HALF_UP);
			}

			payment.setPayAmt(paymentAmount);
			payment.setOverUnderAmt(Env.ZERO);
			CashUtil.setCurrentDate(payment);
			payment.saveEx(transactionName);
			maybePayment.set(payment);
		});
		//	Return payment
		return maybePayment.get();
	}
	
	/**
	 * create Payment
	 * @param request
	 * @return
	 */
	private MPayment createPayment(CreatePaymentRequest request) {
		AtomicReference<MPayment> maybePayment = new AtomicReference<MPayment>();
		Trx.run(transactionName -> {
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			if(request.getOrderId() > 0) {
				MOrder salesOrder = OrderUtil.validateAndGetOrder(request.getOrderId(), transactionName);
				maybePayment.set(CollectingManagement.createPaymentFromOrder(salesOrder, request, pos, transactionName));
			} else if(request.getChargeId() > 0) {
				maybePayment.set(CashManagement.createPaymentFromCharge(request.getChargeId(), request, pos, transactionName));
			} else {
				throw new AdempiereException("@C_Charge_ID@ / @C_Order_ID@ @NotFound@");
			}
		});
		//	Return payment
		return maybePayment.get();
	}



	@Override
	public void getProductPrice(GetProductPriceRequest request, StreamObserver<ProductPrice> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + request.getSearchValue());
			ProductPrice.Builder productPrice = ProductServiceLogic.getProductPrice(request);
			responseObserver.onNext(productPrice.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
	public void listProductPrice(ListProductPriceRequest request, StreamObserver<ListProductPriceResponse> responseObserver) {
		try {
			ListProductPriceResponse.Builder productPriceList = ProductServiceLogic.listProductsPrices(request);
			responseObserver.onNext(productPriceList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
			responseObserver.onError(
				Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void listStocks(ListStocksRequest request, StreamObserver<ListStocksResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			log.fine("Object Requested = " + SessionManager.getSessionUuid());
			ListStocksResponse.Builder stocks = listStocks(request);
			responseObserver.onNext(stocks.build());
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
	 * List Stocks
	 * @param request
	 * @return
	 */
	private ListStocksResponse.Builder listStocks(ListStocksRequest request) {
		ListStocksResponse.Builder builder = ListStocksResponse.newBuilder();
		Trx.run(transactionName -> {
			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
			String sku = request.getSku();
			if(request.getSku().contains(":")) {
				sku = request.getSku().split(":")[0];
			}
			MProduct product = null;
			if (!Util.isEmpty(sku, true)) {
				product = getProductFromSku(sku);
			} else if (!Util.isEmpty(request.getValue(), true)) {
				product = getProductFromValue(request.getValue());
			}
			if(product == null) {
				throw new AdempiereException("@M_Product_ID@ @NotFound@");
			}

			String nexPageToken = null;
			int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
			int limit = LimitUtil.PAGE_SIZE;
			int offset = pageNumber * LimitUtil.PAGE_SIZE;

			final String whereClause = I_M_Storage.COLUMNNAME_M_Product_ID + " = ? "
				+ " AND EXISTS (SELECT 1 FROM C_POSWarehouseAllocation "
				+ " JOIN C_POS ON C_POS.C_POS_ID = C_POSWarehouseAllocation.C_POS_ID "
				+ " JOIN M_Locator ON M_Locator.M_Locator_ID = M_Storage.M_Locator_ID "
				+ " AND C_POSWarehouseAllocation.M_Warehouse_ID = M_Locator.M_Warehouse_ID "
				+ " WHERE C_POSWarehouseAllocation.C_POS_ID = ? "
				+ ")"
			;

			/*
			boolean IsMultiStoreStock = false;
			int warehouseId = store.getM_Warehouse_ID();
			if(!IsMultiStoreStock) {
				whereClause = "AND M_Locator_ID IN (SELECT M_Locator_ID FROM M_Locator WHERE M_Warehouse_ID=" + warehouseId + ")";
			}
			*/
			
			Query query = new Query(
					Env.getCtx(),
					I_M_Storage.Table_Name, 
					whereClause,
					null
				)
				.setParameters(product.getM_Product_ID(), pos.getC_POS_ID())
				.setClient_ID()
				.setOnlyActiveRecords(true);
			int count = query.count();
			query.<MStorage>list().forEach(storage -> builder.addStocks(
				(convertStock(storage)).build())
			);
			//	
			builder.setRecordCount(count);
			//	Set page token
			if(count > offset && count > limit) {
				nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
			}
			builder.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			);
		});
		
		//	
		return builder;
	}

	/**
	 * Get product from SKU
	 * @param sku
	 * @return
	 */
	private MProduct getProductFromSku(String sku) {
		//	SKU
		if(Util.isEmpty(sku)) {
			throw new AdempiereException("@SKU@ @IsMandatory@");
		}
		//	
		MProduct product = new Query(
				Env.getCtx(),
				I_M_Product.Table_Name, 
				"(UPPER(SKU) = UPPER(?))",
				null
			)
			.setParameters(sku.trim())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.first();
		//	Validate product
		if(product == null) {
			throw new AdempiereException("@M_Product_ID@ @NotFound@");
		}
		//	Default
		return product;
	}

	/**
	 * Get product from Value
	 * @param sku
	 * @return
	 */
	private MProduct getProductFromValue(String value) {
		if(Util.isEmpty(value)) {
			throw new AdempiereException("@Value@ @IsMandatory@");
		}
		//	
		MProduct product = new Query(
				Env.getCtx(),
				I_M_Product.Table_Name, 
				"(UPPER(Value) = UPPER(?))",
				null
			)
			.setParameters(value.trim())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.first();
		//	Validate product
		if(product == null) {
			throw new AdempiereException("@M_Product_ID@ @NotFound@");
		}
		//	Default
		return product;
	}

	/**
	 * Convert stock
	 * @param storage
	 * @return
	 */
	private Stock.Builder convertStock(MStorage storage) {
		Stock.Builder builder = Stock.newBuilder();
		if(storage == null) {
			return builder;
		}
		BigDecimal quantityOnHand = Optional.ofNullable(storage.getQtyOnHand()).orElse(Env.ZERO);
		BigDecimal quantityReserved = Optional.ofNullable(storage.getQtyReserved()).orElse(Env.ZERO);
		BigDecimal quantityAvailable = quantityOnHand.subtract(quantityReserved);
		builder.setIsInStock(quantityAvailable.signum() > 0);
		builder.setQuantity(quantityAvailable.doubleValue());
		//	
		MProduct product = MProduct.get(Env.getCtx(), storage.getM_Product_ID());
		MUOM unitOfMeasure = MUOM.get(Env.getCtx(), product.getC_UOM_ID());
		Trx.run(transactionName -> {
			MAttributeSetInstance attribute = new MAttributeSetInstance(Env.getCtx(), storage.getM_AttributeSetInstance_ID(), transactionName);
			builder.setAttributeName(
				StringManager.getValidString(
					attribute.getDescription()
				)
			);
		});
		
		builder.setIsDecimalQuantity(unitOfMeasure.getStdPrecision() != 0);
		//	References
		builder.setProductId(storage.getM_Product_ID());

		builder.setIsManageStock(product.isStocked());
		//	Warehouse
		MWarehouse warehouse = MWarehouse.get(Env.getCtx(), storage.getM_Warehouse_ID());
		builder
			.setWarehouseId(warehouse.getM_Warehouse_ID())
			.setWarehouseName(Optional.ofNullable(warehouse.getName()).orElse("")
		);
		//	
		return builder;
	}

	@Override
	public void listAvailableCash(ListAvailableCashRequest request, StreamObserver<ListAvailableCashResponse> responseObserver) {
		try {
			ListAvailableCashResponse.Builder cashListBuilder = listAvailableCash(request);
			responseObserver.onNext(cashListBuilder.build());
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
	
	/**
	 * List Cash from POS UUID
	 * @param request
	 * @return
	 */
	private ListAvailableCashResponse.Builder listAvailableCash(ListAvailableCashRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		Properties context = Env.getCtx();
		ListAvailableCashResponse.Builder builder = ListAvailableCashResponse.newBuilder();
		final String TABLE_NAME = "C_POSCashAllocation";
		if(MTable.getTable_ID(TABLE_NAME) <= 0) {
			return builder;
		}
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Aisle Seller
		int posId = request.getPosId();
		//	Get Product list
		Query query = new Query(
			context,
				TABLE_NAME,
				"C_POS_ID = ?",
				null
			)
			.setParameters(posId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo);
		int count = query.count();
		query
			.setLimit(limit, offset)
			.list()
			.forEach(availableCash -> {
				MBankAccount bankAccount = MBankAccount.get(context, availableCash.get_ValueAsInt("C_BankAccount_ID"));
				AvailableCash.Builder availableCashBuilder = AvailableCash.newBuilder()
					.setId(
						bankAccount.getC_BankAccount_ID()
					)
					.setName(
						StringManager.getValidString(
							bankAccount.getName()
						)
					)
					.setKey(
						StringManager.getValidString(
							bankAccount.getAccountNo()
						)
					)
					.setIsPosRequiredPin(
						availableCash.get_ValueAsBoolean(I_C_POS.COLUMNNAME_IsPOSRequiredPIN)
					)
					.setBankAccount(
						CoreFunctionalityConvert.convertBankAccount(
							bankAccount
						)
					)
				;

				builder.addCash(availableCashBuilder);
		});
		//	
		builder.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builder.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builder;
	}


	@Override
	public void saveCommandShortcut(SaveCommandShortcutRequest request, StreamObserver<CommandShortcut> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			CommandShortcut.Builder cashListBuilder = saveCommandShortcut(request);
			responseObserver.onNext(cashListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	private CommandShortcut.Builder saveCommandShortcut(SaveCommandShortcutRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

		if (Util.isEmpty(request.getCommand(), true)) {
			throw new AdempiereException("@ECA14_Command@ @NotFound@");
		}
		if (Util.isEmpty(request.getShortcut(), true)) {
			throw new AdempiereException("@ECA14_Shortcut@ @NotFound@");
		}

		final String sqlShorcut = "SELECT ECA14_Command FROM C_POSCommandShortcut WHERE C_POS_ID = ? AND ECA14_Shortcut = ?";
		String commandUsed = DB.getSQLValueString(null, sqlShorcut, pos.getC_POS_ID(), request.getShortcut());
		if (!Util.isEmpty(commandUsed, true)) {
			throw new AdempiereException("@ECA14_Shortcut@ @Used@ " + commandUsed);
		}

		final String whereClause = "C_POS_ID = ? AND ECA14_Command = ?";
		PO shorcutCommand = new Query(
			Env.getCtx(),
			"C_POSCommandShortcut",
			whereClause,
			null
		)
			.setClient_ID()
			.setParameters(pos.getC_POS_ID(), request.getCommand())
			.first()
		;
		if (shorcutCommand == null) {
			MTable table = MTable.get(Env.getCtx(), "C_POSCommandShortcut");
			shorcutCommand = table.getPO(0, null);
			shorcutCommand.set_ValueOfColumn("C_POS_ID", pos.getC_POS_ID());
		}

		shorcutCommand.set_ValueOfColumn("ECA14_Command", request.getCommand());
		shorcutCommand.set_ValueOfColumn("ECA14_Shortcut", request.getShortcut());
		shorcutCommand.saveEx();

		CommandShortcut.Builder builder = POSConvertUtil.convertCommandShorcut(shorcutCommand);

		return builder;
	}


	@Override
	public void listCommandShortcuts(ListCommandShortcutsRequest request, StreamObserver<ListCommandShortcutsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListCommandShortcutsResponse.Builder cashListBuilder = listCommandShortcuts(request);
			responseObserver.onNext(cashListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	private ListCommandShortcutsResponse.Builder listCommandShortcuts(ListCommandShortcutsRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

		Query query = new Query(
			Env.getCtx(),
			"C_POSCommandShortcut",
			I_C_POS.COLUMNNAME_C_POS_ID + " = ?",
			null
		)
			.setParameters(pos.getC_POS_ID())
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;

		int count = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListCommandShortcutsResponse.Builder builderList = ListCommandShortcutsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list()
			.forEach(commandShorcut -> {
				CommandShortcut.Builder builder = POSConvertUtil.convertCommandShorcut(commandShorcut);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}


	@Override
	public void deleteCommandShortcut(DeleteCommandShortcutRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			Empty.Builder cashListBuilder = deleteCommandShortcut(request);
			responseObserver.onNext(cashListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
					.withDescription(e.getLocalizedMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}

	private Empty.Builder deleteCommandShortcut(DeleteCommandShortcutRequest request) {
		if (request.getId() <= 0) {
			throw new AdempiereException("@C_POSCommandShortcut_ID@ @NotFound@");
		}
		Trx.run(transactionName -> {
			PO commandShorcut = RecordUtil.getEntity(Env.getCtx(), "C_POSCommandShortcut", request.getId(), transactionName);
			if (commandShorcut == null || commandShorcut.get_ID() <= 0) {
				throw new AdempiereException("@C_POSCommandShortcut_ID@ @NotFound@");
			}

			commandShorcut.deleteEx(true);
		});

		Empty.Builder builder = Empty.newBuilder();

		return builder;
	}



	@Override
	public void listCampaigns(ListCampaignsRequest request, StreamObserver<ListCampaignsResponse> responseObserver) {
		try {
			if(request == null) {
				throw new AdempiereException("Object Request Null");
			}
			ListCampaignsResponse.Builder cashListBuilder = POS.listCampaigns(request);
			responseObserver.onNext(cashListBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.warning(e.getLocalizedMessage());
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
