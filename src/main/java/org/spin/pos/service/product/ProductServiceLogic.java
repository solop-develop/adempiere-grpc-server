package org.spin.pos.service.product;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPOS;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.backend.grpc.core_functionality.ProductPrice;
import org.spin.backend.grpc.pos.GetProductPriceRequest;
import org.spin.backend.grpc.pos.ListProductPriceRequest;
import org.spin.backend.grpc.pos.ListProductPriceResponse;
import org.spin.pos.service.pos.POS;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

public class ProductServiceLogic {

	/**
	 * Get Product Price Method
	 * @param request
	 * @return
	 */
	public static ProductPrice.Builder getProductPrice(GetProductPriceRequest request) {
		//	Get Product
		MProduct product = null;

		// URL decode to change characteres and add search value to filter
		final String searchValue = TextManager.getValidString(
			TextManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if(!Util.isEmpty(searchValue, true)) {
			List<Object> exactParameters = new ArrayList<Object>();
			exactParameters.add(searchValue);
			exactParameters.add(searchValue);
			exactParameters.add(searchValue);

			product = new Query(
				Env.getCtx(),
				I_M_Product.Table_Name,
				"("
				+ "UPPER(TRIM(Value)) = UPPER(?) "
				+ "OR UPPER(TRIM(UPC)) = UPPER(?) "
				+ "OR UPPER(TRIM(SKU)) = UPPER(?)"
				+ ")",
				null
			)
				.setParameters(exactParameters)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setOrderBy(I_M_Product.COLUMNNAME_Value)
				.first();

			if(product == null) {
				List<Object> parameters = new ArrayList<Object>();
				parameters.add(searchValue);
				parameters.add(searchValue);
				parameters.add(searchValue);
				parameters.add(searchValue);

				product = new Query(
					Env.getCtx(),
					I_M_Product.Table_Name,
					"("
					+ "UPPER(Value) LIKE '%' || UPPER(?) || '%'"
					+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%'"
					+ "OR UPPER(UPC) LIKE '%' || UPPER(?) || '%'"
					+ "OR UPPER(SKU) LIKE '%' || UPPER(?) || '%'"
					+ ")",
					null
				)
					.setParameters(parameters)
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.setOrderBy(I_M_Product.COLUMNNAME_Value)
					.first();
			}
		} else if(!Util.isEmpty(request.getUpc(), true)) {
			if(product == null) {
				product = new Query(
					Env.getCtx(),
					I_M_Product.Table_Name,
					"UPPER(TRIM(UPC)) = UPPER(TRIM(?))",
					null
				)
					.setParameters(request.getUpc())
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.first();
			}
		} else if(!Util.isEmpty(request.getSku(), true)) {
			if(product == null) {
				product = new Query(
					Env.getCtx(),
					I_M_Product.Table_Name,
					"UPPER(TRIM(SKU)) = UPPER(TRIM(?))",
					null
				)
					.setParameters(request.getSku())
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.first();
			}
		} else if(!Util.isEmpty(request.getValue(), true)) {
			if(product == null) {
				product = new Query(
					Env.getCtx(),
					I_M_Product.Table_Name,
					"UPPER(TRIM(Value)) = UPPER(TRIM(?))",
					null
				)
					.setParameters(request.getValue())
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.first();
			}
		} else if(!Util.isEmpty(request.getName(), true)) {
			if(product == null) {
				product = new Query(
					Env.getCtx(),
					I_M_Product.Table_Name,
					"UPPER(Name) LIKE UPPER(?)",
					null
				)
					.setParameters(request.getName())
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.first();
			}
		}
		//	Validate product
		if(product == null) {
			throw new AdempiereException("@M_Product_ID@ @NotFound@");
		}
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		//	Validate Price List
		int priceListId = pos.getM_PriceList_ID();
		if(request.getPriceListId() > 0) {
			priceListId = request.getPriceListId();
		}
		MPriceList priceList = MPriceList.get(Env.getCtx(), priceListId, null);
		AtomicInteger warehouseId = new AtomicInteger(pos.getM_Warehouse_ID());
		if(request.getWarehouseId() > 0) {
			warehouseId.set(request.getWarehouseId());
		}
		//	Get Valid From
		AtomicReference<Timestamp> validFrom = new AtomicReference<>();
		if(!Util.isEmpty(request.getValidFrom())) {
			validFrom.set(
				TimeManager.getTimestampFromString(
					request.getValidFrom()
				)
			);
		} else {
			validFrom.set(TimeUtil.getDay(System.currentTimeMillis()));
		}
		int businessPartnerId = request.getBusinessPartnerId();
		int displayCurrencyId = pos.get_ValueAsInt("DisplayCurrency_ID");
		int conversionTypeId = pos.get_ValueAsInt("C_ConversionType_ID");
		return ProductConvertUtil.convertProductPrice(
			product,
			businessPartnerId,
			priceList,
			warehouseId.get(),
			validFrom.get(),
			displayCurrencyId,
			conversionTypeId,
			Env.ONE
		);
	}

	/**
	 * Get Product Price Method
	 * @param context
	 * @param request
	 * @return
	 */
	public static ListProductPriceResponse.Builder listProductsPrices(ListProductPriceRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		//	Validate Price List
		int priceListId = pos.getM_PriceList_ID();
		if(request.getPriceListId() > 0) {
			priceListId = request.getPriceListId();
		}
		MPriceList priceList = MPriceList.get(Env.getCtx(), priceListId, null);

		//	Get Valid From
		final Timestamp validFrom = TimeUtil.getDay(
			TimeManager.getTimestampFromString(
				request.getValidFrom()
			)
		);

		// URL decode to change characteres and add search value to filter
		final String searchValue = TextManager.getValidString(
			TextManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		final boolean isSearching = !Util.isEmpty(searchValue, true);
		final int businessPartnerId = request.getBusinessPartnerId();

		AtomicInteger warehouseId = new AtomicInteger(pos.getM_Warehouse_ID());
		if(request.getWarehouseId() > 0) {
			warehouseId.set(request.getWarehouseId());
		}
		int displayCurrencyId = pos.get_ValueAsInt("DisplayCurrency_ID");
		int conversionTypeId = pos.get_ValueAsInt("C_ConversionType_ID");
		//	Get Product list, exact code matches first and partial matches when there are none
		Query query = getProductsQuery(
			searchValue,
			isSearching,
			businessPartnerId,
			priceList.getM_PriceList_ID(),
			validFrom
		);
		if(isSearching && query.count() <= 0) {
			query = getProductsQuery(
				searchValue,
				false,
				businessPartnerId,
				priceList.getM_PriceList_ID(),
				validFrom
			);
		}

		//	Set Pagination
		int count = query.count();
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListProductPriceResponse.Builder builder = ListProductPriceResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				TextManager.getValidString(nexPageToken)
			)
		;

		query
			.setLimit(limit, offset)
			.<MProduct>list()
			.forEach(product -> {
				ProductPrice.Builder productPrice = ProductConvertUtil.convertProductPrice(
					product,
					businessPartnerId,
					priceList,
					warehouseId.get(),
					validFrom,
					displayCurrencyId,
					conversionTypeId,
					null
				);
				builder.addProductPrices(productPrice);
			})
		;

		return builder;
	}

	/**
	 * Build the products query, matching the search value exactly (Value, UPC or SKU) or partially
	 * @param searchValue
	 * @param exactMatch
	 * @param businessPartnerId
	 * @param priceListId
	 * @param validFrom
	 * @return
	 */
	private static Query getProductsQuery(String searchValue, boolean exactMatch, int businessPartnerId, int priceListId, Timestamp validFrom) {
		//	Dynamic where clause
		final String codeOperator = exactMatch ? "=" : "LIKE";
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("IsSold = 'Y' ");
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append(
				"AND ("
			);

			if(exactMatch) {
				whereClause.append(
					"("
						+ "UPPER(TRIM(Value)) = UPPER(?) "
						+ "OR UPPER(TRIM(UPC)) = UPPER(?) "
						+ "OR UPPER(TRIM(SKU)) = UPPER(?) "
					+ ") "
				);
				parameters.add(searchValue);
				parameters.add(searchValue);
				parameters.add(searchValue);
			} else {
				whereClause.append(
					"("
						+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
						+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
						+ "OR UPPER(TRIM(UPC)) = UPPER(?) "
						+ "OR UPPER(TRIM(SKU)) = UPPER(?) "
					+ ") "
				);
				parameters.add(searchValue);
				parameters.add(searchValue);
				parameters.add(searchValue);
				parameters.add(searchValue);
			}

			// Find on Product Purchase
			whereClause.append(
					"OR EXISTS("
						+ "SELECT 1 FROM M_Product_PO AS po "
						+ "WHERE po.IsActive = 'Y' "
						+ "AND po.M_Product_ID = M_Product.M_Product_ID "
						+ "AND ("
							+ "UPPER(TRIM(po.UPC)) " + codeOperator + " UPPER(?) "
							// + "OR UPPER(po.VendorProductNo) LIKE UPPER(?) "
						+ ") "
					+ ")"
			);
			parameters.add(searchValue);
			// parameters.add(searchValue);

			// Find on Product Info
			MTable productTable = MTable.get(Env.getCtx(), "M_Product_Info");
			if (productTable != null && productTable.get_ID() > 0) {
				whereClause.append(
					"OR EXISTS("
						+ "SELECT 1 FROM M_Product_Info AS pi "
						+ "WHERE pi.IsActive = 'Y' "
						+ "AND pi.M_Product_ID = M_Product.M_Product_ID "
						+ "AND ("
							+ "UPPER(TRIM(pi.UPC)) " + codeOperator + " UPPER(?) "
							// + "OR UPPER(pi.VendorProductNo) LIKE UPPER(?) "
						+ ") "
					+ ")"
				);
				parameters.add(searchValue);
				// parameters.add(searchValue);
			}

			whereClause.append(
				") "
			);
		}

		//	Add Price List
		whereClause.append(
			"AND EXISTS("
				+ "SELECT 1 FROM M_PriceList_Version AS plv "
				+ "INNER JOIN M_ProductPrice AS pp "
				+ "ON(pp.M_PriceList_Version_ID = plv.M_PriceList_Version_ID) "
				+ "WHERE plv.M_PriceList_ID = ? "
				+ "AND plv.ValidFrom <= ? "
				+ "AND plv.IsActive = 'Y' "
				// + "AND pp.PriceList IS NOT NULL AND pp.PriceList > 0 "
				// + "AND pp.PriceStd IS NOT NULL AND pp.PriceStd > 0 "
				+ "AND pp.M_Product_ID = M_Product.M_Product_ID"
			+ ")"
		);

		if(businessPartnerId > 0) {
			/*
			whereClause.append(
				"AND ("
			);

			// Find on Product Purchase
			whereClause.append(
					"OR EXISTS("
						+ "SELECT 1 FROM M_Product_PO AS po "
						+ "WHERE po.IsActive = 'Y' "
						+ "AND po.M_Product_ID = M_Product.M_Product_ID "
						+ "AND po.C_BPartner_ID = ? "
					+ ")"
			);
			parameters.add(businessPartnerId);

			// Find on Product Info
			MTable productTable = MTable.get(Env.getCtx(), "M_Product_Info");
			if (productTable != null && productTable.get_ID() > 0) {
				whereClause.append(
					"OR EXISTS("
						+ "SELECT 1 FROM M_Product_Info AS pi "
						+ "WHERE pi.IsActive = 'Y' "
						+ "AND pi.M_Product_ID = M_Product.M_Product_ID "
						+ "AND pi.C_BPartner_ID = ? "
					+ ")"
				);
				parameters.add(businessPartnerId);
			}

			whereClause.append(
				") "
			);
			*/
		}

		//	Add parameters
		parameters.add(priceListId);
		parameters.add(validFrom);
		return new Query(
			Env.getCtx(),
			I_M_Product.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(I_M_Product.COLUMNNAME_Value)
		;
	}

}
