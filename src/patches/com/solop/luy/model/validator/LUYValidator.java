/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program.	If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package com.solop.luy.model.validator;

import com.solop.luy.util.LUYUtil;
import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.X_C_TaxGroup;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Util;

/**
 * Write here your change comment
 * Please rename this class and package
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">...</a>
 *
 */
public class LUYValidator implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(LUYValidator.class);
	/** Client */
	private int clientId = -1;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		engine.addModelChange(I_C_BPartner.Table_Name, this);
	}
	
	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}
	
	@Override
	public String modelChange(PO entity, int type) throws Exception {
		if(type == TYPE_BEFORE_NEW
				|| type == TYPE_BEFORE_CHANGE) {
			if(entity.get_TableName().equals(I_C_BPartner.Table_Name)) {
				MBPartner partner = (MBPartner) entity;

				if (partner.is_new()
					|| partner.is_ValueChanged(MBPartner.COLUMNNAME_C_TaxGroup_ID)
					|| partner.is_ValueChanged(MBPartner.COLUMNNAME_TaxID)) {

					if (partner.getC_TaxGroup_ID() > 0 && !Util.isEmpty(partner.getTaxID(), true)) {
						X_C_TaxGroup taxGroup = new X_C_TaxGroup(partner.getCtx(), partner.getC_TaxGroup_ID(), partner.get_TrxName());

						if (taxGroup.getValue().equalsIgnoreCase("RUT")) {
							if (!LUYUtil.validateTaxID(partner.getTaxID().trim())) {
								throw new AdempiereException("@TaxID@ @Invalid@");
							}
						}
					}
				}
			}
		}
		return null;
	}







	@Override
	public String docValidate(PO entity, int timing) {

		return null;
	}
}
