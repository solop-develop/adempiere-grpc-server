package com.solop.sp013.lpe.support.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.documents.IFiscalDocumentLine;
import com.solop.sp013.core.documents.IFiscalDocumentTax;
import com.solop.sp013.core.documents.ReversalDocument;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Optional;

/**
 * Converts a generic IFiscalDocument (built from a MInvoice) into the NubeFact
 * generar_comprobante JSON body (Factura / Boleta / Nota de Crédito / Nota de Débito).
 * Mirror of the Argentina/Uruguay E_InvoiceDocument.
 *
 * @author Gabriel Escalona
 */
public class NF_InvoiceDocument implements INubeFactDocument {

    /**	Receiver document type codes (cliente_tipo_de_documento)	*/
    public static final String DOC_TYPE_RUC = "6";
    public static final String DOC_TYPE_DNI = "1";
    public static final String DOC_TYPE_FOREIGN_CARD = "4";
    public static final String DOC_TYPE_PASSPORT = "7";
    public static final String DOC_TYPE_NON_DOMICILED = "0";
    /**	Generic receiver type for sales below S/700 without an identified customer (boleta)	*/
    public static final String DOC_TYPE_VARIOUS = "-";

    /**	Item tax types (tipo_de_igv): 1 = Gravado, 8 = Exonerado, 9 = Inafecto	*/
    public static final String IGV_TYPE_TAXED = "1";
    public static final String IGV_TYPE_EXEMPT = "8";
    public static final String IGV_TYPE_UNAFFECTED = "9";

    /**	Currency codes (moneda): 1 = Soles, 2 = Dólares, 3 = Euros, 4 = Libra esterlina	*/
    public static final int CURRENCY_PEN = 1;
    public static final int CURRENCY_USD = 2;
    public static final int CURRENCY_EUR = 3;
    public static final int CURRENCY_GBP = 4;

    /**	Default IGV percentage	*/
    public static final BigDecimal DEFAULT_IGV_PERCENTAGE = new BigDecimal("18.00");
    /**	SUNAT transaction (sunat_transaction): 1 = Venta interna	*/
    public static final int SUNAT_TRANSACTION_INTERNAL_SALE = 1;
    /**	Measure units (unidad_de_medida): NIU = product, ZZ = service	*/
    public static final String UNIT_PRODUCT = "NIU";

    protected IFiscalDocument document;

    @Override
    public void setDocument(IFiscalDocument document) {
        this.document = document;
    }

    @Override
    public String getOperation() {
        return OPERATION_GENERATE_COMPROBANTE;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    /**
     * Comprobante type (tipo_de_comprobante) from the SP013_TransactionType assigned to the
     * document type. For Peru the transaction type Value is the SUNAT comprobante code (1..4).
     */
    public int getComprobanteType() {
        String transactionType = document.getTransactionType();
        if(Util.isEmpty(transactionType, true)) {
            throw new AdempiereException("@SP013_TransactionType_ID@ @NotFound@");
        }
        try {
            return Integer.parseInt(transactionType.trim());
        } catch (NumberFormatException e) {
            throw new AdempiereException("@SP013_TransactionType_ID@ @Invalid@: " + transactionType);
        }
    }

    /**
     * Series part of the fiscal number (serie). Expected format SERIE-NUMERO (e.g. F001-123).
     */
    public String getSeries() {
        String[] parts = splitFiscalNumber();
        return parts[0];
    }

    /**
     * Correlative part of the fiscal number (numero), digits only.
     */
    public long getNumber() {
        String[] parts = splitFiscalNumber();
        String value = parts[1].replaceAll("\\D", "");
        if(Util.isEmpty(value, true)) {
            throw new AdempiereException("@DocumentNo@ @Invalid@ (NubeFact): " + getFiscalNumber());
        }
        return Long.parseLong(value);
    }

    private String getFiscalNumber() {
        String fiscalNumber = document.getFiscalDocumentNo();
        if(Util.isEmpty(fiscalNumber, true)) {
            fiscalNumber = document.getDocumentNo();
        }
        return fiscalNumber;
    }

    private String[] splitFiscalNumber() {
        String fiscalNumber = getFiscalNumber();
        if(Util.isEmpty(fiscalNumber, true) || !fiscalNumber.contains("-")) {
            throw new AdempiereException("@DocumentNo@ @Invalid@ (NubeFact, expected SERIE-NUMERO): " + fiscalNumber);
        }
        String value = fiscalNumber.trim();
        int dash = value.indexOf('-');
        return new String[]{value.substring(0, dash).trim(), value.substring(dash + 1).trim()};
    }

    /**
     * NubeFact receiver document type (cliente_tipo_de_documento) from the business partner tax group
     */
    public String getReceiverDocumentType() {
        if(Util.isEmpty(document.getBusinessPartnerTaxId(), true)) {
            return DOC_TYPE_VARIOUS;
        }
        String taxType = Optional.ofNullable(document.getBusinessPartnerTaxType()).orElse("").trim().toUpperCase();
        switch (taxType) {
            case RUC:
                return DOC_TYPE_RUC;
            case DNI:
                return DOC_TYPE_DNI;
            case FOREIGN_CARD:
                return DOC_TYPE_FOREIGN_CARD;
            case PASSPORT:
                return DOC_TYPE_PASSPORT;
            case NON_DOMICILED:
                return DOC_TYPE_NON_DOMICILED;
            default:
                return DOC_TYPE_VARIOUS;
        }
    }

    /**
     * NubeFact currency code (moneda)
     */
    public int getCurrencyCode() {
        String currencyCode = Optional.ofNullable(document.getCurrencyCode()).orElse("").trim().toUpperCase();
        switch (currencyCode) {
            case "USD":
                return CURRENCY_USD;
            case "EUR":
                return CURRENCY_EUR;
            case "GBP":
                return CURRENCY_GBP;
            case "PEN":
            case "":
            default:
                return CURRENCY_PEN;
        }
    }

    @Override
    public ObjectNode getConvertedDocument(ObjectMapper mapper) {
        ObjectNode comprobante = mapper.createObjectNode();
        comprobante.put(NubeFactFields.OPERATION, getOperation());
        comprobante.put(NubeFactFields.VOUCHER_TYPE, getComprobanteType());
        comprobante.put(NubeFactFields.SERIES, getSeries());
        comprobante.put(NubeFactFields.NUMBER, getNumber());
        comprobante.put(NubeFactFields.SUNAT_TRANSACTION, SUNAT_TRANSACTION_INTERNAL_SALE);
        //	Customer
        comprobante.put(NubeFactFields.CUSTOMER_DOCUMENT_TYPE, getReceiverDocumentType());
        comprobante.put(NubeFactFields.CUSTOMER_DOCUMENT_NUMBER, Optional.ofNullable(document.getBusinessPartnerTaxId()).orElse(""));
        comprobante.put(NubeFactFields.CUSTOMER_NAME, Optional.ofNullable(document.getBusinessPartnerName()).orElse(""));
        comprobante.put(NubeFactFields.CUSTOMER_ADDRESS, Optional.ofNullable(document.getAddress1()).orElse(""));
        comprobante.put(NubeFactFields.CUSTOMER_EMAIL, Optional.ofNullable(document.getEMail()).orElse(""));
        //	Dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        comprobante.put(NubeFactFields.ISSUE_DATE, dateFormat.format(document.getDocumentDate()));
        if(document.getDueDate() != null) {
            comprobante.put(NubeFactFields.DUE_DATE, dateFormat.format(document.getDueDate()));
        }
        //	Currency
        int currency = getCurrencyCode();
        comprobante.put(NubeFactFields.CURRENCY, currency);
        if(currency != CURRENCY_PEN) {
            comprobante.put(NubeFactFields.EXCHANGE_RATE, scale(document.getCurrencyRate()));
        }
        comprobante.put(NubeFactFields.IGV_PERCENTAGE, DEFAULT_IGV_PERCENTAGE);
        //	Totals (classified from the document taxes)
        BigDecimal taxedBase = BigDecimal.ZERO;
        BigDecimal exemptBase = BigDecimal.ZERO;
        BigDecimal igvAmount = BigDecimal.ZERO;
        for(IFiscalDocumentTax tax : document.getFiscalDocumentTaxes()) {
            BigDecimal taxRate = Optional.ofNullable(tax.getTaxRate()).orElse(BigDecimal.ZERO);
            BigDecimal baseAmount = scale(tax.getTaxBaseAmount());
            if(taxRate.compareTo(BigDecimal.ZERO) > 0) {
                taxedBase = taxedBase.add(baseAmount);
                igvAmount = igvAmount.add(scale(tax.getTaxAmount()));
            } else {
                exemptBase = exemptBase.add(baseAmount);
            }
        }
        if(taxedBase.signum() != 0) {
            comprobante.put(NubeFactFields.TOTAL_TAXED, taxedBase);
        }
        if(exemptBase.signum() != 0) {
            comprobante.put(NubeFactFields.TOTAL_EXEMPT, exemptBase);
        }
        if(igvAmount.signum() != 0) {
            comprobante.put(NubeFactFields.TOTAL_IGV, igvAmount);
        }
        comprobante.put(NubeFactFields.TOTAL, scale(document.getGrandTotal()));
        //	Credit / debit note: reference the modified document
        addModifiedDocument(comprobante);
        //	Items
        comprobante.set(NubeFactFields.ITEMS, convertItems(mapper));
        //	Credit sale (cuotas)
        ArrayNode creditSale = convertCreditSale(mapper);
        if(creditSale != null && creditSale.size() > 0) {
            comprobante.set(NubeFactFields.CREDIT_SALE, creditSale);
        }
        comprobante.put(NubeFactFields.SEND_AUTOMATICALLY_TO_SUNAT, true);
        comprobante.put(NubeFactFields.SEND_AUTOMATICALLY_TO_CUSTOMER, false);
        return comprobante;
    }

    /**
     * Build the items array (one entry per fiscal document line)
     */
    private ArrayNode convertItems(ObjectMapper mapper) {
        ArrayNode items = mapper.createArrayNode();
        for(IFiscalDocumentLine line : document.getFiscalDocumentLines()) {
            BigDecimal quantity = Optional.ofNullable(line.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal netAmount = scale(line.getLineNetAmount());
            BigDecimal totalAmount = scale(line.getLineTotalAmount());
            BigDecimal igvAmount = totalAmount.subtract(netAmount);
            BigDecimal taxRate = Optional.ofNullable(line.getTaxRate()).orElse(BigDecimal.ZERO);
            BigDecimal unitNet = quantity.signum() == 0 ? netAmount : netAmount.divide(quantity, 6, RoundingMode.HALF_UP);
            BigDecimal unitTotal = quantity.signum() == 0 ? totalAmount : totalAmount.divide(quantity, 6, RoundingMode.HALF_UP);

            ObjectNode item = mapper.createObjectNode();
            item.put(NubeFactFields.MEASURE_UNIT, Optional.ofNullable(line.getProductUnitOfMeasure()).filter(value -> !Util.isEmpty(value, true)).orElse(UNIT_PRODUCT));
            item.put(NubeFactFields.CODE, Optional.ofNullable(line.getProductValue()).orElse(""));
            item.put(NubeFactFields.DESCRIPTION, Optional.ofNullable(line.getProductName()).orElse(Optional.ofNullable(line.getLineDescription()).orElse("")));
            item.put(NubeFactFields.QUANTITY, quantity.setScale(6, RoundingMode.HALF_UP));
            item.put(NubeFactFields.UNIT_VALUE, unitNet);
            item.put(NubeFactFields.UNIT_PRICE, unitTotal);
            BigDecimal discount = Optional.ofNullable(line.getDiscountAmount()).orElse(BigDecimal.ZERO);
            if(discount.signum() != 0) {
                item.put(NubeFactFields.DISCOUNT, scale(discount));
            }
            item.put(NubeFactFields.SUBTOTAL, netAmount);
            item.put(NubeFactFields.IGV_TYPE, taxRate.compareTo(BigDecimal.ZERO) > 0 ? IGV_TYPE_TAXED : IGV_TYPE_EXEMPT);
            item.put(NubeFactFields.IGV, igvAmount);
            item.put(NubeFactFields.TOTAL, totalAmount);
            items.add(item);
        }
        return items;
    }

    /**
     * Build the credit sale (venta_al_credito) cuotas for credit payment rule documents.
     * Single installment for the grand total at the due date (refine with payment terms later).
     */
    private ArrayNode convertCreditSale(ObjectMapper mapper) {
        if(document.getPaymentRule() != IFiscalDocument.PaymentRule.CREDIT || document.getDueDate() == null) {
            return null;
        }
        ArrayNode creditSale = mapper.createArrayNode();
        ObjectNode installment = mapper.createObjectNode();
        installment.put(NubeFactFields.INSTALLMENT, 1);
        installment.put(NubeFactFields.INSTALLMENT_PAYMENT_DATE, new SimpleDateFormat("dd-MM-yyyy").format(document.getDueDate()));
        installment.put(NubeFactFields.AMOUNT, scale(document.getGrandTotal()));
        creditSale.add(installment);
        return creditSale;
    }

    /**
     * Credit / debit notes must reference the modified document (documento_que_se_modifica_*).
     * The fiscal number is expected with the standard format SERIE-NUMERO (F001-123).
     */
    private void addModifiedDocument(ObjectNode comprobante) {
        if(!document.hasReversalDocument()) {
            return;
        }
        ReversalDocument reversalDocument = document.getFirstReversalDocument();
        String reversalNo = Optional.ofNullable(reversalDocument.getFiscalSenderNo())
                .filter(value -> !Util.isEmpty(value, true))
                .orElse(reversalDocument.getDocumentNo());
        if(Util.isEmpty(reversalNo, true) || !reversalNo.contains("-")) {
            return;
        }
        int dash = reversalNo.indexOf('-');
        String series = reversalNo.substring(0, dash).trim();
        String number = reversalNo.substring(dash + 1).replaceAll("\\D", "");
        //	1 = modifies a Factura, 2 = modifies a Boleta (series prefix F / B)
        int modifiedType = series.toUpperCase().startsWith("B") ? 2 : 1;
        comprobante.put(NubeFactFields.MODIFIED_DOCUMENT_TYPE, modifiedType);
        comprobante.put(NubeFactFields.MODIFIED_DOCUMENT_SERIES, series);
        if(!Util.isEmpty(number, true)) {
            comprobante.put(NubeFactFields.MODIFIED_DOCUMENT_NUMBER, Long.parseLong(number));
        }
        //	Default reason: 1 = Anulación de la operación (NC) / Intereses por mora (ND). Refine via config.
        if(getComprobanteType() == Integer.parseInt(NOTA_CREDITO)) {
            comprobante.put(NubeFactFields.CREDIT_NOTE_TYPE, 1);
        } else if(getComprobanteType() == Integer.parseInt(NOTA_DEBITO)) {
            comprobante.put(NubeFactFields.DEBIT_NOTE_TYPE, 1);
        }
    }

    @Override
    public ObjectNode getConsultDocument(ObjectMapper mapper) {
        ObjectNode consult = mapper.createObjectNode();
        consult.put(NubeFactFields.OPERATION, OPERATION_CONSULT_COMPROBANTE);
        consult.put(NubeFactFields.VOUCHER_TYPE, getComprobanteType());
        consult.put(NubeFactFields.SERIES, getSeries());
        consult.put(NubeFactFields.NUMBER, getNumber());
        return consult;
    }

    /**
     * Comunicación de baja (generar_anulacion) for the comprobante.
     */
    @Override
    public ObjectNode getCancelDocument(ObjectMapper mapper, String reason) {
        ObjectNode anulacion = mapper.createObjectNode();
        anulacion.put(NubeFactFields.OPERATION, OPERATION_GENERATE_VOID);
        anulacion.put(NubeFactFields.VOUCHER_TYPE, getComprobanteType());
        anulacion.put(NubeFactFields.SERIES, getSeries());
        anulacion.put(NubeFactFields.NUMBER, getNumber());
        anulacion.put(NubeFactFields.REASON, Util.isEmpty(reason, true) ? "ANULACION" : reason);
        return anulacion;
    }

    @Override
    public boolean isValidForTransactionType(String transactionType) {
        return DocumentBuilder.isValidForComprobante(transactionType);
    }

    protected BigDecimal scale(BigDecimal amount) {
        return Optional.ofNullable(amount).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
