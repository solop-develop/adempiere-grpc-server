package com.solop.sp034.support;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MStore;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.model.MADAppRegistration;



/**
 * Web Store Asset handler that triggers a bulk product asset (photos) import from
 * Google Drive through an n8n webhook.
 *
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class WebStoreAssetGoogleDrive implements IWebStoreAsset {

    private static final CLogger log = CLogger.getCLogger(WebStoreAssetGoogleDrive.class);

    /**	Registration Id	*/
    protected int registrationId = 0;

    /**	Token	*/
    public static final String TOKEN_PARAMETER = "token";
    /**	Provider	*/
    public static final String PROVIDER_PARAMETER = "provider";
    /**	Root Folder	*/
    public static final String ROOT_FOLDER_PARAMETER = "root_folder";

    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Gson gson = new Gson();

    /**	Registration values	*/
    private String webhookUrl;
    private String token;
    private String provider;
    private String rootFolder;



    @Override
    public String testConnection() {
        return "";
    }

    @Override
    public void setAppRegistrationId(int registrationId) {
        this.registrationId = registrationId;
        MADAppRegistration registration = MADAppRegistration.getById(Env.getCtx(), getAppRegistrationId(), null);
        if(registration == null || registration.getAD_AppRegistration_ID() <= 0) {
            throw new AdempiereException("@AD_AppRegistration_ID@ @NotFound@");
        }
        webhookUrl = registration.getHost();
        if(Util.isEmpty(webhookUrl, true)) {
            throw new AdempiereException("@SP034_WSA_HostNotConfigured@");
        }
        token = registration.getParameterValue(TOKEN_PARAMETER);
        if(Util.isEmpty(token, true)) {
            throw new AdempiereException(Msg.getMsg(Env.getCtx(), "SP034_WSA_ParameterNotConfigured", new Object[]{TOKEN_PARAMETER}));
        }
        provider = registration.getParameterValue(PROVIDER_PARAMETER);
        if(Util.isEmpty(provider, true)) {
            throw new AdempiereException(Msg.getMsg(Env.getCtx(), "SP034_WSA_ParameterNotConfigured", new Object[]{PROVIDER_PARAMETER}));
        }
        rootFolder = registration.getParameterValue(ROOT_FOLDER_PARAMETER);
        if(Util.isEmpty(rootFolder, true)) {
            throw new AdempiereException(Msg.getMsg(Env.getCtx(), "SP034_WSA_ParameterNotConfigured", new Object[]{ROOT_FOLDER_PARAMETER}));
        }
    }

    @Override
    public int getAppRegistrationId() {
        return registrationId;
    }


    @Override
    public String call(MStore store, boolean isForce) {
        if(store == null) {
            throw new AdempiereException("@W_Store_ID@ @NotFound@");
        }
        JsonObject requestBodyJson = new JsonObject();
        requestBodyJson.addProperty("store_uuid", store.getUUID());
        requestBodyJson.addProperty("provider", provider);
        requestBodyJson.addProperty("root_folder", rootFolder);
        requestBodyJson.addProperty("force", isForce);
        String json = gson.toJson(requestBodyJson);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(webhookUrl)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new AdempiereException(Msg.getMsg(Env.getCtx(), "SP034_WSA_ServiceError",
                        new Object[]{response.code(), Util.isEmpty(responseBody) ? "-" : responseBody}));
            }
            return buildResultMessage(responseBody);
        } catch (AdempiereException e) {
            throw e;
        } catch (Exception e) {
            log.severe("Error calling web store asset webhook: " + e.getLocalizedMessage());
            throw new AdempiereException(Msg.getMsg(Env.getCtx(), "SP034_WSA_ServiceUnreachable",
                    new Object[]{e.getLocalizedMessage()}));
        }
    }

    /**
     * Build the user-facing message from the webhook response, reading the "skiped" flag.
     * @param responseBody raw JSON body (object or array)
     * @return message to show to the user
     */
    private String buildResultMessage(String responseBody) {
        boolean skiped = false;
        if (!Util.isEmpty(responseBody, true)) {
            try {
                JsonObject responseJson;
                if (responseBody.trim().startsWith("[")) {
                    JsonArray arr = gson.fromJson(responseBody, JsonArray.class);
                    responseJson = arr.size() > 0 ? arr.get(0).getAsJsonObject() : new JsonObject();
                } else {
                    responseJson = gson.fromJson(responseBody, JsonObject.class);
                }
                if (responseJson.has("skiped") && !responseJson.get("skiped").isJsonNull()) {
                    skiped = responseJson.get("skiped").getAsBoolean();
                }
            } catch (Exception e) {
                log.warning("Could not parse web store asset response: " + e.getMessage());
            }
        }
        if (skiped) {
            return Msg.getMsg(Env.getCtx(), "SP034_WSA_ImportInProgress");
        }
        return Msg.getMsg(Env.getCtx(), "SP034_WSA_ImportStarted");
    }
}
