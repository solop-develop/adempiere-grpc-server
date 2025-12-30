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

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for R_Release
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_R_Release extends PO implements I_R_Release, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251223L;

    /** Standard Constructor */
    public X_R_Release (Properties ctx, int R_Release_ID, String trxName)
    {
      super (ctx, R_Release_ID, trxName);
      /** if (R_Release_ID == 0)
        {
			setMemo (null);
			setR_Release_ID (0);
			setTitle (null);
        } */
    }

    /** Load Constructor */
    public X_R_Release (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_R_Release[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Memo.
		@param Memo 
		Memo Text
	  */
	public void setMemo (String Memo)
	{
		set_Value (COLUMNNAME_Memo, Memo);
	}

	/** Get Memo.
		@return Memo Text
	  */
	public String getMemo () 
	{
		return (String)get_Value(COLUMNNAME_Memo);
	}

	/** ReleaseType AD_Reference_ID=54537 */
	public static final int RELEASETYPE_AD_Reference_ID=54537;
	/** Stable = ST */
	public static final String RELEASETYPE_Stable = "ST";
	/** Test = TS */
	public static final String RELEASETYPE_Test = "TS";
	/** Set Release Type.
		@param ReleaseType Release Type	  */
	public void setReleaseType (String ReleaseType)
	{

		set_Value (COLUMNNAME_ReleaseType, ReleaseType);
	}

	/** Get Release Type.
		@return Release Type	  */
	public String getReleaseType () 
	{
		return (String)get_Value(COLUMNNAME_ReleaseType);
	}

	/** Set Release.
		@param R_Release_ID Release	  */
	public void setR_Release_ID (int R_Release_ID)
	{
		if (R_Release_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_R_Release_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_R_Release_ID, Integer.valueOf(R_Release_ID));
	}

	/** Get Release.
		@return Release	  */
	public int getR_Release_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_Release_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Title.
		@param Title 
		Name this entity is referred to as
	  */
	public void setTitle (String Title)
	{
		set_Value (COLUMNNAME_Title, Title);
	}

	/** Get Title.
		@return Name this entity is referred to as
	  */
	public String getTitle () 
	{
		return (String)get_Value(COLUMNNAME_Title);
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