/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.solop.sp016.model.validator;

import org.adempiere.core.domains.models.*;
import org.compiere.acct.Fact;
import org.compiere.model.*;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.solop.sp016.util.ConsignedMaterialUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Useful method for Consignment Material
 * @author Yamel Senih ysenih@erpya.com
 *
 */
public class ConsignedMaterial implements ModelValidator, FactsValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(ConsignedMaterial.class);
	/** Client */
	private int clientId = -1;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		//	Add Persistence for IsDefault values
		engine.addDocValidate(I_C_Order.Table_Name, this);
		engine.addDocValidate(I_C_Invoice.Table_Name, this);
		engine.addModelChange(I_C_Invoice.Table_Name, this);
		engine.addModelChange(I_C_InvoiceLine.Table_Name, this);
		engine.addModelChange(I_C_Order.Table_Name, this);
		engine.addModelChange(I_M_Inventory.Table_Name, this);
		engine.addModelChange(I_M_Product_PO.Table_Name, this);
//		engine.addModelChange(I_M_InOut.Table_Name, this);
		

		//Support Currency Convert on Fact
		engine.addFactsValidate(MInOut.Table_Name, this);
		engine.addFactsValidate(MMatchInv.Table_Name, this);
	}
	
	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}

	@Override
	public String modelChange(PO entity, int type) throws Exception {
		if(type == TYPE_BEFORE_DELETE) {
			if(entity.get_TableName().equals(I_C_InvoiceLine.Table_Name)) {
				MInvoiceLine invoiceLine = (MInvoiceLine) entity;
				MInvoice invoice = invoiceLine.getParent();
				if(!invoice.isSOTrx()) {
					//	For Sales Order Lines
					ConsignedMaterialUtil.getLinkedSalesOrderLinesFromInvoiceLine(invoiceLine.getCtx(), invoiceLine.getC_InvoiceLine_ID(), invoiceLine.get_TrxName()).forEach(orderLine -> {
						orderLine.setLink_OrderLine_ID(-1);
						orderLine.saveEx(invoiceLine.get_TrxName());
					});
					//	For Inventory Line
					ConsignedMaterialUtil.getLinkedInventoryLinesFromInvoiceLine(invoiceLine.getCtx(), invoiceLine.getC_InvoiceLine_ID(), invoiceLine.get_TrxName()).forEach(inventoryLine -> {
						inventoryLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_Link_OrderLine_ID, null);
						inventoryLine.saveEx(invoiceLine.get_TrxName());
					});
				}
			}
		} else if(type == TYPE_BEFORE_NEW
				|| type == TYPE_BEFORE_CHANGE) {
			if(entity.get_TableName().equals(I_C_InvoiceLine.Table_Name)) {
				//	For AP Invoice
				if(entity.is_new()
						|| entity.is_ValueChanged(I_C_InvoiceLine.COLUMNNAME_C_OrderLine_ID)) {
					MInvoiceLine invoiceLine = (MInvoiceLine) entity;
					if(!invoiceLine.getParent().isSOTrx()) {
						ConsignedMaterialUtil.recalculateInvoiceLineRate(invoiceLine);
					}
				}
			} else if(entity.get_TableName().equals(I_C_Order.Table_Name)) {
				//	For Purchase Order set default value when is drop ship based on document type
				MOrder purchaseOrder = (MOrder) entity;
				if(!purchaseOrder.isSOTrx()
						&& purchaseOrder.getC_DocTypeTarget_ID() > 0
						&& (purchaseOrder.is_new()
						|| purchaseOrder.is_ValueChanged(I_C_Order.COLUMNNAME_C_DocTypeTarget_ID))) {
					MDocType documentType = MDocType.get(purchaseOrder.getCtx(), purchaseOrder.getC_DocTypeTarget_ID());
					purchaseOrder.setIsDropShip(documentType.get_ValueAsBoolean(MProduct.COLUMNNAME_IsDropShip));
				}
			} else if(entity.get_TableName().equals(I_M_Inventory.Table_Name)) {
				//	For Inventory Internal use set default value when is drop ship based on document type
				MInventory inventory = (MInventory) entity;
				if(inventory.getC_DocType_ID() > 0
						&& (inventory.is_new()
						|| inventory.is_ValueChanged(I_C_Order.COLUMNNAME_C_DocType_ID))) {
					MDocType documentType = MDocType.get(inventory.getCtx(), inventory.getC_DocType_ID());
					inventory.set_ValueOfColumn(MOrder.COLUMNNAME_IsDropShip, documentType.get_ValueAsBoolean(MProduct.COLUMNNAME_IsDropShip));
				}
			} else if(entity.get_TableName().equals(I_M_Product_PO.Table_Name)) {
				//	For AP Invoice
				if(entity.is_new()
						|| entity.is_ValueChanged(ConsignedMaterialUtil.COLUMNNAME_PriceLastLanded)) {
					MProductPO purchaseProduct = (MProductPO) entity;
					BigDecimal purchasePrice = Optional.ofNullable(purchaseProduct.getPricePO()).orElse(Env.ZERO);
					BigDecimal landedCostPrice = Optional.ofNullable((BigDecimal) purchaseProduct.get_Value(ConsignedMaterialUtil.COLUMNNAME_PriceLastLanded)).orElse(Env.ZERO);
					purchaseProduct.setPricePO(purchasePrice.add(landedCostPrice));
				}
			} else if(entity.get_TableName().equals(I_M_InOut.Table_Name)
					&& type == TYPE_BEFORE_CHANGE) {
//				MInOut inOut = (MInOut) entity;
//				if(inOut.getC_Order_ID() > 0
//						&& inOut.is_ValueChanged(I_M_InOut.COLUMNNAME_M_Warehouse_ID)
//						&& !inOut.isSOTrx()) {
//					MOrder order = (MOrder) inOut.getC_Order();
//					if(order.getM_Warehouse_ID() == inOut.getM_Warehouse_ID()
//							&& order.getAD_Org_ID() != inOut.get_ValueOldAsInt(I_M_InOut.COLUMNNAME_AD_Org_ID)) {
//						inOut.setM_Warehouse_ID(inOut.get_ValueOldAsInt(I_M_InOut.COLUMNNAME_M_Warehouse_ID));
//						inOut.setAD_Org_ID(inOut.get_ValueOldAsInt(I_M_InOut.COLUMNNAME_AD_Org_ID));
//					}
//				}
			}
		}
		return null;
	}

	@Override
	public String docValidate(PO entity, int timing) {
		if(timing == TIMING_BEFORE_COMPLETE) {
			if(entity.get_TableName().equals(I_C_Order.Table_Name)) {
				MOrder order = (MOrder) entity;
				if(!order.isSOTrx()) {
					//	Only Drop Ship
					Arrays.asList(order.getLines())
					.stream()
					.filter(orderLine -> orderLine.getM_Product_ID() > 0)
					.forEach(orderLine -> {
						List<MProductPO> purchaseProductList = MProductPO.getByPartner(orderLine.getCtx(), order.getC_BPartner_ID(), orderLine.getM_Product_ID(), order.get_TrxName());
						Optional<MProductPO> maybePurchaseProduct = purchaseProductList.stream().filter(puchaseProduct -> puchaseProduct.getC_Currency_ID() == order.getC_Currency_ID()).findFirst();
						if(maybePurchaseProduct.isPresent()) {	//	Update Price
							MProductPO purchaseProductToUpdate = maybePurchaseProduct.get();
							purchaseProductToUpdate.setPriceList(orderLine.getPriceActual());
							purchaseProductToUpdate.setPricePO(orderLine.getPriceActual());
							purchaseProductToUpdate.setPriceLastPO(orderLine.getPriceActual());
							purchaseProductToUpdate.setIsActive(true);
							purchaseProductToUpdate.setIsCurrentVendor(true);
							purchaseProductToUpdate.saveEx();
						} else {	//	Create New
							MProduct product = MProduct.get(order.getCtx(), orderLine.getM_Product_ID());
							MProductPO purchaseProductToCreate = new MProductPO(order.getCtx(), orderLine.getM_Product_ID(), order.getC_BPartner_ID(), order.getC_Currency_ID(), order.get_TrxName());
							purchaseProductToCreate.setVendorProductNo(product.getValue());
							purchaseProductToCreate.setC_UOM_ID(product.getC_UOM_ID());
							purchaseProductToCreate.setUPC(product.getUPC());
							purchaseProductToCreate.setPriceList(orderLine.getPriceList());
							purchaseProductToCreate.setPricePO(orderLine.getPriceActual());
							purchaseProductToCreate.setPriceLastPO(orderLine.getPriceActual());
							purchaseProductToCreate.setIsCurrentVendor(true);
							purchaseProductToCreate.saveEx();
						}
					});
				}
			} else if(entity.get_TableName().equals(I_C_Invoice.Table_Name)) {
				//	For Sales Invoice
				MInvoice invoice = (MInvoice) entity;
				if(invoice.isSOTrx()) {
					Arrays.asList(invoice.getLines(true))
						.stream()
						.filter(invoiceLine -> invoiceLine.getM_Product_ID() > 0 && Optional.ofNullable(invoiceLine.getPriceLimit()).orElse(Env.ZERO).compareTo(Env.ZERO) == 0)
						.forEach(invoiceLine -> {
							ConsignedMaterialUtil.setPriceLimitFromLastPurchase(invoiceLine);
							invoiceLine.saveEx();
						});
				}
			}
		} else if(timing == TIMING_AFTER_REVERSECORRECT
				|| timing == TIMING_AFTER_REVERSEACCRUAL
				|| timing == TIMING_AFTER_VOID) {
			if(entity.get_TableName().equals(I_C_Invoice.Table_Name)) {
				MInvoice invoice = (MInvoice) entity;
				if(!invoice.isSOTrx()) {
					//	For Sales Order Lines
					ConsignedMaterialUtil.getLinkedSalesOrderLinesFromInvoice(invoice.getCtx(), invoice.getC_Invoice_ID(), invoice.get_TrxName()).forEach(orderLine -> {
						orderLine.setLink_OrderLine_ID(-1);
						orderLine.saveEx(invoice.get_TrxName());
					});
					//	For Inventory Line
					ConsignedMaterialUtil.getLinkedInventoryLinesFromInvoiceLine(invoice.getCtx(), invoice.getC_Invoice_ID(), invoice.get_TrxName()).forEach(inventoryLine -> {
						inventoryLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_Link_OrderLine_ID, null);
						inventoryLine.saveEx(invoice.get_TrxName());
					});
				}
			} else if(entity.get_TableName().equals(I_C_Order.Table_Name)) {
				MOrder purchaseOrder = (MOrder) entity;
				//	Remove Linked Order Lines
				if(!purchaseOrder.isSOTrx()) {
					//	For Sales Order Lines
					ConsignedMaterialUtil.getLinkedSalesOrderLinesFromPurchaseOrder(purchaseOrder.getCtx(), purchaseOrder.getC_Order_ID(), purchaseOrder.get_TrxName()).forEach(orderLine -> {
						orderLine.setLink_OrderLine_ID(-1);
						orderLine.saveEx(purchaseOrder.get_TrxName());
					});
					//	For Inventory Lines
					ConsignedMaterialUtil.getLinkedInventoryLinesFromPurchaseOrder(purchaseOrder.getCtx(), purchaseOrder.getC_Order_ID(), purchaseOrder.get_TrxName()).forEach(inventoryLine -> {
						inventoryLine.set_ValueOfColumn(MOrderLine.COLUMNNAME_Link_OrderLine_ID, null);
						inventoryLine.saveEx(purchaseOrder.get_TrxName());
					});
				}
			}
		}
		return null;
	}

	@Override
	public String factsValidate(MAcctSchema schema, List<Fact> facts, PO po) {
		Optional.ofNullable(po).ifPresent(entity ->{
			if (entity.get_TableName().equals(MInOut.Table_Name)
					|| entity.get_TableName().equals(MMatchInv.Table_Name)) {
				facts.forEach(fact->{
					Arrays.asList(fact.getLines()).stream().forEach(fLine->{
						if (fLine.getM_Product_ID()!=0) {
							Optional.ofNullable(MProduct.get(entity.getCtx(), fLine.getM_Product_ID()))
									.ifPresent(product ->{
										if (product.isDropShip()) {
											fLine.setAmtSourceCr(Env.ZERO);
											fLine.setAmtSourceDr(Env.ZERO);
											fLine.setAmtAcctCr(Env.ZERO);
											fLine.setAmtAcctDr(Env.ZERO);
										}
							});
						}
					});
				});
			}
		});
		
		return null;
	}
}
