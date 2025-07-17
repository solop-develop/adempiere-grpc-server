/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.adempiere.process;

import io.vavr.Tuple2;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.*;
import org.compiere.process.DocAction;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Trx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 *	Generate Invoices
 *	
 *  @author Susanne Calderon
 *  @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/adempiere/issues/1070">
 * 		@see FR [ 1070 ] Class Not Found on SB for Generate Invoice from Order Line</a>
 */
public class SB_InvoiceGenerateFromOrderLine extends SB_InvoiceGenerateFromOrderLineAbstract {
	/**	The current Shipment	*/
	private MInOut	 	m_ship = null;
	/** Number of Invoices		*/
	private int			m_created = 0;
	/**	Line Number				*/
	private int			m_line = 0;
	/**	Business Partner		*/
	private MBPartner	m_bp = null;
	StringBuilder resultMsg;

	private HashMap<String, List<Tuple2<Integer,Integer>>> groupedOrderLinesAfterDelivery;
	private HashMap<String, List<MOrderLine>> groupedOrderLinesImmediate;
	private HashSet<Integer> ordersToIgnore;
	private HashSet<Integer> ordersCanInvoice;
	private InvoiceGrouping grouping;
	private int withError = 0;

	private int maxLines = 0;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare() {
		super.prepare();
		grouping = InvoiceGrouping.newInstance();
		//	Login Date
		if(getDateInvoiced() == null) {
			setDateInvoiced(Env.getContextAsDate(getCtx(), "#Date"));
			if (getDateInvoiced() == null) {
				setDateInvoiced(new Timestamp(System.currentTimeMillis()));
			}
		}
		//	DocAction check
		if (getDocAction() == null) { 
			setDocAction(DocAction.ACTION_Complete);
		} else if(!DocAction.ACTION_Complete.equals(getDocAction())) {
			setDocAction(DocAction.ACTION_Prepare);
		}
	}	//	prepare

	/**
	 * 	Generate Invoices
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		resultMsg = new StringBuilder();
		groupedOrderLinesAfterDelivery = new HashMap<>();
		groupedOrderLinesImmediate = new HashMap<>();
		ordersToIgnore = new HashSet<>();
		ordersCanInvoice = new HashSet<>();
		getProcessInfo().setTableSelectionId(MOrderLine.Table_ID);
		List<MOrderLine> orderLines = (List<MOrderLine>) getInstancesForSelection(get_TrxName());
		if(orderLines != null) {
			if (getDocTypeId() > 0) {
				MDocType docType = MDocType.get(getCtx(), getDocTypeId());
				maxLines = docType.get_ValueAsInt("MaxLinesPerDocument");
			}
			orderLines.forEach(this::groupLines);
			createAndProcessInvoicesAfterDelivery();
			createAndProcessInvoicesImmediate();

		}
		//	
		return "@Invoice@ @Created@ " + m_created + " [" + resultMsg.toString() + "]" + (withError > 0 ? " | Err: " + withError : "");
	}	//	doIt


	private void groupLines(MOrderLine line) {
		if (ordersToIgnore.contains(line.getC_Order_ID())) {
			return;
		}
		MOrder order = line.getParent();
		boolean afterDelivery = false;
		if (!ordersCanInvoice.contains(order.get_ID())) {
			boolean completeOrder = MOrder.INVOICERULE_AfterOrderDelivered.equals(order.getInvoiceRule());
			if (completeOrder) {
				String whereClause = "QtyOrder > QtyDelivered AND C_Order_ID = ?";
				boolean notFullyDelivered = new Query(getCtx(), MOrderLine.Table_Name, whereClause, get_TrxName())
						.setParameters(order.get_ID())
						.match();

				if (notFullyDelivered){
					ordersToIgnore.add(order.get_ID());
					return;
				}
				BigDecimal toInvoice = Optional.ofNullable(getSelectionAsBigDecimal(line.getC_OrderLine_ID(), "OLINE_QtyEntered")).orElse(line.getQtyOrdered().subtract(line.getQtyInvoiced()));
				if (toInvoice.compareTo(Env.ZERO) == 0 && line.getM_Product_ID() != 0) {
					return;
				}
				ordersCanInvoice.add(order.get_ID());
			}
			afterDelivery = completeOrder || MOrder.INVOICERULE_AfterDelivery.equals(order.getInvoiceRule()) || ordersCanInvoice.contains(order.get_ID());
			if (!afterDelivery) {
				if (MOrder.INVOICERULE_CustomerScheduleAfterDelivery.equals(order.getInvoiceRule()))
				{
					m_bp = new MBPartner (getCtx(), order.getBill_BPartner_ID(), null);
					if (m_bp.getC_InvoiceSchedule_ID() == 0)
					{
						ordersCanInvoice.add(order.get_ID());
						afterDelivery = true;
					}
					else
					{
						MInvoiceSchedule is = MInvoiceSchedule.get(getCtx(), m_bp.getC_InvoiceSchedule_ID(), get_TrxName());
						if (is.canInvoice(order.getDateOrdered(), order.getGrandTotal())) {
							ordersCanInvoice.add(order.get_ID());
							afterDelivery = true;
						} else {
							ordersToIgnore.add(order.get_ID());
							return;
						}
					}
				}	//	Schedule
			}
		}
		//For After Delivery
		if (afterDelivery) {
			String whereClause = MInOutLine.COLUMNNAME_C_OrderLine_ID + " = ? " +
					" AND IsInvoiced = 'N'" +
					" AND EXISTS(SELECT 1 FROM M_InOut io where io.DocStatus IN ('CO', 'CL'))";
			List<Integer> shipLineIds = new Query(getCtx(), MInOutLine.Table_Name, whereClause, get_TrxName())
				.setParameters(line.get_ID())
				.getIDsAsList();

			shipLineIds.forEach(shipLineId -> {
				String keyString = grouping.getKey(order, getDocTypeId(), isConsolidateDocument());
				List<Tuple2<Integer, Integer>> lines = groupedOrderLinesAfterDelivery.getOrDefault(keyString, new ArrayList<>());
				lines.add(new Tuple2<>(order.get_ID(), shipLineId));
				groupedOrderLinesAfterDelivery.put(keyString, lines);
			});
			return;
		}//END For After Delivery

		BigDecimal toInvoice = Optional.ofNullable(getSelectionAsBigDecimal(line.getC_OrderLine_ID(), "OLINE_QtyEntered")).orElse(line.getQtyOrdered().subtract(line.getQtyInvoiced()));
		if (toInvoice.compareTo(Env.ZERO) == 0 && line.getM_Product_ID() != 0) {
			return;
		}
		String keyString = grouping.getKey(order, getDocTypeId(), isConsolidateDocument());
		List<MOrderLine> lines = groupedOrderLinesImmediate.getOrDefault(keyString, new ArrayList<>());
		lines.add(line);
		groupedOrderLinesImmediate.put(keyString, lines);
	}

	private void createAndProcessInvoicesAfterDelivery() {
		groupedOrderLinesAfterDelivery.entrySet().stream().filter(Objects::nonNull).forEach(entry -> {
			try {
				Trx.run(transactionName -> {

					List<Tuple2<Integer, Integer>> lines = entry.getValue();
					AtomicReference<MInvoice> maybeInvoice = new AtomicReference<>();
					AtomicReference<MOrder> maybeOrder = new AtomicReference<>();
					lines.forEach(orderShipLineTuple -> {

						MOrder order = maybeOrder.get();
						if (order == null || order.get_ID() != orderShipLineTuple._1()) {
							order = new MOrder(getCtx(), orderShipLineTuple._1(), transactionName);
							maybeOrder.set(order);
						}
						MInvoice invoice = maybeInvoice.get();
						if (invoice == null) {
							invoice = new MInvoice(order, 0, getDateInvoiced());
							if(getOrgTrxId() != 0) {
								invoice.setAD_Org_ID(getOrgTrxId());
							}
							if (getDocTypeId()  != 0) {
								invoice.setC_DocType_ID(getDocTypeId());
							}
							invoice.saveEx(transactionName);
							maybeInvoice.set(invoice);
						}
						//Create invoice line with transactionName
						MInOutLine shipLine = MInOutLine.get(getCtx(), orderShipLineTuple._2());
						shipLine.set_TrxName(transactionName);
						MInOut ship = shipLine.getParent();
						ship.set_TrxName(transactionName);
						createLine (invoice, order, ship, shipLine, transactionName);
						m_line += 1000;

					});
					MInvoice invoice = maybeInvoice.get();
					invoice.setDocAction(getDocAction());
					if (!invoice.processIt(getDocAction())) {
						addLog("@ProcessFailed@ : " + invoice.getDocumentInfo());
						throw new AdempiereException("@ProcessFailed@ :" + invoice.getDocumentInfo());
					}
					invoice.saveEx();

					m_ship = null;
					m_line = 0;
					addLog(invoice.getC_Invoice_ID(), invoice.getDateInvoiced(), null, invoice.getDocumentNo());
					if (resultMsg.length() > 0) {
						resultMsg.append(", ");
					}
					resultMsg.append(invoice.getDocumentNo());
					m_created++;
				});
			} catch (Exception e) {
				addLog("@Error@ " + e.getLocalizedMessage());
				withError += entry.getValue().size();
				m_ship = null;
				m_line = 0;
				m_bp = null;
			}
		});
	}


	private void createAndProcessInvoicesImmediate() {
		groupedOrderLinesImmediate.entrySet().stream().filter(Objects::nonNull).forEach(entry -> {
			try {
				Trx.run(transactionName -> {

					List<MOrderLine> lines = entry.getValue();
					AtomicReference<MInvoice> maybeInvoice = new AtomicReference<>();
					AtomicReference<MOrder> maybeOrder = new AtomicReference<>();
					lines.forEach(orderLine -> {

						MOrder order = maybeOrder.get();
						if (order == null || order.get_ID() != orderLine.getC_Order_ID()) {
							order = orderLine.getParent();
							order.set_TrxName(transactionName);
							maybeOrder.set(order);
						}
						MInvoice invoice = maybeInvoice.get();
						if (invoice == null) {
							invoice = new MInvoice(order, 0, getDateInvoiced());
							if(getOrgTrxId() != 0) {
								invoice.setAD_Org_ID(getOrgTrxId());
							}
							if (getDocTypeId()  != 0) {
								invoice.setC_DocType_ID(getDocTypeId());
							}
							invoice.saveEx(transactionName);
							maybeInvoice.set(invoice);
						}

						BigDecimal toInvoice = Optional.ofNullable(getSelectionAsBigDecimal(orderLine.getC_OrderLine_ID(), "OLINE_QtyEntered")).orElse(orderLine.getQtyOrdered().subtract(orderLine.getQtyInvoiced()));

						BigDecimal qtyEntered = toInvoice;
						//	Correct UOM for QtyEntered
						if (orderLine.getQtyEntered().compareTo(orderLine.getQtyOrdered()) != 0)
							qtyEntered = toInvoice
									.multiply(orderLine.getQtyEntered())
									.divide(orderLine.getQtyOrdered(), 12, RoundingMode.HALF_UP);
						createLine (invoice, orderLine, toInvoice, qtyEntered, transactionName);
						m_line += 1000;

					});
					MInvoice invoice = maybeInvoice.get();
					invoice.setDocAction(getDocAction());
					if (!invoice.processIt(getDocAction())) {
						addLog("@ProcessFailed@ : " + invoice.getDocumentInfo());
						throw new AdempiereException("@ProcessFailed@ :" + invoice.getDocumentInfo());
					}
					invoice.saveEx();
					m_ship = null;
					m_line = 0;
					addLog(invoice.getC_Invoice_ID(), invoice.getDateInvoiced(), null, invoice.getDocumentNo());
					if (resultMsg.length() > 0) {
						resultMsg.append(", ");
					}
					resultMsg.append(invoice.getDocumentNo());
					m_created++;

				});
			} catch (Exception e) {
				withError += entry.getValue().size();
				addLog("@Error@ " + e.getLocalizedMessage());
				m_ship = null;
				m_line = 0;
				m_bp = null;
			}
		});
	}
	
	
	/**************************************************************************
	 * 	Create Invoice Line from Order Line
	 * @param invoice invoice
	 *	@param orderLine line
	 *	@param qtyInvoiced qty
	 *	@param qtyEntered qty
	 *  @param transactionName transactionName
	 */
	private void createLine (MInvoice invoice, MOrderLine orderLine,
		BigDecimal qtyInvoiced, BigDecimal qtyEntered, String transactionName) {

		//	
		MInvoiceLine line = new MInvoiceLine (invoice);
		line.setOrderLine(orderLine);
		line.setQtyInvoiced(qtyInvoiced);
		line.setQtyEntered(qtyEntered);
		line.setLine(m_line + orderLine.getLine());
		line.saveEx(transactionName);
	}	//	createLine

	/**
	 * 	Create Invoice Line from Shipment
	 *  @param invoice invoice
	 *	@param order order
	 *	@param ship shipment header
	 *	@param sLine shipment line
	 *  @param transactionName trx Name
	 */
	private void createLine (MInvoice invoice, MOrder order, MInOut ship, MInOutLine sLine, String transactionName)
	{
		//	Create Shipment Comment Line
		if (isAddInvoiceReferenceLine()
				&& (m_ship == null 
					|| m_ship.getM_InOut_ID() != ship.getM_InOut_ID()))
		{
			MDocType dt = MDocType.get(getCtx(), ship.getC_DocType_ID());
			if (m_bp == null || m_bp.getC_BPartner_ID() != ship.getC_BPartner_ID())
				m_bp = new MBPartner (getCtx(), ship.getC_BPartner_ID(), transactionName);
			
			//	Reference: Delivery: 12345 - 12.12.12
			MClient client = MClient.get(getCtx(), order.getAD_Client_ID ());
			String AD_Language = client.getAD_Language();
			if (client.isMultiLingualDocument() && m_bp.getAD_Language() != null)
				AD_Language = m_bp.getAD_Language();
			if (AD_Language == null)
				AD_Language = Language.getBaseAD_Language();
			SimpleDateFormat format = DisplayType.getDateFormat 
				(DisplayType.Date, Language.getLanguage(AD_Language));
			String reference = dt.getPrintName(m_bp.getAD_Language())
				+ ": " + ship.getDocumentNo() 
				+ " - " + format.format(ship.getMovementDate());
			m_ship = ship;
			//
			MInvoiceLine line = new MInvoiceLine (invoice);
			line.setTax();
			line.setIsDescription(true);
			line.setDescription(reference);
			line.setLine(m_line + sLine.getLine() - 2);
			line.saveEx(transactionName);
			//	Optional Ship Address if not Bill Address
			if (order.getBill_Location_ID() != ship.getC_BPartner_Location_ID())
			{
				MLocation addr = MLocation.getBPLocation(getCtx(), ship.getC_BPartner_Location_ID(), null);
				line = new MInvoiceLine (invoice);
				line.setTax();
				line.setIsDescription(true);
				line.setDescription(addr.toString());
				line.setLine(m_line + sLine.getLine() - 1);
				line.saveEx(transactionName);
			}
		}
		//	
		MInvoiceLine line = new MInvoiceLine (invoice);
		line.setShipLine(sLine);
		if (sLine.sameOrderLineUOM())
			line.setQtyEntered(sLine.getQtyEntered());
		else
			line.setQtyEntered(sLine.getMovementQty());
		line.setQtyInvoiced(sLine.getMovementQty());
		line.setLine(m_line + sLine.getLine());
		//@Trifon - special handling when ShipLine.ToBeInvoiced='N'
		if (!sLine.isToBeInvoiced()) {
			line.setPriceEntered( Env.ZERO );
			line.setPriceActual( Env.ZERO );
			line.setPriceLimit( Env.ZERO );
			line.setPriceList( Env.ZERO);
			line.setLineNetAmt( Env.ZERO );
			line.setIsDescription( true );
		}
		line.saveEx(transactionName);
		//	Link
		sLine.setIsInvoiced(true);
		sLine.saveEx();
		
		log.fine(line.toString());
	}	//	createLine
	
	
}	//	InvoiceGenerate
