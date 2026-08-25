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

/** Generated Interface for LCO_WH_IVARate
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4
 */
public interface I_LCO_WH_IVARate 
{

    /** TableName=LCO_WH_IVARate */
    public static final String Table_Name = "LCO_WH_IVARate";

    /** AD_Table_ID=2000184 */
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

    /** Column name LCO_WH_IVARate_ID */
    public static final String COLUMNNAME_LCO_WH_IVARate_ID = "LCO_WH_IVARate_ID";

	/** Set I.V.A. Withholding Rate	  */
	public void setLCO_WH_IVARate_ID (int LCO_WH_IVARate_ID);

	/** Get I.V.A. Withholding Rate	  */
	public int getLCO_WH_IVARate_ID();

    /** Column name MinUVTProduct */
    public static final String COLUMNNAME_MinUVTProduct = "MinUVTProduct";

	/** Set Minimum U.V.T. for Goods.
	  * Mínimo de U.V.T. del I.V.A. para retener en bienes/productos.
	  */
	public void setMinUVTProduct (BigDecimal MinUVTProduct);

	/** Get Minimum U.V.T. for Goods.
	  * Mínimo de U.V.T. del I.V.A. para retener en bienes/productos.
	  */
	public BigDecimal getMinUVTProduct();

    /** Column name MinUVTService */
    public static final String COLUMNNAME_MinUVTService = "MinUVTService";

	/** Set Minimum U.V.T. for Services.
	  * Mínimo de U.V.T. del I.V.A. para retener en servicios.
	  */
	public void setMinUVTService (BigDecimal MinUVTService);

	/** Get Minimum U.V.T. for Services.
	  * Mínimo de U.V.T. del I.V.A. para retener en servicios.
	  */
	public BigDecimal getMinUVTService();

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
