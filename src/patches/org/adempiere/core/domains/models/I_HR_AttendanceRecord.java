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

/** Generated Interface for HR_AttendanceRecord
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_HR_AttendanceRecord 
{

    /** TableName=HR_AttendanceRecord */
    public static final String Table_Name = "HR_AttendanceRecord";

    /** AD_Table_ID=54499 */
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

    /** Column name AttendanceTime */
    public static final String COLUMNNAME_AttendanceTime = "AttendanceTime";

	/** Set Attendance Time.
	  * Attendance Time for Employee
	  */
	public void setAttendanceTime (Timestamp AttendanceTime);

	/** Get Attendance Time.
	  * Attendance Time for Employee
	  */
	public Timestamp getAttendanceTime();

    /** Column name Comments */
    public static final String COLUMNNAME_Comments = "Comments";

	/** Set Comments.
	  * Comments or additional information
	  */
	public void setComments (String Comments);

	/** Get Comments.
	  * Comments or additional information
	  */
	public String getComments();

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

    /** Column name HR_AttendanceBatch_ID */
    public static final String COLUMNNAME_HR_AttendanceBatch_ID = "HR_AttendanceBatch_ID";

	/** Set Attendance Batch	  */
	public void setHR_AttendanceBatch_ID (int HR_AttendanceBatch_ID);

	/** Get Attendance Batch	  */
	public int getHR_AttendanceBatch_ID();

	public I_HR_AttendanceBatch getHR_AttendanceBatch() throws RuntimeException;

    /** Column name HR_AttendanceRecord_ID */
    public static final String COLUMNNAME_HR_AttendanceRecord_ID = "HR_AttendanceRecord_ID";

	/** Set Attendance Record.
	  * Attendance Record
	  */
	public void setHR_AttendanceRecord_ID (int HR_AttendanceRecord_ID);

	/** Get Attendance Record.
	  * Attendance Record
	  */
	public int getHR_AttendanceRecord_ID();

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

    /** Column name IsOfflineMark */
    public static final String COLUMNNAME_IsOfflineMark = "IsOfflineMark";

	/** Set Offline Mark.
	  * Attendance mark was made without internet connection and synced later
	  */
	public void setIsOfflineMark (boolean IsOfflineMark);

	/** Get Offline Mark.
	  * Attendance mark was made without internet connection and synced later
	  */
	public boolean isOfflineMark();

    /** Column name IsOutOfTime */
    public static final String COLUMNNAME_IsOutOfTime = "IsOutOfTime";

	/** Set Out of Time.
	  * Attendance mark was made outside the time tolerance (10 minutes)
	  */
	public void setIsOutOfTime (boolean IsOutOfTime);

	/** Get Out of Time.
	  * Attendance mark was made outside the time tolerance (10 minutes)
	  */
	public boolean isOutOfTime();

    /** Column name IsOutOfZone */
    public static final String COLUMNNAME_IsOutOfZone = "IsOutOfZone";

	/** Set Out of Zone.
	  * Attendance mark was made outside the geofence radius of the client location
	  */
	public void setIsOutOfZone (boolean IsOutOfZone);

	/** Get Out of Zone.
	  * Attendance mark was made outside the geofence radius of the client location
	  */
	public boolean isOutOfZone();

    /** Column name Latitude */
    public static final String COLUMNNAME_Latitude = "Latitude";

	/** Set Latitude.
	  * Latitude is a geographic coordinate that specifies the north–south position of a point on the Earth's surface.
	  */
	public void setLatitude (BigDecimal Latitude);

	/** Get Latitude.
	  * Latitude is a geographic coordinate that specifies the north–south position of a point on the Earth's surface.
	  */
	public BigDecimal getLatitude();

    /** Column name Longitude */
    public static final String COLUMNNAME_Longitude = "Longitude";

	/** Set Longitude.
	  * Longitude  is a geographic coordinate that specifies the east–west position of a point on the Earth's surface, or the surface of a celestial body
	  */
	public void setLongitude (BigDecimal Longitude);

	/** Get Longitude.
	  * Longitude  is a geographic coordinate that specifies the east–west position of a point on the Earth's surface, or the surface of a celestial body
	  */
	public BigDecimal getLongitude();

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name SeqNo */
    public static final String COLUMNNAME_SeqNo = "SeqNo";

	/** Set Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public void setSeqNo (int SeqNo);

	/** Get Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public int getSeqNo();

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

	public I_S_ResourceAssignment getS_ResourceAssignment() throws RuntimeException;

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
}
