package org.compiere.model;
import org.adempiere.core.domains.models.X_R_Release;

import java.sql.ResultSet;
import java.util.Properties;

/**
 *  @author Gabriel Escalona
 */
public class MRelease extends X_R_Release {

    public MRelease(Properties ctx, int R_Release_ID, String trxName) {
        super(ctx, R_Release_ID, trxName);
    }

    public MRelease(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {

        return super.beforeSave(newRecord);
    }
}