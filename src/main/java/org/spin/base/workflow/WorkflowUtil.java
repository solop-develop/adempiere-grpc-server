/************************************************************************************
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.spin.base.workflow;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.compiere.db.CConnection;
import org.compiere.interfaces.Server;
import org.compiere.model.MProcess;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFProcess;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Update Center
 */
public class WorkflowUtil {
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(WorkflowUtil.class);



	/**
	 * 	Check Status Change
	 *	@param tableName table name
	 *	@param recordId record
	 *	@param documentStatus current doc status
	 *	@return true if status not changed
	 */
	public static boolean checkStatus(String tableName, int recordId, String documentStatus) {
		final String sql = "SELECT 2 FROM " + tableName 
			+ " WHERE " + tableName + "_ID=" + recordId
			+ " AND DocStatus='" + documentStatus + "'"
		;
		int result = DB.getSQLValue(null, sql);
		return result == 2;
	}



	public static boolean startWorkflow(int workflowId, int processId, int tableId, int recordId, String transactionName) {
		//	Create new Instance
		ProcessInfo processInfo = new ProcessInfo(null, 
			processId,
			tableId,
			recordId
		);
		int userId = Env.getAD_User_ID(Env.getCtx());
		processInfo.setAD_User_ID(userId);
		return startWorkflow(workflowId, processInfo, transactionName);
	}

	public static boolean startWorkflow(int workflowId, ProcessInfo processInfo, String transactionName) {
		/**
		 * 	Check Existence of Workflow Activities
		 */
		String wfStatus = MWFActivity.getActiveInfo(
			Env.getCtx(),
			processInfo.getAD_Process_ID(),
			processInfo.getRecord_ID()
		);
		if (!Util.isEmpty(wfStatus, true)) {
			throw new AdempiereException("@WFActiveForRecord@" + wfStatus);
		}

		MProcess process = MProcess.get(Env.getCtx(), processInfo.getAD_Process_ID());

		// log.fine(AD_Workflow_ID + " - " + processInstance);
		boolean started = false;
		if (process.isServerProcess()) {
			try {
				Server server = CConnection.get().getServer();
				//	See ServerBean
				if (server != null) {
					processInfo = server.workflow(Env.getRemoteCallCtx(Env.getCtx()), processInfo, workflowId);
					log.finest("server => " + processInfo);
					started = true;
				}
			}
			catch (Exception ex) {
				log.log(Level.SEVERE, "AppsServer error", ex);
				started = false;
			}
		}
		//	Run locally
		if (!started && !process.isServerProcess()) {
			if (!Util.isEmpty(transactionName, true)) {
				processInfo.setTransactionName(transactionName);
			}
			MWFProcess wfProcess = ProcessUtil.startWorkFlow(Env.getCtx(), processInfo, workflowId);
			started = wfProcess != null;
		}
		return started;
	}
}
