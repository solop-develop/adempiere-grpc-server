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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.out_bound_order.DocumentHeader;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryRulesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryViasRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentActionsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentHeadersRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentHeadersResponse;
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
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(OutBoundOrderLogic.class);


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



	public static ListDocumentHeadersResponse.Builder listDocumentHeaders(ListDocumentHeadersRequest request) {
		final int clientId = Env.getAD_Client_ID(Env.getCtx());
		final String movementType = request.getMovementType();
		MOrg organization = validateAndGetOrganization(
			request.getOrganizationId()
		);
		MWarehouse warehouse = validateAndGetWarehouse(
			request.getWarehouseId()
		);
		final int salesRegionId = request.getSalesRegionId();
		final int salesRepresentativeId = request.getSalesRepresentativeId();
		final int documentTypeId = request.getDocumentTypeId();

		// filters
		List<Object> parametersList = new ArrayList<Object>();
		parametersList.add(clientId);
		parametersList.add(
			organization.getAD_Org_ID()
		);
		parametersList.add(
			warehouse.getM_Warehouse_ID()
		);

		StringBuffer sql = null;
		if (movementType.equals(I_DD_Order.Table_Name)) {
			//Query for Material Movement
			sql = new StringBuffer(
				"SELECT " +
				"wr.Name AS Warehouse, ord.DD_Order_ID AS ID, ord.UUID, ord.DocumentNo, " +	//	1..3
				"ord.DateOrdered, ord.DatePromised, " +
				"reg.Name AS Region, cit.Name AS City, sr.Name AS SalesRep, " +	//	4..8
				"cp.Name Partner, bploc.Name AS Location, " +	//	9..10
				"loc.Address1, loc.Address2, loc.Address3, loc.Address4, ord.C_BPartner_Location_ID, " +
				"ord.Weight, ord.Volume " +	//	11..17
				"FROM DD_Order ord " +
				"INNER JOIN DD_OrderLine lord ON(lord.DD_Order_ID = ord.DD_Order_ID) " +
				"INNER JOIN M_Product pr ON(pr.M_Product_ID = lord.M_Product_ID) " +
				"INNER JOIN C_BPartner cp ON(cp.C_BPartner_ID = ord.C_BPartner_ID) " +
				"INNER JOIN AD_User sr ON(sr.AD_User_ID = ord.SalesRep_ID) " +
				"INNER JOIN M_Locator wloc ON(wloc.M_Locator_ID = lord.M_Locator_ID) " + 
				"INNER JOIN M_Warehouse wr ON(wr.M_Warehouse_ID = wloc.M_Warehouse_ID) " +
				"INNER JOIN C_BPartner_Location bploc ON(bploc.C_BPartner_Location_ID = ord.C_BPartner_Location_ID) " +
				"INNER JOIN C_Location loc ON(loc.C_Location_ID = bploc.C_Location_ID) " +
				"LEFT JOIN C_Region reg ON(reg.C_Region_ID = loc.C_Region_ID) " +
				"LEFT JOIN C_City cit ON(cit.C_City_ID = loc.C_City_ID) " +
				"LEFT JOIN (SELECT lord.DD_OrderLine_ID, " +
				"	(COALESCE(lord.QtyOrdered, 0) - " +
				"		SUM(" +
				"				CASE WHEN (c.IsDelivered = 'N' AND lc.DD_Order_ID IS NOT NULL AND c.DocStatus = 'CO') " +
				"						THEN COALESCE(lc.MovementQty, 0) " +
				"						ELSE 0 " +
				"				END" +
				"			)" +
				"	) QtyAvailable " +
				"	FROM DD_OrderLine lord " +
				"	LEFT JOIN WM_InOutBoundLine lc ON(lc.DD_OrderLine_ID = lord.DD_OrderLine_ID) " +
				"	LEFT JOIN WM_InOutBound c ON(c.WM_InOutBound_ID = lc.WM_InOutBound_ID) " +
				"	WHERE lord.M_Product_ID IS NOT NULL " +
				"	GROUP BY lord.DD_Order_ID, lord.DD_OrderLine_ID, lord.QtyOrdered " +
				"	ORDER BY lord.DD_OrderLine_ID ASC) qafl " +
				"	ON(qafl.DD_OrderLine_ID = lord.DD_OrderLine_ID) " +
				"WHERE  wr.IsActive = 'Y' " +
				"AND ord.DocStatus = 'CO' " +
				"AND COALESCE(qafl.QtyAvailable, 0) > 0 " +
				"AND ord.AD_Client_ID=? "
			);
			// if (organization.getAD_Org_ID() > 0) {
				sql.append("AND lord.AD_Org_ID=? ");
			// }
			// if (warehouse.getM_Warehouse_ID() > 0) {
				sql.append("AND wr.M_Warehouse_ID=? ");
			// }
			if (salesRegionId > 0) {
				sql.append("AND bploc.C_SalesRegion_ID=? ");
				parametersList.add(salesRegionId);
			}
			if (salesRepresentativeId > 0) {
				sql.append("AND ord.SalesRep_ID=? ");
				parametersList.add(salesRepresentativeId);
			}
			if (documentTypeId > 0) {
				sql.append("AND ord.C_DocType_ID=? ");
				parametersList.add(documentTypeId);
			}

			//	Group By
			sql.append(
				"GROUP BY wr.Name, ord.DD_Order_ID, ord.DocumentNo, ord.DateOrdered, " +
				"ord.DatePromised, ord.Weight, ord.Volume, sr.Name, cp.Name, bploc.Name, " +
				"reg.Name, cit.Name, loc.Address1, loc.Address2, loc.Address3, loc.Address4, ord.C_BPartner_Location_ID "
			);

			//	Having
			sql.append(
				"HAVING (SUM(COALESCE(lord.QtyOrdered, 0)) - SUM(COALESCE(lord.QtyInTransit, 0)) - SUM(COALESCE(lord.QtyDelivered, 0))) > 0 "
			);

			//	Order By
			sql.append("ORDER BY ord.DD_Order_ID ASC");
		} else {//Query for Sales Order
			sql = new StringBuffer(
				"SELECT " +
				"wr.Name AS Warehouse, ord.C_Order_ID AS ID, ord.UUID, ord.DocumentNo, " +	//	1..3
				"ord.DateOrdered, ord.DatePromised, " +
				"reg.Name AS Region, cit.Name AS City, sr.Name SalesRep, " +	//	4..8
				"cp.Name Partner, bploc.Name AS Location, " +	//	9..10
				"loc.Address1, loc.Address2, loc.Address3, loc.Address4, ord.C_BPartner_Location_ID, " +
				"ord.Weight, ord.Volume " +	//	11..17
				"FROM C_Order ord " +
				"INNER JOIN C_OrderLine lord ON(lord.C_Order_ID = ord.C_Order_ID) " +
				"INNER JOIN M_Product pr ON(pr.M_Product_ID = lord.M_Product_ID) " +
				"INNER JOIN C_BPartner cp ON(cp.C_BPartner_ID = ord.C_BPartner_ID) " +
				"INNER JOIN M_Warehouse wr ON(wr.M_Warehouse_ID = ord.M_Warehouse_ID) " +
				"INNER JOIN C_BPartner_Location bploc ON(bploc.C_BPartner_Location_ID = ord.C_BPartner_Location_ID) " +
				"INNER JOIN C_Location loc ON(loc.C_Location_ID = bploc.C_Location_ID) " +
				"LEFT JOIN AD_User sr ON(sr.AD_User_ID = ord.SalesRep_ID) " +
				"LEFT JOIN C_Region reg ON(reg.C_Region_ID = loc.C_Region_ID) " +
				"LEFT JOIN C_City cit ON(cit.C_City_ID = loc.C_City_ID) " +
				"LEFT JOIN (SELECT lord.C_OrderLine_ID, " +
				"	(COALESCE(lord.QtyOrdered, 0) - " +
				"		SUM(" +
				"				CASE WHEN (c.IsDelivered = 'N' AND lc.C_Order_ID IS NOT NULL AND c.DocStatus = 'CO') " +
				"						THEN COALESCE(lc.MovementQty, 0) " +
				"						ELSE 0 " +
				"				END" +
				"			)" +
				"	) QtyAvailable " +
				"	FROM C_OrderLine lord " +
				"	LEFT JOIN WM_InOutBoundLine lc ON(lc.C_OrderLine_ID = lord.C_OrderLine_ID) " +
				"	LEFT JOIN WM_InOutBound c ON(c.WM_InOutBound_ID = lc.WM_InOutBound_ID) " +
				"	WHERE lord.M_Product_ID IS NOT NULL " +
				"	GROUP BY lord.C_Order_ID, lord.C_OrderLine_ID, lord.QtyOrdered " +
				"	ORDER BY lord.C_OrderLine_ID ASC) qafl " +
				"	ON(qafl.C_OrderLine_ID = lord.C_OrderLine_ID) " +
				"WHERE ord.IsSOTrx = 'Y' " +
				"AND wr.IsActive = 'Y' " +
				"AND ord.DocStatus = 'CO' " +
				"AND COALESCE(qafl.QtyAvailable, 0) > 0 " +
				"AND ord.AD_Client_ID=? "
			);
			// if (organization.getAD_Org_ID() > 0) {
				sql.append("AND lord.AD_Org_ID=? ");
			// }
			// if (warehouse.getM_Warehouse_ID() > 0) {
				sql.append("AND wr.M_Warehouse_ID=? ");
			// }
			if (salesRegionId > 0) {
				sql.append("AND bploc.C_SalesRegion_ID=? ");
				parametersList.add(salesRegionId);
			}
			if (salesRepresentativeId > 0) {
				sql.append("AND ord.SalesRep_ID=? ");
				parametersList.add(salesRepresentativeId);
			}
			if (documentTypeId > 0) {
				sql.append("AND ord.C_DocType_ID=? ");
				parametersList.add(documentTypeId);
			}

			//	Group By
			sql.append(
				"GROUP BY wr.Name, ord.C_Order_ID, ord.DocumentNo, ord.DateOrdered, " +
				"ord.DatePromised, ord.Weight, ord.Volume, sr.Name, cp.Name, bploc.Name, " +
				"reg.Name, cit.Name, loc.Address1, loc.Address2, loc.Address3, loc.Address4, ord.C_BPartner_Location_ID "
			);

			//	Having
			sql.append(
				"HAVING (SUM(COALESCE(lord.QtyOrdered, 0)) - SUM(COALESCE(lord.QtyDelivered, 0))) > 0 "
			);

			//	Order By
			sql.append("ORDER BY ord.C_Order_ID ASC");
		}

		AtomicInteger count = new AtomicInteger(0);
		ListDocumentHeadersResponse.Builder builderList = ListDocumentHeadersResponse.newBuilder();
		DB.runResultSet(null, sql.toString(), parametersList, resultSet -> {
			while (resultSet.next()) {
				count.incrementAndGet();
				DocumentHeader.Builder builder = OutBoundOrderConvertUtil.convertDocumentHeader(resultSet);
				builderList.addRecords(builder);
			}
		})
		.onFailure(throwable -> {
			log.severe(throwable.getMessage());
		});

		builderList.setRecordCount(
			count.get()
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
