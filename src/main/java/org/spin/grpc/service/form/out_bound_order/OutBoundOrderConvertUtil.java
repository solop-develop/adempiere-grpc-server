/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.service.form.out_bound_order;

import java.math.BigDecimal;
// import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.X_C_Order;
import org.compiere.model.MRefList;
// import org.compiere.model.MUOM;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.form.out_bound_order.DocumentHeader;
import org.spin.backend.grpc.form.out_bound_order.DocumentLine;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;

public class OutBoundOrderConvertUtil {

	public static DocumentHeader.Builder convertDocumentHeader(ResultSet resultSet) throws SQLException {
		DocumentHeader.Builder builder = DocumentHeader.newBuilder();
		if (resultSet == null) {
			return builder;
		}
		builder.setId(
				resultSet.getInt("ID")
			)
			.setUuid(
				StringManager.getValidString(
					resultSet.getString(
						I_AD_Element.COLUMNNAME_UUID
					)
				)
			)
			.setDocumentNo(
				StringManager.getValidString(
					resultSet.getString(
						I_C_Order.COLUMNNAME_DocumentNo
					)
				)
			)
			.setDateOrdered(
				TimeManager.convertDateToValue(
					resultSet.getTimestamp(
						I_C_Order.COLUMNNAME_DateOrdered
					)
				)
			)
			.setDatePromised(
				TimeManager.convertDateToValue(
					resultSet.getTimestamp(
						I_C_Order.COLUMNNAME_DatePromised
					)
				)
			)
			.setWarehouseId(
				resultSet.getInt(
					I_C_Order.COLUMNNAME_M_Warehouse_ID
				)
			)
			.setWarehouse(
				StringManager.getValidString(
					resultSet.getString("Warehouse")
				)
			)
			.setRegion(
				StringManager.getValidString(
					resultSet.getString("Region")
				)
			)
			.setCity(
				StringManager.getValidString(
					resultSet.getString("City")
				)
			)
			.setSalesRepresentativeId(
				resultSet.getInt(
					I_C_Order.COLUMNNAME_SalesRep_ID
				)
			)
			.setSalesRepresentative(
				StringManager.getValidString(
					resultSet.getString("SalesRep")
				)
			)
			.setBusinessPartnerId(
				resultSet.getInt(
					I_C_Order.COLUMNNAME_C_BPartner_ID
				)
			)
			.setBusinessPartnerValue(
				StringManager.getValidString(
					resultSet.getString("PartnerValue")
				)
			)
			.setBusinessPartner(
				StringManager.getValidString(
					resultSet.getString("Partner")
				)
			)
			.setLocationId(
				resultSet.getInt("C_BPartner_Location_ID")
			)
			.setLocation(
				StringManager.getValidString(
					resultSet.getString("Location")
				)
			)
			.setAddress1(
				StringManager.getValidString(
					resultSet.getString("Address1")
				)
			)
			.setAddress2(
				StringManager.getValidString(
					resultSet.getString("Address2")
				)
			)
			.setAddress3(
				StringManager.getValidString(
					resultSet.getString("Address3")
				)
			)
			.setAddress4(
				StringManager.getValidString(
					resultSet.getString("Address4")
				)
			)
			.setWeight(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("Weight")
				)
			)
			.setVolume(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("Volume")
				)
			)
		;
		return builder;
	}

	public static DocumentLine.Builder convertDocumentLine(ResultSet resultSet) throws SQLException {
		DocumentLine.Builder builder = DocumentLine.newBuilder();
		if (resultSet == null) {
			return builder;
		}
		//	Parameter
		int productUOMId = resultSet.getInt("C_UOM_ID");
		BigDecimal qtyOnHand = Optional.ofNullable(
			resultSet.getBigDecimal("QtyOnHand")
		).orElse(
			Env.ZERO
		);
		BigDecimal qty = Optional.ofNullable(
			resultSet.getBigDecimal("Qty")
		).orElse(
			Env.ZERO
		);
		String deliveryRuleKey = StringManager.getValidString(
			resultSet.getString("DeliveryRule")
		);
		if(Util.isEmpty(deliveryRuleKey, true)) {
			deliveryRuleKey = X_C_Order.DELIVERYRULE_Availability;
		}
		String deliveryRule = MRefList.getListName(
			Env.getCtx(),
			X_C_Order.DELIVERYRULE_AD_Reference_ID,
			deliveryRuleKey
		);

		boolean isStocked = BooleanManager.getBooleanFromString(
			resultSet.getString("IsStocked")
		);

		if (!isStocked) {
			qtyOnHand = qty;
		}

		//	Get Precision
		// int precision = MUOM.getPrecision(Env.getCtx(), productUOMId);
		// 	Valid Quantity On Hand
		// if(!deliveryRuleKey.equals(X_C_Order.DELIVERYRULE_Force) && !deliveryRuleKey.equals(X_C_Order.DELIVERYRULE_Manual)) {
		// 	//FR [ 1 ]
		// 	BigDecimal diff = ((BigDecimal) (isStocked ? Env.ONE : Env.ZERO))
		// 		.multiply(
		// 			qtyOnHand
		// 				.subtract(qty)
		// 				.setScale(precision, RoundingMode.HALF_UP)
		// 		);
		// 	//	Set Quantity
		// 	if(diff.doubleValue() < 0) {
		// 		qty = qty
		// 			.subtract(diff.abs())
		// 			.setScale(precision, RoundingMode.HALF_UP)
		// 		;
		// 	}
		// 	//	Valid Zero
		// 	if (qty.compareTo(Env.ZERO) <= 0) {
		// 		// Omit this record
		// 		// continue;
		// 		return builder;
		// 	}
		// }

		builder.setId(
				resultSet.getInt("ID")
			)
			.setUuid(
				StringManager.getValidString(
					resultSet.getString(
						I_AD_Element.COLUMNNAME_UUID
					)
				)
			)
			.setHeaderId(
				resultSet.getInt("Parent_ID")
			)
			.setDocumentNo(
				StringManager.getValidString(
					resultSet.getString(
						I_C_Order.COLUMNNAME_DocumentNo
					)
				)
			)
			.setWarehouseId(
				resultSet.getInt(
					I_C_Order.COLUMNNAME_M_Warehouse_ID
				)
			)
			.setWarehouse(
				StringManager.getValidString(
					resultSet.getString("Warehouse")
				)
			)
			.setProductId(
				resultSet.getInt(
					I_C_OrderLine.COLUMNNAME_M_Product_ID
				)
			)
			.setProductValue(
				StringManager.getStringFromObject(
					resultSet.getString("ProductValue")
				)
			)
			.setProduct(
				StringManager.getStringFromObject(
					resultSet.getString("Product")
				)
			)
			.setUomId(
				productUOMId
			)
			.setUom(
				StringManager.getValidString(
					resultSet.getString("UOMSymbol")
				)
			)
			.setOrderUomId(
				resultSet.getInt("Order_UOM_ID")
			)
			.setOrderUom(
				StringManager.getValidString(
					resultSet.getString("Order_UOMSymbol")
				)
			)
			.setOnHandQuantity(
				NumberManager.getBigDecimalToString(
					qtyOnHand
				)
			)
			.setQuantity(
				NumberManager.getBigDecimalToString(
					qty
				)
			)
			.setVolume(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal(
						I_M_Product.COLUMNNAME_Volume
					)
				)
			)
			.setWeight(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal(
						I_M_Product.COLUMNNAME_Weight
					)
				)
			)
			.setOrderedQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal(
						I_C_OrderLine.COLUMNNAME_QtyOrdered
					)
				)
			)
			.setReservedQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal(
						I_C_OrderLine.COLUMNNAME_QtyReserved
					)
				)
			)
			.setQuantityInvoiced(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal(
						I_C_OrderLine.COLUMNNAME_QtyInvoiced
					)
				)
			)
			.setDeliveredQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal(
						I_C_OrderLine.COLUMNNAME_QtyDelivered
					)
				)
			)
			.setQuantityInTransit(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("QtyLoc")
				)
			)
			.setDeliveryRuleValue(
				deliveryRuleKey
			)
			.setDeliveryRule(
				StringManager.getValidString(
					deliveryRule
				)
			)
			.setIsStocked(
				isStocked
			)
		;
		return builder;
	}

}
