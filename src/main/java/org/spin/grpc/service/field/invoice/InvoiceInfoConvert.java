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
package org.spin.grpc.service.field.invoice;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_InvoicePaySchedule;
import org.compiere.model.MInvoice;
import org.compiere.util.Env;
import org.spin.backend.grpc.field.invoice.InvoiceInfo;
import org.spin.backend.grpc.field.invoice.InvoicePaySchedule;
import org.spin.service.grpc.util.value.BooleanManager;
import org.spin.service.grpc.util.value.NumberManager;
import org.spin.service.grpc.util.value.TimeManager;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Product Info field
 */
public class InvoiceInfoConvert {

	public static InvoiceInfo.Builder convertInvoiceInfo(ResultSet rs) throws SQLException {
		InvoiceInfo.Builder builder = InvoiceInfo.newBuilder();
		if (rs == null) {
			return builder;
		}
		int invoiceId = rs.getInt(
			I_C_Invoice.COLUMNNAME_C_Invoice_ID
		);
		MInvoice invoice = MInvoice.get(Env.getCtx(), invoiceId);

		builder.setId(
				rs.getInt(
					I_C_Invoice.COLUMNNAME_C_Invoice_ID
				)
			)
			.setUuid(
				ValueManager.validateNull(
					rs.getString(
						I_C_Invoice.COLUMNNAME_UUID
					)
				)
			)
			.setDisplayValue(
				ValueManager.validateNull(
					invoice.getDisplayValue()
				)
			)
			.setBusinessPartner(
				ValueManager.validateNull(
					rs.getString("BusinessPartner")
				)
			)
			.setDateInvoiced(
				TimeManager.convertDateToValue(
					rs.getTimestamp(
						I_C_Invoice.COLUMNNAME_DateInvoiced
					)
				)
			)
			.setDocumentNo(
				ValueManager.validateNull(
					rs.getString(
						I_C_Invoice.COLUMNNAME_DocumentNo
					)
				)
			)
			.setCurrency(
				ValueManager.validateNull(
					rs.getString("Currency")
				)
			)
			.setGrandTotal(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_C_Invoice.COLUMNNAME_GrandTotal
					)
				)
			)
			.setConvertedAmount(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal("ConvertedAmount")
				)
			)
			.setOpenAmount(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal("OpenAmt")
				)
			)
			.setPaymentTerm(
				ValueManager.validateNull(
					rs.getString("PaymentTerm")
				)
			)
			.setIsPaid(
				rs.getBoolean(
					I_C_Invoice.COLUMNNAME_IsPaid
				)
			)
			.setIsSalesTransaction(
				rs.getBoolean(
					I_C_Invoice.COLUMNNAME_IsSOTrx
				)
			)
			.setDescription(
				ValueManager.validateNull(
					rs.getString(
						I_C_Invoice.COLUMNNAME_Description
					)
				)
			)
			.setPoReference(
				ValueManager.validateNull(
					I_C_Invoice.COLUMNNAME_POReference
				)
			)
			.setDocumentStatus(
				ValueManager.validateNull(
					invoice.getDocStatusName()
				)
			)
		;

		return builder;
	}


	public static InvoicePaySchedule.Builder convertInvoicePaySchedule(ResultSet rs) throws SQLException {
		InvoicePaySchedule.Builder builder = InvoicePaySchedule.newBuilder();
		if (rs == null) {
			return builder;
		}

		builder.setId(
				rs.getInt(
					I_C_InvoicePaySchedule.COLUMNNAME_C_InvoicePaySchedule_ID
				)
			)
			// .setUuid(
			// 	ValueManager.validateNull(
			// 		rs.getString(
			// 			I_C_InvoicePaySchedule.COLUMNNAME_UUID
			// 		)
			// 	)
			// )
			.setPaymentCount(
				rs.getInt("PaymentCount")
			)
			.setDueDate(
				TimeManager.convertDateToValue(
					rs.getTimestamp(
						I_C_InvoicePaySchedule.COLUMNNAME_DueDate
					)
				)
			)
			.setCurrency(
				ValueManager.validateNull(
					rs.getString("Currency")
				)
			)
			.setGrandTotal(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal(
						I_C_Invoice.COLUMNNAME_GrandTotal
					)
				)
			)
			.setConvertedAmount(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal("ConvertedAmount")
				)
			)
			.setOpenAmount(
				NumberManager.getBigDecimalToString(
					rs.getBigDecimal("OpenAmt")
				)
			)
			.setIsPaid(
				BooleanManager.getBooleanFromString(
					rs.getString("IsPaid")
				)
			)
		;
		return builder;
	}
}
