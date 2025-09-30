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
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDiscountSchema;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.DeleteOrderLineRequest;
import org.spin.backend.grpc.pos.DeleteOrderRequest;
import org.spin.backend.grpc.pos.Order;
import org.spin.backend.grpc.pos.ProcessReverseSalesRequest;
import org.spin.backend.grpc.pos.ReverseSalesRequest;
import org.spin.backend.grpc.pos.UpdateOrderRequest;
import org.spin.base.util.DocumentUtil;
import org.spin.pos.service.cash.CashManagement;
import org.spin.pos.service.payment.PaymentManagement;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.OrderConverUtil;
import org.spin.service.grpc.util.value.NumberManager;
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
		if (request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Order_ID@");
		}

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
	 * Delete order line from uuid
	 * @param request
	 * @return
	 */
	public static Empty.Builder deleteOrderLine(DeleteOrderLineRequest request) {
		if(request.getId() <= 0) {
			throw new AdempiereException("@C_OrderLine_ID@ @NotFound@");
		}
		Trx.run(transactionName -> {
			MOrderLine orderLine = new Query(
				Env.getCtx(),
				I_C_OrderLine.Table_Name,
				I_C_OrderLine.COLUMNNAME_C_OrderLine_ID + " = ?",
				transactionName
			)
				.setParameters(request.getId())
				.setClient_ID()
				.first()
			;
			if(orderLine != null
					&& orderLine.getC_OrderLine_ID() != 0) {
				//	Validate processed Order
				if(orderLine.isProcessed()) {
					throw new AdempiereException("@C_OrderLine_ID@ @Processed@");
				}
				if(orderLine != null
						&& orderLine.getC_Order_ID() >= 0) {
					MOrder order = orderLine.getParent();
					OrderManagement.validateOrderReleased(order);
					orderLine.deleteEx(true);
					//	Apply Discount from order
					DiscountManagement.configureDiscountRateOff(
						order,
						(BigDecimal) order.get_Value("FlatDiscount"),
						transactionName
					);
				}
			}
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
