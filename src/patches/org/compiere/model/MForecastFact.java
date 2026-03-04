package org.compiere.model;
import org.adempiere.core.domains.models.X_M_ForecastFact;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MForecastFact extends X_M_ForecastFact {


    public MForecastFact(Properties ctx, int M_ForecastFact_ID, String trxName) {
        super(ctx, M_ForecastFact_ID, trxName);
    }
    public MForecastFact(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }



}