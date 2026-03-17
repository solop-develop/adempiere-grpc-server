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

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.compiere.util.Util;

import com.google.protobuf.Value;

/**
 * Class for handle Text (String, Character, Charset) values
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class TextManager {

	/**
	 * Convert null on ""
	 * @param value
	 * @return
	 */
	public static String getValidString(String value) {
		return Optional.ofNullable(value).orElse("");
	}


	/**
	 * Convert Object to String
	 * @param value
	 * @return
	 */
	public static String getStringFromObject(Object value) {
		String textValue = null;
		if (value == null) {
			return textValue;
		}
		if (value instanceof Value) {
			textValue = TextManager.getStringFromProtoValue(
				(Value) value
			);
		} else if (value instanceof Value.Builder) {
			Value newValue = ((Value.Builder) value).build();
			textValue = TextManager.getStringFromProtoValue(
				newValue
			);
		} else {
			// textValue = (String) value;
			textValue = value.toString();
		}
		return textValue;
	}


	/**
	 * Get Decode URL value
	 * @param value
	 * @return
	 */
	public static String getDecodeUrl(String value) {
		// URL decode to change characteres
		return getDecodeUrl(
			value,
			StandardCharsets.UTF_8
		);
	}
	/**
	 * Get Decode URL value
	 * @param value
	 * @param charsetType
	 * @return
	 */
	public static String getDecodeUrl(String value, Charset charsetType) {
		if (Util.isEmpty(value, true)) {
			return value;
		}
		// URL decode to change characteres
		String parseValue = URLDecoder.decode(
			value,
			charsetType
		);
		return parseValue;
	}



	/**
	 * Get String from Proto Value
	 * @param value
	 * @return
	 */
	public static String getStringFromProtoValue(Value value) {
		return getStringFromProtoValue(value, false);
	}
	/**
	 * Get String from Proto Value
	 * @param value
	 * @param uppercase
	 * @return
	 */
	public static String getStringFromProtoValue(Value value, boolean uppercase) {
		if (value == null) {
			return null;
		}
		String stringValue = value.getStringValue();
		if(Util.isEmpty(stringValue, true)) {
			return null;
		}
		//	To Upper case
		if(uppercase) {
			stringValue = stringValue.toUpperCase();
		}
		return stringValue;
	}

	/**
	 * Get Proto Value from a tring
	 * @param value
	 * @return
	 */
	public static Value.Builder getProtoValueFromString(String value) {
		if (value == null) {
			return ValueManager.getProtoValueFromNull();
		}
		return Value.newBuilder().setStringValue(
			TextManager.getValidString(
				value
			)
		);
	}

}
