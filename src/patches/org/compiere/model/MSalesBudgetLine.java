package org.compiere.model;
import org.adempiere.core.domains.models.X_C_SalesBudgetLine;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MSalesBudgetLine extends X_C_SalesBudgetLine {


    public MSalesBudgetLine(Properties ctx, int C_SalesBudgetLine_ID, String trxName) {
        super(ctx, C_SalesBudgetLine_ID, trxName);
    }
    public MSalesBudgetLine(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }



}