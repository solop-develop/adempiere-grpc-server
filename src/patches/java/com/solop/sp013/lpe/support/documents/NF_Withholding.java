package com.solop.sp013.lpe.support.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solop.sp013.core.documents.IFiscalDocumentLine;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Optional;

/**
 * Converts a generic IFiscalDocument into the NubeFact generar_retencion / generar_percepcion JSON
 * body. Mirror of the Uruguay E_Withholding (which also unifies retención y percepción via RetPerc).
 *
 * Both are the SAME document with a different nature: <b>retención</b> is purchase side (withheld
 * from the supplier, the "with" amount is base − applied) and <b>percepción</b> is sales side
 * (charged to the customer, the "with" amount is base + applied). The processor is selected for
 * both types by the {@link DocumentBuilder}; the nature is resolved here from the transaction type.
 *
 * Synchronous (the response already carries aceptada_por_sunat). Sent to the same RUTA/token, has no
 * {@code tipo_de_comprobante}; its items are the related documents (invoices) with the amount applied.
 *
 * NOTE: the per-item related-document identity (tipo / serie / número / fecha) is NOT carried by the
 * generic IFiscalDocument; it is emitted with best-effort values and marked TODO. To issue a valid
 * document those fields must be sourced from the document allocations (related invoice + payment).
 *
 * @author Gabriel Escalona
 */
public class NF_Withholding extends NF_InvoiceDocument {

    /**	Default related document type (documento_relacionado_tipo): 01 = Factura	*/
    public static final String RELATED_DOCUMENT_INVOICE = "01";

    /**
     * True when the document is a percepción (sales), false for a retención (purchases)
     */
    protected boolean isPerception() {
        return PERCEPCION.equals(Optional.ofNullable(document.getTransactionType()).map(String::trim).orElse(""));
    }

    @Override
    public String getOperation() {
        return isPerception() ? OPERATION_GENERATE_PERCEPCION : OPERATION_GENERATE_RETENCION;
    }

    @Override
    public boolean isAsync() {
        //	Synchronous: the response already carries aceptada_por_sunat
        return false;
    }

    @Override
    public ObjectNode getConvertedDocument(ObjectMapper mapper) {
        boolean perception = isPerception();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String emissionDate = dateFormat.format(document.getDocumentDate());
        ObjectNode root = mapper.createObjectNode();
        root.put(NubeFactFields.OPERATION, getOperation());
        root.put(NubeFactFields.SERIES, getSeries());
        root.put(NubeFactFields.NUMBER, getNumber());
        //	Other party: only RUC entities are allowed (cliente_tipo_de_documento must be 6)
        root.put(NubeFactFields.CUSTOMER_DOCUMENT_TYPE, getReceiverDocumentType());
        root.put(NubeFactFields.CUSTOMER_DOCUMENT_NUMBER, Optional.ofNullable(document.getBusinessPartnerTaxId()).orElse(""));
        root.put(NubeFactFields.CUSTOMER_NAME, Optional.ofNullable(document.getBusinessPartnerName()).orElse(""));
        root.put(NubeFactFields.CUSTOMER_ADDRESS, Optional.ofNullable(document.getAddress1()).orElse(""));
        root.put(NubeFactFields.CUSTOMER_EMAIL, Optional.ofNullable(document.getEMail()).orElse(""));
        root.put(NubeFactFields.ISSUE_DATE, emissionDate);
        root.put(NubeFactFields.CURRENCY, "1"); //	Always Soles
        root.put(perception ? NubeFactFields.PERCEPTION_RATE_TYPE : NubeFactFields.RETENTION_RATE_TYPE, getRateCode(perception));
        root.put(NubeFactFields.OBSERVATIONS, Optional.ofNullable(document.getDescription()).orElse(""));
        root.put(NubeFactFields.SEND_AUTOMATICALLY_TO_SUNAT, true);
        root.put(NubeFactFields.SEND_AUTOMATICALLY_TO_CUSTOMER, false);
        //	Items: one per related document (invoice) with the applied amount
        ArrayNode items = mapper.createArrayNode();
        BigDecimal totalApplied = BigDecimal.ZERO;  //	total_percibido / total_retenido
        BigDecimal totalWith = BigDecimal.ZERO;     //	total_cobrado / total_pagado
        int paymentNumber = 1;
        for(IFiscalDocumentLine line : document.getFiscalDocumentLines()) {
            BigDecimal applied = scale(line.getLineTotalAmount());
            BigDecimal base = scale(line.getWithholdingBaseAmount());
            //	Perception adds to the amount, retention subtracts from it
            BigDecimal withAmount = perception ? base.add(applied) : base.subtract(applied);
            ObjectNode item = mapper.createObjectNode();
            //	TODO related document identity (needs the document allocations in the model):
            //	documento_relacionado_tipo / _serie / _numero / _fecha_de_emision
            item.put(NubeFactFields.RELATED_DOCUMENT_TYPE, RELATED_DOCUMENT_INVOICE);
            item.put(NubeFactFields.RELATED_DOCUMENT_SERIES, "");
            item.put(NubeFactFields.RELATED_DOCUMENT_NUMBER, 0);
            item.put(NubeFactFields.RELATED_DOCUMENT_ISSUE_DATE, emissionDate);
            item.put(NubeFactFields.RELATED_DOCUMENT_CURRENCY, getCurrencyCode());
            item.put(NubeFactFields.RELATED_DOCUMENT_TOTAL, base);
            item.put(perception ? NubeFactFields.PERCEPTION_COLLECTION_DATE : NubeFactFields.RETENTION_PAYMENT_DATE, emissionDate);
            item.put(perception ? NubeFactFields.COLLECTION_NUMBER : NubeFactFields.PAYMENT_NUMBER, paymentNumber++);
            item.put(perception ? NubeFactFields.COLLECTION_TOTAL_WITHOUT_PERCEPTION : NubeFactFields.PAYMENT_TOTAL_WITHOUT_RETENTION, base);
            if(getCurrencyCode() != CURRENCY_PEN) {
                item.put(NubeFactFields.EXCHANGE_RATE, scale(document.getCurrencyRate()));
                item.put(NubeFactFields.EXCHANGE_RATE_DATE, emissionDate);
            }
            item.put(perception ? NubeFactFields.PERCEIVED_AMOUNT : NubeFactFields.RETAINED_AMOUNT, applied);
            item.put(perception ? NubeFactFields.PERCEIVED_AMOUNT_DATE : NubeFactFields.RETAINED_AMOUNT_DATE, emissionDate);
            item.put(perception ? NubeFactFields.AMOUNT_COLLECTED_WITH_PERCEPTION : NubeFactFields.AMOUNT_PAID_WITH_RETENTION, withAmount);
            items.add(item);
            totalApplied = totalApplied.add(applied);
            totalWith = totalWith.add(withAmount);
        }
        root.put(perception ? NubeFactFields.TOTAL_PERCEIVED : NubeFactFields.TOTAL_RETAINED, totalApplied);
        root.put(perception ? NubeFactFields.TOTAL_COLLECTED : NubeFactFields.TOTAL_PAID, totalWith);
        root.set(NubeFactFields.ITEMS, items);
        return root;
    }

    /**
     * Rate code (tipo_de_tasa_de_*) from the line withholding/perception rate.
     * Retención: 6% -> 2, else 3% -> 1. Percepción: 1% -> 2, 0.5% -> 3, else 2% -> 1.
     */
    private String getRateCode(boolean perception) {
        BigDecimal rate = firstRate();
        if(perception) {
            if(rate != null && rate.compareTo(BigDecimal.ONE) == 0) {
                return "2";
            }
            if(rate != null && rate.compareTo(new BigDecimal("0.5")) == 0) {
                return "3";
            }
            return "1";
        }
        if(rate != null && rate.compareTo(new BigDecimal("6")) == 0) {
            return "2";
        }
        return "1";
    }

    private BigDecimal firstRate() {
        for(IFiscalDocumentLine line : document.getFiscalDocumentLines()) {
            if(line.getWithholdingRate() != null && line.getWithholdingRate().signum() > 0) {
                return line.getWithholdingRate();
            }
        }
        return null;
    }

    @Override
    public ObjectNode getConsultDocument(ObjectMapper mapper) {
        ObjectNode consult = mapper.createObjectNode();
        consult.put(NubeFactFields.OPERATION, isPerception() ? OPERATION_CONSULT_PERCEPCION : OPERATION_CONSULT_RETENCION);
        consult.put(NubeFactFields.SERIES, getSeries());
        consult.put(NubeFactFields.NUMBER, getNumber());
        return consult;
    }

    /**
     * Reversión de la retención/percepción (generar_reversion_retencion / _percepcion).
     */
    @Override
    public ObjectNode getCancelDocument(ObjectMapper mapper, String reason) {
        ObjectNode reversion = mapper.createObjectNode();
        reversion.put(NubeFactFields.OPERATION, isPerception() ? OPERATION_REVERSE_PERCEPCION : OPERATION_REVERSE_RETENCION);
        reversion.put(NubeFactFields.SERIES, getSeries());
        reversion.put(NubeFactFields.NUMBER, getNumber());
        reversion.put(NubeFactFields.REASON, Util.isEmpty(reason, true) ? "ANULACION" : reason);
        return reversion;
    }

    @Override
    public boolean isValidForTransactionType(String transactionType) {
        return DocumentBuilder.isValidForWithholding(transactionType);
    }
}
