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
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProject;
import org.compiere.model.MRequest;
import org.compiere.model.PO;
import org.eevolution.context.service.infrastructure.domain.entities.MSContract;

import java.util.ArrayList;
import java.util.List;

/**
 * 	Generated Process for (Record Hours Worked)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop http://www.solopsoftware.com
 *  @version Release 3.9.4
 *
 *  Supports three ways of recording hours:
 *  <ul>
 *    <li>Directly from a single Request (Record_ID over R_Request)</li>
 *    <li>From a selection of Requests</li>
 *    <li>Without a Request: from a Project / Contract Record_ID, or standalone
 *        using only the process parameters</li>
 *  </ul>
 */
public class RecordHoursWorked extends RecordHoursWorkedAbstract {
	/** Request ids to iterate when the process runs over a selection */
	private List<Integer> requestIds = new ArrayList<>();

	@Override
	protected void prepare() {
		super.prepare();
		if (isSelection()) {
			requestIds = getSelectionKeys();
		} else if (getRecord_ID() > 0 && getTableName() != null) {
			if (getTableName().equals(I_R_Request.Table_Name)) {
				setRequestId(getRecord_ID());
			} else if (getTableName().equals(I_C_Project.Table_Name)) {
				setProjectId(getRecord_ID());
			} else if (getTableName().equals(MSContract.Table_Name)) {
				setContractId(getRecord_ID());
			}
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (isSelection()) {
			//	Way 2: a selection of Requests
			if (requestIds == null || requestIds.isEmpty()) {
				throw new AdempiereException("@Record_ID@ @NotFound@");
			}
			requestIds.forEach(requestId -> {
				resetRequestDerivedValues();
				setRequestId(requestId);
				setDescription(getSelectionAsString(requestId, "R_Description"));
				setQuantityHours(getSelectionAsBigDecimal(requestId, "R_SP007_QuantityHours"));
				setActivity(getSelectionAsString(requestId, "R_SP007_Activity"));
				loadDefaultValues();
				createAssignment();
			});
		} else {
			//	Way 1: a single Request / Way 3: Project, Contract or standalone parameters
			loadDefaultValues();
			createAssignment();
		}
		return "Ok";
	}

	/**
	 * Clear the values that are derived from the source document so each Request in a
	 * selection loads its own defaults instead of inheriting them from the previous one.
	 */
	private void resetRequestDerivedValues() {
		setBPartnerId(0);
		setProjectId(0);
		setContractId(0);
		setActivityId(0);
		setCampaignId(0);
		setUser1Id(0);
		setUser2Id(0);
		setUser3Id(0);
		setUser4Id(0);
	}

	/**
	 * Fill the missing values from the source document (Request, Project or Contract)
	 * when one is available. When none is set (standalone) it keeps the parameters as entered.
	 */
	private void loadDefaultValues() {
		if (getRequestId() > 0) {
			MRequest request = new MRequest(getCtx(), getRequestId(), get_TrxName());
			loadValueFromPO(request);
		} else if (getProjectId() > 0) {
			MProject project = new MProject(getCtx(), getProjectId(), get_TrxName());
			loadValueFromPO(project);
		} else if (getContractId() > 0) {
			MSContract contract = new MSContract(getCtx(), getContractId(), get_TrxName());
			loadValueFromPO(contract);
		}
	}

	private void loadValueFromPO(PO source) {
		if (getBPartnerId() <= 0) {
			setBPartnerId(source.get_ValueAsInt("C_BPartner_ID"));
		}
		if (getProjectId() <= 0) {
			setProjectId(source.get_ValueAsInt("C_Project_ID"));
		}
		if (getContractId() <= 0) {
			setContractId(source.get_ValueAsInt("S_Contract_ID"));
		}
		if (getActivityId() <= 0) {
			setActivityId(source.get_ValueAsInt("C_Activity_ID"));
		}
		if (getCampaignId() <= 0) {
			setCampaignId(source.get_ValueAsInt("C_Campaign_ID"));
		}
		if (getUser1Id() <= 0) {
			setUser1Id(source.get_ValueAsInt("User1_ID"));
		}
		if (getUser2Id() <= 0) {
			setUser2Id(source.get_ValueAsInt("User2_ID"));
		}
		if (getUser3Id() <= 0) {
			setUser3Id(source.get_ValueAsInt("User3_ID"));
		}
		if (getUser4Id() <= 0) {
			setUser4Id(source.get_ValueAsInt("User4_ID"));
		}
	}

	/**
	 * Build and persist a resource assignment from the current values.
	 */
	private void createAssignment() {
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
		if (getBPartnerId() > 0) {
			assignment.withCustomValue("C_BPartner_ID", getBPartnerId());
		}
		if (getProjectId() > 0) {
			assignment.withCustomValue("C_Project_ID", getProjectId());
		}
		if (getCampaignId() > 0) {
			assignment.withCustomValue("C_Campaign_ID", getCampaignId());
		}
		if (getContractId() > 0) {
			assignment.withCustomValue("S_Contract_ID", getContractId());
		}
		if (getActivityId() > 0) {
			assignment.withCustomValue("C_Activity_ID", getActivityId());
		}
		if (getUser1Id() > 0) {
			assignment.withCustomValue("User1_ID", getUser1Id());
		}
		if (getUser2Id() > 0) {
			assignment.withCustomValue("User2_ID", getUser2Id());
		}
		if (getUser3Id() > 0) {
			assignment.withCustomValue("User3_ID", getUser3Id());
		}
		if (getUser4Id() > 0) {
			assignment.withCustomValue("User4_ID", getUser4Id());
		}
		if (getRequestId() > 0) {
			assignment.withCustomValue("R_Request_ID", getRequestId());
		}
		assignment.save();
	}
}
