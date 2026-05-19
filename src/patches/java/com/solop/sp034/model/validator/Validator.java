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
package com.solop.sp034.model.validator;

import org.compiere.model.MClient;
import org.compiere.model.MTable;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import com.solop.sp034.util.Changes;
import java.util.UUID;

/**
 * Write here your change comment
 * Please rename this class and package
 * @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">...</a>
 *
 */
public class Validator implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(Validator.class);
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
		//	Add Persistence for IsDefault values
		engine.addModelChange(Changes.Table_SP034_Publishing, this);
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
		if(type == TYPE_AFTER_NEW) {
			if(entity.get_TableName().equals(Changes.Table_SP034_Publishing)) {
				MTable allocationTable = MTable.get(entity.getCtx(), Changes.Table_SP034_Allocation);
				new Query(
					entity.getCtx(),
					Changes.Table_SP034_Allocation,
					"M_Product_ID = ? AND SP034_Publishing_ID IS NULL",
					entity.get_TrxName()
				)
					.setParameters(entity.get_ValueAsInt("M_Product_ID"))
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.getIDsAsList().forEach(allocationId -> {
						PO attributeAllocation = allocationTable.getPO(allocationId, entity.get_TrxName());
						PO publishingAllocation = allocationTable.getPO(0, entity.get_TrxName());
						PO.copyValues(attributeAllocation, publishingAllocation);
						publishingAllocation.set_ValueOfColumn("UUID", UUID.randomUUID().toString());
						publishingAllocation.set_ValueOfColumn("SP034_Publishing_ID", entity.get_ID());
						publishingAllocation.saveEx();
					})
				;
			}
		}
		return null;
	}

	@Override
	public String docValidate(PO entity, int timing) {
		// if(timing == TIMING_BEFORE_COMPLETE) {
		// 	if(entity.get_TableName().equals(I_C_Order.Table_Name)) {
		// 		MOrder order = (MOrder) entity;
		// 		MDocType documentType = MDocType.get(entity.getCtx(), order.getC_DocTypeTarget_ID());
		// 		if(order.isSOTrx()
		// 				&& !order.isReturnOrder()
		// 				&& documentType.get_ValueAsBoolean(Changes.SP034_ML_ItemCondition)) {

		// 		}
		// 	}
		// }
		return null;
	}
}
