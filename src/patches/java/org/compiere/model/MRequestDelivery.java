package org.compiere.model;
import org.adempiere.core.domains.models.X_R_RequestDelivery;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MRequestDelivery extends X_R_RequestDelivery {


    public MRequestDelivery(Properties ctx, int R_RequestDelivery_ID, String trxName) {
        super(ctx, R_RequestDelivery_ID, trxName);
    }
    public MRequestDelivery(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {

        if (newRecord && getR_Request_ID() > 0) {
            MRequest request = new MRequest(getCtx(), getR_Request_ID(), get_TrxName());
            Timestamp deliveryDate = getDateInternalDelivery();
            if (deliveryDate == null)
                deliveryDate = new Timestamp(System.currentTimeMillis());
            request.setDateInternalDelivery(deliveryDate);
            request.saveEx();
        }

        boolean rejectChanged = newRecord ? isRejected() : is_ValueChanged(COLUMNNAME_IsRejected);
        if (rejectChanged && getR_Request_ID() > 0) {
            MRequest request = new MRequest(getCtx(), getR_Request_ID(), get_TrxName());

            int qty = new Query(getCtx(), Table_Name,
                    "R_Request_ID=? AND IsRejected='Y' AND R_RequestDelivery_ID<>?", get_TrxName())
                    .setParameters(getR_Request_ID(), get_ID())
                    .count();
            if (isRejected())
                qty++;
            request.setQtyInternalReject(BigDecimal.valueOf(qty));

            if (isRejected()) {
                Timestamp rejectDate = getDateRejected();
                if (rejectDate == null)
                    rejectDate = new Timestamp(System.currentTimeMillis());
                request.setDateInternalReject(rejectDate);
            }
            request.saveEx();
        }

        return super.beforeSave(newRecord);
    }

}