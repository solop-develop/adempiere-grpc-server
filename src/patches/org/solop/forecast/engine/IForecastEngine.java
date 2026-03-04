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

import org.compiere.model.MForecastComparison;
import org.compiere.model.MForecastKPISnapshot;

import java.util.Properties;

/**
 * Forecast Engine Interface
 * Defines the contract for calculating forecast comparison metrics
 * and KPI snapshots. Implementations can be registered per client
 * via the ForecastEngine column in AD_ClientInfo.
 * @author Gabriel Escalona
 */
public interface IForecastEngine {


	void forecastRun(Properties ctx, int tableId, int recordId, String trxName);
	/**
	 * Calculate comparison metrics for a forecast comparison line.
	 * Sets AmtVariance, QtyVariance, VariancePct, Bias, ForecastAccuracy, MAD, MAPE
	 * on the given comparison record.
	 * @param ctx context
	 * @param comparison the forecast comparison record to calculate
	 * @param trxName transaction name
	 */
	void calculateComparison(Properties ctx, MForecastComparison comparison, String trxName);

	/**
	 * Calculate aggregated KPI metrics for a forecast KPI snapshot.
	 * Sets ForecastAccuracy, MAPE, WeightedMAPE, Bias, SKUsInAlert
	 * on the given snapshot record.
	 * @param ctx context
	 * @param snapshot the KPI snapshot record to calculate
	 * @param trxName transaction name
	 */
	void calculateKPISnapshot(Properties ctx, MForecastKPISnapshot snapshot, String trxName);
}
