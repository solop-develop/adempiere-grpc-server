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

/** Generated Interface for C_PPBatchConfiguration
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_C_PPBatchConfiguration 
{

    /** TableName=C_PPBatchConfiguration */
    public static final String Table_Name = "C_PPBatchConfiguration";

    /** AD_Table_ID=55040 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

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

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set Business Partner .
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get Business Partner .
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public I_C_BPartner getC_BPartner() throws RuntimeException;

    /** Column name C_BPartner_Location_ID */
    public static final String COLUMNNAME_C_BPartner_Location_ID = "C_BPartner_Location_ID";

	/** Set Partner Location.
	  * Identifies the (ship to) address for this Business Partner
	  */
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID);

	/** Get Partner Location.
	  * Identifies the (ship to) address for this Business Partner
	  */
	public int getC_BPartner_Location_ID();

	public I_C_BPartner_Location getC_BPartner_Location() throws RuntimeException;

    /** Column name C_PPBatchConfiguration_ID */
    public static final String COLUMNNAME_C_PPBatchConfiguration_ID = "C_PPBatchConfiguration_ID";

	/** Set Payment Processor Batch Configuration	  */
	public void setC_PPBatchConfiguration_ID (int C_PPBatchConfiguration_ID);

	/** Get Payment Processor Batch Configuration	  */
	public int getC_PPBatchConfiguration_ID();

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

    /** Column name FeeCharge_ID */
    public static final String COLUMNNAME_FeeCharge_ID = "FeeCharge_ID";

	/** Set Charge for Fee.
	  * Charge for Fee
	  */
	public void setFeeCharge_ID (int FeeCharge_ID);

	/** Get Charge for Fee.
	  * Charge for Fee
	  */
	public int getFeeCharge_ID();

	public I_C_Charge getFeeCharge() throws RuntimeException;

    /** Column name FeeCurrency_ID */
    public static final String COLUMNNAME_FeeCurrency_ID = "FeeCurrency_ID";

	/** Set Currency for Fee.
	  * Currency for Fee
	  */
	public void setFeeCurrency_ID (int FeeCurrency_ID);

	/** Get Currency for Fee.
	  * Currency for Fee
	  */
	public int getFeeCurrency_ID();

	public I_C_Currency getFeeCurrency() throws RuntimeException;

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

    /** Column name PurchaseInvoiceDocType_ID */
    public static final String COLUMNNAME_PurchaseInvoiceDocType_ID = "PurchaseInvoiceDocType_ID";

	/** Set Purchase Invoice Document Type	  */
	public void setPurchaseInvoiceDocType_ID (int PurchaseInvoiceDocType_ID);

	/** Get Purchase Invoice Document Type	  */
	public int getPurchaseInvoiceDocType_ID();

	public I_C_DocType getPurchaseInvoiceDocType() throws RuntimeException;

    /** Column name SalesInvoiceCharge_ID */
    public static final String COLUMNNAME_SalesInvoiceCharge_ID = "SalesInvoiceCharge_ID";

	/** Set Sales Invoice Charge	  */
	public void setSalesInvoiceCharge_ID (int SalesInvoiceCharge_ID);

	/** Get Sales Invoice Charge	  */
	public int getSalesInvoiceCharge_ID();

	public I_C_Charge getSalesInvoiceCharge() throws RuntimeException;

    /** Column name SalesInvoiceDocType_ID */
    public static final String COLUMNNAME_SalesInvoiceDocType_ID = "SalesInvoiceDocType_ID";

	/** Set Sales Invoice Document Type	  */
	public void setSalesInvoiceDocType_ID (int SalesInvoiceDocType_ID);

	/** Get Sales Invoice Document Type	  */
	public int getSalesInvoiceDocType_ID();

	public I_C_DocType getSalesInvoiceDocType() throws RuntimeException;

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

    /** Column name WithholdingCharge_ID */
    public static final String COLUMNNAME_WithholdingCharge_ID = "WithholdingCharge_ID";

	/** Set Withholding Charge	  */
	public void setWithholdingCharge_ID (int WithholdingCharge_ID);

	/** Get Withholding Charge	  */
	public int getWithholdingCharge_ID();

	public I_C_Charge getWithholdingCharge() throws RuntimeException;

    /** Column name WithholdingDocType_ID */
    public static final String COLUMNNAME_WithholdingDocType_ID = "WithholdingDocType_ID";

	/** Set Withholding Document Type	  */
	public void setWithholdingDocType_ID (int WithholdingDocType_ID);

	/** Get Withholding Document Type	  */
	public int getWithholdingDocType_ID();

	public I_C_DocType getWithholdingDocType() throws RuntimeException;
}
