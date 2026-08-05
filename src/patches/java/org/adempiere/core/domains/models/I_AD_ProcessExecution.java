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

/** Generated Interface for AD_ProcessExecution
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_AD_ProcessExecution 
{

    /** TableName=AD_ProcessExecution */
    public static final String Table_Name = "AD_ProcessExecution";

    /** AD_Table_ID=55193 */
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

    /** Column name AD_PInstance_ID */
    public static final String COLUMNNAME_AD_PInstance_ID = "AD_PInstance_ID";

	/** Set Process Instance.
	  * Instance of the process
	  */
	public void setAD_PInstance_ID (int AD_PInstance_ID);

	/** Get Process Instance.
	  * Instance of the process
	  */
	public int getAD_PInstance_ID();

	public I_AD_PInstance getAD_PInstance() throws RuntimeException;

    /** Column name AD_ProcessExecution_ID */
    public static final String COLUMNNAME_AD_ProcessExecution_ID = "AD_ProcessExecution_ID";

	/** Set Process Execution.
	  * Idempotent Execution Request for an ADempiere Process
	  */
	public void setAD_ProcessExecution_ID (int AD_ProcessExecution_ID);

	/** Get Process Execution.
	  * Idempotent Execution Request for an ADempiere Process
	  */
	public int getAD_ProcessExecution_ID();

    /** Column name AD_Process_ID */
    public static final String COLUMNNAME_AD_Process_ID = "AD_Process_ID";

	/** Set Process.
	  * Process or Report
	  */
	public void setAD_Process_ID (int AD_Process_ID);

	/** Get Process.
	  * Process or Report
	  */
	public int getAD_Process_ID();

	public I_AD_Process getAD_Process() throws RuntimeException;

    /** Column name AD_Role_ID */
    public static final String COLUMNNAME_AD_Role_ID = "AD_Role_ID";

	/** Set Role.
	  * Responsibility Role
	  */
	public void setAD_Role_ID (int AD_Role_ID);

	/** Get Role.
	  * Responsibility Role
	  */
	public int getAD_Role_ID();

	public I_AD_Role getAD_Role() throws RuntimeException;

    /** Column name AD_Scheduler_ID */
    public static final String COLUMNNAME_AD_Scheduler_ID = "AD_Scheduler_ID";

	/** Set Scheduler.
	  * Schedule Processes
	  */
	public void setAD_Scheduler_ID (int AD_Scheduler_ID);

	/** Get Scheduler.
	  * Schedule Processes
	  */
	public int getAD_Scheduler_ID();

	public I_AD_Scheduler getAD_Scheduler() throws RuntimeException;

    /** Column name AD_Table_ID */
    public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	/** Set Table.
	  * Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID);

	/** Get Table.
	  * Database Table information
	  */
	public int getAD_Table_ID();

	public I_AD_Table getAD_Table() throws RuntimeException;

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	public I_AD_User getAD_User() throws RuntimeException;

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

    /** Column name DateFinished */
    public static final String COLUMNNAME_DateFinished = "DateFinished";

	/** Set Date Finished.
	  * Date and Time When the Execution Finished
	  */
	public void setDateFinished (Timestamp DateFinished);

	/** Get Date Finished.
	  * Date and Time When the Execution Finished
	  */
	public Timestamp getDateFinished();

    /** Column name DateStarted */
    public static final String COLUMNNAME_DateStarted = "DateStarted";

	/** Set Date Started.
	  * Date and Time When the Execution Started
	  */
	public void setDateStarted (Timestamp DateStarted);

	/** Get Date Started.
	  * Date and Time When the Execution Started
	  */
	public Timestamp getDateStarted();

    /** Column name ErrorMsg */
    public static final String COLUMNNAME_ErrorMsg = "ErrorMsg";

	/** Set Error Msg	  */
	public void setErrorMsg (String ErrorMsg);

	/** Get Error Msg	  */
	public String getErrorMsg();

    /** Column name ExecutionMode */
    public static final String COLUMNNAME_ExecutionMode = "ExecutionMode";

	/** Set Execution Mode.
	  * Mode That Determines Whether the Process Runs Synchronously or Asynchronously
	  */
	public void setExecutionMode (String ExecutionMode);

	/** Get Execution Mode.
	  * Mode That Determines Whether the Process Runs Synchronously or Asynchronously
	  */
	public String getExecutionMode();

    /** Column name IdempotencyKey */
    public static final String COLUMNNAME_IdempotencyKey = "IdempotencyKey";

	/** Set Idempotency Key.
	  * Client Supplied Key Used to Deduplicate Execution Requests
	  */
	public void setIdempotencyKey (String IdempotencyKey);

	/** Get Idempotency Key.
	  * Client Supplied Key Used to Deduplicate Execution Requests
	  */
	public String getIdempotencyKey();

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

    /** Column name IsCancelRequested */
    public static final String COLUMNNAME_IsCancelRequested = "IsCancelRequested";

	/** Set Cancel Requested.
	  * A Cancellation Has Been Requested for This Execution
	  */
	public void setIsCancelRequested (boolean IsCancelRequested);

	/** Get Cancel Requested.
	  * A Cancellation Has Been Requested for This Execution
	  */
	public boolean isCancelRequested();

    /** Column name LeaseExpiration */
    public static final String COLUMNNAME_LeaseExpiration = "LeaseExpiration";

	/** Set Lease Expiration.
	  * Date and Time When the Worker Lease Expires
	  */
	public void setLeaseExpiration (Timestamp LeaseExpiration);

	/** Get Lease Expiration.
	  * Date and Time When the Worker Lease Expires
	  */
	public Timestamp getLeaseExpiration();

    /** Column name MaxRetries */
    public static final String COLUMNNAME_MaxRetries = "MaxRetries";

	/** Set Maximum Retries.
	  * Maximum Number of Retries Allowed for This Execution
	  */
	public void setMaxRetries (int MaxRetries);

	/** Get Maximum Retries.
	  * Maximum Number of Retries Allowed for This Execution
	  */
	public int getMaxRetries();

    /** Column name Payload */
    public static final String COLUMNNAME_Payload = "Payload";

	/** Set Payload.
	  * Canonical Request Payload in JSON Format
	  */
	public void setPayload (String Payload);

	/** Get Payload.
	  * Canonical Request Payload in JSON Format
	  */
	public String getPayload();

    /** Column name ProcessExecutionStatus */
    public static final String COLUMNNAME_ProcessExecutionStatus = "ProcessExecutionStatus";

	/** Set Process Execution Status.
	  * Current Status of the Execution Request
	  */
	public void setProcessExecutionStatus (String ProcessExecutionStatus);

	/** Get Process Execution Status.
	  * Current Status of the Execution Request
	  */
	public String getProcessExecutionStatus();

    /** Column name Record_ID */
    public static final String COLUMNNAME_Record_ID = "Record_ID";

	/** Set Record ID.
	  * Direct internal record ID
	  */
	public void setRecord_ID (int Record_ID);

	/** Get Record ID.
	  * Direct internal record ID
	  */
	public int getRecord_ID();

    /** Column name RequestHash */
    public static final String COLUMNNAME_RequestHash = "RequestHash";

	/** Set Request Hash	  */
	public void setRequestHash (String RequestHash);

	/** Get Request Hash	  */
	public String getRequestHash();

    /** Column name RetryCount */
    public static final String COLUMNNAME_RetryCount = "RetryCount";

	/** Set Retry Count.
	  * Number of Times the Execution Has Been Retried
	  */
	public void setRetryCount (int RetryCount);

	/** Get Retry Count.
	  * Number of Times the Execution Has Been Retried
	  */
	public int getRetryCount();

    /** Column name Summary */
    public static final String COLUMNNAME_Summary = "Summary";

	/** Set Summary.
	  * Textual summary of this request
	  */
	public void setSummary (String Summary);

	/** Get Summary.
	  * Textual summary of this request
	  */
	public String getSummary();

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
