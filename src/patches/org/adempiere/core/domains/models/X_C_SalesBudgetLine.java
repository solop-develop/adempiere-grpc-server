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

/** Generated Model for C_SalesBudgetLine
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_SalesBudgetLine extends PO implements I_C_SalesBudgetLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260224L;

    /** Standard Constructor */
    public X_C_SalesBudgetLine (Properties ctx, int C_SalesBudgetLine_ID, String trxName)
    {
      super (ctx, C_SalesBudgetLine_ID, trxName);
      /** if (C_SalesBudgetLine_ID == 0)
        {
			setBudgetAmt (Env.ZERO);
// 0
			setC_Period_ID (0);
			setC_SalesBudget_ID (0);
			setC_SalesBudgetLine_ID (0);
			setLine (0);
// @SQL=SELECT COALESCE(MAX(Line),0)+10 FROM C_SalesBudgetLine WHERE C_SalesBudget_ID=@C_SalesBudget_ID@
        } */
    }

    /** Load Constructor */
    public X_C_SalesBudgetLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_SalesBudgetLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Actual Amount.
		@param ActualAmt 
		The actual amount
	  */
	public void setActualAmt (BigDecimal ActualAmt)
	{
		set_Value (COLUMNNAME_ActualAmt, ActualAmt);
	}

	/** Get Actual Amount.
		@return The actual amount
	  */
	public BigDecimal getActualAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ActualAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Budget Amount.
		@param BudgetAmt 
		Budgeted sales amount for this line
	  */
	public void setBudgetAmt (BigDecimal BudgetAmt)
	{
		set_Value (COLUMNNAME_BudgetAmt, BudgetAmt);
	}

	/** Get Budget Amount.
		@return Budgeted sales amount for this line
	  */
	public BigDecimal getBudgetAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BudgetAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Budget Quantity.
		@param BudgetQty 
		Budgeted sales quantity for this line
	  */
	public void setBudgetQty (BigDecimal BudgetQty)
	{
		set_Value (COLUMNNAME_BudgetQty, BudgetQty);
	}

	/** Get Budget Quantity.
		@return Budgeted sales quantity for this line
	  */
	public BigDecimal getBudgetQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_BudgetQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_C_Channel getC_Channel() throws RuntimeException
    {
		return (I_C_Channel)MTable.get(getCtx(), I_C_Channel.Table_Name)
			.getPO(getC_Channel_ID(), get_TrxName());	}

	/** Set Channel.
		@param C_Channel_ID 
		Sales Channel
	  */
	public void setC_Channel_ID (int C_Channel_ID)
	{
		if (C_Channel_ID < 1) 
			set_Value (COLUMNNAME_C_Channel_ID, null);
		else 
			set_Value (COLUMNNAME_C_Channel_ID, Integer.valueOf(C_Channel_ID));
	}

	/** Get Channel.
		@return Sales Channel
	  */
	public int getC_Channel_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Channel_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Period getC_Period() throws RuntimeException
    {
		return (I_C_Period)MTable.get(getCtx(), I_C_Period.Table_Name)
			.getPO(getC_Period_ID(), get_TrxName());	}

	/** Set Period.
		@param C_Period_ID 
		Period of the Calendar
	  */
	public void setC_Period_ID (int C_Period_ID)
	{
		if (C_Period_ID < 1) 
			set_Value (COLUMNNAME_C_Period_ID, null);
		else 
			set_Value (COLUMNNAME_C_Period_ID, Integer.valueOf(C_Period_ID));
	}

	/** Get Period.
		@return Period of the Calendar
	  */
	public int getC_Period_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Period_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_SalesBudget getC_SalesBudget() throws RuntimeException
    {
		return (I_C_SalesBudget)MTable.get(getCtx(), I_C_SalesBudget.Table_Name)
			.getPO(getC_SalesBudget_ID(), get_TrxName());	}

	/** Set Sales Budget.
		@param C_SalesBudget_ID Sales Budget	  */
	public void setC_SalesBudget_ID (int C_SalesBudget_ID)
	{
		if (C_SalesBudget_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_SalesBudget_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_SalesBudget_ID, Integer.valueOf(C_SalesBudget_ID));
	}

	/** Get Sales Budget.
		@return Sales Budget	  */
	public int getC_SalesBudget_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_SalesBudget_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Sales Budget Line.
		@param C_SalesBudgetLine_ID Sales Budget Line	  */
	public void setC_SalesBudgetLine_ID (int C_SalesBudgetLine_ID)
	{
		if (C_SalesBudgetLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_SalesBudgetLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_SalesBudgetLine_ID, Integer.valueOf(C_SalesBudgetLine_ID));
	}

	/** Get Sales Budget Line.
		@return Sales Budget Line	  */
	public int getC_SalesBudgetLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_SalesBudgetLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_SalesRegion getC_SalesRegion() throws RuntimeException
    {
		return (I_C_SalesRegion)MTable.get(getCtx(), I_C_SalesRegion.Table_Name)
			.getPO(getC_SalesRegion_ID(), get_TrxName());	}

	/** Set Sales Region.
		@param C_SalesRegion_ID 
		Sales coverage region
	  */
	public void setC_SalesRegion_ID (int C_SalesRegion_ID)
	{
		if (C_SalesRegion_ID < 1) 
			set_Value (COLUMNNAME_C_SalesRegion_ID, null);
		else 
			set_Value (COLUMNNAME_C_SalesRegion_ID, Integer.valueOf(C_SalesRegion_ID));
	}

	/** Get Sales Region.
		@return Sales coverage region
	  */
	public int getC_SalesRegion_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_SalesRegion_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Cumulative Actual.
		@param CumulativeActualAmt 
		Year-to-date cumulative actual sales amount
	  */
	public void setCumulativeActualAmt (BigDecimal CumulativeActualAmt)
	{
		set_Value (COLUMNNAME_CumulativeActualAmt, CumulativeActualAmt);
	}

	/** Get Cumulative Actual.
		@return Year-to-date cumulative actual sales amount
	  */
	public BigDecimal getCumulativeActualAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_CumulativeActualAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Cumulative Budget.
		@param CumulativeBudgetAmt 
		Year-to-date cumulative budget amount
	  */
	public void setCumulativeBudgetAmt (BigDecimal CumulativeBudgetAmt)
	{
		set_Value (COLUMNNAME_CumulativeBudgetAmt, CumulativeBudgetAmt);
	}

	/** Get Cumulative Budget.
		@return Year-to-date cumulative budget amount
	  */
	public BigDecimal getCumulativeBudgetAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_CumulativeBudgetAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Line No.
		@param Line 
		Unique line for this document
	  */
	public void setLine (int Line)
	{
		set_Value (COLUMNNAME_Line, Integer.valueOf(Line));
	}

	/** Get Line No.
		@return Unique line for this document
	  */
	public int getLine () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Line);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Variance Amount.
		@param VarianceAmt 
		Difference between actual and budgeted amount
	  */
	public void setVarianceAmt (BigDecimal VarianceAmt)
	{
		set_Value (COLUMNNAME_VarianceAmt, VarianceAmt);
	}

	/** Get Variance Amount.
		@return Difference between actual and budgeted amount
	  */
	public BigDecimal getVarianceAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_VarianceAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Variance %.
		@param VariancePct 
		Variance expressed as percentage
	  */
	public void setVariancePct (BigDecimal VariancePct)
	{
		set_Value (COLUMNNAME_VariancePct, VariancePct);
	}

	/** Get Variance %.
		@return Variance expressed as percentage
	  */
	public BigDecimal getVariancePct () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_VariancePct);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}