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
package com.solop.sp033.model;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Interface for SP033_Webhook
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_SP033_Webhook 
{

    /** TableName=SP033_Webhook */
    public static final String Table_Name = "SP033_Webhook";

    /** AD_Table_ID=54984 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name Classname */
    public static final String COLUMNNAME_Classname = "Classname";

	/** Set Classname.
	  * Java Classname
	  */
	public void setClassname (String Classname);

	/** Get Classname.
	  * Java Classname
	  */
	public String getClassname();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name SP033_ContentType */
    public static final String COLUMNNAME_SP033_ContentType = "SP033_ContentType";

	/** Set Content Type.
	  * Content Type
	  */
	public void setSP033_ContentType (String SP033_ContentType);

	/** Get Content Type.
	  * Content Type
	  */
	public String getSP033_ContentType();

    /** Column name SP033_IsTest */
    public static final String COLUMNNAME_SP033_IsTest = "SP033_IsTest";

	/** Set Use Test URL.
	  * Use Test URL
	  */
	public void setSP033_IsTest (boolean SP033_IsTest);

	/** Get Use Test URL.
	  * Use Test URL
	  */
	public boolean isSP033_IsTest();

    /** Column name SP033_Method */
    public static final String COLUMNNAME_SP033_Method = "SP033_Method";

	/** Set Method.
	  * Method
	  */
	public void setSP033_Method (String SP033_Method);

	/** Get Method.
	  * Method
	  */
	public String getSP033_Method();

    /** Column name SP033_PayloadURL */
    public static final String COLUMNNAME_SP033_PayloadURL = "SP033_PayloadURL";

	/** Set Payload URL.
	  * Payload URL
	  */
	public void setSP033_PayloadURL (String SP033_PayloadURL);

	/** Get Payload URL.
	  * Payload URL
	  */
	public String getSP033_PayloadURL();

    /** Column name SP033_TestPayloadURL */
    public static final String COLUMNNAME_SP033_TestPayloadURL = "SP033_TestPayloadURL";

	/** Set Test Payload URL.
	  * Test Payload URL
	  */
	public void setSP033_TestPayloadURL (String SP033_TestPayloadURL);

	/** Get Test Payload URL.
	  * Test Payload URL
	  */
	public String getSP033_TestPayloadURL();

    /** Column name SP033_Webhook_ID */
    public static final String COLUMNNAME_SP033_Webhook_ID = "SP033_Webhook_ID";

	/** Set Webhook	  */
	public void setSP033_Webhook_ID (int SP033_Webhook_ID);

	/** Get Webhook	  */
	public int getSP033_Webhook_ID();

    /** Column name SP033_WebhookTest */
    public static final String COLUMNNAME_SP033_WebhookTest = "SP033_WebhookTest";

	/** Set Test Webhook.
	  * Make a Testing for Webhook
	  */
	public void setSP033_WebhookTest (String SP033_WebhookTest);

	/** Get Test Webhook.
	  * Make a Testing for Webhook
	  */
	public String getSP033_WebhookTest();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name UUID */
    public static final String COLUMNNAME_UUID = "UUID";

	/** Set Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID);

	/** Get Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public String getUUID();

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();
}
