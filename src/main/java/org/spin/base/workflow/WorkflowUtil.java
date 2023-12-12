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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.util.value.ValueManager;

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


	/**
	 * Check document action access
	 * Based on `org.compiere.model.MRole.checkActionAccess` method
	 */
	public static boolean checkDocumentActionAccess(int documentTypeId, String documentAction) {
		if (documentTypeId <= 0) {
			return false;
		}
		if (Util.isEmpty(documentAction, true)) {
			return false;
		}
		Properties context = Env.getCtx();
		MRole role = MRole.get(context, Env.getAD_Role_ID(context));

		final List<Object> params = new ArrayList<Object>();
		params.add(Env.getAD_Client_ID(context));
		params.add(documentTypeId);
		params.add(documentAction);

		final String sql = "SELECT 1" // DISTINCT rl.Value
			+ " FROM AD_Document_Action_Access a"
			+ " INNER JOIN AD_Ref_List rl ON (rl.AD_Reference_ID=135 AND rl.AD_Ref_List_ID=a.AD_Ref_List_ID)"
			+ " WHERE a.IsActive='Y' AND a.AD_Client_ID=? AND a.C_DocType_ID=?" // #1,2
			+ " AND rl.Value = ?"
			+ " AND " + role.getIncludedRolesWhereClause("a.AD_Role_ID", params)
		;
		int withAccess = DB.getSQLValue(null, sql, params);
		return withAccess == 1;
	}



	public static ProcessLog.Builder startWorkflow(String tableName, int recordId, String documentAction) {
		// validate and get table
		final MTable table = RecordUtil.validateAndGetTable(
			tableName
		);

		Properties context = Env.getCtx();
		ProcessLog.Builder response = ProcessLog.newBuilder()
			.setResultTableName(
				ValueManager.validateNull(tableName)
			)
		;
		try {
			if (!table.isDocument()) {
				throw new AdempiereException("@NotValid@ @AD_Table_ID@ @IsDocument@@");
			}

			if (recordId <= 0) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}

			PO entity = RecordUtil.getEntity(context, table.getTableName(), recordId, null);
			if (entity == null || entity.get_ID() <= 0) {
				throw new AdempiereException("@Error@ @PO@ @NotFound@");
			}
			//	Validate as document
			if (!DocAction.class.isAssignableFrom(entity.getClass())) {
				throw new AdempiereException("@Invalid@ @Document@");
			}

			Integer doctypeId = (Integer) entity.get_Value("C_DocType_ID");
			if(doctypeId == null || doctypeId.intValue() <= 0){
				doctypeId = (Integer) entity.get_Value("C_DocTypeTarget_ID");
			}
			boolean isWithAccess = checkDocumentActionAccess(
				doctypeId,
				documentAction
			);
			if (!isWithAccess) {
				throw new AdempiereException("@AccessCannotProcess@");
			}

			entity.set_ValueOfColumn(I_C_Order.COLUMNNAME_DocAction, documentAction);
			entity.saveEx();
			//	Process It
			//	Get WF from Table
			MColumn column = table.getColumn(I_C_Order.COLUMNNAME_DocAction);
			if(column != null) {
				MProcess process = MProcess.get(context, column.getAD_Process_ID());
				if(process.getAD_Workflow_ID() > 0) {
					MWorkflow workFlow = MWorkflow.get (context, process.getAD_Workflow_ID());
					String name = process.get_Translation(I_AD_Process.COLUMNNAME_Name);
					ProcessInfo processInfo = new ProcessInfo(name, process.getAD_Process_ID(), table.getAD_Table_ID(), entity.get_ID());
					processInfo.setAD_User_ID (Env.getAD_User_ID(context));
					processInfo.setAD_Client_ID(Env.getAD_Client_ID(context));
					processInfo.setInterfaceType(ProcessInfo.INTERFACE_TYPE_NOT_SET);
					if(processInfo.getAD_PInstance_ID() <= 0) {
						MPInstance instance = null;
						//	Set to null for reload
						//	BR [ 380 ]
						processInfo.setParameter(null);
						try {
							instance = new MPInstance(
								context,
								processInfo.getAD_Process_ID(),
								processInfo.getRecord_ID()
							);
							instance.setName(name);
							instance.saveEx();
							//	Set Instance
							processInfo.setAD_PInstance_ID(instance.getAD_PInstance_ID());
						} catch (Exception e) { 
							processInfo.setSummary (e.getLocalizedMessage()); 
							processInfo.setError (true); 
							log.warning(processInfo.toString()); 
							processInfo.getSummary();
							throw new AdempiereException(e);
						}
					}
					if (processInfo.isBatch()) {
						workFlow.start(processInfo);		//	may return null
					} else {
						workFlow.startWait(processInfo);	//	may return null
					}
					String summary = processInfo.getSummary();
					response.setSummary(
						ValueManager.validateNull(summary)
					);
					response.setIsError(processInfo.isError());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.severe(e.getLocalizedMessage());
			String summary = e.getLocalizedMessage();
			if (Util.isEmpty(summary, true)) {
				summary = e.getLocalizedMessage();
			}
			response.setSummary(
				ValueManager.validateNull(summary)
			);
			response.setIsError(true);
		}

		return response;
	}

}
