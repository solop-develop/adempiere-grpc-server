/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_M_Movement;
import org.adempiere.core.domains.models.I_M_MovementLine;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_Warehouse;
import org.adempiere.core.domains.models.X_M_Movement;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.form.express_movement.CreateMovementLineRequest;
import org.spin.backend.grpc.form.express_movement.CreateMovementRequest;
import org.spin.backend.grpc.form.express_movement.DeleteMovementLineRequest;
import org.spin.backend.grpc.form.express_movement.DeleteMovementRequest;
import org.spin.backend.grpc.form.express_movement.ListMovementLinesRequest;
import org.spin.backend.grpc.form.express_movement.ListMovementLinesResponse;
import org.spin.backend.grpc.form.express_movement.ListProductsRequest;
import org.spin.backend.grpc.form.express_movement.ListProductsResponse;
import org.spin.backend.grpc.form.express_movement.ListWarehousesRequest;
import org.spin.backend.grpc.form.express_movement.ListWarehousesResponse;
import org.spin.backend.grpc.form.express_movement.Movement;
import org.spin.backend.grpc.form.express_movement.MovementLine;
import org.spin.backend.grpc.form.express_movement.ProcessMovementRequest;
import org.spin.backend.grpc.form.express_movement.Product;
import org.spin.backend.grpc.form.express_movement.UpdateMovementLineRequest;
import org.spin.backend.grpc.form.express_movement.Warehouse;
import org.spin.backend.grpc.form.express_movement.ExpressMovementGrpc.ExpressMovementImplBase;
import org.spin.base.util.DocumentUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Express Movement
 */
public class ExpressMovementServiceImplementation extends ExpressMovementImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(ExpressMovementServiceImplementation.class);
	
	public String tableName = I_M_Movement.Table_Name;


	@Override
	public void listWarehouses(ListWarehousesRequest request, StreamObserver<ListWarehousesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListWarehousesResponse.Builder builderList = listWarehouses(request);
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

	ListWarehousesResponse.Builder listWarehouses(ListWarehousesRequest request) {
		final String whereClause = "M_Warehouse_ID > 0";
		Query query = new Query(
			Env.getCtx(),
			I_M_Warehouse.Table_Name,
			whereClause,
			null
		).setClient_ID();

		int count = query.count();
		String nexPageToken = "";
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListWarehousesResponse.Builder builderList = ListWarehousesResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(nexPageToken)
		;

		query.setLimit(limit, offset)
			.list(MWarehouse.class)
			.forEach(businessPartner -> {
				Warehouse.Builder builder = convertWarehouse(businessPartner);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}

	Warehouse.Builder convertWarehouse(MWarehouse warehouse) {
		Warehouse.Builder builder = Warehouse.newBuilder();
		if (builder == null) {
			return builder;
		}

		builder.setId(warehouse.getM_Warehouse_ID())
			.setUuid(
				ValueUtil.validateNull(warehouse.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(warehouse.getValue())
			)
			.setName(
				ValueUtil.validateNull(warehouse.getName())
			)
			.setDescription(
				ValueUtil.validateNull(warehouse.getDescription())
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
		//	Dynamic where clause
		String whereClause = "IsStocked = 'Y' ";
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		//	For search value
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause += " AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(UPC) = UPPER(?) "
				+ "OR UPPER(SKU) = UPPER(?) "
				+ ")"
			;
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}

		Query query = new Query(
			Env.getCtx(),
			I_M_Product.Table_Name,
			whereClause,
			null
		)
			.setClient_ID()
			.setParameters(parameters);

		int count = query.count();
		String nexPageToken = "";
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
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

	Product.Builder convertProduct(int productId) {
		Product.Builder builder = Product.newBuilder();
		if (productId <= 0) {
			return builder;
		}
		MProduct product = MProduct.get(Env.getCtx(), productId);
		return convertProduct(product);
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



	Movement.Builder convertMovement(MMovement movement) {
		Movement.Builder builder = Movement.newBuilder();
		if (movement == null) {
			return builder;
		}

		builder.setId(movement.getM_Movement_ID())
			.setUuid(
				ValueUtil.validateNull(movement.getUUID())
			)
			.setDocumentNo(
				ValueUtil.validateNull(movement.getDocumentNo())
			)
			.setMovementDate(
				ValueUtil.getLongFromTimestamp(movement.getMovementDate())
			)
			.setDescription(
				ValueUtil.validateNull(movement.getDescription())
			)
			.setIsCompleted(
				DocumentUtil.isCompleted(movement)
			)
		;

		return builder;
	}

	@Override
	public void createMovement(CreateMovementRequest request, StreamObserver<Movement> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Movement.Builder builder = createMovement(request);
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

	private Movement.Builder createMovement(CreateMovementRequest request) {
		AtomicReference<MMovement> maybeMovement = new AtomicReference<MMovement>();

		Trx.run(transactionName -> {
			MMovement inventoryMovement = new MMovement(Env.getCtx(), 0, transactionName);
			inventoryMovement.setMovementDate(RecordUtil.getDate());
			inventoryMovement.saveEx(transactionName);

			maybeMovement.set(inventoryMovement);
		});

		Movement.Builder builder = convertMovement(
			maybeMovement.get()
		);

		return builder;
	}



	@Override
	public void deleteMovement(DeleteMovementRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Empty.Builder builder = deleteMovement(request);
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

	private Empty.Builder deleteMovement(DeleteMovementRequest request) {
		if (request.getId() <= 0 && Util.isEmpty(request.getUuid(), true)) {
			throw new AdempiereException("@M_Movement_ID@ @NotFound@");
		}
		Trx.run(transactionName -> {
			int movementId = request.getId();
			if (movementId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
				movementId = RecordUtil.getIdFromUuid(this.tableName, request.getUuid(), transactionName);
			}
			if (movementId <= 0) {
				throw new AdempiereException("@M_Movement_ID@ @NotFound@");
			}

			MMovement movement = new MMovement(Env.getCtx(), movementId, transactionName);
			if (movement == null || movement.getM_Movement_ID() <= 0) {
				throw new AdempiereException("@M_Movement_ID@ @NotFound@");
			}
			if (!DocumentUtil.isDrafted(movement)) {
				throw new AdempiereException("@Invalid@ @M_Movement_ID@ " + movement.getDocumentNo());
			}
			movement.deleteEx(true);
		});

		return Empty.newBuilder();
	}



	@Override
	public void processMovement(ProcessMovementRequest request, StreamObserver<Movement> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			Movement.Builder builder = processMovement(request);
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

	private Movement.Builder processMovement(ProcessMovementRequest request) {
		AtomicReference<MMovement> shipmentReference = new AtomicReference<MMovement>();

		Trx.run(transactionName -> {
			int movementId = request.getId();
			if (movementId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
				movementId = RecordUtil.getIdFromUuid(this.tableName, request.getUuid(), transactionName);
			}
			if (movementId <= 0) {
				throw new AdempiereException("@M_Movement_ID@ @NotFound@");
			}

			MMovement movement = new MMovement(Env.getCtx(), movementId, transactionName);
			if (movement == null || movement.getM_Movement_ID() <= 0) {
				throw new AdempiereException("@M_Movement_ID@ @NotFound@");
			}
			if (movement.isProcessed()) {
				throw new AdempiereException("@M_Movement_ID@ @Processed@");
			}
			if (!movement.processIt(X_M_Movement.DOCACTION_Complete)) {
				log.warning("@ProcessFailed@ :" + movement.getProcessMsg());
				throw new AdempiereException("@ProcessFailed@ :" + movement.getProcessMsg());
			}
			movement.saveEx(transactionName);

			shipmentReference.set(movement);
		});

		Movement.Builder builder = convertMovement(
			shipmentReference.get()
		);

		return builder;
	}



	@Override
	public void createMovementLine(CreateMovementLineRequest request, StreamObserver<MovementLine> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			MovementLine.Builder builder = createMovementLine(request);
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

	private MovementLine.Builder createMovementLine(CreateMovementLineRequest request) {
		if (request.getMovementId() <= 0 && Util.isEmpty(request.getMovementUuid(), true)) {
			throw new AdempiereException("@M_Movement_ID@ @NotFound@");
		}
		if (request.getProductId() <= 0 && Util.isEmpty(request.getProductUuid(), true)) {
			throw new AdempiereException("@M_Product_ID@ @NotFound@");
		}
		if (request.getWarehouseId() <= 0 && Util.isEmpty(request.getWarehouseUuid(), true)) {
			throw new AdempiereException("@M_Warehouse_ID@ @NotFound@");
		}
		if (request.getWarehouseToId() <= 0 && Util.isEmpty(request.getWarehouseToUuid(), true)) {
			throw new AdempiereException("@M_WarehouseTo_ID@ @NotFound@");
		}

		AtomicReference<MMovementLine> movementLineReference = new AtomicReference<MMovementLine>();

		Trx.run(transactionName -> {
			int movementId = request.getMovementId();
			if (movementId <= 0) {
				movementId = RecordUtil.getIdFromUuid(this.tableName, request.getMovementUuid(), transactionName);
				if (movementId <= 0) {
					throw new AdempiereException("@M_Movement_ID@ @NotFound@");
				}
			}
			MMovement movement = new MMovement(Env.getCtx(), movementId, transactionName);
			if (movement == null || movement.getM_Movement_ID() <= 0) {
				throw new AdempiereException("@M_Movement_ID@ @NotFound@");
			}
			if (movement.isProcessed()) {
				throw new AdempiereException("@M_Movement_ID@ @Processed@");
			}

			int productId = request.getProductId();
			if (productId <= 0) {
				productId = RecordUtil.getIdFromUuid(I_M_Product.Table_Name, request.getProductUuid(), transactionName);
				if (productId <= 0) {
					throw new AdempiereException("@M_Product_ID@ @NotFound@");
				}
			}

			// warehouse and default locator
			int warehouseId = request.getWarehouseId();
			if (warehouseId <= 0) {
				warehouseId = RecordUtil.getIdFromUuid(I_M_Warehouse.Table_Name, request.getWarehouseUuid(), transactionName);
				if (warehouseId <= 0) {
					throw new AdempiereException("@M_Warehouse_ID@ @NotFound@");
				}
			}
			MWarehouse warehouse = MWarehouse.get(Env.getCtx(), warehouseId);
			if (warehouse == null || warehouse.getM_Warehouse_ID() <= 0) {
				throw new AdempiereException("@M_Warehouse_ID@ @NotFound@");
			}
			MLocator locator = warehouse.getDefaultLocator();
			if (locator == null || locator.getM_Locator_ID() <= 0) {
				throw new AdempiereException("@M_Locator_ID@ @NotFounds@");
			}

			// warehouse to and default locator to
			int warehouseToId = request.getWarehouseToId();
			if (warehouseToId <= 0) {
				warehouseToId = RecordUtil.getIdFromUuid(I_M_Warehouse.Table_Name, request.getWarehouseToUuid(), transactionName);
				if (warehouseToId <= 0) {
					throw new AdempiereException("@M_WarehouseTo_ID@ @NotFound@");
				}
			}
			MWarehouse warehouseTo = MWarehouse.get(Env.getCtx(), warehouseToId);
			MLocator locatorTo = warehouseTo.getDefaultLocator();
			if (locatorTo == null || locatorTo.getM_Locator_ID() <= 0) {
				throw new AdempiereException("@M_LocatorTo_ID@ @NotFounds@");
			}

			// is same locator
			if (locator.getM_Locator_ID() == locatorTo.getM_Locator_ID()) {
				throw new AdempiereException("@M_LocatorTo_ID@ == @M_Locator_ID@");
			}

			final String whereClause = "M_Movement_ID = ? "
				+ "AND M_Product_ID = ? "
				+ "AND M_Locator_ID = ? "
				+ "AND M_LocatorTo_ID = ? "
			;
			MMovementLine movementLine = new Query(
				Env.getCtx(),
				I_M_MovementLine.Table_Name,
				whereClause,
				transactionName
			)
				.setParameters(productId, movement.getM_Movement_ID(), locator.getM_Locator_ID(), locatorTo.getM_Locator_ID())
				.setClient_ID()
				.first();

			BigDecimal quantity = BigDecimal.ONE;
			if (request.getQuantity() != null) {
				quantity = ValueUtil.getBigDecimalFromDecimal(request.getQuantity());
				if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
					quantity = BigDecimal.ONE;
				}
			}

			if (movementLine == null) {
				movementLine = new MMovementLine(movement);
				movementLine.setM_Locator_ID(locator.getM_Locator_ID());
				movementLine.setM_LocatorTo_ID(locatorTo.getM_Locator_ID());
				movementLine.setM_Product_ID(productId);
			}

			movementLine.setMovementQty(quantity);
			movementLine.setDescription(
				ValueUtil.validateNull(request.getDescription())
			);
			movementLine.saveEx(transactionName);

			movementLineReference.set(movementLine);
		});

		MovementLine.Builder builder = convertMovementLine(
			movementLineReference.get()
		);

		return builder;
	}



	@Override
	public void deleteMovementLine(DeleteMovementLineRequest request, StreamObserver<Empty> responseObserver) {
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

	private Empty.Builder deleteShipmentLine(DeleteMovementLineRequest request) {
		if (request.getId() <= 0 && Util.isEmpty(request.getUuid(), true)) {
			throw new AdempiereException("@M_MovementLine_ID@ @NotFound@");
		}

		Trx.run(transactionName -> {
			int movementLineId = request.getId();
			if (movementLineId <= 0 && !Util.isEmpty(request.getUuid(), true)) {
				movementLineId = RecordUtil.getIdFromUuid(I_M_MovementLine.Table_Name, request.getUuid(), transactionName);
			}
			if (movementLineId <= 0) {
				throw new AdempiereException("@M_MovementLine_ID@ @NotFound@");
			}

			MMovementLine movementLine = new MMovementLine(Env.getCtx(), movementLineId, transactionName);
			if (movementLine == null || movementLine.getM_MovementLine_ID() <= 0) {
				throw new AdempiereException("@M_MovementLine_ID@ @NotFound@");
			}
			// Validate processed
			if (movementLine.isProcessed()) {
				throw new AdempiereException("@M_MovementLine_ID@ @Processed@");
			}
			movementLine.deleteEx(true);
		});

		return Empty.newBuilder();
	}



	@Override
	public void updateMovementLine(UpdateMovementLineRequest request, StreamObserver<MovementLine> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			MovementLine.Builder builder = updateMovementLine(request);
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

	private MovementLine.Builder updateMovementLine(UpdateMovementLineRequest request) {
		if (request.getId() <= 0 && Util.isEmpty(request.getUuid(), true)) {
			throw new AdempiereException("@M_MovementLine_ID@ @NotFound@");
		}

		AtomicReference<MMovementLine> movementLineReference = new AtomicReference<MMovementLine>();

		Trx.run(transactionName -> {
			int movementLineId = request.getId();
			if (movementLineId <= 0) {
				movementLineId = RecordUtil.getIdFromUuid(I_M_MovementLine.Table_Name, request.getUuid(), transactionName);
				if (movementLineId <= 0) {
					throw new AdempiereException("@M_MovementLine_ID@ @NotFound@");
				}
			}

			MMovementLine movementLine = new MMovementLine(Env.getCtx(), movementLineId, transactionName);
			if (movementLine == null || movementLine.getM_MovementLine_ID() <= 0) {
				throw new AdempiereException("@M_MovementLine_ID@ @NotFound@");
			}
			// Validate processed
			if (movementLine.isProcessed()) {
				throw new AdempiereException("@M_MovementLine_ID@ @Processed@");
			}

			BigDecimal quantity = ValueUtil.getBigDecimalFromDecimal(request.getQuantity());
			if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
				quantity = BigDecimal.ONE;
			}

			movementLine.setMovementQty(quantity);
			movementLine.setDescription(
				ValueUtil.validateNull(request.getDescription())
			);
			movementLine.saveEx(transactionName);

			movementLineReference.set(movementLine);
		});

		MovementLine.Builder builder = convertMovementLine(
			movementLineReference.get()
		);

		return builder;
	}



	@Override
	public void listMovementLines(ListMovementLinesRequest request, StreamObserver<ListMovementLinesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListMovementLinesResponse.Builder builderList = listMovementLines(request);
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

	MovementLine.Builder convertMovementLine(MMovementLine movementLine) {
		MovementLine.Builder builder = MovementLine.newBuilder();
		if (movementLine == null || movementLine.getM_MovementLine_ID() <= 0) {
			return builder;
		}

		builder.setId(movementLine.getM_MovementLine_ID())
			.setUuid(
				ValueUtil.validateNull(movementLine.getUUID())
			)
			.setProduct(
				convertProduct(movementLine.getM_Product_ID())
			)
			.setDescription(
				ValueUtil.validateNull(movementLine.getDescription())
			)
			.setQuantity(
				ValueUtil.getDecimalFromBigDecimal(movementLine.getMovementQty())
			)
			.setLine(movementLine.getLine())
		;
		MLocator locator = MLocator.get(Env.getCtx(), movementLine.getM_Locator_ID());
		if (locator != null && locator.getM_Locator_ID() > 0) {
			MWarehouse warehouse = MWarehouse.get(Env.getCtx(), locator.getM_Warehouse_ID());
			builder.setWarehouseId(warehouse.getM_Warehouse_ID())
				.setWarehouseUuid(
					ValueUtil.validateNull(warehouse.getUUID())
				)
			;
		}
		MLocator locatorTo = MLocator.get(Env.getCtx(), movementLine.getM_LocatorTo_ID());
		if (locatorTo != null && locatorTo.getM_Locator_ID() > 0) {
			MWarehouse warehouseTo = MWarehouse.get(Env.getCtx(), locatorTo.getM_Warehouse_ID());
			builder.setWarehouseId(warehouseTo.getM_Warehouse_ID())
				.setWarehouseUuid(
					ValueUtil.validateNull(warehouseTo.getUUID())
				)
			;
		}

		return builder;
	}

	private ListMovementLinesResponse.Builder listMovementLines(ListMovementLinesRequest request) {
		if (request.getMovementId() <= 0 && Util.isEmpty(request.getMovementUuid(), true)) {
			throw new AdempiereException("@M_Movement_ID@ @NotFound@");
		}
		int movementId = request.getMovementId();
		if (movementId <= 0) {
			movementId = RecordUtil.getIdFromUuid(this.tableName, request.getMovementUuid(), null);
			if (movementId <= 0) {
				throw new AdempiereException("@M_Movement_ID@ @NotFound@");
			}
		}

		Query query = new Query(
			Env.getCtx(),
			I_M_MovementLine.Table_Name,
			I_M_Movement.COLUMNNAME_M_Movement_ID + " = ?",
			null
		)
			.setParameters(movementId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;

		int count = query.count();
		String nexPageToken = null;
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListMovementLinesResponse.Builder builderList = ListMovementLinesResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list(MMovementLine.class)
			.forEach(movementLine -> {
				MovementLine.Builder builder = convertMovementLine(movementLine);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}

}
