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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Org;
import org.adempiere.core.domains.models.I_C_DocType;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_DD_Order;
import org.adempiere.core.domains.models.I_M_Locator;
import org.adempiere.core.domains.models.I_M_Warehouse;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DocTypeNotFoundException;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.eevolution.distribution.model.MDDOrderLine;
import org.eevolution.wms.model.MWMInOutBound;
import org.eevolution.wms.model.MWMInOutBoundLine;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.form.out_bound_order.DocumentHeader;
import org.spin.backend.grpc.form.out_bound_order.DocumentLine;
import org.spin.backend.grpc.form.out_bound_order.GenerateLoadOrderRequest;
import org.spin.backend.grpc.form.out_bound_order.GenerateLoadOrderResponse;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryRulesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDeliveryViasRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentActionsRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentHeadersRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentHeadersResponse;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentLinesRequest;
import org.spin.backend.grpc.form.out_bound_order.ListDocumentLinesResponse;
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
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

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
		// final int columnId = 58193; // WM_InOutBound.AD_Org_ID (needed to allow org 0)
		final int validationRuleId = 130; // AD_Org Trx Security validation (Not Summary - Not 0)
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			0,
			0, 0, 0,
			0, // columnId,
			I_AD_Org.COLUMNNAME_AD_Org_ID, I_AD_Org.Table_Name,
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
		final String whereClause = " M_Warehouse.IsActive = 'Y' "
			+ "AND M_Warehouse.AD_Org_ID = " + organization.getAD_Org_ID()
		;
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
		final String docBaseType = request.getMovementType().equals(I_DD_Order.Table_Name) ? "DOO" : "SOO";
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
		final String movementType = request.getMovementType();
		if (Util.isEmpty(movementType, true) ||
			!(movementType.equals(I_DD_Order.Table_Name) || movementType.equals(I_C_Order.Table_Name))
		 ) {
			throw new AdempiereException("@FillMandatory@ @MovementType@");
		}
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
		final int clientId = Env.getAD_Client_ID(Env.getCtx());
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
				"wr.M_Warehouse_ID, wr.Name AS Warehouse, ord.DD_Order_ID AS ID, ord.UUID, ord.DocumentNo, " +	//	1..3
				"ord.DateOrdered, ord.DatePromised, " +
				"reg.Name AS Region, cit.Name AS City, ord.SalesRep_ID, sr.Name AS SalesRep, " +	//	4..8
				"cp.C_BPartner_ID, COALESCE(cp.Value, '') AS PartnerValue, cp.Name Partner, bploc.Name AS Location, " +	//	9..10
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
				"WHERE wr.IsActive = 'Y' " +
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
				"GROUP BY wr.M_Warehouse_ID, wr.Name, ord.DD_Order_ID, ord.DocumentNo, ord.DateOrdered, " +
				"ord.DatePromised, ord.Weight, ord.Volume, sr.Name, cp.C_BPartner_ID, cp.Value, cp.Name, bploc.Name, " +
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
				"wr.M_Warehouse_ID, wr.Name AS Warehouse, ord.C_Order_ID AS ID, ord.UUID, ord.DocumentNo, " +	//	1..3
				"ord.DateOrdered, ord.DatePromised, " +
				"reg.Name AS Region, cit.Name AS City, ord.SalesRep_ID, sr.Name AS SalesRep, " +	//	4..8
				"cp.C_BPartner_ID, COALESCE(cp.Value, '') AS PartnerValue, cp.Name Partner, bploc.Name AS Location, " +	//	9..10
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
				"GROUP BY wr.M_Warehouse_ID, wr.Name, ord.C_Order_ID, ord.DocumentNo, ord.DateOrdered, " +
				"ord.DatePromised, ord.Weight, ord.Volume, sr.Name, cp.C_BPartner_ID, cp.Value, cp.Name, bploc.Name, " +
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

	public static ListDocumentLinesResponse.Builder listDocumentLines(ListDocumentLinesRequest request) {
		final String movementType = request.getMovementType();
		if (Util.isEmpty(movementType, true) ||
			!(movementType.equals(I_DD_Order.Table_Name) || movementType.equals(I_C_Order.Table_Name))
		 ) {
			throw new AdempiereException("@FillMandatory@ @MovementType@");
		}

		if (request.getHeaderIdsList() == null || request.getHeaderIdsList().isEmpty()) {
			throw new AdempiereException("@FillMandatory@ @Record_ID@");
		}
		String headerIdentifers = request.getHeaderIdsList()
			.stream()
			// .map(String::valueOf)
			.map(String::valueOf)
			.collect(Collectors.joining(", "))
		;
		if (Util.isEmpty(headerIdentifers, true)) {
			headerIdentifers = "0";
		}

		StringBuffer sql = null;
		if (movementType.equals(I_DD_Order.Table_Name)) {
			StringBuffer sqlWhere = new StringBuffer("ord.DD_Order_ID IN(")
				.append(headerIdentifers)
				.append(")")
			;

			sql = new StringBuffer(
				"SELECT alm.M_Warehouse_ID, alm.Name Warehouse, " +
					"lord.DD_OrderLine_ID AS ID, lord.UUID, lord.DD_Order_ID AS Parent_ID, ord.DocumentNo, " +
					"lord.M_Product_ID, COALESCE(pro.Value, '') AS ProductValue, " +
					"(pro.Name || (CASE " + //
					"        WHEN attr.ProductAttribute IS NOT NULL AND attr.ProductAttribute <> '' " +
					"        THEN ' - ' || attr.ProductAttribute " +
					"        ELSE '' " +
					"    END)) AS Product, " +
					"pro.C_UOM_ID, uomp.UOMSymbol, s.QtyOnHand, " +
					"lord.QtyOrdered, lord.C_UOM_ID Order_UOM_ID, uom.UOMSymbol Order_UOMSymbol, lord.QtyReserved, 0 QtyInvoiced, lord.QtyDelivered, " +
					"SUM(" +
					"		COALESCE(CASE " +
					"			WHEN (c.IsDelivered = 'N' AND lc.DD_OrderLine_ID IS NOT NULL AND c.DocStatus = 'CO') " +
					"			THEN lc.MovementQty " +
					"			ELSE 0 " +
					"		END, 0)" +
					") QtyLoc, " +
					"(COALESCE(lord.QtyOrdered, 0) - COALESCE(lord.QtyInTransit, 0) - COALESCE(lord.QtyDelivered, 0) - " +
					"	SUM(" +
					"		COALESCE(CASE " +
					"			WHEN (c.IsDelivered = 'N' AND lc.DD_OrderLine_ID IS NOT NULL AND c.DocStatus = 'CO') " +
					"			THEN lc.MovementQty " +
					"			ELSE 0 " +
					"		END, 0)" +
					"		)" +
					") Qty, " +
					"pro.Weight, pro.Volume, ord.DeliveryRule, pro.IsStocked " +
					"FROM DD_Order ord " +
					"INNER JOIN DD_OrderLine lord ON(lord.DD_Order_ID = ord.DD_Order_ID) " +
					"LEFT JOIN " +
					"    (SELECT lord.M_AttributeSetInstance_ID, productattribute(lord.M_AttributeSetInstance_ID) AS ProductAttribute " +
					"    FROM DD_OrderLine lord) AS attr " +
					"    ON lord.M_AttributeSetInstance_ID = attr.M_AttributeSetInstance_ID " +
					"INNER JOIN M_Locator l ON(l.M_Locator_ID = lord.M_Locator_ID) " + 
					"INNER JOIN M_Warehouse alm ON(alm.M_Warehouse_ID = l.M_Warehouse_ID) " +
					"INNER JOIN M_Product pro ON(pro.M_Product_ID = lord.M_Product_ID) " +
					"INNER JOIN C_UOM uom ON(uom.C_UOM_ID = lord.C_UOM_ID) " +
					"INNER JOIN C_UOM uomp ON(uomp.C_UOM_ID = pro.C_UOM_ID) " +
					"LEFT JOIN WM_InOutBoundLine lc ON(lc.DD_OrderLine_ID = lord.DD_OrderLine_ID) " +
					"LEFT JOIN WM_InOutBound c ON(c.WM_InOutBound_ID = lc.WM_InOutBound_ID) " +
					"LEFT JOIN (" +
					"				SELECT l.M_Warehouse_ID, st.M_Product_ID, " +
					"					COALESCE(SUM(st.QtyOnHand), 0) QtyOnHand, " +
					"					(CASE WHEN p.M_AttributeSet_ID IS NOT NULL THEN COALESCE(st.M_AttributeSetInstance_ID, 0) ELSE 0 END) M_AttributeSetInstance_ID " +
					"				FROM M_Storage st " +
					"				INNER JOIN M_Product p ON(p.M_Product_ID = st.M_Product_ID) " +
					"				INNER JOIN M_Locator l ON(l.M_Locator_ID = st.M_Locator_ID) " +
					"			GROUP BY l.M_Warehouse_ID, st.M_Product_ID, p.M_AttributeSet_ID, 4) s " +
					"														ON(s.M_Product_ID = lord.M_Product_ID " +
					"																AND s.M_Warehouse_ID = l.M_Warehouse_ID " +
					"																AND lord.M_AttributeSetInstance_ID = s.M_AttributeSetInstance_ID) ")
				.append("WHERE ")
				.append(sqlWhere).append(" ")
			;
			//	Group By
			sql.append("GROUP BY alm.M_Warehouse_ID, lord.DD_Order_ID, lord.DD_OrderLine_ID, " +
					"alm.Name, ord.DocumentNo, lord.M_Product_ID, lord.M_AttributeSetInstance_ID, attr.ProductAttribute, " + 
					"pro.Value, pro.Name, lord.C_UOM_ID, uom.UOMSymbol, lord.QtyEntered, " +
					"pro.C_UOM_ID, uomp.UOMSymbol, lord.QtyOrdered, lord.QtyReserved, " +
					"lord.QtyDelivered, pro.Weight, pro.Volume, ord.DeliveryRule, s.QtyOnHand,pro.IsStocked"
				)
				.append(" ")
			;
			//	Having
			sql.append("HAVING (COALESCE(lord.QtyOrdered, 0) - COALESCE(lord.QtyInTransit, 0) - COALESCE(lord.QtyDelivered, 0) - " + 
					"								SUM(" +
					"									COALESCE(CASE " +
					"										WHEN (c.IsDelivered = 'N' AND lc.DD_OrderLine_ID IS NOT NULL AND c.DocStatus = 'CO') " +
					"											THEN lc.MovementQty " +
					"											ELSE 0 " +
					"										END, 0)" +
					"								)" +
					"			) > 0 OR pro.IsStocked = 'N' "
				)
				.append(" ")
			;
			//	Order By
			sql.append("ORDER BY lord.DD_Order_ID ASC");
			
		} else {
			StringBuffer sqlWhere = new StringBuffer("ord.C_Order_ID IN(")
				.append(headerIdentifers)
				.append(")")
			;

			sql = new StringBuffer(
					"SELECT lord.M_Warehouse_ID, alm.Name Warehouse, " +
					"lord.C_OrderLine_ID AS ID, lord.UUID, lord.C_Order_ID AS Parent_ID, ord.DocumentNo, " +
					"lord.M_Product_ID, COALESCE(pro.Value, '') AS ProductValue, " +
					"(pro.Name || (CASE " + //
					"        WHEN attr.ProductAttribute IS NOT NULL AND attr.ProductAttribute <> '' " +
					"        THEN ' - ' || attr.ProductAttribute " +
					"        ELSE '' " +
					"    END)) AS Product, " +
					"pro.C_UOM_ID, uomp.UOMSymbol, s.QtyOnHand, " +
					"lord.QtyOrdered, lord.C_UOM_ID Order_UOM_ID, uom.UOMSymbol Order_UOMSymbol, lord.QtyReserved, lord.QtyInvoiced, lord.QtyDelivered, " +
					"SUM(" +
					"		COALESCE(CASE " +
					"			WHEN (c.IsDelivered = 'N' AND lc.C_OrderLine_ID IS NOT NULL AND c.DocStatus = 'CO') " +
					"			THEN lc.MovementQty - COALESCE(iol.MovementQty, 0) " +
					"			ELSE 0 " +
					"		END, 0)" +
					") QtyLoc, " +
					"(COALESCE(lord.QtyOrdered, 0) - COALESCE(lord.QtyDelivered, 0) - " +
					"	SUM(" +
					"		COALESCE(CASE " +
					"			WHEN (c.IsDelivered = 'N' AND lc.C_OrderLine_ID IS NOT NULL AND c.DocStatus = 'CO') " +
					"			THEN lc.MovementQty - COALESCE(iol.MovementQty, 0) " +
					"			ELSE 0 " +
					"		END, 0)" +
					"		)" +
					") Qty, " +
					"pro.Weight, pro.Volume, ord.DeliveryRule, pro.IsStocked " +
					"FROM C_Order ord " +
					"INNER JOIN C_OrderLine lord ON(lord.C_Order_ID = ord.C_Order_ID) " +
					"LEFT JOIN " +
					"    (SELECT lord.M_AttributeSetInstance_ID, productattribute(lord.M_AttributeSetInstance_ID) AS ProductAttribute " +
					"    FROM C_OrderLine lord) AS attr " +
					"    ON lord.M_AttributeSetInstance_ID = attr.M_AttributeSetInstance_ID " +
					"INNER JOIN M_Warehouse alm ON(alm.M_Warehouse_ID = lord.M_Warehouse_ID) " +
					"INNER JOIN M_Product pro ON(pro.M_Product_ID = lord.M_Product_ID) " +
					"INNER JOIN C_UOM uom ON(uom.C_UOM_ID = lord.C_UOM_ID) " +
					"INNER JOIN C_UOM uomp ON(uomp.C_UOM_ID = pro.C_UOM_ID) " +
					"LEFT JOIN WM_InOutBoundLine lc ON(lc.C_OrderLine_ID = lord.C_OrderLine_ID) " +
					"LEFT JOIN WM_InOutBound c ON(c.WM_InOutBound_ID = lc.WM_InOutBound_ID) " +
					"LEFT JOIN (SELECT iol.WM_InOutBoundLine_ID, SUM(iol.MovementQty) MovementQty "
					+ "						FROM M_InOut io "
					+ "						INNER JOIN M_InOutLine iol ON(iol.M_InOut_ID = io.M_InOut_ID) "
					+ "						WHERE io.DocStatus IN('CO', 'CL') "
					+ "						AND iol.WM_InOutBoundLine_ID IS NOT NULL"
					+ "				GROUP BY iol.WM_InOutBoundLine_ID) iol ON(iol.WM_InOutBoundLine_ID = lc.WM_InOutBoundLine_ID) " +
					"LEFT JOIN (" +
					"				SELECT l.M_Warehouse_ID, st.M_Product_ID, " +
					"					COALESCE(SUM(st.QtyOnHand), 0) QtyOnHand, " +
					"					(CASE WHEN p.M_AttributeSet_ID IS NOT NULL THEN COALESCE(st.M_AttributeSetInstance_ID, 0) ELSE 0 END) M_AttributeSetInstance_ID " +
					"				FROM M_Storage st " +
					"				INNER JOIN M_Product p ON(p.M_Product_ID = st.M_Product_ID) " + 
					"				INNER JOIN M_Locator l ON(l.M_Locator_ID = st.M_Locator_ID) " +
					"			GROUP BY l.M_Warehouse_ID, st.M_Product_ID, p.M_AttributeSet_ID, 4) s " +
					"														ON(s.M_Product_ID = lord.M_Product_ID " +
					"																AND s.M_Warehouse_ID = lord.M_Warehouse_ID " +
					"																AND lord.M_AttributeSetInstance_ID = s.M_AttributeSetInstance_ID) "
				)
				.append("WHERE ")
				.append(sqlWhere).append(" ")
			;
			//	Group By
			sql.append(
					"GROUP BY lord.M_Warehouse_ID, lord.C_Order_ID, lord.C_OrderLine_ID, " +
					"alm.Name, ord.DocumentNo, lord.M_Product_ID, lord.M_AttributeSetInstance_ID, attr.ProductAttribute, " + 
					"pro.Value, pro.Name, lord.C_UOM_ID, uom.UOMSymbol, lord.QtyEntered, " +
					"pro.C_UOM_ID, uomp.UOMSymbol, lord.QtyOrdered, lord.QtyReserved, " + 
					"lord.QtyDelivered, lord.QtyInvoiced, pro.Weight, pro.Volume, ord.DeliveryRule, s.QtyOnHand, pro.IsStocked"
				)
				.append(" ")
			;
			//	Having
			sql.append(
					"HAVING (COALESCE(lord.QtyOrdered, 0) - COALESCE(lord.QtyDelivered, 0) - " + 
					"									SUM(" +
					"										COALESCE(CASE " +
					"											WHEN (c.IsDelivered = 'N' AND lc.C_OrderLine_ID IS NOT NULL AND c.DocStatus = 'CO') " +
					"											THEN lc.MovementQty - COALESCE(iol.MovementQty, 0) " +
					"											ELSE 0 " +
					"										END, 0)" +
					"									)" +
					"			) > 0  OR pro.IsStocked = 'N' "
				)
				.append(" ")
			;
			//	Order By
			sql.append("ORDER BY lord.C_Order_ID ASC");
			
		}

		List<Object> parametersList = new ArrayList<Object>();
		AtomicInteger count = new AtomicInteger(0);
		ListDocumentLinesResponse.Builder builderList = ListDocumentLinesResponse.newBuilder();
		DB.runResultSet(null, sql.toString(), parametersList, resultSet -> {
			while (resultSet.next()) {
				DocumentLine.Builder builder = OutBoundOrderConvertUtil.convertDocumentLine(resultSet);
				if (builder.getId() <= 0) {
					// delivery rule no is manual or force and quantity is zero
					continue;
				}

				count.incrementAndGet();
				builderList.addRecords(builder);
			}
		})
		.onFailure(throwable -> {
			throwable.printStackTrace();
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
		final int columnId = 58206; // WM_InOutBound.DeliveryViaRule
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

	/**
	 * Get Default locator based on stock, else default
	 * @param warehouseId
	 * @param productId
	 * @param attributeSetInstanceId
	 * @param quantity
	 * @param transactionName
	 * @return
	 * @return int
	 */
	private static int getDefaultLocator(int warehouseId, int productId, int attributeSetInstanceId, BigDecimal quantity, String transactionName) {
		int locatorId = MStorage.getM_Locator_ID(
			warehouseId,
			productId,
			attributeSetInstanceId,
			quantity,
			transactionName
		);
		if(locatorId > 0) {
			return locatorId;
		}
		MWarehouse warehouse = MWarehouse.get(Env.getCtx(), warehouseId);
		MLocator locator = MLocator.getDefault(warehouse);
		if(locator == null) {
			MProduct product = MProduct.get(Env.getCtx(), productId);
			throw new AdempiereException("@M_Locator_ID@ @NotFound@ [@M_Product_ID@: " + product.getValue() + " - " + product.getName() + " @M_Warehouse_ID@: " + warehouse.getName() + "]");
		}
		return locator.getM_Locator_ID();
	}
	public static GenerateLoadOrderResponse.Builder generateLoadOrder(GenerateLoadOrderRequest request) {
		final String movementType = request.getMovementType();
		if (Util.isEmpty(movementType, true) ||
			!(movementType.equals(I_DD_Order.Table_Name) || movementType.equals(I_C_Order.Table_Name))
		 ) {
			throw new AdempiereException("@FillMandatory@ @MovementType@");
		}

		MOrg organization = validateAndGetOrganization(
			request.getOrganizationId()
		);
		MWarehouse warehouse = validateAndGetWarehouse(
			request.getWarehouseId()
		);

		Timestamp documentDate = ValueManager.getDateFromTimestampDate(
			request.getDocumentDate()
		);
		if (documentDate == null) {
			throw new AdempiereException("@FillMandatory@ @DocumentDate@");
		}

		Timestamp shipmentDate = ValueManager.getDateFromTimestampDate(
			request.getDocumentDate()
		);
		if (shipmentDate == null) {
			throw new AdempiereException("@FillMandatory@ @ShipDate@");
		}

		if (request.getLinesList() == null || request.getLinesList().isEmpty()) {
			throw new AdempiereException("@NoLines@");
		}

		GenerateLoadOrderResponse.Builder builder = GenerateLoadOrderResponse.newBuilder();
		AtomicInteger linesQuantity = new AtomicInteger();
		Trx.run(transactionName -> {
			AtomicReference<BigDecimal> totalWeightReference = new AtomicReference<BigDecimal>(
				Env.ZERO
			);
			AtomicReference<BigDecimal> totalVolumeReference = new AtomicReference<BigDecimal>(
				Env.ZERO
			);
			// BigDecimal totalWeight = Env.ZERO;
			// BigDecimal totalVolume = Env.ZERO;

			MWMInOutBound outBoundOrder = new MWMInOutBound(Env.getCtx(), 0, transactionName);
			outBoundOrder.setAD_Org_ID(
				organization.getAD_Org_ID()
			);
			//	Set Warehouse
			outBoundOrder.setM_Warehouse_ID(
				warehouse.getM_Warehouse_ID()
			);
			outBoundOrder.setDateTrx(documentDate);
			outBoundOrder.setPickDate(documentDate);
			outBoundOrder.setShipDate(shipmentDate);

			//	Set Document Type
			int targetDocumentTypeId = request.getTargetDocumentTypeId();
			if (targetDocumentTypeId > 0) {
				outBoundOrder.setC_DocType_ID(targetDocumentTypeId);
			} else {
				Optional<MDocType> defaultDocumentType = Arrays.asList(
						MDocType.getOfDocBaseType(
							Env.getCtx(),
							MDocType.DOCBASETYPE_WarehouseManagementOrder
						)
					)
					.stream()
					.filter(documentType -> documentType.isSOTrx())
					.findFirst()
				;

				if (!defaultDocumentType.isPresent()) {
					throw new DocTypeNotFoundException(MDocType.DOCBASETYPE_WarehouseManagementOrder, "");
				}
				outBoundOrder.setC_DocType_ID(defaultDocumentType.get().getC_DocType_ID());
				targetDocumentTypeId = defaultDocumentType.get().getC_DocType_ID();
			}

			//	Delivery Rule
			if (!Util.isEmpty(request.getDeliveryRule(), true)) {
				outBoundOrder.setDeliveryRule(
					request.getDeliveryRule()
				);
			}
			//	Delivery Via Rule
			if (!Util.isEmpty(request.getDeliveryVia())) {
				outBoundOrder.setDeliveryViaRule(
					request.getDeliveryVia()
				);
			}
			//	Set Shipper
			if (request.getShipperId() > 0) {
				outBoundOrder.setM_Shipper_ID(
					request.getShipperId()
				);
			}

			//	Save Order
			outBoundOrder.setDocStatus(MWMInOutBound.DOCSTATUS_Drafted);
			outBoundOrder.setIsSOTrx(true);
			outBoundOrder.saveEx();

			request.getLinesList().forEach(requestLine -> {
				int orderLineId = requestLine.getId();
				int productId = requestLine.getProductId();
				BigDecimal quantity = NumberManager.getBigDecimalFromString(
					requestLine.getQuantity()
				);
				BigDecimal weight = NumberManager.getBigDecimalFromString(
					requestLine.getWeight()
				);
				BigDecimal volume = NumberManager.getBigDecimalFromString(
					requestLine.getVolume()
				);

				//	New Line
				MWMInOutBoundLine outBoundOrderLine = new MWMInOutBoundLine(outBoundOrder);
				outBoundOrderLine.setAD_Org_ID(
					organization.getAD_Org_ID()
				);
				MProduct product = MProduct.get(Env.getCtx(), productId);
				if (movementType.equals(I_DD_Order.Table_Name)) {
					outBoundOrderLine.setDD_OrderLine_ID(orderLineId);
					MDDOrderLine line = new MDDOrderLine(Env.getCtx(), orderLineId, transactionName);
					outBoundOrderLine.setDD_Order_ID(line.getDD_Order_ID());
					outBoundOrderLine.setDD_OrderLine_ID(line.getDD_OrderLine_ID());
					outBoundOrderLine.setM_AttributeSetInstance_ID(line.getM_AttributeSetInstance_ID());
					outBoundOrderLine.setC_UOM_ID(product.getC_UOM_ID());
					outBoundOrderLine.setM_Locator_ID(line.getM_Locator_ID());
					outBoundOrderLine.setM_LocatorTo_ID(line.getM_LocatorTo_ID());
				} else {
					outBoundOrderLine.setC_OrderLine_ID(orderLineId);
					MOrderLine line = new MOrderLine(Env.getCtx(), orderLineId, transactionName);
					outBoundOrderLine.setC_Order_ID(line.getC_Order_ID());
					outBoundOrderLine.setC_OrderLine_ID(line.getC_OrderLine_ID());
					outBoundOrderLine.setM_AttributeSetInstance_ID(line.getM_AttributeSetInstance_ID());
					outBoundOrderLine.setC_UOM_ID(product.getC_UOM_ID());

					int locatorId = request.getLocatorId();
					if (locatorId <= 0) {
						locatorId = getDefaultLocator(
							line.getM_Warehouse_ID(),
							productId,
							line.getM_AttributeSetInstance_ID(),
							quantity,
							transactionName
						);
					}

					outBoundOrderLine.setM_LocatorTo_ID(locatorId);
				}
				outBoundOrderLine.setM_Product_ID(productId);
				outBoundOrderLine.setMovementQty(quantity);
				outBoundOrderLine.setPickedQty(quantity);
				
				//	Add Weight
				BigDecimal totalWeight = totalWeightReference.get().add(weight);
				totalWeightReference.set(totalWeight);
				//	Add Volume
				BigDecimal totalVolume = totalVolumeReference.get().add(volume);
				totalVolumeReference.set(totalVolume);
				//	Save Line
				outBoundOrderLine.saveEx();
				//	Add count
				linesQuantity.incrementAndGet();
			});

			//	Set Header Weight
			outBoundOrder.setWeight(
				totalWeightReference.get()
			);
			//	Set Header Volume
			outBoundOrder.setVolume(
				totalVolumeReference.get()
			);
			//	Save Header
			outBoundOrder.saveEx();

			//	Validate Document Action
			String documentAction = request.getDocumentAction();
			if(Util.isEmpty(documentAction)) {
				documentAction = MWMInOutBound.DOCACTION_Complete;
			}
			//	Complete Order
			outBoundOrder.setDocAction(documentAction);
			outBoundOrder.processIt(documentAction);
			outBoundOrder.saveEx();

			//	Valid Error
			String errorMessage = outBoundOrder.getProcessMsg();
			if (errorMessage != null && outBoundOrder.getDocStatus().equals(MWMInOutBound.DOCSTATUS_Invalid)) {
				throw new AdempiereException(errorMessage);
			}

			String message = Msg.parseTranslation(
				Env.getCtx(),
				"@Created@ = [" + outBoundOrder.getDocumentNo()
				+ "] || @LineNo@" + " = [" + linesQuantity.get() + "]"
				+ (errorMessage != null ? "\n@Errors@:" + errorMessage: "")
			);
			builder.setRecordCount(
					linesQuantity.get()
				)
				.setDocumentNo(
					StringManager.getValidString(
						outBoundOrder.getDocumentNo()
					)
				)
				.setMessage(
					StringManager.getValidString(
						message
					)
				)
			;
		});

		return builder;
	}

}
