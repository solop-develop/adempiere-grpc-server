/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — Retefuente simulada desde Orden de Compra           *
 * Adaptado de org.erpya.lve.util.OrderISLR (Localización Venezuela)                 *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 ************************************************************************************/
package com.solop.lco.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_LCO_WH_Combination;
import org.spin.model.I_WH_Withholding;
import org.adempiere.core.domains.models.X_LCO_WH_Combination;
import org.adempiere.core.domains.models.X_LCO_WH_Concept;
import org.adempiere.core.domains.models.X_LCO_WithholdingSetup;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * Retefuente estimada (IsSimulation=Y) sobre una Orden de Compra completada, para
 * prever cuánto retener al prepagar al proveedor. El documento definitivo se genera
 * desde la factura ({@link APInvoiceRenta}).
 *
 * El concepto se resuelve <b>por línea</b> (estilo LVE): línea de orden
 * (C_OrderLine.LCO_RentaConcept_ID) → producto → cargo, acumulando base por concepto.
 * Prioridades: override de cabecera (C_Order.LCO_RentaConcept_ID) → por línea →
 * fallback tercero → default del setup.
 *
 * @author Solop, www.solopsoftware.com
 */
public class OrderRenta extends AbstractWithholdingSetting implements LCOUtil {

	public OrderRenta(MWHSetting setting) {
		super(setting);
	}

	private MOrder order;
	private MBPartner businessPartner;
	private String personType;
	private BigDecimal tributeUnitAmount = Env.ZERO;
	private Map<Integer, RentaConcept> conceptsToApply;

	@Override
	public boolean isValid() {
		if (getDocument().get_Table_ID() != I_C_Order.Table_ID) {
			addLog("No es una orden (C_Order)");
			return false;
		}
		order = (MOrder) getDocument();
		businessPartner = (MBPartner) order.getC_BPartner();
		setReturnValue(I_WH_Withholding.COLUMNNAME_SourceOrder_ID, order.getC_Order_ID());
		setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, order.getAD_Org_ID());

		if (order.isSOTrx()) {
			addLog("Retefuente desde orden solo aplica a compras");
			return false;
		}
		X_LCO_WithholdingSetup setup = LCOWithholdingHelper.getSetup(getContext(), order.getAD_Org_ID(), getTransactionName());
		if (setup == null) {
			addLog("No existe Configuración de Retenciones (LCO_WithholdingSetup)");
			return false;
		}
		if (setup.isClientExcluded() || setup.isExcludeRentaWithholding()) {
			addLog("Retefuente excluida por configuración (LCO_WithholdingSetup)");
			return false;
		}
		if (businessPartner.get_ValueAsBoolean(COLUMNNAME_LCO_IsRentaExempt)) {
			addLog("El tercero está exento de Retefuente (renta)");
			return false;
		}
		if (businessPartner.get_ValueAsBoolean(COLUMNNAME_IsSelfWithholding)) {
			addLog("El tercero es autorretenedor: no se aplica retención de renta");
			return false;
		}
		String regime = businessPartner.get_ValueAsString(COLUMNNAME_LCO_TaxRegime);
		if (TAXREGIME_Simple.equals(regime) || TAXREGIME_NonContributor.equals(regime)) {
			addLog("El régimen del tercero no está sujeto a retención de renta (" + regime + ")");
			return false;
		}
		personType = businessPartner.get_ValueAsString(COLUMNNAME_LCO_PersonType);
		//	Resolver concepto(s) y base
		conceptsToApply = new LinkedHashMap<Integer, RentaConcept>();
		setConcepts();
		if (conceptsToApply.isEmpty()) {
			addLog("No se encontró Concepto de Retefuente: configúrelo en el producto, el cargo, la línea o la cabecera de la orden");
			return false;
		}
		tributeUnitAmount = LCOWithholdingHelper.getTributeUnitAmount(getContext(), order.getDateOrdered(), getTransactionName());
		if (tributeUnitAmount.signum() == 0) {
			addLog("No se encontró el valor de la UVT (LCO_TributeUnit) vigente");
			return false;
		}
		setRates();
		if (isGenerated()) {
			return false;
		}
		return conceptsToApply.values().stream().anyMatch(rc -> rc.generate);
	}

	@Override
	public String run() {
		conceptsToApply.values().stream()
			.filter(rc -> rc.generate)
			.forEach(rc -> {
				setWithholdingRate(rc.appliedRate);
				addBaseAmount(rc.subjectBase);
				addWithholdingAmount(rc.subjectBase.multiply(getWithholdingRate(true), MathContext.DECIMAL128));
				addDescription(rc.concept.getName() + " (simulación)");
				setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, order.getAD_Org_ID());
				setReturnValue(COLUMNNAME_LCO_WH_Concept_ID, rc.concept.getLCO_WH_Concept_ID());
				setReturnValue(COLUMNNAME_LCO_TributeUnitAmt, tributeUnitAmount);
				if (rc.combination != null)
					setReturnValue(COLUMNNAME_LCO_WH_Combination_ID, rc.combination.getLCO_WH_Combination_ID());
				setReturnValue(MWHWithholding.COLUMNNAME_IsSimulation, true);
				saveResult();
			});
		conceptsToApply.clear();
		return null;
	}

	/**
	 * Override de cabecera → por línea (orden → producto → cargo). <b>No</b> hay
	 * fallback por tercero ni por default del setup.
	 */
	private void setConcepts() {
		int headerConcept = order.get_ValueAsInt(COLUMNNAME_LCO_RentaConcept_ID);
		if (headerConcept > 0) {
			addConcept(headerConcept, order.getTotalLines().abs());
			return;
		}
		for (MOrderLine line : order.getLines()) {
			int conceptId = line.get_ValueAsInt(COLUMNNAME_LCO_RentaConcept_ID);
			if (conceptId == 0 && line.getM_Product_ID() != 0) {
				MProduct product = MProduct.get(getContext(), line.getM_Product_ID());
				if (product != null)
					conceptId = product.get_ValueAsInt(COLUMNNAME_LCO_RentaConcept_ID);
			}
			if (conceptId == 0 && line.getC_Charge_ID() != 0) {
				MCharge charge = MCharge.get(getContext(), line.getC_Charge_ID());
				if (charge != null)
					conceptId = charge.get_ValueAsInt(COLUMNNAME_LCO_RentaConcept_ID);
			}
			if (conceptId > 0)
				addConcept(conceptId, line.getLineNetAmt().abs());
		}
	}

	private void addConcept(int conceptId, BigDecimal base) {
		if (conceptId <= 0)
			return;
		RentaConcept rc = conceptsToApply.get(conceptId);
		if (rc == null) {
			rc = new RentaConcept(new X_LCO_WH_Concept(getContext(), conceptId, getTransactionName()));
			conceptsToApply.put(conceptId, rc);
		}
		rc.base = rc.base.add(base == null ? Env.ZERO : base);
	}

	private void setRates() {
		conceptsToApply.values().forEach(rc -> {
			X_LCO_WH_Concept concept = rc.concept;
			String conceptPersonType = concept.getLCO_PersonType();
			if (!Util.isEmpty(conceptPersonType) && !Util.isEmpty(personType)
					&& !conceptPersonType.equals(personType)) {
				addLog("El concepto " + concept.getName() + " no aplica al tipo de persona del tercero (" + personType + ")");
				return;
			}
			rc.subjectBase = rc.base;
			if (concept.getWithholdingBaseRate() != null && concept.getWithholdingBaseRate().signum() > 0) {
				rc.subjectBase = rc.base.multiply(concept.getWithholdingBaseRate().divide(Env.ONEHUNDRED, MathContext.DECIMAL128));
			}
			BigDecimal minimum = concept.getMinimumUVT() == null ? Env.ZERO
					: concept.getMinimumUVT().multiply(tributeUnitAmount);
			if (rc.subjectBase.compareTo(minimum) < 0 && !concept.isCumulativeWithholding()) {
				addLog("Base inferior a la base mínima de Retefuente en UVT para " + concept.getName()
						+ " (mínimo = " + minimum + ", base sujeta = " + rc.subjectBase + ")");
				return;
			}
			if (concept.isVariableRate()) {
				rc.combination = findCombination(concept, rc.subjectBase);
				if (rc.combination == null) {
					addLog("No se encontró el tramo de retención (LCO_WH_Combination) para " + concept.getName()
							+ " con base " + rc.subjectBase);
					return;
				}
				rc.appliedRate = rc.combination.getWithholdingRate();
			} else {
				rc.appliedRate = concept.getWithholdingRate();
			}
			if (rc.appliedRate == null || rc.appliedRate.signum() == 0) {
				addLog("La tarifa de retención de renta es cero para " + concept.getName());
				return;
			}
			rc.generate = true;
		});
	}

	private X_LCO_WH_Combination findCombination(X_LCO_WH_Concept concept, BigDecimal base) {
		List<X_LCO_WH_Combination> tramos = new Query(getContext(), I_LCO_WH_Combination.Table_Name,
					I_LCO_WH_Combination.COLUMNNAME_LCO_WH_Concept_ID + " = ?", getTransactionName())
				.setOnlyActiveRecords(true)
				.setParameters(concept.getLCO_WH_Concept_ID())
				.setOrderBy(I_LCO_WH_Combination.COLUMNNAME_SeqNo)
				.list();
		return tramos.stream()
				.sorted(Comparator.comparing(X_LCO_WH_Combination::getSeqNo))
				.filter(tramo -> {
					BigDecimal min = tramo.getMinValue().multiply(tributeUnitAmount);
					BigDecimal max = tramo.getMaxValue().multiply(tributeUnitAmount);
					boolean overMin = base.compareTo(min) >= 0;
					boolean underMax = tramo.getMaxValue().signum() == 0 || base.compareTo(max) <= 0;
					return overMin && underMax;
				})
				.findFirst()
				.orElse(null);
	}

	private boolean isGenerated() {
		if (order == null)
			return false;
		return new Query(getContext(), MWHWithholding.Table_Name,
					"SourceOrder_ID = ? AND WH_Definition_ID = ? AND WH_Setting_ID = ? "
					+ "AND Processed = 'Y' AND IsSimulation = 'Y' AND DocStatus IN (?,?)", getTransactionName())
				.setParameters(order.get_ID(), getDefinition().get_ID(), getSetting().get_ID(),
						MWHWithholding.DOCSTATUS_Completed, MWHWithholding.DOCSTATUS_Closed)
				.match();
	}

	private static class RentaConcept {
		private final X_LCO_WH_Concept concept;
		private BigDecimal base = Env.ZERO;
		private BigDecimal subjectBase = Env.ZERO;
		private BigDecimal appliedRate = Env.ZERO;
		private X_LCO_WH_Combination combination = null;
		private boolean generate = false;

		private RentaConcept(X_LCO_WH_Concept concept) {
			this.concept = concept;
		}
	}
}
