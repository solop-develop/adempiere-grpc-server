package org.compiere.model;
import org.adempiere.core.domains.models.X_R_Iteration;
import org.compiere.util.TimeUtil;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

/**
 *  @author Gabriel Escalona
 */
public class MIteration extends X_R_Iteration {

    public MIteration(Properties ctx, int R_Iteration, String trxName) {
        super(ctx, R_Iteration, trxName);
    }

    public MIteration(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {
        int timeQty = 0;
        String timeUnit = "";
        if (newRecord || is_ValueChanged(COLUMNNAME_DateFrom) || is_ValueChanged(COLUMNNAME_DateTo)) {
            if (isExactMonths(getDateFrom(), getDateTo())) {
                timeQty = getMonths(getDateFrom(), getDateTo());
                timeUnit = TIMEUNIT_Month;
            } else if (isExactWeeks(getDateFrom(), getDateTo())){
                timeQty = getWeeks(getDateFrom(), getDateTo());
                timeUnit = TIMEUNIT_Week;
            } else {
                timeQty = (int) ChronoUnit.DAYS.between(
                    getDateFrom().toLocalDateTime().toLocalDate(),
                    getDateTo().toLocalDateTime().toLocalDate()
                );
                timeUnit = TIMEUNIT_Day;
            }
            setDuration(timeQty);
            setTimeUnit(timeUnit);
        }

        return super.beforeSave(newRecord);
    }


    private boolean isExactMonths(Timestamp dateFrom, Timestamp dateTo) {
        LocalDate localDateFrom = dateFrom.toLocalDateTime().toLocalDate();
        LocalDate localDateTo = dateTo.toLocalDateTime().toLocalDate();
        // Calculate months between first days
        long months = ChronoUnit.MONTHS.between(
                localDateFrom.withDayOfMonth(1),
                localDateTo.withDayOfMonth(1)
        );
        // Check if plusMonths gives exact match
        LocalDate test = localDateFrom.plusMonths(months);
        return test.equals(localDateTo);
    }

    private int getMonths(Timestamp dateFrom, Timestamp dateTo){
        LocalDate localDateFrom = dateFrom.toLocalDateTime().toLocalDate();
        LocalDate localDateTo = dateTo.toLocalDateTime().toLocalDate();
        // Calculate months between first days
        long months = ChronoUnit.MONTHS.between(
                localDateFrom.withDayOfMonth(1),
                localDateTo.withDayOfMonth(1)
        );
        return (int) months;
    }

    private int getWeeks(Timestamp dateFrom, Timestamp dateTo){
        LocalDate localDateFrom = dateFrom.toLocalDateTime().toLocalDate();
        LocalDate localDateTo = dateTo.toLocalDateTime().toLocalDate();
        int daysBetween = (int) ChronoUnit.DAYS.between(localDateFrom, localDateTo);
        return daysBetween/7;
    }
    private boolean isExactWeeks(Timestamp ts1, Timestamp ts2) {
        LocalDate localDateFrom = ts1.toLocalDateTime().toLocalDate();
        LocalDate localDateTo = ts2.toLocalDateTime().toLocalDate();

        long daysBetween = ChronoUnit.DAYS.between(localDateFrom, localDateTo);
        return daysBetween % 7 == 0;

    }

    public void updateCompletedPercentage(){
        String whereClause = "EXISTS (SELECT 1 FROM R_RequestAction ra " +
                "WHERE ra.R_Request_ID = R_Request.R_Request_ID " +
                "AND ra.R_Iteration_ID = ?)";
        int totalIssues = new Query(getCtx(), MRequest.Table_Name, whereClause, get_TrxName())
            .setParameters(get_ID())
            .setOnlyActiveRecords(true)
            .count();
        whereClause = "EXISTS (SELECT 1 FROM R_RequestAction ra " +
                "INNER JOIN R_Status rs ON (rs.R_Status_ID = R_Request.R_Status_ID) " +
                "WHERE ra.R_Request_ID = R_Request.R_Request_ID " +
                "AND ra.R_Iteration_ID = ? " +
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