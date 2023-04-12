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
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MCountry;
import org.compiere.model.MLanguage;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;

/**
 * Class for handle Context
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class ContextManager {
	
	/**	Language */
	private static CCache<String, String> languageCache = new CCache<String, String>("Language-gRPC-Service", 30, 0);	//	no time-out

	/**
	 * Set context with attributes
	 * @param windowNo
	 * @param context
	 * @param attributes
	 * @return {Properties} context with new values
	 */
	public static Properties setContextWithAttributes(int windowNo, Properties context, Map<String, Object> attributes) {
		Env.clearWinContext(windowNo);
		if (attributes == null || attributes.size() <= 0) {
			return context;
		}

		//	Fill context
		attributes.entrySet().forEach(attribute -> {
			if(attribute.getValue() instanceof Integer) {
				Env.setContext(context, windowNo, attribute.getKey(), (Integer) attribute.getValue());
			} else if(attribute.getValue() instanceof BigDecimal) {
				String value = null;
				if (attribute.getValue() != null) {
					value = attribute.getValue().toString();
				}
				Env.setContext(context, windowNo, attribute.getKey(), value);
			} else if(attribute.getValue() instanceof Timestamp) {
				Env.setContext(context, windowNo, attribute.getKey(), (Timestamp) attribute.getValue());
			} else if(attribute.getValue() instanceof Boolean) {
				Env.setContext(context, windowNo, attribute.getKey(), (Boolean) attribute.getValue());
			} else if(attribute.getValue() instanceof String) {
				Env.setContext(context, windowNo, attribute.getKey(), (String) attribute.getValue());
			}
		});
		
		return context;
	}

	public static Properties setContextWithAttributes(int windowNo, Properties context, java.util.List<org.spin.backend.grpc.common.KeyValue> values) {
		Map<String, Object> attributes = ValueUtil.convertValuesToObjects(values);
		return setContextWithAttributes(windowNo, context, attributes);
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
