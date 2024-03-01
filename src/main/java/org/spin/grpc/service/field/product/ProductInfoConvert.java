/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
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
package org.spin.grpc.service.field.product;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.adempiere.core.domains.models.I_M_AttributeSet;
import org.adempiere.core.domains.models.I_M_Product;
import org.spin.backend.grpc.field.product.ProductInfo;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Product Info field
 */
public class ProductInfoConvert {
	
	public static ProductInfo.Builder convertProductInfo(ResultSet rs, int priceListVersionId, int warhouseId, boolean isUnconfirmed) throws SQLException {
		ProductInfo.Builder builder = ProductInfo.newBuilder();
		if (rs == null) {
			return builder;
		}

		builder.setId(
				rs.getInt(
					I_M_Product.COLUMNNAME_M_Product_ID
				)
			)
			.setUuid(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_UUID
					)
				)
			)
			.setValue(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_Value
					)
				)
			)
			.setIsStocked(
				rs.getBoolean(
					I_M_Product.COLUMNNAME_IsStocked
				)
			)
			.setName(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_Name
					)
				)
			)
			.setUpc(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_UPC
					)
				)
			)
			.setSku(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_SKU
					)
				)
			)
			.setUom(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_C_UOM_ID
					)
				)
			)
			.setProductCategory(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_M_Product_Category_ID
					)
				)
			)
			.setProductClass(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_M_Product_Class_ID
					)
				)
			)
			.setProductGroup(
				ValueManager.validateNull(
					rs.getString(
						I_M_Product.COLUMNNAME_M_Product_Group_ID
					)
				)
			)
			.setIsInstanceAttribute(
				rs.getBoolean(
					I_M_AttributeSet.COLUMNNAME_IsInstanceAttribute
				)
			)
			.setVendor(
				ValueManager.validateNull(
					rs.getString(
						"Vendor"
					)
				)
			)
			.setIsActive(
				rs.getBoolean(
					I_M_Product.COLUMNNAME_IsActive
				)
			)
		;

		if (warhouseId > 0) {
			builder.setAvailableQuantity(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"QtyAvailable"
						)
					)
				)
				.setOnHandQuantity(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"QtyOnHand"
						)
					)
				)
				.setReservedQuantity(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"QtyReserved"
						)
					)
				)
				.setOrderedQuantity(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"QtyOrdered"
						)
					)
				)
			;
			if (isUnconfirmed) {
				builder.setUnconfirmedQuantity(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal(
								"QtyUnconfirmed"
							)
						)
					)
					.setUnconfirmedMoveQuantity(
						NumberManager.getBigDecimalToString(
							rs.getBigDecimal(
								"QtyUnconfirmedMove"
							)
						)
					)
				;
			}
		}
		if (priceListVersionId > 0) {
			builder.setListPrice(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"PriceList"
						)
					)
				)
				.setStandardPrice(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"PriceStd"
						)
					)
				)
				.setLimitPrice(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"PriceLimit"
						)
					)
				)
				.setMargin(
					NumberManager.getBigDecimalToString(
						rs.getBigDecimal(
							"Margin"
						)
					)
				)
			;
		}
		return builder;
	}
}
