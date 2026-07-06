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

/** Generated Model for R_ReleaseRelated
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_R_ReleaseRelated extends PO implements I_R_ReleaseRelated, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260706L;

    /** Standard Constructor */
    public X_R_ReleaseRelated (Properties ctx, int R_ReleaseRelated_ID, String trxName)
    {
      super (ctx, R_ReleaseRelated_ID, trxName);
      /** if (R_ReleaseRelated_ID == 0)
        {
			setR_ReferenceRelease_ID (0);
			setR_Release_ID (0);
			setR_ReleaseRelated_ID (0);
        } */
    }

    /** Load Constructor */
    public X_R_ReleaseRelated (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_R_ReleaseRelated[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_R_Release getR_ReferenceRelease() throws RuntimeException
    {
		return (I_R_Release)MTable.get(getCtx(), I_R_Release.Table_Name)
			.getPO(getR_ReferenceRelease_ID(), get_TrxName());	}

	/** Set Reference Release.
		@param R_ReferenceRelease_ID Reference Release	  */
	public void setR_ReferenceRelease_ID (int R_ReferenceRelease_ID)
	{
		if (R_ReferenceRelease_ID < 1) 
			set_Value (COLUMNNAME_R_ReferenceRelease_ID, null);
		else 
			set_Value (COLUMNNAME_R_ReferenceRelease_ID, Integer.valueOf(R_ReferenceRelease_ID));
	}

	/** Get Reference Release.
		@return Reference Release	  */
	public int getR_ReferenceRelease_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_ReferenceRelease_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_R_Release getR_Release() throws RuntimeException
    {
		return (I_R_Release)MTable.get(getCtx(), I_R_Release.Table_Name)
			.getPO(getR_Release_ID(), get_TrxName());	}

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

	/** Set Release Related.
		@param R_ReleaseRelated_ID Release Related	  */
	public void setR_ReleaseRelated_ID (int R_ReleaseRelated_ID)
	{
		if (R_ReleaseRelated_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_R_ReleaseRelated_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_R_ReleaseRelated_ID, Integer.valueOf(R_ReleaseRelated_ID));
	}

	/** Get Release Related.
		@return Release Related	  */
	public int getR_ReleaseRelated_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_ReleaseRelated_ID);
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