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
package org.spin.grpc.service.field.field_management;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.field.GetZoomParentRecordRequest;
import org.spin.backend.grpc.field.GetZoomParentRecordResponse;
import org.spin.backend.grpc.field.ListZoomWindowsRequest;
import org.spin.backend.grpc.field.ListZoomWindowsResponse;
import org.spin.backend.grpc.field.ZoomWindow;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceInfo;
import org.spin.service.grpc.util.value.ValueManager;

public class FieldManagementLogic {

	public static ListZoomWindowsResponse.Builder listZoomWindows(ListZoomWindowsRequest request) {
		Properties context = Env.getCtx();
		ListZoomWindowsResponse.Builder builderList = ListZoomWindowsResponse.newBuilder();

		MLookupInfo lookupInfo = ReferenceInfo.getInfoFromRequest(
			0,
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(), request.getTableName(),
			0, null
		);

		if (lookupInfo == null) {
			return builderList;
		}

		List<String> contextColumnsList = ContextManager.getContextColumnNames(
			lookupInfo.QueryDirect,
			lookupInfo.Query,
			lookupInfo.ValidationCode
		);

		MTable table = MTable.get(context, lookupInfo.TableName);
		List<String> keyColumnsList = Arrays.asList(
			table.getKeyColumns()
		);

		builderList.setTableName(
				ValueManager.validateNull(
					lookupInfo.TableName
				)
			)
			.setKeyColumnName(
				ValueManager.validateNull(
					lookupInfo.KeyColumn
				)
			)
			.addAllKeyColumns(
				keyColumnsList
			)
			.setDisplayColumnName(
				ValueManager.validateNull(
					lookupInfo.DisplayColumn
				)
			)
			.addAllContextColumnNames(
				contextColumnsList
			)
		;

		//	Window Reference
		if (lookupInfo.ZoomWindow > 0) {
			ZoomWindow.Builder windowSalesBuilder = FieldManagementConvert.convertZoomWindow(
				context,
				lookupInfo.ZoomWindow,
				lookupInfo.TableName
			);
			builderList.addZoomWindows(
				windowSalesBuilder.build()
			);
		}
		// window reference Purchase Order
		if (lookupInfo.ZoomWindowPO > 0) {
			ZoomWindow.Builder windowPurchaseBuilder = FieldManagementConvert.convertZoomWindow(
				context,
				lookupInfo.ZoomWindowPO,
				lookupInfo.TableName
			);
			builderList.addZoomWindows(
				windowPurchaseBuilder.build()
			);
		}

		return builderList;
	}


	public static GetZoomParentRecordResponse.Builder getZoomParentRecord(GetZoomParentRecordRequest request) {
		if (request.getWindowId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Window_ID@");
		}
		MWindow window = MWindow.get(
			Env.getCtx(),
			request.getWindowId()
		);
		if (window == null || window.getAD_Window_ID() <= 0) {
			throw new AdempiereException("@AD_Window_ID@ @NotFound@");
		}

		if (request.getTabId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}
		MTab currentTab = MTab.get(
			window.getCtx(),
			request.getTabId()
		);
		if (currentTab == null || currentTab.getAD_Tab_ID() <= 0) {
			throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
		}
		MTable currentTable = MTable.get(Env.getCtx(), currentTab.getAD_Table_ID());
		String[] keys = currentTable.getKeyColumns();
		String currentKeyColumnName = currentTable.getTableName() + "_ID";
		if (keys != null && keys.length > 0) {
			currentKeyColumnName = keys[0];
		}
		MColumn currentKeycolumn = currentTable.getColumn(currentKeyColumnName);
		String currentLinkColumn = "";
		if (currentTab.getParent_Column_ID() > 0) {
			currentLinkColumn = MColumn.getColumnName(Env.getCtx(), currentTab.getParent_Column_ID());
		} else if (currentTab.getAD_Column_ID() > 0) {
			currentLinkColumn = MColumn.getColumnName(Env.getCtx(), currentTab.getParent_Column_ID());
		}

		List<MTab> tabsList = Arrays.asList(
				window.getTabs(false, null)
			)
			.stream()
			.filter(tabItem -> {
				return tabItem.isActive();
			})
			.collect(Collectors.toList())
		;
		MTab parentTab = tabsList.stream()
			.filter(tab -> {
				return tab.getTabLevel() == 0;
			})
			.sorted(
				Comparator.comparing(MTab::getSeqNo)
					.thenComparing(MTab::getTabLevel)
					.reversed()
			)
			.findFirst()
			.orElse(null);
		GetZoomParentRecordResponse.Builder builder = GetZoomParentRecordResponse.newBuilder();
		if (parentTab == null) {
			return builder;
		}

		MTable table = MTable.get(Env.getCtx(), parentTab.getAD_Table_ID());
		String parentKeyColum = table.getTableName() + "_ID";
		if (Util.isEmpty(currentLinkColumn, true)) {
			currentLinkColumn = parentKeyColum;
		}

		builder.setParentTabId(
				parentTab.getAD_Tab_ID()
			)
			.setParentTabUuid(
				ValueManager.validateNull(
					parentTab.getUUID()
				)
			)
			.setKeyColumn(
				ValueManager.validateNull(
					parentKeyColum
				)
			)
			.setName(
				ValueManager.validateNull(
					parentTab.get_Translation(
						I_AD_Tab.COLUMNNAME_Name
					)
				)
			)
		;

		final String sql = "SELECT parent." + parentKeyColum + " FROM " + table.getTableName() + " AS parent "
			+ "WHERE EXISTS(SELECT 1 "
				+ "FROM " + currentTable.getTableName() + " AS child "
				+ "WHERE child." + currentLinkColumn + " = parent." + parentKeyColum
				+ " AND child." + currentKeycolumn.getColumnName() + " = ?"
			+ ")"
		;
		Object currentValue = ValueManager.getObjectFromReference(
			request.getValue(),
			currentKeycolumn.getAD_Reference_ID()
		);
		int recordId = DB.getSQLValue(null, sql, currentValue);
		if (recordId >= 0) {
			builder.setRecordId(
				recordId
			);
		}

		return builder;
	}

}
