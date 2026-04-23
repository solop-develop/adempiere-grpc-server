package org.spin.grpc.service.field.in_out;

import org.adempiere.core.domains.models.X_M_InOut;
import org.compiere.model.MBPartner;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.backend.grpc.field.inout.BusinessPartner;
import org.spin.backend.grpc.field.inout.InOutInfo;
import org.spin.base.util.ConvertUtil;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

public class InOutInfoConvert {

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



	public static InOutInfo.Builder convertInOutInfo(int inOutId) {
		InOutInfo.Builder builder = InOutInfo.newBuilder();
		if (inOutId <= 0) {
			return builder;
		}
		MInOut inOut = new MInOut(Env.getCtx(), inOutId, null);
		return convertInOutInfo(inOut);
	}

	public static InOutInfo.Builder convertInOutInfo(MInOut inOut) {
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
		if (inOut.getC_Invoice_ID() > 0) {
			MInvoice invoice = new MInvoice(Env.getCtx(), inOut.getC_Invoice_ID(), null);
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
