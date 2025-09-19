/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/

package org.spin.pos.service.payment;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Ref_List;
import org.adempiere.core.domains.models.I_AD_Reference;
import org.adempiere.core.domains.models.X_C_Payment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.Card;
import org.spin.backend.grpc.pos.CardProvider;
import org.spin.backend.grpc.pos.CreditCardType;
import org.spin.backend.grpc.pos.DeletePaymentReferenceRequest;
import org.spin.backend.grpc.pos.ExistsUnapprovedOnlinePaymentsRequest;
import org.spin.backend.grpc.pos.ExistsUnapprovedOnlinePaymentsResponse;
import org.spin.backend.grpc.pos.ListCardProvidersRequest;
import org.spin.backend.grpc.pos.ListCardProvidersResponse;
import org.spin.backend.grpc.pos.ListCardsRequest;
import org.spin.backend.grpc.pos.ListCardsResponse;
import org.spin.backend.grpc.pos.ListCreditCardTypesRequest;
import org.spin.backend.grpc.pos.ListCreditCardTypesResponse;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.RecordUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.StringManager;

import com.google.protobuf.Empty;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Payment Service Logic for backend of Point Of Sales form
 */
public class PaymentServiceLogic {

	public static ListCreditCardTypesResponse.Builder listCreditCardTypes(ListCreditCardTypesRequest request) {
		// Credit Card Type = 125
		final int referenceId = X_C_Payment.CREDITCARDTYPE_AD_Reference_ID;
		final String whereClause = I_AD_Reference.COLUMNNAME_AD_Reference_ID + " = ? ";

		Query query = new Query(
			Env.getCtx(),
			I_AD_Ref_List.Table_Name,
			whereClause,
			null
		)
			.setParameters(referenceId)
			.setOnlyActiveRecords(true)
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int recordCount = query.count();

		ListCreditCardTypesResponse.Builder builderList = ListCreditCardTypesResponse.newBuilder()
			.setRecordCount(recordCount)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		//	Get List
		query.setLimit(limit, offset)
			.<MRefList>list()
			.forEach(refList -> {
				CreditCardType.Builder builder = PaymentConvertUtil.convertCreditCardType(nexPageToken);
				builderList.addRecords(builder);
			})
		;

		return builderList;
	}


	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static ListCardProvidersResponse.Builder listCardProviders(ListCardProvidersRequest request) {
		// Table Name
		final String tableName = "C_CardProvider";
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("1=1");
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		//	For search value
		final String searchValue = StringManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append(" AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ")"
			);
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
		}

		//	
		String criteriaWhereClause = WhereClauseUtil.getWhereClauseFromCriteria(
			request.getFilters(),
			tableName,
			parameters
		);
		if(whereClause.length() > 0 && !Util.isEmpty(criteriaWhereClause)) {
			whereClause.append(" AND (").append(criteriaWhereClause).append(")");
		}

		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			tableName,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		int count = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListCardProvidersResponse.Builder builderList = ListCardProvidersResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.stream()
			.forEach(cardProviderId -> {
				CardProvider.Builder cardProviderBuilder = PaymentConvertUtil.convertCardProvider(
					cardProviderId
				);
				builderList.addRecords(cardProviderBuilder);
			});
	
		//	Default return
		return builderList;
	}


	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static ListCardsResponse.Builder listCards(ListCardsRequest request) {
		// Table Name
		final String tableName = "C_Card";
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		if (request.getCardProviderId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_CardProvider_ID@");
		}
		whereClause.append("C_CardProvider_ID = ?");
		parameters.add(request.getCardProviderId());

		//	For search value
		final String searchValue = StringManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append(" AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ")"
			);
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
		}

		//	
		String criteriaWhereClause = WhereClauseUtil.getWhereClauseFromCriteria(
			request.getFilters(),
			tableName,
			parameters
		);
		if(whereClause.length() > 0 && !Util.isEmpty(criteriaWhereClause)) {
			whereClause.append(" AND (").append(criteriaWhereClause).append(")");
		}

		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			tableName,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
		;

		int count = query.count();
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		String nexPageToken = null;
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListCardsResponse.Builder builderList = ListCardsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.stream()
			.forEach(cardId -> {
				Card.Builder cardProviderBuilder = PaymentConvertUtil.convertCard(
					cardId
				);
				builderList.addRecords(cardProviderBuilder);
			});
	
		//	Default return
		return builderList;
	}


	/**
	 * Delete order line from uuid
	 * @param request
	 * @return
	 */
	public static Empty.Builder deletePaymentReference(DeletePaymentReferenceRequest request) {
		// MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);
		if(request.getId() <= 0) {
			throw new AdempiereException("@C_POSPaymentReference_ID@ @IsMandatory@");
		}
		if(MTable.get(Env.getCtx(), "C_POSPaymentReference") == null) {
			return Empty.newBuilder();
		}
		Trx.run(transactionName -> {
			PO refundReference = RecordUtil.getEntity(
				Env.getCtx(),
				"C_POSPaymentReference",
				request.getId(),
				transactionName
			);
			if(refundReference == null || refundReference.get_ID() <= 0) {
				throw new AdempiereException("@C_POSPaymentReference_ID@ @NotFound@");
			}
			if ("G".equals(refundReference.get_ValueAsString("TenderType"))) {
				if (refundReference.get_ValueAsInt("ECA14_GiftCard_ID") > 0) {
					GiftCardManagement.unProcessingGiftCard(
							refundReference.get_ValueAsInt("ECA14_GiftCard_ID"), true
					);
				}
			}
			//	Validate processed Order
			refundReference.deleteEx(true);
		});
		//	Return
		return Empty.newBuilder();
	}


	/**
	 * List Payments from Sales Order or POS
	 * @param request
	 * @return
	 */
	public static ExistsUnapprovedOnlinePaymentsResponse.Builder existsUnapprovedOnlinePayments(ExistsUnapprovedOnlinePaymentsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}
		if (request.getOrderId() <= 0) {
			throw new AdempiereException("@C_Order_ID@ @NotFound@");
		}
		int countRecords = PaymentManagement.isOrderWithoutOnlinePaymentApproved(
			request.getOrderId()
		);

		ExistsUnapprovedOnlinePaymentsResponse.Builder builder = ExistsUnapprovedOnlinePaymentsResponse.newBuilder()
			.setRecordCount(
				countRecords
			)
		;

		return builder;
	}

}
