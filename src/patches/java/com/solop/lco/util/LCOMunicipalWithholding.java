/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — Impuesto Municipal (ICA) con territorialidad        *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 ************************************************************************************/
package com.solop.lco.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Properties;

import com.solop.lco.model.I_LCO_MunicipalRate;
import com.solop.lco.model.X_LCO_MunicipalConcept;
import com.solop.lco.model.X_LCO_MunicipalRate;
import org.compiere.model.MLocation;
import org.compiere.model.MOrgInfo;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Cálculo del Impuesto Municipal (ICA) de Colombia con <b>territorialidad estricta</b>.
 *
 * El ICA es municipal: la base es el neto (sin IVA) y la tarifa es la <i>tarifa por mil</i>
 * de la actividad (concepto) <b>en el municipio del agente retenedor</b>. El municipio del
 * agente se toma de la organización ({@code AD_OrgInfo → C_Location → C_City}). La tarifa
 * se resuelve buscando la fila de {@link X_LCO_MunicipalRate} cuya {@code C_City_ID}
 * coincide con la del agente. Si no existe esa fila, la actividad no se causa en la
 * jurisdicción del agente y <b>no se retiene</b> (territorialidad).
 *
 * @author Solop, www.solopsoftware.com
 */
public class LCOMunicipalWithholding implements LCOUtil {

	private LCOMunicipalWithholding() {}

	/** Indica si se debe generar la retención. */
	public boolean valid = false;
	/** Motivo (para {@code addLog}) cuando no aplica. */
	public String logMessage = "";
	/** Concepto municipal. */
	public X_LCO_MunicipalConcept concept = null;
	public int municipalConceptId = 0;
	public int municipalRateId = 0;
	/** Tarifa por mil aplicada (de la fila por ciudad, p. ej. 11,04‰). */
	public BigDecimal ratePerThousand = Env.ZERO;
	/** Tarifa como porcentaje = por mil / 10 (p. ej. 1,104%) — para {@code setWithholdingRate}. */
	public BigDecimal ratePercent = Env.ZERO;
	/** Tarifa como fracción = por mil / 1000 (p. ej. 0,01104) — para el monto. */
	public BigDecimal rate = Env.ZERO;
	/** Base sujeta (neto × tarifa de base si aplica). */
	public BigDecimal subjectBase = Env.ZERO;
	/** Monto retenido = base sujeta × tarifa. */
	public BigDecimal withholdingAmount = Env.ZERO;
	public String conceptName = "";

	/**
	 * Municipio (C_City_ID) del agente retenedor a partir de la organización.
	 * Devuelve 0 si la organización no tiene ubicación o ciudad.
	 */
	public static int getAgentCityId(Properties ctx, int adOrgId, String trxName) {
		MOrgInfo orgInfo = MOrgInfo.get(ctx, adOrgId, trxName);
		if (orgInfo == null || orgInfo.getC_Location_ID() <= 0)
			return 0;
		MLocation location = new MLocation(ctx, orgInfo.getC_Location_ID(), trxName);
		return location.getC_City_ID();
	}

	/**
	 * Evalúa el ICA para un concepto y una base dadas, con la ciudad del agente.
	 * Nunca devuelve {@code null}: si no aplica, {@link #valid} es {@code false} y
	 * {@link #logMessage} explica por qué.
	 *
	 * @param conceptId     concepto municipal (LCO_MunicipalConcept_ID)
	 * @param agentCityId   municipio del agente (C_City_ID)
	 * @param baseAmount    base neta del documento (sin IVA), en valor absoluto
	 * @param tributeUnit   valor de la UVT vigente (para la base mínima)
	 */
	public static LCOMunicipalWithholding evaluate(Properties ctx, int conceptId, int agentCityId,
			BigDecimal baseAmount, BigDecimal tributeUnit, String trxName) {
		LCOMunicipalWithholding r = new LCOMunicipalWithholding();
		if (conceptId <= 0) {
			r.logMessage = "No se encontró el Concepto de Impuesto Municipal (ICA): configúrelo en la cabecera o el tercero";
			return r;
		}
		r.municipalConceptId = conceptId;
		r.concept = new X_LCO_MunicipalConcept(ctx, conceptId, trxName);
		if (r.concept.getLCO_MunicipalConcept_ID() != conceptId) {
			r.logMessage = "El Concepto de Impuesto Municipal no existe (id " + conceptId + ")";
			return r;
		}
		r.conceptName = r.concept.getName();
		if (agentCityId <= 0) {
			r.logMessage = "La organización no tiene municipio configurado (AD_OrgInfo → Ubicación → Ciudad): no se puede aplicar ReteICA";
			return r;
		}
		//	Territorialidad: tarifa por la ciudad del agente
		X_LCO_MunicipalRate rateRow = new Query(ctx, I_LCO_MunicipalRate.Table_Name,
					I_LCO_MunicipalRate.COLUMNNAME_LCO_MunicipalConcept_ID + " = ? AND "
					+ I_LCO_MunicipalRate.COLUMNNAME_C_City_ID + " = ?", trxName)
				.setOnlyActiveRecords(true)
				.setParameters(conceptId, agentCityId)
				.setOrderBy(I_LCO_MunicipalRate.COLUMNNAME_SeqNo)
				.first();
		if (rateRow == null) {
			r.logMessage = "La actividad (" + r.conceptName + ") no tiene tarifa para el municipio del agente: "
					+ "no se causa ReteICA en esta jurisdicción (territorialidad)";
			return r;
		}
		r.municipalRateId = rateRow.getLCO_MunicipalRate_ID();
		r.ratePerThousand = nz(rateRow.getRatePerThousand());
		if (r.ratePerThousand.signum() == 0) {
			r.logMessage = "La tarifa por mil de ReteICA es cero para " + r.conceptName + " en el municipio del agente";
			return r;
		}
		r.ratePercent = r.ratePerThousand.divide(BigDecimal.TEN, MathContext.DECIMAL128);                  // ‰ → %
		r.rate = r.ratePerThousand.divide(Env.ONEHUNDRED.multiply(BigDecimal.TEN), MathContext.DECIMAL128); // ‰ → fracción
		//	Base sujeta (tarifa de base, p. ej. AIU)
		BigDecimal base = baseAmount == null ? Env.ZERO : baseAmount.abs();
		BigDecimal baseRate = nz(r.concept.getWithholdingBaseRate());
		r.subjectBase = baseRate.signum() > 0
				? base.multiply(baseRate.divide(Env.ONEHUNDRED, MathContext.DECIMAL128))
				: base;
		//	Base mínima en UVT
		BigDecimal minimum = nz(r.concept.getMinimumUVT()).multiply(tributeUnit == null ? Env.ZERO : tributeUnit);
		if (minimum.signum() > 0 && r.subjectBase.compareTo(minimum) < 0 && !r.concept.isCumulativeWithholding()) {
			r.logMessage = "Base inferior a la base mínima de ReteICA en UVT para " + r.conceptName
					+ " (mínimo = " + minimum + ", base sujeta = " + r.subjectBase + ")";
			return r;
		}
		r.withholdingAmount = r.subjectBase.multiply(r.rate, MathContext.DECIMAL128);
		r.valid = true;
		return r;
	}

	private static BigDecimal nz(BigDecimal value) {
		return value == null ? Env.ZERO : value;
	}
}
