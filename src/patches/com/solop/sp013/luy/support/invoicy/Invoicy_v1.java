package com.solop.sp013.luy.support.invoicy;

import com.solop.sp013.core.documents.FiscalDocumentBuilder;
import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.documents.NonFiscalDocument;
import com.solop.sp013.core.documents.SenderResponse;
import com.solop.sp013.core.support.GetInvoicesResult;
import com.solop.sp013.core.support.IFiscalSender;
import com.solop.sp013.core.support.IFiscalSenderResponse;
import com.solop.sp013.core.support.IGetElectronicInvoices;
import com.solop.sp013.core.support.IGetInvoicesResult;
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import com.solop.sp013.luy.cfe.dto.invoicy.CFEInvoiCyCollectionType;
import com.solop.sp013.luy.cfe.dto.invoicy.CFEInvoiCyType;
import com.solop.sp013.luy.cfe.dto.invoicy.EncabezadoEnvioType;
import com.solop.sp013.luy.cfe.dto.invoicy.EnvioCFE;
import com.solop.sp013.luy.cfe.dto.invoicy.response.EnvioCFERetorno;
import com.solop.sp013.luy.cfe.dto.invoicy.response.ListaCFERetornoType;
import com.solop.sp013.luy.cfe.helper.dto.ConsultaCFERecibidos;
import com.solop.sp013.luy.cfe.helper.dto.ConsultaCFERecibidosRetorno;
import com.solop.sp013.luy.cfe.helper.dto.DescargaCFERecibidos;
import com.solop.sp013.luy.cfe.helper.dto.DescargaCFERecibidosRetorno;
import com.solop.sp013.luy.support.documents.DocumentBuilder;
import com.solop.sp013.luy.support.documents.ICFEDocument;
import com.solop.sp013.luy.util.LUYChanges;
import io.vavr.Tuple3;
import org.adempiere.core.domains.models.I_AD_OrgInfo;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_I_Invoice;
import org.adempiere.core.domains.models.X_I_Invoice;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MCountry;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPeriod;
import org.compiere.model.MPriceList;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.model.MADAppRegistration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class Invoicy_v1 implements IFiscalSender, IGetElectronicInvoices {

    /**	Registration Id	*/
    private int registrationId = 0;
    /**	CK	*/
    public static final String INVOICY_CK = "invoicy_ck";
    /**	PK	*/
    public static final String INVOICY_PK = "invoicy_pk";
    /**	Push Invoice Endpoint	*/
    public static final String INVOICY_PUSH_INVOICE = "invoicy_push_invoice";
    /**	Get Receipt Endpoint	*/
    public static final String INVOICY_GET_RECEIPT = "invoicy_get_receipt";
    /**	Download Invoice Endpoint	*/
    public static final String INVOICY_DOWNLOAD_RECEIPT = "invoicy_download_receipt";
    /**	Code	*/
    public static final String CODE = "code";
    private String pushInvoiceUrl;
    private String getInvoiceUrl;
    private String downloadInvoiceUrl;
    private String clientCode;
    private String clientPK;
    private String clientCK;


    SimpleDateFormat simpleDateFormat;

    @Override
    public IFiscalSenderResponse sendFiscalDocument(IFiscalDocument fiscalDocument) {
        boolean isManualDocument = false;
        String currentDocumentNo = null;
        if (fiscalDocument.isManualDocument()){
            currentDocumentNo = fiscalDocument.getDocumentNo();
        } else {
            currentDocumentNo = getLastFiscalDocumentNo(getDocumentId(fiscalDocument), getDocumentColumnName(fiscalDocument));
        }
        fiscalDocument.withFiscalDocumentNo(currentDocumentNo);
        ICFEDocument cfeDocument = DocumentBuilder.newInstance().withDocument(fiscalDocument).getDocumentProcessor();
        CFEInvoiCyType documentToSend = cfeDocument.getConvertedDocument();
        if(documentToSend == null) {
            throw new AdempiereException("@SP013.ErrorFiscalDocument@");
        }
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
                     clientCK, pushInvoiceUrl
                    , envioCfe, EnvioCFE.class, envioCfe.getCFE(), CFEInvoiCyCollectionType.class
                    , hash -> envioCfe.getEncabezado().setEmpCK(hash)
                    , EnvioCFERetorno.class
            ).xmlUsed((request, response) -> {
                xmlStrSent.set(request);
                xmlStrResponse.set(response);
            }).call();
            return processResponse(xmlStrSent.get(), xmlStrResponse.get(), envioCfeRetorno, fiscalDocument);
        } catch (Exception e) {
            throw new AdempiereException(e);
        }
    }

    @Override
    public void sendNonFiscalDocument(NonFiscalDocument nonFiscalDocument) {

    }
    private int getDocumentId(IFiscalDocument fiscalDocument) {
        int result = fiscalDocument.getInvoiceId();
        if (result <= 0) {
            result = fiscalDocument.getInOutId();
        }
        return result;
    }
    private String getDocumentColumnName(IFiscalDocument fiscalDocument) {
        String result = "C_Invoice_ID";
        if (fiscalDocument.getInOutId() > 0) {
            result = "M_InOut_ID";
        }
        return result;
    }


    public IFiscalSenderResponse processResponse(String xmlSent, String xmlResponse, EnvioCFERetorno envioCFERetorno, IFiscalDocument document) {
        AtomicReference<IFiscalSenderResponse> response = new AtomicReference<>();
        Trx.run(transactionName -> {
            MTable logTable = MTable.get(Env.getCtx(), LUYChanges.SP013_LUY_Log);
            PO log = logTable.getPO(0, transactionName);
            int documentId = -1;
            try {
                ListaCFERetornoType.CFE cfe = envioCFERetorno.getListaCFE().getCFE().get(0);
                log.setAD_Org_ID(document.getOrganizationId());
                log.set_ValueOfColumn("SP013_SecurityCode", cfe.getCFECodigoSeguridad());
                log.set_ValueOfColumn("SP013_Code", cfe.getCFEMsgCod());
                log.set_ValueOfColumn("SP013_Description", cfe.getCFEMsgDsc());
                log.set_ValueOfColumn("SP013_SerialNo", String.valueOf(cfe.getCFENro()));
                log.set_ValueOfColumn("SP013_ReferenceNo", String.valueOf(cfe.getCFENumReferencia()));
                log.set_ValueOfColumn("SP013_DownloadURL", cfe.getCFERepImpressa());
                log.set_ValueOfColumn("SP013_Serial", cfe.getCFESerie());
                log.set_ValueOfColumn("SP013_StatusCode", String.valueOf(cfe.getCFEStatus()));
                ListaCFERetornoType.CFE.CFEDatosAvanzados cfeAdvanced = cfe.getCFEDatosAvanzados();
                if(cfeAdvanced != null) {
                    log.set_ValueOfColumn("SP013_Resolution", "0000" + cfeAdvanced.getNumResAutorizadora() + "/" + cfeAdvanced.getAnoResAutorizadora());
                    log.set_ValueOfColumn("SP013_Identifier", String.valueOf(cfeAdvanced.getCFECAEId()));
                    log.set_ValueOfColumn("SP013_BeginningNumber", BigDecimal.valueOf(cfeAdvanced.getCFECAENroIni().intValue()));
                    log.set_ValueOfColumn("SP013_EndingNumber", BigDecimal.valueOf(cfeAdvanced.getCFECAENroFin().intValue()));
                    log.set_ValueOfColumn("SP013_DueDate", new Timestamp(cfeAdvanced.getCFECAEFchVenc().toGregorianCalendar().getTimeInMillis()));
                }
                log.set_ValueOfColumn("SP013_QRCode", cfe.getCFEQrCode());
                StringBuilder note = new StringBuilder();
                if (cfe.getErros() != null) {
                    for (ListaCFERetornoType.CFE.Erros.ErrosItem errosItem : cfe.getErros().getErrosItem()) {
                        note.append(errosItem.getCFEErrCod()).append(" - ").append(errosItem.getCFEErrDesc()).append(Env.NL);
                    }
                }
                log.set_ValueOfColumn("Note", note.toString());
                StringBuilder extraInfo = new StringBuilder();
                if (cfe.getErrosDGI() != null) {
                    for (ListaCFERetornoType.CFE.ErrosDGI.ErrosDGIItem errosDGIItem : cfe.getErrosDGI().getErrosDGIItem()) {
                        extraInfo.append(errosDGIItem.getCFERetCod()).append(" - ").append(errosDGIItem.getCFERetDesc()).append(Env.NL);
                    }
                }
                log.set_ValueOfColumn("SP013_AdditionalInfo", extraInfo.toString());
                //  Reference
                documentId = document.getInvoiceId();
                if(documentId > 0) {
                    log.set_ValueOfColumn("C_Invoice_ID", documentId);
                } else if (document.getInOutId() > 0) {
                    documentId = document.getInOutId();
                    log.set_ValueOfColumn("M_InOut_ID", documentId);
                }

                response.set(SenderResponse.newInstance()
                        .withDocumentId(documentId)
                        .withDocumentUuid(document.getDocumentUuid())
                        .withDocumentNo(String.valueOf(cfe.getCFENro()))
                        .withSerialNo(cfe.getCFESerie())
                        .withError(String.valueOf(cfe.getCFEStatus()).equals("3"))
                        .withErrorMessage(extraInfo + " - " + note)
                        .withDounloadUrl(cfe.getCFERepImpressa())
                );
            } catch (Exception e) {
                log.set_ValueOfColumn("Note", e.getLocalizedMessage());
                response.set(SenderResponse.newInstance()
                        .withDocumentId(documentId)
                        .withDocumentUuid(document.getDocumentUuid())
                        .withError(true)
                        .withErrorMessage(e.getLocalizedMessage())
                );
            }
            log.saveEx();
        });
        return response.get();
    }

    private String getLastFiscalDocumentNo(int documentId, String columnName) {

        PO lastLog = new Query(Env.getCtx(), LUYChanges.SP013_LUY_Log, columnName +" = ?", null)
                .setParameters(documentId)
                .setOrderBy("Updated DESC")
                .first();
        if(lastLog != null && lastLog.get_ID() > 0) {
            String serialNo = lastLog.get_ValueAsString("SP013_SerialNo");
            String documentNo = lastLog.get_ValueAsString("SP013_Serial");
            StringBuilder completeDocumentNo = new StringBuilder();
            if(!Util.isEmpty(serialNo)) {
                completeDocumentNo.append(serialNo);
            }
            completeDocumentNo.append(documentNo);
            return completeDocumentNo.toString();
        }
        return null;
    }

    @Override
    public String testConnection() {
        sendFiscalDocument(FiscalDocumentBuilder.fromInvoice(new MInvoice(Env.getCtx(), 1289317, null), registrationId));
        return "Ok";
    }

    @Override
    public void setAppRegistrationId(int registrationId) {
        this.registrationId = registrationId;
        MADAppRegistration registration = MADAppRegistration.getById(Env.getCtx(), getAppRegistrationId(), null);
        int port = registration.getPort();
        String host = registration.getHost();
        if(port > 0 && port != 80) {
            host = host + ":" + port;
        }
        pushInvoiceUrl = registration.getParameterValue(INVOICY_PUSH_INVOICE);
        if(Util.isEmpty(pushInvoiceUrl, true)) {
            throw new AdempiereException("@Host@ " + pushInvoiceUrl + " @NotFound@");
        }
        pushInvoiceUrl = host + pushInvoiceUrl;

        getInvoiceUrl = registration.getParameterValue(INVOICY_GET_RECEIPT);
        if (Util.isEmpty(getInvoiceUrl)){
            throw new AdempiereException("@Host@ " + getInvoiceUrl + " @NotFound@");
        }
        getInvoiceUrl = host + getInvoiceUrl;

        downloadInvoiceUrl = registration.getParameterValue(INVOICY_DOWNLOAD_RECEIPT);
        if (Util.isEmpty(downloadInvoiceUrl)){
            throw new AdempiereException("@Host@ " + downloadInvoiceUrl + " @NotFound@");
        }
        downloadInvoiceUrl = host + downloadInvoiceUrl;

        clientCode = registration.getParameterValue(CODE);
        clientPK = registration.getParameterValue(INVOICY_PK);
        clientCK = registration.getParameterValue(INVOICY_CK);
    }

    @Override
    public int getAppRegistrationId() {
        return registrationId;
    }


    int linesOk;
    int linesErr;
    List<String> processLogs;
    private HashMap<Tuple3, X_I_Invoice> currentInvoices;
    int orgId;
    @Override
    public IGetInvoicesResult getElectronicInvoices(int orgId, Timestamp dateFrom, Timestamp dateTo, int periodId, String transactionName) {

        IGetInvoicesResult response = null;
        if (periodId <= 0) {
            if (dateFrom == null || dateTo == null) {
                throw new AdempiereException("@DateTrx@ @IsMandatory@");
            }
        }

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        processLogs = new ArrayList<>();
        currentInvoices = new HashMap<>();

        ConsultaCFERecibidos.Filtros getReceivedInvoicesRequestFilters = new ConsultaCFERecibidos.Filtros();

        SimpleDateFormat dateFormat = DisplayType.getDateFormat();

        if(dateFrom != null || dateTo != null){
            if (dateFrom != null) {
                processLogs.add("@StartDate@ " + dateFormat.format(dateFrom));
                getReceivedInvoicesRequestFilters.setFechaIni(ElectronicInvoicingUtil.Timestamp_to_XmlGregorianCalendar_OnlyDate(dateFrom));
            }
            if (dateTo != null) {
                processLogs.add("@EndDate@ " + dateFormat.format(dateTo));
                getReceivedInvoicesRequestFilters.setFechaFin(ElectronicInvoicingUtil.Timestamp_to_XmlGregorianCalendar_OnlyDate(dateTo));
            }

        } else if (periodId > 0) {
            MPeriod period = new MPeriod(Env.getCtx(), periodId, null);
            processLogs.add("@C_Period_ID@ " + period.getName());
            getReceivedInvoicesRequestFilters.setFechaIni(ElectronicInvoicingUtil.Timestamp_to_XmlGregorianCalendar_OnlyDate(period.getStartDate()));
            getReceivedInvoicesRequestFilters.setFechaFin(ElectronicInvoicingUtil.Timestamp_to_XmlGregorianCalendar_OnlyDate(period.getEndDate()));
        }

        com.solop.sp013.luy.cfe.helper.dto.EncabezadoEnvioType envioTypeHeader = new com.solop.sp013.luy.cfe.helper.dto.EncabezadoEnvioType();
        envioTypeHeader.setEmpCodigo(clientCode);
        envioTypeHeader.setEmpPK(clientPK);
        ConsultaCFERecibidos getReceivedInvoicesRequest = new ConsultaCFERecibidos();
        getReceivedInvoicesRequest.setEncabezado(envioTypeHeader);
        getReceivedInvoicesRequest.setFiltros(getReceivedInvoicesRequestFilters);

        try{
            ConsultaCFERecibidosRetorno getReceivedInvoicesResponse = (ConsultaCFERecibidosRetorno) new WebServiceHandler(
                clientCK, getInvoiceUrl, getReceivedInvoicesRequest, ConsultaCFERecibidos.class,getReceivedInvoicesRequestFilters,
                ConsultaCFERecibidos.Filtros.class, envioTypeHeader::setEmpCK, ConsultaCFERecibidosRetorno.class
            ).call("Gxaction/AWS_CONSULTARECIBIDOS.Execute", "Xmlconsulta");

            Integer msgCod = getReceivedInvoicesResponse.getEncabezado().getMsgCod();
            String msgDsc = getReceivedInvoicesResponse.getEncabezado().getMsgDsc();

            if (msgDsc != null && !msgDsc.trim().isEmpty()) {
                processLogs.add("@Message@");
                processLogs.add(msgCod + "\t" + msgDsc);
            }

            List<ConsultaCFERecibidosRetorno.RespuestaConsultaCFERecibidos.CFE> receivedInvoices = getReceivedInvoicesResponse.getRespuestaConsultaCFERecibidos().getCFE();
            processLogs.add("@qty.received@ " + receivedInvoices.size());
            linesOk = 0;
            linesErr = 0;
            for (ConsultaCFERecibidosRetorno.RespuestaConsultaCFERecibidos.CFE receivedInvoice : receivedInvoices) {

                DescargaCFERecibidos.Filtros downloadFilters = new DescargaCFERecibidos.Filtros();
                DescargaCFERecibidos.Filtros.CFE downloadCFE = new DescargaCFERecibidos.Filtros.CFE();
                DescargaCFERecibidos.Filtros.CFE.CFEItem downloadCFEItem = new DescargaCFERecibidos.Filtros.CFE.CFEItem();
                downloadCFE.getCFEItem().add(downloadCFEItem);
                downloadFilters.setCFE(downloadCFE);

                downloadCFEItem.setEmiRUT(receivedInvoice.getEmiRUT());
                downloadCFEItem.setCFETipo(receivedInvoice.getCFETipo());
                downloadCFEItem.setCFESerie(receivedInvoice.getCFESerie());
                downloadCFEItem.setCFENumero(receivedInvoice.getCFENumero());

                com.solop.sp013.luy.cfe.helper.dto.EncabezadoEnvioType envioTypeDownloadHeader = new com.solop.sp013.luy.cfe.helper.dto.EncabezadoEnvioType();
                envioTypeDownloadHeader.setEmpPK(clientPK);
                envioTypeDownloadHeader.setEmpCodigo(clientCode);

                DescargaCFERecibidos downloadReceivedInvoicesRequest = new DescargaCFERecibidos();
                downloadReceivedInvoicesRequest.setEncabezado(envioTypeDownloadHeader);
                downloadReceivedInvoicesRequest.setFiltros(downloadFilters);

                DescargaCFERecibidosRetorno downloadReceivedInvoicesResponse = (DescargaCFERecibidosRetorno) new WebServiceHandler(
                    clientCK, downloadInvoiceUrl, downloadReceivedInvoicesRequest ,DescargaCFERecibidos.class,
                    downloadFilters, DescargaCFERecibidos.Filtros.class,
                    envioTypeDownloadHeader::setEmpCK, DescargaCFERecibidosRetorno.class
                ).call("Gxaction/AWS_DESCARGARECIBIDOS.Execute", "Xmldescarga");

                List<DescargaCFERecibidosRetorno.RespuestaDescargaCFERecibidos.CFE> downloadedInvoices = downloadReceivedInvoicesResponse.getRespuestaDescargaCFERecibidos().getCFE();
                for (DescargaCFERecibidosRetorno.RespuestaDescargaCFERecibidos.CFE downloadedInvoice : downloadedInvoices) {

                    // Decode base64Xml CFE Tag
                    String base64XmlDecoded = new String(Base64.getDecoder().decode(downloadedInvoice.getCFEXML().getBytes()));
                    base64XmlDecoded = base64XmlDecoded.replaceAll("^<EnvioCFE>", "<EnvioCFE xmlns=\"http://www.invoicy.com.uy/\">");

                    JAXBContext jaxbContextResponseEnvioCFE = JAXBContext.newInstance(EnvioCFE.class);
                    Unmarshaller jaxbUnmarshallerEnvioCFE = jaxbContextResponseEnvioCFE.createUnmarshaller();
                    EnvioCFE responseEnvioCFE = (EnvioCFE) jaxbUnmarshallerEnvioCFE.unmarshal(new StringReader(base64XmlDecoded));

                    mapEnvioCFEToIInvoices(responseEnvioCFE);

                }
            }
            String resultMessage = "@C_Invoice_ID@ " + currentInvoices.size() + " - @NoOfLines@ @Ok@ " + linesOk + " - Err " + linesErr;
            response = GetInvoicesResult.newInstance()
                .withLinesOk(linesOk)
                .withLinesErr(linesErr)
                .withTotalImportedInvoices(currentInvoices.size())
                .withLogs(processLogs)
                .withResultMessage(resultMessage)
            ;
            return response;
        } catch (Exception e) {
            throw new AdempiereException(e.getLocalizedMessage());
        }
    }



    private void mapEnvioCFEToIInvoices(EnvioCFE envioCFE) {

        List<CFEInvoiCyType> items = envioCFE.getCFE().getCFEItem();
        for (CFEInvoiCyType item : items) {

            for (CFEInvoiCyType.Detalle.Item line : item.getDetalle().getItem()) {
                try {
                    Trx.run(trxName -> {

                        // Blocks
                        CFEInvoiCyType.Emisor emisor = item.getEmisor();
                        CFEInvoiCyType.Receptor receptor = item.getReceptor();
                        String rutReceptor = receptor.getRcpDocRecep();
                        // Organization Information
                        MOrgInfo organizationInformation = new Query(Env.getCtx(), I_AD_OrgInfo.Table_Name, I_AD_OrgInfo.COLUMNNAME_DUNS + "=?", trxName)
                                .setParameters(rutReceptor)
                                .first();
                        int orgReceiverId = 0;
                        if (organizationInformation != null) {
                            orgReceiverId = organizationInformation.getAD_Org_ID();
                            if (orgId > 0 && orgReceiverId != orgId) {
                                return;
                            }
                        }
                        CFEInvoiCyType.IdDoc idDoc = item.getIdDoc();
                        String cfeDocumentNo = idDoc.getCFESerie() + idDoc.getCFENro();
                        CFEInvoiCyType.Totales totales = item.getTotales();

                        // Checking If I_Invoice already exists
                        BigInteger cfeType = item.getIdDoc().getCFETipoCFE();
                        String whereClause = " C_DocType.IsSOTrx = 'N' AND EXISTS (SELECT 1 FROM SP013_TransactionType tt WHERE tt.Value = ? AND tt.SP013_TransactionType_ID = C_DocType.SP013_TransactionType_ID)";
                        MDocType documentType = new Query(Env.getCtx(),MDocType.Table_Name, whereClause, trxName)
                                .setParameters(cfeType.toString())
                                .first();

                        String queryBP = I_C_BPartner.COLUMNNAME_TaxID + "=?";
                        MBPartner businessPartner = new Query(Env.getCtx(), I_C_BPartner.Table_Name, queryBP, trxName)
                                .setParameters(emisor.getEmiRut())
                                .first();

                        String cfeMsg = emisor.getEmiRut() + " " + emisor.getEmiRznSoc() + " - " + idDoc.getCFETipoCFE() + " - " + idDoc.getCFESerie() + idDoc.getCFENro();

                        if (documentType == null || documentType.get_ID() <= 0) {
                            throw new AdempiereException("X - " + cfeMsg + " | @C_DocType_ID@ " + cfeType.toString() + " @NotFound@");
                        }
                        int bPartnerId = businessPartner != null ? businessPartner.getC_BPartner_ID() : 0;

                        // Invoice Interface
                        X_I_Invoice iInvoice = checkI_Invoice(cfeMsg, emisor.getEmiRut(), bPartnerId, documentType.get_ID(), cfeDocumentNo, trxName);


                        if (orgReceiverId > 0) {
                            iInvoice.setAD_Org_ID(orgReceiverId);
                        }

                        iInvoice.setDocTypeName(String.valueOf(idDoc.getCFETipoCFE()));
                        iInvoice.setDocumentNo(cfeDocumentNo);
                        iInvoice.setC_DocType_ID(documentType.get_ID());
                        iInvoice.setIsSOTrx(false);
                        String referenceOrderNumber = idDoc.getCFEIdCompra();
                        if (!Util.isEmpty(referenceOrderNumber)) {
                            iInvoice.set_ValueOfColumn("SP013_ReferenceNo", referenceOrderNumber.trim());
                        }

                        if (businessPartner != null) {
                            iInvoice.setBPartnerValue(businessPartner.getValue());
                            iInvoice.setC_BPartner_ID(businessPartner.getC_BPartner_ID());
                            int defaultChargeId = businessPartner.get_ValueAsInt("SP013_DefaultCharge_ID");
                            if (defaultChargeId > 0) {
                                MCharge charge = MCharge.get(Env.getCtx(), defaultChargeId);
                                iInvoice.setChargeName(charge.getName());
                                iInvoice.setC_Charge_ID(charge.get_ID());
                            }
                        }
                        iInvoice.set_ValueOfColumn("TaxID", emisor.getEmiRut());
                        iInvoice.setName(emisor.getEmiRznSoc());
                        iInvoice.setAddress1(emisor.getEmiDomFiscal());
                        iInvoice.setCity(emisor.getEmiCiudad());
                        iInvoice.setRegionName(emisor.getEmiDepartamento());
                        iInvoice.setPhone(emisor.getEmiTelefono());
                        iInvoice.set_ValueOfColumn("SP013_FiscalComment", idDoc.getCFEAdenda());
                        iInvoice.setCountryCode(true);
                        MCountry country = MCountry.get(Env.getCtx(), receptor.getRcpCodPaisRecep());
                        if (country != null) {
                            iInvoice.setC_Country_ID(country.get_ID());
                        }
                        try {
                            Timestamp documentDate = formatDate(idDoc.getCFEFchEmis().toString());
                            iInvoice.setDateInvoiced(documentDate);
                            iInvoice.setDateAcct(documentDate);
                        } catch (Exception ignore) { }
                        iInvoice.setPaymentTermValue(String.valueOf(idDoc.getCFEFmaPago()));
                        MCurrency currency = MCurrency.get(Env.getCtx(), totales.getTotTpoMoneda().value());
                        if (currency != null) {
                            iInvoice.setC_Currency_ID(currency.get_ID());
                        }

                        boolean isTaxIncluded = BigInteger.valueOf(1).compareTo(idDoc.getCFEMntBruto()) == 0;
                        //TODO: Maybe Cache the priceLists
                        MPriceList priceList = getPriceListByTaxIncluded(currency.getISO_Code(), false, isTaxIncluded);
                        if (priceList != null && priceList.get_ID() > 0) {
                            iInvoice.setPriceListName(priceList.getName());
                            iInvoice.setM_PriceList_ID(priceList.get_ID());
                        } else {
                            throw new AdempiereException(cfeMsg + " | @M_PriceList_ID@ @NotFound@ - @C_Currency_ID@: " + currency.getISO_Code() + " - @IsTaxIncluded@: " + isTaxIncluded);
                        }

                        // Product/Charge Section
                        String productCodes = null;
                        try {
                            productCodes = line.getCodItem().getCodItemItem().stream().map(CFEInvoiCyType.Detalle.Item.CodItem.CodItemItem::getIteCodiCod).collect(Collectors.joining(","));
                        } catch (Exception ignore) { }
                        iInvoice.setProductValue(productCodes);
                        iInvoice.setLineDescription(line.getIteNomItem());
                        iInvoice.setQtyOrdered(line.getIteCantidad());
                        iInvoice.setTaxIndicator(String.valueOf(line.getIteIndFact()));

                        BigDecimal priceActualAmt = line.getIteMontoItem().divide(line.getIteCantidad(), priceList.getStandardPrecision(), RoundingMode.HALF_UP);
                        BigDecimal priceListAmt = line.getItePrecioUnitario();
                        if(line.getIteIndFact() == 7){
                            iInvoice.setPriceActual(line.getItePrecioUnitario().negate());
                            iInvoice.set_ValueOfColumn("PriceList", line.getIteMontoItem().negate());
                            priceActualAmt = priceActualAmt.negate();
                            priceListAmt = priceListAmt.negate();
                        }
                        iInvoice.setPriceActual(priceActualAmt);
                        iInvoice.set_ValueOfColumn("PriceList", priceListAmt);

                        // End
                        iInvoice.saveEx();
                        processLogs.add("OK | " + cfeMsg);
                        linesOk++;
                    });
                } catch (AdempiereException e) {
                    processLogs.add(e.getMessage());
                    linesErr++;
                }
            }
        }
    }

    private X_I_Invoice checkI_Invoice(String cfeMsg, String bpValue, int bPartnerId, int documentTypeId, String documentNo, String trxName) {

        Tuple3<String, Integer, String> key = new Tuple3<>(bpValue, documentTypeId, documentNo);
        X_I_Invoice ret = currentInvoices.get(key);

        // If not exist in this execution and already in DB, duplicated, error
        if (ret == null) {
            String query;
            if (bPartnerId > 0) {
                query = MInvoice.COLUMNNAME_C_BPartner_ID + "=? AND " + MInvoice.COLUMNNAME_C_DocTypeTarget_ID +
                        " =? AND " + MInvoice.COLUMNNAME_DocumentNo + "=? " +
                        " AND " + MInvoice.COLUMNNAME_IsSOTrx + " = 'N'";
                MInvoice existingInvoice = new Query(Env.getCtx(), MInvoice.Table_Name, query, trxName)
                        .setParameters(bPartnerId, documentTypeId, documentNo)
                        .first();
                if (existingInvoice != null) {
                    throw new AdempiereException("X - " + cfeMsg + " | @AlreadyExists@");
                }
            }

            query = "TaxID=? AND " + I_I_Invoice.COLUMNNAME_C_DocType_ID + "=? AND " + I_I_Invoice.COLUMNNAME_DocumentNo + "=?";
            ret = new Query(Env.getCtx(), I_I_Invoice.Table_Name, query, trxName)
                    .setParameters(bpValue, documentTypeId, documentNo)
                    .first();

            // Duplicated in DB error
            if (ret != null) {
                throw new AdempiereException("X - " + cfeMsg + " | @AlreadyExists@");
            }
        }

        // Not exist, it's ok, I add to current execution
        ret = new X_I_Invoice(Env.getCtx(), 0, trxName);
        currentInvoices.put(key, ret);

        return ret;
    }

    private MPriceList getPriceListByTaxIncluded(String ISOCurrency, boolean IsSOPriceList, boolean isTaxIncluded){
        int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
        MCurrency currency = MCurrency.get(Env.getCtx(), ISOCurrency);
        // If currency is null, return the default without looking at currency
        if (currency==null) throw new AdempiereException("@C_Currency_ID@ " + ISOCurrency + " @NotFound@");

        int M_Currency_ID = currency.get_ID();

        MPriceList retValue = null;

        //	Get from DB
        final String whereClause = "AD_Client_ID=? AND IsSOPriceList=? AND C_Currency_ID=? AND IsTaxIncluded=?";
        retValue = new Query(Env.getCtx(), MPriceList.Table_Name, whereClause, null)
                .setParameters(AD_Client_ID, IsSOPriceList ? "Y" : "N", M_Currency_ID, isTaxIncluded ? "Y" : "N")
                .setOnlyActiveRecords(true)
                .setOrderBy("M_PriceList_ID")
                .first();

        //	Return value
        return retValue;
    }

    private Timestamp formatDate(String strDate) throws ParseException {
        return new Timestamp(simpleDateFormat.parse(strDate).getTime());
    }
    
}
