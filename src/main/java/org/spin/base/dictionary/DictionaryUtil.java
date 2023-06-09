package org.spin.base.dictionary;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.util.Env;

public class DictionaryUtil {



	/**
	 * Get reference from column name and table
	 * @param tableId
	 * @param columnName
	 * @return
	 */
	public static int getReferenceId(int tableId, String columnName) {
		MColumn column = MTable.get(Env.getCtx(), tableId).getColumn(columnName);
		if (column == null || column.getAD_Column_ID() <= 0) {
			return -1;
		}
		return column.getAD_Reference_ID();
	}

}
