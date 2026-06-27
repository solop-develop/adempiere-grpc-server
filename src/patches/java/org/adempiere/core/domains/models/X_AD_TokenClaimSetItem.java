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

/** Generated Model for AD_TokenClaimSetItem
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_TokenClaimSetItem extends PO implements I_AD_TokenClaimSetItem, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260626L;

    /** Standard Constructor */
    public X_AD_TokenClaimSetItem (Properties ctx, int AD_TokenClaimSetItem_ID, String trxName)
    {
      super (ctx, AD_TokenClaimSetItem_ID, trxName);
      /** if (AD_TokenClaimSetItem_ID == 0)
        {
			setAD_ClaimValueType (null);
// S
			setAD_TokenClaimSet_ID (0);
			setAD_TokenClaimSetItem_ID (0);
			setClaimKey (null);
			setClaimValue (null);
        } */
    }

    /** Load Constructor */
    public X_AD_TokenClaimSetItem (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_AD_TokenClaimSetItem[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** AD_ClaimValueType AD_Reference_ID=54724 */
	public static final int AD_CLAIMVALUETYPE_AD_Reference_ID=54724;
	/** String = S */
	public static final String AD_CLAIMVALUETYPE_String = "S";
	/** Number = N */
	public static final String AD_CLAIMVALUETYPE_Number = "N";
	/** Boolean = B */
	public static final String AD_CLAIMVALUETYPE_Boolean = "B";
	/** JSON = J */
	public static final String AD_CLAIMVALUETYPE_JSON = "J";
	/** Set Claim Value Type.
		@param AD_ClaimValueType 
		Data type used to serialize the claim value in the JWT
	  */
	public void setAD_ClaimValueType (String AD_ClaimValueType)
	{

		set_Value (COLUMNNAME_AD_ClaimValueType, AD_ClaimValueType);
	}

	/** Get Claim Value Type.
		@return Data type used to serialize the claim value in the JWT
	  */
	public String getAD_ClaimValueType () 
	{
		return (String)get_Value(COLUMNNAME_AD_ClaimValueType);
	}

	public I_AD_TokenClaimSet getAD_TokenClaimSet() throws RuntimeException
    {
		return (I_AD_TokenClaimSet)MTable.get(getCtx(), I_AD_TokenClaimSet.Table_Name)
			.getPO(getAD_TokenClaimSet_ID(), get_TrxName());	}

	/** Set Token Claim Set.
		@param AD_TokenClaimSet_ID Token Claim Set	  */
	public void setAD_TokenClaimSet_ID (int AD_TokenClaimSet_ID)
	{
		if (AD_TokenClaimSet_ID < 1) 
			set_Value (COLUMNNAME_AD_TokenClaimSet_ID, null);
		else 
			set_Value (COLUMNNAME_AD_TokenClaimSet_ID, Integer.valueOf(AD_TokenClaimSet_ID));
	}

	/** Get Token Claim Set.
		@return Token Claim Set	  */
	public int getAD_TokenClaimSet_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_TokenClaimSet_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Token Claim Set Item.
		@param AD_TokenClaimSetItem_ID Token Claim Set Item	  */
	public void setAD_TokenClaimSetItem_ID (int AD_TokenClaimSetItem_ID)
	{
		if (AD_TokenClaimSetItem_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_TokenClaimSetItem_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_TokenClaimSetItem_ID, Integer.valueOf(AD_TokenClaimSetItem_ID));
	}

	/** Get Token Claim Set Item.
		@return Token Claim Set Item	  */
	public int getAD_TokenClaimSetItem_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_TokenClaimSetItem_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Claim Key.
		@param ClaimKey 
		Key of the additional claim (must be namespaced)
	  */
	public void setClaimKey (String ClaimKey)
	{
		set_Value (COLUMNNAME_ClaimKey, ClaimKey);
	}

	/** Get Claim Key.
		@return Key of the additional claim (must be namespaced)
	  */
	public String getClaimKey () 
	{
		return (String)get_Value(COLUMNNAME_ClaimKey);
	}

	/** Set Claim Value.
		@param ClaimValue 
		Value of the additional claim, interpreted per value type
	  */
	public void setClaimValue (String ClaimValue)
	{
		set_Value (COLUMNNAME_ClaimValue, ClaimValue);
	}

	/** Get Claim Value.
		@return Value of the additional claim, interpreted per value type
	  */
	public String getClaimValue () 
	{
		return (String)get_Value(COLUMNNAME_ClaimValue);
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