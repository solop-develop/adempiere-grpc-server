/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.sp017.replenishment;

import org.adempiere.core.domains.models.I_M_Replenish;
import org.adempiere.core.domains.models.X_T_Replenish;
import org.compiere.model.MReplenish;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.ReplenishInterface_V2;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ad replenish class based on Demand Inventory Replenishmen
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class DemandAndMinimumInventory implements ReplenishInterface_V2 {
	/**	Last replenish Date	*/
	public static final String COLUMNNAME_LastReplenishmentDate = "LastReplenishmentDate";

	public static final String COLUMNNAME_DateTrx = "DateTrx";
	public static final String COLUMNNAME_DateFrom = "DateFrom";
	public static final String COLUMNNAME_DateTo = "DateTo";

	@Override
	public BigDecimal getQtyToOrder(MWarehouse warehouse, X_T_Replenish replenishLine, SvrProcess processInstance) {
		MReplenish replenish = getReplenishForProduct(replenishLine, warehouse.getM_Warehouse_ID());
		Timestamp lastReplenishmentDate = new Timestamp(System.currentTimeMillis());
		if(replenish != null) {
			if(replenish.get_Value(COLUMNNAME_LastReplenishmentDate) != null) {
				lastReplenishmentDate = (Timestamp) replenish.get_Value(COLUMNNAME_LastReplenishmentDate);
			}
			List<Object> params = new ArrayList<>();
			params.add(replenishLine.getM_Product_ID());
			params.add(warehouse.getM_Warehouse_ID());

			String dateValidation = "AND t.MovementDate >= ? ";

			if (processInstance.getParameterAsTimestamp(COLUMNNAME_DateTrx) != null) {
				dateValidation = "AND t.MovementDate >= ? AND t.MovementDate <= ?";
				params.add(processInstance.getParameterAsTimestamp(COLUMNNAME_DateTrx));
				params.add(processInstance.getParameterToAsTimestamp(COLUMNNAME_DateTrx));
			} else {
				params.add(lastReplenishmentDate);
			}
			//	Get from last replenish
			BigDecimal quantityToOrder = DB.getSQLValueBDEx(replenishLine.get_TrxName(), "SELECT ABS(SUM(t.MovementQty)) "
					+ "FROM M_Transaction t "
					+ "WHERE t.M_Product_ID = ? "
					+ "AND t.MovementType IN('C-', 'C+') "
					+ "AND EXISTS(SELECT 1 FROM M_Locator l WHERE l.M_Locator_ID = t.M_Locator_ID AND l.M_Warehouse_ID = ?)"
					+ dateValidation,
					params.toArray());
			//	
			if(Optional.ofNullable(quantityToOrder).orElse(Env.ZERO).compareTo(Env.ZERO) <= 0
					&& Optional.ofNullable(replenishLine.getLevel_Min()).orElse(Env.ZERO).compareTo(Env.ZERO) > 0
					&& Optional.ofNullable(replenishLine.getLevel_Min()).orElse(Env.ZERO).compareTo(replenishLine.getQtyOnHand()) > 0) {
				quantityToOrder = replenishLine.getLevel_Min().subtract(replenishLine.getQtyOnHand());
			}
			if(replenish.getM_WarehouseSource_ID() > 0) {
				replenishLine.setM_WarehouseSource_ID(replenish.getM_WarehouseSource_ID());
			}
			return Optional.ofNullable(quantityToOrder).orElse(Env.ZERO);
		}
		//	What else?
		//	Nothing
		return Env.ZERO;
	}
	
	/***
	 * Get replenishment for product and warehouse
	 * @param replenishLine
	 * @param warehouseId
	 * @return
	 */
    private MReplenish getReplenishForProduct(X_T_Replenish replenishLine, int warehouseId) {
    	return new Query(replenishLine.getCtx(), I_M_Replenish.Table_Name, "M_Product_ID=? AND AD_Org_ID IN (0, ?) AND M_Warehouse_ID = ? AND ReplenishType = ?", replenishLine.get_TrxName())
    	.setParameters(replenishLine.getM_Product_ID(), replenishLine.getAD_Org_ID(), warehouseId, MReplenish.REPLENISHTYPE_Custom)
    	.setClient_ID()
    	.setOnlyActiveRecords(true)
    	.first();
    }

	@Override
	public BigDecimal getQtyToOrder(MWarehouse mWarehouse, X_T_Replenish xTReplenish) {
		return null;
	}
}
