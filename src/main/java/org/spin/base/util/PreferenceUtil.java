/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt EdwinBetanc0urt@outlook.com                     *
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
package org.spin.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Preference;
import org.compiere.model.MPreference;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class PreferenceUtil {
	/** Language			*/
	public static final String P_LANGUAGE = "Language";

	/** Role */
	public static final String P_ROLE = "Role";

	/** Client Name */
	public static final String P_CLIENT = "Client";

	/** Org Name */
	public static final String P_ORG = "Organization";

	/** Warehouse Name */
	public static final String P_WAREHOUSE = "Warehouse";


	public static List<String> PROPERTIES_LIST = Arrays.asList(
		P_LANGUAGE, P_ROLE, P_CLIENT, P_ORG, P_WAREHOUSE
	);


	/**
	 * Get Session Preferences
	 * @param userId
	 * @return
	 */
	public static List<MPreference> getSessionPreferences(int userId) {
		List<MPreference> preferencesList = new ArrayList<MPreference>();
		if (userId <= 0) {
			return preferencesList;
		}

		ArrayList<Object> queryParameters = new ArrayList<>();
		queryParameters.add(userId);
		queryParameters.addAll(PreferenceUtil.PROPERTIES_LIST);

		preferencesList = new Query(
			Env.getCtx(),
			I_AD_Preference.Table_Name,
			"AD_User_ID = ? AND Attribute IN(?, ?, ?, ?, ?) AND AD_Window_ID Is NULL",
			null
		)
			.setParameters(queryParameters)
			.<MPreference>list()
		;
		return preferencesList;
	}


	/**
	 * Save Session Preferences
	 * @param userId
	 * @param language
	 * @param roleId
	 * @param clientId
	 * @param organizationId
	 * @param warehouseId
	 */
	public static void saveSessionPreferences(
		// query
		int userId,
		// values
		String language, int roleId, int clientId, int organizationId, int warehouseId
	) {
		List<MPreference> preferencesList = PreferenceUtil.getSessionPreferences(userId);
		for (MPreference preference: preferencesList) {
			String attibuteName = preference.getAttribute();
			if (attibuteName.equals(PreferenceUtil.P_ROLE)) {
				preference.setValue(
					String.valueOf(roleId)
				);
			} else if (attibuteName.equals(PreferenceUtil.P_CLIENT)) {
				preference.setValue(
					String.valueOf(clientId)
				);
			} else if (attibuteName.equals(PreferenceUtil.P_ORG)) {
				preference.setValue(
					String.valueOf(organizationId)
				);
			} else if (attibuteName.equals(PreferenceUtil.P_WAREHOUSE)) {
				preference.setValue(
					String.valueOf(warehouseId)
				);
			} else if (attibuteName.equals(PreferenceUtil.P_LANGUAGE)) {
				preference.setValue(
					language
				);
			}
			preference.save();
		}
	}

}
