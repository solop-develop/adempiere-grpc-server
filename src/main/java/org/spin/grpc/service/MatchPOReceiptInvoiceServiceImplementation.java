/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
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
package org.spin.grpc.service;

import org.adempiere.exceptions.AdempiereException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_AD_System;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_InvoiceLine;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.core.domains.models.I_M_Product;
import org.compiere.model.MBPartner;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProduct;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedFromRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedFromResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedToRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedToResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchesTypesFromRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchesTypesToRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListProductsRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListProductsResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListSearchModesRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListVendorsRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.MatchMode;
import org.spin.backend.grpc.form.match_po_receipt_invoice.MatchType;
import org.spin.backend.grpc.form.match_po_receipt_invoice.Matched;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ProcessRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ProcessResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.Product;
import org.spin.backend.grpc.form.match_po_receipt_invoice.Vendor;
import org.spin.backend.grpc.form.match_po_receipt_invoice.MatchPORReceiptInvoiceGrpc.MatchPORReceiptInvoiceImplBase;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Match PO-Receipt-Invoice form
 */
public class MatchPOReceiptInvoiceServiceImplementation extends MatchPORReceiptInvoiceImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(MatchPOReceiptInvoiceServiceImplementation.class);



	@Override
	public void listMatchesTypesFrom(ListMatchesTypesFromRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listMatchesTypesFrom(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListLookupItemsResponse.Builder listMatchesTypesFrom(ListMatchesTypesFromRequest request) {
		Properties context = Env.getCtx();

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		// Invoice
		LookupItem.Builder lookupInvoice = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchType.INVOICE_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, I_C_Invoice.COLUMNNAME_C_Invoice_ID)
				).build())
		;
		builderList.addRecords(lookupInvoice);

		// Receipt
		LookupItem.Builder lookupReceipt = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchType.RECEIPT_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, I_M_InOut.COLUMNNAME_M_InOut_ID)
				).build())
		;
		builderList.addRecords(lookupReceipt);

		// Purchase Order
		LookupItem.Builder lookupOrder = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchType.RECEIPT_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, I_C_Order.COLUMNNAME_C_Order_ID)
				).build())
		;
		builderList.addRecords(lookupOrder);

		return builderList;
	}



	@Override
	public void listMatchesTypesTo(ListMatchesTypesToRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listMatchesTypesTo(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListLookupItemsResponse.Builder listMatchesTypesTo(ListMatchesTypesToRequest request) {
		Properties context = Env.getCtx();
		MatchType matchTypeFrom = request.getMatchFromType();

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		// Invoice
		if (matchTypeFrom != MatchType.INVOICE) {
			LookupItem.Builder lookupInvoice = LookupItem.newBuilder()
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					ValueUtil.getValueFromInt(
						MatchType.INVOICE_VALUE
					).build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					ValueUtil.getValueFromString(
						Msg.translate(context, I_C_Invoice.COLUMNNAME_C_Invoice_ID)
					).build())
			;
			builderList.addRecords(lookupInvoice);
		}

		// Receipt
		if (matchTypeFrom != MatchType.RECEIPT) {
			LookupItem.Builder lookupReceipt = LookupItem.newBuilder()
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					ValueUtil.getValueFromInt(
						MatchType.RECEIPT_VALUE
					).build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					ValueUtil.getValueFromString(
						Msg.translate(context, I_M_InOut.COLUMNNAME_M_InOut_ID)
					).build())
			;
			builderList.addRecords(lookupReceipt);
		}

		// Purchase Order
		if (matchTypeFrom != MatchType.PURCHASE_ORDER) {
			LookupItem.Builder lookupOrder = LookupItem.newBuilder()
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					ValueUtil.getValueFromInt(
						MatchType.PURCHASE_ORDER_VALUE
					).build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					ValueUtil.getValueFromString(
						Msg.translate(context, I_C_Order.COLUMNNAME_C_Order_ID)
					).build())
			;
			builderList.addRecords(lookupOrder);
		}

		return builderList;
	}

	@Override
	public void listSearchModes(ListSearchModesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listSearchModes(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListLookupItemsResponse.Builder listSearchModes(ListSearchModesRequest request) {
		Properties context = Env.getCtx();

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		LookupItem.Builder lookupMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_NOT_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, "NotMatched")
				).build())
		;
		builderList.addRecords(lookupMatched);

		LookupItem.Builder lookupUnMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, "Matched")
				).build())
		;
		builderList.addRecords(lookupUnMatched);

		return builderList;
	}



	public static Vendor.Builder convertVendor(int businessPartnerId) {
		if (businessPartnerId <= 0) {
			return Vendor.newBuilder();
		}
		MBPartner businessPartner = MBPartner.get(Env.getCtx(), businessPartnerId);
		return convertVendor(businessPartner);
	}
	public static Vendor.Builder convertVendor(MBPartner businessPartner) {
		Vendor.Builder builder = Vendor.newBuilder();
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}

		builder.setId(businessPartner.getC_BPartner_ID())
			.setUuid(
				ValueUtil.validateNull(businessPartner.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(businessPartner.getValue())
			)
			.setTaxId(
				ValueUtil.validateNull(businessPartner.getTaxID())
			)
			.setName(
				ValueUtil.validateNull(businessPartner.getName())
			)
			.setDescription(
				ValueUtil.validateNull(businessPartner.getDescription())
			)
		;

		return builder;
	}

	@Override
	public void listVendors(ListVendorsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listVendors(request);
			responseObserver.onNext(builderList.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	ListLookupItemsResponse.Builder listVendors(ListVendorsRequest request) {
		// Business Partner
		int validationRuleId = 52520; // C_BPartner - Vendor
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			I_C_BPartner.COLUMNNAME_C_BPartner_ID,
			validationRuleId
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void listProducts(ListProductsRequest request, StreamObserver<ListProductsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListProductsResponse.Builder builder = listProducts(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListProductsResponse.Builder listProducts(ListProductsRequest request) {
		//	Dynamic where clause
		String whereClause = "IsPurchased = 'Y' AND IsSummary = 'N' ";
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	For search value
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause += " AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(UPC) = UPPER(?) "
				+ "OR UPPER(SKU) = UPPER(?) "
				+ ")"
			;
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}

		Query query = new Query(
			Env.getCtx(),
			I_M_Product.Table_Name,
			whereClause.toString(),
			null
		)
			.setClient_ID()
			.setParameters(parameters);

		int count = query.count();
		String nexPageToken = "";
		int pageNumber = RecordUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = RecordUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (RecordUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = RecordUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListProductsResponse.Builder builderList = ListProductsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list(MProduct.class)
			.forEach(product -> {
				Product.Builder builder = convertProduct(product);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}

	static Product.Builder convertProduct(int productId) {
		Product.Builder builder = Product.newBuilder();
		if (productId <= 0) {
			return builder;
		}
		MProduct product = MProduct.get(Env.getCtx(), productId);
		return convertProduct(product);
	}
	static Product.Builder convertProduct(MProduct product) {
		Product.Builder builder = Product.newBuilder();
		if (product == null) {
			return builder;
		}
		builder.setId(product.getM_Product_ID())
			.setUuid(
				ValueUtil.validateNull(product.getUUID())
			)
			.setUpc(
				ValueUtil.validateNull(product.getUPC())
			)
			.setSku(
				ValueUtil.validateNull(product.getSKU())
			)
			.setValue(
				ValueUtil.validateNull(product.getValue())
			)
			.setName(
				ValueUtil.validateNull(product.getName())
			)
		;

		return builder;
	}



	public static String getDateColumn(int matchType) {
		if (matchType == MatchType.INVOICE_VALUE) {
			return "hdr.DateInvoiced";
		}
		else if (matchType == MatchType.PURCHASE_ORDER_VALUE) {
			return "hdr.DateOrdered";
		}
		// Receipt
		return "hdr.MovementDate";
	}

	public static String getQuantityColumn(int matchType) {
		if (matchType == MatchType.INVOICE_VALUE) {
			return "lin.QtyInvoiced";
		}
		else if (matchType == MatchType.PURCHASE_ORDER_VALUE) {
			return "lin.QtyOrdered";
		}
		// Receipt
		return "lin.MovementQty";
	}

	public static String getSQL(boolean isMatched, int matchTypeFrom, int matchTypeTo) {
		String sql = "";
		if (matchTypeFrom == MatchType.INVOICE_VALUE) {
			sql = "SELECT lin.C_InvoiceLine_ID AS ID, lin.UUID AS UUID, "
				+ " hdr.C_Invoice_ID AS Header_ID, hrd.Header_UUID, hdr.DateInvoiced AS Date,"
				+ " hdr.C_Invoice_ID, hdr.DocumentNo, hdr.DateInvoiced, "
				+ " bp.Name AS C_BPartner_Name, hdr.C_BPartner_ID, "
				+ " lin.Line, lin.C_InvoiceLine_ID, "
				+ " p.Name AS M_Product_Name, lin.M_Product_ID, "
				+ " lin.QtyInvoiced, SUM(COALESCE(mi.Qty, 0)), org.Name, hdr.AD_Org_ID "
				+ "FROM C_Invoice hdr"
				+ " INNER JOIN AD_Org org ON (hdr.AD_Org_ID = org.AD_Org_ID)"
				+ " INNER JOIN C_BPartner bp ON (hdr.C_BPartner_ID = bp.C_BPartner_ID)"
				+ " INNER JOIN C_InvoiceLine lin ON (hdr.C_Invoice_ID = lin.C_Invoice_ID)"
				+ " INNER JOIN M_Product p ON (lin.M_Product_ID = p.M_Product_ID)"
				+ " INNER JOIN C_DocType dt ON (hdr.C_DocType_ID = dt.C_DocType_ID AND dt.DocBaseType IN ('API', 'APC'))"
				+ " FULL JOIN M_MatchInv mi ON (lin.C_InvoiceLine_ID = mi.C_InvoiceLine_ID) "
				+ "WHERE hdr.DocStatus IN ('CO','CL')"
			;
		}
		else if (matchTypeFrom == MatchType.PURCHASE_ORDER_VALUE) {
			String lineType = matchTypeTo == MatchType.RECEIPT_VALUE ? "M_InOutLine_ID" : "C_InvoiceLine_ID";

			sql = "SELECT lin.C_OrderLine_ID AS ID, lin.UUID AS UUID, "
				+ " hdr.C_Order_ID AS Header_ID, hrd.Header_UUID, hdr.DateOrdered AS Date,"
				+ " hdr.C_Order_ID, hdr.DocumentNo, hdr.DateOrdered, "
				+ " bp.Name AS C_BPartner_Name, hdr.C_BPartner_ID, "
				+ " lin.Line, lin.C_OrderLine_ID, "
				+ " p.Name AS M_Product_Name, lin.M_Product_ID, "
				+ " lin.QtyOrdered, SUM(COALESCE(mo.Qty, 0)), org.Name, hdr.AD_Org_ID "
				+ "FROM C_Order hdr"
				+ " INNER JOIN AD_Org org ON (hdr.AD_Org_ID = org.AD_Org_ID)"
				+ " INNER JOIN C_BPartner bp ON (hdr.C_BPartner_ID = bp.C_BPartner_ID)"
				+ " INNER JOIN C_OrderLine lin ON (hdr.C_Order_ID = lin.C_Order_ID)"
				+ " INNER JOIN M_Product p ON (lin.M_Product_ID = p.M_Product_ID)"
				+ " INNER JOIN C_DocType dt ON (hdr.C_DocType_ID = dt.C_DocType_ID AND dt.DocBaseType = 'POO')"
				+ " FULL JOIN M_MatchPO mo ON (lin.C_OrderLine_ID = mo.C_OrderLine_ID) "
				+ " WHERE "
			;
			
			if (isMatched) {
				sql += " mo." + lineType + " IS NOT NULL "; 
			} else {
 				sql += " ( mo." + lineType + " IS NULL OR "
					+ " (lin.QtyOrdered <> (SELECT sum(mo1.Qty) AS Qty"
					+ " FROM m_matchpo mo1 WHERE "
					+ " mo1.C_ORDERLINE_ID = lin.C_ORDERLINE_ID AND "
					+ " hdr.C_ORDER_ID = lin.C_ORDER_ID AND "
					+ " mo1." + lineType
					+ " IS NOT NULL group by mo1.C_ORDERLINE_ID))) "
				;
			}
			sql += " AND hdr.DocStatus IN ('CO', 'CL') ";
		}
		// Receipt
		else {
			sql = "SELECT lin.M_InOutLine_ID AS ID, lin.UUID AS UUID, "
				+ " hdr.M_InOut_ID AS Header_ID, hrd.Header_UUID, hdr.MovementDate AS Date,"
				+ " hdr.M_InOut_ID, hdr.DocumentNo, hdr.MovementDate, "
				+ " bp.Name AS C_BPartner_Name, hdr.C_BPartner_ID, "
				+ " lin.Line, lin.M_InOutLine_ID, "
				+ " p.Name AS M_Product_Name, lin.M_Product_ID, "
				+ " lin.MovementQty, SUM(COALESCE(m.Qty, 0)), org.Name, hdr.AD_Org_ID "
				+ "FROM M_InOut hdr"
				+ " INNER JOIN AD_Org org ON (hdr.AD_Org_ID = org.AD_Org_ID)"
				+ " INNER JOIN C_BPartner bp ON (hdr.C_BPartner_ID = bp.C_BPartner_ID)"
				+ " INNER JOIN M_InOutLine lin ON (hdr.M_InOut_ID = lin.M_InOut_ID)"
				+ " INNER JOIN M_Product p ON (lin.M_Product_ID = p.M_Product_ID)"
				+ " INNER JOIN C_DocType dt ON (hdr.C_DocType_ID = dt.C_DocType_ID AND dt.DocBaseType IN ('MMR', 'MMS'))"
				+ " FULL JOIN "
				+ (matchTypeFrom == MatchType.PURCHASE_ORDER_VALUE ? "M_MatchPO" : "M_MatchInv")
				+ " m ON (lin.M_InOutLine_ID = m.M_InOutLine_ID) "
				+ "WHERE hdr.DocStatus IN ('CO', 'CL') and dt.issotrx = 'N' "
			;
		}
		return sql;
	}

	public static String getGroupBy(boolean isMatched, int matchType) {
		if (matchType == MatchType.INVOICE_VALUE) {
			return " GROUP BY hdr.C_Invoice_ID,hdr.DocumentNo,hdr.DateInvoiced,bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.C_InvoiceLine_ID,p.Name,lin.M_Product_ID,lin.QtyInvoiced, org.Name, hdr.AD_Org_ID "
				+ "HAVING "
				+ (isMatched ? "0" : "lin.QtyInvoiced")
				+ "<>SUM(COALESCE(mi.Qty,0))"
			;
		}
		else if (matchType == MatchType.PURCHASE_ORDER_VALUE) {
			return " GROUP BY hdr.C_Order_ID,hdr.DocumentNo,hdr.DateOrdered,bp.Name,hdr.C_BPartner_ID,"
				+ " lin.Line,lin.C_OrderLine_ID,p.Name,lin.M_Product_ID,lin.QtyOrdered, org.Name, hdr.AD_Org_ID "
				+ "HAVING "
				+ (isMatched ? "0" : "lin.QtyOrdered")
				+ "<>SUM(COALESCE(mo.Qty,0))";
		}
		// Receipt
		return " GROUP BY hdr.M_InOut_ID,hdr.DocumentNo,hdr.MovementDate,bp.Name,hdr.C_BPartner_ID,"
			+ " lin.Line,lin.M_InOutLine_ID,p.Name,lin.M_Product_ID,lin.MovementQty, org.Name, hdr.AD_Org_ID "
			+ "HAVING "
			+ (isMatched ? "0" : "lin.MovementQty")
			+ "<>SUM(COALESCE(m.Qty,0))"
		;
	}

	public static Matched.Builder convertMatched(ResultSet resultSet) throws SQLException {
		Matched.Builder builder = Matched.newBuilder();
		if (resultSet == null) {
			return builder;
		}

		builder.setId(
				resultSet.getInt("ID")
			)
			.setUuid(
				ValueUtil.validateNull(
					resultSet.getString(I_AD_System.COLUMNNAME_UUID)
				)
			)
			.setHeaderId(
				resultSet.getInt("Header_ID")
			)
			.setHeaderUuid(
				ValueUtil.validateNull(
					resultSet.getString("Header_UUID")
				)
			)
			.setDate(
				ValueUtil.getLongFromTimestamp(
					resultSet.getTimestamp("Date")
				)
			)
			.setDocumentNo(
				ValueUtil.validateNull(
					resultSet.getString(I_C_Invoice.COLUMNNAME_DocumentNo)
				)
			)
			.setLineNo(
				resultSet.getInt(I_C_InvoiceLine.COLUMNNAME_Line)
			)
			.setQuantity(
				ValueUtil.getDecimalFromBigDecimal(
					resultSet.getBigDecimal("Quantity")
				)
			)
			.setProductId(
				resultSet.getInt(I_C_InvoiceLine.COLUMNNAME_M_Product_ID)
			)
			.setProductName(
				ValueUtil.validateNull(
					resultSet.getString("M_Product_Name")
				)
			)
			.setVendorId(
				resultSet.getInt(I_C_Invoice.COLUMNNAME_C_BPartner_ID)
			)
			.setVendorName(
				ValueUtil.validateNull(
					resultSet.getString("C_BPartner_Name")
				)
			)
		;

		return builder;
	}

	@Override
	public void listMatchedFrom(ListMatchedFromRequest request, StreamObserver<ListMatchedFromResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListMatchedFromResponse.Builder builder = listMatchedFrom(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListMatchedFromResponse.Builder listMatchedFrom(ListMatchedFromRequest request) {
		boolean isMatched = request.getMatchMode() == MatchMode.MODE_MATCHED;
		int matchFromType = request.getMatchFromTypeValue();
		int matchToType = request.getMatchToTypeValue();

		final String dateColumn = getDateColumn(matchFromType);
		// final String quantityColumn = getQuantityColumn(matchFromType);
		final String sql = getSQL(isMatched, matchFromType, matchToType);
		final String groupBy = getGroupBy(isMatched, matchFromType);

		String whereClause = "";
		if (request.getProductId() > 0) {
			whereClause += " AND lin.M_Product_ID = " + request.getProductId();
		}
		if (request.getVendorId() > 0) {
			whereClause += " AND hdr.C_BPartner_ID = " + request.getVendorId();
		}

		// Date filter
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(request.getDateFrom());
		Timestamp dateTo = ValueUtil.getTimestampFromLong(request.getDateTo());
		if (dateFrom != null && dateTo != null) {
			whereClause += " AND " + dateColumn + " BETWEEN " + DB.TO_DATE(dateFrom)
				+ " AND " + DB.TO_DATE(dateTo)
			;
		} else if (dateFrom != null) {
			whereClause += " AND " + dateColumn + " >= " + DB.TO_DATE(dateFrom);
		} else if (dateTo != null) {
			whereClause += " AND " + dateColumn + " <= " + DB.TO_DATE(dateTo);
		}

		int recordCount = 0;

		final String sqlWithAccess = MRole.getDefault().addAccessSQL(
			sql + whereClause,
			"hdr",
			MRole.SQL_FULLYQUALIFIED,
			MRole.SQL_RO
		) + groupBy;

		ListMatchedFromResponse.Builder builderList = ListMatchedFromResponse.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlWithAccess, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				recordCount++;

				Matched.Builder matchedBuilder = convertMatched(rs);
				builderList.addRecords(matchedBuilder);
			}
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		builderList.setRecordCount(recordCount);

		return builderList;
	}



	@Override
	public void listMatchedTo(ListMatchedToRequest request, StreamObserver<ListMatchedToResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListMatchedToResponse.Builder builder = listMatchedTo(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListMatchedToResponse.Builder listMatchedTo(ListMatchedToRequest request) {
		boolean isMatched = request.getMatchMode() == MatchMode.MODE_MATCHED;
		int matchFromType = request.getMatchFromTypeValue();
		int matchToType = request.getMatchToTypeValue();
		int matchFromSelectedId = request.getMatchFromSelectedId();

		final String dateColumn = getDateColumn(matchFromType);
		// final String quantityColumn = getQuantityColumn(matchFromType);
		final String sql = getSQL(isMatched, matchFromType, matchToType);
		final String groupBy = getGroupBy(isMatched, matchFromType);

		String whereClause = "";
		if (request.getProductId() > 0) {
			whereClause += " AND lin.M_Product_ID = " + request.getProductId();
		}
		if (request.getVendorId() > 0) {
			whereClause += " AND hdr.C_BPartner_ID = " + request.getVendorId();
		}

		// Date filter
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(request.getDateFrom());
		Timestamp dateTo = ValueUtil.getTimestampFromLong(request.getDateTo());
		if (dateFrom != null && dateTo != null) {
			whereClause += " AND " + dateColumn + " BETWEEN " + DB.TO_DATE(dateFrom)
				+ " AND " + DB.TO_DATE(dateTo)
			;
		} else if (dateFrom != null) {
			whereClause += " AND " + dateColumn + " >= " + DB.TO_DATE(dateFrom);
		} else if (dateTo != null) {
			whereClause += " AND " + dateColumn + " <= " + DB.TO_DATE(dateTo);
		}

		if (request.getIsSameQuantity()) {
			final String quantityColumn = getQuantityColumn(matchFromType);
			Matched.Builder matchedFromSelected = getMatchedSelectedFrom(matchFromSelectedId, isMatched, matchFromType, matchToType);
			BigDecimal quantity = ValueUtil.getBigDecimalFromDecimal(
				matchedFromSelected.getQuantity()
			);
			whereClause += " AND " + quantityColumn + " = " + quantity;
		}

		int recordCount = 0;

		final String sqlWithAccess = MRole.getDefault().addAccessSQL(
			sql + whereClause,
			"hdr",
			MRole.SQL_FULLYQUALIFIED,
			MRole.SQL_RO
		) + groupBy;

		ListMatchedToResponse.Builder builderList = ListMatchedToResponse.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlWithAccess, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				recordCount++;

				Matched.Builder matchedBuilder = convertMatched(rs);
				builderList.addRecords(matchedBuilder);
			}
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		builderList.setRecordCount(recordCount);

		return builderList;
	}

	public Matched.Builder getMatchedSelectedFrom(int matchFromSelectedId, boolean isMatched, int matchTypeFrom, int matchTypeTo) {
		Matched.Builder builder = Matched.newBuilder();
		if (matchFromSelectedId <= 0) {
			return builder;
		}

		// Receipt
		String whereClause = " AND lin.M_InOutLine_ID = ";
		if (matchTypeFrom == MatchType.INVOICE_VALUE) {
			whereClause = " AND lin.C_InvoiceLine_ID = ";
		}
		else if (matchTypeFrom == MatchType.PURCHASE_ORDER_VALUE) {
			whereClause = " AND lin.C_OrderLine_ID = ";
		}
		whereClause += matchFromSelectedId;

		final String sql = getSQL(isMatched, matchTypeFrom, matchTypeTo);
		final String groupBy = getGroupBy(isMatched, matchTypeFrom);

		final String sqlWithAccess = MRole.getDefault().addAccessSQL(
			sql + whereClause,
			"hdr",
			MRole.SQL_FULLYQUALIFIED,
			MRole.SQL_RO
		) + groupBy;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlWithAccess, null);
			rs = pstmt.executeQuery();

			if (rs.next()) {
				builder = convertMatched(rs);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return builder;
	}

	@Override
	public void process(ProcessRequest request, StreamObserver<ProcessResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessResponse.Builder builder = process(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ProcessResponse.Builder process(ProcessRequest request) {
		if (request.getMatchFromSelectedId() <= 0) {
			throw new AdempiereException("");
		}

		AtomicReference<String> atomicStatus = new AtomicReference<String>();

		Trx.run(transactionName -> {
			Properties context = Env.getCtx();

			atomicStatus.set("");
		});

		return ProcessResponse.newBuilder()
			.setMessage(
				ValueUtil.validateNull(
					atomicStatus.get()
				)
			)
		;
	}


}
