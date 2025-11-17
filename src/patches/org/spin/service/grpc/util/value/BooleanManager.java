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

import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.google.protobuf.Value;

/**
 * Class for handle Boolean values
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class BooleanManager {


	/**
	 * Validate if is boolean
	 * @param value
	 * @return
	 */
	public static boolean isBoolean(Object value) {
		if (value == null) {
			return false;
		}
		return isBoolean(
			value.toString()
		);
	}
	/**
	 * Validate if is boolean
	 * @param value
	 * @return
	 */
	public static boolean isBoolean(String value) {
		if (Util.isEmpty(value, true)) {
			return false;
		}
		//	
		return value.equals("Y")
			|| value.equals("N")
			|| value.equals("Yes")
			|| value.equals("No")
			|| value.equals("true")
			|| value.equals("false")
		;
	}


	/**
	 * Validate if is valid data base boolean
	 * @param value
	 * @return
	 */
	public static boolean isValidDataBaseBoolean(String value) {
		if (Util.isEmpty(value, true)) {
			return false;
		}
		//	
		return value.equals("Y")
			|| value.equals("N")
		;
	}


	public static boolean getBooleanFromObject(Object value) {
		boolean booleanValue = false;
		if (value == null) {
			return booleanValue;
		}
		if (value instanceof Boolean) {
			booleanValue = (boolean) value;
		} else if (value instanceof String) {
			booleanValue = BooleanManager.getBooleanFromString(
				(String) value
			);
		} else if (value instanceof Integer) {
			booleanValue = BooleanManager.getBooleanFromInteger(
				(Integer) value
			);
		}
		return booleanValue;
	}


	public static boolean getBooleanFromString(String stringValue) {
		if (Util.isEmpty(stringValue, true)) {
			return false;
		}
		if ("Y".equals(stringValue) || "Yes".equals(stringValue) || "true".equals(stringValue)) {
			return true;
		}
		return false;
	}


	public static boolean getBooleanFromInteger(Integer integerValue) {
		if (integerValue == null) {
			return false;
		}
		return getBooleanFromInt(integerValue);
	}
	public static boolean getBooleanFromInt(int integerValue) {
		if (integerValue == 0 || integerValue == -1) {
			return false;
		}
		return true;
	}



	/**
	 * "Y" / "N" , "Yes" / "Not", "true" / "false"
	 * @param value
	 * @return "Y" / "N"
	 */
	public static String getBooleanToString(String value) {
		return getBooleanToString(
			getBooleanFromString(value)
		);
	}
	public static String getBooleanToString(boolean value) {
		String convertedValue = "N";
		if (value) {
			convertedValue = "Y";
		}
		return convertedValue;
	}

	public static String getBooleanToTranslated(boolean value) {
		return getBooleanToTranslated(
			value,
			Env.getAD_Language(Env.getCtx())
		);
	}
	public static String getBooleanToTranslated(boolean value, String language) {
		return Msg.getMsg(
			language,
			getBooleanToString(value)
		);
	}

	public static String getBooleanToTranslated(String value) {
		String acceptedValue = getBooleanToString(value);
		return getBooleanToTranslated(
			acceptedValue,
			Env.getAD_Language(Env.getCtx())
		);
	}
	public static String getBooleanToTranslated(String value, String language) {
		String acceptedValue = getBooleanToString(value);
		return Msg.getMsg(
			language,
			acceptedValue
		);
	}



	/**
	 * Get value from a boolean value
	 * @param value
	 * @return
	 */
	public static Value.Builder getProtoValueFromBoolean(boolean value) {
		return Value.newBuilder().setBoolValue(value);
	}
	/**
	 * Get value from a Boolean value
	 * @param value
	 * @return
	 */
	public static Value.Builder getProtoValueFromBoolean(Boolean value) {
		if(value == null) {
			// TODO: Validate to false value
			return ValueManager.getProtoValueFromNull();
		}
		return BooleanManager.getProtoValueFromBoolean(
			value.booleanValue()
		);
	}
	/**
	 * Get value from a String Boolean value ("Y" / "N")
	 * @param value
	 * @return Value.Builder
	 */
	public static Value.Builder getProtoValueFromBoolean(String value) {
		return BooleanManager.getProtoValueFromBoolean(
			BooleanManager.getBooleanFromString(value)
		);
	}

	/**
	 * Get Boolean from a Proto Value
	 * @param value
	 * @return
	 */
	public static boolean getBooleanFromProtoValue(Value value) {
		if (value == null) {
			return false;
		}
		if (!Util.isEmpty(value.getStringValue(), true)) {
			return BooleanManager.getBooleanFromString(
				value.getStringValue()
			);
		}

		return value.getBoolValue();
	}

}
