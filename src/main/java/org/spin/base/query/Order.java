/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/

package org.spin.base.query;

import java.util.Map;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * A Stub class that represent a sorting from request
 */
public class Order {

	public static final String ASCENDING = "asc";
	public static final String DESCENDING = "des";
	//	
	public static final String NAME = "name";
	public static final String TYPE = "type";

	private Map<String, Object> order;

	public Order(Map<String, Object> newOrder) {
		this.order = newOrder;
	}

	public String getColumnName() {
		return (String) this.order.get(NAME);
	}

	public String getSortType() {
		return (String) this.order.get(TYPE);
	}

	@Override
	public String toString() {
		return "Order [getColumnName()=" + getColumnName() + ", getSortType()=" + getSortType() + "]";
	}
}
