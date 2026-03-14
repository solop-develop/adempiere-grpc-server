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

import org.adempiere.core.domains.models.I_C_ProjectIssue;
import org.adempiere.core.domains.models.I_C_ProjectLine;
import org.adempiere.core.domains.models.I_C_ProjectPhase;
import org.adempiere.core.domains.models.X_C_Project;
import org.adempiere.core.domains.models.X_I_Project;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.model.MProjectMember;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * 	Project Model
 *
 *	@author Jorg Janke
 *  @author Víctor Pérez Juárez , victor.perez@e-evolution.com , http://www.e-evolution.com
 *  <a href="https://github.com/adempiere/adempiere/issues/1478">
 *  <li>Add support to create request based on Standard Request Type setting on Project Type #1478
 *	@version $Id: MProject.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 *	@author Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 *  	<a href="https://github.com/adempiere/adempiere/issues/2117">
 *		@see FR [ 2117 ] Add Support to Price List on Project</a>
 */
public class MProject extends X_C_Project
{
	private static CCache<Integer, MProject> projectCacheIds = new CCache<Integer, MProject>(Table_Name, 100, 0);
	private static CCache<String, MProject> projectCacheValues = new CCache<String, MProject>(Table_Name, 100, 0);

	/**
	 * Ge project by Id
	 * @param ctx
	 * @param projectId
	 * @param trxName
	 * @return
	 */
	public static MProject getById(Properties ctx, Integer projectId, String trxName) {
		if (projectId <= 0)
			return null;
		if (projectCacheIds.size() == 0)
			getAll(ctx, true, trxName);

		MProject project = projectCacheIds.get(projectId);
		if (project != null)
			return project;

		project =  new Query(ctx , Table_Name , COLUMNNAME_C_Project_ID + "=?" , trxName)
				.setClient_ID()
				.setParameters(projectId)
				.first();

		if (project != null && project.get_ID() > 0) {
			int clientId = Env.getAD_Client_ID(ctx);
			String key = clientId + "#" + project.getValue();
			projectCacheIds.put(project.get_ID(), project);
			projectCacheValues.put(key, project);
		}
		return project;
	}

	/**
	 * Get project by Search Key
	 * @param ctx
	 * @param value
	 * @param trxName
	 * @return
	 */
	public static MProject getByValue(Properties ctx, String value, String trxName) {
		if (value == null)
			return null;
		if (projectCacheValues.size() == 0)
			getAll(ctx, true, trxName);

		int clientId = Env.getAD_Client_ID(ctx);
		String key = clientId + "#" + value;
		MProject project = projectCacheValues.get(key);
		if (project != null && project.get_ID() > 0)
			return project;

		project = new Query(ctx, Table_Name, COLUMNNAME_Value + "=?", trxName)
				.setClient_ID()
				.setParameters(value)
				.first();
		if (project != null && project.get_ID() > 0) {
			projectCacheValues.put(key, project);
			projectCacheIds.put(project.get_ID(), project);
		}
		return project;
	}

	/**
	 * Get all project and create cache
	 * @param ctx
	 * @param resetCache
	 * @param trxName
	 * @return
	 */
	public static List<MProject> getAll(Properties ctx, boolean resetCache, String trxName) {
		List<MProject> projectList;
		if (resetCache || projectCacheIds.size() > 0) {
			projectList = new Query(Env.getCtx(), Table_Name, null, trxName)
					.setClient_ID()
					.setOrderBy(COLUMNNAME_Name)
					.list();
			projectList.stream().forEach(project -> {
				int clientId = Env.getAD_Client_ID(ctx);
				String key = clientId + "#" + project.getValue();
				projectCacheIds.put(project.getC_Project_ID(), project);
				projectCacheValues.put(key, project);
			});
			return projectList;
		}
		projectList = projectCacheIds.entrySet().stream()
				.map(project -> project.getValue())
				.collect(Collectors.toList());
		return projectList;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2151648902207548617L;

	/**
	 * 	Create new Project by copying
	 * 	@param ctx context
	 *	@param C_Project_ID project
	 * 	@param dateDoc date of the document date
	 *	@param trxName transaction
	 *	@return Project
	 */
	public static MProject copyFrom (Properties ctx, int C_Project_ID, Timestamp dateDoc, String trxName)
	{
		MProject from = new MProject (ctx, C_Project_ID, trxName);
		if (from.getC_Project_ID() == 0)
			throw new IllegalArgumentException ("From Project not found C_Project_ID=" + C_Project_ID);
		//
		MProject to = new MProject (ctx, 0, trxName);
		PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("C_Project_ID", I_ZERO);
		//	Set Value with Time
		String Value = to.getValue() + " ";
		String Time = dateDoc.toString();
		int length = Value.length() + Time.length();
		if (length <= 40)
			Value += Time;
		else
			Value += Time.substring (length-40);
		to.setValue(Value);
		to.setInvoicedAmt(Env.ZERO);
		to.setProjectBalanceAmt(Env.ZERO);
		to.setProcessed(false);
		//
		if (!to.save())
			throw new IllegalStateException("Could not create Project");

		if (to.copyDetailsFrom(from) == 0)
			throw new IllegalStateException("Could not create Project Details");

		return to;
	}	//	copyFrom

	
	/**************************************************************************
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_Project_ID id
	 *	@param trxName transaction
	 */
	public MProject(Properties ctx, int C_Project_ID, String trxName)
	{
		super (ctx, C_Project_ID, trxName);
		if (C_Project_ID == 0)
		{
		//	setC_Project_ID(0);
		//	setValue (null);
		//	setC_Currency_ID (0);
			setCommittedAmt (Env.ZERO);
			setCommittedQty (Env.ZERO);
			setInvoicedAmt (Env.ZERO);
			setInvoicedQty (Env.ZERO);
			setPlannedAmt (Env.ZERO);
			setPlannedMarginAmt (Env.ZERO);
			setPlannedQty (Env.ZERO);
			setProjectBalanceAmt (Env.ZERO);
		//	setProjectCategory(PROJECTCATEGORY_General);
			setProjInvoiceRule(PROJINVOICERULE_None);
			setProjectLineLevel(PROJECTLINELEVEL_Project);
			setIsCommitCeiling (false);
			setIsCommitment (false);
			setIsSummary (false);
			setProcessed (false);
		}
	}	//	MProject

	public MProject(X_I_Project projectImport)
	{
		super(projectImport.getCtx() , 0 , projectImport.get_TrxName());
		setAD_Org_ID(projectImport.getAD_Org_ID());
		setM_PriceList_Version_ID(projectImport.getM_PriceList_Version_ID());
		setAD_Color_ID(projectImport.getAD_Color_ID());
		setAD_OrgTrx_ID(projectImport.getAD_OrgTrx_ID());
		setAD_User_ID(projectImport.getAD_User_ID());
		setC_Activity_ID(projectImport.getC_Activity_ID());
		setC_BPartner_ID(projectImport.getC_BPartner_ID());
		setC_BPartner_Location_ID(projectImport.getC_BPartner_Location_ID());
		setC_BPartnerSR_ID(projectImport.getC_BPartnerSR_ID());
		setC_Campaign_ID(projectImport.getC_Campaign_ID());
		setC_Currency_ID(projectImport.getC_Currency_ID());
		setC_PaymentTerm_ID(projectImport.getC_PaymentTerm_ID());
		setC_PaymentTerm_ID(projectImport.getC_PaymentTerm_ID());
		setC_ProjectCategory_ID(projectImport.getC_ProjectCategory_ID());
		setC_ProjectClass_ID(projectImport.getC_ProjectClass_ID());
		setC_ProjectGroup_ID(projectImport.getC_ProjectGroup_ID());
		setC_ProjectStatus_ID(projectImport.getC_ProjectStatus_ID());
		setC_SalesRegion_ID(projectImport.getC_SalesRegion_ID());
		setCommittedAmt(projectImport.getCommittedAmt());
		setCommittedQty(projectImport.getCommittedQty());
		setDateContract(projectImport.getDateContract());
		setDateDeadline(projectImport.getDateDeadline());
		setDateFinish(projectImport.getDateFinish());
		setDateStart(projectImport.getDateStart());
		setDateFinishSchedule(projectImport.getDateFinishSchedule());
		setDateStartSchedule(projectImport.getDateStartSchedule());
		setDescription(projectImport.getDescription());
		setDurationUnit(projectImport.getDurationUnit());
		setInvoicedAmt(projectImport.getInvoicedAmt());
		setInvoicedQty(projectImport.getInvoicedQty());
		setIsCommitCeiling(projectImport.isCommitCeiling());
		setIsCommitment(isCommitment());
		setIsIndefinite(projectImport.isIndefinite());
		setIsSummary(projectImport.isSummary());
		setM_Warehouse_ID(projectImport.getM_Warehouse_ID());
		setName(projectImport.getName());
		setNote(projectImport.getNote());
		setPlannedAmt(projectImport.getPlannedAmt());
		setPlannedMarginAmt(projectImport.getPlannedMarginAmt());
		setPlannedQty(projectImport.getPlannedQty());
		setPOReference(projectImport.getPOReference());
		setPriorityRule(projectImport.getPriorityRule());
		setProjectBalanceAmt(projectImport.getProjectBalanceAmt());
		setProjectLineLevel(projectImport.getProjectLineLevel());
		setProjectManager_ID(projectImport.getProjectManager_ID());
		setProjInvoiceRule(projectImport.getProjInvoiceRule());
		setSalesRep_ID(projectImport.getSalesRep_ID());
		setUser1_ID(projectImport.getUser1_ID());
		setUser2_ID(projectImport.getUser2_ID());
		setUser3_ID(projectImport.getUser3_ID());
		setUser4_ID(projectImport.getUser4_ID());
		setValue(projectImport.getValue());
	}


	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProject(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProject

	/**	Cached PL			*/
	private int		m_M_PriceList_ID = 0;

	/**
	 * 	Get Project Type as Int (is Button).
	 *	@return C_ProjectType_ID id
	 */
	public int getC_ProjectType_ID_Int()
	{
		String pj = super.getC_ProjectType_ID();
		if (pj == null)
			return 0;
		int C_ProjectType_ID = 0;
		try
		{
			C_ProjectType_ID = Integer.parseInt (pj);
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, pj, ex);
		}
		return C_ProjectType_ID;
	}	//	getC_ProjectType_ID_Int

	/**
	 * 	Set Project Type (overwrite r/o)
	 *	@param C_ProjectType_ID id
	 */
	public void setC_ProjectType_ID (int C_ProjectType_ID)
	{
		if (C_ProjectType_ID == 0)
			super.setC_ProjectType_ID (null);
		else
			super.set_Value("C_ProjectType_ID", C_ProjectType_ID);
	}	//	setC_ProjectType_ID

	/**
	 *	String Representation
	 * 	@return info
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer ("MProject[").append(get_ID())
			.append("-").append(getValue()).append(",ProjectCategory=").append(getProjectCategory())
			.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * 	Get Price List from Price List Version
	 *	@return price list or 0
	 */
	public int getM_PriceList_ID()
	{
		//FR [ 2117 ]
		if (super.get_ValueAsInt("M_PriceList_ID") == 0 
				&& getM_PriceList_Version_ID() == 0)
			return 0;
		
		if (super.get_ValueAsInt("M_PriceList_ID") != 0)
			m_M_PriceList_ID = super.get_ValueAsInt("M_PriceList_ID");
		
		if (m_M_PriceList_ID > 0)
			return m_M_PriceList_ID;
		//
		String sql = "SELECT M_PriceList_ID FROM M_PriceList_Version WHERE M_PriceList_Version_ID=?";
		m_M_PriceList_ID = DB.getSQLValue(null, sql, getM_PriceList_Version_ID());
		return m_M_PriceList_ID;
	}	//	getM_PriceList_ID

	/**
	 * 	Set PL Version
	 *	@param M_PriceList_Version_ID id
	 */
	public void setM_PriceList_Version_ID (int M_PriceList_Version_ID)
	{
		super.setM_PriceList_Version_ID(M_PriceList_Version_ID);
		m_M_PriceList_ID = 0;	//	reset
	}	//	setM_PriceList_Version_ID


	/**************************************************************************
	 * 	Get Project Lines
	 *	@return Array of lines
	 */
	public List<MProjectLine> getLines()
	{
		return getLines("");
	}	//	getLines
	
	/**************************************************************************
	 * 	Get Project Lines
	 *	@return Array of lines
	 */
	public List<MProjectLine> getLines(String whereClause)
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		//final String whereClause = "C_Project_ID=?";
		if (Util.isEmpty(whereClause, true))
			whereClause = "C_Project_ID=?";
		else
			whereClause += " AND C_Project_ID=?";
		
		return new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("Line")
			.list();
	}	//	getLines

	/**
	 * 	Get Project Issues
	 *	@return Array of issues
	 */
	public List<MProjectIssue> getIssues()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		String whereClause = "C_Project_ID=?";
		return new Query(getCtx(), I_C_ProjectIssue.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("Line")
			.list();
	}	//	getIssues

	/**
	 * 	Get Project Phases
	 *	@return Array of phases
	 */
	public List<MProjectPhase> getPhases()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		String whereClause = "C_Project_ID=?";
		return new Query(getCtx(), I_C_ProjectPhase.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("SeqNo")
			.list();
	}	//	getPhases

	
	/**************************************************************************
	 * 	Copy Lines/Phase/Task from other Project
	 *	@param project project
	 *	@return number of total lines copied
	 */
	public int copyDetailsFrom (MProject project)
	{
		if (isProcessed() || project == null)
			return 0;
		int count = copyLinesFrom(project)
			+ copyPhasesFrom(project);
		return count;
	}	//	copyDetailsFrom

	/**
	 * 	Copy Lines From other Project
	 *	@param project project
	 *	@return number of lines copied
	 */
	public int copyLinesFrom (MProject project)
	{
		if (isProcessed() || project == null)
			return 0;
		AtomicInteger count = new AtomicInteger(0);
		List<MProjectLine> fromProjectLines = project.getLines();
		fromProjectLines.stream()
				.filter(fromProjectLine ->
						fromProjectLine.getC_ProjectPhase_ID() <= 0
					 || fromProjectLine.getC_ProjectTask_ID() <= 0)
				.forEach(fromProjectLine -> {
					MProjectLine toProjectLine = new MProjectLine(getCtx(), 0, project.get_TrxName());
					PO.copyValues(fromProjectLine, toProjectLine, getAD_Client_ID(), getAD_Org_ID());
					toProjectLine.setC_Project_ID(getC_Project_ID());
					toProjectLine.setInvoicedAmt(Env.ZERO);
					toProjectLine.setInvoicedQty(Env.ZERO);
					toProjectLine.setC_OrderPO_ID(0);
					toProjectLine.setC_Order_ID(0);
					toProjectLine.setProcessed(false);
					toProjectLine.saveEx();
					count.getAndUpdate(no -> no + 1);
				});

		if (fromProjectLines.size() != count.get())
			log.log(Level.SEVERE, "Lines difference - Project=" + fromProjectLines.size() + " <> Saved=" + count);
		return count.get();
	}	//	copyLinesFrom

	/**
	 * 	Copy Phases/Tasks from other Project
	 *	@param fromProject project
	 *	@return number of items copied
	 */
	public int copyPhasesFrom (MProject fromProject)
	{
		if (isProcessed() || fromProject == null)
			return 0;
		AtomicInteger count = new AtomicInteger(0);
		AtomicInteger taskCount = new AtomicInteger(0);
		AtomicInteger lineCount = new AtomicInteger(0);
		//	Get Phases
		List<MProjectPhase> toPhases = getPhases();
		List<MProjectPhase> fromPhases = fromProject.getPhases();
		fromPhases.stream()
				.forEach(fromPhase -> {
					//	Check if Phase already exists
					Boolean exists = toPhases.stream().anyMatch(toPhase -> toPhase.getC_Phase_ID() == fromPhase.getC_Phase_ID());
					//	Phase exist
					if (exists)
						log.info("Phase already exists here, ignored - " + fromPhase);
					else {
						MProjectPhase toPhase = new MProjectPhase(getCtx(), 0, get_TrxName());
						PO.copyValues(fromPhase, toPhase, getAD_Client_ID(), getAD_Org_ID());
						toPhase.setC_Project_ID(getC_Project_ID());
						toPhase.setC_Order_ID(0);
						toPhase.setIsComplete(false);
						toPhase.saveEx();
						count.getAndUpdate(no -> no + 1);
						taskCount.getAndUpdate(taskNo -> taskNo + toPhase.copyTasksFrom(fromPhase));
						lineCount.getAndUpdate(lineNo -> lineNo + toPhase.copyLinesFrom(fromPhase));
					}
				});
		if (fromPhases.size() != count.get())
			log.warning("Count difference - Project=" + fromPhases.size() + " <> Saved=" + count.get());

		return count.get() + taskCount.get() + lineCount.get();
	}	//	copyPhasesFrom


	/**
	 *	Set Project Type and Category.
	 * 	If Service Project copy Project Type Phase/Tasks
	 *	@param type project type
	 */
	public void setProjectType (MProjectType type)
	{
		if (type == null)
			return;
		setC_ProjectType_ID(Integer.toString(type.getC_ProjectType_ID()));
		setProjectCategory(type.getProjectCategory());
		if (type.get_ValueAsString("ProjectBased").equals("L")) {
			createLinesFromType(type,0);
		}else {
			createRequest(type);
			copyPhasesFrom(type);
		}
	}	//	setProjectType


	/**
	 * Create Lines from Project Type 
	 * @param type
	 * @param Parent_ID
	 */
	private void createLinesFromType(MProjectType type, int Parent_ID) {
		if (type == null)
			return;
		List<MStandardProjectLine> standardLines = MStandardProjectLine.getNodes(type, Parent_ID);
		List<MProjectLine> pLines = new ArrayList<MProjectLine>();
		
		standardLines.stream()
				.forEach(stdPLine ->{
					MProjectLine pLine = new MProjectLine(this,stdPLine);
					pLine.save();
					pLines.add(pLine);
				});
		
		
		pLines.stream()
				.forEach(pLine ->{
					MStandardProjectLine stdPLine = (MStandardProjectLine) pLine.getC_StandardProjectLine().getParent();
					if (stdPLine.get_ID()!=0) {
						MProjectLine parentPLine =pLines.stream()
												.filter(parent ->  
													parent.getC_StandardProjectLine_ID() ==stdPLine.getC_StandardProjectLine_ID())
												.findFirst()
												.get(); 
						pLine.setParent_ID(parentPLine.get_ID());
						pLine.save();
					}
				});
	}

	/**
	 *	Copy Phases from Type
	 *	@param type Project Type
	 *	@return count
	 */
	public int copyPhasesFrom (MProjectType type)
	{
		//	create phases
		AtomicInteger count = new AtomicInteger(0);
		AtomicInteger taskCount = new AtomicInteger(0);
		List<MProjectTypePhase> typePhases = type.getPhases();
		typePhases.stream()
				.forEach(fromPhase -> {
					MProjectPhase toPhase = new MProjectPhase(this, fromPhase);
					toPhase.setC_Project_ID(getC_Project_ID());
					toPhase.setProjInvoiceRule(getProjInvoiceRule());
					toPhase.saveEx();
					count.getAndUpdate(no -> no + 1);
					taskCount.getAndUpdate(no -> no + toPhase.copyTasksFrom(fromPhase));
				});
		log.fine("#" + count.get() + "/" + taskCount.get()
			+ " - " + type);
		if (typePhases.size() != count.get())
			log.log(Level.SEVERE, "Count difference - Type=" + typePhases.size() + " <> Saved=" + count.get());
		return count.get();
	}	//	copyPhasesFrom


	/**
	 * create Request Project
	 */
	public void createRequest(MProjectType projectType)
	{
		if (projectType.getR_StandardRequestType_ID() > 0)
		{
			MStandardRequestType standardRequestType = (MStandardRequestType) projectType.getR_StandardRequestType();
			List<MRequest> requests =  standardRequestType.createStandardRequest(this);
			requests.stream().forEach(request -> {
				request.setC_Project_ID(getC_Project_ID());
				request.setDateStartPlan(getDateStartSchedule());
				request.setDateCompletePlan(getDateFinishSchedule());
				request.saveEx();
			});
		}
	}

	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		if (getAD_User_ID() == -1)	//	Summary Project in Dimensions
			setAD_User_ID(0);
		
		//	Set Currency
		if (is_ValueChanged("M_PriceList_Version_ID") && getM_PriceList_Version_ID() != 0)
		{
			MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID(), null);
			if (pl != null && pl.get_ID() != 0)
				setC_Currency_ID(pl.getC_Currency_ID());
		}
		
		if (is_ValueChanged("C_ProjectCategory_ID"))
			setProjectCategory(getC_ProjectCategory().getProjectCategory());
		
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
		if (newRecord && success)
		{
			insert_Accounting("C_Project_Acct", "C_AcctSchema_Default", null);
		}

		//	Value/Name change
		if (success && !newRecord 
			&& (is_ValueChanged("Value") || is_ValueChanged("Name")))
			MAccount.updateValueDescription(getCtx(), "C_Project_ID=" + getC_Project_ID(), get_TrxName());

		if (getSalesRep_ID() > 0 && !MProjectMember.memberExists(this, getSalesRep_ID()))
			MProjectMember.addMember(this, getSalesRep_ID());
		if (getProjectManager_ID() > 0 && !MProjectMember.memberExists(this, getProjectManager_ID()))
			MProjectMember.addMember(this, getProjectManager_ID());
		if (getAD_User_ID() > 0 && !MProjectMember.memberExists(this, getAD_User_ID()))
			MProjectMember.addMember(this, getAD_User_ID());

		if(newRecord || is_ValueChanged(COLUMNNAME_M_Product_ID)) {
			MProjectProduct.getFromProject(this).forEach(productFromProjectId -> {
				MProjectProduct projectProduct = new MProjectProduct(getCtx(), productFromProjectId, get_TrxName());
				projectProduct.delete(true);
			});
			if(getM_Product_ID() > 0) {
				MProjectProduct projectProduct = new MProjectProduct(getCtx(), 0, get_TrxName());
				projectProduct.setC_Project_ID(getC_Project_ID());
				projectProduct.setM_Product_ID(getM_Product_ID());
				projectProduct.saveEx();
			}
		}
		return success;
	}	//	afterSave

	/**
	 * 	Before Delete
	 *	@return true
	 */
	protected boolean beforeDelete ()
	{
		return delete_Accounting("C_Project_Acct"); 
	}	//	beforeDelete
	
	/**
	 * 	Return the Invoices Generated for this Project
	 *	@return invoices
	 *	@author monhate
	 */	
	public MInvoice[] getMInvoices(){
		StringBuilder sb = new StringBuilder();
		sb.append(MInvoice.COLUMNNAME_C_Project_ID).append("=?");
		Query qry = new Query(getCtx(), MInvoice.Table_Name, sb.toString(), get_TrxName());
		qry.setParameters(getC_Project_ID());		
		return (MInvoice[]) qry.list().toArray();
	}

	public String updateProjectPerformanceCalculation() {

		MProjectPerformance pp;

		String sql = "SELECT C_Project_Performance_ID FROM C_Project_Performance WHERE C_Project_ID = " + get_ID();
		int pp_ID= DB.getSQLValueEx(get_TrxName(), sql);

		if(pp_ID > 0){
			pp = new MProjectPerformance(getCtx(), pp_ID, get_TrxName());
		} else {
			pp = new MProjectPerformance(getCtx(), 0, get_TrxName());
			pp.setC_Project_ID(getC_Project_ID());
			pp.saveEx();
		}

		BigDecimal result = Env.ZERO;
		//result = this.calcLineNetAmt();
		//pp.set_ValueOfColumn("ProjectPriceListRevenuePlanned", (Object)result.setScale(2, 4));
		//result = this.calcActualamt();
		//pp.set_ValueOfColumn("ProjectOfferedRevenuePlanned", (Object)result.setScale(2, 4));

		//update planned costs
		result = calcPlannedCostMaterial(getC_Project_ID());
		pp.set_ValueOfColumn("PlannedCostMaterial", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		result = calcPlannedCostResource(getC_Project_ID());
		pp.set_ValueOfColumn("PlannedCostResource", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		result = calcPlannedCostTools(getC_Project_ID());
		pp.set_ValueOfColumn("PlannedCostTools", result.setScale(2, BigDecimal.ROUND_HALF_UP));

		result = this.calcCostOrRevenuePlanned(this.getC_Project_ID(), false, false);
		pp.set_ValueOfColumn("CostPlanned", (Object)result.setScale(2, 4));
		result = this.calcCostOrRevenueActual(this.getC_Project_ID(), false, false);
		pp.set_ValueOfColumn("CostAmt", (Object)result.setScale(2, 4));
		result = this.calcNotInvoicedCostOrRevenue(this.getC_Project_ID(), false, false);
		pp.set_ValueOfColumn("CostNotInvoiced", (Object)result.setScale(2, 4));
		final BigDecimal costExtrapolated = this.calcCostOrRevenueExtrapolated(this.getC_Project_ID(), false, false);
		pp.set_ValueOfColumn("CostExtrapolated", (Object)costExtrapolated.setScale(2, 4));
		result = this.calcCostOrRevenuePlanned(this.getC_Project_ID(), true, false);
		pp.set_ValueOfColumn("RevenuePlanned", (Object)result.setScale(2, 4));
		result = this.calcCostOrRevenueActual(this.getC_Project_ID(), true, false);
		pp.set_ValueOfColumn("RevenueAmt", (Object)result.setScale(2, 4));
		result = this.calcNotInvoicedCostOrRevenue(this.getC_Project_ID(), true, false);
		pp.set_ValueOfColumn("RevenueNotInvoiced", (Object)result.setScale(2, 4));
		final BigDecimal revenueExtrapolated = this.calcCostOrRevenueExtrapolated(this.getC_Project_ID(), true, false);
		pp.set_ValueOfColumn("RevenueExtrapolated", (Object)revenueExtrapolated.setScale(2, 4));
		final BigDecimal costIssueProduct = this.calcCostIssueProduct(this.getC_Project_ID(), false);
		pp.set_ValueOfColumn("CostIssueProduct", (Object)costIssueProduct.setScale(2, 4));
		final BigDecimal costIssueResource = this.calcCostIssueResource(this.getC_Project_ID(), false);
		pp.set_ValueOfColumn("CostIssueResource", (Object)costIssueResource.setScale(2, 4));
		final BigDecimal costIssueInventory = this.calcCostIssueInventory(this.getC_Project_ID(), false);
		pp.set_ValueOfColumn("CostIssueInventory", (Object)costIssueInventory.setScale(2, 4));
		pp.set_ValueOfColumn("CostIssueSum", (Object)costIssueProduct.add(costIssueResource).add(costIssueInventory).setScale(2, 4));
		pp.set_ValueOfColumn("CostDiffExcecution", (Object)((BigDecimal)pp.get_Value("CostPlanned")).subtract(costIssueProduct).subtract(costIssueInventory).setScale(2, 4));
		final BigDecimal sumCosts = costExtrapolated.add(costIssueResource).add(costIssueInventory);
		final BigDecimal grossMargin = revenueExtrapolated.subtract(sumCosts);
		pp.set_ValueOfColumn("GrossMargin", (Object)grossMargin.setScale(2, 4));
		if (sumCosts.compareTo(Env.ZERO) == 0 && revenueExtrapolated.compareTo(Env.ZERO) == 0) {
			pp.set_ValueOfColumn("Margin", (Object)Env.ZERO);
		}
		else if (sumCosts.compareTo(Env.ZERO) != 0) {
			if (revenueExtrapolated.compareTo(Env.ZERO) != 0) {
				pp.set_ValueOfColumn("Margin", (Object)revenueExtrapolated.divide(sumCosts, 6, 4).subtract(Env.ONE).multiply(Env.ONEHUNDRED).setScale(2, 4));
			}
			else {
				pp.set_ValueOfColumn("Margin", (Object)Env.ONEHUNDRED.negate());
			}
		}
		else {
			pp.set_ValueOfColumn("Margin", (Object)Env.ONEHUNDRED);
		}
		BigDecimal grossMarginLL = Env.ZERO;
		if (this.isSummary()) {
			BigDecimal costPlannedLL = this.calcCostOrRevenuePlannedSons(this.getC_Project_ID(), false, true);
			pp.set_Value("CostPlannedLL", (Object)costPlannedLL.setScale(2, 4));
			BigDecimal costAmtLL = this.calcCostOrRevenueActualSons(this.getC_Project_ID(), false, true);
			pp.set_Value("CostAmtLL", (Object)costAmtLL.setScale(2, 4));
			BigDecimal costNotInvoicedLL = this.calcNotInvoicedCostOrRevenueSons(this.getC_Project_ID(), false, true);
			pp.set_Value("CostNotInvoicedLL", (Object)costNotInvoicedLL.setScale(2, 4));
			BigDecimal costExtrapolatedLL = this.calcCostOrRevenueExtrapolatedSons(this.getC_Project_ID(), false, true);
			pp.set_Value("CostExtrapolatedLL", (Object)costExtrapolatedLL.setScale(2, 4));
			BigDecimal revenuePlannedLL = this.calcCostOrRevenuePlannedSons(this.getC_Project_ID(), true, true);
			pp.set_ValueOfColumn("RevenuePlannedLL", (Object)revenuePlannedLL.setScale(2, 4));
			BigDecimal revenueAmtLL = this.calcCostOrRevenueActualSons(this.getC_Project_ID(), true, true);
			pp.set_ValueOfColumn("RevenueAmtLL", (Object)revenueAmtLL.setScale(2, 4));
			BigDecimal revenueNotInvoicedLL = this.calcNotInvoicedCostOrRevenueSons(this.getC_Project_ID(), true, true);
			pp.set_ValueOfColumn("RevenueNotInvoicedLL", (Object)revenueNotInvoicedLL.setScale(2, 4));
			BigDecimal revenueExtrapolatedLL = this.calcCostOrRevenueExtrapolatedSons(this.getC_Project_ID(), true, true);
			pp.set_ValueOfColumn("RevenueExtrapolatedLL", (Object)revenueExtrapolatedLL.setScale(2, 4));
			BigDecimal costIssueProductLL = this.calcCostIssueProductSons(this.getC_Project_ID(), true);
			pp.set_ValueOfColumn("CostIssueProductLL", (Object)costIssueProductLL.setScale(2, 4));
			BigDecimal costIssueResourceLL = this.calcCostIssueResourceSons(this.getC_Project_ID(), true);
			pp.set_ValueOfColumn("CostIssueResourceLL", (Object)costIssueResourceLL.setScale(2, 4));
			BigDecimal costIssueInventoryLL = this.calcCostIssueInventorySons(this.getC_Project_ID(), true);
			pp.set_ValueOfColumn("CostIssueInventoryLL", (Object)costIssueInventoryLL.setScale(2, 4));
			BigDecimal costIssueSumLL = costIssueProductLL.add(costIssueResourceLL).add(costIssueInventoryLL).setScale(2, 4);
			pp.set_ValueOfColumn("CostIssueSumLL", (Object)costIssueSumLL.setScale(2, 4));
			BigDecimal costDiffExcecutionLL = costPlannedLL.subtract(costIssueProductLL).subtract(costIssueInventoryLL).setScale(2, 4);
			pp.set_ValueOfColumn("CostDiffExcecutionLL", (Object)costDiffExcecutionLL.setScale(2, 4));
			grossMarginLL = revenueExtrapolatedLL.subtract(costExtrapolatedLL).subtract(costIssueResourceLL).subtract(costIssueInventoryLL);
			if (grossMarginLL == null) {
				grossMarginLL = Env.ZERO;
			}
			pp.set_ValueOfColumn("GrossMarginLL", (Object)grossMarginLL.setScale(2, 4));
			pp.saveEx();
			final BigDecimal costActualFather = (BigDecimal)this.get_Value("CostAmt");
			final BigDecimal costPlannedFather = (BigDecimal)this.get_Value("CostPlanned");
			final BigDecimal costExtrapolatedFather = (BigDecimal)this.get_Value("CostExtrapolated");
			final BigDecimal revenueExtrapolatedSons = (BigDecimal)this.get_Value("RevenueExtrapolatedLL");
			final BigDecimal weightFather = (BigDecimal)this.get_Value("Weight");
			final BigDecimal volumeFather = (BigDecimal)this.get_Value("Volume");
			final List<MProject> projectsOfFather = new Query(this.getCtx(), "C_Project", "C_Project_Parent_ID=?", this.get_TrxName()).setParameters(new Object[] { this.getC_Project_ID() }).list();
			for (final MProject sonProject : projectsOfFather) {
				final BigDecimal revenueExtrapolatedSon = (BigDecimal)sonProject.get_Value("RevenueExtrapolated");
				final BigDecimal weight = (BigDecimal)sonProject.get_Value("Weight");
				final BigDecimal volume = (BigDecimal)sonProject.get_Value("volume");
				BigDecimal shareRevenue = Env.ZERO;
				BigDecimal shareWeight = Env.ZERO;
				BigDecimal shareVolume = Env.ZERO;
				if (revenueExtrapolatedSon != null && revenueExtrapolatedSons != null && revenueExtrapolatedSons.longValue() != 0L) {
					shareRevenue = revenueExtrapolatedSon.divide(revenueExtrapolatedSons, 5, 5);
				}
				if (weight != null && weightFather != null && weightFather.longValue() != 0L) {
					shareWeight = weight.divide(weightFather, 5, 5);
				}
				if (volume != null && volumeFather != null && volumeFather.longValue() != 0L) {
					shareVolume = volume.divide(volumeFather, 5, 5);
				}
				this.calcCostPlannedInherited(sonProject, costPlannedFather, costActualFather, costExtrapolatedFather, shareVolume, shareWeight, shareRevenue);
				costPlannedLL = costPlannedLL.add((sonProject.get_Value("CostPlannedLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostPlannedLL")));
				costAmtLL = costAmtLL.add((sonProject.get_Value("CostAmtLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostAmtLL")));
				costNotInvoicedLL = costNotInvoicedLL.add((sonProject.get_Value("CostNotInvoicedLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostNotInvoicedLL")));
				costExtrapolatedLL = costExtrapolatedLL.add((sonProject.get_Value("CostExtrapolatedLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostExtrapolatedLL")));
				revenuePlannedLL = revenuePlannedLL.add((sonProject.get_Value("RevenuePlannedLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("RevenuePlannedLL")));
				revenueAmtLL = revenueAmtLL.add((sonProject.get_Value("RevenueAmtLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("RevenueAmtLL")));
				revenueNotInvoicedLL = revenueNotInvoicedLL.add((sonProject.get_Value("RevenueNotInvoicedLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("RevenueNotInvoicedLL")));
				revenueExtrapolatedLL = revenueExtrapolatedLL.add((sonProject.get_Value("RevenueExtrapolatedLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("RevenueExtrapolatedLL")));
				costIssueProductLL = costIssueProductLL.add((sonProject.get_Value("CostIssueProductLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostIssueProductLL")));
				costIssueResourceLL = costIssueResourceLL.add((sonProject.get_Value("CostIssueResourceLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostIssueResourceLL")));
				costIssueInventoryLL = costIssueInventoryLL.add((sonProject.get_Value("CostIssueInventoryLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostIssueInventoryLL")));
				costIssueSumLL = costIssueSumLL.add((sonProject.get_Value("CostIssueSumLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostIssueSumLL")));
				costDiffExcecutionLL = costDiffExcecutionLL.add((sonProject.get_Value("CostDiffExcecutionLL") == null) ? Env.ZERO : ((BigDecimal)sonProject.get_Value("CostDiffExcecutionLL")));
			}
			pp.set_ValueOfColumn("CostPlannedLL", (Object)costPlannedLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostAmtLL", (Object)costAmtLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostNotInvoicedLL", (Object)costNotInvoicedLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostExtrapolatedLL", (Object)costExtrapolatedLL.setScale(2, 4));
			pp.set_ValueOfColumn("RevenuePlannedLL", (Object)revenuePlannedLL.setScale(2, 4));
			pp.set_ValueOfColumn("RevenueAmtLL", (Object)revenueAmtLL.setScale(2, 4));
			pp.set_ValueOfColumn("RevenueNotInvoicedLL", (Object)revenueNotInvoicedLL.setScale(2, 4));
			pp.set_ValueOfColumn("RevenueExtrapolatedLL", (Object)revenueExtrapolatedLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostIssueProductLL", (Object)costIssueProductLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostIssueResourceLL", (Object)costIssueResourceLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostIssueInventoryLL", (Object)costIssueInventoryLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostIssueSumLL", (Object)costIssueSumLL.setScale(2, 4));
			pp.set_ValueOfColumn("CostDiffExcecutionLL", (Object)costDiffExcecutionLL.setScale(2, 4));
			grossMarginLL = revenueExtrapolatedLL.subtract(costExtrapolatedLL).subtract(costIssueResourceLL).subtract(costIssueInventoryLL);
			if (grossMarginLL == null) {
				grossMarginLL = Env.ZERO;
			}
			pp.set_ValueOfColumn("GrossMarginLL", (Object)grossMarginLL.setScale(2, 4));
			pp.saveEx();
		}
		final int C_Project_Parent_ID = pp.get_ValueAsInt("C_Project_Parent_ID");
		if (C_Project_Parent_ID != 0) {
			final MProject fatherProject = new MProject(this.getCtx(), C_Project_Parent_ID, this.get_TrxName());
			result = this.calcCostOrRevenuePlannedSons(C_Project_Parent_ID, false, true);
			fatherProject.set_Value("CostPlannedLL", (Object)result.setScale(2, 4));
			result = this.calcCostOrRevenueActualSons(C_Project_Parent_ID, false, true);
			fatherProject.set_Value("CostAmtLL", (Object)result.setScale(2, 4));
			result = this.calcCostOrRevenueExtrapolatedSons(C_Project_Parent_ID, false, true);
			fatherProject.set_Value("CostExtrapolatedLL", (Object)result.setScale(2, 4));
			fatherProject.saveEx();
			final BigDecimal costActualFather2 = (BigDecimal)fatherProject.get_Value("CostAmt");
			final BigDecimal costPlannedFather2 = (BigDecimal)fatherProject.get_Value("CostPlanned");
			final BigDecimal costExtrapolatedFather2 = (BigDecimal)fatherProject.get_Value("CostExtrapolated");
			final BigDecimal revenueAmtSons = this.calcCostOrRevenueActualSons(C_Project_Parent_ID, true, true);
			final BigDecimal revenuePlannedSons = this.calcCostOrRevenuePlannedSons(C_Project_Parent_ID, true, true);
			final BigDecimal revenueAllExtrapolated = this.calcCostOrRevenueExtrapolatedSons(C_Project_Parent_ID, true, true);
			final BigDecimal weightFather2 = (BigDecimal)fatherProject.get_Value("Weight");
			final BigDecimal volumeFather2 = (BigDecimal)fatherProject.get_Value("Volume");
			final List<MProject> projectsOfFather2 = new Query(this.getCtx(), "C_Project", "C_Project_Parent_ID=?", this.get_TrxName()).setParameters(new Object[] { C_Project_Parent_ID }).list();
			for (final MProject sonProject2 : projectsOfFather2) {
				final BigDecimal revenueExtrapolatedSon2 = (BigDecimal)sonProject2.get_Value("RevenueExtrapolated");
				final BigDecimal weight2 = (BigDecimal)sonProject2.get_Value("Weight");
				BigDecimal volume2 = (BigDecimal)sonProject2.get_Value("volume");
				if (volume2 == null) {
					volume2 = Env.ZERO;
				}
				BigDecimal shareRevenue2 = Env.ZERO;
				BigDecimal shareWeight2 = Env.ZERO;
				BigDecimal shareVolume2 = Env.ZERO;
				if (revenueExtrapolatedSon2 != null && revenueAllExtrapolated.longValue() != 0L) {
					shareRevenue2 = revenueExtrapolatedSon2.divide(revenueAllExtrapolated, 5, 5);
				}
				if (weight2 != null && weightFather2 != null && weightFather2.longValue() != 0L) {
					shareWeight2 = weight2.divide(weightFather2, 5, 5);
				}
				if (volume2 != null && volumeFather2 != null && volumeFather2.longValue() != 0L) {
					shareVolume2 = volume2.divide(volumeFather2, 5, 5);
				}
				this.calcCostPlannedInherited(sonProject2, costPlannedFather2, costActualFather2, costExtrapolatedFather2, shareVolume2, shareWeight2, shareRevenue2);
			}
			fatherProject.set_ValueOfColumn("RevenuePlannedLL", (Object)revenuePlannedSons.setScale(2, 4));
			fatherProject.set_ValueOfColumn("RevenueAmtLL", (Object)revenueAmtSons.setScale(2, 4));
			fatherProject.set_ValueOfColumn("RevenueExtrapolatedLL", (Object)revenueAllExtrapolated.setScale(2, 4));
			fatherProject.saveEx();
			this.saveEx();
			pp.saveEx();
		}
		BigDecimal grossMarginTotal = ((BigDecimal)pp.get_Value("GrossMargin")).add(grossMarginLL);
		if (grossMarginTotal == null) {
			grossMarginTotal = Env.ZERO;
		}
		pp.set_ValueOfColumn("GrossMarginTotal", (Object)grossMarginTotal.setScale(2, 4));
		final Date date = new Date();
		final long time = date.getTime();
		final Timestamp timestamp = new Timestamp(time);
		pp.set_ValueOfColumn("DateLastRun", (Object)timestamp);
		pp.saveEx();
		return "";
	}

	private BigDecimal calcPlannedCostMaterial(int c_Project_ID) {

		String sql = "SELECT COALESCE(SUM(CostAmt),0)" +
				" FROM c_projectline" +
				" WHERE CostElementType = 'M' AND c_project_id = ?";

		BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), c_Project_ID);
		return result;
	}

	private BigDecimal calcPlannedCostResource(int c_Project_ID) {

		String sql = "SELECT COALESCE(SUM(CostAmt),0)" +
				" FROM c_projectline" +
				" WHERE CostElementType = 'R' AND c_project_id = ?";

		BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), c_Project_ID);
		return result;
	}

	private BigDecimal calcPlannedCostTools(int c_Project_ID) {

		String sql = "SELECT COALESCE(SUM(CostAmt),0)" +
				" FROM c_projectline" +
				" WHERE CostElementType = 'T' AND c_project_id = ?";

		BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), c_Project_ID);
		return result;
	}

	private BigDecimal calcCostOrRevenueActual(final int c_Project_ID, final boolean isSOTrx, final boolean isParentProject) {
		final String expresion = "LineNetAmtRealInvoiceLine(c_invoiceline_ID)";
		final StringBuffer whereClause = new StringBuffer();
		whereClause.append("c_invoice_ID IN (SELECT c_invoice_ID FROM c_invoice WHERE docstatus IN ('CO','CL') ");
		whereClause.append(" AND issotrx = ");
		whereClause.append(isSOTrx ? " 'Y') " : " 'N') ");
		if (isParentProject) {
			whereClause.append("AND c_project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
		}
		else {
			whereClause.append("AND c_project_ID = ? ");
		}
		BigDecimal result = Env.ZERO;
		result = new Query(this.getCtx(), "C_InvoiceLine", whereClause.toString(), this.get_TrxName()).setParameters(new Object[] { c_Project_ID }).aggregate(expresion, "SUM");
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostOrRevenuePlanned(final int c_Project_ID, final boolean isSOTrx, final boolean isParentProject) {
		final StringBuffer sql = new StringBuffer();
		sql.append("SELECT COALESCE (SUM( ");
		sql.append("CASE ");
		sql.append("     WHEN pl.istaxincluded = 'Y' ");
		sql.append("     THEN ");
		sql.append("         CASE ");
		sql.append("         WHEN o.docstatus in ('CL') ");
		sql.append("         THEN ((ol.qtyinvoiced * ol.priceactual)- (ol.qtyinvoiced * ol.priceactual)/(1+(t.rate/100)))  ");
		sql.append("         ELSE (ol.linenetamt- ol.linenetamt/(1+(t.rate/100))) ");
		sql.append("         END ");
		sql.append("     ELSE ");
		sql.append("         CASE ");
		sql.append("         WHEN o.docstatus IN ('CL') ");
		sql.append("         THEN (ol.qtyinvoiced * ol.priceactual) ");
		sql.append("         ELSE (ol.linenetamt) ");
		sql.append("         END ");
		sql.append("     END ");
		sql.append("),0) ");
		sql.append("FROM C_OrderLine ol ");
		sql.append("INNER JOIN c_order o ON ol.c_order_ID = o.c_order_ID ");
		sql.append("INNER JOIN m_pricelist pl ON o.m_pricelist_ID = pl.m_pricelist_ID ");
		sql.append("INNER JOIN c_tax t ON ol.c_tax_ID = t.c_tax_ID ");
		sql.append("WHERE ");
		sql.append("o.c_order_ID IN  (select c_order_ID from c_order where docstatus in ('CO','CL','IP')   AND issotrx =  ? ) ");
		if (isParentProject) {
			sql.append("AND o.c_project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
		}
		else {
			sql.append("AND o.c_project_ID = ? ");
		}
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(isSOTrx);
		params.add(c_Project_ID);
		final BigDecimal result = DB.getSQLValueBDEx((String)null, sql.toString(), (List)params);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcNotInvoicedCostOrRevenue(final int c_Project_ID, final boolean isSOTrx, final boolean isParentProject) {
		final StringBuffer sql = new StringBuffer();
		sql.append("SELECT COALESCE (SUM( ");
		sql.append("CASE ");
		sql.append("     WHEN pl.istaxincluded = 'Y' ");
		sql.append("     THEN ");
		sql.append("         CASE ");
		sql.append("         WHEN o.docstatus in ('CL') ");
		sql.append("         THEN 0  ");
		sql.append("         ELSE ((ol.qtyordered-ol.qtyinvoiced)*ol.Priceactual) - (taxamt_Notinvoiced(ol.c_Orderline_ID)) ");
		sql.append("         END ");
		sql.append("     ELSE ");
		sql.append("         CASE ");
		sql.append("         WHEN o.docstatus IN ('CL') ");
		sql.append("         THEN 0 ");
		sql.append("         ELSE ((ol.qtyordered-ol.qtyinvoiced)*ol.Priceactual) ");
		sql.append("         END ");
		sql.append("     END ");
		sql.append("),0) ");
		sql.append("FROM C_OrderLine ol ");
		sql.append("INNER JOIN c_order o ON ol.c_order_ID = o.c_order_ID ");
		sql.append("INNER JOIN m_pricelist pl ON o.m_pricelist_ID = pl.m_pricelist_ID ");
		sql.append("INNER JOIN c_tax t ON ol.c_tax_ID = t.c_tax_ID ");
		sql.append("WHERE ");
		sql.append("o.c_order_ID IN  (select c_order_ID from c_order where docstatus in ('CO','CL','IP')   AND issotrx =  ? ) ");
		if (isParentProject) {
			sql.append("AND o.c_project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
		}
		else {
			sql.append("AND o.c_project_ID = ? ");
		}
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(isSOTrx);
		params.add(c_Project_ID);
		final BigDecimal result = DB.getSQLValueBDEx((String)null, sql.toString(), (List)params);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostOrRevenueExtrapolated(final int c_Project_ID, final boolean isSOTrx, final boolean isParentProject) {
		final BigDecimal result = this.calcNotInvoicedCostOrRevenue(c_Project_ID, isSOTrx, isParentProject).add(this.calcCostOrRevenueActual(c_Project_ID, isSOTrx, isParentProject));
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostOrRevenueActualSons(final int c_Project_Parent_ID, final boolean isSOTrx, final boolean isParentProject) {
		final BigDecimal result = this.calcCostOrRevenueActual(c_Project_Parent_ID, isSOTrx, isParentProject);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostOrRevenuePlannedSons(final int c_Project_Parent_ID, final boolean isSOTrx, final boolean isParentProject) {
		final BigDecimal result = this.calcCostOrRevenuePlanned(c_Project_Parent_ID, isSOTrx, isParentProject);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcNotInvoicedCostOrRevenueSons(final int c_Project_Parent_ID, final boolean isSOTrx, final boolean isParentProject) {
		final BigDecimal result = this.calcNotInvoicedCostOrRevenue(c_Project_Parent_ID, isSOTrx, isParentProject);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostOrRevenueExtrapolatedSons(final int c_Project_Parent_ID, final boolean isSOTrx, final boolean isParentProject) {
		final BigDecimal result = this.calcNotInvoicedCostOrRevenueSons(c_Project_Parent_ID, isSOTrx, isParentProject).add(this.calcCostOrRevenueActualSons(c_Project_Parent_ID, isSOTrx, isParentProject));
		return (result == null) ? Env.ZERO : result;
	}

	private Boolean calcCostPlannedInherited(final MProject son, BigDecimal costPlannedFather, BigDecimal costActualFather, BigDecimal costExtrapolatedFather, BigDecimal shareVolume, BigDecimal shareWeight, BigDecimal shareRevenue) {
		if (son == null) {
			return true;
		}
		if (costPlannedFather == null) {
			costPlannedFather = Env.ZERO;
		}
		if (costActualFather == null) {
			costActualFather = Env.ZERO;
		}
		if (costExtrapolatedFather == null) {
			costExtrapolatedFather = Env.ZERO;
		}
		if (shareVolume == null) {
			shareVolume = Env.ZERO;
		}
		if (shareWeight == null) {
			shareWeight = Env.ZERO;
		}
		if (shareRevenue == null) {
			shareRevenue = Env.ZERO;
		}
		BigDecimal result = Env.ZERO;
		result = costPlannedFather.multiply(shareRevenue);
		son.set_Value("CostPlannedInherited", (Object)result);
		result = costPlannedFather.multiply(shareVolume);
		son.set_Value("CostPlannedVolumeInherited", (Object)result);
		result = costPlannedFather.multiply(shareWeight);
		son.set_Value("CostPlannedWeightInherited", (Object)result);
		result = costActualFather.multiply(shareRevenue);
		son.set_Value("CostAmtInherited", (Object)result);
		result = costActualFather.multiply(shareVolume);
		son.set_Value("CostAmtVolumeInherited", (Object)result);
		result = costActualFather.multiply(shareWeight);
		son.set_Value("CostAmtWeightInherited", (Object)result);
		result = costExtrapolatedFather.multiply(shareRevenue);
		son.set_Value("CostExtrapolatedInherited", (Object)result);
		result = costExtrapolatedFather.multiply(shareVolume);
		son.set_Value("CostExtrapolatedVolInherited", (Object)result);
		result = costExtrapolatedFather.multiply(shareWeight);
		son.set_Value("CostExtrapolatedWghtInherited", (Object)result);
		if (son.getC_Project_ID() != this.getC_Project_ID()) {
			son.saveEx();
		}
		return true;
	}

	private BigDecimal calcActualamt() {
		final StringBuffer sql = new StringBuffer();
		sql.append("select sum (actualamt) ");
		sql.append("from c_project_calculate_price ");
		sql.append("where C_Project_ID=?");
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(this.getC_Project_ID());
		final BigDecimal result = DB.getSQLValueBDEx((String)null, sql.toString(), (List)params);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcLineNetAmt() {
		final StringBuffer sql = new StringBuffer();
		sql.append("select sum (linenetamt) ");
		sql.append("from c_project_calculate_price ");
		sql.append("where C_Project_ID=?");
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(this.getC_Project_ID());
		final BigDecimal result = DB.getSQLValueBDEx((String)null, sql.toString(), (List)params);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostIssueProduct(final int c_Project_ID, final boolean isParentProject) {
		final StringBuffer sql = new StringBuffer();
		sql.append("SELECT COALESCE(SUM(cd.CostAmt + cd.CostAmtLL + cd.CostAdjustment + cd.CostAdjustmentLL),0) ");
		sql.append("FROM C_ProjectIssue pi ");
		sql.append("INNER JOIN M_CostDetail cd ON pi.c_ProjectIssue_ID=cd.c_ProjectIssue_ID ");
		if (isParentProject) {
			sql.append("WHERE pi.C_Project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
		}
		else {
			sql.append("WHERE pi.C_Project_ID=? ");
		}
		sql.append("AND pi.M_InOutLine_ID IS NOT NULL ");
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);
		final BigDecimal result = DB.getSQLValueBDEx((String)null, sql.toString(), (List)params);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostIssueResource(final int c_Project_ID, final boolean isParentProject) {
		final StringBuffer sql = new StringBuffer();
		sql.append("SELECT SUM (pl.committedamt) ");
		sql.append("FROM c_projectline pl ");
		sql.append("INNER JOIN c_project p ON (pl.c_project_id=p.c_project_id) ");
		if (isParentProject) {
			sql.append("WHERE pl.C_Project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
		}
		else {
			sql.append("WHERE pl.C_Project_ID=? ");
		}
		sql.append("AND pl.c_projectissue_ID IS NOT NULL ");
		sql.append("AND pl.s_timeexpenseline_ID IS NOT NULL ");
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);
		final BigDecimal result = DB.getSQLValueBDEx((String)null, sql.toString(), (List)params);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostIssueInventory(final int c_Project_ID, final boolean isParentProject) {
		final StringBuffer sql = new StringBuffer();
		sql.append("SELECT COALESCE(SUM(cd.CostAmt + cd.CostAmtLL + cd.CostAdjustment + cd.CostAdjustmentLL),0) ");
		sql.append("FROM C_ProjectIssue pi ");
		sql.append("INNER JOIN M_CostDetail cd ON pi.c_ProjectIssue_ID=cd.c_ProjectIssue_ID ");
		if (isParentProject) {
			sql.append("WHERE pi.C_Project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
		}
		else {
			sql.append("WHERE pi.C_Project_ID=? ");
		}
		sql.append("AND pi.M_InOutLine_ID IS  NULL ");
		sql.append("AND pi.s_timeexpenseline_ID IS NULL ");
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);
		final BigDecimal result = DB.getSQLValueBDEx((String)null, sql.toString(), (List)params);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostIssueProductSons(final int c_Project_Parent_ID, final boolean isParentProject) {
		final BigDecimal result = this.calcCostIssueProduct(c_Project_Parent_ID, isParentProject);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostIssueResourceSons(final int c_Project_Parent_ID, final boolean isParentProject) {
		final BigDecimal result = this.calcCostIssueResource(c_Project_Parent_ID, isParentProject);
		return (result == null) ? Env.ZERO : result;
	}

	private BigDecimal calcCostIssueInventorySons(final int c_Project_Parent_ID, final boolean isParentProject) {
		final BigDecimal result = this.calcCostIssueInventory(c_Project_Parent_ID, isParentProject);
		return (result == null) ? Env.ZERO : result;
	}

	private String updateProjectPerformanceCalculationSons(final int c_Project_ID, final int levelCount) {
		if (levelCount == 5) {
			return "";
		}
		final String whereClause = "C_Project_Parent_ID=?";
		final ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);
		final List<MProject> childrenProjects = new Query(this.getCtx(), "C_Project", whereClause, this.get_TrxName()).setParameters((List)params).list();
		final MProject project = new MProject(this.getCtx(), c_Project_ID, this.get_TrxName());
		if (childrenProjects == null) {
			project.updateProjectPerformanceCalculation();
			return "";
		}
		for (final MProject childProject : childrenProjects) {
			this.updateProjectPerformanceCalculationSons(childProject.getC_Project_ID(), levelCount + 1);
		}
		project.updateProjectPerformanceCalculation();
		return "";
	}
}	//	MProject
