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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.Util;

/**
 * Class for handle SQL Limit
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class LimitUtil {

	/**	Page Size	*/
	public static final int PAGE_SIZE = 50;


	/**
	 * Get Page Size from client, else from default
	 * @param pageSize
	 * @return
	 */
	public static int getPageSize(int pageSize) {
		return pageSize > 0 ? pageSize : PAGE_SIZE;
	}

	/**
	 * Get Page Prefix
	 * @param sessionUuid
	 * @return
	 */
	public static String getPagePrefix(String sessionUuid) {
		return sessionUuid + "-";
	}



	/**
	 * Validate if can have a next page token
	 * @param count
	 * @param offset
	 * @param limit
	 * @return
	 * @return boolean
	 */
	public static boolean isValidNextPageToken(int count, int offset, int limit) {
		return count > (offset + limit) && count > limit;
	}

	/**
	 * Get Page Number
	 * @param sessionUuid
	 * @param pageToken
	 * @return
	 */
	public static int getPageNumber(String sessionUuid, String pageToken) {
		int page = 1;
		String pagePrefix = getPagePrefix(sessionUuid);
		if(!Util.isEmpty(pageToken, true)) {
			if(pageToken.startsWith(pagePrefix)) {
				try {
					page = Integer.parseInt(pageToken.replace(pagePrefix, ""));
				} catch (Exception e) {
					//	
				}
			} else {
				try {
					page = Integer.parseInt(pageToken);
				} catch (Exception e) {
					//	
				}
			}
		}
		if (page < 1) {
			page = 1;
		}
		//	
		return page;
	}



	/**
	 * Get Query with limit
	 * @param query
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static String getQueryWithLimit(String query, int limit, int offset) {
		Matcher matcher = Pattern.compile(
			OrderByUtil.SQL_ORDER_BY_REGEX,
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
		).matcher(query);
		int positionFrom = -1;
		if(matcher.find()) {
			positionFrom = matcher.start();
			query = query.substring(0, positionFrom) + " AND ROWNUM >= " + offset + " AND ROWNUM <= " + limit + " " + query.substring(positionFrom);
		} else {
			query = query + " AND ROWNUM >= " + offset + " AND ROWNUM <= " + limit;
		}
		return query;
	}

}
