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

/** Generated Interface for C_POSSellerAllocation
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_C_POSSellerAllocation 
{

    /** TableName=C_POSSellerAllocation */
    public static final String Table_Name = "C_POSSellerAllocation";

    /** AD_Table_ID=54872 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 2 - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(2);

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

    /** Column name C_POS_ID */
    public static final String COLUMNNAME_C_POS_ID = "C_POS_ID";

	/** Set POS Terminal.
	  * Point of Sales Terminal
	  */
	public void setC_POS_ID (int C_POS_ID);

	/** Get POS Terminal.
	  * Point of Sales Terminal
	  */
	public int getC_POS_ID();

	public I_C_POS getC_POS() throws RuntimeException;

    /** Column name C_POSSellerAllocation_ID */
    public static final String COLUMNNAME_C_POSSellerAllocation_ID = "C_POSSellerAllocation_ID";

	/** Set POS Seller Allocation	  */
	public void setC_POSSellerAllocation_ID (int C_POSSellerAllocation_ID);

	/** Get POS Seller Allocation	  */
	public int getC_POSSellerAllocation_ID();

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

    /** Column name ECA14_WriteOffByPercent */
    public static final String COLUMNNAME_ECA14_WriteOffByPercent = "ECA14_WriteOffByPercent";

	/** Set WriteOff based on Percent.
	  * WriteOff based on Percent
	  */
	public void setECA14_WriteOffByPercent (boolean ECA14_WriteOffByPercent);

	/** Get WriteOff based on Percent.
	  * WriteOff based on Percent
	  */
	public boolean isECA14_WriteOffByPercent();

    /** Column name IsAccessAllOrgs */
    public static final String COLUMNNAME_IsAccessAllOrgs = "IsAccessAllOrgs";

	/** Set Access all Orgs.
	  * Access all Organizations (no org access control) of the client
	  */
	public void setIsAccessAllOrgs (boolean IsAccessAllOrgs);

	/** Get Access all Orgs.
	  * Access all Organizations (no org access control) of the client
	  */
	public boolean isAccessAllOrgs();

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

    /** Column name IsAllowsAllocateSeller */
    public static final String COLUMNNAME_IsAllowsAllocateSeller = "IsAllowsAllocateSeller";

	/** Set Allows Allocate Seller.
	  * Allows Allocate Seller for this POS Terminal
	  */
	public void setIsAllowsAllocateSeller (boolean IsAllowsAllocateSeller);

	/** Get Allows Allocate Seller.
	  * Allows Allocate Seller for this POS Terminal
	  */
	public boolean isAllowsAllocateSeller();

    /** Column name IsAllowsApplyDiscount */
    public static final String COLUMNNAME_IsAllowsApplyDiscount = "IsAllowsApplyDiscount";

	/** Set Allows Apply Discount (By Document).
	  * Allows Apply Discount for this POS Terminal
	  */
	public void setIsAllowsApplyDiscount (boolean IsAllowsApplyDiscount);

	/** Get Allows Apply Discount (By Document).
	  * Allows Apply Discount for this POS Terminal
	  */
	public boolean isAllowsApplyDiscount();

    /** Column name IsAllowsApplyShemaDiscount */
    public static final String COLUMNNAME_IsAllowsApplyShemaDiscount = "IsAllowsApplyShemaDiscount";

	/** Set Allows Apply Discount (By Document with Schema).
	  * Allows Apply Discount for this POS Terminal
	  */
	public void setIsAllowsApplyShemaDiscount (boolean IsAllowsApplyShemaDiscount);

	/** Get Allows Apply Discount (By Document with Schema).
	  * Allows Apply Discount for this POS Terminal
	  */
	public boolean isAllowsApplyShemaDiscount();

    /** Column name IsAllowsCashClosing */
    public static final String COLUMNNAME_IsAllowsCashClosing = "IsAllowsCashClosing";

	/** Set Allows Cash Closing.
	  * Allows Cash Closing
	  */
	public void setIsAllowsCashClosing (boolean IsAllowsCashClosing);

	/** Get Allows Cash Closing.
	  * Allows Cash Closing
	  */
	public boolean isAllowsCashClosing();

    /** Column name IsAllowsCashOpening */
    public static final String COLUMNNAME_IsAllowsCashOpening = "IsAllowsCashOpening";

	/** Set Allows Cash Opening.
	  * Allows Cash Opening
	  */
	public void setIsAllowsCashOpening (boolean IsAllowsCashOpening);

	/** Get Allows Cash Opening.
	  * Allows Cash Opening
	  */
	public boolean isAllowsCashOpening();

    /** Column name IsAllowsCashWithdrawal */
    public static final String COLUMNNAME_IsAllowsCashWithdrawal = "IsAllowsCashWithdrawal";

	/** Set Allows Cash Withdrawal.
	  * Allows Cash Withdrawal
	  */
	public void setIsAllowsCashWithdrawal (boolean IsAllowsCashWithdrawal);

	/** Get Allows Cash Withdrawal.
	  * Allows Cash Withdrawal
	  */
	public boolean isAllowsCashWithdrawal();

    /** Column name IsAllowsCollectOrder */
    public static final String COLUMNNAME_IsAllowsCollectOrder = "IsAllowsCollectOrder";

	/** Set Allows Collect Order.
	  * Allows collect a Sales Order
	  */
	public void setIsAllowsCollectOrder (boolean IsAllowsCollectOrder);

	/** Get Allows Collect Order.
	  * Allows collect a Sales Order
	  */
	public boolean isAllowsCollectOrder();

    /** Column name IsAllowsConfirmShipment */
    public static final String COLUMNNAME_IsAllowsConfirmShipment = "IsAllowsConfirmShipment";

	/** Set Allows Confirm Shipment.
	  * Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipment (boolean IsAllowsConfirmShipment);

	/** Get Allows Confirm Shipment.
	  * Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipment();

    /** Column name IsAllowsConfirmShipmentByOrder */
    public static final String COLUMNNAME_IsAllowsConfirmShipmentByOrder = "IsAllowsConfirmShipmentByOrder";

	/** Set Allows Confirm Shipment by Order.
	  * Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipmentByOrder (boolean IsAllowsConfirmShipmentByOrder);

	/** Get Allows Confirm Shipment by Order.
	  * Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipmentByOrder();

    /** Column name IsAllowsCreateCustomer */
    public static final String COLUMNNAME_IsAllowsCreateCustomer = "IsAllowsCreateCustomer";

	/** Set Allows Create Customer.
	  * Allows create a Customer from POS
	  */
	public void setIsAllowsCreateCustomer (boolean IsAllowsCreateCustomer);

	/** Get Allows Create Customer.
	  * Allows create a Customer from POS
	  */
	public boolean isAllowsCreateCustomer();

    /** Column name IsAllowsCreateManualDocument */
    public static final String COLUMNNAME_IsAllowsCreateManualDocument = "IsAllowsCreateManualDocument";

	/** Set Allows Create Manual Document	  */
	public void setIsAllowsCreateManualDocument (boolean IsAllowsCreateManualDocument);

	/** Get Allows Create Manual Document	  */
	public boolean isAllowsCreateManualDocument();

    /** Column name IsAllowsCreateOrder */
    public static final String COLUMNNAME_IsAllowsCreateOrder = "IsAllowsCreateOrder";

	/** Set Allows Create Order.
	  * Allows create a Sales Order
	  */
	public void setIsAllowsCreateOrder (boolean IsAllowsCreateOrder);

	/** Get Allows Create Order.
	  * Allows create a Sales Order
	  */
	public boolean isAllowsCreateOrder();

    /** Column name IsAllowsCustomerTemplate */
    public static final String COLUMNNAME_IsAllowsCustomerTemplate = "IsAllowsCustomerTemplate";

	/** Set Allows Customer Template.
	  * Allows Customer Template from POS
	  */
	public void setIsAllowsCustomerTemplate (boolean IsAllowsCustomerTemplate);

	/** Get Allows Customer Template.
	  * Allows Customer Template from POS
	  */
	public boolean isAllowsCustomerTemplate();

    /** Column name IsAllowsDetailCashClosing */
    public static final String COLUMNNAME_IsAllowsDetailCashClosing = "IsAllowsDetailCashClosing";

	/** Set Allows Detailed Cash Closing.
	  * Allows Detailed Cash Closing
	  */
	public void setIsAllowsDetailCashClosing (boolean IsAllowsDetailCashClosing);

	/** Get Allows Detailed Cash Closing.
	  * Allows Detailed Cash Closing
	  */
	public boolean isAllowsDetailCashClosing();

    /** Column name IsAllowsModifyCustomer */
    public static final String COLUMNNAME_IsAllowsModifyCustomer = "IsAllowsModifyCustomer";

	/** Set Allows Modify Customer.
	  * Allows Modify Customer from POS
	  */
	public void setIsAllowsModifyCustomer (boolean IsAllowsModifyCustomer);

	/** Get Allows Modify Customer.
	  * Allows Modify Customer from POS
	  */
	public boolean isAllowsModifyCustomer();

    /** Column name IsAllowsModifyDiscount */
    public static final String COLUMNNAME_IsAllowsModifyDiscount = "IsAllowsModifyDiscount";

	/** Set Allows Modify Discount (By Line).
	  * Allows Modify Discount from Terminal
	  */
	public void setIsAllowsModifyDiscount (boolean IsAllowsModifyDiscount);

	/** Get Allows Modify Discount (By Line).
	  * Allows Modify Discount from Terminal
	  */
	public boolean isAllowsModifyDiscount();

    /** Column name IsAllowsModifyQuantity */
    public static final String COLUMNNAME_IsAllowsModifyQuantity = "IsAllowsModifyQuantity";

	/** Set Allows Modify Quantity.
	  * Allows modifying the quantity
	  */
	public void setIsAllowsModifyQuantity (boolean IsAllowsModifyQuantity);

	/** Get Allows Modify Quantity.
	  * Allows modifying the quantity
	  */
	public boolean isAllowsModifyQuantity();

    /** Column name IsAllowsOpenAmount */
    public static final String COLUMNNAME_IsAllowsOpenAmount = "IsAllowsOpenAmount";

	/** Set Allows Open Amount	  */
	public void setIsAllowsOpenAmount (boolean IsAllowsOpenAmount);

	/** Get Allows Open Amount	  */
	public boolean isAllowsOpenAmount();

    /** Column name IsAllowsPOSManager */
    public static final String COLUMNNAME_IsAllowsPOSManager = "IsAllowsPOSManager";

	/** Set POS Manager.
	  * Allows validate PIN based on POS Manager
	  */
	public void setIsAllowsPOSManager (boolean IsAllowsPOSManager);

	/** Get POS Manager.
	  * Allows validate PIN based on POS Manager
	  */
	public boolean isAllowsPOSManager();

    /** Column name IsAllowsPreviewDocument */
    public static final String COLUMNNAME_IsAllowsPreviewDocument = "IsAllowsPreviewDocument";

	/** Set Allows Print Preview.
	  * Allows print document with preview from POS
	  */
	public void setIsAllowsPreviewDocument (boolean IsAllowsPreviewDocument);

	/** Get Allows Print Preview.
	  * Allows print document with preview from POS
	  */
	public boolean isAllowsPreviewDocument();

    /** Column name IsAllowsPrintDocument */
    public static final String COLUMNNAME_IsAllowsPrintDocument = "IsAllowsPrintDocument";

	/** Set Allows Print Document.
	  * Allows print document from POS
	  */
	public void setIsAllowsPrintDocument (boolean IsAllowsPrintDocument);

	/** Get Allows Print Document.
	  * Allows print document from POS
	  */
	public boolean isAllowsPrintDocument();

    /** Column name IsAllowsReturnOrder */
    public static final String COLUMNNAME_IsAllowsReturnOrder = "IsAllowsReturnOrder";

	/** Set Allows Return Order.
	  * Allows return a Sales Order
	  */
	public void setIsAllowsReturnOrder (boolean IsAllowsReturnOrder);

	/** Get Allows Return Order.
	  * Allows return a Sales Order
	  */
	public boolean isAllowsReturnOrder();

    /** Column name IsAllowsWriteOffAmount */
    public static final String COLUMNNAME_IsAllowsWriteOffAmount = "IsAllowsWriteOffAmount";

	/** Set Allows Writeoff amount.
	  * Allows Writeoff amount
	  */
	public void setIsAllowsWriteOffAmount (boolean IsAllowsWriteOffAmount);

	/** Get Allows Writeoff amount.
	  * Allows Writeoff amount
	  */
	public boolean isAllowsWriteOffAmount();

    /** Column name IsModifyPrice */
    public static final String COLUMNNAME_IsModifyPrice = "IsModifyPrice";

	/** Set Modify Price.
	  * Allow modifying the price
	  */
	public void setIsModifyPrice (boolean IsModifyPrice);

	/** Get Modify Price.
	  * Allow modifying the price
	  */
	public boolean isModifyPrice();

    /** Column name MaximumDailyRefundAllowed */
    public static final String COLUMNNAME_MaximumDailyRefundAllowed = "MaximumDailyRefundAllowed";

	/** Set Maximum Daily Refund Allowed.
	  * Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public void setMaximumDailyRefundAllowed (BigDecimal MaximumDailyRefundAllowed);

	/** Get Maximum Daily Refund Allowed.
	  * Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumDailyRefundAllowed();

    /** Column name MaximumDiscountAllowed */
    public static final String COLUMNNAME_MaximumDiscountAllowed = "MaximumDiscountAllowed";

	/** Set Maximum Discount %.
	  * Discount in percent
	  */
	public void setMaximumDiscountAllowed (BigDecimal MaximumDiscountAllowed);

	/** Get Maximum Discount %.
	  * Discount in percent
	  */
	public BigDecimal getMaximumDiscountAllowed();

    /** Column name MaximumLineDiscountAllowed */
    public static final String COLUMNNAME_MaximumLineDiscountAllowed = "MaximumLineDiscountAllowed";

	/** Set Maximum Line Discount %.
	  * Discount in percent
	  */
	public void setMaximumLineDiscountAllowed (BigDecimal MaximumLineDiscountAllowed);

	/** Get Maximum Line Discount %.
	  * Discount in percent
	  */
	public BigDecimal getMaximumLineDiscountAllowed();

    /** Column name MaximumRefundAllowed */
    public static final String COLUMNNAME_MaximumRefundAllowed = "MaximumRefundAllowed";

	/** Set Maximum Refund Allowed.
	  * Set the maximum refund allowed for this tender type using the POS currency
	  */
	public void setMaximumRefundAllowed (BigDecimal MaximumRefundAllowed);

	/** Get Maximum Refund Allowed.
	  * Set the maximum refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumRefundAllowed();

    /** Column name MaximumShemaDiscountAllowed */
    public static final String COLUMNNAME_MaximumShemaDiscountAllowed = "MaximumShemaDiscountAllowed";

	/** Set Maximum Shema Discount %.
	  * Discount in percent
	  */
	public void setMaximumShemaDiscountAllowed (BigDecimal MaximumShemaDiscountAllowed);

	/** Get Maximum Shema Discount %.
	  * Discount in percent
	  */
	public BigDecimal getMaximumShemaDiscountAllowed();

    /** Column name SalesRep_ID */
    public static final String COLUMNNAME_SalesRep_ID = "SalesRep_ID";

	/** Set Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public void setSalesRep_ID (int SalesRep_ID);

	/** Get Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public int getSalesRep_ID();

	public I_AD_User getSalesRep() throws RuntimeException;

    /** Column name SeqNo */
    public static final String COLUMNNAME_SeqNo = "SeqNo";

	/** Set Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public void setSeqNo (int SeqNo);

	/** Get Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public int getSeqNo();

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

    /** Column name WriteOffAmtCurrency_ID */
    public static final String COLUMNNAME_WriteOffAmtCurrency_ID = "WriteOffAmtCurrency_ID";

	/** Set Currency for write-off per Document.
	  * Currency amount to be written off in invoice currency
	  */
	public void setWriteOffAmtCurrency_ID (int WriteOffAmtCurrency_ID);

	/** Get Currency for write-off per Document.
	  * Currency amount to be written off in invoice currency
	  */
	public int getWriteOffAmtCurrency_ID();

	public I_C_Currency getWriteOffAmtCurrency() throws RuntimeException;

    /** Column name WriteOffAmtTolerance */
    public static final String COLUMNNAME_WriteOffAmtTolerance = "WriteOffAmtTolerance";

	/** Set Tolerance for write-off per Document.
	  * Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffAmtTolerance (BigDecimal WriteOffAmtTolerance);

	/** Get Tolerance for write-off per Document.
	  * Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffAmtTolerance();

    /** Column name WriteOffPercentageTolerance */
    public static final String COLUMNNAME_WriteOffPercentageTolerance = "WriteOffPercentageTolerance";

	/** Set Tolerance for write-off %.
	  * Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffPercentageTolerance (BigDecimal WriteOffPercentageTolerance);

	/** Get Tolerance for write-off %.
	  * Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffPercentageTolerance();
}
