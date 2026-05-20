package com.solop.sp034.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.solop.sp033.interfaces.IWebhook;
import com.solop.sp034.util.DocumentBuilder;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MProduct;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.spin.eca62.support.ResourceMetadata;
import org.spin.util.AttachmentUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
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

    private Map<String, Object> getProductFiles(PO entity) {
        List<String> mediaUrls = new ArrayList<>();
        List<String> assetUrls = new ArrayList<>();
        Map<String, Object> files = new HashMap<>();
        files.put("image_url", null);
        files.put("description_url", null);
        files.put("download_url", null);
        files.put("media", mediaUrls);
        files.put("assets", assetUrls);
        try {
            int productId = entity.get_ValueAsInt("M_Product_ID");
            if (productId <= 0) {
                return files;
            }
            MProduct product = MProduct.get(entity.getCtx(), productId);
            if (product == null) {
                return files;
            }
            files.put("image_url", product.getImageURL());
            files.put("description_url", product.getDescriptionURL());
            files.put("download_url", product.getDownloadURL());
            MClient client = MClient.get(entity.getCtx(), entity.getAD_Client_ID());
            String clientUuid = client.getUUID();
            AttachmentUtil attachmentUtil = AttachmentUtil.getInstance()
                .withClientId(entity.getAD_Client_ID());
            List<String> fileNames = attachmentUtil.getFileNameListFromResourcePath(
                product.getAD_Client_ID(),
                I_M_Product.Table_Name,
                productId,
                ResourceMetadata.ContainerType.ATTACHMENT,
                null
            );
            String baseUrl = String.format("/%s/client/attachment/%s/%d/",
                clientUuid,
                I_M_Product.Table_Name.toLowerCase(),
                productId
            );
            for (String fileName : fileNames) {
                String fileUrl = baseUrl + fileName;
                if (isMediaFile(fileName)) {
                    mediaUrls.add(fileUrl);
                } else {
                    assetUrls.add(fileUrl);
                }
            }
            files.put("media", mediaUrls);
            files.put("assets", assetUrls);
        } catch (Exception e) {
            log.warning("Error getting product files: " + e.getMessage());
        }
        return files;
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
            || lower.endsWith(".gif") || lower.endsWith(".ico") || lower.endsWith(".webp")
            || lower.endsWith(".bmp") || lower.endsWith(".svg");
    }

    private boolean isVideoFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".ogg")
            || lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".wmv")
            || lower.endsWith(".flv") || lower.endsWith(".mkv") || lower.endsWith(".m4v");
    }

    private boolean isMediaFile(String fileName) {
        return isImageFile(fileName) || isVideoFile(fileName);
    }

    private void addMetadata(JsonObject requestBodyJson, PO entity) {
        requestBodyJson.addProperty("event", event);
        requestBodyJson.addProperty("event_id", eventId);
        requestBodyJson.addProperty("language", language.getLanguageCode());
        requestBodyJson.addProperty("table_name", entity.get_TableName());
        requestBodyJson.addProperty("id", entity.get_ID());
        requestBodyJson.addProperty("store_id", entity.get_ValueAsInt("W_Store_ID"));
        requestBodyJson.addProperty("org_id", entity.get_ValueAsInt("AD_Org_ID"));
    }

    private Request prepareRequest(PO entity) {
        JsonObject requestBodyJson = new JsonObject();
        addMetadata(requestBodyJson, entity);
        //  Add Mercado Libre data here
        requestBodyJson.add("document", gson.toJsonTree(documentBuilder.getDocument().getData()));
        requestBodyJson.add("attributes", gson.toJsonTree(getAttributes(entity)));
        int productId = entity.get_ValueAsInt("M_Product_ID");
        if (productId > 0) {
            MProduct product = MProduct.get(entity.getCtx(), productId);
            if (product != null) {
                Object itemCondition = product.get_Value("SP034_ML_ItemCondition");
                if (itemCondition != null && !itemCondition.toString().isEmpty()) {
                    requestBodyJson.addProperty("item_condition", itemCondition.toString());
                }
                Object publishType = product.get_Value("SP034_ML_PublishType");
                if (publishType != null && !publishType.toString().isEmpty()) {
                    requestBodyJson.addProperty("publish_type", publishType.toString());
                }
            }
        }
        Map<String, Object> productFiles = getProductFiles(entity);
        requestBodyJson.add("image_url", gson.toJsonTree(productFiles.get("image_url")));
        requestBodyJson.add("description_url", gson.toJsonTree(productFiles.get("description_url")));
        requestBodyJson.add("download_url", gson.toJsonTree(productFiles.get("download_url")));
        requestBodyJson.add("media", gson.toJsonTree(productFiles.get("media")));
        requestBodyJson.add("assets", gson.toJsonTree(productFiles.get("assets")));
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
                    String responseBody = response.body().string();
                    if (!response.isSuccessful()) {
                        handleWebhookError(entity, response.code(), responseBody);
                        return;
                    }
                    if (responseBody != null && !responseBody.isEmpty()) {
                        handleWebhookSuccess(entity, responseBody);
                    }
                } catch (Exception e) {
                    log.severe("Error calling webhook: " + e.getLocalizedMessage());
                    saveValidationError(entity, "Webhook Error", e.getLocalizedMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            log.severe(e.getLocalizedMessage());
            saveValidationError(entity, "Execution Rejected", e.getLocalizedMessage());
        }
    }

    private void handleWebhookSuccess(PO entity, String responseBody) {
        try {
            JsonObject responseJson;
            if (responseBody.trim().startsWith("[")) {
                JsonArray arr = gson.fromJson(responseBody, JsonArray.class);
                if (arr.size() == 0) return;
                responseJson = arr.get(0).getAsJsonObject();
            } else {
                responseJson = gson.fromJson(responseBody, JsonObject.class);
            }
            if (responseJson.has("success") && responseJson.get("success").getAsBoolean()) {
                if (responseJson.has("publication_url")) {
                    entity.set_ValueOfColumn("SP034_PublicationURL", responseJson.get("publication_url").getAsString());
                }
                if (responseJson.has("publication_id")) {
                    entity.set_ValueOfColumn("SP034_PublicationCode", responseJson.get("publication_id").getAsString());
                }
                entity.set_ValueOfColumn("SP034_ValidationMsg", null);
                String publishStatus = "A";
                if (responseJson.has("new_status")) {
                    switch (responseJson.get("new_status").getAsString().toLowerCase()) {
                        case "active":   publishStatus = "A"; break;
                        case "paused":   publishStatus = "O"; break;
                        case "closed":   publishStatus = "C"; break;
                        default:         publishStatus = "D"; break;
                    }
                }
                entity.set_ValueOfColumn("SP034_PublishStatus", publishStatus);
                entity.saveEx(null);
            } else {
                int mlStatus = 400;
                if (responseJson.has("error_details")) {
                    JsonObject details = responseJson.getAsJsonObject("error_details");
                    if (details.has("http_status") && !details.get("http_status").isJsonNull()) {
                        mlStatus = details.get("http_status").getAsInt();
                    }
                }
                handleWebhookError(entity, mlStatus, responseBody);
            }
        } catch (Exception e) {
            log.warning("Error processing webhook success response: " + e.getMessage());
            saveValidationError(entity, "Response Processing Error", e.getMessage());
        }
    }

    private void handleWebhookError(PO entity, int statusCode, String responseBody) {
        try {
            String errorMessage = "Unknown error";
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    JsonObject err = gson.fromJson(responseBody, JsonObject.class);
                    if (err.has("message")) errorMessage = err.get("message").getAsString();
                    else if (err.has("error")) errorMessage = err.get("error").getAsString();
                    if (err.has("error_details")) {
                        JsonObject details = err.getAsJsonObject("error_details");
                        if (details.has("message")) errorMessage = details.get("message").getAsString();
                    }
                } catch (Exception ignored) {
                    errorMessage = responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
                }
            }
            saveValidationError(entity, "MercadoLibre Error (HTTP " + statusCode + ")", errorMessage);
            entity.set_ValueOfColumn("SP034_PublishStatus", "D");
            entity.saveEx(null);
            log.severe("Webhook error for record " + entity.get_ID() + ": " + errorMessage);
        } catch (Exception e) {
            log.severe("Error handling webhook error: " + e.getMessage());
        }
    }

    private void saveValidationError(PO entity, String errorType, String errorMessage) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            String msg = String.format("[%s] %s: %s", timestamp, errorType, errorMessage);
            if (msg.length() > 2000) msg = msg.substring(0, 1997) + "...";
            entity.set_ValueOfColumn("SP034_ValidationMsg", msg);
            entity.saveEx(null);
        } catch (Exception e) {
            log.severe("Failed to save validation error: " + e.getMessage());
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
