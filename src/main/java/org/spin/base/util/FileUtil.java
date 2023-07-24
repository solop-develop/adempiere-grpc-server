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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.compiere.model.MClientInfo;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.model.MADAttachmentReference;
import org.spin.util.AttachmentUtil;

import com.google.protobuf.ByteString;

/**
 * A helper class for Files
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 */
public class FileUtil {

	/**
	 * Convert Name
	 * @param name
	 * @return
	 */
	public static String getValidFileName(String fileName) {
		if(Util.isEmpty(fileName)) {
			return "";
		}
		return fileName
			.replaceAll("[+^:&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$()*#/><]", "")
			.replaceAll(" ", "-")
		;
	}


	/**
	 * Get resource UUID from image id
	 * @param imageId
	 * @return
	 */
	public static String getResourceUuidFromImageId(int imageId) {
		MADAttachmentReference reference = getResourceFromImageId(imageId);
		if(reference == null) {
			return null;
		}
		//	Return uuid
		return reference.getUUID();
	}


	/**
	 * Get Attachment reference from image ID
	 * @param imageId
	 * @return
	 */
	public static MADAttachmentReference getResourceFromImageId(int imageId) {
		int clientId = Env.getAD_Client_ID(Env.getCtx());
		if(!AttachmentUtil.getInstance().isValidForClient(clientId)) {
			return null;
		}
		//	
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), Env.getAD_Client_ID(Env.getCtx()));
		return MADAttachmentReference.getByImageId(
			Env.getCtx(),
			clientInfo.getFileHandler_ID(),
			imageId,
			null
		);
	}


	public static ByteString getByteStringByOutputStream(OutputStream outputStream) {
		ByteArrayOutputStream buffer = (ByteArrayOutputStream) outputStream;
		byte[] bytes = buffer.toByteArray();
		ByteString resultFile = ByteString.copyFrom(bytes);
		return resultFile;
	}

}
