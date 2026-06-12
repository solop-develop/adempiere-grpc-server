/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package com.solop.sp034.util;

/**
 * Add here all changes for core and static methods
 * Please rename this class and package
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">...</a>
 */
public interface Changes {
	/**	Item Condition	*/
	String SP034_ML_ItemCondition = "SP034_ML_ItemCondition";
	/**	Publish Type	*/
	String SP034_ML_PublishType = "SP034_ML_PublishType";
	/**	Publish Status	*/
	String SP034_ML_PublishStatus = "SP034_ML_PublishStatus";
	/*	Attribute Allocation Table	*/
	String Table_SP034_Allocation = "SP034_Allocation";
	/*	Category Attribute Table	*/
	String Table_SP034_Attribute = "SP034_Attribute";
	/*	Category Attribute LIst Table	*/
	String Table_SP034_AttributeList = "SP034_AttributeList";
	/*	Category Definition (Store) Table	*/
	String Table_SP034_Category = "SP034_Category";
	/*	Product Category (Store) Table	*/
	String Table_SP034_ProductCategory = "SP034_ProductCategory";
	/*	Category Downloaded Table	*/
	String Table_T_SP034_Category = "T_SP034_Category";
	/*	Store Publishing Table	*/
	String Table_SP034_Publishing = "SP034_Publishing";
	/**	Publish Status	*/
	String SP034_PublishStatus = "SP034_PublishStatus";
	/**	Validation Message	*/
	String SP034_ValidationMsg = "SP034_ValidationMsg";
	/**	Publish Status (Active)	*/
	String SP034_PublishStatus_Active = "A";
	/**	Publish Status (Closed)	*/
	String SP034_PublishStatus_Closed = "C";
	/**	Publish Status (Without Publishing)	*/
	String SP034_PublishStatus_Without_Publishing = "D";
	/**	Publish Status (Paused)	*/
	String SP034_PublishStatus_Paused = "O";
	/**	Publish Status (Publishing)	*/
	String SP034_PublishStatus_Publishing = "P";
	/**	Value Type */
	String SP034_ValueType = "SP034_ValueType";
	String SP034_ValueType_Boolean = "B";
	String SP034_ValueType_Date = "D";
	String SP034_ValueType_List = "L";
	String SP034_ValueType_Number = "N";
	String SP034_ValueType_String = "S";
	String SP034_ValueType_Text_Long = "T";
	/**	Category URL */
	String SP034_CategoryUrl = "SP034_CategoryUrl";
	/**	Attributes URL */
	String SP034_AttributesUrl = "SP034_AttributesUrl";
	/**	Publish Title (Product) */
	String SP034_PublishTitle = "SP034_PublishTitle";
	/**	Short Description (Product) */
	String SP034_DescriptionShort = "SP034_DescriptionShort";
	/**	Long Description (Product) */
	String SP034_DescriptionLong = "SP034_DescriptionLong";
	/**	Publish Price List (tax included, original/strikethrough price) */
	String SP034_PublishPriceList = "SP034_PublishPriceList";
	/**	Publish Price Std (tax included, current/sale price) */
	String SP034_PublishPriceStd = "SP034_PublishPriceStd";
	/**	Publish Discount Amount (informative): PublishPriceList - PublishPriceStd */
	String SP034_PublishDiscountAmt = "SP034_PublishDiscountAmt";
	/**	Publish Discount Percentage (informative) */
	String SP034_PublishDiscount = "SP034_PublishDiscount";
	/**	Current Inventory (Publishing): stock at the moment of the last update */
	String SP034_CurrentInventory = "SP034_CurrentInventory";
	/**	Initial Inventory (Publishing): stock captured the first time the product is published */
	String SP034_InitialInventory = "SP034_InitialInventory";
	/**	Drop Ship Warehouse: warehouse the published inventory was taken from */
	String DropShip_Warehouse_ID = "DropShip_Warehouse_ID";
	/**	Publication Code: Publication Code */
	String SP034_Publication_Code = "SP034_Publication_Code";

}
