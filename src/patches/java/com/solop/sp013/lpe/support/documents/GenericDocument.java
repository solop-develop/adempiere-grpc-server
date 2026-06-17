package com.solop.sp013.lpe.support.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solop.sp013.core.documents.IFiscalDocument;

/**
 * Generic (unsupported) document implementation for Peru.
 * Mirror of the Argentina/Uruguay {@code GenericDocument}.
 *
 * @author Gabriel Escalona
 */
public class GenericDocument implements INubeFactDocument {

    @Override
    public void setDocument(IFiscalDocument document) {

    }

    @Override
    public String getOperation() {
        return null;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public ObjectNode getConvertedDocument(ObjectMapper mapper) {
        return null;
    }

    @Override
    public ObjectNode getConsultDocument(ObjectMapper mapper) {
        return null;
    }

    @Override
    public boolean isValidForTransactionType(String transactionType) {
        return false;
    }
}
