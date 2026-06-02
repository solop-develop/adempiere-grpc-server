/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Localización Colombia (LCO) — Retención en la Fuente por Renta (Retefuente)       *
 * Adaptado de org.erpya.lve.util.APInvoiceISLR (Localización Venezuela)             *
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

import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_LCO_WH_Combination;
import org.spin.model.I_WH_Withholding;
import org.adempiere.core.domains.models.X_LCO_WH_Combination;
import org.adempiere.core.domains.models.X_LCO_WH_Concept;
import org.adempiere.core.domains.models.X_LCO_WithholdingSetup;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * Retención en la fuente por Renta (Retefuente) para Colombia.
 *
 * Aplica sobre facturas de compra (AP) y, como autorretención, sobre facturas de
 * venta (AR) cuando la organización es autorretenedora (AD_OrgInfo.IsSelfWithholding).
 *
 * El concepto se resuelve <b>por línea</b> (estilo LVE): producto
 * (M_Product.LCO_RentaConcept_ID) → cargo (C_Charge.LCO_RentaConcept_ID), acumulando
 * la base por concepto, de modo que una factura con bienes/servicios/honorarios genera
 * una retención por cada concepto. Prioridades:
 * <ol>
 *   <li>Override de cabecera: C_Invoice.LCO_RentaConcept_ID → un concepto, base = total.</li>
 *   <li>Por línea: producto → cargo, acumulando LineNetAmt por concepto.</li>
 *   <li>Fallback: tercero (C_BPartner) → default del setup, base = total de la factura.</li>
 * </ol>
 * La tarifa puede ser fija (LCO_WH_Concept) o variable por tramos (LCO_WH_Combination).
 * Respeta la base mínima en UVT y el porcentaje de base sujeta (WithholdingBaseRate).
 *
 * @author Solop, www.solopsoftware.com
 */
public class APInvoiceRenta extends AbstractWithholdingSetting implements LCOUtil {

	public APInvoiceRenta(MWHSetting setting) {
		super(setting);
	}

	private MInvoice invoice;
	private MBPartner businessPartner;
	private String personType;
	private boolean isManual = false;
	private BigDecimal tributeUnitAmount = Env.ZERO;
	/** Conceptos a aplicar (clave = LCO_WH_Concept_ID), con su base e información de cálculo. */
	private Map<Integer, RentaConcept> conceptsToApply;

	@Override
	public boolean isValid() {
		if (getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			addLog("No es una factura (C_Invoice)");
			return false;
		}
		invoice = (MInvoice) getDocument();
		businessPartner = (MBPartner) invoice.getC_BPartner();
		setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoice.getC_Invoice_ID());
		setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, invoice.getAD_Org_ID());

		if (invoice.isReversal()) {
			addLog("La factura está reversada");
			return false;
		}
		MDocType documentType = MDocType.get(getContext(), invoice.getC_DocTypeTarget_ID());
		if (documentType == null
				|| (!documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice)
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APCreditMemo)
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARInvoice)
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo))) {
			addLog("Se requiere una factura o nota de crédito (compra o venta)");
			return false;
		}
		//	Configuración de retenciones
		X_LCO_WithholdingSetup setup = LCOWithholdingHelper.getSetup(getContext(), invoice.getAD_Org_ID(), getTransactionName());
		if (setup == null) {
			addLog("No existe Configuración de Retenciones (LCO_WithholdingSetup)");
			return false;
		}
		if (setup.isClientExcluded() || setup.isExcludeRentaWithholding()) {
			addLog("Retefuente excluida por configuración (LCO_WithholdingSetup)");
			return false;
		}

		//	Resolver el/los concepto(s) y su base
		conceptsToApply = new LinkedHashMap<Integer, RentaConcept>();
		if (invoice.isSOTrx()) {
			//	Autorretención en ventas: la organización debe ser autorretenedora
			isManual = true;
			MOrgInfo orgInfo = MOrgInfo.get(getContext(), invoice.getAD_Org_ID(), getTransactionName());
			if (orgInfo == null || !orgInfo.get_ValueAsBoolean(COLUMNNAME_IsSelfWithholding)) {
				addLog("La organización no es autorretenedora de renta");
				return false;
			}
			personType = PERSONTYPE_LegalPerson;
			addConcept(orgInfo.get_ValueAsInt(COLUMNNAME_LCO_SelfRentaConcept_ID), invoice.getTotalLines().abs());
		} else {
			isManual = false;
			//	Exención del tercero: no se calcula Retefuente
			if (businessPartner.get_ValueAsBoolean(COLUMNNAME_LCO_IsRentaExempt)) {
				addLog("El tercero está exento de Retefuente (renta)");
				return false;
			}
			//	No se retiene a autorretenedores
			if (businessPartner.get_ValueAsBoolean(COLUMNNAME_IsSelfWithholding)) {
				addLog("El tercero es autorretenedor: no se aplica retención de renta");
				return false;
			}
			//	Régimen simple / no contribuyente: no sujetos a retefuente por renta
			String regime = businessPartner.get_ValueAsString(COLUMNNAME_LCO_TaxRegime);
			if (TAXREGIME_Simple.equals(regime) || TAXREGIME_NonContributor.equals(regime)) {
				addLog("El régimen del tercero no está sujeto a retención de renta (" + regime + ")");
				return false;
			}
			personType = businessPartner.get_ValueAsString(COLUMNNAME_LCO_PersonType);
			setConcepts();
		}
		if (conceptsToApply.isEmpty()) {
			addLog("No se encontró Concepto de Retefuente: configúrelo en el producto, el cargo o la cabecera del documento");
			return false;
		}
		//	UVT vigente
		tributeUnitAmount = LCOWithholdingHelper.getTributeUnitAmount(getContext(), invoice.getDateInvoiced(), getTransactionName());
		if (tributeUnitAmount.signum() == 0) {
			addLog("No se encontró el valor de la UVT (LCO_TributeUnit) vigente");
			return false;
		}
		//	Evaluar tarifa/mínimo por cada concepto; registra log de los que no califican
		setRates();
		//	Anti-duplicado
		if (isGenerated()) {
			return false;
		}
		//	Válido si al menos un concepto genera retención
		return conceptsToApply.values().stream().anyMatch(rc -> rc.generate);
	}

	@Override
	public String run() {
		conceptsToApply.values().stream()
			.filter(rc -> rc.generate)
			.forEach(rc -> {
				setWithholdingRate(rc.appliedRate);
				addBaseAmount(rc.subjectBase);
				if (rc.valid)
					addWithholdingAmount(rc.subjectBase.multiply(getWithholdingRate(true), MathContext.DECIMAL128));
				else
					addWithholdingAmount(Env.ZERO);
				addDescription(rc.concept.getName());
				setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, invoice.getAD_Org_ID());
				setReturnValue(COLUMNNAME_LCO_WH_Concept_ID, rc.concept.getLCO_WH_Concept_ID());
				setReturnValue(COLUMNNAME_LCO_TributeUnitAmt, tributeUnitAmount);
				if (rc.combination != null)
					setReturnValue(COLUMNNAME_LCO_WH_Combination_ID, rc.combination.getLCO_WH_Combination_ID());
				setReturnValue(MWHWithholding.COLUMNNAME_IsManual, isManual);
				setReturnValue(MWHWithholding.COLUMNNAME_IsSimulation, !rc.valid);
				saveResult();
			});
		conceptsToApply.clear();
		return null;
	}

	/**
	 * Construye el mapa concepto→base (estilo LVE): override de cabecera, si no por
	 * línea (producto→cargo). <b>No</b> hay fallback por tercero ni por default del
	 * setup: si producto/cargo/cabecera no definen concepto, no se retiene.
	 */
	private void setConcepts() {
		//	1) Override de cabecera
		int headerConcept = invoice.get_ValueAsInt(COLUMNNAME_LCO_RentaConcept_ID);
		if (headerConcept > 0) {
			addConcept(headerConcept, invoice.getTotalLines().abs());
			return;
		}
		//	2) Por línea: producto → cargo
		for (MInvoiceLine line : invoice.getLines()) {
			int conceptId = 0;
			if (line.getM_Product_ID() != 0) {
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

	/** Acumula base en el concepto indicado (suma si ya existía). */
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

	/**
	 * Evalúa para cada concepto: tipo de persona, base sujeta, base mínima en UVT y
	 * tarifa (fija o por tramos). Marca {@code generate} en los que aplican y registra
	 * en el log (español) los que no.
	 */
	private void setRates() {
		conceptsToApply.values().forEach(rc -> {
			X_LCO_WH_Concept concept = rc.concept;
			//	Acotación por tipo de persona del concepto
			String conceptPersonType = concept.getLCO_PersonType();
			if (!Util.isEmpty(conceptPersonType) && !Util.isEmpty(personType)
					&& !conceptPersonType.equals(personType)) {
				addLog("El concepto " + concept.getName() + " no aplica al tipo de persona del tercero (" + personType + ")");
				return;
			}
			//	Base sujeta = base * WithholdingBaseRate%
			rc.subjectBase = rc.base;
			if (concept.getWithholdingBaseRate() != null && concept.getWithholdingBaseRate().signum() > 0) {
				rc.subjectBase = rc.base.multiply(concept.getWithholdingBaseRate().divide(Env.ONEHUNDRED, MathContext.DECIMAL128));
			}
			//	Base mínima en UVT
			BigDecimal minimum = concept.getMinimumUVT() == null ? Env.ZERO
					: concept.getMinimumUVT().multiply(tributeUnitAmount);
			if (rc.subjectBase.compareTo(minimum) < 0) {
				if (concept.isCumulativeWithholding()) {
					rc.valid = false;	//	acumulativa → simulación
				} else {
					addLog("Base inferior a la base mínima de Retefuente en UVT para " + concept.getName()
							+ " (mínimo = " + minimum + ", base sujeta = " + rc.subjectBase + ")");
					return;
				}
			}
			//	Tarifa: variable por tramos o fija
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

	/** Busca el tramo cuyo rango [MinValue*UVT, MaxValue*UVT] contiene la base sujeta. */
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

	/** Anti-duplicado: ¿ya existe retención definitiva para esta factura/definición/setting? */
	private boolean isGenerated() {
		if (invoice == null)
			return false;
		return new Query(getContext(), MWHWithholding.Table_Name,
					"SourceInvoice_ID = ? AND WH_Definition_ID = ? AND WH_Setting_ID = ? "
					+ "AND Processed = 'Y' AND IsSimulation = 'N' AND DocStatus IN (?,?)", getTransactionName())
				.setParameters(invoice.get_ID(), getDefinition().get_ID(), getSetting().get_ID(),
						MWHWithholding.DOCSTATUS_Completed, MWHWithholding.DOCSTATUS_Closed)
				.match();
	}

	/** Acumulador por concepto de renta (base + resolución de tarifa/tramo). */
	private static class RentaConcept {
		private final X_LCO_WH_Concept concept;
		private BigDecimal base = Env.ZERO;
		private BigDecimal subjectBase = Env.ZERO;
		private BigDecimal appliedRate = Env.ZERO;
		private X_LCO_WH_Combination combination = null;
		private boolean generate = false;
		private boolean valid = true;	//	false = simulación (acumulativa bajo mínimo)

		private RentaConcept(X_LCO_WH_Concept concept) {
			this.concept = concept;
		}
	}
}
