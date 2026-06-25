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
package org.adempiere.core.domains.models;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Interface for AD_Token
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_AD_Token 
{

    /** TableName=AD_Token */
    public static final String Table_Name = "AD_Token";

    /** AD_Table_ID=54429 */
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

    /** Column name AD_Role_ID */
    public static final String COLUMNNAME_AD_Role_ID = "AD_Role_ID";

	/** Set Role.
	  * Responsibility Role
	  */
	public void setAD_Role_ID (int AD_Role_ID);

	/** Get Role.
	  * Responsibility Role
	  */
	public int getAD_Role_ID();

	public I_AD_Role getAD_Role() throws RuntimeException;

    /** Column name AD_TokenDefinition_ID */
    public static final String COLUMNNAME_AD_TokenDefinition_ID = "AD_TokenDefinition_ID";

	/** Set Token Definition.
	  * Token Definition, used for define generator class for token
	  */
	public void setAD_TokenDefinition_ID (int AD_TokenDefinition_ID);

	/** Get Token Definition.
	  * Token Definition, used for define generator class for token
	  */
	public int getAD_TokenDefinition_ID();

	public I_AD_TokenDefinition getAD_TokenDefinition() throws RuntimeException;

    /** Column name AD_Token_ID */
    public static final String COLUMNNAME_AD_Token_ID = "AD_Token_ID";

	/** Set Token.
	  * Token for validation and approval
	  */
	public void setAD_Token_ID (int AD_Token_ID);

	/** Get Token.
	  * Token for validation and approval
	  */
	public int getAD_Token_ID();

    /** Column name AD_TokenProfile_ID */
    public static final String COLUMNNAME_AD_TokenProfile_ID = "AD_TokenProfile_ID";

	/** Set Token Profile	  */
	public void setAD_TokenProfile_ID (int AD_TokenProfile_ID);

	/** Get Token Profile	  */
	public int getAD_TokenProfile_ID();

	public I_AD_TokenProfile getAD_TokenProfile() throws RuntimeException;

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	public I_AD_User getAD_User() throws RuntimeException;

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

    /** Column name ExpireDate */
    public static final String COLUMNNAME_ExpireDate = "ExpireDate";

	/** Set Expire Date	  */
	public void setExpireDate (Timestamp ExpireDate);

	/** Get Expire Date	  */
	public Timestamp getExpireDate();

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

    /** Column name IsRevoked */
    public static final String COLUMNNAME_IsRevoked = "IsRevoked";

	/** Set Revoked.
	  * Indicates the token has been revoked
	  */
	public void setIsRevoked (boolean IsRevoked);

	/** Get Revoked.
	  * Indicates the token has been revoked
	  */
	public boolean isRevoked();

    /** Column name RevokedReason */
    public static final String COLUMNNAME_RevokedReason = "RevokedReason";

	/** Set Revoked Reason.
	  * Reason the token was revoked
	  */
	public void setRevokedReason (String RevokedReason);

	/** Get Revoked Reason.
	  * Reason the token was revoked
	  */
	public String getRevokedReason();

    /** Column name Scope */
    public static final String COLUMNNAME_Scope = "Scope";

	/** Set Scope.
	  * Resolved scope string for the token
	  */
	public void setScope (String Scope);

	/** Get Scope.
	  * Resolved scope string for the token
	  */
	public String getScope();

    /** Column name TokenValue */
    public static final String COLUMNNAME_TokenValue = "TokenValue";

	/** Set Token Value.
	  * Value of Token generated
	  */
	public void setTokenValue (String TokenValue);

	/** Get Token Value.
	  * Value of Token generated
	  */
	public String getTokenValue();

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
}
