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

package org.compiere.recognition.process;

import org.adempiere.core.domains.models.I_C_RevenueRecognition_Plan;
import org.compiere.model.MRevenueRecognitionPlan;
import org.compiere.model.MRevenueRecognitionRun;
import org.compiere.model.Query;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 	Generated Process for (Revenue Recognition Run)
 *  @author Yamel Senih, yamel.senih@solopsoftware.com, Solop <a href="http://www.solopsoftware.com">http://www.solopsoftware.com</a>
 */
public class RevenueRecognitionRun extends RevenueRecognitionRunAbstract {
	@Override
	protected String doIt() throws Exception {
		final AtomicInteger revenueCount = new AtomicInteger(0);
		long startTime = System.currentTimeMillis();
		List<Integer> revenuePlanIds = getRevenuePlanIds();
		revenuePlanIds.parallelStream().forEach(revenuePlanId -> {
			try {
				Trx.run(transactionName -> {
					MRevenueRecognitionPlan recognitionPlan = new MRevenueRecognitionPlan(getCtx(), revenuePlanId, transactionName);
					boolean reverseAllPreviousRun = recognitionPlan.getC_RevenueRecognition().isReverseBeforeProcess();
					if(isForce()) {
						MRevenueRecognitionRun previousRecognitionRun = recognitionPlan.getLastValidRecognitionRunForDate(getDateDoc());
						if(previousRecognitionRun != null) {
							previousRecognitionRun.reverseIt(false);
							revenueCount.incrementAndGet();
						}
					}
					if(reverseAllPreviousRun) {
						MRevenueRecognitionRun previousRecognitionRun = recognitionPlan.getPreviousValidRecognitionRun(getDateDoc());
						if(previousRecognitionRun != null) {
							previousRecognitionRun.reverseIt(false);
							revenueCount.incrementAndGet();
						}
					}
					MRevenueRecognitionRun monthlyRecognition = recognitionPlan.getLastValidRecognitionRunForDate(getDateDoc());
					if(monthlyRecognition == null) {
						MRevenueRecognitionRun recognitionRun = new MRevenueRecognitionRun(recognitionPlan, transactionName);
						recognitionRun.setDateDoc(getDateDoc());
						recognitionRun.saveEx();
						String message = null;
						if(!recognitionRun.processIt(MRevenueRecognitionRun.DOCSTATUS_Completed)) {
							message = recognitionRun.getProcessMsg();
						}
						recognitionRun.saveEx();
						recognitionPlan.updateRecognizedAmount(TimeUtil.getDayTime(getDateDoc(), new Timestamp(System.currentTimeMillis())));
						if(!Util.isEmpty(message, true)) {
							addLog(message);
						}
						revenueCount.incrementAndGet();
					}
				});
			} catch (Exception e) {
				addLog(e.getLocalizedMessage());
			}
		});
		long durationTime = System.currentTimeMillis() - startTime;
		return "@C_RevenueRecognition_Run_ID@ @Created@: " + revenueCount.get() + " - " + TimeUtil.formatElapsed(durationTime);
	}

	private List<Integer> getRevenuePlanIds() {
		StringBuilder whereClause = new StringBuilder("Processed = 'Y' AND IsRecognized = 'N'");
		List<Object> parameters = new ArrayList<>();
		if (getOrgId() > 0) {
			whereClause.append(" AND AD_Org_ID = ?");
			parameters.add(getOrgId());
		}
		if(getBPartnerId() > 0) {
			whereClause.append(" AND C_BPartner_ID = ?");
			parameters.add(getBPartnerId());
		}
		if (getContractId() > 0) {
			whereClause.append(" AND S_Contract_ID = ?");
			parameters.add(getContractId());
		}
		if (getProjectId() > 0) {
			whereClause.append(" AND C_Project_ID = ?");
			parameters.add(getProjectId());
		}
		if (getOrderId() > 0) {
			whereClause.append(" AND C_Order_ID = ?");
			parameters.add(getOrderId());
		}
		if (getInvoiceId() > 0) {
			whereClause.append(" AND C_Invoice_ID = ?");
			parameters.add(getInvoiceId());
		}
		if (!isForce()) {
			whereClause.append(" AND (DateLastRun IS NULL OR DateLastRun < ?)");
			parameters.add(getDateDoc());
		}
		return new Query(getCtx(), I_C_RevenueRecognition_Plan.Table_Name, whereClause.toString(), get_TrxName())
				.setParameters(parameters)
				.setOnlyActiveRecords(true)
				.setClient_ID()
				.getIDsAsList();
	}
}