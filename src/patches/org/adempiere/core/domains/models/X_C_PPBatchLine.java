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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for C_PPBatchLine
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_PPBatchLine extends PO implements I_C_PPBatchLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260413L;

    /** Standard Constructor */
    public X_C_PPBatchLine (Properties ctx, int C_PPBatchLine_ID, String trxName)
    {
      super (ctx, C_PPBatchLine_ID, trxName);
      /** if (C_PPBatchLine_ID == 0)
        {
			setC_Payment_ID (0);
			setC_PaymentProcessorBatch_ID (0);
			setC_PPBatchLine_ID (0);
        } */
    }

    /** Load Constructor */
    public X_C_PPBatchLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_PPBatchLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_BankStatement getC_BankStatement() throws RuntimeException
    {
		return (I_C_BankStatement)MTable.get(getCtx(), I_C_BankStatement.Table_Name)
			.getPO(getC_BankStatement_ID(), get_TrxName());	}

	/** Set Bank Statement.
		@param C_BankStatement_ID 
		Bank Statement of account
	  */
	public void setC_BankStatement_ID (int C_BankStatement_ID)
	{
		if (C_BankStatement_ID < 1) 
			set_Value (COLUMNNAME_C_BankStatement_ID, null);
		else 
			set_Value (COLUMNNAME_C_BankStatement_ID, Integer.valueOf(C_BankStatement_ID));
	}

	/** Get Bank Statement.
		@return Bank Statement of account
	  */
	public int getC_BankStatement_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankStatement_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_BankStatementLine getC_BankStatementLine() throws RuntimeException
    {
		return (I_C_BankStatementLine)MTable.get(getCtx(), I_C_BankStatementLine.Table_Name)
			.getPO(getC_BankStatementLine_ID(), get_TrxName());	}

	/** Set Bank statement line.
		@param C_BankStatementLine_ID 
		Line on a statement from this Bank
	  */
	public void setC_BankStatementLine_ID (int C_BankStatementLine_ID)
	{
		if (C_BankStatementLine_ID < 1) 
			set_Value (COLUMNNAME_C_BankStatementLine_ID, null);
		else 
			set_Value (COLUMNNAME_C_BankStatementLine_ID, Integer.valueOf(C_BankStatementLine_ID));
	}

	/** Get Bank statement line.
		@return Line on a statement from this Bank
	  */
	public int getC_BankStatementLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankStatementLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Payment getC_Payment() throws RuntimeException
    {
		return (I_C_Payment)MTable.get(getCtx(), I_C_Payment.Table_Name)
			.getPO(getC_Payment_ID(), get_TrxName());	}

	/** Set Payment.
		@param C_Payment_ID 
		Payment identifier
	  */
	public void setC_Payment_ID (int C_Payment_ID)
	{
		if (C_Payment_ID < 1) 
			set_Value (COLUMNNAME_C_Payment_ID, null);
		else 
			set_Value (COLUMNNAME_C_Payment_ID, Integer.valueOf(C_Payment_ID));
	}

	/** Get Payment.
		@return Payment identifier
	  */
	public int getC_Payment_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Payment_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_PaymentProcessorBatch getC_PaymentProcessorBatch() throws RuntimeException
    {
		return (I_C_PaymentProcessorBatch)MTable.get(getCtx(), I_C_PaymentProcessorBatch.Table_Name)
			.getPO(getC_PaymentProcessorBatch_ID(), get_TrxName());	}

	/** Set Payment Processor Batch.
		@param C_PaymentProcessorBatch_ID 
		Payment Processor Batch
	  */
	public void setC_PaymentProcessorBatch_ID (int C_PaymentProcessorBatch_ID)
	{
		if (C_PaymentProcessorBatch_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_PaymentProcessorBatch_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_PaymentProcessorBatch_ID, Integer.valueOf(C_PaymentProcessorBatch_ID));
	}

	/** Get Payment Processor Batch.
		@return Payment Processor Batch
	  */
	public int getC_PaymentProcessorBatch_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PaymentProcessorBatch_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Payment Processor Batch Line.
		@param C_PPBatchLine_ID Payment Processor Batch Line	  */
	public void setC_PPBatchLine_ID (int C_PPBatchLine_ID)
	{
		if (C_PPBatchLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_PPBatchLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_PPBatchLine_ID, Integer.valueOf(C_PPBatchLine_ID));
	}

	/** Get Payment Processor Batch Line.
		@return Payment Processor Batch Line	  */
	public int getC_PPBatchLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PPBatchLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Payment getDeposit() throws RuntimeException
    {
		return (I_C_Payment)MTable.get(getCtx(), I_C_Payment.Table_Name)
			.getPO(getDeposit_ID(), get_TrxName());	}

	/** Set Deposit Reference.
		@param Deposit_ID 
		Deposit Reference for payment
	  */
	public void setDeposit_ID (int Deposit_ID)
	{
		if (Deposit_ID < 1) 
			set_Value (COLUMNNAME_Deposit_ID, null);
		else 
			set_Value (COLUMNNAME_Deposit_ID, Integer.valueOf(Deposit_ID));
	}

	/** Get Deposit Reference.
		@return Deposit Reference for payment
	  */
	public int getDeposit_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Deposit_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Discount Amount.
		@param DiscountAmt 
		Calculated amount of discount
	  */
	public void setDiscountAmt (BigDecimal DiscountAmt)
	{
		set_Value (COLUMNNAME_DiscountAmt, DiscountAmt);
	}

	/** Get Discount Amount.
		@return Calculated amount of discount
	  */
	public BigDecimal getDiscountAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_DiscountAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Fee Amount.
		@param FeeAmt 
		Fee amount in invoice currency
	  */
	public void setFeeAmt (BigDecimal FeeAmt)
	{
		set_Value (COLUMNNAME_FeeAmt, FeeAmt);
	}

	/** Get Fee Amount.
		@return Fee amount in invoice currency
	  */
	public BigDecimal getFeeAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_FeeAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Manual.
		@param IsManual 
		This is a manual process
	  */
	public void setIsManual (boolean IsManual)
	{
		set_Value (COLUMNNAME_IsManual, Boolean.valueOf(IsManual));
	}

	/** Get Manual.
		@return This is a manual process
	  */
	public boolean isManual () 
	{
		Object oo = get_Value(COLUMNNAME_IsManual);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Payment amount.
		@param PayAmt 
		Amount being paid
	  */
	public void setPayAmt (BigDecimal PayAmt)
	{
		set_Value (COLUMNNAME_PayAmt, PayAmt);
	}

	/** Get Payment amount.
		@return Amount being paid
	  */
	public BigDecimal getPayAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PayAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Processed.
		@param Processed 
		The document has been processed
	  */
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Tax Amount.
		@param TaxAmt 
		Tax Amount for a document
	  */
	public void setTaxAmt (BigDecimal TaxAmt)
	{
		set_Value (COLUMNNAME_TaxAmt, TaxAmt);
	}

	/** Get Tax Amount.
		@return Tax Amount for a document
	  */
	public BigDecimal getTaxAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TaxAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Total Amount.
		@param TotalAmt 
		Total Amount
	  */
	public void setTotalAmt (BigDecimal TotalAmt)
	{
		set_Value (COLUMNNAME_TotalAmt, TotalAmt);
	}

	/** Get Total Amount.
		@return Total Amount
	  */
	public BigDecimal getTotalAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	public I_C_Payment getWithdrawal() throws RuntimeException
    {
		return (I_C_Payment)MTable.get(getCtx(), I_C_Payment.Table_Name)
			.getPO(getWithdrawal_ID(), get_TrxName());	}

	/** Set Withdrawal.
		@param Withdrawal_ID 
		Withdrawal Payment
	  */
	public void setWithdrawal_ID (int Withdrawal_ID)
	{
		if (Withdrawal_ID < 1) 
			set_Value (COLUMNNAME_Withdrawal_ID, null);
		else 
			set_Value (COLUMNNAME_Withdrawal_ID, Integer.valueOf(Withdrawal_ID));
	}

	/** Get Withdrawal.
		@return Withdrawal Payment
	  */
	public int getWithdrawal_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Withdrawal_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Withholding Amt.
		@param WithholdingAmt Withholding Amt	  */
	public void setWithholdingAmt (BigDecimal WithholdingAmt)
	{
		set_Value (COLUMNNAME_WithholdingAmt, WithholdingAmt);
	}

	/** Get Withholding Amt.
		@return Withholding Amt	  */
	public BigDecimal getWithholdingAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WithholdingAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}