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
package org.eevolution.context.service.infrastructure.domain.entities;

import org.adempiere.core.domains.models.I_C_DocType;
import org.adempiere.core.domains.models.I_PP_Period;
import org.adempiere.core.domains.models.I_S_ContractLine;
import org.adempiere.core.domains.models.X_S_Contract;
import org.compiere.model.*;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.manufacturing.model.MPPPeriod;
import org.eevolution.manufacturing.model.MPPPeriodDefinition;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

/**
 * Contract Entity
 */
public class MSContract extends X_S_Contract implements DocAction, DocOptions {

	public MSContract(Properties ctx, int S_Contract_ID, String trxName) {
		super(ctx, S_Contract_ID, trxName);
	}

	public MSContract(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/** Process Message */
	private String processMessage = "";

	/** Just Prepared Flag */
	private boolean justPrepared = false;

	@Override
	protected boolean beforeSave(boolean newRecord) {
		return true;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		return true;
	}

	@Override
	protected boolean beforeDelete() {
		return true;
	}

	@Override
	public String getDocumentInfo() {
		MDocType documentType = MDocType.get(getCtx(), getC_DocType_ID());
		return documentType.getName() + "   " + getDocumentNo();
	}

	@Override
	public boolean processIt(String processAction) throws Exception {
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(processAction, getDocAction());
	}

	@Override
	public boolean unlockIt() {
		log.info("unlockIt -  " + toString());
		return true;
	}

	@Override
	public boolean invalidateIt() {
		log.info("invalidateIt - " + toString());
		setDocAction(X_S_Contract.DOCACTION_Prepare);
		return true;
	}

	@Override
	public String prepareIt() {
		log.info(toString());
		processMessage = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (processMessage != null)
			return DocAction.STATUS_Invalid;

		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getDateDoc(), dt.getDocBaseType(), getAD_Org_ID())) {
			processMessage = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}

		List<MSContractLine> contractLines = getLines();
		if (contractLines.isEmpty()) {
			processMessage = "@NoLines@";
			return DocAction.STATUS_Invalid;
		} else {
			BigDecimal totalLines = Env.ZERO;
			for (MSContractLine contractLine : contractLines) {
				totalLines = totalLines.add(contractLine.getLineNetAmt());
			}
			setTotalLines(totalLines);
			setGrandTotal(totalLines);
		}

		//	Add up Amounts
		processMessage = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (processMessage != null)
			return DocAction.STATUS_Invalid;
		justPrepared = true;
		if (!X_S_Contract.DOCACTION_Complete.equals(getDocAction()))
			setDocAction(X_S_Contract.DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}

	@Override
	public boolean approveIt() {
		log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}

	@Override
	public boolean rejectIt() {
		log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}

	@Override
	public String completeIt() {
		//	Re-Check
		if (!justPrepared) {
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		processMessage = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (processMessage != null) {
			return DocAction.STATUS_Invalid;
		}

		//	Implicit Approval
		if (!isApproved())
			approveIt();
		log.info(toString());
		//
		if (getPP_PeriodDefinition_ID() > 0)
			creteTimeSheetReportExpense();

		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null) {
			processMessage = valid;
			return DocAction.STATUS_Invalid;
		}
		//	Set Definitive Document No
		setDefiniteDocumentNo();
		setProcessed(true);
		setDocAction(X_S_Contract.DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}

	public void creteTimeSheetReportExpense() {
		int lineNo = 10;
		MPPPeriodDefinition periodDefinition = new MPPPeriodDefinition(getCtx(), getPP_PeriodDefinition_ID(), get_TrxName());
		for (MSContractLine contractLine : getLines()) {
			for (MPPPeriod period : periodDefinition.getPeriodsOrderBy(I_PP_Period.COLUMNNAME_PeriodNo)) {
				MTimeExpense timeReport = new MTimeExpense(getCtx(), 0, get_TrxName());
				timeReport.set_ValueOfColumn("S_Contract_ID", get_ID());
				timeReport.setC_BPartner_ID(getC_BPartner_ID());
				timeReport.setDateReport(period.getStartDate());
				timeReport.set_ValueOfColumn("PP_PeriodDefinition_ID", periodDefinition.get_ID());
				timeReport.set_ValueOfColumn("PP_Period_ID", period.get_ID());
				timeReport.set_ValueOfColumn("PP_Calendar_ID", periodDefinition.getPP_Calendar_ID());
				timeReport.setDescription(contractLine.getDescription());
				timeReport.setM_PriceList_ID(getM_PriceList_ID());
				timeReport.setM_Warehouse_ID(getM_Warehouse_ID());
				timeReport.set_ValueOfColumn("C_Project_ID", getC_Project_ID());
				timeReport.set_ValueOfColumn("C_Activity_ID", getC_Activity_ID());
				timeReport.set_ValueOfColumn("C_Campaign_ID", getC_Campaign_ID());
				timeReport.setAD_Org_ID(getAD_Org_ID());
				timeReport.set_ValueOfColumn("AD_OrgTrx_ID", getAD_OrgTrx_ID());
				timeReport.set_ValueOfColumn("User1_ID", getUser1_ID());
				timeReport.set_ValueOfColumn("User2_ID", getUser1_ID());
				timeReport.set_ValueOfColumn("User3_ID", getUser1_ID());
				timeReport.set_ValueOfColumn("User4_ID", getUser1_ID());
				timeReport.setDocStatus(DocAction.ACTION_Complete);
				timeReport.setDocAction(DocAction.STATUS_Drafted);
				timeReport.set_ValueOfColumn("IsSOTrx", isSOTrx());
				timeReport.saveEx();

				MTimeExpenseLine timeReportLine = new MTimeExpenseLine(getCtx(), 0, get_TableName());
				timeReportLine.setS_TimeExpense_ID(timeReport.get_ID());
				timeReportLine.set_ValueOfColumn("S_ContractLine_ID", contractLine.get_ID());
				timeReportLine.setLine(lineNo);
				timeReportLine.set_ValueOfColumn("PP_Calendar_ID", periodDefinition.getPP_Calendar_ID());
				timeReportLine.set_ValueOfColumn("PP_PeriodDefinition_ID", periodDefinition.get_ID());
				timeReportLine.set_ValueOfColumn("PP_Period_ID", period.get_ID());
				timeReportLine.setDateExpense(period.getStartDate());
				timeReportLine.setM_Product_ID(contractLine.getM_Product_ID());
				timeReportLine.setQty(contractLine.getQtyOrdered());
				timeReportLine.setC_BPartner_ID(contractLine.getC_BPartner_ID());
				timeReportLine.setDescription(contractLine.getDescription());
				if (isSOTrx()) {
					timeReportLine.setIsInvoiced(true);
					timeReportLine.setInvoicePrice(contractLine.getPriceActual());
				} else {
					timeReportLine.setExpenseAmt(contractLine.getPriceActual());
				}

				timeReportLine.setC_Currency_ID(contractLine.getC_Currency_ID());
				timeReportLine.setC_Tax_ID(contractLine.getC_Tax_ID());
				timeReportLine.setLineNetAmt(contractLine.getLineNetAmt());
				timeReportLine.setC_Activity_ID(contractLine.getC_Activity_ID());
				timeReportLine.setC_Campaign_ID(contractLine.getC_Campaign_ID());
				timeReportLine.setAD_Org_ID(contractLine.getAD_Org_ID());
				timeReportLine.set_ValueOfColumn("AD_OrgTrx_ID", contractLine.getAD_OrgTrx_ID());
				timeReportLine.set_ValueOfColumn("User1_ID", contractLine.getUser1_ID());
				timeReportLine.set_ValueOfColumn("User2_ID", contractLine.getUser1_ID());
				timeReportLine.set_ValueOfColumn("User3_ID", contractLine.getUser1_ID());
				timeReportLine.set_ValueOfColumn("User4_ID", contractLine.getUser1_ID());
				timeReportLine.set_ValueOfColumn("C_Project_ID", contractLine.getC_Project_ID());
				timeReportLine.setC_ProjectPhase_ID(contractLine.getC_ProjectPhase_ID());
				timeReportLine.setC_ProjectTask_ID(contractLine.getC_ProjectTask_ID());
				timeReportLine.saveEx();
				lineNo = lineNo + 10;
			}
		}
	}

	private void setDefiniteDocumentNo() {
		MDocType documentType = MDocType.get(getCtx(), getC_DocType_ID());
		if (documentType.isOverwriteDateOnComplete())
			setDateDoc(new Timestamp(System.currentTimeMillis()));
		if (documentType.isOverwriteSeqOnComplete()) {
			String value = "";
			int index = p_info.getColumnIndex(I_C_DocType.COLUMNNAME_C_DocType_ID);
			if (index != -1)
				value = DB.getDocumentNo(get_ValueAsInt(index), get_TrxName(), true);
			if (value != null)
				setDocumentNo(value);
		}
	}

	@Override
	public boolean voidIt() {
		log.info("voidIt - " + toString());
		return closeIt();
	}

	@Override
	public boolean closeIt() {
		log.info("closeIt -  " + toString());
		setDocAction(X_S_Contract.DOCACTION_None);
		return true;
	}

	@Override
	public boolean reverseCorrectIt() {
		log.info("reverseCorrectIt - " + toString());
		return false;
	}

	@Override
	public boolean reverseAccrualIt() {
		log.info("reverseAccrualIt - " + toString());
		return false;
	}

	@Override
	public boolean reActivateIt() {
		log.info("reActivateIt - " + toString());
		// Before reActivate
		processMessage = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (processMessage != null)
			return false;
		MDocType documentType = MDocType.get(getCtx(), getC_DocType_ID());

		// After reActivate
		processMessage = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
		if (processMessage != null) {
			return false;
		}

		setDocAction(DocAction.ACTION_Complete);
		setProcessed(false);
		setPosted(false);
		return true;
	}

	@Override
	public int customizeValidActions(String docStatus, Object processing, String orderType, String isSOTrx,
			int AD_Table_ID, String[] docAction, String[] options, int index) {
		//	Valid Document Action
		int seqIndex = index;
		if (AD_Table_ID == I_S_ContractLine.Table_ID) {
			if (docStatus.equals(DocAction.STATUS_Drafted)
					|| docStatus.equals(DocAction.STATUS_InProgress)
					|| docStatus.equals(DocAction.STATUS_Invalid)) {
				options[seqIndex++] = DocAction.ACTION_Prepare;
			} else { //	Complete                    ..  CO
				if (docStatus.equals(DocAction.STATUS_Completed)) {
					options[seqIndex++] = DocAction.ACTION_Void;
					options[seqIndex++] = DocAction.ACTION_ReActivate;
					options[seqIndex++] = DocAction.ACTION_Close;
				} else if (docStatus.equals(DocAction.STATUS_Closed)) {
					options[seqIndex++] = DocAction.ACTION_None;
				}
			}
		}
		return seqIndex;
	}

	@Override
	public String getSummary() {
		StringBuffer sb = new StringBuffer();
		sb.append(getDocumentNo());
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}

	public File createPDF(File file) {
		// ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.INVOICE, getC_Invoice_ID());
		// if (re == null)
		return null;
		//	createPDF//	return re.getPDF(file);
	}

	@Override
	public File createPDF() {
		try {
			File temp = File.createTempFile(get_TableName() + get_ID() + "_", ".pdf");
			return createPDF(temp);
		} catch (Exception e) {
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}

	@Override
	public String getProcessMsg() {
		return processMessage;
	}

	@Override
	public int getDoc_User_ID() {
		return getCreatedBy();
	}

	@Override
	public BigDecimal getApprovalAmt() {
		return BigDecimal.ZERO;
	}

	public List<MSContractLine> getLines() {
		String whereClause = I_S_ContractLine.COLUMNNAME_S_Contract_ID + "=?";
		return new Query(getCtx(), I_S_ContractLine.Table_Name, whereClause, get_TrxName())
				.setParameters(get_ID())
				.list();
	}

}	//	MSContract
