package com.solop.sp013.luy.support.documents.v2;

import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.luy.support.documents.DocumentBuilder;
import com.solop.sp013.luy.support.documents.ICFEDocument;
import org.adempiere.exceptions.AdempiereException;

/**
 * Document Builder Version 2
 * Uses v2 document implementations that work with CFEInvoiCyType for the new provider API structure.
 *
 * @author Gabriel Escalona
 */
public class DocumentBuilder_v2 extends DocumentBuilder {

    public static DocumentBuilder_v2 newInstance() {
        return new DocumentBuilder_v2();
    }

    @Override
    public DocumentBuilder_v2 withDocument(IFiscalDocument document) {
        super.withDocument(document);
        return this;
    }

    /**
     * Get document processor that returns ICFEDocument_v2
     * @return ICFEDocument_v2 processor
     */
    public ICFEDocument_v2 getDocumentProcessor_v2() {
        IFiscalDocument document = getDocument();
        if (document == null) {
            throw new AdempiereException("@SP013.UnsupportedDocument@");
        }

        ICFEDocument_v2 documentProcessor;
        //   e-Ticket / e-Factura Implementation using v2 classes
        if (isValidForETicket(document.getTransactionType()) || isValidForEInvoice(document.getTransactionType())) {
            documentProcessor = new E_InvoiceDocument_v2();
        } else if (isValidForEDeliveryNote(document.getTransactionType())) {
            documentProcessor = new E_DeliveryNote_v2();
        } else if (isValidForEWithholding(document.getTransactionType())) {
            documentProcessor = new E_Withholding_v2();
        } else {
            throw new AdempiereException("@SP013.UnsupportedDocument@");
        }

        //  Set Document
        documentProcessor.setDocument(document);
        return documentProcessor;
    }

    @Override
    public ICFEDocument getDocumentProcessor() {

        IFiscalDocument document = getDocument();
        if (document == null) {
            throw new AdempiereException("@SP013.UnsupportedDocument@");
        }

        ICFEDocument_v2 documentProcessor;
        //   e-Ticket / e-Factura Implementation using v2 classes
        if (isValidForETicket(document.getTransactionType()) || isValidForEInvoice(document.getTransactionType())) {
            documentProcessor = new E_InvoiceDocument_v2();
        } else if (isValidForEDeliveryNote(document.getTransactionType())) {
            documentProcessor = new E_DeliveryNote_v2();
        } else if (isValidForEWithholding(document.getTransactionType())) {
            documentProcessor = new E_Withholding_v2();
        } else {
            throw new AdempiereException("@SP013.UnsupportedDocument@");
        }

        //  Set Document
        documentProcessor.setDocument(document);
        return documentProcessor;






        // For backwards compatibility, delegate to v2 processor
        // Note: This returns ICFEDocument but the actual implementation is ICFEDocument_v2
        //return getDocumentProcessor_v2();
    }
}
