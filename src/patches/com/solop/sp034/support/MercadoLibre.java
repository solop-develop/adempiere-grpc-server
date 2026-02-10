package com.solop.sp034.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.solop.sp033.interfaces.IWebhook;
import com.solop.sp034.util.DocumentBuilder;
import okhttp3.*;

import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.exceptions.AdempiereException;
// import org.compiere.model.MAttachment;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MOrg;
import org.compiere.model.MProduct;
import org.compiere.model.MRefTable;
import org.compiere.model.MStore;
import org.compiere.model.MTable;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.spin.eca62.support.ResourceMetadata;
import org.spin.util.AttachmentUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
// import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MercadoLibre implements IWebhook {
    private static final CLogger log = CLogger.getCLogger(PO.class);
    private final OkHttpClient client = new OkHttpClient();
    private MediaType JSON;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Gson gson = new Gson();
    private String webhookUrl;
    private String method;
    private Language language;
    private String event;
    private int eventId;
    private DocumentBuilder documentBuilder;

    @Override
    public void loadMetaData(Map<String, Object> metadata) {
        webhookUrl = (String) metadata.get(PayloadURL);
        JSON = MediaType.parse(metadata.get(ContentType) + "; charset=utf-8");
        method = (String) metadata.get(Method);
        language = Language.getLoginLanguage();
        event = (String) metadata.get(Event);
        eventId = (Integer) metadata.get(EventId);
    }

    private Map<String, Object> getAttributes(PO entity) {
        Map<String, Object> data = new HashMap<>();
        MTable.get(entity.getCtx(), entity.get_TableName()).getColumnsAsList().forEach(column -> {
            Object value = entity.get_Value(column.getColumnName());
            if(value != null) {
                if(isLookup(column)) {
                    String displayValue = getDisplayValue(column, (int) value);
                    if(displayValue != null) {
                        data.put(camelToSnake(column.getColumnName() + "_display_value"), displayValue);
                    }
                } else if(DisplayType.isDate(column.getAD_Reference_ID())) {
                    Timestamp date = (Timestamp) value;
                    String formatedValue = DisplayType.getDateFormat(column.getAD_Reference_ID(), language, column.getFormatPattern()).format(date);
                    data.put(camelToSnake(column.getColumnName() + "_display_value"), formatedValue);
                }
                data.put(camelToSnake(column.getColumnName()), value);
            }
        });
        return data;
    }

    private boolean isLookup(MColumn column) {
        return (DisplayType.isLookup(column.getAD_Reference_ID())
                || column.getAD_Reference_ID() == DisplayType.Assignment)
                && column.getAD_Reference_ID() != DisplayType.List
                ;
    }

    private String getDisplayValue(MColumn column, int id) {
        String tableName = null;
        if(column.getAD_Reference_ID() == DisplayType.TableDir) {
            tableName = column.getColumnName().replace("_ID", "");
        } else if(column.getAD_Reference_ID() == DisplayType.Table || column.getAD_Reference_ID() == DisplayType.Search) {
            if(column.getAD_Reference_Value_ID() <= 0) {
                tableName = column.getColumnName().replace("_ID", "");
            } else {
                MRefTable referenceTable = MRefTable.getById(Env.getCtx(), column.getAD_Reference_Value_ID());
                tableName = MTable.getTableName(Env.getCtx(), referenceTable.getAD_Table_ID());
            }
        }
        if(tableName == null) {
            return null;
        }
        MTable referenceTable = MTable.get(Env.getCtx(), tableName);
        PO referenceEntity = referenceTable.getPO(id, null);
        if(referenceEntity == null) {
            return null;
        }
        return referenceEntity.getDisplayValue();
    }

    private String camelToSnake(String camelCaseString) {
        if (camelCaseString == null || camelCaseString.isEmpty()) {
            return camelCaseString;
        }

        Pattern pattern = Pattern.compile("(?<=[a-z])(?=[A-Z])");

        return pattern.splitAsStream(camelCaseString)
                .map(String::toLowerCase)
                .collect(Collectors.joining("_"));
    }

    private List<String> getProductImageUrls(PO entity) {
        List<String> imageUrls = new ArrayList<>();

        try {
            // Get M_Product_ID from the entity (SP034_Publishing)
            int productId = entity.get_ValueAsInt("M_Product_ID");
            if (productId <= 0) {
                return imageUrls;
            }

            // Get the product
            MProduct product = MProduct.get(entity.getCtx(), productId);
            if (product == null) {
                return imageUrls;
            }

            // // Get attachment from the product
            // MAttachment attachment = product.getAttachment();
            // if (attachment == null || attachment.getAD_Attachment_ID() <= 0) {
            //     return imageUrls;
            // }

            // Get client UUID for building the path
            MClient client = MClient.get(entity.getCtx(), entity.getAD_Client_ID());
            String clientUuid = client.getUUID();

            // Use AttachmentUtil to get file names
            AttachmentUtil attachmentUtil = AttachmentUtil.getInstance()
                .withClientId(entity.getAD_Client_ID())
            ;

            List<String> fileNames = attachmentUtil.getFileNameListFromResourcePath(
                product.getAD_Client_ID(),
                I_M_Product.Table_Name,
                productId,
                ResourceMetadata.ContainerType.ATTACHMENT,
                null
            );

            // Filter only image files and build URLs
            String baseUrl = String.format("/%s/client/attachment/%s/%d/",
                clientUuid,
                I_M_Product.Table_Name,
                productId
            );

            for (String fileName : fileNames) {
                if (isImageFile(fileName)) {
                    imageUrls.add(baseUrl + fileName);
                }
            }
        } catch (Exception e) {
            log.warning("Error getting product images: " + e.getMessage());
        }

        return imageUrls;
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".jpg") ||
               lowerCaseFileName.endsWith(".jpeg") ||
               lowerCaseFileName.endsWith(".png") ||
               lowerCaseFileName.endsWith(".gif") ||
               lowerCaseFileName.endsWith(".ico") ||
               lowerCaseFileName.endsWith(".webp") ||
               lowerCaseFileName.endsWith(".bmp");
    }

    private void addMetadata(JsonObject requestBodyJson, PO entity) {
        requestBodyJson.addProperty("event", event);
        requestBodyJson.addProperty("event_id", eventId);
        requestBodyJson.addProperty("language", language.getLanguageCode());
        requestBodyJson.addProperty("table_name", entity.get_TableName());
        requestBodyJson.addProperty("id", entity.get_ID());

        MStore store = MStore.get(entity.getCtx(), entity.get_ValueAsInt("W_Store_ID"));
        if (store != null && store.getW_Store_ID() > 0) {
            requestBodyJson.addProperty("store_id", entity.get_ValueAsInt("W_Store_ID"));
            requestBodyJson.addProperty("store_name", store.getName());
            JsonObject storeJson = new JsonObject();
            storeJson.addProperty("id", store.getW_Store_ID());
            storeJson.addProperty("uuid", store.getUUID());
            storeJson.addProperty("name", store.getName());
            storeJson.addProperty("url", store.getURL());

            MWarehouse warehouse = new MWarehouse(entity.getCtx(), store.getM_Warehouse_ID(), null);
            if (warehouse != null) {
                storeJson.addProperty("warehouse_id", store.getM_Warehouse_ID());
                requestBodyJson.addProperty("warehouse_value", warehouse.getValue());
                requestBodyJson.addProperty("warehouse_name", warehouse.getName());
                JsonObject warehouseJson = new JsonObject();
                warehouseJson.addProperty("id", warehouse.getAD_Org_ID());
                warehouseJson.addProperty("uuid", warehouse.getUUID());
                warehouseJson.addProperty("value", warehouse.getValue());
                warehouseJson.addProperty("name", warehouse.getName());
                requestBodyJson.add("warehouse", warehouseJson);
            }

            requestBodyJson.add("store", storeJson);
        }

        MClient client = new MClient(entity.getCtx(), entity.get_ValueAsInt("AD_Client_ID"), null);
        if (client != null) {
            requestBodyJson.addProperty("client_id", client.getAD_Org_ID());
            requestBodyJson.addProperty("client_value", client.getValue());
            requestBodyJson.addProperty("client_name", client.getName());
            JsonObject clientJson = new JsonObject();
            clientJson.addProperty("id", client.getAD_Org_ID());
            clientJson.addProperty("uuid", client.getUUID());
            clientJson.addProperty("value", client.getValue());
            clientJson.addProperty("name", client.getName());
            requestBodyJson.add("client", clientJson);
        }

        MOrg organization = new MOrg(entity.getCtx(), entity.get_ValueAsInt("AD_Org_ID"), null);
        if (organization != null) {
            requestBodyJson.addProperty("org_id", organization.getAD_Org_ID());
            requestBodyJson.addProperty("org_value", organization.getValue());
            requestBodyJson.addProperty("org_name", organization.getName());
            JsonObject organizationJson = new JsonObject();
            organizationJson.addProperty("id", organization.getAD_Org_ID());
            organizationJson.addProperty("uuid", organization.getUUID());
            organizationJson.addProperty("value", organization.getValue());
            organizationJson.addProperty("name", organization.getName());
            requestBodyJson.add("organization", organizationJson);
        }
    }

    private Request prepareRequest(PO entity) {
        JsonObject requestBodyJson = new JsonObject();
        addMetadata(requestBodyJson, entity);
        //  Add Mercado Libre data here
        requestBodyJson.add("document", gson.toJsonTree(documentBuilder.getDocument().getData()));
        requestBodyJson.add("attributes", gson.toJsonTree(getAttributes(entity)));
        //  Add product images
        List<String> imageUrls = getProductImageUrls(entity);
        requestBodyJson.add("images", gson.toJsonTree(imageUrls));
        String json = gson.toJson(requestBodyJson);
        RequestBody body = RequestBody.create(json, JSON);
        HttpUrl.Builder httpUrl = Objects.requireNonNull(HttpUrl.parse(webhookUrl))
                .newBuilder();
        Request.Builder builder = new Request.Builder();
        switch (method) {
            case Method_POST:
                builder.post(body);
                break;
            case Method_PATCH:
                builder.patch(body);
                break;
            case Method_DELETE:
                builder.delete(body);
                break;
            case Method_GET:
                builder.get();
                break;
        }
        return builder.url(httpUrl.build()).build();
    }

    @Override
    public void callWebHook(PO entity) {
        documentBuilder = DocumentBuilder.newInstance().withEntity(entity, eventId);
        if(!documentBuilder.isValid()) {
            return;
        }
        Request request = prepareRequest(entity);
        try {
            executorService.submit(() -> {
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new AdempiereException("Error sending " + entity.get_ID() + ". Response Code: " + response.code() + ", Body: " + response.body().toString());
                    }
                    // Process webhook response
                    String responseBody = response.body().string();
                    if (responseBody != null && !responseBody.isEmpty()) {
                        try {
                            String publicationUrl = null;
                            String publicationId = null;

                            // Check if response is an array (MercadoLibre returns array)
                            if (responseBody.trim().startsWith("[")) {
                                JsonArray responseArray = gson.fromJson(responseBody, JsonArray.class);
                                if (responseArray.size() > 0) {
                                    JsonObject firstItem = responseArray.get(0).getAsJsonObject();
                                    // MercadoLibre uses 'permalink' field
                                    if (firstItem.has("permalink")) {
                                        publicationUrl = firstItem.get("permalink").getAsString();
                                    }
                                    // Also capture MercadoLibre ID
                                    if (firstItem.has("id")) {
                                        publicationId = firstItem.get("id").getAsString();
                                    }
                                }
                            } else {
                                // Handle single object response
                                JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
                                    // MercadoLibre uses 'permalink' field
                                if (responseJson.has("permalink")) {
                                    publicationUrl = responseJson.get("permalink").getAsString();
                                }
                                // Also capture ID if available
                                if (responseJson.has("id")) {
                                    publicationId = responseJson.get("id").getAsString();
                                }
                            }

                            // Update the SP034_Publishing record with the publication data
                            boolean updated = false;
                            if (publicationUrl != null && !publicationUrl.isEmpty()) {
                                entity.set_ValueOfColumn("SP034_PublicationURL", publicationUrl);
                                updated = true;
                                log.info("Publication URL saved: " + publicationUrl);
                            }
                            if (publicationId != null && !publicationId.isEmpty()) {
                                entity.set_ValueOfColumn("SP034_PublicationCode", publicationId);
                                updated = true;
                                log.info("Publication ID saved: " + publicationId);
                            }

                            if (updated) {
                                // entity.setIsDirectLoad(true);
                                entity.saveEx();
                            }
                        } catch (Exception e) {
                            log.warning("Error processing webhook response: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.severe(e.getLocalizedMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            log.severe(e.getLocalizedMessage());
            // Add this to queue to sent after?
        }
    }

    @Override
    public void testWebHook(PO entity) {
        // int id = entity.get_ID();
        JsonObject requestBodyJson = new JsonObject();
        addMetadata(requestBodyJson, entity);
        //  Add Mercado Libre data here
        requestBodyJson.add("attributes", gson.toJsonTree(getAttributes(entity)));
        String json = gson.toJson(requestBodyJson);
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder()
                .url(webhookUrl);
        if(method.equals(Method_POST)) {
            builder.post(body);
        } else if(method.equals(Method_PATCH)) {
            builder.patch(body);
        } else if(method.equals(Method_DELETE)) {
            builder.delete(body);
        } else if(method.equals(Method_GET)) {
            builder.get();
        }
        Request request = prepareRequest(entity);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new AdempiereException("Error sending " + entity.get_ID() + ". Response Code: " + response.code() + ", Body: " + response.body().toString());
            }
        } catch (Exception e) {
            throw new AdempiereException(e);
        }
    }

}
