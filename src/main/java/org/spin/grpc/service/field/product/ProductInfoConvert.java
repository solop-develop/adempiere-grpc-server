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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_M_AttributeSet;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_ProductPrice;
import org.adempiere.core.domains.models.I_M_Storage;
import org.adempiere.core.domains.models.I_M_Substitute;
import org.adempiere.core.domains.models.I_M_Warehouse;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPO;
import org.compiere.model.MUOM;
import org.compiere.util.Env;
import org.spin.backend.grpc.field.product.AvailableToPromise;
import org.spin.backend.grpc.field.product.ProductInfo;
import org.spin.backend.grpc.field.product.RelatedProduct;
import org.spin.backend.grpc.field.product.SubstituteProduct;
import org.spin.backend.grpc.field.product.VendorPurchase;
import org.spin.backend.grpc.field.product.WarehouseStock;
import org.spin.service.grpc.util.base.RecordUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

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

		int productId = rs.getInt(
			I_M_Product.COLUMNNAME_M_Product_ID
		);
		MProduct product = MProduct.get(Env.getCtx(), productId);
		if (product == null || product.getM_Product_ID() <= 0) {
			return builder;
		}

		builder.setId(
				productId
			)
			.setUuid(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_UUID
					)
				)
			)
			.setDisplayValue(
				TextManager.getValidString(
					product.getDisplayValue()
				)
			)
			.setValue(
				TextManager.getValidString(
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
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_Name
					)
				)
			)
			.setUpc(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_UPC
					)
				)
			)
			.setSku(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_SKU
					)
				)
			)
			.setUom(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_C_UOM_ID
					)
				)
			)
			.setProductCategory(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_M_Product_Category_ID
					)
				)
			)
			.setProductClass(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_M_Product_Class_ID
					)
				)
			)
			.setProductClassification(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_M_Product_Classification_ID
					)
				)
			)
			.setProductGroup(
				TextManager.getValidString(
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
				TextManager.getValidString(
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


	public static WarehouseStock.Builder convertWarehouseStock(ResultSet rs) throws SQLException {
		WarehouseStock.Builder builder = WarehouseStock.newBuilder();
		if (rs == null) {
			return builder;
		}
		int id = rs.getInt(
			I_M_Warehouse.COLUMNNAME_M_Warehouse_ID
		);

		builder.setId(id)
			.setUuid(
				TextManager.getValidString(
					RecordUtil.getUuidFromId(
						I_M_Warehouse.Table_Name,
						id
					)
				)
			)
			.setName(
				TextManager.getValidString(
					rs.getString("WarehouseName")
				)
			)
			.setAvailableQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						"QtyAvailable"
					)
				)
			)
			.setOnHandQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_Storage.COLUMNNAME_QtyOnHand
					)
				)
			)
			.setReservedQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_Storage.COLUMNNAME_QtyReserved
					)
				)
			)
			.setOrderedQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_Storage.COLUMNNAME_QtyOrdered
					)
				)
			)
		;

		return builder;
	}


	public static SubstituteProduct.Builder convertSubstituteProduct(ResultSet rs) throws SQLException {
		SubstituteProduct.Builder builder = SubstituteProduct.newBuilder();
		if (rs == null) {
			return builder;
		}

		builder.setId(
				rs.getInt(
					I_M_Substitute.COLUMNNAME_Substitute_ID
				)
			)
			.setValue(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_Value
					)
				)
			)
			.setName(
				TextManager.getValidString(
					rs.getString(
						I_M_Substitute.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				TextManager.getValidString(
					rs.getString(
						I_M_Substitute.COLUMNNAME_Description
					)
				)
			)
			.setWarehouse(
				TextManager.getValidString(
					rs.getString("Warehouse")
				)
			)
			.setAvailableQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						"QtyAvailable"
					)
				)
			)
			.setOnHandQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_Storage.COLUMNNAME_QtyOnHand
					)
				)
			)
			.setReservedQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_Storage.COLUMNNAME_QtyReserved
					)
				)
			)
			.setStandardPrice(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_ProductPrice.COLUMNNAME_PriceStd
					)
				)
			)
		;

		return builder;
	}


	public static RelatedProduct.Builder convertRelatedProduct(ResultSet rs) throws SQLException {
		RelatedProduct.Builder builder = RelatedProduct.newBuilder();
		if (rs == null) {
			return builder;
		}

		builder.setId(
				rs.getInt(
					I_M_Substitute.COLUMNNAME_Substitute_ID
				)
			)
			.setValue(
				TextManager.getValidString(
					rs.getString(
						I_M_Product.COLUMNNAME_Value
					)
				)
			)
			.setName(
				TextManager.getValidString(
					rs.getString(
						I_M_Substitute.COLUMNNAME_Name
					)
				)
			)
			.setDescription(
				TextManager.getValidString(
					rs.getString(
						I_M_Substitute.COLUMNNAME_Description
					)
				)
			)
			.setWarehouse(
				TextManager.getValidString(
					rs.getString("Warehouse")
				)
			)
			.setAvailableQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						"QtyAvailable"
					)
				)
			)
			.setOnHandQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_Storage.COLUMNNAME_QtyOnHand
					)
				)
			)
			.setReservedQuantity(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_Storage.COLUMNNAME_QtyReserved
					)
				)
			)
			.setStandardPrice(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_M_ProductPrice.COLUMNNAME_PriceStd
					)
				)
			)
		;

		return builder;
	}


	public static AvailableToPromise.Builder convertAvaliableToPromise(ResultSet rs) throws SQLException {
		AvailableToPromise.Builder builder = AvailableToPromise.newBuilder();
		if (rs == null) {
			return builder;
		}

		// AvailQty
		BigDecimal onHandQuantity = Optional.ofNullable(
			rs.getBigDecimal(6)
		).orElse(BigDecimal.ZERO);

		// // DeltaQty
		// BigDecimal deliveredQuantity = Optional.ofNullable(
		// 	rs.getBigDecimal(7)
		// ).orElse(BigDecimal.ZERO);

		// QtyOrdered
		BigDecimal orderedQuantity = Optional.ofNullable(
			rs.getBigDecimal(8)
		).orElse(BigDecimal.ZERO);

		// QtyReserved
		BigDecimal reservedQuantity = Optional.ofNullable(
			rs.getBigDecimal(9)
		).orElse(BigDecimal.ZERO);

		BigDecimal availableQuantiy = onHandQuantity.subtract(reservedQuantity);

		BigDecimal expectedQuantity = onHandQuantity.add(orderedQuantity)
			.subtract(reservedQuantity)
		;

		builder.setId(
				rs.getInt(
					I_M_Product.COLUMNNAME_M_Product_ID
				)
			)
			.setName(
				TextManager.getValidString(
					rs.getString("Warehouse")
				)
			)
			.setDocumentNo(
				TextManager.getValidString(
					rs.getString(
						I_C_Order.COLUMNNAME_DocumentNo
					)
				)
			)
			.setLocator(
				TextManager.getValidString(
					rs.getString("Locator")
				)
			)
			.setDate(
				TimeManager.getProtoTimestampFromTimestamp(
					rs.getTimestamp(5)
				)
			)
			.setAvailableQuantity(
				NumberManager.getBigDecimalToString(
					availableQuantiy
				)
			)
			.setOnHandQuantity(
				NumberManager.getBigDecimalToString(
					onHandQuantity
				)
			)
			.setReservedQuantity(
				NumberManager.getBigDecimalToString(
					reservedQuantity
				)
			)
			.setOrderedQuantity(
				NumberManager.getBigDecimalToString(
					orderedQuantity
				)
			)
			.setAvailableToPromiseQuantity(
				NumberManager.getBigDecimalToString(
					expectedQuantity
				)
			)
			.setBusinessPartner(
				TextManager.getValidString(
					rs.getString("BP_Name")
				)
			)
			.setAttributeSetInstance(
				NumberManager.getIntToString(
					rs.getInt("ASI")
				)
			)
		;

		return builder;
	}


	public static VendorPurchase.Builder convertVendorPurchase(MProductPO productVendor) {
		VendorPurchase.Builder builder = VendorPurchase.newBuilder();
		if (productVendor == null) {
			return builder;
		}

		MBPartner businesPartner = MBPartner.get(Env.getCtx(), productVendor.getC_BPartner_ID());
		if (businesPartner != null && businesPartner.getC_BPartner_ID() > 0) {
			builder.setId(
					businesPartner.getC_BPartner_ID()
				)
				.setUuid(
					TextManager.getValidString(
						businesPartner.getUUID()
					)
				)
				.setName(
					TextManager.getValidString(
						businesPartner.getName()
					)
				)
			;
		}

		MUOM unitOfMeasure = MUOM.get(Env.getCtx(), productVendor.getC_UOM_ID());
		if (unitOfMeasure != null && unitOfMeasure.getC_UOM_ID() > 0) {
			builder.setUnitOfMeasure(
				TextManager.getValidString(
					unitOfMeasure.getName()
				)
			);
		}

		String currencyCode = "";
		if (productVendor.getC_Currency_ID() > 0) {
			MCurrency currency = MCurrency.get(Env.getCtx(), productVendor.getC_Currency_ID());
			if (currency != null && currency.getC_Currency_ID() > 0) {
				currencyCode = currency.getISO_Code();
			}
		}

		builder.setIsCurrentVendor(
				productVendor.isCurrentVendor()
			)
			.setCurrency(
				TextManager.getValidString(currencyCode)
			)
			.setListPrice(
				NumberManager.getBigDecimalToString(
					productVendor.getPriceList()
				)
			)
			.setPurchasePrice(
				NumberManager.getBigDecimalToString(
					productVendor.getPricePO()
				)
			)
			.setLastPurchasePrice(
				NumberManager.getBigDecimalToString(
					productVendor.getPriceLastPO()
				)
			)
			.setVendorProductKey(
				TextManager.getValidString(
					productVendor.getVendorProductNo()
				)
			)
			.setMinOrderQuantity(
				NumberManager.getBigDecimalToString(
					productVendor.getOrder_Min()
				)
			)
			.setPromisedDeliveryTime(
				NumberManager.getIntToString(
					productVendor.getDeliveryTime_Promised()
				)
			)
			.setActualDeliveryTime(
				NumberManager.getIntToString(
					productVendor.getDeliveryTime_Actual()
				)
			)
		;

		return builder;
	}

}
