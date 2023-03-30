/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.core.domains.models.I_M_InOutLine;
import org.adempiere.core.domains.models.I_M_Product;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.form.express_shipment.CreateShipmentLineRequest;
import org.spin.backend.grpc.form.express_shipment.CreateShipmentRequest;
import org.spin.backend.grpc.form.express_shipment.DeleteShipmentLineRequest;
import org.spin.backend.grpc.form.express_shipment.DeleteShipmentRequest;
import org.spin.backend.grpc.form.express_shipment.ListProductsRequest;
import org.spin.backend.grpc.form.express_shipment.ListProductsResponse;
import org.spin.backend.grpc.form.express_shipment.ListSalesOrdersRequest;
import org.spin.backend.grpc.form.express_shipment.ListSalesOrdersResponse;
import org.spin.backend.grpc.form.express_shipment.ListShipmentLinesRequest;
import org.spin.backend.grpc.form.express_shipment.ListShipmentLinesResponse;
import org.spin.backend.grpc.form.express_shipment.ProcessShipmentRequest;
import org.spin.backend.grpc.form.express_shipment.Product;
import org.spin.backend.grpc.form.express_shipment.SalesOrder;
import org.spin.backend.grpc.form.express_shipment.Shipment;
import org.spin.backend.grpc.form.express_shipment.ShipmentLine;
import org.spin.backend.grpc.form.express_shipment.UpdateShipmentLineRequest;
import org.spin.backend.grpc.form.express_shipment.ExpressShipmentGrpc.ExpressShipmentImplBase;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Express Shipment
 */
public class ExpressShipmentServiceImplementation extends ExpressShipmentImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(ExpressShipmentServiceImplementation.class);
	
	public String tableName = I_M_InOut.Table_Name;

	@Override
	public void listSalesOrders(ListSalesOrdersRequest request, StreamObserver<ListSalesOrdersResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListSalesOrdersResponse.Builder builderList = listSalesOrders(request);
			responseObserver.onNext(builderList.build());
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
	 * Get default value base on field, process parameter, browse field or column
	 * @param request
	 * @return
	 */
	private ListSalesOrdersResponse.Builder listSalesOrders(ListSalesOrdersRequest request) {
		Properties context = Env.getCtx();

		final String whereClause = "IsSOTrx='Y' "
			+ "AND C_Order.DocStatus IN ('CL','CO')"
			+ "AND EXISTS(SELECT 1 FROM C_OrderLine ol "
			+ "WHERE ol.C_Order_ID = C_Order.C_Order_ID "
			+ "AND COALESCE(ol.QtyOrdered, 0) - COALESCE(ol.QtyDelivered, 0) > 0)"
		;
		Query query = new Query(
			context,
			I_C_Order.Table_Name,
			whereClause,
			null
		);

		String nexPageToken = "";
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = query.count();
		//	Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListSalesOrdersResponse.Builder builderList = ListSalesOrdersResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(nexPageToken)
		;

		query.setLimit(limit, offset)
			.list(MOrder.class)
			.forEach(salesOrder -> {
				SalesOrder.Builder builder = convertSalesOrder(salesOrder);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}

	SalesOrder.Builder convertSalesOrder(MOrder salesOrder) {
		SalesOrder.Builder builder = SalesOrder.newBuilder();
		if (salesOrder == null) {
			return builder;
		}

		builder.setId(salesOrder.getC_Order_ID())
			.setUuid(
				ValueUtil.validateNull(salesOrder.getUUID())
			)
			.setDocumentNo(
				ValueUtil.validateNull(salesOrder.getDocumentNo())
			)
		;

		return builder;
	}



	@Override
	public void listProducts(ListProductsRequest request, StreamObserver<ListProductsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListProductsResponse.Builder builder = listProducts(request);
			responseObserver.onNext(builder.build());
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

	private ListProductsResponse.Builder listProducts(ListProductsRequest request) {
		int orderId = request.getOrderId();
		if (orderId <= 0 && !Util.isEmpty(request.getOrderUuid(), true)) {
			orderId = RecordUtil.getIdFromUuid(this.tableName, request.getOrderUuid(), null);
		}
		if (orderId <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}

		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer("IsSold = 'Y' ")
			.append("AND EXISTS(SELECT 1 FROM C_OrderLine AS OL ")
			.append("WHERE OL.C_Order_ID = ? AND OL.M_Product_ID = M_Product.M_Product_ID) ")
		;
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(orderId);

		//	For search value
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause.append(" AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(UPC) = UPPER(?) "
				+ "OR UPPER(SKU) = UPPER(?) "
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}

		Query query = new Query(
			Env.getCtx(),
			I_M_Product.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters);

		String nexPageToken = "";
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = query.count();
		//	Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListProductsResponse.Builder builderList = ListProductsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list(MProduct.class)
			.forEach(product -> {
				Product.Builder builder = convertProduct(product);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}

	Product.Builder convertProduct(MProduct product) {
		Product.Builder builder = Product.newBuilder();
		if (product == null) {
			return builder;
		}
		builder.setId(product.getM_Product_ID())
			.setUuid(
				ValueUtil.validateNull(product.getUUID())
			)
			.setUpc(
				ValueUtil.validateNull(product.getUPC())
			)
			.setSku(
				ValueUtil.validateNull(product.getSKU())
			)
			.setValue(
				ValueUtil.validateNull(product.getValue())
			)
			.setName(
				ValueUtil.validateNull(product.getName())
			)
		;

		return builder;
	}



	@Override
	public void createShipment(CreateShipmentRequest request, StreamObserver<Shipment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Shipment.Builder builder = createShipment(request);
			responseObserver.onNext(builder.build());
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

	private Shipment.Builder createShipment(CreateShipmentRequest request) {
		Shipment.Builder builder = Shipment.newBuilder();

		return builder;
	}



	@Override
	public void deleteShipment(DeleteShipmentRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Empty.Builder builder = deleteShipment(request);
			responseObserver.onNext(builder.build());
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

	private Empty.Builder deleteShipment(DeleteShipmentRequest request) {
		int shipmentId = request.getId();
		if (shipmentId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
			shipmentId = RecordUtil.getIdFromUuid(this.tableName, request.getUuid(), null);
		}
		if (shipmentId <= 0) {
			throw new AdempiereException("@M_InOut_ID@ @NotFound@");
		}

		MInOut shipment = new MInOut(Env.getCtx(), shipmentId, null);
		shipment.deleteEx(true);

		Empty.Builder builder = Empty.newBuilder();

		return builder;
	}



	@Override
	public void processShipment(ProcessShipmentRequest request, StreamObserver<Shipment> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Shipment.Builder builder = processShipment(request);
			responseObserver.onNext(builder.build());
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

	private Shipment.Builder processShipment(ProcessShipmentRequest request) {
		Shipment.Builder builder = Shipment.newBuilder();

		return builder;
	}



	@Override
	public void createShipmentLine(CreateShipmentLineRequest request, StreamObserver<ShipmentLine> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ShipmentLine.Builder builder = createShipmentLine(request);
			responseObserver.onNext(builder.build());
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

	private ShipmentLine.Builder createShipmentLine(CreateShipmentLineRequest request) {
		ShipmentLine.Builder builder = ShipmentLine.newBuilder();

		return builder;
	}



	@Override
	public void deleteShipmentLine(DeleteShipmentLineRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Empty.Builder builder = deleteShipmentLine(request);
			responseObserver.onNext(builder.build());
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

	private Empty.Builder deleteShipmentLine(DeleteShipmentLineRequest request) {
		int shipmentLineId = request.getId();
		if (shipmentLineId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
			shipmentLineId = RecordUtil.getIdFromUuid(I_M_InOutLine.Table_Name, request.getUuid(), null);
		}
		if (shipmentLineId <= 0) {
			throw new AdempiereException("@M_InOutLine_ID@ @NotFound@");
		}

		MInOutLine shipmentLine = new MInOutLine(Env.getCtx(), shipmentLineId, null);
		shipmentLine.deleteEx(true);

		Empty.Builder builder = Empty.newBuilder();

		return builder;
	}



	@Override
	public void updateShipmentLine(UpdateShipmentLineRequest request, StreamObserver<ShipmentLine> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ShipmentLine.Builder builder = updateShipmentLine(request);
			responseObserver.onNext(builder.build());
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

	private ShipmentLine.Builder updateShipmentLine(UpdateShipmentLineRequest request) {
		ShipmentLine.Builder builder = ShipmentLine.newBuilder();

		return builder;
	}



	@Override
	public void listShipmentLines(ListShipmentLinesRequest request, StreamObserver<ListShipmentLinesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListShipmentLinesResponse.Builder builderList = listShipmentLines(request);
			responseObserver.onNext(builderList.build());
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

	private ListShipmentLinesResponse.Builder listShipmentLines(ListShipmentLinesRequest request) {
		ListShipmentLinesResponse.Builder builderList = ListShipmentLinesResponse.newBuilder();

		return builderList;
	}

}
