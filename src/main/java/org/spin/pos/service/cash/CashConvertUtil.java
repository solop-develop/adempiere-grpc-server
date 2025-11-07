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
package org.spin.pos.service.cash;

import org.compiere.model.MBankStatement;
import org.spin.backend.grpc.pos.CashMovements;
import org.spin.base.util.ConvertUtil;
import org.spin.grpc.service.core_functionality.CoreFunctionalityConvert;
import org.spin.service.grpc.util.value.TextManager;
import org.spin.service.grpc.util.value.TimeManager;

public class CashConvertUtil {

	public static CashMovements.Builder convertCashMovements(MBankStatement bankStatement) {
		CashMovements.Builder cashClosing = CashMovements.newBuilder();
		if (bankStatement == null || bankStatement.getC_BankStatement_ID() <= 0) {
			return cashClosing;
		}

		cashClosing
			.setId(
				bankStatement.getC_BankStatement_ID()
			)
			.setUuid(
				TextManager.getValidString(
					bankStatement.getUUID()
				)
			)
			.setDocumentNo(
				TextManager.getValidString(
					bankStatement.getDocumentNo()
				)
			)
			.setDate(
				TimeManager.getProtoTimestampFromTimestamp(
					bankStatement.getStatementDate()
				)
			)
			.setDocumentType(
				CoreFunctionalityConvert.convertDocumentType(
					bankStatement.getC_DocType_ID()
				)
			)
			.setDocumentStatus(
				ConvertUtil.convertDocumentStatus(
					bankStatement.getDocStatus(),
					bankStatement.getDocStatus(),
					bankStatement.getDocStatus()
				)
			)
			.setDescription(
				TextManager.getValidString(
					bankStatement.getDescription()
				)
			)
		;
		return cashClosing;
	}

}
