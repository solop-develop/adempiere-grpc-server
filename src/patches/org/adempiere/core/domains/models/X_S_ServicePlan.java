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

/** Generated Model for S_ServicePlan
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_S_ServicePlan extends PO implements I_S_ServicePlan, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260311L;

    /** Standard Constructor */
    public X_S_ServicePlan (Properties ctx, int S_ServicePlan_ID, String trxName)
    {
      super (ctx, S_ServicePlan_ID, trxName);
      /** if (S_ServicePlan_ID == 0)
        {
			setDocStatus (null);
// DR
			setFrequencyType (null);
// W
			setIsBillable (true);
// Y
			setS_ServicePlan_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
// @#Date@
        } */
    }

    /** Load Constructor */
    public X_S_ServicePlan (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_S_ServicePlan[")
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

	/** DocStatus AD_Reference_ID=131 */
	public static final int DOCSTATUS_AD_Reference_ID=131;
	/** Drafted = DR */
	public static final String DOCSTATUS_Drafted = "DR";
	/** Completed = CO */
	public static final String DOCSTATUS_Completed = "CO";
	/** Approved = AP */
	public static final String DOCSTATUS_Approved = "AP";
	/** Not Approved = NA */
	public static final String DOCSTATUS_NotApproved = "NA";
	/** Voided = VO */
	public static final String DOCSTATUS_Voided = "VO";
	/** Invalid = IN */
	public static final String DOCSTATUS_Invalid = "IN";
	/** Reversed = RE */
	public static final String DOCSTATUS_Reversed = "RE";
	/** Closed = CL */
	public static final String DOCSTATUS_Closed = "CL";
	/** Unknown = ?? */
	public static final String DOCSTATUS_Unknown = "??";
	/** In Progress = IP */
	public static final String DOCSTATUS_InProgress = "IP";
	/** Waiting Payment = WP */
	public static final String DOCSTATUS_WaitingPayment = "WP";
	/** Waiting Confirmation = WC */
	public static final String DOCSTATUS_WaitingConfirmation = "WC";
	/** Set Document Status.
		@param DocStatus 
		The current status of the document
	  */
	public void setDocStatus (String DocStatus)
	{

		set_Value (COLUMNNAME_DocStatus, DocStatus);
	}

	/** Get Document Status.
		@return The current status of the document
	  */
	public String getDocStatus () 
	{
		return (String)get_Value(COLUMNNAME_DocStatus);
	}

	/** FrequencyType AD_Reference_ID=54622 */
	public static final int FREQUENCYTYPE_AD_Reference_ID=54622;
	/** Weekly = W */
	public static final String FREQUENCYTYPE_Weekly = "W";
	/** Biweekly = BW */
	public static final String FREQUENCYTYPE_Biweekly = "BW";
	/** Monthly = M */
	public static final String FREQUENCYTYPE_Monthly = "M";
	/** One-time = O */
	public static final String FREQUENCYTYPE_One_Time = "O";
	/** Set Frequency Type.
		@param FrequencyType 
		Frequency of event
	  */
	public void setFrequencyType (String FrequencyType)
	{

		set_Value (COLUMNNAME_FrequencyType, FrequencyType);
	}

	/** Get Frequency Type.
		@return Frequency of event
	  */
	public String getFrequencyType () 
	{
		return (String)get_Value(COLUMNNAME_FrequencyType);
	}

	/** Set Billable.
		@param IsBillable 
		Whether hours from this plan are billed to the client
	  */
	public void setIsBillable (boolean IsBillable)
	{
		set_Value (COLUMNNAME_IsBillable, Boolean.valueOf(IsBillable));
	}

	/** Get Billable.
		@return Whether hours from this plan are billed to the client
	  */
	public boolean isBillable () 
	{
		Object oo = get_Value(COLUMNNAME_IsBillable);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_S_Contract getS_Contract() throws RuntimeException
    {
		return (I_S_Contract)MTable.get(getCtx(), I_S_Contract.Table_Name)
			.getPO(getS_Contract_ID(), get_TrxName());	}

	/** Set Contract.
		@param S_Contract_ID 
		Contract
	  */
	public void setS_Contract_ID (int S_Contract_ID)
	{
		if (S_Contract_ID < 1) 
			set_Value (COLUMNNAME_S_Contract_ID, null);
		else 
			set_Value (COLUMNNAME_S_Contract_ID, Integer.valueOf(S_Contract_ID));
	}

	/** Get Contract.
		@return Contract
	  */
	public int getS_Contract_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_Contract_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_S_ResourceType getS_ResourceType() throws RuntimeException
    {
		return (I_S_ResourceType)MTable.get(getCtx(), I_S_ResourceType.Table_Name)
			.getPO(getS_ResourceType_ID(), get_TrxName());	}

	/** Set Resource Type.
		@param S_ResourceType_ID Resource Type	  */
	public void setS_ResourceType_ID (int S_ResourceType_ID)
	{
		if (S_ResourceType_ID < 1) 
			set_Value (COLUMNNAME_S_ResourceType_ID, null);
		else 
			set_Value (COLUMNNAME_S_ResourceType_ID, Integer.valueOf(S_ResourceType_ID));
	}

	/** Get Resource Type.
		@return Resource Type	  */
	public int getS_ResourceType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_ResourceType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

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

	/** Set Total Weekly Hours.
		@param TotalWeeklyHours 
		Sum of planned hours per week for this service plan
	  */
	public void setTotalWeeklyHours (BigDecimal TotalWeeklyHours)
	{
		set_Value (COLUMNNAME_TotalWeeklyHours, TotalWeeklyHours);
	}

	/** Get Total Weekly Hours.
		@return Sum of planned hours per week for this service plan
	  */
	public BigDecimal getTotalWeeklyHours () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalWeeklyHours);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Valid from.
		@param ValidFrom 
		Valid from including this date (first day)
	  */
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_Value (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}

	/** Set Valid to.
		@param ValidTo 
		Valid to including this date (last day)
	  */
	public void setValidTo (Timestamp ValidTo)
	{
		set_Value (COLUMNNAME_ValidTo, ValidTo);
	}

	/** Get Valid to.
		@return Valid to including this date (last day)
	  */
	public Timestamp getValidTo () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidTo);
	}
}