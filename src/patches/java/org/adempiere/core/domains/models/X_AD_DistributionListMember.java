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
import java.util.Properties;

/** Generated Model for AD_DistributionListMember
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_DistributionListMember extends PO implements I_AD_DistributionListMember, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260622L;

    /** Standard Constructor */
    public X_AD_DistributionListMember (Properties ctx, int AD_DistributionListMember_ID, String trxName)
    {
      super (ctx, AD_DistributionListMember_ID, trxName);
      /** if (AD_DistributionListMember_ID == 0)
        {
			setAD_DistributionList_ID (0);
			setAD_DistributionListMember_ID (0);
			setMemberType (null);
        } */
    }

    /** Load Constructor */
    public X_AD_DistributionListMember (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_AD_DistributionListMember[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_DistributionList getAD_DistributionList() throws RuntimeException
    {
		return (I_AD_DistributionList)MTable.get(getCtx(), I_AD_DistributionList.Table_Name)
			.getPO(getAD_DistributionList_ID(), get_TrxName());	}

	/** Set Distribution List.
		@param AD_DistributionList_ID Distribution List	  */
	public void setAD_DistributionList_ID (int AD_DistributionList_ID)
	{
		if (AD_DistributionList_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_DistributionList_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_DistributionList_ID, Integer.valueOf(AD_DistributionList_ID));
	}

	/** Get Distribution List.
		@return Distribution List	  */
	public int getAD_DistributionList_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_DistributionList_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Distribution List Member.
		@param AD_DistributionListMember_ID Distribution List Member	  */
	public void setAD_DistributionListMember_ID (int AD_DistributionListMember_ID)
	{
		if (AD_DistributionListMember_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_DistributionListMember_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_DistributionListMember_ID, Integer.valueOf(AD_DistributionListMember_ID));
	}

	/** Get Distribution List Member.
		@return Distribution List Member	  */
	public int getAD_DistributionListMember_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_DistributionListMember_ID);
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

	/** MemberType AD_Reference_ID=54717 */
	public static final int MEMBERTYPE_AD_Reference_ID=54717;
	/** User = U */
	public static final String MEMBERTYPE_User = "U";
	/** List = L */
	public static final String MEMBERTYPE_List = "L";
	/** Role = R */
	public static final String MEMBERTYPE_Role = "R";
	/** Set Member Type.
		@param MemberType 
		Type of member: U=User, L=List, R=Role.
	  */
	public void setMemberType (String MemberType)
	{

		set_Value (COLUMNNAME_MemberType, MemberType);
	}

	/** Get Member Type.
		@return Type of member: U=User, L=List, R=Role.
	  */
	public String getMemberType () 
	{
		return (String)get_Value(COLUMNNAME_MemberType);
	}

	public I_AD_DistributionList getRef_DistributionList() throws RuntimeException
    {
		return (I_AD_DistributionList)MTable.get(getCtx(), I_AD_DistributionList.Table_Name)
			.getPO(getRef_DistributionList_ID(), get_TrxName());	}

	/** Set Referenced Distribution List.
		@param Ref_DistributionList_ID 
		Nested sublist. Populated only when MemberType is L.
	  */
	public void setRef_DistributionList_ID (int Ref_DistributionList_ID)
	{
		if (Ref_DistributionList_ID < 1) 
			set_Value (COLUMNNAME_Ref_DistributionList_ID, null);
		else 
			set_Value (COLUMNNAME_Ref_DistributionList_ID, Integer.valueOf(Ref_DistributionList_ID));
	}

	/** Get Referenced Distribution List.
		@return Nested sublist. Populated only when MemberType is L.
	  */
	public int getRef_DistributionList_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Ref_DistributionList_ID);
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