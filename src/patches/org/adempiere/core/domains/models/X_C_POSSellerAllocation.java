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
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for C_POSSellerAllocation
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_POSSellerAllocation extends PO implements I_C_POSSellerAllocation, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260119L;

    /** Standard Constructor */
    public X_C_POSSellerAllocation(Properties ctx, int C_POSSellerAllocation_ID, String trxName)
    {
      super (ctx, C_POSSellerAllocation_ID, trxName);
      /** if (C_POSSellerAllocation_ID == 0)
        {
			setC_POS_ID (0);
			setC_POSSellerAllocation_ID (0);
			setSalesRep_ID (0);
// -1
			setSeqNo (0);
// @SQL=SELECT NVL(MAX(SeqNo),0)+10 AS DefaultValue FROM C_POSSellerAllocation WHERE C_POS_ID=@C_POS_ID@
        } */
    }

    /** Load Constructor */
    public X_C_POSSellerAllocation(Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 2 - Client 
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
      StringBuffer sb = new StringBuffer ("X_C_POSSellerAllocation[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_POS getC_POS() throws RuntimeException
    {
		return (I_C_POS)MTable.get(getCtx(), I_C_POS.Table_Name)
			.getPO(getC_POS_ID(), get_TrxName());	}

	/** Set POS Terminal.
		@param C_POS_ID 
		Point of Sales Terminal
	  */
	public void setC_POS_ID (int C_POS_ID)
	{
		if (C_POS_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_POS_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_POS_ID, Integer.valueOf(C_POS_ID));
	}

	/** Get POS Terminal.
		@return Point of Sales Terminal
	  */
	public int getC_POS_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_POS_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set POS Seller Allocation.
		@param C_POSSellerAllocation_ID POS Seller Allocation	  */
	public void setC_POSSellerAllocation_ID (int C_POSSellerAllocation_ID)
	{
		if (C_POSSellerAllocation_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_POSSellerAllocation_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_POSSellerAllocation_ID, Integer.valueOf(C_POSSellerAllocation_ID));
	}

	/** Get POS Seller Allocation.
		@return POS Seller Allocation	  */
	public int getC_POSSellerAllocation_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_POSSellerAllocation_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set WriteOff based on Percent.
		@param ECA14_WriteOffByPercent 
		WriteOff based on Percent
	  */
	public void setECA14_WriteOffByPercent (boolean ECA14_WriteOffByPercent)
	{
		set_Value (COLUMNNAME_ECA14_WriteOffByPercent, Boolean.valueOf(ECA14_WriteOffByPercent));
	}

	/** Get WriteOff based on Percent.
		@return WriteOff based on Percent
	  */
	public boolean isECA14_WriteOffByPercent () 
	{
		Object oo = get_Value(COLUMNNAME_ECA14_WriteOffByPercent);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Access all Orgs.
		@param IsAccessAllOrgs 
		Access all Organizations (no org access control) of the client
	  */
	public void setIsAccessAllOrgs (boolean IsAccessAllOrgs)
	{
		set_Value (COLUMNNAME_IsAccessAllOrgs, Boolean.valueOf(IsAccessAllOrgs));
	}

	/** Get Access all Orgs.
		@return Access all Organizations (no org access control) of the client
	  */
	public boolean isAccessAllOrgs () 
	{
		Object oo = get_Value(COLUMNNAME_IsAccessAllOrgs);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Allocate Seller.
		@param IsAllowsAllocateSeller 
		Allows Allocate Seller for this POS Terminal
	  */
	public void setIsAllowsAllocateSeller (boolean IsAllowsAllocateSeller)
	{
		set_Value (COLUMNNAME_IsAllowsAllocateSeller, Boolean.valueOf(IsAllowsAllocateSeller));
	}

	/** Get Allows Allocate Seller.
		@return Allows Allocate Seller for this POS Terminal
	  */
	public boolean isAllowsAllocateSeller () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsAllocateSeller);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Apply Discount (By Document).
		@param IsAllowsApplyDiscount 
		Allows Apply Discount for this POS Terminal
	  */
	public void setIsAllowsApplyDiscount (boolean IsAllowsApplyDiscount)
	{
		set_Value (COLUMNNAME_IsAllowsApplyDiscount, Boolean.valueOf(IsAllowsApplyDiscount));
	}

	/** Get Allows Apply Discount (By Document).
		@return Allows Apply Discount for this POS Terminal
	  */
	public boolean isAllowsApplyDiscount () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsApplyDiscount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Apply Discount (By Document with Schema).
		@param IsAllowsApplyShemaDiscount 
		Allows Apply Discount for this POS Terminal
	  */
	public void setIsAllowsApplyShemaDiscount (boolean IsAllowsApplyShemaDiscount)
	{
		set_Value (COLUMNNAME_IsAllowsApplyShemaDiscount, Boolean.valueOf(IsAllowsApplyShemaDiscount));
	}

	/** Get Allows Apply Discount (By Document with Schema).
		@return Allows Apply Discount for this POS Terminal
	  */
	public boolean isAllowsApplyShemaDiscount () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsApplyShemaDiscount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Cash Closing.
		@param IsAllowsCashClosing 
		Allows Cash Closing
	  */
	public void setIsAllowsCashClosing (boolean IsAllowsCashClosing)
	{
		set_Value (COLUMNNAME_IsAllowsCashClosing, Boolean.valueOf(IsAllowsCashClosing));
	}

	/** Get Allows Cash Closing.
		@return Allows Cash Closing
	  */
	public boolean isAllowsCashClosing () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCashClosing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Cash Opening.
		@param IsAllowsCashOpening 
		Allows Cash Opening
	  */
	public void setIsAllowsCashOpening (boolean IsAllowsCashOpening)
	{
		set_Value (COLUMNNAME_IsAllowsCashOpening, Boolean.valueOf(IsAllowsCashOpening));
	}

	/** Get Allows Cash Opening.
		@return Allows Cash Opening
	  */
	public boolean isAllowsCashOpening () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCashOpening);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Cash Withdrawal.
		@param IsAllowsCashWithdrawal 
		Allows Cash Withdrawal
	  */
	public void setIsAllowsCashWithdrawal (boolean IsAllowsCashWithdrawal)
	{
		set_Value (COLUMNNAME_IsAllowsCashWithdrawal, Boolean.valueOf(IsAllowsCashWithdrawal));
	}

	/** Get Allows Cash Withdrawal.
		@return Allows Cash Withdrawal
	  */
	public boolean isAllowsCashWithdrawal () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCashWithdrawal);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Collect Order.
		@param IsAllowsCollectOrder 
		Allows collect a Sales Order
	  */
	public void setIsAllowsCollectOrder (boolean IsAllowsCollectOrder)
	{
		set_Value (COLUMNNAME_IsAllowsCollectOrder, Boolean.valueOf(IsAllowsCollectOrder));
	}

	/** Get Allows Collect Order.
		@return Allows collect a Sales Order
	  */
	public boolean isAllowsCollectOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCollectOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Confirm Shipment.
		@param IsAllowsConfirmShipment 
		Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipment (boolean IsAllowsConfirmShipment)
	{
		set_Value (COLUMNNAME_IsAllowsConfirmShipment, Boolean.valueOf(IsAllowsConfirmShipment));
	}

	/** Get Allows Confirm Shipment.
		@return Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipment () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsConfirmShipment);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Confirm Shipment by Order.
		@param IsAllowsConfirmShipmentByOrder 
		Allows Confirm Shipment from Order
	  */
	public void setIsAllowsConfirmShipmentByOrder (boolean IsAllowsConfirmShipmentByOrder)
	{
		set_Value (COLUMNNAME_IsAllowsConfirmShipmentByOrder, Boolean.valueOf(IsAllowsConfirmShipmentByOrder));
	}

	/** Get Allows Confirm Shipment by Order.
		@return Allows Confirm Shipment from Order
	  */
	public boolean isAllowsConfirmShipmentByOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsConfirmShipmentByOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Create Customer.
		@param IsAllowsCreateCustomer 
		Allows create a Customer from POS
	  */
	public void setIsAllowsCreateCustomer (boolean IsAllowsCreateCustomer)
	{
		set_Value (COLUMNNAME_IsAllowsCreateCustomer, Boolean.valueOf(IsAllowsCreateCustomer));
	}

	/** Get Allows Create Customer.
		@return Allows create a Customer from POS
	  */
	public boolean isAllowsCreateCustomer () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCreateCustomer);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Create Manual Document.
		@param IsAllowsCreateManualDocument Allows Create Manual Document	  */
	public void setIsAllowsCreateManualDocument (boolean IsAllowsCreateManualDocument)
	{
		set_Value (COLUMNNAME_IsAllowsCreateManualDocument, Boolean.valueOf(IsAllowsCreateManualDocument));
	}

	/** Get Allows Create Manual Document.
		@return Allows Create Manual Document	  */
	public boolean isAllowsCreateManualDocument ()
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCreateManualDocument);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Create Order.
		@param IsAllowsCreateOrder 
		Allows create a Sales Order
	  */
	public void setIsAllowsCreateOrder (boolean IsAllowsCreateOrder)
	{
		set_Value (COLUMNNAME_IsAllowsCreateOrder, Boolean.valueOf(IsAllowsCreateOrder));
	}

	/** Get Allows Create Order.
		@return Allows create a Sales Order
	  */
	public boolean isAllowsCreateOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCreateOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Customer Template.
		@param IsAllowsCustomerTemplate 
		Allows Customer Template from POS
	  */
	public void setIsAllowsCustomerTemplate (boolean IsAllowsCustomerTemplate)
	{
		set_Value (COLUMNNAME_IsAllowsCustomerTemplate, Boolean.valueOf(IsAllowsCustomerTemplate));
	}

	/** Get Allows Customer Template.
		@return Allows Customer Template from POS
	  */
	public boolean isAllowsCustomerTemplate () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsCustomerTemplate);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Detailed Cash Closing.
		@param IsAllowsDetailCashClosing 
		Allows Detailed Cash Closing
	  */
	public void setIsAllowsDetailCashClosing (boolean IsAllowsDetailCashClosing)
	{
		set_Value (COLUMNNAME_IsAllowsDetailCashClosing, Boolean.valueOf(IsAllowsDetailCashClosing));
	}

	/** Get Allows Detailed Cash Closing.
		@return Allows Detailed Cash Closing
	  */
	public boolean isAllowsDetailCashClosing () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsDetailCashClosing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Modify Customer.
		@param IsAllowsModifyCustomer 
		Allows Modify Customer from POS
	  */
	public void setIsAllowsModifyCustomer (boolean IsAllowsModifyCustomer)
	{
		set_Value (COLUMNNAME_IsAllowsModifyCustomer, Boolean.valueOf(IsAllowsModifyCustomer));
	}

	/** Get Allows Modify Customer.
		@return Allows Modify Customer from POS
	  */
	public boolean isAllowsModifyCustomer () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsModifyCustomer);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Modify Discount (By Line).
		@param IsAllowsModifyDiscount 
		Allows Modify Discount from Terminal
	  */
	public void setIsAllowsModifyDiscount (boolean IsAllowsModifyDiscount)
	{
		set_Value (COLUMNNAME_IsAllowsModifyDiscount, Boolean.valueOf(IsAllowsModifyDiscount));
	}

	/** Get Allows Modify Discount (By Line).
		@return Allows Modify Discount from Terminal
	  */
	public boolean isAllowsModifyDiscount () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsModifyDiscount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Modify Quantity.
		@param IsAllowsModifyQuantity 
		Allows modifying the quantity
	  */
	public void setIsAllowsModifyQuantity (boolean IsAllowsModifyQuantity)
	{
		set_Value (COLUMNNAME_IsAllowsModifyQuantity, Boolean.valueOf(IsAllowsModifyQuantity));
	}

	/** Get Allows Modify Quantity.
		@return Allows modifying the quantity
	  */
	public boolean isAllowsModifyQuantity () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsModifyQuantity);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Open Amount.
		@param IsAllowsOpenAmount Allows Open Amount	  */
	public void setIsAllowsOpenAmount (boolean IsAllowsOpenAmount)
	{
		set_Value (COLUMNNAME_IsAllowsOpenAmount, Boolean.valueOf(IsAllowsOpenAmount));
	}

	/** Get Allows Open Amount.
		@return Allows Open Amount	  */
	public boolean isAllowsOpenAmount () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsOpenAmount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set POS Manager.
		@param IsAllowsPOSManager 
		Allows validate PIN based on POS Manager
	  */
	public void setIsAllowsPOSManager (boolean IsAllowsPOSManager)
	{
		set_Value (COLUMNNAME_IsAllowsPOSManager, Boolean.valueOf(IsAllowsPOSManager));
	}

	/** Get POS Manager.
		@return Allows validate PIN based on POS Manager
	  */
	public boolean isAllowsPOSManager () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsPOSManager);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Print Preview.
		@param IsAllowsPreviewDocument 
		Allows print document with preview from POS
	  */
	public void setIsAllowsPreviewDocument (boolean IsAllowsPreviewDocument)
	{
		set_Value (COLUMNNAME_IsAllowsPreviewDocument, Boolean.valueOf(IsAllowsPreviewDocument));
	}

	/** Get Allows Print Preview.
		@return Allows print document with preview from POS
	  */
	public boolean isAllowsPreviewDocument () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsPreviewDocument);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Print Document.
		@param IsAllowsPrintDocument 
		Allows print document from POS
	  */
	public void setIsAllowsPrintDocument (boolean IsAllowsPrintDocument)
	{
		set_Value (COLUMNNAME_IsAllowsPrintDocument, Boolean.valueOf(IsAllowsPrintDocument));
	}

	/** Get Allows Print Document.
		@return Allows print document from POS
	  */
	public boolean isAllowsPrintDocument () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsPrintDocument);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Return Order.
		@param IsAllowsReturnOrder 
		Allows return a Sales Order
	  */
	public void setIsAllowsReturnOrder (boolean IsAllowsReturnOrder)
	{
		set_Value (COLUMNNAME_IsAllowsReturnOrder, Boolean.valueOf(IsAllowsReturnOrder));
	}

	/** Get Allows Return Order.
		@return Allows return a Sales Order
	  */
	public boolean isAllowsReturnOrder () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsReturnOrder);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allows Writeoff amount.
		@param IsAllowsWriteOffAmount 
		Allows Writeoff amount
	  */
	public void setIsAllowsWriteOffAmount (boolean IsAllowsWriteOffAmount)
	{
		set_Value (COLUMNNAME_IsAllowsWriteOffAmount, Boolean.valueOf(IsAllowsWriteOffAmount));
	}

	/** Get Allows Writeoff amount.
		@return Allows Writeoff amount
	  */
	public boolean isAllowsWriteOffAmount () 
	{
		Object oo = get_Value(COLUMNNAME_IsAllowsWriteOffAmount);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Modify Price.
		@param IsModifyPrice 
		Allow modifying the price
	  */
	public void setIsModifyPrice (boolean IsModifyPrice)
	{
		set_Value (COLUMNNAME_IsModifyPrice, Boolean.valueOf(IsModifyPrice));
	}

	/** Get Modify Price.
		@return Allow modifying the price
	  */
	public boolean isModifyPrice () 
	{
		Object oo = get_Value(COLUMNNAME_IsModifyPrice);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Maximum Daily Refund Allowed.
		@param MaximumDailyRefundAllowed 
		Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public void setMaximumDailyRefundAllowed (BigDecimal MaximumDailyRefundAllowed)
	{
		set_Value (COLUMNNAME_MaximumDailyRefundAllowed, MaximumDailyRefundAllowed);
	}

	/** Get Maximum Daily Refund Allowed.
		@return Set the maximum daily refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumDailyRefundAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumDailyRefundAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Maximum Discount %.
		@param MaximumDiscountAllowed 
		Discount in percent
	  */
	public void setMaximumDiscountAllowed (BigDecimal MaximumDiscountAllowed)
	{
		set_Value (COLUMNNAME_MaximumDiscountAllowed, MaximumDiscountAllowed);
	}

	/** Get Maximum Discount %.
		@return Discount in percent
	  */
	public BigDecimal getMaximumDiscountAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumDiscountAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Maximum Line Discount %.
		@param MaximumLineDiscountAllowed 
		Discount in percent
	  */
	public void setMaximumLineDiscountAllowed (BigDecimal MaximumLineDiscountAllowed)
	{
		set_Value (COLUMNNAME_MaximumLineDiscountAllowed, MaximumLineDiscountAllowed);
	}

	/** Get Maximum Line Discount %.
		@return Discount in percent
	  */
	public BigDecimal getMaximumLineDiscountAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumLineDiscountAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Maximum Refund Allowed.
		@param MaximumRefundAllowed 
		Set the maximum refund allowed for this tender type using the POS currency
	  */
	public void setMaximumRefundAllowed (BigDecimal MaximumRefundAllowed)
	{
		set_Value (COLUMNNAME_MaximumRefundAllowed, MaximumRefundAllowed);
	}

	/** Get Maximum Refund Allowed.
		@return Set the maximum refund allowed for this tender type using the POS currency
	  */
	public BigDecimal getMaximumRefundAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumRefundAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Maximum Shema Discount %.
		@param MaximumShemaDiscountAllowed 
		Discount in percent
	  */
	public void setMaximumShemaDiscountAllowed (BigDecimal MaximumShemaDiscountAllowed)
	{
		set_Value (COLUMNNAME_MaximumShemaDiscountAllowed, MaximumShemaDiscountAllowed);
	}

	/** Get Maximum Shema Discount %.
		@return Discount in percent
	  */
	public BigDecimal getMaximumShemaDiscountAllowed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaximumShemaDiscountAllowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_AD_User getSalesRep() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getSalesRep_ID(), get_TrxName());	}

	/** Set Sales Representative.
		@param SalesRep_ID 
		Sales Representative or Company Agent
	  */
	public void setSalesRep_ID (int SalesRep_ID)
	{
		if (SalesRep_ID < 1) 
			set_Value (COLUMNNAME_SalesRep_ID, null);
		else 
			set_Value (COLUMNNAME_SalesRep_ID, Integer.valueOf(SalesRep_ID));
	}

	/** Get Sales Representative.
		@return Sales Representative or Company Agent
	  */
	public int getSalesRep_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SalesRep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getSalesRep_ID()));
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

	public I_C_Currency getWriteOffAmtCurrency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getWriteOffAmtCurrency_ID(), get_TrxName());	}

	/** Set Currency for write-off per Document.
		@param WriteOffAmtCurrency_ID 
		Currency amount to be written off in invoice currency
	  */
	public void setWriteOffAmtCurrency_ID (int WriteOffAmtCurrency_ID)
	{
		if (WriteOffAmtCurrency_ID < 1) 
			set_Value (COLUMNNAME_WriteOffAmtCurrency_ID, null);
		else 
			set_Value (COLUMNNAME_WriteOffAmtCurrency_ID, Integer.valueOf(WriteOffAmtCurrency_ID));
	}

	/** Get Currency for write-off per Document.
		@return Currency amount to be written off in invoice currency
	  */
	public int getWriteOffAmtCurrency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WriteOffAmtCurrency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Tolerance for write-off per Document.
		@param WriteOffAmtTolerance 
		Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffAmtTolerance (BigDecimal WriteOffAmtTolerance)
	{
		set_Value (COLUMNNAME_WriteOffAmtTolerance, WriteOffAmtTolerance);
	}

	/** Get Tolerance for write-off per Document.
		@return Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffAmtTolerance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WriteOffAmtTolerance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Tolerance for write-off %.
		@param WriteOffPercentageTolerance 
		Tolerance amount to be written off in invoice currency
	  */
	public void setWriteOffPercentageTolerance (BigDecimal WriteOffPercentageTolerance)
	{
		set_Value (COLUMNNAME_WriteOffPercentageTolerance, WriteOffPercentageTolerance);
	}

	/** Get Tolerance for write-off %.
		@return Tolerance amount to be written off in invoice currency
	  */
	public BigDecimal getWriteOffPercentageTolerance () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WriteOffPercentageTolerance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}