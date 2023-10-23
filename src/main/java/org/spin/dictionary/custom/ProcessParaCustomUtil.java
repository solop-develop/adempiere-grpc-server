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
package org.spin.dictionary.custom;

import org.adempiere.core.domains.models.I_AD_ProcessParaCustom;
import org.compiere.model.MProcessParaCustom;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Class for handle Process Para Custom
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class ProcessParaCustomUtil {

	public static MProcessParaCustom getProcessParaCustom(int processParameterId) {
		final int userId = Env.getAD_User_ID(Env.getCtx());
		final String whereClauseUser = "AD_Process_Para_ID = ? AND EXISTS( "
			+ "SELECT 1 FROM AD_ProcessCustom AS pc "
			+ "WHERE pc.AD_User_ID = ? "
			+ "AND pc.AD_ProcessCustom_ID = AD_ProcessParaCustom.AD_ProcessCustom_ID"
		+ ")";
		MProcessParaCustom browseFieldCustom = new Query(
			Env.getCtx(),
			I_AD_ProcessParaCustom.Table_Name,
			whereClauseUser,
			null
		)
			.setParameters(processParameterId, userId)
			.setOnlyActiveRecords(true)
			.first()
		;
		if (browseFieldCustom == null) {
			final int roleId = Env.getAD_Role_ID(Env.getCtx());
			final String whereClauseRole = "AD_Process_Para_ID = ? AND EXISTS( "
				+ "SELECT 1 FROM AD_ProcessCustom AS pc "
				+ "WHERE pc.AD_Role_ID = ? "
				+ "AND pc.AD_ProcessCustom_ID = AD_ProcessParaCustom.AD_ProcessCustom_ID"
			+ ")";
			browseFieldCustom = new Query(
				Env.getCtx(),
				I_AD_ProcessParaCustom.Table_Name,
				whereClauseRole,
				null
			)
				.setParameters(processParameterId, roleId)
				.setOnlyActiveRecords(true)
				.first()
			;
		}
		// TODO: Add to ASP_Level

		return browseFieldCustom;
	}

}
