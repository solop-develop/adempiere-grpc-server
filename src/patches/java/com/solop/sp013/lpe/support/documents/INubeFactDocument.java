package com.solop.sp013.lpe.support.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solop.sp013.core.documents.IFiscalDocument;

/**
 * Document contract for Peru (NubeFact).
 * Holds the SUNAT comprobante type codes (tipo_de_comprobante) and the receiver document
 * groups as the single source of truth shared between the setup ({@code DeployLPENubeFact_v1})
 * and the conversion logic. Mirror of the Argentina/Uruguay {@code ICFEDocument}.
 *
 * The 3-group structure (mirror of Invoicy) is reserved here, but only the
 * {@link NF_InvoiceDocument} processor (comprobantes) is implemented in this iteration:
 *   - generar_comprobante: FACTURA (1), BOLETA (2), NOTA_CREDITO (3), NOTA_DEBITO (4)
 *   - generar_guia       : GRE_REMITENTE (7), GRE_TRANSPORTISTA (8)        -> pending
 *   - generar_retencion  : retencion / percepcion                          -> pending
 *
 * @author Gabriel Escalona
 */
public interface INubeFactDocument {

    /*  Operations (operacion)  */
    String OPERATION_GENERATE_COMPROBANTE = "generar_comprobante";
    String OPERATION_GENERATE_GUIA = "generar_guia";
    String OPERATION_GENERATE_RETENCION = "generar_retencion";
    String OPERATION_GENERATE_PERCEPCION = "generar_percepcion";
    String OPERATION_CONSULT_COMPROBANTE = "consultar_comprobante";
    String OPERATION_CONSULT_GUIA = "consultar_guia";
    String OPERATION_CONSULT_RETENCION = "consultar_retencion";
    String OPERATION_CONSULT_PERCEPCION = "consultar_percepcion";
    String OPERATION_GENERATE_VOID = "generar_anulacion";
    String OPERATION_CONSULT_VOID = "consultar_anulacion";
    String OPERATION_REVERSE_RETENCION = "generar_reversion_retencion";
    String OPERATION_REVERSE_PERCEPCION = "generar_reversion_percepcion";

    /*  Comprobante types (tipo_de_comprobante)  */
    String FACTURA = "1";
    String BOLETA = "2";
    String NOTA_CREDITO = "3";
    String NOTA_DEBITO = "4";
    /*  Guías de remisión  */
    String GRE_REMITENTE = "7";
    String GRE_TRANSPORTISTA = "8";
    /*  Comprobante de Retención (SUNAT 20) y de Percepción (SUNAT 40) — mismo documento, distinta
        naturaleza (compra/venta), ambos manejados por NF_Withholding  */
    String RETENCION = "20";
    String PERCEPCION = "40";

    /*  Receiver document groups (cliente_tipo_de_documento): 6 = RUC, 1 = DNI,
        4 = Carnet de extranjeria, 7 = Pasaporte, 0 = No domiciliado  */
    String RUC = "RUC";
    String DNI = "DNI";
    String FOREIGN_CARD = "CE";
    String PASSPORT = "PASSPORT";
    String NON_DOMICILED = "NO_DOMICILIADO";

    void setDocument(IFiscalDocument document);

    /**
     * NubeFact operation (operacion) used to send the document, e.g. {@code generar_comprobante}
     */
    String getOperation();

    /**
     * Whether the document is validated asynchronously by SUNAT (send then consult). Guías de
     * remisión are async; comprobantes are synchronous.
     */
    boolean isAsync();

    /**
     * Build the JSON body posted to the NubeFact RUTA to send (generar) this document.
     * @param mapper shared Jackson mapper from the api client
     */
    ObjectNode getConvertedDocument(ObjectMapper mapper);

    /**
     * Build the JSON body posted to the NubeFact RUTA to consult (consultar) this document, used
     * by the async two step flow to retrieve the final state once SUNAT accepts it.
     * @param mapper shared Jackson mapper from the api client
     */
    ObjectNode getConsultDocument(ObjectMapper mapper);

    /**
     * Build the JSON body to cancel/void this document at SUNAT (comunicación de baja for
     * comprobantes, reversión for retención/percepción). Returns null when the document type does
     * not support cancellation through the API.
     * @param mapper shared Jackson mapper from the api client
     * @param reason cancellation reason (motivo)
     */
    default ObjectNode getCancelDocument(ObjectMapper mapper, String reason) {
        return null;
    }

    boolean isValidForTransactionType(String transactionType);
}
