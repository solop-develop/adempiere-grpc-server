package org.compiere.model;
import org.adempiere.core.domains.models.X_C_PPBatchLine;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/**
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/338">https://github.com/solop-develop/adempiere-base/issues/338</a>
 */
public class MPPBatchLine extends X_C_PPBatchLine {

    public MPPBatchLine(Properties ctx, int C_PPBatchLine_ID, String trxName) {
        super(ctx, C_PPBatchLine_ID, trxName);
    }

    public MPPBatchLine(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {

        if (isManual() && newRecord) {
            MPayment payment = null;
            if (BigDecimal.ZERO.compareTo(getPayAmt()) == 0) {
                payment = (MPayment) getC_Payment();
                BigDecimal multiplier = BigDecimal.ONE;
                if (!payment.isReceipt()){
                    multiplier = multiplier.negate();
                }
                setPayAmt(payment.getPayAmt().multiply(multiplier));
                setTaxAmt(payment.getTaxAmt().multiply(multiplier));
                setDiscountAmt(payment.getDiscountAmt().multiply(multiplier));
            }
            if (getWithdrawal_ID() <= 0) {
                if (payment == null) {
                    payment = (MPayment) getC_Payment();
                }
                if(payment.getWithdrawal_ID() > 0) {
                    setWithdrawal_ID(payment.getWithdrawal_ID());
                }
                if(payment.getDeposit_ID() > 0) {
                    setDeposit_ID(payment.getDeposit_ID());
                }
            }
        }
        setTotalAmt(getPayAmt().subtract(getDiscountAmt().add(getTaxAmt()).add(getFeeAmt())));
        return super.beforeSave(newRecord);
    }

    @Override
    protected boolean afterSave(boolean newRecord, boolean success) {
        if (isManual() && newRecord && success) {
            MPaymentProcessorBatch batch = new MPaymentProcessorBatch(getCtx(), getC_PaymentProcessorBatch_ID(), get_TrxName());
            batch.updateTotals();
        }
        return super.afterSave(newRecord, success);
    }

    @Override
    protected boolean afterDelete(boolean success) {
        MPaymentProcessorBatch batch = new MPaymentProcessorBatch(getCtx(), getC_PaymentProcessorBatch_ID(), get_TrxName());
        batch.updateTotals();
        return super.afterDelete(success);
    }
}