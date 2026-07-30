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

package org.openup.core.process;

import org.adempiere.core.domains.models.I_S_Contract;
import org.adempiere.core.domains.models.I_S_TimeExpense;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.*;
import org.compiere.process.DocumentEngine;
import org.compiere.util.Env;
import org.eevolution.context.service.infrastructure.domain.entities.MSContract;
import org.openup.core.model.I_S_ContractDiscount;
import org.openup.core.model.MSContractDiscount;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/** Generated Process for (Generar Informe de Gastos desde Contrato de Servicio)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.2
 */
public class ServiceContractGenTimeExpense extends ServiceContractGenTimeExpenseAbstract
{

	private MSContract msContract;
	private List<MSContractDiscount> msContractDiscounts;
	private HashMap<Integer, List<PO>> msContractLinesSorted;
	private Timestamp startDate;
	private Timestamp endDate;
	private List<MTimeExpense> mTimeExpenses;
	private int teCount;

	@Override
	protected void prepare()
	{
		super.prepare();

	}

	@Override
	protected String doIt() throws Exception {

		teCount = 0;

		String whereClause = "";
		List<Object> params = new ArrayList<>();

		if (getContractId() > 0 || getRecord_ID() > 0) {
			whereClause += "S_Contract_ID=?";
			params.add(getContractId() > 0 ? getContractId() : getRecord_ID());
		} else {
			whereClause = "IsActive = 'Y'";
		}

		if (getOrgId() > 0) {
			if (!whereClause.isEmpty()) {
				whereClause += " AND ";
			}
			whereClause += "AD_Org_ID=?";
			params.add(getOrgId());
		}

		if(getBPGroupId() > 0){
			if (!whereClause.isEmpty()) {
				whereClause += " AND ";
			}
			whereClause += I_S_Contract.COLUMNNAME_S_Contract_ID + " IN (SELECT S_Contract_ID FROM S_Contract c" +
					" JOIN C_BPartner p ON c.Bill_BPartner_ID=p.C_BPartner_ID" +
					" WHERE c.S_Contract_ID = " + I_S_Contract.COLUMNNAME_S_Contract_ID + " AND p.C_BP_Group_ID = ?)";
			params.add(getBPGroupId());
		}

		List<MSContract> msContracts = new Query(getCtx(), I_S_Contract.Table_Name, whereClause, get_TrxName())
			.setParameters(params)
			.list();

		for (MSContract contract : msContracts) {
			msContract = contract;
			generateByContract();
		}

		return "Informes de Gasto generados: " + teCount;
	}

	private void generateByContract() {
		startDate = msContract.getDateStart();
		if (startDate == null) {
			throw new AdempiereException("@S_Contract_ID@ @DateStart@ @not.found@");
		}
		if (msContract.isIndefinite()) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(startDate);
			cal.add(Calendar.MONTH, 12);
			endDate = new Timestamp(cal.getTimeInMillis());
		} else {
			if(msContract.getDateFinishSchedule() != null){
				endDate = msContract.getDateFinishSchedule();
			} else {
				throw new AdempiereException("@S_Contract_ID@ @DateFinishSchedule@ @not.found@");
			}
		}

		if(getDateTo() != null){
			if(getDateTo().compareTo(endDate) < 0){
				endDate = getDateTo();
			}
		}

		msContractLinesSorted = new HashMap<>();
		mTimeExpenses = new ArrayList<>();
		List<PO> sContractLines = new Query(getCtx(), "S_ContractLine", "S_Contract_ID=?", get_TrxName())
			.setParameters(msContract.get_ID()).list(PO.class);
		for (PO sContractLine : sContractLines) {
			log.info(sContractLine.toString());
			if (sContractLine.get_ValueAsBoolean("IsRecurrent")) {
				putContractLine(sContractLine);
			}
		}

		msContractDiscounts = null;
		msContractDiscounts = new Query(getCtx(), I_S_ContractDiscount.Table_Name, I_S_ContractDiscount.COLUMNNAME_S_Contract_ID + "=?", get_TrxName())
				.setParameters(msContract.get_ID())
				.list();

		iterateContractLineByFrequency();
		processTimeExpens();
	}

	private void putContractLine(PO contractLine) {
		int frequency = contractLine.get_ValueAsInt("Frequency");
		List<PO> contractLineByFrequency = msContractLinesSorted.computeIfAbsent(frequency, k -> new ArrayList<>());
		contractLineByFrequency.add(contractLine);
	}

	private void iterateContractLineByFrequency() {
		msContractLinesSorted.forEach((frequency, contractLines) -> {
			createNextTimeExpense(startDate, frequency, contractLines);
		});
	}

	private void createNextTimeExpense(Timestamp currentDate, int frequency, List<PO> contractLines) {
		Timestamp nextDate = currentDate;
		if (nextDate == null) {
			throw new AdempiereException("@StartDate@ @not.found@");
		}
		if (endDate == null) {
			throw new AdempiereException("@EndDate@ @not.found@");
		}
		while (frequency > 0 && nextDate.compareTo(endDate) < 0) {
			createTimeExpense(nextDate, contractLines);

			// Setting next date to run
			Calendar cal = Calendar.getInstance();
			cal.setTime(nextDate);
			cal.add(Calendar.MONTH, frequency);

			nextDate = new Timestamp(cal.getTimeInMillis());
		}
	}

	private void createTimeExpense(Timestamp dateTrx, List<PO> contractLines) {
		log.info("Instantiating Time Expense " + dateTrx + ", Count of Lines: " + contractLines.size());


		String dateFrom = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(dateTrx);
		String dateTo = new SimpleDateFormat("yyyy-MM-dd 23:59:59").format(dateTrx);

		String existwhere = "S_Contract_ID=? AND (" + I_S_TimeExpense.COLUMNNAME_DateReport + " >= '" + dateFrom + "' AND " + I_S_TimeExpense.COLUMNNAME_DateReport + " <= '" + dateTo + "') AND " + I_S_TimeExpense.COLUMNNAME_DocStatus + " ='CO' AND IsSelfService='N'";
		boolean alreadyCreated = new Query(getCtx(), I_S_TimeExpense.Table_Name, existwhere, get_TrxName())
				.setParameters(msContract.get_ID())
				.match();

		if (!alreadyCreated) {

			MTimeExpense mTimeExpense = new MTimeExpense(getCtx(), 0, get_TrxName());
			mTimeExpenses.add(mTimeExpense);

			mTimeExpense.setC_BPartner_ID(msContract.getC_BPartner_ID());
			mTimeExpense.set_ValueOfColumn("Bill_BPartner_ID", msContract.getBill_BPartner_ID());
			mTimeExpense.setDateReport(dateTrx);
			mTimeExpense.setM_PriceList_ID(msContract.getM_PriceList_ID());
			mTimeExpense.setM_Warehouse_ID(msContract.getM_Warehouse_ID());
			mTimeExpense.set_ValueOfColumn("S_Contract_ID", msContract.get_ID());
			mTimeExpense.saveEx();

			int line = 10;
			for (PO contractLine : contractLines) {
				MProduct mProduct = new MProduct(getCtx(), contractLine.get_ValueAsInt("M_Product_ID"), get_TrxName());

				BigDecimal amt = (BigDecimal) contractLine.get_Value("PriceEntered");

				MTimeExpenseLine mTimeExpenseLine = new MTimeExpenseLine(getCtx(), 0, get_TrxName());
				mTimeExpenseLine.setS_TimeExpense_ID(mTimeExpense.get_ID());
				mTimeExpenseLine.set_ValueOfColumn("Line", line);
				mTimeExpenseLine.setM_Product_ID(mProduct.get_ID());
				mTimeExpenseLine.setC_Tax_ID(contractLine.get_ValueAsInt("C_Tax_ID"));
				mTimeExpenseLine.setQty((BigDecimal) contractLine.get_Value("QtyEntered"));
				mTimeExpenseLine.setExpenseAmt(amt);
				mTimeExpenseLine.setDateExpense(dateTrx);
				mTimeExpenseLine.setDescription(contractLine.get_ValueAsString("Description"));
				mTimeExpenseLine.setC_Activity_ID(contractLine.get_ValueAsInt("C_Activity_ID"));
				mTimeExpenseLine.set_ValueOfColumn("S_ContractLine_ID", contractLine.get_ID());
				mTimeExpenseLine.setC_Currency_ID(msContract.getC_Currency_ID());

				if (msContractDiscounts != null && !msContractDiscounts.isEmpty()){
					MSContractDiscount msContractDiscount = new MSContractDiscount(getCtx(), msContractDiscounts.get(0).get_ID(), get_TrxName());
					mTimeExpenseLine.set_ValueOfColumn("M_DiscountSchema_ID", msContractDiscount.getM_DiscountSchema_ID());
				}

				mTimeExpenseLine.saveEx();

				contractLine.set_ValueOfColumn("IsPrinted", true);
				contractLine.saveEx();

				line += 10;


				if (isDiscountLineAmt() && msContractDiscounts != null) {
					for (MSContractDiscount msContractDiscount : msContractDiscounts) {
						// TODO iterar y crear las lineas de timeexpense con los descuentos si la categoría de producto coincide descuento y producto
						if (msContractDiscount.getM_Product_Category_ID() == mProduct.getM_Product_Category_ID()) {
							MDiscountSchema discountSchema = MDiscountSchema.get(msContractDiscount.getCtx(), msContractDiscount.getM_DiscountSchema_ID());
							if(discountSchema.get_ValueAsInt("M_Product_ID") <= 0) {
								throw new AdempiereException("@M_DiscountSchema@ " + msContractDiscount.getM_DiscountSchema().getName() + " @not.found@ @M_Product_ID@");
							}
							MProduct discountProduct = MProduct.get(discountSchema.getCtx(), discountSchema.get_ValueAsInt("M_Product_ID"));
							BigDecimal discountAmt = amt.multiply(msContractDiscount.getDiscount()).divide(Env.ONEHUNDRED).negate();

							MTimeExpenseLine mTimeExpenseLineDiscount = new MTimeExpenseLine(getCtx(), 0, get_TrxName());
							mTimeExpenseLineDiscount.setS_TimeExpense_ID(mTimeExpense.get_ID());
							mTimeExpenseLineDiscount.set_ValueOfColumn("Line", line);
							mTimeExpenseLineDiscount.setM_Product_ID(discountProduct.get_ID());
							mTimeExpenseLineDiscount.setC_Tax_ID(contractLine.get_ValueAsInt("C_Tax_ID"));
							mTimeExpenseLineDiscount.setQty(Env.ONE);
							mTimeExpenseLineDiscount.setExpenseAmt(discountAmt);
							mTimeExpenseLineDiscount.setDateExpense(dateTrx);
							//mTimeExpenseLineDiscount.setDescription(contractLine.get_ValueAsString("Description"));
							mTimeExpenseLineDiscount.setDescription(msContractDiscount.get_ValueAsString("Description"));
							mTimeExpenseLineDiscount.setC_Activity_ID(contractLine.get_ValueAsInt("C_Activity_ID"));
							mTimeExpenseLineDiscount.set_ValueOfColumn("S_ContractLine_ID", contractLine.get_ID());

							mTimeExpenseLineDiscount.saveEx();

							line += 10;
						}
					}
				}
			}

			teCount++;
		}
	}

	private void processTimeExpens() {
		for (MTimeExpense mTimeExpens : mTimeExpenses) {
			mTimeExpens.processIt(DocumentEngine.ACTION_Complete);
			mTimeExpens.saveEx();
		}
	}
}
