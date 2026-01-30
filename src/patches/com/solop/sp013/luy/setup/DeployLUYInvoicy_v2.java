package com.solop.sp013.luy.setup;

import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.luy.support.documents.ICFEDocument;
import com.solop.sp013.luy.support.invoicy.Invoicy_v2;
import org.adempiere.core.domains.models.I_AD_AppRegistration;
import org.adempiere.core.domains.models.I_AD_AppRegistration_Para;
import org.adempiere.core.domains.models.I_AD_AppSupport;
import org.adempiere.core.domains.models.I_C_TaxGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.spin.model.MADAppRegistration;
import org.spin.model.MADAppRegistrationPara;
import org.spin.model.MADAppSupport;
import org.spin.util.ISetupDefinition;

import java.util.Properties;

public class DeployLUYInvoicy_v2 implements ISetupDefinition {

    private static final String DESCRIPTION = "(*Created from Setup Automatically*)";
    private static final String APPLICATION_TYPE = ElectronicInvoicingChanges.ElectronicInvoicing_ApplicationType;

    @Override
    public String doIt(Properties context, String transactionName) {
        if (Env.getAD_Client_ID(context) == 0) {
            throw new AdempiereException("@AD_Client_ID@ = System");
        }
        //	App registration
        createDefaultSender(context, transactionName);
        createTaxGroups(context, transactionName);
        createTransactionTypes(context, transactionName);
        createTaxTypes(context, transactionName);
        return "@AD_SetupDefinition_ID@ @Ok@";
    }

    /**
     * Create notifiers as app registration
     * @param context
     * @param transactionName
     */
    private void createDefaultSender(Properties context, String transactionName) {
        MADAppRegistration fiscalSender = new Query(context, I_AD_AppRegistration.Table_Name, "EXISTS(SELECT 1 FROM AD_AppSupport s "
                + "WHERE s.AD_AppSupport_ID = AD_AppRegistration.AD_AppSupport_ID "
                + "AND s.Classname = ?)", transactionName)
                .setParameters(Invoicy_v2.class.getName())
                .setClient_ID()
                .first();
        if(fiscalSender == null
                || fiscalSender.getAD_AppRegistration_ID() <= 0) {
            MADAppSupport queueSupport = new Query(context, I_AD_AppSupport.Table_Name, "Classname = ?", transactionName)
                    .setParameters(Invoicy_v2.class.getName())
                    .first();
            if(queueSupport == null
                    || queueSupport.getAD_AppSupport_ID() <= 0) {
                throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
            }
            fiscalSender = new MADAppRegistration(context, 0, transactionName);
            fiscalSender.setValue("EILUY");
            fiscalSender.setApplicationType(APPLICATION_TYPE);
            fiscalSender.setAD_AppSupport_ID(queueSupport.getAD_AppSupport_ID());
            fiscalSender.setName("Invoicy (Uruguay) V2");
            fiscalSender.setDescription(DESCRIPTION);
            fiscalSender.setVersionNo("2.0");
            fiscalSender.setHost("https://openupsolutionspruebas.migrate.info/");
            fiscalSender.setPort(0);
            fiscalSender.saveEx();
            //  Set Parameters
            new Query(context, I_AD_AppRegistration_Para.Table_Name, I_AD_AppRegistration_Para.COLUMNNAME_AD_AppRegistration_ID + " = ?", transactionName).setParameters(fiscalSender.getAD_AppRegistration_ID()).getIDsAsList().forEach(parameterId -> {
                MADAppRegistrationPara parameter = new MADAppRegistrationPara(context, parameterId, transactionName);
                if(parameter.getParameterName().equals(Invoicy_v2.INVOICY_CK)) {
                    parameter.setParameterValue("uVY0GnRwUEgauYJj8AX7Sv9NpcfCO");
                } else if(parameter.getParameterName().equals(Invoicy_v2.INVOICY_PK)) {
                    parameter.setParameterValue("UxMJu9uAsUTsyTWkMQFQZw==");
                } else if(parameter.getParameterName().equals(Invoicy_v2.INVOICY_PUSH_INVOICE)) {
                    parameter.setParameterValue("/InvoiCy/aws_emissionfactura.aspx");
                } else if(parameter.getParameterName().equals(Invoicy_v2.INVOICY_GET_RECEIPT)) {
                    parameter.setParameterValue("/InvoiCy/aws_consultarecibidos.aspx");
                } else if(parameter.getParameterName().equals(Invoicy_v2.INVOICY_DOWNLOAD_RECEIPT)) {
                    parameter.setParameterValue("/InvoiCy/aws_descargarecibidos.aspx");
                } else if(parameter.getParameterName().equals(Invoicy_v2.CODE)) {
                    parameter.setParameterValue("662");
                }
                parameter.saveEx();
            });
        }
    }

    private void createTaxGroups(Properties context, String transactionName) {
        //  CI
        createTaxGroup(context, ICFEDocument.CI, transactionName);
        //  RUT
        createTaxGroup(context, ICFEDocument.RUT, transactionName);
        //  OTHERS
        createTaxGroup(context, ICFEDocument.OTHERS, transactionName);
        //  PASSPORT
        createTaxGroup(context, ICFEDocument.PASSPORT, transactionName);
        //  DNI
        createTaxGroup(context, ICFEDocument.DNI, transactionName);
    }

    private void createTaxTypes(Properties context, String transactionName) {
        //  1 - Exento de IVA
        createTaxType(context, "1", "Exento de IVA", null, transactionName);
        //  2 - Gravado a Tasa Mínima
        createTaxType(context, "2", "Gravado a Tasa Mínima", null, transactionName);
        //  3 - Gravado a Tasa Básica
        createTaxType(context, "3", "Gravado a Tasa Básica", null, transactionName);
        //  4 - Gravado a Otra Tasa / IVA sobre fictos
        createTaxType(context, "4", "Gravado a Otra Tasa / IVA sobre fictos", null, transactionName);
        //  5 - Entrega Gratuita
        createTaxType(context, "5", "Entrega Gratuita", null, transactionName);
        //  6 - Producto o Servicio no Facturable
        createTaxType(context, "6", "Producto o Servicio no Facturable", null, transactionName);
        //  7 - Producto o Servicio no Facturable Negativo
        createTaxType(context, "7", "Producto o Servicio no Facturable Negativo", null, transactionName);
        //  8 - Item a Rebajar en Remitos
        createTaxType(context, "8", "Item a Rebajar en Remitos", "Remito y Remito Exportación. En area de referencia se debe indicar el Nro de remito que ajusta", transactionName);
        //  9 - Item a Anular en Resguardo
        createTaxType(context, "9", "Item a Anular en Resguardo", "Solo para Resguardos", transactionName);
        //  10 - Exportación y Asimiladas
        createTaxType(context, "10", "Exportación y Asimiladas", null, transactionName);
        //  11 - Impuesto Percibido
        createTaxType(context, "11", "Impuesto Percibido", null, transactionName);
        //  12 - IVA en Suspenso
        createTaxType(context, "12", "IVA en Suspenso", null, transactionName);
        //  13 - Item Vendido por un No Contribuyente (Boleta)
        createTaxType(context, "13", "Item Vendido por un No Contribuyente (Boleta)", "Solo para e-Boleta y notas de corrección", transactionName);
        //  14 - Item Vendido por un Contribuyente IVA Mínimo (..) (Boleta)
        createTaxType(context, "14", "Item Vendido por un Contribuyente IVA Mínimo (..) (Boleta)", null, transactionName);
        //  15 - Item Vendido por un Contribuyente IMEBA (Boleta) (15)
        createTaxType(context, "15", "Item Vendido por un Contribuyente IMEBA (Boleta) (15)", null, transactionName);
        //  16 - Items Vendidos por Contribuyente obligación IVA Mínimo
        createTaxType(context, "16", "Items Vendidos por Contribuyente obligación IVA Mínimo", "IVA Mínimo, Monotributo, Monotributo MIDES, o si se trata de un CFE Venta por Cuenta Agena que el manadante tenga la obligación IVA Mínimo", transactionName);
        //  17 - Compra de Moneda Extranjera y reventa en Moneda Local
        createTaxType(context, "17", "Compra de Moneda Extranjera y reventa en Moneda Local", null, transactionName);
        //  Update Taxes
        DB.executeUpdate("UPDATE C_Tax SET SP013_TaxType_ID = (SELECT MAX(t.SP013_TaxType_ID) FROM SP013_TaxType t WHERE t.Value = C_Tax.TaxIndicator AND t.AD_Client_ID = C_Tax.AD_Client_ID) WHERE TaxIndicator IS NOT NULL AND SP013_TaxType_ID IS NULL AND EXISTS(SELECT 1 FROM SP013_TaxType t WHERE t.Value = C_Tax.TaxIndicator AND t.AD_Client_ID = C_Tax.AD_Client_ID)", transactionName);
    }

    private void createTaxType(Properties context, String value, String name, String description, String transactionName) {
        MTable taxTypeTable = MTable.get(context, ElectronicInvoicingChanges.SP013_TaxType);
        PO taxType = new Query(context, ElectronicInvoicingChanges.SP013_TaxType, I_C_TaxGroup.COLUMNNAME_Value + " = ?", transactionName).setParameters(value).first();
        if(taxType == null) {
            taxType = taxTypeTable.getPO(0, transactionName);
            taxType.set_ValueOfColumn("Value", value);
            taxType.set_ValueOfColumn("Name", name);
            if(description != null) {
                taxType.set_ValueOfColumn("Description", name);
            }
            taxType.saveEx();
        }
    }

    private void createTaxGroup(Properties context, String value, String transactionName) {
        MTable taxGroupTable = MTable.get(context, I_C_TaxGroup.Table_Name);
        PO group = new Query(context, I_C_TaxGroup.Table_Name, I_C_TaxGroup.COLUMNNAME_Value + " = ?", transactionName).setParameters(value).first();
        if(group == null) {
            group = taxGroupTable.getPO(0, transactionName);
            group.set_ValueOfColumn(I_C_TaxGroup.COLUMNNAME_Value, value);
            group.set_ValueOfColumn(I_C_TaxGroup.COLUMNNAME_Name, value);
            group.saveEx();
        }
    }

    private void createTransactionTypes(Properties context, String transactionName) {
        //  e-Ticket
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_TICKET, "e-Ticket", transactionName);
        //  Nota de Credito e-Ticket
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_TICKET_NC, "Nota de Crédito e-Ticket", transactionName);
        //  Nota de Debito e-Ticket
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_TICKET_ND, "Nota de Débito e-Ticket", transactionName);
        //  e-Ticket Venta por Cuenta Ajena
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_TICKET_VXCA, "e-Ticket Venta por Cuenta Ajena", transactionName);
        //  Nota de Credito e-Ticket Venta por Cuenta Ajena
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_TICKET_NC_VXCA, "Nota de Crédito e-Ticket Venta por Cuenta Ajena", transactionName);
        //  Nota de Debito e-Ticket Venta por Cuenta Ajena
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_TICKET_ND_VXCA, "Nota de Débito e-Ticket Venta por Cuenta Ajena", transactionName);
        //  e-Remito
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_DeliveryNote, ICFEDocument.E_REMITO, "e-Remito", transactionName);
        //  e-Resguardo
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Withholding, ICFEDocument.E_RESGUARDO, "e-Resguardo", transactionName);
        //  e-Factura
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_FACTURARET, "e-Factura", transactionName);
        //  Nota de Credito e-Factura
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_FACTURA_NC, "Nota de Crédito e-Factura", transactionName);
        //  Nota de Debito e-Factura
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_FACTURA_ND, "Nota de Débito e-Factura", transactionName);
        //  e-Factura Venta por Cuenta Ajena
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_FACTURA_VXCA, "e-Factura Venta por Cuenta Ajena", transactionName);
        //  Nota de Credito e-Factura Venta por Cuenta Ajena
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_FACTURA_NC_VXCA, "Nota de Crédito e-Factura Venta por Cuenta Ajena", transactionName);
        //  Nota de Debito e-Factura Venta por Cuenta Ajena
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_FACTURA_ND_VXCA, "Nota de Débito e-Factura Venta por Cuenta Ajena", transactionName);
        //  e-Factura Exportación
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_FACTURA_EXP, "e-Factura Exportación", transactionName);
        //  e-Factura Nota de Crédito Exportación
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_FACTURA_NC_EXP, "e-Factura Nota de Crédito Exportación", transactionName);
        //  e-Factura Nota de Débito Exportación
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_FACTURA_ND_EXP, "e-Factura Nota de Débito Exportación", transactionName);

        //Contingency Documents

        //  e-Ticket (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_TICKET_CONTINGENCY, "e-Ticket (Contingencia)", transactionName);
        //  Nota de Credito e-Ticket (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_TICKET_NC_CONTINGENCY, "Nota de Crédito e-Ticket (Contingencia)", transactionName);
        //  Nota de Debito e-Ticket (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_TICKET_ND_CONTINGENCY, "Nota de Débito e-Ticket (Contingencia)", transactionName);
        //  e-Ticket Venta por Cuenta Ajena (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_TICKET_VXCA_CONTINGENCY, "e-Ticket Venta por Cuenta Ajena (Contingencia)", transactionName);
        //  Nota de Credito e-Ticket Venta por Cuenta Ajena (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_TICKET_VXCA_NC_CONTINGENCY, "NC e-Ticket Venta por Cuenta Ajena  (Contingencia)", transactionName);
        //  Nota de Debito e-Ticket Venta por Cuenta Ajena (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_TICKET_VXCA_ND_CONTINGENCY, "ND e-Ticket Venta por Cuenta Ajena (Contingencia)", transactionName);
        //  e-Remito (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_DeliveryNote, ICFEDocument.E_REMITO_CONTINGENCY, "e-Remito (Contingencia)", transactionName);
        //  e-Resguardo (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Withholding, ICFEDocument.E_RESGUARDO_CONTINGENCY, "e-Resguardo (Contingencia)", transactionName);
        //  e-Factura (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_FACTURA_CONTINGENCY, "e-Factura (Contingencia)", transactionName);
        //  Nota de Credito e-Factura (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_FACTURA_NC_CONTINGENCY, "Nota de Crédito e-Factura (Contingencia)", transactionName);
        //  Nota de Debito e-Factura (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_FACTURA_ND_CONTINGENCY, "Nota de Débito e-Factura (Contingencia)", transactionName);
        //  e-Factura Venta por Cuenta Ajena (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_FACTURA_VXCA_CONTINGENCY, "e-Factura Venta por Cuenta Ajena (Contingencia)", transactionName);
        //  Nota de Credito e-Factura Venta por Cuenta Ajena (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_FACTURA_VXCA_NC_CONTINGENCY, "NC e-Factura Venta por Cuenta Ajena (Contingencia)", transactionName);
        //  Nota de Debito e-Factura Venta por Cuenta Ajena (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_FACTURA_VXCA_ND_CONTINGENCY, "ND e-Factura Venta por Cuenta Ajena (Contingencia)", transactionName);
        //  e-Factura Exportación (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, ICFEDocument.E_FACTURA_EXP_CONTINGENCY, "e-Factura Exportación (Contingencia)", transactionName);
        //  e-Factura Nota de Crédito Exportación (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, ICFEDocument.E_FACTURA_EXP_NC_CONTINGENCY, "e-Factura Nota de Crédito Exportación (Contingencia)", transactionName);
        //  e-Factura Nota de Débito Exportación (Contingencia)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, ICFEDocument.E_FACTURA_EXP_ND_CONTINGENCY, "e-Factura Nota de Débito Exportación (Contingencia)", transactionName);

    }

    private void createTransactionType(Properties context, String fiscalDocumentType, String transactionType, String name, String transactionName) {
        MTable taxGroupTable = MTable.get(context, ElectronicInvoicingChanges.SP013_TransactionType);
        PO transactionTypePO = new Query(context, ElectronicInvoicingChanges.SP013_TransactionType, I_C_TaxGroup.COLUMNNAME_Value + " = ?", transactionName).setParameters(transactionType).first();
        if(transactionTypePO == null) {
            transactionTypePO = taxGroupTable.getPO(0, transactionName);
            transactionTypePO.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_FiscalDocumentType, fiscalDocumentType);
            transactionTypePO.set_ValueOfColumn("Value", transactionType);
            transactionTypePO.set_ValueOfColumn("Name", name);
            transactionTypePO.saveEx();
        }
    }
}
