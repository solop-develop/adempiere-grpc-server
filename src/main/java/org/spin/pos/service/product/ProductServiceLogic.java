package org.spin.pos.service.product;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPOS;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
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
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

public class ProductServiceLogic {
	

	/**
	 * Get Product Price Method
	 * @param request
	 * @return
	 */
	public static ProductPrice.Builder getProductPrice(GetProductPriceRequest request) {
		//	Get Product
		MProduct product = null;

		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			List<Object> parameters = new ArrayList<Object>();
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);

			product = new Query(
				Env.getCtx(),
				I_M_Product.Table_Name,
				"("
				+ "UPPER(Value) = UPPER(?)"
				+ "OR UPPER(Name) = UPPER(?)"
				+ "OR UPPER(UPC) = UPPER(?)"
				+ "OR UPPER(SKU) = UPPER(?)"
				+ ")",
				null
			)
				.setParameters(parameters)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.first();
		} else if(!Util.isEmpty(request.getUpc(), true)) {
			if(product == null) {
				Optional<MProduct> optionalProduct = MProduct.getByUPC(Env.getCtx(), request.getUpc(), null).stream().findAny();
				if(optionalProduct.isPresent()) {
					product = optionalProduct.get();
				}
			}
		} else if(!Util.isEmpty(request.getSku(), true)) {
			if(product == null) {
				product = new Query(
					Env.getCtx(),
					I_M_Product.Table_Name,
					"UPPER(SKU) = UPPER(?)",
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
					"UPPER(Value) = UPPER(?)",
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

		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("IsSold = 'Y' ");
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		//	For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append(
				"AND ("
					+ "("
						+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
						+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
						+ "OR UPPER(UPC) = UPPER(?) "
						+ "OR UPPER(SKU) = UPPER(?) "
					+ ") "
					+ "OR EXISTS("
						+ "SELECT 1 FROM M_Product_PO AS po "
						+ "WHERE po.IsActive = 'Y' "
						+ "AND po.M_Product_ID = M_Product.M_Product_ID "
						+ "AND UPPER(po.UPC) LIKE UPPER(?) "
					+ ")"
				+ ") "
			);
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
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
		//	Add parameters
		parameters.add(priceList.getM_PriceList_ID());
		parameters.add(validFrom);
		AtomicInteger warehouseId = new AtomicInteger(pos.getM_Warehouse_ID());
		if(request.getWarehouseId() > 0) {
			warehouseId.set(request.getWarehouseId());
		}
		int businessPartnerId = request.getBusinessPartnerId();
		int displayCurrencyId = pos.get_ValueAsInt("DisplayCurrency_ID");
		int conversionTypeId = pos.get_ValueAsInt("C_ConversionType_ID");
		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			I_M_Product.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;

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
				StringManager.getValidString(nexPageToken)
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

}
