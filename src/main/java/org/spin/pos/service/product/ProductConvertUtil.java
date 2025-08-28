package org.spin.pos.service.product;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPricing;
import org.compiere.model.MStorage;
import org.compiere.model.MTax;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.core_functionality.ProductPrice;
import org.spin.base.util.RecordUtil;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.TimeManager;

public class ProductConvertUtil {

	/**
	 * Get 
	 * @param product
	 * @param businessPartnerId
	 * @param priceList
	 * @param warehouseId
	 * @param validFrom
	 * @param quantity
	 * @return
	 */
	public static ProductPrice.Builder convertProductPrice(MProduct product, int businessPartnerId, MPriceList priceList, int warehouseId, Timestamp validFrom, int displayCurrencyId, int conversionTypeId, BigDecimal priceQuantity) {
		ProductPrice.Builder builder = ProductPrice.newBuilder();
		//	Get Price
		MProductPricing productPricing = new MProductPricing(product.getM_Product_ID(), businessPartnerId, priceQuantity, true, null);
		productPricing.setM_PriceList_ID(priceList.getM_PriceList_ID());
		productPricing.setPriceDate(validFrom);
		builder.setProduct(
			CoreFunctionalityConvert.convertProduct(product)
		);
		int taxCategoryId = product.getC_TaxCategory_ID();
		Optional<MTax> optionalTax = Arrays.asList(MTax.getAll(Env.getCtx()))
			.parallelStream()
			.filter(tax -> {
				return tax.getC_TaxCategory_ID() == taxCategoryId 
					&& (
						tax.isSalesTax() 
						|| (!Util.isEmpty(tax.getSOPOType()
					)
					&& (
						tax.getSOPOType().equals(MTax.SOPOTYPE_Both)
						|| tax.getSOPOType().equals(MTax.SOPOTYPE_SalesTax))
					)
				)
			;
		})
			.findFirst()
		;
		//	Validate
		if(optionalTax.isPresent()) {
			builder.setTaxRate(CoreFunctionalityConvert.convertTaxRate(optionalTax.get()));
		}
		//	Set currency
		builder.setCurrency(CoreFunctionalityConvert.convertCurrency(priceList.getC_Currency_ID()));
		//	Price List Attributes
		builder.setIsTaxIncluded(
				priceList.isTaxIncluded()
			)
			.setValidFrom(
				StringManager.getValidString(
					TimeManager.getTimestampToString(
						productPricing.getPriceDate()
					)
				)
			)
			.setPriceListName(
				StringManager.getValidString(
					priceList.getName()
				)
			)
		;
		//	Pricing
		final int pricePrecision = priceList.getPricePrecision();
		builder.setPricePrecision(pricePrecision);
		//	Prices
		if(Optional.ofNullable(productPricing.getPriceStd()).orElse(Env.ZERO).signum() > 0) {
			BigDecimal priceListAmount = Optional.ofNullable(
					productPricing.getPriceList()
				)
				.orElse(Env.ZERO)
				.setScale(pricePrecision, RoundingMode.HALF_UP)
			;
			BigDecimal priceStandardAmount = Optional.ofNullable(
					productPricing.getPriceStd()
				).orElse(Env.ZERO)
				.setScale(pricePrecision, RoundingMode.HALF_UP)
			;
			BigDecimal priceLimitAmount = Optional.ofNullable(
					productPricing.getPriceList()
				)
				.orElse(Env.ZERO)
				.setScale(pricePrecision, RoundingMode.HALF_UP)
			;
			builder.setPriceList(
					NumberManager.getBigDecimalToString(
						priceListAmount
					)
				)
				.setPriceStandard(
					NumberManager.getBigDecimalToString(
						priceStandardAmount
					)
				)
				.setPriceLimit(
					NumberManager.getBigDecimalToString(
						priceLimitAmount
					)
				)
			;
			//	Get from schema
			if(displayCurrencyId > 0) {
				MCurrency displayCurrency = MCurrency.get(Env.getCtx(), displayCurrencyId);
				builder.setDisplayCurrency(
					CoreFunctionalityConvert.convertCurrency(displayCurrency)
				);
				//	Get
				int conversionRateId = MConversionRate.getConversionRateId(priceList.getC_Currency_ID(), displayCurrencyId, RecordUtil.getDate(), conversionTypeId, Env.getAD_Client_ID(Env.getCtx()), Env.getAD_Org_ID(Env.getCtx()));
				if(conversionRateId > 0) {
					//	TODO: cache or re-query should be resolved
					MConversionRate conversionRate = MConversionRate.get(Env.getCtx(), conversionRateId);
					if(conversionRate != null) {
						BigDecimal multiplyRate = conversionRate.getMultiplyRate();
						// int displayCurrencyPrecision = displayCurrency.getStdPrecision();
						BigDecimal displayPriceListAmount = Optional.ofNullable(
								productPricing.getPriceList()
							)
							.orElse(Env.ZERO)
							.multiply(multiplyRate, MathContext.DECIMAL128)
							.setScale(pricePrecision, RoundingMode.HALF_UP)
						;
						BigDecimal displayPriceStandardAmount = Optional.ofNullable(
								productPricing.getPriceStd()
							)
							.orElse(Env.ZERO)
							.multiply(multiplyRate, MathContext.DECIMAL128)
							.setScale(pricePrecision, RoundingMode.HALF_UP)
						;
						BigDecimal displayPriceLimitAmount = Optional.ofNullable(
								productPricing.getPriceLimit()
							)
							.orElse(Env.ZERO)
							.multiply(multiplyRate, MathContext.DECIMAL128)
							.setScale(pricePrecision, RoundingMode.HALF_UP)
						;
						builder.setDisplayPriceList(
								NumberManager.getBigDecimalToString(
									displayPriceListAmount
								)
							)
							.setDisplayPriceStandard(
								NumberManager.getBigDecimalToString(
									displayPriceStandardAmount
								)
							)
							.setDisplayPriceLimit(
								NumberManager.getBigDecimalToString(
									displayPriceLimitAmount
								))
							.setConversionRate(
								CoreFunctionalityConvert.convertConversionRate(
									conversionRate
								)
							)
						;
					}
				}
			}
		}
		//	Get Storage
		if(warehouseId > 0) {
			AtomicReference<BigDecimal> quantityOnHand = new AtomicReference<BigDecimal>(Env.ZERO);
			AtomicReference<BigDecimal> quantityReserved = new AtomicReference<BigDecimal>(Env.ZERO);
			AtomicReference<BigDecimal> quantityOrdered = new AtomicReference<BigDecimal>(Env.ZERO);
			AtomicReference<BigDecimal> quantityAvailable = new AtomicReference<BigDecimal>(Env.ZERO);
			//	
			Arrays.asList(MStorage.getOfProduct(Env.getCtx(), product.getM_Product_ID(), null))
				.stream()
				.filter(storage -> storage.getM_Warehouse_ID() == warehouseId)
				.forEach(storage -> {
					quantityOnHand.updateAndGet(quantity -> quantity.add(storage.getQtyOnHand()));
					quantityReserved.updateAndGet(quantity -> quantity.add(storage.getQtyReserved()));
					quantityOrdered.updateAndGet(quantity -> quantity.add(storage.getQtyOrdered()));
					quantityAvailable.updateAndGet(quantity -> quantity.add(storage.getQtyOnHand().subtract(storage.getQtyReserved())));
				});
			builder.setQuantityOnHand(NumberManager.getBigDecimalToString(quantityOnHand.get()))
				.setQuantityReserved(NumberManager.getBigDecimalToString(quantityReserved.get()))
				.setQuantityOrdered(NumberManager.getBigDecimalToString(quantityOrdered.get()))
				.setQuantityAvailable(NumberManager.getBigDecimalToString(quantityAvailable.get()))
			;
		}
		return builder;
	}

}
