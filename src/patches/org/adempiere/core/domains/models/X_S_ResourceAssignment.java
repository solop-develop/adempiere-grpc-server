/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.adempiere.core.domains.models;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for S_ResourceAssignment
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_S_ResourceAssignment extends PO implements I_S_ResourceAssignment, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260309L;

    /** Standard Constructor */
    public X_S_ResourceAssignment (Properties ctx, int S_ResourceAssignment_ID, String trxName)
    {
      super (ctx, S_ResourceAssignment_ID, trxName);
      /** if (S_ResourceAssignment_ID == 0)
        {
			setAssignDateFrom (new Timestamp( System.currentTimeMillis() ));
			setIsConfirmed (false);
			setIsManuallyModified (false);
// N
			setName (null);
			setS_ResourceAssignment_ID (0);
			setS_Resource_ID (0);
        } */
    }

    /** Load Constructor */
    public X_S_ResourceAssignment (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 1 - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_S_ResourceAssignment[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Assign From.
		@param AssignDateFrom 
		Assign resource from
	  */
	public void setAssignDateFrom (Timestamp AssignDateFrom)
	{
		set_ValueNoCheck (COLUMNNAME_AssignDateFrom, AssignDateFrom);
	}

	/** Get Assign From.
		@return Assign resource from
	  */
	public Timestamp getAssignDateFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_AssignDateFrom);
	}

	/** Set Assign To.
		@param AssignDateTo 
		Assign resource until
	  */
	public void setAssignDateTo (Timestamp AssignDateTo)
	{
		set_ValueNoCheck (COLUMNNAME_AssignDateTo, AssignDateTo);
	}

	/** Get Assign To.
		@return Assign resource until
	  */
	public Timestamp getAssignDateTo () 
	{
		return (Timestamp)get_Value(COLUMNNAME_AssignDateTo);
	}

	/** AssignmentSourceType AD_Reference_ID=54623 */
	public static final int ASSIGNMENTSOURCETYPE_AD_Reference_ID=54623;
	/** Plan = P */
	public static final String ASSIGNMENTSOURCETYPE_Plan = "P";
	/** Manual = M */
	public static final String ASSIGNMENTSOURCETYPE_Manual = "M";
	/** Substitution = S */
	public static final String ASSIGNMENTSOURCETYPE_Substitution = "S";
	/** Reassignment = R */
	public static final String ASSIGNMENTSOURCETYPE_Reassignment = "R";
	/** Set Assignment Source Type.
		@param AssignmentSourceType 
		How this assignment was originated: (P)lan, (M)anual, (S)ubstitution, (R)eassignment
	  */
	public void setAssignmentSourceType (String AssignmentSourceType)
	{

		set_Value (COLUMNNAME_AssignmentSourceType, AssignmentSourceType);
	}

	/** Get Assignment Source Type.
		@return How this assignment was originated: (P)lan, (M)anual, (S)ubstitution, (R)eassignment
	  */
	public String getAssignmentSourceType () 
	{
		return (String)get_Value(COLUMNNAME_AssignmentSourceType);
	}

	/** AssignmentStatus AD_Reference_ID=54624 */
	public static final int ASSIGNMENTSTATUS_AD_Reference_ID=54624;
	/** Planned = PL */
	public static final String ASSIGNMENTSTATUS_Planned = "PL";
	/** To Coordinate = PC */
	public static final String ASSIGNMENTSTATUS_ToCoordinate = "PC";
	/** In Progress = IP */
	public static final String ASSIGNMENTSTATUS_InProgress = "IP";
	/** Confirmed = CO */
	public static final String ASSIGNMENTSTATUS_Confirmed = "CO";
	/** Pending Auth = PA */
	public static final String ASSIGNMENTSTATUS_PendingAuth = "PA";
	/** Approved = AP */
	public static final String ASSIGNMENTSTATUS_Approved = "AP";
	/** Cancelled = CA */
	public static final String ASSIGNMENTSTATUS_Cancelled = "CA";
	/** Not Attended = NA */
	public static final String ASSIGNMENTSTATUS_NotAttended = "NA";
	/** Set Assignment Status.
		@param AssignmentStatus 
		Operational status: PL(Planned), PC(ToCoordinate), IP(InProgress), CO(Confirmed), PA(PendingAuth), AP(Approved), CA(Cancelled), NA(NotAttended)
	  */
	public void setAssignmentStatus (String AssignmentStatus)
	{

		set_Value (COLUMNNAME_AssignmentStatus, AssignmentStatus);
	}

	/** Get Assignment Status.
		@return Operational status: PL(Planned), PC(ToCoordinate), IP(InProgress), CO(Confirmed), PA(PendingAuth), AP(Approved), CA(Cancelled), NA(NotAttended)
	  */
	public String getAssignmentStatus () 
	{
		return (String)get_Value(COLUMNNAME_AssignmentStatus);
	}

	public I_C_Activity getC_Activity() throws RuntimeException
    {
		return (I_C_Activity)MTable.get(getCtx(), I_C_Activity.Table_Name)
			.getPO(getC_Activity_ID(), get_TrxName());	}

	/** Set Activity.
		@param C_Activity_ID 
		Business Activity
	  */
	public void setC_Activity_ID (int C_Activity_ID)
	{
		if (C_Activity_ID < 1) 
			set_Value (COLUMNNAME_C_Activity_ID, null);
		else 
			set_Value (COLUMNNAME_C_Activity_ID, Integer.valueOf(C_Activity_ID));
	}

	/** Get Activity.
		@return Business Activity
	  */
	public int getC_Activity_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Activity_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_BPartner getC_BPartner() throws RuntimeException
    {
		return (I_C_BPartner)MTable.get(getCtx(), I_C_BPartner.Table_Name)
			.getPO(getC_BPartner_ID(), get_TrxName());	}

	/** Set Business Partner .
		@param C_BPartner_ID 
		Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1) 
			set_Value (COLUMNNAME_C_BPartner_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner .
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_BPartner_Location getC_BPartner_Location() throws RuntimeException
    {
		return (I_C_BPartner_Location)MTable.get(getCtx(), I_C_BPartner_Location.Table_Name)
			.getPO(getC_BPartner_Location_ID(), get_TrxName());	}

	/** Set Partner Location.
		@param C_BPartner_Location_ID 
		Identifies the (ship to) address for this Business Partner
	  */
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID)
	{
		if (C_BPartner_Location_ID < 1) 
			set_Value (COLUMNNAME_C_BPartner_Location_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartner_Location_ID, Integer.valueOf(C_BPartner_Location_ID));
	}

	/** Get Partner Location.
		@return Identifies the (ship to) address for this Business Partner
	  */
	public int getC_BPartner_Location_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_Location_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Campaign getC_Campaign() throws RuntimeException
    {
		return (I_C_Campaign)MTable.get(getCtx(), I_C_Campaign.Table_Name)
			.getPO(getC_Campaign_ID(), get_TrxName());	}

	/** Set Campaign.
		@param C_Campaign_ID 
		Marketing Campaign
	  */
	public void setC_Campaign_ID (int C_Campaign_ID)
	{
		if (C_Campaign_ID < 1) 
			set_Value (COLUMNNAME_C_Campaign_ID, null);
		else 
			set_Value (COLUMNNAME_C_Campaign_ID, Integer.valueOf(C_Campaign_ID));
	}

	/** Get Campaign.
		@return Marketing Campaign
	  */
	public int getC_Campaign_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Campaign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Project getC_Project() throws RuntimeException
    {
		return (I_C_Project)MTable.get(getCtx(), I_C_Project.Table_Name)
			.getPO(getC_Project_ID(), get_TrxName());	}

	/** Set Project.
		@param C_Project_ID 
		Financial Project
	  */
	public void setC_Project_ID (int C_Project_ID)
	{
		if (C_Project_ID < 1) 
			set_Value (COLUMNNAME_C_Project_ID, null);
		else 
			set_Value (COLUMNNAME_C_Project_ID, Integer.valueOf(C_Project_ID));
	}

	/** Get Project.
		@return Financial Project
	  */
	public int getC_Project_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Project_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	public I_HR_Leave getHR_Leave() throws RuntimeException
    {
		return (I_HR_Leave)MTable.get(getCtx(), I_HR_Leave.Table_Name)
			.getPO(getHR_Leave_ID(), get_TrxName());	}

	/** Set Leave.
		@param HR_Leave_ID 
		The Leave Credit History of an Employee
	  */
	public void setHR_Leave_ID (int HR_Leave_ID)
	{
		if (HR_Leave_ID < 1) 
			set_Value (COLUMNNAME_HR_Leave_ID, null);
		else 
			set_Value (COLUMNNAME_HR_Leave_ID, Integer.valueOf(HR_Leave_ID));
	}

	/** Get Leave.
		@return The Leave Credit History of an Employee
	  */
	public int getHR_Leave_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HR_Leave_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Confirmed.
		@param IsConfirmed 
		Assignment is confirmed
	  */
	public void setIsConfirmed (boolean IsConfirmed)
	{
		set_ValueNoCheck (COLUMNNAME_IsConfirmed, Boolean.valueOf(IsConfirmed));
	}

	/** Get Confirmed.
		@return Assignment is confirmed
	  */
	public boolean isConfirmed () 
	{
		Object oo = get_Value(COLUMNNAME_IsConfirmed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Manually Modified.
		@param IsManuallyModified 
		When Y, this assignment is protected from automatic regeneration by plan changes
	  */
	public void setIsManuallyModified (boolean IsManuallyModified)
	{
		set_Value (COLUMNNAME_IsManuallyModified, Boolean.valueOf(IsManuallyModified));
	}

	/** Get Manually Modified.
		@return When Y, this assignment is protected from automatic regeneration by plan changes
	  */
	public boolean isManuallyModified () 
	{
		Object oo = get_Value(COLUMNNAME_IsManuallyModified);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	public I_S_Resource getPreviousResource() throws RuntimeException
    {
		return (I_S_Resource)MTable.get(getCtx(), I_S_Resource.Table_Name)
			.getPO(getPreviousResource_ID(), get_TrxName());	}

	/** Set Previous Resource.
		@param PreviousResource_ID 
		Resource that was assigned before the last modification. Audit trail.
	  */
	public void setPreviousResource_ID (int PreviousResource_ID)
	{
		if (PreviousResource_ID < 1) 
			set_Value (COLUMNNAME_PreviousResource_ID, null);
		else 
			set_Value (COLUMNNAME_PreviousResource_ID, Integer.valueOf(PreviousResource_ID));
	}

	/** Get Previous Resource.
		@return Resource that was assigned before the last modification. Audit trail.
	  */
	public int getPreviousResource_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PreviousResource_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Quantity.
		@param Qty 
		Quantity
	  */
	public void setQty (BigDecimal Qty)
	{
		set_ValueNoCheck (COLUMNNAME_Qty, Qty);
	}

	/** Get Quantity.
		@return Quantity
	  */
	public BigDecimal getQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Qty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Replan Reason.
		@param ReplanReason 
		Free text reason entered by coordinator when replanning this assignment
	  */
	public void setReplanReason (String ReplanReason)
	{
		set_Value (COLUMNNAME_ReplanReason, ReplanReason);
	}

	/** Get Replan Reason.
		@return Free text reason entered by coordinator when replanning this assignment
	  */
	public String getReplanReason () 
	{
		return (String)get_Value(COLUMNNAME_ReplanReason);
	}

	public I_R_Request getR_Request() throws RuntimeException
    {
		return (I_R_Request)MTable.get(getCtx(), I_R_Request.Table_Name)
			.getPO(getR_Request_ID(), get_TrxName());	}

	/** Set Request.
		@param R_Request_ID 
		Request from a Business Partner or Prospect
	  */
	public void setR_Request_ID (int R_Request_ID)
	{
		if (R_Request_ID < 1) 
			set_Value (COLUMNNAME_R_Request_ID, null);
		else 
			set_Value (COLUMNNAME_R_Request_ID, Integer.valueOf(R_Request_ID));
	}

	/** Get Request.
		@return Request from a Business Partner or Prospect
	  */
	public int getR_Request_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_Request_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_S_Contract getS_Contract() throws RuntimeException
    {
		return (I_S_Contract)MTable.get(getCtx(), I_S_Contract.Table_Name)
			.getPO(getS_Contract_ID(), get_TrxName());	}

	/** Set Contract.
		@param S_Contract_ID 
		Contract
	  */
	public void setS_Contract_ID (int S_Contract_ID)
	{
		if (S_Contract_ID < 1) 
			set_Value (COLUMNNAME_S_Contract_ID, null);
		else 
			set_Value (COLUMNNAME_S_Contract_ID, Integer.valueOf(S_Contract_ID));
	}

	/** Get Contract.
		@return Contract
	  */
	public int getS_Contract_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_Contract_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Resource Assignment.
		@param S_ResourceAssignment_ID 
		Resource Assignment
	  */
	public void setS_ResourceAssignment_ID (int S_ResourceAssignment_ID)
	{
		if (S_ResourceAssignment_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_S_ResourceAssignment_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_S_ResourceAssignment_ID, Integer.valueOf(S_ResourceAssignment_ID));
	}

	/** Get Resource Assignment.
		@return Resource Assignment
	  */
	public int getS_ResourceAssignment_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_ResourceAssignment_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_S_Resource getS_Resource() throws RuntimeException
    {
		return (I_S_Resource)MTable.get(getCtx(), I_S_Resource.Table_Name)
			.getPO(getS_Resource_ID(), get_TrxName());	}

	/** Set Resource.
		@param S_Resource_ID 
		Resource
	  */
	public void setS_Resource_ID (int S_Resource_ID)
	{
		if (S_Resource_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_S_Resource_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_S_Resource_ID, Integer.valueOf(S_Resource_ID));
	}

	/** Get Resource.
		@return Resource
	  */
	public int getS_Resource_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_Resource_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getS_Resource_ID()));
    }

	public I_S_Resource getS_Resource_Plan() throws RuntimeException
    {
		return (I_S_Resource)MTable.get(getCtx(), I_S_Resource.Table_Name)
			.getPO(getS_Resource_Plan_ID(), get_TrxName());	}

	/** Set Plan Resource.
		@param S_Resource_Plan_ID 
		Original titular resource as defined in the service plan. NEVER modified after creation.
	  */
	public void setS_Resource_Plan_ID (int S_Resource_Plan_ID)
	{
		if (S_Resource_Plan_ID < 1) 
			set_Value (COLUMNNAME_S_Resource_Plan_ID, null);
		else 
			set_Value (COLUMNNAME_S_Resource_Plan_ID, Integer.valueOf(S_Resource_Plan_ID));
	}

	/** Get Plan Resource.
		@return Original titular resource as defined in the service plan. NEVER modified after creation.
	  */
	public int getS_Resource_Plan_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_Resource_Plan_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_S_ServicePlan getS_ServicePlan() throws RuntimeException
    {
		return (I_S_ServicePlan)MTable.get(getCtx(), I_S_ServicePlan.Table_Name)
			.getPO(getS_ServicePlan_ID(), get_TrxName());	}

	/** Set Service Plan ID.
		@param S_ServicePlan_ID 
		Operational service plan
	  */
	public void setS_ServicePlan_ID (int S_ServicePlan_ID)
	{
		if (S_ServicePlan_ID < 1) 
			set_Value (COLUMNNAME_S_ServicePlan_ID, null);
		else 
			set_Value (COLUMNNAME_S_ServicePlan_ID, Integer.valueOf(S_ServicePlan_ID));
	}

	/** Get Service Plan ID.
		@return Operational service plan
	  */
	public int getS_ServicePlan_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_ServicePlan_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_S_ServiceSchedule getS_ServiceSchedule() throws RuntimeException
    {
		return (I_S_ServiceSchedule)MTable.get(getCtx(), I_S_ServiceSchedule.Table_Name)
			.getPO(getS_ServiceSchedule_ID(), get_TrxName());	}

	/** Set Service Schedule ID.
		@param S_ServiceSchedule_ID 
		Weekly visit pattern for a service plan
	  */
	public void setS_ServiceSchedule_ID (int S_ServiceSchedule_ID)
	{
		if (S_ServiceSchedule_ID < 1) 
			set_Value (COLUMNNAME_S_ServiceSchedule_ID, null);
		else 
			set_Value (COLUMNNAME_S_ServiceSchedule_ID, Integer.valueOf(S_ServiceSchedule_ID));
	}

	/** Get Service Schedule ID.
		@return Weekly visit pattern for a service plan
	  */
	public int getS_ServiceSchedule_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_ServiceSchedule_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_ElementValue getUser1() throws RuntimeException
    {
		return (I_C_ElementValue)MTable.get(getCtx(), I_C_ElementValue.Table_Name)
			.getPO(getUser1_ID(), get_TrxName());	}

	/** Set User List 1.
		@param User1_ID 
		User defined list element #1
	  */
	public void setUser1_ID (int User1_ID)
	{
		if (User1_ID < 1) 
			set_Value (COLUMNNAME_User1_ID, null);
		else 
			set_Value (COLUMNNAME_User1_ID, Integer.valueOf(User1_ID));
	}

	/** Get User List 1.
		@return User defined list element #1
	  */
	public int getUser1_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_User1_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_ElementValue getUser2() throws RuntimeException
    {
		return (I_C_ElementValue)MTable.get(getCtx(), I_C_ElementValue.Table_Name)
			.getPO(getUser2_ID(), get_TrxName());	}

	/** Set User List 2.
		@param User2_ID 
		User defined list element #2
	  */
	public void setUser2_ID (int User2_ID)
	{
		if (User2_ID < 1) 
			set_Value (COLUMNNAME_User2_ID, null);
		else 
			set_Value (COLUMNNAME_User2_ID, Integer.valueOf(User2_ID));
	}

	/** Get User List 2.
		@return User defined list element #2
	  */
	public int getUser2_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_User2_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_ElementValue getUser3() throws RuntimeException
    {
		return (I_C_ElementValue)MTable.get(getCtx(), I_C_ElementValue.Table_Name)
			.getPO(getUser3_ID(), get_TrxName());	}

	/** Set User List 3.
		@param User3_ID 
		User defined list element #3
	  */
	public void setUser3_ID (int User3_ID)
	{
		if (User3_ID < 1) 
			set_Value (COLUMNNAME_User3_ID, null);
		else 
			set_Value (COLUMNNAME_User3_ID, Integer.valueOf(User3_ID));
	}

	/** Get User List 3.
		@return User defined list element #3
	  */
	public int getUser3_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_User3_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_ElementValue getUser4() throws RuntimeException
    {
		return (I_C_ElementValue)MTable.get(getCtx(), I_C_ElementValue.Table_Name)
			.getPO(getUser4_ID(), get_TrxName());	}

	/** Set User List 4.
		@param User4_ID 
		User defined list element #4
	  */
	public void setUser4_ID (int User4_ID)
	{
		if (User4_ID < 1) 
			set_Value (COLUMNNAME_User4_ID, null);
		else 
			set_Value (COLUMNNAME_User4_ID, Integer.valueOf(User4_ID));
	}

	/** Get User List 4.
		@return User defined list element #4
	  */
	public int getUser4_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_User4_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Immutable Universally Unique Identifier.
		@param UUID 
		Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID)
	{
		set_Value (COLUMNNAME_UUID, UUID);
	}

	/** Get Immutable Universally Unique Identifier.
		@return Immutable Universally Unique Identifier
	  */
	public String getUUID () 
	{
		return (String)get_Value(COLUMNNAME_UUID);
	}
}