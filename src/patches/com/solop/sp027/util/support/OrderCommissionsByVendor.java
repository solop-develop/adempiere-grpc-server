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
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
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
 * Calculate Commissions for Order based on Contract
 * @author Gabriel Escalona
 */
public class OrderCommissionsByVendor implements ICommissionCalculation {

	public static OrderCommissionsByVendor newInstance() {
		return new OrderCommissionsByVendor();
	}

	@Override
	public void processCommission(Properties context, int businessPartnerId, int commissionId, int commissionRunId, String transactionName) {
		MCommissionRun commissionRun = new MCommissionRun(context, commissionRunId, transactionName);
		MCommission commission = new MCommission(context, commissionId, transactionName);
		int invoiceId = commissionRun.get_ValueAsInt("SP027_Invoice_ID");
		int orderId = 0;
		String tableName, lineTableName, dateColumnName, qtyColumnName, invoiceLineColumnName;
		if (invoiceId > 0) {
			MInvoice invoice = new MInvoice(context, invoiceId, transactionName);
			if (invoice.isSOTrx()){
				return;
			}
			if (invoice.isReversal() || (!invoice.getDocStatus().equals(MOrder.DOCSTATUS_InProgress)
				&& !invoice.getDocStatus().equals(MOrder.DOCSTATUS_Completed)
				&& !invoice.getDocStatus().equals(MOrder.DOCSTATUS_Closed ))){
				return;
			}
			orderId = invoice.getC_Order_ID();
			tableName = MInvoice.Table_Name;
			lineTableName = MInvoiceLine.Table_Name;
			dateColumnName = MInvoice.COLUMNNAME_DateInvoiced;
			qtyColumnName = MInvoiceLine.COLUMNNAME_QtyInvoiced;
			invoiceLineColumnName = "ol." + MInvoiceLine.COLUMNNAME_C_InvoiceLine_ID;
			businessPartnerId = invoice.getC_BPartner_ID();
		} else {
			orderId = commissionRun.get_ValueAsInt("SP027_Order_ID");
			if (orderId <= 0) {
				return;
			}

			MOrder order = new MOrder(context, orderId, transactionName);
			if (order.isSOTrx()){
				return;
			}
			if (!order.getDocStatus().equals(MOrder.DOCSTATUS_InProgress)
				&& !order.getDocStatus().equals(MOrder.DOCSTATUS_Completed)
				&& !order.getDocStatus().equals(MOrder.DOCSTATUS_Closed )){
				return;
			}
			tableName = MOrder.Table_Name;
			lineTableName = MOrderLine.Table_Name;
			dateColumnName = MOrder.COLUMNNAME_DateOrdered;
			qtyColumnName = MOrderLine.COLUMNNAME_QtyEntered;
			invoiceLineColumnName = "NULL";
			businessPartnerId = order.getC_BPartner_ID();
		}
		List<Integer> commissionParameterIds = null;
		String commissionTableName = "SP027_OrderCommissions";
		if (orderId > 0) {
			String whereClause = "C_Order_ID = ?";
			commissionParameterIds = new Query(context, "SP027_OrderCommissions", whereClause, transactionName)
					.setParameters(orderId)
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.getIDsAsList();
		}
		if (commissionParameterIds == null || commissionParameterIds.isEmpty()) {
			String whereClause = "C_BPartner_ID = ?";
			commissionParameterIds = new Query(context, "SP027_VendorCommissions", whereClause, transactionName)
					.setParameters(businessPartnerId)
					.setClient_ID()
					.setOnlyActiveRecords(true)
					.getIDsAsList();
			commissionTableName = "SP027_VendorCommissions";
		}
		CriteriaUtil criteria = CriteriaUtil.newInstance().withCommission(commission);
		MTable commissionParameterTable = MTable.get(context, commissionTableName);

		int finalBusinessPartnerId = businessPartnerId;
		int recordId = invoiceId > 0 ? invoiceId : orderId;
		commissionParameterIds.forEach(commissionParameterId ->{
			PO commissionParameter = commissionParameterTable.getPO(commissionParameterId, transactionName);
			BigDecimal commissionRate = (BigDecimal)commissionParameter.get_Value("SP027_CommissionRate");
			if (commissionRate == null || commissionRate.signum() == 0 ) {
				return;
			}
			criteria.clear();
			StringBuffer sql = new StringBuffer();

			//	Mandatory Columns
			if(commission.isListDetails()) {
				sql.append("SELECT o.").append(dateColumnName).append(" AS DateDoc, o.C_Currency_ID, ol.LineNetAmt AS Amount, ol.").append(qtyColumnName).append(" Quantity");
				//	Reference Columns
				sql.append(", o.DocumentNo AS Reference, COALESCE(p.Value || ' - ' || p.Name, c.Name, '') AS Info, ").append(invoiceLineColumnName).append( " AS C_InvoiceLine_ID, ol.C_OrderLine_ID");
			} else {
				sql.append("SELECT ? AS DateDoc, o.C_Currency_ID, SUM(ol.LineNetAmt) AS Amount, SUM(ol.").append(qtyColumnName).append(") Quantity");
				//	Reference Columns
				sql.append(", '' AS Reference, '' AS Info, NULL AS C_InvoiceLine_ID,NULL AS C_OrderLine_ID");
				criteria.addParameter(commissionRun.getDateDoc());
			}

			//	FROM
			sql.append(" FROM ").append(tableName).append(" o"
					+ " INNER JOIN ").append(lineTableName).append(" ol ON(ol.").append(tableName).append("_ID = o.").append(tableName).append("_ID)"
					+ " LEFT JOIN M_Product p ON(p.M_Product_ID = ol.M_Product_ID)"
					+ " LEFT JOIN C_Charge c ON(c.C_Charge_ID = ol.C_Charge_ID)");
			//	Default Where
			criteria.addWhereClause(" WHERE o."+tableName+"_ID = ? ");
			criteria.addParameter(recordId);

			//	For Currency
			//criteria.validateAndAddReference(commission.getC_Currency_ID(), "i.C_Currency_ID");
			//	Add all criteria from Commission Line
			addCriteria(commissionParameter, criteria);
			//
			sql.append(criteria.getWhereClause());
			List<Object> parameters = new ArrayList<>(criteria.getParameters());
			//	Add Group By
			if(!commission.isListDetails()) {
				//	Group By
				sql.append(" GROUP BY o.C_Currency_ID");
			}
			//	Process
			processCommission(commission, commissionParameter, commissionRun, finalBusinessPartnerId, sql.toString(), parameters, transactionName);

		});
		commission.setDateLastRun(new Timestamp(System.currentTimeMillis()));
		commission.saveEx();
	}

	/**
	 * Process Commission Line by Seller
	 * @param commission
	 * @param commissionParameter
	 * @param commissionRun
	 * @param businessPartnerId
	 * @param query
	 * @param parameters
	 * @param transactionName
	 */
	private void processCommission(MCommission commission, PO commissionParameter, MCommissionRun commissionRun, int businessPartnerId, String query, List<Object> parameters, String transactionName) {
		MCommissionAmt commissionAmt = new MCommissionAmt(commission.getCtx(), 0, transactionName);
		commissionAmt.setAD_Org_ID(commissionRun.getAD_Org_ID());
		commissionAmt.setC_CommissionRun_ID (commissionRun.getC_CommissionRun_ID());

		commissionAmt.setC_BPartner_ID(businessPartnerId);
		commissionAmt.saveEx();
		AtomicBoolean hasLines = new AtomicBoolean(false);
		DB.runResultSet(transactionName, query, parameters, resultSet -> {
			while (resultSet.next()) {
				hasLines.set(true);
				processCommissionLine(resultSet, commission, commissionParameter, commissionRun, commissionAmt);
			}
		}).onFailure(throwable -> {
			throw new AdempiereException(throwable);
		});
		if(hasLines.get()) {
			BigDecimal commissionPercentage = (BigDecimal)commissionParameter.get_Value("SP027_CommissionRate");
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
	 * @param commissionParameter
	 * @param commissionRun
	 * @param commissionAmt
	 * @throws SQLException
	 */
	private void processCommissionLine(ResultSet resultSet, MCommission commission, PO commissionParameter, MCommissionRun commissionRun, MCommissionAmt commissionAmt) throws SQLException {
		int currencyId = resultSet.getInt("C_Currency_ID");
		BigDecimal amount = Optional.ofNullable(resultSet.getBigDecimal("Amount")).orElse(Env.ZERO);
		BigDecimal quantity = Optional.ofNullable(resultSet.getBigDecimal("Quantity")).orElse(Env.ZERO);
		Timestamp date = resultSet.getTimestamp("DateDoc");
		String reference = resultSet.getString("Reference");
		String info = resultSet.getString("Info");
		int orderLineId = resultSet.getInt("C_OrderLine_ID");
		int invoiceLineId = resultSet.getInt("C_InvoiceLine_ID");
		MCommissionDetail commissionDetail = new MCommissionDetail (commissionAmt, currencyId, amount, quantity);
		commissionDetail.setLineIDs(orderLineId, invoiceLineId);
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
		calculateCommission(commissionParameter, commissionDetail, precision);
	}

	/**
	 * Calculate commission by line
	 * @param commissionParameter
	 * @param commissionDetail
	 * @param precision
	 */
	public void calculateCommission(PO commissionParameter, MCommissionDetail commissionDetail, int precision) {
		//	Calculate
		BigDecimal convertedAmt = commissionDetail.getConvertedAmt();
		BigDecimal commissionAmount = commissionDetail.getConvertedAmt();
		if (convertedAmt == null) {
			convertedAmt = Env.ZERO;
		}
		//	Commission Amount
		BigDecimal multiplier = Optional.ofNullable((BigDecimal)commissionParameter.get_Value("SP027_CommissionRate"))
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


	private void addCriteria(PO commissionParameter, CriteriaUtil criteria) {
		//	Organization
		criteria.validateAndAddReference(commissionParameter.getAD_Org_ID(), "o.AD_Org_ID");
		//	Product
		criteria.validateAndAddReference(commissionParameter.get_ValueAsInt("M_Product_ID"), "ol.M_Product_ID");
		//	Product Category
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Product_Category_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Category_ID");
		//	Product Group
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Product_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Group_ID");
		//	Product Class
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Product_Class_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Class_ID");
		//	Product Classification
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Product_Classification_ID"), "ol.M_Product_ID", "M_Product", "M_Product_Classification_ID");
		//Industry Sector
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Industry_Sector_ID"), "ol.M_Product_ID", "M_Product", "M_Industry_Sector_ID");
		//Material Group
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Material_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Material_Group_ID");
		//Material Type
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Material_Type_ID"), "ol.M_Product_ID", "M_Product", "M_Material_Type_ID");
		//Purchase Group
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Purchase_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Purchase_Group_ID");
		//Sales Group
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("M_Sales_Group_ID"), "ol.M_Product_ID", "M_Product", "M_Sales_Group_ID");
		//	Charge
		criteria.validateAndAddReference(commissionParameter.get_ValueAsInt("C_Charge_ID"), "ol.C_Charge_ID");
		//	Charge Type
		criteria.validateAndAddForeignReference(commissionParameter.get_ValueAsInt("C_ChargeType_ID"), "ol.C_Charge_ID", "C_Charge", "C_ChargeType_ID");


	}
}
