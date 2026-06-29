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

package org.solop.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRelease;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestType;
import org.compiere.model.MStatus;
import org.compiere.model.Query;

import java.util.List;

/** Generated Process for (Publish Release)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public class PublishRelease extends PublishReleaseAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
		if (getRecord_ID() <= 0)
			throw new AdempiereException("@Record_ID@ @NotFound@");
	}

	@Override
	protected String doIt() throws Exception
	{
		MRelease release = new MRelease(getCtx(), getRecord_ID(), get_TrxName());
		if (release.get_ID() <= 0)
			throw new AdempiereException("@Record_ID@ @NotFound@");
		if (release.isProcessed())
			throw new AdempiereException("@AlreadyPosted@");

		final String where = "R_Release_ID=? AND EXISTS (SELECT 1 FROM R_Status s "
				+ "WHERE s.R_Status_ID=R_Request.R_Status_ID AND s.IsClosed='N')";
		List<MRequest> requests = new Query(getCtx(), MRequest.Table_Name, where, get_TrxName())
				.setParameters(getRecord_ID())
				.setOnlyActiveRecords(true)
				.list();

		int closedCount = 0;
		for (MRequest request : requests) {
			MRequestType requestType = MRequestType.get(getCtx(), request.getR_RequestType_ID());
			if (requestType == null || requestType.get_ID()<=0) {
				throw new AdempiereException("@R_RequestType_ID@ @NotFound@ (R_Request_ID=" + request.get_ID() + ")");
			}

			MStatus closedStatus = new Query(getCtx(), MStatus.Table_Name,
					"R_StatusCategory_ID=? AND IsClosed='Y'", get_TrxName())
					.setParameters(requestType.getR_StatusCategory_ID())
					.setOnlyActiveRecords(true)
					.setOrderBy("SeqNo")
					.first();
			if (closedStatus == null) {
				throw new AdempiereException("@R_Status@ @IsClosed@ @NotFound@");
			}

			request.setR_Status_ID(closedStatus.getR_Status_ID());
			request.saveEx();
			closedCount++;
		}

		release.saveEx();

		return "@Closed@: " + closedCount;
	}
}