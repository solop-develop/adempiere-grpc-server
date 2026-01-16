package com.solop.sp013.core.documents;

import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.spin.model.MADAppRegistration;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ReversalDocument {


    /**	Reversal Document No	*/
    private String documentNo = null;
    /**	Reversal Document Date	*/
    private Timestamp documentDate = null;
    /**	Reversal Fiscal Printer No	*/
    private String fiscalSenderNo = null;
    /**	Reversal Fiscal TransactionType	*/
    private String transactionType = null;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public ReversalDocument withCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
        return this;
    }

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public ReversalDocument withConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
        return this;
    }

    /**	Reversal Fiscal TransactionType	*/
    private String currencyCode = null;
    /**	Reversal Fiscal TransactionType	*/
    private BigDecimal conversionRate = null;

    public ReversalDocument() {

    }

    public ReversalDocument(String documentNo, Timestamp documentDate, String fiscalSenderNo, String transactionType) {
        this.documentNo = documentNo;
        this.documentDate = documentDate;
        this.fiscalSenderNo = fiscalSenderNo;
        this.transactionType = transactionType;
    }

    /**
     * Default constructor
     */
    public ReversalDocument(MInvoice document) {
        if(document == null) {
            throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
        }
        //	Fill it
        convertDocument(document);
    }

    /**
     * Convert document like invoice, credit memo or debit memo
     * @param document
     * @return void
     */
    private void convertDocument(MInvoice document) {
        withReversalDocumentNo(document.getDocumentNo());
        withReversalDocumentDate(TimeUtil.getDayTime(document.getDateInvoiced(), document.getUpdated()));
        MOrgInfo organizationInfo = MOrgInfo.get(document.getCtx(), document.getAD_Org_ID(), document.get_TrxName());
        int currencyId = organizationInfo.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalCurrency_ID);
        if(currencyId <= 0) {
            MOrg organization = MOrg.get(document.getCtx(), document.getAD_Org_ID());
            throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_FiscalCurrency_ID + "@ @NotFound@: @AD_Org_ID@ " + organization.getName());
        }

        BigDecimal conversionRate = MConversionRate.getRate(document.getC_Currency_ID(), currencyId, document.getDateAcct(), document.getC_ConversionType_ID(), document.getAD_Client_ID(), document.getAD_Org_ID());
        if(conversionRate == null || conversionRate.compareTo(Env.ZERO) == 0) {
            throw new AdempiereException(MConversionRate.getErrorMessage(document.getCtx(), "ErrorConvertingDocumentCurrencyToBaseCurrency", document.getC_Currency_ID(), currencyId, document.getC_ConversionType_ID(), document.getDateAcct(), document.get_TrxName()));
        }
        withConversionRate(conversionRate);
        withCurrencyCode(MCurrency.getISO_Code(document.getCtx(), document.getC_Currency_ID()));
        if(document.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID) > 0) {
            withReversalFiscalSenderNo(MADAppRegistration.getById(document.getCtx(), document.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID), document.get_TrxName()).getValue());
        }
        MDocType reversalDocumentType = MDocType.get(document.getCtx(), document.getC_DocTypeTarget_ID());
        if(reversalDocumentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID) > 0) {
            MTable transactionTypeTable = MTable.get(document.getCtx(), ElectronicInvoicingChanges.SP013_TransactionType);
            if(transactionTypeTable != null) {
                PO transactionType = transactionTypeTable.getPO(reversalDocumentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID), document.get_TrxName());
                if(transactionType != null) {
                    withReversalTransactionType(transactionType.get_ValueAsString("Value"));
                } else {
                    throw new AdempiereException("@" + ElectronicInvoicingChanges.ReferenceDocument_ID + "@ " + "(@" + ElectronicInvoicingChanges.SP013_TransactionType_ID + "@) @NotFound@");
                }
            }
        } else  {
            throw new AdempiereException("@" + ElectronicInvoicingChanges.ReferenceDocument_ID + "@ " + "(@" + ElectronicInvoicingChanges.SP013_TransactionType_ID + "@) @NotFound@");
        }
    }



    /**
     * @param reversalDocumentNo the reversalDocumentNo to set
     */
    public final ReversalDocument withReversalDocumentNo(String reversalDocumentNo) {
        this.documentNo = reversalDocumentNo;
        return this;
    }

    /**
     * @param reversalDocumentDate the reversalDocumentDate to set
     */
    public final ReversalDocument withReversalDocumentDate(Timestamp reversalDocumentDate) {
        this.documentDate = reversalDocumentDate;
        return this;
    }

    /**
     * @param reversalFiscalSenderNo the reversalFiscalPrinterNo to set
     */
    public final ReversalDocument withReversalFiscalSenderNo(String reversalFiscalSenderNo) {
        this.fiscalSenderNo = reversalFiscalSenderNo;
        return this;
    }

    public ReversalDocument withReversalTransactionType(String reversalTransactionType) {
        this.transactionType = reversalTransactionType;
        return this;
    }

    /**
     * @return the reversalDocumentNo
     */
    public final String getDocumentNo() {
        return documentNo;
    }

    /**
     * @return the reversalDocumentDate
     */
    public final Timestamp getDocumentDate() {
        return documentDate;
    }

    /**
     * @return the reversalFiscalPrinterNo
     */
    public final String getFiscalSenderNo() {
        return fiscalSenderNo;
    }

    public String getTransactionType() {
        return transactionType;
    }
}
