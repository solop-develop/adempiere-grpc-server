package com.solop.sp013.luy.support.documents.v2;

import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.documents.IFiscalDocumentLine;
import com.solop.sp013.core.documents.ReversalDocument;
import com.solop.sp013.luy.cfe.dto.invoicy.RetPerc;
import com.solop.sp013.luy.cfe.dto.invoicy.v2.CFEInvoiCyType;
import com.solop.sp013.luy.support.documents.DocumentBuilder;
import com.solop.sp013.luy.util.StringUtil;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Env;
import org.compiere.util.Util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Version 2 Implementation for e-Withholding (e-Resguardo)
 * Uses CFEInvoiCyType for the new provider API structure.
 *
 * @author Gabriel Escalona
 */
public class E_Withholding_v2 implements ICFEDocument_v2 {

    protected IFiscalDocument document;
    protected final CFEInvoiCyType ficalConvertedDocument;
    protected static final int CONVERSION_RATE_SCALE = 3;

    protected static final String RUT = "RUT";
    protected static final String CI = "CI";
    protected static final String OTHERS = "OTROS";
    protected static final String PASSPORT = "PASSPORT";
    protected static final String DNI = "DNI";

    public E_Withholding_v2() {
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
        invoicyReceptor.setRcpTipoDocRecep(getTaxPayerTypeId(document.getBusinessPartnerTaxType()));
        if (document.getBusinessPartnerTaxType() != null) {
            if (document.getBusinessPartnerTaxType().equalsIgnoreCase(RUT) || document.getBusinessPartnerTaxType().equalsIgnoreCase(CI)) {
                invoicyReceptor.setRcpDocRecep(document.getBusinessPartnerTaxId());
            } else {
                invoicyReceptor.setRcpDocRecep(document.getBusinessPartnerValue());
                if (document.getCountryCode() == null) {
                    throw new AdempiereException("@SP013.MandatoryCountryForBP@");
                }
            }
        }
        invoicyReceptor.setRcpCodPaisRecep(document.getCountryCode());
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
        totals.setTotMntTotRetenido(document.getGrandTotal());
        CFEInvoiCyType.Totales.RetencPercepTot withholdingTotal = new CFEInvoiCyType.Totales.RetencPercepTot();
        document.getFiscalDocumentLines().forEach(documentLine -> {
            CFEInvoiCyType.Totales.RetencPercepTot.RetencPercepTotItem withholdingDetail = new CFEInvoiCyType.Totales.RetencPercepTot.RetencPercepTotItem();
            withholdingDetail.setRetPercCodRet(documentLine.getWithholdingCode());
            withholdingDetail.setRetPercValRetPerc(documentLine.getLineTotalAmount().setScale(2, RoundingMode.HALF_UP));
            withholdingTotal.getRetencPercepTotItem().add(withholdingDetail);
        });
        totals.setRetencPercepTot(withholdingTotal);
        return totals;
    }

    protected CFEInvoiCyType.Detalle convertLines() {
        CFEInvoiCyType.Detalle detail = new CFEInvoiCyType.Detalle();
        List<CFEInvoiCyType.Detalle.Item> invoicyItems = detail.getItem();
        document.getFiscalDocumentLines()
                .forEach(documentLine -> {
                    CFEInvoiCyType.Detalle.Item invoicyItem = new CFEInvoiCyType.Detalle.Item();
                    if (documentLine.getLineTotalAmount().compareTo(Env.ZERO) < 0) {
                        invoicyItem.setIteIndFact(9);
                    }
                    CFEInvoiCyType.Detalle.Item.RetencPercep withholdingItem = new CFEInvoiCyType.Detalle.Item.RetencPercep();
                    CFEInvoiCyType.Detalle.Item.RetencPercep.RetencPercepItem withholdingItemDetail = getRetencPercepItem(documentLine);
                    withholdingItem.getRetencPercepItem().add(withholdingItemDetail);
                    invoicyItem.setRetencPercep(withholdingItem);
                    invoicyItems.add(invoicyItem);
                });
        return detail;
    }

    protected static CFEInvoiCyType.Detalle.Item.RetencPercep.RetencPercepItem getRetencPercepItem(IFiscalDocumentLine documentLine) {
        CFEInvoiCyType.Detalle.Item.RetencPercep.RetencPercepItem withholdingItemDetail = new CFEInvoiCyType.Detalle.Item.RetencPercep.RetencPercepItem();
        withholdingItemDetail.setIteRetPercCodRet(documentLine.getWithholdingCode());
        withholdingItemDetail.setIteRetPercTasa(documentLine.getWithholdingRate().setScale(CONVERSION_RATE_SCALE, RoundingMode.HALF_UP));
        withholdingItemDetail.setIteRetPercValRetPerc(documentLine.getLineTotalAmount().setScale(2, RoundingMode.HALF_UP).abs());
        withholdingItemDetail.setIteRetPerc(RetPerc.R);
        return withholdingItemDetail;
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
        if (document.hasReversalDocument()) {
            CFEInvoiCyType.Referencia.ReferenciaItem referenceItem = new CFEInvoiCyType.Referencia.ReferenciaItem();
            referenceItem.setRefNroLinRef(1);
            ReversalDocument reversalDocument = document.getFirstReversalDocument();
            String prefix = getPrefix(reversalDocument.getDocumentNo());
            if (!Util.isEmpty(prefix)) {
                referenceItem.setRefSerie(prefix);
            }
            referenceItem.setRefNroCFERef(getDocumentNo(reversalDocument.getDocumentNo()));
            referenceItem.setRefTpoDocRef(new BigInteger(reversalDocument.getTransactionType()));
            referenceItem.setRefFechaCFEref(convertTimestampToGregorianCalendar(reversalDocument.getDocumentDate()));
            referenceItem.setRefMontoRef(document.getGrandTotal());
            referenceItem.setRefTpoMonedaRef(com.solop.sp013.luy.cfe.dto.invoicy.TipMonType.valueOf(reversalDocument.getCurrencyCode()));
            referenceItem.setRefTipCambioRef(reversalDocument.getConversionRate().setScale(CONVERSION_RATE_SCALE, RoundingMode.HALF_UP));

            references.add(referenceItem);
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
        return DocumentBuilder.isValidForETicket(transactionType);
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
