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

import java.util.Arrays;
import java.util.List;

import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;

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

		calloutsList.parallelStream().forEach(calloutClassAndMethod -> {
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


}
