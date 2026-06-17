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

import com.solop.sp034.support.IWebStoreAsset;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MStore;
import org.spin.model.MADAppRegistration;
import org.spin.util.support.AppSupportHandler;
import org.spin.util.support.IAppSupport;

/** Generated Process for (Import Web Store Asset)
 *  @author ADempiere (generated)
 *  @version Release 3.9.4
 */
public class ImportWebStoreAsset extends ImportWebStoreAssetAbstract
{
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		if (getStoreId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @W_Store_ID@");
		}
		if (getAppRegistrationId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @AD_AppRegistration_ID@");
		}
		MStore store = MStore.get(getCtx(), getStoreId());
		if (store == null || store.getW_Store_ID() <= 0) {
			throw new AdempiereException("@W_Store_ID@ @NotFound@");
		}
		MADAppRegistration registration = MADAppRegistration.getById(getCtx(), getAppRegistrationId(), get_TrxName());
		IAppSupport support = AppSupportHandler.getInstance().getAppSupport(registration);
		if (support == null
				|| !IWebStoreAsset.class.isAssignableFrom(support.getClass())) {
			throw new AdempiereException("@SP034_WSA_InvalidRegistration@");
		}
		return ((IWebStoreAsset) support).call(store, isForce());
	}
}