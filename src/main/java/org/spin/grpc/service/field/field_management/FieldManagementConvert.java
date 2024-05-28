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
package org.spin.grpc.service.field.field_management;

import java.util.Optional;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Window;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.util.Env;
import org.spin.backend.grpc.field.ZoomWindow;
import org.spin.service.grpc.util.value.ValueManager;
import org.spin.util.ASPUtil;

public class FieldManagementConvert {

	/**
	 * Convert Zoom Window from ID
	 * @param windowId
	 * @return
	 */
	public static ZoomWindow.Builder convertZoomWindow(Properties context, int windowId, String tableName) {
		String language = Env.getAD_Language(context);
		boolean isBaseLanguage = Env.isBaseLanguage(context, null);

		MWindow window = ASPUtil.getInstance(context).getWindow(windowId); // new MWindow(context, windowId, null);
		ZoomWindow.Builder builder = ZoomWindow.newBuilder()
			.setId(
				window.getAD_Window_ID()
			)
			.setName(
				ValueManager.validateNull(
					window.getName()
				)
			)
			.setDescription(
				ValueManager.validateNull(
					window.getDescription()
				)
			)
			.setIsSalesTransaction(
				window.isSOTrx()
			)
		;

		MTable table = MTable.get(context, tableName);
		Optional<MTab> maybeTab = ASPUtil.getInstance(context).getWindowTabs(windowId)
			.stream().filter(currentTab -> {
				return currentTab.getAD_Table_ID() == table.getAD_Table_ID();
			})
			.findFirst()
		;
		if (maybeTab.isPresent()) {
			MTab tab = maybeTab.get();
			builder.setTabId(
					tab.getAD_Tab_ID()
				)
				.setTabUuid(
					ValueManager.validateNull(
						tab.getUUID()
					)
				)
			;
		}

		if (!isBaseLanguage) {
			builder.setName(
					ValueManager.validateNull(
						window.get_Translation(I_AD_Window.COLUMNNAME_Name, language)
					)
				)
				.setDescription(
					ValueManager.validateNull(
						window.get_Translation(I_AD_Window.COLUMNNAME_Description, language)
					)
				)
			;
		}

		//	Return
		return builder;
	}

}
