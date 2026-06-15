package com.solop.sp034.queue;

import com.solop.sp034.process.PublishingProcessingAbstract;
import com.solop.sp034.util.Changes;
import com.solop.sp034.util.PublishingUpdater;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_ProjectIssue;
import org.adempiere.core.domains.models.I_DD_Order;
import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.core.domains.models.I_M_Inventory;
import org.adempiere.core.domains.models.I_M_Movement;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_Production;
import org.adempiere.core.domains.models.I_M_ProductionBatch;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Trx;
import org.eevolution.services.dsl.ProcessBuilder;
import org.spin.queue.model.MADQueue;
import org.spin.queue.util.QueueManager;

import java.util.ArrayList;
import java.util.List;


/**
 * Store Publication Updater (SPU)
 * <p>
 * Mirrors {@code org.solop.queue.storage.StorageUpdate}: derives the affected products from the
 * queued document, but instead of recalculating M_Storage it refreshes every SP034_Publishing of
 * those products (price + inventory, via {@link PublishingUpdater}) and, when the publication is
 * active, re-publishes it through the Publishing Processing process (which triggers the MercadoLibre
 * webhook).
 * @author Gabriel Escalona
 */
public class StorePublicationUpdater extends QueueManager {
    public static String QueueType_PublicationUpdater = "SPU";
    /**	Logger						*/
    protected CLogger log = CLogger.getCLogger(getClass());
    @Override
    public void add(int queueId) {
        log.fine("Publication Updater Queue Added: " + queueId);
    }

    @Override
    public void process(int queueId) {
        MADQueue queue = new MADQueue(getContext(), queueId, getTransactionName());
        int tableId = queue.getAD_Table_ID();
        int recordId = queue.getRecord_ID();
        //  Ignore documents not handled here
        if(tableId != I_C_Order.Table_ID
                && tableId != I_DD_Order.Table_ID
                && tableId != I_M_ProductionBatch.Table_ID
                && tableId != I_M_InOut.Table_ID
                && tableId != I_M_Movement.Table_ID
                && tableId != I_M_Production.Table_ID
                && tableId != I_C_ProjectIssue.Table_ID
                && tableId != I_M_Inventory.Table_ID) {
            return;
        }
        if(recordId <= 0) {
            return;
        }
        List<Integer> productsIds = getProductsIds(tableId, recordId);
        if(productsIds == null || productsIds.isEmpty()) {
            return;
        }
        String inClause = productsIds.toString().replace("[", "(").replace("]", ")");
        MTable publishingTable = MTable.get(getContext(), Changes.Table_SP034_Publishing);
        int publishingTableId = publishingTable.getAD_Table_ID();
        //  Each publishing is refreshed in its own committed transaction so the row lock is
        //  released before the (parallel) re-publish runs. This avoids the self-deadlock that
        //  happened when the queue transaction held the row while the Publishing Processing
        //  process opened a separate transaction to update the very same row.
        List<Integer> toPublish = new ArrayList<>();
        new Query(
                getContext(),
                Changes.Table_SP034_Publishing,
                "M_Product_ID IN " + inClause + " AND SP034_PublishStatus <> 'C'",
                getTransactionName())
                .setClient_ID()
                .setOnlyActiveRecords(true)
                .getIDsAsList()
                .forEach(publishingId -> {
                    try {
                        Trx.run(transactionName -> {
                            PO publishing = publishingTable.getPO(publishingId, transactionName);
                            if(Changes.SP034_PublishStatus_Active.equals(publishing.get_ValueAsString(Changes.SP034_PublishStatus))) {
                                toPublish.add(publishingId);
                                return;
                            }
                            if(!PublishingUpdater.updateData(publishing)) {
                                log.warning("No price list version, skipping publishing " + publishingId);
                                return;
                            }
                            publishing.saveEx();

                        });
                    } catch (Exception e) {
                        log.warning("Error updating publishing " + publishingId + ": " + e.getLocalizedMessage());
                    }
                });
        //  All updates are committed (and unlocked) by now: re-publish the active ones in one
        //  process call so it can publish them in parallel on its own transactions.
        if(!toPublish.isEmpty()) {
            republish(toPublish, publishingTableId);
        }
    }

    /**
     * Derive the affected products from the queued document. Duplicated from
     * {@code StorageUpdate.process()} on purpose so both queues stay independent.
     */
    private List<Integer> getProductsIds(int tableId, int recordId) {
        String existsClause = null;
        if(tableId == I_C_Order.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM C_OrderLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.C_Order_ID = ?)";
        } else if(tableId == I_DD_Order.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM DD_OrderLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.DD_Order_ID = ?)";
        } else if(tableId == I_M_ProductionBatch.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM M_ProductionBatchLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_ProductionBatch_ID = ?)";
        } else if(tableId == I_M_InOut.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM M_InOutLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_InOut_ID = ?)";
        } else if(tableId == I_M_Movement.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM M_MovementLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_Movement_ID = ?)";
        } else if(tableId == I_M_Production.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM M_ProductionLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_Production_ID = ?)";
        } else if(tableId == I_C_ProjectIssue.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM C_ProjectIssue ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.C_ProjectIssue_ID = ?)";
        } else if(tableId == I_M_Inventory.Table_ID) {
            existsClause = "EXISTS(SELECT 1 FROM M_InventoryLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_Inventory_ID = ?)";
        }
        if(existsClause == null) {
            return null;
        }
        return new Query(getContext(), I_M_Product.Table_Name, existsClause, getTransactionName())
                .setParameters(recordId)
                .getIDsAsList();
    }

    /**
     * Re-publish the active records by running the Publishing Processing process with
     * SP034_PublishStatus = Publishing ("P"), which sets the status and triggers the webhook.
     * The ids are passed as a selection so the process handles them on its own transactions
     * (in parallel); no external transaction is shared, which is what previously deadlocked.
     */
    private void republish(List<Integer> publishingIds, int publishingTableId) {
        ProcessInfo info = ProcessBuilder.create(getContext())
                .process(PublishingProcessingAbstract.getProcessId())
                .withSelectedRecordsIds(publishingTableId, publishingIds)
                .withParameter(PublishingProcessingAbstract.SP034_PUBLISHSTATUS, Changes.SP034_PublishStatus_Publishing)
                .execute();
        if(info != null && info.isError()) {
            log.warning("Re-publish failed: " + info.getSummary());
        }
    }
}
