/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.grpc.logic;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.ZoomInfoFactory;
import org.compiere.model.MQuery;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.record_management.ExistsRecordReferencesRequest;
import org.spin.backend.grpc.record_management.ExistsRecordReferencesResponse;
import org.spin.backend.grpc.record_management.ListRecordReferencesRequest;
import org.spin.backend.grpc.record_management.ListRecordReferencesResponse;
import org.spin.backend.grpc.record_management.RecordReferenceInfo;
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



	public static ExistsRecordReferencesResponse.Builder existsRecordReferences(ExistsRecordReferencesRequest request) {
		// validate tab
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @Mandatory@");
		}
		MTab tab = (MTab) RecordUtil.getEntity(Env.getCtx(), I_AD_Tab.Table_Name, request.getTabId(), null);
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		// builder
		ExistsRecordReferencesResponse.Builder builder = ExistsRecordReferencesResponse.newBuilder();

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		// validate multiple keys as accounting tables and translation tables
		if (table == null || !table.isSingleKey()) {
			return builder;
		}

		// validate record
		int recordId = request.getRecordId();
		RecordUtil.validateRecordId(recordId, table.getAccessLevel());

		PO entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), recordId, null);
		if (entity == null) {
			// 	throw new AdempiereException("@Record_ID@ @NotFound@");
			return builder;
		}

		List<ZoomInfoFactory.ZoomInfo> zoomInfos = ZoomInfoFactory.retrieveZoomInfos(entity, tab.getAD_Window_ID())
			.stream()
			.filter(zoomInfo -> {
				return zoomInfo.query.getRecordCount() > 0;
			})
			.collect(Collectors.toList());
		if (zoomInfos == null || zoomInfos.isEmpty()) {
			return builder;
		}
		int recordCount = zoomInfos.size();
		//	Return
		return builder.setRecordCount(recordCount);
	}

	/**
	 * Convert references to gRPC
	 * @param Env.getCtx()
	 * @param request
	 * @return
	 */
	public static ListRecordReferencesResponse.Builder listRecordReferences(ListRecordReferencesRequest request) {
		// validate tab
		if (request.getTabId() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @Mandatory@");
		}
		MTab tab = (MTab) RecordUtil.getEntity(Env.getCtx(), I_AD_Tab.Table_Name, request.getTabId(), null);
		if (tab == null || tab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}

		// builder
		ListRecordReferencesResponse.Builder builder = ListRecordReferencesResponse.newBuilder();

		MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());
		// validate multiple keys as accounting tables and translation tables
		if (!table.isSingleKey()) {
			return builder;
		}

		// validate multiple keys as accounting tables and translation tables
		if (table == null || !table.isSingleKey()) {
			return builder;
		}

		// validate record
		int recordId = request.getRecordId();
		RecordUtil.validateRecordId(recordId, table.getAccessLevel());

		PO entity = RecordUtil.getEntity(Env.getCtx(), table.getTableName(), recordId, null);
		if (entity == null) {
			// 	throw new AdempiereException("@Record_ID@ @NotFound@");
			return builder;
		}

		List<ZoomInfoFactory.ZoomInfo> zoomInfos = ZoomInfoFactory.retrieveZoomInfos(entity, tab.getAD_Window_ID())
			.stream()
			.filter(zoomInfo -> {
				return zoomInfo.query.getRecordCount() > 0;
			})
			.collect(Collectors.toList());
		if (zoomInfos == null || zoomInfos.isEmpty()) {
			return builder;
		}

		zoomInfos.stream().forEach(zoomInfo -> {
			MQuery zoomQuery = zoomInfo.query;
			//
			RecordReferenceInfo.Builder recordReferenceBuilder = RecordReferenceInfo.newBuilder();

			MWindow referenceWindow = MWindow.get(Env.getCtx(), zoomInfo.windowId);
			MTab referenceTab = Arrays.stream(referenceWindow.getTabs(false, null))
				.filter(tabItem -> {
					return zoomQuery.getZoomTableName().equals(tabItem.getAD_Table().getTableName());
				})
				.findFirst()
				.orElse(null)
			;
			recordReferenceBuilder.setWindowId(referenceWindow.getAD_Window_ID());
			if (referenceTab != null && referenceTab.getAD_Tab_ID() > 0) {
				recordReferenceBuilder.setTabId(referenceTab.getAD_Tab_ID());
			}

			String uuid = referenceWindow.getUUID() + '_' + zoomQuery.getZoomTableName() + '_' + zoomQuery.getZoomColumnName();
			RecordUtil.referenceWhereClauseCache.put(uuid, zoomQuery.getWhereClause());

			recordReferenceBuilder.setUuid(uuid)
				.setTableName(
					ValueManager.validateNull(
						zoomQuery.getZoomTableName()
					)
				)
				.setWhereClause(
					ValueManager.validateNull(
						zoomQuery.getWhereClause()
					)
				)
				.setRecordCount(
					zoomQuery.getRecordCount()
				)
				.setDisplayName(
					zoomInfo.destinationDisplay + " (#" + zoomQuery.getRecordCount() + ")"
				)
				.setColumnName(
					ValueManager.validateNull(
						zoomQuery.getZoomColumnName()
					)
				)
				.setValue(
					ValueManager.getValueFromObject(
						zoomQuery.getZoomValue()
					).build()
				)
			;

			//	Add to list
			builder.addReferences(recordReferenceBuilder.build());
		});

		builder.setRecordCount(zoomInfos.size());

		//	Return
		return builder;
	}

}
