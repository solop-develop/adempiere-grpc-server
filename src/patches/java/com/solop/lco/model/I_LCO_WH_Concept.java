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

/** Generated Interface for LCO_WH_Concept
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_LCO_WH_Concept 
{

    /** TableName=LCO_WH_Concept */
    public static final String Table_Name = "LCO_WH_Concept";

    /** AD_Table_ID=2000185 */
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

    /** Column name C_Currency_ID */
    public static final String COLUMNNAME_C_Currency_ID = "C_Currency_ID";

	/** Set Currency.
	  * The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID);

	/** Get Currency.
	  * The Currency for this record
	  */
	public int getC_Currency_ID();

	public org.adempiere.core.domains.models.I_C_Currency getC_Currency() throws RuntimeException;

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

    /** Column name C_Region_ID */
    public static final String COLUMNNAME_C_Region_ID = "C_Region_ID";

	/** Set Region.
	  * Identifies a geographical Region
	  */
	public void setC_Region_ID (int C_Region_ID);

	/** Get Region.
	  * Identifies a geographical Region
	  */
	public int getC_Region_ID();

	public org.adempiere.core.domains.models.I_C_Region getC_Region() throws RuntimeException;

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

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

    /** Column name IsCumulativeWithholding */
    public static final String COLUMNNAME_IsCumulativeWithholding = "IsCumulativeWithholding";

	/** Set Cumulative Withholding.
	  * Retencion acumulativa en el periodo.
	  */
	public void setIsCumulativeWithholding (boolean IsCumulativeWithholding);

	/** Get Cumulative Withholding.
	  * Retencion acumulativa en el periodo.
	  */
	public boolean isCumulativeWithholding();

    /** Column name IsVariableRate */
    public static final String COLUMNNAME_IsVariableRate = "IsVariableRate";

	/** Set Variable Rate.
	  * Y: la tarifa sale de LCO_WH_Combination.
	  */
	public void setIsVariableRate (boolean IsVariableRate);

	/** Get Variable Rate.
	  * Y: la tarifa sale de LCO_WH_Combination.
	  */
	public boolean isVariableRate();

    /** Column name LCO_PersonType */
    public static final String COLUMNNAME_LCO_PersonType = "LCO_PersonType";

	/** Set Person Type.
	  * Tipo de persona al que aplica.
	  */
	public void setLCO_PersonType (String LCO_PersonType);

	/** Get Person Type.
	  * Tipo de persona al que aplica.
	  */
	public String getLCO_PersonType();

    /** Column name LCO_WH_Concept_ID */
    public static final String COLUMNNAME_LCO_WH_Concept_ID = "LCO_WH_Concept_ID";

	/** Set Withholding Concept	  */
	public void setLCO_WH_Concept_ID (int LCO_WH_Concept_ID);

	/** Get Withholding Concept	  */
	public int getLCO_WH_Concept_ID();

    /** Column name MinimumUVT */
    public static final String COLUMNNAME_MinimumUVT = "MinimumUVT";

	/** Set Minimum U.V.T..
	  * Cuantía mínima sujeta, en U.V.T.
	  */
	public void setMinimumUVT (BigDecimal MinimumUVT);

	/** Get Minimum U.V.T..
	  * Cuantía mínima sujeta, en U.V.T.
	  */
	public BigDecimal getMinimumUVT();

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

    /** Column name PrintedName */
    public static final String COLUMNNAME_PrintedName = "PrintedName";

	/** Set Printed Name.
	  * Nombre a imprimir en el documento de retencion.
	  */
	public void setPrintedName (String PrintedName);

	/** Get Printed Name.
	  * Nombre a imprimir en el documento de retencion.
	  */
	public String getPrintedName();

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

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();

    /** Column name WH_Type_ID */
    public static final String COLUMNNAME_WH_Type_ID = "WH_Type_ID";

	/** Set Withholding Type.
	  * Indicates the types of national tax withholdings
	  */
	public void setWH_Type_ID (int WH_Type_ID);

	/** Get Withholding Type.
	  * Indicates the types of national tax withholdings
	  */
	public int getWH_Type_ID();

	public org.adempiere.core.domains.models.I_WH_Type getWH_Type() throws RuntimeException;

    /** Column name WithholdingBaseRate */
    public static final String COLUMNNAME_WithholdingBaseRate = "WithholdingBaseRate";

	/** Set Withholding Base Rate.
	  * Porcentaje de la base sujeta a retencion (p. ej. AIU, 75%).
	  */
	public void setWithholdingBaseRate (BigDecimal WithholdingBaseRate);

	/** Get Withholding Base Rate.
	  * Porcentaje de la base sujeta a retencion (p. ej. AIU, 75%).
	  */
	public BigDecimal getWithholdingBaseRate();

    /** Column name WithholdingRate */
    public static final String COLUMNNAME_WithholdingRate = "WithholdingRate";

	/** Set Withholding Rate.
	  * Withholding Rate applied to Document
	  */
	public void setWithholdingRate (BigDecimal WithholdingRate);

	/** Get Withholding Rate.
	  * Withholding Rate applied to Document
	  */
	public BigDecimal getWithholdingRate();
}
