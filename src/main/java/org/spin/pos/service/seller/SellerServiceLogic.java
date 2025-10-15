/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/

package org.spin.pos.service.seller;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.GenericPO;
import org.compiere.model.MPOS;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.AllocateSellerRequest;
import org.spin.backend.grpc.pos.AvailableSeller;
import org.spin.backend.grpc.pos.DeallocateSellerRequest;
import org.spin.backend.grpc.pos.ListAvailableSellersRequest;
import org.spin.backend.grpc.pos.ListAvailableSellersResponse;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;

import com.google.protobuf.Empty;

public class SellerServiceLogic {

	/**
	 * List shipment Lines from Order UUID
	 * @param request
	 * @return
	 */
	public static ListAvailableSellersResponse.Builder listAvailableSellers(ListAvailableSellersRequest request) {
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		//	
		StringBuffer whereClause = new StringBuffer();
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(pos.getC_POS_ID());
		if(request.getIsOtherAllocated()) {
			whereClause.append(
				"EXISTS("
				+ "SELECT 1 FROM C_POSSellerAllocation AS s "
				+ "WHERE s.C_POS_ID <> ? "
				+ "AND s.SalesRep_ID = AD_User.AD_User_ID "
				+ "OR s.IsActive = 'N'"
				+ ") "
			);
		} else {
			whereClause.append(
				"EXISTS("
				+ "SELECT 1 FROM C_POSSellerAllocation AS s "
				+ "WHERE s.C_POS_ID = ? "
				+ "AND s.SalesRep_ID = AD_User.AD_User_ID "
				+ "AND s.IsActive = 'Y'"
				+ ") "
			);
		}

		// URL decode to change characteres and add search value to filter
		final String searchValue = StringManager.getValidString(
			StringManager.getDecodeUrl(
				request.getSearchValue()
			)
		).strip();
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append(
				" AND ( "
				+ "UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name2) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ ") "
			);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
		}

		Query query = new Query(
			Env.getCtx(),
			I_AD_User.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
		;

		int count = query.count();
		String nextPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nextPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListAvailableSellersResponse.Builder builder = ListAvailableSellersResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(
					nextPageToken
				)
			)
		;

		query
			.setLimit(limit, offset)
			// .<MUser>list()
			.getIDsAsList()
			.forEach(sellerId -> {
				MUser seller = MUser.get(Env.getCtx(), sellerId);
				AvailableSeller.Builder sellerBuilder = SellerConvertUtil.convertSeller(seller);
				builder.addSellers(sellerBuilder);
			})
		;
		//	
		return builder;
	}


	/**
	 * Allocate a seller to point of sales
	 * @param request
	 * @return
	 */
	public static AvailableSeller.Builder allocateSeller(AllocateSellerRequest request) {
		MPOS pointOfSales = POS.validateAndGetPOS(request.getPosId(), true);
		final int posId = pointOfSales.getC_POS_ID();

		if(request.getSalesRepresentativeId() <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @IsMandatory@");
		}
		int salesRepresentativeId = request.getSalesRepresentativeId();

		if(!pointOfSales.get_ValueAsBoolean("IsAllowsAllocateSeller")) {
			throw new AdempiereException("@POS.AllocateSellerNotAllowed@");
		}
		Trx.run(transactionName -> {
			List<Integer> allocatedSellersIds = new Query(Env.getCtx(), "C_POSSellerAllocation", "C_POS_ID = ?", transactionName).setParameters(posId).getIDsAsList();
			if(!pointOfSales.get_ValueAsBoolean("IsAllowsConcurrentUse")) {
				allocatedSellersIds
				.forEach(allocatedSellerId -> {
					PO allocatedSeller = new GenericPO("C_POSSellerAllocation", Env.getCtx(), allocatedSellerId, transactionName);
					if(allocatedSeller.get_ValueAsInt("SalesRep_ID") != salesRepresentativeId) {
						allocatedSeller.set_ValueOfColumn("IsActive", false);
						allocatedSeller.saveEx(transactionName);
					}
				});
			}
			//	For add seller
			PO seller = new Query(Env.getCtx(), "C_POSSellerAllocation", "C_POS_ID = ? AND SalesRep_ID = ?", transactionName).setParameters(posId, salesRepresentativeId).first();
			if(seller == null
					|| seller.get_ID() <= 0) {
				seller = new GenericPO("C_POSSellerAllocation", Env.getCtx(), 0, transactionName);
				seller.set_ValueOfColumn("C_POS_ID", posId);
				seller.set_ValueOfColumn("SalesRep_ID", salesRepresentativeId);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsModifyQuantity,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsModifyQuantity)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsReturnOrder,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsReturnOrder)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsCollectOrder,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsCollectOrder)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsCreateOrder,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsCreateOrder)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsDisplayTaxAmount,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsDisplayTaxAmount)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsDisplayDiscount,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsDisplayDiscount)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsConfirmShipment,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsConfirmShipment)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsConfirmCompleteShipment,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsConfirmCompleteShipment)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsAllocateSeller,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsAllocateSeller)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsConcurrentUse,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsConcurrentUse)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsCashOpening,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsCashOpening)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsCashClosing,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsCashClosing)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsCashWithdrawal,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsCashWithdrawal)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsApplyDiscount,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsApplyDiscount)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_MaximumRefundAllowed,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_MaximumRefundAllowed)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_MaximumDailyRefundAllowed,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_MaximumDailyRefundAllowed)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_MaximumDiscountAllowed,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_MaximumDiscountAllowed)
				);

				// Write Off
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_WriteOffAmtCurrency_ID,
					pointOfSales.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WriteOffAmtCurrency_ID)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_WriteOffAmtTolerance,
					NumberManager.getBigDecimalFromString(
						pointOfSales.get_ValueAsString(ColumnsAdded.COLUMNNAME_WriteOffAmtTolerance)
					)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_ECA14_WriteOffByPercent,
					NumberManager.getBigDecimalFromString(
						pointOfSales.get_ValueAsString(ColumnsAdded.COLUMNNAME_ECA14_WriteOffByPercent)
					)
				);

				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsCreateCustomer,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsCreateCustomer)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsPrintDocument,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsPrintDocument)
				);
				seller.set_ValueOfColumn(
					ColumnsAdded.COLUMNNAME_IsAllowsPreviewDocument,
					pointOfSales.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllowsPreviewDocument)
				);
			}
			seller.set_ValueOfColumn("IsActive", true);
			seller.saveEx(transactionName);
		});
		//	Return
		return AvailableSeller.newBuilder();
	}


	/**
	 * Allocate a seller to point of sales
	 * @param request
	 * @return
	 */
	public static Empty.Builder deallocateSeller(DeallocateSellerRequest request) {
		MPOS pointOfSales = POS.validateAndGetPOS(request.getPosId(), true);
		final int posId = pointOfSales.getC_POS_ID();

		if(request.getSalesRepresentativeId() <= 0) {
			throw new AdempiereException("@SalesRep_ID@ @IsMandatory@");
		}
		int salesRepresentativeId = request.getSalesRepresentativeId();

		if(!pointOfSales.get_ValueAsBoolean("IsAllowsAllocateSeller")) {
			throw new AdempiereException("@POS.AllocateSellerNotAllowed@");
		}
		Trx.run(transactionName -> {
			PO seller = new Query(Env.getCtx(), "C_POSSellerAllocation", "C_POS_ID = ? AND SalesRep_ID = ?", transactionName).setParameters(posId, salesRepresentativeId).first();
			if(seller == null
					|| seller.get_ID() <= 0) {
				throw new AdempiereException("@SalesRep_ID@ @NotFound@");
			}
			seller.set_ValueOfColumn("IsActive", false);
			seller.saveEx(transactionName);
		});
		//	Return
		return Empty.newBuilder();
	}

}
