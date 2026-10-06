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
package org.spin.service.grpc.util.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.util.Env;
import org.spin.util.ASPUtil;

/**
 * Class for handle SQL Order By
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class OrderByUtil {

	public static String SQL_ORDER_BY_REGEX = "\\s+(ORDER BY)\\s+";

	public static Pattern SQL_ORDER_BY_PATTERN = Pattern.compile(
		SQL_ORDER_BY_REGEX,
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL
	);

	/**	ORDER BY with any whitespace between and around its words	*/
	private static final Pattern ANY_ORDER_BY_PATTERN = Pattern.compile(
		"\\s+ORDER\\s+BY\\s+",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL
	);



	/**
	 * Get Order By
	 * @param browser
	 * @return
	 */
	public static String getBrowseOrderBy(MBrowse browser) {
		StringBuilder sqlOrderBy = new StringBuilder();
		for (MBrowseField field : ASPUtil.getInstance().getBrowseOrderByFields(browser.getAD_Browse_ID())) {
			if (sqlOrderBy.length() > 0) {
				sqlOrderBy.append(",");
			}

			MViewColumn viewColumn = MViewColumn.getById(Env.getCtx(), field.getAD_View_Column_ID(), null);
			sqlOrderBy.append(viewColumn.getColumnSQL());
		}
		return sqlOrderBy.length() > 0 ? sqlOrderBy.toString(): "";
	}

	/**
	 * Get Order By Postirion for SB
	 * @param browser
	 * @param browserField
	 * @return
	 */
	public static int getBrowserFieldOrderByPosition(MBrowse browser, MBrowseField browserField) {
		int colOffset = 1; // columns start with 1
		int col = 0;
		for (MBrowseField field : browser.getFields()) {
			int sortBySqlNo = col + colOffset;
			if (browserField.getAD_Browse_Field_ID() == field.getAD_Browse_Field_ID()) {
				return sortBySqlNo;
			}
			col ++;
		}
		return -1;
	}



	/**
	 * Extract only Order By clause from sql
	 * @param sql
	 * @return
	 */
	public static String getOnlyOrderBy(String sql) {
		String orderByClause = "";
		// extract order by clause
		int positionOrderBy = getMainOrderByPosition(sql);
		if (positionOrderBy >= 0) {
			orderByClause = sql.substring(positionOrderBy);
		}
		return orderByClause;
	}


	/**
	 * Remove Order By clause from sql
	 * @param sql
	 * @return
	 */
	public static String removeOrderBy(String sql) {
		String sqlWithoutOrderBy = sql;
		// remove order by clause
		int positionOrderBy = getMainOrderByPosition(sql);
		if (positionOrderBy >= 0) {
			sqlWithoutOrderBy = sql.substring(0, positionOrderBy);
		}
		return sqlWithoutOrderBy;
	}

	/**
	 * Position of the last ORDER BY clause that belongs to the main query, ignoring the
	 * ones inside parentheses (sub-selects, derived tables) and inside string literals
	 * @param sql
	 * @return position of the whitespace before ORDER BY, or -1 if the main query has none
	 */
	public static int getMainOrderByPosition(String sql) {
		if (sql == null || sql.isEmpty()) {
			return -1;
		}
		int position = -1;
		int depth = 0;
		boolean isLiteral = false;
		int index = 0;
		Matcher matcherOrderBy = ANY_ORDER_BY_PATTERN.matcher(sql);
		while (matcherOrderBy.find()) {
			int start = matcherOrderBy.start();
			// parenthesis depth and literal state up to this ORDER BY
			for (; index < start; index++) {
				char character = sql.charAt(index);
				if (character == '\'') {
					isLiteral = !isLiteral;
				} else if (!isLiteral) {
					if (character == '(') {
						depth++;
					} else if (character == ')') {
						depth--;
					}
				}
			}
			if (depth == 0 && !isLiteral) {
				position = start;
			}
		}
		return position;
	}

}
