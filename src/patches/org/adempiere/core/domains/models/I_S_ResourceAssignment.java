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
package org.adempiere.core.domains.models;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Interface for S_ResourceAssignment
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_S_ResourceAssignment 
{

    /** TableName=S_ResourceAssignment */
    public static final String Table_Name = "S_ResourceAssignment";

    /** AD_Table_ID=485 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 1 - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(1);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name AssignDateFrom */
    public static final String COLUMNNAME_AssignDateFrom = "AssignDateFrom";

	/** Set Assign From.
	  * Assign resource from
	  */
	public void setAssignDateFrom (Timestamp AssignDateFrom);

	/** Get Assign From.
	  * Assign resource from
	  */
	public Timestamp getAssignDateFrom();

    /** Column name AssignDateTo */
    public static final String COLUMNNAME_AssignDateTo = "AssignDateTo";

	/** Set Assign To.
	  * Assign resource until
	  */
	public void setAssignDateTo (Timestamp AssignDateTo);

	/** Get Assign To.
	  * Assign resource until
	  */
	public Timestamp getAssignDateTo();

    /** Column name AssignmentSourceType */
    public static final String COLUMNNAME_AssignmentSourceType = "AssignmentSourceType";

	/** Set Assignment Source Type.
	  * How this assignment was originated: (P)lan, (M)anual, (S)ubstitution, (R)eassignment
	  */
	public void setAssignmentSourceType (String AssignmentSourceType);

	/** Get Assignment Source Type.
	  * How this assignment was originated: (P)lan, (M)anual, (S)ubstitution, (R)eassignment
	  */
	public String getAssignmentSourceType();

    /** Column name AssignmentStatus */
    public static final String COLUMNNAME_AssignmentStatus = "AssignmentStatus";

	/** Set Assignment Status.
	  * Operational status: PL(Planned), PC(ToCoordinate), IP(InProgress), CO(Confirmed), PA(PendingAuth), AP(Approved), CA(Cancelled), NA(NotAttended)
	  */
	public void setAssignmentStatus (String AssignmentStatus);

	/** Get Assignment Status.
	  * Operational status: PL(Planned), PC(ToCoordinate), IP(InProgress), CO(Confirmed), PA(PendingAuth), AP(Approved), CA(Cancelled), NA(NotAttended)
	  */
	public String getAssignmentStatus();

    /** Column name C_Activity_ID */
    public static final String COLUMNNAME_C_Activity_ID = "C_Activity_ID";

	/** Set Activity.
	  * Business Activity
	  */
	public void setC_Activity_ID (int C_Activity_ID);

	/** Get Activity.
	  * Business Activity
	  */
	public int getC_Activity_ID();

	public I_C_Activity getC_Activity() throws RuntimeException;

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set Business Partner .
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get Business Partner .
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public I_C_BPartner getC_BPartner() throws RuntimeException;

    /** Column name C_BPartner_Location_ID */
    public static final String COLUMNNAME_C_BPartner_Location_ID = "C_BPartner_Location_ID";

	/** Set Partner Location.
	  * Identifies the (ship to) address for this Business Partner
	  */
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID);

	/** Get Partner Location.
	  * Identifies the (ship to) address for this Business Partner
	  */
	public int getC_BPartner_Location_ID();

	public I_C_BPartner_Location getC_BPartner_Location() throws RuntimeException;

    /** Column name C_Campaign_ID */
    public static final String COLUMNNAME_C_Campaign_ID = "C_Campaign_ID";

	/** Set Campaign.
	  * Marketing Campaign
	  */
	public void setC_Campaign_ID (int C_Campaign_ID);

	/** Get Campaign.
	  * Marketing Campaign
	  */
	public int getC_Campaign_ID();

	public I_C_Campaign getC_Campaign() throws RuntimeException;

    /** Column name C_Project_ID */
    public static final String COLUMNNAME_C_Project_ID = "C_Project_ID";

	/** Set Project.
	  * Financial Project
	  */
	public void setC_Project_ID (int C_Project_ID);

	/** Get Project.
	  * Financial Project
	  */
	public int getC_Project_ID();

	public I_C_Project getC_Project() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name HR_Leave_ID */
    public static final String COLUMNNAME_HR_Leave_ID = "HR_Leave_ID";

	/** Set Leave.
	  * The Leave Credit History of an Employee
	  */
	public void setHR_Leave_ID (int HR_Leave_ID);

	/** Get Leave.
	  * The Leave Credit History of an Employee
	  */
	public int getHR_Leave_ID();

	public I_HR_Leave getHR_Leave() throws RuntimeException;

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsConfirmed */
    public static final String COLUMNNAME_IsConfirmed = "IsConfirmed";

	/** Set Confirmed.
	  * Assignment is confirmed
	  */
	public void setIsConfirmed (boolean IsConfirmed);

	/** Get Confirmed.
	  * Assignment is confirmed
	  */
	public boolean isConfirmed();

    /** Column name IsManuallyModified */
    public static final String COLUMNNAME_IsManuallyModified = "IsManuallyModified";

	/** Set Manually Modified.
	  * When Y, this assignment is protected from automatic regeneration by plan changes
	  */
	public void setIsManuallyModified (boolean IsManuallyModified);

	/** Get Manually Modified.
	  * When Y, this assignment is protected from automatic regeneration by plan changes
	  */
	public boolean isManuallyModified();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name PreviousResource_ID */
    public static final String COLUMNNAME_PreviousResource_ID = "PreviousResource_ID";

	/** Set Previous Resource.
	  * Resource that was assigned before the last modification. Audit trail.
	  */
	public void setPreviousResource_ID (int PreviousResource_ID);

	/** Get Previous Resource.
	  * Resource that was assigned before the last modification. Audit trail.
	  */
	public int getPreviousResource_ID();

	public I_S_Resource getPreviousResource() throws RuntimeException;

    /** Column name Qty */
    public static final String COLUMNNAME_Qty = "Qty";

	/** Set Quantity.
	  * Quantity
	  */
	public void setQty (BigDecimal Qty);

	/** Get Quantity.
	  * Quantity
	  */
	public BigDecimal getQty();

    /** Column name ReplanReason */
    public static final String COLUMNNAME_ReplanReason = "ReplanReason";

	/** Set Replan Reason.
	  * Free text reason entered by coordinator when replanning this assignment
	  */
	public void setReplanReason (String ReplanReason);

	/** Get Replan Reason.
	  * Free text reason entered by coordinator when replanning this assignment
	  */
	public String getReplanReason();

    /** Column name R_Request_ID */
    public static final String COLUMNNAME_R_Request_ID = "R_Request_ID";

	/** Set Request.
	  * Request from a Business Partner or Prospect
	  */
	public void setR_Request_ID (int R_Request_ID);

	/** Get Request.
	  * Request from a Business Partner or Prospect
	  */
	public int getR_Request_ID();

	public I_R_Request getR_Request() throws RuntimeException;

    /** Column name S_Contract_ID */
    public static final String COLUMNNAME_S_Contract_ID = "S_Contract_ID";

	/** Set Contract.
	  * Contract
	  */
	public void setS_Contract_ID (int S_Contract_ID);

	/** Get Contract.
	  * Contract
	  */
	public int getS_Contract_ID();

	public I_S_Contract getS_Contract() throws RuntimeException;

    /** Column name S_ResourceAssignment_ID */
    public static final String COLUMNNAME_S_ResourceAssignment_ID = "S_ResourceAssignment_ID";

	/** Set Resource Assignment.
	  * Resource Assignment
	  */
	public void setS_ResourceAssignment_ID (int S_ResourceAssignment_ID);

	/** Get Resource Assignment.
	  * Resource Assignment
	  */
	public int getS_ResourceAssignment_ID();

    /** Column name S_Resource_ID */
    public static final String COLUMNNAME_S_Resource_ID = "S_Resource_ID";

	/** Set Resource.
	  * Resource
	  */
	public void setS_Resource_ID (int S_Resource_ID);

	/** Get Resource.
	  * Resource
	  */
	public int getS_Resource_ID();

	public I_S_Resource getS_Resource() throws RuntimeException;

    /** Column name S_Resource_Plan_ID */
    public static final String COLUMNNAME_S_Resource_Plan_ID = "S_Resource_Plan_ID";

	/** Set Plan Resource.
	  * Original titular resource as defined in the service plan. NEVER modified after creation.
	  */
	public void setS_Resource_Plan_ID (int S_Resource_Plan_ID);

	/** Get Plan Resource.
	  * Original titular resource as defined in the service plan. NEVER modified after creation.
	  */
	public int getS_Resource_Plan_ID();

	public I_S_Resource getS_Resource_Plan() throws RuntimeException;

    /** Column name S_ServicePlan_ID */
    public static final String COLUMNNAME_S_ServicePlan_ID = "S_ServicePlan_ID";

	/** Set Service Plan ID.
	  * Operational service plan
	  */
	public void setS_ServicePlan_ID (int S_ServicePlan_ID);

	/** Get Service Plan ID.
	  * Operational service plan
	  */
	public int getS_ServicePlan_ID();

	public I_S_ServicePlan getS_ServicePlan() throws RuntimeException;

    /** Column name S_ServiceSchedule_ID */
    public static final String COLUMNNAME_S_ServiceSchedule_ID = "S_ServiceSchedule_ID";

	/** Set Service Schedule ID.
	  * Weekly visit pattern for a service plan
	  */
	public void setS_ServiceSchedule_ID (int S_ServiceSchedule_ID);

	/** Get Service Schedule ID.
	  * Weekly visit pattern for a service plan
	  */
	public int getS_ServiceSchedule_ID();

	public I_S_ServiceSchedule getS_ServiceSchedule() throws RuntimeException;

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name User1_ID */
    public static final String COLUMNNAME_User1_ID = "User1_ID";

	/** Set User List 1.
	  * User defined list element #1
	  */
	public void setUser1_ID (int User1_ID);

	/** Get User List 1.
	  * User defined list element #1
	  */
	public int getUser1_ID();

	public I_C_ElementValue getUser1() throws RuntimeException;

    /** Column name User2_ID */
    public static final String COLUMNNAME_User2_ID = "User2_ID";

	/** Set User List 2.
	  * User defined list element #2
	  */
	public void setUser2_ID (int User2_ID);

	/** Get User List 2.
	  * User defined list element #2
	  */
	public int getUser2_ID();

	public I_C_ElementValue getUser2() throws RuntimeException;

    /** Column name User3_ID */
    public static final String COLUMNNAME_User3_ID = "User3_ID";

	/** Set User List 3.
	  * User defined list element #3
	  */
	public void setUser3_ID (int User3_ID);

	/** Get User List 3.
	  * User defined list element #3
	  */
	public int getUser3_ID();

	public I_C_ElementValue getUser3() throws RuntimeException;

    /** Column name User4_ID */
    public static final String COLUMNNAME_User4_ID = "User4_ID";

	/** Set User List 4.
	  * User defined list element #4
	  */
	public void setUser4_ID (int User4_ID);

	/** Get User List 4.
	  * User defined list element #4
	  */
	public int getUser4_ID();

	public I_C_ElementValue getUser4() throws RuntimeException;

    /** Column name UUID */
    public static final String COLUMNNAME_UUID = "UUID";

	/** Set Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID);

	/** Get Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public String getUUID();
}
