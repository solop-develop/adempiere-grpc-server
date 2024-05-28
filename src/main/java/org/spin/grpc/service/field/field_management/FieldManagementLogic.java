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
import java.util.List;
import java.util.Properties;

import org.compiere.model.MLookupInfo;
import org.compiere.model.MTable;
import org.compiere.util.Env;
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
}
