package org.solop.util;

import org.adempiere.core.domains.models.I_M_StorageSnapshotRun;
import org.compiere.model.MStorageSnapshotRun;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageUpdaterBuilder {
    private final CLogger log = CLogger.getCLogger(this.getClass());
    private final Properties context;
    private final String transactionName;
    private int clientId;
    private int organizationId;
    private int warehouseId;
    private int productId;

    public List<Integer> getProductIds() {
        return productIds;
    }

    private List<Integer> productIds;
    private int productCategoryId;
    private final AtomicInteger processedNewTransactions = new AtomicInteger(0);
    private final AtomicInteger processedNewReservations = new AtomicInteger(0);
    private final AtomicInteger processedSnapshots = new AtomicInteger(0);

    public static StorageUpdaterBuilder newInstance(Properties context, String transactionName) {
        return new StorageUpdaterBuilder(context, transactionName);
    }

    private StorageUpdaterBuilder(Properties context, String transactionName) {
        this.context = context;
        this.transactionName = transactionName;
    }

    public Properties getContext() {
        return context;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public int getClientId() {
        return clientId;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public int getProductId() {
        return productId;
    }

    public int getProductCategoryId() {
        return productCategoryId;
    }

    public StorageUpdaterBuilder withClientId(int clientId) {
        this.clientId = clientId;
        return this;
    }

    public StorageUpdaterBuilder withOrganizationId(int organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public StorageUpdaterBuilder withProductsIds(List<Integer> productsIds) {
        this.productIds = productsIds;
        return this;
    }

    public StorageUpdaterBuilder withWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public StorageUpdaterBuilder withProductId(int productId) {
        this.productId = productId;
        return this;
    }

    public StorageUpdaterBuilder withProductCategoryId(int productCategoryId) {
        this.productCategoryId = productCategoryId;
        return this;
    }

    public String build() {
        if(context == null || transactionName == null) {
            throw new IllegalStateException("Context and Transaction Name are required");
        }
        if(clientId <= 0) {
            throw new IllegalStateException("Client ID must be greater than zero");
        }
        MStorageSnapshotRun lastSnapshot = getLastSnapshot();
        deleteOldStorage();
        if(lastSnapshot != null) {
            createStoreFromSnapshot(lastSnapshot);
            createStoreFromReservations(lastSnapshot);
            createStoreFromTransactions(lastSnapshot);
            log.info("Storage Updated from Snapshot: " + processedSnapshots.get() +
                    ", New Reservations: " + processedNewReservations.get() +
                    ", New Transactions: " + processedNewTransactions.get());
        } else {
            log.info("No previous snapshot found. Storage update skipped.");
        }
        return "Storage Updated from Snapshot: " + processedSnapshots.get() +
                ", New Reservations: " + processedNewReservations.get() +
                ", New Transactions: " + processedNewTransactions.get();
    }

    private MStorageSnapshotRun getLastSnapshot() {
        return new Query(getContext(), I_M_StorageSnapshotRun.Table_Name, null, getTransactionName())
                .setClient_ID()
                .setOnlyActiveRecords(true)
                .setOrderBy("DateLastRun DESC")
                .first();
    }

    private void deleteOldStorage() {
        List<Object> parameters = new ArrayList<>();
        StringBuilder deleteSQL = new StringBuilder("DELETE FROM M_Storage WHERE AD_Client_ID = ? ");
        parameters.add(getClientId());
        if(getOrganizationId() != 0) {
            deleteSQL.append("AND AD_Org_ID = ? ");
            parameters.add(getOrganizationId());
        }
        if(getWarehouseId() != 0) {
            deleteSQL.append("AND EXISTS(SELECT 1 " +
                    "FROM M_Locator l " +
                    "WHERE l.M_Locator_ID = M_Storage.M_Locator_ID " +
                    "AND l.M_Warehouse_ID = ?) ");
            parameters.add(getWarehouseId());
        }
        if(getProductId() != 0) {
            deleteSQL.append("AND M_Product_ID = ? ");
            parameters.add(getProductId());
        }
        if(getProductIds() != null && !getProductIds().isEmpty()) {
            deleteSQL.append("AND M_Product_ID IN").append(getProductIds().toString().replace('[','(').replace(']',')')).append(" ");
        }
        if(getProductCategoryId() != 0) {
            deleteSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID = ? AND M_Product.M_Product_ID = M_Storage.M_Product_ID) ");
            parameters.add(getProductCategoryId());
        }
        //	Log
        log.fine("deleteSQL=" + deleteSQL.toString());

        int storageUpdated = DB.executeUpdateEx(deleteSQL.toString(), parameters.toArray(), getTransactionName());

        //	Log
        log.fine("Storage Deleted=" + storageUpdated);

    }

    private void createStoreFromSnapshot(MStorageSnapshotRun lastSnapshot) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder transactionSQL = new StringBuilder("INSERT INTO M_Storage (M_Storage_ID, AD_Client_ID, AD_Org_ID, M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, QtyOnHand, QtyReserved, QtyOrdered, IsActive, Created, CreatedBy, Updated, UpdatedBy, UUID) " +
                "SELECT nextval('m_storage_seq'), ssr.AD_Client_ID, ssr.AD_Org_ID, sl.M_Product_ID, sl.M_Locator_ID, sl.M_AttributeSetInstance_ID, SUM(sl.QtyOnHand), SUM(sl.QtyReserved), SUM(sl.QtyOrdered), 'Y', now(), ssr.CreatedBy, now(), ssr.UpdatedBy, getUUID() " +
                "FROM M_StorageSnapshotRun ssr " +
                "INNER JOIN M_StorageSnapshot sl ON(sl.M_StorageSnapshotRun_ID = ssr.M_StorageSnapshotRun_ID) " +
                "WHERE sl.AD_Client_ID = ? ");
        parameters.add(getClientId());
        //	Org
        if (getOrganizationId() != 0) {
            transactionSQL.append("AND sl.AD_Org_ID = ? ");
            parameters.add(getOrganizationId());
        }
        //	Warehouse
        if (getWarehouseId() != 0) {
            transactionSQL.append("AND sl.M_Warehouse_ID = ? ");
            parameters.add(getWarehouseId());
        }
        //Product
        if (getProductId() != 0) {
            transactionSQL.append("AND sl.M_Product_ID = ? ");
            parameters.add(getProductId());
        }
        if(getProductIds() != null && !getProductIds().isEmpty()) {
            transactionSQL.append("AND sl.M_Product_ID IN").append(getProductIds().toString().replace('[','(').replace(']',')')).append(" ");
        }
        //Product Category
        if (getProductCategoryId() != 0) {
            transactionSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID= ? AND M_Product.M_Product_ID = sl.M_Product_ID) ");
            parameters.add(getProductCategoryId());
        }
        transactionSQL.append("AND sl.M_StorageSnapshotRun_ID = ? ");
        //	Group By
        transactionSQL.append("GROUP BY ssr.AD_Client_ID, ssr.AD_Org_ID, sl.M_Product_ID, sl.M_Locator_ID, sl.M_AttributeSetInstance_ID, ssr.CreatedBy, ssr.UpdatedBy ");
        parameters.add(lastSnapshot.getM_StorageSnapshotRun_ID());
        log.fine("StorageSQL (Snapshot)=" + transactionSQL);
        int inserted = DB.executeUpdateEx(transactionSQL.toString(), parameters.toArray(), getTransactionName());
        log.fine("Storage Created (Snapshot)=" + inserted);
        if (inserted > 0) {
            processedSnapshots.addAndGet(inserted);
        }
    }

    private void createStoreFromReservations(MStorageSnapshotRun lastSnapshot) {
        List<Object> parameters = new ArrayList<>();
        //	Update the currents
        StringBuilder transactionSQL = new StringBuilder("UPDATE M_Storage AS s " +
                "SET QtyOrdered = s.QtyOrdered + r.QtyOrdered, " +
                "QtyReserved = s.QtyReserved + r.QtyReserved " +
                "FROM (" +
                "SELECT s.M_Storage_ID, " +
                "SUM(CASE WHEN r.ReservationType NOT IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) QtyOrdered, " +
                "SUM(CASE WHEN r.ReservationType IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) QtyReserved " +
                "FROM M_Reservation r " +
                "INNER JOIN M_Storage s ON(s.M_Product_ID = r.M_Product_ID AND s.M_Locator_ID = r.M_Locator_ID AND s.M_AttributeSetInstance_ID = r.M_AttributeSetInstance_ID) ");
        transactionSQL.append("WHERE r.AD_Client_ID = ? ");
        parameters.add(getClientId());
        transactionSQL.append("AND r.DateTrx > ? ");
        parameters.add(lastSnapshot.getDateLastRun());
        //	Org
        if (getOrganizationId() != 0) {
            transactionSQL.append("AND r.AD_Org_ID = ? ");
            parameters.add(getOrganizationId());
        }
        //	Warehouse
        if (getWarehouseId() != 0) {
            transactionSQL.append("AND r.M_Warehouse_ID = ? ");
            parameters.add(getWarehouseId());
        }
        //Product
        if (getProductId() != 0) {
            transactionSQL.append("AND r.M_Product_ID = ? ");
            parameters.add(getProductId());
        }
        if(getProductIds() != null && !getProductIds().isEmpty()) {
            transactionSQL.append("AND r.M_Product_ID IN").append(getProductIds().toString().replace('[','(').replace(']',')')).append(" ");
        }
        //Product Category
        if (getProductCategoryId() != 0) {
            transactionSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID= ? AND M_Product.M_Product_ID = r.M_Product_ID) ");
            parameters.add(getProductCategoryId());
        }
        transactionSQL.append("GROUP BY s.M_Storage_ID " +
                "HAVING SUM(CASE WHEN r.ReservationType IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) <> 0 " +
                "AND SUM(CASE WHEN r.ReservationType NOT IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) <> 0" +
                ") r " +
                "WHERE s.M_Storage_ID = r.M_Storage_ID");
        log.fine("StorageSQL (Reservations Existing)=" + transactionSQL);
        int inserted = DB.executeUpdateEx(transactionSQL.toString(), parameters.toArray(), getTransactionName());
        log.fine("Storage Created (Reservations Existing)=" + inserted);
        if (inserted > 0) {
            processedNewReservations.addAndGet(inserted);
        }
        parameters.clear();
        transactionSQL = new StringBuilder("INSERT INTO M_Storage (M_Storage_ID, AD_Client_ID, AD_Org_ID, M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, QtyOnHand, QtyReserved, QtyOrdered, IsActive, Created, CreatedBy, Updated, UpdatedBy, UUID) " +
                "SELECT nextval('m_storage_seq'), r.AD_Client_ID, r.AD_Org_ID, r.M_Product_ID, r.M_Locator_ID, r.M_AttributeSetInstance_ID, 0, SUM(CASE WHEN r.ReservationType NOT IN('PO+', 'PO-') THEN r.Qty ELSE 0 END), SUM(CASE WHEN r.ReservationType IN('PO+', 'PO-') THEN r.Qty ELSE 0 END), 'Y', now(), 0, now(), 0, getUUID() " +
                "FROM M_Reservation r " +
                "WHERE r.AD_Client_ID = ? ");
        parameters.add(getClientId());
        transactionSQL.append("AND NOT EXISTS(SELECT 1 FROM M_Storage s WHERE s.M_Product_ID = r.M_Product_ID AND s.M_Locator_ID = r.M_Locator_ID AND s.M_AttributeSetInstance_ID = r.M_AttributeSetInstance_ID) ");
        transactionSQL.append("AND r.DateTrx > ? ");
        parameters.add(lastSnapshot.getDateLastRun());
        //	Org
        if (getOrganizationId() != 0) {
            transactionSQL.append("AND r.AD_Org_ID = ? ");
            parameters.add(getOrganizationId());
        }
        //	Warehouse
        if (getWarehouseId() != 0) {
            transactionSQL.append("AND r.M_Warehouse_ID = ? ");
            parameters.add(getWarehouseId());
        }
        //Product
        if (getProductId() != 0) {
            transactionSQL.append("AND r.M_Product_ID = ? ");
            parameters.add(getProductId());
        }
        if(getProductIds() != null && !getProductIds().isEmpty()) {
            transactionSQL.append("AND r.M_Product_ID IN").append(getProductIds().toString().replace('[','(').replace(']',')')).append(" ");
        }
        //Product Category
        if (getProductCategoryId() != 0) {
            transactionSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID= ? AND M_Product.M_Product_ID = r.M_Product_ID) ");
            parameters.add(getProductCategoryId());
        }
        //Group By
        transactionSQL.append("GROUP BY r.AD_Client_ID, r.AD_Org_ID, r.M_Product_ID, r.M_Locator_ID, r.M_Warehouse_ID, r.M_AttributeSetInstance_ID ");
        transactionSQL.append("HAVING SUM(CASE WHEN r.ReservationType IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) <> 0 AND SUM(CASE WHEN r.ReservationType NOT IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) <> 0");
        log.fine("StorageSQL (Reservations)=" + transactionSQL);
        inserted = DB.executeUpdateEx(transactionSQL.toString(), parameters.toArray(), getTransactionName());
        log.fine("Storage Created (Reservations)=" + inserted);
        if (inserted > 0) {
            processedNewReservations.addAndGet(inserted);
        }
    }

    private void createStoreFromTransactions(MStorageSnapshotRun lastSnapshot) {
        List<Object> parameters = new ArrayList<>();
        //	Update the currents
        StringBuilder transactionSQL = new StringBuilder("UPDATE M_Storage AS s " +
                "SET QtyOnHand = s.QtyOnHand + r.QtyOnHand " +
                "FROM (" +
                "SELECT s.M_Storage_ID, SUM(r.MovementQty) QtyOnHand " +
                "FROM M_Transaction r " +
                "INNER JOIN M_Storage s ON(s.M_Product_ID = r.M_Product_ID AND s.M_Locator_ID = r.M_Locator_ID AND s.M_AttributeSetInstance_ID = r.M_AttributeSetInstance_ID) ");
        transactionSQL.append("WHERE r.AD_Client_ID = ? ");
        parameters.add(getClientId());
        transactionSQL.append("AND r.Created > ? ");
        parameters.add(lastSnapshot.getDateLastRun());
        //	Org
        if (getOrganizationId() != 0) {
            transactionSQL.append("AND r.AD_Org_ID = ? ");
            parameters.add(getOrganizationId());
        }
        //	Warehouse
        if (getWarehouseId() != 0) {
            transactionSQL.append("AND EXISTS(SELECT 1 FROM M_Locator l WHERE l.M_Locator_ID = r.M_Locator_ID AND l.M_Warehouse_ID = ?) ");
            parameters.add(getWarehouseId());
        }
        //Product
        if (getProductId() != 0) {
            transactionSQL.append("AND r.M_Product_ID = ? ");
            parameters.add(getProductId());
        }
        if(getProductIds() != null && !getProductIds().isEmpty()) {
            transactionSQL.append("AND r.M_Product_ID IN").append(getProductIds().toString().replace('[','(').replace(']',')')).append(" ");
        }
        //Product Category
        if (getProductCategoryId() != 0) {
            transactionSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID= ? AND M_Product.M_Product_ID = r.M_Product_ID) ");
            parameters.add(getProductCategoryId());
        }
        transactionSQL.append("GROUP BY s.M_Storage_ID " +
                "HAVING SUM(r.MovementQty) <> 0) r " +
                "WHERE s.M_Storage_ID = r.M_Storage_ID");
        log.fine("StorageSQL (Transaction Existing)=" + transactionSQL);
        int inserted = DB.executeUpdateEx(transactionSQL.toString(), parameters.toArray(), getTransactionName());
        log.fine("Storage Created (Transaction Existing)=" + inserted);
        if (inserted > 0) {
            processedNewTransactions.addAndGet(inserted);
        }
        parameters.clear();
        transactionSQL = new StringBuilder("INSERT INTO M_Storage (M_Storage_ID, AD_Client_ID, AD_Org_ID, M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, QtyOnHand, QtyReserved, QtyOrdered, IsActive, Created, CreatedBy, Updated, UpdatedBy, UUID) " +
                "SELECT nextval('m_storage_seq'), mt.AD_Client_ID,mw.AD_Org_ID, mt.M_Product_ID, mt.M_Locator_ID, mt.M_AttributeSetInstance_ID, Sum(mt.MovementQty) MovementQty, 0, 0, 'Y', now(), 0, now(), 0, getUUID() " +
                "FROM M_Transaction mt " +
                "INNER JOIN M_Locator ml ON mt.M_Locator_ID = ml.M_Locator_ID " +
                "INNER JOIN M_Warehouse mw ON mw.M_Warehouse_ID = ml.M_Warehouse_ID " +
                "WHERE mt.AD_Client_ID = ? ");
        parameters.add(getClientId());
        transactionSQL.append("AND NOT EXISTS(SELECT 1 FROM M_Storage s WHERE s.M_Product_ID = mt.M_Product_ID AND s.M_Locator_ID = mt.M_Locator_ID AND s.M_AttributeSetInstance_ID = mt.M_AttributeSetInstance_ID) ");
        transactionSQL.append("AND mt.Created > ? ");
        parameters.add(lastSnapshot.getDateLastRun());
        //	Org
        if (getOrganizationId() != 0) {
            transactionSQL.append("AND mw.AD_Org_ID = ? ");
            parameters.add(getOrganizationId());
        }
        //	Warehouse
        if (getWarehouseId() != 0) {
            transactionSQL.append("AND mw.M_Warehouse_ID = ? ");
            parameters.add(getWarehouseId());
        }
        //Product
        if (getProductId() != 0) {
            transactionSQL.append("AND mt.M_Product_ID = ? ");
            parameters.add(getProductId());
        }
        if(getProductIds() != null && !getProductIds().isEmpty()) {
            transactionSQL.append("AND mt.M_Product_ID IN").append(getProductIds().toString().replace('[','(').replace(']',')')).append(" ");
        }
        //Product Category
        if (getProductCategoryId() != 0) {
            transactionSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID= ? AND M_Product.M_Product_ID = mt.M_Product_ID) ");
            parameters.add(getProductCategoryId());
        }
        //Group By
        transactionSQL.append("GROUP BY mt.AD_Client_ID, mw.AD_Org_ID, mt.M_Product_ID, mt.M_Locator_ID, ml.M_Warehouse_ID, mt.M_AttributeSetInstance_ID ");
        transactionSQL.append("HAVING SUM(mt.MovementQty) <> 0 ");
        log.fine("SnapshotSQL (Transactions)=" + transactionSQL);
        inserted = DB.executeUpdateEx(transactionSQL.toString(), parameters.toArray(), getTransactionName());
        log.fine("Snapshot Created (Transactions)=" + inserted);
        if (inserted > 0) {
            processedNewTransactions.addAndGet(inserted);
        }
    }
}
