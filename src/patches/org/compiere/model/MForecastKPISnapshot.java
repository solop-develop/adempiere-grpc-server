package org.compiere.model;
import org.adempiere.core.domains.models.X_M_ForecastKPISnapshot;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MForecastKPISnapshot extends X_M_ForecastKPISnapshot {


    public MForecastKPISnapshot(Properties ctx, int M_ForecastKPISnapshot_ID, String trxName) {
        super(ctx, M_ForecastKPISnapshot_ID, trxName);
    }
    public MForecastKPISnapshot(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }



}