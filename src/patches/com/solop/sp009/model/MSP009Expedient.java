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
package com.solop.sp009.model;

import com.solop.sp009.util.ImportExportUtil;
import org.adempiere.core.domains.models.I_M_MatchInv;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLandedCostAllocation;
import org.compiere.model.MMatchInv;
import org.compiere.model.MOrder;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPayment;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MProject;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MProjectCategory;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/** Generated Model for SP009_Expedient
 *  @author Adempiere (generated) 
 *  @version Release 3.9.3 - $Id$ */
public class MSP009Expedient extends X_SP009_Expedient implements DocAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 20220905L;
	
	
	/**Expedient Tariff Code Details*/
	private List<TariffCodeExpedientTax> tariffCodeExpedient = new ArrayList<TariffCodeExpedientTax>();
	

    /** Standard Constructor */
    public MSP009Expedient (Properties ctx, int SP009_Expedient_ID, String trxName)
    {
      super (ctx, SP009_Expedient_ID, trxName);
    }

    /** Load Constructor */
    public MSP009Expedient (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName() + get_ID() +"_", ".pdf");
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

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		log.info("unlockIt - " + toString());
	//	setProcessing(false);
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
		
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getDateDoc(), dt.getDocBaseType(), getAD_Org_ID(), get_TrxName()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		//	Add up Amounts
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
		log.info(toString());
		//
		
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		if (getC_Project_ID() == 0)
			createProject();
		//	Set Definitive Document No
		setDefiniteDocumentNo();

		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Set the definite document number after completed
	 */
	private void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			setDateDoc(new Timestamp(System.currentTimeMillis()));
		}
		if (dt.isOverwriteSeqOnComplete()) {
			String value = null;
			int index = p_info.getColumnIndex("C_DocType_ID");
			if (index == -1)
				index = p_info.getColumnIndex("C_DocTypeTarget_ID");
			if (index != -1)		//	get based on Doc Type (might return null)
				value = DB.getDocumentNo(get_ValueAsInt(index), get_TrxName(), true);
			if (value != null) {
				setDocumentNo(value);
			}
		}
	}

	/**
	 * 	Void Document.
	 * 	Same as Close.
	 * 	@return true if success 
	 */
	public boolean voidIt()
	{
		log.info("voidIt - " + toString());
		return closeIt();
	}	//	voidIt
	
	/**
	 * 	Close Document.
	 * 	Cancel not delivered Qunatities
	 * 	@return true if success 
	 */
	public boolean closeIt()
	{
		log.info("closeIt - " + toString());

		//	Close Not delivered Qty
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction
	 * 	@return true if success 
	 */
	public boolean reverseCorrectIt()
	{
		log.info("reverseCorrectIt - " + toString());
		return false;
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual - none
	 * 	@return true if success 
	 */
	public boolean reverseAccrualIt()
	{
		log.info("reverseAccrualIt - " + toString());
		return false;
	}	//	reverseAccrualIt
	
	/** 
	 * 	Re-activate
	 * 	@return true if success 
	 */
	public boolean reActivateIt()
	{
		log.info("reActivateIt - " + toString());
		setProcessed(false);
		if (reverseCorrectIt())
			return true;
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
	//	sb.append(": ")
	//		.append(Msg.translate(getCtx(),"TotalLines")).append("=").append(getTotalLines())
	//		.append(" (#").append(getLines(false).length).append(")");
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
	//	return getSalesRep_ID();
		return 0;
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Approval Amount
	 *	@return amount
	 */
	public BigDecimal getApprovalAmt()
	{
		return null;	//getTotalLines();
	}	//	getApprovalAmt
	
    @Override
    public String toString()
    {
      StringBuffer sb = new StringBuffer ("MIEExpedient[")
        .append(getSummary()).append("]");
      return sb.toString();
    }
    
    /**
     * Get Fiscal Currency Precision
     * @return
     */
    private int getPrecision() {
    	return MCurrency.getStdPrecision(getCtx(), getFiscalCurrency_ID());
    }
    
    /**
     * Create Project
     */
    private void createProject() {
		int defaultProjectCategoryId = getDefaultProjectCategoryId();
		if(defaultProjectCategoryId <= 0) {
			throw new AdempiereException("@C_ProjectCategory_ID@ @NotFound@");
		}
    	MProject project = new MProject(getCtx(), 0, get_TrxName());
    	project.setAD_Org_ID(getAD_Org_ID());
		project.setC_ProjectCategory_ID(defaultProjectCategoryId);
    	project.setName(getDocumentNo());
    	project.setValue(getDocumentNo());
    	project.setC_Currency_ID(getC_Currency_ID());
    	project.setProjectLineLevel(MProject.PROJECTLINELEVEL_Project);
		project.set_ValueOfColumn(ImportExportUtil.COLUMNNAME_SP009_IsExpedientProject, true);
    	project.saveEx();
    	setC_Project_ID(project.get_ID());
		saveEx();
    }

	private int getDefaultProjectCategoryId() {
		return new Query(getCtx(), MProjectCategory.Table_Name, null, get_TrxName())
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setOrderBy("IsDefault DESC")
				.firstId();
	}
    
    /**
     * Get Expedient
     * @param ctx
     * @param IE_Expedient_ID
     * @param trxName
     * @return
     */
    public static MSP009Expedient get (Properties ctx, int IE_Expedient_ID, String trxName)
    {
    	return new MSP009Expedient(ctx, IE_Expedient_ID, trxName);
	}	//	get
    
    @Override
    protected boolean beforeSave(boolean newRecord) {
    	if (getDateDoc() == null)
    		setDateDoc(Env.getContextAsDate(getCtx(), "#Date"));
    	Optional<MDocType> maybeDocumentType = Optional.ofNullable(MDocType.get(getCtx(), getC_DocType_ID()));
    	maybeDocumentType.ifPresent(documentType -> {
    		setIsSOTrx(documentType.isSOTrx());
    	});
    	if (getC_Currency_ID() == 0 ) {
    		Optional<MOrgInfo> maybeOrgInfo = Optional.ofNullable(MOrgInfo.get(getCtx(), getAD_Org_ID(), get_TrxName()));
    		maybeOrgInfo.ifPresent(orgInfo -> {
    			if (orgInfo.getFiscalCurrency_ID() > 0)
					setC_Currency_ID(orgInfo.getFiscalCurrency_ID());
    		});
    		if (getFiscalCurrency_ID() == 0) {
				setFiscalCurrency_ID(Env.getContextAsInt(getCtx(), "@$C_Currency_ID@"));
			}
    	}
		if (getFiscalCurrency_ID() <= 0 ) {
			Optional<MOrgInfo> maybeOrgInfo = Optional.ofNullable(MOrgInfo.get(getCtx(), getAD_Org_ID(), get_TrxName()));
			maybeOrgInfo.ifPresent(orgInfo -> {
				if (orgInfo.getFiscalCurrency_ID() > 0)
					setFiscalCurrency_ID(orgInfo.getFiscalCurrency_ID());
			});
			if (getFiscalCurrency_ID() <= 0) {
				throw new AdempiereException("@FiscalCurrency_ID@ @NotFound@");
			}
		}
    	return super.beforeSave(newRecord);
    }
    
    /**
     * Update Order Balance
     */
    public void updateOrderBalance() {
    	String whereClause = MSP009Expedient.COLUMNNAME_SP009_Expedient_ID
    						.concat("=? AND ")
    						.concat(MSP009Expedient.COLUMNNAME_DocStatus)
    						.concat(" IN (?, ?)");
    	Optional<List<MOrder>> maybeOrders = Optional.ofNullable(new Query(getCtx(), MOrder.Table_Name, whereClause, get_TrxName())
    													.setParameters(getSP009_Expedient_ID(), MSP009Expedient.DOCSTATUS_Completed, MSP009Expedient.DOCSTATUS_Closed)
    													.<MOrder>list());
    	setSP009_BaseAmtOrders(Env.ZERO);
    	setSP009_NetAmtOrders(Env.ZERO);
    	setSP009_TaxAmtOrders(Env.ZERO);
    	setSP009_QtyOrders(Env.ZERO);
    	
    	maybeOrders.ifPresent(orders ->{
    		orders.forEach(order ->{
    			updateOrderBalance(order, false, false);
    		});
    	});
    	saveEx();
    }
    
    /**
     * Update Order Balance
     * @param order
     * @param save
     * @param reversal
     */
    public void updateOrderBalance(MOrder order, boolean save, boolean reversal) {
    	final BigDecimal multiplier = (reversal ? Env.ONE.negate() : Env.ONE);
		BigDecimal currencyRate = Optional.ofNullable(MConversionRate.getRate(order.getC_Currency_ID(),
																				getFiscalCurrency_ID(),
																				order.getDateAcct(), 
																				order.getC_ConversionType_ID(), 
																				order.getAD_Client_ID(), 
																				order.getAD_Org_ID())).orElse(Env.ZERO);
		int stdPrecision = getPrecision();
		setSP009_BaseAmtOrders(getSP009_BaseAmtOrders().add(order.getTotalLines().multiply(multiplier).multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP)));
		setSP009_NetAmtOrders(getSP009_NetAmtOrders().add(order.getGrandTotal().multiply(multiplier).multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP)));
		Arrays.asList(order.getTaxes(true))
			  .forEach(tax -> setSP009_TaxAmtOrders(getSP009_TaxAmtOrders().add(tax.getTaxAmt().multiply(multiplier).multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP))));
		setSP009_QtyOrders(getSP009_QtyOrders().add(Env.ONE.multiply(multiplier)));
		
		if (save)
			saveEx();
    }
    
    /**
     * Update Invoice Balance
     */
    public void updateInvoiceBalance() {
    	String whereClause = "EXISTS (SELECT 1 FROM C_InvoiceLine il WHERE C_Invoice.C_Invoice_ID = il.C_Invoice_ID AND COALESCE(il.".concat(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID)
				.concat(",C_Invoice.").concat(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID).concat(")=?) AND ")
				.concat(MSP009Expedient.COLUMNNAME_DocStatus)
				.concat(" IN (?, ?)");
    	Optional<List<MInvoice>> maybeInvoices = Optional.ofNullable(new Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
											    			.setParameters(getSP009_Expedient_ID(), MSP009Expedient.DOCSTATUS_Completed, MSP009Expedient.DOCSTATUS_Closed)
															.<MInvoice>list());
    	
    	
    	setSP009_BaseAmtInvoices(Env.ZERO);
    	setSP009_NetAmtInvoices(Env.ZERO);
    	setSP009_TaxAmtInvoices(Env.ZERO);
    	setSP009_QtyInvoices(Env.ZERO);
    	setTaxAmt(Env.ZERO);
    	
    	maybeInvoices.ifPresent(invoices ->{
    		invoices.forEach(invoice -> updateInvoiceBalance(invoice, false, false));
		});
    	saveEx();
    	processTariffCodeExpedientTax(true);
    }
    
    /**
     * Update Invoice Balance
     * @param invoice
     * @param save
     * @param reversal
     */
    public void updateInvoiceBalance(MInvoice invoice, boolean save, boolean reversal) {
    	if (!invoice.get_ValueAsBoolean(ImportExportUtil.COLUMNNAME_SP009_IsAffectsExpedientTax))
    		return;
    	BinaryOperator<BigDecimal> _sum = (prevousValue, newValue) -> prevousValue.add(newValue);
		BigDecimal currencyRate = Optional.ofNullable(MConversionRate.getRate(invoice.getC_Currency_ID(), 
														getFiscalCurrency_ID(),
														invoice.getDateAcct(), 
														invoice.getC_ConversionType_ID(), 
														invoice.getAD_Client_ID(), 
														invoice.getAD_Org_ID())).orElse(Env.ZERO);
		int stdPrecision = getPrecision();
		AtomicReference<BigDecimal>  currentBaseAmt =  new AtomicReference<BigDecimal>(Env.ZERO);
		AtomicReference<BigDecimal>  currentNetAmt =  new AtomicReference<BigDecimal>(Env.ZERO);
		AtomicReference<BigDecimal>  currentTaxAmt =  new AtomicReference<BigDecimal>(Env.ZERO);
		boolean expedientTax = invoice.get_ValueAsBoolean(ImportExportUtil.COLUMNNAME_SP009_IsExpedientTax);
		final BigDecimal multiplier = (invoice.isCreditMemo() ? Env.ONE.negate() : Env.ONE).multiply(reversal ? Env.ONE.negate() : Env.ONE);
		if (!expedientTax) {
			Arrays.asList(invoice.getLines()).forEach(invoiceLine ->{
				if (invoiceLine.get_ValueAsInt(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID) == getSP009_Expedient_ID()
						|| (invoiceLine.get_ValueAsInt(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID) == 0 
								&& invoice.get_ValueAsInt(MSP009Expedient.COLUMNNAME_SP009_Expedient_ID) == getSP009_Expedient_ID())) {
					currentBaseAmt.accumulateAndGet(invoiceLine.getLineNetAmt().multiply(multiplier), _sum);
					currentNetAmt.accumulateAndGet(invoiceLine.getLineTotalAmt().multiply(multiplier), _sum);
					currentTaxAmt.accumulateAndGet(invoiceLine.getTaxAmt().multiply(multiplier), _sum);
					if (invoiceLine.getM_Product_ID() > 0) {
						MProduct product = MProduct.get(getCtx(), invoiceLine.getM_Product_ID());
						if (product !=null
								&& product.get_ID() > 0
									&& product.get_ValueAsInt(X_SP009_TariffCode.COLUMNNAME_SP009_TariffCode_ID) > 0) {
							tariffCodeExpedient.add(TariffCodeExpedientTax.newInstance()
																		  .withTariffCode(product.get_ValueAsInt(X_SP009_TariffCode.COLUMNNAME_SP009_TariffCode_ID))
																		  .withSourceInvoice(invoiceLine.getC_Invoice_ID())
																		  .withSourceInvoiceLine(invoiceLine.get_ID())
																		  .withBaseAmount(invoiceLine.getLineNetAmt().multiply(multiplier))
																		  .withCurrencyRate(currencyRate));
						}
					}
					
					Arrays
						.asList(MLandedCostAllocation.getOfInvoiceLine(getCtx(), invoiceLine.get_ID(), get_TrxName()))
						.forEach(landedCostAllocation -> {
							MProduct product = MProduct.get(getCtx(), landedCostAllocation.getM_Product_ID());
							if (product !=null
									&& product.get_ID() > 0
										&& product.get_ValueAsInt(X_SP009_TariffCode.COLUMNNAME_SP009_TariffCode_ID) > 0) {
								new Query(getCtx(), I_M_MatchInv.Table_Name, I_M_MatchInv.COLUMNNAME_M_InOutLine_ID + "=?", get_TrxName())
									.setParameters(landedCostAllocation.getM_InOutLine_ID())
									.<MMatchInv>list()
									.forEach(matchinv -> {
										MInvoiceLine invoiceLineMatch = new MInvoiceLine(getCtx(), matchinv.getC_InvoiceLine_ID(), get_TrxName());
										tariffCodeExpedient.add(TariffCodeExpedientTax.newInstance()
												  .withTariffCode(product.get_ValueAsInt(X_SP009_TariffCode.COLUMNNAME_SP009_TariffCode_ID))
												  .withSourceInvoice(invoiceLine.getC_Invoice_ID())
												  .withSourceInvoiceLine(invoiceLine.get_ID())
												  .withBaseAmount(landedCostAllocation.getAmt().multiply(multiplier))
												  .withCurrencyRate(currencyRate)
												  .withTargetInvoice(invoiceLineMatch.getC_Invoice_ID())
												  .withTargetInvoiceLine(invoiceLineMatch.get_ID()));
										
									});
								}
						});
				}
			});
			setSP009_BaseAmtInvoices(getSP009_BaseAmtInvoices().add(currentBaseAmt.get().multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP)));
			setSP009_NetAmtInvoices(getSP009_NetAmtInvoices().add(currentNetAmt.get().multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP)));
			setSP009_TaxAmtInvoices(getSP009_TaxAmtInvoices().add(currentTaxAmt.get().multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP)));
			setSP009_QtyInvoices(getSP009_QtyInvoices().add(Env.ONE.multiply(reversal ? Env.ONE.negate(): Env.ONE)));
		}else {
			AtomicReference<BigDecimal> taxAmt = new AtomicReference<>(Env.ZERO);
			BinaryOperator<BigDecimal> sum = (BigDecimal previousValue, BigDecimal nextValue) -> previousValue.add(nextValue);
			Arrays.asList(invoice.getLines()).forEach(invoiceLine ->{
				BigDecimal invoiceTaxAmount = Optional.ofNullable((BigDecimal)invoiceLine.get_Value("SP009_TaxAmount")).orElse(Env.ZERO);
				if (invoiceTaxAmount.compareTo(Env.ZERO) == 0)
					invoiceTaxAmount = Optional.ofNullable(invoiceLine.getLineTotalAmt()).orElse(Env.ZERO);
				
				taxAmt.accumulateAndGet(invoiceTaxAmount, sum);
			});
			setTaxAmt(getTaxAmt().add(taxAmt.get().multiply(multiplier).multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP)));
		}
		if (save) {
			saveEx();
			processTariffCodeExpedientTax(false);
		}
    }
    
    /**
     * Process tariff code Expedient Tax
     */
    private void processTariffCodeExpedientTax(boolean clearlOldValues) {
    	if (clearlOldValues) {
    		new Query(getCtx(), X_SP009_Expedient_TC.Table_Name, COLUMNNAME_SP009_Expedient_ID.concat("=?"), get_TrxName())
    			.setParameters(getSP009_Expedient_ID())
    			.<X_SP009_Expedient_TC>list()
    			.forEach(tariffCodeSummary ->{
    				tariffCodeSummary.setSP009_BaseAmtInvoices(Env.ZERO);
    				tariffCodeSummary.saveEx();
    			});
    			
			new Query(getCtx(), X_SP009_Expedient_TC_Detail.Table_Name, COLUMNNAME_SP009_Expedient_ID.concat("=?"), get_TrxName())
    			.setParameters(getSP009_Expedient_ID())
    			.<X_SP009_Expedient_TC_Detail>list()
    			.forEach(tariffCodeSummary ->{
    				tariffCodeSummary.setSP009_BaseAmtInvoices(Env.ZERO);
    				tariffCodeSummary.saveEx();
    			});
    	}
    	int stdPrecision = getPrecision();
    	tariffCodeExpedient
    	.stream()
    	.sorted(Comparator.comparing(TariffCodeExpedientTax::getTariffCodeId))
    	.collect(Collectors.groupingBy(tariffCode -> tariffCode.tariffCodeId,
    				Collectors.summingDouble(tariffCode -> tariffCode.baseAmount.multiply(tariffCode.currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP).doubleValue()))
    			)
    	.entrySet()
    	.forEach(tariffCodeSummary -> {
    		X_SP009_Expedient_TC expedientTariffCodeSummary = Optional.ofNullable(getExpedientTariffCode(tariffCodeSummary.getKey()))
    																	   .orElseGet(() ->{
    																		   X_SP009_Expedient_TC tariffCodeExpedientSummary = new X_SP009_Expedient_TC(getCtx(), 0, get_TrxName());
    																		   tariffCodeExpedientSummary.setSP009_Expedient_ID(get_ID());
    																		   tariffCodeExpedientSummary.setSP009_TariffCode_ID(tariffCodeSummary.getKey());
    																		   tariffCodeExpedientSummary.setSP009_BaseAmtInvoices(Env.ZERO);
    																		   return tariffCodeExpedientSummary;
    																	   });
    		if (expedientTariffCodeSummary!=null) {
    			expedientTariffCodeSummary.setSP009_BaseAmtInvoices(expedientTariffCodeSummary.getSP009_BaseAmtInvoices().add(new BigDecimal(tariffCodeSummary.getValue()).setScale(stdPrecision, RoundingMode.HALF_UP)));
    			expedientTariffCodeSummary.saveEx();
    		}
    	});
    	
    	tariffCodeExpedient
    	.stream()
    	.sorted(Comparator.comparing(TariffCodeExpedientTax::getTariffCodeId))
    	.forEach(tariffCodeExpedientObj -> {
    		X_SP009_Expedient_TC_Detail expedientTariffCodeDetail = Optional.ofNullable(getExpedientTariffCodeDetail(tariffCodeExpedientObj.tariffCodeId, tariffCodeExpedientObj.sourceInvoiceLineId))
					   														.orElseGet(() ->{
				   																X_SP009_Expedient_TC_Detail tariffCodeExpedientDetail = new X_SP009_Expedient_TC_Detail(getCtx(), 0, get_TrxName());
				   																tariffCodeExpedientDetail.setSP009_Expedient_ID(get_ID());
				   																tariffCodeExpedientDetail.setSP009_TariffCode_ID(tariffCodeExpedientObj.tariffCodeId);
				   																tariffCodeExpedientDetail.setSP009_SourceInvoice_ID(tariffCodeExpedientObj.sourceInvoiceId);
				   																tariffCodeExpedientDetail.setSP009_SourceInvoiceLine_ID(tariffCodeExpedientObj.sourceInvoiceLineId);
				   																tariffCodeExpedientDetail.setSP009_TargetInvoice_ID(tariffCodeExpedientObj.targetInvoiceId);
				   																tariffCodeExpedientDetail.setSP009_TargetInvoiceLine_ID(tariffCodeExpedientObj.targetInvoiceLineId);
				   																tariffCodeExpedientDetail.setSP009_BaseAmtInvoices(Env.ZERO);
				   																
				   																return tariffCodeExpedientDetail;
																		   });
			if (expedientTariffCodeDetail!=null) {
				BigDecimal amount = tariffCodeExpedientObj.baseAmount.multiply(tariffCodeExpedientObj.currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP);
				expedientTariffCodeDetail.setSP009_BaseAmtInvoices(expedientTariffCodeDetail.getSP009_BaseAmtInvoices().add(amount));
				expedientTariffCodeDetail.saveEx();
			}
    	});
    	
    	tariffCodeExpedient.clear();
    }
    
    
    /**
     * Update Payment Balance
     */
    public void updatePaymentBalance() {
    	String whereClause = MSP009Expedient.COLUMNNAME_SP009_Expedient_ID
				.concat("=? AND ")
				.concat(MSP009Expedient.COLUMNNAME_DocStatus)
				.concat(" IN (?, ?)");
    	Optional<List<MPayment>> maybePayments = Optional.ofNullable(new Query(getCtx(), MPayment.Table_Name, whereClause, get_TrxName())
											    			.setParameters(getSP009_Expedient_ID(), MSP009Expedient.DOCSTATUS_Completed, MSP009Expedient.DOCSTATUS_Closed)
															.<MPayment>list());
    	setSP009_PaymentsAmount(Env.ZERO);
    	setSP009_QtyPayments(Env.ZERO);
    	maybePayments.ifPresent(payments ->{
    		payments.forEach(payment ->{
    			updatePaymentBalance(payment, false, false);
			});
		});    	
    	
    	saveEx();
    }
    
    /**
     * Update Payment Balance
     * @param payment
     * @param save
     * @param reversal
     */
    public void updatePaymentBalance(MPayment payment, boolean save, boolean reversal) {
    	final BigDecimal multiplier = (reversal ? Env.ONE.negate() : Env.ONE);
		BigDecimal currencyRate = Optional.ofNullable(MConversionRate.getRate(payment.getC_Currency_ID(), 
														getFiscalCurrency_ID(),
														payment.getDateAcct(), 
														payment.getC_ConversionType_ID(), 
														payment.getAD_Client_ID(), 
														payment.getAD_Org_ID())).orElse(Env.ZERO);
		int stdPrecision = getPrecision();
		setSP009_PaymentsAmount(getSP009_PaymentsAmount().add(payment.getPayAmt().multiply(multiplier).multiply(currencyRate).setScale(stdPrecision, RoundingMode.HALF_UP)));
		setSP009_QtyPayments(getSP009_QtyPayments().add(Env.ONE.multiply(multiplier)));
		
	
    	if (save)
    		saveEx();
    }
    
    /**
     * Get Expedient Tariff Code Summary by Tariff Code
     * @param tariffCodeId
     * @return
     */
	private  X_SP009_Expedient_TC getExpedientTariffCode(int tariffCodeId) {
		String whereClause = X_SP009_Expedient_TC.COLUMNNAME_SP009_Expedient_ID.concat("=? AND ")
							.concat(X_SP009_Expedient_TC.COLUMNNAME_SP009_TariffCode_ID).concat("=?");
		
		return new Query(getCtx(), X_SP009_Expedient_TC.Table_Name, whereClause, get_TrxName())
				.setParameters(get_ID(), tariffCodeId)
				.<X_SP009_Expedient_TC>first();
	}
	
	/**
	 * Get Expedient Tariff Code Detail
	 * @param tariffCodeId
	 * @param sourceinvoiceLineId
	 * @return
	 */
	private  X_SP009_Expedient_TC_Detail getExpedientTariffCodeDetail(int tariffCodeId, int sourceinvoiceLineId) {
		String whereClause = X_SP009_Expedient_TC_Detail.COLUMNNAME_SP009_Expedient_ID.concat("=? AND ")
							.concat(X_SP009_Expedient_TC_Detail.COLUMNNAME_SP009_TariffCode_ID).concat("=? AND ")
							.concat(X_SP009_Expedient_TC_Detail.COLUMNNAME_SP009_SourceInvoiceLine_ID).concat("=?");

		return new Query(getCtx(), X_SP009_Expedient_TC_Detail.Table_Name, whereClause, get_TrxName())
					.setParameters(get_ID(), tariffCodeId, sourceinvoiceLineId)
			.<X_SP009_Expedient_TC_Detail>first();
	}
}

/**
 *	Tariff Code Details by Expedient
 */
class TariffCodeExpedientTax {
	int sourceInvoiceId = 0;
	int tariffCodeId  = 0;
	int targetInvoiceLineId = 0;
	int targetInvoiceId = 0;
	int sourceInvoiceLineId = 0;
	BigDecimal baseAmount = Env.ZERO;
	BigDecimal currencyRate = Env.ZERO;
	
	/**
	 * Get Tariff Code
	 * @return
	 */
	public int getTariffCodeId() {
		return tariffCodeId;
	}
	
	/**
	 * Get Source InvoiceLine
	 * @return
	 */
	public int getSourceInvoiceLineId() {
		return sourceInvoiceLineId;
	}
	
	/**
	 * Get Targe Invoice Line
	 * @return
	 */
	public int getTargetInvoiceLineId() {
		return targetInvoiceLineId;
	}
	
	/**
	 * Get source invoice ID
	 * @return
	 */
	public int getSourceInvoiceId() {
		return sourceInvoiceId;
	}
	
	
	/**
	 * Get Target Invoice
	 * @return
	 */
	public int getTargetInvoiceId() {
		return targetInvoiceId;
	}
	
	/**
	 * Create New Instance
	 * @return
	 */
	public static TariffCodeExpedientTax newInstance() {
		return new TariffCodeExpedientTax();
	}
	
	/**
	 * Constructor
	 */
	private TariffCodeExpedientTax() {
	}
	
	/**
	 * With Tariff Code Identifier
	 * @param tariffCodeId
	 * @return
	 */
	public TariffCodeExpedientTax withTariffCode(int tariffCodeId) {
		this.tariffCodeId = tariffCodeId;
		return this;
	}
	
	/**
	 * With Source Invoice
	 * @param sourceInvoiceId
	 * @return
	 */
	public TariffCodeExpedientTax withSourceInvoice(int sourceInvoiceId) {
		this.sourceInvoiceId = sourceInvoiceId;
		return this;
	}
	
	/**
	 * With Target Invoice
	 * @param targetInvoiceId
	 * @return
	 */
	public TariffCodeExpedientTax withTargetInvoice(int targetInvoiceId) {
		this.targetInvoiceId = targetInvoiceId;
		return this;
	}
	
	/**
	 * With Invoice Line Identifier
	 * @param invoiceLineId
	 * @return
	 */
	public TariffCodeExpedientTax withSourceInvoiceLine(int sourceInvoiceLineId) {
		this.sourceInvoiceLineId = sourceInvoiceLineId;
		return this;
	}
	
	/**
	 * With Target Invoice Line
	 * @param targetInvoiceLineId
	 * @return
	 */
	public TariffCodeExpedientTax withTargetInvoiceLine(int targetInvoiceLineId) {
		this.targetInvoiceLineId = targetInvoiceLineId;
		return this;
	}
	
	
	/**
	 * With Base Amount
	 * @param baseAmount
	 * @return
	 */
	public TariffCodeExpedientTax withBaseAmount(BigDecimal baseAmount) {
		this.baseAmount = baseAmount;
		return this;
	}
	
	/**
	 * With Currency rate
	 * @param currencyRate
	 * @return
	 */
	public TariffCodeExpedientTax withCurrencyRate(BigDecimal currencyRate) {
		this.currencyRate = currencyRate;
		return this;
	}
	
	@Override
	public String toString() {
		return  "tariffCodeId = "
				.concat(String.valueOf(Optional.ofNullable(tariffCodeId).orElse(0)))
				.concat(", sourceInvoiceId=")
				.concat(String.valueOf(Optional.ofNullable(sourceInvoiceId).orElse(0)))
				.concat(", sourceInvoiceLineId=")
				.concat(String.valueOf(Optional.ofNullable(sourceInvoiceLineId).orElse(0)))
				.concat(", baseAmount=")
				.concat(String.valueOf(Optional.ofNullable(baseAmount).orElse(Env.ZERO)))
				.concat(", currencyRate=")
				.concat(String.valueOf(Optional.ofNullable(currencyRate).orElse(Env.ZERO)))
				.concat(", targetInvoiceId=")
				.concat(String.valueOf(Optional.ofNullable(targetInvoiceId).orElse(0)))
				.concat(", targetInvoiceLineId=")
				.concat(String.valueOf(Optional.ofNullable(targetInvoiceLineId).orElse(0)))
				;
	}
}