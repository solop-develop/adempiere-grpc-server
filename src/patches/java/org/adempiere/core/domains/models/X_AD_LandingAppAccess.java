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

/** Generated Model for AD_LandingAppAccess
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_LandingAppAccess extends PO implements I_AD_LandingAppAccess, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260617L;

    /** Standard Constructor */
    public X_AD_LandingAppAccess (Properties ctx, int AD_LandingAppAccess_ID, String trxName)
    {
      super (ctx, AD_LandingAppAccess_ID, trxName);
      /** if (AD_LandingAppAccess_ID == 0)
        {
			setAccessLevel (null);
			setAD_LandingAppAccess_ID (0);
			setAD_LandingApp_ID (0);
			setAD_Role_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_LandingAppAccess (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
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
      StringBuffer sb = new StringBuffer ("X_AD_LandingAppAccess[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** AccessLevel AD_Reference_ID=54711 */
	public static final int ACCESSLEVEL_AD_Reference_ID=54711;
	/** Read = R */
	public static final String ACCESSLEVEL_Read = "R";
	/** Write = W */
	public static final String ACCESSLEVEL_Write = "W";
	/** Set Data Access Level.
		@param AccessLevel 
		Access Level required
	  */
	public void setAccessLevel (String AccessLevel)
	{

		set_Value (COLUMNNAME_AccessLevel, AccessLevel);
	}

	/** Get Data Access Level.
		@return Access Level required
	  */
	public String getAccessLevel () 
	{
		return (String)get_Value(COLUMNNAME_AccessLevel);
	}

	/** Set Landing App Access.
		@param AD_LandingAppAccess_ID Landing App Access	  */
	public void setAD_LandingAppAccess_ID (int AD_LandingAppAccess_ID)
	{
		if (AD_LandingAppAccess_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_LandingAppAccess_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_LandingAppAccess_ID, Integer.valueOf(AD_LandingAppAccess_ID));
	}

	/** Get Landing App Access.
		@return Landing App Access	  */
	public int getAD_LandingAppAccess_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_LandingAppAccess_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_LandingApp getAD_LandingApp() throws RuntimeException
    {
		return (I_AD_LandingApp)MTable.get(getCtx(), I_AD_LandingApp.Table_Name)
			.getPO(getAD_LandingApp_ID(), get_TrxName());	}

	/** Set Landing App.
		@param AD_LandingApp_ID Landing App	  */
	public void setAD_LandingApp_ID (int AD_LandingApp_ID)
	{
		if (AD_LandingApp_ID < 1) 
			set_Value (COLUMNNAME_AD_LandingApp_ID, null);
		else 
			set_Value (COLUMNNAME_AD_LandingApp_ID, Integer.valueOf(AD_LandingApp_ID));
	}

	/** Get Landing App.
		@return Landing App	  */
	public int getAD_LandingApp_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_LandingApp_ID);
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