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

/** Generated Model for C_DunningInterestVersion
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_DunningInterestVersion extends PO implements I_C_DunningInterestVersion, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260514L;

    /** Standard Constructor */
    public X_C_DunningInterestVersion (Properties ctx, int C_DunningInterestVersion_ID, String trxName)
    {
      super (ctx, C_DunningInterestVersion_ID, trxName);
      /** if (C_DunningInterestVersion_ID == 0)
        {
			setC_DunningInterestVersion_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_C_DunningInterestVersion (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_DunningInterestVersion[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_DunningInterestType getC_DunningInterestType() throws RuntimeException
    {
		return (I_C_DunningInterestType)MTable.get(getCtx(), I_C_DunningInterestType.Table_Name)
			.getPO(getC_DunningInterestType_ID(), get_TrxName());	}

	/** Set Dunning Interest Type.
		@param C_DunningInterestType_ID Dunning Interest Type	  */
	public void setC_DunningInterestType_ID (int C_DunningInterestType_ID)
	{
		if (C_DunningInterestType_ID < 1) 
			set_Value (COLUMNNAME_C_DunningInterestType_ID, null);
		else 
			set_Value (COLUMNNAME_C_DunningInterestType_ID, Integer.valueOf(C_DunningInterestType_ID));
	}

	/** Get Dunning Interest Type.
		@return Dunning Interest Type	  */
	public int getC_DunningInterestType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DunningInterestType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Dunning Interest Version.
		@param C_DunningInterestVersion_ID Dunning Interest Version	  */
	public void setC_DunningInterestVersion_ID (int C_DunningInterestVersion_ID)
	{
		if (C_DunningInterestVersion_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_DunningInterestVersion_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_DunningInterestVersion_ID, Integer.valueOf(C_DunningInterestVersion_ID));
	}

	/** Get Dunning Interest Version.
		@return Dunning Interest Version	  */
	public int getC_DunningInterestVersion_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DunningInterestVersion_ID);
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