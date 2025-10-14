/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.spin.wms.process;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.process.util.InvoiceGrouping;
import org.compiere.model.*;
import org.compiere.util.Trx;
import org.eevolution.wms.model.MWMInOutBound;
import org.eevolution.wms.model.MWMInOutBoundLine;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for generate invoices from outbound orders
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * @version Release 3.9.3
 * See: https://github.com/adempiere/adempiere/issues/2730
 */
public class GenerateInvoiceInOutBound extends GenerateInvoiceInOutBoundAbstract {
	private HashMap<String, List<MWMInOutBoundLine>> groupedOutBoundLines;
	List<PO> invoicesToPrint;
	private int created = 0;
	private final AtomicInteger withError = new AtomicInteger(0);
	private final StringBuffer generatedDocuments = new StringBuffer();
	private InvoiceGrouping grouping;

	@Override
	protected String doIt() throws Exception {
		grouping = InvoiceGrouping.newInstance();
		groupedOutBoundLines = new HashMap<>();
		invoicesToPrint = new ArrayList<PO>();
		List<MWMInOutBoundLine> outBoundLines = null;
		//	Get from record
		if(isSelection()) {
			// Overwrite table RV_WM_InOutBoundLine by WM_InOutBoundLine
			getProcessInfo().setTableSelectionId(MWMInOutBoundLine.Table_ID);
			outBoundLines = (List<MWMInOutBoundLine>) getInstancesForSelection(get_TrxName());
		} else if(getRecord_ID() > 0) {
			outBoundLines = new Query(getCtx(), MWMInOutBoundLine.Table_Name, MWMInOutBound.COLUMNNAME_WM_InOutBound_ID + "=?", get_TrxName())
					.setParameters(getRecord_ID())
					.setOrderBy(MWMInOutBoundLine.COLUMNNAME_C_Order_ID + ", " + MWMInOutBoundLine.COLUMNNAME_DD_Order_ID)
					.list();
		}
		//	Create
		if(outBoundLines != null) {
			outBoundLines.stream()
					.filter(outBoundLine -> outBoundLine.getQtyToInvoice().signum() > 0 || isIncludeNotAvailable())
					.forEach(this::groupOutBoundLine);
			createAndProcessInvoices();
			printDocument(invoicesToPrint, true);
		}
		return "@Created@ " + created + (generatedDocuments.length() > 0? " [" + generatedDocuments + "]": "") +  (withError.get() > 0 ? " | @Error@ " + withError.get() : "");
	}

	private void createAndProcessInvoices() {
		groupedOutBoundLines.entrySet().stream().filter(Objects::nonNull).forEach(entry -> {
			try {
				Map<Integer, Boolean> orderCache = new HashMap<>();
				Trx.run(transactionName -> {
					List<MWMInOutBoundLine> lines = entry.getValue();
					AtomicReference<MInvoice> maybeInvoice = new AtomicReference<>();
					lines.stream().filter(outboundLine -> {
						MOrderLine orderLine = outboundLine.getOrderLine();
						MOrder order = orderLine.getParent();
						if(order.getInvoiceRule().equals(MOrder.INVOICERULE_AfterOrderDelivered) && outboundLine.getM_InOut_ID() <= 0) {
							if(orderCache.containsKey(outboundLine.getC_Order_ID())) {
								return false;
							}
							addLog("@Error@ @C_Order_ID@ " + order.getDocumentNo() + " - " + "@M_InOut_ID@ @NotFound@");
							orderCache.put(outboundLine.getC_Order_ID(), true);
							return false;
						}
						if(order.getInvoiceRule().equals(MOrder.INVOICERULE_AfterDelivery)) {
							if(outboundLine.getM_InOut_ID() <= 0) {
								MProduct product = MProduct.get(getCtx(), outboundLine.getM_Product_ID());
								addLog("@Error@ @C_Order_ID@ " + order.getDocumentNo() + " - " + "@M_InOut_ID@ @NotFound@: " + product.getValue() + " - " + product.getName());
								return false;
							}
						}
						return true;
					}).forEach(outboundLine -> {
						MOrderLine orderLine = outboundLine.getOrderLine();
						MOrder order = orderLine.getParent();
						MInvoice invoice = maybeInvoice.get();
						if (invoice == null) {
							invoice = new MInvoice(order, 0, getDateInvoiced());
							if(getDocTypeTargetId() > 0) {
								invoice.setC_DocType_ID(getDocTypeTargetId());
							}
							invoice.setIsSOTrx(true);
							invoice.saveEx(transactionName);
							maybeInvoice.set(invoice);
						}
						BigDecimal qtyInvoiced = getSalesOrderQtyToInvoice(outboundLine);
						MInvoiceLine invoiceLine = new MInvoiceLine(outboundLine.getCtx(), 0 , transactionName);
						invoiceLine.setOrderLine(orderLine);
						// Set Shipment Line
						if (outboundLine.getM_InOutLine_ID() > 0) {
							invoiceLine.setM_InOutLine_ID(outboundLine.getM_InOutLine_ID());
						}
						invoiceLine.setC_Invoice_ID(invoice.get_ID());
						invoiceLine.setC_UOM_ID(outboundLine.getC_UOM_ID());
						invoiceLine.setPrice(MUOMConversion.convertProductTo(getCtx(), outboundLine.getM_Product_ID(), outboundLine.getC_UOM_ID(), orderLine.getPriceActual()));
						invoiceLine.setQtyEntered(qtyInvoiced);
						invoiceLine.setQtyInvoiced(qtyInvoiced);
						invoiceLine.setWM_InOutBoundLine_ID(outboundLine.get_ID());
						invoiceLine.saveEx();
					});
					MInvoice invoice = maybeInvoice.get();
					if(invoice != null) {
						invoice.setDocAction(getDocAction());
						if (!invoice.processIt(getDocAction())) {
							addLog("@ProcessFailed@ : " + invoice.getProcessMsg());
							throw new AdempiereException("@ProcessFailed@ :" + invoice.getProcessMsg());
						}
						invoice.saveEx();
						created++;
						addToMessage(invoice.getDocumentNo());
						invoicesToPrint.add(invoice);
					}
				});
			} catch (Exception e) {
				addLog(e.getLocalizedMessage());
				withError.addAndGet(entry.getValue().size());
				log.warning(e.getLocalizedMessage());
			}
		});

	}

	private BigDecimal getSalesOrderQtyToInvoice(MWMInOutBoundLine outboundLine) {
		return Optional.ofNullable(getSelectionAsBigDecimal(outboundLine.getWM_InOutBoundLine_ID(), "QtyToInvoice")).orElse(outboundLine.getQtyToInvoice());
	}

	private void groupOutBoundLine (MWMInOutBoundLine line) {
		if (line.getC_OrderLine_ID() <= 0) {
			return;
		}
		MOrderLine orderLine = line.getOrderLine();
		MOrder order = orderLine.getParent();
		String keyString = grouping.getKey(order, getDocTypeTargetId(), isConsolidateDocument());
		List<MWMInOutBoundLine> lines = groupedOutBoundLines.getOrDefault(keyString, new ArrayList<>());
		lines.add(line);
		groupedOutBoundLines.put(keyString, lines);
	}

	/**
	 * Add Document Info for message to return
	 * @param documentInfo
	 */
	private void addToMessage(String documentInfo) {
		if(generatedDocuments.length() > 0) {
			generatedDocuments.append(", ");
		}
		//
		generatedDocuments.append(documentInfo);
	}
}