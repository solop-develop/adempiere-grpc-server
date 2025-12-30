package org.compiere.model;
import org.adempiere.core.domains.models.X_R_Milestone;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/**
 *  @author Gabriel Escalona
 */
public class MMilestone extends X_R_Milestone {

    public MMilestone(Properties ctx, int R_Milestone_ID, String trxName) {
        super(ctx, R_Milestone_ID, trxName);
    }

    public MMilestone(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {

        return super.beforeSave(newRecord);
    }


    public void updateCompletedPercentage(){
        String whereClause = "EXISTS (SELECT 1 FROM R_RequestAction ra " +
                "WHERE ra.R_Request_ID = R_Request.R_Request_ID " +
                "AND ra.R_Milestone_ID = ?)";
        int totalIssues = new Query(getCtx(), MRequest.Table_Name, whereClause, get_TrxName())
            .setParameters(get_ID())
            .setOnlyActiveRecords(true)
            .count();
        whereClause = "EXISTS (SELECT 1 FROM R_RequestAction ra " +
                "INNER JOIN R_Status rs ON (rs.R_Status_ID = R_Request.R_Status_ID) " +
                "WHERE ra.R_Request_ID = R_Request.R_Request_ID " +
                "AND ra.R_Milestone_ID = ? " +
                "AND rs.IsClosed = 'Y')";
        int completedIssues = new Query(getCtx(), MRequest.Table_Name, whereClause, get_TrxName())
            .setParameters(get_ID())
            .setOnlyActiveRecords(true)
            .count();
        BigDecimal percentage = BigDecimal.ZERO;
        if (totalIssues > 0) {
            percentage = BigDecimal.valueOf(((double)completedIssues / totalIssues) * 100);
        }
        setPercentage(percentage);
    }
}