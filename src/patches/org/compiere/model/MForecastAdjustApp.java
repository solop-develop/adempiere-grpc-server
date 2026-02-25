package org.compiere.model;
import org.adempiere.core.domains.models.X_M_ForecastAdjustApp;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MForecastAdjustApp extends X_M_ForecastAdjustApp {


    public MForecastAdjustApp(Properties ctx, int M_ForecastAdjustApp_ID, String trxName) {
        super(ctx, M_ForecastAdjustApp_ID, trxName);
    }
    public MForecastAdjustApp(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }



}