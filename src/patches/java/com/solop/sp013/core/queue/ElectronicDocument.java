package com.solop.sp013.core.queue;

import com.solop.sp013.core.documents.FiscalDocumentBuilder;
import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.support.IFiscalDocumentChecker;
import com.solop.sp013.core.support.IFiscalSender;
import com.solop.sp013.core.support.IFiscalSenderResponse;
import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrgInfo;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.model.MADAppRegistration;
import org.spin.queue.model.MADQueue;
import org.spin.queue.util.QueueLoader;
import org.spin.queue.util.QueueManager;
import org.spin.util.support.AppSupportHandler;
import org.spin.util.support.IAppSupport;

import java.io.File;
import java.nio.file.Files;

/**
 * Electronic Document Processor (SEI).
 * Handles both C_Invoice (comprobantes) and M_InOut (remitos / guías). For document types
 * flagged as asynchronous validation ({@code SP013_IsAsyncValidation}) the queue performs a
 * two step flow: the first pass sends the document (generar) and the following passes consult
 * it (consultar) until the tax authority accepts it, using {@code SP013_DownloadURL} as the
 * "accepted" signal (it is only filled once the provider returns the final representation).
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class ElectronicDocument extends QueueManager {
    public static String QueueType_ElectronicDocument = "SEI";
    /**	Logger						*/
    protected CLogger log = CLogger.getCLogger(getClass());
    @Override
    public void add(int queueId) {
        log.fine("Electronic Document Queue Added: " + queueId);
    }

    @Override
    public void process(int queueId) {
        MADQueue queue = new MADQueue(getContext(), queueId, getTransactionName());
        int tableId = queue.getAD_Table_ID();
        //  Ignore without record or for tables other than invoice / shipment
        if(queue.getRecord_ID() <= 0
                || (tableId != I_C_Invoice.Table_ID && tableId != I_M_InOut.Table_ID)) {
            return;
        }
        boolean isInvoice = tableId == I_C_Invoice.Table_ID;
        PO document = isInvoice
                ? new MInvoice(getContext(), queue.getRecord_ID(), getTransactionName())
                : new MInOut(getContext(), queue.getRecord_ID(), getTransactionName());
        int documentTypeId = isInvoice
                ? ((MInvoice) document).getC_DocType_ID()
                : ((MInOut) document).getC_DocType_ID();
        MDocType documentType = MDocType.get(getContext(), documentTypeId);
        //	Validate if it document is allowed for send on electronic invoicing
        if(!documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)) {
            queue.setDescription(Msg.parseTranslation(getContext(), "@SP013.DocumentInvalidForED@"));
            queue.saveEx();
            return;
        }
        //	Validate not voided (unless reverse is allowed)
        String docStatus = document.get_ValueAsString("DocStatus");
        if((MInvoice.STATUS_Reversed.equals(docStatus) || MInvoice.STATUS_Voided.equals(docStatus))
                && !document.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse)) {
            queue.setDescription(Msg.parseTranslation(getContext(), "@SP013.DocumentIsVoided@"));
            queue.saveEx();
            return;
        }
        boolean isSent = document.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsSent);
        boolean isAsync = documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAsyncValidation);
        boolean isAccepted = !Util.isEmpty(document.get_ValueAsString(ElectronicInvoicingChanges.SP013_DownloadURL), true);
        //	Decide the action: send (generar) when not sent yet; check (consultar) when the document
        //	is async, already sent and not yet accepted (download url is the accepted-by-SUNAT signal).
        boolean doSend = !isSent;
        boolean doCheck = isSent && isAsync && !isAccepted;
        if(!doSend && !doCheck) {
            queue.setDescription(Msg.parseTranslation(getContext(), "@SP013.DocumentAlreadySent@"));
            queue.saveEx();
            return;
        }
        //	Get Fiscal Sender
        int fiscalSenderId = MOrgInfo.get(getContext(), document.getAD_Org_ID(), null).get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID);
        if(fiscalSenderId == 0) {
            throw new AdempiereException("@SP013.SenderNotFound@");
        }
        try {
            MADAppRegistration registeredApplication = MADAppRegistration.getById(getContext(), fiscalSenderId, null);
            IAppSupport supportedApplication = AppSupportHandler.getInstance().getAppSupport(registeredApplication);
            if(supportedApplication == null) {
                return;
            }
            IFiscalDocument fiscalDocument = isInvoice
                    ? FiscalDocumentBuilder.fromInvoice((MInvoice) document, fiscalSenderId)
                    : FiscalDocumentBuilder.fromInOut((MInOut) document, fiscalSenderId);
            //	Send (generar) or check (consultar)
            IFiscalSenderResponse result;
            if(doSend) {
                if(!IFiscalSender.class.isAssignableFrom(supportedApplication.getClass())) {
                    return;
                }
                result = ((IFiscalSender) supportedApplication).sendFiscalDocument(fiscalDocument);
            } else {
                if(!IFiscalDocumentChecker.class.isAssignableFrom(supportedApplication.getClass())) {
                    queue.setDescription(Msg.parseTranslation(getContext(), "@SP013.CheckNotSupported@"));
                    queue.saveEx();
                    return;
                }
                result = ((IFiscalDocumentChecker) supportedApplication).checkFiscalDocument(fiscalDocument);
            }
            if(result.isError()) {
                throw new AdempiereException("@Error@ " + result.getErrorMessage());
            }
            String fiscalDocumentNo = null;
            if(!Util.isEmpty(result.getDocumentNo())) {
                StringBuilder documentNo = new StringBuilder();
                if(!Util.isEmpty(result.getSerialNo())) {
                    documentNo.append(result.getSerialNo());
                }
                documentNo.append(result.getDocumentNo());
                fiscalDocumentNo = documentNo.toString();
                document.set_ValueOfColumn("DocumentNo", fiscalDocumentNo);
            }
            if(!Util.isEmpty(result.getDownloadUrl())) {
                document.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_DownloadURL, result.getDownloadUrl());
                if(registeredApplication.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_SaveFilesAfterSend)) {
                    File pdf = ElectronicInvoicingUtil.getPdfFileFromUrl(result.getDownloadUrl());
                    if(pdf != null) {
                        MAttachment attachment = isInvoice ? ((MInvoice) document).createAttachment() : ((MInOut) document).createAttachment();
                        attachment.addEntry((fiscalDocumentNo == null ? document.get_ValueAsString("DocumentNo") : fiscalDocumentNo) + ".pdf", Files.readAllBytes(pdf.toPath()));
                        attachment.saveEx();
                        pdf.deleteOnExit();
                    }
                }
            }
            document.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_FiscalSender_ID, fiscalSenderId);
            document.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsSent, true);
            document.saveEx();
            //	Async and still not accepted by SUNAT (no download url): re-enqueue to consult later
            if(isAsync && Util.isEmpty(result.getDownloadUrl(), true)) {
                QueueLoader.getInstance().getQueueManager(QueueType_ElectronicDocument)
                        .withContext(getContext())
                        .withTransactionName(getTransactionName())
                        .withEntity(document)
                        .addToQueue();
            }
        } catch(Exception e) {
            throw new AdempiereException(e);
        }
    }
}
