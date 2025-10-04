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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Interface for fiscal documents like Invoice, Credit Memo, Debit Memo and Material Receipt
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public interface IFiscalDocument {
    
    /**
     * Payment Rule
     */
    enum PaymentRule {
        IMMEDIATE, CREDIT
    }
    
    String getDocumentType();
    String getTransactionType();
    String getDocumentUuid();
    String getDocumentNo();
    String getFiscalDocumentNo();
    Timestamp getDocumentDate();
    String getBusinessPartnerName();
    String getBusinessPartnerLastName();
    String getBusinessPartnerValue();
    String getBusinessPartnerTaxId();
    String getBusinessPartnerTaxType();
    String getBusinessPartnerDuns();
    String getAddress1();
    String getAddress2();
    String getAddress3();
    String getAddress4();
    String getCityName();
    String getRegionName();
    String getCountryName();
    String getCountryCode();
    String getPostalCode();
    String getPhone();
    String getEMail();
    String getDescription();
    String getDocumentNote();
    String getFiscalComment();
    String getPoReferenceNo();
    String getSoReferenceNo();
    String getSalesRepresentativeValue();
    String getSalesRepresentativeName();
    String getPaymentTerm();
    String getSalesRegionValue();
    String getSalesRegionName();
    BigDecimal getTotalLines();
    BigDecimal getGrandTotal();
    String getAmountInWords();
    String getDocumentTypeName();
    String getDeliveryAddress();
    String getDeliveryPhone();
    String getWarehouseName();
    int getProductQuantities();
    String getPosName();
    boolean isDiscountPrinted();
    boolean isTaxIncluded();
    int getDocumentTypeId();
    int getDaysDue();
    Timestamp getDueDate();
    PaymentRule getPaymentRule();
    int getOrganizationId();
    String getOrganizationTaxId();
    String getOrganizationName();
    String getOrganizationPhone();
    String getOrganizationEmail();
    String getOrganizationFiscalCode();
    String getOrganizationAddress1();
    String getOrganizationAddress2();
    String getOrganizationAddress3();
    String getOrganizationAddress4();
    String getOrganizationCityName();
    String getOrganizationRegionName();
    String getOrganizationCountryName();
    String getCurrencyCode();
    int getDocumentFormat();
    int getFiscalSenderId();
    
    List<IFiscalDocumentLine> getFiscalDocumentLines();
    List<IFiscalDocumentTax> getFiscalDocumentTaxes();
    List<IFiscalDocumentPayment> getFiscalDocumentPayments();
    List<ReversalDocument> getFiscalReversalDocuments();
    
    BigDecimal getCurrencyRate();
    BigDecimal getConvertedAmount(BigDecimal amount);
    
    boolean hasReversalDocument();
    ReversalDocument getFirstReversalDocument();
    
    int getInvoiceId();
    int getInOutId();


    boolean isManualDocument();
    // Setter methods
    IFiscalDocument withFiscalDocumentNo(String fiscalDocumentNo);
}