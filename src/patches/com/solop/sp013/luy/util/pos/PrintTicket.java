package com.solop.sp013.luy.util.pos;

import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_C_POS;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.pdf.IText7Document;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPOS;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.print.ReportEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.services.dsl.ProcessBuilder;
import org.solop.process.PrintCollectionReceipts;
import org.spin.pos.util.IPrintTicket;
import org.spin.pos.util.TicketHandler;
import org.spin.pos.util.TicketResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 	POS Print Ticket reference to fiscal document
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class PrintTicket implements IPrintTicket {
    private CLogger log = CLogger.getCLogger (getClass());
    private TicketHandler handler;

    @Override
    public void setHandler(TicketHandler handler) {
        this.handler = handler;
    }

    @Override
    public TicketResult printTicket() {
        TicketResult ticketResult = TicketResult.newInstance();
        int invoiceId = 0;

        if(handler != null && handler.getRecordId() > 0) {
            if(handler.getTableName().equals(I_C_Order.Table_Name)) {
                invoiceId = new Query(
					Env.getCtx(),
					I_C_Invoice.Table_Name,
					I_C_Invoice.COLUMNNAME_C_Order_ID + " = ?",
					handler.getTransactionName()
				)
					.setParameters(handler.getRecordId())
					.setOrderBy(I_C_Invoice.COLUMNNAME_Created + " DESC")
					.firstId()
				;
            } else if(handler.getTableName().equals(I_C_Invoice.Table_Name)) {
                invoiceId = handler.getRecordId();
            }
        }

        if (invoiceId > 0) {
			List<File> pdfList = new ArrayList<>();
			File invoiceFile = null;
            MInvoice invoice = new MInvoice(Env.getCtx(), invoiceId, handler.getTransactionName());

			//  Get URL
            String downloadUrl = invoice.get_ValueAsString(ElectronicInvoicingChanges.SP013_DownloadURL);
            if(!Util.isEmpty(downloadUrl)) {
				log.fine("Download Invoice File");
				invoiceFile = ElectronicInvoicingUtil.getPdfFileFromUrl(downloadUrl);
            } else {
				log.fine("Generate Invoice File");
				ReportEngine reportEngine = ReportEngine.get(invoice.getCtx(), ReportEngine.INVOICE, invoice.getC_Invoice_ID(), invoice.get_TrxName());
				reportEngine.createPDF(invoiceFile);
            }
			if (invoiceFile != null) {
				pdfList.add(invoiceFile);
			}
			if (handler.getPosId() > 0) {
				MPOS pos = new MPOS(Env.getCtx(), handler.getPosId(), handler.getTransactionName());
				if (pos.get_ValueAsBoolean("IsPrintCollet")){
					File collectFile = printCollect(invoice.getC_Order_ID(), invoice.get_TrxName());
					if (collectFile != null) {
						pdfList.add(collectFile);
					}
				}
				if (pos.get_ValueAsBoolean("IsPrintGiftCard")) {
					List<File> giftCardsFiles = printGiftCard(invoice.getC_Order_ID(), invoice.get_TrxName());
					if (giftCardsFiles != null) {
						pdfList.addAll(giftCardsFiles);
					}
				}
			}
            File resultPdf = null;
            try {
                resultPdf = File.createTempFile("InvoicePrint", ".pdf");
                IText7Document.mergePdf(pdfList, resultPdf);
            } catch (Exception e) {
                throw new AdempiereException(e.getLocalizedMessage());
            }
			ticketResult
				.withReportFile(resultPdf)
				.withReportFiles(pdfList)
				.withError(false)
				.withSummary("Ok")
			;
        }
        return ticketResult;
    }

    private List<File> printGiftCard(int orderId, String transactionName) {
        List<File> result = new ArrayList<>();
        MTable giftCardTable = MTable.get(Env.getCtx(),"ECA14_GiftCard");

        if (giftCardTable != null && giftCardTable.get_ID() > 0) {
            String whereClause = "C_Order_ID = ?";
            List<Integer> giftCardIds = new Query(
				Env.getCtx(),
				giftCardTable.getTableName(),
				whereClause,
				transactionName
			)
				.setParameters(orderId)
				.getIDsAsList()
			;
            int giftCardTabId = 55269;
            MTab giftCardTab = MTab.get(Env.getCtx(), giftCardTabId);
			if (giftCardTab == null) {
				log.fine("Without Gift Card tab");
				return null;
			}
			final int processId = giftCardTab.getAD_Process_ID();
			if (processId <= 0) {
				log.fine("Without Gift Card process");
				return null;
			}

			// validate access
			MRole role = MRole.getDefault();
			Boolean isRoleAccess = role.getProcessAccess(processId);
			if (isRoleAccess == null || !isRoleAccess.booleanValue()) {
				throw new AdempiereException("@AccessCannotReport@ @ECA14_GiftCard_ID@");
			}

			log.fine("Generate Gift Card Files");
            giftCardIds.forEach(giftCardId ->{
                ProcessBuilder builder = ProcessBuilder.create(Env.getCtx())
					.process(processId)
					.withRecordId(giftCardTab.getAD_Table_ID(), giftCardId)
					.withoutPrintPreview()
					.withoutBatchMode()
					.withWindowNo(0)
					.withoutTransactionClose()
					.withReportExportFormat("pdf")
				;
                ProcessInfo info = builder.execute(transactionName);
				File giftCardFile = info.getPDFReport();
				if (giftCardFile != null) {
					result.add(giftCardFile);
				}
            });
        }
        return result;
    }

    private File printCollect(int orderId, String transactionName){
		final int processId = PrintCollectionReceipts.getProcessId();
		MProcess process = MProcess.get(Env.getCtx(), processId);

		// validate access
		MRole role = MRole.getDefault();
		Boolean isRoleAccess = role.getProcessAccess(processId);
		if (isRoleAccess == null || !isRoleAccess.booleanValue()) {
			throw new AdempiereException("@AccessCannotReport@ " + process.get_Translation(I_AD_Process.COLUMNNAME_Name));
		}

		final String whereClause = "C_PaymentProcessorLog.Value='Voucher' " +
			"AND C_PaymentProcessorLog.C_Payment_ID IN (SELECT p.C_Payment_ID FROM C_Payment AS p WHERE p.C_Order_ID = ?)";
        boolean canPrint = new Query(
			Env.getCtx(),
			"C_PaymentProcessorLog",
			whereClause,
			transactionName
		)
			.setParameters(orderId)
			.match();
		if (!canPrint) {
			return null;
		}

		log.fine("Generate Collet File");
        ProcessBuilder builder = ProcessBuilder.create(Env.getCtx())
			.process(processId)
            .withRecordId(MOrder.Table_ID, orderId)
            .withoutPrintPreview()
            .withoutBatchMode()
            .withWindowNo(0)
            .withoutTransactionClose()
			.withReportExportFormat("pdf")
		;
        ProcessInfo info = builder.execute(transactionName);

        return info.getPDFReport();
    }

}
