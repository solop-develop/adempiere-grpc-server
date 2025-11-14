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

import org.adempiere.core.domains.models.X_T_Replenish;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.ReplenishInterface;
import org.compiere.util.ReplenishInterface_V2;
import org.compiere.util.Trx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * 	Generated Process for (Product Replenishment Search)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *  @version Release 5.1.2
 */
public class ProductReplenishmentSearch extends ProductReplenishmentSearchAbstract {

	private final Map<String, ReplenishInterface> replenishmentResolver = new HashMap<>();

	@Override
	protected void prepare() {
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception {
		Trx.run(this::prepareTable);
		AtomicReference<String> result = new AtomicReference<>();
		Trx.run(transactionName -> {
			result.set(insertReplenish(transactionName));
		});
		Trx.run(this::runRules);
		return result.get();
	}

	private ReplenishInterface getResolver(String className) {
		ReplenishInterface resolver = null;
		if(className == null) {
			return null;
		}
		if(replenishmentResolver.containsKey(className)) {
			return replenishmentResolver.get(className);
		} else {
			try {
				Class<?> clazz = Class.forName(className);
				resolver = (ReplenishInterface) clazz.getDeclaredConstructor().newInstance();
				replenishmentResolver.put(className, resolver);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Replenishment Class Not Found"
						+ className, e);
			}
		}
		return resolver;
	}

	private void runRules(String transactionName) {
		//	Custom Replenishment
		getCustomReplenish()
				.parallelStream()
				.forEach(replenish -> {
			if(replenish.get_ValueAsString("ReplenishmentClass") != null) {
				ReplenishInterface custom = getResolver(replenish.get_ValueAsString("ReplenishmentClass"));
				if(custom != null) {
					BigDecimal qto = null;
					try {
						MWarehouse warehouse = MWarehouse.get(getCtx(), replenish.getM_Warehouse_ID());
						if (ReplenishInterface_V2.class.isAssignableFrom(custom.getClass())){
							qto = ((ReplenishInterface_V2)custom).getQtyToOrder(warehouse, replenish, this);
						} else {
							qto = custom.getQtyToOrder(warehouse, replenish);
						}
					} catch (Exception e) {
						log.log(Level.SEVERE, custom.toString(), e);
					}
					if (qto == null) {
						qto = Env.ZERO;
					}
					replenish.setQtyToOrder(qto);
					replenish.saveEx();
				}
			}
		});
		//	Delete rows where nothing to order
		String sql = "DELETE FROM T_Replenish "
				+ "WHERE QtyToOrder < 1"
				+ " AND AD_PInstance_ID=" + getAD_PInstance_ID();
		int no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Delete No QtyToOrder=" + no);
		}
	}

	private String insertReplenish(String transactionName) {
		List<Object> parameters = new ArrayList<>();
		StringBuilder insertSql = new StringBuilder("INSERT INTO T_Replenish (AD_PInstance_ID, " +
				"AD_Client_ID, " +
				"AD_Org_ID, " +
				"M_Product_ID, " +
				"ReplenishType, " +
				"Level_Min, " +
				"Level_Max, " +
				"QtyAvailable, " +
				"QtyOnHand, " +
				"QtyReserved, " +
				"QtyOrdered, " +
				"QtyToOrder, " +
				"M_Warehouse_ID, " +
				"M_WarehouseSource_ID, " +
				"C_BPartner_ID, " +
				"Order_Min, " +
				"Order_Pack, " +
				"ReplenishmentClass," +
				"DateTrx) ");

		insertSql.append("SELECT " + getAD_PInstance_ID() + ", r.AD_Client_ID," +
				"    r.AD_Org_ID," +
				"    r.M_Product_ID," +
				"    r.ReplenishType," +
				"    r.Level_Min," +
				"    r.Level_Max," +
				"    r.QtyAvailable," +
				"    r.QtyOnHand," +
				"    r.QtyReserved," +
				"    r.QtyOrdered," +
				"    CASE" +
				"     WHEN r.Order_Pack > 0 AND MOD(CASE" +
				"        WHEN r.QtyToOrder < r.Order_Min AND r.QtyToOrder > 0" +
				"        THEN r.Order_Min" +
				"        ELSE r.QtyToOrder" +
				"       END, CASE WHEN COALESCE(r.Order_Pack, 0) > 0 THEN r.Order_Pack ELSE 1 END) <> 0 AND r.QtyToOrder > 0" +
				"     THEN CASE" +
				"       WHEN r.QtyToOrder < r.Order_Min AND r.QtyToOrder > 0" +
				"       THEN r.Order_Min" +
				"       ELSE r.QtyToOrder" +
				"      END - MOD(CASE" +
				"         WHEN r.QtyToOrder < r.Order_Min AND r.QtyToOrder > 0" +
				"         THEN r.Order_Min" +
				"         ELSE r.QtyToOrder" +
				"        END, CASE WHEN COALESCE(r.Order_Pack, 0) > 0 THEN r.Order_Pack ELSE 1 END) + r.Order_Pack" +
				"     ELSE r.QtyToOrder" +
				"    END AS QtyToOrder," +
				"    r.M_Warehouse_ID," +
				"    r.M_WarehouseSource_ID," +
				"    r.C_BPartner_ID," +
				"    r.Order_Min," +
				"    r.Order_Pack, " +
				"    r.ReplenishmentClass, " +
				"    ?" +
				" FROM (SELECT r.AD_Client_ID," +
				"        r.AD_Org_ID," +
				"        r.M_Product_ID," +
				"        r.ReplenishType," +
				"        COALESCE(r.Level_Min, 0) AS Level_Min," +
				"     COALESCE(r.Level_Max, 0) AS Level_Max," +
				"     COALESCE(s.QtyAvailable, 0) AS QtyAvailable," +
				"     COALESCE(s.QtyOnHand, 0) AS QtyOnHand," +
				"     COALESCE(s.QtyReserved, 0) AS QtyReserved," +
				"     COALESCE(s.QtyOrdered, 0) AS QtyOrdered," +
				"        COALESCE(CASE" +
				"            WHEN r.ReplenishType = '1' THEN" +
				"                CASE" +
				"                    WHEN COALESCE(s.QtyOnHand, 0) - COALESCE(s.QtyReserved, 0) + COALESCE(s.QtyOrdered, 0) <= r.Level_Min" +
				"                    THEN r.Level_Max - COALESCE(s.QtyOnHand, 0) + COALESCE(s.QtyReserved, 0) - COALESCE(s.QtyOrdered, 0)" +
				"                    ELSE 0" +
				"                END" +
				"            WHEN r.ReplenishType = '2' THEN r.Level_Max - COALESCE(s.QtyOnHand, 0) + COALESCE(s.QtyReserved, 0) - COALESCE(s.QtyOrdered, 0)" +
				"            ELSE 0" +
				"        END, 0) AS QtyToOrder," +
				"        r.M_Warehouse_ID," +
				"        COALESCE(r.M_WarehouseSource_ID, w.M_WarehouseSource_ID) AS M_WarehouseSource_ID," +
				"        po.C_BPartner_ID," +
				"        COALESCE(po.Order_Min, 0) AS Order_Min," +
				"        COALESCE(po.Order_Pack, 0) AS Order_Pack, " +
				"        COALESCE(r.ReplenishmentClass, w.ReplenishmentClass) AS ReplenishmentClass" +
				"    FROM M_Replenish r" +
				"    INNER JOIN M_Warehouse w ON(w.M_Warehouse_ID = r.M_Warehouse_ID)" +
				"    LEFT JOIN M_Product_PO po ON(po.M_Product_ID = r.M_Product_ID AND po.IsActive = 'Y' AND po.IsCurrentVendor = 'Y')" +
				"    LEFT JOIN (SELECT s.M_Product_ID, s.M_Warehouse_ID," +
				"                SUM(s.QtyOnHand) AS QtyOnHand," +
				"                SUM(s.QtyOrdered) AS QtyOrdered," +
				"                SUM(s.QtyReserved) AS QtyReserved," +
				"                SUM(s.QtyAvailable) AS QtyAvailable" +
				"            FROM RV_Storage s" +
				"            GROUP BY s.M_Product_ID, s.M_Warehouse_ID) s ON(s.M_Product_ID = r.M_Product_ID AND s.M_Warehouse_ID = r.M_Warehouse_ID)" +
				"    WHERE r.IsActive = 'Y') r " +
				"INNER JOIN M_Product p ON(p.M_Product_ID = r.M_Product_ID)");
		//	Add Where Clause
		insertSql.append(" WHERE r.M_Warehouse_ID = ?");

		parameters.add(getDateTrx());
		parameters.add(getWarehouseId());
		//	Organization
		if(getOrgId() > 0) {
			insertSql.append(" AND r.AD_Org_ID = ?");
			parameters.add(getOrgId());
		}
		//	Source Warehouse
		if(getWarehouseSourceId() > 0) {
			insertSql.append(" AND r.M_WarehouseSource_ID = ?");
			parameters.add(getWarehouseSourceId());
		}
		//	Product
		if(getProductId() > 0) {
			insertSql.append(" AND r.M_Product_ID = ?");
			parameters.add(getProductId());
		}
		//	Product Brand
		if(getBrandId() > 0) {
			insertSql.append(" AND p.M_Brand_ID = ?");
			parameters.add(getBrandId());
		}
		//	Product Industry Sector
		if(getIndustrySectorId() > 0) {
			insertSql.append(" AND p.M_Industry_Sector_ID = ?");
			parameters.add(getIndustrySectorId());
		}
		//	Product Material Type
		if(getMaterialGroupId() > 0) {
			insertSql.append(" AND p.M_Material_Group_ID = ?");
			parameters.add(getMaterialGroupId());
		}
		//	Product Material Type
		if(getMaterialTypeId() > 0) {
			insertSql.append(" AND p.M_Material_Type_ID = ?");
			parameters.add(getMaterialTypeId());
		}
		//	Product Part Type
		if(getPartTypeId() > 0) {
			insertSql.append(" AND p.M_PartType_ID = ?");
			parameters.add(getPartTypeId());
		}
		//	Product Category
		if(getProductCategoryId() > 0) {
			insertSql.append(" AND p.M_Product_Category_ID = ?");
			parameters.add(getProductCategoryId());
		}
		//	Product Class
		if(getProductClassId() > 0) {
			insertSql.append(" AND p.M_Product_Class_ID = ?");
			parameters.add(getProductClassId());
		}
		//	Product Classification
		if(getProductClassificationId() > 0) {
			insertSql.append(" AND p.M_Product_Classification_ID = ?");
			parameters.add(getProductClassificationId());
		}
		//	Product Group
		if(getProductGroupId() > 0) {
			insertSql.append(" AND p.M_Product_Group_ID = ?");
			parameters.add(getProductGroupId());
		}
		//	Product Purchase Group
		if(getPurchaseGroupId() > 0) {
			insertSql.append(" AND p.M_Purchase_Group_ID = ?");
			parameters.add(getPurchaseGroupId());
		}
		//	Product Sales Group
		if(getSalesGroupId() > 0) {
			insertSql.append(" AND p.M_Sales_Group_ID = ?");
			parameters.add(getSalesGroupId());
		}
		//	Vendor
		if(getBPartnerId() > 0) {
			insertSql.append(" AND r.C_BPartner_ID = ?");
			parameters.add(getBPartnerId());
		}
		//	Vendor
		if(getReplenishType() != null) {
			insertSql.append(" AND r.ReplenishType = ?");
			parameters.add(getReplenishType());
		}
		int inserted = DB.executeUpdateEx(insertSql.toString(), parameters.toArray(), transactionName);
		if (inserted != 0) {
			log.fine("Filled Replenishments = " + inserted);
		}
		//	Delete inactive products and replenishments
		String sql = "DELETE T_Replenish r "
				+ "WHERE (EXISTS (SELECT * FROM M_Product p "
				+ "WHERE p.M_Product_ID=r.M_Product_ID AND p.IsActive='N')"
				+ " OR EXISTS (SELECT * FROM M_Replenish rr "
				+ " WHERE rr.M_Product_ID=r.M_Product_ID AND rr.IsActive='N'"
				+ " AND rr.M_Warehouse_ID=" + getWarehouseId() + " ))"
				+ " AND AD_PInstance_ID=" + getAD_PInstance_ID();
		int no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Delete Inactive=" + no);
		}

		//	Ensure Data consistency
		sql = "UPDATE T_Replenish SET QtyOnHand = 0 WHERE QtyOnHand IS NULL";
		no = DB.executeUpdateEx(sql, transactionName);
		sql = "UPDATE T_Replenish SET QtyReserved = 0 WHERE QtyReserved IS NULL";
		no = DB.executeUpdateEx(sql, transactionName);
		sql = "UPDATE T_Replenish SET QtyOrdered = 0 WHERE QtyOrdered IS NULL";
		no = DB.executeUpdateEx(sql, transactionName);

		//	Set Minimum / Maximum Maintain Level
		//	X_M_Replenish.REPLENISHTYPE_ReorderBelowMinimumLevel
		sql = "UPDATE T_Replenish"
				+ " SET QtyToOrder = CASE WHEN QtyOnHand - QtyReserved + QtyOrdered <= Level_Min "
				+ " THEN Level_Max - QtyOnHand + QtyReserved - QtyOrdered "
				+ " ELSE 0 END "
				+ "WHERE ReplenishType='1'"
				+ " AND AD_PInstance_ID=" + getAD_PInstance_ID();
		no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Update Type-1=" + no);
		}
		//
		//	X_M_Replenish.REPLENISHTYPE_MaintainMaximumLevel
		sql = "UPDATE T_Replenish"
				+ " SET QtyToOrder = Level_Max - QtyOnHand + QtyReserved - QtyOrdered "
				+ "WHERE ReplenishType='2'"
				+ " AND AD_PInstance_ID=" + getAD_PInstance_ID();
		no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Update Type-2=" + no);
		}
		//	Minimum Order Quantity
		sql = "UPDATE T_Replenish"
				+ " SET QtyToOrder = Order_Min "
				+ "WHERE QtyToOrder < Order_Min"
				+ " AND QtyToOrder > 0"
				+ " AND AD_PInstance_ID=" + getAD_PInstance_ID();
		no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Set MinOrderQty=" + no);
		}
		//	Even dividable by Pack
		sql = "UPDATE T_Replenish"
				+ " SET QtyToOrder = QtyToOrder - MOD(QtyToOrder, CASE WHEN COALESCE(Order_Pack, 0) > 0 THEN Order_Pack ELSE 1 END) + Order_Pack "
				+ "WHERE MOD(QtyToOrder, CASE WHEN COALESCE(Order_Pack, 0) > 0 THEN Order_Pack ELSE 1 END) <> 0"
				+ " AND QtyToOrder > 0"
				+ " AND AD_PInstance_ID=" + getAD_PInstance_ID();
		no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Set OrderPackQty=" + no);
		}
		//	Check Source Warehouse
		sql = "UPDATE T_Replenish"
				+ " SET M_WarehouseSource_ID = NULL "
				+ "WHERE M_Warehouse_ID=M_WarehouseSource_ID"
				+ " AND AD_PInstance_ID=" + getAD_PInstance_ID();
		no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Set same Source Warehouse=" + no);
		}
		return "@Created@ " + inserted;
	}

	/**
	 * 	Prepare/Check Replenishment Table
	 */
	private void prepareTable(String transactionName) {
		//	Level_Max must be >= Level_Max
		String sql = "UPDATE M_Replenish"
				+ " SET Level_Max = Level_Min "
				+ "WHERE Level_Max < Level_Min";
		int no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Corrected Max_Level=" + no);
		}

		//	Just to be sure
		sql = "DELETE T_Replenish WHERE AD_PInstance_ID=" + getAD_PInstance_ID();
		no = DB.executeUpdateEx(sql, transactionName);
		if (no != 0) {
			log.fine("Delete Existing Temp=" + no);
		}
	}	//	prepareTable

	/**
	 * 	Get Replenish Records
	 *	@return replenish
	 */
	private List<X_T_Replenish> getCustomReplenish() {
		return new Query(getCtx(), X_T_Replenish.Table_Name,
				"AD_PInstance_ID = ?" + " AND " + "ReplenishType='9'", null)
                .setParameters(getAD_PInstance_ID())
                .setOrderBy("M_Warehouse_ID, M_WarehouseSource_ID, C_BPartner_ID")
                .list();
	}	//	getReplenish
}