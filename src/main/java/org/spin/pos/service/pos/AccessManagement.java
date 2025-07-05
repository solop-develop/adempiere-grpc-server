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

package org.spin.pos.service.pos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPOS;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.pos.util.ColumnsAdded;
import org.spin.service.grpc.util.value.NumberManager;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Seller Management for backend of Point Of Sales form
 */
public class AccessManagement {

	/**
	 * Get Boolean value from POS
	 * @param pos
	 * @param userId
	 * @param columnName
	 * @return
	 */
	public static boolean getBooleanValueFromPOS(MPOS pos, int userId, String columnName) {
		PO userAllocated = AccessManagement.getUserAllowed(Env.getCtx(), pos.getC_POS_ID(), userId, null);
		if (userAllocated != null) {
			if (userAllocated.get_ColumnIndex(columnName) >= 0) {
				return userAllocated.get_ValueAsBoolean(columnName);
			}
		}
		// if column exists in C_POS
		if (pos.get_ColumnIndex(columnName) >= 0) {
			return pos.get_ValueAsBoolean(columnName);
		}
		return false;
	}

	/**
	 * Get Integer value from pos
	 * @param pos
	 * @param userId
	 * @param columnName
	 * @return
	 */
	public static int getIntegerValueFromPOS(MPOS pos, int userId, String columnName) {
		PO userAllocated = AccessManagement.getUserAllowed(Env.getCtx(), pos.getC_POS_ID(), userId, null);
		if (userAllocated != null) {
			if (userAllocated.get_ColumnIndex(columnName) >= 0) {
				return userAllocated.get_ValueAsInt(columnName);
			}
		}
		if (pos.get_ColumnIndex(columnName) >= 0) {
			return pos.get_ValueAsInt(columnName);
		}
		return -1;
	}

	/**
	 * Get BigDecimal value from pos
	 * @param pos
	 * @param userId
	 * @param columnName
	 * @return
	 */
	public static BigDecimal getBigDecimalValueFromPOS(MPOS pos, int userId, String columnName) {
		PO userAllocated = AccessManagement.getUserAllowed(Env.getCtx(), pos.getC_POS_ID(), userId, null);
		if (userAllocated != null) {
			if (userAllocated.get_ColumnIndex(columnName) >= 0) {
				return Optional.ofNullable(
					NumberManager.getBigDecimalFromObject(
						userAllocated.get_Value(columnName)
					)
				).orElse(BigDecimal.ZERO);
			}
		}
		if (pos.get_ColumnIndex(columnName) >= 0) {
			return Optional.ofNullable(
				NumberManager.getBigDecimalFromObject(
					pos.get_Value(columnName)
				)
			).orElse(BigDecimal.ZERO);
		}
		return BigDecimal.ZERO;
	}


	/**
	 * Validate if is allowed user
	 * @param context
	 * @param userId
	 * @param transactionName
	 * @return
	 */
	public static PO getUserAllowed(Properties context, int posId, int userId, String transactionName) {
		return new Query(
			context,
			"C_POSSellerAllocation",
			"C_POS_ID = ? AND SalesRep_ID = ?",
			transactionName
		)
			.setParameters(posId, userId)
			.setOnlyActiveRecords(true)
			.setClient_ID()
			.first()
		;
	}


	/**
	 * Just get supervisor access for evaluate based on POS ID, PIN and Requested Access
	 * @param posId
	 * @param userId
	 * @param pin
	 * @param requestedAccess
	 * @return
	 */
	public static PO getSupervisorAccessFromPIN(int posId, int userId, String pin, String requestedAccess, String requestAmount ) {
		if (Util.isEmpty(requestedAccess, true)) {
			return null;
		}

		MPOS pos = POS.validateAndGetPOS(posId, false);

		MTable table = MTable.get(Env.getCtx(), "C_POSSellerAllocation");
		if (table == null) {
			throw new AdempiereException("@TableName@ @NotFound@ C_POSSellerAllocation");
		}


		StringBuffer whereClause = new StringBuffer()
			.append(
				"C_POS_ID = ? "
				+ "AND IsAllowsPOSManager = 'Y' "
				+ "AND EXISTS(SELECT 1 FROM AD_User AS u "
				+ "WHERE u.AD_User_ID = C_POSSellerAllocation.SalesRep_ID "
				+ "AND u.IsActive = 'Y' "
				+ "AND u.UserPIN = ?) "
			)
		;
		List<Object> filtersList = new ArrayList<>();
		filtersList.add(pos.getC_POS_ID());
		filtersList.add(pin);

		if(table.getColumn(requestedAccess) != null) {
			whereClause.append("AND ")
				.append(requestedAccess)
				.append("= 'Y' ")
			;
		}

		BigDecimal amountAccess = NumberManager.getBigDecimalFromString(requestAmount);
		if (amountAccess != null && amountAccess.compareTo(Env.ZERO) != 0) {
			String requestedAmountColumnName = getAmountAccessColumnName(requestedAccess);
			if(requestedAmountColumnName != null) {
				whereClause.append("AND (")
					.append(requestedAmountColumnName).append(" >= ? OR ")
					.append(requestedAmountColumnName).append(" = 0")
					.append(")")
				;
				filtersList.add(amountAccess);
			}
		}

		//	Add PIN
		//	Get if exists
		return new Query(
			Env.getCtx(),
			table.getTableName(),
			whereClause.toString(),
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(filtersList)
			.first()
		;
	}

	private static String getAmountAccessColumnName(String requestedAccess) {
		if(requestedAccess == null) {
			return null;
		}
		if(requestedAccess.equals(ColumnsAdded.COLUMNNAME_IsAllowsModifyDiscount)) {
			return ColumnsAdded.COLUMNNAME_MaximumLineDiscountAllowed;
		}
		if(requestedAccess.equals(ColumnsAdded.COLUMNNAME_IsAllowsApplyDiscount)) {
			return ColumnsAdded.COLUMNNAME_MaximumDiscountAllowed;
		}
		if (requestedAccess.equals(ColumnsAdded.COLUMNNAME_IsAllowsApplyShemaDiscount)) {
			return ColumnsAdded.COLUMNNAME_MaximumShemaDiscountAllowed;
		}
		return null;
	}

}
