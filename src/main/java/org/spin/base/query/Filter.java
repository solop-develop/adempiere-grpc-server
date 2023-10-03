/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
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

package org.spin.base.query;

import java.util.List;
import java.util.Map;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * A Stub class that represent a filters from request
 */
public class Filter {
	
	
	public static final String VOID = "void";
	public static final String EQUAL = "equal";
	public static final String NOT_EQUAL = "not_equal";
	public static final String LIKE = "like";
	public static final String NOT_LIKE = "not_like";
	public static final String GREATER = "greater";
	public static final String GREATER_EQUAL = "greater_equal";
	public static final String LESS = "less";
	public static final String LESS_EQUAL = "less_equal";
	public static final String BETWEEN = "between";
	public static final String NOT_BETWEEN = "not_between";
	public static final String NOT_NULL = "not_null";
	public static final String NULL = "null";
	public static final String IN = "in";
	public static final String NOT_IN = "not_in";
	//	
	public static final String NAME = "name";
	public static final String OPERATOR = "operator";
	public static final String VALUES = "values";
	//	Values
	private static final int FROM = 0;
	private static final int TO = 1;
	
	private Map<String, Object> condition;
	
	public Filter(Map<String, Object> condition) {
		this.condition = condition;
	}

	public void setColumnName(String columnName) {
		condition.put(NAME, columnName);
	}

	public String getColumnName() {
		return (String) condition.get(NAME);
	}
	
	public String getOperator() {
		return (String) condition.get(OPERATOR);
	}

	public Object getValue() {
		return condition.get(VALUES);
	}
	
	@SuppressWarnings("unchecked")
	public List<Object> getValues() {
		Object value = condition.get(VALUES);
		if(value instanceof List) {
			return (List<Object>) value;
		}
		return null;
	}
	
	public Object getFromValue() {
		List<Object> values = getValues();
		if(values == null) {
			return null;
		}
		return values.get(FROM);
	}
	
	public Object getToValue() {
		List<Object> values = getValues();
		if(values == null
				|| values.size() >= 2) {
			return null;
		}
		return values.get(TO);
	}

	@Override
	public String toString() {
		return "Filter [getColumnName()=" + getColumnName() + ", getOperator()=" + getOperator() + ", getValue()="
				+ getValue() + ", getValues()=" + getValues() + ", getFromValue()=" + getFromValue() + ", getToValue()="
				+ getToValue() + "]";
	}
}
