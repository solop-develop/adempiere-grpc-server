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

import org.adempiere.core.domains.models.I_C_ProjectLine;
import org.adempiere.core.domains.models.I_C_ProjectPhase;
import org.adempiere.core.domains.models.I_C_ProjectTask;
import org.adempiere.core.domains.models.X_C_ProjectLine;
import org.adempiere.core.domains.models.X_C_ProjectLineType;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.eevolution.hr.model.MHREmployee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * 	Project Line Model
 *
 *	@author Jorg Janke
 *	@version $Id: MProjectLine.java,v 1.3 2006/07/30 00:51:02 jjanke Exp $
 */
public class MProjectLine extends X_C_ProjectLine
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2668549463273628848L;

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_ProjectLine_ID id
	 *	@param trxName transaction
	 */
	public MProjectLine(Properties ctx, int C_ProjectLine_ID, String trxName)
	{
		super (ctx, C_ProjectLine_ID, trxName);
		if (C_ProjectLine_ID == 0)
		{
		//  setC_Project_ID (0);
		//	setC_ProjectLine_ID (0);
			setLine (0);
			setIsPrinted(true);
			setProcessed(false);
			setInvoicedAmt (Env.ZERO);
			setInvoicedQty (Env.ZERO);
			//
			setPlannedAmt (Env.ZERO);
			setPlannedMarginAmt (Env.ZERO);
			setPlannedPrice (Env.ZERO);
			setPlannedQty (Env.ONE);
		}
	}	//	MProjectLine

	/**
	 * 	Load Constructor
	 * 	@param ctx context
	 * 	@param rs result set
	 *	@param trxName transaction
	 */
	public MProjectLine(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProjectLine

	/**
	 * 	Parent Constructor
	 *	@param project parent
	 */
	public MProjectLine(MProject project)
	{
		this (project.getCtx(), 0, project.get_TrxName());
		setClientOrg(project);
		setC_Project_ID (project.getC_Project_ID());	// Parent
		setLine();
	}	//	MProjectLine

	/**
	 * Constructor for create line from Standard Project Line
	 * @param project
	 * @param stdPLine
	 */
	public MProjectLine(MProject project, MStandardProjectLine stdPLine) {
		this(project.getCtx(), 0, project.get_TrxName());
		setClientOrg(project);
		setC_Project_ID(project.getC_Project_ID());
		setLine(stdPLine.getSeqNo());
		setC_StandardProjectLine_ID(stdPLine.getC_StandardProjectLine_ID());
		setC_ProjectLineType_ID(stdPLine.getC_ProjectLineType_ID());
		setName(stdPLine.getName());
		setDescription(stdPLine.getDescription());
		setHelp(stdPLine.getHelp());
		setIsSummary(stdPLine.isSummary());
		setPriorityRule(stdPLine.getPriorityRule());
		setDurationUnit(stdPLine.getDurationUnit());
		setDurationEstimated(stdPLine.getDurationEstimated());
		setIsIndefinite(stdPLine.isIndefinite());
		setIsMilestone(stdPLine.isMilestone());
		setIsRecurrent(stdPLine.isRecurrent());
		if (isRecurrent()) {
			setFrequencyType(stdPLine.getFrequencyType());
			setFrequency(stdPLine.getFrequency());
			setRunsMax(stdPLine.getRunsMax());
		}
		
		if (getM_Product_ID()!=0) {
			setProjInvoiceRule(MProjectLine.PROJINVOICERULE_ProductQuantity);
			setM_Product_ID(stdPLine.getM_Product_ID());
			setPlannedQty(stdPLine.getStandardQty());
		}
		
		
	}
	/** Parent				*/
	private MProject	m_parent = null;
	
	/**
	 *	Get the next Line No
	 */
	private void setLine()
	{
		setLine(DB.getSQLValue(get_TrxName(), 
			"SELECT COALESCE(MAX(Line),0)+10 FROM C_ProjectLine WHERE C_Project_ID=?", getC_Project_ID()));
	}	//	setLine

	/**
	 * 	Set Product, committed qty, etc.
	 *	@param pi project issue
	 */
	public void setMProjectIssue (MProjectIssue pi)
	{
		setC_ProjectIssue_ID(pi.getC_ProjectIssue_ID());
		setM_Product_ID(pi.getM_Product_ID());
		setCommittedQty(pi.getMovementQty());
		if (getDescription() != null)
			setDescription(pi.getDescription());
	}	//	setMProjectIssue

	/**
	 *	Set PO
	 *	@param C_OrderPO_ID po id
	 */
	public void setC_OrderPO_ID (int C_OrderPO_ID)
	{
		super.setC_OrderPO_ID(C_OrderPO_ID);
	}	//	setC_OrderPO_ID

	/**
	 * 	Get Project
	 *	@return parent
	 */
	public MProject getProject()
	{
		if (m_parent == null && getC_Project_ID() != 0)
		{
			m_parent = new MProject (getCtx(), getC_Project_ID(), get_TrxName());
			if (get_TrxName() != null)
				m_parent.load(get_TrxName());
		}
		return m_parent;
	}	//	getProject
	
	/**
	 * 	Get Limit Price if exists
	 *	@return limit
	 */
	public BigDecimal getLimitPrice()
	{
		BigDecimal limitPrice = getPlannedPrice();
		if (getM_Product_ID() == 0)
			return limitPrice;
		if (getProject() == null)
			return limitPrice;
		boolean isSOTrx = true;
		MProductPricing pp = new MProductPricing (getM_Product_ID(),
			m_parent.getC_BPartner_ID(), getPlannedQty(), isSOTrx, null);
		pp.setM_PriceList_ID(m_parent.getM_PriceList_ID());
		if (pp.calculatePrice())
			limitPrice = pp.getPriceLimit();
		return limitPrice;
	}	//	getLimitPrice
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuffer sb = new StringBuffer ("MProjectLine[");
			sb.append (get_ID()).append ("-")
				.append (getName())
				.append (","+ getLine())
				.append(",C_Project_ID=").append(getC_Project_ID())
				.append(",C_ProjectPhase_ID=").append(getC_ProjectPhase_ID())
				.append(",C_ProjectTask_ID=").append(getC_ProjectTask_ID())
				.append(",C_ProjectIssue_ID=").append(getC_ProjectIssue_ID())
				.append(", M_Product_ID=").append(getM_Product_ID())
				.append(", PlannedQty=").append(getPlannedQty())
				.append ("]");
		return sb.toString ();
	}	//	toString
	
	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		if (getLine() == 0)
			setLine();
		if(newRecord || is_ValueChanged("C_ProjectLineType_ID")) {
			if(getC_ProjectLineType_ID() > 0) {
				X_C_ProjectLineType type = new X_C_ProjectLineType(getCtx(), getC_ProjectLineType_ID(), get_TrxName());
				String lineType = type.get_ValueAsString("ProjectLineType");
				set_ValueOfColumn("ProjectLineType", lineType);
				if(lineType != null) {
					setIsSummary(lineType.equals("P") || lineType.equals("M"));
				}
				if(isSummary()) {
					set_ValueOfColumn("Parent_ID", null);
				}
				if(type.get_ValueAsString("ProjInvoiceRule") != null) {
					set_ValueOfColumn("ProjInvoiceRule", type.get_ValueAsString("ProjInvoiceRule"));
				}
			}
		}
		MProject project = MProject.getById(getCtx(), getC_Project_ID(), get_TrxName());
		if(newRecord) {
			if(project != null) {
				setIsCostBased(project.isCostBased());
				if(getStartDate() == null) {
					setStartDate(project.getDateStart());
				}
				if(getEndDate() == null) {
					setEndDate(project.getDateFinish());
				}
			}
		}
		if(newRecord || is_ValueChanged("M_Product_ID")) {
			if(project != null && project.getM_PriceList_ID() > 0) {
				if(Optional.ofNullable(getPlannedPrice()).orElse(Env.ZERO).compareTo(Env.ZERO) == 0) {
					MProductPricing pp = new MProductPricing (getM_Product_ID(), project.getC_BPartner_ID(), Env.ZERO, true, null);
					pp.setM_PriceList_ID(project.getM_PriceList_ID());
					pp.setPriceDate(TimeUtil.getDay(System.currentTimeMillis()));
					setPlannedPrice(pp.getPriceStd());
				}
			}
			if(Optional.ofNullable(getPlannedQty()).orElse(Env.ZERO).compareTo(Env.ZERO) == 0) {
				BigDecimal quantity = (BigDecimal) get_Value("QtyEntered");
				setPlannedQty(Optional.ofNullable(quantity).orElse(Env.ZERO));
			}
			if(get_ValueAsInt("C_UOM_ID") <= 0 && getM_Product_ID() > 0) {
				MProduct product = MProduct.get(getCtx(), getM_Product_ID());
				set_ValueOfColumn("C_UOM_ID", product.getC_UOM_ID());
			}
		}
		//	Planned Amount
		if (isCostBased()) {
			if(!isSummary()){
				BigDecimal costAmount = null;
				if (is_ValueChanged(COLUMNNAME_Ref_BPartner_ID) && getRef_BPartner_ID() > 0) {
					MHREmployee employee = MHREmployee.getActiveEmployee(getCtx(), getRef_BPartner_ID(), get_TrxName());
					BigDecimal rate;
					MUOM uom = (MUOM) getC_UOM();
					String uomCode = uom.getX12DE355();

					if (employee.getDailySalary().signum() > 0) {
						rate = BigDecimal.valueOf(8);
						if (uomCode.equals(MUOM.X12_DAY)) {
							rate = BigDecimal.ONE;
						}
						costAmount = employee.getDailySalary().divide(rate, RoundingMode.HALF_UP);

					} else if (employee.getMonthlySalary().signum() > 0) {
						rate = BigDecimal.valueOf(240);
						if (uomCode.equals(MUOM.X12_DAY)) {
							rate = BigDecimal.valueOf(30);
						}
						costAmount = employee.getMonthlySalary().divide(rate, RoundingMode.HALF_UP);
					}
				}
				if (is_ValueChanged(COLUMNNAME_S_ResourceType_ID) && getS_ResourceType_ID() > 0 && costAmount == null) {
					MResourceType resourceType = (MResourceType) getS_ResourceType();
					MProductPricing productPrice = new MProductPricing (resourceType.getS_DefaultProduct_ID(),
				0, BigDecimal.ONE, false, get_TrxName());
					productPrice.setM_PriceList_ID(resourceType.getPO_PriceList_ID());
					productPrice.setPriceDate(project.getDateStart());
					productPrice.calculatePrice();
					costAmount = productPrice.getPriceStd();

				}
				if (costAmount == null) {
					costAmount = getCost();
				}
				setCost(costAmount);


				BigDecimal calculatedAmt = BigDecimal.ZERO;
				if (getCost().signum() > 0) {
					calculatedAmt = (BigDecimal.valueOf(1+(getMargin().doubleValue()/100))).multiply(costAmount);
				}
				setPlannedPrice(calculatedAmt);
				setPlannedAmt(getPlannedQty().multiply(calculatedAmt));
			}
		} else {
			setPlannedAmt(getPlannedQty().multiply(getPlannedPrice()));
		}
		
		//	Planned Margin
		if (is_ValueChanged("M_Product_ID") || is_ValueChanged("M_Product_Category_ID")
			|| is_ValueChanged("PlannedQty") || is_ValueChanged("PlannedPrice"))
		{
			if (getM_Product_ID() != 0 && getProject().getM_PriceList_ID() > 0)
			{
				BigDecimal marginEach = getPlannedPrice().subtract(getLimitPrice());
				setPlannedMarginAmt(marginEach.multiply(getPlannedQty()));
			}
			else if (getM_Product_Category_ID() != 0)
			{
				MProductCategory category = MProductCategory.get(getCtx(), getM_Product_Category_ID());
				BigDecimal marginEach = category.getPlannedMargin();
				setPlannedMarginAmt(marginEach.multiply(getPlannedQty()));
			}
		}
		
		//	Phase/Task
		if (is_ValueChanged("C_ProjectTask_ID") && getC_ProjectTask_ID() != 0)
		{
			MProjectTask pt = new MProjectTask(getCtx(), getC_ProjectTask_ID(), get_TrxName());
			if (pt == null || pt.get_ID() == 0)
			{
				log.warning("Project Task Not Found - ID=" + getC_ProjectTask_ID());
				return false;
			}
			else
				setC_ProjectPhase_ID(pt.getC_ProjectPhase_ID());
		}
		if (is_ValueChanged("C_ProjectPhase_ID") && getC_ProjectPhase_ID() != 0)
		{
			MProjectPhase pp = new MProjectPhase(getCtx(), getC_ProjectPhase_ID(), get_TrxName());
			if (pp == null || pp.get_ID() == 0)
			{
				log.warning("Project Phase Not Found - " + getC_ProjectPhase_ID());
				return false;
			}
			else
				setC_Project_ID(pp.getC_Project_ID());
		}
		
		return true;
	}	//	beforeSave
	
		
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return success
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (getParent_ID() > 0) {
			updateSummaryLine();
		}
		updateHeader();
		return success;
	}	//	afterSave
	
	
	/**
	 * 	After Delete
	 *	@param success success
	 *	@return success
	 */
	protected boolean afterDelete (boolean success)
	{
		if (getParent_ID() > 0) {
			updateSummaryLine();
		}
		updateHeader();
		return success;
	}	//	afterDelete
	
	/**
	 * 	Update Header
	 */
	private void updateHeader()
	{
		String sql = "UPDATE C_Project p "
			+ "SET (PlannedAmt,PlannedQty,PlannedMarginAmt,"
				+ "	CommittedAmt,CommittedQty,"
				+ " InvoicedAmt, InvoicedQty) = "
				+ "(SELECT COALESCE(SUM(pl.PlannedAmt),0),COALESCE(SUM(pl.PlannedQty),0),COALESCE(SUM(pl.PlannedMarginAmt),0),"
				+ " COALESCE(SUM(pl.CommittedAmt),0),COALESCE(SUM(pl.CommittedQty),0),"
				+ " COALESCE(SUM(pl.InvoicedAmt),0), COALESCE(SUM(pl.InvoicedQty),0) "
				+ "FROM C_ProjectLine pl "
				+ "WHERE pl.C_Project_ID=p.C_Project_ID AND pl.IsActive='Y' AND pl.Parent_ID IS NULL) "
			+ "WHERE C_Project_ID=" + getC_Project_ID();
		int no = DB.executeUpdate(sql, get_TrxName());
		if (no != 1)
			log.log(Level.SEVERE, "updateHeader project - #" + no);
		/*onhate + globalqss BF 3060367*/
		if (getC_ProjectPhase_ID() != 0) {
			sql ="UPDATE C_ProjectPhase x SET " +
				"	(PlannedAmt, CommittedAmt) = " +
				"(SELECT " +
				"	COALESCE(SUM(l.PlannedAmt),0), " +
				"	COALESCE(SUM(l.CommittedAmt),0) " +
				"FROM C_ProjectLine l " +
				"WHERE l.C_Project_ID=x.C_Project_ID " +
				"  AND l.C_ProjectPhase_ID=x.C_ProjectPhase_ID " +
				"  AND l.IsActive='Y') " +
				"WHERE x.C_Project_ID=" + getC_Project_ID() +
				"  AND x.C_ProjectPhase_ID=" + getC_ProjectPhase_ID();
			no = DB.executeUpdate(sql, get_TrxName());
			if (no != 1)
				log.log(Level.SEVERE, "updateHeader project phase - #" + no);
		}
		if (getC_ProjectTask_ID() != 0) {
			sql = "UPDATE C_ProjectTask x SET " +
					"	(PlannedAmt, CommittedAmt) = " +
					"(SELECT " +
					"	COALESCE(SUM(l.PlannedAmt),0), " +
					"	COALESCE(SUM(l.CommittedAmt),0) " +
					"FROM C_ProjectLine l " +
					"WHERE l.C_ProjectPhase_ID=x.C_ProjectPhase_ID " +
					"  AND l.C_ProjectTask_ID=x.C_ProjectTask_ID " +
					"  AND l.IsActive='Y') " +
					"WHERE x.C_ProjectPhase_ID=" + getC_ProjectPhase_ID() + 
					"  AND x.C_ProjectTask_ID=" + getC_ProjectTask_ID();
			no = DB.executeUpdate(sql, get_TrxName());
			if (no != 1)
				log.log(Level.SEVERE, "updateHeader project task - #" + no);
		}
		/*onhate + globalqss BF 3060367*/		
	} // updateHeader


	private void updateSummaryLine() {
		MProjectLine summaryLine = (MProjectLine) getParent();
		BigDecimal plannedAmt = BigDecimal.ZERO,
				plannedQty = BigDecimal.ZERO,
				plannedMarginAmt = BigDecimal.ZERO,
				committedAmt = BigDecimal.ZERO,
				committedQty = BigDecimal.ZERO,
				invoicedAmt = BigDecimal.ZERO,
				invoicedQty = BigDecimal.ZERO;

		for (Integer childId : summaryLine.getChildrenIds()) {
			MProjectLine child = new MProjectLine(getCtx(), childId, get_TrxName());
			plannedAmt = plannedAmt.add(child.getPlannedAmt());
			plannedQty = plannedQty.add(child.getPlannedQty());
			plannedMarginAmt = plannedMarginAmt.add(child.getPlannedMarginAmt());
			committedAmt = committedAmt.add(child.getCommittedAmt());
			committedQty = committedQty.add(child.getCommittedQty());
			invoicedAmt = invoicedAmt.add(child.getInvoicedAmt());
			invoicedQty = invoicedQty.add(child.getInvoicedQty());
		}
		summaryLine.setPlannedPrice(plannedAmt.divide(plannedQty, RoundingMode.HALF_UP));
		summaryLine.setPlannedAmt(plannedAmt);
		summaryLine.setPlannedQty(plannedQty);
		summaryLine.setPlannedMarginAmt(plannedMarginAmt);
		summaryLine.setCommittedAmt(committedAmt);
		summaryLine.setCommittedQty(committedQty);
		summaryLine.setInvoicedAmt(invoicedAmt);
		summaryLine.setInvoicedQty(invoicedQty);
		summaryLine.saveEx();
	}

	protected Optional<I_C_ProjectTask> projectTask = Optional.empty();
	protected Optional<I_C_ProjectPhase> projectPhase = Optional.empty();

	public Optional<I_C_ProjectTask> getProjectTask() {
		projectTask = Optional.ofNullable(getC_ProjectTask());
		return projectTask;
	}

	public Optional<I_C_ProjectPhase> getProjectPhase() {
			projectPhase = Optional.ofNullable(getC_ProjectPhase());
		return projectPhase;
	}

	public Integer getReposibleId()
	{
		AtomicInteger reposibleId = new AtomicInteger(getCreatedBy());
		getProjectPhase().ifPresent( phase -> reposibleId.set(phase.getResponsible_ID()));
		getProjectTask().ifPresent( task -> reposibleId.set(task.getResponsible_ID()));
		return reposibleId.get();
	}

	public Timestamp getDateStartSchedule(){
		AtomicReference<Timestamp> dateStartSchedule = new AtomicReference<>(super.getDateStartSchedule());
		getProjectPhase().ifPresent(phase -> dateStartSchedule.set(phase.getDateStartSchedule()));
		getProjectTask().ifPresent(task -> dateStartSchedule.set(task.getDateStartSchedule()));
		return dateStartSchedule.get();
	}

	public Timestamp getDateFinishSchedule(){
		AtomicReference<Timestamp> dateStartSchedule = new AtomicReference<>(super.getDateFinishSchedule());
		getProjectPhase().ifPresent(phase -> dateStartSchedule.set(phase.getDateFinishSchedule()));
		getProjectTask().ifPresent(task -> dateStartSchedule.set(task.getDateFinishSchedule()));
		return dateStartSchedule.get();
	}

	public Timestamp getDateDeadline(){
		AtomicReference<Timestamp> dateDeadline = new AtomicReference<>();
		getProjectPhase().ifPresent(phase -> dateDeadline.set(phase.getDateDeadline()));
		getProjectTask().ifPresent(task -> dateDeadline.set(task.getDateDeadline()));
		return dateDeadline.get();
	}

	public Timestamp getDateOrdered() {
		return Optional.ofNullable(getDateStartSchedule()).orElse(getCreated());
	}

	public Timestamp getDatePromised() {
		return Optional.ofNullable(getDateFinishSchedule()).orElse(Optional.ofNullable(getDateDeadline()).orElse(getCreated()));
	}

	public String getPriorityRule()
	{
		AtomicReference<String> priorityRule = new AtomicReference<>();
		getProjectPhase().ifPresent(phase -> priorityRule.set(phase.getPriorityRule()));
		getProjectTask().ifPresent(task -> priorityRule.set(task.getPriorityRule()));
		Optional<String> maybePriorityRule = Optional.ofNullable(priorityRule.get());
		return maybePriorityRule.orElse(MProject.PRIORITYRULE_Medium);
	}

	public List<MProjectLine> getChildren() {
		final String whereClause = "C_Project_ID = ? AND Parent_ID=?";
		return new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
				.setClient_ID()
				.setParameters(getC_Project_ID(), getC_ProjectLine_ID())
				.setOrderBy("Line")
				.list();
	}
	public List<Integer> getChildrenIds() {
		final String whereClause = "C_Project_ID = ? AND Parent_ID=?";
		return new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
				.setClient_ID()
				.setParameters(getC_Project_ID(), getC_ProjectLine_ID())
				.setOnlyActiveRecords(true)
				.setOrderBy("Line")
				.getIDsAsList();
	}
} // MProjectLine