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
package com.solop.lco.model;

import com.solop.lco.model.I_LCO_WH_IVARate;
import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for LCO_WH_IVARate
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_LCO_WH_IVARate extends PO implements I_LCO_WH_IVARate, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260601L;

    /** Standard Constructor */
    public X_LCO_WH_IVARate (Properties ctx, int LCO_WH_IVARate_ID, String trxName)
    {
      super (ctx, LCO_WH_IVARate_ID, trxName);
      /** if (LCO_WH_IVARate_ID == 0)
        {
			setLCO_WH_IVARate_ID (0);
			setName (null);
			setPrintedName (null);
			setValue (null);
			setWithholdingRate (Env.ZERO);
// 0
        } */
    }

    /** Load Constructor */
    public X_LCO_WH_IVARate (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LCO_WH_IVARate[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set I.V.A. Withholding Rate.
		@param LCO_WH_IVARate_ID I.V.A. Withholding Rate	  */
	public void setLCO_WH_IVARate_ID (int LCO_WH_IVARate_ID)
	{
		if (LCO_WH_IVARate_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LCO_WH_IVARate_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LCO_WH_IVARate_ID, Integer.valueOf(LCO_WH_IVARate_ID));
	}

	/** Get I.V.A. Withholding Rate.
		@return I.V.A. Withholding Rate	  */
	public int getLCO_WH_IVARate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_WH_IVARate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Minimum U.V.T. for Goods.
		@param MinUVTProduct 
		Mínimo de U.V.T. del I.V.A. para retener en bienes/productos.
	  */
	public void setMinUVTProduct (BigDecimal MinUVTProduct)
	{
		set_Value (COLUMNNAME_MinUVTProduct, MinUVTProduct);
	}

	/** Get Minimum U.V.T. for Goods.
		@return Mínimo de U.V.T. del I.V.A. para retener en bienes/productos.
	  */
	public BigDecimal getMinUVTProduct () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MinUVTProduct);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Minimum U.V.T. for Services.
		@param MinUVTService 
		Mínimo de U.V.T. del I.V.A. para retener en servicios.
	  */
	public void setMinUVTService (BigDecimal MinUVTService)
	{
		set_Value (COLUMNNAME_MinUVTService, MinUVTService);
	}

	/** Get Minimum U.V.T. for Services.
		@return Mínimo de U.V.T. del I.V.A. para retener en servicios.
	  */
	public BigDecimal getMinUVTService () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MinUVTService);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Printed Name.
		@param PrintedName 
		Nombre a imprimir en el documento de retencion.
	  */
	public void setPrintedName (String PrintedName)
	{
		set_Value (COLUMNNAME_PrintedName, PrintedName);
	}

	/** Get Printed Name.
		@return Nombre a imprimir en el documento de retencion.
	  */
	public String getPrintedName () 
	{
		return (String)get_Value(COLUMNNAME_PrintedName);
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

	/** Set Withholding Rate.
		@param WithholdingRate 
		Withholding Rate applied to Document
	  */
	public void setWithholdingRate (BigDecimal WithholdingRate)
	{
		set_Value (COLUMNNAME_WithholdingRate, WithholdingRate);
	}

	/** Get Withholding Rate.
		@return Withholding Rate applied to Document
	  */
	public BigDecimal getWithholdingRate () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WithholdingRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}