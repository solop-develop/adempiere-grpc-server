package com.solop.sp034.support.documents;

import com.solop.sp034.util.Changes;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_ProductPrice;
import org.adempiere.core.domains.models.I_W_Store;
import org.compiere.model.MClient;
import org.compiere.model.MCurrency;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MStore;
import org.compiere.model.MTable;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Publishing implements IMercadoLibreDocument {

    private final MProduct product;
    private final PO publishing;
    private final MStore store;
    private final String currencyIsoCode;
    private BigDecimal availableQuantity;
    private String publishType;
    private String publishStatus;
    private BigDecimal price;
    private final String categoryCode;

    public Publishing(PO publishing) {
        this.publishing = publishing;
        product = MProduct.get(publishing.getCtx(), publishing.get_ValueAsInt(I_M_Product.COLUMNNAME_M_Product_ID));
        store = MStore.get(publishing.getCtx(), publishing.get_ValueAsInt(I_W_Store.COLUMNNAME_W_Store_ID));
        MPriceList priceList = MPriceList.get(Env.getCtx(), store.getM_PriceList_ID(), null);
        currencyIsoCode = MCurrency.getISO_Code(Env.getCtx(), priceList.getC_Currency_ID());
        categoryCode = getAllocatedCategoryCode(store.getW_Store_ID(), product.getM_Product_ID());
        publishType = publishing.get_ValueAsString(Changes.SP034_ML_PublishType);
        if(Util.isEmpty(publishType, true)) {
            publishType = "bronze";
        }
        resetMessage();
    }

    private void resetMessage() {
        publishing.set_ValueOfColumn(Changes.SP034_ValidationMsg, null);
        publishing.setIsDirectLoad(true);
        publishing.saveEx();
    }
    private void addMessage(String message) {
        String oldMessage = publishing.get_ValueAsString(Changes.SP034_ValidationMsg);
        StringBuilder allMessages = new StringBuilder();
        if(!Util.isEmpty(oldMessage, true)) {
            allMessages.append(oldMessage).append(Env.NL);
        }
        final String parseMessage = Msg.getMsg(publishing.getCtx(), message);
        allMessages.append("- ").append(parseMessage);
        publishing.set_ValueOfColumn(Changes.SP034_ValidationMsg, allMessages.toString());
    }

    @Override
    public boolean isValid(int event) {
        publishStatus = publishing.get_ValueAsString(Changes.SP034_PublishStatus);
        if(event == ModelValidator.TYPE_AFTER_NEW || event == ModelValidator.TYPE_AFTER_CHANGE) {
            if(publishStatus == null || publishStatus.isEmpty() || !publishChanges()) {
                addMessage("SP034.InvalidAction");
                return false;
            }
        }
        availableQuantity = getAvailableQuantity(store, publishType);
        if(availableQuantity.compareTo(Env.ZERO) == 0 && !isUnPublish()) {
            addMessage("SP034.NoAvailableQuantity");
        }
        price = (BigDecimal) publishing.get_Value(I_M_ProductPrice.COLUMNNAME_PriceStd);
        if(Optional.of(price).orElse(Env.ZERO).compareTo(Env.ZERO) <= 0) {
            addMessage("SP034.NoPrice");
        }
        if(Util.isEmpty(categoryCode, true)) {
            addMessage("SP034.NoCategorySelected");
        }
        //  Validate Item condition
        // if(!existsAttribute("ITEM_CONDITION")) {
        // TODO: Add `SP034_ML_ItemCondition` column into `SP034_Publishing` table to redundancy
        // final String itemCondition = publishing.get_ValueAsString(Changes.SP034_ML_ItemCondition);
        final String itemCondition = product.get_ValueAsString(Changes.SP034_ML_ItemCondition);
        if(Util.isEmpty(itemCondition, true)) {
            addMessage("SP034.NoItemConditionSelected");
        }
        return isValid();
    }

    private boolean isValid() {
        boolean isValid = Util.isEmpty(publishing.get_ValueAsString(Changes.SP034_ValidationMsg), true);
        if(!isValid) {
            publishing.set_ValueOfColumn(Changes.SP034_PublishStatus, Changes.SP034_PublishStatus_Without_Publishing);
            publishing.setIsDirectLoad(true);
            publishing.saveEx();
        }
        return isValid;
    }

    private String getAllocatedCategoryCode(int storeId, int productId) {
        PO category = new Query(publishing.getCtx(), Changes.Table_SP034_Category,
                "W_Store_ID = ? " +
                        "AND EXISTS(SELECT 1 FROM SP034_ProductCategory pc WHERE pc.SP034_Category_ID = SP034_Category.SP034_Category_ID AND pc.M_Product_ID = ? AND pc.IsActive = 'Y')", null)
                .setParameters(storeId, productId)
                .setOnlyActiveRecords(true)
                .first();
        if(category == null) {
            return null;
        }
        return category.get_ValueAsString("Value");
    }

    public boolean existsAttribute(String code) {
        return new Query(
            publishing.getCtx(),
            Changes.Table_SP034_Allocation,
            "SP034_Publishing_ID = ? " +
                "AND EXISTS(SELECT 1 FROM SP034_Attribute a WHERE a.SP034_Attribute_ID = SP034_Allocation.SP034_Attribute_ID " +
                "AND a.Value = ?)",
            null
        )
            .setParameters(publishing.get_ID(), code)
            .setClient_ID()
            .setOnlyActiveRecords(true)
            .match()
        ;
    }

    private boolean isUnPublish() {
        return publishStatus.equals(Changes.SP034_PublishStatus_Paused) || publishStatus.equals(Changes.SP034_PublishStatus_Closed);
    }

    private boolean publishChanges() {
        return publishStatus.equals(Changes.SP034_PublishStatus_Paused) || publishStatus.equals(Changes.SP034_PublishStatus_Closed) || publishStatus.equals(Changes.SP034_PublishStatus_Publishing);
    }

    @Override
    public Map<String, Object> getData() {
        Map<String, Object> publishValues = new HashMap<>();
        publishValues.put("client", getClientUuid());
        publishValues.put("code", product.getValue());
        publishValues.put("title", product.getName());
        publishValues.put("listing_type_id", publishType);
        publishValues.put("buying_mode", "buy_it_now");
        publishValues.put("description", product.getDescription());
        publishValues.put("help", product.getHelp());
        publishValues.put("currency_code", currencyIsoCode);
        publishValues.put("category_id", categoryCode);
        publishValues.put("barcode", product.getUPC());
        publishValues.put("price", price);
        publishValues.put("available_quantity", availableQuantity);
        publishValues.put("publish_status", publishStatus);
        publishValues.put("attributes", getAttributes());
        return publishValues;
    }

    private List<Map<String, Object>> getAttributes() {
        List<Map<String, Object>> attributesList = new ArrayList<>();
        MTable allocationTable = MTable.get(publishing.getCtx(), Changes.Table_SP034_Allocation);
        MTable attributeTable = MTable.get(publishing.getCtx(), Changes.Table_SP034_Attribute);
        MTable attributeListTable = MTable.get(publishing.getCtx(), Changes.Table_SP034_AttributeList);
        new Query(publishing.getCtx(), Changes.Table_SP034_Allocation, "SP034_Publishing_ID = ?", null)
                .setParameters(publishing.get_ID())
                .setClient_ID()
                .setOnlyActiveRecords(true)
                .getIDsAsList().forEach(allocationId -> {
                    PO attributeAllocation = allocationTable.getPO(allocationId, null);
                    int attributeId = attributeAllocation.get_ValueAsInt("SP034_Attribute_ID");
                    PO attributeDefinition = attributeTable.getPO(attributeId, null);
                    String type = attributeDefinition.get_ValueAsString(Changes.SP034_ValueType);
                    Object value = null;
                    String name = null;
                    if(type == null) {
                        value = attributeAllocation.get_ValueAsString("SP034_String");
                    } else if(type.equals(Changes.SP034_ValueType_Number)) {
                        value = attributeAllocation.get_Value("SP034_Number");
                        if(Optional.ofNullable((BigDecimal) value).orElse(Env.ZERO).compareTo(Env.ZERO) == 0) {
                            value = null;
                        }
                    } else if(type.equals(Changes.SP034_ValueType_Boolean)) {
                        value = attributeAllocation.get_ValueAsBoolean("SP034_Boolean");
                    } else if(type.equals(Changes.SP034_ValueType_Text_Long)) {
                        value = attributeAllocation.get_ValueAsString("SP034_LongString");
                    } else if(type.equals(Changes.SP034_ValueType_List)) {
                        int attributeListId = attributeAllocation.get_ValueAsInt("SP034_AttributeList_ID");
                        if (attributeListId > 0) {
                            PO attributeList = attributeListTable.getPO(attributeListId, null);
                            value = attributeList.get_ValueAsString("Value");
                            name = attributeList.get_ValueAsString("Name");
                        }
                    } else if(type.equals(Changes.SP034_ValueType_String)) {
                        value = attributeAllocation.get_ValueAsString("SP034_String");
                    }
                    if(value != null) {
                        Map<String, Object> attributes = new HashMap<>();
                        attributes.put("id", attributeDefinition.get_ValueAsString("Value"));
                        attributes.put("value", value);
                        if(!Util.isEmpty(name, true)) {
                            attributes.put("name", name);
                        }
                        attributesList.add(attributes);
                    }
                });
        return attributesList;
    }



    private String getClientUuid() {
        return MClient.get(product.getCtx(), product.getAD_Client_ID()).getUUID();
    }

    private BigDecimal getAvailableQuantity(MStore store, String publishType) {
        if(Util.isEmpty(publishType, true) || publishType.equals("free")) {
            return Env.ONE;
        }
        return getStock(store);
    }

    private BigDecimal getStock(MStore store) {
        Optional<MStorage> maybeStorage = Arrays.stream(MStorage.getOfProduct(Env.getCtx(), product.getM_Product_ID(), product.get_TrxName()))
                .filter(storage -> storage.getM_Warehouse_ID() == store.getM_Warehouse_ID())
                .reduce(StockSummary::add);
        if(maybeStorage.isPresent()) {
            BigDecimal quantityOnHand = Optional.ofNullable(maybeStorage.get().getQtyOnHand()).orElse(Env.ZERO);
            BigDecimal quantityReserved = Optional.ofNullable(maybeStorage.get().getQtyReserved()).orElse(Env.ZERO);
            //	On hand
            if(quantityReserved.signum() < 0) {
                return Env.ZERO;
            }
            //	Get Double
            return quantityOnHand.subtract(quantityReserved);
        }
        //
        return Env.ZERO;
    }

    private static final class StockSummary {
        public static MStorage add(MStorage previousValue, MStorage newValue) {
            previousValue.setQtyOnHand(previousValue.getQtyOnHand().add(newValue.getQtyOnHand()));
            previousValue.setQtyReserved(previousValue.getQtyReserved().add(newValue.getQtyReserved()));
            return previousValue;
        }
    }

}
