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
import okhttp3.*;
import org.adempiere.core.domains.models.I_AD_PInstance;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_W_Store;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MStore;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Util;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Suggest Category)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class SuggestCategory extends SuggestCategoryAbstract {

	private final AtomicInteger created = new AtomicInteger();
	private final AtomicInteger errors = new AtomicInteger();

	@Override
	protected String doIt() throws Exception {
		MProduct product = MProduct.get(getCtx(), getRecord_ID());
		MStore store = MStore.get(getCtx(), getStoreId());
		String categoryUrl = store.get_ValueAsString(Changes.SP034_CategoryUrl);
		if(Util.isEmpty(categoryUrl, true)) {
			throw new AdempiereException("@W_Store_ID@ [@SP034_CategoryUrl@] @NotFound@");
		}
		HttpUrl httpUrl = Objects.requireNonNull(
				HttpUrl.parse(categoryUrl)
			)
			.newBuilder()
			.addQueryParameter("product_name", product.getName())
			.build()
		;
		Request request = new Request.Builder().url(httpUrl).get().build();
		final OkHttpClient client = new OkHttpClient();
		try (Response response = client.newCall(request).execute()) {
			if (response.isSuccessful()) {
				ObjectMapper objectMapper = new ObjectMapper();
				if(response.body() != null) {
					List<CategoryResponse> categories = objectMapper.readValue(response.body().string(), new TypeReference<List<CategoryResponse>>() {});
					createCategories(categories);
				}
			} else {
				throw new AdempiereException("Error sending " + product.getValue() + " - " + product.getName() + ". Response Code: " + response.code() + ", Body: " + response.body().toString());
			}
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
		return "@Created@: " + created + " @Errors@: " + errors;
	}

	private void createCategories(List<CategoryResponse> categories) {
		MTable temporaryCategoryTable = MTable.get(getCtx(), Changes.Table_T_SP034_Category);
		categories.forEach(category -> {
			PO temporaryCategory = temporaryCategoryTable.getPO(0, get_TrxName());
			temporaryCategory.set_ValueOfColumn(I_AD_PInstance.COLUMNNAME_AD_PInstance_ID, getAD_PInstance_ID());
			temporaryCategory.set_ValueOfColumn("Value", category.code);
			temporaryCategory.set_ValueOfColumn("Name", category.name);
			temporaryCategory.set_ValueOfColumn("Description", category.description);
			temporaryCategory.set_ValueOfColumn(I_W_Store.COLUMNNAME_W_Store_ID, getStoreId());
			temporaryCategory.set_ValueOfColumn(I_M_Product.COLUMNNAME_M_Product_ID, getRecord_ID());
			temporaryCategory.saveEx();
			created.incrementAndGet();
		});
	}

}
