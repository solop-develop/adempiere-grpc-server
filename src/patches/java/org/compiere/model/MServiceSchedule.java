package org.compiere.model;

import org.adempiere.core.domains.models.X_S_ServicePlan;
import org.adempiere.core.domains.models.X_S_ServiceSchedule;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DisplayType;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.eevolution.hr.model.MHREmployee;
import org.eevolution.hr.model.MHREmployeeType;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.Properties;

/**
 *    @author Gabriel Escalona
 */
public class MServiceSchedule extends X_S_ServiceSchedule {

public static int DAY_OF_WEEK_REFERENCE = 167;
    public MServiceSchedule(Properties ctx, int X_S_ServiceSchedule, String trxName) {
        super(ctx, X_S_ServiceSchedule, trxName);
    }
    public MServiceSchedule(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }


    /**
     * 	Before Save
     *	@param newRecord new
     *	@return true
     */
    @Override
    protected boolean beforeSave (boolean newRecord)
    {
        if (getS_ServicePlan_ID() <= 0) {
            throw new AdempiereException("@S_ServicePlan_ID@ @NotFound@");
        }
        BigDecimal plannedHours = BigDecimal.valueOf(TimeUtil.getHoursBetween(getTimeFrom(), getTimeTo()));
        setPlannedHours(plannedHours);
        if (getS_Resource_ID() > 0
                && (newRecord || is_ValueChanged(COLUMNNAME_TimeFrom) || is_ValueChanged(COLUMNNAME_TimeTo) || is_ValueChanged(COLUMNNAME_S_Resource_ID))) {
            String whereClause = " S_Resource_ID = ? " +
                " AND DayOfWeek = ? " +
                " AND S_ServiceSchedule_ID <> ? " +
                " AND CAST(TimeTo AS TIME) > CAST(? AS TIME) " +
                " AND CAST(TimeFrom AS TIME) < CAST(? AS TIME) ";
            MServiceSchedule overlappingSchedule = new Query(getCtx(), Table_Name, whereClause, get_TrxName())
                .setParameters(getS_Resource_ID(), getDayOfWeek(), getS_ServiceSchedule_ID(), getTimeFrom(), getTimeTo())
                .setOnlyActiveRecords(true)
                .first();
            MResource resource = (MResource) getS_Resource();
            if (overlappingSchedule != null) {
                X_S_ServicePlan servicePlan = (X_S_ServicePlan) getS_ServicePlan();
                String servicePlanDescription = servicePlan == null ? "" : servicePlan.getDescription();
                String dayOfWeek = MRefList.getListName(getCtx(), DAY_OF_WEEK_REFERENCE, getDayOfWeek());
                String timeFromFormated = DisplayType.getDateFormat(DisplayType.Time).format(getTimeFrom());
                String timeToFormated = DisplayType.getDateFormat(DisplayType.Time).format(getTimeTo());
                String partnerName= "";
                if (getC_BPartner_ID() > 0) {
                    MBPartner partner = (MBPartner) getC_BPartner();
                    partnerName = partner.getName();
                }
                String message = Msg.parseTranslation(getCtx(), "@WeeklyPlan.Overlap@");
                String errorMessage = MessageFormat.format(message,
                        resource.getName(),
                        dayOfWeek,
                        timeFromFormated,
                        timeToFormated,
                        servicePlanDescription,
                        partnerName
                );
                throw new AdempiereException(errorMessage);
            }
            whereClause = "S_Resource_ID = ? AND S_ServiceSchedule_ID <> ?";
            BigDecimal totalHours = new Query(getCtx(), Table_Name, whereClause, get_TrxName())
                .setParameters(getS_Resource_ID(), getS_ServiceSchedule_ID())
                .setOnlyActiveRecords(true)
                .sum("PlannedHours");
            totalHours = totalHours.add(plannedHours);
            MHREmployee employee = (MHREmployee) resource.getHR_Employee();
            MHREmployeeType employeeType = (MHREmployeeType) employee.getHR_EmployeeType();
            BigDecimal maxWeeklyHours = employeeType.getTotalDailyHours().multiply(BigDecimal.valueOf(5));
            if (totalHours.compareTo(maxWeeklyHours) > 0) {
                String maxHoursFormatted = DisplayType.getNumberFormat(DisplayType.Amount).format(maxWeeklyHours);
                String totalHoursFormatted = DisplayType.getNumberFormat(DisplayType.Amount).format(totalHours);
                String message = Msg.parseTranslation(getCtx(), "@WeeklyPlan.PlannedHoursExceeded@");
                String errorMessage = MessageFormat.format(message,
                        resource.getName(),
                        maxHoursFormatted,
                        totalHoursFormatted
                );
                throw new AdempiereException(errorMessage);
            }

        }
        return super.beforeSave(newRecord);
    }	//	beforeSave

}
