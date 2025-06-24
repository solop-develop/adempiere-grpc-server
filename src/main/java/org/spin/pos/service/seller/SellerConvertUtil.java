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

package org.spin.pos.service.seller;

import org.compiere.model.MClientInfo;
import org.compiere.model.MUser;
import org.compiere.util.Env;
import org.spin.backend.grpc.pos.AvailableSeller;
import org.spin.model.MADAttachmentReference;
import org.spin.service.grpc.util.value.StringManager;
import org.spin.util.AttachmentUtil;

public class SellerConvertUtil {

	/**
	 * Convert User entity
	 * @param user
	 * @return
	 */
	public static AvailableSeller.Builder convertSeller(MUser user) {
		AvailableSeller.Builder sellerInfo = AvailableSeller.newBuilder();
		if (user == null) {
			return sellerInfo;
		}
		sellerInfo.setId(
				user.getAD_User_ID()
			)
			.setUuid(
				StringManager.getValidString(
					user.getUUID()
				)
			)
			.setName(
				StringManager.getValidString(
					user.getName()
				)
			)
			.setDescription(
				StringManager.getValidString(
					user.getDescription()
				)
			)
			.setComments(
				StringManager.getValidString(
					user.getComments()
				)
			)
		;

		int clientId = Env.getAD_Client_ID(Env.getCtx());
		if(user.getLogo_ID() > 0 && AttachmentUtil.getInstance().isValidForClient(clientId)) {
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), clientId);
			MADAttachmentReference attachmentReference = MADAttachmentReference.getByImageId(
				Env.getCtx(),
				clientInfo.getFileHandler_ID(),
				user.getLogo_ID(),
				null
			);
			if(attachmentReference != null
					&& attachmentReference.getAD_AttachmentReference_ID() > 0) {
				sellerInfo.setImage(
					StringManager.getValidString(
						attachmentReference.getValidFileName()
					)
				);
			}
		}
		return sellerInfo;
	}

}
