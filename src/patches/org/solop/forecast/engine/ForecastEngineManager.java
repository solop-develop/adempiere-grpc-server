/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2025 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/
package org.solop.forecast.engine;

import org.compiere.model.MClientInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Util;

import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Forecast Engine Manager
 * Factory and cache for IForecastEngine instances per client.
 * Reads the ForecastEngine class name from AD_ClientInfo and
 * instantiates it via reflection. Falls back to DefaultForecastEngine.
 * @author Gabriel Escalona
 */
public class ForecastEngineManager {

	/** Logger */
	private static final CLogger log = CLogger.getCLogger(ForecastEngineManager.class);
	/**
	 * Get the Forecast Engine for a given client.
	 * Looks up from cache first, then tries to instantiate from
	 * AD_ClientInfo.ForecastEngine column, falling back to DefaultForecastEngine.
	 * @param ctx context
	 * @param clientId AD_Client_ID
	 * @return IForecastEngine instance (never null)
	 */
	public static IForecastEngine getForecastEngine(Properties ctx, int clientId) {
		MClientInfo clientInfo = MClientInfo.get(ctx, clientId);
		String className = null;
		if (clientInfo != null) {
			className = clientInfo.getForecastEngine();
		}
		if (Util.isEmpty(className, true)) {
			return new DefaultForecastEngine();
		}

        return createEngine(ctx, className, clientId);
	}

	/**
	 * Create the engine by reading the class name from AD_ClientInfo.
	 * If no class is configured, returns DefaultForecastEngine.
	 * @param ctx context
	 * @param clientId AD_Client_ID
	 * @return IForecastEngine instance
	 */
	private static IForecastEngine createEngine(Properties ctx, String className, int clientId) {
		IForecastEngine engine = null;
		// Instantiate via reflection
		try {
			Class<?> clazz = Class.forName(className.trim());
			if(IForecastEngine.class.isAssignableFrom(clazz)) {
				Constructor<?> constructor = clazz.getDeclaredConstructor();
				log.info("Loaded ForecastEngine: " + className + " for AD_Client_ID=" + clientId);
				engine = (IForecastEngine) constructor.newInstance();
			} else {
				engine = new DefaultForecastEngine();
			}
		} catch (Exception e) {
			log.warning("Failed to load ForecastEngine class: " + className
					+ " for AD_Client_ID=" + clientId + ". Using default. Error: " + e.getMessage());
			engine = new DefaultForecastEngine();
		}
		return engine;
	}
}
