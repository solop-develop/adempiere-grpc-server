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
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Properties;

import com.solop.sp013.core.model.X_SP013_ElectronicLineSummary;
import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.core.util.ElectronicInvoicingUtil;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Use it for fiscal documents line
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop http://www.solopsoftware.com
 */
public class FiscalDocumentLine {

	/**
	 * Default constructor
	 */
	public FiscalDocumentLine(FiscalDocument document, MInvoiceLine documentLine) {
		this.document = document;
		if(documentLine == null) {
			throw new AdempiereException("@C_InvoiceLine_ID@ @NotFound@");
		}
		//	Fill it
		convertDocument(documentLine);
	}

	/**
	 * Construct From Electronic Line Summary
	 */
	public FiscalDocumentLine(FiscalDocument document, X_SP013_ElectronicLineSummary documentLine) {
		this.document = document;
		if(documentLine == null) {
			throw new AdempiereException("@SP013_ElectronicLineSummary_ID@ @NotFound@");
		}
		//	Fill it
		convertDocument(documentLine);
	}

	/**
	 * Convert document like invoice, credit memo or debit memo
	 * @param invoiceLine
	 * @return void
	 */
	private void convertDocument(MInvoiceLine invoiceLine) {
		String productValue = null;
		String productName = null;
		String productDescription = null;
		String productBarcode = null;
		BigDecimal taxRate = Env.ZERO;
		BigDecimal discount = Env.ZERO;
		//	Get Product Attributes
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
			withProductUnitOfMeasure(unitOfMeasure.getUOMSymbol());
		} else if (invoiceLine.getC_Charge_ID() != 0) {
			charge = MCharge.get(invoiceLine.getCtx(), invoiceLine.getC_Charge_ID());
			productValue = String.valueOf(charge.getC_Charge_ID());
			productName = charge.getName();
			productDescription = charge.getDescription();
			MUOM unitOfMeasure = MUOM.get(invoiceLine.getCtx(), 100);
			withProductUnitOfMeasure(unitOfMeasure.getUOMSymbol());
		}
		//	Get tax Category
		final int taxCategoryId = (product != null ? product.getC_TaxCategory_ID() : charge != null ? charge.getC_TaxCategory_ID() : 0);
		//	Get Tax Rate
		if (invoiceLine.getC_Tax_ID() != 0) {
			tax = MTax.get(invoiceLine.getCtx(), invoiceLine.getC_Tax_ID());
			taxRate = tax.getRate();
		} else {
			Optional<MTax> optionalTax = Arrays.stream(MTax.getAll(invoiceLine.getCtx()))
					.filter(taxValue -> taxValue.getC_TaxCategory_ID() == taxCategoryId)
					.sorted(Comparator.comparing(MTax::isDefault).reversed())
					.findFirst();
			if (!optionalTax.isPresent()) {
				throw new AdempiereException("@C_Tax_ID@ @NotFound@: " + invoiceLine);
			}
			tax = optionalTax.get();
			taxRate = tax.getRate();
		}
		if (tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID) > 0) {
			taxIndicator = ElectronicInvoicingUtil.getTaxIndicatorFromTax(invoiceLine.getCtx(), tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID));
		}
		//	Get Withholding Info
		if (invoiceLine.get_ValueAsInt("WH_Definition_ID") > 0) {
			MTable withholdingTable = MTable.get(Env.getCtx(), "WH_Definition");
			if (withholdingTable != null) {
				PO withholdingDefinition = withholdingTable.getPO(invoiceLine.get_ValueAsInt("WH_Definition_ID"), invoiceLine.get_TrxName());
				if (withholdingDefinition != null) {
					String code = withholdingDefinition.get_ValueAsString("Value");
					BigDecimal percentage = (BigDecimal) withholdingDefinition.get_Value("Percentage");
					withWithholdingCode(code);
					withWithholdingRate(Optional.ofNullable(percentage).orElse(Env.ZERO));
				}
			}
		}
		//	Discount
		BigDecimal priceActual = Optional.ofNullable(invoiceLine.getPriceActual()).orElse(Env.ZERO);
		BigDecimal priceList = Optional.ofNullable(invoiceLine.getPriceList()).orElse(Env.ZERO);
		if (priceActual.compareTo(Env.ZERO) > 0
				&& priceList.compareTo(Env.ZERO) > 0) {
			discount = invoiceLine.getPriceList().subtract(invoiceLine.getPriceActual());
			discount = discount.divide(invoiceLine.getPriceList(), MathContext.DECIMAL128);
			discount = discount.multiply(Env.ONEHUNDRED);
		}
		//	Set Attributes
		withDocumentLineUuid(invoiceLine.getUUID())
				.withProductValue(productValue)
				.withProductName(productName)
				.withProductDescription(productDescription)
				.withProductBarCode(productBarcode)
				.withProductPrice(invoiceLine.getPriceActual())
				.withProductPriceList(invoiceLine.getPriceList())
				.withLineDescription(invoiceLine.getDescription())
				.withQuantity(invoiceLine.getQtyInvoiced())
				.withTaxRate(taxRate)
				.withTaxIndicator(taxIndicator)
				.withLineNetAmount(invoiceLine.getLineNetAmt())
				.withLineTotalAmount(invoiceLine.getLineTotalAmt())
				.withDiscount(discount);
	}

	/**
	 * Convert document like invoice, credit memo or debit memo
	 * @param summaryLine
	 * @return void
	 */
	private void convertDocument(X_SP013_ElectronicLineSummary summaryLine) {
		String productValue = null;
		String productName = null;
		String productDescription = null;
		String productBarcode = null;
		BigDecimal taxRate = Env.ZERO;
		BigDecimal discount = Env.ZERO;
		//	Get Product Attributes
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
		if (unitOfMeasure == null) {
			unitOfMeasure = MUOM.get(summaryLine.getCtx(), 100);
		}
		withProductUnitOfMeasure(unitOfMeasure.getUOMSymbol());
		//	Get tax Category
		final int taxCategoryId = (product != null ? product.getC_TaxCategory_ID() : charge != null ? charge.getC_TaxCategory_ID() : 0);
		//	Get Tax Rate
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
			taxIndicator = ElectronicInvoicingUtil.getTaxIndicatorFromTax(summaryLine.getCtx(), tax.get_ValueAsInt(ElectronicInvoicingChanges.SP013_TaxType_ID));
		}

		//	Set Attributes

		BigDecimal multiplier = BigDecimal.ONE.add(
				taxRate.divide(Env.ONEHUNDRED,
						10, RoundingMode.HALF_UP));
		BigDecimal totalAmount = summaryLine.getLineTotalAmt().multiply(multiplier)
				.setScale(2, RoundingMode.HALF_UP);

		withDocumentLineUuid(summaryLine.getUUID())
				.withProductValue(productValue)
				.withProductName(productName)
				.withProductDescription(productDescription)
				.withProductBarCode(productBarcode)
				.withProductPrice(summaryLine.getPriceEntered())
				.withProductPriceList(summaryLine.getPriceEntered())
				.withLineDescription(summaryLine.getDescription())
				.withQuantity(summaryLine.getQty())
				.withTaxRate(taxRate)
				.withTaxIndicator(taxIndicator)
				.withLineNetAmount(summaryLine.getLineTotalAmt())
				.withLineTotalAmount(totalAmount)
				.withDiscount(discount);
	}

	/**	Parent Document	*/
	private FiscalDocument document;
	/**	Document Line UUID	*/
	private String documentLineUuid = null;
	/**	Product Value	*/
	private String productValue = null;
	/**	Business Partner Tax ID	*/
	private String productName = null;
	/**	Document productDescription	*/
	private String productDescription = null;
	/**	Document Note	*/
	private String productBarCode = null;
	/**	Line Description	*/
	private String lineDescription = null;
	/**	Product UOM	*/
	private String productUnitOfMeasure = null;
	/**	Quantity	*/
	private BigDecimal quantity = null;
	/**	Product Price List	*/
	private BigDecimal productPriceList = null;
	/**	Product Price	*/
	private BigDecimal productPrice = null;
	/**	Tax Rate	*/
	private BigDecimal taxRate = null;
	/**	Tax Indicator	*/
	private String taxIndicator = null;
	/**	Discount	*/
	private BigDecimal discount = null;
	/**	Invoice	*/
	private MInvoice invoice = null;
	/**	Line Net Amount	*/
	private BigDecimal lineNetAmount = null;
	/**	Line Net Amount	*/
	private BigDecimal lineTotalAmount = null;
	/**	Product Value	*/
	private String withholdingCode = null;
	/**	Wihholding Rate	*/
	private BigDecimal withholdingRate = Env.ZERO;
	/**	Base Amount	*/
	private BigDecimal withholdingBaseAmount = null;

	/**
	 * Document Type Definition
	 */
	enum DocumentType {
		INVOICE, CREDIT_MEMO, DEBIT_MEMO
	}

	/**
	 * @param documentLineUuid the documentLineUuid to set
	 */
	public final FiscalDocumentLine withDocumentLineUuid(String documentLineUuid) {
		this.documentLineUuid = documentLineUuid;
		return this;
	}

	/**
	 * @param productValue the productValue to set
	 */
	public final FiscalDocumentLine withProductValue(String productValue) {
		this.productValue = productValue;
		return this;
	}

	/**
	 * @param productName the productName to set
	 */
	public final FiscalDocumentLine withProductName(String productName) {
		this.productName = productName;
		return this;
	}

	/**
	 * @param productDescription the productDescription to set
	 */
	public final FiscalDocumentLine withProductDescription(String productDescription) {
		this.productDescription = productDescription;
		return this;
	}

	/**
	 * @param productBarCode the productBarCode to set
	 */
	public final FiscalDocumentLine withProductBarCode(String productBarCode) {
		this.productBarCode = productBarCode;
		return this;
	}

	/**
	 * @param lineDescription the lineDescription to set
	 */
	public final FiscalDocumentLine withLineDescription(String lineDescription) {
		this.lineDescription = lineDescription;
		return this;
	}

	/**
	 * @param quantity the quantity to set
	 */
	public final FiscalDocumentLine withQuantity(BigDecimal quantity) {
		this.quantity = quantity;
		return this;
	}

	/**
	 * @param productPrice the productPrice to set
	 */
	public final FiscalDocumentLine withProductPrice(BigDecimal productPrice) {
		this.productPrice = productPrice;
		return this;
	}

	/**
	 * @param productPriceList the productPriceList to set
	 */
	public final FiscalDocumentLine withProductPriceList(BigDecimal productPriceList) {
		this.productPriceList = productPriceList;
		return this;
	}

	/**
	 * @param taxRate the taxRate to set
	 */
	public final FiscalDocumentLine withTaxRate(BigDecimal taxRate) {
		this.taxRate = taxRate;
		return this;
	}

	/**
	 * @param discount the discount to set
	 */
	public final FiscalDocumentLine withDiscount(BigDecimal discount) {
		this.discount = discount;
		return this;
	}

	/**
	 * @return the documentLineUuid
	 */
	public final String getdocumentLineUuid() {
		return documentLineUuid;
	}

	/**
	 * @return the productValue
	 */
	public final String getProductValue() {
		return productValue;
	}

	/**
	 * @return the productName
	 */
	public final String getProductName() {
		return productName;
	}

	/**
	 * @return the productDescription
	 */
	public final String getProductDescription() {
		return productDescription;
	}

	/**
	 * @return the productBarCode
	 */
	public final String getProductBarCode() {
		return productBarCode;
	}

	/**
	 * @return the lineDescription
	 */
	public final String getLineDescription() {
		return lineDescription;
	}

	/**
	 * @return the quantity
	 */
	public final BigDecimal getQuantity() {
		return quantity;
	}

	/**
	 * @return the productPriceList
	 */
	public final BigDecimal getProductPriceList() {
		return productPriceList;
	}

	/**
	 * @return the productPrice
	 */
	public final BigDecimal getProductPrice() {
		return productPrice;
	}

	/**
	 * @return the taxRate
	 */
	public final BigDecimal getTaxRate() {
		return taxRate;
	}

	/**
	 * @return the discount
	 */
	public final BigDecimal getDiscount() {
		return discount;
	}

	public final BigDecimal getDiscountAmount() {
		return getProductPriceList().subtract(getProductPrice());
	}

	public String getTaxIndicator() {
		return taxIndicator;
	}

	public FiscalDocumentLine withTaxIndicator(String taxIndicator) {
		this.taxIndicator = taxIndicator;
		return this;
	}

	public String getProductUnitOfMeasure() {
		return productUnitOfMeasure;
	}

	public FiscalDocumentLine withProductUnitOfMeasure(String productUnitOfMeasure) {
		this.productUnitOfMeasure = productUnitOfMeasure;
		return this;
	}

	public BigDecimal getLineNetAmount() {
		return lineNetAmount;
	}

	public FiscalDocumentLine withLineNetAmount(BigDecimal lineNetAmount) {
		this.lineNetAmount = lineNetAmount;
		return this;
	}

	public BigDecimal getLineTotalAmount() {
		return lineTotalAmount;
	}

	public FiscalDocumentLine withLineTotalAmount(BigDecimal lineTotalAmount) {
		this.lineTotalAmount = lineTotalAmount;
		return this;
	}

	public String getWithholdingCode() {
		return withholdingCode;
	}

	public FiscalDocumentLine withWithholdingCode(String withholdingCode) {
		this.withholdingCode = withholdingCode;
		return this;
	}

	public BigDecimal getWithholdingRate() {
		return withholdingRate;
	}

	public FiscalDocumentLine withWithholdingRate(BigDecimal withholdingRate) {
		this.withholdingRate = withholdingRate;
		return this;
	}

	public BigDecimal getWithholdingBaseAmount() {
		return withholdingBaseAmount;
	}

	public FiscalDocumentLine withWithholdingBaseAmount(BigDecimal withholdingBaseAmount) {
		this.withholdingBaseAmount = withholdingBaseAmount;
		return this;
	}
}
