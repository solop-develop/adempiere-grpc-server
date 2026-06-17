package com.solop.sp013.lpe.support.nubefact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.documents.NonFiscalDocument;
import com.solop.sp013.core.documents.SenderResponse;
import com.solop.sp013.core.support.IFiscalDocumentCanceller;
import com.solop.sp013.core.support.IFiscalDocumentChecker;
import com.solop.sp013.core.support.IFiscalSender;
import com.solop.sp013.core.support.IFiscalSenderResponse;
import com.solop.sp013.lpe.support.documents.DocumentBuilder;
import com.solop.sp013.lpe.support.documents.INubeFactDocument;
import com.solop.sp013.lpe.support.documents.NubeFactFields;
import com.solop.sp013.lpe.util.LPEChanges;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.model.MADAppRegistration;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Electronic invoicing for Peru through the NubeFact REST API
 * (<a href="https://www.nubefact.com">nubefact.com</a>, JSON integration manual).
 *
 * The comprobante is sent to SUNAT through a single POST to the per-client RUTA:
 * { operacion: generar_comprobante, tipo_de_comprobante, serie, numero, ... }.
 * Mirror of the Argentina AfipSdk_v1 / Uruguay Invoicy_v1.
 *
 * Scope: comprobantes (Factura, Boleta, Nota de Crédito, Nota de Débito). Guías de remisión
 * (GRE) and retenciones are handled by their own processors in later iterations.
 *
 * @author Gabriel Escalona
 */
public class NubeFact_v1 implements IFiscalSender, IFiscalDocumentChecker, IFiscalDocumentCanceller {

    /**	Registration Id	*/
    protected int registrationId = 0;
    /**	Per-client NubeFact RUTA (full URL, e.g. https://api.nubefact.com/api/v1/&lt;id&gt;)	*/
    public static final String NUBEFACT_ROUTE = "nubefact_route";
    /**	NubeFact authorization token	*/
    public static final String NUBEFACT_TOKEN = "token";

    protected String route;
    protected String token;

    @Override
    public IFiscalSenderResponse sendFiscalDocument(IFiscalDocument fiscalDocument) {
        //	Document type agnostic (mirror of Invoicy): the DocumentBuilder selects the matching
        //	processor (comprobante, guía, retención) and each one builds its own JSON with its own
        //	operacion. Unsupported types are rejected by the builder (@SP013.UnsupportedDocument@).
        INubeFactDocument convertedDocument = DocumentBuilder.newInstance().withDocument(fiscalDocument).getDocumentProcessor();
        NubeFactApiClient client = getClient();
        try {
            ObjectNode body = convertedDocument.getConvertedDocument(client.getMapper());
            JsonNode response = client.post(body);
            //	Comprobantes must be accepted by SUNAT right away; guías (async) only need the request
            //	to be accepted by NubeFact, the SUNAT acceptance is retrieved later via checkFiscalDocument.
            return processResponse(response, fiscalDocument, !convertedDocument.isAsync());
        } catch (AdempiereException e) {
            throw e;
        } catch (Exception e) {
            throw new AdempiereException(e);
        }
    }

    @Override
    public IFiscalSenderResponse checkFiscalDocument(IFiscalDocument fiscalDocument) {
        INubeFactDocument convertedDocument = DocumentBuilder.newInstance().withDocument(fiscalDocument).getDocumentProcessor();
        NubeFactApiClient client = getClient();
        try {
            ObjectNode body = convertedDocument.getConsultDocument(client.getMapper());
            JsonNode response = client.post(body);
            //	Pending SUNAT acceptance is not an error: the response simply carries no download url
            //	yet and the queue will check again.
            return processResponse(response, fiscalDocument, false);
        } catch (AdempiereException e) {
            throw e;
        } catch (Exception e) {
            throw new AdempiereException(e);
        }
    }

    @Override
    public IFiscalSenderResponse cancelFiscalDocument(IFiscalDocument fiscalDocument, String reason) {
        INubeFactDocument convertedDocument = DocumentBuilder.newInstance().withDocument(fiscalDocument).getDocumentProcessor();
        NubeFactApiClient client = getClient();
        try {
            ObjectNode body = convertedDocument.getCancelDocument(client.getMapper(), reason);
            if(body == null) {
                throw new AdempiereException("@SP013.UnsupportedDocument@");
            }
            JsonNode response = client.post(body);
            //	The baja is async at SUNAT (returns a ticket): the NubeFact request being accepted is enough.
            return processResponse(response, fiscalDocument, false);
        } catch (AdempiereException e) {
            throw e;
        } catch (Exception e) {
            throw new AdempiereException(e);
        }
    }

    @Override
    public void sendNonFiscalDocument(NonFiscalDocument nonFiscalDocument) {

    }

    /**
     * Process the generar_comprobante response: create the SP013_LPE_Log entry and build the
     * sender response.
     */
    public IFiscalSenderResponse processResponse(JsonNode response, IFiscalDocument document, boolean requireSunatAcceptance) {
        AtomicReference<IFiscalSenderResponse> senderResponse = new AtomicReference<>();
        Trx.run(transactionName -> {
            MTable logTable = MTable.get(Env.getCtx(), LPEChanges.SP013_LPE_Log);
            PO log = logTable.getPO(0, transactionName);
            int documentId = document.getInvoiceId() > 0 ? document.getInvoiceId() : document.getInOutId();
            try {
                //	Every NubeFact response field is stored in its own column of SP013_LPE_Log.
                //	Identity
                int comprobanteType = response.path(NubeFactFields.VOUCHER_TYPE).asInt(0);
                String series = response.path(NubeFactFields.SERIES).asText(null);
                String number = response.has(NubeFactFields.NUMBER) ? response.path(NubeFactFields.NUMBER).asText() : null;
                //	Links: NubeFact returns the explicit enlace_del_* only when configured; otherwise just
                //	the base "enlace" (an HTML page). The pdf/xml/cdr are obtained by appending the extension.
                String baseLink = response.path(NubeFactFields.LINK).asText(null);
                String pdfLink = firstNonEmpty(response.path(NubeFactFields.PDF_LINK).asText(null), appendExtension(baseLink, ".pdf"));
                String xmlLink = firstNonEmpty(response.path(NubeFactFields.XML_LINK).asText(null), appendExtension(baseLink, ".xml"));
                String cdrLink = firstNonEmpty(response.path(NubeFactFields.CDR_LINK).asText(null), appendExtension(baseLink, ".cdr"));
                //	SUNAT result
                boolean acceptedBySunat = response.path(NubeFactFields.ACCEPTED_BY_SUNAT).asBoolean(false);
                String sunatDescription = response.path(NubeFactFields.SUNAT_DESCRIPTION).asText(null);
                String sunatNote = response.path(NubeFactFields.SUNAT_NOTE).asText(null);
                String sunatResponseCode = response.path(NubeFactFields.SUNAT_RESPONSE_CODE).asText(null);
                String sunatSoapError = response.path(NubeFactFields.SUNAT_SOAP_ERROR).asText(null);
                String sunatTicket = response.path(NubeFactFields.SUNAT_TICKET_NUMBER).asText(null);
                //	Fiscal extras
                String hash = response.path(NubeFactFields.HASH_CODE).asText(null);
                String qrCode = response.path(NubeFactFields.QR_CODE_STRING).asText(null);
                String barcode = response.path(NubeFactFields.BARCODE).asText(null);
                boolean hasVoided = response.has(NubeFactFields.VOIDED);
                boolean voided = response.path(NubeFactFields.VOIDED).asBoolean(false);
                //	Optional base64 blobs (only when "guardar archivos" is enabled in NubeFact)
                String pdfZip = response.path(NubeFactFields.PDF_ZIP_BASE64).asText(null);
                String xmlZip = response.path(NubeFactFields.XML_ZIP_BASE64).asText(null);
                String cdrZip = response.path(NubeFactFields.CDR_ZIP_BASE64).asText(null);
                //	Error response: { errors, codigo }
                String errors = response.path(NubeFactFields.ERRORS).asText(null);
                String errorCode = response.has(NubeFactFields.ERROR_CODE) ? response.path(NubeFactFields.ERROR_CODE).asText() : null;

                boolean isError = !Util.isEmpty(errors, true) || (requireSunatAcceptance && !acceptedBySunat);

                log.setAD_Org_ID(document.getOrganizationId());
                if(document.getInvoiceId() > 0) {
                    log.set_ValueOfColumn("C_Invoice_ID", document.getInvoiceId());
                } else if(document.getInOutId() > 0) {
                    log.set_ValueOfColumn("M_InOut_ID", document.getInOutId());
                }
                setColumn(log, "SP013_ComprobanteType", comprobanteType > 0 ? String.valueOf(comprobanteType) : null);
                setColumn(log, "SP013_Serial", series);
                setColumn(log, "SP013_SerialNo", number);
                setColumn(log, "SP013_Hash", hash);
                setColumn(log, "SP013_QRCode", qrCode);
                setColumn(log, "SP013_Barcode", barcode);
                setColumn(log, "SP013_Link", baseLink);
                setColumn(log, "SP013_DownloadURL", pdfLink);
                setColumn(log, "SP013_DownloadXmlURL", xmlLink);
                setColumn(log, "SP013_DownloadCdrURL", cdrLink);
                log.set_ValueOfColumn("SP013_IsAcceptedBySunat", acceptedBySunat);
                if(hasVoided) {
                    log.set_ValueOfColumn("SP013_IsVoided", voided);
                }
                setColumn(log, "SP013_StatusCode", sunatResponseCode);
                setColumn(log, "SP013_Description", sunatDescription);
                setColumn(log, "SP013_SunatTicket", sunatTicket);
                //	Errors (with their code) and the SOAP error are concatenated into Note;
                //	the SUNAT additional notes go to SP013_AdditionalInfo (mirror of Invoicy).
                StringBuilder note = new StringBuilder();
                if(!Util.isEmpty(errors, true)) {
                    note.append(Util.isEmpty(errorCode, true) ? "" : errorCode + " - ").append(errors).append(Env.NL);
                }
                if(!Util.isEmpty(sunatSoapError, true)) {
                    note.append(sunatSoapError).append(Env.NL);
                }
                setColumn(log, "Note", note.toString());
                setColumn(log, "SP013_AdditionalInfo", sunatNote);
                setColumn(log, "SP013_PdfZipBase64", pdfZip);
                setColumn(log, "SP013_XmlZipBase64", xmlZip);
                setColumn(log, "SP013_CdrZipBase64", cdrZip);

                String errorMessage = !Util.isEmpty(errors, true)
                        ? (Util.isEmpty(errorCode, true) ? errors : errorCode + " - " + errors)
                        : sunatDescription;

                senderResponse.set(SenderResponse.newInstance()
                        .withDocumentId(documentId)
                        .withDocumentUuid(document.getDocumentUuid())
                        .withDocumentNo(number)
                        .withSerialNo(Util.isEmpty(series, true) ? null : series + "-")
                        .withError(isError)
                        .withErrorMessage(errorMessage)
                        .withDounloadUrl(pdfLink)
                );
            } catch (Exception e) {
                log.set_ValueOfColumn("Note", e.getLocalizedMessage());
                senderResponse.set(SenderResponse.newInstance()
                        .withDocumentId(documentId)
                        .withDocumentUuid(document.getDocumentUuid())
                        .withError(true)
                        .withErrorMessage(e.getLocalizedMessage())
                );
            }
            log.saveEx();
        });
        return senderResponse.get();
    }

    /**
     * Set a log column only when the value is not empty (keeps the row clean of blank fields)
     */
    private static void setColumn(PO log, String columnName, String value) {
        if(!Util.isEmpty(value, true)) {
            log.set_ValueOfColumn(columnName, value);
        }
    }

    /**
     * First non-empty value, or null when both are empty
     */
    private static String firstNonEmpty(String first, String second) {
        if(!Util.isEmpty(first, true)) {
            return first;
        }
        return Util.isEmpty(second, true) ? null : second;
    }

    /**
     * Append a file extension to the NubeFact base link (enlace), avoiding a double extension
     */
    private static String appendExtension(String baseLink, String extension) {
        if(Util.isEmpty(baseLink, true)) {
            return null;
        }
        return baseLink.endsWith(extension) ? baseLink : baseLink + extension;
    }

    protected NubeFactApiClient getClient() {
        return new NubeFactApiClient(route, token);
    }

    @Override
    public String testConnection() {
        //	NubeFact has no ping endpoint: a consultar_comprobante tells auth (10/11/12) apart from
        //	a document-not-found (24), which already means the route and token are valid.
        NubeFactApiClient client = getClient();
        ObjectNode body = client.getMapper().createObjectNode();
        body.put(NubeFactFields.OPERATION, INubeFactDocument.OPERATION_CONSULT_COMPROBANTE);
        body.put(NubeFactFields.VOUCHER_TYPE, 1);
        body.put(NubeFactFields.SERIES, "FFF1");
        body.put(NubeFactFields.NUMBER, 1);
        JsonNode response = client.post(body);
        if(response.has(NubeFactFields.ERROR_CODE)) {
            String codigo = response.path(NubeFactFields.ERROR_CODE).asText();
            if("10".equals(codigo) || "11".equals(codigo) || "12".equals(codigo)
                    || "50".equals(codigo) || "51".equals(codigo)) {
                throw new AdempiereException("NubeFact [" + codigo + "] " + response.path(NubeFactFields.ERRORS).asText());
            }
        }
        return "Ok";
    }

    @Override
    public void setAppRegistrationId(int registrationId) {
        this.registrationId = registrationId;
        MADAppRegistration registration = MADAppRegistration.getById(Env.getCtx(), getAppRegistrationId(), null);
        route = registration.getParameterValue(NUBEFACT_ROUTE);
        if(Util.isEmpty(route, true)) {
            //	Fall back to the registration host when the route parameter is not set
            route = registration.getHost();
        }
        if(Util.isEmpty(route, true)) {
            throw new AdempiereException("@AD_AppRegistration_ID@: " + NUBEFACT_ROUTE + " @NotFound@");
        }
        token = registration.getParameterValue(NUBEFACT_TOKEN);
        if(Util.isEmpty(token, true)) {
            throw new AdempiereException("@AD_AppRegistration_ID@: " + NUBEFACT_TOKEN + " @NotFound@");
        }
    }

    @Override
    public int getAppRegistrationId() {
        return registrationId;
    }
}
