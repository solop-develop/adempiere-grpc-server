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

import org.adempiere.core.domains.models.I_C_BP_Group_Acct;
import org.adempiere.core.domains.models.I_C_RevenueRecognition_Plan;
import org.adempiere.core.domains.models.I_C_RevenueRecognition_Run;
import org.adempiere.core.domains.models.I_C_ValidCombination;
import org.adempiere.core.domains.models.X_C_RevenueRecognition_Plan;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 *	Revenue Recognition Plan
 *	
 *  @author Jorg Janke
 *  @version $Id: MRevenueRecognitionPlan.java,v 1.2 2006/07/30 00:51:05 jjanke Exp $
 */
public class MRevenueRecognitionPlan extends X_C_RevenueRecognition_Plan
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6748195415080148091L;


	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_RevenueRecognition_Plan_ID id
	 */
	public MRevenueRecognitionPlan(Properties ctx, int C_RevenueRecognition_Plan_ID, String trxName)
	{
		super (ctx, C_RevenueRecognition_Plan_ID, trxName);
		if (C_RevenueRecognition_Plan_ID == 0)
		{
		//	setC_AcctSchema_ID (0);
		//	setC_Currency_ID (0);
		//	setC_InvoiceLine_ID (0);
		//	setC_RevenueRecognition_ID (0);
		//	setC_RevenueRecognition_Plan_ID (0);
		//	setP_Revenue_Acct (0);
		//	setUnEarnedRevenue_Acct (0);
			setTotalAmt (Env.ZERO);
			setRecognizedAmt (Env.ZERO);
		}	
	}	//	MRevenueRecognitionPlan

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 */
	public MRevenueRecognitionPlan(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MRevenueRecognitionPlan

	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return success
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (newRecord)
		{
			MRevenueRecognition rr = new MRevenueRecognition(getCtx(), getC_RevenueRecognition_ID(), get_TrxName());
			if (rr.isTimeBased())
			{
				/**	Get InvoiveQty
				SELECT	QtyInvoiced, M_Product_ID 
				  INTO	v_Qty, v_M_Product_ID
				FROM	C_InvoiceLine 
				WHERE 	C_InvoiceLine_ID=:new.C_InvoiceLine_ID;
				--	Insert
				AD_Sequence_Next ('C_ServiceLevel', :new.AD_Client_ID, v_NextNo);
				INSERT INTO C_ServiceLevel
					(C_ServiceLevel_ID, C_RevenueRecognition_Plan_ID,
					AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,
					M_Product_ID, Description, ServiceLevelInvoiced, ServiceLevelProvided,
					Processing,Processed)
				VALUES
					(v_NextNo, :new.C_RevenueRecognition_Plan_ID,
					:new.AD_Client_ID,:new.AD_Org_ID,'Y',SysDate,:new.CreatedBy,SysDate,:new.UpdatedBy,
					v_M_Product_ID, NULL, v_Qty, 0,
					'N', 'N');
				**/
			}
		}
		return success;
	}	//	afterSave

	/**
	 * Update Recognized Amount
	 */
	public void updateRecognizedAmount(Timestamp runningDate) {
		BigDecimal recognizedAmt = new Query(getCtx(), I_C_RevenueRecognition_Run.Table_Name, I_C_RevenueRecognition_Run.COLUMNNAME_C_RevenueRecognition_Plan_ID + " = ?", get_TrxName())
				.setParameters(getC_RevenueRecognition_Plan_ID())
				.sum(I_C_RevenueRecognition_Run.COLUMNNAME_RecognizedAmt);
		MRevenueRecognition recognition = (MRevenueRecognition) getC_RevenueRecognition();
		int recognizedQuantity = DB.getSQLValue(get_TrxName(), "SELECT COUNT(DISTINCT to_char(r.DateDoc, 'YYYY-MM')) " +
				"FROM C_RevenueRecognition_Run r " +
				"WHERE C_RevenueRecognition_Plan_ID = ? " +
				"AND r.DocStatus IN('CO', 'CL', 'VO', 'RE')", getC_RevenueRecognition_Plan_ID());
		if(recognizedAmt == null) {
			recognizedAmt = Env.ZERO;
		}
		setRecognizedAmt(recognizedAmt);
		setRecognizedRunQty(recognizedQuantity);
		setIsRecognized(Optional.ofNullable(getTotalAmt()).orElse(Env.ZERO).compareTo(recognizedAmt) == 0);
		if(runningDate != null) {
			setDateLastRun(runningDate);
		}
		saveEx();
	}

	public static List<MRevenueRecognitionPlan> getPlansFromInvoiceAndSchema(MInvoice invoice, int accSchemaId) {
		return new Query(invoice.getCtx(), I_C_RevenueRecognition_Plan.Table_Name, "C_Invoice_ID = ? AND C_AcctSchema_ID = ?", invoice.get_TrxName())
				.setParameters(invoice.getC_Invoice_ID(), accSchemaId)
				.<MRevenueRecognitionPlan>list();
	}

	public static List<MRevenueRecognitionPlan> getPlansFromInvoice(MInvoice invoice) {
		return new Query(invoice.getCtx(), I_C_RevenueRecognition_Plan.Table_Name, "C_Invoice_ID = ?", invoice.get_TrxName())
				.setParameters(invoice.getC_Invoice_ID())
				.<MRevenueRecognitionPlan>list();
	}

	public static List<MRevenueRecognitionPlan> getPlansFromOrder(MOrder order) {
		return new Query(order.getCtx(), I_C_RevenueRecognition_Plan.Table_Name, "C_Order_ID = ?", order.get_TrxName())
				.setParameters(order.getC_Order_ID())
				.<MRevenueRecognitionPlan>list();
	}

	public static MRevenueRecognitionPlan getPlanFromInvoiceLineAndSchema(MInvoiceLine invoiceLine, int accSchemaId) {
		return new Query(invoiceLine.getCtx(), I_C_RevenueRecognition_Plan.Table_Name, "C_InvoiceLine_ID = ? AND C_AcctSchema_ID = ?", invoiceLine.get_TrxName())
				.setParameters(invoiceLine.getC_InvoiceLine_ID(), accSchemaId)
				.<MRevenueRecognitionPlan>first();
	}

	public static MRevenueRecognitionPlan getPlanFromOrderLineAndSchema(MOrderLine orderLine, int accSchemaId) {
		return new Query(orderLine.getCtx(), I_C_RevenueRecognition_Plan.Table_Name, "C_OrderLine_ID = ? AND C_AcctSchema_ID = ?", orderLine.get_TrxName())
				.setParameters(orderLine.getC_OrderLine_ID(), accSchemaId)
				.<MRevenueRecognitionPlan>first();
	}

	public MRevenueRecognitionRun getPreviousValidRecognitionRun(Timestamp mothDate) {
		return new Query(getCtx(), I_C_RevenueRecognition_Run.Table_Name, "C_RevenueRecognition_Plan_ID = ? AND DocStatus = 'CO' AND DateDoc <= ?", get_TrxName())
				.setParameters(getC_RevenueRecognition_Plan_ID(), TimeUtil.getMonthLastDay(mothDate))
				.setOrderBy(I_C_RevenueRecognition_Run.Table_Name + "." + I_C_RevenueRecognition_Run.COLUMNNAME_C_RevenueRecognition_Run_ID + " DESC")
				.<MRevenueRecognitionRun>first();
	}

	public MRevenueRecognitionRun getLastValidRecognitionRunForDate(Timestamp mothDate) {
		return new Query(getCtx(), I_C_RevenueRecognition_Run.Table_Name, "C_RevenueRecognition_Plan_ID = ? AND DocStatus = 'CO' AND DateDoc >= ? AND DateDoc <= ?", get_TrxName())
				.setParameters(getC_RevenueRecognition_Plan_ID(), TimeUtil.getMonthFirstDay(mothDate), TimeUtil.getMonthLastDay(mothDate))
				.setOrderBy(I_C_RevenueRecognition_Run.Table_Name + "." + I_C_RevenueRecognition_Run.COLUMNNAME_C_RevenueRecognition_Run_ID + " DESC")
				.<MRevenueRecognitionRun>first();
	}

	public List<Integer> getAllRecognitionsRun() {
		return new Query(getCtx(), I_C_RevenueRecognition_Run.Table_Name, "C_RevenueRecognition_Plan_ID = ?", get_TrxName())
				.setParameters(getC_RevenueRecognition_Plan_ID())
				.getIDsAsList();
	}

	public void reverseAllRecognitionRuns() {
		getAllRecognitionsRun().forEach(runId -> {
			MRevenueRecognitionRun run = new MRevenueRecognitionRun(getCtx(), runId, get_TrxName());
			run.reverseIt(false);
		});
		setIsRecognized(true);
		saveEx();
	}

	public static int getFinalAccountType(boolean isSOTrx, boolean isItem) {
		if(isSOTrx) {
			return ProductCost.ACCTTYPE_P_Revenue;
		}
		if(isItem) {
			return ProductCost.ACCTTYPE_P_InventoryClearing;
		}
		return ProductCost.ACCTTYPE_P_Expense;
	}

	public void setAccountDimensions(PO source) {
		if(source == null) {
			return;
		}
		if(source.get_ColumnIndex(COLUMNNAME_C_BPartner_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_C_BPartner_ID) > 0) {
			setC_BPartner_ID(source.get_ValueAsInt(COLUMNNAME_C_BPartner_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_S_Contract_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_S_Contract_ID) > 0) {
			setS_Contract_ID(source.get_ValueAsInt(COLUMNNAME_S_Contract_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_C_Project_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_C_Project_ID) > 0) {
			setC_Project_ID(source.get_ValueAsInt(COLUMNNAME_C_Project_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_C_Campaign_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_C_Campaign_ID) > 0) {
			setC_Campaign_ID(source.get_ValueAsInt(COLUMNNAME_C_Campaign_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_C_Activity_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_C_Activity_ID) > 0) {
			setC_Activity_ID(source.get_ValueAsInt(COLUMNNAME_C_Activity_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_C_SalesRegion_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_C_SalesRegion_ID) > 0) {
			setC_SalesRegion_ID(source.get_ValueAsInt(COLUMNNAME_C_SalesRegion_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_AD_OrgTrx_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_AD_OrgTrx_ID) > 0) {
			setAD_OrgTrx_ID(source.get_ValueAsInt(COLUMNNAME_AD_OrgTrx_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_User1_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_User1_ID) > 0) {
			setUser1_ID(source.get_ValueAsInt(COLUMNNAME_User1_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_User2_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_User2_ID) > 0) {
			setUser2_ID(source.get_ValueAsInt(COLUMNNAME_User2_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_User3_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_User3_ID) > 0) {
			setUser3_ID(source.get_ValueAsInt(COLUMNNAME_User3_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_User4_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_User4_ID) > 0) {
			setUser4_ID(source.get_ValueAsInt(COLUMNNAME_User4_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_UserElement1_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_UserElement1_ID) > 0) {
			setUserElement1_ID(source.get_ValueAsInt(COLUMNNAME_UserElement1_ID));
		}
		if(source.get_ColumnIndex(COLUMNNAME_UserElement2_ID) > 0 && source.get_ValueAsInt(COLUMNNAME_UserElement2_ID) > 0) {
			setUserElement2_ID(source.get_ValueAsInt(COLUMNNAME_UserElement2_ID));
		}
	}

	public static int getUnearnedRevenueAccountId(Properties context, int businessPartnerGroupId, int acctSchemaId) {
		I_C_BP_Group_Acct groupAccount = new Query(context, I_C_BP_Group_Acct.Table_Name, "C_BP_Group_ID = ? AND C_AcctSchema_ID = ?", null)
				.setParameters(businessPartnerGroupId, acctSchemaId)
				.setOnlyActiveRecords(true)
				.first();
		if(groupAccount != null) {
			I_C_ValidCombination combination = groupAccount.getUnEarnedRevenue_A();
			return combination.getAccount_ID();
		}
		return -1;
	}
}	//	MRevenueRecognitionPlan
