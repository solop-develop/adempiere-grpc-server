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

import java.util.Arrays;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Element;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MTab;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.dictionary.util.WindowUtil;

/**
 * Class for handle SQL Order By
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class OrderByUtil {

	public static String getTabOrderByClause(MTab tab) {
		//	First Prio: Tab Order By
		String orderByClause = tab.getOrderByClause();
		if (!Util.isEmpty(orderByClause, true)) {
			return orderByClause;
		}

		//	Second Prio: Fields (save it)
		orderByClause = getTabOrderByWithFields(tab);
		if (!Util.isEmpty(orderByClause, true)) {
			return orderByClause;
		}

		//	Third Prio: onlyCurrentRows
		orderByClause = I_AD_Element.COLUMNNAME_Created;
		boolean isOnlyCurrentRows = true;
		if (isOnlyCurrentRows && !(WindowUtil.isDetail(tab))) {
			//	first tab only
			orderByClause += " DESC";
		}

		return orderByClause;
	}

	public static String getTabOrderByWithFields(MTab tab) {
		String[] orderBys = getTabOrderBysColumns(tab);
		//	Second Prio: Fields (save it)
		String orderByClause = "";
		int count = 0;
		for (String orderColumnName : orderBys) {
			// String orderColumnName = orderBys[i];
			if (!Util.isEmpty(orderColumnName, true)) {
				if (orderByClause.length() > 0) {
					orderByClause += ",";
				}
				orderByClause += orderColumnName;
				count++;
			}
			if (count > 2) {
				break;
			}
		}

		return orderByClause;
	}

	public static String[] getTabOrderBysColumns(MTab tab) {
		String orderBys[] = new String[3];
		List<MField> fielsList = Arrays.asList(
			tab.getFields(false, null)
		);
		for (MField field : fielsList) {
			if (field.getSortNo() == null || field.getSortNo().compareTo(Env.ZERO) == 0) {
				continue;
			}
			MColumn column = MColumn.get(Env.getCtx(), field.getAD_Column_ID());
			String columnName = column.getColumnName();
			//	Order By
			int sortNo = field.getSortNo().intValue();
			if (sortNo == 0) {
				;
			}
			else if (Math.abs(sortNo) == 1) {
				orderBys[0] = columnName;
				if (sortNo < 0) {
					orderBys[0] += " DESC";
				}
			}
			else if (Math.abs(sortNo) == 2) {
				orderBys[1] = columnName;
				if (sortNo < 0) {
					orderBys[1] += " DESC";
				}
			}
			else if (Math.abs(sortNo) == 3) {
				orderBys[2] = columnName;
				if (sortNo < 0) {
					orderBys[2] += " DESC";
				}
			}
		};

		return orderBys;
	}

}
