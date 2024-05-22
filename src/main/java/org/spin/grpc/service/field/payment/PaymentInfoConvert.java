package org.spin.grpc.service.field.payment;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.compiere.util.Env;
import org.adempiere.core.domains.models.I_C_Payment;
import org.compiere.model.MPayment;
import org.spin.backend.grpc.field.payment.PaymentInfo;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

public class PaymentInfoConvert {
	public static PaymentInfo.Builder convertPaymentInfo(ResultSet rs) throws SQLException {
		PaymentInfo.Builder builder = PaymentInfo.newBuilder();
		if (rs == null) {
			return builder;
		}

		int paymentId = rs.getInt(
			I_C_Payment.COLUMNNAME_C_Payment_ID
		);

		MPayment payment = new MPayment(Env.getCtx(), paymentId, null);


		builder.setId(
				rs.getInt(
					I_C_Payment.COLUMNNAME_C_Payment_ID
				)
			)
			.setUuid(
				ValueManager.validateNull(
					rs.getString(
                        I_C_Payment.COLUMNNAME_UUID
                    )
				)
			)
			.setDisplayValue(
				ValueManager.validateNull(
					payment.getDisplayValue()
				)
			)
			.setBankAccount(
				ValueManager.validateNull(
					rs.getString(
						"BankAccount"
					)
				)
			)
			.setBusinessPartner(
				ValueManager.validateNull(
					rs.getString("BusinessPartner")
				)
			)
			.setDatePayment(
				TimeManager.convertDateToValue(
					rs.getTimestamp(
						I_C_Payment.COLUMNNAME_DateTrx
					)
				)
			)
			.setDocumentNo(
				ValueManager.validateNull(
					rs.getString(
						I_C_Payment.COLUMNNAME_DocumentNo
					)
				)
			)
			.setCurrency(
				ValueManager.validateNull(
					rs.getString("Currency")
				)
			)
			.setPayAmt(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_C_Payment.COLUMNNAME_PayAmt
					)
				)
			)
			.setConvertedAmount(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal("currencyBase")
				)
			)
			.setDiscountAmt(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_C_Payment.COLUMNNAME_DiscountAmt
					)
				)
			)
			.setWriteOffAmt(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_C_Payment.COLUMNNAME_WriteOffAmt
					)
				)
			)
			
			.setIsAllocated(
				rs.getBoolean(
					I_C_Payment.COLUMNNAME_IsAllocated
				)
			)
			// .setIsDelivered(
			// 	rs.getBoolean(
			// 		"isDelivered"
			// 	)
			// )
			// .setDescription(
			// 	ValueManager.validateNull(
			// 		rs.getString(
			// 			I_C_Payment.COLUMNNAME_Description
			// 		)
			// 	)
			// )
			// .setPoReference(
			// 	ValueManager.validateNull(
			// 		rs.getString(
			// 			I_C_Payment.COLUMNNAME_POReference
			// 		)
			// 	)
			// )
			.setDocumentStatus(
				ValueManager.validateNull(
					// rs.getString(
					// 	I_C_Payment.COLUMNNAME_DocStatus
					// )
					payment.getDocStatusName()
					// I_C_Payment.COLUMNNAME_DocStatus
				)
			)
		;

		return builder;
	}
}
