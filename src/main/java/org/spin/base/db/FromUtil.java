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

import org.compiere.util.Util;

/**
 * Class for handle SQL From
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class FromUtil {

	/**
	 * Get regex pattern to match with table name and/or table alias
	 * @param tableName
	 * @param tableNameAlias
	 * @return
	 */
	public static String getPatternTableName(String tableName, String tableNameAlias) {
		// tableName tableName, tableName AS tableName
		String patternTableWithAliases = tableName + "\\s+" + tableName + "|" + tableName + "\\s+AS\\s+" + tableName;
		if (!Util.isEmpty(tableNameAlias, true) && !tableName.equals(tableNameAlias)) {
			// tableName tableAlias, tableName AS tableAlias
			patternTableWithAliases += "|" + tableName + "\\s+" + tableNameAlias + "|" + tableName + "\\s+AS\\s+" + tableNameAlias;
		}
		// only table on last position
		patternTableWithAliases += "|" + tableName;

		return patternTableWithAliases;
	}

}
