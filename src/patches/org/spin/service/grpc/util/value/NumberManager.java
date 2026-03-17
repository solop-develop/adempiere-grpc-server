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

package org.spin.service.grpc.util.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;

import org.compiere.model.MCurrency;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for handle Number (BigDecimal, Integer, Double, Float) values
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class NumberManager {


	/**
	 * Validate if is numeric
	 * @param value
	 * @return
	 */
	public static boolean isNumeric(String value) {
		if(Util.isEmpty(value, true)) {
			return false;
		}
		//	
		return value.matches("[+-]?\\d*(\\.\\d+)?");
	}


	/**
	 * Is BigDecimal
	 * @param value
	 * @return
	 */
	public static boolean isBigDecimal(String value) {
		return getBigDecimalFromString(value) != null;
	}


	public static BigDecimal getBigDecimalFromObject(Object value) {
		BigDecimal numberValue = null;
		if (value == null) {
			return numberValue;
		}

		if (value instanceof String) {
			numberValue = getBigDecimalFromString(
				(String) value
			);
		} else if (value instanceof Integer) {
			Integer intValue = (Integer) value;
			numberValue = BigDecimal.valueOf(intValue);
		} else if (value instanceof Double) {
			numberValue = getBigDecimalFromDouble(
				(Double) value
			);
		} else if (value instanceof Float) {
			numberValue = getBigDecimalFromFloat(
				(Float) value
			);
		} else if (value instanceof Long) {
			long longValue = (long) value;
			numberValue = BigDecimal.valueOf(longValue);
		} else if (value instanceof Value) {
			numberValue = NumberManager.getBigDecimalFromProtoValue(
				(Value) value
			);
		} else if (value instanceof Value.Builder) {
			Value newValue = ((Value.Builder) value).build();
			numberValue = NumberManager.getBigDecimalFromProtoValue(
				newValue
			);
		} else {
			numberValue = (BigDecimal) value;
		}

		if (numberValue != null && numberValue.scale() <= 0) {
			numberValue = numberValue.setScale(2);
		}

		return numberValue;
	}


	/**
	 * Get BigDecimal number from String
	 * @param doubleValue
	 * @return
	 */
	public static BigDecimal getBigDecimalFromDouble(Double doubleValue) {
		BigDecimal numberValue = null;
		if (doubleValue == null) {
			return numberValue;
		}
		//	
		try {
			numberValue = BigDecimal.valueOf(doubleValue);
		} catch (Exception e) {
			
		}
		return numberValue;
	}

	/**
	 * Get BigDecimal number from Float
	 * @param floatValue
	 * @return
	 */
	public static BigDecimal getBigDecimalFromFloat(Float floatValue) {
		BigDecimal numberValue = null;
		if (floatValue == null) {
			return numberValue;
		}
		//	
		try {
			numberValue = BigDecimal.valueOf(floatValue);
		} catch (Exception e) {
			
		}
		return numberValue;
	}

	/**
	 * Get BigDecimal number from String
	 * @param stringValue
	 * @return
	 */
	public static BigDecimal getBigDecimalFromString(String stringValue) {
		BigDecimal numberValue = null;
		if (Util.isEmpty(stringValue, true)) {
			return numberValue;
		}
		//	
		try {
			numberValue = new BigDecimal(stringValue);
		} catch (Exception e) {
			
		}
		return numberValue;
	}

	/**
	 * Convert BigDecimal to String value
	 * @param bigDecimalValue
	 * @return
	 */
	public static String getBigDecimalToString(BigDecimal bigDecimalValue) {
		String stringValue = "";
		if (bigDecimalValue == null) {
			return stringValue;
		}
		return bigDecimalValue.toPlainString();
	}



	/**
	 * TODO: Validate null with int as Object.intValue()
	 * @param value
	 * @return
	 */
	public static Integer getIntegerFromObject(Object value) {
		Integer integerValue = null;
		if (value == null) {
			return integerValue;
		}
		if (value instanceof Integer) {
			integerValue = (Integer) value;
		} else if (value instanceof Long) {
			integerValue = getIntegerFromLong(
				(Long) value
			);
		} else if(value instanceof Float) {
			integerValue = getIntegerFromFloat(
				(Float) value
			);
		} else if(value instanceof Double) {
			integerValue = getIntegerFromDouble(
				(Double) value
			);
		} else if(value instanceof BigDecimal) {
			integerValue = getIntegerFromBigDecimal(
				(BigDecimal) value
			);
		} else if (value instanceof String) {
			integerValue = getIntegerFromString(
				(String) value
			);
		} else if (value instanceof Value) {
			integerValue = NumberManager.getIntegerFromProtoValue(
				(Value) value
			);
		} else if (value instanceof Value.Builder) {
			Value newValue = ((Value.Builder) value).build();
			integerValue = NumberManager.getIntegerFromProtoValue(
				newValue
			);
		}
		return integerValue;
	}

	public static Integer getIntegerFromString(String stringValue) {
		Integer integerValue = null;
		if (Util.isEmpty(stringValue, true)) {
			return integerValue;
		}
		try {
			integerValue = Integer.valueOf(
				stringValue
			);
		} catch (Exception e) {
			integerValue = null;
		}
		return integerValue;
	}


	public static Integer getIntegerFromLong(Long longValue) {
		Integer numberValue = null;
		if (longValue == null) {
			return numberValue;
		}
		numberValue = Math.toIntExact(longValue);
		return numberValue;
	}


	public static Integer getIntegerFromFloat(Float floatValue) {
		Integer numberValue = null;
		if (floatValue == null) {
			return numberValue;
		}
		numberValue = floatValue.intValue();
		return numberValue;
	}


	public static Integer getIntegerFromDouble(Double doubleValue) {
		Integer numberValue = null;
		if (doubleValue == null) {
			return numberValue;
		}
		numberValue = doubleValue.intValue();
		return numberValue;
	}


	public static Integer getIntegerFromBigDecimal(BigDecimal bigDecimalValue) {
		Integer numberValue = null;
		if (bigDecimalValue == null) {
			return numberValue;
		}
		numberValue = bigDecimalValue.intValue();
		return numberValue;
	}


	/**
 	 * @param value
	 * @return
	 */
	public static int getIntFromObject(Object value) {
		int intValue = 0;
		if (value == null) {
			return intValue;
		}
		Integer integerValue = getIntegerFromObject(value);
		if (integerValue != null) {
			intValue = integerValue.intValue();
		}
		return intValue;
	}

	/**
	 * Get Int value from String
	 * @param stringValue
	 * @return
	 */
	public static int getIntFromString(String stringValue) {
		int integerValue = 0;
		if (Util.isEmpty(stringValue, true)) {
			return 0;
		}
		try {
			integerValue = Integer.parseInt(stringValue);
		} catch (Exception e) {
			// log.severe(e.getLocalizedMessage());
		}
		return integerValue;
	}

	/**
	 * Get String value from Int
	 * @param intValue
	 * @return
	 */
	public static String getIntToString(int intValue) {
		return String.valueOf(intValue);
	}

	public static BigDecimal convertFromValueToDecimal(Value value) {
		if(value.hasStringValue()) {
			return NumberManager.getBigDecimalFromString(
				value.getStringValue()
			);
		}
		return null;
	}
	
	public static Value convertFromDecimalToValue(BigDecimal value) {
		if(value != null) {
			return Value.newBuilder().setStringValue(
				getBigDecimalToString(
					value
				)
			).build();
		}
		return Value.newBuilder().build();
	}



	/**
	 * Get integer from a value
	 * @param value
	 * @return
	 */
	public static int getIntegerFromProtoValue(Value value) {
		if (value == null) {
			return 0;
		}
		int intValue = (int) value.getNumberValue();
		if (intValue == 0 && value.hasStringValue()) {
			intValue = NumberManager.getIntFromString(
				value.getStringValue()
			);
		}
		return intValue;
	}

	/**
	 * Get Proto Value from Integer
	 * @param value
	 * @return
	 */
	public static Value.Builder getProtoValueFromInteger(Integer value) {
		if(value == null) {
			return ValueManager.getProtoValueFromNull();
		}
		//	default
		return getProtoValueFromInt(
			value.intValue()
		);
	}
	/**
	 * Get Proto Value alue from Int
	 * @param value
	 * @return
	 */
	public static Value.Builder getProtoValueFromInt(int value) {
		//	default
		return Value.newBuilder().setNumberValue(value);
	}


	/**
	 * Get display value formatted as Amount (DisplayType.Amount)
	 * @param value
	 * @return formatted amount string (e.g. "1,234.56")
	 */
	public static String getAmountDisplayValue(BigDecimal value) {
		return NumberManager.getDisplayValue(value, DisplayType.Amount);
	}
	/**
	 * Get display value formatted as CostPrice (DisplayType.CostPrice)
	 * @param value
	 * @return formatted cost/price string (e.g. "1,234.5600")
	 */
	public static String getCostPriceDisplayValue(BigDecimal value) {
		return NumberManager.getDisplayValue(value, DisplayType.CostPrice);
	}
	/**
	 * Get display value formatted as Quantity (DisplayType.Quantity)
	 * @param value
	 * @return formatted quantity string (e.g. "1,234.56")
	 */
	public static String getQuantityDisplayValue(BigDecimal value) {
		return NumberManager.getDisplayValue(value, DisplayType.Quantity);
	}
	/**
	 * Get display value formatted as Number (DisplayType.Number)
	 * @param value
	 * @return formatted number string (e.g. "1,234.56")
	 */
	public static String getNumberDisplayValue(BigDecimal value) {
		return NumberManager.getDisplayValue(value, DisplayType.Number);
	}
	/**
	 * Get display value formatted as Integer (DisplayType.Integer)
	 * @param value
	 * @return formatted integer string (e.g. "1,234")
	 */
	public static String getIntegerDisplayValue(BigDecimal value) {
		return NumberManager.getDisplayValue(value, DisplayType.Integer);
	}

	/**
	 * Get display value formatted as Amount with currency symbol
	 * @param value
	 * @param currencyId C_Currency_ID
	 * @return formatted amount with currency (e.g. "$ 1,234.56")
	 */
	public static String getAmountDisplayValueWithCurrency(BigDecimal value, int currencyId) {
		return NumberManager.getDisplayValueWithCurrency(value, DisplayType.Amount, currencyId);
	}
	/**
	 * Get display value formatted as CostPrice with currency symbol
	 * @param value
	 * @param currencyId C_Currency_ID
	 * @return formatted cost/price with currency (e.g. "$ 1,234.5600")
	 */
	public static String getCostPriceDisplayValueWithCurrency(BigDecimal value, int currencyId) {
		return NumberManager.getDisplayValueWithCurrency(value, DisplayType.CostPrice, currencyId);
	}

	/**
	 * Get formatted display value from BigDecimal based on DisplayType
	 * @param value
	 * @param displayTypeId reference display type (Amount, CostPrice, Quantity, Number, Integer)
	 * @return formatted string value
	 */
	public static String getDisplayValue(BigDecimal value, int displayTypeId) {
		return NumberManager.getDisplayValue(value, displayTypeId, null);
	}
	/**
	 * Get formatted display value from BigDecimal based on DisplayType
	 * @param value
	 * @param displayTypeId reference display type (Amount, CostPrice, Quantity, Number, Integer)
	 * @return formatted string value
	 */
	public static String getDisplayValue(Object value, int displayTypeId) {
		BigDecimal number = NumberManager.getBigDecimalFromObject(value);
		return NumberManager.getDisplayValue(number, displayTypeId, null);
	}
	/**
	 * Get formatted display value from BigDecimal based on DisplayType and format pattern
	 * @param value
	 * @param displayTypeId reference display type (Amount, CostPrice, Quantity, Number, Integer)
	 * @param formatPattern optional format pattern
	 * @return formatted string value
	 */
	public static String getDisplayValue(BigDecimal value, int displayTypeId, String formatPattern) {
		if (value == null) {
			return "";
		}
		if (!DisplayType.isNumeric(displayTypeId)) {
			return NumberManager.getBigDecimalToString(value);
		}
		Language language = Language.getLoginLanguage();
		DecimalFormat numberFormat = DisplayType.getNumberFormat(
			displayTypeId,
			language,
			formatPattern
		);
		return numberFormat.format(value);
	}


	/**
	 * Get formatted display value from BigDecimal with currency symbol
	 * @param value
	 * @param displayTypeId reference display type (Amount, CostPrice, Quantity, Number, Integer)
	 * @param currencyId C_Currency_ID
	 * @return formatted string value with currency symbol
	 */
	public static String getDisplayValueWithCurrencyFromObject(Object value, int displayTypeId, int currencyId) {
		BigDecimal number = NumberManager.getBigDecimalFromObject(value);
		return NumberManager.getDisplayValueWithCurrency(number, displayTypeId, currencyId, null);
	}
	/**
	 * Get formatted display value from BigDecimal with currency symbol
	 * @param value
	 * @param displayTypeId reference display type (Amount, CostPrice, Quantity, Number, Integer)
	 * @param currencyId C_Currency_ID
	 * @return formatted string value with currency symbol
	 */
	public static String getDisplayValueWithCurrency(BigDecimal value, int displayTypeId, int currencyId) {
		return NumberManager.getDisplayValueWithCurrency(value, displayTypeId, currencyId, null);
	}
	/**
	 * Get formatted display value from BigDecimal with currency symbol
	 * @param value
	 * @param displayTypeId reference display type (Amount, CostPrice, Quantity, Number, Integer)
	 * @param currencyIso C_Currency_ID
	 * @return formatted string value with currency symbol
	 */
	public static String getDisplayValueWithCurrency(BigDecimal value, int displayTypeId, String currencyIso) {
		int currencyId = 0;
		if (!Util.isEmpty(currencyIso, true)) {
			MCurrency currency = MCurrency.get(Env.getCtx(), currencyIso);
			if (currency != null && currency.getC_Currency_ID() > 0) {
				currencyId = currency.getC_Currency_ID();
			}
		}
		return NumberManager.getDisplayValueWithCurrency(value, displayTypeId, currencyId, null);
	}
	/**
	 * Get formatted display value from BigDecimal with currency symbol and format pattern
	 * @param value
	 * @param displayTypeId reference display type (Amount, CostPrice, Quantity, Number, Integer)
	 * @param currencyId C_Currency_ID
	 * @param formatPattern optional format pattern
	 * @return formatted string value with currency symbol
	 */
	public static String getDisplayValueWithCurrency(BigDecimal value, int displayTypeId, int currencyId, String formatPattern) {
		if (value == null) {
			return "";
		}
		BigDecimal formattedValue = value;
		String currencySymbol = "";
		if (currencyId > 0) {
			MCurrency currency = MCurrency.get(Env.getCtx(), currencyId);
			if (currency != null && currency.getC_Currency_ID() > 0) {
				int precision = currency.getStdPrecision();
				formattedValue = value.setScale(precision, RoundingMode.HALF_UP);
				String symbol = currency.getCurSymbol();
				if (!Util.isEmpty(symbol, true)) {
					currencySymbol = symbol + " ";
				}
			}
		}
		String displayValue = NumberManager.getDisplayValue(formattedValue, displayTypeId, formatPattern);
		return currencySymbol + displayValue;
	}


	/**
	 * Validate if is a decimal value
	 * @param value
	 * @return
	 */
	public static boolean isDecimalProtoValue(Value value) {
		if (value == null) {
			return false;
		}
		Map<String, Value> values = value.getStructValue().getFieldsMap();
		if(values == null) {
			return false;
		}
		Value type = values.get(ValueManager.TYPE_KEY);
		if(type == null) {
			return false;
		}
		String validType = TextManager.getValidString(
			type.getStringValue()
		);
		return validType.equals(ValueManager.TYPE_DECIMAL);
	}

	/**
	 * Get BigDecimal from Value object
	 * @param decimalValue
	 * @return BigDecimal
	 */
	public static BigDecimal getBigDecimalFromProtoValue(Value decimalValue) {
		if(decimalValue == null
				|| decimalValue.hasNullValue()
				|| !(decimalValue.hasStringValue() || decimalValue.hasNumberValue() || decimalValue.hasStructValue())) {
			return null;
		}

		if (decimalValue.hasStructValue()) {
			Map<String, Value> values = decimalValue.getStructValue().getFieldsMap();
			if(values != null && !values.isEmpty()) {
				Value type = values.get(ValueManager.TYPE_KEY);
				if (type != null && ValueManager.TYPE_DECIMAL.equals(type.getStringValue())) {
					Value value = values.get(ValueManager.VALUE_KEY);
					if (value != null) {
						if (!Util.isEmpty(value.getStringValue(), false)) {
							return NumberManager.getBigDecimalFromString(
								value.getStringValue()
							);
						}
						if (value.hasNumberValue()) {
							return NumberManager.getBigDecimalFromDouble(
								value.getNumberValue()
							);
						}
					}
				}
			}
		}
		if (!Util.isEmpty(decimalValue.getStringValue(), false)) {
			return NumberManager.getBigDecimalFromString(
				decimalValue.getStringValue()
			);
		}
		if (decimalValue.hasNumberValue()) {
			return NumberManager.getBigDecimalFromDouble(
				decimalValue.getNumberValue()
			);
		}
		return null;
	}

	/**
	 * Get Value object from BigDecimal
	 * @param value
	 * @return Value.Builder
	 */
	public static Value.Builder getProtoValueFromBigDecimal(BigDecimal value) {
		Struct.Builder decimalValue = Struct.newBuilder();
		decimalValue.putFields(
			ValueManager.TYPE_KEY,
			Value.newBuilder().setStringValue(ValueManager.TYPE_DECIMAL).build()
		);

		Value.Builder valueBuilder = Value.newBuilder();
		if (value == null) {
			valueBuilder = ValueManager.getProtoValueFromNull();
		} else {
			String valueString = NumberManager.getBigDecimalToString(
				value
			);
			valueBuilder.setStringValue(
				valueString
			);
		}

		decimalValue.putFields(
			ValueManager.VALUE_KEY,
			valueBuilder.build()
		);
		return Value.newBuilder().setStructValue(decimalValue);
	}

}
