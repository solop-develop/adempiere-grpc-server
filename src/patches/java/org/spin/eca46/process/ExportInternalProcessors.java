/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.spin.eca46.process;

import org.adempiere.core.domains.models.I_AD_AlertProcessor;
import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.core.domains.models.I_AD_Scheduler;
import org.adempiere.core.domains.models.I_AD_WorkflowProcessor;
import org.adempiere.core.domains.models.I_C_AcctProcessor;
import org.adempiere.core.domains.models.I_C_ProjectProcessor;
import org.adempiere.core.domains.models.I_R_RequestProcessor;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAcctProcessor;
import org.compiere.model.MAlertProcessor;
import org.compiere.model.MRequestProcessor;
import org.compiere.model.MScheduler;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflowProcessor;
import org.eevolution.model.MProjectProcessor;
import org.spin.eca46.util.support.Accounting;
import org.spin.eca46.util.support.Alert;
import org.spin.eca46.util.support.IExternalProcessor;
import org.spin.eca46.util.support.IProcessorEntity;
import org.spin.eca46.util.support.Project;
import org.spin.eca46.util.support.Request;
import org.spin.eca46.util.support.Schedule;
import org.spin.eca46.util.support.Workflow;
import org.spin.model.MADAppRegistration;
import org.spin.model.MADTokenDefinition;
import org.spin.util.IThirdPartyAccessGenerator;
import org.spin.util.ITokenGenerator;
import org.spin.util.TokenGeneratorHandler;
import org.spin.util.support.AppSupportHandler;
import org.spin.util.support.IAppSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Generated Process for (Export Internal Processors)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class ExportInternalProcessors extends ExportInternalProcessorsAbstract {

	private AtomicInteger counter = new AtomicInteger();
	
	@Override
	protected String doIt() throws Exception {
		throw	new AdempiereException("Not Implemented");
		//TODO: This process should implement correctly the new way to generate Token with scopes
		/*
		List<IProcessorEntity> processorsList = new ArrayList<>();
		//	For Accounting Processor
		new Query(getCtx(), I_C_AcctProcessor.Table_Name, null, get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(processorId -> processorsList.add(Accounting.newInstance().withAccountingProcessor(new MAcctProcessor(getCtx(), processorId, get_TrxName()))));
		//	For Alert Processor
		new Query(getCtx(), I_AD_AlertProcessor.Table_Name, null, get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(processorId -> processorsList.add(Alert.newInstance().withAlertProcessor(new MAlertProcessor(getCtx(), processorId, get_TrxName()))));
		//	For Project Processor
		new Query(getCtx(), I_C_ProjectProcessor.Table_Name, null, get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(processorId -> processorsList.add(Project.newInstance().withProjectProcessor(new MProjectProcessor(getCtx(), processorId, get_TrxName()))));
		//	For Request Processor
		new Query(getCtx(), I_R_RequestProcessor.Table_Name, null, get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(processorId -> processorsList.add(Request.newInstance().withRequestProcessor(new MRequestProcessor(getCtx(), processorId, get_TrxName()))));
		//	For Schedule Processor
		new Query(getCtx(), I_AD_Scheduler.Table_Name, null, get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(processorId -> processorsList.add(Schedule.newInstance().withSchedulerProcessor(new MScheduler(getCtx(), processorId, get_TrxName()))));
		//	For Workflow Processor
		new Query(getCtx(), I_AD_WorkflowProcessor.Table_Name, null, get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList()
			.forEach(processorId -> processorsList.add(Workflow.newInstance().withWorkflowProcessor(new MWorkflowProcessor(getCtx(), processorId, get_TrxName()))));
		//	Create all
		MADAppRegistration registeredApplication = MADAppRegistration.getById(Env.getCtx(), getExternalProcessorId(), get_TrxName());
		IAppSupport supportedApplication = AppSupportHandler.getInstance().getAppSupport(registeredApplication);
		//	Exists a Application available for it?
		if(supportedApplication != null
				&& IExternalProcessor.class.isAssignableFrom(supportedApplication.getClass())) {
			//	Instance of dKron Processor
			IExternalProcessor dKronProcessor = (IExternalProcessor) supportedApplication;
			dKronProcessor.setHost(getHost());
			dKronProcessor.setTokenAccess(getToken());
			processorsList.forEach(processor -> {
				String result = dKronProcessor.exportProcessor(processor);
				counter.incrementAndGet();
				if(!Util.isEmpty(result)) {
					addLog(result);
				}
			});
		}
		return "@Created@: " + counter.get();
		*/
	}
	
	private String getToken() {
		throw	new AdempiereException("Not Implemented");
		//TODO: This process should implement correctly the new way to generate Token with scopes
		/*
		try {
			//	Validate user and password match
			boolean match = new Query(getCtx(), I_AD_Role.Table_Name, 
					"EXISTS(SELECT 1 FROM AD_User_Roles ur "
					+ "WHERE ur.AD_Role_ID = AD_Role.AD_Role_ID "
					+ "AND ur.AD_User_ID = ?)", get_TrxName())
					.setParameters(getUserId())
					.match();
			if(!match) {
				throw new AdempiereException("@AD_User_ID@ / @AD_Role_ID@ @Mismatched@");
			}
			ITokenGenerator generator = TokenGeneratorHandler.getInstance().getTokenGenerator(MADTokenDefinition.TOKENTYPE_ThirdPartyAccess);
			if(generator == null) {
				throw new AdempiereException("@AD_TokenDefinition_ID@ @NotFound@");
			}
			//	No child of definition
			if(!IThirdPartyAccessGenerator.class.isAssignableFrom(generator.getClass())) {
				throw new AdempiereException("@AD_TokenDefinition_ID@ @Invalid@");	
			}
			//	Generate
			IThirdPartyAccessGenerator thirdPartyAccessGenerator = ((IThirdPartyAccessGenerator) generator);
			//	No token profile here: relies on a Full Access token definition (scope="all")
			return thirdPartyAccessGenerator.generateToken(getUserId(), getRoleId(), 0);
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
		*/
	}
}