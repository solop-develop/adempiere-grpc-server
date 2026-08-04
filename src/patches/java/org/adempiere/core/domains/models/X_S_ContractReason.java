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
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.KeyNamePair;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for S_ContractReason
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_S_ContractReason extends PO implements I_S_ContractReason, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20230828L;

    /** Standard Constructor */
    public X_S_ContractReason(Properties ctx, int S_ContractReason_ID, String trxName)
    {
      super (ctx, S_ContractReason_ID, trxName);
      /** if (S_ContractReason_ID == 0)
        {
			setIsFinalClose (false);
// N
			setName (null);
			setS_ContractReason_ID (0);
        } */
    }

    /** Load Constructor */
    public X_S_ContractReason(Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_S_ContractReason[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set Final Close.
		@param IsFinalClose 
		Entries with Final Close cannot be re-opened
	  */
	public void setIsFinalClose (boolean IsFinalClose)
	{
		set_Value (COLUMNNAME_IsFinalClose, Boolean.valueOf(IsFinalClose));
	}

	/** Get Final Close.
		@return Entries with Final Close cannot be re-opened
	  */
	public boolean isFinalClose () 
	{
		Object oo = get_Value(COLUMNNAME_IsFinalClose);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set S_ContractReason.
		@param S_ContractReason_ID S_ContractReason	  */
	public void setS_ContractReason_ID (int S_ContractReason_ID)
	{
		if (S_ContractReason_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_S_ContractReason_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_S_ContractReason_ID, Integer.valueOf(S_ContractReason_ID));
	}

	/** Get S_ContractReason.
		@return S_ContractReason	  */
	public int getS_ContractReason_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_S_ContractReason_ID);
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