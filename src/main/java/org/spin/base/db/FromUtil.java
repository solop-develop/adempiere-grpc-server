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
package org.spin.base.db;

import java.util.List;

import org.adempiere.model.MView;
import org.adempiere.model.MViewDefinition;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Class for handle SQL From
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class FromUtil {

	public static String getFromClauseByView(int viewId) {
		MView view = new MView(Env.getCtx(), viewId);
		List<MViewDefinition> viewDefinitionsList = view.getViewDefinitions();

		MViewDefinition fromViewDefinition = viewDefinitionsList.get(0);
		MTable table = new MTable(Env.getCtx(), fromViewDefinition.getAD_Table_ID(), null);
		String fromClause = " FROM " + table.getTableName() + " " + fromViewDefinition.getTableAlias();
		return fromClause;
	}


	/**
	 * Get regex pattern to match with table name and/or table alias
	 * @param tableName
	 * @param tableNameAlias
	 * @return
	 */
	public static String getPatternTableName(String tableName, String tableNameAlias) {
		String patternTableWithAliases = "";

		// tableName tableName, tableName AS tableName, tableName (only table on last position)
		String patternOnlyTableName = tableName + "\\s+" + tableName + "|" + tableName + "\\s+AS\\s+" + tableName + "|" + tableName;
		patternTableWithAliases = patternOnlyTableName;

		// tableName tableAlias, tableName AS tableAlias
		String patternOnlyTableAlias = "";
		if (!Util.isEmpty(tableNameAlias, true) && !tableName.equals(tableNameAlias)) {
			patternOnlyTableAlias = tableName + "\\s+" + tableNameAlias + "|" + tableName + "\\s+AS\\s+" + tableNameAlias;
			patternTableWithAliases = patternOnlyTableAlias;
		}

		// if (!Util.isEmpty(patternOnlyTableAlias, false)) {
		// 	patternTableWithAliases = patternOnlyTableAlias + "|" + patternOnlyTableName;
		// }

		// end with some patterns
		patternTableWithAliases = "(" + patternTableWithAliases + ")\\b";

		return patternTableWithAliases;
	}

}
