package org.spin.base.db;

import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.util.Env;
import org.spin.util.ASPUtil;

public class OrderByUtil {

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
	 * @param BrowserField
	 * @return
	 */
	public static int getBrowserFieldOrderByPosition(MBrowse browser, MBrowseField browserField) {
		int colOffset = 1; // columns start with 1
		int col = 0;
		for (MBrowseField field : browser.getFields()) {
			int sortBySqlNo = col + colOffset;
			if (browserField.getAD_Browse_Field_ID() == field.getAD_Browse_Field_ID())
				return sortBySqlNo;
			col ++;
		}
		return -1;
	}

}
