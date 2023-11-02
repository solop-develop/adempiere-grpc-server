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
package org.spin.grpc.logic;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordRequest;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordResponse;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordsBatchRequest;
import org.spin.backend.grpc.record_management.ToggleIsActiveRecordsBatchResponse;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Record Management
 */
public class RecordManagementServiceLogic {

	/**
	 * Validate table exists.
	 * @return table
	 */
	private static MTable validateAndGetTable(String tableName) {
		// validate table
		if (Util.isEmpty(tableName, true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Table_ID@");
		}
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID() <= 0) {
			throw new AdempiereException("@AD_Table_ID@ @NotFound@");
		}
		return table;
	}


	public static ToggleIsActiveRecordsBatchResponse.Builder toggleIsActiveRecords(ToggleIsActiveRecordsBatchRequest request) {
		StringBuilder errorMessage = new StringBuilder();
		AtomicInteger recordsChanges = new AtomicInteger(0);

		Trx.run(transactionName -> {
			MTable table = validateAndGetTable(request.getTableName());
			List<Integer> ids = request.getIdsList();
			if (ids.size() > 0) {
				ids.stream().forEach(id -> {
					PO entity = table.getPO(id, transactionName);
					if (entity != null && entity.get_ID() > 0) {
						if (entity.get_ColumnIndex("Processed") >= 0 && entity.get_ValueAsBoolean("Processed")) {
							return;
						}
						entity.setIsActive(request.getIsActive());
						try {
							entity.saveEx();
							recordsChanges.incrementAndGet();
						} catch (Exception e) {
							e.printStackTrace();
							errorMessage.append(e.getLocalizedMessage());
						}
					}
				});
			}
		});
		ToggleIsActiveRecordsBatchResponse.Builder builder = ToggleIsActiveRecordsBatchResponse.newBuilder()
			.setMessage(
				ValueManager.validateNull(
					errorMessage.toString()
				)
			)
			.setTotalChanges(recordsChanges.get())
		;

		//	Return
		return builder;
	}

	public static ToggleIsActiveRecordResponse.Builder toggleIsActiveRecord(ToggleIsActiveRecordRequest request) {
		StringBuilder errorMessage = new StringBuilder();
		AtomicInteger recordsChanges = new AtomicInteger(0);

		Trx.run(transactionName -> {
			MTable table = validateAndGetTable(request.getTableName());
			if (request.getId() <= 0 && !RecordUtil.isValidId(request.getId(), table.getAccessLevel())) {
				throw new AdempiereException("@FillMandatory@ @Record_ID@");
			}
			if (request.getId() > 0) {
				PO entity = RecordUtil.getEntity(Env.getCtx(), request.getTableName(), request.getId(), transactionName);
				if (entity != null && entity.get_ID() > 0) {
					if (entity.get_ColumnIndex("Processed") >= 0 && entity.get_ValueAsBoolean("Processed")) {
						return;
					}
					entity.setIsActive(request.getIsActive());
					try {
						entity.saveEx();
						recordsChanges.incrementAndGet();
					} catch (Exception e) {
						e.printStackTrace();
						errorMessage.append(e.getLocalizedMessage());
					}
				}
			}
		});
		ToggleIsActiveRecordResponse.Builder builder = ToggleIsActiveRecordResponse.newBuilder()
			.setMessage(
				ValueManager.validateNull(
					errorMessage.toString()
				)
			)
		;
		//	Return
		return builder;
	}

}
