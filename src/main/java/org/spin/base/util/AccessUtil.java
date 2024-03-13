package org.spin.base.util;

import org.adempiere.core.domains.models.I_AD_Process;
import org.compiere.model.MRole;

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

}
