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

import org.adempiere.core.domains.models.I_LCO_MunicipalConcept;
import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for LCO_MunicipalConcept
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_LCO_MunicipalConcept extends PO implements I_LCO_MunicipalConcept, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260601L;

    /** Standard Constructor */
    public X_LCO_MunicipalConcept (Properties ctx, int LCO_MunicipalConcept_ID, String trxName)
    {
      super (ctx, LCO_MunicipalConcept_ID, trxName);
      /** if (LCO_MunicipalConcept_ID == 0)
        {
			setLCO_MunicipalConcept_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_LCO_MunicipalConcept (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LCO_MunicipalConcept[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.adempiere.core.domains.models.I_C_Currency getC_Currency() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Currency)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Currency.Table_Name)
			.getPO(getC_Currency_ID(), get_TrxName());	}

	/** Set Currency.
		@param C_Currency_ID 
		The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1) 
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else 
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
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

	/** Set C.I.I.U. Code.
		@param CIIUCode C.I.I.U. Code	  */
	public void setCIIUCode (String CIIUCode)
	{
		set_Value (COLUMNNAME_CIIUCode, CIIUCode);
	}

	/** Get C.I.I.U. Code.
		@return C.I.I.U. Code	  */
	public String getCIIUCode () 
	{
		return (String)get_Value(COLUMNNAME_CIIUCode);
	}

	public org.adempiere.core.domains.models.I_C_Region getC_Region() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Region)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Region.Table_Name)
			.getPO(getC_Region_ID(), get_TrxName());	}

	/** Set Region.
		@param C_Region_ID 
		Identifies a geographical Region
	  */
	public void setC_Region_ID (int C_Region_ID)
	{
		if (C_Region_ID < 1) 
			set_Value (COLUMNNAME_C_Region_ID, null);
		else 
			set_Value (COLUMNNAME_C_Region_ID, Integer.valueOf(C_Region_ID));
	}

	/** Get Region.
		@return Identifies a geographical Region
	  */
	public int getC_Region_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Region_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Cumulative Withholding.
		@param IsCumulativeWithholding 
		Retencion acumulativa en el periodo.
	  */
	public void setIsCumulativeWithholding (boolean IsCumulativeWithholding)
	{
		set_Value (COLUMNNAME_IsCumulativeWithholding, Boolean.valueOf(IsCumulativeWithholding));
	}

	/** Get Cumulative Withholding.
		@return Retencion acumulativa en el periodo.
	  */
	public boolean isCumulativeWithholding () 
	{
		Object oo = get_Value(COLUMNNAME_IsCumulativeWithholding);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Municipal Concept.
		@param LCO_MunicipalConcept_ID Municipal Concept	  */
	public void setLCO_MunicipalConcept_ID (int LCO_MunicipalConcept_ID)
	{
		if (LCO_MunicipalConcept_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LCO_MunicipalConcept_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LCO_MunicipalConcept_ID, Integer.valueOf(LCO_MunicipalConcept_ID));
	}

	/** Get Municipal Concept.
		@return Municipal Concept	  */
	public int getLCO_MunicipalConcept_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_MunicipalConcept_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** LCO_PersonType AD_Reference_ID=2000118 */
	public static final int LCO_PERSONTYPE_AD_Reference_ID=2000118;
	/** Persona Jurídica = PJ */
	public static final String LCO_PERSONTYPE_PersonaJurídica = "PJ";
	/** Persona Natural = PN */
	public static final String LCO_PERSONTYPE_PersonaNatural = "PN";
	/** Set Person Type.
		@param LCO_PersonType 
		Tipo de persona al que aplica.
	  */
	public void setLCO_PersonType (String LCO_PersonType)
	{

		set_Value (COLUMNNAME_LCO_PersonType, LCO_PersonType);
	}

	/** Get Person Type.
		@return Tipo de persona al que aplica.
	  */
	public String getLCO_PersonType () 
	{
		return (String)get_Value(COLUMNNAME_LCO_PersonType);
	}

	/** Set Minimum U.V.T..
		@param MinimumUVT 
		Cuantía mínima sujeta, en U.V.T.
	  */
	public void setMinimumUVT (BigDecimal MinimumUVT)
	{
		set_Value (COLUMNNAME_MinimumUVT, MinimumUVT);
	}

	/** Get Minimum U.V.T..
		@return Cuantía mínima sujeta, en U.V.T.
	  */
	public BigDecimal getMinimumUVT () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MinimumUVT);
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

	/** Set Rate per Thousand.
		@param RatePerThousand Rate per Thousand	  */
	public void setRatePerThousand (BigDecimal RatePerThousand)
	{
		set_Value (COLUMNNAME_RatePerThousand, RatePerThousand);
	}

	/** Get Rate per Thousand.
		@return Rate per Thousand	  */
	public BigDecimal getRatePerThousand () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_RatePerThousand);
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

	/** Set Withholding Base Rate.
		@param WithholdingBaseRate 
		Porcentaje de la base sujeta a retencion (p. ej. AIU, 75%).
	  */
	public void setWithholdingBaseRate (BigDecimal WithholdingBaseRate)
	{
		set_Value (COLUMNNAME_WithholdingBaseRate, WithholdingBaseRate);
	}

	/** Get Withholding Base Rate.
		@return Porcentaje de la base sujeta a retencion (p. ej. AIU, 75%).
	  */
	public BigDecimal getWithholdingBaseRate () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WithholdingBaseRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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