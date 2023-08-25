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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBank;
import org.compiere.util.Env;

/**
 * A util class for change values for documents
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 */
public class BankManagement {

	public static MBank validateAndGetBank(int bankId) {
		if (bankId <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_Bank_ID@");
		}
		MBank bank = MBank.get(Env.getCtx(), bankId);
		if (bank == null || bank.getC_Bank_ID() <= 0) {
			throw new AdempiereException("@C_Bank_ID@ @NotFound@");
		}
		return bank;
	}

}
