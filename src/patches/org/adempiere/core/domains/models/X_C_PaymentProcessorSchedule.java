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
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for C_PaymentProcessorSchedule
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_PaymentProcessorSchedule extends PO implements I_C_PaymentProcessorSchedule, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250905L;

    /** Standard Constructor */
    public X_C_PaymentProcessorSchedule (Properties ctx, int C_PaymentProcessorSchedule_ID, String trxName)
    {
      super (ctx, C_PaymentProcessorSchedule_ID, trxName);
      /** if (C_PaymentProcessorSchedule_ID == 0)
        {
			setC_PaymentProcessorSchedule_ID (0);
        } */
    }

    /** Load Constructor */
    public X_C_PaymentProcessorSchedule (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_PaymentProcessorSchedule[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Amount.
		@param Amount 
		Amount in a defined currency
	  */
	public void setAmount (BigDecimal Amount)
	{
		set_Value (COLUMNNAME_Amount, Amount);
	}

	/** Get Amount.
		@return Amount in a defined currency
	  */
	public BigDecimal getAmount () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Amount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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
			set_Value (COLUMNNAME_C_PaymentProcessorBatch_ID, null);
		else 
			set_Value (COLUMNNAME_C_PaymentProcessorBatch_ID, Integer.valueOf(C_PaymentProcessorBatch_ID));
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

	/** Set Payment Processor Batch Schedule.
		@param C_PaymentProcessorSchedule_ID Payment Processor Batch Schedule	  */
	public void setC_PaymentProcessorSchedule_ID (int C_PaymentProcessorSchedule_ID)
	{
		if (C_PaymentProcessorSchedule_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_PaymentProcessorSchedule_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_PaymentProcessorSchedule_ID, Integer.valueOf(C_PaymentProcessorSchedule_ID));
	}

	/** Get Payment Processor Batch Schedule.
		@return Payment Processor Batch Schedule	  */
	public int getC_PaymentProcessorSchedule_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_PaymentProcessorSchedule_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Document Date.
		@param DateDoc 
		Date of the Document
	  */
	public void setDateDoc (Timestamp DateDoc)
	{
		set_Value (COLUMNNAME_DateDoc, DateDoc);
	}

	/** Get Document Date.
		@return Date of the Document
	  */
	public Timestamp getDateDoc () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DateDoc);
	}

	/** Set Reference No.
		@param ReferenceNo 
		Your customer or vendor number at the Business Partner's site
	  */
	public void setReferenceNo (String ReferenceNo)
	{
		set_Value (COLUMNNAME_ReferenceNo, ReferenceNo);
	}

	/** Get Reference No.
		@return Your customer or vendor number at the Business Partner's site
	  */
	public String getReferenceNo () 
	{
		return (String)get_Value(COLUMNNAME_ReferenceNo);
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