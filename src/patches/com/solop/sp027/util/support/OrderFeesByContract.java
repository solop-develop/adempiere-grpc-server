/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                     *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package com.solop.sp027.util.support;

import com.solop.sp027.util.CriteriaUtil;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MCommission;
import org.compiere.model.MCommissionAmt;
import org.compiere.model.MCommissionDetail;
import org.compiere.model.MCommissionRun;
import org.compiere.model.MCurrency;
import org.compiere.model.MOrder;
import org.compiere.model.MProject;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.util.ICommissionCalculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Calculate Fees for SalesOrder based on Contract
 * @author Gabriel Escalona
 */
public class OrderFeesByContract implements ICommissionCalculation {

	public static OrderFeesByContract newInstance() {
		return new OrderFeesByContract();
	}

	@Override
	public void processCommission(Properties context, int businessPartnerId, int commissionId, int commissionRunId, String transactionName) {
		MCommissionRun commissionRun = new MCommissionRun(context, commissionRunId, transactionName);
		MCommission commission = new MCommission(context, commissionId, transactionName);
		int orderId = commissionRun.get_ValueAsInt("SP027_Order_ID");
		if (orderId <= 0) {
			return;
		}
		MOrder order = new MOrder (context, orderId, transactionName);
		if (!order.isSOTrx()) {
			return;
		}
		int contractId = order.get_ValueAsInt("S_Contract_ID");
		if (contractId <= 0 && order.getC_Project_ID() > 0) {
			MProject project = MProject.getById(context, order.getC_Project_ID(), transactionName);
			contractId = project.get_ValueAsInt("S_Contract_ID");
		}
		if (contractId <= 0) {
			return;
		}
		businessPartnerId = order.getC_BPartner_ID();


		CriteriaUtil criteria = CriteriaUtil.newInstance().withCommission(commission);
		MTable contractFeesTable = MTable.get(context, "SP027_ContractFees");

		String whereClause = "S_Contract_ID = ?";
		List<Integer> contractFeesIds = new Query(context, "SP027_ContractFees", whereClause, transactionName)
			.setParameters(contractId)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.getIDsAsList();

		int finalBusinessPartnerId = businessPartnerId;
		contractFeesIds.forEach(contractCommissionId ->{
			PO contractFee = contractFeesTable.getPO(contractCommissionId, transactionName);
			BigDecimal commissionRate = (BigDecimal)contractFee.get_Value("SP027_CommissionRate");
			if (commissionRate == null || commissionRate.signum() == 0 ) {
				return;
			}
			criteria.clear();
			StringBuilder sql = new StringBuilder();
			List<Object> parameters = new ArrayList<>();
			//	Mandatory Columns
			if(commission.isListDetails()) {
				sql.append("SELECT o.DateOrdered AS DateDoc, o.C_Currency_ID, ol.LineNetAmt AS Amount, ol.QtyEntered Quantity");
				//	Reference Columns
				sql.append(", o.DocumentNo AS Reference, COALESCE(p.Value || ' - ' || p.Name, c.Name, '') AS Info, ol.C_OrderLine_ID");
				//	FROM
				sql.append(" FROM C_Order o"
						+ " INNER JOIN C_OrderLine ol ON(ol.C_Order_ID = o.C_Order_ID)"
						+ " LEFT JOIN M_Product p ON(p.M_Product_ID = ol.M_Product_ID)"
						+ " LEFT JOIN C_Charge c ON(c.C_Charge_ID = ol.C_Charge_ID)");
			} else {
				sql.append("SELECT ? AS DateDoc, o.C_Currency_ID, SUM(ol.LineNetAmt) AS Amount, SUM(ol.QtyEntered) Quantity");
				//	Reference Columns
				sql.append(", '' AS Reference, '' AS Info, NULL AS C_OrderLine_ID");
				//	FROM
				sql.append(" FROM C_Order o"
						+ " INNER JOIN C_OrderLine ol ON(ol.C_Order_ID = o.C_Order_ID)"
						+ " LEFT JOIN M_Product p ON(p.M_Product_ID = ol.M_Product_ID)"
						+ " LEFT JOIN C_Charge c ON(c.C_Charge_ID = ol.C_Charge_ID)");
				parameters.add(commissionRun.getDateDoc());
			}
			//	Default Where
			criteria.addWhereClause(" WHERE o.DocStatus IN('CO', 'CL', 'IP') AND o.IsSOTrx = 'Y' " +
					" AND o.C_Order_ID = ? " +
					" AND o.C_BPartner_ID = ?");

			criteria.addParameter(orderId);
			criteria.addParameter(finalBusinessPartnerId);

			//	For Currency
			//criteria.validateAndAddReference(commission.getC_Currency_ID(), "i.C_Currency_ID");
			//	Add all criteria from Commission Line
			addCriteria(contractFee, criteria);
			//
			sql.append(criteria.getWhereClause());
			parameters.addAll(criteria.getParameters());
			//	Add Group By
			if(!commission.isListDetails()) {
				//	Group By
				sql.append(" GROUP BY o.C_Currency_ID");
			}
			//	Process
			processCommission(commission, contractFee, commissionRun, finalBusinessPartnerId, sql.toString(), parameters, transactionName);

		});
		commission.setDateLastRun(new Timestamp(System.currentTimeMillis()));
		commission.saveEx();
	}

	/**
	 * Process Commission Line by Seller
	 * @param commission
	 * @param contractFee
	 * @param commissionRun
	 * @param businessPartnerId
	 * @param query
	 * @param parameters
	 * @param transactionName
	 */
	private void processCommission(MCommission commission, PO contractFee, MCommissionRun commissionRun, int businessPartnerId, String query, List<Object> parameters, String transactionName) {
		MCommissionAmt commissionAmt = new MCommissionAmt(commission.getCtx(), 0, transactionName);
		commissionAmt.setAD_Org_ID(commissionRun.getAD_Org_ID());
		commissionAmt.setC_CommissionRun_ID (commissionRun.getC_CommissionRun_ID());

		commissionAmt.setC_BPartner_ID(businessPartnerId);
		commissionAmt.saveEx();
		AtomicBoolean hasLines = new AtomicBoolean(false);
		DB.runResultSet(transactionName, query, parameters, resultSet -> {
			while (resultSet.next()) {
				hasLines.set(true);
				processCommissionLine(resultSet, commission, contractFee, commissionRun, commissionAmt);
			}
		}).onFailure(throwable -> {
			throw new AdempiereException(throwable);
		});
		if(hasLines.get()) {
			BigDecimal commissionPercentage = (BigDecimal)contractFee.get_Value("SP027_CommissionRate");
			commissionAmt.setPercentage(commissionPercentage);
			commissionAmt.updateCommissionAmount();
			commissionAmt.saveEx();
		} else {
			commissionAmt.deleteEx(true, transactionName);
		}
	}

	/**
	 * Process Commission Line
	 * @param resultSet
	 * @param commission
	 * @param contractFee
	 * @param commissionRun
	 * @param commissionAmt
	 * @throws SQLException
	 */
	private void processCommissionLine(ResultSet resultSet, MCommission commission, PO contractFee, MCommissionRun commissionRun, MCommissionAmt commissionAmt) throws SQLException {
		int currencyId = resultSet.getInt("C_Currency_ID");
		BigDecimal amount = Optional.ofNullable(resultSet.getBigDecimal("Amount")).orElse(Env.ZERO);
		BigDecimal quantity = Optional.ofNullable(resultSet.getBigDecimal("Quantity")).orElse(Env.ZERO);
		Timestamp date = resultSet.getTimestamp("DateDoc");
		String reference = resultSet.getString("Reference");
		String info = resultSet.getString("Info");
		int orderLineId = resultSet.getInt("C_OrderLine_ID");
		MCommissionDetail commissionDetail = new MCommissionDetail (commissionAmt, currencyId, amount, quantity);
		commissionDetail.setLineIDs(orderLineId, 0);
		//	Reference, Info,
		if (!Util.isEmpty(reference)) {
			commissionDetail.setReference(reference);
		}
		if (!Util.isEmpty(info)) {
			commissionDetail.setInfo(info);
		}
		//	Convert
		commissionDetail.setConvertedAmt(date);
		//	Rounding data
		int roundingCurrencyId = commissionRun.getC_Currency_ID();
		if(roundingCurrencyId <= 0) {
			roundingCurrencyId = commission.getC_Currency_ID();
		}
		int precision = MCurrency.getStdPrecision(commissionAmt.getCtx(), roundingCurrencyId);
		//	Calculate commission by line
		calculateCommission(contractFee, commissionDetail, precision);
	}

	/**
	 * Calculate commission by line
	 * @param contractFee
	 * @param commissionDetail
	 * @param precision
	 */
	public void calculateCommission(PO contractFee, MCommissionDetail commissionDetail, int precision) {
		//	Calculate
		BigDecimal convertedAmt = commissionDetail.getConvertedAmt();
		BigDecimal commissionAmount = commissionDetail.getConvertedAmt();
		if (convertedAmt == null) {
			convertedAmt = Env.ZERO;
		}
		//	Commission Amount
		BigDecimal multiplier = Optional.ofNullable((BigDecimal)contractFee.get_Value("SP027_CommissionRate"))
				.orElse(BigDecimal.ZERO);
		multiplier = multiplier.divide(Env.ONEHUNDRED, MathContext.DECIMAL128);


		commissionAmount = commissionAmount.multiply(multiplier);
		//	Scale
		if (convertedAmt.scale() > precision) {
			convertedAmt = convertedAmt.setScale(precision, RoundingMode.HALF_UP);
		}
		if (commissionAmount.scale() > precision) {
			commissionAmount = commissionAmount.setScale(precision, RoundingMode.HALF_UP);
		}
		//	Set Commission
		commissionDetail.setConvertedAmt(convertedAmt);
		commissionDetail.setCommissionAmt(commissionAmount);
		commissionDetail.saveEx();
	}


	private void addCriteria(PO contractFee, CriteriaUtil criteria) {
		//	Organization
		criteria.validateAndAddReference(contractFee.getAD_Org_ID(), "o.AD_Org_ID");
		//	BPartner Group
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("C_BP_Group_ID"), "o.C_BPartner_ID", "C_BPartner", "C_BP_Group_ID");
		//	Product
		criteria.validateAndAddReference(contractFee.get_ValueAsInt("M_Product_ID"), "ol.M_Product_ID");
		//	Product Category
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Product_Category_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Category_ID");
		//	Product Group
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Product_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Group_ID");
		//	Product Class
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Product_Class_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Class_ID");
		//	Product Classification
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Product_Classification_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Classification_ID");
		//Industry Sector
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Industry_Sector_ID"), "ol.M_Product_ID", "M_Product", "M_Industry_Sector_ID");
		//Material Group
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Material_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Material_Group_ID");
		//Material Type
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Material_Type_ID"), "ol.M_Product_ID", "M_Product", "M_Material_Type_ID");
		//Purchase Group
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Purchase_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Purchase_Group_ID");
		//Sales Group
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("M_Sales_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Sales_Group_ID");
		//	Charge
		criteria.validateAndAddReference(contractFee.get_ValueAsInt("C_Charge_ID"), "ol.C_Charge_ID");
		//	Charge Type
		criteria.validateAndAddForeignReference(contractFee.get_ValueAsInt("C_ChargeType_ID"), "ol.C_Charge_ID", "C_Charge", "C_ChargeType_ID");


	}
}
