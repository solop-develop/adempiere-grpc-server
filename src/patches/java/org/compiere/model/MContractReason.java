package org.compiere.model;

import org.adempiere.core.domains.models.I_S_ContractReason;
import org.adempiere.core.domains.models.X_S_ContractReason;

import java.sql.ResultSet;
import java.util.Properties;

public class MContractReason extends X_S_ContractReason {
    public MContractReason(Properties ctx, int S_ContractReason_ID, String trxName) {
        super(ctx, S_ContractReason_ID, trxName);
    }

    public MContractReason(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }


    public static MContractReason getFinalReason(Properties ctx, int ad_client_id, String trxName) {
        final String whereClause = COLUMNNAME_IsFinalClose + "='Y' AND " + COLUMNNAME_AD_Client_ID + "=?";
        int reasonId = new Query(ctx, I_S_ContractReason.Table_Name, whereClause, trxName)
                .setOnlyActiveRecords(true)
                .setParameters(ad_client_id)
                .setClient_ID()
                .firstId();

        if (reasonId <= 0)
            return null;

        return new MContractReason(ctx, reasonId, trxName);
    }
}
