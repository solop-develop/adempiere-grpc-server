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

import com.solop.sp034.util.Changes;

import org.adempiere.core.domains.models.I_C_Tax;
import org.adempiere.core.domains.models.I_M_PriceList_Version;
import org.adempiere.core.domains.models.I_M_Product;
import org.adempiere.core.domains.models.I_M_ProductPrice;
import org.adempiere.core.domains.models.I_W_Store;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPricing;
import org.compiere.model.MStore;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Publish Products)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">solopsoftware.com</a>
 */
public class PublishProducts extends PublishProductsAbstract {

	private final AtomicInteger publications = new AtomicInteger();
	private final AtomicInteger errors = new AtomicInteger();
	private MTable publishingTable;

	@Override
	protected String doIt() throws Exception {
		publishingTable = MTable.get(getCtx(), Changes.Table_SP034_Publishing);
		MStore store = MStore.get(getCtx(), getStoreId());
		MPriceList priceList = MPriceList.get(getCtx(), store.getM_PriceList_ID(), null);
		MPriceListVersion version = priceList.getPriceListVersion(TimeUtil.getDay(System.currentTimeMillis()));
		if(version == null) {
			throw new AdempiereException("@M_PriceList_Version_ID@ @NotFound@");
		}
		getValidProductsId().parallelStream().forEach(productId -> {
			try {
				Trx.run(transactionName -> {
					createPublishing(productId, version.getM_PriceList_Version_ID(), transactionName);
				});
			} catch (Exception e) {
				log.warning(e.getLocalizedMessage());
				errors.incrementAndGet();
			}
		});
		return "@Created@: " + publications + " @Errors@: " + errors;
	}

	private void createPublishing(int productId, int priceListVersionId, String transactionName) {
		MProductPricing productPricing = new MProductPricing (productId, 0, Env.ZERO, true, null);
		productPricing.setM_PriceList_Version_ID(priceListVersionId);
		MProduct product = MProduct.get(getCtx(), productId);
		PO publishing = new Query(
			getCtx(),
			Changes.Table_SP034_Publishing,
			"M_Product_ID = ? AND W_Store_ID = ? AND SP034_PublishStatus <> 'C'",
			transactionName
		)
			.setParameters(productId, getStoreId())
			.setClient_ID()
			.first()
		;
		boolean isNew = publishing == null;
		if (isNew) {
			publishing = publishingTable.getPO(0, transactionName);
			publishing.set_ValueOfColumn(I_M_Product.COLUMNNAME_M_Product_ID, productId);
			publishing.set_ValueOfColumn(I_W_Store.COLUMNNAME_W_Store_ID, getStoreId());
			publishing.set_ValueOfColumn(Changes.SP034_PublishStatus, Changes.SP034_PublishStatus_Without_Publishing);
		}
		publishing.set_ValueOfColumn(I_M_PriceList_Version.COLUMNNAME_M_PriceList_Version_ID, priceListVersionId);
		publishing.set_ValueOfColumn(I_M_ProductPrice.COLUMNNAME_PriceList, productPricing.getPriceList());
		publishing.set_ValueOfColumn(I_M_ProductPrice.COLUMNNAME_PriceStd, productPricing.getPriceStd());
		publishing.set_ValueOfColumn(I_M_ProductPrice.COLUMNNAME_PriceLimit, productPricing.getPriceLimit());
		int taxId = getTaxId(product.getC_TaxCategory_ID());
		if(taxId > 0) {
			publishing.set_ValueOfColumn(I_C_Tax.COLUMNNAME_C_Tax_ID, taxId);
		}
		publishing.saveEx();
		refreshAllocations(productId, publishing.get_ID(), isNew, transactionName);
		publications.incrementAndGet();
	}

	private void refreshAllocations(int productId, int publishingId, boolean isNew, String transactionName) {
		MTable allocationTable = MTable.get(getCtx(), Changes.Table_SP034_Allocation);
		if (!isNew) {
			new Query(
				getCtx(),
				Changes.Table_SP034_Allocation,
				"M_Product_ID = ? AND SP034_Publishing_ID = ?",
				transactionName
			)
				.setParameters(productId, publishingId)
				.getIDsAsList()
				.forEach(id -> {
					PO allocationEntity = allocationTable.getPO(id, transactionName);
					allocationEntity.deleteEx(true);
				})
			;
		}
		new Query(
			getCtx(),
			Changes.Table_SP034_Allocation,
			"M_Product_ID = ? AND SP034_Publishing_ID IS NULL",
			null
		)
			.setParameters(productId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(allocationId -> {
				PO fromAllocation = allocationTable.getPO(allocationId, null);
				PO toAllocation = allocationTable.getPO(0, transactionName);
				PO.copyValues(fromAllocation, toAllocation);
				toAllocation.set_ValueOfColumn("UUID", UUID.randomUUID().toString());
				toAllocation.set_ValueOfColumn("SP034_Publishing_ID", publishingId);
				toAllocation.saveEx();
			})
		;
	}

	private int getTaxId(int taxCategoryId) {
		if (taxCategoryId <= 0) {
			return -1;
		}
		return new Query(
			getCtx(),
			I_C_Tax.Table_Name,
			"C_TaxCategory_ID = ? AND (IsSalesTax = 'Y' OR SOPOType IN('S', 'B')) ",
			null
		)
			.setParameters(taxCategoryId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.firstId()
		;
	}

	private List<Integer> getValidProductsId() {
		List<Object> products = new ArrayList<>();
		if(getSelectionKeys() == null || getSelectionKeys().isEmpty()) {
			if(getRecord_ID() <= 0) {
				log.warning("No publishing Record_ID selected");
				return new ArrayList<>();
			}
			products.add(getRecord_ID());
		} else {
			products.addAll(getSelectionKeys());
		}
		if(products == null || products.isEmpty()) {
			log.warning("No products selected");
			return new ArrayList<>();
		}
		return new Query(
			getCtx(),
			I_M_Product.Table_Name,
			"M_Product_ID IN" + products.toString().replace("[", "(").replace("]", ")"),
			get_TrxName()
		)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.getIDsAsList()
		;
	}
}
