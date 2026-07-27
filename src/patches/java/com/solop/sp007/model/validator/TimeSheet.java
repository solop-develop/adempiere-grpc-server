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
import org.compiere.model.*;
import org.compiere.util.CLogger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Optional;
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
			if (entity.get_TableName().equals(I_S_ResourceAssignment.Table_Name)) {
				MResourceAssignment resourceAssignment = (MResourceAssignment) entity;
				copyMissingValuesFromRequest(resourceAssignment);

			}
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

			MResourceAssignment resourceAssignment = (MResourceAssignment) entity;
			if (resourceAssignment.getQty().signum() == 0 && (resourceAssignment.getAssignDateFrom() == null || resourceAssignment.getAssignDateTo() == null)) {
				throw new AdempiereException("@Qty@, @AssignDateFrom@ @or@ @AssignDateTo@ @NotFound@");
			}
			if (resourceAssignment.getAssignDateFrom() != null && resourceAssignment.getAssignDateTo() != null
				&& resourceAssignment.getAssignDateFrom().after(resourceAssignment.getAssignDateTo())) {
				throw new AdempiereException("@AssignDateFrom@ (" + resourceAssignment.getAssignDateFrom() + ") > @AssignDateTo@ ("+resourceAssignment.getAssignDateTo() + ")");
			}

			if (type == TYPE_BEFORE_NEW || type == TYPE_BEFORE_CHANGE
				&& (entity.is_ValueChanged(MResourceAssignment.COLUMNNAME_AssignDateFrom) || entity.is_ValueChanged(MResourceAssignment.COLUMNNAME_AssignDateTo))) {
				if (resourceAssignment.getQty().signum() == 0) {
					BigDecimal quantityEntered = calculateHours(resourceAssignment.getAssignDateFrom(), resourceAssignment.getAssignDateTo());
					resourceAssignment.setQty(quantityEntered);
				}
			}

			//	Incrementally adjust R_Request.TimeSpent by this record's contribution
			//	(Qty when active, else zero) instead of re-summing all assignments.
			if (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE || type == TYPE_AFTER_DELETE) {
				int requestId = entity.get_ValueAsInt("R_Request_ID");
				BigDecimal qty = Optional.ofNullable(resourceAssignment.getQty()).orElse(BigDecimal.ZERO);
				if (type == TYPE_AFTER_NEW) {
					BigDecimal contribution = resourceAssignment.isActive() ? qty : BigDecimal.ZERO;
					TimeSheetControl.adjustRequestTimeSpent(entity.getCtx(), requestId, contribution, entity.get_TrxName());
				} else if (type == TYPE_AFTER_DELETE) {
					BigDecimal contribution = resourceAssignment.isActive() ? qty : BigDecimal.ZERO;
					TimeSheetControl.adjustRequestTimeSpent(entity.getCtx(), requestId, contribution.negate(), entity.get_TrxName());
				} else { //	TYPE_AFTER_CHANGE
					BigDecimal newContribution = resourceAssignment.isActive() ? qty : BigDecimal.ZERO;
					int oldRequestId = entity.is_ValueChanged("R_Request_ID")
							? entity.get_ValueOldAsInt("R_Request_ID") : requestId;
					BigDecimal oldQty = entity.is_ValueChanged(MResourceAssignment.COLUMNNAME_Qty)
							? (BigDecimal) entity.get_ValueOld(MResourceAssignment.COLUMNNAME_Qty) : qty;
					oldQty = Optional.ofNullable(oldQty).orElse(BigDecimal.ZERO);
					boolean oldActive = entity.is_ValueChanged("IsActive") ? !resourceAssignment.isActive() : resourceAssignment.isActive();
					BigDecimal oldContribution = oldActive ? oldQty : BigDecimal.ZERO;
					if (oldRequestId == requestId) {
						TimeSheetControl.adjustRequestTimeSpent(entity.getCtx(), requestId, newContribution.subtract(oldContribution), entity.get_TrxName());
					} else {
						TimeSheetControl.adjustRequestTimeSpent(entity.getCtx(), oldRequestId, oldContribution.negate(), entity.get_TrxName());
						TimeSheetControl.adjustRequestTimeSpent(entity.getCtx(), requestId, newContribution, entity.get_TrxName());
					}
				}
			}
		}
		return null;
	}
	private BigDecimal calculateHours(Timestamp start, Timestamp end) {
		long difference = end.getTime() - start.getTime();
		return BigDecimal.valueOf(difference / (double)TimeUnit.HOURS.toMillis(1)).setScale(2, RoundingMode.DOWN);
	}

	/**
	 * Copy the fields shared between S_ResourceAssignment and R_Request into the assignment,
	 * but only for those the assignment does not already have a value set.
	 * @param resourceAssignment the assignment to complete with its request's data
	 */
	private void copyMissingValuesFromRequest(MResourceAssignment resourceAssignment) {
		int requestId = resourceAssignment.getR_Request_ID();
		if (requestId <= 0) {
			return;
		}
		MRequest request = new MRequest(resourceAssignment.getCtx(), requestId, resourceAssignment.get_TrxName());

		if (resourceAssignment.getAD_Org_ID() <= 0) {
			resourceAssignment.setAD_Org_ID(request.getAD_Org_ID());
		}
		if (resourceAssignment.getC_BPartner_ID() <= 0) {
			resourceAssignment.setC_BPartner_ID(request.getC_BPartner_ID());
		}
		if (resourceAssignment.getC_Project_ID() <= 0) {
			resourceAssignment.setC_Project_ID(request.getC_Project_ID());
		}
		if (resourceAssignment.getS_Contract_ID() <= 0) {
			resourceAssignment.setS_Contract_ID(request.getS_Contract_ID());
		}
		if (resourceAssignment.getC_Activity_ID() <= 0) {
			resourceAssignment.setC_Activity_ID(request.getC_Activity_ID());
		}
		if (resourceAssignment.getC_Campaign_ID() <= 0) {
			resourceAssignment.setC_Campaign_ID(request.getC_Campaign_ID());
		}
		if (resourceAssignment.getUser1_ID() <= 0) {
			resourceAssignment.setUser1_ID(request.getUser1_ID());
		}
		if (resourceAssignment.getUser2_ID() <= 0) {
			resourceAssignment.setUser2_ID(request.getUser2_ID());
		}
		if (resourceAssignment.getUser3_ID() <= 0) {
			resourceAssignment.setUser3_ID(request.getUser3_ID());
		}
		if (resourceAssignment.getUser4_ID() <= 0) {
			resourceAssignment.setUser4_ID(request.getUser4_ID());
		}
	}

	@Override
	public String docValidate(PO entity, int timing) {
		return null;
	}
}
