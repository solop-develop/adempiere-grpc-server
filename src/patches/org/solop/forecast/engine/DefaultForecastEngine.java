/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2025 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/
package org.solop.forecast.engine;

import org.adempiere.core.domains.models.I_M_ForecastComparison;
import org.compiere.model.MClientInfo;
import org.compiere.model.MForecastComparison;
import org.compiere.model.MForecastFact;
import org.compiere.model.MForecastKPISnapshot;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Default Forecast Engine Implementation
 * Provides standard calculation logic for forecast comparison metrics
 * and KPI snapshot aggregation.
 * @author Gabriel Escalona
 */
public class DefaultForecastEngine implements IForecastEngine {

	/** Logger */
	private static final CLogger log = CLogger.getCLogger(DefaultForecastEngine.class);
	/** Scale for percentage calculations */
	private static final int PERCENTAGE_SCALE = 2;
	/** Alert threshold for MAPE (default 25%) */
	private static final BigDecimal ALERT_THRESHOLD = new BigDecimal("25");

	/** 12 product grouping columns present on M_ForecastComparison */
	private static final String[] PRODUCT_GROUPING_COLUMNS = {
		I_M_ForecastComparison.COLUMNNAME_M_Brand_ID,
		I_M_ForecastComparison.COLUMNNAME_M_FreightCategory_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Industry_Sector_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Material_Group_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Material_Type_ID,
		I_M_ForecastComparison.COLUMNNAME_M_PartType_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Product_Category_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Product_Class_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Product_Classification_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Product_Group_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Purchase_Group_ID,
		I_M_ForecastComparison.COLUMNNAME_M_Sales_Group_ID,
	};

	@Override
	public void forecastRun(Properties ctx, int tableId, int recordId, String trxName) {
		MClientInfo clientInfo = MClientInfo.get(ctx);
		String comparisonSource = clientInfo.getComparisonSource();
		String forecastLevel = clientInfo.getForecastLevel();
		List<String> levels = computeLevelsForDocument(tableId, comparisonSource, forecastLevel);
		if (levels.isEmpty()) {
			return;
		}

		if (MOrder.Table_ID == tableId) {
			MOrder order = new MOrder(ctx, recordId, trxName);
			if (!order.isSOTrx()) {
				return;
			}
			BigDecimal multiplier = Env.ONE; //TODO: Should validate reverse
			for (MOrderLine line : order.getLines()) {
				if (line.getM_Product_ID() <= 0) {
					continue;
				}
				processDocumentLine(ctx, MOrderLine.Table_ID, line.getC_OrderLine_ID(),
						line.getM_Product_ID(), order.getSalesRep_ID(), order.getAD_Org_ID(),
						line.getQtyOrdered(), line.getLineNetAmt(), multiplier,
						order.getDateOrdered(), levels, trxName);
			}
		} else if (MInvoice.Table_ID == tableId) {
			MInvoice invoice = new MInvoice(ctx, recordId, trxName);
			if (!invoice.isSOTrx()) {
				return;
			}
			BigDecimal multiplier = invoice.isReversal() ? Env.ONE.negate() : Env.ONE;
			for (MInvoiceLine line : invoice.getLines()) {
				if (line.getM_Product_ID() <= 0) {
					continue;
				}
				processDocumentLine(ctx, MInvoiceLine.Table_ID, line.getC_InvoiceLine_ID(),
						line.getM_Product_ID(), invoice.getSalesRep_ID(), invoice.getAD_Org_ID(),
						line.getQtyInvoiced(), line.getLineNetAmt(), multiplier,
						invoice.getDateInvoiced(), levels, trxName);
			}
		} else if (MInOut.Table_ID == tableId) {
			MInOut inOut = new MInOut(ctx, recordId, trxName);
			if (!inOut.isSOTrx()) {
				return;
			}
			BigDecimal multiplier = inOut.isReversal() ? Env.ONE.negate() : Env.ONE;
			for (MInOutLine line : inOut.getLines()) {
				if (line.getM_Product_ID() <= 0) {
					continue;
				}
				BigDecimal lineNetAmt = Env.ZERO;
				processDocumentLine(ctx, MInOutLine.Table_ID, line.getM_InOutLine_ID(),
						line.getM_Product_ID(), inOut.getSalesRep_ID(), inOut.getAD_Org_ID(),
						line.getMovementQty(), lineNetAmt, multiplier,
						inOut.getMovementDate(), levels, trxName);
			}
		}
	}

	/**
	 * Process a single document line across all applicable forecast levels.
	 */
	private void processDocumentLine(Properties ctx, int lineTableId, int lineRecordId,
			int productId, int salesRepId, int orgId,
			BigDecimal qty, BigDecimal amount, BigDecimal multiplier,
			Timestamp dateDoc, List<String> levels, String trxName) {
		int periodId = resolvePeriodId(ctx, dateDoc, trxName);
		if (periodId <= 0) {
			log.warning("No period found for date: " + dateDoc);
			return;
		}

		for (String level : levels) {
			List<MForecastComparison> comparisons = resolveComparisons(ctx, productId, salesRepId, orgId, level, periodId, trxName);
			if (comparisons.isEmpty()) {
				log.fine("No comparisons found for product=" + productId
					+ " level=" + level + " period=" + periodId + ". Creating new comparison.");
				comparisons = createComparison(ctx, productId, salesRepId, orgId, level, periodId, trxName);
			}

			for (MForecastComparison comparison : comparisons) {
				// Accumulate actuals (multiplier-aware)
				BigDecimal qtyActual = comparison.getQtyActual().add(qty.multiply(multiplier));
				BigDecimal amtActual = comparison.getAmtActual().add(amount.multiply(multiplier));
				comparison.setQtyActual(qtyActual);
				comparison.setAmtActual(amtActual);

				// Create thin bridge fact: line reference + comparison link
				MForecastFact fact = new MForecastFact(ctx, 0, trxName);
				fact.setAD_Table_ID(lineTableId);
				fact.setRecord_ID(lineRecordId);
				fact.setAD_Org_ID(orgId);
				fact.setDateDoc(dateDoc);
				fact.setM_ForecastComparison_ID(comparison.getM_ForecastComparison_ID());

				// Calculate comparison metrics
				calculateComparison(ctx, comparison, trxName);

				// Save
				comparison.saveEx();
				fact.saveEx();
			}
		}
	}

	/**
	 * Compute applicable forecast levels for a given document type.
	 * FI is ALWAYS fed by Invoices. CL and OP depend on ComparisonSource matching the document.
	 */
	private List<String> computeLevelsForDocument(int tableId, String comparisonSource, String forecastLevel) {
		List<String> levels = new ArrayList<>();
		boolean isInvoice = MInvoice.Table_ID == tableId;
		boolean documentMatchesSource = documentMatchesComparisonSource(tableId, comparisonSource);

		// FI: only if document is Invoice AND (forecastLevel=FI or EV)
		if (isInvoice && (MClientInfo.FORECASTLEVEL_Financial.equals(forecastLevel)
				|| MClientInfo.FORECASTLEVEL_Everything.equals(forecastLevel))) {
			levels.add(MClientInfo.FORECASTLEVEL_Financial);
		}

		// CL: if (forecastLevel=CL or EV) AND document matches ComparisonSource
		if (documentMatchesSource && (MClientInfo.FORECASTLEVEL_Classification.equals(forecastLevel)
				|| MClientInfo.FORECASTLEVEL_Everything.equals(forecastLevel))) {
			levels.add(MClientInfo.FORECASTLEVEL_Classification);
		}

		// OP: if (forecastLevel=OP or EV) AND document matches ComparisonSource
		if (documentMatchesSource && (MClientInfo.FORECASTLEVEL_Operational.equals(forecastLevel)
				|| MClientInfo.FORECASTLEVEL_Everything.equals(forecastLevel))) {
			levels.add(MClientInfo.FORECASTLEVEL_Operational);
		}

		return levels;
	}

	/**
	 * Check if the document type matches the configured ComparisonSource.
	 * OR -> Order, SH -> Shipment, IN -> Invoice
	 */
	private boolean documentMatchesComparisonSource(int tableId, String comparisonSource) {
		if (Util.isEmpty(comparisonSource, true)) {
			return false;
		}
		if (MClientInfo.COMPARISONSOURCE_Order.equals(comparisonSource)) {
			return MOrder.Table_ID == tableId;
		} else if (MClientInfo.COMPARISONSOURCE_Shipment.equals(comparisonSource)) {
			return MInOut.Table_ID == tableId;
		} else if (MClientInfo.COMPARISONSOURCE_Invoice.equals(comparisonSource)) {
			return MInvoice.Table_ID == tableId;
		}
		return false;
	}

	/**
	 * Resolve the C_Period_ID for a given date.
	 */
	private int resolvePeriodId(Properties ctx, Timestamp date, String trxName) {
		if (date == null) {
			return 0;
		}
		MPeriod period = MPeriod.get(ctx, date, 0, trxName);
		return period.get_ID();
	}

	/**
	 * Resolve the 12 product grouping values from a product.
	 * Returns only non-null, positive values.
	 */
	private Map<String, Integer> resolveProductGroupings(Properties ctx, int productId) {
		Map<String, Integer> groupings = new HashMap<>();
		if (productId <= 0) {
			return groupings;
		}
		PO product = MProduct.get(ctx, productId);
		if (product == null) {
			return groupings;
		}
		for (String col : PRODUCT_GROUPING_COLUMNS) {
			Integer val = (Integer) product.get_Value(col);
			if (val != null && val > 0) {
				groupings.put(col, val);
			}
		}
		return groupings;
	}

	/**
	 * Create a standalone ForecastComparison when none exists for the document line.
	 * The comparison has zero forecast amounts so that actual sales without a forecast
	 * are visible in reports as forecasting errors.
	 */
	private List<MForecastComparison> createComparison(Properties ctx, int productId,
			int salesRepId, int orgId, String level, int periodId, String trxName) {
		MForecastComparison comparison = new MForecastComparison(ctx, 0, trxName);
		comparison.setForecastLevel(level);
		comparison.setC_Period_ID(periodId);
		comparison.setSalesRep_ID(salesRepId);
		comparison.setAD_Org_ID(orgId);
		comparison.setQtyForecast(Env.ZERO);
		comparison.setAmtForecast(Env.ZERO);
		comparison.setQtyActual(Env.ZERO);
		comparison.setAmtActual(Env.ZERO);

		if (MClientInfo.FORECASTLEVEL_Classification.equals(level)
				|| MClientInfo.FORECASTLEVEL_Operational.equals(level)) {
			Map<String, Integer> groupings = resolveProductGroupings(ctx, productId);
			for (Map.Entry<String, Integer> entry : groupings.entrySet()) {
				comparison.set_ValueOfColumn(entry.getKey(), entry.getValue());
			}
		}

		if (MClientInfo.FORECASTLEVEL_Operational.equals(level)) {
			comparison.setM_Product_ID(productId);
		}

		comparison.saveEx();
		List<MForecastComparison> result = new ArrayList<>();
		result.add(comparison);
		return result;
	}

	/**
	 * Find all valid MForecastComparison records for a product/level/period.
	 * A product can belong to multiple Forecasts, so we return ALL matching comparisons.
	 * FI matches by SalesRep_ID + C_Period_ID.
	 * CL matches by C_Period_ID + all 12 product grouping columns (NULL on comparison = wildcard).
	 * OP matches by C_Period_ID + M_Product_ID only (product is the most granular level).
	 */
	private List<MForecastComparison> resolveComparisons(Properties ctx, int productId,
			int salesRepId, int orgId, String level, int periodId, String trxName) {
		StringBuilder whereClause = new StringBuilder("ForecastLevel=? AND C_Period_ID=? AND AD_Org_ID = ? AND IsActive='Y'");
		List<Object> params = new ArrayList<>();
		params.add(level);
		params.add(periodId);
		params.add(orgId);

		if (MClientInfo.FORECASTLEVEL_Financial.equals(level)) {
			// FI: match by SalesRep_ID
			whereClause.append(" AND SalesRep_ID=?");
			params.add(salesRepId);
		} else if (MClientInfo.FORECASTLEVEL_Operational.equals(level)) {
			// OP: match by M_Product_ID only — product is the most granular dimension
			whereClause.append(" AND M_Product_ID=?");
			params.add(productId);
		} else {
			// CL: match by all 12 product grouping columns
			Map<String, Integer> groupings = resolveProductGroupings(ctx, productId);
			for (String col : PRODUCT_GROUPING_COLUMNS) {
				Integer val = groupings.get(col);
				if (val != null) {
					whereClause.append(" AND (").append(col).append(" IS NULL OR ").append(col).append("=?)");
					params.add(val);
				} else {
					whereClause.append(" AND ").append(col).append(" IS NULL");
				}
			}
		}

		return new Query(ctx, I_M_ForecastComparison.Table_Name,
				whereClause.toString(), trxName)
			.setParameters(params.toArray())
			.list();
	}

	@Override
	public void calculateComparison(Properties ctx, MForecastComparison comparison, String trxName) {
		BigDecimal qtyForecast = comparison.getQtyForecast() != null ? comparison.getQtyForecast() : Env.ZERO;
		BigDecimal qtyActual = comparison.getQtyActual() != null ? comparison.getQtyActual() : Env.ZERO;
		BigDecimal amtForecast = comparison.getAmtForecast() != null ? comparison.getAmtForecast() : Env.ZERO;
		BigDecimal amtActual = comparison.getAmtActual() != null ? comparison.getAmtActual() : Env.ZERO;

		// AmtVariance = Actual - Forecast
		BigDecimal amtVariance = amtActual.subtract(amtForecast);
		comparison.setAmtVariance(amtVariance);

		// QtyVariance = Actual - Forecast
		BigDecimal qtyVariance = qtyActual.subtract(qtyForecast);
		comparison.setQtyVariance(qtyVariance);

		// VariancePct = (Actual - Forecast) / Forecast * 100  (if Forecast != 0)
		if (amtForecast.signum() != 0) {
			BigDecimal variancePct = amtVariance
					.multiply(Env.ONEHUNDRED)
					.divide(amtForecast, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
			comparison.setVariancePct(variancePct);
		} else {
			comparison.setVariancePct(Env.ZERO);
		}

		// Bias = Actual - Forecast (same as variance for single line)
		comparison.setBias(amtVariance);

		// MAD = |Actual - Forecast| (Mean Absolute Deviation for single line)
		BigDecimal mad = amtVariance.abs();
		comparison.setMAD(mad);
		BigDecimal mape = Env.ZERO;
		// MAPE = |Actual - Forecast| / |Forecast| * 100  (if Forecast != 0)
		if (amtActual.signum() != 0) {
			mape = amtVariance.abs()
				.divide(amtActual.abs(), 4, RoundingMode.HALF_UP)
				.multiply(Env.ONEHUNDRED);
			comparison.setMAPE(mape);
		} else if (amtForecast.signum() != 0) {
			mape = Env.ONEHUNDRED;
		}

		// ForecastAccuracy = max(0, 100 - MAPE)
		BigDecimal forecastAccuracy = Env.ONEHUNDRED.subtract(mape).max(Env.ZERO);
		comparison.setForecastAccuracy(forecastAccuracy);

		// DateCalculated
		comparison.setDateCalculated(new Timestamp(System.currentTimeMillis()));
	}

	//TODO: Claude suggestion for Calculate KPISnapshot
	@Override
	public void calculateKPISnapshot(Properties ctx, MForecastKPISnapshot snapshot, String trxName) {
		int forecastId = snapshot.getM_Forecast_ID();
		int periodId = snapshot.getC_Period_ID();

		if (forecastId <= 0) {
			log.warning("No M_Forecast_ID set on KPI Snapshot");
			return;
		}

		// Query aggregated metrics from M_ForecastComparison for this forecast/period/dimension
		StringBuilder whereClause = new StringBuilder("M_Forecast_ID=? AND IsActive='Y'");
		Object[] params;
		if (periodId > 0) {
			whereClause.append(" AND C_Period_ID=?");
			params = new Object[]{forecastId, periodId};
		} else {
			params = new Object[]{forecastId};
		}

		// Get comparison records for aggregation
		List<MForecastComparison> comparisons = new Query(ctx, I_M_ForecastComparison.Table_Name, whereClause.toString(), trxName)
				.setParameters(params)
				.setOnlyActiveRecords(true)
				.list();

		if (comparisons.isEmpty()) {
			snapshot.setForecastAccuracy(Env.ZERO);
			snapshot.setMAPE(Env.ZERO);
			snapshot.setWeightedMAPE(Env.ZERO);
			snapshot.setBias(Env.ZERO);
			snapshot.setSKUsInAlert(0);
			snapshot.setDateCalculated(new Timestamp(System.currentTimeMillis()));
			return;
		}

		BigDecimal totalMape = Env.ZERO;
		BigDecimal totalBias = Env.ZERO;
		BigDecimal totalWeightedMapeNumerator = Env.ZERO;
		BigDecimal totalAmtForecastAbs = Env.ZERO;
		int skusInAlert = 0;
		int count = comparisons.size();

		for (MForecastComparison comp : comparisons) {
			BigDecimal compMape = comp.getMAPE() != null ? comp.getMAPE() : Env.ZERO;
			BigDecimal compBias = comp.getBias() != null ? comp.getBias() : Env.ZERO;
			BigDecimal compAmtForecast = comp.getAmtForecast() != null ? comp.getAmtForecast() : Env.ZERO;
			BigDecimal compAmtVariance = comp.getAmtVariance() != null ? comp.getAmtVariance() : Env.ZERO;

			totalMape = totalMape.add(compMape);
			totalBias = totalBias.add(compBias);

			// Weighted MAPE: weight by |AmtForecast|
			BigDecimal absAmtForecast = compAmtForecast.abs();
			totalAmtForecastAbs = totalAmtForecastAbs.add(absAmtForecast);
			totalWeightedMapeNumerator = totalWeightedMapeNumerator.add(compAmtVariance.abs());

			// SKUs in alert: MAPE > threshold
			if (compMape.compareTo(ALERT_THRESHOLD) > 0) {
				skusInAlert++;
			}
		}

		// MAPE = avg(MAPE)
		BigDecimal avgMape = totalMape.divide(BigDecimal.valueOf(count), PERCENTAGE_SCALE, RoundingMode.HALF_UP);
		snapshot.setMAPE(avgMape);

		// WeightedMAPE = sum(|Actual - Forecast|) / sum(|Forecast|) * 100
		if (totalAmtForecastAbs.signum() != 0) {
			BigDecimal weightedMape = totalWeightedMapeNumerator
					.multiply(Env.ONEHUNDRED)
					.divide(totalAmtForecastAbs, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
			snapshot.setWeightedMAPE(weightedMape);
		} else {
			snapshot.setWeightedMAPE(Env.ZERO);
		}

		// ForecastAccuracy = max(0, 100 - WeightedMAPE)
		BigDecimal weightedMape = snapshot.getWeightedMAPE() != null ? snapshot.getWeightedMAPE() : Env.ZERO;
		BigDecimal forecastAccuracy = Env.ONEHUNDRED.subtract(weightedMape).max(Env.ZERO);
		snapshot.setForecastAccuracy(forecastAccuracy);

		// Bias = avg(Bias)
		BigDecimal avgBias = totalBias.divide(BigDecimal.valueOf(count), PERCENTAGE_SCALE, RoundingMode.HALF_UP);
		snapshot.setBias(avgBias);

		// SKUsInAlert
		snapshot.setSKUsInAlert(skusInAlert);

		// DateCalculated
		snapshot.setDateCalculated(new Timestamp(System.currentTimeMillis()));
	}
}
