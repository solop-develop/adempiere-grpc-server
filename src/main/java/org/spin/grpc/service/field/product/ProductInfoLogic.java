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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.core.domains.models.I_M_AttributeSet;
import org.adempiere.core.domains.models.I_M_AttributeSetInstance;
import org.adempiere.core.domains.models.I_M_PriceList_Version;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_Product_Category;
import org.adempiere.core.domains.models.I_M_Product_Class;
import org.adempiere.core.domains.models.I_M_Product_PO;
import org.adempiere.core.domains.models.I_M_Warehouse;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLookupInfo;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.field.product.ListAttributeSetInstancesRequest;
import org.spin.backend.grpc.field.product.ListAttributeSetsRequest;
import org.spin.backend.grpc.field.product.ListAvailableToPromisesRequest;
import org.spin.backend.grpc.field.product.ListAvailableToPromisesResponse;
import org.spin.backend.grpc.field.product.ListPricesListVersionsRequest;
import org.spin.backend.grpc.field.product.ListProductCategoriesRequest;
import org.spin.backend.grpc.field.product.ListProductClasessRequest;
import org.spin.backend.grpc.field.product.ListProductGroupsRequest;
import org.spin.backend.grpc.field.product.ListProductsInfoRequest;
import org.spin.backend.grpc.field.product.ListProductsInfoResponse;
import org.spin.backend.grpc.field.product.ListVendorPurchasesRequest;
import org.spin.backend.grpc.field.product.ListVendorPurchasesResponse;
import org.spin.backend.grpc.field.product.ListVendorsRequest;
import org.spin.backend.grpc.field.product.ListWarehouseStocksRequest;
import org.spin.backend.grpc.field.product.ListWarehouseStocksResponse;
import org.spin.backend.grpc.field.product.ListWarehousesRequest;
import org.spin.backend.grpc.field.product.ProductInfo;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.ContextManager;
import org.spin.base.util.ReferenceInfo;
import org.spin.grpc.service.UserInterface;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.ValueManager;

public class ProductInfoLogic {

	public static String tableName = I_M_Product.Table_Name;


	public static ListLookupItemsResponse.Builder listWarehouses(ListWarehousesRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_Warehouse.COLUMNNAME_M_Warehouse_ID, I_M_Warehouse.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}

	public static ListLookupItemsResponse.Builder listPricesListVersions(ListPricesListVersionsRequest request) {
		// Warehouse
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			DisplayType.TableDir,
			0, 0, 0,
			0,
			I_M_PriceList_Version.COLUMNNAME_M_PriceList_Version_ID, I_M_PriceList_Version.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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
			I_M_Product_Category.COLUMNNAME_M_Product_Category_ID, I_M_Product_Category.Table_Name
		);

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
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

		ListLookupItemsResponse.Builder builderList = UserInterface.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue(),
			true
		);

		return builderList;
	}



	public static ListProductsInfoResponse.Builder listProductsInfo(ListProductsInfoRequest request) {
		MLookupInfo reference = ReferenceInfo.getInfoFromRequest(
			request.getReferenceId(),
			request.getFieldId(),
			request.getProcessParameterId(),
			request.getBrowseFieldId(),
			request.getColumnId(),
			request.getColumnName(),
			request.getTableName()
		);

		//  Fill Cintext
		Properties context = Env.getCtx();
		int windowNo = ThreadLocalRandom.current().nextInt(1, 8996 + 1);
		ContextManager.setContextWithAttributesFromString(windowNo, context, request.getContextAttributes());

		StringBuffer whereClause = new StringBuffer("1=1");
		// validation code of field
		String validationCode = WhereClauseUtil.getWhereRestrictionsWithAlias(tableName, reference.ValidationCode);
		String parsedValidationCode = Env.parseContext(context, windowNo, validationCode, false);
		if (!Util.isEmpty(reference.ValidationCode, true)) {
			if (Util.isEmpty(parsedValidationCode, true)) {
				throw new AdempiereException("@WhereClause@ @Unparseable@");
			}
			whereClause.append(" AND ").append(parsedValidationCode);
		}

		List<Object> parametersList = new ArrayList<>();

		Query query = new Query(
			context,
			I_M_Product.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parametersList)
			.setOrderBy("Value")
		;

		//	Get page and count
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		// Set page token
		String nexPageToken = null;
		int recordCount = query.count();
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListProductsInfoResponse.Builder builderList = ListProductsInfoResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				ValueManager.validateNull(
					nexPageToken
				)
			)
		;

		// query.setLimit(limit, offset)
		// 	.getIDsAsList()
		// 	.stream().forEach(productId -> {
		// 		ProductInfo.Builder builder = ProductInfo.newBuilder(); // BusinessPartnerConvert.convertBusinessPartner(businessPartnerId);
		// 		builderList.addRecords(builder);
		// 	})
		// ;

		String sql = "SELECT "
			+ "p.M_Product_ID, p.UUID, p.Discontinued, pc.Name AS M_Product_Category_ID, "
			+ "p.Value, p.Name, p.UPC, p.SKU, u.name AS C_UOM_ID, bp.Name AS Vendor, "
			+ "pa.IsInstanceAttribute AS IsInstanceAttribute"
			+ "FROM M_Product AS P"
		;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				ProductInfo.Builder builder = ProductInfo.newBuilder()
					.setId(
						rs.getInt("M_Product_ID")
					)
					.setUuid(
						ValueManager.validateNull(
							rs.getString("UUID")
						)
					)
					.setValue(
						ValueManager.validateNull(
							rs.getString("Value")
						)
					)
				;
				builderList.addRecords(builder);
			}
		} catch (SQLException e) {
			throw new AdempiereException(e);
		} finally {
			DB.close(pstmt);
			DB.close(rs);
		}

		return builderList;
	}



	public static ListWarehouseStocksResponse.Builder listWarehouseStocks(ListWarehouseStocksRequest request) {
		ListWarehouseStocksResponse.Builder builderList = ListWarehouseStocksResponse.newBuilder();

		return builderList;
	}



	public static ListAvailableToPromisesResponse.Builder listAvailableToPromises(ListAvailableToPromisesRequest request) {
		ListAvailableToPromisesResponse.Builder builderList = ListAvailableToPromisesResponse.newBuilder();

		return builderList;
	}



	public static ListVendorPurchasesResponse.Builder listVendorPurchases(ListVendorPurchasesRequest request) {
		ListVendorPurchasesResponse.Builder builderList = ListVendorPurchasesResponse.newBuilder();

		return builderList;
	}

}
