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

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 * A Stub class that represent a filters from request
 */
public class FilterManager {
	
	/**
	 * read filters and convert to stub
	 * @param filter
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Filter> readFilters(String filter) {
		ObjectMapper fileMapper = new ObjectMapper();
		try {
			return fileMapper.readValue(filter, List.class);
		} catch (JsonProcessingException e) {}
		return null;
	}
	
	public static void main(String[] args) {
		List<Filter> filters = readFilters("[{\"name\":\"AD_Client_ID\", \"operator\":\"equal\", \"value\": 1000000}]");
		if(filters != null) {
			System.err.println(filters.getClass().getName());
		}
	}
}
