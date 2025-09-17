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
import java.util.Properties;

/** Generated Model for C_PPBatchConfigurationLine
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_PPBatchConfigurationLine extends PO implements I_C_PPBatchConfigurationLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250916L;

    /** Standard Constructor */
    public X_C_PPBatchConfigurationLine (Properties ctx, int C_PPBatchConfigurationLine_ID, String trxName)
    {
      super (ctx, C_PPBatchConfigurationLine_ID, trxName);
      /** if (C_PPBatchConfigurationLine_ID == 0)
        {
			setC_PPBatchConfiguration_ID (0);
			setC_PPBatchConfigurationLine_ID (0);
        } */
    }

    /** Load Constructor */
    public X_C_PPBatchConfigurationLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_PPBatchConfigurationLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_BankAccount getC_BankAccount() throws RuntimeException
    {
		return (I_C_BankAccount)MTable.get(getCtx(), I_C_BankAccount.Table_Name)
			.getPO(getC_BankAccount_ID(), get_TrxName());	}

	/** Set Bank Account.
		@param C_BankAccount_ID 
		Account at the Bank
	  */
	public void setC_BankAccount_ID (int C_BankAccount_ID)
	{
		if (C_BankAccount_ID < 1) 
			set_Value (COLUMNNAME_C_BankAccount_ID, null);
		else 
			set_Value (COLUMNNAME_C_BankAccount_ID, Integer.valueOf(C_BankAccount_ID));
	}

	/** Get Bank Account.
		@return Account at the Bank
	  */
	public int getC_BankAccount_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BankAccount_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_PPBatchConfiguration getC_PPBatchConfiguration() throws RuntimeException
    {
		return (I_C_PPBatchConfiguration)MTable.get(getCtx(), I_C_PPBatchConfiguration.Table_Name)
			.getPO(getC_PPBatchConfiguration_ID(), get_TrxName());	}

	/** Set Payment Processor Batch Configuration.
		@param C_PPBatchConfiguration_ID Payment Processor Batch Configuration	  */
	public void setC_PPBatchConfiguration_ID (int C_PPBatchConfiguration_ID)
	{
		if (C_PPBatchConfiguration_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_PPBatchConfiguration_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_PPBatchConfiguration_ID, Integer.valueOf(C_PPBatchConfiguration_ID));
	}

	/** Get Payment Processor Batch Configuration.
		@return Payment Processor Batch Configuration	  */
	public int getC_PPBatchConfiguration_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PPBatchConfiguration_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Payment Processor Batch Configuration Line.
		@param C_PPBatchConfigurationLine_ID Payment Processor Batch Configuration Line	  */
	public void setC_PPBatchConfigurationLine_ID (int C_PPBatchConfigurationLine_ID)
	{
		if (C_PPBatchConfigurationLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_PPBatchConfigurationLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_PPBatchConfigurationLine_ID, Integer.valueOf(C_PPBatchConfigurationLine_ID));
	}

	/** Get Payment Processor Batch Configuration Line.
		@return Payment Processor Batch Configuration Line	  */
	public int getC_PPBatchConfigurationLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PPBatchConfigurationLine_ID);
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
}