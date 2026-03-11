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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for S_ServiceSchedule
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_S_ServiceSchedule extends PO implements I_S_ServiceSchedule, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260311L;

    /** Standard Constructor */
    public X_S_ServiceSchedule (Properties ctx, int S_ServiceSchedule_ID, String trxName)
    {
      super (ctx, S_ServiceSchedule_ID, trxName);
      /** if (S_ServiceSchedule_ID == 0)
        {
			setDayOfWeek (null);
			setPlannedHours (Env.ZERO);
			setS_ServicePlan_ID (0);
			setS_ServiceSchedule_ID (0);
			setTimeFrom (new Timestamp( System.currentTimeMillis() ));
			setTimeTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_S_ServiceSchedule (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_S_ServiceSchedule[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** DayOfWeek AD_Reference_ID=167 */
	public static final int DAYOFWEEK_AD_Reference_ID=167;
	/** Sunday = 7 */
	public static final String DAYOFWEEK_Sunday = "7";
	/** Monday = 1 */
	public static final String DAYOFWEEK_Monday = "1";
	/** Tuesday = 2 */
	public static final String DAYOFWEEK_Tuesday = "2";
	/** Wednesday = 3 */
	public static final String DAYOFWEEK_Wednesday = "3";
	/** Thursday = 4 */
	public static final String DAYOFWEEK_Thursday = "4";
	/** Friday = 5 */
	public static final String DAYOFWEEK_Friday = "5";
	/** Saturday = 6 */
	public static final String DAYOFWEEK_Saturday = "6";
	/** Set Day of Week.
		@param DayOfWeek 
		Day of the week: 1=Monday, 2=Tuesday, ..., 7=Sunday (ISO 8601)
	  */
	public void setDayOfWeek (String DayOfWeek)
	{

		set_Value (COLUMNNAME_DayOfWeek, DayOfWeek);
	}

	/** Get Day of Week.
		@return Day of the week: 1=Monday, 2=Tuesday, ..., 7=Sunday (ISO 8601)
	  */
	public String getDayOfWeek () 
	{
		return (String)get_Value(COLUMNNAME_DayOfWeek);
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

	/** Set Planned Hours.
		@param PlannedHours 
		Planned duration in hours for this visit
	  */
	public void setPlannedHours (BigDecimal PlannedHours)
	{
		set_Value (COLUMNNAME_PlannedHours, PlannedHours);
	}

	/** Get Planned Hours.
		@return Planned duration in hours for this visit
	  */
	public BigDecimal getPlannedHours () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PlannedHours);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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
			set_Value (COLUMNNAME_S_Resource_ID, null);
		else 
			set_Value (COLUMNNAME_S_Resource_ID, Integer.valueOf(S_Resource_ID));
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
			set_ValueNoCheck (COLUMNNAME_S_ServicePlan_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_S_ServicePlan_ID, Integer.valueOf(S_ServicePlan_ID));
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

	/** Set Service Schedule ID.
		@param S_ServiceSchedule_ID 
		Weekly visit pattern for a service plan
	  */
	public void setS_ServiceSchedule_ID (int S_ServiceSchedule_ID)
	{
		if (S_ServiceSchedule_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_S_ServiceSchedule_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_S_ServiceSchedule_ID, Integer.valueOf(S_ServiceSchedule_ID));
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

	/** Set Time (From).
		@param TimeFrom 
		Starting Time
	  */
	public void setTimeFrom (Timestamp TimeFrom)
	{
		set_Value (COLUMNNAME_TimeFrom, TimeFrom);
	}

	/** Get Time (From).
		@return Starting Time
	  */
	public Timestamp getTimeFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_TimeFrom);
	}

	/** Set Time (To).
		@param TimeTo 
		Ending Time
	  */
	public void setTimeTo (Timestamp TimeTo)
	{
		set_Value (COLUMNNAME_TimeTo, TimeTo);
	}

	/** Get Time (To).
		@return Ending Time
	  */
	public Timestamp getTimeTo () 
	{
		return (Timestamp)get_Value(COLUMNNAME_TimeTo);
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

	/** Set Week of Month.
		@param WeekOfMonth 
		Which week of the month (1-5). Used for biweekly and monthly frequency
	  */
	public void setWeekOfMonth (int WeekOfMonth)
	{
		set_Value (COLUMNNAME_WeekOfMonth, Integer.valueOf(WeekOfMonth));
	}

	/** Get Week of Month.
		@return Which week of the month (1-5). Used for biweekly and monthly frequency
	  */
	public int getWeekOfMonth () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WeekOfMonth);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}