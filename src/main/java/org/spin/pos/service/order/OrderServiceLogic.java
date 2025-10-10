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

package org.spin.pos.service.order;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDiscountSchema;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.MProduct;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.CreateOrderLineRequest;
import org.spin.backend.grpc.pos.DeleteOrderLineRequest;
import org.spin.backend.grpc.pos.DeleteOrderRequest;
import org.spin.backend.grpc.pos.Order;
import org.spin.backend.grpc.pos.OrderLine;
import org.spin.backend.grpc.pos.ProcessReverseSalesRequest;
import org.spin.backend.grpc.pos.ReverseSalesRequest;
import org.spin.backend.grpc.pos.UpdateManualOrderRequest;
import org.spin.backend.grpc.pos.UpdateOrderLineRequest;
import org.spin.backend.grpc.pos.UpdateOrderRequest;
import org.spin.base.util.DocumentUtil;
import org.spin.grpc.service.TimeControl;
import org.spin.pos.service.cash.CashManagement;
import org.spin.pos.service.payment.PaymentManagement;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.OrderConverUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;

import com.google.protobuf.Empty;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Sales Order / RMA Logic for backend of Point Of Sales form
 */
public class OrderServiceLogic {

	/**
	 * Update Order from ID
	 * @param request
	 * @return
	 */
	public static MOrder updateOrder(UpdateOrderRequest request) {
		AtomicReference<MOrder> orderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder salesOrder = OrderUtil.validateAndGetOrder(request.getId(), transactionName);
			if(!DocumentUtil.isDrafted(salesOrder)) {
				throw new AdempiereException("@C_Order_ID@ @Processed@");
			}

			MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

			Timestamp currentDate = TimeManager.getDate();
			CashManagement.validatePreviousCashClosing(pos, currentDate, transactionName);
			CashManagement.getCurrentCashClosing(pos, currentDate, true, transactionName);

			OrderManagement.validateOrderReleased(salesOrder);
			//	Update Date Ordered
			Timestamp now = TimeUtil.getDay(System.currentTimeMillis());
			salesOrder.setDateOrdered(now);
			salesOrder.setDateAcct(now);
			salesOrder.setDatePromised(now);
			//	POS
			if(request.getPosId() > 0 && salesOrder.getC_POS_ID() <= 0) {
				salesOrder.setC_POS_ID(request.getPosId());
			}
			//	Document Type
			if(request.getDocumentTypeId() > 0) {
				int documentTypeId = request.getDocumentTypeId();
				if(documentTypeId > 0 && documentTypeId != salesOrder.getC_DocTypeTarget_ID()) {
					salesOrder.setC_DocTypeTarget_ID(documentTypeId);
					salesOrder.setC_DocType_ID(documentTypeId);
					//	Set Sequenced No
					String value = DB.getDocumentNo(documentTypeId, transactionName, false, salesOrder);
					if (value != null) {
						salesOrder.setDocumentNo(value);
					}
				}
			}
			//	Business partner
			if(request.getCustomerId() > 0 && salesOrder.getC_POS_ID() > 0) {
				OrderManagement.configureBPartner(salesOrder, request.getCustomerId(), transactionName);
			}
			//	Description
			if(!Util.isEmpty(request.getDescription())) {
				salesOrder.setDescription(request.getDescription());
			}
			//	Campaign
			int campaignId = request.getCampaignId();
			if(campaignId > 0 && campaignId != salesOrder.getC_Campaign_ID()) {
				salesOrder.setC_Campaign_ID(campaignId);
				// update campaign on lines
				for (MOrderLine orderLine: salesOrder.getLines()) {
					orderLine.setC_Campaign_ID(campaignId);
					orderLine.saveEx(transactionName);
				}
			}
			//	Price List
			int priceListId = request.getPriceListId();
			if(priceListId > 0 && priceListId != salesOrder.getM_PriceList_ID()) {
				salesOrder.setM_PriceList_ID(priceListId);
				salesOrder.saveEx(transactionName);
				OrderManagement.configurePriceList(salesOrder);
			}
			//	Warehouse
			int warehouseId = request.getWarehouseId();
			if(warehouseId > 0) {
				salesOrder.setM_Warehouse_ID(warehouseId);
				salesOrder.saveEx(transactionName);
				OrderManagement.configureWarehouse(salesOrder);
			}
			//	Discount Amount
			final BigDecimal discountRate = NumberManager.getBigDecimalFromString(
				request.getDiscountRate()
			);
			if(discountRate != null) {
				final int paymentMethodId = request.getPaymentMethodId();
				if (paymentMethodId > 0) {
					PO paymentTypeAllocation = POS.getPaymentTypeAllocation(paymentMethodId, transactionName);
					if (paymentTypeAllocation == null || paymentTypeAllocation.get_ID() <= 0) {
						throw new AdempiereException("@C_POSPaymentTypeAllocation_ID@ @NotFound@");
					}
					final boolean isDiscountApply = salesOrder.get_ValueAsBoolean("IsAutoDiscountApplied");
					final boolean isDiscountPaymentMethod = paymentTypeAllocation.get_ValueAsBoolean("IsAllowsApplyDiscount");
					boolean isRecalculateDiscount = false;
					if (!isDiscountApply && isDiscountPaymentMethod) {
						isRecalculateDiscount = true;
					} else if (isDiscountApply && !isDiscountPaymentMethod) {
						isRecalculateDiscount = true;
					} else if (isDiscountApply && isDiscountPaymentMethod) {
						isRecalculateDiscount = true;
					}
					salesOrder.set_ValueOfColumn("IsAutoDiscountApplied", isDiscountPaymentMethod);
					salesOrder.saveEx(transactionName);

					if (isRecalculateDiscount) {
						DiscountManagement.configureDiscount(salesOrder, discountRate, transactionName);
					}
				} else {
					DiscountManagement.configureDiscount(salesOrder, discountRate, transactionName);
				}
			}
			//	Discount Off
			BigDecimal discountRateOff = NumberManager.getBigDecimalFromString(
				request.getDiscountRateOff()
			);
			BigDecimal discountAmountOff = NumberManager.getBigDecimalFromString(
				request.getDiscountAmountOff()
			);
			if (request.getDiscountSchemaId() > 0) {
				MDiscountSchema discountSchema = MDiscountSchema.get(
					Env.getCtx(),
					request.getDiscountSchemaId()
				);
				// discountRateOff = discountSchema.getFlatDiscount();
				DiscountManagement.configureDiscount(salesOrder, discountSchema.getFlatDiscount(), transactionName);
			}
			if(discountRateOff != null) {
				DiscountManagement.configureDiscountRateOff(salesOrder, discountRateOff, transactionName);
			} else if(discountAmountOff != null) {
				DiscountManagement.configureDiscountAmountOff(salesOrder, discountAmountOff, transactionName);
			}

			// Sales Representative
			if (request.getSalesRepresentativeId() > 0) {
				salesOrder.setSalesRep_ID(request.getSalesRepresentativeId());
			}

			//	Save
			salesOrder.saveEx(transactionName);
			salesOrder.load(transactionName);
			orderReference.set(salesOrder);
		});

		//	Return order
		return orderReference.get();
	}

	public static Order.Builder updateManualOrder(UpdateManualOrderRequest request) {
		AtomicReference<MOrder> orderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder salesOrder = OrderUtil.validateAndGetOrder(request.getId(), transactionName);
			if(!DocumentUtil.isDrafted(salesOrder)) {
				throw new AdempiereException("@C_Order_ID@ @Processed@");
			}
			// if (salesOrder.get_ValueAsBoolean("IsManual")) {
			// 	throw new AdempiereException("@C_Order_ID@ @IsManual@");
			// }

			final String manualInvociceDocumentNo = request.getManualInvociceDocumentNo();
			salesOrder.set_ValueOfColumn("ManualInvoiceDocumentNo", manualInvociceDocumentNo);

			final String manualShipmentDocumentNo = request.getManualShipmentDocumentNo();
			salesOrder.set_ValueOfColumn("ManualShipmentDocumentNo", manualShipmentDocumentNo);

			// final String manualMovementDocumentNo = request.getManualMovementDocumentNo();
			// salesOrder.set_ValueOfColumn("ManualMovementDocumentNo", manualMovementDocumentNo);

			salesOrder.saveEx();
		});

		return OrderConverUtil.convertOrder(
			orderReference.get()
		);
	}

	/**
	 * Delete order from id
	 * @param request
	 * @return
	 */
	public static Empty.Builder deleteOrder(DeleteOrderRequest request) {
		int orderId = request.getId();
		MOrder order = OrderUtil.validateAndGetOrder(orderId);
		//	Validate drafted
		if(!DocumentUtil.isDrafted(order)) {
			throw new AdempiereException("@C_Order_ID@ @Processed@");
		}
		//	Validate processed Order
		if(order.isProcessed()) {
			throw new AdempiereException("@C_Order_ID@ @Processed@");
		}
		//
		OrderManagement.validateOrderReleased(order);
		order.deleteEx(true);

		//	Return
		return Empty.newBuilder();
	}



	/**
	 * Create order line and return this
	 * @param request
	 * @return
	 */
	public static OrderLine.Builder createAndConvertOrderLine(CreateOrderLineRequest request) {
		//	Validate Order
		final int orderId = request.getOrderId();
		if(orderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_OrderLine_ID@");
		}
		//	Validate Product and charge
		if(request.getProductId() <= 0
				&& request.getChargeId() <= 0
				&& request.getResourceAssignmentId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_Product_ID@ / @C_Charge_ID@ / @S_ResourceAssignment_ID@");
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

	private static MOrderLine addOrderLineFromResourceAssigment(int orderId, int resourceAssignmentId, int warehouseId) {
		if(orderId <= 0) {
			return null;
		}
		//	
		AtomicReference<MOrderLine> orderLineReference = new AtomicReference<MOrderLine>();
		Trx.run(transactionName -> {
			MOrder order = new MOrder(Env.getCtx(), orderId, transactionName);
			//	Valid Complete
			if (!DocumentUtil.isDrafted(order)) {
				throw new AdempiereException("@C_Order_ID@ (#" + order.getDocumentNo() + ") @Processed@");
			}
			// OrderManagement.validateOrderReleased(order);

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
					"S_Resource_ID = ?",
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
				description.append(": ").append(
					DisplayType.getDateFormat(DisplayType.DateTime)
						.format(resourceAssigment.getAssignDateFrom())
				);
				description.append(" ~ ").append(
					DisplayType.getDateFormat(DisplayType.DateTime)
						.format(resourceAssigment.getAssignDateTo())
				);
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
	private static MOrderLine addOrderLine(int orderId, int productId, int chargeId, int warehouseId, BigDecimal quantity) {
		if(orderId <= 0) {
			return null;
		}
		//	
		AtomicReference<MOrderLine> orderLineReference = new AtomicReference<MOrderLine>();
		Trx.run(transactionName -> {
			MOrder order = new MOrder(Env.getCtx(), orderId, transactionName);
			//	Valid Complete
			if (!DocumentUtil.isDrafted(order)) {
				throw new AdempiereException("@C_Order_ID@ (#" + order.getDocumentNo() + ") @Processed@");
			}
			// OrderManagement.validateOrderReleased(order);

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
			MOrderLine orderLine = new Query(
				Env.getCtx(),
				I_C_OrderLine.Table_Name,
				whereClause.toString(),
				transactionName
			)
				.setParameters(parameters)
				.first()
			;
			if (orderLine != null && orderLine.getC_OrderLine_ID() > 0) {
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
	 * Create order line and return this
	 * @param request
	 * @return
	 */
	public static OrderLine.Builder updateAndConvertOrderLine(UpdateOrderLineRequest request) {
		//	Validate Order
		final int orderLineId = request.getId();
		if(orderLineId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_OrderLine_ID@");
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
	 * Delete order line from uuid
	 * @param request
	 * @return
	 */
	public static Empty.Builder deleteOrderLine(DeleteOrderLineRequest request) {
		final int orderLineId = request.getId();
		if(orderLineId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_OrderLine_ID@");
		}
		Trx.run(transactionName -> {
			MOrderLine orderLine = new MOrderLine(Env.getCtx(), orderLineId, transactionName);
			if (orderLine == null || orderLine.getC_OrderLine_ID() <= 0) {
				throw new AdempiereException("@C_OrderLine_ID@ (" + orderLineId + ") @NotFound@");
			}
			if(orderLine.isProcessed()) {
				throw new AdempiereException("@C_OrderLine_ID@ (#" + orderLine.getLine() + ") @Processed@");
			}
			//	Validate processed Order
			if(orderLine.isProcessed()) {
				throw new AdempiereException("@C_OrderLine_ID@ (#" + orderLine.getLine() + ") @Processed@");
			}

			MOrder order = orderLine.getParent();
			if (!DocumentUtil.isDrafted(order)) {
				throw new AdempiereException("@C_Order_ID@ (#" + order.getDocumentNo() + ") @Processed@");
			}
			OrderManagement.validateOrderReleased(order);
			orderLine.deleteEx(true);
			//	Apply Discount from order
			DiscountManagement.configureDiscountRateOff(
				order,
				(BigDecimal) order.get_Value("FlatDiscount"),
				transactionName
			);
		});
		//	Return
		return Empty.newBuilder();
	}


	/**
	 * Reverse Sales Transaction
	 * @param request ReverseSalesRequest
	 * @return Order.Builder
	 */
	public static Order.Builder reverseSalesTransaction(ReverseSalesRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_POS_ID@");
		}
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if (pos.getC_POS_ID() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		int orderId = request.getId();
		if (orderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@");
		}

		MOrder returnOrder = null;
		// Exists Online Payment Approved
		boolean isOnlinePaymentApproved = PaymentManagement.isOrderWithOnlinePaymentApproved(
			orderId
		);
		returnOrder = ReverseSalesTransaction.returnSalesOrder(
			pos,
			orderId,
			request.getDescription(),
			!isOnlinePaymentApproved
		);
		//	Default
		Order.Builder builder = OrderConverUtil.convertOrder(returnOrder);

		return builder;
	}


	/**
	 * Reverse Sales Transaction
	 * @param request ReverseSalesRequest
	 * @return Order.Builder
	 */
	public static Order.Builder processReverseSales(ProcessReverseSalesRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_POS_ID@");
		}
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if (pos.getC_POS_ID() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		final int salesOrderId = request.getSourceOrderId();
		if (salesOrderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @ECA14_Source_Order_ID@");
		}
		final int returnOrderId = request.getId();
		if (returnOrderId <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_RMA_ID@");
		}

		AtomicReference<MOrder> returnOrderReference = new AtomicReference<MOrder>();
		Trx.run(transactionName -> {
			MOrder returnOrder = new MOrder(Env.getCtx(), returnOrderId, transactionName);

			MOrder sourceOrder = new MOrder(Env.getCtx(), salesOrderId, transactionName);
			returnOrder = ReverseSalesTransaction.processReverseSalesOrder(
				pos,
				sourceOrder,
				returnOrder,
				transactionName
			);
			returnOrderReference.set(returnOrder);
		});
		//	Default
		Order.Builder builder = OrderConverUtil.convertOrder(
			returnOrderReference.get()
		);
		return builder;
	}

}
