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

/** Generated Model for HR_AttendanceRecord
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_HR_AttendanceRecord extends PO implements I_HR_AttendanceRecord, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260306L;

    /** Standard Constructor */
    public X_HR_AttendanceRecord (Properties ctx, int HR_AttendanceRecord_ID, String trxName)
    {
      super (ctx, HR_AttendanceRecord_ID, trxName);
      /** if (HR_AttendanceRecord_ID == 0)
        {
			setAttendanceTime (new Timestamp( System.currentTimeMillis() ));
			setHR_AttendanceBatch_ID (0);
			setHR_AttendanceRecord_ID (0);
			setIsOfflineMark (false);
// N
			setIsOutOfTime (false);
// N
			setIsOutOfZone (false);
// N
        } */
    }

    /** Load Constructor */
    public X_HR_AttendanceRecord (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
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
      StringBuffer sb = new StringBuffer ("X_HR_AttendanceRecord[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Attendance Time.
		@param AttendanceTime 
		Attendance Time for Employee
	  */
	public void setAttendanceTime (Timestamp AttendanceTime)
	{
		set_Value (COLUMNNAME_AttendanceTime, AttendanceTime);
	}

	/** Get Attendance Time.
		@return Attendance Time for Employee
	  */
	public Timestamp getAttendanceTime () 
	{
		return (Timestamp)get_Value(COLUMNNAME_AttendanceTime);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getAttendanceTime()));
    }

	/** Set Comments.
		@param Comments 
		Comments or additional information
	  */
	public void setComments (String Comments)
	{
		set_Value (COLUMNNAME_Comments, Comments);
	}

	/** Get Comments.
		@return Comments or additional information
	  */
	public String getComments () 
	{
		return (String)get_Value(COLUMNNAME_Comments);
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

	public I_HR_AttendanceBatch getHR_AttendanceBatch() throws RuntimeException
    {
		return (I_HR_AttendanceBatch)MTable.get(getCtx(), I_HR_AttendanceBatch.Table_Name)
			.getPO(getHR_AttendanceBatch_ID(), get_TrxName());	}

	/** Set Attendance Batch.
		@param HR_AttendanceBatch_ID Attendance Batch	  */
	public void setHR_AttendanceBatch_ID (int HR_AttendanceBatch_ID)
	{
		if (HR_AttendanceBatch_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_HR_AttendanceBatch_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_HR_AttendanceBatch_ID, Integer.valueOf(HR_AttendanceBatch_ID));
	}

	/** Get Attendance Batch.
		@return Attendance Batch	  */
	public int getHR_AttendanceBatch_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HR_AttendanceBatch_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Attendance Record.
		@param HR_AttendanceRecord_ID 
		Attendance Record
	  */
	public void setHR_AttendanceRecord_ID (int HR_AttendanceRecord_ID)
	{
		if (HR_AttendanceRecord_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_HR_AttendanceRecord_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_HR_AttendanceRecord_ID, Integer.valueOf(HR_AttendanceRecord_ID));
	}

	/** Get Attendance Record.
		@return Attendance Record
	  */
	public int getHR_AttendanceRecord_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HR_AttendanceRecord_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Offline Mark.
		@param IsOfflineMark 
		Attendance mark was made without internet connection and synced later
	  */
	public void setIsOfflineMark (boolean IsOfflineMark)
	{
		set_Value (COLUMNNAME_IsOfflineMark, Boolean.valueOf(IsOfflineMark));
	}

	/** Get Offline Mark.
		@return Attendance mark was made without internet connection and synced later
	  */
	public boolean isOfflineMark () 
	{
		Object oo = get_Value(COLUMNNAME_IsOfflineMark);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Out of Time.
		@param IsOutOfTime 
		Attendance mark was made outside the time tolerance (10 minutes)
	  */
	public void setIsOutOfTime (boolean IsOutOfTime)
	{
		set_Value (COLUMNNAME_IsOutOfTime, Boolean.valueOf(IsOutOfTime));
	}

	/** Get Out of Time.
		@return Attendance mark was made outside the time tolerance (10 minutes)
	  */
	public boolean isOutOfTime () 
	{
		Object oo = get_Value(COLUMNNAME_IsOutOfTime);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Out of Zone.
		@param IsOutOfZone 
		Attendance mark was made outside the geofence radius of the client location
	  */
	public void setIsOutOfZone (boolean IsOutOfZone)
	{
		set_Value (COLUMNNAME_IsOutOfZone, Boolean.valueOf(IsOutOfZone));
	}

	/** Get Out of Zone.
		@return Attendance mark was made outside the geofence radius of the client location
	  */
	public boolean isOutOfZone () 
	{
		Object oo = get_Value(COLUMNNAME_IsOutOfZone);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Latitude.
		@param Latitude 
		Latitude is a geographic coordinate that specifies the north–south position of a point on the Earth's surface.
	  */
	public void setLatitude (BigDecimal Latitude)
	{
		set_Value (COLUMNNAME_Latitude, Latitude);
	}

	/** Get Latitude.
		@return Latitude is a geographic coordinate that specifies the north–south position of a point on the Earth's surface.
	  */
	public BigDecimal getLatitude () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Latitude);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Longitude.
		@param Longitude 
		Longitude  is a geographic coordinate that specifies the east–west position of a point on the Earth's surface, or the surface of a celestial body
	  */
	public void setLongitude (BigDecimal Longitude)
	{
		set_Value (COLUMNNAME_Longitude, Longitude);
	}

	/** Get Longitude.
		@return Longitude  is a geographic coordinate that specifies the east–west position of a point on the Earth's surface, or the surface of a celestial body
	  */
	public BigDecimal getLongitude () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Longitude);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Processed.
		@param Processed 
		The document has been processed
	  */
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Sequence.
		@param SeqNo 
		Method of ordering records; lowest number comes first
	  */
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_S_ResourceAssignment getS_ResourceAssignment() throws RuntimeException
    {
		return (I_S_ResourceAssignment)MTable.get(getCtx(), I_S_ResourceAssignment.Table_Name)
			.getPO(getS_ResourceAssignment_ID(), get_TrxName());	}

	/** Set Resource Assignment.
		@param S_ResourceAssignment_ID 
		Resource Assignment
	  */
	public void setS_ResourceAssignment_ID (int S_ResourceAssignment_ID)
	{
		if (S_ResourceAssignment_ID < 1) 
			set_Value (COLUMNNAME_S_ResourceAssignment_ID, null);
		else 
			set_Value (COLUMNNAME_S_ResourceAssignment_ID, Integer.valueOf(S_ResourceAssignment_ID));
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