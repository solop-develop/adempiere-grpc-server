package org.solop.sp016.process.util;

import java.math.BigDecimal;

public class ConsignmentOrderGrouping {
    BigDecimal maxAmount;
    BigDecimal usedAmount;



    BigDecimal deliveredAmount;
    int orderLineId;
    int orderId;

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
}
