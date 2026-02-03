/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com                                         *
 *************************************************************************************/
package org.spin.base.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.MCountry;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.CollectionManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for handle Context
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ContextManager {

	/**
	 * Prefix context of global prefix (#)
	 */
	public final static String GLOBAL_CONTEXT_PREFIX = "#";

	/**
	 * Prefix context of accounting prefix ($)
	 */
	public final static String ACCOUNTING_CONTEXT_PREFIX = "$";

	/**
	 * Prefix context of preference prefix (P|)
	 */
	public final static String PREFERENCE_CONTEXT_PREFIX = "#";

	/**
	 * Prefix context of preference prefix (P0123|)
	 */
	public final static String PREFERENCE_CONTEXT_REGEX = "^P(?:\\||\\d+\\|).*";

	/**
	 * Prefix context of tab sequence prefix (0|)
	 */
	public final static String TAB_CONTEXT_PREFIX = "\\d\\|";

	/**
	 * Prefix context of sql value prefix (@SQL=) or (@SQL =)
	 */
	public final static String SQL_CONTEXT_PREFIX = "^(@SQL)\\s*=";


	public static String joinStrings(String... stringValues) {
		// StringBuilder stringBuilder = new StringBuilder();
		// for (String stringValue : stringValues) {
		// 	if (!Util.isEmpty(stringValue, true)) {
		// 		stringBuilder.append(stringValue);
		// 	}
		// }
		// return stringBuilder.toString();
		return String.join(
			"",
			Arrays.stream(stringValues)
				.filter(s -> !Util.isEmpty(s, true))
				.toArray(String[]::new)
		);
	}

	/**
	 * Get Context column names from context
	 * @param values
	 * @return List<String>
	 */
	public static List<String> getContextColumnNames(String... values) {
		if (values == null || values.length <= 0) {
			return new ArrayList<String>();
		}
		String context = joinStrings(values);
		if (Util.isEmpty(context, true)) {
			return new ArrayList<String>();
		}
		String START = "\\@";  // A literal "(" character in regex
		String END   = "\\@";  // A literal ")" character in regex

		// Captures the word(s) between the above two character(s)
		final String COLUMN_NAME_PATTERN = START + "(#|$|\\d|\\|){0,1}(\\w+)" + END;

		Pattern pattern = Pattern.compile(
			COLUMN_NAME_PATTERN,
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		);
		Matcher matcher = pattern.matcher(context);
		Map<String, Boolean> columnNamesMap = new HashMap<String, Boolean>();
		while(matcher.find()) {
			columnNamesMap.put(matcher.group().replace("@", "").replace("@", ""), true);
		}
		return new ArrayList<String>(columnNamesMap.keySet());
	}

	/**
	 * Determinate if columnName is used on context values
	 * @param columnName
	 * @param context
	 * @return boolean
	 */
	public static boolean isUseParentColumnOnContext(String columnName, String context) {
		if (Util.isEmpty(columnName, true)) {
			return false;
		}
		if (Util.isEmpty(context, true)) {
			return false;
		}

		// @ColumnName@ , @#ColumnName@ , @$ColumnName@
		StringBuffer patternValue = new StringBuffer()
			.append("@")
			.append("($|#|\\d\\|){0,1}")
			.append(columnName)
			.append("(@)")
		;

		Pattern pattern = Pattern.compile(
			patternValue.toString(),
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		);
		Matcher matcher = pattern.matcher(context);
		boolean isUsedParentColumn = matcher.find();

		// TODO: Delete this condition when fix evaluator (readonlyLogic on Client Info)
		// TODO: https://github.com/adempiere/adempiere/pull/4124
		if (!isUsedParentColumn) {
			// @ColumnName , @#ColumnName , @$ColumnName
			patternValue.append("{0,1}");
			Pattern pattern2 = Pattern.compile(
				patternValue.toString(),
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL
			);
			Matcher matcher2 = pattern2.matcher(context);
			isUsedParentColumn = matcher2.find();
		}

		return isUsedParentColumn;
	}



	/**
	 * Is Session context, `#ColumnName` -> `#AD_Client_ID` or `$ColumnName` -> `$C_Currency_ID`
	 * @param contextKey
	 * @return
	 */
	public static boolean isSessionContext(String contextKey) {
		return contextKey.startsWith(GLOBAL_CONTEXT_PREFIX) || contextKey.startsWith(ACCOUNTING_CONTEXT_PREFIX);
	}

	/**
	 * Is preference context, `P|ColumnName` -> `P|EntityType` or `P1234|ColumnName` -> `P5001|C_Tax_ID`
	 * @param contextKey
	 * @return
	 */
	public static boolean isPreferenceConext(String contextKey) {
		return contextKey.startsWith(PREFERENCE_CONTEXT_PREFIX) || contextKey.matches("^P\\d+\\|.*");
	}



	public static Properties setContextWithAttributesFromObjectMap(int windowNo, Properties context, Map<String, Object> attributes) {
		return setContextWithAttributes(windowNo, context, attributes, true);
	}
	
	public static Properties setContextWithAttributesFromStruct(int windowNo, Properties context, Struct attributes) {
		return setContextWithAttributesFromValuesMap(windowNo, context, attributes.getFieldsMap());
	}
	
	public static Properties setContextWithAttributesFromValuesMap(int windowNo, Properties context, Map<String, Value> attributes) {
		Map<String, Object> attributesObject = CollectionManager.getMapObjectFromMapProtoValue(attributes);
		return setContextWithAttributes(
			windowNo,
			context,
			attributesObject,
			true
		);
	}

	public static Properties setContextWithAttributesFromString(int windowNo, Properties context, String jsonValues) {
		return setContextWithAttributesFromString(windowNo, context, jsonValues, true);
	}

	public static Properties setContextWithAttributesFromString(int windowNo, Properties context, String jsonValues, boolean isClearWindow) {
		Map<String, Object> attributes = CollectionManager.getMapFromJsomString(jsonValues);
		return setContextWithAttributes(windowNo, context, attributes, isClearWindow);
	}

	/**
	 * Set context with attributes
	 * @param windowNo
	 * @param context
	 * @param attributes
	 * @param isClearWindow
	 * @return {Properties} context with new values
	 */
	public static Properties setContextWithAttributes(int windowNo, Properties context, Map<String, Object> attributes, boolean isClearWindow) {
		if (isClearWindow) {
			Env.clearWinContext(windowNo);
		}
		if (attributes == null || attributes.size() <= 0) {
			return context;
		}

		//	Fill context
		attributes.entrySet()
			.forEach(attribute -> {
				setWindowContextByObject(context, windowNo, attribute.getKey(), attribute.getValue());
			});

		return context;
	}

	/**
	 * Set context with PO record
	 * @param windowNo
	 * @param context
	 * @param record
	 * @param isClearWindow
	 * @return {Properties} context with new values
	 */
	public static Properties setContextFromPO(int windowNo, Properties context, PO record, boolean isClearWindow) {
		if (isClearWindow) {
			Env.clearWinContext(windowNo);
		}
		if (record == null) {
			return context;
		}

		//	Fill context
		int columnsSize = record.get_ColumnCount();
		for (int index = 0; index < columnsSize; index++) {
			String columnName = record.get_ColumnName(index);
			Object value = record.get_Value(columnName);
			setWindowContextByObject(context, windowNo, columnName, value);
		}

		return context;
	}



	/**
	 * Set context on window by object value
	 * @param context
	 * @param windowNo
	 * @param key
	 * @param value
	 * @return {Properties} context with new values
	 */
	public static void setWindowContextByObject(Properties context, int windowNo, String key, Object value) {
		if (value instanceof Integer) {
			int currentValue = NumberManager.getIntFromObject(value);
			Env.setContext(context, windowNo, key, currentValue);
		} else if (value instanceof BigDecimal) {
			String currentValue = null;
			if (value != null) {
				currentValue = NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromObject(value)
				);
			}
			Env.setContext(context, windowNo, key, currentValue);
		} else if (value instanceof Timestamp) {
			Timestamp currentValue = TimeManager.getTimestampFromObject(value);
			Env.setContext(context, windowNo, key, currentValue);
		} else if (value instanceof Boolean) {
			boolean currentValue = BooleanManager.getBooleanFromObject(value);
			Env.setContext(context, windowNo, key, currentValue);
		} else if (value instanceof String) {
			String currentValue = TextManager.getStringFromObject(value);
			Env.setContext(context, windowNo, key, currentValue);
		} else if (value instanceof ArrayList) {
			// range values
			List<?> values = (List<?>) value;
			if (values != null && !values.isEmpty()) {
				// value from
				Object valueStart = values.get(
					Filter.FROM
				);
				setWindowContextByObject(context, windowNo, key, valueStart);

				// value to
				if (values.size() == 2) {
					Object valueEnd = values.get(
						Filter.TO
					);
					setWindowContextByObject(context, windowNo, key + "_To", valueEnd);
				} else if (values.size() > 2) {
					// `IN` / `NOT IN` operator with multiple values
					;
				}
			}
		}
	}

	/**
	 * Set context on tab by object value
	 * @param context
	 * @param windowNo
	 * @param tabNo
	 * @param key
	 * @param value
	 * @return {Properties} context with new values
	 */
	public static void setTabContextByObject(Properties context, int windowNo, int tabNo, String key, Object value) {
		if (value instanceof Integer) {
			String currentValue = NumberManager.getIntToString(
				NumberManager.getIntFromObject(value)
			);
			Env.setContext(context, windowNo, tabNo, key, currentValue);
		}else if (value instanceof BigDecimal) {
			String currentValue = null;
			if (value != null) {
				currentValue = NumberManager.getBigDecimalToString(
					NumberManager.getBigDecimalFromObject(value)
				);
			}
			Env.setContext(context, windowNo, tabNo, key, currentValue);
		} else if (value instanceof Timestamp) {
			String currentValue = TimeManager.getTimestampToString(
				TimeManager.getTimestampFromObject(value)
			);
			Env.setContext(context, windowNo, tabNo, key, currentValue);
		} else if (value instanceof Boolean) {
			boolean currentValue = BooleanManager.getBooleanFromObject(value);
			Env.setContext(context, windowNo, tabNo, key, currentValue);
		} else if (value instanceof String) {
			String currentValue = TextManager.getStringFromObject(value);
			Env.setContext(context, windowNo, tabNo, key, currentValue);
		}
	}



	/**
	 * Set context on window by object value
	 * @param contextValue
	 * @param displayTypeId
	 * @return {Properties} context with new values
	 */
	public static Object getContextVaue(String contextValue, int displayTypeId) {
		// String contextValue = Env.getContext(context, windowNo, key, false);
		if (contextValue == null) {
			return null;
		} else if (displayTypeId <= 0) {
			return contextValue;
		}

		//	Validate values
		if(DisplayType.isID(displayTypeId) || DisplayType.Integer == displayTypeId) {
			if (DisplayType.Search == displayTypeId || DisplayType.Table == displayTypeId) {
				Object lookupValue = contextValue;
				try {
					// casteable for integer, except `AD_Language`, `EntityType`
					lookupValue = Integer.valueOf(
						lookupValue.toString()
					);
				} catch (Exception e) {
				}
				return lookupValue;
			}
			return NumberManager.getIntegerFromString(contextValue);
		} else if(DisplayType.isNumeric(displayTypeId)) {
			return NumberManager.getBigDecimalFromString(contextValue);
		} else if(DisplayType.YesNo == displayTypeId) {
			return BooleanManager.getBooleanFromString(contextValue);
		} else if(DisplayType.isDate(displayTypeId)) {
			return TimeManager.getTimestampFromString(contextValue);
		} else if(DisplayType.isText(displayTypeId) || DisplayType.List == displayTypeId) {
			return contextValue;
		} else if (DisplayType.Button == displayTypeId) {
			// TODO: Validate with BigDecimal, and Text with reference
			return contextValue;
		}
		//	
		return contextValue;
	}


	/**
	 * Get Default Country
	 * @return
	 */
	public static MCountry getDefaultCountry(Properties context) {
		// MClient client = MClient.get(context);
		// MLanguage language = MLanguage.get(context, client.getAD_Language());
		// MCountry country = MCountry.get(context, language.getCountryCode());
		// //	Verify
		// if(country != null) {
		// 	return country;
		// }
		// //	Default
		// return MCountry.getDefault(context);
		return SessionManager.getDefaultCountry(context);
	}

}
