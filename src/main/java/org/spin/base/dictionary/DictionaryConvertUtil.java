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
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/

package org.spin.base.dictionary;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.core.domains.models.X_AD_Reference;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MWindow;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.dictionary.Reference;
import org.spin.backend.grpc.dictionary.ZoomWindow;
import org.spin.base.util.DictionaryUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ValueUtil;
import org.spin.util.ASPUtil;

public class DictionaryConvertUtil {

	/**
	 * Convert Reference to builder
	 * @param info
	 * @return
	 */
	public static Reference.Builder convertReference(Properties context, MLookupInfo info) {
		if (info == null) {
			return Reference.newBuilder();
		}

		List<String> contextColumnsList = DictionaryUtil.getContextColumnNames(
			Optional.ofNullable(info.QueryDirect).orElse("") + Optional.ofNullable(info.Query).orElse("") + Optional.ofNullable(info.ValidationCode).orElse("")
		);
		Reference.Builder builder = Reference.newBuilder()
			.setTableName(ValueUtil.validateNull(info.TableName))
			.setKeyColumnName(ValueUtil.validateNull(info.KeyColumn))
			.setDisplayColumnName(ValueUtil.validateNull(info.DisplayColumn))
			.addAllContextColumnNames(
				contextColumnsList
			)
		;

		// reference value
		if (info.AD_Reference_Value_ID > 0) {
			builder.setId(info.AD_Reference_Value_ID);
			String uuid = RecordUtil.getUuidFromId(X_AD_Reference.Table_Name, info.AD_Reference_Value_ID);
			builder.setUuid(ValueUtil.validateNull(uuid));
		}

		//	Window Reference
		if (info.ZoomWindow > 0) {
			builder.addZoomWindows(convertZoomWindow(context, info.ZoomWindow).build());
		}
		// window reference Purchase Order
		if (info.ZoomWindowPO > 0) {
			builder.addZoomWindows(convertZoomWindow(context, info.ZoomWindowPO).build());
		}
		//	Return
		return builder;
	}



	/**
	 * Convert Zoom Window from ID
	 * @param windowId
	 * @return
	 */
	public static ZoomWindow.Builder convertZoomWindow(Properties context, int windowId) {
		MWindow window = ASPUtil.getInstance(context).getWindow(windowId); // new MWindow(context, windowId, null);
		//	Get translation
		String name = null;
		String description = null;
		String language = Env.getAD_Language(context);
		if (!Util.isEmpty(language, true)) {
			name = window.get_Translation(I_AD_Window.COLUMNNAME_Name, language);
			description = window.get_Translation(I_AD_Window.COLUMNNAME_Description, language);
		}
		//	Validate for default
		if (Util.isEmpty(name, true)) {
			name = window.getName();
		}
		if (Util.isEmpty(description, true)) {
			description = window.getDescription();
		}
		//	Return
		return ZoomWindow.newBuilder()
			.setId(window.getAD_Window_ID())
			.setUuid(ValueUtil.validateNull(window.getUUID()))
			.setName(ValueUtil.validateNull(name))
			.setDescription(ValueUtil.validateNull(description))
			.setIsSalesTransaction(window.isSOTrx())
		;
	}

}
