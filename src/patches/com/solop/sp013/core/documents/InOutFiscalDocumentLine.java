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
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCharge;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MTax;
import org.compiere.model.MUOM;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Implementation of IFiscalDocumentLine for Material Receipt lines
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop http://www.solopsoftware.com
 */
public class InOutFiscalDocumentLine implements IFiscalDocumentLine {

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

    public InOutFiscalDocumentLine(MInOutLine inOutLine) {
        if(inOutLine == null) {
            throw new AdempiereException("@M_InOutLine_ID@ @NotFound@");
        }
        convertDocument(inOutLine);
    }

    private void convertDocument(MInOutLine inOutLine) {
        String productValue = null;
        String productName = null;
        String productDescription = null;
        String productBarcode = null;
        BigDecimal taxRate = Env.ZERO;
        BigDecimal discount = Env.ZERO;
        
        MProduct product = null;
        MCharge charge = null;
        MTax tax = null;
        
        if (inOutLine.getM_Product_ID() != 0) {
            product = MProduct.get(inOutLine.getCtx(), inOutLine.getM_Product_ID());
            productValue = product.getValue();
            productName = product.getName();
            productDescription = product.getDescription();
            productBarcode = product.getUPC();
            MUOM unitOfMeasure = MUOM.get(inOutLine.getCtx(), product.getC_UOM_ID());
            this.productUnitOfMeasure = unitOfMeasure.getUOMSymbol();
        } else if (inOutLine.getC_Charge_ID() != 0) {
            charge = MCharge.get(inOutLine.getCtx(), inOutLine.getC_Charge_ID());
            productValue = String.valueOf(charge.getC_Charge_ID());
            productName = charge.getName();
            productDescription = charge.getDescription();
            MUOM unitOfMeasure = MUOM.get(inOutLine.getCtx(), 100);
            this.productUnitOfMeasure = unitOfMeasure.getUOMSymbol();
        }
        
        final int taxCategoryId = (product != null ? product.getC_TaxCategory_ID() : charge != null ? charge.getC_TaxCategory_ID() : 0);
        
        Optional<MTax> optionalTax = Arrays.stream(MTax.getAll(inOutLine.getCtx()))
                .filter(taxValue -> taxValue.getC_TaxCategory_ID() == taxCategoryId).max(Comparator.comparing(MTax::isDefault));
        if (optionalTax.isEmpty()) {
            throw new AdempiereException("@C_Tax_ID@ @NotFound@: " + inOutLine);
        }
        tax = optionalTax.get();
        taxRate = tax.getRate();
        
        if (tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID) > 0) {
            this.taxIndicator = ElectronicInvoicingUtil.getTaxIndicatorFromTax(inOutLine.getCtx(), tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID));
        }
        if(Util.isEmpty(this.taxIndicator)) {
            throw new AdempiereException("@" + ElectronicInvoicingChanges.SP013_TaxType_ID + "@ @NotFound@ for @C_Tax_ID@ (" + tax.getName() + ")");
        }
        
        BigDecimal priceActual = Env.ZERO;
        BigDecimal priceList = Env.ZERO;
        
        MInOut inOut = (MInOut) inOutLine.getM_InOut();
        if(inOut.getC_Order_ID() > 0) {
            MOrder order = (MOrder) inOut.getC_Order();
            MOrderLine[] orderLines = order.getLines();
            for(MOrderLine orderLine : orderLines) {
                if(orderLine.getM_Product_ID() == inOutLine.getM_Product_ID()) {
                    priceActual = orderLine.getPriceActual();
                    priceList = orderLine.getPriceList();
                    break;
                }
            }
        }
        
        if (priceActual.compareTo(Env.ZERO) > 0
                && priceList.compareTo(Env.ZERO) > 0) {
            discount = priceList.subtract(priceActual);
            discount = discount.divide(priceList, MathContext.DECIMAL128);
            discount = discount.multiply(Env.ONEHUNDRED);
        }
        BigDecimal movementQty = inOutLine.getMovementQty();
        if (MInOut.MOVEMENTTYPE_CustomerReturns.equals(inOut.getMovementType())){
            movementQty = movementQty.negate();
        }

        BigDecimal netAmount = priceActual.multiply(movementQty);
        BigDecimal totalAmount = netAmount.add(netAmount.multiply(taxRate).divide(Env.ONEHUNDRED, 2, BigDecimal.ROUND_HALF_UP));
        
        this.documentLineUuid = inOutLine.getUUID();
        this.productValue = productValue;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productBarCode = productBarcode;
        this.productPrice = priceActual;
        this.productPriceList = priceList;
        this.lineDescription = inOutLine.getDescription();
        this.quantity = movementQty;
        this.taxRate = taxRate;
        this.lineNetAmount = netAmount;
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