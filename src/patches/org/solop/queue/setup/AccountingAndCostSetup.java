/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2015 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.solop.queue.setup;

import org.compiere.model.MProcess;
import org.compiere.model.MScheduler;
import org.compiere.model.MSchedulerPara;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.spin.queue.process.FlushSystemQueue;
import org.spin.util.ISetupDefinition;

import java.util.Properties;

/**
 * Setup for Accounting and Material Cost
 * @author Gabriel Escalona
 */
public class AccountingAndCostSetup implements ISetupDefinition {

	
	@Override
	public String doIt(Properties context, String transactionName) {
		//	Scheduler
		createAccountingSchedule(context, transactionName);
		createCostingSchedule(context, transactionName);
		return "@AD_SetupDefinition_ID@ @Ok@";
	}
	
	/**
	 * Create Accounting Schedule for setup
	 * @param context
	 * @param transactionName
	 */
	private void createAccountingSchedule(Properties context, String transactionName) {

		MScheduler scheduler = new MScheduler(context, 0, transactionName);
		scheduler.setAD_Org_ID(0);
		scheduler.setName("Process Accounting Queue");
		scheduler.setDescription("Process Accounting Queue");
		scheduler.setAD_Process_ID(FlushSystemQueue.getProcessId());
		scheduler.setSupervisor_ID(Env.getAD_User_ID(context));
		scheduler.setFrequencyType(MScheduler.FREQUENCYTYPE_Minute);
		scheduler.setFrequency(5);
		scheduler.setKeepLogDays(7);
		scheduler.setDateNextRun(TimeUtil.addMinutess(TimeUtil.getDay(System.currentTimeMillis()), scheduler.getFrequency()));
		scheduler.saveEx();
		//	Get Process
		MProcess process = MProcess.get(context, FlushSystemQueue.getProcessId());
		//	Batch Quantity
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.BATCHSTOPROCESS)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("10");
			schedulerParameter.saveEx();
		});
		//	Records Quantity
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.RECORDSBYBATCH)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("100");
			schedulerParameter.saveEx();
		});
		//	Delete records
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.ISDELETEAFTERPROCESS)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("N");
			schedulerParameter.saveEx();
		});
		//	Queue Type
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.AD_QUEUETYPE_ID)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("50014");
			schedulerParameter.saveEx();
		});
	}
	/**
	 * Create Costing Schedule for setup
	 * @param context
	 * @param transactionName
	 */
	private void createCostingSchedule(Properties context, String transactionName) {

		MScheduler scheduler = new MScheduler(context, 0, transactionName);
		scheduler.setAD_Org_ID(0);
		scheduler.setName("Process Costing Queue");
		scheduler.setDescription("Process Costing Queue");
		scheduler.setAD_Process_ID(FlushSystemQueue.getProcessId());
		scheduler.setSupervisor_ID(Env.getAD_User_ID(context));
		scheduler.setFrequencyType(MScheduler.FREQUENCYTYPE_Minute);
		scheduler.setFrequency(5);
		scheduler.setKeepLogDays(7);
		scheduler.setDateNextRun(TimeUtil.addMinutess(TimeUtil.getDay(System.currentTimeMillis()), scheduler.getFrequency()));
		scheduler.saveEx();
		//	Get Process
		MProcess process = MProcess.get(context, FlushSystemQueue.getProcessId());
		//	Batch Quantity
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.BATCHSTOPROCESS)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("10");
			schedulerParameter.saveEx();
		});
		//	Records Quantity
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.RECORDSBYBATCH)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("100");
			schedulerParameter.saveEx();
		});
		//	Delete records
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.ISDELETEAFTERPROCESS)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("N");
			schedulerParameter.saveEx();
		});
		//	Queue Type
		process.getParametersAsList().stream().filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.AD_QUEUETYPE_ID)).findFirst().ifPresent(parameter -> {
			MSchedulerPara schedulerParameter = new MSchedulerPara(context, 0, transactionName);
			schedulerParameter.setAD_Org_ID(0);
			schedulerParameter.setAD_Scheduler_ID(scheduler.getAD_Scheduler_ID());
			schedulerParameter.setAD_Process_Para_ID(parameter.getAD_Process_Para_ID());
			schedulerParameter.setParameterDefault("50015");
			schedulerParameter.saveEx();
		});
	}
}
