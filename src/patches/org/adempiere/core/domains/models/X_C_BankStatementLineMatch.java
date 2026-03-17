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
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for C_BankStatementLineMatch
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_BankStatementLineMatch extends PO implements I_C_BankStatementLineMatch, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260317L;

    /** Standard Constructor */
    public X_C_BankStatementLineMatch (Properties ctx, int C_BankStatementLineMatch_ID, String trxName)
    {
      super (ctx, C_BankStatementLineMatch_ID, trxName);
      /** if (C_BankStatementLineMatch_ID == 0)
        {
			setC_BankStatementLineMatch_ID (0);
			setC_Currency_ID (0);
			setC_Payment_ID (0);
			setMatchDate (new Timestamp( System.currentTimeMillis() ));
// @#Date@
        } */
    }

    /** Load Constructor */
    public X_C_BankStatementLineMatch (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_BankStatementLineMatch[")
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
			set_ValueNoCheck (COLUMNNAME_C_BankStatementLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_BankStatementLine_ID, Integer.valueOf(C_BankStatementLine_ID));
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

	/** Set Bank Statement Line Match.
		@param C_BankStatementLineMatch_ID Bank Statement Line Match	  */
	public void setC_BankStatementLineMatch_ID (int C_BankStatementLineMatch_ID)
	{
		if (C_BankStatementLineMatch_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_BankStatementLineMatch_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_BankStatementLineMatch_ID, Integer.valueOf(C_BankStatementLineMatch_ID));
	}

	/** Get Bank Statement Line Match.
		@return Bank Statement Line Match	  */
	public int getC_BankStatementLineMatch_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankStatementLineMatch_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Currency getC_Currency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getC_Currency_ID(), get_TrxName());	}

	/** Set Currency.
		@param C_Currency_ID 
		The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_Currency_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
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
			set_ValueNoCheck (COLUMNNAME_C_Payment_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_Payment_ID, Integer.valueOf(C_Payment_ID));
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

	public I_I_BankStatement getI_BankStatement() throws RuntimeException
    {
		return (I_I_BankStatement)MTable.get(getCtx(), I_I_BankStatement.Table_Name)
			.getPO(getI_BankStatement_ID(), get_TrxName());	}

	/** Set Import Bank Statement.
		@param I_BankStatement_ID 
		Import of the Bank Statement
	  */
	public void setI_BankStatement_ID (int I_BankStatement_ID)
	{
		if (I_BankStatement_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_I_BankStatement_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_I_BankStatement_ID, Integer.valueOf(I_BankStatement_ID));
	}

	/** Get Import Bank Statement.
		@return Import of the Bank Statement
	  */
	public int getI_BankStatement_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_I_BankStatement_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Manual Match.
		@param IsManualMatch Manual Match	  */
	public void setIsManualMatch (boolean IsManualMatch)
	{
		set_Value (COLUMNNAME_IsManualMatch, Boolean.valueOf(IsManualMatch));
	}

	/** Get Manual Match.
		@return Manual Match	  */
	public boolean isManualMatch () 
	{
		Object oo = get_Value(COLUMNNAME_IsManualMatch);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Match Date.
		@param MatchDate 
		Date when the match between the bank line and the payment was confirmed
	  */
	public void setMatchDate (Timestamp MatchDate)
	{
		set_ValueNoCheck (COLUMNNAME_MatchDate, MatchDate);
	}

	/** Get Match Date.
		@return Date when the match between the bank line and the payment was confirmed
	  */
	public Timestamp getMatchDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_MatchDate);
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