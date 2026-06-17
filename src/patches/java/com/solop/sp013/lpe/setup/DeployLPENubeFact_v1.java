package com.solop.sp013.lpe.setup;

import com.solop.sp013.core.util.ElectronicInvoicingChanges;
import com.solop.sp013.lpe.support.documents.INubeFactDocument;
import com.solop.sp013.lpe.support.nubefact.NubeFact_v1;
import org.adempiere.core.domains.models.I_AD_AppRegistration;
import org.adempiere.core.domains.models.I_AD_AppRegistration_Para;
import org.adempiere.core.domains.models.I_AD_AppSupport;
import org.adempiere.core.domains.models.I_C_TaxGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.spin.model.MADAppRegistration;
import org.spin.model.MADAppRegistrationPara;
import org.spin.model.MADAppSupport;
import org.spin.util.ISetupDefinition;

import java.util.Properties;

/**
 * Setup for NubeFact (Peru): default app registration, comprobante types (tipo_de_comprobante),
 * item tax types (tipo_de_igv) and receiver document groups. Mirror of DeployLARAfipSdk_v1.
 *
 * @author Gabriel Escalona
 */
public class DeployLPENubeFact_v1 implements ISetupDefinition {

    private static final String DESCRIPTION = "(*Created from Setup Automatically*)";
    private static final String APPLICATION_TYPE = ElectronicInvoicingChanges.ElectronicInvoicing_ApplicationType;

    @Override
    public String doIt(Properties context, String transactionName) {
        createDefaultSender(context, transactionName);
        createTaxGroups(context, transactionName);
        createTransactionTypes(context, transactionName);
        createTaxTypes(context, transactionName);
        return "@AD_SetupDefinition_ID@ @Ok@";
    }

    /**
     * Create the NubeFact fiscal sender as app registration (route and token must be completed manually)
     */
    private void createDefaultSender(Properties context, String transactionName) {
        MADAppRegistration fiscalSender = new Query(context, I_AD_AppRegistration.Table_Name, "EXISTS(SELECT 1 FROM AD_AppSupport s "
                + "WHERE s.AD_AppSupport_ID = AD_AppRegistration.AD_AppSupport_ID "
                + "AND s.Classname = ?)", transactionName)
                .setParameters(NubeFact_v1.class.getName())
                .setClient_ID()
                .first();
        if(fiscalSender == null
                || fiscalSender.getAD_AppRegistration_ID() <= 0) {
            MADAppSupport applicationSupport = new Query(context, I_AD_AppSupport.Table_Name, "Classname = ?", transactionName)
                    .setParameters(NubeFact_v1.class.getName())
                    .first();
            if(applicationSupport == null
                    || applicationSupport.getAD_AppSupport_ID() <= 0) {
                throw new AdempiereException("@AD_AppSupport_ID@ @NotFound@");
            }
            fiscalSender = new MADAppRegistration(context, 0, transactionName);
            fiscalSender.setValue("EILPE");
            fiscalSender.setApplicationType(APPLICATION_TYPE);
            fiscalSender.setAD_AppSupport_ID(applicationSupport.getAD_AppSupport_ID());
            fiscalSender.setName("NubeFact (Peru)");
            fiscalSender.setDescription(DESCRIPTION);
            fiscalSender.setVersionNo("1.0");
            fiscalSender.setHost("https://api.nubefact.com");
            fiscalSender.setPort(0);
            fiscalSender.saveEx();
            //  Set Parameters (the route and token must be completed manually)
            new Query(context, I_AD_AppRegistration_Para.Table_Name, I_AD_AppRegistration_Para.COLUMNNAME_AD_AppRegistration_ID + " = ?", transactionName).setParameters(fiscalSender.getAD_AppRegistration_ID()).getIDsAsList().forEach(parameterId -> {
                MADAppRegistrationPara parameter = new MADAppRegistrationPara(context, parameterId, transactionName);
                if(parameter.getParameterName().equals(NubeFact_v1.NUBEFACT_ROUTE)) {
                    parameter.setParameterValue("fe6df4e6-4273-4e51-aa1f-7e742637a191");
                } else if(parameter.getParameterName().equals(NubeFact_v1.NUBEFACT_TOKEN)) {
                    parameter.setParameterValue("eae249eb164742ad9aff2fd0d515eb44d5085fa41d47473ea7e32ea1b8afa3a9");
                }
                parameter.saveEx();
            });
        }
    }

    /**
     * NubeFact receiver document groups (cliente_tipo_de_documento)
     */
    private void createTaxGroups(Properties context, String transactionName) {
        createTaxGroup(context, INubeFactDocument.RUC, transactionName);
        createTaxGroup(context, INubeFactDocument.DNI, transactionName);
        createTaxGroup(context, INubeFactDocument.FOREIGN_CARD, transactionName);
        createTaxGroup(context, INubeFactDocument.PASSPORT, transactionName);
        createTaxGroup(context, INubeFactDocument.NON_DOMICILED, transactionName);
    }

    /**
     * NubeFact item tax types (tipo_de_igv)
     */
    private void createTaxTypes(Properties context, String transactionName) {
        //  1 - Gravado - Operación Onerosa
        createTaxType(context, "1", "Gravado - Operación Onerosa", null, transactionName);
        //  8 - Exonerado - Operación Onerosa
        createTaxType(context, "8", "Exonerado - Operación Onerosa", null, transactionName);
        //  9 - Inafecto - Operación Onerosa
        createTaxType(context, "9", "Inafecto - Operación Onerosa", null, transactionName);
        //  16 - Exportación
        createTaxType(context, "16", "Exportación", null, transactionName);
        //  Link C_Tax to the NubeFact tax type by TaxIndicator (mirror of DeployLARAfipSdk_v1)
        DB.executeUpdate("UPDATE C_Tax SET SP013_TaxType_ID = (SELECT MAX(t.SP013_TaxType_ID) FROM SP013_TaxType t "
                + "WHERE t.Value = C_Tax.TaxIndicator AND t.AD_Client_ID = C_Tax.AD_Client_ID) "
                + "WHERE TaxIndicator IS NOT NULL AND SP013_TaxType_ID IS NULL "
                + "AND EXISTS(SELECT 1 FROM SP013_TaxType t WHERE t.Value = C_Tax.TaxIndicator AND t.AD_Client_ID = C_Tax.AD_Client_ID)", transactionName);
    }

    private void createTaxType(Properties context, String value, String name, String description, String transactionName) {
        MTable taxTypeTable = MTable.get(context, ElectronicInvoicingChanges.SP013_TaxType);
        PO taxType = new Query(context, ElectronicInvoicingChanges.SP013_TaxType, I_C_TaxGroup.COLUMNNAME_Value + " = ?", transactionName).setParameters(value).first();
        if(taxType == null) {
            taxType = taxTypeTable.getPO(0, transactionName);
        }
        //  Create or update with the Peru (NubeFact) data
        taxType.set_ValueOfColumn("Value", value);
        taxType.set_ValueOfColumn("Name", name);
        if(description != null) {
            taxType.set_ValueOfColumn("Description", description);
        }
        taxType.saveEx();
    }

    private void createTaxGroup(Properties context, String value, String transactionName) {
        MTable taxGroupTable = MTable.get(context, I_C_TaxGroup.Table_Name);
        PO group = new Query(context, I_C_TaxGroup.Table_Name, I_C_TaxGroup.COLUMNNAME_Value + " = ?", transactionName).setParameters(value).first();
        if(group == null) {
            group = taxGroupTable.getPO(0, transactionName);
        }
        //  Create or update with the Peru (NubeFact) data
        group.set_ValueOfColumn(I_C_TaxGroup.COLUMNNAME_Value, value);
        group.set_ValueOfColumn(I_C_TaxGroup.COLUMNNAME_Name, value);
        group.saveEx();
    }

    /**
     * NubeFact comprobante types (tipo_de_comprobante)
     */
    private void createTransactionTypes(Properties context, String transactionName) {
        //  Factura
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, INubeFactDocument.FACTURA, "Factura", transactionName);
        //  Boleta
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Invoice, INubeFactDocument.BOLETA, "Boleta", transactionName);
        //  Nota de Crédito
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Credit_Note, INubeFactDocument.NOTA_CREDITO, "Nota de Crédito", transactionName);
        //  Nota de Débito
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Debit_Note, INubeFactDocument.NOTA_DEBITO, "Nota de Débito", transactionName);
        //  Guía de Remisión Remitente (async)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_DeliveryNote, INubeFactDocument.GRE_REMITENTE, "Guía de Remisión Remitente", transactionName);
        //  Guía de Remisión Transportista (async)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_DeliveryNote, INubeFactDocument.GRE_TRANSPORTISTA, "Guía de Remisión Transportista", transactionName);
        //  Comprobante de Retención (compra)
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Withholding, INubeFactDocument.RETENCION, "Comprobante de Retención", transactionName);
        //  Comprobante de Percepción (venta) — mismo documento que retención, distinta naturaleza
        createTransactionType(context, ElectronicInvoicingChanges.SP013_FiscalDocumentType_Withholding, INubeFactDocument.PERCEPCION, "Comprobante de Percepción", transactionName);
    }

    private void createTransactionType(Properties context, String fiscalDocumentType, String transactionType, String name, String transactionName) {
        MTable transactionTypeTable = MTable.get(context, ElectronicInvoicingChanges.SP013_TransactionType);
        PO transactionTypePO = new Query(context, ElectronicInvoicingChanges.SP013_TransactionType, I_C_TaxGroup.COLUMNNAME_Value + " = ?", transactionName).setParameters(transactionType).first();
        if(transactionTypePO == null) {
            transactionTypePO = transactionTypeTable.getPO(0, transactionName);
        }
        //  Create or update with the Peru (NubeFact) data: a single client only holds one country's data
        transactionTypePO.set_ValueOfColumn(ElectronicInvoicingChanges.SP013_FiscalDocumentType, fiscalDocumentType);
        transactionTypePO.set_ValueOfColumn("Value", transactionType);
        transactionTypePO.set_ValueOfColumn("Name", name);
        transactionTypePO.saveEx();
    }
}
