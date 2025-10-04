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

import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import org.adempiere.core.domains.models.I_C_TaxGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MCity;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCountry;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPOS;
import org.compiere.model.MRegion;
import org.compiere.model.MTable;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
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
 * Implementation of IFiscalDocument for Material Receipt sources
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class InOutFiscalDocument implements IFiscalDocument {

    private MInOut inOut;
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

    public InOutFiscalDocument(MInOut document, int fiscalSenderId) {
        if(document == null) {
            throw new AdempiereException("@M_InOut_ID@ @NotFound@");
        }
        this.fiscalSenderId = fiscalSenderId;
        this.inOut = document;
        convertDocument(document);
    }

    private void convertDocument(MInOut document) {
        fiscalDocumentLines = new ArrayList<>();
        fiscalDocumentTaxes = new ArrayList<>();
        fiscalDocumentPayments = new ArrayList<>();
        fiscalReversalDocuments = new ArrayList<>();
        
        loadOrganizationData(document);
        
        MDocType documentType = MDocType.get(document.getCtx(), document.getC_DocType_ID());
        if(Util.isEmpty(documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType))) {
            throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_FiscalDocumentType + "@ @NotFound@");
        }

        this.documentType = documentType.get_ValueAsString(ElectronicInvoicingChanges.SP013_FiscalDocumentType);
        
        if(documentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID) > 0) {
            MTable transactionTypeTable = MTable.get(document.getCtx(), ElectronicInvoicingChanges.SP013_TransactionType);
            if(transactionTypeTable != null) {
                PO transactionType = transactionTypeTable.getPO(documentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID), document.get_TrxName());
                if(transactionType != null) {
                    this.transactionType = transactionType.get_ValueAsString("Value");
                }
            }
        }


        MOrder order = null;
        if(document.getC_Order_ID() > 0) {
            order = (MOrder) document.getC_Order();
            MCurrency currency = MCurrency.get(document.getCtx(), order.getC_Currency_ID());
            this.currencyCode = currency.getISO_Code();
        } else {
            this.currencyCode = organizationCurrency.getISO_Code();
        }
        MInOut reverseOriginalDocument = null;
        if (document.getReversal_ID() > 0) {
            reverseOriginalDocument = new MInOut(document.getCtx(), document.getReversal_ID(), document.get_TrxName());
        }
        if (MInOut.MOVEMENTTYPE_CustomerReturns.equals(document.getMovementType())) {
            if (order != null) {
                int sourceOrderId =order.get_ValueAsInt("ECA14_Source_Order_ID");
                String whereClause = "C_Order_ID = ?";
                reverseOriginalDocument = new Query(document.getCtx(), MInOut.Table_Name, whereClause, document.get_TrxName())
                    .setParameters(sourceOrderId)
                    .first();
            }
        }
        if (reverseOriginalDocument != null){
            String senderNo = null;
            String documentNo = reverseOriginalDocument.getDocumentNo().replaceAll("^", "");
            if(reverseOriginalDocument.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID) > 0) {
                senderNo = MADAppRegistration.getById(document.getCtx(), reverseOriginalDocument.get_ValueAsInt(ElectronicInvoicingChanges.SP013_FiscalSender_ID), document.get_TrxName()).getValue();
            }
            String reversalTransactionType = null;
            MDocType reversalDocumentType = MDocType.get(document.getCtx(), document.getC_DocType_ID());
            if(reversalDocumentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID) > 0) {
                MTable transactionTypeTable = MTable.get(document.getCtx(), ElectronicInvoicingChanges.SP013_TransactionType);
                if(transactionTypeTable != null) {
                    PO transactionType = transactionTypeTable.getPO(reversalDocumentType.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TransactionType_ID), document.get_TrxName());
                    if(transactionType != null) {
                        reversalTransactionType = transactionType.get_ValueAsString("Value");
                    } else {
                        throw new AdempiereException("@" + ElectronicInvoicingChanges.ReferenceDocument_ID + "@ " + "(@" + ElectronicInvoicingChanges.SP013_TransactionType_ID + "@) @NotFound@");
                    }
                }
            } else  {
                throw new AdempiereException("@" + ElectronicInvoicingChanges.ReferenceDocument_ID + "@ " + "(@" + ElectronicInvoicingChanges.SP013_TransactionType_ID + "@) @NotFound@");
            }
            addReversalDocumentInfo(documentNo,reverseOriginalDocument.getMovementDate(), senderNo, reversalTransactionType);
        }

        MBPartner businessPartner = (MBPartner) document.getC_BPartner();
        MBPartnerLocation businessPartnerLocation = (MBPartnerLocation) document.getC_BPartner_Location();
        MLocation location = (MLocation) businessPartnerLocation.getC_Location();
        MCountry country = MCountry.get(document.getCtx(), location.getC_Country_ID());
        MRegion region = MRegion.get(document.getCtx(), location.getC_Region_ID());
        
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
        if(order != null) {
            sOReference = order.getDocumentNo();
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
        this.documentDate = document.getMovementDate();
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
        this.documentTypeId = document.getC_DocType_ID();
        this.dueDate = document.getMovementDate();
        this.phone = businessPartnerLocation.getPhone();
        this.eMail = businessPartnerLocation.getEMail();
        this.postalCode = location.getPostal();

        MWarehouse warehouse = MWarehouse.get(document.getCtx(), document.getM_Warehouse_ID());
        this.warehouseName = warehouse.getName();

        BigDecimal totalLines = Env.ZERO;
        BigDecimal grandTotal = Env.ZERO;
        AtomicInteger productQuantities = new AtomicInteger(0);

        Arrays.asList(document.getLines()).forEach(documentLine -> {
            fiscalDocumentLines.add(new InOutFiscalDocumentLine(documentLine));
            MUOM uom = MUOM.get(document.getCtx(), documentLine.getC_UOM_ID());
            if(uom.get_ColumnIndex(ElectronicInvoicingChanges.SP013_EachUnitCount) >= 0 && uom.get_ValueAsBoolean(ElectronicInvoicingChanges.SP013_EachUnitCount)) {
                productQuantities.getAndAdd(Optional.ofNullable(documentLine.getMovementQty()).orElse(Env.ZERO).intValue());
            } else {
                productQuantities.getAndIncrement();
            }
        });

        for(IFiscalDocumentLine line : fiscalDocumentLines) {
            totalLines = totalLines.add(line.getLineNetAmount());
            grandTotal = grandTotal.add(line.getLineTotalAmount());
        }

        this.totalLines = totalLines;
        this.grandTotal = grandTotal;
        this.productQuantities = productQuantities.get();

        String amountInWords = Msg.getAmtInWords(Env.getLanguage(document.getCtx()), DisplayType.getNumberFormat(DisplayType.Amount).format(grandTotal));
        if(!Util.isEmpty(amountInWords)) {
            this.amountInWords = amountInWords;
        }
    }

    private void loadOrganizationData(MInOut document) {
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
        this.isManualDocument = document.isManualDocument();
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
            if(inOut.getC_Order_ID() > 0) {
                MOrder order = (MOrder) inOut.getC_Order();
                conversionRate = MConversionRate.getRate(order.getC_Currency_ID(), organizationCurrency.getC_Currency_ID(), 
                    inOut.getMovementDate(), order.getC_ConversionType_ID(), inOut.getAD_Client_ID(), inOut.getAD_Org_ID());
            } else {
                conversionRate = BigDecimal.ONE;
            }
            if(conversionRate == null || conversionRate.compareTo(Env.ZERO) == 0) {
                throw new AdempiereException("Error converting currency");
            }
        }
        return conversionRate;
    }

    @Override
    public BigDecimal getConvertedAmount(BigDecimal amount) {
        if(amount == null || amount.compareTo(Env.ZERO) == 0) {
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
    public final InOutFiscalDocument addReversalDocument(MInvoice reversalDocument) {
        fiscalReversalDocuments.add(new ReversalDocument(reversalDocument));
        return this;
    }

    public final InOutFiscalDocument addReversalDocumentInfo(String documentNo, Timestamp documentDate, String fiscalSenderNo, String transactionType) {
        fiscalReversalDocuments.add(new ReversalDocument(documentNo, documentDate, fiscalSenderNo, transactionType));
        return this;
    }

    @Override
    public int getInvoiceId() {
        return 0;
    }

    @Override
    public int getInOutId() {
        if(inOut != null) {
            return inOut.getM_InOut_ID();
        }
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