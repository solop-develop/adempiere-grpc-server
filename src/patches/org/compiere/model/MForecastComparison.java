package org.compiere.model;
import org.adempiere.core.domains.models.X_M_ForecastComparison;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MForecastComparison extends X_M_ForecastComparison {


    public MForecastComparison(Properties ctx, int M_ForecastComparison_ID, String trxName) {
        super(ctx, M_ForecastComparison_ID, trxName);
    }
    public MForecastComparison(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }



}