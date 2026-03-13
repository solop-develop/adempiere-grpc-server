/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
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
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import org.adempiere.core.domains.models.X_C_PPBatchConfiguration;
import org.adempiere.core.domains.models.X_S_Contract;
import org.adempiere.core.domains.models.X_S_TimeExpense;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PeriodClosedException;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.process.DocumentReversalEnabled;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

/**
 * 	Time + Expense Model
 *
 *	@author Jorg Janke
 *
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962 
 *	@version $Id: MTimeExpense.java,v 1.4 2006/07/30 00:51:03 jjanke Exp $
 */
public class MTimeExpense extends X_S_TimeExpense implements DocAction, DocumentReversalEnabled, DocOptions
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1567303438502090279L;

	/**	Process Message 			*/
	private String processMsg = null;
	/**
	 * 	Default Constructor
	 *	@param ctx context
	 *	@param S_TimeExpense_ID id
	 *	@param trxName transaction
	 */
	public MTimeExpense(Properties ctx, int S_TimeExpense_ID, String trxName)
	{
		super (ctx, S_TimeExpense_ID, trxName);
		if (S_TimeExpense_ID == 0)
		{
		//	setC_BPartner_ID (0);
			setDateReport (new Timestamp (System.currentTimeMillis ()));
		//	setDocumentNo (null);
			setIsApproved (false);
		//	setM_PriceList_ID (0);
		//	setM_Warehouse_ID (0);
			super.setProcessed (false);
			setProcessing(false);
		}
	}	//	MTimeExpense

	/**
	 * 	Load Constructor
	 * 	@param ctx context
	 * 	@param rs result set
	 *	@param trxName transaction
	 */
	public MTimeExpense(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MTimeExpense

	/** Default Locator				*/
	private int					m_M_Locator_ID = 0;
	/**	Lines						*/
	private MTimeExpenseLine[]	m_lines = null;
	/** Cached User					*/
	private int					m_AD_User_ID = 0;


	/**
	 * 	Get Lines Convenience Wrapper
	 *	@return array of lines
	 */
	public MTimeExpenseLine[] getLines ()
	{
		return getLines(true);
	}

	/**
	 * 	Get Lines
	 * 	@param requery true requeries
	 *	@return array of lines
	 */
	public MTimeExpenseLine[] getLines (boolean requery)
	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		//
		int C_Currency_ID = getC_Currency_ID();
		ArrayList<MTimeExpenseLine> list = new ArrayList<MTimeExpenseLine>();
		//
		String sql = "SELECT * FROM S_TimeExpenseLine WHERE S_TimeExpense_ID=? ORDER BY Line";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, getS_TimeExpense_ID());
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MTimeExpenseLine te = new MTimeExpenseLine(getCtx(), rs, get_TrxName());
				te.setC_Currency_Report_ID(C_Currency_ID);
				list.add(te);
			}
			rs.close ();
			pstmt.close ();
			pstmt = null;
		}
		catch (SQLException ex)
		{
			log.log(Level.SEVERE, "getLines", ex);
		}
		try
		{
			if (pstmt != null)
				pstmt.close ();
		}
		catch (SQLException ex1)
		{
		}
		pstmt = null;
		//
		m_lines = new MTimeExpenseLine[list.size()];
		list.toArray(m_lines);
		return m_lines;
	}	//	getLines

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getC_DocType_ID() <= 0) {
			Optional<MDocType> doctypeOptional = Arrays.stream(MDocType.getOfDocBaseType(getCtx(), "TE1")).min((docType1, docType2) -> Boolean.compare(docType2.isDefault(), docType1.isDefault()));
			doctypeOptional.ifPresent(docType -> setC_DocType_ID(docType.getC_DocType_ID()));
			if (getC_DocType_ID() <= 0)
				throw new AdempiereException("@C_DocType_ID@ @FillMandatory@");
		}

		return super.beforeSave(newRecord);
	}

	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription

	/**
	 *	Get Default Locator (from Warehouse)
	 *	@return locator
	 */
	public int getM_Locator_ID()
	{
		if (m_M_Locator_ID != 0)
			return m_M_Locator_ID;
		//
		String sql = "SELECT M_Locator_ID FROM M_Locator "
			+ "WHERE M_Warehouse_ID=? AND IsActive='Y' ORDER BY IsDefault DESC, Created";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, getM_Warehouse_ID());
			ResultSet rs = pstmt.executeQuery ();
			if (rs.next ())
				m_M_Locator_ID = rs.getInt(1);
			rs.close ();
			pstmt.close ();
			pstmt = null;
		}
		catch (SQLException ex)
		{
			log.log(Level.SEVERE, "getM_Locator_ID", ex);
		}
		try
		{
			if (pstmt != null)
				pstmt.close ();
		}
		catch (SQLException ex1)
		{
		}
		pstmt = null;
		//
		return m_M_Locator_ID;
	}	//	getM_Locator_ID

	/**
	 * 	Set Processed.
	 * 	Propergate to Lines/Taxes
	 *	@param processed processed
	 */
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		String sql = "UPDATE S_TimeExpenseLine SET Processed='"
			+ (processed ? "Y" : "N")
			+ "' WHERE S_TimeExpense_ID=" + getS_TimeExpense_ID();
		int noLine = DB.executeUpdate(sql, get_TrxName());
		m_lines = null;
		log.fine(processed + " - Lines=" + noLine);
	}	//	setProcessed
	
	/**
	 * 	Get Document Info
	 *	@return document info
	 */
	public String getDocumentInfo()
	{
		return Msg.getElement(getCtx(), "S_TimeExpense_ID") + " " + getDocumentNo();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
	//	ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.INVOICE, getC_Invoice_ID());
	//	if (re == null)
			return null;
	//	return re.getPDF(file);
	}	//	createPDF
	
	/**************************************************************************
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;
	
	private boolean isReversal;
	private boolean isVoided = false;
	
	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		log.info("unlockIt - " + toString());
		setProcessing(false);
		return true;
	}	//	unlockIt
	
	/**
	 * 	Invalidate Document
	 * 	@return true if success 
	 */
	public boolean invalidateIt()
	{
		log.info("invalidateIt - " + toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	
	/**
	 *	Prepare Document
	 * 	@return new status (In Progress or Invalid) 
	 */
	public String prepareIt()
	{
		log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Std Period open? - AP (Reimbursement) Invoice
		if (!MPeriod.isOpen(getCtx(), getDateReport(), MDocType.DOCBASETYPE_APInvoice, getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		
		MTimeExpenseLine[] lines = getLines(false);
		if (lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		//	Add up Amounts
		BigDecimal amt = Env.ZERO;
		for (int i = 0; i < lines.length; i++)
		{
			MTimeExpenseLine line = lines[i];
			amt = amt.add(line.getApprovalAmt());
		}
		setApprovalAmt(amt);

		//	Invoiced but no BP
		for (int i = 0; i < lines.length; i++)
		{
			MTimeExpenseLine line = lines[i];
			if (line.isInvoiced() && line.getC_BPartner_ID() == 0)
			{
				m_processMsg = "@Line@ " + line.getLine() + ": Invoiced, but no Business Partner";
				return DocAction.STATUS_Invalid;
			}
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	public boolean  approveIt()
	{
		log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	public boolean rejectIt()
	{
		log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	/**
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		log.info("completeIt - " + toString());

		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}

		//
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Void Document.
	 * 	Same as Close.
	 * 	@return true if success 
	 */
	public boolean voidIt()
	{

		log.info(toString());
		boolean retValue = false;
		if (DOCSTATUS_Closed.equals(getDocStatus())
				|| DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			processMsg = "Document Closed: " + getDocStatus();
			setDocAction(DOCACTION_None);
			return false;
		}

		//	Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus())
				|| DOCSTATUS_Invalid.equals(getDocStatus())
				|| DOCSTATUS_InProgress.equals(getDocStatus())
				|| DOCSTATUS_Approved.equals(getDocStatus()))

		{
			processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
			if (processMsg != null)
				return false;

			//	Set lines to 0

			setDocStatus(DOCSTATUS_Voided);
			setDocAction(DOCACTION_None);
			addDescription(Msg.getMsg(getCtx(), "Voided"));
			retValue = true;
		}
		else
		{
			boolean accrual = false;
			try
			{
				MPeriod.testPeriodOpen(getCtx(), getDateReport(), MPeriodControl.DOCBASETYPE_ARInvoice, getAD_Org_ID());
			}
			catch (PeriodClosedException e)
			{
				accrual = true;
			}
			isVoided = true;
			if (accrual)
				return reverseAccrualIt();
			else
				return reverseCorrectIt();
		}

		// After Void
		processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (processMsg != null)
			return false;

		setDocAction(DOCACTION_None);

		return retValue;
	}	//	voidIt
	
	/**
	 * 	Close Document.
	 * 	Cancel not delivered Qunatities
	 * 	@return true if success 
	 */
	public boolean closeIt()
	{
		log.info("closeIt - " + toString());
		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
		if (m_processMsg != null)
			return false;
		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;

		//	Close Not delivered Qty
	//	setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt

	@Override
	public void setReversal_ID(int i) {

	}

	@Override
	public int getReversal_ID() {
		return 0;
	}

	@Override
	public MTimeExpense reverseIt(boolean b) {
		log.info("reverseCorrectIt - " + toString());
		String whereClause = "S_TimeExpenseLine.S_TimeExpense_ID = ? " +
				" AND EXISTS (SELECT 1 FROM C_Invoice i " +
				" INNER JOIN C_InvoiceLine il ON (il.C_Invoice_ID = i.C_Invoice_ID) " +
				" WHERE il.C_InvoiceLine_ID = S_TimeExpenseLine.C_InvoiceLine_ID " +
				" AND i.DocStatus IN ('DR','CO', 'CL', 'IP') " +
				")";
		MTimeExpenseLine expenseLineInvoice = new Query(getCtx(), MTimeExpenseLine.Table_Name, whereClause, get_TrxName())
				.setParameters(get_ID())
				.setOnlyActiveRecords(true)
				.setClient_ID()
				.first();
		if(expenseLineInvoice != null && expenseLineInvoice.get_ID() > 0){
			StringBuilder error = new StringBuilder();
			error.append("@S_TimeExpenseLine_ID@: ");

			MInvoiceLine invoiceLine = new MInvoiceLine(getCtx(), expenseLineInvoice.getC_InvoiceLine_ID(), get_TrxName());
			MInvoice invoice = invoiceLine.getParent();
			error.append("@LineNo@: ").append(expenseLineInvoice.getLine())
				.append(" @C_Invoice_ID@: ").append(invoice.getDocumentNo())
				.append(" @DocStatus@: ").append(invoice.getDocStatus()).append("\n");

			processMsg = error.toString();
			throw new AdempiereException(processMsg);
		}
		setDocStatus(DOCSTATUS_Voided);
		setDocAction(DOCACTION_None);
		addDescription(Msg.getMsg(getCtx(), "Voided"));
		saveEx();
		return this;
	}

	/**
	 * 	Reverse Correction
	 * 	@return false 
	 */
	public boolean reverseCorrectIt()
	{
		log.info(toString());
		// Before reverseCorrect
		processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (processMsg != null)
			return false;
		reverseIt(true);

		// After reverseCorrect
		processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (processMsg != null)
			return false;

		setDocAction(DOCACTION_None);
		return true;
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual - none
	 * 	@return false 
	 */
	public boolean reverseAccrualIt()
	{
		log.info("reverseAccrualIt - " + toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;

		MTimeExpense reversal = reverseIt(true);
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;		
		
		return false;
	}	//	reverseAccrualIt

	@Override
	public boolean isReversal() {
		return false;
	}

	@Override
	public void setReversal(boolean b) {

	}

	/** 
	 * 	Re-activate
	 * 	@return true if success 
	 */
	public boolean reActivateIt()
	{
		log.info("reActivateIt - " + toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;	
		
		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;
		
	//	setProcessed(false);
		return false;
	}	//	reActivateIt
	
	
	/*************************************************************************
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	public String getSummary()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getDocumentNo());
		//	: Total Lines = 123.00 (#1)
		sb.append(": ")
			.append(Msg.translate(getCtx(),"ApprovalAmt")).append("=").append(getApprovalAmt())
			.append(" (#").append(getLines(false).length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary
	
	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
		if (m_AD_User_ID != 0)
			return m_AD_User_ID;
		if (getC_BPartner_ID() != 0)
		{
			MUser[] users = MUser.getOfBPartner(getCtx(), getC_BPartner_ID());
			if (users.length > 0)
			{
				m_AD_User_ID = users[0].getAD_User_ID();
				return m_AD_User_ID;
			}
		}
		return getCreatedBy();
	}	//	getDoc_User_ID

	
	/**
	 * 	Get Document Currency
	 *	@return C_Currency_ID
	 */
	public int getC_Currency_ID()
	{
		MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID(), get_TrxName());
		return pl.getC_Currency_ID();
	}	//	getC_Currency_ID

	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds) 
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete

	@Override
	public int customizeValidActions(String docStatus, Object processing,
									 String orderType, String isSOTrx, int tableId,
									 String[] docAction, String[] options, int index) {
		//	Valid Document Action
		if (Table_ID == tableId) {
			if (docStatus.equals(DocumentEngine.STATUS_Drafted)
					|| docStatus.equals(DocumentEngine.STATUS_InProgress)
					|| docStatus.equals(DocumentEngine.STATUS_Invalid)) {
				options[index++] = DocumentEngine.ACTION_Prepare;
			}
			//	Complete                    ..  CO
			else if (docStatus.equals(DocumentEngine.STATUS_Completed)) {
				options[index++] = DocumentEngine.ACTION_Void;
				options[index++] = DocumentEngine.ACTION_ReActivate;
				options[index++] = DocumentEngine.ACTION_Close;

			} else if (docStatus.equals(DocumentEngine.STATUS_Closed)) {
				options[index++] = DocumentEngine.ACTION_None;
			}
		}
		return index;
	}
}	//	MTimeExpense
