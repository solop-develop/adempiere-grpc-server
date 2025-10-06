package com.solop.sp013.luy.support.documents;

import com.solop.sp013.core.documents.IFiscalDocument;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Util;

/**
 * Document Builder
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class DocumentBuilder {
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

    public ICFEDocument getDocumentProcessor() {
        ICFEDocument documentProcessor = new GenericDocument();
        if(document == null) {
            return documentProcessor;
        }
        //   e-Ticket Implementation
        if(isValidForETicket(document.getTransactionType()) || isValidForEInvoice(document.getTransactionType())) {
            documentProcessor = new E_InvoiceDocument();
        } else if(isValidForEDeliveryNote(document.getTransactionType())) {
            documentProcessor = new E_DeliveryNote();
        } else if(isValidForEWithholding(document.getTransactionType())) {
            documentProcessor = new E_Withholding();
        }
        if(documentProcessor instanceof GenericDocument) {
            throw new AdempiereException("@SP013.UnsupportedDocument@");
        }
        //  Set Document
        documentProcessor.setDocument(document);
        return documentProcessor;
    }

    public static boolean isValidForETicket(String transactionType) {
        if(Util.isEmpty(transactionType, true)) {
            return false;
        }
        return transactionType.equals(ICFEDocument.E_TICKET)
                || transactionType.equals(ICFEDocument.E_TICKET_NC)
                || transactionType.equals(ICFEDocument.E_TICKET_ND)
                || transactionType.equals(ICFEDocument.E_TICKET_VXCA)
                || transactionType.equals(ICFEDocument.E_TICKET_NC_VXCA)
                || transactionType.equals(ICFEDocument.E_TICKET_ND_VXCA)
                || transactionType.equals(ICFEDocument.E_TICKET_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_TICKET_NC_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_TICKET_ND_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_TICKET_VXCA_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_TICKET_VXCA_NC_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_TICKET_VXCA_ND_CONTINGENCY)
                ;
    }

    public static boolean isValidForEInvoice(String transactionType) {
        if(Util.isEmpty(transactionType, true)) {
            return false;
        }
        return transactionType.equals(ICFEDocument.E_FACTURARET)
                || transactionType.equals(ICFEDocument.E_FACTURA_NC)
                || transactionType.equals(ICFEDocument.E_FACTURA_ND)
                || transactionType.equals(ICFEDocument.E_FACTURA_VXCA)
                || transactionType.equals(ICFEDocument.E_FACTURA_NC_VXCA)
                || transactionType.equals(ICFEDocument.E_FACTURA_ND_VXCA)
                || transactionType.equals(ICFEDocument.E_FACTURA_EXP)
                || transactionType.equals(ICFEDocument.E_FACTURA_NC_EXP)
                || transactionType.equals(ICFEDocument.E_FACTURA_ND_EXP)
                || transactionType.equals(ICFEDocument.E_FACTURA_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_NC_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_ND_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_EXP_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_EXP_NC_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_EXP_ND_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_VXCA_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_VXCA_NC_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_FACTURA_VXCA_ND_CONTINGENCY)
                ;
    }

    public static boolean isValidForEDeliveryNote(String transactionType) {
        if(Util.isEmpty(transactionType, true)) {
            return false;
        }
        return transactionType.equals(ICFEDocument.E_REMITO)
                || transactionType.equals(ICFEDocument.E_REMITO_CONTINGENCY)
                || transactionType.equals(ICFEDocument.E_REMITO_EXP_CONTINGENCY)
                ;
    }

    public static boolean isValidForEWithholding(String transactionType) {
        if(Util.isEmpty(transactionType, true)) {
            return false;
        }
        return transactionType.equals(ICFEDocument.E_RESGUARDO)
                || transactionType.equals(ICFEDocument.E_RESGUARDO_CONTINGENCY)
                ;
    }
}
