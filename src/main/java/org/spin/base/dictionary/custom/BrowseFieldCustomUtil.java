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

import org.adempiere.core.domains.models.I_AD_BrowseFieldCustom;
import org.compiere.model.MBrowseFieldCustom;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Class for handle Browse Field Custom
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class BrowseFieldCustomUtil {

	public static MBrowseFieldCustom getBrowseFieldCustom(int browseFieldId) {
		final int userId = Env.getAD_User_ID(Env.getCtx());
		final String whereClauseUser = "AD_Browse_Field_ID = ? AND EXISTS( "
			+ "SELECT 1 FROM AD_BrowseCustom AS bc "
			+ "WHERE bc.AD_User_ID = ? "
			+ "AND bc.AD_BrowseCustom_ID = AD_BrowseFieldCustom.AD_BrowseCustom_ID"
		+ ")";
		MBrowseFieldCustom browseFieldCustom = new Query(
			Env.getCtx(),
			I_AD_BrowseFieldCustom.Table_Name,
			whereClauseUser,
			null
		)
			.setParameters(browseFieldId, userId)
			.setOnlyActiveRecords(true)
			.first()
		;
		if (browseFieldCustom == null) {
			final int roleId = Env.getAD_Role_ID(Env.getCtx());
			final String whereClauseRole = "AD_Browse_Field_ID = ? AND EXISTS( "
				+ "SELECT 1 FROM AD_BrowseCustom AS bc "
				+ "WHERE bc.AD_Role_ID = ? "
				+ "AND bc.AD_BrowseCustom_ID = AD_BrowseFieldCustom.AD_BrowseCustom_ID"
			+ ")";
			browseFieldCustom = new Query(
				Env.getCtx(),
				I_AD_BrowseFieldCustom.Table_Name,
				whereClauseRole,
				null
			)
				.setParameters(browseFieldId, roleId)
				.setOnlyActiveRecords(true)
				.first()
			;
		}
		// TODO: Add to ASP_Level

		return browseFieldCustom;
	}

}
