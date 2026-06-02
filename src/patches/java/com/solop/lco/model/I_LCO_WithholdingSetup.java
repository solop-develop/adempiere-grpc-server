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
package com.solop.lco.model;

import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Generated Interface for LCO_WithholdingSetup
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_LCO_WithholdingSetup 
{

    /** TableName=LCO_WithholdingSetup */
    public static final String Table_Name = "LCO_WithholdingSetup";

    /** AD_Table_ID=2000187 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name C_ICACharge_ID */
    public static final String COLUMNNAME_C_ICACharge_ID = "C_ICACharge_ID";

	/** Set ICA Withholding Charge.
	  * Cargo contable para nota de ReteICA.
	  */
	public void setC_ICACharge_ID (int C_ICACharge_ID);

	/** Get ICA Withholding Charge.
	  * Cargo contable para nota de ReteICA.
	  */
	public int getC_ICACharge_ID();

	public org.adempiere.core.domains.models.I_C_Charge getC_ICACharge() throws RuntimeException;

    /** Column name C_IVACharge_ID */
    public static final String COLUMNNAME_C_IVACharge_ID = "C_IVACharge_ID";

	/** Set IVA Withholding Charge.
	  * Cargo contable para nota de ReteIVA.
	  */
	public void setC_IVACharge_ID (int C_IVACharge_ID);

	/** Get IVA Withholding Charge.
	  * Cargo contable para nota de ReteIVA.
	  */
	public int getC_IVACharge_ID();

	public org.adempiere.core.domains.models.I_C_Charge getC_IVACharge() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name C_RentaCharge_ID */
    public static final String COLUMNNAME_C_RentaCharge_ID = "C_RentaCharge_ID";

	/** Set Income Withholding Charge.
	  * Cargo contable para nota de Retefuente.
	  */
	public void setC_RentaCharge_ID (int C_RentaCharge_ID);

	/** Get Income Withholding Charge.
	  * Cargo contable para nota de Retefuente.
	  */
	public int getC_RentaCharge_ID();

	public org.adempiere.core.domains.models.I_C_Charge getC_RentaCharge() throws RuntimeException;

    /** Column name DefaultICAConcept_ID */
    public static final String COLUMNNAME_DefaultICAConcept_ID = "DefaultICAConcept_ID";

	/** Set Default ICA Concept.
	  * Concepto de ICA por defecto.
	  */
	public void setDefaultICAConcept_ID (int DefaultICAConcept_ID);

	/** Get Default ICA Concept.
	  * Concepto de ICA por defecto.
	  */
	public int getDefaultICAConcept_ID();

	public com.solop.lco.model.I_LCO_WH_Concept getDefaultICAConcept() throws RuntimeException;

    /** Column name DefaultIVARate_ID */
    public static final String COLUMNNAME_DefaultIVARate_ID = "DefaultIVARate_ID";

	/** Set Default IVA Withholding Rate.
	  * Tarifa de ReteIVA por defecto.
	  */
	public void setDefaultIVARate_ID (int DefaultIVARate_ID);

	/** Get Default IVA Withholding Rate.
	  * Tarifa de ReteIVA por defecto.
	  */
	public int getDefaultIVARate_ID();

	public com.solop.lco.model.I_LCO_WH_IVARate getDefaultIVARate() throws RuntimeException;

    /** Column name DefaultRentaConcept_ID */
    public static final String COLUMNNAME_DefaultRentaConcept_ID = "DefaultRentaConcept_ID";

	/** Set Default Income Withholding Concept.
	  * Concepto de renta por defecto.
	  */
	public void setDefaultRentaConcept_ID (int DefaultRentaConcept_ID);

	/** Get Default Income Withholding Concept.
	  * Concepto de renta por defecto.
	  */
	public int getDefaultRentaConcept_ID();

	public com.solop.lco.model.I_LCO_WH_Concept getDefaultRentaConcept() throws RuntimeException;

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsClientExcluded */
    public static final String COLUMNNAME_IsClientExcluded = "IsClientExcluded";

	/** Set Client Excluded.
	  * La organizacion (como cliente) esta excluida de que le retengan.
	  */
	public void setIsClientExcluded (boolean IsClientExcluded);

	/** Get Client Excluded.
	  * La organizacion (como cliente) esta excluida de que le retengan.
	  */
	public boolean isClientExcluded();

    /** Column name IsExcludeICAWithholding */
    public static final String COLUMNNAME_IsExcludeICAWithholding = "IsExcludeICAWithholding";

	/** Set Exclude Municipal Tax.
	  * Excluye el cálculo de Impuesto Municipal.
	  */
	public void setIsExcludeICAWithholding (boolean IsExcludeICAWithholding);

	/** Get Exclude Municipal Tax.
	  * Excluye el cálculo de Impuesto Municipal.
	  */
	public boolean isExcludeICAWithholding();

    /** Column name IsExcludeIVAWithholding */
    public static final String COLUMNNAME_IsExcludeIVAWithholding = "IsExcludeIVAWithholding";

	/** Set Exclude I.V.A. Withholding.
	  * Excluye el cálculo de Retención I.V.A.
	  */
	public void setIsExcludeIVAWithholding (boolean IsExcludeIVAWithholding);

	/** Get Exclude I.V.A. Withholding.
	  * Excluye el cálculo de Retención I.V.A.
	  */
	public boolean isExcludeIVAWithholding();

    /** Column name IsExcludeRentaWithholding */
    public static final String COLUMNNAME_IsExcludeRentaWithholding = "IsExcludeRentaWithholding";

	/** Set Exclude Income Tax Withholding.
	  * Excluye el cálculo de Retención Impuesto Sobre La Renta.
	  */
	public void setIsExcludeRentaWithholding (boolean IsExcludeRentaWithholding);

	/** Get Exclude Income Tax Withholding.
	  * Excluye el cálculo de Retención Impuesto Sobre La Renta.
	  */
	public boolean isExcludeRentaWithholding();

    /** Column name IsMunicipalWithholdingAgent */
    public static final String COLUMNNAME_IsMunicipalWithholdingAgent = "IsMunicipalWithholdingAgent";

	/** Set Municipal Withholding Agent	  */
	public void setIsMunicipalWithholdingAgent (boolean IsMunicipalWithholdingAgent);

	/** Get Municipal Withholding Agent	  */
	public boolean isMunicipalWithholdingAgent();

    /** Column name LCO_WithholdingSetup_ID */
    public static final String COLUMNNAME_LCO_WithholdingSetup_ID = "LCO_WithholdingSetup_ID";

	/** Set Withholding Setup	  */
	public void setLCO_WithholdingSetup_ID (int LCO_WithholdingSetup_ID);

	/** Get Withholding Setup	  */
	public int getLCO_WithholdingSetup_ID();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name UUID */
    public static final String COLUMNNAME_UUID = "UUID";

	/** Set Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID);

	/** Get Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public String getUUID();
}
