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
	package com.solop.sp034.queue.setup;

import org.adempiere.core.domains.models.X_AD_StorageUpdateQueue;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcess;
import org.compiere.model.MScheduler;
import org.compiere.model.MSchedulerPara;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.spin.queue.process.FlushSystemQueue;
import org.spin.util.ISetupDefinition;

import java.util.Properties;

/**
 * Setup for Publication Updater
 * @author Gabriel Escalona
 */
public class PublicationUpdaterSetup implements ISetupDefinition {

	private final int QueueType_ID = 50018;
	@Override
	public String doIt(Properties context, String transactionName) {
		//	Scheduler
		if (Env.getAD_Client_ID(context) == 0) {
			throw new AdempiereException("@AD_Client_ID@ = System");
		}
		createStoragePublicationUpdaterQueue(context, transactionName);
		createPublicationUpdaterSchedule(context, transactionName);
		return "@AD_SetupDefinition_ID@ @Ok@";
	}
	
	/**
	 * Create Storage Update Queue config for the publication updater
	 * @param context
	 * @param transactionName
	 */
	private void createStoragePublicationUpdaterQueue(Properties context, String transactionName) {
		boolean exists = new Query(context, X_AD_StorageUpdateQueue.Table_Name, "AD_QueueType_ID = ? AND AD_Org_ID = ?", transactionName)
				.setParameters(QueueType_ID, 0)
				.setClient_ID()
				.match();
		if (exists) {
			return;
		}
		X_AD_StorageUpdateQueue updaterQueue = new X_AD_StorageUpdateQueue(context, 0, transactionName);
		updaterQueue.setAD_Org_ID(0);
		updaterQueue.setAD_QueueType_ID(QueueType_ID);
		updaterQueue.saveEx();

	}
	/**
	 * Create scheduler that flushes the publication updater queue
	 * @param context
	 * @param transactionName
	 */
	private void createPublicationUpdaterSchedule(Properties context, String transactionName) {
		//	Get Process
		MProcess process = MProcess.get(context, FlushSystemQueue.getProcessId());
		//	Skip if a scheduler parameter already targets this queue type
		int queueTypeParaId = process.getParametersAsList().stream()
				.filter(parameter -> parameter.getColumnName().equals(FlushSystemQueue.AD_QUEUETYPE_ID))
				.findFirst()
				.map(parameter -> parameter.getAD_Process_Para_ID())
				.orElse(0);
		boolean exists = queueTypeParaId > 0 && new Query(context, MSchedulerPara.Table_Name, "AD_Process_Para_ID = ? AND ParameterDefault = ?", transactionName)
				.setParameters(queueTypeParaId, String.valueOf(QueueType_ID))
				.setClient_ID()
				.match();
		if (exists) {
			return;
		}
		MScheduler scheduler = new MScheduler(context, 0, transactionName);
		scheduler.setAD_Org_ID(0);
		scheduler.setName("Update Publication Info");
		scheduler.setDescription("Update Publication Info When Storage Updates");
		scheduler.setAD_Process_ID(FlushSystemQueue.getProcessId());
		scheduler.setSupervisor_ID(Env.getAD_User_ID(context));
		scheduler.setFrequencyType(MScheduler.FREQUENCYTYPE_Minute);
		scheduler.setFrequency(1);
		scheduler.setKeepLogDays(7);
		scheduler.setDateNextRun(TimeUtil.addMinutess(TimeUtil.getDay(System.currentTimeMillis()), scheduler.getFrequency()));
		scheduler.saveEx();
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
			schedulerParameter.setParameterDefault(String.valueOf(QueueType_ID));
			schedulerParameter.saveEx();
		});
	}
}
