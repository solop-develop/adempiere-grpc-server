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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
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

	/**	100 constant for margin <-> price math	*/
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	/**	Completed/Closed document status filter	*/
	private static final String DOCSTATUS_DONE = "('CO','CL')";

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
			//	Margin <-> Planned Price interdependency, anchored on the fixed Cost (#2843)
			if (!isSummary() && getCost() != null && getCost().signum() > 0) {
				if (is_ValueChanged(COLUMNNAME_PlannedPrice)) {
					//	Price edited -> derive Margin %
					BigDecimal margin = getPlannedPrice().subtract(getCost())
						.divide(getCost(), 4, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED);
					setMargin(margin);
				} else if (is_ValueChanged(COLUMNNAME_Margin)) {
					//	Margin edited -> derive Planned Price
					BigDecimal price = getCost().multiply(ONE_HUNDRED.add(getMargin()))
						.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
					setPlannedPrice(price);
				}
			}
			//	Summary lines keep the rolled-up PlannedAmt; do not recompute from price (#2843)
			if (!isSummary())
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

		//	Planned cost / planned profit are document-independent: keep them in sync with the
		//	line's own planned qty, cost and amount on every leaf save (#2843)
		if (!isSummary()) {
			setCostPlannedAmt(getPlannedQty().multiply(getCost()));
			setProfitPlannedAmt(getPlannedAmt().subtract(getCostPlannedAmt()));
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

		//	Project Issue just linked to this line -> recalculate only the Issue block
		//	(Delivered/Consumed + ProfitRealized) in this same save (#2843)
		if (!isSummary() && getC_ProjectIssue_ID() > 0
			&& (newRecord || is_ValueChanged(COLUMNNAME_C_ProjectIssue_ID))) {
			recalculateFromDocument(MProjectIssue.Table_Name, false);
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
				+ " InvoicedAmt,InvoicedQty,"
				+ " OrderedAmt,QtyOrdered,"
				+ " DeliveredAmt,QtyDelivered,"
				+ " CostPlannedAmt,"
				+ " CostOrderedAmt,CostOrderedQty,"
				+ " CostInvoicedAmt,CostInvoicedQty,"
				+ " CostReceivedAmt,CostReceivedQty,"
				+ " CostConsumedAmt,CostConsumedQty,"
				+ " ProfitPlannedAmt,ProfitRealizedAmt) = "
				+ "(SELECT COALESCE(SUM(pl.PlannedAmt),0),COALESCE(SUM(pl.PlannedQty),0),COALESCE(SUM(pl.PlannedMarginAmt),0),"
				+ " COALESCE(SUM(pl.CommittedAmt),0),COALESCE(SUM(pl.CommittedQty),0),"
				+ " COALESCE(SUM(pl.InvoicedAmt),0),COALESCE(SUM(pl.InvoicedQty),0),"
				+ " COALESCE(SUM(pl.OrderedAmt),0),COALESCE(SUM(pl.QtyOrdered),0),"
				+ " COALESCE(SUM(pl.DeliveredAmt),0),COALESCE(SUM(pl.QtyDelivered),0),"
				+ " COALESCE(SUM(pl.CostPlannedAmt),0),"
				+ " COALESCE(SUM(pl.CostOrderedAmt),0),COALESCE(SUM(pl.CostOrderedQty),0),"
				+ " COALESCE(SUM(pl.CostInvoicedAmt),0),COALESCE(SUM(pl.CostInvoicedQty),0),"
				+ " COALESCE(SUM(pl.CostReceivedAmt),0),COALESCE(SUM(pl.CostReceivedQty),0),"
				+ " COALESCE(SUM(pl.CostConsumedAmt),0),COALESCE(SUM(pl.CostConsumedQty),0),"
				+ " COALESCE(SUM(pl.ProfitPlannedAmt),0),COALESCE(SUM(pl.ProfitRealizedAmt),0) "
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


	/**
	 *	Roll up every summarization column from the children into the parent
	 *	(Phase/Milestone) line. Saving the parent re-fires its own afterSave, so any
	 *	additional levels and the project header are propagated automatically. (#2843)
	 */
	private void updateSummaryLine() {
		MProjectLine summaryLine = (MProjectLine) getParent();
		if (summaryLine == null)
			return;

		BigDecimal plannedAmt = Env.ZERO, plannedQty = Env.ZERO, plannedMarginAmt = Env.ZERO;
		BigDecimal committedAmt = Env.ZERO, committedQty = Env.ZERO;
		BigDecimal invoicedAmt = Env.ZERO, invoicedQty = Env.ZERO;
		BigDecimal orderedAmt = Env.ZERO, qtyOrdered = Env.ZERO;
		BigDecimal deliveredAmt = Env.ZERO, qtyDelivered = Env.ZERO;
		BigDecimal costPlannedAmt = Env.ZERO;
		BigDecimal costOrderedAmt = Env.ZERO, costOrderedQty = Env.ZERO;
		BigDecimal costInvoicedAmt = Env.ZERO, costInvoicedQty = Env.ZERO;
		BigDecimal costReceivedAmt = Env.ZERO, costReceivedQty = Env.ZERO;
		BigDecimal costConsumedAmt = Env.ZERO, costConsumedQty = Env.ZERO;
		BigDecimal profitPlannedAmt = Env.ZERO, profitRealizedAmt = Env.ZERO;

		for (MProjectLine child : summaryLine.getChildren()) {
			if (!child.isActive())
				continue;
			plannedAmt = plannedAmt.add(child.getPlannedAmt());
			plannedQty = plannedQty.add(child.getPlannedQty());
			plannedMarginAmt = plannedMarginAmt.add(child.getPlannedMarginAmt());
			committedAmt = committedAmt.add(child.getCommittedAmt());
			committedQty = committedQty.add(child.getCommittedQty());
			invoicedAmt = invoicedAmt.add(child.getInvoicedAmt());
			invoicedQty = invoicedQty.add(child.getInvoicedQty());
			orderedAmt = orderedAmt.add(child.getOrderedAmt());
			qtyOrdered = qtyOrdered.add(child.getQtyOrdered());
			deliveredAmt = deliveredAmt.add(child.getDeliveredAmt());
			qtyDelivered = qtyDelivered.add(child.getQtyDelivered());
			costPlannedAmt = costPlannedAmt.add(child.getCostPlannedAmt());
			costOrderedAmt = costOrderedAmt.add(child.getCostOrderedAmt());
			costOrderedQty = costOrderedQty.add(child.getCostOrderedQty());
			costInvoicedAmt = costInvoicedAmt.add(child.getCostInvoicedAmt());
			costInvoicedQty = costInvoicedQty.add(child.getCostInvoicedQty());
			costReceivedAmt = costReceivedAmt.add(child.getCostReceivedAmt());
			costReceivedQty = costReceivedQty.add(child.getCostReceivedQty());
			costConsumedAmt = costConsumedAmt.add(child.getCostConsumedAmt());
			costConsumedQty = costConsumedQty.add(child.getCostConsumedQty());
			profitPlannedAmt = profitPlannedAmt.add(child.getProfitPlannedAmt());
			profitRealizedAmt = profitRealizedAmt.add(child.getProfitRealizedAmt());
		}
		summaryLine.setPlannedAmt(plannedAmt);
		summaryLine.setPlannedQty(plannedQty);
		summaryLine.setPlannedMarginAmt(plannedMarginAmt);
		summaryLine.setCommittedAmt(committedAmt);
		summaryLine.setCommittedQty(committedQty);
		summaryLine.setInvoicedAmt(invoicedAmt);
		summaryLine.setInvoicedQty(invoicedQty);
		summaryLine.setOrderedAmt(orderedAmt);
		summaryLine.setQtyOrdered(qtyOrdered);
		summaryLine.setDeliveredAmt(deliveredAmt);
		summaryLine.setQtyDelivered(qtyDelivered);
		summaryLine.setCostPlannedAmt(costPlannedAmt);
		summaryLine.setCostOrderedAmt(costOrderedAmt);
		summaryLine.setCostOrderedQty(costOrderedQty);
		summaryLine.setCostInvoicedAmt(costInvoicedAmt);
		summaryLine.setCostInvoicedQty(costInvoicedQty);
		summaryLine.setCostReceivedAmt(costReceivedAmt);
		summaryLine.setCostReceivedQty(costReceivedQty);
		summaryLine.setCostConsumedAmt(costConsumedAmt);
		summaryLine.setCostConsumedQty(costConsumedQty);
		summaryLine.setProfitPlannedAmt(profitPlannedAmt);
		summaryLine.setProfitRealizedAmt(profitRealizedAmt);
		summaryLine.saveEx();
	}

	/**
	 *	Recalculate ONLY the summarization columns that depend on the given document type,
	 *	matched strictly by C_ProjectLine_ID, completed/closed only. Each block runs a single
	 *	aggregation and only sets its fields; the caller's save triggers the roll up. (#2843)
	 *	@param tableName source document table (MOrder/MInvoice/MInOut/MProjectIssue Table_Name)
	 *	@param isSOTrx sales (true) vs purchase (false) side of the document
	 */
	public void recalculateFromDocument(String tableName, boolean isSOTrx)
	{
		if (isSummary())
			return;
		if (MOrder.Table_Name.equals(tableName)) {
			if (isSOTrx)
				recalculateSalesOrdered();
			else
				recalculateCostOrdered();
		} else if (MInvoice.Table_Name.equals(tableName)) {
			if (isSOTrx)
				recalculateSalesInvoiced();
			else
				recalculateCostInvoiced();
		} else if (MInOut.Table_Name.equals(tableName)) {
			//	Sales shipment does not map to a column; the "delivered" goes through the Issue
			if (!isSOTrx)
				recalculateCostReceived();
		} else if (MProjectIssue.Table_Name.equals(tableName)) {
			recalculateIssue();
		}
	}	//	recalculateFromDocument

	/**	Ordered (sales): OrderedAmt, QtyOrdered from completed SO lines	*/
	private void recalculateSalesOrdered()
	{
		BigDecimal[] amtQty = getDocumentAmtQty(
			"SELECT COALESCE(SUM(ol.LineNetAmt),0), COALESCE(SUM(ol.QtyOrdered),0) "
			+ "FROM " + MOrderLine.Table_Name + " ol JOIN " + MOrder.Table_Name + " o ON o.C_Order_ID=ol.C_Order_ID "
			+ "WHERE ol.C_ProjectLine_ID=? AND o.IsSOTrx='Y' AND o.DocStatus IN " + DOCSTATUS_DONE, getC_ProjectLine_ID());
		setOrderedAmt(amtQty[0]);
		setQtyOrdered(amtQty[1]);
	}	//	recalcSalesOrdered

	/**	Ordered (purchase): CostOrderedAmt, CostOrderedQty from completed PO lines	*/
	private void recalculateCostOrdered()
	{
		BigDecimal[] amtQty = getDocumentAmtQty(
			"SELECT COALESCE(SUM(ol.LineNetAmt),0), COALESCE(SUM(ol.QtyOrdered),0) "
			+ "FROM " + MOrderLine.Table_Name + " ol JOIN " + MOrder.Table_Name + " o ON o.C_Order_ID=ol.C_Order_ID "
			+ "WHERE ol.C_ProjectLine_ID=? AND o.IsSOTrx='N' AND o.DocStatus IN " + DOCSTATUS_DONE, getC_ProjectLine_ID());
		setCostOrderedAmt(amtQty[0]);
		setCostOrderedQty(amtQty[1]);
	}	//	recalcCostOrdered

	/**	Invoiced (sales/AR): InvoicedAmt, InvoicedQty from completed AR lines	*/
	private void recalculateSalesInvoiced()
	{
		BigDecimal[] amtQty = getDocumentAmtQty(
			"SELECT COALESCE(SUM(il.LineNetAmt),0), COALESCE(SUM(il.QtyInvoiced),0) "
			+ "FROM " + MInvoiceLine.Table_Name + " il JOIN " + MInvoice.Table_Name + " i ON i.C_Invoice_ID=il.C_Invoice_ID "
			+ "WHERE il.C_ProjectLine_ID=? AND i.IsSOTrx='Y' AND i.DocStatus IN " + DOCSTATUS_DONE, getC_ProjectLine_ID());
		setInvoicedAmt(amtQty[0]);
		setInvoicedQty(amtQty[1]);
	}	//	recalcSalesInvoiced

	/**	Invoiced (purchase/AP): CostInvoicedAmt, CostInvoicedQty from completed AP lines	*/
	private void recalculateCostInvoiced()
	{
		BigDecimal[] amtQty = getDocumentAmtQty(
			"SELECT COALESCE(SUM(il.LineNetAmt),0), COALESCE(SUM(il.QtyInvoiced),0) "
			+ "FROM " + MInvoiceLine.Table_Name + " il JOIN " + MInvoice.Table_Name + " i ON i.C_Invoice_ID=il.C_Invoice_ID "
			+ "WHERE il.C_ProjectLine_ID=? AND i.IsSOTrx='N' AND i.DocStatus IN " + DOCSTATUS_DONE, getC_ProjectLine_ID());
		setCostInvoicedAmt(amtQty[0]);
		setCostInvoicedQty(amtQty[1]);
	}	//	recalcCostInvoiced

	/**	Received (purchase receipt): CostReceivedAmt, CostReceivedQty; amount from the linked PO line price	*/
	private void recalculateCostReceived()
	{
		BigDecimal[] amtQty = getDocumentAmtQty(
			"SELECT COALESCE(SUM(iol.MovementQty*COALESCE(ol.PriceActual,0)),0), COALESCE(SUM(iol.MovementQty),0) "
			+ "FROM " + MInOutLine.Table_Name + " iol JOIN " + MInOut.Table_Name + " io ON io.M_InOut_ID=iol.M_InOut_ID "
			+ "LEFT JOIN " + MOrderLine.Table_Name + " ol ON ol.C_OrderLine_ID=iol.C_OrderLine_ID "
			+ "WHERE iol.C_ProjectLine_ID=? AND io.IsSOTrx='N' AND io.DocStatus IN " + DOCSTATUS_DONE, getC_ProjectLine_ID());
		setCostReceivedAmt(amtQty[0]);
		setCostReceivedQty(amtQty[1]);
	}	//	recalcCostReceived

	/**
	 *	Realized via Project Issue: Delivered (sale) and Consumed (cost). The issue stores no price,
	 *	so the amount comes from the line's own PlannedPrice / Cost. Updates ProfitRealizedAmt.
	 */
	private void recalculateIssue()
	{
		BigDecimal issuedQty = Env.ZERO;
		if (getC_ProjectIssue_ID() > 0) {
			issuedQty = DB.getSQLValueBD(get_TrxName(),
				"SELECT COALESCE(SUM(pi.MovementQty),0) FROM " + MProjectIssue.Table_Name + " pi "
				+ "WHERE pi.C_ProjectIssue_ID=? AND pi.Processed='Y'", getC_ProjectIssue_ID());
			if (issuedQty == null)
				issuedQty = Env.ZERO;
		}
		setQtyDelivered(issuedQty);
		setDeliveredAmt(issuedQty.multiply(getPlannedPrice()));
		setCostConsumedQty(issuedQty);
		setCostConsumedAmt(issuedQty.multiply(getCost()));
		setProfitRealizedAmt(getDeliveredAmt().subtract(getCostConsumedAmt()));
	}	//	recalcIssue

	/**
	 *	Run a two-column (amount, quantity) aggregation bound to one C_ProjectLine_ID.
	 *	@return BigDecimal[]{amount, qty}, never null, zero-filled
	 */
	private BigDecimal[] getDocumentAmtQty(String sql, int projectLineId)
	{
		BigDecimal[] result = new BigDecimal[] { Env.ZERO, Env.ZERO };
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, projectLineId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				result[0] = Optional.ofNullable(rs.getBigDecimal(1)).orElse(Env.ZERO);
				result[1] = Optional.ofNullable(rs.getBigDecimal(2)).orElse(Env.ZERO);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
		}
		return result;
	}	//	getDocumentAmtQty

	/**
	 *	Recalculate the given leaf project lines for the block that depends on the source document
	 *	and save them, propagating the roll up. Used by document completion. (#2843)
	 */
	public static void recalculateProjectLines(Properties ctx, Collection<Integer> projectLineIds,
		String trxName, String tableName, boolean isSOTrx)
	{
		if (projectLineIds == null || projectLineIds.isEmpty())
			return;
		for (Integer projectLineId : projectLineIds) {
			if (projectLineId == null || projectLineId <= 0)
				continue;
			MProjectLine projectLine = new MProjectLine(ctx, projectLineId, trxName);
			if (projectLine.get_ID() != projectLineId || projectLine.isSummary())
				continue;
			projectLine.recalculateFromDocument(tableName, isSOTrx);
			projectLine.saveEx();
		}
	}	//	recalculateProjectLines

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