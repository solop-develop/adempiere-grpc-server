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

/** Generated Model for R_Iteration
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_R_Iteration extends PO implements I_R_Iteration, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251226L;

    /** Standard Constructor */
    public X_R_Iteration (Properties ctx, int R_Iteration_ID, String trxName)
    {
      super (ctx, R_Iteration_ID, trxName);
      /** if (R_Iteration_ID == 0)
        {
			setC_Project_ID (0);
			setDateFrom (new Timestamp( System.currentTimeMillis() ));
			setDateTo (new Timestamp( System.currentTimeMillis() ));
			setR_Iteration_ID (0);
        } */
    }

    /** Load Constructor */
    public X_R_Iteration (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_R_Iteration[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set Date From.
		@param DateFrom 
		Starting date for a range
	  */
	public void setDateFrom (Timestamp DateFrom)
	{
		set_Value (COLUMNNAME_DateFrom, DateFrom);
	}

	/** Get Date From.
		@return Starting date for a range
	  */
	public Timestamp getDateFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateFrom);
	}

	/** Set Date To.
		@param DateTo 
		End date of a date range
	  */
	public void setDateTo (Timestamp DateTo)
	{
		set_Value (COLUMNNAME_DateTo, DateTo);
	}

	/** Get Date To.
		@return End date of a date range
	  */
	public Timestamp getDateTo () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateTo);
	}

	/** Set Duration.
		@param Duration 
		Normal Duration in Duration Unit
	  */
	public void setDuration (int Duration)
	{
		set_Value (COLUMNNAME_Duration, Integer.valueOf(Duration));
	}

	/** Get Duration.
		@return Normal Duration in Duration Unit
	  */
	public int getDuration () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Duration);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** IterationStatus AD_Reference_ID=131 */
	public static final int ITERATIONSTATUS_AD_Reference_ID=131;
	/** Drafted = DR */
	public static final String ITERATIONSTATUS_Drafted = "DR";
	/** Completed = CO */
	public static final String ITERATIONSTATUS_Completed = "CO";
	/** Approved = AP */
	public static final String ITERATIONSTATUS_Approved = "AP";
	/** Not Approved = NA */
	public static final String ITERATIONSTATUS_NotApproved = "NA";
	/** Voided = VO */
	public static final String ITERATIONSTATUS_Voided = "VO";
	/** Invalid = IN */
	public static final String ITERATIONSTATUS_Invalid = "IN";
	/** Reversed = RE */
	public static final String ITERATIONSTATUS_Reversed = "RE";
	/** Closed = CL */
	public static final String ITERATIONSTATUS_Closed = "CL";
	/** Unknown = ?? */
	public static final String ITERATIONSTATUS_Unknown = "??";
	/** In Progress = IP */
	public static final String ITERATIONSTATUS_InProgress = "IP";
	/** Waiting Payment = WP */
	public static final String ITERATIONSTATUS_WaitingPayment = "WP";
	/** Waiting Confirmation = WC */
	public static final String ITERATIONSTATUS_WaitingConfirmation = "WC";
	/** Set Iteration Status.
		@param IterationStatus Iteration Status	  */
	public void setIterationStatus (String IterationStatus)
	{

		set_Value (COLUMNNAME_IterationStatus, IterationStatus);
	}

	/** Get Iteration Status.
		@return Iteration Status	  */
	public String getIterationStatus () 
	{
		return (String)get_Value(COLUMNNAME_IterationStatus);
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

	/** Set Percentage.
		@param Percentage 
		Percent of the entire amount
	  */
	public void setPercentage (BigDecimal Percentage)
	{
		set_Value (COLUMNNAME_Percentage, Percentage);
	}

	/** Get Percentage.
		@return Percent of the entire amount
	  */
	public BigDecimal getPercentage () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Percentage);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Iteration.
		@param R_Iteration_ID Iteration	  */
	public void setR_Iteration_ID (int R_Iteration_ID)
	{
		if (R_Iteration_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_R_Iteration_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_R_Iteration_ID, Integer.valueOf(R_Iteration_ID));
	}

	/** Get Iteration.
		@return Iteration	  */
	public int getR_Iteration_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_Iteration_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** TimeUnit AD_Reference_ID=53376 */
	public static final int TIMEUNIT_AD_Reference_ID=53376;
	/** Day = D */
	public static final String TIMEUNIT_Day = "D";
	/** Week = W */
	public static final String TIMEUNIT_Week = "W";
	/** Month = M */
	public static final String TIMEUNIT_Month = "M";
	/** Quarter = Q */
	public static final String TIMEUNIT_Quarter = "Q";
	/** Year = Y */
	public static final String TIMEUNIT_Year = "Y";
	/** Hour = H */
	public static final String TIMEUNIT_Hour = "H";
	/** Minute = I */
	public static final String TIMEUNIT_Minute = "I";
	/** Set Time Unit.
		@param TimeUnit 
		The unit of time for grouping chart data.
	  */
	public void setTimeUnit (String TimeUnit)
	{

		set_Value (COLUMNNAME_TimeUnit, TimeUnit);
	}

	/** Get Time Unit.
		@return The unit of time for grouping chart data.
	  */
	public String getTimeUnit () 
	{
		return (String)get_Value(COLUMNNAME_TimeUnit);
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