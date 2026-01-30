package com.solop.sp013.luy.support.documents.v2;

import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.luy.cfe.dto.invoicy.v2.CFEInvoiCyType;
import com.solop.sp013.luy.support.documents.DocumentBuilder;
import com.solop.sp013.luy.util.StringUtil;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.Util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Version 2 Implementation for e-Invoice Document (e-Ticket and e-Factura)
 * Uses CFEInvoiCyType for the new provider API structure.
 *
 * Override specific convert methods to customize behavior for v2 API.
 *
 * @author Gabriel Escalona
 */
public class E_InvoiceDocument_v2 implements ICFEDocument_v2 {

    protected IFiscalDocument document;
    protected final CFEInvoiCyType ficalConvertedDocument;
    protected final int CONVERSION_RATE_SCALE = 3;

    // Document type constants from ICFEDocument
    protected static final String RUT = "RUT";
    protected static final String CI = "CI";
    protected static final String OTHERS = "OTROS";
    protected static final String PASSPORT = "PASSPORT";
    protected static final String DNI = "DNI";
    protected static final String E_FACTURARET = "111";

    public E_InvoiceDocument_v2() {
        ficalConvertedDocument = new CFEInvoiCyType();
    }

    protected CFEInvoiCyType.IdDoc convertIdDocument() {
        CFEInvoiCyType.IdDoc invoicyIdDoc = new CFEInvoiCyType.IdDoc();
        invoicyIdDoc.setCFETipoCFE(new BigInteger(document.getTransactionType()));
        invoicyIdDoc.setCFEFchEmis(convertTimestampToGregorianCalendar(document.getDocumentDate()));
        invoicyIdDoc.setCFEIdCompra(document.getPoReferenceNo());
        if (document.getFiscalDocumentNo() != null) {
            invoicyIdDoc.setCFESerie(getPrefix(document.getFiscalDocumentNo()));
            invoicyIdDoc.setCFENro(getDocumentNo(document.getFiscalDocumentNo()));
        }
        if (document.isTaxIncluded()) {
            invoicyIdDoc.setCFEMntBruto(BigInteger.valueOf(1));
        } else {
            invoicyIdDoc.setCFEMntBruto(BigInteger.valueOf(0));
        }
        if (document.getPaymentRule().equals(IFiscalDocument.PaymentRule.CREDIT)) {
            invoicyIdDoc.setCFEFmaPago(BigInteger.valueOf(2));
            invoicyIdDoc.setCFEFchVenc(convertTimestampToGregorianCalendar(document.getDueDate()));
        } else {
            invoicyIdDoc.setCFEFmaPago(BigInteger.valueOf(1));
        }
        if (DocumentBuilder.isValidForEInvoice(document.getTransactionType())) {
            invoicyIdDoc.setCFEImpFormato(BigInteger.valueOf(2));
        }
        return invoicyIdDoc;
    }

    protected CFEInvoiCyType.Emisor convertEmisor() {
        CFEInvoiCyType.Emisor invoicyEmisor = new CFEInvoiCyType.Emisor();
        invoicyEmisor.setEmiRznSoc(document.getOrganizationName());
        if (document.getOrganizationPhone() != null) {
            invoicyEmisor.setEmiTelefono(document.getOrganizationPhone());
        }
        if (document.getOrganizationEmail() != null) {
            invoicyEmisor.setEmiCorreoEmisor(document.getOrganizationEmail());
        }
        invoicyEmisor.setEmiSucursal(StringUtil.cutString(document.getOrganizationName(), 20));
        invoicyEmisor.setEmiDomFiscal(document.getOrganizationAddress1());
        if (Util.isEmpty(document.getOrganizationCityName(), true)) {
            throw new AdempiereException("@SP013.OrgCityMandatory@");
        }
        invoicyEmisor.setEmiCiudad(document.getOrganizationCityName());
        invoicyEmisor.setEmiDepartamento(document.getOrganizationRegionName());
        return invoicyEmisor;
    }

    protected int getTaxPayerTypeId(String taxPayerType) {
        int taxPayerTypeId = 0;
        if (taxPayerType == null) {
            return taxPayerTypeId;
        }
        switch (taxPayerType) {
            case RUT:
                taxPayerTypeId = 2;
                break;
            case CI:
                taxPayerTypeId = 3;
                break;
            case OTHERS:
                taxPayerTypeId = 4;
                break;
            case PASSPORT:
                taxPayerTypeId = 5;
                break;
            case DNI:
                taxPayerTypeId = 6;
                break;
        }
        return taxPayerTypeId;
    }

    protected CFEInvoiCyType.Receptor convertReceipt() {
        CFEInvoiCyType.Receptor invoicyReceptor = new CFEInvoiCyType.Receptor();

        if (document.getTransactionType().equalsIgnoreCase(E_FACTURARET) && !document.getBusinessPartnerTaxType().equalsIgnoreCase(RUT)) {
            throw new AdempiereException("@SP013.NoRUTForEFactura@");
        }

        if (document.getBusinessPartnerTaxType() != null) {
            if (document.getBusinessPartnerTaxId() != null) {
                invoicyReceptor.setRcpDocRecep(document.getBusinessPartnerTaxId());
                invoicyReceptor.setRcpTipoDocRecep(getTaxPayerTypeId(document.getBusinessPartnerTaxType()));
                invoicyReceptor.setRcpCodPaisRecep(document.getCountryCode());
            }
            if (!document.getBusinessPartnerTaxType().equalsIgnoreCase(RUT) && !document.getBusinessPartnerTaxType().equalsIgnoreCase(CI)) {
                if (document.getCountryCode() == null) {
                    throw new AdempiereException("@SP013.MandatoryCountryForBP@");
                }
            }
        }
        invoicyReceptor.setRcpRznSocRecep(document.getBusinessPartnerName());
        invoicyReceptor.setRcpDirRecep(document.getAddress1());
        invoicyReceptor.setRcpCiudadRecep(document.getCityName());
        invoicyReceptor.setRcpDeptoRecep(StringUtil.cutString(document.getRegionName(), 30));
        if (document.getPostalCode() != null) {
            invoicyReceptor.setRcpCP(document.getPostalCode());
        }
        invoicyReceptor.setRcpCorreoRecep(document.getEMail());
        invoicyReceptor.setRcpDirPaisRecep(document.getCountryName());
        invoicyReceptor.setRcpDstEntregaRecep(document.getDeliveryAddress());
        return invoicyReceptor;
    }

    protected CFEInvoiCyType.Totales convertTotals() {
        CFEInvoiCyType.Totales totals = new CFEInvoiCyType.Totales();
        totals.setTotTpoMoneda(com.solop.sp013.luy.cfe.dto.invoicy.TipMonType.valueOf(document.getCurrencyCode()));
        totals.setTotTpoCambio(document.getCurrencyRate().setScale(CONVERSION_RATE_SCALE, RoundingMode.HALF_UP));
        totals.setTotMntNoGrv(Env.ZERO);
        totals.setTotMntExpoyAsim(Env.ZERO);
        totals.setTotMntImpuestoPerc(Env.ZERO);
        totals.setTotMntNetoIvaTasaMin(Env.ZERO);
        totals.setTotMntNetoIVATasaBasica(Env.ZERO);
        totals.setTotMntNetoIVAOtra(Env.ZERO);
        totals.setTotMntIVATasaMin(Env.ZERO);
        totals.setTotMntIVATasaBasica(Env.ZERO);
        totals.setTotMntIVAOtra(Env.ZERO);
        totals.setTotMontoNF(Env.ZERO);
        document.getFiscalDocumentTaxes().forEach(tax -> {
            if (tax.getTaxValue().equals("1")) {
                totals.setTotMntNoGrv(totals.getTotMntNoGrv().add(tax.getTaxBaseAmount()));
            } else if (tax.getTaxValue().equals("2")) {
                totals.setTotMntNetoIvaTasaMin(totals.getTotMntNetoIvaTasaMin().add(tax.getTaxBaseAmount()));
                totals.setTotMntIVATasaMin(totals.getTotMntIVATasaMin().add(tax.getTaxAmount()));
            } else if (tax.getTaxValue().equals("3")) {
                totals.setTotMntNetoIVATasaBasica(totals.getTotMntNetoIVATasaBasica().add(tax.getTaxBaseAmount()));
                totals.setTotMntIVATasaBasica(totals.getTotMntIVATasaBasica().add(tax.getTaxAmount()));
            } else if (tax.getTaxValue().equals("10")) {
                totals.setTotMntExpoyAsim(totals.getTotMntExpoyAsim().add(tax.getTaxBaseAmount()));
            } else if (tax.getTaxValue().equals("6")) {
                totals.setTotMontoNF(totals.getTotMontoNF().add(tax.getTaxBaseAmount()));
            } else {
                totals.setTotMntNetoIVAOtra(totals.getTotMntNetoIVAOtra().add(tax.getTaxBaseAmount()));
                totals.setTotMntIVAOtra(totals.getTotMntIVAOtra().add(tax.getTaxAmount()));
            }
        });
        totals.setTotMntTotal(document.getGrandTotal());
        totals.setTotMntPagar(document.getGrandTotal());
        return totals;
    }

    protected CFEInvoiCyType.Detalle convertLines() {
        CFEInvoiCyType.Detalle detalle = new CFEInvoiCyType.Detalle();
        List<CFEInvoiCyType.Detalle.Item> invoicyItems = detalle.getItem();
        document.getFiscalDocumentLines()
                .forEach(documentLine -> {
                    CFEInvoiCyType.Detalle.Item invoicyItem = new CFEInvoiCyType.Detalle.Item();
                    CFEInvoiCyType.Detalle.Item.CodItem codItem = new CFEInvoiCyType.Detalle.Item.CodItem();
                    CFEInvoiCyType.Detalle.Item.CodItem.CodItemItem codItemItem = new CFEInvoiCyType.Detalle.Item.CodItem.CodItemItem();
                    codItemItem.setIteCodiTpoCod("INT1");
                    String productValue = documentLine.getProductValue();
                    if (Util.isEmpty(productValue, true)) {
                        productValue = "INT1";
                    }
                    codItemItem.setIteCodiCod(productValue);
                    codItem.getCodItemItem().add(codItemItem);
                    if (documentLine.getProductBarCode() != null) {
                        CFEInvoiCyType.Detalle.Item.CodItem.CodItemItem codItemItemBarcode = new CFEInvoiCyType.Detalle.Item.CodItem.CodItemItem();
                        codItemItemBarcode.setIteCodiTpoCod("EAN");
                        codItemItemBarcode.setIteCodiCod(documentLine.getProductBarCode());
                        codItem.getCodItemItem().add(codItemItemBarcode);
                    }
                    invoicyItem.setIteIndFact(new BigInteger(documentLine.getTaxIndicator()).intValue());
                    invoicyItem.setIteNomItem(documentLine.getProductName());
                    invoicyItem.setIteDscItem(documentLine.getLineDescription());
                    BigDecimal quantity = documentLine.getQuantity();
                    BigDecimal price = documentLine.getProductPrice();
                    BigDecimal discountRate = documentLine.getDiscount();
                    BigDecimal discountAmount = documentLine.getDiscountAmount();
                    BigDecimal priceList = documentLine.getProductPriceList();
                    if (Optional.of(documentLine.getLineTotalAmount()).orElse(Env.ZERO).compareTo(Env.ZERO) == 0) {
                        if (Optional.ofNullable(quantity).orElse(Env.ZERO).compareTo(Env.ZERO) == 0) {
                            quantity = Env.ONE;
                        }
                        if (Optional.ofNullable(price).orElse(Env.ZERO).compareTo(Env.ZERO) == 0) {
                            price = priceList;
                            discountRate = Env.ONEHUNDRED;
                            discountAmount = priceList;
                        }
                    }
                    invoicyItem.setIteCantidad(quantity);
                    invoicyItem.setIteUniMed(documentLine.getProductUnitOfMeasure());
                    invoicyItem.setItePrecioUnitario(price);
                    if (Optional.ofNullable(discountRate).orElse(Env.ZERO).compareTo(Env.ZERO) != 0) {
                        invoicyItem.setIteDescuentoPct(discountRate);
                        invoicyItem.setIteDescuentoMonto(discountAmount);
                        invoicyItem.setItePrecioUnitario(priceList);
                    }
                    invoicyItem.setIteRecargoPct(Env.ZERO);
                    invoicyItem.setIteRecargoMnt(Env.ZERO);
                    if (document.isTaxIncluded()) {
                        invoicyItem.setIteMontoItem(documentLine.getLineTotalAmount().abs());
                    } else {
                        invoicyItem.setIteMontoItem(documentLine.getLineNetAmount().abs());
                    }
                    invoicyItem.setCodItem(codItem);
                    invoicyItems.add(invoicyItem);
                });
        return detalle;
    }

    protected CFEInvoiCyType.SubTotInfo convertSubtotalInfo() {
        return new CFEInvoiCyType.SubTotInfo();
    }

    protected CFEInvoiCyType.DscRcgGlobal convertDescuentoGlobal() {
        return new CFEInvoiCyType.DscRcgGlobal();
    }

    protected CFEInvoiCyType.MediosPago convertMediosDePago() {
        return new CFEInvoiCyType.MediosPago();
    }

    protected CFEInvoiCyType.Referencia convertReferencia() {
        CFEInvoiCyType.Referencia reference = new CFEInvoiCyType.Referencia();
        List<CFEInvoiCyType.Referencia.ReferenciaItem> references = reference.getReferenciaItem();
        if (document.getDocumentType().equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note) ||
                document.getDocumentType().equals(ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note)) {
            if (document.hasReversalDocument()) {
                AtomicInteger referenceNoItem = new AtomicInteger(1);
                document.getFiscalReversalDocuments().forEach(reversalDocument -> {
                    CFEInvoiCyType.Referencia.ReferenciaItem referenceItem = new CFEInvoiCyType.Referencia.ReferenciaItem();
                    referenceItem.setRefNroLinRef(referenceNoItem.getAndIncrement());
                    String prefix = getPrefix(reversalDocument.getDocumentNo());
                    if (!Util.isEmpty(prefix)) {
                        referenceItem.setRefSerie(prefix);
                    }
                    referenceItem.setRefNroCFERef(getDocumentNo(reversalDocument.getDocumentNo()));
                    referenceItem.setRefTpoDocRef(new BigInteger(reversalDocument.getTransactionType()));
                    referenceItem.setRefFechaCFEref(convertTimestampToGregorianCalendar(reversalDocument.getDocumentDate()));
                    referenceItem.setRefTipCambioRef(reversalDocument.getConversionRate().setScale(CONVERSION_RATE_SCALE, RoundingMode.HALF_UP));
                    referenceItem.setRefTpoMonedaRef(reversalDocument.getCurrencyCode());

                    references.add(referenceItem);
                });
            } else {
                CFEInvoiCyType.Referencia.ReferenciaItem referenceItem = new CFEInvoiCyType.Referencia.ReferenciaItem();
                referenceItem.setRefNroLinRef(1);
                referenceItem.setRefIndGlobal(BigInteger.valueOf(1));
                referenceItem.setRefRazonRef(document.getDescription());
                references.add(referenceItem);
            }
        }
        return reference;
    }

    protected String getPrefix(String documentNo) {
        return documentNo.replaceAll("[^A-Za-z]", "");
    }

    protected BigInteger getDocumentNo(String documentNo) {
        String newDocumentNo = documentNo.replaceAll("[^0-9]", "");
        newDocumentNo = String.format("%1$" + 7 + "s", newDocumentNo).replace(" ", "0");
        return new BigInteger(newDocumentNo);
    }

    protected String getFiscalComment() {
        StringBuilder documentNote = new StringBuilder();
        if (!Util.isEmpty(document.getDocumentNote(), true)) {
            documentNote.append(document.getDocumentNote());
        }
        if (!Util.isEmpty(document.getFiscalComment(), true)) {
            documentNote.append(Env.NL).append(document.getFiscalComment());
        }
        return documentNote.toString();
    }

    protected CFEInvoiCyType.Mandante convertMandante() {
        return new CFEInvoiCyType.Mandante();
    }

    protected void convertDocument() {
        ficalConvertedDocument.setIdDoc(convertIdDocument());
        ficalConvertedDocument.setEmisor(convertEmisor());
        ficalConvertedDocument.setReceptor(convertReceipt());
        ficalConvertedDocument.setTotales(convertTotals());
        ficalConvertedDocument.setDetalle(convertLines());
        ficalConvertedDocument.setSubTotInfo(convertSubtotalInfo());
        ficalConvertedDocument.setDscRcgGlobal(convertDescuentoGlobal());
        ficalConvertedDocument.setMediosPago(convertMediosDePago());
        ficalConvertedDocument.setReferencia(convertReferencia());
        String fiscalComment = getFiscalComment();
        if (!fiscalComment.isEmpty()) {
            ficalConvertedDocument.getIdDoc().setCFEAdenda(fiscalComment);
        }
        ficalConvertedDocument.setMandante(convertMandante());
    }

    @Override
    public void setDocument(IFiscalDocument document) {
        this.document = document;
        convertDocument();
    }

    @Override
    public com.solop.sp013.luy.cfe.dto.invoicy.CFEInvoiCyType getConvertedDocument() {
        // Return null for v1 type - use getConvertedDocument_v2() instead
        return null;
    }

    @Override
    public CFEInvoiCyType getConvertedDocument_v2() {
        return ficalConvertedDocument;
    }

    @Override
    public boolean isValidForTransactionType(String transactionType) {
        return DocumentBuilder.isValidForETicket(transactionType) || DocumentBuilder.isValidForEInvoice(transactionType);
    }

    protected XMLGregorianCalendar convertTimestampToGregorianCalendar(Timestamp timestamp) {
        try {
            GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
            cal.setTime(timestamp);
            return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    DatatypeConstants.FIELD_UNDEFINED
            );
        } catch (DatatypeConfigurationException e) {
            throw new AdempiereException(e);
        }
    }
}
