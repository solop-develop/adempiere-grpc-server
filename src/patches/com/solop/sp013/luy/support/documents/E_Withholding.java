package com.solop.sp013.luy.support.documents;

import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.core.documents.IFiscalDocumentLine;
import com.solop.sp013.core.documents.ReversalDocument;
import com.solop.sp013.luy.cfe.dto.invoicy.CFEInvoiCyType;
import com.solop.sp013.luy.cfe.dto.invoicy.RetPerc;
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

/**
 * Implementation for e-Ticket:
 * <li>Invoice</li>
 * <li>Credit Memo</li>
 * <li>Debit Memo</li>
 * <li>VxCA</li>
 * <li>NC-VxCA</li>
 * <li>NC-VxCA</li>
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class E_Withholding implements ICFEDocument {
    private IFiscalDocument document;
    private final CFEInvoiCyType ficalConvertedDocument;


    public E_Withholding() {
        ficalConvertedDocument = new CFEInvoiCyType();
    }

    private CFEInvoiCyType.IdDoc convertIdDocument() {
        CFEInvoiCyType.IdDoc invoicyIdDoc = new CFEInvoiCyType.IdDoc();
        invoicyIdDoc.setCFETipoCFE(new BigInteger(document.getTransactionType()));
        invoicyIdDoc.setCFEFchEmis(convertTimestampToGregorianCalendar(document.getDocumentDate()));
        if(document.getFiscalDocumentNo() != null) {
            invoicyIdDoc.setCFESerie(getPrefix(document.getFiscalDocumentNo()));
            invoicyIdDoc.setCFENro(getDocumentNo(document.getFiscalDocumentNo()));
        }
        if(document.isTaxIncluded()) {
            invoicyIdDoc.setCFEMntBruto(BigInteger.valueOf(1));
        } else {
            invoicyIdDoc.setCFEMntBruto(BigInteger.valueOf(0));
        }
        if(document.getPaymentRule().equals(IFiscalDocument.PaymentRule.CREDIT)) {
            invoicyIdDoc.setCFEFmaPago(BigInteger.valueOf(2));
            //  Due Date
            invoicyIdDoc.setCFEFchVenc(convertTimestampToGregorianCalendar(document.getDueDate()));
        } else {
            invoicyIdDoc.setCFEFmaPago(BigInteger.valueOf(1));
        }
        return invoicyIdDoc;
    }

    private CFEInvoiCyType.Emisor convertEmisor() {
        CFEInvoiCyType.Emisor invoicyEmisor = new CFEInvoiCyType.Emisor();
        invoicyEmisor.setEmiRznSoc(document.getOrganizationName());
        if(document.getOrganizationPhone() != null) {
            invoicyEmisor.setEmiTelefono(document.getOrganizationPhone());
        }
        if(document.getOrganizationEmail() != null) {
            invoicyEmisor.setEmiCorreoEmisor(document.getOrganizationEmail());
        }
        invoicyEmisor.setEmiSucursal(StringUtil.cutString(document.getOrganizationName(), 20));
        invoicyEmisor.setEmiDomFiscal(document.getOrganizationAddress1());
        if(Util.isEmpty(document.getOrganizationCityName(), true)) {
            throw new AdempiereException("@SP013.OrgCityMandatory@");
        }
        invoicyEmisor.setEmiCiudad(document.getOrganizationCityName());
        invoicyEmisor.setEmiDepartamento(document.getOrganizationRegionName());
        return invoicyEmisor;
    }

    private int getTaxPayerTypeId(String taxPayerType) {
        int taxPayerTypeId = 0;
        if(taxPayerType == null) {
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

    private CFEInvoiCyType.Receptor convertReceipt() {
        CFEInvoiCyType.Receptor invoicyReceptor = new CFEInvoiCyType.Receptor();
        //  Tax Group
        invoicyReceptor.setRcpTipoDocRecep(getTaxPayerTypeId(document.getBusinessPartnerTaxType()));
        if(document.getBusinessPartnerTaxType() != null) {
            if(document.getBusinessPartnerTaxType().equalsIgnoreCase(RUT) || document.getBusinessPartnerTaxType().equalsIgnoreCase(CI)) {
                invoicyReceptor.setRcpDocRecep(document.getBusinessPartnerTaxId());
            } else  {
                invoicyReceptor.setRcpDocRecep(document.getBusinessPartnerValue());
                if(document.getCountryCode() == null) {
                    throw new AdempiereException("@SP013.MandatoryCountryForBP@");
                }
            }
        }
        invoicyReceptor.setRcpCodPaisRecep(document.getCountryCode());
        //  TODO: Validate UI Limit
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

    private CFEInvoiCyType.Totales convertTotals() {
        CFEInvoiCyType.Totales totals = new CFEInvoiCyType.Totales();
        totals.setTotTpoMoneda(com.solop.sp013.luy.cfe.dto.invoicy.TipMonType.valueOf(document.getCurrencyCode()));
        totals.setTotTpoCambio(document.getCurrencyRate().setScale(3, RoundingMode.HALF_UP));
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

    private CFEInvoiCyType.Detalle convertLines() {
        CFEInvoiCyType.Detalle detail = new CFEInvoiCyType.Detalle();
        List<CFEInvoiCyType.Detalle.Item> invoicyItems = detail.getItem();
        document.getFiscalDocumentLines()
                .forEach(documentLine -> {
            CFEInvoiCyType.Detalle.Item invoicyItem = new CFEInvoiCyType.Detalle.Item();
            if(documentLine.getLineTotalAmount().compareTo(Env.ZERO) < 0) {
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

    private static CFEInvoiCyType.Detalle.Item.RetencPercep.RetencPercepItem getRetencPercepItem(IFiscalDocumentLine documentLine) {
        CFEInvoiCyType.Detalle.Item.RetencPercep.RetencPercepItem withholdingItemDetail = new CFEInvoiCyType.Detalle.Item.RetencPercep.RetencPercepItem();
        withholdingItemDetail.setIteRetPercCodRet(documentLine.getWithholdingCode());
        withholdingItemDetail.setIteRetPercTasa(documentLine.getWithholdingRate().toBigInteger());
//            withholdingItemDetail.setIteRetPercMntSujetoaRet(retPercResg.getMntSujetoaRet());
        withholdingItemDetail.setIteRetPercValRetPerc(documentLine.getLineTotalAmount().setScale(2, RoundingMode.HALF_UP).abs());
        withholdingItemDetail.setIteRetPerc(RetPerc.R);
        return withholdingItemDetail;
    }

    private CFEInvoiCyType.SubTotInfo convertSubtotalInfo() {
        CFEInvoiCyType.SubTotInfo subTotalInfo = new CFEInvoiCyType.SubTotInfo();

        return subTotalInfo;
    }

    private CFEInvoiCyType.DscRcgGlobal convertDescuentoGlobal() {
        CFEInvoiCyType.DscRcgGlobal descuentoGlobal = new CFEInvoiCyType.DscRcgGlobal();

        return descuentoGlobal;
    }

    private CFEInvoiCyType.MediosPago convertMediosDePago() {
        CFEInvoiCyType.MediosPago mediosDePago = new CFEInvoiCyType.MediosPago();

        return mediosDePago;
    }

    private CFEInvoiCyType.Referencia convertReferencia() {
        CFEInvoiCyType.Referencia reference = new CFEInvoiCyType.Referencia();
        List<CFEInvoiCyType.Referencia.ReferenciaItem> references = reference.getReferenciaItem();
        //  For References
        //  Credit and Debit Memo
        if(document.hasReversalDocument()) {
            CFEInvoiCyType.Referencia.ReferenciaItem referenceItem = new CFEInvoiCyType.Referencia.ReferenciaItem();
            referenceItem.setRefNroLinRef(1);
            ReversalDocument reversalDocument = document.getFirstReversalDocument();
            String prefix = getPrefix(reversalDocument.getDocumentNo());
            if(!Util.isEmpty(prefix)) {
                referenceItem.setRefSerie(prefix);
            }
            referenceItem.setRefNroCFERef(getDocumentNo(reversalDocument.getDocumentNo()));
            referenceItem.setRefTpoDocRef(new BigInteger(reversalDocument.getTransactionType()));
            referenceItem.setRefFechaCFEref(convertTimestampToGregorianCalendar(reversalDocument.getDocumentDate()));

            if (MSysConfig.getBooleanValue("INVOICY_USENEWDATA", false, Env.getAD_Client_ID(Env.getCtx()))){
                referenceItem.setRefTpoMonedaRef(reversalDocument.getCurrencyCode());
                referenceItem.setRefTipCambioRef(reversalDocument.getConversionRate());
                referenceItem.setRefTipCambioRef(BigDecimal.ONE);
                referenceItem.setRefTpoMonedaRef("UYU");
            }

            references.add(referenceItem);
        }
        return reference;
    }

    private String getPrefix(String documentNo) {
        return documentNo.replaceAll("[^A-Za-z]", "");
    }

    private BigInteger getDocumentNo(String documentNo) {
        String newDocumentNo = documentNo.replaceAll("[^0-9]", "");
        newDocumentNo = String.format("%1$" + 7 + "s", newDocumentNo).replace(" ", "0");
        return new BigInteger(newDocumentNo);
    }

    private String getFiscalComment() {
        StringBuilder documentNote = new StringBuilder();
        if(!Util.isEmpty(document.getDocumentNote(), true)) {
            documentNote.append(document.getDocumentNote());
        }
        if(!Util.isEmpty(document.getFiscalComment(), true)) {
            documentNote.append(Env.NL).append(document.getFiscalComment());

        }
        return documentNote.toString();
    }

    private CFEInvoiCyType.Mandante convertMandante() {
        CFEInvoiCyType.Mandante mandante = new CFEInvoiCyType.Mandante();

        return mandante;
    }

    private void convertDocument() {
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
        if(!fiscalComment.isEmpty()) {
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
    public CFEInvoiCyType getConvertedDocument() {
        return ficalConvertedDocument;
    }

    @Override
    public boolean isValidForTransactionType(String transactionType) {
        return DocumentBuilder.isValidForETicket(transactionType);
    }

    private XMLGregorianCalendar convertTimestampToGregorianCalendar(Timestamp timestamp) {
        try {
            GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
            cal.setTime(timestamp);
            XMLGregorianCalendar xgcal;
            xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH), DatatypeConstants.FIELD_UNDEFINED);
            return xgcal;
        } catch (DatatypeConfigurationException e) {
            throw new AdempiereException(e);
        }
    }
}
