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

package com.solop.sp007.process;

import com.solop.sp007.util.ResourceAssignment;
import org.adempiere.core.domains.models.I_C_Project;
import org.adempiere.core.domains.models.I_R_Request;
import org.compiere.model.MProject;
import org.compiere.model.MRequest;
import org.compiere.model.MTable;
import org.compiere.model.PO;

import java.util.List;

/** 
 * 	Generated Process for (Record Hours Worked)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop http://www.solopsoftware.com
 *  @version Release 3.9.4
 */
public class RecordHoursWorked extends RecordHoursWorkedAbstract {
	List<Integer> recordIds;
	
	@Override
	protected void prepare() {
		super.prepare();
		if (isSelection()) {
			recordIds = getSelectionKeys();
		} else if(getRecord_ID() > 0 && getTableName() != null) {
			if(getTableName().equals(I_R_Request.Table_Name)) {
				setRequestId(getRecord_ID());
			} else if(getTableName().equals(I_C_Project.Table_Name)) {
				setProjectId(getRecord_ID());
			} else if(getTableName().equals("S_Contract")) {
				setContractId(getRecord_ID());
			}
			loadDefaultValues();
		}
	}
	
	private void loadDefaultValues() {
		if(getRequestId() > 0) {
			MRequest request = new MRequest(getCtx(), getRequestId(), get_TrxName());
			loadValueFromPO(request);
		} else if(getProjectId() > 0) {
			MProject project = new MProject(getCtx(), getProjectId(), get_TrxName());
			loadValueFromPO(project);
		} else if(getContractId() > 0) {
			MTable contractTable = MTable.get(getCtx(), "S_Contract");
			if(contractTable != null) {
				PO contract = contractTable.getPO(getRecord_ID(), get_TrxName());
				loadValueFromPO(contract);
			}
		}
	}
	
	private void loadValueFromPO(PO request) {
		if(getBPartnerId() <= 0) {
			setBPartnerId(request.get_ValueAsInt("C_BPartner_ID"));
		}
		if(getProjectId() <= 0) {
			setProjectId(request.get_ValueAsInt("C_Project_ID"));
		}
		if(getContractId() <= 0) {
			setContractId(request.get_ValueAsInt("S_Contract_ID"));
		}
		if(getActivityId() <= 0) {
			setActivityId(request.get_ValueAsInt("C_Activity_ID"));
		}
		if(getCampaignId() > 0) {
			setCampaignId(request.get_ValueAsInt("C_Campaign_ID"));
		}
		if(getUser1Id() <= 0) {
			setUser1Id(request.get_ValueAsInt("User1_ID"));
		}
		if(getUser2Id() <= 0) {
			setUser2Id(request.get_ValueAsInt("User2_ID"));
		}
		if(getUser3Id() <= 0) {
			setUser3Id(request.get_ValueAsInt("User3_ID"));
		}
		if(getUser4Id() <= 0) {
			setUser4Id(request.get_ValueAsInt("User4_ID"));
		}
	}

	@Override
	protected String doIt() throws Exception {
		recordIds.forEach(recordId -> {
			if (isSelection()){
				setRequestId(recordId);
				loadDefaultValues();
				setDescription(getSelectionAsString(recordId, "R_Description"));
				setQuantityHours(getSelectionAsBigDecimal(recordId, "R_SP007_QuantityHours"));
				setActivity(getSelectionAsString(recordId, "R_SP007_Activity"));
			}
			ResourceAssignment assignment = ResourceAssignment.newInstance()
					.withOrganizationId(getOrgId())
					.withResourceId(getResourceId())
					.withServiceDate(getServiceDate())
					.withUserId(getAD_User_ID())
					.withQuantity(getQuantityHours())
					.withReason(getActivity())
					.withDescription(getDescription())
					.withTransactionName(get_TrxName())
					;
			if(getBPartnerId() > 0) {
				assignment.withCustomValue("C_BPartner_ID", getBPartnerId());
			}
			if(getProjectId() > 0) {
				assignment.withCustomValue("C_Project_ID", getProjectId());
			}
			if(getCampaignId() > 0) {
				assignment.withCustomValue("C_Campaign_ID", getCampaignId());
			}
			if(getContractId() > 0) {
				assignment.withCustomValue("S_Contract_ID", getContractId());
			}
			if(getActivityId() > 0) {
				assignment.withCustomValue("C_Activity_ID", getActivityId());
			}
			if(getUser1Id() > 0) {
				assignment.withCustomValue("User1_ID", getUser1Id());
			}
			if(getUser2Id() > 0) {
				assignment.withCustomValue("User2_ID", getUser2Id());
			}
			if(getUser3Id() > 0) {
				assignment.withCustomValue("User3_ID", getUser3Id());
			}
			if(getUser4Id() > 0) {
				assignment.withCustomValue("User4_ID", getUser4Id());
			}
			if (getRequestId() > 0) {
				assignment.withCustomValue("R_Request_ID", getRequestId());
			}
			if (getContractId() > 0) {
				assignment.withCustomValue("S_Contract_ID", getContractId());
			}
			if (getProjectId() > 0) {
				assignment.withCustomValue("C_Project_ID", getProjectId());
			}
			assignment.save();
		});

		return "Ok";
	}
}