package com.solop.sp034.util;

import org.adempiere.core.domains.models.I_C_Tax;
import org.adempiere.core.domains.models.I_M_PriceList_Version;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_ProductPrice;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPricing;
import org.compiere.model.MStorage;
import org.compiere.model.MStore;
import org.compiere.model.MTax;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

/**
 * Single source of truth for refreshing a SP034_Publishing record's price and inventory data.
 * Used both by the publishing creation process ({@code PublishProducts}) and by the publication
 * updater queue ({@code StorePublicationUpdater}) so the calculation only lives in one place.
 *
 * @author Gabriel Escalona
 */
public class PublishingUpdater {

    private PublishingUpdater() {
    }

    /**
     * Recompute and persist the price and inventory columns of the given publishing record and
     * {@code saveEx()} it.
     * <ul>
     *     <li>Prices: pre-tax on the standard columns, tax included on the publish columns.</li>
     *     <li>Inventory: {@code SP034_CurrentInventory} on every call, {@code DropShip_Warehouse_ID}
     *     with the warehouse used, and {@code SP034_InitialInventory} only the first time (when it
     *     is still null).</li>
     * </ul>
     *
     * @param publishing the SP034_Publishing record (must already have M_Product_ID and W_Store_ID)
     * @return {@code true} when the data was updated and saved; {@code false} when it was skipped
     *         (e.g. the store's price list has no current version)
     */
    public static boolean updateData(PO publishing) {
        Properties ctx = publishing.getCtx();
        String trxName = publishing.get_TrxName();
        int productId = publishing.get_ValueAsInt(I_M_Product.COLUMNNAME_M_Product_ID);
        int storeId = publishing.get_ValueAsInt("W_Store_ID");
        MStore store = MStore.get(ctx, storeId);
        MPriceList priceList = MPriceList.get(ctx, store.getM_PriceList_ID(), null);
        MPriceListVersion version = priceList.getPriceListVersion(TimeUtil.getDay(System.currentTimeMillis()));
        if (version == null) {
            return false;
        }
        int precision = priceList.getStandardPrecision();
        MProduct product = MProduct.get(ctx, productId);

        //	Prices
        MProductPricing productPricing = new MProductPricing(productId, 0, Env.ZERO, true, null);
        productPricing.setM_PriceList_Version_ID(version.getM_PriceList_Version_ID());
        productPricing.calculatePrice();
        int taxId = getTaxId(ctx, product.getC_TaxCategory_ID());
        MTax tax = taxId > 0 ? MTax.get(ctx, taxId) : null;
        boolean taxIncluded = productPricing.isTaxIncluded();
        publishing.set_ValueOfColumn(I_M_PriceList_Version.COLUMNNAME_M_PriceList_Version_ID, version.getM_PriceList_Version_ID());
        publishing.set_ValueOfColumn(I_M_ProductPrice.COLUMNNAME_PriceList, productPricing.getPriceList());
        publishing.set_ValueOfColumn(I_M_ProductPrice.COLUMNNAME_PriceStd, productPricing.getPriceStd());
        publishing.set_ValueOfColumn(I_M_ProductPrice.COLUMNNAME_PriceLimit, productPricing.getPriceLimit());
        BigDecimal publishPriceList = addTaxIfNeeded(productPricing.getPriceList(), tax, taxIncluded, precision);
        BigDecimal publishPriceStd = addTaxIfNeeded(productPricing.getPriceStd(), tax, taxIncluded, precision);
        publishing.set_ValueOfColumn(Changes.SP034_PublishPriceList, publishPriceList);
        publishing.set_ValueOfColumn(Changes.SP034_PublishPriceStd, publishPriceStd);
        BigDecimal listPrice = Optional.ofNullable(publishPriceList).orElse(Env.ZERO);
        BigDecimal stdPrice = Optional.ofNullable(publishPriceStd).orElse(Env.ZERO);
        BigDecimal discountAmt = listPrice.subtract(stdPrice);
        publishing.set_ValueOfColumn(Changes.SP034_PublishDiscountAmt, discountAmt);
        BigDecimal discountPercentage = listPrice.signum() > 0
                ? discountAmt.multiply(Env.ONEHUNDRED).divide(listPrice, 2, RoundingMode.HALF_UP)
                : Env.ZERO;
        publishing.set_ValueOfColumn(Changes.SP034_PublishDiscount, discountPercentage);
        if (taxId > 0) {
            publishing.set_ValueOfColumn(I_C_Tax.COLUMNNAME_C_Tax_ID, taxId);
        }

        //	Inventory: current is refreshed on every change, initial only the first time
        int warehouseId = store.getDropShip_Warehouse_ID() > 0
                ? store.getDropShip_Warehouse_ID()
                : store.getM_Warehouse_ID();
        BigDecimal stock = getStock(ctx, trxName, productId, warehouseId);
        publishing.set_ValueOfColumn(Changes.SP034_CurrentInventory, stock);
        publishing.set_ValueOfColumn(Changes.DropShip_Warehouse_ID, warehouseId);
        BigDecimal initialPublishInventory =  (BigDecimal) publishing.get_Value(Changes.SP034_InitialInventory);
        if (Util.isEmpty(publishing.get_ValueAsString(Changes.SP034_PublicationCode), true)
            || initialPublishInventory == null || initialPublishInventory.signum() == 0) {
            publishing.set_ValueOfColumn(Changes.SP034_InitialInventory, stock);
        }
        publishing.saveEx();
        return true;
    }

    /**
     * Stock available for the product on the given warehouse: sum of (QtyOnHand - QtyReserved).
     */
    private static BigDecimal getStock(Properties ctx, String trxName, int productId, int warehouseId) {
        Optional<MStorage> maybeStorage = Arrays.stream(MStorage.getOfProduct(ctx, productId, trxName))
                .filter(storage -> storage.getM_Warehouse_ID() == warehouseId)
                .reduce((previousValue, newValue) -> {
                    previousValue.setQtyOnHand(previousValue.getQtyOnHand().add(newValue.getQtyOnHand()));
                    previousValue.setQtyReserved(previousValue.getQtyReserved().add(newValue.getQtyReserved()));
                    return previousValue;
                });
        if (maybeStorage.isPresent()) {
            BigDecimal quantityOnHand = Optional.ofNullable(maybeStorage.get().getQtyOnHand()).orElse(Env.ZERO);
            BigDecimal quantityReserved = Optional.ofNullable(maybeStorage.get().getQtyReserved()).orElse(Env.ZERO);
            if (quantityReserved.signum() < 0) {
                return Env.ZERO;
            }
            return quantityOnHand.subtract(quantityReserved);
        }
        return Env.ZERO;
    }

    /**
     * Sales tax for the product's tax category.
     */
    private static int getTaxId(Properties ctx, int taxCategoryId) {
        if (taxCategoryId <= 0) {
            return -1;
        }
        return new Query(
                ctx,
                I_C_Tax.Table_Name,
                "C_TaxCategory_ID = ? AND (IsSalesTax = 'Y' OR SOPOType IN('S', 'B')) ",
                null)
                .setParameters(taxCategoryId)
                .setClient_ID()
                .setOnlyActiveRecords(true)
                .firstId();
    }

    /**
     * Add tax to a price when the price list is not tax included.
     */
    private static BigDecimal addTaxIfNeeded(BigDecimal price, MTax tax, boolean taxIncluded, int precision) {
        if (price == null || taxIncluded || tax == null || price.signum() == 0) {
            return price;
        }
        return price.add(tax.calculateTax(price, false, precision));
    }
}
