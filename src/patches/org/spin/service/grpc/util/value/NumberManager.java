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

import com.google.protobuf.Value;
import org.compiere.util.Util;

import java.math.BigDecimal;

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
}
