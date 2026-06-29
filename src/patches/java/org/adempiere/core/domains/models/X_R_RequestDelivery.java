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

/** Generated Model for R_RequestDelivery
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_R_RequestDelivery extends PO implements I_R_RequestDelivery, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260629L;

    /** Standard Constructor */
    public X_R_RequestDelivery (Properties ctx, int R_RequestDelivery_ID, String trxName)
    {
      super (ctx, R_RequestDelivery_ID, trxName);
      /** if (R_RequestDelivery_ID == 0)
        {
			setAD_User_ID (0);
			setDescription (null);
			setIsRejected (false);
// N
			setR_RequestDelivery_ID (0);
			setR_Request_ID (0);
        } */
    }

    /** Load Constructor */
    public X_R_RequestDelivery (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 7 - System - Client - Org 
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
      StringBuffer sb = new StringBuffer ("X_R_RequestDelivery[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set End of Execution Date.
		@param DateInternalDelivery End of Execution Date	  */
	public void setDateInternalDelivery (Timestamp DateInternalDelivery)
	{
		set_Value (COLUMNNAME_DateInternalDelivery, DateInternalDelivery);
	}

	/** Get End of Execution Date.
		@return End of Execution Date	  */
	public Timestamp getDateInternalDelivery () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateInternalDelivery);
	}

	/** Set Date Rejected.
		@param DateRejected 
		Date and time when the delivery was rejected
	  */
	public void setDateRejected (Timestamp DateRejected)
	{
		set_Value (COLUMNNAME_DateRejected, DateRejected);
	}

	/** Get Date Rejected.
		@return Date and time when the delivery was rejected
	  */
	public Timestamp getDateRejected () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateRejected);
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

	/** Set Image URLs.
		@param ImageURLs 
		JSON array of image URLs related to the delivery
	  */
	public void setImageURLs (String ImageURLs)
	{
		set_Value (COLUMNNAME_ImageURLs, ImageURLs);
	}

	/** Get Image URLs.
		@return JSON array of image URLs related to the delivery
	  */
	public String getImageURLs () 
	{
		return (String)get_Value(COLUMNNAME_ImageURLs);
	}

	/** Set Rejected.
		@param IsRejected 
		Indicates whether the delivery was rejected
	  */
	public void setIsRejected (boolean IsRejected)
	{
		set_Value (COLUMNNAME_IsRejected, Boolean.valueOf(IsRejected));
	}

	/** Get Rejected.
		@return Indicates whether the delivery was rejected
	  */
	public boolean isRejected () 
	{
		Object oo = get_Value(COLUMNNAME_IsRejected);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_AD_User getRejectedBy() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getRejectedBy_ID(), get_TrxName());	}

	/** Set Rejected By.
		@param RejectedBy_ID 
		User who rejected the delivery
	  */
	public void setRejectedBy_ID (int RejectedBy_ID)
	{
		if (RejectedBy_ID < 1) 
			set_Value (COLUMNNAME_RejectedBy_ID, null);
		else 
			set_Value (COLUMNNAME_RejectedBy_ID, Integer.valueOf(RejectedBy_ID));
	}

	/** Get Rejected By.
		@return User who rejected the delivery
	  */
	public int getRejectedBy_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_RejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rejection Reason.
		@param RejectionReason 
		Reason why the delivery was rejected
	  */
	public void setRejectionReason (String RejectionReason)
	{
		set_Value (COLUMNNAME_RejectionReason, RejectionReason);
	}

	/** Get Rejection Reason.
		@return Reason why the delivery was rejected
	  */
	public String getRejectionReason () 
	{
		return (String)get_Value(COLUMNNAME_RejectionReason);
	}

	/** Set Request Delivery.
		@param R_RequestDelivery_ID Request Delivery	  */
	public void setR_RequestDelivery_ID (int R_RequestDelivery_ID)
	{
		if (R_RequestDelivery_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_R_RequestDelivery_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_R_RequestDelivery_ID, Integer.valueOf(R_RequestDelivery_ID));
	}

	/** Get Request Delivery.
		@return Request Delivery	  */
	public int getR_RequestDelivery_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_RequestDelivery_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_R_RequestUpdate getR_RequestUpdate() throws RuntimeException
    {
		return (I_R_RequestUpdate)MTable.get(getCtx(), I_R_RequestUpdate.Table_Name)
			.getPO(getR_RequestUpdate_ID(), get_TrxName());	}

	/** Set Request Update.
		@param R_RequestUpdate_ID 
		Request Updates
	  */
	public void setR_RequestUpdate_ID (int R_RequestUpdate_ID)
	{
		if (R_RequestUpdate_ID < 1) 
			set_Value (COLUMNNAME_R_RequestUpdate_ID, null);
		else 
			set_Value (COLUMNNAME_R_RequestUpdate_ID, Integer.valueOf(R_RequestUpdate_ID));
	}

	/** Get Request Update.
		@return Request Updates
	  */
	public int getR_RequestUpdate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_RequestUpdate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Solution Summary.
		@param SolutionSummary 
		Summary of the solution provided
	  */
	public void setSolutionSummary (String SolutionSummary)
	{
		set_Value (COLUMNNAME_SolutionSummary, SolutionSummary);
	}

	/** Get Solution Summary.
		@return Summary of the solution provided
	  */
	public String getSolutionSummary () 
	{
		return (String)get_Value(COLUMNNAME_SolutionSummary);
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

	/** Set Video Links.
		@param VideoLinks 
		JSON array of video links related to the delivery
	  */
	public void setVideoLinks (String VideoLinks)
	{
		set_Value (COLUMNNAME_VideoLinks, VideoLinks);
	}

	/** Get Video Links.
		@return JSON array of video links related to the delivery
	  */
	public String getVideoLinks () 
	{
		return (String)get_Value(COLUMNNAME_VideoLinks);
	}
}