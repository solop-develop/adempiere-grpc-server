/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.solop.process;

import org.compiere.model.MStorageSnapshotRun;
import org.compiere.util.DB;
import org.solop.util.StorageUpdaterBuilder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Update Storage)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public class UpdateStorage extends UpdateStorageAbstract {

	private final AtomicInteger processedTransactions = new AtomicInteger(0);
	private final AtomicInteger processedReservations = new AtomicInteger(0);

	@Override
	protected void prepare() {
		super.prepare();
		if(isCreateSnapshot() && (getWarehouseId() > 0 || getProductCategoryId() > 0 || getProductId() > 0)) {
			throw new IllegalArgumentException("@SSErrorStorage@");
		}
	}

	@Override
	protected String doIt() throws Exception {
		MStorageSnapshotRun snapshotRun = null;
		if(isCreateSnapshot()) {
			snapshotRun = new MStorageSnapshotRun(getCtx(), 0, get_TrxName());
			snapshotRun.setDateLastRun(new Timestamp(System.currentTimeMillis()));
			snapshotRun.saveEx();
			//	Create from Reservations
			createSnapshotFromReservations(snapshotRun);
			//	Create from Transactions
			createSnapshotFromTransactions(snapshotRun);
			//
			snapshotRun.setTransactionsProcessed(BigDecimal.valueOf(processedTransactions.get()));
			snapshotRun.saveEx();
		}
		//	Delete Old Storage
		StorageUpdaterBuilder.newInstance(getCtx(), get_TrxName())
				.withClientId(getAD_Client_ID())
				.withOrganizationId(getOrgId())
				.withWarehouseId(getWarehouseId())
				.withProductCategoryId(getProductCategoryId())
				.withProductId(getProductId())
				.build();

		if(snapshotRun != null) {
			snapshotRun.setProductProcesses(BigDecimal.valueOf(getProcessedProducts()));
			snapshotRun.saveEx();
		}
		return "@TransactionsProcessed@ " + processedTransactions.get();
	}

	private int getProcessedProducts() {
		return DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM M_Product p WHERE p.AD_Client_ID = ? AND EXISTS(SELECT 1 FROM M_Storage s WHERE s.M_Product_ID = p.M_Product_ID)", getAD_Client_ID());
	}

	private void createSnapshotFromReservations(MStorageSnapshotRun snapshotRun) {
		List<Object> parameters = new ArrayList<>();
		StringBuilder transactionSQL = new StringBuilder("INSERT INTO M_StorageSnapshot (M_StorageSnapshot_ID, AD_Client_ID, AD_Org_ID, M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, QtyOnHand, QtyReserved, QtyOrdered, IsActive, Created, CreatedBy, Updated, UpdatedBy, UUID, M_StorageSnapshotRun_ID, DateTrx) " +
				"SELECT nextval('m_storagesnapshot_seq'), r.AD_Client_ID, r.AD_Org_ID, r.M_Product_ID, r.M_Locator_ID, r.M_AttributeSetInstance_ID, 0, SUM(CASE WHEN r.ReservationType NOT IN('PO+', 'PO-') THEN r.Qty ELSE 0 END), SUM(CASE WHEN r.ReservationType IN('PO+', 'PO-') THEN r.Qty ELSE 0 END), 'Y', now(), 0, now(), 0, getUUID(), ?, ? " +
				"FROM M_Reservation r " +
				"WHERE r.AD_Client_ID = ? ");
		parameters.add(snapshotRun.getM_StorageSnapshotRun_ID());
		parameters.add(snapshotRun.getDateLastRun());
		parameters.add(getAD_Client_ID());
		//	Org
		if (getOrgId() != 0) {
			transactionSQL.append("AND r.AD_Org_ID = ? ");
			parameters.add(getOrgId());
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
		//Product Category
		if (getProductCategoryId() != 0) {
			transactionSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID= ? AND M_Product.M_Product_ID = r.M_Product_ID) ");
			parameters.add(getProductCategoryId());
		}
		//Group By
		transactionSQL.append("GROUP BY r.AD_Client_ID, r.AD_Org_ID, r.M_Product_ID, r.M_Locator_ID, r.M_Warehouse_ID, r.M_AttributeSetInstance_ID ");
		transactionSQL.append("HAVING SUM(CASE WHEN r.ReservationType IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) <> 0 AND SUM(CASE WHEN r.ReservationType NOT IN('PO+', 'PO-') THEN r.Qty ELSE 0 END) <> 0");
		log.fine("SnapshotSQL (Reservations)=" + transactionSQL);
		int inserted = DB.executeUpdateEx(transactionSQL.toString(), parameters.toArray(), get_TrxName());
		log.fine("Snapshot Created (Reservations)=" + inserted);
		if (inserted > 0) {
			processedReservations.addAndGet(inserted);
		}
	}

	private void createSnapshotFromTransactions(MStorageSnapshotRun snapshotRun) {
		List<Object> parameters = new ArrayList<>();
		StringBuilder transactionSQL = new StringBuilder("INSERT INTO M_StorageSnapshot (M_StorageSnapshot_ID, AD_Client_ID, AD_Org_ID, M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, QtyOnHand, QtyReserved, QtyOrdered, IsActive, Created, CreatedBy, Updated, UpdatedBy, UUID, M_StorageSnapshotRun_ID, DateTrx) " +
				"SELECT nextval('m_storagesnapshot_seq'), mt.AD_Client_ID,mw.AD_Org_ID, mt.M_Product_ID, mt.M_Locator_ID, mt.M_AttributeSetInstance_ID, Sum(mt.MovementQty) MovementQty, 0, 0, 'Y', now(), 0, now(), 0, getUUID(), ?, ? " +
				"FROM M_Transaction mt " +
				"INNER JOIN M_Locator ml ON mt.M_Locator_ID = ml.M_Locator_ID " +
				"INNER JOIN M_Warehouse mw ON mw.M_Warehouse_ID = ml.M_Warehouse_ID " +
				"WHERE mt.AD_Client_ID = ? ");
		parameters.add(snapshotRun.getM_StorageSnapshotRun_ID());
		parameters.add(snapshotRun.getDateLastRun());
		parameters.add(getAD_Client_ID());
		//	Org
		if (getOrgId() != 0) {
			transactionSQL.append("AND mw.AD_Org_ID = ? ");
			parameters.add(getOrgId());
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
		//Product Category
		if (getProductCategoryId() != 0) {
			transactionSQL.append("AND EXISTS (SELECT 1 FROM M_Product WHERE M_Product.M_Product_Category_ID= ? AND M_Product.M_Product_ID = mt.M_Product_ID) ");
			parameters.add(getProductCategoryId());
		}
		//Group By
		transactionSQL.append("GROUP BY mt.AD_Client_ID, mw.AD_Org_ID, mt.M_Product_ID, mt.M_Locator_ID, ml.M_Warehouse_ID, mt.M_AttributeSetInstance_ID ");
		transactionSQL.append("HAVING SUM(mt.MovementQty) <> 0 ");
		log.fine("SnapshotSQL (Transactions)=" + transactionSQL);
		int inserted = DB.executeUpdateEx(transactionSQL.toString(), parameters.toArray(), get_TrxName());
		log.fine("Snapshot Created (Transactions)=" + inserted);
		if (inserted > 0) {
			processedTransactions.addAndGet(inserted);
		}
	}
}