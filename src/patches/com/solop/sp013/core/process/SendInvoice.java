package com.solop.sp013.core.process;

import com.solop.sp013.core.documents.FiscalDocumentBuilder;
import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.support.IFiscalSender;
import com.solop.sp013.core.support.IFiscalSenderResponse;
import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrgInfo;
import org.compiere.util.Util;
import org.spin.model.MADAppRegistration;
import org.spin.util.support.AppSupportHandler;
import org.spin.util.support.IAppSupport;

import java.io.File;
import java.nio.file.Files;

/**
 * 	Send Invoice to Fiscal Provider
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class SendInvoice extends SendInvoiceAbstract {

	@Override
	protected String doIt() throws Exception {
		if(getRecord_ID() > 0 && getInvoiceId() == 0) {
			setInvoiceId(getRecord_ID());
		}
		if(getInvoiceId() == 0) {
			throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
		}
		//
		MInvoice invoice = new MInvoice(getCtx(), getInvoiceId(), get_TrxName());
		//	Validate if document is allowed for print on fiscal print
		MDocType documentType = MDocType.get(getCtx(), invoice.getC_DocTypeTarget_ID());
		if(!documentType.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsElectronicDocument)) {
			throw new AdempiereException("@SP013.DocumentInvalidForED@");
		}
		//	Validate Printing
		if(invoice.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsSent)) {
			throw new AdempiereException("@SP013.DocumentAlreadySent@");
		}
		//	Validate only completed
		if((invoice.getDocStatus().equals(MInvoice.STATUS_Reversed)
				|| invoice.getDocStatus().equals(MInvoice.STATUS_Voided)) && !invoice.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_IsAllowsReverse)) {
			throw new AdempiereException("@SP013.DocumentIsVoided@");
		}
		//	Get Fiscal Printer
		int fiscalSenderId = MOrgInfo.get(getCtx(), invoice.getAD_Org_ID(), null).get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID);
		if(fiscalSenderId == 0) {
			throw new AdempiereException("@SP013.SenderNotFound@");
		}
		MADAppRegistration registeredApplication = MADAppRegistration.getById(getCtx(), fiscalSenderId, null);
		IAppSupport supportedApplication = AppSupportHandler.getInstance().getAppSupport(registeredApplication);
		//	Exists a Application available for it?
		if(supportedApplication != null
				&& IFiscalSender.class.isAssignableFrom(supportedApplication.getClass())) {
			//	Instance of fiscal printer
			IFiscalSender fiscalPrinter = (IFiscalSender) supportedApplication;
			IFiscalDocument fiscalDocument = FiscalDocumentBuilder.fromInvoice(invoice, fiscalSenderId);
			//	Send document
			IFiscalSenderResponse result = fiscalPrinter.sendFiscalDocument(fiscalDocument);
			if(!result.isError()) {
				if(!Util.isEmpty(result.getDocumentNo())) {
					StringBuilder documentNo = new StringBuilder();
					if(!Util.isEmpty(result.getSerialNo())) {
						documentNo.append(result.getSerialNo());
					}
					documentNo.append(result.getDocumentNo());
					invoice.setDocumentNo(documentNo.toString());
					if(!Util.isEmpty(result.getDownloadUrl())) {
						invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_DownloadURL, result.getDownloadUrl());
						if(registeredApplication.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_SaveFilesAfterSend)) {
							File pdf = ElectronicInvoicingUtil.getPdfFileFromUrl(result.getDownloadUrl());
							if(pdf != null) {
								MAttachment attachment = invoice.createAttachment();
								attachment.addEntry(documentNo + ".pdf", Files.readAllBytes(pdf.toPath()));
								attachment.saveEx();
								pdf.deleteOnExit();
							}
						}
					}
					invoice.saveEx();
				}
			} else {
				throw new AdempiereException("@Error@ " + result.getErrorMessage());
			}
		}
		invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_FiscalSender_ID, fiscalSenderId);
		invoice.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_IsSent, true);
		invoice.saveEx();
		//	Ok
		return "Ok";
	}
}