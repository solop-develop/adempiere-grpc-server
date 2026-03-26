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
package org.spin.pos.service.bank;

import org.compiere.model.MBank;
import org.compiere.util.Env;
import org.spin.backend.grpc.pos.Bank;
import org.spin.service.grpc.util.value.TextManager;

public class BankConvertUtil {

	public static Bank.Builder convertBank(int bankId) {
		if (bankId <= 0) {
			return Bank.newBuilder();
		}
		MBank bank = MBank.get(Env.getCtx(), bankId);
		return convertBank(bank);
	}
	public static Bank.Builder convertBank(MBank bank) {
		Bank.Builder builder = Bank.newBuilder();
		if (bank == null) {
			return builder;
		}
		builder.setId(
				bank.getC_Bank_ID()
			)
			.setName(
				TextManager.getValidString(
					bank.getName()
				)
			)
			.setDescription(
				TextManager.getValidString(
					bank.getDescription()
				)
			)
			.setRoutingNo(
				TextManager.getValidString(
					bank.getRoutingNo()
				)
			)
			.setSwiftCode(
				TextManager.getValidString(
					bank.getSwiftCode()
				)
			)
		;

		return builder;
	}

}
