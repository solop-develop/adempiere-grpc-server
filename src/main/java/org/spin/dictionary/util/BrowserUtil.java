/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/
package org.spin.dictionary.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.model.Query;

public class BrowserUtil {

	/**
	 * Get Browse Fields by selection to display type
	 * @param browser
	 * @return
	 */
	public static Map<String, Integer> getBrowseFieldsSelectionDisplayType(MBrowse browser) {
		List<MBrowseField> browseFields = new Query(
			browser.getCtx(),
			MBrowseField.Table_Name,
			"AD_Browse_ID = ? AND (IsIdentifier = ? OR IsKey = ? OR IsReadOnly = ?)",
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(browser.getAD_Browse_ID(), true, true, false)
			.setOrderBy(MBrowseField.COLUMNNAME_SeqNo)
			.list()
		;

		Map<String, Integer> displayTypeColumns = new HashMap<>();

		if (browseFields == null || browseFields.isEmpty()) {
			return displayTypeColumns;
		}
		browseFields.forEach(browseField -> {
			MViewColumn viewColumn = MViewColumn.getById(
				browser.getCtx(),
				browseField.getAD_View_Column_ID(),
				null
			);

			displayTypeColumns.put(
				viewColumn.getColumnName(),
				browseField.getAD_Reference_ID()
			);
		});

		return displayTypeColumns;
	}

}
