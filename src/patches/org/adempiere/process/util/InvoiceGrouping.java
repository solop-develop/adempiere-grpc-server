package org.adempiere.process.util;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;

import java.util.HashMap;
import java.util.Map;

public class InvoiceGrouping {

    public static InvoiceGrouping newInstance() {
        return new InvoiceGrouping();
    }

    public InvoiceGrouping() {
        maxLinesStorage = new HashMap<>();
        currentLinesStorage = new HashMap<>();
        currentHashStorage = new HashMap<>();
    }

    public String getKey(MOrder order, int invoiceDocumentTypeId, boolean isConsolidated) {
        String orderKey = getOrderKey(order, isConsolidated);
        int maxLines = getMaxLinesStorage(orderKey, order, invoiceDocumentTypeId);
        if(maxLines == 0) {
            return orderKey;
        }
        //  Controlled
        return getHash(orderKey, maxLines);
    }
    public String getKey(MOrder order, int invoiceDocumentTypeId, boolean isConsolidated, String additionalKey) {
        String orderKey = getOrderKey(order, isConsolidated);
        orderKey += additionalKey;
        int maxLines = getMaxLinesStorage(orderKey, order, invoiceDocumentTypeId);
        if(maxLines == 0) {
            return orderKey;
        }
        //  Controlled
        return getHash(orderKey, maxLines);
    }

    private final Map<String, Integer> maxLinesStorage;
    private final Map<String, Integer> currentLinesStorage;
    private final Map<String, Integer> currentHashStorage;

    private String getOrderKey(MOrder order, boolean isConsolidated) {
        if(isConsolidated) {
            return String.valueOf(order.getBill_Location_ID());
        }
        return String.valueOf(order.getC_Order_ID());
    }

    private int getMaxLinesStorage(String orderKey, MOrder order, int invoiceDocumentTypeId) {
        int maxLines = maxLinesStorage.getOrDefault(orderKey, -1);
        if(maxLines == -1) {
            if(invoiceDocumentTypeId > 0) {
                MDocType invoiceDocType = MDocType.get(order.getCtx(),invoiceDocumentTypeId);
                maxLines = invoiceDocType.get_ValueAsInt("MaxLinesPerDocument");
            } else {
                MDocType orderDocType = MDocType.get(order.getCtx(), order.getC_DocTypeTarget_ID());
                int invoiceDocTypeId = orderDocType.getC_DocTypeInvoice_ID();
                if (invoiceDocTypeId <= 0) {
                    throw new AdempiereException("@NotFound@ @C_DocTypeInvoice_ID@ - @C_DocType_ID@:" + orderDocType.get_Translation("Name"));
                }
                MDocType invoiceDocType = MDocType.get(order.getCtx(), invoiceDocTypeId);
                maxLines = invoiceDocType.get_ValueAsInt("MaxLinesPerDocument");
            }
            maxLinesStorage.put(orderKey, maxLines);
        } else {
            return maxLines;
        }
        return maxLines;
    }

    private String getHash(String orderKey, int maxLines) {
        int currentLines = currentLinesStorage.getOrDefault(orderKey, 0);
        int currentHash = currentHashStorage.getOrDefault(orderKey, 0);
        String hash = orderKey + "-" + currentHash;
        currentLines ++;
        if(maxLines == currentLines) {
            currentLines = 0;
            currentHash++;
            currentHashStorage.put(orderKey, currentHash);
        }
        currentLinesStorage.put(orderKey, currentLines);
        return hash;
    }
}
