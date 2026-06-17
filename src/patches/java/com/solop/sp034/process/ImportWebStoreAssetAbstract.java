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

package com.solop.sp034.process;

import org.compiere.process.SvrProcess;

/** Generated Process for (Import Web Store Asset)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.4
 */
public abstract class ImportWebStoreAssetAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SP034_ImportWebStoreAsset";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Import Web Store Asset";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54971;
	/**	Parameter Name for Web Store	*/
	public static final String W_STORE_ID = "W_Store_ID";
	/**	Parameter Name for Application Registration	*/
	public static final String AD_APPREGISTRATION_ID = "AD_AppRegistration_ID";
	/**	Parameter Name for Force	*/
	public static final String ISFORCE = "IsForce";
	/**	Parameter Value for Web Store	*/
	private int storeId;
	/**	Parameter Value for Application Registration	*/
	private int appRegistrationId;
	/**	Parameter Value for Force	*/
	private boolean isForce;

	@Override
	protected void prepare() {
		storeId = getParameterAsInt(W_STORE_ID);
		appRegistrationId = getParameterAsInt(AD_APPREGISTRATION_ID);
		isForce = getParameterAsBoolean(ISFORCE);
	}

	/**	 Getter Parameter Value for Web Store	*/
	protected int getStoreId() {
		return storeId;
	}

	/**	 Setter Parameter Value for Web Store	*/
	protected void setStoreId(int storeId) {
		this.storeId = storeId;
	}

	/**	 Getter Parameter Value for Application Registration	*/
	protected int getAppRegistrationId() {
		return appRegistrationId;
	}

	/**	 Setter Parameter Value for Application Registration	*/
	protected void setAppRegistrationId(int appRegistrationId) {
		this.appRegistrationId = appRegistrationId;
	}

	/**	 Getter Parameter Value for Force	*/
	protected boolean isForce() {
		return isForce;
	}

	/**	 Setter Parameter Value for Force	*/
	protected void setIsForce(boolean isForce) {
		this.isForce = isForce;
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