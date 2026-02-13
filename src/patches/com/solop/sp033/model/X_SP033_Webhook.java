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
package com.solop.sp033.model;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.KeyNamePair;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for SP033_Webhook
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_SP033_Webhook extends PO implements I_SP033_Webhook, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260213L;

    /** Standard Constructor */
    public X_SP033_Webhook (Properties ctx, int SP033_Webhook_ID, String trxName)
    {
      super (ctx, SP033_Webhook_ID, trxName);
      /** if (SP033_Webhook_ID == 0)
        {
			setClassname (null);
// com.solop.sp033.support.DefaultWebhook
			setName (null);
			setSP033_ContentType (null);
// AJ
			setSP033_Method (null);
// POST
			setSP033_PayloadURL (null);
			setSP033_Webhook_ID (0);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_SP033_Webhook (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SP033_Webhook[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Classname AD_Reference_ID=54371 */
	public static final int CLASSNAME_AD_Reference_ID=54371;
	/** Default Webhook = com.solop.sp033.support.DefaultWebhook */
	public static final String CLASSNAME_DefaultWebhook = "com.solop.sp033.support.DefaultWebhook";
	/** Set Classname.
		@param Classname 
		Java Classname
	  */
	public void setClassname (String Classname)
	{

		set_Value (COLUMNNAME_Classname, Classname);
	}

	/** Get Classname.
		@return Java Classname
	  */
	public String getClassname () 
	{
		return (String)get_Value(COLUMNNAME_Classname);
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

	/** SP033_ContentType AD_Reference_ID=54370 */
	public static final int SP033_CONTENTTYPE_AD_Reference_ID=54370;
	/** application/json = AJ */
	public static final String SP033_CONTENTTYPE_ApplicationJson = "AJ";
	/** application/x-www-form-urlencoded = AX */
	public static final String SP033_CONTENTTYPE_ApplicationX_Www_Form_Urlencoded = "AX";
	/** Set Content Type.
		@param SP033_ContentType 
		Content Type
	  */
	public void setSP033_ContentType (String SP033_ContentType)
	{

		set_Value (COLUMNNAME_SP033_ContentType, SP033_ContentType);
	}

	/** Get Content Type.
		@return Content Type
	  */
	public String getSP033_ContentType () 
	{
		return (String)get_Value(COLUMNNAME_SP033_ContentType);
	}

	/** Set Use Test URL.
		@param SP033_IsTest 
		Use Test URL
	  */
	public void setSP033_IsTest (boolean SP033_IsTest)
	{
		set_Value (COLUMNNAME_SP033_IsTest, Boolean.valueOf(SP033_IsTest));
	}

	/** Get Use Test URL.
		@return Use Test URL
	  */
	public boolean isSP033_IsTest () 
	{
		Object oo = get_Value(COLUMNNAME_SP033_IsTest);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** SP033_Method AD_Reference_ID=54372 */
	public static final int SP033_METHOD_AD_Reference_ID=54372;
	/** POST = POST */
	public static final String SP033_METHOD_POST = "POST";
	/** GET = GET */
	public static final String SP033_METHOD_GET = "GET";
	/** PUT = PUT */
	public static final String SP033_METHOD_PUT = "PUT";
	/** PATCH = PATCH */
	public static final String SP033_METHOD_PATCH = "PATCH";
	/** DELETE = DELETE */
	public static final String SP033_METHOD_DELETE = "DELETE";
	/** Set Method.
		@param SP033_Method 
		Method
	  */
	public void setSP033_Method (String SP033_Method)
	{

		set_Value (COLUMNNAME_SP033_Method, SP033_Method);
	}

	/** Get Method.
		@return Method
	  */
	public String getSP033_Method () 
	{
		return (String)get_Value(COLUMNNAME_SP033_Method);
	}

	/** Set Payload URL.
		@param SP033_PayloadURL 
		Payload URL
	  */
	public void setSP033_PayloadURL (String SP033_PayloadURL)
	{
		set_Value (COLUMNNAME_SP033_PayloadURL, SP033_PayloadURL);
	}

	/** Get Payload URL.
		@return Payload URL
	  */
	public String getSP033_PayloadURL () 
	{
		return (String)get_Value(COLUMNNAME_SP033_PayloadURL);
	}

	/** Set Test Payload URL.
		@param SP033_TestPayloadURL 
		Test Payload URL
	  */
	public void setSP033_TestPayloadURL (String SP033_TestPayloadURL)
	{
		set_Value (COLUMNNAME_SP033_TestPayloadURL, SP033_TestPayloadURL);
	}

	/** Get Test Payload URL.
		@return Test Payload URL
	  */
	public String getSP033_TestPayloadURL () 
	{
		return (String)get_Value(COLUMNNAME_SP033_TestPayloadURL);
	}

	/** Set Webhook.
		@param SP033_Webhook_ID Webhook	  */
	public void setSP033_Webhook_ID (int SP033_Webhook_ID)
	{
		if (SP033_Webhook_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SP033_Webhook_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SP033_Webhook_ID, Integer.valueOf(SP033_Webhook_ID));
	}

	/** Get Webhook.
		@return Webhook	  */
	public int getSP033_Webhook_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SP033_Webhook_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Test Webhook.
		@param SP033_WebhookTest 
		Make a Testing for Webhook
	  */
	public void setSP033_WebhookTest (String SP033_WebhookTest)
	{
		set_Value (COLUMNNAME_SP033_WebhookTest, SP033_WebhookTest);
	}

	/** Get Test Webhook.
		@return Make a Testing for Webhook
	  */
	public String getSP033_WebhookTest () 
	{
		return (String)get_Value(COLUMNNAME_SP033_WebhookTest);
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

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getValue());
    }
}