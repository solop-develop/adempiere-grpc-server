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

import org.compiere.model.MClient;
import org.compiere.model.MCountry;
import org.compiere.model.MLanguage;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;
import org.spin.service.grpc.util.query.Filter;
import org.spin.service.grpc.util.value.ValueManager;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Class for handle Context
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ContextManager {
	
	/**	Language */
	private static CCache<String, String> languageCache = new CCache<String, String>("Language-gRPC-Service", 30, 0);	//	no time-out


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
	 * @param context
	 * @return
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
	 * Is Session context, #Global or $Accouting
	 * @param contextKey
	 * @return
	 */
	public static boolean isSessionContext(String contextKey) {
		return contextKey.startsWith("#") || contextKey.startsWith("$");
	}


	public static Properties setContextWithAttributesFromObjectMap(int windowNo, Properties context, Map<String, Object> attributes) {
		return setContextWithAttributes(windowNo, context, attributes, true);
	}
	
	public static Properties setContextWithAttributesFromStruct(int windowNo, Properties context, Struct attributes) {
		return setContextWithAttributesFromValuesMap(windowNo, context, attributes.getFieldsMap());
	}
	
	public static Properties setContextWithAttributesFromValuesMap(int windowNo, Properties context, Map<String, Value> attributes) {
		return setContextWithAttributes(windowNo, context, ValueManager.convertValuesMapToObjects(attributes), true);
	}

	public static Properties setContextWithAttributesFromString(int windowNo, Properties context, String jsonValues) {
		return setContextWithAttributesFromString(windowNo, context, jsonValues, true);
	}

	public static Properties setContextWithAttributesFromString(int windowNo, Properties context, String jsonValues, boolean isClearWindow) {
		Map<String, Object> attributes = ValueManager.convertJsonStringToMap(jsonValues);
		return setContextWithAttributes(windowNo, context, attributes, isClearWindow);
	}

	/**
	 * Set context with attributes
	 * @param windowNo
	 * @param context
	 * @param attributes
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
	 * Set context on window by object value
	 * @param windowNo
	 * @param context
	 * @param windowNo
	 * @param key
	 * @param value
	 * @return {Properties} context with new values
	 */
	public static void setWindowContextByObject(Properties context, int windowNo, String key, Object value) {
		if (value instanceof Integer) {
			Env.setContext(context, windowNo, key, (Integer) value);
		} else if (value instanceof BigDecimal) {
			String currentValue = null;
			if (value != null) {
				currentValue = value.toString();
			}
			Env.setContext(context, windowNo, key, currentValue);
		} else if (value instanceof Timestamp) {
			Env.setContext(context, windowNo, key, (Timestamp) value);
		} else if (value instanceof Boolean) {
			Env.setContext(context, windowNo, key, (Boolean) value);
		} else if (value instanceof String) {
			Env.setContext(context, windowNo, key, (String) value);
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
	 * @param windowNo
	 * @param context
	 * @param windowNo
	 * @param tabNo
	 * @param key
	 * @param value
	 * @return {Properties} context with new values
	 */
	public static void setTabContextByObject(Properties context, int windowNo, int tabNo, String key, Object value) {
		if (value instanceof Integer) {
			Integer currentValue = (Integer) value;
			Env.setContext(context, windowNo, tabNo, key, currentValue.toString());
		}else if (value instanceof BigDecimal) {
			String currentValue = null;
			if (value != null) {
				currentValue = value.toString();
			}
			Env.setContext(context, windowNo, tabNo, key, currentValue);
		} else if (value instanceof Timestamp) {
			Timestamp currentValue = (Timestamp) value;
			Env.setContext(context, windowNo, tabNo, key, currentValue.toString());
		} else if (value instanceof Boolean) {
			Env.setContext(context, windowNo, tabNo, key, (Boolean) value);
		} else if (value instanceof String) {
			Env.setContext(context, windowNo, tabNo, key, (String) value);
		}
	}


	/**
	 * Get Default Country
	 * @return
	 */
	public static MCountry getDefaultCountry() {
		MClient client = MClient.get (Env.getCtx());
		MLanguage language = MLanguage.get(Env.getCtx(), client.getAD_Language());
		MCountry country = MCountry.get(Env.getCtx(), language.getCountryCode());
		//	Verify
		if(country != null) {
			return country;
		}
		//	Default
		return MCountry.getDefault(Env.getCtx());
	}
	
	/**
	 * Get Default from language
	 * @param language
	 * @return
	 */
	public static String getDefaultLanguage(String language) {
		MClient client = MClient.get(Env.getCtx());
		String clientLanguage = client.getAD_Language();
		if(!Util.isEmpty(clientLanguage)
				&& Util.isEmpty(language)) {
			return clientLanguage;
		}
		String defaultLanguage = language;
		if(Util.isEmpty(language)) {
			language = Language.AD_Language_en_US;
		}
		//	Using es / en instead es_VE / en_US
		//	get default
		if(language.length() == 2) {
			defaultLanguage = languageCache.get(language);
			if(!Util.isEmpty(defaultLanguage)) {
				return defaultLanguage;
			}
			defaultLanguage = DB.getSQLValueString(null, "SELECT AD_Language "
					+ "FROM AD_Language "
					+ "WHERE LanguageISO = ? "
					+ "AND (IsSystemLanguage = 'Y' OR IsBaseLanguage = 'Y')", language);
			//	Set language
			languageCache.put(language, defaultLanguage);
		}
		if(Util.isEmpty(defaultLanguage)) {
			defaultLanguage = Language.AD_Language_en_US;
		}
		//	Default return
		return defaultLanguage;
	}
}
