package org.spin.base.util;

import org.adempiere.core.domains.models.I_AD_Process;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MWindowAccess;
import org.compiere.model.Query;
import org.compiere.util.Env;

import java.util.Properties;

public class AccessUtil {

	/**
	 * Porcess is Access by Role or Record
	 * @param processId
	 * @return
	 */
	public static boolean isProcessAccess(int processId) {
		return isProcessAccess(
			MRole.getDefault(),
			processId
		);
	}
	/**
	 * Porcess is Access by Role or Record
	 * @param role
	 * @param processId
	 * @return
	 */
	public static boolean isProcessAccess(MRole role, int processId) {
		if (processId <= 0) {
			return false;
		}
		Boolean isRoleAccess = role.getProcessAccess(processId);
		if (isRoleAccess == null || !isRoleAccess.booleanValue()) {
			return false;
		}
		boolean isRecordAccess = role.isRecordAccess(
			I_AD_Process.Table_ID,
			processId,
			MRole.SQL_RO
		);
		return isRecordAccess;
	}


	/**
	 * Window is Access by Role
	 * @param tableId
	 * @return true if has access, false otherwise
	 */
	public static boolean isWindowAccessByTableID(int tableId) {
		return isWindowAccessByTableID(
				MRole.getDefault(),
				tableId
		);
	}
	/**
	 * Window is Access by Role
	 * @param role
	 * @param tableId
	 * @return true if has access, false otherwise
	 */
	public static boolean isWindowAccessByTableID(MRole role, int tableId) {
		if (tableId <= 0) {
			return false;
		}
		Properties ctx = Env.getCtx();
		MTable tableInstance = MTable.get(ctx, tableId);
		int windowId = tableInstance.getAD_Window_ID();
		Boolean isRoleAccess = role.getWindowAccess(windowId);
		if (isRoleAccess != null && isRoleAccess.booleanValue()) {
			return true;
		}

		isRoleAccess = role.getWindowAccess(tableInstance.getPO_Window_ID());
		if (isRoleAccess != null && isRoleAccess.booleanValue()) {
			return true;

		}
		String whereClause = "EXISTS (SELECT 1 FROM AD_Window w " +
				"INNER JOIN AD_Tab t ON (t.AD_Window_ID = w.AD_Window_ID) " +
				"WHERE w.AD_Window_ID = AD_Window_Access.AD_Window_ID " +
				"AND t.AD_Table_ID = ?) " +
				"AND AD_Window_Access.AD_Role_ID = ? ";
		int count = new Query (ctx, MWindowAccess.Table_Name, whereClause, null)
				.setParameters(tableId, role.getAD_Role_ID())
				.setOnlyActiveRecords(true)
				.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED,
						MRole.SQL_RO)
				.count();
		if (count > 0) {
			return true;
		}
		return false;
	}


}
