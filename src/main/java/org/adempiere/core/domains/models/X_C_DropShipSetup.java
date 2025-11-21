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
import org.compiere.util.KeyNamePair;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for C_DropShipSetup
 *  @author Adempiere (generated) 
 *  @version Release 3.9.4 - $Id$ */
public class X_C_DropShipSetup extends PO implements I_C_DropShipSetup, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251121L;

    /** Standard Constructor */
    public X_C_DropShipSetup(Properties ctx, int C_DropShipSetup_ID, String trxName)
    {
      super (ctx, C_DropShipSetup_ID, trxName);
      /** if (C_DropShipSetup_ID == 0)
        {
			setC_DocType_PO (0);
// -1
			setC_DropShipSetup_ID (0);
			setDropShip_BPartner_ID (0);
// -1
			setDropShip_Location_ID (0);
// -1
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_C_DropShipSetup(Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_C_DropShipSetup[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_C_DocType getC_DocType() throws RuntimeException
    {
		return (I_C_DocType)MTable.get(getCtx(), I_C_DocType.Table_Name)
			.getPO(getC_DocType_ID(), get_TrxName());	}

	/** Set Document Type.
		@param C_DocType_ID 
		Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0) 
			set_Value (COLUMNNAME_C_DocType_ID, null);
		else 
			set_Value (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Purchase Order Doc Type.
		@param C_DocType_PO Purchase Order Doc Type	  */
	public void setC_DocType_PO (int C_DocType_PO)
	{
		set_Value (COLUMNNAME_C_DocType_PO, Integer.valueOf(C_DocType_PO));
	}

	/** Get Purchase Order Doc Type.
		@return Purchase Order Doc Type	  */
	public int getC_DocType_PO () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_PO);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Drop Ship Setup.
		@param C_DropShipSetup_ID Drop Ship Setup	  */
	public void setC_DropShipSetup_ID (int C_DropShipSetup_ID)
	{
		if (C_DropShipSetup_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_C_DropShipSetup_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_DropShipSetup_ID, Integer.valueOf(C_DropShipSetup_ID));
	}

	/** Get Drop Ship Setup.
		@return Drop Ship Setup	  */
	public int getC_DropShipSetup_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DropShipSetup_ID);
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

	public I_C_BPartner getDropShip_BPartner() throws RuntimeException
    {
		return (I_C_BPartner)MTable.get(getCtx(), I_C_BPartner.Table_Name)
			.getPO(getDropShip_BPartner_ID(), get_TrxName());	}

	/** Set Drop Shipment Partner.
		@param DropShip_BPartner_ID 
		Business Partner to ship to
	  */
	public void setDropShip_BPartner_ID (int DropShip_BPartner_ID)
	{
		if (DropShip_BPartner_ID < 1) 
			set_Value (COLUMNNAME_DropShip_BPartner_ID, null);
		else 
			set_Value (COLUMNNAME_DropShip_BPartner_ID, Integer.valueOf(DropShip_BPartner_ID));
	}

	/** Get Drop Shipment Partner.
		@return Business Partner to ship to
	  */
	public int getDropShip_BPartner_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DropShip_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_C_BPartner_Location getDropShip_Location() throws RuntimeException
    {
		return (I_C_BPartner_Location)MTable.get(getCtx(), I_C_BPartner_Location.Table_Name)
			.getPO(getDropShip_Location_ID(), get_TrxName());	}

	/** Set Drop Shipment Location.
		@param DropShip_Location_ID 
		Business Partner Location for shipping to
	  */
	public void setDropShip_Location_ID (int DropShip_Location_ID)
	{
		if (DropShip_Location_ID < 1) 
			set_Value (COLUMNNAME_DropShip_Location_ID, null);
		else 
			set_Value (COLUMNNAME_DropShip_Location_ID, Integer.valueOf(DropShip_Location_ID));
	}

	/** Get Drop Shipment Location.
		@return Business Partner Location for shipping to
	  */
	public int getDropShip_Location_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DropShip_Location_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_AD_User getDropShip_User() throws RuntimeException
    {
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)
			.getPO(getDropShip_User_ID(), get_TrxName());	}

	/** Set Drop Shipment Contact.
		@param DropShip_User_ID 
		Business Partner Contact for drop shipment
	  */
	public void setDropShip_User_ID (int DropShip_User_ID)
	{
		if (DropShip_User_ID < 1) 
			set_Value (COLUMNNAME_DropShip_User_ID, null);
		else 
			set_Value (COLUMNNAME_DropShip_User_ID, Integer.valueOf(DropShip_User_ID));
	}

	/** Get Drop Shipment Contact.
		@return Business Partner Contact for drop shipment
	  */
	public int getDropShip_User_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DropShip_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_Warehouse getDropShip_Warehouse() throws RuntimeException
    {
		return (I_M_Warehouse)MTable.get(getCtx(), I_M_Warehouse.Table_Name)
			.getPO(getDropShip_Warehouse_ID(), get_TrxName());	}

	/** Set Drop Ship Warehouse.
		@param DropShip_Warehouse_ID 
		The (logical) warehouse to use for recording drop ship receipts and shipments.
	  */
	public void setDropShip_Warehouse_ID (int DropShip_Warehouse_ID)
	{
		if (DropShip_Warehouse_ID < 1) 
			set_Value (COLUMNNAME_DropShip_Warehouse_ID, null);
		else 
			set_Value (COLUMNNAME_DropShip_Warehouse_ID, Integer.valueOf(DropShip_Warehouse_ID));
	}

	/** Get Drop Ship Warehouse.
		@return The (logical) warehouse to use for recording drop ship receipts and shipments.
	  */
	public int getDropShip_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DropShip_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Create PO Automatically.
		@param IsCreatePOAutomatically Create PO Automatically	  */
	public void setIsCreatePOAutomatically (boolean IsCreatePOAutomatically)
	{
		set_Value (COLUMNNAME_IsCreatePOAutomatically, Boolean.valueOf(IsCreatePOAutomatically));
	}

	/** Get Create PO Automatically.
		@return Create PO Automatically	  */
	public boolean isCreatePOAutomatically () 
	{
		Object oo = get_Value(COLUMNNAME_IsCreatePOAutomatically);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Document Based Business Partner.
		@param IsDocumentBasedBPartner Document Based Business Partner	  */
	public void setIsDocumentBasedBPartner (boolean IsDocumentBasedBPartner)
	{
		set_Value (COLUMNNAME_IsDocumentBasedBPartner, Boolean.valueOf(IsDocumentBasedBPartner));
	}

	/** Get Document Based Business Partner.
		@return Document Based Business Partner	  */
	public boolean isDocumentBasedBPartner () 
	{
		Object oo = get_Value(COLUMNNAME_IsDocumentBasedBPartner);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
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