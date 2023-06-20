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
import org.spin.backend.grpc.common.Condition;
import org.spin.backend.grpc.common.Operator;
import org.spin.backend.grpc.common.Value.ValueType;

/**
 * Class for handle SQL Operators
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class OperatorUtil {

	/**
	 * Convert operator from gRPC to SQL
	 * @param gRpcOperator
	 * @return
	 */
	public static String convertOperator(int gRpcOperator) {
		String operator = MQuery.EQUAL;
		switch (gRpcOperator) {
			case Operator.BETWEEN_VALUE:
				operator = MQuery.BETWEEN;
				break;
			case Operator.NOT_BETWEEN_VALUE:
				operator = " NOT BETWEEN ";
				break;
			case Operator.EQUAL_VALUE:
				operator = MQuery.EQUAL;
				break;
			case Operator.GREATER_EQUAL_VALUE:
				operator = MQuery.GREATER_EQUAL;
				break;
			case Operator.GREATER_VALUE:
				operator = MQuery.GREATER;
				break;
			case Operator.IN_VALUE:
				operator = " IN ";
				break;
			case Operator.LESS_EQUAL_VALUE:
				operator = MQuery.LESS_EQUAL;
				break;
			case Operator.LESS_VALUE:
				operator = MQuery.LESS;
				break;
			case Operator.LIKE_VALUE:
				operator = MQuery.LIKE;
				break;
			case Operator.NOT_EQUAL_VALUE:
				operator = MQuery.NOT_EQUAL;
				break;
			case Operator.NOT_IN_VALUE:
				operator = " NOT IN ";
				break;
			case Operator.NOT_LIKE_VALUE:
				operator = MQuery.NOT_LIKE;
				break;
			case Operator.NOT_NULL_VALUE:
				operator = MQuery.NOT_NULL;
				break;
			case Operator.NULL_VALUE:
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
	public static int getDefaultOperatorByDisplayType(int displayTypeId) {
		int operator = Operator.EQUAL_VALUE;
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
				operator = Operator.LIKE_VALUE;
				break;
			default:
				break;
			}
		return operator;
	}

	/**
	 * Evaluate value, valueTo, and values to get operator
	 * @param condition
	 * @return
	 */
	public static int getDefaultOperatorByConditionValue(Condition condition) {
		if (condition.getValuesList().size() > 0) {
			// list operator
			return Operator.IN_VALUE;
		} else if (condition.getValueTo().getValueType() != ValueType.UNKNOWN) {
			if (condition.getValue().getValueType() != ValueType.UNKNOWN) {
				// range operator
				return Operator.BETWEEN_VALUE;
			}
			// only to value
			// return Operator.LESS_EQUAL_VALUE;
		}
		if (condition.getValue().getValueType() == ValueType.STRING) {
			// like
			return Operator.LIKE_VALUE;
		}
		// no valid column set default operator
		return Operator.EQUAL_VALUE;
	}

}
