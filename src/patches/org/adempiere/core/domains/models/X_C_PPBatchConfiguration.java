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

/** Generated Model for C_PPBatchConfiguration
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_PPBatchConfiguration extends PO implements I_C_PPBatchConfiguration, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250916L;

    /** Standard Constructor */
    public X_C_PPBatchConfiguration (Properties ctx, int C_PPBatchConfiguration_ID, String trxName)
    {
      super (ctx, C_PPBatchConfiguration_ID, trxName);
      /** if (C_PPBatchConfiguration_ID == 0)
        {
			setC_PPBatchConfiguration_ID (0);
        } */
    }

    /** Load Constructor */
    public X_C_PPBatchConfiguration (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_PPBatchConfiguration[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_BPartner getC_BPartner() throws RuntimeException
    {
		return (I_C_BPartner)MTable.get(getCtx(), I_C_BPartner.Table_Name)
			.getPO(getC_BPartner_ID(), get_TrxName());	}

	/** Set Business Partner .
		@param C_BPartner_ID 
		Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1) 
			set_Value (COLUMNNAME_C_BPartner_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner .
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_BPartner_Location getC_BPartner_Location() throws RuntimeException
    {
		return (I_C_BPartner_Location)MTable.get(getCtx(), I_C_BPartner_Location.Table_Name)
			.getPO(getC_BPartner_Location_ID(), get_TrxName());	}

	/** Set Partner Location.
		@param C_BPartner_Location_ID 
		Identifies the (ship to) address for this Business Partner
	  */
	public void setC_BPartner_Location_ID (int C_BPartner_Location_ID)
	{
		if (C_BPartner_Location_ID < 1) 
			set_Value (COLUMNNAME_C_BPartner_Location_ID, null);
		else 
			set_Value (COLUMNNAME_C_BPartner_Location_ID, Integer.valueOf(C_BPartner_Location_ID));
	}

	/** Get Partner Location.
		@return Identifies the (ship to) address for this Business Partner
	  */
	public int getC_BPartner_Location_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_Location_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

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

	public I_C_Charge getFeeCharge() throws RuntimeException
    {
		return (I_C_Charge)MTable.get(getCtx(), I_C_Charge.Table_Name)
			.getPO(getFeeCharge_ID(), get_TrxName());	}

	/** Set Charge for Fee.
		@param FeeCharge_ID 
		Charge for Fee
	  */
	public void setFeeCharge_ID (int FeeCharge_ID)
	{
		if (FeeCharge_ID < 1) 
			set_Value (COLUMNNAME_FeeCharge_ID, null);
		else 
			set_Value (COLUMNNAME_FeeCharge_ID, Integer.valueOf(FeeCharge_ID));
	}

	/** Get Charge for Fee.
		@return Charge for Fee
	  */
	public int getFeeCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FeeCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_Currency getFeeCurrency() throws RuntimeException
    {
		return (I_C_Currency)MTable.get(getCtx(), I_C_Currency.Table_Name)
			.getPO(getFeeCurrency_ID(), get_TrxName());	}

	/** Set Currency for Fee.
		@param FeeCurrency_ID 
		Currency for Fee
	  */
	public void setFeeCurrency_ID (int FeeCurrency_ID)
	{
		if (FeeCurrency_ID < 1) 
			set_Value (COLUMNNAME_FeeCurrency_ID, null);
		else 
			set_Value (COLUMNNAME_FeeCurrency_ID, Integer.valueOf(FeeCurrency_ID));
	}

	/** Get Currency for Fee.
		@return Currency for Fee
	  */
	public int getFeeCurrency_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FeeCurrency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getPurchaseInvoiceDocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getPurchaseInvoiceDocType_ID(), get_TrxName());	}

	/** Set Purchase Invoice Document Type.
		@param PurchaseInvoiceDocType_ID Purchase Invoice Document Type	  */
	public void setPurchaseInvoiceDocType_ID (int PurchaseInvoiceDocType_ID)
	{
		if (PurchaseInvoiceDocType_ID < 1) 
			set_Value (COLUMNNAME_PurchaseInvoiceDocType_ID, null);
		else 
			set_Value (COLUMNNAME_PurchaseInvoiceDocType_ID, Integer.valueOf(PurchaseInvoiceDocType_ID));
	}

	/** Get Purchase Invoice Document Type.
		@return Purchase Invoice Document Type	  */
	public int getPurchaseInvoiceDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PurchaseInvoiceDocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getSalesInvoiceDocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getSalesInvoiceDocType_ID(), get_TrxName());	}

	/** Set Sales Invoice Document Type.
		@param SalesInvoiceDocType_ID Sales Invoice Document Type	  */
	public void setSalesInvoiceDocType_ID (int SalesInvoiceDocType_ID)
	{
		if (SalesInvoiceDocType_ID < 1) 
			set_Value (COLUMNNAME_SalesInvoiceDocType_ID, null);
		else 
			set_Value (COLUMNNAME_SalesInvoiceDocType_ID, Integer.valueOf(SalesInvoiceDocType_ID));
	}

	/** Get Sales Invoice Document Type.
		@return Sales Invoice Document Type	  */
	public int getSalesInvoiceDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SalesInvoiceDocType_ID);
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

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

	public I_C_Charge getWithholdingCharge() throws RuntimeException
    {
		return (I_C_Charge)MTable.get(getCtx(), I_C_Charge.Table_Name)
			.getPO(getWithholdingCharge_ID(), get_TrxName());	}

	/** Set Withholding Charge.
		@param WithholdingCharge_ID Withholding Charge	  */
	public void setWithholdingCharge_ID (int WithholdingCharge_ID)
	{
		if (WithholdingCharge_ID < 1) 
			set_Value (COLUMNNAME_WithholdingCharge_ID, null);
		else 
			set_Value (COLUMNNAME_WithholdingCharge_ID, Integer.valueOf(WithholdingCharge_ID));
	}

	/** Get Withholding Charge.
		@return Withholding Charge	  */
	public int getWithholdingCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WithholdingCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_DocType getWithholdingDocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getWithholdingDocType_ID(), get_TrxName());	}

	/** Set Withholding Document Type.
		@param WithholdingDocType_ID Withholding Document Type	  */
	public void setWithholdingDocType_ID (int WithholdingDocType_ID)
	{
		if (WithholdingDocType_ID < 1) 
			set_Value (COLUMNNAME_WithholdingDocType_ID, null);
		else 
			set_Value (COLUMNNAME_WithholdingDocType_ID, Integer.valueOf(WithholdingDocType_ID));
	}

	/** Get Withholding Document Type.
		@return Withholding Document Type	  */
	public int getWithholdingDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WithholdingDocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}