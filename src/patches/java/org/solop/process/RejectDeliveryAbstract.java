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

import org.compiere.process.SvrProcess;

/** Generated Process for (Reject Delivery)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class RejectDeliveryAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "RejectDelivery";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Reject Delivery";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54973;
	/**	Parameter Name for Request Delivery	*/
	public static final String R_REQUESTDELIVERY_ID = "R_RequestDelivery_ID";
	/**	Parameter Name for Rejection Reason	*/
	public static final String REJECTIONREASON = "RejectionReason";
	/**	Parameter Value for Request Delivery	*/
	private int requestDeliveryId;
	/**	Parameter Value for Rejection Reason	*/
	private String rejectionReason;

	@Override
	protected void prepare() {
		requestDeliveryId = getParameterAsInt(R_REQUESTDELIVERY_ID);
		rejectionReason = getParameterAsString(REJECTIONREASON);
	}

	/**	 Getter Parameter Value for Request Delivery	*/
	protected int getRequestDeliveryId() {
		return requestDeliveryId;
	}

	/**	 Setter Parameter Value for Request Delivery	*/
	protected void setRequestDeliveryId(int requestDeliveryId) {
		this.requestDeliveryId = requestDeliveryId;
	}

	/**	 Getter Parameter Value for Rejection Reason	*/
	protected String getRejectionReason() {
		return rejectionReason;
	}

	/**	 Setter Parameter Value for Rejection Reason	*/
	protected void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}