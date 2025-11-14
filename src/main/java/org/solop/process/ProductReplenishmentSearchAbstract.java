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

package org.solop.process;

import org.compiere.process.SvrProcess;

import java.sql.Timestamp;

/** Generated Process for (Product Replenishment Search)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class ProductReplenishmentSearchAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "ProductReplenishmentSearch";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Product Replenishment Search";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54794;
	/**	Parameter Name for Organization	*/
	public static final String AD_ORG_ID = "AD_Org_ID";
	/**	Parameter Name for Warehouse	*/
	public static final String M_WAREHOUSE_ID = "M_Warehouse_ID";
	/**	Parameter Name for Replenish Type	*/
	public static final String REPLENISHTYPE = "ReplenishType";
	/**	Parameter Name for Product	*/
	public static final String M_PRODUCT_ID = "M_Product_ID";
	/**	Parameter Name for Business Partner 	*/
	public static final String C_BPARTNER_ID = "C_BPartner_ID";
	/**	Parameter Name for Source Warehouse	*/
	public static final String M_WAREHOUSESOURCE_ID = "M_WarehouseSource_ID";
	/**	Parameter Name for Product Brand	*/
	public static final String M_BRAND_ID = "M_Brand_ID";
	/**	Parameter Name for Industry Sector	*/
	public static final String M_INDUSTRY_SECTOR_ID = "M_Industry_Sector_ID";
	/**	Parameter Name for Material Group	*/
	public static final String M_MATERIAL_GROUP_ID = "M_Material_Group_ID";
	/**	Parameter Name for Material Type	*/
	public static final String M_MATERIAL_TYPE_ID = "M_Material_Type_ID";
	/**	Parameter Name for Part Type	*/
	public static final String M_PARTTYPE_ID = "M_PartType_ID";
	/**	Parameter Name for Product Category	*/
	public static final String M_PRODUCT_CATEGORY_ID = "M_Product_Category_ID";
	/**	Parameter Name for Product Class	*/
	public static final String M_PRODUCT_CLASS_ID = "M_Product_Class_ID";
	/**	Parameter Name for Product Classification	*/
	public static final String M_PRODUCT_CLASSIFICATION_ID = "M_Product_Classification_ID";
	/**	Parameter Name for Product Group	*/
	public static final String M_PRODUCT_GROUP_ID = "M_Product_Group_ID";
	/**	Parameter Name for Purchase Group	*/
	public static final String M_PURCHASE_GROUP_ID = "M_Purchase_Group_ID";
	/**	Parameter Name for Sales Group	*/
	public static final String M_SALES_GROUP_ID = "M_Sales_Group_ID";
	/**	Parameter Name for Transaction Date	*/
	public static final String DATETRX = "DateTrx";
	/**	Parameter Value for Organization	*/
	private int orgId;
	/**	Parameter Value for Warehouse	*/
	private int warehouseId;
	/**	Parameter Value for Replenish Type	*/
	private String replenishType;
	/**	Parameter Value for Product	*/
	private int productId;
	/**	Parameter Value for Business Partner 	*/
	private int bPartnerId;
	/**	Parameter Value for Source Warehouse	*/
	private int warehouseSourceId;
	/**	Parameter Value for Product Brand	*/
	private int brandId;
	/**	Parameter Value for Industry Sector	*/
	private int industrySectorId;
	/**	Parameter Value for Material Group	*/
	private int materialGroupId;
	/**	Parameter Value for Material Type	*/
	private int materialTypeId;
	/**	Parameter Value for Part Type	*/
	private int partTypeId;
	/**	Parameter Value for Product Category	*/
	private int productCategoryId;
	/**	Parameter Value for Product Class	*/
	private int productClassId;
	/**	Parameter Value for Product Classification	*/
	private int productClassificationId;
	/**	Parameter Value for Product Group	*/
	private int productGroupId;
	/**	Parameter Value for Purchase Group	*/
	private int purchaseGroupId;
	/**	Parameter Value for Sales Group	*/
	private int salesGroupId;
	/**	Parameter Value for Transaction Date	*/
	private Timestamp dateTrx;
	/**	Parameter Value for Transaction Date(To)	*/
	private Timestamp dateTrxTo;

	@Override
	protected void prepare() {
		orgId = getParameterAsInt(AD_ORG_ID);
		warehouseId = getParameterAsInt(M_WAREHOUSE_ID);
		replenishType = getParameterAsString(REPLENISHTYPE);
		productId = getParameterAsInt(M_PRODUCT_ID);
		bPartnerId = getParameterAsInt(C_BPARTNER_ID);
		warehouseSourceId = getParameterAsInt(M_WAREHOUSESOURCE_ID);
		brandId = getParameterAsInt(M_BRAND_ID);
		industrySectorId = getParameterAsInt(M_INDUSTRY_SECTOR_ID);
		materialGroupId = getParameterAsInt(M_MATERIAL_GROUP_ID);
		materialTypeId = getParameterAsInt(M_MATERIAL_TYPE_ID);
		partTypeId = getParameterAsInt(M_PARTTYPE_ID);
		productCategoryId = getParameterAsInt(M_PRODUCT_CATEGORY_ID);
		productClassId = getParameterAsInt(M_PRODUCT_CLASS_ID);
		productClassificationId = getParameterAsInt(M_PRODUCT_CLASSIFICATION_ID);
		productGroupId = getParameterAsInt(M_PRODUCT_GROUP_ID);
		purchaseGroupId = getParameterAsInt(M_PURCHASE_GROUP_ID);
		salesGroupId = getParameterAsInt(M_SALES_GROUP_ID);
		dateTrx = getParameterAsTimestamp(DATETRX);
		dateTrxTo = getParameterToAsTimestamp(DATETRX);
	}

	/**	 Getter Parameter Value for Organization	*/
	protected int getOrgId() {
		return orgId;
	}

	/**	 Setter Parameter Value for Organization	*/
	protected void setOrgId(int orgId) {
		this.orgId = orgId;
	}

	/**	 Getter Parameter Value for Warehouse	*/
	protected int getWarehouseId() {
		return warehouseId;
	}

	/**	 Setter Parameter Value for Warehouse	*/
	protected void setWarehouseId(int warehouseId) {
		this.warehouseId = warehouseId;
	}

	/**	 Getter Parameter Value for Replenish Type	*/
	protected String getReplenishType() {
		return replenishType;
	}

	/**	 Setter Parameter Value for Replenish Type	*/
	protected void setReplenishType(String replenishType) {
		this.replenishType = replenishType;
	}

	/**	 Getter Parameter Value for Product	*/
	protected int getProductId() {
		return productId;
	}

	/**	 Setter Parameter Value for Product	*/
	protected void setProductId(int productId) {
		this.productId = productId;
	}

	/**	 Getter Parameter Value for Business Partner 	*/
	protected int getBPartnerId() {
		return bPartnerId;
	}

	/**	 Setter Parameter Value for Business Partner 	*/
	protected void setBPartnerId(int bPartnerId) {
		this.bPartnerId = bPartnerId;
	}

	/**	 Getter Parameter Value for Source Warehouse	*/
	protected int getWarehouseSourceId() {
		return warehouseSourceId;
	}

	/**	 Setter Parameter Value for Source Warehouse	*/
	protected void setWarehouseSourceId(int warehouseSourceId) {
		this.warehouseSourceId = warehouseSourceId;
	}

	/**	 Getter Parameter Value for Product Brand	*/
	protected int getBrandId() {
		return brandId;
	}

	/**	 Setter Parameter Value for Product Brand	*/
	protected void setBrandId(int brandId) {
		this.brandId = brandId;
	}

	/**	 Getter Parameter Value for Industry Sector	*/
	protected int getIndustrySectorId() {
		return industrySectorId;
	}

	/**	 Setter Parameter Value for Industry Sector	*/
	protected void setIndustrySectorId(int industrySectorId) {
		this.industrySectorId = industrySectorId;
	}

	/**	 Getter Parameter Value for Material Group	*/
	protected int getMaterialGroupId() {
		return materialGroupId;
	}

	/**	 Setter Parameter Value for Material Group	*/
	protected void setMaterialGroupId(int materialGroupId) {
		this.materialGroupId = materialGroupId;
	}

	/**	 Getter Parameter Value for Material Type	*/
	protected int getMaterialTypeId() {
		return materialTypeId;
	}

	/**	 Setter Parameter Value for Material Type	*/
	protected void setMaterialTypeId(int materialTypeId) {
		this.materialTypeId = materialTypeId;
	}

	/**	 Getter Parameter Value for Part Type	*/
	protected int getPartTypeId() {
		return partTypeId;
	}

	/**	 Setter Parameter Value for Part Type	*/
	protected void setPartTypeId(int partTypeId) {
		this.partTypeId = partTypeId;
	}

	/**	 Getter Parameter Value for Product Category	*/
	protected int getProductCategoryId() {
		return productCategoryId;
	}

	/**	 Setter Parameter Value for Product Category	*/
	protected void setProductCategoryId(int productCategoryId) {
		this.productCategoryId = productCategoryId;
	}

	/**	 Getter Parameter Value for Product Class	*/
	protected int getProductClassId() {
		return productClassId;
	}

	/**	 Setter Parameter Value for Product Class	*/
	protected void setProductClassId(int productClassId) {
		this.productClassId = productClassId;
	}

	/**	 Getter Parameter Value for Product Classification	*/
	protected int getProductClassificationId() {
		return productClassificationId;
	}

	/**	 Setter Parameter Value for Product Classification	*/
	protected void setProductClassificationId(int productClassificationId) {
		this.productClassificationId = productClassificationId;
	}

	/**	 Getter Parameter Value for Product Group	*/
	protected int getProductGroupId() {
		return productGroupId;
	}

	/**	 Setter Parameter Value for Product Group	*/
	protected void setProductGroupId(int productGroupId) {
		this.productGroupId = productGroupId;
	}

	/**	 Getter Parameter Value for Purchase Group	*/
	protected int getPurchaseGroupId() {
		return purchaseGroupId;
	}

	/**	 Setter Parameter Value for Purchase Group	*/
	protected void setPurchaseGroupId(int purchaseGroupId) {
		this.purchaseGroupId = purchaseGroupId;
	}

	/**	 Getter Parameter Value for Sales Group	*/
	protected int getSalesGroupId() {
		return salesGroupId;
	}

	/**	 Setter Parameter Value for Sales Group	*/
	protected void setSalesGroupId(int salesGroupId) {
		this.salesGroupId = salesGroupId;
	}

	/**	 Getter Parameter Value for Transaction Date	*/
	protected Timestamp getDateTrx() {
		return dateTrx;
	}

	/**	 Setter Parameter Value for Transaction Date	*/
	protected void setDateTrx(Timestamp dateTrx) {
		this.dateTrx = dateTrx;
	}

	/**	 Getter Parameter Value for Transaction Date(To)	*/
	protected Timestamp getDateTrxTo() {
		return dateTrxTo;
	}

	/**	 Setter Parameter Value for Transaction Date(To)	*/
	protected void setDateTrxTo(Timestamp dateTrxTo) {
		this.dateTrxTo = dateTrxTo;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}