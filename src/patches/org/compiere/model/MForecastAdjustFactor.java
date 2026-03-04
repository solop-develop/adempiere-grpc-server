package org.compiere.model;
import org.adempiere.core.domains.models.X_M_ForecastAdjustFactor;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MForecastAdjustFactor extends X_M_ForecastAdjustFactor {


    public MForecastAdjustFactor(Properties ctx, int M_ForecastAdjustFactor_ID, String trxName) {
        super(ctx, M_ForecastAdjustFactor_ID, trxName);
    }
    public MForecastAdjustFactor(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }



}