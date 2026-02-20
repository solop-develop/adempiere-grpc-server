package com.solop.sp033.util;

import com.solop.sp033.interfaces.IWebhook;
import com.solop.sp033.model.MSP033Webhook;
import org.adempiere.core.domains.models.I_AD_Table;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebhookLoader {
    public static WebhookLoader newInstance() {
        return new WebhookLoader();
    }

    private PO entity;
    private int persistenceEvent;
    private int documentEvent;

    public PO getEntity() {
        return entity;
    }

    public WebhookLoader withEntity(PO entity) {
        this.entity = entity;
        return this;
    }

    public int getPersistenceEvent() {
        return persistenceEvent;
    }

    public WebhookLoader withPersistenceEvent(int persistenceEvent) {
        this.persistenceEvent = persistenceEvent;
        return this;
    }

    public int getDocumentEvent() {
        return documentEvent;
    }

    public WebhookLoader withDocumentEvent(int documentEvent) {
        this.documentEvent = documentEvent;
        return this;
    }

    public void run() {
        if(entity == null) {
            throw new AdempiereException("Entity is null");
        }
        if(persistenceEvent <= 0 && documentEvent <= 0) {
            throw new AdempiereException("No Event / Document");
        }
        if(!isValidEvent()) {
            return;
        }
        List<Integer> listeners = getWebhookListeners();
        if(listeners != null) {
            listeners.forEach(this::runListener);
        }
    }

    private void runListener(int listenerId) {
        MSP033Webhook listener = getListener(listenerId);
        if(listener.get_ID() > 0) {
            String className = listener.getClassname();
            if(className != null) {
                IWebhook webhook = WebhookClassLoader.loadClass(className);
                if(webhook != null) {
                    webhook.loadMetaData(getMetadata(listener));
                    webhook.callWebHook(entity);
                }
            }
        }
    }

    public void test(int listenerId) {
        MSP033Webhook listener = getListener(listenerId);
        if(listener.get_ID() > 0) {
            String className = listener.getClassname();
            if(className != null) {
                IWebhook webhook = WebhookClassLoader.loadClass(className);
                if(webhook != null) {
                    webhook.loadMetaData(getMetadata(listener));
                    webhook.testWebHook(entity);
                }
            }
        }
    }

    private Map<String, Object> getMetadata(MSP033Webhook listener) {
        Map<String, Object> metadata = new HashMap<>();
        String url= listener.getSP033_PayloadURL();
        if (listener.isSP033_IsTest()) {
            url = listener.getSP033_TestPayloadURL();
            if (Util.isEmpty(url, true)) {
                throw new AdempiereException("@SP033_TestPayloadURL@ @NotFound@");
            }
        }
        metadata.put(IWebhook.PayloadURL, url);
        metadata.put(IWebhook.ContentType, getValidContentType(listener.getSP033_ContentType()));
        metadata.put(IWebhook.Method, listener.getSP033_Method());
        metadata.put(IWebhook.Name, listener.getName());
        metadata.put(IWebhook.Event, getEventName());
        metadata.put(IWebhook.EventId, persistenceEvent);
        metadata.put(IWebhook.ListenerId, listener.get_ID());
        return metadata;
    }

    private String getValidContentType(String contentType) {
        if(contentType == null) {
            return IWebhook.ContentType_AJ;
        }
        if(contentType.equals(Changes.SP033_ContentType_AX)) {
            return IWebhook.ContentType_AX;
        } else if(contentType.equals(Changes.SP033_ContentType_AJ)) {
            return IWebhook.ContentType_AJ;
        }
        return IWebhook.ContentType_AJ;
    }

    private MSP033Webhook getListener(int listenerId) {
        return new MSP033Webhook(Env.getCtx(), listenerId, null);
    }

    private boolean isValidEvent() {
        return getWhereColumnName() != null;
    }

    private String getEventColumnName() {
        String columnName = null;
        switch (persistenceEvent) {
            case ModelValidator.TYPE_AFTER_NEW:
                columnName = Changes.SP033_Insert;
                break;
            case ModelValidator.TYPE_AFTER_CHANGE:
                columnName = Changes.SP033_Update;
                break;
            case ModelValidator.TYPE_AFTER_DELETE:
                columnName = Changes.SP033_Delete;
                break;
        }
        return columnName;
    }

    private String getDocumentEventColumnName() {
        String columnName = null;
        switch (documentEvent) {
            case ModelValidator.TIMING_AFTER_PREPARE:
                columnName = Changes.SP033_Prepare;
                break;
            case ModelValidator.TIMING_AFTER_COMPLETE:
                columnName = Changes.SP033_Complete;
                break;
            case ModelValidator.TIMING_AFTER_CLOSE:
                columnName = Changes.SP033_Close;
                break;
            case ModelValidator.TIMING_AFTER_VOID:
                columnName = Changes.SP033_Void;
                break;
            case ModelValidator.TIMING_AFTER_REACTIVATE:
                columnName = Changes.SP033_Reactivate;
                break;
            case ModelValidator.TIMING_AFTER_REVERSECORRECT:
            case ModelValidator.TIMING_AFTER_REVERSEACCRUAL:
                columnName = Changes.SP033_Reverse;
                break;
            case ModelValidator.TIMING_AFTER_POST:
                columnName = Changes.SP033_Post;
                break;
        }
        return columnName;
    }

    private String getEventName() {
        String eventName = null;
        if(documentEvent > 0) {
            switch (documentEvent) {
                case ModelValidator.TIMING_AFTER_PREPARE:
                    eventName = "TIMING_AFTER_PREPARE";
                    break;
                case ModelValidator.TIMING_AFTER_COMPLETE:
                    eventName = "TIMING_AFTER_COMPLETE";
                    break;
                case ModelValidator.TIMING_AFTER_CLOSE:
                    eventName = "TIMING_AFTER_CLOSE";
                    break;
                case ModelValidator.TIMING_AFTER_VOID:
                    eventName = "TIMING_AFTER_VOID";
                    break;
                case ModelValidator.TIMING_AFTER_REACTIVATE:
                    eventName = "TIMING_AFTER_REACTIVATE";
                    break;
                case ModelValidator.TIMING_AFTER_REVERSECORRECT:
                    eventName = "TIMING_AFTER_REVERSECORRECT";
                    break;
                case ModelValidator.TIMING_AFTER_REVERSEACCRUAL:
                    eventName = "TIMING_AFTER_REVERSEACCRUAL";
                    break;
                case ModelValidator.TIMING_AFTER_POST:
                    eventName = "TIMING_AFTER_POST";
                    break;
            }
        } else if(persistenceEvent > 0) {
            switch (persistenceEvent) {
                case ModelValidator.TYPE_AFTER_NEW:
                    eventName = "TYPE_AFTER_NEW";
                    break;
                case ModelValidator.TYPE_AFTER_CHANGE:
                    eventName = "TYPE_AFTER_CHANGE";
                    break;
                case ModelValidator.TYPE_AFTER_DELETE:
                    eventName = "TYPE_AFTER_DELETE";
                    break;
            }
        }
        return eventName;
    }

    private String getWhereColumnName() {
        if(documentEvent > 0) {
            return getDocumentEventColumnName();
        }
        return getEventColumnName();
    }

    private List<Integer> getWebhookListeners() {
        return new Query(entity.getCtx(), Changes.SP033_Webhook, "EXISTS(SELECT 1 FROM SP033_WebhookTable wt " +
                "WHERE wt.SP033_Webhook_ID = SP033_Webhook.SP033_Webhook_ID " +
                "AND wt.AD_Table_ID = ? " +
                "AND wt." + getWhereColumnName() + " = 'Y' " +
                "AND wt.IsActive = 'Y')", null)
                .setParameters(entity.get_Table_ID())
                .setOnlyActiveRecords(true)
                .getIDsAsList();
    }

    public static List<String> getPersistenceTableNames(int clientId) {
        List<String> tableNames = new ArrayList<>();
        new Query(Env.getCtx(), I_AD_Table.Table_Name, "EXISTS(SELECT 1 FROM SP033_WebhookTable wt " +
                "WHERE wt.AD_Table_ID = AD_Table.AD_Table_ID " +
                "AND wt.AD_Client_ID = ? " +
                "AND wt.IsActive = 'Y' " +
                "AND (wt.SP033_Insert = 'Y' OR wt.SP033_Update = 'Y' OR wt.SP033_Delete = 'Y'))", null)
                .setOnlyActiveRecords(true)
                .setParameters(clientId)
                .getIDsAsList()
                .forEach(tableId -> {
                    tableNames.add(MTable.getTableName(Env.getCtx(), tableId));
                });
        return tableNames;
    }

    public static List<String> getDocumentTableNames(int clientId) {
        List<String> tableNames = new ArrayList<>();
        new Query(Env.getCtx(), I_AD_Table.Table_Name, "IsDocument = 'Y' " +
                "AND EXISTS(SELECT 1 FROM SP033_WebhookTable wt " +
                "WHERE wt.AD_Table_ID = AD_Table.AD_Table_ID " +
                "AND wt.AD_Client_ID = ? " +
                "AND wt.IsActive = 'Y' " +
                "AND (wt.SP033_Prepare = 'Y' OR wt.SP033_Complete = 'Y' OR wt.SP033_Close = 'Y' OR wt.SP033_Void = 'Y' OR wt.SP033_Reactivate = 'Y' OR wt.SP033_Reverse = 'Y' OR wt.SP033_Post = 'Y'))", null)
                .setOnlyActiveRecords(true)
                .setParameters(clientId)
                .getIDsAsList()
                .forEach(tableId -> {
                    tableNames.add(MTable.getTableName(Env.getCtx(), tableId));
                });
        return tableNames;
    }
}
