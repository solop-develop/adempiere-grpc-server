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
package org.spin.base.interim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_OrderLine;
import org.adempiere.core.domains.models.I_C_Payment;
import org.compiere.model.MDocType;
import org.compiere.model.MProductPricing;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class ContextTemporaryWorkaround {

	/**
	 * Set additonal Env.getCtx() used by callouts
	 * TODO: Remove this method on future
	 * @param calloutClass
	 * @param windowNo
	 * @param calloutBuilder
	 * @return
	 */
	public static org.spin.backend.grpc.user_interface.Callout.Builder setAdditionalContext(String callouts, int windowNo,
		org.spin.backend.grpc.user_interface.Callout.Builder calloutBuilder) {
		if (Util.isEmpty(callouts, true)) {
			return calloutBuilder;
		}

		// separate `org.package.CalloutX.firstMethod; org.any.CalloutY.secondMethod`
		List<String> calloutsList = Arrays.asList(callouts.split("\\s*(,|;)\\s*"));
		if (calloutsList == null || calloutsList.isEmpty()) {
			return calloutBuilder;
		}

		// callouts that modify the context in a customized way
		List<String> calloutsListCustomContext = Arrays.asList(
			org.compiere.model.CalloutOrder.class.getName(),
			org.compiere.model.CalloutPayment.class.getName()
		);

		calloutsList.forEach(calloutClassAndMethod -> {
			if (Util.isEmpty(calloutClassAndMethod, true)) {
				// empty class name
				return;
			}
			// the current callout is not in the custom context list
			boolean isCustomContext = calloutsListCustomContext.stream()
				.anyMatch(calloutClassNameCustomContext -> {
					return calloutClassAndMethod.startsWith(calloutClassNameCustomContext);
				});
			if (isCustomContext) {
				return;
			}

			Struct.Builder contextValues = calloutBuilder.getValuesBuilder();
			if (calloutClassAndMethod.equals("org.compiere.model.CalloutOrder.docType")) {
				// - OrderType
				String docSubTypeSO = Env.getContext(Env.getCtx(), windowNo, "OrderType");
				contextValues.putFields(
					"OrderType",
					ValueManager.getValueFromString(docSubTypeSO).build()
				);

				// - HasCharges
				String hasCharges = Env.getContext(Env.getCtx(), windowNo, "HasCharges");
				contextValues.putFields(
					"HasCharges",
					ValueManager.getValueFromStringBoolean(hasCharges).build()
				);
			}
			else if (calloutClassAndMethod.equals("org.compiere.model.CalloutOrder.priceList")) {
				// - M_PriceList_Version_ID
				int priceListVersionId = Env.getContextAsInt(Env.getCtx(), windowNo, "M_PriceList_Version_ID");
				contextValues.putFields(
					"M_PriceList_Version_ID",
					ValueManager.getValueFromInteger(priceListVersionId).build()
				);
			}
			else if (calloutClassAndMethod.equals("org.compiere.model.CalloutOrder.product")) {
				// - M_PriceList_Version_ID
				int priceListVersionId = Env.getContextAsInt(Env.getCtx(), windowNo, "M_PriceList_Version_ID");
				contextValues.putFields(
					"M_PriceList_Version_ID",
					ValueManager.getValueFromInteger(priceListVersionId).build()
				);

				// - DiscountSchema
				String isDiscountSchema = Env.getContext(Env.getCtx(), windowNo, "DiscountSchema");
				contextValues.putFields(
					"DiscountSchema",
					ValueManager.getValueFromStringBoolean(isDiscountSchema).build()
				);
			}
			else if (calloutClassAndMethod.equals("org.compiere.model.CalloutOrder.charge")) {
				// - DiscountSchema
				String isDiscountSchema = Env.getContext(Env.getCtx(), windowNo, "DiscountSchema");
				contextValues.putFields(
					"DiscountSchema",
					ValueManager.getValueFromStringBoolean(isDiscountSchema).build()
				);
			}
			else if (calloutClassAndMethod.equals("org.compiere.model.CalloutOrder.amt")) {
				// - DiscountSchema
				String isDiscountSchema = Env.getContext(Env.getCtx(), windowNo, "DiscountSchema");
				contextValues.putFields(
					"DiscountSchema",
					ValueManager.getValueFromStringBoolean(isDiscountSchema).build()
				);
			}
			else if (calloutClassAndMethod.equals("org.compiere.model.CalloutOrder.qty")) {
				// - UOMConversion
				String isConversion = Env.getContext(Env.getCtx(), windowNo, "UOMConversion");
				contextValues.putFields(
					"UOMConversion",
					ValueManager.getValueFromStringBoolean(isConversion).build()
				);
			} else if (calloutClassAndMethod.equals("org.compiere.model.CalloutPayment.docType")) {
				String isSalesTransaction = Env.getContext(Env.getCtx(), windowNo, "IsSOTrx");
				contextValues.putFields(
					"IsSOTrx",
					ValueManager.getValueFromStringBoolean(isSalesTransaction).build()
				);
			}
			calloutBuilder.setValues(contextValues);
		});

		return calloutBuilder;
	}



	/**
	 * Add context as additional columns
	 * @param tableName
	 * @param recordRow
	 * @return
	 */
	public static Struct.Builder setContextAsUnknowColumn(String tableName, Struct.Builder recordRow) {
		if (Util.isEmpty(tableName, true)) {
			return recordRow;
		}
		if (I_C_Payment.Table_Name.equals(tableName)) {
			// AS `org.compiere.model.CalloutPayment.docType` to `IsSOTrx`
			int documentTypeId = ValueManager.getIntegerFromValue(
				recordRow.getFieldsMap().get(I_C_Payment.COLUMNNAME_C_DocType_ID)
			);
			if (documentTypeId > 0) {
				MDocType documentType = MDocType.get(Env.getCtx(), documentTypeId);
				boolean isSalesTransaction = documentType.isSOTrx();

				Value.Builder valueBuilder = ValueManager.getValueFromBoolean(
					isSalesTransaction
				);
				recordRow.putFields(
					"IsSOTrx",
					valueBuilder.build()
				);
			}
		} else if (I_C_Order.Table_Name.equals(tableName)) {
			// AS `org.compiere.model.CalloutOrder.docType` to `OrderType`
			int documentTypeId = ValueManager.getIntegerFromValue(
				recordRow.getFieldsMap().get(I_C_Payment.COLUMNNAME_C_DocType_ID)
			);
			if (documentTypeId > 0) {
				MDocType documentType = MDocType.get(Env.getCtx(), documentTypeId);
				Value.Builder subTypeBuilder = ValueManager.getValueFromString(
					documentType.getDocSubTypeSO()
				);
				recordRow.putFields(
					"OrderType",
					subTypeBuilder.build()
				);

				Value.Builder hasChargesBuilder = ValueManager.getValueFromBoolean(
					documentType.isHasCharges()
				);
				recordRow.putFields(
					"HasCharges",
					hasChargesBuilder.build()
				);
			}

			int priceListId = ValueManager.getIntegerFromValue(
				recordRow.getFieldsMap().get(I_C_Order.COLUMNNAME_M_PriceList_ID)
			);
			Timestamp dateOrdered = ValueManager.getTimestampFromValue(
				recordRow.getFieldsMap().get(I_C_Order.COLUMNNAME_DateOrdered)
			);
			int priceListVersionId = DB.getSQLValue(
				null,
				"SELECT M_PriceList_Version_ID FROM M_PriceList_Version WHERE M_PriceList_ID = ? AND ValidFrom <= ? ORDER BY ValidFrom DESC",
				priceListId,
				dateOrdered
			);
			if (priceListVersionId > 0) {
				Value.Builder priceListVersionBuilder = ValueManager.getValueFromInt(
					priceListVersionId
				);
				recordRow.putFields(
					"M_PriceList_Version_ID",
					priceListVersionBuilder.build()
				);
			}
		} else if (I_C_OrderLine.Table_Name.equals(tableName)) {
			// AS `org.compiere.model.CalloutOrder.qty` to `DiscountSchema`
			int chargeId = ValueManager.getIntegerFromValue(
				recordRow.getFieldsMap().get(I_C_OrderLine.COLUMNNAME_C_Charge_ID)
			);
			// AS `org.compiere.model.CalloutOrder.qty` to `UOMConversion`
			int productId = ValueManager.getIntegerFromValue(
				recordRow.getFieldsMap().get(I_C_OrderLine.COLUMNNAME_M_Product_ID)
			);
			if (productId > 0) {
				int orderId = ValueManager.getIntegerFromValue(
					recordRow.getFieldsMap().get(I_C_OrderLine.COLUMNNAME_C_Order_ID)
				);
				int businessPartnerId = ValueManager.getIntegerFromValue(
					recordRow.getFieldsMap().get(I_C_OrderLine.COLUMNNAME_C_BPartner_ID)
				);
				BigDecimal quantityOrdered = ValueManager.getBigDecimalFromValue(
					recordRow.getFieldsMap().get(I_C_OrderLine.COLUMNNAME_QtyOrdered)
				);
				
				// boolean isSalesTransaction = ValueManager.getBooleanFromValue(
				// 	recordRow.getFieldsMap.get(I_C_OrderLine.)
				// )
				String isSalesTransaction = DB.getSQLValueString(
					null,
					"SELECT IsSOTrx FROM C_Order WHERE C_Order_ID = ?",
					orderId
				);
				boolean isSOTrx = BooleanManager.getBooleanFromString(isSalesTransaction);
				MProductPricing pp = new MProductPricing(productId, businessPartnerId, quantityOrdered, isSOTrx, null);
				boolean isDiscountSchema = pp.isDiscountSchema();

				Value.Builder valueBuilder = ValueManager.getValueFromBoolean(
					isDiscountSchema
				);
				recordRow.putFields(
					"DiscountSchema",
					valueBuilder.build()
				);

				BigDecimal quantityEntered = ValueManager.getBigDecimalFromValue(
					recordRow.getFieldsMap().get(I_C_OrderLine.COLUMNNAME_QtyEntered)
				);
				int unitOfMeasureId = ValueManager.getIntegerFromValue(
					recordRow.getFieldsMap().get(I_C_OrderLine.COLUMNNAME_C_UOM_ID)
				);
				BigDecimal quantityEntered1 = quantityEntered.setScale(
					MUOM.getPrecision(Env.getCtx(), unitOfMeasureId), RoundingMode.HALF_UP
				);
				if (quantityEntered.compareTo(quantityEntered1) != 0) {
					quantityEntered = quantityEntered1;
				}
				quantityOrdered = MUOMConversion.convertProductFrom(Env.getCtx(), productId, unitOfMeasureId, quantityEntered);
				if (quantityOrdered == null) {
					quantityOrdered = quantityEntered;
				}

				boolean isConversion = quantityEntered.compareTo(quantityOrdered) != 0;
				Value.Builder uomConversionBuilder = ValueManager.getValueFromBoolean(
					isConversion
				);
				recordRow.putFields(
					"UOMConversion",
					uomConversionBuilder.build()
				);
			} else if (chargeId > 0) {
				Value.Builder valueBuilder = ValueManager.getValueFromBoolean(
					false
				);
				recordRow.putFields(
					"DiscountSchema",
					valueBuilder.build()
				);
			}

		}

		return recordRow;
	}

}
