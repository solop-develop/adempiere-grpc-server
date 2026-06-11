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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_W_Store;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MStore;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solop.sp034.util.Changes;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Generated Process for (Get Attributes)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class GetAttributes extends GetAttributesAbstract {

	private static final CLogger log = CLogger.getCLogger(GetAttributes.class);
	private final AtomicInteger created = new AtomicInteger();
	private final AtomicInteger errors = new AtomicInteger();

	// HTTP timeouts (seconds) for the attributes webhook call. The n8n flow queries MercadoLibre
	// (categories, attributes and top_values) and regularly takes well over OkHttp's 10s default,
	// causing SocketTimeoutException. Configurable per tenant via MSysConfig; constants are the
	// defaults used when the SysConfig entry is absent.
	private static final String SYSCONFIG_CONNECT_TIMEOUT = "SP034_ATTRIBUTES_CONNECT_TIMEOUT";
	private static final String SYSCONFIG_READ_TIMEOUT = "SP034_ATTRIBUTES_READ_TIMEOUT";
	private static final String SYSCONFIG_WRITE_TIMEOUT = "SP034_ATTRIBUTES_WRITE_TIMEOUT";
	private static final String SYSCONFIG_CALL_TIMEOUT = "SP034_ATTRIBUTES_CALL_TIMEOUT";
	private static final int DEFAULT_CONNECT_TIMEOUT = 30;
	private static final int DEFAULT_READ_TIMEOUT = 120;
	private static final int DEFAULT_WRITE_TIMEOUT = 120;
	private static final int DEFAULT_CALL_TIMEOUT = 150;

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

				int clientId = getAD_Client_ID();
				int connectTimeout = MSysConfig.getIntValue(SYSCONFIG_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT, clientId);
				int readTimeout = MSysConfig.getIntValue(SYSCONFIG_READ_TIMEOUT, DEFAULT_READ_TIMEOUT, clientId);
				int writeTimeout = MSysConfig.getIntValue(SYSCONFIG_WRITE_TIMEOUT, DEFAULT_WRITE_TIMEOUT, clientId);
				int callTimeout = MSysConfig.getIntValue(SYSCONFIG_CALL_TIMEOUT, DEFAULT_CALL_TIMEOUT, clientId);
				final OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(connectTimeout, TimeUnit.SECONDS)
					.readTimeout(readTimeout, TimeUnit.SECONDS)
					.writeTimeout(writeTimeout, TimeUnit.SECONDS)
					.callTimeout(callTimeout, TimeUnit.SECONDS)
					.build()
				;

				HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(attributesUrl))
					.newBuilder()
					.addQueryParameter("category_id", value)
					.build()
				;
				Request request = new Request.Builder().url(httpUrl).get().build();
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

	private boolean isAllocatedToCategory(int productId, int categoryId) {
		return new Query(
				getCtx(),
				Changes.Table_SP034_ProductCategory,
				"M_Product_ID = ? AND SP034_Category_ID = ?",
				null
			)
			.setParameters(productId, categoryId)
			.setOnlyActiveRecords(true)
			.match()
		;
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
		// Load all rows in a single query (the original did one extra getPO per id) and delete
		// each via PO so model validators and change logging still fire.
		new Query(
			getCtx(),
			Changes.Table_SP034_AttributeList,
			"SP034_Attribute_ID = ?",
			get_TrxName()
		)
			.setParameters(attributeId)
			.list()
			.forEach(attributeEntity -> ((PO) attributeEntity).deleteEx(true))
		;
	}

	private void createAttributes(PO temporaryCategory, List<AttributeResponse> attributes) {
		String value = temporaryCategory.get_ValueAsString("Value");
		String name = temporaryCategory.get_ValueAsString("Name");
		String description = temporaryCategory.get_ValueAsString("Description");
		int storeId = temporaryCategory.get_ValueAsInt(I_W_Store.COLUMNNAME_W_Store_ID);
		int productId = temporaryCategory.get_ValueAsInt(I_M_Product.COLUMNNAME_M_Product_ID);
		PO category = getCategory(value);
		// Many MercadoLibre categories share the same name (e.g. several "Pelotas" under
		// different domains), so the raw name alone is ambiguous in the lookup and in the
		// product-category allocation. Build a distinguishable "<name> — <domain>" label.
		String displayName = Util.isEmpty(description, true) ? name : name + " — " + description;
		if(category == null) {
			MTable categoryTable = MTable.get(getCtx(), Changes.Table_SP034_Category);
			category = categoryTable.getPO(0, get_TrxName());
			category.setAD_Org_ID(0);
			category.set_ValueOfColumn("Value", value);
			category.set_ValueOfColumn("W_Store_ID", storeId);
		}
		// Always refresh the display fields so re-importing reflects the latest prediction.
		category.set_ValueOfColumn("Name", displayName);
		category.set_ValueOfColumn("Description", description);
		category.saveEx();

		int categoryId = category.get_ID();
		// Link this product to the selected category. Previously this only ran when the
		// category was created for the first time globally, so a product that selected an
		// already-imported category was never allocated -> no attributes to set in the UI.
		// When the product changes category we must clear its old values, but we first
		// snapshot them by attribute code so equivalent attributes in the new category keep
		// the user's value (issue #2845 follow-up: avoid re-typing values on category change).
		Map<String, List<AllocationSnapshot>> preservedValues = new HashMap<>();
		boolean categoryChanged = !isAllocatedToCategory(productId, categoryId);
		if(categoryChanged) {
			preservedValues = captureAllocationValues(productId);
			deleteOldAllocations(productId);
			deleteOldAttributeAllocations(productId);
			createAllocation(productId, categoryId);
		}
		MTable attributeTable = MTable.get(getCtx(), Changes.Table_SP034_Attribute);
		MTable attributeValueTable = MTable.get(getCtx(), Changes.Table_SP034_AttributeList);
		Trx trx = get_TrxName() != null ? Trx.get(get_TrxName(), false) : null;
		// Pre-load the category's existing attributes in a single query (avoids one SELECT
		// per attribute to check existence).
		Map<String, PO> existingByCode = new HashMap<>();
		new Query(
			getCtx(),
			Changes.Table_SP034_Attribute,
			"SP034_Category_ID = ?",
			get_TrxName()
		)
			.setParameters(categoryId)
			.setOnlyActiveRecords(true)
			.list()
			.forEach(po -> {
				existingByCode.put(((PO) po).get_ValueAsString("Value"), (PO) po);
			})
		;
		log.info("Importing " + attributes.size() + " attributes for category '" + value + "' (id=" + categoryId + ")");
		for(int index = 0; index < attributes.size(); index++) {
			AttributeResponse attribute = attributes.get(index);
			// Skip malformed entries: without a code there is no Value to persist
			if(Util.isEmpty(attribute.code, true)) {
				errors.incrementAndGet();
				log.warning("Skipping attribute without code at position " + index + ": " + attribute);
				continue;
			}
			log.fine("Processing attribute [" + index + "] '" + attribute.code + "' (value_type=" + attribute.value_type + ", values=" + (attribute.values != null ? attribute.values.size() : 0) + ")");
			// Isolate each attribute in its own savepoint so a single failure does not
			// poison the shared transaction (PostgreSQL aborts the whole transaction on
			// the first failed statement, which would otherwise drop every later attribute)
			Savepoint savepoint = null;
			try {
				if(trx != null) {
					savepoint = trx.setSavepoint(null);
				}
				boolean isNew;
				PO attributeEntity = existingByCode.get(attribute.code);
				if(attributeEntity == null) {
					isNew = true;
					attributeEntity = attributeTable.getPO(0, get_TrxName());
					attributeEntity.setAD_Org_ID(0);
					attributeEntity.set_ValueOfColumn("SP034_Category_ID", categoryId);
					existingByCode.put(attribute.code, attributeEntity);
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
		// Re-apply the user's previously loaded values to the new category's attributes,
		// matching by attribute code. List values are only restored if they still exist in
		// the new attribute's value list.
		if(!preservedValues.isEmpty()) {
			restoreAllocationValues(productId, categoryId, preservedValues);
		}
		log.info("Finished importing attributes for category '" + value + "': created=" + created.get() + ", errors=" + errors.get());
	}

	/** Snapshot of a user-filled allocation value, kept category-independent (keyed later by attribute code). */
	private static class AllocationSnapshot {
		String stringValue;
		BigDecimal numberValue;
		String booleanValue;
		String longStringValue;
		String listValueCode;
	}

	/** Capture the product's loaded attribute values (template rows, no publishing) indexed by attribute code. */
	private Map<String, List<AllocationSnapshot>> captureAllocationValues(int productId) {
		Map<String, List<AllocationSnapshot>> snapshots = new HashMap<>();
		MTable allocationTable = MTable.get(getCtx(), Changes.Table_SP034_Allocation);
		MTable attributeTable = MTable.get(getCtx(), Changes.Table_SP034_Attribute);
		MTable listTable = MTable.get(getCtx(), Changes.Table_SP034_AttributeList);
		new Query(
				getCtx(),
				Changes.Table_SP034_Allocation,
				"M_Product_ID = ? AND SP034_Publishing_ID IS NULL",
				get_TrxName()
			)
			.setParameters(productId)
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(id -> {
				PO allocation = allocationTable.getPO(id, get_TrxName());
				PO attribute = attributeTable.getPO(allocation.get_ValueAsInt("SP034_Attribute_ID"), get_TrxName());
				if(attribute == null) {
					return;
				}
				String code = attribute.get_ValueAsString("Value");
				if(Util.isEmpty(code, true)) {
					return;
				}
				AllocationSnapshot snapshot = new AllocationSnapshot();
				snapshot.stringValue = allocation.get_ValueAsString("SP034_String");
				snapshot.numberValue = (BigDecimal) allocation.get_Value("SP034_Number");
				snapshot.longStringValue = allocation.get_ValueAsString("SP034_LongString");
				Object booleanRaw = allocation.get_Value("SP034_Boolean");
				if(booleanRaw != null) {
					snapshot.booleanValue = allocation.get_ValueAsBoolean("SP034_Boolean") ? "Y" : "N";
				}
				int listId = allocation.get_ValueAsInt("SP034_AttributeList_ID");
				if(listId > 0) {
					PO listValue = listTable.getPO(listId, get_TrxName());
					if(listValue != null) {
						snapshot.listValueCode = listValue.get_ValueAsString("Value");
					}
				}
				snapshots.computeIfAbsent(code, key -> new ArrayList<>()).add(snapshot);
			})
		;
		return snapshots;
	}

	/** Re-create allocations for the new category from the captured values, matching by attribute code. */
	private void restoreAllocationValues(int productId, int categoryId, Map<String, List<AllocationSnapshot>> preservedValues) {
		MTable allocationTable = MTable.get(getCtx(), Changes.Table_SP034_Allocation);
		preservedValues.forEach((code, snapshots) -> {
			PO attribute = getAttributeCategory(code, categoryId);
			if(attribute == null) {
				// Attribute does not exist in the new category -> nothing to restore.
				return;
			}
			int attributeId = attribute.get_ID();
			snapshots.forEach(snapshot -> {
				try {
					PO allocation = allocationTable.getPO(0, get_TrxName());
					allocation.setAD_Org_ID(0);
					allocation.set_ValueOfColumn(I_M_Product.COLUMNNAME_M_Product_ID, productId);
					allocation.set_ValueOfColumn("SP034_Attribute_ID", attributeId);
					boolean hasValue = false;
					if(!Util.isEmpty(snapshot.listValueCode, true)) {
						PO listValue = findListValue(snapshot.listValueCode, attributeId);
						if(listValue == null) {
							// The previously selected list option is not valid in the new category.
							return;
						}
						allocation.set_ValueOfColumn("SP034_AttributeList_ID", listValue.get_ID());
						hasValue = true;
					}
					if(!Util.isEmpty(snapshot.stringValue, true)) {
						allocation.set_ValueOfColumn("SP034_String", snapshot.stringValue);
						hasValue = true;
					}
					if(snapshot.numberValue != null) {
						allocation.set_ValueOfColumn("SP034_Number", snapshot.numberValue);
						hasValue = true;
					}
					if(!Util.isEmpty(snapshot.longStringValue, true)) {
						allocation.set_ValueOfColumn("SP034_LongString", snapshot.longStringValue);
						hasValue = true;
					}
					if(snapshot.booleanValue != null) {
						allocation.set_ValueOfColumn("SP034_Boolean", "Y".equals(snapshot.booleanValue));
						hasValue = true;
					}
					if(!hasValue) {
						return;
					}
					allocation.saveEx();
				} catch (Exception e) {
					log.warning("Could not restore value for attribute '" + code + "': " + e.getLocalizedMessage());
				}
			});
		});
	}

	private PO findListValue(String value, int attributeId) {
		return new Query(
				getCtx(),
				Changes.Table_SP034_AttributeList,
				"Value = ? AND SP034_Attribute_ID = ?",
				get_TrxName()
			)
			.setParameters(value, attributeId)
			.setOnlyActiveRecords(true)
			.first()
		;
	}

}
