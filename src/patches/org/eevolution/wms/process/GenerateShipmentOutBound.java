/**********************************************************************
 * This file is part of Adempiere ERP Bazaar                          * 
 * http://www.adempiere.org                                           * 
 *                                                                    * 
 * Copyright (C) Victor Perez	                                      * 
 * Copyright (C) Contributors                                         * 
 *                                                                    * 
 * This program is free software; you can redistribute it and/or      * 
 * modify it under the terms of the GNU General Public License        * 
 * as published by the Free Software Foundation; either version 2     * 
 * of the License, or (at your option) any later version.             * 
 *                                                                    * 
 * This program is distributed in the hope that it will be useful,    * 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of     * 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the       * 
 * GNU General Public License for more details.                       * 
 *                                                                    * 
 * You should have received a copy of the GNU General Public License  * 
 * along with this program; if not, write to the Free Software        * 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,         * 
 * MA 02110-1301, USA.                                                * 
 *                                                                    * 
 * Contributors:                                                      * 
 *  - Victor Perez (victor.perez@e-evolution.com	 )                *
 *                                                                    *
 * Sponsors:                                                          *
 *  - e-Evolution (http://www.e-evolution.com/)                       *
 **********************************************************************/

package org.eevolution.wms.process;

import org.adempiere.core.domains.models.X_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.*;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.eevolution.distribution.model.MDDOrder;
import org.eevolution.distribution.model.MDDOrderLine;
import org.eevolution.distribution.process.MovementGenerate;
import org.eevolution.manufacturing.model.MPPCostCollector;
import org.eevolution.manufacturing.model.MPPOrder;
import org.eevolution.manufacturing.model.MPPOrderBOMLine;
import org.eevolution.services.dsl.ProcessBuilder;
import org.eevolution.wms.model.MWMInOutBound;
import org.eevolution.wms.model.MWMInOutBoundLine;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author victor.perez@e-evolution.com, www.e-evolution.com
 * @version $Id: $
 */
public class GenerateShipmentOutBound extends GenerateShipmentOutBoundAbstract {
    private HashSet<String> shipmentsData;

    private HashMap<Integer, Integer> numberOfDocuments;
    private HashMap<String, List<MWMInOutBoundLine>> groupedOutBoundLinesForShipments;
    private HashMap<Integer, List<MWMInOutBoundLine>> groupedOutBoundLinesForMovements;
    private HashMap<Integer, List<MWMInOutBoundLine>> groupedOutBoundLinesForIssues;
    private int documentCreated = 0;
    private final AtomicInteger withError = new AtomicInteger(0);

    /**
     * Get Parameters
     */
    protected void prepare() {
        super.prepare();
    }

    /**
     * Process - Generate Shipment from OutBound
     *
     * @return info
     */
    protected String doIt() throws Exception {
        numberOfDocuments = new HashMap<>();
        groupedOutBoundLinesForShipments = new HashMap<>();
        groupedOutBoundLinesForMovements = new HashMap<>();
        groupedOutBoundLinesForIssues = new HashMap<>();
        shipmentsData = new HashSet<>();
        // Overwrite table RV_WM_InOutBoundLine by WM_InOutBoundLine domain model
        getProcessInfo().setTableSelectionId(MWMInOutBoundLine.Table_ID);
        List<MWMInOutBoundLine> outBoundLines = null;
        if(getRecord_ID() > 0) {
            outBoundLines = new Query(getCtx(), MWMInOutBoundLine.Table_Name, MWMInOutBound.COLUMNNAME_WM_InOutBound_ID + "=?", get_TrxName())
                    .setParameters(getRecord_ID())
                    .setOrderBy(MWMInOutBoundLine.COLUMNNAME_C_Order_ID + ", " + MWMInOutBoundLine.COLUMNNAME_DD_Order_ID)
                    .list();
        } else if(isSelection()) {
            // Overwrite table RV_WM_InOutBoundLine by WM_InOutBoundLine
            getProcessInfo().setTableSelectionId(MWMInOutBoundLine.Table_ID);
            outBoundLines = (List<MWMInOutBoundLine>) getInstancesForSelection(get_TrxName());
        }
        outBoundLines.stream()
                .filter(outBoundLine -> outBoundLine.getQtyToDeliver().signum() > 0 || isIncludeNotAvailable())
                .forEach(this::groupOutBoundLines);
        createAndProcessShipments();
        createAndProcessMovements();
        createAndProcessIssues();

        StringBuilder documentGenerated = new StringBuilder();
        shipmentsData.forEach(value -> documentGenerated.append(" , ").append(value));
        return "@Created@ " + documentCreated + documentGenerated.toString() + (withError.get() > 0 ? " | @Error@ " + withError.get() : "");
    }

    private void createAndProcessShipments() {
        List<PO> documentsToPrint = new ArrayList<PO>();
        groupedOutBoundLinesForShipments.entrySet().stream().filter(Objects::nonNull).forEach(entry -> {
            try {
                Trx.run(transactionName -> {
                    List<MWMInOutBoundLine> lines = entry.getValue();
                    AtomicReference<MInOut> maybeShipment = new AtomicReference<>();
                    lines.forEach(outboundLine -> {
                        MOrderLine orderLine = outboundLine.getOrderLine();
                        MInOut shipment = maybeShipment.get();
                        if (shipment == null) {
                            MOrder order = orderLine.getParent();
                            MDocType orderDocumentType = (MDocType) order.getC_DocType();
                            int docTypeId = orderDocumentType.getC_DocTypeShipment_ID();
                            if (docTypeId == 0) {
                                docTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_MaterialDelivery, orderLine.getAD_Org_ID());
                            }
                            MWMInOutBound outbound = outboundLine.getParent();
                            shipment = new MInOut(order, docTypeId, getMovementDate());
                            shipment.setIsSOTrx(true);
                            shipment.setM_Shipper_ID(outbound.getM_Shipper_ID());
                            shipment.setDescription(outbound.getDescription());
                            shipment.setM_FreightCategory_ID(outbound.getM_FreightCategory_ID());
                            shipment.setFreightCostRule(outbound.getFreightCostRule());
                            shipment.setFreightAmt(outbound.getFreightAmt());
                            shipment.setDocAction(MInOut.DOCACTION_Complete);
                            shipment.setDocStatus(MInOut.DOCSTATUS_Drafted);
                            shipment.saveEx(transactionName);
                            maybeShipment.set(shipment);
                        }
                        BigDecimal qtyToDelivery = getSalesOrderQtyToDelivery(outboundLine);
                        MInOutLine shipmentLine = new MInOutLine(outboundLine.getCtx(), 0, transactionName);
                        shipmentLine.setM_InOut_ID(shipment.getM_InOut_ID());
                        shipmentLine.setM_Locator_ID(outboundLine.getM_LocatorTo_ID());
                        shipmentLine.setM_Product_ID(outboundLine.getM_Product_ID());
                        shipmentLine.setDescription(outboundLine.getDescription());
                        shipmentLine.setC_UOM_ID(outboundLine.getC_UOM_ID());
                        shipmentLine.setQtyEntered(qtyToDelivery);
                        shipmentLine.setMovementQty(qtyToDelivery);
                        shipmentLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
                        shipmentLine.setM_Shipper_ID(outboundLine.getM_Shipper_ID());
                        shipmentLine.setM_FreightCategory_ID(outboundLine.getM_FreightCategory_ID());
                        shipmentLine.setFreightAmt(outboundLine.getFreightAmt());
                        shipmentLine.setM_AttributeSetInstance_ID(outboundLine.getM_AttributeSetInstance_ID());
                        shipmentLine.setWM_InOutBoundLine_ID(outboundLine.getWM_InOutBoundLine_ID());
                        shipmentLine.saveEx();
                        outboundLine.setPickedQty(Optional.ofNullable(outboundLine.getShipmentQtyDelivered()).orElse(Env.ZERO).add(qtyToDelivery));
                        outboundLine.setM_InOutLine_ID(shipmentLine.getM_InOutLine_ID());
                        outboundLine.setM_InOut_ID(shipmentLine.getM_InOut_ID());
                        outboundLine.saveEx();
                    });
                    MInOut shipment = maybeShipment.get();
                    if (!shipment.processIt(getDocAction())) {
                        addLog("@ProcessFailed@ : " + shipment.getProcessMsg());
                        throw new AdempiereException("@ProcessFailed@ :" + shipment.getProcessMsg());
                    }
                    shipment.saveEx();
                    shipmentsData.add(shipment.getDocumentInfo());
                    documentCreated++;
                    addLog(shipment.getDocumentInfo());
                    documentsToPrint.add(shipment);
                });
            } catch (Exception e) {
                addLog(e.getLocalizedMessage());
                withError.addAndGet(entry.getValue().size());
                log.warning(e.getLocalizedMessage());
            }
        });
        printDocument(documentsToPrint, true);
    }
    private void createAndProcessMovements(){
        List<PO> documentsToPrint = new ArrayList<PO>();
        groupedOutBoundLinesForMovements.entrySet().stream().filter(Objects::nonNull).forEach(entry -> {
            try {
                Trx.run(transactionName -> {
                    List<MWMInOutBoundLine> lines = entry.getValue();
                    MDDOrder distributionOrder = new MDDOrder(getCtx(), entry.getKey(), transactionName);
                    lines.forEach(outboundLine -> {
                        MDDOrderLine distributionOrderLine = new MDDOrderLine(outboundLine.getCtx(), outboundLine.getDD_OrderLine_ID(), transactionName);
                        distributionOrderLine.setDescription(outboundLine.getDescription());
                        distributionOrderLine.setConfirmedQty(getDistributionOrderQtyToDelivery(outboundLine, distributionOrderLine));
                        distributionOrderLine.saveEx();
                    });
                    List<Integer> orderIds = new ArrayList<Integer>();
                    orderIds.add(distributionOrder.getDD_Order_ID());

                    ProcessInfo processInfo = ProcessBuilder.create(getCtx())
                            .process(MovementGenerate.getProcessId())
                            .withSelectedRecordsIds(MDDOrder.Table_ID, orderIds)
                            .withParameter(MWMInOutBound.COLUMNNAME_M_Warehouse_ID, distributionOrder.getM_Warehouse_ID())
                            .withParameter(MMovement.COLUMNNAME_MovementDate, getMovementDate())
                            .withoutTransactionClose()
                            .execute(transactionName);
                    if (processInfo.isError()) {
                        throw new AdempiereException(processInfo.getSummary());
                    }
                    addLog(processInfo.getSummary());
                    Arrays.stream(processInfo.getIDs()).forEach(recordId -> {
                        if (recordId <= 0) {
                            return;
                        }
                        MMovement movement = new MMovement(getCtx(), recordId, transactionName);
                        documentCreated++;
                        documentsToPrint.add(movement);
                    });
                });
            } catch (Exception e) {
                addLog(e.getLocalizedMessage());
                withError.addAndGet(entry.getValue().size());
                log.warning(e.getLocalizedMessage());
            }
        });
        printDocument(documentsToPrint, true);
    }
    private void createAndProcessIssues() {
        groupedOutBoundLinesForIssues.entrySet().stream().filter(Objects::nonNull).forEach(entry -> {
            try {
                Trx.run(transactionName -> {
                    List<MWMInOutBoundLine> lines = entry.getValue();

                    lines.forEach(outboundLine -> {
                        MPPOrderBOMLine orderBOMLine = (MPPOrderBOMLine) outboundLine.getPP_Order_BOMLine();
                        MStorage[] storage = MStorage.getAll(getCtx(), orderBOMLine.getM_Product_ID(), outboundLine.getM_LocatorTo_ID(), transactionName);
                        BigDecimal qtyDelivered = getManufacturingOrderQtyToDelivery(outboundLine , orderBOMLine);
                        List<MPPCostCollector> issues = MPPOrder.createIssue(
                                orderBOMLine.getParent(),
                                orderBOMLine,
                                getMovementDate(),
                                qtyDelivered,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                storage,
                                true);
                        if (issues != null) {
                            issues.forEach(costCollector -> {
                                costCollector.setDescription(outboundLine.getDescription());
                                costCollector.saveEx();
                                if (MPPCostCollector.DOCSTATUS_Drafted.equals(costCollector.getDocStatus())
                                        || MPPCostCollector.DOCSTATUS_InProgress.equals(costCollector.getDocStatus())) {
                                    if (!costCollector.processIt(MPPCostCollector.DOCACTION_Complete)) {
                                        addLog("@ProcessFailed@ : " + costCollector.getDocumentInfo());
                                        throw new AdempiereException("@ProcessFailed@ :" + costCollector.getDocumentInfo());
                                    }
                                    costCollector.saveEx();
                                }
                            });
                        }
                    });

                });
            } catch (Exception e) {
                addLog(e.getLocalizedMessage());
                withError.addAndGet(entry.getValue().size());
                log.warning(e.getLocalizedMessage());
            }
        });
    }

    private BigDecimal getSalesOrderQtyToDelivery(MWMInOutBoundLine outboundLine) {
        return Optional.ofNullable(getSelectionAsBigDecimal(outboundLine.getWM_InOutBoundLine_ID(), "QtyToDeliver")).orElse(outboundLine.getQtyToDeliver());
    }

    private BigDecimal getManufacturingOrderQtyToDelivery(MWMInOutBoundLine outboundLine, MPPOrderBOMLine orderBOMLine) {
        BigDecimal qtyToDelivery;
        if (isIncludeNotAvailable())
            qtyToDelivery = outboundLine.getQtyToPick();
        else {
            //Sales Order Qty To Delivery
            BigDecimal manufacturingOrderQtyToDelivery = orderBOMLine.getQtyRequired().subtract(orderBOMLine.getQtyRequired());
            //Outbound Order Qty To Delivery
            BigDecimal outboundOrderQtyToDelivery = outboundLine.getPickedQty().subtract(outboundLine.getManufacturingOrderQtyDelivered());
            //The quantity to delivery of the Outbound order cannot be greater than the pending quantity to delivery  of the sales order.
            if (outboundOrderQtyToDelivery.compareTo(manufacturingOrderQtyToDelivery) > 0)
                qtyToDelivery = manufacturingOrderQtyToDelivery;
            else if (!X_C_Order.DELIVERYRULE_Force.equals(orderBOMLine.getParent().getDeliveryRule()) &&
                    !X_C_Order.DELIVERYRULE_Manual.equals(orderBOMLine.getParent().getDeliveryRule()))
                qtyToDelivery = outboundOrderQtyToDelivery;
            else
                qtyToDelivery = manufacturingOrderQtyToDelivery;
        }
        return qtyToDelivery;
    }

    private BigDecimal getDistributionOrderQtyToDelivery(MWMInOutBoundLine outboundLine, MDDOrderLine orderLine) {
        BigDecimal qtyToDelivery;
        if (isIncludeNotAvailable())
            qtyToDelivery = outboundLine.getQtyToPick();
        else {
            //Sales Order Qty To Delivery
            BigDecimal distributionOrderQtyToDelivery = orderLine.getQtyToDeliver();
            //Outbound Order Qty To Delivery
            BigDecimal outboundOrderQtyToDelivery = outboundLine.getPickedQty().subtract(outboundLine.getDistributionOrderQtyDelivered());
            //The quantity to delivery of the Outbound order cannot be greater than the pending quantity to delivery  of the sales order.
            if (outboundOrderQtyToDelivery.compareTo(distributionOrderQtyToDelivery) > 0)
                qtyToDelivery = distributionOrderQtyToDelivery;
            else if (!X_C_Order.DELIVERYRULE_Force.equals(orderLine.getParent().getDeliveryRule()) &&
                    !X_C_Order.DELIVERYRULE_Manual.equals(orderLine.getParent().getDeliveryRule()))
                qtyToDelivery = outboundOrderQtyToDelivery;
            else
                qtyToDelivery = distributionOrderQtyToDelivery;
        }
        return qtyToDelivery;
    }

    private void groupOutBoundLines(MWMInOutBoundLine line) {
        //For Shipments
        if (line.getC_OrderLine_ID() > 0) {
            MOrderLine orderLine = line.getOrderLine();
            int key = orderLine.getC_Order_ID();
            int keyNumber = numberOfDocuments.getOrDefault(key, 0);
            String keyString = String.valueOf(key) + keyNumber;
            List<MWMInOutBoundLine> lines = groupedOutBoundLinesForShipments.getOrDefault(keyString, new ArrayList<>());
            MOrder order = orderLine.getParent();
            MDocType orderDocumentType = (MDocType) order.getC_DocType();
            int docTypeId = orderDocumentType.getC_DocTypeShipment_ID();
            if (docTypeId == 0) {
                docTypeId = MDocType.getDocType(MDocType.DOCBASETYPE_MaterialDelivery, orderLine.getAD_Org_ID());
            }
            MDocType ShipmentDocType = MDocType.get(getCtx(), docTypeId);
            int maxLines = ShipmentDocType.get_ValueAsInt("MaxLinesPerDocument");

            if (lines.isEmpty()) {
                keyNumber++;
                keyString = String.valueOf(key) + keyNumber;
                groupedOutBoundLinesForShipments.put(keyString, lines);
                numberOfDocuments.put(key, keyNumber);
                lines.add(line);
                return;
            }
            if (maxLines > 0 && lines.size() >= maxLines) {
                keyNumber++;
                keyString = String.valueOf(key) + keyNumber;
                lines = new ArrayList<>();
                groupedOutBoundLinesForShipments.put(keyString, lines);
                numberOfDocuments.put(key, keyNumber);
            }
            lines.add(line);
        }
        //For Movements
        if (line.getDD_OrderLine_ID() > 0) {
            MDDOrderLine distributionOrderLine = new MDDOrderLine(line.getCtx(), line.getDD_OrderLine_ID(), line.get_TrxName());
            List<MWMInOutBoundLine> lines = groupedOutBoundLinesForMovements.getOrDefault(distributionOrderLine.getDD_Order_ID(), new ArrayList<>());
            if (lines.isEmpty()) {
                groupedOutBoundLinesForMovements.put(distributionOrderLine.getDD_Order_ID(), lines);
            }
            lines.add(line);
        }

        // For Manufacturing
        if (line.getPP_Order_BOMLine_ID() > 0) {
            MPPOrderBOMLine orderBOMLine = (MPPOrderBOMLine) line.getPP_Order_BOMLine();
            if (line.getPickedQty().subtract(orderBOMLine.getQtyDelivered()).signum() < 0 && !isIncludeNotAvailable())
                return;

            List<MWMInOutBoundLine> lines = groupedOutBoundLinesForIssues.getOrDefault(orderBOMLine.getParent().getPP_Order_ID(), new ArrayList<>());
            if (lines.isEmpty()) {
                groupedOutBoundLinesForIssues.put(orderBOMLine.getParent().getPP_Order_ID(), lines);
            }
            lines.add(line);
        }
    }
}
