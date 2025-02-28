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

import java.sql.ResultSet;
import java.sql.SQLException;

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
					resultSet.getString("UUID")
				)
			)
			.setDocumentNo(
				StringManager.getValidString(
					resultSet.getString("DocumentNo")
				)
			)
			.setDateOrdered(
				TimeManager.convertDateToValue(
					resultSet.getTimestamp("DateOrdered")
				)
			)
			.setDatePromised(
				TimeManager.convertDateToValue(
					resultSet.getTimestamp("DatePromised")
				)
			)
			.setWarehouse(
				StringManager.getValidString(
					resultSet.getString("Warehouse")
				)
			)
			.setSalesRepresentative(
				StringManager.getValidString(
					resultSet.getString("SalesRep")
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
			.setLocationId(
				resultSet.getInt("C_BPartner_Location_ID")
			)
			.setLocation(
				StringManager.getValidString(
					resultSet.getString("Location")
				)
			)
			.setBusinessPartner(
				StringManager.getValidString(
					resultSet.getString("Partner")
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
		builder.setId(
				resultSet.getInt("ID")
			)
			.setUuid(
				StringManager.getValidString(
					resultSet.getString("UUID")
				)
			)
			.setDocumentNo(
				StringManager.getValidString(
					resultSet.getString("DocumentNo")
				)
			)
			.setWarehouseId(
				resultSet.getInt("M_Warehouse_ID")
			)
			.setWarehouse(
				StringManager.getValidString(
					resultSet.getString("Warehouse")
				)
			)
			.setProductId(
				resultSet.getInt("M_Product_ID")
			)
			.setProduct(
				StringManager.getStringFromObject(
					resultSet.getString("Product")
				)
			)
			.setUomId(
				resultSet.getInt("C_UOM_ID")
			)
			.setUom(
				StringManager.getValidString(
					resultSet.getString("UOMSymbol")
				)
			)
			.setOrderUomId(
				resultSet.getInt("C_UOM_ID")
			)
			.setOrderUom(
				StringManager.getValidString(
					resultSet.getString("UOMSymbol")
				)
			)
			.setOnHandQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("QtyOnHand")
				)
			)
			.setQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("Qty")
				)
			)
			.setVolume(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("Volume")
				)
			)
			.setWeight(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("Weight")
				)
			)
			.setOrderedQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("QtyOrdered")
				)
			)
			.setReservedQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("QtyReserved")
				)
			)
			.setQuantityInvoiced(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("QtyInvoiced")
				)
			)
			.setDeliveredQuantity(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("QtyDelivered")
				)
			)
			.setQuantityInTransit(
				NumberManager.getBigDecimalToString(
					resultSet.getBigDecimal("QtyLoc")
				)
			)
			.setDeliveryRule(
				StringManager.getValidString(
					resultSet.getString("DeliveryRule")
				)
			)
			.setIsStocked(
				BooleanManager.getBooleanFromString(
					resultSet.getString("IsStocked")
				)
			)
		;
		return builder;
	}

}
