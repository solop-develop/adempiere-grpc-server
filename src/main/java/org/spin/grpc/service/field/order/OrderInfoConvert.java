/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Elsio Sanchez elsiosanches@gmail.com                             *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/

package org.spin.grpc.service.field.order;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.adempiere.core.domains.models.I_C_Order;
import org.compiere.model.MOrder;
import org.compiere.util.Env;
import org.spin.backend.grpc.field.order.OrderInfo;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Elsio Sanchez elsiosanches@gmail.com, https://github.com/elsiosanchez
 * Service for backend of Order Info field
 */
public class OrderInfoConvert {

	public static OrderInfo.Builder convertOrderInfo(ResultSet rs) throws SQLException {
		OrderInfo.Builder builder = OrderInfo.newBuilder();
		if (rs == null) {
			return builder;
		}
		int orderId = rs.getInt(
			I_C_Order.COLUMNNAME_C_Order_ID
		);
		MOrder order = new MOrder(Env.getCtx(), orderId, null);

		builder.setId(
				rs.getInt(
					I_C_Order.COLUMNNAME_C_Order_ID
				)
			)
			.setUuid(
				ValueManager.validateNull(
					rs.getString(
						I_C_Order.COLUMNNAME_UUID
					)
				)
			)
			.setDisplayValue(
				ValueManager.validateNull(
					order.getDisplayValue()
				)
			)
			.setBusinessPartner(
				ValueManager.validateNull(
					rs.getString("BusinessPartner")
				)
			)
			.setDateOrdered(
				TimeManager.convertDateToValue(
					rs.getTimestamp(
						I_C_Order.COLUMNNAME_DateOrdered
					)
				)
			)
			.setDocumentNo(
				ValueManager.validateNull(
					rs.getString(
						I_C_Order.COLUMNNAME_DocumentNo
					)
				)
			)
			.setCurrency(
				ValueManager.validateNull(
					rs.getString("Currency")
				)
			)
			.setGrandTotal(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_C_Order.COLUMNNAME_GrandTotal
					)
				)
			)
			.setConvertedAmount(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal("currencyBase")
				)
			)
			.setIsSalesTransaction(
				rs.getBoolean(
					I_C_Order.COLUMNNAME_IsSOTrx
				)
			)
			
			.setIsDelivered(
				rs.getBoolean(
					I_C_Order.COLUMNNAME_IsDelivered
				)
			)
			.setDescription(
				ValueManager.validateNull(
					rs.getString(
						I_C_Order.COLUMNNAME_Description
					)
				)
			)
			.setPoReference(
				ValueManager.validateNull(
					rs.getString(
						I_C_Order.COLUMNNAME_POReference
					)
				)
			)
			.setDocumentStatus(
				ValueManager.validateNull(
					order.getDocStatusName()
				)
			)
		;

		return builder;
	}
}
