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
import org.compiere.model.MRequestDelivery;

import java.sql.Timestamp;

/** Generated Process for (Reject Delivery)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public class RejectDelivery extends RejectDeliveryAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		int id = getRequestDeliveryId();
		if (id <= 0)
			id = getRecord_ID();
		if (id <= 0)
			throw new AdempiereException("@R_RequestDelivery_ID@ @NotFound@");

		MRequestDelivery delivery = new MRequestDelivery(getCtx(), id, get_TrxName());
		if (delivery.get_ID() <= 0)
			throw new AdempiereException("@R_RequestDelivery_ID@ @NotFound@");
		if (delivery.isRejected())
			throw new AdempiereException("@AlreadyRejected@");

		delivery.setIsRejected(true);
		delivery.setDateRejected(new Timestamp(System.currentTimeMillis()));
		delivery.setRejectionReason(getRejectionReason());
		delivery.setRejectedBy_ID(getAD_User_ID());
		delivery.saveEx();

		return "@OK@";
	}
}