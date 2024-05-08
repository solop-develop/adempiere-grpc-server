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

	//	
	public static final String NAME = "name";
	public static final String OPERATOR = "operator";
	public static final String VALUES = "values";
	//	Values
	public static final int FROM = 0;
	public static final int TO = 1;

	private Map<String, Object> condition;

	public Filter(Map<String, Object> newCondition) {
		this.condition = newCondition;
	}

	public void setColumnName(String columnName) {
		this.condition.put(NAME, columnName);
	}

	public String getColumnName() {
		Object key = this.condition.get(NAME);
		if (key == null) {
			return null;
		}
		return (String) key;
	}

	public String getOperator() {
		Object operator = this.condition.get(OPERATOR);
		if (operator == null) {
			return null;
		}
		return (String) operator;
	}

	public Object getValue() {
		return this.condition.get(VALUES);
	}

	@SuppressWarnings("unchecked")
	public List<Object> getValues() {
		Object value = this.condition.get(VALUES);
		if (value == null) {
			return null;
		}
		if(value instanceof List) {
			return (List<Object>) value;
		}
		return null;
	}

	public Object getFromValue() {
		List<Object> values = this.getValues();
		if(values == null || values.isEmpty()) {
			return null;
		}
		return values.get(FROM);
	}

	public Object getToValue() {
		List<Object> values = this.getValues();
		if(values == null || values.isEmpty() || values.size() > 2) {
			return null;
		}
		return values.get(TO);
	}

	@Override
	public String toString() {
		return "Filter [getColumnName()=" + this.getColumnName() +
			", getOperator()=" + this.getOperator() +
			", getValue()=" + this.getValue() + ", getValues()=" + this.getValues() +
			", getFromValue()=" + this.getFromValue() + ", getToValue()=" + this.getToValue() + "]";
	}

}
