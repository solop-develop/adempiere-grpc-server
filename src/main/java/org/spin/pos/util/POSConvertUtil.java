/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   	 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2018-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/

package org.spin.pos.util;

import org.adempiere.core.domains.models.I_C_POS;
import org.compiere.model.MBank;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.spin.backend.grpc.pos.Bank;
import org.spin.backend.grpc.pos.CommandShortcut;
import org.spin.base.util.ValueUtil;

/**
 * This class was created for add all convert methods for POS form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class POSConvertUtil {

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
		builder.setId(bank.getC_Bank_ID())
			.setUuid(
				ValueUtil.validateNull(
					bank.getUUID()
				)
			)
			.setName(
				ValueUtil.validateNull(
					bank.getName()
				)
			)
			.setDescription(
				ValueUtil.validateNull(
					bank.getDescription()
				)
			)
			.setRoutingNo(
				ValueUtil.validateNull(
					bank.getRoutingNo()
				)
			)
			.setSwiftCode(
				ValueUtil.validateNull(
					bank.getSwiftCode()
				)
			)
		;

		return builder;
	}

	public static CommandShortcut.Builder convertCommandShorcut(PO commandShortcut) {
		CommandShortcut.Builder builder = CommandShortcut.newBuilder();
		if (commandShortcut == null) {
			return builder;
		}
		builder.setId(commandShortcut.get_ID())
			.setUuid(
				ValueUtil.validateNull(commandShortcut.get_UUID())
			)
			.setPosId(
				commandShortcut.get_ValueAsInt(I_C_POS.COLUMNNAME_C_POS_ID)
			)
			.setCommand(
				ValueUtil.validateNull(
					commandShortcut.get_ValueAsString("ECA14_Command")
				)
			)
			.setShortcut(
				ValueUtil.validateNull(
					commandShortcut.get_ValueAsString("ECA14_Shortcut")
				)
			)
		;
		return builder;
	}

}
