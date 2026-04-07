/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.spin.eca56.process;

import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.eca56.util.queue.ApplicationDictionary;
import org.spin.queue.util.QueueLoader;

/** Generated Process for (Export Current Role Accesses)
 *  @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 *  @version Release 3.9.4
 */
public class ExportCurrentRoleAccesses extends ExportCurrentRoleAccessesAbstract
{
	@Override
	protected void prepare()
	{
		// Valid Record Identifier
		if (this.getRecord_ID() < 0) {
			throw new AdempiereException("@FillMandatory@ @AD_Role_ID@");
		}

		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		exportCurrentRoleAccesses();
		return "Ok";
	}

	private void exportCurrentRoleAccesses() {
		addLog("@AD_Role_ID@");

		MRole role = new Query(
			getCtx(),
			I_AD_Role.Table_Name,
			I_AD_Role.COLUMNNAME_AD_Role_ID + " = ?",
			get_TrxName()
		)
			.setParameters(this.getRecord_ID())
			.first()
		;

		final int clientId = Env.getAD_Client_ID(getCtx());
		if (clientId != role.getAD_Client_ID()) {
			throw new AdempiereException("@AD_Role_ID@ @RecordOfAnotherClient@");
		}

		QueueLoader.getInstance()
			.getQueueManager(ApplicationDictionary.CODE)
			.withEntity(role)
			.addToQueue()
		;
		addLog(
			role.getAD_Role_ID(),
			null,
			null,
			role.getName()
		);
	}

}
