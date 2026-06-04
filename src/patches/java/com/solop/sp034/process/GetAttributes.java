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
import org.compiere.util.CLogger;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Get Attributes)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class GetAttributes extends GetAttributesAbstract {

	private static final CLogger log = CLogger.getCLogger(GetAttributes.class);
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
		return new Query(
				getCtx(),
				Changes.Table_SP034_Category,
				"Value = ?",
				null
			)
			.setParameters(value)
			.setOnlyActiveRecords(true)
			.first()
		;
	}

	private PO getAttributeCategory(String value, int categoryId) {
		return new Query(
				getCtx(),
				Changes.Table_SP034_Attribute,
				"Value = ? AND SP034_Category_ID = ?",
				null
			)
			.setParameters(value, categoryId)
			.setOnlyActiveRecords(true)
			.first()
		;
	}

	private void deleteOldAttributeAllocations(int productId) {
		MTable allocationTable = MTable.get(getCtx(), Changes.Table_SP034_Allocation);
		new Query(
				getCtx(),
				Changes.Table_SP034_Allocation,
				"M_Product_ID = ?",
				null
			)
			.setParameters(productId)
			.getIDsAsList()
			.forEach(id -> {
				PO allocationEntity = allocationTable.getPO(id, get_TrxName());
				allocationEntity.deleteEx(true);
			})
		;
	}

	private void deleteAttributeListValues(int attributeId) {
		MTable listTable = MTable.get(getCtx(), Changes.Table_SP034_AttributeList);
		new Query(
				getCtx(),
				Changes.Table_SP034_AttributeList,
				"SP034_Attribute_ID = ?",
				null
			)
			.setParameters(attributeId)
			.getIDsAsList()
			.forEach(id -> {
				PO attributeEntity = listTable.getPO(id, get_TrxName());
				attributeEntity.deleteEx(true);
			})
		;
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
			deleteOldAttributeAllocations(productId);
			createAllocation(productId, category.get_ID());
		}

		int categoryId = category.get_ID();
		MTable attributeTable = MTable.get(getCtx(), Changes.Table_SP034_Attribute);
		Trx trx = get_TrxName() != null ? Trx.get(get_TrxName(), false) : null;
		log.info("Importing " + attributes.size() + " attributes for category '" + value + "' (id=" + categoryId + ")");
		for(int index = 0; index < attributes.size(); index++) {
			AttributeResponse attribute = attributes.get(index);
			// Skip malformed entries: without a code there is no Value to persist
			if(Util.isEmpty(attribute.code, true)) {
				errors.incrementAndGet();
				log.warning("Skipping attribute without code at position " + index + ": " + attribute);
				continue;
			}
			log.info("Processing attribute [" + index + "] '" + attribute.code + "' (value_type=" + attribute.value_type + ", values=" + (attribute.values != null ? attribute.values.size() : 0) + ")");
			// Isolate each attribute in its own savepoint so a single failure does not
			// poison the shared transaction (PostgreSQL aborts the whole transaction on
			// the first failed statement, which would otherwise drop every later attribute)
			Savepoint savepoint = null;
			try {
				if(trx != null) {
					savepoint = trx.setSavepoint(null);
				}
				boolean isNew;
				PO attributeEntity = getAttributeCategory(attribute.code, categoryId);
				if(attributeEntity == null) {
					isNew = true;
					attributeEntity = attributeTable.getPO(0, get_TrxName());
					attributeEntity.setAD_Org_ID(0);
					attributeEntity.set_ValueOfColumn("SP034_Category_ID", categoryId);
				} else {
					isNew = false;
				}
				// Always update definition fields (create or refresh)
				attributeEntity.set_ValueOfColumn("Value", attribute.code);
				attributeEntity.set_ValueOfColumn("Name", attribute.name);
				attributeEntity.set_ValueOfColumn("Description", attribute.description);
				attributeEntity.set_ValueOfColumn("IsMandatory", Boolean.TRUE.equals(attribute.mandatory));
				//	Value Type
				if(attribute.values != null && !attribute.values.isEmpty()) {
					attributeEntity.set_ValueOfColumn(Changes.SP034_ValueType, Changes.SP034_ValueType_List);
				} else {
					// value_type can be null when MercadoLibre/n8n omits it; default to String
					String valueType = attribute.value_type != null ? attribute.value_type : "";
					switch (valueType) {
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
				}
				attributeEntity.saveEx();
				int attributeEntityId = attributeEntity.get_ID();
				// On update, refresh list values; on create they are simply added
				if(!isNew) {
					deleteAttributeListValues(attributeEntityId);
				}
				if(attribute.values != null && !attribute.values.isEmpty()) {
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
				created.incrementAndGet();
			} catch (Exception e) {
				errors.incrementAndGet();
				log.warning("Error importing attribute '" + attribute.code + "': " + e.getLocalizedMessage());
				if(trx != null && savepoint != null) {
					try {
						trx.rollback(savepoint);
					} catch (SQLException rollbackError) {
						log.severe("Could not rollback savepoint for attribute '" + attribute.code + "': " + rollbackError.getLocalizedMessage());
					}
				}
			}
		}
		log.info("Finished importing attributes for category '" + value + "': created=" + created.get() + ", errors=" + errors.get());
	}

}
