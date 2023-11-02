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
package org.spin.base.util;

import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.spin.backend.grpc.general_ledger.AccountingDocument;
import org.spin.service.grpc.util.value.ValueManager;

/**
 * This class was created for add all convert methods for General Ledger service
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class GeneralLedgerConvertUtil {

	public static AccountingDocument.Builder convertAccountingDocument(int tableId) {
		AccountingDocument.Builder builder = AccountingDocument.newBuilder();
		if (tableId <= 0) {
			return builder;
		}
		MTable table = MTable.get(Env.getCtx(), tableId);
		builder = convertAccountingDocument(table);
		return builder;
	}
	public static AccountingDocument.Builder convertAccountingDocument(MTable table) {
		AccountingDocument.Builder builder = AccountingDocument.newBuilder();
		if (table == null || table.getAD_Table_ID() <= 0) {
			return builder;
		}

		builder.setId(table.getAD_Table_ID())
			.setName(
				ValueManager.validateNull(
					table.getName()
				)
			)
			.setTableName(
				ValueManager.validateNull(
					table.getTableName()
				)
			)
		;

		return builder;
	}
}
