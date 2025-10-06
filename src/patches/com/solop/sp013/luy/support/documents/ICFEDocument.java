package com.solop.sp013.luy.support.documents;

import com.solop.sp013.core.documents.IFiscalDocument;
import com.solop.sp013.luy.cfe.dto.invoicy.CFEInvoiCyType;


/**
 * Document Contract
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public interface ICFEDocument {
    /* Document Types
     * 2 = RUT
     * 3 = CI
     * 4 = Otros
     * 5 = Pasaporte (todos los paises)
     * 6 = DNI (documento de identificacion de Argentina, Brasil, Chile o Paraguay)
     * */
    String RUT = "RUT";
    String CI = "CI";
    String OTHERS = "OTROS";
    String PASSPORT = "PASSPORT";
    String DNI = "DNI";

    String E_TICKET = "101";      // e-Ticket
    String E_TICKET_NC = "102";   // Nota de Credito e-Ticket
    String E_TICKET_ND = "103";   // Nota de Debito e-Ticket
    String E_FACTURARET = "111";     // e-Factura
    String E_FACTURA_NC = "112";  // Nota de Credito e-Factura
    String E_FACTURA_ND = "113";  // Nota de Debito e-Factura
    String E_REMITO = "181";      // e-Remito
    String E_RESGUARDO = "182";   // e-Resguardo
    String E_TICKET_VXCA = "131";     // e-Ticket Venta por Cuenta Ajena
    String E_TICKET_NC_VXCA = "132";  // Nota de Credito e-Ticket Venta por Cuenta Ajena
    String E_TICKET_ND_VXCA = "133";  // Nota de Debito e-Ticket Venta por Cuenta Ajena
    String E_FACTURA_VXCA = "141";    // e-Factura Venta por Cuenta Ajena
    String E_FACTURA_NC_VXCA = "142"; // Nota de Credito e-Factura Venta por Cuenta Ajena
    String E_FACTURA_ND_VXCA = "143"; // Nota de Debito e-Factura Venta por Cuenta Ajena
    String E_FACTURA_EXP = "121";   //  e-Factura Exportación
    String E_FACTURA_NC_EXP = "122";    //  e-Factura Nota de Crédito Exportación
    String E_FACTURA_ND_EXP = "123";    //  e-Factura Nota de Débito Exportación

    String E_TICKET_CONTINGENCY = "201";
    String E_TICKET_NC_CONTINGENCY = "202";
    String E_TICKET_ND_CONTINGENCY = "203";
    String E_FACTURA_CONTINGENCY = "211";
    String E_FACTURA_NC_CONTINGENCY = "212";
    String E_FACTURA_ND_CONTINGENCY = "213";
    String E_REMITO_CONTINGENCY = "281";
    String E_RESGUARDO_CONTINGENCY = "282";
    String E_FACTURA_EXP_CONTINGENCY = "221";
    String E_FACTURA_EXP_NC_CONTINGENCY = "222";
    String E_FACTURA_EXP_ND_CONTINGENCY = "223";
    String E_REMITO_EXP_CONTINGENCY = "224";
    String E_TICKET_VXCA_CONTINGENCY = "231";
    String E_TICKET_VXCA_NC_CONTINGENCY = "232";
    String E_TICKET_VXCA_ND_CONTINGENCY = "233";
    String E_FACTURA_VXCA_CONTINGENCY = "241";
    String E_FACTURA_VXCA_NC_CONTINGENCY = "242";
    String E_FACTURA_VXCA_ND_CONTINGENCY = "243";
    String E_BOLETA_ENTRADA_CONTINGENCY = "251";
    String E_BOLETA_ENTRADA_NC_CONTINGENCY = "252";
    String E_BOLETA_ENTRADA_ND_CONTINGENCY = "253";


    void setDocument(IFiscalDocument document);
    CFEInvoiCyType getConvertedDocument();
    boolean isValidForTransactionType(String transactionType);
}
