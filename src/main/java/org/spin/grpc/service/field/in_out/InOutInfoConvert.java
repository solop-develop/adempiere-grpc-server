package org.spin.grpc.service.field.in_out;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.X_M_InOut;
import org.compiere.model.MBPartner;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.field.inout.BusinessPartner;
import org.spin.backend.grpc.field.inout.InOutInfo;
import org.spin.base.util.ConvertUtil;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

public class InOutInfoConvert {

	private static final CLogger log = CLogger.getCLogger(InOutInfoConvert.class);

	public static BusinessPartner.Builder convertBusinessPartner(int businessPartnerId) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (businessPartnerId <= 0) {
			return builder;
		}
		MBPartner businessPartner = new MBPartner(Env.getCtx(), businessPartnerId, null);
		return convertBusinessPartner(businessPartner);
	}
	public static BusinessPartner.Builder convertBusinessPartner(MBPartner businessPartner) {
		BusinessPartner.Builder builder = BusinessPartner.newBuilder();
		if (businessPartner == null || businessPartner.getC_BPartner_ID() <= 0) {
			return builder;
		}
		builder
			.setId(
				businessPartner.getC_BPartner_ID()
			)
			.setUuid(
				TextManager.getValidString(
					businessPartner.getUUID()
				)
			)
			.setValue(
				TextManager.getValidString(
					businessPartner.getValue()
				)
			)
			.setDisplayValue(
				TextManager.getValidString(
					businessPartner.getDisplayValue()
				)
			)
			.setTaxId(
				TextManager.getValidString(
					businessPartner.getTaxID()
				)
			)
			.setName(
				TextManager.getValidString(
					businessPartner.getName()
				)
			)
			.setDescription(
				TextManager.getValidString(
					businessPartner.getDescription()
				)
			)
		;
		return builder;
	}



	/**
	 * Invoices related to a delivery
	 * The direct invoice (M_InOut.C_Invoice_ID, invoice-before-delivery flow) is
	 * listed first, then the invoices that invoiced the delivery lines
	 * (C_InvoiceLine.M_InOutLine_ID), ordered by most recent. Distinct list.
	 * @param inOut delivery/receipt
	 * @return ordered distinct list of related C_Invoice_ID
	 */
	public static List<Integer> getRelatedInvoiceIds(MInOut inOut) {
		List<Integer> invoiceIds = new ArrayList<Integer>();
		if (inOut == null || inOut.getM_InOut_ID() <= 0) {
			return invoiceIds;
		}
		// Direct invoice on the delivery header
		if (inOut.getC_Invoice_ID() > 0) {
			invoiceIds.add(inOut.getC_Invoice_ID());
		}
		// Invoices that invoiced the delivery lines
		final String sql = "SELECT cil.C_Invoice_ID "
			+ "FROM C_InvoiceLine AS cil "
			+ "INNER JOIN M_InOutLine AS iol ON (iol.M_InOutLine_ID = cil.M_InOutLine_ID) "
			+ "INNER JOIN C_Invoice AS inv ON (inv.C_Invoice_ID = cil.C_Invoice_ID) "
			+ "WHERE iol.M_InOut_ID = ? "
			+ "GROUP BY cil.C_Invoice_ID, inv.DateInvoiced, inv.Created "
			+ "ORDER BY inv.DateInvoiced DESC, inv.Created DESC"
		;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, inOut.getM_InOut_ID());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int invoiceId = rs.getInt(1);
				if (invoiceId > 0 && !invoiceIds.contains(invoiceId)) {
					invoiceIds.add(invoiceId);
				}
			}
		} catch (SQLException e) {
			log.log(java.util.logging.Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return invoiceIds;
	}



	public static InOutInfo.Builder convertInOutInfo(int inOutId) {
		InOutInfo.Builder builder = InOutInfo.newBuilder();
		if (inOutId <= 0) {
			return builder;
		}
		MInOut inOut = new MInOut(Env.getCtx(), inOutId, null);
		return convertInOutInfo(inOut);
	}

	public static InOutInfo.Builder convertInOutInfo(MInOut inOut) {
		// Single view: show the most relevant related invoice (direct, else derived by line)
		List<Integer> invoiceIds = getRelatedInvoiceIds(inOut);
		int displayInvoiceId = invoiceIds.isEmpty() ? 0 : invoiceIds.get(0);
		return convertInOutInfo(inOut, displayInvoiceId);
	}

	/**
	 * Convert a delivery showing a specific related invoice.
	 * When a delivery relates to several invoices the list is expanded one row
	 * per invoice, each built with a different displayInvoiceId.
	 * @param inOut delivery/receipt
	 * @param displayInvoiceId C_Invoice_ID to show (0 = none)
	 * @return builder
	 */
	public static InOutInfo.Builder convertInOutInfo(MInOut inOut, int displayInvoiceId) {
		InOutInfo.Builder builder = InOutInfo.newBuilder();
		if (inOut == null || inOut.getM_InOut_ID() <= 0) {
			return builder;
		}
		builder
			.setId(
				inOut.get_ID()
			)
			.setUuid(
				TextManager.getValidString(
					inOut.getUUID()
				)
			)
			.setDocumentNo(
				TextManager.getValidString(
					inOut.getDocumentNo()
				)
			)
			.setDisplayValue(
				TextManager.getValidString(
					inOut.getDisplayValue()
				)
			)
			.setMovementDate(
				TimeManager.getProtoTimestampFromTimestamp(
					inOut.getMovementDate()
				)
			)
			.setMovementDateFormatted(
				TextManager.getValidString(
					TimeManager.getDateDisplayValue(
						inOut.getMovementDate()
					)
				)
			)
			.setIsSalesTransaction(
				inOut.isSOTrx()
			)
			.setDescription(
				TextManager.getValidString(
					inOut.getDescription()
				)
			)
			.setOrderReference(
				TextManager.getValidString(
					inOut.getPOReference()
				)
			)
			.setShipDate(
				TimeManager.getProtoTimestampFromTimestamp(
					inOut.getShipDate()
				)
			)
			.setShipDateFormatted(
				TextManager.getValidString(
					TimeManager.getDateDisplayValue(
						inOut.getShipDate()
					)
				)
			)
			.setTrakingNo(
				TextManager.getValidString(
					inOut.getTrackingNo()
				)
			)
			.setIsDropShipment(
				inOut.isDropShip()
			)
		;

		if (inOut.getC_BPartner_ID() > 0) {
			builder.setBusinessPartner(
				convertBusinessPartner(inOut.getC_BPartner_ID())
			);
		}
		if (!Util.isEmpty(inOut.getDocStatus(), true)) {
			builder.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					X_M_InOut.DOCSTATUS_AD_Reference_ID,
					inOut.getDocStatus()
				)
			);
		}
		if (inOut.getDropShip_BPartner_ID() > 0) {
			builder.setDropShipmentPartner(
				convertBusinessPartner(inOut.getDropShip_BPartner_ID())
			);
		}

		// Sales Order of the delivery, the order is the reliable
		// parent even when the direct invoice reference is empty.
		if (inOut.getC_Order_ID() > 0) {
			MOrder order = new MOrder(Env.getCtx(), inOut.getC_Order_ID(), null);
			builder.setOrderNo(
					TextManager.getValidString(
						order.getDocumentNo()
					)
				)
				.setOrderDate(
					TimeManager.getProtoTimestampFromTimestamp(
						order.getDateOrdered()
					)
				)
				.setOrderDateFormatted(
					TextManager.getValidString(
						TimeManager.getDateDisplayValue(
							order.getDateOrdered()
						)
					)
				)
			;
		}

		// Invoice, direct link or derived from the delivery lines.
		if (displayInvoiceId > 0) {
			MInvoice invoice = new MInvoice(Env.getCtx(), displayInvoiceId, null);
			builder.setInvoiceNo(
					TextManager.getValidString(
						invoice.getDocumentNo()
					)
				)
				.setInvoiceDate(
					TimeManager.getProtoTimestampFromTimestamp(
						invoice.getDateInvoiced()
					)
				)
				.setInvoiceDateFormatted(
					TextManager.getValidString(
						TimeManager.getDateDisplayValue(
							invoice.getDateInvoiced()
						)
					)
				)
			;
		}

		return builder;
	}

}
