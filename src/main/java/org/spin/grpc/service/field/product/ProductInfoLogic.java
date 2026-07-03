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
package org.spin.grpc.service.field.product;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_M_AttributeSet;
import org.adempiere.core.domains.models.I_M_AttributeSetInstance;
import org.adempiere.core.domains.models.I_M_PriceList;
import org.adempiere.core.domains.models.I_M_PriceList_Version;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_Product_Category;
import org.adempiere.core.domains.models.I_M_Product_Class;
import org.adempiere.core.domains.models.I_M_Product_Classification;
import org.adempiere.core.domains.models.I_M_Product_Group;
import org.adempiere.core.domains.models.I_M_Product_PO;
import org.adempiere.core.domains.models.I_M_Warehouse;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCurrency;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPO;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.field.product.AvailableToPromise;
import org.spin.backend.grpc.field.product.ExportProductsInfoRequest;
import org.spin.backend.grpc.field.product.ExportProductsInfoResponse;
import org.spin.backend.grpc.field.product.GetLastPriceListVersionRequest;
import org.spin.backend.grpc.field.product.GetProductInfoRequest;
import org.spin.backend.grpc.field.product.ListAttributeSetInstancesRequest;
import org.spin.backend.grpc.field.product.ListAttributeSetsRequest;
import org.spin.backend.grpc.field.product.ListAvailableToPromisesRequest;
import org.spin.backend.grpc.field.product.ListAvailableToPromisesResponse;
import org.spin.backend.grpc.field.product.ListPricesListVersionsRequest;
import org.spin.backend.grpc.field.product.ListProductCategoriesRequest;
import org.spin.backend.grpc.field.product.ListProductClasessRequest;
import org.spin.backend.grpc.field.product.ListProductClassificationsRequest;
import org.spin.backend.grpc.field.product.ListProductGroupsRequest;
import org.spin.backend.grpc.field.product.ListProductsInfoRequest;
import org.spin.backend.grpc.field.product.ListProductsInfoResponse;
import org.spin.backend.grpc.field.product.ListRelatedProductsRequest;
import org.spin.backend.grpc.field.product.ListRelatedProductsResponse;
import org.spin.backend.grpc.field.product.ListSubstituteProductsRequest;
import org.spin.backend.grpc.field.product.ListSubstituteProductsResponse;
import org.spin.backend.grpc.field.product.ListVendorPurchasesRequest;
import org.spin.backend.grpc.field.product.ListVendorPurchasesResponse;
import org.spin.backend.grpc.field.product.ListVendorsRequest;
import org.spin.backend.grpc.field.product.ListWarehouseStocksRequest;
import org.spin.backend.grpc.field.product.ListWarehouseStocksResponse;
import org.spin.backend.grpc.field.product.ListWarehousesRequest;
import org.spin.backend.grpc.field.product.ProductInfo;
import org.spin.backend.grpc.field.product.RelatedProduct;
import org.spin.backend.grpc.field.product.SubstituteProduct;
import org.spin.backend.grpc.field.product.VendorPurchase;
import org.spin.backend.grpc.field.product.WarehouseStock;
import org.spin.base.db.WhereClauseUtil;
import org.spin.eca62.util.S3Manager;
import org.spin.base.util.ContextManager;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.field.field_management.FieldManagementLogic;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.CountUtil;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.db.ParameterUtil;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

public class ProductInfoLogic {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(ProductInfoLogic.class);

	public final static String Table_Name = I_M_Product.Table_Name;

	/** Supported export output formats (case-insensitive) */
	public static final String EXPORT_FORMAT_XLSX = "xlsx";


	public static ListLookupItemsResponse.Builder listWarehouses(ListWarehousesRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_Warehouse.COLUMNNAME_M_Warehouse_ID, I_M_Warehouse.Table_Name,
			0,
			" M_Warehouse.M_Warehouse_ID > 0 "
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}


	public static LookupItem.Builder getLastPriceListVersion(GetLastPriceListVersionRequest request) {
		int priceListId = request.getPriceListId();
		if (priceListId <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_PriceList_ID@");
		}

		MPriceList priceList = MPriceList.get(Env.getCtx(), priceListId, null);
		if (priceList == null || priceList.getM_PriceList_ID() <= 0) {
			throw new AdempiereException("@M_PriceList_ID@ @NotFound@");
		}

		//	Ordered Date
		Timestamp validPriceDate = TimeManager.getTimestampFromProtoTimestamp(
			request.getDateOrdered()
		);
		//	Invocied Date
		if (validPriceDate == null) {
			validPriceDate = TimeManager.getTimestampFromProtoTimestamp(
				request.getDateInvoiced()
			);
		}
		//	Today
		if (validPriceDate == null) {
			validPriceDate = new Timestamp(
				System.currentTimeMillis()
			);
		}

		// NOT USE, chache with loaded price list version into `m_plv` variable
		// MPriceListVersion priceListVersion = priceList.getPriceListVersion(priceDate);

		final String whereClause = "M_PriceList_ID = ? AND TRUNC(ValidFrom, 'DD') <= ?";
		MPriceListVersion priceListVersion = new Query(
			Env.getCtx(),
			I_M_PriceList_Version.Table_Name,
			whereClause,
			null
		)
			.setParameters(priceList.getM_PriceList_ID(), validPriceDate)
			.setOnlyActiveRecords(true)
			.setOrderBy("ValidFrom DESC")
			.first()
		;

		LookupItem.Builder builder = LookupItem.newBuilder()
			.setTableName(
				I_M_PriceList_Version.Table_Name
			)
		;
		if (priceListVersion == null || priceListVersion.getM_PriceList_Version_ID() <= 0) {
			return builder;
		}
		builder = LookupUtil.convertObjectFromResult(
			priceListVersion.getM_PriceList_Version_ID(),
			priceListVersion.getUUID(),
			null,
			priceListVersion.getDisplayValue(),
			priceListVersion.isActive()
		);
		builder.setTableName(
			I_M_PriceList_Version.Table_Name
		);

		return builder;
	}

	public static ListLookupItemsResponse.Builder listPricesListVersions(ListPricesListVersionsRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_PriceList_Version.COLUMNNAME_M_PriceList_Version_ID, I_M_PriceList_Version.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listAttributeSets(ListAttributeSetsRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_AttributeSet.COLUMNNAME_M_AttributeSet_ID, I_M_AttributeSet.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listAttributeSetInstances(ListAttributeSetInstancesRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.PAttribute,
			0, 0, 0,
			0,
			I_M_AttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID, I_M_AttributeSetInstance.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listProductCategories(ListProductCategoriesRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_Product_Category.COLUMNNAME_M_Product_Category_ID, I_M_Product_Category.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listProductGroups(ListProductGroupsRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_Product_Group.COLUMNNAME_M_Product_Group_ID, I_M_Product_Group.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listProductClasses(ListProductClasessRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_Product_Class.COLUMNNAME_M_Product_Class_ID, I_M_Product_Class.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listProductClassifications(ListProductClassificationsRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_Product_Classification.COLUMNNAME_M_Product_Classification_ID, I_M_Product_Classification.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listVendors(ListVendorsRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.Search,
			0, 0, 0,
			0,
			I_M_Product_PO.COLUMNNAME_C_BPartner_ID, I_M_Product_PO.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = FieldManagementLogic.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}


	/**
	 * 	System has Unconfirmed records
	 *	@return true if unconfirmed
	 */
	private static boolean isUnconfirmed() {
		final int clientId = Env.getAD_Client_ID(Env.getCtx());
		int no = DB.getSQLValue(
			null,
			"SELECT 1 FROM M_InOutLineConfirm WHERE AD_Client_ID = ? LIMIT 1",
			clientId
		);
		if (no > 0) {
			return true;
		}
		no = DB.getSQLValue(
			null,
			"SELECT 1 FROM M_MovementLineConfirm WHERE AD_Client_ID = ? LIMIT 1",
			clientId
		);
		return no > 0;
	}


	public static ProductInfo.Builder getProductInfo(GetProductInfoRequest request) {
		//  Fill Cintext
		Properties context = Env.getCtx();

		String sqlQuery = "SELECT "
			+ "p.M_Product_ID, p.UUID, p.Discontinued, "
			+ "p.IsStocked AS IsStocked, "
			+ "pc.Name AS M_Product_Category_ID, "
			+ "pcl.Name AS M_Product_Class_ID, "
			+ "pcls.Name AS M_Product_Classification_ID, "
			+ "pg.Name AS M_Product_Group_ID, "
			+ "p.Value, p.Name, p.UPC, p.SKU, p.IsActive, "
			+ "u.Name AS C_UOM_ID, "
			+ "pa.IsInstanceAttribute AS IsInstanceAttribute "
		;
		String sqlFrom = "FROM M_Product AS p"
			+ " LEFT OUTER JOIN M_AttributeSet AS pa ON (pa.M_AttributeSet_ID=p.M_AttributeSet_ID)"
			+ " LEFT OUTER JOIN M_Product_Class AS pcl ON (pcl.M_Product_Class_ID=p.M_Product_Class_ID)"
			+ " LEFT OUTER JOIN M_Product_Classification AS pcls ON (pcls.M_Product_Classification_ID=p.M_Product_Classification_ID)"
			+ " LEFT OUTER JOIN M_Product_Group AS pg ON (pg.M_Product_Group_ID = p.M_Product_Group_ID)"
			+ " LEFT OUTER JOIN M_Product_Category AS pc ON (pc.M_Product_Category_ID=p.M_Product_Category_ID)"
			+ " LEFT OUTER JOIN C_UOM AS u ON (p.C_UOM_ID=u.C_UOM_ID)"
		;

		String sqlWhere = " WHERE p.AD_Client_ID = ? ";
		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			Env.getAD_Client_ID(context)
		);

		// Price List Version
		String currencyCode = "";
		final int priceListVersionId = request.getPriceListVersionId();
		if (request.getPriceListVersionId() > 0) {
			sqlQuery += ", "
				+ "bomPriceList(p.M_Product_ID, pr.M_PriceList_Version_ID) AS PriceList, "
				+ "bomPriceStd(p.M_Product_ID, pr.M_PriceList_Version_ID) AS PriceStd, "
				+ "bomPriceLimit(p.M_Product_ID, pr.M_PriceList_Version_ID) AS PriceLimit, "
				+ "bomPriceStd(p.M_Product_ID, pr.M_PriceList_Version_ID) - bomPriceLimit(p.M_Product_ID, pr.M_PriceList_Version_ID) AS Margin "
			;
			sqlFrom += " LEFT OUTER JOIN ("
				+			"SELECT mpp.M_Product_ID, mpp.M_PriceList_Version_id, "
				+			"mpp.IsActive, mpp.PriceList, mpp.PriceStd, mpp.PriceLimit"
				+			" FROM M_ProductPrice AS mpp, M_PriceList_Version AS mplv "
				+			" WHERE mplv.M_PriceList_Version_ID = mpp.M_PriceList_Version_ID AND mplv.IsActive = 'Y'"
				+		") AS pr"
				+ " ON (p.M_Product_ID=pr.M_Product_ID AND pr.IsActive='Y') "
			;
			sqlWhere += " AND pr.M_PriceList_Version_ID = ? ";
			filtersList.add(
				priceListVersionId
			);

			final String priceListWhere = "EXISTS ("
				+ "SELECT 1 FROM M_PriceList_Version AS plv "
				+ "WHERE plv.M_PriceList_ID = M_PriceList.M_PriceList_ID "
				+ "AND plv.M_Pricelist_Version_ID = ? "
				+ ")"
			; 
			MPriceList priceList = new Query(
				Env.getCtx(),
				I_M_PriceList.Table_Name,
				priceListWhere,
				null
			)
				.setParameters(priceListVersionId)
				.first()
			;
			if (priceList != null && priceList.getM_PriceList_ID() > 0) {
				MCurrency currency = MCurrency.get(Env.getCtx(), priceList.getC_Currency_ID());
				if (currency != null && currency.getC_Currency_ID() > 0) {
					currencyCode = currency.getISO_Code();
				}
			}
		}

		// Warehouse
		final int warhouseId = request.getWarehouseId();
		boolean isUnconfirmed = false;
		if (warhouseId > 0) {
			sqlQuery += ", "
				+ "CASE WHEN p.IsBOM='N' AND (p.ProductType!='I' OR p.IsStocked='N') "
					+ "THEN to_number(get_Sysconfig('QTY_TO_SHOW_FOR_SERVICES', '99999', p.AD_Client_ID, 0), '99999999999') "
					+ "ELSE bomQtyAvailable(p.M_Product_ID, " + warhouseId + ", 0) "
				+ "END AS QtyAvailable, "
				+ "CASE WHEN p.IsBOM='N' AND (p.ProductType!='I' OR p.IsStocked='N') "
					+ "THEN to_number(get_Sysconfig('QTY_TO_SHOW_FOR_SERVICES', '99999', p.AD_Client_ID, 0), '99999999999') "
					+ "ELSE bomQtyOnHand(p.M_Product_ID, " + warhouseId + ", 0) "
				+ "END AS QtyOnHand, "
				+ "bomQtyReserved(p.M_Product_ID, " + warhouseId + ", 0) AS QtyReserved, "
				+ "bomQtyOrdered(p.M_Product_ID, " + warhouseId + ", 0) AS QtyOrdered "
			;

			isUnconfirmed = isUnconfirmed();
			if (isUnconfirmed) {
				sqlQuery += ", "
					+ "(SELECT SUM(c.TargetQty) FROM M_InOutLineConfirm AS c "
						+ "INNER JOIN M_InOutLine AS il ON (c.M_InOutLine_ID=il.M_InOutLine_ID) "
						+ "INNER JOIN M_InOut AS i ON (il.M_InOut_ID=i.M_InOut_ID) "
						+ "WHERE c.Processed='N' AND i.M_Warehouse_ID=" + warhouseId + " AND il.M_Product_ID=p.M_Product_ID) "
					+ "AS QtyUnconfirmed, "
					+ "(SELECT SUM(c.TargetQty) FROM M_MovementLineConfirm AS c "
						+ "INNER JOIN M_MovementLine AS ml ON (c.M_MovementLine_ID=ml.M_MovementLine_ID) "
						+ "INNER JOIN M_Locator AS l ON (ml.M_LocatorTo_ID=l.M_Locator_ID) "
						+ "WHERE c.Processed='N' AND l.M_Warehouse_ID=" + warhouseId + " AND ml.M_Product_ID=p.M_Product_ID) "
					+ "AS QtyUnconfirmedMove "
				;
			}
			sqlWhere += " AND p.IsSummary='N' ";
		}

		String sql = sqlQuery + sqlFrom + sqlWhere;

		// add where with access restriction
		String parsedSQL = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql,
				"p",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		//	Add Order By
		String sqlOrderBy = " ORDER BY p.Value ";
		if (warhouseId > 0) {
			sqlOrderBy += ", QtyAvailable DESC";
		}
		if (priceListVersionId > 0) {
			sqlOrderBy += ", Margin DESC";
		}

		parsedSQL = parsedSQL + sqlOrderBy;

		ProductInfo.Builder builder = ProductInfo.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(parsedSQL, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				builder = ProductInfoConvert.convertProductInfo(
					rs,
					priceListVersionId,
					warhouseId,
					isUnconfirmed
				);
				builder.setCurrency(
					TextManager.getValidString(currencyCode)
				);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builder;
	}

	public static ListProductsInfoResponse.Builder listProductsInfo(ListProductsInfoRequest request) {
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			Table_Name,
			request.getIsWithoutValidation()
		);

		//  Fill Cintext
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, context, request.getContextAttributes());

		String sqlQuery = "SELECT "
			+ "p.M_Product_ID, p.UUID, p.Discontinued, "
			+ "p.IsStocked AS IsStocked, "
			+ "pc.Name AS M_Product_Category_ID, "
			+ "pcl.Name AS M_Product_Class_ID, "
			+ "pcls.Name AS M_Product_Classification_ID, "
			+ "pg.Name AS M_Product_Group_ID, "
			+ "p.Value, p.Name, p.UPC, p.SKU, p.IsActive, "
			+ "u.Name AS C_UOM_ID, "
			+ "bp.Name AS Vendor, "
			+ "pa.IsInstanceAttribute AS IsInstanceAttribute "
		;
		String sqlFrom = "FROM M_Product AS p "
			+ "LEFT JOIN M_Product_Class AS pcl ON (pcl.M_Product_Class_ID = p.M_Product_Class_ID) "
			+ "LEFT JOIN M_Product_Classification AS pcls ON (pcls.M_Product_Classification_ID = p.M_Product_Classification_ID) "
			+ "LEFT JOIN M_Product_Group AS pg ON (pg.M_Product_Group_ID = p.M_Product_Group_ID) "
			+ "LEFT JOIN M_Product_Category AS pc ON (pc.M_Product_Category_ID = p.M_Product_Category_ID) "
			+ "LEFT JOIN C_UOM AS u ON (p.C_UOM_ID = u.C_UOM_ID) "
			+ "LEFT JOIN M_Product_PO AS ppo ON (ppo.M_Product_ID = p.M_Product_ID AND ppo.IsCurrentVendor = 'Y' AND ppo.IsActive = 'Y' AND ppo.AD_Org_ID in (0, ?)) "
			+ "LEFT JOIN C_BPartner AS bp ON (ppo.C_BPartner_ID = bp.C_BPartner_ID) "
			+ "LEFT JOIN M_AttributeSet AS pa ON (pa.M_AttributeSet_ID = p.M_AttributeSet_ID) "
		;

		String sqlWhere = " WHERE p.AD_Client_ID = ? ";
		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			Env.getAD_Org_ID(context)
		);
		filtersList.add(
			Env.getAD_Client_ID(context)
		);

		// validate is active record
		if (request.getIsOnlyActiveRecords()) {
			sqlWhere += "AND p.IsActive = ? ";
			filtersList.add(true);
		}

		// URL decode to change characteres and add search value to filter
		final String searchValue = TextManager.getValidString(
			TextManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if (!Util.isEmpty(searchValue, true)) {
			sqlWhere += " AND ("
				+ "UPPER(p.Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(p.Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(p.UPC) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(p.SKU) LIKE '%' || UPPER(?) || '%' "
				+ ") "
			;
			filtersList.add(searchValue);
			filtersList.add(searchValue);
			filtersList.add(searchValue);
			filtersList.add(searchValue);
		}

		// Value
		final String value = TextManager.getDecodeUrl(
			request.getValue()
		);
		if (!Util.isEmpty(value)) {
			sqlWhere += " AND UPPER(p.Value) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(value);
		}
		// Name
		final String name = TextManager.getDecodeUrl(
			request.getName()
		);
		if (!Util.isEmpty(name)) {
			sqlWhere += " AND UPPER(p.Name) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(name);
		}
		// UPC/EAN
		final String upc = TextManager.getDecodeUrl(
			request.getUpc()
		);
		if (!Util.isEmpty(upc)) {
			sqlWhere += " AND UPPER(p.UPC) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(upc);
		}
		// SKU
		final String sku = TextManager.getDecodeUrl(
			request.getSku()
		);
		if (!Util.isEmpty(sku)) {
			sqlWhere += " AND UPPER(p.SKU) LIKE '%' || UPPER(?) || '%' ";
			filtersList.add(sku);
		}
		// Product Category
		final int productCategoryId = request.getProductCategoryId();
		if (productCategoryId > 0) {
			// sqlWhere += " AND p.M_Product_Category_ID = ? ";

			//  Optional Product Category
			sqlWhere += " AND ("
				+ "p.M_Product_Category_ID = ? "
				+ "OR p.M_Product_Category_ID IN ("
						+ "SELECT ppc.M_Product_Category_ID "
						+ "FROM M_Product_Category AS ppc "
						+ "WHERE ppc.M_Product_Category_Parent_ID = ? "
						+ "AND ppc.IsActive = 'Y' "
					+ ")"
				+ ")"
			;
			filtersList.add(
				productCategoryId
			);
			filtersList.add(
				productCategoryId
			);
		}
		// Product Class
		if (request.getProductClassId() > 0) {
			sqlWhere += " AND p.M_Product_Class_ID = ? ";
			filtersList.add(
				request.getProductClassId()
			);
		}
		// Product Classification
		if (request.getProductClassificationId() > 0) {
			sqlWhere += " AND p.M_Product_Classification_ID = ? ";
			filtersList.add(
				request.getProductClassificationId()
			);
		}
		// Product Group
		if (request.getProductGroupId() > 0) {
			sqlWhere += " AND p.M_Product_Group_ID = ? ";
			filtersList.add(
				request.getProductGroupId()
			);
		}
		// Attribute Set
		if (request.getAttributeSetId() > 0) {
			sqlWhere += " AND p.M_AttributeSet_ID = ? ";
			filtersList.add(
				request.getAttributeSetId()
			);
		}
		// Attribute Set Instance
		if (request.getAttributeSetInstanceId() > 0) {
			sqlWhere += " AND p.M_AttributeSetInstance_ID = ? ";
			filtersList.add(
				request.getAttributeSetInstanceId()
			);
			
			// String asiWhere = fASI_ID.getAttributeWhere();
			// if (asiWhere.length() > 0)
			// {
			// 	if (asiWhere.startsWith(" AND "))
			// 		asiWhere = asiWhere.substring(5);
			// 	list.add(asiWhere);
			// }
		}
		// Is Stocked
		if (!Util.isEmpty(request.getIsStocked(), true) && BooleanManager.isValidDataBaseBoolean(request.getIsStocked())) {
			sqlWhere += " AND p.IsStocked = ? ";
			boolean isStocked = BooleanManager.getBooleanFromString(
				request.getIsStocked()
			);
			filtersList.add(isStocked);
		}
		// Vendor
		if (request.getVendorId() > 0) {
			sqlWhere += " AND EXISTS("
					+ "SELECT 1 FROM M_Product_PO AS ppo "
					+ "WHERE ppo.C_BPartner_ID = ? "
					+ "AND ppo.M_Product_ID = p.M_Product_ID "
					// + "AND ppo.IsActive = 'Y' "
				+ ")"
			;
			filtersList.add(
				request.getVendorId()
			);
		}

		// Price List Version
		String currencyCode = "";
		final int priceListVersionId = request.getPriceListVersionId();
		if (request.getPriceListVersionId() > 0) {
			sqlQuery += ", "
				+ "bomPriceList(p.M_Product_ID, pr.M_PriceList_Version_ID) AS PriceList, "
				+ "bomPriceStd(p.M_Product_ID, pr.M_PriceList_Version_ID) AS PriceStd, "
				+ "bomPriceLimit(p.M_Product_ID, pr.M_PriceList_Version_ID) AS PriceLimit, "
				+ "bomPriceStd(p.M_Product_ID, pr.M_PriceList_Version_ID) - bomPriceLimit(p.M_Product_ID, pr.M_PriceList_Version_ID) AS Margin "
			;
			sqlFrom += " LEFT OUTER JOIN ("
				+			"SELECT mpp.M_Product_ID, mpp.M_PriceList_Version_id, "
				+			"mpp.IsActive, mpp.PriceList, mpp.PriceStd, mpp.PriceLimit"
				+			" FROM M_ProductPrice AS mpp, M_PriceList_Version AS mplv "
				+			" WHERE mplv.M_PriceList_Version_ID = mpp.M_PriceList_Version_ID AND mplv.IsActive = 'Y'"
				+		") AS pr"
				+ " ON (p.M_Product_ID = pr.M_Product_ID AND pr.IsActive = 'Y') "
			;
			sqlWhere += " AND pr.M_PriceList_Version_ID = ? ";
			filtersList.add(
				priceListVersionId
			);

			final String priceListWhere = "EXISTS ("
					+ "SELECT 1 FROM M_PriceList_Version AS plv "
					+ "WHERE plv.M_PriceList_ID = M_PriceList.M_PriceList_ID "
					+ "AND plv.M_Pricelist_Version_ID = ? "
					// + "AND plv.IsActive = 'Y' "
				+ ")"
			; 
			MPriceList priceList = new Query(
				Env.getCtx(),
				I_M_PriceList.Table_Name,
				priceListWhere,
				null
			)
				.setParameters(priceListVersionId)
				.first()
			;
			if (priceList != null && priceList.getM_PriceList_ID() > 0) {
				MCurrency currency = MCurrency.get(Env.getCtx(), priceList.getC_Currency_ID());
				if (currency != null && currency.getC_Currency_ID() > 0) {
					currencyCode = currency.getISO_Code();
				}
			}
		}

		// Warehouse
		final int warhouseId = request.getWarehouseId();
		boolean isUnconfirmed = false;
		if (warhouseId > 0) {
			sqlQuery += ", "
				+ "CASE WHEN p.IsBOM = 'N' AND (p.ProductType != 'I' OR p.IsStocked = 'N') "
					+ "THEN to_number(get_Sysconfig('QTY_TO_SHOW_FOR_SERVICES', '99999', p.AD_Client_ID, 0), '99999999999') "
					+ "ELSE bomQtyAvailable(p.M_Product_ID, " + warhouseId + ", 0) "
				+ "END AS QtyAvailable, "
				+ "CASE WHEN p.IsBOM = 'N' AND (p.ProductType != 'I' OR p.IsStocked = 'N') "
					+ "THEN to_number(get_Sysconfig('QTY_TO_SHOW_FOR_SERVICES', '99999', p.AD_Client_ID, 0), '99999999999') "
					+ "ELSE bomQtyOnHand(p.M_Product_ID, " + warhouseId + ", 0) "
				+ "END AS QtyOnHand, "
				+ "bomQtyReserved(p.M_Product_ID, " + warhouseId + ", 0) AS QtyReserved, "
				+ "bomQtyOrdered(p.M_Product_ID, " + warhouseId + ", 0) AS QtyOrdered "
			;

			isUnconfirmed = isUnconfirmed();
			if (isUnconfirmed) {
				sqlQuery += ", "
					+ "(SELECT SUM(c.TargetQty) FROM M_InOutLineConfirm AS c "
						+ "INNER JOIN M_InOutLine AS il ON (c.M_InOutLine_ID = il.M_InOutLine_ID) "
						+ "INNER JOIN M_InOut AS i ON (il.M_InOut_ID = i.M_InOut_ID) "
						+ "WHERE c.Processed = 'N' AND i.M_Warehouse_ID = " + warhouseId + " AND il.M_Product_ID = p.M_Product_ID) "
					+ "AS QtyUnconfirmed, "
					+ "(SELECT SUM(c.TargetQty) FROM M_MovementLineConfirm AS c "
						+ "INNER JOIN M_MovementLine AS ml ON (c.M_MovementLine_ID = ml.M_MovementLine_ID) "
						+ "INNER JOIN M_Locator AS l ON (ml.M_LocatorTo_ID = l.M_Locator_ID) "
						+ "WHERE c.Processed = 'N' AND l.M_Warehouse_ID = " + warhouseId + " AND ml.M_Product_ID = p.M_Product_ID) "
					+ "AS QtyUnconfirmedMove "
				;
			}
			sqlWhere += " AND p.IsSummary='N' ";

			// Is Only Stock Available
			if (request.getIsOnlyStockAvailable()) {
				// compare with `QtyOnHand` column
				sqlWhere += " AND ("
					+ "CASE WHEN p.IsBOM = 'N' AND (p.ProductType != 'I' OR p.IsStocked = 'N') "
						+ "THEN to_number(get_Sysconfig('QTY_TO_SHOW_FOR_SERVICES', '99999', p.AD_Client_ID, 0), '99999999999') "
						+ "ELSE bomQtyOnHand(p.M_Product_ID, " + warhouseId + ", 0) "
					+ "END > 0"
					+ ") "
				;
			}
		}

		String sql = sqlQuery + sqlFrom + sqlWhere;

		// add where with access restriction
		String sqlWithRoleAccess = MRole.getDefault(Env.getCtx(), false)
			.addAccessSQL(
				sql,
				"p",
				MRole.SQL_FULLYQUALIFIED,
				MRole.SQL_RO
			);

		// validation code of field
		if (!request.getIsWithoutValidation() && !Util.isEmpty(reference.ValidationCode, true)) {
			String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(
				Table_Name,
				"p",
				reference.ValidationCode
			);
			String parsedValidationCode = Env.parseContext(context, windowNo, validationCode, false);
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException(
					"@AD_Reference_ID@ " + reference.KeyColumn + ", @Code@/@WhereClause@ @Unparseable@"
				);
			}
			sqlWithRoleAccess += " AND " + parsedValidationCode;
		}

		//	Count records
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = CountUtil.countRecords(sqlWithRoleAccess, Table_Name, "p", filtersList);
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListProductsInfoResponse.Builder builderList = ListProductsInfoResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(
					nexPageToken
				)
			)
		;

		String parsedSQL = LimitUtil.getQueryWithLimit(sqlWithRoleAccess, limit, offset);

		//	Add Order By
		String sqlOrderBy = " ORDER BY ";
		// Order by category name only when no category is selected (to group by
		// category); when a category is filtered, all rows share it, so it adds nothing.
		if (productCategoryId <= 0) {
			sqlOrderBy += " pc.Name, ";
		}
		sqlOrderBy += " p.Value ";
		if (warhouseId > 0) {
			sqlOrderBy += ", QtyAvailable DESC";
		}
		if (priceListVersionId > 0) {
			sqlOrderBy += ", Margin DESC";
		}

		parsedSQL = parsedSQL + sqlOrderBy;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(parsedSQL, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				ProductInfo.Builder builder = ProductInfoConvert.convertProductInfo(
					rs,
					priceListVersionId,
					warhouseId,
					isUnconfirmed
				);
				builder.setCurrency(
					TextManager.getValidString(currencyCode)
				);
				builderList.addRecords(builder);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	/**
	 * Export the product search result into an Excel (XLSX) file.
	 * Reuses the same filters and query as {@link #listProductsInfo(ListProductsInfoRequest)},
	 * so the export mirrors what the search shows. When `select_columns` is empty the default
	 * columns (the ones displayed by the list for the current filters) are exported; otherwise
	 * only the requested columns are included, in the given order. When `is_export_all_records`
	 * is true every matching record is exported; otherwise only the current page
	 * (`page_size` + `page_token`) is written, while `record_count` stays the total match count.
	 * The file is uploaded as a temporary resource and its name/URL is returned.
	 * @param request export request, `select_columns` carries the columns to include
	 * @return total record count and the exported file name
	 */
	public static ExportProductsInfoResponse.Builder exportProductsInfo(ExportProductsInfoRequest request) {
		// Output format (case-insensitive). Only Excel/xlsx exists for now; an empty
		// value defaults to it and any other value falls back to it too.
		final String format = TextManager.getValidString(
			request.getFormat()
		).trim().toLowerCase();

		// Reuse the list query (same filters). When exporting all records we bypass
		// pagination; otherwise the request's page_size/page_token are kept so only
		// that page is exported. record_count is always the total matching count.
		ListProductsInfoRequest.Builder listRequestBuilder = toListProductsInfoRequest(request);
		if (request.getIsExportAllRecords()) {
			listRequestBuilder
				.setPageSize(Integer.MAX_VALUE)
				.setPageToken("")
			;
		}
		ListProductsInfoResponse.Builder listBuilder = listProductsInfo(
			listRequestBuilder.build()
		);
		List<ProductInfo> records = listBuilder.getRecordsList();

		// Resolve the columns to export: custom ones when provided, else the list defaults
		List<ExportColumn> exportColumns = resolveExportColumns(
			request.getSelectColumnsList(),
			request.getWarehouseId(),
			request.getPriceListVersionId(),
			records
		);

		// Generate the export file. Future formats (CSV, PDF, ...) would add a branch;
		// any other/unset value falls back to XLSX, so an empty request still exports Excel.
		File exportFile = null;
		try {
			switch (format) {
				case EXPORT_FORMAT_XLSX:
				default:
					exportFile = generateProductsInfoExcel(records, exportColumns);
					break;
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new AdempiereException("@Error@ generating export file: " + e.getLocalizedMessage());
		}

		// Upload as a temporary resource (same mechanism used by other exports)
		String fileName = null;
		try {
			fileName = S3Manager.putTemporaryFile(exportFile);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new AdempiereException("@Error@ uploading file: " + e.getLocalizedMessage());
		}

		return ExportProductsInfoResponse.newBuilder()
			.setRecordCount(
				listBuilder.getRecordCount()
			)
			.setFileName(
				TextManager.getValidString(fileName)
			)
		;
	}


	/**
	 * Map an export request into a list request so both share the same query logic.
	 * The export request mirrors {@link ListProductsInfoRequest}, only adding the
	 * output format, so every filter is copied through as-is.
	 * @param request export request
	 * @return a list request builder with the same filters
	 */
	private static ListProductsInfoRequest.Builder toListProductsInfoRequest(ExportProductsInfoRequest request) {
		ListProductsInfoRequest.Builder builder = ListProductsInfoRequest.newBuilder()
			.setFilters(request.getFilters())
			.setSortBy(request.getSortBy())
			.addAllGroupColumns(request.getGroupColumnsList())
			.addAllSelectColumns(request.getSelectColumnsList())
			.setPageSize(request.getPageSize())
			.setPageToken(request.getPageToken())
			.setSearchValue(request.getSearchValue())
			.setContextAttributes(request.getContextAttributes())
			.setIsOnlyActiveRecords(request.getIsOnlyActiveRecords())
			.setIsWithoutValidation(request.getIsWithoutValidation())
			// references
			.setReferenceId(request.getReferenceId())
			.setTableName(request.getTableName())
			.setColumnName(request.getColumnName())
			.setColumnId(request.getColumnId())
			.setFieldId(request.getFieldId())
			.setProcessParameterId(request.getProcessParameterId())
			.setBrowseFieldId(request.getBrowseFieldId())
			.setDisplayDefinitionFieldId(request.getDisplayDefinitionFieldId())
			// custom filters
			.setValue(request.getValue())
			.setName(request.getName())
			.setUpc(request.getUpc())
			.setSku(request.getSku())
			.setWarehouseId(request.getWarehouseId())
			.setPriceListVersionId(request.getPriceListVersionId())
			.setProductCategoryId(request.getProductCategoryId())
			.setProductGroupId(request.getProductGroupId())
			.setProductClassId(request.getProductClassId())
			.setProductClassificationId(request.getProductClassificationId())
			.setAttributeSetId(request.getAttributeSetId())
			.setAttributeSetInstanceId(request.getAttributeSetInstanceId())
			.setVendorId(request.getVendorId())
			.setIsStocked(request.getIsStocked())
			.setIsOnlyStockAvailable(request.getIsOnlyStockAvailable())
		;
		if (request.hasCurrentValue()) {
			builder.setCurrentValue(
				request.getCurrentValue()
			);
		}
		return builder;
	}


	/**
	 * Definition of an exportable column of the product search result.
	 * Maps a client column key to a translated header and a value extractor.
	 */
	private static class ExportColumn {
		// canonical key = proto field name (what select_columns expects)
		final String key;
		// DB/AD column name: used to translate the header via AD_Element AND accepted as an
		// alternative name in select_columns (besides `key`). Nullable when there is no DB column.
		final String elementColumnName;
		// optional AD_Message key: when set, its translation wins over the element label.
		// Used for computed/aliased columns whose element label would be misleading
		// (e.g. "Margin" the amount vs the "Margin %" element). Nullable.
		final String messageKey;
		// fallback header when no translation is available
		final String defaultHeader;
		// numeric columns are written as numbers, the rest as text
		final boolean isNumeric;
		final Function<ProductInfo, String> valueProvider;

		ExportColumn(String key, String elementColumnName, String defaultHeader, boolean isNumeric, Function<ProductInfo, String> valueProvider) {
			this(key, elementColumnName, null, defaultHeader, isNumeric, valueProvider);
		}

		ExportColumn(String key, String elementColumnName, String messageKey, String defaultHeader, boolean isNumeric, Function<ProductInfo, String> valueProvider) {
			this.key = key;
			this.elementColumnName = elementColumnName;
			this.messageKey = messageKey;
			this.defaultHeader = defaultHeader;
			this.isNumeric = isNumeric;
			this.valueProvider = valueProvider;
		}

		String getHeader() {
			// An explicit AD_Message key wins (computed/aliased columns, e.g. Margin, Vendor)
			if (!Util.isEmpty(messageKey, true)) {
				String message = Msg.getMsg(Env.getCtx(), messageKey);
				if (!Util.isEmpty(message, true) && !message.equals(messageKey)) {
					return message;
				}
			}
			// Otherwise the translated AD_Element label of the DB column
			if (!Util.isEmpty(elementColumnName, true)) {
				String element = Msg.getElement(Env.getCtx(), elementColumnName);
				if (!Util.isEmpty(element, true) && !element.equals(elementColumnName)) {
					return element;
				}
			}
			return defaultHeader;
		}
	}


	/**
	 * Registry of every exportable column, keyed by the client column key and kept in
	 * the same order shown by the list. Booleans are rendered as translated Yes/No text.
	 * @return ordered registry of exportable columns
	 */
	private static Map<String, ExportColumn> getExportColumnRegistry() {
		Map<String, ExportColumn> registry = new LinkedHashMap<String, ExportColumn>();
		putExportColumn(registry, new ExportColumn("value", "Value", "Value", false, product -> product.getValue()));
		putExportColumn(registry, new ExportColumn("name", "Name", "Name", false, product -> product.getName()));
		putExportColumn(registry, new ExportColumn("upc", "UPC", "UPC/EAN", false, product -> product.getUpc()));
		putExportColumn(registry, new ExportColumn("sku", "SKU", "SKU", false, product -> product.getSku()));
		putExportColumn(registry, new ExportColumn("product_category", "M_Product_Category_ID", "Product Category", false, product -> product.getProductCategory()));
		putExportColumn(registry, new ExportColumn("product_group", "M_Product_Group_ID", "Product Group", false, product -> product.getProductGroup()));
		putExportColumn(registry, new ExportColumn("product_class", "M_Product_Class_ID", "Product Class", false, product -> product.getProductClass()));
		putExportColumn(registry, new ExportColumn("product_classification", "M_Product_Classification_ID", "Product Classification", false, product -> product.getProductClassification()));
		putExportColumn(registry, new ExportColumn("uom", "C_UOM_ID", "UOM", false, product -> product.getUom()));
		putExportColumn(registry, new ExportColumn("list_price", "PriceList", "List Price", true, product -> product.getListPrice()));
		putExportColumn(registry, new ExportColumn("standard_price", "PriceStd", "Standard Price", true, product -> product.getStandardPrice()));
		putExportColumn(registry, new ExportColumn("limit_price", "PriceLimit", "Limit Price", true, product -> product.getLimitPrice()));
		putExportColumn(registry, new ExportColumn("margin", null, "Margin", "Margin", true, product -> product.getMargin()));
		putExportColumn(registry, new ExportColumn("is_stocked", "IsStocked", "Is Stocked", false, product -> BooleanManager.getDisplayValue(product.getIsStocked())));
		putExportColumn(registry, new ExportColumn("available_quantity", "QtyAvailable", "Available Quantity", true, product -> product.getAvailableQuantity()));
		putExportColumn(registry, new ExportColumn("on_hand_quantity", "QtyOnHand", "On Hand Quantity", true, product -> product.getOnHandQuantity()));
		putExportColumn(registry, new ExportColumn("reserved_quantity", "QtyReserved", "Reserved Quantity", true, product -> product.getReservedQuantity()));
		putExportColumn(registry, new ExportColumn("ordered_quantity", "QtyOrdered", "Ordered Quantity", true, product -> product.getOrderedQuantity()));
		putExportColumn(registry, new ExportColumn("unconfirmed_quantity", "QtyUnconfirmed", "QtyUnconfirmed", "Unconfirmed Quantity", true, product -> product.getUnconfirmedQuantity()));
		putExportColumn(registry, new ExportColumn("unconfirmed_move_quantity", "QtyUnconfirmedMove", "QtyUnconfirmedMove", "Unconfirmed Move Quantity", true, product -> product.getUnconfirmedMoveQuantity()));
		putExportColumn(registry, new ExportColumn("vendor", "Vendor", "Vendor", "Vendor", false, product -> product.getVendor()));
		putExportColumn(registry, new ExportColumn("description", "Description", "Description", false, product -> product.getDescription()));
		putExportColumn(registry, new ExportColumn("document_note", "DocumentNote", "Document Note", false, product -> product.getDocumentNote()));
		putExportColumn(registry, new ExportColumn("is_active", "IsActive", "Active", false, product -> BooleanManager.getDisplayValue(product.getIsActive())));
		putExportColumn(registry, new ExportColumn("discontinued", "Discontinued", "Discontinued", false, product -> BooleanManager.getDisplayValue(product.getDiscontinued())));
		putExportColumn(registry, new ExportColumn("currency", "C_Currency_ID", "Currency", false, product -> product.getCurrency()));
		return registry;
	}

	private static void putExportColumn(Map<String, ExportColumn> registry, ExportColumn column) {
		registry.put(column.key, column);
	}


	/**
	 * Build a case-insensitive lookup that accepts, for each column, both the proto field
	 * name (the column `key`) and the DB column name, so `select_columns` works with either.
	 * @param registry ordered column registry
	 * @return map from proto key and DB column name (both lowercase) to column
	 */
	private static Map<String, ExportColumn> buildColumnLookup(Map<String, ExportColumn> registry) {
		Map<String, ExportColumn> lookup = new HashMap<String, ExportColumn>();
		for (ExportColumn column : registry.values()) {
			lookup.put(column.key, column);
			if (!Util.isEmpty(column.elementColumnName, true)) {
				lookup.put(
					column.elementColumnName.toLowerCase(),
					column
				);
			}
		}
		return lookup;
	}


	/**
	 * Resolve the columns to export. When the client sends specific columns they are used,
	 * in the given order; each is matched by its proto field name or its DB column name,
	 * unknown names are ignored and columns not applicable to the current filters are skipped
	 * (see {@link #isColumnApplicable}). Otherwise the default columns are those the list shows
	 * for the current filters (price columns only with a price list version, stock columns only
	 * with a warehouse, unconfirmed columns only when present).
	 * @param selectedColumns optional client column keys
	 * @param warehouseId selected warehouse (0 when none)
	 * @param priceListVersionId selected price list version (0 when none)
	 * @param records exported records, used to detect unconfirmed stock data
	 * @return ordered list of columns to write
	 */
	private static List<ExportColumn> resolveExportColumns(List<String> selectedColumns, int warehouseId, int priceListVersionId, List<ProductInfo> records) {
		Map<String, ExportColumn> registry = getExportColumnRegistry();
		List<ExportColumn> columns = new ArrayList<ExportColumn>();

		// Unconfirmed stock data is only present for some warehouse configurations
		boolean hasUnconfirmed = records.stream().anyMatch(record ->
			!Util.isEmpty(record.getUnconfirmedQuantity(), true)
			|| !Util.isEmpty(record.getUnconfirmedMoveQuantity(), true)
		);

		// Custom columns: use exactly the requested ones, in the given order.
		// Both the proto field name and the DB column name are accepted.
		if (selectedColumns != null && !selectedColumns.isEmpty()) {
			Map<String, ExportColumn> columnsByName = buildColumnLookup(registry);
			for (String selectedColumn : selectedColumns) {
				if (Util.isEmpty(selectedColumn, true)) {
					continue;
				}
				ExportColumn column = columnsByName.get(
					selectedColumn.trim().toLowerCase()
				);
				if (column == null) {
					log.warning("Export column not supported, ignored: " + selectedColumn);
					continue;
				}
				// Skip columns whose value cannot be computed for the current filters
				// (price columns need a price list version, stock columns need a warehouse)
				if (!isColumnApplicable(column.key, warehouseId, priceListVersionId, hasUnconfirmed)) {
					continue;
				}
				columns.add(column);
			}
			if (!columns.isEmpty()) {
				return columns;
			}
			// none of the requested columns was valid/applicable: fall back to the defaults
		}

		// Default columns: same ones the list shows for the current filters
		for (ExportColumn column : registry.values()) {
			final String key = column.key;
			if (!isColumnApplicable(key, warehouseId, priceListVersionId, hasUnconfirmed)) {
				continue;
			}
			// columns not shown by the list are only exported when explicitly requested
			if (key.equals("currency") || key.equals("description") || key.equals("document_note")) {
				continue;
			}
			columns.add(column);
		}
		return columns;
	}


	/**
	 * Whether a column can produce a value for the current filters. Price columns need a
	 * selected price list version, stock columns need a selected warehouse, and unconfirmed
	 * columns need a warehouse plus actual unconfirmed data; every other column always applies.
	 * @param key column key
	 * @param warehouseId selected warehouse (0 when none)
	 * @param priceListVersionId selected price list version (0 when none)
	 * @param hasUnconfirmed whether any record has unconfirmed stock data
	 * @return true when the column should be included
	 */
	private static boolean isColumnApplicable(String key, int warehouseId, int priceListVersionId, boolean hasUnconfirmed) {
		// price columns need a selected price list version
		if (key.equals("list_price") || key.equals("standard_price") || key.equals("limit_price") || key.equals("margin")) {
			return priceListVersionId > 0;
		}
		// stock columns need a selected warehouse
		if (key.equals("available_quantity") || key.equals("on_hand_quantity") || key.equals("reserved_quantity") || key.equals("ordered_quantity")) {
			return warehouseId > 0;
		}
		// unconfirmed columns need a warehouse and actual unconfirmed data
		if (key.equals("unconfirmed_quantity") || key.equals("unconfirmed_move_quantity")) {
			return warehouseId > 0 && hasUnconfirmed;
		}
		return true;
	}


	/**
	 * Build the XLSX file with the given records and columns.
	 * @param records exported records
	 * @param exportColumns ordered columns to write
	 * @return generated temporary Excel file
	 * @throws Exception on write error
	 */
	private static File generateProductsInfoExcel(List<ProductInfo> records, List<ExportColumn> exportColumns) throws Exception {
		File excelFile = File.createTempFile("ProductsInfo_" + System.currentTimeMillis(), ".xlsx");

		XSSFWorkbook workBook = new XSSFWorkbook();
		try {
			org.apache.poi.ss.usermodel.Sheet sheet = workBook.createSheet("Products");

			// Header row with bold style
			org.apache.poi.ss.usermodel.CellStyle headerStyle = workBook.createCellStyle();
			org.apache.poi.ss.usermodel.Font headerFont = workBook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);

			org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
			int cellIndex = 0;
			for (ExportColumn column : exportColumns) {
				org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(cellIndex++);
				cell.setCellValue(
					column.getHeader()
				);
				cell.setCellStyle(headerStyle);
			}

			// Data rows
			int rowNum = 1;
			for (ProductInfo record : records) {
				org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
				cellIndex = 0;
				for (ExportColumn column : exportColumns) {
					org.apache.poi.ss.usermodel.Cell cell = row.createCell(cellIndex++);
					String rawValue = column.valueProvider.apply(record);
					if (column.isNumeric) {
						BigDecimal numericValue = NumberManager.getBigDecimalFromString(rawValue);
						if (numericValue != null) {
							// MVP: native numeric cell without a presentation format.
							// TODO: apply a CellStyle/DataFormat (quantities by the product UOM
							// precision, amounts by the price list currency). See docs/export-search-fields.md
							cell.setCellValue(
								numericValue.doubleValue()
							);
						} else {
							cell.setCellValue("");
						}
					} else {
						cell.setCellValue(
							TextManager.getValidString(rawValue)
						);
					}
				}
			}

			// Auto-size columns
			for (int i = 0; i < exportColumns.size(); i++) {
				sheet.autoSizeColumn(i);
			}

			try (FileOutputStream outputStream = new FileOutputStream(excelFile)) {
				workBook.write(outputStream);
			}
		} finally {
			workBook.close();
		}

		return excelFile;
	}


	/**
	 * Validate productId and MProduct, and get instance
	 * @param productId
	 * @return
	 */
	public static MProduct validateAndGetProduct(int productId) {
		if (productId <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_Product_ID@");
		}
		MProduct product = MProduct.get(Env.getCtx(), productId);
		if (product == null || product.getM_Product_ID() <= 0) {
			throw new AdempiereException("@M_Product_ID@ @NotFound@");
		}
		return product;
	}



	public static ListWarehouseStocksResponse.Builder listWarehouseStocks(ListWarehouseStocksRequest request) {
		validateAndGetProduct(
			request.getProductId()
		);

		final String sql = "SELECT M_Warehouse_ID, WarehouseName, "
			+ "SUM(QtyAvailable) AS QtyAvailable, SUM(QtyOnHand) AS QtyOnHand, "
			+ "SUM(QtyReserved) AS QtyReserved, SUM(QtyOrdered) AS QtyOrdered "
			+ "FROM M_PRODUCT_STOCK_V "
			+ "WHERE M_Product_ID = ? "
			+ "AND (QtyOnHand <> 0 OR QtyAvailable <> 0 OR QtyReserved <> 0 OR QtyOrdered <> 0) "
			+ "GROUP BY M_Warehouse_ID, WarehouseName "
			+ "ORDER BY SUM(QtyOnHand) DESC, WarehouseName "
		;

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			request.getProductId()
		);

		int count = CountUtil.countRecords(sql, "M_PRODUCT_STOCK_V", filtersList);

		ListWarehouseStocksResponse.Builder builderList = ListWarehouseStocksResponse.newBuilder()
			.setRecordCount(count)
		;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				WarehouseStock.Builder builder = ProductInfoConvert.convertWarehouseStock(rs);
				builderList.addRecords(builder);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	public static ListSubstituteProductsResponse.Builder listSubstituteProducts(ListSubstituteProductsRequest request) {
		ListSubstituteProductsResponse.Builder builderList = ListSubstituteProductsResponse.newBuilder();

		if (request.getProductId() <= 0 || request.getPriceListVersionId() <= 0) {
			return builderList;
		}

		final String sql = "SELECT Substitute_ID, OrgName AS Warehouse, "
			+ "Value, Name, Description, "
			+ "QtyAvailable, QtyOnHand, QtyReserved, PriceStd "
			+ "FROM M_PRODUCT_SUBSTITUTERELATED_V "
			+ "WHERE M_Product_ID = ? AND M_PriceList_Version_ID = ? AND RowType = 'S' "
		;

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			request.getProductId()
		);
		filtersList.add(
			request.getPriceListVersionId()
		);

		int count = CountUtil.countRecords(sql, "M_PRODUCT_SUBSTITUTERELATED_V", filtersList);
		builderList.setRecordCount(count);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				SubstituteProduct.Builder builder = ProductInfoConvert.convertSubstituteProduct(rs);
				builderList.addRecords(builder);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	public static ListRelatedProductsResponse.Builder listRelatedProducts(ListRelatedProductsRequest request) {
		ListRelatedProductsResponse.Builder builderList = ListRelatedProductsResponse.newBuilder();

		if (request.getProductId() <= 0 || request.getPriceListVersionId() <= 0) {
			return builderList;
		}

		final String sql = "SELECT Substitute_ID, OrgName AS Warehouse, "
			+ "Value, Name, Description, "
			+ "QtyAvailable, QtyOnHand, QtyReserved, PriceStd "
			+ "FROM M_PRODUCT_SUBSTITUTERELATED_V "
			+ "WHERE M_Product_ID = ? AND M_PriceList_Version_ID = ? AND RowType = 'R' "
		;

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			request.getProductId()
		);
		filtersList.add(
			request.getPriceListVersionId()
		);

		int count = CountUtil.countRecords(sql, "M_PRODUCT_SUBSTITUTERELATED_V", filtersList);
		builderList.setRecordCount(count);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				RelatedProduct.Builder builder = ProductInfoConvert.convertRelatedProduct(rs);
				builderList.addRecords(builder);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return builderList;
	}



	public static ListAvailableToPromisesResponse.Builder listAvailableToPromises(ListAvailableToPromisesRequest request) {
		validateAndGetProduct(
			request.getProductId()
		);

		boolean isShowDetail = request.getIsShowDetail();
		int productId = request.getProductId();
		int warehouseId = request.getWarehouseId();

		//	Create the SELECT ..UNION. clause
		//  This is done in-line rather than using prepareTable() so we can add a running sum to the data.
		String sql;
		if (!isShowDetail) {
			sql = "(SELECT s.M_Product_ID, w.Name AS warehouse, l.value AS locator, 0 AS ID, null AS Date,"
				+ " sum(s.QtyOnHand) AS AvailQty, 0 AS DeltaQty, 0 AS QtyOrdered, 0 AS QtyReserved,"
				+ " null AS sumPASI," // " s.PASI,"
				+ " 0 AS ASI,"
				+ " null AS BP_Name, null AS DocumentNo, 10 AS SeqNo";
		}
		else {
			sql = "(SELECT s.M_Product_ID, w.Name AS warehouse, l.value AS locator, s.M_AttributeSetInstance_ID AS ID, null AS Date,"
				+ " s.QtyOnHand AS AvailQty, 0 AS DeltaQty, 0 AS QtyOrdered, 0 AS QtyReserved,"
				+ " CASE WHEN s.PASI  = '' THEN '{' || COALESCE(s.M_AttributeSetInstance_ID,0) || '}' ELSE s.PASI END AS sumPASI,"
				+ " COALESCE(M_AttributeSetInstance_ID,0) AS ASI,"
				+ " null AS BP_Name, null AS DocumentNo,  10 AS SeqNo";
		}
		sql += " FROM (SELECT M_Product_ID, M_Locator_ID, QtyOnHand, QtyReserved, QtyOrdered,"
			+ 		 " COALESCE(productAttribute(M_AttributeSetInstance_ID)::varchar, '') AS PASI,"
			+		 " COALESCE(M_AttributeSetInstance_ID,0) AS M_AttributeSetInstance_ID FROM M_Storage) s "
			+ " INNER JOIN M_Locator l ON (s.M_Locator_ID=l.M_Locator_ID)"
			+ " INNER JOIN M_Warehouse w ON (l.M_Warehouse_ID=w.M_Warehouse_ID)"
			+ " AND s.M_Product_ID=" + productId;
		if (warehouseId > 0) {
			sql += " AND l.M_Warehouse_ID=" + warehouseId;
		}
		//if (m_M_AttributeSetInstance_ID > 0)
		//	sql += " AND s.M_AttributeSetInstance_ID=?";
		if (!isShowDetail) {
			//sql += " AND (s.QtyOnHand<>0)";
			sql += " GROUP BY s.M_Product_ID, w.Name, l.value, s.M_Locator_ID, sumPASI, ASI, BP_Name, DocumentNo, SeqNo ";
		}
		sql += " UNION ALL ";

		//	Orders
		sql += "SELECT ol.M_Product_ID, w.Name AS warehouse, null AS locator, ol.M_AttributeSetInstance_ID AS ID, o.DatePromised AS date,"
			+ " 0 AS AvailQty,"
			+ " ol.QtyDelivered  AS DeltaQty,"
			+ " CASE WHEN o.IsSOTrx = 'N' THEN ol.QtyReserved ELSE 0 END AS QtyOrdered,"
			+ " CASE WHEN o.IsSOTrx = 'Y' THEN ol.QtyReserved ELSE 0 END AS QtyReserved,"
			+ " productAttribute(ol.M_AttributeSetInstance_ID) AS sumPASI,"
			+ " ol.M_AttributeSetInstance_ID AS ASI,"
			+ " bp.Name AS BP_Name, dt.PrintName || ' ' || o.DocumentNo AS DocumentNo, 20 AS SeqNo "
			+ "FROM C_Order o"
			+ " INNER JOIN C_OrderLine ol ON (o.C_Order_ID=ol.C_Order_ID)"
			+ " INNER JOIN C_DocType dt ON (o.C_DocType_ID=dt.C_DocType_ID)"
			+ " INNER JOIN M_Warehouse w ON (ol.M_Warehouse_ID=w.M_Warehouse_ID)"
			+ " INNER JOIN C_BPartner bp  ON (o.C_BPartner_ID=bp.C_BPartner_ID) "
			+ "WHERE ol.QtyReserved<>0 AND o.DocStatus in ('IP','CO')"
			+ " AND ol.M_Product_ID=" + productId;
		if (warehouseId > 0) {
			sql += " AND w.M_Warehouse_ID=" + warehouseId;
		}
		//if (m_M_AttributeSetInstance_ID > 0) {
		//	sql += " AND ol.M_AttributeSetInstance_ID=?";
		//}
		//sql += " ORDER BY M_Product_ID, SeqNo, ID, date, locator";

		sql += " UNION ALL ";
		//	Manufacturing Orders Ordered
		sql += "SELECT o.M_Product_ID, w.Name AS warehouse, null AS locator, o.M_AttributeSetInstance_ID AS ID, o.DatePromised AS date,"
				+ " 0 AS AvailQty,"
				+ " o.QtyDelivered AS DeltaQty,"
				+ " o.QtyOrdered AS QtyOrdered,"
				+ " 0 AS QtyReserved,"
				+ " productAttribute(o.M_AttributeSetInstance_ID) AS sumPASI,"
				+ " o.M_AttributeSetInstance_ID AS ASI,"
				+ " null AS BP_Name, dt.PrintName || ' ' || o.DocumentNo As DocumentNo, 30 AS SeqNo "
				+ "FROM PP_Order o"
				+ " INNER JOIN C_DocType dt ON (o.C_DocType_ID=dt.C_DocType_ID)"
				+ " INNER JOIN M_Warehouse w ON (o.M_Warehouse_ID=w.M_Warehouse_ID)"
				+ "WHERE o.DocStatus in ('IP','CO')"
				+ " AND o.M_Product_ID=" + productId;
		if (warehouseId > 0) {
			sql += " AND w.M_Warehouse_ID=" + warehouseId;
		}

		sql += " UNION ALL ";
		//	Manufacturing Order Reserved
		sql += "SELECT ol.M_Product_ID, w.Name AS warehouse, null AS locator, ol.M_AttributeSetInstance_ID AS ID, o.DatePromised AS date,"
				+ " 0 AS AvailQty,"
				+ " ol.QtyDelivered  AS DeltaQty,"
				+ " 0 AS QtyOrdered,"
				+ " ol.QtyReserved AS QtyReserved,"
				+ " productAttribute(ol.M_AttributeSetInstance_ID) AS sumPASI,"
				+ " ol.M_AttributeSetInstance_ID AS ASI,"
				+ " null AS BP_Name, dt.PrintName || ' ' || o.DocumentNo As DocumentNo, 40 AS SeqNo "
				+ "FROM PP_Order o"
				+ " INNER JOIN PP_Order_BOMLine ol ON (o.PP_Order_ID=ol.PP_Order_ID)"
				+ " INNER JOIN C_DocType dt ON (o.C_DocType_ID=dt.C_DocType_ID)"
				+ " INNER JOIN M_Warehouse w ON (ol.M_Warehouse_ID=w.M_Warehouse_ID)"
				+ "WHERE ol.QtyReserved<>0 AND o.DocStatus in ('IP','CO')"
				+ " AND ol.M_Product_ID=" + productId;
		if (warehouseId > 0) {
			sql += " AND w.M_Warehouse_ID=" + warehouseId;
		}

		sql += " UNION ALL ";
		
		//	Distribution Orders Reserved
		sql += "SELECT ol.M_Product_ID, wf.Name AS warehouse, lf.value AS locator, ol.M_AttributeSetInstance_ID AS ID, ol.DatePromised AS date,"
			+ " 0 AS AvailQty,"
			+ " ol.QtyInTransit AS DeltaQty,"
			+ " 0 AS QtyOrdered,"
			+ " ol.QtyReserved AS QtyReserved,"
			+ " productAttribute(ol.M_AttributeSetInstance_ID) AS sumPASI,"
			+ " ol.M_AttributeSetInstance_ID AS ASI,"
			+ " bp.Name AS BP_Name, dt.PrintName || ' ' || o.DocumentNo As DocumentNo, 50 AS SeqNo "
			+ "FROM DD_Order o"
			+ " INNER JOIN DD_OrderLine ol ON (o.DD_Order_ID=ol.DD_Order_ID)"
			+ " INNER JOIN C_DocType dt ON (o.C_DocType_ID=dt.C_DocType_ID)"
			+ " INNER JOIN M_Locator lf on (lf.M_Locator_ID = ol.M_Locator_ID)"
			+ " INNER JOIN M_Warehouse wf ON (lf.M_Warehouse_ID=wf.M_Warehouse_ID)"
			+ " INNER JOIN C_BPartner bp  ON (o.C_BPartner_ID = bp.C_BPartner_ID) "
			+ "WHERE ol.QtyReserved<>0 AND o.DocStatus in ('IP','CO') AND o.IsDelivered = 'N'"
			+ " AND ol.M_Product_ID=" + productId;
		if (warehouseId > 0) {
			sql += " AND wf.M_Warehouse_ID=" + warehouseId;
		}
		//if (m_M_AttributeSetInstance_ID > 0)
		//	sql += " AND ol.M_AttributeSetInstance_ID=?";

		sql += " UNION ALL ";
		
		//	Distribution Orders Ordered
		sql += "SELECT ol.M_Product_ID, w.Name AS warehouse, l.value AS locator, ol.M_AttributeSetInstanceTo_ID AS ID, ol.DatePromised AS date,"
			+ " 0 AS AvailQty,"
			+ " ol.QtyDelivered AS DeltaQty,"
			+ " ol.QtyOrdered AS QtyOrdered,"
			+ " 0 AS QtyReserved,"
			+ " productAttribute(ol.M_AttributeSetInstanceTo_ID) AS sumPASI,"
			+ " ol.M_AttributeSetInstanceTo_ID AS ASI,"
			+ " bp.Name AS BP_Name, dt.PrintName || ' ' || o.DocumentNo As DocumentNo, 60 AS SeqNo "
			+ "FROM DD_Order o"
			+ " INNER JOIN DD_OrderLine ol ON (o.DD_Order_ID=ol.DD_Order_ID)"
			+ " INNER JOIN C_DocType dt ON (o.C_DocType_ID=dt.C_DocType_ID)"
			+ " INNER JOIN M_Locator l ON (l.M_Locator_ID = ol.M_LocatorTo_ID)"
			+ " INNER JOIN M_Warehouse w ON (l.M_Warehouse_ID=w.M_Warehouse_ID)"
			+ " INNER JOIN C_BPartner bp  ON (o.C_BPartner_ID = bp.C_BPartner_ID) "
			+ "WHERE ol.QtyOrdered - ol.Qtydelivered > 0 AND o.DocStatus in ('IP','CO') AND o.IsDelivered='N'" 
			+ " AND ol.M_Product_ID=" + productId;
		if (warehouseId > 0) {
			sql += " AND w.M_Warehouse_ID=" + warehouseId;
		}
		//if (m_M_AttributeSetInstance_ID > 0) {
		//	sql += " AND ol.M_AttributeSetInstance_ID=?";
		//}
		sql += " ORDER BY M_Product_ID, SeqNo, ID, date, locator)";

		// List<Object> filtersList = new ArrayList<>();
		// filtersList.add(
		// 	request.getProductId()
		// );

		// int count = CountUtil.countRecords(sql, "M_PRODUCT_STOCK_V", filtersList);

		ListAvailableToPromisesResponse.Builder builderList = ListAvailableToPromisesResponse.newBuilder();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int countRecords = 0;
		// Running Available to Promise balance per warehouse (ledger, like ZK).
		// Rows come ordered as stock first and then documents, so the balance
		// accumulates the per-row ATP (on hand + ordered - reserved) and is
		// reset for each warehouse.
		Map<String, BigDecimal> runningBalanceByWarehouse = new HashMap<String, BigDecimal>();
		try {
			pstmt = DB.prepareStatement(sql, null);
			// ParameterUtil.setParametersFromObjectsList(pstmt, filtersList);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				AvailableToPromise.Builder builder = ProductInfoConvert.convertAvaliableToPromise(rs);

				String warehouseKey = builder.getName();
				BigDecimal rowAvailableToPromise = Optional.ofNullable(
					NumberManager.getBigDecimalFromString(
						builder.getAvailableToPromiseQuantity()
					)
				).orElse(BigDecimal.ZERO);
				BigDecimal runningBalance = runningBalanceByWarehouse.getOrDefault(
					warehouseKey, BigDecimal.ZERO
				).add(rowAvailableToPromise);
				runningBalanceByWarehouse.put(warehouseKey, runningBalance);
				builder.setAvailableQuantity(
					NumberManager.getBigDecimalToString(runningBalance)
				);

				builderList.addRecords(builder);
				countRecords++;
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		builderList.setRecordCount(countRecords);

		return builderList;
	}



	public static ListVendorPurchasesResponse.Builder listVendorPurchases(ListVendorPurchasesRequest request) {
		validateAndGetProduct(
			request.getProductId()
		);

		List<Object> filtersList = new ArrayList<>();
		filtersList.add(
			request.getProductId()
		);

		List<MProductPO> productVendorsList = Arrays.asList(
			MProductPO.getOfProduct(Env.getCtx(), request.getProductId(), null)
		);
		if (productVendorsList == null || productVendorsList.isEmpty()) {
			return ListVendorPurchasesResponse.newBuilder();
		}
		int countRecords = productVendorsList.size();

		ListVendorPurchasesResponse.Builder builderList = ListVendorPurchasesResponse.newBuilder()
			.setRecordCount(countRecords)
		;
		productVendorsList.forEach(productVendor -> {
			VendorPurchase.Builder builder = ProductInfoConvert.convertVendorPurchase(productVendor);
			builderList.addRecords(builder);
		});

		return builderList;
	}

}
