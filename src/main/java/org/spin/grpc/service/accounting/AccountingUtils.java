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
package org.spin.grpc.service.accounting;

import java.util.Arrays;
import java.util.List;

import org.adempiere.core.domains.models.I_C_AcctSchema_Element;
import org.adempiere.core.domains.models.I_C_ValidCombination;
import org.adempiere.core.domains.models.X_C_AcctSchema_Element;
// import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MColumn;
import org.compiere.model.Query;
import org.compiere.util.Env;

public class AccountingUtils {

	public static List<String> USER_LIST_COLUMNS = Arrays.asList(
		I_C_ValidCombination.COLUMNNAME_User1_ID,
		I_C_ValidCombination.COLUMNNAME_User2_ID,
		I_C_ValidCombination.COLUMNNAME_User3_ID,
		I_C_ValidCombination.COLUMNNAME_User4_ID
	);


	public static List<String> USER_ELEMENT_COLUMNS = Arrays.asList(
		I_C_ValidCombination.COLUMNNAME_UserElement1_ID,
		I_C_ValidCombination.COLUMNNAME_UserElement2_ID
	);


	public static String overwriteColumnName(String columnName) {
		if (!USER_ELEMENT_COLUMNS.contains(columnName)) {
			return null;
		}
		final int clientId = Env.getAD_Client_ID(Env.getCtx());
		if (clientId <= 0) {
			return null;
		}
		final int accountingSchemaId = Env.getContextAsInt(Env.getCtx(), "$C_AcctSchema_ID");
		if (accountingSchemaId > 0) {
			// MAcctSchema accountingSchema = MAcctSchema.get(Env.getCtx(), accountingSchemaId, null);
			MAcctSchemaElement schemaElement = new Query(
				Env.getCtx(),
				I_C_AcctSchema_Element.Table_Name,
				"C_AcctSchema_ID = ? AND (ElementType = ? OR ElementType = ?)",
				null
			)
				.setParameters(accountingSchemaId, X_C_AcctSchema_Element.ELEMENTTYPE_UserElement1, X_C_AcctSchema_Element.ELEMENTTYPE_UserElement2)
				.first()
			;
			if (schemaElement != null && schemaElement.getAD_Column_ID() > 0) {
				MColumn column = MColumn.get(Env.getCtx(), schemaElement.getAD_Column_ID());
				return column.getColumnName();
			}
		}
		return null;
	}
}
