package com.solop.sp013.lpe.support.documents;

import com.solop.sp013.core.documents.IFiscalDocument;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Util;

import java.util.Arrays;
import java.util.List;

/**
 * Document builder for Peru (NubeFact). Dispatches the fiscal document to the matching
 * {@link INubeFactDocument} implementation based on its transaction type
 * (tipo_de_comprobante). Mirror of the Argentina/Uruguay {@code DocumentBuilder}.
 *
 * Only the comprobante group (FACTURA / BOLETA / NOTA_CREDITO / NOTA_DEBITO) is wired in
 * this iteration. The GRE and withholding groups are reserved in {@link INubeFactDocument}
 * and will be added as new processors without changing this dispatch contract.
 *
 * @author Gabriel Escalona
 */
public class DocumentBuilder {

    /**	Comprobante types (tipo_de_comprobante) sent through the generar_comprobante operation	*/
    private static final List<String> COMPROBANTE_TYPES = Arrays.asList(
            INubeFactDocument.FACTURA, INubeFactDocument.BOLETA,
            INubeFactDocument.NOTA_CREDITO, INubeFactDocument.NOTA_DEBITO
    );

    /**	Guía de remisión types (tipo_de_comprobante) sent through the generar_guia operation	*/
    private static final List<String> GUIA_TYPES = Arrays.asList(
            INubeFactDocument.GRE_REMITENTE, INubeFactDocument.GRE_TRANSPORTISTA
    );

    /**	Retención (generar_retencion) and percepción (generar_percepcion) — same document/processor	*/
    private static final List<String> WITHHOLDING_TYPES = Arrays.asList(
            INubeFactDocument.RETENCION, INubeFactDocument.PERCEPCION
    );

    private IFiscalDocument document;

    public static DocumentBuilder newInstance() {
        return new DocumentBuilder();
    }

    public IFiscalDocument getDocument() {
        return document;
    }

    public DocumentBuilder withDocument(IFiscalDocument document) {
        this.document = document;
        return this;
    }

    public INubeFactDocument getDocumentProcessor() {
        INubeFactDocument documentProcessor = new GenericDocument();
        if(document == null) {
            return documentProcessor;
        }
        if(isValidForComprobante(document.getTransactionType())) {
            documentProcessor = new NF_InvoiceDocument();
        } else if(isValidForGuia(document.getTransactionType())) {
            documentProcessor = new NF_DeliveryNote();
        } else if(isValidForWithholding(document.getTransactionType())) {
            documentProcessor = new NF_Withholding();
        }
        if(documentProcessor instanceof GenericDocument) {
            throw new AdempiereException("@SP013.UnsupportedDocument@");
        }
        documentProcessor.setDocument(document);
        return documentProcessor;
    }

    /**
     * Comprobante types supported by the NubeFact generar_comprobante operation
     */
    public static boolean isValidForComprobante(String transactionType) {
        if(Util.isEmpty(transactionType, true)) {
            return false;
        }
        return COMPROBANTE_TYPES.contains(transactionType.trim());
    }

    /**
     * Guía de remisión types supported by the NubeFact generar_guia operation
     */
    public static boolean isValidForGuia(String transactionType) {
        if(Util.isEmpty(transactionType, true)) {
            return false;
        }
        return GUIA_TYPES.contains(transactionType.trim());
    }

    /**
     * Retención types supported by the NubeFact generar_retencion operation
     */
    public static boolean isValidForWithholding(String transactionType) {
        if(Util.isEmpty(transactionType, true)) {
            return false;
        }
        return WITHHOLDING_TYPES.contains(transactionType.trim());
    }
}
