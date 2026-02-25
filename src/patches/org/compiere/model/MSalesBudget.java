package org.compiere.model;
import org.adempiere.core.domains.models.X_C_SalesBudget;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MSalesBudget extends X_C_SalesBudget {


    public MSalesBudget(Properties ctx, int C_SalesBudget_ID, String trxName) {
        super(ctx, C_SalesBudget_ID, trxName);
    }
    public MSalesBudget(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }



}