package org.storage;

import org.adempiere.core.domains.models.I_AD_StorageUpdateQueue;
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
import org.compiere.util.CLogger;
import org.compiere.util.Msg;
import org.solop.util.StorageUpdaterBuilder;
import org.spin.queue.model.MADQueue;
import org.spin.queue.util.QueueLoader;
import org.spin.queue.util.QueueManager;

import java.util.List;

/**
 * Shopping Documents Export Processor
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class StorageUpdate extends QueueManager {
    public static String QueueType_Storage_Management = "SMM";
    /**	Logger						*/
    protected CLogger log = CLogger.getCLogger(getClass());
    @Override
    public void add(int queueId) {
        log.fine("Shopping Document Queue Added: " + queueId);
    }

    @Override
    public void process(int queueId) {
        MADQueue queue = new MADQueue(getContext(), queueId, getTransactionName());
        //  Ignore without record
        int tableId = queue.getAD_Table_ID();
        int recordId = queue.getRecord_ID();
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
        if(queue.getRecord_ID() <= 0) {
            return;
        }
        List<Integer> productsIds = null;
        if(tableId == I_C_Order.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM C_OrderLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.C_Order_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        } else if(tableId == I_DD_Order.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM DD_OrderLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.DD_Order_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        } else if(tableId == I_M_ProductionBatch.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM M_ProductionBatchLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_ProductionBatch_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        } else if(tableId == I_M_InOut.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM M_InOutLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_InOut_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        } else if(tableId == I_M_Movement.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM M_MovementLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_Movement_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        } else if(tableId == I_M_Production.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM M_ProductionLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_Production_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        } else if(tableId == I_C_ProjectIssue.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM C_ProjectIssue ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.C_ProjectIssue_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        } else if(tableId == I_M_Inventory.Table_ID) {
            productsIds = new Query(getContext(), I_M_Product.Table_Name, "EXISTS(SELECT 1 FROM M_InventoryLine ol WHERE ol.M_Product_ID = M_Product.M_Product_ID AND ol.M_Inventory_ID = ?)", getTransactionName())
                    .setParameters(recordId)
                    .getIDsAsList();
        }
        if(productsIds != null && !productsIds.isEmpty()) {
            String message = StorageUpdaterBuilder.newInstance(getContext(), getTransactionName())
                    .withClientId(queue.getAD_Client_ID())
                    .withProductsIds(productsIds)
                    .build();
            if (message != null) {
                queue.setDescription(Msg.parseTranslation(getContext(), message));
                queue.saveEx();
            }
        }
        //	Chain the same document into every queue configured in AD_StorageUpdateQueue,
        //	resolving the queue manager by the AD_QueueType_ID stored on each record.
        enqueueConfiguredQueues(tableId, recordId);
    }

    /**
     * Enqueue the current document on each queue type configured in AD_StorageUpdateQueue, so that
     * downstream queues (e.g. the publication updater) can react to the storage change.
     * Only configs whose organization is the original document's org (or org 0, meaning all orgs)
     * are enqueued.
     */
    private void enqueueConfiguredQueues(int tableId, int recordId) {
        PO document = MTable.get(getContext(), tableId).getPO(recordId, getTransactionName());
        int documentOrgId = document != null ? document.getAD_Org_ID() : 0;
        MTable storageUpdateQueueTable = MTable.get(getContext(), I_AD_StorageUpdateQueue.Table_Name);
        new Query(getContext(), I_AD_StorageUpdateQueue.Table_Name, null, getTransactionName())
                .setClient_ID()
                .setOnlyActiveRecords(true)
                .getIDsAsList()
                .forEach(id -> {
                    PO config = storageUpdateQueueTable.getPO(id, getTransactionName());
                    int queueTypeId = config.get_ValueAsInt(I_AD_StorageUpdateQueue.COLUMNNAME_AD_QueueType_ID);
                    if(queueTypeId <= 0) {
                        return;
                    }
                    //	Skip configs scoped to a different organization than the document
                    int configOrgId = config.getAD_Org_ID();
                    if(configOrgId != 0 && configOrgId != documentOrgId) {
                        return;
                    }
                    QueueLoader.getInstance().getQueueManager(queueTypeId)
                            .withContext(getContext())
                            .withTransactionName(getTransactionName())
                            .withEntity(tableId, recordId)
                            .addToQueue();
                });
    }

    public static void addDocumentToQueue(PO document) {
        QueueLoader.getInstance().getQueueManager(QueueType_Storage_Management)
                .withContext(document.getCtx())
                .withTransactionName(document.get_TrxName())
                .withEntity(document.get_Table_ID(), document.get_ID())
                .addToQueue();
    }
}
