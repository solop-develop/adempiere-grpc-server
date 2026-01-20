/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2020 E.R.P. Consultores y Asociados.                    *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.sp013.core.documents;

import com.solop.sp013.core.model.X_SP013_ElectronicLineSummary;
import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_TaxGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MCity;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCountry;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPOS;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentTerm;
import org.compiere.model.MPriceList;
import org.compiere.model.MRegion;
import org.compiere.model.MSalesRegion;
import org.compiere.model.MTable;
import org.compiere.model.MUOM;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;
import org.spin.model.MADAppRegistration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of IFiscalDocument for Invoice sources
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class InvoiceFiscalDocument implements IFiscalDocument {

    private MInvoice invoice;
    private int fiscalSenderId;
    private MCurrency organizationCurrency;
    private BigDecimal conversionRate;
    
    private String documentType;
    private String transactionType;
    private String documentUuid;
    private String documentNo;
    private String fiscalDocumentNo;
    private Timestamp documentDate;
    private String businessPartnerName;
    private String businessPartnerLastName;
    private String businessPartnerValue;
    private String businessPartnerTaxId;
    private String businessPartnerTaxType;
    private String businessPartnerDuns;
    private String address1;
    private String address2;
    private String address3;
    private String address4;
    private String cityName;
    private String regionName;
    private String countryName;
    private String countryCode;
    private String postalCode;
    private String phone;
    private String eMail;
    private String description;
    private String documentNote;
    private String fiscalComment;
    private String poReferenceNo;
    private String soReferenceNo;
    private String salesRepresentativeValue;
    private String salesRepresentativeName;
    private String paymentTerm;
    private String salesRegionValue;
    private String salesRegionName;
    private BigDecimal totalLines;
    private BigDecimal grandTotal;
    private String amountInWords;
    private String documentTypeName;
    private String deliveryAddress;
    private String deliveryPhone;
    private String warehouseName;
    private int productQuantities;
    private String posName;
    private boolean discountPrinted;
    private boolean taxIncluded;
    private int documentTypeId;
    private int daysDue;
    private Timestamp dueDate;
    private int organizationId;
    private String organizationTaxId;
    private String organizationName;
    private String organizationPhone;
    private String organizationEmail;
    private String organizationFiscalCode;
    private String organizationAddress1;
    private String organizationAddress2;
    private String organizationAddress3;
    private String organizationAddress4;
    private String organizationCityName;
    private String organizationRegionName;
    private String organizationCountryName;
    private String currencyCode;
    private int documentFormat;
    private boolean isManualDocument;

    private List<IFiscalDocumentLine> fiscalDocumentLines;
    private List<IFiscalDocumentTax> fiscalDocumentTaxes;
    private List<IFiscalDocumentPayment> fiscalDocumentPayments;
    private List<ReversalDocument> fiscalReversalDocuments;

    public InvoiceFiscalDocument(MInvoice document, int fiscalSenderId) {
        if(document == null) {
            throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
        }
        this.fiscalSenderId = fiscalSenderId;
        this.invoice = document;
        convertDocument(document);
    }

    private void convertDocument(MInvoice document) {
        fiscalDocumentLines = new ArrayList<>();
        fiscalDocumentTaxes = new ArrayList<>();
        fiscalDocumentPayments = new ArrayList<>();
        fiscalReversalDocuments = new ArrayList<>();
        
        loadOrganizationData(document);
        
        MDocType documentType = MDocType.get(document.getCtx(), document.getC_DocTypeTarget_ID());
        if(Util.isEmpty(documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType))) {
            throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_FiscalDocumentType + "@ @NotFound@");
        }
        if(documentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID) > 0) {
            MTable transactionTypeTable = MTable.get(document.getCtx(), ElectronicInvoicingChanges.SP013_TransactionType);
            if(transactionTypeTable != null) {
                PO transactionType = transactionTypeTable.getPO(documentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID), document.get_TrxName());
                if(transactionType != null) {
                    this.transactionType = transactionType.get_ValueAsString("Value");
                }
            }
        }
        if(documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType)
            .equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note)
            || documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType)
            .equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note)) {
            int invoiceToAllocateId = document.get_ValueAsInt(ElectronicInvoicingChanges.ReferenceDocument_ID);
            if(invoiceToAllocateId > 0) {
                MInvoice allocatedDocument = new MInvoice(document.getCtx(), invoiceToAllocateId, document.get_TrxName());
                addReversalDocument(allocatedDocument);
            } else {
                List<Integer> allocateInvoiceIds = new Query(document.getCtx(), I_C_Invoice.Table_Name, "EXISTS(SELECT 1 FROM C_AllocateInvoice ai " +
                        "WHERE ai.ReferenceDocument_ID = C_Invoice.C_Invoice_ID " +
                        "AND ai.C_Invoice_ID = ?)", document.get_TrxName())
                        .setParameters(document.getC_Invoice_ID())
                        .getIDsAsList();
                allocateInvoiceIds.forEach(allocatedInvoiceId -> {
                    MInvoice allocatedDocument = new MInvoice(document.getCtx(), allocatedInvoiceId, document.get_TrxName());
                    addReversalDocument(allocatedDocument);
                });
            }
        } else if(documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType)
                .equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Withholding)
                && document.getReversal_ID() > 0) {
            Timestamp documentDate = TimeUtil.getDayTime(document.getDateInvoiced(), document.getUpdated());
            String senderNo = null;
            String documentNo = document.getDocumentNo().replaceAll("^", "");
            if(document.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID) > 0) {
                senderNo = MADAppRegistration.getById(document.getCtx(), document.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID), document.get_TrxName()).getValue();
            }
            addReversalDocumentInfo(documentNo, documentDate, senderNo, getTransactionType());
        }

        String whereClause = X_SP013_ElectronicLineSummary.COLUMNNAME_C_Invoice_ID + " = ?";
        List<Integer> electronicLineSummaryIds = new Query(document.getCtx(), X_SP013_ElectronicLineSummary.Table_Name, whereClause, document.get_TrxName())
                .setParameters(document.get_ID())
                .getIDsAsList();
        if (!electronicLineSummaryIds.isEmpty()) {
            BigDecimal summaryTotal = new Query(document.getCtx(), X_SP013_ElectronicLineSummary.Table_Name, whereClause, document.get_TrxName())
                    .setParameters(document.get_ID())
                    .sum(X_SP013_ElectronicLineSummary.COLUMNNAME_LineTotalAmt);
            if (document.getTotalLines().compareTo(summaryTotal) != 0) {
                throw new AdempiereException("@SP013.SummaryTotalAmt_Invalid@: @TotalAmt@: " + summaryTotal.toString() + " <> @TotalLines@:  " + document.getTotalLines().toString());
            }
        }

        this.documentType = documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType);
        


        MCurrency currency = MCurrency.get(document.getCtx(), document.getC_Currency_ID());
        this.currencyCode = currency.getISO_Code();
        
        MBPartner businessPartner = (MBPartner) document.getC_BPartner();
        MBPartnerLocation businessPartnerLocation = (MBPartnerLocation) document.getC_BPartner_Location();
        MLocation location = (MLocation) businessPartnerLocation.getC_Location();
        MCountry country = MCountry.get(document.getCtx(), location.getC_Country_ID());
        MRegion region = MRegion.get(document.getCtx(), location.getC_Region_ID());
        MPriceList priceList = MPriceList.get(document.getCtx(), document.getM_PriceList_ID(), null);
        
        I_C_TaxGroup businessPartnerTaxGroup = businessPartner.getC_TaxGroup();
        if(businessPartnerTaxGroup != null) {
            this.businessPartnerTaxType = businessPartnerTaxGroup.getValue();
        }
        
        String cityName = location.getCity();
        if(Util.isEmpty(cityName) && location.getC_City_ID() != 0) {
            MCity city = MCity.get(document.getCtx(), location.getC_City_ID());
            cityName = city.getName();
        }
        
        String sOReference = null;
        if(document.getC_Order_ID() != 0) {
            sOReference = document.getC_Order().getDocumentNo();
        }
        int documentFormat = -1;
        String documentFormatString = null;
        MADAppRegistration fiscalSender = MADAppRegistration.getById(document.getCtx(), fiscalSenderId, document.get_TrxName());
        if (fiscalSender !=null && fiscalSender.get_ID() > 0) {
            documentFormatString = fiscalSender.get_ValueAsString(ElectronicInvoicingChanges.SP013_DocumentFormat);
        }
        //	For POS
        if(document.getC_POS_ID() > 0) {
            MPOS pos = MPOS.get(document.getCtx(), document.getC_POS_ID());
            if (!Util.isEmpty(pos.get_ValueAsString(ElectronicInvoicingChanges.SP013_DocumentFormat), true)) {
                documentFormatString = pos.get_ValueAsString(ElectronicInvoicingChanges.SP013_DocumentFormat);
            }
            /*//TODO: Validate if useful
            if(pos != null) {
                withPosName(pos.getName());
            }*/
        }
        if (!Util.isEmpty(documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_DocumentFormat), true)) {
            documentFormatString = documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_DocumentFormat);
        }
        if (!Util.isEmpty(documentFormatString, true)) {
            documentFormat = Integer.parseInt(documentFormatString);
        }
        this.documentFormat = documentFormat;
        this.documentUuid = document.getUUID();
        this.documentNo = document.getDocumentNo();
        this.documentDate = document.getDateInvoiced();
        this.businessPartnerName = businessPartner.getName();
        this.businessPartnerLastName = businessPartner.getName2();
        this.businessPartnerTaxId = businessPartner.getTaxID();
        this.businessPartnerValue = businessPartner.getValue();
        this.businessPartnerDuns = businessPartner.getDUNS();
        this.address1 = location.getAddress1();
        this.address2 = location.getAddress2();
        this.address3 = location.getAddress3();
        this.address4 = location.getAddress4();
        this.cityName = cityName;
        this.regionName = region.getName();
        this.countryName = country.getName();
        this.countryCode = country.getCountryCode();
        this.description = document.getDescription();
        this.documentNote = documentType.getDocumentNote();
        this.poReferenceNo = document.getPOReference();
        this.soReferenceNo = sOReference;
        this.documentTypeName = documentType.getPrintName();
        this.totalLines = document.getTotalLines();
        this.grandTotal = document.getGrandTotal();
        this.discountPrinted = document.isDiscountPrinted();
        this.documentTypeId = document.getC_DocTypeTarget_ID();
        this.taxIncluded = priceList.isTaxIncluded();
        this.dueDate = document.getDateInvoiced();
        this.phone = businessPartnerLocation.getPhone();
        this.eMail = businessPartnerLocation.getEMail();
        this.postalCode = location.getPostal();
        this.isManualDocument = document.isManualDocument();
        this.fiscalComment = document.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalComment);

        if(document.getSalesRep_ID() != 0) {
            MUser salesRepresentative = MUser.get(document.getCtx(), document.getSalesRep_ID());
            this.salesRepresentativeValue = salesRepresentative.getValue();
            this.salesRepresentativeName = salesRepresentative.getName();
        }
        
        if(document.getC_SalesRegion_ID() != 0) {
            MSalesRegion salesRegion = MSalesRegion.getById(document.getCtx(), document.getC_SalesRegion_ID(), document.get_TrxName());
            this.salesRegionValue = salesRegion.getValue();
            this.salesRegionName = salesRegion.getName();
        }
        
        if(document.getC_PaymentTerm_ID() != 0) {
            MPaymentTerm paymentTerm = (MPaymentTerm) document.getC_PaymentTerm();
            this.paymentTerm = paymentTerm.getName();
            this.daysDue = paymentTerm.getNetDays();
            this.dueDate = TimeUtil.addDays(document.getDateInvoiced(), paymentTerm.getNetDays());
        }

        String amountInWords = Msg.getAmtInWords(Env.getLanguage(document.getCtx()), DisplayType.getNumberFormat(DisplayType.Amount).format(document.getGrandTotal()));
        if(!Util.isEmpty(amountInWords)) {
            this.amountInWords = amountInWords;
        }

        AtomicInteger productQuantities = new AtomicInteger(0);

        if (!electronicLineSummaryIds.isEmpty()) {
            electronicLineSummaryIds.forEach( summaryLineId -> {
                X_SP013_ElectronicLineSummary summaryLine = new X_SP013_ElectronicLineSummary(document.getCtx(), summaryLineId, document.get_TrxName());
                fiscalDocumentLines.add(new InvoiceFiscalDocumentLine(summaryLine));
                MUOM uom = MUOM.get(document.getCtx(), summaryLine.getC_UOM_ID());
                if(uom.get_ColumnIndex(ElectronicInvoicingChanges.SP013_EachUnitCount) >= 0 && uom.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_EachUnitCount)) {
                    productQuantities.getAndAdd(Optional.ofNullable(summaryLine.getQty()).orElse(Env.ZERO).intValue());
                } else {
                    productQuantities.getAndIncrement();
                }
            });
        } else {
            Arrays.asList(document.getLines()).forEach(documentLine -> {
                fiscalDocumentLines.add(new InvoiceFiscalDocumentLine(documentLine));
                MUOM uom = MUOM.get(document.getCtx(), documentLine.getC_UOM_ID());
                if(uom.get_ColumnIndex(ElectronicInvoicingChanges.SP013_EachUnitCount) >= 0 && uom.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_EachUnitCount)) {
                    productQuantities.getAndAdd(Optional.ofNullable(documentLine.getQtyInvoiced()).orElse(Env.ZERO).intValue());
                } else {
                    productQuantities.getAndIncrement();
                }
            });
        }

        this.productQuantities = productQuantities.get();
        
        Arrays.asList(document.getTaxes(true)).forEach(documentTax -> fiscalDocumentTaxes.add(new InvoiceTaxFiscalDocumentTax(documentTax)));
        
        MOrder order = (MOrder) document.getC_Order();
        MPayment.getOfOrder(order).forEach(payment -> fiscalDocumentPayments.add(new PaymentFiscalDocumentPayment(payment)));
    }

    private void loadOrganizationData(MInvoice document) {
        MOrg organization = MOrg.get(document.getCtx(), document.getAD_Org_ID());
        MOrgInfo organizationInfo = MOrgInfo.get(document.getCtx(), document.getAD_Org_ID(), document.get_TrxName());
        int currencyId = organizationInfo.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalCurrency_ID);
        if(currencyId <= 0) {
            throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_FiscalCurrency_ID + "@ @NotFound@: @AD_Org_ID@ " + organization.getName());
        }
        organizationCurrency = MCurrency.get(document.getCtx(), currencyId);
        this.organizationId = document.getAD_Org_ID();
        this.organizationTaxId = organizationInfo.getTaxID();
        this.organizationName = organization.getName();
        this.organizationPhone = organizationInfo.getPhone();
        this.organizationEmail = organizationInfo.getEMail();
        this.organizationFiscalCode = organizationInfo.get_ValueAsString(ElectronicInvoicingChanges.SP013_OrgFiscalCode);
        
        MLocation location = (MLocation) organizationInfo.getC_Location();
        if(location != null) {
            MCountry country = MCountry.get(document.getCtx(), location.getC_Country_ID());
            MRegion region = MRegion.get(document.getCtx(), location.getC_Region_ID());
            String cityName = location.getCity();
            if(Util.isEmpty(cityName) && location.getC_City_ID() != 0) {
                MCity city = MCity.get(document.getCtx(), location.getC_City_ID());
                cityName = city.getName();
            }
            this.organizationCountryName = country.getName();
            this.organizationCityName = cityName;
            this.organizationRegionName = region.getName();
            this.organizationAddress1 = location.getAddress1();
            this.organizationAddress2 = location.getAddress2();
            this.organizationAddress3 = location.getAddress3();
            this.organizationAddress4 = location.getAddress4();
        }
    }

    @Override
    public BigDecimal getCurrencyRate() {
        if(Optional.ofNullable(conversionRate).orElse(Env.ZERO).compareTo(Env.ZERO) <= 0) {
            conversionRate = MConversionRate.getRate(invoice.getC_Currency_ID(), organizationCurrency.getC_Currency_ID(), invoice.getDateAcct(), invoice.getC_ConversionType_ID(), invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
            if(conversionRate == null || conversionRate.compareTo(Env.ZERO) == 0) {
                throw new AdempiereException(MConversionRate.getErrorMessage(invoice.getCtx(), "ErrorConvertingDocumentCurrencyToBaseCurrency", invoice.getC_Currency_ID(), organizationCurrency.getC_Currency_ID(), invoice.getC_ConversionType_ID(), invoice.getDateAcct(), invoice.get_TrxName()));
            }
        }
        return conversionRate;
    }

    @Override
    public BigDecimal getConvertedAmount(BigDecimal amount) {
        if(invoice.getC_Currency_ID() == organizationCurrency.getC_Currency_ID()
                || amount == null
                || amount.compareTo(Env.ZERO) == 0) {
            return amount;
        }
        getCurrencyRate();
        return amount.multiply(conversionRate).setScale(organizationCurrency.getStdPrecision(), RoundingMode.HALF_UP);
    }

    @Override
    public boolean hasReversalDocument() {
        return !fiscalReversalDocuments.isEmpty();
    }

    @Override
    public ReversalDocument getFirstReversalDocument() {
        return fiscalReversalDocuments.stream().findFirst().orElse(new ReversalDocument());
    }
    /**
     * @param reversalDocument the reversalDocument to set
     */
    public final InvoiceFiscalDocument addReversalDocument(MInvoice reversalDocument) {
        fiscalReversalDocuments.add(new ReversalDocument(reversalDocument));
        return this;
    }

    public final InvoiceFiscalDocument addReversalDocumentInfo(String documentNo, Timestamp documentDate, String fiscalSenderNo, String transactionType) {
        fiscalReversalDocuments.add(new ReversalDocument(documentNo, documentDate, fiscalSenderNo, transactionType));
        return this;
    }


    @Override
    public int getInvoiceId() {
        if(invoice != null) {
            return invoice.getC_Invoice_ID();
        }
        return 0;
    }

    @Override
    public int getInOutId() {
        return 0;
    }

    @Override
    public PaymentRule getPaymentRule() {
        return daysDue > 0 ? PaymentRule.CREDIT : PaymentRule.IMMEDIATE;
    }

    // Getters implementation
    @Override public String getDocumentType() { return documentType; }
    @Override public String getTransactionType() { return transactionType; }
    @Override public String getDocumentUuid() { return documentUuid; }
    @Override public String getDocumentNo() { return documentNo; }
    @Override public String getFiscalDocumentNo() { return fiscalDocumentNo; }
    @Override public Timestamp getDocumentDate() { return documentDate; }
    @Override public String getBusinessPartnerName() { return businessPartnerName; }
    @Override public String getBusinessPartnerLastName() { return businessPartnerLastName; }
    @Override public String getBusinessPartnerValue() { return businessPartnerValue; }
    @Override public String getBusinessPartnerTaxId() { return businessPartnerTaxId; }
    @Override public String getBusinessPartnerTaxType() { return businessPartnerTaxType; }
    @Override public String getBusinessPartnerDuns() { return businessPartnerDuns; }
    @Override public String getAddress1() { return address1; }
    @Override public String getAddress2() { return address2; }
    @Override public String getAddress3() { return address3; }
    @Override public String getAddress4() { return address4; }
    @Override public String getCityName() { return cityName; }
    @Override public String getRegionName() { return regionName; }
    @Override public String getCountryName() { return countryName; }
    @Override public String getCountryCode() { return countryCode; }
    @Override public String getPostalCode() { return postalCode; }
    @Override public String getPhone() { return phone; }
    @Override public String getEMail() { return eMail; }
    @Override public String getDescription() { return description; }
    @Override public String getDocumentNote() { return documentNote; }
    @Override public String getFiscalComment() { return fiscalComment; }
    @Override public String getPoReferenceNo() { return poReferenceNo; }
    @Override public String getSoReferenceNo() { return soReferenceNo; }
    @Override public String getSalesRepresentativeValue() { return salesRepresentativeValue; }
    @Override public String getSalesRepresentativeName() { return salesRepresentativeName; }
    @Override public String getPaymentTerm() { return paymentTerm; }
    @Override public String getSalesRegionValue() { return salesRegionValue; }
    @Override public String getSalesRegionName() { return salesRegionName; }
    @Override public BigDecimal getTotalLines() { return totalLines; }
    @Override public BigDecimal getGrandTotal() { return grandTotal; }
    @Override public String getAmountInWords() { return amountInWords; }
    @Override public String getDocumentTypeName() { return documentTypeName; }
    @Override public String getDeliveryAddress() { return deliveryAddress; }
    @Override public String getDeliveryPhone() { return deliveryPhone; }
    @Override public String getWarehouseName() { return warehouseName; }
    @Override public int getProductQuantities() { return productQuantities; }
    @Override public String getPosName() { return posName; }
    @Override public boolean isDiscountPrinted() { return discountPrinted; }
    @Override public boolean isTaxIncluded() { return taxIncluded; }
    @Override public int getDocumentTypeId() { return documentTypeId; }
    @Override public int getDaysDue() { return daysDue; }
    @Override public Timestamp getDueDate() { return dueDate; }
    @Override public int getOrganizationId() { return organizationId; }
    @Override public String getOrganizationTaxId() { return organizationTaxId; }
    @Override public String getOrganizationName() { return organizationName; }
    @Override public String getOrganizationPhone() { return organizationPhone; }
    @Override public String getOrganizationEmail() { return organizationEmail; }
    @Override public String getOrganizationFiscalCode() { return organizationFiscalCode; }
    @Override public String getOrganizationAddress1() { return organizationAddress1; }
    @Override public String getOrganizationAddress2() { return organizationAddress2; }
    @Override public String getOrganizationAddress3() { return organizationAddress3; }
    @Override public String getOrganizationAddress4() { return organizationAddress4; }
    @Override public String getOrganizationCityName() { return organizationCityName; }
    @Override public String getOrganizationRegionName() { return organizationRegionName; }
    @Override public String getOrganizationCountryName() { return organizationCountryName; }
    @Override public String getCurrencyCode() { return currencyCode; }
    @Override public int getDocumentFormat() { return documentFormat; }
    @Override public int getFiscalSenderId() { return fiscalSenderId; }
    @Override public List<IFiscalDocumentLine> getFiscalDocumentLines() { return fiscalDocumentLines; }
    @Override public List<IFiscalDocumentTax> getFiscalDocumentTaxes() { return fiscalDocumentTaxes; }
    @Override public List<IFiscalDocumentPayment> getFiscalDocumentPayments() { return fiscalDocumentPayments; }
    @Override public List<ReversalDocument> getFiscalReversalDocuments() { return fiscalReversalDocuments; }
    @Override public boolean isManualDocument() {return isManualDocument; }
    @Override
    public IFiscalDocument withFiscalDocumentNo(String fiscalDocumentNo) {
        this.fiscalDocumentNo = fiscalDocumentNo;
        return this;
    }


}