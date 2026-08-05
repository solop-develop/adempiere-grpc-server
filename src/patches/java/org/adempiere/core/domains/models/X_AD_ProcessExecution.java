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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for AD_ProcessExecution
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_ProcessExecution extends PO implements I_AD_ProcessExecution, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260805L;

    /** Standard Constructor */
    public X_AD_ProcessExecution (Properties ctx, int AD_ProcessExecution_ID, String trxName)
    {
      super (ctx, AD_ProcessExecution_ID, trxName);
      /** if (AD_ProcessExecution_ID == 0)
        {
			setAD_ProcessExecution_ID (0);
			setAD_Process_ID (0);
			setProcessExecutionStatus (null);
// Q
        } */
    }

    /** Load Constructor */
    public X_AD_ProcessExecution (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_AD_ProcessExecution[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_PInstance getAD_PInstance() throws RuntimeException
    {
		return (I_AD_PInstance)MTable.get(getCtx(), I_AD_PInstance.Table_Name)
			.getPO(getAD_PInstance_ID(), get_TrxName());	}

	/** Set Process Instance.
		@param AD_PInstance_ID 
		Instance of the process
	  */
	public void setAD_PInstance_ID (int AD_PInstance_ID)
	{
		if (AD_PInstance_ID < 1) 
			set_Value (COLUMNNAME_AD_PInstance_ID, null);
		else 
			set_Value (COLUMNNAME_AD_PInstance_ID, Integer.valueOf(AD_PInstance_ID));
	}

	/** Get Process Instance.
		@return Instance of the process
	  */
	public int getAD_PInstance_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_PInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Process Execution.
		@param AD_ProcessExecution_ID 
		Idempotent Execution Request for an ADempiere Process
	  */
	public void setAD_ProcessExecution_ID (int AD_ProcessExecution_ID)
	{
		if (AD_ProcessExecution_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_ProcessExecution_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_ProcessExecution_ID, Integer.valueOf(AD_ProcessExecution_ID));
	}

	/** Get Process Execution.
		@return Idempotent Execution Request for an ADempiere Process
	  */
	public int getAD_ProcessExecution_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_ProcessExecution_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Process getAD_Process() throws RuntimeException
    {
		return (I_AD_Process)MTable.get(getCtx(), I_AD_Process.Table_Name)
			.getPO(getAD_Process_ID(), get_TrxName());	}

	/** Set Process.
		@param AD_Process_ID 
		Process or Report
	  */
	public void setAD_Process_ID (int AD_Process_ID)
	{
		if (AD_Process_ID < 1) 
			set_Value (COLUMNNAME_AD_Process_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Process_ID, Integer.valueOf(AD_Process_ID));
	}

	/** Get Process.
		@return Process or Report
	  */
	public int getAD_Process_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Process_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Role getAD_Role() throws RuntimeException
    {
		return (I_AD_Role)MTable.get(getCtx(), I_AD_Role.Table_Name)
			.getPO(getAD_Role_ID(), get_TrxName());	}

	/** Set Role.
		@param AD_Role_ID 
		Responsibility Role
	  */
	public void setAD_Role_ID (int AD_Role_ID)
	{
		if (AD_Role_ID < 0) 
			set_Value (COLUMNNAME_AD_Role_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
	}

	/** Get Role.
		@return Responsibility Role
	  */
	public int getAD_Role_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Scheduler getAD_Scheduler() throws RuntimeException
    {
		return (I_AD_Scheduler)MTable.get(getCtx(), I_AD_Scheduler.Table_Name)
			.getPO(getAD_Scheduler_ID(), get_TrxName());	}

	/** Set Scheduler.
		@param AD_Scheduler_ID 
		Schedule Processes
	  */
	public void setAD_Scheduler_ID (int AD_Scheduler_ID)
	{
		if (AD_Scheduler_ID < 1) 
			set_Value (COLUMNNAME_AD_Scheduler_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Scheduler_ID, Integer.valueOf(AD_Scheduler_ID));
	}

	/** Get Scheduler.
		@return Schedule Processes
	  */
	public int getAD_Scheduler_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Scheduler_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_Table getAD_Table() throws RuntimeException
    {
		return (I_AD_Table)MTable.get(getCtx(), I_AD_Table.Table_Name)
			.getPO(getAD_Table_ID(), get_TrxName());	}

	/** Set Table.
		@param AD_Table_ID 
		Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1) 
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_User getAD_User() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getAD_User_ID(), get_TrxName());	}

	/** Set User/Contact.
		@param AD_User_ID 
		User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1) 
			set_Value (COLUMNNAME_AD_User_ID, null);
		else 
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Date Finished.
		@param DateFinished 
		Date and Time When the Execution Finished
	  */
	public void setDateFinished (Timestamp DateFinished)
	{
		set_Value (COLUMNNAME_DateFinished, DateFinished);
	}

	/** Get Date Finished.
		@return Date and Time When the Execution Finished
	  */
	public Timestamp getDateFinished () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateFinished);
	}

	/** Set Date Started.
		@param DateStarted 
		Date and Time When the Execution Started
	  */
	public void setDateStarted (Timestamp DateStarted)
	{
		set_Value (COLUMNNAME_DateStarted, DateStarted);
	}

	/** Get Date Started.
		@return Date and Time When the Execution Started
	  */
	public Timestamp getDateStarted () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateStarted);
	}

	/** Set Error Msg.
		@param ErrorMsg Error Msg	  */
	public void setErrorMsg (String ErrorMsg)
	{
		set_Value (COLUMNNAME_ErrorMsg, ErrorMsg);
	}

	/** Get Error Msg.
		@return Error Msg	  */
	public String getErrorMsg () 
	{
		return (String)get_Value(COLUMNNAME_ErrorMsg);
	}

	/** ExecutionMode AD_Reference_ID=54735 */
	public static final int EXECUTIONMODE_AD_Reference_ID=54735;
	/** Synchronous = S */
	public static final String EXECUTIONMODE_Synchronous = "S";
	/** Asynchronous = A */
	public static final String EXECUTIONMODE_Asynchronous = "A";
	/** Both (Client Decides) = B */
	public static final String EXECUTIONMODE_BothClientDecides = "B";
	/** Set Execution Mode.
		@param ExecutionMode 
		Mode That Determines Whether the Process Runs Synchronously or Asynchronously
	  */
	public void setExecutionMode (String ExecutionMode)
	{

		set_Value (COLUMNNAME_ExecutionMode, ExecutionMode);
	}

	/** Get Execution Mode.
		@return Mode That Determines Whether the Process Runs Synchronously or Asynchronously
	  */
	public String getExecutionMode () 
	{
		return (String)get_Value(COLUMNNAME_ExecutionMode);
	}

	/** Set Idempotency Key.
		@param IdempotencyKey 
		Client Supplied Key Used to Deduplicate Execution Requests
	  */
	public void setIdempotencyKey (String IdempotencyKey)
	{
		set_Value (COLUMNNAME_IdempotencyKey, IdempotencyKey);
	}

	/** Get Idempotency Key.
		@return Client Supplied Key Used to Deduplicate Execution Requests
	  */
	public String getIdempotencyKey () 
	{
		return (String)get_Value(COLUMNNAME_IdempotencyKey);
	}

	/** Set Cancel Requested.
		@param IsCancelRequested 
		A Cancellation Has Been Requested for This Execution
	  */
	public void setIsCancelRequested (boolean IsCancelRequested)
	{
		set_Value (COLUMNNAME_IsCancelRequested, Boolean.valueOf(IsCancelRequested));
	}

	/** Get Cancel Requested.
		@return A Cancellation Has Been Requested for This Execution
	  */
	public boolean isCancelRequested () 
	{
		Object oo = get_Value(COLUMNNAME_IsCancelRequested);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Lease Expiration.
		@param LeaseExpiration 
		Date and Time When the Worker Lease Expires
	  */
	public void setLeaseExpiration (Timestamp LeaseExpiration)
	{
		set_Value (COLUMNNAME_LeaseExpiration, LeaseExpiration);
	}

	/** Get Lease Expiration.
		@return Date and Time When the Worker Lease Expires
	  */
	public Timestamp getLeaseExpiration () 
	{
		return (Timestamp)get_Value(COLUMNNAME_LeaseExpiration);
	}

	/** Set Maximum Retries.
		@param MaxRetries 
		Maximum Number of Retries Allowed for This Execution
	  */
	public void setMaxRetries (int MaxRetries)
	{
		set_Value (COLUMNNAME_MaxRetries, Integer.valueOf(MaxRetries));
	}

	/** Get Maximum Retries.
		@return Maximum Number of Retries Allowed for This Execution
	  */
	public int getMaxRetries () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_MaxRetries);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Payload.
		@param Payload 
		Canonical Request Payload in JSON Format
	  */
	public void setPayload (String Payload)
	{
		set_Value (COLUMNNAME_Payload, Payload);
	}

	/** Get Payload.
		@return Canonical Request Payload in JSON Format
	  */
	public String getPayload () 
	{
		return (String)get_Value(COLUMNNAME_Payload);
	}

	/** ProcessExecutionStatus AD_Reference_ID=54734 */
	public static final int PROCESSEXECUTIONSTATUS_AD_Reference_ID=54734;
	/** Queued = Q */
	public static final String PROCESSEXECUTIONSTATUS_Queued = "Q";
	/** Running = R */
	public static final String PROCESSEXECUTIONSTATUS_Running = "R";
	/** Completed = O */
	public static final String PROCESSEXECUTIONSTATUS_Completed = "O";
	/** Error = E */
	public static final String PROCESSEXECUTIONSTATUS_Error = "E";
	/** Cancelled = X */
	public static final String PROCESSEXECUTIONSTATUS_Cancelled = "X";
	/** Set Process Execution Status.
		@param ProcessExecutionStatus 
		Current Status of the Execution Request
	  */
	public void setProcessExecutionStatus (String ProcessExecutionStatus)
	{

		set_Value (COLUMNNAME_ProcessExecutionStatus, ProcessExecutionStatus);
	}

	/** Get Process Execution Status.
		@return Current Status of the Execution Request
	  */
	public String getProcessExecutionStatus () 
	{
		return (String)get_Value(COLUMNNAME_ProcessExecutionStatus);
	}

	/** Set Record ID.
		@param Record_ID 
		Direct internal record ID
	  */
	public void setRecord_ID (int Record_ID)
	{
		if (Record_ID < 0) 
			set_Value (COLUMNNAME_Record_ID, null);
		else 
			set_Value (COLUMNNAME_Record_ID, Integer.valueOf(Record_ID));
	}

	/** Get Record ID.
		@return Direct internal record ID
	  */
	public int getRecord_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Record_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Request Hash.
		@param RequestHash Request Hash	  */
	public void setRequestHash (String RequestHash)
	{
		set_Value (COLUMNNAME_RequestHash, RequestHash);
	}

	/** Get Request Hash.
		@return Request Hash	  */
	public String getRequestHash () 
	{
		return (String)get_Value(COLUMNNAME_RequestHash);
	}

	/** Set Retry Count.
		@param RetryCount 
		Number of Times the Execution Has Been Retried
	  */
	public void setRetryCount (int RetryCount)
	{
		set_Value (COLUMNNAME_RetryCount, Integer.valueOf(RetryCount));
	}

	/** Get Retry Count.
		@return Number of Times the Execution Has Been Retried
	  */
	public int getRetryCount () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_RetryCount);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Summary.
		@param Summary 
		Textual summary of this request
	  */
	public void setSummary (String Summary)
	{
		set_Value (COLUMNNAME_Summary, Summary);
	}

	/** Get Summary.
		@return Textual summary of this request
	  */
	public String getSummary () 
	{
		return (String)get_Value(COLUMNNAME_Summary);
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