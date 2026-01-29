package com.solop.sp013.luy.support.invoicy;

import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.support.IFiscalSenderResponse;
import com.solop.sp013.luy.cfe.dto.invoicy.EncabezadoEnvioType;
import com.solop.sp013.luy.cfe.dto.invoicy.response.EnvioCFERetorno;
import com.solop.sp013.luy.cfe.dto.invoicy.v2.CFEInvoiCyCollectionType;
import com.solop.sp013.luy.cfe.dto.invoicy.v2.CFEInvoiCyType;
import com.solop.sp013.luy.cfe.dto.invoicy.v2.EnvioCFE;
import com.solop.sp013.luy.support.documents.v2.DocumentBuilder_v2;
import com.solop.sp013.luy.support.documents.v2.ICFEDocument_v2;
import org.adempiere.exceptions.AdempiereException;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Invoicy Version 2 - For test environment with new API structure
 * Extends v1 and uses v2 document builders and DTOs.
 *
 * Configure the system to use this class instead of Invoicy_v1 for the test environment.
 *
 * @author Gabriel Escalona
 */
public class Invoicy_v2 extends Invoicy_v1 {

    @Override
    public IFiscalSenderResponse sendFiscalDocument(IFiscalDocument fiscalDocument) {
        String currentDocumentNo;
        if (fiscalDocument.isManualDocument()) {
            currentDocumentNo = fiscalDocument.getDocumentNo();
        } else {
            currentDocumentNo = getLastFiscalDocumentNo(getDocumentId(fiscalDocument), getDocumentColumnName(fiscalDocument));
        }
        fiscalDocument.withFiscalDocumentNo(currentDocumentNo);

        // Use v2 DocumentBuilder and get v2 document processor
        ICFEDocument_v2 cfeDocument = DocumentBuilder_v2.newInstance().withDocument(fiscalDocument).getDocumentProcessor_v2();
        CFEInvoiCyType documentToSend = cfeDocument.getConvertedDocument_v2();
        if (documentToSend == null) {
            throw new AdempiereException("@SP013.ErrorFiscalDocument@");
        }

        // Use v2 envelope classes
        EnvioCFE envioCfe = new EnvioCFE();
        envioCfe.setCFE(new CFEInvoiCyCollectionType());
        List<CFEInvoiCyType> invoicyDtos = envioCfe.getCFE().getCFEItem();
        if (fiscalDocument.getDocumentFormat() >= 0) {
            documentToSend.getIdDoc().setCFEImpFormato(BigInteger.valueOf(fiscalDocument.getDocumentFormat()));
        }
        invoicyDtos.add(documentToSend);

        //  Send
        EncabezadoEnvioType header = new EncabezadoEnvioType();
        envioCfe.setEncabezado(header);
        header.setEmpPK(clientPK);
        header.setEmpCodigo(clientCode);
        AtomicReference<String> xmlStrSent = new AtomicReference<>();
        AtomicReference<String> xmlStrResponse = new AtomicReference<>();

        //  Run
        try {
            EnvioCFERetorno envioCfeRetorno = (EnvioCFERetorno) new WebServiceHandler(
                    clientCK, pushInvoiceUrl,
                    envioCfe, EnvioCFE.class, envioCfe.getCFE(), CFEInvoiCyCollectionType.class,
                    hash -> envioCfe.getEncabezado().setEmpCK(hash),
                    EnvioCFERetorno.class
            ).xmlUsed((request, response) -> {
                xmlStrSent.set(request);
                xmlStrResponse.set(response);
            }).call();
            return processResponse(xmlStrSent.get(), xmlStrResponse.get(), envioCfeRetorno, fiscalDocument);
        } catch (Exception e) {
            throw new AdempiereException(e);
        }
    }
}
