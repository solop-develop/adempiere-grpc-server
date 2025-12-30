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
                timeQty = TimeUtil.getMonthsBetween(getDateFrom(), getDateTo());
                timeUnit = TIMEUNIT_Month;
            } else if (isExactWeeks(getDateFrom(), getDateTo())){
                timeQty = TimeUtil.getWeeksBetween(getDateFrom(), getDateTo());
                timeUnit = TIMEUNIT_Week;
            } else {
                timeQty = TimeUtil.getDaysBetween(getDateFrom(), getDateTo(), true,0);
                timeUnit = TIMEUNIT_Day;
            }
            setDuration(timeQty);
            setTimeUnit(timeUnit);
        }

        return super.beforeSave(newRecord);
    }


    private boolean isExactMonths(Timestamp ts1, Timestamp ts2) {
        LocalDate ldt1 = ts1.toLocalDateTime().toLocalDate();
        LocalDate ldt2 = ts2.toLocalDateTime().toLocalDate();
        // Calculate months between first days
        long months = ChronoUnit.MONTHS.between(
                ldt1.withDayOfMonth(1),
                ldt2.withDayOfMonth(1)
        );
        // Check if plusMonths gives exact match
        LocalDate test = ldt1.plusMonths(months);
        return test.equals(ldt2);
    }
    private boolean isExactWeeks(Timestamp ts1, Timestamp ts2) {
        LocalDate date1 = ts1.toLocalDateTime().toLocalDate();
        LocalDate date2 = ts2.toLocalDateTime().toLocalDate();

        long daysBetween = ChronoUnit.DAYS.between(date1, date2);
        return daysBetween % 7 == 0;

    }

    public void updateCompletedPercentage(){
        String whereClause = "R_Iteration_ID = ?";
        int totalIssues = new Query(getCtx(), MRequest.Table_Name, whereClause, get_TrxName())
            .setParameters(get_ID())
            .setOnlyActiveRecords(true)
            .count();
        whereClause = "R_Iteration_ID = ? AND TaskStatus in ('D', 'C')";
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