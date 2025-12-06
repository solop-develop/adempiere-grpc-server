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

import com.solop.core.util.SequenceUtil;
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
import org.compiere.util.DB;
import org.compiere.util.Trx;
import org.solop.sp016.process.util.ConsignmentOrderGrouping;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generated Process for (Create AP Invoice From Sales)
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ConsolidateConsignmentSalesForInvoice extends ConsolidateConsignmentSalesForInvoiceAbstract {
	
	/**	Counter for created	*/
	/**	Lines	*/
	private Map<String, List<ConsignmentOrderGrouping>> productToOrderGroup;
	private Map<Integer, Integer> orderLineToConsignedConsolidate;
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
		consignmentConsolidateTable = MTable.get(getCtx(), "T_ConsignmentSales");
		consignmentDetailTable = MTable.get(getCtx(), "T_ConsignmentSalesDetail");
		if (consignmentConsolidateTable == null || consignmentConsolidateTable.get_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ T_ConsignmentSales @NotFound@");
		}
		if (consignmentDetailTable == null || consignmentDetailTable.get_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ T_ConsignmentSalesDetail @NotFound@");
		}

		consolidateByInvoice();
		consolidateByInventory();

		InsertParallel();
		return "";
	}

	private void consolidateByInventory(){
		//TODO: maybe Modify this like in Invoice
		String whereClause = "EXISTS (SELECT 1 FROM M_Inventory i WHERE i.M_Inventory_ID = M_InventoryLine.M_Inventory_ID AND i.DocStatus IN ('CO', 'CL')) " +
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
		String query = "SELECT i.DateInvoiced, i.AD_Org_ID, il.M_Product_ID, il.C_OrderLine_ID, (il.QtyInvoiced - COALESCE (cd.UsedQty,0)) AS UsedQty FROM C_InvoiceLine il" +
				" INNER JOIN C_Invoice i ON (i.C_Invoice_ID = il.C_Invoice_ID)" +
				" INNER JOIN C_BPartner bp ON (bp.C_BPartner_ID = i.C_BPartner_ID)" +
				" LEFT JOIN (SELECT SUM(cd.qty) AS UsedQty, cd.C_OrderLine_ID FROM C_ConsignmentDetail cd" +
				" GROUP BY cd.C_OrderLine_ID) cd ON (cd.C_OrderLine_ID = il.C_OrderLine_ID)" +
				" WHERE il.M_Product_ID IN (" +
				" SELECT ol.M_Product_ID FROM C_OrderLine ol" +
				" INNER JOIN C_Order o ON (ol.C_Order_ID  = o.C_Order_ID )" +
				" INNER JOIN C_BPartner bp ON (bp.C_BPartner_ID = o.C_BPartner_ID)" +
				" WHERE o.IsDropship ='Y'" +
				" AND o.IsSOTrx = 'Y'" +
				" AND bp.AD_OrgBP_ID IS NOT NULL" +
				" AND o.DocStatus IN ('CO', 'CL')" +
				" GROUP BY ol.M_Product_ID" +
				" )" +
				" AND i.IsSOTrx = 'Y'" +
				" AND i.Ref_Invoice_ID IS NULL " +
				" AND i.DocStatus IN ('CO', 'CL') " +
				" AND EXISTS (SELECT 1 FROM C_Order o2" +
				" INNER JOIN C_OrderLine ol2 ON (ol2.C_Order_ID = o2.C_Order_ID)" +
				" INNER JOIN C_Order purchaseOrder ON (purchaseOrder.C_Order_ID = o2.Ref_Order_ID)" +
				" INNER JOIN C_BPartner bp ON (bp.C_BPartner_ID = o2.C_BPartner_ID)" +
				" WHERE o2.IsDropship ='Y'" +
				" AND o2.IsSOTrx = 'Y'" +
				" AND bp.AD_OrgBP_ID IS NOT NULL" +
				" AND o2.DocStatus IN ('CO', 'CL')" +
				" AND purchaseOrder.AD_Org_ID = i.AD_Org_ID" +
				" AND ol2.M_Product_ID = il.M_Product_ID" +
				" )" +
				"AND (il.QtyInvoiced - COALESCE (cd.UsedQty,0)) > 0";

		DB.runResultSet(get_TrxName(), query, null, resultSet -> {
			while (resultSet.next()) {

				consolidateData(resultSet.getInt("M_Product_ID"), resultSet.getInt("AD_Org_ID"),
						resultSet.getBigDecimal("UsedQty"), null, resultSet.getInt("C_OrderLine_ID"),
						0, resultSet.getTimestamp("DateInvoiced"));
			}
		}).onFailure(throwable -> {
			throw new AdempiereException(throwable);
		});

	}

	private void consolidateData(int productId, int orgId, BigDecimal qty, BigDecimal qtyUsed, int orderLineId, int inventoryLineId, Timestamp dateDoc) {
		String searchKey = productId + "|" + orgId;
		List<ConsignmentOrderGrouping> orderLinesAndQtyList = productToOrderGroup.getOrDefault(searchKey, new ArrayList<>());
		if (orderLinesAndQtyList.isEmpty()) {
			//For the Consigned Sales Order Lines still open
			productToOrderGroup.put(searchKey, orderLinesAndQtyList);
			String whereClauseOrderLine = "M_Product_ID = ? AND QtyDelivered > QtyInvoiced AND EXISTS (SELECT 1 FROM C_Order o " +
					"INNER JOIN C_Order o2 ON (o2.C_Order_ID  = o.Ref_Order_ID) " +
					"WHERE o.IsDropShip = 'Y' AND o.IsSOTrx = 'Y' AND o.C_Order_ID = C_OrderLine.C_Order_ID " +
					"AND o2.AD_Org_ID = ?)";
			List<Integer> openSalesOrderLineIds = new Query(getCtx(), MOrderLine.Table_Name, whereClauseOrderLine, get_TrxName())
					.setParameters(productId, orgId)
					.setOrderBy("Created")
					.getIDsAsList();
			for (Integer openOrderLineId : openSalesOrderLineIds) {
				MOrderLine orderLine = new MOrderLine(getCtx(), openOrderLineId, get_TrxName());
				MOrder order = orderLine.getParent();
				BigDecimal maxQty = orderLine.getQtyDelivered().subtract(orderLine.getQtyInvoiced());
				ConsignmentOrderGrouping orderGroup = new ConsignmentOrderGrouping(maxQty, openOrderLineId, orderLine.getQtyDelivered());
				orderGroup.setProductId(productId);
				orderGroup.setOrderId(orderLine.getC_Order_ID());
				orderGroup.setDateDoc(dateDoc);
				orderGroup.setOrgId(order.getAD_Org_ID());
				orderGroup.setBusinessPartnerId(order.getC_BPartner_ID());
				orderGroup.setInventoryLineId(inventoryLineId);
				orderGroup.setClientSalesOrderLineId(orderLineId);
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
			if (invoiceQty.signum() <= 0) {

				break;
			}
		}
	}

	private void InsertParallel(){

		productToOrderGroup.entrySet().parallelStream().forEach(entry->{
			Trx.run(transactionName -> {
				List<ConsignmentOrderGrouping> orderLinesAndQtyList = entry.getValue();
				for (ConsignmentOrderGrouping orderGroup : orderLinesAndQtyList) {
					Integer consolidateId = orderLineToConsignedConsolidate.get(orderGroup.getOrderLineId());
					if (consolidateId == null) {
						List<Object> params = new ArrayList<>();
						params.add(orderGroup.getOrderLineId());
						params.add(orderGroup.getOrderId());
						params.add(orderGroup.getUsedAmount());
						params.add(getAD_PInstance_ID());
						params.add(orderGroup.getBusinessPartnerId());
						params.add(orderGroup.getProductId());
						params.add(orderGroup.getDateDoc());
						params.add(orderGroup.getMaxAmount());
						params.add(orderGroup.getDeliveredAmount());
						consolidateId = DB.getSQLValue(transactionName, "SELECT " +SequenceUtil.getNextSequenceSqlString("T_ConsignmentSales", false));
						params.add(consolidateId);
						params.add(getAD_Client_ID());
						params.add(orderGroup.getOrgId());
						DB.executeUpdateEx("INSERT INTO T_ConsignmentSales (C_OrderLine_ID, C_Order_ID, QtySold, AD_PInstance_ID, " +
										"C_BPartner_ID, M_Product_ID, DateInvoiced, QtyPending, QtyConsigned, T_ConsignmentSales_ID," +
										"AD_Client_ID, AD_Org_ID, Created, Updated, CreatedBy, UpdatedBy) VALUES (?, " +
										"?, ?, ?, ?, ?, " +
										"?, ?, ?, ?, ?,?, NOW(), NOW(), 0,0)",
								params.toArray(),
								transactionName);
						orderLineToConsignedConsolidate.put(orderGroup.getOrderLineId(), consolidateId);
					}

					List<Object> params = new ArrayList<>();
					params.add(orderGroup.getClientSalesOrderLineId());
					params.add(orderGroup.getInventoryLineId());
					params.add(orderGroup.getUsedAmount());
					params.add(consolidateId);
					params.add(getAD_Client_ID());
					params.add(orderGroup.getOrgId());
					DB.executeUpdateEx("INSERT INTO T_ConsignmentSalesDetail (C_OrderLine_ID, M_InventoryLine_ID, Qty, T_ConsignmentSales_ID, T_ConsignmentSalesDetail_ID," +
									"AD_Client_ID, AD_Org_ID, Created, Updated, CreatedBy, UpdatedBy) VALUES (?,?,?,?,"+SequenceUtil.getNextSequenceSqlString("T_ConsignmentSalesDetail", false) +",?,?, NOW(), NOW(), 0,0)",
							params.toArray(),
							transactionName);
				}
			});

		});

	}

}
