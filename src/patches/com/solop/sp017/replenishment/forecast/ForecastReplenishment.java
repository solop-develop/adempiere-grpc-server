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
package com.solop.sp017.replenishment.forecast;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.core.domains.models.I_M_ForecastLine;
import org.adempiere.core.domains.models.I_M_Replenish;
import org.compiere.model.MReplenish;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.adempiere.core.domains.models.X_T_Replenish;
import org.compiere.util.Env;
import org.compiere.util.ReplenishInterface;
import org.compiere.util.TimeUtil;

/**
 * Ad replenish class based on Forecast
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class ForecastReplenishment implements ReplenishInterface {
    /**	Last replenish Date	*/
    public static final String COLUMNNAME_LastReplenishmentDate = "LastReplenishmentDate";

    @Override
    public BigDecimal getQtyToOrder(MWarehouse warehouse, X_T_Replenish replenishLine) {

        MReplenish replenish = getReplenishForProduct(replenishLine, warehouse.getM_Warehouse_ID());
        Timestamp lastReplenishmentDate = new Timestamp(System.currentTimeMillis());
        BigDecimal quantityToOrder = Env.ZERO;
        if(replenish != null) {
            int manufacturyDays = replenish.get_ValueAsInt("DEMO1_ManufacturyDays");
            int maritimeTransportDays = replenish.get_ValueAsInt("DEMO1_MaritimeDays");
            int airTransportDays = replenish.get_ValueAsInt("DEMO1_AirDays");
            int minimumStockDays = replenish.get_ValueAsInt("DEMO1_MinStockDays");
            int maximumStockDays = replenish.get_ValueAsInt("DEMO1_MaxStockDays");
            int replenishmentLeadTimeDays = manufacturyDays + maritimeTransportDays + maximumStockDays;
            if(replenishmentLeadTimeDays > 0) {
                Timestamp currentDate = TimeUtil.getMonthLastDay(new Timestamp(System.currentTimeMillis()));
                currentDate = TimeUtil.addDays(currentDate, 1);
                lastReplenishmentDate = TimeUtil.addDays(currentDate, replenishmentLeadTimeDays);
                lastReplenishmentDate = TimeUtil.getMonthLastDay(lastReplenishmentDate);
                quantityToOrder = new Query(replenish.getCtx(), I_M_ForecastLine.Table_Name, "M_Product_ID = ? AND DatePromised >= ? AND DatePromised <= ?", replenish.get_TrxName())
                        .setParameters(replenishLine.getM_Product_ID(), currentDate, lastReplenishmentDate)
                        .setOnlyActiveRecords(true)
                        .setClient_ID()
                        .aggregate(I_M_ForecastLine.COLUMNNAME_Qty, Query.AGGREGATE_SUM);
                if(replenish.get_Value(COLUMNNAME_LastReplenishmentDate) != null) {
                    lastReplenishmentDate = (Timestamp) replenish.get_Value(COLUMNNAME_LastReplenishmentDate);
                }
            }
            if(replenish.getM_WarehouseSource_ID() > 0) {
                replenishLine.setM_WarehouseSource_ID(replenish.getM_WarehouseSource_ID());
                replenishLine.saveEx();
            }
            //
            BigDecimal quantityOrdered = Optional.ofNullable(replenishLine.getQtyOrdered()).orElse(Env.ZERO);
            BigDecimal quantityReserved = Optional.ofNullable(replenishLine.getQtyReserved()).orElse(Env.ZERO);
            quantityToOrder = Optional.ofNullable(quantityToOrder).orElse(Env.ZERO);
            return quantityToOrder.subtract(quantityOrdered).add(quantityReserved);
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

}