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
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCharge;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MProduct;
import org.compiere.model.MTax;
import org.compiere.model.MUOM;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.model.MWHDefinition;
import org.spin.model.MWHWithholding;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Implementation of IFiscalDocumentLine for Invoice lines
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop http://www.solopsoftware.com
 */
public class InvoiceFiscalDocumentLine implements IFiscalDocumentLine {

    private String documentLineUuid;
    private String productValue;
    private String productName;
    private String productDescription;
    private String productBarCode;
    private String lineDescription;
    private String productUnitOfMeasure;
    private BigDecimal quantity;
    private BigDecimal productPriceList;
    private BigDecimal productPrice;
    private BigDecimal taxRate;
    private String taxIndicator;
    private BigDecimal discount;
    private BigDecimal lineNetAmount;
    private BigDecimal lineTotalAmount;
    private String withholdingCode;
    private BigDecimal withholdingRate = Env.ZERO;
    private BigDecimal withholdingBaseAmount;

    public InvoiceFiscalDocumentLine(MInvoiceLine invoiceLine) {
        if(invoiceLine == null) {
            throw new AdempiereException("@C_InvoiceLine_ID@ @NotFound@");
        }
        convertDocument(invoiceLine);
    }

    public InvoiceFiscalDocumentLine(X_SP013_ElectronicLineSummary summaryLine) {
        if(summaryLine == null) {
            throw new AdempiereException("@SP013_ElectronicLineSummary_ID@ @NotFound@");
        }
        convertDocumentFromSummary(summaryLine);
    }

    private void convertDocument(MInvoiceLine invoiceLine) {
        String productValue = null;
        String productName = null;
        String productDescription = null;
        String productBarcode = null;
        BigDecimal taxRate = Env.ZERO;
        BigDecimal discount = Env.ZERO;
        
        MProduct product = null;
        MCharge charge = null;
        MTax tax = null;
        
        if (invoiceLine.getM_Product_ID() != 0) {
            product = MProduct.get(invoiceLine.getCtx(), invoiceLine.getM_Product_ID());
            productValue = product.getValue();
            productName = product.getName();
            productDescription = product.getDescription();
            productBarcode = product.getUPC();
            MUOM unitOfMeasure = MUOM.get(invoiceLine.getCtx(), product.getC_UOM_ID());
            this.productUnitOfMeasure = unitOfMeasure.getUOMSymbol();
        } else if (invoiceLine.getC_Charge_ID() != 0) {
            charge = MCharge.get(invoiceLine.getCtx(), invoiceLine.getC_Charge_ID());
            productValue = String.valueOf(charge.getC_Charge_ID());
            productName = charge.getName();
            productDescription = charge.getDescription();
            MUOM unitOfMeasure = MUOM.get(invoiceLine.getCtx(), 100);
            this.productUnitOfMeasure = unitOfMeasure.getUOMSymbol();
        }
        
        final int taxCategoryId = (product != null ? product.getC_TaxCategory_ID() : charge != null ? charge.getC_TaxCategory_ID() : 0);
        
        if (invoiceLine.getC_Tax_ID() != 0) {
            tax = MTax.get(invoiceLine.getCtx(), invoiceLine.getC_Tax_ID());
            taxRate = tax.getRate();
        } else {
            Optional<MTax> optionalTax = Arrays.stream(MTax.getAll(invoiceLine.getCtx()))
                    .filter(taxValue -> taxValue.getC_TaxCategory_ID() == taxCategoryId).max(Comparator.comparing(MTax::isDefault));
            if (optionalTax.isEmpty()) {
                throw new AdempiereException("@C_Tax_ID@ @NotFound@: " + invoiceLine);
            }
            tax = optionalTax.get();
            taxRate = tax.getRate();
        }
        
        if (tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID) > 0) {
            this.taxIndicator = ElectronicInvoicingUtil.getTaxIndicatorFromTax(invoiceLine.getCtx(), tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID));
        }
        if(Util.isEmpty(this.taxIndicator)) {
            throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_TaxType_ID + "@ @NotFound@ for @C_Tax_ID@ (" + tax.getName() + ")");
        }
        int invoiceLineId = invoiceLine.get_ID();
        if (invoiceLine.getReversalLine_ID() > 0) {
            invoiceLineId = invoiceLine.getReversalLine_ID();
        }

        String whereClause = MWHWithholding.COLUMNNAME_C_InvoiceLine_ID + "= ?";
        MWHWithholding withholding = new Query(invoiceLine.getCtx(), MWHWithholding.Table_Name, whereClause, invoiceLine.get_TrxName())
                .setParameters(invoiceLineId)
                .setClient_ID()
                .first();
        if (withholding != null && withholding.get_ID() > 0) {
            MWHDefinition withholdingDefinition = (MWHDefinition)withholding.getWH_Definition();
            if (withholdingDefinition == null || withholdingDefinition.get_ID() <= 0) {
                throw new AdempiereException("@WH_Definition_ID@ @NotFound@");
            }
            String code = withholdingDefinition.get_ValueAsString("Value");
            BigDecimal percentage = withholding.getWithholdingRate();
            this.withholdingCode = code;
            this.withholdingRate = Optional.ofNullable(percentage).orElse(Env.ZERO);
        }
        
        BigDecimal priceActual = Optional.ofNullable(invoiceLine.getPriceActual()).orElse(Env.ZERO);
        BigDecimal priceList = Optional.ofNullable(invoiceLine.getPriceList()).orElse(Env.ZERO);
        if (priceActual.compareTo(Env.ZERO) > 0
                && priceList.compareTo(Env.ZERO) > 0) {
            discount = invoiceLine.getPriceList().subtract(invoiceLine.getPriceActual());
            discount = discount.divide(invoiceLine.getPriceList(), MathContext.DECIMAL128);
            discount = discount.multiply(Env.ONEHUNDRED);
        }
        
        this.documentLineUuid = invoiceLine.getUUID();
        this.productValue = productValue;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productBarCode = productBarcode;
        this.productPrice = invoiceLine.getPriceActual();
        this.productPriceList = invoiceLine.getPriceList();
        this.lineDescription = invoiceLine.getDescription();
        this.quantity = invoiceLine.getQtyInvoiced();
        this.taxRate = taxRate;
        this.lineNetAmount = invoiceLine.getLineNetAmt();
        this.lineTotalAmount = invoiceLine.getLineTotalAmt();
        this.discount = discount;
    }

    private void convertDocumentFromSummary(X_SP013_ElectronicLineSummary summaryLine) {
        String productValue = null;
        String productName = null;
        String productDescription = null;
        String productBarcode = null;
        BigDecimal taxRate = Env.ZERO;
        BigDecimal discount = Env.ZERO;
        
        MProduct product = null;
        MCharge charge = null;
        MTax tax = null;
        
        productValue = summaryLine.getSP013_ElectronicProductType();
        productName = summaryLine.getName();
        productDescription = summaryLine.getDescription();
        MUOM unitOfMeasure = (MUOM) summaryLine.getC_UOM();

        if (summaryLine.getM_Product_ID() != 0) {
            product = MProduct.get(summaryLine.getCtx(), summaryLine.getM_Product_ID());
            productBarcode = product.getUPC();
            if (unitOfMeasure == null) {
                unitOfMeasure = MUOM.get(summaryLine.getCtx(), product.getC_UOM_ID());
            }
            if(Util.isEmpty(productValue)) {
                productValue = product.getValue();
            }
            if (Util.isEmpty(productName)) {
                productName = product.getName();
            }
        } else if (summaryLine.getC_Charge_ID() != 0) {
            charge = MCharge.get(summaryLine.getCtx(), summaryLine.getC_Charge_ID());
            if(Util.isEmpty(productValue)) {
                productValue = String.valueOf(charge.getC_Charge_ID());
            }
            if (Util.isEmpty(productName)) {
                productName = charge.getName();
            }
        }
        if (Util.isEmpty(productValue, true)) {
            productValue = summaryLine.getValue();
        }
        
        if (unitOfMeasure == null) {
            unitOfMeasure = MUOM.get(summaryLine.getCtx(), 100);
        }
        this.productUnitOfMeasure = unitOfMeasure.getUOMSymbol();
        
        final int taxCategoryId = (product != null ? product.getC_TaxCategory_ID() : charge != null ? charge.getC_TaxCategory_ID() : 0);
        
        if (summaryLine.getC_Tax_ID() != 0) {
            tax = MTax.get(summaryLine.getCtx(), summaryLine.getC_Tax_ID());
            taxRate = tax.getRate();
        } else {
            Optional<MTax> optionalTax = Arrays.stream(MTax.getAll(summaryLine.getCtx()))
                    .filter(taxValue -> taxValue.getC_TaxCategory_ID() == taxCategoryId)
                    .sorted(Comparator.comparing(MTax::isDefault).reversed())
                    .findFirst();
            if (!optionalTax.isPresent()) {
                throw new AdempiereException("@C_Tax_ID@ @NotFound@: " + summaryLine);
            }
            tax = optionalTax.get();
            taxRate = tax.getRate();
        }
        
        if (tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID) > 0) {
            this.taxIndicator = ElectronicInvoicingUtil.getTaxIndicatorFromTax(summaryLine.getCtx(), tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID));
        }

        BigDecimal multiplier = BigDecimal.ONE.add(
                taxRate.divide(Env.ONEHUNDRED, 10, BigDecimal.ROUND_HALF_UP));
        BigDecimal totalAmount = summaryLine.getLineTotalAmt().multiply(multiplier)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        this.documentLineUuid = summaryLine.getUUID();
        this.productValue = productValue;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productBarCode = productBarcode;
        this.productPrice = summaryLine.getPriceEntered();
        this.productPriceList = summaryLine.getPriceEntered();
        this.lineDescription = summaryLine.getDescription();
        this.quantity = summaryLine.getQty();
        this.taxRate = taxRate;
        this.lineNetAmount = summaryLine.getLineTotalAmt();
        this.lineTotalAmount = totalAmount;
        this.discount = discount;
    }

    @Override
    public String getdocumentLineUuid() {
        return documentLineUuid;
    }

    @Override
    public String getProductValue() {
        return productValue;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public String getProductDescription() {
        return productDescription;
    }

    @Override
    public String getProductBarCode() {
        return productBarCode;
    }

    @Override
    public String getLineDescription() {
        return lineDescription;
    }

    @Override
    public String getProductUnitOfMeasure() {
        return productUnitOfMeasure;
    }

    @Override
    public BigDecimal getQuantity() {
        return quantity;
    }

    @Override
    public BigDecimal getProductPriceList() {
        return productPriceList;
    }

    @Override
    public BigDecimal getProductPrice() {
        return productPrice;
    }

    @Override
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    @Override
    public String getTaxIndicator() {
        return taxIndicator;
    }

    @Override
    public BigDecimal getDiscount() {
        return discount;
    }

    @Override
    public BigDecimal getDiscountAmount() {
        return getProductPriceList().subtract(getProductPrice());
    }

    @Override
    public BigDecimal getLineNetAmount() {
        return lineNetAmount;
    }

    @Override
    public BigDecimal getLineTotalAmount() {
        return lineTotalAmount;
    }

    @Override
    public String getWithholdingCode() {
        return withholdingCode;
    }

    @Override
    public BigDecimal getWithholdingRate() {
        return withholdingRate;
    }

    @Override
    public BigDecimal getWithholdingBaseAmount() {
        return withholdingBaseAmount;
    }
}