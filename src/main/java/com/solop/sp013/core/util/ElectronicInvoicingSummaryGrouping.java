package com.solop.sp013.core.util;

import com.solop.sp013.core.model.X_SP013_ElectronicLineSummary;
import org.adempiere.core.domains.models.I_C_Commission;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.X_S_Contract;
import org.adempiere.core.domains.models.X_S_ContractLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCharge;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MProject;
import org.compiere.model.MProjectLine;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ElectronicInvoicingSummaryGrouping {

    private final MInvoice invoice;
    public static ElectronicInvoicingSummaryGrouping newInstance(MInvoice newInvoice) {
        return new ElectronicInvoicingSummaryGrouping(newInvoice);
    }

    public ElectronicInvoicingSummaryGrouping(MInvoice newInvoice) {
        invoice = newInvoice;
        invoiceLineGroups = new HashMap<>();
        groupDescription = new HashMap<>();
        seqNo = new AtomicInteger(0);
    }
    private final Map<String, List<MInvoiceLine>> invoiceLineGroups;
    private final Map<String, String> groupDescription;
    private final AtomicInteger seqNo;

    public void process() {
        List<MInvoiceLine> invoiceLines = Arrays.asList(invoice.getLines());
        String billingCriteria = invoice.get_ValueAsString("SP013_BillingCriteria");
        if (Util.isEmpty(billingCriteria) || billingCriteria.equals("M")) {
            return; //TODO: Validate if it can be null and just return
        }
        new Query(invoice.getCtx(), X_SP013_ElectronicLineSummary.Table_Name, X_SP013_ElectronicLineSummary.COLUMNNAME_C_Invoice_ID + "=?", invoice.get_TrxName())
                .setParameters(invoice.get_ID())
                .list()
                .forEach(po -> po.deleteEx(false));
        AtomicBoolean isTaxIncluded = new AtomicBoolean(false);
        MPriceList priceList = (MPriceList) invoice.getM_PriceList();
        if (priceList != null && priceList.get_ValueAsBoolean("isTaxIncluded")) {
            isTaxIncluded.set(true);
        }

        String whereClause = "EXISTS (" +
                "SELECT 1 " +
                "FROM " + I_C_Commission.Table_Name + " c " +
                "WHERE c.M_Product_ID=" + I_M_Product.Table_Name + "." + I_M_Product.COLUMNNAME_M_Product_ID + " " +
                "AND " + I_C_Commission.COLUMNNAME_IsActive + "='Y' " +
                "AND " + I_C_Commission.COLUMNNAME_DocBasisType + "='H'" +
                ")";
        Set<Integer> honoraryProductIds = new HashSet<>(new Query(invoice.getCtx(), I_M_Product.Table_Name, whereClause, invoice.get_TrxName()).getIDsAsList());

        invoiceLines.forEach(invoiceLine -> {
            String description = null;
            String key = "";
            if (honoraryProductIds.contains(invoiceLine.getM_Product_ID())) {
                key = "H" + invoiceLine.getM_Product_ID();
                key += invoiceLine.getC_Tax_ID();
                description = groupDescription.get(key);
                if (description == null) {
                    MProduct product = MProduct.get(invoice.getCtx(), invoiceLine.getM_Product_ID());
                    description = product.getName();
                    groupDescription.put(key, description);
                }
            } else if (billingCriteria.equals("L")){
                key = String.valueOf(invoiceLine.get_ID());
                key += String.valueOf(invoiceLine.getC_Tax_ID());
                description = groupDescription.get(key);
                if (description == null) {
                    MProduct product = invoiceLine.getProduct();
                    MCharge charge = invoiceLine.getCharge();
                    if (product != null && product.get_ID() > 0) {
                        description = Optional.ofNullable(product.getName()).orElse("") + " " + Optional.ofNullable(product.getDescription()).orElse("");
                    } else if (charge != null && charge.get_ID() > 0) {
                        description = Optional.ofNullable(charge.getName()).orElse("") + " " + Optional.ofNullable(charge.getDescription()).orElse("");
                    } else {
                        throw new AdempiereException("@M_Product@ - @C_Charge@ @not.found@");
                    }
                    groupDescription.put(key, description);
                }
            } else if (billingCriteria.equals("P")){
                int projectId = invoiceLine.getC_Project_ID();
                if (projectId <= 0) {
                    int projectLineId = invoiceLine.get_ValueAsInt(MProjectLine.COLUMNNAME_C_ProjectLine_ID);
                    if (projectLineId <= 0 && invoiceLine.getC_OrderLine_ID() >0) {

                        MOrderLine orderLine = (MOrderLine) invoiceLine.getC_OrderLine();
                        projectLineId = orderLine.get_ValueAsInt(MProjectLine.COLUMNNAME_C_ProjectLine_ID);
                    }
                    if (projectLineId > 0) {
                        MProjectLine projectLine = new MProjectLine(invoice.getCtx(), projectLineId, invoice.get_TrxName());
                        projectId = projectLine.getC_Project_ID();
                    }
                }
                key = String.valueOf(projectId);
                key += String.valueOf(invoiceLine.getC_Tax_ID());
                description = groupDescription.get(key);
                if (description == null && projectId > 0) {
                    MProject project = MProject.getById(invoice.getCtx(), projectId, invoice.get_TrxName());
                    description = Optional.ofNullable(project.getDescription()).orElse("");
                    groupDescription.put(key, description);
                }
            } else if (billingCriteria.equals("PP")){
                int projectLineId = invoiceLine.get_ValueAsInt(MProjectLine.COLUMNNAME_C_ProjectLine_ID);
                if (projectLineId <= 0 && invoiceLine.getC_OrderLine_ID() > 0) {
                    MOrderLine orderLine = (MOrderLine)invoiceLine.getC_OrderLine();
                    projectLineId = orderLine.get_ValueAsInt(MProjectLine.COLUMNNAME_C_ProjectLine_ID);
                }
                if (projectLineId > 0) {
                    MProjectLine projectLine = new MProjectLine(invoice.getCtx(), projectLineId, invoice.get_TrxName());
                    String projectLineType = projectLine.get_ValueAsString("ProjectLineType");
                    if (!Util.isEmpty(projectLineType) && projectLineType.equals("T")) {
                        projectLine = (MProjectLine) projectLine.getParent();
                    }
                    key = String.valueOf(projectLine.get_ID());
                    key += String.valueOf(invoiceLine.getC_Tax_ID());
                    description = groupDescription.get(key);
                    if (description == null) {
                        description = Optional.ofNullable(projectLine.getDescription()).orElse("");
                        groupDescription.put(key, description);
                    }
                }
            } else if (billingCriteria.equals("T")){
                int projectLineId = invoiceLine.get_ValueAsInt(MProjectLine.COLUMNNAME_C_ProjectLine_ID);
                if (projectLineId <= 0) {
                    MOrderLine orderLine = (MOrderLine)invoiceLine.getC_OrderLine();
                    if (orderLine != null) {
                        projectLineId = orderLine.get_ValueAsInt(MProjectLine.COLUMNNAME_C_ProjectLine_ID);
                    }
                }
                if (projectLineId > 0) {
                    MProjectLine projectLine = new MProjectLine(invoice.getCtx(), projectLineId, invoice.get_TrxName());
                    String projectLineType = projectLine.get_ValueAsString("ProjectLineType");
                    if (!Util.isEmpty(projectLineType) && projectLineType.equals("T")) {
                        key = String.valueOf(projectLineId);
                        key += String.valueOf(invoiceLine.getC_Tax_ID());
                        description = groupDescription.get(key);
                        if (description == null) {
                            description = Optional.ofNullable(projectLine.getDescription()).orElse("");
                            groupDescription.put(key, description);
                        }
                    }
                }
            } else if (billingCriteria.equals("I")){
                key = String.valueOf(invoice.get_ID());
                key += String.valueOf(invoiceLine.getC_Tax_ID());
            } else if (billingCriteria.equals("C")){
                int contractLineId = invoiceLine.get_ValueAsInt(X_S_ContractLine.COLUMNNAME_S_ContractLine_ID);
                if (contractLineId <= 0 && invoiceLine.getC_OrderLine_ID() >0) {
                    MOrderLine orderLine = (MOrderLine)invoiceLine.getC_OrderLine();
                    if (orderLine != null) {
                        contractLineId = orderLine.get_ValueAsInt(X_S_ContractLine.COLUMNNAME_S_ContractLine_ID);
                    }
                }
                if (contractLineId > 0) {
                    X_S_ContractLine contractLine = new X_S_ContractLine(invoice.getCtx(), contractLineId, invoice.get_TrxName());
                    X_S_Contract contract = (X_S_Contract) contractLine.getS_Contract();
                    key = String.valueOf(contract.get_ID());
                    key += String.valueOf(invoiceLine.getC_Tax_ID());
                    description = groupDescription.get(key);
                    if (description == null) {
                        description = Optional.ofNullable(contract.getDescription()).orElse("");//TODO: They Used Name but it does not have Centralized ID
                        groupDescription.put(key, description);
                    }
                }
            }
            updateLineGroupStorage(key, invoiceLine);
        });
        generateSummaryLines(isTaxIncluded.get());

    }

    private void generateSummaryLines(boolean isTaxIncluded){
        invoiceLineGroups.forEach((key, invoiceLineList) -> {
            X_SP013_ElectronicLineSummary summary = new X_SP013_ElectronicLineSummary(invoice.getCtx(), 0, invoice.get_TrxName());
            AtomicReference<BigDecimal> lineTotalAmt = new AtomicReference<>(BigDecimal.ZERO);
            AtomicBoolean isSameProduct = new AtomicBoolean(true);
            AtomicInteger maybeProductId = new AtomicInteger(0);
            invoiceLineList.forEach(invoiceLine -> {
                BigDecimal amount = isTaxIncluded ? invoiceLine.getLineTotalAmt() : invoiceLine.getLineNetAmt();

                lineTotalAmt.getAndUpdate(newAmount -> newAmount.add(amount));
                if (maybeProductId.get() <= 0) {
                    maybeProductId.set(invoiceLine.getM_Product_ID());
                }
                if (isSameProduct.get()){
                    if (maybeProductId.get() != invoiceLine.getM_Product_ID()) {
                        isSameProduct.set(false);
                    }
                }
            });
            summary.setC_Invoice_ID(invoice.get_ID());
            summary.setSeqNo(seqNo.addAndGet(10));
            if (isSameProduct.get()) {
                MProduct product = new MProduct(invoice.getCtx(), maybeProductId.get(), invoice.get_TrxName());
                String productValue = product.getValue();
                summary.setValue(Util.isEmpty(productValue) ? productValue : product.getUPC());
                summary.setSP013_ElectronicProductType(Util.isEmpty(productValue)
                        ? X_SP013_ElectronicLineSummary.SP013_ELECTRONICPRODUCTTYPE_EANCode
                        : X_SP013_ElectronicLineSummary.SP013_ELECTRONICPRODUCTTYPE_InternalCode);
            }
            summary.setQty(Env.ONE);
            summary.setPriceEntered(lineTotalAmt.get());
            summary.setLineTotalAmt(lineTotalAmt.get());
            summary.setDescription(groupDescription.get(key));


            summary.setC_Tax_ID(invoiceLineList.get(0).getC_Tax_ID());
            summary.saveEx();

        });
    }

    private void updateLineGroupStorage(String key, MInvoiceLine invoiceLine) {
        List<MInvoiceLine> invoiceLineGroup = invoiceLineGroups.getOrDefault(key, new ArrayList<>());
        if (invoiceLineGroup.isEmpty()){
            invoiceLineGroups.put(key, invoiceLineGroup);
        }
        invoiceLineGroup.add(invoiceLine);

    }
}
