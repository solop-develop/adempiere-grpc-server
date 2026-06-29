/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.sp007.util;

import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_S_ResourceAssignment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClientInfo;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MRequest;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MResourceType;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Optional;
import java.util.Properties;

/**
 * Add here all changes for core and static methods of Time Control based on Sales
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class TimeSheetControl {

	/**
	 * Update user and change the last record time
	 * @param resourceAssignment
	 */
	public static void updateSummaryAssignment(MResourceAssignment resourceAssignment) {
		if(resourceAssignment == null) {
			return;
		}
		MUser user = new MUser(resourceAssignment.getCtx(), resourceAssignment.getCreatedBy(), resourceAssignment.get_TrxName());
		user.set_ValueOfColumn(TimeControlChanges.SP007_LastRecordTime, resourceAssignment.getAssignDateFrom());
		user.saveEx();
		//	Create or Update Parent
		AssignmentSummary.newInstance(resourceAssignment).recordSummary();
	}

	/**
	 * Validate if the amount of resource assignment is greather or equals to Resource Type Amount
	 * @param resourceAssignment
	 */
	public static void validateQuantityFromResourceType(MResourceAssignment resourceAssignment) {
		if(resourceAssignment == null) {
			return;
		}
		MResource resource = MResource.get(resourceAssignment.getCtx(), resourceAssignment.getS_Resource_ID());
		MResourceType resourceType = MResourceType.get(resourceAssignment.getCtx(), resource.getS_ResourceType_ID());
		BigDecimal minimumAmount = Optional.ofNullable((BigDecimal) resourceType.get_Value(TimeControlChanges.SP007_MinimumAmount)).orElse(Env.ZERO);
		if(Optional.ofNullable(resourceAssignment.getQty()).orElse(Env.ZERO).compareTo(minimumAmount) < 0) {
			throw new AdempiereException("@SP007.MinimumAmountExceed@");
		}
		if(!isValidateTimeSheetForUser(resourceAssignment.getCtx(), resourceAssignment.getAD_Client_ID(), resourceAssignment.getAD_Org_ID(), resourceAssignment.getCreatedBy())) {
			return;
		}
		//	Validate previous record
		MOrgInfo orgInfo = MOrgInfo.get(resourceAssignment.getCtx(), resourceAssignment.getAD_Org_ID(), null);
		PO lastAssignmentSummaryNotValid = getFirstNotValidAssignmentSummary(resourceAssignment.getCtx(), resourceAssignment.getCreatedBy(), resourceAssignment.getAssignDateFrom());
		if(lastAssignmentSummaryNotValid != null) {
			Timestamp lastRecordTimeNotValid = (Timestamp) lastAssignmentSummaryNotValid.get_Value("DateDoc");
			throw new AdempiereException("@CSP02.PreviousRecordIsInvalid@: " + DisplayType.getDateFormat(DisplayType.Date).format(lastRecordTimeNotValid));
		}
		//	Validate previous date
		PO lastAssignmentSummary = getLastAssignmentSummary(resourceAssignment.getCtx(), resourceAssignment.getCreatedBy(), resourceAssignment.getAssignDateFrom());
		if(lastAssignmentSummary != null) {
			Timestamp lastRecordTime = (Timestamp) lastAssignmentSummary.get_Value("DateDoc");
			if(!lastAssignmentSummary.get_ValueAsBoolean("IsValid")) {
				throw new AdempiereException("@SP007.LastRecordIsInvalid@, @" + TimeControlChanges.SP007_LastRecordTime + "@: " + DisplayType.getDateFormat(DisplayType.Date).format(lastRecordTime));
			}
			//	Validate Previous Time
			boolean onlyBusinessDays = false;
			String onlyBusinessDaysAsString = orgInfo.get_ValueAsString(TimeControlChanges.SP007_OnlyBusinessDay);
			if(Util.isEmpty(onlyBusinessDaysAsString)) {
				onlyBusinessDays = MClientInfo.get(resourceAssignment.getCtx(), resourceAssignment.getAD_Client_ID()).get_ValueAsBoolean(TimeControlChanges.SP007_OnlyBusinessDay);
			} else {
				onlyBusinessDays = onlyBusinessDaysAsString.equals("Y");
			}
			int days = TimeUtil.getDaysBetween(lastRecordTime, resourceAssignment.getAssignDateFrom());
			if(onlyBusinessDays) {
				int[] includeDays = {
						Calendar.MONDAY,
						Calendar.TUESDAY,
						Calendar.WEDNESDAY,
						Calendar.THURSDAY,
						Calendar.FRIDAY
				};
				days = TimeUtil.getBusinessDaysBetween(lastRecordTime, resourceAssignment.getAssignDateFrom(), includeDays);
			}
			if(days > 1) {
				throw new AdempiereException("@SP007.IncorrectHourLoad@, @" + TimeControlChanges.SP007_LastRecordTime + "@: " + DisplayType.getDateFormat(DisplayType.Date).format(lastRecordTime));
			}
		}
	}
	
	public static void autoApproveTimeSheet(MResourceAssignment resourceAssignment) {
		MUser user = MUser.get(resourceAssignment.getCtx(), resourceAssignment.getCreatedBy());
		boolean autoApprove = false;
		String autoApproveFromUser = user.get_ValueAsString(TimeControlChanges.SP007_AutoApproveTime);
		if(autoApproveFromUser != null) {
			autoApprove = autoApproveFromUser.equals("Y");
		} else {
			int roleId = getApprovedRoleFromUser(resourceAssignment.getCtx(), resourceAssignment.getCreatedBy());
			autoApprove = roleId > 0;
		}
		if(autoApprove) {
			resourceAssignment.setIsConfirmed(autoApprove);
		}
	}

	private static boolean isValidateTimeSheetForUser(Properties context, int clientId, int organizationId, int userId) {
		MUser user = getUser(context, userId);
		String validateTimeFromUser = Optional.ofNullable(user.get_ValueAsString(TimeControlChanges.SP007_RecordTimeWorked)).orElse("N");
		if(!validateTimeFromUser.equals("Y")) {
			return false;
		}
		MClientInfo clientInfo = MClientInfo.get(context, clientId);
		MOrgInfo orgInfo = MOrgInfo.get(context, organizationId, null);
		BigDecimal daysGrace = Optional.ofNullable((BigDecimal) clientInfo.get_Value(TimeControlChanges.SP007_GraceDays)).orElse(Env.ZERO);
		if(daysGrace.compareTo(Env.ZERO) <= 0) {
			daysGrace = Optional.ofNullable((BigDecimal) orgInfo.get_Value(TimeControlChanges.SP007_GraceDays)).orElse(Env.ZERO);
		}
		if(daysGrace.compareTo(Env.ZERO) <= 0) {
			return false;
		}
		BigDecimal minimumTime = Optional.ofNullable((BigDecimal) clientInfo.get_Value(TimeControlChanges.SP007_MinimumTime)).orElse(Env.ZERO);
		if(minimumTime.compareTo(Env.ZERO) <= 0) {
			minimumTime = Optional.ofNullable((BigDecimal) orgInfo.get_Value(TimeControlChanges.SP007_MinimumTime)).orElse(Env.ZERO);
		}
		if(minimumTime.compareTo(Env.ZERO) <= 0) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validate if all time sheet was loaded
	 * @param context
	 * @param clientId
	 * @param organizationId
	 * @param roleId
	 * @param userId
	 * @return
	 */
	public static String validateTimeSheetFromUser(Properties context, int clientId, int organizationId, int roleId, int userId) {
		MClientInfo clientInfo = MClientInfo.get(context, clientId);
		MOrgInfo orgInfo = MOrgInfo.get(context, organizationId, null);
		BigDecimal daysGrace = Optional.ofNullable((BigDecimal) clientInfo.get_Value(TimeControlChanges.SP007_GraceDays)).orElse(Env.ZERO);
		if(daysGrace.compareTo(Env.ZERO) <= 0) {
			daysGrace = Optional.ofNullable((BigDecimal) orgInfo.get_Value(TimeControlChanges.SP007_GraceDays)).orElse(Env.ZERO);
		}
		if(daysGrace.compareTo(Env.ZERO) <= 0) {
			return null;
		}
		BigDecimal minimumTime = Optional.ofNullable((BigDecimal) clientInfo.get_Value(TimeControlChanges.SP007_MinimumTime)).orElse(Env.ZERO);
		if(minimumTime.compareTo(Env.ZERO) <= 0) {
			minimumTime = Optional.ofNullable((BigDecimal) orgInfo.get_Value(TimeControlChanges.SP007_MinimumTime)).orElse(Env.ZERO);
		}
		if(minimumTime.compareTo(Env.ZERO) <= 0) {
			return null;
		}
		MRole role = getRole(context, roleId);
		MUser user = getUser(context, userId);
		boolean validateTime = role.get_ValueAsBoolean(TimeControlChanges.SP007_RecordTimeWorked);
		String validateTimeFromUser = user.get_ValueAsString(TimeControlChanges.SP007_RecordTimeWorked);
		if(!Util.isEmpty(validateTimeFromUser)) {
			validateTime = validateTimeFromUser.equals("Y");
		}
		if(!validateTime) {
			return null;
		}
		Timestamp now = TimeUtil.getDay(System.currentTimeMillis());
		PO lastAssigmentSummary = getLastAssignmentSummary(context, userId, now);
		if(lastAssigmentSummary == null) {
			return "@SP007.NoLastRecordTime@";
		}
		Timestamp lastRecordTime = (Timestamp) lastAssigmentSummary.get_Value("DateDoc");
		boolean onlyBusinessDays = false;
		String onlyBusinessDaysAsString = orgInfo.get_ValueAsString(TimeControlChanges.SP007_OnlyBusinessDay);
		if(Util.isEmpty(onlyBusinessDaysAsString)) {
			onlyBusinessDays = MClientInfo.get(context, clientId).get_ValueAsBoolean(TimeControlChanges.SP007_OnlyBusinessDay);
		} else {
			onlyBusinessDays = onlyBusinessDaysAsString.equals("Y");
		}
		int[] includeDays = {
				Calendar.MONDAY,
				Calendar.TUESDAY,
				Calendar.WEDNESDAY,
				Calendar.THURSDAY,
				Calendar.FRIDAY
		};
		int days = TimeUtil.getDaysBetween(lastRecordTime, now);
		if(onlyBusinessDays) {
			days = TimeUtil.getBusinessDaysBetween(lastRecordTime, now, includeDays);
		}
		if(BigDecimal.valueOf(days).compareTo(daysGrace) > 0) {
			return "@SP007.GraceDaysExceeded@, @" + TimeControlChanges.SP007_LastRecordTime + "@: " + DisplayType.getDateFormat(DisplayType.Date).format(lastRecordTime);
		}
		PO lastAssignmentSummaryNotValid = getFirstNotValidAssignmentSummary(context, userId, now);
		if(lastAssignmentSummaryNotValid != null) {
			Timestamp lastRecordTimeNotValid = (Timestamp) lastAssignmentSummaryNotValid.get_Value("DateDoc");
			days = TimeUtil.getDaysBetween(lastRecordTimeNotValid, now);
			if(onlyBusinessDays) {
				days = TimeUtil.getBusinessDaysBetween(lastRecordTimeNotValid, now, includeDays);
			}
			if(BigDecimal.valueOf(days).compareTo(daysGrace) > 0) {
				return "@CSP02.PreviousRecordIsInvalid@: " + DisplayType.getDateFormat(DisplayType.Date).format(lastRecordTimeNotValid);
			}
		}
		return null;
	}
	
	private static MRole getRole(Properties context, int roleId) {
		return new Query(context, I_AD_Role.Table_Name, "AD_Role_ID = ?", null)
				.setParameters(roleId)
				.<MRole>first(); 
	}
	
	private static MUser getUser(Properties context, int userId) {
		return new Query(context, I_AD_User.Table_Name, "AD_User_ID = ?", null)
				.setParameters(userId)
				.<MUser>first(); 
	}

	private static PO getLastValidAssignmentSummary(Properties context, int userId, Timestamp assignmentDate) {
		return new Query(context, TimeControlChanges.SP007_AssignmentSummary, "AD_User_ID = ? AND DateDoc < ? AND IsValid = 'Y'", null)
				.setParameters(userId, assignmentDate)
				.setOrderBy("DateDoc Desc")
				.<MUser>first();
	}

	private static PO getFirstNotValidAssignmentSummary(Properties context, int userId, Timestamp assignmentDate) {
		return new Query(context, TimeControlChanges.SP007_AssignmentSummary, "AD_User_ID = ? AND DateDoc < ? AND IsValid = 'N'", null)
				.setParameters(userId, assignmentDate)
				.setOrderBy("DateDoc Asc")
				.<MUser>first();
	}

	private static PO getLastAssignmentSummary(Properties context, int userId, Timestamp assignmentDate) {
		return new Query(context, TimeControlChanges.SP007_AssignmentSummary, "AD_User_ID = ? AND DateDoc < ?", null)
				.setParameters(userId, assignmentDate)
				.setOrderBy("DateDoc Desc")
				.<MUser>first();
	}

	private static BigDecimal getTotalAssignment(Properties context, int userId, Timestamp from) {
		return new Query(context, I_S_ResourceAssignment.Table_Name, "CreatedBy = ? AND AssignDateFrom >= ?", null)
				.setClient_ID()
				.setParameters(userId, from)
				.aggregate(I_S_ResourceAssignment.COLUMNNAME_Qty, Query.AGGREGATE_SUM, BigDecimal.class);
	}

	private static BigDecimal getTotalAssignment(Properties context, int userId, Timestamp from, Timestamp to, String transactionName) {
		return new Query(context, I_S_ResourceAssignment.Table_Name, "CreatedBy = ? AND AssignDateFrom >= ? AND AssignDateFrom < ?", transactionName)
				.setClient_ID()
				.setParameters(userId, from, to)
				.aggregate(I_S_ResourceAssignment.COLUMNNAME_Qty, Query.AGGREGATE_SUM, BigDecimal.class);
	}
	
	/**
	 * Recalculate and persist R_Request.TimeSpent as the sum of Qty of all active
	 * Resource Assignments linked to the request (R_Request_ID).
	 * @param context
	 * @param requestId
	 * @param transactionName
	 */
	public static void updateRequestTimeSpent(Properties context, int requestId, String transactionName) {
		if(requestId <= 0) {
			return;
		}
		BigDecimal timeSpent = new Query(context, I_S_ResourceAssignment.Table_Name, "R_Request_ID = ? AND IsActive = 'Y'", transactionName)
				.setParameters(requestId)
				.sum(I_S_ResourceAssignment.COLUMNNAME_Qty);
		if(timeSpent == null) {
			timeSpent = Env.ZERO;
		}
		MRequest request = new MRequest(context, requestId, transactionName);
		if(request.get_ID() <= 0) {
			return;
		}
		request.setTimeSpent(timeSpent);
		request.saveEx();
	}

	private static int getApprovedRoleFromUser(Properties context, int userId) {
		return new Query(context, I_AD_Role.Table_Name, "SP007_AutoApproveTime = 'Y' AND EXISTS(SELECT 1 FROM AD_User_Roles ur WHERE ur.AD_Role_ID = AD_Role.AD_Role_ID AND ur.AD_User_ID = ?)", null)
				.setClient_ID()
				.setParameters(userId)
				.firstId();
	}
}
