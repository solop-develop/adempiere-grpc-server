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

/** Generated Model for AD_TokenProfileScope
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_TokenProfileScope extends PO implements I_AD_TokenProfileScope, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260625L;

    /** Standard Constructor */
    public X_AD_TokenProfileScope (Properties ctx, int AD_TokenProfileScope_ID, String trxName)
    {
      super (ctx, AD_TokenProfileScope_ID, trxName);
      /** if (AD_TokenProfileScope_ID == 0)
        {
			setAD_TokenProfile_ID (0);
			setAD_TokenProfileScope_ID (0);
			setAD_TokenScope_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_TokenProfileScope (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_AD_TokenProfileScope[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_AD_TokenProfile getAD_TokenProfile() throws RuntimeException
    {
		return (I_AD_TokenProfile)MTable.get(getCtx(), I_AD_TokenProfile.Table_Name)
			.getPO(getAD_TokenProfile_ID(), get_TrxName());	}

	/** Set Token Profile.
		@param AD_TokenProfile_ID Token Profile	  */
	public void setAD_TokenProfile_ID (int AD_TokenProfile_ID)
	{
		if (AD_TokenProfile_ID < 1) 
			set_Value (COLUMNNAME_AD_TokenProfile_ID, null);
		else 
			set_Value (COLUMNNAME_AD_TokenProfile_ID, Integer.valueOf(AD_TokenProfile_ID));
	}

	/** Get Token Profile.
		@return Token Profile	  */
	public int getAD_TokenProfile_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_TokenProfile_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Token Profile Scope.
		@param AD_TokenProfileScope_ID Token Profile Scope	  */
	public void setAD_TokenProfileScope_ID (int AD_TokenProfileScope_ID)
	{
		if (AD_TokenProfileScope_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_TokenProfileScope_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_TokenProfileScope_ID, Integer.valueOf(AD_TokenProfileScope_ID));
	}

	/** Get Token Profile Scope.
		@return Token Profile Scope	  */
	public int getAD_TokenProfileScope_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_TokenProfileScope_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_TokenScope getAD_TokenScope() throws RuntimeException
    {
		return (I_AD_TokenScope)MTable.get(getCtx(), I_AD_TokenScope.Table_Name)
			.getPO(getAD_TokenScope_ID(), get_TrxName());	}

	/** Set Token Scope.
		@param AD_TokenScope_ID Token Scope	  */
	public void setAD_TokenScope_ID (int AD_TokenScope_ID)
	{
		if (AD_TokenScope_ID < 1) 
			set_Value (COLUMNNAME_AD_TokenScope_ID, null);
		else 
			set_Value (COLUMNNAME_AD_TokenScope_ID, Integer.valueOf(AD_TokenScope_ID));
	}

	/** Get Token Scope.
		@return Token Scope	  */
	public int getAD_TokenScope_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_TokenScope_ID);
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