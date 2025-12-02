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
package com.solop.sp007.model.validator;

import com.solop.sp007.util.AssignmentSummary;
import com.solop.sp007.util.TimeSheetControl;
import org.adempiere.core.domains.models.I_S_ResourceAssignment;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * A validator for create additional line of sales order wjen it have a resource
 * @author Yamel Senih ysenih@erpya.com
 *
 */
public class TimeSheet implements ModelValidator {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(TimeSheet.class);
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
		engine.addModelChange(I_S_ResourceAssignment.Table_Name, this);
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
		if(type == TYPE_BEFORE_NEW) {
			TimeSheetControl.autoApproveTimeSheet((MResourceAssignment) entity);
		} else if(type == TYPE_AFTER_NEW
				|| type == TYPE_AFTER_CHANGE) {
			if(entity.get_TableName().equals(I_S_ResourceAssignment.Table_Name)) {
				//	For Resource Assignment
				if(type == TYPE_AFTER_NEW || entity.is_ValueChanged(I_S_ResourceAssignment.COLUMNNAME_Qty)) {
					TimeSheetControl.validateQuantityFromResourceType((MResourceAssignment) entity);
					TimeSheetControl.updateSummaryAssignment((MResourceAssignment) entity);
				}
				if(entity.is_ValueChanged(I_S_ResourceAssignment.COLUMNNAME_IsConfirmed)) {
					AssignmentSummary.newInstance((MResourceAssignment) entity).updateAssignmentSummary();
				}
			}
		} else if(type == TYPE_AFTER_DELETE) {
			AssignmentSummary.newInstance((MResourceAssignment) entity).updateAssignmentSummary();
		} else if(type == TYPE_BEFORE_CHANGE) {
			if(((MResourceAssignment) entity).isConfirmed()){
				throw new AdempiereException("@CSP02.NotModifyConfirmed@");
			}
		}
		if(entity.get_TableName().equals(I_S_ResourceAssignment.Table_Name)) {
			if (type == TYPE_BEFORE_NEW || (type == TYPE_BEFORE_CHANGE
					&& (entity.is_ValueChanged(MResourceAssignment.COLUMNNAME_AssignDateFrom)
					|| entity.is_ValueChanged(MResourceAssignment.COLUMNNAME_AssignDateTo)))) {

				MResourceAssignment resourceAssignment = (MResourceAssignment) entity;
				BigDecimal quantityEntered = calculateHours(resourceAssignment.getAssignDateFrom(), resourceAssignment.getAssignDateTo());
				resourceAssignment.setQty(quantityEntered);
			}
		}
		return null;
	}
	private BigDecimal calculateHours(Timestamp start, Timestamp end) {
		long difference = end.getTime() - start.getTime();
		return BigDecimal.valueOf(difference / (double)TimeUnit.HOURS.toMillis(1)).setScale(2, RoundingMode.DOWN);
	}

	@Override
	public String docValidate(PO entity, int timing) {
		return null;
	}
}
