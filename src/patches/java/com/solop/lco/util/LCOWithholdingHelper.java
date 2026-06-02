/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — Retenciones                                         *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 ************************************************************************************/
package com.solop.lco.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.core.domains.models.I_LCO_TributeUnit;
import org.adempiere.core.domains.models.I_LCO_WithholdingSetup;
import org.adempiere.core.domains.models.X_LCO_TributeUnit;
import org.adempiere.core.domains.models.X_LCO_WithholdingSetup;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Utilidades estáticas compartidas por los handlers de retención de Colombia.
 * Mantiene los handlers DRY sin introducir herencia (estilo LVE: handlers
 * independientes por tributo + helpers estáticos).
 *
 * @author Solop, www.solopsoftware.com
 */
public class LCOWithholdingHelper {

	/**
	 * Valor de la UVT vigente a una fecha (LCO_TributeUnit con ValidFrom &lt;= fecha,
	 * el más reciente). Devuelve {@link Env#ZERO} si no hay ninguno.
	 */
	public static BigDecimal getTributeUnitAmount(Properties ctx, Timestamp date, String trxName) {
		X_LCO_TributeUnit uvt = new Query(ctx, I_LCO_TributeUnit.Table_Name,
					I_LCO_TributeUnit.COLUMNNAME_ValidFrom + " <= ?", trxName)
				.setOnlyActiveRecords(true)
				.setParameters(date)
				.setOrderBy(I_LCO_TributeUnit.COLUMNNAME_ValidFrom + " DESC")
				.first();
		if (uvt == null)
			return Env.ZERO;
		return uvt.getAmount();
	}

	/**
	 * Configuración maestra de retenciones (LCO_WithholdingSetup) para la organización,
	 * con caída a la organización 0. Devuelve {@code null} si no existe.
	 */
	public static X_LCO_WithholdingSetup getSetup(Properties ctx, int adOrgId, String trxName) {
		return new Query(ctx, I_LCO_WithholdingSetup.Table_Name,
					"AD_Org_ID IN (?, 0)", trxName)
				.setOnlyActiveRecords(true)
				.setParameters(adOrgId)
				.setOrderBy("AD_Org_ID DESC")
				.first();
	}
}
