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
import org.adempiere.process.util.InvoiceGrouping;
import org.compiere.model.MDocType;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generated Process for (Create AP Invoice From Sales)
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class CreateARInvoiceFromSalesDropShipment extends CreateARInvoiceFromSalesDropShipmentAbstract {
	
	/**	Counter for created	*/
	private AtomicInteger created = new AtomicInteger();
	/**	Lines	*/
	private Map<String, List<PO>> groupedOrderLines = new HashMap<>();
	MTable consignmentConsolidateTable;
	MTable consignmentConsolidateDetailTable;
	MTable consignmentDetailTable;
	/**	Invoice Grouping	*/
	private InvoiceGrouping invoiceGrouping;
	/**	Document Multiplier	*/
	private BigDecimal documentMultiplier = Env.ONE;
	@Override
	protected void prepare() {
		super.prepare();
		MDocType documentType = MDocType.get(getCtx(), getDocTypeTargetId());
		if(documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)) {
			documentMultiplier = documentMultiplier.negate();
		}
	}

	@Override
	protected String doIt() throws Exception {
		invoiceGrouping = InvoiceGrouping.newInstance();
		consignmentConsolidateTable = MTable.get(getCtx(), "T_ConsigmentSales");
		consignmentConsolidateDetailTable = MTable.get(getCtx(), "T_ConsigmentSalesDetail");
		consignmentDetailTable = MTable.get(getCtx(), "C_ConsignmentDetail");
		//	Process selections
		getSelectionKeys().forEach(consolidateId -> {
			PO consolidate = consignmentConsolidateTable.getPO(consolidateId, get_TrxName());
			MOrder order = new MOrder(getCtx(), consolidate.get_ValueAsInt("C_Order_ID"), get_TrxName());


			String keyString = invoiceGrouping.getKey(order, getDocTypeTargetId(), isConsolidateDocument());
			List<PO> lines = groupedOrderLines.getOrDefault(keyString, new ArrayList<>());
			lines.add(consolidate);
			groupedOrderLines.put(keyString, lines);

		});


		createInvoices();

		return "@Created@ " + created;
	}

	private void createInvoices() {
		groupedOrderLines.forEach((key, value) -> {


			AtomicReference<MInvoice> maybeInvoice = new AtomicReference<>();
			value.forEach(consolidate -> {
				MInvoice invoice = maybeInvoice.get();
				MOrder order = new MOrder(getCtx(), consolidate.get_ValueAsInt("C_Order_ID"), get_TrxName());
				if (invoice == null) {
					invoice = new MInvoice(order, getDocTypeTargetId(), getDateDoc());
					invoice.setDocAction(getDocAction());
					invoice.setDocStatus(DocAction.STATUS_Drafted);
					//TODO: Set data
					invoice.saveEx();
				}
				MInvoiceLine invoiceLine = new MInvoiceLine(invoice);

				MOrderLine salesOrderLine = new MOrderLine(getCtx(), consolidate.get_ValueAsInt("C_OrderLine_ID"), get_TrxName());
				invoiceLine.setOrderLine(salesOrderLine);

				int uOMId = salesOrderLine.getC_UOM_ID();
				BigDecimal qtyEntered = Env.ZERO;
				BigDecimal qtyInvoiced = (BigDecimal) consolidate.get_Value("Qty");
				MProduct product = MProduct.get(Env.getCtx(), invoiceLine.getM_Product_ID());
				if(uOMId != product.getC_UOM_ID()) {
					MUOM orderUom = MUOM.get(getCtx(), uOMId);
					qtyEntered = MUOMConversion.convertProductFrom(getCtx(), product.getM_Product_ID(), uOMId, qtyInvoiced);
					if(qtyEntered == null) {
						qtyEntered = qtyInvoiced;
					}
					qtyEntered = qtyEntered.setScale(orderUom.getStdPrecision(), RoundingMode.HALF_DOWN);
				} else {
					qtyEntered = qtyInvoiced;
				}
				invoiceLine.setQtyEntered(qtyEntered);
				invoiceLine.setQtyInvoiced(qtyInvoiced);
				invoiceLine.setC_UOM_ID(uOMId);
				//	Save
				invoiceLine.saveEx();
				created.getAndIncrement();
				addLog("@DocumentNo@ " + invoice.getDocumentNo());
				assignOrderLines(invoiceLine.getM_Product_ID(), consolidate.get_ValueAsInt("C_Order_ID"), consolidate.get_ID());
				if (!Util.isEmpty(getDocAction(), true)){
					if (!invoice.processIt(getDocAction())){
						throw new AdempiereException(invoice.getProcessMsg());
					}
				}
			});



		});
	}

	private void assignOrderLines(int productId, int orderId, int consolidateId){
		String whereClause = "M_Product_ID = ? AND EXISTS (SELECT 1 FROM C_Order o WHERE o.C_Order_ID = C_OrderLine.C_Order_ID AND o.Ref_Order_ID = ?)";
		MOrderLine originalPurchaseOrderLine = new Query(getCtx(), MOrderLine.Table_Name, whereClause, get_TrxName())
				.setParameters(productId, orderId)
				.first();
		whereClause = "C_OrderLine_ID IN (SELECT C_OrderLine_ID FROM T_ConsigmentSalesDetail cd WHERE cd.T_ConsigmentSales_ID = ?)";
		List<Integer> salesOrderLineIds = new Query(getCtx(), MOrderLine.Table_Name, whereClause, get_TrxName())
				.setParameters(consolidateId)
				.getIDsAsList();
		salesOrderLineIds.forEach(salesOrderLineId ->{
			MOrderLine salesOrderLine = new MOrderLine(getCtx(), salesOrderLineId, get_TrxName());
			salesOrderLine.setLink_OrderLine_ID(originalPurchaseOrderLine.get_ID());
			salesOrderLine.addDescription(Msg.parseTranslation(getCtx(), "@POReference@ ") + originalPurchaseOrderLine.getParent().getDocumentNo());
			salesOrderLine.saveEx();
		});

		whereClause = "T_ConsigmentSales_ID = ?";
		List<Integer> consolidateDetailIds = new Query(getCtx(), "T_ConsigmentSalesDetail", whereClause, get_TrxName())
				.setParameters(consolidateId)
				.getIDsAsList();
		consolidateDetailIds.forEach(consolidateDetailId ->{
			PO consolidateDetail = consignmentConsolidateDetailTable.getPO(consolidateDetailId, get_TrxName());
			int salesOrderLineId = consolidateDetail.get_ValueAsInt("C_OrderLine_ID");
			int inventoryLineId = consolidateDetail.get_ValueAsInt("M_InventoryLine_ID");
			BigDecimal qty = (BigDecimal) consolidateDetail.get_Value("Qty");

			PO consignmentDetail = consignmentDetailTable.getPO(0, get_TrxName());

			if (salesOrderLineId > 0) {
				MOrderLine salesOrderLine = new MOrderLine(getCtx(), salesOrderLineId, get_TrxName());
				salesOrderLine.setLink_OrderLine_ID(originalPurchaseOrderLine.get_ID());
				salesOrderLine.addDescription(Msg.parseTranslation(getCtx(), "@POReference@ ") + originalPurchaseOrderLine.getParent().getDocumentNo());
				salesOrderLine.saveEx();
				consignmentDetail.set_ValueOfColumn("C_OrderLine_ID", salesOrderLineId);
			}
			if (inventoryLineId > 0) {
				MInventoryLine inventoryLine = new MInventoryLine(getCtx(), inventoryLineId, get_TrxName());
				inventoryLine.set_ValueOfColumn("Link_OrderLine_ID", originalPurchaseOrderLine.get_ID());
				inventoryLine.addDescription(Msg.parseTranslation(getCtx(), "@POReference@ ") + originalPurchaseOrderLine.getParent().getDocumentNo());
				inventoryLine.saveEx();
				consignmentDetail.set_ValueOfColumn("M_InventoryLine_ID", salesOrderLineId);
			}
			consignmentDetail.set_ValueOfColumn("SO_OrderLine_ID", originalPurchaseOrderLine.get_ID());
			consignmentDetail.set_ValueOfColumn("Qty", qty);
			consignmentDetail.saveEx();



		});





	}
}