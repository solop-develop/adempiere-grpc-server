package org.compiere.model;
import org.adempiere.core.domains.models.X_C_PaymentProcessorSchedule;
import org.adempiere.exceptions.AdempiereException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/**
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 *	<a href="https://github.com/solop-develop/adempiere-base/issues/338">https://github.com/solop-develop/adempiere-base/issues/338</a>
 */
public class MPaymentProcessorSchedule extends X_C_PaymentProcessorSchedule {

    public MPaymentProcessorSchedule(Properties ctx, int C_PPBatchLine_ID, String trxName) {
        super(ctx, C_PPBatchLine_ID, trxName);
    }

    public MPaymentProcessorSchedule(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {
        MPaymentProcessorBatch batch = (MPaymentProcessorBatch) getC_PaymentProcessorBatch();
        String whereClause = "C_PaymentProcessorBatch_ID = ? AND C_PaymentProcessorSchedule_ID <> ?";
        BigDecimal scheduleAmount = new Query(getCtx(), Table_Name, whereClause, get_TrxName())
            .setParameters(getC_PaymentProcessorBatch_ID(), getC_PaymentProcessorSchedule_ID())
            .sum(COLUMNNAME_Amount);
        scheduleAmount = scheduleAmount.add(getAmount());
        if (scheduleAmount.compareTo(batch.getApprovalAmt()) > 0) {
            throw new AdempiereException("@Amount@ (" + scheduleAmount +") > @ApprovalAmt@ " + batch.getApprovalAmt() +")");
        }
        return super.beforeSave(newRecord);
    }
}