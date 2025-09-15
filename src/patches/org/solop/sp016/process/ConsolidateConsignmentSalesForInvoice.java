/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com				  		                 *
 *************************************************************************************/
package org.solop.sp016.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.solop.sp016.process.util.ConsignmentOrderGrouping;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generated Process for (Create AP Invoice From Sales)
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ConsolidateConsignmentSalesForInvoice extends ConsolidateConsignmentSalesForInvoiceAbstract {
	
	/**	Counter for created	*/
	/**	Lines	*/
	private Map<String, List<ConsignmentOrderGrouping>> productToOrderGroup;
	private Map<Integer, Integer> orderLineToConsignedConsolidate;
	private Map<Integer, BigDecimal> consolidateQty;
	@Override
	protected void prepare() {
		super.prepare();
	}

	MTable consignmentConsolidateTable;
	MTable consignmentDetailTable;

	@Override
	protected String doIt() throws Exception {
		productToOrderGroup = new HashMap<>();
		orderLineToConsignedConsolidate = new HashMap<>();
		consolidateQty = new HashMap<>();
		consignmentConsolidateTable = MTable.get(getCtx(), "T_ConsigmentSales");
		consignmentDetailTable = MTable.get(getCtx(), "T_ConsigmentSalesDetail");
		if (consignmentConsolidateTable == null || consignmentConsolidateTable.get_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ T_ConsigmentSales @NotFound@");
		}
		if (consignmentDetailTable == null || consignmentDetailTable.get_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ T_ConsigmentSalesDetail @NotFound@");
		}
		consolidateByInvoice();
		consolidateByInventory();
		consolidateQty.forEach((consolidateId, usedQty) -> {
			PO consolidate = consignmentConsolidateTable.getPO(consolidateId, get_TrxName());
			consolidate.set_ValueOfColumn("QtySold", usedQty);
			consolidate.saveEx();
		});
		return "";
	}

	private void consolidateByInventory(){
		String whereClause = "M_InventoryLine.Link_OrderLine_ID IS NULL " +
				"AND EXISTS (SELECT 1 FROM M_Inventory i WHERE i.M_Inventory_ID = M_InventoryLine.M_Inventory_ID AND i.DocStatus IN ('CO', 'CL')) " +
				"AND EXISTS (SELECT 1 FROM C_OrderLine ol2 " +
				"INNER JOIN C_Order o2 ON (o2.C_Order_ID = ol2.C_Order_ID) " +
				"WHERE  ol2.M_Product_ID = M_InventoryLine.M_Product_ID " +
				"AND (SELECT COALESCE(SUM(cd.Qty), 0) FROM C_ConsignmentDetail cd WHERE cd.M_InventoryLine_ID = M_InventoryLine.M_InventoryLine_ID) < M_InventoryLine.QtyInternalUse " +
				"AND o2.IsDropShip = 'Y' AND o2.IsSOTrx = 'N' " +
				"AND o2.DocStatus IN ('CO', 'CL') " +
				"AND (EXISTS ( SELECT 1 " +
				"       FROM adempiere.m_product_po pp " +
				"      WHERE pp.m_product_id = ol2.m_product_id AND o2.c_bpartner_id = pp.c_bpartner_id AND pp.isactive = 'Y'::bpchar AND pp.discontinued = 'N'::bpchar)) " +
				")";
		List<Integer> inventoryLineIds = new Query(getCtx(), MInventoryLine.Table_Name, whereClause, get_TrxName())
				.setClient_ID()
				.getIDsAsList();

		inventoryLineIds.forEach(inventoryLineId -> {

			MInventoryLine inventoryLine = new MInventoryLine(getCtx(), inventoryLineId, get_TrxName());
			MInventory inventory = inventoryLine.getParent();
			BigDecimal qtyUsed = new Query(getCtx(), "C_ConsignmentDetail", whereClause, get_TrxName())
				.setParameters(inventoryLine.get_ID())
				.sum("Qty");
			consolidateData(inventoryLine.getM_Product_ID(), inventory.getAD_Org_ID(), inventoryLine.getMovementQty().subtract(qtyUsed), qtyUsed, 0,inventoryLineId, inventory.getMovementDate());

		});

	}

	private void consolidateByInvoice(){
		String whereClauseInvoiceLine = "EXISTS (SELECT 1 FROM c_invoice i WHERE i.C_Invoice_ID = C_InvoiceLine.C_Invoice_ID AND i.IsSOTrx = 'Y' AND i.DocStatus IN ('CO', 'CL')) " +
				"AND EXISTS (SELECT 1 FROM C_OrderLine ol " +
				"INNER JOIN C_Order o ON (o.C_Order_ID = ol.C_Order_ID) " +
				"INNER JOIN C_OrderLine ol2 ON (ol2.M_Product_ID = ol.M_Product_ID) " +
				"INNER JOIN C_Order o2 ON (o2.C_Order_ID = ol2.C_Order_ID) " +
				"WHERE  ol.C_OrderLine_ID = C_InvoiceLine.C_OrderLine_ID " +
				"AND (SELECT COALESCE(SUM(cd.Qty), 0) FROM C_ConsignmentDetail cd WHERE cd.C_OrderLine_ID = ol.C_OrderLine_ID) < ol.QtyOrdered " +
				"AND o.DocStatus IN ('CO', 'CL') " +
				"AND o2.IsDropShip = 'Y' AND o2.IsSOTrx = 'N' " +
				"AND o2.DocStatus IN ('CO', 'CL') " +
				"AND (EXISTS ( SELECT 1 " +
				"       FROM adempiere.m_product_po pp " +
				"      WHERE pp.m_product_id = ol2.m_product_id AND o2.c_bpartner_id = pp.c_bpartner_id AND pp.isactive = 'Y'::bpchar AND pp.discontinued = 'N'::bpchar)) " +
				")";
		List<Integer> salesInvoiceLineIds = new Query(getCtx(), MInvoiceLine.Table_Name, whereClauseInvoiceLine, get_TrxName())
			.setClient_ID()
			.getIDsAsList();
		String whereClause = "C_OrderLine_ID = ?";
		salesInvoiceLineIds.forEach(salesInvoiceLineId -> {

			MInvoiceLine invoiceLine = new MInvoiceLine(getCtx(), salesInvoiceLineId, get_TrxName());
			BigDecimal qtyUsed = new Query(getCtx(), "C_ConsignmentDetail", whereClause, get_TrxName())
				.setParameters(invoiceLine.getC_OrderLine_ID())
				.sum("Qty");
			MInvoice invoice = invoiceLine.getParent();
			consolidateData(invoiceLine.getM_Product_ID(), invoice.getAD_Org_ID(), invoiceLine.getQtyInvoiced().subtract(qtyUsed), qtyUsed, invoiceLine.getC_OrderLine_ID(),0, invoice.getDateInvoiced());

		});
	}


	private void consolidateData(int productId, int orgId, BigDecimal qty, BigDecimal qtyUsed, int orderLineId, int inventoryLineId, Timestamp dateDoc) {
		String searchKey = productId + "|" + orgId;
		List<ConsignmentOrderGrouping> orderLinesAndQtyList = productToOrderGroup.getOrDefault(searchKey, new ArrayList<>());
		//TODO: Validar cantidad de Linea de Orden o de inventario contra lo asignado en C_ConsignmentDetail
		if (orderLinesAndQtyList.isEmpty()) {
			productToOrderGroup.put(searchKey, orderLinesAndQtyList);
			String whereClauseOrderLine = "M_Product_ID = ? AND QtyOrdered > QtyInvoiced AND EXISTS (SELECT 1 FROM C_Order o " +
					"INNER JOIN C_Order o2 ON (o2.C_Order_ID  = o.Ref_Order_ID) " +
					"WHERE o.IsDropShip = 'Y' AND o.IsSOTrx = 'Y' AND o.C_Order_ID = C_OrderLine.C_Order_ID " +
					"AND o2.AD_Org_ID = ?)";
			List<Integer> openSalesOrderLineIds = new Query(getCtx(), MOrderLine.Table_Name, whereClauseOrderLine, get_TrxName())
					.setParameters(productId, orgId)
					.setOrderBy("Created")
					.getIDsAsList();
			for (Integer openOrderLineId : openSalesOrderLineIds) {
				MOrderLine orderLine = new MOrderLine(getCtx(), openOrderLineId, get_TrxName());
				BigDecimal maxQty = orderLine.getQtyOrdered().subtract(orderLine.getQtyInvoiced());
				ConsignmentOrderGrouping orderGroup = new ConsignmentOrderGrouping(maxQty, openOrderLineId);
				orderGroup.setOrderId(orderLine.getC_Order_ID());
				orderLinesAndQtyList.add(orderGroup);
			}
		}
		BigDecimal invoiceQty = qty;
		for (ConsignmentOrderGrouping orderGroup : orderLinesAndQtyList) {
			BigDecimal maxQty = orderGroup.getMaxAmount();
			BigDecimal usedQty = orderGroup.getUsedAmount();
			BigDecimal availableQty = maxQty.subtract(usedQty);
			if (availableQty.signum() <= 0) {
				continue;
			}
			BigDecimal qtyToUse = invoiceQty;
			if (invoiceQty.compareTo(availableQty) > 0) {
				qtyToUse = availableQty;
			}
			invoiceQty = invoiceQty.subtract(qtyToUse);
			usedQty = usedQty.add(qtyToUse);
			orderGroup.setUsedAmount(usedQty);

			Integer consolidateId = orderLineToConsignedConsolidate.get(orderGroup.getOrderLineId());
			if (consolidateId == null) {
				MOrder order = new MOrder(getCtx(), orderGroup.getOrderId(), get_TrxName());

				PO consolidate = consignmentConsolidateTable.getPO(0, get_TrxName());
				consolidate.set_ValueOfColumn("C_OrderLine_ID", orderGroup.getOrderLineId());
				consolidate.set_ValueOfColumn("C_Order_ID", orderGroup.getOrderId());
				consolidate.set_ValueOfColumn("QtySold", BigDecimal.ZERO);
				consolidate.set_ValueOfColumn("AD_PInstance_ID", getAD_PInstance_ID());
				consolidate.set_ValueOfColumn("C_BPartner_ID", order.getC_BPartner_ID());
				consolidate.set_ValueOfColumn("M_Product_ID", productId);
				consolidate.set_ValueOfColumn("DateInvoiced", dateDoc);
				consolidate.set_ValueOfColumn("QtyPending", maxQty);
				consolidate.set_ValueOfColumn("QtyConsigned", qtyUsed);
				consolidate.saveEx();
				consolidateId = consolidate.get_ID();
				orderLineToConsignedConsolidate.put(orderGroup.getOrderLineId(), consolidateId);
			}
			consolidateQty.put(consolidateId, usedQty);
			PO consignmentDetail = consignmentDetailTable.getPO(0, get_TrxName());
			if (orderLineId > 0) {
				consignmentDetail.set_ValueOfColumn("C_OrderLine_ID", orderLineId);
			}
			if (inventoryLineId > 0) {
				consignmentDetail.set_ValueOfColumn("M_InventoryLine_ID", inventoryLineId);
			}
			consignmentDetail.set_ValueOfColumn("Qty", qtyToUse);
			consignmentDetail.set_ValueOfColumn("T_ConsigmentSales_ID", consolidateId);
			consignmentDetail.saveEx();
			if (invoiceQty.signum() <= 0) {

				break;
			}
		}
	}
}
