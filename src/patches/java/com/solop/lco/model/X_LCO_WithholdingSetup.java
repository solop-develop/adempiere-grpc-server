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

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for LCO_WithholdingSetup
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_LCO_WithholdingSetup extends PO implements I_LCO_WithholdingSetup, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260601L;

    /** Standard Constructor */
    public X_LCO_WithholdingSetup (Properties ctx, int LCO_WithholdingSetup_ID, String trxName)
    {
      super (ctx, LCO_WithholdingSetup_ID, trxName);
      /** if (LCO_WithholdingSetup_ID == 0)
        {
			setLCO_WithholdingSetup_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_LCO_WithholdingSetup (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LCO_WithholdingSetup[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.adempiere.core.domains.models.I_C_Charge getC_ICACharge() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Charge)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Charge.Table_Name)
			.getPO(getC_ICACharge_ID(), get_TrxName());	}

	/** Set ICA Withholding Charge.
		@param C_ICACharge_ID 
		Cargo contable para nota de ReteICA.
	  */
	public void setC_ICACharge_ID (int C_ICACharge_ID)
	{
		if (C_ICACharge_ID < 1) 
			set_Value (COLUMNNAME_C_ICACharge_ID, null);
		else 
			set_Value (COLUMNNAME_C_ICACharge_ID, Integer.valueOf(C_ICACharge_ID));
	}

	/** Get ICA Withholding Charge.
		@return Cargo contable para nota de ReteICA.
	  */
	public int getC_ICACharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ICACharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_C_Charge getC_IVACharge() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Charge)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Charge.Table_Name)
			.getPO(getC_IVACharge_ID(), get_TrxName());	}

	/** Set IVA Withholding Charge.
		@param C_IVACharge_ID 
		Cargo contable para nota de ReteIVA.
	  */
	public void setC_IVACharge_ID (int C_IVACharge_ID)
	{
		if (C_IVACharge_ID < 1) 
			set_Value (COLUMNNAME_C_IVACharge_ID, null);
		else 
			set_Value (COLUMNNAME_C_IVACharge_ID, Integer.valueOf(C_IVACharge_ID));
	}

	/** Get IVA Withholding Charge.
		@return Cargo contable para nota de ReteIVA.
	  */
	public int getC_IVACharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_IVACharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_C_Charge getC_RentaCharge() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Charge)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Charge.Table_Name)
			.getPO(getC_RentaCharge_ID(), get_TrxName());	}

	/** Set Income Withholding Charge.
		@param C_RentaCharge_ID 
		Cargo contable para nota de Retefuente.
	  */
	public void setC_RentaCharge_ID (int C_RentaCharge_ID)
	{
		if (C_RentaCharge_ID < 1) 
			set_Value (COLUMNNAME_C_RentaCharge_ID, null);
		else 
			set_Value (COLUMNNAME_C_RentaCharge_ID, Integer.valueOf(C_RentaCharge_ID));
	}

	/** Get Income Withholding Charge.
		@return Cargo contable para nota de Retefuente.
	  */
	public int getC_RentaCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_RentaCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_LCO_WH_Concept getDefaultICAConcept() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_LCO_WH_Concept)MTable.get(getCtx(), org.adempiere.core.domains.models.I_LCO_WH_Concept.Table_Name)
			.getPO(getDefaultICAConcept_ID(), get_TrxName());	}

	/** Set Default ICA Concept.
		@param DefaultICAConcept_ID 
		Concepto de ICA por defecto.
	  */
	public void setDefaultICAConcept_ID (int DefaultICAConcept_ID)
	{
		if (DefaultICAConcept_ID < 1) 
			set_Value (COLUMNNAME_DefaultICAConcept_ID, null);
		else 
			set_Value (COLUMNNAME_DefaultICAConcept_ID, Integer.valueOf(DefaultICAConcept_ID));
	}

	/** Get Default ICA Concept.
		@return Concepto de ICA por defecto.
	  */
	public int getDefaultICAConcept_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DefaultICAConcept_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_LCO_WH_IVARate getDefaultIVARate() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_LCO_WH_IVARate)MTable.get(getCtx(), org.adempiere.core.domains.models.I_LCO_WH_IVARate.Table_Name)
			.getPO(getDefaultIVARate_ID(), get_TrxName());	}

	/** Set Default IVA Withholding Rate.
		@param DefaultIVARate_ID 
		Tarifa de ReteIVA por defecto.
	  */
	public void setDefaultIVARate_ID (int DefaultIVARate_ID)
	{
		if (DefaultIVARate_ID < 1) 
			set_Value (COLUMNNAME_DefaultIVARate_ID, null);
		else 
			set_Value (COLUMNNAME_DefaultIVARate_ID, Integer.valueOf(DefaultIVARate_ID));
	}

	/** Get Default IVA Withholding Rate.
		@return Tarifa de ReteIVA por defecto.
	  */
	public int getDefaultIVARate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DefaultIVARate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_LCO_WH_Concept getDefaultRentaConcept() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_LCO_WH_Concept)MTable.get(getCtx(), org.adempiere.core.domains.models.I_LCO_WH_Concept.Table_Name)
			.getPO(getDefaultRentaConcept_ID(), get_TrxName());	}

	/** Set Default Income Withholding Concept.
		@param DefaultRentaConcept_ID 
		Concepto de renta por defecto.
	  */
	public void setDefaultRentaConcept_ID (int DefaultRentaConcept_ID)
	{
		if (DefaultRentaConcept_ID < 1) 
			set_Value (COLUMNNAME_DefaultRentaConcept_ID, null);
		else 
			set_Value (COLUMNNAME_DefaultRentaConcept_ID, Integer.valueOf(DefaultRentaConcept_ID));
	}

	/** Get Default Income Withholding Concept.
		@return Concepto de renta por defecto.
	  */
	public int getDefaultRentaConcept_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DefaultRentaConcept_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Client Excluded.
		@param IsClientExcluded 
		La organizacion (como cliente) esta excluida de que le retengan.
	  */
	public void setIsClientExcluded (boolean IsClientExcluded)
	{
		set_Value (COLUMNNAME_IsClientExcluded, Boolean.valueOf(IsClientExcluded));
	}

	/** Get Client Excluded.
		@return La organizacion (como cliente) esta excluida de que le retengan.
	  */
	public boolean isClientExcluded () 
	{
		Object oo = get_Value(COLUMNNAME_IsClientExcluded);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Exclude Municipal Tax.
		@param IsExcludeICAWithholding 
		Excluye el cálculo de Impuesto Municipal.
	  */
	public void setIsExcludeICAWithholding (boolean IsExcludeICAWithholding)
	{
		set_Value (COLUMNNAME_IsExcludeICAWithholding, Boolean.valueOf(IsExcludeICAWithholding));
	}

	/** Get Exclude Municipal Tax.
		@return Excluye el cálculo de Impuesto Municipal.
	  */
	public boolean isExcludeICAWithholding () 
	{
		Object oo = get_Value(COLUMNNAME_IsExcludeICAWithholding);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Exclude I.V.A. Withholding.
		@param IsExcludeIVAWithholding 
		Excluye el cálculo de Retención I.V.A.
	  */
	public void setIsExcludeIVAWithholding (boolean IsExcludeIVAWithholding)
	{
		set_Value (COLUMNNAME_IsExcludeIVAWithholding, Boolean.valueOf(IsExcludeIVAWithholding));
	}

	/** Get Exclude I.V.A. Withholding.
		@return Excluye el cálculo de Retención I.V.A.
	  */
	public boolean isExcludeIVAWithholding () 
	{
		Object oo = get_Value(COLUMNNAME_IsExcludeIVAWithholding);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Exclude Income Tax Withholding.
		@param IsExcludeRentaWithholding 
		Excluye el cálculo de Retención Impuesto Sobre La Renta.
	  */
	public void setIsExcludeRentaWithholding (boolean IsExcludeRentaWithholding)
	{
		set_Value (COLUMNNAME_IsExcludeRentaWithholding, Boolean.valueOf(IsExcludeRentaWithholding));
	}

	/** Get Exclude Income Tax Withholding.
		@return Excluye el cálculo de Retención Impuesto Sobre La Renta.
	  */
	public boolean isExcludeRentaWithholding () 
	{
		Object oo = get_Value(COLUMNNAME_IsExcludeRentaWithholding);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Municipal Withholding Agent.
		@param IsMunicipalWithholdingAgent Municipal Withholding Agent	  */
	public void setIsMunicipalWithholdingAgent (boolean IsMunicipalWithholdingAgent)
	{
		set_Value (COLUMNNAME_IsMunicipalWithholdingAgent, Boolean.valueOf(IsMunicipalWithholdingAgent));
	}

	/** Get Municipal Withholding Agent.
		@return Municipal Withholding Agent	  */
	public boolean isMunicipalWithholdingAgent () 
	{
		Object oo = get_Value(COLUMNNAME_IsMunicipalWithholdingAgent);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Withholding Setup.
		@param LCO_WithholdingSetup_ID Withholding Setup	  */
	public void setLCO_WithholdingSetup_ID (int LCO_WithholdingSetup_ID)
	{
		if (LCO_WithholdingSetup_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LCO_WithholdingSetup_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LCO_WithholdingSetup_ID, Integer.valueOf(LCO_WithholdingSetup_ID));
	}

	/** Get Withholding Setup.
		@return Withholding Setup	  */
	public int getLCO_WithholdingSetup_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_WithholdingSetup_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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