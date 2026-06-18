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

/** Generated Model for AD_LandingApp
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_AD_LandingApp extends PO implements I_AD_LandingApp, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260617L;

    /** Standard Constructor */
    public X_AD_LandingApp (Properties ctx, int AD_LandingApp_ID, String trxName)
    {
      super (ctx, AD_LandingApp_ID, trxName);
      /** if (AD_LandingApp_ID == 0)
        {
			setAD_LandingApp_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_AD_LandingApp (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_AD_LandingApp[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Landing App.
		@param AD_LandingApp_ID Landing App	  */
	public void setAD_LandingApp_ID (int AD_LandingApp_ID)
	{
		if (AD_LandingApp_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_LandingApp_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_LandingApp_ID, Integer.valueOf(AD_LandingApp_ID));
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

	/** Set Icon.
		@param Icon 
		Icon name or reference
	  */
	public void setIcon (String Icon)
	{
		set_Value (COLUMNNAME_Icon, Icon);
	}

	/** Get Icon.
		@return Icon name or reference
	  */
	public String getIcon () 
	{
		return (String)get_Value(COLUMNNAME_Icon);
	}

	/** Set Logo URL.
		@param LogoURL 
		URL of the landing app logo
	  */
	public void setLogoURL (String LogoURL)
	{
		set_Value (COLUMNNAME_LogoURL, LogoURL);
	}

	/** Get Logo URL.
		@return URL of the landing app logo
	  */
	public String getLogoURL () 
	{
		return (String)get_Value(COLUMNNAME_LogoURL);
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

	/** Set Route.
		@param Route 
		Internal relative route, e.g. /nuxt
	  */
	public void setRoute (String Route)
	{
		set_Value (COLUMNNAME_Route, Route);
	}

	/** Get Route.
		@return Internal relative route, e.g. /nuxt
	  */
	public String getRoute () 
	{
		return (String)get_Value(COLUMNNAME_Route);
	}

	/** Set Sequence.
		@param SeqNo 
		Method of ordering records; lowest number comes first
	  */
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set URL.
		@param URL 
		Full URL address - e.g. http://www.adempiere.org
	  */
	public void setURL (String URL)
	{
		set_Value (COLUMNNAME_URL, URL);
	}

	/** Get URL.
		@return Full URL address - e.g. http://www.adempiere.org
	  */
	public String getURL () 
	{
		return (String)get_Value(COLUMNNAME_URL);
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

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}