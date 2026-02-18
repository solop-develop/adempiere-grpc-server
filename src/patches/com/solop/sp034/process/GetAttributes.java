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

package com.solop.sp034.process;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solop.sp034.util.Changes;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_W_Store;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MStore;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Get Attributes)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class GetAttributes extends GetAttributesAbstract {

	private final AtomicInteger created = new AtomicInteger();
	private final AtomicInteger errors = new AtomicInteger();

	@Override
	protected String doIt() throws Exception {
		if(getSelectionKeys() != null) {
			if (getSelectionKeys().size() > 1) {
				throw new AdempiereException("@SP034_SelectOnlyOneRecord@ (" + getSelectionKeys().size() + ")");
			}
			Optional<Integer> maybeCategoryId = getSelectionKeys().stream().findFirst();
			if(maybeCategoryId.isPresent()) {
				int categoryId = maybeCategoryId.get();
				MTable temporaryCategoryTable = MTable.get(getCtx(), Changes.Table_T_SP034_Category);
				PO temporaryCategory = temporaryCategoryTable.getPO(categoryId, get_TrxName());
				String value = temporaryCategory.get_ValueAsString("Value");
				String name = temporaryCategory.get_ValueAsString("Name");
				MStore store = MStore.get(getCtx(), temporaryCategory.get_ValueAsInt(I_W_Store.COLUMNNAME_W_Store_ID));
				String attributesUrl = store.get_ValueAsString(Changes.SP034_AttributesUrl);
				if(Util.isEmpty(attributesUrl, true)) {
					throw new AdempiereException("@W_Store_ID@ [@SP034_AttributesUrl@] @NotFound@");
				}
				HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(attributesUrl))
					.newBuilder()
					.addQueryParameter("category_id", value)
					.build()
				;
				Request request = new Request.Builder().url(httpUrl).get().build();
				final OkHttpClient client = new OkHttpClient();
				try (Response response = client.newCall(request).execute()) {
					if (response.isSuccessful()) {
						ObjectMapper objectMapper = new ObjectMapper();
						if(response.body() != null) {
							List<AttributeResponse> attributes = objectMapper.readValue(response.body().string(), new TypeReference<List<AttributeResponse>>() {});
							createAttributes(temporaryCategory, attributes);
						}
					} else {
						throw new AdempiereException("Error sending " + value + " - " + name + ". Response Code: " + response.code() + ", Body: " + response.body().string());
					}
				} catch (Exception e) {
					throw new AdempiereException(e);
				}
			}
		}
		return "@Created@: " + created + " @Errors@: " + errors;
	}

	private void deleteOldAllocations(int productId) {
		MTable allocationTable = MTable.get(getCtx(), Changes.Table_SP034_ProductCategory);
		new Query(
			getCtx(),
			Changes.Table_SP034_ProductCategory,
			"M_Product_ID = ?",
			null
		)
			.setParameters(productId)
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(allocationId -> {
				PO allocation = allocationTable.getPO(allocationId, get_TrxName());
				allocation.deleteEx(true);
			})
		;
	}

	private void createAllocation(int productId, int categoryId) {
		MTable allocationTable = MTable.get(getCtx(), Changes.Table_SP034_ProductCategory);
		PO allocation = allocationTable.getPO(0, get_TrxName());
		allocation.set_ValueOfColumn(I_M_Product.COLUMNNAME_M_Product_ID, productId);
		allocation.set_ValueOfColumn("SP034_Category_ID", categoryId);
		allocation.saveEx();
	}

	private PO getCategory(String value) {
		return new Query(getCtx(), Changes.Table_SP034_Category, "Value = ?", null).setParameters(value).setOnlyActiveRecords(true).first();
	}

	private PO getAttributeCategory(String value) {
		return new Query(getCtx(), Changes.Table_SP034_Attribute, "Value = ?", null).setParameters(value).setOnlyActiveRecords(true).first();
	}

	private void createAttributes(PO temporaryCategory, List<AttributeResponse> attributes) {
		String value = temporaryCategory.get_ValueAsString("Value");
		String name = temporaryCategory.get_ValueAsString("Name");
		String description = temporaryCategory.get_ValueAsString("Description");
		int storeId = temporaryCategory.get_ValueAsInt(I_W_Store.COLUMNNAME_W_Store_ID);
		int productId = temporaryCategory.get_ValueAsInt(I_M_Product.COLUMNNAME_M_Product_ID);
		PO category = getCategory(value);
		if(category == null) {
			MTable categoryTable = MTable.get(getCtx(), Changes.Table_SP034_Category);
			category = categoryTable.getPO(0, get_TrxName());
			category.setAD_Org_ID(0);
			category.set_ValueOfColumn("Value", value);
			category.set_ValueOfColumn("Name", name);
			category.set_ValueOfColumn("Description", description);
			category.set_ValueOfColumn("W_Store_ID", storeId);
			category.saveEx();
			deleteOldAllocations(productId);
			createAllocation(productId, category.get_ID());

		}
		int categoryId = category.get_ID();
		MTable attributeTable = MTable.get(getCtx(), Changes.Table_SP034_Attribute);
		attributes.forEach(attribute -> {
			PO attributeEntity = getAttributeCategory(attribute.code);
			if(attributeEntity == null) {
				attributeEntity = attributeTable.getPO(0, get_TrxName());
				attributeEntity.setAD_Org_ID(0);
				attributeEntity.set_ValueOfColumn("SP034_Category_ID", categoryId);
				attributeEntity.set_ValueOfColumn("Value", attribute.code);
				attributeEntity.set_ValueOfColumn("Name", attribute.name);
				attributeEntity.set_ValueOfColumn("Description", attribute.description);
				attributeEntity.set_ValueOfColumn("IsMandatory", attribute.mandatory);
				//	Value Type
				switch (attribute.value_type) {
					case "number":
					case "number_unit":
						attributeEntity.set_ValueOfColumn(Changes.SP034_ValueType, Changes.SP034_ValueType_Number);
						break;
					case "boolean":
						attributeEntity.set_ValueOfColumn(Changes.SP034_ValueType, Changes.SP034_ValueType_Boolean);
						break;
					default:
						attributeEntity.set_ValueOfColumn(Changes.SP034_ValueType, Changes.SP034_ValueType_String);
						break;
				}
				if(attribute.values != null && !attribute.values.isEmpty()) {
					attributeEntity.set_ValueOfColumn(Changes.SP034_ValueType, Changes.SP034_ValueType_List);
					attributeEntity.saveEx();
					int attributeEntityId = attributeEntity.get_ID();
					MTable attributeValueTable = MTable.get(getCtx(), Changes.Table_SP034_AttributeList);
					attribute.values.forEach(attributeValue -> {
						PO attributeValueEntity = attributeValueTable.getPO(0, get_TrxName());
						attributeValueEntity.setAD_Org_ID(0);
						attributeValueEntity.set_ValueOfColumn("SP034_Attribute_ID", attributeEntityId);
						attributeValueEntity.set_ValueOfColumn("Value", attributeValue.code);
						attributeValueEntity.set_ValueOfColumn("Name", attributeValue.name);
						attributeValueEntity.saveEx();
					});
				}
				attributeEntity.saveEx();
			}
			created.incrementAndGet();
		});
	}

}
