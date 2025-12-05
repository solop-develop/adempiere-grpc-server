package org.solop.sp016.process.util;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ConsignmentOrderGrouping {
    BigDecimal maxAmount;
    BigDecimal usedAmount;
    BigDecimal deliveredAmount;
    int orderLineId;
    int clientSalesOrderLineId;
    int inventoryLineId;
    int orderId;
    int businessPartnerId;
    Timestamp dateDoc;
    int orgId;

    int productId;
    public ConsignmentOrderGrouping(BigDecimal maxAmount, int orderLineId, BigDecimal deliveredAmount) {
        this.maxAmount = maxAmount;
        this.orderLineId = orderLineId;
        this.usedAmount = BigDecimal.ZERO;
        this.deliveredAmount = deliveredAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public int getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(int orderLineId) {
        this.orderLineId = orderLineId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }
    public BigDecimal getDeliveredAmount() { return deliveredAmount; }

    public void setDeliveredAmount(BigDecimal deliveredAmount) { this.deliveredAmount = deliveredAmount; }
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }
    public Timestamp getDateDoc() {
        return dateDoc;
    }

    public void setDateDoc(Timestamp dateDoc) {
        this.dateDoc = dateDoc;
    }
    public int getBusinessPartnerId() {
        return businessPartnerId;
    }

    public void setBusinessPartnerId(int businessPartnerId) {
        this.businessPartnerId = businessPartnerId;
    }
    public int getOrgId() {
        return orgId;
    }

    public void setOrgId(int orgId) {
        this.orgId = orgId;
    }
    public int getInventoryLineId() {
        return inventoryLineId;
    }

    public void setInventoryLineId(int inventoryLineId) {
        this.inventoryLineId = inventoryLineId;
    }
    public int getClientSalesOrderLineId() {
        return clientSalesOrderLineId;
    }

    public void setClientSalesOrderLineId(int clientSalesOrderLineId) {
        this.clientSalesOrderLineId = clientSalesOrderLineId;
    }
}
