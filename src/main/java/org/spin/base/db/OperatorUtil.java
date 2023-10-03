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

import org.compiere.model.MQuery;
import org.compiere.util.DisplayType;
import org.spin.base.query.Filter;

/**
 * Class for handle SQL Operators
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class OperatorUtil {

	public static final String SQL_OPERATORS_REGEX = "(<>|<=|>=|!=|<|=|>|NOT\\s+IN|IN|NOT\\s+BETWEEN|BETWEEN|NOT\\s+LIKE|LIKE|IS\\s+NULL|IS\\s+NOT\\s+NULL)";


	/**
	 * Convert operator from gRPC to SQL
	 * @param serviceOperator
	 * @return
	 */
	public static String convertOperator(String serviceOperator) {
		String operator = MQuery.EQUAL;
		switch (serviceOperator.toLowerCase()) {
			case Filter.BETWEEN:
				operator = MQuery.BETWEEN;
				break;
			case Filter.NOT_BETWEEN:
				operator = " NOT BETWEEN ";
				break;
			case Filter.EQUAL:
				operator = MQuery.EQUAL;
				break;
			case Filter.GREATER_EQUAL:
				operator = MQuery.GREATER_EQUAL;
				break;
			case Filter.GREATER:
				operator = MQuery.GREATER;
				break;
			case Filter.IN:
				operator = " IN ";
				break;
			case Filter.LESS_EQUAL:
				operator = MQuery.LESS_EQUAL;
				break;
			case Filter.LESS:
				operator = MQuery.LESS;
				break;
			case Filter.LIKE:
				operator = MQuery.LIKE;
				break;
			case Filter.NOT_EQUAL:
				operator = MQuery.NOT_EQUAL;
				break;
			case Filter.NOT_IN:
				operator = " NOT IN ";
				break;
			case Filter.NOT_LIKE:
				operator = MQuery.NOT_LIKE;
				break;
			case Filter.NOT_NULL:
				operator = MQuery.NOT_NULL;
				break;
			case Filter.NULL:
				operator = MQuery.NULL;
				break;
			default:
				operator = MQuery.EQUAL;
				break;
		}
		return operator;
	}

	/**
	 * Get default operator by display type
	 * @param displayTypeId
	 * @return
	 */
	public static String getDefaultOperatorByDisplayType(int displayTypeId) {
		String operator = Filter.EQUAL;
		switch (displayTypeId) {
			case DisplayType.String:
			case DisplayType.Text:
			case DisplayType.TextLong:
			case DisplayType.Memo:
			case DisplayType.FilePath:
			case DisplayType.FileName:
			case DisplayType.FilePathOrName:
			case DisplayType.URL:
			case DisplayType.PrinterName:
				operator = Filter.LIKE;
				break;
			default:
				break;
			}
		return operator;
	}

}
