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

package org.spin.pos.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.core.domains.models.I_AD_PrintFormatItem;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_POS;
import org.adempiere.core.domains.models.I_M_DiscountSchema;
import org.adempiere.core.domains.models.I_M_InOutLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDiscountSchema;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPOS;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUOMConversion;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.pos.AvailableDiscountSchema;
import org.spin.backend.grpc.pos.Customer;
import org.spin.backend.grpc.pos.CustomerTemplate;
import org.spin.backend.grpc.pos.GetCustomerRequest;
import org.spin.backend.grpc.pos.ListAvailableDiscountsRequest;
import org.spin.backend.grpc.pos.ListAvailableDiscountsResponse;
import org.spin.backend.grpc.pos.ListCustomerTemplatesRequest;
import org.spin.backend.grpc.pos.ListCustomerTemplatesResponse;
import org.spin.backend.grpc.pos.ListCustomersRequest;
import org.spin.backend.grpc.pos.ListCustomersResponse;
import org.spin.backend.grpc.pos.ShipmentLine;
import org.spin.backend.grpc.pos.UpdateShipmentLineRequest;
import org.spin.base.db.WhereClauseUtil;
import org.spin.base.util.DocumentUtil;
import org.spin.pos.service.order.ShipmentUtil;
import org.spin.pos.service.pos.POS;
import org.spin.pos.util.POSConvertUtil;
import org.spin.service.grpc.authentication.SessionManager;
import org.spin.service.grpc.util.db.LimitUtil;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.service.grpc.util.value.ValueManager;

public class POSLogic {

	public static ListAvailableDiscountsResponse.Builder listAvailableDiscounts(ListAvailableDiscountsRequest request) {
		if(request.getPosId() <= 0) {
			throw new AdempiereException("@C_POS_ID@ @NotFound@");
		}

		ListAvailableDiscountsResponse.Builder builderList = ListAvailableDiscountsResponse.newBuilder();
		final String TABLE_NAME = "C_POSDiscountAllocation";
		if (MTable.getTable_ID(TABLE_NAME) <= 0) {
			return builderList;
		}

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		//	Dynamic where clause
		//	Aisle Seller
		int posId = request.getPosId();
		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			TABLE_NAME,
			"C_POS_ID = ?",
			null
		)
			.setParameters(posId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo)
		;

		int count = query.count();
		query
			.setLimit(limit, offset)
			.list()
			.forEach(availableDiscountSchema -> {
				MDiscountSchema discountSchema = MDiscountSchema.get(
					Env.getCtx(),
					availableDiscountSchema.get_ValueAsInt(
						I_M_DiscountSchema.COLUMNNAME_M_DiscountSchema_ID
					)
				);

				AvailableDiscountSchema.Builder builder = AvailableDiscountSchema.newBuilder()
					.setId(
						discountSchema.getM_DiscountSchema_ID()
					)
					.setKey(
						StringManager.getValidString(
							discountSchema.getName()
						)
					)
					.setName(
						StringManager.getValidString(
							discountSchema.getName()
						)
					)
					.setIsPosRequiredPin(
						availableDiscountSchema.get_ValueAsBoolean(
							I_C_POS.COLUMNNAME_IsPOSRequiredPIN
						)
					)
					.setFlatDiscountPercetage(
						NumberManager.getBigDecimalToString(
							discountSchema.getFlatDiscount()
						)
					)
				;
				builderList.addDiscounts(builder);
			})
		;
		//	
		builderList.setRecordCount(count);
		//	Set page token
		if(LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(
			StringManager.getValidString(nexPageToken)
		);
		return builderList;
	}


	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static ListCustomersResponse.Builder listCustomers(ListCustomersRequest request) {
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		//	For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			whereClause.append("("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(TaxID) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name2) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Description) LIKE '%' || UPPER(?) || '%'"
				+ ")"
			);
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
			parameters.add(searchValue);
		}
		//	For value
		if(!Util.isEmpty(request.getValue(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Value) LIKE UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Name) LIKE UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Name) LIKE UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.EMail) LIKE UPPER(?)))"
			);
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"("
				+ "EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Phone) LIKE UPPER(?)) "
				+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(bpl.Phone) LIKE UPPER(?))"
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(l.Postal) LIKE UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getPostalCode());
		}
		//	
		String criteriaWhereClause = WhereClauseUtil.getWhereClauseFromCriteria(request.getFilters(), I_C_BPartner.Table_Name, parameters);
		if(whereClause.length() > 0
				&& !Util.isEmpty(criteriaWhereClause)) {
			whereClause.append(" AND (").append(criteriaWhereClause).append(")");
		}

		//	Get Product list
		Query query = new Query(
			Env.getCtx(),
			I_C_BPartner.Table_Name,
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

		ListCustomersResponse.Builder builderList = ListCustomersResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.getIDsAsList()
			.stream()
			.forEach(businessPartnerId -> {
				Customer.Builder customBuilder = POSConvertUtil.convertCustomer(
					businessPartnerId
				);
				builderList.addCustomers(customBuilder);
			});
	
		//	Default return
		return builderList;
	}

	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static Customer.Builder getCustomer(GetCustomerRequest request) {
		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		//	For search value
		final String searchValue = ValueManager.getDecodeUrl(
			request.getSearchValue()
		);
		if(!Util.isEmpty(searchValue, true)) {
			// TODO: Check if it is better with the `LIKE` operator 
			whereClause.append(
				"(UPPER(Value) = UPPER(?) "
				+ "OR UPPER(Name) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(searchValue);
			parameters.add(searchValue);
		}
		//	For value
		if(!Util.isEmpty(request.getValue(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Value) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getValue());
		}
		//	For name
		if(!Util.isEmpty(request.getName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(UPPER(Name) = UPPER(?))"
			);
			//	Add parameters
			parameters.add(request.getName());
		}
		//	for contact name
		if(!Util.isEmpty(request.getContactName(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Name) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getContactName());
		}
		//	EMail
		if(!Util.isEmpty(request.getEmail(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.EMail) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getEmail());
		}
		//	Phone
		if(!Util.isEmpty(request.getPhone(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"("
				+ "EXISTS(SELECT 1 FROM AD_User u "
				+ "WHERE u.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(u.Phone) = UPPER(?)) "
				+ "OR EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(bpl.Phone) = UPPER(?))"
				+ ")"
			);
			//	Add parameters
			parameters.add(request.getPhone());
			parameters.add(request.getPhone());
		}
		//	Postal Code
		if(!Util.isEmpty(request.getPostalCode(), true)) {
			if(whereClause.length() > 0) {
				whereClause.append(" AND ");
			}
			whereClause.append(
				"(EXISTS(SELECT 1 FROM C_BPartner_Location bpl "
				+ "INNER JOIN C_Location l ON(l.C_Location_ID = bpl.C_Location_ID) "
				+ "WHERE bpl.C_BPartner_ID = C_BPartner.C_BPartner_ID "
				+ "AND UPPER(l.Postal) = UPPER(?)))"
			);
			//	Add parameters
			parameters.add(request.getPostalCode());
		}

		//	Get business partner
		MBPartner businessPartner = new Query(
			Env.getCtx(),
			I_C_BPartner.Table_Name,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.first()
		;
		//	Default return
		return POSConvertUtil.convertCustomer(
			businessPartner
		);
	}


	/**
	 * Get Customer
	 * @param request
	 * @return
	 */
	public static ListCustomerTemplatesResponse.Builder listCustomerTemplates(ListCustomerTemplatesRequest request) {		
		MPOS pos = POS.validateAndGetPOS(request.getPosId(), true);

		ListCustomerTemplatesResponse.Builder builderList = ListCustomerTemplatesResponse.newBuilder();

		final String TABLE_NAME = "C_POSBPTemplate";
		if(MTable.getTable_ID(TABLE_NAME) <= 0) {
			// table not found
			return builderList;
		}

		//	Dynamic where clause
		StringBuffer whereClause = new StringBuffer();
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();

		// Add pos filter
		whereClause.append("C_POS_ID = ? ");
		parameters.add(
			pos.getC_POS_ID()
		);

		// whereClause.append(
		// 	"AND EXISTS("
		// 	+ "SELECT 1 FROM C_BPartner AS bp "
		// 	+ "WHERE bp.C_BPartner_ID = C_POSBPTemplate.C_BPartner_ID "
		// 	+ "AND bp.IsActive = ? "
		// 	+ ")"
		// );
		// parameters.add(true);

		//	Get Customer Tempates list
		Query query = new Query(
			Env.getCtx(),
			TABLE_NAME,
			whereClause.toString(),
			null
		)
			.setParameters(parameters)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.setApplyAccessFilter(MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO)
			.setOrderBy(I_AD_PrintFormatItem.COLUMNNAME_SeqNo)
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

		builderList
			.setRecordCount(count)
			.setNextPageToken(
				StringManager.getValidString(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list()
			.stream()
			.forEach(posCustomerTemplate -> {
				CustomerTemplate.Builder customerTemplateBuilder = POSConvertUtil.convertCustomerTemplate(
					posCustomerTemplate
				);
				builderList.addCustomerTemplates(customerTemplateBuilder);
			});
	
		//	Default return
		return builderList;
	}


	public static ShipmentLine.Builder updateShipmentLine(UpdateShipmentLineRequest request) {
		//	Validate Order
		if(request.getShipmentId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_InOut_ID@");
		}
		//	Validate Product and charge
		if(request.getId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_InOutLine_ID@");
		}

		MInOut shipmentHeader = new MInOut(Env.getCtx(), request.getShipmentId(), null);
		if (shipmentHeader == null || shipmentHeader.getM_InOut_ID() <= 0) {
			throw new AdempiereException("@M_InOut_ID@ @NotFound@");
		}
		if(!DocumentUtil.isDrafted(shipmentHeader)) {
			throw new AdempiereException("@M_InOut_ID@ @Processed@");
		}

		AtomicReference<MInOutLine> shipmentLineReference = new AtomicReference<MInOutLine>();
		Trx.run(transactionName -> {
			MInOutLine shipmentLine = new Query(
				Env.getCtx(),
				I_M_InOutLine.Table_Name,
				I_M_InOutLine.COLUMNNAME_M_InOutLine_ID + " = ?",
				transactionName
			)
				.setParameters(request.getId())
				.setClient_ID()
				.first()
			;

			if (shipmentLine == null || shipmentLine.getM_InOutLine_ID() <= 0) {
				throw new AdempiereException("@M_InOutLine_ID@ @NotFound@");
			}
			//	Validate processed Order
			if(shipmentLine.isProcessed()) {
				throw new AdempiereException("@M_InOutLine_ID@ @Processed@");
			}

			// Validate quantity
			MOrderLine sourcerOrderLine = new MOrderLine(Env.getCtx(), shipmentLine.getC_OrderLine_ID(), transactionName);
			if(sourcerOrderLine == null || sourcerOrderLine.getC_OrderLine_ID() <= 0) {
				throw new AdempiereException("@C_OrderLine_ID@ @NotFound@");
			}

			BigDecimal quantity = Optional.ofNullable(
				NumberManager.getBigDecimalFromString(
					request.getQuantity()
				)
			).orElse(Env.ZERO);
			BigDecimal availableQuantity = ShipmentUtil.getAvailableQuantityForShipment(
				sourcerOrderLine.getC_OrderLine_ID(),
				shipmentLine.getM_InOutLine_ID(),
				sourcerOrderLine.getQtyEntered(),
				quantity
			);
			if (availableQuantity.compareTo(Env.ZERO) <= 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}
			if (availableQuantity.compareTo(quantity) < 0) {
				throw new AdempiereException("@QtyInsufficient@");
			}

			BigDecimal convertedQuantity = MUOMConversion.convertProductFrom(
				shipmentLine.getCtx(),
				shipmentLine.getM_Product_ID(),
				shipmentLine.getC_UOM_ID(),
				quantity
			);
			shipmentLine.setQty(convertedQuantity);

			shipmentLine.setDescription(
				request.getDescription()
			);

			//	Save Line
			shipmentLine.saveEx();
			shipmentLineReference.set(shipmentLine);
		});

		//	Convert Line
		return POSConvertUtil.convertShipmentLine(
			shipmentLineReference.get()
		);
	}

}
