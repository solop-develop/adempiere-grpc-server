package com.solop.sp013.lpe.support.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solop.sp013.core.documents.IFiscalDocumentLine;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Optional;

/**
 * Converts a generic IFiscalDocument (built from a MInOut) into the NubeFact generar_guia JSON
 * body (Guía de Remisión Remitente / Transportista). Mirror of the Uruguay E_DeliveryNote.
 *
 * Async (two step) flow: generar_guia is sent first and consultar_guia retrieves the PDF/XML/CDR
 * once SUNAT accepts it (handled by the electronic document queue).
 *
 * NOTE: a complete GRE needs logistics data that the generic IFiscalDocument / MInOut do not carry
 * yet (motivo de traslado, peso bruto, transportista, conductor, ubigeo y puntos de partida/llegada,
 * documentos relacionados). Those fields are emitted with safe defaults and marked with TODO; they
 * must be sourced from new MInOut columns in a follow up iteration before going to production.
 *
 * @author Gabriel Escalona
 */
public class NF_DeliveryNote extends NF_InvoiceDocument {

    /**	Default transfer reason (motivo_de_traslado): 01 = Venta	*/
    public static final String DEFAULT_TRANSFER_REASON = "01";
    /**	Default gross weight unit (peso_bruto_unidad_de_medida): KGM = Kilogramos	*/
    public static final String WEIGHT_UNIT_KG = "KGM";
    /**	Default transport type (tipo_de_transporte): 01 = Transporte público	*/
    public static final String DEFAULT_TRANSPORT_TYPE = "01";

    @Override
    public String getOperation() {
        return OPERATION_GENERATE_GUIA;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public ObjectNode getConvertedDocument(ObjectMapper mapper) {
        ObjectNode guia = mapper.createObjectNode();
        guia.put(NubeFactFields.OPERATION, getOperation());
        guia.put(NubeFactFields.VOUCHER_TYPE, getComprobanteType());
        guia.put(NubeFactFields.SERIES, getSeries());
        guia.put(NubeFactFields.NUMBER, getNumber());
        //	Customer: for GRE Remitente this is the receiver (destinatario); for Transportista, the sender (remitente)
        guia.put(NubeFactFields.CUSTOMER_DOCUMENT_TYPE, getReceiverDocumentType());
        guia.put(NubeFactFields.CUSTOMER_DOCUMENT_NUMBER, Optional.ofNullable(document.getBusinessPartnerTaxId()).orElse(""));
        guia.put(NubeFactFields.CUSTOMER_NAME, Optional.ofNullable(document.getBusinessPartnerName()).orElse(""));
        guia.put(NubeFactFields.CUSTOMER_ADDRESS, Optional.ofNullable(document.getAddress1()).orElse(""));
        guia.put(NubeFactFields.CUSTOMER_EMAIL, Optional.ofNullable(document.getEMail()).orElse(""));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        guia.put(NubeFactFields.ISSUE_DATE, dateFormat.format(document.getDocumentDate()));
        guia.put(NubeFactFields.OBSERVATIONS, Optional.ofNullable(document.getDescription()).orElse(""));
        //	TODO GRE logistics data (needs new MInOut columns): replace these defaults.
        guia.put(NubeFactFields.TRANSFER_REASON, DEFAULT_TRANSFER_REASON);
        guia.put(NubeFactFields.GROSS_WEIGHT_TOTAL, BigDecimal.ONE);
        guia.put(NubeFactFields.GROSS_WEIGHT_UNIT, WEIGHT_UNIT_KG);
        guia.put(NubeFactFields.TRANSPORT_TYPE, DEFAULT_TRANSPORT_TYPE);
        guia.put(NubeFactFields.TRANSFER_START_DATE, dateFormat.format(document.getDocumentDate()));
        //	TODO transportista_* / conductor_* / punto_de_partida_* / punto_de_llegada_* / documento_relacionado[]
        guia.set(NubeFactFields.ITEMS, convertGuiaItems(mapper));
        guia.put(NubeFactFields.SEND_AUTOMATICALLY_TO_CUSTOMER, false);
        return guia;
    }

    @Override
    public ObjectNode getConsultDocument(ObjectMapper mapper) {
        ObjectNode consult = mapper.createObjectNode();
        consult.put(NubeFactFields.OPERATION, OPERATION_CONSULT_GUIA);
        consult.put(NubeFactFields.VOUCHER_TYPE, getComprobanteType());
        consult.put(NubeFactFields.SERIES, getSeries());
        consult.put(NubeFactFields.NUMBER, getNumber());
        return consult;
    }

    /**
     * Build the items array for the guía (quantity, code and description; no amounts/taxes)
     */
    private ArrayNode convertGuiaItems(ObjectMapper mapper) {
        ArrayNode items = mapper.createArrayNode();
        for(IFiscalDocumentLine line : document.getFiscalDocumentLines()) {
            BigDecimal quantity = Optional.ofNullable(line.getQuantity()).orElse(BigDecimal.ZERO);
            ObjectNode item = mapper.createObjectNode();
            item.put(NubeFactFields.MEASURE_UNIT, Optional.ofNullable(line.getProductUnitOfMeasure()).filter(value -> !Util.isEmpty(value, true)).orElse(UNIT_PRODUCT));
            item.put(NubeFactFields.CODE, Optional.ofNullable(line.getProductValue()).orElse(""));
            item.put(NubeFactFields.DESCRIPTION, Optional.ofNullable(line.getProductName()).orElse(Optional.ofNullable(line.getLineDescription()).orElse("")));
            item.put(NubeFactFields.QUANTITY, quantity.setScale(6, RoundingMode.HALF_UP));
            items.add(item);
        }
        return items;
    }

    /**
     * GRE cannot be voided through the API (the baja is done only from SUNAT with Clave Sol).
     */
    @Override
    public ObjectNode getCancelDocument(ObjectMapper mapper, String reason) {
        return null;
    }

    @Override
    public boolean isValidForTransactionType(String transactionType) {
        return DocumentBuilder.isValidForGuia(transactionType);
    }
}
