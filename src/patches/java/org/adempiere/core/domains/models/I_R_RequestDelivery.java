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

/** Generated Interface for R_RequestDelivery
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_R_RequestDelivery 
{

    /** TableName=R_RequestDelivery */
    public static final String Table_Name = "R_RequestDelivery";

    /** AD_Table_ID=55180 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 7 - System - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(7);

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

    /** Column name DateInternalDelivery */
    public static final String COLUMNNAME_DateInternalDelivery = "DateInternalDelivery";

	/** Set End of Execution Date	  */
	public void setDateInternalDelivery (Timestamp DateInternalDelivery);

	/** Get End of Execution Date	  */
	public Timestamp getDateInternalDelivery();

    /** Column name DateRejected */
    public static final String COLUMNNAME_DateRejected = "DateRejected";

	/** Set Date Rejected.
	  * Date and time when the delivery was rejected
	  */
	public void setDateRejected (Timestamp DateRejected);

	/** Get Date Rejected.
	  * Date and time when the delivery was rejected
	  */
	public Timestamp getDateRejected();

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

    /** Column name ImageURLs */
    public static final String COLUMNNAME_ImageURLs = "ImageURLs";

	/** Set Image URLs.
	  * JSON array of image URLs related to the delivery
	  */
	public void setImageURLs (String ImageURLs);

	/** Get Image URLs.
	  * JSON array of image URLs related to the delivery
	  */
	public String getImageURLs();

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

    /** Column name IsRejected */
    public static final String COLUMNNAME_IsRejected = "IsRejected";

	/** Set Rejected.
	  * Indicates whether the delivery was rejected
	  */
	public void setIsRejected (boolean IsRejected);

	/** Get Rejected.
	  * Indicates whether the delivery was rejected
	  */
	public boolean isRejected();

    /** Column name RejectedBy_ID */
    public static final String COLUMNNAME_RejectedBy_ID = "RejectedBy_ID";

	/** Set Rejected By.
	  * User who rejected the delivery
	  */
	public void setRejectedBy_ID (int RejectedBy_ID);

	/** Get Rejected By.
	  * User who rejected the delivery
	  */
	public int getRejectedBy_ID();

	public I_AD_User getRejectedBy() throws RuntimeException;

    /** Column name RejectionReason */
    public static final String COLUMNNAME_RejectionReason = "RejectionReason";

	/** Set Rejection Reason.
	  * Reason why the delivery was rejected
	  */
	public void setRejectionReason (String RejectionReason);

	/** Get Rejection Reason.
	  * Reason why the delivery was rejected
	  */
	public String getRejectionReason();

    /** Column name R_RequestDelivery_ID */
    public static final String COLUMNNAME_R_RequestDelivery_ID = "R_RequestDelivery_ID";

	/** Set Request Delivery	  */
	public void setR_RequestDelivery_ID (int R_RequestDelivery_ID);

	/** Get Request Delivery	  */
	public int getR_RequestDelivery_ID();

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

    /** Column name R_RequestUpdate_ID */
    public static final String COLUMNNAME_R_RequestUpdate_ID = "R_RequestUpdate_ID";

	/** Set Request Update.
	  * Request Updates
	  */
	public void setR_RequestUpdate_ID (int R_RequestUpdate_ID);

	/** Get Request Update.
	  * Request Updates
	  */
	public int getR_RequestUpdate_ID();

	public I_R_RequestUpdate getR_RequestUpdate() throws RuntimeException;

    /** Column name SolutionSummary */
    public static final String COLUMNNAME_SolutionSummary = "SolutionSummary";

	/** Set Solution Summary.
	  * Summary of the solution provided
	  */
	public void setSolutionSummary (String SolutionSummary);

	/** Get Solution Summary.
	  * Summary of the solution provided
	  */
	public String getSolutionSummary();

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

    /** Column name VideoLinks */
    public static final String COLUMNNAME_VideoLinks = "VideoLinks";

	/** Set Video Links.
	  * JSON array of video links related to the delivery
	  */
	public void setVideoLinks (String VideoLinks);

	/** Get Video Links.
	  * JSON array of video links related to the delivery
	  */
	public String getVideoLinks();
}
