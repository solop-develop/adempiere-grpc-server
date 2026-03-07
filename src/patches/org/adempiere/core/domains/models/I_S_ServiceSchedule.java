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

/** Generated Interface for S_ServiceSchedule
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_S_ServiceSchedule 
{

    /** TableName=S_ServiceSchedule */
    public static final String Table_Name = "S_ServiceSchedule";

    /** AD_Table_ID=55095 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

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

    /** Column name DayOfWeek */
    public static final String COLUMNNAME_DayOfWeek = "DayOfWeek";

	/** Set Day of Week.
	  * Day of the week: 1=Monday, 2=Tuesday, ..., 7=Sunday (ISO 8601)
	  */
	public void setDayOfWeek (String DayOfWeek);

	/** Get Day of Week.
	  * Day of the week: 1=Monday, 2=Tuesday, ..., 7=Sunday (ISO 8601)
	  */
	public String getDayOfWeek();

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

    /** Column name PlannedHours */
    public static final String COLUMNNAME_PlannedHours = "PlannedHours";

	/** Set Planned Hours.
	  * Planned duration in hours for this visit
	  */
	public void setPlannedHours (BigDecimal PlannedHours);

	/** Get Planned Hours.
	  * Planned duration in hours for this visit
	  */
	public BigDecimal getPlannedHours();

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

    /** Column name TimeFrom */
    public static final String COLUMNNAME_TimeFrom = "TimeFrom";

	/** Set Time (From).
	  * Starting Time
	  */
	public void setTimeFrom (Timestamp TimeFrom);

	/** Get Time (From).
	  * Starting Time
	  */
	public Timestamp getTimeFrom();

    /** Column name TimeTo */
    public static final String COLUMNNAME_TimeTo = "TimeTo";

	/** Set Time (To).
	  * Ending Time
	  */
	public void setTimeTo (Timestamp TimeTo);

	/** Get Time (To).
	  * Ending Time
	  */
	public Timestamp getTimeTo();

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

    /** Column name WeekOfMonth */
    public static final String COLUMNNAME_WeekOfMonth = "WeekOfMonth";

	/** Set Week of Month.
	  * Which week of the month (1-5). Used for biweekly and monthly frequency
	  */
	public void setWeekOfMonth (int WeekOfMonth);

	/** Get Week of Month.
	  * Which week of the month (1-5). Used for biweekly and monthly frequency
	  */
	public int getWeekOfMonth();
}
