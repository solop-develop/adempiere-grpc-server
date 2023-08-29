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
package org.spin.base.dictionary.custom;

import org.adempiere.core.domains.models.I_AD_FieldCustom;
import org.compiere.model.MFieldCustom;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Class for handle Field Custom
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class FieldCustomUtil {

	public static MFieldCustom getFieldCustom(int fieldId) {
		final int userId = Env.getAD_User_ID(Env.getCtx());

		// final String sql = "SELECT AD_Window_ID FROM AD_Window "
		// 	+ "WHERE EXISTS( "
		// 	+ "	SELECT 1 FROM AD_Tab t "
		// 	+ "	INNER JOIN AD_Field f "
		// 	+ "	ON t.AD_Tab_ID = f.AD_Tab_ID "
		// 	+ "	AND t.AD_Window_ID = ad_window.AD_Window_ID "
		// 	+ "	WHERE f.AD_Field_ID = ? "
		// 	+")";
		// int windowId = DB.getSQLValueEx(null, sql, fieldId);
		// ASPUtil.getInstance().getWindow(windowId);

		final String whereClauseUser = "AD_Field_ID = ? AND EXISTS( "
			+ "SELECT 1 FROM AD_WindowCustom AS wc "
			+ "INNER JOIN AD_TabCustom AS tc "
			+ "	ON tc.AD_WindowCustom_ID = wc.AD_WindowCustom_ID "
			+ "	AND tc.AD_TabCustom_ID = AD_FieldCustom.AD_TabCustom_ID "
			+ "WHERE wc.AD_User_ID = ? "
		+ ")";
		MFieldCustom fieldCustom = new Query(
			Env.getCtx(),
			I_AD_FieldCustom.Table_Name,
			whereClauseUser,
			null
		)
		.setParameters(fieldId, userId)
		.first();
		if (fieldCustom == null) {
			final int roleId = Env.getAD_Role_ID(Env.getCtx());
			final String whereClauseRole = "AD_Field_ID = ? AND EXISTS( "
				+ "SELECT 1 FROM AD_WindowCustom AS wc "
				+ "INNER JOIN AD_TabCustom AS tc "
				+ "	ON tc.AD_WindowCustom_ID = wc.AD_WindowCustom_ID "
				+ "	AND tc.AD_TabCustom_ID = AD_FieldCustom.AD_TabCustom_ID "
				+ "WHERE wc.AD_Role_ID = ? "
			+ ")";
			fieldCustom = new Query(
				Env.getCtx(),
				I_AD_FieldCustom.Table_Name,
				whereClauseRole,
				null
			)
			.setParameters(fieldId, roleId)
			.first();
		}
		// TODO: Add to ASP_Level

		return fieldCustom;
	}

}
