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
package org.spin.grpc.service.form.out_bound_order;

import org.adempiere.core.domains.models.I_AD_Org;
import org.adempiere.core.domains.models.I_C_DocType;
import org.adempiere.core.domains.models.I_DD_Order;
import org.adempiere.core.domains.models.I_M_Locator;
import org.adempiere.core.domains.models.I_M_Warehouse;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MOrg;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryRulesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryViasRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentActionsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentTypesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListLocatorsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListOrganizationsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListSalesRegionsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListSalesRepresentativesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListShippersRequest;
import org.spin.backend.grpc.form.out_bound_order.ListTargetDocumentTypesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListWarehousesRequest;
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.field.field_management.FieldManagementLogic;

public class OutBoundOrderLogic {

	public static MOrg validateAndGetOrganization(int organizationId) {
		if (organizationId < 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Org_ID@");
		}
		if (organizationId == 0) {
			throw new AdempiereException("@Org0NotAllowed@");
		}
		MOrg organization = new Query(
			Env.getCtx(),
			I_AD_Org.Table_Name,
			" AD_Org_ID = ? ",
			null
		)
			.setParameters(organizationId)
			.setClient_ID()
			.first()
		;
		if (organization == null || organization.getAD_Org_ID() <= 0) {
			throw new AdempiereException("@AD_Org_ID@ @NotFound@");
		}
		if (!organization.isActive()) {
			throw new AdempiereException("@AD_Org_ID@ @NotActive@");
		}
		return organization;
	}

	public static ListLookupItemsResponse.Builder listOrganizations(ListOrganizationsRequest request) {
		// Organization filter selection
		final int columnId = 58193; // WM_InOutBound.AD_Org_ID (needed to allow org 0)
		final int validationRuleId = 130; // AD_Org Trx Security validation (Not Summary - Not 0)
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null,
			validationRuleId, null, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listWarehouses(ListWarehousesRequest request) {
		MOrg organization = validateAndGetOrganization(
			request.getOrganizationId()
		);

		// Warehouse filter selection
		final String whereClause = " M_Warehouse.AD_Org_ID = " + organization.getAD_Org_ID();
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0, 0,
			I_M_Warehouse.COLUMNNAME_M_Warehouse_ID, I_M_Warehouse.Table_Name,
			0, whereClause, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listDocumentTypes(ListDocumentTypesRequest request) {
		final String docBaseType = request.getMovementType().equals(I_DD_Order.Table_Name) ? "DOO" : "SSO";
		final String whereClause = "C_DocType.DocBaseType = '" + docBaseType + "' "
			+ "AND (C_DocType.DocSubTypeSO IS NULL OR C_DocType.DocSubTypeSO NOT IN('RM', 'OB')) "
		;
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			0,
			I_C_DocType.COLUMNNAME_C_DocType_ID, I_C_DocType.Table_Name,
			0, whereClause, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listSalesRegions(ListSalesRegionsRequest request) {
		final int columnId = 1823; // C_SalesRegion.C_SalesRegion_ID
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null,
			0, null, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listSalesRepresentatives(ListSalesRepresentativesRequest request) {
		final int columnId = 2186; // C_Order.SalesRep_ID
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null,
			0, null, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listTargetDocumentTypes(ListTargetDocumentTypesRequest request) {
		final String whereClause = "C_DocType.DocBaseType IN ('WMO') AND C_DocType.IsSOTrx = 'Y' ";
		// C_DocType.DocBaseType IN ('WMO') AND C_DocType.IsSOTrx='@IsSOTrx@' // always Y
		// Docyment Type filter selection
		// final int columnId = 58203; // WM_InOutBound.C_DocType_ID
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			0,
			I_C_DocType.COLUMNNAME_C_DocType_ID, I_C_DocType.Table_Name,
			0, whereClause, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listDeliveryRules(ListDeliveryRulesRequest request) {
		final int columnId = 58205; // WM_InOutBound.DeliveryRule
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null,
			0, null, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listDeliveryVias(ListDeliveryViasRequest request) {
		final int columnId = 2186; // WM_InOutBound.DeliveryViaRule
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null,
			0, null, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listShippers(ListShippersRequest request) {
		final int columnId = 58221; // WM_InOutBound.M_Shipper_ID
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null,
			0, null, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static ListLookupItemsResponse.Builder listDocumentActions(ListDocumentActionsRequest request) {
		final int columnId = 58208; // WM_InOutBound.DocAction
		final String whereClause = " AD_Ref_List.Value IN ('CO','PR')";
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			columnId,
			null, null,
			0, whereClause, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}



	public static MWarehouse validateAndGetWarehouse(int warehouseId) {
		if (warehouseId <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_Warehouse_ID@");
		}
		MWarehouse warehouse = new Query(
			Env.getCtx(),
			I_M_Warehouse.Table_Name,
			" M_Warehouse_ID = ? ",
			null
		)
			.setParameters(warehouseId)
			.setClient_ID()
			.first()
		;
		if (warehouse == null || warehouse.getM_Warehouse_ID() <= 0) {
			throw new AdempiereException("@M_Warehouse_ID@ @NotFound@");
		}
		if (!warehouse.isActive()) {
			throw new AdempiereException("@M_Warehouse_ID@ @NotActive@");
		}
		return warehouse;
	}

	public static ListLookupItemsResponse.Builder listLocators(ListLocatorsRequest request) {
		MWarehouse warehouse = validateAndGetWarehouse(
			request.getWarehouseId()
		);
		final String whereClause = "M_Locator.M_Warehouse_ID = " + warehouse.getM_Warehouse_ID();
		// int columnId = 64658; // WM_InOutBound.M_Locator_ID
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			0,
			I_M_Locator.COLUMNNAME_M_Locator_ID, I_M_Locator.Table_Name,
			0, whereClause, false
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			request.getContextAttributes(),
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			request.getIsOnlyActiveRecords()
		);

		return builderList;
	}

}
